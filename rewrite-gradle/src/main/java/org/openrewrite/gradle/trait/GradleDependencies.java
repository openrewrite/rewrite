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
import org.openrewrite.java.trait.Block;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
                                    .map(block -> block.mapStatements(statement -> {
                                        if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof Statement) {
                                            Statement originalStatement = (Statement) ((J.Return) statement).getExpression();
                                            Statement newStatement = mapper.apply(originalStatement);
                                            if (originalStatement == newStatement) {
                                                return statement;
                                            } else if (newStatement instanceof Expression) {
                                                return ((J.Return) statement).withExpression((Expression) newStatement);
                                            } else if (newStatement == null) {
                                                return null;
                                            }
                                        }
                                        return mapper.apply(statement);
                                    }))
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
}
