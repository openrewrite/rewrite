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
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.internal.Dependency;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestIntegration;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.gradle.RemoveRedundantDependencyVersions.Comparator.*;

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

    @Option(displayName = "Except",
            description = "Accepts a list of GAVs. Dependencies matching a GAV will be ignored by this recipe." +
                    " GAV versions are ignored if provided.",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> except;

    public enum Comparator { ANY, EQ, LT, LTE, GT, GTE }

    @Override
    public String getDisplayName() {
        return "Remove redundant explicit dependency versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency versions that are managed by a Gradle `platform`/`enforcedPlatform`.";
    }

    private static final MethodMatcher CONSTRAINTS_MATCHER = new MethodMatcher("DependencyHandlerSpec constraints(..)");
    private static final MethodMatcher INDIVIDUAL_CONSTRAINTS_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");
    private static final VersionComparator VERSION_COMPARATOR = requireNonNull(Semver.validate("latest.release", null).getValue());

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
                    GradleProject gp;
                    final Map<String, List<ResolvedPom>> platforms = new HashMap<>();

                    @Override
                    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                        Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                        if (!maybeGp.isPresent()) {
                            return cu;
                        }

                        gp = maybeGp.get();
                        new GroovyIsoVisitor<ExecutionContext>() {
                            final MethodMatcher platformMatcher = new MethodMatcher("org.gradle.api.artifacts.dsl.DependencyHandler platform(..)");
                            final MethodMatcher enforcedPlatformMatcher = new MethodMatcher("org.gradle.api.artifacts.dsl.DependencyHandler enforcedPlatform(..)");

                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                if (!platformMatcher.matches(m) && !enforcedPlatformMatcher.matches(m)) {
                                    return m;
                                }

                                GroupArtifactVersion gav = null;
                                if (m.getArguments().get(0) instanceof J.Literal) {
                                    J.Literal l = (J.Literal) m.getArguments().get(0);
                                    if (l.getType() == JavaType.Primitive.String) {
                                        Dependency dependency = DependencyStringNotationConverter.parse((String) l.getValue());
                                        gav = new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                                    }
                                } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                                    String groupId = null;
                                    String artifactId = null;
                                    String version = null;

                                    for (Expression arg : m.getArguments()) {
                                        if (!(arg instanceof G.MapEntry &&
                                                ((G.MapEntry) arg).getKey().getType() == JavaType.Primitive.String &&
                                                ((G.MapEntry) arg).getValue().getType() == JavaType.Primitive.String)) {
                                            continue;
                                        }

                                        Object key = ((J.Literal) ((G.MapEntry) arg).getKey()).getValue();
                                        if ("group".equals(key)) {
                                            groupId = (String) ((J.Literal) ((G.MapEntry) arg).getValue()).getValue();
                                        } else if ("name".equals(key)) {
                                            artifactId = (String) ((J.Literal) ((G.MapEntry) arg).getValue()).getValue();
                                        } else if ("version".equals(key)) {
                                            version = (String) ((J.Literal) ((G.MapEntry) arg).getValue()).getValue();
                                        }
                                    }

                                    if (groupId != null && artifactId != null && version != null) {
                                        gav = new GroupArtifactVersion(groupId, artifactId, version);
                                    }
                                }
                                if (gav != null) {
                                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                    try {
                                        ResolvedPom platformPom = mpd.download(gav, null, null, gp.getMavenRepositories())
                                                .resolve(emptyList(), mpd, ctx);
                                        platforms.computeIfAbsent(getCursor().getParentOrThrow(1).firstEnclosingOrThrow(J.MethodInvocation.class).getSimpleName(), k -> new ArrayList<>()).add(platformPom);
                                    } catch (MavenDownloadingException ignored) {
                                    }
                                }
                                return m;
                            }
                        }.visit(cu, ctx);
                        cu = (G.CompilationUnit) new GroovyIsoVisitor<ExecutionContext>() {

                            @Override
                            public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                if (CONSTRAINTS_MATCHER.matches(m)) {
                                    if (m.getArguments().isEmpty() ||
                                            !(m.getArguments().get(0) instanceof J.Lambda) ||
                                            !(((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block)) {
                                        return m;
                                    }
                                    if (((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().isEmpty()) {
                                        return null;
                                    }
                                    return m;
                                } else if (INDIVIDUAL_CONSTRAINTS_MATCHER.matches(m)) {
                                    if (!TypeUtils.isAssignableTo("org.gradle.api.artifacts.Dependency", requireNonNull(m.getMethodType()).getReturnType()) ||
                                            m.getArguments().isEmpty() ||
                                            m.getArguments().get(0).getType() != JavaType.Primitive.String) {
                                        return m;
                                    }
                                    //noinspection DataFlowIssue
                                    if (shouldRemoveRedundantConstraint((String) ((J.Literal) m.getArguments().get(0)).getValue(), m.getSimpleName())) {
                                        return null;
                                    }
                                }
                                return m;
                            }

                            @Override
                            public J.@Nullable Return visitReturn(J.Return _return, ExecutionContext ctx) {
                                J.Return r = super.visitReturn(_return, ctx);
                                if (r.getExpression() == null) {
                                    return null;
                                }
                                return r;
                            }

                            boolean shouldRemoveRedundantConstraint(String dependencyNotation, String configurationName) {
                                return shouldRemoveRedundantConstraint(
                                        DependencyStringNotationConverter.parse(dependencyNotation),
                                        gp.getConfiguration(configurationName));
                            }

                            boolean shouldRemoveRedundantConstraint(@Nullable Dependency constraint, @Nullable GradleDependencyConfiguration c) {
                                if (c == null || constraint == null || constraint.getVersion() == null) {
                                    return false;
                                }
                                if (constraint.getVersion().contains("[") || constraint.getVersion().contains("!!")) {
                                    // https://docs.gradle.org/current/userguide/dependency_versions.html#sec:strict-version
                                    return false;
                                }
                                if ((groupPattern != null && !StringUtils.matchesGlob(constraint.getGroupId(), groupPattern)) ||
                                        (artifactPattern != null && !StringUtils.matchesGlob(constraint.getArtifactId(), artifactPattern))) {
                                    return false;
                                }
                                return Stream.concat(
                                                Stream.of(c),
                                                gp.configurationsExtendingFrom(c, true).stream()
                                        )
                                        .filter(GradleDependencyConfiguration::isCanBeResolved)
                                        .distinct()
                                        .map(conf -> conf.findResolvedDependency(requireNonNull(constraint.getGroupId()), constraint.getArtifactId()))
                                        .filter(Objects::nonNull)
                                        .anyMatch(resolvedDependency -> VERSION_COMPARATOR.compare(null, resolvedDependency.getVersion(), constraint.getVersion()) > 0);
                            }
                        }.visitNonNull(cu, ctx);

                        return super.visitCompilationUnit(cu, ctx);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                        Optional<GradleDependency> maybeGradleDependency = new GradleDependency.Matcher()
                                .groupId(groupPattern)
                                .artifactId(artifactPattern)
                                .get(getCursor());
                        if (!maybeGradleDependency.isPresent()) {
                            return m;
                        }

                        GradleDependency gradleDependency = maybeGradleDependency.get();
                        ResolvedDependency dep = gradleDependency.getResolvedDependency();
                        if (StringUtils.isBlank(dep.getVersion())) {
                            return m;
                        }

                        if (platforms.containsKey(m.getSimpleName())) {
                            for (ResolvedPom platform : platforms.get(m.getSimpleName())) {
                                String managedVersion = platform.getManagedVersion(dep.getGroupId(), dep.getArtifactId(), null, dep.getRequested().getClassifier());
                                if (matchesComparator(managedVersion, dep.getVersion())) {
                                    return maybeRemoveVersion(m);
                                }
                            }
                        }
                        GradleDependencyConfiguration gdc = gp.getConfiguration(m.getSimpleName());
                        if (gdc != null) {
                            for (GradleDependencyConfiguration configuration : gdc.allExtendsFrom()) {
                                if (platforms.containsKey(configuration.getName())) {
                                    for (ResolvedPom platform : platforms.get(configuration.getName())) {
                                        String managedVersion = platform.getManagedVersion(dep.getGroupId(), dep.getArtifactId(), null, dep.getRequested().getClassifier());
                                        if (matchesComparator(managedVersion, dep.getVersion())) {
                                            return maybeRemoveVersion(m);
                                        }
                                    }
                                }
                            }
                        }

                        return m;
                    }

                    private J.MethodInvocation maybeRemoveVersion(J.MethodInvocation m) {
                        if (m.getArguments().get(0) instanceof J.Literal) {
                            J.Literal l = (J.Literal) m.getArguments().get(0);
                            if (l.getType() == JavaType.Primitive.String) {
                                Dependency dep = DependencyStringNotationConverter.parse((String) l.getValue());
                                if (dep == null || dep.getClassifier() != null || dep.getExt() != null) {
                                    return m;
                                }
                                return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                                        ChangeStringLiteral.withStringValue(l, dep.withVersion(null).toStringNotation()))
                                );
                            }
                        } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
                            return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                                return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), entry -> {
                                    if (entry.getKey() instanceof J.Literal && "version".equals(((J.Literal) entry.getKey()).getValue())) {
                                        return null;
                                    }
                                    return entry;
                                }));
                            }));
                        } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                            return m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                G.MapEntry entry = (G.MapEntry) arg;
                                if (entry.getKey() instanceof J.Literal && "version".equals(((J.Literal) entry.getKey()).getValue())) {
                                    return null;
                                }
                                return entry;
                            }));
                        }
                        return m;
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
