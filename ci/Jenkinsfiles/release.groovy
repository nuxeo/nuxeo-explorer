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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Anahide Tchertchian
 */
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void getCurrentVersion() {
  return readMavenPom().getVersion()
}

void getReleaseVersion(givenVersion, version) {
  if (givenVersion.isEmpty()) {
    return version.replace('-SNAPSHOT', '')
  }
  return givenVersion
}

void getNuxeoVersion(version) {
  container('maven') {
    if (version.isEmpty()) {
      return sh(returnStdout: true, script: 'mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=nuxeo.platform.version -q -DforceStdout').trim()
    }
    return version
  }
}

void getMavenReleaseOptions(Boolean skipTests, Boolean skipFunctionalTests) {
  def options = '-DskipDocker'
  if (skipTests) {
    return options + ' -DskipTests'
  }
  if (skipFunctionalTests) {
    return options + ' -DskipITs'
  }
  return options
}

pipeline {

  agent {
    label 'jenkins-nuxeo-package-10'
  }

  options {
    skipStagesAfterUnstable()
  }

  parameters {
    string(name: 'BRANCH_NAME', defaultValue: '18.0_10.10', description: 'The branch to release')
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release Explorer version (optional)')
    string(name: 'NEXT_VERSION', defaultValue: '', description: 'Next Explorer version (next minor version if unset)')
    string(name: 'NUXEO_VERSION', defaultValue: '', description: 'Version of the Nuxeo Server dependency (unchanged if unset)')
    string(name: 'NEXT_NUXEO_VERSION', defaultValue: '', description: 'Next Version of the Nuxeo Server dependency (unchanged if unset)')
    string(name: 'JIRA_ISSUE', defaultValue: '', description: 'Id of the Jira issue for this release')
    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip all tests')
    booleanParam(name: 'SKIP_FUNCTIONAL_TESTS', defaultValue: true, description: 'Skip functional tests (no FF on 10.10 pod anyway)')
    booleanParam(name: 'DRY_RUN', defaultValue: true, description: 'Dry run')
  }

  environment {
    CURRENT_VERSION = getCurrentVersion()
    RELEASE_VERSION = getReleaseVersion(params.RELEASE_VERSION, CURRENT_VERSION)
    MAVEN_ARGS = '-B -nsu -Prelease'
    MAVEN_RELEASE_OPTIONS = getMavenReleaseOptions(params.SKIP_TESTS, params.SKIP_FUNCTIONAL_TESTS)
    MAVEN_SKIP_ENFORCER = ' -Dnuxeo.skip.enforcer=true'
    VERSION = "${RELEASE_VERSION}"
    DRY_RUN = "${params.DRY_RUN}"
    BRANCH_NAME = "${params.BRANCH_NAME}"
    SLACK_CHANNEL = 'explorer-notifs'
  }

  stages {

    stage('Check Parameters') {
      steps {
        script {
          echo """
          ----------------------------------------
          Branch name:                '${BRANCH_NAME}'

          Current version:            '${CURRENT_VERSION}'
          Release version:            '${RELEASE_VERSION}'
          Next version:               '${params.NEXT_VERSION}'

          Nuxeo version:              '${params.NUXEO_VERSION}'
          Next Nuxeo version:         '${params.NEXT_NUXEO_VERSION}'

          Jira issue:                 '${params.JIRA_ISSUE}'

          Skip tests:                 '${params.SKIP_TESTS}'
          Skip functional tests:      '${params.SKIP_FUNCTIONAL_TESTS}'

          Dry run:                    '${params.DRY_RUN}'
          ----------------------------------------
          """
        }
      }
    }

    stage('Set Kubernetes labels') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Set Kubernetes labels
          ----------------------------------------
          """
          echo "Set label 'branch: ${BRANCH_NAME}' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=${BRANCH_NAME}
          """
        }
      }
    }

    stage('Update version') {
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Update version on branch ${BRANCH_NAME}
            ----------------------------------------
            New version: ${RELEASE_VERSION}
            """
            sh """
              git checkout ${BRANCH_NAME}
            """
            if (!params.NUXEO_VERSION.isEmpty()) {
              sh """
                # nuxeo version
                # only replace the first <version> occurrence
                perl -i -pe '!\$x && s|<version>.*?</version>|<version>${params.NUXEO_VERSION}</version>| && (\$x=1)' pom.xml
              """
            }

            sh """
              # explorer version
              mvn ${MAVEN_ARGS} ${MAVEN_SKIP_ENFORCER} versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false
              git diff
            """
          }
        }
      }
    }

    stage('Release') {
      steps {
        container('maven') {
          script {
            echo """
            -------------------------------------------------
            Release Explorer
            -------------------------------------------------
            """
            sh """
              mvn ${MAVEN_ARGS} ${MAVEN_RELEASE_OPTIONS} install
            """

            def message = "Release ${RELEASE_VERSION}"
            if (!params.JIRA_ISSUE.isEmpty()) {
              message = "${params.JIRA_ISSUE}: ${message}"
            }
            sh """
              git diff
              git commit -a -m "${message}"
              git tag -a v${RELEASE_VERSION} -m "${message}"
            """

            if (env.DRY_RUN != "true") {
              sh """
                jx step git credentials
                git config credential.helper store

                git push origin v${RELEASE_VERSION}
              """
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.jar, packages/**/target/nuxeo-*-package-*.zip, **/target/**/*.log, **/target/*.png, **/target/*.html'
          junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml, **/target/surefire-reports/*.xml'
          script {
            if (!params.SKIP_TESTS && !params.SKIP_FUNCTIONAL_TESTS) {
              findText regexp: ".*ERROR.*", fileSet: "ftests/**/log/server.log", unstableIfFound: true
            }
          }
        }
      }
    }

    stage('Deploy Maven Artifacts') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Maven Artifacts
          ----------------------------------------"""
          sh "mvn ${MAVEN_ARGS} ${MAVEN_SKIP_ENFORCER} -DskipTests -DskipDocker deploy"
        }
      }
    }

    stage('Bump branch version') {
      steps {
        container('maven') {
          script {
            sh "git checkout ${BRANCH_NAME}"
            // increment minor version
            def nextVersion = "${params.NEXT_VERSION}"
            if (nextVersion.isEmpty()) {
              nextVersion = sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/\$1+1/e' <<< ${CURRENT_VERSION}").trim()
            }
            echo """
            -----------------------------------------------
            Update ${BRANCH_NAME} version from ${CURRENT_VERSION} to ${nextVersion}
            -----------------------------------------------
            """
            def nextNuxeoVersion = "${params.NEXT_NUXEO_VERSION}"
            if (!nextNuxeoVersion.isEmpty()) {
              sh """
                # only replace the first <version> occurrence
                perl -i -pe '!\$x && s|<version>.*?</version>|<version>${nextNuxeoVersion}</version>| && (\$x=1)' pom.xml
              """
            }

            def message = "Post release ${RELEASE_VERSION}, update ${CURRENT_VERSION} to ${nextVersion}"
            if (!params.JIRA_ISSUE.isEmpty()) {
              message = "${params.JIRA_ISSUE}: ${message}"
            }
            sh """
              # explorer version
              mvn ${MAVEN_ARGS} ${MAVEN_SKIP_ENFORCER} versions:set -DnewVersion=${nextVersion} -DgenerateBackupPoms=false

              git diff
              git commit -a -m "${message}"
            """

            if (env.DRY_RUN != "true") {
              sh """
                jx step git credentials
                git config credential.helper store

                git push origin ${BRANCH_NAME}
              """
            }
          }
        }
      }
    }

  }

  post {
    success {
      script {
        def message = "Release ${RELEASE_VERSION}"
        if (env.DRY_RUN != "true") {
          currentBuild.description = "${message}"
          slackSend(channel: "${SLACK_CHANNEL}", color: "good", message: "Successfully released nuxeo-explorer ${RELEASE_VERSION} <${BUILD_URL}|#${BUILD_NUMBER}>")
        } else {
          currentBuild.description = "(Dry Run) ${message}"
        }
      }
    }
    unsuccessful {
      script {
        if (currentBuild.description?.isEmpty()) {
          currentBuild.description = "(Attempt) Release ${RELEASE_VERSION}"
        }
        if (env.DRY_RUN != "true") {
          slackSend(channel: "${SLACK_CHANNEL}", color: "danger", message: "Failed to release nuxeo-explorer ${RELEASE_VERSION} <${BUILD_URL}|#${BUILD_NUMBER}>")
        }
      }
    }
  }

}
