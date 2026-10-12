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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/config.parse-overrides src/index.ts and @pnpm/resolving.parse-wanted-dependency
 * (pnpm 11.2.2).
 */
final class ParseOverrides {

    /**
     * pnpm's error for an override selector parsePkgAndParentSelector rejects, by its code; null for one it reads.
     */
    static @Nullable String rejects(String selector) {
        try {
            parsePkgAndParentSelector(selector);
            return null;
        } catch (PnpmError e) {
            return "[" + e.getCode() + "] " + e.getMessage();
        }
    }

    private static final Pattern DELIMITER_REGEX = Pattern.compile("[^ |@]>");

    private ParseOverrides() {
    }

    @Value
    static class PackageSelector {
        String name;

        @Nullable
        String bareSpecifier;
    }

    @Value
    @AllArgsConstructor
    static class VersionOverride {
        String selector;

        @Nullable
        PackageSelector parentPkg;

        PackageSelector targetPkg;

        String newBareSpecifier;

        /**
         * From 11.13.0, a convergence override ({@code pkg@}), whose exact version replaces only a declared range it
         * satisfies ({@link PnpmRelease#convergesAnEmptyRangeOverride}).
         */
        boolean converge;

        VersionOverride(String selector, @Nullable PackageSelector parentPkg, PackageSelector targetPkg,
                        String newBareSpecifier) {
            this(selector, parentPkg, targetPkg, newBareSpecifier, false);
        }
    }

    /**
     * {@code parseWantedDependency}: an alias, a bare specifier, or both.
     */
    @Value
    static class ParsedWantedDependency {
        @Nullable
        String alias;

        @Nullable
        String bareSpecifier;
    }

    static List<VersionOverride> parseOverrides(Map<String, String> overrides,
                                                Map<String, @Nullable Map<String, @Nullable Object>> catalogs) {
        return parseOverrides(overrides, catalogs, PnpmRelease.PORTED);
    }

    /**
     * {@link #parseOverrides(Map, Map)} as {@code release} parses them: from 11.13.0 an override of an empty range
     * converges ({@link PnpmRelease#convergesAnEmptyRangeOverride}); before, it is one as any other.
     */
    static List<VersionOverride> parseOverrides(Map<String, String> overrides,
                                                Map<String, @Nullable Map<String, @Nullable Object>> catalogs,
                                                PnpmRelease release) {
        List<VersionOverride> parsed = new ArrayList<>();
        for (String selector : Js.keys(overrides)) {
            String newBareSpecifier = overrides.get(selector);
            PackageSelector[] result = parsePkgAndParentSelector(selector);
            Catalogs.Resolution resolution = Catalogs.resolveFromCatalog(catalogs, result[1].getName(),
                    newBareSpecifier);
            String resolvedCatalog;
            switch (resolution.getType()) {
                case "found":
                    resolvedCatalog = resolution.getSpecifier();
                    break;
                case "misconfiguration":
                    throw new PnpmError("CATALOG_IN_OVERRIDES", "Could not resolve a catalog in the overrides: " +
                                                                resolution.getError().getMessage());
                default:
                    resolvedCatalog = null;
            }
            parsed.add(markConvergeOverride(new VersionOverride(selector, result[0], result[1],
                    resolvedCatalog != null ? resolvedCatalog : newBareSpecifier), release));
        }
        return parsed;
    }

    // markConvergeOverride (11.13.0): an override of an empty range is a convergence override, its value an exact
    // version and its selector no parent>child one; asked of the release only for an empty range
    private static VersionOverride markConvergeOverride(VersionOverride override, PnpmRelease release) {
        PackageSelector parentPkg = override.getParentPkg();
        boolean emptyTargetRange = "".equals(override.getTargetPkg().getBareSpecifier());
        if (parentPkg != null && ("".equals(parentPkg.getBareSpecifier()) || emptyTargetRange)) {
            if (release.convergesAnEmptyRangeOverride()) {
                throw new PnpmError("INVALID_CONVERGENCE_OVERRIDE", "Cannot use an empty range in the \"" +
                        override.getSelector() + "\" selector: convergence overrides (\"pkg@\") cannot be combined " +
                        "with parent>child selectors");
            }
            return override;
        }
        if (!emptyTargetRange || !release.convergesAnEmptyRangeOverride()) {
            return override;
        }
        if (release.underSemver(semver -> NodeSemver.valid(override.getNewBareSpecifier(),
                NodeSemver.Options.DEFAULT.release(semver))) == null) {
            throw new PnpmError("INVALID_CONVERGENCE_OVERRIDE", "The value of the convergence override \"" +
                    override.getSelector() + "\" must be an exact version, but got \"" +
                    override.getNewBareSpecifier() + "\"");
        }
        return new VersionOverride(override.getSelector(), parentPkg, override.getTargetPkg(),
                override.getNewBareSpecifier(), true);
    }

    /**
     * The parent selector (null without one) and the target.
     */
    static PackageSelector[] parsePkgAndParentSelector(String selector) {
        Matcher m = DELIMITER_REGEX.matcher(selector);
        if (m.find()) {
            int delimiterIndex = m.start() + 1;
            String parentSelector = selector.substring(0, delimiterIndex);
            String childSelector = selector.substring(delimiterIndex + 1);
            return new PackageSelector[]{parsePkgSelector(parentSelector), parsePkgSelector(childSelector)};
        }
        return new PackageSelector[]{null, parsePkgSelector(selector)};
    }

    private static PackageSelector parsePkgSelector(String selector) {
        ParsedWantedDependency wantedDep = parseWantedDependency(selector);
        if (wantedDep.getAlias() == null || wantedDep.getAlias().isEmpty()) {
            throw new PnpmError("INVALID_SELECTOR", "Cannot parse the \"" + selector + "\" selector");
        }
        return new PackageSelector(wantedDep.getAlias(), wantedDep.getBareSpecifier());
    }

    static ParsedWantedDependency parseWantedDependency(String rawWantedDependency) {
        int versionDelimiter = rawWantedDependency.indexOf('@', 1);
        if (versionDelimiter != -1) {
            String alias = rawWantedDependency.substring(0, versionDelimiter);
            if (ValidateNpmPackageName.validForOldPackages(alias, ValidateNpmPackageName.Release.V7_0_2)) {
                return new ParsedWantedDependency(alias, rawWantedDependency.substring(versionDelimiter + 1));
            }
            return new ParsedWantedDependency(null, rawWantedDependency);
        }
        if (ValidateNpmPackageName.validForOldPackages(rawWantedDependency,
                ValidateNpmPackageName.Release.V7_0_2)) {
            return new ParsedWantedDependency(rawWantedDependency, null);
        }
        return new ParsedWantedDependency(null, rawWantedDependency);
    }

    /**
     * @pnpm/lockfile.settings-checker {@code createOverridesMapFromParsed}: the {@code overrides} block of the lock.
     */
    static Map<String, @Nullable Object> createOverridesMapFromParsed(@Nullable List<VersionOverride> parsedOverrides) {
        Map<String, @Nullable Object> overridesMap = new LinkedHashMap<>();
        if (parsedOverrides == null) {
            return overridesMap;
        }
        for (VersionOverride override : parsedOverrides) {
            overridesMap.put(override.getSelector(), override.getNewBareSpecifier());
        }
        return overridesMap;
    }
}
