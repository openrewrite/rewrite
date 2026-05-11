/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal.template.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.GenericPatternNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.MatcherPatternNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.PatternTypeNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.TypeNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.TypeParameterNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.TypedPatternNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Correctness tests for the hand-rolled {@link TemplateParameterParser}.
 * Each input is mapped to its canonical stringified AST. These were
 * generated and validated against the previous ANTLR-based parser; they
 * now serve as a regression suite.
 */
class TemplateParameterParserTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
            input                                                    | expected
            int                                                      | Type(name=int, params=[], arrays=0)
            boolean                                                  | Type(name=boolean, params=[], arrays=0)
            String                                                   | Type(name=String, params=[], arrays=0)
            Object                                                   | Type(name=Object, params=[], arrays=0)
            T                                                        | Type(name=T, params=[], arrays=0)
            int[]                                                    | Type(name=int, params=[], arrays=1)
            byte[][]                                                 | Type(name=byte, params=[], arrays=2)
            java.lang.String[][][]                                   | Type(name=java.lang.String, params=[], arrays=3)
            java.util.List                                           | Type(name=java.util.List, params=[], arrays=0)
            java.util.List<java.lang.String>                         | Type(name=java.util.List, params=[Param(variance=null, type=Type(name=java.lang.String, params=[], arrays=0), wildcard=false)], arrays=0)
            java.util.Map<K, V>                                      | Type(name=java.util.Map, params=[Param(variance=null, type=Type(name=K, params=[], arrays=0), wildcard=false), Param(variance=null, type=Type(name=V, params=[], arrays=0), wildcard=false)], arrays=0)
            java.util.List<?>                                        | Type(name=java.util.List, params=[Param(variance=null, type=null, wildcard=true)], arrays=0)
            java.util.List<? extends java.lang.Number>               | Type(name=java.util.List, params=[Param(variance=EXTENDS, type=Type(name=java.lang.Number, params=[], arrays=0), wildcard=false)], arrays=0)
            java.util.List<? super java.lang.Integer>                | Type(name=java.util.List, params=[Param(variance=SUPER, type=Type(name=java.lang.Integer, params=[], arrays=0), wildcard=false)], arrays=0)
            java.util.Map<java.lang.String, int[]>                   | Type(name=java.util.Map, params=[Param(variance=null, type=Type(name=java.lang.String, params=[], arrays=0), wildcard=false), Param(variance=null, type=Type(name=int, params=[], arrays=1), wildcard=false)], arrays=0)
            java.util.List<java.lang.String>[]                       | Type(name=java.util.List, params=[Param(variance=null, type=Type(name=java.lang.String, params=[], arrays=0), wildcard=false)], arrays=1)
            """)
    void typeParses(String input, String expected) {
        assertThat(stringify(TemplateParameterParser.parseType(input.trim())))
                .isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
            input                                                       | expected
            n                                                           | matcher(paramName=n)
            map                                                         | matcher(paramName=map)
            any(int)                                                    | matcher(typed=Typed(name=null, patternType=PatternType(matcher=any, type=Type(name=int, params=[], arrays=0))))
            any()                                                       | matcher(typed=Typed(name=null, patternType=PatternType(matcher=any, type=null)))
            any(java.lang.Object)                                       | matcher(typed=Typed(name=null, patternType=PatternType(matcher=any, type=Type(name=java.lang.Object, params=[], arrays=0))))
            anyArray(int)                                               | matcher(typed=Typed(name=null, patternType=PatternType(matcher=anyArray, type=Type(name=int, params=[], arrays=0))))
            i:any(int)                                                  | matcher(typed=Typed(name=i, patternType=PatternType(matcher=any, type=Type(name=int, params=[], arrays=0))))
            args:anyArray(java.lang.Object)                             | matcher(typed=Typed(name=args, patternType=PatternType(matcher=anyArray, type=Type(name=java.lang.Object, params=[], arrays=0))))
            map:any(java.util.Map<? extends K, ? extends V>)            | matcher(typed=Typed(name=map, patternType=PatternType(matcher=any, type=Type(name=java.util.Map, params=[Param(variance=EXTENDS, type=Type(name=K, params=[], arrays=0), wildcard=false), Param(variance=EXTENDS, type=Type(name=V, params=[], arrays=0), wildcard=false)], arrays=0))))
            stringAssert:any(org.assertj.core.api.AbstractStringAssert<?>) | matcher(typed=Typed(name=stringAssert, patternType=PatternType(matcher=any, type=Type(name=org.assertj.core.api.AbstractStringAssert, params=[Param(variance=null, type=null, wildcard=true)], arrays=0))))
            """)
    void matcherPatternParses(String input, String expected) {
        assertThat(stringify(TemplateParameterParser.parseMatcherPattern(input.trim())))
                .isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
            input                                                                              | expected
            A                                                                                  | Generic(name=A, bounds=[])
            B extends C                                                                        | Generic(name=B, bounds=[Type(name=C, params=[], arrays=0)])
            T extends java.lang.Comparable<? super T>                                          | Generic(name=T, bounds=[Type(name=java.lang.Comparable, params=[Param(variance=SUPER, type=Type(name=T, params=[], arrays=0), wildcard=false)], arrays=0)])
            C extends java.lang.Comparable<? super B> & java.io.Serializable                   | Generic(name=C, bounds=[Type(name=java.lang.Comparable, params=[Param(variance=SUPER, type=Type(name=B, params=[], arrays=0), wildcard=false)], arrays=0), Type(name=java.io.Serializable, params=[], arrays=0)])
            """)
    void genericPatternParses(String input, String expected) {
        assertThat(stringify(TemplateParameterParser.parseGenericPattern(input.trim())))
                .isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // The full set of inputs the test suite needs to keep parsing — these
            // are sampled from `JavaTemplate.builder("...")` and placeholder usages
            // across rewrite-java, rewrite-java-test, and rewrite-java-tck.
            "any(java.lang.Number)",
            "any(java.lang.String)",
            "any(java.io.File)",
            "any(java.util.Locale)",
            "any(java.util.List)",
            "any(java.util.Set)",
            "any(java.util.Collection)",
            "any(java.util.List<String>)",
            "any(java.util.List<T>)",
            "any(java.util.Set<T>)",
            "any(java.lang.Iterable<T>)",
            "any(java.util.List<? extends java.lang.Number>)",
            "any(java.util.Map<K, V>)",
            "any(org.assertj.core.api.AbstractIterableAssert<?, ?, E, ?>)",
            "any(org.assertj.core.api.AbstractMapAssert<?, ?, K, V>)",
            "any(com.google.common.collect.Multimap<K, V>)",
            "any(java.util.stream.Stream<T>)",
            "anyArray(int)",
            "anyArray(java.util.List<String>)",
            "anyArray(java.lang.Object)",
            "format:any(java.lang.String)",
            "ps:any(java.io.PrintStream)",
            "items:anyArray(boolean)",
            "rest:anyArray(java.lang.Object)",
            "a:any(int[])",
            "a:any(java.lang.Object[])"
    })
    void roundTripsRepresentativeInputs(String input) {
        // Ensures every input from real-world JavaTemplate usage parses without throwing.
        assertThat(TemplateParameterParser.parseMatcherPattern(input)).isNotNull();
    }

    @Test
    void rejectsUnknownCharacter() {
        assertThatThrownBy(() -> TemplateParameterParser.parseType("foo!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTrailingTokens() {
        assertThatThrownBy(() -> TemplateParameterParser.parseType("int extra"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnclosedAngleBracket() {
        assertThatThrownBy(() -> TemplateParameterParser.parseType("java.util.List<String"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingMatcherParen() {
        assertThatThrownBy(() -> TemplateParameterParser.parseMatcherPattern("any int"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyInput() {
        assertThatThrownBy(() -> TemplateParameterParser.parseMatcherPattern(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseMatcherPatternReturnsCachedInstance() {
        // Each call site repeatedly parses the same placeholder content, so
        // the parser caches results by input string. Same input → same instance.
        MatcherPatternNode first = TemplateParameterParser.parseMatcherPattern("any(java.lang.Object)");
        MatcherPatternNode second = TemplateParameterParser.parseMatcherPattern("any(java.lang.Object)");
        assertThat(second).isSameAs(first);
    }

    @Test
    void parseGenericPatternReturnsCachedInstance() {
        GenericPatternNode first = TemplateParameterParser.parseGenericPattern("T extends java.lang.Comparable<? super T>");
        GenericPatternNode second = TemplateParameterParser.parseGenericPattern("T extends java.lang.Comparable<? super T>");
        assertThat(second).isSameAs(first);
    }

    // -- Stringification helpers (canonical S-expression-like form) -------

    private static String stringify(MatcherPatternNode m) {
        StringBuilder sb = new StringBuilder();
        sb.append("matcher(");
        if (m.typedPattern() != null) {
            sb.append("typed=");
            stringify(sb, m.typedPattern());
        } else {
            sb.append("paramName=").append(m.parameterName());
        }
        sb.append(")");
        return sb.toString();
    }

    private static void stringify(StringBuilder sb, TypedPatternNode t) {
        sb.append("Typed(name=").append(t.parameterName()).append(", patternType=");
        stringify(sb, t.patternType());
        sb.append(")");
    }

    private static void stringify(StringBuilder sb, PatternTypeNode p) {
        sb.append("PatternType(matcher=").append(p.matcherName()).append(", type=");
        if (p.type() == null) {
            sb.append("null");
        } else {
            stringify(sb, p.type());
        }
        sb.append(")");
    }

    private static String stringify(TypeNode type) {
        StringBuilder sb = new StringBuilder();
        stringify(sb, type);
        return sb.toString();
    }

    private static void stringify(StringBuilder sb, TypeNode type) {
        sb.append("Type(name=").append(type.typeName());
        sb.append(", params=[");
        boolean first = true;
        for (TypeParameterNode p : type.typeParameters()) {
            if (!first) {
                sb.append(", ");
            }
            stringify(sb, p);
            first = false;
        }
        sb.append("], arrays=").append(type.arrayDepth()).append(")");
    }

    private static void stringify(StringBuilder sb, TypeParameterNode p) {
        sb.append("Param(variance=").append(p.variance());
        sb.append(", type=");
        if (p.type() == null) {
            sb.append("null");
        } else {
            stringify(sb, p.type());
        }
        sb.append(", wildcard=").append(p.isWildcard()).append(")");
    }

    private static String stringify(GenericPatternNode g) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generic(name=").append(g.genericName());
        sb.append(", bounds=[");
        boolean first = true;
        for (TypeNode b : g.bounds()) {
            if (!first) {
                sb.append(", ");
            }
            stringify(sb, b);
            first = false;
        }
        sb.append("])");
        return sb.toString();
    }
}
