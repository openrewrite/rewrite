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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

public class EncodingTest {

    @Test
    void checkIso88591() {
        assertThat(isISO88591("yÀaÀÅÈËÑãêïñùý".getBytes(StandardCharsets.ISO_8859_1))).as("not iso").isTrue();
        assertThat(isISO88591("yÀaÀÅÈËÑãêïñùý".getBytes(StandardCharsets.UTF_8))).as("not utf").isFalse();

        assertThat(isISO88591("ÂÂ".getBytes(StandardCharsets.ISO_8859_1))).as("is iso").isTrue();
    }

    @Test
    void compareWindows1252ToISO88591() {
        int startCharacter = 0;
        int lastIso88591Character = 255;

        for (int i = startCharacter; i < lastIso88591Character; i++) {
            String c = Character.toString((char) i);
            byte[] iso = c.getBytes(ISO_8859_1);
            byte[] win = c.getBytes(Charset.forName("Windows-1252"));

            int isoB = iso[0] & 0xFF;
            int winB = win[0] & 0xFF;
            if (128 <= i && i <= 159) {
                // The characters between the range of 128 and 159 do not match.
                // In Windows-1252, the Euro sign () occurs at i == 128 and is the only printable character in the range.
                // Characters between 129 - 159 are control characters.
                assertThat(isoB).isNotEqualTo(winB);
            } else {
                assertThat(isoB).isEqualTo(winB);
            }
        }
    }

    /**
     * Returns true for ISO-8859-1 that is unlikely to also be valid UTF-8.
     * Looks for any character greater than 0x7f and not preceded by a character in the range [0xc2, 0xd0].
     */
    private boolean isISO88591(byte[] bytes) {
        int prev = 0;
        for (byte aByte : bytes) {
            int aInt = (aByte & 0xFF);
            if (aInt >= 0x80 &&
                    (!(aInt >= 0xC2 && aInt <= 0xEF) && prev == 0) ||
                    // UTF-8 conditions.
                    ((prev >= 0xC2 && prev <= 0xDF && notUtfHighByte(aInt)) || // 2 byte sequence
                            (prev >= 0xE0 && prev <= 0xEF && notUtfHighByte(aInt)) || // 3 byte sequence
                            (prev >= 0xF0 && prev <= 0xF7 && notUtfHighByte(aInt)))) { // 4 byte sequence
                return true;
            }
            prev = aInt;
        }
        return false;
    }

    private boolean notUtfHighByte(int b) {
        return !(b >= 0x80 && b <= 0xBF);
    }
}
