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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

interface IfTest {

    @Test
    fun ifElse(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) {
                    }
                    else if(n == 1) {
                    }
                    else {
                    }
                }
            }
        """)[0]

        val iff = a.firstMethodStatement() as J.If

        assertTrue(iff.ifCondition.tree is J.Binary)
        assertTrue(iff.thenPart is J.Block<*>)
        
        assertTrue(iff.elsePart?.statement is J.If)
        val elseIf = iff.elsePart?.statement as J.If
        assertTrue(elseIf.ifCondition.tree is J.Binary)
        assertTrue(elseIf.thenPart is J.Block<*>)
        assertTrue(elseIf.elsePart?.statement is J.Block<*>)

        assertEquals("""
            if(n == 0) {
            }
            else if(n == 1) {
            }
            else {
            }
        """.trimIndent(), iff.printTrimmed())
    }
    
    @Test
    fun noElse(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) {} 
                }
            }
        """)[0]
        
        val iff = a.firstMethodStatement() as J.If
        assertNull(iff.elsePart)
    }

    @Test
    fun singleLineIfElseStatements(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) test();
                    else if(n == 1) test();
                    else test();
                }
            }
        """)[0]

        assertEquals("""
            public void test() {
                if(n == 0) test();
                else if(n == 1) test();
                else test();
            }
        """.trimIndent(), a.classes[0].methods[0].printTrimmed())
    }
}
