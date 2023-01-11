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
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class AddSettingsPluginTest implements RewriteTest {

    @Test
    void addPluginToNewBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPlugin("com.gradle.enterprise", "3.11.x", null)),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3.11(.\\d+)?").matcher(actual);
                assertThat(version.find()).isTrue();
                //language=groovy
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
    void addPluginToExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPlugin("com.gradle.enterprise", "3.11.x", null)),
          settingsGradle(
            """
              plugins {
                  id 'org.openrewrite' version '1'
              }
                            
              rootProject.name = 'my-project'
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3.11.\\d+").matcher(actual);
                assertThat(version.find()).isTrue();
                //language=groovy
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

    @Test
    void addPluginWithPluginManagementBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPlugin("com.gradle.enterprise", "3.11.x", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = 'my-project'
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3.11.\\d+").matcher(actual);
                assertThat(version.find()).isTrue();
                //language=groovy
                return """
                  pluginManagement {
                      repositories {
                          gradlePluginPortal()
                      }
                  }

                  plugins {
                      id 'com.gradle.enterprise' version '%s'
                  }

                  rootProject.name = 'my-project'
                  """.formatted(version.group(0));
            })
          )
        );
    }
}
