/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test vectors of Zig's std.hash.Wyhash at the Zig commit bun 1.3.10 builds with ({@link Wyhash}), and of the
 * Zig 0.10.1 Wyhash bun's src/wyhash.zig copies ({@link Wyhash11}).
 */
class WyhashTest {

    private static final String DIGITS_80 =
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890";

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void zigStdWyhashVectors() {
        assertThat(Wyhash.hash(0, bytes(""))).isEqualTo(0x409638ee2bde459L);
        assertThat(Wyhash.hash(1, bytes("a"))).isEqualTo(0xa8412d091b5fe0a9L);
        assertThat(Wyhash.hash(2, bytes("abc"))).isEqualTo(0x32dd92e4b2915153L);
        assertThat(Wyhash.hash(3, bytes("message digest"))).isEqualTo(0x8619124089a3a16bL);
        assertThat(Wyhash.hash(4, bytes("abcdefghijklmnopqrstuvwxyz"))).isEqualTo(0x7a43afb61d7f5f40L);
        assertThat(Wyhash.hash(5, bytes("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")))
          .isEqualTo(0xff42329b90e50d58L);
        assertThat(Wyhash.hash(6, bytes(DIGITS_80))).isEqualTo(0xc39cab13b115aad3L);
    }

    @Test
    void wyhash11Vectors() {
        assertThat(Wyhash11.hash(0, bytes(""))).isEqualTo(0x0L);
        assertThat(Wyhash11.hash(1, bytes("a"))).isEqualTo(0xbed235177f41d328L);
        assertThat(Wyhash11.hash(2, bytes("abc"))).isEqualTo(0xbe348debe59b27c3L);
        assertThat(Wyhash11.hash(3, bytes("message digest"))).isEqualTo(0x37320f657213a290L);
        assertThat(Wyhash11.hash(4, bytes("abcdefghijklmnopqrstuvwxyz"))).isEqualTo(0xd0b270e1d8a7019cL);
        assertThat(Wyhash11.hash(5, bytes("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")))
          .isEqualTo(0x602a1894d3bbfe7fL);
        assertThat(Wyhash11.hash(6, bytes(DIGITS_80))).isEqualTo(0x829e9c148b75970eL);
    }
}
