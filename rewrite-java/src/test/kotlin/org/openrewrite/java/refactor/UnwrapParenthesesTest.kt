package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J

class UnwrapParenthesesTest : JavaParser() {
    @Test
    fun unwrapAssignment() {
        val a = parse("""
            public class A {
                boolean a;
                {
                    a = (true);
                }
            }
        """.trimIndent())

        val assignment = ((a.classes[0].body.statements[1] as J.Block<*>).statements[0] as J.Assign).assignment as J.Parentheses<*>
        val fixed = a.refactor().visit(UnwrapParentheses(assignment)).fix().fixed

        assertRefactored(fixed, """
            public class A {
                boolean a;
                {
                    a = true;
                }
            }
        """)
    }

    @Test
    fun unwrapIfCondition() {
        val a = parse("""
            public class A {
                {
                    if((true)) {}
                }
            }
        """.trimIndent())

        val cond = ((a.classes[0].body.statements[0] as J.Block<*>).statements[0] as J.If).ifCondition.tree as J.Parentheses<*>
        val fixed = a.refactor().visit(UnwrapParentheses(cond)).fix().fixed

        assertRefactored(fixed, """
            public class A {
                {
                    if(true) {}
                }
            }
        """)
    }
}
