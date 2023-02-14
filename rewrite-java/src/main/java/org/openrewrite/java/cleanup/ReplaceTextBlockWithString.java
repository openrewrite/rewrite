/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType.Primitive;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

public class ReplaceTextBlockWithString extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace text block with regular string";
    }

    @Override
    public String getDescription() {
        return "Replace text block with a regular multi-line string.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesJavaVersion<>(13);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceTextBlockWithStringVisitor();
    }

    private static class ReplaceTextBlockWithStringVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
            if (literal.getType() == Primitive.String &&
                    literal.getValue() != null &&
                    literal.getValueSource() != null &&
                    literal.getValueSource().startsWith("\"\"\"")) {
                String[] fields = ((String) literal.getValue()).split("\n", -1);
                boolean lastLineIsEmpty = fields[fields.length - 1].isEmpty();
                int n = lastLineIsEmpty ? fields.length : fields.length - 1;
                for (int i = 0; i < n; i++) {
                    fields[i] += "\\n";
                }
                int linesNumber = lastLineIsEmpty && fields.length > 1 ? fields.length - 1 : fields.length;
                return autoFormat(Arrays.stream(fields, 0, linesNumber)
                        .map(this::toLiteral)
                        .map(lit -> (Expression) lit.withPrefix(Space.build("\n", Collections.emptyList())))
                        .reduce(this::concatLiterals)
                        .get(), ctx);
            }
            return literal;
        }

        private Expression toLiteral(String str) {
            return new J.Literal(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    str,
                    quote(str),
                    Collections.emptyList(),
                    Primitive.String);
        }

        private String quote(String str) {
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }

        private Expression concatLiterals(Expression left, Expression right) {
            return new J.Binary(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    left,
                    padLeft(Space.build(" ", Collections.emptyList()), J.Binary.Type.Addition),
                    right,
                    left.getType());
        }

        private <T> JLeftPadded<T> padLeft(Space left, T tree) {
            return new JLeftPadded<>(left, tree, Markers.EMPTY);
        }

    }

}
