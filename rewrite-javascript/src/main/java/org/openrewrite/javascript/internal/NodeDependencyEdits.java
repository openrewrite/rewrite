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
package org.openrewrite.javascript.internal;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.pnpm.FindPackages;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

/**
 * The dependency recipes' one engine: the scan, each manifest's decision this cycle (made in generate from the
 * documents as the earlier recipes leave them, and again at a visit that finds one otherwise), and what it writes.
 */
public final class NodeDependencyEdits implements PackageJsonHelper.FileEditor {
    public final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
    private final Rows rows;
    private final Set<Object> inserted = new HashSet<>();
    private final Set<Path> failed = new HashSet<>();
    private final Map<Path, SourceFile> yamlFiles = new HashMap<>();
    private final Map<Path, @Nullable ManagedFile> managed = new HashMap<>();
    private final Map<Path, @Nullable ManagedFile> managedOf = new HashMap<>();
    private final Map<Path, Decision> decisions = new LinkedHashMap<>();
    private final Map<Path, SourceFile> visited = new HashMap<>();
    private final Map<Path, String> left = new LinkedHashMap<>();
    private final Map<Path, Regeneration> regenerations = new HashMap<>();
    private final Set<Path> warnedStale = new HashSet<>();
    private final Set<Path> generated = new HashSet<>();
    private final Map<SourceFile, String> prints = new IdentityHashMap<>();
    private final List<String> pending = new ArrayList<>();
    private @Nullable Decider decider;
    private int version;

    public NodeDependencyEdits(Rows rows) {
        this.rows = rows;
    }

    /**
     * The rows a recipe writes: a failed regeneration (named after {@code packageName} when it names no package) and a
     * declined edit.
     */
    public static Rows rows(NodeLockRegenerationFailures failures, NodeDependencyEditsDeclined declines,
                            @Nullable String packageName) {
        return new Rows() {
            @Override
            public void failure(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Failure failure) {
                failures.insertRow(ctx, LockFileRegeneration.failureRow(packageJson, failure, packageName));
            }

            @Override
            public void declined(ExecutionContext ctx, Path path, String reason) {
                declines.insertRow(ctx, new NodeDependencyEditsDeclined.Row(path.toString(),
                        packageName == null ? "" : packageName, reason));
            }
        };
    }

    /**
     * Note what a dependency recipe's scan reads from {@code sf}.
     */
    public void scan(SourceFile sf) {
        Path p = sf.getSourcePath();
        String basename = String.valueOf(p.getFileName());
        if (sf instanceof Yaml.Documents && (basename.equals(NodeCatalogs.fileName(PackageManager.Pnpm)) ||
                                            basename.equals(NodeCatalogs.fileName(PackageManager.YarnBerry)))) {
            yamlFiles.put(p, sf);
        }
        if (NodeDependencyScan.capturePackageManagerFile(scan, sf)) {
            return;
        }
        if (PackageJsonHelper.isLockFile(basename)) {
            if (sf instanceof Json.Document || sf instanceof Yaml.Documents || sf instanceof PlainText) {
                NodeDependencyScan.captureLock(scan, sf);
            }
            return;
        }
        if (sf instanceof Json.Document && "package.json".equals(basename) &&
            sf.getMarkers().findFirst(NodeResolutionResult.class).isPresent()) {
            scan.projects.computeIfAbsent(p, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson = sf;
        }
    }

    /**
     * Decide every scanned manifest's edit; the package-manager files the decisions create.
     */
    public List<SourceFile> decideAll(ExecutionContext ctx, Decider decider) {
        this.decider = decider;
        PackageJsonHelper.addFileEditor(ctx, this);
        NodeDependencyScan.linkWorkspaceMembers(scan);
        for (Path project : new TreeMap<>(scan.projects).keySet()) {
            if (captured(project) != null) {
                decide(ctx, project, null);
            }
        }
        List<SourceFile> created = new ArrayList<>();
        for (ManagedFile file : managed.values()) {
            if (file != null && file.created) {
                NodeCatalogs output = output(file, current(ctx, file));
                if (output != null && !print(output.getDocument()).isEmpty()) {
                    generated.add(file.path);
                    created.add(output.getDocument());
                }
            }
        }
        return created;
    }

    /**
     * The catalogs the package manager of {@code packageJson} reads for it, as the recipes before this one left
     * them, with this cycle's edits.
     */
    public @Nullable NodeCatalogs of(ExecutionContext ctx, Path packageJson) {
        ManagedFile file = managedOf(packageJson);
        return file == null ? null : current(ctx, file);
    }

    /**
     * {@code packageJson} as the recipes before this one in the cycle leave it, as scanned.
     */
    public @Nullable SourceFile editedBefore(ExecutionContext ctx, Path packageJson) {
        SourceFile captured = captured(packageJson);
        return captured == null ? null : PackageJsonHelper.editedBefore(ctx, this, captured);
    }

    /**
     * Whether the scan holds {@code packageJson} as a manifest the recipes edit.
     */
    public boolean holds(Path packageJson) {
        return captured(packageJson) != null;
    }

    /**
     * A manifest or package-manager file with this cycle's edits, and pnpm's cleanup of the catalogs they leave unused.
     */
    @Override
    public SourceFile edit(ExecutionContext ctx, SourceFile file) {
        ManagedFile managedFile = managed.get(file.getSourcePath());
        if (managedFile != null && managedFile.editedFrom == file && managedFile.editedAt == version) {
            return requireNonNull(managedFile.edited);
        }
        SourceFile edited = editedManifest(file);
        if (managedFile != null && !(managedFile.scanned instanceof NodeCatalogs.Bun)) {
            NodeCatalogs out = output(managedFile, managedFile.replay(managedFile.read(edited)));
            managedFile.editedFrom = file;
            managedFile.editedAt = version;
            managedFile.edited = out == null ? edited : out.getDocument();
            return managedFile.edited;
        }
        return edited;
    }

    /**
     * The visitor writing this cycle's decisions: package-manager files edited in place, manifests edited or marked
     * declined, and each lock regenerated from its importers as edited.
     */
    public TreeVisitor<Tree, ExecutionContext> visitor(Decider decider) {
        this.decider = decider;
        NodeDependencyScan.linkWorkspaceMembers(scan);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();
                pending.clear();
                SourceFile out;
                if (managed.get(p) != null && captured(p) == null) {
                    out = visitManaged(ctx, requireNonNull(managed.get(p)), sf);
                } else if (captured(p) != null) {
                    out = visitManifest(ctx, sf);
                } else if (scan.lockToPackage.containsKey(p)) {
                    out = visitLock(ctx, sf);
                } else {
                    return tree;
                }
                for (String warning : pending) {
                    out = warn(out, warning);
                }
                return out;
            }
        };
    }

    private SourceFile visitManaged(ExecutionContext ctx, ManagedFile file, SourceFile sf) {
        Path p = sf.getSourcePath();
        if (!generated.contains(p)) {
            visited.put(p, sf);
            for (Path manifest : new ArrayList<>(decisions.keySet())) {
                if (managedOf(manifest) == file) {
                    decide(ctx, manifest, p);
                }
            }
        }
        SourceFile out = managedOut(ctx, file, sf);
        left.put(p, print(out));
        return print(out).equals(print(sf)) ? sf : out;
    }

    private SourceFile visitManifest(ExecutionContext ctx, SourceFile sf) {
        Path p = sf.getSourcePath();
        visited.put(p, sf);
        decide(ctx, p, p);
        Decision decision = decisions.get(p);
        SourceFile out = sf;
        if (decision != null && !decision.withdrawn && (decision.manifestEdit != null || holdsCatalogEdits(p))) {
            PackageJsonHelper.EditAndRegenerateResult r = regeneration(ctx, p).result;
            if (r.getModifiedPackageJson() != null) {
                out = r.getModifiedPackageJson();
            }
            if (r.getRegenResult() != null && !r.getRegenResult().isSuccess()) {
                failure(ctx, p, requireNonNull(r.getRegenResult().getFailure()));
                out = warn(out, "lock regeneration failed: " + r.getRegenResult().getErrorMessage());
            }
        }
        for (String warning : decision == null ? Collections.<String>emptySet() : decision.warnings) {
            out = warn(out, warning);
        }
        left.put(p, print(out));
        return out;
    }

    private SourceFile visitLock(ExecutionContext ctx, SourceFile sf) {
        Path p = sf.getSourcePath();
        visited.put(p, sf);
        Path importer = importerOf(ctx, NodeDependencyScan.lockImporters(scan, p));
        LockFileRegeneration.Result result = importer == null ? null :
                regeneration(ctx, importer).result.getRegenResult();
        SourceFile out = sf;
        if (result != null && result.isSuccess()) {
            out = PackageJsonHelper.reparseLock(sf, requireNonNull(result.getLockFileContent()));
        } else if (result != null) {
            failure(ctx, requireNonNull(importer), requireNonNull(result.getFailure()));
            out = warn(sf, "lock regeneration failed: " + result.getErrorMessage());
        }
        left.put(p, print(out));
        return out;
    }

    // the importer whose regeneration the lock takes: the first that edits its own manifest, else the last whose
    // regeneration reads a file this cycle's decisions edit
    private @Nullable Path importerOf(ExecutionContext ctx, List<Path> importers) {
        for (Path importer : importers) {
            Decision decision = decisions.get(importer);
            if (captured(importer) != null && decision != null && !decision.withdrawn &&
                (decision.manifestEdit != null || holdsCatalogEdits(importer))) {
                return importer;
            }
        }
        for (int i = importers.size() - 1; i >= 0; i--) {
            Path importer = importers.get(i);
            if (captured(importer) != null && regeneration(ctx, importer).result.getRegenResult() != null) {
                return importer;
            }
        }
        return null;
    }

    // decide path's edit from the revisions of its manifest and package-manager file before this recipe; at a visit
    // that finds them otherwise, the new decision stands unless a file written already says otherwise
    private void decide(ExecutionContext ctx, Path path, @Nullable Path changed) {
        SourceFile manifest = before(ctx, path);
        if (!(manifest instanceof Json.Document)) {
            return;
        }
        ManagedFile file = managedOf(path);
        SourceFile base = file == null ? null : base(ctx, file);
        Decision previous = decisions.get(path);
        if (previous != null && (previous.withdrawn ||
                                 same(previous.manifest, manifest) && same(previous.base, base))) {
            return;
        }
        Map<Path, String> previousOutputs = previous == null ? emptyMap() : outputs(ctx, previous);
        Decision decision = new Decision(this, path, (Json.Document) manifest, base, ctx);
        install(decision);
        requireNonNull(decider).decide(decision, ctx);
        ManagedFile edited = decision.edits() ? managedOf(path) : null;
        String cleanupDeclined = edited == null ? null : cleanupDeclined(edited);
        if (cleanupDeclined != null) {
            decision.manifestEdit = null;
            decision.fileEdits.clear();
            decision.warn(cleanupDeclined);
            decision.decline(cleanupDeclined);
        }
        rebuild();
        if (previous == null || changed == null) {
            return;
        }
        List<Path> otherwise = leftOtherwise(ctx);
        if (otherwise.isEmpty()) {
            return;
        }
        if (previousOutputs.equals(outputs(ctx, decision))) {
            for (Path lock : otherwise) {
                if (scan.lockToPackage.containsKey(lock) && warnedStale.add(lock)) {
                    cannotKnow(ctx, path, "a recipe before this one changed " + changed + " after this recipe wrote " +
                                          "the lock from its earlier revision (" + lock + ")");
                }
            }
            return;
        }
        Decision withdrawn = new Decision(this, path, (Json.Document) manifest, base, ctx);
        withdrawn.withdrawn = true;
        install(withdrawn);
        rebuild();
        cannotKnow(ctx, path, "a recipe before this one changed " + changed + " after files visited earlier were " +
                              "written by this recipe's decision then" + (previous.description == null ? "" : ", " +
                              previous.description + "; it is now " + decision.description));
    }

    private void install(Decision decision) {
        version++;
        decisions.put(decision.path, decision);
        rebuild();
    }

    private void cannotKnow(ExecutionContext ctx, Path path, String why) {
        pending.add(why);
        failure(ctx, path, new LockFileRegeneration.Failure(LockFileRegeneration.Kind.CANNOT_KNOW,
                LockFileRegeneration.Reason.RESOLUTION_REQUIRED, null, why));
    }

    // the decision's own manifest and its package-manager file, as this cycle's decisions leave them now
    private Map<Path, String> outputs(ExecutionContext ctx, Decision decision) {
        Map<Path, String> out = new LinkedHashMap<>();
        SourceFile manifest = before(ctx, decision.path);
        if (manifest != null) {
            out.put(decision.path, print(editedManifest(manifest)));
        }
        ManagedFile file = managedOf(decision.path);
        if (file != null) {
            NodeCatalogs output = output(file, current(ctx, file));
            out.put(file.path, output == null ? print(base(ctx, file)) : output.print());
        }
        return out;
    }

    // the files visited so far that this cycle's decisions would now leave otherwise than they were left
    private List<Path> leftOtherwise(ExecutionContext ctx) {
        List<Path> otherwise = new ArrayList<>();
        for (Map.Entry<Path, String> file : new ArrayList<>(left.entrySet())) {
            SourceFile at = visited.get(file.getKey());
            String expected = at == null ? null : leftAs(ctx, file.getKey(), at);
            if (expected != null && !expected.equals(file.getValue())) {
                otherwise.add(file.getKey());
            }
        }
        return otherwise;
    }

    private @Nullable String leftAs(ExecutionContext ctx, Path path, SourceFile at) {
        ManagedFile file = managed.get(path);
        if (file != null && captured(path) == null) {
            return print(managedOut(ctx, file, at));
        }
        if (captured(path) != null) {
            return print(editedManifest(at));
        }
        if (!scan.lockToPackage.containsKey(path)) {
            return null;
        }
        Path importer = importerOf(ctx, NodeDependencyScan.lockImporters(scan, path));
        LockFileRegeneration.Result result = importer == null ? null :
                regeneration(ctx, importer).result.getRegenResult();
        return result != null && result.isSuccess() ? result.getLockFileContent() : print(at);
    }

    private void rebuild() {
        for (ManagedFile file : managed.values()) {
            if (file != null) {
                file.reset();
            }
        }
        for (Decision decision : decisions.values()) {
            ManagedFile file = decision.fileEdits.isEmpty() ? null : managedOf(decision.path);
            if (file != null) {
                file.ops.addAll(decision.fileEdits);
            }
        }
    }

    // importer's regeneration from the revisions before this recipe and this cycle's edits, again when they change
    private Regeneration regeneration(ExecutionContext ctx, Path importer) {
        NodeDependencyScan.ProjectState ps = requireNonNull(scan.projects.get(importer));
        SourceFile pkg = requireNonNull(before(ctx, importer));
        Decision decision = decisions.get(importer);
        Function<Json.Document, Json.Document> manifestEdit = decision == null || decision.withdrawn ||
                                                             decision.manifestEdit == null ? Function.identity() :
                decision.manifestEdit;
        Function<Json.Document, Json.Document> edit = doc -> withCatalogEdits(manifestEdit.apply(doc));
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(scan, p -> beforeText(ctx, p),
                importer);
        Map<Path, String> edited = editedFiles(ctx, importer, files);
        List<Object> inputs = Arrays.asList(print(pkg), print(edit.apply((Json.Document) pkg)), files,
                edited);
        Regeneration last = regenerations.get(importer);
        if (last != null && last.inputs.equals(inputs)) {
            return last;
        }
        Regeneration r = new Regeneration(inputs, PackageJsonHelper.editAndRegenerate(pkg, edit, edited,
                ps.capturedLockContent, files, ctx));
        regenerations.put(importer, r);
        return r;
    }

    // what this cycle's decisions change of the files a regeneration for packageJson reads besides it
    private Map<Path, String> editedFiles(ExecutionContext ctx, Path packageJson,
                                          NodeDependencyScan.ProjectFiles files) {
        Map<Path, String> edited = new LinkedHashMap<>();
        Set<Path> manifests = new LinkedHashSet<>(files.getManifests().keySet());
        manifests.addAll(files.getPackageManagerFiles().keySet());
        for (Path manifest : manifests) {
            SourceFile before = manifest.equals(packageJson) || captured(manifest) == null ? null :
                    before(ctx, manifest);
            String text = before == null ? null : print(editedManifest(before));
            if (text != null && !text.equals(print(before))) {
                edited.put(manifest, text);
            }
        }
        ManagedFile file = managedOf(packageJson);
        if (file != null && !(file.scanned instanceof NodeCatalogs.Bun)) {
            NodeCatalogs output = output(file, current(ctx, file));
            String base = print(base(ctx, file));
            if (output != null && !output.print().equals(base)) {
                edited.put(file.path, output.print());
            }
        }
        return edited;
    }

    // the text before this recipe of a file the recipes edit, for what a regeneration reads; null for the scanned one
    private @Nullable String beforeText(ExecutionContext ctx, Path path) {
        if (captured(path) != null) {
            return print(requireNonNull(before(ctx, path)));
        }
        ManagedFile file = managed.get(path);
        if (file != null) {
            return print(base(ctx, file));
        }
        SourceFile yaml = visited.containsKey(path) ? visited.get(path) : yamlFiles.get(path);
        return yaml == null ? null : print(visited.containsKey(path) ? yaml :
                PackageJsonHelper.editedBefore(ctx, this, yaml));
    }

    // a manifest as this recipe last visited it, else as the recipes before this one leave it
    private @Nullable SourceFile before(ExecutionContext ctx, Path packageJson) {
        SourceFile at = visited.get(packageJson);
        if (at != null) {
            return at;
        }
        return editedBefore(ctx, packageJson);
    }

    // a package-manager file before this recipe's edits: as visited, else as the recipes before this one leave it
    private SourceFile base(ExecutionContext ctx, ManagedFile file) {
        if (file.scanned instanceof NodeCatalogs.Bun) {
            file.rebase(requireNonNull(before(ctx, file.path)));
        } else {
            SourceFile at = visited.get(file.path);
            file.rebase(at != null ? at : PackageJsonHelper.editedBefore(ctx, this, file.scanned.getDocument()));
        }
        return file.base.getDocument();
    }

    private NodeCatalogs current(ExecutionContext ctx, ManagedFile file) {
        base(ctx, file);
        return file.current();
    }

    // a package-manager file as visited with this cycle's edits, and pnpm's cleanup
    private SourceFile managedOut(ExecutionContext ctx, ManagedFile file, SourceFile at) {
        NodeCatalogs edited = at == base(ctx, file) ? file.current() : file.replay(file.read(at));
        NodeCatalogs output = output(file, edited);
        if (output == null) {
            return at;
        }
        return at.getClass() == output.getDocument().getClass() ? output.getDocument() :
                PackageJsonHelper.reparseLock(at, output.print());
    }

    private SourceFile editedManifest(SourceFile file) {
        Decision decision = decisions.get(file.getSourcePath());
        SourceFile edited = decision == null || decision.withdrawn || decision.manifestEdit == null ||
                            !(file instanceof Json.Document) ? file : decision.manifestEdit.apply((Json.Document) file);
        return edited instanceof Json.Document ? withCatalogEdits((Json.Document) edited) : edited;
    }

    private void failure(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Failure failure) {
        if (failed.add(packageJson)) {
            rows.failure(ctx, packageJson, failure);
        }
    }

    // pnpm's cleanupUnusedCatalogs, which any install it runs applies, when this cycle edits the workspace; each
    // project as this recipe's decision leaves it
    private @Nullable NodeCatalogs output(ManagedFile file, NodeCatalogs edited) {
        if (!(edited instanceof NodeCatalogs.Pnpm) || !((NodeCatalogs.Pnpm) edited).isCleanupUnusedCatalogs()) {
            return edited;
        }
        boolean touched = false;
        Set<Path> workspaceProjects = workspaceProjects(file);
        if (otherManifest(workspaceProjects) != null) {
            return null;
        }
        List<Map<String, Map<String, String>>> projects = new ArrayList<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : scan.projects.entrySet()) {
            SourceFile captured = project.getValue().capturedPackageJson;
            if (captured == null || managedOf(project.getKey()) != file) {
                continue;
            }
            Decision decision = decisions.get(project.getKey());
            touched |= decision != null && decision.edits();
            if (workspaceProjects != null && !workspaceProjects.contains(project.getKey())) {
                continue;
            }
            SourceFile doc = decision != null ? decision.manifest : captured;
            if (decision != null && !decision.withdrawn && decision.manifestEdit != null) {
                doc = decision.manifestEdit.apply((Json.Document) doc);
            }
            projects.add(PackageJsonHelper.declaredDependencies(doc));
        }
        return touched ? ((NodeCatalogs.Pnpm) edited).withoutUnused(projects) : edited;
    }

    // why pnpm's cleanupUnusedCatalogs, which an edit of the workspace makes pnpm run, is not written in place
    private @Nullable String cleanupDeclined(ManagedFile file) {
        NodeCatalogs current = file.current();
        if (!(current instanceof NodeCatalogs.Pnpm) || !((NodeCatalogs.Pnpm) current).isCleanupUnusedCatalogs()) {
            return null;
        }
        if (current.isFlowStyle() && output(file, current) == null) {
            return "cleanupUnusedCatalogs would make pnpm rewrite the flow-style catalogs of " + file.path +
                   ", so this edit was not made";
        }
        Path other = otherManifest(workspaceProjects(file));
        return other == null ? null : "cleanupUnusedCatalogs counts the dependencies of " + other +
                                      ", which the recipes do not read, so this edit was not made";
    }

    private static @Nullable Path otherManifest(@Nullable Set<Path> projects) {
        if (projects != null) {
            for (Path project : projects) {
                if (!project.endsWith("package.json")) {
                    return project;
                }
            }
        }
        return null;
    }

    // pnpm's allProjects: the scanned projects pnpm-workspace.yaml's packages match, a member in another manifest
    // format by its own path; null when pnpm rejects the file
    private @Nullable Set<Path> workspaceProjects(ManagedFile file) {
        if (!(file.base instanceof NodeCatalogs.Pnpm) || ((NodeCatalogs.Pnpm) file.base).isRejected()) {
            return null;
        }
        NodeCatalogs.Pnpm workspace = (NodeCatalogs.Pnpm) file.base;
        Path dir = file.path.getParent();
        Map<String, Path> manifestDirs = new HashMap<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : scan.projects.entrySet()) {
            if (project.getValue().capturedPackageJson != null) {
                putUnder(manifestDirs, dir, project.getKey());
            }
        }
        for (Path held : scan.paths) {
            if (PnpmLockRegenerator.OTHER_MANIFEST_NAMES.contains(String.valueOf(held.getFileName()))) {
                putUnder(manifestDirs, dir, held);
            }
        }
        Set<Path> projects = new HashSet<>();
        for (String project : FindPackages.workspaceProjects(workspace.getSettings(), manifestDirs.keySet())) {
            projects.add(manifestDirs.get(project));
        }
        return projects;
    }

    private static void putUnder(Map<String, Path> manifestDirs, @Nullable Path dir, Path manifest) {
        Path projectDir = manifest.getParent();
        if (dir == null || projectDir != null && projectDir.startsWith(dir)) {
            Path relative = dir == null ? projectDir : dir.relativize(projectDir);
            manifestDirs.put(relative == null ? "" : relative.toString().replace('\\', '/'), manifest);
        }
    }

    // NodeCatalogs.Bun's root manifest with this cycle's catalog edits
    private Json.Document withCatalogEdits(Json.Document doc) {
        ManagedFile file = managed.get(doc.getSourcePath());
        if (file == null || file.ops.isEmpty() || !(file.scanned instanceof NodeCatalogs.Bun)) {
            return doc;
        }
        return (Json.Document) file.replay(file.read(doc)).getDocument();
    }

    private boolean holdsCatalogEdits(Path packageJson) {
        ManagedFile file = managedOf(packageJson);
        return file != null && file.path.equals(packageJson) && !file.ops.isEmpty();
    }

    private @Nullable ManagedFile managedOf(Path packageJson) {
        if (!managedOf.containsKey(packageJson)) {
            PackageManager pm = packageManager(packageJson);
            Path path = pm == null ? null : file(pm, packageJson);
            managedOf.put(packageJson, pm == null || path == null ? null : managedAt(path, pm));
        }
        return managedOf.get(packageJson);
    }

    private @Nullable ManagedFile managedAt(Path path, PackageManager pm) {
        if (!managed.containsKey(path)) {
            SourceFile document = pm == PackageManager.Bun ? captured(path) : yamlFiles.get(path);
            NodeCatalogs catalogs = document == null ? null : NodeCatalogs.read(pm, document);
            managed.put(path, catalogs == null ? null : new ManagedFile(catalogs, false));
        }
        return managed.get(path);
    }

    // a file beside the manifest the host does not hold, which the decision's edits create
    private @Nullable ManagedFile created(ExecutionContext ctx, Path packageJson, String name) {
        Path path = packageJson.resolveSibling(name);
        ManagedFile file = managed.get(path);
        if (file == null && !scan.paths.contains(path)) {
            SourceFile empty = YamlParser.builder().build().parse(ctx, "").findFirst().orElse(null);
            NodeCatalogs catalogs = empty == null ? null :
                    NodeCatalogs.read(PackageManager.Pnpm, empty.withSourcePath(path));
            if (catalogs != null) {
                file = new ManagedFile(catalogs, true);
                managed.put(path, file);
                managedOf.clear();
            }
        }
        return file;
    }

    // the marker's package manager, else the one a manifest at or above names in packageManager or
    // devEngines.packageManager, else the one whose catalog files it lies under
    private @Nullable PackageManager packageManager(Path packageJson) {
        NodeDependencyScan.ProjectState ps = scan.projects.get(packageJson);
        NodeResolutionResult marker = ps == null || ps.capturedPackageJson == null ? null :
                ps.capturedPackageJson.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker != null && marker.getPackageManager() != null) {
            return marker.getPackageManager();
        }
        for (Path dir = packageJson.getParent(); ; dir = dir.getParent()) {
            SourceFile manifest = captured(dir == null ? Paths.get("package.json") : dir.resolve("package.json"));
            PackageManager named = manifest == null ? null : named(manifest);
            if (named != null) {
                return named;
            }
            if (dir == null) {
                break;
            }
        }
        if (file(PackageManager.Pnpm, packageJson) != null) {
            return PackageManager.Pnpm;
        }
        if (file(PackageManager.YarnBerry, packageJson) != null) {
            return PackageManager.YarnBerry;
        }
        Path bun = file(PackageManager.Bun, packageJson);
        SourceFile root = bun == null ? null : captured(bun);
        NodeCatalogs catalogs = root == null ? null : NodeCatalogs.read(PackageManager.Bun, root);
        return catalogs != null && !catalogs.getCatalogs().isEmpty() ? PackageManager.Bun : null;
    }

    // the nearest file at or above the manifest's directory that pm reads catalogs from
    private @Nullable Path file(PackageManager pm, Path packageJson) {
        String name = NodeCatalogs.fileName(pm);
        if (name == null) {
            return null;
        }
        for (Path dir = packageJson.getParent(); ; dir = dir.getParent()) {
            Path candidate = dir == null ? Paths.get(name) : dir.resolve(name);
            if (pm == PackageManager.Bun) {
                SourceFile manifest = captured(candidate);
                if (manifest instanceof Json.Document && NodeCatalogs.Bun.base((Json.Document) manifest) != null) {
                    return candidate;
                }
            } else if (yamlFiles.containsKey(candidate) || managed.get(candidate) != null) {
                return candidate;
            }
            if (dir == null) {
                return null;
            }
        }
    }

    private @Nullable SourceFile captured(Path packageJson) {
        NodeDependencyScan.ProjectState ps = scan.projects.get(packageJson);
        return ps == null ? null : ps.capturedPackageJson;
    }

    private static @Nullable PackageManager named(SourceFile manifest) {
        if (!(manifest instanceof Json.Document) || !(((Json.Document) manifest).getValue() instanceof
                Json.JsonObject)) {
            return null;
        }
        Json.JsonObject root = (Json.JsonObject) ((Json.Document) manifest).getValue();
        String spec = NodeCatalogs.string(NodeCatalogs.member(root, "packageManager"));
        Json devEngines = NodeCatalogs.member(root, "devEngines");
        Json declared = devEngines instanceof Json.JsonObject ?
                NodeCatalogs.member((Json.JsonObject) devEngines, "packageManager") : null;
        if (spec == null && declared instanceof Json.JsonObject) {
            String name = NodeCatalogs.string(NodeCatalogs.member((Json.JsonObject) declared, "name"));
            String version = NodeCatalogs.string(NodeCatalogs.member((Json.JsonObject) declared, "version"));
            spec = name == null ? null : name + "@" + (version == null ? "" : version);
        }
        if (spec == null) {
            return null;
        }
        if (spec.startsWith("pnpm@")) {
            return PackageManager.Pnpm;
        }
        if (spec.startsWith("bun@")) {
            return PackageManager.Bun;
        }
        if (spec.startsWith("npm@")) {
            return PackageManager.Npm;
        }
        if (spec.startsWith("yarn@")) {
            return spec.startsWith("yarn@1.") || "yarn@1".equals(spec) ? PackageManager.YarnClassic :
                    PackageManager.YarnBerry;
        }
        return null;
    }

    private static <T extends Tree> T warn(T tree, String message) {
        return Markup.markup(tree, new Markup.Warn(randomId(), message, null));
    }

    private boolean same(@Nullable SourceFile a, @Nullable SourceFile b) {
        return a == b || a != null && b != null && print(a).equals(print(b));
    }

    private String print(SourceFile document) {
        return prints.computeIfAbsent(document, PackageJsonHelper::printed);
    }

    /**
     * How a dependency recipe decides one manifest's edit.
     */
    @FunctionalInterface
    public interface Decider {
        void decide(Decision decision, ExecutionContext ctx);
    }

    /**
     * Where a dependency recipe's rows go.
     */
    public interface Rows {
        void failure(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Failure failure);

        void declined(ExecutionContext ctx, Path path, String reason);
    }

    /**
     * What a recipe decides for one manifest from one revision of it and of its package-manager file: the manifest's
     * edit, the edits of that file, the warnings and the rows.
     */
    public static final class Decision {
        private final NodeDependencyEdits acc;
        private final ExecutionContext ctx;

        @Getter
        private final Path path;

        /**
         * The manifest decided from.
         */
        @Getter
        private final Json.Document manifest;

        private final @Nullable SourceFile base;
        private @Nullable Function<Json.Document, Json.Document> manifestEdit;
        private final List<UnaryOperator<NodeCatalogs>> fileEdits = new ArrayList<>();
        private final Set<String> warnings = new LinkedHashSet<>();
        private @Nullable String description;
        private boolean withdrawn;

        private Decision(NodeDependencyEdits acc, Path path, Json.Document manifest, @Nullable SourceFile base,
                         ExecutionContext ctx) {
            this.acc = acc;
            this.path = path;
            this.manifest = manifest;
            this.base = base;
            this.ctx = ctx;
        }

        /**
         * The catalogs the manifest's package manager reads for it, as the recipes before this one left them, with
         * the edits decided so far.
         */
        public @Nullable NodeCatalogs catalogs() {
            ManagedFile file = acc.managedOf(path);
            return file == null ? null : file.current();
        }

        /**
         * Another manifest as the recipes before this one leave it; null when the scan holds none there.
         */
        public @Nullable SourceFile manifestOf(Path packageJson) {
            return acc.editedBefore(ctx, packageJson);
        }

        /**
         * The manifest's package manager: its marker's, else the one it or a manifest above names, else the one whose
         * files it lies under.
         */
        public @Nullable PackageManager getPackageManager() {
            return acc.packageManager(path);
        }

        public void editManifest(Function<Json.Document, Json.Document> edit) {
            Function<Json.Document, Json.Document> before = manifestEdit;
            manifestEdit = before == null ? edit : before.andThen(edit);
            acc.version++;
        }

        /**
         * Edit the catalogs the manifest reads; false, editing nothing, when the edit cannot be made.
         */
        public boolean editCatalogs(Function<NodeCatalogs, @Nullable NodeCatalogs> edit) {
            return edit(acc.managedOf(path), edit);
        }

        /**
         * Edit the text of the file {@code name} beside the manifest, which the package manager reads, creating it
         * when the host holds none there (null text); false, editing nothing, when the edit cannot be made.
         */
        public boolean editFileBeside(String name, Function<@Nullable String, String> edit) {
            ManagedFile file = acc.managedOf(path);
            if (file == null || !file.path.equals(path.resolveSibling(name))) {
                file = acc.created(ctx, path, name);
            }
            boolean created = file != null && file.created;
            return edit(file, c -> {
                try {
                    return c.withText(edit.apply(created && c.print().isEmpty() ? null : c.print()));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            });
        }

        private boolean edit(@Nullable ManagedFile file, Function<NodeCatalogs, @Nullable NodeCatalogs> edit) {
            NodeCatalogs current = file == null ? null : file.current();
            NodeCatalogs edited = current == null ? null : edit.apply(current);
            if (file == null || edited == null) {
                return false;
            }
            if (edited != current) {
                UnaryOperator<NodeCatalogs> op = c -> {
                    NodeCatalogs e = edit.apply(c);
                    return e == null ? c : e;
                };
                fileEdits.add(op);
                file.append(op, edited);
                acc.version++;
            }
            return true;
        }

        /**
         * Mark the manifest with a warning.
         */
        public void warn(String message) {
            warnings.add(message);
        }

        /**
         * Record an edit not made, or not made as asked, and why, once a cycle.
         */
        public void decline(String reason) {
            if (acc.inserted.add(Collections.singletonList(path + "\n" + reason))) {
                acc.rows.declined(ctx, path, reason);
            }
        }

        /**
         * Record that the decision depends on what the engine cannot know, as a failure of the manifest's lock.
         */
        public void cannotKnow(String subject) {
            acc.failure(ctx, path, LockOnlyInstall.cannotKnow(new CannotKnow(subject)));
        }

        /**
         * What the decision is, for the warning written when a later revision overturns it.
         */
        public void describe(String what) {
            description = what;
        }

        /**
         * Write {@code row} once a cycle.
         */
        public <R> void insertRow(DataTable<R> table, ExecutionContext ctx, R row) {
            if (acc.inserted.add(row)) {
                table.insertRow(ctx, row);
            }
        }

        private boolean edits() {
            return manifestEdit != null || !fileEdits.isEmpty();
        }
    }

    // what a regeneration read, and its result
    private static final class Regeneration {
        final List<Object> inputs;
        final PackageJsonHelper.EditAndRegenerateResult result;

        Regeneration(List<Object> inputs, PackageJsonHelper.EditAndRegenerateResult result) {
            this.inputs = inputs;
            this.result = result;
        }
    }

    // a package-manager file the decisions edit (pnpm-workspace.yaml, .yarnrc.yml, or Bun's root manifest), as
    // scanned or created, its revision before this recipe, and the edits decided on it in decision order
    private static final class ManagedFile {
        final Path path;
        final NodeCatalogs scanned;
        final boolean created;
        final List<UnaryOperator<NodeCatalogs>> ops = new ArrayList<>();
        NodeCatalogs base;
        NodeCatalogs current;
        int applied;

        @Nullable
        SourceFile editedFrom;

        int editedAt = -1;

        @Nullable
        SourceFile edited;

        ManagedFile(NodeCatalogs scanned, boolean created) {
            this.path = scanned.getPath();
            this.scanned = scanned;
            this.created = created;
            this.base = scanned;
            this.current = scanned;
        }

        // a revision another recipe may have read as plain text
        NodeCatalogs read(SourceFile revision) {
            if (revision == scanned.getDocument()) {
                return scanned;
            }
            if (revision == base.getDocument()) {
                return base;
            }
            return scanned.reread(revision.getClass() == scanned.getDocument().getClass() ? revision :
                    PackageJsonHelper.reparseLock(scanned.getDocument(), PackageJsonHelper.printed(revision)).withSourcePath(path));
        }

        NodeCatalogs current() {
            while (applied < ops.size()) {
                current = ops.get(applied++).apply(current);
            }
            return current;
        }

        void rebase(SourceFile before) {
            if (before != base.getDocument()) {
                base = read(before);
                current = base;
                applied = 0;
            }
        }

        void append(UnaryOperator<NodeCatalogs> op, NodeCatalogs result) {
            current();
            ops.add(op);
            current = result;
            applied = ops.size();
        }

        void reset() {
            ops.clear();
            current = base;
            applied = 0;
        }

        NodeCatalogs replay(NodeCatalogs from) {
            for (UnaryOperator<NodeCatalogs> op : ops) {
                from = op.apply(from);
            }
            return from;
        }
    }
}
