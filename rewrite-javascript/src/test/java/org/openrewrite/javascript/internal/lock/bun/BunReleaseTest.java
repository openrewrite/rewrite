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
