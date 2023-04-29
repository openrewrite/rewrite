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

import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType.Primitive;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.time.Duration;
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(13), new ReplaceTextBlockWithStringVisitor());
    }

    private static class ReplaceTextBlockWithStringVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
            if (literal.getType() == Primitive.String &&
                literal.getValue() != null &&
                literal.getValueSource() != null &&
                literal.getValueSource().startsWith("\"\"\"")) {
                // Split the literal into lines, including trailing empty lines
                String[] lines = ((String) literal.getValue()).split("\n", -1);
                // Add trailing "\n" to each line but the last one
                // If there is only one line and it's empty, then add "\n" to it as well
                boolean lastLineIsEmpty = lines[lines.length - 1].isEmpty();
                int n = lastLineIsEmpty && lines.length == 1 ? 1 : lines.length - 1;
                for (int i = 0; i < n; i++) {
                    lines[i] += "\\n";
                }
                // Take all lines except the last one if it's empty
                // If there is only one line and it's empty, take it as well
                int linesNumber = !lastLineIsEmpty || lines.length == 1 ? lines.length : lines.length - 1;
                Expression[] literals = new Expression[linesNumber];
                // Add a prefix (possibly containing a comment) of the original literal
                literals[0] = toLiteral(lines[0]).withPrefix(literal.getPrefix());
                // Add newlines before rest string literals
                for (int i = 1; i < linesNumber; i++) {
                    literals[i] = toLiteral(lines[i]).withPrefix(Space.build("\n", Collections.emptyList()));
                }
                // Format the resulting expression
                Expression j = ChainStringBuilderAppendCalls.additiveExpression(literals);
                //noinspection DataFlowIssue
                return j == null ? null : autoFormat(j, ctx);
            }
            return literal;
        }

        private J.Literal toLiteral(String str) {
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
    }

}
