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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Block

interface LiteralTest : JavaTreeTest {

    @Test
    fun intentionallyBadUnicodeCharacter(jp: JavaParser) {
        assertParsePrintAndProcess(
            jp, Block, """
                String[] strings = new String[] { "\"\\u{U1}\"", "\"\\u1234\"", "\"\\u{00AUF}\"" };
            """
        )
    }

    @Test
    fun literalField(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            int n = 0;
        """
    )

    @Test
    fun literalCharacter(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            char c = 'a';
        """
    )

    @Test
    fun literalNumerics(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            double d1 = 1.0d;
            double d2 = 1.0;
            long l1 = 1L;
            long l2 = 1;
        """
    )

    @Test
    fun literalOctal(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            long l = 01L;
            byte b = 01;
            short s = 01;
            int i = 01;
            double d = 01;
            float f = 01;
        """
    )

    @Test
    fun literalBinary(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            long l = 0b10L;
            byte b = 0b10;
            short s = 0b10;
            int i = 0b10;
        """
    )

    @Test
    fun literalHex(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            long l = 0xA0L;
            byte b = 0xA0;
            short s = 0xA0;
            int i = 0xA0;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/405")
    @Test
    fun unmatchedSurrogatePair(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            char c1 = '\uD800';
            char c2 = '\uDfFf';
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/405")
    @Test
    fun unmatchedSurrogatePairInString(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            String s1 = "\uD800";
            String s2 = "\uDfFf";
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1387")
    @Test
    fun multipleUnicodeEscapeCharactersAtValueSourceIndex(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            String s1 = "A\ud83c\udf09";
            String s2 = "B\ud83c\udf09\ud83c\udf09";
            String s3 = "C\uDfFf D \ud83c\udf09\ud83c\udf09";
        """
    )

    @Test
    fun transformString(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            String s = "foo ''";
        """
    )

    @Test
    fun nullLiteral(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            String s = null;
        """
    )

    @Test
    fun transformLong(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            Long l = 2L;
        """
    )

    @Test
    fun variationInSuffixCasing(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            Long l = 0l;
            Long m = 0L;
        """
    )

    @Test
    fun escapedString(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            String s = "\"\t	\n";
        """
    )

    @Test
    fun escapedCharacter(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            char c = '\'';
            char tab = '	';
        """
    )
}
