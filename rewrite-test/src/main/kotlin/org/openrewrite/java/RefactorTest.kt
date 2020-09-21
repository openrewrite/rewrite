package org.openrewrite.java

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting
import org.openrewrite.Refactor
import org.openrewrite.java.tree.J
import java.util.*


class RefactorTest {
    class RefactorTestException : RuntimeException("")

    @Test
    fun throwsEagerlyUnderTest() {
        val cu = J.CompilationUnit(
                UUID.randomUUID(),
                "",
                listOf(),
                null,
                listOf(),
                listOf(),
                Formatting.EMPTY,
                listOf()
        )
        val throwingVisitor = object : JavaRefactorVisitor() {
            override fun visitCompilationUnit(cu: J.CompilationUnit?): J {
                throw RefactorTestException()
            }
        }
        Assertions.assertThrows(RefactorTestException::class.java) {
            Refactor()
                    .visit(throwingVisitor)
                    .fix(listOf(cu))
        }
    }
}