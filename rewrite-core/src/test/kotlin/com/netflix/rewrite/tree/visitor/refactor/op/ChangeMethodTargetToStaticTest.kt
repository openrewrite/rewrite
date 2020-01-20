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
package com.netflix.rewrite.tree.visitor.refactor.op

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.Flag
import com.netflix.rewrite.tree.Tr
import org.junit.Assert.assertTrue
import org.junit.Test

open class ChangeMethodTargetToStaticTest : Parser by OpenJdkParser() {

    @Test
    fun refactorTargetToStatic() {
        val a = """
            package a;
            public class A {
               public void nonStatic() {}
            }
        """.trimIndent()

        val b = """
            package b;
            public class B {
               public static void foo() {}
            }
        """.trimIndent()

        val c = """
            import a.*;
            class C {
               public void test() {
                   new A().nonStatic();
               }
            }
        """.trimIndent()

        val cu = parse(c, a, b)
        val targets = cu.findMethodCalls("a.A nonStatic()")
        val fixed = cu.refactor()
            .changeMethodTargetToStatic(targets, "b.B")
            .changeMethodName(targets, "foo")
            .fix()

        assertRefactored(fixed, """
            import b.B;
            
            class C {
               public void test() {
                   B.foo();
               }
            }
        """)

        val refactoredInv = fixed.classes[0].methods[0].body!!.statements[0] as Tr.MethodInvocation
        assertTrue(refactoredInv.type?.hasFlags(Flag.Static) ?: false)
    }

    @Test
    fun refactorStaticTargetToStatic() {
        val a = """
            package a;
            public class A {
               public static void foo() {}
            }
        """.trimIndent()

        val b = """
            package b;
            public class B {
               public static void foo() {}
            }
        """.trimIndent()

        val c = """
            import static a.A.*;
            class C {
               public void test() {
                   foo();
               }
            }
        """.trimIndent()

        val cu = parse(c, a, b)
        val fixed = cu.refactor()
            .changeMethodTargetToStatic(cu.findMethodCalls("a.A foo()"), "b.B")
            .fix()

        assertRefactored(fixed, """
            import b.B;
            
            class C {
               public void test() {
                   B.foo();
               }
            }
        """)
    }
}
