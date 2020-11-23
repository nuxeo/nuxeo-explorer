// XXX - import done in index page due to broken ESM support, see:
// https://github.com/plotly/plotly.js/issues/3518
// import '../node_modules/plotly.js-dist/plotly.js';

import { graphElement, graphType, menuName, NODE_TYPES } from './constants.js';
import { getBasicLayout } from './layouting.js';
import { clearSelections, getUpdateMenus, highlightUnselected, syncAnnotations } from './menus.js';
import { createFakePoint, selectPoint } from './selecting.js';
import { getEdgeTrace, getNodeTrace } from './tracing.js';

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
  if (options.circ) {
    _renderCirc(graphDiv, fig, options);
  } else {
    _renderBasic(graphDiv, fig, options);
  }

};

const _renderCirc = (graphDiv, fig, options) => {
  // TODO
}

const _renderBasic = (graphDiv, fig, options) => {
  console.log(graphDiv);
  console.log(fig);
  console.log(options);


  var is3D = graphType(fig.type) === '3d';
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
    // FIXME?
    // if (edge.value == EDGE_TYPES.REFERENCES) {
    //   if (!edgesByNodeId[edge.target]) {
    //     edgesByNodeId[edge.target] = [];
    //   }
    //   edgesByNodeId[edge.target].push(edge.id);
    // }
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

  var traces = [
    // groups of nodes and edges, grouped depending on runtime logics
    ...getNodeTrace(nxgraph, 'BUNDLE', { legendgroup: 'bundles' }),
    ...getEdgeTrace(nxgraph, 'REQUIRES', { legendgroup: 'bundles', isFlatEdge: true }),
    ...getNodeTrace(nxgraph, 'COMPONENT', { legendgroup: 'components' }),
    ...getEdgeTrace(nxgraph, 'SOFT_REQUIRES', { legendgroup: 'components', isFlatEdge: true }),
    ...getNodeTrace(nxgraph, 'EXTENSION_POINT', { legendgroup: 'xps' }),
    ...getNodeTrace(nxgraph, 'CONTRIBUTION', { legendgroup: 'xps' }),
    ...getEdgeTrace(nxgraph, 'REFERENCES', { legendgroup: 'xps' }),
  ];

  // push another set of traces for containment (seems to be more efficient than using the 'groupby'
  // transform)
  var containsconf = {
    legendgroup: 'contains',
    visible: 'legendonly',
  }
  for (var type of Object.keys(NODE_TYPES)) {
    traces.push(...getNodeTrace(nxgraph, type, containsconf));
  }
  traces = traces.concat([
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

// TODO notes after feedback from Nelson:
// 1. add package.json (with plotly.js-dist as dep)
// 2. add npm start script to serve the thing (and open plotly.html)
// 3. split things into modules
// (cannot load plotly as ESM, see known bug in d3 preventing it from working https://github.com/plotly/plotly.js/issues/3518)
// 4. higher order functions: see _render a function that return another function
// 5. const { value, options, selectedIndex } = sel; destructuring assingments
// 6. spread operator in maps: { …options } (? not sure still valid in WIP branch)
// 7. flatMap: you can use map and have it produce an array but still end up with a flat array (not an array of arrays)