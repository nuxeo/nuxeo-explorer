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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupExtractor;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.NuxeoArtifactComparator;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.JsonMapper;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class RuntimeSnapshot extends BaseNuxeoArtifact implements DistributionSnapshot {

    /** @since 20.0.0 */
    public static final List<String> LIVE_ALIASES = List.of(SnapshotManager.DISTRIBUTION_ALIAS_CURRENT,
            SnapshotManager.DISTRIBUTION_ALIAS_ADM);

    protected final Date created;

    protected final Date released;

    protected final String name;

    protected final String version;

    protected final Map<String, BundleInfo> bundles = new LinkedHashMap<>();

    protected final Map<String, String> components2Bundles = new HashMap<>();

    protected final Map<String, String> componentAliases2Bundles = new HashMap<>();

    protected final Map<String, String> services2Components = new HashMap<>();

    protected final Map<String, ExtensionPointInfo> extensionPoints = new LinkedHashMap<>();

    protected final Map<String, ExtensionPointInfo> extensionPointAliases = new LinkedHashMap<>();

    protected final Map<String, ExtensionInfo> contributions = new LinkedHashMap<>();

    protected List<BundleGroup> parentBundleGroups = new ArrayList<>();

    protected final Map<String, BundleGroup> bundleGroups = new LinkedHashMap<>();

    protected boolean opsInitialized = false;

    protected final Map<String, OperationInfo> operations = new LinkedHashMap<>();

    protected final Map<String, String> operationAliases = new HashMap<>();

    protected final Map<String, PackageInfo> packages = new HashMap<>();

    protected JavaDocHelper jdocHelper;

    protected boolean pluginSnapshotsInitialized = false;

    protected final Map<String, PluginSnapshot<?>> pluginSnapshots = new HashMap<>();

    protected final List<String> aliases = new LinkedList<>(LIVE_ALIASES);

    public static RuntimeSnapshot build() {
        return new RuntimeSnapshot();
    }

    @JsonCreator
    public RuntimeSnapshot(@JsonProperty("name") String name, @JsonProperty("version") String version,
            @JsonProperty("creationDate") Date created, @JsonProperty("releaseDate") Date released,
            @JsonProperty("bundles") List<BundleInfo> bundles,
            @JsonProperty("operations") List<OperationInfo> operations,
            @JsonProperty("packages") List<PackageInfo> packages,
            @JsonProperty("pluginSnapshots") Map<String, PluginSnapshot<?>> pluginSnapshots) {
        this.created = created;
        this.released = released;
        this.name = name;
        this.version = version;
        index(bundles, packages);
        if (operations != null) {
            operations.forEach(this::addOperation);
        }
        this.opsInitialized = true;
        if (pluginSnapshots != null) {
            this.pluginSnapshots.putAll(pluginSnapshots);
        }
        this.pluginSnapshotsInitialized = true;
    }

    protected RuntimeSnapshot() {
        created = new Date();
        released = null;
        ServerInfo serverInfo = ServerInfo.build();
        this.name = serverInfo.getName();
        this.version = serverInfo.getVersion();
        index(serverInfo.getBundles(), serverInfo.getPackages());
        initOperations();
        initPluginSnapshots();
    }

    protected void index(List<BundleInfo> distributionBundles, List<PackageInfo> packages) {
        if (packages != null) {
            packages.stream().forEach(pkg -> this.packages.put(pkg.getName(), pkg));
        }

        if (distributionBundles == null) {
            return;
        }
        for (BundleInfo bInfo : distributionBundles) {
            String bid = bInfo.getId();
            bundles.put(bid, bInfo);
            for (ComponentInfo cInfo : bInfo.getComponents()) {
                String cid = cInfo.getId();
                components2Bundles.put(cid, bid);
                cInfo.getAliases().forEach(a -> componentAliases2Bundles.put(a, bid));

                for (ServiceInfo sInfo : cInfo.getServices()) {
                    if (sInfo.isOverriden()) {
                        continue;
                    }
                    services2Components.put(sInfo.getId(), cid);
                }

                for (ExtensionPointInfo epi : cInfo.getExtensionPoints()) {
                    extensionPoints.put(epi.getId(), epi);
                    epi.getAliases().forEach(a -> extensionPointAliases.put(a, epi));
                }

                for (ExtensionInfo ei : cInfo.getExtensions()) {
                    contributions.put(ei.getId(), ei);
                }
            }
        }

        BundleGroupExtractor bge = new BundleGroupExtractor(bundles, version);
        parentBundleGroups.addAll(bge.getRoots());
        bundleGroups.putAll(bge.getGroups());
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<BundleGroup> getBundleGroups() {
        return parentBundleGroups.stream()
                                 .sorted(new NuxeoArtifactComparator())
                                 .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public BundleGroup getBundleGroup(String groupId) {
        if (groupId == null) {
            return null;
        }
        if (groupId.startsWith(BundleGroup.PREFIX)) {
            return bundleGroups.get(groupId);
        } else {
            return bundleGroups.get(BundleGroup.PREFIX + groupId);
        }
    }

    protected void browseBundleGroup(BundleGroup group, int level, List<BundleGroupFlatTree> tree) {
        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);
        for (BundleGroup subGroup : group.getSubGroups()) {
            browseBundleGroup(subGroup, level + 1, tree);
        }
    }

    @Override
    public List<String> getBundleIds() {
        return new ArrayList<>(bundles.keySet());
    }

    @Override
    public BundleInfo getBundle(String id) {
        return bundles.get(id);
    }

    @Override
    public List<String> getComponentIds() {
        List<String> componentsIds = new ArrayList<>();
        componentsIds.addAll(components2Bundles.keySet());
        Collections.sort(componentsIds);
        return componentsIds;
    }

    @Override
    public List<ComponentInfo> getComponents() {
        return bundles.values()
                      .stream()
                      .flatMap(b -> b.getComponents().stream())
                      .sorted(new NuxeoArtifactComparator())
                      .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public ComponentInfo getComponent(String id) {
        String bundleId = components2Bundles.getOrDefault(id, componentAliases2Bundles.get(id));
        if (bundleId == null) {
            return null;
        }
        BundleInfo bi = getBundle(bundleId);

        for (ComponentInfo ci : bi.getComponents()) {
            if (ci.getId().equals(id) || ci.getAliases().contains(id)) {
                return ci;
            }
        }
        return null;
    }

    @Override
    public List<String> getServiceIds() {
        List<String> serviceIds = new ArrayList<>();
        serviceIds.addAll(services2Components.keySet());
        Collections.sort(serviceIds);
        return serviceIds;
    }

    @Override
    public List<String> getExtensionPointIds() {
        List<String> epIds = new ArrayList<>();
        epIds.addAll(extensionPoints.keySet());
        Collections.sort(epIds);
        return epIds;
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String id) {
        return extensionPoints.getOrDefault(id, extensionPointAliases.get(id));
    }

    @Override
    public List<String> getContributionIds() {
        List<String> contribIds = new ArrayList<>();
        contribIds.addAll(contributions.keySet());
        Collections.sort(contribIds);
        return contribIds;
    }

    @Override
    public List<ExtensionInfo> getContributions() {
        List<ExtensionInfo> contribs = new ArrayList<>();
        contribs.addAll(contributions.values());
        return contribs;
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return contributions.get(id);
    }

    @Override
    public String getKey() {
        return getName() + "-" + getVersion();
    }

    @Override
    public String getId() {
        return getKey();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public ServiceInfo getService(String id) {
        String cId = services2Components.get(id);
        if (cId == null) {
            return null;
        }

        for (ServiceInfo si : getComponent(cId).getServices()) {
            if (id.equals(si.getId())) {
                return si;
            }
        }
        return null;
    }

    @Override
    public Date getCreationDate() {
        return created;
    }

    @Override
    public Date getReleaseDate() {
        return released;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public String getHierarchyPath() {
        return null;
    }

    protected void initOperations() {
        if (opsInitialized) {
            return;
        }
        AutomationService service = Framework.getService(AutomationService.class);
        if (service == null) {
            return;
        }
        OperationType[] ops = service.getOperations();
        // make sure operations are ordered, as service currently returns any order
        List<OperationType> oops = Arrays.asList(ops);
        oops.sort(Comparator.comparing(OperationType::getId));
        var bundleToOperations = new HashMap<String, List<OperationInfo>>();
        for (OperationType op : oops) {
            OperationDocumentation documentation;
            try {
                documentation = op.getDocumentation();
            } catch (OperationException e) {
                throw new NuxeoException(e);
            }
            OperationInfo opi = new OperationInfoImpl(documentation, getVersion(), op.getType().getCanonicalName(),
                    op.getContributingComponent());
            String cid = opi.getContributingComponent();
            if (StringUtils.isNotBlank(cid) && !OperationInfo.BUILT_IN.equals(cid)) {
                bundleToOperations.computeIfAbsent(opi.getContributingComponent(), c -> new ArrayList<>()).add(opi);
            }
            addOperation(opi);
        }
        opsInitialized = true;

        // post process all components to set operations on them
        for (Map.Entry<String, List<OperationInfo>> e : bundleToOperations.entrySet()) {
            ComponentInfo c = getComponent(e.getKey());
            if (c instanceof ComponentInfoImpl) {
                ((ComponentInfoImpl) c).setOperations(e.getValue());
            }
            e.getValue()
             .stream()
             .filter(OperationInfoImpl.class::isInstance)
             .map(OperationInfoImpl.class::cast)
             .forEach(opi -> opi.setComponent(c));
        }
    }

    protected void addOperation(OperationInfo opi) {
        this.operations.put(opi.getName(), opi);
        String opName = opi.getName();
        List<String> aliases = opi.getAliases();
        if (aliases != null) {
            aliases.forEach(a -> this.operationAliases.put(a, opName));
        }
    }

    @Override
    public OperationInfo getOperation(String id) {
        String finalId = id;
        if (id.startsWith(OperationInfo.ARTIFACT_PREFIX)) {
            finalId = id.substring(OperationInfo.ARTIFACT_PREFIX.length());
        }
        if (!operations.containsKey(finalId) && operationAliases.containsKey(finalId)) {
            finalId = operationAliases.get(finalId);
        }
        return operations.get(finalId);
    }

    @Override
    public List<OperationInfo> getOperations() {
        initOperations();
        return new ArrayList<>(operations.values());
    }

    @Override
    public PackageInfo getPackage(String name) {
        return packages.get(name);
    }

    @Override
    public List<PackageInfo> getPackages() {
        return Collections.unmodifiableList(new ArrayList<>(packages.values()));
    }

    public JavaDocHelper getJavaDocHelper() {
        if (jdocHelper == null) {
            jdocHelper = JavaDocHelper.getHelper(getName(), getVersion());
        }
        return jdocHelper;
    }

    @Override
    public void cleanPreviousArtifacts() {
        // Can't delete anything in a runtime Snapshot
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLatestFT() {
        return false;
    }

    @Override
    public boolean isLatestLTS() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    protected List<Plugin<?>> getPlugins() {
        return Framework.getService(SnapshotManager.class).getPlugins();
    }

    protected ObjectMapper getJsonMapper(SnapshotFilter filter) {
        SnapshotFilter refFilter = null;
        if (filter != null && filter.getReferenceClass() != null) {
            // resolve bundle selection to get reference filter
            var selectedBundles = new ArrayList<NuxeoArtifact>();
            getBundles().stream().filter(filter::accept).forEach(selectedBundles::add);
            try {
                Constructor<? extends SnapshotFilter> constructor = filter.getReferenceClass()
                                                                          .getConstructor(String.class, List.class);
                refFilter = constructor.newInstance(filter.getName() + SnapshotFilter.REFERENCE_FILTER_NAME_SUFFIX,
                        selectedBundles);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        ObjectMapper mapper = JsonMapper.basic(filter, refFilter);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Plugin<?> plugin : getPlugins()) {
            mapper = plugin.enrishJsonMapper(mapper);
        }
        return mapper;
    }

    @Override
    public void writeJson(OutputStream out, SnapshotFilter filter, PrettyPrinter printer) {
        ObjectWriter writer = getJsonMapper(filter).writerFor(DistributionSnapshot.class)
                                                   .withoutRootName()
                                                   .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                                                   .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        if (printer != null) {
            writer = writer.with(printer);
        }
        try {
            writer.writeValue(out, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DistributionSnapshot readJson(InputStream in) {
        ObjectReader reader = getJsonMapper(null).readerFor(DistributionSnapshot.class)
                                                 .withoutRootName()
                                                 .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                                                 .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        try {
            return reader.readValue(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initPluginSnapshots() {
        if (pluginSnapshotsInitialized) {
            return;
        }
        getPlugins().forEach(plugin -> pluginSnapshots.put(plugin.getId(), plugin.getRuntimeSnapshot(this)));
        pluginSnapshotsInitialized = true;
    }

    @Override
    public Map<String, PluginSnapshot<?>> getPluginSnapshots() {
        initPluginSnapshots();
        return Collections.unmodifiableMap(pluginSnapshots);
    }

    @Override
    public List<BundleInfo> getBundles() {
        return Collections.unmodifiableList(new ArrayList<>(bundles.values()));
    }

}
