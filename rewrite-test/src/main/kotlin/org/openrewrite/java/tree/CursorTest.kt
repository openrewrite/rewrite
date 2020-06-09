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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.RetrieveCursor
import org.openrewrite.java.JavaSourceVisitor

interface CursorTest {
    @Test
    fun inSameNameScope(jp: JavaParser) {
        val a = jp.parse("""
            public class A extends B {
                int n;
                
                public void foo(int n1) {
                    for(int n2 = 0;;) {
                    }
                }
                
                static class B {
                    public void foo(int n) {
                    }
                }
                
                interface C {
                    void foo(int n);
                }
                
                enum D {
                    D1, D2;
                    void foo(int n) {}
                }
                
                class E {
                    void foo(int n) {}
                }
            }
        """.trimIndent())

        fun Tree.cursor() = RetrieveCursor(this).visit(a)

        val fieldScope = a.classes[0].fields[0].cursor()!!
        val methodParamScope = a.classes[0].methods[0].params.params[0].cursor()!!
        val forInitScope = a.classes[0].methods[0].body!!.statements.filterIsInstance<J.ForLoop>()[0].control.init.cursor()!!

        assertThat(object : JavaSourceVisitor<Int>() {
            init {
                setCursoringOn()
            }

            override fun defaultTo(t: Tree?): Int = 0

            override fun visitCompilationUnit(cu: J.CompilationUnit?): Int {
                assertTrue(isInSameNameScope(fieldScope, methodParamScope))
                assertFalse(isInSameNameScope(methodParamScope, fieldScope))
                assertTrue(isInSameNameScope(fieldScope, forInitScope))

                val innerClasses = a.classes[0].body.statements.filterIsInstance<J.ClassDecl>()
                assertEquals(4, innerClasses.size)

                innerClasses.forEachIndexed { n, innerClass ->
                    val innerStaticClassMethodParam = innerClass.methods[0].params.params[0].cursor()!!
                    assertEquals(n >= 3, isInSameNameScope(fieldScope, innerStaticClassMethodParam))
                }

                return 1
            }
        }.visit(a)).isEqualTo(1)
    }
}