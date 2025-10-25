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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleDependencyConstraint;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependencies;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.gradle.trait.SpringDependencyManagementPluginEntry;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.*;
import org.openrewrite.trait.Trait;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.gradle.RemoveRedundantDependencyVersions.Comparator.*;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantDependencyVersions extends Recipe {
    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be managed." +
                          "Group is the first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.*",
            required = false)
    @Nullable
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be managed." +
                          "Artifact is the second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Only if managed version is ...",
            description = "Only remove the explicit version if the managed version has the specified comparative relationship to the explicit version. " +
                          "For example, `gte` will only remove the explicit version if the managed version is the same or newer. " +
                          "Default `eq`.",
            valid = {"ANY", "EQ", "LT", "LTE", "GT", "GTE"},
            required = false)
    @Nullable
    Comparator onlyIfManagedVersionIs;

    public enum Comparator {ANY, EQ, LT, LTE, GT, GTE}

    @Override
    public String getDisplayName() {
        return "Remove redundant explicit dependencies and versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency versions that are managed by a Gradle `platform`, `enforcedPlatform` " +
               "or the `io.spring.dependency-management` plugin. Also removes redundant " +
               "direct dependencies and dependency constraints that are already satisfied by transitive dependencies.";
    }

    private static final List<String> DEPENDENCY_MANAGEMENT_METHODS = Arrays.asList(
            "api",
            "implementation",
            "compileOnly",
            "runtimeOnly",
            "testImplementation",
            "testCompileOnly",
            "testRuntimeOnly",
            "debugImplementation",
            "releaseImplementation",
            "androidTestImplementation",
            "featureImplementation",
            "annotationProcessor",
            "kapt",
            "ksp",
            "compile", // deprecated
            "runtime", // deprecated
            "testCompile", // deprecated
            "testRuntime" // deprecated
    );
    private static final VersionComparator VERSION_COMPARATOR = requireNonNull(Semver.validate("latest.release", null).getValue());

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
                    @SuppressWarnings("NotNullFieldNotInitialized")
                    GradleProject gp;
                    final Map<String, List<ResolvedPom>> platforms = new HashMap<>();
                    final Map<String, List<ResolvedDependency>> directDependencies = new HashMap<>();
                    final Map<GroupArtifact, String> springPluginManagedDependencies = new HashMap<>();

                    @Override
                    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (tree instanceof JavaSourceFile) {
                            Optional<GradleProject> maybeGp = tree.getMarkers().findFirst(GradleProject.class);
                            if (!maybeGp.isPresent()) {
                                return (J) tree;
                            }

                            gp = maybeGp.get();

                            if (gp.getPlugins().stream().anyMatch(plugin -> "io.spring.dependency-management".equals(plugin.getId()))) {
                                String springBootVersion = Optional.ofNullable(getSpringBootVersionFromConfiguration("testRuntimeClasspath")).orElseGet(() ->
                                        gp.getNameToConfiguration().keySet().stream()
                                                .map(this::getSpringBootVersionFromConfiguration)
                                                .filter(Objects::nonNull)
                                                .findFirst()
                                                .orElse(null));
                                if (springBootVersion != null) {
                                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                    try {
                                        ResolvedPom platformPom = mpd.download(new GroupArtifactVersion("org.springframework.boot", "spring-boot-dependencies", springBootVersion), null, null, gp.getMavenRepositories())
                                                .resolve(emptyList(), mpd, ctx);
                                        platformPom.getDependencyManagement().stream()
                                                .filter(managedVersion -> managedVersion.getVersion() != null)
                                                .forEach(managedVersion -> springPluginManagedDependencies.put(managedVersion.getGav().asGroupArtifact(), managedVersion.getVersion()));
                                    } catch (MavenDownloadingException ignored) {
                                    }
                                }

                                new JavaIsoVisitor<ExecutionContext>() {
                                    @Override
                                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                        new SpringDependencyManagementPluginEntry.Matcher().get(getCursor()).ifPresent(entry -> entry.getArtifacts().forEach(artifact -> {
                                            if ("mavenBom".equals(method.getSimpleName())) {
                                                MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                                try {
                                                    ResolvedPom platformPom = mpd.download(new GroupArtifactVersion(entry.getGroup(), artifact, entry.getVersion()), null, null, gp.getMavenRepositories())
                                                            .resolve(emptyList(), mpd, ctx);
                                                    platformPom.getDependencyManagement().stream()
                                                            .filter(managedVersion -> managedVersion.getVersion() != null)
                                                            .forEach(managedVersion -> springPluginManagedDependencies.put(managedVersion.getGav().asGroupArtifact(), managedVersion.getVersion()));
                                                } catch (MavenDownloadingException ignored) {
                                                }
                                            } else {
                                                springPluginManagedDependencies.put(new GroupArtifact(entry.getGroup(), artifact), entry.getVersion());
                                            }
                                        }));
                                        return m;
                                    }
                                }.visit(tree, ctx);
                            }

                            MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                            GradleMultiDependency.matcher()
                                    .groupId(groupPattern)
                                    .artifactId(artifactPattern)
                                    .asVisitor(gmd -> gmd.map(gradleDependency -> {
                                        directDependencies.computeIfAbsent(gradleDependency.getConfigurationName(), k -> new ArrayList<>()).add(gradleDependency.getResolvedDependency());
                                        if(!gradleDependency.isPlatform()) {
                                            return gradleDependency.getTree();
                                        }
                                        GroupArtifactVersion gav = gradleDependency.getGav();
                                        try {
                                            ResolvedPom platformPom = mpd.download(gav, null, null, gp.getMavenRepositories())
                                                    .resolve(emptyList(), mpd, ctx);
                                            platforms.computeIfAbsent(gradleDependency.getConfigurationName(), k -> new ArrayList<>()).add(platformPom);
                                        } catch (MavenDownloadingException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return gradleDependency.getTree();
                                    }))
                                    .visit(tree, ctx, getCursor());

                            Set<Statement> statementsToRemove = new HashSet<>();
                            tree = new JavaIsoVisitor<ExecutionContext>() {

                                @Override
                                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                    if ("dependencies".equals(m.getSimpleName())) {
                                        // Gradle tolerates multiple declarations of the same dependency, but only the one with the newest version is used
                                        // Filter out duplicates
                                        Map<GroupArtifactVersion, J.MethodInvocation> requestedToDeclaration = new HashMap<>();
                                        Map<GroupArtifact, List<String>> gaToRequested = new HashMap<>();
                                        new JavaIsoVisitor<ExecutionContext>() {
                                            @Override
                                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                                                J.MethodInvocation m1 = super.visitMethodInvocation(method, executionContext);
                                                new GradleDependency.Matcher().get(getCursor()).ifPresent(it -> {
                                                    if (it.getResolvedDependency().getRequested().getVersion() != null) {
                                                        requestedToDeclaration.put(it.getResolvedDependency().getRequested().getGav(), m1);
                                                        gaToRequested.computeIfAbsent(it.getResolvedDependency().getGav().asGroupArtifact(), (groupArtifact -> new ArrayList<>()))
                                                                .add(it.getResolvedDependency().getRequested().getVersion());
                                                    }
                                                });
                                                return m1;
                                            }
                                        }.visit(m.getArguments().get(0), ctx, getCursor());

                                        for (Map.Entry<GroupArtifact, List<String>> gaToRequestedVersions : gaToRequested.entrySet()) {
                                            GroupArtifact ga = gaToRequestedVersions.getKey();
                                            List<String> requested = gaToRequestedVersions.getValue();
                                            if (requested.size() < 2) {
                                                continue;
                                            }
                                            // The newest version number is relevant, others are redundant and can be removed
                                            requested.stream()
                                                    .sorted(VERSION_COMPARATOR.reversed())
                                                    .skip(1)
                                                    .forEach(redundant -> statementsToRemove.add(requestedToDeclaration.get(new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), redundant))));
                                        }
                                    } else if ("constraints".equals(m.getSimpleName())) {
                                        if (m.getArguments().isEmpty() ||
                                            !(m.getArguments().get(0) instanceof J.Lambda) ||
                                            !(((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block)) {
                                            return m;
                                        }
                                        if (((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().isEmpty()) {
                                            statementsToRemove.add(m);
                                            return m;
                                        }
                                        return m;
                                    } else {
                                        if (!isDependencyManagementMethod(m.getSimpleName()) ||
                                            m.getArguments().isEmpty() ||
                                            m.getArguments().get(0).getType() != JavaType.Primitive.String) {
                                            return m;
                                        }
                                        String value = (String) ((J.Literal) m.getArguments().get(0)).getValue();
                                        Dependency dependency = DependencyNotation.parse(value);
                                        try {
                                            getCursor().dropParentUntil(obj -> obj instanceof J.MethodInvocation && "constraints".equals(((J.MethodInvocation) obj).getSimpleName())).getValue();
                                            if (shouldRemoveRedundantConstraint(dependency, gp.getConfiguration(m.getSimpleName()))) {
                                                //noinspection DataFlowIssue
                                                return null;
                                            }
                                            return m;
                                        } catch (Exception ignore) {
                                        }

                                        try {
                                            if (shouldRemoveRedundantDependency(dependency, m.getSimpleName(), gp.getMavenRepositories(), ctx)) {
                                                statementsToRemove.add(m);
                                                return m;
                                            }
                                        } catch (MavenDownloadingException e) {
                                            return Markup.error(m, e);
                                        }
                                    }
                                    return m;
                                }

                                @Override
                                public J.Return visitReturn(J.Return _return, ExecutionContext ctx) {
                                    J.Return r = super.visitReturn(_return, ctx);
                                    if (r.getExpression() == null) {
                                        //noinspection DataFlowIssue
                                        return null;
                                    }
                                    return r;
                                }

                                private boolean isDependencyManagementMethod(String methodName) {
                                    return DEPENDENCY_MANAGEMENT_METHODS.contains(methodName);
                                }

                                private boolean shouldRemoveRedundantDependency(@Nullable Dependency dependency, String configurationName, List<MavenRepository> repositories, ExecutionContext ctx) throws MavenDownloadingException {
                                    if (dependency == null || !matchesGlob(dependency.getGroupId(), groupPattern) || !matchesGlob(dependency.getArtifactId(), artifactPattern)) {
                                        return false;
                                    }

                                    for (Map.Entry<String, List<ResolvedDependency>> entry : directDependencies.entrySet()) {
                                        for (ResolvedDependency d : entry.getValue()) {
                                            // ignore self
                                            if (d.getGroupId().equals(dependency.getGroupId()) && d.getArtifactId().equals(dependency.getArtifactId())) {
                                                continue;
                                            }

                                            // noinspection ConstantConditions
                                            if (d.getDependencies() == null) {
                                                continue;
                                            }
                                            if (matchesConfiguration(configurationName, entry.getKey()) &&
                                                d.findDependency(dependency.getGroupId(), dependency.getArtifactId()) != null &&
                                                dependsOnNewerVersion(dependency.getGav(), d.getGav().asGroupArtifactVersion(), repositories, ctx)) {
                                                return true;
                                            }
                                        }
                                    }
                                    return false;
                                }

                                private boolean dependsOnNewerVersion(GroupArtifactVersion searchGav, GroupArtifactVersion toSearch, List<MavenRepository> repositories, ExecutionContext ctx) throws MavenDownloadingException {
                                    if (toSearch.getVersion() == null) {
                                        return false;
                                    }
                                    if (searchGav.asGroupArtifact().equals(toSearch.asGroupArtifact())) {
                                        return searchGav.getVersion() == null || matchesComparator(toSearch.getVersion(), searchGav.getVersion());
                                    }
                                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                    try {
                                        List<ResolvedDependency> resolved = mpd.download(toSearch, null, null, repositories)
                                                .resolve(emptyList(), mpd, repositories, ctx)
                                                .resolveDependencies(Scope.Runtime, mpd, ctx);
                                        for (ResolvedDependency r : resolved) {
                                            if (Objects.equals(searchGav.getGroupId(), r.getGroupId()) && Objects.equals(searchGav.getArtifactId(), r.getArtifactId())) {
                                                return searchGav.getVersion() == null || matchesComparator(r.getVersion(), searchGav.getVersion());
                                            }
                                        }
                                    } catch (MavenDownloadingExceptions e) {
                                        throw e.getExceptions().get(0);
                                    }
                                    return false;
                                }

                                boolean matchesConfiguration(String configA, String configB) {
                                    if ("runtimeOnly".equals(configA) && "implementation".equals(configB)) {
                                        return true;
                                    }
                                    if ("testRuntimeOnly".equals(configA)) {
                                        if ("testImplementation".equals(configB) || "implementation".equals(configB)) {
                                            return true;
                                        }
                                    }
                                    return configA.equals(configB);
                                }

                                boolean shouldRemoveRedundantConstraint(@Nullable Dependency constraint, @Nullable GradleDependencyConfiguration c) {
                                    if (c == null || constraint == null || constraint.getVersion() == null) {
                                        return false;
                                    }
                                    if (constraint.getVersion().contains("[") || constraint.getVersion().contains("!!")) {
                                        // https://docs.gradle.org/current/userguide/dependency_versions.html#sec:strict-version
                                        return false;
                                    }
                                    if (!matchesGlob(constraint.getGroupId(), groupPattern) ||
                                        !matchesGlob(constraint.getArtifactId(), artifactPattern)) {
                                        return false;
                                    }

                                    return Stream.concat(
                                                    Stream.of(c),
                                                    gp.configurationsExtendingFrom(c, true).stream()
                                            )
                                            .filter(Objects::nonNull)
                                            .filter(GradleDependencyConfiguration::isCanBeResolved)
                                            .distinct()
                                            .map(conf -> conf.findResolvedDependency(requireNonNull(constraint.getGroupId()), constraint.getArtifactId()))
                                            .filter(Objects::nonNull)
                                            .anyMatch(resolvedDependency -> VERSION_COMPARATOR.compare(null, resolvedDependency.getVersion(), constraint.getVersion()) > 0);
                                }
                            }.visitNonNull(tree, ctx);

                            tree = new JavaVisitor<ExecutionContext>() {
                                @Override
                                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                    J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                                    return new GradleDependencies.Matcher().get(getCursor())
                                            .map(dependencies -> dependencies.filterStatements(s -> !statementsToRemove.contains(s)))
                                            .map(Trait::getTree)
                                            .orElse(m);
                                }
                            }.visitNonNull(tree, ctx);
                        }
                        return super.visit(tree, ctx);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        m = GradleMultiDependency.matcher()
                                .groupId(groupPattern)
                                .artifactId(artifactPattern)
                                .get(getCursor())
                                .map(gmd -> gmd.map(gradleDependency -> {
                                    ResolvedDependency dep = gradleDependency.getResolvedDependency();
                                    if (StringUtils.isBlank(dep.getVersion())) {
                                        return gradleDependency.getTree();
                                    }

                                    if (springPluginManagedDependencies.containsKey(dep.getGav().asGroupArtifact())) {
                                        if (matchesComparator(springPluginManagedDependencies.get(dep.getGav().asGroupArtifact()), dep.getVersion())) {
                                            return gradleDependency.withDeclaredVersion(null).getTree();
                                        }
                                    }

                                    if (platforms.containsKey(gradleDependency.getConfigurationName())) {
                                        for (ResolvedPom platform : platforms.get(gradleDependency.getConfigurationName())) {
                                            String managedVersion = platform.getManagedVersion(dep.getGroupId(), dep.getArtifactId(), null, dep.getRequested().getClassifier());
                                            if (matchesComparator(managedVersion, dep.getVersion())) {
                                                return gradleDependency.withDeclaredVersion(null).getTree();
                                            }
                                        }
                                    }
                                    GradleDependencyConfiguration gdc = gp.getConfiguration(gradleDependency.getConfigurationName());
                                    if (gdc != null) {
                                        for (GradleDependencyConfiguration configuration : gdc.allExtendsFrom()) {
                                            if (platforms.containsKey(configuration.getName())) {
                                                for (ResolvedPom platform : platforms.get(configuration.getName())) {
                                                    String managedVersion = platform.getManagedVersion(dep.getGroupId(), dep.getArtifactId(), null, dep.getRequested().getClassifier());
                                                    if (matchesComparator(managedVersion, dep.getVersion())) {
                                                        return gradleDependency.withDeclaredVersion(null).getTree();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    return gradleDependency.getTree();
                                })).orElse(m);
                        return m;
                    }

                    private @Nullable String getSpringBootVersionFromConfiguration(String configuration) {
                        GradleDependencyConfiguration testRuntimeConfiguration = gp.getConfiguration(configuration);
                        if (testRuntimeConfiguration != null) {
                            for (GradleDependencyConstraint constraint : testRuntimeConfiguration.getConstraints()) {
                                if ("org.springframework.boot".equals(constraint.getGroupId()) && constraint.getStrictVersion() != null) {
                                    return constraint.getStrictVersion();
                                }
                            }
                            for (ResolvedDependency dependency : testRuntimeConfiguration.getDirectResolved()) {
                                if (dependency.getRequested().getVersion() == null && "org.springframework.boot".equals(dependency.getGroupId())) {
                                    return dependency.getVersion();
                                }
                            }
                        }
                        return null;
                    }
                }
        );
    }

    private Comparator determineComparator() {
        if (onlyIfManagedVersionIs != null) {
            return onlyIfManagedVersionIs;
        }
        return EQ;
    }

    private boolean matchesComparator(@Nullable String managedVersion, String requestedVersion) {
        Comparator comparator = determineComparator();
        if (managedVersion == null) {
            return false;
        }
        if (comparator == ANY) {
            return true;
        }
        if (!isExact(managedVersion)) {
            return false;
        }
        int comparison = new LatestIntegration(null).compare(null, managedVersion, requestedVersion);
        if (comparison < 0) {
            return comparator == LT || comparator == LTE;
        } else if (comparison > 0) {
            return comparator == GT || comparator == GTE;
        } else {
            return comparator == EQ || comparator == LTE || comparator == GTE;
        }
    }

    private boolean isExact(String managedVersion) {
        Validated<VersionComparator> maybeVersionComparator = Semver.validate(managedVersion, null);
        return maybeVersionComparator.isValid() && maybeVersionComparator.getValue() instanceof ExactVersion;
    }
}
