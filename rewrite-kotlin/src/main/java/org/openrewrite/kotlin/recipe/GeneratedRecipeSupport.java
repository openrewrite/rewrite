/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package org.openrewrite.kotlin.recipe;

import kotlin.jvm.functions.Function1;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.tree.K;

/**
 * Runtime support for the recipe authoring DSL's K2 compiler plugin
 * (see {@link org.openrewrite.kotlin.recipe.internal.RecipeIrGenerationExtension}).
 * The plugin generates {@link org.openrewrite.Recipe} subclasses whose
 * {@code getVisitor()} body is a single call into one of these factories,
 * keeping the IR codegen small and the matching/replacing logic
 * Java-readable.
 */
public final class GeneratedRecipeSupport {

    private GeneratedRecipeSupport() {}

    /**
     * Visitor for a pattern-mode recipe whose before lambda body is a method
     * invocation rooted at the receiver param. Matching is delegated to
     * {@link MethodMatcher}; the after template is a {@link KotlinTemplate}
     * string with one {@code #{any()}} placeholder per substitution slot.
     *
     * {@code matcherSpecsLines} is a {@code \n}-delimited list of
     * MethodMatcher specs — multi-before recipes
     * ({@code rewrite(b1, b2, ...) to a}) lower to one spec per before lambda
     * and we accept a method invocation when any spec matches.
     *
     * The {@code substitutionSourcesCsv} encodes, in template-left-to-right
     * order, how to fill each placeholder from the matched method invocation:
     * {@code -1} for {@code method.getSelect()} (the receiver), {@code N >= 0}
     * for {@code method.getArguments().get(N)}. The CSV is shared across all
     * matchers because the IR pass requires every before lambda to agree on
     * the captured argument shape (same param-index references and same
     * literal {@code (kind, value)} at each position).
     *
     * Switching from {@code KotlinTemplate.matches} to {@link MethodMatcher} is
     * intentional for v0: {@code KotlinTemplate.matches("#{any()}.foo()", cursor)}
     * does not currently match receiver-placeholder patterns against a
     * concrete invocation — verified by a standalone probe test 2026-05-14.
     */
    public static TreeVisitor<?, ExecutionContext> methodInvocationRewrite(
            String matcherSpecsLines, String afterTemplate, String substitutionSourcesCsv) {
        String[] specs = matcherSpecsLines.isEmpty() ? new String[0] : matcherSpecsLines.split("\n");
        MethodMatcher[] matchers = new MethodMatcher[specs.length];
        for (int i = 0; i < specs.length; i++) {
            matchers[i] = new MethodMatcher(specs[i]);
        }
        int[] substitutionSources = parseCsv(substitutionSourcesCsv);
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                boolean matched = false;
                for (MethodMatcher matcher : matchers) {
                    if (matcher.matches(method)) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    Object[] substitutions = new Object[substitutionSources.length];
                    for (int i = 0; i < substitutionSources.length; i++) {
                        int src = substitutionSources[i];
                        if (src < 0) {
                            if (method.getSelect() == null) {
                                return super.visitMethodInvocation(method, ctx);
                            }
                            substitutions[i] = method.getSelect();
                        } else {
                            if (src >= method.getArguments().size()) {
                                return super.visitMethodInvocation(method, ctx);
                            }
                            substitutions[i] = method.getArguments().get(src);
                        }
                    }
                    return KotlinTemplate.builder(afterTemplate).build()
                            .apply(getCursor(), method.getCoordinates().replace(), substitutions)
                            .withPrefix(method.getPrefix());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }

    /**
     * Visitor for a stateless phase-mode recipe whose body is
     * {@code edit { visitMethodInvocation { call -> ... } }}. The Kotlin lambda
     * receives each method invocation in turn and returns a possibly-transformed
     * {@code J.MethodInvocation}; returning the same instance is a no-op.
     *
     * <p>Unlike {@link #methodInvocationRewrite}, there's no MethodMatcher gate
     * and no KotlinTemplate substitution — the user's lambda body runs as Kotlin
     * for every method invocation in the tree, and the author has full
     * imperative control over what (if anything) to change. This is the entry
     * point for phase-mode recipes; pattern-mode is the declarative shortcut.
     *
     * <p>Also the lowering target for {@code edit(scanRef) { visitMethodInvocation
     * { call -> ... } }}: the IR pass rewrites {@code acc} references inside the
     * body so they read from the enclosing {@code getVisitor(acc)} method's
     * parameter, which the Kotlin lambda captures as a closure variable. From
     * this helper's POV the lambda is identical to the stateless case.
     */
    public static TreeVisitor<?, ExecutionContext> methodInvocationEditVisitor(
            Function1<J.MethodInvocation, J.MethodInvocation> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation transformed = body.invoke(method);
                return super.visitMethodInvocation(transformed, ctx);
            }
        };
    }

    /**
     * Visitor for a phase-mode recipe's scan phase. The Kotlin lambda receives
     * each method invocation in turn and is expected to mutate the recipe's
     * accumulator (captured from the enclosing {@code getScanner(acc)} method's
     * parameter). The tree is never transformed during scanning — the framework
     * discards any structural changes a scanner visitor produces — so the lambda
     * returns {@code Unit} and the helper always defers to {@code super}.
     */
    public static TreeVisitor<?, ExecutionContext> methodInvocationScanVisitor(
            Function1<J.MethodInvocation, kotlin.Unit> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                body.invoke(method);
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }

    /*
     * The remaining helpers below cover the other recognised visitor primitives.
     * Each pair (edit, scan) follows the same shape as methodInvocation*:
     *  - edit: invoke the user's lambda on the node, hand the (possibly new)
     *    node to super so children get visited.
     *  - scan: invoke the lambda for its side effect on the accumulator, leave
     *    the tree unchanged.
     *
     * They're spelled out one at a time rather than factored via reflection so
     * the generated bytecode stays type-stable: KotlinVisitor's `visitX` methods
     * are virtually dispatched, and reflective overrides would lose the
     * compiler's ability to see them as concrete entry points.
     */

    public static TreeVisitor<?, ExecutionContext> classDeclarationEditVisitor(
            Function1<J.ClassDeclaration, J.ClassDeclaration> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration transformed = body.invoke(classDecl);
                return super.visitClassDeclaration(transformed, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> classDeclarationScanVisitor(
            Function1<J.ClassDeclaration, kotlin.Unit> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                body.invoke(classDecl);
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> methodDeclarationEditVisitor(
            Function1<J.MethodDeclaration, J.MethodDeclaration> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration transformed = body.invoke(method);
                return super.visitMethodDeclaration(transformed, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> methodDeclarationScanVisitor(
            Function1<J.MethodDeclaration, kotlin.Unit> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                body.invoke(method);
                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> variableDeclarationsEditVisitor(
            Function1<J.VariableDeclarations, J.VariableDeclarations> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations transformed = body.invoke(multiVariable);
                return super.visitVariableDeclarations(transformed, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> variableDeclarationsScanVisitor(
            Function1<J.VariableDeclarations, kotlin.Unit> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                body.invoke(multiVariable);
                return super.visitVariableDeclarations(multiVariable, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> importEditVisitor(
            Function1<J.Import, J.Import> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitImport(J.Import _import, ExecutionContext ctx) {
                J.Import transformed = body.invoke(_import);
                return super.visitImport(transformed, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> importScanVisitor(
            Function1<J.Import, kotlin.Unit> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitImport(J.Import _import, ExecutionContext ctx) {
                body.invoke(_import);
                return super.visitImport(_import, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> propertyEditVisitor(
            Function1<K.Property, K.Property> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitProperty(K.Property property, ExecutionContext ctx) {
                K.Property transformed = body.invoke(property);
                return super.visitProperty(transformed, ctx);
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> propertyScanVisitor(
            Function1<K.Property, kotlin.Unit> body) {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitProperty(K.Property property, ExecutionContext ctx) {
                body.invoke(property);
                return super.visitProperty(property, ctx);
            }
        };
    }

    private static int[] parseCsv(String csv) {
        if (csv.isEmpty()) {
            return new int[0];
        }
        String[] parts = csv.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }
}
