package org.openrewrite.java

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.tree.*

@ExtendWith(JavaParserResolver::class)
abstract class JavaParserCompatibilityKit {
    abstract fun javaParser(): JavaParser?

    @Nested
    inner class AnnotationTck : AnnotationTest

    @Nested
    inner class ArrayAccessTck : ArrayAccessTest

    @Nested
    inner class ArrayTypeTck : ArrayTypeTest

    @Nested
    inner class AssertTck : AssertTest

    @Nested
    inner class AssignOpTck : AssignOpTest

    @Nested
    inner class AssignTck : AssignTest

    @Nested
    inner class BlockTck : BlockTest

    @Nested
    inner class BreakTck : BreakTest

    @Nested
    inner class BodyTck : BodyTest

    @Nested
    inner class ClassDeclTck : ClassDeclTest

    @Nested
    inner class CommentTck : CommentTest

    @Nested
    inner class CompilationUnitTck : CompilationUnitTest

    @Nested
    inner class ContinueTck : ContinueTest

    @Nested
    inner class CursorTck : CursorTest

    @Nested
    inner class CyclicTypeTck : CyclicTypeTest

    @Nested
    inner class DoWhileLoopTck : DoWhileLoopTest

    @Nested
    inner class EmptyTck : EmptyTest

    @Nested
    inner class EnumTck : EnumTest

    @Nested
    inner class FieldAccessTck : FieldAccessTest

    @Nested
    inner class ForEachLoopTck : ForEachLoopTest

    @Nested
    inner class IdentTck : IdentTest

    @Nested
    inner class IfTck : IfTest

    @Nested
    inner class ImportTck : ImportTest

    @Nested
    inner class InstanceofTck : InstanceOfTest

    @Nested
    inner class JavaTypeTck : JavaTypeTest

    @Nested
    inner class LabelTck : LabelTest

    @Nested
    inner class LambdaTck : LambdaTest

    @Nested
    inner class LiteralTck : LiteralTest

    @Nested
    inner class MemberReferenceTck : MemberReferenceTest

    @Nested
    inner class MethodDeclTck : MethodDeclTest

    @Nested
    inner class MethodInvocationTck : MethodInvocationTest

    @Nested
    inner class ModifierTck : ModifierTest

    @Nested
    inner class NewArrayTck : NewArrayTest

    @Nested
    inner class NewClassTck : NewClassTest

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
    inner class TreeBuilderTck : TreeBuilderTest

    @Nested
    inner class TreeSerializerTck : TreeSerializerTest

    @Nested
    inner class TryCatchTck : TryCatchTest

    @Nested
    inner class TypeCastTck : TypeCastTest

    @Nested
    inner class TypeParameterAndWildcardTck : TypeParameterAndWildcardTest

    @Nested
    inner class UnaryTck : UnaryTest

    @Nested
    inner class VariableDeclsTck : VariableDeclsTest

    @Nested
    inner class WhileLoopTck : WhileLoopTest
}
