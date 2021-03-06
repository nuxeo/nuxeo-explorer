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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.export.api.Exporter;
import org.nuxeo.apidoc.introspection.EmbeddedDocExtractor;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "bundle")
public class BundleWO extends NuxeoArtifactWebObject<BundleInfo> {

    protected BundleInfo getTargetBundleInfo() {
        return getSnapshot().getBundle(nxArtifactId);
    }

    @Override
    public BundleInfo getNxArtifact() {
        if (nxArtifact == null) {
            nxArtifact = getTargetBundleInfo();
        }
        return nxArtifact;
    }

    @Produces(MediaType.TEXT_HTML)
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        BundleInfo bundle = getNxArtifact();
        t.arg("readme", EmbeddedDocExtractor.getHtmlFromMarkdown(bundle.getReadme()));
        t.arg("parentReadme", EmbeddedDocExtractor.getHtmlFromMarkdown(bundle.getParentReadme()));
        List<Exporter> allExporters = getSnapshotManager().getExporters();
        List<Exporter> exporters = allExporters.stream()
                                               .filter(e -> e.displayOn("bundle"))
                                               .collect(Collectors.toList());
        t.arg("exporters", exporters);
        t.arg("exporterCharts", exporters.stream().filter(e -> e.displayOn("chart")).collect(Collectors.toList()));
        t.arg("requirements", getRequirementsInfo(getSnapshot(), getNxArtifact().getRequirements()));
        return t;
    }

    protected Map<String, BundleInfo> getRequirementsInfo(DistributionSnapshot snapshot, List<String> requirements) {
        var res = new LinkedHashMap<String, BundleInfo>();
        requirements.forEach(req -> res.put(req, snapshot.getBundle(req)));
        return res;
    }

    protected class ComponentInfoSorter implements Comparator<ComponentInfo> {
        @Override
        public int compare(ComponentInfo ci0, ComponentInfo ci1) {

            if (ci0.isXmlPureComponent() && !ci1.isXmlPureComponent()) {
                return 1;
            }
            if (!ci0.isXmlPureComponent() && ci1.isXmlPureComponent()) {
                return -1;
            }

            return ci0.getId().compareTo(ci1.getId());
        }
    }

    public List<ComponentWO> getComponents() {
        List<ComponentWO> result = new ArrayList<>();
        BundleInfo bundle = getNxArtifact();

        List<ComponentInfo> cis = new ArrayList<>(bundle.getComponents());
        Collections.sort(cis, new ComponentInfoSorter());

        for (ComponentInfo ci : cis) {
            result.add((ComponentWO) ctx.newObject("component", ci.getId()));
        }
        return result;
    }

    @Override
    public String getSearchCriterion() {
        return "'" + super.getSearchCriterion() + "' Bundle";
    }
}
