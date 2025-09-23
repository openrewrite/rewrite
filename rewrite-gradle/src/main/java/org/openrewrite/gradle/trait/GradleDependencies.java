/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.tree.*;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class GradleDependencies implements Trait<J.MethodInvocation> {
    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    @Getter
    private final Cursor cursor;

    private final List<Statement> statements;

    public @Nullable GradleDependencies withStatements(Predicate<Statement> filter) {
        GradleProject gradleProject = getGradleProject();
        if (gradleProject == null) {
            return this;
        }

        LinkedList<Statement> filteredStatements = new LinkedList<>();
        LinkedList<Comment> comments = new LinkedList<>();
        String whitespace = null;
        boolean lastStatementWasKept = false;
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (statement instanceof J.Return && statement.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
                if (((J.Return) statement).getExpression() instanceof Statement) {
                    statement = ((J.Return) statement).getExpression().withPrefix(statement.getPrefix());
                } else {
                    filteredStatements.add(statement);
                }
            }
            List<Comment> statementComments = statement.getComments();
            if (filter.test(statement)) {
                if (!comments.isEmpty() && whitespace != null) {
                    //A previous statement was removed, and we are joining its comment / whitespace into the current statement
                    Comment last = comments.removeLast();
                    if (statement.getPrefix().getWhitespace().contains("\n")) {
                        comments.addLast(last.withSuffix(statement.getPrefix().getWhitespace()));
                        comments.addAll(statementComments);
                    } else {
                        int j = 0;
                        while (j < statementComments.size() && !statementComments.get(j).getSuffix().contains("\n")) {
                            j++;
                        }
                        comments.add(last.withSuffix(statementComments.get(j).getSuffix()));
                        if (j + 1 < statementComments.size()) {
                            comments.addAll(statementComments.subList(j + 1, statementComments.size()));
                        }
                    }
                    statement = statement.withPrefix(statement.getPrefix().withWhitespace(whitespace).withComments(comments));
                } else if (!lastStatementWasKept && !statement.getPrefix().getWhitespace().contains("\n") && whitespace != null) {
                    int startIndex = 0;
                    int j = 0;
                    while (j < statementComments.size() && !statementComments.get(j).getSuffix().contains("\n")) {
                        j++;
                    }
                    if (j + 1 <= statementComments.size()) {
                        startIndex = j + 1;
                    }
                    if (startIndex < statementComments.size()) {
                        if (startIndex > 0) {
                            statement = statement.withPrefix(statement.getPrefix().withWhitespace(statementComments.get(startIndex - 1).getSuffix()).withComments(statementComments.subList(startIndex, statementComments.size())));
                        }
                    } else {
                        statement = statement.withPrefix(statement.getPrefix().withWhitespace(whitespace).withComments(Collections.emptyList()));
                    }
                }
                comments = new LinkedList<>();
                whitespace = null;
                filteredStatements.add(statement);
                lastStatementWasKept = true;
            } else {
                if (!comments.isEmpty() && whitespace != null) {
                    int startIndex = 0;
                    int endIndex = findIndexOfLastCommentOnNewLine(statementComments) + 1;
                    if (!statement.getPrefix().getWhitespace().contains("\n")) {
                        int j = 0;
                        while (j < statementComments.size() && !statementComments.get(j).getSuffix().contains("\n")) {
                            j++;
                        }
                        Comment last = comments.removeLast();
                        comments.add(last.withSuffix(statementComments.get(j).getSuffix()));
                        if (j + 1 <= statementComments.size()) {
                            startIndex = j + 1;
                        }
                    }
                    if (startIndex < endIndex) {
                        comments.addAll(statementComments.subList(startIndex, endIndex));
                    }
                } else {
                    if (lastStatementWasKept) {
                        comments.addAll(statementComments.subList(0, findIndexOfLastCommentOnNewLine(statementComments) + 1));
                        whitespace = statement.getPrefix().getWhitespace();
                    } else {
                        int startIndex = findIndexOfFirstCommentOnNewLine(statementComments, statement.getPrefix());
                        comments.addAll(statementComments.subList(startIndex, findIndexOfLastCommentOnNewLine(statementComments) + 1));
                        if (startIndex == 0) {
                            whitespace = statement.getPrefix().getWhitespace();
                        } else {
                            whitespace = statementComments.get(startIndex - 1).getSuffix();
                        }
                    }
                }
                lastStatementWasKept = false;
            }
        }

        if (statements.size() == filteredStatements.size() && comments.isEmpty()) {
            return this;
        }

        J.MethodInvocation dependenciesBlock = cursor.getValue();
        LinkedList<Comment> endComments = comments;
        String endWhitespace = whitespace;
        boolean preserveEOLComments = !lastStatementWasKept;
        List<Expression> arguments = ListUtils.mapFirst(dependenciesBlock.getArguments(), expression -> {
                    if (expression instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) expression;
                        if (lambda.getBody() instanceof J.Block) {
                            J.Block body = (J.Block) lambda.getBody();
                            Space end = body.getEnd();
                            if (preserveEOLComments) {
                                end = buildEnd(body.getEnd(), endComments, endWhitespace);
                            }
                            if (filteredStatements.isEmpty() && end.getComments().isEmpty()) {
                                return null;
                            }
                            List<Statement> newStatements = ListUtils.mapLast(filteredStatements, newLast -> {
                                Statement currentLast = statements.get(statements.size() - 1);
                                if (currentLast instanceof J.Return) {
                                    return ((J.Return) currentLast).withExpression(newLast.withPrefix(Space.EMPTY)).withPrefix(newLast.getPrefix());
                                }
                                return newLast;
                            });
                            return lambda.withBody(body.withStatements(newStatements).withEnd(end));
                        }
                    }
                    return expression;
                }
        );
        // Return null to remove the dependencies block entirely if no arguments remain
        if (arguments.isEmpty()) {
            return null;
        }
        dependenciesBlock = dependenciesBlock.withArguments(arguments);
        Cursor newCursor = new Cursor(this.cursor.getParent(), dependenciesBlock);

        return new GradleDependencies(newCursor, filteredStatements);
    }

    private @Nullable GradleProject getGradleProject() {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return null;
        }

        Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
        return maybeGp.orElse(null);
    }

    private Space buildEnd(Space end, LinkedList<Comment> comments, @Nullable String endWhitespace) {
        List<Comment> newComments = new ArrayList<>();
        boolean eolComments = endWhitespace == null || !endWhitespace.contains("\n");
        String endBlockWhitespace = null;
        if (!comments.isEmpty()) {
            for (Comment comment : comments) {
                if (comment instanceof TextComment) {
                    if (comment.getSuffix().contains("\n")) {
                        eolComments = false;
                    }
                    newComments.add(comment);
                    if (endBlockWhitespace == null) {
                        endBlockWhitespace = endWhitespace;
                    }
                }
            }
        }

        List<Comment> existingEndComments = new ArrayList<>();
        eolComments = !end.getWhitespace().contains("\n");
        String whitespace = null;
        if (!eolComments) {
            whitespace = end.getWhitespace();
        }
        if (!end.getComments().isEmpty()) {
            for (Comment comment : end.getComments()) {
                if (comment instanceof TextComment) {
                    if (!eolComments) {
                        existingEndComments.add(comment);
                    } else if (comment.getSuffix().contains("\n")) {
                        eolComments = false;
                    }
                    if (whitespace == null) {
                        whitespace = comment.getSuffix();
                    }
                }
            }
        }
        if (!newComments.isEmpty()) {
            newComments.set(newComments.size() - 1, newComments.get(newComments.size() - 1).withSuffix(whitespace == null ? "" : whitespace));
        } else {
            endBlockWhitespace = whitespace;
        }

        newComments.addAll(existingEndComments);

        return end.withWhitespace(endBlockWhitespace == null ? end.getWhitespace() : endBlockWhitespace).withComments(newComments);
    }

    private static int findIndexOfFirstCommentOnNewLine(List<Comment> comments, Space prefix) {
        if (prefix.getWhitespace().contains("\n")) {
            return 0;
        }
        int index = 0;
        while (index < comments.size()) {
            Comment comment = comments.get(index);
            index++;
            if (comment.getSuffix().contains("\n")) {
                break;
            }
        }
        return index;
    }

    private static int findIndexOfLastCommentOnNewLine(List<Comment> comments) {
        int index = comments.size() - 1;
        while (index >= 0) {
            Comment comment = comments.get(index);
            if (comment.isMultiline()) {
                if (comment.getSuffix().contains("\n")) {
                    return index;
                } else {
                    index--;
                }
            } else {
                return index;
            }
        }
        return index;
    }

    public static class Matcher extends GradleTraitMatcher<GradleDependencies> {
        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleDependencies, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    GradleDependencies dependencies = test(getCursor());
                    return dependencies != null ?
                            (J) visitor.visit(dependencies, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
        }

        @Override
        protected @Nullable GradleDependencies test(Cursor cursor) {
            Object object = cursor.getValue();
            if (object instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

                if (!methodInvocation.getSimpleName().equals("dependencies")) {
                    return null;
                }

                GradleProject gradleProject = getGradleProject(cursor);
                if (gradleProject == null && !(DEPENDENCY_DSL_MATCHER.matches(methodInvocation))) {
                    return null;
                }

                List<Statement> statements = methodInvocation.getArguments().stream()
                        .limit(1)
                        .flatMap(arg -> {
                            if (arg instanceof J.Lambda) {
                                J.Lambda lambda = (J.Lambda) arg;
                                J body = lambda.getBody();
                                if (body instanceof J.Block) {
                                    return ((J.Block) body).getStatements().stream();
                                } else if (body instanceof Statement) {
                                    return Stream.of((Statement) body);
                                }
                            }
                            return Stream.empty();
                        }).collect(Collectors.toList());

                return new GradleDependencies(cursor, statements);
            }

            return null;
        }
    }
}
