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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.nuxeo.common.function.ThrowableConsumer;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @param <B> The builder type to build the expected page
 * @since 2025.0
 */
public abstract class AbstractHtmlPage<B extends AbstractHtmlPage.Builder<B>> extends AbstractHtmlElement {

    protected final B builder;

    protected final String title;

    protected final List<Meta> metas;

    public AbstractHtmlPage(B builder, Source html) {
        super(html.getFirstElement(HTMLElementName.HTML));
        this.builder = builder;
        this.title = TEXT_EXTRACTOR.apply(getFirstElementWithName(HTMLElementName.TITLE));
        this.metas = findElementsWithName(HTMLElementName.META).filter(m -> m.getAttributeValue("name") != null)
                                                               .map(m -> new Meta(m.getAttributeValue("name"),
                                                                       m.getAttributeValue("content")))
                                                               .toList();
    }

    @Override
    public void assertElement() {
        assertEquals(builder.expectedTitle, title);
        builder.containsMetas.forEach(m -> assertTrue("Unable to find: " + m, metas.contains(m)));
        FieldUtils.getAllFieldsList(getClass()).forEach(ThrowableConsumer.asConsumer(field -> {
            if (!Modifier.isStatic(field.getModifiers())) {
                if (field.trySetAccessible()) {
                    if (field.get(this) instanceof HtmlElement htmlElement) {
                        try {
                            htmlElement.assertElement();
                        } catch (AssertionError e) {
                            e.addSuppressed(new RuntimeException("Assertion of field: " + field.getName() + " failed"));
                        }
                    }
                } else {
                    throw new AssertionError("Unable to set accessible field: " + field);
                }
            }
        }));
    }

    public record Meta(String name, String content) {
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<B extends Builder<B>> {

        protected final String expectedTitle;

        protected final List<Meta> containsMetas = new ArrayList<>();

        public Builder(String expectedTitle) {
            this.expectedTitle = expectedTitle;
        }

        public B hasMeta(String name, String content) {
            containsMetas.add(new Meta(name, content));
            return (B) this;
        }

        // helper methods

        protected <E> List<E> toList(E element, E... elements) {
            var list = new ArrayList<E>();
            list.add(element);
            list.addAll(List.of(elements));
            return list;
        }
    }
}
