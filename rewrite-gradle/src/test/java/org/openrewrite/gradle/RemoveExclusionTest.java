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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class RemoveExclusionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExclusion("org.springframework.boot", "spring-boot-starter-tomcat"));
    }

    @DocumentExample
    @Test
    void dependencyExclusion() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                      exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
                  }
              }
              """,
            """
              dependencies {
                  implementation('org.springframework.boot:spring-boot-starter-web:3.5.0')
              }
              """
          )
        );
    }

    @Test
    void keepsOtherExclusions() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                      exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """,
            """
              dependencies {
                  implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void configurationExclusion() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  implementation {
                      exclude module: 'spring-boot-starter-tomcat'
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web:3.5.0'
              }
              """,
            """
              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web:3.5.0'
              }
              """
          )
        );
    }

    @Test
    void allConfigurationsExclusion() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  all*.exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
                  compileOnly {
                      extendsFrom annotationProcessor
                  }
              }
              """,
            """
              configurations {
                  compileOnly {
                      extendsFrom annotationProcessor
                  }
              }
              """
          )
        );
    }

    @Test
    void excludeOfWholeGroup() {
        rewriteRun(
          buildGradle(
            """
              configurations.all {
                  exclude group: 'org.springframework.boot'
                  resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
              }
              """,
            """
              configurations.all {
                  resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
              }
              """
          )
        );
    }

    @Test
    void kotlinDependencyExclusion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:3.5.0") {
                      exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
                  }
              }
              """,
            """
              plugins {
                  java
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:3.5.0")
              }
              """
          )
        );
    }

    @Test
    void kotlinConfigurationExclusion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }

              configurations.all {
                  exclude(module = "spring-boot-starter-tomcat")
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:3.5.0")
              }
              """,
            """
              plugins {
                  java
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:3.5.0")
              }
              """
          )
        );
    }

    @Test
    void requiresGroupOrArtifact() {
        assertThat(new RemoveExclusion(null, null).validate().isValid()).isFalse();
    }

    @Nested
    class NoChange {

        @Test
        void otherModule() {
            rewriteRun(
              buildGradle(
                """
                  dependencies {
                      implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                          exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void excludeOfOtherGroupOnly() {
            rewriteRun(
              buildGradle(
                """
                  configurations.all {
                      exclude group: 'commons-logging'
                  }
                  """
              )
            );
        }

        @Test
        void excludeOfModuleWhenOnlyGroupIsGiven() {
            rewriteRun(
              spec -> spec.recipe(new RemoveExclusion("org.springframework.boot", null)),
              buildGradle(
                """
                  configurations.all {
                      exclude module: 'commons-logging'
                  }
                  """
              )
            );
        }

        @Test
        void fileExcludeOfCopyTask() {
            rewriteRun(
              buildGradle(
                """
                  plugins {
                      id 'java'
                  }

                  jar {
                      exclude 'org.springframework.boot', 'spring-boot-starter-tomcat'
                  }
                  """
              )
            );
        }

        @Test
        void closureThatWasAlreadyEmpty() {
            rewriteRun(
              buildGradle(
                """
                  dependencies {
                      implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                      }
                  }
                  """
              )
            );
        }
    }
}
