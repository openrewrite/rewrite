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
import org.openrewrite.firstMethodStatement

open class ForLoopTest : Parser() {
    
    @Test
    fun forLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(int i = 0; i < 10; i++) {
                    }
                }
            }
        """)
        
        val forLoop = a.firstMethodStatement() as J.ForLoop
        assertTrue(forLoop.control.init is J.VariableDecls)
        assertTrue(forLoop.control.condition is J.Binary)
        assertEquals(1, forLoop.control.update.size)
    }

    @Test
    fun infiniteLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(;;) {
                    }
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as J.ForLoop
        assertTrue(forLoop.control.init is J.Empty)
        assertTrue(forLoop.control.condition is J.Empty)
        assertTrue(forLoop.control.update[0] is J.Empty)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public void test() {
                    for ( int i = 0 ; i < 10 ; i++ ) {
                    }
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as J.ForLoop
        assertEquals("for ( int i = 0 ; i < 10 ; i++ ) {\n}", forLoop.printTrimmed())
    }

    @Test
    fun formatInfiniteLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for ( ; ; ) {}
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as J.ForLoop
        assertEquals("for ( ; ; ) {}", forLoop.printTrimmed())
    }

    @Test
    fun formatLoopNoInit() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    for ( ; i < 10 ; i++ ) {}
                }
            }
        """)

        // FIXME are body statements printed out of order?
        val forLoop = a.classes[0].methods[0].body!!.statements[1] as J.ForLoop
        assertEquals("for ( ; i < 10 ; i++ ) {}", forLoop.printTrimmed())
    }

    @Test
    fun formatLoopNoCondition() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    for(; i < 10; i++) {}
                }
            }
        """)

        val forLoop = a.classes[0].methods[0].body!!.statements[1] as J.ForLoop
        assertEquals("for(; i < 10; i++) {}", forLoop.printTrimmed())
    }

    @Test
    fun statementTerminatorForSingleLineForLoops() {
        val a = parse("""
            public class A {
                public void test() {
                    for(;;) test();
                }
            }
        """)

        val forLoop = a.classes[0].methods[0].body!!.statements[0] as J.ForLoop
        assertEquals("for(;;) test();", forLoop.printTrimmed())
    }

    @Test
    fun initializerIsAnAssignment() {
        val a = parse("""
            public class A {
                int[] a;
                public void test() {
                    int i=0;
                    for(i=0; i<a.length; i++) {}
                }
            }
        """)

        val forLoop = a.classes[0].methods[0].body!!.statements[1]
        assertEquals("for(i=0; i<a.length; i++) {}", forLoop.printTrimmed())
    }

    @Test
    fun multiVariableInitialization() {
        val a = parse("""
            public class A {
                public void test() {
                    for(int i, j = 0;;) {}
                }
            }
        """)

        assertEquals("for(int i, j = 0;;) {}", a.classes[0].methods[0].body!!.statements[0].printTrimmed())
    }
}