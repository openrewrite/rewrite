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
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

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
    void keepsClosureWithComment() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                      exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
                      // keep in sync with the parent build
                  }
              }
              """,
            """
              dependencies {
                  implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                      // keep in sync with the parent build
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
    void excludeOfWholeGroupWhenOnlyGroupIsGiven() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion("org.springframework.boot", null)),
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
    void wildcardGroup() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion("*", "spring-boot-starter-tomcat")),
          buildGradle(
            """
              configurations.all {
                  exclude group: 'com.acme', module: 'spring-boot-starter-tomcat'
                  exclude module: 'spring-boot-starter-tomcat'
                  exclude group: 'com.acme', module: 'spring-boot-starter-jetty'
              }
              """,
            """
              configurations.all {
                  exclude group: 'com.acme', module: 'spring-boot-starter-jetty'
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
    void kotlinPositionalExclusion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:3.5.0") {
                      exclude("org.springframework.boot", "spring-boot-starter-tomcat")
                  }
                  implementation("org.springframework.boot:spring-boot-starter-actuator:3.5.0") {
                      exclude("org.springframework.boot", module = "spring-boot-starter-tomcat")
                  }
                  implementation("org.springframework.boot:spring-boot-starter-webflux:3.5.0") {
                      exclude(null, "spring-boot-starter-tomcat")
                  }
              }
              """,
            """
              plugins {
                  java
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:3.5.0")
                  implementation("org.springframework.boot:spring-boot-starter-actuator:3.5.0")
                  implementation("org.springframework.boot:spring-boot-starter-webflux:3.5.0")
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
    void kotlinConfigurationLookupExclusion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }

              configurations.named("implementation") {
                  exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
              }
              configurations.getByName("runtimeOnly") {
                  exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
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
    void kotlinConfigurationLookupInConfigurationsBlock() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }

              configurations {
                  named("implementation") {
                      exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
                  }
              }
              """,
            """
              plugins {
                  java
              }
              """
          )
        );
    }

    @Test
    void keepsCreatedConfiguration() {
        rewriteRun(
          buildGradleKts(
            """
              configurations.create("tomcatFree") {
                  exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
              }
              """,
            """
              configurations.create("tomcatFree")
              """
          )
        );
    }

    @Test
    void requiresGroupOrArtifact() {
        assertThat(new RemoveExclusion(null, null).validate().isValid()).isFalse();
        assertThat(new RemoveExclusion("*", "*").validate().isValid()).isFalse();
        assertThat(new RemoveExclusion("", "").validate().isValid()).isFalse();
    }

    @Test
    void rejectsPartialWildcards() {
        assertThat(new RemoveExclusion("org.springframework.*", "spring-boot-starter-tomcat").validate().isValid()).isFalse();
        assertThat(new RemoveExclusion("org.springframework.boot", "spring-boot-starter-*").validate().isValid()).isFalse();
        assertThat(new RemoveExclusion("*", "spring-boot-starter-tomcat").validate().isValid()).isTrue();
    }

    @Nested
    class GradleModel {

        @Test
        void removesExclusionFromModel() {
            rewriteRun(
              spec -> spec.beforeRecipe(withToolingApi())
                .recipe(new RemoveExclusion("commons-logging", "commons-logging")),
              buildGradle(
                """
                  plugins {
                      id 'java-library'
                  }

                  repositories {
                      mavenCentral()
                  }

                  dependencies {
                      implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                          exclude group: 'commons-logging', module: 'commons-logging'
                      }
                  }
                  """,
                """
                  plugins {
                      id 'java-library'
                  }

                  repositories {
                      mavenCentral()
                  }

                  dependencies {
                      implementation('org.apache.httpcomponents:httpclient:4.5.13')
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    assertThat(requestedExclusions(cu, "implementation", "httpclient")).isEmpty();
                    assertThat(resolved(cu, "compileClasspath")).contains("commons-logging:commons-logging");
                })
              )
            );
        }

        @Test
        void laterExcludeDependencyDoesNotRestoreRemovedExclusion() {
            rewriteRun(
              spec -> spec.beforeRecipe(withToolingApi())
                .recipes(
                  new RemoveExclusion("commons-logging", "commons-logging"),
                  new ExcludeDependency("commons-codec", "commons-codec", null)),
              buildGradle(
                """
                  plugins {
                      id 'java-library'
                  }

                  repositories {
                      mavenCentral()
                  }

                  dependencies {
                      implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                          exclude group: 'commons-logging', module: 'commons-logging'
                      }
                  }
                  """,
                """
                  plugins {
                      id 'java-library'
                  }

                  repositories {
                      mavenCentral()
                  }

                  dependencies {
                      implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                          exclude group: 'commons-codec', module: 'commons-codec'
                      }
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    assertThat(requestedExclusions(cu, "implementation", "httpclient"))
                      .containsExactly(new GroupArtifact("commons-codec", "commons-codec"));
                    assertThat(resolved(cu, "compileClasspath"))
                      .contains("commons-logging:commons-logging")
                      .doesNotContain("commons-codec:commons-codec");
                })
              )
            );
        }

        private static List<String> resolved(SourceFile cu, String configuration) {
            return configuration(cu, configuration).getResolved().stream()
              .map(d -> d.getGroupId() + ":" + d.getArtifactId())
              .collect(toList());
        }

        private static List<GroupArtifact> requestedExclusions(SourceFile cu, String configuration, String artifactId) {
            return configuration(cu, configuration).getRequested().stream()
              .filter(d -> artifactId.equals(d.getArtifactId()))
              .flatMap(d -> d.getExclusions().stream())
              .collect(toList());
        }

        private static GradleDependencyConfiguration configuration(SourceFile cu, String configuration) {
            GradleDependencyConfiguration conf = cu.getMarkers().findFirst(GradleProject.class)
              .orElseThrow(IllegalStateException::new)
              .getConfiguration(configuration);
            assertThat(conf).isNotNull();
            return conf;
        }
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
        void kotlinPositionalOtherModule() {
            rewriteRun(
              buildGradleKts(
                """
                  configurations.all {
                      exclude("org.springframework.boot", "spring-boot-starter-logging")
                      exclude("org.springframework.boot")
                      exclude("com.acme", module = "spring-boot-starter-tomcat")
                  }
                  """
              )
            );
        }

        @Test
        void excludeOfWholeGroupWhenArtifactIsGiven() {
            rewriteRun(
              buildGradle(
                """
                  configurations.all {
                      exclude group: 'org.springframework.boot'
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
        void moduleFromVariable() {
            rewriteRun(
              buildGradle(
                """
                  def excluded = 'spring-boot-starter-jetty'
                  dependencies {
                      implementation('org.springframework.boot:spring-boot-starter-web:3.5.0') {
                          exclude group: 'org.springframework.boot', module: excluded
                      }
                  }
                  """
              )
            );
        }

        @Test
        void groupFromGString() {
            rewriteRun(
              buildGradle(
                """
                  def g = 'com.acme'
                  configurations.all {
                      exclude group: "${g}", module: 'spring-boot-starter-tomcat'
                  }
                  """
              )
            );
        }

        @Test
        void kotlinModuleFromVariable() {
            rewriteRun(
              buildGradleKts(
                """
                  val excluded = "spring-boot-starter-jetty"
                  configurations.all {
                      exclude(group = "org.springframework.boot", module = excluded)
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
