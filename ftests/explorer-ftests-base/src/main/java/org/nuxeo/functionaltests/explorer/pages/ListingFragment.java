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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;

import org.nuxeo.functionaltests.AbstractHtmlElement;
import org.nuxeo.functionaltests.HtmlLink;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.TextExtractor;

/**
 * Represents a listing fragment on the explorer page.
 *
 * @since 11.1
 */
public class ListingFragment extends AbstractHtmlElement {

    protected final List<Row> rows;

    public ListingFragment(Element element) {
        super(element);
        // skip header
        this.rows = getElementsWithName(HTMLElementName.TR).stream().skip(1).map(Row::new).toList();
    }

    @Override
    public void assertElement() {
        assertEquals(HTMLElementName.TABLE, element.getStartTag().getName());
        assertEquals("contentTable", element.getAttributeValue("id"));
    }

    public void assertContainsRow(String linkText, String linkUrlEnd, String linkDetail) {
        var actualRow = findRowOrThrow(linkText, linkDetail);
        var link = actualRow.getLink();
        assertTrue(link.getHref(), link.getHref().endsWith(linkUrlEnd));
        assertEquals(linkDetail, actualRow.getDetailText());
    }

    public void assertContainsRow(ExpectedRow expectedRow) {
        assertContainsRow(expectedRow.text(), expectedRow.urlEnd(), expectedRow.itemDetail());
    }

    public Row getFirstRow() {
        return rows.getFirst();
    }

    public Row findRowOrThrow(String linkText) {
        return rows.stream()
                   .filter(row -> linkText.equals(row.getLink().getText()))
                   .findFirst()
                   .orElseThrow(() -> new AssertionError("Unable to find the row with link text: " + linkText));
    }

    public Row findRowOrThrow(String linkText, String linkDetail) {
        return rows.stream()
                   .filter(row -> linkText.equals(row.getLink().getText())
                           && Objects.equals(linkDetail, row.getDetailText()))
                   .findFirst()
                   .orElseThrow(() -> new AssertionError(
                           "Unable to find the row with link text: " + linkText + ", detail: " + linkDetail));
    }

    public List<Row> getRows() {
        return rows;
    }

    public void checkListing(int expectedSize, String firstLinkText, String firstLinkURLEnd, String firstLinkDetail) {
        if (expectedSize >= 0) {
            assertEquals(expectedSize, rows.size());
            if (expectedSize == 0) {
                return;
            }
        } else {
            assertFalse(rows.isEmpty());
        }
    }

    public static class Row extends AbstractHtmlElement {

        protected final HtmlLink link;

        protected final String detailText;

        protected Row(Element element) {
            super(element);
            this.link = this.findElementsWithNameAndClass(HTMLElementName.A, "itemLink")
                            .findFirst()
                            .map(HtmlLink::new)
                            .orElseThrow(() -> new AssertionError("Unable to find the link"));
            this.detailText = this.findElementsWithNameAndClass(HTMLElementName.DIV, "itemDetail")
                                  .findFirst()
                                  .map(Element::getTextExtractor)
                                  .map(TextExtractor::toString)
                                  .orElse(null);
        }

        @Override
        public void assertElement() {
            assertEquals(HTMLElementName.TR, element.getStartTag().getName());
        }

        public HtmlLink getLink() {
            return link;
        }

        public String getDetailText() {
            return detailText;
        }
    }

    public record ExpectedRow(String text, String urlEnd, String itemDetail) {
    }
}
