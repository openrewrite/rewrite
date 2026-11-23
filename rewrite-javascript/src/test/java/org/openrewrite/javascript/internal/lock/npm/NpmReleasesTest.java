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

import java.util.ArrayList;
import java.util.List;

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

    private static String reproduces() {
        return NpmReleases.ranges(NpmReleases.proven());
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
        assertThat(NpmReleases.ranges(asList(NpmRelease.of("11.17.0"), NpmRelease.of("11.17.0"))))
          .isEqualTo("11.17.0");
        // a release the resource does not hold stands alone
        assertThat(NpmReleases.ranges(releases("6.14.17", "6.14.18", "7.0.0", "7.0.1")))
          .isEqualTo("6.14.17, 6.14.18, 7.0.0 - 7.0.1");
    }

    // the npm the parser recorded is the one that installs, and 11.17.0 where it recorded none
    @Test
    void aProjectIsInstalledAsTheReleaseThatBuiltItsLst() {
        assertThat(NpmReleases.installedAs(null)).isEqualTo(NpmRelease.PORTED);
        assertThat(NpmReleases.installedAs("11.17.0")).isEqualTo(NpmRelease.PORTED);
        for (NpmRelease proven : NpmReleases.proven()) {
            assertThat(NpmReleases.installedAs(proven.toString())).isEqualTo(proven);
        }
    }

    // a prerelease, npm 6, a release newer than the list, what is no version at all
    @ParameterizedTest
    @ValueSource(strings = {"11.17.0-pre.1", "11.17.0+build.1", "6.14.18", "99.0.0", "11.17", "latest", ""})
    void aVersionThatIsNoReleaseOfTheListIsCannotKnow(String version) {
        assertThatThrownBy(() -> NpmReleases.installedAs(version))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know what npm " + version + " writes, since the engine reproduces npm " +
                      reproduces());
    }

    @Test
    void aReleaseTheEngineDoesNotReproduceIsCannotKnow() {
        NpmRelease first = NpmReleases.proven().get(0);
        List<NpmRelease> before = NpmReleases.all().subList(0, NpmReleases.all().indexOf(first));
        assumeThat(before).isNotEmpty();
        NpmRelease unproven = before.get(before.size() - 1);

        assertThatThrownBy(() -> NpmReleases.installedAs(unproven.toString()))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know what npm " + unproven + " writes, since the engine reproduces npm " +
                      reproduces());
    }
}
