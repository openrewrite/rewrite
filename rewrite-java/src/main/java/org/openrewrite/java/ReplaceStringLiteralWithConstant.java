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
import org.openrewrite.java.tree.TypeUtils;


import java.util.List;


@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceStringLiteralWithConstant extends Recipe {

    @Option(displayName = "Owning type of the constant",
            example = "org.springframework.http.MediaType")
    String owningType;

    @Option(displayName = "Constant name",
            example = "APPLICATION_JSON_VALUE")
    String constantName;

    @Option(displayName = "Literal value",
            example = "application/json")
    String literalValue;

    @Override
    public String getDisplayName() {
        return "Replace String Literal with Constant";
    }

    @Override
    public String getDescription() {
        return "Replace a String Literal with named constant.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Nullable
            J.FieldAccess constant;

            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
                if(isStringLiteral(literal)) {
                    maybeAddImport(owningType,false);
                    return buildConstant().withPrefix(literal.getPrefix());
                }
                return super.visitLiteral(literal, executionContext);
            }

            private boolean isStringLiteral(@Nullable J.Literal literal) {
                return literal!=null && TypeUtils.isString(literal.getType()) &&
                        ((String) literal.getValue()).equals(literalValue);
            }

            private J.FieldAccess buildConstant() {
                if(constant == null) {
                    // Instead of creating new FieldAccess using constructor, parsing code snippet to create Field Access using JavaParser.
                    JavaParser parser = JavaParser.fromJavaVersion().build();
                    List<J.CompilationUnit> result = parser.parse(
                            "import "+owningType+";\n" +
                                    "class Test {\n" +
                                    "   Object o = "+String.join(".",owningType.substring(owningType.lastIndexOf(".")+1),constantName)+";\n" +
                                    "}"
                    );

                    // Retrieving FieldAccess from Compilation Unit
                    J j = ((J.VariableDeclarations) result.get(0).getClasses().get(0).getBody().getStatements().get(0)).getVariables().get(0).getInitializer();
                    if (!(j instanceof J.FieldAccess)) {
                        throw new IllegalArgumentException("Invalid constant " + constantName + " or owningType " + owningType);
                    }

                    J.FieldAccess parsedFieldAccess = (J.FieldAccess) j;
                    constant = parsedFieldAccess.withId(Tree.randomId());
                }
                return constant;
            }
        };
    }
}
