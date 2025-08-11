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
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class EncodingDetectingInputStreamTest {

    private static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");

    @Test
    void detectUTF8Bom() throws Exception {
        String str = "\uFEFF";
        try (EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(str.getBytes(UTF_8)))) {
            assertThat(is.readFully()).isEqualTo("");
            assertThat(is.isCharsetBomMarked()).isTrue();
        }
    }

    @Test
    void emptyUtf8() throws Exception {
        String str = "";
        try (EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(str.getBytes(UTF_8)))) {
            assertThat(is.readFully()).isEqualTo(str);
            assertThat(is.isCharsetBomMarked()).isFalse();
        }
    }

    @Test
    void singleCharUtf8() throws Exception {
        String str = "1";
        try (EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(str.getBytes(UTF_8)))) {
            assertThat(is.readFully()).isEqualTo(str);
            assertThat(is.isCharsetBomMarked()).isFalse();
        }
    }

    @Test
    void skipUTF8Bom() throws Exception {
        String bom = "\uFEFFhello";
        try (EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bom.getBytes(UTF_8)))) {
            assertThat(is.readFully()).isEqualTo("hello");
            assertThat(is.isCharsetBomMarked()).isTrue();
        }
    }

    @Test
    void skipUTF8BomKnownEncoding() throws Exception {
        String bom = "\uFEFFhello";
        try (EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bom.getBytes(UTF_8)), UTF_8)) {
            assertThat(is.readFully()).isEqualTo("hello");
            assertThat(is.isCharsetBomMarked()).isTrue();
        }
    }

    @Test
    void isUtf8() throws Exception {
        List<String> accents = List.of("Café", "Lýðræðisríki");
        for (String accent : accents) {
            try (EncodingDetectingInputStream is = read(accent, UTF_8)) {
                assertThat(is.getCharset()).isEqualTo(UTF_8);
            }
        }
    }

    @Test
    void isWindows1252() throws Exception {
        List<String> accents = List.of("Café", "Lýðræðisríki");
        for (String accent : accents) {
            try (EncodingDetectingInputStream is = read(accent, WINDOWS_1252)) {
                assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
            }
        }
    }

    @Test
    void oddPairInWindows1252() throws Exception {
        // Range 1: 0xC0 - 0xDF == "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞß"
        // Range 2: 0x80 - 0xBF == "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”·–—˜™š›œžŸ¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿"
        // A character in range 1 followed by a character in range 2 encoded in Windows-1252 will be detected as UTF-8.
        try (EncodingDetectingInputStream is = read("À€", WINDOWS_1252)) {
            assertThat(is.getCharset()).isEqualTo(UTF_8);
        }
    }

    @Test
    void utf8Characters() throws Exception {
        for (int i = 192; i < 2048; i++) {
            String c = Character.toString((char) i);
            try (EncodingDetectingInputStream is = read(c, UTF_8)) {
                assertThat(is.getCharset()).isEqualTo(UTF_8);
            }
        }
    }

    @Test
    void windows1252SpecialCharacters() throws Exception {
        String specialCharacters = "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”·–—˜™š›œžŸ¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿";
        for (char c : specialCharacters.toCharArray()) {
            String parse = String.valueOf(c);
            try (EncodingDetectingInputStream is = read(parse, WINDOWS_1252)) {
                assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
            }
        }
    }

    @Test
    void iso88591() {
        for (int i = 0; i < 255; i++) {
            // Skip control characters in ISO-8859-1
            if (!(i >= 128 && i <= 159)) {
                String s = Character.toString((char) i);
                byte[] win = s.getBytes(WINDOWS_1252);
                byte[] iso = s.getBytes(ISO_8859_1);
                assertThat(iso[0]).isEqualTo(win[0]);
            }
        }
    }

    private EncodingDetectingInputStream read(String s, Charset charset) {
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(s.getBytes(charset)));
        is.readFully();
        return is;
    }
}
