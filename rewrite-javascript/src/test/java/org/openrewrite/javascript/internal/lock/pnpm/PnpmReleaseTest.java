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
import java.util.Set;

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
     * workspace/project-manifest-reader normalize (11.3.0): a dependency field of null is read; before, the manifest
     * writer throws. pnpm 11.2.2 exited with this error and 11.3.0 wrote the lock when run on it.
     */
    @Test
    void readsANullDependencyFieldFrom1130() {
        Map<String, @Nullable Object> manifest = obj("{'name':'a','dependencies':null}");
        Manifests.normalizedForTheWriter(manifest, release("11.3.0"));
        assertThatThrownBy(() -> Manifests.normalizedForTheWriter(manifest, release("11.2.2")))
          .isInstanceOfSatisfying(JsError.class, e -> {
              assertThat(e.getName()).isEqualTo("TypeError");
              assertThat(e.getMessage()).isEqualTo("Cannot convert undefined or null to object");
          });
    }

    // the registries of release's getConfig under env, the host's as the user's .npmrc
    private static Map<String, String> userRegistries(@Nullable Map<String, String> env, String release) {
        return PnpmOptions.load("", null, "registry=https://user.test/\n", null, env, release(release))
          .getRegistries();
    }

    /**
     * config/reader index.ts readEnvVar (11.0.6): pnpm_config_userconfig or pnpm_config_npmrc_auth_file names the
     * user .npmrc pnpm reads, which no input holds; before, pnpm reads ~/.npmrc, which the host's registries stand
     * for. An empty value is unset, and an environment the host does not know sets none.
     */
    @Test
    void readsTheUserNpmrcTheEnvironmentNamesFrom1106() {
        Map<String, String> env = Collections.singletonMap("pnpm_config_userconfig", "/x/.npmrc");
        assertThat(userRegistries(env, "11.0.5")).containsEntry("default", "https://user.test/");
        assertThatThrownBy(() -> userRegistries(env, "11.0.6"))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know the .npmrc /x/.npmrc pnpm reads as the user's, which " +
                      "pnpm_config_userconfig names in place of ~/.npmrc");
        assertThatThrownBy(() -> userRegistries(Collections.singletonMap("PNPM_CONFIG_NPMRC_AUTH_FILE", "/y/auth"),
          "11.0.6"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("which PNPM_CONFIG_NPMRC_AUTH_FILE names");
        assertThat(userRegistries(Collections.singletonMap("pnpm_config_userconfig", ""), "11.0.6"))
          .containsEntry("default", "https://user.test/");
        assertThat(userRegistries(null, "11.0.9")).containsEntry("default", "https://user.test/");
    }

    /**
     * config/reader index.ts readNpmEnvVar (11.0.9): NPM_CONFIG_USERCONFIG, which actions/setup-node sets, names the
     * user .npmrc pnpm reads; before, pnpm reads ~/.npmrc all the same.
     */
    @Test
    void readsTheUserNpmrcNpmConfigUserconfigNamesFrom1109() {
        Map<String, String> env = Collections.singletonMap("NPM_CONFIG_USERCONFIG", "/runner/temp/.npmrc");
        assertThat(userRegistries(env, "11.0.8")).containsEntry("default", "https://user.test/");
        assertThatThrownBy(() -> userRegistries(env, "11.0.9"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("the .npmrc /runner/temp/.npmrc pnpm reads as the user's, which " +
                                "NPM_CONFIG_USERCONFIG names");
        assertThatThrownBy(() -> userRegistries(Collections.singletonMap("npm_config_userconfig", "/z/.npmrc"),
          "11.0.9"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("which npm_config_userconfig names");
    }

    // pnpm-workspace.yaml as the writer release runs writes it with a key added
    private static String withAKeyAdded(String yaml, PnpmRelease release) {
        org.openrewrite.javascript.internal.lock.YamlDocument document =
          org.openrewrite.javascript.internal.lock.YamlDocument.parse((org.openrewrite.yaml.tree.Yaml.Documents)
            org.openrewrite.yaml.YamlParser.builder().build().parse(yaml).findFirst()
              .orElseThrow(IllegalStateException::new));
        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>((Map<String, @Nullable Object>) document.toJs());
        Map<String, @Nullable Object> original = new LinkedHashMap<>();
        for (String key : manifest.keySet()) {
            original.put(key, null);
        }
        manifest.put("d", "hoisted");
        new WorkspaceManifestWriter(11, release).write(document, original, manifest);
        return document.toString();
    }

    /**
     * yaml 2.8.4's stringifyNumber (11.0.7, 2.9.0's in 11.2.2): a negative float keeps the fraction digits it was
     * read with, and one written with an exponent keeps that form; before, yaml 2.8.3 drops the first's and pads the
     * second after its exponent. A range across 11.0.7 is asked only of a number the two write otherwise, which the
     * recipes then do not write.
     */
    @Test
    void padsTheFractionDigitsOfANegativeNumberFrom1107() {
        String yaml = "a: -1.50\nb: 0.00000010\nc: 1.50\n";
        assertThat(withAKeyAdded(yaml, release("11.0.7"))).isEqualTo("a: -1.50\nb: 1e-7\nc: 1.50\nd: hoisted\n");
        assertThat(withAKeyAdded(yaml, release("11.0.6")))
          .isEqualTo("a: -1.5\nb: 1e-7.00000000\nc: 1.50\nd: hoisted\n");
        PnpmRelease straddling = PnpmRelease.oneOf("11.0.6 || 11.0.7", Arrays.asList("11.0.6", "11.0.7"));
        assertThat(withAKeyAdded("c: 1.50\n", straddling)).isEqualTo("c: 1.50\nd: hoisted\n");
        assertThatThrownBy(() -> withAKeyAdded(yaml, straddling))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("one of 11.0.6 || 11.0.7, where the releases from 11.0.7 install it otherwise");
    }

    private static final String CODELOAD =
      "https://codeload.github.com/u/foo/tar.gz/98e8ff1da1a89f93d1397a24d7413ed15421c139";

    // the lock-only install adding wrappy 1.0.2 beside github:u/foo, locked without an integrity as pnpm 10 and pnpm
    // 11 before 11.0.7 lock it
    private static String addWrappyBesideAGitHostedTarball(String release) {
        String lock = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                      "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n    dependencies:\n      foo:\n" +
                      "        specifier: github:u/foo#98e8ff1da1a89f93d1397a24d7413ed15421c139\n" +
                      "        version: " + CODELOAD + "\n\npackages:\n\n  foo@" + CODELOAD + ":\n" +
                      "    resolution: {tarball: " + CODELOAD + "}\n    version: 1.0.0\n\nsnapshots:\n\n" +
                      "  foo@" + CODELOAD + ": {}\n";
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"foo\":\"github:u/foo#" +
                        "98e8ff1da1a89f93d1397a24d7413ed15421c139\",\"wrappy\":\"1.0.2\"}}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(NOW)
          .release(release(release))
          .build();
        return new PnpmLockRegenerator(inputs, WRAPPY_ONLY).writeLock();
    }

    /**
     * fetching/tarball-fetcher gitHostedTarballFetcher.ts and installing/package-requester (11.0.7): the git-hosted
     * tarball pnpm fetches for its manifest gives a resolution without an integrity the one fetched, which no input
     * holds (nor whether the store does, which gives none); before, it is written without one.
     */
    @Test
    void pinsTheIntegrityOfAFetchedGitHostedTarballFrom1107() {
        assertThat(addWrappyBesideAGitHostedTarball("11.0.6"))
          .contains("  foo@" + CODELOAD + ":\n    resolution: {tarball: " + CODELOAD + "}\n");
        assertThatThrownBy(() -> addWrappyBesideAGitHostedTarball("11.0.7"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("the integrity of the git-hosted tarball " + CODELOAD + ", which pnpm fetches");
    }

    // a registry of one packument, which records what it is asked for
    private static final class OnePackument implements RegistryMetadata {
        final String name;
        final String packument;
        final List<String> fetched = new ArrayList<>();

        OnePackument(String name, String packument) {
            this.name = name;
            this.packument = packument;
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            fetched.add(registry + name + (fullMetadata ? " (full)" : ""));
            return this.name.equals(name) ? packument : fail("fetched " + registry + name);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return fail("fetched the attestations of " + name + "@" + version);
        }

        @Override
        public @Nullable String document(String url) {
            return fail("fetched " + url);
        }
    }

    // the message of the ERR_PNPM_NO_MATURE_MATCHING_VERSION release's resolver throws under minimumReleaseAgeStrict
    // for foo@^1.0.0, whose one version was released at released, under a cutoff 30 days back
    private static String immatureMessage(String released, PnpmRelease release) {
        RegistryMetadata registry = new OnePackument("foo", "{\"name\":\"foo\",\"dist-tags\":{\"latest\":\"1.0.0\"}," +
                                                            "\"versions\":{\"1.0.0\":{\"name\":\"foo\",\"version\":" +
                                                            "\"1.0.0\",\"dist\":{\"integrity\":\"sha512-x\"," +
                                                            "\"tarball\":\"" + REGISTRY + "foo/-/foo-1.0.0.tgz\"}}}," +
                                                            "\"time\":{\"1.0.0\":\"" + released + "\"}}");
        NpmResolver resolver = new NpmResolver(registry, Collections.singletonMap("default", REGISTRY),
          Collections.emptyMap(), null, false, false, NOW, release, true);
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.publishedBy = NOW.minusSeconds(30L * 24 * 60 * 60);
        try {
            resolver.resolveNpm(new ResolveDependencies.WantedDependency("foo", "^1.0.0", false, false), opts, null);
        } catch (PnpmError e) {
            assertThat(e.getCode()).isEqualTo("ERR_PNPM_NO_MATURE_MATCHING_VERSION");
            return e.getMessage();
        }
        return fail("resolved foo");
    }

    /**
     * resolving/npm-resolver index.ts formatTimeAgo (11.1.0, pnpm/pnpm#11532): the age of an immature version is told
     * in its largest whole unit; before, in minutes under 90 of them, hours under 48 and days after. A range across
     * 11.1.0 is asked only of an age the two tell otherwise.
     */
    @Test
    void tellsAReleaseAgeInItsLargestWholeUnitFrom1110() {
        String thirtyHours = "2026-10-10T18:00:00.000Z";
        assertThat(immatureMessage(thirtyHours, release("11.1.0")))
          .isEqualTo("Version 1.0.0 (released 1 day ago) of foo does not meet the minimumReleaseAge constraint");
        assertThat(immatureMessage(thirtyHours, release("11.0.9")))
          .isEqualTo("Version 1.0.0 (released 30 hours ago) of foo does not meet the minimumReleaseAge constraint");
        String seventyFiveMinutes = "2026-10-11T22:45:00.000Z";
        assertThat(immatureMessage(seventyFiveMinutes, release("11.1.0"))).contains("(released 1 hour ago)");
        assertThat(immatureMessage(seventyFiveMinutes, release("11.0.9"))).contains("(released 75 minutes ago)");
        String thirtySeconds = "2026-10-11T23:59:30.000Z";
        assertThat(immatureMessage(thirtySeconds, release("11.1.0"))).contains("(released a few seconds ago)");
        assertThat(immatureMessage(thirtySeconds, release("11.0.9"))).contains("(released just now)");
        PnpmRelease straddling = PnpmRelease.oneOf("11.0.9 || 11.1.0", Arrays.asList("11.0.9", "11.1.0"));
        assertThat(immatureMessage("2026-10-09T00:00:00.000Z", straddling)).contains("(released 3 days ago)");
        assertThatThrownBy(() -> immatureMessage(seventyFiveMinutes, straddling))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("where the releases from 11.1.0 install it otherwise");
    }

    /**
     * resolving/npm-resolver parseBareSpecifier.ts (11.1.0, pnpm/pnpm#11324): foo: npm:^1.0.0 is a range of foo;
     * before, it names a package "^1.0.0" at the default tag, which no registry has. A range across 11.1.0 is asked
     * only of a range after npm:.
     */
    @Test
    void takesTheAliasAsTheNameOfAnNpmRangeFrom1110() {
        // replaceVersionInBareSpecifier of 11.0.9 takes the range for a name
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("npm:^1.0.0", "1.2.3", Collections.emptyList(),
          release("11.1.0"))).isEqualTo("npm:1.2.3");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("npm:^1.0.0", "1.2.3", Collections.emptyList(),
          release("11.0.9"))).isEqualTo("npm:^1.0.0@1.2.3");
        PickPackageFromMeta.RegistryPackageSpec later = PickPackageFromMeta.parseBareSpecifier("npm:^1.0.0", "foo",
          "latest", REGISTRY, release("11.1.0"));
        assertThat(later).isNotNull();
        assertThat(later.getName()).isEqualTo("foo");
        assertThat(later.getType()).isEqualTo("range");
        PickPackageFromMeta.RegistryPackageSpec earlier = PickPackageFromMeta.parseBareSpecifier("npm:^1.0.0", "foo",
          "latest", REGISTRY, release("11.0.9"));
        assertThat(earlier).isEqualTo(new PickPackageFromMeta.RegistryPackageSpec("tag", "^1.0.0", "latest", null));
        PnpmRelease straddling = PnpmRelease.oneOf("11.0.9 || 11.1.0", Arrays.asList("11.0.9", "11.1.0"));
        PickPackageFromMeta.RegistryPackageSpec named = PickPackageFromMeta.parseBareSpecifier("npm:bar@^1.0.0",
          "foo", "latest", REGISTRY, straddling);
        assertThat(named).isNotNull();
        assertThat(named.getName()).isEqualTo("bar");
        assertThatThrownBy(() -> PickPackageFromMeta.parseBareSpecifier("npm:^1.0.0", "foo", "latest", REGISTRY,
          straddling))
          .isInstanceOf(CannotKnow.class);
    }

    private static final String STD_PATH = "{\"name\":\"@jsr/std__path\",\"dist-tags\":{\"latest\":\"1.1.0\"}," +
                                           "\"versions\":{\"1.0.0\":{\"name\":\"@jsr/std__path\",\"version\":" +
                                           "\"1.0.0\",\"dist\":{\"integrity\":\"sha512-a\",\"tarball\":" +
                                           "\"https://npm.jsr.io/~/11/@jsr/std__path/1.0.0.tgz\"}},\"1.1.0\":{" +
                                           "\"name\":\"@jsr/std__path\",\"version\":\"1.1.0\",\"dist\":{" +
                                           "\"integrity\":\"sha512-b\",\"tarball\":\"https://npm.jsr.io/~/11/" +
                                           "@jsr/std__path/1.1.0.tgz\"}}},\"time\":{\"1.0.0\":" +
                                           "\"2026-01-01T00:00:00.000Z\",\"1.1.0\":\"2026-10-11T23:00:00.000Z\"}}";

    // the id release's jsr resolver picks for @std/path: jsr:^1.0.0 under a cutoff (null for none) that 1.1.0, an
    // hour old, does not meet, and @jsr/std__path in minimumReleaseAgeExclude; what it fetched last in fetched
    private static String pickStdPath(PnpmRelease release, @Nullable Instant cutoff, boolean optional,
                                      List<String> fetched) {
        OnePackument registry = new OnePackument("@jsr/std__path", STD_PATH);
        Map<String, String> registries = new LinkedHashMap<>();
        registries.put("default", REGISTRY);
        registries.put("@jsr", "https://npm.jsr.io/");
        NpmResolver resolver = new NpmResolver(registry, registries, Collections.emptyMap(), null, false, false, NOW,
          release, false);
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.publishedBy = cutoff;
        opts.publishedByExclude = name -> "@jsr/std__path".equals(name) ? Boolean.TRUE : null;
        NpmResolver.ResolveResult picked = resolver.resolveJsr(new ResolveDependencies.WantedDependency("@std/path",
          "jsr:^1.0.0", false, optional), opts, null);
        fetched.addAll(registry.fetched);
        assertThat(picked).isNotNull();
        return picked.id;
    }

    /**
     * resolving/npm-resolver index.ts pickFromSimpleRegistry (11.1.0, pnpm/pnpm#11324): a jsr package in
     * minimumReleaseAgeExclude is picked without the cutoff, and an optional one from the full packument; before, the
     * cutoff holds it to its newest mature version, and the abbreviated packument is fetched. A range across 11.1.0
     * is asked only where either reaches the pick.
     */
    @Test
    void picksAJsrPackageWithItsExclusionsAndOptionalityFrom1110() {
        Instant aDayBack = NOW.minusSeconds(24 * 60 * 60);
        List<String> fetched = new ArrayList<>();
        assertThat(pickStdPath(release("11.1.0"), aDayBack, false, fetched)).isEqualTo("@jsr/std__path@1.1.0");
        assertThat(pickStdPath(release("11.0.9"), aDayBack, false, fetched)).isEqualTo("@jsr/std__path@1.0.0");
        fetched.clear();
        pickStdPath(release("11.1.0"), null, true, fetched);
        pickStdPath(release("11.0.9"), null, true, fetched);
        assertThat(fetched).containsExactly("https://npm.jsr.io/@jsr/std__path (full)",
          "https://npm.jsr.io/@jsr/std__path");
        PnpmRelease straddling = PnpmRelease.oneOf("11.0.9 || 11.1.0", Arrays.asList("11.0.9", "11.1.0"));
        assertThat(pickStdPath(straddling, null, false, fetched)).isEqualTo("@jsr/std__path@1.1.0");
        assertThatThrownBy(() -> pickStdPath(straddling, aDayBack, false, new ArrayList<>()))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> pickStdPath(straddling, null, true, new ArrayList<>()))
          .isInstanceOf(CannotKnow.class);
    }

    /**
     * installing/deps-installer install/index.ts (11.4.0): updateChecksums resolves in full; before, the lock is up
     * to date.
     */
    @Test
    void resolvesInFullToUpdateChecksumsFrom1140() {
        assertThat(install("11.4.0", "", "updateChecksums: true\n", WRAPPY_ONLY).getResolution())
          .isEqualTo("updateChecksums");
        assertThat(install("11.3.0", "", "updateChecksums: true\n", WRAPPY_ONLY).getWritten()).isEqualTo(WRAPPY_LOCK);
    }

    /**
     * installing/deps-installer install/index.ts (11.5.2): pnprServer replaces agent, and from 11.1.3 neither runs
     * under trustPolicy no-downgrade; before 11.1.3 the agent resolves.
     */
    @Test
    void resolvesThroughAPnprServerFrom1152() {
        String pnpr = "pnprServer: https://pnpr.test/\ntrustPolicy: no-downgrade\n";
        assertThatThrownBy(() -> install("11.5.2", "", pnpr, WRAPPY_ONLY))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_TRUST_POLICY_INCOMPATIBLE_WITH_PNPR"));
        // 11.5.1 reads no pnprServer: the trust policy is checked locally
        assertThatThrownBy(() -> install("11.5.1", "", pnpr, WRAPPY_ONLY))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getMessage()).contains("The metadata of wrappy is missing the \"time\" field"));
        String agent = "agent: https://agent.test/\ntrustPolicy: no-downgrade\n";
        assertThatThrownBy(() -> install("11.5.1", "", agent, WRAPPY_ONLY))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_TRUST_POLICY_INCOMPATIBLE_WITH_AGENT"));
        assertThatThrownBy(() -> install("11.1.2", "", agent, WRAPPY_ONLY))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("what the pnpm agent at https://agent.test/ resolves");
    }

    /**
     * config/reader loadNpmrcFiles.ts substituteEnv (11.1.3): a placeholder the environment does not set is dropped;
     * before, envReplace failed and the value was kept as written.
     */
    @Test
    void dropsAnUnresolvedNpmrcPlaceholderFrom1113() {
        String npmrc = "@org:registry=https://${UNSET}.test/\n";
        assertThat(options("", npmrc, null, Collections.emptyMap(), "11.1.3").getRegistries())
          .containsEntry("@org", "https://.test/");
        assertThat(options("", npmrc, null, Collections.emptyMap(), "11.1.2").getRegistries())
          .containsEntry("@org", "https://${unset}.test/");
    }

    /**
     * config/reader parseCreds.ts decodeBase64Credential (11.1.3): an over-padded _auth decodes with its padding
     * mended; before, Node's atob throws.
     */
    @Test
    void decodesACredentialForgivingItsPaddingFrom1113() {
        // "user:pass" with one = too many
        String npmrc = "//registry.npmjs.org/:_auth=dXNlcjpwYXNz=\n";
        options("", npmrc, null, Collections.emptyMap(), "11.1.3");
        assertThatThrownBy(() -> options("", npmrc, null, Collections.emptyMap(), "11.1.2"))
          .isInstanceOfSatisfying(JsError.class, e -> {
              assertThat(e.getName()).isEqualTo("InvalidCharacterError");
              assertThat(e.getMessage()).isEqualTo("Invalid character");
          });
        assertThatThrownBy(() -> options("", "//registry.npmjs.org/:_auth=%%%\n", null, Collections.emptyMap(),
          "11.1.3"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_INVALID_BASE64"));
        assertThat(PnpmOptions.atobError("abcde")).isEqualTo("The string to be decoded is not correctly encoded.");
    }

    /**
     * config/reader loadNpmrcFiles.ts readUrlScopedEnvConfig (11.6.0): an npm_config_//… variable sets its key after
     * the project's .npmrc; before, nothing. "foo" has no user:password separator.
     */
    @Test
    void readsUrlScopedConfigFromTheEnvironmentFrom1160() {
        Map<String, String> env = Collections.singletonMap("npm_config_//registry.npmjs.org/:_auth", "Zm9v");
        assertThatThrownBy(() -> options("", null, null, env, "11.6.0"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_MISSING_SEPARATOR"));
        options("", null, null, env, "11.5.3");
        String bad = "//registry.npmjs.org/:_auth=Zm9v\n";
        Map<String, String> good = Collections.singletonMap("pnpm_config_//registry.npmjs.org/:_auth",
          "dXNlcjpwYXNz");
        options("", bad, null, good, "11.6.0");
        assertThatThrownBy(() -> options("", bad, null, good, "11.5.3"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_MISSING_SEPARATOR"));
    }

    /**
     * config/package-is-installable inferPlatformFromPackageName.ts (11.6.0): an optional package's os and cpu come
     * from its name where it declares none; before, it is any platform's.
     */
    @Test
    void infersAnOptionalPackagesPlatformFromItsNameFrom1160() {
        Host linux = Host.builder().nodeVersion("v24.21.0").os("linux").cpu("x64").build();
        Map<String, @Nullable Object> win = obj("{'name':'foo-win32-x64','version':'1.0.0'}");
        assertThat(PackageIsInstallable.packageIsInstallable("foo-win32-x64@1.0.0", win, false, null, true, null,
          linux, release("11.6.0"))).isFalse();
        assertThat(PackageIsInstallable.packageIsInstallable("foo-win32-x64@1.0.0", win, false, null, true, null,
          linux, release("11.5.3"))).isTrue();
        assertThat(PackageIsInstallable.packageIsInstallable("foo-win32-x64@1.0.0", win, false, null, false, null,
          linux, release("11.6.0"))).isTrue();
        Map<String, @Nullable Object> arm = obj("{'name':'@scope/foo-arm64','version':'1.0.0'}");
        assertThat(PackageIsInstallable.packageIsInstallable("@scope/foo-arm64@1.0.0", arm, false, null, true, null,
          linux, release("11.6.0"))).isTrue();
        assertThat(PackageIsInstallable.inferPlatformFromPackageName("@esbuild/linux-arm64-musl"))
          .isEqualTo(obj("{'os':['linux'],'cpu':['arm64'],'libc':['musl']}"));
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
     * resolving/npm-resolver pickPackage.ts (11.22.0): under a release-age cutoff a resolution that picks the lowest
     * version picks the lowest mature one; before, the highest mature one (small-settings-resolution-mode-lowest-direct
     * records it).
     */
    @Test
    void picksTheLowestMatureVersionFrom11220() {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("^1.0.0", "foo",
          "latest", REGISTRY);
        Map<String, @Nullable Object> meta = fooMeta(null, true);
        PickPackage.PickerOptions lowest = picker("2026-10-12T00:00:00Z", false, "11.22.0").withPickLowestVersion(true);
        assertThat(PickPackage.pickRespectingMinReleaseAge(lowest, spec, meta)).containsEntry("version", "1.0.0");
        assertThat(PickPackage.pickRespectingMinReleaseAge(lowest.withRelease(release("11.21.0")), spec, meta))
          .containsEntry("version", "1.1.0");
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

    private static boolean nodeSatisfies(String node, String wanted, String release) {
        return PackageIsInstallable.checkEngine("p@1.0.0", obj("{'node':'" + wanted + "'}"), node, null,
          release(release)) == null;
    }

    /**
     * The node-semver each release bundles (its bundle's semver 7.7.4, 7.8.0, 7.8.1, 7.8.4, 7.8.5): 7.8.1 (11.3.0)
     * strips build metadata from a whole range, so a hyphen range's lower bound takes the -0 of includePrerelease;
     * 7.8.4 (11.7.0) takes 1.x.3 for no range and gives ^0.2.3 no -0; 7.8.5 (11.9.0) gives ~1.2 its -0. A range of
     * releases asks only where their node-semvers answer otherwise.
     */
    @Test
    void bundlesTheNodeSemverOfItsRelease() {
        assertThat(nodeSatisfies("1.2.3-rc.1", "1.2.3+build - 2.0.0", "11.3.0")).isTrue();
        assertThat(nodeSatisfies("1.2.3-rc.1", "1.2.3+build - 2.0.0", "11.2.2")).isFalse();
        assertThat(nodeSatisfies("0.2.3-rc.1", "^0.2.3", "11.6.0")).isTrue();
        assertThat(nodeSatisfies("0.2.3-rc.1", "^0.2.3", "11.7.0")).isFalse();
        assertThat(nodeSatisfies("1.2.0-rc.1", "~1.2", "11.9.0")).isTrue();
        assertThat(nodeSatisfies("1.2.0-rc.1", "~1.2", "11.8.0")).isFalse();
        assertThat(PickPackageFromMeta.parseBareSpecifier("1.x.3", "foo", "latest", REGISTRY, release("11.6.0"))
          .getType()).isEqualTo("range");
        assertThat(PickPackageFromMeta.parseBareSpecifier("1.x.3", "foo", "latest", REGISTRY, release("11.7.0"))
          .getType()).isEqualTo("tag");
        PnpmRelease straddling = PnpmRelease.oneOf("11.2.2 - 11.3.0", Arrays.asList("11.2.2", "11.3.0"));
        assertThat(nodeSatisfies("1.2.4", "1.2.3+build - 2.0.0", "11.2.2")).isTrue();
        assertThat(PackageIsInstallable.checkEngine("p@1.0.0", obj("{'node':'1.2.3+build - 2.0.0'}"), "1.2.4",
          null, straddling)).isNull();
        assertThatThrownBy(() -> PackageIsInstallable.checkEngine("p@1.0.0",
          obj("{'node':'1.2.3+build - 2.0.0'}"), "1.2.3-rc.1", null, straddling))
          .isInstanceOf(CannotKnow.class);
    }

    /**
     * installing/deps-installer verifyLockfileResolutions.ts (11.7.0): a snapshot's dependency alias that is no
     * package name fails the verification; before, it passes.
     */
    @Test
    void rejectsAnInvalidDependencyAliasInTheLockFrom1170() {
        String lock = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                      "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n    dependencies:\n      foo:\n" +
                      "        specifier: 1.0.0\n        version: 1.0.0\n\npackages:\n\n  foo@1.0.0:\n" +
                      "    resolution: {integrity: sha512-x}\n\n  bar@1.0.0:\n    resolution: {integrity: sha512-y}\n\n" +
                      "snapshots:\n\n  foo@1.0.0:\n    dependencies:\n      .bin: bar@1.0.0\n\n  bar@1.0.0: {}\n";
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(
          LockfileReader.read(lock, release("11.7.0")).getLockfile(), Collections.emptyList(), release("11.7.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_DEPENDENCY_NAME");
              assertThat(e.getMessage()).isEqualTo("The lockfile contains 1 dependency alias that are not valid " +
                                                   "package names:\n  \".bin\"");
          });
        VerifyLockfileResolutions.verifyLockfileResolutions(LockfileReader.read(lock, release("11.6.0"))
          .getLockfile(), Collections.emptyList(), release("11.6.0"));
    }

    /**
     * lockfile/utils toLockfileResolution.ts (11.7.0): a resolution kept with its tarball keeps its path; before, not.
     */
    @Test
    void keepsATarballResolutionsPathFrom1170() {
        String codeload = "https://codeload.github.com/o/r/tar.gz/abc";
        Map<String, @Nullable Object> resolution = obj("{'integrity':'sha512-x','tarball':'" + codeload +
                                                       "','path':'/x'}");
        assertThat(json(LockfileUtils.toLockfileResolution("r", "1.0.0", resolution, REGISTRY, false,
          release("11.7.0")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + codeload +
                                         "','gitHosted':true,'path':'/x'}");
        assertThat(json(LockfileUtils.toLockfileResolution("r", "1.0.0", resolution, REGISTRY, false,
          release("11.6.0")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + codeload + "','gitHosted':true}");
    }

    /**
     * config/reader getNetworkConfigs.ts (11.7.0): a username after a colon and a password after the URL of the
     * same scope are checked together; before, apart.
     */
    @Test
    void groupsCredentialsByRegistryAndScopeFrom1170() {
        String npmrc = "//r.test/:@s:username=u\n//r.test/@s/:_password=%%%\n";
        assertThatThrownBy(() -> options("", npmrc, null, Collections.emptyMap(), "11.7.0"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_INVALID_BASE64"));
        options("", npmrc, null, Collections.emptyMap(), "11.6.0");
    }

    /**
     * store/connection-manager and store/index ReadOnlyStoreIndex (11.7.0): frozenStore opens the store read-only,
     * which a Node.js before 22.15.0 cannot; whether the store is seeded no input says. Before, no such setting.
     */
    @Test
    void opensAFrozenStoreReadOnlyFrom1170() {
        PnpmOptions frozen = options("frozenStore: true\n", null, null, Collections.emptyMap(), "11.7.0");
        assertThatThrownBy(() -> new CreateNewStoreController(WRAPPY_ONLY, NOW, "v20.19.0")
          .createStoreController(frozen))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_FROZEN_STORE_UNSUPPORTED_NODE");
              assertThat(e.getMessage()).endsWith("but the current version is 20.19.0. Upgrade Node.js, or run " +
                                                  "without frozenStore.");
          });
        assertThatThrownBy(() -> new CreateNewStoreController(WRAPPY_ONLY, NOW, "v24.21.0")
          .createStoreController(frozen))
          .isInstanceOf(CannotKnow.class);
        new CreateNewStoreController(WRAPPY_ONLY, NOW, "v20.19.0").createStoreController(
          options("frozenStore: true\n", null, null, Collections.emptyMap(), "11.6.0"));
    }

    /**
     * hooks/read-package-hook createReadPackageHook.ts (11.7.0): the compatibility database's two entries for
     * gatsby-core-utils@&lt;2.14.0-next.1 are merged; before, the last one wins.
     */
    @Test
    void mergesThePackageExtensionsOfASelectorFrom1170() {
        ReadPackageHook merged = ReadPackageHook.createReadPackageHook(false, "", Collections.emptyList(), null,
          null, release("11.7.0"));
        ReadPackageHook last = ReadPackageHook.createReadPackageHook(false, "", Collections.emptyList(), null,
          null, release("11.6.0"));
        assertThat(merged).isNotNull();
        assertThat(last).isNotNull();
        assertThat(merged.apply(obj("{'name':'gatsby-core-utils','version':'2.0.0'}"), null).get("dependencies"))
          .isEqualTo(obj("{'got':'8.3.2','@babel/runtime':'^7.14.8'}"));
        assertThat(last.apply(obj("{'name':'gatsby-core-utils','version':'2.0.0'}"), null).get("dependencies"))
          .isEqualTo(obj("{'got':'8.3.2'}"));
        assertThat(merged.apply(obj("{'name':'gatsby-core-utils','version':'3.0.0'}"), null).get("dependencies"))
          .isNull();
    }

    /**
     * installing/deps-resolver resolveDependencies.ts getDepsToResolve (11.8.0): a child the lock pins to wrappy
     * 1.0.1, which a direct dependency's 1.0.2 supersedes in the child's range "1", is resolved again to 1.0.2;
     * before, the pin is kept. The recorded scenario small-bump-direct-ranged-dependent shows both.
     */
    @Test
    void movesAStalePinUpToADirectVersionFrom1180() {
        String lock = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                      "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n\npackages:\n\n" +
                      "  wrappy@1.0.1:\n    resolution: {integrity: sha512-x}\n\nsnapshots:\n\n  wrappy@1.0.1: {}\n";
        Map<String, PreferredVersions.Selector> wrappy = new LinkedHashMap<>();
        wrappy.put("1.0.2", PreferredVersions.Selector.of("version", PreferredVersions.DIRECT_DEP_SELECTOR_WEIGHT));
        wrappy.put("1.0.1", PreferredVersions.Selector.of("version", 1_000_000));
        PreferredVersions.ByName preferred = PreferredVersions.ByName.of(new LinkedHashMap<>(
          Collections.singletonMap("wrappy", wrappy)));
        for (String release : Arrays.asList("11.8.0", "11.7.0")) {
            LockfileObject lockfile = LockfileReader.read(lock, release(release)).getLockfile();
            ResolveDependencies.ExtendedWantedDependency dep = ResolveDependencies.getDepsToResolve(
              Collections.singletonList(new ResolveDependencies.WantedDependency("wrappy", "1", false, false)),
              lockfile, Collections.singletonMap("default", REGISTRY), null, false,
              Collections.singletonMap("wrappy", "1.0.1"), preferred, release(release)).get(0);
            if ("11.8.0".equals(release)) {
                assertThat(dep.getPreferredVersion()).isEqualTo("1.0.2");
                assertThat(dep.getInfoFromLockfile()).isNull();
            } else {
                assertThat(dep.getPreferredVersion()).isNull();
                assertThat(dep.getInfoFromLockfile()).isNotNull();
            }
        }
    }

    /**
     * resolving/tarball-url isCanonicalRegistryTarballUrl (11.9.0): a scoped tarball URL escaped as %2F is the
     * registry's own and left out of the lock; before, it is kept.
     */
    @Test
    void unescapesAnyCaseSlashInATarballUrlFrom1190() {
        Map<String, @Nullable Object> resolution = obj("{'integrity':'sha512-x','tarball':" +
                                                       "'https://registry.npmjs.org/@s%2Fp/-/p-1.0.0.tgz'}");
        assertThat(json(LockfileUtils.toLockfileResolution("@s/p", "1.0.0", resolution, REGISTRY, false,
          release("11.9.0")))).isEqualTo("{'integrity':'sha512-x'}");
        assertThat(json(LockfileUtils.toLockfileResolution("@s/p", "1.0.0", resolution, REGISTRY, false,
          release("11.8.0")))).isEqualTo("{'integrity':'sha512-x','tarball':" +
                                         "'https://registry.npmjs.org/@s%2Fp/-/p-1.0.0.tgz'}");
    }

    /**
     * config/version-policy evaluateVersionPolicy (11.9.0): the exact versions of every rule that matches are merged;
     * before, the first rule that matches decides.
     */
    @Test
    void mergesExactVersionsAcrossPolicyRulesFrom1190() {
        List<String> rules = Arrays.asList("foo@1.0.0", "foo@2.0.0");
        assertThat(PackageVersionPolicy.isExcluded(PackageVersionPolicy.createPackageVersionPolicy(rules,
          release("11.9.0")), "foo", "2.0.0")).isTrue();
        assertThat(PackageVersionPolicy.isExcluded(PackageVersionPolicy.createPackageVersionPolicy(rules,
          release("11.8.0")), "foo", "2.0.0")).isFalse();
    }

    /**
     * resolving/npm-resolver pickPackage.ts getPkgMetaCacheKey (11.9.0): a name fetched from a second registry is
     * fetched again; before, the first registry's packument is reused.
     */
    @Test
    void keysThePackumentCacheByRegistryFrom1190() {
        List<String> fetched = new ArrayList<>();
        RegistryMetadata registries = new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean fullMetadata) {
                fetched.add(registry + name);
                return WRAPPY;
            }

            @Override
            public @Nullable String attestations(String registry, String name, String version) {
                return null;
            }

            @Override
            public @Nullable String document(String url) {
                return null;
            }
        };
        for (String release : Arrays.asList("11.9.0", "11.8.0")) {
            Map<String, Map<String, @Nullable Object>> metaCache = new LinkedHashMap<>();
            PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("1.0.2", "wrappy",
              "latest", REGISTRY, release(release));
            PickPackage.pickPackage(metaCache, registries, spec, picker("2026-01-01T00:00:00Z", false, release),
              REGISTRY, false, false);
            PickPackage.pickPackage(metaCache, registries, spec, picker("2026-01-01T00:00:00Z", false, release),
              "https://mirror.test/", false, false);
        }
        assertThat(fetched).containsExactly(REGISTRY + "wrappy", "https://mirror.test/wrappy", REGISTRY + "wrappy");
    }

    /**
     * lockfile/preferred-versions (11.10.0): a lock package named constructor is a package as any other; before,
     * pnpm finds Object.prototype.constructor and throws.
     */
    @Test
    void seedsPreferredVersionsWithoutAPrototypeFrom11100() {
        Map<String, PackageSnapshot> snapshots = new LinkedHashMap<>();
        snapshots.put("constructor@1.0.0", new PackageSnapshot());
        assertThat(PreferredVersions.getPreferredVersionsFromLockfileAndManifests(snapshots,
          Collections.emptyList(), release("11.10.0"))).containsKey("constructor");
        assertThatThrownBy(() -> PreferredVersions.getPreferredVersionsFromLockfileAndManifests(snapshots,
          Collections.emptyList(), release("11.9.0")))
          .isInstanceOfSatisfying(JsError.class, e -> {
              assertThat(e.getName()).isEqualTo("TypeError");
              assertThat(e.getMessage()).isEqualTo("uniqueNameVersions[name].add is not a function");
          });
    }

    /**
     * installing/env-installer verifyEnvLockfile.ts (11.8.0): a config dependency's version must be exact; before,
     * nothing checks it.
     */
    @Test
    void verifiesTheEnvDocumentsConfigDependenciesFrom1180() {
        String env = "---\nlockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    configDependencies:\n      foo:\n" +
                     "        specifier: ^1.0.0\n        version: ^1.0.0\n\npackages:\n\n  foo@^1.0.0:\n" +
                     "    resolution: {integrity: sha512-x}\n\nsnapshots:\n\n  foo@^1.0.0: {}\n\n---\n" + EMPTY_LOCK;
        EnvLockfile envLockfile = EnvLockfile.readEnvLockfile(env);
        assertThatThrownBy(() -> EnvInstaller.verifyEnvLockfile(envLockfile, release("11.8.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_CONFIG_DEP_VERSION");
              assertThat(e.getMessage()).isEqualTo("The config dependency \"foo\" has an invalid version \"^1.0.0\"");
          });
        EnvInstaller.verifyEnvLockfile(envLockfile, release("11.7.0"));
    }

    /**
     * installing/commands install.ts dryRunInstall (11.8.0): a dryRun setting resolves in full and writes no lock;
     * before, pnpm install does not read it and finds the lock up to date.
     */
    @Test
    void stopsTheLockWriteOnADryRunFrom1180() {
        assertThat(install("11.8.0", "", "dryRun: true\n", WRAPPY_ONLY).getResolution()).isEqualTo("a dry run");
        assertThat(install("11.7.0", "", "dryRun: true\n", WRAPPY_ONLY).getWritten()).isEqualTo(WRAPPY_LOCK);
    }

    /**
     * config/reader index.ts parsePackageManager (11.8.0): @pnpm/exe@11.2.2 names @pnpm/exe; before, the split at
     * each @ names "" and no version.
     */
    @Test
    void splitsThePackageManagerAtItsVersionsAtFrom1180() {
        Map<String, @Nullable Object> manifest = obj("{'packageManager':'@pnpm/exe@11.2.2'}");
        PnpmOptions.WantedPackageManager after = PnpmOptions.getWantedPackageManager(manifest, release("11.8.0"));
        PnpmOptions.WantedPackageManager before = PnpmOptions.getWantedPackageManager(manifest, release("11.7.0"));
        assertThat(after).isNotNull();
        assertThat(before).isNotNull();
        assertThat(after.getName()).isEqualTo("@pnpm/exe");
        assertThat(after.getVersion()).isEqualTo("11.2.2");
        assertThat(before.getName()).isEqualTo("");
    }

    /**
     * resolving/npm-resolver whichVersionIsPinned.ts (11.8.0): a catalog: specifier pins nothing; before, a catalog
     * name that reads as a major pins the major.
     */
    @Test
    void takesNoPinFromACatalogSpecifierFrom1180() {
        assertThat(NpmResolver.whichVersionIsPinned("catalog:react18", release("11.8.0"))).isNull();
        assertThat(NpmResolver.whichVersionIsPinned("catalog:react18", release("11.7.0"))).isEqualTo("major");
        assertThat(NpmResolver.whichVersionIsPinned("^1.0.0", release("11.8.0"))).isEqualTo("major");
    }

    /**
     * engine/runtime/node-resolver createNodeRuntimeVersionSpec (11.8.0): runtime:~22 is saved as ~22.0.0; before,
     * as ^22.0.0.
     */
    @Test
    void keepsARuntimeSpecifiersRangeOperatorFrom1180() {
        assertThat(nodeRuntime("~22", null, "11.8.0").normalizedBareSpecifier).isEqualTo("runtime:~22.0.0");
        assertThat(nodeRuntime("~22", null, "11.7.0").normalizedBareSpecifier).isEqualTo("runtime:^22.0.0");
        assertThat(nodeRuntime("22", null, "11.8.0").normalizedBareSpecifier).isEqualTo("runtime:22.0.0");
    }

    /**
     * lockfile/utils toLockfileResolution.ts (11.5.3): a tarball URL is git-hosted in any case; before, only in
     * lower case, so that a mixed-case codeload URL is kept without the flag.
     */
    @Test
    void matchesGitHostedTarballUrlsInAnyCaseFrom1153() {
        String codeload = "https://CodeLoad.GitHub.com/u/r/tar.gz/abc";
        Map<String, @Nullable Object> resolution = obj("{'integrity':'sha512-x','tarball':'" + codeload + "'}");
        assertThat(json(LockfileUtils.toLockfileResolution("r", "1.0.0", resolution, REGISTRY, false,
          release("11.5.3")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + codeload + "','gitHosted':true}");
        assertThat(json(LockfileUtils.toLockfileResolution("r", "1.0.0", resolution, REGISTRY, false,
          release("11.5.2")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + codeload + "'}");
    }

    private static DependenciesGraphNode graphNode(String depPath, String... children) {
        DependenciesGraphNode node = new DependenciesGraphNode(depPath, peerPkg("foo", "1.0.0"));
        for (int i = 0; i < children.length; i += 2) {
            node.getChildren().put(children[i], children[i + 1]);
            node.getResolvedPeerNames().add(children[i]);
        }
        return node;
    }

    /**
     * installing/deps-resolver resolvePeers.ts deduplicateDepPaths (11.5.3): two variants with as many dependencies
     * are taken in the order of their dep paths, so the greater one absorbs a variant both include; before, in the
     * order they were made, the last made one.
     */
    @Test
    void breaksPeerDedupeTiesByDepPathFrom1153() {
        String a = "foo@1.0.0(peer@1.0.0)";
        String b = "foo@1.0.0(other@2.0.0)(peer@1.0.0)";
        String c = "foo@1.0.0(other@1.0.0)(peer@1.0.0)";
        Map<String, DependenciesGraphNode> graph = new LinkedHashMap<>();
        graph.put(a, graphNode(a, "peer", "peer@1.0.0"));
        graph.put(b, graphNode(b, "peer", "peer@1.0.0", "other", "other@2.0.0"));
        graph.put(c, graphNode(c, "peer", "peer@1.0.0", "other", "other@1.0.0"));
        List<Set<String>> duplicates = Collections.singletonList(new LinkedHashSet<>(Arrays.asList(a, b, c)));
        assertThat(ResolvePeers.deduplicateAll(graph, duplicates, release("11.5.3")))
          .isEqualTo(Collections.singletonMap(a, b));
        assertThat(ResolvePeers.deduplicateAll(graph, duplicates, release("11.5.2")))
          .isEqualTo(Collections.singletonMap(a, c));
    }

    /**
     * installing/deps-installer verifyLockfileResolutions.ts (11.5.3): a registry dep path whose resolution is a
     * directory fails the verification, with no verifier active; before, it passes.
     */
    @Test
    void rejectsARegistryDepPathWithANonRegistryResolutionFrom1153() {
        String lock = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                      "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n    dependencies:\n      foo:\n" +
                      "        specifier: 1.0.0\n        version: 1.0.0\n\npackages:\n\n  foo@1.0.0:\n" +
                      "    resolution: {directory: ../foo, type: directory}\n\nsnapshots:\n\n  foo@1.0.0: {}\n";
        LockfileObject lockfile = LockfileReader.read(lock, release("11.5.3")).getLockfile();
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(lockfile,
          Collections.emptyList(), release("11.5.3")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_RESOLUTION_SHAPE_MISMATCH");
              assertThat(e.getMessage()).isEqualTo("1 lockfile entries failed verification:\n  foo@1.0.0 a " +
                                                   "registry-style dependency path is backed by a non-registry " +
                                                   "resolution");
          });
        VerifyLockfileResolutions.verifyLockfileResolutions(LockfileReader.read(lock, release("11.5.2"))
          .getLockfile(), Collections.emptyList(), release("11.5.2"));
    }

    /**
     * config/reader loadNpmrcFiles.ts and getOptionsFromRootManifest.ts (11.5.3): the project's .npmrc leaves out a
     * registry or a credential it takes from the environment, and its pnpm-workspace.yaml a registry; before, both
     * are expanded. The user's .npmrc expands them either way.
     */
    @Test
    void ignoresTheEnvironmentInRepositoryRequestDestinationsFrom1153() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("H", "h.test");
        env.put("A", "Zm9v");
        String scoped = "@org:registry=https://${H}/\n";
        assertThat(options("", scoped, null, env, "11.5.2").getRegistries()).containsEntry("@org", "https://h.test/");
        assertThat(options("", scoped, null, env, "11.5.3").getRegistries()).doesNotContainKey("@org");
        assertThat(PnpmOptions.load("", null, scoped, null, env, release("11.5.3")).getRegistries())
          .containsEntry("@org", "https://h.test/");
        // _auth of "foo" has no user:password separator
        String auth = "//registry.npmjs.org/:_auth=${A}\n";
        assertThatThrownBy(() -> options("", auth, null, env, "11.5.2"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_MISSING_SEPARATOR"));
        options("", auth, null, env, "11.5.3");
        String workspace = "registry: ${UNSET}\n";
        assertThatThrownBy(() -> options(workspace, null, null, env, "11.5.2"))
          .isInstanceOf(JsError.class)
          .hasMessageContaining("Failed to replace env in config: ${UNSET}");
        assertThat(options(workspace, null, null, env, "11.5.3").getRegistries())
          .containsEntry("default", REGISTRY);
    }

    /**
     * pnpm packageManagerRegistries.ts, switchCliVersion.ts and syncEnvLockfile.ts (11.5.3): pnpm resolves itself
     * from the registries of the user's .npmrc and the environment alone; before, from the project's too.
     */
    @Test
    void resolvesThePackageManagerFromTrustedConfigFrom1153() {
        String npmrc = "registry=https://mirror.test/\n";
        assertThat(options("", npmrc, null, Collections.emptyMap(), "11.5.3")
          .packageManagerBootstrap(release("11.5.3")).getRegistries()).containsEntry("default", REGISTRY);
        assertThat(options("", npmrc, null, Collections.emptyMap(), "11.5.2")
          .packageManagerBootstrap(release("11.5.2")).getRegistries())
          .containsEntry("default", "https://mirror.test/");
        assertThat(PnpmOptions.load("", npmrc, "registry=https://u.test/\n", null, Collections.emptyMap(),
            release("11.5.3")).packageManagerBootstrap(release("11.5.3")).getRegistries())
          .containsEntry("default", "https://u.test/");
    }

    private static String envDocument(String pnpm, String pnpmResolution) {
        return "---\nlockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    configDependencies: {}\n" +
               "    packageManagerDependencies:\n      '@pnpm/exe':\n        specifier: " + pnpm + "\n" +
               "        version: " + pnpm + "\n      pnpm:\n        specifier: " + pnpm + "\n" +
               "        version: " + pnpm + "\n\npackages:\n\n  '@pnpm/exe@" + pnpm + "':\n" +
               "    resolution: {integrity: sha512-y}\n\n  pnpm@" + pnpm + ":\n    resolution: " + pnpmResolution +
               "\n\nsnapshots:\n\n  '@pnpm/exe@" + pnpm + "': {}\n\n  pnpm@" + pnpm + ": {}\n";
    }

    private static PnpmLockRegenerator switching(String runs, String wanted, String pnpmResolution) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(envDocument(wanted, pnpmResolution) + "\n---\n" + WRAPPY_LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}," +
                        "\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"" + wanted +
                        "\",\"onFail\":\"download\"}}}")
          .env(Collections.emptyMap())
          .now(NOW)
          .host(Host.builder().nodeVersion("v24.21.0").os("darwin").cpu("arm64").build())
          .release(release(runs))
          .build(), WRAPPY_ONLY);
    }

    /**
     * pnpm switchCliVersion.ts and packageManagerLockfile.ts (11.5.3): before handing the install to the release
     * the env document locks, pnpm checks that it is a registry package locked by its integrity alone; before, it
     * hands over without.
     */
    @Test
    void checksTheEnvDocumentBeforeSwitchingFrom1153() {
        String tarball = "{integrity: sha512-x, tarball: 'https://example.test/pnpm.tgz'}";
        assertThatThrownBy(() -> switching("11.5.3", "11.5.2", tarball).install())
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_PACKAGE_MANAGER_LOCKFILE");
              assertThat(e.getMessage()).isEqualTo("The packageManager dependency \"pnpm@11.5.2\" in " +
                                                   "pnpm-lock.yaml must use a registry package path and an " +
                                                   "integrity-only resolution");
          });
        assertThatThrownBy(() -> switching("11.5.2", "11.5.3", tarball).install())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("pnpm 11.5.3, which pnpm 11.5.2 downloads and hands the install over to");
        assertThatThrownBy(() -> switching("11.5.3", "11.5.2", "{integrity: sha512-x}").install())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("pnpm 11.5.2, which pnpm 11.5.3 downloads and hands the install over to");
    }

    /**
     * installing/commands installDeps.ts and verifyPacquetIdentity.ts (11.5.3): a pacquet the configDependencies
     * name and the env document locks for the host has its registry signatures checked, which no input holds;
     * before, a lock-only install never reads it. Without its binary for the host, pnpm uses its own engine.
     */
    @Test
    void verifiesPacquetBeforeInstallingFrom1153() {
        String env = "lockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    configDependencies:\n      pacquet:\n" +
                     "        specifier: 1.0.0\n        version: 1.0.0\n\npackages:\n\n" +
                     "  '@pacquet/darwin-arm64@1.0.0':\n    resolution: {integrity: sha512-y}\n\n" +
                     "  pacquet@1.0.0:\n    resolution: {integrity: sha512-x}\n\nsnapshots:\n\n" +
                     "  '@pacquet/darwin-arm64@1.0.0': {}\n\n  pacquet@1.0.0:\n    optionalDependencies:\n" +
                     "      '@pacquet/darwin-arm64': 1.0.0\n";
        EnvLockfile envLockfile = EnvLockfile.readEnvLockfile("---\n" + env + "\n---\n" + EMPTY_LOCK);
        Host darwin = Host.builder().os("darwin").cpu("arm64").build();
        String workspace = "configDependencies:\n  pacquet: 1.0.0+sha512-x\n";
        assertThatThrownBy(() -> InstallDeps.verifyPacquetIdentity(
          options(workspace, null, null, Collections.emptyMap(), "11.5.3"), () -> envLockfile, darwin))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("signatures of pacquet@1.0.0 and @pacquet/darwin-arm64@1.0.0");
        InstallDeps.verifyPacquetIdentity(options(workspace, null, null, Collections.emptyMap(), "11.5.2"),
          () -> envLockfile, darwin);
        InstallDeps.verifyPacquetIdentity(options(workspace, null, null, Collections.emptyMap(), "11.5.3"),
          () -> envLockfile, Host.builder().os("linux").cpu("x64").build());
    }

    private static final RegistryMetadata NODE_22 = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return null;
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return null;
        }

        @Override
        public @Nullable String document(String url) {
            if (url.endsWith("/release/index.json")) {
                return "[{\"version\":\"v22.0.0\",\"lts\":false},{\"version\":\"v21.0.0\",\"lts\":false}]";
            }
            if (url.endsWith("/release/v22.0.0/SHASUMS256.txt")) {
                return "aa  node-v22.0.0-linux-x64.tar.gz\n";
            }
            return null;
        }
    };

    private static NpmResolver.@Nullable ResolveResult nodeRuntime(String version, @Nullable String mirror,
                                                                  String release) {
        ResolveDependencies.WantedDependency node = new ResolveDependencies.WantedDependency("node",
          "runtime:" + version, false, false);
        Map<String, @Nullable Object> mirrors = mirror == null ? null :
          Collections.singletonMap("release", mirror);
        return NodeRuntimeResolver.resolveNodeRuntime(NODE_22, node, null, null, mirrors, "linux", release(release));
    }

    /**
     * engine/runtime/node-resolver and crypto/shasums-file verifyNodeShasums.ts (11.5.3): a release-channel Node.js
     * runtime is resolved from a SHASUMS256.txt whose signature a Node.js release key made. nodejs.org's own is; what
     * another mirror serves no input says, nor the HTTP status of one pnpm cannot fetch; before, from the file alone.
     */
    @Test
    void verifiesTheNodeShasumsSignatureFrom1153() {
        assertThat(nodeRuntime("22.0.0", null, "11.5.3").id).isEqualTo("node@runtime:22.0.0");
        assertThat(nodeRuntime("22.0.0", "https://mirror.test/download/release/", "11.5.2").id)
          .isEqualTo("node@runtime:22.0.0");
        assertThatThrownBy(() -> nodeRuntime("22.0.0", "https://mirror.test/download/release/", "11.5.3"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("v22.0.0/SHASUMS256.txt.sig");
        assertThatThrownBy(() -> nodeRuntime("21.0.0", null, "11.5.3"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("the HTTP status");
        assertThatThrownBy(() -> nodeRuntime("21.0.0", null, "11.5.2"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_FAILED_DOWNLOAD_SHASUM_FILE"));
    }

    // a's x and b's y both depend on shared, which lists p as a dependency and as a peer, and a depends on p itself, so
    // that under x the peer drops p from shared's dependencies (autoInstallPeers false)
    private static final RegistryMetadata SHARED_CHILDREN = registry(
      new String[]{"a", "\"x\":\"1.0.0\",\"p\":\"1.0.0\"", ""},
      new String[]{"b", "\"y\":\"1.0.0\"", ""},
      new String[]{"x", "\"shared\":\"1.0.0\"", ""},
      new String[]{"y", "\"shared\":\"1.0.0\"", ""},
      new String[]{"shared", "\"p\":\"1.0.0\"", "\"p\":\"1.0.0\""},
      new String[]{"p", "", ""});

    // the snapshots of the lock-only install of a root with b, then a
    private static String installSharedChildren(String release) {
        String lock = new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"b\":\"1.0.0\",\"a\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "autoInstallPeers: false\nminimumReleaseAge: 0\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), SHARED_CHILDREN).writeLock();
        return lock.substring(lock.indexOf("\nsnapshots:\n"));
    }

    // the dependency tree deps-resolver resolves for a root with these dependencies as one of several releases, which
    // only the release that installs can be (a range pnpm 11.2.2 hands the install to)
    private static ResolveDependencyTree.Result resolveSharedChildren(PnpmRelease release, String dependencies) {
        LockfileObject lockfile = new LockfileObject();
        lockfile.set("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        lockfile.getImporters().put(".", new ProjectSnapshot());
        Map<String, String> registries = Collections.singletonMap("default", REGISTRY);
        NpmResolver npmResolver = new NpmResolver(SHARED_CHILDREN, registries, Collections.emptyMap(), null, true,
          false, NOW, release, false);
        ResolveDependencies.ResolutionContext ctx = new ResolveDependencies.ResolutionContext(lockfile, "",
          new PackageRequester(npmResolver, new LinkedHashMap<>(), Host.UNKNOWN, false, false, null, null,
            SHARED_CHILDREN, null));
        ctx.setRelease(release);
        ctx.setRegistries(registries);
        ctx.setHoistPeers(true);
        Map<String, @Nullable Object> manifest = obj("{'name':'root','version':'1.0.0','dependencies':{" +
                                                     dependencies + "}}");
        return ResolveDependencyTree.resolveDependencyTree(Collections.singletonList(new ResolveDependencyTree.Importer(
            ".", "", manifest, ResolveDependencyTree.getWantedDependencies(manifest, false, release))), ctx,
          PreferredVersions.ByName.of(new LinkedHashMap<>()));
    }

    /**
     * installing/deps-resolver resolveDependencies.ts claimChildrenResolution and waitForPackageResolutionTurn
     * (11.7.0, pnpm/pnpm#12358): shared, reached under b's y first, resolves its children in the context of a's x,
     * whose parents' ids are lower, where a's p drops p from shared's dependencies; before, in that of y, the first to
     * resolve. The package is the first occurrence's all the same, without the peer. pnpm 11.6.0 and 11.7.0 wrote
     * these snapshots when run on it. As one of a range across 11.7.0, the release is asked where a request would
     * wait for a shallower one or a claim would be taken over, which a root with a alone reaches neither of.
     */
    @Test
    void resolvesSharedChildrenInADeterministicContextFrom1170() {
        String upToShared = "\nsnapshots:\n\n  a@1.0.0:\n    dependencies:\n      p: 1.0.0\n      x: 1.0.0\n\n" +
                            "  b@1.0.0:\n    dependencies:\n      y: 1.0.0\n\n  p@1.0.0: {}\n\n";
        String fromX = "  x@1.0.0:\n    dependencies:\n      shared: 1.0.0\n\n  y@1.0.0:\n    dependencies:\n" +
                       "      shared: 1.0.0\n";
        assertThat(installSharedChildren("11.6.0"))
          .isEqualTo(upToShared + "  shared@1.0.0:\n    dependencies:\n      p: 1.0.0\n\n" + fromX);
        assertThat(installSharedChildren("11.7.0")).isEqualTo(upToShared + "  shared@1.0.0: {}\n\n" + fromX);
        PnpmRelease straddling = PnpmRelease.oneOf("11.6.0 || 11.7.0", Arrays.asList("11.6.0", "11.7.0"));
        assertThatThrownBy(() -> resolveSharedChildren(straddling, "'b':'1.0.0','a':'1.0.0'"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("one of 11.6.0 || 11.7.0, where the releases from 11.7.0 install it otherwise");
        assertThat(resolveSharedChildren(straddling, "'a':'1.0.0'").getDependenciesTree().values())
          .anySatisfy(node -> {
              assertThat(node.getResolvedPackage().getName()).isEqualTo("shared");
              assertThat(node.children()).isEmpty();
          });
    }

    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

    /**
     * resolving/resolver-base isGitHostedTarballUrl (11.9.0): only a commit's archive is git-hosted, so the archive of
     * a branch is written without gitHosted, lockfile.fs reads a tarball that is no string, and the npm verifier
     * checks a flagged resolution of such a URL; before, any codeload URL with tar.gz is git-hosted.
     */
    @Test
    void recognizesOnlyCommitArchivesAsGitHostedFrom1190() {
        String branch = "https://codeload.github.com/o/r/tar.gz/main";
        Map<String, @Nullable Object> resolution = obj("{'integrity':'sha512-x','tarball':'" + branch + "'}");
        assertThat(json(LockfileUtils.toLockfileResolution("r", "1.0.0", resolution, REGISTRY, false,
          release("11.8.0")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + branch + "','gitHosted':true}");
        assertThat(json(LockfileUtils.toLockfileResolution("r", "1.0.0", resolution, REGISTRY, false,
          release("11.9.0")))).isEqualTo("{'integrity':'sha512-x','tarball':'" + branch + "'}");
        assertThat(LockfileUtils.isGitHostedCommitArchive("https://codeload.github.com/o/r/tar.gz/" + SHA)).isTrue();
        assertThat(LockfileUtils.isGitHostedCommitArchive("https://bitbucket.org/o/r/get/" + SHA + ".tar.gz"))
          .isTrue();
        assertThat(LockfileUtils.isGitHostedCommitArchive("https://gitlab.com/api/v4/projects/7/repository/" +
                                                          "archive.tar.gz?ref=" + SHA)).isTrue();
        assertThat(LockfileUtils.isGitHostedCommitArchive("https://gitlab.com/o/r/-/archive/" + SHA + "/r.tar.gz"))
          .isTrue();
        assertThat(LockfileUtils.isGitHostedCommitArchive("http://codeload.github.com/o/r/tar.gz/" + SHA)).isFalse();

        Map<String, @Nullable Object> numbered = obj("{'lockfileVersion':'9.0','importers':{},'packages':{" +
                                                     "'foo@1.0.0':{'resolution':{'integrity':'sha512-x'," +
                                                     "'tarball':5}}},'snapshots':{'foo@1.0.0':{}}}");
        assertThatThrownBy(() -> LockfileFormatConverters.convertToLockfileObject(obj(json(numbered)),
          release("11.8.0")))
          .isInstanceOf(LockfileFormatConverters.TypeError.class)
          .hasMessage("url.toLowerCase is not a function");
        assertThat(LockfileFormatConverters.convertToLockfileObject(numbered, release("11.9.0")).getPackages())
          .containsKey("foo@1.0.0");

        Map<String, @Nullable Object> flagged = obj("{'integrity':'sha512-x','tarball':'" + branch +
                                                    "','gitHosted':true}");
        assertThat(NpmResolutionVerifier.isNpmRegistryResolution(flagged, release("11.8.0"))).isFalse();
        assertThat(NpmResolutionVerifier.isNpmRegistryResolution(flagged, release("11.9.0"))).isTrue();
    }

    // the npm verifier of release over registry, under minimumReleaseAge where it is given
    private static NpmResolutionVerifier npmVerifier(String release, RegistryMetadata registry,
                                                     @Nullable Double minimumReleaseAge) {
        NpmResolutionVerifier verifier = NpmResolutionVerifier.createNpmResolutionVerifier(
          NpmResolutionVerifier.Options.builder()
            .minimumReleaseAge(minimumReleaseAge)
            .registries(Collections.singletonMap("default", REGISTRY))
            .fetch(registry)
            .now(NOW)
            .release(release(release))
            .build());
        assertThat(verifier).isNotNull();
        return verifier;
    }

    private static void verifyLock(String lock, String release, RegistryMetadata registry,
                                   @Nullable Double minimumReleaseAge) {
        VerifyLockfileResolutions.verifyLockfileResolutions(LockfileReader.read(lock, release(release)).getLockfile(),
          Collections.singletonList(npmVerifier(release, registry, minimumReleaseAge)), release(release));
    }

    // a lock of the orphan package foo 1.0.0 resolved as given
    private static String fooLock(String resolution) {
        return "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
               "importers:\n\n  .: {}\n\npackages:\n\n  foo@1.0.0:\n    resolution: " + resolution + "\n\n" +
               "snapshots:\n\n  foo@1.0.0: {}\n";
    }

    /**
     * resolving/npm-resolver createNpmResolutionVerifier.ts isRegistryTarballResolution and lockfile/utils
     * pkgSnapshotToResolution.ts (11.9.0): a registry tarball locked without an integrity fails the lock's
     * verification, and a non-semver version is a violation; before, the verification passes both and
     * pkgSnapshotToResolution fails on the missing integrity. A tarball that is no string fails pkgSnapshotToResolution
     * from 11.9.0.
     */
    @Test
    void verifiesALockedTarballsIntegrityFrom1190() {
        RegistryMetadata registry = registry(new String[]{"foo", "", ""});
        String tarball = REGISTRY + "foo/-/x-1.0.0.tgz";
        String lock = fooLock("{tarball: " + tarball + "}");
        assertThatThrownBy(() -> verifyLock(lock, "11.9.0", registry, null))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_TARBALL_INTEGRITY");
              assertThat(e.getMessage()).isEqualTo("1 lockfile entries failed verification:\n  foo@1.0.0 has no " +
                                                   "\"integrity\" field, so its downloaded tarball cannot be verified");
          });
        verifyLock(lock, "11.8.0", registry, null);
        Map<String, String> registries = Collections.singletonMap("default", REGISTRY);
        PackageSnapshot snapshot = LockfileReader.read(lock, release("11.8.0")).getLockfile().getPackages()
          .get("foo@1.0.0");
        assertThatThrownBy(() -> LockfileUtils.pkgSnapshotToResolution("foo@1.0.0", snapshot, registries,
          release("11.8.0")))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_TARBALL_INTEGRITY"));
        assertThat(json(LockfileUtils.pkgSnapshotToResolution("foo@1.0.0", snapshot, registries,
          release("11.9.0")))).isEqualTo("{'tarball':'" + tarball + "'}");

        ResolutionVerifier.Result nonSemver = npmVerifier("11.9.0", registry, null)
          .verify(obj("{'integrity':'sha512-x'}"), "foo", "latest", null);
        assertThat(nonSemver.getCode()).isEqualTo("TARBALL_URL_MISMATCH");
        assertThat(nonSemver.getReason()).isEqualTo("has a non-semver version (\"latest\") and so cannot be verified " +
                                                    "against the registry's published metadata");
        assertThat(npmVerifier("11.8.0", registry, null).verify(obj("{'integrity':'sha512-x'}"), "foo", "latest",
          null).isOk()).isTrue();

        PackageSnapshot numbered = new PackageSnapshot();
        numbered.setResolution(obj("{'integrity':'sha512-x','tarball':5}"));
        assertThatThrownBy(() -> LockfileUtils.pkgSnapshotToResolution("foo@1.0.0", numbered, registries,
          release("11.9.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_TARBALL_RESOLUTION");
              assertThat(e.getMessage()).isEqualTo("Cannot install package \"foo@1.0.0\": its lockfile entry has a " +
                                                   "non-string \"tarball\" field.");
          });
    }

    // a registry that answers 404 to every request
    private static final RegistryMetadata NOT_FOUND = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return null;
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return null;
        }

        @Override
        public @Nullable String document(String url) {
            return null;
        }
    };

    /**
     * resolving/npm-resolver createNpmResolutionVerifier.ts runAgeCheck and runTarballUrlCheck (11.9.0): the 404 of
     * a locked package's packument fails the install; before, minimumReleaseAge could not check it, and its tarball
     * URL could not be verified, each a violation.
     */
    @Test
    void failsVerificationOnARegistryFetchErrorFrom1190() {
        String lock = fooLock("{integrity: sha512-x}");
        assertThatThrownBy(() -> verifyLock(lock, "11.8.0", NOT_FOUND, 60.0))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION");
              assertThat(e.getMessage()).isEqualTo("1 lockfile entries failed verification:\n  foo@1.0.0 could not " +
                                                   "be checked against minimumReleaseAge (GET " + REGISTRY +
                                                   "foo: Not Found - 404)");
          });
        assertThatThrownBy(() -> verifyLock(lock, "11.9.0", NOT_FOUND, 60.0))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_FETCH_404");
              assertThat(e.getMessage()).isEqualTo("GET " + REGISTRY + "foo: Not Found - 404");
          });
        String mirrored = fooLock("{integrity: sha512-x, tarball: 'https://mirror.test/foo-1.0.0.tgz'}");
        assertThatThrownBy(() -> verifyLock(mirrored, "11.8.0", NOT_FOUND, null))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_TARBALL_URL_MISMATCH");
              assertThat(e.getMessage()).endsWith("foo@1.0.0 could not be verified against the registry's " +
                                                  "published metadata");
          });
        assertThatThrownBy(() -> verifyLock(mirrored, "11.9.0", NOT_FOUND, null))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_FETCH_404"));
    }

    /**
     * core/error FetchError and redactUrlCredentials (11.9.0): a failed request's message names its URL without the
     * credentials in it; before, as requested.
     */
    @Test
    void redactsUrlCredentialsInFetchErrorsFrom1190() {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("^1.0.0", "foo",
          "latest", "https://u:p@r.test/");
        assertThatThrownBy(() -> PickPackage.pickPackage(new LinkedHashMap<>(), NOT_FOUND, spec,
          picker("2026-01-01T00:00:00Z", false, "11.9.0"), "https://u:p@r.test/", false, false))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_FETCH_404");
              assertThat(e.getMessage()).isEqualTo("GET https://r.test/foo: Not Found - 404");
          });
        assertThatThrownBy(() -> PickPackage.pickPackage(new LinkedHashMap<>(), NOT_FOUND, spec,
          picker("2026-01-01T00:00:00Z", false, "11.8.0"), "https://u:p@r.test/", false, false))
          .hasMessage("GET https://u:p@r.test/foo: Not Found - 404");
        assertThat(PnpmError.redactUrlCredentials("from git+ssh://git@github.com:o/r and ://x@y and https://a@b@c/d"))
          .isEqualTo("from git+ssh://github.com:o/r and ://x@y and https://c/d");
    }

    // a's shared at depth 1 resolves c, whose peer p a provides; b's chain reaches shared again at depth 5, after that
    private static final RegistryMetadata DEEP_SHARED = registry(
      new String[]{"a", "\"shared\":\"1.0.0\",\"p\":\"1.0.0\"", ""},
      new String[]{"b", "\"x1\":\"1.0.0\"", ""},
      new String[]{"x1", "\"x2\":\"1.0.0\"", ""},
      new String[]{"x2", "\"x3\":\"1.0.0\"", ""},
      new String[]{"x3", "\"y\":\"1.0.0\"", ""},
      new String[]{"y", "\"shared\":\"1.0.0\"", ""},
      new String[]{"shared", "\"c\":\"1.0.0\"", ""},
      new String[]{"c", "", "\"p\":\"1.0.0\""},
      new String[]{"p", "", ""});

    // the snapshots of the lock-only install of a root with a and b, peers hoisted by dedupePeerDependents alone
    private static String installDeepShared(String release) {
        String lock = new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1.0.0\",\"b\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "minimumReleaseAge: 0\nautoInstallPeers: false\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), DEEP_SHARED).writeLock();
        return lock.substring(lock.indexOf("\nsnapshots:\n"));
    }

    // a snapshot of one dependency and the transitive peer p
    private static String transitivelyPeeringP(String name, String suffix, String child, String childSuffix) {
        return "  " + name + "@1.0.0" + suffix + ":\n    dependencies:\n      " + child + ": 1.0.0" + childSuffix +
               "\n    transitivePeerDependencies:\n      - p\n";
    }

    // the snapshots of the install, b's chain down to y taking p from the root or not
    private static String deepSharedSnapshots(String chainSuffix) {
        return "\nsnapshots:\n\n  a@1.0.0:\n    dependencies:\n      p: 1.0.0\n      shared: 1.0.0(p@1.0.0)\n\n" +
               transitivelyPeeringP("b", chainSuffix, "x1", chainSuffix) + "\n" +
               "  c@1.0.0(p@1.0.0):\n    dependencies:\n      p: 1.0.0\n\n  p@1.0.0: {}\n\n" +
               transitivelyPeeringP("shared", "(p@1.0.0)", "c", "(p@1.0.0)") + "\n" +
               transitivelyPeeringP("x1", chainSuffix, "x2", chainSuffix) + "\n" +
               transitivelyPeeringP("x2", chainSuffix, "x3", chainSuffix) + "\n" +
               transitivelyPeeringP("x3", chainSuffix, "y", chainSuffix) + "\n" +
               transitivelyPeeringP("y", chainSuffix, "shared", "(p@1.0.0)");
    }

    /**
     * installing/deps-resolver resolveDependencies.ts claimChildrenResolution (11.9.0): shared, reached through b at
     * depth 5 after its children under a resolved, no longer shares their missing peer p, so that b's chain reports
     * none and the root hoists no p for it (dedupePeerDependents, autoInstallPeers false); before, b's chain takes p
     * from the root. pnpm 11.8.0 and 11.9.0 wrote these snapshots when run on it.
     */
    @Test
    void sharesMissingPeersOfChildrenOnlyWithAnOwnerAsDeepFrom1190() {
        assertThat(installDeepShared("11.8.0")).isEqualTo(deepSharedSnapshots("(p@1.0.0)"));
        assertThat(installDeepShared("11.9.0")).isEqualTo(deepSharedSnapshots(""));
    }

    // the dependencies engines.runtime of node asks pnpm to download with this version
    private static @Nullable Object runtimeDependencies(String version, String release) {
        Map<String, @Nullable Object> manifest = obj("{'engines':{'runtime':{'name':'node','version':" + version +
                                                     ",'onFail':'download'}}}");
        PkgManifestUtils.convertEnginesRuntimeToDependencies(manifest, "engines", "dependencies", release(release));
        return manifest.get("dependencies");
    }

    // a Node.js release index whose LTS line is 20
    private static final RegistryMetadata NODE_20_LTS = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return null;
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return null;
        }

        @Override
        public @Nullable String document(String url) {
            if (url.endsWith("/release/index.json")) {
                return "[{\"version\":\"v22.0.0\",\"lts\":false},{\"version\":\"v20.0.0\",\"lts\":\"Iron\"}]";
            }
            if (url.endsWith("/release/v20.0.0/SHASUMS256.txt")) {
                return "aa  node-v20.0.0-linux-x64.tar.gz\n";
            }
            return null;
        }
    };

    /**
     * pkg-manifest/utils convertEnginesRuntimeToDependencies and engine/runtime/node-resolver normalizeRuntimeSpec
     * (11.9.0): a runtime's version is trimmed and must be a string, a blank one latest, so that runtime:lts with a
     * space resolves the LTS line; before, a truthy version is taken as written, and that one is no version pnpm finds.
     */
    @Test
    void trimsRuntimeVersionsFrom1190() {
        assertThat(json(runtimeDependencies("' 22 '", "11.9.0"))).isEqualTo("{'node':'runtime:22'}");
        assertThat(json(runtimeDependencies("' 22 '", "11.8.0"))).isEqualTo("{'node':'runtime: 22 '}");
        assertThat(runtimeDependencies("22", "11.9.0")).isNull();
        assertThat(json(runtimeDependencies("22", "11.8.0"))).isEqualTo("{'node':'runtime:22'}");
        assertThat(json(runtimeDependencies("''", "11.9.0"))).isEqualTo("{'node':'runtime:'}");
        assertThat(runtimeDependencies("''", "11.8.0")).isNull();

        ResolveDependencies.WantedDependency lts = new ResolveDependencies.WantedDependency("node", "runtime:lts ",
          false, false);
        NpmResolver.ResolveResult resolved = NodeRuntimeResolver.resolveNodeRuntime(NODE_20_LTS, lts, null, null, null,
          "linux", release("11.9.0"));
        assertThat(resolved).isNotNull();
        assertThat(resolved.id).isEqualTo("node@runtime:20.0.0");
        assertThatThrownBy(() -> NodeRuntimeResolver.resolveNodeRuntime(NODE_20_LTS, lts, null, null, null, "linux",
          release("11.8.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_NODEJS_VERSION_NOT_FOUND");
              assertThat(e.getMessage()).isEqualTo("Could not find a Node.js version that satisfies lts ");
          });
    }

    /**
     * workspace/project-manifest-reader readDependenciesField (11.9.0): the manifest writer a recursive install runs
     * once the lock is written fails on a devDependencies field that is null; before, it reads no such field. pnpm
     * 11.9.0 wrote the lock and exited with this error, and 11.8.0 wrote it and exited 0, when run on such a root.
     */
    @Test
    void rejectsANonObjectDependenciesFieldOnWriteFrom1190() {
        String workspace = "packages:\n  - '.'\nminimumReleaseAge: 0\n";
        assertThatThrownBy(() -> regenerator("11.9.0", ",\"devDependencies\":null", workspace, WRAPPY_ONLY)
          .writeLock())
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_DEPENDENCIES_FIELD");
              assertThat(e.getMessage()).isEqualTo("The \"devDependencies\" field must be an object.");
          });
        assertThat(regenerator("11.8.0", ",\"devDependencies\":null", workspace, WRAPPY_ONLY).writeLock())
          .isEqualTo(WRAPPY_LOCK);
        assertThat(Manifests.invalidDependenciesField(obj("{'dependencies':'x','devDependencies':{}}"),
          release("11.9.0"))).hasMessage("The \"dependencies\" field must be an object.");
        assertThat(Manifests.invalidDependenciesField(obj("{'dependencies':[]}"), release("11.8.0"))).isNull();
    }

    /**
     * config/reader loadNpmrcFiles.ts readJsonAuthEnv and index.ts (11.10.0): pnpm_config__auth routes the default
     * registry and a scope to the registry it names, over the workspace's registries, and fails where it is no such
     * object; before, pnpm reads no such setting.
     */
    @Test
    void readsTheAuthSettingFrom11100() {
        Map<String, String> env = Collections.singletonMap("pnpm_config__auth", "{\"https://npm.example.com/\":" +
                                                                                "{\"@\":{\"authToken\":\"t\"}," +
                                                                                "\"@org\":{\"authToken\":\"u\"}}}");
        String workspace = "registries:\n  '@org': https://yaml.example.com/\n";
        assertThat(options(workspace, null, null, env, "11.10.0").getRegistries())
          .containsEntry("default", "https://npm.example.com/")
          .containsEntry("@org", "https://npm.example.com/");
        assertThat(options(workspace, null, null, env, "11.9.0").getRegistries())
          .containsEntry("default", REGISTRY)
          .containsEntry("@org", "https://yaml.example.com/");
        assertThatThrownBy(() -> options("", null, null, Collections.singletonMap("PNPM_CONFIG__AUTH",
          "{\"ftp://x.test/\":{}}"), "11.10.0"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_AUTH_SETTING");
              assertThat(e.getMessage()).isEqualTo("pnpm_config__auth[entry 1]: key must be an http(s) registry URL");
          });
        assertThatThrownBy(() -> options("", null, null, Collections.singletonMap("pnpm_config__auth",
          "{\"https://x.test/\":{\"@\":{\"_password\":\"p\"}}}"), "11.10.0"))
          .hasMessage("pnpm_config__auth[entry 1 (https://x.test)][\"@\"][\"_password\"]: unsupported auth field " +
                      "(only \"authToken\" is supported)");
        assertThat(options("", null, null, Collections.singletonMap("pnpm_config__auth", "[]"), "11.9.0")
          .getRegistries()).containsEntry("default", REGISTRY);
    }

    /**
     * config/reader index.ts getConfig (11.10.0): pnpm-workspace.yaml's registry is the default registry, over its
     * registries' default; before, the setting is no registry pnpm resolves from.
     */
    @Test
    void takesTheRegistrySettingAsTheDefaultFrom11100() {
        String workspace = "registry: https://r.example/\n";
        assertThat(options(workspace, null, null, Collections.emptyMap(), "11.10.0").getRegistries())
          .containsEntry("default", "https://r.example/");
        assertThat(options(workspace, null, null, Collections.emptyMap(), "11.9.0").getRegistries())
          .containsEntry("default", REGISTRY);
        String both = "registry: https://r.example/\nregistries:\n  default: https://s.example/\n";
        assertThat(options(both, null, null, Collections.emptyMap(), "11.10.0").getRegistries())
          .containsEntry("default", "https://r.example/");
        assertThat(options(both, null, null, Collections.emptyMap(), "11.9.0").getRegistries())
          .containsEntry("default", "https://s.example/");
    }

    /**
     * resolving/jsr-specifier-parser jsrToNpmPackageName (11.10.0): a JSR name validate-npm-package-name rejects is
     * invalid; before, only one without a slash, the rest taken as they are.
     */
    @Test
    void validatesAJsrPackageNameFrom11100() {
        assertThatThrownBy(() -> JsrSpecifierParser.parseJsrSpecifier("jsr:@s/", null, release("11.10.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_JSR_PACKAGE_NAME");
              assertThat(e.getMessage()).isEqualTo("The package name '@s/' is invalid");
          });
        JsrSpecifierParser.JsrSpec scopeAlone = JsrSpecifierParser.parseJsrSpecifier("jsr:@s/", null,
          release("11.9.0"));
        assertThat(scopeAlone).isNotNull();
        assertThat(scopeAlone.getNpmPkgName()).isEqualTo("@jsr/s__");
        assertThatThrownBy(() -> JsrSpecifierParser.parseJsrSpecifier("jsr:@/x", null, release("11.10.0")))
          .hasMessage("The package name '@/x' is invalid");
        JsrSpecifierParser.JsrSpec nameAlone = JsrSpecifierParser.parseJsrSpecifier("jsr:@/x", null,
          release("11.9.0"));
        assertThat(nameAlone).isNotNull();
        assertThat(nameAlone.getNpmPkgName()).isEqualTo("@jsr/__x");
        JsrSpecifierParser.JsrSpec valid = JsrSpecifierParser.parseJsrSpecifier("jsr:@std/path@1", null,
          release("11.10.0"));
        assertThat(valid).isNotNull();
        assertThat(valid.getNpmPkgName()).isEqualTo("@jsr/std__path");
    }

    /**
     * resolving/npm-resolver parseBareSpecifier.ts parseNamedRegistrySpecifierToRegistryPackageSpec (11.10.0): a
     * named registry spec's name validate-npm-package-name rejects is invalid in every form; before, only a scope
     * without a name.
     */
    @Test
    void validatesANamedRegistryPackageNameFrom11100() {
        Set<String> gh = Collections.singleton("gh");
        assertThatThrownBy(() -> PickPackageFromMeta.parseNamedRegistrySpecifierToRegistryPackageSpec("gh:@/bar", gh,
          null, "latest", release("11.10.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_PACKAGE_NAME");
              assertThat(e.getMessage()).isEqualTo("The package name '@/bar' in named registry 'gh:' is invalid");
          });
        PickPackageFromMeta.NamedRegistryPackageSpec before = PickPackageFromMeta
          .parseNamedRegistrySpecifierToRegistryPackageSpec("gh:@/bar", gh, null, "latest", release("11.9.0"));
        assertThat(before).isNotNull();
        assertThat(before.getSpec().getName()).isEqualTo("@/bar");
        assertThatThrownBy(() -> PickPackageFromMeta.parseNamedRegistrySpecifierToRegistryPackageSpec("gh:@s/../n@1",
          gh, null, "latest", release("11.10.0")))
          .hasMessage("The package name '@s/../n' in named registry 'gh:' is invalid");
        for (String release : Arrays.asList("11.9.0", "11.10.0")) {
            assertThatThrownBy(() -> PickPackageFromMeta.parseNamedRegistrySpecifierToRegistryPackageSpec("gh:@s@1",
              gh, null, "latest", release(release)))
              .hasMessage("The package name '@s' in named registry 'gh:' is invalid");
        }
    }

    // foo's resolution as release from registry, linkWorkspacePackages trying the workspace's foo 2.0.0
    private static NpmResolver.@Nullable ResolveResult resolveFooBesideTheWorkspace(String range, String release,
                                                                                    RegistryMetadata registry) {
        NpmResolver resolver = new NpmResolver(registry, Collections.singletonMap("default", REGISTRY),
          Collections.emptyMap(), null, false, false, NOW, release(release), false);
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.alwaysTryWorkspacePackages = true;
        opts.projectDir = "";
        opts.lockfileDir = "";
        opts.workspacePackages = Collections.singletonMap("foo", Collections.singletonMap("2.0.0",
          new Verification.Project("foo", "foo", obj("{'name':'foo','version':'2.0.0'}"))));
        return resolver.resolveNpm(new ResolveDependencies.WantedDependency("foo", range, false, false), opts, null);
    }

    /**
     * resolving/npm-resolver index.ts resolveNpm (11.10.0): where the registry lacks foo, or any version of it in
     * range, and no version of the workspace's matches, that is the error; before, the registry's.
     */
    @Test
    void reportsTheWorkspaceVersionsTheRegistryLacksFrom11100() {
        String inside = "In : No matching version found for foo@^1.0.0 inside the workspace. Available versions: 2.0.0";
        assertThatThrownBy(() -> resolveFooBesideTheWorkspace("^1.0.0", "11.10.0", NOT_FOUND))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE");
              assertThat(e.getMessage()).isEqualTo(inside);
          });
        assertThatThrownBy(() -> resolveFooBesideTheWorkspace("^1.0.0", "11.9.0", NOT_FOUND))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_FETCH_404");
              assertThat(e.getMessage()).isEqualTo("GET " + REGISTRY + "foo: Not Found - 404");
          });
        RegistryMetadata foo1 = registry(new String[]{"foo", "", ""});
        assertThatThrownBy(() -> resolveFooBesideTheWorkspace("^5.0.0", "11.10.0", foo1))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE"));
        assertThatThrownBy(() -> resolveFooBesideTheWorkspace("^5.0.0", "11.9.0", foo1))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_NO_MATCHING_VERSION"));
        assertThat(resolveFooBesideTheWorkspace("^2.0.0", "11.10.0", NOT_FOUND)).isNotNull();
    }

    // a lock of no dependency beside the orphan opt 1.0.0
    private static final String ORPHAN_OPT_LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                                  "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n\n" +
                                                  "packages:\n\n  opt@1.0.0:\n    resolution: {integrity: sha512-x}" +
                                                  "\n\nsnapshots:\n\n  opt@1.0.0: {}\n";

    // the lock-only install of a root wanting opt in range as optional, from a registry without it
    private static String installMissingOptional(String range, String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(ORPHAN_OPT_LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"optionalDependencies\":{\"opt\":\"" + range +
                        "\"}}")
          .file("pnpm-workspace.yaml", "minimumReleaseAge: 0\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), NOT_FOUND).writeLock();
    }

    /**
     * installing/deps-resolver resolveDependencies.ts wantedLockfileContainsSatisfyingEntry (11.11.0, pnpm/pnpm#12853):
     * an optional dependency the registry lacks, which the lock holds in range, fails the install; before, it is
     * skipped and its locked entry left out. pnpm 11.10.0 wrote this lock, and 11.11.0 failed so, when run on it.
     */
    @Test
    void keepsAFailingOptionalDependencyTheLockResolvesFrom11110() {
        assertThatThrownBy(() -> installMissingOptional("^1.0.0", "11.11.0"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_FETCH_404");
              assertThat(e.getHint()).isEqualTo("opt is not in the npm registry, or you have no permission to " +
                                                "fetch it.");
          });
        String skipped = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                         "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n";
        assertThat(installMissingOptional("^1.0.0", "11.10.0")).isEqualTo(skipped);
        assertThat(installMissingOptional("^2.0.0", "11.11.0")).isEqualTo(skipped);
    }

    // a's c peers on a's b, which peers on x: a's x 1.0.0, where the root's x is x2
    private static final RegistryMetadata HOISTED_PEER_PROVIDER = registry(
      new String[]{"a", "\"b\":\"1.0.0\",\"x\":\"1.0.0\",\"c\":\"1.0.0\"", ""},
      new String[]{"b", "", "\"x\":\"*\""},
      new String[]{"c", "", "\"b\":\"1.0.0\""},
      new String[]{"x", "", ""},
      new String[]{"x2", "", ""});

    // the snapshots of the lock-only install of a root with a and x as npm:x2@1.0.0
    private static String installHoistedPeerProvider(String release) {
        String lock = new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1.0.0\"," +
                        "\"x\":\"npm:x2@1.0.0\"}}")
          .file("pnpm-workspace.yaml", "minimumReleaseAge: 0\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), HOISTED_PEER_PROVIDER).writeLock();
        return lock.substring(lock.indexOf("\nsnapshots:\n"));
    }

    // c's dep path naming the x its b takes
    private static String hoistedPeerProviderSnapshots(String x) {
        return "\nsnapshots:\n\n  a@1.0.0:\n    dependencies:\n      b: 1.0.0(x@1.0.0)\n" +
               "      c: 1.0.0(b@1.0.0(" + x + "))\n      x: 1.0.0\n\n  b@1.0.0(x@1.0.0):\n    dependencies:\n" +
               "      x: 1.0.0\n\n  c@1.0.0(b@1.0.0(" + x + ")):\n    dependencies:\n      b: 1.0.0(x@1.0.0)\n\n" +
               "  x2@1.0.0: {}\n\n  x@1.0.0: {}\n";
    }

    /**
     * installing/deps-resolver resolvePeers.ts (11.11.0, pnpm/pnpm#4993): b, the peer a's c resolves inside a, is
     * attached to the root for reuse and no longer peer-resolved again there, so that c's path names the x a gives b;
     * before, the root's walk binds it to the root's x, x2. pnpm 11.10.0 and 11.11.0 wrote these snapshots when run
     * on it.
     */
    @Test
    void resolvesHoistedPeerProvidersAfterOwnDependenciesFrom11110() {
        assertThat(installHoistedPeerProvider("11.11.0")).isEqualTo(hoistedPeerProviderSnapshots("x@1.0.0"));
        assertThat(installHoistedPeerProvider("11.10.0")).isEqualTo(hoistedPeerProviderSnapshots("x2@1.0.0"));
    }

    // the dependency graph resolvePeers gives a root whose y is a package named @x/../y
    private static Set<String> placeTraversal(String release) {
        Map<NodeId, DependenciesTreeNode> tree = new LinkedHashMap<>();
        treeNode(tree, ">@x/../y/1.0.0>", peerPkg("@x/../y", "1.0.0"), 0);
        List<ProjectToResolve> projects = Collections.singletonList(new ProjectToResolve(".", "",
          Collections.singletonMap("y", NodeId.of(">@x/../y/1.0.0>")), Collections.emptyList()));
        return ResolvePeers.resolvePeers(new LinkedHashSet<>(), projects, tree, false, false, false, false, 1000, "",
          new LinkedHashMap<>(), new LinkedHashSet<>(), release(release)).getDependenciesGraph().keySet();
    }

    /**
     * installing/deps-resolver resolvePeers.ts and fs/symlink-dependency safeJoinModulesDir (11.11.0): a package named
     * @x/../y is refused a directory in the virtual store, whose absolute path the error names; before, it is placed.
     */
    @Test
    void placesAPackageOnlyUnderAValidNameFrom11110() {
        assertThatThrownBy(() -> placeTraversal("11.11.0"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("with the invalid alias \"@x/../y\" (ERR_PNPM_INVALID_DEPENDENCY_NAME");
        assertThat(placeTraversal("11.10.0")).containsExactly("@x/../y/1.0.0");
    }

    // a registry of these packuments by name
    private static RegistryMetadata registryOf(Map<String, String> packuments) {
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

    // packuments of these versions, each {name, version, dependencies, peerDependencies[, other fields]} as the bodies
    // of JSON objects, the last version of a name its latest
    private static RegistryMetadata versions(String[]... versions) {
        Map<String, List<String>> byName = new LinkedHashMap<>();
        Map<String, String> latest = new LinkedHashMap<>();
        for (String[] v : versions) {
            byName.computeIfAbsent(v[0], n -> new ArrayList<>()).add("\"" + v[1] + "\":{\"name\":\"" + v[0] +
              "\",\"version\":\"" + v[1] + "\",\"dependencies\":{" + v[2] + "},\"peerDependencies\":{" + v[3] + "}" +
              (v.length > 4 ? "," + v[4] : "") + ",\"dist\":{\"integrity\":\"sha512-" + v[0].replaceAll("[^a-z]", "") +
              v[1].replaceAll("[^0-9]", "") + "\",\"tarball\":\"" + REGISTRY + v[0] + "/-/" +
              v[0].replaceAll("^@[^/]+/", "") + "-" + v[1] + ".tgz\"}}");
            latest.put(v[0], v[1]);
        }
        Map<String, String> packuments = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> name : byName.entrySet()) {
            packuments.put(name.getKey(), "{\"name\":\"" + name.getKey() + "\",\"dist-tags\":{\"latest\":\"" +
                                          latest.get(name.getKey()) + "\"},\"versions\":{" +
                                          String.join(",", name.getValue()) + "}}");
        }
        return registryOf(packuments);
    }

    // a's c peers on a's b 2.0.0, which the root attaches as a provider; the root's zb is b 1.0.0; proj's d peers on b
    private static final RegistryMetadata ATTACHED_PROVIDER = versions(
      new String[]{"a", "1.0.0", "\"b\":\"2.0.0\",\"c\":\"1.0.0\"", ""},
      new String[]{"b", "1.0.0", "", ""},
      new String[]{"b", "2.0.0", "", ""},
      new String[]{"c", "1.0.0", "", "\"b\":\"*\""},
      new String[]{"d", "1.0.0", "", "\"b\":\"*\""});

    // proj's d, as the lock-only install of the root with a and zb and of proj with d writes it
    private static String installBesideAnAttachedProvider(String release) {
        String lock = new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1.0.0\"," +
                        "\"zb\":\"npm:b@1.0.0\"}}")
          .manifest("proj", "{\"name\":\"proj\",\"version\":\"1.0.0\",\"dependencies\":{\"d\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "packages:\n  - proj\nminimumReleaseAge: 0\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), ATTACHED_PROVIDER).writeLock();
        int proj = lock.indexOf("\n  proj:\n");
        return lock.substring(proj, lock.indexOf("\n\npackages:", proj));
    }

    /**
     * installing/deps-resolver resolveDependencies.ts getHoistableRootDeps (11.12.0): proj's missing peer b is hoisted
     * as the root's zb, b 1.0.0, from the copy of the root's dependencies taken before peers are hoisted; before, the
     * root's own list holds the provider b it attached for a's c by then, whose alias sorts before zb and which saves
     * no specifier, so that b takes the highest version the install holds, 2.0.0. pnpm 11.11.0 and 11.12.0 wrote these
     * importers when run on it.
     */
    @Test
    void copiesTheRootDependenciesPeersAreHoistedAsFrom11120() {
        assertThat(installBesideAnAttachedProvider("11.12.0"))
          .isEqualTo("\n  proj:\n    dependencies:\n      d:\n        specifier: 1.0.0\n" +
                     "        version: 1.0.0(b@1.0.0)");
        assertThat(installBesideAnAttachedProvider("11.11.0"))
          .isEqualTo("\n  proj:\n    dependencies:\n      d:\n        specifier: 1.0.0\n" +
                     "        version: 1.0.0(b@2.0.0)");
    }

    // foo 1.2.3 and 2.0.0; bar depends on and peers on foo ^1.0.0
    private static final RegistryMetadata CONVERGING = versions(
      new String[]{"foo", "1.2.3", "", ""},
      new String[]{"foo", "2.0.0", "", ""},
      new String[]{"bar", "1.0.0", "\"foo\":\"^1.0.0\"", "\"foo\":\"^1.0.0\""});

    // the lock-only install of a root with foo ^2.0.0 and bar under the override foo@ of value
    private static String installConverging(String value, String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"foo\":\"^2.0.0\"," +
                        "\"bar\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "minimumReleaseAge: 0\noverrides:\n  \"foo@\": " + value + "\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), CONVERGING).writeLock();
    }

    // what pnpm 11.12.0 and 11.13.0 wrote: foo as the root installs it, and bar's peer range and peer
    private static String convergedLock(String fooSpecifier, String foo, String barPeerRange) {
        return "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
               "overrides:\n  foo@: 1.2.3\n\nimporters:\n\n  .:\n    dependencies:\n      bar:\n" +
               "        specifier: 1.0.0\n        version: 1.0.0(foo@" + foo + ")\n      foo:\n" +
               "        specifier: " + fooSpecifier + "\n        version: " + foo + "\n\npackages:\n\n" +
               "  bar@1.0.0:\n    resolution: {integrity: sha512-bar100}\n    peerDependencies:\n" +
               "      foo: " + barPeerRange + "\n\n  foo@" + foo + ":\n    resolution: {integrity: sha512-foo" +
               foo.replace(".", "") + "}\n\nsnapshots:\n\n  bar@1.0.0(foo@" + foo + "):\n    dependencies:\n" +
               "      foo: " + foo + "\n\n  foo@" + foo + ": {}\n";
    }

    /**
     * config/parse-overrides markConvergeOverride and hooks/read-package-hook createVersionsOverrider convergeDep
     * (11.13.0): foo@ converges bar's ranges, its dependency's and its peer's, on 1.2.3, which the root's ^2.0.0 does
     * not admit; before, it is an override of any range, the root's too. Its value must be an exact version, and its
     * empty range is no parent>child selector's. pnpm 11.12.0 and 11.13.0 wrote these locks, and 11.13.0 failed so
     * over ^1, when run on it.
     */
    @Test
    void convergesAnEmptyRangeOverrideFrom11130() {
        assertThat(installConverging("1.2.3", "11.13.0")).isEqualTo(convergedLock("^2.0.0", "2.0.0", "1.2.3"));
        assertThat(installConverging("1.2.3", "11.12.0")).isEqualTo(convergedLock("1.2.3", "1.2.3", "1.2.3"));
        assertThatThrownBy(() -> installConverging("^1", "11.13.0"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_CONVERGENCE_OVERRIDE");
              assertThat(e.getMessage()).isEqualTo("The value of the convergence override \"foo@\" must be an " +
                                                   "exact version, but got \"^1\"");
          });
        assertThat(installConverging("^1", "11.12.0")).contains("        specifier: ^1\n        version: 1.2.3\n");
        Map<String, String> parentChild = Collections.singletonMap("bar>foo@", "1.2.3");
        assertThatThrownBy(() -> ParseOverrides.parseOverrides(parentChild, new LinkedHashMap<>(), release("11.13.0")))
          .hasMessage("Cannot use an empty range in the \"bar>foo@\" selector: convergence overrides (\"pkg@\") " +
                      "cannot be combined with parent>child selectors");
        assertThat(ParseOverrides.parseOverrides(parentChild, new LinkedHashMap<>(), release("11.12.0"))).hasSize(1);
    }

    // c peers on q
    private static final RegistryMetadata PEERING_ON_Q = versions(
      new String[]{"c", "1.0.0", "", "\"q\":\"*\""},
      new String[]{"q", "1.0.0", "", ""});

    // the lock-only install of a workspace whose a injects b, b depending on c and giving it q as a devDependency,
    // where peers are neither installed nor hoisted, so that a's copy of b has c without q
    private static String installInjectingFewerPeers(String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\"}")
          .manifest("a", "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"b\":\"workspace:*\"}," +
                         "\"dependenciesMeta\":{\"b\":{\"injected\":true}}}")
          .manifest("b", "{\"name\":\"b\",\"version\":\"1.0.0\",\"dependencies\":{\"c\":\"1.0.0\"}," +
                         "\"devDependencies\":{\"q\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "packages:\n  - a\n  - b\nminimumReleaseAge: 0\nautoInstallPeers: false\n" +
                                       "dedupePeerDependents: false\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), PEERING_ON_Q).writeLock();
    }

    /**
     * installing/deps-resolver dedupeInjectedDeps.ts getDedupeMap (11.13.0): a's copy of b, whose c lacks the peer q
     * b's own c has, is a link to b, b's c 1.0.0(q@1.0.0) being of the same package and having more dependencies;
     * before, only a copy whose children are b's own is, and this one stays file:b. pnpm 11.12.0 and 11.13.0 wrote
     * these locks when run on it.
     */
    @Test
    void dedupesAnInjectedChildTheTargetCoversFrom11130() {
        String b = "\n\n  b:\n    dependencies:\n      c:\n        specifier: 1.0.0\n" +
                   "        version: 1.0.0(q@1.0.0)\n    devDependencies:\n      q:\n        specifier: 1.0.0\n" +
                   "        version: 1.0.0\n\npackages:\n\n";
        String c = "  c@1.0.0:\n    resolution: {integrity: sha512-c100}\n    peerDependencies:\n      q: '*'\n\n" +
                   "  q@1.0.0:\n    resolution: {integrity: sha512-q100}\n\nsnapshots:\n\n";
        String head = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: false\n" +
                      "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n\n  a:\n    dependencies:\n" +
                      "      b:\n        specifier: workspace:*\n";
        String tail = "  c@1.0.0(q@1.0.0):\n    dependencies:\n      q: 1.0.0\n\n  q@1.0.0: {}\n";
        assertThat(installInjectingFewerPeers("11.13.0")).isEqualTo(head + "        version: link:../b" + b + c +
                                                                   tail);
        assertThat(installInjectingFewerPeers("11.12.0")).isEqualTo(head + "        version: file:b\n" +
          "    dependenciesMeta:\n      b:\n        injected: true" + b + "  b@file:b:\n" +
          "    resolution: {directory: b, type: directory}\n\n" + c + "  b@file:b:\n    dependencies:\n" +
          "      c: 1.0.0\n    transitivePeerDependencies:\n      - q\n\n  c@1.0.0: {}\n\n" + tail);
    }

    // whether the store controller of the install command (or else of pnpm's own bootstrap) fetches foo's full
    // packument to resolve it, under the settings
    private static boolean fetchesFooInFull(String workspace, String release, boolean install) {
        List<Boolean> fullMetadata = new ArrayList<>();
        RegistryMetadata foo = new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean full) {
                fullMetadata.add(full);
                return "{\"name\":\"foo\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":" +
                       "\"foo\",\"version\":\"1.0.0\",\"dist\":{\"integrity\":\"sha512-foo\",\"tarball\":\"" +
                       REGISTRY + "foo/-/foo-1.0.0.tgz\"}}}}";
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
        PnpmOptions options = options("minimumReleaseAge: 0\n" + workspace, null, null, Collections.emptyMap(),
          release);
        CreateNewStoreController stores = new CreateNewStoreController(foo, NOW);
        NpmResolver resolver = install ? stores.createInstallStoreController(options) :
          stores.createStoreController(options);
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.projectDir = "";
        opts.lockfileDir = "";
        resolver.resolveNpm(new ResolveDependencies.WantedDependency("foo", "1.0.0", false, false), opts, null);
        assertThat(fullMetadata).hasSize(1);
        return fullMetadata.get(0);
    }

    /**
     * store/connection-manager createNewStoreController.ts shouldFetchFullMetadata (11.13.0): the full packument is
     * fetched under trustPolicy no-downgrade though the registry has a time field, for a libc of '' set, and for
     * pnpm's own store where libc is set; before, no-downgrade fetches it only without the time field, the install
     * command passes {@code libc && true}, which is '', and pnpm's own store reads no libc.
     */
    @Test
    void fetchesFullMetadataForLibcAndNoDowngradeFrom11130() {
        String noDowngrade = "trustPolicy: no-downgrade\nregistrySupportsTimeField: true\n";
        assertThat(fetchesFooInFull(noDowngrade, "11.13.0", true)).isTrue();
        assertThat(fetchesFooInFull(noDowngrade, "11.12.0", true)).isFalse();
        String emptyLibc = "supportedArchitectures:\n  libc: ''\n";
        assertThat(fetchesFooInFull(emptyLibc, "11.13.0", true)).isTrue();
        assertThat(fetchesFooInFull(emptyLibc, "11.12.0", true)).isFalse();
        String musl = "supportedArchitectures:\n  libc: [musl]\n";
        assertThat(fetchesFooInFull(musl, "11.12.0", true)).isTrue();
        assertThat(fetchesFooInFull(musl, "11.13.0", false)).isTrue();
        assertThat(fetchesFooInFull(musl, "11.12.0", false)).isFalse();
        assertThat(fetchesFooInFull("", "11.13.0", true)).isFalse();
    }

    /**
     * workspace/workspace-manifest-reader versioning.ts assertValidWorkspaceManifestVersioning (11.13.0): an invalid
     * versioning setting fails; before, the key is not read. pnpm 11.13.0 failed so, and 11.12.0 wrote the lock, when
     * run on the lane of foo.
     */
    @Test
    void validatesTheVersioningSettingsFrom11130() {
        String mainLane = "versioning:\n  lanes:\n    foo: Main\n";
        assertThatThrownBy(() -> options(mainLane, null, null, Collections.emptyMap(), "11.13.0"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_WORKSPACE_CONFIGURATION");
              assertThat(e.getMessage()).isEqualTo("Invalid versioning.lanes entry for foo: \"main\" is the reserved " +
                                                   "default lane. Remove the entry instead.");
          });
        options(mainLane, null, null, Collections.emptyMap(), "11.12.0");
        assertThatThrownBy(() -> options("versioning: []\n", null, null, Collections.emptyMap(), "11.13.0"))
          .hasMessage("Expected versioning field to be an object, but found - array");
        assertThatThrownBy(() -> options("versioning:\n  maxBump: 3\n", null, null, Collections.emptyMap(),
          "11.13.0"))
          .hasMessage("Expected versioning.maxBump to be one of patch, minor, major, but found - 3");
        assertThatThrownBy(() -> options("versioning:\n  epics:\n    - lead: a\n      packages: []\n", null, null,
          Collections.emptyMap(), "11.13.0"))
          .hasMessage("Expected versioning.epics entry for \"a\" to have a non-empty \"packages\" array of selector " +
                      "strings");
        options("versioning:\n  fixed: [[a, b]]\n  changelog:\n    storage: registry\n", null, null,
          Collections.emptyMap(), "11.13.0");
    }

    // y 1.2.0 and 2.0.0, which a and b depend on; x peers on y by the spec
    private static RegistryMetadata peeringBySpec(String spec) {
        return versions(
          new String[]{"a", "1.0.0", "\"y\":\"1.2.0\"", ""},
          new String[]{"b", "1.0.0", "\"y\":\"2.0.0\"", ""},
          new String[]{"y", "1.2.0", "", ""},
          new String[]{"y", "2.0.0", "", ""},
          new String[]{"x", "1.0.0", "", "\"y\":\"" + spec + "\""});
    }

    // the lock-only install of a root depending on these under the settings
    private static String installPeeringBySpec(String dependencies, String settings, String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\"," + dependencies + "}")
          .file("pnpm-workspace.yaml", "minimumReleaseAge: 0\n" + settings)
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), peeringBySpec("npm:y@^1.0.0")).writeLock();
    }

    /**
     * deps/peer-range getPeerVersionRange and isAcceptablePeerSpec (11.14.0), as installing/deps-resolver
     * hoistPeers.ts, resolvePeers.ts and validatePeerDependencies.ts read them: x's peer spec npm:y@^1.0.0 is the range
     * ^1.0.0, which hoists y 1.2.0 (as autoInstallPeersFromHighestMatch passes on the spec semver-range-intersect
     * rejects) and which the root's y 1.2.0 satisfies under strictPeerDependencies, and a project may peer by it;
     * before, it is no range, y 2.0.0, the highest, is hoisted, the root's y is a bad peer, and the project's spec is
     * invalid. pnpm 11.13.1 and 11.14.0 wrote these locks, and 11.13.1 failed so after writing it and on the root's
     * peer, when run on them.
     */
    @Test
    void readsAPeerRangeOutOfItsSpecifierFrom11140() {
        assertThat(hoistFoo("workspace:^1.0.0", "11.14.0")).containsEntry("foo", "1.2.0");
        assertThat(hoistFoo("workspace:^1.0.0", "11.13.1")).containsEntry("foo", "2.0.0");
        String highestMatch = "autoInstallPeersFromHighestMatch: true\n";
        String abx = "\"dependencies\":{\"a\":\"1.0.0\",\"b\":\"1.0.0\",\"x\":\"1.0.0\"}";
        assertThat(installPeeringBySpec(abx, highestMatch, "11.14.0"))
          .contains("      x:\n        specifier: 1.0.0\n        version: 1.0.0(y@1.2.0)\n");
        assertThat(installPeeringBySpec(abx, highestMatch, "11.13.1"))
          .contains("      x:\n        specifier: 1.0.0\n        version: 1.0.0(y@2.0.0)\n");
        String xy = "\"dependencies\":{\"x\":\"1.0.0\",\"y\":\"1.2.0\"}";
        assertThat(installPeeringBySpec(xy, "strictPeerDependencies: true\n", "11.14.0"))
          .contains("      x:\n        specifier: 1.0.0\n        version: 1.0.0(y@1.2.0)\n");
        assertThatThrownBy(() -> installPeeringBySpec(xy, "strictPeerDependencies: true\n", "11.13.1"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_PEER_DEP_ISSUES"));
        Map<String, @Nullable Object> project = obj("{'name':'root','peerDependencies':{'y':'npm:y@^1.0.0'}}");
        ResolveDependencyTree.validatePeerDependencies("", project, release("11.14.0"));
        assertThatThrownBy(() -> ResolveDependencyTree.validatePeerDependencies("", project, release("11.13.1")))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getMessage()).isEqualTo("The peerDependencies field named 'y' of package 'root' has an " +
                                                   "invalid value: 'npm:y@^1.0.0'");
              assertThat(e.getHint()).isEqualTo("The values in peerDependencies should be either a valid semver " +
                                                "range, a `workspace:` spec, or a `catalog:` spec");
          });
        assertThatThrownBy(() -> ResolveDependencyTree.validatePeerDependencies("",
          obj("{'name':'root','peerDependencies':{'y':'latest'}}"), release("11.14.0")))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getHint()).startsWith(
            "The values in peerDependencies should be a valid semver range, a `workspace:`/`catalog:` spec, or a " +
            "dependency specifier"));
        assertThat(ReadPackageHook.getPeerVersionRange("workspace:^1 ||gh:^2|| file:../x", release("11.14.0")))
          .isEqualTo("^1 || ^2 || *");
    }

    // a lock of foo from github:u/foo at a commit, locked as pnpm 11 locks it
    private static final String GIT_HOSTED_LOCK =
      "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
      "importers:\n\n  .:\n    dependencies:\n      foo:\n" +
      "        specifier: github:u/foo#98e8ff1da1a89f93d1397a24d7413ed15421c139\n        version: " + CODELOAD +
      "\n\npackages:\n\n  foo@" + CODELOAD + ":\n    resolution: {gitHosted: true, integrity: sha512-x, tarball: " +
      CODELOAD + "}\n    version: 1.0.0\n\nsnapshots:\n\n  foo@" + CODELOAD + ": {}\n";

    // the lock-only install over that lock of a project wanting foo by the specifier
    private static String installGitHosted(String specifier, String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(GIT_HOSTED_LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"foo\":\"" + specifier + "\"}}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(NOW)
          .release(release(release))
          .build(), NOT_FOUND).writeLock();
    }

    /**
     * lockfile/verification gitSpecifiersAreEquivalent.ts dependencySpecifiersAreEqual (11.14.0): the lock's
     * github:u/foo#sha is the manifest's u/foo#sha and git+https://github.com/u/foo.git#sha, so the project is up to
     * date and its lock kept; before, it is re-resolved from git, which the engine does not do.
     */
    @Test
    void takesEquivalentGitSpecifiersAsEqualFrom11140() {
        String sha = "98e8ff1da1a89f93d1397a24d7413ed15421c139";
        assertThat(installGitHosted("u/foo#" + sha, "11.14.0")).isEqualTo(GIT_HOSTED_LOCK);
        assertThat(installGitHosted("git+https://github.com/u/foo.git#" + sha, "11.14.0")).isEqualTo(GIT_HOSTED_LOCK);
        assertThatThrownBy(() -> installGitHosted("u/foo#" + sha, "11.13.1")).isInstanceOf(CannotKnow.class);
        assertThat(Verification.dependencySpecifiersAreEqual("gitlab:o/r", "git://GitLab.com/o/r.git")).isTrue();
        assertThat(Verification.dependencySpecifiersAreEqual("o/r#x", "https://example.com/o/r#x")).isFalse();
        assertThat(Verification.dependencySpecifiersAreEqual("o/r#x", "https://example.com/o/r.git#x")).isFalse();
        assertThat(Verification.dependencySpecifiersAreEqual("github:o/r#x#y", "o/r#x#y")).isFalse();
    }

    // the lock-only install over the lock of a project wanting wrappy 1.0.2
    private static String installWrappyOver(String lock, String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(NOW)
          .release(release(release))
          .build(), WRAPPY_ONLY).writeLock();
    }

    /**
     * lockfile/fs write.ts writeLockfileDoc (11.14.0): a lock that already reads as the one written, but for its BOM
     * and CRLF line ends, keeps them; before, it is written over, LF and without a BOM. pnpm 11.13.1 and 11.14.0 did
     * so when run on such a lock.
     */
    @Test
    void leavesAnUnchangedLockAsItIsFrom11140() {
        String bomCrLf = "﻿" + WRAPPY_LOCK.replace("\n", "\r\n");
        assertThat(installWrappyOver(bomCrLf, "11.14.0")).isEqualTo(bomCrLf);
        assertThat(installWrappyOver(bomCrLf, "11.13.1")).isEqualTo(WRAPPY_LOCK);
        String stale = bomCrLf.replace("specifier: 1.0.2", "specifier: ^1.0.0");
        assertThat(installWrappyOver(stale, "11.14.0")).isEqualTo(WRAPPY_LOCK);
    }

    // p 1.0.0 and 1.5.0; d peers on p
    private static final RegistryMetadata PINNED_PEER = versions(
      new String[]{"p", "1.0.0", "", ""},
      new String[]{"p", "1.5.0", "", ""},
      new String[]{"d", "1.0.0", "", "\"p\":\"*\""});

    // proj's d, as the lock-only install of the root with p =1.0.0 and of proj with d writes it
    private static String installBesideAnEqualsPin(String release) {
        String lock = new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"p\":\"=1.0.0\"}}")
          .manifest("proj", "{\"name\":\"proj\",\"version\":\"1.0.0\",\"dependencies\":{\"d\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "packages:\n  - proj\nminimumReleaseAge: 0\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), PINNED_PEER).writeLock();
        int proj = lock.indexOf("\n  proj:\n");
        return lock.substring(proj, lock.indexOf("\n\npackages:", proj));
    }

    /**
     * resolving/npm-resolver whichVersionIsPinned.ts (11.14.0): =1.0.0 pins the patch, so that the root's p saves as
     * 1.0.0, which proj's missing peer p is hoisted as; before, it pins nothing, the root's p saves as ^1.0.0, and
     * proj's p resolves to 1.5.0. pnpm 11.13.1 and 11.14.0 wrote these importers when run on it.
     */
    @Test
    void pinsAVersionPrefixedWithEqualsAsTheVersionFrom11140() {
        assertThat(installBesideAnEqualsPin("11.14.0")).endsWith("        version: 1.0.0(p@1.0.0)");
        assertThat(installBesideAnEqualsPin("11.13.1")).endsWith("        version: 1.0.0(p@1.5.0)");
        assertThat(NpmResolver.whichVersionIsPinned("=1.2.3", release("11.14.0"))).isEqualTo("patch");
        assertThat(NpmResolver.whichVersionIsPinned("=1.2", release("11.14.0"))).isEqualTo("minor");
        assertThat(NpmResolver.whichVersionIsPinned("=1.2.3", release("11.13.1"))).isNull();
        assertThat(NpmResolver.whichVersionIsPinned("catalog:=1", release("11.14.0"))).isNull();
    }

    // the install as runs over the WRAPPY lock of a project whose packageManager field names pnpm at the version,
    // the registry holding pnpm and @pnpm/exe of it
    private static PnpmLockRegenerator handingOverTo(String runs, String version) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(WRAPPY_LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}," +
                        "\"packageManager\":\"pnpm@" + version + "\"}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(NOW)
          .host(Host.builder().nodeVersion("v24.21.0").os("darwin").cpu("arm64").build())
          .release(release(runs))
          .build(), versions(new String[]{"pnpm", version, "", ""}, new String[]{"@pnpm/exe", version, "", ""}));
    }

    /**
     * pnpm switchCliVersion.ts and engine/pm installPnpm.ts assertReleaseIsInstallable (11.14.0): pnpm refuses to
     * hand the install to 11.12.0 or 11.13.0, the env document's or the one it resolves for the packageManager field;
     * before, it hands the install over, which the engine does not follow.
     */
    @Test
    void refusesToSwitchToABrokenReleaseFrom11140() {
        String integrity = "{integrity: sha512-x}";
        assertThatThrownBy(() -> switching("11.14.0", "11.12.0", integrity).install())
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_BROKEN_PNPM_RELEASE");
              assertThat(e.getMessage()).isEqualTo("pnpm v11.12.0 is a broken release and cannot be installed");
              assertThat(e.getHint()).startsWith("Its \"@pnpm/exe\" build shipped without a binary and does not run.");
          });
        assertThatThrownBy(() -> switching("11.13.1", "11.12.0", integrity).install())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("pnpm 11.12.0, which pnpm 11.13.1 downloads and hands the install over to");
        assertThatThrownBy(() -> switching("11.14.0", "11.11.0", integrity).install())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("pnpm 11.11.0, which pnpm 11.14.0 downloads and hands the install over to");
        assertThatThrownBy(() -> handingOverTo("11.14.0", "11.13.0").install())
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getMessage()).isEqualTo("pnpm v11.13.0 is a broken release and cannot be installed"));
        assertThatThrownBy(() -> handingOverTo("11.13.1", "11.13.0").install())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("pnpm 11.13.0, which pnpm 11.13.1 downloads and hands the install over to");
    }

    // x's optional peer o is named only in peerDependenciesMeta; y depends on o; z depends and peers on p
    private static final RegistryMetadata META_PEERS = versions(
      new String[]{"x", "1.0.0", "", "", "\"peerDependenciesMeta\":{\"o\":{\"optional\":true}}"},
      new String[]{"y", "1.0.0", "\"o\":\"1.0.0\"", ""},
      new String[]{"o", "1.0.0", "", ""},
      new String[]{"z", "1.0.0", "\"p\":\"1.0.0\"", "\"p\":\"*\""},
      new String[]{"p", "1.0.0", "", ""});

    // the lock-only install of a root with x and y
    private static String installMetaPeers(String release) {
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"x\":\"1.0.0\",\"y\":\"1.0.0\"}}")
          .file("pnpm-workspace.yaml", "minimumReleaseAge: 0\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), META_PEERS).writeLock();
    }

    // the importers and snapshots the lock-only install writes of a workspace whose a and b depend on z, the one
    // project of them that gives z its peer p being resolved first or not, peers neither installed nor hoisted
    private static String installFirstOccurrence(boolean firstGivesP, String release) {
        String withP = "{\"name\":\"N\",\"version\":\"1.0.0\",\"dependencies\":{\"p\":\"1.0.0\",\"z\":\"1.0.0\"}}";
        String withoutP = "{\"name\":\"N\",\"version\":\"1.0.0\",\"dependencies\":{\"z\":\"1.0.0\"}}";
        String lock = new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .manifest("", "{\"name\":\"root\",\"version\":\"1.0.0\"}")
          .manifest("a", (firstGivesP ? withP : withoutP).replace("N", "a"))
          .manifest("b", (firstGivesP ? withoutP : withP).replace("N", "b"))
          .file("pnpm-workspace.yaml", "packages:\n  - a\n  - b\nminimumReleaseAge: 0\nautoInstallPeers: false\n")
          .env(Collections.emptyMap())
          .now(NOW)
          .release(release(release))
          .build(), META_PEERS).writeLock();
        return lock.substring(lock.indexOf("\nimporters:\n")).replaceAll("\n\npackages:\n(.|\n)*\n\nsnapshots:", "");
    }

    /**
     * installing/deps-resolver resolveDependencies.ts (11.15.0): x's missing peers are those of its resolved package,
     * the optional o named only in peerDependenciesMeta among them, which autoInstallPeers hoists as y's o; before,
     * those of its peerDependencies, which name none. Every occurrence of z takes the peers of the first, which has p
     * as a peer where its project gives it and as its own dependency where not, so that a later occurrence's peers
     * differ from its own manifest's; the lock is the same either way. pnpm 11.14.0 and 11.15.0 wrote these locks
     * when run on them.
     */
    @Test
    void takesMissingPeersFromTheResolvedPackageFrom11150() {
        assertThat(installMetaPeers("11.15.0"))
          .contains("      x:\n        specifier: 1.0.0\n        version: 1.0.0(o@1.0.0)\n")
          .contains("  x@1.0.0(o@1.0.0):\n    optionalDependencies:\n      o: 1.0.0\n");
        assertThat(installMetaPeers("11.14.0"))
          .contains("      x:\n        specifier: 1.0.0\n        version: 1.0.0\n")
          .contains("  x@1.0.0: {}\n");
        for (String release : Arrays.asList("11.14.0", "11.15.0")) {
            assertThat(installFirstOccurrence(true, release)).as(release)
              .isEqualTo("\nimporters:\n\n  .: {}\n\n  a:\n    dependencies:\n      p:\n        specifier: 1.0.0\n" +
                         "        version: 1.0.0\n      z:\n        specifier: 1.0.0\n" +
                         "        version: 1.0.0(p@1.0.0)\n\n  b:\n    dependencies:\n      z:\n" +
                         "        specifier: 1.0.0\n        version: 1.0.0(p@1.0.0)\n\n  p@1.0.0: {}\n\n" +
                         "  z@1.0.0(p@1.0.0):\n    dependencies:\n      p: 1.0.0\n");
            assertThat(installFirstOccurrence(false, release)).as(release)
              .isEqualTo("\nimporters:\n\n  .: {}\n\n  a:\n    dependencies:\n      z:\n        specifier: 1.0.0\n" +
                         "        version: 1.0.0\n\n  b:\n    dependencies:\n      p:\n        specifier: 1.0.0\n" +
                         "        version: 1.0.0\n      z:\n        specifier: 1.0.0\n        version: 1.0.0\n\n" +
                         "  p@1.0.0: {}\n\n  z@1.0.0:\n    dependencies:\n      p: 1.0.0\n");
        }
    }

    // the dependencies of a manifest whose engines.runtime is node, depending on node and bun as runtime: specs, once
    // the runtimeOnFail setting ignore applies
    private static @Nullable Object ignoringRuntimes(String release) {
        Map<String, @Nullable Object> manifest = obj("{'engines':{'runtime':{'name':'node','version':'22.0.0'}}," +
                                                     "'dependencies':{'node':'runtime:22.0.0','bun':'runtime:1.2.0'," +
                                                     "'foo':'1.0.0'}}");
        PkgManifestUtils.applyRuntimeOnFailOverride(manifest, "ignore", release(release));
        return manifest.get("dependencies");
    }

    /**
     * pkg-manifest/utils convertEnginesRuntimeToDependencies.ts applyRuntimeOnFailOverride (11.15.1): runtimeOnFail
     * ignore removes the runtime: dependency of node, which engines.runtime names, and keeps bun's; before, it removes
     * every runtime's.
     */
    @Test
    void dropsOnlyTheRuntimesTheEnginesFieldNamesFrom11151() {
        assertThat(json(ignoringRuntimes("11.15.1"))).isEqualTo("{'bun':'runtime:1.2.0','foo':'1.0.0'}");
        assertThat(json(ignoringRuntimes("11.15.0"))).isEqualTo("{'foo':'1.0.0'}");
    }
}
