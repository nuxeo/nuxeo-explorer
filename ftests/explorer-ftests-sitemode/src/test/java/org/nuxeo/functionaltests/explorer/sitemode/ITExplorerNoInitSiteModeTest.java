/*
 * (C) Copyright 2014-2020 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.MANAGER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.READER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.TEST_PASSWORD;

import jakarta.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.HtmlPageHandler;
import org.nuxeo.functionaltests.explorer.ExplorerTestRule;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.LiveSimplePage;
import org.nuxeo.http.test.HttpClientTestRule;

/**
 * Checks access to some explorer pages in site mode without any persisted distributions.
 *
 * @since 20.0.0
 */
public class ITExplorerNoInitSiteModeTest {

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

    @Rule
    public final HttpClientTestRule readerHttpClient = HttpClientTestRule.builder()
                                                                         .credentials(READER_USERNAME, TEST_PASSWORD)
                                                                         .accept(MediaType.TEXT_HTML)
                                                                         .build();

    @Before
    public void before() {
        explorerHelper.createManagerUser();
        explorerHelper.createReaderUser();
    }

    protected void checkPagesSiteMode(HttpClientTestRule httpClient, boolean isAdmin, boolean isManager) {
        httpClient.buildGetRequest(ExplorerHomePage.URL)
                  .execute(new HtmlPageHandler<>(
                          ExplorerHomePage.builder()
                                          .currentDistribution(false)
                                          .manageDistributionsPresence(isAdmin || isManager)::build));

        httpClient.buildGetRequest(LiveSimplePage.URL).execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT)
                  .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT + '/'
                + ApiBrowserConstants.JSON_ACTION)
                  .accept(MediaType.APPLICATION_JSON)
                  .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
                  .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_LATEST + '/'
                + ApiBrowserConstants.JSON_ACTION)
                  .accept(MediaType.APPLICATION_JSON)
                  .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + "/foo-10.10").execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + "/foo-10.10/" + ApiBrowserConstants.JSON_ACTION)
                  .accept(MediaType.APPLICATION_JSON)
                  .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        httpClient.buildGetRequest(ExplorerHomePage.URL + '/' + ApiBrowserConstants.LIST_COMPONENTS + "/foo-10.10")
                  .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }

    @Test
    public void testPagesByAdmin() {
        adminHttpClient.buildGetRequest(DistribAdminPage.URL)
                       .execute(new HtmlPageHandler<>(DistribAdminPage.builder().savePresence(false)::build));
        checkPagesSiteMode(adminHttpClient, true, true);
    }

    @Test
    public void testPagesByManager() {
        managerHttpClient.buildGetRequest(DistribAdminPage.URL)
                         .execute(new HtmlPageHandler<>(DistribAdminPage.builder()::build));
        checkPagesSiteMode(managerHttpClient, false, true);
    }

    @Test
    public void testPagesByReader() {
        readerHttpClient.buildGetRequest(DistribAdminPage.URL).execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        checkPagesSiteMode(readerHttpClient, false, false);
    }

}
