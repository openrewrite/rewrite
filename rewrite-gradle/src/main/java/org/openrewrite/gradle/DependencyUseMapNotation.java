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

import org.openrewrite.*;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class DependencyUseMapNotation extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use `Map` notation for Gradle dependency declarations";
    }

    @Override
    public String getDescription() {
        return "In Gradle, dependencies can be expressed as a `String` like `\"groupId:artifactId:version\"`, " +
                "or equivalently as a `Map` like `group: 'groupId', name: 'artifactId', version: 'version'` (groovy) " +
                "or `group = \"groupId\", name = \"artifactId\", version = \"version\"` (kotlin). " +
                "This recipe replaces dependencies represented as `Strings` with an equivalent dependency represented as a `Map`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), Preconditions.or(new GroovyScriptVisitor(), new KotlinScriptVisitor()));
    }

    private static class KotlinScriptVisitor extends KotlinVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

            GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher();

            if (!gradleDependencyMatcher.get(getCursor()).isPresent()) {
                return m;
            }
            m = forBasicString(m, this::mapEntry, J.Assignment::withPrefix, Function.identity());
            return forStringTemplate(m, K.StringTemplate.class, K.StringTemplate::getStrings, K.StringTemplate.Expression.class, K.StringTemplate.Expression::getTree, this::mapEntry, this::mapEntry, J.Assignment::withPrefix, Function.identity());
        }

        private J.Assignment mapEntry(String key, String value) {
            return mapEntry(key,
                    new J.Literal(randomId(), Space.build(" ", emptyList()), Markers.EMPTY, value, "\"" + value + "\"", null, JavaType.Primitive.String));
        }

        private J.Assignment mapEntry(String key, Expression e) {
            return new J.Assignment(
                    randomId(),
                    Space.format(" "),
                    Markers.EMPTY,
                    new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), key, null, null),
                    JLeftPadded.build(e).withBefore(Space.SINGLE_SPACE),
                    null
            );
        }
    }

    private static class GroovyScriptVisitor extends GroovyVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

            GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher();

            if (!gradleDependencyMatcher.get(getCursor()).isPresent()) {
                return m;
            }
            m = forBasicString(m, this::mapEntry, G.MapEntry::withPrefix, this::updateTypeForMapArgument);
            return forStringTemplate(m, G.GString.class, G.GString::getStrings, G.GString.Value.class, G.GString.Value::getTree, this::mapEntry, this::mapEntry, G.MapEntry::withPrefix, this::updateTypeForMapArgument);
        }

        private J.MethodInvocation updateTypeForMapArgument(J.MethodInvocation m) {
            JavaType.Method mtype = m.getMethodType();
            if (mtype == null) {
                return m;
            }
            mtype = mtype.withParameterTypes(singletonList(JavaType.ShallowClass.build("java.util.Map")));
            if (m.getName().getType() != null) {
                m = m.withName(m.getName().withType(mtype));
            }
            return m.withMethodType(mtype);
        }

        private G.MapEntry mapEntry(String key, String value) {
            return mapEntry(key,
                    new J.Literal(randomId(), Space.build(" ", emptyList()), Markers.EMPTY, value, "'" + value + "'", null, JavaType.Primitive.String));
        }

        private G.MapEntry mapEntry(String key, Expression e) {
            return new G.MapEntry(
                    randomId(),
                    Space.format(" "),
                    Markers.EMPTY,
                    JRightPadded.build(new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), key, null, null)),
                    e,
                    null
            );
        }
    }

    private static <T extends Expression> J.MethodInvocation forBasicString(J.MethodInvocation m,
                                                                            BiFunction<String, String, T> mapper,
                                                                            BiFunction<T, Space, T> firstItemPrefixer,
                                                                            Function<J.MethodInvocation, J.MethodInvocation> typeAddition) {
        Expression e = m.getArguments().get(0);
        if (!(e instanceof J.Literal)) {
            return m;
        }
        J.Literal arg = (J.Literal) e;
        if (arg.getType() != JavaType.Primitive.String) {
            return m;
        }
        String dependencyString = (String) arg.getValue();
        if (dependencyString == null) {
            return m;
        }
        Dependency dependency = DependencyNotation.parse(dependencyString);
        if (dependency == null) {
            return m;
        }
        List<Expression> arguments = new ArrayList<>();
        arguments.add(firstItemPrefixer.apply(mapper.apply("group", dependency.getGroupId())
                .withMarkers(arg.getMarkers()), arg.getPrefix()));
        arguments.add(mapper.apply("name", dependency.getArtifactId())
                .withMarkers(arg.getMarkers()));
        if (dependency.getVersion() != null) {
            arguments.add(mapper.apply("version", dependency.getVersion())
                    .withMarkers(arg.getMarkers()));
        }
        if (dependency.getClassifier() != null) {
            arguments.add(mapper.apply("classifier", dependency.getClassifier())
                    .withMarkers(arg.getMarkers()));
        }
        if (dependency.getType() != null) {
            arguments.add(mapper.apply("ext", dependency.getType())
                    .withMarkers(arg.getMarkers()));
        }

        Expression lastArg = m.getArguments().get(m.getArguments().size() - 1);
        if (lastArg instanceof J.Lambda) {
            m = m.withArguments(ListUtils.concat(arguments, lastArg));
        } else {
            m = m.withArguments(arguments);
        }

        return typeAddition.apply(m);
    }

    private static <T extends Expression, P extends J, R extends Expression> J.MethodInvocation forStringTemplate(J.MethodInvocation m,
                                                                                                     Class<T> type,
                                                                                                     Function<T, List<J>> partsExtractor,
                                                                                                     Class<P> partsType,
                                                                                                     Function<P, Tree> getTreeFromTemplatePart,
                                                                                                     BiFunction<String, String, R> mapper,
                                                                                                     BiFunction<String, Expression, R> expressionMapper,
                                                                                                     BiFunction<R, Space, R> firstItemPrefixer,
                                                                                                     Function<J.MethodInvocation, J.MethodInvocation> typeAddition) {
        Expression e = m.getArguments().get(0);
        if (!(type.isInstance(e))) {
            return m;
        }
        T template = type.cast(e);
        // Supporting all possible interpolations is impossible
        // Supporting all probable interpolations is difficult
        // This focuses on the most common case: When only the version number is interpolated
        List<J> parts = partsExtractor.apply(template);
        if (parts.size() != 2 || !(parts.get(0) instanceof J.Literal) || !(partsType.isInstance(parts.get(1)))) {
            return m;
        }
        J.Literal arg1 = (J.Literal) parts.get(0);
        if (arg1.getType() != JavaType.Primitive.String || arg1.getValue() == null) {
            return m;
        }
        String[] ga = ((String) arg1.getValue()).split(":");
        if (ga.length != 2) {
            return m;
        }
        P arg2 = partsType.cast(parts.get(1));
        Tree tree = getTreeFromTemplatePart.apply(arg2);
        if (!(tree instanceof Expression)) {
            return m;
        }
        R groupEntry = firstItemPrefixer.apply(mapper.apply("group", ga[0]).withMarkers(e.getMarkers()), e.getPrefix());
        R artifactEntry = mapper.apply("name", ga[1]).withMarkers(e.getMarkers());
        R versionEntry = expressionMapper.apply("version", ((Expression) tree).withPrefix(Space.SINGLE_SPACE)).withMarkers(e.getMarkers());

        Expression lastArg = m.getArguments().get(m.getArguments().size() - 1);
        if (lastArg instanceof J.Lambda) {
            m = m.withArguments(Arrays.asList(groupEntry, artifactEntry, versionEntry, lastArg));
        } else {
            m = m.withArguments(Arrays.asList(groupEntry, artifactEntry, versionEntry));
        }

        return typeAddition.apply(m);
    }
}
