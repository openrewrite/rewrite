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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.openrewrite.Validated.invalid;


@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceStringLiteralWithConstant extends Recipe {

    private static final String CONSTANT_FQN_PARAM = "fullyQualifiedConstantName";

    @Option(displayName = "String literal value to replace",
            description = "The literal that is to be replaced. If not configured, the value of the specified constant will be used by default.",
            example = "application/json",
            required = false)
    @Nullable
    @NonFinal
    String literalValue;

    @Option(displayName = "Fully qualified name of the constant to use in place of String literal", example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String fullyQualifiedConstantName;

    public ReplaceStringLiteralWithConstant(String fullyQualifiedConstantName) {
        this.literalValue = null;
        this.fullyQualifiedConstantName = fullyQualifiedConstantName;
    }

    @JsonCreator
    public ReplaceStringLiteralWithConstant(String literalValue, String fullyQualifiedConstantName) {
        this.literalValue = literalValue;
        this.fullyQualifiedConstantName = fullyQualifiedConstantName;
    }

    @Override
    public String getDisplayName() {
        return "Replace String literal with constant";
    }

    @Override
    public String getDescription() {
        return "Replace String literal with constant, adding import on class if needed.";
    }

    public String getLiteralValue() {
        if (this.literalValue == null && this.fullyQualifiedConstantName != null) {
            try {
                this.literalValue = (String) getConstantValueByFullyQualifiedName(this.fullyQualifiedConstantName);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
                throw new IllegalArgumentException("Failed to retrieve value from the configured constant", e);
            }
        }
        return this.literalValue;
    }

    @Override
    public Validated validate() {
        Validated result = super.validate();
        if (StringUtils.isBlank(fullyQualifiedConstantName)) {
            return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "The constant's fully qualified name may not be empty or blank."));
        }
        try {
            Object constantValue = getConstantValueByFullyQualifiedName(fullyQualifiedConstantName);
            if (constantValue == null) {
                return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "Provided constant should not be null."));
            }
            if (!(constantValue instanceof String)) {
                // currently, we only support string literals, also see visitor implementation
                return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "Unsupported type of constant provided. Only literals can be replaced."));
            }
            return result;
        } catch (ClassNotFoundException e) {
            return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "No class for specified name was found."));
        } catch (NoSuchFieldException e) {
            return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "No field with specified name was found."));
        } catch (IllegalAccessException e) {
            return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "Unable to access specified field."));
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceStringLiteralVisitor(getLiteralValue(), getFullyQualifiedConstantName());
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
        public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
            // Only handle String literals
            if (!TypeUtils.isString(literal.getType()) ||
                !Objects.equals(literalValue, literal.getValue())) {
                return super.visitLiteral(literal, ctx);
            }

            // Prevent changing constant definition
            J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (classDeclaration != null &&
                classDeclaration.getType() != null &&
                owningType.equals(classDeclaration.getType().getFullyQualifiedName())) {
                return super.visitLiteral(literal, ctx);
            }

            maybeAddImport(owningType, false);
            return JavaTemplate.builder(template)
                    .contextSensitive()
                    .imports(owningType)
                    .build()
                    .apply(getCursor(), literal.getCoordinates().replace())
                    .withPrefix(literal.getPrefix());
        }
    }

    @Nullable
    private static Object getConstantValueByFullyQualifiedName(String fullyQualifiedConstantName) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        String owningType = fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.'));
        String constantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf('.') + 1);
        Field constantField = Class.forName(owningType).getField(constantName);
        return constantField.get(null);
    }
}
