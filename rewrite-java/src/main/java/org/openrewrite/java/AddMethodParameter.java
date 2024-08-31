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
            description = "A method pattern that is used to find matching method declarations.",
            example = "com.yourorg.A foo(int, int)")
    String methodPattern;

    @Option(displayName = "Parameter index",
            description = "A zero-based index that indicates the position at which the parameter will be added. At the end by default.",
            example = "0",
            required = false)
    @Nullable
    Integer parameterIndex;

    @Option(displayName = "Parameter type",
            description = "The type of the parameter that gets added.",
            example = "java.lang.String")
    String parameterType;

    @Option(displayName = "Parameter name",
            description = "The name of the parameter that gets added.",
            example = "name")
    String parameterName;

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
        return Preconditions.check(new DeclaresType<>(methodPattern.substring(0, idx)), new AddNullMethodArgumentVisitor(methodPattern));
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
                method = addParameter(method, parameter);
            }
            return autoFormat(method, ctx);
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

            if (parameter.getTypeExpression() instanceof J.FieldAccess) {
                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(parameter.getTypeExpression()));
            }
            return method;
        }

        private J.VariableDeclarations createParameter(J.MethodDeclaration method) {
            TypeTree typeTree = createTypeTree();

            return new J.VariableDeclarations(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    typeTree,
                    null,
                    emptyList(),
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

        private TypeTree createTypeTree() {
            JavaType.Primitive type = JavaType.Primitive.fromKeyword(parameterType);
            if (type != null) {
                return new J.Primitive(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        type
                );
            }
            if (parameterType.indexOf('.') == -1) {
                String javaLangType = TypeUtils.findQualifiedJavaLangTypeName(parameterType);
                if (javaLangType != null) {
                    return new J.Identifier(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            parameterType,
                            JavaType.buildType(javaLangType),
                            null
                    );
                }
            }
            return TypeTree.build(parameterType);
        }
    }
}
