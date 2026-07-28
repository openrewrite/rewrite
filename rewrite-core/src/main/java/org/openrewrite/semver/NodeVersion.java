/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.semver;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single npm/node-semver version ({@code major.minor.patch[-prerelease][+build]}), ported from
 * node-semver's {@code SemVer} class and {@code re.js} {@code FULL} pattern. This is a deliberately
 * <em>separate</em> value type from the Maven-flavored {@link ParsedVersion}/{@link VersionComparator}
 * tokenizer, whose {@code RELEASE_PATTERN} fuses prerelease and build metadata and accepts 4th/5th
 * numeric components -- both wrong for node. Only the {@link LruCache} parse cache is shared.
 * <p>
 * Ordering follows semver.org section 11: numeric fields compare numerically, a version with a prerelease
 * has lower precedence than the same version without, prerelease identifiers compare
 * identifier-by-identifier (numeric &lt; alphanumeric), and build metadata is ignored entirely.
 */
public final class NodeVersion implements Comparable<NodeVersion> {

    // Port of node-semver re.js FULL (major.minor.patch, no leading zeros, prerelease and build split).
    private static final String NUMERIC = "0|[1-9]\\d*";
    private static final String PRERELEASE_ID = "(?:" + NUMERIC + "|\\d*[a-zA-Z-][0-9a-zA-Z-]*)";
    private static final Pattern FULL = Pattern.compile(
            "^v?(" + NUMERIC + ")\\.(" + NUMERIC + ")\\.(" + NUMERIC + ")" +
                    "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))?" +
                    "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    private static final int MAX_CACHE_SIZE = 4_096;
    private static final Map<String, NodeVersion> CACHE = LruCache.bounded(MAX_CACHE_SIZE);
    private static final NodeVersion INVALID = new NodeVersion("", 0, 0, 0, new ArrayList<>(), new ArrayList<>());

    private final String raw;
    private final int major;
    private final int minor;
    private final int patch;
    // Each element is a Long (numeric identifier) or a String (alphanumeric identifier).
    private final List<Object> prerelease;
    private final List<String> build;

    private NodeVersion(String raw, int major, int minor, int patch, List<Object> prerelease, List<String> build) {
        this.raw = raw;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease;
        this.build = build;
    }

    public static @Nullable NodeVersion parse(@Nullable String version) {
        if (version == null) {
            return null;
        }
        NodeVersion cached = CACHE.get(version);
        if (cached != null) {
            return cached == INVALID ? null : cached;
        }
        NodeVersion parsed = doParse(version);
        CACHE.put(version, parsed == null ? INVALID : parsed);
        return parsed;
    }

    private static @Nullable NodeVersion doParse(String version) {
        Matcher m = FULL.matcher(version.trim());
        if (!m.matches()) {
            return null;
        }
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = Integer.parseInt(m.group(3));
        List<Object> prerelease = new ArrayList<>();
        if (m.group(4) != null) {
            for (String id : m.group(4).split("\\.")) {
                prerelease.add(isNumeric(id) ? (Object) Long.parseLong(id) : id);
            }
        }
        List<String> build = new ArrayList<>();
        if (m.group(5) != null) {
            for (String id : m.group(5).split("\\.")) {
                build.add(id);
            }
        }
        return new NodeVersion(version, major, minor, patch, prerelease, build);
    }

    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    public String getRaw() {
        return raw;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public List<Object> getPrerelease() {
        return prerelease;
    }

    public boolean isPrerelease() {
        return !prerelease.isEmpty();
    }

    @Override
    public int compareTo(NodeVersion o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, o.minor);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(patch, o.patch);
        if (c != 0) {
            return c;
        }
        return comparePrerelease(o);
    }

    private int comparePrerelease(NodeVersion o) {
        // A version with a prerelease has lower precedence than the same without one.
        if (prerelease.isEmpty() && o.prerelease.isEmpty()) {
            return 0;
        }
        if (prerelease.isEmpty()) {
            return 1;
        }
        if (o.prerelease.isEmpty()) {
            return -1;
        }
        int n = Math.min(prerelease.size(), o.prerelease.size());
        for (int i = 0; i < n; i++) {
            int c = compareIdentifiers(prerelease.get(i), o.prerelease.get(i));
            if (c != 0) {
                return c;
            }
        }
        // All shared identifiers equal: the longer prerelease list is higher.
        return Integer.compare(prerelease.size(), o.prerelease.size());
    }

    // node-semver compareIdentifiers: numeric < alphanumeric; numerics compare as numbers, others as ASCII.
    private static int compareIdentifiers(Object a, Object b) {
        boolean aNum = a instanceof Long;
        boolean bNum = b instanceof Long;
        if (aNum && bNum) {
            return Long.compare((Long) a, (Long) b);
        }
        if (aNum) {
            return -1;
        }
        if (bNum) {
            return 1;
        }
        return ((String) a).compareTo((String) b);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof NodeVersion && compareTo((NodeVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        for (Object id : prerelease) {
            result = 31 * result + id.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (!prerelease.isEmpty()) {
            sb.append('-').append(join(prerelease));
        }
        if (!build.isEmpty()) {
            sb.append('+').append(join(build));
        }
        return sb.toString();
    }

    private static String join(List<?> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
