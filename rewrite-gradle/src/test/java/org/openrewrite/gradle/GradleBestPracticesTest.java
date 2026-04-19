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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.properties.Assertions.properties;

class GradleBestPracticesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.gradle.GradleBestPractices");
    }

    @Test
    void addsBuildCacheAndParallelProperties() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addsMissingParallelWhenCachingAlreadyEnabled() {
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
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addsMissingCachingWhenParallelAlreadyEnabled() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.parallel=true
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeWhenBothAlreadyEnabled() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeWhenPropertiesAlreadySetToFalse() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.caching=false
              org.gradle.parallel=false
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void preservesExistingProperties() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.jvmargs=-Xmx2g
              project.name=myproject
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.jvmargs=-Xmx2g
              org.gradle.parallel=true
              project.name=myproject
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }
}
