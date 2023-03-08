/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;


@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceStringLiteralWithConstant extends Recipe {

    @Option(displayName = "String literal value to replace", example = "application/json")
    String literalValue;

    @Option(displayName = "Fully qualified name of the constant to use in place of String literal", example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String fullyQualifiedConstantName;

    @Override
    public String getDisplayName() {
        return "Replace String literal with constant";
    }

    @Override
    public String getDescription() {
        return "Replace String literal with constant, adding import on class if needed.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new ReplaceStringLiteralVisitor(literalValue, fullyQualifiedConstantName);
    }

    private static class ReplaceStringLiteralVisitor extends JavaVisitor<ExecutionContext> {

        private final String literalValue;
        private final String owningType;
        private final String template;

        public ReplaceStringLiteralVisitor(String literalValue, String fullyQualifiedConstantName) {
            this.literalValue = literalValue;
            this.owningType = fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.'));
            this.template = fullyQualifiedConstantName.substring(owningType.lastIndexOf('.') + 1);
        }

        @Override
        public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
            // Only handle String literals
            if (!TypeUtils.isString(literal.getType()) ||
                !literalValue.equals(literal.getValue())) {
                return super.visitLiteral(literal, executionContext);
            }

            // Prevent changing constant definition
            J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (classDeclaration != null &&
                classDeclaration.getType() != null &&
                owningType.equals(classDeclaration.getType().getFullyQualifiedName())) {
                return super.visitLiteral(literal, executionContext);
            }

            maybeAddImport(owningType, false);
            return literal
                    .withTemplate(
                            JavaTemplate.builder(this::getCursor, template).imports(owningType).build(),
                            literal.getCoordinates().replace())
                    .withPrefix(literal.getPrefix());
        }
    }
}
