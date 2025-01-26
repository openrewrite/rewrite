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
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyExtension extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New extension",
            description = "An artifact extension.",
            example = "jar")
    String newExtension;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change a Gradle dependency extension";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s` to `%s`", groupId, artifactId, newExtension);
    }

    @Override
    public String getDescription() {
        return "Finds dependencies declared in `build.gradle` files.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(DependencyMatcher.build(groupId + ":" + artifactId));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
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
                        Dependency dependency = DependencyStringNotationConverter.parse(gav);
                        if (dependency != null && !newExtension.equals(dependency.getExt())) {
                            Dependency newDependency = dependency.withExt(newExtension);
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, newDependency.toStringNotation())));
                        }
                    }
                } else if (depArgs.get(0) instanceof G.MapEntry) {
                    G.MapEntry extensionEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String extension = null;

                    String extensionStringDelimiter = "'";
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
                        } else if ("name".equals(keyValue)) {
                            artifactId = valueValue;
                        } else if ("ext".equals(keyValue) && !newExtension.equals(valueValue)) {
                            if (value.getValueSource() != null) {
                                extensionStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                            extensionEntry = arg;
                            extension = valueValue;
                        }
                    }
                    if (groupId == null || artifactId == null || extension == null) {
                        return m;
                    }
                    String delimiter = extensionStringDelimiter;
                    G.MapEntry finalExtension = extensionEntry;
                    m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                        if (arg == finalExtension) {
                            return finalExtension.withValue(((J.Literal) finalExtension.getValue())
                                    .withValue(newExtension)
                                    .withValueSource(delimiter + newExtension + delimiter));
                        }
                        return arg;
                    }));
                } else if (depArgs.get(0) instanceof G.MapLiteral) {
                    G.MapLiteral map = (G.MapLiteral) depArgs.get(0);
                    G.MapEntry extensionEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String extension = null;

                    String extensionStringDelimiter = "'";
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
                        } else if ("name".equals(keyValue)) {
                            artifactId = valueValue;
                        } else if ("ext".equals(keyValue) && !newExtension.equals(valueValue)) {
                            if (value.getValueSource() != null) {
                                extensionStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                            }
                            extensionEntry = arg;
                            extension = valueValue;
                        }
                    }
                    if (groupId == null || artifactId == null || extension == null) {
                        return m;
                    }
                    String delimiter = extensionStringDelimiter;
                    G.MapEntry finalExtension = extensionEntry;
                    m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                        G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                        return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), e -> {
                            if (e == finalExtension) {
                                return finalExtension.withValue(((J.Literal) finalExtension.getValue())
                                        .withValue(newExtension)
                                        .withValueSource(delimiter + newExtension + delimiter));
                            }
                            return e;
                        }));
                    }));
                }

                return m;
            }
        });
    }
}
