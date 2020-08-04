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
package org.openrewrite

import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * Provides a standardized shape for test classes that exercise refactoring visitors to take.
 */
interface RefactorVisitorTest {
    val visitors: Iterable<RefactorVisitor<*>>
        get() = emptyList()

    /**
     * Parse the "before" text, apply the visitors, assert that the result is "after"
     */
    fun <S : SourceFile> assertRefactored(
            parser: Parser<S>,
            visitors: Iterable<RefactorVisitor<*>> = this.visitors,
            visitorsMapped: Iterable<(S) -> RefactorVisitor<in S>> = emptyList(),
            visitorsMappedToMany: Iterable<(S) -> Iterable<RefactorVisitor<in S>>> = emptyList(),
            dependencies: List<String> = listOf(),
            before: String,
            after: String) {
        before.trimIndent()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isRefactoredTo(after.trimIndent())
    }

    /**
     * Parse the "before" file, apply the visitors, assert that the result is "after"
     */
    fun <S : SourceFile> assertRefactored(
            parser: Parser<S>,
            visitors: Iterable<RefactorVisitor<*>> = this.visitors,
            visitorsMapped: Iterable<(S) -> RefactorVisitor<in S>> = emptyList(),
            visitorsMappedToMany: Iterable<(S) -> Iterable<RefactorVisitor<in S>>> = emptyList(),
            dependencies: List<File> = listOf(),
            before: File,
            after: String) {
        before.toPath()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.map { it.toPath() }.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isRefactoredTo(after.trimIndent())
    }
}

interface RefactorVisitorTestForParser<S : SourceFile> : RefactorVisitorTest {
    val parser: Parser<S>

    fun assertRefactored(
            visitors: Iterable<RefactorVisitor<*>> = this.visitors,
            visitorsMapped: Iterable<(S) -> RefactorVisitor<in S>> = emptyList(),
            visitorsMappedToMany: Iterable<(S) -> Iterable<RefactorVisitor<in S>>> = emptyList(),
            dependencies: List<String> = listOf(),
            before: String,
            after: String) {
        return assertRefactored(parser, visitors, visitorsMapped, visitorsMappedToMany, dependencies, before, after)
    }

    fun assertRefactored(
            visitors: Iterable<RefactorVisitor<*>> = this.visitors,
            visitorsMapped: Iterable<(S) -> RefactorVisitor<in S>> = emptyList(),
            visitorsMappedToMany: Iterable<(S) -> Iterable<RefactorVisitor<in S>>> = emptyList(),
            dependencies: List<File> = listOf(),
            before: File,
            after: String) {
        return assertRefactored(parser, visitors, visitorsMapped, visitorsMappedToMany, dependencies, before, after)
    }

    @BeforeEach
    fun beforeEach() {
        parser.reset()
    }
}
