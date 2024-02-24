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
import org.openrewrite.*;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.time.Duration;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
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
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s` to `%s`", groupId, artifactId, newConfiguration);
    }

    @Override
    public String getDescription() {
        return "A common example is the need to change `compile` to `api`/`implementation` as " +
               "[part of the move](https://docs.gradle.org/current/userguide/upgrading_version_6.html) to Gradle 7.x and later.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(DependencyMatcher.build(groupId + ":" + artifactId));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!dependencyDsl.matches(m) || !(StringUtils.isBlank(configuration) || m.getSimpleName().equals(configuration))) {
                    return m;
                }

                DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
                List<Expression> args = m.getArguments();
                if (args.get(0) instanceof J.Literal) {
                    J.Literal arg = (J.Literal) args.get(0);
                    if (!(arg.getValue() instanceof String)) {
                        return m;
                    }

                    Dependency dependency = DependencyStringNotationConverter.parse((String) arg.getValue());
                    if (!dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return m;
                    }
                } else if (args.get(0) instanceof G.GString) {
                    G.GString gString = (G.GString) args.get(0);
                    List<J> strings = gString.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                        return m;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    if (!(groupArtifact.getValue() instanceof String)) {
                        return m;
                    }

                    Dependency dependency = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (!dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return m;
                    }
                } else if (args.get(0) instanceof G.MapEntry && args.size() >= 2) {
                    Expression groupValue = ((G.MapEntry) args.get(0)).getValue();
                    Expression artifactValue = ((G.MapEntry) args.get(1)).getValue();
                    if (!(groupValue instanceof J.Literal) || !(artifactValue instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal groupLiteral = (J.Literal) groupValue;
                    J.Literal artifactLiteral = (J.Literal) artifactValue;
                    if (!(groupLiteral.getValue() instanceof String) || !(artifactLiteral.getValue() instanceof String)) {
                        return m;
                    }

                    if (!dependencyMatcher.matches((String) groupLiteral.getValue(), (String) artifactLiteral.getValue())) {
                        return m;
                    }
                } else if (args.get(0) instanceof J.MethodInvocation) {
                    J.MethodInvocation inner = (J.MethodInvocation) args.get(0);
                    if (!(inner.getSimpleName().equals("project") || inner.getSimpleName().equals("platform") || inner.getSimpleName().equals("enforcedPlatform"))) {
                        return m;
                    }
                    List<Expression> innerArgs = inner.getArguments();
                    if (!(innerArgs.get(0) instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal value = (J.Literal) innerArgs.get(0);
                    if (!(value.getValue() instanceof String)) {
                        return m;
                    }

                    Dependency dependency;
                    if (inner.getSimpleName().equals("project")) {
                        dependency = new Dependency("", ((String) value.getValue()).substring(1), null, null, null);
                    } else {
                        dependency = DependencyStringNotationConverter.parse((String) value.getValue());
                    }

                    if (!dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return m;
                    }
                } else {
                    return m;
                }

                if (newConfiguration.equals(m.getSimpleName())) {
                    return m;
                }

                return m.withName(m.getName().withSimpleName(newConfiguration));
            }
        });
    }
}
