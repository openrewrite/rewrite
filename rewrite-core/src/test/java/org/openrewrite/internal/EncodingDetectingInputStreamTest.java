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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

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

    @ParameterizedTest
    @CsvSource({
            "Café,           UTF-8,        UTF-8",
            "Lyðræðisríki,   UTF-8,        UTF-8",
            "ࠀ,              UTF-8,        UTF-8",
            "世,              UTF-8,        UTF-8",
            "가,              UTF-8,        UTF-8",
            "Hello 世界,      UTF-8,        UTF-8",
            "café 世界 🌟,    UTF-8,        UTF-8",
            "Café,           Windows-1252, Windows-1252",
            "Lyðræðisríki,   Windows-1252, Windows-1252",
    })
    void detectsCharsetForEncodedStrings(String text, String sourceCharset, String expectedCharset) throws Exception {
        try (EncodingDetectingInputStream is = read(text, Charset.forName(sourceCharset))) {
            assertThat(is.getCharset()).isEqualTo(Charset.forName(expectedCharset));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "48 65 6C 6C 6F C3 BC,           valid 2-byte sequence (Helloü)",
            "E4 B8 96,                         valid 3-byte sequence (世)",
            "F0 9F 98 80,                      valid 4-byte sequence (😀)",
            "63 61 66 C3 A9 20 E4 B8 96 20 F0 9F 8C 9F, mixed 2/3/4-byte sequences",
    })
    void detectsUtf8ForValidByteSequences(String hex, String description) {
        byte[] bytes = parseHex(hex);
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).as(description).isEqualTo(UTF_8);
    }

    @ParameterizedTest
    @CsvSource({
            "48 65 6C 6C 6F FC,               byte above 0xF7 (ü in ISO-8859-1)",
            "74 65 73 74 FC 20 E4 20 F6,       multiple high bytes",
            "C0 80,                            overlong 0xC0 lead byte",
            "43 61 66 C0,                      overlong 0xC0 at end",
            "53 61 6F 20 50 61 75 6C 6F C1,    overlong 0xC1 at end",
            "74 65 73 74 C0 A3,                overlong 0xC0 + continuation byte",
            "48 65 6C 6C 6F 80,                bare continuation byte",
            "C3 BC 20 61 6E 64 20 FC,          valid UTF-8 then invalid byte",
            "E4 B8 96 20 FE,                   invalid byte after valid 3-byte",
            "61 62 E4 B8,                      truncated 3-byte sequence",
            "61 F0 9F 98,                      truncated 4-byte sequence",
            "78 E4 B8 7A,                      broken continuation in 3-byte",
            "F0 9F 98 78,                      broken continuation in 4-byte",
    })
    void detectsWindows1252ForInvalidUtf8Bytes(String hex, String description) {
        byte[] bytes = parseHex(hex);
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).as(description).isEqualTo(WINDOWS_1252);
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
            if (!(i >= 128 && i <= 159)) {
                String s = Character.toString((char) i);
                byte[] win = s.getBytes(WINDOWS_1252);
                byte[] iso = s.getBytes(ISO_8859_1);
                assertThat(iso[0]).isEqualTo(win[0]);
            }
        }
    }

    @Test
    void readFullyDecodesIso8859Correctly() {
        byte[] bytes = new byte[]{0x48, (byte) 0xFC, 0x74, 0x74, 0x65};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        String result = is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
        assertThat(result).isEqualTo("Hütte");
    }

    private static byte[] parseHex(String hex) {
        String[] parts = hex.trim().split("\\s+");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return bytes;
    }

    private EncodingDetectingInputStream read(String s, Charset charset) {
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(s.getBytes(charset)));
        is.readFully();
        return is;
    }
}
