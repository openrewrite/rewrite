/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.rpc;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.javascript.JavaScriptParser;
import org.openrewrite.marker.Markup;
import org.openrewrite.rpc.request.Print;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.javascript.Assertions.javascript;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

@Disabled
class JavaScriptRewriteRpcTest implements RewriteTest {
    @TempDir
    Path tempDir;

    JavaScriptRewriteRpc client;

    @BeforeEach
    void before() {
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder()
          .recipeInstallDir(tempDir)
          .log(tempDir.resolve("rpc.log"))
          .verboseLogging()
        );
        client = JavaScriptRewriteRpc.getOrStart();
    }

    @AfterEach
    void after() throws IOException {
        JavaScriptRewriteRpc.shutdownCurrent();
        System.out.println(Files.readString(tempDir.resolve("rpc.log")));
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false)
          .cycles(1);
    }

    @DocumentExample
    @Test
    void runRecipe() {
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client.prepareRecipe("org.openrewrite.example.npm.change-version",
              Map.of("version", "1.0.0")))
            .expectedCyclesThatMakeChanges(1),
          json(
            """
              {
                "name": "my-project",
                "version": "0.0.1"
              }
              """,
            """
              {
                "name": "my-project",
                "version": "1.0.0"
              }
              """,
            spec -> spec.path("package.json")
          )
        );
    }

    @SuppressWarnings("JSUnusedLocalSymbols")
    @DocumentExample
    @Test
    void runSearchRecipe() {
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client.prepareRecipe("org.openrewrite.example.javascript.find-identifier",
              Map.of("identifier", "hello")))
            .expectedCyclesThatMakeChanges(1),
          javascript(
            "const hello = 'world'",
            "const /*~~>*/hello = 'world'"
          )
        );
    }

    @Test
    void printJava() {
        assertThat(client.installRecipes(new File("rewrite/dist-fixtures/modify-all-trees.js")))
          .isEqualTo(1);
        Recipe modifyAll = client.prepareRecipe("org.openrewrite.java.test.modify-all-trees");

        @Language("java")
        String java = """
          class Test {
          }
          """;
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J preVisit(J tree, ExecutionContext ctx) {
                  SourceFile t = (SourceFile) modifyAll.getVisitor().visitNonNull(tree, ctx);
                  assertThat(t.printAll()).isEqualTo(java.trim());
                  stopAfterPreVisit();
                  return tree;
              }
          })),
          java(
            java
          )
        );
    }

    @Test
    void installRecipesFromNpm() {
        assertThat(client.installRecipes("@openrewrite/recipes-npm")).isEqualTo(1);
        assertThat(client.getRecipes()).satisfiesExactly(
          d -> {
              assertThat(d.getDisplayName()).isEqualTo("Change version in `package.json`");
              assertThat(d.getOptions()).satisfiesExactly(
                o -> assertThat(o.isRequired()).isTrue()
              );
          }
        );
    }

    @Test
    void getRecipes() {
        installRecipes();
        assertThat(client.getRecipes()).isNotEmpty();
    }

    @Test
    void prepareRecipe() {
        installRecipes();
        Recipe recipe = client.prepareRecipe("org.openrewrite.example.npm.change-version",
          Map.of("version", "1.0.0"));
        assertThat(recipe.getDescriptor().getDisplayName()).isEqualTo("Change version in `package.json`");
    }

    @SuppressWarnings("JSUnusedLocalSymbols")
    @Test
    void parseAndPrintJavaScript() {
        // language=javascript
        String source = "const two = 1 + 1";

        SourceFile cu = JavaScriptParser.builder().build().parseInputs(List.of(Parser.Input.fromString(
          Path.of("test.js"), source)), null, new InMemoryExecutionContext()).findFirst().orElseThrow();

        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, Integer p) {
                assertThat(binary.getOperator()).isEqualTo(J.Binary.Type.Addition);
                return binary;
            }
        }.visit(cu, 0);

        assertThat(client.print(cu)).isEqualTo(source);
    }

    @Test
    void printText() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text ->
              assertThat(client.print(text)).isEqualTo("Hello Jon!"))
          )
        );
    }

    @Test
    void printFencedMarker() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text -> {
                text = Markup.info(text, "INFO", null);
                String fence = "{{" + text.getMarkers().getMarkers().getFirst().getId() + "}}";
                assertThat(client.print(text, Print.MarkerPrinter.FENCED)).isEqualTo(fence + "Hello Jon!" + fence);
            })
          )
        );
    }

    @Test
    void printSanitizedMarker() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text -> {
                text = Markup.info(text, "INFO", null);
                assertThat(client.print(text, Print.MarkerPrinter.SANITIZED)).isEqualTo("Hello Jon!");
            })
          )
        );
    }

    @Test
    void printDefaultMarker() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text -> {
                text = Markup.info(text, "INFO", null);
                assertThat(client.print(text, Print.MarkerPrinter.DEFAULT)).isEqualTo("~~(INFO)~~>Hello Jon!");
            })
          )
        );
    }

    @Test
    void printJson() {
        @Language("json")
        String packageJson = """
          {
            "name": "my-project",
            "version": "0.0.1"
          }
          """;
        rewriteRun(
          json(packageJson, spec -> spec.beforeRecipe(json ->
            assertThat(client.print(json)).isEqualTo(packageJson.trim())))
        );
    }

    private void installRecipes() {
        File exampleRecipes = new File("rewrite/dist-fixtures/example-recipe.js");
        assertThat(exampleRecipes).exists();
        assertThat(client.installRecipes(exampleRecipes)).isGreaterThan(0);
    }
}
