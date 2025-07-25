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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class DeletePropertyKeyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DeleteProperty("management.metrics.binders.files.enabled", null, null, null));
    }

    @DocumentExample
    @Test
    void singleEntry() {
        rewriteRun(
          yaml("management.metrics.binders.files.enabled: true",
            ""
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1841")
    @Test
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
    void deleteSequenceItem() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("foo.bar.sequence.propertyA",
            null, null, null)),
          yaml(
            """
                foo:
                  bar:
                    sequence:
                      - name: name
                      - propertyA: fieldA
                      - propertyB: fieldB
                    scalar: value
              """,
            """
                foo:
                  bar:
                    sequence:
                      - name: name
                      - propertyB: fieldB
                    scalar: value
              """
          )
        );
    }

    @Test
    void deleteEntireSequence() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("foo.bar.sequence.propertyA",
            null, null, null)),
          yaml(
            """
              foo:
                bar:
                  sequence:
                    - propertyA: fieldA
                  # comments
                  scalar: value
              """,
            """
              foo:
                bar:
                  # comments
                  scalar: value
              """
          )
        );
    }


    @Test
    void deleteFirstItemWithComments() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("foo.bar.sequence",
            null, null, null)),
          yaml(
            """
              foo:
                bar:
                  sequence:
                    - name: name
                    - propertyA: fieldA
                    - propertyB: fieldB
                  # Some comments
                  scalar: value
              """,
            """
              foo:
                bar:
                  # Some comments
                  scalar: value
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2273")
    @SuppressWarnings("YAMLUnusedAnchor")
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @ParameterizedTest
    @ValueSource(strings = {
      "acme.my-project.person.first-name",
      "acme.myProject.person.firstName",
      "acme.my_project.person.first_name"
    })
    void relaxedBinding(String propertyKey) {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty(propertyKey, false, true, null)),
          yaml("acme.my-project.person.first-name: example",
            ""
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @Test
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

    @Issue("https://github.com/openrewrite/rewrite/issues/4204")
    @Test
    void preserveEmptySequencesWithOtherKeys() {
        rewriteRun(
          spec -> spec.recipe(new DeleteProperty("my.key", false, null, null)),
          yaml(
            """
              my.key: qwe
              seq: []
              """,
            """
              seq: []
              """
          )
        );
    }
}
