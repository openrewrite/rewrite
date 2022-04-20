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
package org.openrewrite;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ParserInputTest {

    @Test
    void detectUtf8() {
        Path path = Paths.get("src/test/resources/encoding/UTF8BOM.txt");
        Parser.Input input = new Parser.Input(path, () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException ignore) {
                return null;
            }
        });
        assertThat(input.getCharacterEncoding()).isNotNull();
        assertThat(input.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    void detectUtf16LE() {
        Path path = Paths.get("src/test/resources/encoding/UTF16LEBOM.txt");
        Parser.Input input = new Parser.Input(path, () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException ignore) {
                return null;
            }
        });
        assertThat(input.getCharacterEncoding()).isNotNull();
        assertThat(input.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_16LE);
    }

    @Test
    void detectUtf16BE() {
        Path path = Paths.get("src/test/resources/encoding/UTF16BEBOM.txt");
        Parser.Input input = new Parser.Input(path, () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException ignore) {
                return null;
            }
        });
        assertThat(input.getCharacterEncoding()).isNotNull();
        assertThat(input.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_16BE);
    }

    @Test
    void detectISO88591() {
        Path path = Paths.get("src/test/resources/encoding/ISO88951.txt");
        Parser.Input input = new Parser.Input(path, () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException ignore) {
                return null;
            }
        });
        assertThat(input.getCharacterEncoding()).isNotNull();
        assertThat(input.getCharacterEncoding()).isEqualTo(StandardCharsets.ISO_8859_1);
    }
}
