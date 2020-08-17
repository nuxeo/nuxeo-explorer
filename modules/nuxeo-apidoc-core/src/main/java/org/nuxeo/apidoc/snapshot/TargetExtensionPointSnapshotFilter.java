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
package org.nuxeo.apidoc.snapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;

/**
 * Filter used to include references to target Nuxeo extension points, selected thanks to a previous filter.
 * <p>
 * Will select components declaring the extension point, as well as enclosed bundle and included services and
 * contributions.
 *
 * @since 20.0.0
 */
public class TargetExtensionPointSnapshotFilter implements SnapshotFilter {

    protected final String name;

    protected final List<NuxeoArtifact> selectedArtifacts;

    protected Set<String> targetComponentNames = new HashSet<>();

    protected Set<String> targetExtensionPoints = new HashSet<>();

    public TargetExtensionPointSnapshotFilter(String name, List<NuxeoArtifact> selectedArtifacts) {
        this.name = name;
        this.selectedArtifacts = selectedArtifacts;
        index(selectedArtifacts);
    }

    protected void index(List<NuxeoArtifact> selectedArtifacts) {
        // retrieve target extension points for given artifacts, assuming only complete bundles have been previously
        // selected
        List<ExtensionInfo> allExtensions = selectedArtifacts.stream()
                                                             .filter(BundleInfo.class::isInstance)
                                                             .map(BundleInfo.class::cast)
                                                             .flatMap(b -> b.getComponents().stream())
                                                             .flatMap(c -> c.getExtensions().stream())
                                                             .collect(Collectors.toList());
        allExtensions.forEach(e -> targetExtensionPoints.add(e.getExtensionPoint()));
        allExtensions.forEach(e -> targetComponentNames.add(e.getTargetComponentName().getName()));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean accept(NuxeoArtifact artifact) {
        if (selectedArtifacts.contains(artifact)) {
            return false;
        }
        if (artifact instanceof BundleInfo) {
            return (((BundleInfo) artifact).getComponents()
                                           .stream()
                                           .map(ComponentInfo::getId)
                                           .anyMatch(targetComponentNames::contains));
        }
        if (artifact instanceof ComponentInfo) {
            return targetComponentNames.contains(((ComponentInfo) artifact).getId());
        }
        if (artifact instanceof ExtensionPointInfo) {
            return targetExtensionPoints.contains(((ExtensionPointInfo) artifact).getId());
        }
        if (artifact instanceof ServiceInfo) {
            return false;
        }
        // skip services, contributions, operations, packages
        return false;
    }

    @Override
    public Class<SnapshotFilter> getReferenceClass() {
        return null;
    }

}
