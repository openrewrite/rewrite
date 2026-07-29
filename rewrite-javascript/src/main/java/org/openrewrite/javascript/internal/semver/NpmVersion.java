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
package org.openrewrite.javascript.internal.semver;

import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A strict node-semver version: {@code v?major.minor.patch(-prerelease)?(+build)?}.
 * Comparison semantics are ported from npm/node-semver {@code classes/semver.js}
 * and validated against node-semver's own comparison/equality fixtures.
 */
public final class NpmVersion implements Comparable<NpmVersion> {

    private static final String NUMERIC_ID = "0|[1-9]\\d*";
    private static final String NON_NUMERIC_ID = "\\d*[a-zA-Z-][a-zA-Z0-9-]*";
    private static final String PRERELEASE_ID = "(?:" + NON_NUMERIC_ID + "|" + NUMERIC_ID + ")";
    static final String PRERELEASE = "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))";
    static final String BUILD = "(?:\\+([a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*))";
    private static final Pattern FULL = Pattern.compile(
            "^v?(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")" +
                    PRERELEASE + "?" + BUILD + "?$");

    private final long major;
    private final long minor;
    private final long patch;
    private final String[] prerelease;
    private final String raw;

    private NpmVersion(long major, long minor, long patch, String[] prerelease, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease;
        this.raw = raw;
    }

    public static @Nullable NpmVersion parse(@Nullable String version) {
        if (version == null) {
            return null;
        }
        Matcher m = FULL.matcher(version.trim());
        if (!m.matches()) {
            return null;
        }
        String pre = m.group(4);
        String[] preIds = pre == null ? new String[0] : pre.split("\\.");
        return new NpmVersion(
                Long.parseLong(m.group(1)),
                Long.parseLong(m.group(2)),
                Long.parseLong(m.group(3)),
                preIds,
                version.trim());
    }

    public long getMajor() {
        return major;
    }

    public long getMinor() {
        return minor;
    }

    public long getPatch() {
        return patch;
    }

    public boolean hasPrerelease() {
        return prerelease.length > 0;
    }

    boolean sameTuple(NpmVersion other) {
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int compareTo(NpmVersion o) {
        int c = Long.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Long.compare(minor, o.minor);
        if (c != 0) {
            return c;
        }
        c = Long.compare(patch, o.patch);
        if (c != 0) {
            return c;
        }
        return comparePrerelease(o);
    }

    private int comparePrerelease(NpmVersion o) {
        if (prerelease.length == 0 && o.prerelease.length == 0) {
            return 0;
        }
        if (prerelease.length == 0) {
            return 1;
        }
        if (o.prerelease.length == 0) {
            return -1;
        }
        for (int i = 0; ; i++) {
            if (i >= prerelease.length && i >= o.prerelease.length) {
                return 0;
            }
            if (i >= prerelease.length) {
                return -1;
            }
            if (i >= o.prerelease.length) {
                return 1;
            }
            int c = compareIdentifier(prerelease[i], o.prerelease[i]);
            if (c != 0) {
                return c;
            }
        }
    }

    private static int compareIdentifier(String a, String b) {
        boolean aNum = a.chars().allMatch(Character::isDigit);
        boolean bNum = b.chars().allMatch(Character::isDigit);
        if (aNum && bNum) {
            return new BigInteger(a).compareTo(new BigInteger(b));
        }
        if (aNum) {
            return -1;
        }
        if (bNum) {
            return 1;
        }
        return Integer.signum(a.compareTo(b));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NpmVersion)) {
            return false;
        }
        return compareTo((NpmVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{major, minor, patch, Arrays.hashCode(prerelease)});
    }

    @Override
    public String toString() {
        return raw;
    }
}
