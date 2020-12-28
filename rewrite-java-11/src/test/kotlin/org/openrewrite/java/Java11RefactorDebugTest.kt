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
import org.openrewrite.java.tree.AutoFormatTest
import org.openrewrite.java.tree.MethodTypeBuilderTest
import org.openrewrite.java.utilities.SpansMultipleLinesTest
import org.openrewrite.java.tree.TreeBuilderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AddAnnotationTest: Java11Test(), AddAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AddFieldTest: Java11Test(), AddFieldTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AddImportTest: Java11Test(), AddImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeFieldNameTest: Java11Test(), ChangeFieldNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeFieldTypeTest: Java11Test(), ChangeFieldTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeLiteralTest: Java11Test(), ChangeLiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodNameTest: Java11Test(), ChangeMethodNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodTargetToStaticTest: Java11Test(), ChangeMethodTargetToStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeMethodTargetToVariableTest: Java11Test(), ChangeMethodTargetToVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ChangeTypeTest: Java11Test(), ChangeTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DeleteMethodArgumentTest: Java11Test(), DeleteMethodArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11DeleteStatementTest: Java11Test(), DeleteStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11GenerateNewBeanUsingPropertiesTest: Java11Test(), GenerateNewBeanUsingPropertiesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11GenerateConstructorUsingFieldsTest: Java11Test(), GenerateConstructorUsingFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11GenerateGetter: Java11Test(), GenerateGetterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11GenerateSetter: Java11Test(), GenerateSetterTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11ImplementInterfaceTest: Java11Test(), ImplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11InsertDeclaration: Java11Test(), InsertDeclarationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11InsertMethodArgumentTest: Java11Test(), InsertMethodArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodMatcherTest: Java11Test(), MethodMatcherTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11OrderDeclarationsTest: Java11Test(), OrderDeclarationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11OrderImportsTest: Java11Test(), OrderImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveImportTest: Java11Test(), RemoveImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RemoveUnusedImportsTest: Java11Test(), RemoveUnusedOrdersTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11RenameVariableTest: Java11Test(), RenameVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class JavaReorderMethodArgumentsTest: Java11Test(), ReorderMethodArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SpansMultipleLinesTest: Java11Test(), SpansMultipleLinesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UnwrapParenthesesTest: Java11Test(), UnwrapParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11UseStaticImportTest: Java11Test(), UseStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11TreeBuilderTest: Java11Test(), TreeBuilderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11MethodTypeBuilderTest: Java11Test(), MethodTypeBuilderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11AutoFormatTest: Java11Test(), AutoFormatTest
