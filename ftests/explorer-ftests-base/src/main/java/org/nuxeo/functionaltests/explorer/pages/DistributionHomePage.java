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
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.AbstractHtmlPage;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class DistributionHomePage extends AbstractHtmlPage<DistributionHomePage.Builder> {

    protected final String header;

    public DistributionHomePage(Builder builder, Source html) {
        super(builder, html);
        header = this.findFirstElementWithName(HTMLElementName.H1)
                     .map(TEXT_EXTRACTOR)
                     .orElseThrow(() -> new AssertionError("Unable to find the header"));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void assertElement() {
        super.assertElement();
        if (StringUtils.isNotBlank(builder.expectedHeader)) {
            assertEquals(builder.expectedHeader, header);
        } else {
            assertTrue(header, header.startsWith("Browsing Distribution"));
        }
    }

    public static class Builder extends AbstractHtmlPage.Builder<DistributionHomePage.Builder> {

        protected String expectedHeader;

        public Builder() {
            super("Nuxeo Platform Explorer");
        }

        public Builder header(String expectedHeader) {
            this.expectedHeader = expectedHeader;
            return this;
        }

        public DistributionHomePage build(Source html) {
            return new DistributionHomePage(this, html);
        }
    }
}
