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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents one or more Gradle dependencies declared in a single method invocation.
 * Gradle's groovy DLS allows invocations like:
 * For varargs: implementation('g:a:1.0', 'g:a:2.0', 'dep3:3.0')
 * There is not a corresponding dependency form in the kotlin DSL.
 */
@Value
public class GradleMultiDependency implements Trait<J.MethodInvocation> {
    Cursor cursor;

    /**
     * Optional filters that were used when matching this multi-dependency.
     * When present, the map() method will only apply transformations to dependencies
     * that match these filters.
     */
    @Nullable DependencyMatcher matcher;

    @Getter
    boolean varargs;

    /**
     * Gets the configuration name for these dependencies.
     * For example, "implementation", "testImplementation", "api", etc.
     *
     * @return The configuration name
     */
    public String getConfigurationName() {
        return getTree().getSimpleName();
    }

    /**
     * Check if the cursor points at a method invocation that has multiple arguments which can be interpreted as Gradle's dependency string notation.
     * This is useful to differentiate between the Gradle Groovy DSL's varargs method and the Gradle Kotlin DSL which has
     * dependency methods which accept a sequence of strings representing the individual part of GAV coordinates.
     */
    private static boolean methodArgumentsContainMultipleDependencyNotations(Cursor methodCursor) {
        Object maybeMi = methodCursor.getValue();
        if(!(maybeMi instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) maybeMi;
        return m.getArguments().size() > 1 && m.getArguments().stream()
                .filter(it -> Dependency.parse(it.print(methodCursor).trim()) != null)
                .limit(2)
                .count() > 1;
    }

    /**
     * Maps a transformation function over each GradleDependency in this multi-dependency
     * that matches the provided DependencyMatcher.
     * Returns a new J.MethodInvocation with updated arguments if any dependencies changed.
     *
     * @param mapper Function to transform each matching GradleDependency
     * @return The updated J.MethodInvocation if any changes were made, or the original if not
     */
    public GradleMultiDependency map(Function<GradleDependency, GradleDependency> mapper) {
        J.MethodInvocation m = cursor.getValue();
        if (isVarargs())  {

        } else {
            new GradleDependency.Matcher()
                    .get(cursor)
                    .map(mapper)
                    .get()
        }
        return this;
    }

    public static class Matcher extends GradleTraitMatcher<GradleMultiDependency> {
        private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

        @Nullable
        protected String configuration;

        @Nullable
        protected DependencyMatcher matcher;

        public Matcher(@Nullable DependencyMatcher matcher) {
            this.matcher = matcher;
        }

        public Matcher configuration(@Nullable String configuration) {
            this.configuration = configuration;
            return this;
        }

        public Matcher groupId(@Nullable String groupPattern) {
            if(matcher == null) {
                matcher = new DependencyMatcher(groupPattern, null, null);
            } else {
                matcher = matcher.withGroupPattern(groupPattern);
            }
            return this;
        }

        public Matcher artifactId(@Nullable String artifactPattern) {
            if(matcher == null) {
                matcher = new DependencyMatcher(null, artifactPattern, null);
            } else {
                matcher = matcher.withArtifactPattern(artifactPattern);
            }
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
            List<Expression> args = methodInvocation.getArguments();

            // Check if this is a varargs invocation (multiple literal strings)
            methodArgumentsContainMultipleDependencyNotations(cursor);
            if (args.stream().filter(it -> Dependency.parse(it.print(cursor)) != null).count() > 1) {
                return testVarargs(cursor, methodInvocation, args);
            }

            return new GradleMultiDependency(cursor, matcher, );
        }

        private @Nullable GradleMultiDependency testVarargs(Cursor cursor, J.MethodInvocation methodInvocation, List<Expression> args) {
            if (!withinDependenciesBlock(cursor)) {
                return null;
            }

            if (withinDependencyConstraintsBlock(cursor)) {
                return null;
            }

            if (!DEPENDENCY_DSL_MATCHER.matches(methodInvocation) || "project".equals(methodInvocation.getSimpleName())) {
                return null;
            }

            if (!StringUtils.isBlank(configuration) && !methodInvocation.getSimpleName().equals(configuration)) {
                return null;
            }

            // Parse each literal string or GString argument
            List<Dependency> parsedDependencies = new ArrayList<>();
            for (Expression arg : args) {
                Dependency dep = null;
                if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                    String gav = (String) ((J.Literal) arg).getValue();
                    dep = Dependency.parse(gav);
                } else if (arg instanceof G.GString) {
                    // Handle GString notation: "group:artifact:$version"
                    G.GString gstring = (G.GString) arg;
                    List<J> strings = gstring.getStrings();
                    if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                        String gav = (String) ((J.Literal) strings.get(0)).getValue();
                        dep = Dependency.parse(gav);
                    }
                }
                if (dep != null) {
                    parsedDependencies.add(dep);
                }
            }

            if (parsedDependencies.size() <= 1) {
                return null; // Not really a varargs if only one or zero dependencies
            }

            return new GradleMultiDependency(cursor, matcher);
        }

        private boolean withinDependenciesBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencies");
        }

        private boolean withinDependencyConstraintsBlock(Cursor cursor) {
            return withinBlock(cursor, "constraints") && withinDependenciesBlock(cursor);
        }
    }
}