export const UNSELECTED_COLOR = 'white';
export const MARKER_OPACITY = {
    DEFAULT: 1,
    UNSELECTED: 0.1,
}
export const MARKER_TYPES = {
    NODE: 'node',
    EDGE: 'edge',
}
export const TRACE_TYPES = {
    NODE: 'nodes',
    EDGE: 'edges',
}

export const MENUS = {
    NODE_SIZE_MENU: {
        name: 'NodeSizeMenu',
        label: 'Hide Node Sizes',
    },
    HIGHLIGHT_MENU: {
        name: 'HighlightMenu',
        label: 'Highlight Unselected',
    },
    SELECT_MENU: {
        name: 'SelectMenu',
        label: 'Clear Selections',
    },
};
export const menuName = (id) => MENUS[id]['name'];
export const menuLabel = (id) => MENUS[id]['label'];

export const graphType = (type) => ({
    BASIC_LAYOUT: '2d',
    BASIC_LAYOUT_3D: '3d',
})[type];

export const graphElement = (graphDiv) => document.getElementById(graphDiv);

export const NODE_TYPES = {
    BUNDLE: {
        label: 'Bundles',
        symbol: 'diamond',
    },
    COMPONENT: {
        label: 'Components',
        symbol: 'square',
    },
    SERVICE: {
        label: 'Services',
        symbol: 'diamond-open',
    },
    EXTENSION_POINT: {
        label: 'Extension Points',
        symbol: 'cross',
    },
    CONTRIBUTION: {
        label: 'Contributions',
        symbol: 'circle',
    },
}
export const nodeLabel = (type) => NODE_TYPES[type]['label'];
export const nodeSymbol = (type) => NODE_TYPES[type]['symbol'];
export const nodeWeight = (weight) => (weight) ? weight * 5 : 5;

// hardcode Viridis colorscale for easier retrieval from custom code (maybe can acccess ColorScale features directly (?)
export const NODE_CATEGORY_CMIN = 0;
export const NODE_CATEGORY_CMAX = 3;
//export const NODE_CATEGORY_COLORSCALE = 'Viridis';[
export const NODE_CATEGORY_COLORSCALE = [
    [0, '#440154'], [0.06274509803921569, '#48186a'],
    [0.12549019607843137, '#472d7b'], [0.18823529411764706, '#424086'],
    [0.25098039215686274, '#3b528b'], [0.3137254901960784, '#33638d'],
    [0.3764705882352941, '#2c728e'], [0.4392156862745098, '#26828e'],
    [0.5019607843137255, '#21918c'], [0.5647058823529412, '#1fa088'],
    [0.6274509803921569, '#28ae80'], [0.6901960784313725, '#3fbc73'],
    [0.7529411764705882, '#5ec962'], [0.8156862745098039, '#84d44b'],
    [0.8784313725490196, '#addc30'], [0.9411764705882353, '#d8e219'],
    [1, '#fde725']
];

// XXX: find a better way to do that maybe...
export const colorFromScale = (value) => {
    var scolor = value / (NODE_CATEGORY_CMAX - NODE_CATEGORY_CMIN);
    for (var item of NODE_CATEGORY_COLORSCALE) {
        if (scolor <= item[0]) {
            return item[1];
        }
    }
    return null;
}

export const nodeColor = (category) => ({
    RUNTIME: colorFromScale(0),
    CORE: colorFromScale(1),
    PLATFORM: colorFromScale(2),
    STUDIO: colorFromScale(3),
})[category];

export const EDGE_TYPES = {
    REQUIRES: {
        label: 'Requires Bundle',
        color: '#FFA500', // Orange
    },
    SOFT_REQUIRES: {
        label: 'Requires Component',
        color: '#FFD700', // Gold
    },
    REFERENCES: {
        label: 'Contributes to Extension Point',
        color: '#8FBC8F', // DarkSeaGreen
    },
    CONTAINS: {
        label: 'Contains',
        color: '#87CEFA', // LightSkyBlue
    }
};
export const edgeLabel = (value) => EDGE_TYPES[value]['label'];
export const edgeColor = (value) => EDGE_TYPES[value]['color'];
export const edgeLineMarkerSymbol = (nxgraph) => (nxgraph.is3D ? 'circle' : 'hexagon');
export const edgeLineMarkerSize = (nxgraph) => (nxgraph.is3D ? 2 : 5);