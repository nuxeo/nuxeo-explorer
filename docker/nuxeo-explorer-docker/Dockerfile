# vim:set ft=dockerfile:
ARG BASE_IMAGE

FROM ${BASE_IMAGE}

ARG VERSION
ARG SCM_REF
ARG BUILD_TAG

LABEL org.nuxeo.explorer.version=${VERSION}
LABEL org.nuxeo.explorer.scm-ref=${SCM_REF}
LABEL org.nuxeo.explorer.build-tag=${BUILD_TAG}

ENV NUXEO_INSTALL_HOTFIX=false

RUN cp /etc/nuxeo/nuxeo.conf.template $NUXEO_HOME/bin/nuxeo.conf

COPY install-packages.sh /

COPY target/nuxeo-platform-explorer-package-*.zip /tmp/nuxeo-platform-explorer-package.zip
RUN /install-packages.sh --offline /tmp/nuxeo-platform-explorer-package.zip >&1;

# NXP-30350: remove useless files to limit docker image size
RUN rm -rf $NUXEO_HOME/packages/backup && find $NUXEO_HOME/packages/store/* ! -name 'package.xml' -type f -exec rm -f {} +
# Set appropriate permissions on distribution directory (moved from install-packages.sh as it takes ages)
RUN chmod -R g+rwX $NUXEO_HOME
