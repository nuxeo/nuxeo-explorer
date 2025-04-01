/*
 * (C) Copyright 2020-2025 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.export.graphs.api.Edge;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.introspection.AbstractGraphExporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Basic Graph export using DOT format.
 *
 * @since 20.0.0
 */
public class DOTGraphExporter extends AbstractGraphExporter {

    public DOTGraphExporter(ExporterDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public void export(OutputStream out, DistributionSnapshot distribution, SnapshotFilter filter,
            Map<String, String> properties) {
        GraphExport graph = getDefaultGraph(distribution, filter, properties);

        SimpleDirectedGraph<IdNode, Edge> g = new SimpleDirectedGraph<>(Edge.class);

        int itemIndex = 1;
        Map<String, IdNode> idMap = new HashMap<>();
        for (Node<?> node : graph.getNodes()) {
            IdNode idNode = new IdNode(itemIndex, node);
            g.addVertex(idNode);
            idMap.put(node.getId(), idNode);
            itemIndex++;
        }

        for (Edge edge : graph.getEdges()) {
            Node<?> source = graph.getNode(edge.getSource());
            Node<?> target = graph.getNode(edge.getTarget());
            g.addEdge(idMap.get(source.getId()), idMap.get(target.getId()), edge);
        }

        try {
            Function<IdNode, String> vertexIDProvider = idNode -> String.valueOf(idNode.getId());
            Function<IdNode, Map<String, Attribute>> vertexAttributeProvider = idNode -> {
                var map = new LinkedHashMap<String, Attribute>();
                Node<?> node = idNode.getNode();
                map.put("label", DefaultAttribute.createAttribute(node.getLabel()));
                map.put("weight", DefaultAttribute.createAttribute(node.getWeight()));
                map.put("type", DefaultAttribute.createAttribute(node.getType()));
                for (var entry : node.getAttributes().entrySet()) {
                    map.put(entry.getKey(), DefaultAttribute.createAttribute(entry.getValue()));
                }
                return map;
            };
            var exporter = new DOTExporter<IdNode, Edge>(vertexIDProvider);
            exporter.setVertexAttributeProvider(vertexAttributeProvider);
            exporter.setEdgeAttributeProvider(
                    edge -> Map.of("label", DefaultAttribute.createAttribute(edge.getValue())));
            exporter.exportGraph(g, out);
        } catch (ExportException e) {
            throw new NuxeoException(e);
        }
    }

    static class IdNode {

        int id;

        Node<?> node;

        public IdNode(int id, Node<?> node) {
            super();
            this.id = id;
            this.node = node;
        }

        public int getId() {
            return id;
        }

        public Node<?> getNode() {
            return node;
        }

        @Override
        public String toString() {
            return "IdNode(" + id + ", " + node.getId() + ")";
        }

    }

}
