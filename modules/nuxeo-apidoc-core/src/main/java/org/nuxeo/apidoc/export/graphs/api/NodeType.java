/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.export.graphs.api;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;

/**
 * @since 20.0.0
 */
public enum NodeType {

    UNDEFINED(-1), BUNDLE(0), COMPONENT(1), SERVICE(2), EXTENSION_POINT(2), CONTRIBUTION(3), OPERATION(4), PACKAGE(5);

    private int zindex;

    private NodeType(int zindex) {
        this.zindex = zindex;
    }

    @Override
    public String toString() {
        return name();
    }

    public String getLabel() {
        return StringUtils.capitalize(name().toLowerCase(Locale.ENGLISH));
    }

    public int getZIndex() {
        return zindex;
    }

    public String prefix(String id) {
        String prefix;
        switch (this) {
        case BUNDLE:
            prefix = BundleInfo.TYPE_NAME;
            break;
        case COMPONENT:
            prefix = ComponentInfo.TYPE_NAME;
            break;
        case SERVICE:
            prefix = ServiceInfo.TYPE_NAME;
            break;
        case EXTENSION_POINT:
            prefix = ExtensionPointInfo.TYPE_NAME;
            break;
        case CONTRIBUTION:
            prefix = ExtensionInfo.TYPE_NAME;
            break;
        case OPERATION:
            prefix = OperationInfo.TYPE_NAME;
            break;
        case PACKAGE:
            prefix = PackageInfo.TYPE_NAME;
            break;
        default:
            prefix = UNDEFINED.name();
        }
        return prefix + "-" + id;
    }

    @SuppressWarnings("incomplete-switch")
    public String unprefix(String id) {
        switch (this) {
        case BUNDLE:
            return id.substring(BundleInfo.TYPE_NAME.length() + 1);
        case COMPONENT:
            return id.substring(ComponentInfo.TYPE_NAME.length() + 1);
        case SERVICE:
            return id.substring(ServiceInfo.TYPE_NAME.length() + 1);
        case EXTENSION_POINT:
            return id.substring(ExtensionPointInfo.TYPE_NAME.length() + 1);
        case CONTRIBUTION:
            return id.substring(ExtensionInfo.TYPE_NAME.length() + 1);
        case OPERATION:
            return id.substring(OperationInfo.TYPE_NAME.length() + 1);
        case PACKAGE:
            return id.substring(PackageInfo.TYPE_NAME.length() + 1);
        }
        return id;
    }

    public static NodeType guess(String id) {
        if (id.startsWith(BundleInfo.TYPE_NAME)) {
            return BUNDLE;
        } else if (id.startsWith(ComponentInfo.TYPE_NAME)) {
            return COMPONENT;
        } else if (id.startsWith(ExtensionPointInfo.TYPE_NAME)) {
            return EXTENSION_POINT;
        } else if (id.startsWith(ServiceInfo.TYPE_NAME)) {
            return SERVICE;
        } else if (id.startsWith(ExtensionInfo.TYPE_NAME)) {
            return CONTRIBUTION;
        } else if (id.startsWith(OperationInfo.TYPE_NAME)) {
            return OPERATION;
        } else if (id.startsWith(PackageInfo.TYPE_NAME)) {
            return PACKAGE;
        }
        return UNDEFINED;
    }

    public static NodeType getType(String type) {
        for (NodeType ntype : NodeType.values()) {
            if (ntype.name().equalsIgnoreCase(type)) {
                return ntype;
            }
        }
        return UNDEFINED;
    }

}
