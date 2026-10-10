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
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * A dependency recipe's scan and its decisions this cycle, made in generate from the documents as the earlier
 * recipes leave them, and again at visit time for one that differs, while nothing written yet depends on them.
 */
public final class NodeDependencyEdits implements PackageJsonHelper.FileEditor {
    public final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
    private final Set<Object> rows = new HashSet<>();
    private final Map<Path, SourceFile> files = new HashMap<>();
    private final Map<Path, @Nullable CatalogFile> catalogFiles = new HashMap<>();
    private final Map<Path, @Nullable CatalogFile> catalogFileOf = new HashMap<>();
    private final Map<Path, Decision> decisions = new LinkedHashMap<>();
    private final Map<Path, PackageJsonHelper.EditAndRegenerateResult> results = new LinkedHashMap<>();
    private final Set<Path> written = new HashSet<>();
    private int version;

    /**
     * Note what a dependency recipe's scan reads from {@code sf}.
     */
    public void scan(SourceFile sf) {
        Path p = sf.getSourcePath();
        String basename = String.valueOf(p.getFileName());
        if (sf instanceof Yaml.Documents && (basename.equals(NodeCatalogs.fileName(PackageManager.Pnpm)) ||
                                            basename.equals(NodeCatalogs.fileName(PackageManager.YarnBerry)))) {
            files.put(p, sf);
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
     * Decide every scanned manifest's edit.
     */
    public void decideAll(ExecutionContext ctx, Decider decider) {
        PackageJsonHelper.addFileEditor(ctx, this);
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : new TreeMap<>(scan.projects).entrySet()) {
            SourceFile pkg = project.getValue().capturedPackageJson;
            if (pkg != null) {
                decide(project.getKey(), PackageJsonHelper.editedBefore(ctx, this, pkg), ctx, decider);
            }
        }
    }

    /**
     * The catalogs the package manager of {@code packageJson} reads for it, as the recipes before this one left
     * them, with this cycle's edits.
     */
    public @Nullable NodeCatalogs of(ExecutionContext ctx, Path packageJson) {
        CatalogFile file = catalogFile(packageJson);
        return file == null ? null : rebased(ctx, file).current();
    }

    /**
     * A manifest or catalog file with this cycle's edits, and pnpm's cleanup of the catalogs they leave unused.
     */
    @Override
    public SourceFile edit(ExecutionContext ctx, SourceFile file) {
        CatalogFile catalogFile = catalogFiles.get(file.getSourcePath());
        if (catalogFile != null && catalogFile.editedFrom == file && catalogFile.editedAt == version) {
            return requireNonNull(catalogFile.edited);
        }
        Decision decision = decisions.get(file.getSourcePath());
        SourceFile edited = decision == null || decision.manifestEdit == null || !(file instanceof Json.Document) ?
                file : decision.manifestEdit.apply((Json.Document) file);
        if (catalogFile != null) {
            NodeCatalogs in = edited == catalogFile.scanned.getDocument() ? catalogFile.scanned :
                    catalogFile.scanned.reread(edited);
            NodeCatalogs out = output(catalogFile, catalogFile.replay(in));
            catalogFile.editedFrom = file;
            catalogFile.editedAt = version;
            catalogFile.edited = out == null ? edited : out.getDocument();
            return catalogFile.edited;
        }
        return edited;
    }

    private CatalogFile rebased(ExecutionContext ctx, CatalogFile file) {
        file.rebase(PackageJsonHelper.editedBefore(ctx, this, file.scanned.getDocument()));
        return file;
    }

    /**
     * The visitor applying this cycle's decisions: catalog files edited in place, manifests edited or marked
     * declined, and each lock regenerated once from its importers as edited.
     */
    public TreeVisitor<Tree, ExecutionContext> visitor(Decider decider, FailureRow failureRow) {
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
                SourceFile catalogFile = editedFile(ctx, sf);
                if (catalogFile != null) {
                    return catalogFile;
                }
                NodeDependencyScan.ProjectState ps = scan.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    decide(p, sf, ctx, decider);
                    Decision decision = decisions.get(p);
                    if (decision != null && decision.manifestEdit != null || holdsCatalogEdits(p)) {
                        regenerate(ps, sf, ctx);
                    }
                    SourceFile out = sf;
                    if (ps.modifiedPackageJson != null) {
                        out = NodeDependencyScan.modifiedFor(ps, sf);
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p, failureRow);
                            out = warn(out, "lock regeneration failed: " + ps.regenResult.getErrorMessage());
                        }
                    }
                    for (String decline : decision == null ? Collections.<String>emptySet() : decision.declines) {
                        out = warn(out, decline);
                    }
                    if (out != sf) {
                        return out;
                    }
                }
                List<Path> importers = NodeDependencyScan.lockImporters(scan, p);
                Path last = regenerateLock(ctx, importers);
                NodeDependencyScan.ProjectState lps = last == null ? null : scan.projects.get(last);
                if (lps != null && lps.regenResult != null) {
                    if (lps.regenResult.isSuccess()) {
                        return PackageJsonHelper.reparseLock(sf, lps.regenResult.getLockFileContent());
                    }
                    recordFailure(ctx, lps, last, failureRow);
                    return warn(sf, "lock regeneration failed: " + lps.regenResult.getErrorMessage());
                }
                return tree;
            }
        };
    }

    // decide path's edit from this revision of its manifest and catalogs, unless what this recipe wrote depends on
    // the last one
    private void decide(Path path, SourceFile manifest, ExecutionContext ctx, Decider decider) {
        if (!(manifest instanceof Json.Document)) {
            return;
        }
        CatalogFile catalogs = catalogFile(path);
        SourceFile base = catalogs == null ? null : rebased(ctx, catalogs).base.getDocument();
        Decision previous = decisions.get(path);
        if (previous != null && same(previous.from, manifest) && same(previous.base, base)) {
            return;
        }
        version++;
        if (previous != null && (results.containsKey(path) || written.contains(path))) {
            previous.from = manifest;
            previous.base = base;
            return;
        }
        Map<Path, String> before = previous == null ? emptyMap() : writtenCatalogs(ctx);
        Decision decision = new Decision(this, path, (Json.Document) manifest, base);
        decisions.put(path, decision);
        if (previous != null) {
            rebuild();
        }
        decider.decide(decision, ctx);
        CatalogFile file = decision.edits() ? catalogFile(path) : null;
        if (file != null && file.current().isFlowStyle() && output(file, file.current()) == null) {
            decision.manifestEdit = null;
            decision.catalogEdits.clear();
            decision.decline("cleanupUnusedCatalogs would make pnpm rewrite the flow-style catalogs of " +
                             file.path + ", so this edit was not made");
            rebuild();
        }
        if (previous != null) {
            rebuild();
            if (!before.equals(writtenCatalogs(ctx))) {
                previous.from = manifest;
                previous.base = base;
                decisions.put(path, previous);
                rebuild();
            }
        }
    }

    private void rebuild() {
        for (CatalogFile file : catalogFiles.values()) {
            if (file != null) {
                file.reset();
            }
        }
        for (Decision decision : decisions.values()) {
            CatalogFile file = decision.catalogEdits.isEmpty() ? null : catalogFile(decision.path);
            if (file != null) {
                file.edits.addAll(decision.catalogEdits);
            }
        }
    }

    private Map<Path, String> writtenCatalogs(ExecutionContext ctx) {
        Map<Path, String> out = new HashMap<>();
        for (CatalogFile file : catalogFiles.values()) {
            if (file != null && written.contains(file.path)) {
                NodeCatalogs output = output(file, rebased(ctx, file).current());
                out.put(file.path, output == null ? "" : output.print());
            }
        }
        return out;
    }

    // the lock's importers regenerated as this cycle edits them; the one regenerated last holds every edit
    private @Nullable Path regenerateLock(ExecutionContext ctx, List<Path> importers) {
        for (Path importer : importers) {
            NodeDependencyScan.ProjectState ips = scan.projects.get(importer);
            if (ips == null || ips.capturedPackageJson == null || results.containsKey(importer)) {
                continue;
            }
            Decision decision = decisions.get(importer);
            if (decision != null && decision.manifestEdit != null) {
                regenerate(ips, decision.from, ctx);
                if (ips.modifiedPackageJson != null) {
                    PackageJsonHelper.putLiveTree(ctx, importer, ips.modifiedPackageJson);
                }
            }
        }
        if (lastRegenerated(importers) == null) {
            for (int i = importers.size() - 1; i >= 0; i--) {
                NodeDependencyScan.ProjectState ips = scan.projects.get(importers.get(i));
                CatalogFile file = catalogFile(importers.get(i));
                if (ips != null && ips.capturedPackageJson != null && file != null && !file.edits.isEmpty()) {
                    regenerate(ips, from(ctx, importers.get(i)), ctx);
                    break;
                }
            }
        }
        written.addAll(importers);
        return lastRegenerated(importers);
    }

    private @Nullable Path lastRegenerated(List<Path> importers) {
        Path last = null;
        for (Map.Entry<Path, PackageJsonHelper.EditAndRegenerateResult> result : results.entrySet()) {
            if (importers.contains(result.getKey()) && result.getValue().getRegenResult() != null) {
                last = result.getKey();
            }
        }
        return last;
    }

    // a manifest as the recipes before this one leave it, as this recipe last decided from it
    private SourceFile from(ExecutionContext ctx, Path packageJson) {
        Decision decision = decisions.get(packageJson);
        return decision != null ? decision.from :
                PackageJsonHelper.editedBefore(ctx, this, requireNonNull(captured(packageJson)));
    }

    private void regenerate(NodeDependencyScan.ProjectState ps, SourceFile pkg, ExecutionContext ctx) {
        Path path = pkg.getSourcePath();
        if (results.containsKey(path)) {
            return;
        }
        Decision decision = decisions.get(path);
        Function<Json.Document, Json.Document> manifestEdit = decision == null || decision.manifestEdit == null ?
                Function.identity() : decision.manifestEdit;
        Function<Json.Document, Json.Document> edit = doc -> withCatalogEdits(manifestEdit.apply(doc));
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(scan, ctx, path);
        PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(pkg, edit,
                editedFiles(ctx, path, files), ps.capturedLockContent, files, ctx);
        results.put(path, r);
        if (r.isChanged()) {
            ps.modifiedPackageJson = r.getModifiedPackageJson();
            ps.regenResult = r.getRegenResult();
            ps.editedFrom = pkg;
            ps.edit = edit;
        }
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJson,
                               FailureRow failureRow) {
        if (!ps.failureRecorded && ps.regenResult != null) {
            ps.failureRecorded = true;
            failureRow.insert(ctx, packageJson, ps.regenResult);
        }
    }

    // the project's other manifests and its catalog file that a regeneration for packageJson reads, as the recipes
    // before this one and this cycle's decisions leave them, in any visit order
    private Map<Path, String> editedFiles(ExecutionContext ctx, Path packageJson,
                                          NodeDependencyScan.ProjectFiles files) {
        Map<Path, String> edited = new LinkedHashMap<>();
        Set<Path> manifests = new LinkedHashSet<>(files.getManifests().keySet());
        manifests.addAll(files.getPackageManagerFiles().keySet());
        for (Path manifest : manifests) {
            if (!manifest.equals(packageJson) && captured(manifest) != null) {
                edited.put(manifest, printed(edit(ctx, from(ctx, manifest))));
                written.add(manifest);
            }
        }
        CatalogFile file = catalogFile(packageJson);
        if (file != null && !(file.scanned instanceof NodeCatalogs.Bun)) {
            NodeCatalogs output = output(file, rebased(ctx, file).current());
            written.add(file.path);
            if (output != null && output.getDocument() != file.scanned.getDocument()) {
                edited.put(file.path, output.print());
            }
        }
        return edited;
    }

    // a catalog file as visited, with this cycle's edits, when they change it
    private @Nullable SourceFile editedFile(ExecutionContext ctx, SourceFile visited) {
        Path path = visited.getSourcePath();
        PackageManager pm = String.valueOf(path.getFileName()).equals(NodeCatalogs.fileName(PackageManager.Pnpm)) ?
                PackageManager.Pnpm : PackageManager.YarnBerry;
        CatalogFile file = !files.containsKey(path) ? null : catalogFileAt(path, pm);
        if (file == null) {
            return null;
        }
        written.add(file.path);
        NodeCatalogs edited = visited == rebased(ctx, file).base.getDocument() ? file.current() :
                file.replay(file.scanned.reread(visited));
        NodeCatalogs output = output(file, edited);
        return output == null || output.getDocument() == visited ? null : output.getDocument();
    }

    // pnpm's cleanupUnusedCatalogs, which any install it runs applies, when this cycle edits the workspace; each
    // project as this recipe's decision leaves it
    private @Nullable NodeCatalogs output(CatalogFile file, NodeCatalogs edited) {
        if (!(edited instanceof NodeCatalogs.Pnpm) || !((NodeCatalogs.Pnpm) edited).isCleanupUnusedCatalogs()) {
            return edited;
        }
        boolean touched = false;
        Set<Path> workspaceProjects = workspaceProjects(file);
        List<Map<String, Map<String, String>>> projects = new ArrayList<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : scan.projects.entrySet()) {
            SourceFile captured = project.getValue().capturedPackageJson;
            if (captured == null || catalogFile(project.getKey()) != file) {
                continue;
            }
            Decision decision = decisions.get(project.getKey());
            touched |= decision != null && decision.edits();
            if (workspaceProjects != null && !workspaceProjects.contains(project.getKey())) {
                continue;
            }
            SourceFile doc = decision != null ? decision.from : captured;
            if (doc instanceof Json.Document && decision != null && decision.manifestEdit != null) {
                doc = decision.manifestEdit.apply((Json.Document) doc);
            }
            projects.add(PackageJsonHelper.declaredDependencies(doc));
        }
        return touched ? ((NodeCatalogs.Pnpm) edited).withoutUnused(projects) : edited;
    }

    // pnpm's allProjects: the workspace root and the members its packages list; null without the root's manifest
    private @Nullable Set<Path> workspaceProjects(CatalogFile file) {
        Path root = file.path.resolveSibling("package.json");
        SourceFile manifest = captured(root);
        if (manifest == null) {
            return null;
        }
        Set<Path> projects = new HashSet<>(PackageJsonHelper.workspaceMemberPaths(manifest));
        projects.add(root);
        return projects;
    }

    // NodeCatalogs.Bun's root manifest with this cycle's catalog edits
    private Json.Document withCatalogEdits(Json.Document doc) {
        CatalogFile file = catalogFiles.get(doc.getSourcePath());
        if (file == null || file.edits.isEmpty() || !(file.scanned instanceof NodeCatalogs.Bun)) {
            return doc;
        }
        written.add(file.path);
        return (Json.Document) file.replay(file.scanned.reread(doc)).getDocument();
    }

    private boolean holdsCatalogEdits(Path packageJson) {
        CatalogFile file = catalogFile(packageJson);
        return file != null && file.path.equals(packageJson) && !file.edits.isEmpty();
    }

    private @Nullable CatalogFile catalogFile(Path packageJson) {
        if (!catalogFileOf.containsKey(packageJson)) {
            PackageManager pm = packageManager(packageJson);
            Path path = pm == null ? null : file(pm, packageJson);
            catalogFileOf.put(packageJson, pm == null || path == null ? null : catalogFileAt(path, pm));
        }
        return catalogFileOf.get(packageJson);
    }

    private @Nullable CatalogFile catalogFileAt(Path path, PackageManager pm) {
        if (!catalogFiles.containsKey(path)) {
            SourceFile document = pm == PackageManager.Bun ? captured(path) : files.get(path);
            NodeCatalogs catalogs = document == null ? null : NodeCatalogs.read(pm, document);
            catalogFiles.put(path, catalogs == null ? null : new CatalogFile(catalogs));
        }
        return catalogFiles.get(path);
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
            } else if (files.containsKey(candidate)) {
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

    private static boolean same(@Nullable SourceFile a, @Nullable SourceFile b) {
        return a == b || a != null && b != null && printed(a).equals(printed(b));
    }

    /**
     * How a dependency recipe decides one manifest's edit.
     */
    @FunctionalInterface
    public interface Decider {
        void decide(Decision decision, ExecutionContext ctx);
    }

    /**
     * How a dependency recipe records a lock regeneration that failed.
     */
    @FunctionalInterface
    public interface FailureRow {
        void insert(ExecutionContext ctx, Path packageJson, LockFileRegeneration.Result result);
    }

    /**
     * What a recipe decides for one manifest from one revision of it: the manifest's edit, the catalog edits that edit
     * needs, and the edits it declines.
     */
    public static final class Decision {
        private final NodeDependencyEdits acc;

        @Getter
        private final Path path;

        /**
         * The manifest decided from.
         */
        @Getter
        private final Json.Document manifest;

        private SourceFile from;
        private @Nullable SourceFile base;
        private @Nullable Function<Json.Document, Json.Document> manifestEdit;
        private final List<UnaryOperator<NodeCatalogs>> catalogEdits = new ArrayList<>();
        private final Set<String> declines = new LinkedHashSet<>();

        private Decision(NodeDependencyEdits acc, Path path, Json.Document manifest, @Nullable SourceFile base) {
            this.acc = acc;
            this.path = path;
            this.manifest = manifest;
            this.from = manifest;
            this.base = base;
        }

        /**
         * The catalogs the manifest's package manager reads for it, as the recipes before this one left them, with
         * the edits decided so far.
         */
        public @Nullable NodeCatalogs catalogs() {
            CatalogFile file = acc.catalogFile(path);
            return file == null ? null : file.current();
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
            CatalogFile file = acc.catalogFile(path);
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
                catalogEdits.add(op);
                file.append(op, edited);
                acc.version++;
            }
            return true;
        }

        public void decline(String message) {
            declines.add(message);
        }

        /**
         * Write {@code row} once a cycle.
         */
        public <R> void insertRow(DataTable<R> table, ExecutionContext ctx, R row) {
            if (acc.rows.add(row)) {
                table.insertRow(ctx, row);
            }
        }

        private boolean edits() {
            return manifestEdit != null || !catalogEdits.isEmpty();
        }
    }

    // a catalog file as scanned, as the recipes before this one left it, and the edits decided on it in decision order
    private static final class CatalogFile {
        final Path path;
        final NodeCatalogs scanned;
        final List<UnaryOperator<NodeCatalogs>> edits = new ArrayList<>();
        NodeCatalogs base;
        NodeCatalogs current;
        int applied;

        @Nullable
        SourceFile editedFrom;

        int editedAt = -1;

        @Nullable
        SourceFile edited;

        CatalogFile(NodeCatalogs scanned) {
            this.path = scanned.getPath();
            this.scanned = scanned;
            this.base = scanned;
            this.current = scanned;
        }

        NodeCatalogs current() {
            while (applied < edits.size()) {
                current = edits.get(applied++).apply(current);
            }
            return current;
        }

        void rebase(SourceFile before) {
            if (before != base.getDocument()) {
                base = before == scanned.getDocument() ? scanned : scanned.reread(before);
                current = base;
                applied = 0;
            }
        }

        void append(UnaryOperator<NodeCatalogs> edit, NodeCatalogs result) {
            current();
            edits.add(edit);
            current = result;
            applied = edits.size();
        }

        void reset() {
            edits.clear();
            current = base;
            applied = 0;
        }

        NodeCatalogs replay(NodeCatalogs base) {
            for (UnaryOperator<NodeCatalogs> edit : edits) {
                base = edit.apply(base);
            }
            return base;
        }
    }

    private static String printed(SourceFile document) {
        return document.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }
}
