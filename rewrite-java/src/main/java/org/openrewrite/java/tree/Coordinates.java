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

import org.openrewrite.internal.lang.Nullable;

public abstract class Coordinates <J2 extends J> {

    protected J2 tree;

    protected Coordinates(J2 tree) {
        this.tree = tree;
    }

    protected JavaCoordinates<?> create(@Nullable Space.Location location) {
        return new JavaCoordinates<>(tree, location);
    }

    public JavaCoordinates<?> replaceThis() {
        return create(Space.Location.REPLACE);
    }

    public abstract JavaCoordinates<?> before();

    public static class AnnotatedType extends Coordinates<J.AnnotatedType> {

        protected AnnotatedType(J.AnnotatedType tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ANNOTATED_TYPE_PREFIX); }
    }

    public static class Annotation extends Coordinates<J.Annotation> {

        protected Annotation(J.Annotation tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ANNOTATION_PREFIX); }
    }
    public static class ArrayAccess extends Coordinates<J.ArrayAccess> {

        protected ArrayAccess(J.ArrayAccess tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ARRAY_ACCESS_PREFIX); }
    }

    public static class ArrayDimension extends Coordinates<J.ArrayDimension> {

        protected ArrayDimension(J.ArrayDimension tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.DIMENSION_PREFIX); }
    }

    public static class ArrayType extends Coordinates<J.ArrayType> {

        protected ArrayType(J.ArrayType tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ARRAY_TYPE_PREFIX); }
    }
    public static class Assert extends Coordinates<J.Assert> {

        protected Assert(J.Assert tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ASSERT_PREFIX); }
    }
    public static class Assign extends Coordinates<J.Assign> {

        protected Assign(J.Assign tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ASSIGN_PREFIX); }
    }
    public static class AssignOp extends Coordinates<J.AssignOp> {

        protected AssignOp(J.AssignOp tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ASSIGN_OP_OPERATOR); }
    }
    public static class Binary extends Coordinates<J.Binary> {

        protected Binary(J.Binary tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.BINARY_PREFIX); }
    }
    public static class Block extends Coordinates<J.Block> {

        protected Block(J.Block tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.BLOCK_PREFIX); }
        public JavaCoordinates<?> lastStatement() { return create(Space.Location.BLOCK_END); }
    }
    public static class Break extends Coordinates<J.Break> {

        protected Break(J.Break tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.BREAK_PREFIX); }
    }
    public static class Case extends Coordinates<J.Case> {

        protected Case(J.Case tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.CASE_PREFIX); }
    }
    public static class ClassDecl extends Coordinates<J.ClassDecl> {

        protected ClassDecl(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.CLASS_DECL_PREFIX); }

        /**
         * Intended to add an annotation (represented by the JavaTemplate) as the last annotation associated with the
         * class.
         *
         * @return annotations replacement coordinates
         */
        public JavaCoordinates<?> replaceAnnotations() { return create(Space.Location.ANNOTATION_PREFIX); }

        /**
         * Intended for replacement semantics, where the type parameters will be entirely replaced by the code
         * generated via JavaTemplate. Any template should EXCLUDE &lt; and &gt; as those are rendered by the
         * container.
         *
         * @return type parameters replacement coordinates
         */
        public JavaCoordinates<?> replaceTypeParameters() {return create(Space.Location.TYPE_PARAMETER_SUFFIX); }

        /**
         * Intended for replacement semantics, where the extends clause will be entirely replaced by the code
         * generated via JavaTemplate
         *
         * @return extends clause replacement coordinates
         */
        public JavaCoordinates<?> replaceExtendsClause() { return create(Space.Location.EXTENDS); }

        /**
         * Intended for replacement semantics, where the implements clause will be entirely replaced by the code
         * generated via JavaTemplate
         *
         * @return implements clause replacement coordinates
         */
        public JavaCoordinates<?> replaceImplementsClause() { return create(Space.Location.IMPLEMENTS); }

        /**
         * Intended for replacement semantics, where the class body will be entirely replaced by the code
         * generated via JavaTemplate
         *
         * @return class body replacement coordinates
         */
        public JavaCoordinates<?> replaceBody() { return create(Space.Location.BLOCK_END); }

    }
    public static class CompilationUnit extends Coordinates<J.CompilationUnit> {

        protected CompilationUnit(J.CompilationUnit tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.COMPILATION_UNIT_PREFIX); }
    }
    public static class Continue extends Coordinates<J.Continue> {

        protected Continue(J.Continue tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.CONTINUE_PREFIX); }
    }
    public static class ControlParentheses<J2 extends J> extends Coordinates<J.ControlParentheses<J2>> {

        protected ControlParentheses(J.ControlParentheses<J2> tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.CONTROL_PARENTHESES_PREFIX); }
    }
    public static class DoWhileLoop extends Coordinates<J.DoWhileLoop> {

        protected DoWhileLoop(J.DoWhileLoop tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.DO_WHILE_PREFIX); }
    }
    public static class Empty extends Coordinates<J.Empty> {

        protected Empty(J.Empty tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.EMPTY_PREFIX); }
    }
    public static class EnumValue extends Coordinates<J.EnumValue> {

        protected EnumValue(J.EnumValue tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ENUM_VALUE_PREFIX); }
    }
    public static class EnumValueSet extends Coordinates<J.EnumValueSet> {

        protected EnumValueSet(J.EnumValueSet tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.ENUM_VALUE_SET_PREFIX); }
    }
    public static class FieldAccess extends Coordinates<J.FieldAccess> {

        protected FieldAccess(J.FieldAccess tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.FIELD_ACCESS_PREFIX); }
    }
    public static class ForEachLoop extends Coordinates<J.ForEachLoop> {

        protected ForEachLoop(J.ForEachLoop tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.FOR_EACH_LOOP_PREFIX); }

        public static class Control extends Coordinates<J.ForEachLoop.Control> {
            protected Control(J.ForEachLoop.Control tree) {super(tree); }

            @Override
            public JavaCoordinates<?> before() { return create(Space.Location.FOR_EACH_CONTROL_PREFIX); }
        }
    }
    public static class ForLoop extends Coordinates<J.ForLoop> {

        protected ForLoop(J.ForLoop tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.FOR_PREFIX); }


        public static class Control extends Coordinates<J.ForLoop.Control> {
            protected Control(J.ForLoop.Control tree) {super(tree); }

            @Override
            public JavaCoordinates<?> before() { return create(Space.Location.FOR_CONTROL_PREFIX); }
        }
    }
    public static class Ident extends Coordinates<J.Ident> {

        protected Ident(J.Ident tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.IDENTIFIER_PREFIX); }
    }
    public static class If extends Coordinates<J.If> {

        protected If(J.If tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.IF_PREFIX); }

        public static class Else extends Coordinates<J.If.Else> {
            protected Else(J.If.Else tree) {super(tree); }

            @Override
            public JavaCoordinates<?> before() { return create(Space.Location.FOR_CONTROL_PREFIX); }
        }
    }
    public static class Import extends Coordinates<J.Import> {

        protected Import(J.Import tree) {super(tree); }

        @Override
        public JavaCoordinates<?> before() { return create(Space.Location.IMPORT_PREFIX); }
    }

    public static class InstanceOf extends Coordinates<J.InstanceOf> {

        protected InstanceOf(J.InstanceOf tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.INSTANCEOF_PREFIX);
        }
    }

    public static class Label extends Coordinates<J.Label> {

        protected Label(J.Label tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.LABEL_PREFIX);
        }
    }

    public static class Lambda extends Coordinates<J.Lambda> {

        protected Lambda(J.Lambda tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.LAMBDA_PREFIX);
        }

        public static class Parameters extends Coordinates<J.Lambda.Parameters> {

            protected Parameters(J.Lambda.Parameters tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates<?> before() {
                return create(Space.Location.LAMBDA_PARAMETERS_PREFIX);
            }
        }
    }

    public static class Literal extends Coordinates<J.Literal> {

        protected Literal(J.Literal tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.LITERAL_PREFIX);
        }
    }

    public static class MemberReference extends Coordinates<J.MemberReference> {

        protected MemberReference(J.MemberReference tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.MEMBER_REFERENCE_PREFIX);
        }
    }

    public static class MethodDecl extends Coordinates<J.MethodDecl> {

        protected MethodDecl(J.MethodDecl tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.METHOD_DECL_PREFIX);
        }

        public JavaCoordinates<?> replaceAnnotations() { return create(Space.Location.ANNOTATION_PREFIX); }
        public JavaCoordinates<?> replaceTypeParameters() { return create(Space.Location.TYPE_PARAMETER_SUFFIX); }
        public JavaCoordinates<?> replaceParameters() { return create(Space.Location.METHOD_DECL_PARAMETERS); }
        public JavaCoordinates<?> replaceThrows() { return create(Space.Location.THROWS); }

        /**
         * Intended for replacement semantics, where the method body specified will be entirely replaced by the code
         * generated via JavaTemplate
         *
         * @return method body replacement coordinates
         */
        public JavaCoordinates<?> replaceBody() {
            return create(Space.Location.BLOCK_END);
        }
    }

    public static class MethodInvocation extends Coordinates<J.MethodInvocation> {

        protected MethodInvocation(J.MethodInvocation tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {return create(Space.Location.METHOD_INVOCATION_PREFIX); }
        public JavaCoordinates<?> replaceArguments() { return create(Space.Location.METHOD_INVOCATION_ARGUMENTS); }

    }

    public static class Modifier extends Coordinates<J.Modifier> {

        protected Modifier(J.Modifier tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.MODIFIER_PREFIX);
        }
    }

    public static class MultiCatch extends Coordinates<J.MultiCatch> {

        protected MultiCatch(J.MultiCatch tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.MULTI_CATCH_PREFIX);
        }
    }

    public static class NewArray extends Coordinates<J.NewArray> {

        protected NewArray(J.NewArray tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.NEW_ARRAY_PREFIX);
        }
    }

    public static class NewClass extends Coordinates<J.NewClass> {

        protected NewClass(J.NewClass tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.NEW_CLASS_PREFIX);
        }
    }

    public static class Package extends Coordinates<J.Package> {

        protected Package(J.Package tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.PACKAGE_PREFIX);
        }
    }

    public static class ParameterizedType extends Coordinates<J.ParameterizedType> {

        protected ParameterizedType(J.ParameterizedType tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.PARAMETERIZED_TYPE_PREFIX);
        }
    }

    public static class Parentheses<J2 extends J> extends Coordinates<J.Parentheses<J2>> {

        protected Parentheses(J.Parentheses<J2> tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.PARENTHESES_PREFIX);
        }
    }

    public static class Primitive extends Coordinates<J.Primitive> {

        protected Primitive(J.Primitive tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.PRIMITIVE_PREFIX);
        }
    }

    public static class Return extends Coordinates<J.Return> {

        protected Return(J.Return tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.RETURN_PREFIX);
        }
    }

    public static class Switch extends Coordinates<J.Switch> {

        protected Switch(J.Switch tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.SWITCH_PREFIX);
        }
    }

    public static class Synchronized extends Coordinates<J.Synchronized> {

        protected Synchronized(J.Synchronized tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.SYNCHRONIZED_PREFIX);
        }
    }

    public static class Ternary extends Coordinates<J.Ternary> {

        protected Ternary(J.Ternary tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.TERNARY_PREFIX);
        }
    }

    public static class Throw extends Coordinates<J.Throw> {

        protected Throw(J.Throw tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.THROW_PREFIX);
        }
    }

    public static class Try extends Coordinates<J.Try> {

        protected Try(J.Try tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.TRY_PREFIX);
        }

        public static class Catch extends Coordinates<J.Try.Catch> {

            protected Catch(J.Try.Catch catzch) {
                super(catzch);
            }

            @Override
            public JavaCoordinates<?> before() {
                return create(Space.Location.CATCH_PREFIX);
            }
        }

        public static class Resource extends Coordinates<J.Try.Resource> {

            protected Resource(J.Try.Resource tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates<?> before() {
                return create(Space.Location.TRY_RESOURCE);
            }
        }
    }

    public static class TypeCast extends Coordinates<J.TypeCast> {

        protected TypeCast(J.TypeCast tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.TYPE_CAST_PREFIX);
        }
    }

    public static class TypeParameter extends Coordinates<J.TypeParameter> {

        protected TypeParameter(J.TypeParameter tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.TYPE_PARAMETERS_PREFIX);
        }

        public JavaCoordinates<?> bounds() {
            return create(Space.Location.TYPE_BOUNDS);
        }
    }

    public static class Unary extends Coordinates<J.Unary> {

        protected Unary(J.Unary tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.UNARY_PREFIX);
        }
    }

    public static class VariableDecls extends Coordinates<J.VariableDecls> {

        protected VariableDecls(J.VariableDecls tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.MULTI_VARIABLE_PREFIX);
        }

        public static class NamedVar extends Coordinates<J.VariableDecls.NamedVar> {

            protected NamedVar(J.VariableDecls.NamedVar tree) {
                super(tree);
            }

            @Override
            public JavaCoordinates<?> before() {
                return create(Space.Location.VARIABLE_PREFIX);
            }
        }
    }

    public static class WhileLoop extends Coordinates<J.WhileLoop> {

        protected WhileLoop(J.WhileLoop tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.WHILE_PREFIX);
        }
    }

    public static class Wildcard extends Coordinates<J.Wildcard> {

        protected Wildcard(J.Wildcard tree) {
            super(tree);
        }

        @Override
        public JavaCoordinates<?> before() {
            return create(Space.Location.WILDCARD_PREFIX);
        }
    }
}
