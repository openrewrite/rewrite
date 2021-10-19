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
import org.openrewrite.java.internal.TypeCacheTest
import org.openrewrite.java.search.*
import org.openrewrite.java.style.AutodetectTest
import org.openrewrite.java.tree.TypeTreeTest

//----------------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to JavaVisitorCompatibilityKit.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------------
@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AddImportTest : Java8Test, AddImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AddLicenseHeaderTest : Java8Test, AddLicenseHeaderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AddSerialVersionUidToSerializableTest : Java8Test, AddSerialVersionUidToSerializableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AnnotationTemplateGeneratorTest : Java8Test, AnnotationTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AtomicPrimitiveEqualsUsesGetTest : Java8Test, AtomicPrimitiveEqualsUsesGetTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AutodetectTest : Java8Test, AutodetectTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8BigDecimalRoundingConstantsToEnumsTestTest : Java8Test, BigDecimalRoundingConstantsToEnumsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8BlankLinesTest : Java8Test, BlankLinesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8BlockStatementTemplateGeneratorTest : Java8Test, BlockStatementTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8BooleanChecksNotInvertedTest : Java8Test, BooleanChecksNotInvertedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8CaseInsensitiveComparisonsDoNotChangeCaseTest : Java8Test, CaseInsensitiveComparisonsDoNotChangeCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8CatchClauseOnlyRethrowsTest : Java8Test, CatchClauseOnlyRethrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ControlFlowIndentationTest : Java8Test, ControlFlowIndentationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeFieldNameTest : Java8Test, ChangeFieldNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeFieldTypeTest : Java8Test, ChangeFieldTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeLiteralTest : Java8Test, ChangeLiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeMethodAccessLevelTest : Java8Test, ChangeMethodAccessLevelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeMethodNameTest : Java8Test, ChangeMethodNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeMethodTargetToStaticTest : Java8Test, ChangeMethodTargetToStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeMethodTargetToVariableTest : Java8Test, ChangeMethodTargetToVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangePackageTest : Java8Test, ChangePackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeStaticFieldToMethodTest : Java8Test, ChangeStaticFieldToMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ChangeTypeTest : Java8Test, ChangeTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8CovariantEqualsTest : Java8Test, CovariantEqualsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8DefaultComesLastTest : Java8Test, DefaultComesLastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8DeleteMethodArgumentTest : Java8Test, DeleteMethodArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8DeleteStatementTest : Java8Test, DeleteStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8EmptyBlockTest : Java8Test, EmptyBlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8EmptyNewlineAtEndOfFileTest : Java8Test, EmptyNewlineAtEndOfFileTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8EqualsAvoidsNullTest : Java8Test, EqualsAvoidsNullTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ExplicitInitializationTest : Java8Test, ExplicitInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ExplicitLambdaArgumentTypesTest : Java8Test, ExplicitLambdaArgumentTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ExternalizableHasNoArgConstructorTest : Java8Test, ExternalizableHasNoArgConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FallThroughTest : Java8Test, FallThroughTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FinalClassTest : Java8Test, FinalClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FinalizeLocalVariablesTest : Java8Test, FinalizeLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FixSerializableFieldsTest : Java8Test, FixSerializableFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ForLoopControlVariablePostfixOperatorsTest : Java8Test, ForLoopControlVariablePostfixOperatorsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ForLoopIncrementInUpdateTest : Java8Test, ForLoopIncrementInUpdateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindAnnotationsTest : Java8Test, FindAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindDeprecatedClassesTest : Java8Test, FindDeprecatedClassesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindDeprecatedFieldsTest : Java8Test, FindDeprecatedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindDeprecatedMethodsTest : Java8Test, FindDeprecatedMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindEmptyClassesTest : Java8Test, FindEmptyClassesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindFieldsTest : Java8Test, FindFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindFieldsOfTypeTest : Java8Test, FindFieldsOfTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindInheritedFieldsTest : Java8Test, FindInheritedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindMethodsTest : Java8Test, FindMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindTextTest : Java8Test, FindTextTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindTypesTest : Java8Test, FindTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FindTypesByPackageTest : Java8Test, FindTypesByPackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8HiddenFieldTest : Java8Test, HiddenFieldTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8HideUtilityClassConstructorTest : Java8Test, HideUtilityClassConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8IndexOfChecksShouldUseAStartPositionTest : Java8Test, IndexOfChecksShouldUseAStartPositionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8IndexOfReplaceableByContainsTest : Java8Test, IndexOfReplaceableByContainsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8IndexOfShouldNotCompareGreaterThanZeroTest : Java8Test, IndexOfShouldNotCompareGreaterThanZeroTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8IsEmptyCallOnCollectionsTest : Java8Test, IsEmptyCallOnCollectionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ImplementInterfaceTest : Java8Test, ImplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8InvertConditionTest : Java8Test, InvertConditionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8JavaTemplateTest : Java8Test, JavaTemplateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8JavaTemplateSubstitutionsTest : Java8Test, JavaTemplateSubstitutionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8JavaVisitorTest : Java8Test, JavaVisitorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8LambdaBlockToExpressionTest : Java8Test, LambdaBlockToExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MaybeUsesImportTest : Java8Test, MaybeUsesImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MethodNameCasingTest : Java8Test, MethodNameCasingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MinimumViableSpacingTest : Java8Test, MinimumViableSpacingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MinimumSwitchCasesTest : Java8Test, MinimumSwitchCasesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MissingOverrideAnnotationTest : Java8Test, MissingOverrideAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ModifierOrderTest : Java8Test, ModifierOrderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MultipleVariableDeclarationsTest : Java8Test, MultipleVariableDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NeedBracesTest : Java8Test, NeedBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoDoubleBraceInitializationTest : Java8Test, NoDoubleBraceInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoEqualityInForConditionTest : Java8Test, NoEqualityInForConditionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoFinalizerTest : Java8Test, NoFinalizerTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoRedundantJumpStatementsTest : Java8Test, NoRedundantJumpStatementsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoToStringOnStringTypeTest : Java8Test, NoToStringOnStringTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoValueOfOnStringTypeTest : Java8Test, NoValueOfOnStringTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NormalizeFormatTest : Java8Test, NormalizeFormatTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NormalizeLineBreaksTest : Java8Test, NormalizeLineBreaksTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NormalizeTabsOrSpacesTest : Java8Test, NormalizeTabsOrSpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoStaticImportTest : Java8Test, NoStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoWhitespaceAfterTest : Java8Test, NoWhitespaceAfterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoWhitespaceBeforeTest : Java8Test, NoWhitespaceBeforeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoEmptyCollectionWithRawTypeTest : Java8Test, NoEmptyCollectionWithRawTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NoPrimitiveWrappersForToStringOrCompareToTest : Java8Test, NoPrimitiveWrappersForToStringOrCompareToTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ObjectFinalizeCallsSuperTest : Java8Test, ObjectFinalizeCallsSuperTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8OperatorWrapTest : Java8Test, OperatorWrapTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8OrderImportsTest : Java8Test, OrderImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8PadEmptyForLoopComponentsTest : Java8Test, PadEmptyForLoopComponentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8PrimitiveWrapperClassConstructorToValueOfTest : Java8Test, PrimitiveWrapperClassConstructorToValueOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RedundantFileCreationTest : Java8Test, RedundantFileCreationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveAnnotationTest : Java8Test, RemoveAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveExtraSemicolonsTest : Java8Test, RemoveExtraSemicolonsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveUnusedLocalVariablesTest : Java8Test, RemoveUnusedLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveUnusedPrivateMethodsTest : Java8Test, RemoveUnusedPrivateMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveImportTest : Java8Test, RemoveImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveTrailingWhitespaceTest : Java8Test, RemoveTrailingWhitespaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RemoveUnusedImportsTest : Java8Test, RemoveUnusedImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RenameLocalVariablesToCamelCaseTest : Java8Test, RenameLocalVariablesToCamelCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RenameMethodsNamedHashcodeEqualOrTostringTest : Java8Test, RenameMethodsNamedHashcodeEqualOrTostringTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ReplaceDuplicateStringLiteralsTest : Java8Test, ReplaceDuplicateStringLiteralsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8RenameVariableTest : Java8Test, RenameVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ReorderMethodArgumentsTest : Java8Test, ReorderMethodArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ResultOfMethodCallIgnoredTest : Java8Test, ResultOfMethodCallIgnoredTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SemanticallyEqualTest : Java8Test, SemanticallyEqualTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SimplifyBooleanExpressionTest : Java8Test, SimplifyBooleanExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SimplifyBooleanReturnTest : Java8Test, SimplifyBooleanReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SingleLineCommentsTest : Java8Test, SingleLineCommentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8StaticMethodNotFinalTest : Java8Test, StaticMethodNotFinalTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SpacesTest : Java8Test, SpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TabsAndIndentsTest : Java8Test, TabsAndIndentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TypeCacheTest : Java8Test, TypeCacheTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TypeTest : Java8Test, JavaTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TypeTreeTest : Java8Test, TypeTreeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8StringLiteralEqualityTest : Java8Test, StringLiteralEqualityTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NestedEnumsAreNotStaticTest : Java8Test, NestedEnumsAreNotStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NewStringBuilderBufferWithCharArgumentTest : Java8Test, NewStringBuilderBufferWithCharArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TypecastParenPadTest : Java8Test, TypecastParenPadTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnnecessaryCloseInTryWithResourcesTest : Java8Test, UnnecessaryCloseInTryWithResourcesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnnecessaryExplicitTypeArgumentsTest : Java8Test, UnnecessaryExplicitTypeArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnnecessaryParenthesesTest : Java8Test, UnnecessaryParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnnecessaryPrimitiveAnnotationsTest : Java8Test, UnnecessaryPrimitiveAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnnecessaryThrowsTest : Java8Test, UnnecessaryThrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnwrapParenthesesTest : Java8Test, UnwrapParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UseDiamondOperatorTest : Java8Test, UseDiamondOperatorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UseFilesCreateTempDirectoryTest : Java8Test, UseFilesCreateTempDirectoryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UseStaticImportTest : Java8Test, UseStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UseCollectionInterfacesTest : Java8Test, UseCollectionInterfacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UsesJavaVersionTest : Java8Test, UsesJavaVersionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UseJavaStyleArrayDeclarationsTck : Java8Test, UseJavaStyleArrayDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UseLambdaForFunctionalInterfaceTck : Java8Test, UseLambdaForFunctionalInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UsesMethodTest : Java8Test, UsesMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UsesTypeTest : Java8Test, UsesTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8WhileInsteadOfForTest : Java8Test, WhileInsteadOfForTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8WriteOctalValuesAsDecimalTest : Java8Test, WriteOctalValuesAsDecimalTest

@ExtendWith(JavaParserResolver::class)
class Java8WrappingAndBracesTest : Java8Test, WrappingAndBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SameClassNameTest : Java8Test, SameClassNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SimplifyMethodChainTest : Java8Test, SimplifyMethodChainTest
