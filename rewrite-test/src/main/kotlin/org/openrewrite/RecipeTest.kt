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
 * Provides a standardized shape for test classes that exercise recipes.
 */
interface RecipeTest {
    val visitors: Iterable<Recipe<*>>
        get() = emptyList()

    private fun assertBeforeAndAfterAreDifferent(before: String, after: String) {
        if (before.trimIndent() == after.trimIndent()) {
            throw RuntimeException(
                    "'before' and 'after' are equal. " +
                            "Looks like you're trying to assert that the visitors should make no changes. " +
                            "Instead of RecipeTest.assertRefactored(), use RecipeTest.assertUnchanged()")
        }
    }

    /**
     * Parse the "before" text, apply the visitors, assert that the result is "after"
     */
    fun <S : SourceFile> assertRefactored(
        parser: Parser<S>,
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<String> = listOf(),
        before: String,
        after: String,
        afterConditions: (S) -> Unit = {}) {
        assertBeforeAndAfterAreDifferent(before, after)
        before.trimIndent()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isRefactoredTo(after.trimIndent(), afterConditions)
    }

    /**
     * Just like the other assertRefactored, but for when you want "after" to be lazily-evaluated.
     * This is niche, for those situations where the result should depend on something figured out during refactoring
     */
    fun <S : SourceFile> assertRefactored(
        parser: Parser<S>,
        visitors: Iterable<Recipe<*>>,
        visitorsMapped: Iterable<(S) -> Recipe<in S>>,
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>>,
        dependencies: List<String>,
        before: String,
        after: () -> String,
        afterConditions: (S) -> Unit = {}) {
        // To lazily evaluate after() and maintain consistency with the other versions of assertRefactored,
        // curry in a trimIndent() and assertBeforeAndAfterAreDifferent() to be evaluated no sooner than after() is
        val afterTrimmed: () -> String = {
            val afterText = after().trimIndent()
            assertBeforeAndAfterAreDifferent(before, afterText)
            afterText
        }
        before.trimIndent()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isRefactoredTo(afterTrimmed, afterConditions)
    }

    /**
     * Parse the "before" text, apply the visitors, assert that there are no changes
     */
    fun <S : SourceFile> assertUnchanged(parser: Parser<S>,
                                         visitors: Iterable<Recipe<*>> = this.visitors,
                                         visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
                                         visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
                                         dependencies: List<String> = listOf(),
                                         before: String) {
        before.trimIndent()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isUnchanged
    }

    /**
     * Parse the "before" text, apply the visitors, assert that the result is "after"
     */
    fun <S : SourceFile> assertRefactored(
        parser: Parser<S>,
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<File> = listOf(),
        before: File,
        after: String,
        afterConditions: (S) -> Unit = {}) {
        assertBeforeAndAfterAreDifferent(before.readText(), after)
        before.toPath()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.map { it.toPath() }.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isRefactoredTo(after.trimIndent(), afterConditions)
    }

    /**
     * Parse the "before" text, apply the visitors, assert that there are no changes
     */
    fun <S : SourceFile> assertUnchanged(
        parser: Parser<S>,
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<File> = listOf(),
        before: File) {
        before.toPath()
                .whenParsedBy(parser)
                .whichDependsOn(*dependencies.map { it.toPath() }.toTypedArray())
                .whenVisitedBy(visitors)
                .let { visitorsMapped.fold(it) { acc, visitorMapping -> acc.whenVisitedByMapped(visitorMapping) } }
                .let { visitorsMappedToMany.fold(it) { acc, visitorMapping -> acc.whenVisitedByMany(visitorMapping) } }
                .isUnchanged
    }
}

interface RecipeTestForParser<S : SourceFile> : RecipeTest {
    val parser: Parser<S>

    fun assertRefactored(
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<String> = listOf(),
        before: String,
        after: String,
        afterConditions: (S) -> Unit = {}) {
        return assertRefactored(parser, visitors, visitorsMapped, visitorsMappedToMany,
                dependencies, before, after, afterConditions)
    }

    fun assertRefactored(
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<String> = listOf(),
        before: String,
        after: () -> String,
        afterConditions: (S) -> Unit = {}) {
        return assertRefactored(parser, visitors, visitorsMapped, visitorsMappedToMany,
                dependencies, before, after, afterConditions)
    }

    fun assertRefactored(
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<File> = listOf(),
        before: File,
        after: String,
        afterConditions: (S) -> Unit = {}) {
        return assertRefactored(parser, visitors, visitorsMapped, visitorsMappedToMany, dependencies,
                before, after, afterConditions)
    }

    fun assertUnchanged(
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<String> = listOf(),
        before: String
    ) {
        return assertUnchanged(parser, visitors, visitorsMapped, visitorsMappedToMany, dependencies, before)
    }

    fun assertUnchanged(
        visitors: Iterable<Recipe<*>> = this.visitors,
        visitorsMapped: Iterable<(S) -> Recipe<in S>> = emptyList(),
        visitorsMappedToMany: Iterable<(S) -> Iterable<Recipe<in S>>> = emptyList(),
        dependencies: List<File> = listOf(),
        before: File
    ) {
        return assertUnchanged(parser, visitors, visitorsMapped, visitorsMappedToMany, dependencies, before)
    }

    @BeforeEach
    fun beforeEach() {
        parser.reset()
    }
}
