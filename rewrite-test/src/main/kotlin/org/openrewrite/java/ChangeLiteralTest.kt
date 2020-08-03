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
import org.openrewrite.java.tree.J
import org.openrewrite.whenParsedBy

interface ChangeLiteralTest {
    companion object {
        private val b: String = """
            package b;
            public class B {
               public void singleArg(String s) {}
            }
        """.trimIndent()
    }

    @Test
    fun changeStringLiteralArgument(jp: JavaParser) {
        val a = """
            import b.*;
            class A {
               public void test() {
                   String s = "bar";
                   new B().singleArg("foo (%s)" + s + 0L);
               }
            }
        """.trimIndent()

        a.whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedByMany { cu -> cu.findMethodCalls("b.B singleArg(String)").changeLiterals() }
                .isRefactoredTo("""
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
    fun changeStringLiteralArgumentWithEscapableCharacters(jp: JavaParser) {
        """
            import b.*;
            public class A {
                B b;
                public void test() {
                    b.singleArg("mystring '%s'");
                }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedByMany { cu -> cu.findMethodCalls("b.B singleArg(..)").changeLiterals() }
                .isRefactoredTo("""
                    import b.*;
                    public class A {
                        B b;
                        public void test() {
                            b.singleArg("mystring '{}'");
                        }
                    }
                """)
    }

    private fun List<J.MethodInvocation>.changeLiterals() = flatMap { meth ->
        meth.args.args.map { exp ->
            ChangeLiteral(exp) { s ->
                s?.toString()?.replace("%s", "{}") ?: s
            }
        }
    }
}
