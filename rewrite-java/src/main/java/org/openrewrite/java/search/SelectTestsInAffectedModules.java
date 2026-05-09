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
package org.openrewrite.java.search;

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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.table.AffectedModulesDataTable;
import org.openrewrite.table.TestPlanDataTable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

/**
 * Phase 1 of the {@code mod test} test-selection pipeline: module-level
 * selection. Reads {@link AffectedModulesDataTable} rows produced upstream by
 * the git-diff/affected-modules classifier and emits a
 * {@link TestPlanDataTable} row for every test class discovered in an
 * affected module.
 * <p>
 * In Phase 1, selection is at class granularity only — {@code testMethod} is
 * always blank. Call-graph-based narrowing is out of scope.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class SelectTestsInAffectedModules extends ScanningRecipe<SelectTestsInAffectedModules.Accumulator> {

    // Fully-qualified annotation patterns. Never match by simple name (StackOverflow
    // risk on JavaType.equals is moot here since AnnotationMatcher compares by FQN,
    // but simple-name matching would also incorrectly pick up user-defined @Test
    // classes).
    private static final String JUNIT_JUPITER_TEST = "@org.junit.jupiter.api.Test";
    private static final String JUNIT_4_TEST = "@org.junit.Test";
    private static final String TESTNG_TEST = "@org.testng.annotations.Test";
    private static final String KOTLIN_TEST = "@kotlin.test.Test";

    private static final String[] TEST_ANNOTATION_PATTERNS = {
            JUNIT_JUPITER_TEST,
            JUNIT_4_TEST,
            TESTNG_TEST,
            KOTLIN_TEST,
    };

    /**
     * Spock detection is supertype-based, not annotation-based. A Groovy class
     * that extends {@code spock.lang.Specification} is a Spock test regardless
     * of whether any individual method carries a {@code @Test}-style annotation.
     */
    private static final String SPOCK_SPECIFICATION_FQN = "spock.lang.Specification";

    transient TestPlanDataTable testPlan = new TestPlanDataTable(this);

    @Override
    public String getDisplayName() {
        return "Select tests in affected modules";
    }

    @Override
    public String getDescription() {
        return "Reads the `AffectedModulesDataTable` and emits a `TestPlanDataTable` row for " +
                "every test class in each affected module. Phase 1 of the `mod test` " +
                "pipeline — selection is at class granularity (no call-graph closure).";
    }

    public static class Accumulator {
        /** Affected module paths read from the upstream data table, or empty if none. */
        final Set<String> affectedModules = new HashSet<>();
        /** Module -> reason, preserved from the upstream table for propagation. */
        final Map<String, String> moduleReasons = new HashMap<>();
        /** Module -> trigger path (the changed file that ultimately caused this module to be flagged). */
        final Map<String, String> moduleTriggerPaths = new HashMap<>();
        /** Module path -> inferred runner ("gradle" | "mvn"). Populated as build files are seen. */
        final Map<String, String> moduleRunner = new HashMap<>();
        /** Tests discovered during scanning; resolved/emitted in generate(). */
        final List<PendingRow> pending = new ArrayList<>();
        /** Dedup: one row per (module, class). */
        final Set<String> emittedKeys = new HashSet<>();

        // --- Phase 2: reachability ----------------------------------------
        /**
         * Fully-qualified class names that transitively reach at least one changed
         * symbol. Populated from {@code ReachabilityDataTable} in {@code getInitialValue}.
         */
        final Set<String> reachableClasses = new HashSet<>();
        /**
         * Per-class reachability chain back to a seed, formatted as " -> "-joined
         * {@code Class.method} segments (newest hop first → oldest). Populated by
         * walking {@code ReachabilityDataTable} rows in {@code getInitialValue}; an
         * empty value means "reachable but no chain reconstructable" so the via
         * column still records the upstream module without invented hops.
         */
        final Map<String, String> reachabilityChain = new HashMap<>();
        /**
         * {@code true} if the {@code ReachabilityDataTable} had at least one row.
         * Without it, Phase 2 falls back to Phase 1 module-dep behavior so callers
         * that never run {@code ComputeReachability} still get the old, conservative
         * selection.
         */
        boolean reachabilityProduced;
        /**
         * {@code true} when the reachability analysis emitted a bailout row. Causes
         * the downstream filter to fall back to module-dep-of-affected Phase 1
         * behavior (include every test class in the module).
         */
        boolean reachabilityBailedOut;
    }

    @Value
    private static class PendingRow {
        String module;
        String testClass;
        String language;
        String reason;
        String path;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Accumulator acc = new Accumulator();
        DataTableStore store = DataTableExecutionContextView.view(ctx).getDataTableStore();
        try (Stream<AffectedModulesDataTable.Row> rows =
                     store.getRows(AffectedModulesDataTable.class)) {
            rows.forEach(row -> {
                String module = row.getModule();
                if (module == null) {
                    return;
                }
                acc.affectedModules.add(module);
                String newReason = row.getReason() == null ? "module-dep-changed" : row.getReason();
                // A module can appear with multiple reasons (e.g. both `source-changed`
                // because its own source was edited AND `module-dep-of-affected` because
                // the cascade added it as a downstream dependent of another edited module).
                // Keep the reason with the highest priority — the most inclusive / most
                // conservative one wins so we never downgrade a source-changed module to
                // a reachability-filtered module-dep-of-affected module and silently drop
                // its tests.
                String existing = acc.moduleReasons.get(module);
                if (existing == null || reasonPriority(newReason) > reasonPriority(existing)) {
                    acc.moduleReasons.put(module, newReason);
                    // Track the trigger/via that won alongside the reason — these are
                    // the same shape as the upstream row, so promotion is atomic.
                    acc.moduleTriggerPaths.put(module, nullToEmpty(row.getTriggerPath()));
                }
            });
        }

        // Phase 2: reachability data. Looked up by FQN to keep this recipe out of the
        // dependency arrow that would otherwise run rewrite-java -> rewrite-program-analysis.
        // Missing tables produce an empty stream, so the Phase 1 conservative fallback
        // still works for callers that don't schedule ComputeReachability.
        @SuppressWarnings("deprecation")
        Stream<?> reachabilityRows = store.getRows(REACHABILITY_TABLE_NAME, null);
        // Buffer rows so we can do two passes: (1) collect reachableClasses for the
        // existence test, (2) reconstruct each class's chain by following viaClass
        // pointers. The table is bounded by the BFS frontier, so this is fine.
        Map<String, ChainHop> hopByClass = new HashMap<>();
        try (Stream<?> rows = reachabilityRows) {
            rows.forEach(raw -> {
                acc.reachabilityProduced = true;
                String sourceClass = readStringProp(raw, "getSourceClass");
                if (sourceClass == null || sourceClass.isEmpty()) {
                    return;
                }
                acc.reachableClasses.add(sourceClass);
                String sourceMethod = readStringProp(raw, "getSourceMethod");
                String viaClass = readStringProp(raw, "getViaClass");
                String viaMethod = readStringProp(raw, "getViaMethod");
                String hopKind = readStringProp(raw, "getHopKind");
                // Keep the shortest path per class — first row wins because BFS emits
                // shallower depths first; subsequent rows for the same class would be
                // longer paths into the same destination.
                hopByClass.putIfAbsent(sourceClass,
                        new ChainHop(sourceMethod, viaClass, viaMethod, hopKind));
            });
        }
        for (String reached : acc.reachableClasses) {
            acc.reachabilityChain.put(reached, buildChain(reached, hopByClass));
        }

        // Phase 2: bailout detection. A single row with reason starting with "reachability-"
        // causes the selector to fall back to Phase 1 (include every test class in
        // module-dep-of-affected modules).
        @SuppressWarnings("deprecation")
        Stream<?> bailoutRows = store.getRows(BAILOUT_TABLE_NAME, null);
        try (Stream<?> rows = bailoutRows) {
            rows.forEach(raw -> {
                String reason = readStringProp(raw, "getReason");
                if (reason != null && reason.startsWith("reachability-")) {
                    acc.reachabilityBailedOut = true;
                }
            });
        }

        return acc;
    }

    /**
     * Reflective getter read. ReachabilityDataTable / BailoutReasonsDataTable are defined
     * in {@code rewrite-program-analysis} which depends on rewrite-java — so this module
     * can't import them directly without introducing a cycle. Reading via reflection on
     * the generic row lets us stay loose.
     */
    private static @Nullable String readStringProp(@Nullable Object row, String getter) {
        if (row == null) {
            return null;
        }
        try {
            Object v = row.getClass().getMethod(getter).invoke(row);
            return v == null ? null : v.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** FQN of {@code org.openrewrite.analysis.testselection.table.ReachabilityDataTable}. */
    private static final String REACHABILITY_TABLE_NAME =
            "org.openrewrite.analysis.testselection.table.ReachabilityDataTable";

    /**
     * One reachability hop, captured at parse time from a {@code ReachabilityDataTable}
     * row so we can walk the chain backward without re-reading the row stream.
     * {@link #buildChain} follows {@link #viaClass}/{@link #viaMethod} until it sees
     * {@code hopKind == "seed"}.
     */
    @Value
    private static class ChainHop {
        @Nullable String sourceMethod;
        @Nullable String viaClass;
        @Nullable String viaMethod;
        @Nullable String hopKind;
    }

    /** Cap chain length so a pathological cycle can't blow up the via column. */
    private static final int MAX_CHAIN_HOPS = 8;

    /**
     * Reconstruct the BFS chain for a reached class. Format is
     * {@code "Class.method -> Class.method -> ..."} starting at the reached class
     * (deepest hop) and ending at the seed; segments use simple class names to keep
     * the column readable. Returns an empty string when the chain can't be walked
     * (e.g. the class has no row in the hop map — only happens for an inconsistent
     * data table).
     */
    private static String buildChain(String reached, Map<String, ChainHop> hopByClass) {
        ChainHop hop = hopByClass.get(reached);
        if (hop == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        appendSegment(out, reached, hop.sourceMethod);
        String cursor = reached;
        Set<String> visited = new HashSet<>();
        visited.add(cursor);
        for (int i = 0; i < MAX_CHAIN_HOPS; i++) {
            if ("seed".equals(hop.hopKind) || hop.viaClass == null || hop.viaClass.isEmpty()) {
                break;
            }
            if (!visited.add(hop.viaClass)) {
                break;
            }
            out.append(" -> ");
            appendSegment(out, hop.viaClass, hop.viaMethod);
            ChainHop next = hopByClass.get(hop.viaClass);
            if (next == null) {
                break;
            }
            hop = next;
            cursor = hop.viaClass == null ? cursor : hop.viaClass;
        }
        return out.toString();
    }

    private static void appendSegment(StringBuilder out, String fqn, @Nullable String method) {
        int dot = fqn.lastIndexOf('.');
        out.append(dot < 0 ? fqn : fqn.substring(dot + 1));
        if (method != null && !method.isEmpty()) {
            out.append('.').append(method);
        }
    }

    private static String nullToEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    /** FQN of {@code org.openrewrite.analysis.testselection.table.BailoutReasonsDataTable}. */
    private static final String BAILOUT_TABLE_NAME =
            "org.openrewrite.analysis.testselection.table.BailoutReasonsDataTable";

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (acc.affectedModules.isEmpty() || !(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = sourceFile.getSourcePath().toString().replace('\\', '/');

                // Runner inference: track build files per module.
                String buildModule = moduleForBuildFile(sourcePath);
                if (buildModule != null) {
                    String runner = sourcePath.endsWith("pom.xml") ? "mvn" : "gradle";
                    // "gradle" wins over a later-seen pom if both exist (unusual); first-seen wins otherwise.
                    acc.moduleRunner.putIfAbsent(buildModule, runner);
                }

                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;

                String module = moduleOf(cu, sourcePath);
                if (module == null) {
                    return tree;
                }
                boolean directMatch = acc.affectedModules.contains(module);
                // Fallback-mode match: when the classifier couldn't build a module DAG
                // (no build-file markers in the LST -- single-module repos where
                // mod build omitted build.gradle.kts / pom.xml), it emits a lone
                // synthetic root module "" with no name. ClassifyAffectedModules uses
                // filesystem paths for module identity; this recipe uses JavaProject
                // projectName. The two disagree at the root of a single-module repo,
                // so we accept "" as a wildcard when it's the ONLY affected module.
                boolean fallbackMatch = !directMatch
                        && acc.affectedModules.size() == 1
                        && acc.affectedModules.contains("");
                if (!directMatch && !fallbackMatch) {
                    return tree;
                }

                String language = languageFromPath(sourcePath);
                if (language == null) {
                    // Unknown/non-test-capable extension — skip.
                    return tree;
                }

                // Collect test classes. We walk the CU directly rather than using a
                // full JavaIsoVisitor traversal because we only care about top-level
                // class declarations with at least one @Test-annotated method.
                String reasonKey = directMatch ? module : "";
                String reason = acc.moduleReasons.getOrDefault(reasonKey, "module-dep-changed");
                String moduleTrigger = acc.moduleTriggerPaths.getOrDefault(reasonKey, "");
                AnnotationMatcher[] matchers = new AnnotationMatcher[TEST_ANNOTATION_PATTERNS.length];
                for (int i = 0; i < TEST_ANNOTATION_PATTERNS.length; i++) {
                    matchers[i] = new AnnotationMatcher(TEST_ANNOTATION_PATTERNS[i]);
                }

                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext c) {
                        if (classDecl.getType() == null) {
                            return super.visitClassDeclaration(classDecl, c);
                        }
                        if (classIsTestClass(classDecl, matchers)) {
                            String fqn = classDecl.getType().getFullyQualifiedName();
                            String key = module + "::" + fqn;

                            // Reachability is the foundation: for any module whose
                            // selection was driven by a *symbol-level* change (source
                            // edits, transitive dep on a source-edited module), include
                            // a test only when the BFS proves it reaches a changed
                            // symbol. Two carve-outs keep the conservative behavior:
                            //   * reachability data wasn't produced (Phase 1-only
                            //     callers, or no symbols to seed from) → run everything.
                            //   * the analysis bailed out → run everything.
                            //   * reason indicates a non-symbol diff (build-file-changed,
                            //     repo-root-bailout) → run everything; the call graph
                            //     can't see classpath shifts or whole-repo bailouts.
                            //
                            // Known limitation: reflection / DI containers / annotation
                            // processors / Kotlin metadata-driven dispatch can route
                            // execution through edges the static call graph misses.
                            // Tests that depend on those mechanisms will be silently
                            // dropped here. Plan: build framework-aware adapters so the
                            // call graph picks them up; until then this is the trade-off
                            // we accept in exchange for not running tens of thousands of
                            // tests on every same-module edit.
                            if (acc.reachabilityProduced &&
                                    !acc.reachabilityBailedOut &&
                                    !isWholeModuleConservativeReason(reason) &&
                                    !acc.reachableClasses.contains(fqn)) {
                                return super.visitClassDeclaration(classDecl, c);
                            }

                            // Build the combined path: <TestSimple> -> [chain] -> <TriggerSimple>.
                            // The chain (when present) already starts at the test class and walks
                            // back through reachability hops; we append the trigger file's simple
                            // name if it isn't already the chain's tail so the path always ends
                            // at a recognizable changed-file name.
                            String chain = acc.reachabilityChain.getOrDefault(fqn, "");
                            String path = buildPath(fqn, chain, moduleTrigger);

                            if (acc.emittedKeys.add(key)) {
                                acc.pending.add(new PendingRow(
                                        module, fqn, language, reason, path));
                            }
                        }
                        return super.visitClassDeclaration(classDecl, c);
                    }
                }.visit(cu, ctx);

                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        for (PendingRow row : acc.pending) {
            String runner = acc.moduleRunner.getOrDefault(row.getModule(), "gradle");
            testPlan.insertRow(ctx, new TestPlanDataTable.Row(
                    row.getModule(),
                    row.getTestClass(),
                    "", // testMethod: blank in v1 — class-level selection
                    row.getReason(),
                    row.getLanguage(),
                    runner,
                    row.getPath()
            ));
        }
        return emptyList();
    }

    /**
     * Combine the per-class reachability chain (when available) with the trigger
     * file's simple name into a single human-readable path. The result always
     * starts at the test class's simple name and ends at a changed-file name,
     * giving the user an at-a-glance answer to "why is this test selected?"
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Reachability chain present → use it (already begins with the test
     *       class's simple name and walks back through hops). Append the
     *       trigger file's simple name only if it isn't already the chain's
     *       tail, so we don't duplicate {@code "MavenVisitor -> MavenVisitor"}.</li>
     *   <li>No chain → fall back to {@code "TestSimple -> TriggerFileSimple"}.</li>
     *   <li>No trigger path either → just the test class's simple name.</li>
     * </ul>
     */
    private static String buildPath(String testClassFqn, String chain, String triggerPath) {
        String testSimple = simpleName(testClassFqn);
        String triggerSimple = triggerFileSimpleName(triggerPath);
        String head = (chain == null || chain.isEmpty()) ? testSimple : chain;
        if (triggerSimple == null || triggerSimple.isEmpty()) {
            return head;
        }
        // Chain segments use simple-class names (optionally with ".method" suffix)
        // joined by " -> ". Treat the trigger as already-present when the last
        // segment names the same class — with or without a method suffix.
        int lastSep = head.lastIndexOf(" -> ");
        String tail = lastSep < 0 ? head : head.substring(lastSep + 4);
        int dot = tail.indexOf('.');
        String tailClass = dot < 0 ? tail : tail.substring(0, dot);
        if (tailClass.equals(triggerSimple)) {
            return head;
        }
        return head + " -> " + triggerSimple;
    }

    /**
     * Extract the trigger file's simple name (basename without extension), e.g.
     * {@code "rewrite-maven/src/main/java/org/openrewrite/maven/MavenVisitor.java"}
     * → {@code "MavenVisitor"}. Returns the empty string for a null or empty
     * input so callers can use it as a no-op tail.
     */
    private static String triggerFileSimpleName(@Nullable String triggerPath) {
        if (triggerPath == null || triggerPath.isEmpty()) {
            return "";
        }
        int slash = triggerPath.lastIndexOf('/');
        String file = slash < 0 ? triggerPath : triggerPath.substring(slash + 1);
        int dot = file.lastIndexOf('.');
        return dot < 0 ? file : file.substring(0, dot);
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    @Override
    public int maxCycles() {
        // Purely a data-table-emitting recipe; it does not modify LSTs, so no
        // need to ever run a second cycle.
        return 1;
    }

    /**
     * True when the upstream classification doesn't have a symbol-level diff to
     * reach from — running the call-graph filter would drop every test in the
     * module. We instead include all of the module's tests as a conservative
     * fallback. Specifically:
     * <ul>
     *   <li>{@code build-file-changed} — only the build file (pom.xml,
     *       build.gradle*) was edited. Classpath / dependency / plugin shifts
     *       can change runtime behavior in ways the call graph doesn't see, and
     *       there are no Java symbol changes to seed reachability with.</li>
     *   <li>{@code repo-root-bailout:*} — the classifier couldn't compute the
     *       affected-modules set cleanly. Whole-repo safety net — run
     *       everything in any reachable module.</li>
     * </ul>
     */
    private static boolean isWholeModuleConservativeReason(@Nullable String reason) {
        if (reason == null) {
            return false;
        }
        return "build-file-changed".equals(reason) || reason.startsWith("repo-root-bailout:");
    }

    /**
     * Priority for reason-merging: higher value means the reason is more
     * inclusive / conservative and should win when a module has multiple
     * classifications. Specifically:
     * <ul>
     *   <li>{@code source-changed} (100) — the module had a direct source edit;
     *       every test in it is potentially affected; NEVER downgrade to a
     *       reachability-filtered reason.</li>
     *   <li>{@code build-file-changed} (80) — build file edit; also conservative
     *       "run all in module".</li>
     *   <li>{@code repo-root-bailout:*} (60) — whole-repo safety net.</li>
     *   <li>{@code module-dep-of-affected} / {@code module-dep-changed} (40) —
     *       transitive-only; eligible for the reachability filter.</li>
     * </ul>
     */
    private static int reasonPriority(@Nullable String reason) {
        if (reason == null) {
            return 0;
        }
        if ("source-changed".equals(reason)) {
            return 100;
        }
        if ("build-file-changed".equals(reason)) {
            return 80;
        }
        if (reason.startsWith("repo-root-bailout:")) {
            return 60;
        }
        return 40;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * A class counts as a test class if either (a) it declares at least one method
     * annotated with a known test annotation (JUnit 4/5, TestNG, kotlin.test), or
     * (b) it extends {@code spock.lang.Specification}. The two checks are kept in
     * separate helpers so the reasoning stays clean: annotation scanning is
     * per-method, Spock detection is a single supertype probe.
     */
    private static boolean classIsTestClass(J.ClassDeclaration classDecl, AnnotationMatcher[] matchers) {
        return classHasTestAnnotation(classDecl, matchers) || classExtendsSpockSpecification(classDecl);
    }

    private static boolean classHasTestAnnotation(J.ClassDeclaration classDecl, AnnotationMatcher[] matchers) {
        if (classDecl.getBody() == null) {
            return false;
        }
        for (org.openrewrite.java.tree.Statement stmt : classDecl.getBody().getStatements()) {
            if (!(stmt instanceof J.MethodDeclaration)) {
                continue;
            }
            J.MethodDeclaration method = (J.MethodDeclaration) stmt;
            for (J.Annotation ann : method.getLeadingAnnotations()) {
                for (AnnotationMatcher matcher : matchers) {
                    if (matcher.matches(ann)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Detect Spock specifications by supertype. Uses {@link TypeUtils#isAssignableTo}
     * rather than {@code JavaType.equals()} because the latter can recurse
     * indefinitely through circular type references. {@link TypeUtils} is the
     * project-wide standard for type-shape questions; see the project's CLAUDE.md
     * for the rationale.
     */
    private static boolean classExtendsSpockSpecification(J.ClassDeclaration classDecl) {
        if (classDecl.getType() == null) {
            return false;
        }
        return TypeUtils.isAssignableTo(SPOCK_SPECIFICATION_FQN, classDecl.getType());
    }

    /**
     * Derive the module path for a Java source file. Prefers the
     * {@link JavaProject} marker (set by parsers / test fixtures such as
     * {@code mavenProject("foo", ...)}); falls back to extracting the first
     * path segment before {@code src/} when no marker is present.
     *
     * @return the module path or {@code null} if the file appears to live
     *         directly at the repo root (single-module repo with no leading
     *         module directory).
     */
    private static @Nullable String moduleOf(SourceFile sourceFile, String sourcePath) {
        JavaProject project = sourceFile.getMarkers().findFirst(JavaProject.class).orElse(null);
        if (project != null && project.getProjectName() != null && !project.getProjectName().isEmpty()) {
            return project.getProjectName();
        }
        int srcIdx = sourcePath.indexOf("/src/");
        if (srcIdx > 0) {
            return sourcePath.substring(0, srcIdx);
        }
        // Repo-root test file (e.g. "src/test/java/...") — treat as empty-path module
        // so tests are still attributable.
        if (sourcePath.startsWith("src/")) {
            return "";
        }
        return null;
    }

    /**
     * Returns {@code "java" | "kotlin" | "groovy"} based on file extension, or
     * {@code null} for any other extension.
     */
    static @Nullable String languageFromPath(String sourcePath) {
        if (sourcePath.endsWith(".java")) {
            return "java";
        }
        if (sourcePath.endsWith(".kt") || sourcePath.endsWith(".kts")) {
            return "kotlin";
        }
        if (sourcePath.endsWith(".groovy")) {
            return "groovy";
        }
        return null;
    }

    /**
     * If {@code sourcePath} points at a build file (pom.xml, build.gradle,
     * build.gradle.kts), return the containing module path (everything before
     * the final path segment). Otherwise {@code null}.
     */
    static @Nullable String moduleForBuildFile(String sourcePath) {
        String name = sourcePath;
        int slash = sourcePath.lastIndexOf('/');
        if (slash >= 0) {
            name = sourcePath.substring(slash + 1);
        }
        if (!name.equals("pom.xml") && !name.equals("build.gradle") && !name.equals("build.gradle.kts")) {
            return null;
        }
        if (slash < 0) {
            // repo-root build file
            return "";
        }
        return sourcePath.substring(0, slash);
    }
}
