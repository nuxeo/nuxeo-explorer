/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.browse;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Handles 404 redirections.
 *
 * @since 20.0.0
 */
@WebObject(type = Resource404.TYPE)
public class Resource404 extends DefaultObject {

    public static final String TYPE = "error";

    @GET
    @Produces("text/html")
    public Object doGet() {
        return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_HTML_TYPE).entity(Resource404.getPageContent()).build();
    }

    public static String getPageContent() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("<html>");
        pw.println("<head><title>404 - Resource Not Found</title></head>");
        pw.println("<body>");
        pw.println("<h1>404 Resource Not Found</h1>");
        pw.println("<p>The resource you're trying to access couldn't be found.</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
        return sw.toString();
    }

}
