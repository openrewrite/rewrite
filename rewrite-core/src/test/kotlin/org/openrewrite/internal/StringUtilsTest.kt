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
package org.openrewrite.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.internal.StringUtils.*

class StringUtilsTest {

    @Test
    fun trimIndentBlankLines() {
        val input = """
  
            class {
                
                A field;
            }
            """
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentEndLineNonWhitespace() {
        val input = """

            class {
                
                A field;
            }
            hello"""
        System.out.println("Kotlin : '" + input.trimIndent() + "'")

        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentFirstLineSameAsIndent() {
        val input = """
            
            class {
                
                A field;
            }
        """
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentFirstLineGreaterThanIndent() {
        val input = """
                
            class {
                
                A field;
            }
        """
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentFirstLineEmpty() {
        val input = """

            class {
                
                A field;
            }
        """
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentNoNewLine() {
        val input = " a"
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentOneCharacter() {
        val input = "a"
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentFirstLineNotEmpty() {
        val input = """
    fred
            class {
                
                A field;
            }
        """
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentFirstCharacterNotLineBreak() {
        val input = """fred
            class {
                
                A field;
            }
        """
        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentMinimalIndent() {
        val input = """
        class {
                A field;
            }
        """

        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentNoIndent() {
        val input = "class{\n   A field;\n}"

        assertThat(trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun containsOnlyWhitespaceAndCommentsTest() {
        assertThat(containsOnlyWhitespaceAndComments("")).isTrue
        assertThat(containsOnlyWhitespaceAndComments(" \n\r\t")).isTrue
        assertThat(containsOnlyWhitespaceAndComments(" // hello ")).isTrue
        assertThat(containsOnlyWhitespaceAndComments("//")).isTrue
        assertThat(containsOnlyWhitespaceAndComments("/**/")).isTrue
        assertThat(containsOnlyWhitespaceAndComments("""
            /**
            asdf
            */
        """)).isTrue

        assertThat(containsOnlyWhitespaceAndComments("a")).isFalse
        assertThat(containsOnlyWhitespaceAndComments("""
            // hello
            goodbye
        """)).isFalse
        assertThat(containsOnlyWhitespaceAndComments("a//")).isFalse
        assertThat(containsOnlyWhitespaceAndComments(
                """
            /*
            */
            a
        """)).isFalse
    }

    @Test
    fun replaceFirst() {
        var result = replaceFirst("#{} Fred #{}", "#{}", "I am")
        assertThat(result).isEqualTo("I am Fred #{}")
        result = replaceFirst(result, "#{}", "surely.")
        assertThat(result).isEqualTo("I am Fred surely.")
        result = replaceFirst("#{}#{}#{}", "#{}", "yo")
        assertThat(result).isEqualTo("yo#{}#{}")
        result = replaceFirst(result, "#{}", "yo")
        assertThat(result).isEqualTo("yoyo#{}")
        result = replaceFirst(result, "#{}", "yo")
        assertThat(result).isEqualTo("yoyoyo")
        result = replaceFirst("Nothing to see here", "#{}", "nonsense")
        assertThat(result).isEqualTo("Nothing to see here")
        result = replaceFirst("Nothing to see here", "", "nonsense")
        assertThat(result).isEqualTo("Nothing to see here")
        result = replaceFirst("", "", "nonsense")
        assertThat(result).isEqualTo("")
    }

    @Test
    fun occurrenceCount() {
        assertThat(countOccurrences("yoyoyoyoyo", "yo")).isEqualTo(5)
        assertThat(countOccurrences("yoyoyoyoyo", "yoyo")).isEqualTo(2)
        assertThat(countOccurrences("nonononono", "yo")).isEqualTo(0)
        assertThat(countOccurrences("", "")).isEqualTo(0)
    }

    @Test
    fun globMatching() {
        assertThat(matchesGlob("expression", "expr*")).isTrue
        assertThat(matchesGlob("some/xpath", "some/*")).isTrue
        assertThat(matchesGlob("some/xpath/expression", "some/**")).isTrue
        assertThat(matchesGlob("//some/xpath/expression", "**/xpath/*")).isTrue
    }

    @Test
    fun greatestCommonMargin() {
        assertThat(greatestCommonMargin("""
            |   
            |  
            |    
        """.trimMargin("|"))).isEqualTo("  ")

        assertThat(greatestCommonMargin("""
            |   
            |  s 
            |    
        """.trimMargin("|"))).isEqualTo("  ")

        assertThat(greatestCommonMargin("")).isEqualTo("")
        assertThat(greatestCommonMargin("\n\n")).isEqualTo("")
    }

    @Test
    fun greatestCommonSubstringLength() {
        assertThat(greatestCommonSubstringLength("", "")).isEqualTo(0)
        assertThat(greatestCommonSubstringLength("abc", "def")).isEqualTo(0)
        assertThat(greatestCommonSubstringLength("abc1", "1")).isEqualTo(1)
    }

    @Test
    fun allowConsecutiveLineBreaks() {
        assertThat(trimIndentPreserveCRLF("    \n    \n    a")).isEqualTo("\n\na")
    }
}
