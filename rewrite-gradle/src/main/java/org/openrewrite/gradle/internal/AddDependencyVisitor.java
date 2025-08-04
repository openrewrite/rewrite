/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.internal;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RandomizeIdVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.gradle.AddDependencyVisitor.DependencyModifier.ENFORCED_PLATFORM;
import static org.openrewrite.gradle.AddDependencyVisitor.DependencyModifier.PLATFORM;

@RequiredArgsConstructor
public class AddDependencyVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final MethodMatcher DEPENDENCIES_DSL_MATCHER = new MethodMatcher("* dependencies(..)");
    private static final GradleParser GRADLE_PARSER = GradleParser.builder().build();
    private static final String TEMPLATE_CACHE_MESSAGE_KEY = "__org.openrewrite.gradle.internal.AddDependencyVisitor.cache__";
    public static final String KOTLIN_MAP_SEPARATOR = " = ";
    public static final String GROOVY_MAP_SEPARATOR = ": ";

    private final String configuration;
    private final String groupId;
    private final String artifactId;

    @Nullable
    private final String version;

    @Nullable
    private final String classifier;

    @Nullable
    private final String extension;

    @Nullable
    private final Predicate<Cursor> insertPredicate;

    private final org.openrewrite.gradle.AddDependencyVisitor.@Nullable DependencyModifier dependencyModifier;

    private final boolean isKotlinDsl;

    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
        J j = super.visit(tree, ctx);

        if (j instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) j;

            if (j == tree && insertPredicate != null && insertPredicate.test(new Cursor(getCursor(), j))) {
                if (cu instanceof G.CompilationUnit) {
                    G.CompilationUnit g = (G.CompilationUnit) cu;

                    if (!hasDependenciesBlock(g.getStatements())) {
                        Cursor parent = getCursor();
                        setCursor(new Cursor(parent, g));
                        J.MethodInvocation dependencies = dependenciesDeclaration(ctx)
                                .withPrefix(Space.format("\n\n"));
                        cu = g.withStatements(ListUtils.concat(g.getStatements(), dependencies));
                        setCursor(parent);
                    }
                }
            }

            return cu;
        }
        return j;
    }

    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
        J.Block b = super.visitBlock(block, ctx);
        if (b == block && insertPredicate != null && insertPredicate.test(getCursor())) {
            if (!hasDependenciesBlock(b.getStatements())) {
                Cursor parent = getCursor().dropParentUntil(value -> value instanceof J.MethodInvocation || value == Cursor.ROOT_VALUE);
                Space prefix = b.getStatements().isEmpty() ? Space.format("\n") : Space.format("\n\n");
                Statement dependencies = parent.isRoot() ?
                        dependenciesDeclaration(ctx).withPrefix(prefix) :
                        autoFormat(dependenciesDeclaration(ctx).withPrefix(prefix), ctx, getCursor());
                return b.withStatements(ListUtils.concat(b.getStatements(), dependencies));
            }
        }
        return b;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
        if (DEPENDENCIES_DSL_MATCHER.matches(m, true)) {
            if (insertPredicate != null && !insertPredicate.test(getCursor())) {
                return m;
            }

            Optional<GradleDependency> maybeDependency = new GradleDependency.Matcher()
                    .configuration(configuration)
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .lower(getCursor())
                    .findFirst();
            if (maybeDependency.isPresent()) {
                return m;
            }

            return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                if (arg instanceof J.Lambda) {
                    J.Lambda lambda = (J.Lambda) arg;
                    J.Block body = (J.Block) lambda.getBody();
                    J.MethodInvocation addDependencyInvocation = dependencyDeclaration(body, ctx, new Cursor(new Cursor(getCursor(), lambda), body));
                    InsertDependencyComparator dependencyComparator = new InsertDependencyComparator(body.getStatements(), addDependencyInvocation);

                    List<Statement> statements = new ArrayList<>(body.getStatements());
                    int i = 0;
                    for (; i < body.getStatements().size(); i++) {
                        Statement currentStatement = body.getStatements().get(i);
                        if (dependencyComparator.compare(currentStatement, addDependencyInvocation) > 0) {
                            if (dependencyComparator.getBeforeDependency() != null) {
                                J.MethodInvocation beforeDependency = (J.MethodInvocation) (dependencyComparator.getBeforeDependency() instanceof J.Return ?
                                        requireNonNull(((J.Return) dependencyComparator.getBeforeDependency()).getExpression()) :
                                        dependencyComparator.getBeforeDependency());
                                if (i == 0) {
                                    if (!addDependencyInvocation.getSimpleName().equals(beforeDependency.getSimpleName())) {
                                        statements.set(i, currentStatement.withPrefix(Space.format("\n\n" + currentStatement.getPrefix().getIndent())));
                                    }
                                } else {
                                    Space originalPrefix = addDependencyInvocation.getPrefix();
                                    addDependencyInvocation = addDependencyInvocation.withPrefix(currentStatement.getPrefix());

                                    if (addDependencyInvocation.getSimpleName().equals(beforeDependency.getSimpleName())) {
                                        if (!currentStatement.getPrefix().equals(originalPrefix)) {
                                            statements.set(i, currentStatement.withPrefix(originalPrefix));
                                        }
                                    }
                                }
                            }

                            statements.add(i, addDependencyInvocation);
                            break;
                        }
                    }
                    if (body.getStatements().size() == i) {
                        if (!body.getStatements().isEmpty()) {
                            Statement lastStatement;
                            if (statements.get(i - 1) instanceof J.Return) {
                                J.Return r = (J.Return) statements.remove(i - 1);
                                lastStatement = requireNonNull(r.getExpression()).withPrefix(r.getPrefix());
                                statements.add(lastStatement);
                            } else {
                                lastStatement = statements.get(i - 1);
                            }
                            if (lastStatement instanceof J.MethodInvocation && !((J.MethodInvocation) lastStatement).getSimpleName().equals(addDependencyInvocation.getSimpleName())) {
                                addDependencyInvocation = addDependencyInvocation.withPrefix(Space.format("\n\n" + addDependencyInvocation.getPrefix().getIndent()));
                            }
                        }
                        statements.add(addDependencyInvocation);
                    }
                    body = body.withStatements(statements);
                    return lambda.withBody(body.withStatements(statements));
                }
                return arg;
            }));
        }
        return m;
    }

    /**
     * Update the dependency model, adding the specified dependency to the specified configuration and all configurations
     * which extend from it.
     *
     * @param buildScript   compilation unit owning the {@link GradleProject} marker
     * @param configuration the configuration to add the dependency to
     * @param gav           the group, artifact, and version of the dependency to add
     * @param classifier    the classifier of the dependency to add
     * @param ctx           context which will be used to download the pom for the dependency
     * @return a copy of buildScript with the dependency added
     */
    public static JavaSourceFile addDependency(
            JavaSourceFile buildScript,
            @Nullable GradleDependencyConfiguration configuration,
            GroupArtifactVersion gav,
            @Nullable String classifier,
            ExecutionContext ctx) {
        if (gav.getGroupId() == null || gav.getArtifactId() == null || configuration == null) {
            return buildScript;
        }
        GradleProject gp = buildScript.getMarkers().findFirst(GradleProject.class)
                .orElseThrow(() -> new IllegalArgumentException("Could not find GradleProject"));

        try {
            ResolvedGroupArtifactVersion resolvedGav;
            List<ResolvedDependency> transitiveDependencies;
            if (gav.getVersion() == null) {
                resolvedGav = null;
                transitiveDependencies = emptyList();
            } else {
                MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                Pom pom = mpd.download(gav, null, null, gp.getMavenRepositories());
                ResolvedPom resolvedPom = pom.resolve(emptyList(), mpd, gp.getMavenRepositories(), ctx);
                resolvedGav = resolvedPom.getGav();
                transitiveDependencies = resolvedPom.resolveDependencies(Scope.Runtime, mpd, ctx);
            }
            Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
            Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());

            Set<GradleDependencyConfiguration> configurationsToAdd = Stream.concat(
                            Stream.of(configuration),
                            gp.configurationsExtendingFrom(configuration, true).stream())
                    .collect(toSet());

            for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                if (!configurationsToAdd.contains(gdc)) {
                    newNameToConfiguration.put(gdc.getName(), gdc);
                    continue;
                }

                GradleDependencyConfiguration newGdc = gdc;
                org.openrewrite.maven.tree.Dependency newRequested = new org.openrewrite.maven.tree.Dependency(
                        gav, classifier, "jar", gdc.getName(), emptyList(), null);
                newGdc = newGdc.withRequested(ListUtils.concat(
                        ListUtils.map(gdc.getRequested(), requested -> {
                            // Remove any existing dependency with the same group and artifact id
                            if (Objects.equals(requested.getGroupId(), gav.getGroupId()) && Objects.equals(requested.getArtifactId(), gav.getArtifactId())) {
                                return null;
                            }
                            return requested;
                        }),
                        newRequested));
                if (newGdc.isCanBeResolved() && resolvedGav != null) {
                    newGdc = newGdc.withDirectResolved(ListUtils.concat(
                            ListUtils.map(gdc.getDirectResolved(), resolved -> {
                                // Remove any existing dependency with the same group and artifact id
                                if (Objects.equals(resolved.getGroupId(), resolvedGav.getGroupId()) && Objects.equals(resolved.getArtifactId(), resolvedGav.getArtifactId())) {
                                    return null;
                                }
                                return resolved;
                            }),
                            new ResolvedDependency(null, resolvedGav, newRequested, transitiveDependencies,
                                    emptyList(), "jar", classifier, null, 0, null)));
                }
                newNameToConfiguration.put(newGdc.getName(), newGdc);
            }
            gp = gp.withNameToConfiguration(newNameToConfiguration);
        } catch (MavenDownloadingException | MavenDownloadingExceptions | IllegalArgumentException e) {
            return Markup.warn(buildScript, e);
        }
        return buildScript.withMarkers(buildScript.getMarkers().setByType(gp));
    }

    enum DependencyStyle {
        Map, String
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> List<J2> cache(Cursor cursor, Object key, Supplier<List<? extends J>> ifAbsent) {
        List<J2> js = null;

        Cursor root = cursor.getRoot();
        Map<Object, List<J2>> cache = root.getMessage(TEMPLATE_CACHE_MESSAGE_KEY);
        if (cache == null) {
            cache = new HashMap<>();
            root.putMessage(TEMPLATE_CACHE_MESSAGE_KEY, cache);
        } else {
            js = cache.get(key);
        }

        if (js == null) {
            js = (List<J2>) ifAbsent.get();
            cache.put(key, js);
        }

        return ListUtils.map(js, j -> (J2) new RandomizeIdVisitor<Integer>().visit(j, 0));
    }

    private boolean hasDependenciesBlock(List<Statement> statements) {
        for (Statement statement : statements) {
            if (statement instanceof J.MethodInvocation && DEPENDENCIES_DSL_MATCHER.matches((J.MethodInvocation) statement, true)) {
                return true;
            } else if (statement instanceof J.Return &&
                    ((J.Return) statement).getExpression() instanceof J.MethodInvocation &&
                    DEPENDENCIES_DSL_MATCHER.matches((J.MethodInvocation) ((J.Return) statement).getExpression(), true)) {
                return true;
            }
        }
        return false;
    }

    private J.MethodInvocation dependenciesDeclaration(ExecutionContext ctx) {
        String template = templateDependencies();
        return (J.MethodInvocation) cache(getCursor(), new ContextFreeCacheKey(template, isKotlinDsl, J.MethodInvocation.class), () -> {
            Boolean requirePrintEqualsInput = ctx.getMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT);
            try {
                J.MethodInvocation dependencies;
                ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
                if (isKotlinDsl) {
                    dependencies = (J.MethodInvocation) ((J.Block) GRADLE_PARSER.parseInputs(singletonList(new GradleParser.Input(Paths.get("build.gradle.kts"), () -> new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)))), null, ctx)
                            .findFirst()
                            .map(K.CompilationUnit.class::cast)
                            .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                            .getStatements().get(0)).getStatements().get(0);
                } else {
                    dependencies = (J.MethodInvocation) GRADLE_PARSER.parse(ctx, template)
                            .findFirst()
                            .map(G.CompilationUnit.class::cast)
                            .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                            .getStatements().get(0);
                }
                return singletonList(dependencies);
            } finally {
                ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, requirePrintEqualsInput);
            }
        }).get(0);
    }

    private J.MethodInvocation dependencyDeclaration(J.Block body, ExecutionContext ctx, Cursor cursor) {
        String template = templateDependency(body);
        return (J.MethodInvocation) autoFormat(cache(cursor, new ContextFreeCacheKey(template, isKotlinDsl, J.MethodInvocation.class), () -> {
            Boolean requirePrintEqualsInput = ctx.getMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT);
            try {
                J.MethodInvocation dependency;
                ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
                if (isKotlinDsl) {
                    dependency = (J.MethodInvocation) ((J.Block) GRADLE_PARSER.parseInputs(singletonList(new GradleParser.Input(Paths.get("build.gradle.kts"), () -> new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)))), null, ctx)
                            .findFirst()
                            .map(K.CompilationUnit.class::cast)
                            .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                            .getStatements().get(0)).getStatements().get(0);
                } else {
                    dependency = (J.MethodInvocation) GRADLE_PARSER.parse(ctx, template)
                            .findFirst()
                            .map(G.CompilationUnit.class::cast)
                            .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                            .getStatements().get(0);
                }
                return singletonList(dependency.withPrefix(Space.format("\n")));
            } finally {
                ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, requirePrintEqualsInput);
            }
        }).get(0), ctx, cursor);
    }

    private String templateDependencies() {
        return "dependencies {\n    " + templateDependency(null) + "\n}";
    }

    private String templateDependency(J.@Nullable Block body) {
        DependencyStyle style = autodetectDependencyStyle(body);
        if (isKotlinDsl) {
            return escapeIfNecessary(configuration) + "(" + templatePlatform(templateDependencyNotation(KOTLIN_MAP_SEPARATOR, style)) + ")";
        } else {
            return escapeIfNecessary(configuration) + " " + templatePlatform(templateDependencyNotation(GROOVY_MAP_SEPARATOR, style));
        }
    }

    private String templatePlatform(String dependencyNotation) {
        if (dependencyModifier == PLATFORM) {
            return "platform(" + dependencyNotation + ")";
        } else if (dependencyModifier == ENFORCED_PLATFORM) {
            return "enforcedPlatform(" + dependencyNotation + ")";
        }
        return dependencyNotation;
    }

    private String templateDependencyNotation(String mapSeparator, DependencyStyle style) {
        if (style == DependencyStyle.String) {
            return "\"" + groupId + ":" + artifactId + (version == null ? "" : ":" + version) + (version == null || classifier == null ? "" : ":" + classifier) + (extension == null ? "" : "@" + extension) + "\"";
        } else {
            return "group" + mapSeparator + "\"" + groupId + "\", name" + mapSeparator + "\"" + artifactId + "\"" + (version == null ? "" : ", version" + mapSeparator + "\"" + version + "\"") + (classifier == null ? "" : ", classifier" + mapSeparator + "\"" + classifier + "\"") + (extension == null ? "" : ", ext" + mapSeparator + "\"" + extension + "\"");
        }
    }

    private String escapeIfNecessary(String configurationName) {
        // default is a gradle configuration created by the base plugin and a groovy keyword if
        // it is used it needs to be escaped
        return "default".equals(configurationName) ? "'" + configurationName + "'" : configurationName;
    }

    private DependencyStyle autodetectDependencyStyle(J.@Nullable Block block) {
        if (block == null) {
            return DependencyStyle.String;
        }

        List<Statement> statements = block.getStatements();
        int string = 0;
        int map = 0;
        for (Statement statement : statements) {
            if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation) {
                J.MethodInvocation invocation = (J.MethodInvocation) ((J.Return) statement).getExpression();
                if (invocation.getArguments().get(0) instanceof J.Literal || invocation.getArguments().get(0) instanceof G.GString) {
                    string++;
                } else if (invocation.getArguments().get(0) instanceof G.MapEntry) {
                    map++;
                } else if (invocation.getArguments().get(0) instanceof J.Assignment) {
                    map++;
                }
            } else if (statement instanceof J.MethodInvocation) {
                J.MethodInvocation invocation = (J.MethodInvocation) statement;
                if (invocation.getArguments().get(0) instanceof J.Literal || invocation.getArguments().get(0) instanceof G.GString) {
                    string++;
                } else if (invocation.getArguments().get(0) instanceof G.MapEntry) {
                    map++;
                } else if (invocation.getArguments().get(0) instanceof J.Assignment) {
                    map++;
                }
            }
        }

        return string >= map ? DependencyStyle.String : DependencyStyle.Map;
    }

    @Value
    private static class ContextFreeCacheKey {
        String template;
        boolean isKotlinDsl;
        Class<? extends J> expected;
    }
}
