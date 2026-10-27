/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

class EnableGradleConfigurationCacheTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
                .recipeFromResources("org.openrewrite.gradle.EnableGradleConfigurationCache");
    }

    @DocumentExample("Enable the configuration cache")
    @Test
    void enablesConfigurationCache() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.caching=true
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.configuration-cache=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeWhenAlreadyEnabled() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.configuration-cache=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }
}
