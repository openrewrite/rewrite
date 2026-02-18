/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.ipc.http.HttpSender;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChecksumTest {

    @Test
    void fromHexWithValidInput() {
        Checksum checksum = Checksum.fromHex("SHA-256", "abcdef01");
        assertThat(checksum.getHexValue()).isEqualTo("abcdef01");
    }

    @Test
    void fromHexRejectsNonHexInput() {
        assertThatThrownBy(() -> Checksum.fromHex("SHA-256", "<?xml version"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid hexadecimal");
    }

    @Test
    void fromUriRejectsNonSuccessfulResponse() {
        HttpSender httpSender = request -> new HttpSender.Response(
                403,
                new ByteArrayInputStream("<?xml version=\"1.0\"?>".getBytes(StandardCharsets.UTF_8)),
                () -> {
                });

        assertThatThrownBy(() -> Checksum.fromUri(httpSender, URI.create("https://example.com/checksum.sha256")))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("403");
    }

    @Test
    void fromUriTrimsWhitespace() {
        HttpSender httpSender = request -> new HttpSender.Response(
                200,
                new ByteArrayInputStream("abcdef01\n".getBytes(StandardCharsets.UTF_8)),
                () -> {
                });

        Checksum checksum = Checksum.fromUri(httpSender, URI.create("https://example.com/checksum.sha256"));
        assertThat(checksum.getHexValue()).isEqualTo("abcdef01");
    }
}
