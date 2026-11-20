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

import java.nio.charset.StandardCharsets;

/**
 * Port of bun's src/wyhash.zig {@code Wyhash11} (bun 1.3.10): the hash of package names
 * ({@code String.Builder.stringHash}).
 */
final class Wyhash11 {

    private static final long[] PRIMES = {
            0xa0761d6478bd642fL,
            0xe7037ed1a0b428dbL,
            0x8ebc6af09c88c6e3L,
            0x589965cc75374cc3L,
            0x1d8e4e27c47d124fL,
    };

    private Wyhash11() {
    }

    static long stringHash(String s) {
        return hash(0, s.getBytes(StandardCharsets.UTF_8));
    }

    static long hash(long seed, byte[] input) {
        int alignedLen = input.length - (input.length % 32);
        long state = seed;
        for (int off = 0; off < alignedLen; off += 32) {
            state = mix0(read(input, off, 8), read(input, off + 8, 8), state) ^
                    mix1(read(input, off + 16, 8), read(input, off + 24, 8), state);
        }
        return finalRound(state, alignedLen, input, alignedLen, input.length - alignedLen);
    }

    private static long finalRound(long seed, long msgLen, byte[] b, int o, int remLen) {
        long s;
        switch (remLen) {
            case 0:
                s = seed;
                break;
            case 1:
                s = mix0(read(b, o, 1), PRIMES[4], seed);
                break;
            case 2:
                s = mix0(read(b, o, 2), PRIMES[4], seed);
                break;
            case 3:
                s = mix0((read(b, o, 2) << 8) | read(b, o + 2, 1), PRIMES[4], seed);
                break;
            case 4:
                s = mix0(read(b, o, 4), PRIMES[4], seed);
                break;
            case 5:
                s = mix0((read(b, o, 4) << 8) | read(b, o + 4, 1), PRIMES[4], seed);
                break;
            case 6:
                s = mix0((read(b, o, 4) << 16) | read(b, o + 4, 2), PRIMES[4], seed);
                break;
            case 7:
                s = mix0((read(b, o, 4) << 24) | (read(b, o + 4, 2) << 8) | read(b, o + 6, 1), PRIMES[4], seed);
                break;
            case 8:
                s = mix0(swapped(b, o), PRIMES[4], seed);
                break;
            case 9:
                s = mix0(swapped(b, o), read(b, o + 8, 1), seed);
                break;
            case 10:
                s = mix0(swapped(b, o), read(b, o + 8, 2), seed);
                break;
            case 11:
                s = mix0(swapped(b, o), (read(b, o + 8, 2) << 8) | read(b, o + 10, 1), seed);
                break;
            case 12:
                s = mix0(swapped(b, o), read(b, o + 8, 4), seed);
                break;
            case 13:
                s = mix0(swapped(b, o), (read(b, o + 8, 4) << 8) | read(b, o + 12, 1), seed);
                break;
            case 14:
                s = mix0(swapped(b, o), (read(b, o + 8, 4) << 16) | read(b, o + 12, 2), seed);
                break;
            case 15:
                s = mix0(swapped(b, o), (read(b, o + 8, 4) << 24) | (read(b, o + 12, 2) << 8) | read(b, o + 14, 1),
                        seed);
                break;
            case 16:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed);
                break;
            case 17:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(read(b, o + 16, 1), PRIMES[4], seed);
                break;
            case 18:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(read(b, o + 16, 2), PRIMES[4], seed);
                break;
            case 19:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1((read(b, o + 16, 2) << 8) | read(b, o + 18, 1), PRIMES[4], seed);
                break;
            case 20:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(read(b, o + 16, 4), PRIMES[4], seed);
                break;
            case 21:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1((read(b, o + 16, 4) << 8) | read(b, o + 20, 1), PRIMES[4], seed);
                break;
            case 22:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1((read(b, o + 16, 4) << 16) | read(b, o + 20, 2), PRIMES[4], seed);
                break;
            case 23:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1((read(b, o + 16, 4) << 24) | (read(b, o + 20, 2) << 8) | read(b, o + 22, 1), PRIMES[4], seed);
                break;
            case 24:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(swapped(b, o + 16), PRIMES[4], seed);
                break;
            case 25:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(swapped(b, o + 16), read(b, o + 24, 1), seed);
                break;
            case 26:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(swapped(b, o + 16), read(b, o + 24, 2), seed);
                break;
            case 27:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1(swapped(b, o + 16), (read(b, o + 24, 2) << 8) | read(b, o + 26, 1), seed);
                break;
            case 28:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^ mix1(swapped(b, o + 16), read(b, o + 24, 4), seed);
                break;
            case 29:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1(swapped(b, o + 16), (read(b, o + 24, 4) << 8) | read(b, o + 28, 1), seed);
                break;
            case 30:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1(swapped(b, o + 16), (read(b, o + 24, 4) << 16) | read(b, o + 28, 2), seed);
                break;
            default:
                s = mix0(swapped(b, o), swapped(b, o + 8), seed) ^
                    mix1(swapped(b, o + 16),
                            (read(b, o + 24, 4) << 24) | (read(b, o + 28, 2) << 8) | read(b, o + 30, 1), seed);
                break;
        }
        return mum(s ^ (msgLen + remLen), PRIMES[4]);
    }

    private static long read(byte[] b, int off, int bytes) {
        long v = 0;
        for (int i = bytes - 1; i >= 0; i--) {
            v = (v << 8) | (b[off + i] & 0xFFL);
        }
        return v;
    }

    private static long swapped(byte[] b, int off) {
        return (read(b, off, 4) << 32) | read(b, off + 4, 4);
    }

    static long mum(long a, long b) {
        return Wyhash.multiplyLow(a, b) ^ Wyhash.multiplyHighUnsigned(a, b);
    }

    private static long mix0(long a, long b, long seed) {
        return mum(a ^ seed ^ PRIMES[0], b ^ seed ^ PRIMES[1]);
    }

    private static long mix1(long a, long b, long seed) {
        return mum(a ^ seed ^ PRIMES[2], b ^ seed ^ PRIMES[3]);
    }
}
