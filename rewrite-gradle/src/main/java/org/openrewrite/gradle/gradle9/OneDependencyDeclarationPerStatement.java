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
package org.openrewrite.gradle.gradle9;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class OneDependencyDeclarationPerStatement extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use one dependency declaration per statement";
    }

    @Override
    public String getDescription() {
        return "The Gradle Groovy DSL accepts multiple coordinates in a single configuration call " +
                "(e.g. `implementation 'a:b:1.0', 'c:d:2.0'`), but the Kotlin DSL does not. " +
                "Gradle's best practices recommend declaring a single dependency per statement; " +
                "see the [Gradle dependency best practices](https://docs.gradle.org/current/userguide/best_practices_dependencies.html). " +
                "This recipe splits multi-coordinate Groovy DSL configuration calls into one call per coordinate. " +
                "Run this as a cleanup pass before other dependency-aware recipes (e.g. `UpgradeDependencyVersion`, " +
                "`ChangeDependency`, `RemoveDependency`): those recipes use the `GradleDependency` trait, which only " +
                "inspects the first argument of a configuration call. Coordinates in later positions are invisible " +
                "to them until this recipe reshapes the source into one declaration per statement.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile && !(tree instanceof G.CompilationUnit)) {
                    // Kotlin DSL cannot express the multi-coordinate-per-call form
                    return (J) tree;
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                if (!isInsideDependenciesButNotConstraints(getCursor())) {
                    return b;
                }
                return b.withStatements(ListUtils.flatMap(b.getStatements(), s -> {
                    J.MethodInvocation m;
                    boolean wrappedInReturn;
                    if (s instanceof J.MethodInvocation) {
                        m = (J.MethodInvocation) s;
                        wrappedInReturn = false;
                    } else if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof J.MethodInvocation) {
                        m = (J.MethodInvocation) ((J.Return) s).getExpression();
                        wrappedInReturn = true;
                    } else {
                        return s;
                    }
                    if (m.getSelect() != null) {
                        return s;
                    }
                    List<Expression> args = m.getArguments();
                    if (args.size() < 2) {
                        return s;
                    }
                    // A trailing closure configures a single dependency; leave it alone
                    if (args.get(args.size() - 1) instanceof J.Lambda) {
                        return s;
                    }
                    for (Expression arg : args) {
                        if (!isCoordinateShape(arg)) {
                            return s;
                        }
                    }
                    if (isMultiComponentLiterals(args)) {
                        return s;
                    }

                    // The original statement's prefix may carry a comment that referred to the whole
                    // multi-coordinate line. Keep it attached to the first split only — duplicating
                    // it onto every new statement (including the trailing implicit-return) would be
                    // both misleading and visually noisy.
                    Space stmtPrefix = s.getPrefix();
                    Space subsequentPrefix = Space.build(stmtPrefix.getLastWhitespace(), emptyList());

                    List<Statement> split = new ArrayList<>(args.size());
                    for (int i = 0; i < args.size(); i++) {
                        Expression coord = args.get(i).withPrefix(Space.format(" "));
                        boolean isLast = i == args.size() - 1;
                        if (isLast && wrappedInReturn) {
                            J.MethodInvocation innerMi = m.withArguments(singletonList(coord));
                            split.add(((J.Return) s).withPrefix(subsequentPrefix).withExpression(innerMi));
                        } else {
                            Space prefix = i == 0 ? stmtPrefix : subsequentPrefix;
                            split.add(m.withPrefix(prefix).withArguments(singletonList(coord)));
                        }
                    }
                    return split;
                }));
            }
        });
    }

    private static boolean isInsideDependenciesButNotConstraints(Cursor cursor) {
        boolean insideDependencies = false;
        Cursor c = cursor.getParent();
        while (c != null) {
            Object value = c.getValue();
            if (value instanceof J.MethodInvocation) {
                String name = ((J.MethodInvocation) value).getSimpleName();
                if ("constraints".equals(name)) {
                    return false;
                }
                if ("dependencies".equals(name)) {
                    insideDependencies = true;
                }
            }
            c = c.getParent();
        }
        return insideDependencies;
    }

    private static boolean isCoordinateShape(Expression arg) {
        if (arg instanceof J.Literal || arg instanceof J.Binary || arg instanceof G.GString) {
            return true;
        }
        if (arg instanceof J.MethodInvocation) {
            String name = ((J.MethodInvocation) arg).getSimpleName();
            return "platform".equals(name) || "enforcedPlatform".equals(name) || "project".equals(name);
        }
        return false;
    }

    private static boolean isMultiComponentLiterals(List<Expression> args) {
        if (args.size() < 2 || args.size() > 4) {
            return false;
        }
        for (Expression arg : args) {
            if (!(arg instanceof J.Literal) || !(((J.Literal) arg).getValue() instanceof String)) {
                return false;
            }
        }
        String first = (String) ((J.Literal) args.get(0)).getValue();
        return first != null && !first.contains(":");
    }
}
