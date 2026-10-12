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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What each Bun release does otherwise than 1.3.10, as {@link BunRelease} answers it, each case from the release's
 * source at its {@code bun-v<release>} tag.
 */
class BunReleaseTest {

    private static final String MS = "\"ms\": [\"ms@2.1.3\", \"\", {}, \"sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9y" +
                                     "YXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA==\"],";

    private static String lock(String header, String packages) {
        return "{\n" +
               "  \"lockfileVersion\": 1,\n" +
               header +
               "  \"workspaces\": {\n" +
               "    \"\": {\n" +
               "      \"name\": \"p\",\n" +
               "      \"dependencies\": {\n" +
               "        \"ms\": \"2.1.3\",\n" +
               "      },\n" +
               "    },\n" +
               "  },\n" +
               "  \"packages\": {\n" +
               "    " + packages + "\n" +
               "  }\n" +
               "}\n";
    }

    private static BunOptions options(String release, String userNpmrc) {
        return BunOptions.load(BunRelease.of(release), null, userNpmrc, null, HostEnvironment.UNKNOWN);
    }

    // a lock loaded as the release and written back as it writes a lock it loaded
    private static String writtenBack(String release, String lock) {
        BunOptions options = options(release, null);
        Lockfile lockfile = TextLockfileReader.load(lock, options);
        int configVersion = lockfile.savedConfigVersion != null ? lockfile.savedConfigVersion : 0;
        return TextLockfileWriter.saveFromBinary(Cleaner.clean(lockfile), configVersion, options.getRelease());
    }

    @Test
    void installsAsTheReleaseThatBuiltTheLst() {
        assertThat(BunReleases.installedAs(null)).isEqualTo(BunRelease.PORTED);
        assertThat(BunReleases.installedAs("1.3.10")).hasToString("1.3.10");
        assertThat(BunReleases.all()).startsWith("1.1.39").contains("1.3.10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.1.38", "1.3.10-canary.1", "9.9.9"})
    void aReleaseThatWritesNoBunLockIsCannotKnow(String version) {
        assertThatThrownBy(() -> BunReleases.installedAs(version))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("what bun " + version + " writes, since the engine reproduces bun ");
    }

    @Test
    void namesTheReleasesItReproducesAsRanges() {
        assertThat(BunReleases.ranges(java.util.Arrays.asList("1.3.0", "1.3.1", "1.3.2", "1.3.14", "1.4.0")))
          .isEqualTo("1.3.0 - 1.3.2, 1.3.14 - 1.4.0");
    }

    /**
     * bun.lock.zig, install_with_manager.zig and ConfigVersion.zig (1.3.2): the lock's configVersion is read and
     * written; before, neither, so that a lock loses it and an invalid one is no error.
     */
    @Test
    void holdsTheConfigVersionFrom132() {
        String lock = lock("  \"configVersion\": 1,\n", MS);
        assertThat(writtenBack("1.3.2", lock)).isEqualTo(lock);
        assertThat(writtenBack("1.3.1", lock)).isEqualTo(lock("", MS));
        String invalid = lock("  \"configVersion\": \"one\",\n", MS);
        assertThatThrownBy(() -> TextLockfileReader.load(invalid, options("1.3.2", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Invalid \"configVersion\". Expected a number");
        assertThat(writtenBack("1.3.1", invalid)).isEqualTo(lock("", MS));
    }

    /**
     * dependency.zig (1.3.2): a git or GitHub spec is told and read as hosted-git-info does; before, by
     * {@code isGitHubRepoPath} and {@code Repository.Hosts}, the GitHub repo split at the last slash.
     */
    @Test
    void tellsGitSpecsAsHostedGitInfoDoesFrom132() {
        BunRelease later = BunRelease.of("1.3.2");
        BunRelease earlier = BunRelease.of("1.3.1");
        assertThat(DependencyVersion.Tag.infer("user/repo#semver:^1.0.0", later)).isEqualTo(DependencyVersion.Tag.GITHUB);
        assertThat(DependencyVersion.Tag.infer("user/repo#semver:^1.0.0", earlier))
          .isEqualTo(DependencyVersion.Tag.DIST_TAG);
        assertThat(DependencyVersion.Tag.infer("user/repo/x", later)).isEqualTo(DependencyVersion.Tag.DIST_TAG);
        assertThat(DependencyVersion.Tag.infer("user/repo/x", earlier)).isEqualTo(DependencyVersion.Tag.GITHUB);
        assertThat(DependencyVersion.Tag.infer("https://www.github.com/user/repo", later))
          .isEqualTo(DependencyVersion.Tag.GIT);
        assertThat(DependencyVersion.Tag.infer("https://www.github.com/user/repo", earlier))
          .isEqualTo(DependencyVersion.Tag.TARBALL);
        assertThat(DependencyVersion.Tag.infer("ssh://git@gitlab.com/user/repo.git", earlier))
          .isEqualTo(DependencyVersion.Tag.GIT);

        Repository repository = requireNonNull(DependencyVersion.parse("r", null, "github:user/repo.git#v1", earlier))
          .repository();
        assertThat(repository.owner).isEqualTo("user");
        assertThat(repository.repo).isEqualTo("repo.git");
        assertThat(repository.committish).isEqualTo("v1");
        Repository fromUrl = requireNonNull(DependencyVersion.parse("r", null, "git://github.com/user/repo.git", earlier))
          .repository();
        assertThat(fromUrl.repo).isEqualTo("repo");
        assertThat(requireNonNull(DependencyVersion.parse("r", null, "github:user/repo.git#v1", later)).repository().repo)
          .isEqualTo("repo");
    }

    /**
     * dependency.rs {@code Version::eql} and CatalogMap.rs {@code locate} and {@code parse_append} (1.4.0): catalog
     * specs compare by catalog, the default catalog answers to both names, and a name in both default catalogs fails.
     */
    @Test
    void comparesAndLooksUpCatalogsByNameFrom140() {
        BunRelease later = BunRelease.of("1.4.0");
        BunRelease earlier = BunRelease.of("1.3.14");
        DependencyVersion plain = requireNonNull(DependencyVersion.parse("ms", null, "catalog:", later));
        DependencyVersion named = requireNonNull(DependencyVersion.parse("ms", null, "catalog:default", later));
        assertThat(plain.eql(named, later)).isFalse();
        assertThat(plain.eql(named, earlier)).isTrue();

        Log log = new Log();
        CatalogMap catalogs = new CatalogMap();
        catalogs.parseAppend(log, Json.parse("{\"catalog\":{\"ms\":\"2.1.3\"},\"catalogs\":{\"default\":{\"debug\":\"4\"}}}"),
          later);
        assertThat(catalogs.get("default", "ms", later)).isNotNull();
        assertThat(catalogs.get("default", "ms", earlier)).isNull();
        assertThat(catalogs.get("", "debug", later)).isNotNull();
        assertThat(catalogs.get("", "debug", earlier)).isNull();

        String both = "{\"catalog\":{\"ms\":\"2.1.3\"},\"catalogs\":{\"default\":{\"ms\":\"2.0.0\"}}}";
        assertThatThrownBy(() -> new CatalogMap().parseAppend(log, Json.parse(both), later))
          .hasMessage("InstallFailed");
        assertThat(log.errors).contains("\"ms\" is defined in both \"catalog\" and \"catalogs.default\"; keep one of them");
        new CatalogMap().parseAppend(new Log(), Json.parse(both), earlier);
    }

    /**
     * PackageManager.rs {@code init} (1.4.0): bunfig.toml's registry wins over the .npmrc files; before, they win.
     */
    @Test
    void letsBunfigsRegistryWinFrom140() {
        String bunfig = "[install]\nregistry = \"https://registry.yarnpkg.com/\"\n";
        String npmrc = "registry=https://registry.npmmirror.com/\n";
        assertThat(BunOptions.load(BunRelease.of("1.4.0"), bunfig, npmrc, null, HostEnvironment.UNKNOWN)
          .scopeForPackageName("ms")).isEqualTo("https://registry.yarnpkg.com/");
        assertThat(BunOptions.load(BunRelease.of("1.3.14"), bunfig, npmrc, null, HostEnvironment.UNKNOWN)
          .scopeForPackageName("ms")).isEqualTo("https://registry.npmmirror.com/");
    }

    /**
     * npm.rs {@code Scope::set_url} (1.4.0): a registry is its WHATWG serialization; bunfig.rs (1.4.1): offline reads
     * bun's manifest cache.
     */
    @Test
    void canonicalizesTheRegistryFrom140AndReadsTheCacheOfflineFrom141() {
        String npmrc = "registry=https://NPM.Example.com:443/x/../\n";
        assertThat(BunOptions.load(BunRelease.of("1.4.0"), null, npmrc, null, HostEnvironment.UNKNOWN)
          .scopeForPackageName("ms")).isEqualTo("https://npm.example.com/");
        assertThat(BunOptions.load(BunRelease.of("1.3.14"), null, npmrc, null, HostEnvironment.UNKNOWN)
          .scopeForPackageName("ms")).isEqualTo("https://NPM.Example.com:443/x/../");
        String offline = "[install]\noffline = true\n";
        assertThat(BunOptions.load(BunRelease.of("1.4.1"), offline, null, null, HostEnvironment.UNKNOWN)
          .isReadsTheManifestCache()).isTrue();
        assertThat(BunOptions.load(BunRelease.of("1.4.0"), offline, null, null, HostEnvironment.UNKNOWN)
          .isReadsTheManifestCache()).isFalse();
    }

    /**
     * bun.lock.zig (1.3.7): a {@code ""} registry is the package's scope registry; before, npmjs.
     */
    @Test
    void expandsAnEmptyRegistryToTheScopesFrom137() {
        String npmrc = "registry=https://registry.npmmirror.com/\n";
        Lockfile later = TextLockfileReader.load(lock("", MS), options("1.3.7", npmrc));
        assertThat(later.packages.get(1).resolution.url).isEqualTo("https://registry.npmmirror.com/ms/-/ms-2.1.3.tgz");
        Lockfile earlier = TextLockfileReader.load(lock("", MS), options("1.3.6", npmrc));
        assertThat(earlier.packages.get(1).resolution.url).isEqualTo("https://registry.npmjs.org/ms/-/ms-2.1.3.tgz");
    }

    /**
     * bun.lock.zig (1.3.10): a git, github or tarball package carries its integrity after its other parts; before,
     * it is neither read nor written.
     */
    @Test
    void carriesTheIntegrityOfAGitPackageFrom1310() {
        String github = "\"ms\": [\"ms@github:vercel/ms#1c6264b\", {}, \"vercel-ms-1c6264b\", \"sha512-9OuuhG9wvxtRpm" +
                        "fwdRn3gFoFF4pJl5wXABO+oVzR0hZo/vNB0/gvv2z5RdQICtXAXNE4HPa43xv3XTHPHjjoFg==\"],";
        String withIntegrity = lock("  \"configVersion\": 1,\n", github).replace("\"ms\": \"2.1.3\"",
          "\"ms\": \"github:vercel/ms#2.1.3\"");
        assertThat(writtenBack("1.3.10", withIntegrity)).isEqualTo(withIntegrity);
        assertThat(writtenBack("1.3.9", withIntegrity))
          .isEqualTo(withIntegrity.replaceFirst(", \"sha512-[^\"]*\"]", "]"));
    }

    /**
     * npm.zig PackageManifest.parse (1.3.14): a name peerDependenciesMeta marks optional and the peers do not list is
     * an optional peer of {@code "*"}, at the front of the peers; the peers no longer reset the bundled names.
     */
    @Test
    void impliesAnOptionalPeerOfAPeerDependenciesMetaNameFrom1314() {
        String packument = "{\"name\":\"debug\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"versions\":{\"4.4.3\":" +
                           "{\"name\":\"debug\",\"version\":\"4.4.3\",\"dependencies\":{\"ms\":\"^2.1.3\"}," +
                           "\"bundleDependencies\":[\"ms\"],\"peerDependencies\":{\"p\":\"1\"}," +
                           "\"peerDependenciesMeta\":{\"supports-color\":{\"optional\":true}}," +
                           "\"dist\":{\"tarball\":\"https://registry.npmjs.org/debug/-/debug-4.4.3.tgz\"}}}," +
                           "\"dist-tags\":{\"latest\":\"4.4.3\"}}";
        PackageManifest.PackageVersion later = version(BunRelease.of("1.3.14"), packument);
        assertThat(later.peerDependencies).containsExactly("supports-color", "*", "p", "1");
        assertThat(later.nonOptionalPeerDependenciesStart).isEqualTo(1);
        assertThat(later.bundledDependencies).containsExactly(Wyhash11.stringHash("ms"));
        PackageManifest.PackageVersion earlier = version(BunRelease.of("1.3.13"), packument);
        assertThat(earlier.peerDependencies).containsExactly("p", "1");
        assertThat(earlier.nonOptionalPeerDependenciesStart).isEqualTo(0);
        assertThat(earlier.bundledDependencies).isEmpty();
    }

    @Test
    void aPackumentWithTheMetaAloneHasItsPeerFrom1314() {
        String packument = "{\"name\":\"debug\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"versions\":{\"4.4.3\":" +
                           "{\"name\":\"debug\",\"version\":\"4.4.3\"," +
                           "\"peerDependenciesMeta\":{\"supports-color\":{\"optional\":true},\"x\":{}}," +
                           "\"dist\":{\"tarball\":\"https://registry.npmjs.org/debug/-/debug-4.4.3.tgz\"}}}," +
                           "\"dist-tags\":{\"latest\":\"4.4.3\"}}";
        assertThat(version(BunRelease.of("1.3.14"), packument).peerDependencies)
          .containsExactly("supports-color", "*");
        assertThat(version(BunRelease.of("1.3.13"), packument).peerDependencies).isEmpty();
    }

    /**
     * lockfile/Package.zig parseWithJSON (1.3.14): a workspace's meta-only name is an optional peer of {@code "*"}.
     */
    @Test
    void aMembersMetaOnlyNameIsAnOptionalPeerFrom1314() {
        String manifest = "{\"name\":\"a\",\"peerDependencies\":{\"p\":\"1\"},\"peerDependenciesMeta\":" +
                          "{\"q\":{\"optional\":true},\"p\":{\"optional\":true}}}";
        assertThat(peers("1.3.14", manifest)).containsExactly("p 1 optional", "q * optional");
        assertThat(peers("1.3.13", manifest)).containsExactly("p 1 optional");
    }

    /**
     * WorkspaceMap.zig processNamesArray (1.3.0): a later {@code !} pattern takes a glob's match out of the workspaces.
     */
    @Test
    void aLaterNegationExcludesAWorkspaceFrom130() {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("packages/a", "{\"name\":\"a\"}");
        manifests.put("packages/b", "{\"name\":\"b\"}");
        List<Json.Expr> globs = Json.parse("[\"packages/*\", \"!packages/a\"]").items();
        Workspaces later = new Workspaces(BunRelease.of("1.3.0"));
        later.processNamesArray(new Log(), globs, manifests);
        assertThat(later.map).containsOnlyKeys("packages/b");
        Workspaces earlier = new Workspaces(BunRelease.of("1.2.23"));
        earlier.processNamesArray(new Log(), globs, manifests);
        assertThat(earlier.map).containsOnlyKeys("packages/a", "packages/b");
    }

    private static final String INTEGRITY = "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xA" +
                                            "OtHnSO/tXtF3WRTlA==";

    private static String packument(String name, String peers) {
        return "{\"name\":\"" + name + "\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":" +
               "\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"" + name + "\",\"version\":\"1.0.0\"," +
               "\"peerDependencies\":" + peers + ",\"dist\":{\"tarball\":\"https://registry.npmjs.org/" + name +
               "/-/" + name + "-1.0.0.tgz\",\"integrity\":\"" + INTEGRITY + "\"}}}}";
    }

    /**
     * install_with_manager.zig (1.3.11): the peer queue is processed until it is empty before the wait for the peers,
     * so that q is resolved: the peer of p, which a's peer adds with nothing to fetch once the failed optional p has
     * loaded p's packument. a, a dev dependency, is asked for first, so that its peer p waits for the peers.
     */
    @Test
    void resolvesThePeersOfAPeerItAddsWithNothingToFetchFrom1311() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n" +
                      "      \"name\": \"root\",\n    },\n  },\n  \"packages\": {}\n}\n";
        String manifest = "{\"name\":\"root\",\"devDependencies\":{\"a\":\"1.0.0\"},\"optionalDependencies\":" +
                          "{\"p\":\"^2.0.0\"}}";
        ManifestSource registry = (url, name, extended) -> "a".equals(name) ? packument("a", "{\"p\":\"^1.0.0\"}") :
                "p".equals(name) ? packument("p", "{\"q\":\"^1.0.0\"}") : "q".equals(name) ? packument("q", "{}") :
                        null;
        BunLockRegenerator.Inputs.InputsBuilder inputs = BunLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifest("", manifest)
          .now(java.time.Instant.parse("2026-10-11T00:00:00Z"));
        org.openrewrite.javascript.internal.LockFileRegeneration.Result later = new BunLockRegenerator(
          inputs.release(BunRelease.of("1.3.11")).build(), registry).regenerate();
        assertThat(later.getLockFileContent()).as(String.valueOf(later.getErrorMessage()))
          .contains("\"p\": [\"p@1.0.0\"").contains("\"q\": [\"q@1.0.0\"");
        // 1.3.10 writes p without its peer q, a lock its own reader rejects on the next install
        org.openrewrite.javascript.internal.LockFileRegeneration.Result earlier = new BunLockRegenerator(
          inputs.release(BunRelease.PORTED).build(), registry).regenerate();
        assertThat(requireNonNull(earlier.getFailure()).getKind())
          .isEqualTo(org.openrewrite.javascript.internal.LockFileRegeneration.Kind.SECOND_INSTALL);
        assertThat(earlier.getFailure().getDetail()).contains("Failed to resolve peer dependency 'q' for package 'p'");
        assertThat(earlier.getFailure().getWritten()).contains("\"p\": [\"p@1.0.0\"").doesNotContain("\"q\": [");
    }

    private static final String EMPTY_ROOT = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n  \"workspaces\": " +
                                            "{\n    \"\": {\n      \"name\": \"root\",\n    },\n  },\n  \"packages\": {}\n}\n";

    private static org.openrewrite.javascript.internal.LockFileRegeneration.Result install(String release,
                                                                                            String manifest) {
        ManifestSource registry = (url, name, extended) -> "a".equals(name) ? packument("a", "{\"b\":\"^1.0.0\"}") :
                "b".equals(name) ? packument("b", "{\"c\":\"^1.0.0\"}") : "c".equals(name) ? packument("c", "{}") :
                        "p".equals(name) ? packument("p", "{\"q\":\"^1.0.0\"}") :
                                "q".equals(name) ? packument("q", "{}") : null;
        return new BunLockRegenerator(BunLockRegenerator.Inputs.builder()
          .lock(EMPTY_ROOT)
          .manifest("", manifest)
          .now(java.time.Instant.parse("2026-10-11T00:00:00Z"))
          .release(BunRelease.of(release))
          .build(), registry).regenerate();
    }

    /**
     * install_with_manager.zig's wait for the peers: a pass that leaves the peer c of the peer b queued with no request
     * pending never ends from 1.2.21, whose --lockfile-only fetches no tarball for b, until 1.3.10; installing from a
     * lock whose root lists nothing, as from scratch.
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.2.21", "1.3.0", "1.3.9"})
    void neverFinishesWhereAPeerWaitLeavesPeersQueuedWithNothingPending(String release) {
        org.openrewrite.javascript.internal.LockFileRegeneration.Result result =
          install(release, "{\"name\":\"root\",\"dependencies\":{\"a\":\"1.0.0\"}}");
        assertThat(requireNonNull(result.getFailure()).getKind())
          .isEqualTo(org.openrewrite.javascript.internal.LockFileRegeneration.Kind.PM_FAILS);
        assertThat(result.getFailure().getDetail()).contains("it never finishes").contains("leaves c queued");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2.20", "1.3.10", "1.3.14"})
    void finishesThatWaitBeforeAndAfter(String release) {
        org.openrewrite.javascript.internal.LockFileRegeneration.Result result =
          install(release, "{\"name\":\"root\",\"dependencies\":{\"a\":\"1.0.0\"}}");
        assertThat(result.getLockFileContent()).as(String.valueOf(result.getErrorMessage()))
          .contains("\"a\": [\"a@1.0.0\"").contains("\"b\": [\"b@1.0.0\"").contains("\"c\": [\"c@1.0.0\"");
    }

    /**
     * install_with_manager.zig (1.3.1): the first pass over the peers happens before the wait, which is entered only
     * with a request pending, so that q, queued by that pass with nothing to fetch, is not resolved until 1.3.11
     * drains the queue; before, the wait resolves it (up to 1.2.20; 1.2.21 to 1.3.0 never finish there).
     */
    @Test
    void entersThePeerWaitWhateverIsPendingBefore131() {
        String manifest = "{\"name\":\"root\",\"devDependencies\":{\"a2\":\"1.0.0\"},\"optionalDependencies\":" +
                          "{\"p\":\"^2.0.0\"}}";
        ManifestSource registry = (url, name, extended) -> "a2".equals(name) ? packument("a2", "{\"p\":\"^1.0.0\"}") :
                "p".equals(name) ? packument("p", "{\"q\":\"^1.0.0\"}") : "q".equals(name) ? packument("q", "{}") :
                        null;
        for (String release : new String[]{"1.2.20", "1.3.1"}) {
            org.openrewrite.javascript.internal.LockFileRegeneration.Result result = new BunLockRegenerator(
              BunLockRegenerator.Inputs.builder().lock(EMPTY_ROOT).manifest("", manifest)
                .now(java.time.Instant.parse("2026-10-11T00:00:00Z")).release(BunRelease.of(release)).build(),
              registry).regenerate();
            String written = result.isSuccess() ? result.getLockFileContent() :
              requireNonNull(result.getFailure()).getWritten();
            if ("1.2.20".equals(release)) {
                assertThat(written).as(String.valueOf(result.getErrorMessage())).contains("\"q\": [\"q@1.0.0\"");
            } else {
                assertThat(written).as(String.valueOf(result.getErrorMessage())).doesNotContain("\"q\": [");
            }
        }
    }

    /**
     * ini.zig (1.3.5): a quoted value takes the environment's substitutions, and {@code ${VAR?}} an unset one as
     * nothing.
     */
    @Test
    void expandsTheEnvironmentInAQuotedNpmrcValueFrom135() {
        HostEnvironment env = HostEnvironment.of(Collections.singletonMap("REG", "https://r.example/"));
        String npmrc = "registry=\"${REG}\"\n";
        assertThat(BunOptions.load(BunRelease.of("1.3.5"), null, npmrc, null, env).getRegistry())
          .isEqualTo("https://r.example/");
        assertThat(BunOptions.load(BunRelease.of("1.3.4"), null, npmrc, null, env).getRegistry()).isEqualTo("${REG}");
        assertThat(Ini.parse("a=x${UNSET?}y\n", env, BunRelease.of("1.3.5")).get("a")).isEqualTo("xy");
        assertThat(Ini.parse("a=x${UNSET?}y\n", env, BunRelease.of("1.3.4")).get("a")).isEqualTo("x${UNSET?}y");
    }

    /**
     * bunfig.zig (1.3.0): minimumReleaseAge is a setting; before, it is not read.
     */
    @Test
    void readsTheMinimumReleaseAgeFrom130() {
        String bunfig = "[install]\nminimumReleaseAge = 60\n";
        assertThat(BunOptions.load(BunRelease.of("1.3.0"), bunfig, null, null, HostEnvironment.UNKNOWN)
          .getMinimumReleaseAgeMs()).isEqualTo(60000.0);
        assertThat(BunOptions.load(BunRelease.of("1.2.23"), bunfig, null, null, HostEnvironment.UNKNOWN)
          .getMinimumReleaseAgeMs()).isNull();
    }

    /**
     * bun.lock.zig (1.3.0): an unnamed root's entry ({@code "@root:"}) reads; before, it is an invalid resolution.
     */
    @Test
    void readsAnUnnamedRootEntryFrom130() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"dependencies\": {\n" +
                      "        \"self\": \"file:.\",\n      },\n    },\n  },\n  \"packages\": {\n" +
                      "    \"self\": [\"@root:\", {}],\n  }\n}\n";
        TextLockfileReader.load(lock, options("1.3.0", null));
        assertThatThrownBy(() -> TextLockfileReader.load(lock, options("1.2.23", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Invalid package resolution");
    }

    /**
     * GlobWalker.zig makeComponent (1.3.4): {@code .x} in a workspaces glob is itself; before, the walker takes it for
     * {@code ..} and walks what the engine cannot see.
     */
    @Test
    void walksATwoCharacterDotComponentAsWrittenFrom134() {
        java.util.Map<String, String> manifests = java.util.Collections.singletonMap(".x/a", "{\"name\":\"a\"}");
        List<Json.Expr> globs = Json.parse("[\".x/*\"]").items();
        Workspaces later = new Workspaces(BunRelease.of("1.3.4"));
        later.processNamesArray(new Log(), globs, manifests);
        assertThat(later.map).containsOnlyKeys(".x/a");
        assertThatThrownBy(() -> new Workspaces(BunRelease.of("1.3.3")).processNamesArray(new Log(), globs, manifests))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("whose .x its glob walker takes for ..");
    }

    // 1.4.x: src/install/lockfile/OverrideMap.rs and override_selector.rs

    // the overrides of a root manifest as the release reads them, its members by location, and what it warns of
    private static OverrideMap overrides(String release, String manifest, java.util.Map<String, String> members,
                                         List<String> warnings) {
        BunOptions options = options(release, null);
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>(members);
        manifests.put("", manifest);
        Json.Expr json = Json.parse(manifest);
        Lockfile lockfile = new Lockfile(options.getRelease());
        Package root = new Package();
        root.parseWithJSON(lockfile, options, new Log(), "", json, Features.MAIN, manifests);
        Workspaces workspaces = new Workspaces(options.getRelease());
        Json.Expr names = json.get("workspaces");
        if (names != null) {
            workspaces.processNamesArray(new Log(), names.items(), manifests);
        }
        OverrideMap overrides = new OverrideMap();
        overrides.parseAppend(lockfile, root, json, workspaces, manifests, warnings);
        return overrides;
    }

    private static OverrideMap overrides(String release, String manifest, List<String> warnings) {
        return overrides(release, manifest, Collections.emptyMap(), warnings);
    }

    // a rule as the lock writes its key and value: "parent[@range]>name[@range]: value"
    private static List<String> rules(OverrideMap overrides) {
        List<String> out = new java.util.ArrayList<>();
        for (Dependency dep : overrides.map.values()) {
            out.add(dep.name + ": " + dep.version.literal);
        }
        for (OverrideMap.ScopedOverride rule : overrides.scoped) {
            String parent = rule.parent == null ? "" : rule.parent.name +
                    (rule.parent.version.literal.isEmpty() ? "" : "@" + rule.parent.version.literal) + ">";
            String range = rule.targetRange.literal.isEmpty() ? "" : "@" + rule.targetRange.literal;
            out.add(parent + rule.dep.name + range + ": " + rule.dep.version.literal);
        }
        return out;
    }

    /**
     * OverrideMap.rs parse_from_overrides with override_selector.rs (1.4.0, bun#38333): an object's other keys are
     * rules scoped to its key; a key is a pnpm or Yarn selector or {@code name@range}, {@code //} a comment; what is
     * too deep, unnamed, unranged or no npm range is warned of and skipped. Before, every key is a plain rule as
     * written, and an object counts for its {@code "."} alone.
     */
    @Test
    void readsOverridesScopedToAParentOrARangeFrom140() {
        String manifest = "{\"name\":\"p\",\"dependencies\":{\"debug\":\"2.6.9\"},\"overrides\":{" +
                          "\"// note\":\"x\",\"foo\":\"1.0.0\",\"bar@^2\":\"2.1.0\",\"a>b\":\"5.0.0\"," +
                          "\"debug\":{\".\":\"2.6.8\",\"ms\":\"2.1.3\",\"x>y\":\"1\",\"z\":{\"w\":\"1\"},\"v\":1}," +
                          "\"parent@^1\":{\"child@<2\":\"1.5.0\"},\"x>y>z\":\"1.0.0\",\"@s\":\"1.0.0\"," +
                          "\"c@\":\"1.0.0\",\"d\":\"-\",\"e\":\"\",\"i@latest\":{\"j\":\"1.0.0\"},\"k\":1," +
                          "\"l@npm:^3\":\"3.1.0\",\"m\":\"patch:m@1.0.0#p.patch\"}}";
        List<String> warnings = new java.util.ArrayList<>();
        OverrideMap later = overrides("1.4.0", manifest, warnings);
        assertThat(rules(later)).containsExactly("foo: 1.0.0", "debug: 2.6.8", "bar@^2: 2.1.0", "a>b: 5.0.0",
          "debug>ms: 2.1.3", "parent@^1>child@<2: 1.5.0", "l@^3: 3.1.0");
        assertThat(later.hasScoped()).isTrue();
        assertThat(later.hasRuleForName(Wyhash11.stringHash("ms"))).isTrue();
        assertThat(later.containsName(Wyhash11.stringHash("ms"), "ms")).isFalse();
        assertThat(warnings).containsExactly(
          "Bun currently only supports one level of nested \"overrides\"",
          "Bun currently only supports one level of nested \"overrides\"",
          "Invalid override value for \"v\"",
          "Bun currently only supports one level of nested \"overrides\"",
          "Invalid package name \"@s\"",
          "Missing version range after \"c@\"",
          "Removing \"d\" with \"-\" is not supported",
          "Missing override value",
          "Invalid version range \"latest\" for \"i\"",
          "Invalid override value for \"k\"",
          "Bun currently does not support patched package \"overrides\"");

        OverrideMap earlier = overrides("1.3.14", manifest, new java.util.ArrayList<>());
        assertThat(earlier.hasScoped()).isFalse();
        assertThat(rules(earlier)).containsExactly("// note: x", "foo: 1.0.0", "bar@^2: 2.1.0", "a>b: 5.0.0",
          "debug: 2.6.8", "x>y>z: 1.0.0", "@s: 1.0.0", "c@: 1.0.0", "d: -", "l@npm:^3: 3.1.0");
    }

    /**
     * OverrideMap.rs parse_from_resolutions (1.4.0, bun#38333): a key is a Yarn path of at most one parent, a
     * {@code **} dropped; before, a leading {@code **}{@code /} is dropped and a key with another slash skipped.
     */
    @Test
    void readsResolutionsScopedToAParentFrom140() {
        String manifest = "{\"name\":\"p\",\"resolutions\":{\"**/ms\":\"2.1.3\",\"debug/ms\":\"2.0.0\"," +
                          "\"@s/p\":\"1.0.0\",\"a/b/c\":\"1.0.0\",\"**\":\"1.0.0\",\"@t/q/@u/r\":\"1.0.0\"," +
                          "\"**/x/**/y\":\"1.0.0\",\"q>r\":\"1.0.0\",\"n\":{\"o\":\"1\"}}}";
        List<String> warnings = new java.util.ArrayList<>();
        assertThat(rules(overrides("1.4.0", manifest, warnings))).containsExactly("ms: 2.1.3", "@s/p: 1.0.0",
          "debug>ms: 2.0.0", "@t/q>@u/r: 1.0.0", "x>y: 1.0.0", "q>r: 1.0.0");
        assertThat(warnings).containsExactly("Bun currently only supports one level of nested \"resolutions\"",
          "Missing resolution package name", "Invalid resolution value for \"n\"");
        assertThat(rules(overrides("1.3.14", manifest, new java.util.ArrayList<>())))
          .containsExactly("ms: 2.1.3", "@s/p: 1.0.0", "**: 1.0.0", "q>r: 1.0.0");
    }

    /**
     * OverrideMap.rs parse_from_overrides (1.4.0, bun#38333): an {@code overrides} that is no object is warned of;
     * before, it fails the install (OverrideMap.zig {@code error.Invalid}).
     */
    @Test
    void anOverridesThatIsNoObjectIsWarnedOfFrom140() {
        List<String> warnings = new java.util.ArrayList<>();
        assertThat(overrides("1.4.0", "{\"name\":\"p\",\"overrides\":\"x\"}", warnings).isEmpty()).isTrue();
        assertThat(warnings).containsExactly("\"overrides\" must be an object");
        assertThatThrownBy(() -> overrides("1.3.14", "{\"name\":\"p\",\"overrides\":\"x\"}",
          new java.util.ArrayList<>())).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid");
    }

    /**
     * OverrideMap.rs parse_override_value (1.4.0, bun#38333): {@code "bar": "$foo"} is bar's rule of the spec the
     * root declares for foo, else the one every workspace declares; before, the root's foo itself, which the lock
     * writes as {@code "foo"}.
     */
    @Test
    void takesAReferencedSpecForTheKeysOwnRuleFrom140() {
        String manifest = "{\"name\":\"p\",\"workspaces\":[\"packages/a\",\"packages/b\"],\"dependencies\":" +
                          "{\"foo\":\"^1.2.3\"},\"overrides\":{\"bar\":\"$foo\",\"y\":\"$z\",\"u\":\"$v\"," +
                          "\"w\":\"$nowhere\"}}";
        java.util.Map<String, String> members = new java.util.LinkedHashMap<>();
        members.put("packages/a", "{\"name\":\"a\",\"dependencies\":{\"z\":\"^2.0.0\",\"v\":\"1\"}}");
        members.put("packages/b", "{\"name\":\"b\",\"devDependencies\":{\"z\":\"^2.0.0\",\"v\":\"2\"}}");
        List<String> warnings = new java.util.ArrayList<>();
        assertThat(rules(overrides("1.4.0", manifest, members, warnings))).containsExactly("bar: ^1.2.3",
          "y: ^2.0.0");
        assertThat(warnings).containsExactly(
          "Could not resolve \"$v\": workspaces declare different versions of \"v\"",
          "Could not resolve \"$nowhere\": \"nowhere\" is not in dependencies");
        assertThat(rules(overrides("1.3.14", manifest, members, new java.util.ArrayList<>())))
          .containsExactly("foo: ^1.2.3");
    }

    /**
     * OverrideMap.rs parse_override_value (1.4.1, bun#41302): {@code $a} skips the root's workspaces entry for a and
     * reads the range the root declares as written; 1.4.0 takes that entry, the member's path, as a workspace spec.
     */
    @Test
    void readsAReferencedSpecAsWrittenFrom141() {
        String manifest = "{\"name\":\"p\",\"workspaces\":[\"packages/a\"],\"dependencies\":{\"a\":\"^1.0.0\"}," +
                          "\"overrides\":{\"x\":\"$a\"}}";
        java.util.Map<String, String> members = Collections.singletonMap("packages/a",
          "{\"name\":\"a\",\"version\":\"1.0.0\"}");
        Dependency later = overrides("1.4.1", manifest, members, new java.util.ArrayList<>()).map
          .get(Wyhash11.stringHash("x"));
        assertThat(later.version.tag).isEqualTo(DependencyVersion.Tag.NPM);
        assertThat(later.version.literal).isEqualTo("^1.0.0");
        Dependency earlier = overrides("1.4.0", manifest, members, new java.util.ArrayList<>()).map
          .get(Wyhash11.stringHash("x"));
        assertThat(earlier.version.tag).isEqualTo(DependencyVersion.Tag.WORKSPACE);
        assertThat(earlier.version.literal).isEqualTo("packages/a");
    }

    private static final String SCOPED = "  \"overrides\": {\n" +
                                         "    \"parent@^1\": { \"child@<2\": \"1.5.0\" },\n" +
                                         "    \"foo\": \"1.0.0\",\n" +
                                         "    \"baz\": { \"qux\": \"4.0.0\", \".\": \"3.0.0\" },\n" +
                                         "    \"bar@^2\": { \".\": \"2.1.0\" },\n" +
                                         "    \"a\": { \"b\": \"5.0.0\" },\n" +
                                         "  },\n";

    /**
     * bun.lock.rs (1.4.0, bun#38333): an overrides row that is an object holds its key's rule ({@code "."}) and the
     * rules scoped to it, read at every lockfile version; the lock writes the plain rules, the ranged rules without a
     * parent and the parents' groups merged in key order, at version 3. Before, a row is a string.
     */
    @Test
    void readsAndWritesScopedOverridesFrom140() {
        String lock = lock("  \"configVersion\": 1,\n" + SCOPED, MS);
        String written = writtenBack("1.4.0", lock);
        assertThat(written).startsWith("{\n  \"lockfileVersion\": 3,\n").contains("  },\n" +
          "  \"overrides\": {\n" +
          "    \"a\": {\n" +
          "      \"b\": \"5.0.0\",\n" +
          "    },\n" +
          "    \"bar@^2\": {\n" +
          "      \".\": \"2.1.0\",\n" +
          "    },\n" +
          "    \"baz\": {\n" +
          "      \".\": \"3.0.0\",\n" +
          "      \"qux\": \"4.0.0\",\n" +
          "    },\n" +
          "    \"foo\": \"1.0.0\",\n" +
          "    \"parent@^1\": {\n" +
          "      \"child@<2\": \"1.5.0\",\n" +
          "    },\n" +
          "  },\n" +
          "  \"packages\": {\n");
        assertThat(writtenBack("1.4.0", written)).isEqualTo(written);
        assertThatThrownBy(() -> TextLockfileReader.load(lock, options("1.3.14", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Expected a string");
        assertThatThrownBy(() -> TextLockfileReader.load(lock("  \"overrides\": { \"a\": 1 },\n", MS),
          options("1.4.0", null))).hasMessage("Expected a string or an object");
        assertThatThrownBy(() -> TextLockfileReader.load(lock("  \"overrides\": { \"@s\": { \"a\": \"1\" } },\n",
          MS), options("1.4.0", null))).hasMessage("Invalid override key");
        assertThatThrownBy(() -> TextLockfileReader.load(lock("  \"overrides\": { \"a@latest\": { \"b\": \"1\" } }," +
          "\n", MS), options("1.4.0", null))).hasMessage("Invalid override version");
    }

    // the id of the dependency of the package named owner on name
    private static int edge(Lockfile lockfile, String owner, String name) {
        for (Package pkg : lockfile.packages) {
            if (pkg.name.equals(owner)) {
                for (int id = pkg.dependenciesOff; id < pkg.dependenciesEnd(); id++) {
                    if (lockfile.dependencies.get(id).name.equals(name)) {
                        return id;
                    }
                }
            }
        }
        throw new IllegalArgumentException(owner + " has no dependency on " + name);
    }

    /**
     * OverrideMap.rs get and scoped_rule_for (1.4.0, bun#38333): the scoped rule of the edge's name that matches best
     * wins over the plain one, a ranged parent the edge's owner satisfies over an unranged one over none, a target
     * range the declared range intersects adding to that.
     */
    @Test
    void anEdgeTakesTheScopedRuleItMatchesBestFrom140() {
        String integrity = "\"sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tX" +
                           "tF3WRTlA==\"";
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n" +
                      "      \"dependencies\": { \"a\": \"1.0.0\", \"b\": \"2.0.0\", \"c\": \"^1.0.0\", \"d\": \"1.0.0\" },\n" +
                      "    },\n  },\n  \"overrides\": {\n    \"c\": \"1.5.0\",\n" +
                      "    \"c@^1\": { \".\": \"1.6.0\" },\n    \"a\": { \"c\": \"1.7.0\" },\n" +
                      "    \"a@1\": { \"c@<2\": \"1.8.0\" },\n    \"b\": { \"c\": \"1.9.0\" },\n" +
                      "    \"b@^3\": { \"c\": \"1.10.0\" },\n  },\n  \"packages\": {\n" +
                      "    \"a\": [\"a@1.0.0\", \"\", { \"dependencies\": { \"c\": \"^1.0.0\" } }, " + integrity + "],\n" +
                      "    \"b\": [\"b@2.0.0\", \"\", { \"dependencies\": { \"c\": \"^2.0.0\" } }, " + integrity + "],\n" +
                      "    \"c\": [\"c@1.5.0\", \"\", {}, " + integrity + "],\n" +
                      "    \"d\": [\"d@1.0.0\", \"\", { \"dependencies\": { \"c\": \"latest\" } }, " + integrity + "],\n" +
                      "  }\n}\n";
        Lockfile lockfile = TextLockfileReader.load(lock, options("1.4.0", null));
        OverrideMap overrides = lockfile.overrides;
        long c = Wyhash11.stringHash("c");
        assertThat(requireNonNull(overrides.get(lockfile, edge(lockfile, "p", "c"), c)).literal).isEqualTo("1.6.0");
        assertThat(requireNonNull(overrides.get(lockfile, edge(lockfile, "a", "c"), c)).literal).isEqualTo("1.8.0");
        assertThat(requireNonNull(overrides.get(lockfile, edge(lockfile, "b", "c"), c)).literal).isEqualTo("1.9.0");
        // a declared range that is no npm range matches no target range
        assertThat(requireNonNull(overrides.get(lockfile, edge(lockfile, "d", "c"), c)).literal).isEqualTo("1.5.0");
        assertThat(overrides.hasRuleForName(c)).isTrue();
        assertThat(overrides.hasRuleForName(Wyhash11.stringHash("a"))).isFalse();
    }

    /**
     * Package.rs Diff::generate with OverrideMap::changed (1.4.0, bun#38333): the scoped rules are compared too, a
     * selector spelled otherwise reading as the same rule.
     */
    @Test
    void comparesTheScopedRulesFrom140() {
        List<String> none = new java.util.ArrayList<>();
        OverrideMap nested = overrides("1.4.0", "{\"name\":\"p\",\"overrides\":{\"debug\":{\"ms\":\"2.1.3\"}}}", none);
        OverrideMap pnpm = overrides("1.4.0", "{\"name\":\"p\",\"overrides\":{\"debug>ms\":\"2.1.3\"}}", none);
        OverrideMap other = overrides("1.4.0", "{\"name\":\"p\",\"overrides\":{\"debug>ms\":\"2.1.2\"}}", none);
        OverrideMap plain = overrides("1.4.0", "{\"name\":\"p\",\"overrides\":{\"ms\":\"2.1.3\"}}", none);
        assertThat(OverrideMap.changed(nested, pnpm, BunRelease.of("1.4.0"))).isFalse();
        assertThat(OverrideMap.changed(nested, other, BunRelease.of("1.4.0"))).isTrue();
        assertThat(OverrideMap.changed(nested, plain, BunRelease.of("1.4.0"))).isTrue();
    }

    private static String packumentOf(String name, String... versions) {
        StringBuilder out = new StringBuilder("{\"name\":\"" + name + "\",\"modified\":\"2026-01-01T00:00:00.000Z\"," +
                                              "\"dist-tags\":{\"latest\":\"" + versions[versions.length - 2] +
                                              "\"},\"versions\":{");
        for (int i = 0; i < versions.length; i += 2) {
            out.append(i == 0 ? "" : ",").append('"').append(versions[i]).append("\":{\"name\":\"").append(name)
              .append("\",\"version\":\"").append(versions[i]).append("\",\"dependencies\":").append(versions[i + 1])
              .append(",\"dist\":{\"tarball\":\"https://registry.npmjs.org/").append(name).append("/-/").append(name)
              .append('-').append(versions[i]).append(".tgz\",\"integrity\":\"").append(INTEGRITY).append("\"}}");
        }
        return out.append("}}").toString();
    }

    private static String catalogInstall(String release, String lock, String catalogVersion) {
        String manifest = "{\"name\":\"root\",\"dependencies\":{\"a\":\"1.0.0\"},\"workspaces\":{\"packages\":[]," +
                          "\"catalog\":{\"b\":\"" + catalogVersion + "\"}},\"overrides\":{\"b\":\"catalog:\"}}";
        ManifestSource registry = (url, name, extended) -> "a".equals(name) ?
          packumentOf("a", "1.0.0", "{\"b\":\"^1.0.0\"}") : "b".equals(name) ?
          packumentOf("b", "1.0.0", "{}", "2.0.0", "{}") : null;
        org.openrewrite.javascript.internal.LockFileRegeneration.Result result = new BunLockRegenerator(
          BunLockRegenerator.Inputs.builder().lock(lock).manifest("", manifest)
            .now(java.time.Instant.parse("2026-10-11T00:00:00Z")).release(BunRelease.of(release)).build(),
          registry).regenerate();
        return requireNonNull(result.getLockFileContent(), String.valueOf(result.getErrorMessage()));
    }

    /**
     * install_with_manager.rs (1.4.0, bun#38333): when the catalog changes, the edge on b, whose override is
     * {@code catalog:}, is resolved again; before, only the {@code catalog:} edges are, and b stays where it was.
     */
    @Test
    void resolvesAgainTheEdgesOfACatalogValuedOverrideFrom140() {
        String later = catalogInstall("1.4.0", catalogInstall("1.4.0", EMPTY_ROOT, "1.0.0"), "2.0.0");
        assertThat(later).contains("\"b\": [\"b@2.0.0\"").doesNotContain("\"b@1.0.0\"");
        String earlier = catalogInstall("1.3.14", catalogInstall("1.3.14", EMPTY_ROOT, "1.0.0"), "2.0.0");
        assertThat(earlier).contains("\"b\": [\"b@1.0.0\"").contains("\"b\": \"2.0.0\"");
    }

    private static PackageManifest.PackageVersion version(BunRelease release, String packument) {
        PackageManifest manifest = requireNonNull(PackageManifest.parse(release, new Log(), packument, "debug", false));
        PackageManifest.FindResult pick = requireNonNull(manifest.findBestVersion(
          requireNonNull(DependencyVersion.parse("debug", Wyhash11.stringHash("debug"), "4.4.3")).npm().version));
        return pick.packageVersion;
    }

    private static List<String> peers(String release, String manifest) {
        Lockfile lockfile = new Lockfile();
        Package pkg = new Package();
        pkg.parseWithJSON(lockfile, options(release, null), new Log(), "packages/a", Json.parse(manifest),
          Features.WORKSPACE, Collections.emptyMap());
        List<String> out = new java.util.ArrayList<>();
        for (Dependency dep : lockfile.dependenciesOf(pkg)) {
            if (dep.behavior.isPeer()) {
                out.add(dep.name + " " + dep.version.literal + (dep.behavior.optional() ? " optional" : ""));
            }
        }
        return out;
    }
}
