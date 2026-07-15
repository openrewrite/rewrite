/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml;

import org.junit.jupiter.api.Test;
import org.openrewrite.toml.tree.Toml;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TomlPathsTest {

    private static Toml.Document parse(String src) {
        return (Toml.Document) new TomlParser().parse(src).findFirst().orElseThrow();
    }

    private static List<String> path(String... segments) {
        return Arrays.asList(segments);
    }

    @Test
    void findsKeyAtFlatDottedPath() {
        Toml.Document doc = parse("a.b.c.x = 1\n");
        Toml.KeyValue found = TomlPaths.findKeyValue(doc, path("a", "b", "c", "x"));
        assertThat(found).isNotNull();
        assertThat(((Toml.Literal) found.getValue()).getValue()).isEqualTo(1L);
    }

    @Test
    void findsKeyInsideStandardTable() {
        Toml.Document doc = parse("[a.b.c]\nx = 1\n");
        Toml.KeyValue found = TomlPaths.findKeyValue(doc, path("a", "b", "c", "x"));
        assertThat(found).isNotNull();
        assertThat(((Toml.Literal) found.getValue()).getValue()).isEqualTo(1L);
    }

    @Test
    void findsKeyInsideNestedHeaders() {
        Toml.Document doc = parse(
                "[a]\n" +
                        "[a.b]\n" +
                        "[a.b.c]\n" +
                        "x = 1\n");
        Toml.KeyValue found = TomlPaths.findKeyValue(doc, path("a", "b", "c", "x"));
        assertThat(found).isNotNull();
        assertThat(((Toml.Literal) found.getValue()).getValue()).isEqualTo(1L);
    }

    @Test
    void findsKeyWithMixedTableAndDottedKey() {
        Toml.Document doc = parse("[a.b]\nc.x = 1\n");
        Toml.KeyValue found = TomlPaths.findKeyValue(doc, path("a", "b", "c", "x"));
        assertThat(found).isNotNull();
        assertThat(((Toml.Literal) found.getValue()).getValue()).isEqualTo(1L);
    }

    @Test
    void findsKeyInsideInlineTable() {
        Toml.Document doc = parse("a = {b = {c = {x = 1}}}\n");
        Toml.KeyValue found = TomlPaths.findKeyValue(doc, path("a", "b", "c", "x"));
        assertThat(found).isNotNull();
        assertThat(((Toml.Literal) found.getValue()).getValue()).isEqualTo(1L);
    }

    @Test
    void quotedSegmentWithLiteralDotIsOneSegment() {
        Toml.Document doc = parse("site.\"google.com\" = true\n");
        Toml.KeyValue twoSeg = TomlPaths.findKeyValue(doc, path("site", "google.com"));
        assertThat(twoSeg).isNotNull();
        assertThat(((Toml.Literal) twoSeg.getValue()).getValue()).isEqualTo(true);

        Toml.KeyValue threeSeg = TomlPaths.findKeyValue(doc, path("site", "google", "com"));
        assertThat(threeSeg).isNull();
    }

    @Test
    void bareThreeSegmentDoesNotMatchTwoSegment() {
        Toml.Document doc = parse("site.google.com = true\n");
        assertThat(TomlPaths.findKeyValue(doc, path("site", "google.com"))).isNull();
        assertThat(TomlPaths.findKeyValue(doc, path("site", "google", "com"))).isNotNull();
    }

    @Test
    void returnsNullForMissingPath() {
        Toml.Document doc = parse("[a.b]\nc = 1\n");
        assertThat(TomlPaths.findKeyValue(doc, path("a", "b", "missing"))).isNull();
        assertThat(TomlPaths.findKeyValue(doc, path("nonexistent"))).isNull();
    }

    @Test
    void emptyPathReturnsNull() {
        Toml.Document doc = parse("a = 1\n");
        assertThat(TomlPaths.findKeyValue(doc, path())).isNull();
    }

    @Test
    void findTableMatchesExplicitHeader() {
        Toml.Document doc = parse(
                "[tool.poetry]\n" +
                        "name = \"x\"\n");
        Toml.Table found = TomlPaths.findTable(doc, path("tool", "poetry"));
        assertThat(found).isNotNull();
        assertThat(found.getName().getPath()).containsExactly("tool", "poetry");
    }

    @Test
    void findTableDoesNotMatchImplicitParent() {
        // [a.b] implicitly defines [a], but findTable only matches explicit headers
        Toml.Document doc = parse("[a.b]\nx = 1\n");
        assertThat(TomlPaths.findTable(doc, path("a"))).isNull();
        assertThat(TomlPaths.findTable(doc, path("a", "b"))).isNotNull();
    }

    @Test
    void findKeyValueIgnoresArrayTables() {
        Toml.Document doc = parse(
                "[[products]]\n" +
                        "name = \"Hammer\"\n");
        // Array tables are not searched — there's no way to disambiguate which element
        assertThat(TomlPaths.findKeyValue(doc, path("products", "name"))).isNull();
    }
}
