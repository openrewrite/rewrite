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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/installing.deps-installer src/install/verifyLockfileResolutions.ts (pnpm 11.2.2).
 */
final class VerifyLockfileResolutions {

    private static final int MAX_VIOLATIONS_TO_PRINT = 20;

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
        Map<String, @Nullable Object> resolution;
    }

    static void verifyLockfileResolutions(LockfileObject lockfile, List<? extends ResolutionVerifier> verifiers) {
        if (verifiers.isEmpty() || lockfile.getPackages() == null) {
            return;
        }
        Map<String, Candidate> candidates = collectCandidates(lockfile);
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

    private static Map<String, Candidate> collectCandidates(LockfileObject lockfile) {
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
            String key = name + "@" + version + "@" +
                         (snapshot.getResolution() == null ? "undefined" : Js.jsonStringify(snapshot.getResolution()));
            candidates.put(key, new Candidate(name, version, snapshot.getResolution()));
        }
        return candidates;
    }

    // pnpm checks entries concurrently; each entry's verifiers run in turn and the first failure is its violation
    private static List<ResolutionPolicyViolation> iterateLockfileViolations(
            Map<String, Candidate> candidates, List<? extends ResolutionVerifier> verifiers) {
        List<ResolutionPolicyViolation> violations = new ArrayList<>();
        for (Candidate candidate : candidates.values()) {
            for (ResolutionVerifier verifier : verifiers) {
                ResolutionVerifier.Result result = verifier.verify(candidate.getResolution(), candidate.getName(),
                        candidate.getVersion());
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
