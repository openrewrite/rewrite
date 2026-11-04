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

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.tree.S;

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
}
