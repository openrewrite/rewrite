/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import org.openrewrite.internal.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EncodingDetectingInputStream extends InputStream {
    private final InputStream inputStream;

    @Nullable
    private Charset charset;

    private boolean charsetBomMarked;

    /**
     * Last byte read
     */
    private int prev;
    private int prev2;
    private int prev3;

    public EncodingDetectingInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Charset getCharset() {
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public boolean isCharsetBomMarked() {
        return charsetBomMarked;
    }

    @Override
    public int read() throws IOException {
        int aByte = inputStream.read();

        // if we haven't yet determined a charset...
        if (charset == null && aByte != -1) {
            // The first 128 characters in ASCII share the same bytes and are defaulted to UTF-8.
            if (prev3 == 0xC3 && prev2 == 0xAF && prev == 0xC2) {
                charsetBomMarked = true;
                charset = StandardCharsets.UTF_8;
            } else {
                if (0x80 <= aByte && (!(0xC2 <= aByte && aByte <= 0xEF) && prev == 0) ||
                        ((0xC2 <= prev && prev <= 0xDF && notUtfHighByte(aByte)) || // 2 byte sequence
                        (0xE0 <= prev && prev <= 0xEF && notUtfHighByte(aByte)) || // 3 byte sequence
                        (0xF0 <= prev && prev <= 0xF7 && notUtfHighByte(aByte)) || // 4 byte sequence
                        (0xF8 <= prev && prev <= 0xFB && notUtfHighByte(aByte)) || // 5 byte sequence
                        (0xFC <= prev && prev <= 0xFD && notUtfHighByte(aByte)))) { // 6 byte sequence
                    charset = Charset.forName("Windows-1252");
                }
            }

            prev3 = prev2;
            prev2 = prev;
            prev = aByte;
        }

        return aByte;
    }

    public String readFully() {
        try (InputStream is = this) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }

            byte[] bytes = bos.toByteArray();
            return new String(bytes, 0, bytes.length, getCharset());
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private boolean notUtfHighByte(int b) {
        return !(0x80 <= b && b <= 0xBF);
    }
}
