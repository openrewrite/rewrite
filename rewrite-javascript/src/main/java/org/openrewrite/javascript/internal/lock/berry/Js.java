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
package org.openrewrite.javascript.internal.lock.berry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JavaScript semantics Yarn's plain objects follow: {@code undefined} for a missing property, the order
 * {@code Object.keys} gives, truthiness and {@code JSON.parse}.
 */
final class Js {

    private static final long MAX_ARRAY_INDEX = 4294967294L;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    /**
     * {@code JSON.parse} rejecting its input.
     */
    static final class SyntaxError extends RuntimeException {
        SyntaxError(String message) {
            super(message);
        }
    }

    private Js() {
    }

    /**
     * {@code value[key]}: the property, or {@link Syml#UNDEFINED} when the value is not an object or lacks it.
     */
    static @Nullable Object get(@Nullable Object value, String key) {
        if (value instanceof Map && ((Map<?, ?>) value).containsKey(key)) {
            return ((Map<?, ?>) value).get(key);
        }
        return Syml.UNDEFINED;
    }

    static boolean isObject(@Nullable Object value) {
        return value instanceof Map || value instanceof List;
    }

    static boolean truthy(@Nullable Object value) {
        if (value == null || value == Syml.UNDEFINED || Boolean.FALSE.equals(value)) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return true;
    }

    /**
     * {@code Object.keys}: the integer keys in ascending order, then the rest in insertion order.
     */
    static List<String> keys(Map<String, ?> object) {
        List<String> indices = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        for (String key : object.keySet()) {
            if (arrayIndex(key) >= 0) {
                indices.add(key);
            } else {
                rest.add(key);
            }
        }
        indices.sort((a, b) -> Long.compare(arrayIndex(a), arrayIndex(b)));
        indices.addAll(rest);
        return indices;
    }

    /**
     * {@code Object.entries} of an object, or of an array by index.
     */
    @SuppressWarnings("unchecked")
    static List<Map.Entry<String, @Nullable Object>> entries(@Nullable Object value) {
        List<Map.Entry<String, @Nullable Object>> out = new ArrayList<>();
        if (value instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) value;
            for (String key : keys(map)) {
                out.add(new AbstractMap.SimpleEntry<>(key, map.get(key)));
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                out.add(new AbstractMap.SimpleEntry<>(String.valueOf(i), list.get(i)));
            }
        }
        return out;
    }

    static long arrayIndex(String key) {
        if (key.isEmpty() || key.length() > 10 || (key.length() > 1 && key.charAt(0) == '0')) {
            return -1;
        }
        long index = 0;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            index = index * 10 + (c - '0');
        }
        return index <= MAX_ARRAY_INDEX ? index : -1;
    }

    /**
     * {@code JSON.parse}, objects with their keys in the order JavaScript gives them and numbers as doubles.
     */
    static @Nullable Object jsonParse(String text) {
        try {
            return order(MAPPER.readValue(text, Object.class));
        } catch (JsonProcessingException e) {
            throw new SyntaxError(e.getOriginalMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object order(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, @Nullable Object> ordered = new LinkedHashMap<>();
            for (String key : keys(map)) {
                ordered.put(key, order(map.get(key)));
            }
            return ordered;
        }
        if (value instanceof List) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                list.add(order(item));
            }
            return list;
        }
        if (value instanceof Number && !(value instanceof Double)) {
            return ((Number) value).doubleValue();
        }
        return value;
    }

    /**
     * {@code String(value)} for the values a manifest holds.
     */
    static String string(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e21) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        if (value instanceof Map) {
            return "[object Object]";
        }
        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                Object item = list.get(i);
                out.append(item == null || item == Syml.UNDEFINED ? "" : string(item));
            }
            return out.toString();
        }
        return value.toString();
    }
}
