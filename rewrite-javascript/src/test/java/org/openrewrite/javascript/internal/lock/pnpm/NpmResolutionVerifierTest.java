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
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * pnpm 11.2.2's createNpmResolutionVerifier.test.ts and fetchAttestationPublishedAt.test.ts, less canTrustPastCheck (a
 * cache file) and the attestation request's URL and auth header, the host's from {@link RegistryMetadata#attestations}.
 */
class NpmResolutionVerifierTest {

    private static final Map<String, String> REGISTRIES = singletonMap("default", "https://registry.npmjs.org/");

    /**
     * The registry the upstream tests mock: a document by path whatever the Accept header, attestations by spec.
     */
    private static final class MockRegistry implements RegistryMetadata {
        final Map<String, String> documents = new HashMap<>();
        final Map<String, String> attestations = new HashMap<>();

        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return documents.get(name);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return attestations.get(name + "@" + version);
        }

        @Override
        public @Nullable String document(String url) {
            return documents.get(url);
        }
    }

    private static NpmResolutionVerifier.Options.OptionsBuilder makeVerifierOpts(RegistryMetadata registry) {
        return NpmResolutionVerifier.Options.builder()
          .registries(REGISTRIES)
          .fetch(registry)
          .now(Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static Map<String, @Nullable Object> makeTarballResolution(String name, String version) {
        StringBuilder integrity = new StringBuilder("sha512-");
        for (int i = 0; i < 86; i++) {
            integrity.append('A');
        }
        return obj("{'integrity':'" + integrity + "==','tarball':'https://registry.npmjs.org/" + name + "/-/" +
                   name + "-" + version + ".tgz'}");
    }

    // a version entry of a packument, its dist carrying `extra` members
    private static String version(String name, String version, String shasum, String dist, String rest) {
        return "'" + version + "':{'name':'" + name + "','version':'" + version + "','dist':{'tarball':" +
               "'https://registry.npmjs.org/" + name + "/-/" + name + "-" + version + ".tgz','shasum':'" + shasum +
               "'" + dist + "}" + rest + "}";
    }

    private static String document(String... members) {
        return ("{" + String.join(",", members) + "}").replace('\'', '"');
    }

    private static final String TRUSTED_PUBLISHER =
      ",'_npmUser':{'trustedPublisher':{'id':'gha','oidcConfigId':'cfg'}}";

    private static String provenance(String url) {
        return ",'attestations':{'provenance':{'url':'" + url + "'}}";
    }

    @Test
    void returnsUndefinedWhenNoPolicyIsActive() {
        assertThat(NpmResolutionVerifier.createNpmResolutionVerifier(makeVerifierOpts(new MockRegistry()).build()))
          .isNull();
    }

    @Test
    void flagsATrustedPublisherToProvenanceDowngrade() {
        MockRegistry registry = new MockRegistry();
        registry.documents.put("demo", document("'name':'demo'", "'dist-tags':{'latest':'0.0.2'}",
          "'versions':{" + version("demo", "0.0.1", "aa", "", TRUSTED_PUBLISHER) + "," +
          version("demo", "0.0.2", "bb", provenance("https://example.org/p"), "") + "}",
          "'time':{'0.0.1':'2025-01-01T00:00:00.000Z','0.0.2':'2025-06-01T00:00:00.000Z'}",
          "'modified':'2025-06-01T00:00:00.000Z'"));
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(
          makeVerifierOpts(registry).trustPolicy("no-downgrade").build());
        assertThat(verifier).isNotNull();
        ResolutionVerifier.Result result = verifier.verify(makeTarballResolution("demo", "0.0.2"), "demo", "0.0.2");
        assertThat(result.isOk()).isFalse();
        assertThat(result.getCode()).isEqualTo("TRUST_DOWNGRADE");
    }

    @Test
    void passesASameEvidenceLevelVersion() {
        MockRegistry registry = new MockRegistry();
        registry.documents.put("demo", document("'name':'demo'", "'dist-tags':{'latest':'0.0.2'}",
          "'versions':{" + version("demo", "0.0.1", "aa", provenance("https://example.org/p1"), "") + "," +
          version("demo", "0.0.2", "bb", provenance("https://example.org/p2"), "") + "}",
          "'time':{'0.0.1':'2025-01-01T00:00:00.000Z','0.0.2':'2025-06-01T00:00:00.000Z'}",
          "'modified':'2025-06-01T00:00:00.000Z'"));
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(
          makeVerifierOpts(registry).trustPolicy("no-downgrade").build());
        assertThat(verifier.verify(makeTarballResolution("demo", "0.0.2"), "demo", "0.0.2"))
          .isEqualTo(ResolutionVerifier.Result.OK);
    }

    @Test
    void abbreviatedShortcutRequiresThePinnedVersionToBeInMetadata() {
        MockRegistry registry = new MockRegistry();
        registry.documents.put("unpublished-pkg", document("'name':'unpublished-pkg'", "'dist-tags':{}",
          "'versions':{" + version("unpublished-pkg", "0.0.1", "aa", "", "") + "}",
          "'modified':'2010-01-01T00:00:00.000Z'"));
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(
          makeVerifierOpts(registry).minimumReleaseAge(1440.0).build());
        ResolutionVerifier.Result result = verifier.verify(makeTarballResolution("unpublished-pkg", "0.0.2"),
          "unpublished-pkg", "0.0.2");
        assertThat(result.isOk()).isFalse();
        assertThat(result.getCode()).isEqualTo("MINIMUM_RELEASE_AGE_VIOLATION");
    }

    @Test
    void ignoreMissingTimeFieldPassesTheEntryWhenNoSourceSurfacesATimestamp() {
        MockRegistry registry = new MockRegistry();
        registry.documents.put("time-free-pkg", document("'name':'time-free-pkg'", "'dist-tags':{}",
          "'versions':{" + version("time-free-pkg", "1.0.0", "aa", "", "") + "}",
          "'modified':'2010-01-01T00:00:00.000Z'"));
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(
          makeVerifierOpts(registry).minimumReleaseAge(1440.0).ignoreMissingTimeField(true).build());
        assertThat(verifier.verify(makeTarballResolution("time-free-pkg", "1.0.0"), "time-free-pkg", "1.0.0"))
          .isEqualTo(ResolutionVerifier.Result.OK);
    }

    private static String attestationResponse(String... integratedTimes) {
        StringBuilder attestations = new StringBuilder();
        for (String t : integratedTimes) {
            if (attestations.length() > 0) {
                attestations.append(',');
            }
            attestations.append("{'predicateType':'https://github.com/npm/attestation/tree/main/specs/publish/v0.1',")
              .append("'bundle':{'verificationMaterial':{'tlogEntries':[{'integratedTime':").append(t)
              .append("}]}}}");
        }
        return document("'attestations':[" + attestations + "]");
    }

    /**
     * The verdict on pnpm@11.1.1 when the attestation endpoint answers {@code body}: the package modified after the
     * cutoff, so the abbreviated shortcut does not decide, and a full document whose time is a month old.
     */
    private static ResolutionVerifier.Result verifyWithAttestation(@Nullable String body) {
        MockRegistry registry = new MockRegistry();
        registry.documents.put("pnpm", document("'name':'pnpm'", "'versions':{'11.1.1':{}}",
          "'modified':'2026-05-13T00:00:00.000Z'", "'time':{'11.1.1':'2026-04-12T00:00:00.000Z'}"));
        if (body != null) {
            registry.attestations.put("pnpm@11.1.1", body);
        }
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(makeVerifierOpts(registry)
          .now(Instant.parse("2026-05-13T00:00:00Z")).minimumReleaseAge(1440.0).build());
        return verifier.verify(makeTarballResolution("pnpm", "11.1.1"), "pnpm", "11.1.1");
    }

    private static String violation(String publishedAt) {
        return "was published at " + publishedAt + ", within the minimumReleaseAge cutoff (2026-05-12T00:00:00.000Z)";
    }

    @Test
    void returnsAnIsoTimestampBuiltFromTlogEntriesIntegratedTime() {
        assertThat(verifyWithAttestation(attestationResponse("'1778583836'")).getReason())
          .isEqualTo(violation("2026-05-12T11:03:56.000Z"));
    }

    @Test
    void fallsBackToTheFullDocumentWithoutAUsableAttestation() {
        // 404, 5xx or a failed request: no body; then a malformed body, no attestations array, no usable integratedTime
        for (String body : new String[]{null, "not json", "{}",
          document("'attestations':[{'bundle':{'verificationMaterial':{'tlogEntries':[{}]}}}]")}) {
            assertThat(verifyWithAttestation(body)).as(String.valueOf(body)).isEqualTo(ResolutionVerifier.Result.OK);
        }
    }

    @Test
    void picksTheEarliestIntegratedTimeAcrossMultipleAttestations() {
        assertThat(verifyWithAttestation(attestationResponse("'1778583836'", "'1778583833'")).getReason())
          .isEqualTo(violation("2026-05-12T11:03:53.000Z"));
    }

    @Test
    void acceptsIntegratedTimeAsANumberToo() {
        assertThat(verifyWithAttestation(attestationResponse("1778583836")).getReason())
          .isEqualTo(violation("2026-05-12T11:03:56.000Z"));
    }
}
