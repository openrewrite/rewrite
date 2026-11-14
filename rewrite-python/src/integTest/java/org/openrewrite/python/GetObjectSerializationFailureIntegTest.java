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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.python.Assertions.python;

class GetObjectSerializationFailureIntegTest implements RewriteTest {

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
    void serializationFailureOfARecipeResultReportsThePythonCause() throws IOException {
        installFixture();
        Recipe recipe = client().prepareRecipe("org.openrewrite.python.test.AddCodeclessMarker", Map.of());

        assertThatThrownBy(() -> rewriteRun(
                spec -> spec.recipe(recipe),
                python("x = 1")
        ))
                .hasStackTraceContaining("No RPC codec is registered for the marker CodeclessMarker");
    }

    private void installFixture() throws IOException {
        Path pkgRoot = tempDir.resolve("codecless_marker_pkg");
        Path module = pkgRoot.resolve("rewrite_test_codecless_marker");
        Files.createDirectories(module);
        Files.writeString(pkgRoot.resolve("pyproject.toml"), """
                [project]
                name = "rewrite_test_codecless_marker"
                version = "0.0.0"
                """);
        Files.writeString(module.resolve("__init__.py"), """
                from typing import Any
                from uuid import UUID

                from rewrite import ExecutionContext, Recipe, TreeVisitor, random_id
                from rewrite.marketplace import Python
                from rewrite.markers import Marker
                from rewrite.python.visitor import PythonVisitor
                from rewrite.utils import lst_dataclass


                @lst_dataclass
                class CodeclessMarker(Marker):
                    _id: int | UUID


                class AddCodeclessMarker(Recipe):
                    @property
                    def name(self):
                        return "org.openrewrite.python.test.AddCodeclessMarker"

                    @property
                    def display_name(self):
                        return "Add a marker that has no RPC codec"

                    @property
                    def description(self):
                        return "Attaches a marker to the compilation unit that the RPC layer cannot serialize."

                    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
                        class Visitor(PythonVisitor[ExecutionContext]):
                            def visit_compilation_unit(self, cu, p):
                                cu = super().visit_compilation_unit(cu, p)
                                return cu.replace(markers=cu.markers.add(CodeclessMarker(random_id())))

                        return Visitor()


                def activate(marketplace):
                    marketplace.install(AddCodeclessMarker, Python)
                """);
        client().installRecipes(pkgRoot.toFile());
    }

    private PythonRewriteRpc client() {
        return PythonRewriteRpc.getOrStart();
    }
}
