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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.export.graphs.api.Edge;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.introspection.AbstractGraphExporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;

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

        DOTExporter<IdNode, Edge> exporter = new DOTExporter<>(idNode -> String.valueOf(idNode.getId()));
        exporter.setVertexAttributeProvider(v -> {
            Node<?> node = v.getNode();
            var map = new LinkedHashMap<String, String>();
            map.put("label", node.getLabel());
            map.put("weight", String.valueOf(node.getWeight()));
            map.put("type", node.getType());
            map.putAll(node.getAttributes());
            return map.entrySet()
                      .stream()
                      .collect(Collectors.toMap(Map.Entry::getKey, e -> DefaultAttribute.createAttribute(e.getValue()),
                              (e1, e2) -> e1, LinkedHashMap::new));

        });
        exporter.setEdgeAttributeProvider(e -> Map.of("label", DefaultAttribute.createAttribute(e.getValue())));
        exporter.exportGraph(g, out);
    }

    class IdNode {

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
