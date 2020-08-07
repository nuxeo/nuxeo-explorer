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

import org.nuxeo.apidoc.export.graphs.api.Edge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 20.0.0
 */
@JsonIgnoreType
public class EdgeImpl implements Edge {

    protected final String source;

    protected final String target;

    protected final String value;

    protected int weight = 0;

    @JsonCreator
    public EdgeImpl(@JsonProperty("source") String source, @JsonProperty("target") String target,
            @JsonProperty("value") String value, @JsonProperty("weight") int weight) {
        super();
        this.source = source;
        this.target = target;
        this.value = value;
        this.weight = weight;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getValue() {
        return value;
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
    public Edge copy() {
        return new EdgeImpl(source, target, value, weight);
    }

}
