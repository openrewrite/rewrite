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
import org.openrewrite.groovy.tree.G.CompilationUnit;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.test.SourceSpecs.dir;

class AddDevelocityGradlePluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new AddDevelocityGradlePlugin("3.x", null, null, null, null, null));
    }

    private static Consumer<SourceSpec<CompilationUnit>> interpolateResolvedVersion(@Language("groovy") String after) {
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
                  id 'com.gradle.enterprise' version '%s'
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
                  id 'com.gradle.enterprise' version '%s'
              }
                            
              rootProject.name = 'my-project'
              """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2697")
    @Test
    void withConfigurationInSettings() {
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
                Matcher versionMatcher = Pattern.compile("id 'com\\.gradle\\.enterprise' version '(.*?)'").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                VersionComparator versionComparator = requireNonNull(Semver.validate("[3.14,)", null).getValue());
                assertThat(versionComparator.compare(null, "3.14", version)).isLessThanOrEqualTo(0);

                return """
                  plugins {
                      id 'com.gradle.enterprise' version '%s'
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
                  id 'com.gradle.enterprise' version '%s'
              }
                            
              rootProject.name = 'my-project'
              """
            )
          )
        );
    }
}
