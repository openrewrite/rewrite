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
package org.openrewrite.json;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class DeleteKeyTest implements RewriteTest {
    @DocumentExample
    @Test
    void deleteNestedKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.metadata.name", null)),
          json(
                """
              {
                "apiVersion": "v1",
                "metadata": {
                  "name": "monitoring-tools",
                  "namespace": "monitoring-tools"
                }
              }
              """,
            """
              {
                "apiVersion": "v1",
                "metadata": {
                  "namespace": "monitoring-tools"
                }
              }
              """
          )
        );
    }

    @Test
    void deleteArrayKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects.kind", null)),
          json(
                """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount",
                    "name": "monitoring-tools"
                  }
                ]
              }
              """,
            """
              {
                "subjects": [
                  {
                    "name": "monitoring-tools"
                  }
                ]
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1175")
    @Test
    void deleteNestedKeyRemovingUnusedKeysRecursively() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.b.c.d", true)),
          json(
                """
              {
                "a": "a-value",
                "b": {
                  "c": {
                    "d": "d-value"
                  }
                }
              }
              """,
            """
              {
                "a": "a-value"
              }
              """
          )
        );
    }

    @Test
    void doNotDeleteEmptyParentsByDefault() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node", null)),
          json(
                """
              {
                "name": "my-package",
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "name": "my-package",
                "engines": {}
              }
              """
          )
        );
    }

    @Test
    void declarativeRecipeWithoutTheOption() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
                """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.json.DeleteEnginesNode
              displayName: Delete `engines.node`
              description: Deletes the `engines.node` key.
              recipeList:
                - org.openrewrite.json.DeleteKey:
                    keyPath: $.engines.node
              """,
            "org.openrewrite.json.DeleteEnginesNode"
          ),
          json(
                """
              {
                "name": "my-package",
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "name": "my-package",
                "engines": {}
              }
              """
          )
        );
    }

    @Test
    void declarativeRecipeWithTheOption() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
                """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.json.DeleteEnginesNode
              displayName: Delete `engines.node`
              description: Deletes the `engines.node` key.
              recipeList:
                - org.openrewrite.json.DeleteKey:
                    keyPath: $.engines.node
                    deleteEmptyParents: true
              """,
            "org.openrewrite.json.DeleteEnginesNode"
          ),
          json(
                """
              {
                "name": "my-package",
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "name": "my-package"
              }
              """
          )
        );
    }

    @SuppressWarnings("deprecation")
    @Test
    void singleArgumentConstructorRetainsDefaultBehavior() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node")),
          json(
                """
              {
                "name": "my-package",
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "name": "my-package",
                "engines": {}
              }
              """
          )
        );
    }

    @Test
    void deleteEmptyParent() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node", true)),
          json(
                """
              {
                "name": "my-package",
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "name": "my-package"
              }
              """
          )
        );
    }

    @Test
    void keepParentThatStillHasMembers() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node", true)),
          json(
                """
              {
                "engines": {
                  "node": ">=20",
                  "npm": ">=10"
                }
              }
              """,
            """
              {
                "engines": {
                  "npm": ">=10"
                }
              }
              """
          )
        );
    }

    @Test
    void keepObjectsThatWereAlreadyEmpty() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node", true)),
          json(
                """
              {
                "scripts": {},
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "scripts": {}
              }
              """
          )
        );
    }

    @Test
    void keepArraysThatWereAlreadyEmpty() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node", true)),
          json(
                """
              {
                "keywords": [],
                "engines": {
                  "node": ">=20"
                }
              }
              """,
            """
              {
                "keywords": []
              }
              """
          )
        );
    }

    @Test
    void keepEmptiedRootObject() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.node", true)),
          json(
                """
              {
                "node": ">=20"
              }
              """,
            """
              {}
              """
          )
        );
    }

    @Test
    void deleteEmptyObjectsAndArrays() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects.kind", true)),
          json(
                """
              {
                "apiVersion": "v1",
                "subjects": [
                  {
                    "kind": "ServiceAccount"
                  }
                ]
              }
              """,
            """
              {
                "apiVersion": "v1"
              }
              """
          )
        );
    }

    @Test
    void keepArrayWithRemainingElements() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects.kind", true)),
          json(
                """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount"
                  },
                  {
                    "name": "monitoring-tools"
                  }
                ]
              }
              """,
            """
              {
                "subjects": [
                  {
                    "name": "monitoring-tools"
                  }
                ]
              }
              """
          )
        );
    }

    @Test
    void deleteDeeplyNestedEmptyParents() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.b.c.d.e.f", true)),
          json(
                """
              {
                "keep": "keep-value",
                "a": {
                  "b": {
                    "c": {
                      "d": {
                        "e": {
                          "f": "f-value"
                        }
                      }
                    }
                  }
                }
              }
              """,
            """
              {
                "keep": "keep-value"
              }
              """
          )
        );
    }

    @Test
    void stopDeletingAtAncestorWithOtherMembers() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.b.c.d.e", true)),
          json(
                """
              {
                "a": {
                  "b": {
                    "keep": "keep-value",
                    "c": {
                      "d": {
                        "e": "e-value"
                      }
                    }
                  }
                }
              }
              """,
            """
              {
                "a": {
                  "b": {
                    "keep": "keep-value"
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void deleteEmptyParentsThroughArrays() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.b[*].c.d", true)),
          json(
                """
              {
                "keep": "keep-value",
                "a": {
                  "b": [
                    {
                      "c": {
                        "d": "d-value"
                      }
                    }
                  ]
                }
              }
              """,
            """
              {
                "keep": "keep-value"
              }
              """
          )
        );
    }

    @Test
    void deleteLastKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.npm", null)),
          json(
                """
              {
                "engines": {
                  "node": ">=20",
                  "npm": ">=10"
                }
              }
              """,
            """
              {
                "engines": {
                  "node": ">=20"
                }
              }
              """
          )
        );
    }

    @Test
    void retainCommentOnKeyThatBecomesLast() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.npm", null)),
          json(
                """
              {
                "node": ">=20" /* the runtime */,
                "npm": ">=10"
              }
              """,
            """
              {
                "node": ">=20" /* the runtime */
              }
              """
          )
        );
    }

    @Test
    void retainCommentOnArrayElementThatBecomesLast() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects[*].kind", true)),
          json(
                """
              {
                "subjects": [
                  {
                    "name": "monitoring-tools"
                  } /* the only one left */,
                  {
                    "kind": "ServiceAccount"
                  }
                ]
              }
              """,
            """
              {
                "subjects": [
                  {
                    "name": "monitoring-tools"
                  } /* the only one left */
                ]
              }
              """
          )
        );
    }

    @Test
    void deleteEmptyParentWithTrailingComma() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.engines.node", true)),
          json(
                """
              {
                "name": "my-package",
                "engines": {
                  "node": ">=20",
                }
              }
              """,
            """
              {
                "name": "my-package"
              }
              """
          )
        );
    }
}
