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
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents multiple Gradle dependencies declared in a single method invocation (varargs).
 * For example: implementation('dep1:1.0', 'dep2:2.0', 'dep3:3.0')
 */
@Value
public class GradleMultiDependency implements Trait<J.MethodInvocation> {
    Cursor cursor;

    @Getter
    List<GradleDependency> dependencies;

    /**
     * Optional filters that were used when matching this multi-dependency.
     * When present, the map() method will only apply transformations to dependencies
     * that match these filters.
     */
    @Nullable
    String groupIdFilter;

    @Nullable
    String artifactIdFilter;

    /**
     * Gets the configuration name for these dependencies.
     * For example, "implementation", "testImplementation", "api", etc.
     *
     * @return The configuration name
     */
    public String getConfigurationName() {
        J.MethodInvocation m = cursor.getValue();
        return m.getSimpleName();
    }

    /**
     * Maps a transformation function over each GradleDependency in this multi-dependency
     * that matches the provided DependencyMatcher.
     * Returns a new J.MethodInvocation with updated arguments if any dependencies changed.
     *
     * @param matcher The DependencyMatcher to filter which dependencies to transform
     * @param mapper Function to transform each matching GradleDependency
     * @return The updated J.MethodInvocation if any changes were made, or the original if not
     */
    public J.MethodInvocation map(DependencyMatcher matcher, Function<GradleDependency, GradleDependency> mapper) {
        J.MethodInvocation m = cursor.getValue();
        List<Expression> originalArgs = m.getArguments();
        List<Expression> newArgs = new ArrayList<>(originalArgs.size());
        boolean anyChanged = false;

        for (int i = 0; i < originalArgs.size(); i++) {
            Expression arg = originalArgs.get(i);
            GradleDependency dep = findDependencyForArg(i);

            if (dep != null && dep.matches(matcher)) {
                GradleDependency mapped = mapper.apply(dep);
                if (mapped != dep) {
                    // The dependency was modified
                    anyChanged = true;
                    // Extract the literal from the synthetic wrapper
                    J.MethodInvocation syntheticWrapper = mapped.getTree();
                    if (!syntheticWrapper.getArguments().isEmpty()) {
                        newArgs.add(syntheticWrapper.getArguments().get(0));
                    } else {
                        newArgs.add(arg);
                    }
                } else {
                    newArgs.add(arg);
                }
            } else {
                newArgs.add(arg);
            }
        }

        return anyChanged ? m.withArguments(newArgs) : m;
    }

    /**
     * Maps a transformation function over each GradleDependency in this multi-dependency.
     * If groupIdFilter and/or artifactIdFilter are set, only applies the transformation
     * to dependencies that match those filters.
     * Returns a new J.MethodInvocation with updated arguments if any dependencies changed.
     *
     * @param mapper Function to transform each GradleDependency
     * @return The updated J.MethodInvocation if any changes were made, or the original if not
     */
    public J.MethodInvocation map(Function<GradleDependency, GradleDependency> mapper) {
        J.MethodInvocation m = cursor.getValue();
        List<Expression> originalArgs = m.getArguments();
        List<Expression> newArgs = new ArrayList<>(originalArgs.size());
        boolean anyChanged = false;

        for (int i = 0; i < originalArgs.size(); i++) {
            Expression arg = originalArgs.get(i);
            GradleDependency dep = findDependencyForArg(i);

            if (dep != null && shouldTransform(dep)) {
                GradleDependency mapped = mapper.apply(dep);
                if (mapped != dep) {
                    // The dependency was modified
                    anyChanged = true;
                    // Extract the literal from the synthetic wrapper
                    J.MethodInvocation syntheticWrapper = mapped.getTree();
                    if (!syntheticWrapper.getArguments().isEmpty()) {
                        newArgs.add(syntheticWrapper.getArguments().get(0));
                    } else {
                        newArgs.add(arg); // Fallback to original
                    }
                } else {
                    newArgs.add(arg);
                }
            } else {
                // Not a dependency argument or doesn't match filters, keep as-is
                newArgs.add(arg);
            }
        }

        return anyChanged ? m.withArguments(newArgs) : m;
    }

    /**
     * Determines if a dependency should be transformed based on the configured filters.
     */
    private boolean shouldTransform(GradleDependency dependency) {
        if (groupIdFilter == null && artifactIdFilter == null) {
            // No filters configured, transform all dependencies
            return true;
        }

        String groupId = dependency.getDeclaredGroupId();
        String artifactId = dependency.getDeclaredArtifactId();

        boolean groupMatches = groupIdFilter == null ||
            (groupId != null && StringUtils.matchesGlob(groupId, groupIdFilter));
        boolean artifactMatches = artifactIdFilter == null ||
            (artifactId != null && StringUtils.matchesGlob(artifactId, artifactIdFilter));

        return groupMatches && artifactMatches;
    }

    /**
     * Finds the GradleDependency corresponding to the argument at the given index.
     */
    @Nullable
    private GradleDependency findDependencyForArg(int index) {
        J.MethodInvocation m = cursor.getValue();
        int literalIndex = 0;
        for (int i = 0; i <= index && i < m.getArguments().size(); i++) {
            Expression arg = m.getArguments().get(i);
            if (arg instanceof J.Literal) {
                if (i == index) {
                    return literalIndex < dependencies.size() ? dependencies.get(literalIndex) : null;
                }
                literalIndex++;
            } else if (i == index) {

                return null;
            }
        }
        return null;
    }

    public static class Matcher extends GradleTraitMatcher<GradleMultiDependency> {
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
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleMultiDependency, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    GradleMultiDependency multiDependency = test(getCursor());
                    return multiDependency != null ?
                            (J) visitor.visit(multiDependency, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
        }

        @Override
        protected @Nullable GradleMultiDependency test(Cursor cursor) {
            Object object = cursor.getValue();
            if (!(object instanceof J.MethodInvocation)) {
                return null;
            }

            J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

            if (!withinDependenciesBlock(cursor)) {
                return null;
            }

            if (!DEPENDENCY_DSL_MATCHER.matches(methodInvocation) || "project".equals(methodInvocation.getSimpleName())) {
                return null;
            }

            if (!StringUtils.isBlank(configuration) && !methodInvocation.getSimpleName().equals(configuration)) {
                return null;
            }

            // Check if there are multiple literal string arguments (varargs case)
            List<Expression> args = methodInvocation.getArguments();
            if (args.size() <= 1) {
                return null; // Not a multi-dependency
            }

            List<GradleDependency> dependencies = new ArrayList<>();
            GradleProject gradleProject = getGradleProject(cursor);

            // Process each literal string argument as a dependency
            for (Expression arg : args) {
                if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                    String gav = (String) ((J.Literal) arg).getValue();
                    Dependency dep = Dependency.parse(gav);
                    if (dep != null) {
                        // Create a synthetic method invocation wrapping just this one dependency
                        J.MethodInvocation synthetic = methodInvocation.withArguments(Collections.singletonList(arg));
                        Cursor syntheticCursor = new Cursor(cursor.getParent(), synthetic);

                        // Create a resolved dependency for this
                        ResolvedDependency resolvedDependency = ResolvedDependency.builder()
                                .depth(-1)
                                .gav(new ResolvedGroupArtifactVersion(
                                        null,
                                        dep.getGroupId() != null ? dep.getGroupId() : "",
                                        dep.getArtifactId(),
                                        dep.getVersion() != null ? dep.getVersion() : "",
                                        null))
                                .type(dep.getType())
                                .classifier(dep.getClassifier())
                                .requested(dep)
                                .build();

                        dependencies.add(new GradleDependency(syntheticCursor, resolvedDependency));
                    }
                }
            }

            if (dependencies.size() <= 1) {
                return null; // Not really a multi-dependency if only one or zero dependencies found
            }

            return new GradleMultiDependency(cursor, dependencies, groupId, artifactId);
        }

        private boolean withinDependenciesBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencies");
        }
    }
}