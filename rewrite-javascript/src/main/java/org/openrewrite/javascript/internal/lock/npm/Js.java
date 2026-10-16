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
import org.openrewrite.javascript.internal.lock.EcmaScript;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JavaScript's semantics for the values and strings npm reads from package and lock data.
 */
final class Js extends EcmaScript {

    /**
     * A member a value inherits from its prototype, such as {@code constructor}: truthy and never a string.
     */
    static final Object INHERITED = new Object() {
        @Override
        public String toString() {
            return "function";
        }
    };

    // the prototypes of V8 13.6, which Node 24 runs npm 11 on
    private static final Set<String> ARRAY_MEMBERS = members(
            "at concat copyWithin entries every fill filter find findIndex findLast findLastIndex flat flatMap " +
            "forEach includes indexOf join keys lastIndexOf map pop push reduce reduceRight reverse shift slice " +
            "some sort splice toReversed toSorted toSpliced unshift values with");
    private static final Set<String> STRING_MEMBERS = members(
            "anchor at big blink bold charAt charCodeAt codePointAt concat endsWith fixed fontcolor fontsize " +
            "includes indexOf isWellFormed italics lastIndexOf link localeCompare match matchAll normalize padEnd " +
            "padStart repeat replace replaceAll search slice small split startsWith strike sub substr substring " +
            "sup toLocaleLowerCase toLocaleUpperCase toLowerCase toUpperCase toWellFormed trim trimEnd trimLeft " +
            "trimRight trimStart");
    private static final Set<String> NUMBER_MEMBERS = members("toExponential toFixed toPrecision");


    private Js() {
    }

    private static Set<String> members(String names) {
        return new HashSet<>(Arrays.asList(names.split(" ")));
    }

    /**
     * {@code receiver[key]} for a value parsed from JSON: {@link #UNDEFINED} without such a property, {@link
     * #INHERITED} for a prototype member; IllegalArgumentException for JavaScript's TypeError on null or undefined.
     */
    static @Nullable Object get(@Nullable Object receiver, String key) {
        if (receiver == null || receiver == UNDEFINED) {
            String type = receiver == null ? "null" : "undefined";
            throw new IllegalArgumentException("Cannot read properties of " + type + " (reading '" + key + "')");
        }
        if (receiver instanceof Map) {
            Map<?, ?> object = (Map<?, ?>) receiver;
            if (object.containsKey(key)) {
                return object.get(key);
            }
        } else if (receiver instanceof List) {
            List<?> array = (List<?>) receiver;
            long index = arrayIndex(key);
            if (index >= 0 && index < array.size()) {
                Object element = array.get((int) index);
                return element == HOLE ? UNDEFINED : element;
            }
            if ("length".equals(key)) {
                return array.size();
            }
            if (ARRAY_MEMBERS.contains(key)) {
                return INHERITED;
            }
        } else if (receiver instanceof String) {
            String string = (String) receiver;
            long index = arrayIndex(key);
            if (index >= 0 && index < string.length()) {
                return String.valueOf(string.charAt((int) index));
            }
            if ("length".equals(key)) {
                return string.length();
            }
            if (STRING_MEMBERS.contains(key)) {
                return INHERITED;
            }
        } else if (receiver instanceof Number && NUMBER_MEMBERS.contains(key)) {
            return INHERITED;
        }
        return objectPrototypeMember(key) != null ? INHERITED : UNDEFINED;
    }

    /**
     * {@code delete receiver[key]} in strict mode: an object loses the property, an array keeps a {@link #HOLE}, and an
     * undeletable property (a string's characters, a length) is JavaScript's TypeError.
     */
    static void delete(@Nullable Object receiver, String key) {
        if (receiver instanceof String || receiver instanceof List) {
            long index = arrayIndex(key);
            boolean fixed = "length".equals(key) ||
                            receiver instanceof String && index >= 0 && index < ((String) receiver).length();
            if (fixed) {
                throw new IllegalArgumentException("Cannot delete property '" + key + "' of [object " +
                                                   (receiver instanceof String ? "String" : "Array") + "]");
            }
        }
        deleteSloppy(receiver, key);
    }

    /**
     * {@code delete receiver[key]} outside strict mode: a property that cannot be deleted is left alone.
     */
    @SuppressWarnings("unchecked")
    static void deleteSloppy(@Nullable Object receiver, String key) {
        if (receiver == null || receiver == UNDEFINED) {
            throw new IllegalArgumentException("Cannot convert undefined or null to object");
        }
        if (receiver instanceof Map) {
            ((Map<String, Object>) receiver).remove(key);
        } else if (receiver instanceof List) {
            List<Object> array = (List<Object>) receiver;
            long index = arrayIndex(key);
            if (index >= 0 && index < array.size()) {
                array.set((int) index, HOLE);
            }
        }
    }


    /**
     * How Node's {@code ERR_INVALID_ARG_TYPE} names the value it received.
     */
    static String received(@Nullable Object value) {
        if (value == null || value == UNDEFINED) {
            return String.valueOf(value);
        }
        if (value instanceof Number) {
            return "type number (" + numberToString((Number) value) + ")";
        }
        if (value instanceof Boolean) {
            return "type boolean (" + value + ")";
        }
        if (value instanceof String) {
            return "type string ('" + value + "')";
        }
        return "an instance of " + (value instanceof List ? "Array" : "Object");
    }
}
