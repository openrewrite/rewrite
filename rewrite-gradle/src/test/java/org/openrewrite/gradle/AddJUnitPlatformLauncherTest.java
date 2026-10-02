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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

class AddJUnitPlatformLauncherTest implements RewriteTest {

    private static final String JUPITER_TEST = """
      import org.junit.jupiter.api.Test;
      public class A {
          @Test
          void foo() {
          }
      }
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
          .recipe(new AddJUnitPlatformLauncher());
    }

    @DocumentExample
    @Test
    void addJUnitPlatformLauncher() {
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
                  """
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
              spec -> spec.after(buildGradle -> {
                  assertThat(buildGradle).contains("testRuntimeOnly \"org.junit.platform:junit-platform-launcher:");
                  return buildGradle;
              })
            )
          )
        );
    }

    @Test
    void versionlessWhenManagedBySpringDependencyManagement() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                    id 'io.spring.dependency-management' version '1.1.7'
                }

                repositories {
                    mavenCentral()
                }

                dependencyManagement {
                    imports {
                        mavenBom 'org.junit:junit-bom:6.0.1'
                    }
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter'
                }
                """,
              """
                plugins {
                    id 'java'
                    id 'io.spring.dependency-management' version '1.1.7'
                }

                repositories {
                    mavenCentral()
                }

                dependencyManagement {
                    imports {
                        mavenBom 'org.junit:junit-bom:6.0.1'
                    }
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter'

                    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
                }
                """
            )
          )
        );
    }

    @Test
    void versionlessWhenManagedByPlatform() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation platform('org.junit:junit-bom:6.0.1')
                    testImplementation 'org.junit.jupiter:junit-jupiter'
                }
                """,
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation platform('org.junit:junit-bom:6.0.1')
                    testImplementation 'org.junit.jupiter:junit-jupiter'

                    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
                }
                """
            )
          )
        );
    }

    @Test
    void versionlessWithJUnit6() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.0.1'
                }
                """,
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.0.1'

                    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
                }
                """
            )
          )
        );
    }

    @Test
    void versionlessWithJUnit5() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'
                }
                """,
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'

                    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
                }
                """
            )
          )
        );
    }

    @Test
    void alignsWithJUnitPlatformWithoutBom() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.5.2'
                }
                """,
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.5.2'

                    testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.5.2"
                }
                """
            )
          )
        );
    }

    @Test
    void springBoot4AlreadyBringsLauncher() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                    id 'org.springframework.boot' version '4.0.1'
                    id 'io.spring.dependency-management' version '1.1.7'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.springframework.boot:spring-boot-starter-test'
                }
                """
            )
          )
        );
    }

    @Test
    void launcherAlreadyPresent() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(JUPITER_TEST)
            ),
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:6.0.1'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                }
                """
            )
          )
        );
    }
}
