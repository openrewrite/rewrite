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
package org.openrewrite.python.internal.index;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipenv's environment variable expansion for {@code [[source]]} fields:
 * {@code os.path.expandvars} semantics, with unset variables left literal.
 */
final class EnvExpansion {
    private static final Pattern VAR = Pattern.compile("\\$(\\w+|\\{[^}]*})");

    // Userinfo additionally accepts pip's quoted form '${VAR}', stripping the quotes.
    private static final Pattern USERINFO_VAR = Pattern.compile("'\\$\\{([^}]*)}'|\\$(\\w+|\\{[^}]*})");

    private EnvExpansion() {
    }

    static String expandVars(String s, Environment env) {
        Matcher m = VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if (name.startsWith("{")) {
                name = name.substring(1, name.length() - 1);
            }
            String value = env.getenv(name);
            m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? m.group() : value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static boolean hasPlaceholder(String s) {
        return VAR.matcher(s).find();
    }

    /**
     * An expanded URL plus whether any placeholder survived expansion, judged on the
     * pre-encoding form (percent-encoding a partially expanded userinfo would
     * otherwise mask its remaining placeholders).
     */
    static final class Expansion {
        final String url;
        final boolean unresolvedPlaceholders;

        Expansion(String url, boolean unresolvedPlaceholders) {
            this.url = url;
            this.unresolvedPlaceholders = unresolvedPlaceholders;
        }
    }

    /**
     * Pipenv's {@code expand_url_credentials}: userinfo is env-expanded and, only when
     * expansion changed it, split on the first {@code :} and percent-encoded per part;
     * the rest of the URL gets plain expandvars. A URL without {@code $} passes through
     * untouched.
     */
    static Expansion expand(String url, Environment env) {
        if (url.indexOf('$') < 0) {
            return new Expansion(url, false);
        }
        int[] range = Urls.userinfoRange(url);
        if (range == null) {
            String expanded = expandVars(url, env);
            return new Expansion(expanded, hasPlaceholder(expanded));
        }
        String userinfo = url.substring(range[0], range[1]);
        String expanded = expandUserinfo(userinfo, env);
        String rest = expandVars(url.substring(range[1]), env);
        String encoded;
        if (expanded.equals(userinfo)) {
            // untouched credentials keep their exact bytes, unresolved placeholders included
            encoded = userinfo;
        } else {
            int colon = expanded.indexOf(':');
            encoded = colon < 0 ?
                    quote(expanded) :
                    quote(expanded.substring(0, colon)) + ":" + quote(expanded.substring(colon + 1));
        }
        return new Expansion(url.substring(0, range[0]) + encoded + rest,
                hasPlaceholder(url.substring(0, range[0]) + expanded + rest));
    }

    static String expandUrl(String url, Environment env) {
        return expand(url, env).url;
    }

    private static String expandUserinfo(String s, Environment env) {
        Matcher m = USERINFO_VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if (name == null) {
                name = m.group(2);
                if (name.startsWith("{")) {
                    name = name.substring(1, name.length() - 1);
                }
            }
            String value = env.getenv(name);
            m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? m.group() : value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * urllib's {@code quote(s, safe="")}: everything but unreserved characters is
     * encoded, including any pre-existing {@code %XX} escapes.
     */
    static String quote(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte value : bytes) {
            int b = value & 0xFF;
            if (isUnreserved(b)) {
                sb.append((char) b);
            } else {
                sb.append('%')
                        .append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)))
                        .append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
            }
        }
        return sb.toString();
    }

    static String percentDecode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                out.write((Character.digit(s.charAt(i + 1), 16) << 4) | Character.digit(s.charAt(i + 2), 16));
                i += 2;
            } else {
                byte[] chars = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                out.write(chars, 0, chars.length);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean isUnreserved(int b) {
        return (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9') ||
                b == '-' || b == '.' || b == '_' || b == '~';
    }

    private static boolean isHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
