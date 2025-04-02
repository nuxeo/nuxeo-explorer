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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class ComponentArtifactPage extends ArtifactPage<ComponentArtifactPage.Builder> {

    protected final List<String> aliases;

    protected final boolean resolutionOrderPresence;

    protected final boolean startOrderPresence;

    protected final Integer declaredStartOrder;

    protected final String implementation;

    protected final boolean xmlSourcePresence;

    public ComponentArtifactPage(Builder builder, Source html) {
        super(builder, html);
        aliases = this.findElementsWithNameAndClass(HTMLElementName.UL, "aliases")
                      .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                      .map(TEXT_EXTRACTOR)
                      .toList();
        resolutionOrderPresence = this.findElementsWithNameAndClass(HTMLElementName.DIV, "resolutionOrder")
                                      .map(TEXT_EXTRACTOR)
                                      .anyMatch(StringUtils::isNotBlank);
        startOrderPresence = this.findElementsWithNameAndClass(HTMLElementName.SPAN, "startOrder")
                                 .map(TEXT_EXTRACTOR)
                                 .anyMatch(StringUtils::isNotBlank);
        declaredStartOrder = this.findElementsWithNameAndClass(HTMLElementName.SPAN, "declaredStartOrder")
                                 .map(TEXT_EXTRACTOR)
                                 .map(Integer::parseInt)
                                 .findFirst()
                                 .orElse(null);
        implementation = this.findElementsWithNameAndClass(HTMLElementName.SPAN, "javadoc")
                             .map(TEXT_EXTRACTOR)
                             .findFirst()
                             .orElse(null);
        xmlSourcePresence = this.findElementWithId("xmlSource")
                                .map(TEXT_EXTRACTOR)
                                .filter(StringUtils::isNotBlank)
                                .isPresent();
    }

    public static Builder builder(String componentName, String bundleId) {
        return new Builder(componentName, bundleId);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedAliases, aliases);
        assertTrue("Resolution order presence", resolutionOrderPresence);
        assertEquals("Start order presence", builder.expectedStartOrderPresence, startOrderPresence);
        assertEquals(builder.expectedDeclaredStartOrder, declaredStartOrder);
        assertEquals(builder.expectedImplementation, implementation);
        assertEquals("XML Source presence", builder.expectedXmlSourcePresence, xmlSourcePresence);
    }

    // @Override
    // public void checkSelectedTab() {
    // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
    // header.checkSelectedTab(header.components);
    // }

    public static class Builder extends ArtifactPage.Builder<Builder> {

        protected List<String> expectedAliases = List.of();

        protected boolean expectedStartOrderPresence;

        protected Integer expectedDeclaredStartOrder;

        protected String expectedImplementation;

        protected boolean expectedXmlSourcePresence;

        public Builder(String expectedComponentName, String expectedBundleId) {
            super("Component " + expectedComponentName);
            contentInfoDescription("In bundle " + expectedBundleId);
        }

        public Builder aliases(String expectedAlias, String... expectedAliases) {
            return aliases(toList(expectedAlias, expectedAliases));
        }

        public Builder aliases(List<String> expectedAliases) {
            this.expectedAliases = List.copyOf(expectedAliases);
            return this;
        }

        public Builder startOrderPresence(boolean expectedStartOrderPresence) {
            this.expectedStartOrderPresence = expectedStartOrderPresence;
            return this;
        }

        public Builder declaredStartOrder(Integer expectedDeclaredStartOrder) {
            this.expectedDeclaredStartOrder = expectedDeclaredStartOrder;
            return this;
        }

        public Builder implementation(String expectedImplementation) {
            this.expectedImplementation = expectedImplementation;
            return this;
        }

        public Builder xmlSourcePresence(boolean expectedXmlSourcePresence) {
            this.expectedXmlSourcePresence = expectedXmlSourcePresence;
            return this;
        }

        public ComponentArtifactPage build(Source html) {
            return new ComponentArtifactPage(this, html);
        }
    }
}
