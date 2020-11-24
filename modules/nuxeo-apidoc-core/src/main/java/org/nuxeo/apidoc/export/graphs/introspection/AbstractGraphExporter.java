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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.export.api.AbstractExporter;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.export.graphs.api.Edge;
import org.nuxeo.apidoc.export.graphs.api.EdgeType;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.api.NodeAttribute;
import org.nuxeo.apidoc.export.graphs.api.NodeCategory;
import org.nuxeo.apidoc.export.graphs.api.NodeType;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;

/**
 * Basic implementation relying on introspection of distribution.
 *
 * @since 20.0.0
 */
public abstract class AbstractGraphExporter extends AbstractExporter {

    public AbstractGraphExporter(ExporterDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * Introspect the graph, ignoring bundle groups but selecting all other nodes.
     * <ul>
     * <li>bundles
     * <li>components
     * <li>extension points declarations
     * <li>services
     * <li>contributions
     * </ul>
     * <p>
     * Relations between nodes will respect the general introspection of bundles.
     */
    protected GraphExport getDefaultGraph(DistributionSnapshot distribution, SnapshotFilter filter,
            Map<String, String> properties) {
        GraphExport graph = new DefaultGraphExport();

        Map<String, Integer> nodeHits = new HashMap<>();
        Set<String> isolatedBundles = new HashSet<>();

        for (BundleInfo bundle : distribution.getBundles()) {
            if (filter != null && !filter.accept(bundle)) {
                continue;
            }
            NodeCategory category = NodeCategory.guessCategory(bundle);
            Node<?> bundleNode = createBundleNode(bundle, category);
            bundleNode.setWeight(1);
            graph.addNode(bundleNode);
            for (String requirement : bundle.getRequirements()) {
                Node<?> refNode = createReferenceNode(NodeType.BUNDLE.prefix(requirement), NodeType.BUNDLE.name());
                graph.addEdge(createEdge(bundleNode, refNode, EdgeType.REQUIRES.name()));
            }
            if (bundle.getRequirements().isEmpty()) {
                isolatedBundles.add(NodeType.BUNDLE.prefix(bundle.getId()));
            }
            for (ComponentInfo component : bundle.getComponents()) {
                if (filter != null && !filter.accept(component)) {
                    continue;
                }
                Node<?> compNode = createComponentNode(component, category);
                graph.addNode(compNode);
                graph.addEdge(createEdge(bundleNode, compNode, EdgeType.CONTAINS.name()));
                for (ServiceInfo service : component.getServices()) {
                    if (filter != null && !filter.accept(service)) {
                        continue;
                    }
                    if (service.isOverriden()) {
                        continue;
                    }
                    Node<?> serviceNode = createServiceNode(service, category);
                    graph.addNode(serviceNode);
                    graph.addEdge(createEdge(compNode, serviceNode, EdgeType.CONTAINS.name()));
                    hit(nodeHits, compNode.getId());
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    if (filter != null && !filter.accept(xp)) {
                        continue;
                    }
                    Node<?> xpNode = createXPNode(xp, category);
                    graph.addNode(xpNode);
                    graph.addEdge(createEdge(compNode, xpNode, EdgeType.CONTAINS.name()));
                    hit(nodeHits, compNode.getId());
                }

                for (ExtensionInfo contribution : component.getExtensions()) {
                    if (filter != null && !filter.accept(contribution)) {
                        continue;
                    }
                    Node<?> contNode = createContributionNode(contribution, category);
                    graph.addNode(contNode);
                    // add link to corresponding component
                    graph.addEdge(createEdge(compNode, contNode, EdgeType.CONTAINS.name()));
                    hit(nodeHits, compNode.getId());

                    // also add link to target extension point, "guessing" the extension point id, not counting for
                    // hits
                    String targetId = NodeType.EXTENSION_POINT.prefix(contribution.getExtensionPoint());
                    Node<?> refNode = createReferenceNode(targetId, NodeType.EXTENSION_POINT.name());
                    graph.addEdge(createEdge(contNode, refNode, EdgeType.REFERENCES.name()));
                    hit(nodeHits, targetId);
                    hit(nodeHits, NodeType.COMPONENT.prefix(contribution.getTargetComponentName().getRawName()));
                }

                // compute requirements
                for (String requirement : component.getRequirements()) {
                    Node<?> refNode = createReferenceNode(NodeType.COMPONENT.prefix(requirement),
                            NodeType.COMPONENT.name());
                    graph.addEdge(createEdge(compNode, refNode, EdgeType.SOFT_REQUIRES.name()));
                }

            }
        }

        // Adds potentially missing bundle root
        String rrId = getBundleRootId();
        Node<?> rrNode = graph.getNode(rrId);
        if (rrNode == null) {
            rrNode = createBundleRoot();
            graph.addNode(rrNode);
        }
        // Adds potentially missing link to bundle root for isolated (children nodes)
        List<Node<?>> orphans = new ArrayList<>();
        // handle missing references in all edges too
        for (Edge edge : graph.getEdges()) {
            Node<?> source = graph.getNode(edge.getSource());
            if (source == null) {
                source = addMissingNode(graph, edge.getSource());
            }
            Node<?> target = graph.getNode(edge.getTarget());
            if (target == null) {
                target = addMissingNode(graph, edge.getTarget());
                if (NodeType.BUNDLE.name().equals(target.getType())
                        && EdgeType.REQUIRES.name().equals(edge.getValue())) {
                    orphans.add(target);
                }
            }
        }
        // handle isolated bundles
        for (Node<?> orphan : orphans) {
            requireBundleRootNode(graph, rrNode, orphan);
        }
        for (String child : isolatedBundles) {
            requireBundleRootNode(graph, rrNode, graph.getNode(child));
        }

        // handle packages
        for (PackageInfo pkg : distribution.getPackages()) {
            if (filter != null && !filter.accept(pkg)) {
                continue;
            }
            OptionalLong index = pkg.getBundleInfo()
                                    .values()
                                    .stream()
                                    .filter(Objects::nonNull)
                                    .map(BundleInfo::getMinResolutionOrder)
                                    .filter(Objects::nonNull)
                                    .mapToLong(Long::longValue)
                                    .min();
            Node<?> pkgNode = createPackageNode(pkg, NodeCategory.PLATFORM, index.orElseGet(null));
            graph.addNode(pkgNode);
            for (String bundleId : pkg.getBundles()) {
                Node<?> refNode = createReferenceNode(NodeType.BUNDLE.prefix(bundleId), NodeType.BUNDLE.name());
                graph.addEdge(createEdge(pkgNode, refNode, EdgeType.CONTAINS.name()));
                hit(nodeHits, pkgNode.getId());
            }
        }

        refine(graph, nodeHits);

        return graph;
    }

    protected void requireBundleRootNode(GraphExport graph, Node<?> rootNode, Node<?> bundleNode) {
        if (bundleNode == null) {
            return;
        }
        // make it require the root node, unless we're handling the root itself
        String rootId = getBundleRootId();
        if (!rootId.equals(bundleNode.getId())) {
            graph.addEdge(createEdge(bundleNode, rootNode, EdgeType.REQUIRES.name()));
        }
    }

    /**
     * Refines graphs for display.
     * <ul>
     * <li>sets weights computed from hits map
     * <li>handles zero weights
     */
    protected void refine(GraphExport graph, Map<String, Integer> hits) {
        for (Map.Entry<String, Integer> hit : hits.entrySet()) {
            setWeight(graph, hit.getKey(), hit.getValue());
        }
    }

    protected Node<?> addMissingNode(GraphExport graph, String id) {
        // try to guess type and category according to prefix
        NodeCategory cat = NodeCategory.PLATFORM;
        NodeType type = NodeType.guess(id);
        String unprefixedId = type.unprefix(id);
        cat = NodeCategory.guess(unprefixedId);
        Node<?> node = createNode(id, unprefixedId, type.name(), getDefaultNodeWeight(), cat.name(), null, null);
        graph.addNode(node);
        return node;
    }

    protected String getBundleRootId() {
        return NodeType.BUNDLE.prefix(BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE);
    }

    protected int getDefaultNodeWeight() {
        return 1;
    }

    protected int getDefaultEdgeWeight() {
        return 1;
    }

    protected Node<?> createNode(String id, String label, String type, int weight, String category, Long index,
            NuxeoArtifact object) {
        Node<?> node = new NodeImpl<>(id, label, type, weight, object);
        if (StringUtils.isNotBlank(category)) {
            node.setAttribute(NodeAttribute.CATEGORY.key(), category);
        }
        if (index != null) {
            node.setAttribute(NodeAttribute.INDEX.key(), String.valueOf(index));
        }
        return node;
    }

    protected Node<?> createReferenceNode(String id, String type) {
        // No category, no extra info
        return createNode(id, id, type, getDefaultNodeWeight(), null, null, null);
    }

    protected Node<?> createBundleRoot() {
        return createNode(getBundleRootId(), BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE, NodeType.BUNDLE.name(),
                getDefaultNodeWeight(), NodeCategory.RUNTIME.name(), null, null);
    }

    protected Node<?> createBundleNode(BundleInfo bundle, NodeCategory cat) {
        String bid = bundle.getId();
        String pbid = NodeType.BUNDLE.prefix(bid);
        return createNode(pbid, bid, NodeType.BUNDLE.name(), getDefaultNodeWeight(), cat.name(),
                bundle.getMinResolutionOrder(), bundle);
    }

    protected Node<?> createComponentNode(ComponentInfo component, NodeCategory cat) {
        String compid = component.getId();
        String pcompid = NodeType.COMPONENT.prefix(compid);
        return createNode(pcompid, compid, NodeType.COMPONENT.name(), getDefaultNodeWeight(), cat.name(),
                component.getResolutionOrder(), component);
    }

    protected Node<?> createServiceNode(ServiceInfo service, NodeCategory cat) {
        String sid = service.getId();
        String psid = NodeType.SERVICE.prefix(sid);
        return createNode(psid, sid, NodeType.SERVICE.name(), getDefaultNodeWeight(), cat.name(),
                service.getComponent().getResolutionOrder(), service);
    }

    protected Node<?> createXPNode(ExtensionPointInfo xp, NodeCategory cat) {
        String xpid = xp.getId();
        String pxpid = NodeType.EXTENSION_POINT.prefix(xpid);
        return createNode(pxpid, xpid, NodeType.EXTENSION_POINT.name(), getDefaultNodeWeight(), cat.name(),
                xp.getComponent().getResolutionOrder(), xp);
    }

    protected Node<?> createContributionNode(ExtensionInfo contribution, NodeCategory cat) {
        String cid = contribution.getId();
        String pcid = NodeType.CONTRIBUTION.prefix(cid);
        return createNode(pcid, cid, NodeType.CONTRIBUTION.name(), getDefaultNodeWeight(), cat.name(),
                contribution.getComponent().getResolutionOrder(), contribution);
    }

    protected Node<?> createPackageNode(PackageInfo pkg, NodeCategory cat, Long index) {
        String pid = pkg.getId();
        String ppid = NodeType.PACKAGE.prefix(pid);
        return createNode(ppid, pid, NodeType.PACKAGE.name(), getDefaultNodeWeight(), cat.name(), index, pkg);
    }

    protected Edge createEdge(Node<?> source, Node<?> target, String value) {
        return new EdgeImpl(source.getId(), target.getId(), value, getDefaultEdgeWeight());
    }

    protected void hit(Map<String, Integer> hits, String source) {
        if (hits.containsKey(source)) {
            hits.put(source, hits.get(source) + 1);
        } else {
            hits.put(source, Integer.valueOf(1));
        }
    }

    protected void setWeight(GraphExport graph, String nodeId, int hit) {
        Node<?> node = graph.getNode(nodeId);
        if (node != null) {
            node.setWeight(node.getWeight() + hit);
        }
    }

}
