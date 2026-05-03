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
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersion extends ScanningRecipe<UpgradeDependencyVersion.Accumulator> {

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
                "regenerates the lock file by running the package manager. Matching is by exact package " +
                "name or glob pattern. " +
                "v1 uses simple string inequality for the upgrade check (always overwrites). A future " +
                "version will use semver to skip already-up-to-date constraints. " +
                "Not safe to use as a precondition: invokes the package manager and publishes per-project " +
                "state shared with other dependency recipes.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "options",
                "At least one of `packageName` or `packagePattern` must be specified.",
                this,
                r -> r.packageName != null || r.packagePattern != null));
    }

    static class Accumulator {
        final Map<Path, ProjectState> projects = new HashMap<>();
        final Map<Path, Path> lockToPackage = new HashMap<>();
    }

    static class ProjectState {
        @Nullable SourceFile capturedPackageJson;
        @Nullable String capturedLockContent;
        @Nullable Map<String, String> configFiles;
        @Nullable List<MatchedDependency> matchedDeps;
        @Nullable SourceFile modifiedPackageJson;
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
                    ps.matchedDeps = findMatches(sf);
                }
                return tree;
            }
        };
    }

    private List<MatchedDependency> findMatches(SourceFile pkg) {
        NodeResolutionResult marker = pkg.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null) return Collections.emptyList();
        Predicate<String> nameMatcher = buildNameMatcher();
        List<MatchedDependency> result = new ArrayList<>();
        collectMatches(result, "dependencies", marker.getDependencies(), nameMatcher);
        collectMatches(result, "devDependencies", marker.getDevDependencies(), nameMatcher);
        collectMatches(result, "peerDependencies", marker.getPeerDependencies(), nameMatcher);
        collectMatches(result, "optionalDependencies", marker.getOptionalDependencies(), nameMatcher);
        collectMatches(result, "bundledDependencies", marker.getBundledDependencies(), nameMatcher);
        return result;
    }

    private Predicate<String> buildNameMatcher() {
        if (packageName != null) {
            return packageName::equals;
        }
        Pattern p = PackageJsonHelper.compileGlobPattern(packagePattern);
        return s -> p.matcher(s).matches();
    }

    private void collectMatches(List<MatchedDependency> out, String scopeName,
                                @Nullable List<Dependency> deps, Predicate<String> nameMatcher) {
        if (deps == null) return;
        for (Dependency d : deps) {
            if (nameMatcher.test(d.getName())) {
                // TODO: add semver.gt check (matches TS shouldUpgrade); for v1 we always set the new version.
                String currentVersion = d.getVersionConstraint() == null ? "" : d.getVersionConstraint();
                if (!newVersion.equals(currentVersion)) {
                    out.add(new MatchedDependency(d.getName(), scopeName, currentVersion));
                }
            }
        }
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
                    if (ps.matchedDeps != null && !ps.matchedDeps.isEmpty()) {
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
                    if (pkg != null && lockPs.matchedDeps != null && !lockPs.matchedDeps.isEmpty()) {
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
                if (ps.matchedDeps == null || ps.matchedDeps.isEmpty()) return;
                List<MatchedDependency> matches = ps.matchedDeps;
                PackageJsonHelper.EditAndRegenerateResult r = PackageJsonHelper.editAndRegenerate(
                        pkg,
                        doc -> PackageJsonHelper.upgradeVersion(doc, matches, newVersion),
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
