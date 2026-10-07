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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.tree.ParseError;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeDependency extends Recipe {

    @Value
    private static class ExcludeTarget {
        @Nullable String group;
        @Nullable String module;

        boolean covers(ExcludeTarget desired) {
            return (group == null || Objects.equals(group, desired.group)) &&
                    (module == null || Objects.equals(module, desired.module));
        }
    }

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. " +
                    "Supports `*` as a glob. Partial globs (e.g. `com.foo.*`) are expanded against the resolved " +
                    "dependency graph since Gradle itself does not accept wildcards in `exclude`. A bare `*` is " +
                    "emitted using Gradle's native omit-the-group form (`exclude module: '...'`).",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. " +
                    "Supports `*` as a glob; a bare `*` is emitted as `exclude group: '...'` (omit the module).",
            example = "guava")
    String artifactId;

    @Option(displayName = "Configuration",
            description = "The configuration from which to exclude the dependency. " +
                    "If omitted, the exclusion is applied to every configuration via a `configurations.all` block.",
            example = "runtimeClasspath",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Exclude Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Exclude specified dependency from any configuration that transitively includes it. " +
                "Inserts (or merges into) a `configurations.all { exclude group: '...', module: '...' }` block, " +
                "which is Gradle's native equivalent of Maven's per-dependency `<exclusions>`. " +
                "Supports both Groovy and Kotlin DSL build scripts.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                JavaSourceFile sf = (JavaSourceFile) tree;
                Optional<GradleProject> maybeGp = sf.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()) {
                    return sf;
                }
                GradleProject gp = maybeGp.get();

                String targetConfiguration = StringUtils.isBlank(configuration) ? "all" : configuration;
                List<ExcludeTarget> targets = resolveTargets(gp, targetConfiguration);
                if (targets.isEmpty()) {
                    return sf;
                }

                if (sf instanceof G.CompilationUnit) {
                    return handleGroovy((G.CompilationUnit) sf, targetConfiguration, targets, ctx);
                }
                if (sf instanceof K.CompilationUnit) {
                    return handleKotlin((K.CompilationUnit) sf, targetConfiguration, targets, ctx);
                }
                return sf;
            }

            private List<ExcludeTarget> resolveTargets(GradleProject gp, String targetConfiguration) {
                boolean groupBareWildcard = "*".equals(groupId);
                boolean artifactBareWildcard = "*".equals(artifactId);
                if (groupBareWildcard && artifactBareWildcard) {
                    return Collections.emptyList();
                }
                boolean groupHasGlob = groupId.contains("*");
                boolean artifactHasGlob = artifactId.contains("*");

                Iterable<GradleDependencyConfiguration> configs;
                if ("all".equals(targetConfiguration)) {
                    configs = gp.getConfigurations();
                } else {
                    GradleDependencyConfiguration gdc = gp.getConfiguration(targetConfiguration);
                    configs = gdc == null ? Collections.emptyList() : singletonList(gdc);
                }

                Set<ExcludeTarget> result = new LinkedHashSet<>();
                for (GradleDependencyConfiguration gdc : configs) {
                    for (ResolvedDependency rd : gdc.getResolved()) {
                        if (!matchesGlob(rd.getGroupId(), groupId) || !matchesGlob(rd.getArtifactId(), artifactId)) {
                            continue;
                        }
                        String g = groupBareWildcard ? null : (groupHasGlob ? rd.getGroupId() : groupId);
                        String m = artifactBareWildcard ? null : (artifactHasGlob ? rd.getArtifactId() : artifactId);
                        result.add(new ExcludeTarget(g, m));
                        if (!groupHasGlob && !artifactHasGlob) {
                            return new ArrayList<>(result);
                        }
                    }
                }
                return new ArrayList<>(result);
            }

            // ---- Groovy DSL ----

            private J handleGroovy(G.CompilationUnit cu, String targetConfiguration, List<ExcludeTarget> targets, ExecutionContext ctx) {
                List<ExcludeTarget> missing = targetsNotAlreadyExcluded(cu, targetConfiguration, targets, false);
                if (missing.isEmpty()) {
                    return cu;
                }
                J.MethodInvocation existing = findConfigurationsBlockGroovy(cu, targetConfiguration);
                if (existing != null) {
                    return mergeIntoExistingBlockGroovy(cu, existing, missing, ctx);
                }
                return insertNewBlockGroovy(cu, targetConfiguration, missing, ctx);
            }

            private J.@Nullable MethodInvocation findConfigurationsBlockGroovy(G.CompilationUnit cu, String targetConfiguration) {
                for (Statement s : cu.getStatements()) {
                    if (s instanceof J.MethodInvocation && isConfigurationsBlockGroovy((J.MethodInvocation) s, targetConfiguration)) {
                        return (J.MethodInvocation) s;
                    }
                }
                return null;
            }

            private boolean isConfigurationsBlockGroovy(J.MethodInvocation m, String targetConfiguration) {
                if (!targetConfiguration.equals(m.getSimpleName())) {
                    return false;
                }
                Expression select = m.getSelect();
                return select instanceof J.Identifier && "configurations".equals(((J.Identifier) select).getSimpleName());
            }

            private J mergeIntoExistingBlockGroovy(G.CompilationUnit cu, J.MethodInvocation existing, List<ExcludeTarget> toAdd, ExecutionContext ctx) {
                return new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (m != existing || m.getArguments().isEmpty() || !(m.getArguments().get(0) instanceof J.Lambda)) {
                            return m;
                        }
                        J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                        if (!(lambda.getBody() instanceof J.Block)) {
                            return m;
                        }
                        J.Block body = (J.Block) lambda.getBody();
                        for (ExcludeTarget t : toAdd) {
                            String snippet = groovyExclude(t);
                            body = GroovyTemplate.builder(snippet).build().apply(
                                    new Cursor(getCursor(), body),
                                    body.getStatements().isEmpty() ?
                                            body.getCoordinates().firstStatement() :
                                            body.getStatements().get(body.getStatements().size() - 1).getCoordinates().after());
                        }
                        return m.withArguments(singletonList(lambda.withBody(body)));
                    }
                }.visitNonNull(cu, ctx);
            }

            private J insertNewBlockGroovy(G.CompilationUnit cu, String targetConfiguration, List<ExcludeTarget> targets, ExecutionContext ctx) {
                if (cu.getStatements().isEmpty()) {
                    return cu;
                }
                StringBuilder snippet = new StringBuilder("configurations.").append(targetConfiguration).append(" {\n");
                for (ExcludeTarget t : targets) {
                    snippet.append("    ").append(groovyExclude(t)).append("\n");
                }
                snippet.append("}");
                Statement parsed = GradleParser.builder().build()
                        .parseInputs(singletonList(Parser.Input.fromString(Paths.get("build.gradle"), snippet.toString())), null, ctx)
                        .findFirst()
                        .map(p -> {
                            if (p instanceof ParseError) {
                                throw ((ParseError) p).toException();
                            }
                            return (G.CompilationUnit) p;
                        })
                        .map(p -> p.getStatements().get(0))
                        .orElseThrow(() -> new IllegalStateException("Could not parse configurations block snippet"));

                int insertAtIdx = cu.getStatements().size();
                for (int i = 0; i < cu.getStatements().size(); i++) {
                    Statement s = cu.getStatements().get(i);
                    if (s instanceof J.MethodInvocation && "dependencies".equals(((J.MethodInvocation) s).getSimpleName())) {
                        insertAtIdx = i;
                        break;
                    }
                }
                return cu.withStatements(ListUtils.insert(cu.getStatements(),
                        parsed.withPrefix(Space.format("\n\n")), insertAtIdx));
            }

            // ---- Kotlin DSL ----

            private J handleKotlin(K.CompilationUnit cu, String targetConfiguration, List<ExcludeTarget> targets, ExecutionContext ctx) {
                if (cu.getStatements().isEmpty() || !(cu.getStatements().get(0) instanceof J.Block)) {
                    return cu;
                }
                J.Block topBlock = (J.Block) cu.getStatements().get(0);
                List<ExcludeTarget> missing = targetsNotAlreadyExcluded(cu, targetConfiguration, targets, true);
                if (missing.isEmpty()) {
                    return cu;
                }
                J.MethodInvocation existing = findConfigurationsBlockKotlin(topBlock, targetConfiguration);
                if (existing != null) {
                    return mergeIntoExistingBlockKotlin(cu, existing, missing, ctx);
                }
                return insertNewBlockKotlin(cu, topBlock, targetConfiguration, missing, ctx);
            }

            private J.@Nullable MethodInvocation findConfigurationsBlockKotlin(J.Block topBlock, String targetConfiguration) {
                for (Statement s : topBlock.getStatements()) {
                    if (s instanceof J.MethodInvocation && isConfigurationsBlockKotlin((J.MethodInvocation) s, targetConfiguration)) {
                        return (J.MethodInvocation) s;
                    }
                }
                return null;
            }

            private boolean isConfigurationsBlockKotlin(J.MethodInvocation m, String targetConfiguration) {
                Expression select = m.getSelect();
                if (!(select instanceof J.Identifier) || !"configurations".equals(((J.Identifier) select).getSimpleName())) {
                    return false;
                }
                if ("all".equals(targetConfiguration)) {
                    return "all".equals(m.getSimpleName());
                }
                // configurations.named("foo") { ... }
                if ("named".equals(m.getSimpleName()) && !m.getArguments().isEmpty() &&
                        m.getArguments().get(0) instanceof J.Literal &&
                        targetConfiguration.equals(((J.Literal) m.getArguments().get(0)).getValue())) {
                    return true;
                }
                // configurations.<name> { ... } (less common in Kotlin DSL but valid for a few)
                return targetConfiguration.equals(m.getSimpleName());
            }

            private J mergeIntoExistingBlockKotlin(K.CompilationUnit cu, J.MethodInvocation existing, List<ExcludeTarget> toAdd, ExecutionContext ctx) {
                return new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (m != existing) {
                            return m;
                        }
                        J.Lambda lambda = findTrailingLambda(m);
                        if (lambda == null || !(lambda.getBody() instanceof J.Block)) {
                            return m;
                        }
                        J.Block body = (J.Block) lambda.getBody();
                        List<Statement> bodyStatements = new ArrayList<>(body.getStatements());
                        for (ExcludeTarget t : toAdd) {
                            Statement parsedExclude = parseKotlinStatement(kotlinExcludeInBlock(t), ctx);
                            bodyStatements.add(parsedExclude.withPrefix(Space.format("\n    ")));
                        }
                        body = body.withStatements(bodyStatements);
                        J.Lambda newLambda = lambda.withBody(body);
                        List<Expression> newArgs = new ArrayList<>(m.getArguments());
                        for (int i = 0; i < newArgs.size(); i++) {
                            if (newArgs.get(i) == lambda) {
                                newArgs.set(i, newLambda);
                                break;
                            }
                        }
                        return m.withArguments(newArgs);
                    }
                }.visitNonNull(cu, ctx);
            }

            private J insertNewBlockKotlin(K.CompilationUnit cu, J.Block topBlock, String targetConfiguration, List<ExcludeTarget> targets, ExecutionContext ctx) {
                StringBuilder snippet = new StringBuilder();
                if ("all".equals(targetConfiguration)) {
                    snippet.append("configurations.all {\n");
                } else {
                    snippet.append("configurations.named(\"").append(targetConfiguration).append("\") {\n");
                }
                for (ExcludeTarget t : targets) {
                    snippet.append("    ").append(kotlinExcludeInBlock(t)).append("\n");
                }
                snippet.append("}");
                Statement parsed = parseKotlinStatement(snippet.toString(), ctx);

                List<Statement> stmts = topBlock.getStatements();
                int insertAtIdx = stmts.size();
                for (int i = 0; i < stmts.size(); i++) {
                    Statement s = stmts.get(i);
                    if (s instanceof J.MethodInvocation && "dependencies".equals(((J.MethodInvocation) s).getSimpleName())) {
                        insertAtIdx = i;
                        break;
                    }
                }
                J.Block newTopBlock = topBlock.withStatements(ListUtils.insert(stmts,
                        parsed.withPrefix(Space.format("\n\n")), insertAtIdx));
                return cu.withStatements(ListUtils.mapFirst(cu.getStatements(), s -> newTopBlock));
            }

            private Statement parseKotlinStatement(String snippet, ExecutionContext ctx) {
                return GradleParser.builder().build()
                        .parseInputs(singletonList(Parser.Input.fromString(Paths.get("build.gradle.kts"), snippet)), null, ctx)
                        .findFirst()
                        .map(p -> {
                            if (p instanceof ParseError) {
                                throw ((ParseError) p).toException();
                            }
                            return (K.CompilationUnit) p;
                        })
                        .map(p -> ((J.Block) p.getStatements().get(0)).getStatements().get(0))
                        .orElseThrow(() -> new IllegalStateException("Could not parse Kotlin DSL snippet"));
            }

            private J.@Nullable Lambda findTrailingLambda(J.MethodInvocation m) {
                for (Expression arg : m.getArguments()) {
                    if (arg instanceof J.Lambda) {
                        return (J.Lambda) arg;
                    }
                }
                return null;
            }

            // ---- Shared ----

            private List<ExcludeTarget> targetsNotAlreadyExcluded(JavaSourceFile sf, String targetConfiguration, List<ExcludeTarget> targets, boolean kotlinDsl) {
                List<ExcludeTarget> existing = collectExcludedPairs(sf, targetConfiguration, kotlinDsl);
                List<ExcludeTarget> missing = new ArrayList<>();
                for (ExcludeTarget desired : targets) {
                    boolean covered = false;
                    for (ExcludeTarget e : existing) {
                        if (e.covers(desired)) {
                            covered = true;
                            break;
                        }
                    }
                    if (!covered) {
                        missing.add(desired);
                    }
                }
                return missing;
            }

            private List<ExcludeTarget> collectExcludedPairs(JavaSourceFile sf, String targetConfiguration, boolean kotlinDsl) {
                List<ExcludeTarget> excluded = new ArrayList<>();
                new JavaIsoVisitor<List<ExcludeTarget>>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<ExcludeTarget> acc) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, acc);
                        if (!"exclude".equals(m.getSimpleName())) {
                            return m;
                        }
                        if (!isInsideTargetConfiguration(getCursor(), targetConfiguration, kotlinDsl)) {
                            return m;
                        }
                        String g = extractNamedArg(m, "group", kotlinDsl);
                        String a = extractNamedArg(m, "module", kotlinDsl);
                        if (g != null || a != null) {
                            acc.add(new ExcludeTarget(g, a));
                        }
                        return m;
                    }
                }.visit(sf, excluded);
                return excluded;
            }

            private boolean isInsideTargetConfiguration(Cursor cursor, String targetConfiguration, boolean kotlinDsl) {
                Cursor c = cursor.getParent();
                while (c != null && c.getValue() != Cursor.ROOT_VALUE) {
                    if (c.getValue() instanceof J.MethodInvocation) {
                        J.MethodInvocation m = (J.MethodInvocation) c.getValue();
                        if (kotlinDsl ? isConfigurationsBlockKotlin(m, targetConfiguration) :
                                isConfigurationsBlockGroovy(m, targetConfiguration)) {
                            return true;
                        }
                    }
                    c = c.getParent();
                }
                return false;
            }

            private @Nullable String extractNamedArg(J.MethodInvocation method, String name, boolean kotlinDsl) {
                for (Expression arg : method.getArguments()) {
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

            private String groovyExclude(ExcludeTarget t) {
                return "exclude " + excludeArgs(t, "group: '", "'", "module: '", "'");
            }

            private String kotlinExcludeInBlock(ExcludeTarget t) {
                return "exclude(" + excludeArgs(t, "group = \"", "\"", "module = \"", "\"") + ")";
            }

            private String excludeArgs(ExcludeTarget t, String gPre, String gPost, String mPre, String mPost) {
                StringBuilder sb = new StringBuilder();
                if (t.getGroup() != null) {
                    sb.append(gPre).append(t.getGroup()).append(gPost);
                }
                if (t.getModule() != null) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(mPre).append(t.getModule()).append(mPost);
                }
                return sb.toString();
            }
        });
    }
}
