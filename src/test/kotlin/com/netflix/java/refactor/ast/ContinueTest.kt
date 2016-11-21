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
package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class ContinueTest(p: Parser): Parser by p {
    
    @Test
    fun continueFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) continue;
                }
            }
        """)
        
        val whileLoop = a.firstMethodStatement() as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Continue)
        assertNull((whileLoop.body as Tr.Continue).label)
    }

    @Test
    fun continueFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        continue labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Continue)
        assertEquals("labeled", (whileLoop.body as Tr.Continue).label?.name)
    }

    @Test
    fun formatContinueLabeled() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        continue labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertEquals("continue labeled", whileLoop.body.printTrimmed())
    }
}