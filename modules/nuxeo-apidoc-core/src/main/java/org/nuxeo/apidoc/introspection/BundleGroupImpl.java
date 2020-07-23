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
package org.nuxeo.apidoc.introspection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.NuxeoArtifactComparator;
import org.nuxeo.ecm.core.api.Blob;

import com.fasterxml.jackson.annotation.JsonCreator;

public class BundleGroupImpl extends BaseNuxeoArtifact implements BundleGroup {

    protected final String key;

    protected final String name;

    protected final List<BundleGroup> subGroups = new ArrayList<>();

    protected final List<String> bundleIds = new ArrayList<>();

    protected String version;

    protected BundleGroup parentGroup;

    protected final List<String> parentIds = new ArrayList<>();

    protected final List<Blob> readmes = new ArrayList<>();

    public BundleGroupImpl(String id) {
        this.key = id;
        if (id.startsWith(BundleGroup.PREFIX)) {
            name = id.substring(BundleGroup.PREFIX.length());
        } else {
            name = id;
        }
    }

    @JsonCreator
    public BundleGroupImpl() {
        this.key = null;
        this.name = null;
    }

    /**
     * @since 11.1
     */
    public void addParent(String bgId) {
        parentIds.add(bgId);
    }

    @Override
    public String getName() {
        return name;
    }

    public void add(BundleGroupImpl group) {
        subGroups.add(group);
    }

    public void add(String bundleId) {
        bundleIds.add(bundleId);
    }

    @Override
    public List<BundleGroup> getSubGroups() {
        return subGroups.stream()
                        .sorted(new NuxeoArtifactComparator())
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<String> getBundleIds() {
        return bundleIds.stream()
                        .sorted()
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public BundleGroup getParentGroup() {
        return parentGroup;
    }

    /**
     * @since 11.1
     */
    public void setParentGroup(BundleGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    @Override
    public List<String> getParentIds() {
        return Collections.unmodifiableList(parentIds);
    }

    @Override
    public String getId() {
        return key;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        String path = "";
        for (String parentId : parentIds) {
            path = path + "/" + parentId;
        }
        return path + "/" + getId();
    }

    @Override
    public List<Blob> getReadmes() {
        return Collections.unmodifiableList(readmes);
    }

    public void addReadme(Blob readme) throws IOException {
        if (readme == null) {
            return;
        }
        String content = readme.getString();
        if (content == null) {
            return;
        }
        // check for duplicates as the same readme can already be referenced by multiple children
        for (Blob er : readmes) {
            if (content.equals(er.getString())) {
                return;
            }
        }
        readmes.add(readme);
    }

}
