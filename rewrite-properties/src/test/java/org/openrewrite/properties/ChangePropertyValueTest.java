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
class ChangePropertyValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePropertyValue(
          "management.metrics.binders.files.enabled",
          "false",
          null,
          null,
          null
        ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/575")
    @Test
    void preserveComment() {
        rewriteRun(
          properties(
            """
              management.metrics.binders.files.enabled=true
              # comment
              """,
            """
              management.metrics.binders.files.enabled=false
              # comment
              """
          )
        );
    }

    @Test
    void changeValue() {
        rewriteRun(
          properties(
            "management.metrics.binders.files.enabled=true",
            "management.metrics.binders.files.enabled=false"
          )
        );
    }

    @Test
    void conditionallyChangeValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("quarkus.quartz.store-type", "jdbc-cmt", "db", null, null)),
          properties(
            "quarkus.quartz.store-type=db",
            "quarkus.quartz.store-type=jdbc-cmt"
          )
        );
    }

    @Test
    void conditionallyChangeValueNoChange() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("quarkus.quartz.store-type", "jdbc-cmt", "cache", null, null)),
          properties(
            "quarkus.quartz.store-type=db"
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
          spec -> spec.recipe(new ChangePropertyValue(propertyKey, "updated", "example", null, null)),
          properties(
            """
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              acme.my-project.person.first-name=updated
              acme.myProject.person.firstName=updated
              acme.my_project.person.first_name=updated
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void exactMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue(
            "acme.my-project.person.first-name",
            "updated",
            "example",
            false,
            null
          )),
          properties(
            """
              acme.my-project.person.first-name=example
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """,
            """
              acme.my-project.person.first-name=updated
              acme.myProject.person.firstName=example
              acme.my_project.person.first_name=example
              """
          )
        );
    }

    @Test
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("management.metrics", "false", "true", null, "**/a.properties")),
          properties(
            "management.metrics=true",
            "management.metrics=false",
            spec -> spec.path("a.properties")
          ),
          properties(
            "management.metrics=true",
            spec -> spec.path("b.properties")
          )
        );
    }
}
