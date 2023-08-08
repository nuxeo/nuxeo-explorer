/*
 * (C) Copyright 2018-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Anahide Tchertchian
 */
library identifier: "platform-ci-shared-library@v0.0.26"

pipeline {
  agent {
    label 'jenkins-nuxeo-jsf-lts-2021'
  }
  options {
    buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5'))
    disableConcurrentBuilds(abortPrevious: true)
    githubProjectProperty(projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer')
  }
  environment {
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    MAVEN_ARGS = '-B -nsu'
    VERSION = nxUtils.getVersion()
  }
  stages {
    stage('Set Labels') {
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabels()
          }
        }
      }
    }
    stage('Update version') {
      steps {
        container('maven') {
          script {
            nxMvn.updateVersion()
          }
        }
      }
    }
    stage('Compile') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'explorer/compile') {
            echo """
            ----------------------------------------
            Compile
            ----------------------------------------"""
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn ${MAVEN_ARGS} -DskipTests -DskipDocker install"
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, **/target/nuxeo-*-package-*.zip'
        }
      }
    }
    stage('Run Unit Tests') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'explorer/utests') {
            echo """
            ----------------------------------------
            Run Unit Tests
            ----------------------------------------"""
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn  ${MAVEN_ARGS} -f modules test"
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, **/target/nuxeo-*-package-*.zip, **/target/**/*.log, **/target/*.png, **/target/*.html'
          junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
      }
    }
    stage('Run Functional Tests') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'explorer/ftests') {
            script {
              echo """
              ----------------------------------------
              Run Functional Tests
              ----------------------------------------"""
              echo "MAVEN_OPTS=$MAVEN_OPTS"
              sh "mvn ${MAVEN_ARGS} -f ftests verify"
              nxUtils.lookupText(regexp: ".*ERROR.*(?=(?:\\n.*)*\\[.*FrameworkLoader\\] Nuxeo Platform is Trying to Shut Down)",
                  fileSet: "ftests/**/log/server.log", unstableIfFound: true)
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/**/*.log, **/target/*.png, **/target/*.html'
          junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
      }
    }
    stage('Git commit, tag and push') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Git commit, tag and push
            ----------------------------------------
            """
            nxGit.commitTagPush()
          }
        }
      }
    }
    stage('Deploy Maven artifacts') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'explorer/maven/deploy', message: 'Deploy Maven artifacts') {
            script {
              echo """
              ----------------------------------------
              Deploy Maven artifacts
              ----------------------------------------"""
              nxMvn.deploy()
            }
          }
        }
      }
    }
    stage('Deploy Nuxeo packages') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'explorer/package/deploy') {
            echo """
            ----------------------------------------
            Upload Nuxeo Packages to ${CONNECT_PREPROD_SITE_URL}
            ----------------------------------------"""
            script {
              def nxPackages = findFiles(glob: 'packages/nuxeo-*-package/target/nuxeo-*-package*.zip')
              for (nxPackage in nxPackages) {
                nxUtils.postForm(credentialsId: 'connect-preprod', url: "${CONNECT_PREPROD_SITE_URL}marketplace/upload?batch=true",
                    form: ["package=@${nxPackage.path}"])
              }
            }
          }
        }
      }
    }
  }

  post {
    always {
      script {
        currentBuild.description = "Build ${VERSION}"
        nxJira.updateIssues()
      }
    }
    success {
      script {
        if (!nxUtils.isPullRequest()
            && !hudson.model.Result.SUCCESS.toString().equals(currentBuild.getPreviousBuild()?.getResult())) {
          nxSlack.success(message: "Successfully built <${BUILD_URL}|nuxeo-explorer ${BRANCH_NAME} #${BUILD_NUMBER}>")
        }
      }
    }
    failure { // use failure instead of "unsuccessful" because of frequent UNSTABLE status on ftests
      script {
        if (!nxUtils.isPullRequest()
            && ![hudson.model.Result.ABORTED.toString(), hudson.model.Result.NOT_BUILT.toString()].contains(currentBuild.result)) {
          nxSlack.error(message: "Failed to build <${BUILD_URL}|nuxeo-explorer ${BRANCH_NAME} #${BUILD_NUMBER}>")
        }
      }
    }
  }
}
