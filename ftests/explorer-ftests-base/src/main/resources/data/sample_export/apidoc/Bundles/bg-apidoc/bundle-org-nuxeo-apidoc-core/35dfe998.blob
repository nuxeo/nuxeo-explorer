## About Explorer

These modules provide an API to browse the Nuxeo distribution tree:

    - BundleGroup (maven group or artificial grouping)
      - Bundle
        - Component
          - Service
          - Extension Points
          - Contributions
    - Operations
    - Packages

The Nuxeo Distribution can be:

 - live: in memory (meaning runtime introspection)
 - persisted: saved in Nuxeo Repository as a tree of Documents

The following documentation items are also extracted:

 - documentation that is built-in Nuxeo Runtime descriptors
 - readme files that may be embedded inside the jar

## What it can be used for

 - browse you distribution
 - check that a given contribution is deployed
 - play with Nuxeo Runtime

## Configuration

The template `explorer-sitemode` enables the nuxeo.conf property `org.nuxeo.apidoc.site.mode` and
defines an anonymous user.
The property `org.nuxeo.apidoc.site.mode` comes with a more user friendly design and hides the current
"live" distribution from display and API.

The template `explorer-virtualadmin` disables the usual `Administrator` user creation at database
initialization and adds a virtual admin user with name `apidocAdmin`, whose password can be changed using
nuxeo.conf property `org.nuxeo.apidoc.apidocAdmin.password`.

## Modules

This plugin is composed of 3 bundles:

 - nuxeo-apidoc-core: for the low level API on the live runtime
 - nuxeo-apidoc-repo: for the persistence of exported content on the Nuxeo repository
 - nuxeo-apidoc-webengine: for JAX-RS API and Webview
