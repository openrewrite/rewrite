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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of json-stringify-nice 1.1.4 over {@code JSON.stringify}: scalars before objects, preferred keys
 * first, the rest in {@code localeCompare('en')} order.
 */
final class JsonStringifyNice {

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
        String gap = gap(space);
        StringBuilder out = new StringBuilder();
        serialize(obj, new Sorter(prefKeys), gap, "", out, new ArrayList<>());
        return out.toString() + (Js.truthy(space) ? "\n" : "");
    }

    private static String gap(@Nullable Object space) {
        if (space instanceof Number) {
            double d = ((Number) space).doubleValue();
            int n = Double.isNaN(d) ? 0 : (int) Math.min(10, Math.floor(d));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append(' ');
            }
            return sb.toString();
        }
        if (space instanceof String) {
            String s = (String) space;
            return s.length() > 10 ? s.substring(0, 10) : s;
        }
        return "";
    }

    private static final class Sorter {
        private final List<String> prefKeys;
        private final Map<Object, Map<String, Object>> seen = new IdentityHashMap<>();

        Sorter(List<String> prefKeys) {
            this.prefKeys = prefKeys;
        }

        @Nullable
        Object apply(@Nullable Object val) {
            if (!isObj(val)) {
                return val;
            }
            Map<String, Object> cached = seen.get(val);
            if (cached != null) {
                return cached;
            }
            List<Map.Entry<String, Object>> entries = new ArrayList<>(Js.entries(val));
            entries.sort((a, b) -> isObj(a.getValue()) == isObj(b.getValue()) ? compare(a.getKey(), b.getKey()) :
                    isObj(a.getValue()) ? 1 : -1);
            Map<String, Object> ret = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : entries) {
                ret.put(e.getKey(), e.getValue());
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

    @SuppressWarnings("unchecked")
    private static void serialize(@Nullable Object raw, Sorter sorter, String gap, String indent, StringBuilder out,
                                  List<Object> stack) {
        Object value = sorter.apply(raw);
        if (value == null || value == Js.UNDEFINED || value == Js.HOLE) {
            out.append("null");
        } else if (value instanceof String) {
            out.append(Js.quote((String) value));
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            out.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : Js.numberToString((Number) value));
        } else if (value instanceof List) {
            serializeArray((List<Object>) value, sorter, gap, indent, out, stack);
        } else if (value instanceof Map) {
            serializeObject((Map<String, Object>) value, sorter, gap, indent, out, stack);
        } else {
            out.append(Js.quote(String.valueOf(value)));
        }
    }

    private static void enter(Object value, List<Object> stack) {
        for (Object o : stack) {
            if (o == value) {
                throw new IllegalArgumentException("Converting circular structure to JSON");
            }
        }
        stack.add(value);
    }

    private static void serializeObject(Map<String, Object> object, Sorter sorter, String gap, String indent,
                                        StringBuilder out, List<Object> stack) {
        enter(object, stack);
        String stepback = indent;
        indent = indent + gap;
        out.append('{');
        boolean any = false;
        for (Map.Entry<String, Object> entry : Js.entries(object)) {
            Object v = entry.getValue();
            if (v == Js.UNDEFINED) {
                continue;
            }
            out.append(any ? "," : "");
            if (!gap.isEmpty()) {
                out.append('\n').append(indent);
            }
            out.append(Js.quote(entry.getKey())).append(gap.isEmpty() ? ":" : ": ");
            serialize(v, sorter, gap, indent, out, stack);
            any = true;
        }
        if (any && !gap.isEmpty()) {
            out.append('\n').append(stepback);
        }
        out.append('}');
        stack.remove(stack.size() - 1);
    }

    private static void serializeArray(List<Object> array, Sorter sorter, String gap, String indent,
                                       StringBuilder out, List<Object> stack) {
        enter(array, stack);
        String stepback = indent;
        indent = indent + gap;
        out.append('[');
        for (int i = 0; i < array.size(); i++) {
            out.append(i > 0 ? "," : "");
            if (!gap.isEmpty()) {
                out.append('\n').append(indent);
            }
            serialize(array.get(i), sorter, gap, indent, out, stack);
        }
        if (!array.isEmpty() && !gap.isEmpty()) {
            out.append('\n').append(stepback);
        }
        out.append(']');
        stack.remove(stack.size() - 1);
    }
}
