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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.marker.Builtin;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.go;

/**
 * The type attribution a Go recipe qualifies calls by has to survive the trip to Java: a composite literal's own
 * type, the conversion Go spells with call syntax, and the marker that separates a builtin from a call that
 * simply failed to resolve.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoTypeAttributionIntegTest implements RewriteTest {

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
    void compositeCarriesItsType() {
        List<JavaType> types = new ArrayList<>();
        rewriteRun(
                go(
                        """
                                package main

                                import "crypto/tls"

                                func f() {
                                	_ = []tls.Config{{InsecureSkipVerify: true}}
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            new GolangVisitor<ExecutionContext>() {
                                @Override
                                public J visitComposite(Go.Composite composite, ExecutionContext ctx) {
                                    types.add(composite.getType());
                                    return super.visitComposite(composite, ctx);
                                }
                            }.visit(cu, new InMemoryExecutionContext());
                            assertThat(types).hasSize(2);
                            assertThat(types.get(0)).isInstanceOf(JavaType.Array.class);
                            assertThat(TypeUtils.asFullyQualified(((JavaType.Array) types.get(0)).getElemType()))
                                    .isNotNull()
                                    .extracting(JavaType.FullyQualified::getFullyQualifiedName)
                                    .isEqualTo("crypto/tls.Config");
                            // The elided inner literal has no type expression to read.
                            assertThat(TypeUtils.asFullyQualified(types.get(1)))
                                    .isNotNull()
                                    .extracting(JavaType.FullyQualified::getFullyQualifiedName)
                                    .isEqualTo("crypto/tls.Config");
                        })
                )
        );
    }

    @Test
    void conversionAndBuiltinSurvive() {
        List<String> conversions = new ArrayList<>();
        List<JavaType> conversionTypes = new ArrayList<>();
        List<String> builtins = new ArrayList<>();
        rewriteRun(
                go(
                        """
                                package main

                                func f(b []byte) {
                                	_ = string(b)
                                	_ = len(b)
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            new JavaIsoVisitor<ExecutionContext>() {
                                @Override
                                public J.TypeCast visitTypeCast(J.TypeCast cast, ExecutionContext ctx) {
                                    conversions.add(cast.printTrimmed(getCursor()));
                                    conversionTypes.add(cast.getType());
                                    return super.visitTypeCast(cast, ctx);
                                }

                                @Override
                                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                                    mi.getMarkers().findFirst(Builtin.class)
                                            .ifPresent(m -> builtins.add(mi.getSimpleName()));
                                    return super.visitMethodInvocation(mi, ctx);
                                }
                            }.visit(cu, new InMemoryExecutionContext());
                            // Printing goes back over RPC, so the Go layout has to come out of the Java-side tree.
                            assertThat(conversions).containsExactly("string(b)");
                            assertThat(conversionTypes).containsExactly(JavaType.Primitive.String);
                            assertThat(builtins).containsExactly("len");
                        })
                )
        );
    }
}
