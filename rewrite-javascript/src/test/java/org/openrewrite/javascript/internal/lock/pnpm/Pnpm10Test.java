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

    private static final String NPMRC = "registry=http://localhost:4873/\n";

    private static String packument(String name, String integrity, String shasum) {
        return "{\"name\":\"" + name + "\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"" +
               name + "\",\"version\":\"1.0.0\",\"dist\":{\"tarball\":\"http://localhost:4873/" + name + "/-/" +
               name + "-1.0.0.tgz\",\"integrity\":\"" + integrity + "\",\"shasum\":\"" + shasum + "\"}}}," +
               "\"time\":{\"created\":\"2020-01-01T00:00:00.000Z\",\"modified\":\"2020-01-01T00:00:00.000Z\"," +
               "\"1.0.0\":\"2020-01-01T00:00:00.000Z\"}}";
    }

    private static final String DEP_INTEGRITY =
      "sha512-hAyBwXfGUDrvY3rJOGAWn411jy6sX1ifUlEmMwhBYM7pFVbp0e3m1AMrWN2NnKt3aB8mpgjDAwTibf+fXhI3/w==";

    private static final String OTHER_INTEGRITY =
      "sha512-tBV0kA2uSDkrOcxQgrUQdqYoRrx0uHovDsAYo48MnidKvjG1sI9z4v2to1h9JHumBWFey4OLY8xQcxzLib2Bjw==";

    private static final Map<String, String> DEP_AND_OTHER = Map.of(
      "dep", packument("dep", DEP_INTEGRITY, "832727229312793eba0436b0c08b08ee30a71f7b"),
      "other", packument("other", OTHER_INTEGRITY, "f41d4acc285c465486f582b03e45bfe22cdbae20"));

    private static final String ALIAS_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            %s:
              specifier: npm:dep@1.0.0
              version: dep@1.0.0
            dep:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        dep@1.0.0:
          resolution: {integrity: %s}

      snapshots:

        dep@1.0.0: {}
      """;

    /**
     * A project's dependency alias that is no package name, added beside a locked one, as pnpm read it: from 10.34.0 (pnpm/pnpm 455cfcd84, the
     * backport of 11.4.0's check) {@code _x} is {@code ERR_PNPM_INVALID_DEPENDENCY_NAME}; before, it is resolved.
     * pnpm 10 checks with validate-npm-package-name 5.0.0, which takes {@code -x} for a name.
     */
    @ParameterizedTest
    @CsvSource({"10.12.1, _x, true", "10.33.4, _x, true", "10.34.0, _x, false", "10.34.9, _x, false",
      "10.33.4, -x, true", "10.34.0, -x, true", "10.34.9, -x, true"})
    void aDependencyAliasThatIsNoPackageName(String version, String alias, boolean resolves) {
        Result result = regenerate(DEP_LOCK.formatted("{integrity: " + DEP_INTEGRITY + "}"),
          Collections.singletonMap("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"dep\":\"1.0.0\",\"" +
                                       alias + "\":\"npm:dep@1.0.0\"}}"),
          Collections.singletonMap(".npmrc", NPMRC), version,
          DEP_AND_OTHER);
        if (resolves) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(ALIAS_LOCK.formatted(
              alias.startsWith("-") ? "'" + alias + "'" : alias, DEP_INTEGRITY));
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getDetail()).contains("ERR_PNPM_INVALID_DEPENDENCY_NAME",
              "The current package contains a dependency with an invalid name: \"" + alias + "\"");
        }
    }

    private static final String DEP_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            dep:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        dep@1.0.0:
          resolution: %s

      snapshots:

        dep@1.0.0: {}
      """;

    private static final String DEP_AND_OTHER_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            dep:
              specifier: 1.0.0
              version: 1.0.0
            other:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        dep@1.0.0:
          resolution: {integrity: %s}

        other@1.0.0:
          resolution: {integrity: %s}

      snapshots:

        dep@1.0.0: {}

        other@1.0.0: {}
      """.formatted(DEP_INTEGRITY, OTHER_INTEGRITY);

    private static Result addOther(String lockedResolution, String version) {
        return regenerate(DEP_LOCK.formatted(lockedResolution),
          Collections.singletonMap("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"dep\":\"1.0.0\"," +
                                       "\"other\":\"1.0.0\"}}"),
          Collections.singletonMap(".npmrc", NPMRC), version, DEP_AND_OTHER);
    }

    /**
     * A registry package locked without an integrity, as pnpm installed a new dependency beside it: from 10.34.1
     * (pnpm/pnpm b8196b82d, the backport of 11.4.0's check) it is {@code ERR_PNPM_MISSING_TARBALL_INTEGRITY}; before,
     * it is resolved again.
     */
    @ParameterizedTest
    @CsvSource({"10.12.1, true", "10.34.0, true", "10.34.1, false", "10.34.9, false"})
    void aLockedRegistryTarballWithoutAnIntegrity(String version, boolean resolves) {
        Result result = addOther("{tarball: http://localhost:4873/dep/-/dep-1.0.0.tgz}", version);
        if (resolves) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(DEP_AND_OTHER_LOCK);
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getDetail()).contains("ERR_PNPM_MISSING_TARBALL_INTEGRITY",
              "Cannot install package \"dep@1.0.0\": its lockfile entry has no \"integrity\" field, so pnpm " +
              "cannot verify the downloaded tarball.");
        }
    }

    /**
     * A registry dep path locked with a tarball no registry serves, as pnpm installed a new dependency beside it: from
     * 10.34.2 (pnpm/pnpm 14bceb1e0, the backport of 11.5.3's check, which pkgSnapshotToResolution makes in pnpm 10)
     * it is {@code ERR_PNPM_RESOLUTION_SHAPE_MISMATCH}, a git host matched in any case; before, it is resolved again.
     */
    @ParameterizedTest
    @CsvSource({
      "10.12.1, https://CODELOAD.GITHUB.COM/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, true",
      "10.34.1, https://CODELOAD.GITHUB.COM/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, true",
      "10.34.2, https://CODELOAD.GITHUB.COM/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, false",
      "10.34.9, https://CODELOAD.GITHUB.COM/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, false",
      "10.34.1, https://codeload.github.com/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, true",
      "10.34.2, https://codeload.github.com/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, false",
      "10.34.9, https://codeload.github.com/o/r/tar.gz/0123456789abcdef0123456789abcdef01234567, false",
      "10.34.1, //evil.example.com/dep.tgz, true",
      "10.34.2, //evil.example.com/dep.tgz, false",
      "10.34.9, //evil.example.com/dep.tgz, false"})
    void aRegistryDepPathLockedWithANonRegistryTarball(String version, String tarball, boolean resolves) {
        Result result = addOther("{integrity: " + DEP_INTEGRITY + ", tarball: '" + tarball + "'}", version);
        if (resolves) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(DEP_AND_OTHER_LOCK);
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getDetail()).contains("ERR_PNPM_RESOLUTION_SHAPE_MISMATCH",
              "Cannot use the lockfile entry of \"dep@1.0.0\": its registry-style dependency path is backed by a " +
              "non-registry resolution.");
        }
    }

    private static final String CODELOAD =
      "https://codeload.github.com/juliangruber/isarray/tar.gz/8c8dd70d48ad5d0897733ca6726302a54ca9e177";

    private static final String CODELOAD_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            gh:
              specifier: 1.0.0
              version: 1.0.0
            other:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        gh@1.0.0:
          resolution: {%sintegrity: sha512-AdqQ9oCmE2zQX5USbkguKC934IQDugKRKBqb1YY+vZ+jzVcwMmicxtrlIW6XulwlkeW2zdqcCCG9HD4okAW63A==, tarball: %s}

        other@1.0.0:
          resolution: {integrity: sha512-B5T0c5qMU4jr7vSqdzBzA7kl08kqg3zcGy7CWODLqYz7n+VXkvT8vGLGqrlEW8oKm9jLQj3wHbyJK19YUpZflw==}

      snapshots:

        gh@1.0.0: {}

        other@1.0.0: {}
      """;

    private static final String OTHER_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            other:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        other@1.0.0:
          resolution: {integrity: sha512-B5T0c5qMU4jr7vSqdzBzA7kl08kqg3zcGy7CWODLqYz7n+VXkvT8vGLGqrlEW8oKm9jLQj3wHbyJK19YUpZflw==}

      snapshots:

        other@1.0.0: {}
      """;

    /**
     * A registry version whose tarball is a git host's, added beside a locked package, as pnpm wrote it: from 10.33.4
     * (pnpm/pnpm#11491, the backport of 11.0.7's change) its resolution is marked {@code gitHosted}; before, it is a
     * tarball URL the registry does not give, kept as it is.
     */
    @ParameterizedTest
    @CsvSource({"10.12.1, false", "10.33.3, false", "10.33.4, true", "10.34.1, true", "10.34.2, true",
      "10.34.9, true"})
    void aRegistryVersionServedFromAGitHost(String version, boolean marked) {
        Result result = regenerate(OTHER_LOCK,
          Collections.singletonMap("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"gh\":\"1.0.0\"," +
                                       "\"other\":\"1.0.0\"}}"),
          Collections.singletonMap(".npmrc", NPMRC), version, Map.of(
            "gh", "{\"name\":\"gh\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"gh\"," +
                  "\"version\":\"1.0.0\",\"dist\":{\"tarball\":\"" + CODELOAD + "\",\"integrity\":\"sha512-AdqQ9oCm" +
                  "E2zQX5USbkguKC934IQDugKRKBqb1YY+vZ+jzVcwMmicxtrlIW6XulwlkeW2zdqcCCG9HD4okAW63A==\",\"shasum\":" +
                  "\"2e83c98da40336402045e4060ef4b126baf4bb9e\"}}},\"time\":{\"created\":\"2020-01-01T00:00:00.000Z\"," +
                  "\"modified\":\"2020-01-01T00:00:00.000Z\",\"1.0.0\":\"2020-01-01T00:00:00.000Z\"}}",
            "other", packument("other", "sha512-B5T0c5qMU4jr7vSqdzBzA7kl08kqg3zcGy7CWODLqYz7n+VXkvT8vGLGqrlEW8oKm9jLQj3wHbyJ" +
                                        "K19YUpZflw==", "23ac8410a43fd0899f90ec5185c937a064b766bf")));
        assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(CODELOAD_LOCK.formatted(marked ? "gitHosted: true, " : "",
          CODELOAD));
    }

    private static final String ALIASED_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            al:
              specifier: npm:%1$s@1.0.0
              version: '%1$s@1.0.0'
            other:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        '%1$s@1.0.0':
          resolution: {%2$s}

        other@1.0.0:
          resolution: {integrity: sha512-1WjJq5GqOxeNBDV2ikO07Gtgaw8VYF0sHJH/pBiH6yo4uFmbenvvmQUMSsXUgbJAFDiqpr9sIM/5rNY88HQ1oQ==}

      snapshots:

        '%1$s@1.0.0': {}

        other@1.0.0: {}
      """;

    private static final String OTHER_INTEGRITY_8 =
      "sha512-1WjJq5GqOxeNBDV2ikO07Gtgaw8VYF0sHJH/pBiH6yo4uFmbenvvmQUMSsXUgbJAFDiqpr9sIM/5rNY88HQ1oQ==";

    /**
     * An npm alias of a package named {@code -z}, added beside a locked package, as pnpm wrote it: from 10.34.5
     * (pnpm/pnpm#12890, the backport of 11.11.0's change) resolvePeers places a package only under a name
     * validate-npm-package-name takes, which pnpm 10's 5.0.0 does, unlike pnpm 11's 7.0.2.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.34.4", "10.34.5", "10.34.9"})
    void anAliasOfAPackageNamedWithALeadingHyphen(String version) {
        Result result = regenerate(OTHER_LOCK.replace(
            "sha512-B5T0c5qMU4jr7vSqdzBzA7kl08kqg3zcGy7CWODLqYz7n+VXkvT8vGLGqrlEW8oKm9jLQj3wHbyJK19YUpZflw==",
            OTHER_INTEGRITY_8),
          Collections.singletonMap("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"al\":" +
                                       "\"npm:-z@1.0.0\",\"other\":\"1.0.0\"}}"),
          Collections.singletonMap(".npmrc", NPMRC), version, Map.of(
            "-z", packument("-z", "sha512-jKBjpmZqvXcLROitNXVd7DGD5QwHLYxGI/DOozp7ci5NJaN1viNp55E4JM1sTrA2nFinf0Ek95723tn6" +
                                  "+BoJCQ==", "c242c03be9ff45305dc761a364338120f7192007"),
            "other", packument("other", OTHER_INTEGRITY_8, "415f98b4ddae23eb30fc22fc9849264d14b07f39")));
        assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(ALIASED_LOCK.formatted("-z", "integrity: " +
          "sha512-jKBjpmZqvXcLROitNXVd7DGD5QwHLYxGI/DOozp7ci5NJaN1viNp55E4JM1sTrA2nFinf0Ek95723tn6+BoJCQ=="));
    }

    private static final String RELEASE_AGE_LOCK = """
      lockfileVersion: '9.0'

      settings:
        autoInstallPeers: true
        excludeLinksFromLockfile: false

      importers:

        .:
          dependencies:
            dep:
              specifier: ^1.0.0
              version: %1$s
            other:
              specifier: 1.0.0
              version: 1.0.0

      packages:

        dep@%1$s:
          resolution: {integrity: %2$s}

        other@1.0.0:
          resolution: {integrity: sha512-T+IZa8fsNV04GfEvW4q7Hpp3OgkztzM+8J2cSU6kfhvsFOaENlLtH0f5Zh8/ay1cDg9g3+0HA0eRMsFXrF8deQ==}

      snapshots:

        dep@%1$s: {}

        other@1.0.0: {}
      """;

    private static final String RELEASE_AGE_DEP_VERSIONS = "\"versions\":{\"1.0.0\":{\"name\":\"dep\",\"version\":" +
      "\"1.0.0\",\"dist\":{\"tarball\":\"http://localhost:4873/dep/-/dep-1.0.0.tgz\",\"integrity\":\"sha512-DHXEiK7g17yW" +
      "T6SWglM2dM3+HU+DGCzHL2V0nbSzk4W7usDHxHQBFdkzzqyfKpp8k5w6zJVB1DZ4cPbSp8jcNw==\",\"shasum\":" +
      "\"c8fecbd14a4e7dfe7cf429d1fec0142c21d97325\"}},\"1.1.0\":{\"name\":\"dep\",\"version\":\"1.1.0\",\"dist\":" +
      "{\"tarball\":\"http://localhost:4873/dep/-/dep-1.1.0.tgz\",\"integrity\":\"sha512-ZeJMvVBT7q82GsgnBrNyugpLq5D3" +
      "L2V66yPFftLLxaRIJVIvKz4GYmLi2h0vIwVc3PaAvYK/7m3QvUhHRE6siQ==\",\"shasum\":" +
      "\"5f931eb51238f55847e8e0b2cfd0905eda2fb6ff\"}}}";

    /**
     * A registry that gives an abbreviated packument without a time field (its modified before the cutoff) where
     * registry-supports-time-field says it has one, under minimum-release-age, as pnpm resolved a new dependency: from
     * 10.34.0 (pnpm/pnpm#11630, the backport of 11.1.2's change) pnpm fetches the full packument, whose time makes 1.1.0
     * immature, whatever the modified; before, the picker meets no time ({@code ERR_PNPM_MISSING_TIME}). pnpm 10.12.1
     * has no minimum-release-age, and picks 1.1.0.
     */
    @ParameterizedTest
    @CsvSource({"10.12.1, 1.1.0", "10.33.4, ", "10.34.0, 1.0.0", "10.34.9, 1.0.0"})
    void anAbbreviatedPackumentWithoutTimeUnderAReleaseAge(String version, @Nullable String picked) {
        String full = "{\"name\":\"dep\",\"dist-tags\":{\"latest\":\"1.1.0\"}," + RELEASE_AGE_DEP_VERSIONS +
                      ",\"time\":{\"created\":\"2020-01-01T00:00:00.000Z\",\"modified\":\"2020-01-01T00:00:00.000Z\"," +
                      "\"1.0.0\":\"2020-01-01T00:00:00.000Z\",\"1.1.0\":\"2099-01-01T00:00:00.000Z\"}}";
        String abbreviated = "{\"name\":\"dep\",\"dist-tags\":{\"latest\":\"1.1.0\"},\"modified\":" +
                             "\"2020-01-01T00:00:00.000Z\"," + RELEASE_AGE_DEP_VERSIONS + "}";
        String other = packument("other", "sha512-T+IZa8fsNV04GfEvW4q7Hpp3OgkztzM+8J2cSU6kfhvsFOaENlLtH0f5Zh8/ay1cDg9g3" +
                                          "+0HA0eRMsFXrF8deQ==", "0140d8d61616b2c1d9993d53994e6491ff227b4d");
        Result result = regenerate(OTHER_LOCK.replace(
            "sha512-B5T0c5qMU4jr7vSqdzBzA7kl08kqg3zcGy7CWODLqYz7n+VXkvT8vGLGqrlEW8oKm9jLQj3wHbyJK19YUpZflw==",
            "sha512-T+IZa8fsNV04GfEvW4q7Hpp3OgkztzM+8J2cSU6kfhvsFOaENlLtH0f5Zh8/ay1cDg9g3+0HA0eRMsFXrF8deQ=="),
          Collections.singletonMap("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"dep\":\"^1.0.0\"," +
                                       "\"other\":\"1.0.0\"}}"),
          Collections.singletonMap(".npmrc", NPMRC + "minimum-release-age=1440\nregistry-supports-time-field=true\n"),
          version, Map.of("dep", full, "other", other), Map.of("dep", abbreviated, "other", other),
          Collections.emptyMap());
        if (picked != null) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(RELEASE_AGE_LOCK.formatted(picked, "1.0.0".equals(picked) ?
              "sha512-DHXEiK7g17yWT6SWglM2dM3+HU+DGCzHL2V0nbSzk4W7usDHxHQBFdkzzqyfKpp8k5w6zJVB1DZ4cPbSp8jcNw==" :
              "sha512-ZeJMvVBT7q82GsgnBrNyugpLq5D3L2V66yPFftLLxaRIJVIvKz4GYmLi2h0vIwVc3PaAvYK/7m3QvUhHRE6siQ=="));
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getDetail()).contains("ERR_PNPM_MISSING_TIME",
              "The metadata of dep is missing the \"time\" field");
        }
    }

    /**
     * A project .npmrc naming the file loaded as the user's or global config, there a repository file setting
     * auto-install-peers=false, as pnpm read it: before 10.34.3 (pnpm/pnpm#12328, @pnpm/npm-conf 3.0.3) npm-conf loads
     * that file, which is no input of the engine; from 10.34.3 it does not, and the lock is up to date.
     */
    @ParameterizedTest
    @CsvSource({"10.0.0, userconfig", "10.11.0, userconfig", "10.12.1, userconfig", "10.34.2, userconfig",
      "10.34.3, userconfig", "10.34.9, userconfig", "10.12.1, globalconfig", "10.34.2, globalconfig",
      "10.34.3, globalconfig", "10.34.9, globalconfig", "10.12.1, prefix", "10.34.2, prefix", "10.34.3, prefix",
      "10.34.9, prefix"})
    void aProjectNpmrcNamingTheUserOrGlobalConfig(String version, String key) {
        String lock = OTHER_LOCK.replace(
          "sha512-B5T0c5qMU4jr7vSqdzBzA7kl08kqg3zcGy7CWODLqYz7n+VXkvT8vGLGqrlEW8oKm9jLQj3wHbyJK19YUpZflw==",
          "sha512-T+IZa8fsNV04GfEvW4q7Hpp3OgkztzM+8J2cSU6kfhvsFOaENlLtH0f5Zh8/ay1cDg9g3+0HA0eRMsFXrF8deQ==");
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".npmrc", NPMRC + key + "=" + ("prefix".equals(key) ? "pfx" : key.replace("config", "") + ".npmrc") +
                            "\n");
        files.put("prefix".equals(key) ? "pfx/etc/npmrc" : key.replace("config", "") + ".npmrc",
          "auto-install-peers=false\n");
        Result result = regenerate(lock,
          Collections.singletonMap("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"other\":\"1.0.0\"}}"),
          files, version, Collections.singletonMap("other", packument("other", "sha512-T+IZa8fsNV04GfEvW4q7Hpp3OgkztzM" +
                                                                               "+8J2cSU6kfhvsFOaENlLtH0f5Zh8/ay1cDg9g3+0HA0eRMsFXrF8deQ==", "0140d8d61616b2c1d9993d53994e6491ff227b4d")));
        if (PnpmRelease.of(version).atLeast(10, 34, 3)) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(lock);
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
            assertThat(result.getFailure().getDetail()).contains("pnpm 10 native profile",
              "a project .npmrc naming " + key);
        }
    }

    private static Result regenerate(@Nullable String lock, Map<String, String> manifests, Map<String, String> files,
                                     String version, Map<String, String> full, Map<String, String> abbreviated,
                                     Map<String, String> env) {
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifests(manifests)
          .files(files)
          .paths(PnpmScenarios.paths(files, manifests, ""))
          .env(env)
          .now(Instant.parse("2026-11-25T00:00:00Z"))
          .release(PnpmRelease.of(version))
          .build();
        return new PnpmLockRegenerator(inputs, new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean fullMetadata) {
                return (fullMetadata ? full : abbreviated).get(name);
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
