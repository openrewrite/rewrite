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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

interface SwitchTest {
    
    @Test
    fun switch(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    case 0: break;
                    }
                }
            }
        """)[0]
        
        val switch = a.firstMethodStatement() as J.Switch
        assertTrue(switch.selector.tree is J.Ident)
        assertEquals(1, switch.cases.statements.size)
        
        val case0 = switch.cases.statements[0]
        assertTrue(case0.pattern is J.Literal)
        assertTrue(case0.statements[0] is J.Break)
    }
    
    @Test
    fun switchWithDefault(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    default: System.out.println("default!");
                    }
                }
            }
        """)[0]

        val switch = a.firstMethodStatement() as J.Switch
        assertTrue(switch.selector.tree is J.Ident)
        assertEquals(1, switch.cases.statements.size)

        val default = switch.cases.statements[0]
        assertEquals("default", (default.pattern as J.Ident).simpleName)
        assertTrue(default.statements[0] is J.MethodInvocation)
    }

    @Test
    fun format(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    default: break;
                    }
                }
            }
        """)[0]

        val switch = a.firstMethodStatement() as J.Switch
        assertEquals("""
            switch(n) {
            default: break;
            }
        """.trimIndent(), switch.printTrimmed())
    }
    
    @Test
    fun switchWithNoCases(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {}
                }
            }
        """)[0]

        val switch = a.firstMethodStatement() as J.Switch
        assertTrue(switch.selector.tree is J.Ident)
        assertEquals(0, switch.cases.statements.size)
    }

    @Test
    fun multipleCases(jp: JavaParser) {
        val aSrc = """
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    case 0: {
                       break;
                    }
                    case 1: {
                       break;
                    }
                    }
                }
            }
        """.trimIndent()

        assertEquals(aSrc, jp.parse(aSrc)[0].printTrimmed())
    }
}
