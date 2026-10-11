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
import org.openrewrite.javascript.internal.lock.V8Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of yarn src/lockfile/stringify.js (yarn 1.22.22): the lockfile syntax yarn writes, every map's keys in
 * priority-then-alphabetical order and the keys that share one object on one line.
 */
final class LockfileStringifier {

    static final String YARN_VERSION = "1.22.22";

    private static final Pattern WRAP_CHARS = Pattern.compile("[:" + Js.WHITESPACE_CLASS + "\\n\\\\\",\\[\\]]");

    private static final Map<String, Integer> PRIORITIES = new HashMap<>();

    static {
        String[] names = {"name", "version", "uid", "resolved", "integrity", "registry", "dependencies"};
        for (int i = 0; i < names.length; i++) {
            PRIORITIES.put(names[i], i + 1);
        }
    }

    /**
     * The members every object literal inherits from {@code Object.prototype}, by the string {@code >} converts
     * {@code priorities[key]} to: two of them compare as strings, one beside a number as {@code NaN}.
     */
    private static final Map<String, String> OBJECT_PROTOTYPE = new HashMap<>();

    static {
        for (String name : Arrays.asList("__defineGetter__", "__defineSetter__", "hasOwnProperty", "__lookupGetter__",
                "__lookupSetter__", "isPrototypeOf", "propertyIsEnumerable", "toString", "valueOf", "toLocaleString")) {
            OBJECT_PROTOTYPE.put(name, "function " + name + "() { [native code] }");
        }
        OBJECT_PROTOTYPE.put("constructor", "function Object() { [native code] }");
        OBJECT_PROTOTYPE.put("__proto__", "[object Object]");
    }

    private LockfileStringifier() {
    }

    static boolean shouldWrapKey(String str) {
        return str.startsWith("true") ||
               str.startsWith("false") ||
               WRAP_CHARS.matcher(str).find() ||
               (!str.isEmpty() && str.charAt(0) >= '0' && str.charAt(0) <= '9') ||
               str.isEmpty() || !isAsciiLetter(str.charAt(0));
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    static String maybeWrap(Object str) {
        if (str instanceof Boolean) {
            return str.toString();
        }
        if (str instanceof Number) {
            return Js.numberToString(((Number) str).doubleValue());
        }
        String s = Js.string(str);
        return shouldWrapKey(s) ? Js.quote(s) : s;
    }

    static int priorityThenAlphaSort(String a, String b) {
        Object pa = priority(a);
        Object pb = priority(b);
        if (pa != null || pb != null) {
            return greaterThan(pa == null ? 100 : pa, pb == null ? 100 : pb) ? 1 : -1;
        }
        return Misc.sortAlpha(a, b);
    }

    // `priorities[key]`: its number, an inherited member's string, null when falsy
    private static @Nullable Object priority(String key) {
        Integer p = PRIORITIES.get(key);
        return p != null ? p : OBJECT_PROTOTYPE.get(key);
    }

    // `>` over a number or a member's string: two strings by code units; a string beside a number is NaN
    private static boolean greaterThan(Object a, Object b) {
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b) > 0;
        }
        return a instanceof Integer && b instanceof Integer && (Integer) a > (Integer) b;
    }

    static List<String> sortKeys(List<String> keys) {
        List<String> a = new ArrayList<>(keys);
        V8Sort.sort(a, LockfileStringifier::priorityThenAlphaSort);
        return a;
    }

    @SuppressWarnings("unchecked")
    private static String stringify(Object obj, String indent, boolean topLevel) {
        Map<String, @Nullable Object> map;
        if (obj instanceof Map) {
            map = (Map<String, @Nullable Object>) obj;
        } else if (obj instanceof List) {
            map = new LinkedHashMap<>();
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                map.put(String.valueOf(i), list.get(i));
            }
        } else {
            throw new JsError(JsError.TYPE_ERROR, "");
        }
        List<String> lines = new ArrayList<>();
        List<String> keys = sortKeys(Js.keys(map));
        Set<String> addedKeys = new HashSet<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object val = map.get(key);
            if (val == null || addedKeys.contains(key)) {
                continue;
            }
            List<String> valKeys = new ArrayList<>();
            valKeys.add(key);
            if (val instanceof Map || val instanceof List) {
                for (int j = i + 1; j < keys.size(); j++) {
                    if (val == map.get(keys.get(j))) {
                        valKeys.add(keys.get(j));
                    }
                }
            }
            valKeys.sort(Misc::sortAlpha);
            List<String> wrapped = new ArrayList<>();
            for (String valKey : valKeys) {
                wrapped.add(maybeWrap(valKey));
            }
            String keyLine = String.join(", ", wrapped);
            String type = Js.typeOf(val);
            if ("string".equals(type) || "boolean".equals(type) || "number".equals(type)) {
                lines.add(keyLine + " " + maybeWrap(val));
            } else {
                lines.add(keyLine + ":\n" + stringify(val, indent + "  ", false) + (topLevel ? "\n" : ""));
            }
            addedKeys.addAll(valKeys);
        }
        return indent + String.join("\n" + indent, lines);
    }

    /**
     * The module's default export: the entries with the header yarn writes above them, {@code nodeVersion} written with
     * {@code enableVersions}.
     */
    static String stringify(Map<String, @Nullable Object> obj, boolean noHeader, boolean enableVersions,
                            @Nullable String nodeVersion) {
        String val = stringify(obj, "", true);
        if (noHeader) {
            return val;
        }
        List<String> lines = new ArrayList<>();
        lines.add("# THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.");
        lines.add("# yarn lockfile v" + LockfileParser.LOCKFILE_VERSION);
        if (enableVersions) {
            lines.add("# yarn v" + YARN_VERSION);
            lines.add("# node " + nodeVersion);
        }
        lines.add("\n");
        lines.add(val);
        return String.join("\n", lines);
    }
}
