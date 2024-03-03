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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

public class ChangePluginVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @Test
    void change() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginVersion("org.openrewrite.rewrite", "5.x", null)),
          settingsGradle(
            """
              pluginManagement {
                  plugins {
                      String v = '5.40.0'
                      id 'org.openrewrite.rewrite' version v
                  }
              }
              """,
            """
              pluginManagement {
                  plugins {
                      String v = '5.40.0'
                      id 'org.openrewrite.rewrite' version '5.40.6'
                  }
              }
              """
          )
        );
    }
}
