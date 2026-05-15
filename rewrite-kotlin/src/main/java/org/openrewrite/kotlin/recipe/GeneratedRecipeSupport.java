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
     * Visitor for the simplest pattern-mode recipe shape:
     * a single {@code rewrite { p -> p.foo(...) } to { p -> p.bar(...) }} clause whose
     * {@code before} lambda body is a method invocation rooted at the parameter.
     *
     * Matching uses {@link MethodMatcher} with a spec derived from the before
     * lambda at compile time (currently zero-arg shape only: {@code "<paramTypeFqn> <methodName>()"}).
     * Replacement runs {@link KotlinTemplate#builder(String)}.apply() with
     * {@code method.getSelect()} as the single substitution; the after template
     * is a {@code #{any()}.someMethod(...)} string built by the IR pass.
     * Original prefix (leading whitespace/comments) is preserved.
     *
     * Switching from {@code KotlinTemplate.matches} to {@link MethodMatcher} is
     * intentional for v0: {@code KotlinTemplate.matches("#{any()}.foo()", cursor)}
     * does not currently match receiver-placeholder patterns against a
     * concrete invocation — verified by a standalone probe test 2026-05-14.
     */
    public static TreeVisitor<?, ExecutionContext> methodInvocationReceiverRewrite(
            String matcherSpec, String afterTemplate) {
        MethodMatcher matcher = new MethodMatcher(matcherSpec);
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (matcher.matches(method) && method.getSelect() != null) {
                    return KotlinTemplate.builder(afterTemplate).build()
                            .apply(getCursor(), method.getCoordinates().replace(), method.getSelect())
                            .withPrefix(method.getPrefix());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
