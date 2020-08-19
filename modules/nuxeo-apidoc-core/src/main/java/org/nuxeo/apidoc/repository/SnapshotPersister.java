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
package org.nuxeo.apidoc.repository;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.OperationInfoDocAdapter;
import org.nuxeo.apidoc.adapters.PackageInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;

public class SnapshotPersister {

    private static final Logger log = LogManager.getLogger(SnapshotPersister.class);

    public static final String Root_PATH = "/";

    public static final String Root_NAME = "nuxeo-distributions";

    public static final String Operation_Root_NAME = "Automation";

    public static final String Bundle_Root_NAME = "Bundles";

    /** @since 11.1 */
    public static final String PACKAGE_ROOT_NAME = "Packages";

    public DocumentModel getSubRoot(CoreSession session, DocumentModel root, String name) {
        DocumentRef rootRef = new PathRef(root.getPathAsString() + name);
        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }
        return createRoot(session, root.getPathAsString(), name, false);
    }

    public DocumentModel getDistributionRoot(CoreSession session) {
        DocumentRef rootRef = new PathRef(Root_PATH + Root_NAME);
        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }
        return CoreInstance.doPrivileged(session.getRepositoryName(), privilegedSession -> {
            return createRoot(privilegedSession, Root_PATH, Root_NAME, true);
        });
    }

    /**
     * Creates a workspace folder and returns it.
     *
     * @since 20.0.0
     */
    public static DocumentModel createRoot(CoreSession session, String parentPath, String name, boolean setAcl) {
        DocumentModel root = session.createDocumentModel(parentPath, name, DistributionSnapshot.CONTAINER_TYPE_NAME);
        root.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, name);
        root = session.createDocument(root);

        if (setAcl) {
            ACL acl = new ACLImpl();
            acl.add(new ACE(SecurityHelper.getApidocReadersGroup(), SecurityConstants.READ, true));
            acl.add(new ACE(SecurityHelper.getApidocManagersGroup(), SecurityConstants.WRITE, true));
            ACP acp = root.getACP();
            acp.addACL(acl);
            session.setACP(root.getRef(), acp, true);
        }

        // flush caches
        session.save();
        return session.getDocument(root.getRef());
    }

    public DistributionSnapshot persist(DistributionSnapshot snapshot, CoreSession session, String label,
            SnapshotFilter filter, Map<String, Serializable> properties, List<String> reservedKeys,
            List<Plugin<?>> plugins) throws DocumentValidationException {

        RepositoryDistributionSnapshot distribContainer = RepositoryDistributionSnapshot.create(snapshot, session,
                getDistributionRoot(session).getPathAsString(), label, properties, reservedKeys);

        distribContainer.cleanPreviousArtifacts();

        DocumentModel bundleContainer = getSubRoot(session, distribContainer.getDoc(), Bundle_Root_NAME);

        if (filter != null) {
            // create VGroup that contains only the target bundles
            BundleGroupImpl vGroup = new BundleGroupImpl(filter.getName());
            vGroup.setVersion(snapshot.getVersion());
            List<NuxeoArtifact> selectedBundles = new ArrayList<NuxeoArtifact>();
            for (BundleInfo bundle : snapshot.getBundles()) {
                if (filter.accept(bundle)) {
                    selectedBundles.add(bundle);
                    vGroup.add(bundle.getId());
                }
            }

            persistBundleGroup(snapshot, filter, vGroup, session, label + "-bundles", bundleContainer);

            Class<? extends SnapshotFilter> refClass = filter.getReferenceClass();
            if (refClass != null) {
                try {
                    String refFilterName = filter.getName() + SnapshotFilter.REFERENCE_FILTER_NAME_SUFFIX;
                    Constructor<? extends SnapshotFilter> constructor = refClass.getConstructor(String.class,
                            List.class);
                    SnapshotFilter refFilter = constructor.newInstance(refFilterName, selectedBundles);
                    // create VGroup that contains only the reference bundles
                    BundleGroupImpl reference = new BundleGroupImpl(refFilterName);
                    reference.setVersion(snapshot.getVersion());
                    snapshot.getBundles()
                            .stream()
                            .filter(refFilter::accept)
                            .map(BundleInfo::getId)
                            .forEach(reference::add);
                    persistBundleGroup(snapshot, refFilter, reference, session, refFilterName, bundleContainer);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }

        } else {
            List<BundleGroup> bundleGroups = snapshot.getBundleGroups();
            for (BundleGroup bundleGroup : bundleGroups) {
                persistBundleGroup(snapshot, null, bundleGroup, session, label, bundleContainer);
            }
        }

        DocumentModel opContainer = getSubRoot(session, distribContainer.getDoc(), Operation_Root_NAME);
        persistOperations(snapshot, snapshot.getOperations(), session, label, opContainer, filter);

        DocumentModel packagesContainer = getSubRoot(session, distribContainer.getDoc(), PACKAGE_ROOT_NAME);
        persistPackages(snapshot, snapshot.getPackages(), session, label, packagesContainer, filter);

        // handle plugins persistence
        for (Plugin<?> plugin : plugins) {
            plugin.persist(snapshot, session, distribContainer.getDoc(), filter);
        }

        // needed for tests
        session.save();

        return distribContainer;
    }

    protected void persistBundleGroup(DistributionSnapshot snapshot, SnapshotFilter filter, BundleGroup bundleGroup,
            CoreSession session, String label, DocumentModel parent) {
        if (log.isTraceEnabled()) {
            log.trace("Persist bundle group " + bundleGroup.getId());
        }
        DocumentModel bundleGroupDoc = BundleGroupDocAdapter.create(bundleGroup, session, parent.getPathAsString())
                                                            .getDoc();

        for (String bundleId : bundleGroup.getBundleIds()) {
            persistBundle(snapshot, filter, snapshot.getBundle(bundleId), session, label, bundleGroupDoc);
        }

        for (BundleGroup subGroup : bundleGroup.getSubGroups()) {
            persistBundleGroup(snapshot, filter, subGroup, session, label, bundleGroupDoc);
        }
    }

    protected void persistBundle(DistributionSnapshot snapshot, SnapshotFilter filter, BundleInfo bundleInfo,
            CoreSession session, String label, DocumentModel parent) {
        if (log.isTraceEnabled()) {
            log.trace("Persist bundle " + bundleInfo.getId());
        }
        DocumentModel bundleDoc = BundleInfoDocAdapter.create(bundleInfo, session, parent.getPathAsString()).getDoc();

        for (ComponentInfo ci : bundleInfo.getComponents()) {
            if (filter == null || filter.accept(ci)) {
                persistComponent(snapshot, filter, ci, session, label, bundleDoc);
            }
        }
    }

    protected void persistComponent(DistributionSnapshot snapshot, SnapshotFilter filter, ComponentInfo ci,
            CoreSession session, String label, DocumentModel parent) {

        DocumentModel componentDoc = ComponentInfoDocAdapter.create(ci, session, parent.getPathAsString()).getDoc();
        String componentDocPath = componentDoc.getPathAsString();

        for (ExtensionPointInfo epi : ci.getExtensionPoints()) {
            if (filter == null || filter.accept(epi)) {
                ExtensionPointInfoDocAdapter.create(epi, session, componentDocPath);
            }
        }

        for (ServiceInfo si : ci.getServices()) {
            if (filter == null || filter.accept(si)) {
                ServiceInfoDocAdapter.create(si, session, componentDocPath);
            }
        }

        for (ExtensionInfo ei : ci.getExtensions()) {
            if (filter == null || filter.accept(ei)) {
                ExtensionInfoDocAdapter.create(ei, 0, session, componentDocPath);
            }
        }
    }

    protected void persistOperations(DistributionSnapshot snapshot, List<OperationInfo> operations, CoreSession session,
            String label, DocumentModel parent, SnapshotFilter filter) {
        for (OperationInfo op : operations) {
            if (filter == null || filter.accept(op)) {
                OperationInfoDocAdapter.create(op, session, parent.getPathAsString());
            }
        }
    }

    protected void persistPackages(DistributionSnapshot snapshot, List<PackageInfo> packages, CoreSession session,
            String label, DocumentModel parent, SnapshotFilter filter) {
        for (PackageInfo pkg : packages) {
            if (filter == null || filter.accept(pkg)) {
                PackageInfoDocAdapter.create(pkg, session, parent.getPathAsString());
            }
        }
    }

}
