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

import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of extglob 0.3.2 as yarn 1.22.22 bundles it for micromatch 2.3.11, which asks it for the regex source with
 * special characters escaped.
 */
final class Extglob {

    private static final Pattern REVERSED_NEGATION = Pattern.compile("!\\(([^\\w*()])");
    private static final Pattern EXTENSION_NEGATION = Pattern.compile("([*/])\\.!\\([*]\\)");
    private static final Pattern EXTGLOB = Pattern.compile("(\\\\?[@?!+*$]\\\\?)(\\(([^()]*?)\\))");

    private Extglob() {
    }

    static String extglob(String str) {
        str = V8RegExp.replace(str, REVERSED_NEGATION, true, m -> m.group(1) + "!(");
        str = V8RegExp.replace(str, EXTENSION_NEGATION, true, m -> "/".equals(m.group(1)) ?
                escape("\\/[^.]+") : escape("[^.]+"));

        Map<String, String> o = new LinkedHashMap<>();
        int i = 0;
        Matcher m;
        while ((m = EXTGLOB.matcher(str)).find()) {
            String id = "__EXTGLOB_" + (i++) + "__";
            o.put(id, wrap(m.group(3), m.group(1)));
            str = Js.splitJoin(str, m.group(), id);
        }

        List<String> keys = new ArrayList<>(o.keySet());
        for (int k = keys.size() - 1; k >= 0; k--) {
            str = Js.splitJoin(str, keys.get(k), o.get(keys.get(k)));
        }
        return Js.splitJoin(str, ".", "\\.");
    }

    private static String wrap(String inner, String prefix) {
        inner = escape(inner);
        switch (prefix) {
            case "!":
                return "(?!" + inner + ")[^/]%%%~";
            case "@":
                return "(?:" + inner + ")";
            case "+":
                return "(?:" + inner + ")+";
            case "*":
                return "(?:" + inner + ")%%";
            case "?":
                return "(?:" + inner + "|)";
            default:
                return inner;
        }
    }

    private static String escape(String str) {
        return Js.splitJoin(Js.splitJoin(str, "*", "[^/]%%%~"), ".", "\\.");
    }
}
