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
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BaseNuxeoLiveArtifact;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.PackageInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 11.1
 */
public class PackageInfoImpl extends BaseNuxeoLiveArtifact implements PackageInfo {

    protected final Map<String, BundleInfo> bundles = new LinkedHashMap<>();

    protected final String id;

    protected final String name;

    protected final String title;

    protected final String version;

    protected final String packageType;

    protected final List<String> dependencies = new ArrayList<>();

    protected final List<String> optionalDependencies = new ArrayList<>();

    protected final List<String> conflicts = new ArrayList<>();

    public PackageInfoImpl(String id, String name, String version, String title, String packageType,
            List<String> dependencies, List<String> optionalDependencies, List<String> conflicts) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.version = version;
        this.packageType = packageType;
        if (dependencies != null) {
            this.dependencies.addAll(dependencies);
        }
        if (optionalDependencies != null) {
            this.optionalDependencies.addAll(optionalDependencies);
        }
        if (conflicts != null) {
            this.conflicts.addAll(conflicts);
        }
    }

    @JsonCreator
    private PackageInfoImpl() {
        this.id = null;
        this.name = null;
        this.title = null;
        this.version = null;
        this.packageType = null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return "/" + getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getPackageType() {
        return packageType;
    }

    @Override
    public List<String> getBundles() {
        return new ArrayList<>(bundles.keySet());
    }

    @JsonProperty("bundles")
    protected void setBundles(List<String> bundles) {
        if (bundles != null) {
            bundles.forEach(b -> this.bundles.put(b, null));
        }
    }

    public void addBundle(BundleInfo bundle) {
        if (bundle != null) {
            bundles.putIfAbsent(bundle.getId(), bundle);
        }
    }

    @Override
    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    @Override
    public List<String> getOptionalDependencies() {
        return Collections.unmodifiableList(optionalDependencies);
    }

    @Override
    public List<String> getConflicts() {
        return Collections.unmodifiableList(conflicts);
    }

    @Override
    public Map<String, BundleInfo> getBundleInfo() {
        return Collections.unmodifiableMap(bundles);
    }

}
