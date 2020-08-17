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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerTest;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class BundleArtifactPage extends ArtifactPage {

    /** @since 20.0.0 */
    @FindBy(xpath = "//div[@id='registrationOrder']")
    public WebElement registrationOrder;

    @Required
    @FindBy(xpath = "//table[@class='listTable']")
    public WebElement mavenDetails;

    @FindBy(xpath = "//ul[contains(@class, 'packages')]")
    public WebElement packages;

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
                + "Manifest";
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
                    + "Manifest";
        }
        checkCommon("Bundle org.nuxeo.apidoc.core", "Bundle org.nuxeo.apidoc.core", groupTitle, toc);
        try {
            String readme = AbstractExplorerTest.getReferenceContent(
                    Paths.get(legacy ? "data/core_readme_legacy.txt" : "data/core_readme.txt"));
            String parentReadme = AbstractExplorerTest.getReferenceContent(Paths.get("data/apidoc_readme.txt"));
            checkDocumentationText("ReadMe.md\n" + readme + "\nParent Documentation: ReadMe.md\n" + parentReadme);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkGroupId("org.nuxeo.ecm.platform");
        checkArtifactId("nuxeo-apidoc-core");
        checkRequirements(null);
        checkRegistrationOrder(false);
        checkPackages(legacy ? null : "platform-explorer");
    }

    @Override
    public void checkAlternative() {
        checkCommon("Bundle org.nuxeo.apidoc.webengine", "Bundle org.nuxeo.apidoc.webengine",
                "In bundle group org.nuxeo.apidoc", "Documentation\n" //
                        + "Requirements\n" //
                        + "Components\n" //
                        + "Packages\n" //
                        + "Maven Artifact\n" //
                        + "Manifest");
        checkGroupId("org.nuxeo.ecm.platform");
        checkArtifactId("nuxeo-apidoc-webengine");
        checkRequirements(List.of("org.nuxeo.ecm.webengine.core", "org.nuxeo.apidoc.core"));
        checkRegistrationOrder(false);
        checkPackages("platform-explorer");
    }

    public void checkAlternative2() {
        checkCommon("Bundle org.nuxeo.apidoc.repo", "Bundle org.nuxeo.apidoc.repo", "In bundle group org.nuxeo.apidoc",
                "Documentation\n" //
                        + "Registration Order\n" //
                        + "Components\n" //
                        + "Packages\n" //
                        + "Maven Artifact\n" //
                        + "Manifest");
        checkGroupId("org.nuxeo.ecm.platform");
        checkArtifactId("nuxeo-apidoc-repo");
        checkRequirements(null);
        checkRegistrationOrder(true);
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
    public void checkRegistrationOrder(boolean set) {
        try {
            assertEquals(!set, StringUtils.isBlank(registrationOrder.getText()));
        } catch (NoSuchElementException e) {
            assertFalse(set);
        }
    }

    public void checkPackages(String expected) {
        checkTextIfExists(expected, packages);
    }

}
