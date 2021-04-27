# vim:set ft=dockerfile:
ARG BASE_IMAGE

FROM ${BASE_IMAGE}

ARG VERSION
ARG SCM_REF
ARG BUILD_TAG

LABEL org.nuxeo.explorer.version=${VERSION}
LABEL org.nuxeo.explorer.scm-ref=${SCM_REF}
LABEL org.nuxeo.explorer.build-tag=${BUILD_TAG}

# Workaround missing support for --chown flag with COPY instruction in Kaniko
# TODO: remove and use COPY --chown when fixed in Kaniko, or find a proper way
COPY --chown=900:0 target/nuxeo-platform-explorer-package-*.zip /packages/nuxeo-platform-explorer-package.zip
COPY --chown=900:0 target/nuxeo-amazon-s3-package-*.zip /packages/nuxeo-amazon-s3-package.zip
# Workaround of above permission issues, otherwise we can't clean the package once installed
USER root
RUN /install-packages.sh --offline /packages/nuxeo-platform-explorer-package.zip /packages/nuxeo-amazon-s3-package.zip >&1;

# NXP-30350: remove useless files to limit docker image size
RUN rm -rf $NUXEO_HOME/packages/backup && find $NUXEO_HOME/packages/store/* ! -name 'package.xml' -type f -exec rm -f {} +

RUN chown -R 900:0 $NUXEO_HOME \
  && chmod -R g+rwX $NUXEO_HOME
USER 900
