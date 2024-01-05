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
package org.openrewrite.properties.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class FindPropertiesTest implements RewriteTest {

    @DocumentExample
    @Test
    void findProperty() {
        rewriteRun(
          spec -> spec.recipe(new FindProperties("management.metrics.binders.files.enabled", null)),
          properties(
            "management.metrics.binders.files.enabled=true",
            "management.metrics.binders.files.enabled=~~>true"
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "acme.my-project.person.first-name",
      "acme.myProject.person.firstName",
      "acme.my_project.person.first_name",
    })
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void relaxedBinding(String propertyKey) {
        rewriteRun(
          spec -> spec.recipe(new FindProperties(propertyKey, true)),
          properties(
            """
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              acme.my-project.person.first-name=~~>example
              acme.myProject.person.firstName=~~>example
              acme.my_project.person.first_name=~~>example
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void exactMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindProperties("acme.my-project.person.first-name", false)),
          properties(
            """
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              acme.my-project.person.first-name=~~>example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void globMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindProperties("acme.my-project.*", false)),
          properties(
            """
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              acme.my-project.person.first-name=~~>example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """
          )
        );
    }

    @Test
    void valueWithLineContinuation() {
        rewriteRun(
          spec -> spec.recipe(new FindProperties("foo", null)),
          properties(
            """
              foo=tr\\
                ue
              """,
            """
              foo=~~>tr\\
                ue
              """
          )
        );
    }
}
