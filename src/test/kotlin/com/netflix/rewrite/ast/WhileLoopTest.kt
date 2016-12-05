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
package com.netflix.rewrite.ast

import com.netflix.rewrite.firstMethodStatement
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class WhileLoopTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            |public class A {
            |    public void test() {
            |        while ( true ) { }
            |    }
            |}
        """)
    }

    val whileLoop by lazy { a.firstMethodStatement() as Tr.WhileLoop }

    @Test
    fun whileLoop() {
        assertTrue(whileLoop.condition.tree is Tr.Literal)
        assertTrue(whileLoop.body is Tr.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("while ( true ) { }", whileLoop.printTrimmed())
    }

    @Test
    fun statementTerminatorForSingleLineWhileLoops() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) test();
                }
            }
        """)

        val forLoop = a.classes[0].methods()[0].body!!.statements[0] as Tr.WhileLoop
        assertEquals("while(true) test();", forLoop.printTrimmed())
    }
}