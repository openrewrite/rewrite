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
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

/**
 * Read-only accessors over the parsed {@code pyproject.toml} LST: table lookup, key/value
 * unwrapping, and string-array extraction with fail-loud validation.
 */
final class UvLockToml {

    private UvLockToml() {
    }

    /**
     * The table TOML defines under {@code name}, wherever its entries are written: in a {@code [name]} table, as dotted
     * keys or an inline table under an enclosing table, or at the top level. Each entry is keyed relative to the table.
     */
    static Toml.@Nullable Table findTable(Toml.Document pyproject, String name) {
        List<String> target = keyPath(name);
        Toml.Table base = null;
        List<Toml> entries = new ArrayList<>();
        for (TomlValue value : pyproject.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                String tableName = tableName(table);
                List<String> path = tableName == null ? new ArrayList<>() : keyPath(tableName);
                if (path.equals(target)) {
                    base = table;
                    entries.addAll(table.getValues());
                } else if (startsWith(target, path) && collectNested(table.getValues(), target.subList(path.size(), target.size()), entries)) {
                    base = base != null ? base : table;
                }
            }
        }
        return base == null ? null : base.withValues(entries);
    }

    // entries an enclosing table defines for the table at the relative path {@code rest}
    private static boolean collectNested(List<Toml> values, List<String> rest, List<Toml> into) {
        boolean found = false;
        for (Toml value : values) {
            if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getKey() instanceof Toml.Identifier)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            Toml.Identifier key = (Toml.Identifier) kv.getKey();
            List<String> path = keyPath(key.getName());
            if (path.size() > rest.size() && startsWith(path, rest)) {
                String relative = String.join(".", path.subList(rest.size(), path.size()));
                into.add(kv.withKey(key.withName(relative).withSource(relative)));
                found = true;
            } else if (path.equals(rest) && kv.getValue() instanceof Toml.Table) {
                into.addAll(((Toml.Table) kv.getValue()).getValues());
                found = true;
            }
        }
        return found;
    }

    private static boolean startsWith(List<String> path, List<String> prefix) {
        return path.size() >= prefix.size() && path.subList(0, prefix.size()).equals(prefix);
    }

    /** A TOML key's segments, split at dots outside quotes, each unquoted and trimmed. */
    static List<String> keyPath(String key) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '.') {
                segments.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        segments.add(current.toString().trim());
        return segments;
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

    static boolean hasKey(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue && key.equals(keyName((Toml.KeyValue) value))) {
                return true;
            }
        }
        return false;
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

    /**
     * Each dependency group under its normalized name, with every {@code include-group} expanded in place, as uv
     * records an including group. An exact duplicate an include brings in is listed once.
     */
    static Map<String, List<String>> dependencyGroups(Toml.Table groups) {
        Map<String, Toml.Array> declared = new LinkedHashMap<>();
        for (Toml value : groups.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            String group = keyName(kv);
            if (group == null || !(kv.getValue() instanceof Toml.Array)) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null, "Unreadable [dependency-groups] entry");
            }
            declared.put(Pep508Requirement.canonicalize(group), (Toml.Array) kv.getValue());
        }
        Map<String, List<String>> expanded = new LinkedHashMap<>();
        for (String group : declared.keySet()) {
            Set<String> specs = new LinkedHashSet<>();
            expandGroup(group, declared, new LinkedHashSet<>(), specs);
            expanded.put(group, new ArrayList<>(specs));
        }
        return expanded;
    }

    private static void expandGroup(String group, Map<String, Toml.Array> declared, Set<String> path, Set<String> specs) {
        Toml.Array array = declared.get(group);
        if (array == null) {
            throw new EngineFailure(Reason.MALFORMED_MANIFEST, null, "include-group names an undeclared group: " + group);
        }
        if (!path.add(group)) {
            throw new EngineFailure(Reason.MALFORMED_MANIFEST, null, "Dependency groups include each other: " + path);
        }
        for (Toml element : array.getValues()) {
            if (element instanceof Toml.Empty) {
                continue;
            }
            Object v = element instanceof Toml.Literal ? ((Toml.Literal) element).getValue() : null;
            if (v instanceof String) {
                specs.add((String) v);
                continue;
            }
            String included = element instanceof Toml.Table ? includedGroup((Toml.Table) element) : null;
            if (included == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null, "Unreadable [dependency-groups] entry in " + group);
            }
            expandGroup(Pep508Requirement.canonicalize(included), declared, path, specs);
        }
        path.remove(group);
    }

    private static @Nullable String includedGroup(Toml.Table table) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue && "include-group".equals(keyName((Toml.KeyValue) value)) &&
                    ((Toml.KeyValue) value).getValue() instanceof Toml.Literal) {
                Object v = ((Toml.Literal) ((Toml.KeyValue) value).getValue()).getValue();
                return v instanceof String ? (String) v : null;
            }
        }
        return null;
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
