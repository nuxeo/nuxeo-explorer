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

import java.util.function.Function;

import net.htmlparser.jericho.Element;

/**
 * @since 2025.0
 */
public interface HtmlElement {

    Function<? super Element, String> TEXT_EXTRACTOR = element -> element.getTextExtractor().toString();

    /**
     * Assert the content of the segment, which is mainly a page or an element.
     */
    void assertElement();

    /**
     * Retrieves the element with given id within this element and return it as {@link HtmlElement}.
     */
    <H extends HtmlElement> H withIdAs(String id, Function<? super Element, H> constructor);

    /**
     * Retrieves the first element with given name and class within this element and return it as {@link HtmlElement}.
     */
    <H extends HtmlElement> H firstWithNameAndClassAs(String name, String classAttribute,
            Function<? super Element, H> constructor);
}
