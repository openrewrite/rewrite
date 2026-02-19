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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.Trait;

/**
 * Represents an extra property in a Gradle build script where the value is a string literal.
 * <p>
 * This trait only matches properties where the value can be statically determined as a string literal.
 * Properties with variable references, expressions, or non-string values will not match and the trait
 * will not be created for them.
 * <p>
 * Handles multiple syntax forms:
 * <ul>
 *   <li>Variable declarations: {@code def propertyName = 'value'} or {@code val propertyName = "value"}</li>
 *   <li>ext block assignments: {@code ext { propertyName = 'value' }}</li>
 *   <li>ext field access: {@code ext.propertyName = 'value'}</li>
 *   <li>ext.set() method: {@code ext.set("propertyName", "value")}</li>
 * </ul>
 */
@Value
public class ExtraProperty implements Trait<J> {
    Cursor cursor;
    String propertyName;
    String currentValue;
    PropertySyntax syntax;

    /**
     * The syntax form used to declare this property.
     */
    public enum PropertySyntax {
        /**
         * Variable declaration: {@code def foo = "bar"} or {@code val foo = "bar"}
         */
        VARIABLE_DECLARATION,
        /**
         * Assignment in ext block: {@code ext { foo = "bar" }}
         */
        EXT_BLOCK_ASSIGNMENT,
        /**
         * Field access assignment: {@code ext.foo = "bar"}
         */
        EXT_FIELD_ACCESS,
        /**
         * Method invocation: {@code ext.set("foo", "bar")}
         */
        EXT_SET_METHOD
    }

    public String getName() {
        return propertyName;
    }

    /**
     * Gets the string literal value of this property.
     * This method always returns a non-null string because the trait is only created
     * when a string literal value can be extracted from the source code.
     *
     * @return The property's string literal value
     */
    public String getValue() {
        return currentValue;
    }

    /**
     * Update the property value.
     *
     * @param newValue The new value to set
     * @return A new ExtraProperty with the updated value, or this if no change was made
     */
    public ExtraProperty withValue(String newValue) {
        if (newValue == null || newValue.equals(currentValue)) {
            return this;
        }

        J updatedTree = updateTreeWithValue(cursor.getValue(), newValue);
        if (updatedTree == cursor.getValue()) {
            return this;
        }

        return new ExtraProperty(new Cursor(cursor.getParent(), updatedTree), propertyName, newValue, syntax);
    }

    private J updateTreeWithValue(J tree, String newValue) {
        switch (syntax) {
            case VARIABLE_DECLARATION:
                J.VariableDeclarations.NamedVariable var = (J.VariableDeclarations.NamedVariable) tree;
                if (var.getInitializer() instanceof J.Literal) {
                    J.Literal literal = (J.Literal) var.getInitializer();
                    return var.withInitializer(ChangeStringLiteral.withStringValue(literal, newValue));
                }
                break;

            case EXT_BLOCK_ASSIGNMENT:
            case EXT_FIELD_ACCESS:
                J.Assignment assignment = (J.Assignment) tree;
                if (assignment.getAssignment() instanceof J.Literal) {
                    J.Literal literal = (J.Literal) assignment.getAssignment();
                    return assignment.withAssignment(ChangeStringLiteral.withStringValue(literal, newValue));
                }
                break;

            case EXT_SET_METHOD:
                J.MethodInvocation method = (J.MethodInvocation) tree;
                if (method.getArguments().size() == 2 && method.getArguments().get(1) instanceof J.Literal) {
                    J.Literal valueLiteral = (J.Literal) method.getArguments().get(1);
                    J.Literal newValueLiteral = ChangeStringLiteral.withStringValue(valueLiteral, newValue);
                    return method.withArguments(ListUtils.mapLast(method.getArguments(), arg -> newValueLiteral));
                }
                break;
        }

        return tree;
    }

    public static class Matcher extends GradleTraitMatcher<ExtraProperty> {
        @Nullable
        private String propertyName;

        private boolean matchVariableDeclarations = true;

        /**
         * Match only properties with the specified name.
         */
        public Matcher propertyName(@Nullable String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        /**
         * Control whether to match variable declarations (def/val).
         * Default is true.
         */
        public Matcher matchVariableDeclarations(boolean matchVariableDeclarations) {
            this.matchVariableDeclarations = matchVariableDeclarations;
            return this;
        }

        /**
         * Tests if the cursor points to an extra property with a string literal value.
         * Returns null if:
         * <ul>
         *   <li>The node is not an extra property declaration</li>
         *   <li>The property value is not a string literal (e.g., variable reference, expression)</li>
         *   <li>The property name doesn't match the configured filter (if set)</li>
         * </ul>
         *
         * @param cursor The cursor to test
         * @return An ExtraProperty trait if matched, null otherwise
         */
        @Override
        protected @Nullable ExtraProperty test(Cursor cursor) {
            Object node = cursor.getValue();

            // Check for variable declaration: def foo = "bar" or val foo = "bar"
            if (matchVariableDeclarations && node instanceof J.VariableDeclarations.NamedVariable) {
                J.VariableDeclarations.NamedVariable var = (J.VariableDeclarations.NamedVariable) node;
                if (var.getInitializer() instanceof J.Literal) {
                    J.Literal literal = (J.Literal) var.getInitializer();
                    if (literal.getValue() instanceof String) {
                        String name = var.getSimpleName();
                        if (propertyName == null || propertyName.equals(name)) {
                            return new ExtraProperty(
                                    cursor,
                                    name,
                                    (String) literal.getValue(),
                                    PropertySyntax.VARIABLE_DECLARATION
                            );
                        }
                    }
                }
            }

            // Check for assignment: ext { foo = "bar" } or ext.foo = "bar"
            if (node instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) node;
                if (!(assignment.getAssignment() instanceof J.Literal)) {
                    return null;
                }
                J.Literal literal = (J.Literal) assignment.getAssignment();
                if (!(literal.getValue() instanceof String)) {
                    return null;
                }

                String name = null;
                PropertySyntax syntax = null;

                // Check for ext { foo = "bar" }
                if (assignment.getVariable() instanceof J.Identifier) {
                    name = ((J.Identifier) assignment.getVariable()).getSimpleName();
                    J.MethodInvocation enclosingMethod = cursor.firstEnclosing(J.MethodInvocation.class);
                    if (enclosingMethod != null && "ext".equals(enclosingMethod.getSimpleName())) {
                        syntax = PropertySyntax.EXT_BLOCK_ASSIGNMENT;
                    }
                }
                // Check for ext.foo = "bar"
                else if (assignment.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();
                    name = fieldAccess.getSimpleName();
                    if ((fieldAccess.getTarget() instanceof J.Identifier &&
                            "ext".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName())) ||
                            (fieldAccess.getTarget() instanceof J.FieldAccess &&
                                    "ext".equals(((J.FieldAccess) fieldAccess.getTarget()).getSimpleName()))) {
                        syntax = PropertySyntax.EXT_FIELD_ACCESS;
                    }
                }

                if (name != null && syntax != null && (propertyName == null || propertyName.equals(name))) {
                    return new ExtraProperty(cursor, name, (String) literal.getValue(), syntax);
                }
            }

            // Check for ext.set("foo", "bar")
            if (node instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) node;
                if ("set".equals(method.getSimpleName()) &&
                        method.getSelect() instanceof J.Identifier &&
                        "ext".equals(((J.Identifier) method.getSelect()).getSimpleName()) &&
                        method.getArguments().size() == 2) {

                    if (!(method.getArguments().get(0) instanceof J.Literal)) {
                        return null;
                    }
                    J.Literal keyLiteral = (J.Literal) method.getArguments().get(0);
                    if (!(keyLiteral.getValue() instanceof String)) {
                        return null;
                    }
                    String name = (String) keyLiteral.getValue();

                    if (!(method.getArguments().get(1) instanceof J.Literal)) {
                        return null;
                    }
                    J.Literal valueLiteral = (J.Literal) method.getArguments().get(1);
                    if (!(valueLiteral.getValue() instanceof String)) {
                        return null;
                    }

                    if (propertyName == null || propertyName.equals(name)) {
                        return new ExtraProperty(
                                cursor,
                                name,
                                (String) valueLiteral.getValue(),
                                PropertySyntax.EXT_SET_METHOD
                        );
                    }
                }
            }

            return null;
        }
    }
}
