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
import org.openrewrite.Validated;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.time.Duration;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeDependencyConfiguration extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New configuration",
            description = "A dependency configuration container.",
            example = "api")
    String newConfiguration;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change a Gradle dependency configuration";
    }

    @Override
    public String getDescription() {
        return "Finds dependencies declared in `build.gradle` files.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(DependencyMatcher.build(groupId + ":" + artifactId));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    public GroovyVisitor<ExecutionContext> getVisitor() {
        return new GroovyVisitor<ExecutionContext>() {
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, context);
                if (!dependencyDsl.matches(m) || !(StringUtils.isBlank(configuration) || m.getSimpleName().equals(configuration))) {
                    return m;
                }

                if (!newConfiguration.equals(m.getSimpleName())) {
                    m = m.withName(m.getName().withSimpleName(newConfiguration));
                }

                return m;
            }
        };
    }
}
