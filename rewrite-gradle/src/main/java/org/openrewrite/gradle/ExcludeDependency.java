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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.marker.OmitParentheses;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.tree.ParseError;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeDependency extends Recipe {

    private static final String STUB_CACHE_KEY = ExcludeDependency.class.getName() + ".stubCache";
    private static final Set<String> SKIPPED_BLOCKS = new HashSet<>(Arrays.asList("buildscript", "subprojects", "allprojects"));
    private static final Pattern COORDINATE = Pattern.compile("[A-Za-z0-9_.\\-]+");

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. " +
                    "If omitted (or `*`), the exclude targets every group containing the given artifact. Partial wildcards are not supported.",
            example = "com.google.guava",
            required = false)
    @Nullable
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. " +
                    "If omitted (or `*`), the exclude targets every artifact in the given group. Partial wildcards are not supported.",
            example = "guava",
            required = false)
    @Nullable
    String artifactId;

    @Option(displayName = "Configuration",
            description = "If specified, only direct dependency declarations in that configuration are considered. " +
                    "If omitted, declarations in every configuration are considered.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Exclude Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`",
                groupId == null ? "*" : groupId,
                artifactId == null ? "*" : artifactId);
    }

    @Override
    public String getDescription() {
        return "Exclude the specified dependency from any direct dependency declaration that transitively includes it. " +
                "The exclusion is attached to each matching direct dependency, mirroring Maven's per-dependency `<exclusions>` behavior. " +
                "Supports both Groovy and Kotlin DSL build scripts.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "coordinates",
                "At least one of `groupId` or `artifactId` must be provided (and not `*`)",
                this,
                r -> effective(r.groupId) != null || effective(r.artifactId) != null))
                .and(Validated.test("groupId", "Must be an exact group (letters, digits, `.`, `-`, `_`) or `*`; partial wildcards are not supported",
                        groupId, ExcludeDependency::isExactOrBareWildcard))
                .and(Validated.test("artifactId", "Must be an exact artifact (letters, digits, `.`, `-`, `_`) or `*`; partial wildcards are not supported",
                        artifactId, ExcludeDependency::isExactOrBareWildcard));
    }

    static boolean isExactOrBareWildcard(@Nullable String v) {
        return v == null || effective(v) == null || COORDINATE.matcher(v).matches();
    }

    static @Nullable String effective(@Nullable String v) {
        return (v == null || v.isEmpty() || "*".equals(v)) ? null : v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                List<ExcludedDeclaration> excluded = new ArrayList<>();
                Map<GroupArtifact, Set<String>> unexcluded = new HashMap<>();
                Tree after = excludeVisitor(excluded, unexcluded).visit(tree, ctx);
                if (after == tree || !(after instanceof JavaSourceFile)) {
                    return after;
                }
                JavaSourceFile sourceFile = (JavaSourceFile) after;
                Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
                if (maybeGp.isPresent()) {
                    sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().setByType(
                            updateGradleModel(maybeGp.get(), excluded, unexcluded, ctx)));
                    JavaSourceSet.markDirty(ctx, sourceFile);
                }
                return sourceFile;
            }
        });
    }

    private TreeVisitor<?, ExecutionContext> excludeVisitor(List<ExcludedDeclaration> excluded,
                                                            Map<GroupArtifact, Set<String>> unexcluded) {
        String effG = effective(groupId);
        String effA = effective(artifactId);
        return new GradleDependency.Matcher()
                .asVisitor((dep, ctx) -> {
                    J.MethodInvocation m = dep.getTree();

                    // buildscript { } is the plugin classpath; subprojects/allprojects { } target other projects' models
                    if (insideSkippedBlock(dep.getCursor())) {
                        return m;
                    }

                    // If the direct dep itself matches the exclusion target, leave it alone —
                    // that's RemoveDependency's job.
                    boolean groupMatchesDirect = effG == null || effG.equals(dep.getGroupId());
                    boolean artifactMatchesDirect = effA == null || effA.equals(dep.getArtifactId());
                    if (groupMatchesDirect && artifactMatchesDirect) {
                        return m;
                    }

                    // Transitive match (null side = match anything).
                    if (dep.getResolvedDependencyWithTransitives().findDependency(effG, effA) == null) {
                        return m;
                    }

                    boolean kotlinDsl = dep.getCursor().firstEnclosing(K.CompilationUnit.class) != null;

                    if (alreadyExcludedBy(m, kotlinDsl, effG, effA)) {
                        return m;
                    }

                    GroupArtifact declared = new GroupArtifact(dep.getGroupId(), dep.getArtifactId());
                    if (!StringUtils.isBlank(configuration) && !configuration.equals(dep.getConfigurationName())) {
                        unexcluded.computeIfAbsent(declared, k -> new HashSet<>()).add(dep.getConfigurationName());
                        return m;
                    }

                    excluded.add(new ExcludedDeclaration(
                            dep.getConfigurationName(),
                            declared,
                            ListUtils.concat(existingExclusions(m, kotlinDsl), toGroupArtifact(effG, effA))));
                    return attachExclude(m, dep.getCursor(), kotlinDsl, effG, effA, ctx);
                });
    }

    @Value
    private static class ExcludedDeclaration {
        String configuration;
        GroupArtifact declared;
        List<GroupArtifact> exclusions;
    }

    private static GroupArtifact toGroupArtifact(@Nullable String g, @Nullable String a) {
        return new GroupArtifact(g == null ? "*" : g, a == null ? "*" : a);
    }

    // Build-script excludes aren't in the parsed model, so carry them over or re-resolution would drop them.
    private List<GroupArtifact> existingExclusions(J.MethodInvocation m, boolean kotlinDsl) {
        J.Lambda lambda = trailingLambda(m);
        if (lambda == null || !(lambda.getBody() instanceof J.Block)) {
            return emptyList();
        }
        List<GroupArtifact> exclusions = new ArrayList<>();
        for (Statement raw : ((J.Block) lambda.getBody()).getStatements()) {
            Statement s = unwrapReturn(raw);
            if (s instanceof J.MethodInvocation && "exclude".equals(((J.MethodInvocation) s).getSimpleName())) {
                String g = extractNamedArg((J.MethodInvocation) s, "group", kotlinDsl);
                String a = extractNamedArg((J.MethodInvocation) s, "module", kotlinDsl);
                if (g != null || a != null) {
                    exclusions.add(toGroupArtifact(g, a));
                }
            }
        }
        return exclusions;
    }

    private static GradleProject updateGradleModel(GradleProject gp, List<ExcludedDeclaration> excluded,
                                                   Map<GroupArtifact, Set<String>> unexcluded, ExecutionContext ctx) {
        List<MavenRepository> repositories = gp.getMavenRepositories();
        for (ExcludedDeclaration e : excluded) {
            if (gp.getConfiguration(e.getConfiguration()) == null) {
                continue;
            }
            Set<String> stillIncludedBy = unexcluded.getOrDefault(e.getDeclared(), emptySet());
            gp = gp.mapConfiguration(e.getConfiguration(), conf -> inheritsAny(conf, stillIncludedBy) ? conf :
                    conf.mapDependencies(d -> withExclusions(d, e), repositories, ctx), ctx);
        }
        return gp;
    }

    private static Dependency withExclusions(Dependency d, ExcludedDeclaration e) {
        if (!Objects.equals(d.getGroupId(), e.getDeclared().getGroupId()) ||
                !Objects.equals(d.getArtifactId(), e.getDeclared().getArtifactId())) {
            return d;
        }
        List<GroupArtifact> current = d.getExclusions() == null ? emptyList() : d.getExclusions();
        Set<GroupArtifact> merged = new LinkedHashSet<>(current);
        merged.addAll(e.getExclusions());
        return merged.size() == current.size() ? d : d.withExclusions(new ArrayList<>(merged));
    }

    // Gradle only drops a transitive when every declaration of the same module excludes it.
    private static boolean inheritsAny(GradleDependencyConfiguration conf, Set<String> configurations) {
        if (configurations.contains(conf.getName())) {
            return true;
        }
        for (GradleDependencyConfiguration parent : conf.allExtendsFrom()) {
            if (configurations.contains(parent.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean insideSkippedBlock(Cursor cursor) {
        return enclosingInvocation(cursor, SKIPPED_BLOCKS) != null;
    }

    private static @Nullable Cursor enclosingInvocation(Cursor cursor, Set<String> names) {
        Cursor c = cursor.getParent();
        while (c != null) {
            Object v = c.getValue();
            if (v instanceof J.MethodInvocation && names.contains(((J.MethodInvocation) v).getSimpleName())) {
                return c;
            }
            c = c.getParent();
        }
        return null;
    }

    private boolean alreadyExcludedBy(J.MethodInvocation m, boolean kotlinDsl,
                                      @Nullable String effG, @Nullable String effA) {
        J.Lambda lambda = trailingLambda(m);
        if (lambda == null || !(lambda.getBody() instanceof J.Block)) {
            return false;
        }
        for (Statement raw : ((J.Block) lambda.getBody()).getStatements()) {
            Statement s = unwrapReturn(raw);
            if (coversTransitiveFalse(s, kotlinDsl)) {
                return true;
            }
            if (!(s instanceof J.MethodInvocation)) {
                continue;
            }
            J.MethodInvocation stmt = (J.MethodInvocation) s;
            if (!"exclude".equals(stmt.getSimpleName())) {
                continue;
            }
            String existingG = extractNamedArg(stmt, "group", kotlinDsl);
            String existingA = extractNamedArg(stmt, "module", kotlinDsl);
            // Existing covers target if its filter is broader than or equal to ours.
            // null on existing = "any", so always covers; non-null existing must match target exactly.
            boolean groupCovers = existingG == null || Objects.equals(existingG, effG);
            boolean moduleCovers = existingA == null || Objects.equals(existingA, effA);
            if (groupCovers && moduleCovers && (existingG != null || existingA != null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean coversTransitiveFalse(Statement s, boolean kotlinDsl) {
        if (kotlinDsl) {
            if (!(s instanceof J.Assignment)) {
                return false;
            }
            J.Assignment assn = (J.Assignment) s;
            if (!(assn.getVariable() instanceof J.Identifier) ||
                    !"isTransitive".equals(((J.Identifier) assn.getVariable()).getSimpleName())) {
                return false;
            }
            return assn.getAssignment() instanceof J.Literal &&
                    Boolean.FALSE.equals(((J.Literal) assn.getAssignment()).getValue());
        }
        if (!(s instanceof J.Assignment)) {
            return false;
        }
        J.Assignment assn = (J.Assignment) s;
        if (!(assn.getVariable() instanceof J.Identifier) ||
                !"transitive".equals(((J.Identifier) assn.getVariable()).getSimpleName())) {
            return false;
        }
        return assn.getAssignment() instanceof J.Literal &&
                Boolean.FALSE.equals(((J.Literal) assn.getAssignment()).getValue());
    }

    static @Nullable String extractNamedArg(J.MethodInvocation method, String name, boolean kotlinDsl) {
        List<Expression> args = method.getArguments();
        for (int i = 0; i < args.size(); i++) {
            Expression arg = args.get(i);
            // Kotlin's `exclude(group, module)` also takes its arguments by position
            if (kotlinDsl && arg instanceof J.Literal && i == ("group".equals(name) ? 0 : 1)) {
                Object v = ((J.Literal) arg).getValue();
                return v == null ? null : v.toString();
            }
            if (!kotlinDsl && arg instanceof G.MapEntry) {
                G.MapEntry entry = (G.MapEntry) arg;
                if (entry.getKey() instanceof J.Literal &&
                        name.equals(((J.Literal) entry.getKey()).getValue()) &&
                        entry.getValue() instanceof J.Literal) {
                    Object v = ((J.Literal) entry.getValue()).getValue();
                    return v == null ? null : v.toString();
                }
            }
            if (kotlinDsl && arg instanceof J.Assignment) {
                J.Assignment assn = (J.Assignment) arg;
                if (assn.getVariable() instanceof J.Identifier &&
                        name.equals(((J.Identifier) assn.getVariable()).getSimpleName()) &&
                        assn.getAssignment() instanceof J.Literal) {
                    Object v = ((J.Literal) assn.getAssignment()).getValue();
                    return v == null ? null : v.toString();
                }
            }
        }
        return null;
    }

    private J.MethodInvocation attachExclude(J.MethodInvocation m, Cursor cursor, boolean kotlinDsl,
                                             @Nullable String effG, @Nullable String effA, ExecutionContext ctx) {
        String declIndent = indentOf(cursor);
        String bodyIndent = declIndent + indentStep(cursor, declIndent);
        J.Lambda existingLambda = trailingLambda(m);
        if (existingLambda != null && existingLambda.getBody() instanceof J.Block) {
            Statement excludeStmt = parseExcludeStatement(kotlinDsl, effG, effA, ctx);
            J.Block body = (J.Block) existingLambda.getBody();
            Space indentPrefix;
            if (body.getStatements().isEmpty()) {
                body = body.withEnd(Space.format("\n" + declIndent));
                indentPrefix = Space.format("\n" + bodyIndent);
            } else {
                indentPrefix = body.getStatements().get(body.getStatements().size() - 1).getPrefix();
            }
            Statement withPrefix = excludeStmt.withPrefix(indentPrefix);
            J.Block newBody = body.withStatements(ListUtils.concat(body.getStatements(), withPrefix));
            J.Lambda newLambda = existingLambda.withBody(newBody);
            return m.withArguments(ListUtils.map(m.getArguments(), a -> a == existingLambda ? newLambda : a));
        }
        J.Lambda freshLambda = parseFreshLambda(kotlinDsl, effG, effA, ctx);
        J.Block freshBody = (J.Block) freshLambda.getBody();
        freshLambda = freshLambda.withBody(freshBody
                .withStatements(ListUtils.map(freshBody.getStatements(), st -> st.withPrefix(Space.format("\n" + bodyIndent))))
                .withEnd(Space.format("\n" + declIndent)));
        List<Expression> args = m.getArguments();
        if (!kotlinDsl) {
            // Reinstate parens on Groovy's parens-less invocation form (strip OmitParentheses + leading
            // space on existing args) so the trailing-closure lambda prints correctly.
            args = ListUtils.map(args, (idx, a) -> {
                Expression stripped = a.withMarkers(a.getMarkers()
                        .removeByType(OmitParentheses.class)
                        .removeByType(org.openrewrite.java.marker.OmitParentheses.class));
                return idx == 0 ? stripped.withPrefix(Space.EMPTY) : stripped;
            });
        }
        return m.withArguments(ListUtils.concat(args, freshLambda));
    }

    // An implicit return around the last statement of a closure holds that statement's prefix.
    private static String indentOf(Cursor cursor) {
        J j = cursor.getValue();
        Space prefix = j.getPrefix();
        Object parent = cursor.getParentTreeCursor().getValue();
        if (parent instanceof J.Return && ((J.Return) parent).getExpression() == j) {
            prefix = ((J.Return) parent).getPrefix();
        }
        return prefix.getLastWhitespace().contains("\n") ? prefix.getIndent() : "";
    }

    // One indent step, measured as the declaration's indent beyond its enclosing dependencies block.
    private static String indentStep(Cursor cursor, String declIndent) {
        Cursor deps = enclosingInvocation(cursor, singleton("dependencies"));
        String depsIndent = deps == null ? "" : indentOf(deps);
        if (declIndent.length() > depsIndent.length() && declIndent.startsWith(depsIndent)) {
            return declIndent.substring(depsIndent.length());
        }
        return "    ";
    }

    private static Statement unwrapReturn(Statement s) {
        if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof Statement) {
            return (Statement) ((J.Return) s).getExpression();
        }
        return s;
    }

    private J.@Nullable Lambda trailingLambda(J.MethodInvocation m) {
        List<Expression> args = m.getArguments();
        for (int i = args.size() - 1; i >= 0; i--) {
            if (args.get(i) instanceof J.Lambda) {
                return (J.Lambda) args.get(i);
            }
        }
        return null;
    }

    private Statement parseExcludeStatement(boolean kotlinDsl, @Nullable String effG, @Nullable String effA, ExecutionContext ctx) {
        J.Lambda stubLambda = parseStubLambda(kotlinDsl, effG, effA, ctx);
        return unwrapReturn(((J.Block) stubLambda.getBody()).getStatements().get(0));
    }

    private J.Lambda parseFreshLambda(boolean kotlinDsl, @Nullable String effG, @Nullable String effA, ExecutionContext ctx) {
        return parseStubLambda(kotlinDsl, effG, effA, ctx).withPrefix(Space.format(" "));
    }

    private static String groovyExcludeSyntax(@Nullable String g, @Nullable String a) {
        if (g != null && a != null) {
            return "exclude group: '" + g + "', module: '" + a + "'";
        }
        if (g != null) {
            return "exclude group: '" + g + "'";
        }
        return "exclude module: '" + a + "'";
    }

    private static String kotlinExcludeSyntax(@Nullable String g, @Nullable String a) {
        if (g != null && a != null) {
            return "exclude(group = \"" + g + "\", module = \"" + a + "\")";
        }
        if (g != null) {
            return "exclude(group = \"" + g + "\")";
        }
        return "exclude(module = \"" + a + "\")";
    }

    private J.Lambda parseStubLambda(boolean kotlinDsl, @Nullable String effG, @Nullable String effA, ExecutionContext ctx) {
        String snippet =
                "dependencies {\n" +
                        "    " + (kotlinDsl ? "implementation(\"x:x:1\")" : "implementation('x:x:1')") + " {\n" +
                        "        " + (kotlinDsl ? kotlinExcludeSyntax(effG, effA) : groovyExcludeSyntax(effG, effA)) + "\n" +
                        "    }\n" +
                        "}";
        // GradleParser is slow, and the snippet only varies with the DSL, so parse it once per run.
        Map<String, J.Lambda> cache = ctx.computeMessageIfAbsent(STUB_CACHE_KEY, k -> new HashMap<>());
        return cache.computeIfAbsent(snippet, s -> {
            J.MethodInvocation deps = (J.MethodInvocation) parseAsGradle(s, kotlinDsl, ctx).get(0);
            J.Lambda depsLambda = (J.Lambda) deps.getArguments().get(deps.getArguments().size() - 1);
            J.MethodInvocation stub = (J.MethodInvocation) unwrapReturn(((J.Block) depsLambda.getBody()).getStatements().get(0));
            return (J.Lambda) stub.getArguments().get(stub.getArguments().size() - 1);
        });
    }

    private static List<Statement> parseAsGradle(String snippet, boolean kotlinDsl, ExecutionContext ctx) {
        SourceFile cu = GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(Paths.get(kotlinDsl ? "build.gradle.kts" : "build.gradle"), snippet)), null, ctx)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not parse Gradle snippet"));
        if (cu instanceof ParseError) {
            throw ((ParseError) cu).toException();
        }
        if (cu instanceof K.CompilationUnit) {
            // Kotlin scripts wrap their top-level statements in a block
            return ((J.Block) ((K.CompilationUnit) cu).getStatements().get(0)).getStatements();
        }
        return ((G.CompilationUnit) cu).getStatements();
    }
}
