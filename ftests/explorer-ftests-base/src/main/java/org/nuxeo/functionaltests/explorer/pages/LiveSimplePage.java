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

import static org.junit.Assert.assertTrue;

import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.AbstractHtmlPage;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * Page representing home at /site/distribution.
 *
 * @since 11.1
 */
public class LiveSimplePage extends AbstractHtmlPage<LiveSimplePage.Builder> {

    public static final String URL = ExplorerHomePage.URL + '/' + SnapshotManager.DISTRIBUTION_ALIAS_ADM;

    protected final String header;

    protected final String statsContent;

    protected final ListingFragment bundles;

    protected LiveSimplePage(Builder builder, Source html) {
        super(builder, html);
        header = this.findFirstElementWithName(HTMLElementName.H1)
                     .map(TEXT_EXTRACTOR)
                     .orElseThrow(() -> new AssertionError("Unable to find the header"));
        statsContent = this.findElementWithId("stats")
                           .map(TEXT_EXTRACTOR)
                           .orElseThrow(() -> new AssertionError("Unable to find the stats"));
        bundles = withIdAs("contentTable", ListingFragment::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertTrue(header, header.startsWith("Your current Nuxeo Distribution is"));
        assertTrue(statsContent.contains("Number of Bundles"));
        assertTrue(statsContent.contains("Number of Components"));
        assertTrue(statsContent.contains("Number of Services"));
        assertTrue(statsContent.contains("Number of Services"));
        assertTrue(statsContent.contains("Number of Extension Points"));
        assertTrue(statsContent.contains("Number of Contributions"));
        assertTrue(statsContent.contains("Number of Operations"));
        assertTrue(statsContent.contains("Number of Packages"));
    }

    public ListingFragment getBundles() {
        return bundles;
    }

    public static class Builder extends AbstractHtmlPage.Builder<Builder> {

        public Builder() {
            super("Nuxeo Platform Explorer");
        }

        public LiveSimplePage build(Source html) {
            return new LiveSimplePage(this, html);
        }
    }
}
