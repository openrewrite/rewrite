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
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.gradle.Assertions.settingsGradle;
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
              
              
              buildCache {
                  local {
                      enabled = true
                      directory = file('build-cache')
                  }
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void removeEntireBuildCacheWhenOnlyRemoteExists() {
        rewriteRun(
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
            ""
          )
        );
    }

    @Test
    void removeGradleEnterpriseConfiguration() {
        rewriteRun(
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
              
              
              buildCache {
                  local {
                      enabled = true
                  }
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void noChangesWhenNoRemoteCache() {
        rewriteRun(
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


              buildCache {
                  remote(HttpBuildCache) {
                      url = 'https://cache.example.com'
                  }
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void removeOnlyExtensionsWhenNoBuildCacheBlock() {
        rewriteRun(
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
            ""
          )
        );
    }

    @Test
    void preserveOtherExtensionsAndPlugins() {
        rewriteRun(
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
