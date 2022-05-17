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
class Java17AnnotationTest: Java17Test, AnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ArrayAccessTest: Java17Test, ArrayAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ArrayTypeTest: Java17Test, ArrayTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AssertTest: Java17Test, AssertTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AssignmentOperationTest: Java17Test, AssignmentOperationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AssignmentTest: Java17Test, AssignmentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BinaryTest: Java17Test, BinaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BlockTest: Java17Test, BlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BreakTest: Java17Test, BreakTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ClassDeclarationTest: Java17Test, ClassDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CommentTest: Java17Test, CommentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CompilationUnitTest: Java17Test, CompilationUnitTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ContinueTest: Java17Test, ContinueTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DoWhileLoopTest: Java17Test, DoWhileLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EmptyTest: Java17Test, EmptyTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EnumTest: Java17Test, EnumTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FieldAccessTest: Java17Test, FieldAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ForLoopTest: Java17Test, ForLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ForEachLoopTest: Java17Test, ForEachLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17IdentifierTest: Java17Test, IdentifierTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17IfTest: Java17Test, IfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ImportTest: Java17Test, ImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17InstanceOfTest: Java17Test, InstanceOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavadocTest: Java17Test, JavadocTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaParserTest: Java17Test, JavaParserTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17LabelTest: Java17Test, LabelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17LambdaTest: Java17Test, LambdaTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17LiteralTest: Java17Test, LiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MemberReferenceTest: Java17Test, MemberReferenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MethodDeclarationTest: Java17Test, MethodDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MethodInvocationTest: Java17Test, MethodInvocationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MethodMatcherTest: Java17Test, MethodMatcherTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MethodParamPadTest: Java17Test, MethodParamPadTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NewArrayTest: Java17Test, NewArrayTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NewClassTest: Java17Test, NewClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17PackageTest: Java17Test, PackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ParenthesesTest: Java17Test, ParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17PrimitiveTest: Java17Test, PrimitiveTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ReturnTest: Java17Test, ReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SwitchTest: Java17Test, SwitchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SynchronizedTest: Java17Test, SynchronizedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TernaryTest: Java17Test, TernaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ThrowTest: Java17Test, ThrowTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TryCatchTest: Java17Test, TryCatchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypeCastTest: Java17Test, TypeCastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypeParameterAndWildcardTest: Java17Test, TypeParameterAndWildcardTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypeUtilsTest: Java17Test, TypeUtilsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnaryTest: Java17Test, UnaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17VariableDeclarationsTest: Java17Test, VariableDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17WhileLoopTest: Java17Test, WhileLoopTest
