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
import org.openrewrite.test.RewriteTest

class MethodInvocationTest : RewriteTest {

    @Test
    fun gradle() = rewriteRun(
        groovy(
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
            """
        )
    )

    @Test
    fun emptyArgsWithParens() = rewriteRun(
        groovy("mavenCentral()")
    )

    @Test
    fun nullSafeDereference() = rewriteRun(
        groovy(
            """
            Map m = [:]
            m?.clear()
        """
        )
    )

    @Test
    fun mapLiteralFirstArgument() = rewriteRun(
        groovy(
            """
                foo(["foo" : "bar"])
            """
        )
    )

    @Test
    fun namedArgumentsInDeclaredOrder() = rewriteRun(
        groovy(
            """
                def acceptsNamedArguments (Map a, int i) { }
                acceptsNamedArguments(foo: "bar", 1)
            """
        )
    )

    @Test
    fun namedArgumentsAfterPositionalArguments() = rewriteRun(
        groovy(
            """
                def acceptsNamedArguments (Map a, int i) { }
                acceptsNamedArguments(1, foo: "bar")
            """
        )
    )

    @Test
    fun namedArgumentBeforeClosure() = rewriteRun(
        groovy(
            """
                def acceptsNamedArguments(Map a, int i, Closure c) {}
                acceptsNamedArguments(1, foo: "bar") { }
            """
        )
    )

    @Test
    fun namedArgumentsBeforeClosure() = rewriteRun(
        groovy(
            """
                def acceptsNamedArguments(Map a, Closure c) {}
                acceptsNamedArguments(foo: "bar", bar: "baz") { }
            """
        )
    )

    @Test
    fun namedArgumentsBetweenPositionalArguments() = rewriteRun(
        groovy(
            """
                def acceptsNamedArguments(Map a, int n, int m) { }
                acceptsNamedArguments(1, foo: "bar", 2)
            """
        )
    )

    @Test
    fun namedArgumentsAllOverThePlace() = rewriteRun(
        groovy(
            """
                def acceptsNamedArguments(Map a, int n, int m) { }
                acceptsNamedArguments(1, foo: "bar", 2, bar: "baz")
            """
        )
    )


    @Test
    fun closureWithImplicitParameter() = rewriteRun(
        groovy(
            """
                def acceptsClosure(Closure cl) {}
                acceptsClosure {
                    println(it)
                }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1236")
    @Test
    fun closureWithNamedParameter() = rewriteRun(
        groovy(
            """
                def acceptsClosure(Closure cl) {}
                acceptsClosure { foo ->
                    println(foo)
                }
            """
        )
    )

    @Test
    fun closureWithNamedParameterAndType() = rewriteRun(
        groovy(
            """
                def acceptsClosure(Closure cl) {}
                acceptsClosure { String foo ->
                    println(foo)
                }
            """
        )
    )

    @Test
    fun closureArgumentInParens() = rewriteRun(
        groovy(
            """
                def acceptsClosure(Closure cl) {}
                acceptsClosure({})
            """
        )
    )

    @Test
    fun closureArgumentAfterEmptyParens() = rewriteRun(
        groovy(
            """
                def acceptsClosure(Closure cl) {}
                acceptsClosure ( /* () */ ) { /* {} */ }
            """
        )
    )

    @Test
    fun closureReturn() = rewriteRun(
        groovy(
            """
                foo {
                    return
                }
            """
        )
    )
}
