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

interface ChangeMethodNameTest {
    companion object {
        private val b: String = """
                class B {
                   public void singleArg(String s) {}
                   public void arrArg(String[] s) {}
                   public void varargArg(String... s) {}
                }
            """.trimIndent()
    }

    @Test
    fun changeMethodNameForMethodWithSingleArgDeclarative(jp: JavaParser) {
        val a = """
            class A {
               public void test() {
                   new B().singleArg("boo");
               }
            }
        """.trimIndent()

        val cu = jp.parse(a, b)

        val fixed = cu.refactor()
                .visit(ChangeMethodName().apply { setMethod("B singleArg(String)"); setName("bar") })
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar("boo");
               }
            }
        """)
    }

    @Test
    fun changeMethodNameForMethodWithSingleArg(jp: JavaParser) {
        val a = """
            class A {
               public void test() {
                   new B().singleArg("boo");
               }
            }
        """.trimIndent()

        val cu = jp.parse(a, b)

        val fixed = cu.refactor()
                .visit(ChangeMethodName().apply {
                    setMethod("B singleArg(String)")
                    name = "bar"
                })
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar("boo");
               }
            }
        """)
    }

    @Test
    fun changeMethodNameForMethodWithArrayArg(jp: JavaParser) {
        val a = """
            class A {
               public void test() {
                   new B().arrArg(new String[] {"boo"});
               }
            }
        """.trimIndent()

        val cu = jp.parse(a, b)

        val fixed = cu.refactor()
                .visit(ChangeMethodName().apply {
                    setMethod("B arrArg(String[])")
                    name = "bar"
                })
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar(new String[] {"boo"});
               }
            }
        """)
    }

    @Test
    fun changeMethodNameForMethodWithVarargArg(jp: JavaParser) {
        val a = """
            class A {
               public void test() {
                   new B().varargArg("boo", "again");
               }
            }
        """.trimIndent()

        val cu = jp.parse(a, b)

        val fixed = cu.refactor()
                .visit(ChangeMethodName().apply {
                    setMethod("B varargArg(String...)")
                    name = "bar"
                })
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar("boo", "again");
               }
            }
        """)
    }

    @Test
    fun changeMethodNameWhenMatchingAgainstMethodWithNameThatIsAnAspectjToken(jp: JavaParser) {
        val b = """
            class B {
               public void error() {}
               public void foo() {}
            }
        """.trimIndent()

        val a = """
            class A {
               public void test() {
                   new B().error();
               }
            }
        """.trimIndent()

        val cu = jp.parse(a, b)
        val fixed = cu.refactor()
                .visit(ChangeMethodName().apply {
                    setMethod("B error()")
                    name = "foo"
                })
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().foo();
               }
            }
        """)
    }

    @Test
    fun changeMethodDeclarationForMethodWithSingleArg(jp: JavaParser) {
        val a = """
            class A {
               public void foo(String s) {
               }
            }
        """.trimIndent()

        val cu = jp.parse(a, b)

        val fixed = cu.refactor()
                .visit(ChangeMethodName().apply {
                    setMethod("A foo(String)")
                    name = "bar"
                })
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void bar(String s) {
               }
            }
        """)
    }
}
