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
package org.openrewrite.python.internal.pep440;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A PEP 440 version, ported from pypa/packaging's {@code version.Version}. Ordering replicates
 * packaging's {@code _cmpkey} exactly, including trailing-zero-trimmed release comparison,
 * dev/pre/post sentinel ordering and per-segment local version comparison.
 */
public class PythonVersion implements Comparable<PythonVersion> {
    // Port of packaging's VERSION_PATTERN, full-matched with surrounding whitespace allowed.
    private static final Pattern PATTERN = Pattern.compile(
            "[\\p{IsWhite_Space}]*" +
                    "v?" +
                    "(?:(?<epoch>[0-9]+)!)?" +
                    "(?<release>[0-9]+(?:\\.[0-9]+)*)" +
                    "(?<pre>[._-]?(?<preL>alpha|a|beta|b|preview|pre|c|rc)[._-]?(?<preN>[0-9]+)?)?" +
                    "(?<post>(?:-(?<postN1>[0-9]+))|(?:[._-]?(?<postL>post|rev|r)[._-]?(?<postN2>[0-9]+)?))?" +
                    "(?<dev>[._-]?(?<devL>dev)[._-]?(?<devN>[0-9]+)?)?" +
                    "(?:\\+(?<local>[a-z0-9]+(?:[._-][a-z0-9]+)*))?" +
                    "[\\p{IsWhite_Space}]*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LOCAL_SEPARATORS = Pattern.compile("[._-]");

    private static final long[] EMPTY = new long[0];

    // Sort ranks matching packaging: dev-only < a < b < rc < stable.
    private static final int PRE_RANK_DEV_ONLY = -1;
    private static final int PRE_RANK_STABLE = 3;

    private final String value;
    private final long epoch;
    private final long[] release;
    private final @Nullable String preLetter;
    private final long preNumber;
    private final @Nullable Long post;
    private final @Nullable Long dev;
    // Local segments: localTexts[i] == null means the segment is numeric (localNumbers[i]).
    private final long @Nullable [] localNumbers;
    private final @Nullable String @Nullable [] localTexts;

    // Comparison key per packaging _cmpkey.
    private final long[] trimmedRelease;
    private final long[] suffixKey;

    private PythonVersion(String value, long epoch, long[] release, @Nullable String preLetter, long preNumber,
                          @Nullable Long post, @Nullable Long dev,
                          long @Nullable [] localNumbers, @Nullable String @Nullable [] localTexts) {
        this.value = value;
        this.epoch = epoch;
        this.release = release;
        this.preLetter = preLetter;
        this.preNumber = preNumber;
        this.post = post;
        this.dev = dev;
        this.localNumbers = localNumbers;
        this.localTexts = localTexts;

        int end = release.length;
        while (end > 0 && release[end - 1] == 0) {
            end--;
        }
        this.trimmedRelease = end == release.length ? release : Arrays.copyOf(release, end);

        long preRank;
        long preN;
        if (preLetter == null && post == null && dev != null) {
            preRank = PRE_RANK_DEV_ONLY;
            preN = 0;
        } else if (preLetter == null) {
            preRank = PRE_RANK_STABLE;
            preN = 0;
        } else {
            preRank = "a".equals(preLetter) ? 0 : "b".equals(preLetter) ? 1 : 2;
            preN = preNumber;
        }
        this.suffixKey = new long[]{
                preRank, preN,
                post == null ? 0 : 1, post == null ? 0 : post,
                dev == null ? 1 : 0, dev == null ? 0 : dev
        };
    }

    public static @Nullable PythonVersion parse(@Nullable String version) {
        if (version == null) {
            return null;
        }
        Matcher m = PATTERN.matcher(version);
        if (!m.matches()) {
            return null;
        }
        try {
            long epoch = m.group("epoch") == null ? 0 : Long.parseLong(m.group("epoch"));
            String[] releaseParts = m.group("release").split("\\.");
            long[] release = new long[releaseParts.length];
            for (int i = 0; i < releaseParts.length; i++) {
                release[i] = Long.parseLong(releaseParts[i]);
            }

            String preLetter = null;
            long preNumber = 0;
            String[] pre = parseLetterVersion(m.group("preL"), m.group("preN"));
            if (pre != null) {
                preLetter = pre[0];
                preNumber = Long.parseLong(pre[1]);
            }

            String postNumber = m.group("postN1") != null ? m.group("postN1") : m.group("postN2");
            String[] postParsed = parseLetterVersion(m.group("postL"), postNumber);
            Long post = postParsed == null ? null : Long.parseLong(postParsed[1]);

            String[] devParsed = parseLetterVersion(m.group("devL"), m.group("devN"));
            Long dev = devParsed == null ? null : Long.parseLong(devParsed[1]);

            long[] localNumbers = null;
            String[] localTexts = null;
            String local = m.group("local");
            if (local != null) {
                String[] parts = LOCAL_SEPARATORS.split(local);
                localNumbers = new long[parts.length];
                localTexts = new String[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    if (isDigits(parts[i])) {
                        localNumbers[i] = Long.parseLong(parts[i]);
                    } else {
                        localTexts[i] = parts[i].toLowerCase(Locale.ROOT);
                    }
                }
            }

            return new PythonVersion(version, epoch, release, preLetter, preNumber, post, dev,
                    localNumbers, localTexts);
        } catch (NumberFormatException e) {
            // Numeric components beyond Long range are unsupported.
            return null;
        }
    }

    // Port of packaging _parse_letter_version; returns {letter, number} or null.
    private static String @Nullable [] parseLetterVersion(@Nullable String letter, @Nullable String number) {
        if (letter != null) {
            return new String[]{normalizeLetter(letter.toLowerCase(Locale.ROOT)), number == null ? "0" : number};
        }
        if (number != null) {
            // Implicit post release syntax, e.g. "1.0-1".
            return new String[]{"post", number};
        }
        return null;
    }

    private static String normalizeLetter(String letter) {
        switch (letter) {
            case "alpha":
                return "a";
            case "beta":
                return "b";
            case "c":
            case "pre":
            case "preview":
                return "rc";
            case "rev":
            case "r":
                return "post";
            default:
                return letter;
        }
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    /**
     * The original string this version was parsed from.
     */
    public String getValue() {
        return value;
    }

    public int getEpoch() {
        return (int) epoch;
    }

    public long[] getRelease() {
        return release.clone();
    }

    public @Nullable String getPreLetter() {
        return preLetter;
    }

    public @Nullable Long getPre() {
        return preLetter == null ? null : preNumber;
    }

    public @Nullable Long getPost() {
        return post;
    }

    public @Nullable Long getDev() {
        return dev;
    }

    /**
     * The normalized local version segment (e.g. {@code "abc.1"}), or null.
     */
    public @Nullable String getLocal() {
        if (localNumbers == null || localTexts == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < localNumbers.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(localTexts[i] == null ? Long.toString(localNumbers[i]) : localTexts[i]);
        }
        return sb.toString();
    }

    public boolean isPrerelease() {
        return dev != null || preLetter != null;
    }

    public boolean isPostrelease() {
        return post != null;
    }

    public boolean isDevrelease() {
        return dev != null;
    }

    /**
     * The public portion of the version, i.e. everything before the local segment.
     */
    public String getPublicVersion() {
        String s = toString();
        int plus = s.indexOf('+');
        return plus < 0 ? s : s.substring(0, plus);
    }

    /**
     * The version without pre/post/dev/local segments, e.g. {@code "1!1.0"}.
     */
    public String getBaseVersion() {
        StringBuilder sb = new StringBuilder();
        if (epoch != 0) {
            sb.append(epoch).append('!');
        }
        appendRelease(sb, release);
        return sb.toString();
    }

    private static void appendRelease(StringBuilder sb, long[] release) {
        for (int i = 0; i < release.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(release[i]);
        }
    }

    /**
     * Canonical string form, mirroring packaging's {@code Version.__str__}.
     */
    @Override
    public String toString() {
        return toString(release);
    }

    String toTrimmedString() {
        return toString(trimmedRelease.length == 0 ? new long[]{0} : trimmedRelease);
    }

    private String toString(long[] releaseSegments) {
        StringBuilder sb = new StringBuilder();
        if (epoch != 0) {
            sb.append(epoch).append('!');
        }
        appendRelease(sb, releaseSegments);
        if (preLetter != null) {
            sb.append(preLetter).append(preNumber);
        }
        if (post != null) {
            sb.append(".post").append(post);
        }
        if (dev != null) {
            sb.append(".dev").append(dev);
        }
        String local = getLocal();
        if (local != null) {
            sb.append('+').append(local);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(PythonVersion o) {
        int c = comparePublicKey(o);
        if (c != 0) {
            return c;
        }
        return compareLocal(o);
    }

    // Comparison of (epoch, trimmed release, suffix); ignores the local segment.
    int comparePublicKey(PythonVersion o) {
        int c = Long.compare(epoch, o.epoch);
        if (c != 0) {
            return c;
        }
        c = compareLongArrays(trimmedRelease, o.trimmedRelease);
        if (c != 0) {
            return c;
        }
        return compareLongArrays(suffixKey, o.suffixKey);
    }

    boolean baseEquals(PythonVersion o) {
        return epoch == o.epoch && Arrays.equals(trimmedRelease, o.trimmedRelease);
    }

    private int compareLocal(PythonVersion o) {
        if (localNumbers == null) {
            return o.localNumbers == null ? 0 : -1;
        }
        if (o.localNumbers == null) {
            return 1;
        }
        int n = Math.min(localNumbers.length, o.localNumbers.length);
        for (int i = 0; i < n; i++) {
            // Per PEP 440 string segments sort before numeric ones.
            String a = localTexts == null ? null : localTexts[i];
            String b = o.localTexts == null ? null : o.localTexts[i];
            long an = a == null ? localNumbers[i] : -1;
            long bn = b == null ? o.localNumbers[i] : -1;
            int c = Long.compare(an, bn);
            if (c != 0) {
                return c;
            }
            c = (a == null ? "" : a).compareTo(b == null ? "" : b);
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(localNumbers.length, o.localNumbers.length);
    }

    private static int compareLongArrays(long[] a, long[] b) {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int c = Long.compare(a[i], b[i]);
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(a.length, b.length);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof PythonVersion && compareTo((PythonVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(epoch);
        result = 31 * result + Arrays.hashCode(trimmedRelease);
        result = 31 * result + Arrays.hashCode(suffixKey);
        if (localNumbers != null && localTexts != null) {
            for (int i = 0; i < localNumbers.length; i++) {
                result = 31 * result + (localTexts[i] == null ? Long.hashCode(localNumbers[i]) : localTexts[i].hashCode());
            }
        }
        return result;
    }
}
