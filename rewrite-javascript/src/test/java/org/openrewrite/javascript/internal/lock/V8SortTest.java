/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.EcmaScript.HOLE;
import static org.openrewrite.javascript.internal.lock.EcmaScript.UNDEFINED;

/**
 * V8's own test/mjsunit/array-sort.js where it applies to a list and a comparator, and Node v24.19.0's sort over
 * generated arrays and comparators ({@code v8sort/generate/sort-cases.js}), call for call.
 */
class V8SortTest {

    @Test
    void numbersByAComparator() {
        List<Integer> a = new ArrayList<>(Arrays.asList(200, 45, 7));
        V8Sort.sort(a, (x, y) -> x - y);
        assertThat(a).containsExactly(7, 45, 200);
    }

    @Test
    void strings() {
        List<String> a = new ArrayList<>(Arrays.asList("cc", "c", "aa", "a", "bb", "b", "ab", "ac"));
        V8Sort.sort(a, String::compareTo);
        assertThat(a).containsExactly("a", "aa", "ab", "ac", "b", "bb", "c", "cc");
    }

    @Test
    void objectsByTheirStrings() {
        Object obj0 = named("a");
        Object obj1 = named("b");
        Object obj2 = named("c");
        List<Object> a = new ArrayList<>(Arrays.asList(obj2, obj0, obj1));
        V8Sort.sort(a, Comparator.comparing(Object::toString));
        assertThat(a).containsExactly(obj0, obj1, obj2);
    }

    @Test
    void holesAreCompactedToTheEnd() {
        List<Object> a = new ArrayList<>(Collections.nCopies(11, HOLE));
        a.set(4, "18");
        a.set(10, "12");
        V8Sort.sort(a, Comparator.comparing(Object::toString));
        assertThat(a).hasSize(11);
        assertThat(a.subList(0, 2)).containsExactly("12", "18");
        assertThat(a.subList(2, 11)).containsOnly(HOLE);
    }

    @Test
    void undefinedSortsLastWithoutBeingCompared() {
        List<Object> a = new ArrayList<>(Arrays.asList(3, UNDEFINED, 2));
        V8Sort.sort(a, (x, y) -> {
            assertThat(x).isNotSameAs(UNDEFINED);
            assertThat(y).isNotSameAs(UNDEFINED);
            return (Integer) x - (Integer) y;
        });
        assertThat(a).containsExactly(2, 3, UNDEFINED);
    }

    @Test
    void anUnsoundComparatorKeepsTheElements() {
        List<Object> a = new ArrayList<>(Arrays.asList(3, UNDEFINED, 2));
        V8Sort.sort(a, (x, y) -> 1);
        V8Sort.sort(a, (x, y) -> (Integer) x - (Integer) y);
        assertThat(a).containsExactly(2, 3, UNDEFINED);
    }

    @Test
    void theComparatorSeesOnlyTheArraysElements() {
        List<double[]> a = new ArrayList<>();
        for (double v : new double[]{1, 2, 4, 8, 16, 32, 64, 128, 256, -0.0}) {
            a.add(new double[]{v});
        }
        List<double[]> elements = new ArrayList<>(a);
        V8Sort.sort(a, (x, y) -> {
            assertThat(elements).anySatisfy(e -> assertThat(e).isSameAs(x));
            assertThat(elements).anySatisfy(e -> assertThat(e).isSameAs(y));
            return (int) Math.signum(x[0] - y[0]);
        });
        assertThat(a.get(0)[0]).isEqualTo(-0.0);
        assertThat(a.get(9)[0]).isEqualTo(256);
    }

    @Test
    void longArraysOfEqualElementsKeepTheirOrder() {
        for (int n : new int[]{22, 2000}) {
            List<Object> a = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                a.add(new Object());
            }
            List<Object> before = new ArrayList<>(a);
            V8Sort.sort(a, (x, y) -> 0);
            assertThat(a).containsExactlyElementsOf(before);
        }
    }

    @ParameterizedTest(name = "{0} {1} n={2} seed={3}")
    @MethodSource("cases")
    void sortsAsNode(String comparator, String pattern, int n, long seed, String input, String output, int calls) {
        List<Object> a = new ArrayList<>();
        List<String> tokens = tokens(input);
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            a.add("u".equals(token) ? UNDEFINED : "h".equals(token) ? HOLE :
                    new Element(i, Integer.parseInt(token)));
        }
        int[] count = new int[1];
        Comparator<Element> compare = comparator(comparator, xorshift((int) seed));
        V8Sort.sort(a, (x, y) -> {
            count[0]++;
            return compare.compare((Element) x, (Element) y);
        });

        List<String> sorted = new ArrayList<>();
        for (Object e : a) {
            sorted.add(e == UNDEFINED ? "u" : e == HOLE ? "h" : String.valueOf(((Element) e).index));
        }
        assertThat(String.join(",", sorted)).isEqualTo(output);
        assertThat(count[0]).as("comparator calls").isEqualTo(calls);
    }

    static Stream<Arguments> cases() {
        InputStream gzipped = Objects.requireNonNull(V8SortTest.class.getResourceAsStream("v8sort/sort-cases.tsv.gz"));
        List<Arguments> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(gzipped),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = line.split("\t", -1);
                rows.add(Arguments.of(f[0], f[1], tokens(f[3]).size(), Long.parseLong(f[2]), f[3], f[4],
                        Integer.parseInt(f[5])));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows.stream();
    }

    private static Comparator<Element> comparator(String name, IntSupplier next) {
        switch (name) {
            case "random":
                return (a, b) -> Integer.remainderUnsigned(next.getAsInt(), 3) - 1;
            case "low":
                return (a, b) -> Integer.remainderUnsigned(next.getAsInt(), 10) < 8 ? -1 : 1;
            case "high":
                return (a, b) -> Integer.remainderUnsigned(next.getAsInt(), 10) < 8 ? 1 : -1;
            case "values":
                return (a, b) -> a.value - b.value;
            case "noisy":
                return (a, b) -> {
                    if (Integer.remainderUnsigned(next.getAsInt(), 50) == 0) {
                        return b.value - a.value == 0 ? 1 : b.value - a.value;
                    }
                    return a.value - b.value;
                };
            default:
                throw new IllegalArgumentException(name);
        }
    }

    private static IntSupplier xorshift(int seed) {
        int[] x = {seed == 0 ? 1 : seed};
        return () -> {
            x[0] ^= x[0] << 13;
            x[0] ^= x[0] >>> 17;
            x[0] ^= x[0] << 5;
            return x[0];
        };
    }

    private static List<String> tokens(String list) {
        return list.isEmpty() ? Collections.emptyList() : Arrays.asList(list.split(","));
    }

    private static Object named(String name) {
        return new Object() {
            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static final class Element {
        final int index;
        final int value;

        Element(int index, int value) {
            this.index = index;
            this.value = value;
        }
    }
}
