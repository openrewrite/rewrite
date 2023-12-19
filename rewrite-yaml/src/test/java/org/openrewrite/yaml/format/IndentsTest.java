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

class IndentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new IndentsVisitor<>(
            YamlDefaultStyles.indents(),
            null
          )
        ));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3531")
    void multilineString() {
        rewriteRun(
          yaml("""
            foo:
              bar: >
                A multiline string.
              baz:
                quz: Another string.
            """
          )
        );
    }

    @DocumentExample
    @Test
    void indentSequence() {
        rewriteRun(
          yaml("""
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

    @Test
    void indents() {
        rewriteRun(
          yaml("""
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1135")
    void maintainIndentSpacingOnMixedTypeSequences() {
        rewriteRun(
          yaml("""
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
}
