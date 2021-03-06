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

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

public class ComponentInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ComponentInfo {

    private static final Logger log = LogManager.getLogger(ComponentInfoDocAdapter.class);

    public ComponentInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public static ComponentInfoDocAdapter create(ComponentInfo componentInfo, CoreSession session,
            String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("component-" + componentInfo.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, componentInfo.getName());
        doc.setPropertyValue(PROP_COMPONENT_ID, componentInfo.getId());
        doc.setPropertyValue(PROP_COMPONENT_NAME, componentInfo.getName());
        doc.setPropertyValue(PROP_COMPONENT_CLASS, componentInfo.getComponentClass());
        doc.setPropertyValue(PROP_BUILT_IN_DOC, componentInfo.getDocumentation());
        doc.setPropertyValue(PROP_IS_XML, Boolean.valueOf(componentInfo.isXmlPureComponent()));
        doc.setPropertyValue(PROP_REQUIREMENTS, (Serializable) componentInfo.getRequirements());
        doc.setPropertyValue(PROP_RESOLUTION_ORDER, componentInfo.getResolutionOrder());
        doc.setPropertyValue(PROP_DECLARED_START_ORDER, componentInfo.getDeclaredStartOrder());
        doc.setPropertyValue(PROP_START_ORDER, componentInfo.getStartOrder());
        doc.setPropertyValue(PROP_ALIASES, (Serializable) componentInfo.getAliases());

        Blob xmlBlob = Blobs.createBlob(componentInfo.getXmlFileContent(), "text/xml", null,
                componentInfo.getXmlFileName());
        doc.setPropertyValue(NuxeoArtifact.CONTENT_PROPERTY_PATH, (Serializable) xmlBlob);

        fillContextData(doc);
        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new ComponentInfoDocAdapter(doc);
    }

    @Override
    public BundleInfo getBundle() {
        DocumentModel parent = getCoreSession().getDocument(doc.getParentRef());
        return parent.getAdapter(BundleInfo.class);
    }

    @Override
    public String getComponentClass() {
        return safeGet(PROP_COMPONENT_CLASS);
    }

    @Override
    public String getDocumentation() {
        return safeGet(PROP_BUILT_IN_DOC);
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    @Override
    public List<ExtensionPointInfo> getExtensionPoints() {
        String query = QueryHelper.select(ExtensionPointInfo.TYPE_NAME, doc, NXQL.ECM_POS);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(ExtensionPointInfo.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    @Override
    public List<ExtensionInfo> getExtensions() {
        String query = QueryHelper.select(ExtensionInfo.TYPE_NAME, doc, NXQL.ECM_POS);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(ExtensionInfo.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return safeGet(PROP_COMPONENT_NAME);
    }

    @Override
    public String getXmlFileContent() {
        return safeGetContent(safeGet(NuxeoArtifact.CONTENT_PROPERTY_PATH), "");
    }

    @Override
    public String getXmlFileName() {
        Blob xml = safeGet(NuxeoArtifact.CONTENT_PROPERTY_PATH);
        return xml == null ? "" : xml.getFilename() == null ? "" : xml.getFilename();
    }

    @Override
    public URL getXmlFileUrl() {
        return null;
    }

    @Override
    public boolean isXmlPureComponent() {
        Boolean isXml = safeGet(PROP_IS_XML, Boolean.TRUE);
        return isXml == null ? true : isXml.booleanValue();
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getVersion() {

        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle != null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for Component " + getId());
        return "?";
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public List<ServiceInfo> getServices() {
        String query = QueryHelper.select(ServiceInfo.TYPE_NAME, doc, NXQL.ECM_POS);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(ServiceInfo.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    @Override
    public List<String> getRequirements() {
        return safeGet(PROP_REQUIREMENTS);
    }

    @Override
    public Long getResolutionOrder() {
        return safeGet(PROP_RESOLUTION_ORDER);
    }

    @Override
    public void setResolutionOrder(Long order) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getDeclaredStartOrder() {
        return safeGet(PROP_DECLARED_START_ORDER);
    }

    @Override
    public void setDeclaredStartOrder(Long order) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getStartOrder() {
        return safeGet(PROP_START_ORDER);
    }

    @Override
    public void setStartOrder(Long order) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OperationInfo> getOperations() {
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, doc, OperationInfo.PROP_CONTRIBUTING_COMPONENT,
                getName());
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream().map(doc -> doc.getAdapter(OperationInfo.class)).collect(Collectors.toList());
    }

    @Override
    public List<String> getAliases() {
        return safeGet(PROP_ALIASES);
    }

}
