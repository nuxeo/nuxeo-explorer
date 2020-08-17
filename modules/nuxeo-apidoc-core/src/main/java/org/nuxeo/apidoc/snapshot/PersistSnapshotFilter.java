/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;

/**
 * Filter for persistence of a live runtime snapshot.
 *
 * @since 20.0.0
 */
public class PersistSnapshotFilter implements SnapshotFilter {

    protected final String name;

    protected final Class<? extends SnapshotFilter> referenceClass;

    protected final boolean checkAsPrefixes;

    protected final List<String> bundles = new ArrayList<>();

    protected final List<String> nxpackages = new ArrayList<>();

    protected final List<String> javaPackagePrefixes = new ArrayList<>();

    public PersistSnapshotFilter(String bundleGroupName) {
        this(bundleGroupName, true);
    }

    public PersistSnapshotFilter(String bundleGroupName, boolean checkAsPrefixes) {
        this(bundleGroupName, checkAsPrefixes, null);
    }

    public PersistSnapshotFilter(String bundleGroupName, boolean checkAsPrefixes,
            Class<? extends SnapshotFilter> referenceClass) {
        this.name = bundleGroupName;
        this.checkAsPrefixes = checkAsPrefixes;
        this.referenceClass = referenceClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean accept(NuxeoArtifact artifact) {
        if (artifact instanceof BundleInfo) {
            return includeBundle((BundleInfo) artifact, true);
        }
        if (artifact instanceof ComponentInfo) {
            return includeComponent((ComponentInfo) artifact, true);
        }
        if (artifact instanceof ServiceInfo) {
            return includeComponent(((ServiceInfo) artifact).getComponent(), false);
        }
        if (artifact instanceof ExtensionPointInfo) {
            return includeComponent(((ExtensionPointInfo) artifact).getComponent(), false);
        }
        if (artifact instanceof ExtensionInfo) {
            return includeComponent(((ExtensionInfo) artifact).getComponent(), true);
        }
        if (artifact instanceof OperationInfo) {
            return includeOperation((OperationInfo) artifact, true);
        }
        if (artifact instanceof PackageInfo) {
            return includePackage((PackageInfo) artifact);
        }
        return true;
    }

    @Override
    public Class<? extends SnapshotFilter> getReferenceClass() {
        return referenceClass;
    }

    public void addBundle(String bundlePrefix) {
        bundles.add(bundlePrefix);
    }

    public void addNuxeoPackage(String nuxeoPackage) {
        nxpackages.add(nuxeoPackage);
    }

    public void addPackagesPrefix(String packagesPrefix) {
        javaPackagePrefixes.add(packagesPrefix);
    }

    protected boolean check(String candidate, String selection) {
        return (checkAsPrefixes && candidate.startsWith(selection))
                || (!checkAsPrefixes && candidate.equals(selection));
    }

    protected boolean includeBundle(BundleInfo bundle, boolean checkOps) {
        String bundleId = bundle.getId();
        for (String sb : bundles) {
            if (check(bundleId, sb)) {
                return true;
            }
        }
        for (String sp : nxpackages) {
            if (bundle.getPackages().stream().anyMatch(s -> check(s, sp))) {
                return true;
            }
        }
        if (checkOps) {
            return bundle.getComponents()
                         .stream()
                         .flatMap(c -> c.getOperations().stream())
                         .anyMatch(op -> includeOperation(op, false));
        }
        return false;
    }

    protected boolean includeComponent(ComponentInfo component, boolean checkOps) {
        if (includeBundle(component.getBundle(), false)) {
            return true;
        }
        if (checkOps) {
            return component.getOperations().stream().anyMatch(op -> includeOperation(op, false));
        }
        return false;
    }

    /**
     * @implNote checks the operation class against package prefixes. Note some operations are actually not classes, so
     *           it could be possible to filter on contributing component, retrieving its bundle, and checking bundle
     *           prefixes too... (not done).
     */
    protected boolean includeOperation(OperationInfo op, boolean checkComponents) {
        for (String pprefix : javaPackagePrefixes) {
            if (op.getOperationClass().startsWith(pprefix)) {
                return true;
            }
        }
        if (checkComponents) {
            ComponentInfo comp = op.getComponent();
            if (comp != null) {
                return includeComponent(comp, false);
            }
        }
        return false;
    }

    /**
     * Checks the Nuxeo packages bundles against Nuxeo packages prefix, as well as included bundles against bundle
     * prefixes.
     *
     * @since 11.1
     */
    protected boolean includePackage(PackageInfo pkg) {
        for (String sp : nxpackages) {
            if (check(pkg.getName(), sp)) {
                return true;
            }
        }
        for (String bundle : pkg.getBundles()) {
            for (String sb : bundles) {
                if (check(bundle, sb)) {
                    return true;
                }
            }
        }
        return false;
    }

}
