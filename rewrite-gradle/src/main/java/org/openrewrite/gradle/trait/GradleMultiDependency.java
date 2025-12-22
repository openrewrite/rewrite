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
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import static java.util.Collections.singletonList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.openrewrite.gradle.trait.GradleDependency.isDependencyDeclaration;

/**
 * Represents one or more Gradle dependencies declared in a single method invocation.
 * Gradle's groovy DLS allows invocations like:
 * For varargs: implementation('g:a:1.0', 'g:a:2.0', 'dep3:3.0')
 * There is not a corresponding dependency form in the kotlin DSL.
 */
@Value
public class GradleMultiDependency implements Trait<J.MethodInvocation> {
    @With
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
    @SuppressWarnings("unused")
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
        if (!(maybeMi instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) maybeMi;
        long count = m.getArguments().stream()
                .filter(it -> {
                    String printed = it.print(methodCursor).trim();
                    // Strip surrounding quotes if present (from Groovy/Kotlin string literals)
                    if ((printed.startsWith("'") && printed.endsWith("'")) ||
                        (printed.startsWith("\"") && printed.endsWith("\""))) {
                        printed = printed.substring(1, printed.length() - 1);
                    }
                    return DependencyNotation.parse(printed) != null;
                })
                .limit(2)
                .count();
        return m.getArguments().size() > 1 && count > 1;
    }

    /**
     * Maps a transformation function over each GradleDependency in this multi-dependency
     * that matches the provided DependencyMatcher.
     * Returns a new J.MethodInvocation with updated arguments if any dependencies changed.
     *
     * @param mapper Function to transform each matching GradleDependency
     * @return The updated J.MethodInvocation if any changes were made, or the original if not
     */
    public J.MethodInvocation map(Function<GradleDependency, J.MethodInvocation> mapper) {
        if (isVarargs()) {
            return getTree().withArguments(
                ListUtils.map(getTree().getArguments(), argument -> {
                    // Make a synthetic GradleDependency representing wrapping a single dependency notation
                    J.MethodInvocation m = getTree().withArguments(singletonList(argument));
                    Optional<GradleDependency> dep = new GradleDependency.Matcher()
                            .matcher(matcher)
                            .get(new Cursor(getCursor().getParent(), m));
                    if (dep.isPresent()) {
                        J.MethodInvocation result = mapper.apply(dep.get());
                        return result.getArguments().get(0);
                    }
                    return argument;
                })
            );
        } else {
            Optional<GradleDependency> dep = new GradleDependency.Matcher()
                    .matcher(matcher)
                    .get(cursor);
            if (dep.isPresent()) {
                return mapper.apply(dep.get());
            }
        }
        return getTree();
    }

    public void forEach(Consumer<GradleDependency> consumer) {
        if (isVarargs()) {
            for (Expression argument : getTree().getArguments()) {
                J.MethodInvocation m = getTree().withArguments(singletonList(argument));
                new GradleDependency.Matcher()
                        .matcher(matcher)
                        .get(new Cursor(getCursor().getParent(), m))
                        .ifPresent(consumer);
            }
        } else {
            new GradleDependency.Matcher()
                    .matcher(matcher)
                    .get(cursor)
                    .ifPresent(consumer);
        }
    }

    public static Matcher matcher() {
        return new Matcher();
    }

    public static class Matcher extends GradleTraitMatcher<GradleMultiDependency> {

        @Nullable
        protected String configuration;

        @Nullable
        protected DependencyMatcher matcher;

        public Matcher matcher(@Nullable DependencyMatcher matcher) {
            this.matcher = matcher;
            return this;
        }

        public Matcher configuration(@Nullable String configuration) {
            this.configuration = configuration;
            return this;
        }

        public Matcher groupId(@Nullable String groupPattern) {
            if (matcher == null) {
                matcher = new DependencyMatcher(groupPattern, null, null);
            } else {
                matcher = matcher.withGroupPattern(groupPattern);
            }
            return this;
        }

        public Matcher artifactId(@Nullable String artifactPattern) {
            if (matcher == null) {
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
            if (!isDependencyDeclaration(cursor)) {
                return null;
            }

            J.MethodInvocation methodInvocation = cursor.getValue();
            if (!StringUtils.isBlank(configuration) && !methodInvocation.getSimpleName().equals(configuration)) {
                return null;
            }

            return new GradleMultiDependency(cursor, matcher, methodArgumentsContainMultipleDependencyNotations(cursor));
        }
    }
}
