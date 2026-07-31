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
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddDependency extends ScanningRecipe<NodeDependencyScan.Accumulator> {

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
        return "Add an npm dependency to `package.json` and regenerate the lock file by running the " +
                "package manager. If the dependency already exists in any scope, the recipe is a no-op. " +
                "Not safe to use as a precondition: invokes the package manager and publishes per-project " +
                "state shared with other dependency recipes.";
    }

    private String targetScope() { return scope == null ? "dependencies" : scope; }

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

    private boolean matchesAdd(SourceFile pkg) {
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null) return false;
        return notIn(marker.getDependencies()) && notIn(marker.getDevDependencies())
                && notIn(marker.getPeerDependencies()) && notIn(marker.getOptionalDependencies())
                && notIn(marker.getBundledDependencies());
    }

    private boolean notIn(@Nullable List<Dependency> deps) {
        if (deps == null) return true;
        for (Dependency d : deps) if (packageName.equals(d.getName())) return false;
        return true;
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
                    if (matchesAdd(sf)) {
                        ensureComputed(ps, sf, ctx);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = ps.modifiedPackageJson;
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            recordFailure(ctx, ps, p);
                            out = Markup.warn(out, new RuntimeException(
                                    "lock regeneration failed: " + ps.regenResult.getErrorMessage()));
                        }
                        PackageJsonHelper.putLiveTree(ctx, p, out);
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
                        if (pkg != null && matchesAdd(pkg)) {
                            ensureComputed(ips, pkg, ctx);
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

            private void ensureComputed(NodeDependencyScan.ProjectState ps, SourceFile pkg, ExecutionContext ctx) {
                if (ps.modifiedPackageJson != null) return;
                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg,
                        doc -> PackageJsonHelper.addDependency(doc, packageName, version, targetScope()),
                        ps.capturedLockContent,
                        ctx);
                if (r.isChanged()) {
                    ps.modifiedPackageJson = r.getModifiedPackageJson();
                    ps.regenResult = r.getRegenResult();
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
