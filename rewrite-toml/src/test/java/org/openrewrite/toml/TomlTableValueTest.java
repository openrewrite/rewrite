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
package org.openrewrite.toml;

import org.junit.jupiter.api.Test;
import org.openrewrite.toml.tree.Toml;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TomlTableValueTest {

    @Test
    void findsExistingKeysAndReadsStringValues() {
        Toml.Table table = table("{ group = \"org.example\", name = \"library\", count = 2 }");

        assertThat(TomlTableValue.has(table, "name")).isTrue();
        assertThat(TomlTableValue.has(table, "count")).isTrue();
        assertThat(TomlTableValue.has(table, "missing")).isFalse();
        assertThat(TomlTableValue.getString(table, "name")).isEqualTo("library");
        assertThat(TomlTableValue.getString(table, "missing")).isNull();
        assertThat(TomlTableValue.getString(table, "count")).isNull();
    }

    @Test
    void replacesStringValuesAndPreservesQuoteStyleAndFormatting() {
        Toml.Document document = document(
                "library = { group = \"org.example\", name = 'old' } # keep this comment\n"
        );
        Toml.Table table = table(document);

        Toml.Table updated = TomlTableValue.withString(table, "name", "new");

        assertThat(literal(updated, "name"))
                .returns("new", Toml.Literal::getValue)
                .returns("'new'", Toml.Literal::getSource);
        assertThat(print(withTableValue(document, updated)))
                .isEqualTo("library = { group = \"org.example\", name = 'new' } # keep this comment\n");
    }

    @Test
    void doesNotReplaceNonStringValues() {
        Toml.Document document = document("library = { count = 2 }\n");
        Toml.Table table = table(document);

        Toml.Table updated = TomlTableValue.withString(table, "count", "new");

        assertThat(literal(updated, "count"))
                .returns(2L, Toml.Literal::getValue)
                .returns("2", Toml.Literal::getSource);
        assertThat(print(withTableValue(document, updated))).isEqualTo("library = { count = 2 }\n");
    }

    @Test
    void doesNotAddMissingValueWithString() {
        Toml.Document document = document("library = { group = \"org.example\" }\n");
        Toml.Table table = table(document);

        Toml.Table updated = TomlTableValue.withString(table, "name", "new");

        assertThat(updated.getValues())
                .extracting(value -> keyName((Toml.KeyValue) value))
                .containsExactly("group");
        assertThat(print(withTableValue(document, updated)))
                .isEqualTo("library = { group = \"org.example\" }\n");
    }

    @Test
    void doesNotReplaceExistingNonStringValueWithStringOrAdd() {
        Toml.Document document = document("library = { count = 2 }\n");
        Toml.Table table = table(document);

        Toml.Table updated = TomlTableValue.withStringOrAdd(table, "count", "new");

        assertThat(literal(updated, "count"))
                .returns(2L, Toml.Literal::getValue)
                .returns("2", Toml.Literal::getSource);
        assertThat(print(withTableValue(document, updated))).isEqualTo("library = { count = 2 }\n");
    }

    @Test
    void replacesExistingValueWithStringOrAdd() {
        Toml.Document document = document("library = { group = \"org.example\", name = \"old\" }\n");
        Toml.Table table = table(document);

        Toml.Table updated = TomlTableValue.withStringOrAdd(table, "name", "new");

        assertThat(updated.getValues())
                .extracting(value -> keyName((Toml.KeyValue) value))
                .containsExactly("group", "name");
        assertThat(literal(updated, "name"))
                .returns("new", Toml.Literal::getValue)
                .returns("\"new\"", Toml.Literal::getSource);
        assertThat(print(withTableValue(document, updated)))
                .isEqualTo("library = { group = \"org.example\", name = \"new\" }\n");
    }

    @Test
    void addsMissingValueWithStringOrAddAndPreservesExistingPadding() {
        Toml.Document document = document("library = { group = \"org.example\", name = \"old\" }\n");
        Toml.Table table = table(document);

        Toml.Table updated = TomlTableValue.withStringOrAdd(table, "version", "1.0.0");

        assertThat(updated.getValues())
                .extracting(value -> keyName((Toml.KeyValue) value))
                .containsExactly("group", "name", "version");
        assertThat(literal(updated, "version"))
                .returns("1.0.0", Toml.Literal::getValue)
                .returns("\"1.0.0\"", Toml.Literal::getSource);
        assertThat(print(withTableValue(document, updated)))
                .isEqualTo("library = { group = \"org.example\", name = \"old\", version = \"1.0.0\" }\n");
    }

    private static Toml.Table table(String source) {
        return table(document("library = " + source + "\n"));
    }

    private static Toml.Table table(Toml.Document document) {
        return (Toml.Table) ((Toml.KeyValue) document.getValues().getFirst()).getValue();
    }

    private static Toml.Literal literal(Toml.Table table, String key) {
        return (Toml.Literal) keyValue(table, key).getValue();
    }

    private static Toml.KeyValue keyValue(Toml.Table table, String key) {
        return table.getValues().stream()
                .map(Toml.KeyValue.class::cast)
                .filter(keyValue -> key.equals(keyName(keyValue)))
                .findFirst()
                .orElseThrow();
    }

    private static String keyName(Toml.KeyValue keyValue) {
        return ((Toml.Identifier) keyValue.getKey()).getName();
    }

    private static Toml.Document withTableValue(Toml.Document document, Toml.Table table) {
        Toml.KeyValue keyValue = (Toml.KeyValue) document.getValues().getFirst();
        return document.withValues(List.of(keyValue.withValue(table)));
    }

    private static Toml.Document document(String source) {
        return (Toml.Document) new TomlParser().parse(source).findFirst().orElseThrow();
    }

    private static String print(Toml.Document document) {
        return document.printAll();
    }
}
