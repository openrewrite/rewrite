/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import java.util.Comparator;
import java.util.UUID;

public abstract class CoordinateBuilder {
    J tree;

    CoordinateBuilder(J tree) {
        this.tree = tree;
    }

    JavaCoordinates before(Space.Location location) {
        return new JavaCoordinates(tree, location, JavaCoordinates.Mode.BEFORE, null);
    }

    JavaCoordinates after(@SuppressWarnings("SameParameterValue") Space.Location location) {
        return new JavaCoordinates(tree, location, JavaCoordinates.Mode.AFTER, null);
    }

    JavaCoordinates replace(Space.Location location) {
        return new JavaCoordinates(tree, location, JavaCoordinates.Mode.REPLACEMENT, null);
    }

    /**
     * Even though statements are not a superset of expressions,
     * this provides a way for statements that are
     * also expressions to have a generic coordinate.
     */
    public static class Statement extends Expression {
        public Statement(org.openrewrite.java.tree.Statement tree) {
            super(tree);
        }

        public JavaCoordinates after() {
            return after(Space.Location.STATEMENT_PREFIX);
        }

        public JavaCoordinates before() {
            return before(Space.Location.STATEMENT_PREFIX);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.STATEMENT_PREFIX);
        }
    }

    public static class Expression extends CoordinateBuilder {
        /**
         * @param tree This is of type {@link J} only so that
         *             statements that are not expressions can call
         *             super on this type. Since {@link Statement}
         *             overrides {@link #replace}, this is not
         *             otherwise a problem.
         */
        public Expression(J tree) {
            super(tree);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.EXPRESSION_PREFIX);
        }
    }

    public static class Annotation extends Expression {
        Annotation(J.Annotation tree) {
            super(tree);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.ANNOTATION_PREFIX);
        }

        public JavaCoordinates replaceArguments() {
            return replace(Space.Location.ANNOTATION_ARGUMENTS);
        }
    }

    public static class Block extends Statement {
        Block(J.Block tree) {
            super(tree);
        }

        public JavaCoordinates firstStatement() {
            if (((J.Block) tree).getStatements().isEmpty()) {
                return lastStatement();
            } else {
                return ((J.Block) tree).getStatements().get(0).getCoordinates().before();
            }
        }

        public JavaCoordinates addStatement(Comparator<org.openrewrite.java.tree.Statement> idealOrdering) {
            return new JavaCoordinates(tree, Space.Location.BLOCK_END, JavaCoordinates.Mode.BEFORE, idealOrdering);
        }

        public JavaCoordinates addMethodDeclaration(Comparator<J.MethodDeclaration> idealOrdering) {
            Comparator<UUID> natural = Comparator.naturalOrder();
            return addStatement((org.openrewrite.java.tree.Statement s1, org.openrewrite.java.tree.Statement s2) -> s1 instanceof J.MethodDeclaration && s2 instanceof J.MethodDeclaration ?
                    idealOrdering.compare((J.MethodDeclaration) s1, (J.MethodDeclaration) s2) :
                    natural.compare(s1.getId(), s2.getId())
            );
        }

        public JavaCoordinates lastStatement() {
            return before(Space.Location.BLOCK_END);
        }
    }

    public static class ClassDeclaration extends Statement {
        ClassDeclaration(J.ClassDeclaration tree) {
            super(tree);
        }

        /**
         * @param idealOrdering The new annotation will be inserted in as close to an ideal ordering
         *                      as possible, understanding that the existing annotations may not be
         *                      ordered according to the comparator.
         * @return A variable with a new annotation, inserted before the annotation it would appear
         * before in an ideal ordering, or as the last annotation if it would not appear before any
         * existing annotations in an ideal ordering.
         */
        public JavaCoordinates addAnnotation(Comparator<J.Annotation> idealOrdering) {
            return new JavaCoordinates(tree, Space.Location.ANNOTATIONS, JavaCoordinates.Mode.BEFORE, idealOrdering);
        }

        public JavaCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public JavaCoordinates replaceTypeParameters() {
            return replace(Space.Location.TYPE_PARAMETERS);
        }

        public JavaCoordinates replaceExtendsClause() {
            return replace(Space.Location.EXTENDS);
        }

        public JavaCoordinates replaceImplementsClause() {
            return replace(Space.Location.IMPLEMENTS);
        }

        public JavaCoordinates addImplementsClause() {
            return new JavaCoordinates(tree, Space.Location.IMPLEMENTS, JavaCoordinates.Mode.AFTER, null);
        }
    }

    public static class FieldAccess extends CoordinateBuilder {
        public FieldAccess(J.FieldAccess tree) {
            super(tree);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.FIELD_ACCESS_PREFIX);
        }
    }

    public static class Identifier extends Expression {
        public Identifier(J.Identifier tree) {
            super(tree);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.IDENTIFIER_PREFIX);
        }
    }

    public static class Lambda {
        public static class Parameters extends CoordinateBuilder {
            Parameters(J.Lambda.Parameters tree) {
                super(tree);
            }

            public JavaCoordinates replace() {
                return replace(Space.Location.LAMBDA_PARAMETERS_PREFIX);
            }
        }
    }

    public static class MethodDeclaration extends Statement {
        MethodDeclaration(J.MethodDeclaration tree) {
            super(tree);
        }

        /**
         * @param idealOrdering The new annotation will be inserted in as close to an ideal ordering
         *                      as possible, understanding that the existing annotations may not be
         *                      ordered according to the comparator.
         * @return A method with a new annotation, inserted before the annotation it would appear
         * before in an ideal ordering, or as the last annotation if it would not appear before any
         * existing annotations in an ideal ordering.
         */
        public JavaCoordinates addAnnotation(Comparator<J.Annotation> idealOrdering) {
            return new JavaCoordinates(tree, Space.Location.ANNOTATIONS, JavaCoordinates.Mode.BEFORE, idealOrdering);
        }

        public JavaCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public JavaCoordinates replaceTypeParameters() {
            return replace(Space.Location.TYPE_PARAMETERS);
        }

        public JavaCoordinates replaceParameters() {
            return replace(Space.Location.METHOD_DECLARATION_PARAMETERS);
        }

        public JavaCoordinates replaceThrows() {
            return replace(Space.Location.THROWS);
        }

        public JavaCoordinates replaceBody() {
            return replace(Space.Location.BLOCK_PREFIX);
        }
    }

    public static class MethodInvocation extends Statement {
        MethodInvocation(J.MethodInvocation tree) {
            super(tree);
        }

        public JavaCoordinates replaceArguments() {
            return replace(Space.Location.METHOD_INVOCATION_ARGUMENTS);
        }

        /**
         * Indicates replacement of the invocation's name and argument list, while preserving its select.
         */
        public JavaCoordinates replaceMethod() {
            return replace(Space.Location.METHOD_INVOCATION_NAME);
        }
    }

    public static class Unary extends Statement {
        Unary(J.Unary tree) {
            super(tree);
        }

        @Override
        JavaCoordinates after(Space.Location location) {
            return after(isModifying() ? Space.Location.STATEMENT_PREFIX : Space.Location.EXPRESSION_PREFIX);
        }

        @Override
        public JavaCoordinates before() {
            return before(isModifying() ? Space.Location.STATEMENT_PREFIX : Space.Location.EXPRESSION_PREFIX);
        }

        @Override
        public JavaCoordinates replace() {
            return replace(isModifying() ? Space.Location.STATEMENT_PREFIX : Space.Location.EXPRESSION_PREFIX);
        }

        private boolean isModifying() {
            return ((J.Unary) tree).getOperator().isModifying();
        }
    }

    public static class Package extends Statement {
        Package(J.Package tree) {
            super(tree);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.PACKAGE_PREFIX);
        }
    }

    public static class VariableDeclarations extends Statement {
        VariableDeclarations(J.VariableDeclarations tree) {
            super(tree);
        }

        public JavaCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public JavaCoordinates addAnnotation(Comparator<J.Annotation> idealOrdering) {
            return new JavaCoordinates(tree, Space.Location.ANNOTATIONS, JavaCoordinates.Mode.BEFORE, idealOrdering);
        }
    }

    public static class Yield extends Statement {
        Yield(J.Yield tree) {
            super(tree);
        }

        public JavaCoordinates replace() {
            return replace(Space.Location.YIELD_PREFIX);
        }
    }
}
