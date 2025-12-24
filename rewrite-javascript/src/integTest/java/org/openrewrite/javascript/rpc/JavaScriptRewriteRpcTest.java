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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.javascript.JavaScriptIsoVisitor;
import org.openrewrite.javascript.JavaScriptParser;
import org.openrewrite.javascript.style.Autodetect;
import org.openrewrite.marker.Markup;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.rpc.request.Print;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.javascript.Assertions.*;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

@SuppressWarnings("JSUnusedLocalSymbols")
class JavaScriptRewriteRpcTest implements RewriteTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder()
          .recipeInstallDir(tempDir)
          .metricsCsv(tempDir.resolve("rpc.csv"))
          .log(tempDir.resolve("rpc.log"))
          .traceRpcMessages()
        );
    }

    @AfterEach
    void after() throws IOException {
        JavaScriptRewriteRpc.shutdownCurrent();
        if (Files.exists(tempDir.resolve("rpc.csv"))) {
            System.out.println(Files.readString(tempDir.resolve("rpc.csv")));
        }
        if (Files.exists(tempDir.resolve("rpc.log"))) {
            System.out.println(Files.readString(tempDir.resolve("rpc.log")));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @DocumentExample
    @Test
    void runRecipe() {
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.example.npm.change-version",
              Map.of("version", "1.0.0"))),
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
    void printSubtree() {
        rewriteRun(
          typescript(
            "console.log('hello');",
            spec -> spec.beforeRecipe(cu -> new JavaScriptIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    //language=typescript
                    assertThat(client().print(method, getCursor().getParentOrThrow())).isEqualTo("console.log('hello')");
                    return method;
                }
            }.visit(cu, 0))
          )
        );
    }

    @SuppressWarnings("JSUnusedLocalSymbols")
    @Test
    void runSearchRecipe() {
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.example.javascript.find-identifier",
              Map.of("identifier", "hello"))),
          javascript(
            "const hello = 'world'",
            "const /*~~>*/hello = 'world'"
          )
        );
    }

    @ParameterizedTest
    @SuppressWarnings("JSUnusedLocalSymbols")
    @ValueSource(booleans = {true, false})
    void runSearchRecipeWithJavaRecipeActingAsPrecondition(boolean matchesPrecondition) {
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.example.javascript.remote-find-identifier-with-path",
              Map.of("identifier", "hello", "requiredPath", "hello.js"))),
          matchesPrecondition ?
            javascript(
              "const hello = 'world'",
              "const /*~~>*/hello = 'world'",
              spec -> spec.path("hello.js")
            ) :
            javascript(
              "const hello = 'world'",
              spec -> spec.path("not-hello.js")
            )
        );
    }

    @Test
    void printJava() {
        assertThat(client().installRecipes(new File("rewrite/dist-fixtures/modify-all-trees.js")).getRecipesInstalled())
          .isEqualTo(1);
        Recipe modifyAll = client().prepareRecipe("org.openrewrite.java.test.modify-all-trees");

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
        assertThat(client().installRecipes("@openrewrite/recipes-npm").getRecipesInstalled()).isEqualTo(1);
        assertThat(client().getMarketplace(new RecipeBundle("npm", "@openrewrite/recipes-npm", null, null, null)).getAllRecipes()).satisfiesExactly(
          d -> {
              assertThat(d.getDisplayName()).isEqualTo("Change version in `package.json`");
          }
        );
    }

    @Test
    void getRecipes() {
        installRecipes();
        assertThat(client().getMarketplace(new RecipeBundle("npm", "@openrewrite/recipes-npm", null, null, null))
          .getAllRecipes()).isNotEmpty();
    }

    @Test
    void prepareRecipe() {
        installRecipes();
        Recipe recipe = client().prepareRecipe("org.openrewrite.example.npm.change-version",
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

        assertThat(client().print(cu)).isEqualTo(source);
    }

    @Test
    void printText() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text ->
              assertThat(client().print(text)).isEqualTo("Hello Jon!"))
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
                assertThat(client().print(text, Print.MarkerPrinter.FENCED)).isEqualTo(fence + "Hello Jon!" + fence);
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
                assertThat(client().print(text, Print.MarkerPrinter.SANITIZED)).isEqualTo("Hello Jon!");
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
                assertThat(client().print(text, Print.MarkerPrinter.DEFAULT)).isEqualTo("~~(INFO)~~>Hello Jon!");
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
            assertThat(client().print(json)).isEqualTo(packageJson.trim())))
        );
    }

    @SuppressWarnings({"TypeScriptCheckImport", "JSUnusedLocalSymbols"})
    @Test
    void javaTypeAcrossRpcBoundary(@TempDir Path projectDir) {
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.example.javascript.mark-class-types", Map.of())),
          npm(
            projectDir,
            typescript(
              """
                import _ from 'lodash';
                const result = _.map([1, 2, 3], n => n * 2);
                """,
              """
                import /*~~(_.LoDashStatic)~~>*/_ from 'lodash';
                const result = /*~~(_.LoDashStatic)~~>*/_.map([1, 2, 3], n => n * 2);
                """
            ),
            packageJson(
              """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  },
                  "devDependencies": {
                    "@types/lodash": "^4.14.195"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void runScanningRecipeThatEdits() {
        // This test verifies that the accumulator from the scanning phase
        // is correctly passed to the editor phase over RPC.
        installRecipes();
        rewriteRun(
          spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.example.text.scanning-editor", Map.of()))
            .cycles(1)
            .expectedCyclesThatMakeChanges(1),
          text("file1", "file1 (count: 2)"),
          text("file2", "file2 (count: 2)")
        );
    }

    @Test
    void environmentVariableIsSetRemotely() {
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder()
          .recipeInstallDir(tempDir)
          .environment(Map.of("HTTPS_PROXY", "http://unused:3128"))
        );
        installRecipes();

        rewriteRun(spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.example.javascript.replace-assignment",
              Map.of("variable", "HTTPS_PROXY"))),
          javascript(
            "const v = 'value'",
            "const v = 'http://unused:3128'"
          )
        );
    }

    @Test
    void parseProject(@TempDir Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("package.json"), """
          {"name": "test-project", "version": "1.0.0"}
          """);
        Files.writeString(projectDir.resolve("index.js"), "const x = 1;");
        Files.writeString(projectDir.resolve("other.js"), "const y = 2;");

        List<SourceFile> sourceFiles = client()
          .parseProject(projectDir, new InMemoryExecutionContext())
          .toList();

        assertThat(sourceFiles).hasSize(3);

        List<String> paths = sourceFiles.stream()
          .map(sf -> sf.getSourcePath().toString())
          .toList();
        assertThat(paths).containsExactlyInAnyOrder("package.json", "index.js", "other.js");

        // Verify content is parseable and printable
        for (SourceFile sf : sourceFiles) {
            assertThat(sf).isNotInstanceOf(ParseError.class);
            assertThat(client().print(sf)).isNotEmpty();
        }

        // Verify that both JS files share the same Autodetect marker instance (deduplication)
        SourceFile indexJs = sourceFiles.stream()
          .filter(sf -> sf.getSourcePath().toString().equals("index.js"))
          .findFirst().orElseThrow();
        SourceFile otherJs = sourceFiles.stream()
          .filter(sf -> sf.getSourcePath().toString().equals("other.js"))
          .findFirst().orElseThrow();

        Autodetect indexAutodetect = indexJs.getMarkers().findFirst(Autodetect.class).orElseThrow();
        Autodetect otherAutodetect = otherJs.getMarkers().findFirst(Autodetect.class).orElseThrow();

        assertThat(indexAutodetect).isSameAs(otherAutodetect);
    }

    @Test
    void parseProjectWithExclusions(@TempDir Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("package.json"), """
          {"name": "test-project", "version": "1.0.0"}
          """);
        Files.writeString(projectDir.resolve("index.js"), "const x = 1;");
        Files.createDirectories(projectDir.resolve("vendor"));
        Files.writeString(projectDir.resolve("vendor/external.js"), "const y = 2;");

        List<SourceFile> sourceFiles = client()
          .parseProject(projectDir, List.of("**/vendor/**"), new InMemoryExecutionContext())
          .toList();

        assertThat(sourceFiles).hasSize(2);

        List<String> paths = sourceFiles.stream()
          .map(sf -> sf.getSourcePath().toString())
          .toList();
        assertThat(paths)
          .containsExactlyInAnyOrder("package.json", "index.js")
          .noneMatch(p -> p.contains("vendor"));
    }

    private void installRecipes() {
        File exampleRecipes = new File("rewrite/dist-fixtures/example-recipe.js");
        assertThat(exampleRecipes).exists();
        assertThat(client().installRecipes(exampleRecipes).getRecipesInstalled()).isGreaterThan(0);
    }

    private JavaScriptRewriteRpc client() {
        return JavaScriptRewriteRpc.getOrStart();
    }
}
