/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test
import org.openrewrite.Issue

class MethodInvocationTest : GroovyTreeTest {

    @Test
    fun gradle() = assertParsePrintAndProcess(
        """
            plugins {
                id 'java-library'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.hibernate:hibernate-core:3.6.7.Final'
                api 'com.google.guava:guava:23.0'
                testImplementation 'junit:junit:4.+'
            }
        """.trimIndent()
    )

    @Test
    fun emptyArgsWithParens() = assertParsePrintAndProcess(
        """
            mavenCentral()
        """.trimIndent()
    )

    @Test
    fun nullSafeDereference() = assertParsePrintAndProcess(
        """
            Map m
            m?.clear()
        """
    )

    @Test
    fun mapLiteralFirstArgument() = assertParsePrintAndProcess(
        """
            foo(["foo" : "bar"])
        """
    )

    @Test
    fun namedArgumentsInDeclaredOrder() = assertParsePrintAndProcess("""
        def acceptsNamedArguments (Map a, int i) { }
        acceptsNamedArguments(foo: "bar", 1)
    """)

    @Test
    fun namedArgumentsAfterPositionalArguments() = assertParsePrintAndProcess("""
        def acceptsNamedArguments (Map a, int i) { }
        acceptsNamedArguments(1, foo: "bar")
    """)

    @Test
    fun namedArgumentBeforeClosure() = assertParsePrintAndProcess("""
        def acceptsNamedArguments(Map a, int i, Closure c) {}
        acceptsNamedArguments(1, foo: "bar") { }
    """)

    @Test
    fun namedArgumentsBeforeClosure() = assertParsePrintAndProcess("""
        def acceptsNamedArguments(Map a, Closure c) {}
        acceptsNamedArguments(foo: "bar", bar: "baz") { }
    """)

    @Test
    fun namedArgumentsBetweenPositionalArguments() = assertParsePrintAndProcess("""
        def acceptsNamedArguments(Map a, int n, int m) { }
        acceptsNamedArguments(1, foo: "bar", 2)
    """)

    @Test
    fun namedArgumentsAllOverThePlace() = assertParsePrintAndProcess("""
        def acceptsNamedArguments(Map a, int n, int m) { }
        acceptsNamedArguments(1, foo: "bar", 2, bar: "baz")
    """.trimIndent())


    @Test
    fun closureWithImplicitParameter() = assertParsePrintAndProcess("""
        def acceptsClosure(Closure cl) {}
        acceptsClosure {
            println(it)
        }
    """)

    @Issue("https://github.com/openrewrite/rewrite/issues/1236")
    @Test
    fun closureWithNamedParameter() = assertParsePrintAndProcess("""
        def acceptsClosure(Closure cl) {}
        acceptsClosure { foo ->
            println(foo)
        }
    """)

    @Test
    fun closureWithNamedParameterAndType() = assertParsePrintAndProcess("""
        def acceptsClosure(Closure cl) {}
        acceptsClosure { String foo ->
            println(foo)
        }
    """)

    @Test
    fun closureReturn() = assertParsePrintAndProcess("""
        foo {
            return
        }
    """)
}
