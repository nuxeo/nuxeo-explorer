/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo-explorer/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void setGitHubBuildStatus(String context, String message, String state) {
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/nuxeo/nuxeo-explorer'],
    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
    statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
  ])
}

String getVersion(referenceBranch) {
  String version = readMavenPom().getVersion()
  return BRANCH_NAME == referenceBranch ? version : version + "-${BRANCH_NAME}"
}

String getCommitSha1() {
  return sh(returnStdout: true, script: 'git rev-parse HEAD').trim();
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

void dockerDeploy(String imageName) {
  String imageTag = "nuxeo/${imageName}:${VERSION}"
  String internalImage = "${DOCKER_REGISTRY}/${imageTag}"
  String explorerImage = "${NUXEO_DOCKER_REGISTRY}/${imageTag}"
  echo "Push ${explorerImage}"
  dockerPull(internalImage)
  dockerTag(internalImage, explorerImage)
  dockerPush(explorerImage)
}

String getPreviewTemplatesOverride(isReferenceBranch) {
  if (isReferenceBranch) {
    // activate dedicated profiles on master preview
    return 'nuxeo.templates=default,mongodb,explorer-sitemode,explorer-virtualadmin'
  }
  // NXP-29494: override templates to avoid activating s3 in PR preview
  return 'nuxeo.templates=default'
}

pipeline {
  agent {
    label 'jenkins-nuxeo-package-11'
  }
  triggers {
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: "/nuxeo/nuxeo/${BRANCH_NAME}",
    )
  }
  environment {
    CONNECT_PROD_URL = "https://connect.nuxeo.com/nuxeo"
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    MAVEN_ARGS = '-B -nsu'
    REFERENCE_BRANCH = 'master'
    SCM_REF = "${getCommitSha1()}"
    VERSION = "${getVersion(REFERENCE_BRANCH)}"
    PERSISTENCE = "${BRANCH_NAME == REFERENCE_BRANCH}"
    NUXEO_TEMPLATE_OVERRIDE = "${getPreviewTemplatesOverride(BRANCH_NAME == REFERENCE_BRANCH)}"
    NUXEO_DOCKER_REGISTRY = 'docker-private.packages.nuxeo.com'
    PREVIEW_NAMESPACE = "nuxeo-explorer-${BRANCH_NAME.toLowerCase()}"
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
          echo "Set label 'branch: ${BRANCH_NAME}' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=${BRANCH_NAME}
          """
        }
      }
    }
    stage('Compile') {
      steps {
        setGitHubBuildStatus('explorer/compile', 'Compile', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh "mvn ${MAVEN_ARGS} -DskipTests -DskipDocker install"
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, **/target/nuxeo-*-package-*.zip'
        }
        success {
          setGitHubBuildStatus('explorer/compile', 'Compile', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/compile', 'Compile', 'FAILURE')
        }
      }
    }
    stage('Run Unit Tests') {
      steps {
        setGitHubBuildStatus('explorer/utests', 'Run Unit Tests', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run Unit Tests
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh "mvn  ${MAVEN_ARGS} -f modules test"
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, **/target/nuxeo-*-package-*.zip, **/target/**/*.log, **/target/*.png, **/target/*.html'
          junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
        success {
          setGitHubBuildStatus('explorer/utests', 'Run Unit Tests', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/utests', 'Run Unit Tests', 'FAILURE')
        }
      }
    }
    stage('Run Functional Tests') {
      steps {
        setGitHubBuildStatus('explorer/ftests', 'Run Functional Tests', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run Functional Tests
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh "mvn ${MAVEN_ARGS} -f ftests verify"
        }
        findText regexp: ".*ERROR.*", fileSet: "ftests/**/log/server.log", unstableIfFound: true
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/**/*.log, **/target/*.png, **/target/*.html'
          junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
        success {
          setGitHubBuildStatus('explorer/ftests', 'Run Functional Tests', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/ftests', 'Run Functional Tests', 'FAILURE')
        }
      }
    }
    stage('Deploy Nuxeo packages') {
      when {
        branch "${REFERENCE_BRANCH}"
      }
      steps {
        setGitHubBuildStatus('explorer/package/deploy', 'Deploy Nuxeo Packages', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Upload Nuxeo Packages to ${CONNECT_PROD_URL}
          ----------------------------------------"""
          withCredentials([usernameColonPassword(credentialsId: 'connect-prod', variable: 'CONNECT_PASS')]) {
            sh """
              PACKAGES_TO_UPLOAD="packages/nuxeo-*-package/target/nuxeo-*-package*.zip"
              for file in \$PACKAGES_TO_UPLOAD ; do
                curl --fail -i -u "$CONNECT_PASS" -F package=@\$(ls \$file) "$CONNECT_PROD_URL"/site/marketplace/upload?batch=true ;
              done
            """
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('explorer/package/deploy', 'Deploy Nuxeo Packages', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/package/deploy', 'Deploy Nuxeo Packages', 'FAILURE')
        }
      }
    }
    stage('Build Docker Images') {
      when {
        anyOf {
          branch 'PR-*'
          branch "${REFERENCE_BRANCH}"
        }
      }
      steps {
        setGitHubBuildStatus('explorer/docker/build', 'Build Docker Images', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Build Explorer Docker image
          ----------------------------------------
          Image tag: ${VERSION}
          Registry: ${DOCKER_REGISTRY}
          """
          script {
            def moduleDir = 'docker/nuxeo-explorer-docker'
            def nuxeoImageVersion = sh(returnStdout: true, script: 'mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=nuxeo.platform.version -q -DforceStdout').trim();
            if (nuxeoImageVersion.isEmpty()) {
                error("nuxeo image version is empty")
            }
            // push images to the Jenkins X internal Docker registry
            sh """
              NUXEO_IMAGE_VERSION=${nuxeoImageVersion} envsubst < ${moduleDir}/skaffold.yaml > ${moduleDir}/skaffold.yaml~gen
              skaffold build -f ${moduleDir}/skaffold.yaml~gen
              # waiting skaffold + kaniko + container-stucture-tests issue
              #  see https://github.com/GoogleContainerTools/skaffold/issues/3907
              docker pull ${DOCKER_REGISTRY}/nuxeo/nuxeo-explorer:${VERSION}
              container-structure-test test --image ${DOCKER_REGISTRY}/nuxeo/nuxeo-explorer:${VERSION} --config ${moduleDir}/test/*
            """
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('explorer/docker/build', 'Build Docker Images', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/docker/build', 'Build Docker Images', 'FAILURE')
        }
      }
    }
    stage('Deploy Docker Images') {
      when {
        branch "${REFERENCE_BRANCH}"
      }
      steps {
        setGitHubBuildStatus('explorer/docker/deploy', 'Deploy Docker Images', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Explorer Docker image
          ----------------------------------------
          Image tag: ${VERSION}
          Registry: ${DOCKER_REGISTRY}
          """
          dockerDeploy("nuxeo-explorer")
        }
      }
      post {
        success {
          setGitHubBuildStatus('explorer/docker/deploy', 'Deploy Docker Images', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/docker/deploy', 'Deploy Docker Images', 'FAILURE')
        }
      }
    }
    stage('Deploy Preview') {
      when {
        anyOf {
          branch "${REFERENCE_BRANCH}"
          allOf {
            branch 'PR-*'
            expression {
              return pullRequest.labels.contains('preview')
            }
          }
        }
      }
      steps {
        setGitHubBuildStatus('explorer/preview/deploy', 'Deploy Preview', 'PENDING')
        container('maven') {
          dir('helm/preview') {
            echo """
            ----------------------------------------
            Deploy Preview environment
            ----------------------------------------"""
            script {
              boolean isReferenceBranch = BRANCH_NAME == REFERENCE_BRANCH
              // first substitute docker image names and versions
              withCredentials([usernamePassword(credentialsId: 'explorer-preview', passwordVariable: 'EXPLORER_PASSWORD', usernameVariable: 'UNUSED_USERNAME')]) {
                def explorerPasswordProp = "org.nuxeo.apidoc.apidocAdmin.password=${EXPLORER_PASSWORD}"
                sh """
                  mv values.yaml values.yaml.tosubst
                  NUXEO_EXPLORER_CUSTOM_PARAMS=${isReferenceBranch ? explorerPasswordProp : ''} \
                  envsubst < values.yaml.tosubst > values.yaml
                """
              }
              // second create target namespace (if doesn't exist) and copy secrets to target namespace
              String currentNs = sh(returnStdout: true, script: 'jx -b ns | sed -r "s/^Using namespace \'([^\']+)\'.+\\$/\\1/"').trim()
              boolean nsExist = sh(returnStatus: true, script: "kubectl get namespace ${PREVIEW_NAMESPACE}") == 0
              // Only used with jx preview on pr branches
              String noCommentOpt = '';
              if (nsExist) {
                noCommentOpt = '--no-comment'
                // Previous preview deployment needs to be scaled to 0 to be replaced correctly
                sh "kubectl --namespace ${PREVIEW_NAMESPACE} scale deployment preview --replicas=0"
              } else {
                sh "kubectl create namespace ${PREVIEW_NAMESPACE}"
              }
              sh "kubectl --namespace platform get secret kubernetes-docker-cfg -ojsonpath='{.data.\\.dockerconfigjson}' | base64 --decode > /tmp/config.json"
              sh """kubectl create secret generic kubernetes-docker-cfg \
                  --namespace=${PREVIEW_NAMESPACE} \
                  --from-file=.dockerconfigjson=/tmp/config.json \
                  --type=kubernetes.io/dockerconfigjson --dry-run -o yaml | kubectl apply -f -"""
              String previewCommand = isReferenceBranch ?
                // To avoid jx gc cron job, reference branch previews are deployed by calling jx step helm install instead of jx preview
                "jx step helm install --namespace ${PREVIEW_NAMESPACE} --name ${PREVIEW_NAMESPACE} --verbose ."
                // When deploying a pr preview, we use jx preview which gc the merged pull requests
                : "jx preview --namespace ${PREVIEW_NAMESPACE} --verbose --source-url=https://github.com/nuxeo/nuxeo-explorer --preview-health-timeout 15m ${noCommentOpt}"

              // third build and deploy the chart
              // waiting for https://github.com/jenkins-x/jx/issues/5797 to be fixed in order to remove --source-url
              sh """
                jx step helm build --verbose
                mkdir target && helm template . --output-dir target
                ${previewCommand}
              """
              if (isReferenceBranch) {
                // When not using jx preview, we need to expose the nuxeo url by hand
                url = sh(returnStdout: true, script: "kubectl get svc --namespace ${PREVIEW_NAMESPACE} preview -o go-template='{{index .metadata.annotations \"fabric8.io/exposeUrl\"}}'")
                echo """
                  ----------------------------------------
                  Preview available at: ${url}
                  ----------------------------------------"""
              }
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/requirements.lock, **/charts/*.tgz, **/target/**/*.yaml'
        }
        success {
          setGitHubBuildStatus('explorer/preview/deploy', 'Deploy Preview', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('explorer/preview/deploy', 'Deploy Preview', 'FAILURE')
        }
      }
    }
  }
  post {
    always {
      script {
        if (BRANCH_NAME == REFERENCE_BRANCH) {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
  }
}
