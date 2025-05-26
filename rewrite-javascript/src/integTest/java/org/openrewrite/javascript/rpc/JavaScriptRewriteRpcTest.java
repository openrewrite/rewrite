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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

@Disabled
class JavaScriptRewriteRpcTest implements RewriteTest {
    JavaScriptRewriteRpc client;
    PrintStream log;

    @BeforeEach
    void before() throws FileNotFoundException {
        this.log = new PrintStream(new FileOutputStream("rpc.java.log"));
        this.client = JavaScriptRewriteRpc.start(
          Environment.builder().build(),
          "node",
          "--enable-source-maps",
          // Uncomment this to debug the server
//          "--inspect-brk",
          "./rewrite/dist/src/rpc/server.js"
        );

        client.batchSize(20)
          .timeout(Duration.ofMinutes(10))
          .traceGetObjectOutput()
          .traceGetObjectInput(log);
    }

    @AfterEach
    void after() {
        log.close();
        client.shutdown();
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

    @Test
    void printJava() {
        assertThat(client.installRecipes(new File("rewrite/dist/test/modify-all-trees.js")))
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
              public J preVisit(@NonNull J tree, @NonNull ExecutionContext ctx) {
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

    @Test
    void parseAndPrintJavaScript() {
        // language=javascript
        String source = "const two = 1 + 1";

        SourceFile cu = client.parse("javascript", List.of(Parser.Input.fromString(
          Paths.get("test.js"), source)), null).getFirst();

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
        File exampleRecipes = new File("rewrite/dist/test/example-recipe.js");
        assertThat(exampleRecipes).exists();
        assertThat(client.installRecipes(exampleRecipes)).isGreaterThan(0);
    }
}
