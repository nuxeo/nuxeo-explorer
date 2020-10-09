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

properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '10', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

pipeline {

  agent {
    label 'jenkins-nuxeo-package-11'
  }

  triggers {
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: "/nuxeo/release-nuxeo-jsf-ui",
    )
  }

  parameters {
    string(name: 'PROMOTED_NUXEO_VERSION', defaultValue: '', description: 'Promoted version of the target Nuxeo Server Image.\nSample: \'11.3\'.\nWill be ignored if job is triggered from upstream build.')
    string(name: 'ORIGINAL_NUXEO_VERSION', defaultValue: '', description: 'Original version of the target Nuxeo Server Image.\nSample: \'11.3.48\'.\nWill be ignored if job is triggered from upstream build.')
    text(name: 'DEFAULT_PACKAGE_LIST', defaultValue: '', description: 'The list of packages to install for snapshot.\nSample: \'nuxeo-csv nuxeo-quota-1.0.0\'.\nWill override the default job triggered job value if not empty.')
    text(name: 'ADDITIONAL_PACKAGE_LIST', defaultValue: '', description: 'The additional list of packages to install for snapshot.\nWill override the default job triggered job value if not empty.')
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
            booleanParam(name: 'UPLOAD_TO_PROD', value: true)
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
            if (b.projectName != 'test-trigger' && b.fullProjectName != 'nuxeo/release-nuxeo' && b.fullProjectName != 'nuxeo/release-nuxeo-jsf-ui') {
              continue
            }
            hasUpstream = true
            // parse description which should look like "Release 11.2 from build 11.2.13"
            def matcher = (b.description =~ /Release\s(\d+\.\d+)\sfrom\sbuild\s(\d+\.\d+\.\d+).*/)
            if (matcher.matches()) {
              def promotedVersion = matcher[0][1]
              echo "Parsed promoted version: ${promotedVersion}"
              jobParams.add(string(name: 'NUXEO_VERSION', value: promotedVersion))
              def originalVersion = matcher[0][2]
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
