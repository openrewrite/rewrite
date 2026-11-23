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
package org.openrewrite.javascript.semver;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver.Options;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedMap;

/**
 * Port of node-semver classes/semver.js: a version under its {@code FULL} grammar, or {@code LOOSE} when
 * {@code loose}. One that does not parse is not {@linkplain #isStrictSemver() strict}.
 */
final class NodeSemverVersion {

    // node-semver re.js grammar fragments, shared with the range and comparator.
    static final String NUMERIC_ID = "0|[1-9]\\d*";
    static final String PRERELEASE_ID = "(?:" + NUMERIC_ID + "|\\d*[a-zA-Z-][0-9a-zA-Z-]*)";
    static final String PRERELEASE_GROUP = "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))";
    static final String BUILD = "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)";

    static final String PRERELEASE_ID_LOOSE = "(?:\\d*[a-zA-Z-][0-9a-zA-Z-]*|\\d+)";
    static final String PRERELEASE_GROUP_LOOSE = "(?:-?(" + PRERELEASE_ID_LOOSE + "(?:\\." + PRERELEASE_ID_LOOSE +
            ")*))";
    static final String LOOSE_PLAIN = "[v=\\s]*(?:\\d+)\\.(?:\\d+)\\.(?:\\d+)" + PRERELEASE_GROUP_LOOSE + "?" + BUILD +
            "?";

    // Port of node-semver re.js FULL (major.minor.patch, no leading zeros, prerelease and build split).
    private static final Pattern STRICT_PATTERN = Pattern.compile(
            "^v?(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")" +
                    "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))?" +
                    "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    // node-semver re.js LOOSE: leading v/=/whitespace, leading zeros, and a prerelease without its hyphen.
    private static final Pattern LOOSE_PATTERN = Pattern.compile(
            "^[v=\\s]*(\\d+)\\.(\\d+)\\.(\\d+)" +
                    "(?:-?(" + PRERELEASE_ID_LOOSE + "(?:\\." + PRERELEASE_ID_LOOSE + ")*))?" +
                    "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    private static final Pattern NUMERIC = Pattern.compile("[0-9]+");

    private static final int MAX_LENGTH = 256;
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    private static final Map<String, NodeSemverVersion> CACHE = lru(4_096);
    private static final Map<String, NodeSemverVersion> LOOSE_CACHE = lru(1_024);

    private static final NodeSemverVersion NO_MATCH = new NodeSemverVersion(false, 0, 0, 0, emptyList(), emptyList());

    private final boolean strictSemver;

    private final long strictMajor;
    private final long strictMinor;
    private final long strictPatch;

    // Each element is a Long (numeric identifier) or a String (alphanumeric identifier).
    private final List<Object> strictPrerelease;
    private final List<String> strictBuild;

    private NodeSemverVersion(boolean strictSemver, long strictMajor, long strictMinor, long strictPatch,
                              List<Object> strictPrerelease, List<String> strictBuild) {
        this.strictSemver = strictSemver;
        this.strictMajor = strictMajor;
        this.strictMinor = strictMinor;
        this.strictPatch = strictPatch;
        this.strictPrerelease = strictPrerelease;
        this.strictBuild = strictBuild;
    }

    // Recipes run on a ForkJoinPool, so the map is synchronized.
    static <K, V> Map<K, V> lru(int maxSize) {
        return synchronizedMap(new LinkedHashMap<K, V>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    /**
     * node-semver's {@code new SemVer(version, options)}. Unparseable input yields a non-strict result.
     */
    static NodeSemverVersion parse(String version, Options options) {
        Map<String, NodeSemverVersion> cache = options.loose ? LOOSE_CACHE : CACHE;
        NodeSemverVersion cached = cache.get(version);
        if (cached != null) {
            return cached;
        }
        NodeSemverVersion parsed = nodeView(options.loose ? LOOSE_PATTERN : STRICT_PATTERN, version);
        if (parsed == null) {
            parsed = NO_MATCH;
        }
        cache.put(version, parsed);
        return parsed;
    }

    private static @Nullable NodeSemverVersion nodeView(Pattern grammar, String version) {
        if (version.length() > MAX_LENGTH) {
            return null;
        }
        Matcher m = grammar.matcher(version.trim());
        if (!m.matches()) {
            return null;
        }
        // node-semver rejects components beyond Number.MAX_SAFE_INTEGER.
        try {
            long major = Long.parseLong(m.group(1));
            long minor = Long.parseLong(m.group(2));
            long patch = Long.parseLong(m.group(3));
            if (major > MAX_SAFE_INTEGER || minor > MAX_SAFE_INTEGER || patch > MAX_SAFE_INTEGER) {
                return null;
            }
            List<Object> prerelease = emptyList();
            if (m.group(4) != null) {
                prerelease = new ArrayList<>();
                for (String id : m.group(4).split("\\.")) {
                    prerelease.add(numericIdentifier(id));
                }
            }
            List<String> build = emptyList();
            if (m.group(5) != null) {
                build = new ArrayList<>();
                for (String id : m.group(5).split("\\.")) {
                    build.add(id);
                }
            }
            return new NodeSemverVersion(true, major, minor, patch, prerelease, build);
        } catch (NumberFormatException overflow) {
            return null;
        }
    }

    private static Object numericIdentifier(String id) {
        if (isNumeric(id)) {
            try {
                long num = Long.parseLong(id);
                if (num < MAX_SAFE_INTEGER) {
                    return num;
                }
            } catch (NumberFormatException ignored) {
                // a numeric identifier beyond long range stays alphanumeric, as in node-semver
            }
        }
        return id;
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

    boolean isStrictSemver() {
        return strictSemver;
    }

    long strictMajor() {
        return strictMajor;
    }

    long strictMinor() {
        return strictMinor;
    }

    long strictPatch() {
        return strictPatch;
    }

    /**
     * @return the prerelease identifiers, each a {@code Long} (numeric) or a {@code String} (alphanumeric); empty
     * when there is none or the version is not strict.
     */
    List<Object> strictPrerelease() {
        return strictPrerelease;
    }

    boolean hasPrerelease() {
        return !strictPrerelease.isEmpty();
    }

    /**
     * node-semver's {@code SemVer.version}: the strict rendering without build metadata.
     */
    String nodeVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append(strictMajor).append('.').append(strictMinor).append('.').append(strictPatch);
        if (!strictPrerelease.isEmpty()) {
            sb.append('-').append(join(strictPrerelease));
        }
        return sb.toString();
    }

    /**
     * SemVer 2.0.0 section 11 precedence; build metadata ignored. Both versions must be
     * {@linkplain #isStrictSemver() strict}.
     */
    int comparePrecedence(NodeSemverVersion o) {
        int c = Long.compare(strictMajor, o.strictMajor);
        if (c != 0) {
            return c;
        }
        c = Long.compare(strictMinor, o.strictMinor);
        if (c != 0) {
            return c;
        }
        c = Long.compare(strictPatch, o.strictPatch);
        if (c != 0) {
            return c;
        }
        return comparePrerelease(o);
    }

    private int comparePrerelease(NodeSemverVersion o) {
        // A version with a prerelease has lower precedence than the same without one.
        if (strictPrerelease.isEmpty() && o.strictPrerelease.isEmpty()) {
            return 0;
        }
        if (strictPrerelease.isEmpty()) {
            return 1;
        }
        if (o.strictPrerelease.isEmpty()) {
            return -1;
        }
        int n = Math.min(strictPrerelease.size(), o.strictPrerelease.size());
        for (int i = 0; i < n; i++) {
            int c = compareIdentifiers(strictPrerelease.get(i), o.strictPrerelease.get(i));
            if (c != 0) {
                return c;
            }
        }
        // All shared identifiers equal: the longer prerelease list is higher.
        return Integer.compare(strictPrerelease.size(), o.strictPrerelease.size());
    }

    /**
     * node-semver's {@code SemVer.compareBuild}: the build identifiers in order, a missing one lowest, the first two
     * that differ as strings deciding through {@code compareIdentifiers}.
     */
    int compareBuild(NodeSemverVersion o) {
        for (int i = 0; ; i++) {
            if (i >= strictBuild.size() && i >= o.strictBuild.size()) {
                return 0;
            }
            if (i >= o.strictBuild.size()) {
                return 1;
            }
            if (i >= strictBuild.size()) {
                return -1;
            }
            String a = strictBuild.get(i);
            String b = o.strictBuild.get(i);
            if (!a.equals(b)) {
                return compareStringIdentifiers(a, b);
            }
        }
    }

    // node-semver compareIdentifiers over two strings: numeric ones as numbers, below any other
    private static int compareStringIdentifiers(String a, String b) {
        boolean anum = NUMERIC.matcher(a).matches();
        boolean bnum = NUMERIC.matcher(b).matches();
        if (anum && bnum) {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        }
        if (anum) {
            return -1;
        }
        if (bnum) {
            return 1;
        }
        return a.compareTo(b) < 0 ? -1 : 1;
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
