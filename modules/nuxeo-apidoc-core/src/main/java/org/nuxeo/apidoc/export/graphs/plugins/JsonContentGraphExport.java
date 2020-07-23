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
package org.nuxeo.apidoc.export.graphs.plugins;

import java.util.LinkedHashMap;

import org.nuxeo.apidoc.export.graphs.api.Edge;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.introspection.ContentGraphExport;
import org.nuxeo.apidoc.export.graphs.introspection.EdgeImpl;
import org.nuxeo.apidoc.export.graphs.introspection.NodeImpl;
import org.nuxeo.apidoc.snapshot.JsonMapper;
import org.nuxeo.apidoc.snapshot.JsonPrettyPrinter;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Exporter for Json graph format.
 *
 * @since 20.0.0
 */
public class JsonContentGraphExport extends ContentGraphExport {

    protected static final String NAME = "graph.json";

    protected static final String MIMETYPE = "application/json";

    public JsonContentGraphExport() {
        super();
    }

    @Override
    public void updateContent(GraphExport graph) {
        init(graph);

        final ObjectMapper mapper = JsonMapper.basic(null);
        mapper.registerModule(new SimpleModule().addAbstractTypeMapping(Node.class, NodeImpl.class)
                                                .addAbstractTypeMapping(Edge.class, EdgeImpl.class));
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("name", graph.getName());
        values.put("title", graph.getTitle());
        values.put("description", graph.getDescription());
        values.put("type", graph.getType());
        if (!graph.getProperties().isEmpty()) {
            values.put("properties", graph.getProperties());
        }
        values.put("nodes", graph.getNodes());
        values.put("edges", graph.getEdges());
        try {
            ObjectWriter writer = mapper.writerFor(LinkedHashMap.class)
                                        .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                                        .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            if (Boolean.valueOf(graph.getProperty("pretty", "false"))) {
                writer = writer.with(new JsonPrettyPrinter());
            }
            String content = writer.writeValueAsString(values);
            setContent(content);
            setContentName(NAME);
            setContentType(MIMETYPE);
        } catch (JsonProcessingException e) {
            throw new NuxeoException(e);
        }
    }

}
