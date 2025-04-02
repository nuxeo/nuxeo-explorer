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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.HtmlTable;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class BundleArtifactPage extends ArtifactPage<BundleArtifactPage.Builder> {

    protected final HtmlTable mavenDetails;

    /** @since 20.0.0 */
    protected final boolean resolutionOrderPresence;

    protected final String packages;

    protected BundleArtifactPage(Builder builder, Source html) {
        super(builder, html);
        mavenDetails = this.findElementsWithNameAndClass(HTMLElementName.TABLE, "listTable")
                           .findFirst()
                           .map(HtmlTable::new)
                           .orElseThrow(() -> new AssertionError("Unable to find the maven details"));
        resolutionOrderPresence = this.findElementWithId("resolutionOrder")
                                      .map(TEXT_EXTRACTOR)
                                      .filter(StringUtils::isNotBlank)
                                      .isPresent();
        packages = this.findElementsWithNameAndClass(HTMLElementName.UL, "packages")
                       .map(TEXT_EXTRACTOR)
                       .findFirst()
                       .orElse(null);
    }

    public static Builder builder(String bundleGroupId, String bundleId) {
        return new Builder(bundleGroupId, bundleId);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        var mavenGroupId = mavenDetails.getCell(1, 1).getText();
        var mavenArtifactId = mavenDetails.getCell(2, 1).getText();
        assertEquals(builder.expectedMavenGroupId, mavenGroupId);
        assertEquals(builder.expectedMavenArtifactId, mavenArtifactId);
        assertEquals("Resolution order presence", builder.expectedResolutionOrderPresence, resolutionOrderPresence);
        assertEquals(builder.expectedPackages, packages);
    }

    public static class Builder extends ArtifactPage.Builder<Builder> {

        protected String expectedMavenGroupId;

        protected String expectedMavenArtifactId;

        protected boolean expectedResolutionOrderPresence;

        protected String expectedPackages;

        public Builder(String expectedBundleGroupId, String expectedBundleId) {
            super("Bundle " + expectedBundleId);
            contentInfoDescription("In bundle group " + expectedBundleGroupId);
        }

        public Builder mavenArtifact(String expectedMavenGroupId, String expectedMavenArtifactId) {
            this.expectedMavenGroupId = expectedMavenGroupId;
            this.expectedMavenArtifactId = expectedMavenArtifactId;
            return this;
        }

        public Builder resolutionOrderPresence(boolean expectedResolutionOrderPresence) {
            this.expectedResolutionOrderPresence = expectedResolutionOrderPresence;
            return this;
        }

        public Builder packages(String expectedPackages) {
            this.expectedPackages = expectedPackages;
            return this;
        }

        public BundleArtifactPage build(Source html) {
            return new BundleArtifactPage(this, html);
        }
    }
}
