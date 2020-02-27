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
import org.openrewrite.asClass
import org.openrewrite.firstMethodStatement

open class TernaryTest : Parser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                int n;
                public void test() {
                    String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                }
            }
        """)
    }

    private val evenOrOdd by lazy { a.firstMethodStatement() as J.VariableDecls }
    private val ternary by lazy { evenOrOdd.vars[0].initializer as J.Ternary }

    @Test
    fun ternary() {
        assertEquals("java.lang.String", ternary.type.asClass()?.fullyQualifiedName)
        assertTrue(ternary.condition is J.Binary)
        assertTrue(ternary.truePart is J.Literal)
        assertTrue(ternary.falsePart is J.Literal)
    }

    @Test
    fun format() {
        assertEquals("""n % 2 == 0 ? "even" : "odd"""", ternary.printTrimmed())
    }
}