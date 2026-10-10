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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of deep-equal 1.0.1 without {@code strict}, as yarn 1.22.22 bundles it, over the values a lock holds:
 * identical values, {@code ==} between primitives, and objects with the same keys holding deep-equal values.
 */
final class DeepEqual {

    private DeepEqual() {
    }

    static boolean deepEqual(@Nullable Object actual, @Nullable Object expected) {
        if (actual == expected || (actual != null && !isObject(actual) && actual.equals(expected))) {
            return true;
        }
        if (!Js.truthy(actual) || !Js.truthy(expected) || !isObject(actual) && !isObject(expected)) {
            return looseEquals(actual, expected);
        }
        return objEquiv(actual, expected);
    }

    private static boolean isObject(Object value) {
        return value instanceof Map || value instanceof List;
    }

    // ==
    private static boolean looseEquals(@Nullable Object a, @Nullable Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (isObject(a) || isObject(b)) {
            if (isObject(a) && isObject(b)) {
                return a == b;
            }
            return Js.string(isObject(a) ? a : b).equals(Js.string(isObject(a) ? b : a));
        }
        if (a instanceof String && b instanceof String) {
            return a.equals(b);
        }
        return Js.toNumber(a) == Js.toNumber(b);
    }


    private static boolean objEquiv(Object a, Object b) {
        Map<String, @Nullable Object> ka = entries(a);
        Map<String, @Nullable Object> kb = entries(b);
        if (ka.size() != kb.size()) {
            return false;
        }
        if (!new HashSet<>(ka.keySet()).equals(kb.keySet())) {
            return false;
        }
        for (String key : ka.keySet()) {
            if (!deepEqual(ka.get(key), kb.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, @Nullable Object> entries(Object value) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
            out.put(entry.getKey(), entry.getValue());
        }
        return out;
    }
}
