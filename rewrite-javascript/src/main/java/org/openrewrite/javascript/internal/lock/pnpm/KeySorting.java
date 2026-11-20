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
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/object.key-sorting src/index.ts (pnpm 11.2.2) over sort-keys 6.0.0 and
 * @pnpm/util.lex-comparator 3.0.2: a copy of an object with its keys sorted, its nested plain objects too when deep.
 */
final class KeySorting {

    static final Comparator<String> LEX_COMPARE = KeySorting::lexCompare;

    static final Set<String> OBJECT_PROTOTYPE = new HashSet<>(Arrays.asList("constructor", "__defineGetter__",
            "__defineSetter__", "hasOwnProperty", "__lookupGetter__", "__lookupSetter__", "isPrototypeOf",
            "propertyIsEnumerable", "toString", "valueOf", "__proto__", "toLocaleString"));

    private KeySorting() {
    }

    /**
     * {@code lexCompare}: {@code a > b ? 1 : a < b ? -1 : 0} over UTF-16 code units.
     */
    static int lexCompare(String a, String b) {
        int c = a.compareTo(b);
        return c > 0 ? 1 : c < 0 ? -1 : 0;
    }

    static Map<String, @Nullable Object> sortDirectKeys(Map<String, @Nullable Object> obj) {
        return sortKeys(obj, LEX_COMPARE, false);
    }

    static Map<String, @Nullable Object> sortDeepKeys(Map<String, @Nullable Object> obj) {
        return sortKeys(obj, LEX_COMPARE, true);
    }

    static Map<String, @Nullable Object> sortKeysByPriority(Map<String, Integer> priority, boolean deep,
                                                            Map<String, @Nullable Object> obj) {
        return sortKeys(obj, (left, right) -> compareWithPriority(priority, left, right), deep);
    }

    private static int compareWithPriority(Map<String, Integer> priority, String left, String right) {
        if (OBJECT_PROTOTYPE.contains(left) || OBJECT_PROTOTYPE.contains(right)) {
            // the priority table is a plain object: such a key finds a function, and the comparison is NaN
            throw new CannotKnow("the order pnpm sorts the key \"" + (OBJECT_PROTOTYPE.contains(left) ? left : right) +
                                 "\" in (V8's sort over a NaN comparison)");
        }
        Integer leftPriority = priority.get(left);
        Integer rightPriority = priority.get(right);
        if (leftPriority != null && rightPriority != null) {
            return leftPriority - rightPriority;
        }
        if (leftPriority != null) {
            return -1;
        }
        if (rightPriority != null) {
            return 1;
        }
        return lexCompare(left, right);
    }

    /**
     * sort-keys {@code sortKeys} of a plain object; a value already sorted once is shared, as its cache does.
     */
    static Map<String, @Nullable Object> sortKeys(Map<String, @Nullable Object> object, Comparator<String> compare,
                                                  boolean deep) {
        return new Sorter(compare, deep).sortObject(object);
    }

    private static final class Sorter {
        final Comparator<String> compare;
        final boolean deep;
        final Map<Object, Object> cache = new IdentityHashMap<>();

        Sorter(Comparator<String> compare, boolean deep) {
            this.compare = compare;
            this.deep = deep;
        }

        @SuppressWarnings("unchecked")
        List<@Nullable Object> sortArray(List<@Nullable Object> array) {
            Object cached = cache.get(array);
            if (cached != null) {
                return (List<@Nullable Object>) cached;
            }
            List<@Nullable Object> result = new ArrayList<>();
            cache.put(array, result);
            for (Object item : array) {
                if (item instanceof List) {
                    result.add(deep ? sortArray((List<@Nullable Object>) item) : item);
                } else if (item instanceof Map) {
                    result.add(deep ? sortObject((Map<String, @Nullable Object>) item) : item);
                } else {
                    result.add(item);
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> sortObject(Map<String, @Nullable Object> object) {
            Object cached = cache.get(object);
            if (cached != null) {
                return (Map<String, @Nullable Object>) cached;
            }
            Map<String, @Nullable Object> result = new LinkedHashMap<>();
            List<String> keys = new ArrayList<>(Js.keys(object));
            keys.sort(compare);
            cache.put(object, result);
            for (String key : keys) {
                Object value = object.get(key);
                if (deep) {
                    if (value instanceof List) {
                        value = sortArray((List<@Nullable Object>) value);
                    } else if (value instanceof Map) {
                        value = sortObject((Map<String, @Nullable Object>) value);
                    }
                }
                result.put(key, value);
            }
            return result;
        }
    }
}
