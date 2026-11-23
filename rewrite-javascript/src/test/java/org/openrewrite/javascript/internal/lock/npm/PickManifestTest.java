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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.V8Date;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * npm-pick-manifest 11.0.3 test/index.js.
 */
class PickManifestTest {

    private static Map<String, Object> packument(String json) {
        return NpmTreeSupport.json(json);
    }

    private static final String FOUR_VERSIONS = "{\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                "\"1.0.1\":{\"version\":\"1.0.1\"},\"1.0.2\":{\"version\":\"1.0.2\"}," +
                                                "\"2.0.0\":{\"version\":\"2.0.0\"}}}";

    private static String version(Map<String, Object> packument, String wanted) {
        return (String) PickManifest.pickManifest(packument, wanted).get("version");
    }

    private static String version(Map<String, Object> packument, String wanted, PickManifest.Options opts) {
        return (String) PickManifest.pickManifest(packument, wanted, opts).get("version");
    }

    private static String code(Throwable t) {
        return ((PickManifest.PickFailure) t).getCode();
    }

    @Test
    void basicCaratRangeSelection() {
        assertThat(version(packument(FOUR_VERSIONS), "^1.0.0")).isEqualTo("1.0.2");
    }

    @Test
    void basicTildeRangeSelection() {
        assertThat(version(packument(FOUR_VERSIONS), "~1.0.0")).isEqualTo("1.0.2");
    }

    @Test
    void basicMathematicalRangeSelection() {
        assertThat(version(packument(FOUR_VERSIONS), ">=1.0.0 <2")).isEqualTo("1.0.2");
        assertThat(version(packument(FOUR_VERSIONS), "=1.0.0")).isEqualTo("1.0.0");
    }

    @Test
    void basicVersionSelection() {
        assertThat(version(packument(FOUR_VERSIONS), "1.0.0")).isEqualTo("1.0.0");
    }

    @Test
    void basicTagSelection() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"foo\":\"1.0.1\"}," +
                                                 "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                 "\"1.0.1\":{\"version\":\"1.0.1\"}," +
                                                 "\"1.0.2\":{\"version\":\"1.0.2\"}," +
                                                 "\"2.0.0\":{\"version\":\"2.0.0\"}}}");
        assertThat(version(metadata, "foo")).isEqualTo("1.0.1");
    }

    @Test
    void errorsIfANonRegistrySpecIsProvided() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"foo\":\"1.0.1\"}," +
                                                 "\"versions\":{\"1.0.1\":{\"version\":\"1.0.1\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "!?!?!?!"))
          .hasMessageContaining("Invalid tag name");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "file://foo.tar.gz"))
          .hasMessageContaining("Only tag, version, and range are supported");
    }

    @Test
    void skipsAnyInvalidVersionKeys() {
        Map<String, Object> metadata = packument("{\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"},\"lol " +
                                                 "ok\":{\"version\":\"1.0.1\"}}}");
        assertThat(version(metadata, "^1.0.0")).isEqualTo("1.0.0");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "^1.0.1"))
          .isInstanceOf(PickManifest.PickFailure.class)
          .matches(t -> "ETARGET".equals(code(t)));
    }

    @Test
    void etargetIfRangeDoesNotMatchAnything() {
        Map<String, Object> metadata = packument("{\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                 "\"2.0.0\":{\"version\":\"2.0.0\"}," +
                                                 "\"2.0.5\":{\"version\":\"2.0.5\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "^2.1.0"))
          .matches(t -> "ETARGET".equals(code(t)));
    }

    @Test
    void e403IfVersionIsForbidden() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"latest\":\"2.1.0\"}," +
                                                 "\"policyRestrictions\":{\"versions\":{\"2.1.0\":{\"version\":" +
                                                 "\"2.1.0\"}}},\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                 "\"2.0.0\":{\"version\":\"2.0.0\"}," +
                                                 "\"2.0.5\":{\"version\":\"2.0.5\"}}}");
        assertThat(version(metadata, "2")).isEqualTo("2.0.5");
        assertThat(version(metadata, "")).isEqualTo("2.0.5");
        assertThat(version(metadata, "1 || 2")).isEqualTo("2.0.5");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "2.1.0"))
          .matches(t -> "E403".equals(code(t)));
    }

    @Test
    void e403IfVersionIsForbiddenProvidedAMinorVersion() {
        Map<String, Object> metadata = packument("{\"policyRestrictions\":{\"versions\":{\"2.1.0\":{\"version\":" +
                                                 "\"2.1.0\"},\"2.1.5\":{\"version\":\"2.1.5\"}}}," +
                                                 "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                 "\"2.0.0\":{\"version\":\"2.0.0\"}," +
                                                 "\"2.0.5\":{\"version\":\"2.0.5\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "2.1"))
          .matches(t -> "E403".equals(code(t)));
    }

    @Test
    void e403IfVersionIsForbiddenProvidedAMajorVersion() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"latest\":\"2.0.5\",\"borked\":\"2.1.5\"}," +
                                                 "\"policyRestrictions\":{\"versions\":{\"1.0.0\":{\"version\":" +
                                                 "\"1.0.0\"},\"2.1.0\":{\"version\":\"2.1.0\"}," +
                                                 "\"2.1.5\":{\"version\":\"2.1.5\"}}}," +
                                                 "\"versions\":{\"2.0.0\":{\"version\":\"2.0.0\"}," +
                                                 "\"2.0.5\":{\"version\":\"2.0.5\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "1"))
          .matches(t -> "E403".equals(code(t)));
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "borked"))
          .matches(t -> "E403".equals(code(t)));
    }

    @Test
    void ifDefaultTagMatchesAGivenRangeUseIt() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"foo\":\"1.0.1\",\"latest\":\"1.0.0\"}," +
                                                 "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                 "\"1.0.1\":{\"version\":\"1.0.1\"}," +
                                                 "\"1.0.2\":{\"version\":\"1.0.2\"}," +
                                                 "\"2.0.0\":{\"version\":\"2.0.0\"}}}");
        PickManifest.Options foo = PickManifest.Options.builder().defaultTag("foo").build();
        assertThat(version(metadata, "^1.0.0", foo)).isEqualTo("1.0.1");
        assertThat(version(metadata, "^2.0.0", foo)).isEqualTo("2.0.0");
        assertThat(version(metadata, "^1.0.0")).isEqualTo("1.0.0");
    }

    @Test
    void starRangesUseDefaultTagIfNoVersionsMatch() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"latest\":\"1.0.0-pre.0\"," +
                                                 "\"beta\":\"2.0.0-beta.0\"}," +
                                                 "\"versions\":{\"1.0.0-pre.0\":{\"version\":\"1.0.0-pre.0\"}," +
                                                 "\"1.0.0-pre.1\":{\"version\":\"1.0.0-pre.1\"}," +
                                                 "\"2.0.0-beta.0\":{\"version\":\"2.0.0-beta.0\"}," +
                                                 "\"2.0.0-beta.1\":{\"version\":\"2.0.0-beta.1\"}}}");
        PickManifest.Options beta = PickManifest.Options.builder().defaultTag("beta").build();
        assertThat(version(metadata, "*", beta)).isEqualTo("2.0.0-beta.0");
        assertThat(version(metadata, "*")).isEqualTo("1.0.0-pre.0");
        assertThat(version(metadata, "", beta)).isEqualTo("2.0.0-beta.0");
        assertThat(version(metadata, "")).isEqualTo("1.0.0-pre.0");
    }

    @Test
    void errorsIfMetadataHasNoVersions() {
        assertThatThrownBy(() -> PickManifest.pickManifest(packument("{\"versions\":{}}"), "^1.0.0"))
          .matches(t -> "ENOVERSIONS".equals(code(t)));
        assertThatThrownBy(() -> PickManifest.pickManifest(packument("{}"), "^1.0.0"))
          .matches(t -> "ENOVERSIONS".equals(code(t)));
    }

    @Test
    void errorsIfMetadataHasNoVersionsOrRestrictedVersions() {
        assertThatThrownBy(() -> PickManifest.pickManifest(
          packument("{\"versions\":{},\"policyRestrictions\":{\"versions\":{}}}"), "^1.0.0"))
          .matches(t -> "ENOVERSIONS".equals(code(t)));
        assertThatThrownBy(() -> PickManifest.pickManifest(packument("{}"), "^1.0.0"))
          .matches(t -> "ENOVERSIONS".equals(code(t)));
    }

    @Test
    void matchesEvenIfRequestedVersionHasSpaces() {
        assertThat(version(packument(FOUR_VERSIONS), "  1.0.0 ")).isEqualTo("1.0.0");
    }

    @Test
    void matchesEvenIfRequestedVersionHasGarbage() {
        assertThat(version(packument(FOUR_VERSIONS), "== 1.0.0 || foo")).isEqualTo("1.0.0");
    }

    private static final String DEPRECATED = "{\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                             "\"1.0.1\":{\"version\":\"1.0.1\"},\"1.1.0\":{\"version\":\"1.1.0\"," +
                                             "\"deprecated\":\"yes\"},\"2.0.0\":{\"version\":\"2.0.0\"}}}";

    @Test
    void matchesSkipDeprecatedVersions() {
        assertThat(version(packument(DEPRECATED), "^1.0.0")).isEqualTo("1.0.1");
    }

    @Test
    void matchesDeprecatedVersionsIfWeHaveTo() {
        assertThat(version(packument(DEPRECATED), "^1.1.0")).isEqualTo("1.1.0");
    }

    @Test
    void willUseDeprecatedVersionIfNoOtherSuitableMatch() {
        assertThat(version(packument(DEPRECATED), "^1.1.0")).isEqualTo("1.1.0");
    }

    @Test
    void acceptsOptsBeforeOptionToDoDateBasedCutoffs() {
        Map<String, Object> metadata = packument("{\"dist-tags\":{\"latest\":\"3.0.0\"}," +
                                                 "\"time\":{\"modified\":\"2018-01-03T00:00:00.000Z\"," +
                                                 "\"created\":\"2018-01-01T00:00:00.000Z\"," +
                                                 "\"1.0.0\":\"2018-01-01T00:00:00.000Z\"," +
                                                 "\"2.0.0\":\"2018-01-02T00:00:00.000Z\"," +
                                                 "\"2.0.1\":\"2018-01-03T00:00:00.000Z\"," +
                                                 "\"2.0.2\":\"2018-01-03T00:00:00.123Z\"," +
                                                 "\"3.0.0\":\"2018-01-04T00:00:00.000Z\"}," +
                                                 "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                 "\"2.0.0\":{\"version\":\"2.0.0\"}," +
                                                 "\"2.0.1\":{\"version\":\"2.0.1\"}," +
                                                 "\"3.0.0\":{\"version\":\"3.0.0\"}}}");
        PickManifest.Options before0102 = PickManifest.Options.builder().before("2018-01-02").build();
        assertThat(version(metadata, "*", before0102)).isEqualTo("2.0.0");
        assertThat(version(metadata, "latest", before0102)).isEqualTo("2.0.0");
        assertThat(version(metadata, "*", PickManifest.Options.builder()
          .before(V8Date.dateParse("2018-01-03T00:00:00.000Z")).build())).isEqualTo("2.0.1");
        assertThat(version(metadata, "*", PickManifest.Options.builder()
          .before(Instant.parse("2018-01-03T00:00:00.000Z")).build())).isEqualTo("2.0.1");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "3.0.0", before0102))
          .matches(t -> "ETARGET".equals(code(t)));
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "",
          PickManifest.Options.builder().before("1918-01-02").build()))
          .matches(t -> "ENOVERSIONS".equals(code(t)));
        assertThat(version(metadata, "^2", before0102)).isEqualTo("2.0.0");
        assertThatThrownBy(() -> PickManifest.pickManifest(metadata, "^3", before0102))
          .hasMessageContaining("with a date before");
    }

    @Test
    void prefersVersionsThatSatisfyTheEnginesRequirement() {
        Map<String, Object> pack = packument("{\"dist-tags\":{\"latest\":\"1.5.0\"},\"versions\":{" +
                                             "\"1.0.0\":{\"version\":\"1.0.0\",\"engines\":{\"node\":\">=4\"}}," +
                                             "\"1.1.0\":{\"version\":\"1.1.0\",\"engines\":{\"node\":\">=6\"}}," +
                                             "\"1.2.0\":{\"version\":\"1.2.0\",\"engines\":{\"node\":\">=8\"}}," +
                                             "\"1.3.0\":{\"version\":\"1.3.0\",\"engines\":{\"node\":\">=10\"}}," +
                                             "\"1.4.0\":{\"version\":\"1.4.0\",\"engines\":{\"node\":\">=12\"}}," +
                                             "\"1.5.0\":{\"version\":\"1.5.0\",\"engines\":{\"node\":\">=14\"}}," +
                                             "\"1.5.1\":{\"version\":\"1.5.0\",\"engines\":{\"node\":\">=14\"}}}}");
        assertThat(version(pack, "1.x", node("14.0.0"))).isEqualTo("1.5.0");
        assertThat(version(pack, "1.x", node("12.0.0"))).isEqualTo("1.4.0");
        assertThat(version(pack, "1.x", node("10.0.0"))).isEqualTo("1.3.0");
        assertThat(version(pack, "1.x", node("8.0.0"))).isEqualTo("1.2.0");
        assertThat(version(pack, "1.x", node("6.0.0"))).isEqualTo("1.1.0");
        assertThat(version(pack, "1.x", node("4.0.0"))).isEqualTo("1.0.0");
        assertThat(version(pack, "1.x", node("1.2.3"))).isEqualTo("1.5.0");
    }

    private static PickManifest.Options node(String nodeVersion) {
        return PickManifest.Options.builder().nodeVersion(nodeVersion).build();
    }

    @Test
    void supportSelectingStagedVersionsIfAllowedByOptions() {
        Map<String, Object> pack = packument("{\"dist-tags\":{\"latest\":\"1.0.0\",\"borked\":\"2.0.0\"}," +
                                             "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}}," +
                                             "\"stagedVersions\":{\"versions\":{\"2.0.0\":{\"version\":\"2.0.0\"}}}," +
                                             "\"time\":{\"1.0.0\":\"2018-01-03T00:00:00.000Z\"}}");
        PickManifest.Options staged = PickManifest.Options.builder().includeStaged(true).build();
        assertThat(version(pack, "1||2")).isEqualTo("1.0.0");
        assertThat(version(pack, "1||2", staged)).isEqualTo("1.0.0");
        assertThat(version(pack, "2", staged)).isEqualTo("2.0.0");
        assertThat(version(pack, "2", staged.toBuilder().before("2018-01-01").build())).isEqualTo("2.0.0");
        assertThatThrownBy(() -> PickManifest.pickManifest(pack, "2")).matches(t -> "ETARGET".equals(code(t)));
        assertThatThrownBy(() -> PickManifest.pickManifest(pack, "borked")).matches(t -> "ETARGET".equals(code(t)));
    }

    private static final String VULNY = "{\"name\":\"vulny\",\"dist-tags\":{\"latest\":\"1.0.3\"}," +
                                        "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                        "\"1.0.1\":{\"version\":\"1.0.1\"},\"1.0.2\":{\"version\":\"1.0.2\"}," +
                                        "\"1.0.3\":{\"version\":\"1.0.3\"},\"2.0.0\":{\"version\":\"2.0.0\"}}}";

    @Test
    void supportExcludingAvoidedVersionRanges() {
        assertThat(version(packument(VULNY), "^1.0.0", PickManifest.Options.builder().avoid(">=1.0.3").build()))
          .isEqualTo("1.0.2");
        Map<String, Object> cannotAvoid = PickManifest.pickManifest(packument(VULNY), "^1.0.0",
          PickManifest.Options.builder().avoid("1.x").build());
        assertThat(cannotAvoid).containsEntry("version", "1.0.3").containsEntry("_shouldAvoid", true);
    }

    @Test
    void supportExcludingAvoidedVersionRangesStrictly() {
        Map<String, Object> manifest = PickManifest.pickManifest(packument(VULNY), "^1.0.2",
          PickManifest.Options.builder().avoid("1.x >1.0.2").avoidStrict(true).build());
        assertThat(manifest).containsEntry("version", "1.0.2");

        Map<String, Object> breakRange = PickManifest.pickManifest(packument(VULNY), "1.0.2",
          PickManifest.Options.builder().avoid("1.x <1.0.3").avoidStrict(true).build());
        assertThat(breakRange).containsEntry("version", "1.0.3")
          .containsEntry("_outsideDependencyRange", true).containsEntry("_isSemVerMajor", false);

        Map<String, Object> majorBreak = PickManifest.pickManifest(packument(VULNY), "1.0.2",
          PickManifest.Options.builder().avoid("1.x").avoidStrict(true).build());
        assertThat(majorBreak).containsEntry("version", "2.0.0")
          .containsEntry("_outsideDependencyRange", true).containsEntry("_isSemVerMajor", true);

        assertThatThrownBy(() -> PickManifest.pickManifest(packument(VULNY), "^1.0.0",
          PickManifest.Options.builder().avoid("<3.0.0").avoidStrict(true).build()))
          .isInstanceOf(PickManifest.PickFailure.class)
          .hasMessage("No avoidable versions for vulny")
          .matches(t -> "ETARGET".equals(code(t)) && "<3.0.0".equals(((PickManifest.PickFailure) t).getAvoid()));
    }

    @Test
    void normalizePackageBins() {
        String bin = "./bin/foobar.js";
        Map<String, Object> metadata = packument("{\"name\":\"foobar\",\"versions\":{" +
                                                 "\"1.0.0\":{\"bin\":\"" + bin +
                                                   "\",\"name\":\"foobar\",\"version\":\"1.0.0\"}," +
                                                 "\"1.0.1\":{\"bin\":\"" + bin +
                                                   "\",\"name\":\"foobar\",\"version\":\"1.0.1\"}," +
                                                 "\"1.0.2\":{\"bin\":\"" + bin +
                                                   "\",\"name\":\"foobar\",\"version\":\"1.0.2\"}," +
                                                 "\"2.0.0\":{\"bin\":\"" + bin +
                                                   "\",\"name\":\"foobar\",\"version\":\"2.0.0\"}}}");
        Map<String, Object> metadataScoped = packument("{\"nameScoped\":\"@scope/foobar\",\"versions\":{" +
                                                       "\"1.0.0\":{\"bin\":\"" + bin +
                                                         "\",\"name\":\"@scope/foobar\",\"version\":\"1.0.0\"}," +
                                                       "\"1.0.1\":{\"bin\":\"" + bin +
                                                         "\",\"name\":\"@scope/foobar\",\"version\":\"1.0.1\"}," +
                                                       "\"1.0.2\":{\"bin\":\"" + bin +
                                                         "\",\"name\":\"@scope/foobar\",\"version\":\"1.0.2\"}," +
                                                       "\"2.0.0\":{\"bin\":\"" + bin +
                                                         "\",\"name\":\"@scope/foobar\",\"version\":\"2.0.0\"}}}");

        Map<String, Object> manifest = PickManifest.pickManifest(metadata, "^1.0.0");
        assertThat(manifest).isEqualTo(json("{\"name\":\"foobar\",\"version\":\"1.0.2\"," +
                                            "\"bin\":{\"foobar\":\"bin/foobar.js\"}}"));

        Map<String, Object> manifestScoped = PickManifest.pickManifest(metadataScoped, "^1.0.0");
        assertThat(manifestScoped).isEqualTo(json("{\"name\":\"@scope/foobar\",\"version\":\"1.0.2\"," +
                                                  "\"bin\":{\"foobar\":\"bin/foobar.js\"}}"));
    }

    @Test
    void noMatchingVersion() {
        assertThatThrownBy(() -> PickManifest.pickManifest(packument("{\"name\":\"package\"}"), "1.1.1"))
          .hasMessageContaining("No matching version found");
    }

    // beyond upstream's cases, what npm-pick-manifest 11.0.3 answers: a spread of an empty pick, and a nameless
    // packument
    @Test
    void avoidStrictWithNothingToPick() {
        Map<String, Object> oddly = packument("{\"name\":\"oddly\",\"dist-tags\":{\"latest\":\"1.0.2\"}," +
                                              "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                              "\"1.0.2\":{\"version\":\"2.5.0\"}}}");
        assertThat(PickManifest.pickManifest(oddly, "1.0.2",
          PickManifest.Options.builder().avoid(">=2").avoidStrict(true).build()))
          .isEqualTo(json("{\"_outsideDependencyRange\":true,\"_isSemVerMajor\":false}"));
    }

    @Test
    void namelessPackumentRendersUndefined() {
        Map<String, Object> nameless = packument("{\"dist-tags\":{\"latest\":\"1.0.0\"}," +
                                                 "\"versions\":{\"1.0.0\":{\"version\":\"1.0.0\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(nameless, "2.0.0"))
          .hasMessage("No matching version found for undefined@2.0.0.");
        assertThatThrownBy(() -> PickManifest.pickManifest(packument("{\"versions\":{}}"), null))
          .hasMessage("No versions available for undefined");
    }

    // what npm-pick-manifest 9.0.1 (npm 10.8.1) and 9.1.0 (npm 10.8.2) each answer, asked of the libraries themselves:
    // 9.0.1 takes the dist-tag's version when the range allows it, whatever else is wrong with it, and ranks the
    // others without regard to staged or restricted versions
    @Test
    void distTagVersionUncheckedBefore9_1_0() {
        PickManifest.Options old = PickManifest.Options.builder().release(PickManifest.Release.V9_0_1).build();
        Map<String, Object> deprecatedLatest = packument("{\"dist-tags\":{\"latest\":\"2.0.0\"},\"versions\":{" +
                                                         "\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                         "\"2.0.0\":{\"version\":\"2.0.0\",\"deprecated\":\"no\"}}}");
        assertThat(version(deprecatedLatest, "*", old)).isEqualTo("2.0.0");
        assertThat(version(deprecatedLatest, "*")).isEqualTo("1.0.0");

        Map<String, Object> deprecatedInRange = packument("{\"dist-tags\":{\"latest\":\"1.0.1\"},\"versions\":{" +
                                                          "\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                          "\"1.0.1\":{\"version\":\"1.0.1\",\"deprecated\":\"no\"}}}");
        assertThat(version(deprecatedInRange, "^1.0.0", old)).isEqualTo("1.0.1");
        assertThat(version(deprecatedInRange, "^1.0.0")).isEqualTo("1.0.0");

        Map<String, Object> engineLatest = packument("{\"dist-tags\":{\"latest\":\"2.0.0\"},\"versions\":{" +
                                                     "\"1.0.0\":{\"version\":\"1.0.0\"},\"2.0.0\":{" +
                                                     "\"version\":\"2.0.0\",\"engines\":{\"node\":\">=99\"}}}}");
        assertThat(version(engineLatest, "*", old.toBuilder().nodeVersion("20.0.0").build())).isEqualTo("2.0.0");
        assertThat(version(engineLatest, "*", PickManifest.Options.builder().nodeVersion("20.0.0").build()))
          .isEqualTo("1.0.0");

        Map<String, Object> restrictedLatest = packument("{\"dist-tags\":{\"latest\":\"2.0.0\"}," +
                                                         "\"policyRestrictions\":{\"versions\":{" +
                                                         "\"2.0.0\":{\"version\":\"2.0.0\"}}},\"versions\":{" +
                                                         "\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                         "\"2.0.0\":{\"version\":\"2.0.0\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(restrictedLatest, "*", old))
          .matches(t -> "E403".equals(code(t)));
        assertThat(version(restrictedLatest, "*")).isEqualTo("1.0.0");
    }

    @Test
    void stagedAndRestrictedUnrankedBefore9_1_0() {
        PickManifest.Options old = PickManifest.Options.builder().release(PickManifest.Release.V9_0_1).build();
        Map<String, Object> restricted = packument("{\"policyRestrictions\":{\"versions\":{" +
                                                   "\"1.0.1\":{\"version\":\"1.0.1\"}}},\"versions\":{" +
                                                   "\"1.0.0\":{\"version\":\"1.0.0\"}}}");
        assertThatThrownBy(() -> PickManifest.pickManifest(restricted, "^1.0.0", old))
          .matches(t -> "E403".equals(code(t)));
        assertThat(version(restricted, "^1.0.0")).isEqualTo("1.0.0");

        Map<String, Object> staged = packument("{\"stagedVersions\":{\"versions\":{" +
                                               "\"1.0.1\":{\"version\":\"1.0.1\"}}},\"versions\":{" +
                                               "\"1.0.0\":{\"version\":\"1.0.0\"}}}");
        assertThat(version(staged, "^1.0.0", old.toBuilder().includeStaged(true).build())).isEqualTo("1.0.1");
        assertThat(version(staged, "^1.0.0", PickManifest.Options.builder().includeStaged(true).build()))
          .isEqualTo("1.0.0");

        Map<String, Object> stagedLatest = packument("{\"dist-tags\":{\"latest\":\"1.0.1\"}," +
                                                     "\"stagedVersions\":{\"versions\":{" +
                                                     "\"1.0.1\":{\"version\":\"1.0.1\"}}},\"versions\":{" +
                                                     "\"1.0.0\":{\"version\":\"1.0.0\"}," +
                                                     "\"1.0.1\":{\"version\":\"1.0.1\"}}}");
        assertThat(version(stagedLatest, "^1.0.0", old.toBuilder().includeStaged(true).build())).isEqualTo("1.0.1");
        assertThat(version(stagedLatest, "^1.0.0", PickManifest.Options.builder().includeStaged(true).build()))
          .isEqualTo("1.0.0");
    }

    private static PickManifest.Options release(String npm) {
        return PickManifest.Options.builder().npm(NpmRelease.of(npm)).build();
    }

    // npm-pick-manifest as each npm bundles it with its node-semver, asked for "1.2+a - 2": 7.7.2 and the releases
    // before it read >=2.0.0 <3.0.0-0, 7.7.3 to 7.8.0 a range no version satisfies, 7.8.1 on >=1.2.0 <3.0.0-0
    @Test
    void rangeAsTheSemverOfTheReleaseReadsIt() {
        Map<String, Object> one = packument("{\"name\":\"x\",\"versions\":{\"1.5.0\":{\"version\":\"1.5.0\"}}}");
        Map<String, Object> two = packument("{\"name\":\"x\",\"versions\":{\"2.5.0\":{\"version\":\"2.5.0\"}}}");
        String range = "1.2+a - 2";
        for (String npm : new String[]{"10.0.0", "10.9.4", "11.0.0", "11.6.1"}) {
            assertThatThrownBy(() -> PickManifest.pickManifest(one, range, release(npm))).as(npm)
              .matches(t -> "ETARGET".equals(code(t)));
            assertThat(version(two, range, release(npm))).as(npm).isEqualTo("2.5.0");
        }
        for (String npm : new String[]{"10.9.5", "10.9.9", "11.6.2", "11.15.0"}) {
            assertThatThrownBy(() -> PickManifest.pickManifest(one, range, release(npm))).as(npm)
              .matches(t -> "ETARGET".equals(code(t)));
            assertThatThrownBy(() -> PickManifest.pickManifest(two, range, release(npm))).as(npm)
              .matches(t -> "ETARGET".equals(code(t)));
        }
        for (String npm : new String[]{"11.16.0", "11.17.0"}) {
            assertThat(version(one, range, release(npm))).as(npm).isEqualTo("1.5.0");
            assertThat(version(two, range, release(npm))).as(npm).isEqualTo("2.5.0");
        }
    }
}
