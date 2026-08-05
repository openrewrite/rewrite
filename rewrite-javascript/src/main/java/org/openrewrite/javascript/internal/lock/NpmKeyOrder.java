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
package org.openrewrite.javascript.internal.lock;

import java.util.Arrays;
import java.util.List;

/**
 * Reproduces the key ordering npm applies when it serializes {@code package-lock.json}, so an inserted entry
 * lands byte-exactly where {@code npm install} would place it. npm sorts through {@code json-stringify-nice}:
 * the preference list {@link #SW_KEY_ORDER} first, then the rest by JS {@code localeCompare(_, 'en')} (ICU
 * collation, unlike Java's {@code compareTo}/{@code Collator}), which over the npm name domain reduces to the
 * per-character weights below, validated byte-identical against V8. The value-kind partition (objects after
 * scalars) is handled by the patcher at the insertion site; this class orders keys within a partition.
 */
final class NpmKeyOrder {

    /** The preference list npm passes to {@code json-stringify-nice}. */
    private static final List<String> SW_KEY_ORDER = Arrays.asList(
            "name", "version", "lockfileVersion", "resolved", "integrity", "requires", "packages", "dependencies");

    private NpmKeyOrder() {
    }

    /** Order two object keys exactly as npm's lock serializer would within one value-kind partition. */
    static int compareKeys(String a, String b) {
        int ai = SW_KEY_ORDER.indexOf(a);
        int bi = SW_KEY_ORDER.indexOf(b);
        if (ai >= 0 && bi >= 0) {
            return Integer.compare(ai, bi);
        }
        if (ai >= 0) {
            return -1;
        }
        if (bi >= 0) {
            return 1;
        }
        return localeCompareEn(a, b);
    }

    /** A weight-based replica of V8 {@code String.prototype.localeCompare(other, 'en')} over npm names. */
    private static int localeCompareEn(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            int d = primaryWeight(a.charAt(i)) - primaryWeight(b.charAt(i));
            if (d != 0) {
                return d < 0 ? -1 : 1;
            }
        }
        if (a.length() != b.length()) {
            return a.length() < b.length() ? -1 : 1;
        }
        // Primary-equal: lowercase sorts before uppercase, then by code point.
        for (int i = 0; i < a.length(); i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca != cb) {
                boolean al = ca == Character.toLowerCase(ca);
                boolean bl = cb == Character.toLowerCase(cb);
                if (al != bl) {
                    return al ? -1 : 1;
                }
                return ca < cb ? -1 : 1;
            }
        }
        return 0;
    }

    private static int primaryWeight(char ch) {
        switch (ch) {
            case '/': return 0;
            case '@': return 1;
            case '_': return 2;
            case '-': return 3;
            case '.': return 4;
            default:
                if (ch >= '0' && ch <= '9') {
                    return 10 + (ch - '0');
                }
                char lower = Character.toLowerCase(ch);
                if (lower >= 'a' && lower <= 'z') {
                    return 100 + (lower - 'a');
                }
                return 500 + ch;
        }
    }
}
