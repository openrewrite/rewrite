/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class VariableDeclarationsTest : GroovyTreeTest {

    @Test
    fun singleVariableDeclaration() = assertParsePrintAndProcess(
        "def a = 1"
    )

    @Test
    fun singleVariableDeclarationStaticallyTyped() = assertParsePrintAndProcess(
        """
            int a = 1
            List<String> l
        """
    )

    @Disabled
    @Test
    fun typeParamWithUpperBound() = assertParsePrintAndProcess(
        """
            List<? extends String> l
        """
    )

    @Disabled
    @Test
    fun typeParamWithLowerBound() = assertParsePrintAndProcess(
        """
            List<? super String> l
            """
    )

    @Disabled
    @Test
    fun singleTypeMultipleVariableDeclaration() = assertParsePrintAndProcess(
        "def a = 1, b = 1"
    )

    @Disabled
    @Test
    fun multipleTypeMultipleVariableDeclaration() = assertParsePrintAndProcess(
        "def a = 1, b = 's'"
    )

    @Test
    fun genericVariableDeclaration() = assertParsePrintAndProcess(
        "def a = new HashMap<String, String>()"
    )
}
