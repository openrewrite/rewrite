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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class DependencyInsight extends Recipe {
    transient DependenciesInUse dependenciesInUse = new DependenciesInUse(this);

    private static final MethodMatcher DEPENDENCY_CONFIGURATION_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. If not specified, all configurations will be searched.",
            example = "compileClasspath",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Gradle dependency insight";
    }

    @Override
    public String getDescription() {
        return "Find direct and transitive dependencies matching a group, artifact, and optionally a configuration name. " +
               "Results include dependencies that either directly match or transitively include a matching dependency.";
    }

    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {

            Map<String, List<ResolvedDependency>> configToMatchingDependencies;

            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile compilationUnit, ExecutionContext executionContext) {
                GradleProject gp = compilationUnit.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gp == null) {
                    return compilationUnit;
                }

                configToMatchingDependencies = gp.getConfigurations().stream()
                        .filter(c -> configuration == null || c.getName().equals(configuration))
                        .collect(toMap(
                                GradleDependencyConfiguration::getName,
                                c -> c.getResolved().stream()
                                        .filter(resolvedDependency ->
                                                resolvedDependency.findDependency(groupIdPattern, artifactIdPattern) != null
                                        ).collect(toList())));

                if (configToMatchingDependencies.isEmpty()) {
                    return compilationUnit;
                }

                return super.visitJavaSourceFile(compilationUnit, executionContext);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!DEPENDENCY_CONFIGURATION_MATCHER.matches(m) || !configToMatchingDependencies.containsKey(m.getSimpleName()) ||
                    m.getArguments().isEmpty()) {
                    return m;
                }

                Expression arg = m.getArguments().get(0);
                if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                    String[] gav = ((String) ((J.Literal) arg).getValue()).split(":");
                    if (gav.length < 2) {
                        return m;
                    }
                    String groupId = gav[0];
                    String artifactId = gav[1];
                    //noinspection DuplicatedCode
                    Optional<ResolvedDependency> maybeMatch = configToMatchingDependencies.get(m.getSimpleName()).stream()
                            .filter(dep -> Objects.equals(dep.getGroupId(), groupId) && Objects.equals(dep.getArtifactId(), artifactId))
                            .findAny();
                    if (!maybeMatch.isPresent()) {
                        return m;
                    }
                    ResolvedDependency match = maybeMatch.get().findDependency(groupIdPattern, artifactIdPattern);
                    if (match != null) {
                        return SearchResult.found(m, match.getGroupId() + ":" + match.getArtifactId() + ":" + match.getVersion());
                    }
                } else if (arg instanceof G.MapEntry) {
                    String groupId = null;
                    String artifactId = null;
                    for (Expression argExp : m.getArguments()) {
                        if (!(argExp instanceof G.MapEntry)) {
                            continue;
                        }
                        G.MapEntry gavPart = (G.MapEntry) argExp;
                        if (!(gavPart.getKey() instanceof J.Literal)) {
                            continue;
                        }
                        String key = (String) ((J.Literal) gavPart.getKey()).getValue();
                        if ("group".equals(key)) {
                            groupId = (String) ((J.Literal) gavPart.getValue()).getValue();
                        } else if ("name".equals(key)) {
                            artifactId = (String) ((J.Literal) gavPart.getValue()).getValue();
                        }
                        if (groupId != null && artifactId != null) {
                            break;
                        }
                    }

                    String finalGroupId = groupId;
                    String finalArtifactId = artifactId;
                    //noinspection DuplicatedCode
                    Optional<ResolvedDependency> maybeMatch = configToMatchingDependencies.get(m.getSimpleName()).stream()
                            .filter(dep -> Objects.equals(dep.getGroupId(), finalGroupId) && Objects.equals(dep.getArtifactId(), finalArtifactId))
                            .findAny();
                    if (!maybeMatch.isPresent()) {
                        return m;
                    }

                    ResolvedDependency match = maybeMatch.get().findDependency(groupIdPattern, artifactIdPattern);
                    if (match != null) {
                        Optional<JavaProject> javaProject = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                                .findFirst(JavaProject.class);
                        Optional<JavaSourceSet> javaSourceSet = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                                .findFirst(JavaSourceSet.class);

                        dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                                javaProject.map(JavaProject::getProjectName).orElse(""),
                                javaSourceSet.map(JavaSourceSet::getName).orElse("main"),
                                match.getGroupId(),
                                match.getArtifactId(),
                                match.getVersion(),
                                match.getDatedSnapshotVersion(),
                                StringUtils.isBlank(match.getRequested().getScope()) ? "compile" :
                                        match.getRequested().getScope(),
                                match.getDepth()
                        ));

                        return SearchResult.found(m, match.getGroupId() + ":" + match.getArtifactId() + ":" + match.getVersion());
                    }
                }
                return m;
            }
        };
    }
}
