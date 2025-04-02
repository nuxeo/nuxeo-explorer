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

import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.nuxeo.common.function.ThrowableRunnable;
import org.nuxeo.functionaltests.HtmlForm;
import org.nuxeo.http.test.HttpClientTestRule;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.FormControl;
import net.htmlparser.jericho.FormControlType;

/**
 * Fragment for both main page and admin page, showing a similar upload form.
 *
 * @since 11.1
 */
public class UploadFragment extends HtmlForm {

    protected final FormControl input;

    protected final FormControl source;

    protected final FormControl upload;

    public UploadFragment(Element element) {
        super(element);
        input = this.findFormControlWithName("archive")
                    .orElseThrow(() -> new AssertionError("Unable to find the archive input"));
        source = this.findFormControlWithName("source")
                     .orElseThrow(() -> new AssertionError("Unable to find the source input"));
        upload = this.findFormControlWithId("upload")
                     .orElseThrow(() -> new AssertionError("Unable to find the upload button"));
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(FormControlType.FILE, input.getFormControlType());
        assertEquals(FormControlType.HIDDEN, source.getFormControlType());
        assertEquals(FormControlType.SUBMIT, upload.getFormControlType());
    }

    public HttpClientTestRule.RequestBuilder buildFormRequest(HttpClientTestRule httpClient, File file) {
        var entity = MultipartEntityBuilder.create()
                                           .addBinaryBody(input.getName(), file)
                                           .addTextBody(source.getName(), source.getValues().getFirst())
                                           .build();
        var builder = switch (getFormMethod()) {
            case "POST" -> httpClient.buildPostRequest(getFormAction());
            case "PUT" -> httpClient.buildPutRequest(getFormAction());
            default -> throw new AssertionError("Unrecognized form method: " + getFormMethod());
        };
        try {
            // don't use entity#getContent as it as a maximum content length limit
            var pipedInputStream = new PipedInputStream();
            new Thread(ThrowableRunnable.asRunnable(() -> {
                try (var pipedOutputStream = new PipedOutputStream(pipedInputStream)) {
                    entity.writeTo(pipedOutputStream);
                }
            })).start();
            return builder.contentType(entity.getContentType().getValue()).entity(pipedInputStream);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate multipart entity", e);
        }
    }
}
