package org.openrewrite

import org.junit.jupiter.api.BeforeEach
import org.openrewrite.java.JavaParser

/**
 * Provides a standardized shape for test classes that exercise refactoring visitors to take.
 */
interface RefactoringVisitorTests<T: Parser<*>> {
    val parser: T

    val visitors: Iterable<RefactorVisitor<*>>

    /**
     * Parse the "before" text, apply the visitors, assert that the result is "after"
     */
    fun assertRefactored(
            visitors: Iterable<RefactorVisitor<*>> = this.visitors,
            dependencies: List<String> = listOf(),
            before: String,
            after: String) {
        before.trimIndent()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.toTypedArray())
                .whenVisitedBy(visitors)
                .isRefactoredTo(after.trimIndent())
    }

    @BeforeEach
    fun beforeEach() {
        if(parser is JavaParser) {
            (parser as JavaParser).reset()
        }
    }
}
