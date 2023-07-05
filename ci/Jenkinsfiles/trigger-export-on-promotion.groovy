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
* Script handling trigger of automated export on nuxeo promotion.
*
* This script will trigger job using pipeline at export.groovy with build-related parameters.
* Expected related job names:
* - /nuxeo/release-nuxeo as upstream job
* - export-platform-explorer-reference job (in same directory) as downstream/triggered job
*/

import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

pipeline {

  agent {
    label 'jenkins-nuxeo-jsf-lts-2021'
  }

  environment {
    SLACK_CHANNEL = 'explorer-notifs'
  }

  stages {

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

    stage('Trigger Export on Nuxeo Promotion') {
      steps {
        script {
          def jobParams = [
            booleanParam(name: 'UPLOAD_AS_PROMOTED', value: true),
            booleanParam(name: 'DOWNLOAD_PACKAGES_FROM_PROD', value: true),
            // do not upload to prod for now
            //booleanParam(name: 'UPLOAD_TO_PROD', value: true)
          ]
          if (!params.DEFAULT_PACKAGE_LIST.trim().isEmpty()) {
            jobParams.add(text(name: 'DEFAULT_PACKAGE_LIST', value: params.DEFAULT_PACKAGE_LIST))
          }
          if (!params.ADDITIONAL_PACKAGE_LIST.trim().isEmpty()) {
            jobParams.add(text(name: 'ADDITIONAL_PACKAGE_LIST', value: params.ADDITIONAL_PACKAGE_LIST))
          }

          def hasUpstream = false
          def wasTriggered = false;
          for (RunWrapper b: currentBuild.upstreamBuilds) {
            echo "Checking upstream: ${b.projectName} with description '${b.description}'"
            if (b.projectName != 'test-trigger' && b.projectName != 'release-nuxeo-2021' && b.projectName != 'release-nuxeo-jsf-ui') {
              continue
            }
            hasUpstream = true
            // parse description which should look like "Release 2021.22 from build 2021.22.4"
            def matcher1 = (b.description =~ /Release\s(\d+\.\d+)\sfrom\sbuild\s(\d+\.\d+\.\d+).*/)
            // parse description which should look like "Release 2021.22.4"
            def matcher2 = (b.description =~ /Release\s((\d+\.\d+)\.\d+).*/)
            if (matcher1.matches() || matcher2.matches()) {
              def promotedVersion;
              def originalVersion;
              if (matcher1.matches()) {
                promotedVersion = matcher1[0][1]
                originalVersion = matcher1[0][2]
              } else if (matcher2.matches()) {
                promotedVersion = matcher2[0][2]
                originalVersion = matcher2[0][1]
              }
              echo "Parsed promoted version: ${promotedVersion}"
              jobParams.add(string(name: 'NUXEO_VERSION', value: promotedVersion))

              echo "Parsed original version: ${originalVersion}"
              jobParams.add(string(name: 'UPLOAD_ALIASES', value: originalVersion))

              echo "Triggering job with parameters: ${jobParams} for ${b.absoluteUrl}"
              build job: 'export-platform-explorer-reference', parameters: jobParams, wait: false
              wasTriggered = true
              currentBuild.description = "Triggered export for ${promotedVersion}"
              // stop at first matching upstream build as they can pile up when using relaunch
              break;
            } else {
              def message = "Version not found on upstream build description: ${b.description}"
              currentBuild.description = "${message}"
              currentBuild.result = 'FAILURE';
              echo "Failing with message: ${message}"
              error(currentBuild.description)
            }
          }

          if (!hasUpstream && !params.PROMOTED_NUXEO_VERSION.isEmpty()) {
            // same trigger with given job param as version
            jobParams.add(string(name: 'NUXEO_VERSION', value: params.PROMOTED_NUXEO_VERSION))
            jobParams.add(string(name: 'UPLOAD_ALIASES', value: params.ORIGINAL_NUXEO_VERSION))
            build job: 'export-platform-explorer-reference', parameters: jobParams, wait: false
            wasTriggered = true
            currentBuild.description = "Triggered export for ${params.PROMOTED_NUXEO_VERSION}"
          }

          if (!wasTriggered) {
            def message = 'No build triggered.'
            currentBuild.description = "${message}"
            currentBuild.result = 'FAILURE';
            echo "Failing with message: ${message}"
          }

        }
      }
    }

  }

  post {
    unsuccessful {
      script {
        slackSend(channel: "${SLACK_CHANNEL}", color: "danger", message: "Failed to trigger nuxeo-explorer reference export for Nuxeo ${params.PROMOTED_NUXEO_VERSION}: <${BUILD_URL}|#${BUILD_NUMBER}>")
      }
    }
  }

}
