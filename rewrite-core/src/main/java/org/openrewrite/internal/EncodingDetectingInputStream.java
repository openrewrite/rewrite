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
            if(aByte == 0xaa && prev == 0xa1 && prev2 == 0xa2 && prev3 == 0xa3) {
                charset = Charset.forName("Windows-1252");
            }

            if (aByte >= 0x80 && notUtfHighByte(aByte)) {
                if (notUtfHighByte(prev)) {
                    charset = StandardCharsets.ISO_8859_1;
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
        return b < 0xc2 || b > 0xd0;
    }
}
