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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
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

    @Override
    public String getDisplayName() {
        return "Replace constant with literal value";
    }

    @Override
    public String getDescription() {
        return "Replace a named constant with a literal value when you wish to remove the old constant. A `String` literal must include escaped quotes.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Nullable
            J.Literal literal;

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext executionContext) {
                if (isConstantReference(fieldAccess.getName())) {
                    maybeRemoveImport(owningType);
                    return buildLiteral().withPrefix(fieldAccess.getPrefix());
                }
                return super.visitFieldAccess(fieldAccess, executionContext);
            }

            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext executionContext) {
                if (isConstantReference(ident)) {
                    maybeRemoveImport(owningType);
                    return buildLiteral().withPrefix(ident.getPrefix());
                }
                return super.visitIdentifier(ident, executionContext);
            }

            private boolean isConstantReference(J.Identifier ident) {
                JavaType.Variable varType = ident.getFieldType();
                if (varType == null || !varType.getName().equals(constantName)) {
                    return false;
                }
                J parent = getCursor().getParentTreeCursor().getValue();
                if (parent instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) parent).getName() == ident) {
                    // this identifier is an actual variable declaration, not a reference
                    return false;
                }
                return TypeUtils.isOfClassType(varType.getOwner(), owningType);
            }

            private J.Literal buildLiteral() {
                if (literal == null) {
                    JavaParser parser = JavaParser.fromJavaVersion().build();
                    List<J.CompilationUnit> result = parser.parse("class $ { Object o = " + literalValue + "; }");

                    J j = ((J.VariableDeclarations) result.get(0).getClasses().get(0).getBody().getStatements().get(0)).getVariables().get(0).getInitializer();
                    if (!(j instanceof J.Literal)) {
                        throw new IllegalArgumentException("Unknown literal type for literal value: " + literalValue);
                    }

                    J.Literal parsedLiteral = (J.Literal) j;
                    literal = parsedLiteral.withId(Tree.randomId());
                }
                return literal;
            }
        };
    }
}
