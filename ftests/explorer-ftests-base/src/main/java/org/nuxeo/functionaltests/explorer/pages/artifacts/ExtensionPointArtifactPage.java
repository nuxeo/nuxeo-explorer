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

import java.util.List;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class ExtensionPointArtifactPage extends ArtifactPage<ExtensionPointArtifactPage.Builder> {

    protected final List<String> aliases;

    protected final List<String> descriptors;

    public ExtensionPointArtifactPage(Builder builder, Source html) {
        super(builder, html);
        aliases = this.findElementsWithNameAndClass(HTMLElementName.UL, "aliases")
                      .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                      .map(TEXT_EXTRACTOR)
                      .toList();
        descriptors = this.findElementsWithNameAndClass(HTMLElementName.UL, "descriptors")
                          .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                          .map(TEXT_EXTRACTOR)
                          .toList();
    }

    public static Builder builder(String extensionTarget, String extensionPoint) {
        return new Builder(extensionTarget, extensionPoint);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedAliases, aliases);
        assertEquals(builder.expectedDescriptors, descriptors);
    }

    // @Override
    // public void checkAlternative() {
    // checkCommon("Extension point org.nuxeo.ecm.core.schema.TypeService--doctype", "Extension point doctype",
    // "In component org.nuxeo.ecm.core.schema.TypeService",
    // "Documentation\n" + "Contribution Descriptors\n" + "Existing Contributions");
    // checkDescriptorsText("org.nuxeo.ecm.core.schema.DocumentTypeDescriptor");
    // checkDescriptorsText("org.nuxeo.ecm.core.schema.FacetDescriptor");
    // checkDescriptorsText("org.nuxeo.ecm.core.schema.ProxiesDescriptor");
    // checkAliases(null);
    // }

    // @Override
    // public void checkSelectedTab() {
    // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
    // header.checkSelectedTab(header.extensionPoints);
    // }

    public static class Builder extends ArtifactPage.Builder<Builder> {

        protected List<String> expectedAliases = List.of();

        protected List<String> expectedDescriptors = List.of();

        public Builder(String expectedExtensionTarget, String expectedExtensionPoint) {
            super(String.format("Extension point %s--%s", expectedExtensionTarget, expectedExtensionPoint),
                    "Extension point " + expectedExtensionPoint);
            contentInfoDescription("In component " + expectedExtensionTarget);
        }

        public Builder aliases(String expectedAlias, String... expectedAliases) {
            return aliases(toList(expectedAlias, expectedAliases));
        }

        public Builder aliases(List<String> expectedAliases) {
            this.expectedAliases = List.copyOf(expectedAliases);
            return this;
        }

        public Builder descriptors(String expectedDescriptor, String... expectedDescriptors) {
            return descriptors(toList(expectedDescriptor, expectedDescriptors));
        }

        public Builder descriptors(List<String> expectedDescriptors) {
            this.expectedDescriptors = List.copyOf(expectedDescriptors);
            return this;
        }

        public ExtensionPointArtifactPage build(Source html) {
            return new ExtensionPointArtifactPage(this, html);
        }
    }
}
