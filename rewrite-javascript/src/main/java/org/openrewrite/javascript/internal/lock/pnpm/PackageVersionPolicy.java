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
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Port of @pnpm/config.version-policy src/index.ts (pnpm 11.2.2): a policy answers true for every version of a
 * package, the exact versions it names, or false.
 */
final class PackageVersionPolicy {

    private PackageVersionPolicy() {
    }

    static Function<String, Object> createPackageVersionPolicy(List<String> patterns) {
        return createPackageVersionPolicy(patterns, PnpmRelease.PORTED);
    }

    /**
     * The policy as {@code release} evaluates it: from 11.9.0 the exact versions of every rule that matches are
     * merged until a rule of the bare name; before, the first rule that matches decides
     * ({@link PnpmRelease#mergesExactVersionsAcrossPolicyRules}), asked where the two differ.
     */
    static Function<String, Object> createPackageVersionPolicy(List<String> patterns, PnpmRelease release) {
        List<VersionPolicyRule> rules = new ArrayList<>();
        for (String pattern : patterns) {
            ParsedVersionPolicyRule parsed = parseVersionPolicyRule(pattern);
            rules.add(new VersionPolicyRule(Matcher.createMatcher(parsed.packageName), parsed.exactVersions));
        }
        return pkgName -> {
            Object first = evaluateVersionPolicy(rules, pkgName);
            Object merged = evaluateMergedVersionPolicy(rules, pkgName);
            return first.equals(merged) || !release.mergesExactVersionsAcrossPolicyRules() ? first : merged;
        };
    }

    // evaluateVersionPolicy of 11.9.0
    private static Object evaluateMergedVersionPolicy(List<VersionPolicyRule> rules, String pkgName) {
        List<String> matchedVersions = null;
        for (VersionPolicyRule rule : rules) {
            if (!rule.nameMatcher.test(pkgName)) {
                continue;
            }
            if (rule.exactVersions.isEmpty()) {
                return matchedVersions == null ? Boolean.TRUE : matchedVersions;
            }
            if (matchedVersions == null) {
                matchedVersions = new ArrayList<>();
            }
            for (String version : rule.exactVersions) {
                if (!matchedVersions.contains(version)) {
                    matchedVersions.add(version);
                }
            }
        }
        return matchedVersions == null ? Boolean.FALSE : matchedVersions;
    }

    static Function<String, Object> createPackageVersionPolicyOrThrow(List<String> patterns, String key) {
        return createPackageVersionPolicyOrThrow(patterns, key, PnpmRelease.PORTED);
    }

    static Function<String, Object> createPackageVersionPolicyOrThrow(List<String> patterns, String key,
                                                                      PnpmRelease release) {
        try {
            return createPackageVersionPolicy(patterns, release);
        } catch (PnpmError e) {
            throw new PnpmError("INVALID_" + key.replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT),
                    "Invalid value in " + key + ": " + e.getMessage());
        }
    }

    @Value
    static class PublishedByPolicy {
        @Nullable
        Instant publishedBy;

        @Nullable
        Function<String, Object> publishedByExclude;
    }

    /**
     * {@code getPublishedByPolicy}, its {@code Date.now()} the given clock.
     */
    static PublishedByPolicy getPublishedByPolicy(@Nullable Double minimumReleaseAge,
                                                  @Nullable List<String> minimumReleaseAgeExclude, Instant now) {
        return new PublishedByPolicy(
                minimumReleaseAge != null && minimumReleaseAge != 0 && !minimumReleaseAge.isNaN() ?
                        Instant.ofEpochMilli((long) (now.toEpochMilli() - minimumReleaseAge * 60 * 1000)) : null,
                minimumReleaseAgeExclude != null ?
                        createPackageVersionPolicyOrThrow(minimumReleaseAgeExclude, "minimumReleaseAgeExclude") : null);
    }

    /**
     * {@code isExcluded} of the npm resolver's verifier: whether the policy covers this version.
     */
    static boolean isExcluded(@Nullable Function<String, Object> policy, String name, String version) {
        if (policy == null) {
            return false;
        }
        Object result = policy.apply(name);
        return Boolean.TRUE.equals(result) || result instanceof List && ((List<?>) result).contains(version);
    }

    private static Object evaluateVersionPolicy(List<VersionPolicyRule> rules, String pkgName) {
        for (VersionPolicyRule rule : rules) {
            if (!rule.nameMatcher.test(pkgName)) {
                continue;
            }
            if (rule.exactVersions.isEmpty()) {
                return true;
            }
            return rule.exactVersions;
        }
        return false;
    }

    private static final class VersionPolicyRule {
        final Predicate<String> nameMatcher;
        final List<String> exactVersions;

        VersionPolicyRule(Predicate<String> nameMatcher, List<String> exactVersions) {
            this.nameMatcher = nameMatcher;
            this.exactVersions = exactVersions;
        }
    }

    private static final class ParsedVersionPolicyRule {
        final String packageName;
        final List<String> exactVersions;

        ParsedVersionPolicyRule(String packageName, List<String> exactVersions) {
            this.packageName = packageName;
            this.exactVersions = exactVersions;
        }
    }

    private static ParsedVersionPolicyRule parseVersionPolicyRule(String pattern) {
        boolean isScoped = pattern.startsWith("@");
        int atIndex = isScoped ? pattern.indexOf('@', 1) : pattern.indexOf('@');
        if (atIndex == -1) {
            return new ParsedVersionPolicyRule(pattern, new ArrayList<>());
        }
        String packageName = pattern.substring(0, atIndex);
        String versionsPart = pattern.substring(atIndex + 1);
        List<String> exactVersions = parseExactVersionsUnion(versionsPart);
        if (exactVersions == null) {
            throw new PnpmError("INVALID_VERSION_UNION",
                    "Invalid versions union. Found: \"" + pattern + "\". Use exact versions only.");
        }
        if (packageName.contains("*")) {
            throw new PnpmError("NAME_PATTERN_IN_VERSION_UNION",
                    "Name patterns are not allowed with version unions. Found: \"" + pattern + "\"");
        }
        return new ParsedVersionPolicyRule(packageName, exactVersions);
    }

    private static @Nullable List<String> parseExactVersionsUnion(String versionsStr) {
        List<String> versions = new ArrayList<>();
        for (String versionRaw : versionsStr.split("\\|\\|", -1)) {
            String version = NodeSemver.valid(versionRaw, NodeSemver.Options.DEFAULT);
            if (version == null) {
                return null;
            }
            versions.add(version);
        }
        return versions;
    }
}
