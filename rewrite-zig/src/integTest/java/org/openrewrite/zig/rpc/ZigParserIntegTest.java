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
package org.openrewrite.zig.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.zig.tree.Zig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.zig.Assertions.zig;

/**
 * Integration tests that parse Zig source via the RPC subprocess and verify
 * the resulting LST round-trips through Java correctly.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ZigParserIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        // The zigBuild task places the binary at build/rewrite-zig-rpc
        Path binaryPath = Paths.get("build/rewrite-zig-rpc").toAbsolutePath();
        ZigRewriteRpc.setFactory(ZigRewriteRpc.builder()
                .zigBinaryPath(binaryPath)
                .log(tempDir.resolve("zig-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() {
        ZigRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
                .allowNonWhitespaceInWhitespace(true)
                .build());
    }

    @Test
    void helloWorld() {
        rewriteRun(
                zig(
                        """
                                const std = @import("std");

                                pub fn main() void {
                                    std.debug.print("Hello, World!\\n", .{});
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(2);
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class);
                            assertThat(zigCu.getStatements().get(1)).isInstanceOf(J.MethodDeclaration.class);
                        })
                )
        );
    }

    @Test
    void simpleFunction() {
        rewriteRun(
                zig(
                        """
                                fn add(a: i32, b: i32) i32 {
                                    return a + b;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getSimpleName()).isEqualTo("add");
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody()).isInstanceOf(J.Block.class);
                        })
                )
        );
    }

    @Test
    void constAndVar() {
        rewriteRun(
                zig(
                        """
                                const x: u32 = 42;
                                var y: u32 = 0;
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(2);
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class);
                            assertThat(zigCu.getStatements().get(1)).isInstanceOf(J.VariableDeclarations.class);
                        })
                )
        );
    }

    // ---------------------------------------------------------------
    // Task 9: Structural validation tests
    // ---------------------------------------------------------------

    @Test
    void verifyFunctionParsedAsMethodDeclaration() {
        rewriteRun(
                zig("""
                                fn add(a: i32, b: i32) i32 {
                                    return a + b;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).isNotEmpty();
                            assertThat(zigCu.getStatements().get(0))
                                    .isInstanceOf(J.MethodDeclaration.class);
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getSimpleName()).isEqualTo("add");
                        })
                )
        );
    }

    @Test
    void verifyConstParsedAsVariableDeclarations() {
        rewriteRun(
                zig("""
                                const x: u32 = 42;
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            assertThat(zigCu.getStatements().get(0))
                                    .isInstanceOf(J.VariableDeclarations.class);
                        })
                )
        );
    }

    @Test
    void verifyNoOpVisitorPreservesTree() {
        rewriteRun(
                zig("""
                                const x: u32 = 42;
                                fn main() void {}
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            var noopVisitor = new JavaIsoVisitor<ExecutionContext>() {};
                            var result = (Zig.CompilationUnit) noopVisitor.visit(
                                    zigCu, new InMemoryExecutionContext());
                            assertThat(result).isNotNull();
                            assertThat(result.getStatements()).hasSameSizeAs(zigCu.getStatements());
                        })
                )
        );
    }

    // ---------------------------------------------------------------
    // Task 10: Round-trip tests
    // ---------------------------------------------------------------

    @Test
    void structDeclaration() {
        rewriteRun(
                zig("""
                                const Point = struct {
                                    x: f64,
                                    y: f64,
                                };
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            // const Point = struct { ... } parses as a variable declaration
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class);
                        }))
        );
    }

    @Test
    void enumDeclaration() {
        rewriteRun(
                zig("""
                                const Color = enum {
                                    red,
                                    green,
                                    blue,
                                };
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            // const Color = enum { ... } parses as a variable declaration
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class);
                        }))
        );
    }

    @Test
    void deferStatement() {
        rewriteRun(
                zig("""
                                const std = @import("std");
                                fn open() void {
                                    defer std.debug.print("cleanup\\n", .{});
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(2);
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class);
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(1);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements().get(0)).isInstanceOf(Zig.Defer.class);
                        }))
        );
    }

    @Test
    void testBlock() {
        rewriteRun(
                zig("""
                                const std = @import("std");
                                test "addition" {
                                    const x: i32 = 1 + 2;
                                    try std.testing.expectEqual(@as(i32, 3), x);
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(2);
                            assertThat(zigCu.getStatements().get(1)).isInstanceOf(Zig.TestDecl.class);
                        }))
        );
    }

    @Test
    void errorHandling() {
        rewriteRun(
                zig("""
                                fn divide(a: f64, b: f64) !f64 {
                                    if (b == 0) return error.DivisionByZero;
                                    return a / b;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements()).hasSize(2);
                        }))
        );
    }

    @Test
    void comments() {
        rewriteRun(
                zig("""
                                // Top-level comment
                                const x: u32 = 42;

                                /// Doc comment
                                fn documented() void {}
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(2);
                            // Both statements should be proper types, not J.Unknown
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class);
                            assertThat(zigCu.getStatements().get(1)).isInstanceOf(J.MethodDeclaration.class);
                        }))
        );
    }

    @Test
    void multipleDeclarations() {
        rewriteRun(
                zig("""
                                const std = @import("std");

                                const MAX_SIZE: usize = 1024;
                                var global_count: u32 = 0;

                                pub fn main() void {
                                    std.debug.print("Hello\\n", .{});
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(4);
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.VariableDeclarations.class); // const std
                            assertThat(zigCu.getStatements().get(1)).isInstanceOf(J.VariableDeclarations.class); // const MAX_SIZE
                            assertThat(zigCu.getStatements().get(2)).isInstanceOf(J.VariableDeclarations.class); // var global_count
                            assertThat(zigCu.getStatements().get(3)).isInstanceOf(J.MethodDeclaration.class);    // pub fn main
                        }))
        );
    }

    @Test
    void emptyFunction() {
        rewriteRun(
                zig("""
                                fn noop() void {}
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody()).isInstanceOf(J.Block.class);
                            assertThat(fn.getBody().getStatements()).isEmpty();
                        }))
        );
    }

    @Test
    void functionWithMultipleStatements() {
        rewriteRun(
                zig("""
                                fn compute(x: i32, y: i32) i32 {
                                    const sum = x + y;
                                    const product = x * y;
                                    return sum + product;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements()).hasSize(3);
                        }))
        );
    }

    // ---------------------------------------------------------------
    // Control flow: if/else round-trip tests
    // ---------------------------------------------------------------

    @Test
    void ifStatement() {
        rewriteRun(
                zig("""
                                fn abs(x: i32) i32 {
                                    if (x < 0) return -x;
                                    return x;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements().get(0)).isInstanceOf(J.If.class);
                        }))
        );
    }

    @Test
    void ifElseStatement() {
        rewriteRun(
                zig("""
                                fn abs(x: i32) i32 {
                                    if (x < 0) return -x else return x;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.If ifStmt = (J.If) fn.getBody().getStatements().get(0);
                            assertThat(ifStmt.getElsePart()).isNotNull();
                        }))
        );
    }

    @Test
    void ifElseBlock() {
        rewriteRun(
                zig("""
                                fn classify(x: i32) i32 {
                                    if (x > 0) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.If ifStmt = (J.If) fn.getBody().getStatements().get(0);
                            assertThat(ifStmt.getElsePart()).isNotNull();
                        }))
        );
    }

    @Test
    void verifyIfParsedCorrectly() {
        rewriteRun(
                zig("""
                                fn abs(x: i32) i32 {
                                    if (x < 0) return -x;
                                    return x;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements().get(0)).isInstanceOf(J.If.class);
                        })
                )
        );
    }

    // ---------------------------------------------------------------
    // Control flow: while round-trip tests
    // ---------------------------------------------------------------

    @Test
    void whileLoop() {
        rewriteRun(
                zig("""
                                fn count() void {
                                    while (true) {
                                        return;
                                    }
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements().get(0)).isInstanceOf(J.WhileLoop.class);
                        }))
        );
    }

    @Test
    void verifyWhileParsedCorrectly() {
        rewriteRun(
                zig("""
                                fn count() void {
                                    while (true) {
                                        return;
                                    }
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements().get(0)).isInstanceOf(J.WhileLoop.class);
                        })
                )
        );
    }

    // ---------------------------------------------------------------
    // Unary operations round-trip tests
    // ---------------------------------------------------------------

    @Test
    void unaryNegation() {
        rewriteRun(
                zig("""
                                fn neg(x: i32) i32 {
                                    return -x;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            assertThat(ret.getExpression()).isInstanceOf(J.Unary.class);
                        }))
        );
    }

    @Test
    void verifyUnaryParsedCorrectly() {
        rewriteRun(
                zig("""
                                fn neg(x: i32) i32 {
                                    return -x;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            assertThat(ret.getExpression()).isInstanceOf(J.Unary.class);
                        })
                )
        );
    }

    @Test
    void boolNot() {
        rewriteRun(
                zig("""
                                fn invert(b: bool) bool {
                                    return !b;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            assertThat(ret.getExpression()).isInstanceOf(J.Unary.class);
                        }))
        );
    }

    // ---------------------------------------------------------------
    // Try/catch/orelse round-trip tests
    // ---------------------------------------------------------------

    @Test
    void tryCatch() {
        rewriteRun(
                zig("""
                                fn read(buf: []u8) usize {
                                    return doRead(buf) catch 0;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            // catch is mapped as J.Binary
                            assertThat(ret.getExpression()).isInstanceOf(J.Binary.class);
                        }))
        );
    }

    @Test
    void orelseExpression() {
        rewriteRun(
                zig("""
                                fn unwrap(opt: ?i32) i32 {
                                    return opt orelse 0;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            // orelse is mapped as J.Binary
                            assertThat(ret.getExpression()).isInstanceOf(J.Binary.class);
                        }))
        );
    }

    @Test
    void tryExpression() {
        rewriteRun(
                zig("""
                                fn doSomething(x: i32) !i32 {
                                    return try compute(x);
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            // try is mapped as J.Unary
                            assertThat(ret.getExpression()).isInstanceOf(J.Unary.class);
                        }))
        );
    }

    // ---------------------------------------------------------------
    // Slice round-trip tests
    // ---------------------------------------------------------------

    @Test
    void sliceExpression() {
        rewriteRun(
                zig("""
                                fn first(buf: []const u8) []const u8 {
                                    return buf[0..1];
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            assertThat(ret.getExpression()).isInstanceOf(Zig.Slice.class);
                        }))
        );
    }

    @Test
    void arrayAccess() {
        rewriteRun(
                zig("""
                                fn getFirst(buf: []const u8) u8 {
                                    return buf[0];
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            assertThat(ret.getExpression()).isInstanceOf(J.ArrayAccess.class);
                        }))
        );
    }

    @Test
    void compoundAssignment() {
        rewriteRun(
                zig("""
                                fn accumulate(x: *i32) void {
                                    x.* += 1;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            assertThat(fn.getBody().getStatements().get(0)).isInstanceOf(J.AssignmentOperation.class);
                        }))
        );
    }

    @Test
    void errorUnionType() {
        rewriteRun(
                zig("""
                                fn parse(buf: []const u8) !usize {
                                    return 0;
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            assertThat(zigCu.getStatements()).hasSize(1);
                            // function parsed as MethodDeclaration even with error union return type
                            assertThat(zigCu.getStatements().get(0)).isInstanceOf(J.MethodDeclaration.class);
                        }))
        );
    }

    // ---------------------------------------------------------------
    // Switch expression round-trip tests
    // ---------------------------------------------------------------

    @Test
    void switchExpression() {
        rewriteRun(zig("""
                        fn toStr(x: u8) []const u8 {
                            return switch (x) {
                                0 => "zero",
                                1 => "one",
                                else => "other",
                            };
                        }
                        """,
                spec -> spec.afterRecipe(cu -> {
                    Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                    J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                    assertThat(fn.getBody()).isNotNull();
                    // Verify switch is NOT J.Unknown
                    J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                    assertThat(ret.getExpression()).isNotNull();
                    assertThat(ret.getExpression()).isNotInstanceOf(J.Unknown.class);
                })));
    }

    @Test
    void switchExpressionSimple() {
        rewriteRun(zig("""
                        fn classify(x: i32) i32 {
                            return switch (x) {
                                0 => 100,
                                else => 0,
                            };
                        }
                        """,
                spec -> spec.afterRecipe(cu -> {
                    Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                    J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                    assertThat(fn.getBody()).isNotNull();
                    J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                    assertThat(ret.getExpression()).isInstanceOf(J.SwitchExpression.class);
                })));
    }

    // ---------------------------------------------------------------
    // For loop round-trip tests
    // ---------------------------------------------------------------

    @Test
    void forLoop() {
        // For loops are mapped to J.Unknown on the Java side because Zig's
        // multi-input/payload-capture for syntax doesn't fit J.ForEachLoop.Control.
        // This test explicitly asserts J.Unknown so it breaks when for loops get
        // a proper mapping — at which point update to assert the rich type.
        rewriteRun(zig("""
                        fn sum(items: []const i32) i32 {
                            var total: i32 = 0;
                            for (items) |item| {
                                total += item;
                            }
                            return total;
                        }
                        """,
                spec -> spec.afterRecipe(cu -> {
                    Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                    J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                    assertThat(fn.getBody()).isNotNull();
                    assertThat(fn.getBody().getStatements()).hasSize(3);
                    // Known gap: for loop is J.Unknown, not a structured type.
                    // Update this assertion when for loop mapping is implemented.
                    assertThat(fn.getBody().getStatements().get(1))
                            .isInstanceOf(J.Unknown.class);
                })));
    }

    @Test
    void verifySliceParsedCorrectly() {
        rewriteRun(
                zig("""
                                fn first(buf: []const u8) []const u8 {
                                    return buf[0..1];
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Zig.CompilationUnit zigCu = (Zig.CompilationUnit) cu;
                            J.MethodDeclaration fn = (J.MethodDeclaration) zigCu.getStatements().get(0);
                            assertThat(fn.getBody()).isNotNull();
                            J.Return ret = (J.Return) fn.getBody().getStatements().get(0);
                            assertThat(ret.getExpression()).isInstanceOf(Zig.Slice.class);
                        })
                )
        );
    }
}
