/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class JenkinsFileTest implements RewriteTest {

    @Test
    void jenkinsfile() {
        // the Jenkinsfile from spring-projects/spring-data-release
        rewriteRun(
          groovy(
            """
              def p = [:]
              node {
                checkout scm
                p = readProperties interpolate: true, file: 'ci/release.properties'
              }
              pipeline {
                agent none
                triggers {
                  pollSCM 'H/10 * * * *'
                }
                options {
                  disableConcurrentBuilds()
                  buildDiscarder(logRotator(numToKeepStr: '14'))
                }
                stages {
                  stage('Build the Spring Data release tools container') {
                    when {
                      anyOf {
                         changeset 'ci/Dockerfile'
                         changeset 'ci/java-tools.properties'
                      }
                    }
                    agent {
                      label 'data'
                    }
                    steps {
                      script {
                        def image = docker.build("springci/spring-data-release-tools:0.12", "ci")
                        docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
                          image.push()
                        }
                      }
                    }
                  }
                  stage('Ship It') {
                    when {
                      branch 'release'
                    }
                    agent {
                      docker {
                        image 'springci/spring-data-release-tools:0.12'
                      }
                    }
                    options { timeout(time: 4, unit: 'HOURS') }
                    environment {
                      GITHUB = credentials('3a20bcaa-d8ad-48e3-901d-9fbc941376ee')
                      GITHUB_TOKEN = credentials('7b3ebbea-7001-479b-8578-b8c464dab973')
                      REPO_SPRING_IO = credentials('repo_spring_io-jenkins-release-token')
                      ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
                      STAGING_PROFILE_ID = credentials('spring-data-release-deployment-maven-central-staging-profile-id')
                      MAVEN_SIGNING_KEY = credentials('spring-gpg-private-key')
                      MAVEN_SIGNING_KEY_PASSWORD = credentials('spring-gpg-passphrase')
                      GIT_SIGNING_KEY = credentials('spring-gpg-github-private-key-jenkins')
                      GIT_SIGNING_KEY_PASSWORD = credentials('spring-gpg-github-passphrase-jenkins')
                      SONATYPE = credentials('oss-login')
                    }
                    steps {
                      script {
                        sh "ci/build-spring-data-release-cli.bash"
                        sh "ci/build-and-distribute.bash ${p['release.version']}"
                        slackSend(
                          color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
                          channel: '#spring-data-dev',
                          message: (currentBuild.currentResult == 'SUCCESS')
                              ? "`${env.BUILD_URL}` - Build and distribute ${p['release.version']} passed! Release the build (if needed)."
                              : "`${env.BUILD_URL}` - Build and distribute ${p['release.version']} failed!")
                      }
                    }
                  }
                }
                post {
                  changed {
                    script {
                      slackSend(
                          color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
                          channel: '#spring-data-dev',
                          message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\\n${env.BUILD_URL}")
                      emailext(
                          subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
                          mimeType: 'text/html',
                          recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                          body: "<a href=\\"${env.BUILD_URL}\\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
                    }
                  }
                }
              }
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void jenkinsfileWithDestructuringAssignment() {
        rewriteRun(
          groovy(
            """
              pipeline {
                agent any
                stages {
                  stage('Build') {
                    steps {
                      script {
                        def tag = '1.2.3'
                        def (major, minor, patch) = tag.tokenize('.')
                        echo "Major: ${major}, Minor: ${minor}, Patch: ${patch}"
                      }
                    }
                  }
                }
              }
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/4887")
    @Test
    void jenkinsfileWithComment() {
        // the Jenkinsfile adapted from https://github.com/jenkinsci/ssh-plugin/blob/158.ve2a_e90fb_7319/Jenkinsfile
        rewriteRun(
          groovy(
            """
              /* https://github.com */
              foo()
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }
}
