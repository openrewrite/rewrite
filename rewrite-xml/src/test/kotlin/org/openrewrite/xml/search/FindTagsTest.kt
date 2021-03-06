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
package org.openrewrite.xml.search

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlRecipeTest

class FindTagsTest : XmlRecipeTest {

    @Test
    fun simpleElement() = assertChanged(
        parser,
        FindTags("/dependencies/dependency"),
        before = """
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
        """,
        after = """
            <dependencies>
                <!--~~>--><dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
        """
    )

    @Test
    fun wildcard() = assertChanged(
        parser,
        FindTags("/dependencies/*"),
        before = """
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
        """,
        after = """
            <dependencies>
                <!--~~>--><dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
        """
    )

    @Test
    fun noMatch() = assertUnchanged(
        parser,
        FindTags("/dependencies/dne"),
        before = """
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
        """
    )

    @Test
    fun staticFind() {
        @Language("xml") val before = """
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
        """
        val source = parser.parse(*(arrayOf(before.trimIndent()))).iterator().next()
        val matchingTags = FindTags.find(source, "/dependencies/dependency")
        assertThat(matchingTags).isNotNull.isNotEmpty
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = FindTags(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("xPath")

        recipe = FindTags("/dependencies/dependency")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
