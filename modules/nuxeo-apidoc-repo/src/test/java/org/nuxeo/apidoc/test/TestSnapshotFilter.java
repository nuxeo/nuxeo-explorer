/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.PersistSnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.TargetExtensionPointSnapshotFilter;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.apidoc.repo:apidoc-automation-test-contrib.xml")
public class TestSnapshotFilter extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Before
    public void init() throws PackageException, IOException {
        mockPackageServices();
    }

    protected void checkApiDoc(String filterName, DistributionSnapshot snapshot, boolean isRef) {
        assertNotNull(snapshot);
        if (isRef) {
            assertEquals(List.of(filterName, filterName + SnapshotFilter.REFERENCE_FILTER_NAME_SUFFIX),
                    snapshot.getBundleGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        } else {
            assertEquals(List.of(filterName),
                    snapshot.getBundleGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        }
        assertEquals(List.of("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo"),
                snapshot.getBundleGroup(filterName).getBundleIds());
        if (isRef) {
            assertEquals(
                    List.of("org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.scripting", "org.nuxeo.ecm.core",
                            "org.nuxeo.ecm.core.api", "org.nuxeo.ecm.core.event", "org.nuxeo.ecm.core.schema",
                            "org.nuxeo.runtime"),
                    snapshot.getBundleGroup(filterName + SnapshotFilter.REFERENCE_FILTER_NAME_SUFFIX).getBundleIds());
        }
        if (isRef) {
            assertEquals(
                    List.of("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo", "org.nuxeo.ecm.automation.core",
                            "org.nuxeo.ecm.automation.scripting", "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api",
                            "org.nuxeo.ecm.core.event", "org.nuxeo.ecm.core.schema", "org.nuxeo.runtime"),
                    snapshot.getBundleIds());
        } else {
            assertEquals(List.of("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo"), snapshot.getBundleIds());
        }
        if (isRef) {
            assertEquals(List.of(
                    // inner components
                    "org.nuxeo.apidoc.adapterContrib", "org.nuxeo.apidoc.doctypeContrib",
                    "org.nuxeo.apidoc.lifecycle.contrib", "org.nuxeo.apidoc.listener.contrib",
                    "org.nuxeo.apidoc.schemaContrib", "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                    "org.nuxeo.apidoc.test.automation", "org.nuxeo.apidoc.test.works",
                    // referenced components
                    "org.nuxeo.automation.scripting.internals.AutomationScriptingComponent",
                    "org.nuxeo.ecm.core.api.DocumentAdapterService", "org.nuxeo.ecm.core.event.EventServiceComponent",
                    "org.nuxeo.ecm.core.lifecycle.LifeCycleService",
                    "org.nuxeo.ecm.core.operation.OperationServiceComponent", "org.nuxeo.ecm.core.schema.TypeService",
                    "org.nuxeo.ecm.core.work.service", "org.nuxeo.runtime.ConfigurationService"),
                    snapshot.getComponentIds());
        } else {
            assertEquals(
                    List.of("org.nuxeo.apidoc.adapterContrib", "org.nuxeo.apidoc.doctypeContrib",
                            "org.nuxeo.apidoc.lifecycle.contrib", "org.nuxeo.apidoc.listener.contrib",
                            "org.nuxeo.apidoc.schemaContrib", "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                            "org.nuxeo.apidoc.test.automation", "org.nuxeo.apidoc.test.works"),
                    snapshot.getComponentIds());
        }
        if (isRef) {
            assertEquals(List.of("org.nuxeo.apidoc.search.ArtifactSearcher",
                    "org.nuxeo.apidoc.snapshot.SnapshotManager",
                    "org.nuxeo.automation.scripting.api.AutomationScriptingService",
                    "org.nuxeo.ecm.automation.AutomationAdmin", "org.nuxeo.ecm.automation.AutomationService",
                    "org.nuxeo.ecm.automation.context.ContextService",
                    "org.nuxeo.ecm.automation.core.events.EventHandlerRegistry",
                    "org.nuxeo.ecm.automation.core.trace.TracerFactory",
                    "org.nuxeo.ecm.core.api.adapter.DocumentAdapterService", "org.nuxeo.ecm.core.event.EventProducer",
                    "org.nuxeo.ecm.core.event.EventService", "org.nuxeo.ecm.core.event.EventServiceAdmin",
                    "org.nuxeo.ecm.core.lifecycle.LifeCycleService",
                    "org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler",
                    "org.nuxeo.ecm.core.schema.SchemaManager", "org.nuxeo.ecm.core.schema.TypeProvider",
                    "org.nuxeo.ecm.core.work.api.WorkManager",
                    "org.nuxeo.runtime.services.config.ConfigurationService"), snapshot.getServiceIds());
        } else {
            assertEquals(
                    List.of("org.nuxeo.apidoc.search.ArtifactSearcher", "org.nuxeo.apidoc.snapshot.SnapshotManager"),
                    snapshot.getServiceIds());
        }
        if (isRef) {
            assertEquals(List.of(
                    // inner extension points
                    "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--exporters",
                    "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                    // referenced extension points
                    "org.nuxeo.automation.scripting.internals.AutomationScriptingComponent--operation",
                    "org.nuxeo.ecm.core.api.DocumentAdapterService--adapters",
                    "org.nuxeo.ecm.core.event.EventServiceComponent--listener",
                    "org.nuxeo.ecm.core.lifecycle.LifeCycleService--types",
                    "org.nuxeo.ecm.core.operation.OperationServiceComponent--chains",
                    "org.nuxeo.ecm.core.operation.OperationServiceComponent--operations",
                    "org.nuxeo.ecm.core.schema.TypeService--doctype", "org.nuxeo.ecm.core.schema.TypeService--schema",
                    "org.nuxeo.ecm.core.work.service--queues", "org.nuxeo.runtime.ConfigurationService--configuration"),
                    snapshot.getExtensionPointIds());
        } else {
            assertEquals(
                    List.of("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--exporters",
                            "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins"),
                    snapshot.getExtensionPointIds());
        }
        assertEquals(List.of("org.nuxeo.apidoc.adapterContrib--adapters", "org.nuxeo.apidoc.doctypeContrib--doctype",
                "org.nuxeo.apidoc.lifecycle.contrib--types", "org.nuxeo.apidoc.listener.contrib--listener",
                "org.nuxeo.apidoc.schemaContrib--schema",
                "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration",
                "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration1",
                "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration2",
                "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration3",
                "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--exporters",
                "org.nuxeo.apidoc.test.automation--chains", "org.nuxeo.apidoc.test.automation--operation",
                "org.nuxeo.apidoc.test.automation--operations", "org.nuxeo.apidoc.test.works--queues",
                "org.nuxeo.apidoc.test.works--queues1"), snapshot.getContributionIds());
        assertEquals(List.of("Document.Create", "Scripting.HelloWorld", "createDoc"),
                snapshot.getOperations().stream().map(OperationInfo::getName).collect(Collectors.toList()));
        assertEquals(List.of(MOCK_PACKAGE_ID),
                snapshot.getPackages().stream().map(PackageInfo::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFilterBundle() throws IOException {
        String filterName = "apidoc-bundle";
        PersistSnapshotFilter filter = new PersistSnapshotFilter(filterName, false, null);
        filter.addBundle("org.nuxeo.apidoc");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);
        // nothing selected: filter should hold exact bundle names
        assertEquals(0, snapshot.getBundleIds().size());

        filter = new PersistSnapshotFilter(filterName, false, null);
        filter.addBundle("org.nuxeo.apidoc.core");
        filter.addBundle("org.nuxeo.apidoc.repo");
        snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);

        checkApiDoc(filterName, snapshot, false);
    }

    @Test
    public void testFilterBundlePrefix() throws IOException {
        String filterName = "apidoc-bundle";
        PersistSnapshotFilter filter = new PersistSnapshotFilter(filterName);
        filter.addBundle("org.nuxeo.apidoc");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);

        checkApiDoc(filterName, snapshot, false);
    }

    @Test
    public void testFilterBundlePrefixReference() throws IOException {
        String filterName = "apidoc-bundle";
        PersistSnapshotFilter filter = new PersistSnapshotFilter(filterName, true,
                TargetExtensionPointSnapshotFilter.class);
        filter.addBundle("org.nuxeo.apidoc");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        checkApiDoc(filterName, snapshot, true);
    }

    @Test
    public void testFilterNuxeoPackage() throws IOException {
        String filterName = "apidoc-pack";
        PersistSnapshotFilter filter = new PersistSnapshotFilter(filterName, false, null);
        filter.addNuxeoPackage(MOCK_PACKAGE_NAME);

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        checkApiDoc(filterName, snapshot, false);
    }

    @Test
    public void testFilterNuxeoPackagePrefix() throws IOException {
        String filterName = "apidoc-pack";
        PersistSnapshotFilter filter = new PersistSnapshotFilter(filterName, true, null);
        filter.addNuxeoPackage("platform-");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        checkApiDoc(filterName, snapshot, false);
    }

    @Test
    public void testFilterNuxeoPackageReference() throws IOException {
        String filterName = "apidoc-pack";
        PersistSnapshotFilter filter = new PersistSnapshotFilter(filterName, false,
                TargetExtensionPointSnapshotFilter.class);
        filter.addNuxeoPackage(MOCK_PACKAGE_NAME);

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        checkApiDoc(filterName, snapshot, true);
    }

    @Test
    public void testFilterJavaPackagePrefix() throws IOException {
        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc-java");
        filter.addPackagesPrefix("org.nuxeo.ecm.automation.core.operations.services.query");
        filter.addPackagesPrefix("org.nuxeo.ecm.automation.core.operations.services.workmanager");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);

        assertEquals(List.of("apidoc-java"),
                snapshot.getBundleGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        assertEquals(List.of("org.nuxeo.ecm.automation.features"), snapshot.getBundleIds());
        assertEquals(List.of("org.nuxeo.ecm.core.automation.features.operations"), snapshot.getComponentIds());
        assertEquals(0, snapshot.getServiceIds().size());
        assertEquals(0, snapshot.getExtensionPointIds().size());
        assertEquals(
                List.of("org.nuxeo.ecm.core.automation.features.operations--chains",
                        "org.nuxeo.ecm.core.automation.features.operations--operations"),
                snapshot.getContributionIds());
        assertEquals(List.of("Repository.Query", "Repository.ResultSetQuery", "WorkManager.RunWorkInFailure"),
                snapshot.getOperations().stream().map(OperationInfo::getName).collect(Collectors.toList()));
        assertEquals(0, snapshot.getPackages().size());
    }

}
