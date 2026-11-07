/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

@Issue("https://github.com/moderneinc/customer-requests/issues/2389")
class HasMinimumJavaVersionBuildScriptTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
          .recipeFromYaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.gradle.GatedOnJava17
              description: Test.
              preconditions:
                - org.openrewrite.Singleton
                - org.openrewrite.java.search.HasMinimumJavaVersion:
                    version: 17
              recipeList:
                - org.openrewrite.gradle.AddDependency:
                    groupId: org.junit.platform
                    artifactId: junit-platform-launcher
                    version: 1.x
                    acceptTransitive: true
                    configuration: testRuntimeOnly
                    onlyIfUsing: org.junit.jupiter.api.Test
              """,
            "org.openrewrite.gradle.GatedOnJava17"
          );
    }

    @Test
    void buildScriptPassesWhenRepositoryMeetsMinimum() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(
                """
                  import org.junit.jupiter.api.Test;
                  public class A {
                      @Test
                      void foo() {
                      }
                  }
                  """,
                spec -> spec.markers(javaVersion(17))
              )
            ),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }

                repositories {
                    mavenCentral()
                }
                """,
              spec -> spec.after(buildGradle -> assertThat(buildGradle)
                .contains("testRuntimeOnly \"org.junit.platform:junit-platform-launcher:")
                .actual())
            )
          )
        );
    }

    @Test
    void buildScriptPassesWithMinimumAsOnlyPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.gradle.OnlyGatedOnJava17
              description: Test.
              preconditions:
                - org.openrewrite.java.search.HasMinimumJavaVersion:
                    version: 17
              recipeList:
                - org.openrewrite.gradle.AddDependency:
                    groupId: org.junit.platform
                    artifactId: junit-platform-launcher
                    version: 1.x
                    acceptTransitive: true
                    configuration: testRuntimeOnly
                    onlyIfUsing: org.junit.jupiter.api.Test
              """,
            "org.openrewrite.gradle.OnlyGatedOnJava17"
          ),
          mavenProject("project",
            srcTestJava(
              java(
                """
                  import org.junit.jupiter.api.Test;
                  public class A {
                      @Test
                      void foo() {
                      }
                  }
                  """,
                spec -> spec.markers(javaVersion(17))
              )
            ),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }

                repositories {
                    mavenCentral()
                }
                """,
              spec -> spec.after(buildGradle -> assertThat(buildGradle)
                .contains("testRuntimeOnly \"org.junit.platform:junit-platform-launcher:")
                .actual())
            )
          )
        );
    }

    @Test
    void buildScriptBlockedWhenRepositoryBelowMinimum() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(
                """
                  import org.junit.jupiter.api.Test;
                  public class A {
                      @Test
                      void foo() {
                      }
                  }
                  """,
                spec -> spec.markers(javaVersion(11))
              )
            ),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }

                repositories {
                    mavenCentral()
                }
                """
            )
          )
        );
    }
}
