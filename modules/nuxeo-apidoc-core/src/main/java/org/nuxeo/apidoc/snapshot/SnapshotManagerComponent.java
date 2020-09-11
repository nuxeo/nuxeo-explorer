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

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.Descriptor;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.export.api.Exporter;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginDescriptor;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.search.ArtifactSearcherImpl;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

public class SnapshotManagerComponent extends DefaultComponent implements SnapshotManager {

    private static final Logger log = LogManager.getLogger(SnapshotManagerComponent.class);

    /**
     * ArtifactSearcher service, moved from removed documentation service.
     *
     * @since 11.1
     */
    protected final ArtifactSearcher searcher = new ArtifactSearcherImpl();

    /**
     * Extension point for plugins.
     *
     * @since 11.1
     */
    public static final String XP_PLUGINS = "plugins";

    /**
     * Extension point for exports.
     *
     * @since 20.0.0
     */
    public static final String XP_EXPORTERS = "exporters";

    protected volatile DistributionSnapshot runtimeSnapshot;

    protected static final String IMPORT_TMP = "tmpImport";

    protected final SnapshotPersister persister = new SnapshotPersister();

    protected SnapshotListener componentListener;

    protected final Map<String, Plugin<?>> plugins = new LinkedHashMap<>();

    protected final Map<String, Exporter> exporters = new LinkedHashMap<>();

    public SnapshotManagerComponent() {
        componentListener = new SnapshotListener();
        ComponentManager compManager = Framework.getRuntime().getComponentManager();
        compManager.addComponentListener(componentListener);
        compManager.addListener(componentListener);
    }

    @Override
    public DistributionSnapshot getRuntimeSnapshot() {
        if (isSiteMode()) {
            throw new RuntimeServiceException("Live runtime cannot be snapshotted.");
        }
        if (runtimeSnapshot == null) {
            synchronized (this) {
                if (runtimeSnapshot == null) {
                    runtimeSnapshot = RuntimeSnapshot.build();
                }
            }
        }
        return runtimeSnapshot;
    }

    @Override
    public DistributionSnapshot getSnapshot(String key, CoreSession session) {
        if (RuntimeSnapshot.LIVE_ALIASES.contains(key)) {
            if (!canSeeRuntimeSnapshot(session)) {
                throw new RuntimeServiceException("Live runtime cannot be snapshotted.");
            }
            return getRuntimeSnapshot();
        }
        List<DistributionSnapshot> snaps = getPersistentSnapshots(session, key, true);
        if (snaps.isEmpty() && canSeeRuntimeSnapshot(session)) {
            DistributionSnapshot rtsnap = getRuntimeSnapshot();
            if (rtsnap.getKey().equals(key)) {
                return rtsnap;
            }
        }
        return snaps.get(0);
    }

    @Override
    public List<DistributionSnapshot> listPersistentSnapshots(CoreSession session) {
        List<DistributionSnapshot> distribs = RepositoryDistributionSnapshot.readPersistentSnapshots(session);
        Collections.sort(distribs,
                reverseOrder(comparing(DistributionSnapshot::getVersion)).thenComparing(DistributionSnapshot::getName));
        return distribs;
    }

    @Override
    public Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session) {
        Map<String, DistributionSnapshot> persistentSnapshots = new HashMap<>();
        for (DistributionSnapshot snap : RepositoryDistributionSnapshot.readPersistentSnapshots(session)) {
            persistentSnapshots.put(snap.getKey(), snap);
            for (String alias : snap.getAliases()) {
                persistentSnapshots.put(alias, snap);
            }
        }
        return persistentSnapshots;
    }

    @Override
    public List<DistributionSnapshot> getPersistentSnapshots(CoreSession session, String key, boolean includeAliases) {
        return RepositoryDistributionSnapshot.readPersistentSnapshots(session, key, includeAliases);
    }

    protected boolean canSeeRuntimeSnapshot(CoreSession session) {
        if (!isSiteMode()) {
            return SecurityHelper.canSnapshotLiveDistribution(session.getPrincipal());
        }
        return false;
    }

    @Override
    public List<DistributionSnapshotDesc> getAvailableDistributions(CoreSession session) {
        List<DistributionSnapshotDesc> distribs = RepositoryDistributionSnapshot.readPersistentSnapshots(session)
                                                                                .stream()
                                                                                .filter(snap -> !snap.isHidden())
                                                                                .collect(Collectors.toList());
        if (canSeeRuntimeSnapshot(session)) {
            distribs.add(0, getRuntimeSnapshot());
        }
        return distribs;
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session) {
        return persistRuntimeSnapshot(session, null, null, null, null);
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name,
            Map<String, Serializable> properties, List<String> reservedKeys, SnapshotFilter filter) {
        if (!canSeeRuntimeSnapshot(session)) {
            throw new RuntimeServiceException("Live runtime cannot be snapshotted.");
        }
        DistributionSnapshot liveSnapshot = getRuntimeSnapshot();
        return persister.persist(liveSnapshot, session, name, filter, properties, reservedKeys, getPlugins());
    }

    @Override
    public List<String> getAvailableVersions(CoreSession session, NuxeoArtifact nxItem) {
        List<String> versions = new ArrayList<>();

        List<DistributionSnapshot> distribs = new ArrayList<>();
        distribs.addAll(getPersistentSnapshots(session).values());
        if (canSeeRuntimeSnapshot(session)) {
            distribs.add(getRuntimeSnapshot());
        }

        for (DistributionSnapshot snap : distribs) {

            String version = null;
            if (BundleGroup.TYPE_NAME.equals(nxItem.getArtifactType())) {
                BundleGroup bg = snap.getBundleGroup(nxItem.getId());
                if (bg != null) {
                    version = bg.getVersion();
                }
            } else if (BundleInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                BundleInfo bi = snap.getBundle(nxItem.getId());
                if (bi != null) {
                    version = bi.getVersion();
                }
            } else if (ComponentInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ComponentInfo ci = snap.getComponent(nxItem.getId());
                if (ci != null) {
                    version = ci.getVersion();
                }
            } else if (ExtensionInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ExtensionInfo ei = snap.getContribution(nxItem.getId());
                if (ei != null) {
                    version = ei.getVersion();
                }
            } else if (ExtensionPointInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ExtensionPointInfo epi = snap.getExtensionPoint(nxItem.getId());
                if (epi != null) {
                    version = epi.getVersion();
                }
            } else if (ServiceInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ServiceInfo si = snap.getService(nxItem.getId());
                if (si != null) {
                    version = si.getVersion();
                }
            } else if (OperationInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                OperationInfo oi = snap.getOperation(nxItem.getId());
                if (oi != null) {
                    version = oi.getVersion();
                }
            }

            if (version != null && !versions.contains(version)) {
                versions.add(version);
            }
        }
        return versions;
    }

    @Override
    public void exportSnapshot(CoreSession session, String key, OutputStream out) throws IOException {

        DistributionSnapshot snap = getSnapshot(key, session);

        if (snap == null) {
            throw new NuxeoException("Unable to find Snapshot " + key);
        }

        if (snap.isLive()) {
            throw new NuxeoException("Can not export a live distribution snapshot : " + key);
        }

        RepositoryDistributionSnapshot docSnap = (RepositoryDistributionSnapshot) snap;
        DocumentModel root = docSnap.getDoc();

        DocumentReader reader = new DocumentTreeReader(session, root);
        DocumentWriter writer = new NuxeoArchiveWriter(out);
        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        reader.close();
        writer.close();
    }

    @Override
    public void importSnapshot(CoreSession session, InputStream is, Map<String, Serializable> properties,
            List<String> reservedKeys) throws IOException, DocumentValidationException {
        DocumentModel snapDoc = importTmpSnapshot(session, is);
        validateImportedSnapshot(session, snapDoc.getId(), properties, reservedKeys);
    }

    @Override
    public DocumentModel importTmpSnapshot(CoreSession session, InputStream is) throws IOException {
        DocumentModel container = persister.getDistributionRoot(session);
        DocumentRef tmpRef = new PathRef(container.getPathAsString(), IMPORT_TMP);

        // create tmp dir for import
        DocumentModel tmp;
        if (session.exists(tmpRef)) {
            tmp = session.getChild(container.getRef(), IMPORT_TMP);
            session.removeChildren(tmp.getRef());
        } else {
            tmp = session.createDocumentModel(container.getPathAsString(), IMPORT_TMP,
                    DistributionSnapshot.CONTAINER_TYPE_NAME);
            tmp.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, "tmpImport");
            tmp = session.createDocument(tmp);
            session.save();
        }

        // run import
        DocumentReader reader = new NuxeoArchiveReader(is);
        DocumentWriter writer = new DocumentModelWriter(session, tmp.getPathAsString()) {
            @Override
            protected void beforeCreateDocument(DocumentModel doc) {
                BaseNuxeoArtifactDocAdapter.fillContextData(doc);
            }
        };
        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        reader.close();
        writer.close();

        // make sure it's hidden until validation, and move it
        DocumentModel snapDoc = session.getChildren(tmp.getRef()).get(0);
        snapDoc.setPropertyValue(DistributionSnapshot.PROP_HIDE, true);
        BaseNuxeoArtifactDocAdapter.fillContextData(snapDoc);
        snapDoc = session.saveDocument(snapDoc);
        snapDoc = session.move(snapDoc.getRef(), session.getParentDocumentRef(tmp.getRef()), null);

        // cleanup tmp dir
        session.removeDocument(tmp.getRef());
        session.save();

        return snapDoc;
    }

    @Override
    public void validateImportedSnapshot(CoreSession session, String distribDocId, Map<String, Serializable> properties,
            List<String> reservedKeys) throws DocumentValidationException {
        DocumentModel snapDoc = session.getDocument(new IdRef(distribDocId));
        // update props
        var repoSnap = (RepositoryDistributionSnapshot) snapDoc.getAdapter(DistributionSnapshot.class);
        var finalProps = new HashMap<String, Serializable>();
        if (properties != null) {
            finalProps.putAll(properties);
        }
        // unhide distrib
        finalProps.put(DistributionSnapshot.PROP_HIDE, false);
        repoSnap.updateDocument(session, finalProps, null, reservedKeys);
    }

    @Override
    public void initWebContext(HttpServletRequest request) {
        if (isSiteMode()) {
            return;
        }
        try {
            DistributionSnapshot rsnap = getRuntimeSnapshot();
            for (Plugin<?> plugin : getPlugins()) {
                plugin.initWebContext(rsnap, request);
            }
        } catch (RuntimeServiceException e) {
            log.warn("Illegal access to runtime snapshot", e);
        }
    }

    @Override
    public List<Plugin<?>> getPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(plugins.values()));
    }

    @Override
    public Plugin<?> getPlugin(String id) {
        return plugins.get(id);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        fillRegistry(XP_PLUGINS, plugins, PluginDescriptor.class);
        fillRegistry(XP_EXPORTERS, exporters, ExporterDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    protected <T> void fillRegistry(String xp, Map<String, T> registry, Class<?> descriptorClass) {
        registry.clear();
        List<Descriptor> descriptors = getDescriptors(xp);
        for (Descriptor descriptor : descriptors) {
            try {
                Class<?> clazz = Class.forName(descriptor.getKlass());
                Constructor<?> constructor = clazz.getConstructor(descriptorClass);
                T instance = (T) constructor.newInstance(descriptor);
                registry.put(descriptor.getId(), instance);
            } catch (ReflectiveOperationException e) {
                String msg = String.format(
                        "Failed to register contribution with id '%s' on '%s': error initializing class '%s' (%s).",
                        descriptor.getId(), name, descriptor.getKlass(), e.toString());
                log.error(msg, e);
                addRuntimeMessage(Level.ERROR, msg);
            }
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        plugins.clear();
        exporters.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(SnapshotManager.class)) {
            return (T) this;
        } else if (adapter.isAssignableFrom(ArtifactSearcher.class)) {
            return (T) searcher;
        } else if (adapter.isAssignableFrom(SnapshotListener.class)) {
            return (T) componentListener;
        }
        return null;
    }

    @Override
    public boolean isSiteMode() {
        return Framework.isBooleanPropertyTrue(PROPERTY_SITE_MODE);
    }

    @Override
    public List<Exporter> getExporters() {
        return Collections.unmodifiableList(new ArrayList<>(exporters.values()));
    }

    @Override
    public Exporter getExporter(String id) {
        return exporters.get(id);
    }

}
