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
import com.netflix.rewrite.Parser
import org.junit.Assert.*
import org.junit.Test

open class BreakTest : Parser() {

    @Test
    fun breakFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) break;
                }
            }
        """)

        val whileLoop = a.firstMethodStatement() as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertNull((whileLoop.body as Tr.Break).label)
    }

    @Test
    fun breakFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertEquals("labeled", (whileLoop.body as Tr.Break).label?.simpleName)
    }

    @Test
    fun formatLabeledBreak() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertEquals("break labeled", whileLoop.body.printTrimmed())
    }
}