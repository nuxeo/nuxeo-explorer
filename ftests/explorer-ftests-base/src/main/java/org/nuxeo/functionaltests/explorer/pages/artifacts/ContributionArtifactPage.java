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
package org.nuxeo.functionaltests.explorer.pages.artifacts;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class ContributionArtifactPage extends ArtifactPage<ContributionArtifactPage.Builder> {

    protected final boolean registrationOrderPresence;

    public ContributionArtifactPage(Builder builder, Source html) {
        super(builder, html);
        registrationOrderPresence = this.findElementsWithNameAndClass(HTMLElementName.DIV, "registrationOrder")
                                        .map(TEXT_EXTRACTOR)
                                        .anyMatch(StringUtils::isNotBlank);
    }

    public static Builder builder(String componentName, String extensionPoint) {
        return new Builder(componentName, extensionPoint);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertTrue("Registration order presence", registrationOrderPresence);
    }

    // @Override
    // public void checkSelectedTab() {
    // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
    // header.checkSelectedTab(header.contributions);
    // }

    public static class Builder extends ArtifactPage.Builder<Builder> {

        public Builder(String expectedComponentName, String expectedExtensionPoint) {
            super(String.format("Contribution %s--%s", expectedComponentName, expectedExtensionPoint));
            contentInfoDescription("In component " + expectedComponentName);
            tableOfContents("Documentation", "Extension Point", "Registration Order", "Contributed Items",
                    "XML Source");
        }

        public ContributionArtifactPage build(Source html) {
            return new ContributionArtifactPage(this, html);
        }
    }
}
