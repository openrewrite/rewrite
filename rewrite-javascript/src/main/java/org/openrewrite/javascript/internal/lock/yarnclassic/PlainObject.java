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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An object literal yarn builds ({@code {}}, js-yaml's mappings): a lookup on it finds Object.prototype's members,
 * unlike on the prototype-less objects of yarn's lockfile parser and JSON reader.
 */
final class PlainObject extends LinkedHashMap<String, @Nullable Object> {

    static final String PROTO = "__proto__";

    // Node 24's Object.prototype and Array.prototype functions
    private static final List<String> OBJECT_FUNCTIONS = Arrays.asList("constructor", "__defineGetter__",
            "__defineSetter__", "hasOwnProperty", "__lookupGetter__", "__lookupSetter__", "isPrototypeOf",
            "propertyIsEnumerable", "toString", "valueOf", "toLocaleString");
    private static final List<String> ARRAY_FUNCTIONS = Arrays.asList("constructor", "at", "concat", "copyWithin",
            "fill", "find", "findIndex", "findLast", "findLastIndex", "lastIndexOf", "pop", "push", "reverse", "shift",
            "unshift", "slice", "sort", "splice", "includes", "indexOf", "join", "keys", "entries", "values",
            "forEach", "filter", "flat", "flatMap", "map", "every", "some", "reduce", "reduceRight", "toReversed",
            "toSorted", "toSpliced", "with", "toLocaleString", "toString");

    /**
     * The built-in function every object literal inherits under a key ({@code toString}), else null.
     */
    static @Nullable BuiltinFunction objectFunction(String key) {
        return OBJECT_FUNCTIONS.contains(key) ? new BuiltinFunction("constructor".equals(key) ? "Object" : key) : null;
    }

    /**
     * The built-in function every array inherits under a key ({@code map}, {@code toString}), else null.
     */
    static @Nullable BuiltinFunction arrayFunction(String key) {
        if (ARRAY_FUNCTIONS.contains(key)) {
            return new BuiltinFunction("constructor".equals(key) ? "Array" : key);
        }
        return objectFunction(key);
    }

    /**
     * A value of {@code JSON.parse}, whose objects inherit Object.prototype.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Object fromJson(@Nullable Object value) {
        if (value instanceof Map) {
            PlainObject object = new PlainObject();
            for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) value).entrySet()) {
                object.put(entry.getKey(), fromJson(entry.getValue()));
            }
            return object;
        }
        if (value instanceof List) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object item : (List<?>) value) {
                list.add(fromJson(item));
            }
            return list;
        }
        return value;
    }

    /**
     * A copy of the object and of every object and array in it, each kept as the kind of object it is.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> copyOf(Map<String, @Nullable Object> object) {
        Map<String, @Nullable Object> copy = object instanceof PlainObject ? new PlainObject() : new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : object.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = copyOf((Map<String, @Nullable Object>) value);
            } else if (value instanceof List) {
                value = new ArrayList<>((List<?>) value);
            }
            copy.put(entry.getKey(), value);
        }
        return copy;
    }

    static final class BuiltinFunction {
        private final String name;

        BuiltinFunction(String name) {
            this.name = name;
        }

        /**
         * {@code String(fn)}.
         */
        @Override
        public String toString() {
            return "function " + name + "() { [native code] }";
        }
    }
}
