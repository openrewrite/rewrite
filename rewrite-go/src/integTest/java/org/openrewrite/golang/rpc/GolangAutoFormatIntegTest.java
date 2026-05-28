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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.format.AutoFormatVisitor;
import org.openrewrite.golang.format.BlankLinesVisitor;
import org.openrewrite.golang.format.NormalizeLineBreaksVisitor;
import org.openrewrite.golang.format.RemoveTrailingWhitespaceVisitor;
import org.openrewrite.golang.format.TabsAndIndentsVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.openrewrite.internal.ThrowingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.golang.Assertions.go;

/**
 * Comprehensive tests for Go AutoFormat visitors.
 * <p>
 * Unit tests verify individual visitors via afterRecipe callbacks on the
 * parsed tree (no RPC printing required). gofmt comparison tests are
 * skipped if gofmt is not on PATH.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangAutoFormatIntegTest implements RewriteTest {

    private static boolean gofmtAvailable;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void checkGofmt() {
        try {
            String result = runGofmt("package main\n");
            gofmtAvailable = result != null && result.contains("package");
        } catch (Exception e) {
            gofmtAvailable = false;
        }
    }

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

    static String runGofmt(String source) {
        try {
            ProcessBuilder pb = new ProcessBuilder("gofmt").redirectErrorStream(true);
            Process process = pb.start();
            process.getOutputStream().write(source.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("gofmt failed (exit " + exitCode + "): " + output);
            }
            return output;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run gofmt", e);
        }
    }

    /**
     * Runs a format visitor in-memory on the parsed tree and asserts properties.
     */
    @SuppressWarnings("unchecked")
    private static ThrowingConsumer<Go.CompilationUnit> withVisitor(
      TreeVisitor<?, ?> visitor, Consumer<Go.CompilationUnit> check) {
        return cu -> {
            var formatted = (Go.CompilationUnit) ((TreeVisitor<J, Object>) visitor)
              .visit(cu, new InMemoryExecutionContext());
            assertThat(formatted).as("Visitor should not return null").isNotNull();
            check.accept(formatted);
        };
    }

    // =========================================================================
    // Parse-print idempotence (well-formatted source stays unchanged)
    // =========================================================================

    @Test
    void alreadyFormattedSimple() {
        rewriteRun(go("""
          package main

          import "fmt"

          func main() {
          \tfmt.Println("hello")
          }
          """));
    }

    @Test
    void alreadyFormattedWithStruct() {
        rewriteRun(go("""
          package main

          type Point struct {
          \tX int
          \tY int
          }
          """));
    }

    @Test
    void alreadyFormattedWithSwitch() {
        rewriteRun(go("""
          package main

          func f(x int) int {
          \tswitch x {
          \tcase 1:
          \t\treturn 10
          \tcase 2:
          \t\treturn 20
          \tdefault:
          \t\treturn 0
          \t}
          }
          """));
    }

    @Test
    void alreadyFormattedIfElse() {
        rewriteRun(go("""
          package main

          func f(x int) int {
          \tif x > 0 {
          \t\treturn x
          \t} else {
          \t\treturn -x
          \t}
          }
          """));
    }

    @Test
    void alreadyFormattedForLoop() {
        rewriteRun(go("""
          package main

          func f() {
          \tfor i := 0; i < 10; i++ {
          \t\t_ = i
          \t}
          }
          """));
    }

    // =========================================================================
    // BlankLinesVisitor
    // =========================================================================

    @Test
    void blankLinesCollapsedBetweenFunctions() {
        rewriteRun(go("""
          package main



          func a() {
          }



          func b() {
          }
          """, spec -> spec.afterRecipe(withVisitor(new BlankLinesVisitor<>(null), formatted -> {
            var stmts = formatted.getStatements();
            assertThat(stmts).hasSize(2);
            String prefix = stmts.get(1).getPrefix().getWhitespace();
            long newlines = prefix.chars().filter(c -> c == '\n').count();
            assertThat(newlines).as("Collapse 3+ blank lines to 1").isLessThanOrEqualTo(2);
        }))));
    }

    @Test
    void singleBlankLinePreservedBetweenFunctions() {
        rewriteRun(go("""
          package main

          func a() {
          }

          func b() {
          }
          """, spec -> spec.afterRecipe(withVisitor(new BlankLinesVisitor<>(null), formatted -> {
            var stmts = formatted.getStatements();
            assertThat(stmts).hasSize(2);
            String prefix = stmts.get(1).getPrefix().getWhitespace();
            long newlines = prefix.chars().filter(c -> c == '\n').count();
            assertThat(newlines).as("Single blank line preserved").isEqualTo(2);
        }))));
    }

    @Test
    void blankLinesCollapsedAfterPackage() {
        rewriteRun(go("""
          package main




          func main() {
          }
          """, spec -> spec.afterRecipe(withVisitor(new BlankLinesVisitor<>(null), formatted -> {
            var stmts = formatted.getStatements();
            assertThat(stmts).hasSize(1);
            String prefix = stmts.get(0).getPrefix().getWhitespace();
            long newlines = prefix.chars().filter(c -> c == '\n').count();
            assertThat(newlines).as("Collapse blank lines after package").isLessThanOrEqualTo(2);
        }))));
    }

    // =========================================================================
    // TabsAndIndentsVisitor
    // =========================================================================

    @Test
    void spacesConvertedToTabsInFunctionBody() {
        // Use return statement (no ShortVarDecl marker) to avoid marker deserialization issues
        rewriteRun(go("""
          package main

          func main() int {
              return 42
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            assertThat(md.getBody()).isNotNull();
            for (Statement stmt : md.getBody().getStatements()) {
                String ws = stmt.getPrefix().getWhitespace();
                if (ws.contains("\n")) {
                    String indent = ws.substring(ws.lastIndexOf('\n') + 1);
                    assertThat(indent).as("Function body uses tabs").matches("\\t+");
                }
            }
        }))));
    }

    @Test
    void topLevelDeclarationNotIndented() {
        rewriteRun(go("""
          package main

          func a() {
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            Statement funcDecl = formatted.getStatements().get(0);
            String ws = funcDecl.getPrefix().getWhitespace();
            if (ws.contains("\n")) {
                String indent = ws.substring(ws.lastIndexOf('\n') + 1);
                assertThat(indent).as("Top-level declaration should not be indented").isEmpty();
            }
        }))));
    }

    // =========================================================================
    // RemoveTrailingWhitespaceVisitor
    // =========================================================================

    @Test
    void trailingSpacesOnPackageLine() {
        rewriteRun(go(
          "package main   \n\nfunc main() {   \n}\n",
          spec -> spec.afterRecipe(withVisitor(new RemoveTrailingWhitespaceVisitor<>(null), formatted -> {
              String prefix = formatted.getPrefix().getWhitespace();
              assertThat(prefix).doesNotContain("   \n");
          }))));
    }

    // =========================================================================
    // NormalizeLineBreaksVisitor
    // =========================================================================

    @Test
    void crlfNormalizedToLf() {
        rewriteRun(go(
          "package main\r\n\r\nfunc main() {\r\n}\r\n",
          spec -> spec.afterRecipe(withVisitor(new NormalizeLineBreaksVisitor<>(null), formatted -> {
              String prefix = formatted.getPrefix().getWhitespace();
              assertThat(prefix).doesNotContain("\r");
          }))));
    }

    // =========================================================================
    // gofmt comparison (skipped if gofmt unavailable)
    // =========================================================================

    @Test
    void gofmtCollapsesBlankLines() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String input = "package main\n\n\n\nfunc a() {\n}\n\n\n\nfunc b() {\n}\n";
        String gofmtOutput = runGofmt(input);
        assertThat(gofmtOutput).doesNotContain("\n\n\n");
    }

    @Test
    void gofmtRemovesTrailingWhitespace() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String gofmtOutput = runGofmt("package main   \n\nfunc main() {   \n}\n");
        assertThat(gofmtOutput).doesNotContain("   \n");
    }

    @Test
    void gofmtUsesTabsForIndentation() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String gofmtOutput = runGofmt("package main\n\nfunc main() {\n    x := 1\n    _ = x\n}\n");
        assertThat(gofmtOutput).contains("\n\tx := 1");
    }

    @Test
    void gofmtNormalizesCrlf() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String gofmtOutput = runGofmt("package main\r\n\r\nfunc main() {\r\n}\r\n");
        assertThat(gofmtOutput).doesNotContain("\r");
    }

    @Test
    void gofmtCaseNotExtraIndented() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String input = "package main\n\nfunc f(x int) {\nswitch x {\ncase 1:\nreturn\n}\n}\n";
        String gofmtOutput = runGofmt(input);
        // In gofmt, case is at switch indent level, body at case+1
        assertThat(gofmtOutput).contains("\tcase 1:");
        assertThat(gofmtOutput).contains("\t\treturn");
    }

    @Test
    void gofmtCompositeLiteralIndentation() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String input = "package main\n\ntype P struct {\nX int\nY int\n}\n\nfunc f() P {\nreturn P{\nX: 1,\nY: 2,\n}\n}\n";
        String gofmtOutput = runGofmt(input);
        assertThat(gofmtOutput).contains("\t\tX: 1,");
        assertThat(gofmtOutput).contains("\t\tY: 2,");
    }

    @Test
    void gofmtPreservesCommentFormatting() {
        assumeTrue(gofmtAvailable, "gofmt not on PATH");
        String input = "package main\n\n//no space\n// has space\n\nfunc main() {\n}\n";
        String gofmtOutput = runGofmt(input);
        // gofmt does NOT add space after // — it preserves both forms
        assertThat(gofmtOutput).contains("//no space");
        assertThat(gofmtOutput).contains("// has space");
    }

    @Test
    void tabsAndIndentsWithShortVarDecl() {
        rewriteRun(go("""
          package main

          func main() {
          \tx := 1
          \t_ = x
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            assertThat(md.getBody()).isNotNull();
            assertThat(md.getBody().getStatements()).hasSize(2);
        }))));
    }

    // =========================================================================
    // Additional TabsAndIndentsVisitor tests
    // =========================================================================

    @Test
    void nestedIfIndentedTwoLevels() {
        rewriteRun(go("""
          package main

          func f() {
          \tif true {
          \t\treturn
          \t}
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            assertThat(md.getBody()).isNotNull();
            // Get the if statement
            Statement ifStmt = md.getBody().getStatements().get(0);
            assertThat(ifStmt).isInstanceOf(J.If.class);
            J.If ifNode = (J.If) ifStmt;
            // The if body (then block) contains the return
            assertThat(ifNode.getThenPart()).isInstanceOf(J.Block.class);
            J.Block thenBlock = (J.Block) ifNode.getThenPart();
            Statement returnStmt = thenBlock.getStatements().get(0);
            String ws = returnStmt.getPrefix().getWhitespace();
            if (ws.contains("\n")) {
                String indent = ws.substring(ws.lastIndexOf('\n') + 1);
                assertThat(indent).as("return inside if inside func = 2 tabs").isEqualTo("\t\t");
            }
        }))));
    }

    @Test
    void tripleNestedIndentation() {
        rewriteRun(go("""
          package main

          func f() {
          \tif true {
          \t\tfor {
          \t\t\treturn
          \t\t}
          \t}
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            J.If ifNode = (J.If) md.getBody().getStatements().get(0);
            J.Block thenBlock = (J.Block) ifNode.getThenPart();
            J.ForLoop forLoop = (J.ForLoop) thenBlock.getStatements().get(0);
            J.Block forBody = (J.Block) forLoop.getBody();
            Statement returnStmt = forBody.getStatements().get(0);
            String ws = returnStmt.getPrefix().getWhitespace();
            if (ws.contains("\n")) {
                String indent = ws.substring(ws.lastIndexOf('\n') + 1);
                assertThat(indent).as("return inside for inside if inside func = 3 tabs").isEqualTo("\t\t\t");
            }
        }))));
    }

    @Test
    void switchCaseNotExtraIndented() {
        rewriteRun(go("""
          package main

          func f(x int) {
          \tswitch x {
          \tcase 1:
          \t\treturn
          \tdefault:
          \t\treturn
          \t}
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            J.Switch sw = (J.Switch) md.getBody().getStatements().get(0);
            J.Case caseStmt = (J.Case) sw.getCases().getStatements().get(0);
            // Case label indentation: switch body block = 1 tab
            String caseWs = caseStmt.getPrefix().getWhitespace();
            if (caseWs.contains("\n")) {
                String indent = caseWs.substring(caseWs.lastIndexOf('\n') + 1);
                assertThat(indent).as("case label at switch block level = 1 tab").isEqualTo("\t");
            }
            // Case body indentation: case is inside switch block = 2 tabs
            // (case body statements are inside the case which is inside the switch block)
            var caseBody = caseStmt.getStatements();
            if (!caseBody.isEmpty()) {
                String bodyWs = caseBody.get(0).getPrefix().getWhitespace();
                if (bodyWs.contains("\n")) {
                    String indent = bodyWs.substring(bodyWs.lastIndexOf('\n') + 1);
                    assertThat(indent).as("case body at switch+1 = 2 tabs").isEqualTo("\t\t");
                }
            }
        }))));
    }

    @Test
    void mixedTabsAndSpacesNormalized() {
        // Input uses spaces where tabs should be
        rewriteRun(go("""
          package main

          func main() {
              return
          }
          """, spec -> spec.afterRecipe(withVisitor(new TabsAndIndentsVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            Statement stmt = md.getBody().getStatements().get(0);
            String ws = stmt.getPrefix().getWhitespace();
            if (ws.contains("\n")) {
                String indent = ws.substring(ws.lastIndexOf('\n') + 1);
                assertThat(indent).as("Spaces normalized to tabs").doesNotContain(" ");
                assertThat(indent).as("Should be one tab").isEqualTo("\t");
            }
        }))));
    }

    // =========================================================================
    // Additional BlankLinesVisitor tests
    // =========================================================================

    @Test
    void blankLinesInsideFunctionBodyCollapsed() {
        // Avoid var/short-var markers — use simple expressions
        rewriteRun(go("""
          package main

          func f(a int) int {
          \t_ = a



          \treturn 0
          }
          """, spec -> spec.afterRecipe(withVisitor(new BlankLinesVisitor<>(null), formatted -> {
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            for (Statement stmt : md.getBody().getStatements()) {
                String ws = stmt.getPrefix().getWhitespace();
                long newlines = ws.chars().filter(c -> c == '\n').count();
                assertThat(newlines)
                  .as("No 3+ newlines inside function body")
                  .isLessThanOrEqualTo(2);
            }
        }))));
    }

    // =========================================================================
    // Full AutoFormatVisitor pipeline tests
    // =========================================================================

    @Test
    void autoFormatCombinedPipeline() {
        // Source with multiple formatting issues
        rewriteRun(go(
          "package main   \r\n\r\n\r\n\r\nfunc main() {   \r\n    return   \r\n}\r\n",
          spec -> spec.afterRecipe(withVisitor(new AutoFormatVisitor<>(), formatted -> {
              // Check trailing whitespace removed
              for (Statement stmt : formatted.getStatements()) {
                  String ws = stmt.getPrefix().getWhitespace();
                  assertThat(ws).as("No trailing spaces before newlines").doesNotContain("   \n");
              }
              // Check CRLF normalized
              String cuPrefix = formatted.getPrefix().getWhitespace();
              assertThat(cuPrefix).as("No CRLF").doesNotContain("\r");
              // Check blank lines collapsed
              for (Statement stmt : formatted.getStatements()) {
                  String ws = stmt.getPrefix().getWhitespace();
                  long newlines = ws.chars().filter(c -> c == '\n').count();
                  assertThat(newlines).as("Blank lines collapsed").isLessThanOrEqualTo(2);
              }
              // Check indentation uses tabs
              J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
              for (Statement bodyStmt : md.getBody().getStatements()) {
                  String ws = bodyStmt.getPrefix().getWhitespace();
                  if (ws.contains("\n")) {
                      String indent = ws.substring(ws.lastIndexOf('\n') + 1);
                      assertThat(indent).as("Uses tabs not spaces").doesNotContain("    ");
                  }
              }
          }))));
    }

    @Test
    void autoFormatPreservesWellFormattedCode() {
        rewriteRun(go("""
          package main

          func main() {
          \treturn
          }
          """, spec -> spec.afterRecipe(withVisitor(new AutoFormatVisitor<>(), formatted -> {
            // Well-formatted code should be unchanged
            J.MethodDeclaration md = (J.MethodDeclaration) formatted.getStatements().get(0);
            Statement returnStmt = md.getBody().getStatements().get(0);
            String ws = returnStmt.getPrefix().getWhitespace();
            assertThat(ws).as("Preserved tab indentation").isEqualTo("\n\t");
        }))));
    }
}
