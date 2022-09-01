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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.JavaTreeTest.NestingLevel.CompilationUnit

interface ImportTest : JavaTreeTest {

    @Test
    fun typeName(jp: JavaParser.Builder<*, *>) {
        val cu = jp.logCompilationWarningsAndErrors(true).build().parse("""
            import static java.util.Map.Entry;
            import java.util.Map.Entry;
            
            import java.util.List;
            import java.util.*;
            
            import static java.nio.charset.StandardCharsets.UTF_8;
            import static java.util.Collections.emptyList;
        """.trimIndent())[0]

        assertThat(cu.imports.map { it.typeName }).containsExactly(
            "java.util.Map${'$'}Entry",
            "java.util.Map${'$'}Entry",
            "java.util.List",
            "java.util.*",
            "java.nio.charset.StandardCharsets",
            "java.util.Collections"
        )
    }

    @Test
    fun packageName(jp: JavaParser) {
        val cu = jp.parse("""
            import static java.util.Map.Entry;
            import java.util.Map.Entry;
            
            import java.util.List;
            import java.util.*;
            
            import static java.nio.charset.StandardCharsets.UTF_8;
            import static java.util.Collections.emptyList;
        """.trimIndent())[0]

        assertThat(cu.imports.map { it.packageName }).containsExactly(
            "java.util",
            "java.util",
            "java.util",
            "java.util",
            "java.nio.charset",
            "java.util"
        )
    }

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

        val (b, c) = a.imports

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

        val (b, c) = a.imports

        assertTrue(b < c)
        assertTrue(c > b)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2156")
    @Test
    fun uppercasePackage(jp: JavaParser) {
        val cus = jp.parse("""
            package org.openrewrite.BadPackage;
            
            public class Foo {
                public static class Bar {
                }
            }
        """,
            """
            package org.openrewrite;
            
            import org.openrewrite.BadPackage.Foo;
            import org.openrewrite.BadPackage.Foo.Bar;
            
            public class Bar {
                private Foo foo; 
            }
            
        """)
        assertThat(cus[0].packageDeclaration!!.packageName).isEqualTo("org.openrewrite.BadPackage")
        assertThat(cus[1].imports[0].packageName).isEqualTo("org.openrewrite.BadPackage")
        assertThat(cus[1].imports[1].packageName).isEqualTo("org.openrewrite.BadPackage")
    }
}
