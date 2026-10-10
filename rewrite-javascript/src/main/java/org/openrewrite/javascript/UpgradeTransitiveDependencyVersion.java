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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

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
        /** Each workspace root's override, decided once; null when declined. */
        final Map<Path, @Nullable PackageJsonOverrides> overrides = new HashMap<>();
        /** What each root's regeneration last read, so it runs again only when that changed. */
        final Map<Path, List<Object>> regeneratedFrom = new HashMap<>();
        /** The revision of each root package.json and pnpm-workspace.yaml as this recipe visited it. */
        final Map<Path, SourceFile> visited = new HashMap<>();
        final Set<Path> generated = new HashSet<>();
        /** The revisions this recipe published, told apart from those of the recipes before it. */
        final Set<SourceFile> published = Collections.newSetFromMap(new IdentityHashMap<>());
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
            Path root = project.getKey();
            Path workspaceManifest = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
            if (packageManager(project.getValue().capturedPackageJson) != NodeResolutionResult.PackageManager.Pnpm ||
                acc.scan.paths.contains(workspaceManifest) ||
                !root.equals(workspaceRoot(acc.scan, root, NodeResolutionResult.PackageManager.Pnpm))) {
                continue;
            }
            String content = workspaceManifest(acc, ctx, root);
            if (content != null) {
                acc.generated.add(workspaceManifest);
                YamlParser.builder().build().parse(ctx, content).findFirst()
                        .ifPresent(yaml -> created.add(yaml.withSourcePath(workspaceManifest)));
            }
        }
        return created;
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
                    return editedWorkspaceManifest(acc, ctx, sf);
                }

                NodeDependencyScan.ProjectState ps = acc.scan.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    return editedManifest(acc, ctx, ps, sf);
                }

                for (Path importer : NodeDependencyScan.lockImporters(acc.scan, p)) {
                    NodeDependencyScan.ProjectState ips = acc.scan.projects.get(importer);
                    if (ips == null || ips.capturedPackageJson == null) continue;
                    LockFileRegeneration.Result result = regeneratedLock(acc, ctx, importer, ips, p);
                    if (result != null) {
                        if (result.isSuccess()) {
                            return PackageJsonHelper.reparseLock(sf, result.getLockFileContent());
                        }
                        recordFailure(ctx, ips, importer, result);
                        return Markup.warn(sf, new RuntimeException(
                                "lock regeneration failed: " + result.getErrorMessage()));
                    }
                }
                return tree;
            }
        };
    }

    private SourceFile editedManifest(Accumulator acc, ExecutionContext ctx, NodeDependencyScan.ProjectState ps,
                                      SourceFile sf) {
        Path p = sf.getSourcePath();
        NodeResolutionResult.PackageManager pm = packageManager(sf);
        if (pm == null || !(sf instanceof Json.Document)) {
            return sf;
        }
        Path root = workspaceRoot(acc.scan, p, pm);
        if (!root.equals(p)) {
            overrideOf(acc, ctx, p);
            return sf;
        }
        acc.visited.put(p, sf);
        PackageJsonOverrides override = overrideOf(acc, ctx, p);
        if (override == null || override.apply((Json.Document) sf) == sf) {
            return sf;
        }
        regenerate(acc, ctx, p, ps);
        if (ps.modifiedPackageJson == null) {
            return sf;
        }
        SourceFile out = NodeDependencyScan.modifiedFor(ps, sf);
        publish(acc, ctx, p, out);
        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
            recordFailure(ctx, ps, p, ps.regenResult);
            return Markup.warn(out, new RuntimeException("lock regeneration failed: " +
                                                         ps.regenResult.getErrorMessage()));
        }
        return out;
    }

    private SourceFile editedWorkspaceManifest(Accumulator acc, ExecutionContext ctx, SourceFile sf) {
        Path path = sf.getSourcePath();
        Path root = path.resolveSibling("package.json");
        NodeDependencyScan.ProjectState ps = acc.scan.projects.get(root);
        if (ps == null || ps.capturedPackageJson == null) {
            return sf;
        }
        if (!acc.generated.contains(path)) {
            acc.visited.put(path, sf);
        }
        PackageJsonOverrides override = overrideOf(acc, ctx, root);
        if (override == null || override.getPlace() != PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            return sf;
        }
        String text = print(sf);
        String content;
        try {
            content = override.applyToWorkspaceManifest(text);
        } catch (IllegalArgumentException e) {
            return Markup.warn(sf, e);
        }
        if (content.equals(text)) {
            return sf;
        }
        SourceFile out = PackageJsonHelper.reparseLock(sf, content);
        publish(acc, ctx, path, out);
        return out;
    }

    // the lock lockPath as importer's edit leaves it: the root's own lock, or the lock of a member that keeps its own
    private LockFileRegeneration.@Nullable Result regeneratedLock(Accumulator acc, ExecutionContext ctx, Path importer,
                                                                 NodeDependencyScan.ProjectState ips, Path lockPath) {
        NodeResolutionResult.PackageManager pm = packageManager(ips.capturedPackageJson);
        if (pm == null) {
            return null;
        }
        Path root = workspaceRoot(acc.scan, importer, pm);
        if (root.equals(importer)) {
            regenerate(acc, ctx, root, ips);
            if (ips.modifiedPackageJson != null && !acc.visited.containsKey(root)) {
                publish(acc, ctx, root, ips.modifiedPackageJson);
            }
            return ips.regenResult;
        }
        PackageJsonOverrides override = overrideOf(acc, ctx, importer);
        NodeDependencyScan.ProjectState rootPs = acc.scan.projects.get(root);
        if (override == null || rootPs == null || ips.capturedLockContent == null ||
            !importer.equals(acc.scan.lockToPackage.get(lockPath)) ||
            packageManager(rootPs.capturedPackageJson) != pm) {
            return null;
        }
        // pnpm writes a lock per project when the workspace does not share one, and each reads the root's overrides
        Map<Path, String> edited = new LinkedHashMap<>(workspaceEdit(acc, ctx, root, override));
        SourceFile rootBase = base(acc, ctx, root, rootPs);
        if (rootBase instanceof Json.Document && override.apply((Json.Document) rootBase) != rootBase) {
            edited.put(root, print(override.apply((Json.Document) rootBase)));
        }
        if (edited.isEmpty()) {
            return null;
        }
        SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, importer);
        ips.regenResult = PackageJsonHelper.regenerateLockContent(pkg == null ? ips.capturedPackageJson : pkg,
                ips.capturedLockContent, NodeDependencyScan.projectFiles(acc.scan, ctx, importer), edited, ctx);
        return ips.regenResult;
    }

    // the root's lock, from the root and its members as they stand, again only when what it read has changed since
    private void regenerate(Accumulator acc, ExecutionContext ctx, Path root, NodeDependencyScan.ProjectState ps) {
        PackageJsonOverrides override = overrideOf(acc, ctx, root);
        if (override == null) {
            return;
        }
        SourceFile base = base(acc, ctx, root, ps);
        Map<Path, String> edited = workspaceEdit(acc, ctx, root, override);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc.scan, ctx, root);
        Map<Path, String> otherManifests = new HashMap<>(files.getManifests());
        otherManifests.remove(root);
        List<Object> inputs = Arrays.asList(print(base), otherManifests, files.getPackageManagerFiles(),
                files.getPaths(), edited);
        if (inputs.equals(acc.regeneratedFrom.put(root, inputs))) {
            return;
        }
        PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(base,
                override::apply, edited, ps.capturedLockContent, files, ctx);
        ps.modifiedPackageJson = r.getModifiedPackageJson();
        ps.regenResult = r.getRegenResult();
        ps.editedFrom = base;
        ps.edit = override::apply;
        Path workspaceManifest = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        String written = edited.get(workspaceManifest);
        if (written != null && !acc.visited.containsKey(workspaceManifest) && !acc.generated.contains(workspaceManifest)) {
            YamlParser.builder().build().parse(ctx, written).findFirst().ifPresent(yaml ->
                    publish(acc, ctx, workspaceManifest, yaml.withSourcePath(workspaceManifest)));
        }
    }

    // pnpm-workspace.yaml with the override over its revision before this recipe, when that changes it
    private Map<Path, String> workspaceEdit(Accumulator acc, ExecutionContext ctx, Path root,
                                           PackageJsonOverrides override) {
        if (override.getPlace() != PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            return emptyMap();
        }
        Path path = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        String before = workspaceBase(acc, ctx, path);
        try {
            String written = override.applyToWorkspaceManifest(before);
            return written.equals(before) ? emptyMap() : singletonMap(path, written);
        } catch (IllegalArgumentException e) {
            return emptyMap();
        }
    }

    private @Nullable String workspaceManifest(Accumulator acc, ExecutionContext ctx, Path root) {
        PackageJsonOverrides override = overrideOf(acc, ctx, root);
        if (override == null || override.getPlace() != PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            return null;
        }
        Path path = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        return workspaceEdit(acc, ctx, root, override).get(path);
    }

    // the revision of pnpm-workspace.yaml before this recipe's edit: as visited, else as an earlier recipe left it
    private static @Nullable String workspaceBase(Accumulator acc, ExecutionContext ctx, Path path) {
        if (acc.generated.contains(path)) {
            return null;
        }
        SourceFile visited = acc.visited.get(path);
        if (visited != null) {
            return print(visited);
        }
        SourceFile live = PackageJsonHelper.getLiveTree(ctx, path);
        return live != null && !acc.published.contains(live) ? print(live) : acc.scan.packageManagerFiles.get(path);
    }

    // the root's package.json before this recipe's edit: as visited, else as an earlier recipe left it, else scanned
    private static SourceFile base(Accumulator acc, ExecutionContext ctx, Path root,
                                   NodeDependencyScan.ProjectState ps) {
        SourceFile visited = acc.visited.get(root);
        if (visited != null) {
            return visited;
        }
        SourceFile live = PackageJsonHelper.getLiveTree(ctx, root);
        return live != null && !acc.published.contains(live) ? live : requireNonNull(ps.capturedPackageJson);
    }

    private static void publish(Accumulator acc, ExecutionContext ctx, Path path, SourceFile revision) {
        acc.published.add(revision);
        PackageJsonHelper.putLiveTree(ctx, path, revision);
    }

    // the override of the workspace root of packageJson, decided once from the root's revision before this recipe
    private @Nullable PackageJsonOverrides overrideOf(Accumulator acc, ExecutionContext ctx, Path packageJson) {
        NodeDependencyScan.ProjectState ps = acc.scan.projects.get(packageJson);
        NodeResolutionResult.PackageManager pm = ps == null ? null : packageManager(ps.capturedPackageJson);
        if (pm == null) {
            return null;
        }
        Path root = workspaceRoot(acc.scan, packageJson, pm);
        NodeDependencyScan.ProjectState rootPs = acc.scan.projects.get(root);
        if (rootPs == null || packageManager(rootPs.capturedPackageJson) == null) {
            decline(ctx, ps, packageJson, "the override belongs in the workspace root's " + root +
                                          ", which is not a package.json the recipe edits");
            return null;
        }
        if (!acc.overrides.containsKey(root)) {
            acc.overrides.put(root, decide(acc, ctx, root, rootPs));
        }
        return acc.overrides.get(root);
    }

    private @Nullable PackageJsonOverrides decide(Accumulator acc, ExecutionContext ctx, Path root,
                                                  NodeDependencyScan.ProjectState ps) {
        SourceFile pkg = base(acc, ctx, root, ps);
        NodeResolutionResult.PackageManager pm = packageManager(ps.capturedPackageJson);
        if (!(pkg instanceof Json.Document) || pm == null) {
            return null;
        }
        Path workspaceManifest = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        String current = acc.scan.packageManagerFiles.get(workspaceManifest);
        if (pm == NodeResolutionResult.PackageManager.Pnpm && current == null &&
            acc.scan.paths.contains(workspaceManifest)) {
            ps.failureRecorded = true;
            lockRegenerationFailures.insertRow(ctx, new NodeLockRegenerationFailures.Row(root.toString(),
                    packageName, LockFileRegeneration.Reason.RESOLUTION_REQUIRED.toString(),
                    "the engine cannot know " + PackageJsonOverrides.WORKSPACE_MANIFEST +
                    " (its contents were not read)"));
            return null;
        }
        List<DependencyPathSegment> path = dependencyPath == null ? null :
                PackageJsonOverrides.parsePath(dependencyPath);
        PackageJsonOverrides override = PackageJsonOverrides.of(pm, (Json.Document) pkg, current, packageName,
                newVersion, path);
        String declined = override.declined();
        if (declined == null && override.getPlace() == PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            try {
                override.applyToWorkspaceManifest(current);
            } catch (IllegalArgumentException e) {
                declined = String.valueOf(e.getMessage());
            }
        }
        if (declined != null) {
            decline(ctx, ps, root, declined);
            return null;
        }
        return override;
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

    private static NodeResolutionResult.@Nullable PackageManager packageManager(@Nullable SourceFile pkg) {
        NodeResolutionResult marker = pkg == null ? null :
                pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        return marker == null ? null : marker.getPackageManager();
    }

    private static String print(SourceFile sf) {
        return sf.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    private void decline(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path path, String why) {
        if (!ps.failureRecorded) {
            ps.failureRecorded = true;
            lockRegenerationFailures.insertRow(ctx, new NodeLockRegenerationFailures.Row(path.toString(),
                    packageName, LockFileRegeneration.Reason.UNSUPPORTED_ENTRY_TYPE.toString(), why));
        }
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJsonPath,
                               LockFileRegeneration.Result result) {
        if (!ps.failureRecorded) {
            ps.failureRecorded = true;
            LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, result, packageName);
        }
    }
}
