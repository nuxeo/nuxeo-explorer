/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.apidoc.api;

import java.util.List;

import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;

import com.fasterxml.jackson.annotation.JsonBackReference;

public interface ExtensionInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXContribution";

    String PROP_CONTRIB_ID = "nxcontribution:contribId";

    String PROP_DOC = "nxcontribution:documentation";

    String PROP_EXTENSION_POINT = "nxcontribution:extensionPoint";

    String PROP_TARGET_COMPONENT_NAME = "nxcontribution:targetComponentName";

    String PROP_TARGET_REGISTRATION_ORDER = "nxcontribution:registrationOrder";

    /**
     * Returns a key combining the target component name and the extension point name.
     */
    String getExtensionPoint();

    String getDocumentation();

    String getDocumentationHtml();

    ComponentName getTargetComponentName();

    String getXml();

    List<ContributionItem> getContributionItems();

    @JsonBackReference("extension")
    ComponentInfo getComponent();

    /**
     * Returns the registration order on target extension point, as indicated by {@link ComponentManager}.
     *
     * @since 20.0.0
     */
    Long getRegistrationOrder();

    /**
     * Sets the registration order on target extension point, as indicated by {@link ComponentManager}.
     *
     * @since 20.0.0
     */
    void setRegistrationOrder(Long order);

    /**
     * Returns the locally computed unique id for a contribution.
     *
     * @since 20.0.0
     */
    public static String computeId(String componentName, String extensionPoint) {
        return computeId(componentName, extensionPoint, -1);
    }

    /**
     * Returns the locally computed unique id for a contribution.
     * <p>
     * Index accounts for potential multiple contributions to the same extension point (or another component with that
     * same extension point name...).
     *
     * @since 20.0.0
     */
    public static String computeId(String componentName, String extensionPoint, long index) {
        String id = componentName + "--" + extensionPoint;
        if (index > 0) {
            id += index;
        }
        return id;
    }

}
