/*
 * (C) Copyright 2014-2025 Nuxeo (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.nomode;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_NOT_FOUND_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.READER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.TEST_PASSWORD;

import java.io.IOException;
import java.util.function.UnaryOperator;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.HtmlPageHandler;
import org.nuxeo.functionaltests.explorer.ExplorerTestRule;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ComponentArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.OperationArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ServiceArtifactPage;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.JsonNodeHandler;

/**
 * Test explorer webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerTest {

    @ClassRule
    public static final ExplorerTestRule EXPLORER_HELPER = new ExplorerTestRule();

    protected static final String LIVE_NAME = "Nuxeo Platform";

    protected static String liveVersion;

    @Rule
    public final HttpClientTestRule readerHttpClient = HttpClientTestRule.builder()
                                                                         .credentials(READER_USERNAME, TEST_PASSWORD)
                                                                         .accept(MediaType.TEXT_HTML)
                                                                         .build();

    /**
     * Since 20.0.0, the live distrib can only be seen by admins --> init one for tests
     */
    @BeforeClass
    public static void initPersistedDistrib() {
        liveVersion = EXPLORER_HELPER.persistLiveDistribution(LIVE_NAME);
        EXPLORER_HELPER.createReaderUser();
    }

    /**
     * Checks the distrib admin page is hidden to any non-admin user.
     */
    @Test
    public void testDistribAdminPage() {
        readerHttpClient.buildGetRequest(DistribAdminPage.URL).execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testHomePageLiveDistrib() {
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL)
                        .execute(new HtmlPageHandler<>(
                                ExplorerHomePage.builder().firstPersistedDistribution(LIVE_NAME, liveVersion)::build));
    }

    @Test
    public void testHomePageCurrentDistrib() {
        // since 20.0.0: does not redirect to current live distrib anymore, only available to admins
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT)
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testHomePageLatestDistrib() {
        // since 20.0.0: does not redirect to current live distrib anymore, only to first persisted distrib named "nuxeo
        // platform" and alike (if it exists)
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
                        .execute(new HtmlPageHandler<>(DistributionHomePage.builder()::build));
    }

    @Test
    public void testUpdateLatestDistrib() {
        readerHttpClient.buildGetRequest(DistribAdminPage.UPDATE_URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(DistribAdminPage.UPDATE_URL + '/' + getDistribId(LIVE_NAME, liveVersion))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testDeleteDistrib() {
        readerHttpClient.buildGetRequest(DistribAdminPage.DELETE_URL + '/' + getDistribId(LIVE_NAME, liveVersion))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    /**
     * Non-regression test for NXP-29193.
     */
    @Test
    public void testHomePageInvalidDistrib() {
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + "/foo-10.0").execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    protected String getArtifactURL(String type, String id) {
        return ExplorerTestRule.getArtifactURL(getDistribId(LIVE_NAME, liveVersion), type, id);
    }

    @Test
    public void testExtensionPoints() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstExtensionPoints);
        // checkExtensionPoints(false, false, false);
    }

    @Test
    public void testExtensionPointsAlternative() {
        readerHttpClient.buildGetRequest(
                getArtifactURL(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.schema.TypeService--doctype"))
                        .execute(new HtmlPageHandler<>(
                                ExtensionPointArtifactPage.builder("org.nuxeo.ecm.core.schema.TypeService", "doctype")
                                                          .tableOfContents("Documentation", "Contribution Descriptors",
                                                                  "Existing Contributions")
                                                          .documentationHtmlFromResource(
                                                                  "data/TypeService_doctype_extensionPoint_documentation.html")
                                                          .descriptors(
                                                                  "Class: org.nuxeo.ecm.core.schema.DocumentTypeDescriptor",
                                                                  "Class: org.nuxeo.ecm.core.schema.FacetDescriptor",
                                                                  "Class: org.nuxeo.ecm.core.schema.ProxiesDescriptor")::build));
    }

    @Test
    public void testExtensionPointAliases() {
        readerHttpClient.buildGetRequest(getArtifactURL(ExtensionPointInfo.TYPE_NAME, "foo"))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        var schedulerExtensionPointPageHandler = new HtmlPageHandler<>(
                ExtensionPointArtifactPage.builder("org.nuxeo.ecm.core.scheduler.SchedulerService", "schedule")
                                          .tableOfContents("Documentation", "Aliases", "Contribution Descriptors",
                                                  "Existing Contributions")
                                          .documentationHtmlFromResource(
                                                  "data/SchedulerService_schedule_extensionPoint_documentation.html")
                                          .descriptors("Class: org.nuxeo.ecm.core.scheduler.ScheduleImpl")
                                          .aliases(
                                                  "org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService--schedule")::build);
        readerHttpClient.buildGetRequest(
                getArtifactURL(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.scheduler.SchedulerService--schedule"))
                        .execute(schedulerExtensionPointPageHandler);
        readerHttpClient.buildGetRequest(getArtifactURL(ExtensionPointInfo.TYPE_NAME,
                "org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService--schedule"))
                        .execute(schedulerExtensionPointPageHandler);
    }

    @Test
    public void testContributions() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstContributions);
        // checkContributions(false, false, false);
    }

    @Test
    public void testContributionsAlternative() {
        readerHttpClient.buildGetRequest(
                getArtifactURL(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener"))
                        .execute(new HtmlPageHandler<>(
                                ContributionArtifactPage.builder("org.nuxeo.apidoc.listener.contrib", "listener")
                                                        .documentation(
                                                                "These contributions are used for latest distribution flag update "
                                                                        + "and XML attributes extractions in extension points.")

                                ::build));
        // checkContributionItemText(1,
        // "<listener async=\"false\" class=\"org.nuxeo.apidoc.listener.LatestDistributionsListener\"
        // name=\"latestDistributionsListener\" postCommit=\"false\">\n" //
        // + " <documentation>\n" //
        // + " Updates latest distribution flag.\n" //
        // + " </documentation>\n" //
        // + " <event>aboutToCreate</event>\n" //
        // + " <event>beforeDocumentModification</event>\n" //
        // + " </listener>\n" //
        // + "listener latestDistributionsListener\n" //
        // + "Updates latest distribution flag.");
    }

    @Test
    public void testServices() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstServices);
        // checkServices(false, false, false);
    }

    @Test
    public void testServicesAlternative() {
        readerHttpClient.buildGetRequest(
                getArtifactURL(ServiceInfo.TYPE_NAME, "org.nuxeo.ecm.platform.types.TypeManager"))
                        .execute(new HtmlPageHandler<>(
                                ServiceArtifactPage.builder("org.nuxeo.ecm.platform.types.TypeManager",
                                        "org.nuxeo.ecm.platform.types.TypeService")
                                                   .tableOfContents("Implementation")::build));
    }

    @Test
    public void testOperations() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstOperations);
        // checkOperations(false, false);
    }

    @Test
    public void testOperationsAlternative() {
        readerHttpClient.buildGetRequest(getArtifactURL(OperationInfo.TYPE_NAME, "FileManager.ImportWithMetaData"))
                        .execute(new HtmlPageHandler<>(
                                OperationArtifactPage.builder("FileManager.ImportWithMetaData",
                                        "FileManager.ImportWithMetaData",
                                        "org.nuxeo.ecm.core.automation.features.operations")
                                                     .tableOfContents("Parameters", "Signature",
                                                             "Implementation Information", "JSON Definition")
                                                     .additionalInfo("Chain")
                                                     .signature("bloblist, blob", "documents, document")
                                                     .implementation(
                                                             "org.nuxeo.ecm.automation.core.impl.OperationChainCompiler.CompiledChainImpl")

                                ::build));
        // checkContributingComponentText("Contributing Component org.nuxeo.ecm.core.automation.features.operations");
    }

    @Test
    public void testComponents() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstExtensionPoints);
        // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        // header = header.navigateTo(header.components);
        // header.checkSelectedTab(header.components);
        // checkComponents(false, false, false);
    }

    @Test
    public void testComponentsAlternative() {
        readerHttpClient.buildGetRequest(
                getArtifactURL(ComponentInfo.TYPE_NAME, "org.nuxeo.ecm.automation.server.marshallers"))
                        .execute(new HtmlPageHandler<>(
                                ComponentArtifactPage.builder("org.nuxeo.ecm.automation.server.marshallers",
                                        "org.nuxeo.ecm.automation.io")
                                                     .tableOfContents("Resolution Order", "Contributions", "XML Source")
                                                     .startOrderPresence(false)
                                                     .xmlSourcePresence(true)::build));
    }

    protected void checkOverridePage(String url, String referenceFilePath) {
        readerHttpClient.buildGetRequest(url).accept(MediaType.TEXT_XML).execute(response -> {
            try (var expectedStream = ExplorerTestRule.getReferenceStream(referenceFilePath)) {
                assert expectedStream != null;
                assertEquals(IOUtils.toString(expectedStream, UTF_8),
                        IOUtils.toString(response.getEntityInputStream(), UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read expected file", e);
            }
            return null;
        });
    }

    /**
     * Non-regression test for NXP-29755
     *
     * @since 20.1.0
     */
    @Test
    public void testComponentsOverride() {
        String distribId = getDistribId(LIVE_NAME, liveVersion);
        String componentId = "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent";
        String url = String.format("%s/%s/%s/%s/override", ExplorerHomePage.URL, distribId,
                ApiBrowserConstants.VIEW_COMPONENT, componentId);
        String filterUrl = String.format("%s?contributionId=%s--%s", url, componentId, "exporters");
        checkOverridePage(url, "data/override_component_reference.xml");
        checkOverridePage(filterUrl, "data/override_contribution_reference.xml");
    }

    @Test
    public void testComponentAliases() {
        readerHttpClient.buildGetRequest(getArtifactURL(ComponentInfo.TYPE_NAME, "foo"))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        var schedulerComponentPageHandler = new HtmlPageHandler<>(
                ComponentArtifactPage.builder("org.nuxeo.ecm.core.scheduler.SchedulerService",
                        "org.nuxeo.ecm.core.event")
                                     .tableOfContents("Documentation", "Requirements", "Aliases", "Resolution Order",
                                             "Start Order", "Implementation", "Services", "Extension Points",
                                             "Contributions", "XML Source")
                                     .documentation("Core scheduler registry service.")
                                     .requirements("org.nuxeo.runtime.cluster.ClusterService")
                                     .implementation("org.nuxeo.ecm.core.scheduler.SchedulerServiceImpl")
                                     .startOrderPresence(true)
                                     .aliases("org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService")
                                     .xmlSourcePresence(true)::build);
        readerHttpClient.buildGetRequest(
                getArtifactURL(ComponentInfo.TYPE_NAME, "org.nuxeo.ecm.core.scheduler.SchedulerService"))
                        .execute(schedulerComponentPageHandler);
        readerHttpClient.buildGetRequest(getArtifactURL(ComponentInfo.TYPE_NAME,
                "org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService"))
                        .execute(schedulerComponentPageHandler);
    }

    @Test
    public void testBundles() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstExtensionPoints);
        // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        // header = header.navigateTo(header.bundles);
        // header.checkSelectedTab(header.bundles);
        // checkBundles(false, false, false);
    }

    @Test
    public void testBundlesAlternative() {
        readerHttpClient.buildGetRequest(getArtifactURL(BundleInfo.TYPE_NAME, "org.nuxeo.apidoc.webengine"))
                        .execute(new HtmlPageHandler<>(
                                BundleArtifactPage.builder("org.nuxeo.apidoc", "org.nuxeo.apidoc.webengine")
                                                  .tableOfContents("Documentation", "Requirements", "Components",
                                                          "Packages", "Maven Artifact", "Manifest", "Exports", "Charts")
                                                  .documentationHtmlFromResource("data/apidoc_webengine_readmes.html")
                                                  .mavenArtifact("org.nuxeo.ecm.platform", "nuxeo-apidoc-webengine")
                                                  .requirements("org.nuxeo.ecm.webengine.core", "org.nuxeo.apidoc.core")
                                                  .packages("platform-explorer")::build));
    }

    @Test
    public void testBundlesAlternative2() {
        readerHttpClient.buildGetRequest(getArtifactURL(BundleInfo.TYPE_NAME, "org.nuxeo.apidoc.repo"))
                        .execute(new HtmlPageHandler<>(
                                BundleArtifactPage.builder("org.nuxeo.apidoc", "org.nuxeo.apidoc.repo")
                                                  .tableOfContents("Documentation", "Resolution Order", "Components",
                                                          "Packages", "Maven Artifact", "Manifest", "Exports", "Charts")
                                                  .documentationHtmlFromResource("data/apidoc_repo_readmes.html")
                                                  .mavenArtifact("org.nuxeo.ecm.platform", "nuxeo-apidoc-repo")
                                                  .resolutionOrderPresence(true)
                                                  .packages("platform-explorer")::build));
    }

    @Test
    public void testBundleGroups() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstPersistedDistrib);
        // DistributionHomePage dhome = asPage(DistributionHomePage.class);
        // dhome.clickOn(dhome.bundleGroups);
        // checkBundleGroups(false, null, false, false);
    }

    @Test
    public void testBundleGroupsAlternative() {
        // check subgroup
        // goToArtifact(BundleGroup.TYPE_NAME, "org.nuxeo.ecm.directory");
        // BundleGroupArtifactPage apage = asPage(BundleGroupArtifactPage.class);
        // apage.checkAlternative();
    }

    @Test
    public void testPackages() {
        // ExplorerHomePage home = goHome();
        // home.clickOn(home.firstPersistedDistrib);
        // DistributionHomePage dhome = asPage(DistributionHomePage.class);
        // dhome.clickOn(dhome.packages);
        // checkPackages(false, false);
    }

    /**
     * Non-regression test for NXP-29820.
     *
     * @since 20.2.0
     */
    @Test
    public void testOverrideContributionGetURL() {
        String contribUrl = getArtifactURL(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener");
        // open(contribUrl + "/override");
        checkOverridePage(contribUrl + "/override", "data/override_reference.xml");
    }

    /**
     * Non-regression test for NXP-29378.
     */
    @Test
    public void testPseudoComponent() {
        readerHttpClient.buildGetRequest(getArtifactURL(ComponentInfo.TYPE_NAME, "org.nuxeo.runtime.started"))
                        .execute(new HtmlPageHandler<>(
                                ComponentArtifactPage.builder("org.nuxeo.runtime.started", "org.nuxeo.runtime")
                                                     .tableOfContents("Resolution Order")
                                                     .xmlSourcePresence(false)::build));
    }

    @Test
    public void testJson() {
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + getDistribId(LIVE_NAME, liveVersion) + '/'
                + ApiBrowserConstants.JSON_ACTION).accept(MediaType.APPLICATION_JSON).execute(new JsonNodeHandler());
        // setup page load timeout of 3 mins as persisted export can take time (default: 1 min)
    }

    /** @since 22 */
    @Test
    public void testPackagesJson() {
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + getDistribId(LIVE_NAME, liveVersion) + '/'
                + ApiBrowserConstants.LIST_PACKAGES)
                        .accept(MediaType.APPLICATION_JSON)
                        .executeAndConsume(new JsonNodeHandler(), node -> {
                            var packages = node.get("packages");
                            assertNotNull(packages);
                            assertTrue(packages.isArray());
                            assertEquals(3, packages.size());
                            assertEquals("nuxeo-opensearch1-embed", packages.get(0).asText());
                            assertEquals("nuxeo-search-client-opensearch1", packages.get(1).asText());
                            assertEquals("platform-explorer", packages.get(2).asText());
                        });
    }

    @Test
    public void testInvalidArtifactPages() {
        UnaryOperator<String> getInvalidArtifactURLByType = type -> getArtifactURL(type, "foo");
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(BundleGroup.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(BundleInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ComponentInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ExtensionInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ExtensionPointInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ServiceInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(PackageInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        readerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(OperationInfo.TYPE_NAME))
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    /** @since 22 */
    @Test
    public void testStatsPage() throws IOException {
        // open(String.format("%s%s", ExplorerHomePage.URL, Distribution.VIEW_STATS));
        // asPage(StatsPage.class).check();
    }

    protected String getDistribId(String distribName, String version) {
        return distribName + '-' + version;
    }
}
