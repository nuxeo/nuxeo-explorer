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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.export.graphs.api.Edge;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.GraphType;
import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.introspection.AbstractGraphExporter;
import org.nuxeo.apidoc.export.graphs.introspection.EdgeImpl;
import org.nuxeo.apidoc.export.graphs.introspection.NodeImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.JsonMapper;
import org.nuxeo.apidoc.snapshot.JsonPrettyPrinter;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Basic Graph export with json format.
 *
 * @since 20.0.0
 */
public class JsonGraphExporter extends AbstractGraphExporter {

    public JsonGraphExporter(ExporterDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public void export(OutputStream out, DistributionSnapshot distribution, SnapshotFilter filter,
            Map<String, String> properties) {
        GraphExport graph = getDefaultGraph(distribution, filter, properties);

        // avoid serializing full objects as they're all included as nodes
        final ObjectMapper mapper = JsonMapper.basic(null, null, false);
        mapper.registerModule(new SimpleModule().addAbstractTypeMapping(Node.class, NodeImpl.class)
                                                .addAbstractTypeMapping(Edge.class, EdgeImpl.class));
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("name", getName());
        values.put("title", getTitle());
        values.put("description", getDescription());
        values.put("type", GraphType.BASIC.name());
        if (!getProperties().isEmpty()) {
            values.put("properties", getProperties());
        }
        values.put("nodes", graph.getNodes());
        values.put("edges", graph.getEdges());
        try {
            ObjectWriter writer = mapper.writerFor(LinkedHashMap.class)
                                        .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                                        .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            if (isPrettyPrint(properties, "false")) {
                writer = writer.with(new JsonPrettyPrinter());
            }
            writer.writeValue(out, values);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected boolean isPrettyPrint(Map<String, String> properties, String defaultValue) {
        return Boolean.valueOf(getProperties().getOrDefault("pretty", defaultValue))
                || (properties != null && Boolean.valueOf(properties.getOrDefault("pretty", defaultValue)));
    }

}
