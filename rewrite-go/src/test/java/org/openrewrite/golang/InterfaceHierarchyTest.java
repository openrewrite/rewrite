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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Java recipe's visitor runs over Go trees too, and the hierarchy walks of {@link TypeUtils} assume, as Java
 * guarantees, that no type inherits from itself.
 */
class InterfaceHierarchyTest {

    @BeforeAll
    static void startEngine() throws Exception {
        BundledEngine.use();
    }

    @AfterAll
    static void stopEngine() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void hierarchyWalksEndOverInterfacesWithTheSameMethods() {
        SourceFile cu = GolangParser.builder().build()
          .parse(new InMemoryExecutionContext(), """
            package nettesting

            import "context"

            type ServerRunner interface {
            	Run(ctx context.Context) error
            }

            type ClientSimulator interface {
            	Run(ctx context.Context) error
            }

            func start(r ServerRunner) error {
            	return r.Run(context.Background())
            }
            """)
          .findFirst().orElseThrow();

        Map<String, JavaType.FullyQualified> declared = new HashMap<>();
        J.MethodInvocation[] run = new J.MethodInvocation[1];
        new GolangVisitor<Integer>() {
            @Override
            public J visitIdentifier(J.Identifier ident, Integer p) {
                if (ident.getType() instanceof JavaType.FullyQualified fq) {
                    declared.putIfAbsent(fq.getFullyQualifiedName(), fq);
                }
                return super.visitIdentifier(ident, p);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, Integer p) {
                run[0] = method.getSimpleName().equals("Run") ? method : run[0];
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0);
        JavaType.FullyQualified runner = declared.get("nettesting.ServerRunner");
        assertThat(runner).isNotNull();
        assertThat(run[0]).isNotNull();

        assertThat(TypeUtils.isAssignableTo("nettesting.Unrelated", runner)).isFalse();
        assertThat(new MethodMatcher("nettesting.Unrelated Run(..)", true).matches(run[0])).isFalse();
    }
}
