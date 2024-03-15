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
import org.openrewrite.*;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateJavaCompatibility extends Recipe {
    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    @Option(displayName = "Compatibility type",
            description = "The compatibility type to change",
            valid = {"source", "target"},
            required = false)
    @Nullable
    CompatibilityType compatibilityType;

    @Option(displayName = "Declaration style",
            description = "The desired style to write the new version as when being written to the `sourceCompatibility` " +
                    "or `targetCompatibility` variables. Default, match current source style. " +
                    "(ex. Enum: `JavaVersion.VERSION_11`, Number: 11, or String: \"11\")",
            valid = {"Enum", "Number", "String"},
            required = false)
    @Nullable
    DeclarationStyle declarationStyle;

    @Option(displayName = "Allow downgrade",
            description = "Allow downgrading the Java version.",
            required = false)
    @Nullable
    Boolean allowDowngrade;

    @Option(displayName = "Add compatibility type if missing",
            description = "Adds the specified compatibility type if one is not found.",
            required = false)
    @Nullable
    Boolean addIfMissing;

    private static final String SOURCE_COMPATIBILITY_FOUND = "SOURCE_COMPATIBILITY_FOUND";
    private static final String TARGET_COMPATIBILITY_FOUND = "TARGET_COMPATIBILITY_FOUND";

    @Override
    public String getDisplayName() {
        return "Update Gradle project Java compatibility";
    }

    @Override
    public String getDescription() {
        return "Find and updates the Java compatibility for the Gradle project.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("version", "Version must be > 0.", version, v -> v > 0));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {
            final MethodMatcher sourceCompatibilityDsl = new MethodMatcher("RewriteGradleProject setSourceCompatibility(..)");
            final MethodMatcher targetCompatibilityDsl = new MethodMatcher("RewriteGradleProject setTargetCompatibility(..)");
            final MethodMatcher javaLanguageVersionMatcher = new MethodMatcher("org.gradle.jvm.toolchain.JavaLanguageVersion of(int)");
            final MethodMatcher javaVersionToVersionMatcher = new MethodMatcher("org.gradle.api.JavaVersion toVersion(..)");

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext executionContext) {
                G.CompilationUnit c = (G.CompilationUnit) super.visitCompilationUnit(cu, executionContext);
                if (getCursor().pollMessage(SOURCE_COMPATIBILITY_FOUND) == null) {
                    c = addCompatibilityTypeToSourceFile(c, "source");
                }
                if (getCursor().pollMessage(TARGET_COMPATIBILITY_FOUND) == null) {
                    c = addCompatibilityTypeToSourceFile(c, "target");
                }
                return c;
            }

            @Override
            public J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);

                if (a.getVariable() instanceof J.Identifier) {
                    J.Identifier variable = (J.Identifier) a.getVariable();
                    if ("sourceCompatibility".equals(variable.getSimpleName())) {
                        getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, SOURCE_COMPATIBILITY_FOUND, a.getAssignment());
                    }
                    if ("targetCompatibility".equals(variable.getSimpleName())) {
                        getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, TARGET_COMPATIBILITY_FOUND, a.getAssignment());
                    }

                    if (compatibilityType == null) {
                        if (!("sourceCompatibility".equals(variable.getSimpleName()) || "targetCompatibility".equals(variable.getSimpleName()))) {
                            return a;
                        }
                    } else if (!(compatibilityType.toString().toLowerCase() + "Compatibility").equals(variable.getSimpleName())) {
                        return a;
                    }
                } else if (a.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) a.getVariable();
                    if (compatibilityType == null) {
                        if (!("sourceCompatibility".equals(fieldAccess.getSimpleName()) || "targetCompatibility".equals(fieldAccess.getSimpleName()))) {
                            return a;
                        }
                    } else if (!(compatibilityType.toString().toLowerCase() + "Compatibility").equals(fieldAccess.getSimpleName())) {
                        return a;
                    }
                } else {
                    return a;
                }

                DeclarationStyle currentStyle = getCurrentStyle(a.getAssignment());
                int currentMajor = getMajorVersion(a.getAssignment());
                if (shouldUpdateVersion(currentMajor) || shouldUpdateStyle(currentStyle)) {
                    DeclarationStyle actualStyle = declarationStyle == null ? currentStyle : declarationStyle;
                    return a.withAssignment(changeExpression(a.getAssignment(), actualStyle));
                }

                return a;
            }

            private boolean shouldUpdateVersion(int currentMajor) {
                return currentMajor < version || currentMajor > version && Boolean.TRUE.equals(allowDowngrade);
            }

            private boolean shouldUpdateStyle(@Nullable DeclarationStyle currentStyle) {
                return declarationStyle != null && declarationStyle != currentStyle;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if ("sourceCompatibility".equals(m.getSimpleName())) {
                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, SOURCE_COMPATIBILITY_FOUND, true);
                }
                if ("targetCompatibility".equals(m.getSimpleName())) {
                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, TARGET_COMPATIBILITY_FOUND, true);
                }
                if (javaLanguageVersionMatcher.matches(m)) {
                    List<Expression> args = m.getArguments();

                    if (args.size() == 1 && args.get(0) instanceof J.Literal) {
                        J.Literal versionArg = (J.Literal) args.get(0);
                        if (versionArg.getValue() instanceof Integer) {
                            Integer versionNumber = (Integer) versionArg.getValue();
                            if (shouldUpdateVersion(versionNumber)) {
                                return m.withArguments(
                                        Collections.singletonList(versionArg.withValue(version)
                                                .withValueSource(version.toString())));
                            } else {
                                return m;
                            }
                        }
                    }

                    return SearchResult.found(m, "Attempted to update to Java version to " + version
                            + "  but was unsuccessful, please update manually");
                }

                if (sourceCompatibilityDsl.matches(m) || targetCompatibilityDsl.matches(m)) {
                    if (compatibilityType != null && (
                            (compatibilityType == CompatibilityType.source && !sourceCompatibilityDsl.matches(m)) ||
                                    (compatibilityType == CompatibilityType.target && !targetCompatibilityDsl.matches(m)))) {
                        return m;
                    }

                    if (m.getArguments().size() == 1 && (m.getArguments().get(0) instanceof J.Literal || m.getArguments().get(0) instanceof J.FieldAccess)) {
                        DeclarationStyle currentStyle = getCurrentStyle(m.getArguments().get(0));
                        int currentMajor = getMajorVersion(m.getArguments().get(0));
                        if (shouldUpdateVersion(currentMajor) || shouldUpdateStyle(declarationStyle)) {
                            DeclarationStyle actualStyle = declarationStyle == null ? currentStyle : declarationStyle;
                            return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> changeExpression(arg, actualStyle)));
                        } else {
                            return m;
                        }
                    }

                    return SearchResult.found(m, "Attempted to update to Java version to " + version
                            + "  but was unsuccessful, please update manually");
                }

                return m;
            }

            private int getMajorVersion(@Nullable String version) {
                if(version == null) {
                    return -1;
                }
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
                        return (int) requireNonNull(argument.getValue());
                    } else if (type == JavaType.Primitive.Double) {
                        return getMajorVersion(requireNonNull(argument.getValue()).toString());
                    }
                } else if (expression instanceof J.FieldAccess) {
                    J.FieldAccess field = (J.FieldAccess) expression;
                    J.Identifier identifier = field.getName();
                    return getMajorVersion(identifier.getSimpleName());
                } else if (expression instanceof J.MethodInvocation && javaVersionToVersionMatcher.matches((J.MethodInvocation) expression)) {
                    J.MethodInvocation method = (J.MethodInvocation) expression;
                    if (method.getArguments().get(0) instanceof J.Literal) {
                        return getMajorVersion(method.getArguments().get(0));
                    }
                }

                return -1;
            }

            private @Nullable DeclarationStyle getCurrentStyle(Expression expression) {
                if (expression instanceof J.Literal) {
                    J.Literal argument = (J.Literal) expression;
                    JavaType.Primitive type = argument.getType();
                    if (type == JavaType.Primitive.String) {
                        return DeclarationStyle.String;
                    } else if (type == JavaType.Primitive.Int) {
                        return DeclarationStyle.Number;
                    } else if (type == JavaType.Primitive.Double) {
                        return DeclarationStyle.Number;
                    }
                } else if (expression instanceof J.FieldAccess) {
                    return DeclarationStyle.Enum;
                }

                return null;
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

            private Expression changeExpression(Expression expression, @Nullable DeclarationStyle style) {
                if (expression instanceof J.Literal) {
                    J.Literal literal = (J.Literal) expression;
                    if (style == DeclarationStyle.String) {
                        String newVersion = version <= 8 ? "1." + version : String.valueOf(version);
                        if (literal.getType() == JavaType.Primitive.String) {
                            expression = ChangeStringLiteral.withStringValue(literal, newVersion);
                        } else {
                            expression = literal.withType(JavaType.Primitive.String).withValue(newVersion).withValueSource("'" + newVersion + "'");
                        }
                    } else if (style == DeclarationStyle.Enum) {
                        String name = version <= 8 ? "VERSION_1_" + version : "VERSION_" + version;
                        expression = new J.FieldAccess(
                                randomId(),
                                literal.getPrefix(),
                                literal.getMarkers(),
                                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "JavaVersion", JavaType.ShallowClass.build("org.gradle.api.JavaVersion"), null),
                                new JLeftPadded<>(Space.EMPTY, new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null), Markers.EMPTY),
                                JavaType.ShallowClass.build("org.gradle.api.JavaVersion")
                        );
                    } else if (style == DeclarationStyle.Number) {
                        if (version <= 8) {
                            double doubleValue = Double.parseDouble("1." + version);
                            expression = literal.withType(JavaType.Primitive.Double).withValue(doubleValue).withValueSource("1." + version);
                        } else {
                            expression = literal.withType(JavaType.Primitive.Int).withValue(version).withValueSource(String.valueOf(version));
                        }
                    }
                } else if (expression instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                    if (style == DeclarationStyle.String) {
                        String newVersion = version <= 8 ? "1." + version : String.valueOf(version);
                        expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), newVersion, "'" + newVersion + "'", emptyList(), JavaType.Primitive.String);
                    } else if (style == DeclarationStyle.Enum) {
                        String name = version <= 8 ? "VERSION_1_" + version : "VERSION_" + version;
                        expression = fieldAccess.withName(fieldAccess.getName().withSimpleName(name));
                    } else if (style == DeclarationStyle.Number) {
                        if (version <= 8) {
                            double doubleValue = Double.parseDouble("1." + version);
                            expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), doubleValue, String.valueOf(doubleValue), emptyList(), JavaType.Primitive.Double);
                        } else {
                            expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), version, String.valueOf(version), emptyList(), JavaType.Primitive.Int);
                        }
                    }
                } else if (expression instanceof J.MethodInvocation && javaVersionToVersionMatcher.matches((J.MethodInvocation) expression)) {
                    J.MethodInvocation m = (J.MethodInvocation) expression;
                    if (style == null) {
                        expression = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                            if (arg instanceof J.Literal) {
                                if (arg.getType() == JavaType.Primitive.String) {
                                    String newVersion = version <= 8 ? "1." + version : String.valueOf(version);
                                    return ChangeStringLiteral.withStringValue((J.Literal) arg, newVersion);
                                } else if (arg.getType() == JavaType.Primitive.Int) {
                                    return ((J.Literal) arg).withValue(version).withValueSource(String.valueOf(version));
                                } else if (arg.getType() == JavaType.Primitive.Double) {
                                    if (version <= 8) {
                                        double doubleValue = Double.parseDouble("1." + version);
                                        return ((J.Literal) arg).withValue(doubleValue).withValueSource(String.valueOf(doubleValue));
                                    } else {
                                        return ((J.Literal) arg).withType(JavaType.Primitive.Int).withValue(version).withValueSource(String.valueOf(version));
                                    }
                                }
                            }
                            return arg;
                        }));
                    } else if (style == DeclarationStyle.String) {
                        String newVersion = version <= 8 ? "1." + version : String.valueOf(version);
                        expression = new J.Literal(randomId(), m.getPrefix(), m.getMarkers(), newVersion, "'" + newVersion + "'", emptyList(), JavaType.Primitive.String);
                    } else if (style == DeclarationStyle.Enum) {
                        String name = version <= 8 ? "VERSION_1_" + version : "VERSION_" + version;
                        expression = new J.FieldAccess(
                                randomId(),
                                m.getPrefix(),
                                m.getMarkers(),
                                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "JavaVersion", JavaType.ShallowClass.build("org.gradle.api.JavaVersion"), null),
                                new JLeftPadded<>(Space.EMPTY, new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null), Markers.EMPTY),
                                JavaType.ShallowClass.build("org.gradle.api.JavaVersion")
                        );
                    } else if (style == DeclarationStyle.Number) {
                        if (version <= 8) {
                            double doubleValue = Double.parseDouble("1." + version);
                            expression = new J.Literal(randomId(), m.getPrefix(), m.getMarkers(), doubleValue, String.valueOf(doubleValue), emptyList(), JavaType.Primitive.Double);
                        } else {
                            expression = new J.Literal(randomId(), m.getPrefix(), m.getMarkers(), version, String.valueOf(version), emptyList(), JavaType.Primitive.Int);
                        }
                    }
                }

                return expression;
            }
        });
    }

    private G.CompilationUnit addCompatibilityTypeToSourceFile(G.CompilationUnit c, String compatibilityType) {
        if ((this.compatibilityType == null || compatibilityType.equals(this.compatibilityType.toString())) && Boolean.TRUE.equals(addIfMissing)) {
            G.CompilationUnit sourceFile = (G.CompilationUnit) GradleParser.builder().build().parse("\n" + compatibilityType + "Compatibility = " + styleMissingCompatibilityVersion())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to parse compatibility type as a Gradle file"));
            sourceFile.getStatements();
            c = c.withStatements(ListUtils.concatAll(c.getStatements(), sourceFile.getStatements()));
        }
        return c;
    }

    private String styleMissingCompatibilityVersion() {
        if (declarationStyle == DeclarationStyle.String) {
            return version <= 8 ? "'1." + version + "'" : "'" + version + "'";
        } else if (declarationStyle == DeclarationStyle.Enum) {
            return version <= 8 ? "JavaVersion.VERSION_1_" + version : "JavaVersion.VERSION_" + version;
        } else if (version <= 8) {
            return "1." + version;
        }
        return String.valueOf(version);
    }

    public enum CompatibilityType {
        source, target
    }

    public enum DeclarationStyle {
        Enum, Number, String
    }
}
