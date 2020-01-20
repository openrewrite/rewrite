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

import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class LambdaTest(p: Parser): Parser by p {

    val a: Tr.CompilationUnit by lazy {
        parse("""
            import java.util.function.Function;
            public class A {
                Function<String, String> func = (String s) -> "";
            }
        """)
    }

    private val lambda by lazy { a.classes[0].fields[0].vars[0].initializer as Tr.Lambda }

    @Test
    fun lambda() {
        assertEquals(1, lambda.paramSet.params.size)
        assertTrue(lambda.body is Tr.Literal)
    }

    @Test
    fun format() {
        assertEquals("(String s) -> \"\"", lambda.printTrimmed())
    }

    @Test
    fun untypedLambdaParameter() {
        val a = parse("""
            import java.util.*;
            public class A {
                List<String> list = new ArrayList<>();
                public void test() {
                    list.stream().filter(s -> s.isEmpty());
                }
            }
        """)

        assertEquals("list.stream().filter(s -> s.isEmpty())",
                a.classes[0].methods[0].body!!.statements[0].printTrimmed())
    }

    @Test
    fun optionalSingleParameterParentheses() {
        val a = parse("""
            import java.util.*;
            public class A {
                List<String> list = new ArrayList<>();
                public void test() {
                    list.stream().filter((s) -> s.isEmpty());
                }
            }
        """)

        assertEquals("list.stream().filter((s) -> s.isEmpty())",
                a.classes[0].methods[0].body!!.statements[0].printTrimmed())
    }

    @Test
    fun rightSideBlock() {
        val a = parse("""
            public class A {
                Action a = ( ) -> { };
            }

            interface Action {
                void call();
            }
        """)

        val lambda = a.classes[0].fields[0].vars[0].initializer!!
        assertEquals("( ) -> { }", lambda.printTrimmed())
    }

    @Test
    fun multipleParameters() {
        val a = parse("""
            import java.util.function.BiConsumer;
            public class A {
                BiConsumer<String, String> a = (s1, s2) -> { };
            }
        """)

        val lambda = a.classes[0].fields[0].vars[0].initializer!!
        assertEquals("(s1, s2) -> { }", lambda.printTrimmed())
    }
}