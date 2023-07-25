/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.yaml;

import org.openrewrite.internal.lang.NonNull;
import org.yaml.snakeyaml.events.Event;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Maintains a sliding buffer of characters used to determine format prefixes of
 * YAML AST elements.
 */
class FormatPreservingReader extends Reader {

    private final Reader delegate;

    private ArrayList<Character> buffer = new ArrayList<>();
    private int bufferIndex = 0;

    FormatPreservingReader(Reader delegate) {
        this.delegate = delegate;
    }

    String prefix(int lastEnd, int startIndex) {
        assert lastEnd <= startIndex;

        int prefixLen = startIndex - lastEnd;
        if (prefixLen > 0) {
            char[] prefix = new char[prefixLen];

            for (int i = 0; i < prefixLen; i++) {
                prefix[i] = buffer.get(lastEnd - bufferIndex + i);
            }
            if (lastEnd > bufferIndex) {
                buffer = new ArrayList<>(buffer.subList(lastEnd - bufferIndex, buffer.size()));
                bufferIndex = lastEnd;
            }

            return new String(prefix);
        }
        return "";
    }

    public String prefix(int lastEnd, Event event) {
        return prefix(lastEnd, event.getStartMark().getIndex());
    }

    public String readStringFromBuffer(int start, int end) {
        int length = end - start + 1;
        char[] readBuff = new char[length];
        for (int i = 0; i < length; i++) {
            int bufferOffset = start + i - bufferIndex;
            readBuff[i] = buffer.get(bufferOffset);
        }
        return new String(readBuff);
    }

    @Override
    public int read(@NonNull char[] cbuf, int off, int len) throws IOException {
        int read = delegate.read(cbuf, off, len);
        if (read > 0) {
            buffer.ensureCapacity(buffer.size() + read);
            for (int i = 0; i < read; i++) {
                char e = cbuf[i];
                if (Character.UnicodeBlock.of(e) != Character.UnicodeBlock.BASIC_LATIN) {
                    throw new IllegalArgumentException("Only ASCII characters are supported for now");
                }
                buffer.add(e);
            }
        }
        return read;
    }

    /**
     * Processes a stream of bytes, and returns a Stream of Unicode codepoints
     * associated with the characters derived from that byte stream.
     *
     * @param bais ByteArrayInputStream to be processed.
     * @return A stream of Unicode codepoints derived from UTF-8 characters in the supplied stream.
     */
//    private static Stream<Integer> processByteStream(ByteArrayInputStream bais) {
//
//        int nextByte = 0;
//        byte b = 0;
//        byte[] utf8Bytes = null;
//        int byteCount = 0;
//        List<Integer> codePoints = new ArrayList<>();
//
//        while ((nextByte = bais.read()) != -1) {
//            b = (byte) nextByte;
//            byteCount = Main.getByteCount(b);
//            utf8Bytes = new byte[byteCount];
//            utf8Bytes[0] = (byte) nextByte;
//            for (int i = 1; i < byteCount; i++) { // Get any subsequent bytes for this UTF-8 character.
//                nextByte = bais.read();
//                utf8Bytes[i] = (byte) nextByte;
//            }
//            int codePoint = new String(utf8Bytes, StandardCharsets.UTF_8).codePointAt(0);
//            codePoints.add(codePoint);
//        }
//        return codePoints.stream();
//    }

    /**
     * Returns the number of bytes in a UTF-8 character based on the bit pattern
     * of the supplied byte. The only valid values are 1, 2 3 or 4. If the
     * byte has an invalid bit pattern an IllegalArgumentException is thrown.
     *
     * @param b The first byte of a UTF-8 character.
     * @return The number of bytes for this UTF-* character.
     * @throws IllegalArgumentException if the bit pattern is invalid.
     */
    private static int getByteCount(byte b) throws IllegalArgumentException {
        if ((b >= 0)) return 1;                                             // Pattern is 0xxxxxxx.
        if ((b >= (byte) 0b11000000) && (b <= (byte) 0b11011111)) return 2; // Pattern is 110xxxxx.
        if ((b >= (byte) 0b11100000) && (b <= (byte) 0b11101111)) return 3; // Pattern is 1110xxxx.
        if ((b >= (byte) 0b11110000) && (b <= (byte) 0b11110111)) return 4; // Pattern is 11110xxx.
        throw new IllegalArgumentException(); // Invalid first byte for UTF-8 character.
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
