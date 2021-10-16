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
public class ChangeDependencyVersion extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    @Nullable
    String artifactId;

    @Option(displayName = "New Version",
            description = "The version number to update the dependency to",
            example = "1.0")
    String newVersion;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change a Gradle dependency version";
    }

    @Override
    public String getDescription() {
        return "Finds dependencies declared in `build.gradle` files.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected GroovyVisitor<ExecutionContext> getVisitor() {
        MethodMatcher dependency = new MethodMatcher("DependencyHandlerSpec *(..)");
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
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
                                        StringUtils.matchesGlob(gavs[0], groupId) &&
                                        StringUtils.matchesGlob(gavs[1], artifactId) &&
                                        !StringUtils.matchesGlob(gavs[2], newVersion)) {
                                    String valueSource = ((J.Literal) depArgs.get(0)).getValueSource();
                                    String delimiter = (valueSource == null) ? versionStringDelimiter
                                            : valueSource.substring(0, valueSource.indexOf(gav));

                                    String newGav = gavs[0] + ":" + gavs[1] + ":" + newVersion;
                                    m = m.withArguments(ListUtils.map(m.getArguments(), (n, arg) ->
                                            n == 0 ?
                                                    ((J.Literal) arg).withValue(newGav).withValueSource(delimiter + newGav + delimiter) :
                                                    arg
                                    ));
                                }
                            }
                        } else if(depArgs.get(0) instanceof G.MapEntry) {
                            G.MapEntry groupEntry = null;
                            G.MapEntry artifactEntry = null;
                            G.MapEntry versionEntry = null;

                            for(Expression e : depArgs) {
                                if(!(e instanceof G.MapEntry)) {
                                    continue;
                                }
                                G.MapEntry arg = (G.MapEntry)e;
                                if(!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                                    continue;
                                }
                                J.Literal key = (J.Literal) arg.getKey();
                                J.Literal value = (J.Literal) arg.getValue();
                                if(!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                                    continue;
                                }
                                String keyValue = (String)key.getValue();
                                String valueValue = (String)value.getValue();
                                if("group".equals(keyValue) && StringUtils.matchesGlob(valueValue, groupId)) {
                                    groupEntry = arg;
                                } else if("name".equals(keyValue) && StringUtils.matchesGlob(valueValue, artifactId)) {
                                    artifactEntry = arg;
                                } else if("version".equals(keyValue) && !valueValue.equals(newVersion)) {
                                    if(value.getValueSource() != null) {
                                        versionStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                                    }
                                    versionEntry = arg;
                                }
                            }
                            if(groupEntry == null || artifactEntry == null || versionEntry == null) {
                                return m;
                            }
                            G.MapEntry finalVersion = versionEntry;
                            String delimiter = versionStringDelimiter;
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                if(arg == finalVersion) {
                                    return finalVersion.withValue(((J.Literal)finalVersion.getValue())
                                            .withValue(newVersion)
                                            .withValueSource(delimiter + newVersion + delimiter));
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
