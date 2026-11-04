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
import org.openrewrite.javascript.internal.lock.EcmaScript.JsonReplacer;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.V8Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of json-stringify-nice 1.1.4, a {@code JSON.stringify} replacer: scalars before objects, preferred keys
 * first, the rest in {@code localeCompare('en')} order; with its release as an option.
 */
final class JsonStringifyNice {

    /**
     * The json-stringify-nice release: 1.1.4, as 1.1.3 sorts; or 1.1.1, whose {@code isObj} answers a falsy value
     * with that value, so that the sort compares an empty string or a {@code false} as neither a scalar nor an object
     * and the order it leaves is V8's sort's.
     */
    enum Release {
        V1_1_1, V1_1_4
    }

    private JsonStringifyNice() {
    }

    static String stringify(@Nullable Object obj) {
        return stringify(obj, Collections.emptyList(), 2);
    }

    /**
     * @param space {@code JSON.stringify}'s space: a number of spaces or an indent string; nothing for
     *              {@code null}, {@code 0}, {@code false} or {@code ""}
     */
    static String stringify(@Nullable Object obj, List<String> prefKeys, @Nullable Object space) {
        return stringify(obj, prefKeys, space, Release.V1_1_4);
    }

    static String stringify(@Nullable Object obj, List<String> prefKeys, @Nullable Object space, Release release) {
        String json = Js.jsonStringify(obj, new Sorter(prefKeys, release), space);
        return (json == null ? "undefined" : json) + (Js.truthy(space) ? "\n" : "");
    }

    private static final class Sorter implements JsonReplacer {
        private final List<String> prefKeys;
        private final Release release;
        private final Map<Object, Map<String, Object>> seen = new IdentityHashMap<>();

        Sorter(List<String> prefKeys, Release release) {
            this.prefKeys = prefKeys;
            this.release = release;
        }

        @Override
        public @Nullable Object replace(String key, @Nullable Object val) {
            if (!isObj(val)) {
                return val;
            }
            Map<String, Object> cached = seen.get(val);
            if (cached != null) {
                return cached;
            }
            List<Map.Entry<String, Object>> entries = new ArrayList<>(Js.entries(val));
            if (release == Release.V1_1_1) {
                // isObj(av) === isObj(bv): a falsy value is its own answer, unequal to any other kind's
                V8Sort.sort(entries, (a, b) -> Js.strictEquals(isObj111(a.getValue()), isObj111(b.getValue())) ?
                        compare(a.getKey(), b.getKey()) : Js.truthy(isObj111(a.getValue())) ? 1 : -1);
            } else {
                entries.sort((a, b) -> isObj(a.getValue()) == isObj(b.getValue()) ?
                        compare(a.getKey(), b.getKey()) : isObj(a.getValue()) ? 1 : -1);
            }
            Map<String, Object> ret = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : entries) {
                // the reduce's set[k] = v makes an own __proto__ the new object's prototype, or nothing
                if (!"__proto__".equals(e.getKey())) {
                    ret.put(e.getKey(), e.getValue());
                }
            }
            seen.put(val, ret);
            return ret;
        }

        private int compare(String ak, String bk) {
            boolean a = prefKeys.contains(ak);
            boolean b = prefKeys.contains(bk);
            if (a && !b) {
                return -1;
            }
            if (b && !a) {
                return 1;
            }
            if (a) {
                return prefKeys.indexOf(ak) - prefKeys.indexOf(bk);
            }
            return LocaleCompare.en(ak, bk);
        }
    }

    private static boolean isObj(@Nullable Object val) {
        return val instanceof Map;
    }

    // 1.1.1: val && !Array.isArray(val) && typeof val === 'object'
    private static @Nullable Object isObj111(@Nullable Object val) {
        return !Js.truthy(val) ? val : val instanceof Map;
    }
}
