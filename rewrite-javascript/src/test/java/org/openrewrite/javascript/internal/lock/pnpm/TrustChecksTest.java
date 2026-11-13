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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * resolving/npm-resolver/test/trustChecks.test.ts of pnpm 11.2.2.
 */
class TrustChecksTest {

    private static final String TRUSTED_PUBLISHER = ",'_npmUser':{'name':'test-publisher'," +
                                                    "'email':'publisher@example.com','trustedPublisher':" +
                                                    "{'id':'test-provider','oidcConfigId':'oidc:test-config-123'}}";
    private static final String PROVENANCE = ",'attestations':{'provenance':" +
                                             "{'predicateType':'https://slsa.dev/provenance/v1'}}";

    private static String version(String name, String version, String dist, String rest) {
        return "'" + version + "':{'name':'" + name + "','version':'" + version + "','dist':{'shasum':'abc123'," +
               "'tarball':'https://registry.example.com/" + name + "/-/" + name + "-" + version + ".tgz'" + dist +
               "}" + rest + "}";
    }

    private static Map<String, @Nullable Object> meta(String name, String latest, String versions,
                                                      @Nullable String time) {
        return obj("{'name':'" + name + "','dist-tags':{'latest':'" + latest + "'},'versions':{" + versions + "}" +
                   (time == null ? "" : ",'time':{" + time + "}") + "}");
    }

    private static void failIfTrustDowngraded(Map<String, @Nullable Object> meta, String version) {
        failIfTrustDowngraded(meta, version, null, null);
    }

    private static void failIfTrustDowngraded(Map<String, @Nullable Object> meta, String version,
                                              @Nullable Function<String, Object> trustPolicyExclude,
                                              @Nullable Double trustPolicyIgnoreAfter) {
        TrustChecks.failIfTrustDowngraded(meta, version, trustPolicyExclude, trustPolicyIgnoreAfter, Instant.now());
    }

    @Test
    void getTrustEvidenceReturnsTrustedPublisherWhenNpmUserTrustedPublisherExists() {
        assertThat(TrustChecks.getTrustEvidence(obj("{" + version("foo", "1.0.0", "", TRUSTED_PUBLISHER) + "}")
          .get("1.0.0"))).isEqualTo("trustedPublisher");
    }

    @Test
    void getTrustEvidenceReturnsTrustedPublisherEvenWhenAttestationsProvenanceExists() {
        assertThat(TrustChecks.getTrustEvidence(obj("{" + version("foo", "2.0.0", PROVENANCE, TRUSTED_PUBLISHER) +
                                                    "}").get("2.0.0"))).isEqualTo("trustedPublisher");
    }

    @Test
    void getTrustEvidenceReturnsProvenanceWhenProvenanceExists() {
        assertThat(TrustChecks.getTrustEvidence(obj("{" + version("foo", "1.0.0", PROVENANCE, "") + "}")
          .get("1.0.0"))).isEqualTo("provenance");
    }

    @Test
    void getTrustEvidenceReturnsUndefinedWhenProvenanceAndAttestationsAreUndefined() {
        assertThat(TrustChecks.getTrustEvidence(obj("{" + version("foo", "1.0.0", "", "") + "}").get("1.0.0")))
          .isNull();
    }

    @Test
    void getTrustEvidenceReturnsUndefinedWhenNpmUserExistsButTrustedPublisherIsUndefined() {
        assertThat(TrustChecks.getTrustEvidence(obj("{" + version("foo", "1.0.0", "",
          ",'_npmUser':{'name':'test-user','email':'user@example.com'}") + "}").get("1.0.0"))).isNull();
    }

    @Test
    void succeedsWhenNoVersionsHaveAttestation() {
        assertThatCode(() -> failIfTrustDowngraded(meta("foo", "2.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0", "", ""),
          "'1.0.0':'2025-01-01T00:00:00.000Z','2.0.0':'2025-02-01T00:00:00.000Z'"), "2.0.0"))
          .doesNotThrowAnyException();
    }

    @Test
    void succeedsForVersionPublishedBeforeFirstAttestedVersion() {
        assertThatCode(() -> failIfTrustDowngraded(meta("foo", "2.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0", PROVENANCE, ""),
          "'1.0.0':'2025-01-01T00:00:00.000Z','2.0.0':'2025-02-01T00:00:00.000Z'"), "1.0.0"))
          .doesNotThrowAnyException();
    }

    private static final String THREE_TIMES = "'1.0.0':'2025-01-01T00:00:00.000Z','2.0.0':'2025-02-01T00:00:00.000Z'," +
                                              "'3.0.0':'2025-03-01T00:00:00.000Z'";

    @Test
    void throwsAnErrorWhenDowngradingFromProvenanceToNone() {
        assertThatThrownBy(() -> failIfTrustDowngraded(meta("foo", "3.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0", PROVENANCE, "") + "," +
          version("foo", "3.0.0", "", ""), THREE_TIMES), "3.0.0"))
          .hasMessageContaining("High-risk trust downgrade");
    }

    @Test
    void doesNotThrowAnErrorWhenOnlyPrereleaseVersionsHadProvenance() {
        assertThatCode(() -> failIfTrustDowngraded(meta("foo", "3.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0-0", PROVENANCE, "") + "," +
          version("foo", "3.0.0", "", ""), "'1.0.0':'2025-01-01T00:00:00.000Z'," +
                                           "'2.0.0-0':'2025-02-01T00:00:00.000Z','3.0.0':'2025-03-01T00:00:00.000Z'"),
          "3.0.0")).doesNotThrowAnyException();
    }

    @Test
    void throwsAnErrorWhenDowngradingFromTrustedPublisherToProvenance() {
        assertThatThrownBy(() -> failIfTrustDowngraded(meta("foo", "3.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0", "", TRUSTED_PUBLISHER) + "," +
          version("foo", "3.0.0", PROVENANCE, ""), THREE_TIMES), "3.0.0"))
          .hasMessageContaining("High-risk trust downgrade");
    }

    @Test
    void throwsAnErrorWhenDowngradingFromTrustedPublisherToNone() {
        assertThatThrownBy(() -> failIfTrustDowngraded(meta("foo", "3.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0", "", TRUSTED_PUBLISHER) + "," +
          version("foo", "3.0.0", "", ""), THREE_TIMES), "3.0.0"))
          .hasMessageContaining("High-risk trust downgrade");
    }

    @Test
    void succeedsWhenMaintainingSameTrustLevel() {
        assertThatCode(() -> failIfTrustDowngraded(meta("foo", "3.0.0",
          version("foo", "1.0.0", "", "") + "," + version("foo", "2.0.0", "", TRUSTED_PUBLISHER) + "," +
          version("foo", "3.0.0", "", TRUSTED_PUBLISHER), THREE_TIMES), "3.0.0"))
          .doesNotThrowAnyException();
    }

    @Test
    void throwsAnErrorWhenVersionTimeIsMissing() {
        assertThatThrownBy(() -> failIfTrustDowngraded(meta("foo", "2.0.0",
          version("foo", "1.0.0", PROVENANCE, "") + "," + version("foo", "2.0.0", "", ""),
          "'1.0.0':'2025-01-01T00:00:00.000Z'"), "2.0.0"))
          .hasMessageContaining("Missing time");
    }

    private static final String DOWNGRADE_TIMES =
      "'2.0.0':'2025-02-01T00:00:00.000Z','3.0.0':'2025-03-01T00:00:00.000Z'";

    @Test
    void allowsDowngradeWhenPackageAtVersionIsInExcludeList() {
        Map<String, @Nullable Object> meta = meta("foo", "3.0.0",
          version("foo", "2.0.0", PROVENANCE, "") + "," + version("foo", "3.0.0", "", ""), DOWNGRADE_TIMES);
        assertThatCode(() -> failIfTrustDowngraded(meta, "3.0.0",
          PackageVersionPolicy.createPackageVersionPolicy(singletonList("foo@3.0.0")), null))
          .doesNotThrowAnyException();
        assertThatThrownBy(() -> failIfTrustDowngraded(meta, "3.0.0"))
          .hasMessageContaining("High-risk trust downgrade");
    }

    @Test
    void allowsDowngradeWhenPackageNameIsInExcludeList() {
        Map<String, @Nullable Object> meta = meta("bar", "3.0.0",
          version("bar", "2.0.0", "", TRUSTED_PUBLISHER) + "," + version("bar", "3.0.0", "", ""), DOWNGRADE_TIMES);
        assertThatCode(() -> failIfTrustDowngraded(meta, "3.0.0",
          PackageVersionPolicy.createPackageVersionPolicy(singletonList("bar")), null))
          .doesNotThrowAnyException();
    }

    @Test
    void doesNotFailWithMissingTimeWhenPackageAtVersionIsExcludedAndTimeFieldIsMissing() {
        assertThatCode(() -> failIfTrustDowngraded(meta("baz", "1.0.0", version("baz", "1.0.0", "", ""), null),
          "1.0.0", PackageVersionPolicy.createPackageVersionPolicy(singletonList("baz@1.0.0")), null))
          .doesNotThrowAnyException();
    }

    @Test
    void doesNotFailWithMissingTimeWhenPackageNameIsExcludedAndTimeFieldIsMissing() {
        assertThatCode(() -> failIfTrustDowngraded(meta("qux", "2.0.0",
          version("qux", "1.0.0", "", "") + "," + version("qux", "2.0.0", "", ""), null),
          "2.0.0", PackageVersionPolicy.createPackageVersionPolicy(singletonList("qux")), null))
          .doesNotThrowAnyException();
    }

    @Test
    void allowsDowngradeWhenVersionIsOlderThanIgnoreAfterThreshold() {
        Map<String, @Nullable Object> meta = meta("foo", "3.0.0",
          version("foo", "2.0.0", PROVENANCE, "") + "," + version("foo", "3.0.0", "", ""), DOWNGRADE_TIMES);
        assertThatCode(() -> failIfTrustDowngraded(meta, "3.0.0", null, 60.0 * 24 * 30))
          .doesNotThrowAnyException();
        assertThatThrownBy(() -> failIfTrustDowngraded(meta, "3.0.0"))
          .hasMessageContaining("High-risk trust downgrade");
    }
}
