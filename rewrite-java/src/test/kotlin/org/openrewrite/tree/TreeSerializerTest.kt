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
package org.openrewrite.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Parser

/**
 * Test that flyweights survive a serialization/deserialization cycle
 */
open class TreeSerializerTest : Parser() {

    private val serializer = TreeSerializer()

    private val aSource = """
            public class A {
                A a = foo();
                A a2 = foo();

                public A foo() { return null; }
            }
        """

    @Test
    fun `round trip serialization of AST preserves flyweights`() {
        val a = parse(aSource)
        val aBytes = serializer.write(a)
        val aDeser = serializer.read(aBytes)

        assertEquals(a, aDeser)
        assertTrue(a.classes[0].type === aDeser.classes[0].type)
        assertTrue((a.classes[0].fields + aDeser.classes[0].fields)
                .map { it.vars[0].initializer?.type }
                .toSet()
                .size == 1)
    }

    @Test
    fun `round trip serialization of AST list`() {
        val a = parse(aSource)
        val aBytes = serializer.write(listOf(a))
        assertEquals(a, serializer.readList(aBytes)[0])
    }
}