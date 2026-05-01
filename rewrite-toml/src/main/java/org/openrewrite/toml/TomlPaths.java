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
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.marker.InlineTable;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.List;

/**
 * Path-based lookup over a {@link Toml.Document}. Resolves a logical key path
 * (a list of unquoted segment names) to the AST node for that key, regardless
 * of whether the document expressed it as
 * <ul>
 *     <li>a flat dotted key ({@code a.b.c.x = 1}),</li>
 *     <li>nested table headers ({@code [a.b.c] x = 1}),</li>
 *     <li>nested inline tables ({@code a = {b = {c = {x = 1}}}}), or</li>
 *     <li>any combination of the above (e.g., {@code [a.b] c.x = 1}).</li>
 * </ul>
 *
 * <p>Quoted segments containing literal dots are treated as a single segment,
 * so {@code site."google.com"} resolves at path {@code ["site", "google.com"]}
 * (length 2), distinct from {@code site.google.com} which resolves at
 * {@code ["site", "google", "com"]} (length 3).
 *
 * <p>Array tables ({@code [[products]]}) are not searched: there is no way to
 * disambiguate which element of the array a path refers to.
 */
public final class TomlPaths {

    private TomlPaths() {
    }

    /**
     * Find the {@link Toml.KeyValue} at the given logical key path, or
     * {@code null} if no such key exists.
     */
    public static Toml.@Nullable KeyValue findKeyValue(Toml.Document doc, List<String> path) {
        if (path.isEmpty()) {
            return null;
        }
        return findKeyValueIn(doc.getValues(), path);
    }

    /**
     * Find a standard (non-array, non-inline) {@link Toml.Table} whose header
     * matches the given logical key path, or {@code null} if no such table
     * exists. Implicit tables defined only via dotted keys (e.g. {@code [a.b]}
     * implicitly defines {@code [a]}) are not returned — only tables that are
     * explicitly written as {@code [path]} are matched.
     */
    public static Toml.@Nullable Table findTable(Toml.Document doc, List<String> path) {
        if (path.isEmpty()) {
            return null;
        }
        for (TomlValue value : doc.getValues()) {
            if (!(value instanceof Toml.Table)) {
                continue;
            }
            Toml.Table table = (Toml.Table) value;
            if (isStandardTable(table) && table.getName() != null && path.equals(table.getName().getPath())) {
                return table;
            }
        }
        return null;
    }

    private static Toml.@Nullable KeyValue findKeyValueIn(List<? extends Toml> elements, List<String> targetSuffix) {
        for (Toml element : elements) {
            if (element instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) element;
                List<String> kvPath = kv.getKey().getPath();
                if (kvPath.equals(targetSuffix)) {
                    return kv;
                }
                if (kv.getValue() instanceof Toml.Table) {
                    Toml.KeyValue found = recurseAtPrefix(kvPath, ((Toml.Table) kv.getValue()).getValues(), targetSuffix);
                    if (found != null) {
                        return found;
                    }
                }
            } else if (element instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) element;
                if (!isStandardTable(table) || table.getName() == null) {
                    continue;
                }
                Toml.KeyValue found = recurseAtPrefix(table.getName().getPath(), table.getValues(), targetSuffix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Toml.@Nullable KeyValue recurseAtPrefix(List<String> prefix, List<? extends Toml> children, List<String> targetSuffix) {
        if (prefix.size() >= targetSuffix.size() || !targetSuffix.subList(0, prefix.size()).equals(prefix)) {
            return null;
        }
        return findKeyValueIn(children, targetSuffix.subList(prefix.size(), targetSuffix.size()));
    }

    private static boolean isStandardTable(Toml.Table table) {
        return !table.getMarkers().findFirst(InlineTable.class).isPresent() &&
                !table.getMarkers().findFirst(ArrayTable.class).isPresent();
    }
}
