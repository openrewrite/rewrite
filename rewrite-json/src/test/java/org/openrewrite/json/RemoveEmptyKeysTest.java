/*
 * Copyright 2026 the original author or authors.
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

class RemoveEmptyKeysTest implements RewriteTest {

    @DocumentExample
    @Test
    void removeEmptyObject() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "name": "my-package",
                "engines": {},
                "version": "1.0.0"
              }
              """,
            """
              {
                "name": "my-package",
                "version": "1.0.0"
              }
              """
          )
        );
    }

    @Test
    void removeEmptyArray() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "files": [],
                "version": "1.0.0"
              }
              """,
            """
              {
                "version": "1.0.0"
              }
              """
          )
        );
    }

    @Test
    void removeLastKeyPreservingClosingBraceWhitespace() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "name": "my-package",
                "engines": {}
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1175")
    @Test
    void removeChainOfContainersFromTheInsideOut() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "apiVersion": "v1",
                "metadata": {
                  "labels": {
                    "annotations": {}
                  }
                }
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
    void emptyDocumentRootIsRetained() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "engines": {}
              }
              """,
            """
              {}
              """
          )
        );
    }

    @Test
    void doNotRemovePopulatedContainers() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "engines": {
                  "node": ">=20"
                },
                "files": [
                  "dist"
                ]
              }
              """
          )
        );
    }

    @Test
    void nullAndEmptyStringAreNotEmpty() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "license": null,
                "description": ""
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveArrayElements() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "subjects": [
                  {},
                  []
                ]
              }
              """
          )
        );
    }

    @Test
    void removeKeysWithinArrayElements() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount",
                    "metadata": {}
                  }
                ]
              }
              """,
            """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount"
                  }
                ]
              }
              """
          )
        );
    }

    @Test
    void removeFirstKey() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "engines": {},
                "name": "my-package"
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
    void keysLimitsWhichKeysAreRemoved() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.engines", null)),
          json(
            """
              {
                "engines": {},
                "scripts": {},
                "name": "my-package"
              }
              """,
            """
              {
                "scripts": {},
                "name": "my-package"
              }
              """
          )
        );
    }

    @Test
    void keysMatchingAtAnyDepth() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$..devDependencies", null)),
          json(
            """
              {
                "devDependencies": {},
                "workspaces": {
                  "web": {
                    "devDependencies": {},
                    "dependencies": {}
                  }
                }
              }
              """,
            """
              {
                "workspaces": {
                  "web": {
                    "dependencies": {}
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void keysThatDoNotMatchLeaveTheDocumentAlone() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.engines", null)),
          json(
            """
              {
                "scripts": {}
              }
              """
          )
        );
    }

    @Test
    void keysCleanUpWithinTheSelectedSubtree() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.engines", null)),
          json(
            """
              {
                "engines": {
                  "node": {}
                }
              }
              """,
            """
              {}
              """
          )
        );
    }

    @Test
    void keysWithinArrayElementsOfTheSelectedSubtree() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.subjects", null)),
          json(
            """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount",
                    "metadata": {}
                  }
                ],
                "rules": {
                  "verbs": {}
                }
              }
              """,
            """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount"
                  }
                ],
                "rules": {
                  "verbs": {}
                }
              }
              """
          )
        );
    }

    @Test
    void keysCascadeUpToTheSelectedKey() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.metadata", null)),
          json(
            """
              {
                "apiVersion": "v1",
                "metadata": {
                  "labels": {
                    "annotations": {}
                  }
                }
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
    void cascadeToRemovesTheKeyItEmptied() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.spec.template", "$.spec")),
          json(
            """
              {
                "spec": {
                  "template": {
                    "volumes": {}
                  }
                },
                "other": 1
              }
              """,
            """
              {
                "other": 1
              }
              """
          )
        );
    }

    @Test
    void cascadeToStopsAtItsOwnBoundary() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.a.b.c", "$.a.b")),
          json(
            """
              {
                "a": {
                  "b": {
                    "c": {
                      "d": {}
                    }
                  }
                }
              }
              """,
            """
              {
                "a": {}
              }
              """
          )
        );
    }

    @Test
    void cascadeToLeavesKeysItDidNotEmptyAlone() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.spec.template", "$.spec")),
          json(
            """
              {
                "spec": {
                  "template": {
                    "volumes": {}
                  },
                  "selector": {}
                }
              }
              """,
            """
              {
                "spec": {
                  "selector": {}
                }
              }
              """
          )
        );
    }

    @Test
    void keysSelectingSeveralCeilings() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$['metadata','spec']", null)),
          json(
            """
              {
                "metadata": {
                  "labels": {}
                },
                "spec": {
                  "ports": {}
                },
                "status": {
                  "phase": {}
                }
              }
              """,
            """
              {
                "status": {
                  "phase": {}
                }
              }
              """
          )
        );
    }

    @Test
    void keysDoNotCascadeAboveTheSelectedSubtree() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys("$.metadata.labels", null)),
          json(
            """
              {
                "apiVersion": "v1",
                "metadata": {
                  "labels": {
                    "annotations": {}
                  }
                }
              }
              """,
            """
              {
                "apiVersion": "v1",
                "metadata": {}
              }
              """
          )
        );
    }

    @Test
    void preserveCommentsPrecedingTheClosingBrace() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "name": "my-package" /* trailing */,
                "engines": {}
                // last word
              }
              """,
            """
              {
                "name": "my-package" /* trailing */
                // last word
              }
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyKeys(null, null)),
          json(
            """
              {
                "name": "my-package",
                "engines": {},
              }
              """,
            """
              {
                "name": "my-package",
              }
              """
          )
        );
    }
}
