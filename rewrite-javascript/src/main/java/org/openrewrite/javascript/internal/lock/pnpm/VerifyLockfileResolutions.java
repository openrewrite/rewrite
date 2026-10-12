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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/installing.deps-installer src/install/verifyLockfileResolutions.ts (pnpm 11.2.2).
 */
final class VerifyLockfileResolutions {

    private static final int MAX_VIOLATIONS_TO_PRINT = 20;

    private static final Pattern HTTP_URL = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);

    private VerifyLockfileResolutions() {
    }

    @Value
    static class ResolutionPolicyViolation {
        String name;
        String version;
        @Nullable
        Map<String, @Nullable Object> resolution;
        String code;
        String reason;
    }

    @Value
    private static class Candidate {
        String name;
        String version;

        @Nullable
        String nonSemverVersion;

        @Nullable
        Map<String, @Nullable Object> resolution;
    }

    static void verifyLockfileResolutions(LockfileObject lockfile, List<? extends ResolutionVerifier> verifiers) {
        verifyLockfileResolutions(lockfile, verifiers, PnpmRelease.PORTED);
    }

    /**
     * The verification as {@code release} makes it: from 11.5.2 an entry's non-semver version (a tarball's URL) tells
     * it from a registry version of the same name ({@link PnpmRelease#bindsALockedTarballUrlToTheRegistry}).
     */
    static void verifyLockfileResolutions(LockfileObject lockfile, List<? extends ResolutionVerifier> verifiers,
                                          PnpmRelease release) {
        if (lockfile.getPackages() == null) {
            return;
        }
        // from 11.7.0 a dependency alias of the lock that is no package name fails first
        List<String> invalidAliases = collectInvalidAliases(lockfile);
        if (!invalidAliases.isEmpty() && release.rejectsAnInvalidDependencyAliasInTheLock()) {
            throw buildInvalidAliasError(invalidAliases);
        }
        // from 11.5.3 a registry dep path backed by another resolution fails ahead of any verifier
        List<ResolutionPolicyViolation> shapeViolations = collectShapeViolations(lockfile, release);
        if (!shapeViolations.isEmpty() && release.rejectsARegistryDepPathWithANonRegistryResolution()) {
            throw buildVerificationError(shapeViolations);
        }
        if (verifiers.isEmpty()) {
            return;
        }
        Map<String, Candidate> candidates = collectCandidates(lockfile, release);
        if (candidates.isEmpty()) {
            return;
        }
        List<ResolutionPolicyViolation> violations = iterateLockfileViolations(candidates, verifiers);
        if (!violations.isEmpty()) {
            throw buildVerificationError(violations);
        }
    }

    static PnpmError buildVerificationError(List<ResolutionPolicyViolation> violations) {
        List<ResolutionPolicyViolation> sorted = new ArrayList<>(violations);
        try {
            sorted.sort((a, b) -> LocaleCompare.en(a.getName() + "@" + a.getVersion(),
                    b.getName() + "@" + b.getVersion()));
        } catch (LocaleCompare.UnknownOrder e) {
            throw new CannotKnow("the order Intl.Collator gives the lockfile entries that failed verification");
        }
        Set<String> distinctCodes = new LinkedHashSet<>();
        for (ResolutionPolicyViolation v : sorted) {
            distinctCodes.add(v.getCode());
        }
        boolean isMixed = distinctCodes.size() > 1;
        String errorCode = isMixed ? "LOCKFILE_RESOLUTION_VERIFICATION" : sorted.get(0).getCode();
        List<ResolutionPolicyViolation> visible = sorted.subList(0, Math.min(MAX_VIOLATIONS_TO_PRINT, sorted.size()));
        int omitted = sorted.size() - visible.size();
        List<String> lines = new ArrayList<>();
        for (ResolutionPolicyViolation v : visible) {
            lines.add(isMixed ?
                    "  " + v.getName() + "@" + v.getVersion() + " [" + v.getCode() + "] " + v.getReason() :
                    "  " + v.getName() + "@" + v.getVersion() + " " + v.getReason());
        }
        String breakdown = String.join("\n", lines);
        String details = omitted > 0 ? breakdown + "\n  …and " + omitted + " more" : breakdown;
        return new PnpmError(errorCode, sorted.size() + " lockfile entries failed verification:\n" + details,
                "The lockfile contains entries that the active policies reject. " +
                "This can mean the lockfile is stale, or that someone committed a " +
                "lockfile that bypassed the policy locally — inspect recent changes " +
                "to pnpm-lock.yaml before trusting it. If the changes look expected, " +
                "run \"pnpm clean --lockfile\" and then \"pnpm install\" to rebuild from " +
                "a fresh resolution. Alternatively, relax the policy that flagged " +
                "them.");
    }

    private static Map<String, Candidate> collectCandidates(LockfileObject lockfile, PnpmRelease release) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        Map<String, PackageSnapshot> packages = lockfile.getPackages();
        if (packages == null) {
            return candidates;
        }
        for (String depPath : Js.keys(packages)) {
            PackageSnapshot snapshot = packages.get(depPath);
            DepPath.DependencyPath pkgInfo = DepPath.parse(depPath);
            String name = pkgInfo.getName();
            String version = snapshot.getVersion() != null ? snapshot.getVersion() : pkgInfo.getVersion();
            if (name == null || name.isEmpty() || version == null || version.isEmpty()) {
                continue;
            }
            String nonSemverVersion = pkgInfo.getNonSemverVersion();
            // 11.5.2's key also names the non-semver version, which tells apart only entries that have one
            String key = name + "@" + version + "@" +
                         (nonSemverVersion != null && release.bindsALockedTarballUrlToTheRegistry() ?
                                 nonSemverVersion + "@" : "") +
                         (snapshot.getResolution() == null ? "undefined" : Js.jsonStringify(snapshot.getResolution()));
            candidates.put(key, new Candidate(name, version, nonSemverVersion, snapshot.getResolution()));
        }
        return candidates;
    }

    // collectCandidates' invalidAliases of 11.7.0: the importers' aliases, then the snapshots', each once
    private static List<String> collectInvalidAliases(LockfileObject lockfile) {
        Set<String> invalid = new LinkedHashSet<>();
        for (ProjectSnapshot importer : lockfile.getImporters().values()) {
            pushInvalidAliases(importer.getDependencies(), invalid);
            pushInvalidAliases(importer.getDevDependencies(), invalid);
            pushInvalidAliases(importer.getOptionalDependencies(), invalid);
        }
        Map<String, PackageSnapshot> packages = lockfile.getPackages();
        if (packages != null) {
            for (PackageSnapshot snapshot : packages.values()) {
                pushInvalidAliases(snapshot.getDependencies(), invalid);
                pushInvalidAliases(snapshot.getOptionalDependencies(), invalid);
            }
        }
        return new ArrayList<>(invalid);
    }

    private static void pushInvalidAliases(@Nullable Map<String, ?> deps, Set<String> invalid) {
        if (deps == null) {
            return;
        }
        for (String alias : deps.keySet()) {
            if (!ValidateNpmPackageName.validForOldPackages(alias, ValidateNpmPackageName.Release.V7_0_2)) {
                invalid.add(alias);
            }
        }
    }

    private static PnpmError buildInvalidAliasError(List<String> aliases) {
        List<String> sorted = new ArrayList<>(aliases);
        Collections.sort(sorted);
        List<String> lines = new ArrayList<>();
        for (String alias : sorted.subList(0, Math.min(MAX_VIOLATIONS_TO_PRINT, sorted.size()))) {
            lines.add("  " + Js.jsonStringify(alias));
        }
        int omitted = sorted.size() - lines.size();
        String details = String.join("\n", lines) + (omitted > 0 ? "\n  …and " + omitted + " more" : "");
        return new PnpmError("INVALID_DEPENDENCY_NAME", "The lockfile contains " + aliases.size() + " dependency " +
                (aliases.size() == 1 ? "alias" : "aliases") + " that are not valid package names:\n" + details,
                "A dependency alias becomes a directory under node_modules, so it must be a valid npm package name " +
                "— a single `name` or `@scope/name` with no leading `.` or `_`, and not a reserved name such as " +
                "`node_modules`. An alias containing path-traversal segments or a reserved name such as `.bin` or " +
                "`.pnpm` could make an install write outside the intended directory or overwrite pnpm-owned " +
                "layout. This usually means the lockfile was tampered with — inspect recent changes to " +
                "pnpm-lock.yaml before trusting it.");
    }

    // collectCandidates' shapeViolations of 11.5.3, one for each dep path
    private static List<ResolutionPolicyViolation> collectShapeViolations(LockfileObject lockfile,
                                                                          PnpmRelease release) {
        List<ResolutionPolicyViolation> violations = new ArrayList<>();
        Map<String, PackageSnapshot> packages = lockfile.getPackages();
        if (packages == null) {
            return violations;
        }
        for (String depPath : Js.keys(packages)) {
            PackageSnapshot snapshot = packages.get(depPath);
            DepPath.DependencyPath pkgInfo = DepPath.parse(depPath);
            String name = pkgInfo.getName();
            String version = snapshot.getVersion() != null ? snapshot.getVersion() : pkgInfo.getVersion();
            if (name == null || name.isEmpty() || version == null || version.isEmpty()) {
                continue;
            }
            if (pkgInfo.getNonSemverVersion() == null &&
                !isRegistryShapedResolution(snapshot.getResolution(), release)) {
                violations.add(new ResolutionPolicyViolation(name, version, snapshot.getResolution(),
                        "RESOLUTION_SHAPE_MISMATCH",
                        "a registry-style dependency path is backed by a non-registry resolution"));
            }
        }
        return violations;
    }

    @SuppressWarnings("unchecked")
    private static boolean isRegistryShapedResolution(@Nullable Object resolution, PnpmRelease release) {
        if (resolution == null || resolution == Js.UNDEFINED) {
            return true;
        }
        if (!(resolution instanceof Map)) {
            return false;
        }
        Map<String, @Nullable Object> r = (Map<String, @Nullable Object>) resolution;
        Object type = r.get("type");
        if ("variations".equals(type)) {
            Object variants = r.get("variants");
            if (!(variants instanceof List)) {
                return false;
            }
            for (Object variant : (List<Object>) variants) {
                if (variant instanceof Map &&
                    !isRegistryShapedResolution(((Map<String, Object>) variant).get("resolution"), release)) {
                    return false;
                }
            }
            return true;
        }
        if (type instanceof String && ((String) type).startsWith("custom:")) {
            return true;
        }
        if (type != null && type != Js.UNDEFINED) {
            return false;
        }
        Object gitHosted = r.get("gitHosted");
        if (gitHosted != null && gitHosted != Js.UNDEFINED && !Boolean.FALSE.equals(gitHosted)) {
            return false;
        }
        Object tarball = r.get("tarball");
        if (tarball instanceof String && !((String) tarball).isEmpty()) {
            String url = (String) tarball;
            if (!HTTP_URL.matcher(url).find()) {
                return false;
            }
            // from 11.9.0 only a commit's archive is git-hosted (PnpmRelease#recognizesOnlyCommitArchivesAsGitHosted)
            boolean gitHostedUrl = LockfileUtils.isGitHostedTarballUrlInAnyCase(url);
            boolean commitArchive = LockfileUtils.isGitHostedCommitArchive(url);
            return !(gitHostedUrl == commitArchive || !release.recognizesOnlyCommitArchivesAsGitHosted() ?
                    gitHostedUrl : commitArchive);
        }
        return true;
    }

    // pnpm checks entries concurrently; each entry's verifiers run in turn and the first failure is its violation
    private static List<ResolutionPolicyViolation> iterateLockfileViolations(
            Map<String, Candidate> candidates, List<? extends ResolutionVerifier> verifiers) {
        List<ResolutionPolicyViolation> violations = new ArrayList<>();
        for (Candidate candidate : candidates.values()) {
            for (ResolutionVerifier verifier : verifiers) {
                ResolutionVerifier.Result result = verifier.verify(candidate.getResolution(), candidate.getName(),
                        candidate.getVersion(), candidate.getNonSemverVersion());
                if (!result.isOk()) {
                    violations.add(new ResolutionPolicyViolation(candidate.getName(), candidate.getVersion(),
                            candidate.getResolution(), result.getCode(), result.getReason()));
                    break;
                }
            }
        }
        return violations;
    }
}
