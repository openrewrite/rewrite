package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaSourceVisitor
import org.openrewrite.java.tree.J

open class FindAnnotationTest : JavaParser() {

    val foo = """
        package com.netflix.foo;
        public @interface Foo {
            String bar();
            String baz();
        }
    """

    @Test
    fun matchesSimpleFullyQualifiedAnnotation() {
        val a = parse("""
            @Deprecated
            public class A {}
        """)

        assertTrue(a.classes[0].findAnnotations("@java.lang.Deprecated").isNotEmpty())
    }

    @Test
    fun matchesAnnotationOnMethod() {
        val a = parse("""
            public class A {
                @Deprecated
                public void foo() {}
            }
        """)

        val deprecated = a.classes[0].methods[0].findAnnotations("@java.lang.Deprecated").firstOrNull()
        assertNotNull(deprecated)

        assertThat(object : JavaSourceVisitor<String?>() {
            override fun defaultTo(t: Tree?): String? = null
            override fun isCursored(): Boolean = true

            override fun visitAnnotation(annotation: J.Annotation?): String? {
                return if (annotation === deprecated) enclosingMethod()?.simpleName else super.visitAnnotation(annotation)
            }
        }.visit(a)).isEqualTo("foo")
    }

    @Test
    fun matchesAnnotationOnField() {
        val a = parse("""
            public class A {
                @Deprecated String s;
            }
        """)

        assertTrue(a.classes[0].fields[0].findAnnotations("@java.lang.Deprecated").isNotEmpty())
    }

    @Test
    fun doesNotMatchNotFullyQualifiedAnnotations() {
        val a = parse("""
            @Deprecated
            public class A {}
        """)

        assertTrue(a.classes[0].findAnnotations("@Deprecated").isEmpty())
    }

    @Test
    fun matchesSingleAnnotationParameter() {
        val a = parse("""
            @SuppressWarnings("deprecation")
            public class A {}
        """)

        assertTrue(a.classes[0].findAnnotations("""@java.lang.SuppressWarnings("deprecation")""").isNotEmpty())
        assertTrue(a.classes[0].findAnnotations("""@java.lang.SuppressWarnings("foo")""").isEmpty())
    }

    @Test
    fun matchesNamedParameters() {
        val a = parse("""
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """, foo)

        assertTrue(a.classes[0].findAnnotations("""@com.netflix.foo.Foo(bar="quux",baz="bar")""").isNotEmpty())
        assertTrue(a.classes[0].findAnnotations("""@com.netflix.foo.Foo(bar="qux",baz="bar")""").isEmpty())
    }

    @Test
    fun matchesNamedParametersRegardlessOfOrder() {
        val a = parse("""
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """, foo)

        assertTrue(a.classes[0].findAnnotations("""@com.netflix.foo.Foo(baz="bar",bar="quux")""").isNotEmpty())
    }
}
