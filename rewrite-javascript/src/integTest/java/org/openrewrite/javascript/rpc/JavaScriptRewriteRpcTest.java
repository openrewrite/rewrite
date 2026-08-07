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
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.javascript.JavaScriptIsoVisitor;
import org.openrewrite.javascript.JavaScriptParser;
import org.openrewrite.javascript.UpgradeDependencyVersion;
import org.openrewrite.javascript.style.Autodetect;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.marker.Markup;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.RpcRecipe;
import org.openrewrite.rpc.request.Print;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;
import org.openrewrite.yaml.tree.Yaml;

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
        // Restore the default factory so this test's per-test @TempDir-backed
        // factory does not leak into later test classes that lazily (re)start
        // the RPC process on the same thread.
        JavaScriptRewriteRpc.resetFactory();
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
    void startsWhenLogParentDirectoryIsMissing() {
        // given a log path whose parent directory does not exist yet (mimics a
        // torn-down @TempDir that a stale factory still references)
        Path missingParent = tempDir.resolve("does-not-exist-yet");
        Path log = missingParent.resolve("rpc.log");
        assertThat(Files.exists(missingParent)).isFalse();

        // when starting the RPC process configured to log there
        JavaScriptRewriteRpc rpc = JavaScriptRewriteRpc.builder()
          .recipeInstallDir(tempDir)
          .log(log)
          .get();

        // then the process starts and the log (with its parent) is created
        // rather than failing with NoSuchFileException
        try {
            assertThat(rpc).isNotNull();
            assertThat(Files.exists(log)).isTrue();
        } finally {
            rpc.shutdown();
        }
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
                  var t = (SourceFile) modifyAll.getVisitor().visitNonNull(tree, ctx);
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
        assertThat(recipe.getDescriptor().getOptions().size()).isEqualTo(1);
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
    void parseProject(@TempDir Path projectDir) throws Exception {
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
    void parseProjectWithExclusions(@TempDir Path projectDir) throws Exception {
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

    @Test
    void parseProjectSubset(@TempDir Path projectDir) throws Exception {
        Files.writeString(projectDir.resolve("package.json"), """
          {"name": "test-project", "version": "1.0.0"}
          """);
        Files.writeString(projectDir.resolve("math.ts"), """
          export function add(a: number, b: number): number {
              return a + b;
          }
          """);
        Files.writeString(projectDir.resolve("app.ts"), """
          import {add} from "./math";
          const result = add(1, 2);
          """);

        // Full parse establishes the baseline: which path app.ts gets and how add(...) resolves.
        List<SourceFile> full = client()
          .parseProject(projectDir, new InMemoryExecutionContext())
          .toList();

        SourceFile fullApp = full.stream()
          .filter(sf -> sf.getSourcePath().toString().equals("app.ts"))
          .findFirst().orElseThrow();
        JavaType.Method fullAddType = findAddInvocationType(fullApp);
        assertThat(fullAddType).as("add(...) should resolve to a method type in a full parse").isNotNull();

        // Subset parse: ask for only app.ts. The whole project is still loaded for typing.
        List<SourceFile> subset = client()
          .parseProject(projectDir, ParseProjectOptions.builder()
            .files(List.of("app.ts"))
            .build(), new InMemoryExecutionContext())
          .toList();

        // (a) Only app.ts is returned — not package.json, not the unchanged math.ts.
        assertThat(subset).hasSize(1);
        SourceFile subsetApp = subset.get(0);
        assertThat(subsetApp).isInstanceOf(JS.CompilationUnit.class);

        // (b) Its sourcePath matches the full-parse path exactly.
        assertThat(subsetApp.getSourcePath()).isEqualTo(fullApp.getSourcePath());

        // (c) Types that resolve in a full parse still resolve in the subset parse — proving the whole
        // project (math.ts) was loaded for type context even though it was not returned.
        JavaType.Method subsetAddType = findAddInvocationType(subsetApp);
        assertThat(subsetAddType).as("add(...) should still resolve in a subset parse").isNotNull();
        assertThat(subsetAddType.toString()).isEqualTo(fullAddType.toString());
    }

    @Test
    void parseProjectSubsetWithUnknownFileYieldsNothing(@TempDir Path projectDir) throws Exception {
        Files.writeString(projectDir.resolve("index.ts"), "const x = 1;");

        // A subset entry that discovery wouldn't have parsed (missing/excluded/not a source file)
        // simply matches nothing rather than erroring.
        List<SourceFile> subset = client()
          .parseProject(projectDir, ParseProjectOptions.builder()
            .files(List.of("does-not-exist.ts"))
            .build(), new InMemoryExecutionContext())
          .toList();

        assertThat(subset).isEmpty();
    }

    private static JavaType.Method findAddInvocationType(SourceFile sf) {
        JavaType.Method[] found = new JavaType.Method[1];
        new JavaScriptIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if ("add".equals(method.getSimpleName())) {
                    found[0] = method.getMethodType();
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(sf, 0);
        return found[0];
    }

    @Test
    void parseProjectWithVariousYamlStructures(@TempDir Path projectDir) throws Exception {
        Files.writeString(projectDir.resolve("package.json"), """
          {"name": "test-project", "version": "1.0.0"}
          """);

        // Simple nested mappings
        Files.writeString(projectDir.resolve("simple.yml"), """
          top:
            middle:
              bottom: value
          """);

        // Anchors and aliases
        Files.writeString(projectDir.resolve("anchors.yml"), """
          defaults: &defaults
            adapter: postgres
            host: localhost
          development:
            database: dev_db
            <<: *defaults
          """);

        // Sequences with nested mappings
        Files.writeString(projectDir.resolve("sequences.yml"), """
          items:
            - name: first
              value: 1
            - name: second
              value: 2
          """);

        // Multi-document
        Files.writeString(projectDir.resolve("multi.yml"), """
          ---
          doc1: value1
          ---
          doc2: value2
          """);

        // Flow sequences and flow mappings
        Files.writeString(projectDir.resolve("flow.yml"), """
          flow_seq: [a, b, c]
          flow_map: {key1: val1, key2: val2}
          nested_flow: {outer: {inner: deep}}
          """);

        // Deeply nested (3+ levels)
        Files.writeString(projectDir.resolve("deep.yml"), """
          level1:
            level2:
              level3:
                level4: deep_value
              sibling3: sibling_value
            sibling2: value
          """);

        List<SourceFile> sourceFiles = client()
          .parseProject(projectDir, new InMemoryExecutionContext())
          .toList();

        List<SourceFile> yamlFiles = sourceFiles.stream()
          .filter(sf -> sf.getSourcePath().toString().endsWith(".yml"))
          .toList();

        assertThat(yamlFiles).hasSize(6);
        for (SourceFile yamlFile : yamlFiles) {
            assertThat(yamlFile)
              .as("File %s should parse as YAML, not ParseError", yamlFile.getSourcePath())
              .isInstanceOf(Yaml.Documents.class)
              .isNotInstanceOf(ParseError.class);
            assertThat(client().print(yamlFile))
              .as("File %s should print non-empty", yamlFile.getSourcePath())
              .isNotEmpty();
        }
    }

    @Test
    void parseProjectWithYamlContainingNestedFlowMappings(@TempDir Path projectDir) throws Exception {
        Files.writeString(projectDir.resolve("package.json"), """
          {"name": "test-project", "version": "1.0.0"}
          """);
        // Reproduces the pattern from GitHub Actions workflow files like:
        //   on: { push: {}, pull_request: {} }
        // where nested flow mappings caused "Yaml.Mapping may not have a non-empty prefix"
        // during RPC deserialization on the Java side.
        Files.createDirectories(projectDir.resolve(".github/workflows"));
        Files.writeString(projectDir.resolve(".github/workflows/ci.yml"), """
          name: CI
          on: { push: {}, pull_request: {} }
          jobs:
            test:
              runs-on: ubuntu-latest
              steps:
                - uses: actions/checkout@v4
          """);

        List<SourceFile> sourceFiles = client()
          .parseProject(projectDir, new InMemoryExecutionContext())
          .toList();

        SourceFile yamlFile = sourceFiles.stream()
          .filter(sf -> sf.getSourcePath().toString().endsWith("ci.yml"))
          .findFirst().orElseThrow();

        assertThat(yamlFile).isInstanceOf(Yaml.Documents.class);
        assertThat(yamlFile).isNotInstanceOf(ParseError.class);
        assertThat(client().print(yamlFile)).isNotEmpty();
    }

    @Test
    void parseProjectWithYamlFlowCollectionKeys(@TempDir Path projectDir) throws Exception {
        Files.writeString(projectDir.resolve("package.json"), """
          {"name": "test-project", "version": "1.0.0"}
          """);

        // Flow mapping used as a mapping key (valid YAML, no ? needed)
        // The yaml npm CST parser produces a flow-collection token as the key,
        // which the TS parser converts to Yaml.Mapping - but MappingEntry.key
        // expects YamlKey (Scalar | Alias). This causes ClassCastException
        // when sent via RPC to Java.
        Files.writeString(projectDir.resolve("complex-keys.yml"), """
          {a: 1}: value1
          {b: 2, c: 3}: value2
          simple: normal_value
          """);

        List<SourceFile> sourceFiles = client()
          .parseProject(projectDir, new InMemoryExecutionContext())
          .toList();

        SourceFile yamlFile = sourceFiles.stream()
          .filter(sf -> sf.getSourcePath().toString().endsWith("complex-keys.yml"))
          .findFirst().orElseThrow();

        assertThat(yamlFile).isInstanceOf(Yaml.Documents.class);
        assertThat(yamlFile).isNotInstanceOf(ParseError.class);
        assertThat(client().print(yamlFile)).isNotEmpty();
    }

    /**
     * Tests that a JavaScript recipe can delegate to a Java recipe via RPC.
     * This validates the "npm ecosystem" use case where JS code can invoke Java recipes.
     * Flow: Java test -> JS recipe (JavaChangeMethodName) -> Java recipe (ChangeMethodName) -> result
     */
    @SuppressWarnings({"TypeScriptCheckImport", "JSUnusedLocalSymbols"})
    @Test
    void jsRecipeDelegatingToJavaRecipe(@TempDir Path projectDir) {
        installRecipes();
        rewriteRun(
          spec -> spec.recipe(client().prepareRecipe(
            "org.openrewrite.example.java.change-method-name",
            Map.of(
              "methodPattern", "_.LoDashStatic max(..)",
              "newMethodName", "maximum"
            ))),
          npm(
            projectDir,
            typescript(
              """
                import _ from 'lodash';
                const result = _.max(1, 2);
                """,
              """
                import _ from 'lodash';
                const result = _.maximum(1, 2);
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

    /**
     * A JS composite whose {@code recipeList()} contains a recipe that delegates to a Java recipe
     * (the shape of Angular's {@code UpgradeToAngular19}). The host re-prepares each child by id while
     * building {@code RpcRecipe.getRecipeList()}; the Java-delegate child misses in the JS marketplace,
     * so the JS server must answer {@code delegatesTo} (rather than throwing "Could not find recipe
     * with id ...") and the host resolves it from its own marketplace as a LOCAL Java recipe.
     * <p>
     * The host (this JVM) is configured with a runtime-classpath marketplace + resolver that owns its
     * bundled {@code org.openrewrite.javascript.*} recipes — mirroring the moderne-cli concession.
     */
    @Test
    void jsCompositeWithJavaDelegateChild() {
        Environment env = Environment.builder().scanRuntimeClasspath("org.openrewrite.javascript").build();
        RecipeMarketplace marketplace = env.toMarketplace(RecipeBundle.runtimeClasspath());
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder()
          .recipeInstallDir(tempDir)
          .marketplace(marketplace)
          .resolvers(List.of(new RuntimeClasspathResolver(marketplace)))
          .log(tempDir.resolve("rpc.log")));

        assertThat(client().installRecipes(new File("rewrite/dist-fixtures/composite-with-java-delegate.js"))
          .getRecipesInstalled()).isGreaterThan(0);

        Recipe composite = client().prepareRecipe("org.openrewrite.example.npm.composite-with-java-delegate");

        Recipe delegate = composite.getRecipeList().stream()
          .filter(r -> "org.openrewrite.javascript.UpgradeDependencyVersion".equals(r.getName()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("expected an org.openrewrite.javascript.UpgradeDependencyVersion child"));
        assertThat(delegate)
          .isInstanceOf(UpgradeDependencyVersion.class)
          .isNotInstanceOf(RpcRecipe.class);
    }

    /**
     * A runtime-classpath {@link RecipeBundleResolver} that materializes a bundled recipe by id straight
     * off the JVM classpath, mirroring the moderne-cli {@code RuntimeClasspathRecipeBundleResolver}.
     */
    private static class RuntimeClasspathResolver implements RecipeBundleResolver {
        private final RecipeMarketplace marketplace;

        RuntimeClasspathResolver(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
        }

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
                    return new RecipeLoader(null).load(listing.getName(), Map.of()).getDescriptor();
                }

                @Override
                public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
                    return new RecipeLoader(null).load(listing.getName(), options);
                }
            };
        }
    }

    /**
     * Regression test for <a href="https://github.com/moderneinc/customer-requests/issues/2234">#2234</a>:
     * a string enum declaration would send a {@link JavaType.Primitive} (resolved from the union of
     * enum literals) for {@code J.ClassDeclaration.type}, which the Java-side RPC receiver rejects
     * with "A class can only be type attributed with a fully qualified type name".
     */
    @Test
    void parseStringEnumDeclaration() {
        rewriteRun(
          typescript(
            """
              export enum ContentType {
                APPLICATION_JSON = 'application/json',
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaScriptIsoVisitor<Integer>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                    assertThat(classDecl.getType())
                      .as("enum declaration must have a FullyQualified type")
                      .isInstanceOf(JavaType.Class.class);
                    JavaType.Class type = (JavaType.Class) classDecl.getType();
                    assertThat(type.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Enum);
                    assertThat(type.getFullyQualifiedName()).contains("ContentType");
                    return classDecl;
                }
            }.visit(cu, 0))
          )
        );
    }

    private void installRecipes() {
        var exampleRecipes = new File("rewrite/dist-fixtures/example-recipe.js");
        assertThat(exampleRecipes).exists();
        assertThat(client().installRecipes(exampleRecipes).getRecipesInstalled()).isGreaterThan(0);
    }

    private JavaScriptRewriteRpc client() {
        return JavaScriptRewriteRpc.getOrStart();
    }
}
