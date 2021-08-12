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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class ComponentArtifactPage extends ArtifactPage {

    @FindBy(css = ".javadoc")
    public WebElement javadoc;

    @FindBy(css = ".resolutionOrder")
    public WebElement resolutionOrder;

    @FindBy(css = ".startOrder")
    public WebElement startOrder;

    @FindBy(css = ".declaredStartOrder")
    public WebElement declaredStartOrder;

    @FindBy(xpath = "//div[@id='xmlSource']")
    public WebElement xmlSource;

    public ComponentArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean includeReferences, boolean legacy) {
        String toc = "Documentation\n" + "Resolution Order\n" + "Start Order\n" + "Implementation\n" + "Services\n"
                + "Extension Points\n" + "Contributions\n" + "XML Source";
        if (legacy) {
            toc = "Documentation\n" + "Implementation\n" + "Services\n" + "Extension Points\n" + "XML Source";
        }
        checkCommon("Component org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                "Component org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", "In bundle org.nuxeo.apidoc.repo", toc);
        checkRequirements(null);
        checkDocumentationText(
                "This component handles the introspection of the current live Runtime as a distribution.\n" //
                        + "It can also persist this introspection as Nuxeo documents, to handle import and export of external distributions.");
        checkImplementationText("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");
        checkResolutionOrder(!legacy);
        checkStartOrder(!legacy);
        checkDeclaredStartOrder(null);
        checkXMLSource(true);
        checkAliases(null);
    }

    @Override
    public void checkAlternative() {
        checkCommon("Component org.nuxeo.ecm.automation.server.marshallers",
                "Component org.nuxeo.ecm.automation.server.marshallers", "In bundle org.nuxeo.ecm.automation.io",
                "Requirements\n" + "Resolution Order\n" + "Contributions\n" + "XML Source");
        checkRequirements(List.of("org.nuxeo.ecm.platform.contentview.json.marshallers"));
        checkDocumentationText(null);
        checkImplementationText(null);
        checkJavadocLink(null);
        checkResolutionOrder(true);
        checkStartOrder(false);
        checkDeclaredStartOrder(null);
        checkXMLSource(true);
        checkAliases(null);
    }

    public void checkVersioningComponent() {
        checkCommon("Component org.nuxeo.ecm.core.api.versioning.VersioningService",
                "Component org.nuxeo.ecm.core.api.versioning.VersioningService", "In bundle org.nuxeo.ecm.core",
                "Documentation\n" + "Aliases\n" + "Resolution Order\n" + "Start Order\n" + "Implementation\n"
                        + "Services\n" + "Extension Points\n" + "XML Source");
        checkRequirements(null);
        checkImplementationText("org.nuxeo.ecm.core.versioning.VersioningComponent");
        checkResolutionOrder(true);
        checkStartOrder(true);
        checkDeclaredStartOrder(null);
        checkXMLSource(true);
        checkAliases(List.of("org.nuxeo.ecm.core.versioning.VersioningService"));
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.components);
    }

    public void checkImplementationText(String expected) {
        checkTextIfExists(expected, javadoc);
    }

    public void checkJavadocLink(String expected) {
        checkLink(expected, javadoc);
    }

    public void checkResolutionOrder(boolean set) {
        checkSetIfExists(set, resolutionOrder);
    }

    public void checkStartOrder(boolean set) {
        checkSetIfExists(set, startOrder);
    }

    public void checkDeclaredStartOrder(Long value) {
        checkTextIfExists(value != null ? value.toString() : null, declaredStartOrder);
    }

    /** @since 20.0.0 */
    public void checkXMLSource(boolean set) {
        try {
            assertEquals(!set, StringUtils.isBlank(xmlSource.getText()));
        } catch (NoSuchElementException e) {
            assertFalse(set);
        }
    }

}
