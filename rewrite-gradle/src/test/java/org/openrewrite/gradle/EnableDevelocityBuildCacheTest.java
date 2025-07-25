/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;

class EnableDevelocityBuildCacheTest implements RewriteTest {

    @DocumentExample
    @Test
    void addBuildCacheRemoteConfig() {
        rewriteRun(spec -> spec.recipe(new EnableDevelocityBuildCache("true", "System.getenv(\"CI\") != null")),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
                  buildCache {
                      remote(develocity.buildCache) {
                          enabled = true
                          push = System.getenv("CI") != null
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addBuildCacheRemoteConfigExtendedConfig() {
        rewriteRun(spec -> spec.recipe(new EnableDevelocityBuildCache("true", "System.getenv(\"CI\") != null")),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
                  buildScan {
                      uploadInBackground = true
                  }
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
                  buildScan {
                      uploadInBackground = true
                  }
                  buildCache {
                      remote(develocity.buildCache) {
                          enabled = true
                          push = System.getenv("CI") != null
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addBuildCacheRemoteConfigWithOnlyPush() {
        rewriteRun(spec -> spec.recipe(new EnableDevelocityBuildCache(null, "true")),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
                  buildCache {
                      remote(develocity.buildCache) {
                          push = true
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotModifyBuildCacheConfig() {
        rewriteRun(spec -> spec.recipe(new EnableDevelocityBuildCache(null, "#{isTrue(env['CI'])}")),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://dev.example.com/'
                  buildCache {
                      remote(develocity.buildCache) {
                          push = false
                      }
                  }
              }
              """
          )
        );
    }
}
