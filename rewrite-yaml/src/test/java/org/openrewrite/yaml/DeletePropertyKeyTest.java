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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class DeletePropertyKeyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DeleteProperty("management.metrics.binders.files.enabled", null, null, null));
    }

    @Test
    void singleEntry() {
        rewriteRun(
          yaml("management.metrics.binders.files.enabled: true",
            ""
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1841")
    void firstItem() {
        rewriteRun(
          yaml(
            """
              management.metrics.binders.files.enabled: true
              server.port: 8080
              """,
            """
              server.port: 8080
              """
          )
        );
    }

    @Test
    void lastItem() {
        rewriteRun(
          yaml(
            """
              server.port: 8080
              management.metrics.binders.files.enabled: true
              """,
            """
              server.port: 8080
              """
          )
        );
    }

    @Test
    void middleItem() {
        rewriteRun(
          yaml(
            """
              app.name: foo
              management.metrics.binders.files.enabled: true
              server.port: 8080
              """,
            """
              app.name: foo
              server.port: 8080
              """
          )
        );
    }

    @Test
    void downDeeper() {
        rewriteRun(
          yaml(
            """
              management.metrics:
                enabled: true
                binders.files.enabled: true
              server.port: 8080
              """,
            """
              management.metrics:
                enabled: true
              server.port: 8080
              """
          )
        );
    }

    @SuppressWarnings("YAMLUnusedAnchor")
    @Issue("https://github.com/openrewrite/rewrite/issues/2273")
    @Test
    void aliasAnchorPairs() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("bar.yo", null, null, null)),
          yaml(
            """
              bar:
                &abc yo: friend
              baz:
                *abc: friendly
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "acme.my-project.person.first-name",
      "acme.myProject.person.firstName",
      "acme.my_project.person.first_name"
    })
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void relaxedBinding(String propertyKey) {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty(propertyKey, false, true, null)),
          yaml("acme.my-project.person.first-name: example",
            ""
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void exactMatch() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("acme.my-project.person.first-name", false, false, null)),
          yaml(
            """
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              acme.my-project.person.first-name: example
              """,
            """
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              """
          )
        );
    }

    @Test
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("apiVersion", true, null, "**/a.yml")),
          yaml("apiVersion: v1", "", spec -> spec.path("a.yml")),
          yaml("apiVersion: v1", spec -> spec.path("b.yml"))
        );

    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1769")
    @Test
    void preservesOriginalIndentStructureOfExistingHierarchy() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("my.old.key", false, null, null)),
          yaml(
            """
                my:
                  old:
                    key:
                      color: blue
                      style: retro
                  other:
                    key: qwe
              """,
            """
                my:
                  other:
                    key: qwe
              """
          )
        );
    }
}
