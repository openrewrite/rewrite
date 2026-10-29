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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.marker.JenkinsSharedLibraryStep;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class JenkinsFileTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/6243")
    @Test
    void annotatedScriptFieldsWithListInitializer() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field

              @Field def foo

              // list of pull requests created
              @Field List<String> newPullRequests = []

              pipeline {
                  stages {
                      stage('Setup') {
                          steps {
                              script {
                                  // empty
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

    @Issue("https://github.com/openrewrite/rewrite/issues/7229")
    @Test
    void sharedLibraryStepHasProvenanceWithoutInventedSignature() {
        G.CompilationUnit cu = parse(null,
          input("Jenkinsfile", "def value = deploy('prod')"),
          input("vars/deploy.groovy", "def call(String target) { target }"));
        J.VariableDeclarations declaration = (J.VariableDeclarations) cu.getStatements().get(0);
        J.MethodInvocation call = (J.MethodInvocation) declaration.getVariables().get(0).getInitializer();
        assertThat(call.getMethodType()).isNull();
        assertThat(call.getMarkers().findFirst(JenkinsSharedLibraryStep.class)).hasValueSatisfying(marker ->
          assertThat(marker.getSourcePath()).isEqualTo("vars/deploy.groovy"));
    }

    @Test
    void libraryAnnotationDoesNotAttributeBuiltinsOrUnknownCalls() {
        G.CompilationUnit cu = parse(null, input("Jenkinsfile",
          "@Library(['my-lib@1.2.0', 'other-lib@feature/test']) _\nnode { sh 'ls'; echo 'hi'; typo() }"));
        new GroovyIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                assertThat(method.getMethodType()).isNull();
                assertThat(method.getMarkers().findFirst(JenkinsSharedLibraryStep.class)).isEmpty();
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0);
    }

    @Test
    void unrelatedLibraryAnnotationDoesNotAttributeCalls() {
        G.CompilationUnit cu = parse(null, input("other.groovy",
          "@interface Library { String value() }\n@Library('other') class Example { def run() { deploy() } }"));
        assertNoSharedLibrarySteps(cu);
    }

    @Test
    void nestedVarsPackageIsNotASharedLibrary() {
        G.CompilationUnit cu = parse(null,
          input("Jenkinsfile", "format()"),
          input("src/main/groovy/com/acme/vars/format.groovy", "def call() {}"));
        assertNoSharedLibrarySteps(cu);
    }

    @Test
    void nestedVarsSourcesAreNotSteps() {
        G.CompilationUnit cu = parse(null,
          input("Jenkinsfile", "deploy()"),
          input("vars/nested/deploy.groovy", "def call() {}"));
        assertNoSharedLibrarySteps(cu);
    }

    @Test
    void resolvedLocalMethodKeepsItsType() {
        G.CompilationUnit cu = parse(null,
          input("Jenkinsfile", "def deploy() { 1 }\ndeploy()"),
          input("vars/deploy.groovy", "def call() {}"));
        J.MethodInvocation call = (J.MethodInvocation) cu.getStatements().get(1);
        assertThat(call.getMethodType()).isNotNull();
        assertThat(call.getMarkers().findFirst(JenkinsSharedLibraryStep.class)).isEmpty();
    }

    @Test
    void ordinaryGroovySourcesDoNotConsumeSharedLibrarySteps() {
        for (String path : Arrays.asList("build.gradle", "other.groovy")) {
            G.CompilationUnit cu = parse(null, input(path, "deploy()"),
              input("vars/deploy.groovy", "def call() {}"));
            assertNoSharedLibrarySteps(cu);
        }
    }

    @Test
    void absolutePathsAreRelativeToLibraryRoot() {
        Path root = Paths.get("checkout/job-1234").toAbsolutePath();
        G.CompilationUnit cu = parse(root,
          Parser.Input.fromString(root.resolve("Jenkinsfile"), "deploy()"),
          Parser.Input.fromString(root.resolve("vars/deploy.groovy"), "def call() {}"));
        J.MethodInvocation call = (J.MethodInvocation) cu.getStatements().get(0);
        assertThat(call.getMarkers().findFirst(JenkinsSharedLibraryStep.class)).hasValueSatisfying(marker ->
          assertThat(marker.getSourcePath()).isEqualTo("vars/deploy.groovy"));
    }

    @Test
    void varsSourcesCanCallOtherSharedLibrarySteps() {
        G.CompilationUnit cu = parse(null, input("vars/first.groovy", "deploy()"),
          input("vars/deploy.groovy", "def call() {}"));
        assertThat(((J.MethodInvocation) cu.getStatements().get(0)).getMarkers()
          .findFirst(JenkinsSharedLibraryStep.class)).isPresent();
    }

    @Test
    void reusedContextAndInterleavedStreamsDoNotLeakSteps() {
        GroovyParser parser = GroovyParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        List<Parser.Input> first = Arrays.asList(input("Jenkinsfile", "deploy()"),
          input("vars/deploy.groovy", "def call() {}"));
        try (Stream<SourceFile> firstBatch = parser.parseInputs(first, null, ctx);
             Stream<SourceFile> secondBatch = parser.parseInputs(
               Arrays.asList(input("Jenkinsfile", "deploy()")), null, ctx)) {
            assertNoSharedLibrarySteps((G.CompilationUnit) secondBatch.findFirst().orElseThrow());
            assertThat(((J.MethodInvocation) ((G.CompilationUnit) firstBatch.findFirst().orElseThrow())
              .getStatements().get(0)).getMarkers().findFirst(JenkinsSharedLibraryStep.class)).isPresent();
        }
    }

    @Test
    void builtinAndExplicitReceiverCallsDoNotReceiveProvenance() {
        G.CompilationUnit cu = parse(null,
          input("Jenkinsfile", "node { sh 'ls'; echo 'hi'; helper.deploy() }"),
          input("vars/deploy.groovy", "def call() {}"));
        assertNoSharedLibrarySteps(cu);
    }

    @Test
    void separateBatchesDoNotGuessAnExternalStep() {
        GroovyParser parser = GroovyParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        try (Stream<SourceFile> library = parser.parseInputs(
          Arrays.asList(input("vars/deploy.groovy", "def call() {}")), null, ctx)) {
            assertThat(library.collect(toList())).allMatch(G.CompilationUnit.class::isInstance);
        }
        try (Stream<SourceFile> jenkinsfile = parser.parseInputs(
          Arrays.asList(input("Jenkinsfile", "deploy()")), null, ctx)) {
            assertNoSharedLibrarySteps((G.CompilationUnit) jenkinsfile.findFirst().orElseThrow());
        }
    }

    @Test
    void inputsAreTraversedOnlyOnce() {
        List<Parser.Input> inputs = Arrays.asList(input("Jenkinsfile", "deploy()"),
          input("vars/deploy.groovy", "def call() {}"));
        Iterator<Parser.Input> iterator = inputs.iterator();
        try (Stream<SourceFile> parsed = GroovyParser.builder().build().parseInputs(() -> iterator,
          null, new InMemoryExecutionContext(Throwable::printStackTrace))) {
            assertThat(parsed.collect(toList())).hasSize(2).allMatch(G.CompilationUnit.class::isInstance);
        }
    }

    private static Parser.Input input(String path, String source) {
        return Parser.Input.fromString(Paths.get(path), source);
    }

    private static G.CompilationUnit parse(@Nullable Path root, Parser.Input... inputs) {
        try (Stream<SourceFile> parsed = GroovyParser.builder().build().parseInputs(Arrays.asList(inputs), root,
          new InMemoryExecutionContext(Throwable::printStackTrace))) {
            List<SourceFile> files = parsed.collect(toList());
            assertThat(files).allMatch(G.CompilationUnit.class::isInstance);
            return (G.CompilationUnit) files.get(0);
        }
    }

    private static void assertNoSharedLibrarySteps(G.CompilationUnit cu) {
        new GroovyIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                assertThat(method.getMarkers().findFirst(JenkinsSharedLibraryStep.class)).isEmpty();
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0);
    }
}
