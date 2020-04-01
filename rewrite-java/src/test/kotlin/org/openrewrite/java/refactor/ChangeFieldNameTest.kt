package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.JavaType

open class ChangeFieldNameTest : JavaParser() {

    @Test
    fun changeFieldName() {
        val a = parse("""
            import java.util.List;
            public class A {
               List collection = null;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .visit(ChangeFieldName(a.classes[0].type.asClass(), "collection", "list"))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            public class A {
               List list = null;
            }
        """)
    }

    @Test
    fun changeFieldNameReferences() {
        val b = parse("""
            public class B {
               int n;
               
               {
                   n = 1;
                   n /= 2;
                   if(n + 1 == 2) {}
                   n++;
               }
               
               public int foo(int n) {
                   return n + this.n;
               }
            }
        """.trimIndent())

        val fixed = b.refactor()
                .visit(ChangeFieldName(JavaType.Class.build("B"), "n", "n1"))
                .fix().fixed

        assertRefactored(fixed, """
            public class B {
               int n1;
               
               {
                   n1 = 1;
                   n1 /= 2;
                   if(n1 + 1 == 2) {}
                   n1++;
               }
               
               public int foo(int n) {
                   return n + this.n1;
               }
            }
        """)
    }

    @Test
    fun changeFieldNameReferencesInOtherClass() {
        val b = """
            public class B {
               int n;
            }
        """.trimIndent()

        val a = parse("""
            public class A {
                B b = new B();
                {
                    b.n = 1;
                }
            }
        """.trimIndent(), b)

        val fixed = a.refactor()
                .visit(ChangeFieldName(JavaType.Class.build("B"), "n", "n1"))
                .fix().fixed

        assertRefactored(fixed, """
            public class A {
                B b = new B();
                {
                    b.n1 = 1;
                }
            }
        """)
    }
}
