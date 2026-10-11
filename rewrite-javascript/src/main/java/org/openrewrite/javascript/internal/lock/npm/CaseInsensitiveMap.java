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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Port of @npmcli/arborist lib/case-insensitive-map.js (npm 11.17.0).
 */
public final class CaseInsensitiveMap<V> {

    private final Map<String, V> entries = new LinkedHashMap<>();
    private final Map<String, String> keys = new HashMap<>();

    private static String normKey(String key) {
        return Normalizer.normalize(key, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
    }

    public @Nullable V get(@Nullable Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        String normKey = normKey((String) key);
        return keys.containsKey(normKey) ? entries.get(keys.get(normKey)) : null;
    }

    public void set(String key, V val) {
        String normKey = normKey(key);
        if (keys.containsKey(normKey)) {
            entries.remove(keys.get(normKey));
        }
        keys.put(normKey, key);
        entries.put(key, val);
    }

    public boolean delete(String key) {
        String normKey = normKey(key);
        if (keys.containsKey(normKey)) {
            String prevKey = keys.remove(normKey);
            return entries.remove(prevKey) != null;
        }
        return false;
    }

    public boolean has(@Nullable Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        String normKey = normKey((String) key);
        return keys.containsKey(normKey) && entries.containsKey(keys.get(normKey));
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * A snapshot, so callers may change the map while iterating as JavaScript code does.
     */
    public List<V> values() {
        return new ArrayList<>(entries.values());
    }

    public List<String> keys() {
        return new ArrayList<>(entries.keySet());
    }

    public List<Map.Entry<String, V>> entries() {
        return new ArrayList<>(entries.entrySet());
    }
}
