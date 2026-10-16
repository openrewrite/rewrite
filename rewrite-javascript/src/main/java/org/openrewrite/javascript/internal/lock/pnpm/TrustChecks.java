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

import org.openrewrite.javascript.internal.lock.V8Date;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Port of @pnpm/resolving.npm-resolver src/trustChecks.ts (pnpm 11.2.2); a trusted publisher counts with a provenance
 * attestation only from 11.4.0 ({@link PnpmRelease#requiresProvenanceOfATrustedPublisher}), and a staged publish is
 * the strongest evidence from 11.5.0 ({@link PnpmRelease#ranksAStagedPublish}).
 */
final class TrustChecks {

    private static final String STAGED_PUBLISH = "stagedPublish";
    private static final String TRUSTED_PUBLISHER = "trustedPublisher";
    private static final String PROVENANCE = "provenance";

    private TrustChecks() {
    }

    /**
     * {@link #failIfTrustDowngraded(Map, String, Function, Double, Instant, PnpmRelease)} as 11.2.2 runs it.
     */
    static void failIfTrustDowngraded(Map<String, @Nullable Object> meta, String version,
                                      @Nullable Function<String, Object> trustPolicyExclude,
                                      @Nullable Double trustPolicyIgnoreAfter, Instant now) {
        failIfTrustDowngraded(meta, version, trustPolicyExclude, trustPolicyIgnoreAfter, now, PnpmRelease.PORTED);
    }

    /**
     * @param now the clock {@code trustPolicyIgnoreAfter} counts from
     */
    @SuppressWarnings("unchecked")
    static void failIfTrustDowngraded(Map<String, @Nullable Object> meta, String version,
                                      @Nullable Function<String, Object> trustPolicyExclude,
                                      @Nullable Double trustPolicyIgnoreAfter, Instant now, PnpmRelease release) {
        if (trustPolicyExclude != null) {
            Object excludeResult = trustPolicyExclude.apply(Js.string(meta.get("name")));
            if (Boolean.TRUE.equals(excludeResult)) {
                return;
            }
            if (excludeResult instanceof List && ((List<?>) excludeResult).contains(version)) {
                return;
            }
        }
        PickPackageFromMeta.assertMetaHasTime(meta);
        Object versionPublishedAt = property(meta.get("time"), version);
        if (!Js.truthy(versionPublishedAt)) {
            throw new PnpmError("TRUST_CHECK_FAIL", "Missing time for version " + version + " of " +
                                                    Js.string(meta.get("name")) + " in metadata");
        }
        double versionDate = V8Date.dateParse(Js.string(versionPublishedAt));
        if (trustPolicyIgnoreAfter != null && trustPolicyIgnoreAfter != 0 && !trustPolicyIgnoreAfter.isNaN()) {
            double minutesSincePublish = (now.toEpochMilli() - versionDate) / (1000 * 60);
            if (minutesSincePublish > trustPolicyIgnoreAfter) {
                return;
            }
        }
        Object manifest = property(meta.get("versions"), version);
        if (!Js.truthy(manifest)) {
            throw new PnpmError("TRUST_CHECK_FAIL", "Missing version object for version " + version + " of " +
                                                    Js.string(meta.get("name")) + " in metadata");
        }
        String strongestEvidencePriorToRequestedVersion = detectStrongestTrustEvidenceBeforeDate(meta, versionDate,
                NodeSemver.prerelease(version, NodeSemver.Options.LOOSE) == null, release);
        if (strongestEvidencePriorToRequestedVersion == null) {
            return;
        }
        String currentTrustEvidence = getTrustEvidence(manifest, release);
        if (currentTrustEvidence == null ||
            rank(strongestEvidencePriorToRequestedVersion) > rank(currentTrustEvidence)) {
            throw new PnpmError("TRUST_DOWNGRADE", "High-risk trust downgrade for \"" +
                                                   Js.string(meta.get("name")) + "@" + version +
                                                   "\" (possible package takeover)",
                    "Trust checks are based solely on publish date, not semver. " +
                    "A package cannot be installed if any earlier-published version had stronger trust evidence. " +
                    "Earlier versions had " + prettyPrintTrustEvidence(strongestEvidencePriorToRequestedVersion) +
                    ", but this version has " + prettyPrintTrustEvidence(currentTrustEvidence) + ". " +
                    "A trust downgrade may indicate a supply chain incident.");
        }
    }

    // TRUST_RANK of 11.5.0; before, a trusted publisher over anything else
    private static int rank(String trustEvidence) {
        return STAGED_PUBLISH.equals(trustEvidence) ? 3 : TRUSTED_PUBLISHER.equals(trustEvidence) ? 2 : 1;
    }

    private static String prettyPrintTrustEvidence(@Nullable String trustEvidence) {
        if (STAGED_PUBLISH.equals(trustEvidence)) {
            return "staged publish";
        }
        if (TRUSTED_PUBLISHER.equals(trustEvidence)) {
            return "trusted publisher";
        }
        if (PROVENANCE.equals(trustEvidence)) {
            return "provenance attestation";
        }
        return "no trust evidence";
    }

    @SuppressWarnings("unchecked")
    private static @Nullable String detectStrongestTrustEvidenceBeforeDate(Map<String, @Nullable Object> meta,
                                                                          double beforeDate,
                                                                          boolean excludePrerelease,
                                                                          PnpmRelease release) {
        String best = null;
        Object versions = meta.get("versions");
        if (!(versions instanceof Map)) {
            return null;
        }
        for (String version : Js.keys((Map<String, @Nullable Object>) versions)) {
            if (excludePrerelease && NodeSemver.prerelease(version, NodeSemver.Options.LOOSE) != null) {
                continue;
            }
            Object ts = property(meta.get("time"), version);
            if (!Js.truthy(ts)) {
                continue;
            }
            double publishedAt = V8Date.dateParse(Js.string(ts));
            if (!(publishedAt < beforeDate)) {
                continue;
            }
            String trustEvidence = getTrustEvidence(((Map<String, @Nullable Object>) versions).get(version), release);
            if (trustEvidence == null) {
                continue;
            }
            // 11.5.0's ranking; 11.2.2 returns at the first trusted publisher, which ranks highest of what it finds,
            // as only a release from 11.5.0 finds a staged publish
            if (best == null || rank(trustEvidence) > rank(best)) {
                best = trustEvidence;
                if (STAGED_PUBLISH.equals(best)) {
                    return best;
                }
            }
        }
        return best;
    }

    /**
     * {@link #getTrustEvidence(Object, PnpmRelease)} as 11.2.2 reads it.
     */
    static @Nullable String getTrustEvidence(@Nullable Object manifest) {
        return getTrustEvidence(manifest, PnpmRelease.PORTED);
    }

    static @Nullable String getTrustEvidence(@Nullable Object manifest, PnpmRelease release) {
        Object npmUser = property(manifest, "_npmUser");
        Object provenance = property(property(property(manifest, "dist"), "attestations"), PROVENANCE);
        if (Js.truthy(property(npmUser, "approver")) && release.ranksAStagedPublish()) {
            return STAGED_PUBLISH;
        }
        if (Js.truthy(property(npmUser, TRUSTED_PUBLISHER)) &&
            (Js.truthy(provenance) || !release.requiresProvenanceOfATrustedPublisher())) {
            return TRUSTED_PUBLISHER;
        }
        if (Js.truthy(provenance)) {
            return PROVENANCE;
        }
        return null;
    }

    // value?.[key] over the JSON values the port reads
    @SuppressWarnings("unchecked")
    private static @Nullable Object property(@Nullable Object value, String key) {
        return value instanceof Map ? ((Map<String, @Nullable Object>) value).get(key) : null;
    }
}
