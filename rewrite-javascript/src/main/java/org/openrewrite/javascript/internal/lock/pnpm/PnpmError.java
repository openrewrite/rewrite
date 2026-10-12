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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Port of @pnpm/error {@code PnpmError} (pnpm 11.2.2): an error pnpm raises with a code, {@code ERR_PNPM_} and
 * the name it was given.
 */
@Getter
class PnpmError extends RuntimeException {

    private final String code;

    private @Nullable String hint;

    PnpmError(String code, String message) {
        this(code, message, null);
    }

    PnpmError(String code, String message, @Nullable String hint) {
        super(message);
        this.code = "ERR_PNPM_" + code;
        this.hint = hint;
    }

    // err.hint ??= hint, as pnpm adds one to an error it rethrows
    void hintIfNone(String hint) {
        if (this.hint == null) {
            this.hint = hint;
        }
    }

    /**
     * The URL a {@code FetchError}'s message names, {@code GET <url>: …}: from 11.9.0 without its credentials
     * ({@link #redactUrlCredentials}, {@link PnpmRelease#redactsUrlCredentialsInFetchErrors}), asked where it has any.
     */
    static String requestUrl(String url, PnpmRelease release) {
        String redacted = redactUrlCredentials(url);
        return redacted.equals(url) || !release.redactsUrlCredentialsInFetchErrors() ? url : redacted;
    }

    /**
     * {@code redactUrlCredentials} (11.9.0): the user info of every URL in {@code text} left out, an authority ending
     * at its first {@code /}, {@code ?}, {@code #} or whitespace and its user info at the last {@code @} before.
     */
    static String redactUrlCredentials(String text) {
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (cursor < text.length()) {
            int schemeSep = text.indexOf("://", cursor);
            if (schemeSep == -1) {
                return result.append(text, cursor, text.length()).toString();
            }
            int authorityStart = schemeSep + 3;
            result.append(text, cursor, authorityStart);
            cursor = authorityStart;
            if (schemeSep == 0 || !isSchemeTailChar(text.charAt(schemeSep - 1))) {
                continue;
            }
            int lastAt = -1;
            for (int i = authorityStart; i < text.length(); i++) {
                char code = text.charAt(i);
                if (code == '/' || code == '?' || code == '#' || isAsciiWhitespace(code)) {
                    break;
                }
                if (code == '@') {
                    lastAt = i;
                }
            }
            if (lastAt != -1) {
                cursor = lastAt + 1;
            }
        }
        return result.toString();
    }

    private static boolean isSchemeTailChar(char code) {
        return code >= '0' && code <= '9' || code >= 'A' && code <= 'Z' || code >= 'a' && code <= 'z';
    }

    private static boolean isAsciiWhitespace(char code) {
        return code == ' ' || code == '\t' || code == '\n' || code == 0x0B || code == '\f' || code == '\r';
    }
}
