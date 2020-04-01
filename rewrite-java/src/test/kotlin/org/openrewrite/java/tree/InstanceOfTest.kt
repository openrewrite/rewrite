package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass
import org.openrewrite.java.firstMethodStatement

open class InstanceOfTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                Object o;
                public void test() {
                    boolean b = o instanceof String;
                }
            }
        """)
    }

    private val variable by lazy { a.firstMethodStatement() as J.VariableDecls }
    private val instanceof by lazy { variable.vars[0].initializer as J.InstanceOf }

    @Test
    fun instanceOf() {
        assertEquals("java.lang.String", (instanceof.clazz as J.Ident).type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun format() {
        assertEquals("o instanceof String", instanceof.printTrimmed())
    }
}