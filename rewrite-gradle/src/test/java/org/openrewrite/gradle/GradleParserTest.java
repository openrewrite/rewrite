/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;

public class GradleParserTest implements RewriteTest {

    @Test
    void buildGradleAndSettingsGradle() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              gradleEnterprise {
                  server = 'https://enterprise-samples.gradle.com'
                  buildScan {
                      publishAlways()
                  }
              }
              """
          )
        );
    }

    @Test
    void allowImports() {
        rewriteRun(
          buildGradle(
            """
              import org.gradle.api.Project
              
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation "org.openrewrite:rewrite-java:latest.release"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(4);
                assertThat(cu.getStatements().get(0)).isInstanceOf(J.Import.class);
                J.Import i = (J.Import) cu.getStatements().get(0);
                assertThat(i.getTypeName()).isEqualTo("org.gradle.api.Project");
                assertThat(cu.getStatements().get(3)).isInstanceOf(J.MethodInvocation.class);
                J.MethodInvocation m = (J.MethodInvocation) cu.getStatements().get(3);
                assertThat(m.getMethodType()).isNotNull();
                assertThat(m.getMethodType().getDeclaringType().getFullyQualifiedName()).isNotNull();
            })
          )
        );
    }

    @Test
    void allowMethodDeclaration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation "org.openrewrite:rewrite-java:latest.release"
              }

              def greet() {
                  return "Hello, world!"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(4);
                assertThat(cu.getStatements().get(2)).isInstanceOf(J.MethodInvocation.class);
                J.MethodInvocation m = (J.MethodInvocation) cu.getStatements().get(2);
                assertThat(m.getMethodType()).isNotNull();
                assertThat(m.getMethodType().getDeclaringType().getFullyQualifiedName()).isNotNull();
                assertThat(cu.getStatements().get(3)).isInstanceOf(J.MethodDeclaration.class);
                J.MethodDeclaration d = (J.MethodDeclaration) cu.getStatements().get(3);
                assertThat(d.getSimpleName()).isEqualTo("greet");
            })
          )
        );
    }

    @Test
    void dontDropLeadingComments() {
        rewriteRun(
          buildGradle(
            """
              /*
               * LICENSE
               */
              import org.gradle.api.Project
              
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void dontClobberExistingComments() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              // Some comment
              dependencies {
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void handleImportsThatArentTheFirstStatement() {
        rewriteRun(
          buildGradle(
            """
              import org.gradle.api.Project
              
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }
}
