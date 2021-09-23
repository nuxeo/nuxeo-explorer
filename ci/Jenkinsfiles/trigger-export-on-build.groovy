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
* Script handling trigger of automated export on nuxeo build.
*
* This script will trigger the job using pipeline at export.groovy with build-related parameters.
* Expected related job names:
* - /nuxeo/nuxeo/master as upstream job
* - export-platform-explorer-reference job (in same directory) as downstream/triggered job
*/

import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '10', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

pipeline {

  agent {
    label 'jenkins-nuxeo-package-lts-2021'
  }

  parameters {
    string(name: 'NUXEO_VERSION', defaultValue: '', description: 'Version of the target Nuxeo Server Image.\nSample: \'11.3.48\'.\nWill be ignored if jobs is triggered from upstream build.')
    text(name: 'DEFAULT_PACKAGE_LIST', defaultValue: '', description: 'The list of packages to install for snapshot.\nSample: \'nuxeo-csv nuxeo-quota-1.0.0\'.\nWill override the default job triggered job value if not empty.')
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

    stage('Trigger Export on Nuxeo Build') {
      steps {
        script {
          def jobParams = []
          if (!params.DEFAULT_PACKAGE_LIST.trim().isEmpty()) {
            jobParams.add(string(name: 'DEFAULT_PACKAGE_LIST', value: params.DEFAULT_PACKAGE_LIST))
          }

          def hasUpstream = false
          def wasTriggered = false
          for (RunWrapper b: currentBuild.upstreamBuilds) {
            if (b.projectName != 'test-trigger' && b.fullProjectName != 'nuxeo/lts/nuxeo/2021') {
              continue
            }
            hasUpstream = true
            // parse description which should look like "Build 11.3.49" or "Build 11.3.49: comment"
            def matcher = (b.description =~ /Build\s(\d+\.\d+\.\d+).*/)
            if (matcher.matches()) {
              def version = matcher[0][1]
              echo "Parsed version: ${version}"
              jobParams.add(string(name: 'NUXEO_VERSION', value: version))
              echo "Triggering job with parameters: ${jobParams} for ${b.absoluteUrl}"
              build job: 'export-platform-explorer-reference', parameters: jobParams, wait: false
              wasTriggered = true
              currentBuild.description = "Triggered export for ${version}"
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

          if (!hasUpstream && !params.NUXEO_VERSION.isEmpty()) {
            // same trigger with given job param as version
            jobParams.add(string(name: 'NUXEO_VERSION', value: params.NUXEO_VERSION))
            build job: 'export-platform-explorer-reference', parameters: jobParams, wait: false
            wasTriggered = true
            currentBuild.description = "Triggered export for ${params.NUXEO_VERSION}"
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
        slackSend(channel: "${SLACK_CHANNEL}", color: "danger", message: "Failed to trigger nuxeo-explorer reference export for Nuxeo ${params.NUXEO_VERSION}: <${BUILD_URL}|#${BUILD_NUMBER}>")
      }
    }
  }

}
