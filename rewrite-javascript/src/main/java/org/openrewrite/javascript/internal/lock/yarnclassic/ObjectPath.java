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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of object-path 0.11.4 {@code has} and {@code set} (yarn 1.22.22's bundle).
 */
final class ObjectPath {

    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    private ObjectPath() {
    }

    static boolean has(@Nullable Object obj, String path) {
        for (String key : path.split("\\.", -1)) {
            if ((isNumber(key) && obj instanceof List && Double.parseDouble(key) < ((List<?>) obj).size()) ||
                Js.hasOwnProperty(obj, key)) {
                obj = Js.get(obj, key);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * @throws JsError the {@code TypeError} of a property set on a string, number or boolean the path runs into.
     */
    static void set(Map<String, @Nullable Object> obj, String path, @Nullable Object value) {
        if (!path.isEmpty()) {
            set((Object) obj, Arrays.asList(path.split("\\.", -1)), value);
        }
    }

    private static void set(Object obj, List<String> path, @Nullable Object value) {
        String currentPath = path.get(0);
        if (path.size() == 1) {
            put(obj, currentPath, value);
            return;
        }
        Object currentValue = Js.get(obj, currentPath);
        if (currentValue == null) {
            currentValue = isNumber(path.get(1)) ? new ArrayList<>() : new LinkedHashMap<String, Object>();
            put(obj, currentPath, currentValue);
        }
        // an array's named property: its keys as Object.keys gives them
        if (currentValue instanceof List && Js.arrayIndex(path.get(1)) < 0) {
            Map<String, @Nullable Object> object = new LinkedHashMap<>();
            List<?> list = (List<?>) currentValue;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null) {
                    object.put(String.valueOf(i), list.get(i));
                }
            }
            currentValue = object;
            put(obj, currentPath, currentValue);
        }
        set(currentValue, path.subList(1, path.size()), value);
    }

    @SuppressWarnings("unchecked")
    private static void put(Object obj, String key, @Nullable Object value) {
        if (obj instanceof Map) {
            ((Map<String, @Nullable Object>) obj).put(key, value);
        } else if (obj instanceof List) {
            List<@Nullable Object> list = (List<@Nullable Object>) obj;
            int index = (int) Js.arrayIndex(key);
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, value);
        } else if (obj instanceof String && Js.get(obj, key) != null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot assign to read only property '" + key + "' of string '" +
                                                  obj + "'");
        } else {
            throw new JsError(JsError.TYPE_ERROR, "Cannot create property '" + key + "' on " + Js.typeOf(obj) +
                                                  " '" + Js.string(obj) + "'");
        }
    }

    // getKey: a key that reads back as the integer it parses to is a number
    private static boolean isNumber(String key) {
        return INTEGER.matcher(key).matches() && Js.numberToString(Double.parseDouble(key)).equals(key);
    }
}
