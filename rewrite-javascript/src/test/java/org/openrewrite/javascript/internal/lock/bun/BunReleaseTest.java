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
