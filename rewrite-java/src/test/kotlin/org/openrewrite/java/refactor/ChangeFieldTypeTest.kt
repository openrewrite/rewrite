package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

open class ChangeFieldTypeTest : JavaParser() {

    @Test
    fun changeFieldType() {
        val a = parse("""
            import java.util.List;
            public class A {
               List collection;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .visit(ChangeFieldType(a.classes[0].findFields("java.util.List")[0], "java.util.Collection"))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.Collection;
            
            public class A {
               Collection collection;
            }
        """)
    }
}