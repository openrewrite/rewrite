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
import org.openrewrite.javascript.internal.NodeCatalogs;
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PackageJsonOverrides;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
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
                "the package manager cannot read is not written, and the reason is recorded. Not safe to use as a " +
                "precondition: consults the package registry over the network and publishes per-project state " +
                "shared with other dependency recipes.";
    }

    public static class Accumulator implements PackageJsonHelper.FileEditor {
        final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
        final UpgradeTransitiveDependencyVersion recipe;
        /** Each pnpm-workspace.yaml as scanned. */
        final Map<Path, SourceFile> workspaceManifests = new HashMap<>();
        /** Each root's override as the recipes before this one leave its files, for the recipes after it. */
        final Map<Path, PackageJsonOverrides> edits = new HashMap<>();
        final Set<NodeDependencyProtocolsSkipped.Row> protocolRows = new HashSet<>();
        final Set<NodeDependencyEditsDeclined.Row> declineRows = new HashSet<>();
        /** Each workspace root's override, decided from the revisions before this recipe it was last asked for. */
        final Map<Path, PackageJsonOverrides> overrides = new HashMap<>();
        /** The override each root's files visited so far were left by; a later one must leave them alike. */
        final Map<Path, PackageJsonOverrides> followed = new HashMap<>();
        /** Each root's files visited so far, by path: the text as visited and as this recipe left it. */
        final Map<Path, Map<Path, String[]>> left = new HashMap<>();
        final Set<Path> overturned = new HashSet<>();
        /** Why the file being visited is warned: its root's decision was overturned at this visit. */
        @Nullable String overturnedAt;
        /** What each root's regeneration last read, so it runs again only when that changed. */
        final Map<Path, List<Object>> regeneratedFrom = new HashMap<>();
        /** What each root's lock was regenerated from when this recipe left it. */
        final Map<Path, List<Object>> lockLeftFrom = new HashMap<>();
        /** The revision of each root package.json and pnpm-workspace.yaml as this recipe visited it. */
        final Map<Path, SourceFile> visited = new HashMap<>();
        final Set<Path> generated = new HashSet<>();
        /** The revisions this recipe published, told apart from those of the recipes before it. */
        final Set<SourceFile> published = Collections.newSetFromMap(new IdentityHashMap<>());

        Accumulator(UpgradeTransitiveDependencyVersion recipe) {
            this.recipe = recipe;
        }

        @Override
        public SourceFile edit(ExecutionContext ctx, SourceFile file) {
            return recipe.editedForLaterRecipes(this, ctx, file);
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(this);
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

                if (sf instanceof Yaml.Documents && PackageJsonOverrides.WORKSPACE_MANIFEST.equals(basename)) {
                    acc.workspaceManifests.put(p, sf);
                }
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
        PackageJsonHelper.addFileEditor(ctx, acc);
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
                SourceFile out = edited(acc, ctx, (SourceFile) tree);
                String overturned = acc.overturnedAt;
                acc.overturnedAt = null;
                return overturned == null ? out : warn(out, overturned);
            }
        };
    }

    private SourceFile edited(Accumulator acc, ExecutionContext ctx, SourceFile sf) {
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
            Path root = rootOf(acc, ctx, importer);
            if (root != null) {
                left(acc, root, p, sf, result != null && result.isSuccess() ? result.getLockFileContent() : print(sf));
                if (result != null && result.isSuccess() && importer.equals(root)) {
                    acc.lockLeftFrom.putIfAbsent(root, acc.regeneratedFrom.get(root));
                }
            }
            if (result != null) {
                if (result.isSuccess()) {
                    return PackageJsonHelper.reparseLock(sf, result.getLockFileContent());
                }
                recordFailure(ctx, ips, importer, result);
                return warn(sf, "lock regeneration failed: " + result.getErrorMessage());
            }
        }
        return sf;
    }

    private static void left(Accumulator acc, Path root, Path file, SourceFile visited, String leftAs) {
        acc.left.computeIfAbsent(root, k -> new LinkedHashMap<>()).put(file, new String[]{print(visited), leftAs});
    }

    // whether override leaves the root's files visited so far as they were left; a lock, only when it leaves it be
    private static boolean leavesAsLeft(Accumulator acc, Path root, PackageJsonOverrides override) {
        boolean declined = override.declined() != null;
        Path workspaceManifest = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        for (Map.Entry<Path, String[]> file : acc.left.getOrDefault(root, emptyMap()).entrySet()) {
            String visited = file.getValue()[0];
            String expected = visited;
            if (!declined && file.getKey().equals(root) && acc.visited.get(root) instanceof Json.Document) {
                expected = print(override.apply((Json.Document) acc.visited.get(root)));
            } else if (!declined && file.getKey().equals(workspaceManifest)) {
                expected = override.getPlace() == PackageJsonOverrides.Place.WORKSPACE_MANIFEST ?
                        override.applyToWorkspaceManifest() : visited;
            } else if (!declined && !file.getKey().equals(root)) {
                return false;
            }
            if (!expected.equals(file.getValue()[1])) {
                return false;
            }
        }
        return true;
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
            rootOf(acc, ctx, p);
            return sf;
        }
        acc.visited.put(p, sf);
        PackageJsonOverrides override = overrideOf(acc, ctx, p);
        if (override == null || override.apply((Json.Document) sf) == sf) {
            left(acc, p, p, sf, print(sf));
            return sf;
        }
        regenerate(acc, ctx, p, ps);
        if (ps.modifiedPackageJson == null) {
            left(acc, p, p, sf, print(sf));
            return sf;
        }
        SourceFile out = NodeDependencyScan.modifiedFor(ps, sf);
        publish(acc, ctx, p, out);
        left(acc, p, p, sf, print(out));
        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
            recordFailure(ctx, ps, p, ps.regenResult);
            return warn(out, "lock regeneration failed: " + ps.regenResult.getErrorMessage());
        }
        return out;
    }

    private SourceFile editedWorkspaceManifest(Accumulator acc, ExecutionContext ctx, SourceFile sf) {
        Path path = sf.getSourcePath();
        Path root = path.resolveSibling("package.json");
        NodeDependencyScan.ProjectState ps = acc.scan.projects.get(root);
        NodeResolutionResult.PackageManager pm = ps == null ? null : packageManager(ps.capturedPackageJson);
        if (pm != NodeResolutionResult.PackageManager.Pnpm || !root.equals(workspaceRoot(acc.scan, root, pm))) {
            return sf;
        }
        if (!acc.generated.contains(path)) {
            acc.visited.put(path, sf);
        }
        PackageJsonOverrides override = overrideOf(acc, ctx, root);
        if (override == null || override.getPlace() != PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            left(acc, root, path, sf, print(sf));
            return sf;
        }
        String content = override.applyToWorkspaceManifest();
        left(acc, root, path, sf, content);
        if (content.equals(print(sf))) {
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
        NodeDependencyScan.ProjectState rootPs = acc.scan.projects.get(root);
        if (rootPs == null || ips.capturedLockContent == null || packageManager(rootPs.capturedPackageJson) != pm ||
            !importer.equals(acc.scan.lockToPackage.get(lockPath))) {
            return null;
        }
        PackageJsonOverrides override = overrideOf(acc, ctx, importer);
        if (override == null) {
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
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc.scan, ctx, importer);
        for (Map.Entry<Path, String> manifest : otherManifests(acc, ctx, importer, files).entrySet()) {
            edited.putIfAbsent(manifest.getKey(), manifest.getValue());
        }
        ips.regenResult = PackageJsonHelper.regenerateLockContent(PackageJsonHelper.editedBefore(ctx, acc,
                requireNonNull(ips.capturedPackageJson)), ips.capturedLockContent, files, edited, ctx);
        return ips.regenResult;
    }

    // the root's lock, from the root and its members as they stand, again only when what it read has changed since
    private void regenerate(Accumulator acc, ExecutionContext ctx, Path root, NodeDependencyScan.ProjectState ps) {
        PackageJsonOverrides override = overrideOf(acc, ctx, root);
        if (override == null) {
            return;
        }
        SourceFile base = base(acc, ctx, root, ps);
        Path workspaceManifest = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        Map<Path, String> edited = workspaceEdit(acc, ctx, root, override);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc.scan, ctx, root);
        Map<Path, String> otherManifests = otherManifests(acc, ctx, root, files);
        List<Object> inputs = Arrays.asList(print(base), otherManifests, files.getPackageManagerFiles(),
                files.getPaths(), edited);
        if (inputs.equals(acc.regeneratedFrom.put(root, inputs))) {
            return;
        }
        List<Object> lockFrom = acc.lockLeftFrom.get(root);
        if (lockFrom != null && !lockFrom.equals(inputs) && acc.overturned.add(root)) {
            String why = "a recipe before this one changed " + root + " or a file its lock reads after this recipe " +
                         "wrote the lock from their earlier revisions";
            acc.overturnedAt = why;
            failure(ctx, ps, root, why);
        }
        Map<Path, String> read = new LinkedHashMap<>(edited);
        if (!(base instanceof Json.Document) || override.apply((Json.Document) base) != base || !edited.isEmpty()) {
            read.putAll(otherManifests);
        }
        PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(base,
                override::apply, read, singletonMap(workspaceManifest, workspaceBase(acc, ctx, workspaceManifest)),
                ps.capturedLockContent, files, ctx);
        ps.modifiedPackageJson = r.getModifiedPackageJson();
        ps.regenResult = r.getRegenResult();
        ps.editedFrom = base;
        ps.edit = override::apply;
        String written = edited.get(workspaceManifest);
        if (written != null && !acc.visited.containsKey(workspaceManifest) &&
            !acc.generated.contains(workspaceManifest)) {
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
        String written = override.applyToWorkspaceManifest();
        return written.equals(workspaceBase(acc, ctx, path)) ? emptyMap() : singletonMap(path, written);
    }

    private @Nullable String workspaceManifest(Accumulator acc, ExecutionContext ctx, Path root) {
        PackageJsonOverrides override = overrideOf(acc, ctx, root);
        if (override == null || override.getPlace() != PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            return null;
        }
        Path path = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
        return workspaceEdit(acc, ctx, root, override).get(path);
    }

    // the revision of pnpm-workspace.yaml before this recipe's edit: as visited, else as the recipes before it left it
    private static @Nullable String workspaceBase(Accumulator acc, ExecutionContext ctx, Path path) {
        if (acc.generated.contains(path)) {
            return null;
        }
        SourceFile visited = acc.visited.get(path);
        if (visited != null) {
            return print(visited);
        }
        SourceFile scanned = acc.workspaceManifests.get(path);
        return scanned != null ? print(PackageJsonHelper.editedBefore(ctx, acc, scanned)) :
                acc.scan.packageManagerFiles.get(path);
    }

    // the root's package.json before this recipe's edit: as visited, else as the recipes before it left it
    private static SourceFile base(Accumulator acc, ExecutionContext ctx, Path root,
                                   NodeDependencyScan.ProjectState ps) {
        SourceFile visited = acc.visited.get(root);
        return visited != null ? visited : PackageJsonHelper.editedBefore(ctx, acc,
                requireNonNull(ps.capturedPackageJson));
    }

    // the lock's other manifests a regeneration for packageJson reads, as the recipes before this one leave them
    private static Map<Path, String> otherManifests(Accumulator acc, ExecutionContext ctx, Path packageJson,
                                                   NodeDependencyScan.ProjectFiles files) {
        Set<Path> manifests = new LinkedHashSet<>(files.getManifests().keySet());
        manifests.addAll(files.getPackageManagerFiles().keySet());
        Map<Path, String> out = new LinkedHashMap<>();
        for (Path manifest : manifests) {
            NodeDependencyScan.ProjectState ps = acc.scan.projects.get(manifest);
            if (!manifest.equals(packageJson) && ps != null && ps.capturedPackageJson != null) {
                out.put(manifest, print(PackageJsonHelper.editedBefore(ctx, acc, ps.capturedPackageJson)));
            }
        }
        return out;
    }

    private static void publish(Accumulator acc, ExecutionContext ctx, Path path, SourceFile revision) {
        acc.published.add(revision);
        PackageJsonHelper.putLiveTree(ctx, path, revision);
    }

    // the override of packageJson's workspace root, from the revisions before this recipe of the root and its
    // pnpm-workspace.yaml; null when declined, or when they overturn the decision files were written by
    private @Nullable PackageJsonOverrides overrideOf(Accumulator acc, ExecutionContext ctx, Path packageJson) {
        Path root = rootOf(acc, ctx, packageJson);
        if (root == null) {
            return null;
        }
        NodeDependencyScan.ProjectState ps = requireNonNull(acc.scan.projects.get(root));
        PackageJsonOverrides override = decide(acc, ctx, root, ps);
        if (override == null) {
            return null;
        }
        PackageJsonOverrides followed = acc.followed.computeIfAbsent(root, k -> override);
        if (!followed.writesAs(override) && leavesAsLeft(acc, root, override)) {
            acc.followed.put(root, override);
        } else if (!followed.writesAs(override)) {
            if (acc.overturned.add(root)) {
                String why = "a recipe before this one changed " + root + " or its " +
                             PackageJsonOverrides.WORKSPACE_MANIFEST + " after files visited earlier were left by " +
                             "the override's place then, " + followed + "; it is now " + override;
                acc.overturnedAt = why;
                failure(ctx, ps, root, why);
            }
            return null;
        }
        String declined = override.declined();
        if (declined != null) {
            decline(acc, ctx, root, declined);
            return null;
        }
        if (writes(acc, ctx, root, ps, override)) {
            List<NodeDependencyProtocolsSkipped.Row> references = references(acc, ctx, root,
                    base(acc, ctx, root, ps));
            for (NodeDependencyProtocolsSkipped.Row row : references) {
                if (acc.protocolRows.add(row)) {
                    protocolsSkipped.insertRow(ctx, row);
                }
            }
            if (!references.isEmpty()) {
                return null;
            }
            String outranked = override.outranked();
            if (outranked != null) {
                decline(acc, ctx, root, outranked);
            }
        }
        return override;
    }

    // whether the override changes the file it is written in
    private static boolean writes(Accumulator acc, ExecutionContext ctx, Path root,
                                  NodeDependencyScan.ProjectState ps, PackageJsonOverrides override) {
        if (override.getPlace() == PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            return !override.applyToWorkspaceManifest().equals(workspaceBase(acc, ctx,
                    root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST)));
        }
        SourceFile pkg = base(acc, ctx, root, ps);
        return pkg instanceof Json.Document && override.apply((Json.Document) pkg) != pkg;
    }

    // the declarations the override would win over unnamed: packageName through a value that is no version range,
    // in the root or a member it governs, or for an override scoped to a member, in that member
    private List<NodeDependencyProtocolsSkipped.Row> references(Accumulator acc, ExecutionContext ctx, Path root,
                                                                SourceFile rootRevision) {
        List<DependencyPathSegment> path = dependencyPath == null ? null :
                PackageJsonOverrides.parsePath(dependencyPath);
        String parent = path == null || path.isEmpty() ? null : path.get(path.size() - 1).getName();
        NodeResolutionResult.PackageManager pm = packageManager(rootRevision);
        List<NodeDependencyProtocolsSkipped.Row> rows = new ArrayList<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project :
                new TreeMap<>(acc.scan.projects).entrySet()) {
            SourceFile captured = project.getValue().capturedPackageJson;
            if (captured == null || !root.equals(workspaceRoot(acc.scan, project.getKey(), pm))) {
                continue;
            }
            SourceFile manifest = project.getKey().equals(root) ? rootRevision :
                    PackageJsonHelper.editedBefore(ctx, acc, captured);
            if (parent != null && (project.getKey().equals(root) ||
                                   !parent.equals(PackageJsonHelper.declaredName(manifest)))) {
                continue;
            }
            for (Map.Entry<String, Map<String, String>> scope :
                    PackageJsonHelper.declaredDependencies(manifest).entrySet()) {
                String value = scope.getValue().get(packageName);
                Spec spec = value == null ? null : NodeCatalogs.classify(packageName, value);
                if (value != null && (spec == null || !spec.isVersionSpec())) {
                    rows.add(new NodeDependencyProtocolsSkipped.Row(root.toString(), project.getKey().toString(),
                            packageName, scope.getKey(), spec == null || spec.getProtocol() == null ? "" :
                            spec.getProtocol(), value, newVersion));
                }
            }
        }
        return rows;
    }

    // file as the recipes before this one leave it, with this recipe's override, for the recipes after it
    private SourceFile editedForLaterRecipes(Accumulator acc, ExecutionContext ctx, SourceFile file) {
        Path p = file.getSourcePath();
        boolean workspaceManifest = PackageJsonOverrides.WORKSPACE_MANIFEST.equals(String.valueOf(p.getFileName()));
        Path root = workspaceManifest ? p.resolveSibling("package.json") : p;
        NodeDependencyScan.ProjectState ps = acc.scan.projects.get(root);
        NodeResolutionResult.PackageManager pm = ps == null ? null : packageManager(ps.capturedPackageJson);
        if (pm == null || !root.equals(workspaceRoot(acc.scan, root, pm)) ||
            !workspaceManifest && !(file instanceof Json.Document)) {
            return file;
        }
        SourceFile pkg = workspaceManifest ? PackageJsonHelper.editedBefore(ctx, acc,
                requireNonNull(ps.capturedPackageJson)) : file;
        String workspace = null;
        if (pm == NodeResolutionResult.PackageManager.Pnpm) {
            Path path = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
            SourceFile scanned = acc.workspaceManifests.get(path);
            if (workspaceManifest) {
                workspace = print(file);
            } else if (scanned != null) {
                workspace = print(PackageJsonHelper.editedBefore(ctx, acc, scanned));
            } else if (acc.scan.paths.contains(path)) {
                return file;
            }
        }
        if (!(pkg instanceof Json.Document)) {
            return file;
        }
        PackageJsonOverrides last = acc.edits.get(root);
        PackageJsonOverrides override = last != null ? last.from((Json.Document) pkg, workspace) :
                PackageJsonOverrides.of(pm, (Json.Document) pkg, workspace, packageName, newVersion,
                        dependencyPath == null ? null : PackageJsonOverrides.parsePath(dependencyPath));
        acc.edits.put(root, override);
        if (override.declined() != null || !references(acc, ctx, root, pkg).isEmpty()) {
            return file;
        }
        if (!workspaceManifest) {
            return override.apply((Json.Document) file);
        }
        if (override.getPlace() != PackageJsonOverrides.Place.WORKSPACE_MANIFEST) {
            return file;
        }
        String content = override.applyToWorkspaceManifest();
        return content.equals(workspace) ? file : PackageJsonHelper.reparseLock(file, content);
    }

    // packageJson's workspace root; null, with a row, when that is not a package.json the recipe edits
    private @Nullable Path rootOf(Accumulator acc, ExecutionContext ctx, Path packageJson) {
        NodeDependencyScan.ProjectState ps = acc.scan.projects.get(packageJson);
        NodeResolutionResult.PackageManager pm = ps == null ? null : packageManager(ps.capturedPackageJson);
        if (pm == null) {
            return null;
        }
        Path root = workspaceRoot(acc.scan, packageJson, pm);
        NodeDependencyScan.ProjectState rootPs = acc.scan.projects.get(root);
        if (rootPs == null || packageManager(rootPs.capturedPackageJson) == null) {
            decline(acc, ctx, packageJson, "the override belongs in the workspace root's " + root +
                                           ", which is not a package.json the recipe edits");
            return null;
        }
        return root;
    }

    // the root's override as its package.json and pnpm-workspace.yaml stand before this recipe, decided again when
    // either has changed since it was last asked for
    private @Nullable PackageJsonOverrides decide(Accumulator acc, ExecutionContext ctx, Path root,
                                                  NodeDependencyScan.ProjectState ps) {
        SourceFile pkg = base(acc, ctx, root, ps);
        NodeResolutionResult.PackageManager pm = packageManager(ps.capturedPackageJson);
        if (!(pkg instanceof Json.Document) || pm == null) {
            return null;
        }
        String workspace = null;
        if (pm == NodeResolutionResult.PackageManager.Pnpm) {
            Path workspaceManifest = root.resolveSibling(PackageJsonOverrides.WORKSPACE_MANIFEST);
            if (acc.scan.paths.contains(workspaceManifest) &&
                !acc.scan.packageManagerFiles.containsKey(workspaceManifest)) {
                failure(ctx, ps, root, LockOnlyInstall.PNPM.failure(new CannotKnow(
                        PackageJsonOverrides.WORKSPACE_MANIFEST + " (its contents were not read)")));
                return null;
            }
            workspace = workspaceBase(acc, ctx, workspaceManifest);
        }
        PackageJsonOverrides last = acc.overrides.get(root);
        PackageJsonOverrides override = last != null ? last.from((Json.Document) pkg, workspace) :
                PackageJsonOverrides.of(pm, (Json.Document) pkg, workspace, packageName, newVersion,
                        dependencyPath == null ? null : PackageJsonOverrides.parsePath(dependencyPath));
        acc.overrides.put(root, override);
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

    private void failure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path path, String why) {
        failure(ctx, ps, path, new LockFileRegeneration.Failure(LockFileRegeneration.Kind.CANNOT_KNOW,
                LockFileRegeneration.Reason.RESOLUTION_REQUIRED, null, why));
    }

    private void failure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path path,
                         LockFileRegeneration.Failure failure) {
        recordFailure(ctx, ps, path, LockFileRegeneration.Result.failure(failure));
    }

    private void decline(Accumulator acc, ExecutionContext ctx, Path path, String why) {
        NodeDependencyEditsDeclined.Row row = new NodeDependencyEditsDeclined.Row(path.toString(), packageName, why);
        if (acc.declineRows.add(row)) {
            editsDeclined.insertRow(ctx, row);
        }
    }

    private static <T extends Tree> T warn(T tree, String message) {
        return Markup.markup(tree, new Markup.Warn(Tree.randomId(), message, null));
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJsonPath,
                               LockFileRegeneration.Result result) {
        if (!ps.failureRecorded) {
            ps.failureRecorded = true;
            LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, result, packageName);
        }
    }
}
