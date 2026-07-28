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
import org.openrewrite.javascript.internal.DependencyPathSegment;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PackageJsonOverrides;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<UpgradeTransitiveDependencyVersion.Accumulator> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);

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
        return "Pins or upgrades a transitive npm dependency by adding an override entry to `package.json` " +
                "and regenerating the lock file. For npm and Bun, adds to the `overrides` field; " +
                "for Yarn, adds to `resolutions`; for pnpm, adds to `pnpm.overrides`. " +
                "The override is idempotent — if the entry already exists with the same version, no change is made. " +
                "Not safe to use as a precondition: invokes the package manager and publishes per-project " +
                "state shared with other dependency recipes.";
    }

    static class Accumulator {
        final Map<Path, ProjectState> projects = new HashMap<>();
        final Map<Path, Path> lockToPackage = new HashMap<>();
    }

    static class ProjectState {
        @Nullable SourceFile capturedPackageJson;
        @Nullable String capturedLockContent;
        @Nullable SourceFile modifiedPackageJson;
        LockFileRegeneration.@Nullable Result regenResult;
        boolean failureRecorded;
    }

    @Override public Accumulator getInitialValue(ExecutionContext ctx) { return new Accumulator(); }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();
                String basename = p.getFileName().toString();

                if (PackageJsonHelper.isLockFile(basename)) {
                    if (sf instanceof Json.Document || sf instanceof Yaml.Documents || sf instanceof PlainText) {
                        Path packagePath = PackageJsonHelper.correspondingPackageJsonPath(p);
                        ProjectState ps = acc.projects.computeIfAbsent(packagePath, k -> new ProjectState());
                        ps.capturedLockContent = sf.printAll();
                        acc.lockToPackage.put(p, packagePath);
                    }
                    return tree;
                }
                if (sf instanceof Json.Document && "package.json".equals(basename)) {
                    NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                    if (marker == null) return tree;
                    ProjectState ps = acc.projects.computeIfAbsent(p, k -> new ProjectState());
                    ps.capturedPackageJson = sf;
                }
                return tree;
            }
        };
    }

    private boolean canApply(SourceFile pkg) {
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        return marker != null && marker.getPackageManager() != null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        linkWorkspaceMembers(acc);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();

                ProjectState ps = acc.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    if (canApply(sf)) {
                        ensureComputed(ps, sf, ctx);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = ps.modifiedPackageJson;
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p);
                            out = Markup.warn(out, new RuntimeException(
                                    "lock regeneration failed: " + ps.regenResult.getErrorMessage()));
                        }
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        return out;
                    }
                }

                Path packagePath = acc.lockToPackage.get(p);
                if (packagePath == null) return tree;
                ProjectState rootPs = acc.projects.get(packagePath);
                if (rootPs == null) return tree;

                for (Path importer : lockImporters(acc, packagePath, rootPs)) {
                    ProjectState ips = acc.projects.get(importer);
                    if (ips == null) continue;
                    if (ips.modifiedPackageJson == null) {
                        SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, importer);
                        if (pkg == null) pkg = ips.capturedPackageJson;
                        if (pkg != null && canApply(pkg)) {
                            ensureComputed(ips, pkg, ctx);
                            if (ips.modifiedPackageJson != null) {
                                PackageJsonHelper.putLiveTree(ctx, importer, ips.modifiedPackageJson);
                            }
                        }
                    }
                    if (ips.regenResult != null) {
                        if (ips.regenResult.isSuccess()) {
                            return PackageJsonHelper.reparseLock(sf, ips.regenResult.getLockFileContent());
                        }
                        recordFailure(ctx, ips, importer);
                        return Markup.warn(sf, new RuntimeException(
                                "lock regeneration failed: " + ips.regenResult.getErrorMessage()));
                    }
                }
                return tree;
            }

            private void ensureComputed(ProjectState ps, SourceFile pkg, ExecutionContext ctx) {
                if (ps.modifiedPackageJson != null) return;
                NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                if (marker == null || marker.getPackageManager() == null) return;
                NodeResolutionResult.PackageManager pm = marker.getPackageManager();
                List<DependencyPathSegment> parsedPath = dependencyPath == null
                        ? null
                        : PackageJsonOverrides.parsePath(dependencyPath);

                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg,
                        doc -> PackageJsonHelper.upgradeTransitive(doc, pm, packageName, newVersion, parsedPath),
                        ps.capturedLockContent,
                        ctx);
                if (r.isChanged()) {
                    ps.modifiedPackageJson = r.getModifiedPackageJson();
                    ps.regenResult = r.getRegenResult();
                }
            }
        };
    }

    private void recordFailure(ExecutionContext ctx, ProjectState ps, Path packageJsonPath) {
        if (ps.failureRecorded || ps.regenResult == null) {
            return;
        }
        ps.failureRecorded = true;
        LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, ps.regenResult, packageName);
    }

    /**
     * Link each workspace-member {@code package.json} to the ancestor root lock, so editing a member
     * regenerates that lock. A member with its own sibling lock keeps it.
     */
    private void linkWorkspaceMembers(Accumulator acc) {
        for (Map.Entry<Path, Path> e : acc.lockToPackage.entrySet()) {
            ProjectState root = acc.projects.get(e.getValue());
            if (root == null || root.capturedPackageJson == null || root.capturedLockContent == null) {
                continue;
            }
            for (Path member : PackageJsonHelper.workspaceMemberPaths(root.capturedPackageJson)) {
                ProjectState memberPs = acc.projects.get(member);
                if (memberPs != null && memberPs.capturedLockContent == null) {
                    memberPs.capturedLockContent = root.capturedLockContent;
                }
            }
        }
    }

    /** The manifests that can regenerate this lock: the sibling manifest first, then any workspace members it covers. */
    private List<Path> lockImporters(Accumulator acc, Path packagePath, ProjectState rootPs) {
        List<Path> importers = new ArrayList<>();
        importers.add(packagePath);
        if (rootPs.capturedPackageJson != null) {
            for (Path member : PackageJsonHelper.workspaceMemberPaths(rootPs.capturedPackageJson)) {
                if (!member.equals(packagePath) && acc.projects.containsKey(member)) {
                    importers.add(member);
                }
            }
        }
        return importers;
    }
}
