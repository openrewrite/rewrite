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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

class Pnpm10Test {

    @Test
    void supportsOnlyTheRecordedPnpm10Releases() {
        for (String version : new String[]{"10.0.0", "10.11.0", "10.34.9"}) {
            assertThat(PnpmReleases.runs(version)).hasToString(version);
        }
        for (String version : new String[]{"10.0.1", "10.11.1", "10.34.8", "^10.0.0"}) {
            assertThatThrownBy(() -> PnpmReleases.installs(version))
              .isInstanceOf(org.openrewrite.javascript.internal.lock.CannotKnow.class);
        }
    }

    @Test
    void registryAliasesAreWithinTheNativeProfile() {
        assertThat(Pnpm10Profile.unsupportedSpecifier("npm:wrap-ansi@^7.0.0")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9"})
    void keepsLockedPreferredVersionsUnweighted(String version) {
        Map<String, Map<String, PreferredVersions.Selector>> preferred =
          PreferredVersions.getPreferredVersionsFromLockfileAndManifests(
            lockfile("{'packages':{'ms@2.1.3':{'resolution':{'integrity':'sha512-a'}}}}").getPackages(),
            Collections.emptyList(), PnpmRelease.of(version));
        assertThat(preferred.get("ms").get("2.1.3").getWeight()).isNull();
        assertThat(ReadPackageHook.compatPackageExtensions(PnpmRelease.of(version)))
          .doesNotContainKey("notistack@^3.0.0");
        assertThat(ReadPackageHook.compatPackageExtensions(PnpmRelease.PORTED))
          .containsKey("notistack@^3.0.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9"})
    void refusesUnportedPatchAndRuntimePaths(String version) {
        PnpmScenarios.Scenario scenario = PnpmScenarios.discover().stream()
          .filter(s -> s.getName().equals("pnpm" + version.replace(".", "") + "-upgrade"))
          .findFirst().orElseThrow(AssertionError::new);
        for (String manifest : new String[]{
          "{\"name\":\"test\",\"pnpm\":{\"patchedDependencies\":{\"ms\":\"patches/ms.patch\"}}}",
          "{\"name\":\"test\",\"devEngines\":{\"runtime\":{\"name\":\"node\",\"version\":\"22\"}}}"
        }) {
            PnpmLockRegenerator.Inputs inputs = PnpmScenarioTest.inputs(scenario, scenario.file("before"),
              Collections.emptyMap(), version).toBuilder().manifests(Collections.singletonMap("", manifest)).build();
            Result result = new PnpmLockRegenerator(inputs, new ScenarioRegistry(scenario, "after")).regenerate();
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
            assertThat(result.getFailure().getDetail()).contains("pnpm 10 native profile");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner/repo", "owner/repo#main", "./pkg", "../pkg", "/tmp/pkg"})
    void refusesNewNonRegistryDependenciesBeforeResolution(String spec) {
        for (String version : new String[]{"10.0.0", "10.11.0", "10.34.9"}) {
            PnpmScenarios.Scenario scenario = PnpmScenarios.discover().stream()
              .filter(s -> s.getName().equals("pnpm" + version.replace(".", "") + "-upgrade"))
              .findFirst().orElseThrow(AssertionError::new);
            String manifest = "{\"name\":\"test\",\"dependencies\":{\"new-dependency\":\"" + spec + "\"}}";
            PnpmLockRegenerator.Inputs inputs = PnpmScenarioTest.inputs(scenario, scenario.file("before"),
              Collections.emptyMap(), version).toBuilder().manifests(Collections.singletonMap("", manifest)).build();
            Result result = new PnpmLockRegenerator(inputs, new ScenarioRegistry(scenario, "after")).regenerate();
            assertThat(result.getFailure()).as(version + " " + spec).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
            assertThat(result.getFailure().getDetail()).as(version + " " + spec)
              .contains("pnpm 10 native profile", spec);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9"})
    void keepsSupportedDependencyFormsWithinTheProfile(String version) {
        PnpmScenarios.Scenario scenario = PnpmScenarios.discover().stream()
          .filter(s -> s.getName().equals("pnpm" + version.replace(".", "") + "-upgrade"))
          .findFirst().orElseThrow(AssertionError::new);
        for (String spec : new String[]{"latest", "next", "1.0.0", "^1.0.0", ">=1 <3", "*",
          "npm:ms@^2", "npm:@scope/package@^1", "workspace:*", "workspace:../pkg", "catalog:",
          "catalog:default", "link:./pkg"}) {
            String manifest = "{\"name\":\"test\",\"dependencies\":{\"dependency\":\"" + spec + "\"}}";
            PnpmLockRegenerator.Inputs inputs = PnpmScenarioTest.inputs(scenario, scenario.file("before"),
              Collections.emptyMap(), version).toBuilder().manifests(Collections.singletonMap("", manifest)).build();
            assertThat(Pnpm10Profile.unsupported(inputs, PnpmRelease.of(version)))
              .as(version + " " + spec).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9"})
    void readsNpmrcPeerSettings(String version) {
        PnpmOptions options = PnpmOptions.load(null, "auto-install-peers=false\n", null,
          obj("{'name':'test','version':'1.0.0'}"), Collections.emptyMap(), PnpmRelease.of(version));
        assertThat(options.get("autoInstallPeers")).isEqualTo(false);
        assertThat(options.get("minimumReleaseAge")).isEqualTo(0.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9"})
    void readsManifestOverrides(String version) {
        PnpmOptions options = PnpmOptions.load(null, null, null,
          obj("{'name':'test','pnpm':{'overrides':{'is-number':'6.0.0'}}}"),
          Collections.emptyMap(), PnpmRelease.of(version));
        assertThat(options.getOverrides()).containsEntry("is-number", "6.0.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9"})
    void reproducesNativeRecordings(String version) {
        String prefix = "pnpm" + version.replace(".", "") + "-";
        int count = 0;
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            if (!scenario.getName().startsWith(prefix)) {
                continue;
            }
            count++;
            ScenarioRegistry registry = new ScenarioRegistry(scenario, "after");
            Result result = new PnpmLockRegenerator(PnpmScenarioTest.inputs(scenario, scenario.file("before"),
              scenario.otherLocks(org.openrewrite.javascript.internal.lock.LockScenario.Phase.BEFORE), version),
              registry).regenerate();
            assertThat(result.isSuccess()).as(scenario.getName() + ": " + result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).as(scenario.getName()).isEqualTo(scenario.file("after"));
        }
        assertThat(count).isGreaterThanOrEqualTo(8);
    }
}
