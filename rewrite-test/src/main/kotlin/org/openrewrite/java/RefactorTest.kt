/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting
import org.openrewrite.Refactor
import org.openrewrite.java.tree.J
import java.util.*

class RefactorTest {
    class RefactorTestException : RuntimeException("")
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

    @Test
    fun throwsEagerly() {
        Assertions.assertThrows(RefactorTestException::class.java) {
            Refactor(true)
                    .visit(throwingVisitor)
                    .fix(listOf(cu))
        }
    }

    @Test
    fun suppressesExceptions() {
        Refactor()
                .visit(throwingVisitor)
                .fix(listOf(cu))
    }
}