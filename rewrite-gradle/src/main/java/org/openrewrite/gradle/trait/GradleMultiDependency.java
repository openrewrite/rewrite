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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import static java.util.Collections.singletonList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.openrewrite.gradle.trait.GradleDependency.isDependencyDeclaration;

/**
 * Represents one or more Gradle dependencies declared in a single method invocation.
 * Gradle's groovy DLS allows invocations like:
 * For varargs: implementation('g:a:1.0', 'g:a:2.0', files('lib/local.jar'))
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
     * Whether the call declares several dependencies, read the way Gradle's Groovy DSL reads its arguments.
     * With exactly two arguments, Gradle applies a closure in second place as configuration.
     * A variable in second place may hold such a closure.
     * At least one argument must be a string or map notation, so that a dependency written as map entries stays one.
     */
    private static boolean declaresSeveralDependencies(Cursor methodCursor) {
        Object maybeMi = methodCursor.getValue();
        if (!(maybeMi instanceof J.MethodInvocation) || methodCursor.firstEnclosing(K.CompilationUnit.class) != null) {
            return false;
        }
        List<Expression> arguments = ((J.MethodInvocation) maybeMi).getArguments();
        if (arguments.size() < 2) {
            return false;
        }
        if (arguments.size() == 2) {
            Expression second = arguments.get(1);
            if (second instanceof J.Lambda || second instanceof J.Identifier) {
                return false;
            }
        }
        return arguments.stream().anyMatch(it -> isNotation(it, methodCursor));
    }

    private static boolean isNotation(Expression argument, Cursor cursor) {
        if (argument instanceof G.MapLiteral) {
            return true;
        }
        if (argument instanceof J.Literal) {
            Object value = ((J.Literal) argument).getValue();
            return value instanceof String && DependencyNotation.parse((String) value) != null;
        }
        if (argument instanceof G.GString) {
            String printed = argument.withPrefix(Space.EMPTY).print(cursor);
            return DependencyNotation.parse(printed.substring(1, printed.length() - 1)) != null;
        }
        return false;
    }

    /**
     * Maps a transformation function over each GradleDependency in this multi-dependency
     * that matches the provided DependencyMatcher.
     * Returns a new J.MethodInvocation with updated arguments if any dependencies changed.
     * Of each call the mapper returns, only the dependency's argument and the call's markers are kept.
     *
     * @param mapper Function to transform each matching GradleDependency, or to remove it by returning null
     * @return The updated J.MethodInvocation if any changes were made, the original if not,
     * or null if every dependency was removed
     */
    public J.@Nullable MethodInvocation map(Function<GradleDependency, J.@Nullable MethodInvocation> mapper) {
        if (isVarargs()) {
            J.MethodInvocation m = getTree();
            List<JRightPadded<Expression>> arguments = m.getPadding().getArguments().getPadding().getElements();
            // Marker edits pass from one notation's synthetic call to the next, then onto the real call
            AtomicReference<Markers> markers = new AtomicReference<>(m.getMarkers());
            List<JRightPadded<Expression>> mapped = ListUtils.map(arguments, argument -> {
                // Make a synthetic GradleDependency representing wrapping a single dependency notation
                J.MethodInvocation single = m.withMarkers(markers.get())
                        .withArguments(singletonList(argument.getElement()));
                Optional<GradleDependency> dep = new GradleDependency.Matcher()
                        .matcher(matcher)
                        .get(new Cursor(getCursor().getParent(), single));
                if (dep.isPresent()) {
                    J.MethodInvocation result = mapper.apply(dep.get());
                    if (result == null) {
                        return null;
                    }
                    markers.set(result.getMarkers());
                    return argument.withElement(result.getArguments().get(0));
                }
                return argument;
            });
            if (mapped.isEmpty()) {
                return null;
            }
            // The argument that becomes first takes the original first argument's whitespace and keeps its own comments.
            // The argument that becomes last takes the original last argument's trailing space.
            String firstWhitespace = arguments.get(0).getElement().getPrefix().getWhitespace();
            mapped = ListUtils.mapFirst(mapped, a -> a.withElement(a.getElement().withPrefix(a.getElement().getPrefix().withWhitespace(firstWhitespace))));
            mapped = ListUtils.mapLast(mapped, a -> a.withAfter(arguments.get(arguments.size() - 1).getAfter()));
            return m.getPadding().withArguments(m.getPadding().getArguments().getPadding().withElements(mapped))
                    .withMarkers(markers.get());
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

            return new GradleMultiDependency(cursor, matcher, declaresSeveralDependencies(cursor));
        }
    }
}
