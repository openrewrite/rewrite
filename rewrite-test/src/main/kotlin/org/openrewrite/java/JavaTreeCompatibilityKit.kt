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
package org.openrewrite.java

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.tree.*

//----------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to Java11TreeDebugTests.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------

@ExtendWith(JavaParserResolver::class)
abstract class JavaTreeCompatibilityKit {
    abstract fun javaParser(): JavaParser.Builder<*, *>

    @Nested
    inner class AnnotationTck : AnnotationTest

    @Nested
    inner class ArrayAccessTck : ArrayAccessTest

    @Nested
    inner class ArrayTypeTck : ArrayTypeTest

    @Nested
    inner class AssertTck : AssertTest

    @Nested
    inner class AssignmentOperationTck : AssignmentOperationTest

    @Nested
    inner class AssignmentTck : AssignmentTest

    @Nested
    inner class BinaryTck: BinaryTest

    @Nested
    inner class BlockTck : BlockTest

    @Nested
    inner class BreakTck : BreakTest

    @Nested
    inner class ClassDeclarationTck : ClassDeclarationTest

    @Nested
    inner class CommentTck : CommentTest

    @Nested
    inner class CompilationUnitTck : CompilationUnitTest

    @Nested
    inner class ContinueTck : ContinueTest

    @Nested
    inner class DoWhileLoopTck : DoWhileLoopTest

    @Nested
    inner class EmptyTck : EmptyTest

    @Nested
    inner class EnumTck : EnumTest

    @Nested
    inner class FieldAccessTck : FieldAccessTest

    @Nested
    inner class ForLoopTck : ForLoopTest

    @Nested
    inner class ForEachLoopTck : ForEachLoopTest

    @Nested
    inner class IdentifierTck : IdentifierTest

    @Nested
    inner class IfTck : IfTest

    @Nested
    inner class ImportTck : ImportTest

    @Nested
    inner class InstanceOfTck : InstanceOfTest

    @Nested
    inner class JavadocTck : JavadocTest

    @Nested
    inner class JavaParserTck : JavaParserTest

    @Nested
    inner class LabelTck : LabelTest

    @Nested
    inner class LambdaTck : LambdaTest

    @Nested
    inner class LiteralTck : LiteralTest

    @Nested
    inner class MemberReferenceTck : MemberReferenceTest

    @Nested
    inner class MethodDeclarationTck : MethodDeclarationTest

    @Nested
    inner class MethodInvocationTck : MethodInvocationTest

    @Nested
    inner class MethodMatcherTck : MethodMatcherTest

    @Nested
    inner class MethodParamPadTck : MethodParamPadTest

    @Nested
    inner class NewArrayTck : NewArrayTest

    @Nested
    inner class NewClassTck : NewClassTest

    @Nested
    inner class PackageTck : PackageTest

    @Nested
    inner class ParenthesesTck : ParenthesesTest

    @Nested
    inner class PrimitiveTck : PrimitiveTest

    @Nested
    inner class ReturnTck : ReturnTest

    @Nested
    inner class SwitchTck : SwitchTest

    @Nested
    inner class SynchronizedTck : SynchronizedTest

    @Nested
    inner class TernaryTck : TernaryTest

    @Nested
    inner class ThrowTck : ThrowTest

    @Nested
    inner class TryCatchTck : TryCatchTest

    @Nested
    inner class TypeCastTck : TypeCastTest

    @Nested
    inner class TypeParameterAndWildcardTck : TypeParameterAndWildcardTest

    @Nested
    inner class TypeUtilsTck: TypeUtilsTest

    @Nested
    inner class UnaryTck : UnaryTest

    @Nested
    inner class VariableDeclarationsTck : VariableDeclarationsTest

    @Nested
    inner class WhileLoopTck : WhileLoopTest
}
