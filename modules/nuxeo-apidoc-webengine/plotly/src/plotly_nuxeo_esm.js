// XXX - import done in index page due to broken ESM support, see:
// https://github.com/plotly/plotly.js/issues/3518
// import '../node_modules/plotly.js-dist/plotly.js';

import { is3D, GRAPH_TYPES } from './constants.js';

import {
  traceNodes,
  traceEdges,
  traceMesh,
} from './tracing.js';

export default (datasource, datasource_title, graph) => Plotly.d3.json(datasource, _render(datasource, datasource_title, graph));

const _render = (_, datasource_title, graph) => (err, fig) => {

    if (err) {
        alert("Error retrieving json data");
        return;
    }

    const graph_type = GRAPH_TYPES[fig.type];
    if (graph_type == undefined) {
        console.alert("Unsupported graph type " + fig.type);
    }

    const is3DGraph = is3D(fig.type);

    const nodes_by_key = Object.fromEntries(fig.nodes.map(node => [node.id, node]));

    const trace_bundles = traceNodes(fig, 'BUNDLE', {
        name: 'Bundles',
        legendgroup: "bundles",
    });
    const trace_brequires = traceEdges(fig, nodes_by_key, 'REQUIRES', {
        name: 'Requires Bundle',
        legendgroup: "bundles",
    });
    const trace_components = traceNodes(fig, 'COMPONENT', {
        name: 'Components',
        legendgroup: "components",
    });
    const trace_crequires = traceEdges(fig, nodes_by_key, 'SOFT_REQUIRES', {
        name: 'Requires Component',
        legendgroup: "components",
    });
    const trace_xps = traceNodes(fig, 'EXTENSION_POINT', {
        name: 'Extension Points',
        legendgroup: "xps",
    });
    const trace_contributions = traceNodes(fig, 'CONTRIBUTION', {
        name: 'Contributions',
        legendgroup: "xps",
    });
    const trace_references = traceEdges(fig, nodes_by_key, 'REFERENCES', {
        name: 'Contributes to Extension Point',
        legendgroup: "xps",
    });

    const trace_bundles_cont = traceNodes(fig, 'BUNDLE', {
        name: 'Bundles',
        legendgroup: "contains",
        visible: 'legendonly',
    });
    const trace_components_cont = traceNodes(fig, 'COMPONENT', {
        name: 'Components',
        legendgroup: "contains",
        visible: 'legendonly',
    });
    const trace_services_cont = traceNodes(fig, 'SERVICE', {
        name: 'Services',
        legendgroup: "contains",
        visible: 'legendonly',
    });
    const trace_xps_cont = traceNodes(fig, 'EXTENSION_POINT', {
        name: 'Extension Points',
        legendgroup: "contains",
        visible: 'legendonly',
    });
    const trace_contributions_cont = traceNodes(fig, 'CONTRIBUTION', {
        name: 'Contributions',
        legendgroup: "contains",
        visible: 'legendonly',
    });
    const trace_contains = traceEdges(fig, nodes_by_key, 'CONTAINS', {
        name: 'Contains',
        legendgroup: "contains",
        visible: 'legendonly',
    });
    const trace_mesh = traceMesh(fig, nodes_by_key, 'CONTAINS', {
        name: 'Mesh Tentative (WIP)',
        legendgroup: "contains",
        visible: 'legendonly',
    });

    const data = [
        trace_bundles, trace_brequires,
        trace_components, trace_crequires,
        trace_xps, trace_contributions, trace_references,
        trace_bundles_cont, trace_components_cont, trace_services_cont, trace_xps_cont, trace_contributions_cont, trace_contains, trace_mesh
        ];

    if (is3DGraph) {
        const trace_references_cont = traceEdges(fig, nodes_by_key, 'REFERENCES', {
            name: 'Contributes to Extension Point',
            legendgroup: "contains",
            visible: 'legendonly',
        });
        data.push(trace_references_cont);
    }

    const axis = {
            showbackground: false,
            showspikes: false,
            showgrid: false,
            showticklabels: false,
            zeroline: false,
            title: "",
    }
    const scene = {
            xaxis: axis,
            yaxis: axis,
    }
    if (is3DGraph) {
        scene.zaxis = axis;
    }

    const layout = {
            title: {text: `<b>${fig.title}</b> (${datasource_title})<br><i>${fig.description}</i>`},
            showlegend: true,
            legend : {
                borderwidth: 1,
                bgcolor: '#fff',
            },
            hovermode: 'closest',
            margin: {t: 100},
            paper_bgcolor: "#eee",
            width: 2000,
            height: 2000,
    };
    if (is3DGraph) {
        layout.scene = scene;
    } else {
        Object.assign(layout, scene);
    }

    Plotly.newPlot(graph, data, layout, {responsive: true});

};
