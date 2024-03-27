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
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.format.BlankLinesVisitor;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Preconditions.not;

@Incubating(since = "8.18.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeTransitiveDependencyVersion extends Recipe {
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

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker), new GroovyVisitor<ExecutionContext>() {
            GradleProject gradleProject;

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                gradleProject = cu.getMarkers().findFirst(GradleProject.class)
                        .orElseThrow(() -> new IllegalStateException("Unable to find GradleProject marker."));

                Map<GroupArtifact, Map<GradleDependencyConfiguration, String>> toUpdate = new HashMap<>();

                DependencyVersionSelector versionSelector = new DependencyVersionSelector(metadataFailures, gradleProject, null);
                for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
                    for (ResolvedDependency resolved : configuration.getResolved()) {
                        if (resolved.getDepth() > 0 && dependencyMatcher.matches(resolved.getGroupId(),
                                resolved.getArtifactId(), resolved.getVersion())) {
                            try {
                                String selected = versionSelector.select(resolved.getGav(), configuration.getName(),
                                        version, versionPattern, ctx);
                                if (selected == null || resolved.getVersion().equals(selected)) {
                                    continue;
                                }

                                GradleDependencyConfiguration constraintConfig = constraintConfiguration(configuration);
                                if (constraintConfig == null) {
                                    continue;
                                }

                                toUpdate.merge(
                                        new GroupArtifact(resolved.getGroupId(), resolved.getArtifactId()),
                                        singletonMap(constraintConfig, selected),
                                        (existing, update) -> {
                                            Map<GradleDependencyConfiguration, String> all = new HashMap<>(existing);
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
                                return Markup.warn(cu, e);
                            }
                        }
                    }
                }

                if (!toUpdate.isEmpty()) {
                    cu = (G.CompilationUnit) Preconditions.check(not(new UsesMethod<>(CONSTRAINTS_MATCHER)),
                            new AddConstraintsBlock()).visitNonNull(cu, ctx);

                    for (Map.Entry<GroupArtifact, Map<GradleDependencyConfiguration, String>> update : toUpdate.entrySet()) {
                        Map<GradleDependencyConfiguration, String> configs = update.getValue();
                        for (Map.Entry<GradleDependencyConfiguration, String> config : configs.entrySet()) {
                            cu = (G.CompilationUnit) new AddConstraint(config.getKey().getName(), new GroupArtifactVersion(update.getKey().getGroupId(),
                                    update.getKey().getArtifactId(), config.getValue()), because).visitNonNull(cu, ctx);
                        }
                    }
                }

                return cu;
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
            @Nullable
            private GradleDependencyConfiguration constraintConfiguration(GradleDependencyConfiguration config) {
                String constraintConfigName = config.getName();
                switch (constraintConfigName) {
                    case "compileClasspath":
                    case "compileOnly":
                    case "compile":
                        constraintConfigName = "implementation";
                        break;
                    case "runtimeClasspath":
                    case "runtime":
                        constraintConfigName = "runtimeOnly";
                        break;
                    case "testCompileClasspath":
                    case "testCompile":
                        constraintConfigName = "testImplementation";
                        break;
                    case "testRuntimeClasspath":
                    case "testRuntime":
                        constraintConfigName = "testRuntimeOnly";
                        break;
                }

                GradleDependencyConfiguration configuration = gradleProject.getConfiguration(constraintConfigName);
                if (configuration != null && configuration.isTransitive()) {
                    return configuration;
                }
                return null;
            }
        });
    }

    private static class AddConstraintsBlock extends GroovyIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (DEPENDENCIES_DSL_MATCHER.matches(method)) {
                J withConstraints = (J) GradleParser.builder().build().parse(
                        //language=groovy
                        "plugins { id 'java' }\n" +
                        "dependencies {\n" +
                        "    constraints {\n" +
                        "    }\n" +
                        "}\n"
                ).findFirst().orElseThrow(() -> new IllegalStateException("Unable to parse constraints block"));

                Statement constraints = FindMethods.find(withConstraints, "org.gradle.api.artifacts.dsl.DependencyHandler constraints(..)", true)
                        .stream()
                        .filter(J.MethodInvocation.class::isInstance)
                        .map(J.MethodInvocation.class::cast)
                        .filter(m2 -> m2.getSimpleName().equals("constraints"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to find constraints block"))
                        .withMarkers(Markers.EMPTY);

                return autoFormat(m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                    if(!(arg instanceof J.Lambda)) {
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
            }

            return m;
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class AddConstraint extends GroovyIsoVisitor<ExecutionContext> {
        String config;
        GroupArtifactVersion gav;

        @Nullable
        String because;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if(!CONSTRAINTS_MATCHER.matches(m)) {
                return m;
            }
            String ga = gav.getGroupId() + ":" + gav.getArtifactId() + ":";
            AtomicReference<String> existingConstraintVersion = new AtomicReference<>();
            J.MethodInvocation existingConstraint = FindMethods.find(m, CONSTRAINT_MATCHER, true).stream()
                    .filter(J.MethodInvocation.class::isInstance)
                    .map(J.MethodInvocation.class::cast)
                    .filter(c -> c.getSimpleName().equals(config) && c.getArguments().stream()
                            .anyMatch(arg -> {
                                        if (!(arg instanceof J.Literal) || ((J.Literal) arg).getValue() == null) {
                                            return false;
                                        }
                                        String value = ((J.Literal) arg).getValue().toString();
                                        if (!value.startsWith(ga)) {
                                            return false;
                                        }
                                        existingConstraintVersion.set(value.substring(value.lastIndexOf(':') + 1));
                                        return true;
                                    }
                            )
                    ).findFirst()
                    .orElse(null);
            if (Objects.equals(gav.getVersion(), existingConstraintVersion.get())) {
                return m;
            }
            if(existingConstraint == null) {
                m = (J.MethodInvocation) new CreateConstraintVisitor(config, gav, because)
                        .visitNonNull(m, ctx, requireNonNull(getCursor().getParent()));
            } else {
                m = (J.MethodInvocation) new UpdateConstraintVersionVisitor(gav, existingConstraint, because)
                        .visitNonNull(m, ctx, requireNonNull(getCursor().getParent()));
            }
            return m;
        }
    }
    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CreateConstraintVisitor extends GroovyIsoVisitor<ExecutionContext> {

        String config;
        GroupArtifactVersion gav;
        @Nullable
        String because;
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            J withConstraint = (J) GradleParser.builder().build().parse(String.format(
                    "plugin { id 'java' }\n" +
                    "dependencies { constraints {\n" +
                    "    %s('%s')%s\n" +
                    "}}",
                    config,
                    gav,
                    because == null ? "" : String.format(" {\n   because '%s'\n}", because)
            )).findFirst().orElseThrow(() -> new IllegalStateException("Unable to parse constraint"));

            Statement constraint = FindMethods.find(withConstraint, CONSTRAINT_MATCHER, true)
                    .stream()
                    .filter(J.MethodInvocation.class::isInstance)
                    .map(J.MethodInvocation.class::cast)
                    .filter(m2 -> m2.getSimpleName().equals(config))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to find constraint"))
                    .withMarkers(Markers.EMPTY);

            m = autoFormat(m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                if(!(arg instanceof J.Lambda)) {
                    return arg;
                }
                J.Lambda dependencies = (J.Lambda) arg;
                if (!(dependencies.getBody() instanceof J.Block)) {
                    return arg;
                }
                J.Block body = (J.Block) dependencies.getBody();

                return dependencies.withBody(body.withStatements(
                        ListUtils.concat(constraint, body.getStatements())));
            })), constraint, ctx, getCursor().getParentOrThrow());
            return m;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class UpdateConstraintVersionVisitor extends GroovyIsoVisitor<ExecutionContext> {
        GroupArtifactVersion gav;
        J.MethodInvocation existingConstraint;
        @Nullable
        String because;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if(existingConstraint.isScope(m)) {
                AtomicBoolean updated = new AtomicBoolean(false);
                m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                    if(arg instanceof J.Literal) {
                        char quote;
                        if(((J.Literal) arg).getValueSource() == null) {
                            quote = '\'';
                        } else {
                            quote = ((J.Literal) arg).getValueSource().charAt(0);
                        }
                        return ((J.Literal) arg).withValue(gav.toString())
                                .withValueSource(quote + gav.toString() + quote);
                    }
                    if(because != null) {
                        Expression arg2 = (Expression) new UpdateBecauseTextVisitor(because)
                                .visitNonNull(arg, ctx, getCursor());
                        if(arg2 != arg) {
                            updated.set(true);
                        }
                        return arg2;
                    }
                    return arg;
                }));
                if(because != null && !updated.get()) {
                    m = (J.MethodInvocation) new CreateBecauseVisitor(because).visitNonNull(m, ctx, requireNonNull(getCursor().getParent()));
                }
            }
            return m;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class UpdateBecauseTextVisitor extends GroovyIsoVisitor<ExecutionContext> {
        String because;
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if(!"because".equals(m.getSimpleName())) {
                return m;
            }
            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                if(arg instanceof J.Literal) {
                    char quote;
                    if(((J.Literal) arg).getValueSource() == null) {
                        quote = '"';
                    } else {
                        quote = ((J.Literal) arg).getValueSource().charAt(0);
                    }
                    return ((J.Literal) arg).withValue(because)
                            .withValueSource(quote + because + quote);
                }
                return arg;
            }));
            return m;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CreateBecauseVisitor extends GroovyIsoVisitor<ExecutionContext> {
        String because;
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            J.Lambda becauseArg = GradleParser.builder().build().parse(String.format(
                    "plugin { id 'java' }\n" +
                    "dependencies { constraints {\n" +
                    "    implementation('org.openrewrite:rewrite-core:8.0.0') {\n" +
                    "        because '%s'\n" +
                    "    }\n" +
                    "}}",
                    because))
                    .map(G.CompilationUnit.class::cast)
                    .map(cu -> (J.MethodInvocation) cu.getStatements().get(1))
                    .map(J.MethodInvocation.class::cast)
                    .map(dependencies -> (J.Lambda) dependencies.getArguments().get(0))
                    .map(dependenciesClosure -> ((J.Block)dependenciesClosure.getBody()).getStatements().get(0))
                    .map(J.Return.class::cast)
                    .map(returnConstraints -> ((J.MethodInvocation) requireNonNull(returnConstraints.getExpression())).getArguments().get(0))
                    .map(J.Lambda.class::cast)
                    .map(constraintsClosure -> ((J.Block)constraintsClosure.getBody()).getStatements().get(0))
                    .map(J.Return.class::cast)
                    .map(returnImplementation -> ((J.MethodInvocation) requireNonNull(returnImplementation.getExpression())).getArguments().get(1))
                    .map(J.Lambda.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to parse because text"));
            m = m.withArguments(ListUtils.concat(m.getArguments().subList(0, 1), becauseArg));
            m = autoFormat(m, ctx, getCursor().getParentOrThrow());
            return m;
        }
    }
}
