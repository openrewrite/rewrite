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
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.semver.DependencyMatcher;

import java.util.*;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyClassifier extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New classifier",
            description = "A qualification classifier for the dependency.",
            example = "sources",
            required = false)
    @Nullable
    String newClassifier;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change a Gradle dependency classifier";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s` to `%s`", groupId, artifactId, newClassifier);
    }

    @Override
    public String getDescription() {
        return "Changes classifier of an existing dependency declared in `build.gradle` files.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(DependencyMatcher.build(groupId + ":" + artifactId));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());

            GradleProject gradleProject;

            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()) {
                    return cu;
                }

                gradleProject = maybeGp.get();

                G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                if (g != cu) {
                    g = g.withMarkers(g.getMarkers().setByType(updateGradleModel(gradleProject)));
                }
                return g;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher()
                        .configuration(configuration)
                        .groupId(groupId)
                        .artifactId(artifactId);

                if (!gradleDependencyMatcher.get(getCursor()).isPresent()) {
                    return m;
                }

                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal) {
                    String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                    if (gav != null) {
                        Dependency dependency = DependencyNotation.parse(gav);
                        if (dependency != null && dependency.getVersion() != null && !Objects.equals(newClassifier, dependency.getClassifier())) {
                            Dependency newDependency = dependency.withClassifier(newClassifier);
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, DependencyNotation.toStringNotation(newDependency))));
                        }
                    }
                } else if (depArgs.get(0) instanceof G.MapEntry) {
                    G.MapEntry classifierEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String version = null;
                    String classifier = null;

                    String groupDelimiter = "'";
                    G.MapEntry mapEntry = null;
                    String classifierStringDelimiter = null;
                    int index = 0;
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
                            if (value.getValueSource() != null) {
                                groupDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                        } else if ("name".equals(keyValue)) {
                            if (index > 0 && mapEntry == null) {
                                mapEntry = arg;
                            }
                            artifactId = valueValue;
                        } else if ("version".equals(keyValue)) {
                            version = valueValue;
                        } else if ("classifier".equals(keyValue)) {
                            if (value.getValueSource() != null) {
                                classifierStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                            classifierEntry = arg;
                            classifier = valueValue;
                        }
                        index++;
                    }
                    if (groupId == null || artifactId == null || Objects.equals(newClassifier, classifier)) {
                        return m;
                    }

                    if (classifier == null) {
                        String delimiter = groupDelimiter;
                        List<Expression> args = m.getArguments();
                        J.Literal keyLiteral = new J.Literal(Tree.randomId(), mapEntry == null ? Space.EMPTY : mapEntry.getKey().getPrefix(), Markers.EMPTY, "classifier", "classifier", null, JavaType.Primitive.String);
                        J.Literal valueLiteral = new J.Literal(Tree.randomId(), mapEntry == null ? Space.EMPTY : mapEntry.getValue().getPrefix(), Markers.EMPTY, newClassifier, delimiter + newClassifier + delimiter, null, JavaType.Primitive.String);
                        args.add(new G.MapEntry(Tree.randomId(), mapEntry == null ? Space.EMPTY : mapEntry.getPrefix(), Markers.EMPTY, JRightPadded.build(keyLiteral), valueLiteral, null));
                        m = m.withArguments(args);
                    } else {
                        G.MapEntry finalClassifier = classifierEntry;
                        if (newClassifier == null) {
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> arg == finalClassifier ? null : arg));
                        } else {
                            String delimiter = classifierStringDelimiter; // `classifierStringDelimiter` cannot be null
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                if (arg == finalClassifier) {
                                    return finalClassifier.withValue(((J.Literal) finalClassifier.getValue())
                                            .withValue(newClassifier)
                                            .withValueSource(delimiter + newClassifier + delimiter));
                                }
                                return arg;
                            }));
                        }
                    }
                } else if (depArgs.get(0) instanceof G.MapLiteral) {
                    G.MapLiteral map = (G.MapLiteral) depArgs.get(0);
                    G.MapEntry classifierEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String classifier = null;

                    String groupDelimiter = "'";
                    G.MapEntry mapEntry = null;
                    String classifierStringDelimiter = null;
                    int index = 0;
                    for (G.MapEntry arg : map.getElements()) {
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
                            if (value.getValueSource() != null) {
                                groupDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                        } else if ("name".equals(keyValue)) {
                            if (index > 0 && mapEntry == null) {
                                mapEntry = arg;
                            }
                            artifactId = valueValue;
                        } else if ("classifier".equals(keyValue)) {
                            if (value.getValueSource() != null) {
                                classifierStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                            classifierEntry = arg;
                            classifier = valueValue;
                        }
                        index++;
                    }
                    if (groupId == null || artifactId == null || Objects.equals(newClassifier, classifier)) {
                        return m;
                    }

                    if (classifier == null) {
                        String delimiter = groupDelimiter;
                        G.MapEntry finalMapEntry = mapEntry;
                        J.Literal keyLiteral = new J.Literal(Tree.randomId(), mapEntry == null ? Space.EMPTY : mapEntry.getKey().getPrefix(), Markers.EMPTY, "classifier", "classifier", null, JavaType.Primitive.String);
                        J.Literal valueLiteral = new J.Literal(Tree.randomId(), mapEntry == null ? Space.EMPTY : mapEntry.getValue().getPrefix(), Markers.EMPTY, newClassifier, delimiter + newClassifier + delimiter, null, JavaType.Primitive.String);
                        m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                            G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                            return mapLiteral.withElements(ListUtils.concat(mapLiteral.getElements(), new G.MapEntry(Tree.randomId(), finalMapEntry == null ? Space.EMPTY : finalMapEntry.getPrefix(), Markers.EMPTY, JRightPadded.build(keyLiteral), valueLiteral, null)));
                        }));
                    } else {
                        G.MapEntry finalClassifier = classifierEntry;
                        if (newClassifier == null) {
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                                return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), e -> e == finalClassifier ? null : e));
                            }));
                        } else {
                            String delimiter = classifierStringDelimiter; // `classifierStringDelimiter` cannot be null
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                                return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), e -> {
                                    if (e == finalClassifier) {
                                        return finalClassifier.withValue(((J.Literal) finalClassifier.getValue())
                                                .withValue(newClassifier)
                                                .withValueSource(delimiter + newClassifier + delimiter));
                                    }
                                    return e;
                                }));
                            }));
                        }
                    }
                }

                return m;
            }

            private GradleProject updateGradleModel(GradleProject gp) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    if (!StringUtils.isBlank(configuration) && !configuration.equals(gdc.getName())) {
                        newNameToConfiguration.put(gdc.getName(), gdc);
                        continue;
                    }

                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId()) && !Objects.equals(requested.getClassifier(), newClassifier)) {
                            return requested.withClassifier(newClassifier);
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> {
                        if (depMatcher.matches(resolved.getGroupId(), resolved.getArtifactId()) && !Objects.equals(resolved.getClassifier(), newClassifier)) {
                            return resolved.withClassifier(newClassifier);
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
