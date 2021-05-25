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
package org.openrewrite.yaml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.tree.Yaml

class SequenceTest: YamlParserTest {

    @Test
    fun blockSequence() = assertRoundTrip(
            source = """
                - apples
                - oranges
            """,
            afterConditions = { y ->
                assertThat((y.documents[0].block as Yaml.Sequence).entries.map { it.block }.map { it as Yaml.Scalar }.map { it.value })
                        .containsExactly("apples", "oranges")
            }
    )

    @Test
    fun blockSequenceOfMappings() = assertRoundTrip("""
            - name: Fred
              age: 45
            - name: Barney
              age: 25
    """)

    @Test
    fun multiLineInlineSequenceWithFunnyIndentation() = assertRoundTrip("""
            [
                a, 
            b,
                    c,
            ]
    """)


    @Test
    fun sequenceOfEmptyInlineSequence() = assertRoundTrip("- []")

    @Test
    fun sequenceOfMapOfEmptyInlineSequence() = assertRoundTrip("- foo: []")


    @Test
    fun sequenceOfInlineSequence() = assertRoundTrip("- [a, b]")

    @Test
    fun sequenceOfMapOfInlineSequence() = assertRoundTrip("- foo: [a, b]")

    @Test
    fun sequencesOfSequencesOfSequences() = assertRoundTrip("""
        [[],
        [1, 2, 3, []],
        [ [ ] ],]
    """)

    @Test
    fun sequenceOfMixedSequences() = assertRoundTrip("""
        - []
        - [ 1 ]
        - foo: []
        - bar:
        - baz: [
            a]
    """)

    @Test
    fun inlineSequenceWithWhitespaceBeforeCommas() = assertRoundTrip(
            source = "[1 ,2  ,0]",
            afterConditions = { y ->
                val seq = y.documents.first().block as Yaml.Sequence
                assertThat(seq.entries[0].trailingCommaPrefix).isEqualTo(" ")
                assertThat(seq.entries[1].trailingCommaPrefix).isEqualTo("  ")
                assertThat(seq.entries[2].trailingCommaPrefix).isNull()
            }
    )
}
