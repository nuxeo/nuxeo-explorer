
import { getAnnotationsLayoutUpdate, getInitialAnnotations } from './annotating.js';
import { graphElement, menuLabel, menuName, UNSELECTED_COLOR } from './constants.js';

function highlightUnselected(lnxgraph, doHighlight) {
  var traces = graphElement(lnxgraph.div).data;
  // use color for selection, see https://github.com/plotly/plotly.js/issues/2186
  var colors = traces.reduce(function (res, trace) {
    if (!trace.selectedindexes || trace.selectedindexes.length == 0) {
      // reset to original color(s) or unselected color
      res.push(doHighlight ? trace.originalcolors : UNSELECTED_COLOR);
    } else if (doHighlight) {
      // reset to original color(s)
      res.push(trace.originalcolors);
    } else {
      // take into account selections
      var tcolors;
      if (Array.isArray(trace.originalcolors)) {
        tcolors = [...trace.originalcolors];
      } else {
        tcolors = [...Array(trace.x.length).fill(trace.originalcolors)];
      }
      tcolors = tcolors.map((color, index) => trace.selectedindexes.includes(index) ? color : UNSELECTED_COLOR);
      res.push(tcolors);
    }
    return res;
  }, []);
  Plotly.restyle(lnxgraph.div, { 'marker.color': colors }, [...Array(traces.length).keys()]);
  setHighlightButtonActive(lnxgraph.div, doHighlight);
}

function clearSelections(lnxgraph) {
  setClearSelectionsButtonVisible(lnxgraph.div, false);
  // reset dupe detection helpers
  lnxgraph.selected = [];
  // reset all annotations and selected info on all traces too
  var data_update = { selectedindexes: null }; // XXX: update with an empty array does not work ok...
  var layout_update = getAnnotationsLayoutUpdate(lnxgraph, []);
  Plotly.update(lnxgraph.div, data_update, layout_update);
  // reset highlight
  highlightUnselected(lnxgraph, true);
}

// make annotations visible or not depending on the corresponding trace visibility
function syncAnnotations(lnxgraph) {
  var traces = graphElement(lnxgraph.div).data;
  var annotations = getInitialAnnotations(lnxgraph);
  var vtraces = traces.map(trace => (trace.visible == true || trace.visible == undefined));
  var updatedAnnotations = annotations.reduce(function (res, a) {
    a.visible = a.traceindexes.some(index => vtraces[index] == true);
    res.push(a);
    return res;
  }, []);
  if (annotations != updatedAnnotations) {
    var layout_update = getAnnotationsLayoutUpdate(lnxgraph, updatedAnnotations);
    Plotly.relayout(lnxgraph.div, layout_update);
  }
}

function getUpdateMenus(traces) {
  var menus = [];

  var msizes = traces.reduce(function (res, trace) {
    if ('marker' in trace) {
      res.push(trace.marker.size);
    } else {
      res.push([]);
    }
    return res;
  }, []);
  menus.push({
    name: menuName('NODE_SIZE_MENU'),
    type: 'buttons',
    x: 0.40, xanchor: 'left',
    // size is shown by default -> inactive
    active: -1,
    buttons: [{
      label: menuLabel('NODE_SIZE_MENU'),
      method: 'restyle',
      // toggle args
      args: ['marker.size', '6'],
      // will put selected markers back to original size
      args2: ['marker.size', msizes],
    }],
  });

  menus.push({
    name: menuName('HIGHLIGHT_MENU'),
    type: 'buttons',
    direction: 'down',
    x: 0.55, xanchor: 'center',
    // highlighted by default -> active
    active: 0,
    buttons: [{
      label: menuLabel('HIGHLIGHT_MENU'),
      // handled through plotly_buttonclicked event
      method: 'skip',
      execute: false,
    }],
  });

  menus.push({
    name: menuName('SELECT_MENU'),
    type: 'buttons',
    direction: 'down',
    x: 0.75, xanchor: 'center',
    // will be visible on selection existence only
    visible: false,
    showactive: false,
    buttons: [{
      label: menuLabel('SELECT_MENU'),
      // handled through plotly_buttonclicked event
      method: 'skip',
      execute: false,
    }],
  });

  // TODO: package selection (?) Plotly menus are not user-friendly with large data...

  return menus;
}

function getMenuIndex(graphDiv, menuName) {
  var menus = graphElement(graphDiv).layout.updatemenus;
  for (var i = 0; i < menus.length; i++) {
    if (menus[i].name == menuName) {
      return i;
    };
  }
  return -1;
}

function setMenuButtonActive(graphDiv, menuName, active) {
  var index = getMenuIndex(graphDiv, menuName);
  Plotly.relayout(graphDiv, 'updatemenus[' + index + '].active', (active ? 0 : -1));
}

function setHighlightButtonActive(graphDiv, active) {
  setMenuButtonActive(graphDiv, menuName('HIGHLIGHT_MENU'), active);
}

function isHighlightButtonActive(graphDiv) {
  var index = getMenuIndex(graphDiv, menuName('HIGHLIGHT_MENU'));
  return graphElement(graphDiv).layout.updatemenus[index].active == 0;
}

function setClearSelectionsButtonVisible(graphDiv, visible) {
  var index = getMenuIndex(graphDiv, menuName('SELECT_MENU'));
  console.log(index);
  // for some reason this does not need to go through a Plotly.relayout call
  graphElement(graphDiv).layout.updatemenus[index].visible = visible;
}

export {
  getUpdateMenus,
  isHighlightButtonActive,
  highlightUnselected,
  setClearSelectionsButtonVisible,
  clearSelections,
  syncAnnotations,
};

