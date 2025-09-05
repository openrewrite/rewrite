/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class GradleDependency implements Trait<J.MethodInvocation> {
    Cursor cursor;

    @Getter
    ResolvedDependency resolvedDependency;

    public static class Matcher extends GradleTraitMatcher<GradleDependency> {
        private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

        @Nullable
        protected String configuration;

        @Nullable
        protected String groupId;

        @Nullable
        protected String artifactId;

        public Matcher configuration(@Nullable String configuration) {
            this.configuration = configuration;
            return this;
        }

        public Matcher groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Matcher artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleDependency, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    GradleDependency dependency = test(getCursor());
                    return dependency != null ?
                            (J) visitor.visit(dependency, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
        }

        @Override
        protected @Nullable GradleDependency test(Cursor cursor) {
            Object object = cursor.getValue();
            if (object instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

                if (!withinDependenciesBlock(cursor)) {
                    return null;
                }

                if (withinDependencyConstraintsBlock(cursor)) {
                    // A dependency constraint is different from an actual dependency
                    return null;
                }

                GradleProject gradleProject = getGradleProject(cursor);
                GradleDependencyConfiguration gdc = getConfiguration(gradleProject, methodInvocation);
                if (gdc == null && !(DEPENDENCY_DSL_MATCHER.matches(methodInvocation) && !"project".equals(methodInvocation.getSimpleName()))) {
                    return null;
                }

                if (!StringUtils.isBlank(configuration) && !methodInvocation.getSimpleName().equals(configuration)) {
                    return null;
                }

                org.openrewrite.gradle.internal.Dependency dependency = null;
                Expression argument = methodInvocation.getArguments().get(0);
                if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
                    dependency = parseDependency(methodInvocation.getArguments());
                } else if (argument instanceof J.Binary && ((J.Binary) argument).getLeft() instanceof J.Literal) {
                    dependency = parseDependency(Arrays.asList(((J.Binary) argument).getLeft()));
                } else if (argument instanceof J.MethodInvocation) {
                    if ("platform".equals(((J.MethodInvocation) argument).getSimpleName()) ||
                            "enforcedPlatform".equals(((J.MethodInvocation) argument).getSimpleName())) {
                        dependency = parseDependency(((J.MethodInvocation) argument).getArguments());
                    } else if ("project".equals(((J.MethodInvocation) argument).getSimpleName())) {
                        // project dependencies are not yet supported
                        return null;
                    }
                }

                if (dependency == null) {
                    return null;
                }

                if (gdc != null) {
                    if (gdc.isCanBeResolved()) {
                        for (ResolvedDependency resolvedDependency : gdc.getResolved()) {
                            if ((groupId == null || matchesGlob(resolvedDependency.getGroupId(), groupId)) &&
                                    (artifactId == null || matchesGlob(resolvedDependency.getArtifactId(), artifactId))) {
                                Dependency req = resolvedDependency.getRequested();
                                if ((req.getGroupId() == null || req.getGroupId().equals(dependency.getGroupId())) &&
                                        req.getArtifactId().equals(dependency.getArtifactId())) {
                                    return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
                                }
                            }
                        }
                    } else {
                        for (GradleDependencyConfiguration transitiveConfiguration : gradleProject.configurationsExtendingFrom(gdc, true)) {
                            if (transitiveConfiguration.isCanBeResolved()) {
                                for (ResolvedDependency resolvedDependency : transitiveConfiguration.getResolved()) {
                                    if ((groupId == null || matchesGlob(resolvedDependency.getGroupId(), groupId)) &&
                                            (artifactId == null || matchesGlob(resolvedDependency.getArtifactId(), artifactId))) {
                                        Dependency req = resolvedDependency.getRequested();
                                        if ((req.getGroupId() == null || req.getGroupId().equals(dependency.getGroupId())) &&
                                                req.getArtifactId().equals(dependency.getArtifactId())) {
                                            return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if ((groupId == null || matchesGlob(dependency.getGroupId(), groupId)) &&
                        (artifactId == null || matchesGlob(dependency.getArtifactId(), artifactId))) {
                    // Couldn't find the actual resolved dependency, return a virtualized one instead
                    ResolvedDependency resolvedDependency = ResolvedDependency.builder()
                            .depth(-1)
                            .gav(new ResolvedGroupArtifactVersion(null, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() != null ? dependency.getVersion() : "", null))
                            .classifier(dependency.getClassifier())
                            .type(dependency.getExt())
                            .requested(Dependency.builder()
                                    .scope(methodInvocation.getSimpleName())
                                    .type(dependency.getExt())
                                    .gav(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
                                    .classifier(dependency.getClassifier())
                                    .build())
                            .build();
                    return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
                }
            }

            return null;
        }

        /**
         * Our Gradle model doesn't truly know the requested versions as it isn't able to get that from the Gradle API.
         * So if this Trait has figured out which declaration made the request resulting in a particular resolved dependency
         * use that more-accurate information instead.
         */
        private static ResolvedDependency withRequested(ResolvedDependency resolved, org.openrewrite.gradle.internal.Dependency requested) {
            return resolved.withRequested(resolved.getRequested().withGav(requested.getGav()));
        }


        private static @Nullable GradleDependencyConfiguration getConfiguration(@Nullable GradleProject gradleProject, J.MethodInvocation methodInvocation) {
            if (gradleProject == null) {
                return null;
            }

            String methodName = methodInvocation.getSimpleName();
            if ("classpath".equals(methodName)) {
                return gradleProject.getBuildscript().getConfiguration(methodName);
            } else {
                return gradleProject.getConfiguration(methodName);
            }
        }

        private boolean withinDependenciesBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencies");
        }

        private boolean withinDependencyConstraintsBlock(Cursor cursor) {
            return withinBlock(cursor, "constraints") && withinDependenciesBlock(cursor);
        }

        private org.openrewrite.gradle.internal.@Nullable Dependency parseDependency(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            if (argument instanceof J.Literal) {
                return DependencyStringNotationConverter.parse((String) ((J.Literal) argument).getValue());
            } else if (argument instanceof G.GString) {
                G.GString gstring = (G.GString) argument;
                List<J> strings = gstring.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                }
            } else if (argument instanceof G.MapLiteral) {
                List<Expression> mapEntryExpressions = ((G.MapLiteral) argument).getElements()
                        .stream()
                        .map(e -> (Expression) e)
                        .collect(toList());
                return getMapEntriesDependency(mapEntryExpressions);
            } else if (argument instanceof G.MapEntry) {
                return getMapEntriesDependency(arguments);
            } else if (argument instanceof J.Assignment) {
                String group = null;
                String artifact = null;

                for (Expression e : arguments) {
                    if (!(e instanceof J.Assignment)) {
                        continue;
                    }
                    J.Assignment arg = (J.Assignment) e;
                    if (!(arg.getVariable() instanceof J.Identifier) || !(arg.getAssignment() instanceof J.Literal)) {
                        continue;
                    }
                    J.Identifier identifier = (J.Identifier) arg.getVariable();
                    J.Literal value = (J.Literal) arg.getAssignment();
                    if (!(value.getValue() instanceof String)) {
                        continue;
                    }
                    String name = identifier.getSimpleName();
                    if ("group".equals(name)) {
                        group = (String) value.getValue();
                    } else if ("name".equals(name)) {
                        artifact = (String) value.getValue();
                    }
                }

                if (group == null || artifact == null) {
                    return null;
                }

                return new org.openrewrite.gradle.internal.Dependency(group, artifact, null, null, null);
            } else if (argument instanceof K.StringTemplate) {
                K.StringTemplate template = (K.StringTemplate) argument;
                List<J> strings = template.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                }
            }

            return null;
        }

        private static org.openrewrite.gradle.internal.@Nullable Dependency getMapEntriesDependency(List<Expression> arguments) {
            String group = null;
            String artifact = null;

            for (Expression e : arguments) {
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
                if ("group".equals(keyValue)) {
                    group = (String) value.getValue();
                } else if ("name".equals(keyValue)) {
                    artifact = (String) value.getValue();
                }
            }

            if (group == null || artifact == null) {
                return null;
            }

            return new org.openrewrite.gradle.internal.Dependency(group, artifact, null, null, null);
        }
    }
}
