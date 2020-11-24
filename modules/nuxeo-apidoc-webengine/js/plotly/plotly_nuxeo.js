// XXX - import done in index page due to broken ESM support, see:
// https://github.com/plotly/plotly.js/issues/3518
// import '../node_modules/plotly.js-dist/plotly.js';

import { graphElement, graphType, menuName, NODE_TYPES } from './constants.js';
import { getBasicLayout } from './layouting.js';
import { clearSelections, getUpdateMenus, highlightUnselected, syncAnnotations } from './menus.js';
import { createFakePoint, selectPoint } from './selecting.js';
import { getEdgeTrace, getNodeTrace } from './tracing.js';
import { getPositions } from './positioning.js';

export default (graphDiv, options) => Plotly.d3.json(options.datasource, _render(graphDiv, options));

const _render = (graphDiv, options) => (err, fig) => {

  if (err) {
    alert("Error retrieving json data");
    console.error(err);
    return;
  }

  var datasource = options.datasource;
  if (!datasource) {
    alert("No datasource");
    return;
  }

  if (graphType(fig.type) == undefined) {
    var err = "Unsupported graph type " + fig.type;
    alert(err);
    console.error(err);
    return;
  }

  _renderBasic(graphDiv, fig, options);

};

const _renderBasic = (graphDiv, fig, options) => {

  var is3D = (graphType(fig.type) === '3d' || options.threeD === true);
  console.log(is3D);
  console.log(options.threeD);

  var nodesById = fig.nodes.reduce(function (map, node) {
    map[node.id] = node;
    return map;
  }, {});
  var edgesByNodeId = {},
    edgesById = {};
  for (var i = 0; i < fig.edges.length; i++) {
    var edge = fig.edges[i];
    // assign ids to edges for easier management in selection
    edge.id = `NXEdge${i}-${edge.value}`;
    // fill references in edgesByNodeId map
    edgesById[edge.id] = edge;
    if (!edgesByNodeId[edge.source]) {
      edgesByNodeId[edge.source] = [];
    }
    edgesByNodeId[edge.source].push(edge.id);
  }
  // wrap these helpers inside a graph object
  var nxgraph = {
    div: graphDiv,
    is3D: is3D,
    options: options,
    // helper objects for initial rendering and management of annotations on selection
    nodesById: nodesById,
    edgesById: edgesById,
    edgesByNodeId: edgesByNodeId,
    // share original parsed data
    fig: fig,
  }

  if (!fig.type.endsWith("_LAYOUT")) {
    nxgraph = getPositions(nxgraph, is3D);
  }

  var traces = [];
  var legendonlyconf = {
    visible: 'legendonly',
  }

  // individual traces and groups of nodes and edges, grouped depending on runtime logics
  traces = traces.concat([
    ...getNodeTrace(nxgraph, 'PACKAGE', {}),
    ...getNodeTrace(nxgraph, 'BUNDLE', {}),
    ...getNodeTrace(nxgraph, 'BUNDLE', Object.assign({}, legendonlyconf, { legendgroup: 'bundles' })),
    ...getEdgeTrace(nxgraph, 'REQUIRES', Object.assign({}, legendonlyconf, { legendgroup: 'bundles', isFlatEdge: true })),
    ...getNodeTrace(nxgraph, 'COMPONENT', {}),
    ...getNodeTrace(nxgraph, 'COMPONENT', Object.assign({}, legendonlyconf, { legendgroup: 'components' })),
    ...getEdgeTrace(nxgraph, 'SOFT_REQUIRES', Object.assign({}, legendonlyconf, { legendgroup: 'components', isFlatEdge: true })),
    ...getNodeTrace(nxgraph, 'SERVICE', {}),
    ...getNodeTrace(nxgraph, 'EXTENSION_POINT', {}),
    ...getNodeTrace(nxgraph, 'CONTRIBUTION', {}),
    ...getNodeTrace(nxgraph, 'EXTENSION_POINT', Object.assign({}, legendonlyconf, { legendgroup: 'xps' })),
    ...getNodeTrace(nxgraph, 'CONTRIBUTION', Object.assign({}, legendonlyconf, { legendgroup: 'xps' })),
    ...getEdgeTrace(nxgraph, 'REFERENCES', Object.assign({}, legendonlyconf, { legendgroup: 'xps' })),
  ]);

  // push another set of traces for containment (seems to be more efficient than using the 'groupby'
  // transform)
  var containsconf = Object.assign({}, legendonlyconf, {
    legendgroup: 'contains',
  });
  for (var type of Object.keys(NODE_TYPES)) {
    traces.push(...getNodeTrace(nxgraph, type, containsconf));
  } traces = traces.concat([
    ...getEdgeTrace(nxgraph, 'CONTAINS', containsconf),
  ]);

  var template = { layout: getBasicLayout(nxgraph) };
  var layout = {
    template: template,
    updatemenus: getUpdateMenus(traces),
  };

  // plot creation
  Plotly.newPlot(graphDiv, traces, layout, { responsive: true });

  // events management
  var gd = graphElement(graphDiv);
  var lightNXGraph = {
    div: graphDiv,
    is3D: is3D,
    options: options,
    selected: [],
  }

  // avoid recursion bug, see https://github.com/plotly/plotly.js/issues/1025
  var clickCalled = false;
  gd.on('plotly_click', function (data) {
    if (!clickCalled) {
      //console.log("click: ", data);
      clickCalled = true;
      var point = data.points[0];
      if (point.customdata) {
        selectPoint(lightNXGraph, point, true);
      }
      clickCalled = false;
    }
  });
  gd.on('plotly_restyle', function (data) {
    // catch traces visibility changes to sync annotations, see https://community.plotly.com/t/how-to-catch-trace-visibility-changed/5554
    syncAnnotations(lightNXGraph);
  });
  // TODO: toggle selection on annotation click?
  gd.on('plotly_clickannotation', function (event, data) {
    console.log("annotation event: ", event);
  });

  // custom menus management
  gd.on('plotly_buttonclicked', function (data) {
    if (data.menu.name == menuName('SELECT_MENU')) {
      clearSelections(lightNXGraph);
    } else if (data.menu.name == menuName('HIGHLIGHT_MENU')) {
      highlightUnselected(lightNXGraph, data.active != 0);
    }
  });

  // init and hook bundle selection, might trigger an update if selections exist
  var bundles = fig.nodes.filter(function (node) { return node.type == 'BUNDLE' });
  initBundleSelect(lightNXGraph, bundles);

  // XXX for debug only
  gd.on('plotly_afterplot', function () {
    console.log('done plotting');
    console.log(document.getElementById(graphDiv).data);
  });
}

function initBundleSelect(lnxgraph, bundles) {
  if (!lnxgraph.options.bundleselector) {
    return;
  }
  var gd = graphElement(lnxgraph.div);
  var selector = gd.parentNode.querySelector(lnxgraph.options.bundleselector);
  var firstOption = selector.querySelector('option');
  selector.textContent = '';
  if (firstOption) {
    selector.appendChild(firstOption);
  }
  for (var i = 0; i < bundles.length; i++) {
    var currentOption = document.createElement('option');
    currentOption.text = bundles[i]['label'];
    currentOption.value = bundles[i]['id'];
    selector.appendChild(currentOption);
  }
  selector.addEventListener('change', function () {
    selectPoint(lnxgraph, createFakePoint(selector.value, true));
  }, false);

  // init selection based on current value too
  if (lnxgraph.options.selectedbundle) {
    selectPoint(lnxgraph, createFakePoint(lnxgraph.options.selectedbundle, true));
  }

  return selector;

};
