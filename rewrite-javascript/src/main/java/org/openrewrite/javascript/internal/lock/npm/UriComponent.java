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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * ECMAScript's {@code encodeURIComponent} and {@code decodeURIComponent}.
 */
public final class UriComponent {

    private static final String UNRESERVED = "-_.!~*'()";

    private UriComponent() {
    }

    /**
     * @throws IllegalArgumentException where JavaScript throws {@code URIError}.
     */
    public static String decodeURIComponent(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c != '%') {
                out.append(c);
                i++;
                continue;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (i < s.length() && s.charAt(i) == '%') {
                if (i + 2 >= s.length()) {
                    throw new IllegalArgumentException("URI malformed");
                }
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw new IllegalArgumentException("URI malformed");
                }
                bytes.write(hi * 16 + lo);
                i += 3;
            }
            try {
                out.append(StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes.toByteArray())));
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException("URI malformed", e);
            }
        }
        return out.toString();
    }

    /**
     * @throws IllegalArgumentException for a lone surrogate, where JavaScript throws {@code URIError}.
     */
    public static String encodeURIComponent(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                UNRESERVED.indexOf(c) >= 0) {
                out.append(c);
                continue;
            }
            int cp;
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1))) {
                    throw new IllegalArgumentException("URI malformed");
                }
                cp = Character.toCodePoint(c, s.charAt(++i));
            } else if (Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("URI malformed");
            } else {
                cp = c;
            }
            for (byte b : new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8)) {
                out.append('%').append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)))
                        .append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
            }
        }
        return out.toString();
    }
}
