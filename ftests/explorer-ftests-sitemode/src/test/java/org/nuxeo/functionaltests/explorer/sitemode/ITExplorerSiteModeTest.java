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
package org.nuxeo.functionaltests.explorer.sitemode;

import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_NOT_FOUND_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.READER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.TEST_PASSWORD;
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
 * Test explorer in site mode.
 *
 * @since 20.0.0
 */
public class ITExplorerSiteModeTest {

    @ClassRule
    public static final ExplorerTestRule EXPLORER_HELPER = new ExplorerTestRule();

    @Rule
    public final HttpClientTestRule readerHttpClient = HttpClientTestRule.builder()
                                                                         .credentials(READER_USERNAME, TEST_PASSWORD)
                                                                         .accept(MediaType.TEXT_HTML)
                                                                         .build();

    @BeforeClass
    public static void initPersistedDistrib() throws IOException {
        EXPLORER_HELPER.importSampleExportDistribution();
        EXPLORER_HELPER.createReaderUser();
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() {
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL)
                        .execute(new HtmlPageHandler<>(
                                ExplorerHomePage.builder()
                                                .firstPersistedDistribution(SAMPLE_EXPORT_DISTRIBUTION_NAME,
                                                        SAMPLE_EXPORT_DISTRIBUTION_VERSION)::build));
    }

    /**
     * Checks the distrib admin page is hidden to any non-admin user.
     */
    @Test
    public void testDistribAdminPage() {
        readerHttpClient.buildGetRequest(DistribAdminPage.URL).execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testHomePageCurrentDistrib() {
        // since 20.0.0: does not redirect to current live distrib anymore, only available to admins
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT)
                        .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testHomePageLatestDistrib() {
        // persisted distrib redirection
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
                        .execute(new HtmlPageHandler<>(DistributionHomePage.builder()::build));
    }

    @Test
    public void testDeleteDistrib() {
        readerHttpClient.buildGetRequest(DistribAdminPage.DELETE_URL + '/' + SAMPLE_EXPORT_DISTRIBUTION_NAME + '-'
                + SAMPLE_EXPORT_DISTRIBUTION_VERSION).execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testSampleDistrib() {
        readerHttpClient.buildGetRequest(ExplorerHomePage.URL)
                        .execute(new HtmlPageHandler<>(
                                ExplorerHomePage.builder()
                                                .firstPersistedDistribution(SAMPLE_EXPORT_DISTRIBUTION_NAME,
                                                        SAMPLE_EXPORT_DISTRIBUTION_VERSION)::build));

        readerHttpClient.buildGetRequest(
                ExplorerHomePage.URL + '/' + SAMPLE_EXPORT_DISTRIBUTION_NAME + '-' + SAMPLE_EXPORT_DISTRIBUTION_VERSION)
                        .execute(new HtmlPageHandler<>(DistributionHomePage.builder()::build));
    }

}
