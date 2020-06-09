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

interface LambdaTest {

    @Test
    fun lambda(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.function.Function;
            public class A {
                Function<String, String> func = (String s) -> "";
            }
        """)

        val lambda = a.classes[0].fields[0].vars[0].initializer as J.Lambda

        assertEquals(1, lambda.paramSet.params.size)
        assertTrue(lambda.body is J.Literal)
        assertEquals("(String s) -> \"\"", lambda.printTrimmed())
    }

    @Test
    fun untypedLambdaParameter(jp: JavaParser) {
        val a = jp.parse("""
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
    fun optionalSingleParameterParentheses(jp: JavaParser) {
        val a = jp.parse("""
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
    fun rightSideBlock(jp: JavaParser) {
        val a = jp.parse("""
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
    fun multipleParameters(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.function.BiConsumer;
            public class A {
                BiConsumer<String, String> a = (s1, s2) -> { };
            }
        """)

        val lambda = a.classes[0].fields[0].vars[0].initializer!!
        assertEquals("(s1, s2) -> { }", lambda.printTrimmed())
    }
}
