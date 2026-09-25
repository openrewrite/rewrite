/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.ChangeValue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

/**
 * A dependency recipe that visits the lock before its package.json computes the edit from the
 * package.json captured at scan time. A change an earlier recipe in the same run made to that
 * package.json must survive, rather than being reverted when the package.json is visited.
 */
class DependencyEditAfterEarlierPackageJsonChangeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        HttpSender http = request -> new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
        });
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
        spec.executionContext(ctx).cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void removalKeepsScriptChangeMadeEarlierInTheRun() {
        rewriteRun(
                spec -> spec.recipes(
                        new ChangeValue("$.scripts.start", "\"node --enable-source-maps index.js\""),
                        new RemoveDependency("left-pad", null)),
                packageLock(
                        """
                        {
                          "name": "app",
                          "version": "1.0.0",
                          "lockfileVersion": 3,
                          "requires": true,
                          "packages": {
                            "": {
                              "name": "app",
                              "version": "1.0.0",
                              "dependencies": {
                                "is-number": "^7.0.0",
                                "left-pad": "^1.3.0"
                              }
                            },
                            "node_modules/is-number": {
                              "version": "7.0.0",
                              "resolved": "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
                              "integrity": "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==",
                              "license": "MIT",
                              "engines": {
                                "node": ">=0.12.0"
                              }
                            },
                            "node_modules/left-pad": {
                              "version": "1.3.0",
                              "resolved": "https://registry.npmjs.org/left-pad/-/left-pad-1.3.0.tgz",
                              "integrity": "sha512-XI5MPzVNApjAyhQzphX8BkmKsKUxD4LdyK24iZeQEU5Q1iQ9Ge4q7QkEvDdQjpGEXVT/cDgiFSjW2NTFKxnIr8oA==",
                              "deprecated": "use String.prototype.padStart()",
                              "license": "WTFPL"
                            }
                          }
                        }
                        """,
                        """
                        {
                          "name": "app",
                          "version": "1.0.0",
                          "lockfileVersion": 3,
                          "requires": true,
                          "packages": {
                            "": {
                              "name": "app",
                              "version": "1.0.0",
                              "dependencies": {
                                "is-number": "^7.0.0"
                              }
                            },
                            "node_modules/is-number": {
                              "version": "7.0.0",
                              "resolved": "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
                              "integrity": "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==",
                              "license": "MIT",
                              "engines": {
                                "node": ">=0.12.0"
                              }
                            }
                          }
                        }
                        """
                ),
                packageJson(
                        """
                        {
                          "name": "app",
                          "version": "1.0.0",
                          "scripts": {
                            "start": "node -r source-map-support/register index.js"
                          },
                          "dependencies": {
                            "is-number": "^7.0.0",
                            "left-pad": "^1.3.0"
                          }
                        }
                        """,
                        """
                        {
                          "name": "app",
                          "version": "1.0.0",
                          "scripts": {
                            "start": "node --enable-source-maps index.js"
                          },
                          "dependencies": {
                            "is-number": "^7.0.0"
                          }
                        }
                        """,
                        nodeResolutionResult(PackageManager.Npm,
                                dependency("is-number", "^7.0.0"), dependency("left-pad", "^1.3.0"))
                )
        );
    }
}
