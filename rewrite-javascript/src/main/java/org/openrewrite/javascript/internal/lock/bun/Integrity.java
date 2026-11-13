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

import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Arrays;
import java.util.Base64;

/**
 * Port of bun's src/install/integrity.zig (bun 1.3.10), with integrity.rs (1.4.0) beside it: the digest decoded and
 * encoded again with Zig's unpadded base64, then {@code =} appended for sha1 and {@code ==} for every other algorithm.
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
        return parseSHASum(buf, BunRelease.PORTED);
    }

    /**
     * {@code parseSHASum}: the first 40 characters as pairs of digits, letters read in base 36; from 1.4.0
     * hexadecimal digits only, an odd count of them an error ({@link BunRelease#readsAShasumAsHexadecimal()}).
     *
     * @throws IllegalArgumentException {@code InvalidCharacter}
     */
    static Integrity parseSHASum(String buf, BunRelease release) {
        if (buf.isEmpty()) {
            return UNKNOWN;
        }
        byte[] value = new byte[DIGEST_BUF_LEN];
        int end = Math.min(40, buf.length());
        int outI = 0;
        int i = 0;
        if (release.readsAShasumAsHexadecimal()) {
            if (end % 2 != 0) {
                throw new IllegalArgumentException("InvalidCharacter");
            }
            while (i < end) {
                int hi = hexValue(buf.charAt(i));
                int lo = hexValue(buf.charAt(i + 1));
                if (hi < 0 || lo < 0) {
                    throw new IllegalArgumentException("InvalidCharacter");
                }
                value[outI++] = (byte) (hi << 4 | lo);
                i += 2;
            }
            return new Integrity(Tag.SHA1, value);
        }
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

    // hex_digit_value: -1 for anything but 0-9, a-f and A-F
    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    static Integrity parse(String buf) {
        return parse(buf, BunRelease.PORTED);
    }

    /**
     * {@code parse}: the whole string is one hash; from 1.4.0 the strongest of the hashes it holds separated by ASCII
     * whitespace, each read up to a {@code ?} ({@link BunRelease#keepsTheStrongestOfSeveralHashes()}); before 1.2.20
     * decoded from its eighth character ({@link BunRelease#decodesAnIntegrityPastItsAlgorithm()}), and before 1.2.21
     * into 64 bytes whatever its algorithm ({@link BunRelease#refusesAnIntegrityLongerThanItsDigest()}).
     *
     * @throws CannotKnow before 1.2.21 for a hash of more than 64 bytes, which bun decodes past its buffer
     */
    static Integrity parse(String buf, BunRelease release) {
        if (!release.keepsTheStrongestOfSeveralHashes()) {
            return parseEntry(buf, false, release);
        }
        Integrity strongest = UNKNOWN;
        int start = 0;
        for (int i = 0; i <= buf.length(); i++) {
            if (i == buf.length() || isAsciiWhitespace(buf.charAt(i))) {
                Integrity parsed = parseEntry(buf.substring(start, i), true, release);
                if (parsed.tag.ordinal() > strongest.tag.ordinal()) {
                    strongest = parsed;
                }
                start = i + 1;
            }
        }
        return strongest;
    }

    // u8::is_ascii_whitespace: no vertical tab
    private static boolean isAsciiWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000C' || c == '\r';
    }

    // parse_entry, the hash read up to a '?' from 1.4.0
    private static Integrity parseEntry(String buf, boolean cutAtQuery, BunRelease release) {
        if (buf.length() < "sha256-".length()) {
            return UNKNOWN;
        }
        int dash = buf.substring(0, Math.min(buf.length(), 7)).indexOf('-');
        boolean pastAlgorithm = release.decodesAnIntegrityPastItsAlgorithm();
        if (dash < 0 || pastAlgorithm && buf.length() <= dash + 1) {
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
        // before 1.2.20 from past "sha256-" whatever the algorithm, two characters into a sha1's hash
        String hash = buf.substring(pastAlgorithm ? dash + 1 : "sha256-".length());
        int query = cutAtQuery ? hash.indexOf('?') : -1;
        String input = Strings.trimRight(query >= 0 ? hash.substring(0, query) : hash, "=");
        int leftover = input.length() % 4;
        if (leftover == 1) {
            return UNKNOWN;
        }
        int decodedSize = input.length() / 4 * 3 + leftover * 3 / 4;
        if (release.refusesAnIntegrityLongerThanItsDigest()) {
            if (decodedSize > tag.digestLen) {
                return UNKNOWN;
            }
        } else if (decodedSize > DIGEST_BUF_LEN && writesPastTheBuffer(input)) {
            throw new CannotKnow("what bun " + release + " makes of the integrity " + buf + ", whose hash it decodes " +
                                 "past its 64-byte buffer");
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

    // std.base64 Decoder.decode into 64 bytes: its chunked loops stop short of the buffer's end, and the byte-a-time
    // loop after them writes the 65th byte at the 87th character unchecked, unless a character before it is invalid
    private static boolean writesPastTheBuffer(String input) {
        for (int k = 0; k <= 86; k++) {
            if (base64Index(input.charAt(k)) < 0) {
                return false;
            }
        }
        return true;
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
