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
package org.openrewrite.yaml.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindPropertyTest implements RewriteTest {

    @DocumentExample
    @Test
    void findProperty() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("management.metrics.binders.files.enabled", null, null)),
          yaml(
            "management.metrics.binders.files.enabled: true",
            "management.metrics.binders.files.enabled: ~~>true"
          )
        );
    }

    @Test
    void findGlobProperty() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("management.metrics.binders.*.enabled", null, null)),
          yaml(
            "management.metrics.binders.files.enabled: true",
            "management.metrics.binders.files.enabled: ~~>true"
          )
        );
    }

    @Test
    void findPropertyWithSpecificValueMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("my.cool.property", null, "my-matching-value")),
          yaml(
            "my.cool.property: my-matching-value",
            "my.cool.property: ~~>my-matching-value"
          )
        );
    }

    @Test
    void findPropertyWithSpecificValueMatchSingleQuotes() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("my.cool.property", null, "my-matching-value")),
          yaml(
            "my.cool.property: 'my-matching-value'",
            "my.cool.property: ~~>'my-matching-value'"
          )
        );
    }

    @Test
    void findPropertyWithSpecificValueMatchDoubleQuotes() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("my.cool.property", null, "my-matching-value")),
          yaml(
            "my.cool.property: \"my-matching-value\"",
            "my.cool.property: ~~>\"my-matching-value\""
          )
        );
    }

    @Disabled("how do I test that the search has no hits?")
    @Test
    void findPropertyWithSpecificValueNoMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("my.cool.property", null, "my-non-matching-value")),
          yaml(
            "my.cool.property: my-matching-value",
            "my.cool.property: my-matching-value"
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
          spec -> spec.recipe(new FindProperty(propertyKey, true, null)),
          yaml(
            """
              acme.my-project.person.first-name: example
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              """,
            """
              acme.my-project.person.first-name: ~~>example
              acme.myProject.person.firstName: ~~>example
              acme.my_project.person.first_name: ~~>example
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @Test
    void exactMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindProperty("acme.my-project.person.first-name", false, null)),
          yaml(
            """
              acme.my-project.person.first-name: example
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              """,
            """
              acme.my-project.person.first-name: ~~>example
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              """
          )
        );
    }
}
