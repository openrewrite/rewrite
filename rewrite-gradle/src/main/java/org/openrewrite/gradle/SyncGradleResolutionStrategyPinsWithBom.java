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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.LatestIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class SyncGradleResolutionStrategyPinsWithBom extends Recipe {

    private static final String METHOD_BECAUSE = "because";
    private static final String METHOD_BUILDSCRIPT = "buildscript";
    private static final String METHOD_EACH_DEPENDENCY = "eachDependency";
    private static final String METHOD_RESOLUTION_STRATEGY = "resolutionStrategy";
    private static final String METHOD_USE_VERSION = "useVersion";

    @Override
    public String getDisplayName() {
        return "Sync Gradle `resolutionStrategy` pins with managed versions";
    }

    @Override
    public String getDescription() {
        return "In `configurations.all { resolutionStrategy.eachDependency { ... } }`, rewrite each " +
               "`details.useVersion '…'` literal to the version a platform or BOM would otherwise " +
               "resolve to, when the pinned version is strictly lower. Skips branches with a " +
               "`because(...)` clause (those are the responsibility of `RemoveRedundantSecurityResolutionRules`). " +
               "When a branch pins multiple artifacts via `name in [...]` and the managed versions " +
               "diverge across them, the branch is split into one `else if` per distinct target version.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Nullable GradleProject gradleProject;
            boolean insideBuildscript;

            private void maybeInit() {
                if (gradleProject == null) {
                    JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sf != null) {
                        gradleProject = sf.getMarkers().findFirst(GradleProject.class).orElse(null);
                    }
                }
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                maybeInit();

                boolean enteredBuildscript = METHOD_BUILDSCRIPT.equals(method.getSimpleName());
                if (enteredBuildscript) {
                    insideBuildscript = true;
                }
                boolean currentlyInBuildscript = insideBuildscript;

                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (enteredBuildscript) {
                    insideBuildscript = false;
                }

                if (!isEachDependency(m, getCursor()) || m.getArguments().isEmpty()) {
                    return m;
                }

                Expression maybeClosure = m.getArguments().get(0);
                if (!(maybeClosure instanceof J.Lambda) || !(((J.Lambda) maybeClosure).getBody() instanceof J.Block)) {
                    return m;
                }
                J.Lambda closure = (J.Lambda) maybeClosure;
                J.Block body = (J.Block) closure.getBody();

                List<Statement> newStatements = processStatements(body.getStatements(), currentlyInBuildscript, ctx);
                if (newStatements == body.getStatements()) {
                    return m;
                }
                return m.withArguments(singletonList(closure.withBody(body.withStatements(newStatements))));
            }

            private List<Statement> processStatements(List<Statement> statements, boolean inBuildscript, ExecutionContext ctx) {
                return ListUtils.map(statements, s -> {
                    if (s instanceof J.If) {
                        return processIf((J.If) s, inBuildscript, ctx);
                    }
                    return s;
                });
            }

            // Returns a single J.If (possibly a longer chain) with any splits incorporated as else-if branches.
            // Any pre-existing dangling `else`/`else if` is preserved on the trailing branch.
            private J.If processIf(J.If ifStatement, boolean inBuildscript, ExecutionContext ctx) {
                // Recurse into any else-if first so cascading transforms are preserved.
                J.If current = ifStatement;
                if (current.getElsePart() != null && current.getElsePart().getBody() instanceof J.If) {
                    J.If replacedElse = processIf((J.If) current.getElsePart().getBody(), inBuildscript, ctx);
                    current = current.withElsePart(current.getElsePart().withBody(replacedElse));
                }

                PredicateInfo pred = extractPredicate(current.getIfCondition().getTree());
                if (pred == null) {
                    return current;
                }
                ThenInfo then = extractThen(current.getThenPart());
                if (then == null || then.hasBecause) {
                    return current;
                }
                if (gradleProject == null) {
                    return current;
                }

                // For each artifact, find the version that would resolve without the pin constraint.
                // Target = max(pinnedVersion, resolvedWithoutPin). If no entry resolves, target = pinnedVersion.
                LatestIntegration semver = new LatestIntegration(null);
                Map<String, List<String>> artifactsByTarget = new LinkedHashMap<>();
                for (String artifactId : pred.artifactIds) {
                    String resolved = findResolvedVersionWithoutConstraint(
                            new GroupArtifact(pred.groupId, artifactId), inBuildscript, ctx);
                    String target = (resolved != null && semver.compare(null, resolved, then.version) > 0)
                            ? resolved
                            : then.version;
                    artifactsByTarget.computeIfAbsent(target, k -> new ArrayList<>()).add(artifactId);
                }

                // No divergence AND nothing to upgrade → leave untouched.
                if (artifactsByTarget.size() == 1 && artifactsByTarget.keySet().iterator().next().equals(then.version)) {
                    return current;
                }

                // Single target that differs from the current pin → rewrite the literal in place.
                if (artifactsByTarget.size() == 1) {
                    String newVersion = artifactsByTarget.keySet().iterator().next();
                    return rewriteUseVersionLiteral(current, newVersion);
                }

                // Divergent targets → split into N sibling branches (Option A: flat else-if with duplicated group check).
                return splitBranch(current, pred, artifactsByTarget);
            }

            private J.If rewriteUseVersionLiteral(J.If ifStatement, String newVersion) {
                // Scope the rewrite to the then-block only; else/else-if branches are processed separately
                // and shouldn't share the same target.
                Statement originalThen = ifStatement.getThenPart();
                Statement newThen = (Statement) new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, p);
                        if (!METHOD_USE_VERSION.equals(m.getSimpleName()) || m.getArguments().isEmpty()) {
                            return m;
                        }
                        Expression arg = m.getArguments().get(0);
                        if (!(arg instanceof J.Literal) || !(((J.Literal) arg).getValue() instanceof String)) {
                            return m;
                        }
                        J.Literal lit = (J.Literal) arg;
                        String oldSource = lit.getValueSource();
                        String newSource = oldSource == null
                                ? "'" + newVersion + "'"
                                : oldSource.replace((String) lit.getValue(), newVersion);
                        return m.withArguments(singletonList(lit.withValue(newVersion).withValueSource(newSource)));
                    }
                }.visit(originalThen, 0);
                return newThen == originalThen ? ifStatement : ifStatement.withThenPart(newThen);
            }

            // Produce a flat `if ... else if ... else if ...` chain (as a single J.If with nested else-ifs),
            // one branch per distinct target version. Single-artifact groups emit `name == 'x'`;
            // multi-artifact groups emit `name in [...]` by filtering the original list literal to those entries
            // (which carries all original padding over). The original dangling else part is re-attached to the
            // LAST generated branch.
            private J.If splitBranch(J.If original, PredicateInfo pred, Map<String, List<String>> artifactsByTarget) {
                J.If.Else originalElse = original.getElsePart();
                List<Map.Entry<String, List<String>>> entries = new ArrayList<>(artifactsByTarget.entrySet());

                // Build deepest else-if first and chain upward.
                J.If chain = null;
                for (int i = entries.size() - 1; i >= 0; i--) {
                    Map.Entry<String, List<String>> entry = entries.get(i);
                    J.If branch = buildGroupBranch(original, pred, entry.getValue(), entry.getKey());
                    if (chain == null) {
                        chain = (originalElse == null) ? branch : branch.withElsePart(originalElse);
                    } else {
                        // Chain goes into an else slot, so the inner if's prefix should be a single space
                        // (producing `else if`); otherwise it inherits the top-level newline+indent of the
                        // original branch and prints as `else\n    if`.
                        J.If chainAsElseIf = chain.withPrefix(Space.format(" "));
                        J.If.Else wrapperElse = new J.If.Else(
                                Tree.randomId(),
                                Space.format(" "),
                                Markers.EMPTY,
                                JRightPadded.build(chainAsElseIf));
                        chain = branch.withElsePart(wrapperElse);
                    }
                }
                return chain;
            }

            // Clone the original if-branch, rewriting the name-check portion of the predicate to match this group
            // (collapsing to `==` for size-1 groups, filtering the original list for larger groups) and the
            // useVersion literal to the given target version.
            private J.If buildGroupBranch(J.If original, PredicateInfo pred, List<String> artifactIds, String targetVersion) {
                J.If literalRewritten = rewriteUseVersionLiteral(original, targetVersion);
                J.ControlParentheses<Expression> cond = literalRewritten.getIfCondition();
                Expression newPredicate = replaceNameCheck(cond.getTree(), pred, artifactIds);
                return literalRewritten.withIfCondition(cond.withTree(newPredicate));
            }

            // Walk the top-level `group==g && <name-check>` predicate, locate the <name-check> sub-expression
            // (the same one PredicateInfo.nameCheckExpr points to by reference), and swap it for a filtered
            // version matching artifactIds. Reference-identity lookup lets us dispatch on any supported shape
            // (==, Groovy `in`, Kotlin `in`, OR chain) without re-recognizing it here.
            private Expression replaceNameCheck(Expression predicate, PredicateInfo pred, List<String> artifactIds) {
                return (Expression) new JavaIsoVisitor<Integer>() {
                    @Override
                    public J visit(@Nullable Tree tree, Integer p) {
                        if (tree == pred.nameCheckExpr) {
                            return buildNameCheck((Expression) tree, false, artifactIds);
                        }
                        return super.visit(tree, p);
                    }
                }.visit(predicate, 0);
            }

            // Build the replacement name-check in the same shape as the original, filtered to just the kept
            // artifacts. If only one artifact survives: always collapse to `<name-access> == '<v>'` for readability.
            // Dispatch by original shape so that lists stay as lists and OR chains stay as OR chains.
            // If the original was wrapped in parentheses, the replacement is re-wrapped in the same parens
            // (preserving prefix/padding, and preserving operator precedence for OR chains).
            @SuppressWarnings("unchecked")
            private Expression buildNameCheck(Expression original, boolean nameOnLeft, List<String> artifactIds) {
                J.Parentheses<Expression> wrappingParens = null;
                while (original instanceof J.Parentheses) {
                    wrappingParens = (J.Parentheses<Expression>) original;
                    original = (Expression) wrappingParens.getTree();
                }
                Expression inner = buildNameCheckInner(original, artifactIds);
                // Only re-wrap in parens when the output is itself an OR chain, where the parens are needed
                // for `&&`/`||` precedence. For `in [...]` or `==` outputs the parens are redundant noise.
                boolean needsParens = wrappingParens != null
                        && inner instanceof J.Binary
                        && ((J.Binary) inner).getOperator() == J.Binary.Type.Or;
                if (needsParens) {
                    return wrappingParens.getPadding().withTree(
                            wrappingParens.getPadding().getTree().withElement(inner));
                }
                // Collapsing to `==`: inherit the wrapping parens' prefix so the ` && ` space stays correct.
                if (wrappingParens != null) {
                    return inner.withPrefix(wrappingParens.getPrefix());
                }
                return inner;
            }

            private Expression buildNameCheckInner(Expression original, List<String> artifactIds) {
                if (artifactIds.size() == 1) {
                    return buildSingleEquals(original, artifactIds.get(0));
                }
                if (original instanceof G.Binary) {
                    G.Binary origIn = (G.Binary) original;
                    if (origIn.getOperator() == G.Binary.Type.In && origIn.getRight() instanceof G.ListLiteral) {
                        return filterGroovyListLiteral(origIn, (G.ListLiteral) origIn.getRight(), artifactIds);
                    }
                }
                if (original instanceof K.Binary) {
                    K.Binary origContains = (K.Binary) original;
                    if (origContains.getOperator() == K.Binary.Type.Contains
                            && origContains.getRight() instanceof J.MethodInvocation) {
                        return filterKotlinListOf(origContains, (J.MethodInvocation) origContains.getRight(), artifactIds);
                    }
                }
                if (original instanceof J.Binary && ((J.Binary) original).getOperator() == J.Binary.Type.Or) {
                    return filterOrChain((J.Binary) original, artifactIds);
                }
                throw new IllegalStateException("Unhandled name-check shape for multi-artifact split: " + original.getClass().getSimpleName());
            }

            // Collapse any shape to `<name-access> == '<v>'`. The quote style — single vs double — follows the
            // DSL: Groovy inputs use `'...'`, Kotlin inputs use `"..."`. We detect by looking at the first
            // String literal we can find in the original expression; this covers all input shapes we extract.
            private Expression buildSingleEquals(Expression original, String artifact) {
                String quote = quoteStyleOf(original);
                J.Literal lit = new J.Literal(
                        Tree.randomId(),
                        Space.format(" "),
                        Markers.EMPTY,
                        artifact, quote + artifact + quote, null,
                        JavaType.Primitive.String);
                Expression nameAccess = nameAccessOf(original);
                return new J.Binary(
                        Tree.randomId(),
                        original.getPrefix(),
                        Markers.EMPTY,
                        nameAccess,
                        JLeftPadded.build(J.Binary.Type.Equal).withBefore(Space.format(" ")),
                        lit,
                        null);
            }

            private Expression filterGroovyListLiteral(G.Binary origIn, G.ListLiteral origList, List<String> artifactIds) {
                List<Expression> filtered = new ArrayList<>();
                for (Expression el : origList.getElements()) {
                    if (el instanceof J.Literal
                            && ((J.Literal) el).getValue() instanceof String
                            && artifactIds.contains(((J.Literal) el).getValue())) {
                        filtered.add(el);
                    }
                }
                // If the kept element[0] is not the original element[0], it carries its original
                // "space after preceding comma" prefix (`" "`). Reset to EMPTY so the list prints as `[a, b]`
                // and not `[ a, b]`.
                if (!filtered.isEmpty() && filtered.get(0) != origList.getElements().get(0)) {
                    filtered.set(0, filtered.get(0).withPrefix(Space.EMPTY));
                }
                return origIn.withRight(origList.withElements(filtered));
            }

            private Expression filterKotlinListOf(K.Binary origContains, J.MethodInvocation origListOf, List<String> artifactIds) {
                List<Expression> filtered = new ArrayList<>();
                for (Expression arg : origListOf.getArguments()) {
                    if (arg instanceof J.Literal
                            && ((J.Literal) arg).getValue() instanceof String
                            && artifactIds.contains(((J.Literal) arg).getValue())) {
                        filtered.add(arg);
                    }
                }
                // Same leading-space fixup as for the Groovy list literal: method arguments carry their
                // post-comma space in `prefix`; the first kept argument has to have Space.EMPTY if we dropped
                // whatever argument preceded it.
                if (!filtered.isEmpty() && filtered.get(0) != origListOf.getArguments().get(0)) {
                    filtered.set(0, filtered.get(0).withPrefix(Space.EMPTY));
                }
                return origContains.withRight(origListOf.withArguments(filtered));
            }

            // Filter the OR chain to just the leaves whose literal is in artifactIds, preserving left-to-right order.
            // Rebuilds a left-associative Or chain: `((kept[0]) || kept[1]) || kept[2] ...`.
            private Expression filterOrChain(J.Binary origOrRoot, List<String> artifactIds) {
                List<J.Binary> allLeaves = new ArrayList<>();
                collectOrLeaves(origOrRoot, allLeaves);
                List<J.Binary> kept = new ArrayList<>();
                for (J.Binary leaf : allLeaves) {
                    String literal = nameEqualsLiteral(leaf);
                    if (literal != null && artifactIds.contains(literal)) {
                        kept.add(leaf);
                    }
                }
                if (kept.size() == 1) {
                    // Shouldn't happen (size==1 is handled by buildSingleEquals above) but defensively collapse.
                    return kept.get(0).withPrefix(origOrRoot.getPrefix());
                }
                // Rebuild left-associative chain. First leaf keeps the root's prefix; subsequent leaves keep
                // their own prefix (single space from `|| name == 'x'`).
                Expression chain = kept.get(0).withPrefix(origOrRoot.getPrefix());
                for (int i = 1; i < kept.size(); i++) {
                    chain = new J.Binary(
                            Tree.randomId(),
                            origOrRoot.getPrefix(),
                            Markers.EMPTY,
                            chain,
                            JLeftPadded.build(J.Binary.Type.Or).withBefore(Space.format(" ")),
                            kept.get(i).withPrefix(Space.format(" ")),
                            null);
                }
                return chain;
            }

            // Walks a J.Binary Or tree and appends every leaf (`name == 'x'`) J.Binary in left-to-right order.
            private void collectOrLeaves(J.Binary or, List<J.Binary> out) {
                for (Expression side : Arrays.asList(or.getLeft(), or.getRight())) {
                    if (side instanceof J.Binary) {
                        J.Binary b = (J.Binary) side;
                        if (b.getOperator() == J.Binary.Type.Or) {
                            collectOrLeaves(b, out);
                        } else if (b.getOperator() == J.Binary.Type.Equal) {
                            out.add(b);
                        }
                    }
                }
            }

            // Find the first String literal under this expression (used only for quote-style detection).
            // Returns "'" (Groovy convention) or "\"" (Kotlin convention), defaulting to Groovy.
            private String quoteStyleOf(Expression e) {
                String[] holder = new String[]{"'"};
                try {
                    new JavaIsoVisitor<Integer>() {
                        @Override
                        public J.Literal visitLiteral(J.Literal lit, Integer p) {
                            if (lit.getValue() instanceof String && lit.getValueSource() != null && lit.getValueSource().length() >= 2) {
                                holder[0] = lit.getValueSource().substring(0, 1);
                                throw new RuntimeException("found");
                            }
                            return lit;
                        }
                    }.visit(e, 0);
                } catch (RuntimeException ignored) {
                    // intentional short-circuit once we find any literal
                }
                return holder[0];
            }

            // Pull the `requested.name` field access out of the original name-check expression. Falls through
            // J.Binary (`==` / `||`), G.Binary (Groovy `in`), and K.Binary (Kotlin `in`) shapes. For an OR chain,
            // reaches into the first leaf's `==` to grab its name access.
            private Expression nameAccessOf(Expression original) {
                // Transparently unwrap any parentheses that might wrap the expression (e.g. `(a || b)`).
                while (original instanceof J.Parentheses) {
                    original = (Expression) ((J.Parentheses<?>) original).getTree();
                }
                if (original instanceof J.Binary) {
                    J.Binary b = (J.Binary) original;
                    if (b.getOperator() == J.Binary.Type.Or) {
                        // Recurse into the leftmost side until we find a non-Or binary.
                        return nameAccessOf(b.getLeft());
                    }
                    if ("name".equals(fieldOf(b.getLeft()))) return b.getLeft();
                    if ("name".equals(fieldOf(b.getRight()))) return b.getRight();
                }
                if (original instanceof G.Binary) {
                    G.Binary b = (G.Binary) original;
                    if ("name".equals(fieldOf(b.getLeft()))) return b.getLeft();
                }
                if (original instanceof K.Binary) {
                    K.Binary b = (K.Binary) original;
                    if ("name".equals(fieldOf(b.getLeft()))) return b.getLeft();
                }
                throw new IllegalStateException("Could not locate name field access on original predicate; shape=" + original.getClass().getSimpleName());
            }

            // Lookup the version a BOM/platform would resolve for this GA, with the resolution strategy constraint
            // removed. Mirrors RemoveRedundantSecurityResolutionRules#findResolvedVersion[WithoutConstraint].
            private @Nullable String findResolvedVersionWithoutConstraint(GroupArtifact ga, boolean inBuildscript, ExecutionContext ctx) {
                if (gradleProject == null) return null;
                if (inBuildscript && gradleProject.getBuildscript() != null) {
                    GradleBuildscript bs = gradleProject.getBuildscript();
                    for (GradleDependencyConfiguration config : bs.getConfigurations()) {
                        if (config.isCanBeResolved()) {
                            GradleDependencyConfiguration simulated = config.removeConstraints(
                                    singleton(ga), bs.getMavenRepositories(), ctx);
                            ResolvedDependency r = simulated.findResolvedDependency(ga.getGroupId(), ga.getArtifactId());
                            if (r != null) {
                                return r.getVersion();
                            }
                        }
                    }
                    return null;
                }
                GradleProject simulated = gradleProject.removeConstraints(singleton(ga), ctx);
                for (GradleDependencyConfiguration config : simulated.getConfigurations()) {
                    if (config.isCanBeResolved()) {
                        ResolvedDependency r = config.findResolvedDependency(ga.getGroupId(), ga.getArtifactId());
                        if (r != null) {
                            return r.getVersion();
                        }
                    }
                }
                return null;
            }
        });
    }

    // --- Static helpers for predicate / then-block extraction. Kept outside the inner visitor for readability. ---

    @Value
    static class PredicateInfo {
        String groupId;
        List<String> artifactIds;
        // The original expression that matched name(s). Used by buildNameCheck to decide the output shape
        // (name == 'x', name in [...], name in listOf(...), or `name == 'a' || name == 'b' || ...`).
        Expression nameCheckExpr;
    }

    @Value
    static class ThenInfo {
        String version;
        boolean hasBecause;
    }

    static @Nullable PredicateInfo extractPredicate(Expression predicate) {
        if (!(predicate instanceof J.Binary)) {
            return null;
        }
        J.Binary and = (J.Binary) predicate;
        if (and.getOperator() != J.Binary.Type.And) {
            return null;
        }
        String groupId = null;
        List<String> artifactIds = null;
        Expression nameCheckExpr = null;

        for (Expression side : Arrays.asList(and.getLeft(), and.getRight())) {
            String maybeGroup = extractGroupEquals(side);
            if (maybeGroup != null) {
                groupId = maybeGroup;
                continue;
            }
            List<String> names = extractNameMatch(side);
            if (names != null) {
                artifactIds = names;
                nameCheckExpr = side;
            }
        }

        if (groupId == null || artifactIds == null) {
            return null;
        }
        return new PredicateInfo(groupId, artifactIds, nameCheckExpr);
    }

    // `requested.group == 'g'` → "g". Else null.
    private static @Nullable String extractGroupEquals(Expression side) {
        if (!(side instanceof J.Binary)) {
            return null;
        }
        J.Binary b = (J.Binary) side;
        if (b.getOperator() != J.Binary.Type.Equal) {
            return null;
        }
        String field = fieldOf(b.getLeft());
        String literal = literalOf(b.getRight());
        if (field == null) {
            field = fieldOf(b.getRight());
            literal = literalOf(b.getLeft());
        }
        return "group".equals(field) && literal != null ? literal : null;
    }

    // Pulls the artifact IDs out of a name-check expression, in whatever shape the author used:
    //   `name == 'x'`                        → [x]
    //   `name in ['a', 'b']`                 → [a, b]   (Groovy G.Binary with In op + G.ListLiteral)
    //   `name in listOf("a", "b")`           → [a, b]   (Kotlin K.Binary with Contains op + listOf call)
    //   `name == 'a' || name == 'b' || ...`  → [a, b, …] (J.Binary Or chain of == on name)
    // Returns null if the expression doesn't fit one of these shapes.
    private static @Nullable List<String> extractNameMatch(Expression side) {
        // `(A || B)` and `((A || B))` come through as J.Parentheses; unwrap transparently.
        while (side instanceof J.Parentheses) {
            Expression inner = (Expression) ((J.Parentheses<?>) side).getTree();
            side = inner;
        }
        if (side instanceof J.Binary) {
            J.Binary b = (J.Binary) side;
            if (b.getOperator() == J.Binary.Type.Equal) {
                String literal = nameEqualsLiteral(b);
                return literal == null ? null : singletonList(literal);
            }
            if (b.getOperator() == J.Binary.Type.Or) {
                List<String> leaves = new ArrayList<>();
                return collectOrChainNames(b, leaves) ? leaves : null;
            }
        }
        if (side instanceof G.Binary) {
            G.Binary gb = (G.Binary) side;
            if (gb.getOperator() == G.Binary.Type.In && "name".equals(fieldOf(gb.getLeft()))) {
                return listLiteralsOf(gb.getRight());
            }
        }
        if (side instanceof K.Binary) {
            K.Binary kb = (K.Binary) side;
            if (kb.getOperator() == K.Binary.Type.Contains && "name".equals(fieldOf(kb.getLeft()))) {
                return listOfLiteralsOf(kb.getRight());
            }
        }
        return null;
    }

    // `name == 'x'` → "x". Any other shape → null. Works for both left and right side being the field.
    private static @Nullable String nameEqualsLiteral(J.Binary b) {
        if (b.getOperator() != J.Binary.Type.Equal) {
            return null;
        }
        if ("name".equals(fieldOf(b.getLeft()))) {
            return literalOf(b.getRight());
        }
        if ("name".equals(fieldOf(b.getRight()))) {
            return literalOf(b.getLeft());
        }
        return null;
    }

    // Walks a J.Binary Or tree, collecting all leaf `name == 'x'` literals in left-to-right order.
    // Returns true iff every leaf matched the expected shape (otherwise the chain mixes in something else
    // we don't know how to handle and we should bail).
    private static boolean collectOrChainNames(J.Binary or, List<String> out) {
        for (Expression side : Arrays.asList(or.getLeft(), or.getRight())) {
            if (side instanceof J.Binary) {
                J.Binary b = (J.Binary) side;
                if (b.getOperator() == J.Binary.Type.Or) {
                    if (!collectOrChainNames(b, out)) {
                        return false;
                    }
                    continue;
                }
                if (b.getOperator() == J.Binary.Type.Equal) {
                    String literal = nameEqualsLiteral(b);
                    if (literal != null) {
                        out.add(literal);
                        continue;
                    }
                }
            }
            return false;
        }
        return true;
    }

    static @Nullable ThenInfo extractThen(Statement thenPart) {
        if (!(thenPart instanceof J.Block)) {
            return null;
        }
        AtomicReference<String> version = new AtomicReference<>();
        AtomicBoolean hasBecause = new AtomicBoolean(false);
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, Integer p) {
                J.MethodInvocation mi = super.visitMethodInvocation(m, p);
                if (METHOD_USE_VERSION.equals(mi.getSimpleName()) && !mi.getArguments().isEmpty()) {
                    Expression arg = mi.getArguments().get(0);
                    if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                        version.set((String) ((J.Literal) arg).getValue());
                    }
                } else if (METHOD_BECAUSE.equals(mi.getSimpleName())) {
                    hasBecause.set(true);
                }
                return mi;
            }
        }.visit(thenPart, 0);
        if (version.get() == null) {
            return null;
        }
        return new ThenInfo(version.get(), hasBecause.get());
    }

    private static @Nullable String fieldOf(Expression e) {
        if (e instanceof J.FieldAccess) {
            return ((J.FieldAccess) e).getSimpleName();
        }
        return null;
    }

    private static @Nullable String literalOf(Expression e) {
        if (e instanceof J.Literal && ((J.Literal) e).getValue() instanceof String) {
            return (String) ((J.Literal) e).getValue();
        }
        return null;
    }

    private static @Nullable List<String> listLiteralsOf(Expression e) {
        if (!(e instanceof G.ListLiteral)) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Expression el : ((G.ListLiteral) e).getElements()) {
            if (!(el instanceof J.Literal) || !(((J.Literal) el).getValue() instanceof String)) {
                return null;
            }
            out.add((String) ((J.Literal) el).getValue());
        }
        return out;
    }

    // `listOf("a", "b")` → ["a", "b"]. Anything else → null. The method invocation must be a bare call
    // (no select) named "listOf" with all-String arguments.
    private static @Nullable List<String> listOfLiteralsOf(Expression e) {
        if (!(e instanceof J.MethodInvocation)) {
            return null;
        }
        J.MethodInvocation m = (J.MethodInvocation) e;
        if (!"listOf".equals(m.getSimpleName()) || m.getSelect() != null) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Expression arg : m.getArguments()) {
            if (!(arg instanceof J.Literal) || !(((J.Literal) arg).getValue() instanceof String)) {
                return null;
            }
            out.add((String) ((J.Literal) arg).getValue());
        }
        return out;
    }

    static boolean isEachDependency(J.MethodInvocation m, Cursor cursor) {
        if (!METHOD_EACH_DEPENDENCY.equals(m.getSimpleName())) {
            return false;
        }
        if (m.getSelect() instanceof J.Identifier &&
                METHOD_RESOLUTION_STRATEGY.equals(((J.Identifier) m.getSelect()).getSimpleName())) {
            return true;
        }
        if (m.getSelect() == null) {
            Cursor parent = cursor.dropParentUntil(v ->
                    v == Cursor.ROOT_VALUE ||
                            (v instanceof J.MethodInvocation &&
                                    METHOD_RESOLUTION_STRATEGY.equals(((J.MethodInvocation) v).getSimpleName())));
            return parent.getValue() != Cursor.ROOT_VALUE;
        }
        return false;
    }
}
