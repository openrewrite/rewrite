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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.openrewrite.internal.StringUtils

interface RecipeTest {
    val recipe: Recipe?
        get() = null

    val treePrinter: TreePrinter<*, *>?
        get() = null

    fun assertChanged(
        parser: Parser<*>,
        recipe: Recipe? = this.recipe,
        before: String,
        after: String
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull()

        val source = parser.parse(before.trimIndent()).first()

        val results = recipe!!.run(listOf(source),
            ExecutionContext.builder()
                .maxCycles(2)
                .doOnError { t: Throwable? -> Assertions.fail<Any>("Recipe threw an exception", t) }
                .build())

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.find { s -> source === s.before }

        assertThat(result).`as`("The recipe must make changes").isNotNull()
        assertThat(result!!.after).isNotNull()
        assertThat(result.after!!.printTrimmed(treePrinter ?: TreePrinter.identity<Tree, Any>()))
            .isEqualTo(after.trimIndent())
    }

    fun assertUnchanged(parser: Parser<*>, recipe: Recipe? = this.recipe, before: String?) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull()

        val source = parser.parse(StringUtils.trimIndent(before)).iterator().next()
        val results = recipe!!.run(listOf(source))

        assertThat(results).`as`("The recipe must not make changes").isEmpty()
    }
}
