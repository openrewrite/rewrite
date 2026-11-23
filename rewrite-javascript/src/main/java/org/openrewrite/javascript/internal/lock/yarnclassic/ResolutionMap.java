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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Minimatch;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of yarn src/resolution-map.js and util/parse-package-path.js (yarn 1.22.22).
 */
final class ResolutionMap {

    private static final String GLOBAL_NESTED_DEP_PATTERN = "**/";
    private static final Pattern PACKAGE_PATH = Pattern.compile("(@[^/]+/)?([^/]+)");
    private static final Pattern WRONG_PATTERNS = Pattern.compile("/\\z|/{2,}|\\*+\\z");

    @Value
    static class Resolution {
        String name;
        String range;
        String globPattern;
        String pattern;
    }

    final Map<String, List<Resolution>> resolutionsByPackage = new LinkedHashMap<>();
    final Config config;
    final Set<DependencyRequestPattern> delayQueue = new LinkedHashSet<>();

    /**
     * The root's patterns, whose lock entries a resolution keeps from 1.3.1 to 1.5.1 ({@code setTopLevelPatterns}).
     */
    Set<String> topLevelPatterns = new LinkedHashSet<>();

    ResolutionMap(Config config) {
        this.config = config;
    }

    /**
     * Reads the {@code resolutions} field.
     * @throws JsError the {@code TypeError} the exotic resolvers raise on a value that is not a string.
     */
    void init(@Nullable Map<String, @Nullable Object> resolutions) {
        if (resolutions == null) {
            return;
        }
        for (String globPattern : Js.keys(resolutions)) {
            Resolution info = parsePatternInfo(globPattern, resolutions.get(globPattern));
            if (info != null) {
                resolutionsByPackage.computeIfAbsent(info.getName(), k -> new ArrayList<>()).add(info);
            }
        }
    }

    void addToDelayQueue(DependencyRequestPattern req) {
        delayQueue.add(req);
    }

    @Nullable
    Resolution parsePatternInfo(String globPattern, @Nullable Object range) {
        if (!isValidPackagePath(globPattern)) {
            return null;
        }
        List<String> directories = parsePackagePath(globPattern);
        String name = directories.isEmpty() ? null : directories.remove(directories.size() - 1);

        boolean validRange = range instanceof String &&
                             NodeSemver.validRange((String) range, Config.SEMVER) != null;
        // the field's own null, which fix.js empties from 1.3.1, is JSON's for the git resolver's url.parse
        if (range == null) {
            throw new JsError(Js.INVALID_ARG_TYPE, "The \"url\" argument must be of type string. Received null");
        }
        if (!validRange && ExoticResolvers.getExoticResolver(range, config.getRelease()) == null) {
            return null;
        }
        String r = Js.string(range);

        if (globPattern.equals(name)) {
            globPattern = GLOBAL_NESTED_DEP_PATTERN + name;
        }
        return new Resolution(Js.string(name), r, globPattern, Js.string(name) + "@" + r);
    }

    /**
     * The pattern of the first resolution whose glob matches the request's path from the root's dependency down,
     * {@code ""} when no resolution names the package, null when none matches.
     */
    @Nullable
    String find(String reqPattern, List<String> parentNames) {
        NormalizedPattern normalized = NormalizedPattern.normalizePattern(reqPattern);
        List<Resolution> resolutions = resolutionsByPackage.get(normalized.getName());
        if (resolutions == null) {
            return "";
        }
        List<String> path = new ArrayList<>(parentNames);
        path.add(normalized.getName());
        String modulePath = String.join("/", path);
        for (Resolution resolution : resolutions) {
            if (Minimatch.minimatch(modulePath, resolution.getGlobPattern(), Minimatch.Release.V3_0_4)) {
                return resolution.getPattern();
            }
        }
        return null;
    }

    /**
     * The glob yarn matches a resolution's key against, as parsePatternInfo makes it ({@code **}{@code /name} for a
     * bare name); null for a key it ignores.
     */
    static @Nullable String globPattern(String key) {
        if (!isValidPackagePath(key)) {
            return null;
        }
        List<String> directories = parsePackagePath(key);
        return directories.size() == 1 && key.equals(directories.get(0)) ? GLOBAL_NESTED_DEP_PATTERN + key : key;
    }

    /**
     * The keys of {@code resolutions} for the package {@code key} names that find tries before {@code key}, in the
     * order init reads them: every one when {@code key} is not there.
     */
    static List<String> triedBefore(Map<String, @Nullable Object> resolutions, String key) {
        List<String> before = new ArrayList<>();
        List<String> path = parsePackagePath(key);
        String name = path.isEmpty() ? null : path.get(path.size() - 1);
        for (String other : Js.keys(resolutions)) {
            if (other.equals(key)) {
                break;
            }
            List<String> otherPath = parsePackagePath(other);
            Object range = resolutions.get(other);
            if (isValidPackagePath(other) && !otherPath.isEmpty() &&
                otherPath.get(otherPath.size() - 1).equals(name) && (ExoticResolvers.getExoticResolver(range) != null ||
                range instanceof String && NodeSemver.validRange((String) range, Config.SEMVER) != null)) {
                before.add(other);
            }
        }
        return before;
    }

    static List<String> parsePackagePath(String input) {
        List<String> parts = new ArrayList<>();
        Matcher m = PACKAGE_PATH.matcher(input);
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }

    static boolean isValidPackagePath(String input) {
        return !WRONG_PATTERNS.matcher(input).find();
    }

    static boolean shouldUpdateLockfile(@Nullable Map<String, @Nullable Object> lockfileEntry,
                                        @Nullable PackageReference resolutionEntry) {
        if (lockfileEntry == null || resolutionEntry == null) {
            return false;
        }
        Object resolved = lockfileEntry.get("resolved");
        return resolved == null ? resolutionEntry.remote.resolved != null :
                !resolved.equals(resolutionEntry.remote.resolved);
    }
}
