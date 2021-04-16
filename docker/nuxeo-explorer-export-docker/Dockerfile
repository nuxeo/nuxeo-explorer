# vim:set ft=dockerfile:
ARG BASE_IMAGE

FROM ${BASE_IMAGE}

ARG VERSION
ARG BUILD_TAG

ARG USE_LOCAL_EXPLORER

ARG CONNECT_EXPLORER_CLID
ARG CONNECT_EXPLORER_URL
ARG NUXEO_EXPLORER_PACKAGE

ARG CONNECT_EXPORT_CLID
ARG CONNECT_EXPORT_URL
ARG EXPORT_PACKAGE_LIST

LABEL org.nuxeo.explorer.export.version=${VERSION}
LABEL org.nuxeo.explorer.export.build-tag=${BUILD_TAG}

USER root

# install explorer package
COPY --chown=900:0 README.md target/nuxeo-platform-explorer-package-*.zip /packages/

RUN if [ "${USE_LOCAL_EXPLORER}" = "true" ]; then \
    mv /packages/nuxeo-platform-explorer-package-*.zip /packages/nuxeo-platform-explorer-package.zip \
    /install-packages.sh --offline /packages/nuxeo-platform-explorer-package.zip >&1; \
  elif [ ! -z "${CONNECT_EXPLORER_CLID}" ]; then \
    /install-packages.sh --clid "${CONNECT_EXPLORER_CLID}" --connect-url "${CONNECT_EXPLORER_URL}" ${NUXEO_EXPLORER_PACKAGE} >&1; \
  fi

# install reference packages
RUN if [ ! -z "${CONNECT_EXPORT_CLID}" ] && [ ! -z "${EXPORT_PACKAGE_LIST// }" ]; then \
    # replace "+" by space on package list because a conversion occurs when built with maven via a property
    /install-packages.sh --clid "${CONNECT_EXPORT_CLID}" --connect-url "${CONNECT_EXPORT_URL}" ${EXPORT_PACKAGE_LIST//+/ } >&1; \
  fi

RUN NUXEO_CONF=$NUXEO_HOME/bin/nuxeo.conf $NUXEO_HOME/bin/nuxeoctl mp-list

# NXP-30350: remove useless files to limit docker image size
RUN rm -rf $NUXEO_HOME/packages/backup
RUN chown -R 900:0 $NUXEO_HOME \
  && chmod -R g+rwX $NUXEO_HOME
USER 900

# override start command to disable strict mode
CMD ["nuxeoctl", "console", "--lenient"]
