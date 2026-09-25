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
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersion extends ScanningRecipe<NodeDependencyScan.Accumulator> {

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
        return "Upgrades the version constraint of matching npm dependencies in `package.json` and " +
                "regenerates the lock file. Matching is by exact package name or glob pattern. " +
                "A dependency declared as `catalog:` or `catalog:<name>` keeps its constraint in " +
                "`pnpm-workspace.yaml` or `.yarnrc.yml`, so the catalog entry is updated in place and " +
                "the manifest is left alone. That happens only when every consumer of the entry is also " +
                "being upgraded, since moving the entry moves all of them; otherwise the entry and the " +
                "manifest are both left alone and the skip is reported. The lock file cannot yet follow " +
                "a catalog edit, so one is reported as a regeneration failure rather than written " +
                "incorrectly. Other specifier protocols (`workspace:`, `patch:`, `portal:`, `npm:`) have " +
                "no such declaration to follow and are always left alone. " +
                "v1 uses simple string inequality for the upgrade check (always overwrites). A future " +
                "version will use semver to skip already-up-to-date constraints. " +
                "Not safe to use as a precondition: publishes per-project state shared with other " +
                "dependency recipes.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "options",
                "At least one of `packageName` or `packagePattern` must be specified.",
                this,
                r -> r.packageName != null || r.packagePattern != null));
    }

    @Override public NodeDependencyScan.Accumulator getInitialValue(ExecutionContext ctx) { return new NodeDependencyScan.Accumulator(); }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NodeDependencyScan.Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();
                String basename = p.getFileName().toString();

                if (PackageJsonHelper.isLockFile(basename)) {
                    if (sf instanceof Json.Document || sf instanceof Yaml.Documents || sf instanceof PlainText) {
                        Path packagePath = PackageJsonHelper.correspondingPackageJsonPath(p);
                        NodeDependencyScan.ProjectState ps = acc.projects.computeIfAbsent(packagePath, k -> new NodeDependencyScan.ProjectState());
                        ps.capturedLockContent = sf.printAll();
                        acc.lockToPackage.put(p, packagePath);
                    }
                    return tree;
                }
                if (NodeCatalogs.isWorkspaceFile(basename) && sf instanceof Yaml.Documents) {
                    acc.workspaceFiles.put(p, (Yaml.Documents) sf);
                    return tree;
                }
                if (sf instanceof Json.Document && "package.json".equals(basename)) {
                    // Recorded before the marker check: a member that never resolved still consumes
                    // whatever catalog entry its manifest references.
                    acc.manifests.add(p);
                    Map<String, String> catalogRefs = NodeCatalogs.catalogReferences((Json.Document) sf);
                    if (!catalogRefs.isEmpty()) {
                        acc.catalogRefs.put(p, catalogRefs);
                    }
                    NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                    if (marker == null) return tree;
                    NodeDependencyScan.ProjectState ps = acc.projects.computeIfAbsent(p, k -> new NodeDependencyScan.ProjectState());
                    ps.capturedPackageJson = sf;
                    ps.matchedDeps = findMatches(sf, ps.skippedProtocols);
                    acc.catalogEditsStale = true;
                }
                return tree;
            }
        };
    }

    /**
     * The dependencies this recipe can upgrade, collecting into {@code skipped} those it matched but must
     * leave alone because their version position holds a specifier protocol. {@code skipped} is cleared
     * first, so recomputing against a live tree replaces rather than appends.
     */
    private List<MatchedDependency> findMatches(SourceFile pkg, List<MatchedDependency> skipped) {
        skipped.clear();
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null) return emptyList();
        Predicate<String> nameMatcher = buildNameMatcher();
        List<MatchedDependency> result = new ArrayList<>();
        collectMatches(result, skipped, "dependencies", marker.getDependencies(), nameMatcher);
        collectMatches(result, skipped, "devDependencies", marker.getDevDependencies(), nameMatcher);
        collectMatches(result, skipped, "peerDependencies", marker.getPeerDependencies(), nameMatcher);
        collectMatches(result, skipped, "optionalDependencies", marker.getOptionalDependencies(), nameMatcher);
        collectMatches(result, skipped, "bundledDependencies", marker.getBundledDependencies(), nameMatcher);
        return result;
    }

    private Predicate<String> buildNameMatcher() {
        if (packageName != null) {
            return packageName::equals;
        }
        Pattern p = PackageJsonHelper.compileGlobPattern(packagePattern);
        return s -> p.matcher(s).matches();
    }

    private void collectMatches(List<MatchedDependency> out, List<MatchedDependency> skipped, String scopeName,
                                @Nullable List<Dependency> deps, Predicate<String> nameMatcher) {
        if (deps == null) return;
        for (Dependency d : deps) {
            if (nameMatcher.test(d.getName())) {
                // TODO: add semver.gt check (matches TS shouldUpgrade); for v1 we always set the new version.
                String currentVersion = d.getVersionConstraint() == null ? "" : d.getVersionConstraint();
                if (PackageJsonHelper.dependencySpecifierProtocol(currentVersion) != null) {
                    // The constraint lives behind the protocol, not here; writing the new version into this
                    // position would drop the reference and take the package out of whatever holds it.
                    skipped.add(new MatchedDependency(d.getName(), scopeName, currentVersion));
                } else if (!newVersion.equals(currentVersion)) {
                    out.add(new MatchedDependency(d.getName(), scopeName, currentVersion));
                }
            }
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyScan.Accumulator acc) {
        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.decideCatalogEdits(acc, newVersion);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();

                Set<NodeCatalogs.CatalogEntry> catalogEdits = acc.catalogEdits.get(p);
                if (catalogEdits != null) {
                    SourceFile edited = sf;
                    for (NodeCatalogs.CatalogEntry edit : catalogEdits) {
                        edited = NodeCatalogs.updateEntry(edited, edit.getCatalogName(),
                                edit.getPackageName(), newVersion);
                    }
                    return edited;
                }

                NodeDependencyScan.ProjectState ps = acc.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    // If scanner found no matches on the original tree, check the live tree:
                    // a prior recipe (e.g. AddDependency) may have added the dep in this cycle.
                    if (ps.matchedDeps != null && ps.matchedDeps.isEmpty()) {
                        SourceFile liveTree = PackageJsonHelper.getLiveTree(ctx, p);
                        if (liveTree != null) {
                            ps.matchedDeps = findMatches(liveTree, ps.skippedProtocols);
                            acc.catalogEditsStale = true;
                        }
                    }
                    reportProtocolSkips(ctx, acc, ps, p);
                    if (ps.matchedDeps != null && !ps.matchedDeps.isEmpty()) {
                        SourceFile effectiveSf = sf;
                        SourceFile liveTree = PackageJsonHelper.getLiveTree(ctx, p);
                        if (liveTree != null) {
                            effectiveSf = liveTree;
                        }
                        ensureComputed(ps, effectiveSf, ctx);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = ps.modifiedPackageJson;
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p);
                            return Markup.warn(out, new RuntimeException(
                                    "lock regeneration failed: " + ps.regenResult.getErrorMessage()));
                        }
                        return out;
                    }
                }

                Path packagePath = acc.lockToPackage.get(p);
                if (packagePath == null) return tree;
                NodeDependencyScan.ProjectState rootPs = acc.projects.get(packagePath);
                if (rootPs == null) return tree;

                for (Path importer : NodeDependencyScan.lockImporters(acc, packagePath, rootPs)) {
                    NodeDependencyScan.ProjectState ips = acc.projects.get(importer);
                    if (ips == null) continue;
                    if (ips.modifiedPackageJson == null) {
                        SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, importer);
                        if (pkg == null) pkg = ips.capturedPackageJson;
                        // If the scanner found no matches on the original tree, recompute from the live tree.
                        if (ips.matchedDeps != null && ips.matchedDeps.isEmpty() && pkg != null) {
                            ips.matchedDeps = findMatches(pkg, ips.skippedProtocols);
                            acc.catalogEditsStale = true;
                        }
                        reportProtocolSkips(ctx, acc, ips, importer);
                        if (pkg != null && ips.matchedDeps != null && !ips.matchedDeps.isEmpty()) {
                            ensureComputed(ips, pkg, ctx);
                            if (ips.modifiedPackageJson != null) {
                                PackageJsonHelper.putLiveTree(ctx, importer, ips.modifiedPackageJson);
                            }
                        }
                    }
                    // Two independent verdicts about one lock: what the manifest edits produced, and
                    // whether a catalog edit left it behind. Reporting the second must not discard the
                    // first, or a plain dependency bumped in the same run loses its regenerated lock.
                    LockFileRegeneration.Result stale = staleCatalogLock(ips);
                    if (ips.regenResult != null && ips.regenResult.isSuccess()) {
                        SourceFile relocked = PackageJsonHelper.reparseLock(sf, ips.regenResult.getLockFileContent());
                        if (stale == null) {
                            return relocked;
                        }
                        recordCatalogStaleness(ctx, ips, importer, stale);
                        return warnOnce(relocked, "lock regeneration incomplete: " + stale.getErrorMessage());
                    }
                    if (ips.regenResult != null) {
                        recordFailure(ctx, ips, importer);
                        return Markup.warn(sf, new RuntimeException(
                                "lock regeneration failed: " + ips.regenResult.getErrorMessage()));
                    }
                    if (stale != null) {
                        recordCatalogStaleness(ctx, ips, importer, stale);
                        return warnOnce(sf, "lock regeneration failed: " + stale.getErrorMessage());
                    }
                }
                return tree;
            }

            private void ensureComputed(NodeDependencyScan.ProjectState ps, SourceFile pkg, ExecutionContext ctx) {
                if (ps.modifiedPackageJson != null) return;
                if (ps.matchedDeps == null || ps.matchedDeps.isEmpty()) return;
                List<MatchedDependency> matches = ps.matchedDeps;
                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg,
                        doc -> PackageJsonHelper.upgradeVersion(doc, matches, newVersion),
                        ps.capturedLockContent,
                        ctx);
                if (r.isChanged()) {
                    ps.modifiedPackageJson = r.getModifiedPackageJson();
                    ps.regenResult = r.getRegenResult();
                }
            }
        };
    }

    /**
     * A catalog edit leaves this project's lock behind, and the native engine cannot update it: the lock
     * records the reference verbatim under `importers` and keeps the resolved version in a `catalogs:`
     * block it has no model for. Refuse loudly rather than leave a lock that still installs the old
     * version, which pnpm does not report (pnpm/pnpm#9369).
     */
    /**
     * A stale catalog is recomputed every cycle, but the warning must not be: a second marker on a tree
     * that already carries one is a change, and the recipe would never settle.
     */
    private static SourceFile warnOnce(SourceFile sf, String message) {
        return sf.getMarkers().findFirst(Markup.Warn.class).isPresent() ?
                sf :
                Markup.warn(sf, new RuntimeException(message));
    }

    private LockFileRegeneration.@Nullable Result staleCatalogLock(NodeDependencyScan.ProjectState ps) {
        if (ps.catalogEntriesEdited.isEmpty() || ps.capturedLockContent == null) {
            return null;
        }
        StringBuilder detail = new StringBuilder("the catalog entry was updated but the lock cannot be:");
        for (NodeCatalogs.CatalogEntry entry : ps.catalogEntriesEdited) {
            detail.append(' ').append(entry.getPackageName()).append(" resolves through ")
                    .append(NodeCatalogs.DEFAULT_CATALOG.equals(entry.getCatalogName()) ?
                            "the default catalog" : "catalog " + entry.getCatalogName()).append(';');
        }
        return LockFileRegeneration.Result.failure(new LockFileRegeneration.Failure(
                LockFileRegeneration.Reason.UNSUPPORTED_ENTRY_TYPE,
                ps.catalogEntriesEdited.get(0).getPackageName(),
                detail.toString()));
    }

    private void recordCatalogStaleness(ExecutionContext ctx, NodeDependencyScan.ProjectState ps,
                                        Path packageJsonPath, LockFileRegeneration.Result stale) {
        if (ps.catalogStalenessReported) {
            return;
        }
        ps.catalogStalenessReported = true;
        LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, stale, packageName);
    }

    /** One row per matched dependency left alone for its specifier protocol, emitted once per project. */
    private void reportProtocolSkips(ExecutionContext ctx, NodeDependencyScan.Accumulator acc,
                                     NodeDependencyScan.ProjectState ps, Path packageJsonPath) {
        if (ps.protocolsReported || ps.skippedProtocols.isEmpty()) {
            return;
        }
        ps.protocolsReported = true;
        for (MatchedDependency skipped : ps.skippedProtocols) {
            if (NodeDependencyScan.isFollowedIntoCatalog(acc, packageJsonPath, skipped)) {
                // Followed into its catalog entry, so nothing was skipped.
                continue;
            }
            protocolsSkipped.insertRow(ctx, new NodeDependencyProtocolsSkipped.Row(
                    packageJsonPath.toString(),
                    skipped.getPackageName(),
                    skipped.getDependencyScope(),
                    PackageJsonHelper.dependencySpecifierProtocol(skipped.getCurrentVersion()),
                    skipped.getCurrentVersion(),
                    newVersion));
        }
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJsonPath) {
        if (ps.failureRecorded || ps.regenResult == null) {
            return;
        }
        ps.failureRecorded = true;
        LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, ps.regenResult, packageName);
    }
}
