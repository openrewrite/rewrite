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

import java.util.Arrays;
import java.util.Base64;

/**
 * Port of bun's src/install/integrity.zig (bun 1.3.10): the digest decoded and encoded again with Zig's unpadded
 * base64, then {@code =} appended for sha1 and {@code ==} for every other algorithm.
 */
final class Integrity {

    enum Tag {
        UNKNOWN(0), SHA1(20), SHA256(32), SHA384(48), SHA512(64);

        final int digestLen;

        Tag(int digestLen) {
            this.digestLen = digestLen;
        }

        boolean isSupported() {
            return this != UNKNOWN;
        }
    }

    static final Integrity UNKNOWN = new Integrity(Tag.UNKNOWN, new byte[64]);

    private static final int DIGEST_BUF_LEN = 64;

    final Tag tag;
    private final byte[] value;

    private Integrity(Tag tag, byte[] value) {
        this.tag = tag;
        this.value = value;
    }

    static Integrity parseSHASum(String buf) {
        if (buf.isEmpty()) {
            return UNKNOWN;
        }
        byte[] value = new byte[DIGEST_BUF_LEN];
        int end = Math.min(40, buf.length());
        int outI = 0;
        int i = 0;
        while (i < end) {
            int x0 = hexDigit(buf.charAt(i));
            i++;
            if (i >= buf.length()) {
                throw new IllegalArgumentException("InvalidCharacter");
            }
            int x1 = hexDigit(buf.charAt(i));
            value[outI] = (byte) (x0 << 4 | x1);
            outI++;
            i++;
        }
        return new Integrity(Tag.SHA1, value);
    }

    // digits and letters as base 36: bun reads 'g'..'z' too
    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException("InvalidCharacter");
    }

    static Integrity parse(String buf) {
        if (buf.length() < "sha256-".length()) {
            return UNKNOWN;
        }
        int dash = buf.substring(0, Math.min(buf.length(), 7)).indexOf('-');
        if (dash < 0 || buf.length() <= dash + 1) {
            return UNKNOWN;
        }
        Tag tag;
        switch (buf.substring(0, dash)) {
            case "sha1":
                tag = Tag.SHA1;
                break;
            case "sha256":
                tag = Tag.SHA256;
                break;
            case "sha384":
                tag = Tag.SHA384;
                break;
            case "sha512":
                tag = Tag.SHA512;
                break;
            default:
                return UNKNOWN;
        }
        String input = Strings.trimRight(buf.substring(dash + 1), "=");
        int leftover = input.length() % 4;
        if (leftover == 1) {
            return UNKNOWN;
        }
        int decodedSize = input.length() / 4 * 3 + leftover * 3 / 4;
        if (decodedSize > tag.digestLen) {
            return UNKNOWN;
        }
        byte[] out = new byte[DIGEST_BUF_LEN];
        int acc = 0;
        int accLen = 0;
        int destIdx = 0;
        for (int k = 0; k < input.length(); k++) {
            int d = base64Index(input.charAt(k));
            if (d < 0) {
                return UNKNOWN;
            }
            acc = ((acc << 6) + d) & 0xFFF;
            accLen += 6;
            if (accLen >= 8) {
                accLen -= 8;
                out[destIdx++] = (byte) (acc >> accLen);
            }
        }
        if (accLen > 4 || (acc & ((1 << accLen) - 1)) != 0) {
            return UNKNOWN;
        }
        return new Integrity(tag, out);
    }

    private static int base64Index(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        } else if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        } else if (c == '+') {
            return 62;
        } else if (c == '/') {
            return 63;
        }
        return -1;
    }

    boolean eql(Integrity other) {
        return tag == other.tag && Arrays.equals(value, other.value);
    }

    /**
     * {@code Integrity.format}: nothing for an unknown tag.
     */
    @Override
    public String toString() {
        if (!tag.isSupported()) {
            return "";
        }
        String prefix = tag.name().toLowerCase() + "-";
        String encoded = Base64.getEncoder().withoutPadding().encodeToString(Arrays.copyOf(value, tag.digestLen));
        return prefix + encoded + (tag == Tag.SHA1 ? "=" : "==");
    }
}
