import copy from 'rollup-plugin-copy';
import { terser } from 'rollup-plugin-terser';

// XXX - would be cleaner to target local build folder and use mvn to copy resources
const TARGET = '../target/classes/skin/resources/script';

export default {
  input: 'plotly/plotly_nuxeo.js',
  output: {
    file: `${TARGET}/plotly.nuxeo.js`,
    format: 'iife',
    name: 'plot', // using IIFE so we need a name for the default export
  },
  plugins: [
    copy({
      targets: [
        { src: 'node_modules/plotly.js-dist/plotly.js', dest: TARGET },
      ],
    }),
    terser()
  ],
};
