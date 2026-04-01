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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FormatPreservingReaderTest {

    @Test
    void readWithOffset() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 3, 5);
            assertThat(formatPreservingReader.prefix(0, 5)).isEqualTo("01234");
        }
    }

    @Test
    void allInCurrentBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 10);
            assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
        }
    }

    @Test
    void stringAllInCurrentBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 10);
            assertThat(formatPreservingReader.readStringFromBuffer(0, 3)).isEqualTo("0123");
        }
    }

    @Test
    void stringAtEndOfCurrentBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 10);
            assertThat(formatPreservingReader.readStringFromBuffer(8, 10)).isEqualTo("89");
        }
    }

    @Test
    void allInPreviousBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 5);
            formatPreservingReader.read(charArray, 0, 5);
            assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
        }
    }

    @Test
    void stringAllInPreviousBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 5);
            formatPreservingReader.read(charArray, 0, 5);
            assertThat(formatPreservingReader.readStringFromBuffer(0, 3)).isEqualTo("0123");
        }
    }

    @Test
    void splitBetweenPrevAndCurrentBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 1);
            formatPreservingReader.read(charArray, 0, 9);
            assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
        }
    }

    @Test
    void stringSplitBetweenPrevAndCurrentBuffer() throws Exception {
        try (FormatPreservingReader formatPreservingReader = new FormatPreservingReader("0123456789")) {
            char[] charArray = new char[10];
            formatPreservingReader.read(charArray, 0, 1);
            formatPreservingReader.read(charArray, 0, 9);
            assertThat(formatPreservingReader.readStringFromBuffer(0, 3)).isEqualTo("0123");
        }
    }
}
