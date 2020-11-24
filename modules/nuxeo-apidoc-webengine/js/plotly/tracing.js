import { edgeColor, edgeLabel, edgeLineMarkerSize, edgeLineMarkerSymbol, MARKER_OPACITY, MARKER_TYPES, nodeColor, nodeLabel, nodeSymbol, nodeWeight, TRACE_TYPES } from './constants.js';

const HOVERTEMPLATE = "%{customdata.annotation}<extra></extra>";
const nodeMarkerAnnotation = (node) => `<b>${node.label}</b><br />Type: ${node.type}<br />Category: ${node.attributes.category}<br />Weight: ${node.weight}`;
const edgeLineMarkerAnnotation = (edge, source, target) => `${source.label}<br /><b>${edge.value}</b><br />${target.label}`;

function getNodeMarkerCustomData(nxgraph, node) {
  // resolve dependencies to highlight when selecting this node
  // XXX: depending on node type and each related edge value, resolve some references here or not, maybe?
  var links = nxgraph.edgesByNodeId[node.id];
  var data = {
    markertype: MARKER_TYPES.NODE,
    id: node.id,
    annotation: nodeMarkerAnnotation(node),
    links: links,
  };
  return data;
}

function getEdgeLineMarkerCustomData(nxgraph, edge) {
  // resolve dependencies to highlight when selecting this edge
  var links = [edge.source, edge.target];
  var source = nxgraph.nodesById[edge.source],
    target = nxgraph.nodesById[edge.target];
  var data = {
    markertype: MARKER_TYPES.EDGE,
    id: edge.id,
    annotation: edgeLineMarkerAnnotation(edge, source, target),
    links: links,
  };
  return data;
}

function getNodeTrace(nxgraph, type, config) {
  var nodes = nxgraph.fig.nodes.filter(function (node) { return type ? node.type == type : true });
  var references = nodes.reduce(function (map, node, index) {
    map[node.id] = [index];
    return map;
  }, {});
  var colors = nodes.map(node => nodeColor(node.attributes.category));
  var trace = {
    name: `<b>${type ? nodeLabel(type) : "Nodes"}</b>`,
    type: 'scatter',
    mode: 'markers',
    x: nodes.map(node => node.x),
    y: nodes.map(node => node.y),
    hovertemplate: HOVERTEMPLATE,
    hoverlabel: { font: { size: 20 } },
    customdata: nodes.map(node => getNodeMarkerCustomData(nxgraph, node)),
    marker: {
      symbol: (type ? nodeSymbol(type) : nodes.map(node => nodeSymbol(node.type))),
      size: nodes.map(node => nodeWeight(node.weight)),
      // XXX: color is not shared with hover background in 3D...
      color: colors, // to be changed on selection/highlight in 3D
      opacity: MARKER_OPACITY.DEFAULT, // to be changed on selection/highlight in 2D
      line: {
        color: 'rgb(50,50,50)',
        width: 0.5
      },
    },
    // additional custom trace info
    tracetype: TRACE_TYPES.NODE,
    references: references,
    selectedindexes: [],
    originalcolors: colors, // keep original colors to handle selections and annotations
  };
  if (nxgraph.is3D) {
    Object.assign(trace, {
      'type': 'scatter3d',
      'z': nodes.map(node => node.z),

    });
  }
  Object.assign(trace, config);
  return [trace];
}

// use markers instead of lines because of restrictions on lines (color + width + marker opacity)
function getEdgeTrace(nxgraph, type, config) {
  var edges = nxgraph.fig.edges.filter(function (edge) { return type ? edge.value == type : true });
  var nbiterations = 4;
  var x = computeEdgeLineMarkers(edges, nxgraph.nodesById, 'x', nbiterations); // will server as final size reference
  var nbpoints = x.length / edges.length;
  var mreferences = edges.reduce(function (map, edge, index) {
    map[edge.id] = [...Array(nbpoints).keys()].map(i => index * nbpoints + i);
    return map;
  }, {});
  var customdata = edges.map(edge => getEdgeLineMarkerCustomData(nxgraph, edge));
  var colors = type ? edgeColor(type) : edges.map(edge => edgeColor(edge.value));
  var allcolors = (type ? colors : computeLineMarkersData(colors, nbpoints));
  var name = (type ? edgeLabel(type) : "Edges");
  if (type) {
    name = `<b><span style="color:${edgeColor(type)}">${name}</span></b>`;
  }
  var markers = {
    name: name,
    type: 'scattergl',
    mode: 'markers',
    x: x,
    y: computeEdgeLineMarkers(edges, nxgraph.nodesById, 'y', nbiterations),
    hovertemplate: HOVERTEMPLATE,
    hoverlabel: { font: { size: 20 } },
    customdata: computeLineMarkersData(customdata, nbpoints),
    marker: {
      symbol: edgeLineMarkerSymbol(nxgraph),
      color: allcolors, // to be changed on selection/highlight
      size: edgeLineMarkerSize(nxgraph),
      line: {
        color: 'rgb(50,50,50)',
        width: 0.3,
      },
    },
    // additional custom trace info
    tracetype: TRACE_TYPES.EDGE,
    references: mreferences,
    selectedindexes: [],
    originalcolors: allcolors, // keep original colors to handle selections and annotations
  };
  if (nxgraph.is3D) {
    Object.assign(markers, {
      type: 'scatter3d',
      z: computeEdgeLineMarkers(edges, nxgraph.nodesById, 'z', nbiterations),
    });
  }
  Object.assign(markers, config);
  return [markers];
}

function computeEdgeLines(edges, nodesById, axis) {
  return edges.reduce(function (res, edge) {
    res.push(nodesById[edge.source][axis], nodesById[edge.target][axis], null);
    return res;
  }, []);
}

// helps building an additional trace to show marker points on relations for better text on hover
function computeEdgeLineMarkers(edges, nodesById, axis, nbiterations) {
  return edges.reduce(function (res, edge) {
    res.push(...midpoints(nodesById[edge.source][axis], nodesById[edge.target][axis], 0, nbiterations));
    return res;
  }, []);
}

// adapt other data content to line markers multiplication thanks to above logics
function computeLineMarkersData(data, nbpoints) {
  return data.reduce(function (res, item) {
    res.push(...Array(nbpoints).fill(item));
    return res;
  }, []);
}

function midpoints(a, b, iteration, nbmax) {
  var res = [];
  if (iteration > nbmax) {
    return res;
  }
  var m = midpoint(a, b);
  res.push(m);
  res.push(...midpoints(a, m, iteration + 1, nbmax));
  res.push(...midpoints(m, b, iteration + 1, nbmax));
  return res;
}

function midpoint(sourceaxis, targetaxis) {
  return (sourceaxis + targetaxis) / 2;
}

export {
  getNodeTrace,
  getEdgeTrace,
};

