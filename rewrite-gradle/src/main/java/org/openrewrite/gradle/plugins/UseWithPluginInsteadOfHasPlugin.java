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
package org.openrewrite.gradle.plugins;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class UseWithPluginInsteadOfHasPlugin extends Recipe {

    private static final Set<String> PLUGIN_CONTAINERS = new HashSet<>(asList("plugins", "pluginManager"));

    // Inside the `withPlugin` action these resolve to the `AppliedPlugin` or the closure, not the project
    private static final Set<String> SHADOWED_NAMES = new HashSet<>(asList(
            "it", "this", "delegate", "owner", "id", "name", "namespace", "getId", "getName", "getNamespace"));

    @Getter
    final String displayName = "Use `pluginManager.withPlugin` instead of checking `hasPlugin`";

    @Getter
    final String description = "Replaces `if (plugins.hasPlugin(\"id\")) { ... }` with `pluginManager.withPlugin(\"id\") { ... }`. " +
            "`hasPlugin` only checks whether the plugin has been applied so far, so the block is silently skipped " +
            "when the plugin is applied later; `withPlugin` runs the block whenever the plugin is applied, before or after. " +
            "Conditions with an `else` branch, or blocks that return, assign script variables, or refer to names the " +
            "`withPlugin` action would shadow, are left unchanged. " +
            "See [don't assume your plugin is applied after another](https://docs.gradle.org/current/userguide/best_practices_general.html#dont_assume_plugin_order).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitIf(J.If iff, ExecutionContext ctx) {
                J.If i = (J.If) super.visitIf(iff, ctx);
                J.MethodInvocation hasPlugin = hasPluginCondition(i);
                if (hasPlugin == null || i.getElsePart() != null || !(i.getThenPart() instanceof J.Block) ||
                    !isStatement() || !isSafeToDefer((J.Block) i.getThenPart())) {
                    return i;
                }
                J.MethodInvocation withPlugin = withPluginTemplate(ctx);
                if (withPlugin == null) {
                    return i;
                }
                J.Block thenBlock = (J.Block) i.getThenPart();
                Expression pluginManager = toPluginManager(hasPlugin.getSelect());
                Expression pluginId = hasPlugin.getArguments().get(0);
                return withPlugin
                        .withPrefix(i.getPrefix())
                        .withSelect(pluginManager.withPrefix(withPlugin.getSelect() == null ? Space.EMPTY : withPlugin.getSelect().getPrefix()))
                        .withArguments(ListUtils.map(withPlugin.getArguments(), arg -> {
                            if (arg instanceof J.Lambda) {
                                J.Lambda lambda = (J.Lambda) arg;
                                J.Block body = (J.Block) lambda.getBody();
                                return lambda.withBody(body
                                        .withStatements(thenBlock.getStatements())
                                        .withEnd(thenBlock.getEnd()));
                            }
                            return pluginId.withPrefix(arg.getPrefix());
                        }));
            }

            private boolean isStatement() {
                Object parent = getCursor().getParentTreeCursor().getValue();
                return parent instanceof J.Block || parent instanceof JavaSourceFile;
            }

            private J.@Nullable MethodInvocation withPluginTemplate(ExecutionContext ctx) {
                boolean kotlin = getCursor().firstEnclosing(K.CompilationUnit.class) != null;
                SourceFile parsed = GradleParser.builder().build()
                        .parseInputs(singletonList(Parser.Input.fromString(
                                Paths.get(kotlin ? "build.gradle.kts" : "build.gradle"),
                                "pluginManager.withPlugin(\"id\") {\n}")), null, ctx)
                        .findFirst()
                        .orElse(null);
                if (parsed instanceof ParseError) {
                    return null;
                }
                Statement statement = null;
                if (parsed instanceof G.CompilationUnit) {
                    statement = ((G.CompilationUnit) parsed).getStatements().get(0);
                } else if (parsed instanceof K.CompilationUnit && ((K.CompilationUnit) parsed).getStatements().get(0) instanceof J.Block) {
                    statement = ((J.Block) ((K.CompilationUnit) parsed).getStatements().get(0)).getStatements().get(0);
                }
                return statement instanceof J.MethodInvocation ? (J.MethodInvocation) statement : null;
            }

            private boolean isSafeToDefer(J.Block thenBlock) {
                Set<String> scriptVariables = new HashSet<>();
                new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<String> names) {
                        names.add(variable.getSimpleName());
                        return super.visitVariable(variable, names);
                    }
                }.visit(getCursor().firstEnclosingOrThrow(JavaSourceFile.class), scriptVariables);

                AtomicBoolean unsafe = new AtomicBoolean();
                new JavaIsoVisitor<AtomicBoolean>() {
                    int lambdaDepth;
                    int loopDepth;

                    @Override
                    public J.Lambda visitLambda(J.Lambda lambda, AtomicBoolean u) {
                        lambdaDepth++;
                        J.Lambda l = super.visitLambda(lambda, u);
                        lambdaDepth--;
                        return l;
                    }

                    @Override
                    public J.ForLoop visitForLoop(J.ForLoop forLoop, AtomicBoolean u) {
                        loopDepth++;
                        J.ForLoop f = super.visitForLoop(forLoop, u);
                        loopDepth--;
                        return f;
                    }

                    @Override
                    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, AtomicBoolean u) {
                        loopDepth++;
                        J.ForEachLoop f = super.visitForEachLoop(forLoop, u);
                        loopDepth--;
                        return f;
                    }

                    @Override
                    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, AtomicBoolean u) {
                        loopDepth++;
                        J.WhileLoop w = super.visitWhileLoop(whileLoop, u);
                        loopDepth--;
                        return w;
                    }

                    @Override
                    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, AtomicBoolean u) {
                        loopDepth++;
                        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, u);
                        loopDepth--;
                        return d;
                    }

                    @Override
                    public J.Return visitReturn(J.Return _return, AtomicBoolean u) {
                        if (lambdaDepth == 0) {
                            u.set(true);
                        }
                        return super.visitReturn(_return, u);
                    }

                    @Override
                    public J.Break visitBreak(J.Break breakStatement, AtomicBoolean u) {
                        if (lambdaDepth == 0 && loopDepth == 0) {
                            u.set(true);
                        }
                        return breakStatement;
                    }

                    @Override
                    public J.Continue visitContinue(J.Continue continueStatement, AtomicBoolean u) {
                        if (lambdaDepth == 0 && loopDepth == 0) {
                            u.set(true);
                        }
                        return continueStatement;
                    }

                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean u) {
                        if (assignsScriptVariable(assignment.getVariable())) {
                            u.set(true);
                        }
                        return super.visitAssignment(assignment, u);
                    }

                    @Override
                    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, AtomicBoolean u) {
                        if (assignsScriptVariable(assignOp.getVariable())) {
                            u.set(true);
                        }
                        return super.visitAssignmentOperation(assignOp, u);
                    }

                    @Override
                    public J.Unary visitUnary(J.Unary unary, AtomicBoolean u) {
                        if (unary.getOperator().isModifying() && assignsScriptVariable(unary.getExpression())) {
                            u.set(true);
                        }
                        return super.visitUnary(unary, u);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean u) {
                        if (method.getSelect() == null && SHADOWED_NAMES.contains(method.getSimpleName())) {
                            u.set(true);
                        }
                        return super.visitMethodInvocation(method, u);
                    }

                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean u) {
                        if (SHADOWED_NAMES.contains(identifier.getSimpleName()) && isUnqualifiedReference(identifier)) {
                            u.set(true);
                        }
                        return identifier;
                    }

                    private boolean isUnqualifiedReference(J.Identifier identifier) {
                        Object parent = getCursor().getParentTreeCursor().getValue();
                        if (parent instanceof J.FieldAccess) {
                            return ((J.FieldAccess) parent).getName() != identifier;
                        }
                        if (parent instanceof J.MethodInvocation) {
                            return ((J.MethodInvocation) parent).getName() != identifier;
                        }
                        return !(parent instanceof J.VariableDeclarations.NamedVariable);
                    }

                    private boolean assignsScriptVariable(Expression variable) {
                        return variable instanceof J.Identifier &&
                               scriptVariables.contains(((J.Identifier) variable).getSimpleName());
                    }
                }.visit(thenBlock, unsafe, getCursor());
                return !unsafe.get();
            }
        });
    }

    private static J.@Nullable MethodInvocation hasPluginCondition(J.If iff) {
        Expression condition = iff.getIfCondition().getTree();
        if (!(condition instanceof J.MethodInvocation)) {
            return null;
        }
        J.MethodInvocation m = (J.MethodInvocation) condition;
        if (!"hasPlugin".equals(m.getSimpleName()) || m.getArguments().size() != 1 ||
            !(m.getArguments().get(0) instanceof J.Literal) ||
            !(((J.Literal) m.getArguments().get(0)).getValue() instanceof String)) {
            return null;
        }
        Expression select = m.getSelect();
        if (select instanceof J.Identifier && PLUGIN_CONTAINERS.contains(((J.Identifier) select).getSimpleName()) ||
            select instanceof J.FieldAccess && PLUGIN_CONTAINERS.contains(((J.FieldAccess) select).getSimpleName())) {
            return m;
        }
        return null;
    }

    private static Expression toPluginManager(@Nullable Expression select) {
        if (select instanceof J.Identifier) {
            return ((J.Identifier) select).withSimpleName("pluginManager");
        }
        J.FieldAccess fieldAccess = (J.FieldAccess) select;
        return fieldAccess.withName(fieldAccess.getName().withSimpleName("pluginManager"));
    }
}
