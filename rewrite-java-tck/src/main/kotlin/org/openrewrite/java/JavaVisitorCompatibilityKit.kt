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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.cleanup.*
import org.openrewrite.java.controlflow.ControlFlowDotFileViewerTest
import org.openrewrite.java.controlflow.ControlFlowTest
import org.openrewrite.java.controlflow.GuardTest
import org.openrewrite.java.dataflow.*
import org.openrewrite.java.format.*
import org.openrewrite.java.recipes.ExecutionContextParameterNameTest
import org.openrewrite.java.recipes.PublicGetVisitorTest
import org.openrewrite.java.recipes.SetDefaultEstimatedEffortPerOccurrenceTest
import org.openrewrite.java.search.*
import org.openrewrite.java.style.AutodetectTest
import org.openrewrite.java.tree.TypeTreeTest

//----------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to Java11VisitorDebugTests.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------

@Suppress("unused")
@ExtendWith(JavaParserResolver::class)
abstract class JavaVisitorCompatibilityKit {
    abstract fun javaParser(): JavaParser.Builder<*, *>

    @Nested
    inner class AddImportTck : AddImportTest

    @Nested
    inner class AddLicenseHeaderTck : AddLicenseHeaderTest

    @Nested
    inner class AddOrUpdateAnnotationAttributeTck : AddOrUpdateAnnotationAttributeTest

    @Nested
    inner class AddSerialVersionUidToSerializableTck : AddSerialVersionUidToSerializableTest

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
    inner class CombineSemanticallyEqualCatchBlocksTcx : CombineSemanticallyEqualCatchBlocksTest

    @Nested
    inner class CompareEnumWithEqualityOperatorTcx : CompareEnumWithEqualityOperatorTest

    @Nested
    inner class CaseInsensitiveComparisonsDoNotChangeCaseTck : CaseInsensitiveComparisonsDoNotChangeCaseTest

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
    inner class ChangeStaticFieldToMethodTestTck : ChangeStaticFieldToMethodTest

    @Nested
    inner class ChangeTypeTck : ChangeTypeTest

    @Nested
    inner class CountLinesTck : CountLinesTest

    @Nested
    inner class ControlFlowTck : ControlFlowTest

    @Nested
    inner class ControlFlowDotFileViewerTck : ControlFlowDotFileViewerTest

    @Nested
    inner class CovariantEqualsTck : CovariantEqualsTest

    @Disabled("flaky")
    @Nested
    inner class DataflowInsanityTck : DataflowInsanityTest

    @Nested
    inner class DataflowRealWorldExamplesTck : DataflowRealWorldExamplesTest

    @Nested
    inner class DefaultComesLastTck : DefaultComesLastTest

    @Nested
    inner class DeleteMethodArgumentTck : DeleteMethodArgumentTest

    @Nested
    inner class DeleteStatementTck : DeleteStatementTest

    @Nested
    inner class DoesNotUseRewriteSkipTck : DoesNotUseRewriteSkipTest

    @Nested
    inner class EmptyBlockTck : EmptyBlockTest

    @Nested
    inner class EmptyNewlineAtEndOfFileTck : EmptyNewlineAtEndOfFileTest

    @Nested
    inner class EqualsAvoidsNullTck : EqualsAvoidsNullTest

    @Nested
    inner class ExplicitCharsetOnStringGetBytesTck : ExplicitCharsetOnStringGetBytesTest

    @Nested
    inner class ExecutionContextParameterNameTck : ExecutionContextParameterNameTest

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
    inner class FindAnnotationsTck : FindAnnotationsTest

    @Nested
    inner class FindMissingTypesTck : FindMissingTypesTest

    @Nested
    inner class FixSerializableFieldsTck : FixSerializableFieldsTest

    @Nested
    inner class ForLoopControlVariablePostfixOperatorsTck : ForLoopControlVariablePostfixOperatorsTest

    @Nested
    inner class ForLoopIncrementInUpdateTck : ForLoopIncrementInUpdateTest

    @Nested
    inner class FindDeprecatedClassesTck : FindDeprecatedClassesTest

    @Nested
    inner class FindDeprecatedFieldsTck : FindDeprecatedFieldsTest

    @Nested
    inner class FindEmptyMethodsTck : FindEmptyMethodsTest

    @Nested
    inner class FindFieldsTck : FindFieldsTest

    @Nested
    inner class FindFieldsOfTypeTck : FindFieldsOfTypeTest

    @Nested
    inner class FindInheritedFieldsTck : FindInheritedFieldsTest

    @Nested
    inner class FindDeprecatedMethodsTck : FindDeprecatedMethodsTest

    @Nested
    inner class FindFlowBetweenMethodsTck : FindFlowBetweenMethodsTest

    @Nested
    inner class FindLiteralsTck : FindLiteralsTest

    @Nested
    inner class FindLocalFlowPathsNumericTck : FindLocalFlowPathsNumericTest

    @Nested
    inner class FindLocalFlowPathsStringTck : FindLocalFlowPathsStringTest

    @Nested
    inner class FindLocalFlowToExternalSinkTck : FindLocalTaintFlowToExternalSinkTest

    @Nested
    inner class FindLocalTaintFlowTck : FindLocalTaintFlowTest

    @Nested
    inner class FindMethodsTck : FindMethodsTest

    @Nested
    inner class FindRepeatableAnnotationsTck : FindRepeatableAnnotationsTest

    @Nested
    inner class DeimplementInterfaceTck : DeimplementInterfaceTest

    @Nested
    inner class ExtractInterfaceTck : ExtractInterfaceTest

    @Nested
    inner class FindTextTck : FindTextTest

    @Nested
    inner class FindImportsTck : FindImportsTest

    @Nested
    inner class FindTypesTck : FindTypesTest

    @Nested
    inner class GenerateGetterAndSetterVisitorTck : GenerateGetterAndSetterVisitorTest

    @Nested
    inner class GuardTck: GuardTest

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
    inner class InlineVariableTck : InlineVariableTest

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
    inner class LambdaBlockToExpressionTck : LambdaBlockToExpressionTest

    @Nested
    inner class LowercasePackageTck: LowercasePackageTest

    @Nested
    inner class LineCounterTck : LineCounterTest

    @Nested
    inner class PotentiallyDeadCodeTck : PotentiallyDeadCodeTest

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
    inner class NestedEnumsAreNotStaticTck : NestedEnumsAreNotStaticTest

    @Nested
    inner class StringBuilderBUfferNotInstantiatedWithCharTck : NewStringBuilderBufferWithCharArgumentTest

    @Nested
    inner class NoEqualityInForConditionTck : NoEqualityInForConditionTest

    @Nested
    inner class NoDoubleBraceInitializationTck : NoDoubleBraceInitializationTest

    @Nested
    inner class NoFinalizerTck : NoFinalizerTest

    @Nested
    inner class NoPrimitiveWrappersForToStringOrCompareToTck : NoPrimitiveWrappersForToStringOrCompareToTest

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
    inner class ObjectFinalizeCallsSuperTck : ObjectFinalizeCallsSuperTest

    @Nested
    inner class OperatorWrapTck : OperatorWrapTest

    @Nested
    inner class OrderImportsTck : OrderImportsTest

    @Nested
    inner class PadEmptyForLoopComponentsTck : PadEmptyForLoopComponentsTest

    @Nested
    inner class PrimitiveWrapperClassConstructorToValueOfTck : PrimitiveWrapperClassConstructorToValueOfTest

    @Nested
    inner class PublicGetVisitorTck : PublicGetVisitorTest

    @Nested
    inner class RecipeExceptionDemonstrationTck : RecipeExceptionDemonstrationTest

    @Nested
    inner class RecipeMarkupDemonstrationTck : RecipeMarkupDemonstrationTest

    @Nested
    inner class RedundantFileCreationTck : RedundantFileCreationTest

    @Nested
    inner class RemoveAnnotationTck : RemoveAnnotationTest

    @Nested
    inner class RemoveCallsToSystemGcTck : RemoveCallsToSystemGcTest

    @Nested
    inner class RemoveEmptyJavaDocParametersTck : RemoveEmptyJavaDocParametersTest

    @Nested
    inner class RemoveExtraSemicolonsTck : RemoveExtraSemicolonsTest

    @Nested
    inner class RemoveJavaDocAuthorTagTck : RemoveJavaDocAuthorTagTest

    @Nested
    inner class RemoveMethodCallVisitorTck: RemoveMethodCallVisitorTest

    @Nested
    inner class RemoveObjectsIsNullTck : RemoveObjectsIsNullTest

    @Nested
    inner class RemoveRemoveRedundantTypeCastTck : RemoveRedundantTypeCastTest

    @Nested
    inner class RemoveUnneededAssertionTck: RemoveUnneededAssertionTest

    @Nested
    inner class RemoveUnneededBlockTck : RemoveUnneededBlockTest

    @Nested
    inner class RemoveImplementsTck : RemoveImplementsTest

    @Nested
    inner class RemoveUnusedLocalVariablesTck : RemoveUnusedLocalVariablesTest

    @Nested
    inner class RemoveUnusedPrivateFieldsTck : RemoveUnusedPrivateFieldsTest

    @Nested
    inner class RemoveUnusedPrivateMethodsTck : RemoveUnusedPrivateMethodsTest

    @Nested
    inner class RenameJavaDocParamNameVisitorTck : RenameJavaDocParamNameVisitorTest

    @Nested
    inner class RenameExceptionInEmptyCatchTck : RenameExceptionInEmptyCatchTest

    @Nested
    inner class RemoveImportTck : RemoveImportTest

    @Nested
    inner class RemoveTrailingWhitespaceTck : RemoveTrailingWhitespaceTest

    @Nested
    inner class RemoveUnusedImportsTck : RemoveUnusedImportsTest

    @Nested
    inner class RenameVariableTck : RenameVariableTest

    @Nested
    inner class RenameLocalVariablesToCamelCaseTck : RenameLocalVariablesToCamelCaseTest

    @Nested
    inner class RenamePrivateFieldsToCamelCaseTck : RenamePrivateFieldsToCamelCaseTest

    @Nested
    inner class RenameMethodsNamedHashcodeEqualOrTostringTck : RenameMethodsNamedHashcodeEqualOrTostringTest

    @Nested
    inner class ReferentialEqualityToObjectEqualsTck : ReferentialEqualityToObjectEqualsTest

    @Nested
    inner class ReplaceDuplicateStringLiteralsTestTck : ReplaceDuplicateStringLiteralsTest

    @Nested
    inner class ReplaceReplaceLambdaWithMethodReferenceTck : ReplaceLambdaWithMethodReferenceTest

    @Nested
    inner class ReorderMethodArgumentsTck : ReorderMethodArgumentsTest

    @Nested
    inner class ReplaceConstantTck : ReplaceConstantTest

    @Nested
    inner class ReplaceThreadRunWithThreadStartTck : ReplaceThreadRunWithThreadStartTest

    @Nested
    inner class ResultOfMethodCallIgnoredTck : ResultOfMethodCallIgnoredTest

    @Nested
    inner class SemanticallyEqualTck : SemanticallyEqualTest

    @Nested
    inner class SetDefaultEstimatedEffortPerOccurrenceTck : SetDefaultEstimatedEffortPerOccurrenceTest

    @Nested
    inner class SimplifyBooleanExpressionTck : SimplifyBooleanExpressionTest

    @Nested
    inner class SimplifyBooleanReturnTck : SimplifyBooleanReturnTest

    @Nested
    inner class SimplifyCompoundStatementTck : SimplifyCompoundStatementTest

    @Nested
    inner class SimplifyConsecutiveAssignmentsTck : SimplifyConsecutiveAssignmentsTest

    @Nested
    inner class SimplifyConstantIfBranchExecutionTck : SimplifyConstantIfBranchExecutionTest

    @Nested
    inner class SimplifyMethodChainTck : SimplifyMethodChainTest

    @Nested
    inner class SingleLineCommentsTck : SingleLineCommentsTest

    @Nested
    inner class StaticMethodNotFinalTck : StaticMethodNotFinalTest

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
    inner class UnnecessaryCatchTck : UnnecessaryCatchTest

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
    inner class UnwrapRepeatableAnnotationsTck : UnwrapRepeatableAnnotationsTest

    @Nested
    inner class UseDiamondOperatorTck : UseDiamondOperatorTest

    @Nested
    inner class UseAsBuilderTck : UseAsBuilderTest

    @Nested
    inner class UseCollectionInterfacesTck : UseCollectionInterfacesTest

    @Nested
    inner class UsesJavaVersionTck : UsesJavaVersionTest

    @Nested
    inner class UriCreatedWithHttpSchemeTck : UriCreatedWithHttpSchemeTest

    @Nested
    inner class UseJavaStyleArrayDeclarationsTck : UseJavaStyleArrayDeclarationsTest

    @Nested
    inner class UseLambdaForFunctionalInterfaceTck : UseLambdaForFunctionalInterfaceTest

    @Nested
    inner class UseStandardCharsetTck : UseStandardCharsetTest

    @Nested
    inner class UsesAllMethodsTck : UsesAllMethodsTest

    @Nested
    inner class UsesMethodTck : UsesMethodTest

    @Nested
    inner class UseStringReplaceTestTck : UseStringReplaceTest

    @Nested
    inner class UseStaticImportTck : UseStaticImportTest

    @Nested
    inner class UpdateSourcePositionsTck : UpdateSourcePositionsTest

    @Nested
    inner class UsesTypeTck : UsesTypeTest

    @Nested
    inner class VariableNameUtilsTck : VariableNameUtilsTest

    @Nested
    inner class WhileInsteadOfForTck : WhileInsteadOfForTest

    @Nested
    inner class WrappingAndBracesTck : WrappingAndBracesTest

    @Nested
    inner class WriteOctalValuesAsDecimalTck : WriteOctalValuesAsDecimalTest
}
