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
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.semver.NodeSemver;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddDependency extends ScanningRecipe<NodeCatalogs.Accumulator> {

    private static final String CATALOG = "catalog:";

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);

    @Option(displayName = "Package name",
            description = "The name of the npm package to add (e.g., `lodash`, `@types/node`).",
            example = "lodash")
    String packageName;

    @Option(displayName = "Version",
            description = "The version constraint to set (e.g., `^5.0.0`, `~2.1.0`, `3.0.0`).",
            example = "^5.0.0")
    String version;

    @Option(displayName = "Scope",
            description = "The dependency scope: `dependencies`, `devDependencies`, `peerDependencies`, " +
                    "or `optionalDependencies`. Defaults to `dependencies`.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Override public String getDisplayName() { return "Add npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Add an npm dependency to `package.json` and regenerate the lock file natively, without executing the " +
                "package manager. In a pnpm workspace with catalogs it writes what `pnpm add` writes under the " +
                "workspace's `catalogMode`. If the dependency already exists in any scope, the recipe is a no-op. " +
                "Not safe to use as a precondition: consults the package registry over the network and publishes " +
                "per-project state shared with other dependency recipes.";
    }

    private String targetScope() { return scope == null ? "dependencies" : scope; }

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
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : new TreeMap<>(acc.scan.projects).entrySet()) {
            SourceFile pkg = project.getValue().capturedPackageJson;
            if (!(pkg instanceof Json.Document) || !matchesAdd(pkg)) {
                continue;
            }
            Path path = project.getKey();
            if (PackageJsonHelper.isNonObjectMember((Json.Document) pkg, targetScope())) {
                acc.declines.computeIfAbsent(path, k -> new LinkedHashSet<>()).add("`" + targetScope() +
                        "` is not an object, so " + packageName + " was not added");
                continue;
            }
            NodeCatalogs catalogs = acc.packageManager(path) == PackageManager.Pnpm ? acc.of(path) : null;
            String value = catalogs == null ? version : pnpmAdd(acc, ctx, path, catalogs);
            if (value == null) {
                continue;
            }
            acc.manifestEdits.put(path, doc -> PackageJsonHelper.addDependency(doc, packageName, value,
                    targetScope()));
            if (catalogs != null && catalogs.isCleanupUnusedCatalogs()) {
                Json.Document added = PackageJsonHelper.addDependency((Json.Document) pkg, packageName, value,
                        targetScope());
                List<Map<String, String>> declarations = acc.declarations(ctx, catalogs.getPath(),
                        singletonMap(path, added));
                acc.edit(path, c -> c.withoutUnused(declarations));
            }
        }
        return emptyList();
    }

    // what pnpm add writes under catalogMode: catalog: when the default catalog holds the version, else per mode
    private @Nullable String pnpmAdd(NodeCatalogs.Accumulator acc, ExecutionContext ctx, Path path,
                                     NodeCatalogs catalogs) {
        String entry = catalogs.specifier(NodeCatalogs.DEFAULT, packageName);
        if (version.equals(entry)) {
            return CATALOG;
        }
        String mode = catalogs.getCatalogMode();
        if ("manual".equals(mode)) {
            return version;
        }
        if (entry == null) {
            acc.edit(path, c -> c.withEntry(NodeCatalogs.DEFAULT, packageName, version));
            return CATALOG;
        }
        if (NodeSemver.validRange(version, NodeSemver.Options.DEFAULT) != null &&
            NodeSemver.validRange(entry, NodeSemver.Options.DEFAULT) != null) {
            try {
                if (NodeSemver.compare(version, entry, NodeSemver.Options.DEFAULT) == 0) {
                    return CATALOG;
                }
            } catch (IllegalArgumentException e) {
                return reject(acc, ctx, path, "pnpm fails on this edit: TypeError: Invalid Version");
            }
        }
        if ("strict".equals(mode)) {
            return reject(acc, ctx, path, "pnpm rejects this edit: [ERR_PNPM_CATALOG_VERSION_MISMATCH] Wanted " +
                                          "dependency outside the version range defined in catalog");
        }
        return version;
    }

    private @Nullable String reject(NodeCatalogs.Accumulator acc, ExecutionContext ctx, Path path, String detail) {
        NodeLockRegenerationFailures.Row row = new NodeLockRegenerationFailures.Row(path.toString(), packageName,
                LockFileRegeneration.Reason.MALFORMED_MANIFEST.toString(), detail);
        if (acc.rows.add(row)) {
            lockRegenerationFailures.insertRow(ctx, row);
        }
        acc.declines.computeIfAbsent(path, k -> new LinkedHashSet<>()).add(packageName + "@" + version +
                " was not added: " + detail);
        return null;
    }

    private boolean matchesAdd(SourceFile pkg) {
        for (Map<String, String> declared : PackageJsonHelper.declaredDependencies(pkg).values()) {
            if (declared.containsKey(packageName)) {
                return false;
            }
        }
        return true;
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
                    if (acc.manifestEdits.containsKey(p) && matchesAdd(sf)) {
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
                        if (pkg != null && acc.manifestEdits.containsKey(importer) && matchesAdd(pkg)) {
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
                Function<Json.Document, Json.Document> edit = acc.manifestEdits.get(path);
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

    private static <T extends Tree> T warn(T tree, String message) {
        return Markup.markup(tree, new Markup.Warn(randomId(), message, null));
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
