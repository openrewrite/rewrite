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

import static org.assertj.core.api.Assertions.assertThat;

class SemanticallyEqualTest {

    @Test
    void literalsAreEqual() {
        String toml = "key = \"value\"";
        Toml.Document doc1 = parseToml(toml);
        Toml.Document doc2 = parseToml(toml);

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void literalsWithDifferentWhitespaceAreEqual() {
        Toml.Document doc1 = parseToml("key = \"value\"");
        Toml.Document doc2 = parseToml("key=\"value\"");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void differentLiteralsAreNotEqual() {
        Toml.Document doc1 = parseToml("key = \"value1\"");
        Toml.Document doc2 = parseToml("key = \"value2\"");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void arraysAreEqual() {
        String toml = "key = [1, 2, 3]";
        Toml.Document doc1 = parseToml(toml);
        Toml.Document doc2 = parseToml(toml);

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void arraysWithDifferentValuesAreNotEqual() {
        Toml.Document doc1 = parseToml("key = [1, 2, 3]");
        Toml.Document doc2 = parseToml("key = [1, 2, 4]");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void arraysWithDifferentLengthsAreNotEqual() {
        Toml.Document doc1 = parseToml("key = [1, 2, 3]");
        Toml.Document doc2 = parseToml("key = [1, 2]");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void tablesAreEqual() {
        String toml = """
            [[package.contributors]]
            name = "Alice"
            email = "alice@example.com"
            """;
        Toml.Document doc1 = parseToml(toml);
        Toml.Document doc2 = parseToml(toml);

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void tablesWithDifferentValuesAreNotEqual() {
        Toml.Document doc1 = parseToml("""
            [[package.contributors]]
            name = "Alice"
            email = "alice@example.com"
            """);
        Toml.Document doc2 = parseToml("""
            [[package.contributors]]
            name = "Alice"
            email = "bob@example.com"
            """);

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void multipleTablesAreEqual() {
        String toml = """
            [[package.contributors]]
            name = "Alice"

            [[package.contributors]]
            name = "Bob"
            """;
        Toml.Document doc1 = parseToml(toml);
        Toml.Document doc2 = parseToml(toml);

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void identicalReferencesAreEqual() {
        Toml.Document doc = parseToml("key = \"value\"");
        assertThat(SemanticallyEqual.areEqual(doc, doc)).isTrue();
    }

    @Test
    void nullLiteralsAreEqual() {
        // Both documents have empty values
        Toml.Document doc1 = parseToml("");
        Toml.Document doc2 = parseToml("");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void differentTypesAreNotEqual() {
        Toml.Document doc1 = parseToml("key = \"value\"");
        Toml.Document doc2 = parseToml("key = 123");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void booleanLiteralsAreEqual() {
        Toml.Document doc1 = parseToml("enabled = true");
        Toml.Document doc2 = parseToml("enabled = true");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void booleanAndStringLiteralsAreNotEqual() {
        Toml.Document doc1 = parseToml("enabled = \"true\"");
        Toml.Document doc2 = parseToml("enabled = true");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void differentBooleanLiteralsAreNotEqual() {
        Toml.Document doc1 = parseToml("enabled = true");
        Toml.Document doc2 = parseToml("enabled = false");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void numericLiteralsAreEqual() {
        Toml.Document doc1 = parseToml("port = 8080");
        Toml.Document doc2 = parseToml("port = 8080");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void differentNumericLiteralsAreNotEqual() {
        Toml.Document doc1 = parseToml("port = 8080");
        Toml.Document doc2 = parseToml("port = 8081");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    @Test
    void nestedArraysAreEqual() {
        String toml = "matrix = [[1, 2], [3, 4]]";
        Toml.Document doc1 = parseToml(toml);
        Toml.Document doc2 = parseToml(toml);

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isTrue();
    }

    @Test
    void nestedArraysWithDifferentValuesAreNotEqual() {
        Toml.Document doc1 = parseToml("matrix = [[1, 2], [3, 4]]");
        Toml.Document doc2 = parseToml("matrix = [[1, 2], [3, 5]]");

        assertThat(SemanticallyEqual.areEqual(doc1, doc2)).isFalse();
    }

    private Toml.Document parseToml(String content) {
        return new TomlParser()
                .parse(content)
                .findFirst()
                .map(Toml.Document.class::cast)
                .orElseThrow(() -> new RuntimeException("Failed to parse TOML: " + content));
    }
}
