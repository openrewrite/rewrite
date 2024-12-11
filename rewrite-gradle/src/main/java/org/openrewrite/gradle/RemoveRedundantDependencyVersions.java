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
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
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
            valid = {"any", "eq", "lt", "lte", "gt", "gte"},
            required = false)
    @Nullable
    Comparator onlyIfManagedVersionIs;

    @Option(displayName = "Except",
            description = "Accepts a list of GAVs. Dependencies matching a GAV will be ignored by this recipe."
                    + " GAV versions are ignored if provided.",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> except;

    public enum Comparator {
        ANY,
        EQ,
        LT,
        LTE,
        GT,
        GTE
    }

    @Override
    public String getDisplayName() {
        return "Remove redundant explicit dependency versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency versions that are managed by a Gradle `platform`/`enforcedPlatform`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new IsBuildGradle<>(),
                new GroovyIsoVisitor<ExecutionContext>() {
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

                                if (m.getArguments().get(0) instanceof J.Literal) {
                                    J.Literal l = (J.Literal) m.getArguments().get(0);
                                    if (l.getType() != JavaType.Primitive.String) {
                                        return m;
                                    }

                                    Dependency dependency = DependencyStringNotationConverter.parse((String) l.getValue());
                                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                    try {
                                        ResolvedPom platformPom = mpd.download(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()), null, null, gp.getMavenRepositories())
                                                .resolve(Collections.emptyList(), mpd, ctx);
                                        platforms.computeIfAbsent(getCursor().getParent(1).firstEnclosing(J.MethodInvocation.class).getSimpleName(), k -> new ArrayList<>()).add(platformPom);
                                    } catch (MavenDownloadingException e) {
                                        return m;
                                    }
                                } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                                    String groupId = null;
                                    String artifactId = null;
                                    String version = null;

                                    for (Expression arg : m.getArguments()) {
                                        if (!(arg instanceof G.MapEntry)) {
                                            continue;
                                        }

                                        G.MapEntry entry = (G.MapEntry) arg;
                                        if (!(entry.getKey() instanceof J.Literal) || !(entry.getValue() instanceof J.Literal)) {
                                            continue;
                                        }

                                        J.Literal key = (J.Literal) entry.getKey();
                                        J.Literal value = (J.Literal) entry.getValue();
                                        if (key.getType() != JavaType.Primitive.String || value.getType() != JavaType.Primitive.String) {
                                            continue;
                                        }

                                        switch ((String) key.getValue()) {
                                            case "group":
                                                groupId = (String) value.getValue();
                                                break;
                                            case "name":
                                                artifactId = (String) value.getValue();
                                                break;
                                            case "version":
                                                version = (String) value.getValue();
                                                break;
                                        }
                                    }

                                    if (groupId == null || artifactId == null || version == null) {
                                        return m;
                                    }

                                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                    try {
                                        ResolvedPom platformPom = mpd.download(new GroupArtifactVersion(groupId, artifactId, version), null, null, gp.getMavenRepositories())
                                                .resolve(Collections.emptyList(), mpd, ctx);
                                        platforms.computeIfAbsent(getCursor().getParent(1).firstEnclosing(J.MethodInvocation.class).getSimpleName(), k -> new ArrayList<>()).add(platformPom);
                                    } catch (MavenDownloadingException e) {
                                        return m;
                                    }
                                }
                                return m;
                            }
                        }.visit(cu, ctx);

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
                        ResolvedDependency d = gradleDependency.getResolvedDependency();
                        if (StringUtils.isBlank(d.getVersion())) {
                            return m;
                        }

                        if (platforms.containsKey(m.getSimpleName())) {
                            for (ResolvedPom platform : platforms.get(m.getSimpleName())) {
                                String managedVersion = platform.getManagedVersion(d.getGroupId(), d.getArtifactId(), null, d.getRequested().getClassifier());
                                if (matchesComparator(managedVersion, d.getVersion())) {
                                    return maybeRemoveVersion(m);
                                }
                            }
                        }
                        GradleDependencyConfiguration gdc = gp.getConfiguration(m.getSimpleName());
                        if (gdc != null) {
                            for (GradleDependencyConfiguration configuration : gdc.allExtendsFrom()) {
                                if (platforms.containsKey(configuration.getName())) {
                                    for (ResolvedPom platform : platforms.get(configuration.getName())) {
                                        String managedVersion = platform.getManagedVersion(d.getGroupId(), d.getArtifactId(), null, d.getRequested().getClassifier());
                                        if (matchesComparator(managedVersion, d.getVersion())) {
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
                            if (l.getType() != JavaType.Primitive.String) {
                                return m;
                            }

                            Dependency dep = DependencyStringNotationConverter.parse((String) l.getValue())
                                    .withVersion(null);
                            if (dep.getClassifier() != null || dep.getExt() != null) {
                                return m;
                            }

                            return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue(l, dep.toStringNotation())));
                        } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
                            return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                                return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), entry -> {
                                    if (entry.getKey() instanceof J.Literal &&
                                            "version".equals(((J.Literal) entry.getKey()).getValue())) {
                                        return null;
                                    }
                                    return entry;
                                }));
                            }));
                        } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                            return m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                G.MapEntry entry = (G.MapEntry) arg;
                                if (entry.getKey() instanceof J.Literal &&
                                        "version".equals(((J.Literal) entry.getKey()).getValue())) {
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
        return Comparator.EQ;
    }

    private boolean matchesComparator(@Nullable String managedVersion, String requestedVersion) {
        Comparator comparator = determineComparator();
        if (managedVersion == null) {
            return false;
        }
        if (comparator.equals(Comparator.ANY)) {
            return true;
        }
        if (!isExact(managedVersion)) {
            return false;
        }
        int comparison = new LatestIntegration(null)
                .compare(null, managedVersion, requestedVersion);
        if (comparison < 0) {
            return comparator.equals(Comparator.LT) || comparator.equals(Comparator.LTE);
        } else if (comparison > 0) {
            return comparator.equals(Comparator.GT) || comparator.equals(Comparator.GTE);
        } else {
            return comparator.equals(Comparator.EQ) || comparator.equals(Comparator.LTE) || comparator.equals(Comparator.GTE);
        }
    }

    private boolean isExact(String managedVersion) {
        Validated<VersionComparator> maybeVersionComparator = Semver.validate(managedVersion, null);
        return maybeVersionComparator.isValid() && maybeVersionComparator.getValue() instanceof ExactVersion;
    }
}
