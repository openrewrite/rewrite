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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class AddGradleEnterpriseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddGradleEnterprise("3.x"));
    }

    @Test
    void addNewBuildPluginsBlock() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))),
          buildGradle(
            "",
            spec -> spec.after(actual -> {
                  assertThat(actual).isNotNull();
                  Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
                  assertThat(version.find()).isTrue();
                  return """
                    plugins {
                        id 'com.gradle.build-scan' version '%s'
                    }
                    """.formatted(version.group(0));
              })
          ),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    @Disabled("Need to be able to specify Gradle wrapper to generate tooling model for")
    void addExistingBuildPluginsBlock() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              """,
            spec -> spec.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "5.6.1"))
              .after(actual -> {
                  assertThat(actual).isNotNull();
                  Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
                  assertThat(version.find()).isTrue();
                  return """
                    plugins {
                        id("java")
                        id("com.gradle.build-scan") version "%s"
                    }
                    """.formatted(version.group(0));
              })
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
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
                assertThat(version.find()).isTrue();
                return """
                  plugins {
                      id 'com.gradle.enterprise' version '%s'
                  }

                  rootProject.name = 'my-project'
                  """.formatted(version.group(0));
            })
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
                  id 'org.openrewrite' version '1'
              }
                            
              rootProject.name = 'my-project'
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
                assertThat(version.find()).isTrue();
                return """
                  plugins {
                      id 'org.openrewrite' version '1'
                      id 'com.gradle.enterprise' version '%s'
                  }
                                
                  rootProject.name = 'my-project'
                  """.formatted(version.group(0));
            })
          )
        );
    }
}
