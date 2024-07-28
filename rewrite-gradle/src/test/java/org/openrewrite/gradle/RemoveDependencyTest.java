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
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
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
                    .filter(dep -> dep.getGroupId().equals("org.springframework.boot") && dep.getArtifactId().equals("spring-boot-starter-web"))
                    .findAny())
                  .as("GradleProject requested dependencies should have been updated to remove `spring-boot-starter-web`")
                  .isNotPresent();
                assertThat(
                  compileClasspath.getResolved().stream()
                    .filter(dep -> dep.getGroupId().equals("org.springframework.boot") && dep.getArtifactId().equals("spring-boot-starter-web"))
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

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
                  testImplementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
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
                  testImplementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
              }
              """
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
}
