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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

class Pnpm10Test {

    @Test
    void supportsOnlyTheRecordedPnpm10Releases() {
        for (String version : new String[]{"10.0.0", "10.11.0", "10.12.1", "10.34.9"}) {
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
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.34.9"})
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
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.34.9"})
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
        for (String version : new String[]{"10.0.0", "10.11.0", "10.12.1", "10.34.9"}) {
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
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.34.9"})
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
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.34.9"})
    void readsNpmrcPeerSettings(String version) {
        PnpmOptions options = PnpmOptions.load(null, "auto-install-peers=false\n", null,
          obj("{'name':'test','version':'1.0.0'}"), Collections.emptyMap(), PnpmRelease.of(version));
        assertThat(options.get("autoInstallPeers")).isEqualTo(false);
        assertThat(options.get("minimumReleaseAge")).isEqualTo(0.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.34.9"})
    void readsManifestOverrides(String version) {
        PnpmOptions options = PnpmOptions.load(null, null, null,
          obj("{'name':'test','pnpm':{'overrides':{'is-number':'6.0.0'}}}"),
          Collections.emptyMap(), PnpmRelease.of(version));
        assertThat(options.getOverrides()).containsEntry("is-number", "6.0.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.34.9"})
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

    private static final String CATALOG_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      catalogs:
        default:
          cat-dep:
            specifier: ^1.0.0
            version: %1$s

      importers:

        .: {}

        a:
          dependencies:
            cat-dep:
              specifier: 'catalog:'
              version: %1$s
      %2$s
      packages:

        cat-dep@%1$s:
          resolution: {integrity: %3$s}

      snapshots:

        cat-dep@%1$s: {}
      """;

    private static final String CAT_DEP_1_0_0 =
      "sha512-kWGs5m99E2gyG9BkYB96C0+PiKPh+XZ/jmC57oZ5iK6+MsRY9w/xAIMrb85VIi+jPdKOM0V246OQSvk7oGc7hw==";

    private static final String CAT_DEP_1_1_0 =
      "sha512-NXHSXlYAXL9D644n2bkX1/rwyDr9Nou7rtXtpakJDUvAHby6NbpV3dptheWFml/+Q4j1bL/IXgo9G+9nyZwDpg==";

    private static String catalogLock(String version, String integrity, boolean b) {
        return String.format(CATALOG_LOCK, version, b ? """

            b:
              dependencies:
                cat-dep:
                  specifier: 'catalog:'
                  version: %s
          """.formatted(version) : "\n  b: {}\n", integrity);
    }

    /**
     * A new project taking a catalog entry whose locked version the registry no longer lists, as pnpm wrote it: before
     * 10.27.0 pnpm requests the locked version and finds none; from 10.27.0 it requests the catalog's range, preferring
     * the locked version, and moves every project to the range's highest.
     */
    @ParameterizedTest
    @CsvSource({"10.0.0, false", "10.11.0, false", "10.12.1, false", "10.34.9, true"})
    void aNewProjectOfACatalogEntryWhoseLockedVersionIsGone(String version, boolean moves) {
        Map<String, String> manifests = new LinkedHashMap<>();
        manifests.put("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"private\":true}");
        manifests.put("a", "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"cat-dep\":\"catalog:\"}}");
        manifests.put("b", "{\"name\":\"b\",\"version\":\"1.0.0\",\"dependencies\":{\"cat-dep\":\"catalog:\"}}");
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pnpm-workspace.yaml", "packages:\n  - a\n  - b\ncatalog:\n  cat-dep: ^1.0.0\n");
        files.put(".npmrc", "registry=http://localhost:4873/\n");
        Result result = regenerate(catalogLock("1.0.0", CAT_DEP_1_0_0, false), manifests, files, version,
          Collections.singletonMap("cat-dep", "{\"name\":\"cat-dep\",\"dist-tags\":{\"latest\":\"1.1.0\"}," +
            "\"versions\":{\"1.1.0\":{\"name\":\"cat-dep\",\"version\":\"1.1.0\",\"dist\":{\"tarball\":" +
            "\"http://localhost:4873/cat-dep/-/cat-dep-1.1.0.tgz\",\"integrity\":\"" + CAT_DEP_1_1_0 + "\"," +
            "\"shasum\":\"ebb4c7b61c5adf257c12c75b90480cce0e6c1c4e\"}}},\"time\":{\"created\":" +
            "\"2020-01-01T00:00:00.000Z\",\"modified\":\"2020-01-01T00:00:00.000Z\"," +
            "\"1.1.0\":\"2020-01-01T00:00:00.000Z\"}}"));
        if (moves) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(catalogLock("1.1.0", CAT_DEP_1_1_0, true));
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getDetail()).contains("ERR_PNPM_NO_MATCHING_VERSION",
              "No matching version found for cat-dep@1.0.0");
        }
    }

    private static final String SV_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            sv:
              specifier: %s
              version: 1.0.0

      packages:

        sv@1.0.0:
          resolution: {integrity: sha512-RYH+luspi+Yusbu/tqeNkGIQg503/YfJkXWA6wgMtosmjRU/gAYRviqsUUo6hqRhG2MIDidBdxOB3q44uluO6Q==}

      snapshots:

        sv@1.0.0: {}
      """;

    /**
     * A range with build metadata after a partial version, as pnpm read it: semver 7.7.3 (pnpm 10.29.0) strips the
     * metadata first; before, the range is invalid, and no resolver takes it.
     */
    @ParameterizedTest
    @CsvSource({"10.0.0, false", "10.11.0, false", "10.12.1, false", "10.34.9, true"})
    void aRangeWithBuildMetadata(String version, boolean resolves) {
        Result result = regenerate(SV_LOCK.formatted("1.0.0"),
          Collections.singletonMap("", "{\"name\":\"b\",\"version\":\"1.0.0\",\"dependencies\":{\"sv\":\"^1+build\"}}"),
          Collections.singletonMap(".npmrc", "registry=http://localhost:4873/\n"), version,
          Collections.singletonMap("sv", "{\"name\":\"sv\",\"dist-tags\":{\"latest\":\"1.1.0\"},\"versions\":{" +
            "\"1.0.0\":{\"name\":\"sv\",\"version\":\"1.0.0\",\"dist\":{\"tarball\":" +
            "\"http://localhost:4873/sv/-/sv-1.0.0.tgz\",\"integrity\":\"sha512-RYH+luspi+Yusbu/tqeNkGIQg503/YfJkXWA6" +
            "wgMtosmjRU/gAYRviqsUUo6hqRhG2MIDidBdxOB3q44uluO6Q==\",\"shasum\":\"918784f9deb65d4eb78bc1fed144bbaedd3320ea\"}}," +
            "\"1.1.0\":{\"name\":\"sv\",\"version\":\"1.1.0\",\"dist\":{\"tarball\":" +
            "\"http://localhost:4873/sv/-/sv-1.1.0.tgz\",\"integrity\":\"sha512-cFVuxxtP16L3on22l8evtsBX+Q5TiP7/aVNEW" +
            "5OfI/Yj1qqFCc72dCqaW7ru4EKUYnxileZPFGB+Jxp2fruOhw==\",\"shasum\":\"4ff8451bc8d619f7de1326bc556a3d1433e97af6\"}}}," +
            "\"time\":{\"created\":\"2020-01-01T00:00:00.000Z\",\"modified\":\"2020-01-01T00:00:00.000Z\"," +
            "\"1.0.0\":\"2020-01-01T00:00:00.000Z\",\"1.1.0\":\"2020-01-01T00:00:00.000Z\"}}"));
        if (resolves) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(SV_LOCK.formatted("^1+build"));
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getDetail()).contains("sv@^1+build",
              "isn't supported by any available resolver");
        }
    }

    private static Result regenerate(@Nullable String lock, Map<String, String> manifests, Map<String, String> files,
                                     String version, Map<String, String> packuments) {
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifests(manifests)
          .files(files)
          .paths(PnpmScenarios.paths(files, manifests, ""))
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(Instant.parse("2026-11-25T00:00:00Z"))
          .release(PnpmRelease.of(version))
          .build();
        return new PnpmLockRegenerator(inputs, new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean fullMetadata) {
                return packuments.get(name);
            }

            @Override
            public @Nullable String attestations(String registry, String name, String version) {
                return null;
            }

            @Override
            public @Nullable String document(String url) {
                return null;
            }
        }).regenerate();
    }
}
