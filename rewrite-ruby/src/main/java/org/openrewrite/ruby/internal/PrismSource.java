/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.ruby.internal;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;

/**
 * The one source text, held both as the bytes Prism parsed and as the string the LST is printed
 * from, with a translation between Prism's byte offsets and the string's char offsets.
 */
public class PrismSource {
    private final byte[] bytes;
    private final String text;
    private final Charset charset;

    /**
     * {@code null} when every byte maps to exactly one char, which is the common case and lets
     * {@link #charOffset(int)} short-circuit.
     */
    private final int @Nullable [] byteToChar;

    public PrismSource(String text, Charset charset) {
        this.text = text;
        this.charset = charset;
        this.bytes = text.getBytes(charset);
        this.byteToChar = bytes.length == text.length() ? null : buildMapping(text, charset, bytes.length);
    }

    private static int[] buildMapping(String text, Charset charset, int byteLength) {
        int[] mapping = new int[byteLength + 1];
        int byteOffset = 0;
        for (int charOffset = 0; charOffset < text.length(); ) {
            int codePoint = text.codePointAt(charOffset);
            int charCount = Character.charCount(codePoint);
            int byteCount = new String(Character.toChars(codePoint)).getBytes(charset).length;
            for (int i = 0; i < byteCount; i++) {
                mapping[byteOffset + i] = charOffset;
            }
            byteOffset += byteCount;
            charOffset += charCount;
        }
        mapping[byteLength] = text.length();
        return mapping;
    }

    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Prism refuses to close a heredoc whose terminator is the last line of a file with no trailing
     * newline, so it is always handed a newline-terminated copy. Appending at the very end leaves
     * every offset into the original text unchanged.
     */
    public byte[] getParseBytes() {
        if (bytes.length > 0 && bytes[bytes.length - 1] == '\n') {
            return bytes;
        }
        byte[] terminated = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, terminated, 0, bytes.length);
        terminated[bytes.length] = '\n';
        return terminated;
    }

    public String getText() {
        return text;
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * @param byteOffset A Prism offset into {@link #getBytes()}.
     * @return The corresponding index into {@link #getText()}.
     */
    public int charOffset(int byteOffset) {
        if (byteToChar == null) {
            return byteOffset;
        }
        if (byteOffset >= byteToChar.length) {
            return text.length();
        }
        return byteToChar[byteOffset];
    }
}
