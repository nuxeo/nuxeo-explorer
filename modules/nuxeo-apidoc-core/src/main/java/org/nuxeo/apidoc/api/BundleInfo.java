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
package org.nuxeo.apidoc.api;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

public interface BundleInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXBundle";

    String PROP_ARTIFACT_GROUP_ID = "nxbundle:artifactGroupId";

    String PROP_ARTIFACT_ID = "nxbundle:artifactId";

    String PROP_ARTIFACT_VERSION = "nxbundle:artifactVersion";

    String PROP_BUNDLE_ID = "nxbundle:bundleId";

    String PROP_JAR_NAME = "nxbundle:jarName";

    /** @since 11.1 */
    String PROP_README = "nxbundle:readme";

    /** @since 11.1 */
    String PROP_PARENT_README = "nxbundle:parentReadme";

    /** @since 11.1 */
    String PROP_REQUIREMENTS = "nxbundle:requirements";

    /** @since 20.0.0 */
    String PROP_MIN_REGISTRATION_ORDER = "nxbundle:minResolutionOrder";

    /** @since 20.0.0 */
    String PROP_MAX_REGISTRATION_ORDER = "nxbundle:maxResolutionOrder";

    /** @since 11.1 */
    String PROP_PACKAGES = "nxbundle:packages";

    /** @since 11.1 */
    String RUNTIME_CONFIG_BUNDLE = "org.nuxeo.ecm.config";

    /**
     * Pseudo bundle, considered to be the root of all runtime bundles.
     *
     * @since 20.0.0
     */
    String RUNTIME_ROOT_PSEUDO_BUNDLE = "org.nuxeo.runtime.root";

    @JsonManagedReference("bundle")
    List<ComponentInfo> getComponents();

    String getFileName();

    String getBundleId();

    /**
     * Returns the requirements set in the bundle MANIFEST.
     *
     * @since 11.1
     */
    List<String> getRequirements();

    String getManifest();

    String getLocation();

    /**
     * Returns the corresponding maven group id.
     * <p>
     * Can differ from corresponding id returned by {@link #getBundleGroup()} depending on maven introspection.
     *
     * @since 11.1
     */
    String getGroupId();

    /**
     * @since 11.1
     */
    void setGroupId(String groupId);

    String getArtifactId();

    String getArtifactVersion();

    /**
     * Returns the corresponding bundle group.
     * <p>
     * Can differ from corresponding id returned by {@link #getGroupId()} depending on maven introspection.
     *
     * @since 11.1
     */
    BundleGroup getBundleGroup();

    /**
     * @since 11.1
     */
    void setBundleGroup(BundleGroup bundleGroup);

    /**
     * @since 11.1
     */
    Blob getReadme();

    /**
     * @since 11.1
     */
    Blob getParentReadme();

    /**
     * Returns the minimal resolution order among this bundle's components.
     *
     * @since 20.0.0
     */
    Long getMinResolutionOrder();

    /**
     * Sets the minimal resolution order among this bundle's components.
     *
     * @since 20.0.0
     */
    void setMinResolutionOrder(Long order);

    /**
     * Returns the maximal resolution order among this bundle's components.
     *
     * @since 20.0.0
     */
    Long getMaxResolutionOrder();

    /**
     * Sets the maximal resolution order among this bundle's components.
     *
     * @since 20.0.0
     */
    void setMaxResolutionOrder(Long order);

    /**
     * Returns the list of Nuxeo packages containing this bundle.
     * <p>
     * Even if a given bundle will probably be held by only one package, in theory it could be included in several
     * packages, hence the list.
     * <p>
     * The package name is used instead of the package id (holding the version) because at a given time, only one
     * version of a package should be installed on a distribution.
     *
     * @since 11.1
     */
    List<String> getPackages();

    /**
     * Returns all services for this bundle.
     *
     * @since 20.1.0
     */
    @JsonIgnore
    List<ServiceInfo> getServices();

    /**
     * Returns all extension points for this bundle.
     *
     * @since 20.1.0
     */
    @JsonIgnore
    List<ExtensionPointInfo> getExtensionPoints();

    /**
     * Returns all extensions for this bundle.
     *
     * @since 20.1.0
     */
    @JsonIgnore
    List<ExtensionInfo> getExtensions();

}
