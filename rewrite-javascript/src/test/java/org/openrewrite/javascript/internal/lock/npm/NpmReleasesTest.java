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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

class NpmReleasesTest {

    private static List<NpmRelease> releases(String... versions) {
        List<NpmRelease> releases = new ArrayList<>();
        for (String version : versions) {
            releases.add(NpmRelease.of(version));
        }
        return releases;
    }

    @Test
    void everyStableReleaseFromTheFirstWithArboristInOrder() {
        List<NpmRelease> all = NpmReleases.all();

        assertThat(all).isSorted().doesNotHaveDuplicates();
        assertThat(all.get(0)).hasToString("7.0.0");
        // the releases either side of a major, and the last patch of a line kept up beside the next major
        assertThat(all).containsSubsequence(releases("7.24.2", "8.0.0", "8.19.4", "9.0.0", "9.9.4", "10.0.0",
          "10.9.9", "11.0.0", "11.17.0", "12.0.0"));
    }

    @Test
    void theEngineReproducesTheReleaseItPorts() {
        assertThat(NpmReleases.isProven(NpmRelease.PORTED)).isTrue();
        assertThat(NpmReleases.proven()).contains(NpmRelease.PORTED).isSubsetOf(NpmReleases.all()).isSorted();
        assertThat(NpmReleases.isProven(NpmRelease.of("6.14.18"))).isFalse();
    }

    @Test
    void releasesAreNamedByTheRunsTheyFill() {
        assertThat(NpmReleases.ranges(emptyList())).isEmpty();
        assertThat(NpmReleases.ranges(releases("11.17.0"))).isEqualTo("11.17.0");
        // 10.9.9 is followed by 11.0.0, and 10.8.3 by 10.9.0
        assertThat(NpmReleases.ranges(releases("11.1.0", "10.9.9", "11.0.0", "10.9.8"))).isEqualTo("10.9.8 - 11.1.0");
        assertThat(NpmReleases.ranges(releases("12.2.0", "10.8.2", "10.8.3", "10.9.1", "12.1.0", "12.0.2")))
          .isEqualTo("10.8.2 - 10.8.3, 10.9.1, 12.0.2 - 12.2.0");
        assertThat(NpmReleases.ranges(asList(NpmRelease.of("11.17.0"), NpmRelease.of("11.17.0").followed())))
          .isEqualTo("11.17.0");
        // a release the resource does not hold stands alone
        assertThat(NpmReleases.ranges(releases("6.14.17", "6.14.18", "7.0.0", "7.0.1")))
          .isEqualTo("6.14.17, 6.14.18, 7.0.0 - 7.0.1");
    }

    private static Map<String, Object> pkg(String json) {
        return NpmTreeSupport.json(json);
    }

    private static Map<String, Object> devEnginesPackageManager(String packageManager) {
        return pkg("{\"name\":\"p\",\"devEngines\":{\"packageManager\":" + packageManager + "}}");
    }

    private static String allowed(Map<String, Object> pkg) {
        List<NpmRelease> allowed = NpmReleases.allowed(pkg);
        return allowed == null ? "every release" : NpmReleases.ranges(allowed);
    }

    // the engine's releases a range holds, newest first, by the test's own reading of the range
    private static List<NpmRelease> reproduced(String range) {
        List<NpmRelease> reproduced = new ArrayList<>();
        for (NpmRelease release : NpmReleases.proven()) {
            if (NodeSemver.satisfies(release.toString(), range, NodeSemver.Options.DEFAULT)) {
                reproduced.add(0, release);
            }
        }
        return reproduced;
    }

    private static String reproduces() {
        return NpmReleases.ranges(NpmReleases.proven());
    }

    @Test
    void aProjectThatNamesNoReleaseIsInstalledAsEveryReleaseTheEngineReproduces() {
        assertThat(NpmReleases.undeclared()).isEqualTo(reproduced("*")).contains(NpmRelease.PORTED)
          .hasSameSizeAs(NpmReleases.proven());

        for (String manifest : new String[]{"{}", "{\"name\":\"p\",\"version\":\"1.0.0\"}",
          "{\"devEngines\":{\"runtime\":{\"name\":\"node\",\"version\":\">=20\"}}}", "{\"engines\":{\"npm\":\"^10\"}}"}) {
            assertThat(NpmReleases.allowed(pkg(manifest))).as(manifest).isNull();
            assertThat(NpmReleases.candidates(pkg(manifest))).as(manifest).isEqualTo(NpmReleases.undeclared());
        }
    }

    // corepack's parseSpec with a version enforced; a hash follows the version as build metadata
    @Test
    void packageManagerNamingAnNpmVersionAllowsThatReleaseAlone() {
        assertThat(allowed(pkg("{\"packageManager\":\"npm@10.9.2\"}"))).isEqualTo("10.9.2");
        assertThat(allowed(pkg("{\"packageManager\":\"npm@10.9.2+sha512.3a0b1e7c\"}"))).isEqualTo("10.9.2");
        // npm does not hold the field to devEngines: corepack does, and npm 10.9.2 reports what it finds
        assertThat(allowed(pkg("{\"packageManager\":\"npm@10.9.2\",\"devEngines\":{\"packageManager\":" +
                               "{\"name\":\"npm\",\"version\":\"^11\"}}}"))).isEqualTo("10.9.2");

        assertThat(NpmReleases.candidates(pkg("{\"packageManager\":\"npm@" + NpmRelease.PORTED + "\"}")))
          .containsExactly(NpmRelease.PORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"pnpm@9.15.0\"", "\"yarn@4.10.3+sha512.3a0b1e7c\"", "\"bun@1.3.10\"", "\"npm@^11\"",
      "\"npm@11\"", "\"npm@latest\"", "\"npm\"", "\"npm@\"",
      "\"npm@https://registry.npmjs.org/npm/-/npm-11.17.0.tgz\"", "11", "null", "{\"name\":\"npm\"}"})
    void packageManagerThatNamesNoNpmVersionAllowsEveryRelease(String packageManager) {
        assertThat(allowed(pkg("{\"packageManager\":" + packageManager + "}"))).isEqualTo("every release");
        // and leaves devEngines to say
        assertThat(allowed(pkg("{\"packageManager\":" + packageManager + ",\"devEngines\":{\"packageManager\":" +
                               "{\"name\":\"npm\",\"version\":\"10.9.2\"}}}"))).isEqualTo("10.9.2");
    }

    @Test
    void devEnginesPackageManagerAllowsTheReleasesItsCheckPasses() {
        String newest = NpmReleases.all().get(NpmReleases.all().size() - 1).toString();

        assertThat(allowed(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"^10\"}")))
          .isEqualTo("10.0.0 - 10.9.9");
        assertThat(allowed(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"^10\",\"onFail\":\"error\"}")))
          .isEqualTo("10.0.0 - 10.9.9");
        assertThat(allowed(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"^10\",\"onFail\":\"download\"}")))
          .isEqualTo("10.0.0 - 10.9.9");
        assertThat(allowed(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"11.17.0\"}")))
          .isEqualTo("11.17.0");
        // the project says which npm it uses, also where that npm is too old to check
        assertThat(allowed(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"<10.9\"}")))
          .isEqualTo("7.0.0 - 10.8.3");
        // one alternative that matches is enough
        assertThat(allowed(devEnginesPackageManager("[{\"name\":\"pnpm\",\"version\":\"^9\"}," +
                                                    "{\"name\":\"npm\",\"version\":\">=11.17.0\"}]")))
          .isEqualTo("11.17.0 - " + newest);
        assertThat(allowed(devEnginesPackageManager("[{\"name\":\"npm\",\"version\":\"^9\",\"onFail\":\"ignore\"}," +
                                                    "{\"name\":\"npm\",\"version\":\"11.17.0\"}]")))
          .isEqualTo("9.0.0 - 9.9.4, 11.17.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {
      // any npm
      "{\"name\":\"npm\"}",
      // a mismatch npm only warns of or ignores, by the last alternative's onFail
      "{\"name\":\"npm\",\"version\":\"^10\",\"onFail\":\"warn\"}",
      "{\"name\":\"npm\",\"version\":\"^10\",\"onFail\":\"ignore\"}",
      "[{\"name\":\"npm\",\"version\":\"^10\"},{\"name\":\"pnpm\",\"onFail\":\"warn\"}]",
      // no release passes: another package manager, a range none satisfies, a version that is no range
      "{\"name\":\"pnpm\"}",
      "{\"name\":\"pnpm\",\"version\":\"^9\",\"onFail\":\"download\"}",
      "{\"name\":\"npm\",\"version\":\">=99\"}",
      "{\"name\":\"npm\",\"version\":\"latest\"}",
      // shapes npm fails on
      "\"npm@11\"", "{\"name\":\"npm\",\"version\":11}", "{\"version\":\"^10\"}",
      "{\"name\":\"npm\",\"version\":\"^10\",\"onFail\":\"never\"}", "null",
      // no alternative at all
      "[]"})
    void devEnginesPackageManagerThatSettlesNoReleaseAllowsEveryRelease(String packageManager) {
        assertThat(allowed(devEnginesPackageManager(packageManager))).isEqualTo("every release");
    }

    @Test
    void devEnginesThatIsNoObjectAllowsEveryRelease() {
        assertThat(allowed(pkg("{\"devEngines\":\"npm@^10\"}"))).isEqualTo("every release");
        assertThat(allowed(pkg("{\"devEngines\":null}"))).isEqualTo("every release");
        assertThat(allowed(pkg("{\"devEngines\":[{\"packageManager\":{\"name\":\"npm\",\"version\":\"^10\"}}]}")))
          .isEqualTo("every release");
    }

    @Test
    void theCandidatesAreTheAllowedReleasesTheEngineReproducesNewestFirst() {
        assertThat(NpmReleases.candidates(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\">=10.9.0\"}")))
          .isEqualTo(reproduced(">=10.9.0")).contains(NpmRelease.PORTED);
        assertThat(NpmReleases.candidates(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"" +
                                                                   NpmRelease.PORTED + "\"}")))
          .containsExactly(NpmRelease.PORTED);
    }

    @Test
    void releasesTheEngineDoesNotReproduceAreCannotKnow() {
        // an npm that is no release the engine knows of
        assertThatThrownBy(() -> NpmReleases.candidates(pkg("{\"packageManager\":\"npm@6.14.18\"}")))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know what npm 6.14.18 writes, since the engine reproduces npm " +
                      reproduces());
        assertThatThrownBy(() -> NpmReleases.candidates(pkg("{\"packageManager\":\"npm@11.17.0-pre.1\"}")))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know what npm 11.17.0-pre.1 writes, since the engine reproduces npm " +
                      reproduces());

        // the releases before the first it reproduces
        NpmRelease first = NpmReleases.proven().get(0);
        List<NpmRelease> before = NpmReleases.all().subList(0, NpmReleases.all().indexOf(first));
        assumeThat(before).isNotEmpty();
        NpmRelease unproven = before.get(before.size() - 1);
        assertThatThrownBy(() -> NpmReleases.candidates(pkg("{\"packageManager\":\"npm@" + unproven + "\"}")))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know what npm " + unproven + " writes, since the engine reproduces npm " +
                      reproduces());
        assertThatThrownBy(() -> NpmReleases.candidates(devEnginesPackageManager("{\"name\":\"npm\",\"version\":\"<" +
                                                                                 first + "\"}")))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know what npm " + NpmReleases.ranges(before) + " writes, since the engine " +
                      "reproduces npm " + reproduces());
    }

    // what an install's stand-in is held to beside its thresholds: the releases npm's check lets install the project
    @Test
    void devEnginesRefuseTheReleasesTheirCheckFailsWithAnError() {
        Object devEngines = pkg("{\"packageManager\":{\"name\":\"npm\",\"version\":\"^11.18.0\"}}");

        assertThat(NpmReleases.devEnginesRefuse(devEngines, NpmRelease.of("11.17.0"))).isTrue();
        assertThat(NpmReleases.devEnginesRefuse(devEngines, NpmRelease.of("11.18.0").followed())).isFalse();
        assertThat(NpmReleases.devEnginesRefuse(devEngines, NpmRelease.of("12.0.0"))).isTrue();
        assertThat(NpmReleases.devEnginesRefuse(pkg("{\"packageManager\":{\"name\":\"npm\",\"version\":\"^11.18.0\"," +
                                                    "\"onFail\":\"warn\"}}"), NpmRelease.of("11.17.0"))).isFalse();
        assertThat(NpmReleases.devEnginesRefuse(pkg("{\"runtime\":{\"name\":\"bun\"}}"), NpmRelease.of("11.17.0")))
          .isFalse();
        assertThat(NpmReleases.devEnginesRefuse(null, NpmRelease.of("11.17.0"))).isFalse();
    }
}
