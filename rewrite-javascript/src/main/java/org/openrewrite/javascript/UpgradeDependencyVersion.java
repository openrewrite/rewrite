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
import org.openrewrite.javascript.internal.MatchedDependency;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersion extends ScanningRecipe<NodeCatalogs.Accumulator> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

    @Option(displayName = "Package name",
            description = "Exact package name to match. Mutually exclusive with `packagePattern`; " +
                    "at least one is required.",
            example = "lodash",
            required = false)
    @Nullable String packageName;

    @Option(displayName = "Package pattern",
            description = "Glob pattern matching package names (e.g., `@types/*`). " +
                    "Mutually exclusive with `packageName`; at least one is required.",
            example = "@types/*",
            required = false)
    @Nullable String packagePattern;

    @Option(displayName = "New version",
            description = "The new version constraint to set on matching dependencies.",
            example = "^5.0.0")
    String newVersion;

    @Override public String getDisplayName() { return "Upgrade npm dependency version"; }

    @Override public String getDescription() {
        return "Upgrades the version constraint of matching npm dependencies in `package.json` and regenerates the " +
                "lock file natively, without executing the package manager. Matching is by exact package name or " +
                "glob pattern. A `catalog:` reference is upgraded in its catalog entry. v1 uses simple string " +
                "inequality for the upgrade check (always overwrites). A future version will use semver to skip " +
                "already-up-to-date constraints. Not safe to use as a precondition: consults the package registry " +
                "over the network and publishes per-project state shared with other dependency recipes.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "options",
                "At least one of `packageName` or `packagePattern` must be specified.",
                this,
                r -> r.packageName != null || r.packagePattern != null));
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
        Predicate<String> nameMatcher = buildNameMatcher();
        for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : new TreeMap<>(acc.scan.projects).entrySet()) {
            SourceFile pkg = project.getValue().capturedPackageJson;
            if (pkg == null) {
                continue;
            }
            List<MatchedDependency> matches = new ArrayList<>();
            Map<String, Map<String, String>> declared = PackageJsonHelper.declaredDependencies(pkg);
            for (Map.Entry<String, Map<String, String>> scope : declared.entrySet()) {
                for (Map.Entry<String, String> dependency : scope.getValue().entrySet()) {
                    String name = dependency.getKey();
                    String value = dependency.getValue();
                    if (!nameMatcher.test(name) || newVersion.equals(value)) {
                        continue;
                    }
                    Spec spec = NodeCatalogs.classify(name, value);
                    if (spec != null && spec.isVersionSpec()) {
                        matches.add(new MatchedDependency(name, scope.getKey(), value));
                    } else if (spec == null || spec.getType() != Spec.Type.CATALOG ||
                               !followCatalog(acc, project.getKey(), name, value)) {
                        String protocol = spec == null || spec.getProtocol() == null ? "" : spec.getProtocol();
                        NodeDependencyProtocolsSkipped.Row row = new NodeDependencyProtocolsSkipped.Row(
                                project.getKey().toString(), project.getKey().toString(), name, scope.getKey(),
                                protocol, value, newVersion);
                        if (acc.rows.add(row)) {
                            protocolsSkipped.insertRow(ctx, row);
                        }
                    }
                }
            }
            project.getValue().matchedDeps = matches;
        }
        return emptyList();
    }

    private boolean followCatalog(NodeCatalogs.Accumulator acc, Path packageJson, String name, String value) {
        NodeCatalogs catalogs = acc.of(packageJson);
        String catalog = catalogs == null ? null : catalogs.catalogOf(value);
        NodeCatalogs edited = catalog == null ? null : catalogs.withSpecifier(catalog, name, newVersion);
        if (edited == null) {
            return false;
        }
        if (edited != catalogs) {
            acc.edit(packageJson, c -> c.withSpecifier(catalog, name, newVersion));
        }
        return true;
    }

    private List<MatchedDependency> findMatches(SourceFile pkg) {
        Predicate<String> nameMatcher = buildNameMatcher();
        List<MatchedDependency> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> scope : PackageJsonHelper.declaredDependencies(pkg).entrySet()) {
            for (Map.Entry<String, String> dependency : scope.getValue().entrySet()) {
                Spec spec = NodeCatalogs.classify(dependency.getKey(), dependency.getValue());
                if (nameMatcher.test(dependency.getKey()) && !newVersion.equals(dependency.getValue()) &&
                    spec != null && spec.isVersionSpec()) {
                    result.add(new MatchedDependency(dependency.getKey(), scope.getKey(), dependency.getValue()));
                }
            }
        }
        return result;
    }

    private Predicate<String> buildNameMatcher() {
        if (packageName != null) {
            return packageName::equals;
        }
        Pattern p = PackageJsonHelper.compileGlobPattern(packagePattern);
        return s -> p.matcher(s).matches();
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
                    SourceFile effectiveSf = sf;
                    SourceFile liveTree = PackageJsonHelper.getLiveTree(ctx, p);
                    if (liveTree != null) {
                        effectiveSf = liveTree;
                    }
                    // a prior recipe (e.g. AddDependency) may have added the dep in this cycle
                    if (ps.matchedDeps != null && ps.matchedDeps.isEmpty() && liveTree != null) {
                        ps.matchedDeps = findMatches(liveTree);
                    }
                    if (ps.matchedDeps != null && !ps.matchedDeps.isEmpty() || holdsCatalogEdits(acc, p)) {
                        ensureComputed(ps, effectiveSf, ctx);
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
                        if (ips.matchedDeps != null && ips.matchedDeps.isEmpty() && pkg != null) {
                            ips.matchedDeps = findMatches(pkg);
                        }
                        if (pkg != null && (ips.matchedDeps != null && !ips.matchedDeps.isEmpty() ||
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
                    return warn(sf, lps.regenResult);
                }
                return tree;
            }

            private void ensureComputed(NodeDependencyScan.ProjectState ps, SourceFile pkg, ExecutionContext ctx) {
                Path path = pkg.getSourcePath();
                if (acc.results.containsKey(path)) return;
                List<MatchedDependency> matches = ps.matchedDeps == null ? emptyList() : ps.matchedDeps;
                Function<Json.Document, Json.Document> edit = doc ->
                        acc.withCatalogEdits(PackageJsonHelper.upgradeVersion(doc, matches, newVersion));
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

    // a Bun root manifest is the catalog file its members' references are followed into
    private static boolean holdsCatalogEdits(NodeCatalogs.Accumulator acc, Path packageJson) {
        NodeCatalogs catalogs = acc.of(packageJson);
        return catalogs != null && catalogs.getPath().equals(packageJson) && acc.editsCatalogsOf(packageJson);
    }

    private static <T extends Tree> T warn(T tree, LockFileRegeneration.Result result) {
        return Markup.markup(tree, new Markup.Warn(randomId(), "lock regeneration failed: " +
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
