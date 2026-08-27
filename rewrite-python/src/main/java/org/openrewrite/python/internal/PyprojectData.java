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
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reconstructs a {@code pyproject.toml} LST into the nested {@code Map}/{@code List}/scalar tree
 * that Python's {@code tomllib} would produce, so that tool content-hash algorithms (poetry, pdm)
 * can be reproduced byte-for-byte. Dotted table headers become nested maps and {@code [[header]]}
 * array-of-tables become lists.
 */
public final class PyprojectData {

    private PyprojectData() {
    }

    public static Map<String, Object> toNestedMap(Toml.Document doc) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (TomlValue value : doc.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                if (table.getName() != null) {
                    boolean arrayTable = table.getMarkers().findFirst(ArrayTable.class).isPresent();
                    placeTable(root, splitKey(table.getName().getName()), tableToMap(table), arrayTable);
                }
            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void placeTable(Map<String, Object> root, List<String> path, Map<String, Object> body,
                                   boolean arrayTable) {
        Map<String, Object> parent = root;
        for (int i = 0; i < path.size() - 1; i++) {
            Object child = parent.get(path.get(i));
            if (child instanceof List) {
                List<Object> list = (List<Object>) child;
                child = list.get(list.size() - 1);
            }
            if (!(child instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                parent.put(path.get(i), child);
            }
            parent = (Map<String, Object>) child;
        }
        String leaf = path.get(path.size() - 1);
        Object existing = parent.get(leaf);
        if (arrayTable) {
            List<Object> list = existing instanceof List ? (List<Object>) existing : new ArrayList<>();
            list.add(body);
            parent.put(leaf, list);
        } else if (existing instanceof Map) {
            // a parent table declared after its child subtable: merge rather than clobber
            ((Map<String, Object>) existing).putAll(body);
        } else {
            parent.put(leaf, body);
        }
    }

    private static Map<String, Object> tableToMap(Toml.Table table) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (kv.getKey() instanceof Toml.Identifier) {
                    putDotted(map, splitKey(((Toml.Identifier) kv.getKey()).getName()), toJava(kv.getValue()));
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static void putDotted(Map<String, Object> map, List<String> path, @Nullable Object value) {
        Map<String, Object> parent = map;
        for (int i = 0; i < path.size() - 1; i++) {
            Object child = parent.get(path.get(i));
            if (!(child instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                parent.put(path.get(i), child);
            }
            parent = (Map<String, Object>) child;
        }
        parent.put(path.get(path.size() - 1), value);
    }

    private static @Nullable Object toJava(Toml value) {
        if (value instanceof Toml.Literal) {
            return ((Toml.Literal) value).getValue();
        }
        if (value instanceof Toml.Table) {
            return tableToMap((Toml.Table) value);
        }
        if (value instanceof Toml.Array) {
            List<Object> list = new ArrayList<>();
            for (Toml element : ((Toml.Array) value).getValues()) {
                if (!(element instanceof Toml.Empty)) {
                    list.add(toJava(element));
                }
            }
            return list;
        }
        return null;
    }

    /** Split a possibly dotted, possibly quoted TOML key path into its segments. */
    private static List<String> splitKey(String name) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (inQuote) {
                if (c == quote) {
                    inQuote = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                quote = c;
            } else if (c == '.') {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());
        return parts;
    }
}
