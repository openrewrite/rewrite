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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.UriComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/config.parse-overrides src/index.ts and @pnpm/resolving.parse-wanted-dependency
 * (pnpm 11.2.2), with validate-npm-package-name 7.0.2's {@code validForOldPackages}.
 */
public final class ParseOverrides {

    /**
     * pnpm's error for an override selector parsePkgAndParentSelector rejects, by its code; null for one it reads.
     */
    public static @Nullable String rejects(String selector) {
        try {
            parsePkgAndParentSelector(selector);
            return null;
        } catch (PnpmError e) {
            return "[" + e.getCode() + "] " + e.getMessage();
        }
    }

    private static final Pattern DELIMITER_REGEX = Pattern.compile("[^ |@]>");
    private static final Pattern SCOPED_PACKAGE = Pattern.compile("^(?:@([^/]+?)[/])?([^/]+?)$");

    private ParseOverrides() {
    }

    @Value
    static class PackageSelector {
        String name;

        @Nullable
        String bareSpecifier;
    }

    @Value
    static class VersionOverride {
        String selector;

        @Nullable
        PackageSelector parentPkg;

        PackageSelector targetPkg;

        String newBareSpecifier;
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
            parsed.add(new VersionOverride(selector, result[0], result[1],
                    resolvedCatalog != null ? resolvedCatalog : newBareSpecifier));
        }
        return parsed;
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
            if (validForOldPackages(alias)) {
                return new ParsedWantedDependency(alias, rawWantedDependency.substring(versionDelimiter + 1));
            }
            return new ParsedWantedDependency(null, rawWantedDependency);
        }
        if (validForOldPackages(rawWantedDependency)) {
            return new ParsedWantedDependency(rawWantedDependency, null);
        }
        return new ParsedWantedDependency(null, rawWantedDependency);
    }

    /**
     * validate-npm-package-name 7.0.2: a name with no errors (its warnings do not count for old packages).
     */
    static boolean validForOldPackages(String name) {
        if (name.isEmpty() || name.startsWith(".") || name.startsWith("-") || name.startsWith("_")) {
            return false;
        }
        if (!Js.trim(name).equals(name)) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if ("node_modules".equals(lower) || "favicon.ico".equals(lower)) {
            return false;
        }
        if (!UriComponent.encodeURIComponent(name).equals(name)) {
            Matcher nameMatch = SCOPED_PACKAGE.matcher(name);
            if (nameMatch.matches()) {
                String user = nameMatch.group(1);
                String pkg = nameMatch.group(2);
                if (pkg.startsWith(".")) {
                    return false;
                }
                if (user != null && UriComponent.encodeURIComponent(user).equals(user) &&
                    UriComponent.encodeURIComponent(pkg).equals(pkg)) {
                    return true;
                }
            }
            return false;
        }
        return true;
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
