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
import org.openrewrite.java.search.*

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindAnnotationTest: Java11Test(), FindAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindFieldsTest: Java11Test(), FindFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindInheritedFieldsTest: Java11Test(), FindInheritedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindMethodsTest: Java11Test(), FindMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindReferencesToVariableTest: Java11Test(), FindReferencesToVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11FindTypeTest: Java11Test(), FindTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11HasImportTest: Java11Test(), HasImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11HasTypeTest: Java11Test(), HasTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SemanticallyEqualTest: Java11Test(), SemanticallyEqualTest
