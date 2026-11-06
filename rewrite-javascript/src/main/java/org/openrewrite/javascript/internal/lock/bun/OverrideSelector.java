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

/**
 * Port of bun's src/install/lockfile/override_selector.rs (from 1.4.0, {@link BunRelease#readsScopedOverrides}): an
 * override's key as the package it targets and the parent it is scoped to, each a name and a range.
 */
final class OverrideSelector {

    /**
     * {@code PackageSelector}: a name, and the range it is scoped to, empty for every version.
     */
    static final class PackageSelector {
        final String name;
        final String range;

        PackageSelector(String name, String range) {
            this.name = name;
            this.range = range;
        }
    }

    enum Kind {
        EMPTY_NAME, INVALID_NAME, TOO_DEEP, EMPTY_RANGE
    }

    /**
     * {@code SelectorError}.
     */
    static final class Invalid extends Exception {
        final Kind kind;

        Invalid(Kind kind) {
            super(kind.name(), null, false, false);
            this.kind = kind;
        }
    }

    // <[u8]>::trim_ascii's whitespace (u8::is_ascii_whitespace)
    private static final String ASCII_WHITESPACE = " \t\n\u000C\r";

    final @Nullable PackageSelector parent;
    final PackageSelector target;

    private OverrideSelector(@Nullable PackageSelector parent, PackageSelector target) {
        this.parent = parent;
        this.target = target;
    }

    private static boolean isBareScope(String name) {
        return name.startsWith("@") && name.indexOf('/') < 0;
    }

    /**
     * {@code parse_package_segment}: {@code name}, {@code name@range} or {@code name@npm:range}.
     */
    static PackageSelector parsePackageSegment(String segment) throws Invalid {
        if (segment.isEmpty()) {
            throw new Invalid(Kind.EMPTY_NAME);
        }
        String[] split = Dependency.splitNameAndMaybeVersion(segment);
        String name = split[0];
        if (name.isEmpty()) {
            throw new Invalid(Kind.EMPTY_NAME);
        }
        if (isBareScope(name)) {
            throw new Invalid(Kind.INVALID_NAME);
        }
        String range = split[1] == null ? "" : split[1];
        if (range.startsWith("npm:")) {
            range = range.substring("npm:".length());
        }
        if (range.isEmpty() && segment.length() > name.length()) {
            throw new Invalid(Kind.EMPTY_RANGE);
        }
        return new PackageSelector(name, range);
    }

    /**
     * {@code parse_selector}: a comment ({@code //}), pnpm's {@code parent>child}, or a Yarn path of at most two
     * packages.
     */
    static OverrideSelector parse(String key) throws Invalid {
        if (key.startsWith("//")) {
            return new OverrideSelector(null, new PackageSelector(key, ""));
        }
        int delimiter = pnpmDelimiter(key);
        if (delimiter >= 0) {
            return parsePnpm(key, delimiter);
        }
        return parseYarnPath(key);
    }

    // pnpm's parent>child delimiter (/[^ |@]>/): the first > not preceded by a space, | or @
    private static int pnpmDelimiter(String key) {
        int from = 1;
        while (from < key.length()) {
            int i = key.indexOf('>', from);
            if (i < 0) {
                return -1;
            }
            char before = key.charAt(i - 1);
            if (before == ' ' || before == '|' || before == '@') {
                from = i + 1;
            } else {
                return i;
            }
        }
        return -1;
    }

    private static OverrideSelector parsePnpm(String key, int delimiter) throws Invalid {
        String parent = Strings.trim(key.substring(0, delimiter), ASCII_WHITESPACE);
        String name = Strings.trim(key.substring(delimiter + 1), ASCII_WHITESPACE);
        if (name.isEmpty()) {
            throw new Invalid(Kind.EMPTY_NAME);
        }
        if (pnpmDelimiter(name) >= 0) {
            throw new Invalid(Kind.TOO_DEEP);
        }
        PackageSelector parentSelector = parsePackageSegment(parent);
        return new OverrideSelector(parentSelector, parsePackageSegment(name));
    }

    private static OverrideSelector parseYarnPath(String key) throws Invalid {
        String[] segments = new String[2];
        int count = 0;
        // the start of the next /-delimited token, -1 past the last
        int cursor = 0;
        while (cursor >= 0) {
            int start = cursor;
            int slash = key.indexOf('/', start);
            String token = slash < 0 ? key.substring(start) : key.substring(start, slash);
            int next = slash < 0 ? -1 : slash + 1;
            cursor = next;
            if (token.isEmpty()) {
                throw new Invalid(Kind.EMPTY_NAME);
            }
            if ("**".equals(token)) {
                if (next < 0) {
                    throw new Invalid(Kind.EMPTY_NAME);
                }
                continue;
            }
            int end = start + token.length();
            if (token.startsWith("@")) {
                if (next < 0) {
                    throw new Invalid(Kind.INVALID_NAME);
                }
                int nameSlash = key.indexOf('/', next);
                String name = nameSlash < 0 ? key.substring(next) : key.substring(next, nameSlash);
                cursor = nameSlash < 0 ? -1 : nameSlash + 1;
                if (name.isEmpty()) {
                    throw new Invalid(Kind.EMPTY_NAME);
                }
                end = next + name.length();
            }
            if (count == segments.length) {
                throw new Invalid(Kind.TOO_DEEP);
            }
            segments[count++] = key.substring(start, end);
        }
        if (count == 0) {
            throw new Invalid(Kind.EMPTY_NAME);
        }
        if (count == 1) {
            return new OverrideSelector(null, parsePackageSegment(segments[0]));
        }
        PackageSelector parent = parsePackageSegment(segments[0]);
        return new OverrideSelector(parent, parsePackageSegment(segments[1]));
    }
}
