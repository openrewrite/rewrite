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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.expectType;
import static org.openrewrite.golang.Assertions.go;

/**
 * Go's basic types cross as named types keyed on their Go type name.
 * {@code JavaType.Primitive} is a fixed enum and
 * {@code JavaTypeReceiver#visitPrimitive} throws on a keyword outside it, so
 * these parse through the RPC subprocess to hold the Java side to the names the
 * Go type mapper produces.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoBasicTypeAttributionIntegTest implements RewriteTest {

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
    public void defaults(RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
                .allowNonWhitespaceInWhitespace(true)
                .build());
    }

    @Test
    void basicTypesArriveUnderTheirGoNames() {
        rewriteRun(
                go(
                        """
                                package main

                                func main() {
                                \tvar i int
                                \tvar i32 int32
                                \tvar i64 int64
                                \tvar u8 uint8
                                \tvar b byte
                                \tvar r rune
                                \tvar f64 float64
                                \tvar c128 complex128
                                \tvar s string
                                \tvar ok bool
                                \t_, _, _, _, _, _, _, _, _, _ = i, i32, i64, u8, b, r, f64, c128, s, ok
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            expectType(cu, "i", "int");
                            expectType(cu, "i32", "int32");
                            expectType(cu, "i64", "int64");
                            expectType(cu, "u8", "uint8");
                            expectType(cu, "b", "byte");
                            expectType(cu, "r", "rune");
                            expectType(cu, "f64", "float64");
                            expectType(cu, "c128", "complex128");
                            expectType(cu, "s", "string");
                            expectType(cu, "ok", "bool");
                        })
                )
        );
    }

    @Test
    void collidingTypesArriveDistinct() {
        rewriteRun(
                go(
                        """
                                package main

                                func main() {
                                \tvar i int
                                \tvar i32 int32
                                \tvar b byte
                                \tvar i8 int8
                                \tvar f64 float64
                                \tvar c128 complex128
                                \tvar r rune
                                \t_, _, _, _, _, _, _ = i, i32, b, i8, f64, c128, r
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            Map<String, JavaType> types = declaredTypes(cu);
                            assertThat(types.get("i")).isNotEqualTo(types.get("i32"));
                            assertThat(types.get("b")).isNotEqualTo(types.get("i8"));
                            assertThat(types.get("f64")).isNotEqualTo(types.get("c128"));
                            assertThat(types.get("r")).isNotEqualTo(types.get("i8"));
                        })
                )
        );
    }

    private static Map<String, JavaType> declaredTypes(Tree cu) {
        Map<String, JavaType> types = new LinkedHashMap<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                types.putIfAbsent(identifier.getSimpleName(), identifier.getType());
                return identifier;
            }
        }.visit(cu, 0);
        return types;
    }
}
