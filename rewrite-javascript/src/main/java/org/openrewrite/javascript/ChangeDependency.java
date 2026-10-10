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
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeDependency extends ScanningRecipe<NodeCatalogs.Accumulator> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

    @Option(displayName = "Old package name",
            description = "The current name of the npm package to rename.",
            example = "lodash")
    String oldPackageName;

    @Option(displayName = "New package name",
            description = "The new name to use for the package.",
            example = "lodash-es")
    String newPackageName;

    @Option(displayName = "New version",
            description = "Optional new version constraint to set on the renamed package.",
            example = "^5.0.0",
            required = false)
    @Nullable String newVersion;

    @Option(displayName = "Scope",
            description = "The dependency scope: `dependencies`, `devDependencies`, etc. " +
                    "When omitted, all scopes are searched.",
            valid = {"dependencies", "devDependencies", "peerDependencies",
                    "optionalDependencies", "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Override public String getDisplayName() { return "Change npm dependency"; }
    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` -> `%s`", oldPackageName, newPackageName);
    }

    @Override public String getDescription() {
        return "Renames an npm dependency in `package.json` and optionally updates its version constraint. A " +
                "`catalog:` reference keeps its catalog, which gains the new name's entry. After modifying the " +
                "package.json, the lock file is regenerated natively, without executing the package manager. Not " +
                "safe to use as a precondition: consults the package registry over the network and publishes " +
                "per-project state shared with other dependency recipes.";
    }

    @Override
    public NodeCatalogs.Accumulator getInitialValue(ExecutionContext ctx) {
        return new NodeCatalogs.Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NodeCatalogs.Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();
                String basename = p.getFileName().toString();

                acc.capture(sf);
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
    public Collection<? extends SourceFile> generate(NodeCatalogs.Accumulator acc, ExecutionContext ctx) {
        Map<Path, SourceFile> renamed = new LinkedHashMap<>();
        Map<Path, Path> cleaned = new LinkedHashMap<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : new TreeMap<>(acc.scan.projects).entrySet()) {
            SourceFile pkg = project.getValue().capturedPackageJson;
            if (!(pkg instanceof Json.Document)) {
                continue;
            }
            Path path = project.getKey();
            Map<String, Boolean> keepValue = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, String>> declared :
                    PackageJsonHelper.declaredDependencies(pkg).entrySet()) {
                String value = declared.getValue().get(oldPackageName);
                if (value == null || scope != null && !scope.equals(declared.getKey())) {
                    continue;
                }
                Spec spec = NodeCatalogs.classify(oldPackageName, value);
                NodeCatalogs catalogs = acc.of(path);
                String catalog = catalogs == null ? null : catalogs.catalogOf(value);
                String entry = catalog == null ? null : newVersion != null ? newVersion :
                        catalogs.specifier(catalog, oldPackageName);
                if (spec != null && (spec.isVersionSpec() || spec.isLocation() && newVersion != null)) {
                    keepValue.put(declared.getKey(), false);
                } else if (spec != null && spec.getType() == Spec.Type.CATALOG && entry != null) {
                    keepValue.put(declared.getKey(), true);
                    acc.edit(path, c -> c.withEntry(catalog, newPackageName, entry));
                    if (catalogs.isCleanupUnusedCatalogs()) {
                        cleaned.putIfAbsent(catalogs.getPath(), path);
                    }
                } else {
                    decline(acc, ctx, path, declared.getKey(), value, spec);
                }
            }
            if (!keepValue.isEmpty()) {
                project.getValue().matchedDeps = emptyList();
                renamed.put(path, rename((Json.Document) pkg, keepValue));
                acc.manifestEdits.put(path, doc -> rename(doc, keepValue));
            }
        }
        for (Map.Entry<Path, Path> workspace : cleaned.entrySet()) {
            List<Map<String, String>> declarations = acc.declarations(ctx, workspace.getKey(), renamed);
            acc.edit(workspace.getValue(), c -> c.withoutUnused(declarations));
        }
        return emptyList();
    }

    private Json.Document rename(Json.Document doc, Map<String, Boolean> keepValue) {
        Json.Document out = doc;
        for (Map.Entry<String, Boolean> declared : keepValue.entrySet()) {
            out = PackageJsonHelper.changeDependency(out, oldPackageName, newPackageName,
                    declared.getValue() ? null : newVersion, declared.getKey());
        }
        return out;
    }

    private void decline(NodeCatalogs.Accumulator acc, ExecutionContext ctx, Path path, String declaredScope,
                         String value, @Nullable Spec spec) {
        String protocol = spec == null || spec.getProtocol() == null ? "" : spec.getProtocol();
        NodeDependencyProtocolsSkipped.Row row = new NodeDependencyProtocolsSkipped.Row(path.toString(),
                path.toString(), oldPackageName, declaredScope, protocol, value, newVersion == null ? "" : newVersion);
        if (acc.rows.add(row)) {
            protocolsSkipped.insertRow(ctx, row);
        }
        String remedy = spec != null && spec.isLocation() ? "pass a newVersion to replace it with a registry version" :
                spec != null && spec.getType() == Spec.Type.CATALOG ? "its catalog holds no entry for it" :
                        "rename it where the value points first";
        acc.declines.computeIfAbsent(path, k -> new LinkedHashSet<>()).add(oldPackageName + " is declared as " +
                value + (protocol.isEmpty() ? "" : " (" + protocol + ")") + " and was left unchanged: " + remedy);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeCatalogs.Accumulator acc) {
        NodeDependencyScan.linkWorkspaceMembers(acc.scan);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();

                SourceFile catalogFile = acc.editedFile(sf);
                if (catalogFile != null) {
                    return catalogFile;
                }
                NodeDependencyScan.ProjectState ps = acc.scan.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    if (acc.manifestEdits.containsKey(p) || holdsCatalogEdits(acc, p)) {
                        ensureComputed(ps, sf, ctx);
                    }
                    SourceFile out = sf;
                    if (ps.modifiedPackageJson != null) {
                        out = NodeDependencyScan.modifiedFor(ps, sf);
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p);
                            out = warn(out, "lock regeneration failed: " + ps.regenResult.getErrorMessage());
                        }
                    }
                    for (String decline : acc.declines.getOrDefault(p, new LinkedHashSet<>())) {
                        out = warn(out, decline);
                    }
                    if (out != sf) {
                        return out;
                    }
                }

                List<Path> importers = NodeDependencyScan.lockImporters(acc.scan, p);
                for (Path importer : importers) {
                    NodeDependencyScan.ProjectState ips = acc.scan.projects.get(importer);
                    if (ips == null) continue;
                    if (!acc.results.containsKey(importer)) {
                        SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, importer);
                        if (pkg == null) pkg = ips.capturedPackageJson;
                        if (pkg != null && (acc.manifestEdits.containsKey(importer) ||
                                            acc.editsCatalogsOf(importer))) {
                            ensureComputed(ips, pkg, ctx);
                            if (ips.modifiedPackageJson != null) {
                                PackageJsonHelper.putLiveTree(ctx, importer, ips.modifiedPackageJson);
                            }
                        }
                    }
                }
                Path last = acc.lastRegenerated(importers);
                NodeDependencyScan.ProjectState lps = last == null ? null : acc.scan.projects.get(last);
                if (lps != null && lps.regenResult != null) {
                    if (lps.regenResult.isSuccess()) {
                        return PackageJsonHelper.reparseLock(sf, lps.regenResult.getLockFileContent());
                    }
                    recordFailure(ctx, lps, last);
                    return warn(sf, "lock regeneration failed: " + lps.regenResult.getErrorMessage());
                }
                return tree;
            }

            private void ensureComputed(NodeDependencyScan.ProjectState ps, SourceFile pkg, ExecutionContext ctx) {
                Path path = pkg.getSourcePath();
                if (acc.results.containsKey(path)) return;
                Function<Json.Document, Json.Document> manifestEdit = acc.manifestEdits.getOrDefault(path,
                        Function.identity());
                Function<Json.Document, Json.Document> edit = doc -> acc.withCatalogEdits(manifestEdit.apply(doc));
                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg, edit, acc.editedFiles(ctx, path), ps.capturedLockContent,
                        NodeDependencyScan.projectFiles(acc.scan, ctx, path), ctx);
                acc.results.put(path, r);
                if (r.isChanged()) {
                    ps.modifiedPackageJson = r.getModifiedPackageJson();
                    ps.regenResult = r.getRegenResult();
                    ps.editedFrom = pkg;
                    ps.edit = edit;
                }
            }
        };
    }

    private static boolean holdsCatalogEdits(NodeCatalogs.Accumulator acc, Path packageJson) {
        NodeCatalogs catalogs = acc.of(packageJson);
        return catalogs != null && catalogs.getPath().equals(packageJson) && acc.editsCatalogsOf(packageJson);
    }

    private static <T extends Tree> T warn(T tree, String message) {
        return Markup.markup(tree, new Markup.Warn(randomId(), message, null));
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJsonPath) {
        if (ps.failureRecorded || ps.regenResult == null) {
            return;
        }
        ps.failureRecorded = true;
        LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, ps.regenResult,
                oldPackageName);
    }
}
