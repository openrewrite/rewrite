/*
 * Copyright 2023 the original author or authors.
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

class RemoveSettingsPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveSettingsPlugin("com.gradle.enterprise"));
    }

    @DocumentExample
    @Test
    void removePlugin() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id "com.gradle.enterprise" version "3.12.0"
              }
              """,
            ""
          )
        );
    }

    @Test
    void removePluginKts() {
        rewriteRun(
          settingsGradleKts(
            """
              plugins {
                  id("com.gradle.enterprise") version "3.12.0"
              }
              """,
            ""
          )
        );
    }
}
