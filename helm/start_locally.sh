#!/bin/bash

# global parameters and utilities
export DOCKER_REGISTRY="docker-private.packages.nuxeo.com"

exec 4>&2        # logging stream (file descriptor 4) defaults to STDERR
exec 5>/dev/null # execution stream (file descriptor 5) defaults to /dev/null
verbosity=3      # default to show infos
silent_lvl=0
err_lvl=1
wrn_lvl=2
inf_lvl=3
dbg_lvl=4
trc_lvl=5

log_notify() { log $silent_lvl "$1"; } # Always prints
log_error() { log $err_lvl "[ERROR] $1"; }
log_warn() { log $wrn_lvl "[WARN] $1"; }
log_info() { log $inf_lvl "[INFO] $1"; }
log_debug() { log $dbg_lvl "[DEBUG] $1"; }
log() {
  color=""
  normal=""
  if tput colors >/dev/null; then
    color="$(tput sgr0)"
    normal="$(tput sgr0)"
    case "$1" in
    1) color="\033[1;31m" ;; # red
    2) color="033[1;33m" ;; # yellow
    3) color="\033[1;34m" ;; # blue
    4) color="\033[36m" ;; # cyan
    esac
  fi
  if [ $verbosity -ge "$1" ]; then
    # Expand escaped characters
    echo -e "$color$2$normal" >&4
  fi
}

replace_in_file() {
  command="sed"
  # change executable for OSX environments
  if type gsed >/dev/null; then
    command="gsed"
  fi
  ${command} -i "$1" "$2" >&5
}

usage() {
  echo "Usage:"
  echo "  $0 [OPTIONS]"
  echo "Options:"
  echo "  -h               : display this help message"
  echo "  -q               : decrease verbosity level (can be repeated: -qq, -qqq)"
  echo "  -v               : increase verbosity level (can be repeated: -vv, -vvv)"
  echo "  -i instance_clid : use this instance_clid instead of prompting"
  echo "  -n namespace     : install the preview into this namespace instead of 'nuxeo-explorer'"
  echo "  -V version       : use this version instead of the one in pom.xml"
}

initWorkspace() {
  helm_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" >&5 && pwd)"
  project_dir="${helm_dir}/.."
  working_dir="${helm_dir}/target"
  mkdir -p "${working_dir}"
}

getOpts() {
  while getopts ":hqvi:n:V:" o; do
    case "${o}" in
    h)
      usage
      exit 0
      ;;
    q)
      ((verbosity = verbosity - 1))
      ;;
    v)
      ((verbosity = verbosity + 1))
      ;;
    i)
      instance_clid=${OPTARG}
      ;;
    n)
      namespace=${OPTARG}
      ;;
    V)
      version=${OPTARG}
      ;;
    :)
      log_error "Invalid option: $OPTARG requires an argument"
      exit 2
      ;;
    *)
      log_error "Invalid options: $1"
      usage
      exit 1
      ;;
    esac
  done
}

evaluateMavenExpression() {
  mvn -nsu org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression="$1" -f "$2" -q -DforceStdout 2>&5
}

prepareParameters() {
  log_info "Prepare deployment"
  if [[ "$verbosity" -ge "$trc_lvl" ]]; then
    set -x
    exec 5>&2 # redirect execution stream to STDERR
  fi
  if [[ -n "${version}" ]]; then
    log_debug "A version was supplied - read explorer version from docker image"
    explorer_hmi_image="${DOCKER_REGISTRY}/nuxeo/nuxeo-explorer:${version}"
    docker pull "${explorer_hmi_image}" >&5
    explorer_version=$(docker inspect --format="{{index .Config.Labels \"explorer-version\"}}" "${explorer_hmi_image}" 2>&5)
  else
    log_debug "No version supplied through CLI parameters - read it from pom.xml"
    version=$(evaluateMavenExpression "project.version" "${project_dir}/pom.xml")
    explorer_version=$(evaluateMavenExpression "explorer.version" "${project_dir}/pom.xml")
  fi

  if [[ -z "${namespace}" ]]; then
    log_debug "No namespace supplied through CLI parameters - default to nuxeo-explorer"
    namespace="nuxeo-explorer"
  fi

  # create instance-clid secret if it doesn't exist
  # we use default namespace in order to keep the secret after preview namespace deletion
  if ! kubectl -n default get secret | grep instance-clid >&5; then
    log_info "Create secret: instance-clid in: default namespace"
    # ask for clid path if cli not submitted on command line
    if [[ -z "${instance_clid}" ]]; then
      instance_clid_path=$(whiptail --title "Nuxeo Explorer" --inputbox "Enter the full path of a Nuxeo instance.clid file:" 10 60 "$PWD/instance.clid" 3>&1 1>&2 2>&3)
      kubectl -n default create secret generic instance-clid --from-file=instance.clid="${instance_clid_path}" >&5
    else
      kubectl -n default create secret generic instance-clid --from-literal=instance.clid="${instance_clid}" >&5
    fi
  else
    log_debug "Secret: instance-clid exists in: default namespace"
  fi
  # retrieve instance.clid value
  instance_clid=$(kubectl -n default get secret instance-clid --output="jsonpath={.data.instance\.clid}" 2>&5)
}

deploy() {
  local_values="${working_dir}/values.yaml"

  log_debug "Prepare: ${local_values} to deploy preview locally"
  EXPLORER_VERSION=${explorer_version} PREVIEW_NAMESPACE=${namespace} VERSION=${version} \
    envsubst <"${helm_dir}/preview/values.yaml" 1>"${local_values}" 2>&5

  replace_in_file 's/platform.dev.nuxeo.com/docker.localhost/g' "${local_values}"
  replace_in_file 's/https/http/g' "${local_values}"

  log_info "Start deployment"
  # create preview namespace if it doesn't exist
  if ! kubectl get namespace | grep nuxeo-explorer >&5; then
    log_info "Create namespace: ${namespace}"
    kubectl create namespace ${namespace} >&5
  else
    log_debug "Using namespace: ${namespace}"
  fi

  # copy secret to preview namespace
  if ! kubectl -n "${namespace}" get secret | grep preview-docker-cfg >&5; then
    log_info "Copy docker config secret: docker-cfg from: default namespace to: ${namespace} namespace"
    docker_cfg=$(kubectl -n default get secret docker-cfg --output="jsonpath={.data.\.dockerconfigjson}" | base64 --decode 2>&5)
    kubectl -n "${namespace}" create secret generic preview-docker-cfg \
      --from-literal=.dockerconfigjson="${docker_cfg}" \
      --type=kubernetes.io/dockerconfigjson >&5
  else
    log_debug "Secret: preview-docker-cfg exists in: ${namespace} namespace"
  fi

  nuxeo_image_id="unknown"
  if [[ $version == *SNAPSHOT ]]; then
    nuxeo_image_id=$(docker images nuxeo/nuxeo-explorer:"${version}" -q 2>&5)
  fi

  log_debug "Init Helm and update chart dependencies"
  helm init --wait >&5
  helm repo add jenkins-x http://chartmuseum.jenkins-x.io >&5
  helm repo add nuxeo-platform https://chartmuseum.platform.dev.nuxeo.com >&5
  helm dep update "${helm_dir}/preview" >&5
  log_debug "Deploy Helm chart"
  helm upgrade arender-preview "${helm_dir}/preview" --install --cleanup-on-fail --namespace ${namespace} -f "${local_values}" \
    --set expose.config.domain=docker.localhost \
    --set expose.config.tlsacme=false \
    --set expose.config.tlsSecretName=~ \
    --set local.instance_clid="${instance_clid}" \
    --set nuxeo.nuxeo.image.repository=nuxeo/nuxeo-explorer \
    --set nuxeo.nuxeo.image.pullPolicy=IfNotPresent \
    --set nuxeo.nuxeo.podAnnotations."preview/scm\.ref"="image-${nuxeo_image_id}"
  log_info "Preview deployed locally, check stack status with: kubectl -n ${namespace} get pod"
}

initWorkspace
getOpts "$@"
prepareParameters
deploy
