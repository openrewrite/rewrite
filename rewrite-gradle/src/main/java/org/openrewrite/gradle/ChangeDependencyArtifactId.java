/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.DependencyMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeDependencyArtifactId extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use.",
            example = "jackson-custom")
    String newArtifactId;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change Gradle dependency artifact";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Change the artifact of a specified Gradle dependency.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(DependencyMatcher.build(groupId + ":" + artifactId));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");

            final Map<GroupArtifact, GroupArtifact> updatedDependencies = new HashMap<>();

            @Override
            public G visitCompilationUnit(G.CompilationUnit compilationUnit, ExecutionContext ctx) {
                G.CompilationUnit cu = (G.CompilationUnit) super.visitCompilationUnit(compilationUnit, ctx);
                if(cu != compilationUnit) {
                    cu = cu.withMarkers(cu.getMarkers().withMarkers(ListUtils.map(cu.getMarkers().getMarkers(), m -> {
                        if (m instanceof GradleProject) {
                            return updateModel((GradleProject) m, updatedDependencies);
                        }
                        return m;
                    })));
                }
                return cu;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher();

                if (!((gradleDependencyMatcher.get(getCursor()).isPresent() || dependencyDsl.matches(m)) && (StringUtils.isBlank(configuration) || m.getSimpleName().equals(configuration)))) {
                    return m;
                }

                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry) {
                    m = updateDependency(m);
                } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                           (((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("platform") ||
                            ((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("enforcedPlatform"))) {
                    m = m.withArguments(ListUtils.map(depArgs, platform -> updateDependency((J.MethodInvocation) platform)));
                }

                return m;
            }

            private J.MethodInvocation updateDependency(J.MethodInvocation m) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal) {
                    String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                    if (gav != null) {
                        Dependency dependency = DependencyStringNotationConverter.parse(gav);
                        if (dependency != null && !newArtifactId.equals(dependency.getArtifactId()) &&
                            ((dependency.getVersion() == null && depMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) ||
                             (dependency.getVersion() != null && depMatcher.matches(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())))) {
                            Dependency newDependency = dependency.withArtifactId(newArtifactId);
                            updatedDependencies.put(dependency.getGav().asGroupArtifact(), newDependency.getGav().asGroupArtifact());
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, newDependency.toStringNotation())));
                        }
                    }
                } else if (depArgs.get(0) instanceof G.GString) {
                    List<J> strings = ((G.GString) depArgs.get(0)).getStrings();
                    if (strings.size() >= 2 &&
                        strings.get(0) instanceof J.Literal) {
                        Dependency dependency = DependencyStringNotationConverter.parse((String) requireNonNull(((J.Literal) strings.get(0)).getValue()));
                        if (dependency != null && !newArtifactId.equals(dependency.getArtifactId()) &&
                            depMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                            Dependency newDependency = dependency.withArtifactId(newArtifactId);
                            updatedDependencies.put(dependency.getGav().asGroupArtifact(), newDependency.getGav().asGroupArtifact());
                            String replacement = newDependency.toStringNotation();
                            m = m.withArguments(ListUtils.mapFirst(depArgs, arg -> {
                                G.GString gString = (G.GString) arg;
                                return gString.withStrings(ListUtils.mapFirst(gString.getStrings(), l -> ((J.Literal) l).withValue(replacement).withValueSource(replacement)));
                            }));
                        }
                    }
                } else if (depArgs.get(0) instanceof G.MapEntry) {
                    G.MapEntry artifactEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String version = null;

                    String versionStringDelimiter = "'";
                    for (Expression e : depArgs) {
                        if (!(e instanceof G.MapEntry)) {
                            continue;
                        }
                        G.MapEntry arg = (G.MapEntry) e;
                        if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                            continue;
                        }
                        J.Literal key = (J.Literal) arg.getKey();
                        J.Literal value = (J.Literal) arg.getValue();
                        if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                            continue;
                        }
                        String keyValue = (String) key.getValue();
                        String valueValue = (String) value.getValue();
                        if ("group".equals(keyValue)) {
                            groupId = valueValue;
                        } else if ("name".equals(keyValue) && !newArtifactId.equals(valueValue)) {
                            if (value.getValueSource() != null) {
                                versionStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                            artifactEntry = arg;
                            artifactId = valueValue;
                        } else if ("version".equals(keyValue)) {
                            version = valueValue;
                        }
                    }
                    if (groupId == null || artifactId == null ||
                        (version == null && !depMatcher.matches(groupId, artifactId)) ||
                        (version != null && !depMatcher.matches(groupId, artifactId, version))) {
                        return m;
                    }
                    String delimiter = versionStringDelimiter;
                    G.MapEntry finalArtifact = artifactEntry;
                    m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                        if (arg == finalArtifact) {
                            return finalArtifact.withValue(((J.Literal) finalArtifact.getValue())
                                    .withValue(newArtifactId)
                                    .withValueSource(delimiter + newArtifactId + delimiter));
                        }
                        return arg;
                    }));
                }

                return m;
            }
        });
    }

    private GradleProject updateModel(GradleProject gp, Map<GroupArtifact, GroupArtifact> updatedDependencies) {
        Map<String, GradleDependencyConfiguration> nameToConfigurations = gp.getNameToConfiguration();
        Map<String, GradleDependencyConfiguration> updatedNameToConfigurations = new HashMap<>();
        for (Map.Entry<String, GradleDependencyConfiguration> nameToConfiguration : nameToConfigurations.entrySet()) {
            String configurationName = nameToConfiguration.getKey();
            GradleDependencyConfiguration configuration = nameToConfiguration.getValue();

            List<org.openrewrite.maven.tree.Dependency> newRequested = configuration.getRequested()
                    .stream()
                    .map(requested -> requested.withGav(requested.getGav()
                            .withGroupArtifact(updatedDependencies.getOrDefault(requested.getGav().asGroupArtifact(), requested.getGav().asGroupArtifact()))))
                    .collect(Collectors.toList());

            List<ResolvedDependency> newResolved = configuration.getResolved().stream()
                    .map(resolved ->
                        resolved.withGav(resolved.getGav()
                                .withGroupArtifact(updatedDependencies.getOrDefault(resolved.getGav().asGroupArtifact(), resolved.getGav().asGroupArtifact()))))
                    .collect(Collectors.toList());

            updatedNameToConfigurations.put(configurationName, configuration.withRequested(newRequested).withDirectResolved(newResolved));
        }

        return gp.withNameToConfiguration(updatedNameToConfigurations);
    }
}
