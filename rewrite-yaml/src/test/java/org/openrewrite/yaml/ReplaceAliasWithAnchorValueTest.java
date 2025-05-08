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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class ReplaceAliasWithAnchorValueTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(ReplaceAliasWithAnchorValueVisitor::new));
    }

    @DocumentExample
    @Test
    void simpleCase() {
        rewriteRun(
          yaml(
                """
              bar:
                &abc yo: friend
              baz:
                *abc: friendly
              """,
            """
              bar:
                &abc yo: friend
              baz:
                yo: friendly
              """
          )
        );
    }

    @Test
    void aliasRefersToLastKnownAnchorValue() {
        rewriteRun(
          yaml(
                """
              definitions:
                steps:
                  - step: &build-test
                      name: Build and test
                  - step2: &build-deploy
                      name: Build and Deploy
              pipelines:
                branches:
                  develop:
                    - step: *build-test
                  master:
                    - step: *build-deploy
              environments:
                branches:
                  test:
                    - step: &build-test
                        name: ReUsed Build and Test
                  qa:
                    - step: *build-test
              """,
            """
              definitions:
                steps:
                  - step: &build-test
                      name: Build and test
                  - step2: &build-deploy
                      name: Build and Deploy
              pipelines:
                branches:
                  develop:
                    - step:
                      name: Build and test
                  master:
                    - step:
                      name: Build and Deploy
              environments:
                branches:
                  test:
                    - step: &build-test
                        name: ReUsed Build and Test
                  qa:
                    - step:
                        name: ReUsed Build and Test
              """
          )
        );
    }

    @Test
    void howAboutSequences() {
        rewriteRun(
          yaml(
                """
              stages:
                - id: &id ping_api_endpoint
                  name: *id
                - id: &id get_token
                  name: *id
                - id: &id do_something
                  name: *id
                - id: &id revoke_token
                  name: *id
              """,
            """
              stages:
                - id: &id ping_api_endpoint
                  name: ping_api_endpoint
                - id: &id get_token
                  name: get_token
                - id: &id do_something
                  name: do_something
                - id: &id revoke_token
                  name: revoke_token
              """
          )
        );
    }

}
