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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface ImportTest : JavaTreeTest {

    @Test
    fun classImport(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.List;
            public class A {}
        """
    )

    @Test
    fun starImport(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.*;
            public class A {}
        """
    )

    @Test
    fun compare(jp: JavaParser) {
        val a = jp.parse(
            """
            import b.B;
            import c.c.C;
        """.trimIndent()
        )[0]

        val (b, c) = a.imports.map { it.elem }

        assertTrue(b < c)
        assertTrue(c > b)
    }

    @Test
    fun compareSamePackageDifferentNameLengths(jp: JavaParser) {
        val a = jp.parse(
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
        """.trimIndent()
        )[0]

        val (b, c) = a.imports.map { it.elem }

        assertTrue(b < c)
        assertTrue(c > b)
    }
}
