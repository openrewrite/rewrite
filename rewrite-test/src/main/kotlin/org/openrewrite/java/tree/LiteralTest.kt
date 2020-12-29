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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaParserTest
import org.openrewrite.java.JavaParserTest.NestingLevel.Block

interface LiteralTest : JavaParserTest {

    @Test
    fun literalField(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            int n = 0;
        """
    )

    @Test
    fun literalCharacter(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            char c = 'a';
        """
    )

    @Test
    fun literalNumerics(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            double d1 = 1.0d;
            double d2 = 1.0;
            long l1 = 1L;
            long l2 = 1;
        """
    )

    @Test
    fun literalOctal(jp: JavaParser) = assertParseAndPrint(
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
    fun literalBinary(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            long l = 0b10L;
            byte b = 0b10;
            short s = 0b10;
            int i = 0b10;
        """
    )

    @Test
    fun literalHex(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            long l = 0xA0L;
            byte b = 0xA0;
            short s = 0xA0;
            int i = 0xA0;
        """
    )

    @Test
    fun transformString(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            String s = "foo ''";
        """
    )

    @Test
    fun nullLiteral(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            String s = null;
        """
    )

    @Test
    fun transformLong(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            Long l = 2L;
        """
    )

    @Test
    fun variationInSuffixCasing(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            Long l = 0l;
            Long m = 0L;
        """
    )

    @Test
    fun escapedString(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            String s = "\"";
        """
    )

    @Test
    fun escapedCharacter(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            char c = '\'';
        """
    )
}
