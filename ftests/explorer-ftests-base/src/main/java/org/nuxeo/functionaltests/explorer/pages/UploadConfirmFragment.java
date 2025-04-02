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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.HtmlForm;
import org.nuxeo.http.test.HttpClientTestRule;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.FormControl;
import net.htmlparser.jericho.FormControlType;

/**
 * Fragment upload confirmation.
 *
 * @since 11.1
 */
public class UploadConfirmFragment extends HtmlForm {

    protected final FormControl nameInput;

    protected final FormControl versionInput;

    protected final FormControl keyInput;

    protected final FormControl titleInput;

    protected final FormControl distributionDocIdInput;

    protected final FormControl sourceInput;

    protected final FormControl importButton;

    public UploadConfirmFragment(Element element) {
        super(element);
        nameInput = this.findFormControlWithName("nxdistribution:name")
                        .orElseThrow(() -> new AssertionError("Unable to find the name input"));
        versionInput = this.findFormControlWithName("nxdistribution:version")
                           .orElseThrow(() -> new AssertionError("Unable to find the version input"));
        keyInput = this.findFormControlWithName("nxdistribution:key")
                       .orElseThrow(() -> new AssertionError("Unable to find the key input"));
        titleInput = this.findFormControlWithName("dc:title")
                         .orElseThrow(() -> new AssertionError("Unable to find the key input"));
        distributionDocIdInput = this.findFormControlWithName("distribDocId")
                                     .orElseThrow(() -> new AssertionError("Unable to find the distribDocId input"));
        sourceInput = this.findFormControlWithName("source")
                          .orElseThrow(() -> new AssertionError("Unable to find the source input"));
        importButton = this.findFormControlWithId("doImport")
                           .orElseThrow(() -> new AssertionError("Unable to find the import button"));
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(FormControlType.TEXT, nameInput.getFormControlType());
        assertEquals(FormControlType.TEXT, versionInput.getFormControlType());
        assertEquals(FormControlType.TEXT, keyInput.getFormControlType());
        assertEquals(FormControlType.TEXT, titleInput.getFormControlType());
        assertEquals(FormControlType.HIDDEN, distributionDocIdInput.getFormControlType());
        assertEquals(FormControlType.HIDDEN, sourceInput.getFormControlType());
        assertEquals(FormControlType.SUBMIT, importButton.getFormControlType());
    }

    public HttpClientTestRule.RequestBuilder buildFormRequest(HttpClientTestRule httpClient, String newName,
            String newVersion) {
        var builder = switch (getFormMethod()) {
            case "POST" -> httpClient.buildPostRequest(getFormAction());
            case "PUT" -> httpClient.buildPutRequest(getFormAction());
            default -> throw new AssertionError("Unrecognized form method: " + getFormMethod());
        };
        String nameValue = StringUtils.defaultIfBlank(newName, nameInput.getPredefinedValue());
        String versionValue = StringUtils.defaultIfBlank(newVersion, versionInput.getPredefinedValue());
        return builder.entity(Map.of( //
                nameInput.getName(), nameValue, //
                versionInput.getName(), versionValue, //
                keyInput.getName(), nameValue + '-' + versionValue, //
                titleInput.getName(), nameValue, // just for display in tests
                distributionDocIdInput.getName(), distributionDocIdInput.getValues().getFirst(), //
                sourceInput.getName(), sourceInput.getValues().getFirst()));
    }
}
