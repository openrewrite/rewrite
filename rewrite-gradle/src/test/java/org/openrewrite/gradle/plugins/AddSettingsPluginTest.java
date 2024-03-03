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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class AddSettingsPluginTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new AddSettingsPlugin("com.gradle.enterprise", "3.11.x", null));
    }

    @Test
    void addPluginToEmptyFile() {
        rewriteRun(
          settingsGradle(
            "",
            interpolateResolvedVersion(
              """
                plugins {
                    id 'com.gradle.enterprise' version '%s'
                }
                """
            )
          )
        );
    }

    @Test
    void addPluginToNewBlock() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion(
              """
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
    void addPluginToExistingBlock() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
              }
                            
              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion(
              """
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
    void addPluginWithPluginManagementBlock() {
        rewriteRun(
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = 'my-project'
              """,
            interpolateResolvedVersion(
              """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                plugins {
                    id 'com.gradle.enterprise' version '%s'
                }

                rootProject.name = 'my-project'
                """
            )
          )
        );
    }

    private static Consumer<SourceSpec<G.CompilationUnit>> interpolateResolvedVersion(@Language("groovy") String after) {
        return spec -> spec.after(actual -> {
            assertThat(actual).isNotNull();
            Matcher version = Pattern.compile("3\\.\\d+(\\.\\d+)?").matcher(actual);
            assertThat(version.find()).isTrue();
            return after.formatted(version.group(0));
        });
    }
}
