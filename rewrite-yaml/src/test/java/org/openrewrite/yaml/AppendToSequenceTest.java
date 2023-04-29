/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.yaml.Assertions.yaml;

class AppendToSequenceTest implements RewriteTest {

    @DocumentExample
    @Test
    void appendToSequenceHasDashTrue() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              null,
              null
            )),
          yaml(
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
              """,
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
                      - strawberry
              """
          )
        );
    }

    @Test
    void appendToSequenceOfSingleQuotedValuesHasDashTrue() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              List.of("apple", "blueberry"),
              true
            )),
          yaml(
            """
                  things:
                    fruit:
                      - 'apple'
                      - 'blueberry'
              """,
            """
                  things:
                    fruit:
                      - 'apple'
                      - 'blueberry'
                      - 'strawberry'
              """
          )
        );
    }

    @Test
    void appendToSequenceWhenExistingSequenceValuesMatchInExactOrder() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              List.of("apple", "blueberry"),
              false
            )),
          yaml(
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
              """,
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
                      - strawberry
              """
          )
        );
    }

    @Test
    void appendToSequenceWhenExistingSequenceValuesMatchInAnyOrder() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              List.of("blueberry", "apple"),
              true
            )),
          yaml(
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
              """,
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
                      - strawberry
              """
          )
        );
    }

    @Test
    void doNotAppendToSequenceWhenExistingSequenceValuesDoNotMatch() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              List.of("zzz"),
              false
            )),
          yaml(
            """
                  things:
                    fruit:
                      - apple
                      - blueberry
              """
          )
        );
    }

    @Test
    void appendToSequenceOfNameValuePair() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "name: strawberry",
              null,
              null
            )),
          yaml(
            """
                  things:
                    fruit:
                      - name: apple
                      - name: blueberry
                    animals:
                      - cat
                      - dog
              """,
            """
                  things:
                    fruit:
                      - name: apple
                      - name: blueberry
                      - name: strawberry
                    animals:
                      - cat
                      - dog
              """
          )
        );
    }

    @Test
    void appendToSequenceOfNameValuePairMatchExistingValuesInAnyOrder() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "name: strawberry",
              List.of("name: blueberry", "name: apple"),
              true
            )),
          yaml(
            """
                  things:
                    fruit:
                      - name: apple
                      - name: blueberry
                    animals:
                      - cat
                      - dog
              """,
            """
                  things:
                    fruit:
                      - name: apple
                      - name: blueberry
                      - name: strawberry
                    animals:
                      - cat
                      - dog
              """
          )
        );
    }

    @Test
    void appendToSequenceOfLiteralsHasDashFalse() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              null,
              null
            )),
          yaml(
            """
                  things:
                    fruit: [apple, blueberry]
                    animals:
                      - cat
                      - dog
              """,
            """
                  things:
                    fruit: [apple, blueberry, strawberry]
                    animals:
                      - cat
                      - dog
              """
          )
        );
    }

    @Test
    void appendToSequenceOfSingleQuotedValuesHasDashFalse() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              null,
              null
            )),
          yaml(
            """
                  things:
                    fruit: ['apple', 'blueberry']
                    animals:
                      - cat
                      - dog
              """,
            """
                  things:
                    fruit: ['apple', 'blueberry', 'strawberry']
                    animals:
                      - cat
                      - dog
              """
          )
        );
    }

    @Test
    void appendToSequenceOfDoubleQuotedValuesHasDashFalse() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              null,
              null
            )),
          yaml(
            """
                  things:
                    fruit: ["apple", "blueberry"]
                    animals:
                      - cat
                      - dog
              """,
            """
                  things:
                    fruit: ["apple", "blueberry", "strawberry"]
                    animals:
                      - cat
                      - dog
              """
          )
        );
    }

    @Test
    void appendToEmptySequence() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.things.fruit",
              "strawberry",
              null,
              null
            )),
          yaml(
            """
                  things:
                    fruit: []
              """,
            """
                  things:
                    fruit: [strawberry]
              """
          )
        );
    }

    @Test
    void modifyRegionList() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.prod.regions",
              "name: us-foo-2",
              List.of("name: us-foo-1", "name: us-bar-1"),
              true
            )),
          yaml(
            """
                  prod:
                    regions:
                      - name: us-bar-1
                      - name: us-foo-1
                    other:
                      - name: outerspace-1
              """,
            """
                  prod:
                    regions:
                      - name: us-bar-1
                      - name: us-foo-1
                      - name: us-foo-2
                    other:
                      - name: outerspace-1
              """
          )
        );
    }

    @Test
    void doesNotModifyRegionListBecauseValueIsAlreadyPresent() {
        rewriteRun(
          spec -> spec
            .recipe(new AppendToSequence(
              "$.prod.regions",
              "name: us-foo-2",
              List.of("name: us-foo-1", "name: us-bar-1"),
              true
            )),
          yaml(
            """
                  prod:
                    regions:
                      - name: us-bar-1
                      - name: us-foo-1
                      - name: us-foo-2
                    other:
                      - name: outerspace-1
              """
          )
        );
    }
}
