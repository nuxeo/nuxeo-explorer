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

import org.apache.commons.lang3.builder.ToStringBuilder;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;

/**
 * @since 2025.0
 */
public class HtmlLink extends AbstractHtmlElement {

    public HtmlLink(Element element) {
        super(element);
    }

    @Override
    public void assertElement() {
        assertEquals(HTMLElementName.A, element.getStartTag().getName());
    }

    public String getText() {
        return element.getTextExtractor().toString();
    }

    public String getHref() {
        return element.getAttributeValue("href");
    }

    public String getHrefAndStrip(String path) {
        return getHref().replaceAll("^(?:/nuxeo)?" + path, "");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("href", getHref()).append("text", getText()).build();
    }
}
