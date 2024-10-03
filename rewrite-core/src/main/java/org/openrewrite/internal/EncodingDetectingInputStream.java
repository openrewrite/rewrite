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

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EncodingDetectingInputStream extends InputStream {
    private static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final InputStream inputStream;

    @Nullable
    private Charset charset;

    private boolean bomChecked;
    private boolean charsetBomMarked;

    /**
     * Last byte read
     */
    private int prev;
    private int prev2;

    boolean maybeTwoByteSequence = false;
    boolean maybeThreeByteSequence = false;
    boolean maybeFourByteSequence = false;

    public EncodingDetectingInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.charset = null;
    }

    public EncodingDetectingInputStream(InputStream inputStream, @Nullable Charset charset) {
        this.inputStream = inputStream;
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public boolean isCharsetBomMarked() {
        return charsetBomMarked;
    }

    @Override
    public int read() throws IOException {
        int read;
        if (!bomChecked) {
            read = checkAndSkipUtf8Bom();
            if (charsetBomMarked) {
                read = inputStream.read();
            }
        } else {
            read = inputStream.read();
        }


        // if we haven't yet determined a charset...
        if (read == -1) {
            if (charset == null) {
                if (maybeTwoByteSequence || maybeThreeByteSequence || maybeFourByteSequence) {
                    charset = WINDOWS_1252;
                } else {
                    charset = StandardCharsets.UTF_8;
                }
            }
        } else if (charset == null) {
            guessCharset(read);
        }
        return read;
    }

    private void guessCharset(int aByte) {
        if (utf8TwoByteSequence(aByte)) {
            maybeTwoByteSequence = true;
        } else if (utf8ThreeByteSequence(aByte)) {
            maybeThreeByteSequence = true;
        } else if (utf8FourByteSequence(aByte)) {
            maybeFourByteSequence = true;
        } else if (maybeTwoByteSequence) {
            if (!utf8SequenceEnd(aByte)) {
                charset = WINDOWS_1252;
            } else {
                maybeTwoByteSequence = false;
                prev = -1;
            }
        } else if (maybeThreeByteSequence) {
            if (!utf8SequenceEnd(aByte)) {
                charset = WINDOWS_1252;
            }

            if (utf8SequenceEnd(prev) && utf8SequenceEnd(aByte)) {
                maybeThreeByteSequence = false;
                prev = -1;
            }
        } else if (maybeFourByteSequence) {
            if (utf8SequenceEnd(prev2) && utf8SequenceEnd(prev) && !utf8SequenceEnd(aByte) || utf8SequenceEnd(prev) && !utf8SequenceEnd(aByte) || !utf8SequenceEnd(aByte)) {
                charset = WINDOWS_1252;
            }

            if (utf8SequenceEnd(prev2) && utf8SequenceEnd(prev) && utf8SequenceEnd(aByte)) {
                maybeFourByteSequence = false;
                prev = -1;
            }
        } else if (utf8SequenceEnd(aByte)) {
            charset = WINDOWS_1252;
        }

        prev2 = prev;
        prev = aByte;
    }

    public String readFully() {
        try (InputStream is = this) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream() {
                @Override
                public synchronized String toString() {
                    return new String(buf, 0, count, getCharset());
                }
            };
            byte[] buffer = new byte[4096];
            int n;
            // Note that `is` is this, so the BOM will be checked in `read()`
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }

            return bos.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int checkAndSkipUtf8Bom() throws IOException {
        // `Files#newInputStream()` does not need to support mark/reset, so one at the time...
        bomChecked = true;
        int read = inputStream.read();
        if ((byte) read != UTF8_BOM[0]) {
            return read;
        }
        read = inputStream.read();
        if ((byte) read != UTF8_BOM[1]) {
            return read;
        }
        read = inputStream.read();
        if ((byte) read != UTF8_BOM[2]) {
            return read;
        }
        charsetBomMarked = true;
        charset = StandardCharsets.UTF_8;
        // return anything other that -1
        return -2;
    }

    // The first byte of a UTF-8 two byte sequence is between 0xC0 - 0xDF.
    private boolean utf8TwoByteSequence(int b) {
        return 0xC0 <= b && b <= 0xDF;
    }

    // The first byte of a UTF-8 three byte sequence is between 0xE0 - 0xEF.
    private boolean utf8ThreeByteSequence(int b) {
        return 0xE0 <= b && b <= 0xEF;
    }

    // The first byte of a UTF-8 four byte sequence is between 0xF0 - 0xF7.
    private boolean utf8FourByteSequence(int b) {
        return 0xF0 <= b && b <= 0xF7;
    }

    // A UTF-8 byte sequence must end between 0x80 - 0xBF.
    private boolean utf8SequenceEnd(int b) {
        return 0x80 <= b && b <= 0xBF;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            inputStream.close();
        }
    }
}
