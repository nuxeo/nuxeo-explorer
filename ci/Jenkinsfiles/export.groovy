/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Anahide Tchertchian
 */

/**
* Script handling automated export of explorer-generated artifacts.
*
* This script will build a docker image with target packages to be snapshotted.
* The snashot will then be exported and uploaded to a target explorer instance.
*/
library identifier: "platform-ci-shared-library@v0.0.22"

// all packages released with the platform
def defaultPackages = 'easyshare nuxeo-csv nuxeo-drive nuxeo-imap-connector nuxeo-multi-tenant nuxeo-platform-importer nuxeo-quota nuxeo-signature nuxeo-template-rendering shibboleth-authentication nuxeo-liveconnect nuxeo-platform-3d'
// additional packades released independently, usually after a platform release promotion
// excluding nuxeo-spreadsheet, not released with JSF-related packages
def additionalPackages = 'cas2-authentication nuxeo-diff nuxeo-platform-user-registration nuxeo-virtualnavigation nuxeo-web-ui nuxeo-jsf-ui nuxeo-arender'

String getCurrentNamespace() {
  container('maven') {
    return sh(returnStdout: true, script: "kubectl get pod ${NODE_NAME} -ojsonpath='{..namespace}'")
  }
}

String getExportImageVersion(String explorerVersion, String nuxeoVersion) {
  def ev = explorerVersion.trim()
  def nv = nuxeoVersion.trim()
  if (ev.isEmpty()) {
    ev = 'unknown';
  }
  return "${ev}-${nv}"
}

String getExplorerPackageId(String version) {
  def res = "platform-explorer"
  if (!version.isEmpty()) {
    res += '-' + version.trim()
  }
  return res
}

String getPackageList(downloadPackagesFromProd, defaultPackages, additionalPackages) {
  def res = "${defaultPackages} ${downloadPackagesFromProd ? additionalPackages : ''}".trim()
  if (res.isEmpty()) {
    // avoid variable not to be declared
    return " "
  }
  return res
}

pipeline {

  agent {
    label 'jenkins-nuxeo-jsf-lts-2021'
  }

  environment {
    CURRENT_NAMESPACE = getCurrentNamespace()

    NUXEO_DOCKER_REGISTRY = 'docker-private.packages.nuxeo.com'
    NUXEO_IMAGE_VERSION = "${params.NUXEO_VERSION}"
    EXPLORER_EXPORT_NAMESPACE = "explorer-export"

    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo/site/'
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo/site/'

    CONNECT_EXPLORER_URL = "${CONNECT_PROD_URL}"
    CONNECT_EXLORER_CLID = 'instance-clid'
    NUXEO_EXPLORER_PACKAGE = getExplorerPackageId(params.NUXEO_EXPLORER_VERSION)

    CONNECT_EXPORT_URL = "${params.DOWNLOAD_PACKAGES_FROM_PROD ? CONNECT_PROD_URL : CONNECT_PREPROD_URL}"
    CONNECT_EXPORT_CLID = "${params.DOWNLOAD_PACKAGES_FROM_PROD ? 'instance-clid': 'instance-clid-preprod'}"
    EXPORT_PACKAGE_LIST = getPackageList(params.DOWNLOAD_PACKAGES_FROM_PROD, params.DEFAULT_PACKAGE_LIST, params.ADDITIONAL_PACKAGE_LIST)

    VERSION = getExportImageVersion(params.NUXEO_EXPLORER_VERSION, NUXEO_IMAGE_VERSION)
    // NXP-29494: override templates to avoid activating s3
    NUXEO_TEMPLATE_OVERRIDE = 'nuxeo.templates=default'

    UPLOAD_EXPORT = "${params.UPLOAD_EXPORT}"
    UPLOAD_URL = "${params.UPLOAD_TO_PROD ? 'https://explorer.nuxeo.com/nuxeo' : 'https://explorer.beta.nuxeocloud.com/nuxeo'}"
    UPLOAD_CREDS_ID = "${params.UPLOAD_TO_PROD ? 'explorer-prod' : 'explorer-beta-nco'}"

    CURL_OPTIONS = "--fail --location --connect-timeout 180 --max-time 300 --retry 2"
    SLACK_CHANNEL = 'explorer-notifs'
  }

  stages {

    stage('Check Parameters') {
      steps {
        script {
          echo """
          ----------------------------------------
          Nuxeo version:                 '${NUXEO_IMAGE_VERSION}'
          Explorer package:              '${NUXEO_EXPLORER_PACKAGE}'

          Connect Explorer URL:          '${CONNECT_EXPLORER_URL}'
          Connect Export URL:            '${CONNECT_EXPORT_URL}'
          Download Packages from Prod:   '${params.DOWNLOAD_PACKAGES_FROM_PROD}'
          Package List:                  '${EXPORT_PACKAGE_LIST}'
          Snapshot Name:                 '${params.SNAPSHOT_NAME}'
          Perform Json Export:           '${params.PERFORM_JSON_EXPORT}'

          Upload Export:                 '${UPLOAD_EXPORT}'
          Upload URL:                    '${UPLOAD_URL}'
          Upload as Promoted:            '${params.UPLOAD_AS_PROMOTED}'
          ----------------------------------------
          """
          if (params.NUXEO_VERSION.isEmpty()) {
            currentBuild.result = 'ABORTED';
            def message = 'NUXEO_VERSION parameters is mandatory'
            currentBuild.description = "${message}"
            echo "${message}"
            error(currentBuild.description)
          }
          currentBuild.description = "Generating reference export for ${NUXEO_IMAGE_VERSION}"
        }
      }
    }

    stage('Set Labels') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Set Kubernetes resource labels
          ----------------------------------------
          """
          echo "Set label 'branch: export' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=export
          """
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Build Explorer Export Docker Image
          ----------------------------------------
          Image tag: ${VERSION}
          Registry: ${DOCKER_REGISTRY}
          """
          script {
            def moduleDir = 'docker/nuxeo-explorer-export-docker'
            withCredentials([string(credentialsId: CONNECT_EXLORER_CLID, variable: 'CONNECT_EXPLORER_CLID_VALUE'),
                             string(credentialsId: CONNECT_EXPORT_CLID, variable: 'CONNECT_EXPORT_CLID_VALUE')]) {
              // replace lines by "--"
              def oneLineExplorerClid = sh(
                returnStdout: true,
                script: '''#!/bin/bash +x
                  echo -e "$CONNECT_EXPLORER_CLID_VALUE" | sed ':a;N;\$!ba;s/\\n/--/g'
                '''
              )
              def oneLineExportClid = sh(
                returnStdout: true,
                script: '''#!/bin/bash +x
                  echo -e "$CONNECT_EXPORT_CLID_VALUE" | sed ':a;N;\$!ba;s/\\n/--/g'
                '''
              )
              withEnv(["CONNECT_EXPLORER_CLID=${oneLineExplorerClid}", "CONNECT_EXPORT_CLID=${oneLineExportClid}"]) {
                sh "envsubst < ${moduleDir}/skaffold.yaml > ${moduleDir}/skaffold.yaml~gen"
              }
            }
            sh "skaffold build -f ${moduleDir}/skaffold.yaml~gen"
          }
        }
      }
    }

    stage('Perform Export') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Explorer export environment
          ----------------------------------------"""
          nxWithHelmfileDeployment(namespace: EXPLORER_EXPORT_NAMESPACE, secrets: [[name: 'instance-clid', namespace: 'platform']]) {
            script {
              String nuxeoUrl = "nuxeo.${EXPLORER_EXPORT_NAMESPACE}.svc.cluster.local/nuxeo"
              String explorerUrl = "${nuxeoUrl}/site/distribution"
              echo """
              ----------------------------------------
              Perform export on ${explorerUrl}
              ----------------------------------------
              """
              String distribId = "${params.SNAPSHOT_NAME}-${NUXEO_IMAGE_VERSION}".replaceAll(" ", "%20")
              String curlCommand = "curl --user Administrator:Administrator ${CURL_OPTIONS}"
              retry (2) {
                sh """
                  ${curlCommand} -d 'name=${params.SNAPSHOT_NAME}' -d 'version=${NUXEO_IMAGE_VERSION}' -H 'Accept: text/plain' ${explorerUrl}/save
                  ${curlCommand} ${explorerUrl} --output home_after_save.html
                  ${curlCommand} ${nuxeoUrl}/site/automation/Elasticsearch.WaitForIndexing -H 'Content-Type:application/json' -X POST -d '{"params":{"timeoutSecond": "3600", "refresh": "true", "waitForAudit": "true"},"context":{}}'
                """
              }
              retry (2) {
                sh "${curlCommand} ${explorerUrl}/download/${distribId} --output export.zip"
              }
              if (params.PERFORM_JSON_EXPORT) {
                retry (2) {
                  sh "${curlCommand} ${explorerUrl}/${distribId}/json --output export.json"
                }
              }
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/home*.html, **/export.json, **/export.zip, **/requirements.lock, **/charts/*.tgz, **/target/**/*.yaml'
        }
      }
    }

    stage('Upload Snapshot to Explorer') {
      when {
        not {
          environment name: 'UPLOAD_EXPORT', value: 'false'
        }
      }
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Upload Export to ${UPLOAD_URL}
            ----------------------------------------"""
            withCredentials([usernameColonPassword(credentialsId: UPLOAD_CREDS_ID, variable: 'EXPLORER_PASS')]) {
              String curlCommand = 'curl --user $EXPLORER_PASS $CURL_OPTIONS'
              def aliases = 'latest'
              if (!params.UPLOAD_AS_PROMOTED) {
                def xVersion = sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/x/e' <<< ${NUXEO_IMAGE_VERSION}").trim()
                aliases = "next\n${xVersion}"
              }
              if (!params.UPLOAD_ALIASES.trim().isEmpty()) {
                aliases += "\n${params.UPLOAD_ALIASES}"
              }
              sh """
                ${curlCommand} -Fsnap=@\$(find "\$(pwd)"/export.zip -type f) -F \$'nxdistribution:aliases=${aliases}' ${UPLOAD_URL}/site/distribution/uploadDistrib
              """
              currentBuild.description = "Uploaded reference export for ${NUXEO_IMAGE_VERSION}"
            }
          }
        }
      }
    }
  }

  post {
    success {
      script {
        if (env.UPLOAD_EXPORT == "true" && !hudson.model.Result.SUCCESS.toString().equals(currentBuild.getPreviousBuild()?.getResult())) {
          slackSend(channel: "${SLACK_CHANNEL}", color: "good", message: "Successfully <${BUILD_URL}|uploaded> nuxeo-explorer reference export for ${NUXEO_IMAGE_VERSION} :robot_face:")
        }
      }
    }
    unsuccessful {
      script {
        if (env.UPLOAD_EXPORT == "true") {
          slackSend(channel: "${SLACK_CHANNEL}", color: "danger", message: "Failed to <${BUILD_URL}|upload> nuxeo-explorer reference export for ${NUXEO_IMAGE_VERSION}")
        }
      }
    }
  }

}
