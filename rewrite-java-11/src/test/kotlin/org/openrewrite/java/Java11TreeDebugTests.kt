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

//----------------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to JavaTreeCompatibilityKit.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------------
@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AnnotationTest: Java11Test, AnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ArrayAccessTest: Java11Test, ArrayAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ArrayTypeTest: Java11Test, ArrayTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AssertTest: Java11Test, AssertTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AssignmentOperationTest: Java11Test, AssignmentOperationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AssignmentTest: Java11Test, AssignmentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11BlockTest: Java11Test, BlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11BreakTest: Java11Test, BreakTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ClassDeclarationTest: Java11Test, ClassDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CommentTest: Java11Test, CommentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CompilationUnitTest: Java11Test, CompilationUnitTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ContinueTest: Java11Test, ContinueTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DoWhileLoopTest: Java11Test, DoWhileLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11EmptyTest: Java11Test, EmptyTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11EnumTest: Java11Test, EnumTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FieldAccessTest: Java11Test, FieldAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ForLoopTest: Java11Test, ForLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ForEachLoopTest: Java11Test, ForEachLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11IdentifierTest: Java11Test, IdentifierTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11IfTest: Java11Test, IfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ImportTest: Java11Test, ImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11InstanceOfTest: Java11Test, InstanceOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaParserTest: Java11Test, JavaParserTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaTypeSerializerTest: Java11Test, JavaTypeSerializerTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11LabelTest: Java11Test, LabelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11LambdaTest: Java11Test, LambdaTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11LiteralTest: Java11Test, LiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MemberReferenceTest: Java11Test, MemberReferenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodDeclarationTest: Java11Test, MethodDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodInvocationTest: Java11Test, MethodInvocationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodMatcherTest: Java11Test, MethodMatcherTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodParamPadTest: Java11Test, MethodParamPadTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NewArrayTest: Java11Test, NewArrayTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NewClassTest: Java11Test, NewClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11PackageTest: Java11Test, PackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ParenthesesTest: Java11Test, ParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11PrimitiveTest: Java11Test, PrimitiveTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ReturnTest: Java11Test, ReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SwitchTest: Java11Test, SwitchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SynchronizedTest: Java11Test, SynchronizedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TernaryTest: Java11Test, TernaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ThrowTest: Java11Test, ThrowTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TryCatchTest: Java11Test, TryCatchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TypeCastTest: Java11Test, TypeCastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TypeParameterAndWildcardTest: Java11Test, TypeParameterAndWildcardTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnaryTest: Java11Test, UnaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11VariableDeclarationsTest: Java11Test, VariableDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11WhileLoopTest: Java11Test, WhileLoopTest
