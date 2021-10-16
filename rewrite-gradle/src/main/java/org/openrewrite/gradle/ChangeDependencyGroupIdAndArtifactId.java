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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeDependencyGroupIdAndArtifactId extends Recipe {
    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use.",
            example = "corp.internal.openrewrite.recipe")
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use.",
            example = "rewrite-testing-frameworks")
    String newArtifactId;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change Gradle dependency groupId and artifactId";
    }

    @Override
    public String getDescription() {
        return "Change the groupId and artifactId of a specified Gradle dependency. ";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected GroovyVisitor<ExecutionContext> getVisitor() {
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                MethodMatcher dependency = new MethodMatcher("DependencyHandlerSpec *(..)");
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, context);
                if (dependency.matches(m)) {
                    if (configuration == null || m.getSimpleName().equals(configuration)) {
                        List<Expression> depArgs = m.getArguments();
                        String versionStringDelimiter = "'";
                        if (depArgs.get(0) instanceof J.Literal) {
                            String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                            if (gav != null) {
                                String[] gavs = gav.split(":");

                                if (gavs.length >= 3 &&
                                        !newGroupId.equals(gavs[0]) && StringUtils.matchesGlob(gavs[0], oldGroupId) &&
                                        !newArtifactId.equals(gavs[1]) && StringUtils.matchesGlob(gavs[1], oldArtifactId)) {
                                    String valueSource = ((J.Literal) depArgs.get(0)).getValueSource();
                                    String delimiter = (valueSource == null) ? versionStringDelimiter
                                            : valueSource.substring(0, valueSource.indexOf(gav));

                                    String newGav = newGroupId + ":" + newArtifactId + ":" + gavs[2];
                                    m = m.withArguments(ListUtils.map(m.getArguments(), (n, arg) ->
                                            n == 0 ?
                                                    ((J.Literal) arg).withValue(newGav).withValueSource(delimiter + newGav + delimiter) :
                                                    arg
                                    ));
                                }
                            }
                        } else if (depArgs.get(0) instanceof G.MapEntry) {
                            G.MapEntry groupEntry = null;
                            G.MapEntry artifactEntry = null;

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
                                if ("group".equals(keyValue)
                                        && !newGroupId.equals(valueValue)
                                        && StringUtils.matchesGlob(valueValue, oldGroupId)) {
                                    if (value.getValueSource() != null) {
                                        versionStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                                    }
                                    groupEntry = arg;
                                } else if ("name".equals(keyValue)
                                        && !newArtifactId.equals(valueValue)
                                        && StringUtils.matchesGlob(valueValue, oldArtifactId)) {
                                    artifactEntry = arg;
                                }
                            }
                            if (groupEntry == null || artifactEntry == null) {
                                return m;
                            }
                            String delimiter = versionStringDelimiter;
                            G.MapEntry finalGroup = groupEntry;
                            G.MapEntry finalArtifact = artifactEntry;
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                if (arg == finalGroup) {
                                    return finalGroup.withValue(((J.Literal)finalGroup.getValue())
                                            .withValue(newGroupId)
                                            .withValueSource(delimiter + newGroupId + delimiter));
                                } else if (arg == finalArtifact) {
                                    return finalArtifact.withValue(((J.Literal)finalArtifact.getValue())
                                            .withValue(newArtifactId)
                                            .withValueSource(delimiter + newArtifactId + delimiter));
                                }
                                return arg;
                            }));
                        }
                    }
                }
                return m;
            }
        };
    }
}
