package org.openrewrite.java

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JavaParserResolver::class)
abstract class JavaRefactorCompatibilityKit {
    abstract fun javaParser(): JavaParser

    @Nested
    inner class AddAnnotationTck : AddAnnotationTest

    @Nested
    inner class AddFieldTck : AddFieldTest

    @Nested
    inner class AddImportTck : AddImportTest

    @Nested
    inner class ChangeFieldNameTck : ChangeFieldNameTest

    @Nested
    inner class ChangeFieldTypeTck : ChangeFieldTypeTest

    @Nested
    inner class ChangeLiteralTck : ChangeLiteralTest

    @Nested
    inner class ChangeMethodNameTck : ChangeMethodNameTest

    @Nested
    inner class ChangeMethodTargetToStaticTck : ChangeMethodTargetToStaticTest

    @Nested
    inner class ChangeTypeTck : ChangeTypeTest

    @Nested
    inner class DeleteMethodArgumentTck : DeleteMethodArgumentTest

    @Nested
    inner class DeleteStatementTck : DeleteStatementTest

    @Nested
    inner class GenerateConstructorUsingFieldsTck : GenerateConstructorUsingFieldsTest

    @Nested
    inner class ImplementInterfaceTck : ImplementInterfaceTest

    @Nested
    inner class InsertMethodArgumentTck : InsertMethodArgumentTest

    @Nested
    inner class MethodMatcherTck : MethodMatcherTest

    @Nested
    inner class RemoveImportTck : RemoveImportTest

    @Nested
    inner class RenameVariableTck : RenameVariableTest

    @Nested
    inner class ReorderMethodArgumentsTck : ReorderMethodArgumentsTest

    @Nested
    inner class UnwrapParenthesesTck : UnwrapParenthesesTest
}
