/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDuplicateDependencies extends Recipe {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    String displayName = "Remove duplicate Gradle dependencies";

    String description = "Removes duplicated dependency declarations from the `dependencies` block of `build.gradle` " +
            "and `build.gradle.kts` files. A declaration is only considered a duplicate when an earlier declaration " +
            "in the same `dependencies` block uses the same configuration and the same coordinates, including " +
            "version, classifier and extension. Declarations that differ only by version are left untouched.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!"dependencies".equals(m.getSimpleName()) ||
                        m.getArguments().size() != 1 ||
                        !(m.getArguments().get(0) instanceof J.Lambda)) {
                    return m;
                }
                J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                if (!(lambda.getBody() instanceof J.Block)) {
                    return m;
                }
                J.Block body = (J.Block) lambda.getBody();

                Cursor methodCursor = new Cursor(getCursor().getParentOrThrow(), m);
                Cursor bodyCursor = new Cursor(new Cursor(methodCursor, lambda), body);

                Set<String> seen = new HashSet<>();
                List<Statement> statements = new ArrayList<>(body.getStatements().size());
                Space lineCommentOfLastKept = null;
                for (Statement statement : body.getStatements()) {
                    String key = dependencyKey(bodyCursor, statement);
                    if (key == null || seen.add(key)) {
                        if (lineCommentOfLastKept != null) {
                            statement = statement.withPrefix(afterRemoved(lineCommentOfLastKept, statement.getPrefix()));
                            lineCommentOfLastKept = null;
                        }
                        statements.add(statement);
                    } else if (lineCommentOfLastKept == null) {
                        lineCommentOfLastKept = sameLineComment(statement.getPrefix());
                    }
                }
                if (statements.size() == body.getStatements().size()) {
                    return m;
                }
                if (lineCommentOfLastKept != null) {
                    body = body.withEnd(afterRemoved(lineCommentOfLastKept, body.getEnd()));
                }
                return m.withArguments(singletonList(lambda.withBody(body.withStatements(statements))));
            }

            private @Nullable String dependencyKey(Cursor bodyCursor, Statement statement) {
                Cursor cursor;
                if (statement instanceof J.MethodInvocation) {
                    cursor = new Cursor(bodyCursor, statement);
                } else if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation) {
                    cursor = new Cursor(new Cursor(bodyCursor, statement), ((J.Return) statement).getExpression());
                } else {
                    return null;
                }
                if (!GradleDependency.isDependencyDeclaration(cursor)) {
                    return null;
                }
                J.MethodInvocation dependency = cursor.getValue();
                if (dependency.getArguments().isEmpty() || dependency.getArguments().get(0) instanceof J.Empty) {
                    return null;
                }
                return (dependency.getSelect() == null ? "" : canonical(dependency.getSelect(), cursor) + ".") +
                        dependency.getSimpleName() + canonical(dependency.getArguments(), cursor);
            }
        });
    }

    // A comment on the line of a statement is held by the prefix of whatever follows it
    private static Space sameLineComment(Space prefix) {
        if (prefix.getComments().isEmpty() || prefix.getWhitespace().contains("\n")) {
            return Space.EMPTY;
        }
        return Space.build(prefix.getWhitespace(), singletonList(prefix.getComments().get(0).withSuffix("")));
    }

    private static Space afterRemoved(Space lineCommentOfLastKept, Space prefix) {
        List<Comment> comments = prefix.getComments();
        Space own = sameLineComment(prefix).getComments().isEmpty() ? prefix :
                Space.build(comments.get(0).getSuffix(), comments.subList(1, comments.size()));
        if (lineCommentOfLastKept.getComments().isEmpty()) {
            return own;
        }
        return Space.build(lineCommentOfLastKept.getWhitespace(), ListUtils.insert(own.getComments(),
                lineCommentOfLastKept.getComments().get(0).withSuffix(own.getWhitespace()), 0));
    }

    private static String canonical(List<Expression> arguments, Cursor cursor) {
        if (arguments.size() == 1 && arguments.get(0) instanceof G.MapLiteral) {
            // `implementation([group: 'g', name: 'a'])` is equivalent to `implementation group: 'g', name: 'a'`
            arguments = new ArrayList<>(((G.MapLiteral) arguments.get(0)).getElements());
        }
        List<String> parts = new ArrayList<>(arguments.size());
        boolean named = true;
        for (Expression argument : arguments) {
            if (!(argument instanceof G.MapEntry) && !(argument instanceof J.Assignment)) {
                named = false;
            }
            parts.add(canonical(argument, cursor));
        }
        if (named) {
            Collections.sort(parts);
        }
        return parts.stream().collect(joining(",", "(", ")"));
    }

    private static String canonical(Expression expression, Cursor cursor) {
        if (expression instanceof J.Literal) {
            J.Literal literal = (J.Literal) expression;
            return literal.getValue() instanceof String ?
                    "'" + literal.getValue() + "'" :
                    String.valueOf(literal.getValueSource());
        }
        if (expression instanceof G.MapEntry) {
            G.MapEntry entry = (G.MapEntry) expression;
            return canonical(entry.getKey(), cursor) + ":" + canonical(entry.getValue(), cursor);
        }
        if (expression instanceof J.Assignment && ((J.Assignment) expression).getVariable() instanceof J.Identifier) {
            J.Assignment assignment = (J.Assignment) expression;
            return "'" + ((J.Identifier) assignment.getVariable()).getSimpleName() + "':" +
                    canonical(assignment.getAssignment(), cursor);
        }
        if (expression instanceof J.MethodInvocation && ((J.MethodInvocation) expression).getSelect() == null) {
            J.MethodInvocation invocation = (J.MethodInvocation) expression;
            return invocation.getSimpleName() + canonical(invocation.getArguments(), cursor);
        }
        return WHITESPACE.matcher(expression.withMarkers(Markers.EMPTY).printTrimmed(cursor)).replaceAll(" ");
    }
}
