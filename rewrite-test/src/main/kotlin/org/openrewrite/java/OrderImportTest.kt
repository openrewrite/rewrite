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
package org.openrewrite.java

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

interface OrderImportTest {
    @Test
    fun orderImports(jp: JavaParser) {
        val a = jp.parse("""
            import static java.util.stream.Collectors.joining;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            import java.util.Set;
            import org.openrewrite.java.tree.JavaType;
            import org.openrewrite.java.tree.TypeUtils;
            
            public class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import org.openrewrite.java.tree.JavaType;
            import org.openrewrite.java.tree.TypeUtils;

            import java.util.ArrayList;
            import java.util.Objects;
            import java.util.Set;
            import java.util.regex.Pattern;

            import static java.util.stream.Collectors.joining;
            
            public class A {}
        """.trimIndent())
    }

    @Test
    fun blankLineThenEmptyBlockThenNonEmptyBlock(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.ArrayList;
            import java.util.Objects;

            import org.openrewrite.java.tree.JavaType;
            
            public class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import org.openrewrite.java.tree.JavaType;

            import java.util.ArrayList;
            import java.util.Objects;
            
            public class A {}
        """.trimIndent())
    }

    @Test
    fun foldIntoStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            import java.util.Set;
            import java.util.Map;
            
            public class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            import java.util.regex.Pattern;
            
            public class A {}
        """.trimIndent())
    }

    @Test
    fun blankLinesNotFollowedByBlockArentAdded(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            
            import static java.util.Collections.*;
            
            public class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            public class A {}
        """.trimIndent())
    }

    @Test
    fun foldIntoExistingStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            
            public class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            import java.util.regex.Pattern;
            
            public class A {}
        """.trimIndent())
    }

    @Test
    fun idempotence(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            import java.util.regex.Pattern;
            
            public class A {}
        """.trimIndent())

        val fix = a.refactor().visit(OrderImports.intellij().apply {
            setRemoveUnused(false)
        }).fix()
        println(fix.fixed.printTrimmed())
        Assertions.assertThat(fix.rulesThatMadeChanges).isEmpty()
    }

    @Test
    fun unfoldStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            
            public class A {
                List list;
                List list2;
            }
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij()).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            public class A {
                List list;
                List list2;
            }
        """.trimIndent())
    }

    @Test
    fun removeUnused(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            
            public class A {
            }
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij()).fix().fixed

        assertRefactored(fixed, """
            
            public class A {
            }
        """.trimIndent())
    }

    @Test
    fun unfoldStaticStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            
            import static java.util.Collections.*;
            
            public class A {
                List list = emptyList();
            }
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports.intellij()).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            import static java.util.Collections.emptyList;
            
            public class A {
                List list = emptyList();
            }
        """.trimIndent())
    }
}