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

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.export.graphs.api.Node;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node implentation with positioning information.
 *
 * @since 20.0.0
 */
@JsonIgnoreType
public class PositionedNodeImpl<T extends NuxeoArtifact> extends NodeImpl<T> {

    protected float x;

    protected float y;

    protected float z;

    public PositionedNodeImpl(@JsonProperty("id") String id, @JsonProperty("label") String label,
            @JsonProperty("type") String type, @JsonProperty("weight") int weight) {
        this(id, label, type, weight, null);
    }

    public PositionedNodeImpl(@JsonProperty("id") String id, @JsonProperty("label") String label,
            @JsonProperty("type") String type, @JsonProperty("weight") int weight, T object) {
        super(id, label, type, weight, object);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    @Override
    public Node<T> copy() {
        PositionedNodeImpl<T> copy = new PositionedNodeImpl<>(id, label, type, weight, object);
        copy.setAttributes(getAttributes());
        copy.setX(x);
        copy.setY(y);
        copy.setZ(z);
        return copy;
    }

}
