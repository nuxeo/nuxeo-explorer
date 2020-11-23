export function getBasicLayout(nxgraph) {
  var axis = {
    showbackground: false,
    showspikes: false,
    showgrid: false,
    showticklabels: false,
    zeroline: false,
    title: '',
  },
    scene = {
      xaxis: axis,
      yaxis: axis,
    };

  var title = `<b>${nxgraph.fig.title}</b>`;
  if (nxgraph.options.datasourcetitle) {
    title += ` (${nxgraph.options.datasourcetitle})`;
  }
  if (nxgraph.fig.description) {
    title += `<br><i>${nxgraph.fig.description}</i>`;
  }

  var layout = {
    title: { text: title },
    showlegend: true,
    hovermode: 'closest',
    hoverdistance: 50, // no effect on 3d...
    paper_bgcolor: '#eee',
    width: 2000,
    height: 2000,
    legend: {
      borderwidth: 1,
      bgcolor: '#fff',
      tracegroupgap: 30,
    },
  };

  if (nxgraph.is3D) {
    Object.assign(scene, {
      zaxis: axis,
      camera: {
        // default camera eye: 1.25, 1.25, 1.25
        eye: { x: 0.8, y: 2, z: 0.8 }
      }
    });
    Object.assign(layout, { scene: scene });
  } else {
    // axis directly on layout, outside of scene, in 2D
    Object.assign(layout, scene);
  }
  return layout;
}