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
class Java11ClassDeclTest: Java11Test(), ClassDeclTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11CompilationUnitTest: Java11Test(), CompilationUnitTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11EnumTest: Java11Test(), EnumTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaTypeTest: Java11Test(), JavaTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11NewClassTest: Java11Test(), NewClassTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SwitchTest: Java11Test(), SwitchTest
