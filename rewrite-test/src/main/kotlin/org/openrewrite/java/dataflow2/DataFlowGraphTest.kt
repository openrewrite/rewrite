/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.dataflow2.ProgramPoint.ENTRY
import org.openrewrite.java.dataflow2.ProgramPoint.EXIT
import org.openrewrite.java.tree.J

interface DataFlowGraphTest : DataFlowTest {

    fun compile(s :String, jp: JavaParser): J.CompilationUnit {
        val source = template.replace("/*__FRAGMENT__*/", s)
        return jp.parse(source)[0]
    }

    val template: String
        get() = """
            class A {
                void a(){};
                void b(){};
                void m(String u, String v, int w) {
                    a();
                    /*__FRAGMENT__*/
                    b();
                }
                void c(){}
            }
        """.trimIndent()

    @Test
    fun assertTest(jp: JavaParser) {
        val ac = "assert(w == 1)"
        val cu :J.CompilationUnit = compile("$ac;", jp)

        assertPrevious(cu, "assert(w == 1)", ENTRY, "(w == 1)")
        assertPrevious(cu, "assert(w == 1)", EXIT, "(w == 1)")

        assertPrevious(cu, "(w == 1)", ENTRY, "w == 1")
        assertPrevious(cu, "(w == 1)", EXIT, "(w == 1)")

        assertPrevious(cu, "b()", ENTRY, "(w == 1)")
        assertPrevious(cu, "b()", EXIT, "b()")
    }

    @Test
    fun assertWithDetailTest(jp: JavaParser) {
        val ac = "assert w == 1 : \"invalid\""
        val cu :J.CompilationUnit = compile("$ac;", jp)

        assertPrevious(cu, ac, ENTRY, "w == 1")
        assertPrevious(cu, ac, EXIT, "w == 1", "\"invalid\"")

        assertPrevious(cu, "\"invalid\"", ENTRY, "w == 1")
        assertPrevious(cu, "\"invalid\"", EXIT, "\"invalid\"")

        assertPrevious(cu, "w", J.Binary::class.java, ENTRY, "\"invalid\"")
        assertPrevious(cu, "w", J.Binary::class.java, EXIT, "w")

        assertPrevious(cu, "b()", ENTRY, "w == 1", "\"invalid\"")
        assertPrevious(cu, "b()", EXIT, "b()")
    }

    @Test
    fun inIfTest(jp:JavaParser) {
        val stub = "if (w > 0) { w = -1; } c();"
        val cu :J.CompilationUnit = compile(stub, jp)
        assertPrevious(cu,"if (w > 0) { w = -1; }", ENTRY, "a()")
        assertPrevious(cu,"if (w > 0) { w = -1; }", EXIT, "{ w = -1; }", "(w > 0)")

        assertPrevious(cu, "(w > 0)", ENTRY, "w > 0")
        assertPrevious(cu, "(w > 0)", EXIT, "(w > 0)")

        assertPrevious(cu, "w > 0", ENTRY, "0")
        assertPrevious(cu, "w > 0", EXIT, "w > 0")

        assertPrevious(cu, "c()", J.MethodInvocation::class.java, ENTRY, "{ w = -1; }", "(w > 0)")
        assertPrevious(cu, "c()", J.MethodInvocation::class.java, EXIT, "c()")
    }

    @Test
    fun binaryTest(jp: JavaParser) {
        val ac = """if (5 == 1) {}"""
        val cu :J.CompilationUnit = compile(ac, jp)

        assertPrevious(cu, "5 == 1", ENTRY, "1")
        assertPrevious(cu, "5 == 1", EXIT, "5 == 1")

        assertPrevious(cu, "1", ENTRY, "5")
        assertPrevious(cu, "1", EXIT, "1")

        assertPrevious(cu, "5", ENTRY, "a()")
        assertPrevious(cu, "5", EXIT, "5")
    }

    @Test
    fun arrayAccessTest(jp: JavaParser) {
        val ac = "s = str[0]"
        val cu :J.CompilationUnit = compile(ac, jp)

        assertPrevious(cu, "s = str[0]", ENTRY, "str[0]")
        assertPrevious(cu, "s = str[0]", EXIT, "s = str[0]")

        assertPrevious(cu, "str[0]", ENTRY,"0")
        assertPrevious(cu, "str[0]", EXIT,"str[0]")

        assertPrevious(cu, "0", ENTRY, "str")
        assertPrevious(cu, "0", EXIT, "0")

        assertPrevious(cu, "str", ENTRY,"a()")
        assertPrevious(cu, "str", EXIT,"str")
    }

    @Test
    fun methodInvocationTest(jp: JavaParser) {
        val ac = "abc = myMethod(a1, b1)"
        val cu: J.CompilationUnit = compile(ac, jp)

        assertPrevious(cu, ac, ENTRY, "myMethod(a1, b1)")
        assertPrevious(cu, ac, EXIT, ac)

        assertPrevious(cu, "myMethod(a1, b1)", ENTRY, "b1")
        assertPrevious(cu, "myMethod(a1, b1)", EXIT, "myMethod(a1, b1)")

        assertPrevious(cu, "b1", ENTRY, "a1")
        assertPrevious(cu, "b1", EXIT, "b1")

        assertPrevious(cu, "a1", ENTRY, "b1")
        assertPrevious(cu, "a1", EXIT, "a1")

        assertPrevious(cu, "myMethod", ENTRY, "a()")
        assertPrevious(cu, "myMethod", EXIT, "myMethod")
    }

    @Test
    fun basicDataFlowTest(jp: JavaParser) {
        val source = """
            class C {
                void a() {}
                void b() {}
                void m() {
                    a();
                    int i = u + v, j = w;
                    b();
                }
            }
        """.trimIndent()
        val cu :J.CompilationUnit = jp.parse(source)[0]
        assertThat(cu.printAll()).isEqualTo(source)
        assertPrevious(cu, "b()", ENTRY, "j = w")
        assertPrevious(cu,"j = w", EXIT, "j = w")
        assertPrevious(cu,"j = w", ENTRY, "w")
        assertPrevious(cu,"w", EXIT,"w")
        assertPrevious(cu,"w", ENTRY,"i = u + v")
        assertPrevious(cu,"i = u + v", EXIT, "i = u + v")
        assertPrevious(cu,"i = u + v", ENTRY, "u + v")
        assertPrevious(cu,"u + v", EXIT, "u + v")
        assertPrevious(cu,"u + v", ENTRY, "v")
        assertPrevious(cu,"v", EXIT, "v")
        assertPrevious(cu,"v", ENTRY, "u")
        assertPrevious(cu,"u", EXIT, "u")
        assertPrevious(cu,"u", ENTRY, "a()")
    }

}