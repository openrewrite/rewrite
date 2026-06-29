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

import java.util.Map;
import java.util.regex.Matcher;

import static org.openrewrite.semver.VersionComparator.PRE_RELEASE_ENDING;
import static org.openrewrite.semver.VersionComparator.RELEASE_PATTERN;

/**
 * The cached result of matching a version string against {@link VersionComparator#RELEASE_PATTERN}
 * (and {@link VersionComparator#PRE_RELEASE_ENDING}).
 * <p>
 * Version selection evaluates a selector against the <em>full</em> published version list of a
 * dependency. Without caching, every candidate allocates a fresh {@link Matcher} (and its
 * capture-group array). Because parsing is pure and the same version strings recur across
 * dependencies and, at scale, across the modules of a large multi-module build, the parse results
 * are memoized here. This removes both the {@code Matcher} and the regex-group allocations for
 * repeat parses, which dominate allocation during a large multi-module dependency upgrade.
 * <p>
 * The cache is a small bounded LRU. The win comes from re-use <em>across</em> {@code upgrade(...)}
 * calls (the same version list is re-evaluated for each module of a multi-module build), so the
 * cache must outlive a single call; the LRU caps the retained footprint and evicts cold entries. The
 * distinct version-<em>string</em> set stays small even for broad upgrades because version numbers
 * repeat across the artifacts of a release train (e.g. {@code spring-boot-starter:3.0.0} and
 * {@code spring-boot-actuator:3.0.0} share the string {@code "3.0.0"}), so the working set fits
 * within the bound without thrashing.
 */
final class ParsedVersion {

    /**
     * Upper bound on retained entries. Comfortably larger than the distinct version-string working
     * set of a large multi-module upgrade (hundreds to low thousands), while keeping the retained
     * footprint to a few hundred kilobytes.
     */
    private static final int MAX_CACHE_SIZE = 4_096;

    private static final Map<String, ParsedVersion> CACHE = LruCache.bounded(MAX_CACHE_SIZE);

    private static final ParsedVersion NO_MATCH = new ParsedVersion(false, null, null, false);

    private final boolean matches;

    /**
     * {@link VersionComparator#RELEASE_PATTERN} numeric capture groups 1..5 (major, minor, patch,
     * micro, and a fifth component), each {@code null} when the corresponding group is absent.
     * {@code null} as a whole when the version did not match.
     */
    private final @Nullable String @Nullable [] groups;

    private final @Nullable String qualifier;

    private final boolean preReleaseEnding;

    private ParsedVersion(boolean matches, @Nullable String @Nullable [] groups, @Nullable String qualifier, boolean preReleaseEnding) {
        this.matches = matches;
        this.groups = groups;
        this.qualifier = qualifier;
        this.preReleaseEnding = preReleaseEnding;
    }

    static ParsedVersion parse(String version) {
        ParsedVersion cached = CACHE.get(version);
        if (cached != null) {
            return cached;
        }
        ParsedVersion parsed = doParse(version);
        CACHE.put(version, parsed);
        return parsed;
    }

    private static ParsedVersion doParse(String version) {
        Matcher matcher = RELEASE_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return NO_MATCH;
        }
        @Nullable String[] groups = new String[]{
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                matcher.group(4),
                matcher.group(5)
        };
        String qualifier = matcher.group("qualifier");
        boolean preReleaseEnding = PRE_RELEASE_ENDING.matcher(version).find();
        return new ParsedVersion(true, groups, qualifier, preReleaseEnding);
    }

    boolean matches() {
        return matches;
    }

    /**
     * @param index a 1-based {@link VersionComparator#RELEASE_PATTERN} capture-group index in the
     *              range 1..5 (major, minor, patch, micro, fifth).
     * @return the captured numeric component, or {@code null} if that component is absent or the
     * version did not match the release pattern.
     */
    @Nullable String group(int index) {
        return groups == null ? null : groups[index - 1];
    }

    /**
     * @return the {@code qualifier} named group (the {@code [-.+]}-prefixed suffix), or {@code null}
     * if there is no qualifier or the version did not match the release pattern.
     */
    @Nullable String qualifier() {
        return qualifier;
    }

    /**
     * @return whether the version ends with a recognized pre-release qualifier (alpha, beta,
     * milestone, rc, snapshot, ...) per {@link VersionComparator#PRE_RELEASE_ENDING}.
     */
    boolean isPreReleaseEnding() {
        return preReleaseEnding;
    }
}
