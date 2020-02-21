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
package com.netflix.rewrite.tree

import com.netflix.rewrite.asClass
import com.netflix.rewrite.firstMethodStatement
import com.netflix.rewrite.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

open class TernaryTest : Parser() {
    val a: Tr.CompilationUnit by lazy {
        parse("""
            public class A {
                int n;
                public void test() {
                    String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                }
            }
        """)
    }

    private val evenOrOdd by lazy { a.firstMethodStatement() as Tr.VariableDecls }
    private val ternary by lazy { evenOrOdd.vars[0].initializer as Tr.Ternary }

    @Test
    fun ternary() {
        assertEquals("java.lang.String", ternary.type.asClass()?.fullyQualifiedName)
        assertTrue(ternary.condition is Tr.Binary)
        assertTrue(ternary.truePart is Tr.Literal)
        assertTrue(ternary.falsePart is Tr.Literal)
    }

    @Test
    fun format() {
        assertEquals("""n % 2 == 0 ? "even" : "odd"""", ternary.printTrimmed())
    }
}