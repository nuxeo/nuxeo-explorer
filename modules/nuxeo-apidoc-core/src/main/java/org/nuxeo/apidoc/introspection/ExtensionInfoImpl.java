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

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.DocumentException;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.documentation.XMLContributionParser;
import org.nuxeo.runtime.model.ComponentName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;

public class ExtensionInfoImpl extends BaseNuxeoArtifact implements ExtensionInfo {

    private static final Logger log = LogManager.getLogger(ExtensionInfoImpl.class);

    protected final String id;

    protected final ComponentInfo component;

    protected String extensionPoint;

    protected String documentation;

    protected String xml;

    protected ComponentName targetComponentName;

    protected Object[] contribution;

    /** @since 20.0.0 */
    protected Long registrationOrder;

    public ExtensionInfoImpl(ComponentInfo component, String extensionPoint, long index) {
        this.id = ExtensionInfo.computeId(component.getName(), extensionPoint, index);
        this.component = component;
        this.extensionPoint = extensionPoint;
    }

    @JsonCreator
    private ExtensionInfoImpl() {
        this.id = null;
        this.component = null;
    }

    @Override
    public String getExtensionPoint() {
        return ExtensionPointInfo.computeId(targetComponentName.getName(), extensionPoint);
    }

    @JsonSetter("extensionPoint")
    private void setExtensionPoint(String xp) {
        if (xp != null) {
            this.extensionPoint = xp.substring(xp.lastIndexOf("--") + 2);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    @Override
    public ComponentName getTargetComponentName() {
        return targetComponentName;
    }

    public void setTargetComponentName(ComponentName targetComponentName) {
        this.targetComponentName = targetComponentName;
    }

    @Override
    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    @Override
    public String getVersion() {
        return component.getVersion();
    }

    @Override
    public String getArtifactType() {
        return ExtensionInfo.TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return component.getHierarchyPath() + "/" + VirtualNodesConsts.Contributions_VNODE_NAME + "/" + getId();
    }

    @Override
    public List<ContributionItem> getContributionItems() {
        try {
            return XMLContributionParser.extractContributionItems(getXml());
        } catch (DocumentException e) {
            log.error(e, e);
            return Collections.emptyList();
        }
    }

    public void setContributionItems(List<ContributionItem> contributions) {
        // NOOP, useful for jackson deserialization
    }

    @Override
    public ComponentInfo getComponent() {
        return component;
    }

    @Override
    public Long getRegistrationOrder() {
        return registrationOrder;
    }

    @Override
    public void setRegistrationOrder(Long order) {
        this.registrationOrder = order;
    }

}
