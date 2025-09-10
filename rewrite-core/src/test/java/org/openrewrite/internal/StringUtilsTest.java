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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.internal.StringUtils.*;

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
        // exact matches
        assertThat(matchesGlob("test", null)).isFalse();
        assertThat(matchesGlob("test", "")).isFalse();
        assertThat(matchesGlob("", "")).isTrue();
        assertThat(matchesGlob("test", "test")).isTrue();

        // matches with ?'s
        assertThat(matchesGlob("test", "t?st")).isTrue();
        assertThat(matchesGlob("test", "??st")).isTrue();
        assertThat(matchesGlob("test", "tes?")).isTrue();
        assertThat(matchesGlob("test", "te??")).isTrue();
        assertThat(matchesGlob("test", "?es?")).isTrue();
        assertThat(matchesGlob("tes", "tes?")).isFalse();
        assertThat(matchesGlob("testt", "tes?")).isFalse();
        assertThat(matchesGlob("tsst", "tes?")).isFalse();

        // matches with *
        assertThat(matchesGlob("test", "*")).isTrue();
        assertThat(matchesGlob("test", "test*")).isTrue();
        assertThat(matchesGlob("testTest", "test*")).isTrue();
        assertThat(matchesGlob("test", "*test")).isTrue();
        assertThat(matchesGlob("testtest", "*test")).isTrue();
        assertThat(matchesGlob("testtest", "test*test")).isTrue();
        assertThat(matchesGlob("testtest", "tes*est")).isTrue();

        // Exhaustive cases
        assertThat(matchesGlob("testaaatestaaatest", "test*test*test")).isTrue();
        assertThat(matchesGlob("bestabatest", "?est*test")).isTrue();
        assertThat(matchesGlob("testabctestabctesabctest", "test*test*test")).isTrue();
        assertThat(matchesGlob("test.txt", "*.txt")).isTrue();
        assertThat(matchesGlob("test.txt", "test.*")).isTrue();
        assertThat(matchesGlob("test.txt", "*.*")).isTrue();
        assertThat(matchesGlob("test.test.txt", "*.*")).isTrue();
    }

    @SuppressWarnings("TextBlockMigration")
    @Test
    void greatestCommonMargin() {
        assertThat(StringUtils.greatestCommonMargin(
          """
           \s
            \s
              \s
          """)).isEqualTo("  ");

        assertThat(StringUtils.greatestCommonMargin(
          """
            \s
            s\s
             \s
          """)).isEqualTo("  ");

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
