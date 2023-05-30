/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeDependency extends Recipe {
    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "rewrite-testing-frameworks",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Change Gradle dependency";
    }

    @Override
    public String getDescription() {
        return "Change a Gradle dependency coordinates.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");

            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                if (g != cu) {
                    GradleProject gp = g.getMarkers().findFirst(GradleProject.class)
                            .orElseThrow(() -> new IllegalArgumentException("Gradle files are expected to have a GradleProject marker."));
                    g = g.withMarkers(g.getMarkers().setByType(updateGradleModel(gp)));
                }
                return g;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!dependencyDsl.matches(m)) {
                    return m;
                }

                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry) {
                    m = updateDependency(m, ctx);
                } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                        (((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("platform") ||
                                ((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("enforcedPlatform"))) {
                    m = m.withArguments(ListUtils.mapFirst(depArgs, platform -> updateDependency((J.MethodInvocation) platform, ctx)));
                }

                return m;
            }

            private J.MethodInvocation updateDependency(J.MethodInvocation m, ExecutionContext ctx) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal) {
                    String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                    if (gav != null) {
                        Dependency original = DependencyStringNotationConverter.parse(gav);
                        if (depMatcher.matches(original.getGroupId(), original.getArtifactId())) {
                            Dependency updated = original;
                            if (newGroupId != null && !updated.getGroupId().equals(newGroupId)) {
                                updated = updated.withGroupId(newGroupId);
                            }
                            if (newArtifactId != null && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withArtifactId(newArtifactId);
                            }
                            if (newVersion != null) {
                                doAfterVisit(new UpgradeDependencyVersion(updated.getGroupId(), updated.getArtifactId(), newVersion, versionPattern));
                            }
                            if (original != updated) {
                                String replacement = updated.toStringNotation();
                                m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, replacement)));
                            }
                        }
                    }
                } else if (m.getArguments().get(0) instanceof G.GString) {
                    List<J> strings = ((G.GString) depArgs.get(0)).getStrings();
                    if (strings.size() >= 2 &&
                            strings.get(0) instanceof J.Literal) {
                        Dependency original = DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                        if (depMatcher.matches(original.getGroupId(), original.getArtifactId())) {
                            Dependency updated = original;
                            if (newGroupId != null && !updated.getGroupId().equals(newGroupId)) {
                                updated = updated.withGroupId(newGroupId);
                            }
                            if (newArtifactId != null && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withArtifactId(newArtifactId);
                            }
                            if (newVersion != null) {
                                doAfterVisit(new UpgradeDependencyVersion(updated.getGroupId(), updated.getArtifactId(), newVersion, versionPattern));
                            }
                            if (original != updated) {
                                String replacement = updated.toStringNotation();
                                m = m.withArguments(ListUtils.mapFirst(depArgs, arg -> {
                                    G.GString gString = (G.GString) arg;
                                    return gString.withStrings(ListUtils.mapFirst(gString.getStrings(), l -> ((J.Literal) l).withValue(replacement).withValueSource(replacement)));
                                }));
                            }
                        }
                    }
                } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                    G.MapEntry groupEntry = null;
                    G.MapEntry artifactEntry = null;
                    String groupId = null;
                    String artifactId = null;

                    String valueDelimiter = "'";
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
                            if (value.getValueSource() != null) {
                                valueDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                            groupEntry = arg;
                            groupId = valueValue;
                        } else if ("name".equals(keyValue)) {
                            artifactEntry = arg;
                            artifactId = valueValue;
                        }
                    }
                    if (groupId == null || artifactId == null) {
                        return m;
                    }
                    if (!depMatcher.matches(groupId, artifactId)) {
                        return m;
                    }
                    String updatedGroupId = groupId;
                    if (newGroupId != null && !updatedGroupId.equals(newGroupId)) {
                        updatedGroupId = newGroupId;
                    }
                    String updatedArtifactId = artifactId;
                    if (newArtifactId != null && !updatedArtifactId.equals(newArtifactId)) {
                        updatedArtifactId = newArtifactId;
                    }
                    if (newVersion != null) {
                        doAfterVisit(new UpgradeDependencyVersion(updatedGroupId, updatedArtifactId, newVersion, versionPattern));
                    }

                    if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId)) {
                        String delimiter = valueDelimiter;
                        G.MapEntry finalGroup = groupEntry;
                        G.MapEntry finalArtifact = artifactEntry;
                        m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                            if (arg == finalGroup) {
                                return finalGroup.withValue(((J.Literal) finalGroup.getValue())
                                        .withValue(newGroupId)
                                        .withValueSource(delimiter + newGroupId + delimiter));
                            }
                            if (arg == finalArtifact) {
                                return finalArtifact.withValue(((J.Literal) finalArtifact.getValue())
                                        .withValue(newArtifactId)
                                        .withValueSource(delimiter + newArtifactId + delimiter));
                            }
                            return arg;
                        }));
                    }
                }

                return m;
            }

            private GradleProject updateGradleModel(GradleProject gp) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            GroupArtifactVersion gav = requested.getGav();
                            if (newGroupId != null) {
                                gav = gav.withGroupId(newGroupId);
                            }
                            if (newArtifactId != null) {
                                gav = gav.withArtifactId(newArtifactId);
                            }
                            if (gav != requested.getGav()) {
                                return requested.withGav(gav);
                            }
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withResolved(ListUtils.map(gdc.getResolved(), resolved -> {
                        if (depMatcher.matches(resolved.getGroupId(), resolved.getArtifactId())) {
                            ResolvedGroupArtifactVersion gav = resolved.getGav();
                            if (newGroupId != null) {
                                gav = gav.withGroupId(newGroupId);
                            }
                            if (newArtifactId != null) {
                                gav = gav.withArtifactId(newArtifactId);
                            }
                            if (gav != resolved.getGav()) {
                                return resolved.withGav(gav);
                            }
                        }
                        return resolved;
                    }));
                    anyChanged |= newGdc != gdc;
                    newNameToConfiguration.put(newGdc.getName(), newGdc);
                }
                if (anyChanged) {
                    gp = gp.withNameToConfiguration(newNameToConfiguration);
                }
                return gp;
            }
        });
    }
}
