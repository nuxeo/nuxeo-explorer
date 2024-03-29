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
package org.nuxeo.functionaltests.explorer.nomode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.nuxeo.apidoc.browse.Distribution;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.StatsPage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleGroupArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ComponentArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.OperationArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ServiceArtifactPage;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerTest;

/**
 * Test explorer webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerTest extends AbstractExplorerTest {

    protected static final String LIVE_NAME = "Nuxeo Platform";

    protected static String liveVersion;

    /**
     * Since 20.0.0, the live distrib can only be seen by admins --> init one for tests
     */
    @BeforeClass
    public static void initPersistedDistrib() {
        loginAsAdmin();
        open(DistribAdminPage.URL);
        liveVersion = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(LIVE_NAME, false, false);
        doLogout();
    }

    @AfterClass
    public static void cleanupPersistedDistrib() {
        cleanupPersistedDistributions();
    }

    @Before
    public void before() {
        RestHelper.createUserIfDoesNotExist(READER_USERNAME, TEST_PASSWORD, null, null, null, null, null);
        doLogin();
    }

    @After
    public void after() {
        doLogout();
        RestHelper.cleanup();
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() {
        goHome();
    }

    /**
     * Checks the distrib admin page is hidden to any non-admin user.
     */
    @Test
    public void testDistribAdminPage() {
        openAndCheck(DistribAdminPage.URL, true);
    }

    @Test
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        home.checkNoCurrentDistrib();
        UploadFragment.checkCannotSee();
    }

    @Test
    public void testHomePageCurrentDistrib() {
        // since 20.0.0: does not redirect to current live distrib anymore, only available to admins
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT), true);
    }

    @Test
    public void testHomePageLatestDistrib() {
        // since 20.0.0: does not redirect to current live distrib anymore, only to first persisted distrib named "nuxeo
        // platform" and alike (if it exists)
        open(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_LATEST));
        asPage(DistributionHomePage.class).check();
    }

    @Test
    public void testUpdateLatestDistrib() {
        openAndCheck(DistribAdminPage.UPDATE_URL + SnapshotManager.DISTRIBUTION_ALIAS_LATEST, true);
        openAndCheck(DistribAdminPage.UPDATE_URL + getDistribId(LIVE_NAME, liveVersion), true);
    }

    @Test
    public void testDeleteDistrib() {
        openAndCheck(DistribAdminPage.DELETE_URL + getDistribId(LIVE_NAME, liveVersion), true);
    }

    /**
     * Non-regression test for NXP-29193.
     */
    @Test
    public void testHomePageInvalidDistrib() {
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, "foo-10.10"), true);
    }

    protected String getArtifactURL(String type, String id) {
        return getArtifactURL(type, id, getDistribId(LIVE_NAME, liveVersion));
    }

    protected void goToArtifact(String type, String id) {
        open(getArtifactURL(type, id));
    }

    @Test
    public void testExtensionPoints() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstExtensionPoints);
        checkExtensionPoints(false, false, false);
    }

    @Test
    public void testExtensionPointsAlternative() {
        goToArtifact(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.schema.TypeService--doctype");
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testExtensionPointAliases() {
        openAndCheck(getArtifactURL(ExtensionPointInfo.TYPE_NAME, "foo"), true);
        goToArtifact(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.scheduler.SchedulerService--schedule");
        asPage(ExtensionPointArtifactPage.class).checkSchedulerServiceSchedule();
        goToArtifact(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService--schedule");
        asPage(ExtensionPointArtifactPage.class).checkSchedulerServiceSchedule();
    }

    @Test
    public void testContributions() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstContributions);
        checkContributions(false, false, false);
    }

    @Test
    public void testContributionsAlternative() {
        goToArtifact(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener");
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testServices() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstServices);
        checkServices(false, false, false);
    }

    @Test
    public void testServicesAlternative() {
        goToArtifact(ServiceInfo.TYPE_NAME, "org.nuxeo.ecm.platform.types.TypeManager");
        ServiceArtifactPage apage = asPage(ServiceArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testOperations() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstOperations);
        checkOperations(false, false);
    }

    @Test
    public void testOperationsAlternative() {
        goToArtifact(OperationInfo.TYPE_NAME, "FileManager.ImportWithMetaData");
        OperationArtifactPage apage = asPage(OperationArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testComponents() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header = header.navigateTo(header.components);
        header.checkSelectedTab(header.components);
        checkComponents(false, false, false);
    }

    @Test
    public void testComponentsAlternative() {
        goToArtifact(ComponentInfo.TYPE_NAME, "org.nuxeo.ecm.automation.server.marshallers");
        ComponentArtifactPage apage = asPage(ComponentArtifactPage.class);
        apage.checkAlternative();
    }

    protected void checkOverridePage(String url, String referenceFilePath) {
        driver.get(NUXEO_URL + url);
        try {
            assertEquals(AbstractExplorerTest.getReferenceContent(Paths.get(referenceFilePath)),
                    driver.getPageSource());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        // perform home navigation after, to avoid LogTestWatchMan failing on taking a screenshot of this XML page...
        driver.get(NUXEO_URL);
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
        String url = String.format("%s%s/%s/%s/override", ExplorerHomePage.URL, distribId,
                ApiBrowserConstants.VIEW_COMPONENT, componentId);
        String filterUrl = String.format("%s?contributionId=%s--%s", url, componentId, "exporters");
        checkOverridePage(url, "data/override_component_reference.xml");
        checkOverridePage(filterUrl, "data/override_contribution_reference.xml");
    }

    @Test
    public void testComponentAliases() {
        openAndCheck(getArtifactURL(ComponentInfo.TYPE_NAME, "foo"), true);
        goToArtifact(ComponentInfo.TYPE_NAME, "org.nuxeo.ecm.core.scheduler.SchedulerService");
        asPage(ComponentArtifactPage.class).checkSchedulerComponent();
        goToArtifact(ComponentInfo.TYPE_NAME, "org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService");
        asPage(ComponentArtifactPage.class).checkSchedulerComponent();
    }

    @Test
    public void testBundles() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header = header.navigateTo(header.bundles);
        header.checkSelectedTab(header.bundles);
        checkBundles(false, false, false);
    }

    @Test
    public void testBundlesAlternative() {
        goToArtifact(BundleInfo.TYPE_NAME, "org.nuxeo.apidoc.webengine");
        BundleArtifactPage apage = asPage(BundleArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testBundlesAlternative2() {
        goToArtifact(BundleInfo.TYPE_NAME, "org.nuxeo.apidoc.repo");
        BundleArtifactPage apage = asPage(BundleArtifactPage.class);
        apage.checkAlternative2();
    }

    @Test
    public void testBundleGroups() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstPersistedDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.clickOn(dhome.bundleGroups);
        checkBundleGroups(false, null, false, false);
    }

    @Test
    public void testBundleGroupsAlternative() {
        // check subgroup
        goToArtifact(BundleGroup.TYPE_NAME, "org.nuxeo.ecm.directory");
        BundleGroupArtifactPage apage = asPage(BundleGroupArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testPackages() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.firstPersistedDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.clickOn(dhome.packages);
        checkPackages(false, false);
    }

    @Test
    public void testOverrideContribution() throws IOException {
        goToArtifact(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener");
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.toggleGenerateOverride();
        storeWindowHandle();
        apage.doGenerateOverride();
        switchToNewWindow();
        String expected = AbstractExplorerTest.getReferenceContent(Paths.get("data/override_reference.xml"));
        assertEquals(expected, driver.getPageSource());
        switchBackToPreviousWindow();
    }

    /**
     * Non-regression test for NXP-29820.
     *
     * @since 20.2.0
     */
    @Test
    public void testOverrideContributionGetURL() {
        String contribUrl = getArtifactURL(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener");
        open(contribUrl + "/override");
        checkOverridePage(contribUrl + "/override", "data/override_reference.xml");
    }

    /**
     * Non-regression test for NXP-19766.
     */
    @Test
    public void testOverrideContributionWithXMLComments() {
        goToArtifact(ExtensionInfo.TYPE_NAME, "org.nuxeo.ecm.platform.comment.defaultPermissions--permissions");
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.toggleGenerateOverride();
        storeWindowHandle();
        apage.doGenerateOverride();
        switchToNewWindow();
        String generatedXML = driver.getPageSource();
        assertNotNull(generatedXML);
        assertFalse(generatedXML.contains("parsererror"));
        switchBackToPreviousWindow();
    }

    @Test
    public void testOverrideContributionFromExtensionPoint() throws IOException {
        goToArtifact(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.event.EventServiceComponent--listener");
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        storeWindowHandle();
        apage.generateOverride("org.nuxeo.apidoc.listener.contrib--listener");
        switchToNewWindow();
        String expected = AbstractExplorerTest.getReferenceContent(Paths.get("data/override_xp_reference.xml"));
        assertEquals(expected, driver.getPageSource());
        switchBackToPreviousWindow();
    }

    /**
     * Non-regression test for NXP-29378.
     */
    @Test
    public void testPseudoComponent() {
        goToArtifact(ComponentInfo.TYPE_NAME, "org.nuxeo.runtime.started");
        ComponentArtifactPage apage = asPage(ComponentArtifactPage.class);
        apage.checkCommon("Component org.nuxeo.runtime.started", "Component org.nuxeo.runtime.started",
                "In bundle org.nuxeo.runtime", "Resolution Order");
        apage.checkRequirements(null);
        apage.checkDocumentationText(null);
        apage.checkImplementationText(null);
        apage.checkJavadocLink(null);
        apage.checkResolutionOrder(true);
        apage.checkStartOrder(false);
        apage.checkDeclaredStartOrder(null);
        apage.checkXMLSource(false);
    }

    @Test
    public void testJson() {
        try {
            // setup page load timeout of 3 mins as persisted export can take time (default: 1 min)
            driver.manage().timeouts().pageLoadTimeout(180, TimeUnit.SECONDS);
            checkJson(getDistribId(LIVE_NAME, liveVersion), false);
        } finally {
            driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
        }
    }

    /** @since 22 */
    @Test
    public void testPackagesJson() throws IOException {
        String url = String.format("%s%s%s/%s/", NUXEO_URL, ExplorerHomePage.URL, getDistribId(LIVE_NAME, liveVersion),
                ApiBrowserConstants.LIST_PACKAGES);
        assertEquals("{\"packages\":[\"platform-explorer\"]}",
                getJsonContent(READER_USERNAME, TEST_PASSWORD, url, false));
    }

    @Test
    public void testInvalidArtifactPages() {
        openAndCheck(getArtifactURL(BundleGroup.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(BundleInfo.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(ComponentInfo.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(ExtensionInfo.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(ExtensionPointInfo.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(ServiceInfo.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(PackageInfo.TYPE_NAME, "foo"), true);
        openAndCheck(getArtifactURL(OperationInfo.TYPE_NAME, "foo"), true);
    }

    /** @since 22 */
    @Test
    public void testStatsPage() throws IOException {
        open(String.format("%s%s", ExplorerHomePage.URL, Distribution.VIEW_STATS));
        asPage(StatsPage.class).check();
    }

}
