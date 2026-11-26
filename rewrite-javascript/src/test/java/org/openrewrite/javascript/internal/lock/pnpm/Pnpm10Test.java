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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.semver.NodeSemver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

class Pnpm10Test {

    /**
     * The supported pnpm 10 releases without native recordings of their own, each with the recorded release whose
     * recordings test it: pnpm's lock-relevant code, the libraries it bundles included, does not change between them,
     * and no question of {@link PnpmRelease} tells them apart ({@link #anAliasAnswersEveryQuestionAsItsRecording}).
     */
    private static final Map<String, String> RECORDED_AS = Map.ofEntries(
      // v10.12.0..v10.12.1 holds one commit, which makes @pnpm/tsconfig private
      Map.entry("10.12.0", "10.12.1"),
      // v10.33.0..v10.33.3: self-update, npm passthrough, global bin shim, fork-bomb guard and picomatch 4.0.3 to 4.0.4
      Map.entry("10.33.1", "10.33.0"), Map.entry("10.33.2", "10.33.0"), Map.entry("10.33.3", "10.33.0"),
      // v10.34.5..v10.34.9: self-update, signing keys, headless and modules-cleaner, Node 24 for @pnpm/exe
      Map.entry("10.34.5", "10.34.9"), Map.entry("10.34.6", "10.34.9"), Map.entry("10.34.7", "10.34.9"),
      Map.entry("10.34.8", "10.34.9")
    );

    static Stream<String> pnpm10Releases() {
        return PnpmReleases.all().stream().filter(r -> r.startsWith("10.") && PnpmReleases.isProven(r));
    }

    static String recorded(String version) {
        return RECORDED_AS.getOrDefault(version, version);
    }

    private static PnpmScenarios.Scenario upgrade(String version) {
        String name = "pnpm" + recorded(version).replace(".", "") + "-upgrade";
        return PnpmScenarios.discover().stream()
          .filter(s -> s.getName().equals(name))
          .findFirst().orElseThrow(() -> new AssertionError("no scenario " + name));
    }

    // the questions of PnpmRelease that releases answer differently, asked of them as one release they may be
    private static List<String> questionsTellingApart(String... versions) {
        List<String> releases = new ArrayList<>(PnpmReleases.all());
        releases.retainAll(Arrays.asList(versions));
        PnpmRelease either = PnpmRelease.oneOf(String.join(" || ", versions), releases);
        List<String> differing = new ArrayList<>();
        int asked = 0;
        for (Method question : PnpmRelease.class.getDeclaredMethods()) {
            if (Modifier.isStatic(question.getModifiers()) || question.getParameterCount() != 0 ||
                question.getReturnType() != boolean.class && question.getReturnType() != NodeSemver.Release.class) {
                continue;
            }
            asked++;
            question.setAccessible(true);
            try {
                question.invoke(either);
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof CannotKnow)) {
                    throw new AssertionError(question.getName(), e.getCause());
                }
                differing.add(question.getName());
            } catch (IllegalAccessException e) {
                throw new AssertionError(question.getName(), e);
            }
        }
        assertThat(asked).as("questions asked").isGreaterThan(100);
        return differing;
    }

    // the recording's inputs as a project that names the release it is replayed as, which its recording names
    private static PnpmLockRegenerator.Inputs pinnedTo(PnpmLockRegenerator.Inputs inputs, String version) {
        String recorded = recorded(version);
        if (recorded.equals(version)) {
            return inputs;
        }
        String pin = "\"packageManager\": \"pnpm@" + recorded + "\"";
        assertThat(inputs.getManifests().get("")).contains(pin);
        Map<String, String> manifests = new LinkedHashMap<>();
        for (Map.Entry<String, String> manifest : inputs.getManifests().entrySet()) {
            manifests.put(manifest.getKey(), manifest.getValue().replace(pin,
              "\"packageManager\": \"pnpm@" + version + "\""));
        }
        return inputs.toBuilder().clearManifests().manifests(manifests).build();
    }

    @Test
    void everySupportedReleaseIsRecordedOrAliasedToARecording() {
        for (Map.Entry<String, String> alias : RECORDED_AS.entrySet()) {
            assertThat(PnpmReleases.isProven(alias.getKey())).as(alias.getKey()).isTrue();
            assertThat(RECORDED_AS).as(alias.getValue()).doesNotContainKey(alias.getValue());
        }
        for (String version : pnpm10Releases().collect(Collectors.toList())) {
            assertThat(PnpmScenarios.discover()).as(version)
              .filteredOn(s -> s.getName().startsWith("pnpm" + version.replace(".", "") + "-"))
              .hasSize(RECORDED_AS.containsKey(version) ? 0 : 8);
        }
    }

    @Test
    void anAliasAnswersEveryQuestionAsItsRecording() {
        assertThat(questionsTellingApart("10.11.0", "10.12.1")).contains("prefersTheCurrentPackageVersion");
        for (Map.Entry<String, String> alias : RECORDED_AS.entrySet()) {
            assertThat(PnpmRelease.of(alias.getKey()).atLeast(11, 0, 0))
              .isEqualTo(PnpmRelease.of(alias.getValue()).atLeast(11, 0, 0));
            assertThat(questionsTellingApart(alias.getKey(), alias.getValue())).as(alias.getKey()).isEmpty();
        }
    }

    @Test
    void supportsOnlyTheRecordedPnpm10Releases() {
        for (String version : pnpm10Releases().collect(Collectors.toList())) {
            assertThat(PnpmReleases.runs(version)).hasToString(version);
        }
        for (String version : new String[]{"10.0.1", "10.11.1", "10.34.4", "^10.0.0"}) {
            assertThatThrownBy(() -> PnpmReleases.installs(version))
              .isInstanceOf(org.openrewrite.javascript.internal.lock.CannotKnow.class);
        }
    }

    @Test
    void registryAliasesAreWithinTheNativeProfile() {
        assertThat(Pnpm10Profile.unsupportedSpecifier("npm:wrap-ansi@^7.0.0")).isFalse();
    }

    @ParameterizedTest
    @MethodSource("pnpm10Releases")
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
    @MethodSource("pnpm10Releases")
    void refusesUnportedPatchAndRuntimePaths(String version) {
        PnpmScenarios.Scenario scenario = upgrade(version);
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
        for (String version : pnpm10Releases().collect(Collectors.toList())) {
            PnpmScenarios.Scenario scenario = upgrade(version);
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
    @MethodSource("pnpm10Releases")
    void keepsSupportedDependencyFormsWithinTheProfile(String version) {
        PnpmScenarios.Scenario scenario = upgrade(version);
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
    @MethodSource("pnpm10Releases")
    void readsNpmrcPeerSettings(String version) {
        PnpmOptions options = PnpmOptions.load(null, "auto-install-peers=false\n", null,
          obj("{'name':'test','version':'1.0.0'}"), Collections.emptyMap(), PnpmRelease.of(version));
        assertThat(options.get("autoInstallPeers")).isEqualTo(false);
        assertThat(options.get("minimumReleaseAge")).isEqualTo(0.0);
    }

    @ParameterizedTest
    @MethodSource("pnpm10Releases")
    void readsManifestOverrides(String version) {
        PnpmOptions options = PnpmOptions.load(null, null, null,
          obj("{'name':'test','pnpm':{'overrides':{'is-number':'6.0.0'}}}"),
          Collections.emptyMap(), PnpmRelease.of(version));
        assertThat(options.getOverrides()).containsEntry("is-number", "6.0.0");
    }

    @ParameterizedTest
    @MethodSource("pnpm10Releases")
    void reproducesNativeRecordings(String version) {
        String prefix = "pnpm" + recorded(version).replace(".", "") + "-";
        int count = 0;
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            if (!scenario.getName().startsWith(prefix)) {
                continue;
            }
            count++;
            ScenarioRegistry registry = new ScenarioRegistry(scenario, "after");
            Result result = new PnpmLockRegenerator(pinnedTo(PnpmScenarioTest.inputs(scenario, scenario.file("before"),
              scenario.otherLocks(org.openrewrite.javascript.internal.lock.LockScenario.Phase.BEFORE), version),
              version), registry).regenerate();
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
    @CsvSource({"10.0.0, false", "10.11.0, false", "10.12.0, false", "10.12.1, false", "10.33.0, true",
      "10.33.4, true", "10.34.9, true"})
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
    @CsvSource({"10.0.0, false", "10.11.0, false", "10.12.0, false", "10.12.1, false", "10.33.0, true",
      "10.33.4, true", "10.34.9, true"})
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

    /**
     * A pnpm-workspace.yaml holding a setting but no packages, beside package.json files in other directories, as
     * pnpm read it: before 10.5.0 the file needs packages; in 10.29.0 to 10.29.1 and 10.31.0 to 10.32.0 every
     * package.json outside node_modules is a project; otherwise the root alone.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.11.1", "10.12.0", "10.12.1", "10.28.2", "10.29.1", "10.29.2",
      "10.29.3", "10.30.3", "10.31.0", "10.32.0", "10.32.1", "10.33.0", "10.34.9"})
    void aWorkspaceManifestWithoutPackages(String version) {
        assertRecorded("workspace-without-packages", version);
    }

    /**
     * A lock whose importer version its unchanged range does not admit, as pnpm installed it: from 10.15.0
     * (pnpm/pnpm#9832) the importer is out of date and resolved again; before, the lock is kept as it is.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.14.0", "10.15.0", "10.16.0", "10.34.9"})
    void anImporterVersionOutsideItsRange(String version) {
        assertRecorded("importer-version-out-of-range", version);
    }

    /**
     * A bare {@code workspace:} of a prerelease workspace package, as pnpm linked it, before and after 10.29.0
     * (pnpm/pnpm#10436) alike.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.28.2", "10.29.1", "10.34.9"})
    void aBareWorkspaceSpecOfAPrerelease(String version) {
        assertRecorded("bare-workspace-prerelease", version);
    }

    /**
     * A dependency the registry lacks, or has no version of in range, under link-workspace-packages, beside a workspace
     * package of a version outside the range, as pnpm resolved it: from 10.31.0 (pnpm/pnpm#10704) the registry's error;
     * before, the workspace package is linked.
     */
    @ParameterizedTest
    @CsvSource({"workspace-fallback-absent, 10.12.1", "workspace-fallback-absent, 10.30.3",
      "workspace-fallback-absent, 10.31.0", "workspace-fallback-absent, 10.32.1", "workspace-fallback-absent, 10.34.9",
      "workspace-fallback-no-match, 10.12.1", "workspace-fallback-no-match, 10.30.3",
      "workspace-fallback-no-match, 10.31.0", "workspace-fallback-no-match, 10.32.1",
      "workspace-fallback-no-match, 10.34.9"})
    void aWorkspaceFallbackOutsideTheRange(String recording, String version) {
        assertRecorded(recording, version);
    }

    /**
     * An npm alias without a range, of a package whose latest is below its highest version, as pnpm resolved it,
     * before and after 10.27.0 (pnpm/pnpm#10374) alike.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.26.2", "10.27.0", "10.34.9"})
    void anNpmAliasWithoutARange(String version) {
        assertRecorded("npm-alias-without-range", version);
    }

    /**
     * A registry setting with an uppercase host, as pnpm wrote the packages it resolved: from 10.28.1
     * (normalize-registry-url 2.0.1, pnpm/pnpm#10470) the registry is the tarballs' own, and they are left out;
     * before, each tarball is written.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.12.1", "10.26.2", "10.27.0", "10.28.0", "10.28.1", "10.28.2", "10.34.9"})
    void aRegistryWithAnUppercaseHost(String version) {
        assertRecorded("registry-uppercase-host", version);
    }

    /**
     * A dependency whose full packument alone declares libc, under minimumReleaseAge, as pnpm wrote it: from 10.16.0
     * (pnpm/pnpm#9957) pnpm 10 fetches full metadata, and the lock has the libc; before, no release age is read.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.15.0", "10.33.0", "10.34.9"})
    void aReleaseAgeFetchesFullMetadata(String version) {
        assertRecorded("release-age-libc", version);
    }

    /**
     * A minimumReleaseAge from 10.16.0 to 10.32.x, whose rules the port does not have
     * ({@link PnpmRelease#readsAReleaseAgeThePortDoesNot}).
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.16.0", "10.26.2", "10.28.2"})
    void refusesAReleaseAgeOfTheReleasesThatChangedIt(String version) {
        assertCannotKnow("release-age-libc", version, "pnpm 10 native profile for minimumReleaseAge");
    }

    /**
     * A root manifest's supportedArchitectures.libc, as pnpm wrote a dependency whose full packument alone declares
     * libc: every pnpm 10 fetches full metadata for it, from 10.14.0 (pnpm/pnpm#9745) through getFetchFullMetadata.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.13.1", "10.14.0", "10.34.9"})
    void aRootManifestsLibc(String version) {
        assertRecorded("libc-manifest", version);
    }

    /**
     * A pnpm-workspace.yaml's supportedArchitectures.libc, as pnpm wrote a dependency whose full packument alone
     * declares libc: from 10.14.0 (pnpm/pnpm#9745) full metadata is fetched for it; before, not.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.13.1", "10.14.0"})
    void aWorkspaceManifestsLibc(String version) {
        assertRecorded("libc-workspace-packages", version);
    }

    /**
     * A package locked with hasBin whose abbreviated packument names no bin, its full one a directories.bin, beside a
     * new dependency, as pnpm wrote it: from 10.18.3 (pnpm/pnpm#10065) hasBin is kept; before, it is dropped.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.18.2", "10.18.3", "10.27.0", "10.34.9"})
    void aLockedHasBinOfAManifestWithoutBin(String version) {
        assertRecorded("locked-has-bin", version);
    }

    /**
     * A packument's tarball URL with a default port, or an uppercase host, as pnpm wrote it: from 10.26.0
     * (pnpm/pnpm#10273) the URL is serialized as WHATWG URL does; before, written as it is.
     */
    @ParameterizedTest
    @CsvSource({"tarball-url-port80, 10.12.1", "tarball-url-port80, 10.25.0", "tarball-url-port80, 10.26.0",
      "tarball-url-port80, 10.27.0", "tarball-url-port80, 10.34.9", "tarball-url-upper, 10.12.1",
      "tarball-url-upper, 10.25.0", "tarball-url-upper, 10.26.0", "tarball-url-upper, 10.27.0",
      "tarball-url-upper, 10.34.9"})
    void aTarballUrlNotInItsCanonicalForm(String recording, String version) {
        assertRecorded(recording, version);
    }

    /**
     * trustPolicy no-downgrade, a newer version without the provenance an older has, as pnpm resolved a new dependency:
     * from 10.21.0 (pnpm/pnpm#10103) {@code ERR_PNPM_TRUST_DOWNGRADE}; before, the setting is no setting. Before
     * 10.5.0 a pnpm-workspace.yaml needs packages.
     */
    @ParameterizedTest
    @CsvSource({"trust-policy-npmrc, 10.0.0", "trust-policy-npmrc, 10.12.1", "trust-policy-npmrc, 10.20.0",
      "trust-policy-npmrc, 10.33.0", "trust-policy-npmrc, 10.34.9", "trust-policy-workspace, 10.0.0",
      "trust-policy-workspace, 10.12.1", "trust-policy-workspace, 10.20.0", "trust-policy-workspace, 10.33.0",
      "trust-policy-workspace, 10.34.9"})
    void aTrustPolicy(String recording, String version) {
        assertRecorded(recording, version);
    }

    /**
     * trustPolicy no-downgrade from 10.21.0 to 10.32.x, whose rules the port does not have
     * ({@link PnpmRelease#readsATrustPolicyThePortDoesNot}).
     */
    @ParameterizedTest
    @CsvSource({"trust-policy-npmrc, 10.21.0", "trust-policy-npmrc, 10.26.2", "trust-policy-workspace, 10.21.0",
      "trust-policy-workspace, 10.26.2"})
    void refusesATrustPolicyOfTheReleasesThatChangedIt(String recording, String version) {
        assertCannotKnow(recording, version, "pnpm 10 native profile for trustPolicy no-downgrade");
    }

    /**
     * A dependency whose manifest has engines.runtime, as pnpm wrote it: before 10.21.0 (pnpm/pnpm#10141) an engines
     * field like any other; from then a {@code runtime:} dependency, which the pnpm 10 profile does not resolve.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.20.0"})
    void theEnginesRuntimeOfADependency(String version) {
        assertRecorded("engines-runtime-of-a-dependency", version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.21.0", "10.27.0", "10.34.9"})
    void refusesTheRuntimeDependencyOfADependency(String version) {
        assertCannotKnow("engines-runtime-of-a-dependency", version, "runtime:");
    }

    /**
     * A cycle of peers through npm aliases of the packages in it (upstream's deadlock case), as pnpm resolved it:
     * from 10.12.4 (pnpm/pnpm#9675) the lock; before, pnpm waits forever.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.4", "10.34.9"})
    void aPeerCycleThroughNpmAliases(String version) {
        assertRecorded("aliased-peer-cycle", version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.12.1", "10.12.3"})
    void refusesAPeerCycleThroughNpmAliasesPnpmWaitsForForever(String version) {
        assertCannotKnow("aliased-peer-cycle", version, "waits for forever");
    }

    /**
     * An auto-installed peer an override then pins to an exact version below the locked one, as pnpm wrote it: from
     * 10.30.2 (pnpm/pnpm#10655) the pinned version; before, the locked one is kept.
     */
    @ParameterizedTest
    @ValueSource(strings = {"10.12.1", "10.29.3", "10.30.1", "10.30.2", "10.32.1", "10.33.0", "10.34.9"})
    void anExactPeerVersion(String version) {
        assertRecorded("exact-peer-version", version);
    }

    private static void assertRecorded(String name, String version) {
        Pnpm10Recording recording = Pnpm10Recording.of(name);
        Result result = recording.regenerate(version);
        String lock = recording.lock(version);
        if (lock != null) {
            assertThat(result.isSuccess()).as(result.getErrorMessage()).isTrue();
            assertThat(result.getLockFileContent()).isEqualTo(lock);
        } else {
            assertThat(result.getFailure()).as(result.getLockFileContent()).isNotNull();
            assertThat(result.getFailure().getKind()).isNotEqualTo(Kind.CANNOT_KNOW);
            assertThat(result.getFailure().getDetail()).contains(recording.error(version));
        }
    }

    private static void assertCannotKnow(String name, String version, String detail) {
        Result result = Pnpm10Recording.of(name).regenerate(version);
        assertThat(result.getFailure()).as(result.getLockFileContent()).isNotNull();
        assertThat(result.getFailure().getKind()).as(result.getFailure().getDetail()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).contains(detail);
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
