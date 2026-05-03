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
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDependency extends ScanningRecipe<RemoveDependency.Accumulator> {

    @Option(displayName = "Package name",
            description = "The name of the npm package to remove (e.g., `lodash`, `@types/node`).",
            example = "lodash")
    String packageName;

    @Option(displayName = "Scope",
            description = "The dependency scope to remove from: `dependencies`, `devDependencies`, `peerDependencies`, " +
                    "`optionalDependencies`, or `bundledDependencies`. If not specified, removes from all scopes.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies", "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Override public String getDisplayName() { return "Remove npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Remove an npm dependency from `package.json` and regenerate the lock file. " +
                "If the dependency does not exist in any scope, the recipe is a no-op.";
    }

    static class Accumulator {
        final Map<Path, ProjectState> projects = new HashMap<>();
        final Map<Path, Path> lockToPackage = new HashMap<>();
    }

    static class ProjectState {
        @Nullable SourceFile capturedPackageJson;
        @Nullable String capturedLockContent;
        @Nullable Map<String, String> configFiles;
        @Nullable SourceFile modifiedPackageJson;
        @Nullable Set<String> scopesContainingPackage;
        LockFileRegeneration.@Nullable Result regenResult;
    }

    @Override public Accumulator getInitialValue(ExecutionContext ctx) { return new Accumulator(); }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
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
                        ProjectState ps = acc.projects.computeIfAbsent(packagePath, k -> new ProjectState());
                        ps.capturedLockContent = sf.printAll();
                        acc.lockToPackage.put(p, packagePath);
                    }
                    return tree;
                }
                if (sf instanceof Json.Document && "package.json".equals(basename)) {
                    NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
                    if (marker == null) return tree;
                    ProjectState ps = acc.projects.computeIfAbsent(p, k -> new ProjectState());
                    ps.capturedPackageJson = sf;
                    ps.configFiles = PackageJsonHelper.serializeConfigFiles(marker);
                }
                return tree;
            }
        };
    }

    private @Nullable Set<String> findContainingScopes(SourceFile pkg) {
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null) return null;
        Set<String> result = new LinkedHashSet<>();
        addIfPresent(result, "dependencies", marker.getDependencies());
        addIfPresent(result, "devDependencies", marker.getDevDependencies());
        addIfPresent(result, "peerDependencies", marker.getPeerDependencies());
        addIfPresent(result, "optionalDependencies", marker.getOptionalDependencies());
        addIfPresent(result, "bundledDependencies", marker.getBundledDependencies());
        if (scope != null) result.retainAll(Collections.singleton(scope));
        return result.isEmpty() ? null : result;
    }

    private void addIfPresent(Set<String> out, String name, @Nullable List<Dependency> deps) {
        if (deps != null) for (Dependency d : deps) if (packageName.equals(d.getName())) { out.add(name); return; }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile sf = (SourceFile) tree;
                Path p = sf.getSourcePath();

                ProjectState ps = acc.projects.get(p);
                if (ps != null && ps.capturedPackageJson != null) {
                    if ((ps.scopesContainingPackage = findContainingScopes(sf)) != null) {
                        ensureComputed(ps, sf);
                    }
                    if (ps.modifiedPackageJson != null) {
                        SourceFile out = ps.modifiedPackageJson;
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            out = Markup.warn(out, new RuntimeException(
                                    "lock regeneration failed: " + ps.regenResult.getErrorMessage()));
                        }
                        PackageJsonHelper.putLiveTree(ctx, p, out);
                        return out;
                    }
                }

                Path packagePath = acc.lockToPackage.get(p);
                if (packagePath == null) return tree;
                ProjectState lockPs = acc.projects.get(packagePath);
                if (lockPs == null) return tree;
                if (lockPs.modifiedPackageJson == null) {
                    SourceFile pkg = PackageJsonHelper.getLiveTree(ctx, packagePath);
                    if (pkg == null) pkg = lockPs.capturedPackageJson;
                    if (pkg != null && (lockPs.scopesContainingPackage = findContainingScopes(pkg)) != null) {
                        ensureComputed(lockPs, pkg);
                        if (lockPs.modifiedPackageJson != null) {
                            PackageJsonHelper.putLiveTree(ctx, packagePath, lockPs.modifiedPackageJson);
                        }
                    }
                }
                if (lockPs.regenResult != null && lockPs.regenResult.isSuccess()) {
                    return PackageJsonHelper.reparseLock(sf, lockPs.regenResult.getLockFileContent());
                }
                return tree;
            }

            private void ensureComputed(ProjectState ps, SourceFile pkg) {
                if (ps.modifiedPackageJson != null) return;
                if (ps.scopesContainingPackage == null) return;
                Set<String> scopes = ps.scopesContainingPackage;
                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg,
                        doc -> PackageJsonHelper.removeDependency(doc, packageName, scopes),
                        ps.capturedLockContent,
                        ps.configFiles);
                if (r.isChanged()) {
                    ps.modifiedPackageJson = r.getModifiedPackageJson();
                    ps.regenResult = r.getRegenResult();
                }
            }
        };
    }
}
