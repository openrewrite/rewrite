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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;

class RemoveExtensionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExtension("buildCache"));
    }

    @Test
    void removeBuildCacheFromSettingsGradle() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-project'
              
              buildCache {
                  local {
                      enabled = true
                  }
                  remote(HttpBuildCache) {
                      url = "https://example.com/cache"
                      push = true
                  }
              }
              
              include 'subproject'
              """,
            """
              rootProject.name = 'my-project'
              
              include 'subproject'
              """
          )
        );
    }

    @Test
    void removeBuildCacheFromSettingsGradleKts() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = "my-project"
              
              buildCache {
                  local {
                      isEnabled = true
                  }
                  remote<HttpBuildCache> {
                      url = uri("https://example.com/cache")
                      isPush = true
                  }
              }
              
              include("subproject")
              """,
            """
              rootProject.name = "my-project"
              
              include("subproject")
              """,
            spec -> spec.path("settings.gradle.kts")
          )
        );
    }
}
