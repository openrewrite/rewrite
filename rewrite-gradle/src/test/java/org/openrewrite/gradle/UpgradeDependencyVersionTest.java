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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.openrewrite.gradle.Assertions.*;
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
    void guavaCompileOnly() {
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
                GradleDependencyConfiguration compileClasspath = maybeGp.get().getConfiguration("compileClasspath");
                assertThat(compileClasspath).isNotNull();
                assertThat(compileClasspath.getRequested())
                  .as("GradleProject requested dependencies should have been updated with the new version of guava")
                  .anySatisfy(dep -> {
                      assertThat(dep.getGroupId()).isEqualTo("com.google.guava");
                      assertThat(dep.getArtifactId()).isEqualTo("guava");
                      assertThat(dep.getVersion()).isEqualTo("30.1.1-jre");
                  });
                assertThat(compileClasspath.getResolved())
                  .as("GradleProject resolved dependencies should have been updated with the new version of guava")
                  .anySatisfy(dep -> {
                      assertThat(dep.getGroupId()).isEqualTo("com.google.guava");
                      assertThat(dep.getArtifactId()).isEqualTo("guava");
                      assertThat(dep.getVersion()).isEqualTo("30.1.1-jre");
                  });
            })
          )
        );
    }

    @Test
    void fromMilestoneToLatestPatch() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("org.apache.tomcat.embed", "*", "latest.patch", null)),
          //language=groovy
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.0.0-M1'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.0.27'
              }
              """
          )
        );
    }

    @Test
    void mockitoTestImplementation() {
        rewriteRun(recipeSpec -> {
              recipeSpec.beforeRecipe(withToolingApi())
                .recipe(new UpgradeDependencyVersion("org.mockito", "*", "4.11.0", null));
          },
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              dependencies {
                testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
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
                testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
              }
              """,
            spec -> spec.afterRecipe(after -> {
                Optional<GradleProject> maybeGp = after.getMarkers().findFirst(GradleProject.class);
                assertThat(maybeGp).isPresent();
                GradleDependencyConfiguration testCompileClasspath = maybeGp.get().getConfiguration("testCompileClasspath");
                assertThat(testCompileClasspath).isNotNull();
                assertThat(testCompileClasspath.getRequested())
                  .as("GradleProject requested dependencies should have been updated with the new version of mockito")
                  .anySatisfy(dep -> {
                      assertThat(dep.getGroupId()).isEqualTo("org.mockito");
                      assertThat(dep.getArtifactId()).isEqualTo("mockito-junit-jupiter");
                  });
                assertThat(testCompileClasspath.getResolved())
                  .as("GradleProject resolved dependencies should have been updated with the new version of mockito")
                  .anySatisfy(dep -> {
                      assertThat(dep.getGroupId()).isEqualTo("org.mockito");
                      assertThat(dep.getArtifactId()).isEqualTo("mockito-junit-jupiter");
                  });
            })
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
    void updateVersionInVariableToRelease() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot", "3.5.x", null)),
          //language=groovy
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              def springBootVersion = '3.1.5'
              repositories {
                mavenCentral()
              }

              dependencies {
                implementation ("org.springframework.boot:spring-boot:$springBootVersion")
              }
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .containsPattern("def springBootVersion = '3.5.\\d+'")
                .actual()
            )
          )
        );
    }

    @Test
    void deeplyNestedProjectDependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              dependencies {
                implementation project(":foo:bar:baz:qux:quux")
              }
              """,
            spec -> spec.path("build.gradle")
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }
              """,
            spec -> spec.path("foo/bar/baz/qux/quux/build.gradle")
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              include("foo:bar:baz:qux:quux")
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
              """
          )
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

    @ParameterizedTest
    @ValueSource(strings = {"$guavaVersion", "${guavaVersion}"})
    void mapNotationGStringInterpolation(String stringInterpolationReference) {
        rewriteRun(
          buildGradle(
                  """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              def guavaVersion = '29.0-jre'

              dependencies {
                implementation(group: "com.google.guava", name: "guava", version: "%s")
              }
              """.formatted(stringInterpolationReference),
                  """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              def guavaVersion = '30.1.1-jre'

              dependencies {
                implementation(group: "com.google.guava", name: "guava", version: "%s")
              }
              """.formatted(stringInterpolationReference)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"$guavaVersion", "${guavaVersion}"})
    void mapNotationKStringTemplateInterpolation(String stringInterpolationReference) {
        rewriteRun(
          buildGradleKts(
                  """
              plugins {
                `java-library`
              }

              repositories {
                mavenCentral()
              }

              val guavaVersion = "29.0-jre"

              dependencies {
                implementation(group = "com.google.guava", name = "guava", version = "%s")
              }
              """.formatted(stringInterpolationReference),
                  """
              plugins {
                `java-library`
              }

              repositories {
                mavenCentral()
              }

              val guavaVersion = "30.1.1-jre"

              dependencies {
                implementation(group = "com.google.guava", name = "guava", version = "%s")
              }
              """.formatted(stringInterpolationReference)
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
    void upgradesVariablesDefinedInExtraPropertiesInBuildscript() {
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
              """,
            spec -> spec.afterRecipe(cu ->
                //noinspection DataFlowIssue
                assertThat(cu.getMarkers().findFirst(GradleProject.class))
                  .get()
                  .asInstanceOf(type(GradleProject.class))
                  .extracting(gp -> gp.getBuildscript().getConfigurations())
                  .asInstanceOf(list(GradleDependencyConfiguration.class))
                  .singleElement()
                  .extracting(conf -> conf.findResolvedDependency("com.google.guava", "guava"))
                  .isNotNull()
                  .extracting(ResolvedDependency::getVersion)
                  .as("GradleProject model should reflect the updated guava version")
                  .isEqualTo("30.1.1-jre"))
          )
        );
    }

    @Test
    void upgradesMultiModuleVariablesDefinedInExtraPropertiesInBuildscript() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'test'
              include 'module1'
              include 'module2'
              """
          ),
          buildGradle(
            """
              buildscript {
                  ext {
                      guavaVersion = "29.0-jre"
                  }
              }

              repositories {
                  mavenCentral()
              }

              subprojects {
                  apply plugin: 'java'

                  repositories {
                      mavenCentral()
                  }
              }
              """,
            """
              buildscript {
                  ext {
                      guavaVersion = "30.1.1-jre"
                  }
              }

              repositories {
                  mavenCentral()
              }

              subprojects {
                  apply plugin: 'java'

                  repositories {
                      mavenCentral()
                  }
              }
              """
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'com.google.guava:guava:' + guavaVersion
              }
              """,
            spec -> spec.path("module1/build.gradle")
          ), buildGradle(
            """
              dependencies {
                  implementation 'com.google.guava:guava:' + guavaVersion
              }
              """,
            spec -> spec.path("module2/build.gradle")
          )
        );
    }

    @Test
    void upgradesVariablesDefinedInExtraPropertiesAlsoInBuildscript() {
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

                  apply plugin: "java-library"

                  dependencies {
                    implementation ("com.google.guava:guava:$guavaVersion")
                  }
              }
              """,
            spec -> spec.path("build.gradle")
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              include("moduleA")
              """
          ),
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """,
            spec -> spec.path("moduleA/build.gradle")
          )
        );
    }

    @Test
    void versionInParentPropertiesFiles() {
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
              """,
            spec -> spec.path("build.gradle")
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              include("moduleA")
              """
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
    void versionInParentUsedInChildModules() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              ext {
                guavaVersion = "29.0-jre"
              }

              repositories {
                mavenCentral()
              }
              """,
              """
              plugins {
                id 'java-library'
              }

              ext {
                guavaVersion = "30.1.1-jre"
              }

              repositories {
                mavenCentral()
              }
              """,
            spec -> spec.path("build.gradle")
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              include("moduleA")
              """
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
                implementation "com.google.guava:guava:${guavaVersion}"
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
    void retainLatestReleaseOrLatestIntegrationIfUsed() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.projectlombok", "lombok", "1.18.*", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.projectlombok:lombok:latest.release")
                  testImplementation("org.projectlombok:lombok:latest.integration")
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

              repositories {
                  mavenCentral()
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

              repositories {
                  mavenCentral()
              }

              dependencies {
                  swaggerCodegen "org.openapitools:openapi-generator-cli:5.2.1"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4275")
    @Test
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

    @Issue("https://github.com/openrewrite/rewrite-java-dependencies/pull/106")
    @Test
    void isAcceptable() {
        // Mimic org.openrewrite.java.dependencies.UpgradeTransitiveDependencyVersion#getVisitor
        UpgradeDependencyVersion guava = new UpgradeDependencyVersion("com.google.guava", "guava", "30.x", "-jre");
        TreeVisitor<?, ExecutionContext> visitor = guava.getVisitor();

        SourceFile sourceFile = PlainTextParser.builder().build().parse("not a gradle file").findFirst().orElseThrow().withSourcePath(Path.of("not-a-gradle-file.txt"));
        assertThat(visitor.isAcceptable(sourceFile, new InMemoryExecutionContext())).isFalse();

        sourceFile = PropertiesParser.builder().build().parse("guavaVersion=29.0-jre").findFirst().orElseThrow();
        assertThat(visitor.isAcceptable(sourceFile, new InMemoryExecutionContext())).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4333")
    @Test
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

    @Issue("https://github.com/openrewrite/rewrite/issues/4333")
    @Test
    void exactVersionWithRegexPattern() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "32.1.1", "-.*?droid")),
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
    void upgradesDependencyVersionDefinedInJvmTestSuite() {
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
                              implementation 'com.google.guava:guava:29.0-jre'
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
                              implementation 'com.google.guava:guava:30.1.1-jre'
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void removesTransitiveDependenciesFromGradleProjectMarker() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("io.vertx", "vertx-core", "5.0.1", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'io.vertx:vertx-core:3.9.8'
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
                  implementation 'io.vertx:vertx-core:5.0.1'
              }
              """,
            spec -> spec.afterRecipe(after -> {
                Optional<GradleProject> maybeGp = after.getMarkers().findFirst(GradleProject.class);
                assertThat(maybeGp).isPresent();
                GradleDependencyConfiguration compileClasspath = maybeGp.get().getConfiguration("compileClasspath");
                assertThat(compileClasspath).isNotNull();

                // Check that vertx-core is updated to 5.0.1
                assertThat(compileClasspath.getResolved())
                  .as("GradleProject resolved dependencies should have vertx-core 5.0.1")
                  .anySatisfy(dep -> {
                      assertThat(dep.getGroupId()).isEqualTo("io.vertx");
                      assertThat(dep.getArtifactId()).isEqualTo("vertx-core");
                      assertThat(dep.getVersion()).isEqualTo("5.0.1");
                  });

                // Check that netty-codec is NOT listed amongst the dependencies as it is not a dependency of vertx-core 5.0.1
                assertThat(compileClasspath.getResolved())
                  .as("GradleProject resolved dependencies should NOT contain netty-codec after upgrade to vertx-core 5.0.1")
                  .noneMatch(dep -> "io.netty".equals(dep.getGroupId()) && "netty-codec".equals(dep.getArtifactId()));
            })
          )
        );
    }

    @Test
    void dependenciesBlockInFreestandingScript() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.fasterxml.jackson.core", "jackson-databind", "2.17.0-2.17.2", null)),
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
                  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
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
                  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
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

    @Issue("https://github.com/openrewrite/rewrite/issues/4655")
    @Test
    void issue4655() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              version='ORC-246-1-SNAPSHOT'
              dependencies {
                implementation "com.veon.eurasia.oraculum:jira-api:$version"
              }
              """
          )
        );
    }

    @Test
    void cannotDownloadMetaDataWhenNoRepositoriesAreDefined() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              dependencies {
                implementation "com.google.guava:guava:29.0-jre"
              }
              """,
            """
              plugins {
                id 'java-library'
              }

              dependencies {
                /*~~(com.google.guava:guava failed. Unable to download metadata.)~~>*/implementation "com.google.guava:guava:29.0-jre"
              }
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
                  implementation("com.google.guava:guava:30.1.1-jre")
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
                  implementation(group = "com.google.guava", name = "guava", version = "29.0-jre")
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
                  implementation(group = "com.google.guava", name = "guava", version = "30.1.1-jre")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslVariable() {
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
                  val guavaVersion = "29.0-jre"
                  implementation(group = "com.google.guava", name = "guava", version = guavaVersion)
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
                  val guavaVersion = "30.1.1-jre"
                  implementation(group = "com.google.guava", name = "guava", version = guavaVersion)
              }
              """
          )
        );
    }

    @Test
    void kotlinDslStringInterpolation() {
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
                  val guavaVersion = "29.0-jre"
                  implementation("com.google.guava:guava:${guavaVersion}")
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
                  val guavaVersion = "30.1.1-jre"
                  implementation("com.google.guava:guava:${guavaVersion}")
              }
              """
          )
        );
    }

    @Test
    void shellCommandsWithEscapes() {
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

              task executeShellCommands {
                  doLast {
                      exec {
                          commandLine 'bash', '-c', \"""
                              RESPONSE=\\$(curl --location -s --request POST "https://localhost")
                              echo "TEST" > "$someVar"
                          \"""
                      }
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
                compileOnly 'com.google.guava:guava:30.1.1-jre'
                runtimeOnly ('com.google.guava:guava:30.1.1-jre')
              }

              task executeShellCommands {
                  doLast {
                      exec {
                          commandLine 'bash', '-c', \"""
                              RESPONSE=\\$(curl --location -s --request POST "https://localhost")
                              echo "TEST" > "$someVar"
                          \"""
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void updateVersionDefinedInExtraPropertiesWithInlineReferenceInDependenciesGradle() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation "com.google.guava:guava:${guavaVersion}"
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              buildscript {
                  ext {
                      guavaVersion = "29.0-jre"
                  }
              }

              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencies.gradle'
              """,
            """
              buildscript {
                  ext {
                      guavaVersion = "30.1.1-jre"
                  }
              }

              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencies.gradle'
              """
          )
        );
    }

    @Test
    void updateVersionDefinedInExtraPropertiesWithConcatenatedReferenceInDependenciesGradle() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation 'com.google.guava:guava:' + guavaVersion
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              buildscript {
                  ext {
                      guavaVersion = "29.0-jre"
                  }
              }

              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencies.gradle'
              """,
            """
              buildscript {
                  ext {
                      guavaVersion = "30.1.1-jre"
                  }
              }

              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencies.gradle'
              """
          )
        );
    }

    @Test
    void ignoreVersionDefinedInExtraPropertiesWithConcatenatedReferenceInDependenciesGradleWhenConcatenationLeftIsNotADependency() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation 'com.google.guava:' + 'guava:' + guavaVersion
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              buildscript {
                  ext {
                      guavaVersion = "29.0-jre"
                  }
              }

              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencies.gradle'
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "\"com.fasterxml.jackson.core:jackson-databind:${gradle.jackson}\"",
      "group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: gradle.jackson",
      "group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: \"$gradle.jackson\"",
      "group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: \"${gradle.jackson}\""
    })
    void upgradeVersionInSettingsGradleExt(String dependencyNotation) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.fasterxml.jackson.core", "jackson-databind", "2.15.0", null)),
          settingsGradle(
            """
              gradle.ext {
                  jackson = '2.13.3'
              }
              """,
            """
              gradle.ext {
                  jackson = '2.15.0'
              }
              """
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
                  implementation %s
              }
              """.formatted(dependencyNotation)
          )
        );
    }

    @Test
    void doesNotDowngradeRegularDependencyVersion() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("org.apache.tomcat.embed", "tomcat-embed-core", "10.1.33", null)),
          //language=groovy
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.43'
              }
              """
          )
        );
    }

    @Test
    void doesNotDowngradeBuildscriptDependencyVersion() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "29.0-jre", null)),
          //language=groovy
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath("com.google.guava:guava:30.1.1-jre")
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotDowngradeRegularDependencyVersionInSettingsGradleExt() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.fasterxml.jackson.core", "jackson-databind", "2.13.2", null)),
          settingsGradle(
            """
              gradle.ext {
                  jackson = '2.13.3'
              }
              """
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
                  implementation "com.fasterxml.jackson.core:jackson-databind:${gradle.jackson}"
              }
              """
          )
        );
    }

    @Test
    void doesNotDowngradeBuildscriptDependencyVersionInSettingsGradleExt() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.fasterxml.jackson.core", "jackson-databind", "2.13.2", null)),
          settingsGradle(
            """
              gradle.ext {
                  jackson = '2.13.3'
              }
              """
          ),
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath "com.fasterxml.jackson.core:jackson-databind:${gradle.jackson}"
                  }
              }
              """
          )
        );
    }
}
