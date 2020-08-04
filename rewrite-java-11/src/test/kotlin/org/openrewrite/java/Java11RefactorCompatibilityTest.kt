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
@file:Suppress("unused")

package org.openrewrite.java

import org.junit.jupiter.api.extension.ExtendWith

class Java11RefactorCompatibilityTest: JavaRefactorCompatibilityKit() {
    override fun javaParser(): Java11Parser = Java11Parser.builder().build()
}

abstract class Java11Test {
    fun javaParser(): Java11Parser = Java11Parser.builder().build()
}

//@ExtendWith(JavaParserResolver::class)
//class Java11AddAnnotationTest: Java11Test(), AddAnnotationTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11AddFieldTest: Java11Test(), AddFieldTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11AddImportTest: Java11Test(), AddImportTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeFieldNameTest: Java11Test(), ChangeFieldNameTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeFieldTypeTest: Java11Test(), ChangeFieldTypeTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeLiteralTest: Java11Test(), ChangeLiteralTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeMethodNameTest: Java11Test(), ChangeMethodNameTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeMethodTargetToStaticTest: Java11Test(), ChangeMethodTargetToStaticTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeMethodTargetToVariableTest: Java11Test(), ChangeMethodTargetToVariableTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ChangeTypeTest: Java11Test(), ChangeTypeTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11DeleteMethodArgumentTest: Java11Test(), DeleteMethodArgumentTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11DeleteStatementTest: Java11Test(), DeleteStatementTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11GenerateConstructorUsingFieldsTest: Java11Test(), GenerateConstructorUsingFieldsTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11ImplementInterfaceTest: Java11Test(), ImplementInterfaceTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11InsertMethodArgumentTest: Java11Test(), InsertMethodArgumentTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11MethodMatcherTest: Java11Test(), MethodMatcherTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11OrderImportTest: Java11Test(), OrderImportTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11RemoveImportTest: Java11Test(), RemoveImportTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11RenameVariableTest: Java11Test(), RenameVariableTest
//
//@ExtendWith(JavaParserResolver::class)
//class JavaReorderMethodArgumentsTest: Java11Test(), ReorderMethodArgumentsTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11UnwrapParenthesesTest: Java11Test(), UnwrapParenthesesTest
//
//@ExtendWith(JavaParserResolver::class)
//class Java11UseStaticImportTest: Java11Test(), UseStaticImportTest
