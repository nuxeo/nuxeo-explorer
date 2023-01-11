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
* Script mimicking upstream job to test triggers.
*
* This script will trigger jobs using pipelines at trigger-export-on-build.groovy and
* trigger-export-on-promotion.groovy with expected description.
* Expected related job names, in same directory:
* - test-trigger for this job name
* - trigger-export-on-build for downstream build export trigger
* - trigger-export-on-promotion for downstream promotion export trigger
*/

pipeline {

  agent {
    label 'jenkins-nuxeo-jsf-lts-2021'
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

    stage('Test Trigger Export') {
      steps {
        script {
          if (params.BUILD_DESCRIPTION.isEmpty()) {
            currentBuild.result = 'ABORTED';
            def message = 'BUILD_DESCRIPTION parameter is mandatory.'
            currentBuild.description = "${message}"
            echo "Aborting with message: ${message}"
            error(currentBuild.description)
          }
          currentBuild.description = "${params.BUILD_DESCRIPTION}"
          if (params.IS_PROMOTION) {
            build job: 'trigger-export-on-promotion', wait: false
          } else {
            build job: 'trigger-export-on-build', wait: false
          }
        }
      }
    }

  }

}
