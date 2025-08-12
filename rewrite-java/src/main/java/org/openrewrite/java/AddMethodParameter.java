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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.DeclaresType;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddMethodParameter extends Recipe {

    /**
     * A method pattern that is used to find matching method declarations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find the method declarations to modify.",
            example = "com.yourorg.A foo(int, int)")
    String methodPattern;

    @Option(displayName = "Parameter type",
            description = "The type of the parameter that gets added.",
            example = "java.lang.String")
    String parameterType;

    @Option(displayName = "Parameter name",
            description = "The name of the parameter that gets added.",
            example = "name")
    String parameterName;

    @Option(displayName = "Parameter index",
            description = "A zero-based index that indicates the position at which the parameter will be added. At the end by default.",
            example = "0",
            required = false)
    @Nullable
    Integer parameterIndex;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s %s` in methods `%s`", parameterType, parameterName, methodPattern);
    }

    @Override
    public String getDisplayName() {
        return "Add method parameter to a method declaration";
    }

    @Override
    public String getDescription() {
        return "Adds a new method parameter to an existing method declaration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        int idx = methodPattern.indexOf('#');
        idx = idx == -1 ? methodPattern.indexOf(' ') : idx;
        boolean typePattern = idx != -1 && methodPattern.lastIndexOf('*', idx) != -1;
        return Preconditions.check(typePattern ? new DeclaresMatchingType(methodPattern.substring(0, idx)) : new DeclaresType<>(methodPattern.substring(0, idx)), new AddNullMethodArgumentVisitor(methodPattern));
    }

    private class AddNullMethodArgumentVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public AddNullMethodArgumentVisitor(String methodPattern) {
            this.methodMatcher = new MethodMatcher(methodPattern);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            method = super.visitMethodDeclaration(method, ctx);
            J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (enclosing != null && methodMatcher.matches(method, enclosing)) {
                for (Statement parameter : method.getParameters()) {
                    if (parameter instanceof J.VariableDeclarations && ((J.VariableDeclarations) parameter).getVariables().get(0).getSimpleName().equals(parameterName)) {
                        return method;
                    }
                }
                J.VariableDeclarations parameter = createParameter(method);
                method = autoFormat(addParameter(method, parameter), parameter, ctx, getCursor().getParentTreeCursor());
            }
            return method;
        }

        private J.MethodDeclaration addParameter(J.MethodDeclaration method, J.VariableDeclarations parameter) {
            List<Statement> originalParameters = method.getParameters();
            if (method.getParameters().isEmpty() || method.getParameters().size() == 1 && method.getParameters().get(0) instanceof J.Empty) {
                originalParameters = new ArrayList<>();
            } else {
                if (parameterIndex == null || parameterIndex != 0) {
                    parameter = parameter.withPrefix(Space.SINGLE_SPACE);
                } else {
                    originalParameters = ListUtils.mapFirst(originalParameters, p -> p.getPrefix().isEmpty() ? p.withPrefix(Space.SINGLE_SPACE) : p);
                }
            }

            if (parameterIndex == null) {
                method = method.withParameters(ListUtils.concat(originalParameters, parameter));
            } else {
                method = method.withParameters(ListUtils.insert(originalParameters, parameter, parameterIndex));
            }

            if (parameter.getTypeExpression() != null && !(parameter.getTypeExpression() instanceof J.Identifier || parameter.getTypeExpression() instanceof J.Primitive)) {
                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(parameter.getTypeExpression()));
            }
            return method;
        }

        private J.VariableDeclarations createParameter(J.MethodDeclaration method) {
            TypeTree typeTree = createTypeTree(parameterType);

            return new J.VariableDeclarations(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    typeTree,
                    null,
                    singletonList(
                            new JRightPadded<>(
                                    new J.VariableDeclarations.NamedVariable(
                                            randomId(),
                                            Space.EMPTY,
                                            Markers.EMPTY,
                                            new J.Identifier(
                                                    randomId(),
                                                    Space.EMPTY,
                                                    Markers.EMPTY,
                                                    emptyList(),
                                                    parameterName,
                                                    typeTree.getType(),
                                                    new JavaType.Variable(
                                                            null,
                                                            0,
                                                            parameterName,
                                                            method.getMethodType(),
                                                            typeTree.getType(),
                                                            null
                                                    )
                                            ),
                                            emptyList(),
                                            null,
                                            null
                                    ),
                                    Space.EMPTY,
                                    Markers.EMPTY
                            )
                    )
            );
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
                for (String typeParam : typeName.substring(genericsIndex + 1, typeName.lastIndexOf('>')).split(",")) {
                    typeParameters.add(JRightPadded.build((Expression) createTypeTree(typeParam.trim())));
                }
                return new J.ParameterizedType(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        rawType,
                        JContainer.build(Space.EMPTY, typeParameters, Markers.EMPTY),
                        new JavaType.Parameterized(null, (JavaType.FullyQualified) rawType.getType(), null)
                );
            }
            JavaType.Primitive type = JavaType.Primitive.fromKeyword(typeName);
            if (type != null) {
                return new J.Primitive(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        type
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
            if (typeName.indexOf('.') == -1) {
                String javaLangType = TypeUtils.findQualifiedJavaLangTypeName(typeName);
                if (javaLangType != null) {
                    return new J.Identifier(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            typeName,
                            JavaType.buildType(javaLangType),
                            null
                    );
                }
            }
            TypeTree typeTree = TypeTree.build(typeName);
            // somehow the type attribution is incomplete, but `ChangeType` relies on this
            if (typeTree instanceof J.FieldAccess) {
                typeTree = ((J.FieldAccess) typeTree).withName(((J.FieldAccess) typeTree).getName().withType(typeTree.getType()));
            } else if (typeTree.getType() == null) {
                typeTree = ((J.Identifier) typeTree).withType(JavaType.ShallowClass.build(typeName));
            }
            return typeTree;
        }
    }

    private static class DeclaresMatchingType extends JavaIsoVisitor<ExecutionContext> {
        private final TypeMatcher typeMatcher;

        public DeclaresMatchingType(String type) {
            this.typeMatcher = new TypeMatcher(type);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (classDecl.getType() != null && typeMatcher.matches(classDecl.getType())) {
                return SearchResult.found(classDecl);
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }
    }
}
