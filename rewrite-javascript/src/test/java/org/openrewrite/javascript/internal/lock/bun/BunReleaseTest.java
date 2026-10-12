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
import java.util.Map;

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

    // 1.4.x: the reader's binding, the hoist, the diff and the clean

    private static String npm(String key, String nameAtVersion, String info) {
        return "    \"" + key + "\": [\"" + nameAtVersion + "\", \"\", " + info + ", \"" + INTEGRITY + "\"],\n";
    }

    private static String lockWith(String workspaces, String packages) {
        return "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n" + workspaces + "  },\n  \"packages\": {\n" +
               packages + "  }\n}\n";
    }

    private static String rootWith(String dependencies) {
        return "    \"\": {\n      \"name\": \"r\",\n      \"dependencies\": " + dependencies + ",\n    },\n";
    }

    private static Dependency dependencyOf(Lockfile lockfile, String pkgName, String depName) {
        return lockfile.dependencies.get(dependencyIdOf(lockfile, pkgName, depName));
    }

    // the first package of the name and its first dependency of the name
    private static int dependencyIdOf(Lockfile lockfile, String pkgName, String depName) {
        for (Package pkg : lockfile.packages) {
            if (pkg.name.equals(pkgName)) {
                for (int id = pkg.dependenciesOff; id < pkg.dependenciesEnd(); id++) {
                    if (lockfile.dependencies.get(id).name.equals(depName)) {
                        return id;
                    }
                }
            }
        }
        throw new AssertionError(pkgName + " has no dependency " + depName);
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

    /**
     * OverrideMap.rs with override_selector.rs (1.4.0, bun#38333), read over what the recipes write for an override
     * along a path: one parent, a nested object in overrides or a Yarn path in resolutions, is a rule; a deeper path,
     * or a parent whose range is no npm range, is declined as bun warns of it. Before, any path is declined.
     */
    @Test
    void theRecipesWriteAnOverrideOneParentDeepFrom140() {
        org.openrewrite.javascript.internal.lock.ManifestRules later = BunManifestRules.of(BunRelease.of("1.4.0"));
        org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace overrides =
          org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace.OVERRIDES;
        org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace resolutions =
          org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace.RESOLUTIONS;
        List<org.openrewrite.javascript.internal.lock.DependencyPathSegment> isOdd = java.util.Arrays.asList(
          new org.openrewrite.javascript.internal.lock.DependencyPathSegment("is-odd", "3.0.1"));
        List<org.openrewrite.javascript.internal.lock.DependencyPathSegment> twoParents = java.util.Arrays.asList(
          new org.openrewrite.javascript.internal.lock.DependencyPathSegment("is-even", null),
          new org.openrewrite.javascript.internal.lock.DependencyPathSegment("is-odd", null));
        List<org.openrewrite.javascript.internal.lock.DependencyPathSegment> scoped = java.util.Arrays.asList(
          new org.openrewrite.javascript.internal.lock.DependencyPathSegment("@s/a", "npm:^1"));
        List<org.openrewrite.javascript.internal.lock.DependencyPathSegment> tagged = java.util.Arrays.asList(
          new org.openrewrite.javascript.internal.lock.DependencyPathSegment("is-odd", "latest"));

        assertThat(later.overrideDeclined("is-number", "7.0.0", isOdd, overrides)).isNull();
        assertThat(later.overrideDeclined("is-number", "7.0.0", isOdd, resolutions)).isNull();
        assertThat(later.overrideKey("is-number", isOdd, Collections.emptyList())).isEqualTo("is-odd@3.0.1/is-number");
        assertThat(later.overrideDeclined("@t/b", "7.0.0", scoped, resolutions)).isNull();
        assertThat(later.overrideKey("@t/b", scoped, Collections.emptyList())).isEqualTo("@s/a@npm:^1/@t/b");
        assertThat(later.overrideDeclined("is-number", "7.0.0", twoParents, overrides))
          .isEqualTo("Bun currently only supports one level of nested \"overrides\"");
        assertThat(later.overrideDeclined("is-number", "7.0.0", twoParents, resolutions))
          .isEqualTo("Bun currently only supports one level of nested \"resolutions\"");
        assertThat(later.overrideDeclined("is-number", "7.0.0", tagged, overrides))
          .isEqualTo("Invalid version range \"latest\" for \"is-odd\"");

        assertThat(BunManifestRules.RULES.overrideDeclined("is-number", "7.0.0", isOdd, overrides))
          .isEqualTo("bun does not support nested overrides");
        assertThat(BunManifestRules.RULES.overrideDeclined("is-number", "7.0.0", Collections.emptyList(), overrides))
          .isNull();
        // a release the engine does not reproduce reads a path it cannot say how
        assertThat(BunManifestRules.RULES.installedAs("9.9.9")
          .overrideDeclined("is-number", "7.0.0", isOdd, overrides)).contains("cannot know what bun 9.9.9 writes");
    }

    // a version of a packument, with what `extra` adds to it (",\"dependencies\":{...}")
    private static String registryVersion(String name, String version, String extra) {
        return "\"" + version + "\":{\"name\":\"" + name + "\",\"version\":\"" + version + "\"" + extra +
               ",\"dist\":{\"tarball\":\"https://registry.npmjs.org/" + name + "/-/" + name.replaceAll(".*/", "") +
               "-" + version + ".tgz\",\"integrity\":\"" + INTEGRITY + "\"}}";
    }

    private static String registryPackument(String name, String latest, String... versions) {
        return "{\"name\":\"" + name + "\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":\"" +
               latest + "\"},\"versions\":{" + String.join(",", versions) + "}}";
    }

    private static org.openrewrite.javascript.internal.LockFileRegeneration.Result install(
      String release, String lock, java.util.Map<String, String> manifests, java.util.Map<String, String> packuments) {
        BunLockRegenerator.Inputs.InputsBuilder inputs = BunLockRegenerator.Inputs.builder()
          .lock(lock)
          .now(java.time.Instant.parse("2026-10-11T00:00:00Z"))
          .release(BunRelease.of(release));
        for (java.util.Map.Entry<String, String> manifest : manifests.entrySet()) {
            inputs.manifest(manifest.getKey(), manifest.getValue());
        }
        return new BunLockRegenerator(inputs.build(), (url, name, extended) -> packuments.get(name)).regenerate();
    }

    private static String written(org.openrewrite.javascript.internal.LockFileRegeneration.Result result) {
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        return requireNonNull(result.getLockFileContent());
    }

    private static String failure(org.openrewrite.javascript.internal.LockFileRegeneration.Result result) {
        assertThat(result.isSuccess()).as(String.valueOf(result.getLockFileContent())).isFalse();
        return requireNonNull(result.getFailure()).getKind() + " " + result.getFailure().getDetail();
    }

    private static java.util.Map<String, String> rootManifest(String manifest) {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", manifest);
        return manifests;
    }

    /**
     * PackageManagerEnqueue.rs enqueue_dependency_list (1.4.0): every dependency of the list is enqueued; before, a
     * list whose first dependency is a peer that a later dependency also names leaves that peer out. (With the groups
     * sorted peers last, from 1.2.19, a parsed root never starts so; this list is built by hand.)
     */
    @Test
    void enqueuesEveryDependencyOfTheListFrom140() {
        for (String release : new String[]{"1.3.14", "1.4.0"}) {
            BunOptions options = options(release, null);
            Lockfile lockfile = new Lockfile(options.getRelease());
            Package root = new Package();
            root.name = "root";
            root.nameHash = Wyhash11.stringHash("root");
            root.resolution = Resolution.ROOT;
            long x = Wyhash11.stringHash("x");
            lockfile.dependencies.add(new Dependency(x, "x", requireNonNull(DependencyVersion.parse("x", x, "1.0.0")),
              Behavior.of(Behavior.PEER)));
            lockfile.dependencies.add(new Dependency(x, "x", requireNonNull(DependencyVersion.parse("x", x, "1.0.0")),
              Behavior.of(Behavior.PROD)));
            lockfile.resolutions.add(Package.INVALID_PACKAGE_ID);
            lockfile.resolutions.add(Package.INVALID_PACKAGE_ID);
            root.dependenciesLen = 2;
            lockfile.appendPackage(root);
            PackageManager manager = new PackageManager(lockfile, options, new Log(), Collections.emptyMap(),
              (url, name, extended) -> {
                  throw new AssertionError("nothing is fetched: " + name);
              }, 0);
            manager.enqueueDependencyList(0, 2);
            if ("1.4.0".equals(release)) {
                assertThat(manager.peerDependencies).containsExactly(0);
            } else {
                assertThat(manager.peerDependencies).isEmpty();
            }
        }
    }

    /**
     * PackageManagerEnqueue.rs (1.4.0, bun#33835): the npm: alias {@code @ts/old} of the package the root's alias
     * {@code typescript} installs is not redirected to that alias; before, its target typescript was, so that it
     * resolves to the aliasing package itself.
     */
    @Test
    void redirectsOnlyPlainDependenciesToAnAliasFrom140() {
        java.util.Map<String, String> packuments = new java.util.LinkedHashMap<>();
        packuments.put("@ts/ts6", registryPackument("@ts/ts6", "6.0.2",
          registryVersion("@ts/ts6", "6.0.2", ",\"dependencies\":{\"@ts/old\":\"npm:typescript@^6\"}")));
        packuments.put("typescript", registryPackument("typescript", "6.0.3",
          registryVersion("typescript", "6.0.3", "")));
        java.util.Map<String, String> manifests =
          rootManifest("{\"name\":\"root\",\"dependencies\":{\"typescript\":\"npm:@ts/ts6@6.0.2\"}}");
        assertThat(written(install("1.4.0", EMPTY_ROOT, manifests, packuments)))
          .contains("\"@ts/old\": [\"typescript@6.0.3\"");
        assertThat(written(install("1.3.14", EMPTY_ROOT, manifests, packuments)))
          .contains("\"@ts/old\": [\"@ts/ts6@6.0.2\"");
    }

    // x@1.0.0 and z@1.0.0 at the root; z has the peer x ^1.0.0 and w, whose x ~1.5.0 adds x@1.5.0 after z's peer
    private static Lockfile resolvedPeerPick(String release) {
        BunOptions options = options(release, null);
        String manifest = "{\"name\":\"root\",\"dependencies\":{\"x\":\"1.0.0\",\"z\":\"1.0.0\"}}";
        java.util.Map<String, String> packuments = new java.util.LinkedHashMap<>();
        packuments.put("x", registryPackument("x", "1.5.0", registryVersion("x", "1.0.0", ""),
          registryVersion("x", "1.5.0", "")));
        packuments.put("z", registryPackument("z", "1.0.0", registryVersion("z", "1.0.0",
          ",\"dependencies\":{\"w\":\"1.0.0\"},\"peerDependencies\":{\"x\":\"^1.0.0\"}")));
        packuments.put("w", registryPackument("w", "1.0.0", registryVersion("w", "1.0.0",
          ",\"dependencies\":{\"x\":\"~1.5.0\"}")));
        Lockfile lockfile = new Lockfile(options.getRelease());
        java.util.Map<String, String> manifests = rootManifest(manifest);
        PackageManager manager = new PackageManager(lockfile, options, new Log(), manifests,
          (url, name, extended) -> packuments.get(name), 0);
        Package root = new Package();
        root.parseWithJSON(lockfile, options, new Log(), "", Json.parse(manifest), Features.MAIN, manifests);
        root = lockfile.appendPackage(root);
        manager.enqueueDependencyList(root.dependenciesOff, root.dependenciesLen);
        manager.resolve(lockfile.packages.size());
        return lockfile;
    }

    private static String peerResolution(Lockfile lockfile) {
        for (int i = 0; i < lockfile.dependencies.size(); i++) {
            if (lockfile.dependencies.get(i).behavior.isPeer()) {
                return String.valueOf(lockfile.packages.get(lockfile.resolutions.get(i)).resolution.version);
            }
        }
        throw new AssertionError("no peer");
    }

    /**
     * PackageManagerEnqueue.rs get_or_put_resolved_package_with_find_result (1.4.0, bun#30412): z's peer x ^1.0.0,
     * reached once x's packument holds the root's x@1.0.0 but not its pick 1.5.0, waits for the peers, and takes the
     * highest x it accepts then, 1.5.0, which w added meanwhile; before, it takes 1.0.0 at once.
     */
    @Test
    void bindsAPeerBeforeThePeersOnlyToItsPickFrom140() {
        assertThat(peerResolution(resolvedPeerPick("1.4.0"))).isEqualTo("1.5.0");
        assertThat(peerResolution(resolvedPeerPick("1.3.14"))).isEqualTo("1.0.0");
    }

    /**
     * lockfile.rs get_package_id (1.4.0, bun#30412): a range does not take a package this install added below its
     * pick and of another major, unless an exact range added it or the lock holds it.
     */
    @Test
    void keepsARangeOffALowerMajorAddedThisInstallFrom140() {
        for (String release : new String[]{"1.3.14", "1.4.0"}) {
            Lockfile lockfile = new Lockfile(BunRelease.of(release));
            Package x = new Package();
            x.name = "x";
            x.nameHash = Wyhash11.stringHash("x");
            x.resolution = Resolution.npm(SemverVersion.parse("1.9.0").version.min(), "");
            int id = lockfile.appendPackage(x).meta.id;
            DependencyVersion atLeast1 = requireNonNull(DependencyVersion.parse("x", x.nameHash, ">=1"));
            Resolution pick = Resolution.npm(SemverVersion.parse("2.0.0").version.min(), "");
            assertThat(lockfile.getPackageID(x.nameHash, atLeast1, pick)).isEqualTo("1.4.0".equals(release) ? null : id);
            assertThat(lockfile.getPackageID(x.nameHash, atLeast1,
              Resolution.npm(SemverVersion.parse("1.9.5").version.min(), ""))).isEqualTo(id);
            lockfile.markExactPin(id);
            assertThat(lockfile.getPackageID(x.nameHash, atLeast1, pick)).isEqualTo(id);
            lockfile.exactPinned.clear();
            lockfile.markLoadedPackages();
            assertThat(lockfile.getPackageID(x.nameHash, atLeast1, pick)).isEqualTo(id);
        }
    }

    /**
     * The install of x ^1 (1.9.0) and x >=1 (2.0.0) in one wave: from 1.4.0 the order their packuments arrive in no
     * longer decides whether >=1 takes 1.9.0, so both are installed; before, the engine cannot know.
     */
    @Test
    void installsARangeBesideALowerMajorWhateverTheArrivalOrderFrom140() {
        java.util.Map<String, String> packuments = new java.util.LinkedHashMap<>();
        packuments.put("x", registryPackument("x", "2.0.0", registryVersion("x", "1.9.0", ""),
          registryVersion("x", "2.0.0", "")));
        packuments.put("a", registryPackument("a", "1.0.0", registryVersion("a", "1.0.0",
          ",\"dependencies\":{\"x\":\"^1\"}")));
        packuments.put("b", registryPackument("b", "1.0.0", registryVersion("b", "1.0.0",
          ",\"dependencies\":{\"x\":\">=1\"}")));
        java.util.Map<String, String> manifests =
          rootManifest("{\"name\":\"root\",\"dependencies\":{\"a\":\"1.0.0\",\"b\":\"1.0.0\"}}");
        assertThat(written(install("1.4.0", EMPTY_ROOT, manifests, packuments)))
          .contains("\"x\": [\"x@1.9.0\"").contains("\"b/x\": [\"x@2.0.0\"");
        assertThat(failure(install("1.3.14", EMPTY_ROOT, manifests, packuments))).isEqualTo(
          "CANNOT_KNOW the engine cannot know manifest arrival order: x for a@1.0.0 (^1) and b@1.0.0 (>=1) resolved " +
          "in one install");
        // an exact range's pick is taken whatever its major, so that the order decides again
        packuments.put("a", registryPackument("a", "1.0.0", registryVersion("a", "1.0.0",
          ",\"dependencies\":{\"x\":\"1.9.0\"}")));
        assertThat(failure(install("1.4.0", EMPTY_ROOT, manifests, packuments))).isEqualTo(
          "CANNOT_KNOW the engine cannot know manifest arrival order: x for a@1.0.0 (1.9.0) and b@1.0.0 (>=1) " +
          "resolved in one install");
    }

    private static final String MS_IN_A_WORKSPACE = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n" +
                                                    "  \"workspaces\": {\n    \"\": {\n      \"name\": \"root\",\n" +
                                                    "      \"dependencies\": {\n        \"ms\": \"2.0.0\",\n      },\n" +
                                                    "    },\n    \"packages/a\": {\n      \"name\": \"a\",\n" +
                                                    "      \"dependencies\": {\n        \"ms\": \"^2.0.0\",\n" +
                                                    "      },\n    },\n  },\n  \"packages\": {\n" +
                                                    "    \"a\": [\"a@workspace:packages/a\"],\n\n" +
                                                    "    \"ms\": [\"ms@2.0.0\", \"\", {}, \"" + INTEGRITY + "\"],\n" +
                                                    "  }\n}\n";

    /**
     * update_transitive.rs DirectDependencies::redirect_dependents (1.4.0, bun#38333): the root's ms 2.0.0 becomes
     * ^2.1.0 (2.1.3) while the workspace a keeps ms ^2.0.0 on the locked 2.0.0: a's edge follows the root's to 2.1.3,
     * and 2.0.0 leaves the lock; before, a keeps 2.0.0.
     */
    @Test
    void redirectsDependentsOfAMovedDirectDependencyFrom140() {
        java.util.Map<String, String> packuments = java.util.Collections.singletonMap("ms", registryPackument("ms",
          "2.1.3", registryVersion("ms", "2.0.0", ""), registryVersion("ms", "2.1.3", "")));
        java.util.Map<String, String> manifests = rootManifest("{\"name\":\"root\",\"workspaces\":[\"packages/a\"]," +
                                                               "\"dependencies\":{\"ms\":\"^2.1.0\"}}");
        manifests.put("packages/a", "{\"name\":\"a\",\"dependencies\":{\"ms\":\"^2.0.0\"}}");
        String later = written(install("1.4.0", MS_IN_A_WORKSPACE, manifests, packuments));
        assertThat(later).contains("\"ms\": [\"ms@2.1.3\"").doesNotContain("ms@2.0.0");
        String earlier = written(install("1.3.14", MS_IN_A_WORKSPACE, manifests, packuments));
        assertThat(earlier).contains("\"ms\": [\"ms@2.1.3\"").contains("\"a/ms\": [\"ms@2.0.0\"");
    }

    /**
     * PackageManagerEnqueue.rs get_or_put_resolved_package (1.4.0, bun#31417, bun#32452): x's file:../b leaves x's
     * directory, so that b resolves to nothing, unless an override names b; before, to a folder package.
     */
    @Test
    void refusesAFolderPathLeavingItsPackageFrom140() {
        java.util.Map<String, String> packuments = java.util.Collections.singletonMap("x", registryPackument("x",
          "1.0.0", registryVersion("x", "1.0.0", ",\"dependencies\":{\"b\":\"file:../b\"}")));
        java.util.Map<String, String> manifests = rootManifest("{\"name\":\"root\",\"dependencies\":{\"x\":\"1.0.0\"}}");
        assertThat(failure(install("1.4.0", EMPTY_ROOT, manifests, packuments))).isEqualTo(
          "PM_REJECTS bun rejects this edit: Could not find package.json for \"file:../b\" dependency \"b\"; " +
          "b@file:../b failed to resolve");
        assertThat(written(install("1.3.14", EMPTY_ROOT, manifests, packuments)))
          .contains("\"x/b\": [\"b@file:../b\", {}]");
        java.util.Map<String, String> overridden = rootManifest("{\"name\":\"root\",\"dependencies\":{\"x\":\"1.0.0\"}," +
                                                                "\"overrides\":{\"b\":\"file:../b\"}}");
        assertThat(written(install("1.4.0", EMPTY_ROOT, overridden, packuments)))
          .contains("\"x/b\": [\"b@file:../b\", {}]");
        // bun on Windows takes \b for absolute, which leaves the package, and on POSIX for a name below it
        java.util.Map<String, String> backslash = java.util.Collections.singletonMap("x", registryPackument("x",
          "1.0.0", registryVersion("x", "1.0.0", ",\"dependencies\":{\"b\":\"file:\\\\b\"}")));
        assertThat(failure(install("1.4.0", EMPTY_ROOT, manifests, backslash)))
          .startsWith("CANNOT_KNOW the engine cannot know folder dependency b@file:\\b (bun on Windows");
    }

    /**
     * PackageManagerEnqueue.rs and lockfile.rs is_trusted_folder_dependency (1.4.3, bun#33106): the root's file:
     * package a may name a folder above its own; from 1.4.0 to 1.4.2 that resolves to nothing.
     */
    @Test
    void trustsTheFolderPathsOfALocalPackageFrom143() {
        java.util.Map<String, String> manifests = rootManifest("{\"name\":\"root\",\"dependencies\":{\"a\":\"file:./a\"}}");
        manifests.put("a", "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"b\":\"file:../../b\"}}");
        assertThat(written(install("1.4.3", EMPTY_ROOT, manifests, Collections.emptyMap())))
          .contains("\"a/b\": [\"b@file:../b\", {}]");
        assertThat(failure(install("1.4.2", EMPTY_ROOT, manifests, Collections.emptyMap()))).isEqualTo(
          "PM_REJECTS bun rejects this edit: Could not find package.json for \"file:../b\" dependency \"b\"; " +
          "b@file:../../b failed to resolve");
    }

    /**
     * PackageManagerEnqueue.rs get_or_put_resolved_package (1.4.0, bun#37669): x's workspace:* resolves only to the
     * root's workspace w; before, to the folder its path names, whose manifest the engine is not given here.
     */
    @Test
    void resolvesAForeignWorkspaceSpecOnlyToTheRootsWorkspaceFrom140() {
        java.util.Map<String, String> packuments = java.util.Collections.singletonMap("x", registryPackument("x",
          "1.0.0", registryVersion("x", "1.0.0", ",\"dependencies\":{\"w\":\"workspace:*\"}")));
        java.util.Map<String, String> manifests = rootManifest("{\"name\":\"root\",\"dependencies\":{\"x\":\"1.0.0\"}}");
        assertThat(failure(install("1.4.0", EMPTY_ROOT, manifests, packuments))).isEqualTo(
          "PM_REJECTS bun rejects this edit: Workspace dependency \"w\" not found; w@workspace:* failed to resolve");
        assertThat(failure(install("1.3.14", EMPTY_ROOT, manifests, packuments)))
          .startsWith("CANNOT_KNOW the engine cannot know workspace member manifest of w@workspace:*");
        java.util.Map<String, String> withW = rootManifest("{\"name\":\"root\",\"workspaces\":[\"packages/w\"]," +
                                                           "\"dependencies\":{\"x\":\"1.0.0\"}}");
        withW.put("packages/w", "{\"name\":\"w\",\"version\":\"1.0.0\"}");
        assertThat(written(install("1.4.0", EMPTY_ROOT, withW, packuments)))
          .isEqualTo(written(install("1.3.14", EMPTY_ROOT, withW, packuments)))
          .contains("\"w\": [\"w@workspace:packages/w\"]");
    }

    private static final String FOLDER_CYCLE = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n" +
                                               "  \"workspaces\": {\n    \"\": {\n      \"name\": \"root\",\n" +
                                               "      \"dependencies\": {\n        \"a\": \"file:./a\",\n" +
                                               "        \"b\": \"file:./b\",\n      },\n    },\n  },\n" +
                                               "  \"packages\": {\n" +
                                               "    \"a\": [\"a@file:a\", { \"dependencies\": { \"b\": \"file:../b\" } }],\n\n" +
                                               "    \"b\": [\"b@file:b\", { \"dependencies\": { \"a\": \"file:../a\" } }],\n" +
                                               "  }\n}\n";

    /**
     * Tree.rs process_subtree (1.4.0, bun#38333): the folder packages a and b depend on each other, and each is placed
     * once below the other; before, the hoisting nests them inside each other forever, which the port reports where
     * a folder package comes below itself through folder packages alone.
     */
    @Test
    void skipsAFolderPackageADirectoryAboveHoldsFrom140() {
        assertThat(writtenBack("1.4.0", FOLDER_CYCLE))
          .contains("\"a/b\": [\"b@file:b\"")
          .contains("\"b/a\": [\"a@file:a\"")
          .doesNotContain("\"a/b/a\"");
        assertThatThrownBy(() -> writtenBack("1.3.14", FOLDER_CYCLE))
          .isInstanceOf(org.openrewrite.javascript.internal.lock.Rejected.class)
          .hasMessageContaining("it never finishes: its hoisting places the folder package a@");
    }

    private static final String PEER_ON_A_FOLDER = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n" +
                                                   "  \"workspaces\": {\n    \"\": {\n      \"name\": \"root\",\n" +
                                                   "      \"dependencies\": {\n        \"p\": \"1.0.0\",\n" +
                                                   "        \"x\": \"file:./x\",\n      },\n    },\n  },\n" +
                                                   "  \"packages\": {\n" +
                                                   "    \"p\": [\"p@1.0.0\", \"\", { \"peerDependencies\": { \"x\": " +
                                                   "\"^1.0.0\" } }, \"" + INTEGRITY + "\"],\n\n" +
                                                   "    \"x\": [\"x@file:x\", {}],\n  }\n}\n";

    /**
     * Tree.rs process_subtree (1.4.1, bun#40564): p's peer x, which the lock binds to the root's folder x, dedupes
     * with it; before, it nests a second copy, p/x.
     */
    @Test
    void dedupesAPeerResolvedToAFolderPackageFrom141() {
        assertThat(writtenBack("1.4.1", PEER_ON_A_FOLDER)).doesNotContain("\"p/x\"");
        assertThat(writtenBack("1.4.0", PEER_ON_A_FOLDER)).contains("\"p/x\": [\"x@file:x\", {}]");
    }

    // the package a package's dependency of the name resolves to, as name@resolution; null for none
    private static @org.jspecify.annotations.Nullable String resolutionOf(Lockfile lockfile, String pkgName,
                                                                           String depName) {
        int pkgId = lockfile.resolutions.get(dependencyIdOf(lockfile, pkgName, depName));
        return pkgId < lockfile.packages.size() ? lockfile.packages.get(pkgId).toString() : null;
    }

    /**
     * bun.lock.rs {@code may_stay_unresolved} (1.4.0): a peer the lock provides no package for loads unresolved;
     * before, the lock is invalid.
     */
    @Test
    void readsAPeerTheLockLeavesUnresolvedFrom140() {
        String lock = lockWith(rootWith("{ \"a\": \"1.0.0\" }"),
          npm("a", "a@1.0.0", "{ \"peerDependencies\": { \"b\": \"^1.0.0\" } }"));
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.4.0", null)), "a", "b")).isNull();
        assertThatThrownBy(() -> TextLockfileReader.load(lock, options("1.3.14", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Failed to resolve peer dependency 'b' for package 'a'");
    }

    /**
     * bun.lock.rs {@code resolve_peer_dep_version_based} (1.4.0): y's peer react ^17 binds to the highest react that
     * satisfies it, x's nested 17.0.2; before, by y's path to the root's 18.2.0.
     */
    @Test
    void bindsALoadedPeerByVersionFrom140() {
        String lock = lockWith(rootWith("{ \"react\": \"18.2.0\", \"x\": \"1.0.0\", \"y\": \"1.0.0\" }"),
          npm("react", "react@18.2.0", "{}") +
          npm("x", "x@1.0.0", "{ \"dependencies\": { \"react\": \"17.0.2\" } }") +
          npm("x/react", "react@17.0.2", "{}") +
          npm("y", "y@1.0.0", "{ \"peerDependencies\": { \"react\": \"^17.0.0\" } }"));
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.4.0", null)), "y", "react"))
          .isEqualTo("react@17.0.2");
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.3.14", null)), "y", "react"))
          .isEqualTo("react@18.2.0");
    }

    /**
     * CatalogMap.rs {@code strip_reference} (1.4.0): outside the root and the workspaces a {@code catalog:} edge is
     * left unresolved with its literal, and a {@code catalog:} peer is an optional peer of {@code *}.
     */
    @Test
    void stripsACatalogReference() {
        Dependency dep = new Dependency("ms", requireNonNull(DependencyVersion.parse("ms", null, "catalog:")),
          Behavior.of(Behavior.PROD));
        CatalogMap.stripReference(dep);
        assertThat(dep.version.tag).isEqualTo(DependencyVersion.Tag.UNINITIALIZED);
        assertThat(dep.version.literal).isEqualTo("catalog:");
        Dependency peer = new Dependency("p", requireNonNull(DependencyVersion.parse("p", null, "catalog:x")),
          Behavior.of(Behavior.PEER));
        CatalogMap.stripReference(peer);
        assertThat(peer.behavior.isOptionalPeer()).isTrue();
        assertThat(peer.version.tag).isEqualTo(DependencyVersion.Tag.NPM);
        assertThat(peer.version.npm().version.isStar()).isTrue();
        assertThat(peer.version.literal).isEqualTo("catalog:x");
        // dependency.rs parse_with_tag (1.4.0): an uninitialized spec keeps its literal; before, it is none
        assertThat(requireNonNull(DependencyVersion.parseWithTag("ms", null, "catalog:",
          DependencyVersion.Tag.UNINITIALIZED, "catalog:", BunRelease.of("1.4.0"))).literal).isEqualTo("catalog:");
        assertThat(DependencyVersion.parseWithTag("ms", null, "catalog:", DependencyVersion.Tag.UNINITIALIZED,
          "catalog:", BunRelease.of("1.3.14"))).isNull();
    }

    /**
     * bun.lock.rs {@code parse_append_dependencies(.., catalogs_apply)} (1.4.0): a package of the lock references no
     * catalog, so that its {@code catalog:} edge stays unresolved and is written back as it was, and its
     * {@code catalog:} peer is an optional peer; before, both resolve against the root's catalog.
     */
    @Test
    void stripsTheCatalogReferencesOfALockedPackageFrom140() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n" +
                      rootWith("{ \"a\": \"1.0.0\", \"ms\": \"catalog:\" }") +
                      "  },\n  \"catalog\": {\n    \"ms\": \"2.1.3\",\n    \"p\": \"^1.0.0\",\n  },\n" +
                      "  \"packages\": {\n" +
                      npm("a", "a@1.0.0", "{ \"dependencies\": { \"ms\": \"catalog:\" }, \"peerDependencies\": " +
                                          "{ \"p\": \"catalog:\" } }") +
                      npm("ms", "ms@2.1.3", "{}") +
                      npm("p", "p@1.0.0", "{}") +
                      "  }\n}\n";
        Lockfile later = TextLockfileReader.load(lock, options("1.4.0", null));
        assertThat(dependencyOf(later, "a", "ms").version.tag).isEqualTo(DependencyVersion.Tag.UNINITIALIZED);
        assertThat(resolutionOf(later, "a", "ms")).isNull();
        assertThat(dependencyOf(later, "a", "p").behavior.isOptionalPeer()).isTrue();
        assertThat(writtenBack("1.4.0", lock))
          .contains("\"a\": [\"a@1.0.0\", \"\", { \"dependencies\": { \"ms\": \"catalog:\" }, \"peerDependencies\": " +
                    "{ \"p\": \"catalog:\" }, \"optionalPeers\": [\"p\"] }");
        Lockfile earlier = TextLockfileReader.load(lock, options("1.3.14", null));
        assertThat(dependencyOf(earlier, "a", "ms").version.tag).isEqualTo(DependencyVersion.Tag.CATALOG);
        assertThat(resolutionOf(earlier, "a", "ms")).isEqualTo("ms@2.1.3");
        assertThat(dependencyOf(earlier, "a", "p").behavior.isOptionalPeer()).isFalse();
    }

    /**
     * Package.rs {@code from_npm} and {@code parse_dependency} (1.4.0): a registry manifest's and a folder's
     * {@code catalog:} dependency is stripped; a workspace's is not.
     */
    @Test
    void stripsTheCatalogReferencesOfAManifestOutsideTheWorkspacesFrom140() {
        String packument = "{\"name\":\"a\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"versions\":{\"1.0.0\":" +
                           "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"ms\":\"catalog:\"}," +
                           "\"dist\":{\"tarball\":\"https://registry.npmjs.org/a/-/a-1.0.0.tgz\"}}}," +
                           "\"dist-tags\":{\"latest\":\"1.0.0\"}}";
        for (String release : new String[]{"1.4.0", "1.3.14"}) {
            BunRelease bunRelease = BunRelease.of(release);
            PackageManifest manifest = requireNonNull(PackageManifest.parse(bunRelease, new Log(), packument, "a",
              false));
            PackageManifest.FindResult pick = requireNonNull(manifest.findBestVersion(
              requireNonNull(DependencyVersion.parse("a", Wyhash11.stringHash("a"), "1.0.0")).npm().version));
            Lockfile lockfile = new Lockfile(bunRelease);
            Package pkg = Package.fromNPM(lockfile, manifest, pick.version, pick.packageVersion, Features.NPM);
            assertThat(lockfile.dependencies.get(pkg.dependenciesOff).version.tag).as(release)
              .isEqualTo("1.4.0".equals(release) ? DependencyVersion.Tag.UNINITIALIZED : DependencyVersion.Tag.CATALOG);
        }
        String folder = "{\"name\":\"f\",\"dependencies\":{\"ms\":\"catalog:\"}}";
        assertThat(firstDependency("1.4.0", folder, Features.FOLDER).version.tag)
          .isEqualTo(DependencyVersion.Tag.UNINITIALIZED);
        assertThat(firstDependency("1.3.14", folder, Features.FOLDER).version.tag)
          .isEqualTo(DependencyVersion.Tag.CATALOG);
        assertThat(firstDependency("1.4.0", folder, Features.WORKSPACE).version.tag)
          .isEqualTo(DependencyVersion.Tag.CATALOG);
    }

    private static Dependency firstDependency(String release, String manifest, Features features) {
        Lockfile lockfile = new Lockfile(BunRelease.of(release));
        Package pkg = new Package();
        pkg.parseWithJSON(lockfile, options(release, null), new Log(), "f", Json.parse(manifest), features,
          Collections.emptyMap());
        return lockfile.dependencies.get(pkg.dependenciesOff);
    }

    /**
     * Package.rs {@code parse_dependency} (1.4.0, bun#37669): a folder's {@code workspace:} spec names no workspace
     * path; the root's and a workspace's still do.
     */
    @Test
    void registersTheWorkspaceSpecsOfTheWorkspacesOnlyFrom140() {
        String folder = "{\"name\":\"f\",\"dependencies\":{\"w\":\"workspace:*\"}}";
        for (String release : new String[]{"1.4.0", "1.3.14"}) {
            for (Features features : new Features[]{Features.FOLDER, Features.WORKSPACE}) {
                Lockfile lockfile = new Lockfile(BunRelease.of(release));
                new Package().parseWithJSON(lockfile, options(release, null), new Log(), "f", Json.parse(folder),
                  features, Collections.emptyMap());
                assertThat(lockfile.workspacePaths.containsKey(Wyhash11.stringHash("w"))).as(release)
                  .isEqualTo(!"1.4.0".equals(release) || features == Features.WORKSPACE);
            }
        }
    }

    /**
     * bun.lock.rs {@code find_resolution_bounded_at_bundle} (1.4.0): the optional peer c of the bundled b/a is looked up
     * no higher than b, so that it stays unresolved; before, the walk reaches the root's c.
     */
    @Test
    void boundsTheOptionalPeersOfABundleFrom140() {
        String lock = lockWith(rootWith("{ \"b\": \"1.0.0\", \"c\": \"1.0.0\" }"),
          npm("b", "b@1.0.0", "{ \"dependencies\": { \"a\": \"1.0.0\" } }") +
          npm("b/a", "a@1.0.0", "{ \"peerDependencies\": { \"c\": \"^1.0.0\" }, \"optionalPeers\": [\"c\"], " +
                                "\"bundled\": true }") +
          npm("c", "c@1.0.0", "{}"));
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.4.0", null)), "a", "c")).isNull();
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.3.14", null)), "a", "c"))
          .isEqualTo("c@1.0.0");
    }

    /**
     * bun.lock.rs (1.4.0, bun#38765): a lock with workspaces and no {@code "packages"} binds the root's edges to the
     * workspaces; before, it leaves them without resolutions.
     */
    @Test
    void resolvesALockWithoutPackagesFrom140() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"r\",\n" +
                      "    },\n    \"packages/a\": {\n      \"name\": \"a\",\n    },\n  }\n}\n";
        Lockfile later = TextLockfileReader.load(lock, options("1.4.0", null));
        assertThat(later.resolutions).hasSameSizeAs(later.dependencies);
        assertThat(resolutionOf(later, "r", "a")).isEqualTo("a@workspace:packages/a");
        assertThat(TextLockfileReader.load(lock, options("1.3.14", null)).resolutions).isEmpty();
    }

    /**
     * bun.lock.rs (1.4.3, bun#42884): the bundled mark of a tarball package, in its entry's second element, is read;
     * before, only a third element's.
     */
    @Test
    void findsTheBundledMarkInAnyEntryFrom143() {
        String lock = lockWith(rootWith("{ \"b\": \"1.0.0\" }"),
          npm("b", "b@1.0.0", "{ \"dependencies\": { \"a\": \"https://example.com/a.tgz\" } }") +
          "    \"b/a\": [\"a@https://example.com/a.tgz\", { \"bundled\": true }],\n");
        assertThat(dependencyOf(TextLockfileReader.load(lock, options("1.4.3", null)), "b", "a").behavior.isBundled())
          .isTrue();
        assertThat(dependencyOf(TextLockfileReader.load(lock, options("1.4.2", null)), "b", "a").behavior.isBundled())
          .isFalse();
    }

    /**
     * bun.lock.rs {@code tag_workspace_links} (1.4.1, bun#41302): a root's dist-tag edge the lock resolves to a
     * workspace stays a dist tag, as a reparse of package.json gives it; a range the workspace's version satisfies
     * becomes a workspace link; before, both do.
     */
    @Test
    void tagsWorkspaceLinksAsAReparseDoesFrom141() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n" +
                      rootWith("{ \"a\": \"latest\", \"b\": \"^1.0.0\" }") +
                      "    \"packages/a\": {\n      \"name\": \"a\",\n      \"version\": \"1.0.0\",\n    },\n" +
                      "    \"packages/b\": {\n      \"name\": \"b\",\n      \"version\": \"1.0.0\",\n    },\n" +
                      "  },\n  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n" +
                      "    \"b\": [\"b@workspace:packages/b\"],\n  }\n}\n";
        Lockfile later = TextLockfileReader.load(lock, options("1.4.1", null));
        Lockfile earlier = TextLockfileReader.load(lock, options("1.4.0", null));
        for (Lockfile lockfile : new Lockfile[]{later, earlier}) {
            Package root = lockfile.packages.get(0);
            for (int id = root.dependenciesOff; id < root.dependenciesEnd(); id++) {
                Dependency dep = lockfile.dependencies.get(id);
                if (dep.behavior.isWorkspace()) {
                    continue;
                }
                DependencyVersion.Tag expected = lockfile == later && "a".equals(dep.name) ?
                  DependencyVersion.Tag.DIST_TAG : DependencyVersion.Tag.WORKSPACE;
                assertThat(dep.version.tag).as(dep.name + " at " + lockfile.release).isEqualTo(expected);
                assertThat(lockfile.packages.get(lockfile.resolutions.get(id)).name).isEqualTo(dep.name);
            }
        }
    }

    /**
     * Package.rs {@code parse_dependency} with lockfile.rs {@code linked_workspace_path} (1.4.1, bun#41302): the root's
     * {@code *} links the workspace without a version; before, only a version links.
     */
    @Test
    void linksAStarRangeToAWorkspaceWithoutVersionFrom141() {
        Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", "{\"name\":\"r\",\"workspaces\":[\"packages/a\"],\"dependencies\":{\"a\":\"*\"}}");
        manifests.put("packages/a", "{\"name\":\"a\"}");
        assertThat(rootDependencyTag("1.4.1", manifests, "a", false)).isEqualTo(DependencyVersion.Tag.WORKSPACE);
        assertThat(rootDependencyTag("1.4.0", manifests, "a", false)).isEqualTo(DependencyVersion.Tag.NPM);
    }

    // the version tag of the root's dependency of the name, workspace or not
    private static DependencyVersion.Tag rootDependencyTag(String release, Map<String, String> manifests,
                                                           String name, boolean workspace) {
        Lockfile lockfile = new Lockfile(BunRelease.of(release));
        Package root = new Package();
        root.parseWithJSON(lockfile, options(release, null), new Log(), "", Json.parse(manifests.get("")),
          Features.MAIN, manifests);
        for (Dependency dep : lockfile.dependenciesOf(root)) {
            if (dep.name.equals(name) && dep.behavior.isWorkspace() == workspace) {
                return dep.version.tag;
            }
        }
        throw new AssertionError("no " + name);
    }

    /**
     * Package.rs parse_with_json (1.4.1, bun#39816): a workspace's patchedDependencies are not read; the root's are.
     */
    @Test
    void readsPatchedDependenciesFromTheRootOnlyFrom141() {
        String manifest = "{\"name\":\"a\",\"patchedDependencies\":{\"x@1.0.0\":\"patches/x.patch\"}}";
        assertThat(patched("1.4.1", manifest, Features.WORKSPACE)).isEmpty();
        assertThat(patched("1.4.0", manifest, Features.WORKSPACE)).hasSize(1);
        assertThat(patched("1.4.1", manifest, Features.MAIN)).hasSize(1);
    }

    private static Map<Long, String> patched(String release, String manifest, Features features) {
        Lockfile lockfile = new Lockfile(BunRelease.of(release));
        new Package().parseWithJSON(lockfile, options(release, null), new Log(), "", Json.parse(manifest), features,
          Collections.emptyMap());
        return lockfile.patchedDependencies;
    }

    /**
     * Package.rs parse_with_json (1.4.1, bun#40014): the root's {@code workspaces.selfContained} is an array of
     * strings, or the package.json is invalid; before, it is not read.
     */
    @ParameterizedTest
    @ValueSource(strings = {"\"apps\"", "[1]"})
    void readsSelfContainedWorkspacesFrom141(String selfContained) {
        String manifest = "{\"name\":\"r\",\"workspaces\":{\"packages\":[],\"selfContained\":" + selfContained + "}}";
        Log log = new Log();
        assertThatThrownBy(() -> new Package().parseWithJSON(new Lockfile(BunRelease.of("1.4.1")),
          options("1.4.1", null), log, "", Json.parse(manifest), Features.MAIN, Collections.emptyMap()))
          .hasMessage("InvalidPackageJSON");
        assertThat(log.errors.get(0)).startsWith("\"workspaces.selfContained\" ");
        new Package().parseWithJSON(new Lockfile(BunRelease.of("1.4.0")), options("1.4.0", null), new Log(), "",
          Json.parse(manifest), Features.MAIN, Collections.emptyMap());
    }

    /**
     * Tree.rs {@code hoist_dependency} (1.4.0, bun#36303): w's foo ^1, besides its dev foo ^2 hoisted to the root, is
     * deduped onto that edge of w's own; before, it nests as "w/foo".
     */
    @Test
    void dedupesAPackagesEdgesToOneNameFrom140() {
        String root = "{\"name\":\"r\",\"workspaces\":[\"packages/w\"]}";
        String member = "{\"name\":\"w\",\"dependencies\":{\"foo\":\"^1.0.0\"},\"devDependencies\":{\"foo\":\"^2.0.0\"}}";
        ManifestSource registry = (url, name, extended) -> "foo".equals(name) ?
          "{\"name\":\"foo\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":\"2.0.0\"}," +
          "\"versions\":{" + fooVersion("1.0.0") + "," + fooVersion("2.0.0") + "}}" : null;
        BunLockRegenerator.Inputs.InputsBuilder inputs = BunLockRegenerator.Inputs.builder()
          .lock(EMPTY_ROOT)
          .manifest("", root)
          .manifest("packages/w", member)
          .now(java.time.Instant.parse("2026-10-11T00:00:00Z"));
        org.openrewrite.javascript.internal.LockFileRegeneration.Result later =
          new BunLockRegenerator(inputs.release(BunRelease.of("1.4.0")).build(), registry).regenerate();
        assertThat(later.getLockFileContent()).as(String.valueOf(later.getErrorMessage()))
          .contains("\"foo\": [\"foo@2.0.0\"").doesNotContain("\"w/foo\"");
        org.openrewrite.javascript.internal.LockFileRegeneration.Result earlier =
          new BunLockRegenerator(inputs.release(BunRelease.of("1.3.14")).build(), registry).regenerate();
        String written = earlier.isSuccess() ? earlier.getLockFileContent() :
          requireNonNull(earlier.getFailure()).getWritten();
        assertThat(written).as(String.valueOf(earlier.getErrorMessage())).contains("\"w/foo\": [\"foo@1.0.0\"");
    }

    private static String fooVersion(String version) {
        return "\"" + version + "\":{\"name\":\"foo\",\"version\":\"" + version + "\",\"dist\":{\"tarball\":" +
               "\"https://registry.npmjs.org/foo/-/foo-" + version + ".tgz\",\"integrity\":\"" + INTEGRITY + "\"}}";
    }

    /**
     * Tree.rs {@code hoist_dependency} (1.4.0, bun#38333): ui's {@code catalog:} peer react, its catalog range
     * satisfied by the react a hoisted to the root, dedupes; before, no version satisfies it, and it nests as
     * "ui/react".
     */
    @Test
    void dedupesACatalogPeerByItsRangeFrom140() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"r\",\n" +
                      "    },\n    \"packages/a\": {\n      \"name\": \"a\",\n      \"dependencies\": {\n" +
                      "        \"react\": \"18.2.0\",\n      },\n    },\n    \"packages/ui\": {\n" +
                      "      \"name\": \"ui\",\n      \"peerDependencies\": {\n        \"react\": \"catalog:\",\n" +
                      "      },\n    },\n  },\n  \"catalog\": {\n    \"react\": \"^18.0.0\",\n  },\n" +
                      "  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n" +
                      npm("react", "react@18.2.0", "{}") +
                      "    \"ui\": [\"ui@workspace:packages/ui\"],\n" +
                      npm("ui/react", "react@18.3.1", "{}") +
                      "  }\n}\n";
        assertThat(writtenBack("1.4.0", lock)).doesNotContain("\"ui/react\"");
        assertThat(writtenBack("1.3.14", lock)).contains("\"ui/react\": [\"react@18.3.1\"");
    }

    /**
     * Tree.rs {@code Rebind} (1.4.0, bun#37426): a's optional peer p, locked to the nested 1.0.0, dedupes onto the
     * root's p 1.2.0 and is bound to it; before, it keeps 1.0.0.
     */
    @Test
    void rebindsAnOptionalPeerItDedupesFrom140() {
        String lock = lockWith(rootWith("{ \"a\": \"1.0.0\", \"p\": \"1.2.0\" }"),
          npm("a", "a@1.0.0", "{ \"peerDependencies\": { \"p\": \"^1.0.0\" }, \"optionalPeers\": [\"p\"] }") +
          npm("a/p", "p@1.0.0", "{}") +
          npm("p", "p@1.2.0", "{}"));
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.4.0", null)), "a", "p")).isEqualTo("p@1.2.0");
        assertThat(resolutionOf(TextLockfileReader.load(lock, options("1.3.14", null)), "a", "p")).isEqualTo("p@1.0.0");
    }

    /**
     * lockfile.rs {@code resolve} (1.4.0, bun#37426): a's unresolved optional peer p, placed at the root before b's p
     * replaces it there, is bound late, so that the hoist runs again and the root's folder holds a's edge; before, one
     * hoist leaves b's there.
     */
    @Test
    void hoistsAgainAfterAnOptionalPeerIsBoundLateFrom140() {
        String lock = lockWith(rootWith("{ \"a\": \"1.0.0\", \"b\": \"1.0.0\" }"),
          npm("a", "a@1.0.0", "{ \"peerDependencies\": { \"p\": \"^1.0.0\" }, \"optionalPeers\": [\"p\"] }") +
          npm("b", "b@1.0.0", "{ \"dependencies\": { \"p\": \"1.0.0\" } }") +
          npm("p", "p@1.0.0", "{}"));
        for (String release : new String[]{"1.4.0", "1.3.14"}) {
            Lockfile lockfile = TextLockfileReader.load(lock, options(release, null));
            int aPeer = dependencyIdOf(lockfile, "a", "p");
            lockfile.resolutions.set(aPeer, Package.INVALID_PACKAGE_ID);
            lockfile.resolve();
            assertThat(lockfile.trees.get(0).dependencies).as(release)
              .contains("1.4.0".equals(release) ? aPeer : dependencyIdOf(lockfile, "b", "p"));
            assertThat(resolutionOf(lockfile, "a", "p")).isEqualTo("p@1.0.0");
        }
    }

    /**
     * lockfile.rs {@code clean_with_logger} and Package.rs {@code clone} (1.4.0): an install that changes resolutions
     * drops p, which only a's optional peer resolves to; before, and without changes, p stays.
     */
    @Test
    void dropsWhatOnlyOptionalPeersHoldFrom140() {
        String lock = lockWith(rootWith("{ \"a\": \"1.0.0\" }"),
          npm("a", "a@1.0.0", "{ \"peerDependencies\": { \"p\": \"^1.0.0\" }, \"optionalPeers\": [\"p\"] }") +
          npm("p", "p@1.0.0", "{}"));
        assertThat(cleaned("1.4.0", lock, false)).doesNotContain("\"p\": [\"p@1.0.0\"");
        assertThat(cleaned("1.4.0", lock, true)).contains("\"p\": [\"p@1.0.0\"");
        assertThat(cleaned("1.3.14", lock, false)).contains("\"p\": [\"p@1.0.0\"");
    }

    private static String cleaned(String release, String lock, boolean keepOptionalPeerTargets) {
        BunOptions options = options(release, null);
        return TextLockfileWriter.saveFromBinary(Cleaner.clean(TextLockfileReader.load(lock, options),
          keepOptionalPeerTargets), 0, options.getRelease());
    }

    // the root's diff of a lock loaded as the release against the project's manifests, with the id mapping it fills
    private static PackageDiff.Summary rootDiff(String release, String lock, Map<String, String> manifests,
                                                int[][] mapping) {
        return rootDiff(options(release, null), lock, manifests, mapping);
    }

    private static PackageDiff.Summary rootDiff(BunOptions options, String lock, Map<String, String> manifests,
                                                int[][] mapping) {
        Log log = new Log();
        Lockfile from = TextLockfileReader.load(lock, options);
        Lockfile to = new Lockfile(options.getRelease());
        Package root = new Package();
        root.parseWithJSON(to, options, log, "", Json.parse(manifests.get("")), Features.MAIN, manifests);
        mapping[0] = new int[root.dependenciesLen];
        java.util.Arrays.fill(mapping[0], Package.INVALID_PACKAGE_ID);
        return PackageDiff.generate(options, log, from, to, from.packages.get(0), root, mapping[0], manifests);
    }

    /**
     * Package.rs {@code Diff::generate} (1.4.0): lodash 4.17.20, locked for "4.17.20", stays where the range becomes
     * ^4.17.15, which accepts it; before, it is resolved again. Either way it is an update.
     */
    @Test
    void keepsALockedVersionTheNewRangeAcceptsFrom140() {
        String lock = lockWith(rootWith("{ \"lodash\": \"4.17.20\" }"), npm("lodash", "lodash@4.17.20", "{}"));
        Map<String, String> manifests = Collections.singletonMap("",
          "{\"name\":\"r\",\"dependencies\":{\"lodash\":\"^4.17.15\"}}");
        int[][] mapping = new int[1][];
        assertThat(rootDiff("1.4.0", lock, manifests, mapping).update).isEqualTo(1);
        assertThat(mapping[0]).containsExactly(0);
        assertThat(rootDiff("1.3.14", lock, manifests, mapping).update).isEqualTo(1);
        assertThat(mapping[0]).containsExactly(Package.INVALID_PACKAGE_ID);
    }

    private static Map<String, String> workspace(String member) {
        Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", "{\"name\":\"r\",\"workspaces\":[\"packages/*\"]}");
        manifests.put("packages/a", member);
        return manifests;
    }

    private static final String WORKSPACE_A = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n" +
                                              "      \"name\": \"r\",\n    },\n    \"packages/a\": {\n" +
                                              "      \"name\": \"a\",\n      \"version\": \"1.0.0\",\n    },\n" +
                                              "  },\n  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n" +
                                              "  }\n}\n";

    /**
     * Package.rs {@code Diff::generate} (1.4.0): a workspace whose package.json changed its trusted dependencies alone
     * keeps its resolution; before, any diff is an update.
     */
    @Test
    void keepsAWorkspaceWhoseDiffChangesNoResolutionFrom140() {
        Map<String, String> manifests = workspace("{\"name\":\"a\",\"version\":\"1.0.0\",\"trustedDependencies\":" +
                                                  "[\"x\"]}");
        int[][] mapping = new int[1][];
        assertThat(rootDiff("1.4.0", WORKSPACE_A, manifests, mapping).hasDiffs()).isFalse();
        assertThat(mapping[0]).containsExactly(0);
        assertThat(rootDiff("1.3.14", WORKSPACE_A, manifests, mapping).update).isEqualTo(1);
        assertThat(mapping[0]).containsExactly(Package.INVALID_PACKAGE_ID);
    }

    /**
     * Package.rs {@code Diff::generate} (1.4.1, bun#41302): a workspace's version change alone is a diff, which writes
     * bun.lock again; before, it is none.
     */
    @Test
    void diffsWorkspaceVersionsFrom141() {
        Map<String, String> manifests = workspace("{\"name\":\"a\",\"version\":\"1.1.0\"}");
        int[][] mapping = new int[1][];
        PackageDiff.Summary later = rootDiff("1.4.1", WORKSPACE_A, manifests, mapping);
        assertThat(later.workspaceVersionsChanged).isTrue();
        assertThat(later.hasDiffs()).isTrue();
        assertThat(later.changesResolutions()).isFalse();
        assertThat(rootDiff("1.4.0", WORKSPACE_A, manifests, mapping).hasDiffs()).isFalse();
        assertThat(rootDiff("1.4.1", WORKSPACE_A, workspace("{\"name\":\"a\",\"version\":\"1.0.0\"}"), mapping)
          .hasDiffs()).isFalse();
    }

    /**
     * Package.rs {@code Diff::generate} with pruned_workspaces.rs (1.4.0): a depends on b, which bun.lock lists but
     * whose package.json is gone, and the install fails; before, b is a removal like any other. A frozen install does
     * not count b removed where nothing depends on it.
     */
    @Test
    void failsOnAWorkspaceMissingFromDiskFrom140() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"r\",\n" +
                      "    },\n    \"packages/a\": {\n      \"name\": \"a\",\n      \"dependencies\": {\n" +
                      "        \"b\": \"workspace:*\",\n      },\n    },\n    \"packages/b\": {\n" +
                      "      \"name\": \"b\",\n    },\n  },\n  \"packages\": {\n" +
                      "    \"a\": [\"a@workspace:packages/a\"],\n    \"b\": [\"b@workspace:packages/b\"],\n  }\n}\n";
        Map<String, String> manifests = workspace("{\"name\":\"a\",\"dependencies\":{\"b\":\"workspace:*\"}}");
        int[][] mapping = new int[1][];
        assertThatThrownBy(() -> rootDiff("1.4.0", lock, manifests, mapping))
          .isInstanceOf(org.openrewrite.javascript.internal.lock.Rejected.class)
          .hasMessage("workspace \"a\" depends on workspace \"b\" (packages/b), which is listed in bun.lock but not " +
                      "on disk");
        assertThat(rootDiff("1.3.14", lock, manifests, mapping).remove).isEqualTo(1);

        String alone = lock.replace("\"b\": \"workspace:*\"", "\"c\": \"1.0.0\"").replace("\"packages\": {\n",
          "\"packages\": {\n" + npm("c", "c@1.0.0", "{}"));
        Map<String, String> unrelated = workspace("{\"name\":\"a\",\"dependencies\":{\"c\":\"1.0.0\"}}");
        BunOptions frozen = BunOptions.load(BunRelease.of("1.4.0"), "[install]\nfrozenLockfile = true\n", null, null,
          HostEnvironment.UNKNOWN);
        PackageDiff.Summary pruned = rootDiff(frozen, alone, unrelated, mapping);
        assertThat(pruned.remove).isZero();
        assertThat(pruned.prunedWorkspaces).containsExactly(Wyhash11.stringHash("b"));
        assertThat(rootDiff("1.4.0", alone, unrelated, mapping).remove).isEqualTo(1);
    }

    /**
     * bun.lock.rs {@code write_workspace_deps} (1.4.1, bun#40809): a root's dependencies are written by their names
     * as they are, so that one renamed since its parse moves; before, in the order of the parse.
     */
    @Test
    void sortsWorkspaceDependenciesByNameFrom141() {
        String lock = lockWith(rootWith("{ \"b\": \"1.0.0\", \"c\": \"1.0.0\" }"),
          npm("b", "b@1.0.0", "{}") + npm("c", "c@1.0.0", "{}"));
        for (String release : new String[]{"1.4.1", "1.4.0"}) {
            BunOptions options = options(release, null);
            Lockfile lockfile = Cleaner.clean(TextLockfileReader.load(lock, options));
            // as assign_resolution renames an edge named by its literal
            lockfile.dependencies.get(dependencyIdOf(lockfile, "r", "c")).name = "a";
            String workspaces = TextLockfileWriter.saveFromBinary(lockfile, 0, options.getRelease()).split("\"packages\"")[0];
            assertThat(workspaces.indexOf("\"a\": \"1.0.0\"") < workspaces.indexOf("\"b\": \"1.0.0\"")).as(release)
              .isEqualTo("1.4.1".equals(release));
        }
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
