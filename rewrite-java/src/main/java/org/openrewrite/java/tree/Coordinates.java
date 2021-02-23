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

public abstract class Coordinates {
    J tree;

    Coordinates(J tree) {
        this.tree = tree;
    }

    JavaCoordinates insert(Space.Location location) {
        return new JavaCoordinates(tree, location, JavaCoordinates.Mode.INSERTION, null);
    }

    JavaCoordinates replace(Space.Location location) {
        return new JavaCoordinates(tree, location, JavaCoordinates.Mode.REPLACEMENT, null);
    }

    public JavaCoordinates replace() {
        return new JavaCoordinates(tree, null, JavaCoordinates.Mode.REPLACEMENT, null);
    }

    public abstract JavaCoordinates before();

    public static class AnnotatedType extends Coordinates {
        AnnotatedType(J.AnnotatedType tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ANNOTATED_TYPE_PREFIX);
        }
    }

    public static class Annotation extends Coordinates {
        Annotation(J.Annotation tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ANNOTATION_PREFIX);
        }
    }

    public static class ArrayAccess extends Coordinates {
        ArrayAccess(J.ArrayAccess tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ARRAY_ACCESS_PREFIX);
        }
    }

    public static class ArrayDimension extends Coordinates {
        ArrayDimension(J.ArrayDimension tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.DIMENSION_PREFIX);
        }
    }

    public static class ArrayType extends Coordinates {
        ArrayType(J.ArrayType tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ARRAY_TYPE_PREFIX);
        }
    }

    public static class Assert extends Coordinates {
        Assert(J.Assert tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ASSERT_PREFIX);
        }
    }

    public static class Assignment extends Coordinates {
        Assignment(J.Assignment tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ASSIGNMENT_PREFIX);
        }
    }

    public static class AssignmentOperation extends Coordinates {
        AssignmentOperation(J.AssignmentOperation tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ASSIGNMENT_OPERATION_PREFIX);
        }
    }

    public static class Binary extends Coordinates {
        Binary(J.Binary tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.BINARY_PREFIX);
        }
    }

    public static class Block extends Coordinates {
        Block(J.Block tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.BLOCK_PREFIX);
        }

        public JavaCoordinates lastStatement() {
            return insert(Space.Location.BLOCK_END);
        }
    }

    public static class Break extends Coordinates {
        Break(J.Break tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.BREAK_PREFIX);
        }
    }

    public static class Case extends Coordinates {
        Case(J.Case tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.CASE_PREFIX);
        }
    }

    public static class ClassDeclaration extends Coordinates {
        ClassDeclaration(J.ClassDeclaration tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.CLASS_DECLARATION_PREFIX);
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
            return new JavaCoordinates(tree, Space.Location.ANNOTATIONS, JavaCoordinates.Mode.INSERTION, idealOrdering);
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

        public JavaCoordinates replaceBody() {
            return replace(Space.Location.BLOCK_PREFIX);
        }

        public static class Kind extends Coordinates {
            Kind(J.ClassDeclaration.Kind tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.CLASS_KIND);
            }
        }
    }

    public static class CompilationUnit extends Coordinates {
        CompilationUnit(J.CompilationUnit tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.COMPILATION_UNIT_PREFIX);
        }
    }

    public static class Continue extends Coordinates {
        Continue(J.Continue tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.CONTINUE_PREFIX);
        }
    }

    public static class ControlParentheses extends Coordinates {
        ControlParentheses(J.ControlParentheses<?> tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.CONTROL_PARENTHESES_PREFIX);
        }
    }

    public static class DoWhileLoop extends Coordinates {
        DoWhileLoop(J.DoWhileLoop tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.DO_WHILE_PREFIX);
        }
    }

    public static class Empty extends Coordinates {
        Empty(J.Empty tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.EMPTY_PREFIX);
        }
    }

    public static class EnumValue extends Coordinates {
        EnumValue(J.EnumValue tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ENUM_VALUE_PREFIX);
        }
    }

    public static class EnumValueSet extends Coordinates {
        EnumValueSet(J.EnumValueSet tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.ENUM_VALUE_SET_PREFIX);
        }
    }

    public static class FieldAccess extends Coordinates {
        FieldAccess(J.FieldAccess tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.FIELD_ACCESS_PREFIX);
        }
    }

    public static class ForEachLoop extends Coordinates {
        ForEachLoop(J.ForEachLoop tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.FOR_EACH_LOOP_PREFIX);
        }

        public static class Control extends Coordinates {
            Control(J.ForEachLoop.Control tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.FOR_EACH_CONTROL_PREFIX);
            }
        }
    }

    public static class ForLoop extends Coordinates {
        ForLoop(J.ForLoop tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.FOR_PREFIX);
        }


        public static class Control extends Coordinates {
            Control(J.ForLoop.Control tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.FOR_CONTROL_PREFIX);
            }
        }
    }

    public static class Identifier extends Coordinates {
        Identifier(J.Identifier tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.IDENTIFIER_PREFIX);
        }
    }

    public static class If extends Coordinates {
        If(J.If tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.IF_PREFIX);
        }

        public static class Else extends Coordinates {
            Else(J.If.Else tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.FOR_CONTROL_PREFIX);
            }
        }
    }

    public static class Import extends Coordinates {
        Import(J.Import tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.IMPORT_PREFIX);
        }
    }

    public static class InstanceOf extends Coordinates {
        InstanceOf(J.InstanceOf tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.INSTANCEOF_PREFIX);
        }
    }

    public static class Label extends Coordinates {
        Label(J.Label tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.LABEL_PREFIX);
        }
    }

    public static class Lambda extends Coordinates {
        Lambda(J.Lambda tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.LAMBDA_PREFIX);
        }

        public static class Parameters extends Coordinates {

            Parameters(J.Lambda.Parameters tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.LAMBDA_PARAMETERS_PREFIX);
            }
        }
    }

    public static class Literal extends Coordinates {
        Literal(J.Literal tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.LITERAL_PREFIX);
        }
    }

    public static class MemberReference extends Coordinates {
        MemberReference(J.MemberReference tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.MEMBER_REFERENCE_PREFIX);
        }
    }

    public static class MethodDeclaration extends Coordinates {
        MethodDeclaration(J.MethodDeclaration tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.METHOD_DECLARATION_PREFIX);
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
            return new JavaCoordinates(tree, Space.Location.ANNOTATIONS, JavaCoordinates.Mode.INSERTION, idealOrdering);
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

    public static class MethodInvocation extends Coordinates {
        MethodInvocation(J.MethodInvocation tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.METHOD_INVOCATION_PREFIX);
        }

        public JavaCoordinates replaceArguments() {
            return replace(Space.Location.METHOD_INVOCATION_ARGUMENTS);
        }

    }

    public static class Modifier extends Coordinates {
        Modifier(J.Modifier tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.MODIFIER_PREFIX);
        }
    }

    public static class MultiCatch extends Coordinates {
        MultiCatch(J.MultiCatch tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.MULTI_CATCH_PREFIX);
        }
    }

    public static class NewArray extends Coordinates {
        NewArray(J.NewArray tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.NEW_ARRAY_PREFIX);
        }
    }

    public static class NewClass extends Coordinates {
        NewClass(J.NewClass tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.NEW_CLASS_PREFIX);
        }
    }

    public static class Package extends Coordinates {
        Package(J.Package tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.PACKAGE_PREFIX);
        }
    }

    public static class ParameterizedType extends Coordinates {
        ParameterizedType(J.ParameterizedType tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.PARAMETERIZED_TYPE_PREFIX);
        }
    }

    public static class Parentheses extends Coordinates {
        Parentheses(J.Parentheses<?> tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.PARENTHESES_PREFIX);
        }
    }

    public static class Primitive extends Coordinates {
        Primitive(J.Primitive tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.PRIMITIVE_PREFIX);
        }
    }

    public static class Return extends Coordinates {
        Return(J.Return tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.RETURN_PREFIX);
        }
    }

    public static class Switch extends Coordinates {
        Switch(J.Switch tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.SWITCH_PREFIX);
        }
    }

    public static class Synchronized extends Coordinates {
        Synchronized(J.Synchronized tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.SYNCHRONIZED_PREFIX);
        }
    }

    public static class Ternary extends Coordinates {
        Ternary(J.Ternary tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.TERNARY_PREFIX);
        }
    }

    public static class Throw extends Coordinates {
        Throw(J.Throw tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.THROW_PREFIX);
        }
    }

    public static class Try extends Coordinates {
        Try(J.Try tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.TRY_PREFIX);
        }

        public static class Catch extends Coordinates {
            Catch(J.Try.Catch catzch) {
                super(catzch);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.CATCH_PREFIX);
            }
        }

        public static class Resource extends Coordinates {
            Resource(J.Try.Resource tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.TRY_RESOURCE);
            }
        }
    }

    public static class TypeCast extends Coordinates {
        TypeCast(J.TypeCast tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.TYPE_CAST_PREFIX);
        }
    }

    public static class TypeParameter extends Coordinates {
        TypeParameter(J.TypeParameter tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.TYPE_PARAMETERS_PREFIX);
        }

        public JavaCoordinates bounds() {
            return insert(Space.Location.TYPE_BOUNDS);
        }
    }

    public static class TypeParameters extends Coordinates {
        TypeParameters(J.TypeParameters tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.TYPE_PARAMETERS_PREFIX);
        }
    }

    public static class Unary extends Coordinates {
        Unary(J.Unary tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.UNARY_PREFIX);
        }
    }

    public static class VariableDeclarations extends Coordinates {
        VariableDeclarations(J.VariableDeclarations tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.VARIABLE_DECLARATIONS_PREFIX);
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
            return new JavaCoordinates(tree, Space.Location.ANNOTATIONS, JavaCoordinates.Mode.INSERTION, idealOrdering);
        }

        public JavaCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public static class NamedVar extends Coordinates {
            NamedVar(J.VariableDeclarations.NamedVariable tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates before() {
                return insert(Space.Location.VARIABLE_PREFIX);
            }
        }
    }

    public static class WhileLoop extends Coordinates {
        WhileLoop(J.WhileLoop tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.WHILE_PREFIX);
        }
    }

    public static class Wildcard extends Coordinates {
        Wildcard(J.Wildcard tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates before() {
            return insert(Space.Location.WILDCARD_PREFIX);
        }
    }
}
