package com.netflix.java.refactor.ast

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@Ignore
class AnnotationMatcherTest : AbstractRefactorTest() {
    @Test
    fun matchesSimpleFullyQualifiedAnnotation() {
        val a = java("""
            @Deprecated
            public class A {}
        """)
        
        assertTrue(parseJava(a)
                .findAnnotations("@java.lang.Deprecated")
                .isNotEmpty())
    }

    @Test
    fun doesNotMatchNotFullyQualifiedAnnotations() {
        assertFalse(
            parseJava(
                java(
                    """
                    @Deprecated
                    public class A {}
                    """
                )
            )
                .findAnnotations("@Deprecated")
                .isEmpty()
        )
    }

    @Test
    fun matchesSingleAnnotationParameter() {
        val source = parseJava(
            java(
                """
                @SuppressWarnings("deprecation")
                public class A {}
                """

            )
        )

        assertFalse(
            source
                .findAnnotations(
                    """@java.lang.SuppressWarnings("deprecation")"""
                )
                .isEmpty()
        )

        assertTrue(
            source
                .findAnnotations(
                    """@java.lang.SuppressWarnings("foo")"""

                )
                .isEmpty()
        )
    }

    @Test
    fun matchesNamedParameters() {
        val source = parseJava(
            java(
                """
                package com.netflix.foo

                @interface Foo {
                    String bar();
                    String baz();
                }

                @Foo(bar="quux", baz="bar")
                public class A {}
                """
            )
        )

        assertFalse(
            source
                .findAnnotations(
                    """@com.netflix.foo.Foo(bar="quux",baz="bar")"""
                )
                .isEmpty()
        )

        assertTrue(
            source
                .findAnnotations(
                    """@com.netflix.foo.Foo(bar="qux",baz="bar")"""
                )
                .isEmpty()
        )
    }

    @Test
    fun matchesNamedParametersRegardlessOfOrder() {
        val source = parseJava(
            java(
                """
                package com.netflix.foo

                @interface Foo {
                    String bar();
                    String baz();
                }

                @Foo(bar="quux", baz="bar")
                public class A {}
                """
            )
        )

        assertFalse(
            source
                .findAnnotations(
                    """@com.netflix.foo.Foo(baz="bar",bar="quux")"""
                )
                .isEmpty()
        )
    }
}
