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
package org.openrewrite.java

import org.junit.jupiter.api.Test

interface InsertMethodArgumentTest {
    companion object {
        val b = """
            class B {
               public void foo() {}
               public void foo(String s) {}
               public void foo(String s1, String s2) {}
               public void foo(String s1, String s2, String s3) {}
            
               public void bar(String s, String s2) {}
               public void bar(String s, String s2, String s3) {}
            }
        """.trimIndent()

        val a = """
            class A {
               public void test() {
                   B b = new B();
                   b.foo();
                   b.foo("1");
               }
            }
        """.trimIndent()
    }

    @Test
    fun insertArgumentDeclarative(jp: JavaParser) {
        val cu = jp.parse(a, b)

        val fixed = cu.refactor().visit(InsertMethodArgument().apply {
            setMethod("B foo(String)")
            setIndex(0)
            setSource("\"0\"")
        }).fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   B b = new B();
                   b.foo();
                   b.foo("0", "1");
               }
            }
        """)
    }

    @Test
    fun insertArgument(jp: JavaParser) {
        val cu = jp.parse(a, b)
        val oneParamFoos = cu.findMethodCalls("B foo(String)")

        val fixed = cu.refactor()
                .fold(oneParamFoos) { InsertMethodArgument.Scoped(it, 0, "\"0\"") }
                .fold(oneParamFoos) { InsertMethodArgument.Scoped(it, 2, "\"2\"") }
                .fold(cu.findMethodCalls("B foo()")) { InsertMethodArgument.Scoped(it, 0, "\"0\"") }
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   B b = new B();
                   b.foo("0");
                   b.foo("0", "1", "2");
               }
            }
        """)
    }
}
