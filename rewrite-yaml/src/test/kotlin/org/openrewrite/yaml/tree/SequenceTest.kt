package org.openrewrite.yaml.tree

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class SequenceTest: YamlParserTest {

    @Test
    fun blockSequence() = assertRoundTrip(
            source = """
                - apples
                - oranges
            """,
            afterConditions = { y ->
                Assertions.assertThat((y.documents[0].block as Yaml.Sequence).entries.map { it.block }
                    .map { it as Yaml.Scalar }.map { it.value })
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
                Assertions.assertThat(seq.entries[0].trailingCommaPrefix).isEqualTo(" ")
                Assertions.assertThat(seq.entries[1].trailingCommaPrefix).isEqualTo("  ")
                Assertions.assertThat(seq.entries[2].trailingCommaPrefix).isNull()
            }
    )
}
