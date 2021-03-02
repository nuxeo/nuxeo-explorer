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
package org.nuxeo.functionaltests.explorer.pages.artifacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class BundleArtifactPage extends ArtifactPage {

    /** @since 20.0.0 */
    @FindBy(xpath = "//div[@id='resolutionOrder']")
    public WebElement resolutionOrder;

    @Required
    @FindBy(xpath = "//table[@class='listTable']")
    public WebElement mavenDetails;

    @FindBy(xpath = "//ul[contains(@class, 'packages')]")
    public WebElement packages;

    @Required
    @FindBy(xpath = "//ul[@class='exports']")
    public WebElement exports;

    public BundleArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean includeReferences, boolean legacy) {
        String groupTitle = "In bundle group org.nuxeo.apidoc";
        String toc = "Documentation\n" //
                + "Components\n" //
                + "Packages\n" //
                + "Maven Artifact\n"//
                + "Manifest\n" //
                + "Exports\n" //
                + "Charts";
        if (partial) {
            groupTitle = "In bundle group my-partial-server";
        }
        if (includeReferences) {
            groupTitle = "In bundle group my-partial-ref-server";
        }
        if (legacy) {
            groupTitle = "In bundle group apidoc";
            // legacy does not hold packages
            toc = "Documentation\n" //
                    + "Components\n" //
                    + "Maven Artifact\n" //
                    + "Manifest\n" //
                    + "Exports\n" //
                    + "Charts";
        }
        checkCommon("Bundle org.nuxeo.apidoc.core", "Bundle org.nuxeo.apidoc.core", groupTitle, toc);
        try {
            String readmes = AbstractExplorerTest.getReferenceContent(
                    Paths.get(legacy ? "data/apidoc_core_readmes_legacy.html" : "data/apidoc_core_readmes.html"));
            checkDocumentationHTML(readmes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkGroupId("org.nuxeo.ecm.platform");
        checkArtifactId("nuxeo-apidoc-core");
        checkRequirements(null);
        checkResolutionOrder(false);
        checkPackages(legacy ? null : "platform-explorer");

        WebElement jsonExport = exports.findElement(By.linkText("Json Export"));
        checkJsonLink(jsonExport);
        WebElement graphExport = exports.findElement(By.linkText("Json Graph"));
        checkJsonLink(graphExport);
    }

    @Override
    public void checkAlternative() {
        checkCommon("Bundle org.nuxeo.apidoc.webengine", "Bundle org.nuxeo.apidoc.webengine",
                "In bundle group org.nuxeo.apidoc", "Documentation\n" //
                        + "Requirements\n" //
                        + "Components\n" //
                        + "Packages\n" //
                        + "Maven Artifact\n" //
                        + "Manifest\n" //
                        + "Exports\n" //
                        + "Charts");
        checkGroupId("org.nuxeo.ecm.platform");
        checkArtifactId("nuxeo-apidoc-webengine");
        checkRequirements(Arrays.asList("org.nuxeo.ecm.webengine.core", "org.nuxeo.apidoc.core"));
        checkResolutionOrder(false);
        checkPackages("platform-explorer");
    }

    public void checkAlternative2() {
        checkCommon("Bundle org.nuxeo.apidoc.repo", "Bundle org.nuxeo.apidoc.repo", "In bundle group org.nuxeo.apidoc",
                "Documentation\n" //
                        + "Resolution Order\n" //
                        + "Components\n" //
                        + "Packages\n" //
                        + "Maven Artifact\n" //
                        + "Manifest\n" //
                        + "Exports\n" //
                        + "Charts");
        checkGroupId("org.nuxeo.ecm.platform");
        checkArtifactId("nuxeo-apidoc-repo");
        checkRequirements(null);
        checkResolutionOrder(true);
        checkPackages("platform-explorer");
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.bundles);
    }

    public void checkGroupId(String id) {
        WebElement groupId = mavenDetails.findElement(By.xpath(".//tr[2]//td"));
        assertNotNull(groupId);
        assertEquals(id, groupId.getText());
    }

    public void checkArtifactId(String id) {
        WebElement artifactId = mavenDetails.findElement(By.xpath(".//tr[3]//td"));
        assertNotNull(artifactId);
        assertEquals(id, artifactId.getText());
    }

    /** @since 20.0.0 */
    public void checkResolutionOrder(boolean set) {
        checkSetIfExists(set, resolutionOrder);
    }

    public void checkPackages(String expected) {
        checkTextIfExists(expected, packages);
    }

}
