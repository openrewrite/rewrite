/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.testselection;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.DataTableStore;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.attributes.ProjectAttribute;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.table.AffectedModulesDataTable;
import org.openrewrite.table.ChangedFilesDataTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Collections.emptyList;

/**
 * Classifies a set of changed files ({@link ChangedFilesDataTable}) into the set of
 * modules that must be re-tested ({@link AffectedModulesDataTable}).
 * <p>
 * This is the first step in the {@code mod test} Phase 1 pipeline (module-level
 * selection). It does not execute tests or choose test classes — it only answers
 * the question "which modules are touched (directly or transitively) by this
 * change set?".
 * <p>
 * A module DAG is reconstructed from the LSTs themselves (not from {@code gradle}
 * or {@code mvn} invocations):
 * <ul>
 *     <li>Maven — {@link MavenResolutionResult#getModules()} on each pom</li>
 *     <li>Gradle — the {@link GradleProject} marker (preferred): each build script's
 *         marker supplies the module's Gradle path and, via
 *         {@link GradleDependencyConfiguration#getRequested()} +
 *         {@link ProjectAttribute}, its inter-project dependency edges. This path works
 *         uniformly for Groovy ({@code G.CompilationUnit}) and Kotlin DSL
 *         ({@code K.CompilationUnit}) since it does not depend on Gradle-DSL type
 *         attribution on method invocations.</li>
 *     <li>Gradle — fallback for build scripts with no marker: {@code include(...)} in
 *         {@code settings.gradle(.kts)} enumerates modules; {@code project(':...')}
 *         calls inside a {@code dependencies} block add inter-module edges (this
 *         pattern-match path requires Gradle DSL types to be attributed, so it only
 *         works for Groovy DSL reliably).</li>
 * </ul>
 * The DAG is held in memory for the duration of the recipe run and is not
 * emitted as its own data table in v1.
 *
 * @see ChangedFilesDataTable
 * @see AffectedModulesDataTable
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ClassifyAffectedModules extends ScanningRecipe<ClassifyAffectedModules.Accumulator> {

    transient AffectedModulesDataTable affectedModules = new AffectedModulesDataTable(this);

    String displayName = "Classify affected modules from a change set";

    String description = "Reads `ChangedFilesDataTable` and, using the module DAG recovered from the LSTs, " +
            "emits one `AffectedModulesDataTable` row per (module, reason) pair explaining " +
            "which modules must be re-tested.";

    @Override
    public int maxCycles() {
        // Emits rows only, does not modify LSTs; one pass is enough.
        return 1;
    }

    // ---------------------------------------------------------------------
    // Accumulator
    // ---------------------------------------------------------------------

    public static class Accumulator {
        /**
         * Module path (repo-relative, forward-slash separated; root module is "")
         * → mutable {@link ModuleInfo}.
         * Uses {@link TreeMap} for stable (shortest-first) ordering, which makes
         * "deepest-prefix" matching easy via descending iteration.
         */
        final Map<String, ModuleInfo> modulesByPath = new TreeMap<>();

        /**
         * {@code groupId:artifactId} → module path, for resolving Maven inter-module
         * dependencies after all poms have been scanned.
         */
        final Map<String, String> mavenGavToModule = new HashMap<>();

        /**
         * Normalized Gradle path (colon-prefixed, e.g. {@code :rewrite-core}; root
         * is {@code ":"}) → filesystem module path. Populated from the
         * {@link GradleProject} marker on each build script; used after scanning to
         * resolve inter-project edges collected from {@link ProjectAttribute}.
         */
        final Map<String, String> gradlePathToModule = new HashMap<>();

        /**
         * Reverse DAG: {@code dependsOnMe[X] = {Y | Y depends on X}}.
         * Populated lazily at emission time by walking {@link ModuleInfo#dependsOn}.
         */
        @Nullable Map<String, Set<String>> reverseDag;
    }

    public static class ModuleInfo {
        final String path;
        /** Modules that this module declares a dependency on (forward DAG). */
        final Set<String> dependsOn = new HashSet<>();
        /** Maven {@code groupId:artifactId} references requested by this module's pom. */
        final Set<String> mavenDependencyGavs = new HashSet<>();
        /**
         * Gradle-path references to other projects that this module depends on, collected
         * from {@link GradleProject}'s configurations via {@link ProjectAttribute}. These
         * are resolved to filesystem module paths in a post-scan step once all markers
         * have been observed.
         */
        final Set<String> gradleProjectDeps = new HashSet<>();
        /**
         * {@code true} once this module's inter-module edges have been populated from its
         * {@link GradleProject} marker. When true, the MethodMatcher-based fallback walk
         * is skipped — the marker is authoritative.
         */
        boolean hasGradleMarker;

        ModuleInfo(String path) {
            this.path = path;
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    // ---------------------------------------------------------------------
    // Scanner: build the module DAG from LSTs
    // ---------------------------------------------------------------------

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {

            private final MethodMatcher settingsIncludeMatcher =
                    new MethodMatcher("org.gradle.api.initialization.Settings include(..)", true);
            private final MethodMatcher dependenciesDslMatcher =
                    new MethodMatcher("org.gradle.api.Project dependencies(..)", true);
            private final MethodMatcher dependencyHandlerProjectMatcher =
                    new MethodMatcher("org.gradle.api.artifacts.dsl.DependencyHandler project(..)", true);

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sf = (SourceFile) tree;
                String sourcePath = normalize(sf.getSourcePath().toString());

                // --- Maven ---------------------------------------------------
                MavenResolutionResult mrr = sf.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (mrr != null) {
                    String modulePath = dirOf(sourcePath);
                    ModuleInfo info = acc.modulesByPath.computeIfAbsent(modulePath, ModuleInfo::new);
                    ResolvedPom pom = mrr.getPom();
                    if (pom != null) {
                        String gav = pom.getGroupId() + ":" + pom.getArtifactId();
                        acc.mavenGavToModule.put(gav, modulePath);
                        // Record requested dependencies by G:A so we can resolve to sibling modules
                        // after all poms have been scanned.
                        if (pom.getRequested() != null && pom.getRequested().getDependencies() != null) {
                            for (org.openrewrite.maven.tree.Dependency d : pom.getRequested().getDependencies()) {
                                if (d.getGav() != null) {
                                    info.mavenDependencyGavs.add(d.getGroupId() + ":" + d.getArtifactId());
                                }
                            }
                        }
                    }

                    // Parent/aggregator edge: record that every declared child depends on this
                    // aggregator, so a change to the aggregator cascades to all children via the
                    // reverse DAG.
                    for (MavenResolutionResult child : mrr.getModules()) {
                        ResolvedPom childPom = child.getPom();
                        if (childPom == null || childPom.getRequested() == null ||
                                childPom.getRequested().getSourcePath() == null) {
                            continue;
                        }
                        String childPath = dirOf(normalize(childPom.getRequested().getSourcePath().toString()));
                        ModuleInfo childInfo = acc.modulesByPath.computeIfAbsent(childPath, ModuleInfo::new);
                        childInfo.dependsOn.add(info.path);
                    }
                    return tree;
                }

                // --- Gradle settings -----------------------------------------
                if (sourcePath.endsWith("settings.gradle") || sourcePath.endsWith("settings.gradle.kts")) {
                    final String rootDir = dirOf(sourcePath);
                    // Ensure the root module exists even if it has no build.gradle.
                    acc.modulesByPath.computeIfAbsent(rootDir, ModuleInfo::new);

                    new org.openrewrite.java.JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext c) {
                            J.MethodInvocation m = super.visitMethodInvocation(mi, c);
                            if (settingsIncludeMatcher.matches(m)) {
                                for (String colonPath : literalStringArgs(m)) {
                                    String slashPath = gradlePathToRelativeDir(colonPath);
                                    if (slashPath == null) {
                                        continue;
                                    }
                                    String modulePath = joinPath(rootDir, slashPath);
                                    acc.modulesByPath.computeIfAbsent(modulePath, ModuleInfo::new);
                                }
                            }
                            return m;
                        }
                    }.visit(sf, ctx);
                    return tree;
                }

                // --- Gradle build --------------------------------------------
                if (sourcePath.endsWith("build.gradle") || sourcePath.endsWith("build.gradle.kts")) {
                    final String modulePath = dirOf(sourcePath);
                    ModuleInfo info = acc.modulesByPath.computeIfAbsent(modulePath, ModuleInfo::new);

                    // Prefer the GradleProject marker when present: it enumerates this
                    // module and its inter-project edges without needing any Gradle DSL
                    // type attribution on the LST. Works uniformly for Groovy DSL and
                    // Kotlin DSL (K.CompilationUnit).
                    GradleProject gp = sf.getMarkers().findFirst(GradleProject.class).orElse(null);
                    if (gp != null) {
                        info.hasGradleMarker = true;
                        String normalizedGradlePath = normalizeGradlePath(gp.getPath());
                        acc.gradlePathToModule.put(normalizedGradlePath, modulePath);
                        for (GradleDependencyConfiguration cfg : gp.getNameToConfiguration().values()) {
                            if (cfg == null || cfg.getRequested() == null) {
                                continue;
                            }
                            for (Dependency d : cfg.getRequested()) {
                                if (d == null) {
                                    continue;
                                }
                                ProjectAttribute pa = d.findAttribute(ProjectAttribute.class).orElse(null);
                                if (pa == null || pa.getPath() == null) {
                                    continue;
                                }
                                String target = normalizeGradlePath(pa.getPath());
                                if (!target.equals(normalizedGradlePath)) {
                                    info.gradleProjectDeps.add(target);
                                }
                            }
                        }
                        return tree;
                    }

                    // Fallback: no marker available (e.g. isolated test parsing, or a
                    // parse failure upstream). Walk the DSL by MethodMatcher as before.
                    // This path only reliably sees inter-project edges in Groovy DSL
                    // since it depends on Gradle DSL types being attributed.
                    new org.openrewrite.java.JavaIsoVisitor<ExecutionContext>() {
                        boolean insideDependencies;

                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext c) {
                            boolean opened = false;
                            if (dependenciesDslMatcher.matches(mi)) {
                                insideDependencies = true;
                                opened = true;
                            }
                            try {
                                J.MethodInvocation m = super.visitMethodInvocation(mi, c);
                                if (insideDependencies && isProjectCall(m)) {
                                    for (String colonPath : literalStringArgs(m)) {
                                        String slashPath = gradlePathToRelativeDir(colonPath);
                                        if (slashPath == null) {
                                            continue;
                                        }
                                        // Inter-module dependency edges are always recorded with
                                        // paths relative to the same root as the current module.
                                        // Without a settings.gradle anchor we cannot know the root
                                        // with certainty; fall back to resolving against the
                                        // module's own parent directory.
                                        String rootDir = rootOfModule(modulePath);
                                        info.dependsOn.add(joinPath(rootDir, slashPath));
                                    }
                                }
                                return m;
                            } finally {
                                if (opened) {
                                    insideDependencies = false;
                                }
                            }
                        }

                        private boolean isProjectCall(J.MethodInvocation m) {
                            if (dependencyHandlerProjectMatcher.matches(m)) {
                                return true;
                            }
                            // Fallback for isolated parsed Gradle DSL where the DependencyHandler
                            // receiver isn't attributed: accept a top-level `project(':...')` call
                            // with exactly one string literal argument.
                            if (m.getSelect() == null && "project".equals(m.getSimpleName()) &&
                                    m.getArguments().size() == 1 &&
                                    m.getArguments().get(0) instanceof J.Literal) {
                                Object v = ((J.Literal) m.getArguments().get(0)).getValue();
                                return v instanceof String && ((String) v).startsWith(":");
                            }
                            return false;
                        }
                    }.visit(sf, ctx);
                    return tree;
                }

                return tree;
            }
        };
    }

    /**
     * Normalize a Gradle path for lookup: ensure a leading {@code :}, collapse
     * {@code null} and empty to the root project path {@code ":"}. So
     * {@code "rewrite-core"}, {@code ":rewrite-core"} all map to {@code ":rewrite-core"};
     * {@code ""}, {@code null}, {@code ":"} all map to {@code ":"}.
     */
    private static String normalizeGradlePath(@Nullable String p) {
        if (p == null || p.isEmpty() || ":".equals(p)) {
            return ":";
        }
        return p.startsWith(":") ? p : ":" + p;
    }

    // ---------------------------------------------------------------------
    // Emission
    // ---------------------------------------------------------------------

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        DataTableStore store = DataTableExecutionContextView.view(ctx).getDataTableStore();
        List<ChangedFilesDataTable.Row> changes = store.getRows(ChangedFilesDataTable.class)
                .map(r -> (ChangedFilesDataTable.Row) r)
                .collect(java.util.stream.Collectors.toList());

        if (changes.isEmpty()) {
            return emptyList();
        }
        if (acc.modulesByPath.isEmpty()) {
            // No build-file markers landed in the LST corpus. This happens on
            // single-module repos where `mod build` omitted the root build.gradle[.kts]
            // / pom.xml (observed on openrewrite/rewrite-migrate-java and junit-team/junit4).
            // Fall back to a synthetic root module so source-tree edits still get
            // attributed rather than silently producing an empty test plan.
            // Multi-module repos retain their real modulesByPath population from the
            // scanner; this branch only triggers when markers are entirely absent.
            for (ChangedFilesDataTable.Row change : changes) {
                String path = normalize(change.getPath());
                if (path.contains("/src/") || path.startsWith("src/")) {
                    acc.modulesByPath.put("", new ModuleInfo(""));
                    break;
                }
            }
            if (acc.modulesByPath.isEmpty()) {
                return emptyList();
            }
        }

        // Resolve Maven sibling dependencies: for each module's collected G:A refs,
        // if that G:A corresponds to another module in this repo, record a forward
        // edge (thisModule depends on siblingModule). This is skipped cleanly when
        // a G:A refers to an external dep not tracked in this repo.
        for (ModuleInfo info : acc.modulesByPath.values()) {
            for (String ga : info.mavenDependencyGavs) {
                String siblingPath = acc.mavenGavToModule.get(ga);
                if (siblingPath != null && !siblingPath.equals(info.path)) {
                    info.dependsOn.add(siblingPath);
                }
            }
        }

        // Resolve Gradle inter-project edges collected from GradleProject markers.
        // The target Gradle path is mapped to a filesystem module path using the
        // gradlePathToModule table built during scanning. Unresolvable paths
        // (targets whose own build.gradle failed to parse or produce a marker) are
        // silently skipped.
        for (ModuleInfo info : acc.modulesByPath.values()) {
            for (String gradlePath : info.gradleProjectDeps) {
                String target = acc.gradlePathToModule.get(gradlePath);
                if (target != null && !target.equals(info.path)) {
                    info.dependsOn.add(target);
                }
            }
        }

        // Resolve the root module once. It's the module whose path is a prefix of all
        // other discovered modules' paths; the empty-string path wins if present.
        String rootModule = inferRootModule(acc.modulesByPath.keySet());

        // Deduplicate emitted (module, reason) pairs so multiple changes don't produce
        // identical rows.
        Set<String> emitted = new HashSet<>();

        for (ChangedFilesDataTable.Row change : changes) {
            classify(acc, rootModule, change.getPath(), emitted, ctx);
        }
        return emptyList();
    }

    private void classify(Accumulator acc, String rootModule, String rawPath,
                          Set<String> emitted, ExecutionContext ctx) {
        String path = normalize(rawPath);
        String owning = findOwningModule(acc.modulesByPath.keySet(), path);

        // "Within the root module" means: either no module owns this path, or the
        // owner *is* the root module. Anything deeper belongs to a submodule.
        boolean atRepoRoot = owning == null || owning.equals(rootModule);
        String pathWithinRoot = stripPrefix(path, rootModule);

        if (atRepoRoot) {
            // Source files directly at the repo root (single-module layout where
            // sources live at <repo>/src/main/java/... rather than under a subdir)
            // belong to the root module itself. Attribute them as source-changes
            // rather than dropping through to classifyRepoRootPath, which would
            // see an "unknown root file" and emit a full-repo bailout.
            if (pathWithinRoot.startsWith("src/")) {
                emit(acc, rootModule, "source-changed", path, "", emitted, ctx);
                for (String downstream : transitiveDependentsOf(acc, rootModule)) {
                    emit(acc, downstream, "module-dep-of-affected", path, rootModule, emitted, ctx);
                }
                return;
            }
            RepoRootKind kind = classifyRepoRootPath(pathWithinRoot);
            switch (kind) {
                case NO_OP:
                    return;
                case FALLBACK:
                case UNKNOWN:
                    emitFullRepoFallback(acc, pathWithinRoot, emitted, ctx);
                    return;
            }
        }

        // We're inside a non-root module.
        String relativeToModule = stripPrefix(path, owning);
        String fileName = fileNameOf(relativeToModule);
        if (isBuildFileName(fileName) && !relativeToModule.contains("/")) {
            emit(acc, owning, "build-file-changed", path, "", emitted, ctx);
            for (String downstream : transitiveDependentsOf(acc, owning)) {
                emit(acc, downstream, "module-dep-of-affected", path, owning, emitted, ctx);
            }
            return;
        }
        if (isUnderSrcTree(relativeToModule)) {
            emit(acc, owning, "source-changed", path, "", emitted, ctx);
            for (String downstream : transitiveDependentsOf(acc, owning)) {
                emit(acc, downstream, "module-dep-of-affected", path, owning, emitted, ctx);
            }
            return;
        }

        // File is in a module directory but not recognized as source or build file.
        // Conservative: treat like a build-file change to the owning module — cascade
        // to transitive dependents so nothing gets silently dropped.
        emit(acc, owning, "source-changed", path, "", emitted, ctx);
        for (String downstream : transitiveDependentsOf(acc, owning)) {
            emit(acc, downstream, "module-dep-of-affected", path, owning, emitted, ctx);
        }
    }

    /**
     * Emit one {@link AffectedModulesDataTable.Row}. Dedup is on
     * {@code (module, reason)} — only the first observed {@code (triggerPath, via)}
     * wins for a given (module, reason) pair, keeping row count proportional to
     * the module DAG rather than the changed-files set. Downstream consumers
     * dedupe further when assembling the test plan.
     */
    private void emit(Accumulator acc, String modulePath, String reason,
                      String triggerPath, String via,
                      Set<String> emitted, ExecutionContext ctx) {
        // Only emit for modules we actually know about.
        if (!acc.modulesByPath.containsKey(modulePath)) {
            return;
        }
        String key = modulePath + "\0" + reason;
        if (emitted.add(key)) {
            affectedModules.insertRow(ctx,
                    new AffectedModulesDataTable.Row(modulePath, reason, triggerPath, via));
        }
    }

    private void emitFullRepoFallback(Accumulator acc, String triggerPath,
                                      Set<String> emitted, ExecutionContext ctx) {
        String reason = "repo-root-bailout:" + triggerPath;
        for (String modulePath : acc.modulesByPath.keySet()) {
            String key = modulePath + "\0" + reason;
            if (emitted.add(key)) {
                affectedModules.insertRow(ctx,
                        new AffectedModulesDataTable.Row(modulePath, reason, triggerPath, ""));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Repo-root file classification
    // ---------------------------------------------------------------------

    private enum RepoRootKind { NO_OP, FALLBACK, UNKNOWN }

    /**
     * @param p path relative to the repo-root directory (i.e. with the root module
     *          prefix stripped). A truly-at-root file has no slash before the
     *          first path component.
     */
    private static RepoRootKind classifyRepoRootPath(String p) {
        if (p.isEmpty()) {
            return RepoRootKind.UNKNOWN;
        }
        String fileName = fileNameOf(p);
        boolean directlyAtRoot = !p.contains("/");

        // --- Known no-op docs / meta files ----------------------------------
        if (directlyAtRoot) {
            if (startsWithIgnoreCase(fileName, "README") ||
                    startsWithIgnoreCase(fileName, "CHANGELOG") ||
                    startsWithIgnoreCase(fileName, "LICENSE") ||
                    startsWithIgnoreCase(fileName, "NOTICE") ||
                    ".gitignore".equals(fileName) ||
                    ".gitattributes".equals(fileName) ||
                    ".editorconfig".equals(fileName) ||
                    "CODEOWNERS".equals(fileName)) {
                return RepoRootKind.NO_OP;
            }
        }
        if (p.startsWith("docs/") || p.equals("docs")) {
            return RepoRootKind.NO_OP;
        }

        // --- Known full-repo fallback triggers ------------------------------
        if (p.startsWith(".moderne/") && p.endsWith(".yml")) {
            return RepoRootKind.FALLBACK;
        }
        if (p.startsWith(".github/workflows/")) {
            return RepoRootKind.FALLBACK;
        }
        if (p.equals(".gitlab-ci.yml")) {
            return RepoRootKind.FALLBACK;
        }
        if (p.startsWith("gradle/") || p.equals("gradle")) {
            return RepoRootKind.FALLBACK;
        }
        if (directlyAtRoot) {
            if ("gradle-wrapper.properties".equals(fileName) ||
                    "gradlew".equals(fileName) ||
                    "gradlew.bat".equals(fileName) ||
                    "gradle.properties".equals(fileName) ||
                    "settings.gradle".equals(fileName) ||
                    "settings.gradle.kts".equals(fileName) ||
                    "build.gradle".equals(fileName) ||
                    "build.gradle.kts".equals(fileName) ||
                    "pom.xml".equals(fileName) ||
                    "package.json".equals(fileName) ||
                    "Makefile".equals(fileName) ||
                    fileName.startsWith("Dockerfile")) {
                return RepoRootKind.FALLBACK;
            }
        }

        return RepoRootKind.UNKNOWN;
    }

    // ---------------------------------------------------------------------
    // DAG utilities
    // ---------------------------------------------------------------------

    private Set<String> transitiveDependentsOf(Accumulator acc, String module) {
        Map<String, Set<String>> reverse = acc.reverseDag;
        if (reverse == null) {
            reverse = buildReverseDag(acc);
            acc.reverseDag = reverse;
        }
        Set<String> visited = new HashSet<>();
        List<String> stack = new ArrayList<>();
        stack.add(module);
        while (!stack.isEmpty()) {
            String cur = stack.remove(stack.size() - 1);
            Set<String> dependents = reverse.get(cur);
            if (dependents == null) {
                continue;
            }
            for (String dep : dependents) {
                if (visited.add(dep)) {
                    stack.add(dep);
                }
            }
        }
        return visited;
    }

    private static Map<String, Set<String>> buildReverseDag(Accumulator acc) {
        Map<String, Set<String>> reverse = new HashMap<>();
        for (ModuleInfo info : acc.modulesByPath.values()) {
            for (String target : info.dependsOn) {
                reverse.computeIfAbsent(target, k -> new HashSet<>()).add(info.path);
            }
        }
        return reverse;
    }

    // ---------------------------------------------------------------------
    // Path / naming utilities
    // ---------------------------------------------------------------------

    private static String normalize(String p) {
        return p.replace('\\', '/');
    }

    private static String dirOf(String path) {
        int idx = path.lastIndexOf('/');
        return idx < 0 ? "" : path.substring(0, idx);
    }

    private static String fileNameOf(String path) {
        int idx = path.lastIndexOf('/');
        return idx < 0 ? path : path.substring(idx + 1);
    }

    private static String joinPath(String a, String b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + "/" + b;
    }

    private static String stripPrefix(String path, @Nullable String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return path;
        }
        if (path.equals(prefix)) {
            return "";
        }
        if (path.startsWith(prefix + "/")) {
            return path.substring(prefix.length() + 1);
        }
        return path;
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.length() >= prefix.length() &&
                s.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    private static boolean isBuildFileName(String fileName) {
        return fileName.startsWith("build.gradle") ||
                fileName.startsWith("settings.gradle") ||
                "pom.xml".equals(fileName) ||
                "gradle.properties".equals(fileName);
    }

    private static boolean isUnderSrcTree(String relativeToModule) {
        return relativeToModule.startsWith("src/");
    }

    /**
     * Convert a Gradle colon-path like {@code ":foo:bar"} or {@code "foo:bar"} to a
     * slash-path {@code "foo/bar"} suitable for joining with the settings.gradle's
     * directory.
     */
    private static @Nullable String gradlePathToRelativeDir(String gradlePath) {
        if (gradlePath == null || gradlePath.isEmpty()) {
            return null;
        }
        String trimmed = gradlePath.startsWith(":") ? gradlePath.substring(1) : gradlePath;
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replace(':', '/');
    }

    private static String rootOfModule(String modulePath) {
        // Best-effort parent-of-parent heuristic for inter-module edges: assume the
        // module's immediate parent dir is the Gradle root. This is only used when
        // no settings.gradle is observed.
        int idx = modulePath.lastIndexOf('/');
        return idx < 0 ? "" : modulePath.substring(0, idx);
    }

    /**
     * Find the deepest module whose path is a prefix of {@code filePath}. Returns
     * {@code null} if the root module's path is not "" and doesn't match.
     */
    private static @Nullable String findOwningModule(Set<String> modulePaths, String filePath) {
        String best = null;
        int bestLen = -1;
        for (String mp : modulePaths) {
            if (mp.isEmpty()) {
                if (bestLen < 0) {
                    best = "";
                    bestLen = 0;
                }
                continue;
            }
            if (filePath.equals(mp) || filePath.startsWith(mp + "/")) {
                if (mp.length() > bestLen) {
                    best = mp;
                    bestLen = mp.length();
                }
            }
        }
        return best;
    }

    /**
     * Pick the module that serves as the repository root: the module whose path is
     * a prefix of every other discovered module path. Breaks ties by shortest path.
     * Falls back to the lexicographically-first module path.
     */
    private static String inferRootModule(Set<String> paths) {
        if (paths.contains("")) {
            return "";
        }
        List<String> sorted = new ArrayList<>(paths);
        Collections.sort(sorted);
        String candidate = sorted.get(0);
        for (String p : sorted) {
            if (!p.equals(candidate) && !p.startsWith(candidate + "/")) {
                // Not a clean single-root layout. Fall back to the shortest path we saw.
                return candidate;
            }
        }
        return candidate;
    }

    /**
     * Extract the string values of literal arguments to a method invocation, skipping
     * non-literal or non-string arguments.
     */
    private static List<String> literalStringArgs(J.MethodInvocation m) {
        List<String> out = new ArrayList<>();
        for (org.openrewrite.java.tree.Expression arg : m.getArguments()) {
            if (arg instanceof J.Literal) {
                Object v = ((J.Literal) arg).getValue();
                if (v instanceof String) {
                    out.add((String) v);
                }
            }
        }
        return out;
    }
}
