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

import org.junit.After;
import org.junit.Before;
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
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.ListingFragment;
import org.nuxeo.functionaltests.explorer.pages.LiveSimplePage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerTest;

/**
 * Test explorer "adm" "simple" webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerSimpleTest extends AbstractExplorerTest {

    @Before
    public void before() {
        // since 20.0.0: need to be an admin to browse "simple" pages
        loginAsAdmin();
    }

    @After
    public void after() {
        doLogout();
        RestHelper.cleanup();
    }

    @Override
    protected boolean hasNavigationHeader() {
        return false;
    }

    /**
     * Non-regression test for NXP-28911.
     */
    @Test
    public void testLiveDistributionSimplePage() {
        open(LiveSimplePage.URL);
        LiveSimplePage distrib = asPage(LiveSimplePage.class);
        distrib.check();

        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "org.nuxeo.admin.center", "/viewBundle/org.nuxeo.admin.center", null);
        listing = listing.filterOn("org.nuxeo.apidoc");
        listing.checkListing(3, "org.nuxeo.apidoc.core", "/viewBundle/org.nuxeo.apidoc.core", null);

        listing.navigateToFirstItem();
        asPage(BundleArtifactPage.class).check();

        open(LiveSimplePage.URL + "viewBundle/org.nuxeo.apidoc.core");
        asPage(BundleArtifactPage.class).check();
    }

    @Test
    public void testExtensionPoints() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_EXTENSIONPOINTS);
        checkExtensionPoints(false, false, false);
    }

    @Test
    public void testContributions() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_CONTRIBUTIONS);
        checkContributions(false, false, false);
    }

    @Test
    public void testServices() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_SERVICES);
        checkServices(false, false);
    }

    @Test
    public void testOperations() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_OPERATIONS);
        checkOperations(false, false);
    }

    @Test
    public void testComponents() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_COMPONENTS);
        checkComponents(false, false, false);
    }

    @Test
    public void testBundles() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_BUNDLES);
        checkBundles(false, false, false);
    }

    @Test
    public void testBundlesGroups() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_BUNDLEGROUPS);
        checkBundleGroups(false, null, false, false);
    }

    @Test
    public void testPackages() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_PACKAGES);
        checkPackages(false, false);
    }

    protected String getArtifactURL(String type, String id) {
        return getArtifactURL(type, "foo", SnapshotManager.DISTRIBUTION_ALIAS_ADM);
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

}
