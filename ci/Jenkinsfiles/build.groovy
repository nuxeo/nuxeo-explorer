/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
  String imageTag = "${ORG}/${imageName}:${VERSION}"
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
  triggers {
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: '/nuxeo/nuxeo/${env.BRANCH_NAME}',
    )
  }
  environment {
    APP_NAME = 'nuxeo-explorer'
    // waiting for https://github.com/jenkins-x/jx/issues/4076 to put it in Global EnvVars
    CONNECT_PROD_UPLOAD = "https://connect.nuxeo.com/nuxeo/site/marketplace/upload?batch=true"
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    REFERENCE_BRANCH = "master"
    SCM_REF = "${getCommitSha1()}"
    VERSION = "${getVersion(REFERENCE_BRANCH)}"
    PERSISTENCE = "${BRANCH_NAME == REFERENCE_BRANCH}"
    NUXEO_DOCKER_REGISTRY = "docker-private.packages.nuxeo.com"
    PREVIEW_NAMESPACE = "$APP_NAME-${BRANCH_NAME.toLowerCase()}"
    ORG = "nuxeo"
  }
  stages {
    stage('Set labels') {
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
    stage('Compile and test') {
      steps {
        setGitHubBuildStatus('explorer/compile', 'Compile and test', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh 'mvn -B -nsu -DskipDocker install'
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, **/target/nuxeo-*-package-*.zip, **/target/**/*.log, **/target/*.png, **/target/*.html'
          junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
        success {
          setGitHubBuildStatus('explorer/compile', 'Compile and test', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('explorer/compile', 'Compile and test', 'FAILURE')
        }
      }
    }
    stage('Deploy Nuxeo packages') {
      when {
        branch "${REFERENCE_BRANCH}"
      }
      steps {
        setGitHubBuildStatus('explorer/package/deploy', 'Deploy Nuxeo packages', 'PENDING')
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
      post {
        success {
          setGitHubBuildStatus('explorer/package/deploy', 'Deploy Nuxeo packages', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('explorer/package/deploy', 'Deploy Nuxeo packages', 'FAILURE')
        }
      }
    }
    stage('Build Docker images') {
      when {
        anyOf {
          branch 'PR-*'
          branch "${REFERENCE_BRANCH}"
        }
      }
      steps {
        setGitHubBuildStatus('explorer/docker/build', 'Build and deploy Docker images', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Build Explorer Docker image
          ----------------------------------------
          Image tag: ${VERSION}
          Registry: ${DOCKER_REGISTRY}
          """
          script {
            def moduleDir="docker/nuxeo-explorer-docker"
            // push images to the Jenkins X internal Docker registry
            sh """
              envsubst < ${moduleDir}/skaffold.yaml > ${moduleDir}/skaffold.yaml~gen
              skaffold build -f ${moduleDir}/skaffold.yaml~gen
              # waiting skaffold + kaniko + container-stucture-tests issue
              #  see https://github.com/GoogleContainerTools/skaffold/issues/3907
              docker pull ${DOCKER_REGISTRY}/${ORG}/nuxeo-explorer:${VERSION}
              container-structure-test test --image ${DOCKER_REGISTRY}/${ORG}/nuxeo-explorer:${VERSION} --config ${moduleDir}/test/*
            """
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('explorer/docker/build', 'Build and deploy Docker images', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('explorer/docker/build', 'Build and deploy Docker images', 'FAILURE')
        }
      }
    }
    stage('Deploy Docker images') {
      when {
        branch "${REFERENCE_BRANCH}"
      }
      steps {
        setGitHubBuildStatus('explorer/docker/deploy', 'Deploy Docker images', 'PENDING')
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
          setGitHubBuildStatus('explorer/docker/deploy', 'Deploy Docker images', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('explorer/docker/deploy', 'Deploy Docker images', 'FAILURE')
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
        setGitHubBuildStatus('explorer/preview/deploy', 'Deploy Explorer Preview', 'PENDING')
        container('maven') {
          dir('helm/preview') {
            echo """
            ----------------------------------------
            Deploy Preview environment
            ----------------------------------------"""
            // first substitute docker image names and versions
            sh """
              mv values.yaml values.yaml.tosubst
              envsubst < values.yaml.tosubst > values.yaml
            """
            // second create target namespace (if doesn't exist) and copy secrets to target namespace
            script {
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
              boolean isReferenceBranch = BRANCH_NAME == REFERENCE_BRANCH
              String previewCommand = isReferenceBranch ?
                // To avoid jx gc cron job, reference branch previews are deployed by calling jx step helm install instead of jx preview
                "jx step helm install  --namespace ${PREVIEW_NAMESPACE} --name ${PREVIEW_NAMESPACE} --verbose ."
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
          setGitHubBuildStatus('explorer/preview/deploy', 'Deploy Explorer Preview', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('explorer/preview/deploy', 'Deploy Explorer Preview', 'FAILURE')
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
