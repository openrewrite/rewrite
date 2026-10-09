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
package org.openrewrite.python;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.python.Assertions.pipfile;
import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.python;

/**
 * Python recipes run over RPC against the TOML files the JVM parses ({@code pyproject.toml},
 * {@code Pipfile}): the documents reach the Python peer, and its edits come back.
 */
class PythonTomlRecipeIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() throws IOException {
        PythonRewriteRpc.shutdownCurrent();
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
        Path log = tempDir.resolve("python-rpc.log");
        if (Files.exists(log)) {
            System.out.println("=== Python RPC Log ===");
            System.out.println(Files.readString(log));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void pythonScanningRecipeSeesPyprojectAndPipfile() throws IOException {
        // given
        Recipe recipe = prepare("org.openrewrite.python.test.RecordScannedSourcePaths");

        // when
        rewriteRun(
                spec -> spec.recipe(recipe),
                pyproject(
                        """
                                [project]
                                name = "demo"
                                version = "0.1.0"
                                """
                ),
                pipfile(
                        """
                                [packages]
                                """
                ),
                // then
                python(
                        """
                                print("hi")
                                """,
                        """
                                /*~~(Pipfile, main.py, pyproject.toml)~~>*/print("hi")
                                """,
                        spec -> spec.path("main.py")
                )
        );
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void pythonTomlVisitorEditsEveryLiteralType() throws IOException {
        // given
        Recipe recipe = prepare("org.openrewrite.python.test.SetTomlLiterals");

        // when
        rewriteRun(
                spec -> spec.recipe(recipe),
                pyproject(
                        """
                                [project]
                                name = "demo"
                                version = "0.1.0" # bumped

                                [tool.demo]
                                count = 42
                                ratio = 3.25
                                enabled = true
                                released = 1979-05-27
                                built = 1979-05-27T07:32:00
                                at = 07:32:00
                                stamp = 1979-05-27T07:32:00-08:00
                                untouched = [ 1, { x = 2 } ]
                                """,
                        """
                                [project]
                                name = "demo"
                                version = "0.2.0" # bumped

                                [tool.demo]
                                count = 43
                                ratio = inf
                                enabled = false
                                released = 2026-10-02
                                built = 2026-10-02T12:30:00
                                at = 12:30:00.500000
                                stamp = 2026-10-02T12:30:00+02:00
                                untouched = [ 1, { x = 2 } ]
                                """,
                        // then
                        spec -> spec.afterRecipe(doc -> assertThat(literalValues(doc)).containsExactly(
                                entry("name", "demo"),
                                entry("version", "0.2.0"),
                                entry("count", 43L),
                                entry("ratio", Double.POSITIVE_INFINITY),
                                entry("enabled", false),
                                entry("released", LocalDate.of(2026, 10, 2)),
                                entry("built", LocalDateTime.of(2026, 10, 2, 12, 30)),
                                entry("at", LocalTime.of(12, 30, 0, 500_000_000)),
                                entry("stamp", OffsetDateTime.parse("2026-10-02T12:30:00+02:00")),
                                entry("x", 2L)
                        ))
                ),
                pipfile(
                        """
                                [packages]
                                requests = "==2.31.0"
                                """,
                        """
                                [packages]
                                requests = "==2.32.0"
                                """
                ),
                python(
                        """
                                version = "0.1.0"
                                """
                )
        );
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void pythonRecipeReadsPythonResolutionResultOfPyproject() throws IOException {
        // given
        Recipe recipe = prepare("org.openrewrite.python.test.FindProjectName");

        // when
        rewriteRun(
                spec -> spec.recipe(recipe),
                pyproject(
                        """
                                [project]
                                name = "demo"
                                version = "0.1.0"
                                """,
                        // then
                        """
                                ~~(demo 0.1.0)~~>[project]
                                name = "demo"
                                version = "0.1.0"
                                """
                )
        );
    }

    private static Map<String, Object> literalValues(Toml.Document doc) {
        Map<String, Object> values = new LinkedHashMap<>();
        new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, Integer p) {
                if (keyValue.getValue() instanceof Toml.Literal) {
                    values.put(((Toml.Identifier) keyValue.getKey()).getName(), ((Toml.Literal) keyValue.getValue()).getValue());
                }
                return super.visitKeyValue(keyValue, p);
            }
        }.visit(doc, 0);
        return values;
    }

    private Recipe prepare(String recipeName) throws IOException {
        Path pkgRoot = tempDir.resolve("toml_pkg");
        Path module = pkgRoot.resolve("rewrite_test_toml_recipes");
        Files.createDirectories(module);
        Files.writeString(pkgRoot.resolve("pyproject.toml"), """
                [project]
                name = "rewrite_test_toml_recipes"
                version = "0.0.0"
                """);
        Files.writeString(module.resolve("__init__.py"), """
                import math
                from datetime import date, datetime, time, timedelta, timezone
                from typing import Any, Optional, Set

                from rewrite import ScanningRecipe, Recipe, SearchResult, SourceFile, Tree, TreeVisitor
                from rewrite.marketplace import Python
                from rewrite.python.markers import PythonResolutionResult
                from rewrite.toml import Document, Identifier, Literal, TomlVisitor


                class _Scanner(TreeVisitor):
                    def __init__(self, acc: Set[str]):
                        super().__init__()
                        self.acc = acc

                    def visit(self, tree: Optional[Tree], p: Any, parent=None) -> Optional[Tree]:
                        if isinstance(tree, SourceFile):
                            self.acc.add(str(tree.source_path))
                        return tree


                class _Marker(TreeVisitor):
                    def __init__(self, acc: Set[str]):
                        super().__init__()
                        self.acc = acc

                    def visit(self, tree: Optional[Tree], p: Any, parent=None) -> Optional[Tree]:
                        if not isinstance(tree, SourceFile) or str(tree.source_path) != "main.py":
                            return tree
                        if any(isinstance(m, SearchResult) for m in tree.markers.markers):
                            return tree
                        return SearchResult.found(tree, ", ".join(sorted(self.acc)))


                class RecordScannedSourcePaths(ScanningRecipe[Set[str]]):
                    @property
                    def name(self):
                        return "org.openrewrite.python.test.RecordScannedSourcePaths"

                    @property
                    def display_name(self):
                        return "Record scanned source paths"

                    @property
                    def description(self):
                        return "Marks main.py with every source path the scanner was handed."

                    def initial_value(self, ctx) -> Set[str]:
                        return set()

                    def scanner(self, acc: Set[str]) -> TreeVisitor:
                        return _Scanner(acc)

                    def editor_with_data(self, acc: Set[str]) -> TreeVisitor:
                        return _Marker(acc)


                NEW_LITERALS = {
                    "version": ('"0.2.0"', "0.2.0"),
                    "count": ("43", 43),
                    "ratio": ("inf", math.inf),
                    "enabled": ("false", False),
                    "released": ("2026-10-02", date(2026, 10, 2)),
                    "built": ("2026-10-02T12:30:00", datetime(2026, 10, 2, 12, 30)),
                    "at": ("12:30:00.500000", time(12, 30, 0, 500000)),
                    "stamp": ("2026-10-02T12:30:00+02:00",
                              datetime(2026, 10, 2, 12, 30, tzinfo=timezone(timedelta(hours=2)))),
                    "requests": ('"==2.32.0"', "==2.32.0"),
                }


                class _SetLiterals(TomlVisitor):
                    def visit_key_value(self, key_value, p):
                        key_value = super().visit_key_value(key_value, p)
                        if not isinstance(key_value.key, Identifier) or not isinstance(key_value.value, Literal):
                            return key_value
                        new = NEW_LITERALS.get(key_value.key.name)
                        if new is None or key_value.value.source == new[0]:
                            return key_value
                        return key_value.replace(value=key_value.value.replace(source=new[0], value=new[1]))


                class SetTomlLiterals(Recipe):
                    @property
                    def name(self):
                        return "org.openrewrite.python.test.SetTomlLiterals"

                    @property
                    def display_name(self):
                        return "Set TOML literals"

                    @property
                    def description(self):
                        return "Sets a literal of every TOML type."

                    def editor(self) -> TreeVisitor:
                        return _SetLiterals()


                class _FindProjectName(TomlVisitor):
                    def visit_document(self, document, p):
                        resolution = document.markers.find_first(PythonResolutionResult)
                        if resolution is None or not document.values:
                            return document
                        first = document.values[0]
                        if any(isinstance(m, SearchResult) for m in first.markers.markers):
                            return document
                        found = SearchResult.found(first, f"{resolution.name} {resolution.version}")
                        return document.replace(values=[found] + list(document.values[1:]))


                class FindProjectName(Recipe):
                    @property
                    def name(self):
                        return "org.openrewrite.python.test.FindProjectName"

                    @property
                    def display_name(self):
                        return "Find project name"

                    @property
                    def description(self):
                        return "Marks pyproject.toml with the project name and version of its PythonResolutionResult."

                    def editor(self) -> TreeVisitor:
                        return _FindProjectName()


                def activate(marketplace):
                    marketplace.install(RecordScannedSourcePaths, Python)
                    marketplace.install(SetTomlLiterals, Python)
                    marketplace.install(FindProjectName, Python)
                """);
        PythonRewriteRpc.getOrStart().installRecipes(pkgRoot.toFile());
        return PythonRewriteRpc.getOrStart().prepareRecipe(recipeName, Map.of());
    }
}
