/*
 * Copyright 2020 the original authors.
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
package com.netflix.rewrite.tree

import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.*
import org.junit.Test

open class CursorTest : Parser by OpenJdkParser() {
    @Test
    fun inSameNameScope() {
        val a = parse("""
            public class A {
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

        fun Tree.cursor() = a.cursor(this)

        val fieldScope = a.classes[0].fields[0].cursor()!!
        val methodParamScope = a.classes[0].methods[0].params.params[0].cursor()!!
        val forInitScope = a.classes[0].methods[0].body!!.statements.filterIsInstance<Tr.ForLoop>()[0].control.init.cursor()!!

        assertTrue(fieldScope.isInSameNameScope(methodParamScope))
        assertFalse(methodParamScope.isInSameNameScope(fieldScope))

        assertTrue(fieldScope.isInSameNameScope(forInitScope))

        val innerClasses = a.classes[0].body.statements.filterIsInstance<Tr.ClassDecl>()
        assertEquals(4, innerClasses.size)

        innerClasses.forEachIndexed { n, innerClass ->
            val innerStaticClassMethodParam = innerClass.methods[0].params.params[0].cursor()!!
            assertEquals(n >= 3, fieldScope.isInSameNameScope(innerStaticClassMethodParam))
        }
    }
}