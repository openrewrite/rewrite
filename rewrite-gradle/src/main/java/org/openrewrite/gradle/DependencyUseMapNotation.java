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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Arrays;

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
                "or equivalently as a `Map` like `group: 'groupId', name: 'artifactId', version: 'version'`. " +
                "This recipe replaces dependencies represented as `Strings` with an equivalent dependency represented as a `Map`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher dependencyDsl = MethodMatcher.create("DependencyHandlerSpec *(..)");
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, context);
                if (!dependencyDsl.matches(m)) {
                    return m;
                }
                m = forBasicString(m);
                m = forGString(m);
                return m;
            }

            private J.MethodInvocation forBasicString(J.MethodInvocation m) {
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
                String[] gav = dependencyString.split(":");
                G.MapEntry groupEntry = mapEntry("group", gav[0])
                        .withMarkers(arg.getMarkers())
                        .withPrefix(arg.getPrefix());
                G.MapEntry artifactEntry = mapEntry("name", gav[1])
                        .withMarkers(arg.getMarkers());
                G.MapEntry versionEntry = mapEntry("version", gav[2])
                        .withMarkers(arg.getMarkers());
                m = m.withArguments(Arrays.asList(groupEntry, artifactEntry, versionEntry));

                return updateTypeForMapArgument(m);
            }

            private J.MethodInvocation forGString(J.MethodInvocation m) {
                Expression e = m.getArguments().get(0);
                if (!(e instanceof G.GString)) {
                    return m;
                }
                G.GString g = (G.GString)e;
                // Supporting all possible GString interpolations is impossible
                // Supporting all probable GString interpolations is difficult
                // This focuses on the most common case: When only the version number is interpolated
                if (g.getStrings().size() != 2 || !(g.getStrings().get(0) instanceof J.Literal)
                        || !(g.getStrings().get(1) instanceof G.GString.Value)) {
                    return m;
                }
                J.Literal arg1 = (J.Literal)g.getStrings().get(0);
                if (arg1.getType() != JavaType.Primitive.String || arg1.getValue() == null) {
                    return m;
                }
                String[] ga = ((String) arg1.getValue()).split(":");
                if (ga.length != 2) {
                    return m;
                }
                G.GString.Value arg2 = (G.GString.Value)g.getStrings().get(1);
                if (!(arg2.getTree() instanceof Expression)) {
                    return m;
                }
                G.MapEntry groupEntry = mapEntry("group", ga[0])
                        .withMarkers(e.getMarkers())
                        .withPrefix(e.getPrefix());
                G.MapEntry artifactEntry = mapEntry("name", ga[1])
                        .withMarkers(e.getMarkers());
                G.MapEntry versionEntry = mapEntry("version", (Expression) arg2.getTree().withPrefix(Space.format(" ")))
                        .withMarkers(e.getMarkers());
                m = m.withArguments(Arrays.asList(groupEntry, artifactEntry, versionEntry));

                return updateTypeForMapArgument(m);
            }

            private J.MethodInvocation updateTypeForMapArgument(J.MethodInvocation m) {
                JavaType.Method mtype = m.getMethodType();
                if (mtype == null) {
                    return m;
                }
                mtype = mtype.withParameterTypes(singletonList(JavaType.ShallowClass.build("java.util.Map")));
                return m.withMethodType(mtype);
            }
        };
    }

    private static G.MapEntry mapEntry(String key, String value) {
        return mapEntry(key,
                new J.Literal(randomId(), Space.build(" ", emptyList()), Markers.EMPTY, value, "'" + value + "'", null, JavaType.Primitive.String));
    }

    private static G.MapEntry mapEntry(String key, Expression e) {
        return new G.MapEntry(
                randomId(),
                Space.format(" "),
                Markers.EMPTY,
                JRightPadded.build(new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, key, null, null)),
                e,
                null
        );
    }
}
