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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.ChangeValue;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

/**
 * A recipe that visits the lock before its package.json computes the edit from the scanned package.json; a change an
 * earlier recipe in the run made to it must survive.
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
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true,
                        false)));
        spec.executionContext(ctx);
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
                        """,
                        s -> s.noTrim()
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

    /**
     * An earlier recipe sets the range of the dependency that is then renamed, so the lock has to be written from
     * the range as that recipe left it, whether the project's lock is visited before its package.json or after, and
     * whether the project is the repository's only one or a lock root nested in another project.
     */
    @ParameterizedTest(name = "nested: {0}")
    @ValueSource(booleans = {false, true})
    void renameInstallsTheRangeAnEarlierRecipeSet(boolean nested) {
        String dir = nested ? "backend/" : "";
        String manifest = fixture("pkg-before").replace("\"is-number\": \"^6.0.0\"",
                "\"is-number\": \"^6.0.0\",\n    \"pad-left\": \"^0.0.1\"");
        Map<String, String> before = new LinkedHashMap<>();
        Map<String, String> after = new LinkedHashMap<>();
        before.put(dir + "package.json", manifest);
        after.put(dir + "package.json", manifest.replace("\"pad-left\": \"^0.0.1\"", "\"left-pad\": \"^1.3.0\""));
        before.put(dir + "package-lock.json", fixture("before"));
        after.put(dir + "package-lock.json", fixture("after"));
        if (nested) {
            before.put("package.json", fixture("pkg-before"));
            after.put("package.json", fixture("pkg-before"));
            before.put("package-lock.json", fixture("before"));
            after.put("package-lock.json", fixture("before"));
        }
        Recipe recipe = new CompositeRecipe(Arrays.asList(
                new ChangeValue("$.dependencies.pad-left", "\"^1.3.0\""),
                new ChangeDependency("pad-left", "left-pad", null, null)));

        List<String> failures = new ArrayList<>();
        for (List<String> order : orders(new ArrayList<>(before.keySet()))) {
            ExecutionContext ctx = addLeafRegistry();
            List<SourceFile> sources = new ArrayList<>();
            for (String path : order) {
                sources.add(parse(ctx, path, before.get(path)));
            }
            RecipeRun run = recipe.run(new InMemoryLargeSourceSet(sources), ctx);
            Map<String, String> actual = new LinkedHashMap<>(before);
            for (Result result : run.getChangeset().getAllResults()) {
                SourceFile sf = Objects.requireNonNull(result.getAfter());
                actual.put(sf.getSourcePath().toString(),
                        sf.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)));
                sf.getMarkers().findFirst(Markup.Warn.class).ifPresent(warn ->
                        failures.add(order + ": " + sf.getSourcePath() + " is warned " + warn.getMessage()));
            }
            for (String path : after.keySet()) {
                if (!after.get(path).equals(actual.get(path))) {
                    failures.add(order + ": " + path + " is\n" + actual.get(path));
                }
            }
            for (NodeLockRegenerationFailures.Row row : run.getDataTableRows(NodeLockRegenerationFailures.class)) {
                failures.add(order + ": " + row.getSourcePath() + " failed to regenerate: " + row.getDetail());
            }
        }
        assertThat(failures).isEmpty();
    }

    @Test
    void aRunWithoutAnotherCycleReportsTheDeferredManifest() {
        String manifest = fixture("pkg-before").replace("\"is-number\": \"^6.0.0\"",
                "\"is-number\": \"^6.0.0\",\n    \"pad-left\": \"^0.0.1\"");
        ExecutionContext ctx = addLeafRegistry();
        Recipe recipe = new CompositeRecipe(Arrays.asList(
                new ChangeValue("$.dependencies.pad-left", "\"^1.3.0\""),
                new ChangeDependency("pad-left", "left-pad", null, null)));
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(Arrays.asList(
                parse(ctx, "package-lock.json", fixture("before")), parse(ctx, "package.json", manifest))), ctx, 1);
        assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class))
                .singleElement()
                .satisfies(row -> assertThat(row.getDetail()).contains("a recipe before this one changed package.json"));
        assertThat(run.getChangeset().getAllResults())
                .anySatisfy(result -> assertThat(Objects.requireNonNull(result.getAfter()).getMarkers()
                        .findFirst(Markup.Warn.class)).isPresent());
    }

    private static Json.Document parse(ExecutionContext ctx, String path, String text) {
        Json.Document doc = (Json.Document) JsonParser.builder().build().parse(ctx, text).findFirst().orElseThrow();
        doc = doc.withSourcePath(Paths.get(path));
        if (path.endsWith("package.json")) {
            doc = doc.withMarkers(doc.getMarkers().add(text.contains("pad-left") ?
                    nodeResolutionResult(PackageManager.Npm, dependency("is-number", "^6.0.0"),
                            dependency("pad-left", "^0.0.1")) :
                    nodeResolutionResult(PackageManager.Npm, dependency("is-number", "^6.0.0"))));
        }
        return doc;
    }

    private static ExecutionContext addLeafRegistry() {
        Map<String, String> routes = new HashMap<>();
        routes.put("https://registry.npmjs.org/left-pad", fixture("http/left-pad"));
        routes.put("https://registry.npmjs.org/left-pad/1.3.0", fixture("http/left-pad-1.3.0"));
        routes.put("https://registry.npmjs.org/is-number", fixture("http/is-number"));
        routes.put("https://registry.npmjs.org/is-number/6.0.0", fixture("http/is-number-6.0.0"));
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        });
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true,
                        false)));
        return ctx;
    }

    private static List<List<String>> orders(List<String> paths) {
        if (paths.size() <= 1) {
            return singletonList(paths);
        }
        List<List<String>> orders = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            List<String> rest = new ArrayList<>(paths);
            String first = rest.remove(i);
            for (List<String> tail : orders(rest)) {
                List<String> order = new ArrayList<>();
                order.add(first);
                order.addAll(tail);
                orders.add(order);
            }
        }
        return orders;
    }

    private static String fixture(String name) {
        try (InputStream in = DependencyEditAfterEarlierPackageJsonChangeTest.class.getClassLoader()
                .getResourceAsStream("lock/npm/add-leaf/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
