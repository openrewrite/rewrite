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
package org.openrewrite.yaml

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe

class CoalescePropertiesTest : YamlRecipeTest {
    override val recipe: Recipe
        get() = CoalesceProperties()

    @Test
    fun fold() = assertChanged(
        before = """
            management:
                metrics:
                    enable.process.files: true
                endpoint:
                    health:
                        show-components: always
                        show-details: always
        """,
        after = """
            management:
                metrics.enable.process.files: true
                endpoint.health:
                    show-components: always
                    show-details: always
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1103")
    @Test
    fun foldSequenceOfObjects() = assertChanged(
        before = """
          foo:
            bar:
              scalar: value
              sequence:
                - name: name
                  propertyA: fieldA
                  propertyB: fieldB
                - name: name
                  propertyA: fieldA
                  propertyB: fieldB
        """,
        after = """
          foo.bar:
            scalar: value
            sequence:
              - name: name
                propertyA: fieldA
                propertyB: fieldB
              - name: name
                propertyA: fieldA
                propertyB: fieldB
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1103")
    @Test
    fun foldSequence() = assertChanged(
        before = """
          foo:
            bar:
              baz: value
              buz:
                - item1
                - item2
                - item3
        """,
        after = """
          foo.bar:
            baz: value
            buz:
              - item1
              - item2
              - item3
        """
    )

    @Test
    @Disabled
    fun group() = assertChanged(
        before = """
            management.metrics.enable.process.files: true
            management.metrics.enable.jvm: true
        """,
        after = """
            management.metrics.enable:
                process.files: true
                jvm: true
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1125")
    @Disabled
    fun foldWithCommentsInPrefix() = assertChanged(
        before = """
            a:
              b:
                # d-comment
                d:
                  e.f: true
                c: c-value
        """,
        after = """
            a.b:
              # d-comment
              d.e.f: true
              c: c-value
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1125")
    @Disabled
    fun foldWithCommentsAfterKey() = assertChanged(
        before = """
            a:
              b:
                d: # d-comment
                  e.f: true
                c: c-value
        """,
        after = """
            a.b:
              d: # d-comment
                e.f: true
              c: c-value
        """
    )

}
