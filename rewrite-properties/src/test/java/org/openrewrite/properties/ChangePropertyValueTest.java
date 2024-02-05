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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class ChangePropertyValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePropertyValue(
          "management.metrics.binders.files.enabled",
          "false",
          null,
          false,
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

    @DocumentExample
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
          spec -> spec.recipe(new ChangePropertyValue("quarkus.quartz.store-type", "jdbc-cmt", "db", false, null)),
          properties(
            "quarkus.quartz.store-type=db",
            "quarkus.quartz.store-type=jdbc-cmt"
          )
        );
    }

    @Test
    void conditionallyChangeValueNoChange() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("quarkus.quartz.store-type", "jdbc-cmt", "cache", false, null)),
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
          spec -> spec.recipe(new ChangePropertyValue(propertyKey, "updated", "example", false, null)),
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
            false
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
    void regex() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar$1", "f(o+)", true, null)),
          properties(
            "my.prop=foooo",
            "my.prop=baroooo"
          )
        );
    }

    @Test
    void regexDefaultOff() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar$1", ".+", null, null)),
          properties(
            "my.prop=foo"
          )
        );
    }

    @Test
    void partialMatchRegex() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("*", "[replaced:$1]", "\\[replaceme:(.*?)]", true, null)),
          properties("""
            multiple=[replaceme:1][replaceme:2]
            multiple-prefixed=test[replaceme:1]test[replaceme:2]
            multiple-suffixed=[replaceme:1]test[replaceme:2]test
            multiple-both=test[replaceme:1]test[replaceme:2]test
            """, """
            multiple=[replaced:1][replaced:2]
            multiple-prefixed=test[replaced:1]test[replaced:2]
            multiple-suffixed=[replaced:1]test[replaced:2]test
            multiple-both=test[replaced:1]test[replaced:2]test
            """)
        );
    }

    @Test
    void partialMatchNonRegex() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("*", "replaced", "replaceme", null, null)),
          properties("""
            multiple=[replaceme:1][replaceme:2]
            multiple-prefixed=test[replaceme:1]test[replaceme:2]
            multiple-suffixed=[replaceme:1]test[replaceme:2]test
            multiple-both=test[replaceme:1]test[replaceme:2]test
            """)
        );
    }

    @Test
    void validatesThatOldValueIsRequiredIfRegexEnabled() {
        assertTrue(new ChangePropertyValue("my.prop", "bar", null, true, null).validate().isInvalid());
    }
}
