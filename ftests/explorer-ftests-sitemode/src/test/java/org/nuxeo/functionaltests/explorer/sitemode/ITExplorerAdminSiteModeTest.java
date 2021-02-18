/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.DistributionUpdatePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;

/**
 * Test Explorer pages usually handled by admins.
 *
 * @since 20.0.0
 */
public class ITExplorerAdminSiteModeTest extends AbstractExplorerSiteModeTest {

    @Before
    public void before() {
        doLogin();
    }

    @After
    public void after() {
        doLogout();
    }

    @Override
    protected void doLogin() {
        loginAsAdmin();
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() {
        goHome();
    }

    @Test
    public void testDistribAdminPage() {
        open(DistribAdminPage.URL);
        DistribAdminPage page = asPage(DistribAdminPage.class);
        page.check();
        page.checkCannotSave();
    }

    @Test
    public void testHomePageCurrentDistrib() {
        // since 20.0.0: cannot see current live distrib anymore
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT), true);
    }

    @Test
    public void testHomePageLatestDistrib() {
        open(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_LATEST));
        // persisted distrib redirection
        asPage(DistributionHomePage.class).check();
    }

    @Test
    public void testSampleDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        home.checkFirstPersistedDistrib(DISTRIB_NAME, DISTRIB_VERSION);
        UploadFragment.checkCannotSee();

        String distribId = getDistribId(DISTRIB_NAME, DISTRIB_VERSION);
        home.checkPersistedDistrib(distribId);
        checkDistrib(distribId, true, SAMPLE_BUNDLE_GROUP, false, true);
    }

    @Test
    public void testSampleDistribImport() throws IOException {
        try {
            File file = createSampleZip(true);
            String newVersion = "2.0.0";
            String newDistribName = "apidoc-site-mode-newer";
            open(DistribAdminPage.URL);
            asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);

            open(ExplorerHomePage.URL);
            String newDistribId = getDistribId(newDistribName, newVersion);
            asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
            checkDistrib(newDistribId, true, SAMPLE_BUNDLE_GROUP, false, true);

            // edit persisted distrib
            open(DistribAdminPage.UPDATE_URL + newDistribId);
            DistributionUpdatePage upage = asPage(DistributionUpdatePage.class);
            upage.check();

            String newerDistribName = newDistribName + "-updated";
            String newerDistribId = getDistribId(newerDistribName, newVersion);
            upage.updateString(upage.name, newerDistribName);
            upage.updateString(upage.key, newerDistribId);
            upage.updateString(upage.aliases, "alias");
            upage.submit();

            DistribAdminPage adminPage = asPage(DistribAdminPage.class);
            adminPage.checkSuccessMessage("Update Done.");
            adminPage.checkPersistedDistrib(newerDistribId);

            open(ExplorerHomePage.URL);
            asPage(ExplorerHomePage.class).checkPersistedDistrib(newerDistribId);
            open(String.format("%s%s/", ExplorerHomePage.URL, newerDistribId));
            asPage(DistributionHomePage.class).checkHeader(newerDistribId);
            open(String.format("%s%s/", ExplorerHomePage.URL, "alias"));
            asPage(DistributionHomePage.class).checkHeader(newerDistribId);
        } finally {
            // avoid conflict with testSample
            cleanupPersistedDistributions();
            // recreate the deleted sample, as expected by other tests
            doLogout();
            ITExplorerApidocManagerSiteModeTest.initPersistedDistrib();
        }
    }

    @Test
    public void testSampleDistribDelete() throws IOException {
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).deleteFirstPersistedDistrib();
        String distribId = getDistribId(DISTRIB_NAME, DISTRIB_VERSION);
        asPage(DistribAdminPage.class).checkPersistedDistribNotPresent(distribId);
        // recreate the deleted sample, as expected by other tests
        doLogout();
        ITExplorerApidocManagerSiteModeTest.initPersistedDistrib();
    }

}
