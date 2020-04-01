package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class LambdaTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            import java.util.function.Function;
            public class A {
                Function<String, String> func = (String s) -> "";
            }
        """)
    }

    private val lambda by lazy { a.classes[0].fields[0].vars[0].initializer as J.Lambda }

    @Test
    fun lambda() {
        assertEquals(1, lambda.paramSet.params.size)
        assertTrue(lambda.body is J.Literal)
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