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

public interface Coordinates {

    JavaTreeCoordinates around();
    JavaTreeCoordinates before();

    class AnnotatedTypeCoordinates extends AbstractCoordinates {

        protected AnnotatedTypeCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AnnotationCoordinates extends AbstractCoordinates {

        protected AnnotationCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ArrayAccessCoordinates extends AbstractCoordinates {

        protected ArrayAccessCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ArrayDimensionCoordinates extends AbstractCoordinates {

        protected ArrayDimensionCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ArrayTypeCoordinates extends AbstractCoordinates {

        protected ArrayTypeCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AssertCoordinates extends AbstractCoordinates {

        protected AssertCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AssignCoordinates extends AbstractCoordinates {

        protected AssignCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class AssignOpCoordinates extends AbstractCoordinates {

        protected AssignOpCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class BinaryCoordinates extends AbstractCoordinates {

        protected BinaryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class BlockCoordinates extends AbstractCoordinates {

        protected BlockCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class BreakCoordinates extends AbstractCoordinates {

        protected BreakCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class CaseCoordinates extends AbstractCoordinates {

        protected CaseCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ClassDeclCoordinates extends AbstractCoordinates {

        protected ClassDeclCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { return create(Space.Location.CLASS_DECL_PREFIX); }
        public JavaTreeCoordinates extending() {return create(Space.Location.EXTENDS); }

        //TODO MOAR!
    }
    class CompilationUnitCoordinates extends AbstractCoordinates {

        protected CompilationUnitCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ContinueCoordinates extends AbstractCoordinates {

        protected ContinueCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ControlParenthesesCoordinates extends AbstractCoordinates {

        protected ControlParenthesesCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class DoWhileLoopCoordinates extends AbstractCoordinates {

        protected DoWhileLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class EmptyCoordinates extends AbstractCoordinates {

        protected EmptyCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class EnumValueCoordinates extends AbstractCoordinates {

        protected EnumValueCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class EnumValueSetCoordinates extends AbstractCoordinates {

        protected EnumValueSetCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class FieldAccessCoordinates extends AbstractCoordinates {

        protected FieldAccessCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ForEachLoopCoordinates extends AbstractCoordinates {

        protected ForEachLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ForLoopCoordinates extends AbstractCoordinates {

        protected ForLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class IdentCoordinates extends AbstractCoordinates {

        protected IdentCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class IfCoordinates extends AbstractCoordinates {

        protected IfCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ImportCoordinates extends AbstractCoordinates {

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

    class InstanceOfCoordinates extends AbstractCoordinates {

        protected InstanceOfCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class LabelCoordinates extends AbstractCoordinates {

        protected LabelCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class LambdaCoordinates extends AbstractCoordinates {

        protected LambdaCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class LiteralCoordinates extends AbstractCoordinates {

        protected LiteralCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class MemberReferenceCoordinates extends AbstractCoordinates {

        protected MemberReferenceCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class MethodDeclCoordinates extends AbstractCoordinates {

        protected MethodDeclCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { return create(Space.Location.METHOD_DECL_PREFIX); }
        public JavaTreeCoordinates arguments() {return create(Space.Location.METHOD_DECL_ARGUMENTS); }

        //TODO MOAR!
    }
    class MethodInvocationCoordinates extends AbstractCoordinates {

        protected MethodInvocationCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ModifierCoordinates extends AbstractCoordinates {

        protected ModifierCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class MultiCatchCoordinates extends AbstractCoordinates {

        protected MultiCatchCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class NewArrayCoordinates extends AbstractCoordinates {

        protected NewArrayCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class NewClassCoordinates extends AbstractCoordinates {

        protected NewClassCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class PackageCoordinates extends AbstractCoordinates {

        protected PackageCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ParameterizedTypeCoordinates extends AbstractCoordinates {

        protected ParameterizedTypeCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ParenthesesCoordinates extends AbstractCoordinates {

        protected ParenthesesCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class PrimitiveCoordinates extends AbstractCoordinates {

        protected PrimitiveCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ReturnCoordinates extends AbstractCoordinates {

        protected ReturnCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class SwitchCoordinates extends AbstractCoordinates {

        protected SwitchCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class SynchronizedCoordinates extends AbstractCoordinates {

        protected SynchronizedCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TernaryCoordinates extends AbstractCoordinates {

        protected TernaryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class ThrowCoordinates extends AbstractCoordinates {

        protected ThrowCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TryCoordinates extends AbstractCoordinates {

        protected TryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TypeCastCoordinates extends AbstractCoordinates {

        protected TypeCastCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class TypeParameterCoordinates extends AbstractCoordinates {

        protected TypeParameterCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class UnaryCoordinates extends AbstractCoordinates {

        protected UnaryCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class VariableDeclsCoordinates extends AbstractCoordinates {

        protected VariableDeclsCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class WhileLoopCoordinates extends AbstractCoordinates {

        protected WhileLoopCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }
    class WildcardCoordinates extends AbstractCoordinates {

        protected WildcardCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { throw new UnsupportedOperationException("Not Implemented"); }

        //TODO MOAR!
    }

}
