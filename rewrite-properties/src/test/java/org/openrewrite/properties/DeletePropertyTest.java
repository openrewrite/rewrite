/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class DeletePropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DeleteProperty("delete.me", null, null));
    }

    @Test
    void basic() {
        rewriteRun(
          properties(
            """
              preserve = foo
              delete.me = baz
              delete.me.not = bar
              """,
            """
              preserve = foo
              delete.me.not = bar
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @ParameterizedTest
    @ValueSource(strings = {
      "acme.my-project.person.first-name",
      "acme.myProject.person.firstName",
      "acme.my_project.person.first_name",
    })
    void relaxedBinding(String propertyKey) {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty(propertyKey, true, null)),
          properties(
            """
              spring.datasource.schema=classpath*:db/database/schema.sql
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            "spring.datasource.schema=classpath*:db/database/schema.sql"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @Test
    void exactMatch() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("acme.my-project.person.first-name", false, null)),
          properties(
            """
              spring.datasource.schema=classpath*:db/database/schema.sql
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              spring.datasource.schema=classpath*:db/database/schema.sql
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @Test
    void updatePrefix() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("acme.my-project.person.first-name", false, null)),
          properties(
            """
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1776")
    @Test
    void matchesGlob() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("management.metrics.export.dynatrace.*", false, null)),
          properties(
            """
              management.metrics.export.dynatrace.api-token=YOUR_TOKEN
              management.metrics.export.dynatrace.device-id=YOUR_DEVICE_ID
              management.metrics.export.dynatrace.uri=YOUR_URI
              management.metrics.export.datadog.api-key=YOUR_KEY
              management.metrics.export.datadog.step=30s
              """,
            """
              management.metrics.export.datadog.api-key=YOUR_KEY
              management.metrics.export.datadog.step=30s
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1776")
    @ParameterizedTest
    @ValueSource(strings = {
      "acme.my-project.*.first-name",
      "acme.myProject.person.*"
    })
    void matchesGlobWithRelaxedBinding(String propertyKey) {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty(propertyKey, true, null)),
            properties(
              """
                acme.notMyProject.person=example
                acme.my-project.person.first-name=example
                acme.myProject.person.firstName=example
                acme.my_project.person.first_name=example
                """,
              """
                acme.notMyProject.person=example
                """
            )
          );
    }
}
