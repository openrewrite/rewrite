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

import lombok.Data;
import lombok.Getter;
import org.openrewrite.internal.lang.NonNull;
import org.yaml.snakeyaml.events.Event;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a sliding buffer of characters used to determine format prefixes of
 * YAML AST elements.
 */
class FormatPreservingReader extends Reader {
    private final Reader delegate;
    private final Map<Integer, Range> indexMapping = new HashMap<>();
    private final int characterCount;

    @Data
    public static class Range {
        private final int start;
        private final int end;
    }

    private ArrayList<Character> buffer = new ArrayList<>();

    @Getter
    private int bufferIndex = 0;

    FormatPreservingReader(String source) {
        this.delegate = new StringReader(source);

        int index = 0;
        int cursor = 0;
        while (cursor < source.length()) {
            int newCursor = source.offsetByCodePoints(cursor, 1);
            int start = cursor;
            int offset = 0;

            if (newCursor > cursor + 1) {
                offset = newCursor - cursor - 1;
            }

            int end = start + offset;
            indexMapping.put(index, new Range(start, end));
            index++;
            cursor = newCursor;
        }

        indexMapping.put(index, new Range(cursor, cursor));
        characterCount = index;
    }

    String prefix(int lastEnd, int startIndex) {
        if (lastEnd >= characterCount) {
            return "";
        }

        lastEnd = indexMapping.get(lastEnd).getStart();
        startIndex = indexMapping.get(startIndex).getStart();

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

        start = indexMapping.get(start).getStart();
        end = indexMapping.get(end).getEnd();

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
