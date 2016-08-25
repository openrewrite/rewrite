package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Deprecated

class FindAnnotationsTest: AbstractRefactorTest() {

    @Test
    fun findAnnotation() {
        val a = java("""
            @Deprecated
            public class A {
            }
        """)

        val results = parseJava(a).findAnnotations(Deprecated::class.java)
        assertEquals("java.lang.Deprecated", results.first().name)
    }

    @Test
    fun findUnresolvableAnnotation() {
        val a = java("""
            @Foo
            public class A {
            }
        """)

        var results = parseJava(a).findAnnotations("Foo")
        assertEquals("Foo", results.first().name)
    }

    @Test
    fun findAnnotationWithParameters() {
        val a = java("""
            @Foo(
                bar="Baz",
                quux=1.1,
                foo={"bar", "baz"}
            )
            public class A {
            }
        """)

        var results = parseJava(a)
            .findAnnotations("Foo")
        assertEquals("Foo", results.first().name)
    }

    @Test
    fun findAnnotationWithSingleUnnamedParameter() {
        val a = java("""
            @Foo("bar")
            public class A {
            }
        """)

        var results = parseJava(a).findAnnotations("Foo")
        assertEquals("Foo", results.first().name)
    }

}
