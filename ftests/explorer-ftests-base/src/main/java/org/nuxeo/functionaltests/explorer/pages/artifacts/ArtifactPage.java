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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.AbstractHtmlPage;
import org.nuxeo.functionaltests.explorer.ExplorerTestRule;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;

/**
 * Page representing a selected artifact.
 *
 * @since 11.1
 */
public abstract class ArtifactPage<B extends ArtifactPage.Builder<B>> extends AbstractHtmlPage<B> {

    protected final String contentInfoHeader;

    protected final String contentInfoDescription;

    protected final List<String> tableOfContents;

    protected final String documentation;

    protected final String documentationHtml;

    protected final List<String> requirements;

    protected ArtifactPage(B builder, Source html) {
        super(builder, html);
        Predicate<Element> contentInfoFilter = el -> "contentinfo".equals(el.getAttributeValue("role"));
        contentInfoHeader = this.getElementsWithName(HTMLElementName.ARTICLE)
                                .stream()
                                .filter(contentInfoFilter)
                                .map(el -> el.getFirstElement(HTMLElementName.H1))
                                .map(TEXT_EXTRACTOR)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Unable to find artifact header"));
        contentInfoDescription = this.getElementsWithName(HTMLElementName.ARTICLE)
                                     .stream()
                                     .filter(contentInfoFilter)
                                     .flatMap(el -> el.getAllElements(HTMLElementName.DIV).stream())
                                     .filter(el -> hasClass(el, "include-in"))
                                     .map(TEXT_EXTRACTOR)
                                     .findFirst()
                                     .orElse(null);
        tableOfContents = this.findElementsWithNameAndClass(HTMLElementName.H2, "toc").map(TEXT_EXTRACTOR).toList();
        documentation = this.findElementsWithNameAndClass(HTMLElementName.DIV, "documentation")
                            .findAny()
                            .stream()
                            .flatMap(div -> div.getAllElements(HTMLElementName.P).stream())
                            .map(TEXT_EXTRACTOR)
                            .filter(StringUtils::isNotBlank)
                            .reduce((p1, p2) -> String.join(System.lineSeparator(), p1, p2))
                            .orElse(null);
        documentationHtml = this.findElementsWithNameAndClass(HTMLElementName.DIV, "documentation")
                                .findAny()
                                // .map(el -> el.getChildElements().getFirst())
                                .map(el -> new SourceFormatter(el).setTidyTags(true).setCollapseWhiteSpace(true))
                                .map(Object::toString)
                                .orElse(null);
        requirements = this.findElementWithId("requirements")
                           .stream()
                           .flatMap(element -> element.getAllElements(HTMLElementName.LI).stream())
                           .map(TEXT_EXTRACTOR)
                           .toList();
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedContentInfoHeader, contentInfoHeader);
        assertEquals(builder.expectedContentInfoDescription, contentInfoDescription);
        assertEquals(builder.expectedTableOfContents, tableOfContents);
        if (builder.expectedDocumentation != null) {
            assertEquals(builder.expectedDocumentation, documentation);
        } else {
            assertEquals(builder.expectedDocumentationHtml, documentationHtml);
        }
        assertEquals(builder.expectedRequirements, requirements);
    }

    @SuppressWarnings("unchecked")
    public static class Builder<B extends Builder<B>> extends AbstractHtmlPage.Builder<B> {

        protected final String expectedContentInfoHeader;

        protected String expectedContentInfoDescription;

        protected List<String> expectedTableOfContents = List.of();

        protected String expectedDocumentation;

        protected String expectedDocumentationHtml;

        protected List<String> expectedRequirements = List.of();

        public Builder(String expectedTitle) {
            this(expectedTitle, expectedTitle);
        }

        public Builder(String expectedTitle, String expectedContentInfoHeader) {
            super(expectedTitle);
            this.expectedContentInfoHeader = expectedContentInfoHeader;
        }

        public B contentInfoDescription(String expectedContentInfoDescription) {
            this.expectedContentInfoDescription = expectedContentInfoDescription;
            return (B) this;
        }

        public B tableOfContents(String expectedTableOfContent, String... expectedTableOfContents) {
            return tableOfContents(toList(expectedTableOfContent, expectedTableOfContents));
        }

        public B tableOfContents(List<String> expectedTableOfContents) {
            this.expectedTableOfContents = List.copyOf(expectedTableOfContents);
            return (B) this;
        }

        /**
         * Assert the given documentation text against the retrieved page.
         * <p>
         * Only one method can be used among {@link #documentation(String)} or {@link #documentationHtml}.
         */
        public B documentation(String expectedDocumentation) {
            if (this.expectedDocumentationHtml != null) {
                throw new IllegalStateException("Only one of documentation and documentationHtml can be set");
            }
            this.expectedDocumentation = expectedDocumentation;
            return (B) this;
        }

        /**
         * Assert the given documentation HMTL against the retrieved page.
         * <p>
         * Only one method can be used among {@link #documentation(String)} or {@link #documentationHtml}.
         */
        public B documentationHtml(String expectedDocumentationHtml) {
            if (this.expectedDocumentation != null) {
                throw new IllegalStateException("Only one of documentation and documentationHtml can be set");
            }
            this.expectedDocumentationHtml = expectedDocumentationHtml;
            return (B) this;
        }

        public B documentationHtmlFromResource(String expectedResource) {
            try (var stream = ExplorerTestRule.getReferenceStream(expectedResource)) {
                assert stream != null;
                return documentationHtml(IOUtils.toString(stream, UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read expected file", e);
            }
        }

        public B requirements(String expectedRequirement, String... expectedRequirements) {
            return requirements(toList(expectedRequirement, expectedRequirements));
        }

        public B requirements(List<String> expectedRequirements) {
            this.expectedRequirements = List.copyOf(expectedRequirements);
            return (B) this;
        }
    }
}
