/*
 * (C) Copyright 2012-2020 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.JsonPrettyPrinter;
import org.nuxeo.apidoc.snapshot.PersistSnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.TargetExtensionPointSnapshotFilter;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestJson extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Before
    public void initMocks() throws PackageException, IOException {
        mockPackageServices();
    }

    @Test
    public void canSerializeRuntimeAndReadBackLive() throws IOException {
        DistributionSnapshot snapshot = RuntimeSnapshot.build();
        assertNotNull(snapshot);
        canSerializeAndReadBack(snapshot, false);
    }

    @Test
    public void canSerializeRepositoryAndReadBackPersisted() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);

        assertTrue(snapshot instanceof RepositoryDistributionSnapshot);
        canSerializeAndReadBack(snapshot, true);
    }

    protected void canSerializeAndReadBack(DistributionSnapshot snap, boolean persisted) throws IOException {
        checkSnapshot(snap, persisted, false);
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            snap.writeJson(sink, null, null);
            checkSnapshot(snap, persisted, false);
            try (OutputStream file = Files.newOutputStream(Paths.get("target/test.json"),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                file.write(sink.toByteArray());
            }
            try (ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray())) {
                DistributionSnapshot snapshot = snap.readJson(source);
                assertNotNull(snapshot);
                checkSnapshot(snapshot, persisted, false);
            }
        }
    }

    protected SnapshotFilter getFilter(Class<? extends SnapshotFilter> refFilter) {
        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc", false, refFilter) {
            @Override
            public boolean accept(NuxeoArtifact artifact) {
                if (artifact instanceof OperationInfo) {
                    OperationInfo op = (OperationInfo) artifact;
                    return Arrays.asList("Actions.GET", "AttachFiles").contains(op.getName());
                }
                return super.accept(artifact);
            };
        };
        filter.addNuxeoPackage(MOCK_PACKAGE_NAME);
        return filter;
    }

    @Test
    public void canWritePartial() throws IOException {
        RuntimeSnapshot snapshot = RuntimeSnapshot.build();
        assertNotNull(snapshot);

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        snapshot.writeJson(sink, getFilter(null), new JsonPrettyPrinter());

        checkJsonContentEquals("test-export.json", sink.toString());
    }

    @Test
    public void canWritePartialRef() throws IOException {
        RuntimeSnapshot snapshot = RuntimeSnapshot.build();
        assertNotNull(snapshot);

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        snapshot.writeJson(sink, getFilter(TargetExtensionPointSnapshotFilter.class), new JsonPrettyPrinter());

        checkJsonContentEquals("test-export-ref.json", sink.toString());
    }

    @Test
    public void canWritePartialPersisted() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(snapshot);

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        snapshot.writeJson(sink, getFilter(null), new JsonPrettyPrinter());

        checkJsonContentEquals("test-export.json", sink.toString());
    }

    @Test
    public void canWritePartialRefPersisted() throws Exception {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(snapshot);

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        snapshot.writeJson(sink, getFilter(TargetExtensionPointSnapshotFilter.class), new JsonPrettyPrinter());

        // ordering is different with persisted distribs --> use JSONAssert comparison instead
        checkJsonAssertEquals("test-export-ref.json", sink.toString());
    }

    /**
     * Reads a reference export kept in tests, to detect potential compatibility changes.
     *
     * @implNote reference file "test-export.json" is initialized thanks to above method, keeping only a few operations
     *           and bundles.
     * @since 11.1
     */
    @Test
    public void canReadPartial() throws IOException {
        RuntimeSnapshot runtimeSnapshot = RuntimeSnapshot.build();
        String export = getReferenceContent(getReferencePath("test-export.json"));
        try (ByteArrayInputStream source = new ByteArrayInputStream(export.getBytes())) {
            DistributionSnapshot snapshot = runtimeSnapshot.readJson(source);
            checkSnapshot(snapshot, false, true);
        }
    }

    protected void checkSnapshot(DistributionSnapshot snapshot, boolean persisted, boolean partial) throws IOException {
        assertNotNull(snapshot);
        String dname = "Nuxeo";
        assertEquals(dname, snapshot.getName());
        String sVersion = "unknown";
        if (partial) {
            sVersion = "mockTestVersion";
        }
        assertEquals(sVersion, snapshot.getVersion());
        if (partial) {
            assertNull(snapshot.getCreationDate());
        } else {
            assertNotNull(snapshot.getCreationDate());
        }
        assertEquals(String.format("%s-%s", dname, sVersion), snapshot.getKey());

        BundleInfo bundle = snapshot.getBundle("org.nuxeo.apidoc.repo");
        assertNotNull(bundle);
        assertEquals("nuxeo-apidoc-repo", bundle.getArtifactId());
        assertEquals(BundleInfo.TYPE_NAME, bundle.getArtifactType());

        String version = "mockTestArtifactVersion";
        if (partial) {
            assertEquals(version, bundle.getArtifactVersion());
        } else {
            version = bundle.getArtifactVersion();
            assertNotNull(version);
            assertTrue(version.trim().length() > 0);
        }

        assertEquals("org.nuxeo.apidoc.repo", bundle.getBundleId());
        assertEquals("org.nuxeo.ecm.platform", bundle.getGroupId());
        assertEquals("/grp:org.nuxeo.ecm.platform/grp:org.nuxeo.apidoc/org.nuxeo.apidoc.repo",
                bundle.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.repo", bundle.getId());
        assertNull(bundle.getReadme());
        // manifest replaced in tests as it can differ depending on the jar build
        assertNotNull(bundle.getManifest());
        Blob parentReadme = bundle.getParentReadme();
        assertNotNull(parentReadme);
        checkContentEquals("apidoc_snapshot/apidoc_readme.txt", parentReadme.getString());
        assertEquals(Arrays.asList(), bundle.getRequirements());
        assertEquals(Long.valueOf(66), bundle.getMinResolutionOrder());
        assertEquals(Long.valueOf(72), bundle.getMaxResolutionOrder());
        assertEquals(version, bundle.getVersion());
        // check readme on core bundle instead
        BundleInfo coreBundle = snapshot.getBundle("org.nuxeo.apidoc.core");
        Blob readme = coreBundle.getReadme();
        assertNotNull(readme);
        checkContentEquals("apidoc_snapshot/core_readme.txt", readme.getString());
        readme = coreBundle.getParentReadme();
        assertNotNull(readme);
        checkContentEquals("apidoc_snapshot/apidoc_readme.txt", readme.getString());

        // check introspected bundle group
        BundleGroup group = bundle.getBundleGroup();
        assertNotNull(group);
        assertEquals(BundleGroup.TYPE_NAME, group.getArtifactType());
        assertEquals(sVersion, group.getVersion());
        assertEquals(Arrays.asList("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo"), group.getBundleIds());
        assertEquals("grp:org.nuxeo.apidoc", group.getId());
        assertEquals("org.nuxeo.apidoc", group.getName());
        assertEquals("/grp:org.nuxeo.ecm.platform/grp:org.nuxeo.apidoc", group.getHierarchyPath());
        assertEquals(Arrays.asList("grp:org.nuxeo.ecm.platform"), group.getParentIds());
        List<Blob> readmes = group.getReadmes();
        assertNotNull(readmes);
        assertEquals(1, readmes.size());
        checkContentEquals("apidoc_snapshot/apidoc_readme.txt", readmes.get(0).getString());
        assertEquals(Arrays.asList(), group.getSubGroups());
        assertEquals(sVersion, group.getVersion());
        assertNotNull(group.getParentGroup());
        assertEquals("grp:org.nuxeo.ecm.platform", group.getParentGroup().getId());

        // check bundle group from maven group id
        BundleGroup mvnGroup = snapshot.getBundleGroup(bundle.getGroupId());
        assertNotNull(mvnGroup);
        assertEquals("grp:org.nuxeo.ecm.platform", mvnGroup.getId());
        assertEquals("org.nuxeo.ecm.platform", mvnGroup.getName());
        assertEquals(BundleGroup.TYPE_NAME, mvnGroup.getArtifactType());
        assertEquals(sVersion, mvnGroup.getVersion());
        assertEquals("/grp:org.nuxeo.ecm.platform", mvnGroup.getHierarchyPath());
        if (partial) {
            assertEquals(Arrays.asList(), mvnGroup.getBundleIds());
        } else {
            assertTrue(mvnGroup.getBundleIds().size() > 1);
            assertFalse(mvnGroup.getBundleIds().contains("org.nuxeo.apidoc.core"));
            assertFalse(mvnGroup.getBundleIds().contains("org.nuxeo.apidoc.repo"));
        }
        assertEquals(Arrays.asList(), mvnGroup.getParentIds());
        List<Blob> mvnReadmes = mvnGroup.getReadmes();
        assertNotNull(mvnReadmes);
        if (partial) {
            assertEquals(0, mvnReadmes.size());
        } else {
            assertEquals(1, mvnReadmes.size());
            checkContentEquals("apidoc_snapshot/apidoc_readme.txt", mvnReadmes.get(0).getString());
        }
        if (partial) {
            assertEquals(Arrays.asList("grp:org.nuxeo.apidoc"),
                    mvnGroup.getSubGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        } else {
            List<String> sgids = mvnGroup.getSubGroups().stream().map(BundleGroup::getId).collect(Collectors.toList());
            assertTrue(sgids.size() > 1);
            assertTrue(sgids.contains("grp:org.nuxeo.ecm.directory"));
            assertTrue(sgids.contains("grp:org.nuxeo.apidoc"));
        }
        assertEquals(sVersion, mvnGroup.getVersion());
        assertNull(mvnGroup.getParentGroup());

        // check components
        List<ComponentInfo> components = bundle.getComponents();
        assertNotNull(components);
        assertEquals(7, components.size());
        ComponentInfo smcomp = snapshot.getComponent("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");
        assertNotNull(smcomp);
        assertEquals(ComponentInfo.TYPE_NAME, smcomp.getArtifactType());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", smcomp.getComponentClass());
        assertEquals("<p>\n" //
                + "      This component handles the introspection of the current live Runtime as a distribution.\n" //
                + "    </p>\n" //
                + "<p>\n" //
                + "      It can also persist this introspection as Nuxeo documents, to handle import and export of external distributions.\n" //
                + "    </p>\n", smcomp.getDocumentation());
        assertEquals("<p/>\n" //
                + "<p>\n" //
                + "This component handles the introspection of the current live Runtime as a distribution.\n" //
                + "</p>\n" //
                + "<p>\n" //
                + "It can also persist this introspection as Nuxeo documents, to handle import and export of external distributions.\n" //
                + "</p>", smcomp.getDocumentationHtml());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/grp:org.nuxeo.apidoc/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                smcomp.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", smcomp.getId());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", smcomp.getName());
        assertEquals(version, smcomp.getVersion());
        assertFalse(smcomp.isXmlPureComponent());
        checkContentEquals("apidoc_snapshot/processed-snapshot-service-framework.xml", smcomp.getXmlFileContent());
        assertEquals(Arrays.asList(), smcomp.getRequirements());
        assertEquals(Long.valueOf(69), smcomp.getResolutionOrder());
        assertNull(smcomp.getDeclaredStartOrder());
        assertEquals(Long.valueOf(135), smcomp.getStartOrder());

        // check json back reference
        assertNotNull(smcomp.getBundle());
        assertEquals("org.nuxeo.apidoc.repo", smcomp.getBundle().getId());

        // check services
        assertNotNull(smcomp.getServices());
        assertEquals(3, smcomp.getServices().size());
        ServiceInfo service = smcomp.getServices().get(0);
        assertEquals(ServiceInfo.TYPE_NAME, service.getArtifactType());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", service.getComponentId());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/grp:org.nuxeo.apidoc/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent/Services/org.nuxeo.apidoc.snapshot.SnapshotManager",
                service.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManager", service.getId());
        assertEquals(version, service.getVersion());
        assertFalse(service.isOverriden());
        // check json back reference
        assertNotNull(service.getComponent());
        // check second service id
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotListener", smcomp.getServices().get(1).getId());
        // check third service id
        assertEquals("org.nuxeo.apidoc.search.ArtifactSearcher", smcomp.getServices().get(2).getId());

        // check extension points
        assertNotNull(smcomp.getExtensionPoints());
        assertEquals(2, smcomp.getExtensionPoints().size());
        ExtensionPointInfo xp = smcomp.getExtensionPoints().get(0);
        assertEquals(ExtensionPointInfo.TYPE_NAME, xp.getArtifactType());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", xp.getComponentId());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/grp:org.nuxeo.apidoc/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent/ExtensionPoints/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                xp.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins", xp.getId());
        assertEquals("plugins (org.nuxeo.apidoc.snapshot.SnapshotManagerComponent)", xp.getLabel());
        assertEquals("plugins", xp.getName());
        assertEquals(version, xp.getVersion());
        assertNotNull(xp.getDescriptors());
        assertEquals(1, xp.getDescriptors().length);
        assertEquals("org.nuxeo.apidoc.plugin.PluginDescriptor", xp.getDescriptors()[0]);
        assertEquals("<p>\n" //
                + "        A plugin can introspect and persist information related to the current runtime environment.\n" //
                + "      </p>\n" //
                + "<p>\n" //
                + "        Sample contribution:\n" //
                + "        <code>\n" //
                + "        <extension point=\"plugins\" target=\"org.nuxeo.apidoc.snapshot.SnapshotManagerComponent\">\n" //
                + "            <plugin class=\"org.nuxeo.apidoc.seam.plugin.SeamPlugin\"\n" //
                + "                id=\"seam\" snapshotClass=\"org.nuxeo.apidoc.seam.introspection.SeamRuntimeSnapshot\">\n" //
                + "                <ui>\n" //
                + "                    <label>Seam Components</label>\n" //
                + "                    <viewType>seam</viewType>\n" //
                + "                    <homeView>listSeamComponents</homeView>\n" //
                + "                    <styleClass>seam</styleClass>\n" //
                + "                </ui>\n" //
                + "            </plugin>\n" //
                + "        </extension>\n" //
                + "    </code>\n" //
                + "</p>\n" //
                + "<p>\n" //
                + "        The class should implement the\n" //
                + "        <b>org.nuxeo.apidoc.plugin.Plugin</b>\n" //
                + "        interface.\n" //
                + "      </p>\n" //
                + "<p>\n" //
                + "        UI elements are used for rendering on webengine pages. The view type should match a webengine resource type,\n" //
                + "        and\n" //
                + "        the module holding this resource should be contributed to the main webengine module as a fragment using:\n" //
                + "        <code>\n" //
                + "          Fragment-Host: org.nuxeo.apidoc.webengine\n" //
                + "        </code>\n" //
                + "</p>\n", xp.getDocumentation());
        assertEquals("<p/>\n" //
                + "<p>\n" //
                + "A plugin can introspect and persist information related to the current runtime environment.\n" //
                + "</p>\n" //
                + "<p>\n" //
                + "Sample contribution:\n" //
                + "<p/><pre><code>        &lt;extension point=\"plugins\" target=\"org.nuxeo.apidoc.snapshot.SnapshotManagerComponent\">\n" //
                + "            &lt;plugin class=\"org.nuxeo.apidoc.seam.plugin.SeamPlugin\"\n" //
                + "                id=\"seam\" snapshotClass=\"org.nuxeo.apidoc.seam.introspection.SeamRuntimeSnapshot\">\n" //
                + "                &lt;ui>\n" //
                + "                    &lt;label>Seam Components&lt;/label>\n" //
                + "                    &lt;viewType>seam&lt;/viewType>\n" //
                + "                    &lt;homeView>listSeamComponents&lt;/homeView>\n" //
                + "                    &lt;styleClass>seam&lt;/styleClass>\n" //
                + "                &lt;/ui>\n" //
                + "            &lt;/plugin>\n" //
                + "        &lt;/extension>\n" //
                + "</code></pre><p/>\n" //
                + "</p>\n" //
                + "<p>\n" //
                + "The class should implement the\n" //
                + "<b>org.nuxeo.apidoc.plugin.Plugin</b>\n" //
                + "interface.\n" //
                + "</p>\n" //
                + "<p>\n" //
                + "UI elements are used for rendering on webengine pages. The view type should match a webengine resource type,\n" //
                + "and\n" //
                + "the module holding this resource should be contributed to the main webengine module as a fragment using:\n" //
                + "<p/><pre><code>          Fragment-Host: org.nuxeo.apidoc.webengine\n" //
                + "</code></pre><p/>\n" + //
                "</p>", xp.getDocumentationHtml());
        // check json back reference
        assertNotNull(xp.getComponent());

        // check extensions
        assertNotNull(smcomp.getExtensions());
        assertEquals(5, smcomp.getExtensions().size());

        // check another component with contributions
        ComponentInfo smcont = snapshot.getComponent("org.nuxeo.apidoc.doctypeContrib");
        assertNotNull(smcont);
        assertNotNull(smcont.getExtensions());
        assertEquals(1, smcont.getExtensions().size());
        ExtensionInfo ext = smcont.getExtensions().get(0);
        assertEquals(ExtensionInfo.TYPE_NAME, ext.getArtifactType());
        assertNotNull(ext.getContributionItems());
        assertEquals(9, ext.getContributionItems().size());
        assertEquals("\n" //
                + "      These contributions provide document types that handle persistence of introspected distributions.\n" //
                + "    \n", ext.getDocumentation());
        assertEquals("<p/>\n" //
                + "These contributions provide document types that handle persistence of introspected distributions.\n" //
                + "<p/>", ext.getDocumentationHtml());
        assertEquals("NXDistribution", ext.getContributionItems().get(0).getId());
        assertEquals("doctype NXDistribution", ext.getContributionItems().get(0).getLabel());
        assertNotNull(ext.getContributionItems().get(0).getXml());
        assertNotNull(ext.getContributionItems().get(0).getRawXml());
        assertEquals("", ext.getContributionItems().get(0).getDocumentation());
        assertEquals("org.nuxeo.ecm.core.schema.TypeService--doctype", ext.getExtensionPoint());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/grp:org.nuxeo.apidoc/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.doctypeContrib/Contributions/org.nuxeo.apidoc.doctypeContrib--doctype",
                ext.getHierarchyPath());
        assertEquals(new ComponentName("service:org.nuxeo.ecm.core.schema.TypeService"), ext.getTargetComponentName());
        assertEquals(version, ext.getVersion());
        assertEquals(Long.valueOf(2), ext.getRegistrationOrder());
        // check json back reference
        assertNotNull(ext.getComponent());

        // check contribution items doc on another component
        ComponentInfo lcomp = snapshot.getComponent("org.nuxeo.apidoc.listener.contrib");
        assertNotNull(lcomp);
        assertNotNull(lcomp.getExtensions());
        assertEquals(1, lcomp.getExtensions().size());
        ExtensionInfo lext = lcomp.getExtensions().get(0);
        assertEquals("\n" //
                + "      These contributions are used for latest distribution flag update and XML attributes extractions in\n" //
                + "      extension points.\n" //
                + "    \n", lext.getDocumentation());
        assertEquals("<p/>\n" //
                + "These contributions are used for latest distribution flag update and XML attributes extractions in\n" //
                + "extension points.\n" //
                + "<p/>", lext.getDocumentationHtml());
        assertNotNull(lext.getContributionItems());
        assertEquals(3, lext.getContributionItems().size());
        assertEquals("<p/>\nUpdates latest distribution flag.", lext.getContributionItems().get(0).getDocumentation());
        assertEquals("<p/>\nListener in charge of triggering AttributesExtractorScheduler.",
                lext.getContributionItems().get(1).getDocumentation());
        // this last one uses tag "description" instead of "documentation"
        assertEquals("<p/>\nSchedules a work for XML attributes extraction.",
                lext.getContributionItems().get(2).getDocumentation());

        // check operations
        List<OperationInfo> operations = snapshot.getOperations();
        assertNotNull(operations);
        if (partial) {
            assertEquals(2, operations.size());
        }
        OperationInfo op = operations.get(0);
        assertNotNull(op);
        assertEquals(OperationInfo.TYPE_NAME, op.getArtifactType());
        assertEquals("Services", op.getCategory());
        assertEquals("org.nuxeo.ecm.core.automation.features.operations", op.getContributingComponent());
        assertEquals(
                "Retrieve list of available actions for a given category. Action context is built based on the Operation context "
                        + "(currentDocument will be fetched from Context if not provided as input). If this operation is executed in a chain"
                        + " that initialized the Seam context, it will be used for Action context",
                op.getDescription());
        assertEquals("/op:Actions.GET", op.getHierarchyPath());
        assertEquals("op:Actions.GET", op.getId());
        assertEquals("List available actions", op.getLabel());
        assertEquals("Actions.GET", op.getName());
        assertNotNull(op.getParams());
        assertEquals(2, op.getParams().size());
        assertEquals(Arrays.asList("void", "blob", "document", "blob"), op.getSignature());
        assertEquals(Arrays.asList(), op.getAliases());

        // check packages (mocked in tests)
        List<PackageInfo> packages = snapshot.getPackages();
        assertNotNull(packages);
        assertEquals(1, packages.size());
        PackageInfo pkg = packages.get(0);
        assertNotNull(pkg);
        assertEquals(PackageInfo.TYPE_NAME, pkg.getArtifactType());
        assertEquals(Arrays.asList("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo"), pkg.getBundles());
        assertEquals("platform-explorer-mock-1.0.1", pkg.getId());
        assertEquals("platform-explorer-mock", pkg.getName());
        if (partial) {
            assertEquals("mockTestVersion", pkg.getVersion());
        } else {
            assertEquals("1.0.1", pkg.getVersion());
        }
        assertEquals("Platform Explorer Mock", pkg.getTitle());
        assertEquals(PackageType.ADDON.toString(), pkg.getPackageType());
        assertEquals(Arrays.asList("platform-explorer-base"), pkg.getDependencies());
        assertEquals(Arrays.asList(), pkg.getOptionalDependencies());
        assertEquals(Arrays.asList(), pkg.getConflicts());

        // check package retrieval through API
        PackageInfo pkg2 = snapshot.getPackage("platform-explorer-mock");
        assertNotNull(pkg2);
        assertEquals("platform-explorer-mock-1.0.1", pkg2.getId());
    }

}
