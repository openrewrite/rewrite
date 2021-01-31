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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.fail
import org.openrewrite.java.JavaVisitor
import java.io.File

interface RecipeTest {
    val recipe: Recipe?
        get() = null

    val treePrinter: TreePrinter<*>?
        get() = null

    val parser: Parser<*>?
        get() = null

    fun assertChanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: String,
        dependsOn: Array<String> = emptyArray(),
        after: String
    ) {
        assertChanged(parser, recipe, before, dependsOn, after) {}
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : SourceFile> assertChanged(
        parser: Parser<T>? = this.parser as Parser<T>?,
        recipe: Recipe? = this.recipe,
        before: String,
        dependsOn: Array<String> = emptyArray(),
        after: String,
        afterConditions: (T) -> Unit = { }
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val m = ObjectMapper()
            .registerModule(ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

        val recipeMapper = m.setVisibility(
            m.serializationConfig.defaultVisibilityChecker
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        )

        assertThat(recipeMapper.readValue(recipeMapper.writeValueAsString(recipe), recipe!!.javaClass))
            .`as`("Recipe must be serializable/deserializable")
            .isEqualTo(recipe)

        val source = parser!!.parse(*(arrayOf(before.trimIndent()) + dependsOn)).first()

        val results = recipe.run(listOf(source),
            ExecutionContext.builder()
                .maxCycles(1)
                .doOnError { t: Throwable? -> fail<Any>("Recipe threw an exception", t) }
                .build())

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.find { s -> source === s.before }

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        assertThat(result.after!!.print(treePrinter ?: TreePrinter.identity<Any>(), null))
            .isEqualTo(after.trimIndent())
    }

    fun assertChanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: File,
        dependsOn: Array<File> = emptyArray(),
        after: String
    ) {
        assertChanged(parser, recipe, before, dependsOn, after) {}
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : SourceFile> assertChanged(
        parser: Parser<T>? = this.parser as Parser<T>?,
        recipe: Recipe? = this.recipe,
        before: File,
        dependsOn: Array<File> = emptyArray(),
        after: String,
        afterConditions: (T) -> Unit = { }
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = parser!!.parse((listOf(before) + dependsOn).map { it.toPath() }, null).first()

        val results = recipe!!.run(listOf(source),
            ExecutionContext.builder()
                .maxCycles(2)
                .doOnError { t: Throwable? -> fail<Any>("Recipe threw an exception", t) }
                .build())

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.find { s -> source === s.before }

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        assertThat(result.after!!.printTrimmed(treePrinter ?: TreePrinter.identity<Any>()))
            .isEqualTo(after.trimIndent())
    }

    fun assertUnchanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: String,
        dependsOn: Array<String> = emptyArray()
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = parser!!.parse(*(arrayOf(before.trimIndent()) + dependsOn)).iterator().next()
        val results = recipe!!.run(listOf(source))

        results.forEach { result ->
            if (result.diff().isEmpty()) {
                fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
            }
        }

        for (result in results) {
            assertThat(result.after?.print())
                .`as`("The recipe must not make changes")
                .isEqualTo(result.before?.print())
        }
    }

    fun assertUnchanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: File,
        dependsOn: Array<File> = emptyArray()
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = parser!!.parse((listOf(before) + dependsOn).map { it.toPath() }, null).first()
        val results = recipe!!.run(listOf(source))

        results.forEach { result ->
            if (result.diff().isEmpty()) {
                fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
            }
        }

        for (result in results) {
            assertThat(result.after?.print())
                .`as`("The recipe must not make changes")
                .isEqualTo(result.before?.print())
        }
    }

    fun JavaVisitor<ExecutionContext>.toRecipe(cursored: Boolean = false) = object : Recipe() {
        override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
            if(cursored) {
                this@toRecipe.setCursoringOn()
            }
            return this@toRecipe
        }
    }
}
