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

import lombok.Getter;
import org.yaml.snakeyaml.events.Event;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Maintains a sliding buffer of characters used to determine format prefixes of
 * YAML AST elements.
 */
class FormatPreservingReader extends Reader {
    private final Reader delegate;

    // whether the source has multi bytes (> 2 bytes) unicode characters
    private final boolean hasMultiBytesUnicode;
    // Characters index to source index mapping, valid only when `hasMultiBytesUnicode` is true.
    // Snake yaml parser is based on characters index and reader is based on source index. If there are any >2 bytes
    // unicode characters in source code, it will make the index mismatch.
    private final int[] indexes;

    private ArrayList<Character> buffer = new ArrayList<>();

    @Getter
    private int bufferIndex = 0;

    FormatPreservingReader(String source) {
        this.delegate = new StringReader(source);

        boolean hasUnicodes = false;
        int[] pos = new int[source.length() + 1];

        int cursor = 0;
        int i = 1;
        pos[0] = 0;

        while (cursor < source.length()) {
            int newCursor = source.offsetByCodePoints(cursor, 1);
            if (newCursor > cursor + 1) {
                hasUnicodes = true;
            }
            pos[i++] = newCursor;
            cursor = newCursor;
        }

        hasMultiBytesUnicode = hasUnicodes;
        indexes = hasMultiBytesUnicode ? pos : new int[]{};
    }

    String prefix(int lastEnd, int startIndex) {
        if (hasMultiBytesUnicode) {
            lastEnd = indexes[lastEnd];
            startIndex = indexes[startIndex];
        }

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
        if (end < start) {
            return "";
        }

        if (hasMultiBytesUnicode) {
            start = indexes[start];
            end = indexes[end + 1] - 1;
        }

        int length = end - start + 1;
        char[] readBuff = new char[length];
        for (int i = 0; i < length; i++) {
            int bufferOffset = start + i - bufferIndex;
            readBuff[i] = buffer.get(bufferOffset);
        }
        return new String(readBuff);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = delegate.read(cbuf, off, len);
        if (read > 0) {
            buffer.ensureCapacity(buffer.size() + read);
            for (int i = 0; i < read; i++) {
                char e = cbuf[i];
                buffer.add(e);
            }
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
