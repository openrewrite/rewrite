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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindPlugins;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class InlineAppliedScript extends ScanningRecipe<InlineAppliedScript.Accumulator> {

    @Option(displayName = "Plugin ID",
            description = "The ID of a plugin that applies the other `.gradle` or `.gradle.kts` scripts in a project's " +
                          "directory itself, so that no `apply from` appears in the build file. When a project applies " +
                          "this plugin, those scripts are appended to the end of its build file and deleted.",
            example = "com.example.dependencies",
            required = false)
    @Nullable
    String pluginId;

    String displayName = "Inline applied Gradle scripts";

    String description = "Replaces `apply from: 'dependencies.gradle'` (or `apply(from = \"dependencies.gradle.kts\")`) " +
                         "in a build file with the statements of the applied script, moving the script's imports to " +
                         "the top of the build file, and deletes the script once every place applying it was inlined. " +
                         "This leaves dependency declarations in the build file, where the other Gradle recipes can " +
                         "edit them. A script is left alone when inlining could change behavior: when its path cannot " +
                         "be resolved statically, it is applied inside a closure or with `to:`, it has a top-level " +
                         "`return` or its own `buildscript` or `plugins` block, it declares names that collide with " +
                         "the build file's, it is applied from more than one place, or it is written in a different " +
                         "DSL than the build file.";

    public static class Accumulator {
        final Map<Path, GradleFile> files = new HashMap<>();
        final Set<Path> settingsDirs = new HashSet<>();
        final Set<Path> ambiguousApplyPaths = new HashSet<>();
        final Map<Path, List<ApplySite>> appendedSites = new HashMap<>();

        @Nullable
        Plan plan;
    }

    enum Kind {
        BUILD,
        SETTINGS,
        SCRIPT
    }

    enum Dsl {
        GROOVY,
        KOTLIN
    }

    enum Base {
        PROJECT_DIR,
        ROOT_DIR
    }

    @Value
    static class GradleFile {
        Path path;
        Kind kind;
        Dsl dsl;
        List<ApplySite> applySites;
        Set<String> declaredNames;
        boolean selfContained;
        boolean appliesPlugin;

        /**
         * Printed source, kept only for scripts.
         */
        @Nullable
        String source;

        Path projectDir() {
            return path.getParent() == null ? Paths.get("") : path.getParent();
        }
    }

    @Value
    static class ApplySite {
        @Nullable
        ScriptLocation location;

        boolean inlinable;
    }

    @Value
    static class ScriptLocation {
        Base base;
        String path;

        Path resolve(Path projectDir, Path rootDir) {
            return (base == Base.ROOT_DIR ? rootDir : projectDir).resolve(path).normalize();
        }

        ScriptLocation relativeToRoot() {
            return new ScriptLocation(Base.ROOT_DIR, path);
        }
    }

    static class Plan {
        final Map<Path, Set<Path>> inlinedInto = new HashMap<>();
        final Set<Path> deletable = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof G.CompilationUnit || tree instanceof K.CompilationUnit)) {
                    return tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                Path path = cu.getSourcePath();
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(".gradle") && !fileName.endsWith(".gradle.kts")) {
                    return tree;
                }
                Kind kind = kind(cu, fileName);
                if (kind == Kind.SETTINGS) {
                    acc.settingsDirs.add(path.getParent() == null ? Paths.get("") : path.getParent());
                }
                ScriptAnalysis analysis = new ScriptAnalysis();
                analysis.visit(cu, ctx);
                for (ApplySite site : analysis.applySites) {
                    if (!site.isInlinable() && site.getLocation() != null && site.getLocation().getBase() == Base.PROJECT_DIR) {
                        acc.ambiguousApplyPaths.add(Paths.get(site.getLocation().getPath()).normalize());
                    }
                }
                acc.files.put(path, new GradleFile(
                        path,
                        kind,
                        cu instanceof K.CompilationUnit ? Dsl.KOTLIN : Dsl.GROOVY,
                        analysis.applySites,
                        analysis.declaredNames,
                        analysis.selfContained,
                        kind == Kind.BUILD && pluginId != null && appliesPlugin(cu, ctx),
                        kind == Kind.SCRIPT ? cu.printAll() : null));
                return tree;
            }
        };
    }

    private boolean appliesPlugin(JavaSourceFile cu, ExecutionContext ctx) {
        if (new FindPlugins(requireNonNull(pluginId), null).getVisitor().visit(cu, ctx) != cu) {
            return true;
        }
        for (Statement statement : topLevel(cu)) {
            if (isApplyPlugin(statement, requireNonNull(pluginId))) {
                return true;
            }
        }
        return false;
    }

    private static List<Statement> topLevel(JavaSourceFile cu) {
        if (cu instanceof G.CompilationUnit) {
            return ((G.CompilationUnit) cu).getStatements();
        }
        List<Statement> statements = new ArrayList<>();
        for (Statement s : ((K.CompilationUnit) cu).getStatements()) {
            if (s instanceof J.Block) {
                statements.addAll(((J.Block) s).getStatements());
            }
        }
        return statements;
    }

    /**
     * Recognizes {@code apply plugin: 'id'} and {@code apply(plugin = "id")}.
     */
    private static boolean isApplyPlugin(Statement statement, String pluginId) {
        if (!(statement instanceof J.MethodInvocation) || !"apply".equals(((J.MethodInvocation) statement).getSimpleName())) {
            return false;
        }
        for (Expression argument : ((J.MethodInvocation) statement).getArguments()) {
            Expression value = null;
            if (argument instanceof G.MapEntry && ((G.MapEntry) argument).getKey() instanceof J.Literal &&
                "plugin".equals(((J.Literal) ((G.MapEntry) argument).getKey()).getValue())) {
                value = ((G.MapEntry) argument).getValue();
            } else if (argument instanceof J.Assignment && isIdentifier(((J.Assignment) argument).getVariable(), "plugin")) {
                value = ((J.Assignment) argument).getAssignment();
            }
            if (value instanceof J.Literal && pluginId.equals(((J.Literal) value).getValue())) {
                return true;
            }
        }
        return false;
    }

    private static Kind kind(JavaSourceFile cu, String fileName) {
        if ("settings.gradle".equals(fileName) || "settings.gradle.kts".equals(fileName)) {
            return Kind.SETTINGS;
        }
        if ("build.gradle".equals(fileName) || "build.gradle.kts".equals(fileName) ||
            cu.getMarkers().findFirst(GradleProject.class).isPresent()) {
            return Kind.BUILD;
        }
        return Kind.SCRIPT;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        Plan plan = plan(acc);
        return Preconditions.check(!plan.inlinedInto.isEmpty(), new TreeVisitor<Tree, ExecutionContext>() {
            @Nullable
            GradleParser parser;

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                Path path = cu.getSourcePath();
                if (plan.deletable.contains(path)) {
                    return null;
                }
                Set<Path> targets = plan.inlinedInto.get(path);
                GradleFile buildFile = acc.files.get(path);
                if (targets == null || buildFile == null) {
                    return tree;
                }
                Inliner inliner = new Inliner(acc, targets, buildFile, rootDir(acc, path), ctx);
                if (cu instanceof G.CompilationUnit) {
                    G.CompilationUnit g = (G.CompilationUnit) cu;
                    g = g.getPadding().withStatements(inliner.inline(g.getPadding().getStatements()));
                    return g.withStatements(withImports(g.getStatements(), inliner.imports));
                } else if (cu instanceof K.CompilationUnit) {
                    K.CompilationUnit k = (K.CompilationUnit) cu;
                    k = k.withStatements(ListUtils.mapFirst(k.getStatements(), s -> s instanceof J.Block ?
                            ((J.Block) s).getPadding().withStatements(inliner.inline(((J.Block) s).getPadding().getStatements())) : s));
                    return k.withImports(withImports(k.getImports(), inliner.imports));
                }
                return tree;
            }

            private GradleParser parser() {
                if (parser == null) {
                    parser = GradleParser.builder().build();
                }
                return parser;
            }

            class Inliner {
                final Accumulator acc;
                final Set<Path> targets;
                final GradleFile buildFile;
                final Path projectDir;
                final Path rootDir;
                final ExecutionContext ctx;
                final List<J.Import> imports = new ArrayList<>();

                Inliner(Accumulator acc, Set<Path> targets, GradleFile buildFile, Path rootDir, ExecutionContext ctx) {
                    this.acc = acc;
                    this.targets = targets;
                    this.buildFile = buildFile;
                    this.projectDir = buildFile.projectDir();
                    this.rootDir = rootDir;
                    this.ctx = ctx;
                }

                List<JRightPadded<Statement>> inline(List<JRightPadded<Statement>> statements) {
                    List<JRightPadded<Statement>> result = expand(statements);
                    for (ApplySite site : acc.appendedSites.getOrDefault(buildFile.getPath(), emptyList())) {
                        Path target = requireNonNull(site.getLocation()).resolve(projectDir, rootDir);
                        if (targets.contains(target)) {
                            Space leading = result.isEmpty() ? Space.EMPTY : Space.format("\n\n");
                            List<JRightPadded<Statement>> body = expand(scriptBody(parse(target), imports));
                            result = ListUtils.concatAll(result, ListUtils.mapFirst(body,
                                    first -> first.map(s -> s.withPrefix(merge(leading, s.getPrefix())))));
                        }
                    }
                    return result;
                }

                List<JRightPadded<Statement>> expand(List<JRightPadded<Statement>> statements) {
                    return ListUtils.flatMap(statements, padded -> {
                        Path target = inlinedTarget(padded.getElement());
                        if (target == null) {
                            return padded;
                        }
                        List<JRightPadded<Statement>> body = expand(scriptBody(parse(target), imports));
                        Space applyPrefix = padded.getElement().getPrefix();
                        return ListUtils.mapFirst(body, first -> first.map(s -> s.withPrefix(merge(applyPrefix, s.getPrefix()))));
                    });
                }

                private @Nullable Path inlinedTarget(Statement statement) {
                    if (!(statement instanceof J.MethodInvocation)) {
                        return null;
                    }
                    ApplySite site = applySite((J.MethodInvocation) statement);
                    if (site == null || !site.isInlinable() || site.getLocation() == null) {
                        return null;
                    }
                    Path target = site.getLocation().resolve(projectDir, rootDir);
                    return targets.contains(target) ? target : null;
                }

                private JavaSourceFile parse(Path target) {
                    GradleFile script = acc.files.get(target);
                    //noinspection DataFlowIssue
                    return parser().parseInputs(singletonList(Parser.Input.fromString(target, script.getSource())), null, ctx)
                            .findFirst()
                            .map(GradleParser.requireParsed(JavaSourceFile.class))
                            .orElseThrow(() -> new IllegalStateException("Could not parse " + target));
                }
            }
        });
    }

    /**
     * The script's top-level statements, with its imports collected separately.
     */
    private static List<JRightPadded<Statement>> scriptBody(JavaSourceFile cu, List<J.Import> imports) {
        List<JRightPadded<Statement>> body = new ArrayList<>();
        if (cu instanceof G.CompilationUnit) {
            for (JRightPadded<Statement> padded : ((G.CompilationUnit) cu).getPadding().getStatements()) {
                if (padded.getElement() instanceof J.Import) {
                    imports.add((J.Import) padded.getElement());
                } else {
                    body.add(padded);
                }
            }
        } else if (cu instanceof K.CompilationUnit) {
            imports.addAll(cu.getImports());
            for (Statement s : ((K.CompilationUnit) cu).getStatements()) {
                if (s instanceof J.Block) {
                    body.addAll(((J.Block) s).getPadding().getStatements());
                }
            }
        }
        return body;
    }

    /**
     * Keeps the whitespace and comments before the {@code apply from} statement, followed by any comments heading the script.
     */
    private static Space merge(Space applyPrefix, Space scriptPrefix) {
        if (scriptPrefix.getComments().isEmpty()) {
            return applyPrefix;
        }
        return applyPrefix.withComments(ListUtils.concatAll(applyPrefix.getComments(), scriptPrefix.getComments()));
    }

    /**
     * Adds the inlined scripts' imports after the last existing import, skipping ones already present. Works on
     * a Groovy script's statements, where imports are mixed in, as well as on a Kotlin script's import list.
     */
    @SuppressWarnings("unchecked")
    private static <T extends J> List<T> withImports(List<T> statements, List<J.Import> imports) {
        Set<String> existing = new HashSet<>();
        int insertAt = 0;
        for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i) instanceof J.Import) {
                existing.add(importKey((J.Import) statements.get(i)));
                insertAt = i + 1;
            }
        }
        List<T> added = new ArrayList<>();
        for (J.Import anImport : imports) {
            if (existing.add(importKey(anImport))) {
                added.add((T) anImport.withPrefix(Space.format("\n")));
            }
        }
        if (added.isEmpty()) {
            return statements;
        }
        List<T> result = new ArrayList<>(statements);
        if (insertAt == 0 && !statements.isEmpty() && !(statements.get(0) instanceof J.Import)) {
            T first = statements.get(0);
            added.set(0, (T) added.get(0).withPrefix(first.getPrefix()));
            result.set(0, (T) first.withPrefix(Space.format("\n\n")));
        } else if (insertAt == 0) {
            added.set(0, (T) added.get(0).withPrefix(Space.EMPTY));
        }
        result.addAll(insertAt, added);
        return result;
    }

    private static String importKey(J.Import anImport) {
        return (anImport.isStatic() ? "static " : "") + anImport.getTypeName() + "." + anImport.getQualid().getSimpleName() +
               (anImport.getAlias() == null ? "" : " as " + anImport.getAlias().getSimpleName());
    }

    /**
     * Decides which scripts to inline into which build files, and which scripts can then be deleted. Every place a
     * script is applied from counts as a reference to it, and a script is deleted only when every reference to it
     * was inlined.
     */
    private Plan plan(Accumulator acc) {
        if (acc.plan != null) {
            return acc.plan;
        }
        Map<Path, Integer> references = new HashMap<>();
        Map<Path, Map<Path, Integer>> referencesPerBuildFile = new HashMap<>();
        Set<Path> reached = new HashSet<>();
        for (GradleFile file : acc.files.values()) {
            if (file.getKind() != Kind.SCRIPT) {
                Map<Path, Integer> perBuildFile = referencesPerBuildFile.computeIfAbsent(file.getPath(), p -> new HashMap<>());
                countReferences(acc, file, file.projectDir(), rootDir(acc, file.getPath()), references, perBuildFile, reached, new HashSet<>());
            }
        }
        // The plugin applies every other script in the project directory not already applied from somewhere
        for (GradleFile buildFile : acc.files.values()) {
            if (buildFile.isAppliesPlugin()) {
                List<ApplySite> appended = new ArrayList<>();
                for (GradleFile script : acc.files.values()) {
                    if (script.getKind() == Kind.SCRIPT && script.projectDir().equals(buildFile.projectDir()) &&
                        !reached.contains(script.getPath())) {
                        appended.add(new ApplySite(location(Base.PROJECT_DIR, script.getPath().getFileName().toString()), true));
                    }
                }
                appended.sort(Comparator.comparing(site -> requireNonNull(site.getLocation()).getPath()));
                acc.appendedSites.put(buildFile.getPath(), appended);
                GradleFile withAppended = new GradleFile(buildFile.getPath(), buildFile.getKind(), buildFile.getDsl(),
                        appended, emptySet(), true, true, null);
                countReferences(acc, withAppended, buildFile.projectDir(), rootDir(acc, buildFile.getPath()), references,
                        referencesPerBuildFile.computeIfAbsent(buildFile.getPath(), p -> new HashMap<>()), reached, new HashSet<>());
            }
        }
        // A script applied only from somewhere this recipe could not follow still applies whatever it applies
        for (GradleFile file : acc.files.values()) {
            if (file.getKind() == Kind.SCRIPT && !reached.contains(file.getPath())) {
                countReferences(acc, file, file.projectDir(), rootDir(acc, file.getPath()), references, new HashMap<>(), reached, new HashSet<>());
            }
        }

        Plan plan = new Plan();
        Map<Path, Integer> inlinedReferences = new HashMap<>();
        for (GradleFile buildFile : acc.files.values()) {
            if (buildFile.getKind() == Kind.BUILD) {
                Set<Path> inlined = new HashSet<>();
                Set<String> names = new HashSet<>(buildFile.getDeclaredNames());
                Path rootDir = rootDir(acc, buildFile.getPath());
                decide(acc, buildFile, sites(acc, buildFile), true, rootDir, references,
                        referencesPerBuildFile.get(buildFile.getPath()), names, inlined, inlinedReferences, new HashSet<>());
                if (!inlined.isEmpty()) {
                    plan.inlinedInto.put(buildFile.getPath(), inlined);
                }
            }
        }
        for (Map.Entry<Path, Integer> inlined : inlinedReferences.entrySet()) {
            if (inlined.getValue().equals(references.get(inlined.getKey()))) {
                plan.deletable.add(inlined.getKey());
            }
        }
        acc.plan = plan;
        return plan;
    }

    private static void countReferences(Accumulator acc, GradleFile file, Path projectDir, Path rootDir,
                                        Map<Path, Integer> references, Map<Path, Integer> perBuildFile,
                                        Set<Path> reached, Set<Path> visiting) {
        for (ApplySite site : file.getApplySites()) {
            if (site.getLocation() == null) {
                continue;
            }
            Path target = site.getLocation().resolve(projectDir, rootDir);
            references.merge(target, 1, Integer::sum);
            perBuildFile.merge(target, 1, Integer::sum);
            reached.add(target);
            GradleFile script = acc.files.get(target);
            if (script != null && script.getKind() == Kind.SCRIPT && visiting.add(target)) {
                countReferences(acc, script, projectDir, rootDir, references, perBuildFile, reached, visiting);
                visiting.remove(target);
            }
        }
    }

    private static List<ApplySite> sites(Accumulator acc, GradleFile buildFile) {
        return ListUtils.concatAll(buildFile.getApplySites(), acc.appendedSites.getOrDefault(buildFile.getPath(), emptyList()));
    }

    private static void decide(Accumulator acc, GradleFile buildFile, List<ApplySite> sites, boolean fileInlined, Path rootDir,
                               Map<Path, Integer> references, Map<Path, Integer> perBuildFile, Set<String> names,
                               Set<Path> inlined, Map<Path, Integer> inlinedReferences, Set<Path> visiting) {
        for (ApplySite site : sites) {
            if (site.getLocation() == null) {
                continue;
            }
            Path target = site.getLocation().resolve(buildFile.projectDir(), rootDir);
            GradleFile script = acc.files.get(target);
            if (script == null || script.getKind() != Kind.SCRIPT || visiting.contains(target)) {
                continue;
            }
            boolean inline = fileInlined && site.isInlinable() && canInline(acc, buildFile, script, references, perBuildFile, names);
            if (inline) {
                inlined.add(target);
                names.addAll(script.getDeclaredNames());
                inlinedReferences.merge(target, 1, Integer::sum);
            }
            visiting.add(target);
            decide(acc, buildFile, script.getApplySites(), inline, rootDir, references, perBuildFile, names, inlined, inlinedReferences, visiting);
            visiting.remove(target);
        }
    }

    private static boolean canInline(Accumulator acc, GradleFile buildFile, GradleFile script, Map<Path, Integer> references,
                                     Map<Path, Integer> perBuildFile, Set<String> names) {
        if (script.getSource() == null || script.getDsl() != buildFile.getDsl() || !script.isSelfContained()) {
            return false;
        }
        Path target = script.getPath();
        if (perBuildFile.getOrDefault(target, 0) != 1 || references.getOrDefault(target, 0) != 1) {
            return false;
        }
        for (Path ambiguous : acc.ambiguousApplyPaths) {
            if (target.endsWith(ambiguous)) {
                return false;
            }
        }
        return Collections.disjoint(names, script.getDeclaredNames());
    }

    /**
     * The directory of the nearest settings file at or above the given file, or else the repository root.
     */
    private static Path rootDir(Accumulator acc, Path path) {
        for (Path dir = path.getParent(); dir != null; dir = dir.getParent()) {
            if (acc.settingsDirs.contains(dir)) {
                return dir;
            }
        }
        return Paths.get("");
    }

    /**
     * Collects the apply sites, top-level declarations and anything preventing a script from being inlined.
     */
    private static class ScriptAnalysis extends JavaIsoVisitor<ExecutionContext> {
        final List<ApplySite> applySites = new ArrayList<>();
        final Set<String> declaredNames = new HashSet<>();
        boolean selfContained = true;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            boolean topLevel = isTopLevel(getCursor());
            ApplySite site = applySite(method);
            if (site != null) {
                applySites.add(topLevel ? site : new ApplySite(site.getLocation(), false));
            } else if (topLevel && ("buildscript".equals(method.getSimpleName()) ||
                                    "plugins".equals(method.getSimpleName()) ||
                                    "pluginManagement".equals(method.getSimpleName()))) {
                selfContained = false;
            }
            return super.visitMethodInvocation(method, ctx);
        }

        @Override
        public J.Return visitReturn(J.Return aReturn, ExecutionContext ctx) {
            if (!aReturn.getMarkers().findFirst(ImplicitReturn.class).isPresent() && endsScript(getCursor())) {
                selfContained = false;
            }
            return super.visitReturn(aReturn, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            if (isTopLevel(getCursor())) {
                for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                    declaredNames.add(variable.getSimpleName());
                }
            }
            return super.visitVariableDeclarations(multiVariable, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (isTopLevel(getCursor())) {
                declaredNames.add(method.getSimpleName());
            }
            return super.visitMethodDeclaration(method, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (isTopLevel(getCursor())) {
                declaredNames.add(classDecl.getSimpleName());
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        private static boolean isTopLevel(Cursor cursor) {
            Object parent = cursor.getParentTreeCursor().getValue();
            if (parent instanceof G.CompilationUnit) {
                return true;
            }
            return parent instanceof J.Block && cursor.getParentTreeCursor().getParentTreeCursor().getValue() instanceof K.CompilationUnit;
        }

        /**
         * A {@code return} outside any closure, method or class ends evaluation of the script it is in.
         */
        private static boolean endsScript(Cursor cursor) {
            for (Cursor c = cursor.getParentTreeCursor(); ; c = c.getParentTreeCursor()) {
                Object value = c.getValue();
                if (value instanceof J.Lambda || value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration ||
                    value instanceof J.NewClass) {
                    return false;
                }
                if (value instanceof JavaSourceFile) {
                    return true;
                }
            }
        }
    }

    /**
     * Recognizes {@code apply from: ...} and {@code apply(from = ...)}, returning {@code null} for any other call.
     * The returned site is inlinable only when {@code from} is the sole argument; the caller further narrows that to
     * top-level statements.
     */
    private static @Nullable ApplySite applySite(J.MethodInvocation method) {
        if (!"apply".equals(method.getSimpleName()) ||
            !(method.getSelect() == null || isIdentifier(method.getSelect(), "project"))) {
            return null;
        }
        Expression from = null;
        boolean onlyFrom = true;
        for (Expression argument : method.getArguments()) {
            if (argument instanceof G.MapEntry && ((G.MapEntry) argument).getKey() instanceof J.Literal &&
                "from".equals(((J.Literal) ((G.MapEntry) argument).getKey()).getValue())) {
                from = ((G.MapEntry) argument).getValue();
            } else if (argument instanceof J.Assignment && isIdentifier(((J.Assignment) argument).getVariable(), "from")) {
                from = ((J.Assignment) argument).getAssignment();
            } else if (!(argument instanceof J.Empty)) {
                onlyFrom = false;
            }
        }
        if (from == null) {
            return null;
        }
        ScriptLocation location = location(from);
        return new ApplySite(location, onlyFrom && location != null);
    }

    private static @Nullable ScriptLocation location(Expression expression) {
        if (expression instanceof J.Literal) {
            Object value = ((J.Literal) expression).getValue();
            return value instanceof String ? location(Base.PROJECT_DIR, (String) value) : null;
        }
        if (expression instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) expression;
            if (!"file".equals(m.getSimpleName()) || m.getArguments().size() != 1) {
                return null;
            }
            ScriptLocation location = location(m.getArguments().get(0));
            if (location == null || m.getSelect() == null || isIdentifier(m.getSelect(), "project")) {
                return location;
            }
            return isIdentifier(m.getSelect(), "rootProject") ? location.relativeToRoot() : null;
        }
        List<J> parts;
        if (expression instanceof G.GString) {
            parts = ((G.GString) expression).getStrings();
        } else if (expression instanceof K.StringTemplate) {
            parts = ((K.StringTemplate) expression).getStrings();
        } else {
            return null;
        }
        Base base = Base.PROJECT_DIR;
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            J part = parts.get(i);
            if (part instanceof J.Literal && ((J.Literal) part).getValue() instanceof String) {
                path.append((String) ((J.Literal) part).getValue());
            } else if (i == 0) {
                J tree = part instanceof G.GString.Value ? ((G.GString.Value) part).getTree() :
                        part instanceof K.StringTemplate.Expression ? ((K.StringTemplate.Expression) part).getTree() : null;
                Base dir = directory(tree);
                if (dir == null) {
                    return null;
                }
                base = dir;
            } else {
                return null;
            }
        }
        if (parts.isEmpty() || parts.get(0) instanceof J.Literal) {
            return location(Base.PROJECT_DIR, path.toString());
        }
        return path.length() > 1 && path.charAt(0) == '/' ? location(base, path.substring(1)) : null;
    }

    private static @Nullable ScriptLocation location(Base base, String path) {
        if (path.isEmpty() || path.contains("://") || Paths.get(path).isAbsolute()) {
            return null;
        }
        return new ScriptLocation(base, path);
    }

    private static @Nullable Base directory(@Nullable J tree) {
        String name;
        if (tree instanceof J.Identifier) {
            name = ((J.Identifier) tree).getSimpleName();
        } else if (tree instanceof J.FieldAccess && ((J.FieldAccess) tree).getTarget() instanceof J.Identifier) {
            name = ((J.Identifier) ((J.FieldAccess) tree).getTarget()).getSimpleName() + "." + ((J.FieldAccess) tree).getSimpleName();
        } else {
            return null;
        }
        switch (name) {
            case "rootDir":
            case "project.rootDir":
            case "rootProject.rootDir":
            case "rootProject.projectDir":
                return Base.ROOT_DIR;
            case "projectDir":
            case "project.projectDir":
                return Base.PROJECT_DIR;
            default:
                return null;
        }
    }

    private static boolean isIdentifier(@Nullable Expression expression, String name) {
        return expression instanceof J.Identifier && name.equals(((J.Identifier) expression).getSimpleName());
    }
}
