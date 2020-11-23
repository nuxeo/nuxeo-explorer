import { getAnnotationsLayoutUpdate, getInitialAnnotations } from './annotating.js';
import { graphElement, MARKER_TYPES, TRACE_TYPES } from './constants.js';
import { highlightUnselected, isHighlightButtonActive, setClearSelectionsButtonVisible } from './menus.js';

function createFakePoint(id, isNode) {
  return {
    fake: true,
    customdata: {
      markertype: (isNode ? MARKER_TYPES.NODE : MARKER_TYPES.EDGE),
      id: id,
    }
  };
};

function getSelectionInfo(lnxgraph, id, trace, traceindex, toggle) {
  var indexes = trace.references[id];
  if (!indexes) {
    return null;
  }
  var index = indexes[0];
  if (trace.tracetype == TRACE_TYPES.EDGE) {
    // annotate the middle marker
    index = indexes[parseInt(indexes.length / 2) + 1];
  }
  var info = {
    x: trace.x[index],
    y: trace.y[index],
    links: trace.customdata[index].links,
    visible: trace.visible === true || trace.visible === undefined,
    traceindexes: [traceindex],
  }
  if (lnxgraph.is3D) {
    Object.assign(info, { z: trace.z[index] });
  }
  if (!toggle) {
    // fill up other info that will be useful for annotation creation
    Object.assign(info, {
      annotationtext: trace.customdata[index].annotation,
      isEdge: trace.tracetype != TRACE_TYPES.NODE, // XX: flat edges should not be flagged as is maybe
      isFlatEdge: trace.isFlatEdge == true,
    });
    var color = trace.originalcolors;
    if (Array.isArray(color)) {
      color = trace.originalcolors[index];
    }
    if (trace.tracetype == TRACE_TYPES.NODE) {
      var textcolor = color > 1 ? 'black' : 'white';
      Object.assign(info, { color: color, textcolor: textcolor });
    } else {
      Object.assign(info, { color: color, textcolor: 'black' });
    }
  }
  //console.log("selection info for " + id + ": ", info);
  return info;
}

// change selected points and relations to:
// - udpate colors on nodes (handling rgb opacity in 3D)
// - update width on edges (on 2D)
// - show corresponding annotations on the fly
function selectPoint(lnxgraph, point) {
  var isFirstSelection = lnxgraph.selected.length == 0;
  if (isFirstSelection) {
    setClearSelectionsButtonVisible(lnxgraph.div, true);
  }
  var traces = graphElement(lnxgraph.div).data;
  var traceupdates = traces.reduce(function (res, trace) {
    if (trace.selectedindexes) {
      res.push([...trace.selectedindexes]);
    } else {
      res.push([]);
    }
    return res;
  }, []);
  var update = {
    traces: traces,
    annotations: getInitialAnnotations(lnxgraph),
    traceupdates: traceupdates,
  };
  selectPointRecursive(lnxgraph, update, point, true);
  var dataUpdate = { selectedindexes: update.traceupdates };
  var layoutUpdate = getAnnotationsLayoutUpdate(lnxgraph, update.annotations);
  var traceIndices = [...Array(traceupdates.length).keys()];
  Plotly.update(lnxgraph.div, dataUpdate, layoutUpdate, traceIndices);
  // maybe highlight new selections
  if (!isHighlightButtonActive(lnxgraph.div)) {
    highlightUnselected(lnxgraph, false);
  }
}

function selectPointRecursive(lnxgraph, update, point, doTarget) {
  //console.log("selectPointRecursive: ", point);
  var id = point.customdata.id,
    toggle = lnxgraph.selected.includes(id);
  if (toggle) {
    lnxgraph.selected.splice(lnxgraph.selected.indexOf(id), 1);
  } else {
    lnxgraph.selected.push(id);
  }

  var sinfo = null;
  // for each trace, get the element index for this node, and perform selection
  for (var ti = 0; ti < update.traces.length; ti++) {
    var trace = update.traces[ti];
    var indexes = trace.references[id];
    if (indexes) {
      if (sinfo == null) {
        sinfo = getSelectionInfo(lnxgraph, id, trace, ti, toggle);
      } else {
        sinfo.traceindexes.push(ti);
      }
      var tup = update.traceupdates[ti];
      for (var i of indexes) {
        if (toggle) {
          tup = tup.splice(tup.indexOf(i), 1);
        } else {
          tup.push(i);
        }
      }
    }
  }

  if (sinfo == null) {
    // no reference to selected point, should not happen
    return;
  }

  console.log("sinfo: ", sinfo);
  // annotations
  updateAnnotation(lnxgraph, update, sinfo, !toggle);
  // follow links
  if (sinfo.links && sinfo.links.length > 0) {
    var links = sinfo.links;
    console.log("links ", links);
    var markertype = point.customdata.markertype;
    console.log("markertype", markertype);
    console.log("doTarget", doTarget);
    if (markertype == MARKER_TYPES.NODE && doTarget) {
      // select all link edges recursively
      console.log("linking edges ", links);
      for (var link of links) {
        selectPointRecursive(lnxgraph, update, createFakePoint(link, false), false);
      }
    } else if (markertype == MARKER_TYPES.EDGE) {
      if (doTarget) {
        // select all links recursively
        console.log("linking nodes ", links);
        for (var link of links) {
          selectPointRecursive(lnxgraph, update, createFakePoint(link, true), false);
        }
      } else {
        console.log("linking edge ", links[1]);
        // at least select target node
        selectPointRecursive(lnxgraph, update, createFakePoint(links[1], true), false);
      }
    }
  }
}

function updateAnnotation(lnxgraph, update, sinfo, doCreate) {
  if (doCreate) {
    var ax = lnxgraph.is3D ? -100 : -50;
    if (lnxgraph.is3D && sinfo.isEdge && !sinfo.isFlatEdge) {
      ax = -200;
    }
    var ay = lnxgraph.is3D ? -150 : -100;
    if (lnxgraph.is3D && sinfo.isEdge) {
      ay = sinfo.isFlatEdge ? 150 : 0;
    }
    var annotation = {
      x: sinfo.x,
      y: sinfo.y,
      ax: ax,
      ay: ay,
      text: sinfo.annotationtext,
      opacity: 0.7,
      font: {
        size: 20,
        color: sinfo.textcolor,
      },
      bgcolor: sinfo.color,
      showarrow: true,
      arrowhead: 2,
      bordercolor: 'black',
      borderwidth: 1,
      borderpad: 4,
      // handle events on annotation
      captureevents: true,
      // make annotation visible only if trace is visible
      visible: sinfo.visible == true,
      // keep trace index for annotation visibility trigger on trace visibility
      traceindexes: sinfo.traceindexes,
    }
    if (lnxgraph.is3D) {
      Object.assign(annotation, { z: sinfo.z });
    }
    update.annotations.push(annotation);
  } else {
    // remove annotation
    var aindex = null;
    var is3D = lnxgraph.is3D;
    var annotations = update.annotations;
    for (var i = 0; i < annotations.length; i++) {
      if (annotations[i].x === sinfo.x && annotations[i].y == sinfo.y && (!is3D || annotations[i].z == sinfo.z)) {
        aindex = i;
        break;
      }
    }
    annotations.splice(aindex, 1);
  }
}

export {
  createFakePoint,
  selectPoint,
};