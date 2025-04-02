/*
 * (C) Copyright 2020-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.functionaltests.explorer.nomode;

import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_NOT_FOUND_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_OK_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.MANAGER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.TEST_PASSWORD;

import java.io.File;
import java.util.function.UnaryOperator;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.test.ModuleUnderTest;
import org.nuxeo.functionaltests.HtmlLink;
import org.nuxeo.functionaltests.HtmlPageHandler;
import org.nuxeo.functionaltests.explorer.ExplorerTestRule;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.http.test.HttpClientTestRule;

import net.htmlparser.jericho.HTMLElementName;

/**
 * Tests features for {@link SecurityHelper#DEFAULT_APIDOC_MANAGERS_GROUP} members.
 *
 * @since 20.0.0
 */
public class ITExplorerApidocManagerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File(ModuleUnderTest.getOutputDirectory()));

    @Rule
    public final ExplorerTestRule explorerHelper = new ExplorerTestRule();

    @Rule
    public final HttpClientTestRule adminHttpClient = HttpClientTestRule.builder()
                                                                        .adminCredentials()
                                                                        .accept(MediaType.TEXT_HTML)
                                                                        .build();

    @Rule
    public final HttpClientTestRule managerHttpClient = HttpClientTestRule.builder()
                                                                          .credentials(MANAGER_USERNAME, TEST_PASSWORD)
                                                                          .accept(MediaType.TEXT_HTML)
                                                                          .build();

    @Before
    public void before() {
        explorerHelper.createManagerUser();
    }

    @Test
    public void testDistribAdminPage() {
        managerHttpClient.buildGetRequest(DistribAdminPage.URL)
                         .execute(new HtmlPageHandler<>(DistribAdminPage.builder()
                                                                        // since 20.0.0: cannot save anymore
                                                                        .savePresence(false)::build));
    }

    @Test
    public void testHomePageLiveDistrib() {
        managerHttpClient.buildGetRequest(ExplorerHomePage.URL)
                         .execute(new HtmlPageHandler<>(ExplorerHomePage.builder()
                                                                        // since 20.0.0: cannot see current live distrib
                                                                        .currentDistribution(false)
                                                                        .uploadFragmentPresence(true)
                                                                        .manageDistributionsPresence(true)::build));
    }

    @Test
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        // log as admin to perform export of live distrib first
        String version = explorerHelper.persistLiveDistribution(distribName);
        String distribId = distribName + '-' + version;
        var distribAdminPage = managerHttpClient.buildGetRequest(DistribAdminPage.URL)
                                                .execute(new HtmlPageHandler<>(
                                                        DistribAdminPage.builder()
                                                                        .hasDistribution(distribName, version)::build));

        // check importing it back
        var exportLink = distribAdminPage.getDistributions()
                                         .getCell(1, 6)
                                         .firstWithNameAndClassAs(HTMLElementName.A, "export-zip", HtmlLink::new)
                                         .getHrefAndStrip("");
        var exportFile = managerHttpClient.buildGetRequest(exportLink).execute(response -> {
            try (var responseStream = response.getEntityInputStream()) {
                var file = folder.newFile("nuxeo-distribution-%s.zip".formatted(distribId));
                FileUtils.copyToFile(responseStream, file);
                return file;
            }
        });
        explorerHelper.importDistributionFromDistribAdminPage(managerHttpClient, "imported-server", "1.0.0",
                exportFile);
        managerHttpClient.buildGetRequest(ExplorerHomePage.URL + "/imported-server-1.0.0")
                         .execute(HTTP_STATUS_OK_CHECKER);
    }

    @Test
    public void testLivePartialDistribExportAndImport() {
        String distribName = "my-partial-server";
        // log as admin to perform export of live distrib first
        String version = explorerHelper.persistLiveDistribution(distribName, ExplorerTestRule.PersistOption.PARTIAL);
        String distribId = distribName + '-' + version;
        var distribAdminPage = managerHttpClient.buildGetRequest(DistribAdminPage.URL)
                                                .execute(new HtmlPageHandler<>(
                                                        DistribAdminPage.builder()
                                                                        .hasDistribution(distribName, version)::build));

        // check importing it back
        var exportLink = distribAdminPage.getDistributions()
                                         .getCell(1, 6)
                                         .firstWithNameAndClassAs(HTMLElementName.A, "export-zip", HtmlLink::new)
                                         .getHrefAndStrip("");
        var exportFile = managerHttpClient.buildGetRequest(exportLink).execute(response -> {
            try (var responseStream = response.getEntityInputStream()) {
                var file = folder.newFile("nuxeo-distribution-%s.zip".formatted(distribId));
                FileUtils.copyToFile(responseStream, file);
                return file;
            }
        });
        explorerHelper.importDistributionFromDistribAdminPage(managerHttpClient, "partial-imported-server", "1.0.0",
                exportFile);
        managerHttpClient.buildGetRequest(ExplorerHomePage.URL + "/partial-imported-server-1.0.0")
                         .execute(HTTP_STATUS_OK_CHECKER);
    }

    @Test
    public void testLiveDistribExportAndDelete() {
        String distribName = "my-server-to-delete";
        // log as admin to perform export of live distrib first
        String version = explorerHelper.persistLiveDistribution(distribName);
        String distribId = distribName + '-' + version;
        managerHttpClient.buildGetRequest(DistribAdminPage.URL)
                         .execute(new HtmlPageHandler<>(
                                 DistribAdminPage.builder().hasDistribution(distribName, version)::build));
        managerHttpClient.buildGetRequest(DistribAdminPage.DELETE_URL + '/' + distribId)
                         .execute(new HtmlPageHandler<>(
                                 DistribAdminPage.builder().hasNotDistribution(distribId, version)::build));
    }

    @Test
    public void testJson() {
        managerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT + '/'
                + ApiBrowserConstants.JSON_ACTION)
                         .accept(MediaType.APPLICATION_JSON)
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    /** @since 22 */
    @Test
    public void testPackagesJson() {
        managerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT + '/'
                + ApiBrowserConstants.LIST_PACKAGES)
                         .accept(MediaType.APPLICATION_JSON)
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testInvalidArtifactPages() {
        UnaryOperator<String> getInvalidArtifactURLByType = type -> getArtifactURL(type, "foo");
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(BundleGroup.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(BundleInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ComponentInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ExtensionInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ExtensionPointInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ServiceInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(PackageInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        managerHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(OperationInfo.TYPE_NAME))
                         .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    protected String getArtifactURL(String type, String id) {
        return ExplorerTestRule.getArtifactURL(SnapshotManager.DISTRIBUTION_ALIAS_CURRENT, type, id);
    }
}
