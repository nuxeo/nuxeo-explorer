nuxeo/nuxeo-explorer-export Docker Image
========================================

This module is reponsible for building the Explorer Docker image for
automated exports.

Compared to the main docker image, this image can take an independent
Nuxeo version to snapshot, a given explorer package version to install
(should be compatible with the target Nuxeo image version), and a list
of additional packages to be installed for snapshotting.

This image does not aim at being published: it is only interesting to
be started and for an export to be generated. This export is the
actual archive that aims at being saved/exported somewhere.

Locally, the image can be built with Maven:
```bash
mvn clean install -Dconnect.explorer.clid=CLID -Dconnect.explorer.url=https://connect.nuxeo.com/nuxeo/site/ -Dnuxeo.explorer.version=20.0.0-RC1 -Dconnect.export.clid=CLID -Dconnect.explorer.url=https://connect.nuxeo.com/nuxeo/site/ -Dexport.package.list="nuxeo-quota platform-explorer"
```

On Jenkins X, skaffold takes care of building and testing the image.
