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

    private static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");

    @Test
    void detectUTF8Bom() {
        String bom = "ï»¿";
        assertThat(read(bom, UTF_8).isCharsetBomMarked()).isTrue();
    }

    @Test
    void utf8() {
        assertThat(read("yÀaÀÅÈËÑãêïñùý", UTF_8).getCharset()).isEqualTo(UTF_8);
    }

    @Test
    void windows1252() {
        assertThat(read("yÀaÀÅÈËÑãêïñùý", WINDOWS_1252).getCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void utf8Characters() {
        for (int i = 0; i < 10000; i++) {
            String c = Character.toString((char) i);
            assertThat(read(c, UTF_8).getCharset()).isEqualTo(UTF_8);
        }
    }

    @Test
    void windows1252SpecialCharacters() {
        String specialCharacters = "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”·–—˜™š›œžŸ¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿";
        for (char c : specialCharacters.toCharArray()) {
            String parse = String.valueOf(c);
            assertThat(read(parse, WINDOWS_1252).getCharset()).isEqualTo(WINDOWS_1252);
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
