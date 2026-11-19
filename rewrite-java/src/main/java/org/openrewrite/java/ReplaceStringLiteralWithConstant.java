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
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.reflect.Field;
import java.util.Objects;

import static java.util.Collections.emptyList;
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
    @Nullable
    String fullyQualifiedConstantName;

    String displayName = "Replace String literal with constant";

    String description = "Replace String literal with constant, adding import on class if needed.";

    public @Nullable String getLiteralValue() {
        if (this.literalValue == null && this.fullyQualifiedConstantName != null) {
            try {
                this.literalValue = (String) getConstantValueByFullyQualifiedName(this.fullyQualifiedConstantName);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
                // Failed to retrieve unspecified value; field might be missing in this version of the class
                return null;
            }
        }
        return this.literalValue;
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> result = super.validate();
        if (StringUtils.isBlank(fullyQualifiedConstantName)) {
            return result.and(invalid(CONSTANT_FQN_PARAM, fullyQualifiedConstantName, "The constant's fully qualified name may not be empty or blank."));
        }
        if (StringUtils.isBlank(literalValue)) {
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
        return result;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String value = getLiteralValue();
        if (value == null) {
            return TreeVisitor.noop();
        } else {
            assert fullyQualifiedConstantName != null : "Validation should have failed if constant name is null";
            return new ReplaceStringLiteralVisitor(value, fullyQualifiedConstantName);
        }
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
            J constant = JavaTemplate.builder(template)
                    .imports(owningType)
                    .build()
                    .apply(getCursor(), literal.getCoordinates().replace())
                    .withPrefix(literal.getPrefix());
            return constant instanceof J.FieldAccess ? attributed((J.FieldAccess) constant) : constant;
        }

        // A constant declared outside the JDK is unknown to the template parser, so attribute its access here
        private J.FieldAccess attributed(J.FieldAccess constant) {
            if (TypeUtils.isWellFormedType(constant.getType())) {
                return constant;
            }
            JavaType.FullyQualified target = JavaType.ShallowClass.build(owningType);
            JavaType.FullyQualified owner = target;
            long flags = Flag.Public.getBitMask() | Flag.Static.getBitMask() | Flag.Final.getBitMask();
            try {
                Class<?> owningClass = loadClass(owningType);
                Field field = owningClass.getField(constant.getSimpleName());
                target = JavaType.ShallowClass.build(owningClass.getName());
                owner = JavaType.ShallowClass.build(field.getDeclaringClass().getName());
                flags = field.getModifiers();
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                // The literal value was given explicitly, so the constant's class need not be on the classpath
            }
            JavaType.FullyQualified string = JavaType.ShallowClass.build("java.lang.String");
            JavaType.Variable fieldType = new JavaType.Variable(null, flags, constant.getSimpleName(), owner, string,
                    emptyList());
            return constant
                    .withTarget(constant.getTarget().withType(target))
                    .withName(constant.getName().withType(string).withFieldType(fieldType))
                    .withType(string);
        }
    }

    private static @Nullable Object getConstantValueByFullyQualifiedName(String fullyQualifiedConstantName) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        String owningType = fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.'));
        String constantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf('.') + 1);
        Field constantField = loadClass(owningType).getField(constantName);
        return constantField.get(null);
    }

    // A nested class may be written with dots, as in source, rather than with its binary name
    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        String binaryName = name;
        while (true) {
            try {
                return Class.forName(binaryName);
            } catch (ClassNotFoundException e) {
                int lastDot = binaryName.lastIndexOf('.');
                if (lastDot < 0) {
                    throw e;
                }
                binaryName = binaryName.substring(0, lastDot) + '$' + binaryName.substring(lastDot + 1);
            }
        }
    }
}
