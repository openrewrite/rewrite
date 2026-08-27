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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Read-only accessors over the parsed {@code pyproject.toml} LST: table lookup, key/value
 * unwrapping, and string-array extraction with fail-loud validation.
 */
final class UvLockToml {

    private UvLockToml() {
    }

    static Toml.@Nullable Table findTable(Toml.Document pyproject, String name) {
        for (TomlValue value : pyproject.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                if (name.equals(tableName(table))) {
                    return table;
                }
            }
        }
        return null;
    }

    static @Nullable String tableName(Toml.Table table) {
        Toml.Identifier name = table.getName();
        return name != null ? name.getName() : null;
    }

    static @Nullable String keyName(Toml.KeyValue kv) {
        if (kv.getKey() instanceof Toml.Identifier) {
            return unquote(((Toml.Identifier) kv.getKey()).getName());
        }
        return null;
    }

    static String unquote(String s) {
        if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') &&
                s.charAt(s.length() - 1) == s.charAt(0)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    static @Nullable String literalString(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (key.equals(keyName(kv)) && kv.getValue() instanceof Toml.Literal) {
                    Object v = ((Toml.Literal) kv.getValue()).getValue();
                    if (v instanceof String) {
                        return (String) v;
                    }
                }
            }
        }
        return null;
    }

    static List<String> stringArray(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (key.equals(keyName(kv))) {
                    if (!(kv.getValue() instanceof Toml.Array)) {
                        throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                                "Expected an array for " + key);
                    }
                    return stringArrayValues((Toml.Array) kv.getValue(), key);
                }
            }
        }
        return emptyList();
    }

    static List<String> stringArrayValues(Toml.Array array, String context) {
        List<String> strings = new ArrayList<>();
        for (Toml element : array.getValues()) {
            if (element instanceof Toml.Empty) {
                continue; // trailing comma
            }
            Object v = element instanceof Toml.Literal ? ((Toml.Literal) element).getValue() : null;
            if (!(v instanceof String)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Non-string entry in " + context + " (e.g. include-group) is not supported");
            }
            strings.add((String) v);
        }
        return strings;
    }

    static boolean hasIndexKey(Toml value) {
        if (value instanceof Toml.Table) {
            return literalString((Toml.Table) value, "index") != null;
        }
        if (value instanceof Toml.Array) {
            for (Toml element : ((Toml.Array) value).getValues()) {
                if (!(element instanceof Toml.Table) ||
                        literalString((Toml.Table) element, "index") == null) {
                    return false;
                }
            }
            return !((Toml.Array) value).getValues().isEmpty();
        }
        return false;
    }
}
