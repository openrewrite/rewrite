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

/**
 * Maintains a lookback window of characters used to determine format prefixes of
 * YAML AST elements.
 */
class FormatPreservingReader extends Reader {
    private final Reader delegate;

    private final char[] currentBuffer = new char[1025];
    private int currentBufferIndex = 0;
    private int currentBufferLength = 0;

    private final char[] prevBuffer = new char[1025];
    private int prevBufferIndex = 0;
    private int prevBufferLength = 0;

    FormatPreservingReader(Reader delegate) {
        this.delegate = delegate;
    }

    // VisibleForTesting
    String prefix(int lastEnd, int startIndex) {
        assert lastEnd <= startIndex;

        int prefixLen = startIndex - lastEnd;
        if (prefixLen > 0) {
            char[] prefix = new char[prefixLen];

            if (lastEnd < currentBufferIndex) {
                int prevBufferAvailable = prevBufferLength - prevBufferIndex - lastEnd;
                System.arraycopy(prevBuffer, lastEnd - prevBufferIndex, prefix, 0,
                        Math.min(prevBufferAvailable, prefixLen));
                if (prefixLen > prevBufferAvailable) {
                    System.arraycopy(currentBuffer, 0, prefix, prevBufferAvailable, prefixLen - prevBufferAvailable);
                }
            } else {
                System.arraycopy(currentBuffer, lastEnd - currentBufferIndex, prefix, 0, prefixLen);
            }

            return new String(prefix);
        }
        return "";
    }

    public String prefix(int lastEnd, Event event) {
        return prefix(lastEnd, event.getStartMark().getIndex());
    }

    @Override
    public int read(@NonNull char[] cbuf, int off, int len) throws IOException {
        System.arraycopy(currentBuffer, 0, prevBuffer, 0, currentBufferLength);
        prevBufferIndex = currentBufferIndex;
        prevBufferLength = currentBufferLength;

        int read = delegate.read(currentBuffer, off, len);
        currentBufferIndex += currentBufferLength;
        currentBufferLength = len;

        if (read > 0) {
            System.arraycopy(currentBuffer, 0, cbuf, 0, read);
        }

        return read;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
