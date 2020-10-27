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
package org.nuxeo.apidoc.adapters;

import static org.nuxeo.ecm.core.api.validation.DocumentValidationService.CTX_MAP_KEY;
import static org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing.TURN_OFF;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.picture.listener.PictureViewsGenerationListener;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class BaseNuxeoArtifactDocAdapter extends BaseNuxeoArtifact {

    private static final Logger log = LogManager.getLogger(BaseNuxeoArtifactDocAdapter.class);

    protected DocumentModel doc;

    protected static final ThreadLocal<CoreSession> localCoreSession = new ThreadLocal<>();

    protected static final String LISTING_LIMIT_PROPERTY = "org.nuxeo.apidoc.listing.limit";

    protected static final String DEFAULT_LISTING_LIMIT = "2000";

    public static void setLocalCoreSession(CoreSession session) {
        localCoreSession.set(session);
    }

    public static void releaseLocalCoreSession() {
        localCoreSession.remove();
    }

    protected BaseNuxeoArtifactDocAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    protected static String computeDocumentName(String name) {
        return IdUtils.generateId(name, "-", true, 500);
    }

    protected static int getListingLimit() {
        return Integer.valueOf(Framework.getProperty(LISTING_LIMIT_PROPERTY, DEFAULT_LISTING_LIMIT));
    }

    protected static String getRootPath(CoreSession session, String basePath, String suffix) {
        PathRef rootRef = new PathRef(basePath);
        if (session.exists(rootRef)) {
            Path path = new Path(basePath).append(suffix);
            rootRef = new PathRef(path.toString());
            if (session.exists(rootRef)) {
                return path.toString();
            } else {
                DocumentModel root = session.createDocumentModel("Folder");
                root.setPathInfo(basePath, suffix);
                root = session.createDocument(root);
                return root.getPathAsString();
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return doc.getId().hashCode();
    }

    @JsonIgnore
    public DocumentModel getDoc() {
        return doc;
    }

    protected CoreSession getCoreSession() {
        CoreSession session = null;
        if (doc != null) {
            session = doc.getCoreSession();
        }
        if (session == null) {
            session = localCoreSession.get();
        }
        return session;
    }

    protected <T> T getParentNuxeoArtifact(Class<T> artifactClass) {
        List<DocumentModel> parents = getCoreSession().getParentDocuments(doc.getRef());
        return parents.stream()
                      .map(doc -> doc.getAdapter(artifactClass))
                      .filter(Objects::nonNull)
                      .findFirst()
                      .orElse(null);
    }

    protected <T> T safeGet(String xPath) {
        return safeGet(xPath, null);
    }

    @SuppressWarnings("unchecked")
    protected <T> T safeGet(String xPath, Object defaultValue) {
        try {
            T value = (T) doc.getPropertyValue(xPath);
            return value;
        } catch (PropertyException e) {
            log.error("Error while getting property " + xPath, e);
            if (defaultValue == null) {
                return null;
            }
            return (T) defaultValue;
        }
    }

    protected String safeGetContent(Blob blob, String defaultValue) {
        if (blob == null) {
            return defaultValue;
        }
        if (StringUtils.isBlank(blob.getEncoding())) {
            blob.setEncoding(StandardCharsets.UTF_8.name());
        }
        try {
            return blob.getString();
        } catch (IOException e) {
            log.error("Error while reading blob", e);
            return defaultValue;
        }
    }

    @Override
    public String getHierarchyPath() {
        List<DocumentModel> parents = getCoreSession().getParentDocuments(doc.getRef());
        Collections.reverse(parents);

        String path = "";
        for (DocumentModel doc : parents) {
            if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
                break;
            }
            if (doc.getType().equals(DistributionSnapshot.CONTAINER_TYPE_NAME)) {
                // skip containers
                continue;
            }
            NuxeoArtifact item = doc.getAdapter(NuxeoArtifact.class);

            path = "/" + item.getId() + path;
        }
        return path;
    }

    /**
     * Fills context data to avoid useless processings on snapshot documents create/update.
     *
     * @since 20.0.0
     */
    public static void fillContextData(DocumentModel doc) {
        // disable validation
        doc.putContextData(CTX_MAP_KEY, TURN_OFF);
        // NXP-28928: disable useless thumbnail computation for explorer documents: could be costly and is not needed in
        // this context
        doc.putContextData(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION, true);
        // NXP-29435: disable picture view computation even if explorer documents should not actually go through it,
        // hoping it speeds things in some edge cases (see server logs attached to NXP-29433)
        doc.putContextData(PictureViewsGenerationListener.DISABLE_PICTURE_VIEWS_GENERATION_LISTENER, true);
    }

    protected static DocumentModelList query(CoreSession session, String query) {
        if (Framework.isBooleanPropertyTrue(SnapshotManager.PROPERTY_USE_ES)) {
            ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
            return ess.query(new NxQueryBuilder(session).nxql(query).limit(getListingLimit()));
        } else {
            return session.query(query);
        }
    }

    protected static List<String> queryAndFetchIds(CoreSession session, String idProp, String type,
            DocumentModel parent, String order) {
        String query = QueryHelper.select(idProp, type, parent, order);
        PartialList<Map<String, Serializable>> res = session.queryProjection(query, getListingLimit(), 0);
        return res.stream().map(e -> e.get(idProp)).map(String.class::cast).collect(Collectors.toList());
    }

}
