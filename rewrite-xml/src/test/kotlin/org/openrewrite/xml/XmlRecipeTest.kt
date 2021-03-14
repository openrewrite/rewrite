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
package org.openrewrite.xml

import org.intellij.lang.annotations.Language
import org.openrewrite.*
import org.openrewrite.marker.SearchResult

interface XmlRecipeTest : RecipeTest {
    override val parser: XmlParser
        get() = XmlParser.builder().build()

    override val treePrinter: TreePrinter<*>?
        get() = SearchResult.printer("<!--~~>-->", "<!--~~(%s)~~>-->")

    fun assertChanged(
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>,
        @Language("xml") after: String,
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, 1) {}
    }

    fun assertChanged(
        @Language("xml") before: String,
        @Language("xml") after: String,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, 1) {}
    }

    fun assertChanged(
        parser: Parser<*>?,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>,
        @Language("xml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>,
        @Language("xml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    fun assertChanged(
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>,
        @Language("xml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("xml") before: String,
        @Language("xml") after: String,
        cycles: Int,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles) {}
    }

    fun <T : SourceFile> assertChanged(
        parser: Parser<T>?,
        recipe: Recipe?,
        @Language("xml") before: String,
        @Language("xml") after: String,
        cycles: Int,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles) {}
    }
    
    override fun assertChanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>,
        @Language("xml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    override fun <T : SourceFile> assertChanged(
        parser: Parser<T>?,
        recipe: Recipe?,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>,
        @Language("xml") after: String,
        cycles: Int,
        afterConditions: (T) -> Unit
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, afterConditions)
    }

    override fun assertUnchanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }
}
