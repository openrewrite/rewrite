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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.TrailingLambdaArgument;

/**
 * Runtime support for the recipe authoring DSL's K2 compiler plugin
 * (see {@link org.openrewrite.kotlin.recipe.internal.RecipeIrGenerationExtension}).
 * The plugin generates {@link org.openrewrite.Recipe} subclasses whose
 * {@code getVisitor()} body is a single call into one of these factories,
 * keeping the IR codegen small and the matching/replacing logic
 * Java-readable.
 *
 * <p>Phase 3 of the DSL rewrite will add a {@code methodInvocationRewriteJava}
 * parallel of the Kotlin path and {@code composeSequential(vararg visitors)}
 * for multi-statement edit/scan/generate bodies. Until then this class carries
 * only the canonical KotlinVisitor-rooted {@code rewrite { } to { }} helper.
 */
public final class GeneratedRecipeSupport {

    private GeneratedRecipeSupport() {}

    /**
     * Visitor for a {@code rewrite { ... } to { ... }} recipe whose before
     * lambda body is a method invocation rooted at the receiver param. Matching
     * is delegated to {@link MethodMatcher}; the after template is a
     * {@link KotlinTemplate} string with one {@code #{any()}} placeholder per
     * substitution slot.
     *
     * {@code matcherSpecsLines} is a {@code \n}-delimited list of
     * MethodMatcher specs — multi-before recipes
     * ({@code rewrite(b1, b2, ...) to a}) yield one spec per before lambda
     * and we accept a method invocation when any spec matches.
     *
     * The {@code substitutionSourcesCsv} encodes, in template-left-to-right
     * order, how to fill each placeholder from the matched method invocation:
     * {@code -1} for {@code method.getSelect()} (the receiver), {@code N >= 0}
     * for {@code method.getArguments().get(N)}. The CSV is shared across all
     * matchers because the IR pass requires every before lambda to agree on
     * the captured argument shape (same param-index references and same
     * literal {@code (kind, value)} at each position).
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
                    J result = KotlinTemplate.builder(afterTemplate).build()
                            .apply(getCursor(), method.getCoordinates().replace(), substitutions);
                    // Preserve the matched call's reified type arguments. The
                    // after-template's type args (if any) are placeholders the
                    // author wrote because Kotlin won't let `enumValues<T>()` /
                    // `enumEntries<T>()` etc. parse without a concrete type
                    // there. The matched call carries the real type — copy it
                    // over so the rewrite is type-arg-faithful for reified
                    // callees. No-op when the result isn't a method invocation
                    // or the matched call had no type args.
                    if (result instanceof J.MethodInvocation) {
                        JContainer<Expression> matchedTypeArgs = method.getPadding().getTypeParameters();
                        if (matchedTypeArgs != null) {
                            result = ((J.MethodInvocation) result).withTypeParameters(matchedTypeArgs);
                        }
                        result = preserveTrailingLambdaShape((J.MethodInvocation) result);
                    }
                    return result.withPrefix(method.getPrefix());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }

    /**
     * Compose visitors sequentially: each runs in declaration order on the
     * result of the previous one. Backs multi-statement {@code edit/scan/generate}
     * bodies in {@code org.openrewrite.RecipeDsl}.
     *
     * <p>Semantics (per plan §7):
     * <ul>
     *   <li>{@code isAcceptable(SourceFile, ctx)} returns true iff ANY inner
     *       visitor accepts the source. Non-accepting inner visitors are skipped
     *       for that source.</li>
     *   <li>{@code visit(tree, ctx)} threads {@code tree} through each visitor in
     *       declaration order, restarting cursor state between visitors. Cursor
     *       messages set by one inner visitor are NOT visible to the next.
     *       Returning the same instance does NOT short-circuit — the next visitor
     *       still runs.</li>
     *   <li>Exceptions propagate; there's no partial-state recovery.</li>
     * </ul>
     */
    public static TreeVisitor<?, ExecutionContext> composeSequential(TreeVisitor<?, ExecutionContext>[] visitors) {
        if (visitors.length == 0) {
            return TreeVisitor.noop();
        }
        if (visitors.length == 1) {
            return visitors[0];
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                for (TreeVisitor<?, ExecutionContext> v : visitors) {
                    if (v.isAcceptable(sourceFile, ctx)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree current = tree;
                for (TreeVisitor<?, ExecutionContext> v : visitors) {
                    if (current instanceof SourceFile && !v.isAcceptable((SourceFile) current, ctx)) {
                        continue;
                    }
                    Tree next = v.visit(current, ctx, new Cursor(null, Cursor.ROOT_VALUE));
                    if (next != null) {
                        current = next;
                    }
                }
                return current;
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

    /**
     * Restore Kotlin trailing-lambda call shape on a template-substituted result.
     *
     * <p>The substitution sources come straight off the matched invocation's
     * argument list. When the matched call site used trailing-lambda syntax
     * ({@code xs.foo { ... }}), the captured {@link J.Lambda} carries a
     * {@link TrailingLambdaArgument} marker — that marker tells
     * {@code KotlinPrinter#visitArgumentsContainer} to emit {@code )} <em>before</em>
     * the lambda. After the substitution lands inside a parenthesised template
     * call ({@code xs.bar(#{any()})}), the marker still fires but the template's
     * container has no {@link OmitParentheses} marker, so the printer emits
     * {@code xs.bar(){ ... }} (an empty pair of parens followed by the lambda).
     *
     * <p>Re-attach {@link OmitParentheses} to the args container when the last
     * arg is a {@code TrailingLambdaArgument}-marked lambda. The output is the
     * idiomatic {@code xs.bar { ... }}.
     */
    private static J.MethodInvocation preserveTrailingLambdaShape(J.MethodInvocation method) {
        JContainer<Expression> args = method.getPadding().getArguments();
        List<JRightPadded<Expression>> padded = args.getPadding().getElements();
        if (padded.isEmpty()) {
            return method;
        }
        JRightPadded<Expression> lastPadded = padded.get(padded.size() - 1);
        Expression last = lastPadded.getElement();
        if (!(last instanceof J.Lambda) ||
            !last.getMarkers().findFirst(TrailingLambdaArgument.class).isPresent() ||
            args.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
            return method;
        }
        // The template parsed without trailing-lambda syntax, so the lambda's
        // own prefix is "" (it sat flush against the placeholder). Once the
        // parens are gone, Kotlin convention is one space between the method
        // name and `{`.
        Expression spaced = last.getPrefix().getWhitespace().isEmpty()
                ? last.withPrefix(Space.SINGLE_SPACE)
                : last;
        List<JRightPadded<Expression>> rebuilt = new ArrayList<>(padded);
        rebuilt.set(rebuilt.size() - 1, lastPadded.withElement(spaced));
        JContainer<Expression> reshaped = args.getPadding()
                .withElements(rebuilt)
                .withMarkers(args.getMarkers().addIfAbsent(new OmitParentheses(Tree.randomId())));
        return method.getPadding().withArguments(reshaped);
    }
}
