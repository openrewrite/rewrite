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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core sources/configUtils.ts (Yarn 4.10.3).
 */
final class ConfigUtils {

    private ConfigUtils() {
    }

    /**
     * A value with the source it came from: {@code [id, value, RESOLVED_RC_FILE]}.
     */
    @Value
    static class Resolved {
        String source;

        @Nullable
        Object value;
    }

    private enum ValueType {
        OBJECT, ARRAY, LITERAL, UNDEFINED
    }

    private static boolean isObject(@Nullable Object data) {
        return data instanceof Map;
    }

    private static ValueType getValueType(@Nullable Object data) {
        if (data == Syml.UNDEFINED) {
            return ValueType.UNDEFINED;
        }
        if (isObject(data)) {
            return ValueType.OBJECT;
        }
        if (data instanceof List) {
            return ValueType.ARRAY;
        }
        return ValueType.LITERAL;
    }

    private static boolean isConflictMarker(@Nullable Object data) {
        return isObject(data) && ((Map<?, ?>) data).containsKey("onConflict") &&
               ((Map<?, ?>) data).get("onConflict") instanceof String;
    }

    // {onConflict, value}
    @SuppressWarnings("unchecked")
    private static Object[] normalizeValue(@Nullable Object data) {
        if (data == Syml.UNDEFINED || !isConflictMarker(data)) {
            return new Object[]{"default", data};
        }
        Map<String, @Nullable Object> marker = (Map<String, @Nullable Object>) data;
        if (marker.containsKey("value")) {
            return new Object[]{marker.get("onConflict"), marker.get("value")};
        }
        Map<String, @Nullable Object> value = new LinkedHashMap<>(marker);
        value.remove("onConflict");
        return new Object[]{marker.get("onConflict"), value};
    }

    private static Object[] getNormalized(@Nullable Object data, String key) {
        Object rawValue = isObject(data) && ((Map<?, ?>) data).containsKey(key) ? ((Map<?, ?>) data).get(key) :
                Syml.UNDEFINED;
        return normalizeValue(rawValue);
    }

    @SuppressWarnings("unchecked")
    private static Resolved attachIdToTree(@Nullable Object data, String id) {
        if (isObject(data)) {
            Map<String, @Nullable Object> result = new LinkedHashMap<>();
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) data;
            for (String key : Js.keys(map)) {
                result.put(key, attachIdToTree(map.get(key), id));
            }
            return new Resolved(id, result);
        }
        if (data instanceof List) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object item : (List<?>) data) {
                list.add(attachIdToTree(item, id));
            }
            return new Resolved(id, list);
        }
        return new Resolved(id, data);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Resolved resolveValueAt(List<Object[]> rcFiles, String key, int firstVisiblePosition,
                                                     int resolveAtPosition) {
        ValueType expectedValueType = null;
        List<Object[]> relevantValues = new ArrayList<>();
        int lastRelevantPosition = resolveAtPosition;
        int currentResetPosition = 0;

        for (int t = resolveAtPosition - 1; t >= firstVisiblePosition; --t) {
            String id = (String) rcFiles.get(t)[0];
            Object[] normalized = getNormalized(rcFiles.get(t)[1], key);
            String onConflict = (String) normalized[0];
            Object value = normalized[1];
            ValueType valueType = getValueType(value);
            if (valueType == ValueType.UNDEFINED) {
                continue;
            }
            if (expectedValueType == null) {
                expectedValueType = valueType;
            }
            if (valueType != expectedValueType || "hardReset".equals(onConflict)) {
                currentResetPosition = lastRelevantPosition;
                break;
            }
            if (valueType == ValueType.LITERAL) {
                return new Resolved(id, value);
            }
            relevantValues.add(0, new Object[]{id, value});
            if ("reset".equals(onConflict)) {
                currentResetPosition = t;
                break;
            }
            if ("extend".equals(onConflict) && t == firstVisiblePosition) {
                firstVisiblePosition = 0;
            }
            lastRelevantPosition = t;
        }

        if (expectedValueType == null) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (Object[] relevant : relevantValues) {
            ids.add((String) relevant[0]);
        }
        String source = String.join(", ", ids);
        if (expectedValueType == ValueType.ARRAY) {
            List<@Nullable Object> concatenated = new ArrayList<>();
            for (Object[] relevant : relevantValues) {
                for (Object item : (List<?>) relevant[1]) {
                    concatenated.add(attachIdToTree(item, (String) relevant[0]));
                }
            }
            return new Resolved(source, concatenated);
        }
        if (expectedValueType == ValueType.OBJECT) {
            Map<String, @Nullable Object> conglomerate = new LinkedHashMap<>();
            for (Object[] relevant : relevantValues) {
                Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) relevant[1];
                for (String k : Js.keys(map)) {
                    conglomerate.put(k, map.get(k));
                }
            }
            List<String> keys = Js.keys(conglomerate);
            Map<String, @Nullable Object> result = new LinkedHashMap<>();

            List<Object[]> nextIterationValues = new ArrayList<>();
            for (Object[] rcFile : rcFiles) {
                nextIterationValues.add(new Object[]{rcFile[0], getNormalized(rcFile[1], key)[1]});
            }
            int hardResetLocation = -1;
            for (int i = nextIterationValues.size() - 1; i >= 0; i--) {
                ValueType valueType = getValueType(nextIterationValues.get(i)[1]);
                if (valueType != ValueType.OBJECT && valueType != ValueType.UNDEFINED) {
                    // findLastIndex over the reversed array: an index counted from the end
                    hardResetLocation = nextIterationValues.size() - 1 - i;
                    break;
                }
            }
            if (hardResetLocation != -1) {
                List<Object[]> slice = nextIterationValues.subList(hardResetLocation + 1, nextIterationValues.size());
                for (String k : keys) {
                    result.put(k, resolveValueAt(slice, k, 0, slice.size()));
                }
            } else {
                for (String k : keys) {
                    result.put(k, resolveValueAt(nextIterationValues, k, currentResetPosition,
                            nextIterationValues.size()));
                }
            }
            return new Resolved(source, result);
        }
        throw new JsError(JsError.ERROR, "Assertion failed: Non-extendable value type");
    }

    /**
     * @param rcFiles each file's path and parsed content, the farthest from the project first
     */
    static @Nullable Resolved resolveRcFiles(List<Object[]> rcFiles) {
        List<Object[]> wrapped = new ArrayList<>();
        for (Object[] rcFile : rcFiles) {
            Map<String, @Nullable Object> dot = new LinkedHashMap<>();
            dot.put(".", rcFile[1]);
            wrapped.add(new Object[]{rcFile[0], dot});
        }
        return resolveValueAt(wrapped, ".", 0, wrapped.size());
    }

    static @Nullable Object getValue(@Nullable Object value) {
        return value instanceof Resolved ? ((Resolved) value).value : value;
    }

    static @Nullable Object getValueByTree(@Nullable Object valueBase) {
        Object value = getValue(valueBase);
        if (value instanceof List) {
            List<@Nullable Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) {
                out.add(getValueByTree(item));
            }
            return out;
        }
        if (value instanceof Map) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
                out.put(entry.getKey(), getValueByTree(entry.getValue()));
            }
            return out;
        }
        return value;
    }

    static @Nullable String getSource(@Nullable Object value) {
        return value instanceof Resolved ? ((Resolved) value).source : null;
    }
}
