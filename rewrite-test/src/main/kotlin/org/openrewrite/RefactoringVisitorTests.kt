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
            before: String,
            after: String) {
        before.whenParsedBy(parser)
                .whenVisitedBy(visitors)
                .isRefactoredTo(after)
    }

    @BeforeEach
    fun beforeEach() {
        if(parser is JavaParser) {
            (parser as JavaParser).reset()
        }
    }
}
