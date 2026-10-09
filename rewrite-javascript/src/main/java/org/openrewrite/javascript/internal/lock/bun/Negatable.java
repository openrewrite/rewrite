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
package org.openrewrite.javascript.internal.lock.bun;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of bun's src/install/npm.zig {@code Negatable} and its os, cpu and libc tables (bun 1.3.10): a bit set of the
 * values a list allows, named in {@code ComptimeStringMap} order (length, then bytes), as {@link #toJson} writes.
 */
final class Negatable {

    static final class Kind {
        final Map<String, Integer> nameMap;
        final int allValue;

        private Kind(String... names) {
            Map<String, Integer> declared = new LinkedHashMap<>();
            int all = 0;
            for (int i = 0; i < names.length; i++) {
                declared.put(names[i], 1 << (i + 1));
                all |= 1 << (i + 1);
            }
            this.allValue = all;
            this.nameMap = new LinkedHashMap<>();
            declared.entrySet().stream()
                    .sorted((a, b) -> a.getKey().length() != b.getKey().length() ?
                            Integer.compare(a.getKey().length(), b.getKey().length()) :
                            Strings.order(a.getKey(), b.getKey()))
                    .forEach(e -> nameMap.put(e.getKey(), e.getValue()));
        }
    }

    static final Kind OPERATING_SYSTEM = new Kind("aix", "darwin", "freebsd", "linux", "openbsd", "sunos", "win32",
            "android");
    static final Kind ARCHITECTURE = new Kind("arm", "arm64", "ia32", "mips", "mipsel", "ppc", "ppc64", "s390", "s390x",
            "x32", "x64");
    static final Kind LIBC = new Kind("glibc", "musl");

    static final int NONE = 0;

    private final Kind kind;
    private int added;
    private int removed;
    private boolean hadWildcard;
    private boolean hadUnrecognizedValues;

    private Negatable(Kind kind) {
        this.kind = kind;
    }

    int combine() {
        int add = hadWildcard ? kind.allValue : added;
        if (add == 0 && removed == 0) {
            return hadUnrecognizedValues ? NONE : kind.allValue;
        }
        if (add == 0) {
            return kind.allValue & ~removed;
        }
        if (removed == 0) {
            return add;
        }
        return add & ~removed;
    }

    void apply(String str) {
        if (str.isEmpty()) {
            return;
        }
        if ("any".equals(str)) {
            hadWildcard = true;
            return;
        }
        if ("none".equals(str)) {
            hadUnrecognizedValues = true;
            return;
        }
        boolean isNot = str.charAt(0) == '!';
        Integer field = kind.nameMap.get(str.substring(isNot ? 1 : 0));
        if (field == null) {
            if (!isNot) {
                hadUnrecognizedValues = true;
            }
            return;
        }
        if (isNot) {
            removed |= field;
        } else {
            added |= field;
        }
    }

    static int fromJson(Kind kind, Json.Expr expr) {
        Negatable n = new Negatable(kind);
        if (expr.isArray()) {
            for (Json.Expr item : expr.items()) {
                String value = item.asString();
                if (value != null) {
                    n.apply(value);
                }
            }
        } else if (expr.isString()) {
            n.apply(expr.asString());
        }
        return n.combine();
    }

    static void toJson(Kind kind, int field, StringBuilder out) {
        if (field == NONE) {
            out.append("\"none\"");
            return;
        }
        int removedCount = 0;
        for (int value : kind.nameMap.values()) {
            if ((field & value) == 0) {
                removedCount++;
            }
        }
        int included = kind.nameMap.size() - removedCount;
        boolean printIncluded = removedCount > kind.nameMap.size() - removedCount;
        boolean one = printIncluded && included == 1 || !printIncluded && removedCount == 1;
        if (!one) {
            out.append("[ ");
        }
        for (Map.Entry<String, Integer> kv : kind.nameMap.entrySet()) {
            boolean has = (field & kv.getValue()) != 0;
            if (has && printIncluded) {
                out.append('"').append(kv.getKey()).append('"');
                if (one) {
                    return;
                }
                out.append(", ");
            } else if (!has && !printIncluded) {
                out.append("\"!").append(kv.getKey()).append('"');
                if (one) {
                    return;
                }
                out.append(", ");
            }
        }
        out.append(']');
    }
}
