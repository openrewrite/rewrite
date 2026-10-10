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
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PackageJsonOverrides;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

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
        return "Pins or upgrades a transitive npm dependency with an override at the workspace root, where its " +
                "package manager reads it, and regenerates the lock file natively, without executing the package " +
                "manager: npm's `overrides`, Bun's `overrides` or `resolutions`, Yarn's `resolutions`, and for " +
                "pnpm `pnpm-workspace.yaml` or `pnpm.overrides` by the pnpm release the project names. An override " +
                "the package manager cannot read is not written, and the reason is recorded. Not safe to use as a " +
                "precondition: consults the package registry over the network and publishes per-project state " +
                "shared with other dependency recipes.";
    }

    public static class Accumulator {
        final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
        /** The package-manager files each workspace root's edit changes or creates, by the root's package.json. */
        final Map<Path, Map<Path, String>> editedFiles = new HashMap<>();
        /** The edit of each pnpm-workspace.yaml, to apply again over a revision an earlier recipe made since. */
        final Map<Path, UnaryOperator<String>> workspaceEdits = new HashMap<>();
        final Set<Path> computed = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();
                String basename = p.getFileName().toString();

                if (NodeDependencyScan.capturePackageManagerFile(acc.scan, sf)) return tree;
                if (PackageJsonHelper.isLockFile(basename)) {
                    if (sf instanceof Json.Document || sf instanceof Yaml.Documents || sf instanceof PlainText) {
                        NodeDependencyScan.captureLock(acc.scan, sf);
                    }
                    return tree;
                }
                if (sf instanceof Json.Document && "package.json".equals(basename)) {
                    NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                    if (marker == null) return tree;
                    NodeDependencyScan.ProjectState ps = acc.scan.projects.computeIfAbsent(p, k ->
                            new NodeDependencyScan.ProjectState());
                    ps.capturedPackageJson = sf;
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        NodeDependencyScan.linkWorkspaceMembers(acc.scan);
        List<SourceFile> created = new ArrayList<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : acc.scan.projects.entrySet()) {
            SourceFile pkg = project.getValue().capturedPackageJson;
            Path workspaceManifest = project.getKey().resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
            NodeResolutionResult marker = pkg == null ? null :
                    pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
            if (marker == null || marker.getPackageManager() != NodeResolutionResult.PackageManager.Pnpm ||
                acc.scan.paths.contains(workspaceManifest) ||
                !project.getKey().equals(workspaceRoot(acc.scan, project.getKey(), marker.getPackageManager())) ||
                PackageJsonOverrides.place(marker.getPackageManager(), (Json.Document) pkg, null) !=
                PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
                continue;
            }
            ensureComputed(acc, project.getValue(), pkg, ctx);
            String content = acc.editedFiles.getOrDefault(project.getKey(), emptyMap()).get(workspaceManifest);
            if (content != null) {
                YamlParser.builder().build().parse(ctx, content).findFirst()
                        .ifPresent(yaml -> created.add(yaml.withSourcePath(workspaceManifest)));
            }
        }
        return created;
    }

    private static boolean canApply(SourceFile pkg) {
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        return marker != null && marker.getPackageManager() != null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        NodeDependencyScan.linkWorkspaceMembers(acc.scan);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();

                if (PackageJsonOverrides.WORKSPACE_MANIFEST.equals(String.valueOf(p.getFileName()))) {
                    return editedWorkspaceManifest(sf, ctx);
                }

                NodeDependencyScan.ProjectState ps = acc.scan.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    if (canApply(sf)) {
                        ensureComputed(acc, ps, sf, ctx);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = NodeDependencyScan.modifiedFor(ps, sf);
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p);
                            return Markup.warn(out, new RuntimeException(
                                    "lock regeneration failed: " + ps.regenResult.getErrorMessage()));
                        }
                        return out;
                    }
                }

                for (Path importer : NodeDependencyScan.lockImporters(acc.scan, p)) {
                    NodeDependencyScan.ProjectState ips = acc.scan.projects.get(importer);
                    if (ips == null) continue;
                    if (!acc.computed.contains(importer)) {
                        SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, importer);
                        if (pkg == null) pkg = ips.capturedPackageJson;
                        if (pkg != null && canApply(pkg)) {
                            ensureComputed(acc, ips, pkg, ctx);
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

            private SourceFile editedWorkspaceManifest(SourceFile sf, ExecutionContext ctx) {
                Path root = sf.getSourcePath().resolveSibling("package.json");
                NodeDependencyScan.ProjectState ps = acc.scan.projects.get(root);
                if (ps == null || ps.capturedPackageJson == null) {
                    return sf;
                }
                SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, root);
                if (pkg == null) pkg = ps.capturedPackageJson;
                if (canApply(pkg)) {
                    ensureComputed(acc, ps, pkg, ctx);
                }
                Path path = sf.getSourcePath();
                String content = acc.editedFiles.getOrDefault(root, emptyMap()).get(path);
                UnaryOperator<String> edit = acc.workspaceEdits.get(path);
                String visited = sf.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
                if (content != null && edit != null && !visited.equals(acc.scan.packageManagerFiles.get(path))) {
                    try {
                        content = edit.apply(visited);
                    } catch (IllegalArgumentException e) {
                        return Markup.warn(sf, e);
                    }
                }
                return content == null || content.equals(visited) ? sf : PackageJsonHelper.reparseLock(sf, content);
            }
        };
    }

    private void ensureComputed(Accumulator acc, NodeDependencyScan.ProjectState ps, SourceFile pkg,
                                ExecutionContext ctx) {
        Path path = pkg.getSourcePath();
        if (!acc.computed.add(path) || !(pkg instanceof Json.Document)) {
            return;
        }
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || marker.getPackageManager() == null) {
            return;
        }
        NodeResolutionResult.PackageManager pm = marker.getPackageManager();
        Path root = workspaceRoot(acc.scan, path, pm);
        if (!root.equals(path)) {
            NodeDependencyScan.ProjectState rootPs = acc.scan.projects.get(root);
            if (rootPs == null || rootPs.capturedPackageJson == null || !canApply(rootPs.capturedPackageJson)) {
                decline(ctx, ps, path, "the override belongs in the workspace root's " + root +
                                       ", which is not a package.json the recipe edits");
            } else {
                SourceFile rootPkg = PackageJsonHelper.getLiveTree(ctx, root);
                ensureComputed(acc, rootPs, rootPkg == null ? rootPs.capturedPackageJson : rootPkg, ctx);
            }
            return;
        }
        Json.Document doc = (Json.Document) pkg;
        Path workspaceManifest = path.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        String current = acc.scan.packageManagerFiles.get(workspaceManifest);
        if (pm == NodeResolutionResult.PackageManager.Pnpm && current == null &&
            acc.scan.paths.contains(workspaceManifest)) {
            ps.failureRecorded = true;
            lockRegenerationFailures.insertRow(ctx, new NodeLockRegenerationFailures.Row(path.toString(),
                    packageName, LockFileRegeneration.Reason.RESOLUTION_REQUIRED.toString(),
                    "the engine cannot know " + PackageJsonOverrides.WORKSPACE_MANIFEST +
                    " (its contents were not read)"));
            return;
        }
        List<DependencyPathSegment> parsedPath = dependencyPath == null ? null :
                PackageJsonOverrides.parsePath(dependencyPath);
        PackageJsonOverrides.Place place = PackageJsonOverrides.place(pm, doc, current);
        String declined = PackageJsonOverrides.declined(pm, doc, place, current, parsedPath);
        if (declined != null) {
            decline(ctx, ps, path, declined);
            return;
        }

        Map<Path, String> edited = emptyMap();
        if (place == PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            UnaryOperator<String> workspaceEdit = text ->
                    PackageJsonOverrides.applyWorkspaceOverride(text, doc, packageName, newVersion, parsedPath);
            String written;
            try {
                written = workspaceEdit.apply(current);
            } catch (RuntimeException e) {
                decline(ctx, ps, path, String.valueOf(e.getMessage()));
                return;
            }
            if (written.equals(current)) {
                return;
            }
            edited = singletonMap(workspaceManifest, written);
            acc.workspaceEdits.put(workspaceManifest, workspaceEdit);
        }

        Function<Json.Document, Json.Document> edit = d ->
                PackageJsonOverrides.applyOverride(d, place, pm, packageName, newVersion, parsedPath);
        PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(pkg, edit, edited,
                ps.capturedLockContent, NodeDependencyScan.projectFiles(acc.scan, ctx, path), ctx);
        if (r.isChanged()) {
            ps.modifiedPackageJson = r.getModifiedPackageJson();
            ps.regenResult = r.getRegenResult();
            ps.editedFrom = pkg;
            ps.edit = edit;
            acc.editedFiles.put(path, r.getEditedFiles());
            regenerateMemberLocks(acc, path, pm, r.getEditedFiles(), ctx);
        }
    }

    // pnpm writes a lock per project when the workspace does not share one, and each reads the root's overrides
    private static void regenerateMemberLocks(Accumulator acc, Path root, NodeResolutionResult.PackageManager pm,
                                              Map<Path, String> edited, ExecutionContext ctx) {
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : acc.scan.projects.entrySet()) {
            Path member = project.getKey();
            NodeDependencyScan.ProjectState ps = project.getValue();
            Path lock = NodeDependencyScan.lockFor(acc.scan, member);
            if (member.equals(root) || ps.capturedPackageJson == null || ps.capturedLockContent == null ||
                ps.regenResult != null || !canApply(ps.capturedPackageJson) || lock == null ||
                !member.equals(acc.scan.lockToPackage.get(lock)) || !root.equals(workspaceRoot(acc.scan, member, pm))) {
                continue;
            }
            SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, member);
            ps.regenResult = PackageJsonHelper.regenerateLockContent(pkg == null ? ps.capturedPackageJson : pkg,
                    ps.capturedLockContent, NodeDependencyScan.projectFiles(acc.scan, ctx, member), edited, ctx);
        }
    }

    // pnpm's workspace root holds the pnpm-workspace.yaml found above packageJson; for the others, the package.json
    // whose lock an edit of packageJson lands in, else the nearest that lists it as a member
    private static Path workspaceRoot(NodeDependencyScan.Accumulator acc, Path packageJson,
                                      NodeResolutionResult.@Nullable PackageManager pm) {
        if (pm == NodeResolutionResult.PackageManager.Pnpm) {
            for (Path dir = packageJson.getParent(); ; dir = dir.getParent()) {
                Path manifest = dir == null ? Paths.get(PackageJsonOverrides.WORKSPACE_MANIFEST) :
                        dir.resolve(PackageJsonOverrides.WORKSPACE_MANIFEST);
                if (acc.paths.contains(manifest)) {
                    return manifest.resolveSibling("package.json");
                }
                if (dir == null) {
                    break;
                }
            }
        }
        Path lock = NodeDependencyScan.lockFor(acc, packageJson);
        Path root = lock == null ? null : acc.lockToPackage.get(lock);
        if (root != null) {
            return root;
        }
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : acc.projects.entrySet()) {
            SourceFile captured = project.getValue().capturedPackageJson;
            if (captured != null && !project.getKey().equals(packageJson) &&
                PackageJsonHelper.workspaceMemberPaths(captured).contains(packageJson)) {
                return project.getKey();
            }
        }
        return packageJson;
    }

    private void decline(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path path, String why) {
        if (!ps.failureRecorded) {
            ps.failureRecorded = true;
            lockRegenerationFailures.insertRow(ctx, new NodeLockRegenerationFailures.Row(path.toString(),
                    packageName, LockFileRegeneration.Reason.UNSUPPORTED_ENTRY_TYPE.toString(), why));
        }
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJsonPath) {
        if (ps.failureRecorded || ps.regenResult == null) {
            return;
        }
        ps.failureRecorded = true;
        LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, ps.regenResult,
                packageName);
    }
}
