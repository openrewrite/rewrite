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
package com.netflix.rewrite.tree

import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class BinaryTest(p: Parser): Parser by p {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.classes[0].fields[0].vars[0].initializer as Tr.Binary
        assertTrue(bin.operator is Tr.Binary.Operator.Addition)
        assertTrue(bin.left is Tr.Literal)
        assertTrue(bin.right is Tr.Literal)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)

        val bin = a.classes[0].fields[0].vars[0].initializer as Tr.Binary
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
