package org.openrewrite.yaml.tree

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class DocumentTest: YamlParserTest {

    @Test
    fun explicitStart() = assertRoundTrip(
            source = """
                ---
                type: specs.openrewrite.org/v1beta/visitor
                ---
                
                type: specs.openrewrite.org/v1beta/recipe
            """,
            afterConditions = { y ->
                Assertions.assertThat(y.documents).hasSize(2)
                Assertions.assertThat(y.documents[0].isExplicit).isTrue()
            }
    )

    @Test
    fun explicitEnd() = assertRoundTrip(
            source = """
                type: specs.openrewrite.org/v1beta/visitor
                ...
                ---
                type: specs.openrewrite.org/v1beta/recipe
                
                ...
            """,
            afterConditions = { y ->
                Assertions.assertThat(y.documents).hasSize(2)
                Assertions.assertThat(y.documents[0].end.isExplicit).isTrue()
            }
    )

    @Test
    fun implicitStart() = assertRoundTrip(
            source = "type: specs.openrewrite.org/v1beta/visitor",
            afterConditions = { y ->
                Assertions.assertThat(y.documents).hasSize(1)
                Assertions.assertThat(y.documents[0].isExplicit).isFalse()
            }
    )

    @Test
    fun implicitEnd() = assertRoundTrip(
            source = "type: specs.openrewrite.org/v1beta/visitor",
            afterConditions = { y ->
                Assertions.assertThat(y.documents).hasSize(1)
                Assertions.assertThat(y.documents[0].end.isExplicit).isFalse()
            }
    )
}
