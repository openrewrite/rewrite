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
package org.openrewrite.yaml.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.style.YamlDefaultStyles;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.yaml.Assertions.yaml;

@SuppressWarnings("KubernetesUnknownResourcesInspection")
class IndentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new IndentsVisitor<>(
            YamlDefaultStyles.indents(),
            null
          )
        ));
    }

    @DocumentExample
    @Test
    void indentSequence() {
        rewriteRun(
          yaml(
                """
                  root:
                      - a: 0
                        b: 0
              """,
            """
                  root:
                    - a: 0
                      b: 0
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3531")
    @Test
    void multilineString() {
        rewriteRun(
          yaml(
                """
            foo:
              bar: >
                A multiline string.
              baz:
                quz: Another string.
            """
          )
        );
    }

    @Test
    void indents() {
        rewriteRun(
          yaml(
                """
                  apiVersion: storage.cnrm.cloud.google.com/v1beta1
                  kind: StorageBucket
                  spec:
                          bucketPolicyOnly: true
                          lifecycleRule:
                                  - action:
                                      type: Delete
                                    condition:
                                      age: 7
              """,
            """
                  apiVersion: storage.cnrm.cloud.google.com/v1beta1
                  kind: StorageBucket
                  spec:
                    bucketPolicyOnly: true
                    lifecycleRule:
                      - action:
                          type: Delete
                        condition:
                          age: 7
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1135")
    @Test
    void maintainIndentSpacingOnMixedTypeSequences() {
        rewriteRun(
          yaml(
                """
                steps:
                  - checkout
                  - run:
                      name: Install dependencies
                      command: npm ci
                  - run:
                      name: Run tests
                      command: npm run test:ci
            """
          )
        );
    }

    @Test
    void indentSequenceComments() {
        rewriteRun(
          yaml(
                """
              key:
              # a under-indented
                  # a over-indented
                - a :
                # b under-indented
                      # b over-indented
                     - b
              """,
            """
              key:
                # a under-indented
                # a over-indented
                - a :
                    # b under-indented
                    # b over-indented
                    - b
              """
          )
        );
    }

    @Test
    void indentMappingComments() {
        rewriteRun(
          yaml(
                """
              key: # no change
              # under-indented
                  # over-indented
                a : # no change
              
              
                # under-indented
                    # over-indented
                  b : c
              """,
            """
              key: # no change
                # under-indented
                # over-indented
                a : # no change
              
              
                  # under-indented
                  # over-indented
                  b : c
              """
          )
        );
    }

    @Test
    void indentRootComments() {
        rewriteRun(
          yaml(
                """
                # over-indented 1
              ---
                # over-indented 2
              key: value # no change
                # over-indented 2
              key2: value2
              """,
            """
              # over-indented 1
              ---
              # over-indented 2
              key: value # no change
              # over-indented 2
              key2: value2
              """
          )
        );
    }
}
