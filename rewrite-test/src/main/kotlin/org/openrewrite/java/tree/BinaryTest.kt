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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface BinaryTest {
    
    @Test
    fun arithmetic(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n = 0 + 1;
            }
        """)[0]
        
        val bin = a.classes[0].fields[0].vars[0].initializer as J.Binary
        assertTrue(bin.operator is J.Binary.Operator.Addition)
        assertTrue(bin.left is J.Literal)
        assertTrue(bin.right is J.Literal)
    }

    @Test
    fun format(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n = 0 + 1;
            }
        """)[0]

        val bin = a.classes[0].fields[0].vars[0].initializer as J.Binary
        assertEquals("0 + 1", bin.printTrimmed())
    }

    /**
     * String folding needs to be disabled in the parser to preserve the binary expression in the AST!
     * @see com.sun.tools.javac.parser.JavacParser.allowStringFolding
     */
    @Test
    fun formatFoldableStrings(jp: JavaParser) {
        val a = """
            public class A {
                String s = "a" + "b";
            }
        """

        assertEquals("\"a\" + \"b\"", jp.parse(a)[0].classes[0].fields[0].vars[0].initializer?.printTrimmed())
    }

    @Test
    fun endOfLineBreaks(jp: JavaParser) {
        val aSource = """
            import java.util.Objects;
            public class A {
                {
                    boolean b = Objects.equals(1, 2) //
                        && Objects.equals(3, 4) //
                        && Objects.equals(4, 5);
                }
            }
        """.trimIndent()

        val a = jp.parse(aSource)[0]

        assertEquals(aSource, a.print())
    }
}
