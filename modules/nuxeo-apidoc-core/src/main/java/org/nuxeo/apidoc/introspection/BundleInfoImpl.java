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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.ecm.core.api.Blob;

import com.fasterxml.jackson.annotation.JsonCreator;

public class BundleInfoImpl extends BaseNuxeoArtifact implements BundleInfo {

    protected final String bundleId;

    protected final List<ComponentInfo> components = new ArrayList<>();

    protected String fileName;

    protected String manifest;

    protected String location;

    /** @since 11.1 */
    protected final List<String> requirements = new ArrayList<>();

    protected String groupId;

    protected String artifactId;

    protected String artifactVersion;

    protected BundleGroup bundleGroup;

    protected Blob readme;

    protected Blob parentReadme;

    /** @since 20.0.0 */
    protected Long minResolutionOrder;

    /** @since 20.0.0 */
    protected Long maxResolutionOrder;

    /** @since 11.1 */
    protected final List<String> packages = new ArrayList<>();

    // cache lists

    protected List<ExtensionPointInfo> extensionPoints;

    protected List<ServiceInfo> services;

    protected List<ExtensionInfo> extensions;

    public BundleInfoImpl(String bundleId) {
        this.bundleId = bundleId;
    }

    @JsonCreator
    private BundleInfoImpl() {
        this.bundleId = null;
    }

    @Override
    public BundleGroup getBundleGroup() {
        return bundleGroup;
    }

    @Override
    public void setBundleGroup(BundleGroup bundleGroup) {
        this.bundleGroup = bundleGroup;
    }

    @Override
    public List<ComponentInfo> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public void addComponent(ComponentInfoImpl component) {
        components.add(component);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getBundleId() {
        return bundleId;
    }

    @Override
    public List<String> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public void setRequirements(List<String> requirements) {
        this.requirements.clear();
        if (requirements != null) {
            this.requirements.addAll(requirements);
        }
    }

    @Override
    public List<String> getPackages() {
        return packages.stream().sorted().collect(Collectors.toUnmodifiableList());
    }

    public void setPackages(List<String> packages) {
        this.packages.clear();
        if (packages != null) {
            this.packages.addAll(packages);
        }
    }

    @Override
    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    @Override
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    @Override
    public String getId() {
        return bundleId;
    }

    @Override
    public String getVersion() {
        return artifactVersion;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return getBundleGroup().getHierarchyPath() + "/" + getId();
    }

    @Override
    public Blob getReadme() {
        return readme;
    }

    @Override
    public Blob getParentReadme() {
        return parentReadme;
    }

    public void setReadme(Blob readme) {
        this.readme = readme;
    }

    public void setParentReadme(Blob parentReadme) {
        this.parentReadme = parentReadme;
    }

    @Override
    public Long getMinResolutionOrder() {
        return minResolutionOrder;
    }

    @Override
    public void setMinResolutionOrder(Long order) {
        this.minResolutionOrder = order;
    }

    @Override
    public Long getMaxResolutionOrder() {
        return maxResolutionOrder;
    }

    @Override
    public void setMaxResolutionOrder(Long order) {
        this.maxResolutionOrder = order;
    }

    @Override
    public List<ServiceInfo> getServices() {
        if (services == null) {
            services = new ArrayList<>();
            getComponents().forEach(c -> services.addAll(c.getServices()));
            services.sort(Comparator.comparing(NuxeoArtifact::getId));
        }
        return Collections.unmodifiableList(services);
    }

    @Override
    public List<ExtensionPointInfo> getExtensionPoints() {
        if (extensionPoints == null) {
            extensionPoints = new ArrayList<>();
            getComponents().forEach(c -> extensionPoints.addAll(c.getExtensionPoints()));
            extensionPoints.sort(Comparator.comparing(NuxeoArtifact::getId));
        }
        return Collections.unmodifiableList(extensionPoints);
    }

    @Override
    public List<ExtensionInfo> getExtensions() {
        if (extensions == null) {
            extensions = new ArrayList<>();
            getComponents().forEach(c -> extensions.addAll(c.getExtensions()));
            extensions.sort(Comparator.comparing(NuxeoArtifact::getId));
        }
        return Collections.unmodifiableList(extensions);
    }

}
