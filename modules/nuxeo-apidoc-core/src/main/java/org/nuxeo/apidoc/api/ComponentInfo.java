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

import java.net.URL;
import java.util.List;

import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentStartOrders;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;

public interface ComponentInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXComponent";

    String PROP_COMPONENT_ID = "nxcomponent:componentId";

    String PROP_COMPONENT_NAME = "nxcomponent:componentName";

    String PROP_COMPONENT_CLASS = "nxcomponent:componentClass";

    String PROP_BUILT_IN_DOC = "nxcomponent:builtInDocumentation";

    /** @since 11.1 */
    String PROP_REQUIREMENTS = "nxcomponent:requirements";

    /** @since 20.0.0 */
    String PROP_RESOLUTION_ORDER = "nxcomponent:resolutionOrder";

    /** @since 20.0.0 */
    String PROP_DECLARED_START_ORDER = "nxcomponent:declaredStartOrder";

    /** @since 20.0.0 */
    String PROP_START_ORDER = "nxcomponent:startOrder";

    String PROP_IS_XML = "nxcomponent:isXML";

    /** @since 22.0.0 */
    String PROP_ALIASES = "nxcomponent:aliases";

    @Override
    @JsonIgnore
    String getId();

    String getName();

    @JsonBackReference("bundle")
    BundleInfo getBundle();

    @JsonManagedReference("extensionpoint")
    List<ExtensionPointInfo> getExtensionPoints();

    @JsonManagedReference("extension")
    List<ExtensionInfo> getExtensions();

    String getDocumentation();

    String getDocumentationHtml();

    @JsonManagedReference("service")
    List<ServiceInfo> getServices();

    String getComponentClass();

    boolean isXmlPureComponent();

    @JsonIgnore
    URL getXmlFileUrl();

    String getXmlFileName();

    String getXmlFileContent();

    /**
     * Returns the requirements set in the component declaration.
     *
     * @since 11.1
     */
    List<String> getRequirements();

    /**
     * Returns the resolution order as indicated by {@link ComponentManager}.
     *
     * @since 20.0.0
     */
    Long getResolutionOrder();

    /**
     * Sets the resolution order as indicated by {@link ComponentManager}.
     *
     * @since 20.0.0
     */
    void setResolutionOrder(Long order);

    /**
     * Returns the start order as indicated by {@link ComponentManager}.
     *
     * @since 20.0.0
     */
    Long getStartOrder();

    /**
     * Sets the start order as indicated by {@link ComponentManager}.
     *
     * @since 20.0.0
     */
    void setStartOrder(Long order);

    /**
     * Returns the declared start order as indicated by {@link Component#getApplicationStartedOrder()}.
     * <p>
     * Returns null if no particular order is set, although the framework will use 0 for XML components, and
     * {@link ComponentStartOrders#DEFAULT} for java components.
     *
     * @since 20.0.0
     */
    Long getDeclaredStartOrder();

    /**
     * Sets the declared start order as indicated by {@link Component#getApplicationStartedOrder()}.
     *
     * @since 20.0.0
     */
    void setDeclaredStartOrder(Long order);

    /** @since 20.0.0 */
    @JsonIgnore
    List<OperationInfo> getOperations();

    /** @since 22.0.0 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> getAliases();

}
