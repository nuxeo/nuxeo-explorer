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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.plugin.PluginSnapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.PrettyPrinter;

public interface DistributionSnapshot extends DistributionSnapshotDesc {

    String TYPE_NAME = "NXDistribution";

    String CONTAINER_TYPE_NAME = "NXExplorerFolder";

    /** @since 22.0.0 */
    String OLD_CONTAINER_TYPE_NAME = "Workspace";

    String PROP_NAME = "nxdistribution:name";

    String PROP_VERSION = "nxdistribution:version";

    String PROP_KEY = "nxdistribution:key";

    /**
     * @since 8.3
     */
    String PROP_LATEST_FT = "nxdistribution:latestFT";

    /**
     * @since 8.3
     */
    String PROP_LATEST_LTS = "nxdistribution:latestLTS";

    /**
     * @since 8.3
     */
    String PROP_ALIASES = "nxdistribution:aliases";

    /**
     * @since 8.3
     */
    String PROP_HIDE = "nxdistribution:hide";

    /**
     * @since 8.3
     */
    String PROP_RELEASED = "nxdistribution:released";

    /**
     * @since 11.1
     */
    String PROP_CREATED = "dc:created";

    /**
     * Returns a key, combining {@link #getName()} and {@link #getVersion()}.
     */
    String getKey();

    void cleanPreviousArtifacts();

    /**
     * Returns the map of bundles by id.
     * <p>
     * This extra getter is particularly useful for json export/import.
     *
     * @since 11.1
     */
    List<BundleInfo> getBundles();

    /**
     * Returns the list of parent group bundles.
     */
    @JsonIgnore
    List<BundleGroup> getBundleGroups();

    BundleGroup getBundleGroup(String groupId);

    @JsonIgnore
    List<String> getBundleIds();

    BundleInfo getBundle(String id);

    @JsonIgnore
    List<String> getComponentIds();

    /**
     * Returns the list of all components, sorted by id.
     *
     * @since 20.1.0
     */
    @JsonIgnore
    List<ComponentInfo> getComponents();

    ComponentInfo getComponent(String id);

    @JsonIgnore
    List<String> getServiceIds();

    ServiceInfo getService(String id);

    @JsonIgnore
    List<String> getExtensionPointIds();

    ExtensionPointInfo getExtensionPoint(String id);

    @JsonIgnore
    List<String> getContributionIds();

    @JsonIgnore
    List<ExtensionInfo> getContributions();

    ExtensionInfo getContribution(String id);

    OperationInfo getOperation(String id);

    List<OperationInfo> getOperations();

    /**
     * Returns the package with given name (version not included).
     * <p>
     * Uses the package name instead of the id to simplify API, taking into account the fact that a single version of a
     * package can be installed in a distribution at a given time.
     *
     * @since 11.1
     */
    PackageInfo getPackage(String name);

    /**
     * Returns the list of all packages, sorted by id.
     *
     * @since 11.1
     */
    List<PackageInfo> getPackages();

    /**
     * @since 8.3
     */
    @JsonIgnore
    boolean isLatestFT();

    /**
     * @since 8.3
     */
    @JsonIgnore
    boolean isLatestLTS();

    /**
     * @since 8.3
     */
    @JsonIgnore
    List<String> getAliases();

    /**
     * @since 8.3
     */
    @JsonIgnore
    boolean isHidden();

    /**
     * Serializes in json the current instance, using given filter.
     *
     * @since 20.0.0
     */
    void writeJson(OutputStream out, SnapshotFilter filter, PrettyPrinter printer);

    /**
     * Reads the given json according to current json mapper (see {@link #getJsonMapper()}.
     *
     * @since 11.1
     */
    DistributionSnapshot readJson(InputStream in);

    /**
     * Returns a map of additional plugin resources.
     *
     * @since 11.1
     */
    Map<String, PluginSnapshot<?>> getPluginSnapshots();

}
