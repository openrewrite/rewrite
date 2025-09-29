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
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class GradleDependencies implements Trait<J.MethodInvocation> {
    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    @Getter
    private final Cursor cursor;

    public @Nullable GradleDependencies removeDependency(@Nullable String group, @Nullable String artifact) {
        return filterStatements(statement -> !new GradleDependency.Matcher().artifactId(artifact).groupId(group).get(statement, getCursor()).isPresent());
    }

    public @Nullable GradleDependencies filterStatements(Predicate<Statement> predicate) {
        return mapStatements(statement -> predicate.test(statement) ? statement : null);
    }

    public @Nullable GradleDependencies mapStatements(Function<Statement, @Nullable Statement> mapper) {
        GradleProject gradleProject = getGradleProject();
        if (gradleProject == null) {
            return this;
        }

        List<Expression> arguments = ListUtils.mapFirst(getTree().getArguments(), (Function<Expression, Expression>) expression -> {
                    if (expression instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) expression;
                        if (lambda.getBody() instanceof J.Block) {
                            return new Block.Matcher().get(lambda.getBody(), new Cursor(cursor, lambda))
                                    .map(block -> block.mapStatements(mapper))
                                    .map(Trait::getTree)
                                    .filter(block -> !(block.getStatements().isEmpty() && block.getComments().isEmpty() && block.getEnd().getComments().isEmpty()))
                                    .map(lambda::withBody)
                                    .orElse(null);
                        }
                        return lambda;
                    }
                    return expression;
                }
        );
        if (arguments.isEmpty()) {
            return null;
        }
        Cursor newCursor = new Cursor(this.cursor.getParent(), getTree().withArguments(arguments));

        return new GradleDependencies(newCursor);
    }

    private @Nullable GradleProject getGradleProject() {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return null;
        }

        Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
        return maybeGp.orElse(null);
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

                if (!"dependencies".equals(methodInvocation.getSimpleName())) {
                    return null;
                }

                GradleProject gradleProject = getGradleProject(cursor);
                if (gradleProject == null && !(DEPENDENCY_DSL_MATCHER.matches(methodInvocation))) {
                    return null;
                }

                return new GradleDependencies(cursor);
            }

            return null;
        }
    }

    @RequiredArgsConstructor
    private static class Block implements Trait<J.Block> {
        @Getter
        private final Cursor cursor;

        public Block mapStatements(Function<Statement, @Nullable Statement> mapper) {
            return mapStatements(mapper, (J.Return::withExpression));
        }

        public Block mapStatements(Function<Statement, @Nullable Statement> mapper, BiFunction<J.Return, Expression, J.Return> returnMapper) {
            J.Block block = getTree();
            List<Statement> statements = block.getStatements();
            List<Statement> newStatements = new LinkedList<>();
            LinkedList<Comment> comments = new LinkedList<>();
            String whitespace = null;
            boolean lastStatementWasKept = false;
            for (Statement statement : statements) {
                if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof Statement) {
                    statement = ((J.Return) statement).getExpression().withPrefix(statement.getPrefix());
                }
                Statement mappedStatement = mapper.apply(statement);
                if (mappedStatement != null) {
                    // We want to keep the element (it might have been changed though).
                    List<Comment> statementComments = mappedStatement.getComments();
                    if (!comments.isEmpty() && whitespace != null) {
                        //A previous statement was removed, it had comments and we now need to add these comments to the current element.
                        Comment last = comments.removeLast();
                        if (mappedStatement.getPrefix().getWhitespace().contains("\n")) {
                            // We need to add the prefix of the current statement as the suffix of the last comment of the previous block.
                            comments.addLast(last.withSuffix(mappedStatement.getPrefix().getWhitespace()));
                            comments.addAll(statementComments);
                        } else {
                            // Add the prefix of the current statement as the suffix of the last comment which is on the same line. (multiple multiline comments could be present)
                            int j = 0;
                            while (j < statementComments.size() && !statementComments.get(j).getSuffix().contains("\n")) {
                                j++;
                            }
                            comments.add(last.withSuffix(statementComments.get(j).getSuffix()));
                            // Now that we have suffixed the correct comment, we can add remaining comments of the current statement
                            if (j + 1 < statementComments.size()) {
                                comments.addAll(statementComments.subList(j + 1, statementComments.size()));
                            }
                        }
                        //make sure that the previous removed element's whitespace get used and the newly calculated list of comments containing also the previous removed element's comments.
                        statement = mappedStatement.withPrefix(mappedStatement.getPrefix().withWhitespace(whitespace).withComments(comments));
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
                            statement = statement.withPrefix(statement.getPrefix().withWhitespace(whitespace).withComments(emptyList()));
                        }
                    }
                    comments = new LinkedList<>();
                    whitespace = null;
                    newStatements.add(statement);
                    lastStatementWasKept = true;
                } else {
                    List<Comment> statementComments = statement.getComments();
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

            newStatements = ListUtils.mapLast(newStatements, (Function<Statement, Statement>) newLast -> {
                Statement currentLast = statements.get(statements.size() - 1);
                if (currentLast instanceof J.Return && !(newLast instanceof J.Return)) {
                    if (newLast instanceof Expression) {
                        J.Return currentReturn = (J.Return) currentLast;
                        Space statementPrefix = currentReturn.getMarkers().findFirst(ImplicitReturn.class).isPresent() ? Space.EMPTY : Space.SINGLE_SPACE;
                        J.Return mappedReturn = returnMapper.apply(currentReturn, newLast.withPrefix(statementPrefix));
                        if (mappedReturn.getExpression() instanceof Statement) {
                            if (mapper.apply((Statement) mappedReturn.getExpression()) == null) {
                                throw new IllegalArgumentException("The return statement replacement result should not be one that gets filtered out to avoid cyclic changes that result in the entire block being cleared. Did you return something from the old return that still return null when the mapper would be applied?");
                            }
                        } else if (mapper.apply(mappedReturn) == null) {
                            throw new IllegalArgumentException("The return statement replacement result should not be one that gets filtered out to avoid cyclic changes that result in the entire block being cleared. Did you return something from the old return that still return null when the mapper would be applied?");
                        }
                        return mappedReturn
                                .withPrefix(newLast.getPrefix());
                    }
                }
                return newLast;
            });
            if (statements.equals(newStatements) && comments.isEmpty()) {
                return this;
            }

            if (!lastStatementWasKept) {
                block = block.withEnd(buildEnd(block.getEnd(), comments, whitespace));
            }

            block = block.withStatements(newStatements);

            return new Block(new Cursor(this.cursor.getParent(), block));
        }

        private Space buildEnd(Space end, List<Comment> comments, @Nullable String endWhitespace) {
            List<Comment> newComments = new ArrayList<>();
            String endBlockWhitespace = null;
            if (!comments.isEmpty()) {
                for (Comment comment : comments) {
                    if (comment instanceof TextComment) {
                        newComments.add(comment);
                        if (endBlockWhitespace == null) {
                            endBlockWhitespace = endWhitespace;
                        }
                    }
                }
            }

            List<Comment> existingEndComments = new ArrayList<>();
            boolean eolComments = !end.getWhitespace().contains("\n");
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

        private static class Matcher extends SimpleTraitMatcher<Block> {
            @Override
            public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<Block, P> visitor) {
                return new JavaVisitor<P>() {
                    @Override
                    public J visitBlock(J.Block block, P p) {
                        Block trait = test(getCursor());
                        return trait != null ?
                                (J) visitor.visit(trait, p) :
                                super.visitBlock(block, p);
                    }
                };
            }

            @Override
            protected @Nullable Block test(Cursor cursor) {
                Object object = cursor.getValue();
                if (object instanceof J.Block) {
                    return new Block(cursor);
                }

                return null;
            }
        }
    }
}
