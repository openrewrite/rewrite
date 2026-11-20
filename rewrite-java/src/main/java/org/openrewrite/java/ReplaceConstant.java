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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceConstant extends Recipe {
    @Option(displayName = "Owning type of the constant",
            description = "The target type in which the constant to be replaced is defined.",
            example = "com.google.common.base.Charsets")
    String owningType;

    @Option(displayName = "Constant name",
            description = "The name of the constant field to replace.",
            example = "UTF_8")
    String constantName;

    @Option(displayName = "Literal value",
            description = "The literal value to replace.",
            example = "UTF_8")
    String literalValue;

    String displayName = "Replace constant with literal value";

    String description = "Replace a named constant with a literal value when you wish to remove the old constant. A `String` literal must include escaped quotes.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> replacementVisitor = new JavaVisitor<ExecutionContext>() {
            J.@Nullable Literal literal;

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (isConstant(fieldAccess.getName().getFieldType())) {
                    maybeRemoveImport(owningType);
                    return buildReplacement(fieldAccess);
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                if (isConstant(ident.getFieldType()) && !isVariableDeclaration()) {
                    maybeRemoveImport(owningType);
                    return buildReplacement(ident);
                }
                return super.visitIdentifier(ident, ctx);
            }

            private boolean isConstant(JavaType.@Nullable Variable varType) {
                return varType != null && TypeUtils.isOfClassType(varType.getOwner(), owningType) &&
                        varType.getName().equals(constantName);
            }

            private boolean isVariableDeclaration() {
                J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
                if (declaration == null) {
                    return false;
                }
                JavaType.Variable variableType = declaration.getVariables().get(0).getVariableType();
                if (variableType == null) {
                    return true;
                }

                JavaType.FullyQualified ownerFqn = TypeUtils.asFullyQualified(variableType.getOwner());
                if (ownerFqn == null) {
                    return true;
                }

                return constantName.equals(declaration.getVariables().get(0).getSimpleName()) &&
                        owningType.equals(ownerFqn.getFullyQualifiedName());
            }

            private J buildReplacement(Expression reference) {
                J.Literal replacement = buildLiteral();
                JavaType type = reference.getType();
                Cursor argument = getCursor();
                Cursor parent = argument.getParentTreeCursor();
                while (parent.getValue() instanceof J.Parentheses) {
                    argument = parent;
                    parent = argument.getParentTreeCursor();
                }
                if (replacement.getType() == JavaType.Primitive.Null && TypeUtils.isWellFormedType(type) &&
                        (!(type instanceof JavaType.Primitive) || TypeUtils.isString(type)) &&
                        parent.getValue() instanceof MethodCall &&
                        ((MethodCall) parent.getValue()).getArguments().contains(argument.getValue())) {
                    MethodCall call = parent.getValue();
                    JavaType.Method method = call.getMethodType();
                    if (method != null && !method.getParameterTypes().isEmpty()) {
                        List<JavaType> parameters = method.getParameterTypes();
                        int index = call.getArguments().indexOf(argument.getValue());
                        JavaType parameter = parameters.get(Math.min(index, parameters.size() - 1));
                        if (method.hasFlags(Flag.Varargs) && index >= parameters.size() - 1 &&
                                parameter instanceof JavaType.Array && !TypeUtils.isAssignableTo(parameter, type)) {
                            parameter = ((JavaType.Array) parameter).getElemType();
                        }
                        // The constant's type may be inaccessible at the call site. Prefer the selected parameter.
                        if (!(parameter instanceof JavaType.Primitive) &&
                                !(parameter instanceof JavaType.GenericTypeVariable) && TypeUtils.isWellFormedType(parameter)) {
                            type = parameter;
                        }
                    }
                    // Preserve overload and varargs selection when null loses the constant's declared type.
                    // https://github.com/openrewrite/rewrite/issues/2579
                    String castType = TypeUtils.toFullyQualifiedName(TypeUtils.toString(type));
                    J.TypeCast cast = JavaTemplate.builder("(" + castType + ") null")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), reference.getCoordinates().replace());
                    doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(cast));
                    return cast.withPrefix(reference.getPrefix());
                }
                return replacement.withPrefix(reference.getPrefix());
            }

            private J.Literal buildLiteral() {
                if (literal == null) {
                    JavaParser parser = JavaParser.fromJavaVersion().build();
                    J.CompilationUnit result = parser.parse("class $ { Object o = " + literalValue + "; }")
                            .findFirst()
                            .map(J.CompilationUnit.class::cast)
                            .orElseThrow(() -> new IllegalStateException("Expected to have one parsed compilation unit."));

                    J j = ((J.VariableDeclarations) result.getClasses().get(0).getBody().getStatements().get(0)).getVariables().get(0).getInitializer();
                    if (!(j instanceof J.Literal)) {
                        throw new IllegalArgumentException("Unknown literal type for literal value: " + literalValue);
                    }

                    J.Literal parsedLiteral = (J.Literal) j;
                    literal = parsedLiteral.withId(Tree.randomId());
                }
                return literal;
            }
        };
        return Preconditions.check(
                new UsesType<>(owningType, true),
                replacementVisitor
        );
    }
}
