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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.python.Assertions.python;

/**
 * A Java recipe edits the file between two Python recipes in one cycle. The peer is sent no Evict until
 * every recipe has run on the file, so the second Python visit sees the Java edit only if the peer
 * fetches the tree again for it.
 */
class JavaEditBetweenPythonVisitsIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @Override
    public void defaults(RecipeSpec spec) {
        // An RpcRecipe is not serializable to/from YAML.
        spec.validateRecipeSerialization(false)
          .typeValidationOptions(TypeValidation.none());
    }

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
    }

    @ParameterizedTest(name = "bundles behind children: {0}")
    @ValueSource(booleans = {false, true})
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void secondPythonVisitSeesJavaEdit(boolean behindChildren) throws IOException {
        PythonRewriteRpc.Builder builder = PythonRewriteRpc.builder().log(tempDir.resolve("python-rpc.log"));
        PythonRewriteRpc.setFactory(behindChildren ? builder.recipeInstallDir(tempDir.resolve("bundles")) : builder);
        PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
        Recipe alphaToBeta = install(rpc, "alpha_pkg", "AlphaToBeta", "alpha", "beta");
        Recipe betaToGamma = install(rpc, "beta_pkg", "BetaToGamma", "beta", "gamma");

        rewriteRun(
          spec -> spec
            .recipes(alphaToBeta, new ChangeMethodName("str upper(..)", "toUpperCase", false, false), betaToGamma)
            .cycles(1)
            .expectedCyclesThatMakeChanges(1),
          python(
            """
              alpha = "hello".upper()
              """,
            """
              gamma = "hello".toUpperCase()
              """
          )
        );
    }

    /**
     * Install a bundle holding one identifier-renaming recipe. The entry point is what scopes
     * discovery to this distribution once it is behind a child.
     */
    private Recipe install(PythonRewriteRpc rpc, String dist, String recipe, String from, String to)
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
        return rpc.prepareRecipe("org.openrewrite.python.test." + recipe, Map.of());
    }
}
