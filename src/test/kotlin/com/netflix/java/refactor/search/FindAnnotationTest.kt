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
package com.netflix.java.refactor.search

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory

abstract class FindAnnotationTest(p: Parser): Parser by p {

    val foo = """
        package com.netflix.foo;
        public @interface Foo {
            String bar();
            String baz();
        }
    """

    @Before
    fun before() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.level = Level.DEBUG
    }

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

        val annotation = a.classes[0].methods()[0].findAnnotations("@java.lang.Deprecated").firstOrNull()
        assertNotNull(annotation)
        assertEquals("foo", a.cursor(annotation)?.enclosingMethod()?.simpleName)
    }

    @Test
    fun matchesAnnotationOnField() {
        val a = parse("""
            public class A {
                @Deprecated String s;
            }
        """)

        assertTrue(a.classes[0].fields()[0].findAnnotations("@java.lang.Deprecated").isNotEmpty())
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

class OracleFindAnnotationTest: FindAnnotationTest(OracleJdkParser())