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

pipeline {
  agent {
    label 'jenkins-nuxeo-package-11'
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
  }
  stages {
    stage('Compile and test') {
      steps {
        setGitHubBuildStatus('explorer/compile', 'Compile and test', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh 'mvn -B -nsu install'
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, **/target/nuxeo-*-package-*.zip, **/target/**/*.log **/target/*.png, **/target/*.html'
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
