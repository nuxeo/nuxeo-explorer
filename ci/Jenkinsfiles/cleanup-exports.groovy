/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

/**
* Script cleaning up exports on target explorer site.
*
* Cleans up oldest distributions marked as "next" on preview explorer site, keeping only a given number of them.
* Allows avoiding piling up exports from nuxeo builds on preview.
* Triggered daily by a cron.
*/
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '10', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

pipeline {

  agent {
    label 'jenkins-nuxeo-package-lts-2021'
  }

  triggers {
    // every day between 8AM and 10AM
    cron('H H(8-10) * * *')
  }

  parameters {
    string(name: 'NUMBER_KEEP', defaultValue: '15', description: 'Number of exports to keep.')
    string(name: 'TARGET_URL', defaultValue: 'https://preview-nuxeo-explorer-master.platform.dev.nuxeo.com/nuxeo', description: 'Target Explorer instance to cleanup.')
    string(name: 'TARGET_CREDS_ID', defaultValue: 'explorer-preview', description: 'Target Explorer instance credentials id.')
  }

  environment {
    CURL_OPTIONS = "--fail --location --connect-timeout 180 --max-time 300 --retry 2"
    SLACK_CHANNEL = 'explorer-notifs'
  }

  stages {
    stage('Check Parameters') {
      steps {
        script {
          def errorMessage = ""
          if (params.TARGET_URL.isEmpty() || params.TARGET_CREDS_ID.isEmpty()) {
            errorMessage = 'TARGET_URL and TARGET_CREDS_ID parameters are mandatory'
          } else if (!params.NUMBER_KEEP.isInteger() || params.NUMBER_KEEP.toInteger() < 0) {
            errorMessage = "Invalid number of exports '${params.NUMBER_KEEP}' (expecting integer >= 0)"
          }
          if (!errorMessage.isEmpty()) {
            currentBuild.result = 'ABORTED';
            currentBuild.description = "${errorMessage}"
            echo "${errorMessage}"
            error(currentBuild.description)
          }
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
    stage('Cleanup exports') {
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Cleanup Exports on ${TARGET_URL}
            ----------------------------------------"""
            withCredentials([usernameColonPassword(credentialsId: TARGET_CREDS_ID, variable: 'EXPLORER_PASS')]) {
              def curlCommand = "curl --user ${EXPLORER_PASS} ${CURL_OPTIONS}"
              def query = "SELECT * FROM NXDistribution WHERE nxdistribution:aliases='next' ORDER BY dc:created ASC"
              def curlGet = "${curlCommand} -G --data-urlencode \"query=${query}\" ${TARGET_URL}/api/v1/search/lang/NXQL/execute"
              def responseGet = sh(script: curlGet, returnStdout: true).trim()
              def json = readJSON text: responseGet

              def num = json.entries.size()
              def numDeleted = 0
              def numToDelete = num - params.NUMBER_KEEP.toInteger()
              if (numToDelete > 0) {
                for (def entry: json.entries) {
                  echo "Deleting ${entry.uid}"
                  // perform delete
                  sh(script: "${curlCommand} -X DELETE ${TARGET_URL}/api/v1/id/${entry.uid}")
                  numDeleted++
                  if (numDeleted >= numToDelete) {
                    break
                  }
                }
              }
              currentBuild.description = "Deleted ${numDeleted} out of ${num} exports"
              echo "${currentBuild.description}"
            }
          }
        }
      }
    }
  }

  post {
    unsuccessful {
      script {
        slackSend(channel: "${SLACK_CHANNEL}", color: "danger", message: "Failed to cleanup old exports on ${params.TARGET_URL}: <${BUILD_URL}|#${BUILD_NUMBER}>")
      }
    }
  }

}
