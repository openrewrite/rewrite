package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

open class InsertMethodArgumentTest : JavaParser() {

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

    @Test
    fun refactorInsertArgument() {
        val a = """
            class A {
               public void test() {
                   B b = new B();
                   b.foo();
                   b.foo("1");
               }
            }
        """.trimIndent()

        val cu = parse(a, b)
        val oneParamFoos = cu.findMethodCalls("B foo(String)")

        val fixed = cu.refactor()
                .fold(oneParamFoos) { InsertMethodArgument(it, 0, "\"0\"") }
                .fold(oneParamFoos) { InsertMethodArgument(it, 2, "\"2\"") }
                .fold(cu.findMethodCalls("B foo()")) { InsertMethodArgument(it, 0, "\"0\"") }
                .fix().fixed

        // FIXME re-add this compatibility test once reordering is implemented
//          .findMethodCalls("B bar(String, String)")
//              .changeArguments()
//                  .reorderByArgName("s2", "s") // compatibility of reordering and insertion
//                  .insert(0, "\"0\"")

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
