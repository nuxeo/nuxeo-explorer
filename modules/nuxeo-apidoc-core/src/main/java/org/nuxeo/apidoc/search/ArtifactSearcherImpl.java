/*
 * (C) Copyright 2006-2025 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.apidoc.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.runtime.api.Framework;

public class ArtifactSearcherImpl implements ArtifactSearcher {

    protected NuxeoArtifact mapDoc2Artifact(DocumentModel doc) {
        return switch (doc.getType()) {
            case BundleGroup.TYPE_NAME -> new BundleGroupDocAdapter(doc);
            case BundleInfo.TYPE_NAME -> new BundleInfoDocAdapter(doc);
            case ComponentInfo.TYPE_NAME -> new ComponentInfoDocAdapter(doc);
            case ExtensionPointInfo.TYPE_NAME -> new ExtensionPointInfoDocAdapter(doc);
            case ExtensionInfo.TYPE_NAME -> new ExtensionInfoDocAdapter(doc);
            case DistributionSnapshot.TYPE_NAME -> new RepositoryDistributionSnapshot(doc);
            case ServiceInfo.TYPE_NAME -> new ServiceInfoDocAdapter(doc);
            default -> null;
        };
    }

    @Override
    public List<NuxeoArtifact> searchArtifact(CoreSession session, String distribId, String fulltext) {
        List<NuxeoArtifact> result = new ArrayList<>();

        DistributionSnapshot snap = Framework.getService(SnapshotManager.class).getSnapshot(distribId, session);
        if (!(snap instanceof RepositoryDistributionSnapshot)) {
            return Collections.emptyList();
        }

        DocumentModel dist = ((RepositoryDistributionSnapshot) snap).getDoc();
        String query = "SELECT * FROM Document WHERE ecm:path STARTSWITH '" + dist.getPathAsString() + "'";
        if (fulltext != null) {
            query += " AND " + NXQL.ECM_FULLTEXT + " = " + NXQL.escapeString(fulltext);
        }

        DocumentModelList docs = Framework.getService(SearchService.class)
                                          .search(SearchQuery.builder(query, session).limit(MAX_RESULTS).build())
                                          .loadDocuments(session);
        for (DocumentModel doc : docs) {
            NuxeoArtifact artifact = mapDoc2Artifact(doc);
            if (artifact != null) {
                result.add(artifact);
            }
        }
        return result;
    }

    @Override
    public List<NuxeoArtifact> filterArtifact(CoreSession session, String distribId, String type, String fulltext) {
        List<NuxeoArtifact> result = new ArrayList<>();

        List<NuxeoArtifact> matchingArtifacts = searchArtifact(session, distribId, fulltext);

        Map<String, ArtifactWithWeight> sortMap = new HashMap<>();

        for (NuxeoArtifact matchingArtifact : matchingArtifacts) {
            ArtifactWithWeight artifactWithWeight;
            NuxeoArtifact matchingParentArtifact = resolveInTree(session, distribId, matchingArtifact, type);
            if (matchingParentArtifact != null) {
                artifactWithWeight = new ArtifactWithWeight(matchingParentArtifact);
            } else if (matchingArtifact.getArtifactType().equals(type)) {
                artifactWithWeight = new ArtifactWithWeight(matchingArtifact);
            } else {
                continue;
            }

            String id = artifactWithWeight.getArtifact().getId();
            if (sortMap.containsKey(id)) {
                sortMap.get(id).addHit();
            } else {
                sortMap.put(id, new ArtifactWithWeight(matchingParentArtifact));
            }
        }

        List<ArtifactWithWeight> artifacts = new ArrayList<>(sortMap.values());
        Collections.sort(artifacts);

        for (ArtifactWithWeight item : artifacts) {
            result.add(item.getArtifact());
        }
        return result;
    }

    protected NuxeoArtifact resolveInTree(CoreSession session, String distribId, NuxeoArtifact matchingArtifact,
            String searchedType) {
        String cType = matchingArtifact.getArtifactType();
        if (cType.equals(searchedType)) {
            return matchingArtifact;
        }
        BaseNuxeoArtifactDocAdapter docAdapter = (BaseNuxeoArtifactDocAdapter) matchingArtifact;
        DocumentModel doc = docAdapter.getDoc();
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        Collections.reverse(parents);
        for (DocumentModel parent : parents) {
            if (parent.getType().equals(searchedType)) {
                return mapDoc2Artifact(parent);
            }
        }
        return null;
    }

}
