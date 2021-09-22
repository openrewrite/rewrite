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
import org.openrewrite.java.cleanup.*
import org.openrewrite.java.format.*
import org.openrewrite.java.internal.TypeCacheTest
import org.openrewrite.java.search.*
import org.openrewrite.java.style.AutodetectTest
import org.openrewrite.java.tree.TypeTreeTest

//----------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to Java11VisitorDebugTests.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------

@ExtendWith(JavaParserResolver::class)
abstract class JavaVisitorCompatibilityKit {
    abstract fun javaParser(): JavaParser.Builder<*, *>

    @Nested
    inner class AddImportTck : AddImportTest

    @Nested
    inner class AddLicenseHeaderTck : AddLicenseHeaderTest

    @Nested
    inner class AnnotationTemplateGeneratorTck : AnnotationTemplateGeneratorTest

    @Nested
    inner class AtomicPrimitiveEqualsUsesGetTck : AtomicPrimitiveEqualsUsesGetTest

    @Nested
    inner class AutodetectTck : AutodetectTest

    @Nested
    inner class BigDecimalRoundingConstantsToEnumsTck : BigDecimalRoundingConstantsToEnumsTest

    @Nested
    inner class BlankLinesTck : BlankLinesTest

    @Nested
    inner class BlockStatementTemplateGeneratorTck : BlockStatementTemplateGeneratorTest

    @Nested
    inner class BooleanChecksNotInvertedTck : BooleanChecksNotInvertedTest

    @Nested
    inner class CatchClauseOnlyRethrowsTcx : CatchClauseOnlyRethrowsTest

    @Nested
    inner class ControlFlowIndentationTcx : ControlFlowIndentationTest

    @Nested
    inner class ChangeFieldNameTck : ChangeFieldNameTest

    @Nested
    inner class ChangeFieldTypeTck : ChangeFieldTypeTest

    @Nested
    inner class ChangeLiteralTck : ChangeLiteralTest

    @Nested
    inner class ChangeMethodAccessLevelTck : ChangeMethodAccessLevelTest

    @Nested
    inner class ChangeMethodNameTck : ChangeMethodNameTest

    @Nested
    inner class ChangeMethodTargetToStaticTck : ChangeMethodTargetToStaticTest

    @Nested
    inner class ChangeMethodTargetToVariableTck : ChangeMethodTargetToVariableTest

    @Nested
    inner class ChangePackageTck : ChangePackageTest

    @Nested
    inner class ChangeTypeTck : ChangeTypeTest

    @Nested
    inner class CovariantEqualsTck : CovariantEqualsTest

    @Nested
    inner class DefaultComesLastTck : DefaultComesLastTest

    @Nested
    inner class DeleteMethodArgumentTck : DeleteMethodArgumentTest

    @Nested
    inner class DeleteStatementTck : DeleteStatementTest

    @Nested
    inner class EmptyBlockTck : EmptyBlockTest

    @Nested
    inner class EmptyNewlineAtEndOfFileTck : EmptyNewlineAtEndOfFileTest

    @Nested
    inner class EqualsAvoidsNullTck : EqualsAvoidsNullTest

    @Nested
    inner class ExplicitInitializationTck : ExplicitInitializationTest

    @Nested
    inner class ExplicitLambdaArgumentTypesTck : ExplicitLambdaArgumentTypesTest

    @Nested
    inner class ExternalizableHasNoArgConstructorTck : ExternalizableHasNoArgConstructorTest

    @Nested
    inner class FallThroughTck : FallThroughTest

    @Nested
    inner class FinalClassTck : FinalClassTest

    @Nested
    inner class FinalizeLocalVariablesTck : FinalizeLocalVariablesTest

    @Nested
    inner class ForLoopControlVariablePostfixOperatorsTck : ForLoopControlVariablePostfixOperatorsTest

    @Nested
    inner class ForLoopIncrementInUpdateTck : ForLoopIncrementInUpdateTest

    @Nested
    inner class LineCounterTck : LineCounterTest

    @Nested
    inner class StaticMethodNotFinalTck : StaticMethodNotFinalTest

    @Nested
    inner class TypeCacheTck : TypeCacheTest

    @Nested
    inner class FindAnnotationsTck : FindAnnotationsTest

    @Nested
    inner class FindDeprecatedClassesTck : FindDeprecatedClassesTest

    @Nested
    inner class FindDeprecatedFieldsTck : FindDeprecatedFieldsTest

    @Nested
    inner class FindFieldsTck : FindFieldsTest

    @Nested
    inner class FindFieldsOfTypeTck : FindFieldsOfTypeTest

    @Nested
    inner class FindInheritedFieldsTck : FindInheritedFieldsTest


    @Nested
    inner class FindDeprecatedMethodsTck : FindDeprecatedMethodsTest

    @Nested
    inner class FindMethodsTck : FindMethodsTest

    @Nested
    inner class FindTextTck : FindTextTest

    @Nested
    inner class FindTypesTck : FindTypesTest

    @Nested
    inner class HiddenFieldTck : HiddenFieldTest

    @Nested
    inner class HideUtilityClassConstructorTck : HideUtilityClassConstructorTest

    @Nested
    inner class IndexOfChecksShouldUseAStartPositionTck : IndexOfChecksShouldUseAStartPositionTest

    @Nested
    inner class IndexOfReplaceableByContainsTck : IndexOfReplaceableByContainsTest

    @Nested
    inner class IndexOfShouldNotCompareGreaterThanZeroTckTest : IndexOfShouldNotCompareGreaterThanZeroTest

    @Nested
    inner class IsEmptyCallOnCollectionsTckTest : IsEmptyCallOnCollectionsTest

    @Nested
    inner class ImplementInterfaceTck : ImplementInterfaceTest

    @Nested
    inner class InvertConditionTck : InvertConditionTest

    @Nested
    inner class JavaTemplateTck : JavaTemplateTest

    @Nested
    inner class JavaTemplateSubstitutionsTck : JavaTemplateSubstitutionsTest

    @Nested
    inner class JavaTypeTck : JavaTypeTest

    @Nested
    inner class JavaVisitorTestTck : JavaVisitorTest

    @Nested
    inner class MaybeUsesImportTck : MaybeUsesImportTest

    @Nested
    inner class MethodNameCasingTck : MethodNameCasingTest

    @Nested
    inner class MinimumViableSpacingTck : MinimumViableSpacingTest

    @Nested
    inner class MinimumSwitchCasesTck : MinimumSwitchCasesTest

    @Nested
    inner class MissingOverrideAnnotationTck : MissingOverrideAnnotationTest

    @Nested
    inner class ModifierOrderTck : ModifierOrderTest

    @Nested
    inner class MultipleVariableDeclarationsTck : MultipleVariableDeclarationsTest

    @Nested
    inner class NeedBracesTck : NeedBracesTest

    @Nested
    inner class NoEqualityInForConditionTck : NoEqualityInForConditionTest

    @Nested
    inner class NoFinalizerTck : NoFinalizerTest

    @Nested
    inner class NoRedundantJumpStatementsTck : NoRedundantJumpStatementsTest

    @Nested
    inner class NoToStringOnStringTck : NoToStringOnStringTypeTest

    @Nested
    inner class NoValueOfOnStringTypeTck : NoValueOfOnStringTypeTest

    @Nested
    inner class NoStaticImportTck : NoStaticImportTest

    @Nested
    inner class NormalizeFormatTck : NormalizeFormatTest

    @Nested
    inner class NormalizeTabsOrSpacesTck : NormalizeTabsOrSpacesTest

    @Nested
    inner class NoWhitespaceAfterTck : NoWhitespaceAfterTest

    @Nested
    inner class NoWhitespaceBeforeTck : NoWhitespaceBeforeTest

    @Nested
    inner class NoEmptyCollectionWithRawTypeTck : NoEmptyCollectionWithRawTypeTest

    @Nested
    inner class OperatorWrapTck : OperatorWrapTest

    @Nested
    inner class OrderImportsTck : OrderImportsTest

    @Nested
    inner class PadEmptyForLoopComponentsTck : PadEmptyForLoopComponentsTest

    @Nested
    inner class PrimitiveWrapperClassConstructorToValueOfTck : PrimitiveWrapperClassConstructorToValueOfTest

    @Nested
    inner class RedundantFileCreationTck : RedundantFileCreationTest

    @Nested
    inner class RemoveAnnotationTck : RemoveAnnotationTest

    @Nested
    inner class RemoveExtraSemicolonsTck : RemoveExtraSemicolonsTest

    @Nested
    inner class RemoveUnusedLocalVariablesTck : RemoveUnusedLocalVariablesTest

    @Nested
    inner class RemoveUnusedPrivateMethodsTck : RemoveUnusedPrivateMethodsTest

    @Nested
    inner class RemoveImportTck : RemoveImportTest

    @Nested
    inner class RemoveTrailingWhitespaceTck : RemoveTrailingWhitespaceTest

    @Nested
    inner class RemoveUnusedImportsTck : RemoveUnusedImportsTest

    @Nested
    inner class RenameVariableTck : RenameVariableTest

    @Nested
    inner class ReorderMethodArgumentsTck : ReorderMethodArgumentsTest

    @Nested
    inner class ResultOfMethodCallIgnoredTck : ResultOfMethodCallIgnoredTest

    @Nested
    inner class SemanticallyEqualTck : SemanticallyEqualTest

    @Nested
    inner class SimplifyBooleanExpressionTck : SimplifyBooleanExpressionTest

    @Nested
    inner class SimplifyBooleanReturnTck : SimplifyBooleanReturnTest

    @Nested
    inner class SimplifyMethodChainTck : SimplifyMethodChainTest

    @Nested
    inner class StringBuilderBUfferNotInstantiatedWithCharTck : NewStringBuilderBufferWithCharArgumentTest

    @Nested
    inner class RenameLocalVariablesToCamelCaseTck : RenameLocalVariablesToCamelCaseTest

    @Nested
    inner class SpacesTck : SpacesTest

    @Nested
    inner class TabsAndIndentsTck : TabsAndIndentsTest

    @Nested
    inner class TypeTreeTck : TypeTreeTest

    @Nested
    inner class StringLiteralEqualityTestTck : StringLiteralEqualityTest

    @Nested
    inner class TypecastParenPadTestTck : TypecastParenPadTest

    @Nested
    inner class UnnecessaryPrimitiveAnnotationsTck : UnnecessaryPrimitiveAnnotationsTest

    @Nested
    inner class UnnecessaryCloseInTryWithResourcesTck : UnnecessaryCloseInTryWithResourcesTest

    @Nested
    inner class UnnecessaryExplicitTypeArgumentsTck : UnnecessaryExplicitTypeArgumentsTest

    @Nested
    inner class UnnecessaryParenthesesTck : UnnecessaryParenthesesTest

    @Nested
    inner class UnnecessaryThrowsTck : UnnecessaryThrowsTest

    @Nested
    inner class UnwrapParenthesesTck : UnwrapParenthesesTest

    @Nested
    inner class UseDiamondOperatorTck : UseDiamondOperatorTest

    @Nested
    inner class UsesJavaVersionTck : UsesJavaVersionTest

    @Nested
    inner class UseJavaStyleArrayDeclarationsTck : UseJavaStyleArrayDeclarationsTest

    @Nested
    inner class UsesMethodTck : UsesMethodTest

    @Nested
    inner class UseStaticImportTck : UseStaticImportTest

    @Nested
    inner class UsesTypeTck : UsesTypeTest

    @Nested
    inner class WhileInsteadOfForTck : WhileInsteadOfForTest

    @Nested
    inner class WrappingAndBracesTck : WrappingAndBracesTest

    @Nested
    inner class WriteOctalValuesAsDecimalTck : WriteOctalValuesAsDecimalTest
}
