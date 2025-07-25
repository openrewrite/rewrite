/*
 * Copyright 2024 the original author or authors.
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
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.format.BlankLinesVisitor;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Preconditions.not;
import static org.openrewrite.gradle.UpgradeDependencyVersion.getGradleProjectKey;
import static org.openrewrite.gradle.UpgradeDependencyVersion.replaceVersion;

@SuppressWarnings("GroovyAssignabilityCheck")
@Incubating(since = "8.18.0")
@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<UpgradeTransitiveDependencyVersion.DependencyVersionState> {
    private static final MethodMatcher DEPENDENCIES_DSL_MATCHER = new MethodMatcher("RewriteGradleProject dependencies(..)");
    private static final MethodMatcher CONSTRAINTS_MATCHER = new MethodMatcher("org.gradle.api.artifacts.dsl.DependencyHandler constraints(..)", true);
    private static final String CONSTRAINT_MATCHER = "org.gradle.api.artifacts.dsl.DependencyHandler *(..)";

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors). " +
                          "Defaults to `latest.release`.",
            example = "29.X",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'newVersion' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Because",
            description = "The reason for upgrading the transitive dependency. For example, we could be responding to a vulnerability.",
            required = false,
            example = "CVE-2021-1234")
    @Nullable
    String because;

    @Option(displayName = "Include configurations",
            description = "A list of configurations to consider during the upgrade. For example, For example using `implementation, runtimeOnly`, we could be responding to a deployable asset vulnerability only (ignoring test scoped vulnerabilities).",
            required = false,
            example = "implementation, runtimeOnly")
    @Nullable
    List<String> onlyForConfigurations;

    /**
     * This recipe needs to generate LST elements representing "constraints" and "because" method invocations.
     * Aside from their parameterization with different arguments they are otherwise identical.
     * GradleParser isn't particularly fast, so in a recipe run which involves more than one UpgradeTransitiveDependencyVersion
     * it is much faster to produce these LST elements only once then manipulate their arguments.
     * This largely mimics how caching works in JavaTemplate. If we create a Gradle/GroovyTemplate this could be refactored.
     */
    private static Map<String, Optional<JavaSourceFile>> snippetCache(ExecutionContext ctx) {
        //noinspection unchecked
        return (Map<String, Optional<JavaSourceFile>>) ctx.getMessages()
                .computeIfAbsent(UpgradeTransitiveDependencyVersion.class.getName() + ".snippetCache", k -> new HashMap<String, Optional<G.CompilationUnit>>());
    }

    private static Optional<JavaSourceFile> parseAsGradle(String snippet, boolean isKotlinDsl, ExecutionContext ctx) {
        return snippetCache(ctx)
                .computeIfAbsent(snippet, s -> GradleParser.builder().build().parseInputs(Collections.singletonList(
                                new Parser.Input(
                                        Paths.get("build.gradle" + (isKotlinDsl ? ".kts" : "")),
                                        () -> new ByteArrayInputStream(snippet.getBytes(StandardCharsets.UTF_8))
                                )), null, ctx)
                        .findFirst()
                        .map(maybeCu -> {
                            maybeCu.getMarkers()
                                    .findFirst(ParseExceptionResult.class)
                                    .ifPresent(per -> {
                                        throw new IllegalStateException("Encountered exception " + per.getExceptionType() + " with message " + per.getMessage() + " on snippet:\n" + snippet);
                                    });
                            return (JavaSourceFile) maybeCu;
                        }));
    }

    @Override
    public String getDisplayName() {
        return "Upgrade transitive Gradle dependencies";
    }

    @Override
    public String getDescription() {
        return "Upgrades the version of a transitive dependency in a Gradle build file. " +
               "There are many ways to do this in Gradle, so the mechanism for upgrading a " +
               "transitive dependency must be considered carefully depending on your style " +
               "of dependency management.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    @Value
    public static class DependencyVersionState {
        Map<String, Map<GroupArtifact, Map<GradleDependencyConfiguration, String>>> updatesPerProject = new LinkedHashMap<>();
        Set<GradleProject> modules = new HashSet<>();
    }

    @Override
    public DependencyVersionState getInitialValue(ExecutionContext ctx) {
        return new DependencyVersionState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(DependencyVersionState acc) {
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker), new JavaVisitor<ExecutionContext>() {
            @SuppressWarnings("NotNullFieldNotInitialized")
            GradleProject gradleProject;

            final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                       (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts"));
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    gradleProject = tree.getMarkers().findFirst(GradleProject.class)
                            .orElseThrow(() -> new IllegalStateException("Unable to find GradleProject marker."));
                    acc.modules.add(gradleProject);
                    acc.updatesPerProject.putIfAbsent(getGradleProjectKey(gradleProject), new HashMap<>());

                    DependencyVersionSelector versionSelector = new DependencyVersionSelector(metadataFailures, gradleProject, null);

                    // Determine the configurations used to declare dependencies that requested dependencies in the build
                    List<GradleDependencyConfiguration> declaredConfigurations = gradleProject.getConfigurations().stream()
                            .filter(c -> c.isCanBeDeclared() && !c.getRequested().isEmpty())
                            .collect(Collectors.toList());

                    configurations:
                    for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
                        // Skip when there's a direct dependency, as per openrewrite/rewrite#5355
                        for (Dependency dependency : configuration.getRequested()) {
                            if (dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                                continue configurations;
                            }
                        }
                        for (ResolvedDependency resolved : configuration.getResolved()) {
                            if (resolved.getDepth() > 0 && dependencyMatcher.matches(resolved.getGroupId(),
                                    resolved.getArtifactId(), resolved.getVersion())) {
                                try {
                                    String selected = versionSelector.select(resolved.getGav(), configuration.getName(),
                                            version, versionPattern, ctx);
                                    if (selected == null || resolved.getVersion().equals(selected)) {
                                        continue;
                                    }

                                    GradleDependencyConfiguration constraintConfig = constraintConfiguration(configuration, declaredConfigurations);
                                    if (constraintConfig == null) {
                                        continue;
                                    }

                                    acc.updatesPerProject.get(getGradleProjectKey(gradleProject)).merge(
                                            new GroupArtifact(resolved.getGroupId(), resolved.getArtifactId()),
                                            singletonMap(constraintConfig, selected),
                                            (existing, update) -> {
                                                Map<GradleDependencyConfiguration, String> all = new LinkedHashMap<>(existing);
                                                all.putAll(update);
                                                all.keySet().removeIf(c -> {
                                                    if (c == null) {
                                                        return true; // TODO ?? how does this happen
                                                    }

                                                    for (GradleDependencyConfiguration config : all.keySet()) {
                                                        if (c.allExtendsFrom().contains(config)) {
                                                            return true;
                                                        }

                                                        // TODO there has to be a better way!
                                                        if (c.getName().equals("runtimeOnly")) {
                                                            if (config.getName().equals("implementation")) {
                                                                return true;
                                                            }
                                                        }
                                                        if (c.getName().equals("testRuntimeOnly")) {
                                                            if (config.getName().equals("testImplementation") ||
                                                                config.getName().equals("implementation")) {
                                                                return true;
                                                            }
                                                        }
                                                    }
                                                    return false;
                                                });
                                                return all;
                                            }
                                    );
                                } catch (MavenDownloadingException e) {
                                    return Markup.warn((JavaSourceFile) tree, e);
                                }
                            }
                        }
                    }
                }
                return super.visit(tree, ctx);
            }

            /*
             * It is typical in Gradle for there to be a number of unresolvable configurations that developers put
             * dependencies into in their build files, like implementation. Other configurations like compileClasspath
             * and runtimeClasspath are resolvable and will directly or transitively extend from those unresolvable
             * configurations.
             *
             * It isn't impossible to manage the version of the dependency directly in the child configuration, but you
             * might end up with the same dependency managed multiple times, something like this:
             *
             * constraints {
             *     runtimeClasspath("g:a:v") { }
             *     compileClasspath("g:a:v") { }
             *     testRuntimeClasspath("g:a:v") { }
             *     testCompileClasspath("g:a:v") { }
             * }
             *
             * whereas if we find a common root configuration, the above can be simplified to:
             *
             * constraints {
             *     implementation("g:a:v") { }
             * }
             */
            private @Nullable GradleDependencyConfiguration constraintConfiguration(GradleDependencyConfiguration config, List<GradleDependencyConfiguration> declaredConfigurations) {
                // Check if the resolved configuration e.g. compileClasspath extends from a declared configuration
                // defined in the build e.g. implementation. Constraints should only use the configuration name of the
                // dependency declared in the build.
                Optional<GradleDependencyConfiguration> declaredConfig = config.getExtendsFrom().stream()
                        .filter(declaredConfigurations::contains)
                        .findFirst();

                // The configuration name for the used constraint should be the name of the declared configuration if it exists,
                // otherwise the name of the current configuration
                String constraintConfigName = declaredConfig.map(GradleDependencyConfiguration::getName)
                        .orElseGet(config::getName);

                if (onlyForConfigurations != null && !onlyForConfigurations.isEmpty()) {
                    if (!onlyForConfigurations.contains(constraintConfigName)) {
                        return null;
                    }
                } else {
                    for (GradleDependencyConfiguration extended : config.getExtendsFrom()) {
                        if (extended.getName().equals(constraintConfigName)) {
                            return extended;
                        }
                    }
                }

                GradleDependencyConfiguration configuration = gradleProject.getConfiguration(constraintConfigName);
                if (configuration != null && configuration.isTransitive()) {
                    return configuration;
                }

                return null;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyVersionState acc) {
        final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker), new TreeVisitor<Tree, ExecutionContext>() {
            private final UpdateGradle updateGradle = new UpdateGradle(acc.getUpdatesPerProject());
            private final UpdateDependencyLock updateLockFile = new UpdateDependencyLock(acc.modules);

            @Override
            public boolean isAcceptable(SourceFile sf, ExecutionContext ctx) {
                return updateGradle.isAcceptable(sf, ctx) || updateLockFile.isAcceptable(sf, ctx);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                if (t instanceof SourceFile) {
                    SourceFile sf = (SourceFile) t;
                    if (updateGradle.isAcceptable(sf, ctx)) {
                        t = updateGradle.visitNonNull(t, ctx);
                    }
                    Optional<GradleProject> projectMarker = t.getMarkers().findFirst(GradleProject.class);
                    if ((tree != t || updateLockFile.isAcceptable(sf, ctx)) && projectMarker.isPresent()) {
                        GradleProject gradleProject = projectMarker.get();
                        gradleProject = updatedModel(projectMarker.get(), acc.updatesPerProject.get(getGradleProjectKey(gradleProject)), ctx);
                        if (projectMarker.get() != gradleProject) {
                            t = t.withMarkers(t.getMarkers().setByType(gradleProject));
                        }
                    }
                    if (updateLockFile.isAcceptable(sf, ctx)) {
                        t = updateLockFile.visitNonNull(t, ctx);
                    }
                }
                return t;
            }

            private GradleProject updatedModel(GradleProject gradleProject, Map<GroupArtifact, Map<GradleDependencyConfiguration, String>> toUpdate, ExecutionContext ctx) {
                GradleProject gp = gradleProject;
                Set<String> configNames = gp.getConfigurations().stream()
                        .map(GradleDependencyConfiguration::getName)
                        .collect(Collectors.toSet());
                Set<GroupArtifactVersion> gavs = new LinkedHashSet<>();
                for (Map.Entry<GroupArtifact, Map<GradleDependencyConfiguration, String>> update : toUpdate.entrySet()) {
                    if (!dependencyMatcher.matches(update.getKey().getGroupId(), update.getKey().getArtifactId())) {
                        continue;
                    }
                    Map<GradleDependencyConfiguration, String> configs = update.getValue();
                    String groupId = update.getKey().getGroupId();
                    String artifactId = update.getKey().getArtifactId();
                    for (Map.Entry<GradleDependencyConfiguration, String> configToVersion : configs.entrySet()) {
                        String newVersion = configToVersion.getValue();
                        gavs.add(new GroupArtifactVersion(groupId, artifactId, newVersion));
                    }
                }
                for (GroupArtifactVersion gav : gavs) {
                    gp = replaceVersion(gp, ctx, gav, configNames);
                }
                return gp;
            }
        });
    }


    @RequiredArgsConstructor
    private class UpdateGradle extends JavaVisitor<ExecutionContext> {

        final Map<String, Map<GroupArtifact, Map<GradleDependencyConfiguration, String>>> updatesPerProject;
        final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

        @SuppressWarnings("NotNullFieldNotInitialized")
        GradleProject gradleProject;

        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                gradleProject = cu.getMarkers().findFirst(GradleProject.class).orElseThrow(() -> new IllegalStateException("Unable to find GradleProject marker."));

                Map<GroupArtifact, Map<GradleDependencyConfiguration, String>> projectRequiredUpdates = updatesPerProject.getOrDefault(getGradleProjectKey(gradleProject), emptyMap());
                if (!projectRequiredUpdates.isEmpty()) {
                    if (projectRequiredUpdates.keySet().stream().noneMatch(ga -> dependencyMatcher.matches(ga.getGroupId(), ga.getArtifactId()))) {
                        return cu;
                    }
                    cu = (JavaSourceFile) Preconditions.check(
                            not(new JavaIsoVisitor<ExecutionContext>() {
                                @Override
                                public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                                    if (tree instanceof G.CompilationUnit) {
                                        return new UsesMethod<>(CONSTRAINTS_MATCHER).visit(tree, ctx);
                                    } else {
                                        // K is not type attributed, so do things more manually
                                        return super.visit(tree, ctx);
                                    }
                                }

                                @Override
                                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                    if (m.getSimpleName().equals("constraints") && withinBlock(getCursor(), "dependencies")) {
                                        return SearchResult.found(m);
                                    }
                                    return m;
                                }
                            }),
                            new AddConstraintsBlock(cu instanceof K.CompilationUnit)
                    ).visitNonNull(cu, ctx);

                    for (Map.Entry<GroupArtifact, Map<GradleDependencyConfiguration, String>> update : projectRequiredUpdates.entrySet()) {
                        if (!dependencyMatcher.matches(update.getKey().getGroupId(), update.getKey().getArtifactId())) {
                            continue;
                        }
                        Map<GradleDependencyConfiguration, String> configs = update.getValue();
                        for (Map.Entry<GradleDependencyConfiguration, String> config : configs.entrySet()) {
                            cu = (JavaSourceFile) new AddConstraint(cu instanceof K.CompilationUnit, config.getKey().getName(), new GroupArtifactVersion(update.getKey().getGroupId(),
                                    update.getKey().getArtifactId(), config.getValue()), gradleProject, because).visitNonNull(cu, ctx);
                        }
                    }

                    // Spring dependency management plugin stomps on constraints. Use an alternative mechanism it does not override
                    if (gradleProject.getPlugins().stream()
                            .anyMatch(plugin -> "io.spring.dependency-management".equals(plugin.getId()))) {
                        cu = (JavaSourceFile) new DependencyConstraintToRule().getVisitor().visitNonNull(cu, ctx);
                    }
                }

                return cu;
            }
            return super.visit(tree, ctx);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class AddConstraintsBlock extends JavaIsoVisitor<ExecutionContext> {
        boolean isKotlinDsl;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (DEPENDENCIES_DSL_MATCHER.matches(method)) {
                G.CompilationUnit withConstraints = (G.CompilationUnit) parseAsGradle(
                        //language=groovy
                        "plugins { id 'java' }\n" +
                        "dependencies {\n" +
                        "    constraints {\n" +
                        "    }\n" +
                        "}\n", false, ctx)
                        .orElseThrow(() -> new IllegalStateException("Unable to parse constraints block"));

                Statement constraints = FindMethods.find(withConstraints, "org.gradle.api.artifacts.dsl.DependencyHandler constraints(..)", true)
                        .stream()
                        .filter(J.MethodInvocation.class::isInstance)
                        .map(J.MethodInvocation.class::cast)
                        .filter(m2 -> m2.getSimpleName().equals("constraints"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to find constraints block"))
                        .withMarkers(Markers.EMPTY);

                return autoFormat(m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                    if (!(arg instanceof J.Lambda)) {
                        return arg;
                    }
                    J.Lambda dependencies = (J.Lambda) arg;
                    if (!(dependencies.getBody() instanceof J.Block)) {
                        return m;
                    }
                    J.Block body = (J.Block) dependencies.getBody();

                    List<Statement> statements = ListUtils.mapFirst(body.getStatements(), stat -> stat.withPrefix(stat.getPrefix().withWhitespace(
                            BlankLinesVisitor.minimumLines(stat.getPrefix().getWhitespace(), 1))));
                    return dependencies.withBody(body.withStatements(
                            ListUtils.concat(constraints, statements)));
                })), constraints, ctx, getCursor().getParentOrThrow());
            } else if (isKotlinDsl && m.getSimpleName().equals("dependencies") && getCursor().getParentTreeCursor().firstEnclosing(J.MethodInvocation.class) == null) {
                K.CompilationUnit withConstraints = (K.CompilationUnit) parseAsGradle(
                        //language=kotlin
                        "plugins { id(\"java\") }\n" +
                        "dependencies {\n" +
                        "    constraints {}\n" +
                        "}\n", true, ctx)
                        .orElseThrow(() -> new IllegalStateException("Unable to parse constraints block"));

                J.MethodInvocation constraints = withConstraints.getStatements()
                        .stream()
                        .map(J.Block.class::cast)
                        .flatMap(block -> block.getStatements().stream())
                        .filter(J.MethodInvocation.class::isInstance)
                        .map(J.MethodInvocation.class::cast)
                        .filter(m2 -> m2.getSimpleName().equals("dependencies"))
                        .flatMap(dependencies -> ((J.Block) ((J.Lambda) dependencies.getArguments().get(0)).getBody()).getStatements().stream())
                        .filter(J.MethodInvocation.class::isInstance)
                        .map(J.MethodInvocation.class::cast)
                        .filter(m2 -> m2.getSimpleName().equals("constraints"))
                        .findFirst()
                        .map(m2 -> m2.withArguments(ListUtils.mapFirst(m2.getArguments(), arg -> {
                            J.Lambda lambda = (J.Lambda) arg;
                            return lambda.withBody(((J.Block) lambda.getBody()).withEnd(Space.format("\n")));
                        })))
                        .orElseThrow(() -> new IllegalStateException("Unable to find constraints block"))
                        .withMarkers(Markers.EMPTY);

                return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                    if (!(arg instanceof J.Lambda)) {
                        return arg;
                    }
                    J.Lambda dependencies = (J.Lambda) arg;
                    if (!(dependencies.getBody() instanceof J.Block)) {
                        return m;
                    }
                    J.Block body = (J.Block) dependencies.getBody();

                    List<Statement> statements = ListUtils.mapFirst(body.getStatements(), stat -> stat.withPrefix(stat.getPrefix().withWhitespace(
                            BlankLinesVisitor.minimumLines(stat.getPrefix().getWhitespace(), 1))));
                    return dependencies.withBody(body.withStatements(
                            ListUtils.concat(autoFormat(constraints, ctx, getCursor().getParentOrThrow()), statements)));
                }));
            }

            return m;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class AddConstraint extends JavaIsoVisitor<ExecutionContext> {
        boolean isKotlinDsl;
        String config;
        GroupArtifactVersion gav;
        GradleProject gradleProject;

        @Nullable
        String because;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!CONSTRAINTS_MATCHER.matches(m) && !(isKotlinDsl && m.getSimpleName().equals("constraints") && withinBlock(getCursor(), "dependencies"))) {
                return m;
            }
            String ga = gav.getGroupId() + ":" + gav.getArtifactId();
            String existingConstraintVersion = null;
            J.MethodInvocation existingConstraint = null;
            List<J.MethodInvocation> constraintsToRemove = new ArrayList<>();
            MethodMatcher constraintMatcher = new MethodMatcher(CONSTRAINT_MATCHER, true);

            // Find the configuration being added
            GradleDependencyConfiguration targetConfig = gradleProject.getConfiguration(config);

            // Check all constraints
            if (!(m.getArguments().get(0) instanceof J.Lambda) || !(((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block)) {
                return m;
            }
            for (Statement statement : ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements()) {
                if (statement instanceof J.MethodInvocation || (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation)) {
                    J.MethodInvocation m2 = (J.MethodInvocation) (statement instanceof J.Return ? ((J.Return) statement).getExpression() : statement);
                    if (constraintMatcher.matches(m2) || (isKotlinDsl && m.getSimpleName().equals("constraints"))) {
                        if (matchesConstraint(m2, ga)) {
                            if (m2.getSimpleName().equals(config)) {
                                existingConstraint = m2;
                                org.openrewrite.gradle.internal.Dependency notation = DependencyStringNotationConverter.parse((String) requireNonNull(((J.Literal) m2.getArguments().get(0)).getValue()));
                                if (notation == null) {
                                    continue;
                                }
                                existingConstraintVersion = notation.getVersion();
                            } else if (targetConfig != null) {
                                // Check if this constraint is on a configuration that extends from our target
                                GradleDependencyConfiguration constraintConfig = gradleProject.getConfiguration(m2.getSimpleName());
                                if (constraintConfig != null && constraintConfig.allExtendsFrom().contains(targetConfig)) {
                                    constraintsToRemove.add(m2);
                                }
                            }
                        }
                    }
                }
            }

            if (Objects.equals(gav.getVersion(), existingConstraintVersion)) {
                return m;
            }

            // Remove constraints from child configurations
            if (!constraintsToRemove.isEmpty()) {
                m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                    if (!(arg instanceof J.Lambda)) {
                        return arg;
                    }
                    J.Lambda lambda = (J.Lambda) arg;
                    if (!(lambda.getBody() instanceof J.Block)) {
                        return arg;
                    }
                    J.Block body = (J.Block) lambda.getBody();
                    List<Statement> statements = new ArrayList<>(body.getStatements());
                    statements.removeIf(statement -> {
                        if (statement instanceof J.MethodInvocation) {
                            return constraintsToRemove.contains(statement);
                        } else if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation) {
                            return constraintsToRemove.contains(((J.Return) statement).getExpression());
                        }
                        return false;
                    });
                    return lambda.withBody(body.withStatements(statements));
                }));
            }

            if (existingConstraint == null) {
                m = (J.MethodInvocation) new CreateConstraintVisitor(config, gav, because, isKotlinDsl)
                        .visitNonNull(m, ctx, requireNonNull(getCursor().getParent()));
            } else {
                m = (J.MethodInvocation) new UpdateConstraintVersionVisitor(gav, existingConstraint, because, isKotlinDsl)
                        .visitNonNull(m, ctx, requireNonNull(getCursor().getParent()));
            }
            return m;
        }

        private boolean matchesConstraint(J.MethodInvocation m, String ga) {
            Expression arg = m.getArguments().get(0);
            if (!(arg instanceof J.Literal) || ((J.Literal) arg).getValue() == null) {
                return false;
            }
            String value = ((J.Literal) arg).getValue().toString();
            return value.startsWith(ga);
        }
    }

    //language=groovy
    private static final String INDIVIDUAL_CONSTRAINT_SNIPPET_GROOVY =
            "plugins {\n" +
            "    id 'java'\n" +
            "}\n" +
            "dependencies {\n" +
            "    constraints {\n" +
            "        implementation('foobar')\n" +
            "    }\n" +
            "}";
    //language=kotlin
    private static final String INDIVIDUAL_CONSTRAINT_SNIPPET_KOTLIN =
            "plugins {\n" +
            "    id(\"java\")\n" +
            "}\n" +
            "dependencies {\n" +
            "    constraints {\n" +
            "        implementation(\"foobar\")\n" +
            "    }\n" +
            "}";
    //language=groovy
    private static final String INDIVIDUAL_CONSTRAINT_BECAUSE_SNIPPET_GROOVY =
            "plugins {\n" +
            "    id 'java'\n" +
            "}\n" +
            "dependencies {\n" +
            "    constraints {\n" +
            "        implementation('foobar') {\n" +
            "            because 'because'\n" +
            "        }\n" +
            "    }\n" +
            "}";
    //language=kotlin
    private static final String INDIVIDUAL_CONSTRAINT_BECAUSE_SNIPPET_KOTLIN =
            "plugins {\n" +
            "    id(\"java\")\n" +
            "}\n" +
            "dependencies {\n" +
            "    constraints {\n" +
            "        implementation(\"foobar\") {\n" +
            "            because(\"because\")\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CreateConstraintVisitor extends JavaIsoVisitor<ExecutionContext> {

        String config;
        GroupArtifactVersion gav;

        @Nullable
        String because;

        boolean isKotlinDsl;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if ("version".equals(method.getSimpleName())) {
                return method;
            }
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            J.MethodInvocation constraint;
            if (!isKotlinDsl) {
                constraint = parseAsGradle(because == null ? INDIVIDUAL_CONSTRAINT_SNIPPET_GROOVY : INDIVIDUAL_CONSTRAINT_BECAUSE_SNIPPET_GROOVY, false, ctx)
                        .map(G.CompilationUnit.class::cast)
                        .map(it -> (J.MethodInvocation) it.getStatements().get(1))
                        .map(dependenciesMethod -> (J.Lambda) dependenciesMethod.getArguments().get(0))
                        .map(dependenciesClosure -> (J.Block) dependenciesClosure.getBody())
                        .map(dependenciesBody -> (J.Return) dependenciesBody.getStatements().get(0))
                        .map(returnConstraints -> (J.MethodInvocation) returnConstraints.getExpression())
                        .map(constraintsInvocation -> (J.Lambda) constraintsInvocation.getArguments().get(0))
                        .map(constraintsLambda -> (J.Block) constraintsLambda.getBody())
                        .map(constraintsBlock -> (J.Return) constraintsBlock.getStatements().get(0))
                        .map(returnConfiguration -> (J.MethodInvocation) returnConfiguration.getExpression())
                        .map(it -> it.withName(it.getName().withSimpleName(config))
                                .withArguments(ListUtils.map(it.getArguments(), arg -> {
                                    if (arg instanceof J.Literal) {
                                        return ((J.Literal) requireNonNull(arg))
                                                .withValue(gav.toString())
                                                .withValueSource("'" + gav + "'");
                                    } else if (arg instanceof J.Lambda && because != null) {
                                        return (Expression) new GroovyIsoVisitor<Integer>() {
                                            @Override
                                            public J.Literal visitLiteral(J.Literal literal, Integer integer) {
                                                return literal.withValue(because)
                                                        .withValueSource("'" + because + "'");
                                            }
                                        }.visitNonNull(arg, 0);
                                    }
                                    return arg;
                                })))
                        // Assign a unique ID so multiple constraints can be added
                        .map(it -> it.withId(Tree.randomId()))
                        .orElseThrow(() -> new IllegalStateException("Unable to find constraint"));
            } else {
                constraint = parseAsGradle(because == null ? INDIVIDUAL_CONSTRAINT_SNIPPET_KOTLIN : INDIVIDUAL_CONSTRAINT_BECAUSE_SNIPPET_KOTLIN, true, ctx)
                        .map(K.CompilationUnit.class::cast)
                        .map(it -> (J.Block) it.getStatements().get(0))
                        .map(it -> (J.MethodInvocation) it.getStatements().get(1))
                        .map(dependenciesMethod -> (J.Lambda) dependenciesMethod.getArguments().get(0))
                        .map(dependenciesClosure -> (J.Block) dependenciesClosure.getBody())
                        .map(dependenciesBody -> (J.MethodInvocation) dependenciesBody.getStatements().get(0))
                        .map(constraintsInvocation -> (J.Lambda) constraintsInvocation.getArguments().get(0))
                        .map(constraintsLambda -> (J.Block) constraintsLambda.getBody())
                        .map(constraintsBlock -> (J.MethodInvocation) constraintsBlock.getStatements().get(0))
                        .map(it -> it.withName(it.getName().withSimpleName(config))
                                .withArguments(ListUtils.map(it.getArguments(), arg -> {
                                    if (arg instanceof J.Literal) {
                                        return ChangeStringLiteral.withStringValue((J.Literal) requireNonNull(arg), gav.toString());
                                    } else if (arg instanceof J.Lambda && because != null) {
                                        return (Expression) new KotlinIsoVisitor<Integer>() {
                                            @Override
                                            public J.Literal visitLiteral(J.Literal literal, Integer integer) {
                                                return ChangeStringLiteral.withStringValue(literal, because);
                                            }
                                        }.visitNonNull(arg, 0);
                                    }
                                    return arg;
                                })))
                        // Assign a unique ID so multiple constraints can be added
                        .map(it -> it.withId(Tree.randomId()))
                        .orElseThrow(() -> new IllegalStateException("Unable to find constraint"));
            }

            return autoFormat(m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                if (!(arg instanceof J.Lambda)) {
                    return arg;
                }
                J.Lambda dependencies = (J.Lambda) arg;
                if (!(dependencies.getBody() instanceof J.Block)) {
                    return arg;
                }
                J.Block body = (J.Block) dependencies.getBody();

                return dependencies.withBody(body.withStatements(
                        ListUtils.concat(constraint, body.getStatements())));
            })), ctx, getCursor().getParentOrThrow());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class UpdateConstraintVersionVisitor extends JavaIsoVisitor<ExecutionContext> {
        GroupArtifactVersion gav;
        J.MethodInvocation existingConstraint;

        @Nullable
        String because;

        boolean isKotlinDsl;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if ("version".equals(method.getSimpleName())) {
                return method;
            }
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (existingConstraint.isScope(m)) {
                AtomicBoolean updatedBecause = new AtomicBoolean(false);
                m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                    if (arg instanceof J.Literal) {
                        String valueSource = ((J.Literal) arg).getValueSource();
                        char quote;
                        if (valueSource == null) {
                            quote = '\'';
                        } else {
                            quote = valueSource.charAt(0);
                        }
                        return ((J.Literal) arg).withValue(gav.toString())
                                .withValueSource(quote + gav.toString() + quote);
                    } else if (arg instanceof J.Lambda) {
                        arg = (Expression) new RemoveVersionVisitor().visitNonNull(arg, ctx);
                    }
                    if (because != null) {
                        Expression arg2 = (Expression) new UpdateBecauseTextVisitor(because)
                                .visitNonNull(arg, ctx, getCursor());
                        if (arg2 != arg) {
                            updatedBecause.set(true);
                        }
                        return arg2;
                    }
                    return arg;
                }));
                if (because != null && !updatedBecause.get()) {
                    m = (J.MethodInvocation) new CreateBecauseVisitor(because, isKotlinDsl).visitNonNull(m, ctx, requireNonNull(getCursor().getParent()));
                }
            }
            return m;
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class RemoveVersionVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.@Nullable Return visitReturn(J.Return _return, ExecutionContext ctx) {
            J.Return r = super.visitReturn(_return, ctx);
            if (r.getExpression() == null) {
                return null;
            }
            return r;
        }

        @Override
        public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if ("version".equals(m.getSimpleName()) && m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Lambda) {
                return null;
            }
            return m;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class UpdateBecauseTextVisitor extends JavaIsoVisitor<ExecutionContext> {
        String because;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!"because".equals(m.getSimpleName())) {
                return m;
            }
            return m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                if (arg instanceof J.Literal) {
                    char quote;
                    if (((J.Literal) arg).getValueSource() == null) {
                        quote = '"';
                    } else {
                        quote = ((J.Literal) arg).getValueSource().charAt(0);
                    }
                    return ((J.Literal) arg).withValue(because)
                            .withValueSource(quote + because + quote);
                }
                return arg;
            }));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CreateBecauseVisitor extends JavaIsoVisitor<ExecutionContext> {
        String because;
        boolean isKotlinDsl;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            J.Lambda becauseArg;
            if (!isKotlinDsl) {
                becauseArg = parseAsGradle(INDIVIDUAL_CONSTRAINT_BECAUSE_SNIPPET_GROOVY, false, ctx)
                        .map(G.CompilationUnit.class::cast)
                        .map(cu -> (J.MethodInvocation) cu.getStatements().get(1))
                        .map(dependencies -> (J.Lambda) dependencies.getArguments().get(0))
                        .map(dependenciesClosure -> ((J.Block) dependenciesClosure.getBody()).getStatements().get(0))
                        .map(J.Return.class::cast)
                        .map(returnConstraints -> ((J.MethodInvocation) requireNonNull(returnConstraints.getExpression())).getArguments().get(0))
                        .map(J.Lambda.class::cast)
                        .map(constraintsClosure -> ((J.Block) constraintsClosure.getBody()).getStatements().get(0))
                        .map(J.Return.class::cast)
                        .map(returnImplementation -> ((J.MethodInvocation) requireNonNull(returnImplementation.getExpression())).getArguments().get(1))
                        .map(J.Lambda.class::cast)
                        .map(it -> (J.Lambda) new GroovyIsoVisitor<Integer>() {
                            @Override
                            public J.Literal visitLiteral(J.Literal literal, Integer integer) {
                                return literal.withValue(because)
                                        .withValueSource("'" + because + "'");
                            }
                        }.visitNonNull(it, 0))
                        .orElseThrow(() -> new IllegalStateException("Unable to parse because text"));
            } else {
                becauseArg = parseAsGradle(INDIVIDUAL_CONSTRAINT_BECAUSE_SNIPPET_KOTLIN, true, ctx)
                        .map(K.CompilationUnit.class::cast)
                        .map(cu -> (J.Block) cu.getStatements().get(0))
                        .map(block -> (J.MethodInvocation) block.getStatements().get(1))
                        .map(dependencies -> (J.Lambda) dependencies.getArguments().get(0))
                        .map(dependenciesClosure -> ((J.Block) dependenciesClosure.getBody()).getStatements().get(0))
                        .map(J.Return.class::cast)
                        .map(returnConstraints -> ((J.MethodInvocation) requireNonNull(returnConstraints.getExpression())).getArguments().get(0))
                        .map(J.Lambda.class::cast)
                        .map(constraintsClosure -> ((J.Block) constraintsClosure.getBody()).getStatements().get(0))
                        .map(J.Return.class::cast)
                        .map(returnImplementation -> ((J.MethodInvocation) requireNonNull(returnImplementation.getExpression())).getArguments().get(1))
                        .map(J.Lambda.class::cast)
                        .map(it -> (J.Lambda) new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Literal visitLiteral(J.Literal literal, Integer integer) {
                                return literal.withValue(because)
                                        .withValueSource("\"" + because + "\"");
                            }
                        }.visitNonNull(it, 0))
                        .orElseThrow(() -> new IllegalStateException("Unable to parse because text"));
            }
            m = m.withArguments(ListUtils.concat(m.getArguments().subList(0, 1), becauseArg));
            return autoFormat(m, ctx, getCursor().getParentOrThrow());
        }
    }

    private static boolean withinBlock(Cursor cursor, String name) {
        Cursor parentCursor = cursor.getParent();
        while (parentCursor != null) {
            if (parentCursor.getValue() instanceof J.MethodInvocation) {
                J.MethodInvocation m = parentCursor.getValue();
                if (m.getSimpleName().equals(name)) {
                    return true;
                }
            }
            parentCursor = parentCursor.getParent();
        }

        return false;
    }
}
