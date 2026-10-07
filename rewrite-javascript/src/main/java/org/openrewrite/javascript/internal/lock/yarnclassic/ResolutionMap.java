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
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of yarn src/resolution-map.js and util/parse-package-path.js (yarn 1.22.22): the root manifest's
 * {@code resolutions}, each a glob over the path of package names from a root dependency down and the pattern it
 * substitutes.
 */
final class ResolutionMap {

    private static final String GLOBAL_NESTED_DEP_PATTERN = "**/";
    private static final Pattern PACKAGE_PATH = Pattern.compile("(@[^/]+/)?([^/]+)");
    private static final Pattern WRONG_PATTERNS = Pattern.compile("/$|/{2,}|\\*+$");

    @Value
    static class Resolution {
        String name;
        String range;
        String globPattern;
        String pattern;
    }

    final Map<String, List<Resolution>> resolutionsByPackage = new LinkedHashMap<>();
    final YarnClassicOptions config;
    final Set<DependencyRequestPattern> delayQueue = new LinkedHashSet<>();
    final List<String> warnings = new ArrayList<>();

    ResolutionMap(YarnClassicOptions config) {
        this.config = config;
    }

    /**
     * Reads the {@code resolutions} field.
     *
     * @throws JsError the {@code TypeError} yarn raises on a value that is not a string, an object among them.
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
            warnings.add(Lang.format("Resolution field $0 does not end with a valid package name and will be ignored",
                    globPattern));
            return null;
        }
        List<String> directories = parsePackagePath(globPattern);
        String name = directories.isEmpty() ? null : directories.remove(directories.size() - 1);

        if (!(range instanceof String)) {
            // getExoticResolver(range) runs each resolver's isVersion, whose first string method fails on it
            throw new JsError(JsError.TYPE_ERROR, range instanceof Map ? "Cannot convert object to primitive value" :
                    "pattern.startsWith is not a function");
        }
        String r = (String) range;
        if (NodeSemver.validRange(r, YarnClassicOptions.SEMVER) == null &&
            ExoticResolvers.getExoticResolver(r) == null) {
            warnings.add(Lang.format("Resolution field $0 has an invalid version entry and may be ignored", r));
            return null;
        }

        if (globPattern.equals(name)) {
            globPattern = GLOBAL_NESTED_DEP_PATTERN + name;
        }
        return new Resolution(String.valueOf(name), r, globPattern, name + "@" + r);
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
