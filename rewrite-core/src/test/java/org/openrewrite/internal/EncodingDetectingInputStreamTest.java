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
        // 0xC0 and 0xC1 are invalid UTF-8 lead bytes (overlong encodings forbidden by RFC 3629),
        // so Windows-1252 text starting with À (0xC0) is now correctly detected.
        try (EncodingDetectingInputStream is = read(new String(new byte[]{(byte) 0xC0, (byte) 0x80}, WINDOWS_1252), WINDOWS_1252)) {
            assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
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

    @Test
    void detectsWindows1252WhenFileContainsByteAboveF7() {
        // 0xFC is 'ü' in ISO-8859-1/Windows-1252, and is never valid in UTF-8
        byte[] bytes = new byte[]{'H', 'e', 'l', 'l', 'o', (byte) 0xFC};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void detectsWindows1252WithMultipleHighBytes() {
        // 0xFC='ü', 0xE4 starts a valid UTF-8 3-byte sequence but 0xFC does not
        byte[] bytes = new byte[]{'t', 'e', 's', 't', (byte) 0xFC, ' ', (byte) 0xE4, ' ', (byte) 0xF6};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void validUtf8MultiBytesDetectedAsUtf8() {
        // 'ü' encoded as UTF-8: 0xC3 0xBC
        byte[] bytes = new byte[]{'H', 'e', 'l', 'l', 'o', (byte) 0xC3, (byte) 0xBC};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(UTF_8);
    }

    @Test
    void readFullyDecodesIso8859Correctly() {
        // "Hütte" in ISO-8859-1: H=0x48, ü=0xFC, t=0x74, t=0x74, e=0x65
        byte[] bytes = new byte[]{0x48, (byte) 0xFC, 0x74, 0x74, 0x65};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        String result = is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
        assertThat(result).isEqualTo("Hütte");
    }

    @Test
    void detectsWindows1252ForOverlongC0() {
        // 0xC0 is 'À' in ISO-8859-1, and is an invalid UTF-8 lead byte (overlong)
        byte[] bytes = new byte[]{'C', 'a', 'f', (byte) 0xC0};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void detectsWindows1252ForOverlongC1() {
        // 0xC1 is 'Á' in ISO-8859-1, and is an invalid UTF-8 lead byte (overlong)
        byte[] bytes = new byte[]{'S', 'a', 'o', ' ', 'P', 'a', 'u', 'l', 'o', (byte) 0xC1};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void detectsWindows1252ForC0FollowedByContinuationByte() {
        // 0xC0 0xA3 looks like a UTF-8 two-byte sequence but is actually overlong
        // In ISO-8859-1 this would be 'À£'
        byte[] bytes = new byte[]{'t', 'e', 's', 't', (byte) 0xC0, (byte) 0xA3};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void utf8ThreeByteSequences() throws Exception {
        // U+0800 (first 3-byte), U+4E16 (世), U+AC00 (가)
        for (String s : List.of("\u0800", "世", "가", "Hello 世界")) {
            try (EncodingDetectingInputStream is = read(s, UTF_8)) {
                assertThat(is.getCharset())
                        .as("Expected UTF-8 for: %s", s)
                        .isEqualTo(UTF_8);
            }
        }
    }

    @Test
    void utf8FourByteSequences() throws Exception {
        // U+1F600 (😀), U+10000 (first 4-byte), U+1F4A9 (💩)
        for (String s : List.of("\uD83D\uDE00", "\uD800\uDC00", "\uD83D\uDCA9", "Hello \uD83D\uDE00 world")) {
            try (EncodingDetectingInputStream is = read(s, UTF_8)) {
                assertThat(is.getCharset())
                        .as("Expected UTF-8 for: %s", s)
                        .isEqualTo(UTF_8);
            }
        }
    }

    @Test
    void truncatedThreeByteSequence() {
        // 0xE4 starts a 3-byte sequence, but only one continuation byte follows
        byte[] bytes = new byte[]{'a', 'b', (byte) 0xE4, (byte) 0xB8};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void truncatedFourByteSequence() {
        // 0xF0 starts a 4-byte sequence, but only two continuation bytes follow
        byte[] bytes = new byte[]{'a', (byte) 0xF0, (byte) 0x9F, (byte) 0x98};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void bareContinuationByte() {
        // 0x80-0xBF without a preceding lead byte is invalid UTF-8
        byte[] bytes = new byte[]{'H', 'e', 'l', 'l', 'o', (byte) 0x80};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void validUtf8FollowedByInvalidByte() {
        // Valid 2-byte UTF-8 (0xC3 0xBC = ü), then ASCII, then invalid byte 0xFC
        byte[] bytes = new byte[]{(byte) 0xC3, (byte) 0xBC, ' ', 'a', 'n', 'd', ' ', (byte) 0xFC};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void mixedMultiByteUtf8() throws Exception {
        // 2-byte (é), 3-byte (世), 4-byte (😀), all valid UTF-8
        String s = "caf\u00E9 \u4E16\u754C \uD83D\uDE00";
        try (EncodingDetectingInputStream is = read(s, UTF_8)) {
            assertThat(is.getCharset()).isEqualTo(UTF_8);
        }
    }

    @Test
    void invalidByteAfterValidThreeByteSequence() {
        // Valid 3-byte (世 = 0xE4 0xB8 0x96), then invalid 0xFE
        byte[] bytes = new byte[]{(byte) 0xE4, (byte) 0xB8, (byte) 0x96, ' ', (byte) 0xFE};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void brokenContinuationInThreeByteSequence() {
        // 0xE4 expects two continuation bytes, but second byte is ASCII
        byte[] bytes = new byte[]{'x', (byte) 0xE4, (byte) 0xB8, 'z'};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void brokenContinuationInFourByteSequence() {
        // 0xF0 expects three continuation bytes, but third byte is ASCII
        byte[] bytes = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, 'x'};
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(bytes));
        is.readFully();
        assertThat(is.getCharset()).isEqualTo(WINDOWS_1252);
    }

    private EncodingDetectingInputStream read(String s, Charset charset) {
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(s.getBytes(charset)));
        is.readFully();
        return is;
    }
}
