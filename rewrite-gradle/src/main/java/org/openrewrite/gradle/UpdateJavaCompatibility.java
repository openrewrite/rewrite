/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeJavaCompatibility extends Recipe {
    @Option(displayName = "New version",
            description = "The Java version to update source compatibility to. All allowed variations of Gradle's `org.gradle.api.JavaVersion` are allowed. " +
                          "This means that we accept versions in the form of doubles (ex. 1.8, 1.11), whole numbers (ex. 8, 11), strings (ex. \"1.8\", \"8\", '1.11', '11'), " +
                          "and `org.gradle.api.JavaVersion` enum values (ex. `VERSION_1_8`, `VERSION_11`, `JavaVersion.VERSION_1_8`, `JavaVersion.VERSION_11`).",
            example = "11")
    String newVersion;

    @Option(displayName = "Compatibility Type",
            description = "The compatibility type to change",
            valid = {"source", "target"})
    String compatibilityType;

    @Override
    public String getDisplayName() {
        return "Change Gradle project Java compatibility";
    }

    @Override
    public String getDescription() {
        return "Find and updates the Java compatibility for the Gradle project.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyVisitor<ExecutionContext>() {
            final MethodMatcher sourceCompatibilityDsl = new MethodMatcher("RewriteGradleProject setSourceCompatibility(..)");
            final MethodMatcher targetCompatibilityDsl = new MethodMatcher("RewriteGradleProject setTargetCompatibility(..)");

            @Override
            public J visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                J.Assignment a = (J.Assignment) super.visitAssignment(assignment, executionContext);

                if (a.getVariable() instanceof J.Identifier) {
                    J.Identifier var = (J.Identifier) a.getVariable();
                    if (!(compatibilityType + "Compatibility").equals(var.getSimpleName())) {
                        return a;
                    }
                } else if (a.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) a.getVariable();
                    if (!(compatibilityType + "Compatibility").equals(fieldAccess.getSimpleName())) {
                        return a;
                    }
                } else {
                    return a;
                }

                int newMajor = getMajorVersion(newVersion);
                if (newMajor == -1) {
                    return a;
                }
                Class<?> requestedType = determineRequestedType(newVersion);

                int currentMajor = getMajorVersion(a.getAssignment());
                if (currentMajor != newMajor) {
                    a = a.withAssignment(changeExpression(a.getAssignment(), requestedType, newVersion));
                }

                return a;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (("source".equals(compatibilityType) && !sourceCompatibilityDsl.matches(m)) || ("target".equals(compatibilityType) && !targetCompatibilityDsl.matches(m))) {
                    return m;
                }

                int newMajor = getMajorVersion(newVersion);
                if (newMajor == -1) {
                    return m;
                }
                Class<?> requestedType = determineRequestedType(newVersion);

                int currentMajor = getMajorVersion(m.getArguments().get(0));
                if (currentMajor != newMajor) {
                    m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> changeExpression(arg, requestedType, newVersion)));
                }

                return m;
            }

            private int getMajorVersion(String version) {
                try {
                    return Integer.parseInt(normalize(version));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }

            private int getMajorVersion(Expression expression) {
                if (expression instanceof J.Literal) {
                    J.Literal argument = (J.Literal) expression;
                    JavaType.Primitive type = argument.getType();
                    if (type == JavaType.Primitive.String) {
                        return getMajorVersion((String) argument.getValue());
                    } else if (type == JavaType.Primitive.Int) {
                        return (int) argument.getValue();
                    } else if (type == JavaType.Primitive.Double) {
                        return getMajorVersion(argument.getValue().toString());
                    }
                } else if (expression instanceof J.FieldAccess) {
                    J.FieldAccess field = (J.FieldAccess) expression;
                    J.Identifier identifier = field.getName();
                    return getMajorVersion(identifier.getSimpleName());
                }

                return -1;
            }

            private String normalize(String version) {
                if (version.contains("\"") || version.contains("'")) {
                    version = version.replace("\"", "").replace("'", "");
                }

                if (!version.contains(".") && !version.contains("_")) {
                    return version;
                }

                if (version.contains("_")) {
                    String removePrefix = version.substring(version.indexOf("_") + 1);
                    if (removePrefix.startsWith("1_")) {
                        return removePrefix.substring(removePrefix.indexOf("_") + 1);
                    } else {
                        return removePrefix;
                    }
                } else {
                    return version.substring(version.indexOf(".") + 1);
                }
            }

            private Class<?> determineRequestedType(String value) {
                if (value.contains("\"") || value.contains("'")) {
                    return String.class;
                }

                if (value.startsWith("JavaVersion.") || value.startsWith("VERSION_")) {
                    return Enum.class;
                }

                if (value.contains(".")) {
                    return double.class;
                }

                return int.class;
            }

            private Expression changeExpression(Expression expression, Class<?> requestedType, String requestedValue) {
                if (expression instanceof J.Literal) {
                    J.Literal literal = (J.Literal) expression;
                    if (String.class.equals(requestedType)) {
                        expression = literal.withType(JavaType.Primitive.String).withValue(requestedValue).withValueSource(requestedValue);
                    } else if (Enum.class.equals(requestedType)) {
                        String name = requestedValue.substring(requestedValue.indexOf(".") + 1);
                        expression = new J.FieldAccess(
                                randomId(),
                                literal.getPrefix(),
                                literal.getMarkers(),
                                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, "JavaVersion", JavaType.ShallowClass.build("org.gradle.api.JavaVersion"), null),
                                new JLeftPadded<>(Space.EMPTY, new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, name, null, null), Markers.EMPTY),
                                JavaType.ShallowClass.build("org.gradle.api.JavaVersion")
                        );
                    } else if (double.class.equals(requestedType)) {
                        double doubleValue = Double.parseDouble(requestedValue);
                        expression = literal.withType(JavaType.Primitive.Double).withValue(doubleValue).withValueSource(requestedValue);
                    } else if (int.class.equals(requestedType)) {
                        int intValue = Integer.parseInt(requestedValue);
                        expression = literal.withType(JavaType.Primitive.Int).withValue(intValue).withValueSource(requestedValue);
                    }
                } else if (expression instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                    if (String.class.equals(requestedType)) {
                        expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), requestedValue, requestedValue, Collections.emptyList(), JavaType.Primitive.String);
                    } else if (Enum.class.equals(requestedType)) {
                        String name = requestedValue.substring(requestedValue.indexOf(".") + 1);
                        expression = fieldAccess.withName(fieldAccess.getName().withSimpleName(name));
                    } else if (double.class.equals(requestedType)) {
                        double doubleValue = Double.parseDouble(requestedValue);
                        expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), doubleValue, requestedValue, Collections.emptyList(), JavaType.Primitive.Double);
                    } else if (int.class.equals(requestedType)) {
                        int intValue = Integer.parseInt(requestedValue);
                        expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), intValue, requestedValue, Collections.emptyList(), JavaType.Primitive.Int);
                    }
                }

                return expression;
            }
        };
    }
}
