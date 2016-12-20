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
package com.netflix.rewrite.ast.tck

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.parse.OracleJdkParser

class OracleParserCompilationUnitTest: CompilationUnitTest(OracleJdkParser())
class OracleParserImportTest: ImportTest(OracleJdkParser())
class OracleParserNewClassTest: NewClassTest(OracleJdkParser())
class OracleParserClassDeclTest: ClassDeclTest(OracleJdkParser())
class OracleParserLiteralTest: LiteralTest(OracleJdkParser())
class OracleParserIdentTest: IdentTest(OracleJdkParser())
class OracleParserFieldAccessTest: FieldAccessTest(OracleJdkParser())
class OracleParserMethodDeclTest: MethodDeclTest(OracleJdkParser())
class OracleParserPrimitiveTest: PrimitiveTest(OracleJdkParser())
class OracleParserBlockTest : BlockTest(OracleJdkParser())
class OracleParserMethodInvocationTest: MethodInvocationTest(OracleJdkParser())
class OracleParserBinaryTest : BinaryTest(OracleJdkParser())
class OracleParserUnaryTest: UnaryTest(OracleJdkParser())
class OracleParserForLoopTest: ForLoopTest(OracleJdkParser())
class OracleParserForEachLoopTest: ForEachLoopTest(OracleJdkParser())
class OracleParserIfTest: IfTest(OracleJdkParser())
class OracleParserTernaryTest: TernaryTest(OracleJdkParser())
class OracleParserWhileLoopTest: WhileLoopTest(OracleJdkParser())
class OracleParserDoWhileLoopTest: DoWhileLoopTest(OracleJdkParser())
class OracleParserBreakTest: BreakTest(OracleJdkParser())
class OracleParserContinueTest: ContinueTest(OracleJdkParser())
class OracleParserLabelTest: LabelTest(OracleJdkParser())
class OracleParserVariableDeclsTest : VariableDeclTest(OracleJdkParser())
class OracleParserReturnTest: ReturnTest(OracleJdkParser())
class OracleParserSwitchTest: SwitchTest(OracleJdkParser())
class OracleParserAssignTest: AssignTest(OracleJdkParser())
class OracleParserThrowTest: ThrowTest(OracleJdkParser())
class OracleParserTryCatchTest : TryCatchTest(OracleJdkParser())
class OracleParserSynchronizedTest: SynchronizedTest(OracleJdkParser())
class OracleParserEmptyTest: EmptyTest(OracleJdkParser())
class OracleParserParenthesesTest: ParenthesesTest(OracleJdkParser())
class OracleParserAssignOpTest: AssignOpTest(OracleJdkParser())
class OracleParserInstanceOfTest: InstanceOfTest(OracleJdkParser())
class OracleParserNewArrayTest: NewArrayTest(OracleJdkParser())
class OracleParserArrayAccessTest: ArrayAccessTest(OracleJdkParser())
class OracleParserLambdaTest: LambdaTest(OracleJdkParser())
class OracleCyclicTypeTest: CyclicTypeTest(OracleJdkParser())
class OracleAnnotationTest: AnnotationTest(OracleJdkParser())
class OracleTypeParameterAndWildcardTest : TypeParameterAndWildcardTest(OracleJdkParser())
class OracleTypeCastTest: TypeCastTest(OracleJdkParser())
class OracleArrayTypeTest: ArrayTypeTest(OracleJdkParser())
class OracleCommentTest: CommentTest(OracleJdkParser())