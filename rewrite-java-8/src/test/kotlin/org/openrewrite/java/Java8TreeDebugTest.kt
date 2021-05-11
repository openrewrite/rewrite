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
class Java8AnnotationTest: Java8Test, AnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ArrayAccessTest: Java8Test, ArrayAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ArrayTypeTest: Java8Test, ArrayTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AssertTest: Java8Test, AssertTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AssignmentOperationTest: Java8Test, AssignmentOperationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8AssignmentTest: Java8Test, AssignmentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8BlockTest: Java8Test, BlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8BreakTest: Java8Test, BreakTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ClassDeclarationTest: Java8Test, ClassDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8CommentTest: Java8Test, CommentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8CompilationUnitTest: Java8Test, CompilationUnitTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ContinueTest: Java8Test, ContinueTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8DoWhileLoopTest: Java8Test, DoWhileLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8EmptyTest: Java8Test, EmptyTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8EnumTest: Java8Test, EnumTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8FieldAccessTest: Java8Test, FieldAccessTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ForEachLoopTest: Java8Test, ForEachLoopTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8IdentifierTest: Java8Test, IdentifierTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8IfTest: Java8Test, IfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ImportTest: Java8Test, ImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8InstanceOfTest: Java8Test, InstanceOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8JavaParserTest: Java8Test, JavaParserTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8JavaTypeSerializerTest: Java8Test, JavaTypeSerializerTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8LabelTest: Java8Test, LabelTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8LambdaTest: Java8Test, LambdaTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8LiteralTest: Java8Test, LiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MemberReferenceTest: Java8Test, MemberReferenceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MethodDeclarationTest: Java8Test, MethodDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MethodInvocationTest: Java8Test, MethodInvocationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8MethodMatcherTest: Java8Test, MethodMatcherTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NewArrayTest: Java8Test, NewArrayTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8NewClassTest: Java8Test, NewClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8PackageTest: Java8Test, PackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ParenthesesTest: Java8Test, ParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8PrimitiveTest: Java8Test, PrimitiveTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ReturnTest: Java8Test, ReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SwitchTest: Java8Test, SwitchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8SynchronizedTest: Java8Test, SynchronizedTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TernaryTest: Java8Test, TernaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8ThrowTest: Java8Test, ThrowTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TryCatchTest: Java8Test, TryCatchTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TypeCastTest: Java8Test, TypeCastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8TypeParameterAndWildcardTest: Java8Test, TypeParameterAndWildcardTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8UnaryTest: Java8Test, UnaryTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8VariableDeclarationsTest: Java8Test, VariableDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java8WhileLoopTest: Java8Test, WhileLoopTest
