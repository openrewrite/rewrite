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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.internal.Dependency;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class DependencyUseStringNotation extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use `String` notation for Gradle dependency declarations";
    }

    @Override
    public String getDescription() {
        return "In Gradle, dependencies can be expressed as a `String` like `\"groupId:artifactId:version\"`, " +
                "or equivalently as a `Map` like `group: 'groupId', name: 'artifactId', version: 'version'`. " +
                "This recipe replaces dependencies represented as `Maps` with an equivalent dependency represented as a `String`, " +
                "as recommended per the [Gradle best practices for dependencies to use a single GAV](https://docs.gradle.org/8.14.2/userguide/best_practices_dependencies.html#single-gav-string).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher();

                if (!gradleDependencyMatcher.get(getCursor()).isPresent()) {
                    return m;
                }

                if (m.getArguments().isEmpty()) {
                    return m;
                }

                Map<String, Expression> mapNotation = new HashMap<>();
                if (m.getArguments().get(0) instanceof G.MapLiteral) {
                    G.MapLiteral arg = (G.MapLiteral) m.getArguments().get(0);

                    for (G.MapEntry entry : arg.getElements()) {
                        if (entry.getKey() instanceof J.Literal) {
                            J.Literal key = (J.Literal) entry.getKey();
                            if (key.getType() == JavaType.Primitive.String) {
                                mapNotation.put((String) key.getValue(), entry.getValue());
                            }
                        }
                    }

                    J.Literal stringNotation = toLiteral(arg.getPrefix(), arg.getMarkers(), mapNotation);
                    if (stringNotation == null) {
                        return m;
                    }

                    Expression lastArg = m.getArguments().get(m.getArguments().size() - 1);
                    if (lastArg instanceof J.Lambda) {
                        m = m.withArguments(Arrays.asList(stringNotation, lastArg));
                    } else {
                        m = m.withArguments(singletonList(stringNotation));
                    }
                } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                    G.MapEntry firstEntry = (G.MapEntry) m.getArguments().get(0);
                    Space prefix = firstEntry.getPrefix();
                    Markers markers = firstEntry.getMarkers();

                    for (Expression e : m.getArguments()) {
                        if (e instanceof G.MapEntry) {
                            G.MapEntry entry = (G.MapEntry) e;
                            if (entry.getKey() instanceof J.Literal) {
                                J.Literal key = (J.Literal) entry.getKey();
                                if (key.getType() == JavaType.Primitive.String) {
                                    mapNotation.put((String) key.getValue(), entry.getValue());
                                }
                            }
                        }
                    }

                    J.Literal stringNotation = toLiteral(prefix, markers, mapNotation);
                    if (stringNotation == null) {
                        return m;
                    }

                    Expression lastArg = m.getArguments().get(m.getArguments().size() - 1);
                    if (lastArg instanceof J.Lambda) {
                        m = m.withArguments(Arrays.asList(stringNotation, lastArg));
                    } else {
                        m = m.withArguments(singletonList(stringNotation));
                    }
                } else if (m.getArguments().get(0) instanceof J.Assignment) {
                    J.Assignment firstEntry = (J.Assignment) m.getArguments().get(0);
                    Space prefix = firstEntry.getPrefix();
                    Markers markers = firstEntry.getMarkers();

                    for (Expression e : m.getArguments()) {
                        if (e instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) e;
                            if (assignment.getVariable() instanceof J.Identifier) {
                                J.Identifier key = (J.Identifier) assignment.getVariable();
                                mapNotation.put(key.getSimpleName(), assignment.getAssignment());
                            }
                        }
                    }

                    J.Literal stringNotation = toLiteral(prefix, markers, mapNotation);
                    if (stringNotation == null) {
                        return m;
                    }

                    Expression lastArg = m.getArguments().get(m.getArguments().size() - 1);
                    if (lastArg instanceof J.Lambda) {
                        m = m.withArguments(Arrays.asList(stringNotation, lastArg));
                    } else {
                        m = m.withArguments(singletonList(stringNotation));
                    }
                }

                return m;
            }

            private J.@Nullable Literal toLiteral(Space prefix, Markers markers, Map<String, Expression> mapNotation) {
                // Name is the only required key in a dependency map.
                if (mapNotation.containsKey("name")) {
                    String group = coerceToStringNotation(mapNotation.get("group"));
                    String name = coerceToStringNotation(mapNotation.get("name"));
                    String version = coerceToStringNotation(mapNotation.get("version"));
                    String classifier = coerceToStringNotation(mapNotation.get("classifier"));
                    String extension = coerceToStringNotation(mapNotation.get("ext"));

                    Dependency dependency = new Dependency(group, name, version, classifier, extension);
                    String stringNotation = dependency.toStringNotation();

                    return new J.Literal(randomId(), prefix, markers, stringNotation, "\"" + stringNotation + "\"", emptyList(), JavaType.Primitive.String);
                }

                return null;
            }

            private @Nullable String coerceToStringNotation(Expression expression) {
                if (expression instanceof J.Literal) {
                    return (String) ((J.Literal) expression).getValue();
                } else if (expression instanceof J.Identifier) {
                    return "$" + ((J.Identifier) expression).getSimpleName();
                } else if (expression instanceof G.GString) {
                    List<J> str = ((G.GString) expression).getStrings();
                    StringBuilder sb = new StringBuilder();
                    for (J valuePart : str) {
                        if (valuePart instanceof Expression) {
                            sb.append(coerceToStringNotation((Expression) valuePart));
                        } else if (valuePart instanceof G.GString.Value) {
                            J tree = ((G.GString.Value) valuePart).getTree();
                            if (tree instanceof Expression) {
                                sb.append(coerceToStringNotation((Expression) tree));
                            }
                            //Can it be something else? If so, what?
                        }
                    }
                    return sb.toString();
                }
                return null;
            }
        });
    }
}
