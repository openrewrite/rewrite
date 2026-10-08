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

            // Walk the predicate; find the subtree that is the name check (either `name == 'x'` or `name in [...]`)
            // and swap it to match the given artifactIds (collapsing length-1 lists to `==`).
            private Expression replaceNameCheck(Expression predicate, PredicateInfo pred, List<String> artifactIds) {
                return (Expression) new JavaIsoVisitor<Integer>() {
                    @Override
                    public J visit(@Nullable Tree tree, Integer p) {
                        if (tree instanceof J.Binary) {
                            J.Binary b = (J.Binary) tree;
                            if (b.getOperator() == J.Binary.Type.Equal && isNameSide(b)) {
                                return buildNameCheck(b, pred.wasNameCheckOnLeft, artifactIds);
                            }
                            if (b.getOperator() == J.Binary.Type.And) {
                                return b.withLeft((Expression) visit(b.getLeft(), p))
                                        .withRight((Expression) visit(b.getRight(), p));
                            }
                        }
                        if (tree instanceof G.Binary) {
                            G.Binary gb = (G.Binary) tree;
                            if ((gb.getOperator() == G.Binary.Type.In || gb.getOperator() == G.Binary.Type.NotIn) && isNameSide(gb)) {
                                return buildNameCheck(gb, pred.wasNameCheckOnLeft, artifactIds);
                            }
                        }
                        return (J) tree;
                    }

                    private boolean isNameSide(J.Binary b) {
                        return "name".equals(fieldOf(b.getLeft())) || "name".equals(fieldOf(b.getRight()));
                    }

                    private boolean isNameSide(G.Binary b) {
                        return "name".equals(fieldOf(b.getLeft()));
                    }
                }.visit(predicate, 0);
            }

            // Build the replacement name-check. For size-1 artifact groups: `<name-access> == 'artifact'`.
            // For size-2+ groups: filter the original `G.ListLiteral` to only the keep-set, preserving padding.
            // (Reusing the original list literal avoids building commas/brackets from scratch.)
            private Expression buildNameCheck(Expression original, boolean nameOnLeft, List<String> artifactIds) {
                if (artifactIds.size() == 1) {
                    String v = artifactIds.get(0);
                    J.Literal lit = new J.Literal(
                            Tree.randomId(),
                            Space.format(" "),
                            Markers.EMPTY,
                            v, "'" + v + "'", null,
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
                // Multi-artifact: must originate from a `name in [...]` predicate.
                if (!(original instanceof G.Binary)) {
                    throw new IllegalStateException("Multi-artifact split expects an `in [...]` predicate; got " + original.getClass().getSimpleName());
                }
                G.Binary origIn = (G.Binary) original;
                if (origIn.getOperator() != G.Binary.Type.In || !(origIn.getRight() instanceof G.ListLiteral)) {
                    throw new IllegalStateException("Multi-artifact split expects `in` operator with ListLiteral RHS");
                }
                G.ListLiteral origList = (G.ListLiteral) origIn.getRight();
                List<Expression> filtered = new ArrayList<>();
                for (Expression el : origList.getElements()) {
                    if (el instanceof J.Literal
                            && ((J.Literal) el).getValue() instanceof String
                            && artifactIds.contains(((J.Literal) el).getValue())) {
                        filtered.add(el);
                    }
                }
                // In the original list, element[0] has prefix "" (no leading comma to space over)
                // while later elements have prefix " ". When we filter to a subset that doesn't include
                // the original element[0], the new first element still carries its " " prefix and the list
                // prints as `[ 'foo', 'bar']`. Reset to no-leading-space so output matches `['foo', 'bar']`.
                if (!filtered.isEmpty() && filtered.get(0) != origList.getElements().get(0)) {
                    Expression first = filtered.get(0);
                    filtered.set(0, first.withPrefix(Space.EMPTY));
                }
                return origIn.withRight(origList.withElements(filtered));
            }

            // Pull the `requested.name` field access out of either side of the original binary.
            private Expression nameAccessOf(Expression original) {
                if (original instanceof J.Binary) {
                    J.Binary b = (J.Binary) original;
                    if ("name".equals(fieldOf(b.getLeft()))) return b.getLeft();
                    if ("name".equals(fieldOf(b.getRight()))) return b.getRight();
                }
                if (original instanceof G.Binary) {
                    G.Binary b = (G.Binary) original;
                    if ("name".equals(fieldOf(b.getLeft()))) return b.getLeft();
                }
                throw new IllegalStateException("Could not locate name field access on original predicate");
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
        boolean wasNameCheckOnLeft; // reserved for future formatting fidelity
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
        AtomicReference<String> groupId = new AtomicReference<>();
        AtomicReference<List<String>> artifactIds = new AtomicReference<>();

        for (Expression side : Arrays.asList(and.getLeft(), and.getRight())) {
            if (side instanceof J.Binary) {
                J.Binary b = (J.Binary) side;
                if (b.getOperator() == J.Binary.Type.Equal) {
                    String field = fieldOf(b.getLeft());
                    String literal = literalOf(b.getRight());
                    if (field == null) {
                        field = fieldOf(b.getRight());
                        literal = literalOf(b.getLeft());
                    }
                    if ("group".equals(field) && literal != null) {
                        groupId.set(literal);
                    } else if ("name".equals(field) && literal != null) {
                        artifactIds.set(singletonList(literal));
                    }
                }
            } else if (side instanceof G.Binary) {
                G.Binary gb = (G.Binary) side;
                if (gb.getOperator() == G.Binary.Type.In) {
                    String field = fieldOf(gb.getLeft());
                    if ("name".equals(field)) {
                        List<String> literals = listLiteralsOf(gb.getRight());
                        if (literals != null) {
                            artifactIds.set(literals);
                        }
                    }
                }
            }
        }

        if (groupId.get() == null || artifactIds.get() == null) {
            return null;
        }
        return new PredicateInfo(groupId.get(), artifactIds.get(), false);
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
