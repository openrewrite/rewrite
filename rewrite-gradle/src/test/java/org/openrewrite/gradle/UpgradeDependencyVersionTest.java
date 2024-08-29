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
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainTextParser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.customGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

class UpgradeDependencyVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "30.x", "-jre"));
    }

    @DocumentExample
    @Test
    void guava() {
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
                compileOnly 'com.google.guava:guava:29.0-jre'
                runtimeOnly ('com.google.guava:guava:29.0-jre')
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
                compileOnly 'com.google.guava:guava:30.1.1-jre'
                runtimeOnly ('com.google.guava:guava:30.1.1-jre')
              }
              """,
            spec -> spec.afterRecipe(after -> {
                Optional<GradleProject> maybeGp = after.getMarkers().findFirst(GradleProject.class);
                assertThat(maybeGp).isPresent();
                GradleProject gp = maybeGp.get();
                GradleDependencyConfiguration compileClasspath = gp.getConfiguration("compileClasspath");
                assertThat(compileClasspath).isNotNull();
                assertThat(
                  compileClasspath.getRequested().stream()
                    .filter(dep -> "com.google.guava".equals(dep.getGroupId()) && "guava".equals(dep.getArtifactId()) && "30.1.1-jre".equals(dep.getVersion()))
                    .findAny())
                  .as("GradleProject requested dependencies should have been updated with the new version of guava")
                  .isPresent();
                assertThat(
                  compileClasspath.getResolved().stream()
                    .filter(dep -> "com.google.guava".equals(dep.getGroupId()) && "guava".equals(dep.getArtifactId()) && "30.1.1-jre".equals(dep.getVersion()))
                    .findAny())
                  .as("GradleProject requested dependencies should have been updated with the new version of guava")
                  .isPresent();
            })
          )
        );
    }

    @Test
    void noRepos() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              dependencies {
                compileOnly 'com.google.guava:guava:29.0-jre'
              }
              """,
            """
              plugins {
                id 'java-library'
              }
              
              dependencies {
                /*~~(com.google.guava:guava failed. Unable to download metadata.)~~>*/compileOnly 'com.google.guava:guava:29.0-jre'
              }
              """
          )
        );
    }

    @Test
    void noReposProperties() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              dependencies {
                compileOnly "com.google.guava:guava:${guavaVersion}"
              }
              """,
            """
              plugins {
                id 'java-library'
              }
              
              dependencies {
                /*~~(com.google.guava:guava failed. Unable to download metadata.)~~>*/compileOnly "com.google.guava:guava:${guavaVersion}"
              }
              """
          )
        );
    }

    @Test
    void updateVersionInVariable() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              def guavaVersion = '29.0-jre'
              def otherVersion = "latest.release"
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:$guavaVersion")
                implementation "com.fasterxml.jackson.core:jackson-databind:$otherVersion"
              }
              """,
            """
              plugins {
                id 'java-library'
              }
              
              def guavaVersion = '30.1.1-jre'
              def otherVersion = "latest.release"
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:$guavaVersion")
                implementation "com.fasterxml.jackson.core:jackson-databind:$otherVersion"
              }
              """
          )
        );
    }

    @Test
    void varargsDependency() {
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
                implementation(
                  'com.google.guava:guava-gwt:29.0-jre',
                  'com.google.guava:guava:29.0-jre')
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
                implementation(
                  'com.google.guava:guava-gwt:29.0-jre',
                  'com.google.guava:guava:30.1.1-jre')
              }
              """)
        );
    }

    @Test
    void mapNotationVariable() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              def guavaVersion = '29.0-jre'
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation group: "com.google.guava", name: "guava", version: guavaVersion
              }
              """,
            """
              plugins {
                id 'java-library'
              }
              
              def guavaVersion = '30.1.1-jre'
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation group: "com.google.guava", name: "guava", version: guavaVersion
              }
              """
          )
        );
    }

    @Test
    void mapNotationLiteral() {
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
                implementation (group: "com.google.guava", name: "guava", version: '29.0-jre')
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
                implementation (group: "com.google.guava", name: "guava", version: '30.1.1-jre')
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3227")
    @Test
    void worksWithPlatform() {
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
                implementation platform("com.google.guava:guava:29.0-jre")
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
                implementation platform("com.google.guava:guava:30.1.1-jre")
              }
              """
          )
        );
    }

    @Test
    void upgradesVariablesDefinedInExtraProperties() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  ext {
                      guavaVersion = "29.0-jre"
                  }
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath("com.google.guava:guava:${guavaVersion}")
                  }
              }
              
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              ext {
                  guavaVersion2 = "29.0-jre"
              }
              
              dependencies {
                  implementation "com.google.guava:guava:${guavaVersion2}"
              }
              """,
            """
              buildscript {
                  ext {
                      guavaVersion = "30.1.1-jre"
                  }
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath("com.google.guava:guava:${guavaVersion}")
                  }
              }
              
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              ext {
                  guavaVersion2 = "30.1.1-jre"
              }
              
              dependencies {
                  implementation "com.google.guava:guava:${guavaVersion2}"
              }
              """
          )
        );
    }

    @Test
    void matchesGlobs() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.*", "gua*", "30.x", "-jre")),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              ext {
                  guavaVersion = "29.0-jre"
              }
              
              def guavaVersion2 = "29.0-jre"
              dependencies {
                  implementation("com.google.guava:guava:29.0-jre")
                  implementation group: "com.google.guava", name: "guava", version: "29.0-jre"
                  implementation "com.google.guava:guava:${guavaVersion}"
                  implementation group: "com.google.guava", name: "guava", version: guavaVersion2
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              ext {
                  guavaVersion = "30.1.1-jre"
              }
              
              def guavaVersion2 = "30.1.1-jre"
              dependencies {
                  implementation("com.google.guava:guava:30.1.1-jre")
                  implementation group: "com.google.guava", name: "guava", version: "30.1.1-jre"
                  implementation "com.google.guava:guava:${guavaVersion}"
                  implementation group: "com.google.guava", name: "guava", version: guavaVersion2
              }
              """
          )
        );
    }

    @Test
    void defaultsToLatestRelease() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", null, "-jre")),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation 'com.google.guava:guava:29.0-jre'
              }
              """,
            spec -> spec.after(after -> {
                Matcher versionMatcher = Pattern.compile("implementation 'com\\.google\\.guava:guava:(.*?)'").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                assertThat(version).isNotEqualTo("29.0-jre");

                return """
                  plugins {
                    id 'java-library'
                  }
                  
                  repositories {
                    mavenCentral()
                  }
                  
                  dependencies {
                    implementation 'com.google.guava:guava:%s'
                  }
                  """.formatted(version);
            })
          )
        );
    }

    @Test
    void versionInPropertiesFile() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:$guavaVersion")
              }
              """
          )
        );
    }

    @Test
    void versionInPropertiesFileNotUpdatedIfNoDependencyVariable() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:30.1.1-jre")
              }
              """
          )
        );
    }

    @Test
    void versionInParentAndMultiModulePropertiesFiles() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("moduleA/gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:$guavaVersion")
              }
              """,
            spec -> spec.path("build.gradle")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:$guavaVersion")
              }
              """,
            spec -> spec.path("moduleA/build.gradle")
          )
        );
    }


    @Test
    void versionInParentSubprojectDefinitionWithPropertiesFiles() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                      mavenCentral()
              }
              
              subprojects {
                  repositories {
                      mavenCentral()
                  }
              
                  dependencies {
                    implementation ("com.google.guava:guava:$guavaVersion")
                  }
              }
              """,
            spec -> spec.path("build.gradle")
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("moduleA/build.gradle")
          )
        );
    }


    @Test
    void versionOnlyInMultiModuleChildPropertiesFiles() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("moduleA/gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                implementation ("com.google.guava:guava:$guavaVersion")
              }
              """,
            spec -> spec.path("moduleA/build.gradle")
          )
        );
    }

    @Test
    void mapNotationVariableInPropertiesFile() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation group: "com.google.guava", name: "guava", version: guavaVersion
              }
              """
          )
        );
    }

    @Test
    void mapNotationGStringVariableInPropertiesFile() {
        rewriteRun(
          properties(
            """
              guavaVersion=29.0-jre
              """,
            """
              guavaVersion=30.1.1-jre
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation group: "com.google.guava", name: "guava", version: "${guavaVersion}"
              }
              """
          )
        );
    }

    @Test
    void globVersionsInPropertiesFileWithMultipleVersionsOnlyUpdatesCorrectProperty() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.security", "*", "5.4.x", null)),
          properties(
            """
              springBootVersion=3.0.0
              springSecurityVersion=5.4.0
              """,
            """
              springBootVersion=3.0.0
              springSecurityVersion=5.4.11
              """,
            spec -> spec.path("gradle.properties")
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
                  implementation("org.springframework.boot:spring-boot-starter-actuator:${springBootVersion}")
                  implementation("org.springframework.security:spring-security-oauth2-core:${springSecurityVersion}")
              }
              """
          )
        );
    }

    @Test
    void disallowDowngrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.security", "*", "5.3.x", null)),
          properties(
            """
              springBootVersion=3.0.0
              springSecurityVersion=5.4.0
              """,
            spec -> spec.path("gradle.properties")
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
                  implementation("org.springframework.boot:spring-boot-starter-actuator:${springBootVersion}")
                  implementation("org.springframework.security:spring-security-oauth2-core:${springSecurityVersion}")
              }
              """
          )
        );
    }

    @Test
    void dontDowngradeWhenExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "28.0", "-jre")),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation 'com.google.guava:guava:29.0-jre'
              }
              """
          )
        );
    }

    @Test
    void leaveConstraintsAlone() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "29.0", "-jre")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  constraints {
                      implementation("com.google.guava:guava:28.0-jre")
                  }
                  implementation("com.google.guava:guava")
              }
              """
          )
        );
    }

    @Test
    void unknownConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.openapitools", "openapi-generator-cli", "5.2.1", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id "org.hidetake.swagger.generator" version "2.18.2"
              }
              dependencies {
                  swaggerCodegen "org.openapitools:openapi-generator-cli:5.2.0"
              }
              """,
            """
              plugins {
                  id 'java'
                  id "org.hidetake.swagger.generator" version "2.18.2"
              }
              dependencies {
                  swaggerCodegen "org.openapitools:openapi-generator-cli:5.2.1"
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4275")
    void noActionForNonStringLiterals() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation(gradleApi())
                jar {
                  enabled(true)
                }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-java-dependencies/pull/106")
    void isAcceptable() {
        // Mimic org.openrewrite.java.dependencies.UpgradeTransitiveDependencyVersion#getVisitor
        UpgradeDependencyVersion guava = new UpgradeDependencyVersion("com.google.guava", "guava", "30.x", "-jre");
        TreeVisitor<?, ExecutionContext> visitor = guava.getVisitor();

        SourceFile sourceFile = PlainTextParser.builder().build().parse("not a gradle file").findFirst().orElseThrow();
        assertThat(visitor.isAcceptable(sourceFile, new InMemoryExecutionContext())).isFalse();

        sourceFile = PropertiesParser.builder().build().parse("guavaVersion=29.0-jre").findFirst().orElseThrow();
        assertThat(visitor.isAcceptable(sourceFile, new InMemoryExecutionContext())).isTrue();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4333")
    void exactVersionWithExactPattern() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "32.1.1", "-jre")),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation('com.google.guava:guava:29.0-jre')
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
                implementation('com.google.guava:guava:32.1.1-jre')
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4333")
    void exactVersionWithRegexPattern() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "32.1.1", ".*droid")),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              
              repositories {
                mavenCentral()
              }
              
              dependencies {
                implementation('com.google.guava:guava:29.0-android')
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
                implementation('com.google.guava:guava:32.1.1-android')
              }
              """
          )
        );
    }

    @Test
    void updateDependenciesGradle() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("mysql", "mysql-connector-java", "8.0.33", null)),
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories {
                mavenCentral()
              }
              apply from: "dependencies.gradle"
              """
          ),
          customGradle(
            """
              dependencies {
                implementation 'mysql:mysql-connector-java:8.0.11'
              }
              """,
            """
              dependencies {
                implementation 'mysql:mysql-connector-java:8.0.33'
              }
              """
            , spec -> spec.path("dependencies.gradle")
          )
        );
    }

}
