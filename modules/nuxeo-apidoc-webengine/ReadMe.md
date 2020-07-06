
## About nuxeo-apidoc-webengine

This bundle provide a WebEngine API on top of the nuxeo-apidoc-core API.

Each object provided by the core layer is wrapped in a WebObject that is then associated with views.

The same WebEngine API can be used in 2 mode :

 - embedded mode: can be embedded as frames, as it was embedded inside the JSF admin center, for instance
 - full model: in /site/distribution/
