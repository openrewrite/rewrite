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
    void iso88591() {
        assertThat(read("yÀaÀÅÈËÑãêïñùý", ISO_8859_1).getCharset()).isEqualTo(ISO_8859_1);
    }

    @Test
    void utf8() {
        assertThat(read("yÀaÀÅÈËÑãêïñùý", UTF_8).getCharset()).isEqualTo(UTF_8);
    }

    @Test
    void falseNegativeUtf8() {
        // unlikely false negative -- two consecutive characters in the range [0xc2, 0xd0]
        assertThat(read("ÂÂ", ISO_8859_1).getCharset()).isEqualTo(UTF_8);
    }

    private EncodingDetectingInputStream read(String s, Charset charset) {
        EncodingDetectingInputStream is = new EncodingDetectingInputStream(new ByteArrayInputStream(s.getBytes(charset)));
        StringUtils.readFully(is);
        return is;
    }
}
