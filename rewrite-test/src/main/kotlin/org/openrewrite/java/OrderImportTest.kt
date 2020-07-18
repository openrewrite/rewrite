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

import org.assertj.core.api.Assertions.assertThat
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

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
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

            class A {}
        """.trimIndent())
    }

    @Test
    fun blankLineThenEmptyBlockThenNonEmptyBlock(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.ArrayList;
            import java.util.Objects;

            import org.openrewrite.java.tree.JavaType;

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import org.openrewrite.java.tree.JavaType;

            import java.util.ArrayList;
            import java.util.Objects;

            class A {}
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

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            import java.util.regex.Pattern;

            class A {}
        """.trimIndent())
    }

    @Test
    fun blankLinesNotFollowedByBlockArentAdded(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;

            import static java.util.Collections.*;

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;

            import static java.util.Collections.*;

            class A {}
        """.trimIndent())
    }

    @Test
    fun foldIntoExistingStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            import java.util.regex.Pattern;

            class A {}
        """.trimIndent())
    }

    @Test
    fun idempotence(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            import java.util.regex.Pattern;

            class A {}
        """.trimIndent())

        val fix = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix()

        println(fix.fixed.printTrimmed())

        assertThat(fix.rulesThatMadeChanges).isEmpty()
    }

    @Test
    fun unfoldStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;

            class A {
                List list;
                List list2;
            }
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports()).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;

            class A {
                List list;
                List list2;
            }
        """.trimIndent())
    }

    @Test
    fun removeUnused(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;

            class A {
            }
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports()).fix().fixed

        assertRefactored(fixed, """

            class A {
            }
        """.trimIndent())
    }

    @Test
    fun unfoldStaticStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;

            import static java.util.Collections.*;

            class A {
                List list = emptyList();
            }
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports()).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;

            import static java.util.Collections.emptyList;

            class A {
                List list = emptyList();
            }
        """.trimIndent())
    }

    @Test
    fun packagePatternEscapesDots(jp: JavaParser) {
        val a = jp.parse("""
            import javax.annotation.Nonnull;

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import javax.annotation.Nonnull;

            class A {}
        """.trimIndent())
    }

    @Test
    fun twoImportsFollowedByStar(jp: JavaParser) {
        val a = jp.parse("""
            import java.io.IOException;
            import java.io.UncheckedIOException;
            import java.nio.files.*;

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertRefactored(fixed, """
            import java.io.IOException;
            import java.io.UncheckedIOException;
            import java.nio.files.*;

            class A {}
        """.trimIndent())
    }

    @Test
    fun springCloudFormat(jp: JavaParser) {
        val a = jp.parse("""
            import java.io.ByteArrayOutputStream;
            import java.nio.charset.StandardCharsets;
            import java.util.Collections;
            import java.util.zip.GZIPOutputStream;

            import javax.servlet.ReadListener;
            import javax.servlet.ServletInputStream;
            import javax.servlet.ServletOutputStream;

            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;

            import org.springframework.core.io.buffer.DataBuffer;
            import org.springframework.core.io.buffer.DataBufferFactory;
            import org.springframework.http.HttpHeaders;
            import org.springframework.util.MultiValueMap;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.server.ServerWebExchange;

            import static java.util.Arrays.stream;
            import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.toAsyncPredicate;

            class A {}
        """.trimIndent())

        val orderImports = OrderImports().apply {
            setLayout(OrderImports.Layout.builder(999, 999)
                    .importPackage("java.*")
                    .blankLine()
                    .importPackage("javax.*")
                    .blankLine()
                    .importAllOthers()
                    .blankLine()
                    .importPackage("org.springframework.*")
                    .blankLine()
                    .importStaticAllOthers()
                    .build())

            setRemoveUnused(false)
        }

        val fixed = a.refactor().visit(orderImports).fix().fixed

        assertRefactored(fixed, """
            import java.io.ByteArrayOutputStream;
            import java.nio.charset.StandardCharsets;
            import java.util.Collections;
            import java.util.zip.GZIPOutputStream;

            import javax.servlet.ReadListener;
            import javax.servlet.ServletInputStream;
            import javax.servlet.ServletOutputStream;

            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;

            import org.springframework.core.io.buffer.DataBuffer;
            import org.springframework.core.io.buffer.DataBufferFactory;
            import org.springframework.http.HttpHeaders;
            import org.springframework.util.MultiValueMap;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.server.ServerWebExchange;

            import static java.util.Arrays.stream;
            import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.toAsyncPredicate;

            class A {}
        """.trimIndent())
    }

    @Test
    fun importSorting(jp: JavaParser) {
        val a = jp.parse("""
            import r.core.Flux;
            import s.core.Flux;
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;

            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply {
            setRemoveUnused(false)
        }).fix().fixed

        assertThat(fixed.imports.map { it.packageName.substringBefore('.') })
                .containsExactly("com", "org", "r", "reactor", "s")
    }

    @Test
    fun foldGroupOfStaticImportsThatAppearLast(jp: JavaParser) {
        val a = jp.parse("""
            import static java.util.stream.Collectors.toList;
            import static java.util.stream.Collectors.toMap;
            import static java.util.stream.Collectors.toSet;
            
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(OrderImports().apply { setRemoveUnused(false) }).fix().fixed

        assertRefactored(fixed, """
            import static java.util.stream.Collectors.*;
            
            class A {}
        """.trimIndent())
    }
}
