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
public class PackageWO extends NuxeoArtifactWebObject<PackageInfo> {

    public static final String TYPE = "package";

    @Override
    public PackageInfo getNxArtifact() {
        if (nxArtifact == null) {
            nxArtifact = getTargetPackageInfo(getSnapshot());
        }
        return nxArtifact;
    }

    protected PackageInfo getTargetPackageInfo(DistributionSnapshot snapshot) {
        return snapshot.getPackage(nxArtifactId);
    }

    protected String getPackageNameWithoutVersion(String dependency) {
        int index = dependency.indexOf(":");
        if (index > 0) {
            return dependency.substring(0, index);
        }
        return dependency;
    }

    protected Map<String, PackageInfo> getDependenciesInfo(DistributionSnapshot snapshot, List<String> packages) {
        var res = new LinkedHashMap<String, PackageInfo>();
        packages.forEach(pkg -> res.put(pkg, snapshot.getPackage(getPackageNameWithoutVersion(pkg))));
        return res;
    }

    @Produces("text/html")
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        DistributionSnapshot snapshot = getSnapshot();
        PackageInfo pkg = getNxArtifact();
        String marketplaceURL = PackageInfo.getMarketplaceURL(pkg, true);
        t.arg("marketplaceURL", marketplaceURL);
        Map<String, BundleInfo> binfo = pkg.getBundleInfo();
        t.arg("bundles", binfo);

        var components = new ArrayList<ComponentInfo>();
        var sLabels = new ArrayList<ArtifactLabel>();
        var xps = new ArrayList<ExtensionPointInfo>();
        var conts = new ArrayList<ExtensionInfo>();
        binfo.values().stream().filter(Objects::nonNull).forEach(bi -> {
            components.addAll(bi.getComponents());
            bi.getServices()
              .stream()
              .map(ServiceInfo::getId)
              .map(ArtifactLabel::createLabelFromService)
              .forEach(sLabels::add);
            xps.addAll(bi.getExtensionPoints());
            conts.addAll(bi.getExtensions());
        });
        t.arg("components", components);
        t.arg("services", sLabels);
        t.arg("extensionpoints", xps);
        t.arg("contributions", conts);

        t.arg("dependencies", getDependenciesInfo(snapshot, pkg.getDependencies()));
        t.arg("optionalDependencies", getDependenciesInfo(snapshot, pkg.getOptionalDependencies()));
        t.arg("conflicts", getDependenciesInfo(snapshot, pkg.getConflicts()));

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
