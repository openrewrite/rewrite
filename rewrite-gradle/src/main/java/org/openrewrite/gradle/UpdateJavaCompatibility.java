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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateJavaCompatibility extends Recipe {
    private static final MethodMatcher SOURCE_COMPATIBILITY_DSL = new MethodMatcher("RewriteGradleProject setSourceCompatibility(..)");
    private static final MethodMatcher TARGET_COMPATIBILITY_DSL = new MethodMatcher("RewriteGradleProject setTargetCompatibility(..)");

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
        return Preconditions.check(new IsBuildGradle<>(), Preconditions.or(new GroovyScriptVisitor(), new KotlinScriptVisitor()));
    }

    private class GroovyScriptVisitor extends GroovyIsoVisitor<ExecutionContext> {
        @Override
        public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
            G.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            if (getCursor().pollMessage(SOURCE_COMPATIBILITY_FOUND) == null) {
                c = addCompatibilityTypeToSourceFile(c, "source", ctx);
            }
            if (getCursor().pollMessage(TARGET_COMPATIBILITY_FOUND) == null) {
                c = addCompatibilityTypeToSourceFile(c, "target", ctx);
            }
            return c;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            return handleAssignment(super.visitAssignment(assignment, ctx), getCursor(), G.CompilationUnit.class);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            return handleMethodInvocation(super.visitMethodInvocation(method, ctx), getCursor(), G.CompilationUnit.class);
        }

        private G.CompilationUnit addCompatibilityTypeToSourceFile(G.CompilationUnit c, String targetCompatibilityType, ExecutionContext ctx) {
            if ((compatibilityType == null || targetCompatibilityType.equals(compatibilityType.toString())) && TRUE.equals(addIfMissing)) {
                G.CompilationUnit sourceFile = (G.CompilationUnit) GradleParser.builder().build()
                        .parse(ctx, "\n" + targetCompatibilityType + "Compatibility = " + styleMissingCompatibilityVersion(declarationStyle))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to parse compatibility type as a Gradle file"));
                c = c.withStatements(ListUtils.concatAll(c.getStatements(), sourceFile.getStatements()));
            }
            return c;
        }
    }

    private class KotlinScriptVisitor extends KotlinIsoVisitor<ExecutionContext> {
        @Override
        public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
            K.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            if (getCursor().pollMessage(SOURCE_COMPATIBILITY_FOUND) == null) {
                c = addCompatibilityTypeToSourceFile(c, "source", ctx);
            }
            if (getCursor().pollMessage(TARGET_COMPATIBILITY_FOUND) == null) {
                c = addCompatibilityTypeToSourceFile(c, "target", ctx);
            }
            return super.visitCompilationUnit(c, ctx);
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            return handleAssignment(super.visitAssignment(assignment, ctx), getCursor(), K.CompilationUnit.class);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            return handleMethodInvocation(super.visitMethodInvocation(method, ctx), getCursor(), K.CompilationUnit.class);
        }

        private K.CompilationUnit addCompatibilityTypeToSourceFile(K.CompilationUnit c, String targetCompatibilityType, ExecutionContext ctx) {
            if ((compatibilityType == null || targetCompatibilityType.equals(compatibilityType.toString())) && TRUE.equals(addIfMissing)) {
                J withExistingJavaMethod = maybeAddToExistingJavaMethod(c, targetCompatibilityType, ctx);
                if (withExistingJavaMethod != c) {
                    return (K.CompilationUnit) withExistingJavaMethod;
                }

                K.CompilationUnit sourceFile = (K.CompilationUnit) KotlinParser.builder()
                        .isKotlinScript(true)
                        .build().parse(ctx, "\n\njava {\n    " + targetCompatibilityType + "Compatibility = " + styleMissingCompatibilityVersion(DeclarationStyle.Enum) + "\n}")
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to parse compatibility type as a Gradle file"));
                c = c.withStatements(ListUtils.concatAll(c.getStatements(), sourceFile.getStatements()));
            }
            return c;
        }

        private J maybeAddToExistingJavaMethod(K.CompilationUnit c, String compatibilityType, ExecutionContext ctx) {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if ("java".equals(method.getSimpleName())) {
                        return new JavaIsoVisitor<ExecutionContext>() {
                            @Override
                            public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                                J.Block body = (J.Block) lambda.getBody();
                                List<Statement> statements = body.getStatements();
                                K.CompilationUnit sourceFile = (K.CompilationUnit) KotlinParser.builder()
                                        .isKotlinScript(true)
                                        .build().parse(ctx, "\n    " + compatibilityType + "Compatibility = " + styleMissingCompatibilityVersion(DeclarationStyle.Enum))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("Unable to parse compatibility type as a Gradle file"));
                                return lambda.withBody(body.withStatements(ListUtils.concatAll(statements, sourceFile.getStatements())));
                            }
                        }.visitMethodInvocation(method, ctx);
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            }.visitNonNull(c, ctx);
        }
    }

    public J.Assignment handleAssignment(J.Assignment a, Cursor c, Class<?> enclosing) {
        if (a.getVariable() instanceof J.Identifier) {
            J.Identifier variable = (J.Identifier) a.getVariable();
            if ("sourceCompatibility".equals(variable.getSimpleName())) {
                c.putMessageOnFirstEnclosing(enclosing, SOURCE_COMPATIBILITY_FOUND, true);
            }
            if ("targetCompatibility".equals(variable.getSimpleName())) {
                c.putMessageOnFirstEnclosing(enclosing, TARGET_COMPATIBILITY_FOUND, true);
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
                if (!("sourceCompatibility".equals(fieldAccess.getSimpleName()) || "targetCompatibility".equals(fieldAccess.getSimpleName()) ||
                        ("release".equals(fieldAccess.getSimpleName()) &&
                                ((fieldAccess.getTarget() instanceof J.Identifier && "options".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName())) ||
                                        (fieldAccess.getTarget() instanceof J.FieldAccess && "options".equals(((J.FieldAccess) fieldAccess.getTarget()).getSimpleName())))))) {
                    return a;
                }
            } else if (!(compatibilityType.toString().toLowerCase() + "Compatibility").equals(fieldAccess.getSimpleName())) {
                return a;
            }
        } else {
            return a;
        }

        DeclarationStyle currentStyle = getCurrentStyle(a.getAssignment());
        Integer currentMajor = getMajorVersion(a.getAssignment());
        if (shouldUpdateVersion(currentMajor) || shouldUpdateStyle(currentStyle)) {
            DeclarationStyle actualStyle = declarationStyle == null ? currentStyle : declarationStyle;
            return a.withAssignment(changeJavaVersion(a.getAssignment(), actualStyle));
        }

        return a;
    }

    private boolean shouldUpdateVersion(@Nullable Integer currentMajor) {
        return currentMajor != null && (currentMajor < version || currentMajor > version && TRUE.equals(allowDowngrade));
    }

    private boolean shouldUpdateStyle(@Nullable DeclarationStyle currentStyle) {
        return declarationStyle != null && declarationStyle != currentStyle;
    }

    public J.MethodInvocation handleMethodInvocation(J.MethodInvocation m, Cursor c, Class<?> enclosing) {
        if ("sourceCompatibility".equals(m.getSimpleName())) {
            c.putMessageOnFirstEnclosing(enclosing, SOURCE_COMPATIBILITY_FOUND, true);
        }
        if ("targetCompatibility".equals(m.getSimpleName())) {
            c.putMessageOnFirstEnclosing(enclosing, TARGET_COMPATIBILITY_FOUND, true);
        }
        if ("jvmToolchain".equals(m.getSimpleName()) || isMethodInvocation(m, "JavaLanguageVersion", "of")) {
            List<Expression> args = m.getArguments();

            if (args.size() == 1) {
                if (args.get(0) instanceof J.Literal || ("jvmToolchain".equals(m.getSimpleName()) && args.get(0) instanceof J.Lambda)) {
                    Integer currentMajor = getMajorVersion(args.get(0));
                    if (shouldUpdateVersion(currentMajor)) {
                        return m.withArguments(ListUtils.mapFirst(m.getArguments(), it -> changeJavaVersion(it, null)));
                    }
                    return m;
                }
            }

            return SearchResult.found(m, "Attempted to update to Java version to " + version +
                    "  but was unsuccessful, please update manually");
        }

        if (SOURCE_COMPATIBILITY_DSL.matches(m) || TARGET_COMPATIBILITY_DSL.matches(m)) {
            if (compatibilityType != null && (
                    (compatibilityType == CompatibilityType.source && !SOURCE_COMPATIBILITY_DSL.matches(m)) ||
                            (compatibilityType == CompatibilityType.target && !TARGET_COMPATIBILITY_DSL.matches(m)))) {
                return m;
            }

            if (m.getArguments().size() == 1 && (m.getArguments().get(0) instanceof J.Literal || m.getArguments().get(0) instanceof J.FieldAccess)) {
                DeclarationStyle currentStyle = getCurrentStyle(m.getArguments().get(0));
                Integer currentMajor = getMajorVersion(m.getArguments().get(0));
                if (shouldUpdateVersion(currentMajor) || shouldUpdateStyle(declarationStyle)) {
                    DeclarationStyle actualStyle = declarationStyle == null ? currentStyle : declarationStyle;
                    return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> changeJavaVersion(arg, actualStyle)));
                }
                return m;
            }

            return SearchResult.found(m, "Attempted to update to Java version to " + version +
                    "  but was unsuccessful, please update manually");
        }

        return m;
    }

    private int getMajorVersion(@Nullable String version) {
        if (version == null) {
            return -1;
        }
        try {
            return Integer.parseInt(normalize(version));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private @Nullable Integer getMajorVersion(Expression expression) {
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
        } else if (isMethodInvocation(expression, "JavaVersion", "toVersion")) {
            J.MethodInvocation method = (J.MethodInvocation) expression;
            if (method.getArguments().get(0) instanceof J.Literal) {
                return getMajorVersion(method.getArguments().get(0));
            }
        }

        return null;
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
            }
            return removePrefix;
        }

        return version.substring(version.indexOf(".") + 1);
    }

    private Expression changeJavaVersion(Expression expression, @Nullable DeclarationStyle style) {
        String newJavaVersion = version <= 8 ? "1." + version : String.valueOf(version);
        String newJavaVersionEnum = version <= 8 ? "VERSION_1_" + version : "VERSION_" + version;
        double newJavaVersionDouble = Double.parseDouble("1." + version);

        if (expression instanceof J.Literal) {
            J.Literal literal = (J.Literal) expression;
            if (style == null) {
                if (literal.getType() == JavaType.Primitive.String) {
                    return changeJavaVersion(literal, DeclarationStyle.String);
                } else if (literal.getType() == JavaType.Primitive.Int || literal.getType() == JavaType.Primitive.Double) {
                    return changeJavaVersion(literal, DeclarationStyle.Number);
                }
            } else if (style == DeclarationStyle.String) {
                if (literal.getType() == JavaType.Primitive.String) {
                    expression = ChangeStringLiteral.withStringValue(literal, newJavaVersion);
                } else {
                    expression = literal.withType(JavaType.Primitive.String).withValue(newJavaVersion).withValueSource("'" + newJavaVersion + "'");
                }
            } else if (style == DeclarationStyle.Enum) {
                expression = changeJavaVersion(newJavaVersionEnum, literal.getPrefix(), literal.getMarkers());
            } else if (style == DeclarationStyle.Number) {
                if (version <= 8) {
                    expression = literal.withType(JavaType.Primitive.Double).withValue(newJavaVersionDouble).withValueSource("1." + version);
                } else {
                    expression = literal.withType(JavaType.Primitive.Int).withValue(version).withValueSource(String.valueOf(version));
                }
            }
        } else if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            if (style == DeclarationStyle.String) {
                expression = new J.Literal(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(), newJavaVersion, "'" + newJavaVersion + "'", emptyList(), JavaType.Primitive.String);
            } else if (style == DeclarationStyle.Enum) {
                expression = fieldAccess.withName(fieldAccess.getName().withSimpleName(newJavaVersionEnum));
            } else if (style == DeclarationStyle.Number) {
                expression = changeJavaVersion(newJavaVersionDouble, fieldAccess.getPrefix(), fieldAccess.getMarkers());
            }
        } else if (isMethodInvocation(expression, "JavaVersion", "toVersion")) {
            J.MethodInvocation m = (J.MethodInvocation) expression;
            if (style == null) {
                expression = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                    if (arg instanceof J.Literal) {
                        if (arg.getType() == JavaType.Primitive.String) {
                            return changeJavaVersion(arg, DeclarationStyle.String);
                        } else if (arg.getType() == JavaType.Primitive.Int || arg.getType() == JavaType.Primitive.Double) {
                            return changeJavaVersion(arg, DeclarationStyle.Number);
                        }
                    }
                    return arg;
                }));
            } else if (style == DeclarationStyle.String) {
                expression = new J.Literal(randomId(), m.getPrefix(), m.getMarkers(), newJavaVersion, "'" + newJavaVersion + "'", emptyList(), JavaType.Primitive.String);
            } else if (style == DeclarationStyle.Enum) {
                expression = changeJavaVersion(newJavaVersionEnum, m.getPrefix(), m.getMarkers());
            } else if (style == DeclarationStyle.Number) {
                expression = changeJavaVersion(newJavaVersionDouble, m.getPrefix(), m.getMarkers());
            }
        }

        return expression;
    }

    private Expression changeJavaVersion(String newJavaVersionEnum, Space prefix, Markers markers) {
        return new J.FieldAccess(
                randomId(),
                prefix,
                markers,
                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "JavaVersion", JavaType.ShallowClass.build("org.gradle.api.JavaVersion"), null),
                new JLeftPadded<>(Space.EMPTY, new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), newJavaVersionEnum, null, null), Markers.EMPTY),
                JavaType.ShallowClass.build("org.gradle.api.JavaVersion")
        );
    }

    private Expression changeJavaVersion(double newJavaVersionDouble, Space prefix, Markers markers) {
        if (version <= 8) {
            return new J.Literal(randomId(), prefix, markers, newJavaVersionDouble, String.valueOf(newJavaVersionDouble), emptyList(), JavaType.Primitive.Double);
        }
        return new J.Literal(randomId(), prefix, markers, version, String.valueOf(version), emptyList(), JavaType.Primitive.Int);
    }

    private static boolean isMethodInvocation(J expression, String clazz, String method) {
        return expression instanceof J.MethodInvocation &&
                ((J.MethodInvocation) expression).getSimpleName().equals(method) &&
                ((J.MethodInvocation) expression).getSelect() instanceof J.Identifier &&
                ((J.Identifier) ((J.MethodInvocation) expression).getSelect()).getSimpleName().equals(clazz);
    }

    private String styleMissingCompatibilityVersion(@Nullable DeclarationStyle declarationStyle) {
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
