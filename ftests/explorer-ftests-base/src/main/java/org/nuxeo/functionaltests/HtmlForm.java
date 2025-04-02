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

import java.util.Optional;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.FormControl;
import net.htmlparser.jericho.FormField;
import net.htmlparser.jericho.HTMLElementName;

/**
 * @since 2025.0
 */
public class HtmlForm extends AbstractHtmlElement {

    public HtmlForm(Element element) {
        super(element);
    }

    @Override
    public void assertElement() {
        assertEquals(HTMLElementName.FORM, element.getStartTag().getName());
    }

    protected Optional<FormControl> findFormControlWithId(String id) {
        return findElementWithId(id).map(Element::getFormControl);
    }

    protected Optional<FormControl> findFormControlWithName(String name) {
        return Optional.ofNullable(element.getFormFields().get(name)).map(FormField::getFormControl);
    }

    public String getFormMethod() {
        return element.getAttributeValue("method");
    }

    public String getFormAction() {
        return element.getAttributeValue("action").replaceFirst("^/nuxeo", "");
    }
}
