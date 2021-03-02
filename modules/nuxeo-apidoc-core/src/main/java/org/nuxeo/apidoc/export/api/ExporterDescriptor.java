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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.Descriptor;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
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

    @XNode("title")
    String title;

    @XNode("description")
    String description;

    @XNode("filename")
    String filename;

    @XNode("mimetype")
    String mimetype;

    @XNodeList(value = "display/on", type = ArrayList.class, componentType = String.class, nullByDefault = true)
    List<String> displays;

    @XNodeMap(value = "properties/property", key = "@name", type = HashMap.class, componentType = String.class, nullByDefault = true)
    Map<String, String> properties;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getKlass() {
        return klass;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimetype() {
        return mimetype;
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(properties);
    }

    public boolean displayOn(String page) {
        return displays != null && displays.contains(page);
    }

    @Override
    public org.nuxeo.runtime.model.Descriptor merge(org.nuxeo.runtime.model.Descriptor o) {
        ExporterDescriptor other = (ExporterDescriptor) o;
        ExporterDescriptor merged = new ExporterDescriptor();
        merged.id = id;
        merged.klass = other.klass != null ? other.klass : klass;
        merged.title = other.title != null ? other.title : title;
        merged.description = other.description != null ? other.description : description;
        merged.filename = other.filename != null ? other.filename : filename;
        merged.mimetype = other.mimetype != null ? other.mimetype : mimetype;
        merged.displays = other.displays != null ? other.displays : displays;
        merged.properties = other.properties != null ? other.properties : properties;
        return merged;
    }

}
