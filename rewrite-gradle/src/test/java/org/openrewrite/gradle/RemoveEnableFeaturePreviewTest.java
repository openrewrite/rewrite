/*
 * Copyright 2024 the original author or authors.
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

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class RemoveEnableFeaturePreviewTest implements RewriteTest {

    @Test
    void testFail() {
        org.junit.jupiter.api.Assertions.assertTrue(false,
          "Tested this recipe manually by publishing to maven local and used it in a local project, "
            + "doesn't seem to be working, so failing the build for now so it won't get merged yet");
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_singleQuotes() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
                enableFeaturePreview('ONE_LOCKFILE_PER_PROJECT')
              """,
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_doubleQuotes() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
                enableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")
                              
                include 'service'
              """,
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_noChange() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                
                enableFeaturePreview("DIFFERENT_FEATURE")
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_noChangeNoArgument() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                
                enableFeaturePreview()
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_noChangeNullArgument() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                
                enableFeaturePreview(null)
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

}
