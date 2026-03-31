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
     * Number of UTF-8 continuation bytes (0x80-0xBF) still expected
     * to complete the current multi-byte sequence. Zero when idle.
     */
    int remainingContinuationBytes = 0;

    public EncodingDetectingInputStream(InputStream inputStream) {
        this(inputStream, null);
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
            if (charset == null || charset == StandardCharsets.UTF_8) {
                read = checkAndSkipUtf8Bom();
                if (charsetBomMarked) {
                    read = inputStream.read();
                }
            } else {
                bomChecked = true;
                read = inputStream.read();
            }
        } else {
            read = inputStream.read();
        }


        // if we haven't yet determined a charset...
        if (read == -1) {
            if (charset == null) {
                if (remainingContinuationBytes > 0) {
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

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (charset == null) {
            // we need to read the bytes one-by-one to determine the encoding
            return super.read(b, off, len);
        } else if (charset == StandardCharsets.UTF_8 && !bomChecked) {
            int read = checkAndSkipUtf8Bom();
            if (read == -1) {
                return -1;
            } else if (!charsetBomMarked) {
                b[off++] = (byte) read;
            }
            read = inputStream.read(b, off, len - 1);
            return read == -1 ? charsetBomMarked ? -1 : 1 : (charsetBomMarked ? 0 : 1) + read;
        } else {
            return inputStream.read(b, off, len);
        }
    }

    private void guessCharset(int aByte) {
        if (remainingContinuationBytes > 0) {
            if (aByte >= 0x80 && aByte <= 0xBF) {
                remainingContinuationBytes--;
            } else {
                charset = WINDOWS_1252;
            }
        } else if (aByte <= 0x7F) {
            // ASCII — valid, nothing to track
        } else if (aByte >= 0xC2 && aByte <= 0xDF) {
            remainingContinuationBytes = 1;
        } else if (aByte >= 0xE0 && aByte <= 0xEF) {
            remainingContinuationBytes = 2;
        } else if (aByte >= 0xF0 && aByte <= 0xF4) {
            remainingContinuationBytes = 3;
        } else {
            // 0x80-0xBF (bare continuation), 0xC0-0xC1 (overlong),
            // 0xF5-0xFF (above max Unicode) — all invalid UTF-8
            charset = WINDOWS_1252;
        }
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
            while ((n = is.read(buffer, 0, buffer.length)) != -1) {
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



    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            inputStream.close();
        }
    }
}
