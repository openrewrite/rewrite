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

public abstract class Coordinates {

    private J tree;

    protected Coordinates(J tree) {
        this.tree = tree;
    }

    protected JavaTreeCoordinates create(@Nullable Space.Location location) {
        return new JavaTreeCoordinates(tree, location);
    }

    public JavaTreeCoordinates around() {
        return create(null);
    }

    public abstract JavaTreeCoordinates before();

    class AnnotatedTypeCoordinates extends Coordinates {

        protected AnnotatedTypeCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AnnotationCoordinates extends Coordinates {

        protected AnnotationCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ArrayAccessCoordinates extends Coordinates {

        protected ArrayAccessCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ArrayDimensionCoordinates extends Coordinates {

        protected ArrayDimensionCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ArrayTypeCoordinates extends Coordinates {

        protected ArrayTypeCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AssertCoordinates extends Coordinates {

        protected AssertCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AssignCoordinates extends Coordinates {

        protected AssignCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AssignOpCoordinates extends Coordinates {

        protected AssignOpCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class BinaryCoordinates extends Coordinates {

        protected BinaryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class BlockCoordinates extends Coordinates {

        protected BlockCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class BreakCoordinates extends Coordinates {

        protected BreakCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class CaseCoordinates extends Coordinates {

        protected CaseCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ClassDeclCoordinates extends Coordinates {

        protected ClassDeclCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { return create(Space.Location.CLASS_DECL_PREFIX); }
        public JavaTreeCoordinates extending() {return create(Space.Location.EXTENDS); }

        //TODO MOAR!
    }
    class CompilationUnitCoordinates extends Coordinates {

        protected CompilationUnitCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ContinueCoordinates extends Coordinates {

        protected ContinueCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ControlParenthesesCoordinates extends Coordinates {

        protected ControlParenthesesCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class DoWhileLoopCoordinates extends Coordinates {

        protected DoWhileLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class EmptyCoordinates extends Coordinates {

        protected EmptyCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class EnumValueCoordinates extends Coordinates {

        protected EnumValueCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class EnumValueSetCoordinates extends Coordinates {

        protected EnumValueSetCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class FieldAccessCoordinates extends Coordinates {

        protected FieldAccessCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ForEachLoopCoordinates extends Coordinates {

        protected ForEachLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ForLoopCoordinates extends Coordinates {

        protected ForLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class IdentCoordinates extends Coordinates {

        protected IdentCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class IfCoordinates extends Coordinates {

        protected IfCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ImportCoordinates extends Coordinates {

        protected ImportCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }

// --------------------------------
// --------------------------------
// --------------------------------
// --------------------------------
// --------------------------------
// --------------------------------

    class InstanceOfCoordinates extends Coordinates {

        protected InstanceOfCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class LabelCoordinates extends Coordinates {

        protected LabelCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class LambdaCoordinates extends Coordinates {

        protected LambdaCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class LiteralCoordinates extends Coordinates {

        protected LiteralCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class MemberReferenceCoordinates extends Coordinates {

        protected MemberReferenceCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class MethodDeclCoordinates extends Coordinates {

        protected MethodDeclCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { return create(Space.Location.METHOD_DECL_PREFIX); }
        public JavaTreeCoordinates arguments() {return create(Space.Location.METHOD_DECL_ARGUMENTS); }

        //TODO MOAR!
    }
    class MethodInvocationCoordinates extends Coordinates {

        protected MethodInvocationCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ModifierCoordinates extends Coordinates {

        protected ModifierCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class MultiCatchCoordinates extends Coordinates {

        protected MultiCatchCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class NewArrayCoordinates extends Coordinates {

        protected NewArrayCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class NewClassCoordinates extends Coordinates {

        protected NewClassCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class PackageCoordinates extends Coordinates {

        protected PackageCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ParameterizedTypeCoordinates extends Coordinates {

        protected ParameterizedTypeCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ParenthesesCoordinates extends Coordinates {

        protected ParenthesesCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class PrimitiveCoordinates extends Coordinates {

        protected PrimitiveCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ReturnCoordinates extends Coordinates {

        protected ReturnCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class SwitchCoordinates extends Coordinates {

        protected SwitchCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class SynchronizedCoordinates extends Coordinates {

        protected SynchronizedCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TernaryCoordinates extends Coordinates {

        protected TernaryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ThrowCoordinates extends Coordinates {

        protected ThrowCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TryCoordinates extends Coordinates {

        protected TryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TypeCastCoordinates extends Coordinates {

        protected TypeCastCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TypeParameterCoordinates extends Coordinates {

        protected TypeParameterCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class UnaryCoordinates extends Coordinates {

        protected UnaryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class VariableDeclsCoordinates extends Coordinates {

        protected VariableDeclsCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class WhileLoopCoordinates extends Coordinates {

        protected WhileLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class WildcardCoordinates extends Coordinates {

        protected WildcardCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }

}
