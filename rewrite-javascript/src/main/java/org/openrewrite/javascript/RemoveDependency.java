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
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDependency extends ScanningRecipe<NodeCatalogs.Accumulator> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);

    @Option(displayName = "Package name",
            description = "The name of the npm package to remove (e.g., `lodash`, `@types/node`).",
            example = "lodash")
    String packageName;

    @Option(displayName = "Scope",
            description = "The dependency scope to remove from: `dependencies`, `devDependencies`, " +
                    "`peerDependencies`, `optionalDependencies`, or `bundledDependencies`. If not specified, removes " +
                    "from all scopes.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies",
                    "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Override public String getDisplayName() { return "Remove npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Remove an npm dependency from `package.json` and regenerate the lock file natively, without " +
                "executing the package manager. A pnpm workspace with `cleanupUnusedCatalogs` also loses the catalog " +
                "entries no project references any more. If the dependency does not exist in any scope, the recipe " +
                "is a no-op.";
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
        Map<Path, SourceFile> removed = new LinkedHashMap<>();
        Map<Path, Path> cleaned = new LinkedHashMap<>();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : new TreeMap<>(acc.scan.projects).entrySet()) {
            SourceFile pkg = project.getValue().capturedPackageJson;
            Set<String> scopes = pkg == null ? null : findContainingScopes(pkg);
            if (!(pkg instanceof Json.Document) || scopes == null) {
                continue;
            }
            Path path = project.getKey();
            project.getValue().scopesContainingPackage = scopes;
            acc.manifestEdits.put(path, doc -> PackageJsonHelper.removeDependency(doc, packageName, scopes));
            removed.put(path, PackageJsonHelper.removeDependency((Json.Document) pkg, packageName, scopes));
            NodeCatalogs catalogs = acc.of(path);
            if (catalogs != null && catalogs.isCleanupUnusedCatalogs()) {
                cleaned.putIfAbsent(catalogs.getPath(), path);
            }
        }
        for (Map.Entry<Path, Path> workspace : cleaned.entrySet()) {
            List<Map<String, String>> declarations = acc.declarations(ctx, workspace.getKey(), removed);
            acc.edit(workspace.getValue(), c -> c.withoutUnused(declarations));
        }
        return emptyList();
    }

    private @Nullable Set<String> findContainingScopes(SourceFile pkg) {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, String>> declared : PackageJsonHelper.declaredDependencies(pkg).entrySet()) {
            if (declared.getValue().containsKey(packageName) && (scope == null || scope.equals(declared.getKey()))) {
                result.add(declared.getKey());
            }
        }
        return result.isEmpty() ? null : result;
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
                    if (acc.manifestEdits.containsKey(p) && findContainingScopes(sf) != null) {
                        ensureComputed(ps, sf, ctx);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = NodeDependencyScan.modifiedFor(ps, sf);
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p);
                            return warn(out, ps.regenResult);
                        }
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
                        if (pkg != null && acc.manifestEdits.containsKey(importer) &&
                            findContainingScopes(pkg) != null) {
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
                    return warn(sf, lps.regenResult);
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

    private static <T extends Tree> T warn(T tree, LockFileRegeneration.Result result) {
        return Markup.markup(tree, new Markup.Warn(Tree.randomId(), "lock regeneration failed: " +
                                                                    result.getErrorMessage(), null));
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
