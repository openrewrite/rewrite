/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.graph.DependencyTreeWalker;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Find direct and transitive dependencies, marking first order dependencies that
 * either match or transitively include a dependency matching {@link #groupIdPattern} and
 * {@link #artifactIdPattern}.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class DependencyInsight extends Recipe {
    transient DependenciesInUse dependenciesInUse = new DependenciesInUse(this);

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. All scopes are searched by default.",
            valid = {"compile", "test", "runtime", "provided", "system"},
            example = "compile",
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                    "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used." +
                    "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate()
                .and(Validated.test("scope", "scope is a valid Maven scope", scope,
                        s -> Scope.fromName(s) != Scope.Invalid))
                .and(Validated.test(
                        "coordinates",
                        "groupIdPattern AND artifactIdPattern must not both be generic wildcards",
                        this,
                        r -> !("*".equals(r.groupIdPattern) && "*".equals(artifactIdPattern))
                ));
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Override
    public String getDisplayName() {
        return "Maven dependency insight";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupIdPattern, artifactIdPattern);
    }

    @Override
    public String getDescription() {
        return "Find direct and transitive dependencies matching a group, artifact, and scope. " +
                "Results include dependencies that either directly match or transitively include a matching dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Scope requestedScope = scope == null ? null : Scope.fromName(scope);

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                String projectName = document.getMarkers()
                        .findFirst(JavaProject.class)
                        .map(JavaProject::getProjectName)
                        .orElse("");
                String sourceSetName = document.getMarkers()
                        .findFirst(JavaSourceSet.class)
                        .map(JavaSourceSet::getName)
                        .orElse("main");

                DependencyTreeWalker.Matches<Scope> matches = new DependencyTreeWalker.Matches<>();
                collectMatchingDependencies(projectName, sourceSetName, getResolutionResult(), requestedScope, matches, ctx);

                if (matches.isEmpty()) {
                    return document;
                }

                return (Xml.Document) new MarkIndividualDependency(onlyDirect, matches.byScope(), matches.byDirectDependency()).visitNonNull(document, ctx);
            }

            private void collectMatchingDependencies(
                    String projectName,
                    String sourceSetName,
                    MavenResolutionResult resolutionResult,
                    @Nullable Scope requestedScope,
                    DependencyTreeWalker.Matches<Scope> matches,
                    ExecutionContext ctx
            ) {
                VersionComparator versionComparator = version != null ? Semver.validate(version, null).getValue() : null;
                DependencyMatcher dependencyMatcher = new DependencyMatcher(groupIdPattern, artifactIdPattern, versionComparator);

                if (requestedScope != null) {
                    Set<ResolvedGroupArtifactVersion> gavs = new HashSet<>();
                    for (ResolvedDependency dependency : resolutionResult.getDependencies().get(requestedScope)) {
                        matches.collect(requestedScope, dependency, dependencyMatcher,
                                (matched, path) -> {
                                    if (gavs.add(matched.getGav())) {
                                        createDataTableRow(projectName, sourceSetName, requestedScope, matched.getGav(), path, ctx);
                                    }
                                });
                    }
                } else {
                    for (Map.Entry<Scope, List<ResolvedDependency>> entry : resolutionResult.getDependencies().entrySet()) {
                        Set<ResolvedGroupArtifactVersion> gavs = new HashSet<>();
                        Scope scope = entry.getKey();
                        for (ResolvedDependency dependency : entry.getValue()) {
                            matches.collect(scope, dependency, dependencyMatcher,
                                    (matched, path) -> {
                                        if (gavs.add(matched.getGav())) {
                                            createDataTableRow(projectName, sourceSetName, scope, matched.getGav(), path, ctx);
                                        }
                                    });
                        }
                    }
                }
            }

            private void createDataTableRow(
                    String projectName,
                    String sourceSetName,
                    Scope scope,
                    ResolvedGroupArtifactVersion gav,
                    Deque<ResolvedDependency> dependencyPath,
                    ExecutionContext ctx
            ) {
                String dependencyGraph = DependencyTreeWalker.renderPath(scope.name().toLowerCase(), dependencyPath);
                dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                        projectName,
                        sourceSetName,
                        gav.getGroupId(),
                        gav.getArtifactId(),
                        gav.getVersion(),
                        gav.getDatedSnapshotVersion(),
                        scope.name().toLowerCase(),
                        dependencyPath.size() - 1,
                        dependencyGraph
                ));
            }
        };
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    private static class MarkIndividualDependency extends MavenIsoVisitor<ExecutionContext> {
        @Nullable Boolean onlyDirect;
        Map<Scope, Set<GroupArtifactVersion>> scopeToDirectDependency;
        Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (!isDependencyTag()) {
                return t;
            }

            Scope tagScope = Scope.fromName(tag.getChildValue("scope").orElse("compile"));
            for (Map.Entry<Scope, Set<GroupArtifactVersion>> entry : scopeToDirectDependency.entrySet()) {
                if (tagScope == entry.getKey() || tagScope.isInClasspathOf(entry.getKey())) {
                    return new MavenDependency.Matcher().get(getCursor()).map(dependency -> {
                        ResolvedGroupArtifactVersion gav = dependency.getResolvedDependency().getGav();
                        Optional<GroupArtifactVersion> scopeGav = entry.getValue().stream()
                                .filter(dep -> dep.asGroupArtifact().equals(gav.asGroupArtifact()))
                                .findAny();
                        if (scopeGav.isPresent()) {
                            Set<GroupArtifactVersion> mark = directDependencyToTargetDependency.get(gav.asGroupArtifactVersion());
                            if (mark == null) {
                                return null;
                            }
                            String resultText = mark.stream()
                                    .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
                                    .sorted()
                                    .collect(joining(","));
                            if (!resultText.isEmpty()) {
                                if (Boolean.TRUE.equals(onlyDirect)) {
                                    if (mark.stream().anyMatch(target -> gav.asGroupArtifactVersion().equals(target))) {
                                        return SearchResult.found(t, resultText);
                                    }
                                } else {
                                    return SearchResult.found(t, resultText);
                                }
                            }
                        }
                        return null;
                    }).orElse(t);
                }
            }
            return t;
        }
    }
}
