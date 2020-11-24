import { NODE_TYPES, EDGE_TYPES } from './constants.js';

export const ROOT_BUNDLE_ID = 'NXBundle-org.nuxeo.runtime.root';

export function getPositions(nxgraph, is3D) {
  // position nodes depending on type
  var pnodes = [];
  for (var type in NODE_TYPES) {
    var nodes = nxgraph.fig.nodes.filter(function (node) { return type ? node.type == type : true })
      .filter(function (node) { return node.id != ROOT_BUNDLE_ID })
      .sort((a, b) => a.index - b.index);
    var total = nodes.length;
    nodes.forEach(function (node, i) {
      pnodes.push(getNodePositioned(node, is3D, NODE_TYPES[type].level, i, total));
    });
  }
  if (ROOT_BUNDLE_ID in nxgraph.nodesById) {
    var root = nxgraph.nodesById[ROOT_BUNDLE_ID];
    root.x = 0;
    root.y = 0;
    if (is3D) {
      root.z = NODE_TYPES.BUNDLE.level;
    }
    pnodes.push(root);
  }
  nxgraph.fig.nodes = pnodes;
  // filter requirements to root, placed at the center
  nxgraph.fig.edges = nxgraph.fig.edges.filter(function (edge) { return (edge.value != 'REQUIRES' || edge.target != ROOT_BUNDLE_ID)});
  return nxgraph;
}

export function getPositions3D(nxgraph) {
  // TODO
  return getPositions2D(nxgraph);
}

export function getNodePositioned(node, is3D, radius, index, total) {
  node.x = radius * Math.cos(2 * Math.PI * index / total);
  node.y = radius * Math.sin(2 * Math.PI * index / total);
  if (is3D) {
    node.z = radius;
  }
  return node;
}
