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
  if (version.isEmpty()) {
    return readMavenPom().getProperties().getProperty('nuxeo.image.version')
  }
  return version
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

void dockerPull(String image) {
  sh "docker pull ${image}"
}

void dockerTag(String image, String tag) {
  sh "docker tag ${image} ${tag}"
}

void dockerPush(String image) {
  sh "docker push ${image}"
}

void dockerDeploy(String imageName, String releaseVersion) {
  String imageTag = "${ORG}/${imageName}:${releaseVersion}"
  String internalImage = "${DOCKER_REGISTRY}/${imageTag}"
  String explorerImage = "${NUXEO_DOCKER_REGISTRY}/${imageTag}"
  echo "Push ${explorerImage}"
  dockerPull(internalImage)
  dockerTag(internalImage, explorerImage)
  dockerPush(explorerImage)
}

pipeline {

  agent {
    label 'jenkins-nuxeo-package-11'
  }

  options {
    skipStagesAfterUnstable()
  }

  parameters {
    string(name: 'BRANCH_NAME', defaultValue: 'master', description: 'The branch to release')
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release Explorer version (optional)')
    string(name: 'NEXT_VERSION', defaultValue: '', description: 'Next Explorer version (next minor version if unset)')
    string(name: 'NUXEO_VERSION', defaultValue: '', description: 'Version of the Nuxeo Server dependency (unchanged if unset)')
    booleanParam(name: 'NUXEO_VERSION_IS_PROMOTED', defaultValue: true, description: 'Uncheck if releasing a RC version, against a non-promoted Nuxeo build')
    string(name: 'NEXT_NUXEO_VERSION', defaultValue: '', description: 'Next Version of the Nuxeo Server dependency (unchanged if unset)')
    string(name: 'NEXT_NUXEO_IMAGE_VERSION', defaultValue: '', description: 'Next Version of the Nuxeo Server Image dependency (unchanged if unset)')
    string(name: 'JIRA_ISSUE', defaultValue: '', description: 'Id of the Jira issue for this release')
    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip all tests')
    booleanParam(name: 'SKIP_FUNCTIONAL_TESTS', defaultValue: false, description: 'Skip functional tests')
    booleanParam(name: 'DRY_RUN', defaultValue: true, description: 'Dry run')
  }

  environment {
    CURRENT_VERSION = getCurrentVersion()
    RELEASE_VERSION = getReleaseVersion(params.RELEASE_VERSION, CURRENT_VERSION)
    NUXEO_IMAGE_VERSION = getNuxeoVersion(params.NUXEO_VERSION)
    MAVEN_ARGS = '-B -nsu -Prelease'
    MAVEN_RELEASE_OPTIONS = getMavenReleaseOptions(params.SKIP_TESTS, params.SKIP_FUNCTIONAL_TESTS)
    MAVEN_SKIP_ENFORCER = ' -Dnuxeo.skip.enforcer=true'
    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo'
    NUXEO_DOCKER_REGISTRY = 'docker-private.packages.nuxeo.com'
    ORG = 'nuxeo'
    VERSION = "${RELEASE_VERSION}"
    NUXEO_IMAGE_NAME = 'nuxeo'
    DRY_RUN = "${params.DRY_RUN}"
    BRANCH_NAME = "${params.BRANCH_NAME}"
  }

  stages {

    stage('Check Parameters') {
      steps {
        script {
          if (!params.NUXEO_VERSION_IS_PROMOTED && !RELEASE_VERSION.contains('RC')) {
            currentBuild.result = 'ABORTED';
            currentBuild.description = "Can only release a RC against a non-promoted Nuxeo version"
            error(currentBuild.description)
          }
          echo """
          ----------------------------------------
          Branch name:                '${BRANCH_NAME}'

          Current version:            '${CURRENT_VERSION}'
          Release version:            '${RELEASE_VERSION}'
          Next version:               '${params.NEXT_VERSION}'

          Nuxeo version:              '${params.NUXEO_VERSION}'
          Next Nuxeo version:         '${params.NEXT_NUXEO_VERSION}'
          Next Nuxeo image version:   '${params.NEXT_NUXEO_IMAGE_VERSION}'

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
            if (!params.NUXEO_VERSION_IS_PROMOTED) {
              sh """
                # hack: use nuxeo-ecm instead of nuxeo-parent to retrieve a non-promoted nuxeo version
                perl -i -pe 's|<artifactId>nuxeo-parent</artifactId>|<artifactId>nuxeo-ecm</artifactId>|' pom.xml
              """
            }
            if (!params.NUXEO_VERSION.isEmpty()) {
              sh """
                # nuxeo version
                # only replace the first <version> occurrence
                perl -i -pe '!\$x && s|<version>.*?</version>|<version>${NUXEO_VERSION}</version>| && (\$x=1)' pom.xml

                perl -i -pe 's|<nuxeo.image.version>.*?</nuxeo.image.version>|<nuxeo.image.version>${NUXEO_VERSION}</nuxeo.image.version>|' pom.xml
              """
            }

            sh """
              # explorer version
              mvn ${MAVEN_ARGS} ${MAVEN_SKIP_ENFORCER} versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false
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

            echo """
            ----------------------------------------
            Build Explorer Docker image
            ----------------------------------------
            Image tag: ${RELEASE_VERSION}
            Registry: ${DOCKER_REGISTRY}
            Nuxeo Image tag: ${NUXEO_VERSION}
            """
            def moduleDir="docker/nuxeo-explorer-docker"
            // push images to the Jenkins X internal Docker registry
            sh """
              envsubst < ${moduleDir}/skaffold.yaml > ${moduleDir}/skaffold.yaml~gen
              skaffold build -f ${moduleDir}/skaffold.yaml~gen
              # waiting skaffold + kaniko + container-stucture-tests issue
              #  see https://github.com/GoogleContainerTools/skaffold/issues/3907
              docker pull ${DOCKER_REGISTRY}/${ORG}/nuxeo-explorer:${RELEASE_VERSION}
              container-structure-test test --image ${DOCKER_REGISTRY}/${ORG}/nuxeo-explorer:${RELEASE_VERSION} --config ${moduleDir}/test/*
            """

            def message = "Release ${RELEASE_VERSION}"
            if (!params.JIRA_ISSUE.isEmpty()) {
              message = "${params.JIRA_ISSUE}: ${message}"
            }
            sh """
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
          junit testResults: allowEmptyResults: true, '**/target/failsafe-reports/*.xml, **/target/surefire-reports/*.xml'
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

    stage('Upload Nuxeo Packages') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Nuxeo packages
          ----------------------------------------"""
          withCredentials([usernameColonPassword(credentialsId: 'connect-prod', variable: 'CONNECT_PASS')]) {
            sh """
              PACKAGES_TO_UPLOAD="packages/nuxeo-*-package/target/nuxeo-*-package*.zip"
              for file in \$PACKAGES_TO_UPLOAD ; do
                curl -i -u "$CONNECT_PASS" -F package=@\$(ls \$file) "$CONNECT_PROD_UPLOAD" ;
              done
            """
          }
        }
      }
    }

    stage('Promote Docker images') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          echo """
          -----------------------------------------------
          Tag Docker images with version ${RELEASE_VERSION}
          -----------------------------------------------
          """
          dockerDeploy("nuxeo-explorer", "${RELEASE_VERSION}")
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
            if (!params.NUXEO_VERSION_IS_PROMOTED) {
              sh """
                # hack: replace back nuxeo-ecm replacement
                perl -i -pe 's|<artifactId>nuxeo-ecm</artifactId>|<artifactId>nuxeo-parent</artifactId>|' pom.xml
              """
            }
            def nextNuxeoVersion = "${params.NEXT_NUXEO_VERSION}"
            if (!nextNuxeoVersion.isEmpty()) {
              sh """
                # only replace the first <version> occurrence
                perl -i -pe '!\$x && s|<version>.*?</version>|<version>${nextNuxeoVersion}</version>| && (\$x=1)' pom.xml
              """
            }
            def nextNuxeoImageVersion = "${params.NEXT_NUXEO_IMAGE_VERSION}"
            if (!nextNuxeoImageVersion.isEmpty()) {
              sh """
                perl -i -pe 's|<nuxeo.image.version>.*?</nuxeo.image.version>|<nuxeo.image.version>${nextNuxeoImageVersion}</nuxeo.image.version>|' pom.xml
              """
            }

            def message = "Post release ${RELEASE_VERSION}, update ${CURRENT_VERSION} to ${nextVersion}"
            if (!params.JIRA_ISSUE.isEmpty()) {
              message = "${params.JIRA_ISSUE}: ${message}"
            }
            sh """
              # explorer version
              mvn ${MAVEN_ARGS} ${MAVEN_SKIP_ENFORCER} versions:set -DnewVersion=${nextVersion} -DgenerateBackupPoms=false

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
        if (env.DRY_RUN != "true") {
          currentBuild.description = "Release ${RELEASE_VERSION}"
        }
      }
    }
  }
}
