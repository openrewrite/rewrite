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
package org.openrewrite.visitor.refactor.op

import org.openrewrite.assertRefactored
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.JavaParser
import org.openrewrite.tree.Flag
import org.openrewrite.tree.J

open class ChangeMethodTargetToStaticTest : JavaParser() {

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
                .fold(targets) { ChangeMethodTargetToStatic(it, "b.B") }
                .fold(targets) { ChangeMethodName(it, "foo") }
                .fix().fixed

        assertRefactored(fixed, """
            import b.B;
            
            class C {
               public void test() {
                   B.foo();
               }
            }
        """)

        val refactoredInv = fixed.classes[0].methods[0].body!!.statements[0] as J.MethodInvocation
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
                .fold(cu.findMethodCalls("a.A foo()")) { ChangeMethodTargetToStatic(it, "b.B") }
                .fix().fixed

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
