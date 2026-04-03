/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Tests that Java-defined recipes work on Go source code via bidirectional RPC.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangRecipeIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
          .goBinaryPath(binaryPath)
          .log(tempDir.resolve("go-rpc.log"))
          .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
          .allowNonWhitespaceInWhitespace(true)
          .identifiers(false)
          .methodInvocations(false)
          .build());
    }

    /**
     * Rename a boolean variable — exercises modifying an identifier in a
     * VariableDeclarations context with a boolean literal initializer.
     * This is similar to what SimplifyBooleanExpression needs.
     */
    @Test
    void renameBooleanVar() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new org.openrewrite.java.JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier ident, org.openrewrite.ExecutionContext ctx) {
                  if ("x".equals(ident.getSimpleName())) {
                      return ident.withSimpleName("flag");
                  }
                  return ident;
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          go(
            """
              package main

              func f() {
              \tvar x = true
              \t_ = x
              }
              """,
            """
              package main

              func f() {
              \tvar flag = true
              \t_ = flag
              }
              """
          )
        );
    }

    /**
     * Tests Go-native recipe execution via the full RPC Visit path.
     * The RenameXToFlag recipe has a real Go Editor() that visits identifiers.
     * This exercises: PrepareRecipe → Visit → getObjectFromJava → Go visitor →
     * modified tree sent back to Java via GetObject.
     */
    /**
     * Tests Go-native recipe execution via the full RPC Visit path.
     * Uses the RPC API directly: PrepareRecipe → Visit → GetObject round-trip.
     */
    @Test
    void goNativeRecipeViaRpc() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc f() {\n\tvar x = true\n\t_ = x\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();
        var recipe = rpc.prepareRecipe("org.openrewrite.golang.test.RenameXToFlag");
        Tree result = recipe.getVisitor().visit(cu, new InMemoryExecutionContext());
        assertThat(result).isNotNull().isInstanceOf(SourceFile.class);
        assertThat(result).isNotSameAs(cu);
        String printed = GoRewriteRpc.getOrStart().print((SourceFile) result);
        assertThat(printed).contains("var flag = true").contains("_ = flag");
    }

    /**
     * Simulates the CLI path where the tree is loaded from a JAR (no RPC baseline).
     * Forces the Visit RPC to use ADD (not CHANGE) by resetting the Go RPC state.
     */
    @Test
    void goNativeRecipeViaRpcWithAddPath() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc f() {\n\tvar x = true\n\t_ = x\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        // Reset Go RPC state to simulate a fresh session (like CLI's mod run)
        rpc.reset();

        var recipe = rpc.prepareRecipe("org.openrewrite.golang.test.RenameXToFlag");
        Tree result = recipe.getVisitor().visit(cu, new InMemoryExecutionContext());
        assertThat(result).isNotNull().isInstanceOf(SourceFile.class);
        assertThat(result).isNotSameAs(cu);
        String printed = rpc.print((SourceFile) result);
        assertThat(printed).contains("var flag = true").contains("_ = flag");
    }

    /**
     * Simulates the CLI path where the tree has type information from Go's
     * type checker. The tree is parsed (which populates type info), then
     * reset + Visit forces the ADD path.
     */
    /**
     * Simulates the full CLI path: parse → reset → installRecipes → prepareRecipe → visit.
     * This matches what mod run does.
     */
    @Test
    void goNativeRecipeViaRpcFullCliPath() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = "package main\n\nfunc f() {\n\tvar x = true\n\t_ = x\n}\n";
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        // Reset to simulate fresh session, then install recipes (like CLI does)
        rpc.reset();
        rpc.installRecipes(
                new java.io.File("/Users/jonathan/Projects/github/moderneinc/recipes-go/recipes-code-quality"));

        var recipe = rpc.prepareRecipe("org.openrewrite.golang.test.RenameXToFlag");
        Tree result = recipe.getVisitor().visit(cu, new InMemoryExecutionContext());
        assertThat(result).isNotNull().isInstanceOf(SourceFile.class);
        String printed = rpc.print((SourceFile) result);
        assertThat(printed).contains("var flag = true").contains("_ = flag");
    }

    @Test
    void renameIdentifier() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new org.openrewrite.java.JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier ident, org.openrewrite.ExecutionContext ctx) {
                  if ("hello".equals(ident.getSimpleName())) {
                      return ident.withSimpleName("world");
                  }
                  return ident;
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          go(
            """
              package main

              func hello() {
              }
              """,
            """
              package main

              func world() {
              }
              """
          )
        );
    }

    @Test
    void changeMethodName() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new org.openrewrite.java.JavaIsoVisitor<>() {
              final org.openrewrite.java.MethodMatcher matcher =
                new org.openrewrite.java.MethodMatcher("fmt Println(..)");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, org.openrewrite.ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (matcher.matches(m)) {
                      m = m.withName(m.getName().withSimpleName("Print"));
                  }
                  return m;
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          go(
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Println("hello")
              }
              """,
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Print("hello")
              }
              """
          )
        );
    }

    @Test
    void findMethodsByPattern() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("fmt Println(..)", null)),
          go(
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Println("hello")
              }
              """,
            """
              package main

              import "fmt"

              func main() {/*~~>*/
              \tfmt.Println("hello")
              }
              """
          )
        );
    }

    @Test
    void findTypesByFullyQualifiedName() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("main.Point", null)),
          go(
            """
              package main

              type Point struct {
              \tX int
              \tY int
              }
              """,
            """
              package main

              type /*~~>*/Point struct {
              \tX int
              \tY int
              }
              """
          )
        );
    }

    @Test
    void changeLocalStructType() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeType("main.Point", "main.Vector", null)),
          go(
            """
              package main

              type Point struct {
              \tX int
              \tY int
              }
              """,
            """
              package main

              type Vector struct {
              \tX int
              \tY int
              }
              """
          )
        );
    }

    /**
     * Rename an identifier in a function with short var decl and method call.
     * Exercises the Print round-trip with delta encoding.
     */
    @Test
    void renameWithShortVarDecl() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new org.openrewrite.java.JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier ident, org.openrewrite.ExecutionContext ctx) {
                  if ("cfg".equals(ident.getSimpleName())) {
                      return ident.withSimpleName("config");
                  }
                  return ident;
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          go(
            """
              package main

              import "os"

              func f() {
              \tcfg := os.Getenv("X")
              \tif cfg != "0" {
              \t}
              }
              """,
            """
              package main

              import "os"

              func f() {
              \tconfig := os.Getenv("X")
              \tif config != "0" {
              \t}
              }
              """
          )
        );
    }

    @Test
    void addImportForCrossPackageType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new org.openrewrite.java.JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier ident, org.openrewrite.ExecutionContext ctx) {
                  if ("hello".equals(ident.getSimpleName())) {
                      maybeAddImport("net/http", "Handler", null, null, false);
                      return ident.withSimpleName("world");
                  }
                  return ident;
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          go(
            """
              package main

              func hello() {
              }
              """,
            """
              package main

              import (
              \t"net/http"
              )

              func world() {
              }
              """
          )
        );
    }
}
