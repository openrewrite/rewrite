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
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;

import static org.openrewrite.internal.StringUtils.*;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void containsOnlyWhitespaceAndCommentsTest() {
        assertThat(containsOnlyWhitespaceAndComments("")).isTrue();
        assertThat(containsOnlyWhitespaceAndComments(" \n\r\t")).isTrue();
        assertThat(containsOnlyWhitespaceAndComments(" // hello ")).isTrue();
        assertThat(containsOnlyWhitespaceAndComments("//")).isTrue();
        assertThat(containsOnlyWhitespaceAndComments("/**/")).isTrue();
        assertThat(containsOnlyWhitespaceAndComments("""
              /**
              asdf
              */
          """)).isTrue();

        assertThat(containsOnlyWhitespaceAndComments("a")).isFalse();
        assertThat(containsOnlyWhitespaceAndComments("""
              // hello
              goodbye
          """)).isFalse();
        assertThat(containsOnlyWhitespaceAndComments("a//")).isFalse();
        assertThat(containsOnlyWhitespaceAndComments(
          """
                /*
                */
                a
            """)).isFalse();
    }

    @Test
    void replaceFirst() {
        var result = StringUtils.replaceFirst("#{} Fred #{}", "#{}", "I am");
        assertThat(result).isEqualTo("I am Fred #{}");
        result = StringUtils.replaceFirst(result, "#{}", "surely.");
        assertThat(result).isEqualTo("I am Fred surely.");
        result = StringUtils.replaceFirst("#{}#{}#{}", "#{}", "yo");
        assertThat(result).isEqualTo("yo#{}#{}");
        result = StringUtils.replaceFirst(result, "#{}", "yo");
        assertThat(result).isEqualTo("yoyo#{}");
        result = StringUtils.replaceFirst(result, "#{}", "yo");
        assertThat(result).isEqualTo("yoyoyo");
        result = StringUtils.replaceFirst("Nothing to see here", "#{}", "nonsense");
        assertThat(result).isEqualTo("Nothing to see here");
        result = StringUtils.replaceFirst("Nothing to see here", "", "nonsense");
        assertThat(result).isEqualTo("Nothing to see here");
        result = StringUtils.replaceFirst("", "", "nonsense");
        assertThat(result).isEqualTo("");
    }

    @Test
    void occurrenceCount() {
        assertThat(countOccurrences("yoyoyoyoyo", "yo")).isEqualTo(5);
        assertThat(countOccurrences("yoyoyoyoyo", "yoyo")).isEqualTo(2);
        assertThat(countOccurrences("nonononono", "yo")).isEqualTo(0);
        assertThat(countOccurrences("", "")).isEqualTo(0);
    }

    @Test
    void globMatching() {
        assertThat(matchesGlob("expression", "expr*")).isTrue();
        assertThat(matchesGlob("some/xpath", "some/*")).isTrue();
        assertThat(matchesGlob("some/xpath/expression", "some/**")).isTrue();
        assertThat(matchesGlob("//some/xpath/expression", "**/xpath/*")).isTrue();
    }

    @SuppressWarnings("TextBlockMigration")
    @Test
    void greatestCommonMargin() {
        assertThat(StringUtils.greatestCommonMargin(
          "" +
          "  \n" +
          "   \n" +
          "     \n")).isEqualTo("  ");

        assertThat(StringUtils.greatestCommonMargin(
          "" +
          "   \n" +
          "  s \n" +
          "    \n")).isEqualTo("  ");

        assertThat(StringUtils.greatestCommonMargin("")).isEqualTo("");
        assertThat(StringUtils.greatestCommonMargin("\n\n")).isEqualTo("");
    }

    @Test
    void greatestCommonSubstringLength() {
        assertThat(StringUtils.greatestCommonSubstringLength("", "")).isEqualTo(0);
        assertThat(StringUtils.greatestCommonSubstringLength("abc", "def")).isEqualTo(0);
        assertThat(StringUtils.greatestCommonSubstringLength("abc1", "1")).isEqualTo(1);
    }

    @Test
    void allowConsecutiveLineBreaks() {
        assertThat(trimIndentPreserveCRLF("    \n    \n    a")).isEqualTo("\n\na");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2268")
    @Test
    void trailingCrlf() {
        assertThat(trimIndentPreserveCRLF("    \r\n    \r\n    a\r\n")).isEqualTo("\r\n\r\na");
    }
}
