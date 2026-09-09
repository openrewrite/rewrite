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
package org.openrewrite.gradle.plugins;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.dir;

class AddDevelocityGradlePluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new AddDevelocityGradlePlugin("3.x", null, null, null, null, null));
    }

    private static Consumer<SourceSpec<G.CompilationUnit>> interpolateResolvedVersion(@Language("groovy") String after) {
        return spec -> spec.after(actual -> {
            assertThat(actual).isNotNull();
            Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
            assertThat(version.find()).isTrue();
            return after.formatted(version.group(0));
        });
    }

    private static SourceSpecs wrapperProperties(String gradleVersion) {
        return properties(
          """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            """.formatted(gradleVersion),
          spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
        );
    }

    private static Consumer<SourceSpec<K.CompilationUnit>> interpolateResolvedVersionKts(@Language("kotlin") String after) {
        return spec -> spec.after(actual -> {
            assertThat(actual).isNotNull();
            Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
            assertThat(version.find()).isTrue();
            return after.formatted(version.group(0));
        });
    }

    @Test
    void onlyChangeRootBuildGradle() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))),
          buildGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.build-scan' version '%s'
              }
              """
            )
          ),
          dir("subproject", buildGradle("")),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              include("subproject")
              """
          )
        );
    }

    @Test
    void addNewBuildPluginsBlock() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))),
          buildGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.build-scan' version '%s'
              }
              """
            )
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void addExistingBuildPluginsBlock() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """,
            interpolateResolvedVersion("""
              plugins {
                  id "java"
                  id "com.gradle.build-scan" version "%s"
              }
              """
            )
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void addNewSettingsPluginsBlock() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1"))),
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.develocity' version '%s'
              }

              rootProject.name = 'my-project'
              """
            )
          )
        );
    }

    @Test
    void addExistingSettingsPluginsBlock() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1"))),
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              plugins {
              }

              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.develocity' version '%s'
              }

              rootProject.name = 'my-project'
              """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2697")
    @Test
    void withGradleEnterpriseConfigurationInSettings() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin("3.16.x", "https://ge.sam.com/", true, true, true, AddDevelocityGradlePlugin.PublishCriteria.Always)),
          buildGradle(
            ""
          ),
          settingsGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.enterprise' version '%s'
              }
              gradleEnterprise {
                  server = 'https://ge.sam.com/'
                  allowUntrustedServer = true
                  buildScan {
                      publishAlways()
                      uploadInBackground = true
                      capture {
                          taskInputFiles = true
                      }
                  }
              }
              """
            )
          )
        );
    }

    @Test
    void withDevelocityConfigurationInSettings() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin("3.x", "https://ge.sam.com/", true, true, true, AddDevelocityGradlePlugin.PublishCriteria.Always)),
          buildGradle(
            ""
          ),
          settingsGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.develocity' version '%s'
              }
              develocity {
                  server = 'https://ge.sam.com/'
                  allowUntrustedServer = true
                  buildScan {
                      publishing.onlyIf { true }
                      uploadInBackground = true
                      capture {
                          fileFingerprints = true
                      }
                  }
              }
              """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2697")
    @Test
    void withConfigurationOldInputCapture() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin("3.6", null, null, true, null, null)),
          buildGradle(
            ""
          ),
          settingsGradle(
            "",
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.6'
              }
              gradleEnterprise {
                  buildScan {
                      captureTaskInputFiles = true
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultsToLatestRelease() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin(null, null, null, null, null, null)),
          buildGradle(
            ""
          ),
          settingsGradle(
            "",
            spec -> spec.after(after -> {
                Matcher versionMatcher = Pattern.compile("id 'com\\.gradle\\.develocity' version '(.*?)'").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                VersionComparator versionComparator = requireNonNull(Semver.validate("[3.14,)", null).getValue());
                assertThat(versionComparator.compare(null, "3.14", version)).isLessThanOrEqualTo(0);

                return """
                  plugins {
                      id 'com.gradle.develocity' version '%s'
                  }
                  """.formatted(version);
            })
          )
        );
    }

    @Test
    void addNewSettingsPluginsBlockWithLicenseHeader() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1"))),
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              /*
               * Licensed to...
               */

              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion("""
              /*
               * Licensed to...
               */

              plugins {
                  id 'com.gradle.develocity' version '%s'
              }

              rootProject.name = 'my-project'
              """
            )
          )
        );
    }

    @Test
    void settingsPluginsBlockUsingWrapperPropertiesWithoutBuildToolMarker() {
        rewriteRun(
          wrapperProperties("7.6.1"),
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.develocity' version '%s'
              }

              rootProject.name = 'my-project'
              """
            )
          )
        );
    }

    @Test
    void buildPluginsBlockUsingWrapperPropertiesWithoutBuildToolMarker() {
        rewriteRun(
          wrapperProperties("5.6.1"),
          buildGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.build-scan' version '%s'
              }
              """
            )
          ),
          dir("subproject", buildGradle("")),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              include("subproject")
              """
          )
        );
    }

    @Test
    void wrapperPropertiesUsedWhenBuildToolMarkerIsForAnotherBuildTool() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.ModerneCli, "3.44.0"))),
          wrapperProperties("7.6.1"),
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.develocity' version '%s'
              }

              rootProject.name = 'my-project'
              """
            )
          )
        );
    }

    @Test
    void buildToolMarkerWinsOverWrapperProperties() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))),
          // The marker says Gradle 5, so the plugin belongs in the root build.gradle even though the wrapper says 7
          wrapperProperties("7.6.1"),
          buildGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.build-scan' version '%s'
              }
              """
            )
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void nestedBuildUsesItsOwnWrapperRatherThanTheRootWrapper() {
        rewriteRun(
          // Gradle 5 at the root puts the plugin in the root build.gradle
          wrapperProperties("5.6.1"),
          buildGradle(
            "",
            interpolateResolvedVersion("""
              plugins {
                  id 'com.gradle.build-scan' version '%s'
              }
              """
            )
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          ),
          // Gradle 7 in the nested build puts it in that build's settings.gradle instead
          dir("nested",
            wrapperProperties("7.6.1"),
            buildGradle(""),
            settingsGradle(
              """
                rootProject.name = 'nested'
                """,
              interpolateResolvedVersion("""
                plugins {
                    id 'com.gradle.develocity' version '%s'
                }

                rootProject.name = 'nested'
                """
              )
            )
          )
        );
    }

    @Test
    void noBuildToolMarkerAndNoWrapperPropertiesMakesNoChanges() {
        rewriteRun(
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void distributionUrlWithoutExtractableVersionMakesNoChanges() {
        rewriteRun(
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.example/repo/gradle-nightly.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          buildGradle(
            ""
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void settingsPluginsBlockKtsUsingWrapperPropertiesWithoutBuildToolMarker() {
        rewriteRun(
          wrapperProperties("7.6.1"),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            """
              rootProject.name = "my-project"
              """,
            interpolateResolvedVersionKts("""
              plugins {
                  id("com.gradle.develocity") version "%s"
              }

              rootProject.name = "my-project"
              """
            )
          )
        );
    }

    @Test
    void addNewSettingsPluginsBlockKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1"))),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            """
              rootProject.name = "my-project"
              """,
            interpolateResolvedVersionKts("""
              plugins {
                  id("com.gradle.develocity") version "%s"
              }

              rootProject.name = "my-project"
              """
            )
          )
        );
    }

    @Test
    void addExistingSettingsPluginsBlockKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1"))),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            """
              plugins {
              }

              rootProject.name = "my-project"
              """,
            interpolateResolvedVersionKts("""
              plugins {
                  id("com.gradle.develocity") version "%s"
              }

              rootProject.name = "my-project"
              """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2697")
    @Test
    void withGradleEnterpriseConfigurationInSettingsKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin("3.16.x", "https://ge.sam.com/", true, true, true, AddDevelocityGradlePlugin.PublishCriteria.Always)),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            "",
            interpolateResolvedVersionKts("""
              plugins {
                  id("com.gradle.enterprise") version "%s"
              }
              gradleEnterprise {
                  server.set("https://ge.sam.com/")
                  allowUntrustedServer.set(true)
                  buildScan {
                      publishAlways()
                      uploadInBackground.set(true)
                      capture {
                          taskInputFiles.set(true)
                      }
                  }
              }
              """
            )
          )
        );
    }

    @Test
    void withDevelocityConfigurationInSettingsKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin("3.x", "https://ge.sam.com/", true, true, true, AddDevelocityGradlePlugin.PublishCriteria.Always)),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            "",
            interpolateResolvedVersionKts("""
              plugins {
                  id("com.gradle.develocity") version "%s"
              }
              develocity {
                  server.set("https://ge.sam.com/")
                  allowUntrustedServer.set(true)
                  buildScan {
                      publishing.onlyIf { true }
                      uploadInBackground.set(true)
                      capture {
                          fileFingerprints.set(true)
                      }
                  }
              }
              """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2697")
    @Test
    void withConfigurationOldInputCaptureKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin("3.6", null, null, true, null, null)),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            "",
            """
              plugins {
                  id("com.gradle.enterprise") version "3.6"
              }
              gradleEnterprise {
                  buildScan {
                      captureTaskInputFiles.set(true)
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultsToLatestReleaseKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1")))
            .recipe(new AddDevelocityGradlePlugin(null, null, null, null, null, null)),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            "",
            spec -> spec.after(after -> {
                Matcher versionMatcher = Pattern.compile("id\\(\"com\\.gradle\\.develocity\"\\) version \"(.*?)\"").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                VersionComparator versionComparator = requireNonNull(Semver.validate("[3.14,)", null).getValue());
                assertThat(versionComparator.compare(null, "3.14", version)).isLessThanOrEqualTo(0);

                return """
                  plugins {
                      id("com.gradle.develocity") version "%s"
                  }
                  """.formatted(version);
            })
          )
        );
    }

    @Test
    void addNewSettingsPluginsBlockWithLicenseHeaderKts() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "7.6.1"))),
          buildGradleKts(
            ""
          ),
          settingsGradleKts(
            """
              /*
               * Licensed to...
               */

              rootProject.name = "my-project"
              """,
            interpolateResolvedVersionKts("""
              /*
               * Licensed to...
               */

              plugins {
                  id("com.gradle.develocity") version "%s"
              }

              rootProject.name = "my-project"
              """
            )
          )
        );
    }
}
