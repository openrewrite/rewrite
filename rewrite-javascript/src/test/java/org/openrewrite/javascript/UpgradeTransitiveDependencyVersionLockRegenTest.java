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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

/**
 * PM-free end-to-end tests of native lock regeneration for an override of a transitive dependency.
 * No package manager is executed: the registry is stubbed over the run's {@link HttpSender} with
 * recorded packument/manifest responses, and the regenerated lock is asserted against a golden
 * recorded from a real {@code npm install --package-lock-only}.
 * <p>
 * An override edits no declared dependency, so unlike the closure-unchanged bumps in
 * {@link UpgradeDependencyVersionLockRegenTest} a byte-identical lock is the failure here, not the
 * expected outcome: the override moves the transitive resolution and the lock must follow it.
 * <p>
 * A fail-loud fix (warn, record a failure row, leave the lock at {@code 6.0.0}) would still fail
 * this test, deliberately: a recipe run has to be atomic. {@code npm install} is what a developer
 * runs <em>before</em> committing, so a run that needs it afterwards has not produced a
 * committable change.
 */
class UpgradeTransitiveDependencyVersionLockRegenTest implements RewriteTest {

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        HttpSender http = request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
    }

    /**
     * {@code is-odd@3.0.1} pulls {@code is-number@6.0.0} transitively. Overriding {@code is-number}
     * to {@code ^7.0.0} must write the override <em>and</em> re-pin the lock to {@code 7.0.0}: a
     * manifest whose override disagrees with its lock is rejected outright by {@code npm ci}
     * ("Invalid: lock file's is-number@6.0.0 does not satisfy is-number@7.0.0").
     */
    @Test
    void npmOverrideOfATransitiveRegeneratesTheLock() {
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/transitive-override/http/is-odd-3.0.1"));
        routes.put("https://registry.npmjs.org/is-number", resource("lock/npm/transitive-override/http/is-number"));
        routes.put("https://registry.npmjs.org/is-number/6.0.0", resource("lock/npm/transitive-override/http/is-number-6.0.0"));
        routes.put("https://registry.npmjs.org/is-number/7.0.0", resource("lock/npm/transitive-override/http/is-number-7.0.0"));

        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "^7.0.0", null)).executionContext(ctx),
                packageJson(
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "dependencies": {
                            "is-odd": "3.0.1"
                          }
                        }
                        """,
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "dependencies": {
                            "is-odd": "3.0.1"
                          },
                          "overrides": {
                            "is-number": "^7.0.0"
                          }
                        }
                        """,
                        isOddBringingIsNumber6(),
                        s -> s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("a manifest whose lock was regenerated must not be marked up")
                                .isEmpty())),
                packageLock(
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "lockfileVersion": 3,
                          "requires": true,
                          "packages": {
                            "": {
                              "name": "npm-transitive-override",
                              "version": "1.0.0",
                              "dependencies": {
                                "is-odd": "3.0.1"
                              }
                            },
                            "node_modules/is-number": {
                              "version": "6.0.0",
                              "resolved": "https://registry.npmjs.org/is-number/-/is-number-6.0.0.tgz",
                              "integrity": "sha512-Wu1VHeILBK8KAWJUAiSZQX94GmOE45Rg6/538fKwiloUu21KncEkYGPqob2oSZ5mUT73vLGrHQjKw3KMPwfDzg==",
                              "license": "MIT",
                              "engines": {
                                "node": ">=0.10.0"
                              }
                            },
                            "node_modules/is-odd": {
                              "version": "3.0.1",
                              "resolved": "https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz",
                              "integrity": "sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==",
                              "license": "MIT",
                              "dependencies": {
                                "is-number": "^6.0.0"
                              },
                              "engines": {
                                "node": ">=4"
                              }
                            }
                          }
                        }
                        """,
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "lockfileVersion": 3,
                          "requires": true,
                          "packages": {
                            "": {
                              "name": "npm-transitive-override",
                              "version": "1.0.0",
                              "dependencies": {
                                "is-odd": "3.0.1"
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
                            "node_modules/is-odd": {
                              "version": "3.0.1",
                              "resolved": "https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz",
                              "integrity": "sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==",
                              "license": "MIT",
                              "dependencies": {
                                "is-number": "^6.0.0"
                              },
                              "engines": {
                                "node": ">=4"
                              }
                            }
                          }
                        }
                        """,
                        s -> s.noTrim().afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("successful regeneration must not warn").isEmpty()))
        );
    }

    /**
     * A {@code dependencyPath} run writes a nested override the resolver cannot apply. Leaving the manifest
     * edited while the lock contradicts it reproduces the broken state this recipe exists to prevent, so a
     * refusal must change nothing and warn.
     */
    @Test
    void aRefusedOverrideChangesNothing() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "^7.0.0", "a>is-odd")).executionContext(ctx),
                packageJson(
                        """
                        {
                          "name": "npm-transitive-override",
                          "dependencies": {
                            "is-odd": "3.0.1"
                          }
                        }
                        """,
                        """
                        /*~~(lock regeneration failed: RESOLUTION_REQUIRED [a]: override nested under a is not supported)~~>*/{
                          "name": "npm-transitive-override",
                          "dependencies": {
                            "is-odd": "3.0.1"
                          }
                        }
                        """,
                        isOddBringingIsNumber6()),
                packageLock(
                        """
                        {
                          "name": "npm-transitive-override",
                          "lockfileVersion": 3,
                          "packages": {
                            "": {"name": "npm-transitive-override", "dependencies": {"is-odd": "3.0.1"}},
                            "node_modules/is-odd": {"version": "3.0.1"}
                          }
                        }
                        """,
                        """
                        /*~~(lock regeneration failed: RESOLUTION_REQUIRED [a]: override nested under a is not supported)~~>*/{
                          "name": "npm-transitive-override",
                          "lockfileVersion": 3,
                          "packages": {
                            "": {"name": "npm-transitive-override", "dependencies": {"is-odd": "3.0.1"}},
                            "node_modules/is-odd": {"version": "3.0.1"}
                          }
                        }
                        """,
                        s -> s.noTrim())
        );
    }

    @Test
    void npmScopedOverrideRegeneratesLock() {
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/transitive-override/http/is-odd-3.0.1"));
        routes.put("https://registry.npmjs.org/is-number", resource("lock/npm/transitive-override/http/is-number"));
        routes.put("https://registry.npmjs.org/is-number/6.0.0", resource("lock/npm/transitive-override/http/is-number-6.0.0"));
        routes.put("https://registry.npmjs.org/is-number/7.0.0", resource("lock/npm/transitive-override/http/is-number-7.0.0"));

        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "^7.0.0", "is-odd")).executionContext(ctx),
                packageJson(
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "dependencies": {
                            "is-odd": "3.0.1"
                          }
                        }
                        """,
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "dependencies": {
                            "is-odd": "3.0.1"
                          },
                          "overrides": {
                            "is-odd": {
                              "is-number": "^7.0.0"
                            }
                          }
                        }
                        """,
                        isOddBringingIsNumber6()),
                packageLock(
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "lockfileVersion": 3,
                          "requires": true,
                          "packages": {
                            "": {
                              "name": "npm-transitive-override",
                              "version": "1.0.0",
                              "dependencies": {
                                "is-odd": "3.0.1"
                              }
                            },
                            "node_modules/is-number": {
                              "version": "6.0.0",
                              "resolved": "https://registry.npmjs.org/is-number/-/is-number-6.0.0.tgz",
                              "integrity": "sha512-Wu1VHeILBK8KAWJUAiSZQX94GmOE45Rg6/538fKwiloUu21KncEkYGPqob2oSZ5mUT73vLGrHQjKw3KMPwfDzg==",
                              "license": "MIT",
                              "engines": {
                                "node": ">=0.10.0"
                              }
                            },
                            "node_modules/is-odd": {
                              "version": "3.0.1",
                              "resolved": "https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz",
                              "integrity": "sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==",
                              "license": "MIT",
                              "dependencies": {
                                "is-number": "^6.0.0"
                              },
                              "engines": {
                                "node": ">=4"
                              }
                            }
                          }
                        }
                        """,
                        """
                        {
                          "name": "npm-transitive-override",
                          "version": "1.0.0",
                          "lockfileVersion": 3,
                          "requires": true,
                          "packages": {
                            "": {
                              "name": "npm-transitive-override",
                              "version": "1.0.0",
                              "dependencies": {
                                "is-odd": "3.0.1"
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
                            "node_modules/is-odd": {
                              "version": "3.0.1",
                              "resolved": "https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz",
                              "integrity": "sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==",
                              "license": "MIT",
                              "dependencies": {
                                "is-number": "^6.0.0"
                              },
                              "engines": {
                                "node": ">=4"
                              }
                            }
                          }
                        }
                        """,
                        s -> s.noTrim())
        );
    }

    private static NodeResolutionResult isOddBringingIsNumber6() {
        ResolvedDependency isNumber6 = new ResolvedDependency("is-number", "6.0.0",
                emptyList(), emptyList(), emptyList(), emptyList(),
                singletonMap("node", ">=0.10.0"), "MIT");
        ResolvedDependency isOdd301 = new ResolvedDependency("is-odd", "3.0.1",
                singletonList(new Dependency("is-number", "^6.0.0", isNumber6)),
                emptyList(), emptyList(), emptyList(),
                singletonMap("node", ">=4"), "MIT");
        return new NodeResolutionResult(
                Tree.randomId(), "npm-transitive-override", "1.0.0", null, "package.json", null,
                singletonList(new Dependency("is-odd", "3.0.1", isOdd301)),
                emptyList(), emptyList(), emptyList(), emptyList(),
                asList(isOdd301, isNumber6), PackageManager.Npm, null, null);
    }

    private static String resource(String path) {
        try (InputStream in = UpgradeTransitiveDependencyVersionLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
