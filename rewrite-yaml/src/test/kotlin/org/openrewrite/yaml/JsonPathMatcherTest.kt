/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.yaml

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.yaml.tree.Yaml

class JsonPathMatcherTest {

    @Language("yaml")
    private val simple = arrayOf("""
        ---
        literal: $.literal
        object:
          literal: $.object.literal
        list:
          - literal: $.object.list[0]
    """.trimIndent())

    @Language("yaml")
    private val sliceList = arrayOf("""
        ---
        list:
          - item1: index0
            property: property1
          - item2: index1
            property: property2
          - item3: index2
            property: property3
    """.trimIndent())

    @Language("yaml")
    private val complex = arrayOf("""
        ---
        literal: $.literal
        object:
          literal: $.object.literal
          object:
            literal: $.object.object.literal
            list:
              - literal: $.object.list[0].literal
        literals:
          - $.literal[0]
        objects:
          literal: $.objects.literal
          object:
            literal: $.objects.object.literal
            object:
              literal: $.objects.object.object.literal
            list:
              - literal: $.objects.object.list[0].literal
        lists:
          - list:
            - object:
                literal: $.lists[0].list[0].object.literal
                object:
                  literal: $.lists[0].list[0].object.object.literal
                list:
                  - literal: $.lists[0].list[0].object.list[0].literal
      """.trimIndent())

    @Test
    fun doesNotMatchMissingProperty() = assertNotMatched(
        jsonPath = "$.none",
        before = simple
    )

    @Test
    fun wildcardAtRoot() = assertMatched(
        jsonPath = "$.*",
        before = simple,
        after = arrayOf("literal: $.literal",
            """
                object:
                literal: $.object.literal
            """.trimIndent(),
            """
                list:
                - literal: $.object.list[0]
            """.trimIndent()
        )
    )

    @Disabled("Matches include false positives.")
    @Test
    fun multipleWildcards() = assertMatched(
        jsonPath = "$.*.*",
        before = simple,
        after = arrayOf("literal: $.object.literal", "- literal: $.object.list[0]")
    )

    @Disabled("Matches include false positives.")
    @Test
    fun allPropertiesInKeyFromRoot() = assertMatched(
        // This produces two false positives from $.literal and $.list.literal.
        jsonPath = "$.*.literal",
        before = simple,
        after = arrayOf("literal: $.object.literal")
    )

    @Test
    fun matchObjectAtRoot() = assertMatched(
        jsonPath = "$.object",
        before = simple,
        after = arrayOf(
            """
                object:
                literal: $.object.literal
            """.trimIndent())
    )

    @Test
    fun matchLiteral() = assertMatched(
        jsonPath = "$.literal",
        before = simple,
        after = arrayOf("literal: $.literal"),
    )

    @Test
    fun dotOperatorIntoObject() = assertMatched(
        jsonPath = "$.object.literal",
        before = simple,
        after = arrayOf("literal: $.object.literal")
    )

    @Test
    fun dotOperatorByBracketName() = assertMatched(
        jsonPath = "$.['object'].['literal']",
        before = simple,
        after = arrayOf("literal: $.object.literal")
    )

    @Test
    fun bracketNameAtRoot() = assertMatched(
        jsonPath = "['object']",
        before = simple,
        after = arrayOf(
            """
                object:
                literal: $.object.literal
            """.trimIndent())
    )

    @Test
    fun relativePath() = assertMatched(
        jsonPath = ".literal",
        before = simple,
        after = arrayOf("literal: $.literal", "literal: $.object.literal", "literal: $.object.list[0]")
    )

    @Test
    fun recurseToMatchProperties() = assertMatched(
        jsonPath = "$..object.literal",
        before = complex,
        after = arrayOf(
            "literal: $.object.literal",
            "literal: $.object.object.literal",
            "literal: $.objects.object.literal",
            "literal: $.objects.object.object.literal",
            "literal: $.lists[0].list[0].object.literal",
            "literal: $.lists[0].list[0].object.object.literal")
    )

    @Test
    fun recurseFromScopeOfObject() = assertMatched(
        jsonPath = "$.object..literal",
        before = complex,
        after = arrayOf(
            "literal: $.object.literal",
            "literal: $.object.object.literal",
            "literal: $.object.list[0].literal")
    )

    @Test
    fun firstNElements() = assertMatched(
        jsonPath = "$.list[:1]",
        before = sliceList,
        after = arrayOf(
            """
                - item1: index0
                property: property1
            """.trimIndent())
    )

    @Test
    fun lastNElements() = assertMatched(
        jsonPath = "$.list[-1:]",
        before = sliceList,
        after = arrayOf(
            """
                - item3: index2
                property: property3
            """.trimIndent())
    )

    @Test
    fun fromStartToEndPos() = assertMatched(
        jsonPath = "$.list[0:1]",
        before = sliceList,
        after = arrayOf(
            """
                - item1: index0
                property: property1
            """.trimIndent(),
            """
                - item2: index1
                property: property2
            """.trimIndent())
    )

    @Test
    fun allElementsFromStartPos() = assertMatched(
        jsonPath = "$.list[1:]",
        before = sliceList,
        after = arrayOf(
            """
                - item2: index1
                property: property2
            """.trimIndent(),
            """
                - item3: index2
                property: property3
         """.trimIndent())
    )

    @Test
    fun allElements() = assertMatched(
        jsonPath = "$.list[*]",
        before = sliceList,
        after = arrayOf(
            """
                - item1: index0
                property: property1
            """.trimIndent(),
            """
                - item2: index1
                property: property2
            """.trimIndent(),
            """
                - item3: index2
                property: property3
            """.trimIndent())
    )

    @Test
    fun bracketOperatorByNames() = assertMatched(
        jsonPath = "$.['literal', 'object']",
        before = simple,
        after = arrayOf("literal: $.literal",
            """
                object:
                literal: $.object.literal
            """.trimIndent())
    )

    @Test
    fun doesNotMatchIndex() = assertNotMatched(
        jsonPath = "$.list[4]",
        before = sliceList
    )

    @Test
    fun bracketOperatorByIndexes() = assertMatched(
        jsonPath = "$.list[0, 1]",
        before = sliceList,
        after = arrayOf(
            """
                - item1: index0
                property: property1
            """.trimIndent(),"""
                - item2: index1
                property: property2
            """.trimIndent())
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun doesNotMatchWrongValue() = assertNotMatched(
        jsonPath = "$..list[?(@.literal == 'no-match')].literal",
        before = complex
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun filterOnPropertyWithEquality() = assertMatched(
        jsonPath = "$..list[?(@.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
        before = complex,
        after = arrayOf("literal: $.lists[0].list[0].object.list[0].literal")
    )

    @Disabled("Implement logical &&.")
    @Test
    fun doesNotMatchWrongAnd() = assertNotMatched(
        jsonPath = "$..list[?(@.literal == 'no-match' && @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
        before = complex
    )

    @Disabled("Implement logical &&.")
    @Test
    fun filterOnPropertyWithAnd() = assertMatched(
        jsonPath = "$..list[?($.literal == '$.literal' && @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
        before = complex,
        after = arrayOf("literal: $.lists[0].list[0].object.list[0].literal")
    )

    @Disabled("Implement logical || and operator precedence")
    @Test
    fun filterOnPropertyWithOr() = assertMatched(
        jsonPath = "$..list[?(@.literal == 'no-match' || @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
        before = complex,
        after = arrayOf("literal: $.lists[0].list[0].object.list[0].literal")
    )

    @Disabled("Implement logical || and operator precedence")
    @Test
    fun doesNotMatchWrongOr() = assertNotMatched(
        jsonPath = "$..list[?(@.literal == 'no-match-1' || @.literal == 'no-match-2')].literal",
        before = complex
    )

    @Test
    fun unaryExpression() = assertMatched(
        jsonPath = "$.list[?(@.property)]",
        before = sliceList,
        after = arrayOf(
            """
                item1: index0
                   property: property1
            """.trimIndent(),
            """
                item2: index1
                   property: property2
            """.trimIndent(),
            """
                item3: index2
                   property: property3
            """.trimIndent()
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun filterOnPropertyWithMatches() = assertMatched(
        jsonPath = "$..list[?(@.literal =~ '.*objects.*')].literal",
        before = complex,
        after = arrayOf("literal: $.objects.object.list[0].literal")
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun recursiveDecentToFilter() = assertMatched(
        jsonPath = "$..[?(@.literal =~ '.*objects.object.literal.*')]",
        before = complex,
        after = arrayOf("$.objects.object.literal",
            """
                literal: $.objects.object.literal
                object:
                  literal: $.objects.object.object.literal
                list:
                  - literal: $.objects.object.list[0].literal
            """.trimIndent())
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun nestedSequences() = assertMatched(
        jsonPath = "$..list[*].[?(@.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
        before = complex,
        after = arrayOf("literal: $.lists[0].list[0].object.list[0].literal")
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun matchesLiteralInListByFilterCondition() = assertMatched(
        jsonPath = "$.list.*[?(@.item1 == 'index0')].item1",
        before = sliceList,
        after = arrayOf("item1: index0")
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    fun matchesListWithByFilterConditionInList() = assertMatched(
        jsonPath = "$.list.*[?(@.item3 == 'index2')]",
        before = sliceList,
        after = arrayOf("""
            item3: index2
               property: property3
            """.trimIndent())
    )

    @Test
    fun returnResultsWithVisitDocument() {
        val ctx = InMemoryExecutionContext { it.printStackTrace() }
        val documents = YamlParser().parse(ctx, *simple)
        val matcher = JsonPathMatcher("$.literal")
        val results = ArrayList<Yaml>()
        documents.forEach {
            object : YamlIsoVisitor<MutableList<Yaml>>() {
                override fun visitDocument(document: Yaml.Document, p: MutableList<Yaml>): Yaml.Document {
                    val d = super.visitDocument(document, p)
                    if (matcher.find<Yaml>(cursor).isPresent) {
                        p.add(d)
                    }
                    return d
                }
            }.visit(it, results)
        }
        @Suppress("SameParameterValue")
        assertThat(results).hasSize(1)
        assertThat(results[0] is Yaml.Document).isTrue
    }

    private fun assertNotMatched(before: Array<String>, jsonPath: String) {
        val results = visit(before, jsonPath, false)
        assertThat(results).hasSize(0)
    }

    private fun assertMatched(before: Array<String>, after: Array<String>, jsonPath: String, printMatches: Boolean = false) {
        val results = visit(before, jsonPath, printMatches)
        assertThat(results).hasSize(after.size)
        for (n in results.indices) {
            assertThat(results[n]).isEqualTo(after[n])
        }
    }

    private fun visit(before: Array<String>, jsonPath: String, printMatches: Boolean): List<String> {
        val ctx = InMemoryExecutionContext { it.printStackTrace() }
        val documents = YamlParser().parse(ctx, *before)
        if (documents.isEmpty()) {
            return emptyList()
        }
        val matcher = JsonPathMatcher(jsonPath)

        val results = ArrayList<String>()
        documents.forEach {
            object : YamlIsoVisitor<MutableList<String>>() {
                override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: MutableList<String>): Yaml.Mapping.Entry {
                    val e = super.visitMappingEntry(entry, p)
                    if (matcher.matches(cursor)) {
                        val j: Yaml = e.withPrefix("")
                        val match = j.printTrimmed(cursor)
                        if (printMatches) {
                            println("matched in visitMappingEntry")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return e
                }

                override fun visitMapping(mapping: Yaml.Mapping, p: MutableList<String>): Yaml.Mapping {
                    val e = super.visitMapping(mapping, p)
                    if (matcher.matches(cursor)) {
                        val j: Yaml = e.withPrefix("")
                        val match = j.printTrimmed(cursor)
                        if (printMatches) {
                            println("matched in visitMapping")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return e
                }

                override fun visitSequence(sequence: Yaml.Sequence, p: MutableList<String>): Yaml.Sequence {
                    val e = super.visitSequence(sequence, p)
                    if (matcher.matches(cursor)) {
                        val j: Yaml = e.withPrefix("")
                        val match = j.printTrimmed(cursor)
                        if (printMatches) {
                            println("matched in visitSequence")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return e
                }

                override fun visitSequenceEntry(entry: Yaml.Sequence.Entry, p: MutableList<String>): Yaml.Sequence.Entry {
                    val e = super.visitSequenceEntry(entry, p)
                    if (matcher.matches(cursor)) {
                        val j: Yaml = e.withPrefix("")
                        val match = j.printTrimmed(cursor)
                        if (printMatches) {
                            println("matched in visitSequenceEntry")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return e
                }

                override fun visitScalar(scalar: Yaml.Scalar, p: MutableList<String>): Yaml.Scalar {
                    val e = super.visitScalar(scalar, p)
                    if (matcher.matches(cursor)) {
                        val j: Yaml = e.withPrefix("")
                        val match = j.printTrimmed(cursor)
                        if (printMatches) {
                            println("matched in visitScalar")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return e
                }
            }.visit(it, results)
        }
        return results
    }
}
