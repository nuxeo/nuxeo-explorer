import { graphElement } from './constants.js';


function getInitialAnnotations(lnxgraph) {
  var gd = graphElement(lnxgraph.div);
  var annotations = [];
  // init but dereference annotations as the array will be changed by update
  if (lnxgraph.is3D && gd.layout.scene.annotations) {
      annotations = [...gd.layout.scene.annotations];
  } else if (gd.layout.annotations) {
      annotations = [...gd.layout.annotations];
  }
  return annotations;
}

function getAnnotationsLayoutUpdate(lnxgraph, annotations) {
  var layout_update = {};
  if (lnxgraph.is3D) {
      layout_update = {
          scene: {
              annotations: annotations,
              // preserve camera eye position
              camera: graphElement(lnxgraph.div).layout.scene.camera,
          },
      };
  } else {
      layout_update = { annotations: annotations };
  }
  //console.log("annot lupdate ", layout_update);
  return layout_update;
}

export {
  getInitialAnnotations,
  getAnnotationsLayoutUpdate,
};