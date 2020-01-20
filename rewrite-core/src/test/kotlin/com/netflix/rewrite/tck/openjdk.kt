/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tck

import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.tree.*

class OpenJdkAnnotationTest: AnnotationTest(OpenJdkParser())
class OpenJdkParserArrayAccessTest: ArrayAccessTest(OpenJdkParser())
class OpenJdkParserArrayTypeTest: ArrayTypeTest(OpenJdkParser())
class OpenJdkParserAssertTest: AssertTest(OpenJdkParser())
class OpenJdkParserAssignTest: AssignTest(OpenJdkParser())
class OpenJdkParserAssignOpTest: AssignOpTest(OpenJdkParser())
class OpenJdkParserBinaryTest : BinaryTest(OpenJdkParser())
class OpenJdkParserBlockTest : BlockTest(OpenJdkParser())
class OpenJdkParserBodyTest: BodyTest(OpenJdkParser())
class OpenJdkParserBreakTest: BreakTest(OpenJdkParser())
class OpenJdkParserClassDeclTest: ClassDeclTest(OpenJdkParser())
class OpenJdkParserCompilationUnitTest: CompilationUnitTest(OpenJdkParser())
class OpenJdkParserCommentTest: CommentTest(OpenJdkParser())
class OpenJdkParserContinueTest: ContinueTest(OpenJdkParser())
class OpenJdkParserCyclicTypeTest: CyclicTypeTest(OpenJdkParser())
class OpenJdkParserDoWhileLoopTest: DoWhileLoopTest(OpenJdkParser())
class OpenJdkParserEmptyTest: EmptyTest(OpenJdkParser())
class OpenJdkParserFieldAccessTest: FieldAccessTest(OpenJdkParser())
class OpenJdkParserForEachLoopTest: ForEachLoopTest(OpenJdkParser())
class OpenJdkParserForLoopTest: ForLoopTest(OpenJdkParser())
class OpenJdkParserIdentTest: IdentTest(OpenJdkParser())
class OpenJdkParserIfTest: IfTest(OpenJdkParser())

class OpenJdkParserImportTest: ImportTest(OpenJdkParser())
class OpenJdkParserInstanceOfTest: InstanceOfTest(OpenJdkParser())
class OpenJdkParserLabelTest: LabelTest(OpenJdkParser())
class OpenJdkParserLambdaTest: LambdaTest(OpenJdkParser())
class OpenJdkParserLiteralTest: LiteralTest(OpenJdkParser())
class OpenJdkParserMemberReferenceTest: MemberReferenceTest(OpenJdkParser())
class OpenJdkParserMethodDeclTest: MethodDeclTest(OpenJdkParser())
class OpenJdkParserMethodInvocationTest: MethodInvocationTest(OpenJdkParser())
class OpenJdkParserNewArrayTest: NewArrayTest(OpenJdkParser())
class OpenJdkParserNewClassTest: NewClassTest(OpenJdkParser())
class OpenJdkParserParenthesesTest: ParenthesesTest(OpenJdkParser())
class OpenJdkParserPrimitiveTest: PrimitiveTest(OpenJdkParser())
class OpenJdkParserReturnTest: ReturnTest(OpenJdkParser())
class OpenJdkParserSwitchTest: SwitchTest(OpenJdkParser())
class OpenJdkParserSynchronizedTest: SynchronizedTest(OpenJdkParser())
class OpenJdkParserTernaryTest: TernaryTest(OpenJdkParser())
class OpenJdkParserThrowTest: ThrowTest(OpenJdkParser())
class OpenJdkParserTryCatchTest : TryCatchTest(OpenJdkParser())
class OpenJdkParserTypeCastTest: TypeCastTest(OpenJdkParser())
class OpenJdkParserTypeParameterAndWildcardTest : TypeParameterAndWildcardTest(OpenJdkParser())
class OpenJdkParserUnaryTest: UnaryTest(OpenJdkParser())
class OpenJdkParserVariableDeclsTest : VariableDeclsTest(OpenJdkParser())
class OpenJdkParserWhileLoopTest: WhileLoopTest(OpenJdkParser())
