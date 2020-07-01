nuxeo/nuxeo-explorer Docker Image
===================================

This module is reponsible to build the Explorer Docker image.

Locally, the image can be built with Maven:
```bash
mvn clean install
```

Then the [tests](./test) can be run with [container-structure-test](https://github.com/GoogleContainerTools/container-structure-tests).
You can navigate to the repository in order to install it and then just type (don't forget to replace tag):
```bash
container-structure-test test --image nuxeo/nuxeo-explorer:${project.version} --config test/*.yml
```

On Jenkins X, skaffold takes care of building and testing the image.
