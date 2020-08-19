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
package org.nuxeo.functionaltests.explorer.nomode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.DistributionUpdatePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerDownloadTest;
import org.openqa.selenium.By;

/**
 * Test Explorer pages usually handled by admins.
 *
 * @since 11.1
 */
public class ITExplorerAdminTest extends AbstractExplorerDownloadTest {

    /**
     * Helper method to ease up adaptations of child classes methods.
     */
    protected boolean isAdminTest() {
        return true;
    }

    @Before
    public void before() {
        doLogin();
    }

    @After
    public void after() {
        doLogout();
        cleanupPersistedDistributions();
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
        open(ExplorerHomePage.URL);
        asPage(ExplorerHomePage.class).checkManageLink();
        open(DistribAdminPage.URL);
        DistribAdminPage page = asPage(DistribAdminPage.class);
        page.check();
        if (isAdminTest()) {
            page.checkCanSave();
        } else {
            // since 20.0.0: cannot save anymore
            page.checkCannotSave();
        }
    }

    @Test
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        if (isAdminTest()) {
            home.checkCurrentDistrib();
            UploadFragment.checkCanSee();
            checkHomeLiveDistrib();
        } else {
            // since 20.0.0: cannot see current live distrib anymore
            home.checkNoCurrentDistrib();
            UploadFragment.checkCanSee();
        }
    }

    protected String checkLiveDistribExport(String distribName, boolean fullCheck) {
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, false, false);
        String distribId = getDistribId(distribName, version);
        asPage(DistribAdminPage.class).checkPersistedDistrib(distribId);
        if (fullCheck) {
            checkDistrib(distribId, false, null, false, false);
        }
        return distribId;
    }

    protected void checkLiveDistribImport(String distribId) {
        // check importing it back
        open(DistribAdminPage.URL);
        String filename = getDistribExportName(distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        open(DistribAdminPage.URL);
        String newDistribName = "imported-server";
        String newVersion = "1.0.0";
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);
        // NXP-29154: check redirection to admin page
        asPage(DistribAdminPage.class);
        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(distribId, false, null, false, false);
    }

    @Test
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        String distribId = checkLiveDistribExport(distribName, true);
        checkLiveDistribImport(distribId);
    }

    protected String checkLivePartialDistribExport(String distribName, boolean includeReferences, boolean fullCheck) {
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, true, includeReferences);
        String distribId = getDistribId(distribName, version);
        asPage(DistribAdminPage.class).checkPersistedDistrib(distribId);
        if (fullCheck) {
            checkDistrib(distribId, true, distribName, includeReferences, false);
        }
        return distribId;
    }

    protected void checkLivePartialDistribImport(String distribName, String distribId) {
        // check importing it back
        open(DistribAdminPage.URL);
        String filename = getDistribExportName(distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        // import it from the home page this time
        open(ExplorerHomePage.URL);
        String newDistribName = "partial-imported-server";
        String newVersion = "1.0.0";
        asPage(ExplorerHomePage.class).importPersistedDistrib(file, newDistribName, newVersion, null);
        // NXP-29154: check redirection to home page
        asPage(ExplorerHomePage.class);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(distribId, true, distribName, false, false);
    }

    @Test
    public void testLivePartialDistribExportAndImport() {
        String distribName = "my-partial-server";
        String distribId = checkLivePartialDistribExport(distribName, false, true);
        checkLivePartialDistribImport(distribName, distribId);
    }

    @Test
    public void testLivePartialRefDistribExport() {
        checkLivePartialDistribExport("my-partial-ref-server", true, true);
    }

    /**
     * @implNote non-regression test for NXP-14948: sample export contains code from quota plugin
     */
    @Test
    public void testSampleDistribImport() throws IOException {
        File zip = createSampleZip(false);

        String newDistribName = "apidoc-imported";
        String newVersion = "1.0.0";
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).importPersistedDistrib(zip, newDistribName, newVersion,
                "Details: Not a valid Nuxeo Archive - no marker file found");

        // add the needed ".nuxeo-archive" file at the root of the zip and retry
        zip = createSampleZip(true);

        Locator.scrollAndForceClick(driver.findElement(By.linkText("RETRY")));
        asPage(DistribAdminPage.class).importPersistedDistrib(zip, newDistribName, newVersion, null);

        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(newDistribId, true, SAMPLE_BUNDLE_GROUP, false, true);

        // edit persisted distrib
        open(DistribAdminPage.URL);
        DistributionUpdatePage upage = asPage(DistribAdminPage.class).updateFirstPersistedDistrib();
        upage.check();
        upage.checkStringValue(newDistribName, upage.title);
        upage.checkStringValue(newDistribName, upage.name);
        upage.checkStringValue(newVersion, upage.version);
        upage.checkStringValue(newDistribId, upage.key);
        upage.checkStringValue(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), upage.released);
        upage.checkStringValue("", upage.aliases);
        upage.checkCheckBoxValue(false, upage.latestLTS);
        upage.checkCheckBoxValue(false, upage.latestFT);
        upage.checkCheckBoxValue(false, upage.hidden);

        // check validation on required field
        upage.updateString(upage.name, "");
        upage.submit();
        upage = asPage(DistributionUpdatePage.class);
        upage.checkErrorMessage("Please fill all required fields.");

        // check validation on reserved alias
        upage.updateString(upage.name, newDistribName);
        upage.updateString(upage.aliases, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT);
        upage.submit();
        upage = asPage(DistributionUpdatePage.class);
        upage.checkErrorMessage("Distribution key or alias is reserved: 'current'");

        String newerDistribName = "apidoc-imported-updated";
        String newerVersion = "2.1.0";
        String newerDistribId = getDistribId(newerDistribName, newerVersion);
        String alias1 = "alias1";
        String alias2 = "alias2";

        upage.updateString(upage.name, newerDistribName);
        upage.updateString(upage.version, newerVersion);
        upage.updateString(upage.key, newerDistribId);
        upage.updateString(upage.aliases, alias1 + "\n" + alias2);
        upage.submit();

        DistribAdminPage adminPage = asPage(DistribAdminPage.class);
        adminPage.checkSuccessMessage("Update Done.");
        adminPage.checkPersistedDistrib(newerDistribId);

        open(ExplorerHomePage.URL);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newerDistribId);

        // check aliases
        open(String.format("%s%s/", ExplorerHomePage.URL, alias1));
        asPage(DistributionHomePage.class).checkHeader(newerDistribId);
        open(String.format("%s%s/", ExplorerHomePage.URL, alias2));
        asPage(DistributionHomePage.class).checkHeader(newerDistribId);

        // check hiding distrib
        open(DistribAdminPage.UPDATE_URL + newerDistribId);
        upage = asPage(DistributionUpdatePage.class);
        upage.checkCheckBoxValue(false, upage.hidden);
        upage.updateCheckBox(upage.hidden, true);
        upage.submit();

        adminPage = asPage(DistribAdminPage.class);
        adminPage.checkSuccessMessage("Update Done.");
        adminPage.checkPersistedDistrib(newerDistribId);
        open(ExplorerHomePage.URL);
        asPage(ExplorerHomePage.class).checkPersistedDistribNotPresent(newDistribId);
        // check we can still navigate to it
        open(String.format("%s%s/", ExplorerHomePage.URL, newerDistribId));
        asPage(DistributionHomePage.class).checkHeader(newerDistribId);
        open(String.format("%s%s/", ExplorerHomePage.URL, alias1));
        asPage(DistributionHomePage.class).checkHeader(newerDistribId);
    }

    @Test
    public void testLiveDistribExportAndDelete() {
        String distribName = "my-server-to-delete";
        String distribId = checkLivePartialDistribExport(distribName, false, false);
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).deleteFirstPersistedDistrib();
        asPage(DistribAdminPage.class).checkPersistedDistribNotPresent(distribId);
    }

    @Test
    public void testJson() throws IOException {
        checkJson(SnapshotManager.DISTRIBUTION_ALIAS_CURRENT, !isAdminTest());
    }

}
