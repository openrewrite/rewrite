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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class LabelTest : JavaParser() {
    
    @Test
    fun labeledWhileLoop() {
        val orig = """
            |public class A {
            |    public void test() {
            |        labeled: while(true) {
            |        }
            |    }
            |}
        """.trimMargin()
        val a = parse(orig)
        
        val labeled = a.firstMethodStatement() as J.Label
        assertEquals("labeled", labeled.label.simpleName)
        assertTrue(labeled.statement is J.WhileLoop)
        assertEquals(orig, a.print())
    }

    @Test
    fun nonEmptyLabeledWhileLoop() {
        val orig = """
            |public class A {
            |    public void test() {
            |        outer: while(true) {
            |            while(true) {
            |                break outer;
            |            }
            |        }
            |    }
            |}
        """.trimMargin()

        val a = parse(orig)

        val labeled = a.firstMethodStatement() as J.Label
        assertEquals("outer", labeled.label.simpleName)
        assertTrue(labeled.statement is J.WhileLoop)
        assertEquals(orig, a.print())
    }
}
