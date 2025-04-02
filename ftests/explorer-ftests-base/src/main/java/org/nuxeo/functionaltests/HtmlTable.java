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
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;

/**
 * @since 2025.0
 */
public class HtmlTable extends AbstractHtmlElement {

    protected final List<Row> rows;

    public HtmlTable(Element element) {
        super(element);
        rows = this.findElementsWithName(HTMLElementName.TR).map(Row::new).toList();
    }

    @Override
    public void assertElement() {
        assertEquals(HTMLElementName.TABLE, element.getStartTag().getName());
    }

    public void assertContainsRow(ExpectedRow expectedRow) {
        if (rows.stream()
                .noneMatch(row -> Objects.equals(
                        new ExpectedRow(row.getColumns().stream().map(Column::getText).toArray(String[]::new)),
                        expectedRow))) {
            throw new AssertionError("Unable to find the expected row " + expectedRow);
        }
    }

    public void assertDoesNotContainRow(ExpectedRow expectedRow) {
        if (rows.stream()
                .anyMatch(row -> Objects.equals(
                        new ExpectedRow(row.getColumns().stream().map(Column::getText).toArray(String[]::new)),
                        expectedRow))) {
            throw new AssertionError("Able to find the expected row " + expectedRow);
        }
    }

    public Row getRow(int index) {
        if (index >= rows.size()) {
            throw new AssertionError("Unable to find the row with index: " + index);
        }
        return rows.get(index);
    }

    public List<Row> getRows() {
        return rows;
    }

    public Column getCell(int rowIndex, int columnIndex) {
        return getRow(rowIndex).getColumn(columnIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("rowSize", rows.size()).build();
    }

    public static class Row extends AbstractHtmlElement {

        protected final List<Column> columns;

        public Row(Element element) {
            super(element);
            columns = element.getChildElements().stream().map(Column::new).toList();
        }

        @Override
        public void assertElement() {
            assertEquals(HTMLElementName.TR, element.getStartTag().getName());
        }

        public Column getColumn(int index) {
            if (index >= columns.size()) {
                throw new AssertionError("Unable to find the column with index: " + index);
            }
            return columns.get(index);
        }

        public List<Column> getColumns() {
            return columns;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("columnSize", columns.size()).build();
        }
    }

    public static class Column extends AbstractHtmlElement {

        public Column(Element element) {
            super(element);
        }

        @Override
        public void assertElement() {
            assertTrue("The table column is not a td/th",
                    Set.of(HTMLElementName.TD, HTMLElementName.TH).contains(element.getStartTag().getName()));
        }

        public String getText() {
            return TEXT_EXTRACTOR.apply(element);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("text", getText()).build();
        }
    }

    /**
     * @param columns the expected columns, use {@code null} to discard equals assertion
     */
    public record ExpectedRow(String... columns) {

        @Override
        public boolean equals(Object other) {
            if (other instanceof ExpectedRow(String[] otherColumns)) {
                if (columns.length != otherColumns.length) {
                    return false;
                }
                for (int i = 0; i < columns.length; i++) {
                    var column = columns[i];
                    var otherColumn = otherColumns[i];
                    if (column == null || otherColumn == null) {
                        continue; // discard equals assertion
                    } else if (!column.equals(otherColumn)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(columns);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("columns", columns).build();
        }
    }
}
