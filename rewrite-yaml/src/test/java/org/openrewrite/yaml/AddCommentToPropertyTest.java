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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class AddCommentToPropertyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddCommentToProperty("management.metrics.enabled", "This property is deprecated", null, null));
    }

    @DocumentExample
    @Test
    void addCommentToNestedProperty() {
        rewriteRun(
          yaml(
            """
              management:
                metrics:
                  enabled: true
              """,
            """
              management:
                metrics:
                  # This property is deprecated
                  enabled: true
              """
          )
        );
    }

    @Test
    void addCommentToDotNotationProperty() {
        rewriteRun(
          yaml(
            """
              management.metrics.enabled: true
              server.port: 8080
              """,
            """
              # This property is deprecated
              management.metrics.enabled: true
              server.port: 8080
              """
          )
        );
    }

    @Test
    void addCommentToMiddleProperty() {
        rewriteRun(
          yaml(
            """
              server.port: 8080
              management.metrics.enabled: true
              app.name: test
              """,
            """
              server.port: 8080
              # This property is deprecated
              management.metrics.enabled: true
              app.name: test
              """
          )
        );
    }

    @Test
    void doNotAddDuplicateComment() {
        rewriteRun(
          yaml(
            """
              management:
                metrics:
                  # This property is deprecated
                  enabled: true
              """
          )
        );
    }

    @Test
    void globPatternMatchesMultipleProperties() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToProperty("management.metrics.*.enabled", "Deprecated", null, null)),
          yaml(
            """
              management:
                metrics:
                  files:
                    enabled: true
                  jvm:
                    enabled: false
              """,
            """
              management:
                metrics:
                  files:
                    # Deprecated
                    enabled: true
                  jvm:
                    # Deprecated
                    enabled: false
              """
          )
        );
    }

    @Test
    void relaxedBindingMatchesVariants() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToProperty("acme.my-project.enabled", "Check this setting", true, null)),
          yaml(
            """
              acme:
                myProject:
                  enabled: true
              """,
            """
              acme:
                myProject:
                  # Check this setting
                  enabled: true
              """
          )
        );
    }

    @Test
    void exactMatchDoesNotMatchVariants() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToProperty("acme.my-project.enabled", "Check this setting", false, null)),
          yaml(
            """
              acme:
                myProject:
                  enabled: true
              """
          )
        );
    }

    @Test
    void filePatternFiltersFiles() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToProperty("server.port", "Check port", null, "**/application.yml")),
          yaml(
            "server.port: 8080",
            """
              # Check port
              server.port: 8080
              """,
            spec -> spec.path("src/main/resources/application.yml")
          ),
          yaml(
            "server.port: 8080",
            spec -> spec.path("src/main/resources/other.yml")
          )
        );
    }
}
