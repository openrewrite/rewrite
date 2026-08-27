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
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.go;

/**
 * A conversion's type sits in {@code J.TypeCast}'s {@code clazz}, a
 * {@code J.ControlParentheses<TypeTree>}. A {@code J.Parentheses} satisfies that
 * slot's erased type and fails its checkcast.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoParenthesizedConversionTypeIntegTest implements RewriteTest {

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
                .build());
    }

    /**
     * {@code getTypesInUse()} walks the whole tree, which is the step of the LST
     * write path {@code mod build} takes.
     */
    @Test
    void typesInUseWalksAPointerConversion() {
        rewriteRun(
                go(
                        """
                                package main

                                func main() {
                                	p := (*int)(nil)
                                	println(p == nil)
                                }
                                """,
                        spec -> spec.afterRecipe(cu ->
                                assertThat(((Go.CompilationUnit) cu).getTypesInUse().getTypesInUse()).isNotNull())
                )
        );
    }

    @Test
    void typesInUseWalksAnInterfaceSatisfactionAssertion() {
        rewriteRun(
                go(
                        """
                                package main

                                type Iface interface {
                                	M()
                                }

                                type T struct{}

                                func (t *T) M() {
                                }

                                var _ Iface = (*T)(nil)
                                """,
                        spec -> spec.afterRecipe(cu ->
                                assertThat(((Go.CompilationUnit) cu).getTypesInUse().getTypesInUse()).isNotNull())
                )
        );
    }

    @Test
    void aParenthesizedConversionTypeIsATypeTree() {
        rewriteRun(
                go(
                        """
                                package main

                                func main() {
                                	_ = (*int)(nil)
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            J.TypeCast[] found = new J.TypeCast[1];
                            new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                                @Override
                                public J.TypeCast visitTypeCast(J.TypeCast typeCast, Integer p) {
                                    found[0] = typeCast;
                                    return super.visitTypeCast(typeCast, p);
                                }
                            }.visit(cu, 0);

                            assertThat(found[0]).as("no conversion in the tree").isNotNull();
                            TypeTree clazz = found[0].getClazz().getTree();
                            assertThat(clazz).isInstanceOf(J.ParenthesizedTypeTree.class);
                            assertThat(((J.ParenthesizedTypeTree) clazz).getParenthesizedType().getTree())
                                    .isInstanceOf(Go.PointerType.class);
                        })
                )
        );
    }
}
