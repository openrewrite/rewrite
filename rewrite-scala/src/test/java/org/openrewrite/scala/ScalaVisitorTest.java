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
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.scala.tree.S;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ScalaVisitorTest {

    @Test
    void visitExpressionIsDispatchedForScalaExpressionNodes() {
        // given
        ScalaParser parser = ScalaParser.builder().classpath(JavaParser.runtimeClasspath()).build();
        List<SourceFile> parsed = parser.parse(
          """
            object Test {
                def f(xs: Int*): Unit = {}
                val ys = Seq(1, 2, 3)
                f(ys: _*)
            }
            """
        ).collect(Collectors.toList());
        SourceFile cu = parsed.get(0);

        boolean[] sawSplat = {false};
        ScalaVisitor<ExecutionContext> visitor = new ScalaVisitor<>() {
            @Override
            public J visitExpression(Expression expression, ExecutionContext ctx) {
                if (expression instanceof S.SplatExpression) {
                    sawSplat[0] = true;
                }
                return super.visitExpression(expression, ctx);
            }
        };

        // when
        visitor.visit(cu, new InMemoryExecutionContext());

        // then
        assertThat(sawSplat[0])
          .as("visitSplatExpression must dispatch through visitExpression")
          .isTrue();
    }

    @Test
    void javaVisitorVisitsEachNodeOnceInSourceOrder() {
        // given
        ScalaParser parser = ScalaParser.builder().classpath(JavaParser.runtimeClasspath()).build();
        SourceFile cu = parser.parse(
          """
            package p
            import java.lang.System.out
            class S
            """
        ).toList().getFirst();

        List<String> visited = new ArrayList<>();
        JavaIsoVisitor<ExecutionContext> visitor = new JavaIsoVisitor<>() {
            @Override
            public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, ExecutionContext ctx) {
                if (loc == JRightPadded.Location.PACKAGE || loc == JRightPadded.Location.IMPORT) {
                    visited.add(loc.name());
                }
                return super.visitRightPadded(right, loc, ctx);
            }

            @Override
            public J.Import visitImport(J.Import import_, ExecutionContext ctx) {
                visited.add("import");
                return super.visitImport(import_, ctx);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                visited.add(identifier.getSimpleName());
                return super.visitIdentifier(identifier, ctx);
            }
        };

        // when
        visitor.visit(cu, new InMemoryExecutionContext());

        // then
        assertThat(visited).containsExactly("PACKAGE", "p", "IMPORT", "import", "java", "lang", "System", "out", "S");
    }
}
