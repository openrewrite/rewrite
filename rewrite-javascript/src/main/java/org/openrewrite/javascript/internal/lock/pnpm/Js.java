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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The JavaScript semantics pnpm's code relies on beyond the shared model: the dates and binary values
 * {@link JsYaml} loads, Ramda's {@code equals}, spread copies.
 */
final class Js extends EcmaScript {

    private Js() {
    }

    /**
     * What pnpm prints of an error: an {@code Error}'s message, another kind's name before it.
     */
    static String message(RuntimeException e) {
        return e instanceof JsError && !JsError.ERROR.equals(((JsError) e).getName()) ? e.toString() :
                String.valueOf(e.getMessage());
    }

    /**
     * A copy holding the keys in {@code Object.keys} order, as a spread or {@code Object.fromEntries} builds it.
     */
    static <V> Map<String, V> ordered(Map<String, V> object) {
        Map<String, V> out = new LinkedHashMap<>();
        for (String key : keys(object)) {
            out.put(key, object.get(key));
        }
        return out;
    }

    /**
     * {@code String(value)} for the values a YAML document or package.json holds; a date's text is cannot-know.
     */
    public static String string(@Nullable Object value) {
        return JsYaml.string(value);
    }

    /**
     * Ramda's {@code equals}: by value, objects by their own keys whatever the order.
     */
    @SuppressWarnings("unchecked")
    static boolean equals(@Nullable Object a, @Nullable Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            if (Double.isNaN(x)) {
                return Double.isNaN(y);
            }
            return x == y && (x != 0 || 1 / x == 1 / y);
        }
        if (a instanceof Map && b instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) a;
            Map<String, Object> n = (Map<String, Object>) b;
            if (!m.keySet().equals(n.keySet())) {
                return false;
            }
            for (Map.Entry<String, Object> entry : m.entrySet()) {
                if (!equals(entry.getValue(), n.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof List && b instanceof List) {
            List<Object> l = (List<Object>) a;
            List<Object> k = (List<Object>) b;
            if (l.size() != k.size()) {
                return false;
            }
            for (int i = 0; i < l.size(); i++) {
                if (!equals(l.get(i), k.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(a, b);
    }

    /**
     * {@code JSON.stringify} as a template literal prints it, {@code "undefined"} for undefined; a date as its
     * {@code toJSON}, any other object YAML gives beyond JSON as one without keys.
     */
    public static String jsonStringify(@Nullable Object value) {
        String json = EcmaScript.jsonStringify(json(value));
        return json == null ? "undefined" : json;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object json(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, @Nullable Object> object = new LinkedHashMap<>();
            for (Map.Entry<String, ?> member : ((Map<String, ?>) value).entrySet()) {
                object.put(member.getKey(), json(member.getValue()));
            }
            return object;
        }
        if (value instanceof List) {
            List<@Nullable Object> array = new ArrayList<>();
            for (Object item : (List<?>) value) {
                array.add(json(item));
            }
            return array;
        }
        if (value instanceof JsYaml.Timestamp) {
            return ((JsYaml.Timestamp) value).toIsoString();
        }
        if (value == null || value == UNDEFINED || value instanceof String || value instanceof Number ||
            value instanceof Boolean) {
            return value;
        }
        return new LinkedHashMap<>();
    }
}
