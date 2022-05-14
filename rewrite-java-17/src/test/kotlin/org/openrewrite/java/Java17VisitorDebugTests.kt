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

import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.DebugOnly
import org.openrewrite.java.cleanup.*
import org.openrewrite.java.format.*
import org.openrewrite.java.recipes.ExecutionContextParameterNameTest
import org.openrewrite.java.recipes.SetDefaultEstimatedEffortPerOccurrenceTest
import org.openrewrite.java.search.*
import org.openrewrite.java.style.AutodetectTest
import org.openrewrite.java.tree.TypeTreeTest

//----------------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to JavaVisitorCompatibilityKit.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------------
@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AddImportTest : Java17Test, AddImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AddLicenseHeaderTest : Java17Test, AddLicenseHeaderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AddOrUpdateAnnotationAttribute : Java17Test, AddOrUpdateAnnotationAttributeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AddSerialVersionUidToSerializableTest : Java17Test, AddSerialVersionUidToSerializableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AnnotationTemplateGeneratorTest : Java17Test, AnnotationTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AtomicPrimitiveEqualsUsesGetTest : Java17Test, AtomicPrimitiveEqualsUsesGetTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AutodetectTest : Java17Test, AutodetectTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BigDecimalRoundingConstantsToEnumsTestTest : Java17Test, BigDecimalRoundingConstantsToEnumsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BlankLinesTest : Java17Test, BlankLinesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BlockStatementTemplateGeneratorTest : Java17Test, BlockStatementTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BooleanChecksNotInvertedTest : Java17Test, BooleanChecksNotInvertedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CaseInsensitiveComparisonsDoNotChangeCaseTest : Java17Test, CaseInsensitiveComparisonsDoNotChangeCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CatchClauseOnlyRethrowsTest : Java17Test, CatchClauseOnlyRethrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CompareEnumWithEqualityOperatorTest : Java17Test, CompareEnumWithEqualityOperatorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ControlFlowIndentationTest : Java17Test, ControlFlowIndentationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeFieldNameTest : Java17Test, ChangeFieldNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeFieldTypeTest : Java17Test, ChangeFieldTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeLiteralTest : Java17Test, ChangeLiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodAccessLevelTest : Java17Test, ChangeMethodAccessLevelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodNameTest : Java17Test, ChangeMethodNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodTargetToStaticTest : Java17Test, ChangeMethodTargetToStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodTargetToVariableTest : Java17Test, ChangeMethodTargetToVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangePackageTest : Java17Test, ChangePackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeStaticFieldToMethodTest : Java17Test, ChangeStaticFieldToMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeTypeTest : Java17Test, ChangeTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CovariantEqualsTest : Java17Test, CovariantEqualsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DefaultComesLastTest : Java17Test, DefaultComesLastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DeleteMethodArgumentTest : Java17Test, DeleteMethodArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DeleteStatementTest : Java17Test, DeleteStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DoesNotUseRewriteSkipTest : Java17Test, DoesNotUseRewriteSkipTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EmptyBlockTest : Java17Test, EmptyBlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EmptyNewlineAtEndOfFileTest : Java17Test, EmptyNewlineAtEndOfFileTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EqualsAvoidsNullTest : Java17Test, EqualsAvoidsNullTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExplicitCharsetOnStringGetBytesTest : Java17Test, ExplicitCharsetOnStringGetBytesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExecutionContextParameterNameTest : Java17Test, ExecutionContextParameterNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExplicitInitializationTest : Java17Test, ExplicitInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExplicitLambdaArgumentTypesTest : Java17Test, ExplicitLambdaArgumentTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExternalizableHasNoArgsConstructorTest : Java17Test, ExternalizableHasNoArgConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FallThroughTest : Java17Test, FallThroughTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FinalClassTest : Java17Test, FinalClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FinalizeLocalVariablesTest : Java17Test, FinalizeLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FixSerializableFieldsTest : Java17Test, FixSerializableFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ForLoopControlVariablePostfixOperatorsTest : Java17Test, ForLoopControlVariablePostfixOperatorsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ForLoopIncrementInUpdateTest : Java17Test, ForLoopIncrementInUpdateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindAnnotationsTest : Java17Test, FindAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindDeprecatedClassesTest : Java17Test, FindDeprecatedClassesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindDeprecatedFieldsTest : Java17Test, FindDeprecatedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindDeprecatedMethodsTest : Java17Test, FindDeprecatedMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindEmptyClassesTest : Java17Test, FindEmptyClassesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindEmptyMethodsTest : Java17Test, FindEmptyMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindFieldsTest : Java17Test, FindFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindFieldsOfTypeTest : Java17Test, FindFieldsOfTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindInheritedFieldsTest : Java17Test, FindInheritedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindMethodsTest : Java17Test, FindMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindLiteralsTest : Java17Test, FindLiteralsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DeimplementInterfaceTest : Java17Test, DeimplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExtractInterfaceTest : Java17Test, ExtractInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindTextTest : Java17Test, FindTextTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindTypesTest : Java17Test, FindTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17GenerateGetterTestAndSetter : Java17Test, GenerateGetterAndSetterVisitorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17HiddenFieldTest : Java17Test, HiddenFieldTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17HideUtilityClassConstructorTest : Java17Test, HideUtilityClassConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17IndexOfChecksShouldUseAStartPositionTest : Java17Test, IndexOfChecksShouldUseAStartPositionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17IndexOfReplaceableByContainsTest : Java17Test, IndexOfReplaceableByContainsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17IndexOfShouldNotCompareGreaterThanZeroTest : Java17Test, IndexOfShouldNotCompareGreaterThanZeroTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17IsEmptyCallOnCollectionsTest : Java17Test, IsEmptyCallOnCollectionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ImplementInterfaceTest : Java17Test, ImplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17InvertConditionTest : Java17Test, InvertConditionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaTemplateTest : Java17Test, JavaTemplateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaTemplateSubstitutionsTest : Java17Test, JavaTemplateSubstitutionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaVisitorTest : Java17Test, JavaVisitorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17LambdaBlockToExpressionTest : Java17Test, LambdaBlockToExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17LineCounterTest : Java17Test, LineCounterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17PotentiallyDeadCodeTest : Java17Test, PotentiallyDeadCodeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MaybeUsesImportTest : Java17Test, MaybeUsesImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MethodNameCasingTest : Java17Test, MethodNameCasingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MinimumViableSpacingTest : Java17Test, MinimumViableSpacingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MinimumSwitchCasesTest : Java17Test, MinimumSwitchCasesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MissingOverrideAnnotationTest : Java17Test, MissingOverrideAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ModifierOrderTest : Java17Test, ModifierOrderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MultipleVariableDeclarationsTest : Java17Test, MultipleVariableDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NeedBracesTest : Java17Test, NeedBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoDoubleBraceInitializationTest : Java17Test, NoDoubleBraceInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoEqualityInForConditionTest : Java17Test, NoEqualityInForConditionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoFinalizerTest : Java17Test, NoFinalizerTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoRedundantJumpStatementsTest : Java17Test, NoRedundantJumpStatementsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoToStringOnStringTypeTest : Java17Test, NoToStringOnStringTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoValueOfOnStringTypeTest : Java17Test, NoValueOfOnStringTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoStaticImport : Java17Test, NoStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NormalizeFormatTest : Java17Test, NormalizeFormatTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NormalizeLineBreaksTest : Java17Test, NormalizeLineBreaksTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NormalizeTabsOrSpacesTest : Java17Test, NormalizeTabsOrSpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoWhitespaceAfterTest : Java17Test, NoWhitespaceAfterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoWhitespaceBeforeTest : Java17Test, NoWhitespaceBeforeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoEmptyCollectionWithRawTypeTest : Java17Test, NoEmptyCollectionWithRawTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NoPrimitiveWrappersForToStringOrCompareToTest : Java17Test, NoPrimitiveWrappersForToStringOrCompareToTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ObjectFinalizeCallsSuperTest : Java17Test, ObjectFinalizeCallsSuperTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17OperatorWrapTest : Java17Test, OperatorWrapTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17OrderImportsTest : Java17Test, OrderImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17PadEmptyForLoopComponentsTest : Java17Test, PadEmptyForLoopComponentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17PrimitiveWrapperClassConstructorToValueOfTest : Java17Test, PrimitiveWrapperClassConstructorToValueOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RedundantFileCreationTest : Java17Test, RedundantFileCreationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveAnnotationTest : Java17Test, RemoveAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveImplementsTest : Java17Test, RemoveImplementsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveExtraSemicolonsTest : Java17Test, RemoveExtraSemicolonsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveJavaDocAuthorTagTest : Java17Test, RemoveJavaDocAuthorTagTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveRedundantTypeCastTest : Java17Test, RemoveRedundantTypeCastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveUnneededBlockTest : Java17Test, RemoveUnneededBlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveUnusedLocalVariablesTest : Java17Test, RemoveUnusedLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveUnusedPrivateMethodsTest : Java17Test, RemoveUnusedPrivateMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenameExceptionInEmptyCatchTest : Java17Test, RenameExceptionInEmptyCatchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveImportTest : Java17Test, RemoveImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveTrailingWhitespaceTest : Java17Test, RemoveTrailingWhitespaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveUnusedImportsTest : Java17Test, RemoveUnusedImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenameLocalVariablesToCamelCaseTest : Java17Test, RenameLocalVariablesToCamelCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenamePrivateFieldsToCamelCaseTest : Java17Test, RenamePrivateFieldsToCamelCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenameMethodsNamedHashcodeEqualOrTostringTest : Java17Test, RenameMethodsNamedHashcodeEqualOrTostringTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ReplaceDuplicateStringLiteralsTest : Java17Test, ReplaceDuplicateStringLiteralsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ReplaceLambdaWithMethodReferenceTest : Java17Test, ReplaceLambdaWithMethodReferenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenameVariableTest : Java17Test, RenameVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ReorderMethodArgumentsTest : Java17Test, ReorderMethodArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ReplaceConstantTest : Java17Test, ReplaceConstantTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ResultOfMethodCallIgnoredTest : Java17Test, ResultOfMethodCallIgnoredTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SemanticallyEqualTest : Java17Test, SemanticallyEqualTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyBooleanExpressionTest : Java17Test, SimplifyBooleanExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyBooleanReturnTest : Java17Test, SimplifyBooleanReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17StaticMethodNotFinalTest : Java17Test, StaticMethodNotFinalTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SingleLineCommentsTest : Java17Test, SingleLineCommentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SpacesTest : Java17Test, SpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TabsAndIndentsTest : Java17Test, TabsAndIndentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypeTest : Java17Test, JavaTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypeTreeTest : Java17Test, TypeTreeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17StringLiteralEqualityTest : Java17Test, StringLiteralEqualityTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NestedEnumsAreNotStaticTest : Java17Test, NestedEnumsAreNotStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NewStringBuilderBufferWithCharArgumentTest : Java17Test, NewStringBuilderBufferWithCharArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SetDefaultEstimatedEffortPerOccurrenceTest : Java17Test, SetDefaultEstimatedEffortPerOccurrenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypecastParenPadTest : Java17Test, TypecastParenPadTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryCloseInTryWithResourcesTest : Java17Test, UnnecessaryCloseInTryWithResourcesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryExplicitTypeArgumentsTest : Java17Test, UnnecessaryExplicitTypeArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryParenthesesTest : Java17Test, UnnecessaryParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryPrimitiveAnnotationsTest : Java17Test, UnnecessaryPrimitiveAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryThrowsTest : Java17Test, UnnecessaryThrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnwrapParenthesesTest : Java17Test, UnwrapParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseDiamondOperatorTest : Java17Test, UseDiamondOperatorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseJavaStyleArrayDeclarations : Java17Test, UseJavaStyleArrayDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseLambdaForFunctionalInterface : Java17Test, UseLambdaForFunctionalInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseStandardCharsetTest : Java17Test, UseStandardCharsetTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseStaticImportTest : Java17Test, UseStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UpdateSourcePositionsTest : Java17Test, UpdateSourcePositionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseCollectionInterfacesTest : Java17Test, UseCollectionInterfacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UsesJavaVersionTest : Java17Test, UsesJavaVersionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UsesMethodTest : Java17Test, UsesMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseStringReplaceTest : Java17Test, UseStringReplaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UsesTypeTest : Java17Test, UsesTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyCompoundStatementTest : Java17Test, SimplifyCompoundStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17WhileInsteadOfForTest : Java17Test, WhileInsteadOfForTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17WrappingAndBracesTest : Java17Test, WrappingAndBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17WriteOctalValuesAsDecimalTest : Java17Test, WriteOctalValuesAsDecimalTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SameClassNameTest : Java17Test, SameClassNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyConsecutiveAssignmentsTest : Java17Test, SimplifyConsecutiveAssignmentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyConstantIfBranchExecutionTest : Java17Test, SimplifyConstantIfBranchExecutionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyMethodChainTest : Java17Test, SimplifyMethodChainTest
