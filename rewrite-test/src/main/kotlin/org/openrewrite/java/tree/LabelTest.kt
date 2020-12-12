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

interface LabelTest {
    
    @Test
    fun labeledWhileLoop(jp: JavaParser) {
        val aSrc = """
            public class A {
                public void test() {
                    labeled : while(true) {
                    }
                }
            }
        """.trimIndent()

        val a = jp.parse(aSrc)[0]
        
        val labeled = a.firstMethodStatement() as J.Label
        assertEquals("labeled", labeled.label.simpleName)
        assertTrue(labeled.statement is J.WhileLoop)
        assertEquals(aSrc, a.print())
    }

    @Test
    fun nonEmptyLabeledWhileLoop(jp: JavaParser) {
        val aSrc = """
            public class A {
                public void test() {
                    outer : while(true) {
                        while(true) {
                            break outer;
                        }
                    }
                }
            }
        """.trimIndent()

        val a = jp.parse(aSrc)[0]

        val labeled = a.firstMethodStatement() as J.Label
        assertEquals("outer", labeled.label.simpleName)
        assertTrue(labeled.statement is J.WhileLoop)
        assertEquals(aSrc, a.print())
    }
}
