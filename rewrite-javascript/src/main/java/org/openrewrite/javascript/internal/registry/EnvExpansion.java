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
package org.openrewrite.javascript.internal.registry;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * npm's {@code ${VAR}} expansion for npmrc values, extended with the
 * {@code ${VAR:-default}} form documented for npmrc. An unset variable with no default is
 * left literal and flags the value as unresolved, so the client can refuse to fetch it
 * rather than request a half-expanded URL.
 */
final class EnvExpansion {
    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]+)}");

    private EnvExpansion() {
    }

    /**
     * An expanded value plus whether any placeholder survived expansion (an unset variable
     * without a default).
     */
    static final class Expansion {
        final @Nullable String value;
        final boolean unresolvedPlaceholders;

        Expansion(@Nullable String value, boolean unresolvedPlaceholders) {
            this.value = value;
            this.unresolvedPlaceholders = unresolvedPlaceholders;
        }
    }

    static Expansion expand(@Nullable String s, Environment env) {
        if (s == null || s.indexOf('$') < 0) {
            return new Expansion(s, false);
        }
        Matcher m = VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        boolean unresolved = false;
        while (m.find()) {
            String inner = m.group(1);
            String name = inner;
            String def = null;
            int dash = inner.indexOf(":-");
            if (dash >= 0) {
                name = inner.substring(0, dash);
                def = inner.substring(dash + 2);
            }
            String value = env.getenv(name);
            String replacement;
            if (value != null) {
                replacement = value;
            } else if (def != null) {
                replacement = def;
            } else {
                replacement = m.group();
                unresolved = true;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return new Expansion(sb.toString(), unresolved);
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

    private static boolean isHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
