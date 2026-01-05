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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class AddSettingsPluginTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new AddSettingsPlugin("com.gradle.enterprise", "3.11.4", null, null, null));
    }

    @Test
    void resolvePluginVersion() {
        rewriteRun(
          settingsGradle(
            "",
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.11.4'
              }
              """
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
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.11.4'
              }
              
              rootProject.name = 'my-project'
              """
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
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.11.4'
              }
              
              rootProject.name = 'my-project'
              """
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
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              
              plugins {
                  id 'com.gradle.enterprise' version '3.11.4'
              }
              
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void addPluginApplyFalse() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new AddSettingsPlugin("com.gradle.enterprise", "3.11.x", null, false, null)),
          settingsGradle(
            "",
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.11.4' apply false
              }
              """
          )
        );
    }

    @Test
    void addPluginWithPluginManagementAndBuildscriptBlocks() {
        rewriteRun(
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }
              
              rootProject.name = 'my-project'
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }
              
              plugins {
                  id 'com.gradle.enterprise' version '3.11.4'
              }
              
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void addPluginWithPluginManagementAndBuildscriptBlocksKotlin() {
        rewriteRun(
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }
              
              rootProject.name = "my-project"
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }
              
              plugins {
                  id("com.gradle.enterprise") version "3.11.4"
              }
              
              rootProject.name = "my-project"
              """
          )
        );
    }
}
