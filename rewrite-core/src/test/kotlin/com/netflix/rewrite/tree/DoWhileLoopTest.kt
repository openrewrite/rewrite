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

import com.netflix.rewrite.firstMethodStatement
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class DoWhileLoopTest(p: Parser): Parser by p {
    val a: Tr.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() { do { } while ( true ) ; }
            }
        """)
    }

    private val whileLoop by lazy { a.firstMethodStatement() as Tr.DoWhileLoop }

    @Test
    fun doWhileLoop() {
        assertTrue(whileLoop.condition.tree is Tr.Literal)
        assertTrue(whileLoop.body is Tr.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("{ do { } while ( true ) ; }", a.classes[0].methods[0].body!!.printTrimmed())
    }
}