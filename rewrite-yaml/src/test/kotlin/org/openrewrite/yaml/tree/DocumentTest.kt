/*
 * Copyright 2021 the original author or authors.
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
