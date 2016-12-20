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
package com.netflix.rewrite.ast

import com.netflix.rewrite.fields
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class UnaryTest(p: Parser): Parser by p {
    
    @Test
    fun negation() {
        val a = parse("""
            public class A {
                boolean b = !(1 == 2);
            }
        """)

        val unary = a.fields()[0].vars[0].initializer as Tr.Unary
        assertTrue(unary.operator is Tr.Unary.Operator.Not)
        assertTrue(unary.expr is Tr.Parentheses<*>)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int i = 0;
                int j = ++i;
                int k = i++;
            }
        """)

        val (prefix, postfix) = a.classes[0].fields().subList(1, 3).map { it.vars[0].initializer as Tr.Unary }
        assertEquals("++i", prefix.printTrimmed())
        assertEquals("i++", postfix.printTrimmed())
    }
}