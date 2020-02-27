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

open class UnaryTest : JavaParser() {
    
    @Test
    fun negation() {
        val a = parse("""
            public class A {
                boolean b = !(1 == 2);
            }
        """)

        val unary = a.classes[0].fields[0].vars[0].initializer as J.Unary
        assertTrue(unary.operator is J.Unary.Operator.Not)
        assertTrue(unary.expr is J.Parentheses<*>)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int i = 0;
                int j = ++i;
                int k = i ++;
            }
        """)

        val (prefix, postfix) = a.classes[0].fields.subList(1, 3)
        assertEquals("int j = ++i", prefix.printTrimmed())
        assertEquals("int k = i ++", postfix.printTrimmed())
    }
}