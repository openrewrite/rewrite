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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
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
import org.openrewrite.kotlin.tree.K;

/**
 * Runtime support for the recipe authoring DSL's K2 compiler plugin
 * (see {@link org.openrewrite.kotlin.recipe.internal.RecipeIrGenerationExtension}).
 * The plugin generates {@link org.openrewrite.Recipe} subclasses whose
 * {@code getVisitor()} body is a single call into one of these factories,
 * keeping the IR codegen small and the matching/replacing logic
 * Java-readable.
 *
 * <p>{@code methodInvocationRewrite} (Kotlin) and {@code methodInvocationRewriteJava}
 * are parallel paths selected by the LST-structural classifier in the IR
 * extension: the default is {@code methodInvocationRewriteJava} (a
 * {@link JavaVisitor} walks both Java AND Kotlin sources via
 * {@link org.openrewrite.kotlin.TreeVisitorAdapter}); the Kotlin variant kicks
 * in only when the before/after templates structurally reference a {@code K.*}
 * LST node — see {@code RecipeIrLanguageDescriptors.isKotlinSpecificTreeNode}.
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
        // Chain mode: a single matcher spec line containing a tab encodes a
        // two-segment chain <outerSpec>\t<innerSpec>. Substitution-source CSV
        // is segment-tagged ("o:N" / "i:N"). v1 supports single-before chains
        // only; multi-before chains are rejected at IR-pass time.
        if (matcherSpecsLines.indexOf('\t') >= 0) {
            return chainMethodInvocationRewrite(matcherSpecsLines, afterTemplate, substitutionSourcesCsv);
        }
        String[] specs = matcherSpecsLines.isEmpty() ? new String[0] : matcherSpecsLines.split("\n");
        MethodMatcher[] matchers = new MethodMatcher[specs.length];
        for (int i = 0; i < specs.length; i++) {
            matchers[i] = new MethodMatcher(specs[i]);
        }
        int[][] substitutionSourcesByMatcher = parseCsvPerMatcher(substitutionSourcesCsv, specs.length);
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                int matchedIdx = -1;
                for (int i = 0; i < matchers.length; i++) {
                    if (matchers[i].matches(method)) {
                        matchedIdx = i;
                        break;
                    }
                }
                if (matchedIdx >= 0) {
                    int[] substitutionSources = substitutionSourcesByMatcher[matchedIdx];
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
     * Chain-mode body for {@link #methodInvocationRewrite}. The before lambda
     * was a two-segment chained call (e.g. {@code xs.filter(p).first()}); the
     * matcher spec is {@code <outerSpec>\t<innerSpec>} and the substitution-
     * source CSV uses segment-tagged entries ({@code o:N} for outer-call args
     * with {@code o:-1} for the outer receiver, {@code i:N} / {@code i:-1} for
     * the inner segment).
     *
     * <p>Walks {@link J.MethodInvocation}; matches the outer spec, then
     * checks that {@code method.getSelect()} is itself a {@link J.MethodInvocation}
     * matching the inner spec. If both match, extracts substitutions per the
     * tagged CSV and applies the after template.
     */
    private static TreeVisitor<?, ExecutionContext> chainMethodInvocationRewrite(
            String matcherSpecsLine, String afterTemplate, String substitutionSourcesCsv) {
        int tabIdx = matcherSpecsLine.indexOf('\t');
        MethodMatcher outerMatcher = new MethodMatcher(matcherSpecsLine.substring(0, tabIdx));
        MethodMatcher innerMatcher = new MethodMatcher(matcherSpecsLine.substring(tabIdx + 1));
        ChainSourceRef[] sources = parseChainCsv(substitutionSourcesCsv);
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!outerMatcher.matches(method)) {
                    return super.visitMethodInvocation(method, ctx);
                }
                if (!(method.getSelect() instanceof J.MethodInvocation)) {
                    return super.visitMethodInvocation(method, ctx);
                }
                J.MethodInvocation inner = (J.MethodInvocation) method.getSelect();
                if (!innerMatcher.matches(inner)) {
                    return super.visitMethodInvocation(method, ctx);
                }
                Object[] substitutions = new Object[sources.length];
                for (int i = 0; i < sources.length; i++) {
                    ChainSourceRef ref = sources[i];
                    J.MethodInvocation target = ref.outer ? method : inner;
                    if (ref.pos < 0) {
                        if (target.getSelect() == null) {
                            return super.visitMethodInvocation(method, ctx);
                        }
                        substitutions[i] = target.getSelect();
                    } else {
                        if (ref.pos >= target.getArguments().size()) {
                            return super.visitMethodInvocation(method, ctx);
                        }
                        substitutions[i] = target.getArguments().get(ref.pos);
                    }
                }
                J result = KotlinTemplate.builder(afterTemplate).build()
                        .apply(getCursor(), method.getCoordinates().replace(), substitutions);
                if (result instanceof J.MethodInvocation) {
                    JContainer<Expression> matchedTypeArgs = method.getPadding().getTypeParameters();
                    if (matchedTypeArgs != null) {
                        result = ((J.MethodInvocation) result).withTypeParameters(matchedTypeArgs);
                    }
                    result = preserveTrailingLambdaShape((J.MethodInvocation) result);
                }
                return result.withPrefix(method.getPrefix());
            }
        };
    }

    private static class ChainSourceRef {
        final boolean outer;
        final int pos;
        ChainSourceRef(boolean outer, int pos) { this.outer = outer; this.pos = pos; }
    }

    private static ChainSourceRef[] parseChainCsv(String csv) {
        if (csv.isEmpty()) {
            return new ChainSourceRef[0];
        }
        String[] parts = csv.split(",");
        ChainSourceRef[] result = new ChainSourceRef[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            int colonIdx = p.indexOf(':');
            if (colonIdx < 0) {
                throw new IllegalStateException("Chain CSV entry missing segment tag: " + p);
            }
            char tag = p.charAt(0);
            int pos = Integer.parseInt(p.substring(colonIdx + 1));
            result[i] = new ChainSourceRef(tag == 'o', pos);
        }
        return result;
    }

    /**
     * Variant of {@link #methodInvocationRewrite} for recipes whose before
     * lambda body is {@code someCall()!!} (a method invocation immediately
     * not-null-asserted). In the rewrite-kotlin LST that pattern parses as
     * {@code K.Unary(NotNull, J.MethodInvocation)} — visiting only
     * {@code J.MethodInvocation} would replace just the call and leave a
     * stranded {@code !!} on the rewritten template, so this visitor walks
     * {@link K.Unary} instead and swaps the whole not-null expression for the
     * after template.
     *
     * <p>Matcher specs, substitution-source CSV, and trailing-lambda fix-up
     * semantics are identical to the bare variant — only the entry point and
     * the replacement coordinate (the K.Unary, not its inner J.MethodInvocation)
     * change.
     */
    public static TreeVisitor<?, ExecutionContext> methodInvocationRewriteKotlinNotNull(
            String matcherSpecsLines, String afterTemplate, String substitutionSourcesCsv) {
        String[] specs = matcherSpecsLines.isEmpty() ? new String[0] : matcherSpecsLines.split("\n");
        MethodMatcher[] matchers = new MethodMatcher[specs.length];
        for (int i = 0; i < specs.length; i++) {
            matchers[i] = new MethodMatcher(specs[i]);
        }
        int[][] substitutionSourcesByMatcher = parseCsvPerMatcher(substitutionSourcesCsv, specs.length);
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitUnary(K.Unary unary, ExecutionContext ctx) {
                if (unary.getOperator() != K.Unary.Type.NotNull) {
                    return super.visitUnary(unary, ctx);
                }
                if (!(unary.getExpression() instanceof J.MethodInvocation)) {
                    return super.visitUnary(unary, ctx);
                }
                J.MethodInvocation method = (J.MethodInvocation) unary.getExpression();
                int matchedIdx = -1;
                for (int i = 0; i < matchers.length; i++) {
                    if (matchers[i].matches(method)) {
                        matchedIdx = i;
                        break;
                    }
                }
                if (matchedIdx < 0) {
                    return super.visitUnary(unary, ctx);
                }
                int[] substitutionSources = substitutionSourcesByMatcher[matchedIdx];
                Object[] substitutions = new Object[substitutionSources.length];
                for (int i = 0; i < substitutionSources.length; i++) {
                    int src = substitutionSources[i];
                    if (src < 0) {
                        if (method.getSelect() == null) {
                            return super.visitUnary(unary, ctx);
                        }
                        substitutions[i] = method.getSelect();
                    } else {
                        if (src >= method.getArguments().size()) {
                            return super.visitUnary(unary, ctx);
                        }
                        substitutions[i] = method.getArguments().get(src);
                    }
                }
                J result = KotlinTemplate.builder(afterTemplate).build()
                        .apply(getCursor(), unary.getCoordinates().replace(), substitutions);
                if (result instanceof J.MethodInvocation) {
                    JContainer<Expression> matchedTypeArgs = method.getPadding().getTypeParameters();
                    if (matchedTypeArgs != null) {
                        result = ((J.MethodInvocation) result).withTypeParameters(matchedTypeArgs);
                    }
                    result = preserveTrailingLambdaShape((J.MethodInvocation) result);
                }
                return result.withPrefix(unary.getPrefix());
            }
        };
    }

    /**
     * Visitor for a {@code rewrite { d: Duration -> d.inHours } to { d -> d.inWholeHours }}
     * recipe whose before lambda body is a property access (rather than a
     * method invocation). The Kotlin parser models {@code obj.prop} as a
     * {@link J.FieldAccess} — so this visitor walks {@link J.FieldAccess}
     * instead of {@link J.MethodInvocation}.
     *
     * <p>Matcher specs use the {@code <owner-fqn>#<property-name>} format
     * (rather than the {@link MethodMatcher} spelling), since property access
     * has no parenthesised parameter list. Each line of
     * {@code matcherSpecsLines} is one such spec; the visitor accepts a
     * field access iff its target type FQN and selector name match any spec.
     *
     * <p>Property accessors have no value arguments, so the substitution
     * source CSV always references the receiver via {@code -1} — the IR pass
     * still emits the receiver placeholder uniformly with the method-invocation
     * path, which keeps the after-template synthesis logic shared.
     */
    public static TreeVisitor<?, ExecutionContext> propertyAccessRewrite(
            String matcherSpecsLines, String afterTemplate, String substitutionSourcesCsv) {
        String[] specs = matcherSpecsLines.isEmpty() ? new String[0] : matcherSpecsLines.split("\n");
        String[] specOwners = new String[specs.length];
        String[] specNames = new String[specs.length];
        for (int i = 0; i < specs.length; i++) {
            int hash = specs[i].indexOf('#');
            if (hash < 0) {
                specOwners[i] = "*";
                specNames[i] = specs[i];
            } else {
                specOwners[i] = specs[i].substring(0, hash);
                specNames[i] = specs[i].substring(hash + 1);
            }
        }
        int[][] substitutionSourcesByMatcher = parseCsvPerMatcher(substitutionSourcesCsv, specs.length);
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                String name = fieldAccess.getName().getSimpleName();
                String targetTypeFqn = fullyQualifiedNameOf(fieldAccess.getTarget().getType());
                int matchedIdx = -1;
                for (int i = 0; i < specs.length; i++) {
                    if (!specNames[i].equals(name)) {
                        continue;
                    }
                    if (!"*".equals(specOwners[i]) && !specOwners[i].equals(targetTypeFqn)) {
                        continue;
                    }
                    matchedIdx = i;
                    break;
                }
                if (matchedIdx < 0) {
                    return super.visitFieldAccess(fieldAccess, ctx);
                }
                int[] substitutionSources = substitutionSourcesByMatcher[matchedIdx];
                Object[] substitutions = new Object[substitutionSources.length];
                for (int i = 0; i < substitutionSources.length; i++) {
                    int src = substitutionSources[i];
                    if (src < 0) {
                        substitutions[i] = fieldAccess.getTarget();
                    } else {
                        // Property accessors have no positional args — the IR
                        // pass should never have emitted a non-negative source
                        // for this path. Bail out rather than substituting a
                        // placeholder we can't satisfy.
                        return super.visitFieldAccess(fieldAccess, ctx);
                    }
                }
                J result = KotlinTemplate.builder(afterTemplate).build()
                        .apply(getCursor(), fieldAccess.getCoordinates().replace(), substitutions);
                return result.withPrefix(fieldAccess.getPrefix());
            }
        };
    }

    private static @Nullable String fullyQualifiedNameOf(org.openrewrite.java.tree.@Nullable JavaType type) {
        if (type instanceof org.openrewrite.java.tree.JavaType.FullyQualified) {
            return ((org.openrewrite.java.tree.JavaType.FullyQualified) type).getFullyQualifiedName();
        }
        return null;
    }

    /**
     * Java-rooted parallel of {@link #methodInvocationRewrite}. Used when the
     * LST-structural classifier defaults a {@code rewrite { } to { }} clause
     * to a {@link JavaVisitor}: the visitor walks both Java and Kotlin sources
     * via {@link org.openrewrite.kotlin.TreeVisitorAdapter}, but the template
     * is parsed via {@link JavaTemplate}.
     *
     * <p>Matching, substitution-source semantics, and the multi-spec {@code \n}
     * delimiter are identical to the Kotlin variant — only the visitor type
     * and the template engine differ. There is no trailing-lambda fix-up:
     * Java has no trailing-lambda syntax, so the {@link OmitParentheses}
     * dance is Kotlin-specific.
     */
    public static TreeVisitor<?, ExecutionContext> methodInvocationRewriteJava(
            String matcherSpecsLines, String afterTemplate, String substitutionSourcesCsv) {
        String[] specs = matcherSpecsLines.isEmpty() ? new String[0] : matcherSpecsLines.split("\n");
        MethodMatcher[] matchers = new MethodMatcher[specs.length];
        for (int i = 0; i < specs.length; i++) {
            matchers[i] = new MethodMatcher(specs[i]);
        }
        int[][] substitutionSourcesByMatcher = parseCsvPerMatcher(substitutionSourcesCsv, specs.length);
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                int matchedIdx = -1;
                for (int i = 0; i < matchers.length; i++) {
                    if (matchers[i].matches(method)) {
                        matchedIdx = i;
                        break;
                    }
                }
                if (matchedIdx >= 0) {
                    int[] substitutionSources = substitutionSourcesByMatcher[matchedIdx];
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
                    J result = JavaTemplate.builder(afterTemplate).build()
                            .apply(getCursor(), method.getCoordinates().replace(), substitutions);
                    if (result instanceof J.MethodInvocation) {
                        JContainer<Expression> matchedTypeArgs = method.getPadding().getTypeParameters();
                        if (matchedTypeArgs != null) {
                            result = ((J.MethodInvocation) result).withTypeParameters(matchedTypeArgs);
                        }
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
     * Parse a substitution-source CSV that may be a single shared list (no
     * {@code \n}) or one CSV per matcher ({@code \n}-delimited, one entry per
     * matcher spec). Returns an {@code int[][]} of length {@code matcherCount}
     * — the same array repeated for the shared case, or a per-matcher array
     * for the mixed-shape multi-before case.
     *
     * <p>Mixed-shape multi-before: a recipe like
     * {@code rewrite({ s: String -> s.foo() }, { s: String -> bar(s) }) to { s -> s.qux() }}
     * binds {@code s} to the receiver in the first matcher and to arg 0 in
     * the second. The after template's placeholder ORDER is shared (since
     * the template is derived from the same after lambda), but the substitution
     * sources differ per-matcher.
     */
    private static int[][] parseCsvPerMatcher(String csv, int matcherCount) {
        if (matcherCount == 0) {
            return new int[0][];
        }
        if (csv.indexOf('\n') < 0) {
            int[] shared = parseCsv(csv);
            int[][] result = new int[matcherCount][];
            for (int i = 0; i < matcherCount; i++) {
                result[i] = shared;
            }
            return result;
        }
        String[] lines = csv.split("\n", -1);
        if (lines.length != matcherCount) {
            throw new IllegalStateException(
                    "expected " + matcherCount + " per-matcher CSVs but got " + lines.length);
        }
        int[][] result = new int[matcherCount][];
        for (int i = 0; i < matcherCount; i++) {
            result[i] = parseCsv(lines[i]);
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
