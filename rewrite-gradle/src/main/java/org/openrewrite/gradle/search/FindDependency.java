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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDependency extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in. If omitted then all configurations will be searched.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Find Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Finds dependencies declared in `build.gradle` files. See the [reference](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph) on Gradle configurations or the diagram below for a description of what configuration to use." +
                " A project's compile and runtime classpath is based on these configurations.\n\n<img alt=\"Gradle compile classpath\" src=\"https://docs.gradle.org/current/userguide/img/java-library-ignore-deprecated-main.png\" width=\"200px\"/>\n" +
                " A project's test classpath is based on these configurations.\n\n<img alt=\"Gradle test classpath\" src=\"https://docs.gradle.org/current/userguide/img/java-library-ignore-deprecated-test.png\" width=\"200px\"/>.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dependency = new MethodMatcher("DependencyHandlerSpec *(..)");
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (dependency.matches(method)) {
                    if (StringUtils.isBlank(configuration) || method.getSimpleName().equals(configuration)) {
                        List<Expression> depArgs = method.getArguments();
                        if (depArgs.get(0) instanceof J.Literal) {
                            String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                            assert gav != null;
                            String[] parts = gav.split(":");
                            if(gav.length() >= 2) {
                                if (StringUtils.matchesGlob(parts[0], groupId) &&
                                    StringUtils.matchesGlob(parts[1], artifactId)) {
                                    return SearchResult.found(method);
                                }
                            }
                        }
                    }
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
