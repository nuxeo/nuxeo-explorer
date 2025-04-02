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
package org.nuxeo.functionaltests.explorer.sitemode;

import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_NOT_FOUND_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestRule.SAMPLE_EXPORT_DISTRIBUTION_NAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestRule.SAMPLE_EXPORT_DISTRIBUTION_VERSION;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.HtmlPageHandler;
import org.nuxeo.functionaltests.explorer.ExplorerTestRule;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.http.test.HttpClientTestRule;

/**
 * Test Explorer pages usually handled by admins.
 *
 * @since 20.0.0
 */
public class ITExplorerAdminSiteModeTest {

    @ClassRule
    public static final ExplorerTestRule EXPLORER_HELPER = new ExplorerTestRule();

    @Rule
    public final HttpClientTestRule adminHttpClient = HttpClientTestRule.builder()
                                                                        .adminCredentials()
                                                                        .accept(MediaType.TEXT_HTML)
                                                                        .build();

    @BeforeClass
    public static void initPersistedDistrib() throws IOException {
        EXPLORER_HELPER.importSampleExportDistribution();
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() {
        adminHttpClient.buildGetRequest(ExplorerHomePage.URL)
                       .execute(new HtmlPageHandler<>(
                               ExplorerHomePage.builder()
                                               .firstPersistedDistribution(SAMPLE_EXPORT_DISTRIBUTION_NAME,
                                                       SAMPLE_EXPORT_DISTRIBUTION_VERSION)
                                               .manageDistributionsPresence(true)::build));
    }

    @Test
    public void testDistribAdminPage() {
        adminHttpClient.buildGetRequest(DistribAdminPage.URL)
                       .execute(new HtmlPageHandler<>(DistribAdminPage.builder()::build));
    }

    @Test
    public void testHomePageCurrentDistrib() {
        // since 20.0.0: cannot see current live distrib anymore
        adminHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT)
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testHomePageLatestDistrib() {
        // persisted distrib redirection
        adminHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
                       .execute(new HtmlPageHandler<>(DistributionHomePage.builder()::build));
    }

    @Test
    public void testSampleDistribDelete() throws IOException {
        adminHttpClient.buildGetRequest(DistribAdminPage.URL)
                       .execute(new HtmlPageHandler<>(
                               DistribAdminPage.builder()
                                               .hasDistribution(SAMPLE_EXPORT_DISTRIBUTION_NAME,
                                                       SAMPLE_EXPORT_DISTRIBUTION_VERSION)::build));
        adminHttpClient.buildGetRequest(DistribAdminPage.DELETE_URL + '/' + SAMPLE_EXPORT_DISTRIBUTION_NAME + '-'
                + SAMPLE_EXPORT_DISTRIBUTION_VERSION)
                       .execute(new HtmlPageHandler<>(
                               DistribAdminPage.builder()
                                               .hasNotDistribution(SAMPLE_EXPORT_DISTRIBUTION_NAME,
                                                       SAMPLE_EXPORT_DISTRIBUTION_VERSION)::build));
        // recreate the deleted sample, as expected by other tests
        EXPLORER_HELPER.importSampleExportDistribution();
    }

}
