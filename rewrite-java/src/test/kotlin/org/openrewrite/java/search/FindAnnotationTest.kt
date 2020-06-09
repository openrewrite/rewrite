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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaSourceVisitor
import org.openrewrite.java.tree.J

interface FindAnnotationTest {
    companion object {
        const val foo = """
            package com.netflix.foo;
            public @interface Foo {
                String bar();
                String baz();
            }
        """
    }

    @Test
    fun matchesSimpleFullyQualifiedAnnotation(jp: JavaParser) {
        val a = jp.parse("""
            @Deprecated
            public class A {}
        """)

        assertTrue(a.classes[0].findAnnotations("@java.lang.Deprecated").isNotEmpty())
    }

    @Test
    fun matchesAnnotationOnMethod(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                @Deprecated
                public void foo() {}
            }
        """)

        val deprecated = a.classes[0].methods[0].findAnnotations("@java.lang.Deprecated").firstOrNull()
        assertNotNull(deprecated)

        assertThat(object : JavaSourceVisitor<String?>() {
            init {
                setCursoringOn()
            }

            override fun defaultTo(t: Tree?): String? = null

            override fun visitAnnotation(annotation: J.Annotation?): String? {
                return if (annotation === deprecated) enclosingMethod()?.simpleName else super.visitAnnotation(annotation)
            }
        }.visit(a)).isEqualTo("foo")
    }

    @Test
    fun matchesAnnotationOnField(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                @Deprecated String s;
            }
        """)

        assertTrue(a.classes[0].fields[0].findAnnotations("@java.lang.Deprecated").isNotEmpty())
    }

    @Test
    fun doesNotMatchNotFullyQualifiedAnnotations(jp: JavaParser) {
        val a = jp.parse("""
            @Deprecated
            public class A {}
        """)

        assertTrue(a.classes[0].findAnnotations("@Deprecated").isEmpty())
    }

    @Test
    fun matchesSingleAnnotationParameter(jp: JavaParser) {
        val a = jp.parse("""
            @SuppressWarnings("deprecation")
            public class A {}
        """)

        assertTrue(a.classes[0].findAnnotations("""@java.lang.SuppressWarnings("deprecation")""").isNotEmpty())
        assertTrue(a.classes[0].findAnnotations("""@java.lang.SuppressWarnings("foo")""").isEmpty())
    }

    @Test
    fun matchesNamedParameters(jp: JavaParser) {
        val a = jp.parse("""
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """, foo)

        assertTrue(a.classes[0].findAnnotations("""@com.netflix.foo.Foo(bar="quux",baz="bar")""").isNotEmpty())
        assertTrue(a.classes[0].findAnnotations("""@com.netflix.foo.Foo(bar="qux",baz="bar")""").isEmpty())
    }

    @Test
    fun matchesNamedParametersRegardlessOfOrder(jp: JavaParser) {
        val a = jp.parse("""
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """, foo)

        assertTrue(a.classes[0].findAnnotations("""@com.netflix.foo.Foo(baz="bar",bar="quux")""").isNotEmpty())
    }
}
