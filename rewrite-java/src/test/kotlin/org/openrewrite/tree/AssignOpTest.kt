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
import org.openrewrite.JavaParser
import org.openrewrite.firstMethodStatement

open class AssignOpTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                int n = 0;
                public void test() {
                    n += 1;
                }
            }
        """)
    }

    private val assign by lazy {
        a.firstMethodStatement() as J.AssignOp
    }

    @Test
    fun compoundAssignment() {
        assertTrue(assign.operator is J.AssignOp.Operator.Addition)
    }

    @Test
    fun format() {
        assertEquals("n += 1", assign.printTrimmed())
    }
}