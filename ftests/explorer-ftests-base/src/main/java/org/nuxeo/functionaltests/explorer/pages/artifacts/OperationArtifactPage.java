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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.HtmlTable;
import org.nuxeo.functionaltests.HtmlTable.ExpectedRow;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class OperationArtifactPage extends ArtifactPage<OperationArtifactPage.Builder> {

    protected final String description;

    protected final HtmlTable info;

    protected final HtmlTable parameters;

    protected final HtmlTable signature;

    protected final String implementation;

    protected final String contributingComponent;

    protected final String json;

    public OperationArtifactPage(Builder builder, Source html) {
        super(builder, html);
        description = this.findElementsWithNameAndClass(HTMLElementName.DIV, "description")
                          .map(TEXT_EXTRACTOR)
                          .findFirst()
                          .orElse(null);
        info = this.findElementsWithNameAndClass(HTMLElementName.DIV, "info")
                   .findFirst()
                   .map(el -> el.getFirstElement(HTMLElementName.TABLE))
                   .map(HtmlTable::new)
                   .orElseThrow(() -> new AssertionError("Unable to find the operation info"));
        parameters = this.findElementsWithNameAndClass(HTMLElementName.DIV, "parameters")
                         .findFirst()
                         .map(el -> el.getFirstElement(HTMLElementName.TABLE))
                         .map(HtmlTable::new)
                         .orElse(null);
        signature = this.findElementsWithNameAndClass(HTMLElementName.DIV, "signature")
                        .findFirst()
                        .map(el -> el.getFirstElement(HTMLElementName.TABLE))
                        .map(HtmlTable::new)
                        .orElseThrow(() -> new AssertionError("Unable to find the operation signature"));
        implementation = this.findElementsWithNameAndClass(HTMLElementName.SPAN, "javadoc")
                             .map(TEXT_EXTRACTOR)
                             .findFirst()
                             .orElseThrow(() -> new AssertionError("Unable to find the implementation"));
        contributingComponent = this.findElementsWithNameAndClass(HTMLElementName.TR, "contributingComponent")
                                    .map(el -> el.getFirstElement(HTMLElementName.TD))
                                    .map(TEXT_EXTRACTOR)
                                    .findFirst()
                                    .orElseThrow(() -> new AssertionError("Unable to find the contributing component"));
        json = this.findElementsWithNameAndClass(HTMLElementName.DIV, "json")
                   .map(TEXT_EXTRACTOR)
                   .findFirst()
                   .orElse(null);
    }

    public static Builder builder(String operationId, String operationLabel, String componentName) {
        return new Builder(operationId, operationLabel, componentName);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedDescription, description);
        assertEquals(builder.expectedId, info.getCell(0, 1).getText());
        assertEquals(builder.expectedCategory, info.getCell(1, 1).getText());
        assertEquals(builder.expectedLabel, info.getCell(2, 1).getText());
        // TODO requires / since?
        if (parameters == null) {
            assertTrue("No operation parameters found", builder.containsOperationParameters.isEmpty());
        } else {
            builder.containsOperationParameters.forEach(parameters::assertContainsRow);
        }
        assertEquals(builder.expectedInputSignature, signature.getCell(0, 1).getText());
        assertEquals(builder.expectedOutputSignature, signature.getCell(1, 1).getText());
        assertEquals(builder.expectedImplementation, implementation);
        assertEquals(builder.expectedComponentName, contributingComponent);
    }

    // @Override
    // public void checkSelectedTab() {
    // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
    // header.checkSelectedTab(header.operations);
    // }

    // field positions are given by data position in the html page and not based on their final modifier
    public static class Builder extends ArtifactPage.Builder<Builder> {

        protected String expectedDescription;

        protected final String expectedId;

        protected final String expectedLabel;

        protected String expectedCategory;

        protected final List<ExpectedRow> containsOperationParameters = new ArrayList<>();

        protected String expectedInputSignature;

        protected String expectedOutputSignature;

        protected String expectedImplementation;

        protected final String expectedComponentName;

        protected String expectedJson;

        public Builder(String expectedId, String expectedLabel, String expectedComponentName) {
            super("Operation " + expectedId, String.format("Operation %s (%s)", expectedId, expectedLabel));
            contentInfoDescription("In component " + expectedComponentName);
            this.expectedId = expectedId;
            this.expectedLabel = expectedLabel;
            this.expectedComponentName = expectedComponentName;
        }

        public Builder description(String expectedDescription) {
            this.expectedDescription = expectedDescription;
            return this;
        }

        public Builder additionalInfo(String expectedCategory) {
            this.expectedCategory = expectedCategory;
            return this;
        }

        public Builder hasParameterRow(String... columns) {
            containsOperationParameters.add(new ExpectedRow(columns));
            return this;
        }

        public Builder signature(String expectedInputSignature, String expectedOutputSignature) {
            this.expectedInputSignature = expectedInputSignature;
            this.expectedOutputSignature = expectedOutputSignature;
            return this;
        }

        public Builder implementation(String expectedImplementation) {
            this.expectedImplementation = expectedImplementation;
            return this;
        }

        public Builder json(String expectedJson) {
            this.expectedJson = expectedJson;
            return this;
        }

        public OperationArtifactPage build(Source html) {
            return new OperationArtifactPage(this, html);
        }
    }
}
