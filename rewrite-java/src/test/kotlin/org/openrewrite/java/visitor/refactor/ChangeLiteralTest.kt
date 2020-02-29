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
package org.openrewrite.java.visitor.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J

open class ChangeLiteralTest : JavaParser() {

    val b: String = """
        package b;
        public class B {
           public void singleArg(String s) {}
        }
    """.trimIndent()

    @Test
    fun changeStringLiteralArgument() {
        val a = """
            import b.*;
            class A {
               public void test() {
                   String s = "bar";
                   new B().singleArg("foo (%s)" + s + 0L);
               }
            }
        """.trimIndent()

        val cu = parse(a, b)
        val fixed = cu.findMethodCalls("b.B singleArg(String)").changeLiterals(cu)

        assertRefactored(fixed, """
            import b.*;
            class A {
               public void test() {
                   String s = "bar";
                   new B().singleArg("foo ({})" + s + 0L);
               }
            }
        """)
    }

    @Test
    fun changeStringLiteralArgumentWithEscapableCharacters() {
        val a = """
            import b.*;
            public class A {
                B b;
                public void test() {
                    b.singleArg("mystring '%s'");
                }
            }
        """.trimIndent()

        val cu = parse(a, b)
        val fixed = cu.findMethodCalls("b.B singleArg(..)").changeLiterals(cu)

        assertRefactored(fixed, """
            import b.*;
            public class A {
                B b;
                public void test() {
                    b.singleArg("mystring '{}'");
                }
            }
        """)
    }

    private fun List<J.MethodInvocation>.changeLiterals(cu: J.CompilationUnit) = fold(cu.refactor()) { acc, meth ->
        meth.args.args
                .fold(acc, { acc2, exp ->
                    acc2.visit(ChangeLiteral(exp) { s ->
                        s?.toString()?.replace("%s", "{}") ?: s
                    })
                })
    }.fix().fixed
}
