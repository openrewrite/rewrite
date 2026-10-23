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
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SpecUtils;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

/**
 * The dependency recipes' one engine: the scan, each manifest's decision this cycle (made in generate from the
 * documents as the earlier recipes leave them, and again at a visit that finds one otherwise), and what it writes. It
 * keeps the text of each file it reads, not its tree, from one source file to the next.
 */
public final class NodeDependencyEdits implements PackageJsonHelper.FileEditor {

    /**
     * Why a manifest the recipe would edit is left as it is when the host parsed it without its marker: the marker
     * records the registries its lock is regenerated from.
     */
    public static final String UNMARKED = "this package.json was parsed without its NodeResolutionResult marker, " +
                                          "so its lock cannot be regenerated and it was not edited";

    public final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
    private final Rows rows;
    private final @Nullable String resolved;
    private final Set<Object> inserted = new HashSet<>();
    private final Set<Path> failed = new HashSet<>();
    private final Map<Path, String> yamlFiles = new HashMap<>();
    private final Map<Path, @Nullable ManagedFile> managed = new HashMap<>();
    private final Map<Path, @Nullable ManagedFile> managedOf = new HashMap<>();
    private final Map<Path, Rejected> rejected = new HashMap<>();
    private final Map<Path, Decision> decisions = new LinkedHashMap<>();
    private final Map<Path, String> visited = new HashMap<>();
    private final Map<Path, NodeDependencyScan.Manifest> visitedManifests = new HashMap<>();
    private final Map<Path, String> left = new LinkedHashMap<>();
    private final Map<Path, Regeneration> regenerations = new HashMap<>();
    private final Set<Path> warnedStale = new HashSet<>();
    private final Set<Path> generated = new HashSet<>();
    private final Map<Path, Map<String, String>> editedManifests = new HashMap<>();
    private int editedManifestsAt = -1;
    private final List<String> pending = new ArrayList<>();
    private @Nullable Decider decider;
    private @Nullable SourceFile visiting;
    private int version;
    private boolean linked;

    public NodeDependencyEdits(Rows rows) {
        this(rows, null);
    }

    /**
     * An engine whose decisions ask whether each manifest's marker resolves {@code resolved}, by
     * {@link Decision#resolves()}.
     */
    public NodeDependencyEdits(Rows rows, @Nullable String resolved) {
        this.rows = rows;
        this.resolved = resolved;
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
        linked = false;
        Path p = sf.getSourcePath();
        String basename = String.valueOf(p.getFileName());
        if (sf instanceof Yaml.Documents && readsCatalogsFrom(basename)) {
            yamlFiles.put(p, print(sf));
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
        // one parsed without its marker too: the package manager reads it, and a recipe that would edit it says so
        if (sf instanceof Json.Document && "package.json".equals(basename)) {
            scan.projects.computeIfAbsent(p, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson =
                    NodeDependencyScan.Manifest.of(sf, resolved);
        }
    }

    private static boolean readsCatalogsFrom(String fileName) {
        for (LockFileRegeneration regeneration : LockFileRegeneration.ALL) {
            if (fileName.equals(regeneration.manifestRules().catalogFile())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decide every scanned manifest's edit; the package-manager files the decisions create.
     */
    public List<SourceFile> decideAll(ExecutionContext ctx, Decider decider) {
        this.decider = decider;
        PackageJsonHelper.addFileEditor(ctx, this);
        linkWorkspaceMembers();
        for (Path project : new TreeMap<>(scan.projects).keySet()) {
            if (captured(project) != null) {
                decide(ctx, project, null);
            }
        }
        List<SourceFile> created = new ArrayList<>();
        for (ManagedFile file : managed.values()) {
            if (file != null && file.created) {
                NodeCatalogs output = output(file, current(ctx, file));
                if (output != null && !output.print().isEmpty()) {
                    generated.add(file.path);
                    created.add(output.getDocument());
                }
            }
        }
        return created;
    }

    // once per scan: a recipe run asks for a visitor for each source file, and linking maps every workspace
    private void linkWorkspaceMembers() {
        if (!linked) {
            NodeDependencyScan.linkWorkspaceMembers(scan);
            linked = true;
        }
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
        NodeDependencyScan.Manifest captured = captured(packageJson);
        return captured == null ? null : captured.document(editedBeforeText(ctx, captured));
    }

    // a manifest's text as the recipes before this one in the cycle leave it, as scanned
    private String editedBeforeText(ExecutionContext ctx, NodeDependencyScan.Manifest captured) {
        return PackageJsonHelper.editedBefore(ctx, this, captured.getPath(), captured.getText());
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
    public String edit(ExecutionContext ctx, Path path, String text) {
        ManagedFile managedFile = managed.get(path);
        if (managedFile != null && text.equals(managedFile.editedFrom) && managedFile.editedAt == version) {
            return requireNonNull(managedFile.edited);
        }
        String edited = editedManifest(path, text);
        if (managedFile != null && !managedFile.bun) {
            NodeCatalogs out = output(managedFile, managedFile.replay(managedFile.read(edited)));
            managedFile.editedFrom = text;
            managedFile.editedAt = version;
            managedFile.edited = out == null ? edited : out.print();
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
        linkWorkspaceMembers();
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
                visiting = sf;
                try {
                    if (managed.get(p) != null && captured(p) == null) {
                        out = visitManaged(ctx, requireNonNull(managed.get(p)), sf);
                    } else if (captured(p) != null) {
                        out = visitManifest(ctx, sf);
                    } else if (scan.lockToPackage.containsKey(p)) {
                        out = visitLock(ctx, sf);
                    } else {
                        return tree;
                    }
                } finally {
                    visiting = null;
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
        String text = print(sf);
        if (!generated.contains(p)) {
            visited.put(p, text);
            for (Path manifest : new ArrayList<>(decisions.keySet())) {
                if (managedOf(manifest) == file) {
                    decide(ctx, manifest, p);
                }
            }
        }
        SourceFile out = managedOut(file, sf);
        String leftAs = out == sf ? text : print(out);
        left.put(p, leftAs);
        return leftAs.equals(text) ? sf : out;
    }

    private SourceFile visitManifest(ExecutionContext ctx, SourceFile sf) {
        Path p = sf.getSourcePath();
        NodeDependencyScan.Manifest at = NodeDependencyScan.Manifest.of(sf, resolved);
        visited.put(p, at.getText());
        visitedManifests.put(p, at);
        decide(ctx, p, p);
        Decision decision = decisions.get(p);
        SourceFile out = sf;
        if (decision != null && !decision.withdrawn && (decision.manifestEdit != null || holdsCatalogEdits(p))) {
            Regeneration r = regeneration(ctx, p);
            Json.Document edited = sf instanceof Json.Document ? editOf(p).apply((Json.Document) sf) : null;
            if (edited != null && edited != sf) {
                out = PackageJsonHelper.withResolvedDeps(PackageJsonHelper.refreshMarker(edited), r.result,
                        r.installs);
            }
            if (r.result != null && !r.result.isSuccess()) {
                failure(ctx, p, requireNonNull(r.result.getFailure()));
                out = warn(out, "lock regeneration failed: " + r.result.getErrorMessage());
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
        String text = print(sf);
        visited.put(p, text);
        Path importer = importerOf(ctx, NodeDependencyScan.lockImporters(scan, p));
        LockFileRegeneration.Result result = importer == null ? null : regeneration(ctx, importer).result;
        SourceFile out = sf;
        if (result != null && result.isSuccess()) {
            String content = requireNonNull(result.getLockFileContent());
            out = content.equals(text) ? sf : PackageJsonHelper.reparseLock(sf, content);
        } else if (result != null) {
            failure(ctx, requireNonNull(importer), requireNonNull(result.getFailure()));
            out = warn(sf, "lock regeneration failed: " + result.getErrorMessage());
        } else {
            String beside = writtenBeside(ctx, p);
            out = beside == null || beside.equals(text) ? sf : PackageJsonHelper.reparseLock(sf, beside);
        }
        left.put(p, out == sf ? text : print(out));
        return out;
    }

    // what the regeneration of the lock beside p writes at p besides that lock: npm's yarn.lock
    private @Nullable String writtenBeside(ExecutionContext ctx, Path p) {
        for (Path lock : scan.lockToPackage.keySet()) {
            if (lock.equals(p) || !Objects.equals(lock.getParent(), p.getParent())) {
                continue;
            }
            Path importer = importerOf(ctx, NodeDependencyScan.lockImporters(scan, lock));
            LockFileRegeneration.Result result = importer == null ? null : regeneration(ctx, importer).result;
            String text = result == null ? null : result.getFiles().get(String.valueOf(p.getFileName()));
            if (text != null) {
                return text;
            }
        }
        return null;
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
            if (captured(importer) != null && regeneration(ctx, importer).result != null) {
                return importer;
            }
        }
        return null;
    }

    // decide path's edit from the revisions of its manifest and package-manager file before this recipe; at a visit
    // that finds them otherwise, the new decision stands unless a file written already says otherwise
    private void decide(ExecutionContext ctx, Path path, @Nullable Path changed) {
        String text = before(ctx, path);
        if (text == null) {
            return;
        }
        ManagedFile file = managedOf(path);
        String base = file == null ? null : base(ctx, file);
        Decision previous = decisions.get(path);
        if (previous != null && (previous.withdrawn ||
                                 previous.text.equals(text) && Objects.equals(previous.base, base))) {
            return;
        }
        Json.Document manifest = document(path, text);
        if (manifest == null) {
            return;
        }
        Map<Path, String> previousOutputs = previous == null ? emptyMap() : outputs(ctx, previous);
        Decision decision = new Decision(this, path, manifest, text, base, requireNonNull(revision(path)));
        install(decision);
        try {
            requireNonNull(decider).decide(decision, ctx);
            if (decision.edits() && !manifest.getMarkers().findFirst(NodeResolutionResult.class).isPresent()) {
                decision.editNothing();
                version++;
                decision.decline(ctx, UNMARKED);
            }
            ManagedFile edited = decision.edits() ? managedOf(path) : null;
            String cleanupDeclined = edited == null ? null : cleanupDeclined(edited);
            if (cleanupDeclined != null) {
                decision.editNothing();
                version++;
                decision.warn(cleanupDeclined);
                decision.decline(ctx, cleanupDeclined);
            }
        } finally {
            decision.manifest = null;
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
        Decision withdrawn = new Decision(this, path, null, text, base, decision.revision);
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
        String manifest = before(ctx, decision.path);
        if (manifest != null) {
            out.put(decision.path, editedManifest(decision.path, manifest));
        }
        ManagedFile file = managedOf(decision.path);
        if (file != null) {
            NodeCatalogs output = output(file, current(ctx, file));
            out.put(file.path, output == null ? base(ctx, file) : output.print());
        }
        return out;
    }

    // the files visited so far that this cycle's decisions would now leave otherwise than they were left
    private List<Path> leftOtherwise(ExecutionContext ctx) {
        List<Path> otherwise = new ArrayList<>();
        for (Map.Entry<Path, String> file : new ArrayList<>(left.entrySet())) {
            String at = visited.get(file.getKey());
            String expected = at == null ? null : leftAs(ctx, file.getKey(), at);
            if (expected != null && !expected.equals(file.getValue())) {
                otherwise.add(file.getKey());
            }
        }
        return otherwise;
    }

    private @Nullable String leftAs(ExecutionContext ctx, Path path, String at) {
        ManagedFile file = managed.get(path);
        if (file != null && captured(path) == null) {
            NodeCatalogs output = output(file, at.equals(base(ctx, file)) ? file.current() :
                    file.replay(file.read(at)));
            return output == null ? at : output.print();
        }
        if (captured(path) != null) {
            return editedManifest(path, at);
        }
        if (!scan.lockToPackage.containsKey(path)) {
            return null;
        }
        Path importer = importerOf(ctx, NodeDependencyScan.lockImporters(scan, path));
        LockFileRegeneration.Result result = importer == null ? null : regeneration(ctx, importer).result;
        if (result == null) {
            String beside = writtenBeside(ctx, path);
            return beside == null ? at : beside;
        }
        return result.isSuccess() ? result.getLockFileContent() : at;
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
        String pkg = requireNonNull(before(ctx, importer));
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(scan, p -> beforeText(ctx, p),
                importer);
        Map<Path, String> edited = editedFiles(ctx, importer, files);
        List<Object> inputs = Arrays.asList(pkg, editedManifest(importer, pkg), files, edited);
        Regeneration last = regenerations.get(importer);
        if (last != null && last.inputs.equals(inputs)) {
            return last;
        }
        Json.Document before = document(importer, pkg);
        Json.Document after = before == null ? null : editOf(importer).apply(before);
        Map<Path, String> changed = PackageJsonHelper.changedFiles(edited, files);
        LockFileRegeneration.Result result = after == null || after == before && changed.isEmpty() ||
                                             ps.capturedLockContent == null ? null :
                PackageJsonHelper.regenerateLockContent(PackageJsonHelper.refreshMarker(after),
                        ps.capturedLockContent, files, changed, ctx);
        Regeneration r = new Regeneration(inputs, result, files.getPackageManager());
        regenerations.put(importer, r);
        return r;
    }

    // the edit this cycle's decision makes of a manifest, with the catalog edits of Bun's root manifest
    private Function<Json.Document, Json.Document> editOf(Path packageJson) {
        Decision decision = decisions.get(packageJson);
        Function<Json.Document, Json.Document> manifestEdit = decision == null || decision.withdrawn ||
                                                             decision.manifestEdit == null ? Function.identity() :
                decision.manifestEdit;
        return doc -> withCatalogEdits(manifestEdit.apply(doc));
    }

    // what this cycle's decisions change of the files a regeneration for packageJson reads besides it
    private Map<Path, String> editedFiles(ExecutionContext ctx, Path packageJson,
                                          NodeDependencyScan.ProjectFiles files) {
        Map<Path, String> edited = new LinkedHashMap<>();
        Set<Path> manifests = new LinkedHashSet<>(files.getManifests().keySet());
        manifests.addAll(files.getPackageManagerFiles().keySet());
        for (Path manifest : manifests) {
            String before = manifest.equals(packageJson) || captured(manifest) == null ? null :
                    before(ctx, manifest);
            String text = before == null ? null : editedManifest(manifest, before);
            if (text != null && !text.equals(before)) {
                edited.put(manifest, text);
            }
        }
        ManagedFile file = managedOf(packageJson);
        if (file != null && !file.bun) {
            NodeCatalogs output = output(file, current(ctx, file));
            String base = base(ctx, file);
            if (output != null && !output.print().equals(base)) {
                edited.put(file.path, output.print());
            }
        }
        return edited;
    }

    // the text before this recipe of a file the recipes edit, for what a regeneration reads; null for the scanned one
    private @Nullable String beforeText(ExecutionContext ctx, Path path) {
        if (captured(path) != null) {
            return requireNonNull(before(ctx, path));
        }
        ManagedFile file = managed.get(path);
        if (file != null) {
            return base(ctx, file);
        }
        String at = visited.get(path);
        if (at != null) {
            return at;
        }
        String yaml = yamlFiles.get(path);
        return yaml == null ? null : PackageJsonHelper.editedBefore(ctx, this, path, yaml);
    }

    // a manifest's text as this recipe last visited it, else as the recipes before this one leave it
    private @Nullable String before(ExecutionContext ctx, Path packageJson) {
        String at = visited.get(packageJson);
        if (at != null) {
            return at;
        }
        NodeDependencyScan.Manifest captured = captured(packageJson);
        return captured == null ? null : editedBeforeText(ctx, captured);
    }

    // a revision of a manifest, its text: the document being visited, else the text parsed again with what was kept
    // of that revision's markers
    private Json.@Nullable Document document(Path packageJson, String text) {
        if (visiting != null && packageJson.equals(visiting.getSourcePath())) {
            return visiting instanceof Json.Document ? (Json.Document) visiting : null;
        }
        NodeDependencyScan.Manifest revision = revision(packageJson);
        return revision == null ? null : revision.document(text);
    }

    // the manifest whose markers a decision reads: as this recipe last visited it, else as scanned
    private NodeDependencyScan.@Nullable Manifest revision(Path packageJson) {
        NodeDependencyScan.Manifest at = visitedManifests.get(packageJson);
        return at != null ? at : captured(packageJson);
    }

    // a package-manager file's text before this recipe's edits: as visited, else as the recipes before this one leave
    // it
    private String base(ExecutionContext ctx, ManagedFile file) {
        if (file.bun) {
            file.rebase(requireNonNull(before(ctx, file.path)));
        } else {
            String at = visited.get(file.path);
            file.rebase(at != null ? at : PackageJsonHelper.editedBefore(ctx, this, file.path, file.scanned));
        }
        return file.base;
    }

    private NodeCatalogs current(ExecutionContext ctx, ManagedFile file) {
        base(ctx, file);
        return file.current();
    }

    // a package-manager file as visited with this cycle's edits, and pnpm's cleanup
    private SourceFile managedOut(ManagedFile file, SourceFile at) {
        NodeCatalogs output = output(file, file.replay(file.read(at)));
        if (output == null) {
            return at;
        }
        return at.getClass() == output.getDocument().getClass() ? output.getDocument() :
                PackageJsonHelper.reparseLock(at, output.print());
    }

    // a manifest's text with this cycle's edits, the same for the same revision until a decision changes: every
    // regeneration asks for each manifest of its project
    private String editedManifest(Path path, String text) {
        if (editedManifestsAt != version) {
            editedManifests.clear();
            editedManifestsAt = version;
        }
        Map<String, String> revisions = editedManifests.computeIfAbsent(path, k -> new HashMap<>());
        String known = revisions.get(text);
        if (known != null) {
            return known;
        }
        Decision decision = decisions.get(path);
        Function<Json.Document, Json.Document> manifestEdit = decision == null || decision.withdrawn ? null :
                decision.manifestEdit;
        ManagedFile file = managed.get(path);
        String edited = text;
        if (manifestEdit != null || file != null && file.bun && !file.ops.isEmpty()) {
            NodeDependencyScan.Manifest revision = revision(path);
            Json.Document doc = revision == null ? null : revision.parse(text);
            if (doc != null) {
                edited = print(withCatalogEdits(manifestEdit == null ? doc : manifestEdit.apply(doc)));
            }
        }
        revisions.put(text, edited);
        return edited;
    }

    private void failure(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Failure failure) {
        if (failed.add(packageJson)) {
            rows.failure(ctx, packageJson, failure);
        }
    }

    // pnpm's cleanupUnusedCatalogs, which any install it runs applies, when this cycle edits the workspace; each
    // project as this recipe's decision leaves it
    private @Nullable NodeCatalogs output(ManagedFile file, NodeCatalogs edited) {
        if (!(edited instanceof NodeCatalogs.Pnpm) || !((NodeCatalogs.Pnpm) edited).cleansUp()) {
            return edited;
        }
        boolean touched = false;
        for (Map.Entry<Path, Decision> decision : decisions.entrySet()) {
            touched |= managedOf(decision.getKey()) == file && decision.getValue().edits();
        }
        if (!touched) {
            return edited;
        }
        try {
            return ((NodeCatalogs.Pnpm) edited).withoutUnused(projects(file, new HashMap<>()));
        } catch (CannotKnow | IllegalArgumentException e) {
            return null;
        } catch (JsError e) {
            // the override value it trips on is one pnpm rejects before it cleans up (catalog-cleanup-override-number)
            return edited;
        }
    }

    // why pnpm's cleanupUnusedCatalogs, which an edit of the workspace makes pnpm run, is not written in place
    private @Nullable String cleanupDeclined(ManagedFile file) {
        NodeCatalogs current = file.current();
        if (!(current instanceof NodeCatalogs.Pnpm) || !((NodeCatalogs.Pnpm) current).isCleanupUnusedCatalogs()) {
            return null;
        }
        if (current.declined() != null) {
            return "cleanupUnusedCatalogs makes pnpm rewrite " + file.path + ", and " + current.declined() +
                   ", so this edit was not made";
        }
        if (!((NodeCatalogs.Pnpm) current).cleansUp()) {
            return null;
        }
        Map<String, Path> unread = new HashMap<>();
        try {
            ((NodeCatalogs.Pnpm) current).withoutUnused(projects(file, unread));
            return null;
        } catch (IllegalArgumentException e) {
            return "cleanupUnusedCatalogs would make pnpm rewrite " + file.path + ", and " + e.getMessage() +
                   ", so this edit was not made";
        } catch (CannotKnow e) {
            Path other = unread.isEmpty() ? file.path : unread.values().iterator().next();
            return "cleanupUnusedCatalogs counts the dependencies of " + other + ", which the recipes do not read, " +
                   "so this edit was not made";
        } catch (JsError e) {
            return null;
        }
    }

    // the projects under the workspace's directory by directory from it, each as this cycle's decisions leave it; one
    // whose manifest the recipes do not read (another format than package.json) is null, its file in unread
    private Map<String, @Nullable Map<String, @Nullable Object>> projects(ManagedFile file, Map<String, Path> unread) {
        Path dir = file.path.getParent();
        Map<String, @Nullable Map<String, @Nullable Object>> projects = new LinkedHashMap<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : scan.projects.entrySet()) {
            NodeDependencyScan.Manifest captured = project.getValue().capturedPackageJson;
            String at = captured == null ? null : under(dir, project.getKey());
            if (at == null) {
                continue;
            }
            Decision decision = decisions.get(project.getKey());
            Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, String>> scope : (decision != null ? decision.declared() :
                    captured.getDeclared()).entrySet()) {
                manifest.put(scope.getKey(), new LinkedHashMap<>(scope.getValue()));
            }
            projects.put(at, manifest);
        }
        for (Path held : scan.paths) {
            String at = LockFileRegeneration.PNPM.readsAsManifest(String.valueOf(held.getFileName())) ?
                    under(dir, held) : null;
            if (at != null) {
                projects.put(at, null);
                unread.put(at, held);
            }
        }
        return projects;
    }

    // the directory of a project's manifest relative to dir; null outside it
    private static @Nullable String under(@Nullable Path dir, Path manifest) {
        Path projectDir = manifest.getParent();
        if (dir != null && (projectDir == null || !projectDir.startsWith(dir))) {
            return null;
        }
        Path relative = dir == null ? projectDir : dir.relativize(projectDir);
        return relative == null ? "" : relative.toString().replace('\\', '/');
    }

    // NodeCatalogs.Bun's root manifest with this cycle's catalog edits
    private Json.Document withCatalogEdits(Json.Document doc) {
        ManagedFile file = managed.get(doc.getSourcePath());
        if (file == null || file.ops.isEmpty() || !file.bun) {
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
            PackageManager pm;
            try {
                pm = packageManager(packageJson);
            } catch (Rejected e) {
                rejected.put(packageJson, e);
                pm = null;
            }
            Path path = pm == null ? null : file(pm, packageJson);
            managedOf.put(packageJson, pm == null || path == null ? null : managedAt(path, pm));
        }
        return managedOf.get(packageJson);
    }

    private @Nullable ManagedFile managedAt(Path path, PackageManager pm) {
        if (!managed.containsKey(path)) {
            String yaml = yamlFiles.get(path);
            NodeDependencyScan.Manifest root = yaml == null ? captured(path) : null;
            Function<String, @Nullable SourceFile> parse = root != null ? root::parse :
                    text -> PackageJsonHelper.parseYaml(path, text);
            String scanned = yaml != null ? yaml : root == null ? null : root.getText();
            SourceFile document = scanned == null ? null : parse.apply(scanned);
            String rootManifest = rootBeside(path);
            NodeCatalogs catalogs = document == null ? null : NodeCatalogs.read(pm, document, rootManifest);
            managed.put(path, catalogs == null ? null : new ManagedFile(catalogs, parse, rootManifest, false));
        }
        return managed.get(path);
    }

    // the package.json beside a package-manager file, as scanned, which names the release that writes the file
    private @Nullable String rootBeside(Path file) {
        NodeDependencyScan.Manifest root = captured(file.resolveSibling("package.json"));
        return root == null ? null : root.getText();
    }

    // a file beside the manifest the host does not hold, which the decision's edits create
    private @Nullable ManagedFile created(ExecutionContext ctx, Path packageJson, String name) {
        Path path = packageJson.resolveSibling(name);
        ManagedFile file = managed.get(path);
        if (file == null && !scan.paths.contains(path)) {
            SourceFile empty = YamlParser.builder().build().parse(ctx, "").findFirst().orElse(null);
            ManifestRules rules = writes(name);
            String rootManifest = rootBeside(path);
            NodeCatalogs catalogs = empty == null || rules == null ? null :
                    NodeCatalogs.read(rules, empty.withSourcePath(path), rootManifest);
            if (catalogs != null) {
                file = new ManagedFile(catalogs, text -> PackageJsonHelper.parseYaml(path, text), rootManifest, true);
                managed.put(path, file);
                managedOf.clear();
                rejected.clear();
            }
        }
        return file;
    }

    // the rules of the package manager whose workspace settings file is named so
    private static @Nullable ManifestRules writes(String name) {
        for (LockFileRegeneration regeneration : LockFileRegeneration.ALL) {
            if (name.equals(regeneration.manifestRules().workspaceSettingsFile())) {
                return regeneration.manifestRules();
            }
        }
        return null;
    }

    // the package manager that installs the manifest by the scan, its marker's or its workspace's; else the one
    // corepack finds named from the manifest's directory up, else the one whose catalog files it lies under; throws
    // Rejected for a spec corepack rejects
    private @Nullable PackageManager packageManager(Path packageJson) {
        PackageManager installs = NodeDependencyScan.packageManager(scan, packageJson);
        if (installs != null) {
            return installs;
        }
        PackageManager named = named(packageJson.getParent());
        if (named != null) {
            return named;
        }
        if (file(PackageManager.Pnpm, packageJson) != null) {
            return PackageManager.Pnpm;
        }
        if (file(PackageManager.YarnBerry, packageJson) != null) {
            return PackageManager.YarnBerry;
        }
        Path bun = file(PackageManager.Bun, packageJson);
        NodeDependencyScan.Manifest root = bun == null ? null : captured(bun);
        Json.Document document = root == null ? null : root.parse(root.getText());
        NodeCatalogs catalogs = document == null ? null : NodeCatalogs.read(PackageManager.Bun, document);
        return catalogs != null && !catalogs.getCatalogs().isEmpty() ? PackageManager.Bun : null;
    }

    // the nearest file at or above the manifest's directory that pm reads catalogs from
    private @Nullable Path file(PackageManager pm, Path packageJson) {
        ManifestRules rules = LockFileRegeneration.manifestRules(pm);
        String name = rules.catalogFile();
        if (name == null) {
            return null;
        }
        for (Path dir = packageJson.getParent(); ; dir = dir.getParent()) {
            Path candidate = dir == null ? Paths.get(name) : dir.resolve(name);
            NodeDependencyScan.Manifest manifest = captured(candidate);
            if (manifest != null ? rules.catalogsAt(manifest.getText()) != null :
                    yamlFiles.containsKey(candidate) || managed.get(candidate) != null) {
                return candidate;
            }
            if (dir == null) {
                return null;
            }
        }
    }

    private NodeDependencyScan.@Nullable Manifest captured(Path packageJson) {
        NodeDependencyScan.ProjectState ps = scan.projects.get(packageJson);
        return ps == null ? null : ps.capturedPackageJson;
    }

    // corepack's loadSpec from dir up to the repository's root: the spec of the nearest packageManager, else of the
    // topmost manifest's devEngines.packageManager; bun, which corepack does not run, by its name
    private @Nullable PackageManager named(@Nullable Path dir) {
        SpecUtils.Lookup lookup = SpecUtils.loadSpec("", dir == null ? 0 : dir.getNameCount(), up -> {
            Path at = (dir == null ? Paths.get(up) : dir.resolve(up)).normalize();
            NodeDependencyScan.Manifest manifest = captured(at.toString().isEmpty() ? Paths.get("package.json") :
                    at.resolve("package.json"));
            return manifest == null ? null : manifest.getText();
        }, path -> false, HostEnvironment.UNKNOWN);
        Object raw = lookup.getRawPmSpec();
        if ("bun".equals(raw) || raw instanceof String && ((String) raw).startsWith("bun@")) {
            return PackageManager.Bun;
        }
        SpecUtils.Descriptor spec = lookup.getSpec(true);
        if (spec == null) {
            return null;
        }
        if ("yarn".equals(spec.getName())) {
            String version = SpecUtils.installVersion(spec);
            return version != null && version.startsWith("1.") ? PackageManager.YarnClassic : PackageManager.YarnBerry;
        }
        return "pnpm".equals(spec.getName()) ? PackageManager.Pnpm : PackageManager.Npm;
    }

    private static <T extends Tree> T warn(T tree, String message) {
        return Markup.markup(tree, new Markup.Warn(randomId(), message, null));
    }

    private static String print(SourceFile document) {
        return PackageJsonHelper.printed(document);
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

        @Getter
        private final Path path;

        // the manifest decided from, while the decision is made
        private Json.@Nullable Document manifest;

        // the texts of the manifest and its package-manager file decided from, and what was kept of the manifest's
        // markers
        private final String text;
        private final @Nullable String base;
        private final NodeDependencyScan.Manifest revision;

        private @Nullable Function<Json.Document, Json.Document> manifestEdit;
        private @Nullable Map<String, Map<String, String>> declared;
        private final List<UnaryOperator<NodeCatalogs>> fileEdits = new ArrayList<>();
        private final Set<String> warnings = new LinkedHashSet<>();
        private @Nullable String description;
        private boolean withdrawn;

        private Decision(NodeDependencyEdits acc, Path path, Json.@Nullable Document manifest, String text,
                         @Nullable String base, NodeDependencyScan.Manifest revision) {
            this.acc = acc;
            this.path = path;
            this.manifest = manifest;
            this.text = text;
            this.base = base;
            this.revision = revision;
        }

        /**
         * The manifest decided from, while the decision is made.
         */
        public Json.Document getManifest() {
            return requireNonNull(manifest);
        }

        /**
         * The catalogs the manifest's package manager reads for it, as the recipes before this one left them, with
         * the edits decided so far; throws Rejected, with corepack's message, for a spec corepack rejects.
         */
        public @Nullable NodeCatalogs catalogs() {
            ManagedFile file = acc.managedOf(path);
            Rejected rejected = acc.rejected.get(path);
            if (rejected != null) {
                throw rejected;
            }
            return file == null ? null : file.current();
        }

        /**
         * Another manifest's text as the recipes before this one leave it; null when the scan holds none there.
         */
        public @Nullable String manifestOf(ExecutionContext ctx, Path packageJson) {
            NodeDependencyScan.Manifest captured = acc.captured(packageJson);
            return captured == null ? null : acc.editedBeforeText(ctx, captured);
        }

        /**
         * The manifest's package manager: its marker's, else the one corepack finds named, else the one whose files
         * it lies under; throws Rejected, with corepack's message, for a spec corepack rejects.
         */
        public @Nullable PackageManager getPackageManager() {
            return acc.packageManager(path);
        }

        /**
         * Whether the lock the manifest's marker records resolves the package the engine asks about; true when it
         * records no resolutions at all.
         */
        public boolean resolves() {
            return revision.resolves();
        }

        public void editManifest(Function<Json.Document, Json.Document> edit) {
            Function<Json.Document, Json.Document> before = manifestEdit;
            manifestEdit = before == null ? edit : before.andThen(edit);
            declared = null;
            acc.version++;
        }

        /**
         * Edit the catalogs the manifest reads; false, editing nothing, when the edit cannot be made.
         */
        public boolean editCatalogs(Function<NodeCatalogs, @Nullable NodeCatalogs> edit) {
            return edit(acc.managedOf(path), edit);
        }

        /**
         * Edit the file {@code name} beside the manifest, which the package manager reads, creating it when the host
         * holds none there; false, editing nothing, when the edit cannot be made in place.
         */
        public boolean editFileBeside(ExecutionContext ctx, String name,
                                      Function<NodeCatalogs, @Nullable NodeCatalogs> edit) {
            ManagedFile file = acc.managedOf(path);
            if (file == null || !file.path.equals(path.resolveSibling(name))) {
                file = acc.created(ctx, path, name);
            }
            return edit(file, c -> {
                try {
                    return edit.apply(c);
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
        public void decline(ExecutionContext ctx, String reason) {
            if (acc.inserted.add(Collections.singletonList(path + "\n" + reason))) {
                acc.rows.declined(ctx, path, reason);
            }
        }

        /**
         * Record that the decision depends on what the engine cannot know, as a failure of the manifest's lock.
         */
        public void cannotKnow(ExecutionContext ctx, CannotKnow what) {
            acc.failure(ctx, path, LockOnlyInstall.cannotKnow(what));
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

        // the edits undone, as when they cannot be made
        private void editNothing() {
            manifestEdit = null;
            declared = null;
            fileEdits.clear();
        }

        // the dependencies the manifest declares as the decision leaves it, for pnpm's cleanup of unused catalogs
        private Map<String, Map<String, String>> declared() {
            if (declared == null) {
                Json.Document doc = manifest != null ? manifest : revision.parse(text);
                if (doc != null && !withdrawn && manifestEdit != null) {
                    doc = manifestEdit.apply(doc);
                }
                declared = doc == null ? new LinkedHashMap<>() : PackageJsonHelper.declaredDependencies(doc);
            }
            return declared;
        }
    }

    // what a regeneration read, and its result
    private static final class Regeneration {
        final List<Object> inputs;
        final LockFileRegeneration.@Nullable Result result;

        // the package manager that installs the manifest where its marker names another; null for the marker's
        final @Nullable PackageManager installs;

        Regeneration(List<Object> inputs, LockFileRegeneration.@Nullable Result result,
                     @Nullable PackageManager installs) {
            this.inputs = inputs;
            this.result = result;
            this.installs = installs;
        }
    }

    // a package-manager file the decisions edit (pnpm-workspace.yaml, .yarnrc.yml, or Bun's root manifest), as
    // scanned or created, its revision before this recipe, and the edits decided on it in decision order; the
    // catalogs of a revision hold its tree, so they are kept only while memory allows and read again from its text
    private static final class ManagedFile {
        final Path path;
        final String scanned;
        final boolean created;
        final boolean bun;
        final ManifestRules rules;
        final @Nullable String rootManifest;
        final List<UnaryOperator<NodeCatalogs>> ops = new ArrayList<>();
        String base;
        int applied;

        // a revision's text parsed as the host parsed the file
        final Function<String, @Nullable SourceFile> parse;

        Reference<@Nullable NodeCatalogs> scannedCatalogs;
        Reference<@Nullable NodeCatalogs> baseCatalogs;
        Reference<@Nullable NodeCatalogs> current;

        @Nullable
        String editedFrom;

        int editedAt = -1;

        @Nullable
        String edited;

        ManagedFile(NodeCatalogs catalogs, Function<String, @Nullable SourceFile> parse,
                    @Nullable String rootManifest, boolean created) {
            this.path = catalogs.getPath();
            this.scanned = catalogs.print();
            this.created = created;
            this.bun = catalogs instanceof NodeCatalogs.Bun;
            this.rules = catalogs.getRules();
            this.rootManifest = rootManifest;
            this.parse = parse;
            this.base = scanned;
            this.scannedCatalogs = new SoftReference<>(catalogs);
            this.baseCatalogs = scannedCatalogs;
            this.current = scannedCatalogs;
        }

        // the catalogs as scanned or created
        NodeCatalogs scanned() {
            NodeCatalogs catalogs = scannedCatalogs.get();
            if (catalogs == null) {
                catalogs = requireNonNull(NodeCatalogs.read(rules, requireNonNull(parse.apply(scanned)),
                        rootManifest));
                scannedCatalogs = new SoftReference<>(catalogs);
            }
            return catalogs;
        }

        // a revision another recipe may have read as plain text
        NodeCatalogs read(SourceFile revision) {
            NodeCatalogs catalogs = scanned();
            return catalogs.reread(revision.getClass() == catalogs.getDocument().getClass() ? revision :
                    PackageJsonHelper.reparseLock(catalogs.getDocument(), PackageJsonHelper.printed(revision)));
        }

        NodeCatalogs read(String revision) {
            NodeCatalogs at = revision.equals(base) ? baseCatalogs.get() : null;
            if (at != null) {
                return at;
            }
            NodeCatalogs catalogs = scanned();
            if (revision.equals(scanned)) {
                return catalogs;
            }
            SourceFile document = parse.apply(revision);
            return catalogs.reread(document == null ? catalogs.getDocument() : document);
        }

        NodeCatalogs current() {
            NodeCatalogs cached = current.get();
            NodeCatalogs at = cached;
            if (at == null) {
                at = base();
                applied = 0;
            }
            while (applied < ops.size()) {
                at = ops.get(applied++).apply(at);
            }
            if (at != cached) {
                current = new SoftReference<>(at);
            }
            return at;
        }

        private NodeCatalogs base() {
            NodeCatalogs at = baseCatalogs.get();
            if (at == null) {
                at = read(base);
                baseCatalogs = new SoftReference<>(at);
            }
            return at;
        }

        void rebase(String before) {
            if (!before.equals(base)) {
                base = before;
                baseCatalogs = new SoftReference<>(null);
                current = baseCatalogs;
                applied = 0;
            }
        }

        void append(UnaryOperator<NodeCatalogs> op, NodeCatalogs result) {
            current();
            ops.add(op);
            current = new SoftReference<>(result);
            applied = ops.size();
        }

        void reset() {
            ops.clear();
            current = baseCatalogs;
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
