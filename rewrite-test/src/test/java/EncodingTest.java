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
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class EncodingTest {

    @Test
    void checkIso88591() {
        assertThat(isISO88591("yÀaÀÅÈËÑãêïñùý".getBytes(StandardCharsets.ISO_8859_1))).as("not iso").isTrue();
        assertThat(isISO88591("yÀaÀÅÈËÑãêïñùý".getBytes(StandardCharsets.UTF_8))).as("not utf").isFalse();

        // unlikely false negative -- two consecutive characters in the range [0xc2, 0xd0]
        assertThat(isISO88591("ÂÂ".getBytes(StandardCharsets.ISO_8859_1))).as("is iso").isFalse();
    }

    /**
     * Returns true for ISO-8859-1 that is unlikely to also be valid UTF-8.
     * Looks for any character greater than 0x7f and not preceded by a character in the range [0xc2, 0xd0].
     */
    private boolean isISO88591(byte[] bytes) {
        byte prev = 0;
        for (byte aByte : bytes) {
            if ((aByte & 0xff) >= 0x80 && notUtfHighByte(aByte)) {
                if (notUtfHighByte(prev)) {
                    return true;
                }
            }
            prev = aByte;
        }
        return false;
    }

    private boolean notUtfHighByte(byte b) {
        return (b & 0xff) < 0xc2 || (b & 0xff) > 0xd0;
    }
}
