/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.functionaltests.explorer.pages.artifacts;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.AbstractHtmlPage;
import org.nuxeo.functionaltests.explorer.pages.ListingFragment;
import org.nuxeo.functionaltests.explorer.pages.ListingFragment.ExpectedRow;

import net.htmlparser.jericho.Source;

/**
 * @since 2025.0
 */
public class ArtifactsPage extends AbstractHtmlPage<ArtifactsPage.Builder> {

    protected final ListingFragment artifacts;

    protected ArtifactsPage(Builder builder, Source html) {
        super(builder, html);
        this.artifacts = withIdAs("contentTable", ListingFragment::new);
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        builder.containsArtifacts.forEach(artifacts::assertContainsRow);
    }

    public ListingFragment getArtifacts() {
        return artifacts;
    }

    public static class Builder extends AbstractHtmlPage.Builder<Builder> {

        protected final List<ExpectedRow> containsArtifacts = new ArrayList<>();

        public Builder(String expectedTitle) {
            super(expectedTitle);
        }

        public Builder hasArtifactRow(String expectedLinkText, String expectedLinkUrlEnd) {
            containsArtifacts.add(new ExpectedRow(expectedLinkText, expectedLinkUrlEnd, null));
            return this;
        }

        public Builder hasArtifactRow(String expectedLinkText, String expectedLinkUrlEnd, String expectedItemDetail) {
            containsArtifacts.add(new ExpectedRow(expectedLinkText, expectedLinkUrlEnd, expectedItemDetail));
            return this;
        }

        public ArtifactsPage build(Source html) {
            return new ArtifactsPage(this, html);
        }
    }
}
