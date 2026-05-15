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
