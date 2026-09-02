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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcRecipe;
import org.openrewrite.rpc.request.BatchVisit;
import org.openrewrite.rpc.request.BatchVisitResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A tree visited by two recipe bundles in one BatchVisit.
 * <p>
 * With more than one bundle installed the peer runs each behind its own child process, so the two
 * visitors here execute in different interpreters. The renames chain — {@code alpha} to
 * {@code beta} in one bundle, {@code beta} to {@code gamma} in the other — so the result is
 * {@code gamma} only if the first bundle's edit reached the second bundle's input.
 */
class CrossBundleBatchVisitIntegTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .recipeInstallDir(tempDir.resolve("bundles"))
                .log(tempDir.resolve("python-rpc.log")));
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

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void oneBatchVisitSpansTwoBundles() throws IOException {
        PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
        String editAlphaToBeta = install(rpc, "alpha_pkg", "AlphaToBeta", "alpha", "beta");
        String editBetaToGamma = install(rpc, "beta_pkg", "BetaToGamma", "beta", "gamma");

        ExecutionContext ctx = new InMemoryExecutionContext();
        SourceFile cu = PythonParser.builder().build()
                .parse(ctx, "alpha = 1\n")
                .findFirst()
                .orElseThrow();

        List<BatchVisitResponse.BatchVisitResult> results = rpc.batchVisit(cu, ctx, null,
                List.of(new BatchVisit.BatchVisitItem(editAlphaToBeta, null),
                        new BatchVisit.BatchVisitItem(editBetaToGamma, null))).getResults();

        assertThat(results).extracting("modified").containsExactly(true, true);
        SourceFile after = rpc.getObject(cu.getId().toString(),
                DynamicDispatchRpcCodec.canonicalSourceFileType(cu.getClass()));
        assertThat(rpc.print(after)).isEqualTo("gamma = 1\n");
    }

    /**
     * Install a bundle holding one identifier-renaming recipe and return its edit visitor.
     * The entry point is what scopes discovery to this distribution once it is behind a child.
     */
    private String install(PythonRewriteRpc rpc, String dist, String recipe, String from, String to)
            throws IOException {
        Path pkgRoot = tempDir.resolve(dist);
        Path module = pkgRoot.resolve(dist);
        Files.createDirectories(module);
        Files.writeString(pkgRoot.resolve("pyproject.toml"), """
                [project]
                name = "%s"
                version = "0.0.0"

                [project.entry-points."openrewrite.recipes"]
                %s = "%s:activate"
                """.formatted(dist, dist, dist));
        Files.writeString(module.resolve("__init__.py"), """
                from rewrite import CategoryDescriptor, Recipe
                from rewrite.python.visitor import PythonVisitor


                class %s(Recipe):
                    @property
                    def name(self): return "org.openrewrite.python.test.%s"

                    @property
                    def display_name(self): return "%s"

                    @property
                    def description(self): return "Renames `%s` to `%s`."

                    def editor(self):
                        class _V(PythonVisitor):
                            def visit_identifier(self, ident, p):
                                if ident.simple_name == "%s":
                                    return ident.replace(_simple_name="%s")
                                return ident
                        return _V()


                def activate(marketplace):
                    marketplace.install(%s, [CategoryDescriptor(display_name="Test")])
                """.formatted(recipe, recipe, recipe, from, to, from, to, recipe));
        rpc.installRecipes(pkgRoot.toFile());
        return ((RpcRecipe) rpc.prepareRecipe("org.openrewrite.python.test." + recipe, Map.of()))
                .getEditVisitor();
    }
}
