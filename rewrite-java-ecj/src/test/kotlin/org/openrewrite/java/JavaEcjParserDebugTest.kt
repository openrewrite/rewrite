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
import org.openrewrite.java.tree.*

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjAnnotationTest : JavaEcjTest(), AnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjArrayAccessTest : JavaEcjTest(), ArrayAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjArrayTypeTest : JavaEcjTest(), ArrayTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjAssertTest : JavaEcjTest(), AssertTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjAssignOpTest : JavaEcjTest(), AssignOpTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjAssignTest : JavaEcjTest(), AssignTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjBlockTest : JavaEcjTest(), BlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjBreakTest : JavaEcjTest(), BreakTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjBodyTest : JavaEcjTest(), BodyTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjClassDeclTest : JavaEcjTest(), ClassDeclTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjCommentTest : JavaEcjTest(), CommentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjCompilationUnitTest : JavaEcjTest(), CompilationUnitTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjContinueTest : JavaEcjTest(), ContinueTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjCursorTest : JavaEcjTest(), CursorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjCyclicTypeTest : JavaEcjTest(), CyclicTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjDoWhileLoopTest : JavaEcjTest(), DoWhileLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjEmptyTest : JavaEcjTest(), EmptyTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjEnumTest : JavaEcjTest(), EnumTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjFieldAccessTest : JavaEcjTest(), FieldAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjForEachLoopTest : JavaEcjTest(), ForEachLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjIdentTest : JavaEcjTest(), IdentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjIfTest : JavaEcjTest(), IfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjImportTest : JavaEcjTest(), ImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjInstanceofTest : JavaEcjTest(), InstanceOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjJavaTypeTest : JavaEcjTest(), JavaTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjLabelTest : JavaEcjTest(), LabelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjLambdaTest : JavaEcjTest(), LambdaTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjLiteralTest : JavaEcjTest(), LiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjMemberReferenceTest : JavaEcjTest(), MemberReferenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjMethodDeclTest : JavaEcjTest(), MethodDeclTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjMethodInvocationTest : JavaEcjTest(), MethodInvocationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjModifierTest : JavaEcjTest(), ModifierTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjNewArrayTest : JavaEcjTest(), NewArrayTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjNewClassTest : JavaEcjTest(), NewClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjPackageTest : JavaEcjTest(), PackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjParenthesesTest : JavaEcjTest(), ParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjPrimitiveTest : JavaEcjTest(), PrimitiveTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjReturnTest : JavaEcjTest(), ReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjSwitchTest : JavaEcjTest(), SwitchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjSynchronizedTest : JavaEcjTest(), SynchronizedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjTernaryTest : JavaEcjTest(), TernaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjThrowTest : JavaEcjTest(), ThrowTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjTreeBuilderTest : JavaEcjTest(), TreeBuilderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjCompilationUnitSerializerTest : JavaEcjTest(), CompilationUnitSerializerTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjTryCatchTest : JavaEcjTest(), TryCatchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjTypeCastTest : JavaEcjTest(), TypeCastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjTypeParameterAndWildcardTest : JavaEcjTest(), TypeParameterAndWildcardTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjUnaryTest : JavaEcjTest(), UnaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjVariableDeclsTest : JavaEcjTest(), VariableDeclsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaEcjWhileLoopTest : JavaEcjTest(), WhileLoopTest
