/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.ast

import com.netflix.rewrite.fields
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class LiteralTest(p: Parser): Parser by p {
    
    @Test
    fun literalField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals(0, literal.value)
        assertEquals(TypeTag.Int, literal.typeTag)
        assertEquals("0", literal.printTrimmed())
    }

    @Test
    fun literalCharacter() {
        val a = parse("""
            public class A {
                char c = 'a';
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals('a', literal.value)
        assertEquals(TypeTag.Char, literal.typeTag)
        assertEquals("'a'", literal.printTrimmed())
    }

    @Test
    fun literalNumerics() {
        val a = parse("""
            public class A {
                double d1 = 1.0d;
                double d2 = 1.0;
                long l1 = 1L;
                long l2 = 1;
            }
        """)

        val (d1, d2, l1, l2) = a.fields(0..3).map { it.vars[0].initializer as Tr.Literal }
        assertEquals("1.0d", d1.printTrimmed())
        assertEquals("1.0", d2.printTrimmed())
        assertEquals("1L", l1.printTrimmed())
        assertEquals("1", l2.printTrimmed())
    }

    @Test
    fun literalOctal() {
        val a = parse("""
            public class A {
                long l = 01L;
                byte b = 01;
                short s = 01;
                int i = 01;
                double d = 01;
                float f = 01;
            }
        """)

        a.fields(0..5).map { it.vars[0].initializer as Tr.Literal }.forEach {
            assertEquals("expected octal notation for ${it.typeTag}", "01", it.printTrimmed().trimEnd('L'))
        }
    }

    @Test
    fun literalBinary() {
        val a = parse("""
            public class A {
                long l = 0b10L;
                byte b = 0b10;
                short s = 0b10;
                int i = 0b10;
            }
        """)

        a.fields(0..3).map { it.vars[0].initializer as Tr.Literal }.forEach {
            assertEquals("expected binary notation for ${it.typeTag}", "0b10", it.printTrimmed().trimEnd('L'))
        }
    }

    @Test
    fun literalHex() {
        val a = parse("""
            public class A {
                long l = 0xA0L;
                byte b = 0xA0;
                short s = 0xA0;
                int i = 0xA0;
            }
        """)

        a.fields(0..3).map { it.vars[0].initializer as Tr.Literal }.forEach {
            assertEquals("expected hex notation for ${it.typeTag}", "0xA0", it.printTrimmed().trimEnd('L'))
        }
    }

    @Test
    fun transformString() {
        val a = parse("""
            public class A {
                String s = "foo ''";
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("\"foo\"", literal.transformValue<String> { it.substringBefore(' ') })
    }

    @Test
    fun nullLiteral() {
        val a = parse("""
            public class A {
                String s = null;
            }
        """)

        assertEquals("null", a.fields()[0].vars[0].initializer?.printTrimmed())
    }

    @Test
    fun transformLong() {
        val a = parse("""
            public class A {
                Long l = 2L;
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("4L", literal.transformValue<Long> { it * 2 })
    }

    @Test
    fun variationInSuffixCasing() {
        val a = parse("""
            public class A {
                Long l = 0l;
                Long m = 0L;
            }
        """)

        val (lower, upper) = a.fields(0..1).map { it.vars[0].initializer as Tr.Literal }

        assertEquals("0L", upper.printTrimmed())
        assertEquals("0l", lower.printTrimmed())
    }

    @Test
    fun escapedString() {
        val a = parse("""
            public class A {
                String s = "\"";
            }
        """)

        val s = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("\"\\\"\"", s.printTrimmed())
    }

    @Test
    fun escapedCharacter() {
        val a = parse("""
            public class A {
                char c = '\'';
            }
        """)

        val s = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("'\\''", s.printTrimmed())
    }
}