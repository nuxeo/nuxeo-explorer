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
package org.nuxeo.functionaltests.explorer.pages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.AbstractHtmlPage;
import org.nuxeo.functionaltests.HtmlLink;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * Page representing home at /site/distribution.
 *
 * @since 11.1
 */
public class ExplorerHomePage extends AbstractHtmlPage<ExplorerHomePage.Builder> {

    public static final String URL = "/site/distribution";

    protected final String currentPlatform;

    protected final HtmlLink currentDistrib;

    protected final HtmlLink firstPersistedDistrib;

    protected final UploadFragment uploadFragment;

    protected final HtmlLink manageDistribs;

    protected ExplorerHomePage(Builder builder, Source html) {
        super(builder, html);
        currentPlatform = this.findElementsWithNameAndClass(HTMLElementName.SPAN, "current")
                              .map(TEXT_EXTRACTOR)
                              .findFirst()
                              .orElse(null);
        currentDistrib = this.findElementsWithNameAndClass(HTMLElementName.A, "currentDistrib")
                             .map(HtmlLink::new)
                             .findFirst()
                             .orElse(null);
        firstPersistedDistrib = this.findElementsWithNameAndClass(HTMLElementName.A, "distrib")
                                    .map(HtmlLink::new)
                                    .findFirst()
                                    .orElse(null);
        uploadFragment = this.findElementsWithNameAndClass(HTMLElementName.FORM, "upload-fragment")
                             .map(UploadFragment::new)
                             .findFirst()
                             .orElse(null);
        manageDistribs = this.findElementsWithNameAndClass(HTMLElementName.A, "manageDistributions")
                             .map(HtmlLink::new)
                             .findFirst()
                             .orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void assertElement() {
        super.assertElement();
        if (builder.expectedCurrentDistribution) {
            assertEquals("Running Platform", currentPlatform);
            assertEquals('/' + SnapshotManager.DISTRIBUTION_ALIAS_CURRENT + '/', currentDistrib.getHrefAndStrip(URL));
        } else {
            assertNull(currentPlatform);
            assertNull(currentDistrib);
        }
        if (StringUtils.isNotBlank(builder.expectedFirstPersistedDistributionName)) {
            assertEquals(builder.expectedFirstPersistedDistributionName + ' '
                    + builder.expectedFirstPersistedDistributionVersion, firstPersistedDistrib.getText());
            assertEquals(
                    '/' + builder.expectedFirstPersistedDistributionName + '-'
                            + builder.expectedFirstPersistedDistributionVersion + '/',
                    firstPersistedDistrib.getHrefAndStrip(URL));
        } else {
            assertNull(firstPersistedDistrib);
        }
        if (builder.expectedUploadFragmentPresence) {
            assertNotNull(uploadFragment);
        } else {
            assertNull(uploadFragment);
        }
        if (builder.expectedManageDistributionsPresence) {
            assertNotNull(manageDistribs);
        } else {
            assertNull(manageDistribs);
        }
    }

    public UploadFragment getUploadFragment() {
        return uploadFragment;
    }

    public static class Builder extends AbstractHtmlPage.Builder<Builder> {

        protected boolean expectedCurrentDistribution;

        protected String expectedFirstPersistedDistributionName;

        protected String expectedFirstPersistedDistributionVersion;

        protected boolean expectedUploadFragmentPresence;

        protected boolean expectedManageDistributionsPresence;

        public Builder() {
            super("Nuxeo Platform Explorer");
        }

        public Builder currentDistribution(boolean expectedCurrentDistribution) {
            this.expectedCurrentDistribution = expectedCurrentDistribution;
            return this;
        }

        public Builder firstPersistedDistribution(String expectedFirstPersistedDistributionName,
                String expectedFirstPersistedDistributionVersion) {
            this.expectedFirstPersistedDistributionName = expectedFirstPersistedDistributionName;
            this.expectedFirstPersistedDistributionVersion = expectedFirstPersistedDistributionVersion;
            return this;
        }

        public Builder uploadFragmentPresence(boolean expectedUploadFragmentPresence) {
            this.expectedUploadFragmentPresence = expectedUploadFragmentPresence;
            return this;
        }

        public Builder manageDistributionsPresence(boolean expectedManageDistributionsPresence) {
            this.expectedManageDistributionsPresence = expectedManageDistributionsPresence;
            return this;
        }

        public ExplorerHomePage build(Source html) {
            return new ExplorerHomePage(this, html);
        }
    }
}
