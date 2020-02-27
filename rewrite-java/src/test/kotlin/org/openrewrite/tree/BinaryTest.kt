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
package org.openrewrite.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Parser

open class BinaryTest : Parser() {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.classes[0].fields[0].vars[0].initializer as J.Binary
        assertTrue(bin.operator is J.Binary.Operator.Addition)
        assertTrue(bin.left is J.Literal)
        assertTrue(bin.right is J.Literal)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)

        val bin = a.classes[0].fields[0].vars[0].initializer as J.Binary
        assertEquals("0 + 1", bin.printTrimmed())
    }

    /**
     * String folding needs to be disabled in the parser to preserve the binary expression in the AST!
     * @see com.sun.tools.javac.parser.JavacParser.allowStringFolding
     */
    @Test
    fun formatFoldableStrings() {
        val a = """
            public class A {
                String s = "a" + "b";
            }
        """

        assertEquals("\"a\" + \"b\"", parse(a).classes[0].fields[0].vars[0].initializer?.printTrimmed())
    }
}
