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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.browse.Distribution;
import org.nuxeo.functionaltests.AbstractHtmlPage;
import org.nuxeo.functionaltests.HtmlTable;
import org.nuxeo.functionaltests.HtmlTable.ExpectedRow;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * Page representing the administration of distributions
 *
 * @since 11.1
 */
public class DistribAdminPage extends AbstractHtmlPage<DistribAdminPage.Builder> {

    public static final String URL = ExplorerHomePage.URL + '/' + Distribution.VIEW_ADMIN;

    /** @since 20.0.0 */
    public static final String UPDATE_URL = ExplorerHomePage.URL + '/' + Distribution.UPDATE_ACTION;

    /** @since 20.0.0 */
    public static final String DELETE_URL = ExplorerHomePage.URL + '/' + Distribution.DELETE_ACTION;

    /** @since 2025 */
    public static final String SAVE_URL = ExplorerHomePage.URL + '/' + Distribution.SAVE_ACTION;

    /** @since 2025 */
    public static final String SAVE_EXTENDED_URL = ExplorerHomePage.URL + '/' + Distribution.SAVE_EXTENDED_ACTION;

    protected static final String DUPLICATE_KEY_CLASS = "duplicateKey";

    protected final List<String> duplicateKeys;

    protected final boolean savePresence;

    protected final boolean savePartialPresence;

    protected final boolean stdFormPresence;

    protected final boolean extendedFormPresence;

    protected final HtmlTable distributions;

    protected final UploadFragment uploadFragment;

    public DistribAdminPage(Builder builder, Source html) {
        super(builder, html);
        duplicateKeys = this.findElementsWithNameAndClass(HTMLElementName.DIV, DUPLICATE_KEY_CLASS)
                            .map(TEXT_EXTRACTOR)
                            .toList();
        savePresence = this.findElementWithId("save").isPresent();
        savePartialPresence = this.findElementWithId("savePartial").isPresent();
        stdFormPresence = this.findElementWithId("stdSave").isPresent();
        extendedFormPresence = this.findElementWithId("extendedSave").isPresent();
        distributions = this.firstWithNameAndClassAs(HTMLElementName.TABLE, "distributions", HtmlTable::new);
        uploadFragment = this.firstWithNameAndClassAs(HTMLElementName.FORM, "upload-fragment", UploadFragment::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedDuplicateKeys, duplicateKeys);
        assertEquals(builder.expectedSavePresence, savePresence);
        assertEquals(builder.expectedSavePresence, savePartialPresence);
        assertEquals(builder.expectedSavePresence, stdFormPresence);
        assertEquals(builder.expectedSavePresence, extendedFormPresence);
        builder.containsDistributions.forEach(distributions::assertContainsRow);
        builder.doesNotContainDistributions.forEach(distributions::assertDoesNotContainRow);
    }

    public HtmlTable getDistributions() {
        return distributions;
    }

    public UploadFragment getUploadFragment() {
        return uploadFragment;
    }

    public String getLiveVersion() {
        return this.findElementWithId("stdSave")
                   .map(element -> element.getFirstElement("name", "version", true))
                   .map(TEXT_EXTRACTOR)
                   .orElseThrow(() -> new AssertionError("Unable to find the distribution version"));
    }

    public static class Builder extends AbstractHtmlPage.Builder<Builder> {

        protected List<String> expectedDuplicateKeys = List.of();

        protected boolean expectedSavePresence;

        protected final List<ExpectedRow> containsDistributions = new ArrayList<>();

        protected final List<ExpectedRow> doesNotContainDistributions = new ArrayList<>();

        public Builder() {
            super("Nuxeo Platform Explorer");
        }

        public Builder duplicateKeys(String expectedDuplicateKey, String... expectedDuplicateKeys) {
            return duplicateKeys(toList(expectedDuplicateKey, expectedDuplicateKeys));
        }

        public Builder duplicateKeys(List<String> expectedDuplicateKeys) {
            this.expectedDuplicateKeys = List.copyOf(expectedDuplicateKeys);
            return this;
        }

        public Builder savePresence(boolean expectedSavePresence) {
            this.expectedSavePresence = expectedSavePresence;
            return this;
        }

        public Builder hasDistribution(String distributionId, String version) {
            this.containsDistributions.add(new ExpectedRow(distributionId, null, version, null, null, null, null));
            return this;
        }

        public Builder hasNotDistribution(String distributionId, String version) {
            this.doesNotContainDistributions.add(
                    new ExpectedRow(distributionId, null, version, null, null, null, null));
            return this;
        }

        public DistribAdminPage build(Source html) {
            return new DistribAdminPage(this, html);
        }
    }
}
