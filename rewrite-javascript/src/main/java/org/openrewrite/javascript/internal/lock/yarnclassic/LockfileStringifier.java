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
import java.util.Collections;
import java.util.Comparator;
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
     * The members every object literal inherits from {@code Object.prototype}: {@code priorities[key]} is truthy
     * for them and compares as {@code NaN}.
     */
    private static final Set<String> OBJECT_PROTOTYPE = new HashSet<>(Arrays.asList(
            "constructor", "__defineGetter__", "__defineSetter__", "hasOwnProperty", "__lookupGetter__",
            "__lookupSetter__", "isPrototypeOf", "propertyIsEnumerable", "toString", "valueOf", "__proto__",
            "toLocaleString"));

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
        String s = (String) str;
        return shouldWrapKey(s) ? Js.quote(s) : s;
    }

    static int priorityThenAlphaSort(String a, String b) {
        double pa = priority(a);
        double pb = priority(b);
        if (pa != 0 || pb != 0) {
            return (pa == 0 ? 100 : pa) > (pb == 0 ? 100 : pb) ? 1 : -1;
        }
        return Misc.sortAlpha(a, b);
    }

    // 0 when `priorities[key]` is falsy, NaN when it is an inherited member
    private static double priority(String key) {
        Integer p = PRIORITIES.get(key);
        return p != null ? p : OBJECT_PROTOTYPE.contains(key) ? Double.NaN : 0;
    }

    /**
     * {@code Array.prototype.sort} as V8 runs it on an array shorter than 64 elements: the leading run, then a
     * binary insertion of each remaining element. Any order of distinct keys comes out the same under a consistent
     * comparator; only an inherited member's name makes {@code priorityThenAlphaSort} inconsistent.
     */
    static List<String> sortKeys(List<String> keys) {
        List<String> a = new ArrayList<>(keys);
        int n = a.size();
        if (n < 2) {
            return a;
        }
        if (n >= 64) {
            for (String key : a) {
                if (OBJECT_PROTOTYPE.contains(key)) {
                    throw new UnknownOrder(key);
                }
            }
            a.sort(LockfileStringifier::priorityThenAlphaSort);
            return a;
        }
        Comparator<String> c = LockfileStringifier::priorityThenAlphaSort;
        int runHi = 1;
        if (c.compare(a.get(1), a.get(0)) < 0) {
            runHi = 2;
            while (runHi < n && c.compare(a.get(runHi), a.get(runHi - 1)) < 0) {
                runHi++;
            }
            Collections.reverse(a.subList(0, runHi));
        } else {
            runHi = 2;
            while (runHi < n && c.compare(a.get(runHi), a.get(runHi - 1)) >= 0) {
                runHi++;
            }
        }
        for (int start = runHi; start < n; start++) {
            String pivot = a.get(start);
            int left = 0;
            int right = start;
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (c.compare(pivot, a.get(mid)) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            for (int p = start; p > left; p--) {
                a.set(p, a.get(p - 1));
            }
            a.set(left, pivot);
        }
        return a;
    }

    /**
     * A key yarn sorts with an inconsistent comparator in an array too long for the port's sort to follow V8's.
     */
    static final class UnknownOrder extends RuntimeException {
        UnknownOrder(String key) {
            super("the order yarn writes the keys of a map holding \"" + key + "\" in");
        }
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
            if (val instanceof String || val instanceof Boolean || val instanceof Number) {
                lines.add(keyLine + " " + maybeWrap(val));
            } else {
                lines.add(keyLine + ":\n" + stringify(val, indent + "  ", false) + (topLevel ? "\n" : ""));
            }
            addedKeys.addAll(valKeys);
        }
        return indent + String.join("\n" + indent, lines);
    }

    /**
     * The module's default export: the entries with the header yarn writes above them.
     *
     * @param nodeVersion {@code process.version} of the Node yarn runs under, written with {@code enableVersions}.
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
