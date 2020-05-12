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

open class DoWhileLoopTest : JavaParser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() { do { } while ( true ) ; }
            }
        """)
    }

    private val whileLoop by lazy { a.firstMethodStatement() as J.DoWhileLoop }

    @Test
    fun doWhileLoop() {
        assertTrue(whileLoop.whileCondition.condition.tree is J.Literal)
        assertTrue(whileLoop.body is J.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("{ do { } while ( true ) ; }", a.classes[0].methods[0].body!!.printTrimmed())
    }
}