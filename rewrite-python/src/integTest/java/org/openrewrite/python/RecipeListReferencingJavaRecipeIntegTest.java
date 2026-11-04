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
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.marketplace.RecipeBundle.runtimeClasspath;
import static org.openrewrite.test.SourceSpecs.text;

/**
 * End-to-end proof of the Python -> Java direction of cross-language composition:
 * a pure-{@code recipe_list()} Python composite (no {@code editor()} of its own)
 * lists a Java recipe by id, with options passed as plain snake_case kwargs.
 * <p>
 * The JVM is the orchestrator. It materializes the Python composite over RPC as
 * an {@link org.openrewrite.rpc.RpcRecipe}; in {@code getRecipeList()} it
 * round-trips {@code PrepareRecipe} for the Java child id, the Python peer
 * answers with a {@code delegatesTo} response, and the JVM instantiates the real
 * Java recipe natively. Because the recipe runs natively, its full
 * {@link org.openrewrite.ScanningRecipe} lifecycle (scan + generate + edit) and
 * non-Python source files (here a plain-text file) work without any special
 * handling.
 * <p>
 * The Java child {@code org.openrewrite.text.AppendToTextFile} is a
 * {@code ScanningRecipe} that edits a non-Python file; its options exercise the
 * snake_case -> camelCase mapping ({@code relative_file_name} ->
 * {@code relativeFileName}, {@code existing_file_strategy} ->
 * {@code existingFileStrategy}) and string-to-enum coercion ({@code "Continue"}).
 * <p>
 * The Python composite is supplied as a test-only fixture package installed into
 * the peer via the {@code InstallRecipes} RPC, and the delegated Java recipe is
 * made resolvable to the JVM's {@code delegatesTo} consumer by a marketplace
 * scanned from the runtime classpath (mirroring {@code RewriteRpcTest}). No
 * production code or shipped recipe is involved.
 */
class RecipeListReferencingJavaRecipeIntegTest implements RewriteTest {

    Environment env = Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.text")
            .build();
    RecipeMarketplace marketplace = env.toMarketplace(runtimeClasspath());

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .marketplace(marketplace)
                .resolvers(List.of(new TestRecipeBundleResolver()))
                .log(tempDir.resolve("python-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() throws IOException {
        PythonRewriteRpc.shutdownCurrent();
        // Reset factory to default so other tests don't inherit a log path
        // pointing at this test's (soon-to-be-deleted) temp directory.
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
        Path log = tempDir.resolve("python-rpc.log");
        if (Files.exists(log)) {
            System.out.println("=== Python RPC Log ===");
            System.out.println(Files.readString(log));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        // An RpcRecipe is not serializable to/from YAML.
        spec.validateRecipeSerialization(false);
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void pureRecipeListCompositeDelegatesToJavaScanningRecipe() throws IOException {
        installCompositeFixture();

        Recipe composite = client().prepareRecipe(
                "org.openrewrite.python.test.AppendViaJavaRecipe", Map.of());

        rewriteRun(
                spec -> spec.recipe(composite),
                text(
                        """
                                existing
                                """,
                        """
                                existing
                                content
                                """,
                        spec -> spec.path("file.txt").noTrim()
                )
        );
    }

    /**
     * Write a minimal Python package whose {@code activate()} installs a pure
     * {@code recipe_list()} composite that references a Java recipe, then install
     * it into the running peer via the {@code InstallRecipes} RPC.
     */
    private void installCompositeFixture() throws IOException {
        Path pkgRoot = tempDir.resolve("composite_pkg");
        Path module = pkgRoot.resolve("rewrite_test_composite");
        Files.createDirectories(module);
        Files.writeString(pkgRoot.resolve("pyproject.toml"), """
                [project]
                name = "rewrite_test_composite"
                version = "0.0.0"
                """);
        Files.writeString(module.resolve("__init__.py"), """
                from rewrite import Recipe
                from rewrite.marketplace import Python
                from rewrite.rpc.rpc_recipe import RpcRecipe


                class AppendViaJavaRecipe(Recipe):
                    @property
                    def name(self):
                        return "org.openrewrite.python.test.AppendViaJavaRecipe"

                    @property
                    def display_name(self):
                        return "Append via Java recipe"

                    @property
                    def description(self):
                        return "Pure recipe_list composite that delegates to a Java ScanningRecipe."

                    def recipe_list(self):
                        return [
                            RpcRecipe(
                                "org.openrewrite.text.AppendToTextFile",
                                relativeFileName="file.txt",
                                content="content",
                                preamble="preamble",
                                appendNewline=True,
                                existingFileStrategy="Continue",
                            ),
                        ]


                def activate(marketplace):
                    marketplace.install(AppendViaJavaRecipe, Python)
                """);
        client().installRecipes(pkgRoot.toFile());
    }

    private PythonRewriteRpc client() {
        return PythonRewriteRpc.getOrStart();
    }

    /**
     * Resolves recipes straight from the runtime-classpath marketplace, so the
     * JVM's {@code delegatesTo} consumer (which does a marketplace lookup) can
     * find and natively instantiate the delegated Java recipe. Mirrors the
     * resolver in {@code RewriteRpcTest}.
     */
    class TestRecipeBundleResolver implements RecipeBundleResolver {
        @Override
        public String getEcosystem() {
            return "runtime";
        }

        @Override
        public RecipeBundleReader resolve(RecipeBundle bundle) {
            return new RecipeBundleReader() {
                @Override
                public RecipeBundle getBundle() {
                    return bundle;
                }

                @Override
                public RecipeMarketplace read() {
                    return marketplace;
                }

                @Override
                public RecipeDescriptor describe(RecipeListing listing) {
                    return env.activateRecipes(listing.getName()).getDescriptor();
                }

                @Override
                public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
                    return new RecipeLoader(null).load(listing.getName(), options);
                }
            };
        }
    }
}
