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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;

/**
 * Port of bun's src/semver/Version.zig (bun 1.3.10), with Version.rs (1.4.0) beside it. The core numbers are u64:
 * compared unsigned, saturating at {@code -1L}, printed unsigned; before 1.2.23 u32s, saturating at
 * {@link #MAX_U32} ({@link BunRelease#readsSixtyFourBitVersionNumbers()}). The tag's {@code pre} and {@code build} are
 * empty when absent.
 */
final class SemverVersion {

    static final long MAX = -1L;
    static final long MAX_U32 = 0xFFFFFFFFL;
    static final SemverVersion ZERO = new SemverVersion(0, 0, 0, "", "");

    final long major;
    final long minor;
    final long patch;
    final String pre;
    final String build;

    SemverVersion(long major, long minor, long patch, String pre, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.pre = pre;
        this.build = build;
    }

    static SemverVersion of(long major, long minor, long patch) {
        return new SemverVersion(major, minor, patch, "", "");
    }

    SemverVersion with(long major, long minor, long patch) {
        return new SemverVersion(major, minor, patch, pre, build);
    }

    /**
     * The ceiling of a major, minor or patch: the u64 maximum, before 1.2.23 the u32 maximum.
     */
    static long max(BunRelease release) {
        return release.readsSixtyFourBitVersionNumbers() ? MAX : MAX_U32;
    }

    static long saturatingIncrement(long n, BunRelease release) {
        return n == max(release) ? n : n + 1;
    }

    boolean hasPre() {
        return !pre.isEmpty();
    }

    boolean hasBuild() {
        return !build.isEmpty();
    }

    boolean isZero() {
        return patch == 0 && minor == 0 && major == 0;
    }

    boolean eql(SemverVersion rhs) {
        return major == rhs.major && minor == rhs.minor && patch == rhs.patch && rhs.pre.equals(pre);
    }

    int orderWithoutTag(SemverVersion rhs) {
        int c = Long.compareUnsigned(major, rhs.major);
        if (c != 0) {
            return Integer.signum(c);
        }
        c = Long.compareUnsigned(minor, rhs.minor);
        if (c != 0) {
            return Integer.signum(c);
        }
        c = Long.compareUnsigned(patch, rhs.patch);
        if (c != 0) {
            return Integer.signum(c);
        }
        if (hasPre()) {
            if (!rhs.hasPre()) {
                return -1;
            }
        } else if (rhs.hasPre()) {
            return 1;
        }
        return 0;
    }

    int order(SemverVersion rhs, BunRelease release) {
        int o = orderWithoutTag(rhs);
        if (o != 0) {
            return o;
        }
        if (hasPre() && rhs.hasPre()) {
            return orderPre(pre, rhs.pre, release);
        }
        int preOrder = Strings.order(pre, rhs.pre);
        if (preOrder != 0) {
            return preOrder;
        }
        return Strings.order(build, rhs.build);
    }

    int orderWithoutBuild(SemverVersion rhs, BunRelease release) {
        int o = orderWithoutTag(rhs);
        if (o != 0) {
            return o;
        }
        if (hasPre() && rhs.hasPre()) {
            return orderPre(pre, rhs.pre, release);
        }
        return Strings.order(pre, rhs.pre);
    }

    /**
     * {@code Tag.orderPre}: identifiers that parse as numbers of the release's width compare as numbers, before the
     * others, which compare as text.
     */
    static int orderPre(String lhs, String rhs, BunRelease release) {
        String[] l = lhs.split("\\.", -1);
        String[] r = rhs.split("\\.", -1);
        long max = max(release);
        int i = 0;
        while (true) {
            String lp = i < l.length ? l[i] : null;
            String rp = i < r.length ? r[i] : null;
            i++;
            if (lp == null && rp == null) {
                return 0;
            }
            if (rp == null) {
                return 1;
            }
            if (lp == null) {
                return -1;
            }
            Long lu = parseUnsigned(lp, max);
            Long ru = parseUnsigned(rp, max);
            if (lu != null && ru == null) {
                return -1;
            }
            if (lu == null && ru != null) {
                return 1;
            }
            if (lu == null) {
                int o = Strings.order(lp, rp);
                if (o != 0) {
                    return o;
                }
                continue;
            }
            int o = Long.compareUnsigned(lu, ru);
            if (o != 0) {
                return Integer.signum(o);
            }
        }
    }

    // std.fmt.parseUnsigned(u64, s, 10), or u32's where max is its maximum: digits and '_' between digits, no sign
    private static @Nullable Long parseUnsigned(String s, long max) {
        Long v = parseUnsigned(s);
        return v == null || Long.compareUnsigned(v, max) > 0 ? null : v;
    }

    private static @Nullable Long parseUnsigned(String s) {
        if (s.isEmpty() || s.charAt(0) == '_' || s.charAt(s.length() - 1) == '_') {
            return null;
        }
        long v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                continue;
            }
            if (c < '0' || c > '9') {
                return null;
            }
            long next = v * 10 + (c - '0');
            if (Long.compareUnsigned(v, Long.divideUnsigned(-1L, 10)) > 0 ||
                Long.compareUnsigned(next, v * 10) < 0) {
                return null;
            }
            v = next;
        }
        return v;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(Long.toUnsignedString(major)).append('.').append(Long.toUnsignedString(minor)).append('.')
                .append(Long.toUnsignedString(patch));
        if (hasPre()) {
            out.append('-').append(pre);
        }
        if (hasBuild()) {
            out.append('+').append(build);
        }
        return out.toString();
    }

    static boolean isTaggedVersionOnly(String input) {
        String version = Strings.trim(input, Strings.WHITESPACE_CHARS);
        if (version.isEmpty() || !isAlphabetic(version.charAt(0))) {
            return false;
        }
        for (int i = 1; i < version.length(); i++) {
            char c = version.charAt(i);
            if (!isAlphabetic(c) && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAlphabetic(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    /**
     * A version some of whose core numbers may be missing.
     */
    static final class Partial {
        @Nullable Long major;
        @Nullable Long minor;
        @Nullable Long patch;
        String pre = "";
        String build = "";

        SemverVersion min() {
            return new SemverVersion(major == null ? 0 : major, minor == null ? 0 : minor, patch == null ? 0 : patch,
                    pre, build);
        }

        SemverVersion max() {
            return new SemverVersion(major == null ? MAX : major, minor == null ? MAX : minor,
                    patch == null ? MAX : patch, pre, build);
        }
    }

    static final class ParseResult {
        SemverQuery.Wildcard wildcard = SemverQuery.Wildcard.NONE;
        boolean valid = true;
        Partial version = new Partial();
        int len;
    }

    private static final class TagResult {
        String pre = "";
        String build = "";
        int len;
    }

    private static TagResult parseTag(String input) {
        return parseTagWithPreCount(input, 0);
    }

    private static TagResult parseTagWithPreCount(String input, int initialPreCount) {
        int buildCount = 0;
        int preCount = initialPreCount;
        for (int k = 0; k < input.length(); k++) {
            char c = input.charAt(k);
            if (c == ' ') {
                break;
            }
            if (c == '+') {
                buildCount++;
            } else if (c == '-') {
                preCount++;
            }
        }
        TagResult result = new TagResult();
        if (buildCount == 0 && preCount == 0) {
            return result;
        }
        int state = 0;
        int start = 0;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '+') {
                if (state == 1 || state == 0 && initialPreCount > 0) {
                    result.pre = input.substring(start, i);
                }
                if (state != 2) {
                    state = 2;
                    start = i + 1;
                }
            } else if (c == '-') {
                if (state != 1) {
                    state = 1;
                    start = i + 1;
                }
            } else if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '.')) {
                if (state == 1) {
                    result.pre = input.substring(start, i);
                    state = 0;
                } else if (state == 2) {
                    result.build = input.substring(start, i);
                    state = 0;
                }
                break;
            }
            i++;
        }
        if (state == 0 && initialPreCount > 0) {
            state = 1;
            start = 0;
        }
        if (state == 1) {
            result.pre = input.substring(start, i);
        } else if (state == 2) {
            result.build = input.substring(start, i);
        }
        result.len = i;
        return result;
    }

    static ParseResult parse(String input) {
        return parse(input, BunRelease.PORTED);
    }

    /**
     * {@code parse} as the release reads a version: from 1.4.0 one of whitespace, {@code v} and {@code =} alone is
     * invalid ({@link BunRelease#rejectsAVersionOfSeparatorsAlone()}), and a number of more than 20 digits is its
     * value ({@link BunRelease#readsALongVersionNumberByItsValue()}); before 1.2.23 a number is a u32
     * ({@link BunRelease#readsSixtyFourBitVersionNumbers()}).
     *
     * @throws CannotKnow before 1.2.2 for a wildcard right after the patch number
     *                    ({@link BunRelease#skipsAWildcardAfterThePatchNumber()}), where bun reaches code it takes for
     *                    unreachable
     */
    static ParseResult parse(String input, BunRelease release) {
        ParseResult result = new ParseResult();
        int partI = 0;
        int partStartI;
        int lastCharI;
        if (input.isEmpty()) {
            result.valid = false;
            return result;
        }
        boolean isDone = false;
        // before 1.4.0 the index stays 0 when only separators are found, which reads one starting with a space as 0.0.0
        int i = release.rejectsAVersionOfSeparatorsAlone() ? input.length() : 0;
        for (int c = 0; c < input.length(); c++) {
            char ch = input.charAt(c);
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\u000B' || ch == '\u000C' || ch == 'v' ||
                ch == '=') {
                continue;
            }
            i = c;
            break;
        }
        if (i == input.length()) {
            result.valid = false;
            if (release.rejectsAVersionOfSeparatorsAlone()) {
                result.wildcard = SemverQuery.Wildcard.MAJOR;
                result.len = i;
            }
            return result;
        }
        while (i < input.length()) {
            if (isDone) {
                break;
            }
            char ch = input.charAt(i);
            if (ch == ' ') {
                break;
            } else if (ch == '|' || ch == '^' || ch == '#' || ch == '&' || ch == '%' || ch == '!') {
                if (i > 0) {
                    i -= 1;
                }
                break;
            } else if (ch >= '0' && ch <= '9') {
                partStartI = i;
                i++;
                while (i < input.length() && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                    i++;
                }
                lastCharI = i;
                Long number = parseVersionNumber(input.substring(partStartI, lastCharI), release);
                if (partI == 0) {
                    result.version.major = number;
                    partI = 1;
                } else if (partI == 1) {
                    result.version.minor = number;
                    partI = 2;
                } else if (partI == 2) {
                    result.version.patch = number;
                    partI = 3;
                }
                if (i < input.length() && input.charAt(i) == '.' && partI != 3) {
                    i++;
                }
            } else if (ch == '.') {
                result.valid = false;
                break;
            } else if (ch == '-' || ch == '+') {
                if (partI < 2 && result.wildcard == SemverQuery.Wildcard.NONE) {
                    result.valid = false;
                    break;
                }
                partStartI = i;
                while (i < input.length() && input.charAt(i) == ' ') {
                    i++;
                }
                TagResult tag = parseTag(input.substring(partStartI));
                result.version.pre = tag.pre;
                result.version.build = tag.build;
                i += tag.len;
                break;
            } else if (ch == 'x' || ch == '*' || ch == 'X') {
                i++;
                while (i < input.length() &&
                       (input.charAt(i) == 'x' || input.charAt(i) == '*' || input.charAt(i) == 'X')) {
                    i++;
                }
                if (i < input.length() && input.charAt(i) == '.') {
                    i++;
                }
                if (result.wildcard == SemverQuery.Wildcard.NONE) {
                    if (partI == 0) {
                        result.wildcard = SemverQuery.Wildcard.MAJOR;
                        partI = 1;
                    } else if (partI == 1) {
                        result.wildcard = SemverQuery.Wildcard.MINOR;
                        partI = 2;
                    } else if (partI == 2) {
                        result.wildcard = SemverQuery.Wildcard.PATCH;
                        partI = 3;
                    } else if (!release.skipsAWildcardAfterThePatchNumber()) {
                        throw new CannotKnow("what bun " + release + " makes of the version \"" + input + "\": a " +
                                             "wildcard right after its patch number has its parser reach code it " +
                                             "takes for unreachable");
                    }
                }
            } else {
                if (result.wildcard == SemverQuery.Wildcard.NONE && partI >= 2 &&
                    (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z')) {
                    partStartI = i;
                    TagResult tag = parseTagWithPreCount(input.substring(partStartI), 1);
                    result.version.pre = tag.pre;
                    result.version.build = tag.build;
                    i += tag.len;
                    isDone = true;
                    break;
                }
                result.valid = false;
                isDone = true;
                break;
            }
        }
        if (result.wildcard == SemverQuery.Wildcard.NONE) {
            if (partI == 0) {
                result.wildcard = SemverQuery.Wildcard.MAJOR;
            } else if (partI == 1) {
                result.wildcard = SemverQuery.Wildcard.MINOR;
            } else if (partI == 2) {
                result.wildcard = SemverQuery.Wildcard.PATCH;
            }
        }
        result.len = i;
        return result;
    }

    /**
     * {@code parseVersionNumber} of the digits the parser scanned: missing past 20 digits, 0 past the u64 maximum; from
     * 1.4.0 the value of any count of digits, 0 past the maximum; before 1.2.23 missing past 10 digits, 0 past the u32
     * maximum.
     */
    private static @Nullable Long parseVersionNumber(String input, BunRelease release) {
        if (release.readsALongVersionNumberByItsValue()) {
            try {
                return Long.parseUnsignedLong(input);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        int digits = release.readsSixtyFourBitVersionNumbers() ? 20 : 10;
        StringBuilder bytes = new StringBuilder();
        for (int k = 0; k < input.length(); k++) {
            char c = input.charAt(k);
            if (c == 'X' || c == 'x' || c == '*') {
                return null;
            }
            if (c >= '0' && c <= '9') {
                if (bytes.length() + 1 > digits) {
                    return null;
                }
                bytes.append(c);
            } else if (c == ' ' || c == '.') {
                break;
            }
        }
        if (bytes.length() == 0) {
            return null;
        }
        try {
            long value = Long.parseUnsignedLong(bytes.toString());
            return Long.compareUnsigned(value, max(release)) > 0 ? 0L : value;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
