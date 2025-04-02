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

import java.io.IOException;
import java.util.function.Function;

import jakarta.ws.rs.core.MediaType;

import org.nuxeo.http.test.HttpResponse;
import org.nuxeo.http.test.handler.AbstractStatusCodeHandler;

import net.htmlparser.jericho.Source;

/**
 * @since 2025.0
 */
public class HtmlPageHandler<P extends AbstractHtmlPage> extends AbstractStatusCodeHandler<P> {

    protected final Function<Source, P> constructor;

    public HtmlPageHandler(Function<Source, P> constructor) {
        super();
        this.constructor = constructor;
    }

    public HtmlPageHandler(int status, Function<Source, P> constructor) {
        super(status);
        this.constructor = constructor;
    }

    @Override
    protected P doHandleResponse(HttpResponse response) throws IOException {
        String contentType = response.getType();
        if (contentType == null) {
            throw new AssertionError("HTTP Content-Type header is null, expected to start with:<text/html>");
        } else if (!contentType.startsWith(MediaType.TEXT_HTML)) {
            throw new AssertionError("HTTP Content-Type header mismatch, expected to start with:<text/html> but was:<"
                    + contentType + ">");
        }
        try (var stream = response.getEntityInputStream()) {
            Source source = new Source(stream);
            P page = constructor.apply(source);
            page.assertElement();
            return page;
        }
    }
}
