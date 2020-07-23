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
package org.nuxeo.apidoc.export.graphs.introspection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.export.graphs.api.Node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 20.0.0
 */
@JsonIgnoreType
public class NodeImpl<T extends NuxeoArtifact> implements Node<T> {

    protected final String id;

    protected final String label;

    protected final String type;

    protected final T object;

    protected int weight = 0;

    protected Map<String, String> attributes = new HashMap<>();

    @JsonCreator
    public NodeImpl(@JsonProperty("id") String id, @JsonProperty("label") String label,
            @JsonProperty("type") String type, @JsonProperty("weight") int weight) {
        this(id, label, type, weight, null);
    }

    @JsonCreator
    public NodeImpl(@JsonProperty("id") String id, @JsonProperty("label") String label,
            @JsonProperty("type") String type, @JsonProperty("weight") int weight, T object) {
        super();
        this.id = id;
        this.label = label;
        this.type = type;
        this.object = object;
        this.weight = weight;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public T getObject() {
        return object;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String getAttribute(String key, String defaultValue) {
        if (attributes.containsKey(key)) {
            return attributes.get(key);
        }
        return defaultValue;
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        this.attributes.clear();
        this.attributes.putAll(attributes);
    }

    @Override
    public void setAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    @Override
    public Node<T> copy() {
        NodeImpl<T> copy = new NodeImpl<>(id, label, type, weight, object);
        copy.setAttributes(getAttributes());
        return copy;
    }

}
