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
import org.openrewrite.javascript.internal.MatchedDependency;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<NodeDependencyScan.Accumulator> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

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
        return "Pins or upgrades a transitive npm dependency by adding an override entry to `package.json` " +
                "and regenerating the lock file natively, without executing the package manager. For npm and Bun, adds to the `overrides` field; " +
                "for Yarn, adds to `resolutions`; for pnpm, adds to `pnpm.overrides`. " +
                "The override is idempotent — if the entry already exists with the same version, no change is made. " +
                "Not safe to use as a precondition: consults the package registry over the network and publishes per-project " +
                "state shared with other dependency recipes.";
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
                if (sf instanceof Json.Document && "package.json".equals(basename)) {
                    NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                    if (marker == null) return tree;
                    NodeDependencyScan.ProjectState ps = acc.projects.computeIfAbsent(p, k -> new NodeDependencyScan.ProjectState());
                    ps.capturedPackageJson = sf;
                }
                return tree;
            }
        };
    }

    private boolean canApply(SourceFile pkg) {
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        return marker != null && marker.getPackageManager() != null;
    }

    /** A second marker on a tree that already carries one is a change, and the recipe would never settle. */
    private static SourceFile warnOnce(SourceFile sf, String message) {
        return sf.getMarkers().findFirst(Markup.Warn.class).isPresent() ?
                sf :
                Markup.warn(sf, new IllegalStateException(message));
    }

    /**
     * The key this dialect writes its override into exists but is not an object, so there is nowhere to
     * write. Appending would leave the manifest with two members of that name; declining leaves it
     * valid, and the marker is what makes the decline visible in the run.
     */
    private @Nullable String unusableOverrideContainer(SourceFile sf) {
        if (!(sf instanceof Json.Document)) {
            return null;
        }
        NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || marker.getPackageManager() == null) {
            return null;
        }
        return PackageJsonOverrides.unusableOverrideContainerKey((Json.Document) sf, marker.getPackageManager());
    }

    /**
     * The declarations an override written into {@code manifestPath} would silently win over. A scoped
     * override {@code foo>acme-logger} pins only the copy under {@code foo}, so it reaches a reference
     * only when {@code foo} is itself a workspace package declaring one.
     */
    private Map<Path, List<MatchedDependency>> findProtocolSkips(NodeDependencyScan.Accumulator acc,
                                                                 ExecutionContext ctx, Path manifestPath) {
        // Gated on the parsed path, not on `dependencyPath`, so this agrees with the guard in
        // `upgradeTransitive`: a path that parses to no segments is a global override.
        List<DependencyPathSegment> parsed = parsedPath();
        return NodeDependencyScan.findProtocolReferences(acc, ctx, manifestPath, packageName,
                parsed == null || parsed.isEmpty() ? null : parsed.get(parsed.size() - 1).getName());
    }

    private @Nullable List<DependencyPathSegment> parsedPath() {
        return dependencyPath == null ? null : PackageJsonOverrides.parsePath(dependencyPath);
    }

    /**
     * Whether an override would actually be written, so a manifest that already holds the requested
     * entry is not reported as a decline: nothing was declined, there was nothing left to do.
     */
    private boolean overrideWouldBeWritten(SourceFile pkg) {
        if (!(pkg instanceof Json.Document)) {
            return false;
        }
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || marker.getPackageManager() == null) {
            return false;
        }
        Json.Document doc = (Json.Document) pkg;
        return PackageJsonOverrides.applyOverride(
                doc, marker.getPackageManager(), packageName, newVersion, parsedPath()) != doc;
    }

    /**
     * The one predicate both entry points honour. The manifest visit reports and marks the decline; the
     * lock path only obeys it, because the lock is regenerated from the manifests and a lock carrying an
     * override the manifest says was never written is worse than either alone. {@code upgradeTransitive}
     * is no backstop here: for a workspace root the reference sits in a member, and the root's own
     * document says nothing about it.
     */
    private boolean declines(NodeDependencyScan.Accumulator acc, ExecutionContext ctx,
                             Path manifestPath, SourceFile pkg) {
        return unusableOverrideContainer(pkg) != null ||
                !findProtocolSkips(acc, ctx, manifestPath).isEmpty();
    }

    /** One row per declaration the override was declined for, emitted once per project. */
    private void reportProtocolSkips(ExecutionContext ctx, NodeDependencyScan.ProjectState ps,
                                     Path manifestPath, Map<Path, List<MatchedDependency>> skips) {
        if (ps.protocolsReported || skips.isEmpty()) {
            return;
        }
        ps.protocolsReported = true;
        for (Map.Entry<Path, List<MatchedDependency>> declaring : skips.entrySet()) {
            for (MatchedDependency declaration : declaring.getValue()) {
                protocolsSkipped.insertRow(ctx, new NodeDependencyProtocolsSkipped.Row(
                        manifestPath.toString(),
                        declaring.getKey().toString(),
                        declaration.getPackageName(),
                        declaration.getDependencyScope(),
                        PackageJsonHelper.dependencySpecifierProtocol(declaration.getCurrentVersion()),
                        declaration.getCurrentVersion(),
                        newVersion));
            }
        }
    }

    /**
     * Mark the manifest with why the override was declined. A warning rather than an error: nothing is
     * broken, a requested change was declined and the file is left valid.
     */
    private SourceFile markProtocolSkips(SourceFile sf, Map<Path, List<MatchedDependency>> skips) {
        StringBuilder message = new StringBuilder();
        for (Map.Entry<Path, List<MatchedDependency>> declaring : skips.entrySet()) {
            for (MatchedDependency declaration : declaring.getValue()) {
                if (message.length() > 0) {
                    message.append(' ');
                }
                message.append("`").append(declaration.getPackageName()).append("` is declared as `")
                        .append(declaration.getCurrentVersion()).append("`, a ")
                        .append(PackageJsonHelper.dependencySpecifierProtocol(declaration.getCurrentVersion()))
                        .append(" specifier rather than a version constraint");
                // Naming the manifest is what makes the warn and the rows match up, since a row's
                // `Declared in` differs from its `Source path` exactly in this case.
                if (!declaring.getKey().equals(sf.getSourcePath())) {
                    message.append(", in the workspace member `").append(declaring.getKey()).append("`");
                }
                message.append('.');
            }
        }
        message.append(" An override beside it would silently win over whatever that specifier resolves")
                .append(" to, so none was written for `").append(packageName).append("`.");
        return warnOnce(sf, message.toString());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyScan.Accumulator acc) {
        NodeDependencyScan.linkWorkspaceMembers(acc);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();

                NodeDependencyScan.ProjectState ps = acc.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    if (canApply(sf)) {
                        String unusable = unusableOverrideContainer(sf);
                        if (unusable != null) {
                            return warnOnce(sf, "`" + unusable + "` is not an object in this `package.json`," +
                                    " so there is nowhere to write the override for `" + packageName + "`," +
                                    " and it was not written. Make `" + unusable + "` an object first.");
                        }
                        Map<Path, List<MatchedDependency>> skips = findProtocolSkips(acc, ctx, p);
                        if (!skips.isEmpty() && overrideWouldBeWritten(sf)) {
                            reportProtocolSkips(ctx, ps, p, skips);
                            return markProtocolSkips(sf, skips);
                        }
                        ensureComputed(ps, p, sf, ctx);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = NodeDependencyScan.modifiedFor(ps, sf);
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
                        if (pkg != null && canApply(pkg)) {
                            ensureComputed(ips, importer, pkg, ctx);
                            if (ips.modifiedPackageJson != null) {
                                PackageJsonHelper.putLiveTree(ctx, importer, ips.modifiedPackageJson);
                            }
                        }
                    }
                    if (ips.regenResult != null) {
                        if (ips.regenResult.isSuccess()) {
                            return PackageJsonHelper.reparseLock(sf, ips.regenResult.getLockFileContent());
                        }
                        recordFailure(ctx, ips, importer);
                        return Markup.warn(sf, new RuntimeException(
                                "lock regeneration failed: " + ips.regenResult.getErrorMessage()));
                    }
                }
                return tree;
            }

            private void ensureComputed(NodeDependencyScan.ProjectState ps, Path manifestPath,
                                        SourceFile pkg, ExecutionContext ctx) {
                if (ps.modifiedPackageJson != null) return;
                if (declines(acc, ctx, manifestPath, pkg)) return;
                NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                if (marker == null || marker.getPackageManager() == null) return;
                NodeResolutionResult.PackageManager pm = marker.getPackageManager();
                List<DependencyPathSegment> parsedPath = parsedPath();

                Function<Json.Document, Json.Document> edit = doc -> PackageJsonHelper.upgradeTransitive(doc, pm, packageName, newVersion, parsedPath);
                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg, edit, ps.capturedLockContent, ctx);
                if (r.isChanged()) {
                    ps.modifiedPackageJson = r.getModifiedPackageJson();
                    ps.regenResult = r.getRegenResult();
                    ps.editedFrom = pkg;
                    ps.edit = edit;
                }
            }
        };
    }

    private void recordFailure(ExecutionContext ctx, NodeDependencyScan.ProjectState ps, Path packageJsonPath) {
        if (ps.failureRecorded || ps.regenResult == null) {
            return;
        }
        ps.failureRecorded = true;
        LockFileRegeneration.insertFailureRow(ctx, lockRegenerationFailures, packageJsonPath, ps.regenResult, packageName);
    }
}
