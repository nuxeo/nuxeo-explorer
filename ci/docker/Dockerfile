ARG NUXEO_VERSION=2025

FROM docker-private.packages.nuxeo.com/nuxeo/nuxeo:${NUXEO_VERSION}

ARG BUILD_TAG=unknown
ARG SCM_REF=unknown
ARG VERSION=unknown

LABEL org.nuxeo.explorer.build-tag=$BUILD_TAG
LABEL org.nuxeo.explorer.scm-ref=$SCM_REF
LABEL org.nuxeo.explorer.version=$VERSION

COPY --chown=900:0 target/nuxeo-platform-explorer-package-$VERSION.zip /packages/
