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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class RemoveDevelocityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .recipeFromResource("/META-INF/rewrite/gradle.yml", "org.openrewrite.gradle.plugins.RemoveDevelocity");
    }

    @DocumentExample
    @Test
    void removeGradleEnterprise() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.16'
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
                  buildCache {
                      remote(gradleEnterprise.buildCache) {
                          enabled = true
                          push = System.getenv("CI") != null
                      }
                  }
              }
              """,
            ""
          )
        );
    }

    @Test
    void removeDevelocity() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://ge.sam.com/'
                  allowUntrustedServer = true
                  buildScan {
                      uploadInBackground = true
                      capture {
                          fileFingerprints = true
                      }
                  }
                  buildCache {
                      remote(develocity.buildCache) {
                          enabled = true
                          push = System.getenv("CI") != null
                      }
                  }
              }
              """,
            ""
          )
        );
    }

    @Test
    void removeRemoteCacheWithLocalPreserved() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                develocity {
                    server = 'https://ge.example.com'
                }
                
                buildCache {
                    local {
                        enabled = true
                        directory = file('build-cache')
                    }
                    remote(develocity.buildCache) {
                        enabled = true
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                buildCache {
                    local {
                        enabled = true
                        directory = file('build-cache')
                    }
                }
                """
            )
        );
    }

    @Test
    void removeEntireBuildCacheWhenOnlyRemoteExists() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                develocity {
                    server = 'https://ge.example.com'
                }
                
                buildCache {
                    remote(develocity.buildCache) {
                        enabled = true
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                """
            )
        );
    }

    @Test
    void removeHttpBuildCache() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                develocity {
                    server = 'https://ge.example.com'
                }
                
                buildCache {
                    remote(HttpBuildCache) {
                        url = 'https://other-cache.com/cache/'
                    }
                    remote(develocity.buildCache) {
                        enabled = true
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                """
            )
        );
    }

    @Test
    void removeAllRemoteConfigurations() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                develocity {
                    server = 'https://ge.example.com'
                    buildScan {
                        termsOfUseUrl = 'https://gradle.com/terms-of-service'
                        termsOfUseAgree = 'yes'
                    }
                }
                
                buildCache {
                    local {
                        enabled = true
                    }
                    remote(HttpBuildCache) {
                        url = 'https://cache.com'
                    }
                    remote(develocity.buildCache) {
                        push = true
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                buildCache {
                    local {
                        enabled = true
                    }
                }
                """
            )
        );
    }

    @Test
    void removeGradleEnterpriseConfiguration() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.enterprise' version '3.16.2'
                }
                
                gradleEnterprise {
                    server = 'https://ge.example.com'
                    buildScan {
                        publishAlways()
                    }
                }
                
                buildCache {
                    local {
                        enabled = true
                    }
                    remote(gradleEnterprise.buildCache) {
                        enabled = true
                        push = true
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.enterprise' version '3.16.2'
                }
                
                buildCache {
                    local {
                        enabled = true
                    }
                }
                """
            )
        );
    }

    @Test
    void noChangesWhenNoRemoteCache() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """                
                buildCache {
                    local {
                        enabled = true
                    }
                }
                """
            )
        );
    }

    @Test
    void removeEntireBuildCacheWithOnlyHttpBuildCache() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                develocity {
                    server = 'https://ge.example.com'
                }
                
                buildCache {
                    remote(HttpBuildCache) {
                        url = 'https://cache.example.com'
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                """
            )
        );
    }

    @Test
    void removeOnlyExtensionsWhenNoBuildCacheBlock() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                
                develocity {
                    server = 'https://ge.example.com'
                    buildScan {
                        termsOfUseUrl = 'https://gradle.com/terms-of-service'
                        termsOfUseAgree = 'yes'
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                }
                """
            )
        );
    }

    @Test
    void preserveOtherExtensionsAndPlugins() {
        rewriteRun(
            spec -> spec.recipe(new RemoveDevelocityConfiguration()),
            settingsGradle(
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
                }
                
                rootProject.name = 'my-project'
                
                develocity {
                    server = 'https://ge.example.com'
                }
                
                buildCache {
                    local {
                        enabled = true
                    }
                    remote(develocity.buildCache) {
                        enabled = true
                    }
                }
                """,
                """
                plugins {
                    id 'com.gradle.develocity' version '3.17'
                    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
                }
                
                rootProject.name = 'my-project'
                
                buildCache {
                    local {
                        enabled = true
                    }
                }
                """
            )
        );
    }
}
