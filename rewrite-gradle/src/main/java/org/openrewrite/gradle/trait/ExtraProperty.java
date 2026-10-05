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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.marker.IndexedAccess;
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
 *   <li>ext subscript access: {@code ext['propertyName'] = 'value'} or Kotlin {@code extra["propertyName"] = "value"}</li>
 *   <li>set() in ext block: {@code ext { set('propertyName', 'value') }}</li>
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
        EXT_SET_METHOD,
        /**
         * Subscript access: {@code ext['foo'] = "bar"}
         */
        EXT_SUBSCRIPT_ACCESS,
        /**
         * set() method inside ext block: {@code ext { set('foo', 'bar') }}
         */
        EXT_BLOCK_SET_METHOD
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
            case EXT_SUBSCRIPT_ACCESS:
                J.Assignment assignment = (J.Assignment) tree;
                if (assignment.getAssignment() instanceof J.Literal) {
                    J.Literal literal = (J.Literal) assignment.getAssignment();
                    return assignment.withAssignment(ChangeStringLiteral.withStringValue(literal, newValue));
                }
                break;

            case EXT_SET_METHOD:
            case EXT_BLOCK_SET_METHOD:
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
            String name;
            Expression value;
            PropertySyntax syntax;
            if (node instanceof J.VariableDeclarations.NamedVariable) {
                if (!matchVariableDeclarations) {
                    return null;
                }
                J.VariableDeclarations.NamedVariable var = (J.VariableDeclarations.NamedVariable) node;
                name = var.getSimpleName();
                value = var.getInitializer();
                syntax = PropertySyntax.VARIABLE_DECLARATION;
            } else if (node instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) node;
                syntax = assignmentSyntax(assignment.getVariable(), cursor);
                name = assignedName(assignment.getVariable());
                value = assignment.getAssignment();
            } else if (node instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) node;
                syntax = setSyntax(method, cursor);
                name = syntax == null ? null : stringLiteral(method.getArguments().get(0));
                value = syntax == null ? null : method.getArguments().get(1);
            } else {
                return null;
            }
            String literal = stringLiteral(value);
            if (syntax == null || name == null || literal == null || (propertyName != null && !propertyName.equals(name))) {
                return null;
            }
            return new ExtraProperty(cursor, name, literal, syntax);
        }

        /**
         * The property assigned at the cursor through any {@code ext}/{@code extra} form, whatever the value's shape.
         */
        public static @Nullable String assignedProperty(Cursor cursor) {
            Object node = cursor.getValue();
            if (node instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) node;
                return assignmentSyntax(assignment.getVariable(), cursor) == null ? null : assignedName(assignment.getVariable());
            }
            if (node instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) node;
                return setSyntax(method, cursor) == null ? null : stringLiteral(method.getArguments().get(0));
            }
            return null;
        }

        private static @Nullable PropertySyntax assignmentSyntax(Expression variable, Cursor cursor) {
            if (variable instanceof J.Identifier) {
                J.MethodInvocation enclosingMethod = cursor.firstEnclosing(J.MethodInvocation.class);
                return enclosingMethod != null && "ext".equals(enclosingMethod.getSimpleName()) ? PropertySyntax.EXT_BLOCK_ASSIGNMENT : null;
            }
            if (variable instanceof J.FieldAccess) {
                return isExt(((J.FieldAccess) variable).getTarget()) ? PropertySyntax.EXT_FIELD_ACCESS : null;
            }
            if (variable instanceof G.Binary) {
                G.Binary binary = (G.Binary) variable;
                return binary.getOperator() == G.Binary.Type.Access && isExt(binary.getLeft()) && stringLiteral(binary.getRight()) != null ?
                        PropertySyntax.EXT_SUBSCRIPT_ACCESS : null;
            }
            if (variable instanceof J.MethodInvocation) {
                // Kotlin extra["foo"] parses as an indexed <get> invocation
                J.MethodInvocation get = (J.MethodInvocation) variable;
                return get.getMarkers().findFirst(IndexedAccess.class).isPresent() && isExt(get.getSelect()) &&
                        get.getArguments().size() == 1 && stringLiteral(get.getArguments().get(0)) != null ?
                        PropertySyntax.EXT_SUBSCRIPT_ACCESS : null;
            }
            return null;
        }

        private static @Nullable String assignedName(Expression variable) {
            if (variable instanceof J.Identifier) {
                return ((J.Identifier) variable).getSimpleName();
            }
            if (variable instanceof J.FieldAccess) {
                return ((J.FieldAccess) variable).getSimpleName();
            }
            if (variable instanceof G.Binary) {
                return stringLiteral(((G.Binary) variable).getRight());
            }
            if (variable instanceof J.MethodInvocation) {
                return stringLiteral(((J.MethodInvocation) variable).getArguments().get(0));
            }
            return null;
        }

        private static @Nullable PropertySyntax setSyntax(J.MethodInvocation method, Cursor cursor) {
            if (!"set".equals(method.getSimpleName()) || method.getArguments().size() != 2) {
                return null;
            }
            if (isExt(method.getSelect())) {
                return PropertySyntax.EXT_SET_METHOD;
            }
            return withinBlock(cursor, "ext") ? PropertySyntax.EXT_BLOCK_SET_METHOD : null;
        }

        private static boolean isExt(@Nullable Expression expression) {
            String name = expression instanceof J.Identifier ? ((J.Identifier) expression).getSimpleName() :
                    expression instanceof J.FieldAccess ? ((J.FieldAccess) expression).getSimpleName() : null;
            return "ext".equals(name) || "extra".equals(name);
        }

        private static @Nullable String stringLiteral(@Nullable Expression expression) {
            return expression instanceof J.Literal && ((J.Literal) expression).getValue() instanceof String ?
                    (String) ((J.Literal) expression).getValue() : null;
        }
    }
}
