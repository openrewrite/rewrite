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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class EncodingDetectingInputStreamTest {

    @Test
    void detectUTF8Bom() {
        String bom = "ï»¿";
        assertThat(read(bom, UTF_8).isCharsetBomMarked()).isTrue();
    }

    @Test
    void iso88591() {
        assertThat(read("yÀaÀÅÈËÑãêïñùý", ISO_8859_1).getCharset()).isEqualTo(ISO_8859_1);
    }

    @Test
    void utf8() {
        assertThat(read("yÀaÀÅÈËÑãêïñùý", UTF_8).getCharset()).isEqualTo(UTF_8);
    }

    @Test
    void UTF_8() {
        for (int i = 0; i < 1000000; i++) {
            String c = Character.toString((char) i);
            assertThat(read(c, UTF_8).getCharset()).isEqualTo(UTF_8);
        }
    }

    @Test
    void ISO_8859_1() {
        int startCharacter = 128;
        int lastIso88591Character = 256;

        for (int i = startCharacter; i < lastIso88591Character; i++) {
            // char of i is concatenated because the bytes are equal to the start of a UTF-8 sequence.
            // So, there must be at least two characters in a source file to detect it is not UTF-8.
            String c = Character.toString((char) i) + (char) i;
            assertThat(read(c, ISO_8859_1).getCharset()).isEqualTo(ISO_8859_1);
        }
    }

    @Test
    void oddPairInISO_8859_1() {
        assertThat(read("ÂÂ", ISO_8859_1).getCharset()).isEqualTo(ISO_8859_1);
    }

    private EncodingDetectingInputStream read(String s, Charset charset) {
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(s.getBytes(charset)));
        is.readFully();
        return is;
    }
}
