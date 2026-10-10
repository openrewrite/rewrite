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
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JavaScript regular expression source as V8 reads it, as a {@link Pattern}, and {@code String.prototype.replace}.
 */
final class JsRegExp {

    private JsRegExp() {
    }

    /**
     * {@code new RegExp(source)}: null when V8 throws its SyntaxError.
     */
    static @Nullable Pattern compile(String source) {
        return V8RegExp.compile(source);
    }

    /**
     * {@code String.prototype.replace} with a replacement string, its {@code $} patterns included.
     */
    static String replace(String s, Pattern re, boolean global, String replacement) {
        return replace(s, re, global, m -> substitution(s, m, replacement));
    }

    /**
     * {@code String.prototype.replace} with a replacement function of the match.
     */
    static String replace(String s, Pattern re, boolean global, Function<Matcher, String> replacer) {
        Matcher m = re.matcher(s);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(s, last, m.start()).append(replacer.apply(m));
            last = m.end();
            if (!global) {
                break;
            }
        }
        return out.append(s.substring(last)).toString();
    }

    /**
     * {@code String.prototype.replace} of the first occurrence of a string.
     */
    static String replace(String s, String search, String replacement) {
        int at = s.indexOf(search);
        if (at < 0) {
            return s;
        }
        Matcher m = Pattern.compile(Pattern.quote(search)).matcher(s);
        m.find(at);
        return s.substring(0, at) + substitution(s, m, replacement) + s.substring(at + search.length());
    }

    // GetSubstitution
    private static String substitution(String s, Matcher m, String replacement) {
        StringBuilder out = new StringBuilder();
        int groups = m.groupCount();
        for (int i = 0; i < replacement.length(); i++) {
            char c = replacement.charAt(i);
            if (c != '$' || i + 1 == replacement.length()) {
                out.append(c);
                continue;
            }
            char next = replacement.charAt(i + 1);
            if (next == '$') {
                out.append('$');
                i++;
            } else if (next == '&') {
                out.append(m.group());
                i++;
            } else if (next == '`') {
                out.append(s, 0, m.start());
                i++;
            } else if (next == '\'') {
                out.append(s.substring(m.end()));
                i++;
            } else if (next >= '0' && next <= '9') {
                int one = next - '0';
                int two = i + 2 < replacement.length() && Character.isDigit(replacement.charAt(i + 2)) ?
                        one * 10 + replacement.charAt(i + 2) - '0' : -1;
                if (two >= 1 && two <= groups) {
                    out.append(group(m, two));
                    i += 2;
                } else if (one >= 1 && one <= groups) {
                    out.append(group(m, one));
                    i++;
                } else {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String group(Matcher m, int n) {
        String g = m.group(n);
        return g == null ? "" : g;
    }
}
