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
package org.nuxeo.apidoc.export.api;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.apidoc.api.Descriptor;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for export generators.
 *
 * @since 20.0.0
 */
@XObject("exporter")
public class ExporterDescriptor implements Descriptor {

    @XNode("@id")
    String id;

    @XNode("@class")
    String klass;

    @XNodeMap(value = "properties/property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getKlass() {
        return klass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
