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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The npm release an install is reproduced as. The port follows npm 11.17.0; what another release does differently is
 * asked here, one question per upstream change, named for what npm does and answered by the release that changed it.
 * The ported modules read as 11.17.0's source with each other release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version: a release that is
 * {@linkplain #followed() followed} notes each release it was compared with, so that another release giving the same
 * answers ({@link #answersAs}) is known to install the project the same way without installing it again.
 */
public final class NpmRelease implements Comparable<NpmRelease> {

    /**
     * The release the port follows, and the one an install runs as when nothing names another.
     */
    public static final NpmRelease PORTED = new NpmRelease(11, 17, 0, null);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    // the releases an install compared this one with; null when its answers are not being noted
    private final @Nullable Set<NpmRelease> asked;

    private NpmRelease(int major, int minor, int patch, @Nullable Set<NpmRelease> asked) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.asked = asked;
    }

    /**
     * The release of a version such as {@code 10.9.2}; a prerelease or a range names none.
     */
    public static NpmRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not an npm release: " + version);
        }
        return new NpmRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                null);
    }

    /**
     * This release, noting from here on every release an install compares it with.
     */
    public NpmRelease followed() {
        return new NpmRelease(major, minor, patch, new LinkedHashSet<>());
    }

    /**
     * Whether this release answers every question {@code followed} was asked as it did: an install that asked
     * nothing else of its release runs the same under both.
     */
    public boolean answersAs(NpmRelease followed) {
        if (followed.asked == null) {
            return equals(followed);
        }
        for (NpmRelease threshold : followed.asked) {
            if (compareTo(threshold) >= 0 != followed.compareTo(threshold) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        NpmRelease threshold = new NpmRelease(major, minor, patch, null);
        if (asked != null) {
            asked.add(threshold);
        }
        return compareTo(threshold) >= 0;
    }

    // @npmcli/config definitions.js

    /**
     * The default of {@code allow-git}: {@code none} from npm 12.0.0.
     */
    String allowGitDefault() {
        return atLeast(12, 0, 0) ? "none" : "all";
    }

    /**
     * The default of {@code allow-remote}: {@code none} from npm 12.0.0.
     */
    String allowRemoteDefault() {
        return atLeast(12, 0, 0) ? "none" : "all";
    }

    @Override
    public int compareTo(NpmRelease o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) {
            c = Integer.compare(minor, o.minor);
        }
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof NpmRelease && compareTo((NpmRelease) o) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
