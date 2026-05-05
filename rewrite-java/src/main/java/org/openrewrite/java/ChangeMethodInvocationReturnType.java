/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method invocation return type",
            description = "The fully qualified new return type of method invocation. " +
                    "Parameterized types like `java.util.Set<java.lang.String>` are supported.",
            example = "long")
    String newReturnType;

    String displayName = "Change method invocation return type";

    String description = "Changes the return type of a method invocation.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !newReturnType.equals(type.getReturnType().toString())) {
                    JavaType newType = createTypeTree(newReturnType).getType();
                    type = type.withReturnType(newType);
                    m = m.withMethodType(type);
                    if (m.getName().getType() != null) {
                        m = m.withName(m.getName().withType(type));
                    }
                    methodUpdated = true;
                }
                return m;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                methodUpdated = false;
                JavaType originalType = multiVariable.getType();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                if (methodUpdated) {
                    TypeTree newTypeTree = createTypeTree(newReturnType);
                    JavaType newType = newTypeTree.getType();

                    removeImportsForType(originalType);
                    addImportsForType(newType);

                    if (mv.getTypeExpression() != null) {
                        mv = mv.withTypeExpression(newTypeTree.withPrefix(mv.getTypeExpression().getPrefix()));
                    }

                    mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                        JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                        if (varType != null && !varType.equals(newType)) {
                            return var.withType(newType).withName(var.getName().withType(newType));
                        }
                        return var;
                    }));
                }

                return mv;
            }

            private void addImportsForType(@Nullable JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    maybeAddImport(parameterized.getType());
                    for (JavaType param : parameterized.getTypeParameters()) {
                        addImportsForType(param);
                    }
                } else if (type instanceof JavaType.Array) {
                    addImportsForType(((JavaType.Array) type).getElemType());
                } else if (type instanceof JavaType.FullyQualified) {
                    maybeAddImport((JavaType.FullyQualified) type);
                }
            }

            private void removeImportsForType(@Nullable JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    maybeRemoveImport(parameterized.getType());
                    for (JavaType param : parameterized.getTypeParameters()) {
                        removeImportsForType(param);
                    }
                } else if (type instanceof JavaType.Array) {
                    removeImportsForType(((JavaType.Array) type).getElemType());
                } else if (type instanceof JavaType.FullyQualified) {
                    maybeRemoveImport((JavaType.FullyQualified) type);
                }
            }

            private TypeTree createTypeTree(String typeName) {
                int arrayIndex = typeName.lastIndexOf('[');
                if (arrayIndex != -1) {
                    TypeTree elementType = createTypeTree(typeName.substring(0, arrayIndex));
                    return new J.ArrayType(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            elementType,
                            null,
                            JLeftPadded.build(Space.EMPTY),
                            new JavaType.Array(null, elementType.getType(), null)
                    );
                }
                int genericsIndex = typeName.indexOf('<');
                if (genericsIndex != -1) {
                    TypeTree rawType = createTypeTree(typeName.substring(0, genericsIndex));
                    List<JRightPadded<Expression>> typeParameters = new ArrayList<>();
                    List<JavaType> typeParameterTypes = new ArrayList<>();
                    List<String> rawArgs = splitTypeArguments(typeName.substring(genericsIndex + 1, typeName.lastIndexOf('>')));
                    for (int i = 0; i < rawArgs.size(); i++) {
                        TypeTree paramTree = createTypeTree(rawArgs.get(i).trim());
                        if (i > 0) {
                            paramTree = paramTree.withPrefix(Space.SINGLE_SPACE);
                        }
                        typeParameters.add(JRightPadded.build((Expression) paramTree));
                        typeParameterTypes.add(paramTree.getType());
                    }
                    JavaType.FullyQualified rawFqn = TypeUtils.asFullyQualified(rawType.getType());
                    return new J.ParameterizedType(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            rawType,
                            JContainer.build(Space.EMPTY, typeParameters, Markers.EMPTY),
                            new JavaType.Parameterized(null, rawFqn, typeParameterTypes)
                    );
                }
                JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(typeName);
                if (primitive != null) {
                    return new J.Primitive(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            primitive
                    );
                }
                if ("?".equals(typeName)) {
                    return new J.Wildcard(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            null,
                            null
                    );
                }
                if (typeName.startsWith("?") && typeName.contains("extends")) {
                    return new J.Wildcard(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            new JLeftPadded<>(Space.SINGLE_SPACE, J.Wildcard.Bound.Extends, Markers.EMPTY),
                            createTypeTree(typeName.substring(typeName.indexOf("extends") + "extends".length() + 1).trim()).withPrefix(Space.SINGLE_SPACE)
                    );
                }
                if (typeName.startsWith("?") && typeName.contains("super")) {
                    return new J.Wildcard(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            new JLeftPadded<>(Space.SINGLE_SPACE, J.Wildcard.Bound.Super, Markers.EMPTY),
                            createTypeTree(typeName.substring(typeName.indexOf("super") + "super".length() + 1).trim()).withPrefix(Space.SINGLE_SPACE)
                    );
                }
                if (typeName.indexOf('.') == -1) {
                    String javaLangType = TypeUtils.findQualifiedJavaLangTypeName(typeName);
                    JavaType type = javaLangType != null ?
                            JavaType.buildType(javaLangType) :
                            JavaType.ShallowClass.build(typeName);
                    return new J.Identifier(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            typeName,
                            type,
                            null
                    );
                }
                return new J.Identifier(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        typeName.substring(typeName.lastIndexOf('.') + 1),
                        JavaType.ShallowClass.build(typeName),
                        null
                );
            }

            private List<String> splitTypeArguments(String args) {
                List<String> result = new ArrayList<>();
                int depth = 0;
                int start = 0;
                for (int i = 0; i < args.length(); i++) {
                    char c = args.charAt(i);
                    if (c == '<') {
                        depth++;
                    } else if (c == '>') {
                        depth--;
                    } else if (c == ',' && depth == 0) {
                        result.add(args.substring(start, i));
                        start = i + 1;
                    }
                }
                result.add(args.substring(start));
                return result;
            }
        };
    }
}
