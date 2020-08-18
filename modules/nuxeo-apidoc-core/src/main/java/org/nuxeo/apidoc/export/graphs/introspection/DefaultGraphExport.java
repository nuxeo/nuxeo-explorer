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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.export.graphs.api.Edge;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.api.NodeFilter;

/**
 * @since 20.0.0
 */
public class DefaultGraphExport implements GraphExport {

    protected final List<Node<?>> nodes = new ArrayList<>();

    protected final List<Edge> edges = new ArrayList<>();

    protected final Map<String, Node<?>> nodeMap = new HashMap<>();

    @Override
    public void addNode(Node<?> node) {
        this.nodes.add(node);
        this.nodeMap.put(node.getId(), node);
    }

    @Override
    public void addEdge(Edge edge) {
        this.edges.add(edge);
    }

    @Override
    public Node<?> getNode(String id) {
        return nodeMap.get(id);
    }

    @Override
    public List<Node<?>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public <T extends GraphExport> T copy(Class<T> targetClass, NodeFilter nodeFilter)
            throws ReflectiveOperationException {
        T graph = targetClass.getConstructor().newInstance();
        for (Node<?> node : getNodes()) {
            if (nodeFilter == null || nodeFilter.accept(node)) {
                graph.addNode(node.copy());
            }
        }
        for (Edge edge : getEdges()) {
            if (nodeFilter == null) {
                graph.addEdge(edge.copy());
            } else {
                Node<?> source = getNode(edge.getSource());
                Node<?> target = getNode(edge.getTarget());
                if (nodeFilter.accept(source) && nodeFilter.accept(target)) {
                    graph.addEdge(edge.copy());
                }
            }
        }
        return graph;
    }

}
