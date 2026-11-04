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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.NodeCatalogs;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PackageJsonOverrides;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);

    @Option(displayName = "Package name",
            description = "The name of the transitive npm dependency to upgrade.",
            example = "lodash")
    String packageName;

    @Option(displayName = "New version",
            description = "The version constraint to set on the override entry.",
            example = "^5.0.0")
    String newVersion;

    @Option(displayName = "Dependency path",
            description = "Optional dependency path (pnpm-style `a>b>c` or yarn-style `a/b/c`) " +
                    "to scope the override. When omitted, applies as a global override.",
            example = "express>accepts",
            required = false)
    @Nullable String dependencyPath;

    @Override public String getDisplayName() { return "Upgrade transitive npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Pins or upgrades a transitive npm dependency with an override at the workspace root, where its " +
                "package manager reads it, and regenerates the lock file natively, without executing the package " +
                "manager: npm's `overrides`, Bun's `overrides` or `resolutions`, Yarn's `resolutions`, and for " +
                "pnpm `pnpm-workspace.yaml` or `pnpm.overrides` by the pnpm release the project names. An override " +
                "the package manager cannot read is not written, and the reason is recorded. None is written for a " +
                "package the lock does not resolve. Not safe to use as a " +
                "precondition: consults the package registry over the network and publishes per-project state " +
                "shared with other dependency recipes.";
    }

    @Override
    public NodeDependencyEdits getInitialValue(ExecutionContext ctx) {
        return new NodeDependencyEdits(NodeDependencyEdits.rows(lockRegenerationFailures, editsDeclined, packageName),
                packageName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NodeDependencyEdits acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
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
        return acc.decideAll(ctx, (decision, c) -> decide(acc, decision, c));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor((decision, c) -> decide(acc, decision, c));
    }

    // the override, at the workspace root only, where its package manager reads it
    private void decide(NodeDependencyEdits acc, NodeDependencyEdits.Decision decision, ExecutionContext ctx) {
        PackageManager pm;
        try {
            pm = decision.getPackageManager();
        } catch (Rejected e) {
            decision.decline(ctx, String.valueOf(e.getMessage()));
            return;
        }
        if (pm == null || !decision.resolves()) {
            return;
        }
        Path path = decision.getPath();
        ManifestRules rules = LockFileRegeneration.manifestRules(pm);
        Path root;
        try {
            root = workspaceRoot(acc.scan, path, pm, rules);
        } catch (Rejected e) {
            decision.decline(ctx, String.valueOf(e.getMessage()));
            return;
        }
        if (!root.equals(path)) {
            if (!acc.holds(root)) {
                decision.decline(ctx, "the override belongs in the workspace root's " + root +
                                      ", which is not a package.json the recipe edits");
            }
            return;
        }
        String workspace = null;
        String settingsFile = rules.workspaceSettingsFile();
        if (settingsFile != null) {
            Path workspaceManifest = root.resolveSibling(settingsFile);
            NodeCatalogs catalogs = decision.catalogs();
            if (catalogs != null && catalogs.getPath().equals(workspaceManifest)) {
                workspace = catalogs.print();
            } else if (acc.scan.paths.contains(workspaceManifest)) {
                decision.cannotKnow(ctx, CannotKnow.unread(settingsFile));
                return;
            }
        }
        Json.Document manifest = decision.getManifest();
        PackageJsonOverrides override = PackageJsonOverrides.of(pm, manifest, workspace, packageName, newVersion,
                dependencyPath == null ? null : PackageJsonOverrides.parsePath(dependencyPath));
        decision.describe(override.toString());
        String declined = override.declined();
        if (declined != null) {
            decision.decline(ctx, declined);
            return;
        }
        boolean inWorkspace = override.getPlace() == OverridePlace.WORKSPACE_MANIFEST;
        if (inWorkspace ? override.applyToWorkspaceManifest().equals(workspace) :
                override.apply(manifest) == manifest) {
            return;
        }
        List<NodeDependencyProtocolsSkipped.Row> references = references(acc, ctx, root, manifest, pm, rules);
        for (NodeDependencyProtocolsSkipped.Row row : references) {
            decision.insertRow(protocolsSkipped, ctx, row);
        }
        if (!references.isEmpty()) {
            return;
        }
        String outranked = override.outranked();
        if (outranked != null) {
            decision.decline(ctx, outranked);
        }
        if (inWorkspace) {
            decision.editFileBeside(ctx, requireNonNull(settingsFile), c -> c instanceof NodeCatalogs.Pnpm ?
                    override.from(c.print()).applyTo((NodeCatalogs.Pnpm) c) : null);
        } else {
            decision.editManifest(override::apply);
        }
    }

    // the declarations the override would win over unnamed: packageName through a value that is no version range,
    // in the root or a member it governs, or for an override scoped to a member, in that member
    private List<NodeDependencyProtocolsSkipped.Row> references(NodeDependencyEdits acc, ExecutionContext ctx,
                                                                Path root, Json.Document rootRevision,
                                                                PackageManager pm, ManifestRules rules) {
        List<DependencyPathSegment> path = dependencyPath == null ? null :
                PackageJsonOverrides.parsePath(dependencyPath);
        String parent = path == null || path.isEmpty() ? null : path.get(path.size() - 1).getName();
        List<NodeDependencyProtocolsSkipped.Row> rows = new ArrayList<>();
        for (Path project : new TreeSet<>(acc.scan.projects.keySet())) {
            SourceFile manifest = project.equals(root) ? rootRevision : acc.editedBefore(ctx, project);
            if (manifest == null || !root.equals(governingRoot(acc.scan, project, pm, rules)) ||
                parent != null && (project.equals(root) || !parent.equals(PackageJsonHelper.declaredName(manifest)))) {
                continue;
            }
            for (Map.Entry<String, Map<String, String>> scope :
                    PackageJsonHelper.declaredDependencies(manifest).entrySet()) {
                String value = scope.getValue().get(packageName);
                Spec spec = value == null ? null : Spec.ofDependencyOrNull(packageName, value);
                if (value != null && (spec == null || !spec.isVersionSpec())) {
                    rows.add(new NodeDependencyProtocolsSkipped.Row(root.toString(), project.toString(),
                            packageName, scope.getKey(), spec == null || spec.getProtocol() == null ? "" :
                            spec.getProtocol(), value, newVersion));
                }
            }
        }
        return rows;
    }

    // the package manager's workspace root: the one its settings file names, else where it installs
    private static Path workspaceRoot(NodeDependencyScan.Accumulator acc, Path packageJson, PackageManager pm,
                                      ManifestRules rules) {
        Path dir = packageJson.getParent();
        String found = rules.workspaceRoot(path -> acc.paths.contains(Paths.get(path)),
                dir == null ? "" : dir.toString().replace('\\', '/'));
        if (found != null) {
            return found.isEmpty() ? Paths.get("package.json") : Paths.get(found).resolve("package.json");
        }
        return NodeDependencyScan.installRootOf(acc, packageJson, pm);
    }

    // a project whose workspace root the package manager rejects is governed by none
    private static @Nullable Path governingRoot(NodeDependencyScan.Accumulator acc, Path packageJson,
                                                PackageManager pm, ManifestRules rules) {
        try {
            return workspaceRoot(acc, packageJson, pm, rules);
        } catch (Rejected e) {
            return null;
        }
    }
}
