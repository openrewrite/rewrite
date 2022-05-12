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
import org.openrewrite.java.dataflow.*
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
class Java11AddImportTest : Java11Test, AddImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AddLicenseHeaderTest : Java11Test, AddLicenseHeaderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AddOrUpdateAnnotationAttribute : Java11Test, AddOrUpdateAnnotationAttributeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AddSerialVersionUidToSerializableTest : Java11Test, AddSerialVersionUidToSerializableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AnnotationTemplateGeneratorTest : Java11Test, AnnotationTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AtomicPrimitiveEqualsUsesGetTest : Java11Test, AtomicPrimitiveEqualsUsesGetTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AutodetectTest : Java11Test, AutodetectTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11BigDecimalRoundingConstantsToEnumsTestTest : Java11Test, BigDecimalRoundingConstantsToEnumsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11BlankLinesTest : Java11Test, BlankLinesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11BlockStatementTemplateGeneratorTest : Java11Test, BlockStatementTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11BooleanChecksNotInvertedTest : Java11Test, BooleanChecksNotInvertedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CaseInsensitiveComparisonsDoNotChangeCaseTest : Java11Test, CaseInsensitiveComparisonsDoNotChangeCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CatchClauseOnlyRethrowsTest : Java11Test, CatchClauseOnlyRethrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CompareEnumWithEqualityOperatorTest : Java11Test, CompareEnumWithEqualityOperatorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ControlFlowIndentationTest : Java11Test, ControlFlowIndentationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeFieldNameTest : Java11Test, ChangeFieldNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeFieldTypeTest : Java11Test, ChangeFieldTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeLiteralTest : Java11Test, ChangeLiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodAccessLevelTest : Java11Test, ChangeMethodAccessLevelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodNameTest : Java11Test, ChangeMethodNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodTargetToStaticTest : Java11Test, ChangeMethodTargetToStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodTargetToVariableTest : Java11Test, ChangeMethodTargetToVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangePackageTest : Java11Test, ChangePackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeStaticFieldToMethodTest : Java11Test, ChangeStaticFieldToMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeTypeTest : Java11Test, ChangeTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CovariantEqualsTest : Java11Test, CovariantEqualsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DefaultComesLastTest : Java11Test, DefaultComesLastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DeleteMethodArgumentTest : Java11Test, DeleteMethodArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DeleteStatementTest : Java11Test, DeleteStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DoesNotUseRewriteSkipTest : Java11Test, DoesNotUseRewriteSkipTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11EmptyBlockTest : Java11Test, EmptyBlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11EmptyNewlineAtEndOfFileTest : Java11Test, EmptyNewlineAtEndOfFileTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11EqualsAvoidsNullTest : Java11Test, EqualsAvoidsNullTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ExplicitCharsetOnStringGetBytesTest : Java11Test, ExplicitCharsetOnStringGetBytesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ExecutionContextParameterNameTest : Java11Test, ExecutionContextParameterNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ExplicitInitializationTest : Java11Test, ExplicitInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ExplicitLambdaArgumentTypesTest : Java11Test, ExplicitLambdaArgumentTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ExternalizableHasNoArgsConstructorTest : Java11Test, ExternalizableHasNoArgConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FallThroughTest : Java11Test, FallThroughTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FinalClassTest : Java11Test, FinalClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FinalizeLocalVariablesTest : Java11Test, FinalizeLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FixSerializableFieldsTest : Java11Test, FixSerializableFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ForLoopControlVariablePostfixOperatorsTest : Java11Test, ForLoopControlVariablePostfixOperatorsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ForLoopIncrementInUpdateTest : Java11Test, ForLoopIncrementInUpdateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindAnnotationsTest : Java11Test, FindAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindDeprecatedClassesTest : Java11Test, FindDeprecatedClassesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindDeprecatedFieldsTest : Java11Test, FindDeprecatedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindDeprecatedMethodsTest : Java11Test, FindDeprecatedMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindEmptyClassesTest : Java11Test, FindEmptyClassesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindEmptyMethodsTest : Java11Test, FindEmptyMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindFieldsTest : Java11Test, FindFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindFieldsOfTypeTest : Java11Test, FindFieldsOfTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindInheritedFieldsTest : Java11Test, FindInheritedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindMethodsTest : Java11Test, FindMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindLiteralsTest : Java11Test, FindLiteralsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindLocalFlowPathsTest : Java11Test, FindLocalFlowPathsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DeimplementInterfaceTest : Java11Test, DeimplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ExtractInterfaceTest : Java11Test, ExtractInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindTextTest : Java11Test, FindTextTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindTypesTest : Java11Test, FindTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11GenerateGetterTestAndSetter : Java11Test, GenerateGetterAndSetterVisitorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11HiddenFieldTest : Java11Test, HiddenFieldTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11HideUtilityClassConstructorTest : Java11Test, HideUtilityClassConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11IndexOfChecksShouldUseAStartPositionTest : Java11Test, IndexOfChecksShouldUseAStartPositionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11IndexOfReplaceableByContainsTest : Java11Test, IndexOfReplaceableByContainsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11IndexOfShouldNotCompareGreaterThanZeroTest : Java11Test, IndexOfShouldNotCompareGreaterThanZeroTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11IsEmptyCallOnCollectionsTest : Java11Test, IsEmptyCallOnCollectionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ImplementInterfaceTest : Java11Test, ImplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11InvertConditionTest : Java11Test, InvertConditionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaTemplateTest : Java11Test, JavaTemplateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaTemplateSubstitutionsTest : Java11Test, JavaTemplateSubstitutionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaVisitorTest : Java11Test, JavaVisitorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11LambdaBlockToExpressionTest : Java11Test, LambdaBlockToExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11LineCounterTest : Java11Test, LineCounterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11PotentiallyDeadCodeTest : Java11Test, PotentiallyDeadCodeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MaybeUsesImportTest : Java11Test, MaybeUsesImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodNameCasingTest : Java11Test, MethodNameCasingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MinimumViableSpacingTest : Java11Test, MinimumViableSpacingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MinimumSwitchCasesTest : Java11Test, MinimumSwitchCasesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MissingOverrideAnnotationTest : Java11Test, MissingOverrideAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ModifierOrderTest : Java11Test, ModifierOrderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MultipleVariableDeclarationsTest : Java11Test, MultipleVariableDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NeedBracesTest : Java11Test, NeedBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoDoubleBraceInitializationTest : Java11Test, NoDoubleBraceInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoEqualityInForConditionTest : Java11Test, NoEqualityInForConditionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoFinalizerTest : Java11Test, NoFinalizerTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoRedundantJumpStatementsTest : Java11Test, NoRedundantJumpStatementsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoToStringOnStringTypeTest : Java11Test, NoToStringOnStringTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoValueOfOnStringTypeTest : Java11Test, NoValueOfOnStringTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoStaticImport : Java11Test, NoStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NormalizeFormatTest : Java11Test, NormalizeFormatTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NormalizeLineBreaksTest : Java11Test, NormalizeLineBreaksTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NormalizeTabsOrSpacesTest : Java11Test, NormalizeTabsOrSpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoWhitespaceAfterTest : Java11Test, NoWhitespaceAfterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoWhitespaceBeforeTest : Java11Test, NoWhitespaceBeforeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoEmptyCollectionWithRawTypeTest : Java11Test, NoEmptyCollectionWithRawTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NoPrimitiveWrappersForToStringOrCompareToTest : Java11Test, NoPrimitiveWrappersForToStringOrCompareToTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ObjectFinalizeCallsSuperTest : Java11Test, ObjectFinalizeCallsSuperTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11OperatorWrapTest : Java11Test, OperatorWrapTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11OrderImportsTest : Java11Test, OrderImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11PadEmptyForLoopComponentsTest : Java11Test, PadEmptyForLoopComponentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11PrimitiveWrapperClassConstructorToValueOfTest : Java11Test, PrimitiveWrapperClassConstructorToValueOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RedundantFileCreationTest : Java11Test, RedundantFileCreationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveAnnotationTest : Java11Test, RemoveAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveImplementsTest : Java11Test, RemoveImplementsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveExtraSemicolonsTest : Java11Test, RemoveExtraSemicolonsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveJavaDocAuthorTagTest : Java11Test, RemoveJavaDocAuthorTagTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveRedundantTypeCastTest : Java11Test, RemoveRedundantTypeCastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveUnneededBlockTest : Java11Test, RemoveUnneededBlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveUnusedLocalVariablesTest : Java11Test, RemoveUnusedLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveUnusedPrivateMethodsTest : Java11Test, RemoveUnusedPrivateMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RenameExceptionInEmptyCatchTest : Java11Test, RenameExceptionInEmptyCatchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveImportTest : Java11Test, RemoveImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveObjectsIsNullTest : Java11Test, RemoveObjectsIsNullTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveTrailingWhitespaceTest : Java11Test, RemoveTrailingWhitespaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveUnusedImportsTest : Java11Test, RemoveUnusedImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RenameLocalVariablesToCamelCaseTest : Java11Test, RenameLocalVariablesToCamelCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RenamePrivateFieldsToCamelCaseTest : Java11Test, RenamePrivateFieldsToCamelCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RenameMethodsNamedHashcodeEqualOrTostringTest : Java11Test, RenameMethodsNamedHashcodeEqualOrTostringTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ReplaceDuplicateStringLiteralsTest : Java11Test, ReplaceDuplicateStringLiteralsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ReplaceLambdaWithMethodReferenceTest : Java11Test, ReplaceLambdaWithMethodReferenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RenameVariableTest : Java11Test, RenameVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ReorderMethodArgumentsTest : Java11Test, ReorderMethodArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ReplaceConstantTest : Java11Test, ReplaceConstantTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ResultOfMethodCallIgnoredTest : Java11Test, ResultOfMethodCallIgnoredTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SemanticallyEqualTest : Java11Test, SemanticallyEqualTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SimplifyBooleanExpressionTest : Java11Test, SimplifyBooleanExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SimplifyBooleanReturnTest : Java11Test, SimplifyBooleanReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11StaticMethodNotFinalTest : Java11Test, StaticMethodNotFinalTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SingleLineCommentsTest : Java11Test, SingleLineCommentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SpacesTest : Java11Test, SpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TabsAndIndentsTest : Java11Test, TabsAndIndentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TypeTest : Java11Test, JavaTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TypeTreeTest : Java11Test, TypeTreeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11StringLiteralEqualityTest : Java11Test, StringLiteralEqualityTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NestedEnumsAreNotStaticTest : Java11Test, NestedEnumsAreNotStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NewStringBuilderBufferWithCharArgumentTest : Java11Test, NewStringBuilderBufferWithCharArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SetDefaultEstimatedEffortPerOccurrenceTest : Java11Test, SetDefaultEstimatedEffortPerOccurrenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TypecastParenPadTest : Java11Test, TypecastParenPadTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnnecessaryCloseInTryWithResourcesTest : Java11Test, UnnecessaryCloseInTryWithResourcesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnnecessaryExplicitTypeArgumentsTest : Java11Test, UnnecessaryExplicitTypeArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnnecessaryParenthesesTest : Java11Test, UnnecessaryParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnnecessaryPrimitiveAnnotationsTest : Java11Test, UnnecessaryPrimitiveAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnnecessaryThrowsTest : Java11Test, UnnecessaryThrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnwrapParenthesesTest : Java11Test, UnwrapParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseDiamondOperatorTest : Java11Test, UseDiamondOperatorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseJavaStyleArrayDeclarations : Java11Test, UseJavaStyleArrayDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseLambdaForFunctionalInterface : Java11Test, UseLambdaForFunctionalInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseStandardCharsetTest : Java11Test, UseStandardCharsetTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseStaticImportTest : Java11Test, UseStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UpdateSourcePositionsTest : Java11Test, UpdateSourcePositionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseCollectionInterfacesTest : Java11Test, UseCollectionInterfacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UsesJavaVersionTest : Java11Test, UsesJavaVersionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UriCreatedWithHttpSchemeTest : Java11Test, UriCreatedWithHttpSchemeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UsesMethodTest : Java11Test, UsesMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseStringReplaceTest : Java11Test, UseStringReplaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ReferentialEqualityToObjectEqualsTest : Java11Test, ReferentialEqualityToObjectEqualsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UsesTypeTest : Java11Test, UsesTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SimplifyCompoundStatementTest : Java11Test, SimplifyCompoundStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11WhileInsteadOfForTest : Java11Test, WhileInsteadOfForTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11WrappingAndBracesTest : Java11Test, WrappingAndBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11WriteOctalValuesAsDecimalTest : Java11Test, WriteOctalValuesAsDecimalTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SameClassNameTest : Java11Test, SameClassNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SimplifyConsecutiveAssignmentsTest : Java11Test, SimplifyConsecutiveAssignmentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SimplifyConstantIfBranchExecutionTest : Java11Test, SimplifyConstantIfBranchExecutionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SimplifyMethodChainTest : Java11Test, SimplifyMethodChainTest
