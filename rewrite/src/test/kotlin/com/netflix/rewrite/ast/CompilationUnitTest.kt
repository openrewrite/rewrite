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

import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class CompilationUnitTest(p: Parser): Parser by p {
    
    @Test
    fun imports() {
        val a = parse("""
            import java.util.List;
            import java.io.*;
            public class A {}
        """)

        assertEquals(2, a.imports.size)
    }

    @Test
    fun classes() {
        val a = parse("""
            public class A {}
            class B{}
        """)

        assertEquals(2, a.classes.size)
    }
    
    @Test
    fun format() {
        val a = """
            |/* Comment */
            |package a;
            |import java.util.List;
            |
            |public class A { }
        """
        
        assertEquals(a.trimMargin(), parse(a).printTrimmed())
    }
}