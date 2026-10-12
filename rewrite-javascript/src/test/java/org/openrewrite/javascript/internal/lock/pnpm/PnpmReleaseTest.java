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
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.DependenciesTreeNode;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.NodeId;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.PeerDependency;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.ResolvedPackage;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.DependenciesGraphNode;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.ProjectToResolve;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * What each pnpm release does otherwise than 11.2.2, as {@link PnpmRelease} answers it: each question on both sides of
 * the release that changed it, from the module upstream changed (tag {@code v<release>} of pnpm/pnpm). Where no
 * recorded scenario reaches a change, the case is what the releases wrote when run on it.
 */
class PnpmReleaseTest {

    private static final String REGISTRY = "https://registry.npmjs.org/";
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");

    private static PnpmRelease release(String version) {
        return PnpmRelease.of(version);
    }

    @Test
    void reproducesTheProvenReleases() {
        assertThat(PnpmReleases.all()).startsWith("9.0.0").contains("11.2.2", "12.12.1");
        assertThat(PnpmReleases.runs(null)).isNull();
        assertThat(PnpmReleases.runs("11.0.6")).hasToString("11.0.6");
        assertThat(PnpmReleases.runs("11.5.1")).hasToString("11.5.1");
        assertThat(PnpmReleases.isProven("11.0.6 - 11.5.1")).isTrue();
        assertThat(PnpmReleases.isProven("^11.0.6")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.0.2", "10.34.6", "12.0.0", "11.6.0-rc.0", "13.0.0"})
    void aReleaseOutsideTheProvenOnesIsCannotKnow(String version) {
        assertThatThrownBy(() -> PnpmReleases.runs(version))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("what pnpm " + version + " writes, since the engine reproduces pnpm " +
                                PnpmReleases.ranges(PnpmReleases.proven()));
    }

    @Test
    void aRangeOfReleasesAnswersWhereTheyAgree() {
        PnpmRelease either = PnpmReleases.installs("11.2.2 || 11.5.1");
        assertThat(either.marksGitHostedTarballs()).isTrue();
        assertThatThrownBy(either::cutsCyclesOfPendingPeers)
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("one of 11.2.2 || 11.5.1, where the releases from 11.5.0 install it otherwise");
        assertThatThrownBy(either::version).isInstanceOf(CannotKnow.class);
    }

    /**
     * config/reader index.ts (11.0.4): minimumReleaseAge set explicitly makes minimumReleaseAgeStrict default to true;
     * before, it stays unset.
     */
    @Test
    void defaultsMinimumReleaseAgeStrictFrom1104() {
        assertThat(options("minimumReleaseAge: 60\n", null, null, Collections.emptyMap(), "11.0.4")
          .get("minimumReleaseAgeStrict")).isEqualTo(true);
        assertThat(options("minimumReleaseAge: 60\n", null, null, Collections.emptyMap(), "11.0.3")
          .get("minimumReleaseAgeStrict")).isNull();
    }

    /**
     * config/reader env.ts (11.0.6): a {@code PNPM_CONFIG_} variable sets a setting; before, only the lower-case form.
     */
    @Test
    void readsUpperCaseConfigVariablesFrom1106() {
        Map<String, String> env = Collections.singletonMap("PNPM_CONFIG_MINIMUM_RELEASE_AGE", "5");
        assertThat(options("", null, null, env, "11.0.6").get("minimumReleaseAge")).isEqualTo(5.0);
        assertThat(options("", null, null, env, "11.0.5").get("minimumReleaseAge")).isEqualTo(1440.0);
    }

    /**
     * config/reader getOptionsFromRootManifest.ts (11.0.6): overrides that are no object of strings are
     * {@code ERR_PNPM_INVALID_OVERRIDES}; what a release before makes of them the engine does not follow.
     */
    @Test
    void rejectsOverridesThatAreNoStringsFrom1106() {
        assertThatThrownBy(() -> options("overrides:\n  foo: 1\n", null, null, Collections.emptyMap(), "11.0.6"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_OVERRIDES");
              assertThat(e.getMessage()).isEqualTo("The value of overrides.foo should be a string, but got number");
          });
        assertThatThrownBy(() -> options("overrides:\n  foo: 1\n", null, null, Collections.emptyMap(), "11.0.5"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("what pnpm 11.0.5 makes of overrides that are no object of strings");
    }

    /**
     * workspace/workspace-manifest-writer (11.0.6): a level that is not sorted keeps its order, a new key last;
     * before, every level is sorted, packages first, as pnpm 10 does, whose writer the recipes take for it.
     */
    @Test
    void keepsTheWorkspaceManifestKeyOrderFrom1106() {
        assertThat(release("11.0.6").keepsTheWorkspaceManifestKeyOrder()).isTrue();
        assertThat(release("11.0.5").keepsTheWorkspaceManifestKeyOrder()).isFalse();
        List<String> unsorted = Arrays.asList("packages", "zeta", "alpha");
        assertThat(new WorkspaceManifestWriter(11).insertionIndex(unsorted, "beta")).isEqualTo(3);
        assertThat(new WorkspaceManifestWriter(10).insertionIndex(unsorted, "beta")).isEqualTo(2);
    }

    /**
     * lockfile/utils toLockfileResolution.ts (11.0.7): a git-hosted tarball's resolution is marked
     * {@code gitHosted: true}; before, it has no such field.
     */
    @Test
    void marksAGitHostedTarballFrom1107() {
        String codeload = "https://codeload.github.com/jonschlinkert/is-number/tar.gz/98e8ff1da1a89f93d1397a24d7413ed15421c139";
        Map<String, @Nullable Object> resolution = obj("{'integrity':'sha512-x','tarball':'" + codeload + "'}");
        assertThat(json(LockfileUtils.toLockfileResolution("is-number", "7.0.0", resolution, REGISTRY, false,
          release("11.0.7")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + codeload + "','gitHosted':true}");
        assertThat(json(LockfileUtils.toLockfileResolution("is-number", "7.0.0", resolution, REGISTRY, false,
          release("11.0.6")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + codeload + "'}");
    }

    /**
     * lockfile/utils toLockfileResolution.ts (11.0.8, pnpm/pnpm#11276): under lockfileIncludeTarballUrl false, a
     * tarball URL the registry does not give is kept; before, the integrity alone. The env installer passes no
     * setting, which 11.0.7 compares the tarball of, and fails on a resolution without one.
     */
    @Test
    void keepsATarballUrlTheRegistryCannotGiveFrom1108() {
        String mirror = "https://registry.npmmirror.com/ms/-/ms-2.1.3.tgz";
        Map<String, @Nullable Object> resolution = obj("{'integrity':'sha512-x','tarball':'" + mirror + "'}");
        assertThat(json(LockfileUtils.toLockfileResolution("ms", "2.1.3", resolution, REGISTRY, false,
          release("11.0.8")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + mirror + "'}");
        assertThat(json(LockfileUtils.toLockfileResolution("ms", "2.1.3", resolution, REGISTRY, false,
          release("11.0.7")))).isEqualTo("{'integrity':'sha512-x'}");
        Map<String, @Nullable Object> integrityAlone = obj("{'integrity':'sha512-x'}");
        assertThat(json(LockfileUtils.toLockfileResolution("ms", "2.1.3", integrityAlone, REGISTRY, null,
          release("11.0.8")))).isEqualTo("{'integrity':'sha512-x'}");
        assertThatThrownBy(() -> LockfileUtils.toLockfileResolution("ms", "2.1.3", integrityAlone, REGISTRY, null,
          release("11.0.7")))
          .isInstanceOf(JsError.class)
          .hasMessageContaining("Cannot read properties of undefined (reading 'replaceAll')");
    }

    private static final String DIRECTORY_LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                                 "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n" +
                                                 "    dependencies:\n      foo:\n        specifier: file:../foo\n" +
                                                 "        version: file:../foo\n\npackages:\n\n" +
                                                 "  foo@file:../foo: {}\n\nsnapshots:\n\n  foo@file:../foo: {}\n";

    /**
     * lockfile/fs lockfileFormatConverters.ts (11.2.0): a package without a resolution whose dep path is a
     * {@code file:} directory reads as that directory's resolution; before, it has none.
     */
    @Test
    void readsADirectoryResolutionFromItsDepPathFrom1120() {
        assertThat(json(LockfileReader.read(DIRECTORY_LOCK, release("11.2.0")).getLockfile().getPackages()
          .get("foo@file:../foo").getResolution())).isEqualTo("{'directory':'../foo','type':'directory'}");
        assertThat(LockfileReader.read(DIRECTORY_LOCK, release("11.1.3")).getLockfile().getPackages()
          .get("foo@file:../foo").getResolution()).isNull();
    }

    /**
     * lockfile/utils pkgSnapshotToResolution.ts (11.4.0): a registry package locked without an integrity is
     * {@code ERR_PNPM_MISSING_TARBALL_INTEGRITY}; before, its tarball URL is filled in.
     */
    @Test
    void requiresALockedTarballsIntegrityFrom1140() {
        PackageSnapshot snapshot = new PackageSnapshot();
        snapshot.setResolution(new LinkedHashMap<>());
        Map<String, String> registries = Collections.singletonMap("default", REGISTRY);
        assertThatThrownBy(() -> LockfileUtils.pkgSnapshotToResolution("ms@2.1.3", snapshot, registries,
          release("11.4.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_TARBALL_INTEGRITY");
              assertThat(e.getMessage()).isEqualTo("Cannot install package \"ms@2.1.3\": its lockfile entry has " +
                                                   "no \"integrity\" field, so pnpm cannot verify the downloaded " +
                                                   "tarball.");
          });
        assertThat(json(LockfileUtils.pkgSnapshotToResolution("ms@2.1.3", snapshot, registries, release("11.3.0"))))
          .isEqualTo("{'tarball':'https://registry.npmjs.org/ms/-/ms-2.1.3.tgz'}");
    }

    /**
     * lockfile/fs yamlDocuments.ts (11.1.3, pnpm/pnpm#11612): a lock with CRLF line ends reads its env document;
     * before, the document separator is not found.
     */
    @Test
    void readsACrLfLockWithAnEnvDocumentFrom1113() {
        String env = "lockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    configDependencies: {}\n";
        String lock = ("---\n" + env + "\n---\nlockfileVersion: '9.0'\n").replace("\n", "\r\n");
        assertThat(LockfileReader.readFirstYamlDocument(lock, release("11.1.3"))).isEqualTo(env);
        assertThat(LockfileReader.readFirstYamlDocument(lock, release("11.1.2"))).isNull();
        assertThat(LockfileReader.extractMainDocument(lock, release("11.1.3"))).isEqualTo("lockfileVersion: '9.0'\n");
        assertThat(LockfileReader.extractMainDocument(lock, release("11.1.2"))).isEqualTo(lock);
    }

    private static PnpmOptions options(String workspace, @Nullable String npmrc,
                                       @Nullable Map<String, @Nullable Object> rootManifest,
                                       Map<String, String> env, String release) {
        return PnpmOptions.load(workspace, npmrc, null, rootManifest, env, release(release));
    }

    /**
     * config/reader getOptionsFromRootManifest.ts and resolving/npm-resolver (11.1.0, pnpm/pnpm#11324):
     * namedRegistries is read, its URLs take the environment and each is checked; before, it is no setting.
     */
    @Test
    void readsNamedRegistriesFrom1110() {
        Map<String, String> env = Collections.singletonMap("REG", "https://npm.example.com/");
        PnpmOptions later = options("namedRegistries:\n  acme: ${REG}\n", null, null, env, "11.1.0");
        assertThat(json(later.getNamedRegistries())).isEqualTo("{'acme':'https://npm.example.com/'}");
        assertThat(CreateNewStoreController.namedRegistries(later)).containsEntry("acme", "https://npm.example.com/");
        PnpmOptions earlier = options("namedRegistries:\n  acme: ${REG}\n", null, null, env, "11.0.9");
        assertThat(json(earlier.getNamedRegistries())).isEqualTo("{'acme':'${REG}'}");
        assertThat(CreateNewStoreController.namedRegistries(earlier)).isEmpty();

        String bad = "namedRegistries:\n  foo: ftp://example.com/\n";
        assertThatThrownBy(() -> CreateNewStoreController.namedRegistries(options(bad, null, null, env, "11.1.0")))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_URL"));
        assertThat(CreateNewStoreController.namedRegistries(options(bad, null, null, env, "11.0.9"))).isEmpty();
    }

    /**
     * config/reader getOptionsFromRootManifest.ts translateUpdateSettings and translateAuditSettings (11.16.0): the
     * update and audit settings are objects, and update no longer the install's; before, both are read as they are
     * (open-install-options-as-if-unset records it).
     */
    @Test
    void translatesTheUpdateAndAuditSettingsFrom11160() {
        assertThatThrownBy(() -> options("update: false\n", null, null, Collections.emptyMap(), "11.16.0"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_SETTING");
              assertThat(e.getMessage()).isEqualTo("The \"update\" setting should be an object, but got boolean");
          });
        assertThat(options("update: false\n", null, null, Collections.emptyMap(), "11.15.1").get("update"))
          .isEqualTo(false);
        PnpmOptions translated = options("update:\n  ignoreDeps: [foo]\naudit:\n  level: high\n", null, null,
          Collections.emptyMap(), "11.16.0");
        assertThat(translated.get("update")).isNull();
        assertThat(json(translated.get("updateConfig"))).isEqualTo("{'ignoreDependencies':['foo']}");
        assertThat(translated.get("auditLevel")).isEqualTo("high");
        assertThatThrownBy(() -> options("audit:\n  level: severe\n", null, null, Collections.emptyMap(), "11.16.0"))
          .hasMessage("The \"audit.level\" setting should be one of info, low, moderate, high, critical, but got " +
                      "\"severe\"");
    }

    /**
     * config/reader index.ts parseDevEnginesPackageManager (11.1.3, pnpm/pnpm#11676): devEngines.packageManager as
     * one object without onFail takes pmOnFail's default, download; before, error.
     */
    @Test
    void takesPmOnFailForDevEnginesWithoutOnFailFrom1113() {
        Map<String, @Nullable Object> manifest = obj("{'name':'p','devEngines':{'packageManager':" +
                                                     "{'name':'pnpm','version':'11.0.0'}}}");
        assertThat(options("", null, manifest, Collections.emptyMap(), "11.1.3").getWantedPackageManager()
          .getOnFail()).isEqualTo("download");
        assertThat(options("", null, obj(json(manifest)), Collections.emptyMap(), "11.1.2").getWantedPackageManager()
          .getOnFail()).isEqualTo("error");
    }

    /**
     * config/reader index.ts getNodeVersionFromEnginesRuntime (11.4.0): a node runtime whose version is no valid
     * range gives no nodeVersion; before, semver's minVersion throws (open-runtime-invalid-range).
     */
    @Test
    void skipsAnInvalidNodeRuntimeRangeFrom1140() {
        Map<String, @Nullable Object> manifest = obj("{'devEngines':{'runtime':{'name':'node','version':'>=x.y'}}}");
        assertThat(PnpmOptions.getNodeVersionFromEnginesRuntime(manifest, release("11.4.0"))).isNull();
        assertThatThrownBy(() -> PnpmOptions.getNodeVersionFromEnginesRuntime(manifest, release("11.3.0")))
          .isInstanceOf(JsError.class)
          .hasMessageContaining("Invalid comparator: >=x.y");
    }

    /**
     * config/reader loadNpmrcFiles.ts (11.4.0): an .npmrc's unscoped credentials are pinned to its registry, where a
     * credential scoped there already wins; before, the unscoped ones are checked as the default registry's.
     */
    @Test
    void rescopesUnscopedCredentialsFrom1140() {
        // _auth of "bad" decodes to no user:password pair; the scoped one is user:pass
        String npmrc = "_auth=bad\n//registry.npmjs.org/:_auth=dXNlcjpwYXNz\n";
        options("", npmrc, null, Collections.emptyMap(), "11.4.0");
        assertThatThrownBy(() -> options("", npmrc, null, Collections.emptyMap(), "11.3.0"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_MISSING_SEPARATOR"));
        // pinned to the registry the same .npmrc names, the unscoped one is checked there
        assertThatThrownBy(() -> options("", "registry=https://npm.example.com/\n_auth=bad\n", null,
          Collections.emptyMap(), "11.4.0"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_MISSING_SEPARATOR"));
    }

    /**
     * pnpm syncEnvLockfile.ts (11.1.3, pnpm/pnpm#11674): the package manager check records the release that runs in
     * an env document without a pnpm entry; before, only over an entry the wanted range does not satisfy.
     */
    @Test
    void recordsTheReleaseAnEnvDocumentLacksFrom1113() {
        PnpmOptions.WantedPackageManager pm = new PnpmOptions.WantedPackageManager("pnpm", "^11.0.0", "error", true);
        EnvLockfile without = EnvLockfile.createEnvLockfile();
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm, () -> without, release("11.1.3"))).isEqualTo("11.1.3");
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm, () -> without, release("11.1.2"))).isNull();
    }

    /**
     * config/pick-registry-for-package (11.2.1): an npm: alias whose target is unscoped is fetched from the default
     * registry; before, from the registry of the alias's own scope.
     */
    @Test
    void fetchesAnUnscopedAliasTargetFromTheDefaultRegistryFrom1121() {
        Map<String, String> registries = new LinkedHashMap<>();
        registries.put("default", REGISTRY);
        registries.put("@private", "https://npm.private.example/");
        assertThat(PickRegistryForPackage.pickRegistryForPackage(registries, "@private/foo", "npm:lodash@^1",
          release("11.2.1"))).isEqualTo(REGISTRY);
        assertThat(PickRegistryForPackage.pickRegistryForPackage(registries, "@private/foo", "npm:lodash@^1",
          release("11.2.0"))).isEqualTo("https://npm.private.example/");
        assertThat(PickRegistryForPackage.pickRegistryForPackage(registries, "@private/foo", "npm:@private/bar@^1",
          release("11.2.0"))).isEqualTo("https://npm.private.example/");
    }

    private static final String WRAPPY = "{\"name\":\"wrappy\",\"dist-tags\":{\"latest\":\"1.0.2\"},\"versions\":" +
                                         "{\"1.0.2\":{\"name\":\"wrappy\",\"version\":\"1.0.2\",\"dist\":{\"integrity\":" +
                                         "\"sha512-l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wx" +
                                         "tcFB4bgpQPRcR3QCvezPcQ==\",\"tarball\":\"https://registry.npmjs.org/wrappy" +
                                         "/-/wrappy-1.0.2.tgz\"}}},\"modified\":\"2022-06-26T23:35:39.000Z\"}";

    // wrappy's abbreviated packument, all a test's install fetches
    private static final RegistryMetadata WRAPPY_ONLY = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return "wrappy".equals(name) ? WRAPPY : fail("fetched " + registry + name);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return fail("fetched the attestations of " + name + "@" + version);
        }

        @Override
        public @Nullable String document(String url) {
            return fail("fetched " + url);
        }
    };

    private static final String MS_TARBALL = "https://registry.yarnpkg.com/ms/-/ms-2.1.3.tgz";
    private static final String MS_INTEGRITY = "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz" +
                                               "7R9xAOtHnSO/tXtF3WRTlA==";

    // what pnpm 11.4.0 and 11.5.0 wrote for {"ms": MS_TARBALL} on a lock-only install
    private static final String TARBALL_LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                               "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n" +
                                               "    dependencies:\n      ms:\n        specifier: " + MS_TARBALL + "\n" +
                                               "        version: " + MS_TARBALL + "\n\npackages:\n\n  ms@" +
                                               MS_TARBALL + ":\n    resolution: {integrity: " + MS_INTEGRITY +
                                               ", tarball: " + MS_TARBALL + "}\n    version: 2.1.3\n\nsnapshots:\n\n" +
                                               "  ms@" + MS_TARBALL + ": {}\n";

    // what each release wrote over it, with wrappy 1.0.2 added beside ms, its lock-only install fetching no tarball
    private static String tarballLockWithWrappy(boolean msIntegrity) {
        return "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
               "importers:\n\n  .:\n    dependencies:\n      ms:\n        specifier: " + MS_TARBALL + "\n" +
               "        version: " + MS_TARBALL + "\n      wrappy:\n        specifier: 1.0.2\n" +
               "        version: 1.0.2\n\npackages:\n\n  ms@" + MS_TARBALL + ":\n    resolution: {" +
               (msIntegrity ? "integrity: " + MS_INTEGRITY + ", " : "") + "tarball: " + MS_TARBALL + "}\n" +
               "    version: 2.1.3\n\n  wrappy@1.0.2:\n    resolution: {integrity: sha512-l4Sp/DRseor9wL6EvV2+TuQn63" +
               "dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRcR3QCvezPcQ==}\n\nsnapshots:\n\n  ms@" +
               MS_TARBALL + ": {}\n\n  wrappy@1.0.2: {}\n";
    }

    private static String addWrappyBesideMs(String release) {
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(TARBALL_LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"ms\":\"" + MS_TARBALL +
                        "\",\"wrappy\":\"1.0.2\"}}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(NOW)
          .release(release(release))
          .build();
        return new PnpmLockRegenerator(inputs, WRAPPY_ONLY).writeLock();
    }

    /**
     * installing/package-requester packageRequester.ts (11.5.0, pnpm/pnpm#12001): a locked remote tarball the
     * resolver gives without an integrity, and pnpm does not fetch, keeps the locked integrity; before, it is written
     * without one. pnpm 11.2.2, 11.4.0 and 11.5.0 wrote these locks when run on it.
     */
    @Test
    void keepsALockedIntegrityTheResolverDoesNotGiveFrom1150() {
        assertThat(addWrappyBesideMs("11.5.0")).isEqualTo(tarballLockWithWrappy(true));
        assertThat(addWrappyBesideMs("11.4.0")).isEqualTo(tarballLockWithWrappy(false));
        assertThat(addWrappyBesideMs("11.2.2")).isEqualTo(tarballLockWithWrappy(false));
    }

    private static final String WRAPPY_LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                              "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n" +
                                              "    dependencies:\n      wrappy:\n        specifier: 1.0.2\n" +
                                              "        version: 1.0.2\n\npackages:\n\n  wrappy@1.0.2:\n" +
                                              "    resolution: {integrity: sha512-l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9X" +
                                              "kghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRcR3QCvezPcQ==}\n\nsnapshots:\n\n" +
                                              "  wrappy@1.0.2: {}\n";

    // the lock-only install of a project with wrappy beside fields, as the release, up to its resolution
    private static Install.Result install(String release, String fields, @Nullable String workspace,
                                          RegistryMetadata registry) {
        return regenerator(release, fields, workspace, registry).install();
    }

    private static PnpmLockRegenerator regenerator(String release, String fields, @Nullable String workspace,
                                                   RegistryMetadata registry) {
        PnpmLockRegenerator.Inputs.InputsBuilder inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(WRAPPY_LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}" + fields + "}")
          .env(Collections.emptyMap())
          .now(NOW)
          .host(Host.builder().nodeVersion("v24.21.0").os("darwin").cpu("arm64").build())
          .release(release(release));
        if (workspace != null) {
            inputs.file("pnpm-workspace.yaml", workspace);
        }
        return new PnpmLockRegenerator(inputs.build(), registry);
    }

    /**
     * pnpm main.ts checkRuntime (11.4.0, pnpm/pnpm#11818): a runtime the root manifest wants with onFail error is
     * checked against the one that runs; before, only download does anything. pnpm 11.4.0 failed and 11.3.0 wrote
     * the lock when run on it with Node.js 24.
     */
    @Test
    void checksTheRuntimeVersionFrom1140() {
        String runtime = ",\"devEngines\":{\"runtime\":{\"name\":\"node\",\"version\":\">=99\",\"onFail\":\"error\"}}";
        assertThatThrownBy(() -> install("11.4.0", runtime, "minimumReleaseAge: 0\n", WRAPPY_ONLY))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_BAD_RUNTIME_VERSION");
              assertThat(e.getMessage())
                .isEqualTo("This project requires Node.js >=99. Your current Node.js is v24.21.0");
              assertThat(e.getHint()).startsWith("If you want to bypass this version check, set \"runtimeOnFail\"");
          });
        assertThat(install("11.3.0", runtime, "minimumReleaseAge: 0\n", WRAPPY_ONLY).getWritten())
          .isEqualTo(WRAPPY_LOCK);
        // warn only warns, and the setting decides over the manifest
        assertThat(install("11.4.0", runtime.replace("\"error\"", "\"warn\""), "minimumReleaseAge: 0\n",
          WRAPPY_ONLY).getWritten()).isEqualTo(WRAPPY_LOCK);
        assertThat(install("11.4.0", runtime, "minimumReleaseAge: 0\nruntimeOnFail: ignore\n", WRAPPY_ONLY)
          .getWritten()).isEqualTo(WRAPPY_LOCK);
        // whether the host runs Deno the inputs do not say
        assertThatThrownBy(() -> install("11.4.0", runtime.replace("node", "deno"), "minimumReleaseAge: 0\n",
          WRAPPY_ONLY))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("whether the host runs Deno >=99");
    }

    /**
     * installing/deps-resolver getWantedDependencies.ts and getNonDevWantedDependencies.ts (11.4.0): a dependency
     * name no npm package can have is {@code ERR_PNPM_INVALID_DEPENDENCY_NAME}; before, it is resolved. pnpm 11.4.0
     * failed so, and 11.3.0 went to resolve {@code .hidden}, when run on it.
     */
    @Test
    void rejectsAnInvalidDependencyNameFrom1140() {
        Map<String, @Nullable Object> project = obj("{'name':'p','dependencies':{'.hidden':'1.0.0'}}");
        assertThatThrownBy(() -> ResolveDependencyTree.getWantedDependencies(project, true, release("11.4.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_DEPENDENCY_NAME");
              assertThat(e.getMessage())
                .isEqualTo("The current package contains a dependency with an invalid name: \".hidden\"");
          });
        assertThat(ResolveDependencyTree.getWantedDependencies(project, true, release("11.3.0"))).hasSize(1);
        Map<String, @Nullable Object> pkg = obj("{'name':'q','version':'1.0.0','dependencies':{'node_modules':'1'}}");
        assertThatThrownBy(() -> ResolveDependencies.getNonDevWantedDependencies(pkg, release("11.4.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getMessage())
            .isEqualTo("Package \"q@1.0.0\" contains a dependency with an invalid name: \"node_modules\""));
        assertThat(ResolveDependencies.getNonDevWantedDependencies(pkg, release("11.3.0"))).hasSize(1);
    }

    // a registry that serves wrappy and counts what it is asked for
    private static final class Counting implements RegistryMetadata {
        final List<String> fetched = new ArrayList<>();

        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            fetched.add(name + (fullMetadata ? " (full)" : ""));
            return WRAPPY_ONLY.packument(registry, name, fullMetadata);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return WRAPPY_ONLY.attestations(registry, name, version);
        }

        @Override
        public @Nullable String document(String url) {
            return WRAPPY_ONLY.document(url);
        }
    }

    /**
     * installing/deps-installer install/index.ts (11.3.0): trustLockfile skips the verification of the lock's
     * resolutions against minimumReleaseAge; before, it is no setting, and the verification reads the packuments.
     */
    @Test
    void readsTrustLockfileFrom1130() {
        Counting trusting = new Counting();
        assertThat(install("11.3.0", "", "trustLockfile: true\n", trusting).getWritten()).isEqualTo(WRAPPY_LOCK);
        assertThat(trusting.fetched).isEmpty();
        Counting verifying = new Counting();
        assertThat(install("11.2.2", "", "trustLockfile: true\n", verifying).getWritten()).isEqualTo(WRAPPY_LOCK);
        assertThat(verifying.fetched).isNotEmpty();
    }

    /**
     * resolving/npm-resolver trustChecks.ts (11.4.0): a trusted publisher counts as trust evidence only with a
     * provenance attestation; before, alone.
     */
    @Test
    void requiresProvenanceOfATrustedPublisherFrom1140() {
        Map<String, @Nullable Object> alone = obj("{'_npmUser':{'name':'gh','trustedPublisher':{'id':'github'}}}");
        assertThat(TrustChecks.getTrustEvidence(alone, release("11.4.0"))).isNull();
        assertThat(TrustChecks.getTrustEvidence(alone, release("11.3.0"))).isEqualTo("trustedPublisher");
        Map<String, @Nullable Object> attested = obj("{'_npmUser':{'name':'gh','trustedPublisher':{'id':'github'}}," +
                                                     "'dist':{'attestations':{'provenance':{'predicateType':'x'}}}}");
        assertThat(TrustChecks.getTrustEvidence(attested, release("11.4.0"))).isEqualTo("trustedPublisher");
    }

    /**
     * resolving/npm-resolver trustChecks.ts (11.5.0, pnpm/pnpm#11887): a staged publish, approved by another
     * maintainer, is the strongest trust evidence, and a version without one after it a downgrade; before, none.
     */
    @Test
    void ranksAStagedPublishFrom1150() {
        Map<String, @Nullable Object> staged = obj("{'_npmUser':{'name':'a','approver':{'name':'b'}}," +
                                                   "'dist':{'attestations':{'provenance':{'predicateType':'x'}}}}");
        assertThat(TrustChecks.getTrustEvidence(staged, release("11.5.0"))).isEqualTo("stagedPublish");
        assertThat(TrustChecks.getTrustEvidence(staged, release("11.4.0"))).isEqualTo("provenance");
        Map<String, @Nullable Object> meta = obj("{'name':'foo','versions':{" +
                                                 "'1.0.0':{'name':'foo','version':'1.0.0','_npmUser':{'name':'a'," +
                                                 "'approver':{'name':'b'}},'dist':{'attestations':{'provenance':{}}}}," +
                                                 "'1.0.1':{'name':'foo','version':'1.0.1','dist':{'attestations':" +
                                                 "{'provenance':{}}}}},'time':{'1.0.0':'2026-01-01T00:00:00.000Z'," +
                                                 "'1.0.1':'2026-02-01T00:00:00.000Z'}}");
        assertThatThrownBy(() -> TrustChecks.failIfTrustDowngraded(meta, "1.0.1", null, null, NOW,
          release("11.5.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_TRUST_DOWNGRADE"));
        TrustChecks.failIfTrustDowngraded(meta, "1.0.1", null, null, NOW, release("11.4.0"));
    }

    private static Map<String, @Nullable Object> fooMeta(@Nullable String modified, boolean withTime) {
        return obj("{'name':'foo','dist-tags':{'latest':'1.1.0'},'versions':{" +
                   "'1.0.0':{'name':'foo','version':'1.0.0','dist':{'tarball':'" + REGISTRY + "foo/-/foo-1.0.0.tgz'}}," +
                   "'1.1.0':{'name':'foo','version':'1.1.0','dist':{'tarball':'" + REGISTRY + "foo/-/foo-1.1.0.tgz'}}}" +
                   (modified == null ? "" : ",'modified':'" + modified + "'") +
                   (withTime ? ",'time':{'1.0.0':'2026-10-10T00:00:00.000Z','1.1.0':'2026-10-11T00:00:00.000Z'}" : "") +
                   "}");
    }

    private static PickPackage.PickerOptions picker(String cutoff, boolean strict, String release) {
        return PickPackage.PickerOptions.builder().publishedBy(Instant.parse(cutoff)).strictPublishedByCheck(strict)
          .release(release(release)).build();
    }

    /**
     * resolving/npm-resolver pickPackage.ts and index.ts (11.1.3, pnpm/pnpm#10438, #10488, #11687): a range of
     * immature versions only is picked at its lowest and reported as a policy violation; before, under
     * minimumReleaseAgeStrict the picker leaves it unpicked, which the resolver fails as immature.
     */
    @Test
    void checksTheReleaseAgeAsAPolicyFrom1113() {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("^1.0.0", "foo",
          "latest", REGISTRY);
        Map<String, @Nullable Object> meta = fooMeta(null, true);
        assertThat(PickPackage.pickRespectingMinReleaseAge(picker("2026-10-01T00:00:00Z", true, "11.1.3"), spec,
          meta)).containsEntry("version", "1.0.0");
        assertThat(PickPackage.pickRespectingMinReleaseAge(picker("2026-10-01T00:00:00Z", true, "11.1.2"), spec,
          meta)).isNull();
        assertThat(PickPackage.pickRespectingMinReleaseAge(picker("2026-10-01T00:00:00Z", false, "11.1.2"), spec,
          meta)).containsEntry("version", "1.0.0");
    }

    /**
     * resolving/npm-resolver pickPackageFromMeta.ts (11.2.0): an abbreviated packument modified exactly at the
     * release-age cutoff counts as mature; before, it needs the time field it lacks.
     */
    @Test
    void takesAPackumentModifiedAtTheCutoffAsMatureFrom1120() {
        String cutoff = "2026-10-01T00:00:00.000Z";
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("^1.0.0", "foo",
          "latest", REGISTRY);
        assertThat(PickPackageFromMeta.pickPackageFromMeta(PickPackageFromMeta::pickVersionByVersionRange, null,
          Instant.parse(cutoff), null, fooMeta(cutoff, false), spec, release("11.2.0")))
          .containsEntry("version", "1.1.0");
        assertThatThrownBy(() -> PickPackageFromMeta.pickPackageFromMeta(PickPackageFromMeta::pickVersionByVersionRange,
          null, Instant.parse(cutoff), null, fooMeta(cutoff, false), spec, release("11.1.3")))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_TIME"));
    }

    /**
     * resolving/npm-resolver pickPackage.ts (11.1.2): an abbreviated packument the run already holds, changed after
     * the release-age cutoff, is fetched again in full; before, it is picked from as it is, which needs the time
     * field it lacks.
     */
    @Test
    void upgradesAHeldPackumentForTheReleaseAgeFrom1112() {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("^1.0.0", "foo",
          "latest", REGISTRY);
        List<String> fetched = new ArrayList<>();
        RegistryMetadata full = new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean fullMetadata) {
                fetched.add(name + (fullMetadata ? " (full)" : ""));
                return json(fooMeta("2026-10-11T00:00:00.000Z", true)).replace('\'', '"');
            }

            @Override
            public @Nullable String attestations(String registry, String name, String version) {
                return fail("fetched attestations");
            }

            @Override
            public @Nullable String document(String url) {
                return fail("fetched " + url);
            }
        };
        Map<String, Map<String, @Nullable Object>> held = new LinkedHashMap<>();
        held.put("foo", fooMeta("2026-10-11T00:00:00.000Z", false));
        assertThat(PickPackage.pickPackage(held, full, spec, picker("2026-10-01T00:00:00Z", false, "11.1.2"),
          REGISTRY, false, false).getPickedPackage()).containsEntry("version", "1.0.0");
        assertThat(fetched).containsExactly("foo (full)");
        held.put("foo", fooMeta("2026-10-11T00:00:00.000Z", false));
        assertThatThrownBy(() -> PickPackage.pickPackage(held, full, spec, picker("2026-10-01T00:00:00Z", false,
          "11.1.1"), REGISTRY, false, false))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_TIME"));
        assertThat(fetched).containsExactly("foo (full)");
    }

    /**
     * resolving/default-resolver (11.1.1): the local resolver's path shapes come after the named registries, so
     * {@code gh:@scope/pkg} is fetched from GitHub's registry; before, a spec with a slash is a local directory.
     * pnpm 11.1.1 went to npm.pkg.github.com, and 11.1.0 linked the directory, which did not exist, when run on it;
     * whether it exists the engine does not know without the project's paths.
     */
    @Test
    void resolvesNamedRegistrySpecsBeforeLocalPathsFrom1111() {
        String gh = ",\"devDependencies\":{\"foo\":\"gh:@acme/foo@1.0.0\"}";
        assertThatThrownBy(() -> regenerator("11.1.1", gh, "minimumReleaseAge: 0\n", WRAPPY_ONLY).writeLock())
          .hasMessageContaining("fetched https://npm.pkg.github.com/@acme/foo");
        assertThatThrownBy(() -> regenerator("11.1.0", gh, "minimumReleaseAge: 0\n", WRAPPY_ONLY).writeLock())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("the package.json of gh:@acme/foo@1.0.0");
    }

    /**
     * installing/deps-resolver updateLockfile.ts (11.5.1, pnpm/pnpm#12067): a remote tarball's entry rebuilt without
     * an integrity keeps the locked one for the same tarball; before, it is written without. Upstream's
     * updateLockfile.test.ts of 11.5.1.
     */
    @Test
    void keepsTheIntegrityOfALockedRemoteTarballFrom1151() {
        assertThat(json(rebuiltXlsx("11.5.1"))).isEqualTo("{'tarball':'" + XLSX + "','integrity':'" + XLSX_INTEGRITY +
                                                          "'}");
        assertThat(json(rebuiltXlsx("11.5.0"))).isEqualTo("{'tarball':'" + XLSX + "'}");
    }

    private static final String XLSX = "https://cdn.sheetjs.com/xlsx-0.18.5/xlsx-0.18.5.tgz";
    private static final String XLSX_INTEGRITY = "sha512-AaaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa" +
                                                 "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaA==";

    private static @Nullable Map<String, @Nullable Object> rebuiltXlsx(String release) {
        String depPath = "xlsx@" + XLSX;
        ResolvedPackage xlsx = new ResolvedPackage(XLSX, "xlsx", "0.18.5");
        xlsx.setResolution(obj("{'tarball':'" + XLSX + "'}"));
        Map<String, DependenciesGraphNode> graph = new LinkedHashMap<>();
        graph.put(depPath, new DependenciesGraphNode(depPath, xlsx));
        LockfileObject lockfile = new LockfileObject();
        ProjectSnapshot importer = new ProjectSnapshot();
        importer.setDependencies(new LinkedHashMap<>(Collections.singletonMap("xlsx", XLSX)));
        importer.setSpecifiers(new LinkedHashMap<>(Collections.singletonMap("xlsx", XLSX)));
        lockfile.getImporters().put(".", importer);
        PackageSnapshot locked = new PackageSnapshot();
        locked.setResolution(obj("{'tarball':'" + XLSX + "','integrity':'" + XLSX_INTEGRITY + "'}"));
        lockfile.setPackages(new LinkedHashMap<>(Collections.singletonMap(depPath, locked)));
        return UpdateLockfile.updateLockfile(graph, lockfile, Collections.singletonMap("default", REGISTRY), false,
          release(release)).getPackages().get(depPath).getResolution();
    }

    /**
     * installing/deps-resolver resolvePeers.ts (11.5.1, pnpm/pnpm#12079): a parent package a node inherits gives way
     * to its own child of the name where the two were resolved against other versions of a peer one of its children
     * shares; before, only where they differ. Upstream's resolvePeers.ts test of 11.5.1.
     */
    @Test
    void replacesAnInheritedParentThatBreaksAPeerDiamondFrom1151() {
        assertThat(diamond("11.5.1")).contains("plugin/1.0.0(parser/1.0.0(typescript/1.0.0))(typescript/1.0.0)")
          .doesNotContain("plugin/1.0.0(parser/1.0.0(typescript/2.0.0))(typescript/1.0.0)");
        assertThat(diamond("11.5.0")).contains("plugin/1.0.0(parser/1.0.0(typescript/2.0.0))(typescript/1.0.0)");
    }

    private static List<String> diamond(String release) {
        ResolvedPackage ts1 = peerPkg("typescript", "1.0.0");
        ResolvedPackage ts2 = peerPkg("typescript", "2.0.0");
        ResolvedPackage parser = peerPkg("parser", "1.0.0", "typescript");
        ResolvedPackage plugin = peerPkg("plugin", "1.0.0", "parser", "typescript");
        ResolvedPackage umbrella = peerPkg("umbrella", "1.0.0", "typescript");
        ResolvedPackage app = peerPkg("app", "1.0.0");
        Map<NodeId, DependenciesTreeNode> tree = new LinkedHashMap<>();
        treeNode(tree, ">typescript/2.0.0>", ts2, 0);
        treeNode(tree, ">parser/1.0.0>", parser, 0);
        treeNode(tree, ">app/1.0.0>", app, 0, "typescript", ">app/1.0.0>typescript/1.0.0>", "umbrella",
          ">app/1.0.0>umbrella/1.0.0>");
        treeNode(tree, ">app/1.0.0>typescript/1.0.0>", ts1, 1);
        treeNode(tree, ">app/1.0.0>umbrella/1.0.0>", umbrella, 1, "plugin", ">app/1.0.0>umbrella/1.0.0>plugin/1.0.0>",
          "parser", ">app/1.0.0>umbrella/1.0.0>parser/1.0.0>");
        treeNode(tree, ">app/1.0.0>umbrella/1.0.0>plugin/1.0.0>", plugin, 2);
        treeNode(tree, ">app/1.0.0>umbrella/1.0.0>parser/1.0.0>", parser, 2);
        Map<String, NodeId> direct = new LinkedHashMap<>();
        direct.put("typescript", NodeId.of(">typescript/2.0.0>"));
        direct.put("parser", NodeId.of(">parser/1.0.0>"));
        direct.put("app", NodeId.of(">app/1.0.0>"));
        List<ProjectToResolve> projects = Collections.singletonList(new ProjectToResolve(".", "", direct,
          Collections.emptyList()));
        return new ArrayList<>(ResolvePeers.resolvePeers(new LinkedHashSet<>(Arrays.asList("typescript", "parser")),
          projects, tree, false, false, false, false, 1000, "", new LinkedHashMap<>(), new LinkedHashSet<>(),
          release(release)).getDependenciesGraph().keySet());
    }

    private static ResolvedPackage peerPkg(String name, String version, String... peers) {
        ResolvedPackage pkg = new ResolvedPackage("", name, version);
        pkg.setPkgIdWithPatchHash(name + "/" + version);
        Map<String, PeerDependency> peerDependencies = new LinkedHashMap<>();
        for (String peer : peers) {
            peerDependencies.put(peer, new PeerDependency("*"));
        }
        pkg.setPeerDependencies(peerDependencies);
        return pkg;
    }

    private static void treeNode(Map<NodeId, DependenciesTreeNode> tree, String nodeId, ResolvedPackage pkg,
                                 int depth, String... children) {
        DependenciesTreeNode node = new DependenciesTreeNode(pkg, depth);
        Map<String, NodeId> childNodeIds = new LinkedHashMap<>();
        for (int i = 0; i < children.length; i += 2) {
            childNodeIds.put(children[i], NodeId.of(children[i + 1]));
        }
        node.setChildren(childNodeIds);
        node.setInstallable(true);
        tree.put(NodeId.of(nodeId), node);
    }

    // packuments by name, each of one version 1.0.0 with the given dependencies and peers: name, deps, peers
    private static RegistryMetadata registry(String[]... packages) {
        Map<String, String> packuments = new LinkedHashMap<>();
        for (String[] p : packages) {
            String name = p[0];
            packuments.put(name, "{\"name\":\"" + name + "\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{" +
                                 "\"1.0.0\":{\"name\":\"" + name + "\",\"version\":\"1.0.0\",\"dependencies\":{" + p[1] +
                                 "},\"peerDependencies\":{" + p[2] + "},\"optionalDependencies\":{" +
                                 (p.length > 3 ? p[3] : "") + "},\"dist\":{\"integrity\":\"sha512-" +
                                 name.replaceAll("[^a-z]", "") + "\",\"tarball\":\"" + REGISTRY + name +
                                 "/-/x-1.0.0.tgz\"}}}}");
        }
        return new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean fullMetadata) {
                String packument = packuments.get(name);
                return packument != null ? packument : fail("fetched " + registry + name);
            }

            @Override
            public @Nullable String attestations(String registry, String name, String version) {
                return fail("fetched the attestations of " + name + "@" + version);
            }

            @Override
            public @Nullable String document(String url) {
                return fail("fetched " + url);
            }
        };
    }

    private static final String EMPTY_LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                             "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n";

    private static String installAliasedCycle(String release) {
        String a = "@pnpm.e2e/aliased-cycle-a-real";
        String x = "@pnpm.e2e/aliased-cycle-x";
        String y = "@pnpm.e2e/aliased-cycle-y";
        RegistryMetadata registry = registry(
          new String[]{a, "\"b\":\"npm:@pnpm.e2e/aliased-cycle-b-real@1.0.0\"," +
                          "\"c\":\"npm:@pnpm.e2e/aliased-cycle-c-real@1.0.0\"", ""},
          new String[]{"@pnpm.e2e/aliased-cycle-b-real", "\"" + x + "\":\"1.0.0\"", "\"a\":\"npm:" + a + "@1.0.0\""},
          new String[]{"@pnpm.e2e/aliased-cycle-c-real", "\"" + y + "\":\"1.0.0\"", "\"a\":\"npm:" + a + "@1.0.0\""},
          new String[]{x, "", "\"" + y + "\":\"1.0.0\""},
          new String[]{y, "", "\"" + x + "\":\"1.0.0\""});
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(EMPTY_LOCK)
          .manifest("", "{\"name\":\"root\",\"dependencies\":{\"a\":\"npm:" + a + "@1.0.0\"}}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(NOW)
          .release(release(release))
          .build();
        return new PnpmLockRegenerator(inputs, registry).writeLock();
    }

    /**
     * installing/deps-resolver resolvePeers.ts calculateDepPath (11.5.0, pnpm/pnpm#11999): a cycle of peers one of
     * the pending peers is in counts as cyclic, so that the paths of the cycle's members do not wait on each other;
     * before, only a cycle the package itself is in, and pnpm hangs. Upstream's cyclicPeerDeterminism.ts of 11.5.0.
     */
    @Test
    void cutsCyclesOfPendingPeersFrom1150() {
        assertThat(installAliasedCycle("11.5.0"))
          .contains("      a:\n        specifier: npm:@pnpm.e2e/aliased-cycle-a-real@1.0.0\n" +
                    "        version: '@pnpm.e2e/aliased-cycle-a-real@1.0.0");
        assertThatThrownBy(() -> installAliasedCycle("11.4.0"))
          .hasMessageContaining("never settled");
    }

    /**
     * installing/env-installer resolveConfigDeps.ts and resolveOptionalSubdeps.ts (11.2.0, 11.2.1): a config
     * dependency's optional dependencies are resolved into the env document; before 11.2.0, the config dependency's
     * snapshot is empty. 11.2.1 marks a new one's snapshot optional where 11.2.0 leaves it empty, which the env
     * document's pruning marks optional either way, as reachable through optional dependencies only.
     */
    @Test
    void resolvesTheOptionalDependenciesOfAConfigDependencyFrom1120() {
        String foo = "@pnpm.e2e/foo";
        String bar = "@pnpm.e2e/bar";
        RegistryMetadata registry = registry(new String[]{foo, "", "", "\"" + bar + "\":\"1.0.0\""},
          new String[]{bar, "", ""});
        assertThat(json(configDeps("11.2.1", registry).snapshots()))
          .isEqualTo("{'" + bar + "@1.0.0':{'optional':true},'" + foo + "@1.0.0':{'optionalDependencies':{'" + bar +
                     "':'1.0.0'}}}");
        assertThat(json(configDeps("11.2.0", registry).snapshots()))
          .isEqualTo(json(configDeps("11.2.1", registry).snapshots()));
        EnvLockfile before = configDeps("11.1.3", registry);
        assertThat(json(before.snapshots())).isEqualTo("{'" + foo + "@1.0.0':{}}");
        assertThat(before.packages()).containsOnlyKeys(foo + "@1.0.0");
    }

    private static EnvLockfile configDeps(String release, RegistryMetadata registry) {
        EnvLockfile.OnDisk lockFile = new EnvLockfile.OnDisk(null);
        new EnvInstaller(registry, NOW, Host.UNKNOWN).resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'1.0.0'}"), lockFile, PnpmOptions.load(null, null, null, null,
            Collections.singletonMap("pnpm_config_minimum_release_age", "0"), release(release)));
        EnvLockfile written = EnvLockfile.readEnvLockfile(lockFile.text());
        assertThat(written).isNotNull();
        return written;
    }

    // upstream's resolvePeers.ts test of 11.5.2, "locked peer provider preferences": the consumer, under wrapper,
    // the lock records with peer 2.0.0, which retainer keeps in the graph; the project's own peer is 1.0.0
    private static List<String> lockedPeerProvider(boolean secondPass, boolean nestedPeerMustWin) {
        ResolvedPackage peer1 = peerPkg("peer", "1.0.0");
        ResolvedPackage peer2 = peerPkg("peer", "2.0.0");
        ResolvedPackage consumer = peerPkg("consumer", "1.0.0");
        consumer.getPeerDependencies().put("peer", new PeerDependency(">=1"));
        Map<NodeId, DependenciesTreeNode> tree = new LinkedHashMap<>();
        treeNode(tree, ">peer/1.0.0>", peer1, 0);
        tree.get(NodeId.of(">peer/1.0.0>")).setPreviousDepPath("peer/1.0.0");
        treeNode(tree, ">retainer/1.0.0>", peerPkg("retainer", "1.0.0"), 0, "peer", ">retainer/1.0.0>peer/2.0.0>");
        treeNode(tree, ">retainer/1.0.0>peer/2.0.0>", peer2, 1);
        tree.get(NodeId.of(">retainer/1.0.0>peer/2.0.0>")).setPreviousDepPath("peer/2.0.0");
        if (nestedPeerMustWin) {
            treeNode(tree, ">wrapper/1.0.0>", peerPkg("wrapper", "1.0.0"), 0, "consumer",
              ">wrapper/1.0.0>consumer/1.0.0>", "peer", ">peer/1.0.0>");
            tree.get(NodeId.of(">wrapper/1.0.0>")).setDependencyNamesWhoseCurrentProviderMustWin(
              new LinkedHashSet<>(Collections.singletonList("peer")));
        } else {
            treeNode(tree, ">wrapper/1.0.0>", peerPkg("wrapper", "1.0.0"), 0, "consumer",
              ">wrapper/1.0.0>consumer/1.0.0>");
        }
        treeNode(tree, ">wrapper/1.0.0>consumer/1.0.0>", consumer, 1);
        tree.get(NodeId.of(">wrapper/1.0.0>consumer/1.0.0>")).setLockedPeerContext(
          Collections.singletonMap("peer", "peer/2.0.0"));
        Map<String, NodeId> direct = new LinkedHashMap<>();
        if (!nestedPeerMustWin) {
            direct.put("peer", NodeId.of(">peer/1.0.0>"));
        }
        direct.put("retainer", NodeId.of(">retainer/1.0.0>"));
        direct.put("wrapper", NodeId.of(">wrapper/1.0.0>"));
        List<ProjectToResolve> projects = Collections.singletonList(new ProjectToResolve("", "", direct,
          Collections.emptyList(), new LinkedHashSet<>(), new LinkedHashSet<>()));
        ResolvePeers.Result first = ResolvePeers.resolvePeers(new LinkedHashSet<>(Collections.singletonList("peer")),
          projects, tree, false, false, false, false, 1000, "", new LinkedHashMap<>(), new LinkedHashSet<>(),
          release("11.5.2"));
        ResolvePeers.Result result = !secondPass ? first : ResolvePeers.resolvePeers(
          new LinkedHashSet<>(Collections.singletonList("peer")), projects, tree, false, false, false, false, 1000,
          "", new LinkedHashMap<>(), new LinkedHashSet<>(), release("11.5.2"), first.getPathsByNodeId());
        return new ArrayList<>(result.getDependenciesGraph().keySet());
    }

    /**
     * installing/deps-resolver index.ts and resolvePeers.ts (11.5.2, pnpm/pnpm#12083): peers are resolved again from
     * the peer contexts the lock records, a provider still in the graph and in range kept unless a current choice
     * must win; before, once. Upstream's resolvePeers.ts test of 11.5.2.
     */
    @Test
    void prefersThePeerContextsTheLockRecordsFrom1152() {
        assertThat(release("11.5.2").prefersThePeerContextsTheLockRecords()).isTrue();
        assertThat(release("11.5.1").prefersThePeerContextsTheLockRecords()).isFalse();
        assertThat(lockedPeerProvider(false, false)).contains("consumer/1.0.0(peer/1.0.0)");
        assertThat(lockedPeerProvider(true, false)).contains("consumer/1.0.0(peer/2.0.0)");
        assertThat(lockedPeerProvider(true, true)).contains("consumer/1.0.0(peer/1.0.0)")
          .doesNotContain("consumer/1.0.0(peer/2.0.0)");
    }

    private static ResolutionVerifier.Result verifyTarball(String release, String lockedTarball) {
        RegistryMetadata registry = registry(new String[]{"foo", "", ""});
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(
          NpmResolutionVerifier.Options.builder()
            .minimumReleaseAge(0.0)
            .registries(Collections.singletonMap("default", REGISTRY))
            .fetch(registry)
            .now(NOW)
            .release(release(release))
            .build());
        return verifier == null ? ResolutionVerifier.Result.OK :
          verifier.verify(obj("{'integrity':'sha512-x','tarball':'" + lockedTarball + "'}"), "foo", "1.0.0", null);
    }

    /**
     * resolving/npm-resolver createNpmResolutionVerifier.ts (11.5.2): every install verifies the lock's resolutions,
     * and a registry entry's explicit tarball URL must be the one the registry's metadata lists; before, nothing is
     * verified without minimumReleaseAge or trustPolicy.
     */
    @Test
    void bindsALockedTarballUrlToTheRegistryFrom1152() {
        assertThat(verifyTarball("11.5.2", REGISTRY + "foo/-/x-1.0.0.tgz").isOk()).isTrue();
        ResolutionVerifier.Result mismatch = verifyTarball("11.5.2", "https://evil.example/foo-1.0.0.tgz");
        assertThat(mismatch.getCode()).isEqualTo("TARBALL_URL_MISMATCH");
        assertThat(mismatch.getReason()).isEqualTo("has a tarball URL (https://evil.example/foo-1.0.0.tgz) that does " +
                                                   "not match the registry's published metadata (" + REGISTRY +
                                                   "foo/-/x-1.0.0.tgz)");
        assertThat(verifyTarball("11.5.1", "https://evil.example/foo-1.0.0.tgz").isOk()).isTrue();
    }

    /**
     * resolving/npm-resolver createNpmResolutionVerifier.ts isNpmRegistryResolution (11.5.2): a tarball neither http
     * nor https is no registry resolution to verify; before, it is.
     */
    @Test
    void verifiesOnlyHttpTarballsFrom1152() {
        Map<String, @Nullable Object> local = obj("{'integrity':'sha512-x','tarball':'file:foo-1.0.0.tgz'}");
        assertThat(NpmResolutionVerifier.isNpmRegistryResolution(local, release("11.5.2"))).isFalse();
        assertThat(NpmResolutionVerifier.isNpmRegistryResolution(local, release("11.5.1"))).isTrue();
    }

    private static Map<String, @Nullable Object> catalogPrefer(String entry) {
        return WorkspaceManifest.readWorkspaceManifest("catalogMode: prefer\ncatalog:\n  wrappy: " + entry + "\n");
    }

    /**
     * installing/deps-installer install/index.ts installSome (11.5.2, pnpm/pnpm#11570): catalogMode compares a range
     * as no version, a mismatch; before, semver.eq throws Invalid Version. And a {@code runtime:} dependency is
     * never saved to the catalog.
     */
    @Test
    void comparesCatalogSpecifiersAsVersionsOnlyFrom1152() {
        assertThat(Install.installSome("wrappy", "^1.0.0", catalogPrefer("^1.0.2"), release("11.5.2")))
          .isEqualTo(new Install.Saved("^1.0.0", null));
        assertThatThrownBy(() -> Install.installSome("wrappy", "^1.0.0", catalogPrefer("^1.0.2"), release("11.5.1")))
          .isInstanceOf(org.openrewrite.javascript.internal.lock.Rejected.class)
          .hasMessageContaining("Invalid Version");
        Map<String, @Nullable Object> runtime = WorkspaceManifest.readWorkspaceManifest(
          "catalogMode: strict\ncatalog:\n  node: 20.0.0\n");
        assertThat(Install.installSome("node", "runtime:22.0.0", runtime, release("11.5.2")))
          .isEqualTo(new Install.Saved("runtime:22.0.0", null));
        assertThatThrownBy(() -> Install.installSome("node", "runtime:22.0.0", runtime, release("11.5.1")))
          .hasMessageContaining("ERR_PNPM_CATALOG_VERSION_MISMATCH");
    }

    private static Map<String, String> hoistFoo(String range, String release) {
        Map<String, PreferredVersions.Selector> preferred = new LinkedHashMap<>();
        preferred.put("1.2.0", PreferredVersions.Selector.of("version"));
        preferred.put("2.0.0", PreferredVersions.Selector.of("version"));
        Map<String, Map<String, PreferredVersions.Selector>> allPreferred = Collections.singletonMap("foo", preferred);
        return ResolveDependencies.hoistPeers(true, allPreferred, Collections.emptyList(),
          Collections.singletonList(new java.util.AbstractMap.SimpleEntry<>("foo",
            new ResolveDependencies.MissingPeerInfo(range, false))), release(release));
    }

    /**
     * installing/deps-resolver hoistPeers.ts (11.12.0): a missing peer's range takes the highest preferred version
     * it satisfies, or itself; before, only an exact version is matched, and a range takes the highest preferred
     * version (peer-cycle-bump and small-peer-only-provider-fork record it).
     */
    @Test
    void hoistsAPeerInRangeFrom11120() {
        assertThat(hoistFoo("^1.0.0", "11.12.0")).containsEntry("foo", "1.2.0");
        assertThat(hoistFoo("^1.0.0", "11.11.0")).containsEntry("foo", "2.0.0");
        assertThat(hoistFoo("^3.0.0", "11.12.0")).containsEntry("foo", "^3.0.0");
        assertThat(hoistFoo("^3.0.0", "11.11.0")).containsEntry("foo", "2.0.0");
        assertThat(hoistFoo("2.0.0", "11.11.0")).containsEntry("foo", "2.0.0");
    }

    /**
     * installing/deps-resolver hoistPeers.ts (11.5.1, pnpm/pnpm#12075): a version only the lock holds is a candidate
     * for a missing optional peer. #9151 asks it through {@link ResolveDependencies#LOCKED_OPTIONAL_PEER_CANDIDATES},
     * which names the same releases.
     */
    @Test
    void countsLockedVersionsAsOptionalPeerCandidatesFrom1151() {
        for (String version : PnpmReleases.all()) {
            if (PnpmReleases.isProven(version)) {
                assertThat(release(version).countsLockedVersionsAsOptionalPeerCandidates()).as(version)
                  .isEqualTo(Main.releaseIn(version, ResolveDependencies.LOCKED_OPTIONAL_PEER_CANDIDATES));
            }
        }
        assertThat(release("11.5.1").countsLockedVersionsAsOptionalPeerCandidates()).isTrue();
        assertThat(release("11.5.0").countsLockedVersionsAsOptionalPeerCandidates()).isFalse();
    }
}
