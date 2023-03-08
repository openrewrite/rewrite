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


@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceStringLiteralWithConstant extends Recipe {

    @Option(displayName = "Fully Qualified Name of the constant",
            example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String fullyQualifiedConstantName;

    @Option(displayName = "(Optional) String literal value that needs to be replaced. Only String literal accepted.",
            example = "application/json",
            required = false )
    @Nullable
    String literalValue;

    public ReplaceStringLiteralWithConstant(String fullyQualifiedConstantName) {
        this.fullyQualifiedConstantName = fullyQualifiedConstantName;
        this.literalValue = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf(".")+1);
    }

    public ReplaceStringLiteralWithConstant(String fullyQualifiedConstantName, String literalValue) {
        this.fullyQualifiedConstantName = fullyQualifiedConstantName;
        this.literalValue = literalValue;
    }

    @Override
    public String getDisplayName() {
        return "Replace String Literal with Constant";
    }

    @Override
    public String getDescription() {
        return "Replace a String Literal with named constant. If String literal value no provided, the recipe will use constant's Name in place of literal value.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
                if(isStringLiteral(literal)) {
                    String owningType = fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf("."));
                    String constantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf(".")+1);
                    String className = owningType.substring(owningType.lastIndexOf(".")+1);

                    maybeAddImport(owningType,false);

                    String template = String.join(".",className,constantName);
                    J j = literal.withTemplate(JavaTemplate.builder(this::getCursor,template).imports(owningType).build(),literal.getCoordinates().replace());

                    if (!(j instanceof J.FieldAccess)) {
                        throw new IllegalArgumentException("Invalid FQN : " + fullyQualifiedConstantName);
                    }

                    return j.withPrefix(literal.getPrefix());
                }
                return super.visitLiteral(literal, executionContext);
            }

            private boolean isStringLiteral(@Nullable J.Literal literal) {
                return literal!=null && TypeUtils.isString(literal.getType()) &&
                        ((String) literal.getValue()).equals(literalValue);
            }

        };
    }
}
