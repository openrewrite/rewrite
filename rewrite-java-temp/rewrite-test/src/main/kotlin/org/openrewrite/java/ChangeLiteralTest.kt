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
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.tree.J

interface ChangeLiteralTest: RefactorVisitorTest {
    companion object {
        private val b: String = """
            package b;
            public class B {
               public void singleArg(String s) {}
            }
        """.trimIndent()
    }

    @Test
    fun changeStringLiteralArgument(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(b),
            visitorsMappedToMany = listOf { cu ->
                cu.findMethodCalls("b.B singleArg(String)").changeLiterals()
            },
            before = """
                import b.*;
                class A {
                   public void test() {
                       String s = "bar";
                       new B().singleArg("foo (%s)" + s + 0L);
                   }
                }
            """,
            after = """
                import b.*;
                class A {
                   public void test() {
                       String s = "bar";
                       new B().singleArg("foo ({})" + s + 0L);
                   }
                }
            """
    )

    @Test
    fun changeStringLiteralArgumentWithEscapableCharacters(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(b),
            visitorsMappedToMany = listOf { cu ->
                cu.findMethodCalls("b.B singleArg(..)").changeLiterals()
            },
            before = """
                import b.*;
                public class A {
                    B b;
                    public void test() {
                        b.singleArg("mystring '%s'");
                    }
                }
            """,
            after = """
                import b.*;
                public class A {
                    B b;
                    public void test() {
                        b.singleArg("mystring '{}'");
                    }
                }
            """
    )

    private fun List<J.MethodInvocation>.changeLiterals() = flatMap { meth ->
        meth.args.args.map { exp ->
            ChangeLiteral.Scoped(exp) { s ->
                s?.toString()?.replace("%s", "{}") ?: s
            }
        }
    }
}
