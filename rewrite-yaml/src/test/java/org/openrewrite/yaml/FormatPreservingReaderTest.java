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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FormatPreservingReaderTest {

    @Test
    void allInCurrentBuffer() throws IOException {
        var text = "0123456789";
        var formatPreservingReader = new FormatPreservingReader(text);

        char[] charArray = new char[10];
        formatPreservingReader.read(charArray, 0, 10);
        assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
    }

    @Test
    void allInPreviousBuffer() throws IOException {
        var text = "0123456789";
        var formatPreservingReader = new FormatPreservingReader(text);

        char[] charArray = new char[10];

        formatPreservingReader.read(charArray, 0, 5);
        formatPreservingReader.read(charArray, 0, 5);

        assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
    }

    @Test
    void splitBetweenPrevAndCurrentBuffer() throws IOException {
        var text = "0123456789";
        var formatPreservingReader = new FormatPreservingReader(text);

        char[] charArray = new char[10];

        formatPreservingReader.read(charArray, 0, 1);
        formatPreservingReader.read(charArray, 0, 9);

        assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
    }
}
