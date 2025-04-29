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
library identifier: "platform-ci-shared-library@v0.0.55"

String getCLIDSecret() {
  container('maven') {
    def nuxeoParentVersion = readMavenPom().getParent().getVersion()
    // target connect preprod if nuxeo-parent is a snapshot version or a build version
    return nuxeoParentVersion.matches("^\\d+\\.\\d+(-SNAPSHOT|\\.\\d+)\$") ? 'instance-clid-preprod' : 'instance-clid'
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-package-lts-2025'
  }
  options {
    buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5'))
    disableConcurrentBuilds(abortPrevious: true)
    githubProjectProperty(projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer')
  }
  environment {
    CONNECT_CLID_SECRET = getCLIDSecret()
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    MAVEN_ARGS = '-B -nsu'
    VERSION = nxUtils.getVersion()
    NUXEO_EXPLORER_PACKAGE_PATH = "packages/nuxeo-platform-explorer-package/target/nuxeo-platform-explorer-package-${VERSION}.zip"
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
    stage('Build') {
      parallel {
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
        stage('Formatting check') {
          when {
            // if current version is higher than default branch (aka: version in maintenance) run formatting check
            expression { nxGitHub.getReferenceBranch().compareToIgnoreCase(nxGitHub.getDefaultBranch()) > 0 }
          }
          steps {
            container('maven') {
              warnError(message: 'Formatting check has failed') {
                nxWithGitHubStatus(context: 'explorer/lint', message: 'Lint') {
                  script {
                    echo """
                     ----------------------------------------
                     Check formatting
                     ----------------------------------------"""
                    sh "git fetch origin 2025:origin/2025"
                    sh "mvn -B -nsu -V -Dcustom.environment=spotless spotless:check"
                  }
                }
              }
            }
          }
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
              withCredentials([string(credentialsId: env.CONNECT_CLID_SECRET, variable: 'INSTANCE_CLID')]) {
                sh(script: '''#!/bin/bash +x
                  echo -e "$INSTANCE_CLID" >| /tmp/instance.clid
                ''')
                withEnv(["TEST_CLID_PATH=/tmp/instance.clid"]) {
                  echo "MAVEN_OPTS=$MAVEN_OPTS"
                  sh "mvn ${MAVEN_ARGS} -f ftests verify"
                  nxUtils.lookupText(regexp: ".*ERROR.*(?=(?:\\n.*)*\\[.*FrameworkLoader\\] Nuxeo Platform is Trying to Shut Down)",
                      fileSet: "ftests/**/log/server.log", unstableIfFound: true)
                }
              }
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
    stage('Build Docker image') {
      when {
        // only needed for preview
        expression { nxUtils.isPullRequest() && pullRequest.labels.contains('preview') }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'docker/build') {
            script {
              sh "mkdir -p ci/docker/target && cp ${NUXEO_EXPLORER_PACKAGE_PATH} ci/docker/target"
              def nuxeoVersion = sh(returnStdout: true,
                  script: 'mvn org.apache.maven.plugins:maven-help-plugin:3.3.0:evaluate -Dexpression=nuxeo.platform.version -q -DforceStdout')
              nxDocker.build(skaffoldFile: 'ci/docker/skaffold.yaml', envVars: ["NUXEO_VERSION=${nuxeoVersion}"])
            }
          }
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
    stage('Deploy Nuxeo package') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'explorer/package/deploy') {
            echo """
            ----------------------------------------
            Upload Nuxeo Package to ${CONNECT_PREPROD_SITE_URL}
            ----------------------------------------"""
            script {
              nxUtils.postForm(credentialsId: 'connect-preprod', url: "${CONNECT_PREPROD_SITE_URL}marketplace/upload?batch=true",
                  form: ["package=@${NUXEO_EXPLORER_PACKAGE_PATH}"])
            }
          }
        }
      }
    }
    stage('Deploy Preview') {
      when {
        expression { nxUtils.isPullRequest() && pullRequest.labels.contains('preview') }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'preview', message: 'Deploy preview') {
            script {
              echo """
              ----------------------------------------
              Deploy preview environment
              ----------------------------------------"""
              // Kubernetes namespace, requires lower case alphanumeric characters
              def previewNamespace = "${CURRENT_NAMESPACE}-explorer-${BRANCH_NAME}-preview".replaceAll('\\.', '-').toLowerCase()
              nxHelmfile.template(namespace: previewNamespace, environment: 'preview', outputDir: 'target')
              nxHelmfile.deploy(namespace: previewNamespace, environment: "preview",
                  secrets: [[name: env.CONNECT_CLID_SECRET, namespace: 'platform'], [name: 'platform-cluster-tls', namespace: 'platform']])
              def host = sh(returnStdout: true, script: """
                kubectl get ingress nuxeo \
                  --namespace=${previewNamespace} \
                  -ojsonpath='{.spec.rules[*].host}'
              """)
              def previewURL = "https://${host}"
              echo """
              -----------------------------------------------
              Preview available at: ${previewURL}
              -----------------------------------------------"""
              nxGitHub.commentPullRequest(
                  branch: CHANGE_BRANCH,
                  body: ":star: PR built and available in a preview environment **${previewNamespace}** [here](${previewURL})"
              )
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/**/*.yaml'
        }
      }
    }
  }

  post {
    always {
      script {
        nxUtils.setBuildDescription()
        nxJira.updateIssues()
        nxUtils.notifyBuildStatusIfNecessary()
      }
    }
  }
}
