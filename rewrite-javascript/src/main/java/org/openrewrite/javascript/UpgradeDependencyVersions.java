/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.semver.NodeSemver;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersions extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

    @Option(displayName = "Versions",
            description = "Package-name glob patterns mapped to target version constraints. All matching upgrades " +
                    "are decided together before regenerating the lock file. Must not be empty. " +
                    "Overlapping patterns must specify the same target constraint.",
            example = "{\"@angular/*\": \"16.x\", \"typescript\": \"5.0.x\"}")
    Map<String, String> versions;

    @Option(displayName = "Reconcile npm peer dependencies",
            description = "Recover from stale npm lock peer conflicts only when the complete upgraded manifest " +
                    "resolves without a lock. The rebuilt lock must pass a subsequent plain install validation. " +
                    "Defaults to false; genuine peer conflicts leave the dependency edits unapplied.",
            required = false)
    @Nullable Boolean reconcileNpmPeerDependencies;

    @Option(displayName = "Unless resolving",
            description = "Leave a `package.json` as it is when it declares, in any scope, a package that one of these " +
                    "`name@range` entries holds for. An entry is split at the last `@`, and its name is a glob " +
                    "pattern. It holds for a declared package the pattern matches when the version the lock file " +
                    "resolves satisfies the range or, without one, when the declared constraint admits a version in " +
                    "the range, so `^20.0.0` holds for `<23`. A dependency that is not a version range, such as a " +
                    "`file:` or `workspace:` reference, holds for none. Declarations are read as they stand before the recipes " +
                    "in a cycle edit them. Leave unset to upgrade in every `package.json`.",
            example = "ag-grid-*@<23",
            required = false)
    @Nullable List<String> unlessResolving;

    public UpgradeDependencyVersions(Map<String, String> versions, @Nullable Boolean reconcileNpmPeerDependencies) {
        this(versions, reconcileNpmPeerDependencies, null);
    }

    @JsonCreator
    public UpgradeDependencyVersions(Map<String, String> versions, @Nullable Boolean reconcileNpmPeerDependencies,
                                     @Nullable List<String> unlessResolving) {
        this.versions = versions;
        this.reconcileNpmPeerDependencies = reconcileNpmPeerDependencies;
        this.unlessResolving = unlessResolving;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade npm dependency versions together";
    }

    @Override
    public String getDescription() {
        return "Upgrades matching npm dependencies as one coordinated edit and regenerates the lock file once. " +
                "Uses the same version-range, peer-range, protocol and catalog rules as `UpgradeDependencyVersion`. " +
                "Optionally reconciles stale npm lock peer conflicts after verifying the upgraded dependencies " +
                "resolve together. Optionally leaves alone a `package.json` that declares a given package at a " +
                "given version. Not safe to use as a precondition: consults the package registry over the " +
                "network and publishes per-project state shared with other dependency recipes.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate().and(Validated.test("versions",
                "Must contain at least one package pattern and target version.", versions,
                v -> v != null && !v.isEmpty()));
        if (unlessResolving != null) {
            for (String entry : unlessResolving) {
                validated = validated.and(Validated.test("unlessResolving",
                        "entries are `name@range` with a valid npm semver range", entry, e -> nameAndRange(e) != null));
            }
        }
        if (versions == null) {
            return validated;
        }
        boolean entriesValid = versions.entrySet().stream().allMatch(e ->
                e.getKey() != null && !StringUtils.isBlank(e.getKey()) &&
                e.getValue() != null && !StringUtils.isBlank(e.getValue()));
        validated = validated.and(Validated.test("versions",
                "Package patterns and target versions must not be null or blank.", versions, v -> entriesValid));
        if (!entriesValid) {
            return validated;
        }
        List<String> patterns = new ArrayList<>(versions.keySet());
        Collections.sort(patterns);
        for (int i = 0; i < patterns.size(); i++) {
            for (int j = i + 1; j < patterns.size(); j++) {
                String left = patterns.get(i);
                String right = patterns.get(j);
                if (!versions.get(left).equals(versions.get(right)) && patternsOverlap(left, right)) {
                    validated = validated.and(Validated.invalid("versions", versions,
                            "Overlapping package patterns `" + left + "` and `" + right +
                                    "` must specify the same target version constraint."));
                }
            }
        }
        return validated;
    }

    // Intersection of the * and ? glob languages used by PackageJsonHelper.compileGlobPattern.
    // Each reachable pair is a position in the two patterns after consuming the same package-name prefix.
    private static boolean patternsOverlap(String left, String right) {
        boolean[][] reachable = new boolean[left.length() + 1][right.length() + 1];
        reachable[0][0] = true;
        for (int i = 0; i <= left.length(); i++) {
            for (int j = 0; j <= right.length(); j++) {
                if (!reachable[i][j]) {
                    continue;
                }
                boolean leftStar = i < left.length() && left.charAt(i) == '*';
                boolean rightStar = j < right.length() && right.charAt(j) == '*';
                if (leftStar) {
                    reachable[i + 1][j] = true;
                }
                if (rightStar) {
                    reachable[i][j + 1] = true;
                }
                if (i < left.length() && j < right.length() &&
                    (leftStar || rightStar || left.charAt(i) == '?' || right.charAt(j) == '?' ||
                     left.charAt(i) == right.charAt(j))) {
                    reachable[leftStar ? i : i + 1][rightStar ? j : j + 1] = true;
                }
            }
        }
        return reachable[left.length()][right.length()];
    }

    @Override
    public boolean causesAnotherCycle() {
        return NodeDependencyEdits.deferredThisCycle();
    }

    @Override
    public void onComplete(ExecutionContext ctx) {
        NodeDependencyEdits.reportUnresolvedDeferrals(ctx);
    }

    @Override
    public NodeDependencyEdits getInitialValue(ExecutionContext ctx) {
        Validated<Object> validated = validate();
        if (validated.isInvalid()) {
            throw new ValidationException(validated);
        }
        return new NodeDependencyEdits(NodeDependencyEdits.rows(lockRegenerationFailures, editsDeclined, null),
                Boolean.TRUE.equals(reconcileNpmPeerDependencies));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NodeDependencyEdits acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    acc.scan((SourceFile) tree);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(NodeDependencyEdits acc, ExecutionContext ctx) {
        return acc.decideAll(ctx, this::decide);
    }

    private void decide(NodeDependencyEdits.Decision decision, ExecutionContext ctx) {
        if (resolvesAny(decision)) {
            return;
        }
        for (Map.Entry<String, String> entry : versions.entrySet()) {
            new UpgradeDependencyVersion(null, entry.getKey(), entry.getValue())
                    .decide(decision, ctx, protocolsSkipped);
        }
    }

    // whether the manifest declares a package an unlessResolving entry holds for
    private boolean resolvesAny(NodeDependencyEdits.Decision decision) {
        if (unlessResolving == null) {
            return false;
        }
        NodeDependencyScan.Manifest manifest = decision.scanned();
        for (String entry : unlessResolving) {
            Map.Entry<String, String> nameAndRange = nameAndRange(entry);
            if (nameAndRange == null) {
                continue;
            }
            Pattern name = PackageJsonHelper.compileGlobPattern(nameAndRange.getKey());
            for (Map<String, String> declared : manifest.getDeclared().values()) {
                for (Map.Entry<String, String> dependency : declared.entrySet()) {
                    if (name.matcher(dependency.getKey()).matches() &&
                        resolves(manifest, dependency.getKey(), dependency.getValue(), nameAndRange.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean resolves(NodeDependencyScan.Manifest manifest, String name, String constraint, String range) {
        try {
            String resolved = manifest.resolvedVersion(name);
            if (resolved != null) {
                return NodeSemver.satisfies(resolved, range, NodeSemver.Options.LOOSE);
            }
            return NodeSemver.validRange(constraint, NodeSemver.Options.LOOSE) != null &&
                   NodeSemver.intersects(constraint, range, NodeSemver.Options.LOOSE);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Map.@Nullable Entry<String, String> nameAndRange(@Nullable String entry) {
        if (entry == null) {
            return null;
        }
        String trimmed = entry.trim();
        int at = trimmed.lastIndexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return null;
        }
        String range = trimmed.substring(at + 1);
        return NodeSemver.validRange(range, NodeSemver.Options.LOOSE) == null ? null :
                new AbstractMap.SimpleImmutableEntry<>(trimmed.substring(0, at), range);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor(this::decide);
    }
}
