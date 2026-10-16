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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDuplicateDependencies extends Recipe {

    String displayName = "Remove duplicate Gradle dependencies";

    String description = "Removes duplicated dependency declarations from the `dependencies` block of `build.gradle` " +
            "and `build.gradle.kts` files. A declaration is only considered a duplicate when an earlier declaration " +
            "in the same `dependencies` block uses the same configuration and the same coordinates, including " +
            "version, classifier and extension, whether those are written as a single string or as separate " +
            "arguments. Declarations that differ only by version are left untouched.";

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
                // The prefix of the removed duplicates, to be carried over to the next statement that is kept
                Space removedPrefix = null;
                boolean previousRemoved = false;
                for (Statement statement : body.getStatements()) {
                    Space prefix = previousRemoved ? withoutTrailingComments(statement.getPrefix()) : statement.getPrefix();
                    String key = dependencyKey(bodyCursor, statement);
                    if (key == null) {
                        // Any other statement, such as a variable assignment, could change what a later
                        // declaration that reads the same refers to, so only compare declarations that are adjacent
                        seen.clear();
                    } else if (!seen.add(key)) {
                        removedPrefix = removedPrefix == null ? prefix : merge(removedPrefix, prefix, true);
                        previousRemoved = true;
                        continue;
                    }
                    if (removedPrefix != null) {
                        prefix = merge(removedPrefix, prefix, true);
                        removedPrefix = null;
                    }
                    statements.add(statement.withPrefix(prefix));
                    previousRemoved = false;
                }
                if (statements.size() == body.getStatements().size()) {
                    return m;
                }
                Space end = previousRemoved ? withoutTrailingComments(body.getEnd()) : body.getEnd();
                if (removedPrefix != null) {
                    end = merge(removedPrefix, end, false);
                }
                return m.withArguments(singletonList(lambda.withBody(body.withStatements(statements).withEnd(end))));
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
                if (dependency.getArguments().isEmpty() || dependency.getArguments().get(0) instanceof J.Empty ||
                        callsMethodOnReceiver(dependency.getArguments())) {
                    return null;
                }
                return (dependency.getSelect() == null ? "" : canonical(dependency.getSelect(), cursor) + ".") +
                        dependency.getSimpleName() + canonical(dependency.getArguments(), cursor);
            }
        });
    }

    /**
     * A call such as `iterator.next()` may return something else each time, so identical looking
     * declarations containing it are not necessarily duplicates.
     */
    private static boolean callsMethodOnReceiver(List<Expression> arguments) {
        return new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if (method.getSelect() != null) {
                    found.set(true);
                    return method;
                }
                return super.visitMethodInvocation(method, found);
            }
        }.reduce(arguments, new AtomicBoolean()).get();
    }

    /**
     * Drop the comments at the start of a space that are on the same line as the preceding element,
     * as they belong to that element.
     */
    private static Space withoutTrailingComments(Space space) {
        List<Comment> comments = space.getComments();
        String whitespace = space.getWhitespace();
        int i = 0;
        while (i < comments.size() && !whitespace.contains("\n")) {
            whitespace = comments.get(i).getSuffix();
            i++;
        }
        return i == 0 ? space : Space.build(whitespace, comments.subList(i, comments.size()));
    }

    /**
     * Combine the prefix of removed statements with the prefix of the element that follows them, so that comments
     * of the removed statements are not lost and, optionally, a blank line before them is retained.
     */
    private static Space merge(Space removed, Space next, boolean keepBlankLines) {
        if (removed.getComments().isEmpty()) {
            if (keepBlankLines &&
                    newlines(removed.getWhitespace()) > newlines(next.getWhitespace()) &&
                    next.getWhitespace().contains("\n")) {
                String r = removed.getWhitespace();
                String n = next.getWhitespace();
                return next.withWhitespace(r.substring(0, r.lastIndexOf('\n')) + n.substring(n.lastIndexOf('\n')));
            }
            return next;
        }
        List<Comment> comments = new ArrayList<>(removed.getComments());
        int last = comments.size() - 1;
        comments.set(last, comments.get(last).withSuffix(next.getWhitespace()));
        comments.addAll(next.getComments());
        return Space.build(removed.getWhitespace(), comments);
    }

    private static long newlines(String whitespace) {
        return whitespace.chars().filter(c -> c == '\n').count();
    }

    /**
     * Produce a representation of the arguments of a dependency declaration that is insensitive to formatting,
     * quote style, and the order of named arguments, so that semantically identical declarations compare as equal.
     */
    private static String canonical(List<Expression> arguments, Cursor cursor) {
        if (arguments.size() == 1 && arguments.get(0) instanceof G.MapLiteral) {
            // `implementation([group: 'g', name: 'a'])` is equivalent to `implementation group: 'g', name: 'a'`
            arguments = new ArrayList<>(((G.MapLiteral) arguments.get(0)).getElements());
        } else if (arguments.size() == 1 && arguments.get(0) instanceof J.Literal &&
                ((J.Literal) arguments.get(0)).getValue() instanceof String) {
            String notation = (String) ((J.Literal) arguments.get(0)).getValue();
            Dependency dependency = DependencyNotation.parse(notation);
            if (dependency != null && DependencyNotation.toStringNotation(dependency).equals(notation)) {
                return asNamedArguments(dependency);
            }
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

    /**
     * The same representation as the named arguments `group: 'g', name: 'a', version: 'v'` produce, so that
     * `'g:a:v'` compares as equal to them.
     */
    private static String asNamedArguments(Dependency dependency) {
        Map<String, @Nullable String> components = new TreeMap<>();
        components.put("classifier", dependency.getClassifier());
        components.put("ext", dependency.getType());
        components.put("group", dependency.getGroupId());
        components.put("name", dependency.getArtifactId());
        components.put("version", dependency.getVersion());
        return components.entrySet().stream()
                .filter(component -> component.getValue() != null)
                .map(component -> "'" + component.getKey() + "':'" + component.getValue() + "'")
                .collect(joining(",", "(", ")"));
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
        // Collapse formatting, but not the contents of string literals
        J normalized = new JavaIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(@Nullable Space space, Space.Location loc, Integer p) {
                return space == null || space.isEmpty() ? space : Space.SINGLE_SPACE;
            }
        }.visitNonNull(expression, 0, cursor);
        return normalized.withMarkers(Markers.EMPTY).printTrimmed(cursor);
    }
}
