package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

open class DeleteStatementTest : JavaParser() {

    @Test
    fun deleteField() {
        val a = parse("""
            import java.util.List;
            public class A {
               List collection = null;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .fold(a.classes[0].findFields("java.util.List")) { DeleteStatement(it) }
                .fix().fixed

        assertRefactored(fixed, """
            public class A {
            }
        """)
    }
}
