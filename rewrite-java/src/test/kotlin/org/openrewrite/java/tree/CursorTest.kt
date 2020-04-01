package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRetrieveCursorVisitor
import org.openrewrite.java.JavaSourceVisitor

open class CursorTest : JavaParser() {
    @Test
    fun inSameNameScope() {
        val a = parse("""
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

        fun Tree.cursor() = JavaRetrieveCursorVisitor(id).visit(a)

        val fieldScope = a.classes[0].fields[0].cursor()!!
        val methodParamScope = a.classes[0].methods[0].params.params[0].cursor()!!
        val forInitScope = a.classes[0].methods[0].body!!.statements.filterIsInstance<J.ForLoop>()[0].control.init.cursor()!!

        assertThat(object : JavaSourceVisitor<Int>() {
            override fun defaultTo(t: Tree?): Int = 0
            override fun isCursored(): Boolean = true

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