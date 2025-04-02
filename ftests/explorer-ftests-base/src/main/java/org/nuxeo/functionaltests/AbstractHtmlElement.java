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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import net.htmlparser.jericho.Element;

/**
 * @since 2025.0
 */
public abstract class AbstractHtmlElement implements HtmlElement {

    protected final Element element;

    protected AbstractHtmlElement(Element element) {
        this.element = Objects.requireNonNull(element, "Element must not be null");
    }

    @Override
    public <H extends HtmlElement> H withIdAs(String id, Function<? super Element, H> constructor) {
        return findElementWithId(id).map(constructor)
                                    .orElseThrow(() -> new AssertionError("Unable to find element with id: " + id));
    }

    @Override
    public <H extends HtmlElement> H firstWithNameAndClassAs(String name, String classAttribute,
            Function<? super Element, H> constructor) {
        return this.findElementsWithNameAndClass(name, classAttribute)
                   .map(constructor)
                   .findFirst()
                   .orElseThrow(() -> new AssertionError(
                           "Unable to find first element with name: " + name + ", class: " + classAttribute));
    }

    protected Optional<Element> findElementWithId(String id) {
        return Optional.ofNullable(element.getFirstElement("id", id, true));
    }

    protected Optional<Element> findFirstElementWithName(String name) {
        return Optional.ofNullable(element.getFirstElement(name));
    }

    protected Element getFirstElementWithName(String name) {
        return element.getFirstElement(name);
    }

    protected List<Element> getElementsWithName(String name) {
        return element.getAllElements(name);
    }

    protected Stream<Element> findElementsWithName(String name) {
        return element.getAllElements(name).stream();
    }

    protected Stream<Element> findElementsWithNameAndClass(String name, String classAttribute) {
        return this.findElementsWithName(name).filter(el -> hasClass(el, classAttribute));
    }

    protected static boolean hasClass(Element element, String classAttribute) {
        return List.of(StringUtils.defaultString(element.getAttributeValue("class")).split(" "))
                   .contains(classAttribute);
    }
}
