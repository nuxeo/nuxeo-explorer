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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.export.api.Exporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * Web Object for {@link PackageInfo} display.
 *
 * @since 11.1
 */
@WebObject(type = PackageWO.TYPE)
public class PackageWO extends NuxeoArtifactWebObject {

    public static final String TYPE = "package";

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetPackageInfo(getSnapshot());
    }

    protected PackageInfo getTargetPackageInfo(DistributionSnapshot snapshot) {
        return snapshot.getPackage(nxArtifactId);
    }

    protected Map<String, BundleInfo> getBundleInfo(DistributionSnapshot snapshot, List<String> bundles) {
        Map<String, BundleInfo> res = new LinkedHashMap<String, BundleInfo>();
        bundles.forEach(bid -> res.put(bid, snapshot.getBundle(bid)));
        return res;
    }

    protected List<ComponentInfo> getComponentInfo(List<BundleInfo> bundles) {
        List<ComponentInfo> res = new ArrayList<ComponentInfo>();
        bundles.forEach(b -> res.addAll(b.getComponents()));
        return res;
    }

    protected List<ServiceInfo> getServiceInfo(List<ComponentInfo> components) {
        List<ServiceInfo> res = new ArrayList<ServiceInfo>();
        components.forEach(c -> res.addAll(c.getServices()));
        return res;
    }

    protected List<ExtensionPointInfo> getExtensionPointInfo(List<ComponentInfo> components) {
        List<ExtensionPointInfo> res = new ArrayList<ExtensionPointInfo>();
        components.forEach(c -> res.addAll(c.getExtensionPoints()));
        return res;
    }

    protected List<ExtensionInfo> getContributionInfo(List<ComponentInfo> components) {
        List<ExtensionInfo> res = new ArrayList<ExtensionInfo>();
        components.forEach(c -> res.addAll(c.getExtensions()));
        return res;
    }

    @Produces("text/html")
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        DistributionSnapshot snapshot = getSnapshot();
        PackageInfo pkg = getTargetPackageInfo(snapshot);
        String marketplaceURL = PackageInfo.getMarketplaceURL(pkg, true);
        t.arg("marketplaceURL", marketplaceURL);
        Map<String, BundleInfo> binfo = getBundleInfo(snapshot, pkg.getBundles());
        t.arg("bundles", binfo);
        List<ComponentInfo> components = getComponentInfo(
                binfo.values().stream().filter(Objects::nonNull).collect(Collectors.toList()));
        t.arg("components", components);
        t.arg("services", getServiceInfo(components));
        t.arg("extensionpoints", getExtensionPointInfo(components));
        t.arg("contributions", getContributionInfo(components));

        List<Exporter> exporters = getSnapshotManager().getExporters()
                                                       .stream()
                                                       .filter(e -> e.displayOn("package"))
                                                       .collect(Collectors.toList());
        t.arg("exporters", exporters);
        return t;
    }

    @Override
    public String getSearchCriterion() {
        return "'" + super.getSearchCriterion() + "' Package";
    }

}
