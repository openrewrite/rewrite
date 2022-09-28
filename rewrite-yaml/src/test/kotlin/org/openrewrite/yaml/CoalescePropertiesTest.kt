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
              sequence:
                - name: name
                  propertyA: fieldA
                  propertyB: fieldB
                - name: name
                  propertyA: fieldA
                  propertyB: fieldB
              scalar: value
        """,
        after = """
          foo.bar:
            sequence:
              - name: name
                propertyA: fieldA
                propertyB: fieldB
              - name: name
                propertyA: fieldA
                propertyB: fieldB
            scalar: value
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1133")
    fun foldSequenceOfObjectsFormattedWithDashDirectlyUnderMappingKey() = assertChanged(
        before = """
          matrix:
            include:
            # comment-a
            # comment-b
            - name: entry-0-name # comment-c
                # comment-d
              value: entry-0-value
              # comment-e
            - name: entry-1-name
              value: entry-1-value
        """,
        after = """
          matrix.include:
          # comment-a
          # comment-b
          - name: entry-0-name # comment-c
              # comment-d
            value: entry-0-value
            # comment-e
          - name: entry-1-name
            value: entry-1-value
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1103")
    @Test
    fun foldSequence() = assertChanged(
        before = """
          foo:
            bar:
              buz:
                - item1
                - item2
                - item3
              baz: value
        """,
        after = """
          foo.bar:
            buz:
              - item1
              - item2
              - item3
            baz: value
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
    fun foldWithCommentsInPrefixWhenCommentsHaveDifferentIndentThanTheirElement() = assertChanged(
        before = """
            a:
              b:
              # d-comment
                d:
                  e.f: true
               # c-comment
                c:
                  d: d-value
        """,
        after = """
            a.b:
            # d-comment
              d.e.f: true
             # c-comment
              c.d: d-value
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1125")
    fun doNotFoldKeysWithCommentsInPrefix() = assertChanged(
        before = """
            a:
              b:
                d: # d-comment
                  e:
                    f: f-value # f-comment
                c:
                  # g-comment
                  g: 
                    h: h-value
        """,
        after = """
            a.b:
              d: # d-comment
                e.f: f-value # f-comment
              c:
                # g-comment
                g.h: h-value
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1142")
    fun doNotShiftYamlCommentsInPrefixFollowingPreviousYamlObject() = assertChanged(
        before = """
          a:
            b:
              c: c-value  # c-comment
              d: d-value # d-comment
              e: e-value   # e-comment
              f: f-value

              g: g-value
        """,
        after = """
            a.b:
              c: c-value  # c-comment
              d: d-value # d-comment
              e: e-value   # e-comment
              f: f-value

              g: g-value
        """
    )
    @Test
    fun doNotCoalesceDocumentsHavingAnchorsAndAliases() = assertUnchanged(
        before = """
            management:
                metrics:
                    &id enable.process.files: true
                endpoint:
                    health:
                        show-components: always
                        show-details: always
                        *id: false
        """
    )
}
