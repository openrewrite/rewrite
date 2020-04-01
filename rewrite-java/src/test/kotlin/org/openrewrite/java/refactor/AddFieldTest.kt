package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.Formatting.EMPTY
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J
import java.util.Collections.singletonList

open class AddFieldTest : JavaParser() {
    val private: List<J.Modifier> = singletonList(J.Modifier.Private(randomId(), EMPTY) as J.Modifier)

    @Test
    fun addFieldDefaultIndent() {
        val a = parse("""
            class A {
            }
        """.trimIndent())

        val fixed = a.refactor()
                .visit(AddField(a.classes[0], private, "java.util.List", "list", "new ArrayList<>()"))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {
                private List list = new ArrayList<>();
            }
        """)
    }

    @Test
    fun addFieldMatchSpaces() {
        val a = parse("""
            import java.util.List;
            
            class A {
              List l;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .visit(AddField(a.classes[0], private, "java.util.List", "list", null))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {
              private List list;
              List l;
            }
        """)
    }

    @Test
    fun addFieldMatchTabs() {
        val a = parse("""
            import java.util.List;
            
            class A {
                       List l;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .visit(AddField(a.classes[0], private, "java.util.List", "list", null))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {
                       private List list;
                       List l;
            }
        """)
    }
}
