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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.dir;

class AddPropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddProperty("org.gradle.caching", "true", true, null));
    }

    @Test
    void addToExistingPropertiesFile() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              project.name=helloworld
              """,
            //language=properties
            """
              org.gradle.caching=true
              project.name=helloworld
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void overwriteExistingProperty() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              project.name=helloworld
              org.gradle.caching=false
              """,
            //language=properties
            """
              project.name=helloworld
              org.gradle.caching=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addToNewPropertiesFile() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            null,
            //language=properties
            """
              org.gradle.caching=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addOnlyToSpecifiedFilePattern() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("org.gradle.caching", "true", null, "gradle.properties")),
          buildGradle("plugins { id 'java' }"),
          properties(
            "",
            //language=properties
            """
              org.gradle.caching=true
              """,
            spec -> spec.path("gradle.properties")
          ),
          dir("project1",
            properties(
              "",
              spec -> spec.path("gradle.properties")
            )
          ),
          dir("project2",
            properties(
              "",
              spec -> spec.path("gradle.properties")
            )
          )
        );
    }
}
