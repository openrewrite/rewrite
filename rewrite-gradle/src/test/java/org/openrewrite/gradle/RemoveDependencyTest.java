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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;

class RemoveDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveDependency("org.springframework.boot", "spring-boot*", null));
    }

    @DocumentExample
    @Test
    void removeGradleDependencyUsingStringNotation() {
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
                  implementation "org.springframework.boot:spring-boot-starter-web:2.7.0"
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                assertThat(maybeGp).isPresent();
                GradleProject gp = maybeGp.get();
                GradleDependencyConfiguration compileClasspath = gp.getConfiguration("compileClasspath");
                assertThat(
                  compileClasspath.getRequested().stream()
                    .filter(dep -> "org.springframework.boot".equals(dep.getGroupId()) && "spring-boot-starter-web".equals(dep.getArtifactId()))
                    .findAny())
                  .as("GradleProject requested dependencies should have been updated to remove `spring-boot-starter-web`")
                  .isNotPresent();
                assertThat(
                  compileClasspath.getResolved().stream()
                    .filter(dep -> "org.springframework.boot".equals(dep.getGroupId()) && "spring-boot-starter-web".equals(dep.getArtifactId()))
                    .findAny())
                  .as("GradleProject resolved dependencies should have been updated to remove `spring-boot-starter-web`")
                  .isNotPresent();
            })
          )
        );
    }

    @Test
    void removeGradleDependencyUsingStringNotationWithExclusion() {
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
                  implementation("org.springframework.boot:spring-boot-starter-web:2.7.0") {
                      exclude group: "junit"
                  }
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeGradleDependencyUsingMapNotation() {
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
                  implementation group: "org.springframework.boot", name: "spring-boot-starter-web", version: "2.7.0"
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeMatchingNotationsOfSeveralInOneCall() {
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
                  implementation("org.springframework.boot:spring-boot-starter-web:2.7.0", "com.google.guava:guava:29.0-jre")
                  implementation "org.springframework.boot:spring-boot-starter-jdbc:2.7.0", "org.springframework.boot:spring-boot-starter-json:2.7.0"
                  testImplementation(
                          // Actuator
                          [group: "org.springframework.boot", name: "spring-boot-starter-actuator", version: "2.7.0"],
                          // JUnit
                          [group: "junit", name: "junit", version: "4.13.2"],
                          // Spring test support
                          [group: "org.springframework.boot", name: "spring-boot-starter-test", version: "2.7.0"]
                  )
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
                  implementation("com.google.guava:guava:29.0-jre")
                  testImplementation(
                          // JUnit
                          [group: "junit", name: "junit", version: "4.13.2"]
                  )
              }
              """
          )
        );
    }

    @Test
    void removeMatchingNotationNextToFilesDependency() {
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
                  implementation "org.springframework.boot:spring-boot-starter-web:2.7.0", files("lib/local.jar")
                  runtimeOnly files("lib/other.jar"), "org.springframework.boot:spring-boot-starter-json:2.7.0"
                  testImplementation /* JSON */ "org.springframework.boot:spring-boot-starter-json:2.7.0", files("lib/test.jar")
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
                  implementation files("lib/local.jar")
                  runtimeOnly files("lib/other.jar")
                  testImplementation files("lib/test.jar")
              }
              """
          )
        );
    }

    @Test
    void removeSingleDependencyWrittenAsSeveralArguments() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def bootVersion = '2.7.0'
              def withoutLogging = { exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging' }

              dependencies {
                  implementation group: 'org.springframework.boot', name: 'spring-boot-starter-web', version:bootVersion
                  implementation 'org.springframework.boot:spring-boot-starter-json:2.7.0', withoutLogging
                  implementation 'com.google.guava:guava:29.0-jre'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def bootVersion = '2.7.0'
              def withoutLogging = { exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging' }

              dependencies {
                  implementation 'com.google.guava:guava:29.0-jre'
              }
              """
          )
        );
    }

    @Test
    void removeKotlinDependencyWithConfigurationArgument() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:2.7.0", dependencyConfiguration = { isTransitive = false })
                  implementation("com.google.guava:guava:29.0-jre")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.google.guava:guava:29.0-jre")
              }
              """
          )
        );
    }

    @Test
    void removeGradleDependencyUsingMapNotationWithExclusion() {
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
                  implementation(group: "org.springframework.boot", name: "spring-boot-starter-web", version: "2.7.0") {
                      exclude group: "junit"
                  }
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeWhenUsingVariableReplacement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              def springBootVersion = "2.7.0"
              dependencies {
                  implementation "org.springframework.boot:spring-boot-starter-web:${springBootVersion}"
                  implementation group: "org.springframework.boot", name: "spring-boot-starter-web", version: springBootVersion
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              def springBootVersion = "2.7.0"
              dependencies {
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removePlatformDependency() {
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
                  implementation platform("org.springframework.boot:spring-boot-dependencies:2.7.0")
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void onlyRemoveFromSpecifiedConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.springframework.boot", "spring-boot-starter-test", "implementation")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              configurations{
                  create("isolated")
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
                  testImplementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
                  add("isolated", "org.springframework.boot:spring-boot-starter-test:2.7.0")
              }
              """,
            """
              plugins {
                  id 'java-library'
              }
              
              configurations{
                  create("isolated")
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  testImplementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
                  add("isolated", "org.springframework.boot:spring-boot-starter-test:2.7.0")
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getMarkers().findFirst(GradleProject.class)
                  .map(gp -> gp.getConfiguration("compileClasspath"))
                  .map(conf -> conf.getRequested().stream()
                    .noneMatch(d -> "org.springframework.boot".equals(d.getGroupId()) &&
                      "spring-boot-starter-test".equals(d.getArtifactId()))))
                  .as("spring-boot-starter-test should be removed from compileClasspath (extends implementation)")
                  .hasValue(true);
                assertThat(cu.getMarkers().findFirst(GradleProject.class)
                  .map(gp -> gp.getConfiguration("isolated"))
                  .map(conf -> conf.getRequested().stream()
                    .anyMatch(d -> "org.springframework.boot".equals(d.getGroupId()) &&
                      "spring-boot-starter-test".equals(d.getArtifactId()))))
                  .as("spring-boot-starter-test should remain in isolated (does not extend implementation)")
                  .hasValue(true);
            })
          )
        );
    }

    @Test
    void removeLastDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.junit.vintage", "junit-vintage-engine", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation "org.springframework.boot:spring-boot-starter-web:2.7.0"
                  testImplementation "org.junit.vintage:junit-vintage-engine:5.6.2"
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
                  implementation "org.springframework.boot:spring-boot-starter-web:2.7.0"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4033")
    @Test
    void removeFromSubproject() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.hibernate", "hibernate-entitymanager", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id "org.openrewrite.rewrite" version "6.8.2"
              }
              
              group = 'org.example'
              version = '1.0-SNAPSHOT'
              
              repositories {
                  mavenCentral()
              }
              
              rewrite {
                  activeRecipe("com.example.RemoveHibernateEntityManager")
              }
              
              dependencies {
                  rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.6.4"))
              }
              """
          ),
          settingsGradle(
            """
              rootProject.name = 'OpenRewrite-example'
              include 'example'
              """
          ),
          mavenProject(
            "subproject",
            buildGradle(
              """
                plugins {
                    id 'java'
                }
                
                group = 'org.example'
                version = '1.0-SNAPSHOT'
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.hibernate:hibernate-entitymanager:5.6.15.Final'
                }
                
                test {
                    useJUnitPlatform()
                }
                """,
              """
                plugins {
                    id 'java'
                }
                
                group = 'org.example'
                version = '1.0-SNAPSHOT'
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                }
                
                test {
                    useJUnitPlatform()
                }
                """
            )
          )
        );
    }

    @Test
    void removeBuildscriptDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.springframework.boot", "spring-boot-gradle-plugin", null)),
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath "org.springframework.boot:spring-boot-gradle-plugin:2.7.18"
                  }
              }
              """,
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeDependencyDefinedInJvmTestSuite() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
                  id 'jvm-test-suite'
              }
              
              repositories {
                  mavenCentral()
              }
              
              testing {
                  suites {
                      test {
                          dependencies {
                              implementation "org.springframework.boot:spring-boot-starter-web:2.7.0"
                              implementation "org.junit.vintage:junit-vintage-engine:5.6.2"
                          }
                      }
                  }
              }
              """,
            """
              plugins {
                  id "java-library"
                  id 'jvm-test-suite'
              }
              
              repositories {
                  mavenCentral()
              }
              
              testing {
                  suites {
                      test {
                          dependencies {
                              implementation "org.junit.vintage:junit-vintage-engine:5.6.2"
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dependenciesBlockInFreestandingScript() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenLocal()
                  mavenCentral()
                  maven {
                     url = uri("https://central.sonatype.com/repository/maven-snapshots")
                  }
              }
              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:2.7.0")
                  implementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """,
            """
              repositories {
                  mavenLocal()
                  mavenCentral()
                  maven {
                     url = uri("https://central.sonatype.com/repository/maven-snapshots")
                  }
              }
              dependencies {
                  implementation "org.junit.vintage:junit-vintage-engine:5.6.2"
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              apply from: 'dependencies.gradle'
              """
          )
        );
    }

    @Test
    void kotlinDslString() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter-web:2.7.0")
                  testImplementation("org.junit.vintage:junit-vintage-engine:5.6.2")
              }
              """,
            """
              plugins {
                  `java-library`
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  testImplementation("org.junit.vintage:junit-vintage-engine:5.6.2")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslMap() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(group = "org.springframework.boot", name = "spring-boot-starter-web", version = "2.7.0")
                  testImplementation(group = "org.junit.vintage", name = "junit-vintage-engine", version = "5.6.2")
              }
              """,
            """
              plugins {
                  `java-library`
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  testImplementation(group = "org.junit.vintage", name = "junit-vintage-engine", version = "5.6.2")
              }
              """
          )
        );
    }
}
