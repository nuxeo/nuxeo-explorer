/*
 * (C) Copyright 2012-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = RedirectResource.TYPE)
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
public class RedirectResource extends DefaultObject {

    /** @since 20.0.0 */
    public static final String TYPE = "redirectWO";

    protected String orgDistributionId = null;

    protected String targetDistributionId = null;

    @Override
    protected void initialize(Object... args) {
        orgDistributionId = (String) args[0];
        targetDistributionId = (String) args[1];
        targetDistributionId = targetDistributionId.replace(" ", "%20");
    }

    @GET
    @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    public Object get() {
        return newLocation(targetDistributionId, null);
    }

    @GET
    @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Path("/{subPath:.*}")
    public Object catchAll(@PathParam("subPath") String subPath) {
        return newLocation(targetDistributionId, subPath);
    }

    protected Response newLocation(String target, String subPath) {
        String path = getPrevious().getPath();
        String url = ctx.getServerURL().append(path).append("/").append(target).toString();
        if (subPath != null) {
            url = url + "/" + subPath;
        }
        return redirect(url);
    }
}
