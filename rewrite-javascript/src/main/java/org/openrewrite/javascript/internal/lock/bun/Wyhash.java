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

/**
 * Port of Zig's lib/std/hash/wyhash.zig {@code Wyhash.hash} at the Zig commit bun 1.3.10 builds with
 * (oven-sh/zig c031cbebf5b063210473ff5204a24ebfb2492c72): the hash {@code AutoContext(u64)} applies to a key.
 */
final class Wyhash {

    private static final long[] SECRET = {
            0xa0761d6478bd642fL,
            0xe7037ed1a0b428dbL,
            0x8ebc6af09c88c6e3L,
            0x589965cc75374cc3L,
    };

    private Wyhash() {
    }

    /**
     * {@code std.hash_map.getAutoHashFn(u64)}: a u64 has a unique representation, so its little-endian bytes are
     * hashed.
     */
    static long hashU64(long key) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (key >>> (8 * i));
        }
        return hash(0, bytes);
    }

    static long hash(long seed, byte[] input) {
        long[] state = new long[3];
        state[0] = seed ^ mix(seed ^ SECRET[0], SECRET[1]);
        state[1] = state[0];
        state[2] = state[0];
        long a;
        long b;
        if (input.length <= 16) {
            if (input.length >= 4) {
                int end = input.length - 4;
                int quarter = (input.length >> 3) << 2;
                a = (read(input, 0, 4) << 32) | read(input, quarter, 4);
                b = (read(input, end, 4) << 32) | read(input, end - quarter, 4);
            } else if (input.length > 0) {
                a = ((input[0] & 0xFFL) << 16) | ((input[input.length >> 1] & 0xFFL) << 8) |
                    (input[input.length - 1] & 0xFFL);
                b = 0;
            } else {
                a = 0;
                b = 0;
            }
        } else {
            int i = 0;
            if (input.length >= 48) {
                while (i + 48 < input.length) {
                    for (int r = 0; r < 3; r++) {
                        long x = read(input, i + 8 * (2 * r), 8);
                        long y = read(input, i + 8 * (2 * r + 1), 8);
                        state[r] = mix(x ^ SECRET[r + 1], y ^ state[r]);
                    }
                    i += 48;
                }
                state[0] ^= state[1] ^ state[2];
            }
            int start = i;
            int k = 0;
            while (k + 16 < input.length - start) {
                state[0] = mix(read(input, start + k, 8) ^ SECRET[1], read(input, start + k + 8, 8) ^ state[0]);
                k += 16;
            }
            a = read(input, input.length - 16, 8);
            b = read(input, input.length - 8, 8);
        }
        a ^= SECRET[1];
        b ^= state[0];
        long lo = multiplyLow(a, b);
        long hi = multiplyHighUnsigned(a, b);
        return mix(lo ^ SECRET[0] ^ input.length, hi ^ SECRET[1]);
    }

    private static long mix(long a, long b) {
        return multiplyLow(a, b) ^ multiplyHighUnsigned(a, b);
    }

    private static long read(byte[] b, int off, int bytes) {
        long v = 0;
        for (int i = bytes - 1; i >= 0; i--) {
            v = (v << 8) | (b[off + i] & 0xFFL);
        }
        return v;
    }

    static long multiplyLow(long a, long b) {
        return a * b;
    }

    static long multiplyHighUnsigned(long a, long b) {
        long aLo = a & 0xFFFFFFFFL;
        long aHi = a >>> 32;
        long bLo = b & 0xFFFFFFFFL;
        long bHi = b >>> 32;
        long loLo = aLo * bLo;
        long hiLo = aHi * bLo;
        long loHi = aLo * bHi;
        long hiHi = aHi * bHi;
        long cross = (loLo >>> 32) + (hiLo & 0xFFFFFFFFL) + loHi;
        return hiHi + (hiLo >>> 32) + (cross >>> 32);
    }
}
