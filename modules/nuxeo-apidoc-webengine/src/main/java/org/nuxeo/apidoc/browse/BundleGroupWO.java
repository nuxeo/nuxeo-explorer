/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.introspection.EmbeddedDocExtractor;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "bundleGroup")
public class BundleGroupWO extends NuxeoArtifactWebObject<BundleGroup> {

    protected BundleGroup getTargetBundleGroup() {
        return getSnapshot().getBundleGroup(nxArtifactId);
    }

    @Override
    public BundleGroup getNxArtifact() {
        if (nxArtifact == null) {
            nxArtifact = getTargetBundleGroup();
        }
        return nxArtifact;
    }

    @Produces(MediaType.TEXT_HTML)
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        t.arg("readmes",
                getNxArtifact().getReadmes()
                               .stream()
                               .map(EmbeddedDocExtractor::getHtmlFromMarkdown)
                               .collect(Collectors.toList()));
        return t;
    }

    public List<BundleWO> getBundles() {
        return getNxArtifact().getBundleIds()
                              .stream()
                              .map(bid -> (BundleWO) ctx.newObject("bundle", bid))
                              .collect(Collectors.toList());
    }

    public List<BundleGroupWO> getSubGroups() {
        return getNxArtifact().getSubGroups()
                              .stream()
                              .map(bg -> (BundleGroupWO) ctx.newObject("bundleGroup", bg.getId()))
                              .collect(Collectors.toList());
    }

}
