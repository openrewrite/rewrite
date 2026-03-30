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

import org.jspecify.annotations.Nullable;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlKey;

import java.util.List;

/**
 * Recursively check the equality of TOML elements.
 * Ignores whitespace and formatting differences.
 */
public class SemanticallyEqual {

    private SemanticallyEqual() {
    }

    public static boolean areEqual(Toml first, Toml second) {
        SemanticallyEqualVisitor sme = new SemanticallyEqualVisitor();
        sme.visit(first, second);
        return sme.areEqual;
    }

    @SuppressWarnings("ConstantConditions")
    private static class SemanticallyEqualVisitor extends TomlVisitor<Toml> {

        boolean areEqual = true;

        @Override
        public Toml visitDocument(Toml.Document document, Toml other) {
            if (document == other) {
                return null;
            }
            if (!(other instanceof Toml.Document)) {
                areEqual = false;
                return null;
            }
            Toml.Document otherDocument = (Toml.Document) other;
            visitList(document.getValues(), otherDocument.getValues());
            return null;
        }

        @Override
        public Toml visitTable(Toml.Table table, Toml other) {
            if (table == other) {
                return null;
            }
            if (!(other instanceof Toml.Table)) {
                areEqual = false;
                return null;
            }
            Toml.Table otherTable = (Toml.Table) other;

            // Compare table names
            if (!keyEquals(table.getName(), otherTable.getName())) {
                areEqual = false;
                return null;
            }

            // Compare table values
            visitList(table.getValues(), otherTable.getValues());
            return null;
        }

        @Override
        public Toml visitKeyValue(Toml.KeyValue keyValue, Toml other) {
            if (keyValue == other) {
                return null;
            }
            if (!(other instanceof Toml.KeyValue)) {
                areEqual = false;
                return null;
            }
            Toml.KeyValue otherKeyValue = (Toml.KeyValue) other;

            // Compare keys
            if (!keyEquals(keyValue.getKey(), otherKeyValue.getKey())) {
                areEqual = false;
                return null;
            }

            // Compare values
            visit(keyValue.getValue(), otherKeyValue.getValue());
            return null;
        }

        @Override
        public Toml visitLiteral(Toml.Literal literal, Toml other) {
            if (literal == other) {
                return null;
            }
            if (!(other instanceof Toml.Literal)) {
                areEqual = false;
                return null;
            }
            Toml.Literal otherLiteral = (Toml.Literal) other;

            if (literal.getType() != otherLiteral.getType()) {
                areEqual = false;
                return null;
            }

            Object val1 = literal.getValue();
            Object val2 = otherLiteral.getValue();

            if (val1 == null && val2 == null) {
                return null;
            }
            if (val1 == null || val2 == null) {
                areEqual = false;
                return null;
            }

            // Compare values using toString for consistent comparison
            if (!val1.toString().equals(val2.toString())) {
                areEqual = false;
            }

            return null;
        }

        @Override
        public Toml visitArray(Toml.Array array, Toml other) {
            if (array == other) {
                return null;
            }
            if (!(other instanceof Toml.Array)) {
                areEqual = false;
                return null;
            }
            Toml.Array otherArray = (Toml.Array) other;

            List<Toml> values1 = array.getValues();
            List<Toml> values2 = otherArray.getValues();

            if (values1.size() != values2.size()) {
                areEqual = false;
                return null;
            }

            visitList(values1, values2);
            return null;
        }

        @Override
        public Toml visitIdentifier(Toml.Identifier identifier, Toml other) {
            if (identifier == other) {
                return null;
            }
            if (!(other instanceof Toml.Identifier)) {
                areEqual = false;
                return null;
            }
            Toml.Identifier otherIdentifier = (Toml.Identifier) other;

            if (!identifier.getName().equals(otherIdentifier.getName())) {
                areEqual = false;
            }

            return null;
        }

        @Override
        public Toml visitEmpty(Toml.Empty empty, Toml other) {
            if (empty == other) {
                return null;
            }
            if (!(other instanceof Toml.Empty)) {
                areEqual = false;
            }
            return null;
        }

        private void visitList(@Nullable List<? extends Toml> list1, @Nullable List<? extends Toml> list2) {
            if (!areEqual) {
                return;
            }

            if (list1 == null && list2 == null) {
                return;
            }
            if (list1 == null || list2 == null || list1.size() != list2.size()) {
                areEqual = false;
                return;
            }

            for (int i = 0; i < list1.size(); i++) {
                visit(list1.get(i), list2.get(i));
                if (!areEqual) {
                    return;
                }
            }
        }

        private boolean keyEquals(@Nullable TomlKey key1, @Nullable TomlKey key2) {
            if (key1 == key2) {
                return true;
            }
            if (key1 == null || key2 == null) {
                return false;
            }

            // Both keys must be of the same type
            if (key1.getClass() != key2.getClass()) {
                return false;
            }

            // Compare identifier keys
            if (key1 instanceof Toml.Identifier && key2 instanceof Toml.Identifier) {
                return ((Toml.Identifier) key1).getName().equals(((Toml.Identifier) key2).getName());
            }

            return false;
        }
    }
}
