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
     * integrity.zig Integrity.parse (1.2.20, 1.2.21): a sha1 is decoded from past its {@code -} from 1.2.20, and a
     * hash longer than its digest is unknown from 1.2.21. As a lock's sha1 of 20 bytes and sha1 of 32 bytes are
     * written back: 1.2.19 both {@code ""}; 1.2.20 both, the second cut to 20 bytes; 1.2.21 the first alone.
     */
    @Test
    void decodesAnIntegrityPastItsAlgorithmFrom1220AndRefusesOneLongerThanItsDigestFrom1221() {
        String sha1 = "sha1-AQIDBAUGBwgJCgsMDQ4PEBESExQ=";
        String long32 = "sha1-AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=";
        assertThat(Integrity.parse(sha1, BunRelease.of("1.2.20"))).hasToString(sha1);
        assertThat(Integrity.parse(sha1, BunRelease.of("1.2.19"))).hasToString("");
        assertThat(Integrity.parse(long32, BunRelease.of("1.2.21"))).hasToString("");
        assertThat(Integrity.parse(long32, BunRelease.of("1.2.20"))).hasToString(sha1);
        assertThat(Integrity.parse(long32, BunRelease.of("1.2.19"))).hasToString("");
        assertThat(Integrity.parse("sha256-", BunRelease.of("1.2.19")))
          .hasToString("sha256-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
        assertThat(Integrity.parse("sha256-", BunRelease.of("1.2.20"))).hasToString("");

        String lock = lock("", "\"ms\": [\"ms@2.1.3\", \"\", {}, \"" + sha1 + "\"],");
        assertThat(writtenBack("1.2.20", lock)).isEqualTo(lock);
        assertThat(writtenBack("1.2.19", lock)).isEqualTo(lock.replace(sha1, ""));

        StringBuilder past64 = new StringBuilder("sha512-");
        for (int i = 0; i < 88; i++) {
            past64.append('A');
        }
        assertThatThrownBy(() -> Integrity.parse(past64.toString(), BunRelease.of("1.2.20")))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("past its 64-byte buffer");
        assertThat(Integrity.parse(past64.toString(), BunRelease.of("1.2.21"))).hasToString("");
        assertThat(Integrity.parse(past64.toString().replaceFirst("AAAA", "!AAA"), BunRelease.of("1.2.20")))
          .hasToString("");
    }

    /**
     * migration.zig (1.2.23): a pnpm-lock.yaml is migrated where no package-lock.json or yarn.lock comes first, and
     * package.json rewritten; before, it is ignored, so that only a bun.lockb stops the walk for the install root.
     * (Beside a pnpm-lock.yaml, 1.2.23 prints {@code migrated lockfile from pnpm-lock.yaml}; 1.2.22 installs afresh.)
     */
    @Test
    void migratesAPnpmLockFrom1223() {
        java.util.Set<String> pnpm = java.util.Collections.singleton("pnpm-lock.yaml");
        assertThat(Lockfile.rewritesAnotherFile(pnpm::contains, BunRelease.of("1.2.23"))).isTrue();
        assertThat(Lockfile.rewritesAnotherFile(pnpm::contains, BunRelease.of("1.2.22"))).isFalse();
        java.util.Set<String> yarnFirst = new java.util.HashSet<>(java.util.Arrays.asList("pnpm-lock.yaml", "yarn.lock"));
        assertThat(Lockfile.rewritesAnotherFile(yarnFirst::contains, BunRelease.of("1.2.23"))).isFalse();
        java.util.Set<String> lockb = java.util.Collections.singleton("bun.lockb");
        assertThat(Lockfile.rewritesAnotherFile(lockb::contains, BunRelease.of("1.2.0"))).isTrue();
    }

    /**
     * install_with_manager.zig runAndWait (1.2.22): bunfig's security scanner runs for {@code bun install} too, whose
     * verdict the engine cannot know; 1.2.21 runs it for {@code bun add} alone. (A lock-only install naming a scanner
     * not installed: 1.2.21 saves the lock; 1.2.22 {@code error: Security scanner 'zz-no-such-scanner' is configured in
     * bunfig.toml but is not installed.})
     */
    @Test
    void runsTheSecurityScannerOnInstallFrom1222() {
        String bunfig = "[install.security]\nscanner = \"@acme/bun-scanner\"\n";
        assertThat(BunOptions.load(BunRelease.of("1.2.22"), bunfig, null, null, HostEnvironment.UNKNOWN)
          .getSecurityScanner()).isEqualTo("@acme/bun-scanner");
        assertThat(BunOptions.load(BunRelease.of("1.2.21"), bunfig, null, null, HostEnvironment.UNKNOWN)
          .getSecurityScanner()).isNull();
        BunLockRegenerator.Inputs.InputsBuilder inputs = BunLockRegenerator.Inputs.builder()
          .lock(EMPTY_ROOT)
          .manifest("", "{\"name\":\"root\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .bunfig(bunfig)
          .now(java.time.Instant.parse("2026-10-11T00:00:00Z"));
        ManifestSource registry = (url, name, extended) -> "ms".equals(name) ?
          registryPackument("ms", "2.1.3", registryVersion("ms", "2.1.3", "")) : null;
        assertThat(failure(new BunLockRegenerator(inputs.release(BunRelease.of("1.3.10")).build(), registry)
          .regenerate()))
          .startsWith("CANNOT_KNOW the engine cannot know whether bun 1.3.10 saves the lock past the security scanner");
        assertThat(written(new BunLockRegenerator(inputs.release(BunRelease.of("1.2.21")).build(), registry)
          .regenerate())).contains("\"ms\": [\"ms@2.1.3\"");
    }

    /**
     * ini.zig loadNpmrcConfig (1.2.13): an .npmrc's byte order mark is dropped; before, it starts the first key. (With
     * bunfig naming one local registry and such an .npmrc another, 1.2.13 asks the .npmrc's, 1.2.12 bunfig's.)
     */
    @Test
    void stripsTheByteOrderMarkOfNpmrcFrom1213() {
        String npmrc = "﻿registry=https://registry.npmmirror.com/\n";
        assertThat(options("1.2.13", npmrc).scopeForPackageName("ms")).isEqualTo("https://registry.npmmirror.com/");
        assertThat(options("1.2.12", npmrc).scopeForPackageName("ms")).isEqualTo("https://registry.npmjs.org/");
    }

    /**
     * bunfig.zig (1.2.16) and ini.zig loadNpmrc (1.2.19): {@code linkWorkspacePackages} and
     * {@code link-workspace-packages} are read from those releases; before, neither is a setting. (Turned off, a
     * root's {@code "a": "1.0.0"} beside the member a 1.0.0 goes to the registry with 1.2.16 and 1.2.19 and links the
     * member with 1.2.15 and 1.2.18.)
     */
    @Test
    void readsLinkWorkspacePackagesFromBunfigFrom1216AndNpmrcFrom1219() {
        String bunfig = "[install]\nlinkWorkspacePackages = false\n";
        assertThat(BunOptions.load(BunRelease.of("1.2.16"), bunfig, null, null, HostEnvironment.UNKNOWN)
          .isLinkWorkspacePackages()).isFalse();
        assertThat(BunOptions.load(BunRelease.of("1.2.15"), bunfig, null, null, HostEnvironment.UNKNOWN)
          .isLinkWorkspacePackages()).isTrue();
        String npmrc = "link-workspace-packages=false\n";
        assertThat(options("1.2.19", npmrc).isLinkWorkspacePackages()).isFalse();
        assertThat(options("1.2.18", npmrc).isLinkWorkspacePackages()).isTrue();
    }

    /**
     * toml_parser.zig parseValue (1.2.16): {@code [[} is one token again after an inline table's close; before, a
     * table array header there is two {@code [}s, and bunfig does not parse ({@code bun install} of 1.2.15:
     * {@code error: Expected key but found [}, {@code SyntaxError: failed to load bunfig}; 1.2.16 installs).
     */
    @Test
    void lexesADoubleBracketAfterAValueFrom1216() {
        String bunfig = "[install]\nscopes = { a = \"https://a.example/\" }\n[[x]]\ny = 1\n";
        assertThat(Toml.parse(bunfig, BunRelease.of("1.2.16")).getObject("install")).isNotNull();
        assertThatThrownBy(() -> Toml.parse(bunfig, BunRelease.of("1.2.15")))
          .isInstanceOf(Toml.SyntaxError.class)
          .hasMessage("Expected key but found [");
        String afterAnArray = "[install]\nx = [1]\n[[x]]\ny = 1\n";
        assertThat(Toml.parse(afterAnArray, BunRelease.of("1.2.15")).getObject("install")).isNotNull();
    }

    /**
     * lockfile.zig Package.parseWithJSON (1.2.14): a workspaces object without packages declares none; before, it
     * fails as a value neither array nor object does, with the old wording ({@code "workspaces": {"nohoist": []}}:
     * 1.2.13 {@code error: Workspaces expects an array of strings, e.g.}; 1.2.14 installs).
     */
    @Test
    void acceptsAWorkspacesObjectWithoutPackagesFrom1214() {
        String catalogOnly = "{\"name\":\"r\",\"workspaces\":{\"catalog\":{\"ms\":\"2.1.3\"}}}";
        assertThat(rootParseErrors("1.2.14", catalogOnly)).isEmpty();
        assertThat(rootParseErrors("1.2.13", catalogOnly)).containsExactly("Workspaces expects an array of strings");
        String string = "{\"name\":\"r\",\"workspaces\":\"packages/*\"}";
        assertThat(rootParseErrors("1.2.14", string)).containsExactly("\"workspaces\" expects an array of strings");
        assertThat(rootParseErrors("1.2.13", string)).containsExactly("Workspaces expects an array of strings");
    }

    // the errors parsing the root's package.json logs
    private static List<String> rootParseErrors(String release, String manifest) {
        Log log = new Log();
        try {
            new Package().parseWithJSON(new Lockfile(BunRelease.of(release)), options(release, null), log, "",
              Json.parse(manifest), Features.MAIN, java.util.Collections.singletonMap("", manifest));
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("InvalidPackageJSON");
        }
        return log.errors;
    }

    /**
     * The recipes' reading of catalogs: none before 1.2.14 ({@code catalog:} a git spec), the workspaces object's
     * alone before 1.2.19, the root's own where the workspaces hold none from 1.2.19.
     */
    @Test
    void theRecipesReadCatalogsAsTheReleaseDoes() {
        String topLevel = "{\"name\":\"r\",\"workspaces\":[\"packages/*\"],\"catalog\":{\"ms\":\"2.1.3\"}}";
        String nested = "{\"name\":\"r\",\"workspaces\":{\"packages\":[\"packages/*\"],\"catalog\":{\"ms\":\"2.1.3\"}}}";
        BunManifestRules before = BunManifestRules.of(BunRelease.of("1.2.13"));
        assertThat(before.catalogFile()).isNull();
        assertThat(before.catalogsAt(nested)).isNull();
        assertThat(before.catalogOf("catalog:")).isNull();
        BunManifestRules workspacesOnly = BunManifestRules.of(BunRelease.of("1.2.18"));
        assertThat(workspacesOnly.catalogsAt(nested)).containsExactly("workspaces");
        assertThat(workspacesOnly.catalogsAt(topLevel)).isNull();
        assertThat(workspacesOnly.catalogs(nested)).containsOnlyKeys("");
        assertThat(workspacesOnly.catalogOf("catalog:")).isEqualTo("");
        BunManifestRules later = BunManifestRules.of(BunRelease.of("1.2.19"));
        assertThat(later.catalogsAt(topLevel)).isEmpty();
        assertThat(later.catalogs(topLevel).get("")).containsEntry("ms", "2.1.3");
    }

    /**
     * default-trusted-dependencies.txt: {@code opencode-ai} from 1.2.16, {@code yarn} until 1.2.19,
     * {@code @anthropic-ai/claude-code} from 1.3.12.
     */
    @Test
    void trustsTheDefaultListOfTheRelease() {
        int opencode = (int) Wyhash11.stringHash("opencode-ai");
        int yarn = (int) Wyhash11.stringHash("yarn");
        int claude = (int) Wyhash11.stringHash("@anthropic-ai/claude-code");
        assertThat(DefaultTrustedDependencies.of(BunRelease.of("1.2.16"))).contains(opencode, yarn).doesNotContain(claude);
        assertThat(DefaultTrustedDependencies.of(BunRelease.of("1.2.15"))).contains(yarn).doesNotContain(opencode);
        assertThat(DefaultTrustedDependencies.of(BunRelease.of("1.2.19"))).contains(opencode).doesNotContain(yarn);
        assertThat(DefaultTrustedDependencies.of(BunRelease.of("1.3.12"))).contains(claude);
        assertThat(DefaultTrustedDependencies.of(BunRelease.of("1.3.11"))).doesNotContain(claude)
          .isEqualTo(DefaultTrustedDependencies.HASHES);
    }

    /**
     * js_printer.zig writePreQuotedString over highway_strings.cpp's escape search (1.2.11 until 1.3.0): a tab in the
     * last {@code len % N} bytes after a printed character is written as it is. As the darwin-arm64 releases (N = 16)
     * write a root named {@code p<tab>q}: 1.2.10 and 1.3.0 {@code "p\tq"}, 1.2.11 and 1.2.23 the tab itself; and
     * {@code p<tab>} and 20 {@code q}s escaped by 1.2.11, the tab in its vectors, which wider ones would not hold.
     */
    @Test
    void leavesATabInTheEscapeSearchsTailFrom1211Until130() {
        assertThat(json("p\tq", "1.3.0")).isEqualTo("\"p\\tq\"");
        assertThat(json("p\tq", "1.2.10")).isEqualTo("\"p\\tq\"");
        assertThat(json("p\tq", "1.2.11")).isEqualTo("\"p\tq\"");
        assertThat(json("p\tq", "1.2.23")).isEqualTo("\"p\tq\"");
        assertThat(json("\tq", "1.2.11")).isEqualTo("\"\\tq\"");
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            q.append('q');
        }
        // 81 bytes after the p: the tab is in the vectors of every width
        assertThat(json("p\t" + q, "1.2.11")).startsWith("\"p\\tq");
        // 21 bytes: in 16 lanes' vectors, in 32 lanes' scalar tail
        assertThatThrownBy(() -> json("p\t" + q.substring(0, 20), "1.2.11"))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("vector width");
    }

    private static String json(String text, String release) {
        StringBuilder out = new StringBuilder();
        Strings.appendJsonString(out, text, true, BunRelease.of(release));
        return out.toString();
    }

    /**
     * bun.lock.zig Stringifier (1.2.3): overrides and patched dependencies are JSON-escaped; before, written as they
     * are (an override {@code "x": "file:./a\"b"} the tree uses: 1.2.2 writes {@code "x": "file:./a"b",}, 1.2.3
     * {@code "x": "file:./a\"b",}).
     */
    @Test
    void escapesOverridesAndPatchedDependenciesFrom123() {
        String lock = lock("  \"patchedDependencies\": {\n    \"ms@2.1.3\": \"patches\\\\ms.patch\",\n  },\n" +
                           "  \"overrides\": {\n    \"ms\": \"file:..\\\\ms\",\n  },\n", MS);
        assertThat(writtenBack("1.2.3", lock))
          .contains("    \"ms@2.1.3\": \"patches\\\\ms.patch\",\n")
          .contains("    \"ms\": \"file:..\\\\ms\",\n");
        assertThat(writtenBack("1.2.2", lock))
          .contains("    \"ms@2.1.3\": \"patches\\ms.patch\",\n")
          .contains("    \"ms\": \"file:..\\ms\",\n");
    }

    /**
     * bun.lock.zig Stringifier (1.2.8, 1.2.14): bun.lock lists every override from 1.2.8, sorted by name from 1.2.14;
     * before 1.2.8 only those the hoisted tree's dependencies are named for.
     */
    @Test
    void writesEveryOverrideFrom128SortedFrom1214() {
        String lock = lock("  \"overrides\": {\n    \"zz\": \"1.0.0\",\n    \"ms\": \"2.1.3\",\n  },\n", MS);
        assertThat(writtenBack("1.2.14", lock)).contains("  \"overrides\": {\n    \"ms\": \"2.1.3\",\n" +
                                                         "    \"zz\": \"1.0.0\",\n  },\n");
        assertThat(writtenBack("1.2.13", lock)).contains("  \"overrides\": {\n    \"zz\": \"1.0.0\",\n" +
                                                         "    \"ms\": \"2.1.3\",\n  },\n");
        assertThat(writtenBack("1.2.8", lock)).contains("    \"zz\": \"1.0.0\",\n");
        assertThat(writtenBack("1.2.7", lock)).contains("  \"overrides\": {\n    \"ms\": \"2.1.3\",\n  },\n")
          .doesNotContain("zz");
    }

    /**
     * bun.lock.zig Stringifier over an install: three folder dependencies each overridden and an override nothing uses,
     * in package.json's order zz, c, a, b. As the real releases wrote it: 1.2.7 a, c, b (the used ones in their hash
     * map's slot order), 1.2.8 and 1.2.13 zz, c, a, b, 1.2.14 a, b, c, zz. A second install of 1.2.8 rewrites its lock
     * with a, b, c, zz, the diff having sorted the overrides in place, so that the engine declines the edit there.
     */
    @Test
    void writesTheOverridesOfAnInstallAsEachReleaseDid() {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", "{\"name\":\"p\",\"dependencies\":{\"a\":\"file:./a\",\"b\":\"file:./b\",\"c\":\"file:./c\"}," +
                          "\"overrides\":{\"zz\":\"1.0.0\",\"c\":\"file:./c\",\"a\":\"file:./a\",\"b\":\"file:./b\"}}");
        for (String name : new String[]{"a", "b", "c"}) {
            manifests.put(name, "{\"name\":\"" + name + "\",\"version\":\"1.0.0\"}");
        }
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n    },\n" +
                      "  },\n  \"packages\": {}\n}\n";
        java.util.Map<String, String> none = java.util.Collections.emptyMap();
        assertThat(overridesOf(written(install("1.2.7", lock, manifests, none)))).containsExactly("a", "c", "b");
        for (String release : new String[]{"1.2.8", "1.2.13"}) {
            assertThat(failure(install(release, lock, manifests, none))).isEqualTo("SECOND_INSTALL a second bun " +
              "install over the lock written for this edit would rewrite it: line 14 \"\"zz\": \"1.0.0\",\" becomes " +
              "\"\"a\": \"file:./a\",\"");
        }
        assertThat(overridesOf(written(install("1.2.14", lock, manifests, none)))).containsExactly("a", "b", "c", "zz");
    }

    // the keys of a written lock's overrides, in order
    private static List<String> overridesOf(String lock) {
        List<String> keys = new java.util.ArrayList<>();
        int start = lock.indexOf("  \"overrides\": {\n");
        if (start < 0) {
            return keys;
        }
        for (String line : lock.substring(start).split("\n")) {
            if (line.equals("  },")) {
                break;
            }
            if (line.startsWith("    \"")) {
                keys.add(line.substring(5, line.indexOf('"', 5)));
            }
        }
        return keys;
    }

    /**
     * lockfile.zig Package.Diff.generate (1.2.8): both sides' overrides are sorted before they are compared; before,
     * compared as read, so that the same overrides in another order are a change.
     */
    @Test
    void sortsOverridesBeforeComparingThemFrom128() {
        BunRelease later = BunRelease.of("1.2.8");
        BunRelease earlier = BunRelease.of("1.2.7");
        assertThat(OverrideMap.changed(overrideMap("ms", "zz"), overrideMap("zz", "ms"), later)).isFalse();
        assertThat(OverrideMap.changed(overrideMap("ms", "zz"), overrideMap("zz", "ms"), earlier)).isTrue();
        assertThat(OverrideMap.changed(overrideMap("ms", "zz"), overrideMap("ms", "zz"), earlier)).isFalse();
    }

    // plain overrides of the names, each to 1.0.0, in that order
    private static OverrideMap overrideMap(String... names) {
        OverrideMap overrides = new OverrideMap();
        for (String name : names) {
            long nameHash = Wyhash11.stringHash(name);
            overrides.map.put(nameHash, new Dependency(nameHash, name,
              requireNonNull(DependencyVersion.parse(name, nameHash, "1.0.0", BunRelease.PORTED)), Behavior.NONE));
        }
        return overrides;
    }

    /**
     * bun.lock.zig parseIntoBinaryLockfile (1.2.3 only): each package entry is a package of its own; before and after,
     * an entry equal to an earlier one is that package.
     */
    @Test
    void readsEveryPackageEntryAsItsOwnPackageIn123() {
        String lock = lockWith(rootWith("{ \"a\": \"1.0.0\", \"b\": \"1.0.0\" }"),
          npm("a", "a@1.0.0", "{ \"dependencies\": { \"b\": \"1.0.0\" } }") +
          npm("b", "b@1.0.0", "{}") +
          npm("a/b", "b@1.0.0", "{}"));
        assertThat(TextLockfileReader.load(lock, options("1.2.3", null)).packages).hasSize(4);
        assertThat(TextLockfileReader.load(lock, options("1.2.2", null)).packages).hasSize(3);
        assertThat(TextLockfileReader.load(lock, options("1.2.4", null)).packages).hasSize(3);
    }

    /**
     * bun.lock.zig parseIntoBinaryLockfile and mapDepToPkg (1.2.4): a dependency is bundled where its own key is
     * marked so; before, wherever the entry it binds to is, and a mark that is no boolean fails.
     */
    @Test
    void bundlesADependencyByItsOwnKeyFrom124() {
        String lock = lockWith(rootWith("{ \"a\": \"1.0.0\" }"),
          npm("a", "a@1.0.0", "{ \"dependencies\": { \"b\": \"1.0.0\", \"c\": \"1.0.0\" } }") +
          npm("a/b", "b@1.0.0", "{ \"bundled\": true }") +
          npm("a/c", "c@1.0.0", "{ \"dependencies\": { \"b\": \"1.0.0\" }, \"bundled\": true }"));
        Lockfile later = TextLockfileReader.load(lock, options("1.2.4", null));
        assertThat(dependencyOf(later, "a", "b").behavior.isBundled()).isTrue();
        assertThat(dependencyOf(later, "c", "b").behavior.isBundled()).isFalse();
        Lockfile earlier = TextLockfileReader.load(lock, options("1.2.3", null));
        assertThat(dependencyOf(earlier, "a", "b").behavior.isBundled()).isTrue();
        assertThat(dependencyOf(earlier, "c", "b").behavior.isBundled()).isTrue();

        String notABoolean = lock.replace("{ \"bundled\": true }", "{ \"bundled\": \"yes\" }");
        TextLockfileReader.load(notABoolean, options("1.2.4", null));
        assertThatThrownBy(() -> TextLockfileReader.load(notABoolean, options("1.2.3", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Expected a boolean");
    }

    /**
     * bun.lock.zig parseIntoBinaryLockfile (1.2.5): a workspace entry under a key no member took is bound to its
     * member; before, it is {@code Unknown workspace}. (A root's {@code "x": "workspace:packages/a"}: 1.2.4 writes the
     * entry {@code "x": ["a@workspace:packages/a"]} and, installing again, fails it with {@code error: Unknown
     * workspace: 'packages/a'}, {@code InvalidPackageInfo: failed to parse lockfile} and ignores the lock; 1.2.5 reads
     * it.)
     */
    @Test
    void readsAWorkspaceEntryUnderAnotherKeyFrom125() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n" +
                      rootWith("{ \"x\": \"workspace:*\" }") +
                      "    \"packages/a\": {\n      \"name\": \"a\",\n    },\n" +
                      "  },\n  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n" +
                      "    \"x\": [\"a@workspace:packages/a\"],\n  }\n}\n";
        TextLockfileReader.load(lock, options("1.2.5", null));
        assertThatThrownBy(() -> TextLockfileReader.load(lock, options("1.2.4", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Unknown workspace: 'packages/a'");
    }

    /**
     * bin.zig Bin.toJson(.multi_line) (1.2.3): a workspace's bin map has an entry a line; before, the entries follow
     * one another.
     */
    @Test
    void writesAWorkspaceBinAnEntryALineFrom123() {
        Bin map = new Bin(Bin.Tag.MAP, java.util.Arrays.asList("a", "a.js", "b", "b.js"));
        Bin named = new Bin(Bin.Tag.NAMED_FILE, java.util.Arrays.asList("a", "a.js"));
        assertThat(multiLine(map, "1.2.3")).isEqualTo("{\n      \"a\": \"a.js\",\n      \"b\": \"b.js\",\n    }");
        assertThat(multiLine(map, "1.2.2")).isEqualTo("{\n      \"a\": \"a.js\",      \"b\": \"b.js\"    }");
        assertThat(multiLine(named, "1.2.3")).isEqualTo("{\n      \"a\": \"a.js\",\n    }");
        assertThat(multiLine(named, "1.2.2")).isEqualTo("{\n      \"a\": \"a.js\"\n    }");
    }

    /**
     * As 1.2.2 and 1.2.3 wrote the workspaces m, of bins m1 and m2, and n, of the bin n1.
     */
    @Test
    void writesAWorkspacesBinAsEachReleaseDid() {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", "{\"name\":\"p\",\"workspaces\":[\"packages/*\"]}");
        manifests.put("packages/m", "{\"name\":\"m\",\"version\":\"1.0.0\",\"bin\":{\"m1\":\"m1.js\",\"m2\":\"m2.js\"}}");
        manifests.put("packages/n", "{\"name\":\"n\",\"version\":\"1.0.0\",\"bin\":{\"n1\":\"n1.js\"}}");
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n    },\n" +
                      "  },\n  \"packages\": {}\n}\n";
        java.util.Map<String, String> none = java.util.Collections.emptyMap();
        assertThat(written(install("1.2.2", lock, manifests, none)))
          .contains("      \"bin\": {\n        \"m1\": \"m1.js\",        \"m2\": \"m2.js\"      },\n")
          .contains("      \"bin\": {\n        \"n1\": \"n1.js\"\n      },\n");
        assertThat(written(install("1.2.3", lock, manifests, none)))
          .contains("      \"bin\": {\n        \"m1\": \"m1.js\",\n        \"m2\": \"m2.js\",\n      },\n")
          .contains("      \"bin\": {\n        \"n1\": \"n1.js\",\n      },\n");
    }

    // a bin as a workspace's in bun.lock, two levels in
    private static String multiLine(Bin bin, String release) {
        StringBuilder out = new StringBuilder();
        bin.toJsonMultiLine(out, new int[]{2}, BunRelease.of(release));
        return out.toString();
    }

    /**
     * install.zig enqueueDependencyWithMainAndSuccessFn (1.2.3): no override applies to the root's edge to a member;
     * before, an override of the member's name replaces it.
     */
    @Test
    void keepsOverridesOffTheRootsEdgeToAMemberFrom123() {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", "{\"name\":\"r\",\"workspaces\":[\"packages/a\"],\"overrides\":{\"a\":\"1.0.0\"}}");
        manifests.put("packages/a", "{\"name\":\"a\",\"version\":\"2.0.0\"}");
        java.util.Map<String, String> packuments = java.util.Collections.singletonMap("a",
          registryPackument("a", "1.0.0", registryVersion("a", "1.0.0", "")));
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"r\",\n    },\n" +
                      "    \"packages/a\": {\n      \"name\": \"a\",\n      \"version\": \"2.0.0\",\n    },\n  },\n" +
                      "  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n  }\n}\n";
        assertThat(written(install("1.2.3", lock, manifests, packuments)))
          .contains("\"a\": [\"a@workspace:packages/a\"]");
        assertThat(written(install("1.2.2", lock, manifests, packuments)))
          .contains("\"a\": [\"a@1.0.0\", \"\", {}, \"" + INTEGRITY + "\"]");
    }

    /**
     * As 1.2.2 and 1.2.3 installed a member a overridden by a folder b of the same name: 1.2.2 resolves the root's edge
     * to the folder and lists no workspace, 1.2.3 links the member.
     */
    @Test
    void overridesTheRootsEdgeToAMemberAsEachReleaseDid() {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        manifests.put("", "{\"name\":\"p\",\"workspaces\":[\"packages/a\"],\"overrides\":{\"a\":\"file:./b\"}}");
        manifests.put("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"}");
        manifests.put("b", "{\"name\":\"a\",\"version\":\"9.0.0\"}");
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n    },\n" +
                      "  },\n  \"packages\": {}\n}\n";
        java.util.Map<String, String> none = java.util.Collections.emptyMap();
        assertThat(written(install("1.2.2", lock, manifests, none))).isEqualTo("{\n  \"lockfileVersion\": 1,\n" +
          "  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n    },\n  },\n  \"overrides\": {\n" +
          "    \"a\": \"file:./b\",\n  },\n  \"packages\": {\n    \"a\": [\"a@file:b\", {}],\n  }\n}\n");
        assertThat(written(install("1.2.3", lock, manifests, none))).contains("    \"packages/a\": {\n")
          .contains("    \"a\": [\"a@workspace:packages/a\"],\n");
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
     * GlobWalker.zig makeComponent (1.3.4): {@code .x} in a workspaces glob is itself, a hidden directory the walker
     * passes over until 1.4.0; before, the walker takes it for {@code ..} and walks what the engine cannot see.
     */
    @Test
    void walksATwoCharacterDotComponentAsWrittenFrom134() {
        java.util.Map<String, String> manifests = java.util.Collections.singletonMap(".x/a", "{\"name\":\"a\"}");
        List<Json.Expr> globs = Json.parse("[\".x/*\"]").items();
        Workspaces later = new Workspaces(BunRelease.of("1.3.4"));
        later.processNamesArray(new Log(), globs, manifests);
        assertThat(later.map).isEmpty();
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
     * bun.lock.rs {@code find_resolution_bounded_at_bundle} (1.4.0): the optional peer c of the bundled b/a is looked
     * up no higher than b, so that it stays unresolved; before, the walk reaches the root's c.
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
        String member = "{\"name\":\"w\",\"dependencies\":{\"foo\":\"^1.0.0\"},\"devDependencies\":" +
                        "{\"foo\":\"^2.0.0\"}}";
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

        ManifestSource none = (url, name, extended) -> null;
        BunLockRegenerator.Inputs.InputsBuilder inputs = BunLockRegenerator.Inputs.builder()
          .lock(WORKSPACE_A)
          .manifests(manifests)
          .now(java.time.Instant.parse("2026-10-11T00:00:00Z"));
        org.openrewrite.javascript.internal.LockFileRegeneration.Result rewritten =
          new BunLockRegenerator(inputs.release(BunRelease.of("1.4.1")).build(), none).regenerate();
        assertThat(rewritten.getLockFileContent()).as(String.valueOf(rewritten.getErrorMessage()))
          .contains("\"version\": \"1.1.0\"");
        org.openrewrite.javascript.internal.LockFileRegeneration.Result kept =
          new BunLockRegenerator(inputs.release(BunRelease.of("1.4.0")).build(), none).regenerate();
        assertThat(kept.getLockFileContent()).as(String.valueOf(kept.getErrorMessage()))
          .contains("\"version\": \"1.0.0\"");
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
        org.openrewrite.javascript.internal.LockFileRegeneration.Result result = new BunLockRegenerator(
          BunLockRegenerator.Inputs.builder().lock(lock).manifests(manifests)
            .now(java.time.Instant.parse("2026-10-11T00:00:00Z")).release(BunRelease.of("1.4.0")).build(),
          (url, name, extended) -> null).regenerate();
        assertThat(requireNonNull(result.getFailure()).getKind())
          .isEqualTo(org.openrewrite.javascript.internal.LockFileRegeneration.Kind.PM_REJECTS);
        assertThat(result.getFailure().getDetail()).contains("which is listed in bun.lock but not on disk");

        String alone =lock.replace("\"b\": \"workspace:*\"", "\"c\": \"1.0.0\"").replace("\"packages\": {\n",
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
            String workspaces = TextLockfileWriter.saveFromBinary(lockfile, 0, options.getRelease())
              .split("\"packages\"")[0];
            assertThat(workspaces.indexOf("\"a\": \"1.0.0\"") < workspaces.indexOf("\"b\": \"1.0.0\"")).as(release)
              .isEqualTo("1.4.1".equals(release));
        }
    }

    /**
     * dependency.rs is_tarball (1.4.0, bun#38333): {@code .tar} and any case end a tarball; before, {@code .tgz} or
     * {@code .tar.gz} as written.
     */
    @Test
    void tellsATarballByItsSuffixInAnyCaseFrom140() {
        BunRelease later = BunRelease.of("1.4.0");
        BunRelease earlier = BunRelease.of("1.3.14");
        DependencyVersion.Tag tarball = DependencyVersion.Tag.TARBALL;
        assertThat(DependencyVersion.Tag.infer("./vendor/x.tar", later)).isEqualTo(tarball);
        assertThat(DependencyVersion.Tag.infer("./vendor/x.tar", earlier)).isEqualTo(DependencyVersion.Tag.FOLDER);
        assertThat(DependencyVersion.Tag.infer("file:x.tar", later)).isEqualTo(tarball);
        assertThat(DependencyVersion.Tag.infer("file:x.tar", earlier)).isEqualTo(DependencyVersion.Tag.FOLDER);
        assertThat(DependencyVersion.Tag.infer("./x.TGZ", later)).isEqualTo(tarball);
        assertThat(DependencyVersion.Tag.infer("./x.TGZ", earlier)).isEqualTo(DependencyVersion.Tag.FOLDER);
        assertThat(DependencyVersion.Tag.infer("user/repo.tar", later)).isEqualTo(tarball);
        assertThat(DependencyVersion.Tag.infer("user/repo.tar", earlier)).isEqualTo(DependencyVersion.Tag.GITHUB);
        assertThat(DependencyVersion.Tag.infer("https://github.com/u/r.tar", later)).isEqualTo(tarball);
        assertThat(DependencyVersion.Tag.infer("https://github.com/u/r.tar", earlier))
          .isEqualTo(DependencyVersion.Tag.GITHUB);
        assertThat(DependencyVersion.Tag.infer("beta.tar", later)).isEqualTo(tarball);
        assertThat(DependencyVersion.Tag.infer("beta.tar", earlier)).isEqualTo(DependencyVersion.Tag.DIST_TAG);

        DependencyVersion.TarballInfo local = requireNonNull(DependencyVersion.parse("x", null, "./vendor/x.tar", later))
          .tarball();
        assertThat(local.remote).isFalse();
        assertThat(local.uri).isEqualTo("./vendor/x.tar");
        assertThat(Resolution.fromTextLockfile("./vendor/x.tar", later).tag).isEqualTo(Resolution.Tag.LOCAL_TARBALL);
        assertThatThrownBy(() -> Resolution.fromTextLockfile("./vendor/x.tar", earlier))
          .isInstanceOf(Resolution.UnexpectedResolution.class);
    }

    /**
     * bun.lock.rs (1.4.0, bun#31339): a package whose name would leave node_modules is an invalid package; before, it
     * is read.
     */
    @Test
    void rejectsAnUnsafePackageNameFrom140() {
        String lock = lock("", MS.replace("[\"ms@2.1.3\"", "[\"../ms@2.1.3\""));
        TextLockfileReader.load(lock, options("1.3.14", null));
        assertThatThrownBy(() -> TextLockfileReader.load(lock, options("1.4.0", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Invalid package name");

        assertThat(Dependency.isSafeInstallFolderName("ms")).isTrue();
        assertThat(Dependency.isSafeInstallFolderName("@scope/ms")).isTrue();
        assertThat(Dependency.isSafeInstallFolderName("..ms")).isTrue();
        for (String unsafe : new String[]{"", "..", "../ms", "a/../b", "a//b", "/a", "a/", ".", "a:b", "a\\b", "a\0"}) {
            assertThat(Dependency.isSafeInstallFolderName(unsafe)).as(unsafe).isFalse();
        }
    }

    /**
     * Tree.rs process_subtree (1.4.0, bun#31221): a dependency whose name would leave node_modules is not hoisted;
     * before, it is.
     */
    @Test
    void skipsAnUnsafeDependencyNameFrom140() {
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n" +
                      "      \"dependencies\": {\n        \"a:b\": \"npm:ms@2.1.3\",\n      },\n    },\n  },\n" +
                      "  \"packages\": {\n    " + MS.replace("\"ms\": [", "\"a:b\": [") + "\n  }\n}\n";
        assertThat(hoisted(TextLockfileReader.load(lock, options("1.3.14", null)))).containsExactly("a:b");
        assertThat(hoisted(TextLockfileReader.load(lock, options("1.4.0", null)))).isEmpty();
    }

    /**
     * integrity.rs Integrity::parse (1.4.0, bun#33072): the strongest of several hashes, each up to a {@code ?};
     * before, the whole string is one hash, so that a packument's {@code shasum} stands in for it.
     */
    @Test
    void keepsTheStrongestOfSeveralHashesFrom140() {
        String sha1 = "sha1-" + java.util.Base64.getEncoder().encodeToString(new byte[20]);
        BunRelease later = BunRelease.of("1.4.0");
        BunRelease earlier = BunRelease.of("1.3.14");
        assertThat(Integrity.parse(INTEGRITY + " " + sha1, later)).hasToString(INTEGRITY);
        assertThat(Integrity.parse(sha1 + "\n" + INTEGRITY, later)).hasToString(INTEGRITY);
        assertThat(Integrity.parse(INTEGRITY + "?x=1", later)).hasToString(INTEGRITY);
        assertThat(Integrity.parse(INTEGRITY + " " + sha1, earlier).tag).isEqualTo(Integrity.Tag.UNKNOWN);
        assertThat(Integrity.parse(INTEGRITY + "?x=1", earlier).tag).isEqualTo(Integrity.Tag.UNKNOWN);

        String packument = "{\"name\":\"ms\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":" +
                           "\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"ms\",\"version\":\"1.0.0\",\"dist\":{" +
                           "\"integrity\":\"" + INTEGRITY + " " + sha1 + "\",\"shasum\":\"" +
                           "3cd0599b099384b815c10f7fa7df0092b62d534f\"}}}}";
        assertThat(picked(later, packument, "ms").integrity).hasToString(INTEGRITY);
        assertThat(picked(earlier, packument, "ms").integrity).hasToString("sha1-PNBZmwmThLgVwQ9/p98AkrYtU08=");
    }

    /**
     * integrity.rs parse_sha_sum (1.4.0, bun#30412): hexadecimal digits only; before, {@code g} to {@code z} read as
     * 16 to 35.
     */
    @Test
    void readsAShasumAsHexadecimalFrom140() {
        String shasum = "3cd0599b099384b815c10f7fa7df0092b62d534g";
        assertThat(Integrity.parseSHASum(shasum, BunRelease.of("1.3.14")).tag).isEqualTo(Integrity.Tag.SHA1);
        assertThatThrownBy(() -> Integrity.parseSHASum(shasum, BunRelease.of("1.4.0")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("InvalidCharacter");
        assertThat(Integrity.parseSHASum("3CD0599B099384B815C10F7FA7DF0092B62D534F", BunRelease.of("1.4.0")))
          .hasToString("sha1-PNBZmwmThLgVwQ9/p98AkrYtU08=");

        String packument = "{\"name\":\"ms\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":" +
                           "\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"ms\",\"version\":\"1.0.0\",\"dist\":{" +
                           "\"shasum\":\"" + shasum + "\"}}}}";
        assertThat(picked(BunRelease.of("1.4.0"), packument, "ms").integrity.tag).isEqualTo(Integrity.Tag.UNKNOWN);
        assertThat(picked(BunRelease.of("1.3.14"), packument, "ms").integrity.tag).isEqualTo(Integrity.Tag.SHA1);
    }

    /**
     * resolver_hooks.rs Repository::order (1.4.0, bun#36578): two resolutions of one repo and committish order by
     * their resolved tag; before, they are equal.
     */
    @Test
    void ordersRepositoriesByTheirResolvedTagFrom140() {
        Repository first = new Repository("o", "r", "main", "o-r-1111111", "");
        Repository second = new Repository("o", "r", "main", "o-r-2222222", "");
        assertThat(first.order(second, BunRelease.of("1.4.0"))).isNegative();
        assertThat(first.order(second, BunRelease.of("1.3.14"))).isZero();
        Resolution l = Resolution.repository(Resolution.Tag.GITHUB, second);
        Resolution r = Resolution.repository(Resolution.Tag.GITHUB, first);
        assertThat(l.order(r, BunRelease.of("1.4.0"))).isPositive();
        assertThat(l.order(r, BunRelease.of("1.3.14"))).isZero();
    }

    /**
     * repository.rs parse_append_github (1.4.0, bun#36165): the owner splits off at the last slash before the
     * {@code #}; before, at the last slash, which leaves a repo without an owner its first character dropped, and one
     * after the {@code #} has bun slice the repo from past its end.
     */
    @Test
    void splitsAGitHubResolutionAtTheSlashBeforeItsHashFrom140() {
        BunRelease later = BunRelease.of("1.4.0");
        BunRelease earlier = BunRelease.of("1.3.14");
        Repository branch = Repository.parseAppendGithub("github:o/r#feat/x", later);
        assertThat(branch.owner).isEqualTo("o");
        assertThat(branch.repo).isEqualTo("r");
        assertThat(branch.committish).isEqualTo("feat/x");
        assertThatThrownBy(() -> Resolution.fromTextLockfile("github:o/r#feat/x", earlier))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("slices from past its end");
        Repository ownerless = Repository.parseAppendGithub("github:repo", later);
        assertThat(ownerless.owner).isEmpty();
        assertThat(ownerless.repo).isEqualTo("repo");
        assertThat(Repository.parseAppendGithub("github:repo", earlier).repo).isEqualTo("epo");
        Repository plain = Repository.parseAppendGithub("github:o/r#v1", later);
        assertThat(plain.owner + " " + plain.repo + " " + plain.committish).isEqualTo("o r v1");
        Repository plainBefore = Repository.parseAppendGithub("github:o/r#v1", earlier);
        assertThat(plainBefore.owner + " " + plainBefore.repo + " " + plainBefore.committish).isEqualTo("o r v1");
    }

    /**
     * dependency.rs clone_with_different_buffers (1.4.1, bun#41302): a workspace dependency keeps its path; before,
     * the clone reads its literal as the path.
     */
    @Test
    void keepsAWorkspaceDependencysPathWhenClonedFrom141() {
        DependencyVersion linked = requireNonNull(DependencyVersion.parse("a", null, "^1.0.0")).asWorkspace("packages/a");
        Dependency dependency = new Dependency("a", linked, Behavior.of(Behavior.PROD));
        DependencyVersion later = dependency.cloneDependency(BunRelease.of("1.4.1")).version;
        assertThat(later.tag).isEqualTo(DependencyVersion.Tag.WORKSPACE);
        assertThat(later.literal).isEqualTo("^1.0.0");
        assertThat(later.string()).isEqualTo("packages/a");
        assertThat(dependency.cloneDependency(BunRelease.of("1.4.0")).version.string()).isEqualTo("^1.0.0");
    }

    /**
     * npm.rs PackageManifest::parse (1.4.3, bun#43035 with bun#43115): a versions key that is no version is skipped
     * without an error; before, with {@code Failed to parse dependency <key>}.
     */
    @Test
    void skipsAVersionKeyThatIsNoVersionSilentlyFrom143() {
        String packument = "{\"name\":\"ms\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":" +
                           "\"1.0.0\"},\"versions\":{\"next\":{\"name\":\"ms\"},\"1.0.0\":{\"name\":\"ms\"," +
                           "\"version\":\"1.0.0\"}}}";
        Log later = new Log();
        PackageManifest manifest = requireNonNull(PackageManifest.parse(BunRelease.of("1.4.3"), later, packument, "ms",
          false));
        assertThat(later.errors).isEmpty();
        assertThat(manifest.releaseVersions).hasSize(1);
        Log earlier = new Log();
        PackageManifest.parse(BunRelease.of("1.4.2"), earlier, packument, "ms", false);
        assertThat(earlier.errors).containsExactly("Failed to parse dependency next");
    }

    /**
     * semver lib.rs Builder (1.4.0 only; bun#40406 from 1.4.1): an 8-byte packument string whose last byte is not
     * ASCII is kept as an offset into the packument, which reads whatever the buffer holds there.
     */
    @Test
    void misplacesAnEightByteNonAsciiManifestStringIn140() {
        String packument = "{\"name\":\"p\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":" +
                           "\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"p\",\"version\":\"1.0.0\"," +
                           "\"peerDependencies\":{\"quux\":\"1.0.0-é\"}}}}";
        assertThatThrownBy(() -> PackageManifest.parse(BunRelease.of("1.4.0"), new Log(), packument, "p", false))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("\"1.0.0-é\"");
        assertThat(picked(BunRelease.of("1.4.1"), packument, "p").peerDependencies).containsExactly("quux", "1.0.0-é");
        assertThat(picked(BunRelease.of("1.3.14"), packument, "p").peerDependencies)
          .containsExactly("quux", "1.0.0-é");
    }

    /**
     * npm.rs PackageManifest::parse (1.4.0 until 1.4.3, bun#43145): beside an empty bin the counting pass leaves a
     * directories.bin out, which panics the build pass where the buffer cannot hold it; within the buffer's least
     * slack it fits.
     */
    @Test
    void leavesAnEmptyBinsDirectoryUncountedFrom140Until143() {
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longPath.append("./");
        }
        String uncounted = binPackument(longPath + "bin");
        assertThatThrownBy(() -> PackageManifest.parse(BunRelease.of("1.4.2"), new Log(), uncounted, "p", false))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("directories.bin of p@1.0.0");
        assertThatThrownBy(() -> PackageManifest.parse(BunRelease.of("1.4.0"), new Log(), uncounted, "p", false))
          .isInstanceOf(CannotKnow.class);
        assertThat(picked(BunRelease.of("1.4.3"), uncounted, "p").bin.tag).isEqualTo(Bin.Tag.DIR);
        assertThat(picked(BunRelease.of("1.3.14"), uncounted, "p").bin.tag).isEqualTo(Bin.Tag.DIR);
        assertThat(picked(BunRelease.of("1.4.2"), binPackument("./node_modules/.bin"), "p").bin.value)
          .containsExactly("./node_modules/.bin");
    }

    private static String binPackument(String directory) {
        return "{\"name\":\"p\",\"modified\":\"2026-01-01T00:00:00.000Z\",\"dist-tags\":{\"latest\":\"1.0.0\"}," +
               "\"versions\":{\"1.0.0\":{\"name\":\"p\",\"version\":\"1.0.0\",\"bin\":\"\",\"directories\":{" +
               "\"bin\":\"" + directory + "\"},\"dist\":{\"tarball\":\"https://registry.npmjs.org/p/-/p-1.0.0.tgz\"}}}}";
    }

    /**
     * SemverQuery.rs parse (1.4.0, bun#31234): a chunk without a comparator after the first is skipped; before, bun
     * reaches code it takes for unreachable.
     */
    @Test
    void skipsARangeChunkWithoutAComparatorFrom140() {
        BunRelease later = BunRelease.of("1.4.0");
        SemverQuery.Group dangling = SemverQuery.parse("1 || - foo", later);
        assertThat(dangling.satisfies(SemverVersion.of(1, 2, 3))).isTrue();
        assertThat(dangling.satisfies(SemverVersion.of(2, 0, 0))).isFalse();
        assertThat(SemverQuery.parse("1 || - 2", later).satisfies(SemverVersion.of(2, 5, 0))).isFalse();
        assertThatThrownBy(() -> SemverQuery.parse("1 || - foo", BunRelease.of("1.3.14")))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("takes for unreachable");
        assertThat(SemverQuery.parse("--canary.24 1.2.3", BunRelease.of("1.3.14")).satisfies(SemverVersion.of(1, 2, 3)))
          .isTrue();
    }

    /**
     * Version.rs parse (1.4.0, bun#33072): separators alone are no version; before, one starting with a space is
     * 0.0.0. A bun.lock's workspace {@code "version": " "} is an invalid semver version from 1.4.0.
     */
    @Test
    void rejectsAVersionOfSeparatorsAloneFrom140() {
        assertThat(SemverVersion.parse(" ", BunRelease.of("1.4.0")).valid).isFalse();
        SemverVersion.ParseResult earlier = SemverVersion.parse(" ", BunRelease.of("1.3.14"));
        assertThat(earlier.valid).isTrue();
        assertThat(earlier.version.min()).hasToString("0.0.0");
        assertThat(SemverVersion.parse("v =", BunRelease.of("1.4.0")).len).isEqualTo(3);
        assertThat(SemverVersion.parse("v =", BunRelease.of("1.3.14")).len).isZero();

        String lock = "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"p\",\n" +
                      "    },\n    \"packages/a\": {\n      \"name\": \"a\",\n      \"version\": \" \",\n    },\n" +
                      "  },\n  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n  }\n}\n";
        TextLockfileReader.load(lock, options("1.3.14", null));
        assertThatThrownBy(() -> TextLockfileReader.load(lock, options("1.4.0", null)))
          .isInstanceOf(TextLockfileReader.ParseError.class)
          .hasMessage("Invalid semver version");
    }

    /**
     * Version.rs parse_version_number (1.4.0, bun#34521): a number of more than 20 digits is its value; before,
     * missing, so that {@code ^} and 21 nines take any version.
     */
    @Test
    void readsALongVersionNumberByItsValueFrom140() {
        StringBuilder nines = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            nines.append('9');
        }
        String caret = "^" + nines;
        assertThat(SemverQuery.parse(caret, BunRelease.of("1.4.0")).satisfies(SemverVersion.of(1, 0, 0))).isFalse();
        assertThat(SemverQuery.parse(caret, BunRelease.of("1.3.14")).satisfies(SemverVersion.of(1, 0, 0))).isTrue();
        String padded = "1.0.0000000000000000000005";
        assertThat(SemverVersion.parse(padded, BunRelease.of("1.4.0")).version.patch).isEqualTo(5L);
        assertThat(SemverVersion.parse(padded, BunRelease.of("1.3.14")).version.patch).isNull();
    }

    /**
     * SemverQuery.rs and SemverRange.rs (1.4.0, bun#34600): an upper bound past the u64 maximum ends at {@code <=} it;
     * before, the range is empty.
     */
    @Test
    void boundsARangeAtTheCeilingInclusivelyFrom140() {
        String max = "18446744073709551615";
        BunRelease later = BunRelease.of("1.4.0");
        BunRelease earlier = BunRelease.of("1.3.14");
        SemverVersion top = SemverVersion.of(SemverVersion.MAX, 0, 0);
        for (String range : new String[]{"^" + max, "~" + max, max, max + ".x", "1.0.0 - " + max}) {
            assertThat(SemverQuery.parse(range, later).satisfies(top)).as(range).isTrue();
            assertThat(SemverQuery.parse(range, earlier).satisfies(top)).as(range).isFalse();
        }
        SemverVersion minor = SemverVersion.of(1, SemverVersion.MAX, 3);
        assertThat(SemverQuery.parse("~1." + max + ".3", later).satisfies(minor)).isTrue();
        assertThat(SemverQuery.parse("~1." + max + ".3", earlier).satisfies(minor)).isFalse();
        SemverVersion patch = SemverVersion.of(0, 0, SemverVersion.MAX);
        assertThat(SemverQuery.parse("^0.0." + max, later).satisfies(patch)).isTrue();
        assertThat(SemverQuery.parse("^0.0." + max, earlier).satisfies(patch)).isFalse();
    }

    /**
     * semver.zig Version.parse (1.2.2): a wildcard right after the patch number is skipped; before, the parser reaches
     * code it takes for unreachable. The darwin-arm64 builds of 1.2.0 and 1.2.1 skip it as 1.2.2 does
     * ({@code Bun.semver.satisfies("1.2.4", "^1.2.3x")} is true), which another target's build need not.
     */
    @Test
    void skipsAWildcardAfterThePatchNumberFrom122() {
        SemverVersion.ParseResult later = SemverVersion.parse("1.2.3x", BunRelease.of("1.2.2"));
        assertThat(later.valid).isTrue();
        assertThat(later.wildcard).isEqualTo(SemverQuery.Wildcard.NONE);
        assertThat(later.version.min()).hasToString("1.2.3");
        assertThat(SemverQuery.parse("^1.2.3x", BunRelease.of("1.2.2")).satisfies(SemverVersion.of(1, 2, 4))).isTrue();
        assertThatThrownBy(() -> SemverQuery.parse("^1.2.3*", BunRelease.of("1.2.1")))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("takes for unreachable");
    }

    /**
     * Version.zig {@code VersionType(u64)} (1.2.23): a version's numbers are u64s; before, u32s, as
     * {@code Bun.semver} of 1.2.22 and 1.2.23 answer: {@code satisfies("1.0.0", "1.0.4294967296")} is true before and
     * false after, as is {@code satisfies("1.0.0", "1.0.12345678901")}; {@code satisfies("4294967295.0.0",
     * "^4294967295")} is false before and true after; {@code order("1.0.0-alpha.10000000000",
     * "1.0.0-alpha.5000000000")} is -1 before and 1 after.
     */
    @Test
    void readsSixtyFourBitVersionNumbersFrom1223() {
        BunRelease later = BunRelease.of("1.2.23");
        BunRelease earlier = BunRelease.of("1.2.22");
        assertThat(SemverVersion.parse("1.0.4294967296", later).version.patch).isEqualTo(4294967296L);
        assertThat(SemverVersion.parse("1.0.4294967296", earlier).version.patch).isZero();
        assertThat(SemverVersion.parse("1.0.12345678901", later).version.patch).isEqualTo(12345678901L);
        assertThat(SemverVersion.parse("1.0.12345678901", earlier).version.patch).isNull();
        assertThat(SemverVersion.parse("1.0.1709012345678", earlier).version.min()).hasToString("1.0.0");

        SemverVersion one = SemverVersion.of(1, 0, 0);
        assertThat(SemverQuery.parse("1.0.4294967296", later).satisfies(one)).isFalse();
        assertThat(SemverQuery.parse("1.0.4294967296", earlier).satisfies(one)).isTrue();
        assertThat(SemverQuery.parse("1.0.12345678901", later).satisfies(one)).isFalse();
        assertThat(SemverQuery.parse("1.0.12345678901", earlier).satisfies(one)).isTrue();

        SemverVersion top = SemverVersion.of(SemverVersion.MAX_U32, 0, 0);
        assertThat(SemverQuery.parse("^4294967295", later).satisfies(top)).isTrue();
        assertThat(SemverQuery.parse("^4294967295", earlier).satisfies(top)).isFalse();
        assertThat(SemverQuery.parse("<=1", earlier).head.head.range.left.version)
          .hasToString("1.4294967295.4294967295");

        SemverVersion big = SemverVersion.parse("1.0.0-alpha.10000000000", later).version.min();
        SemverVersion small = SemverVersion.parse("1.0.0-alpha.5000000000", later).version.min();
        assertThat(big.order(small, later)).isPositive();
        assertThat(big.order(small, earlier)).isNegative();
        assertThat(SemverQuery.parse(">1.0.0-alpha.5000000000", later).satisfies(big)).isTrue();
        assertThat(SemverQuery.parse(">1.0.0-alpha.5000000000", earlier).satisfies(big)).isFalse();
    }

    private static PackageManifest.PackageVersion picked(BunRelease release, String packument, String name) {
        PackageManifest manifest = requireNonNull(PackageManifest.parse(release, new Log(), packument, name, false));
        return requireNonNull(manifest.findByDistTag("latest")).packageVersion;
    }

    // the names of the dependencies the hoisting placed
    private static List<String> hoisted(Lockfile lockfile) {
        List<String> names = new java.util.ArrayList<>();
        for (int id : lockfile.hoistedDependencies) {
            names.add(lockfile.dependencies.get(id).name);
        }
        return names;
    }

    private static java.util.Set<String> workspaces(String release, String globs, String... locations) {
        java.util.Map<String, String> manifests = new java.util.LinkedHashMap<>();
        for (String location : locations) {
            manifests.put(location, "{\"name\":\"" + location.replace('/', '-') + "\"}");
        }
        Workspaces names = new Workspaces(BunRelease.of(release));
        names.processNamesArray(new Log(), Json.parse(globs).items(), manifests);
        return names.map.keySet();
    }

    /**
     * matcher.rs match (1.4.0, bun#33759): a {@code **} right after the {@code !} is a globstar, so that a later
     * {@code !**}{@code /test} takes the directories named test below the top out of the workspaces.
     */
    @Test
    void readsAGlobstarAfterANegationFrom140() {
        String globs = "[\"packages/**\", \"!**/test\"]";
        assertThat(workspaces("1.4.0", globs, "packages/a", "packages/a/test")).containsOnly("packages/a");
        assertThat(workspaces("1.3.14", globs, "packages/a", "packages/a/test"))
          .containsOnly("packages/a", "packages/a/test");
    }

    /**
     * lib.rs detect_glob_syntax (1.4.0, bun#37052): the backslashes are counted right before each token, so that
     * {@code pkgs/\*x\*} is a path; before, a glob.
     */
    @Test
    void countsTheBackslashesBeforeEachGlobTokenFrom140() {
        assertThat(Glob.detectGlobSyntax("pkgs/\\*x\\*", BunRelease.of("1.4.0"))).isFalse();
        assertThat(Glob.detectGlobSyntax("pkgs/\\*x\\*", BunRelease.of("1.3.14"))).isTrue();
        assertThat(Glob.detectGlobSyntax("pkgs/\\*x*", BunRelease.of("1.4.0"))).isTrue();
        assertThat(Glob.detectGlobSyntax("packages\\*", BunRelease.of("1.3.14"))).isFalse();
    }

    /**
     * GlobWalker.rs match_pattern_dir and match_pattern_impl (1.4.0, bun#32853): a hidden directory the pattern names
     * is walked into; wildcards still pass hidden ones over. Before, every hidden one is passed over.
     */
    @Test
    void walksIntoAHiddenDirectoryThePatternNamesFrom140() {
        String[] locations = {".github/actions/a", "packages/.cache/b", "packages/c", ".h/d"};
        assertThat(workspaces("1.4.0", "[\".github/actions/*\", \"packages/**\", \"*/d\", \".*/d\"]", locations))
          .containsOnly(".github/actions/a", "packages/c", ".h/d");
        assertThat(workspaces("1.3.14", "[\".github/actions/*\", \"packages/**\", \"*/d\", \".*/d\"]", locations))
          .containsOnly("packages/c");
    }

    /**
     * GlobWalker.zig matchPatternSlow's matcher: src/glob/ascii.zig before 1.2.3, src/glob/match.zig from 1.2.3
     * (bun#16824), whose skipBranch goes to its own group's close from 1.2.4. Each row is what
     * {@code new Bun.Glob(pattern).match(path)}, which calls the same matcher, answers in 1.2.2, 1.2.3 and 1.2.4.
     */
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
      "{a,ab}b|ab|false|true|true",
      "{a,ab}b|abb|true|true|true",
      "{ab,a}b|ab|false|true|true",
      "{a,{b,c}x}|a|true|false|true",
      "{a,{b,c}x}|ax|false|true|false",
      "{a,{b,c}x}|bx|false|true|true",
      "{a,{b,c}x}|b|true|false|false",
      "![ab|a|false|true|true",
      "[ab|a|false|false|false",
      "a}|a}|false|true|true",
      "'a,b'|'a,b'|false|true|true",
      "pkg-*|pkg-a|true|true|true",
      "{pkg,lib}-*|lib-x|true|true|true",
      "*{a,b}|xb|true|true|true",
      "{*a,b}c|xac|true|true|true",
      "!{a,b}|c|true|true|true",
      "!{a,b}|a|false|false|false",
      "{a,b|a|false|true|true",
      "{a,b}}|a}|false|true|true",
    })
    void triesEveryBraceBranchFrom123AndSkipsToTheBranchsOwnCloseFrom124(String pattern, String path, boolean in122,
                                                                         boolean in123, boolean in124) {
        assertThat(Glob.match(pattern, path, BunRelease.of("1.2.2")).matches()).as("1.2.2").isEqualTo(in122);
        assertThat(Glob.match(pattern, path, BunRelease.of("1.2.3")).matches()).as("1.2.3").isEqualTo(in123);
        assertThat(Glob.match(pattern, path, BunRelease.of("1.2.4")).matches()).as("1.2.4").isEqualTo(in124);
    }

    /**
     * As 1.2.2 and 1.2.3 read {@code "workspaces": ["packages/{a,ab}b"]} beside packages/ab and packages/abb.
     */
    @Test
    void matchesAWorkspacesGlobsBranchesAsTheReleaseDoes() {
        String[] locations = {"packages/ab", "packages/abb"};
        assertThat(workspaces("1.2.3", "[\"packages/{a,ab}b\"]", locations)).containsOnly("packages/ab", "packages/abb");
        assertThat(workspaces("1.2.2", "[\"packages/{a,ab}b\"]", locations)).containsOnly("packages/abb");
    }

    // the error the release's JSON parser fails a package.json or bun.lock with, as "name: first message"; "reads past"
    // the error 1.3.x only logs where its parse returns what it read
    private static String jsonFailure(String release, String json) {
        try {
            Json.parse(json, BunRelease.of(release));
        } catch (Json.SyntaxError e) {
            return e.error + ": " + e.getMessage();
        } catch (CannotKnow e) {
            return "reads past " + e.getMessage().replaceFirst(".*JSON it reads past its own error \\((.*)\\)$", "$1");
        }
        return "parses";
    }

    /**
     * json.rs, json_index.rs and json_stage2.rs (1.4.0, bun#33032): json.rs's {@code error_messages} table, with
     * package.json's options.
     */
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', quoteCharacter = '`', value = {
      "`   `|ParserError: Unexpected end of file",
      "{|ParserError: Expected \"}\" but found end of file",
      "[1|ParserError: Expected \"]\" but found end of file",
      "\"abc|SyntaxError: Unterminated string literal",
      "\"ab\\ncd\"|SyntaxError: Unterminated string literal",
      "\"ab\\tcd\"|SyntaxError: Syntax Error",
      "{\"a\" 1}|ParserError: Expected \":\" but found \"1\"",
      "{1: 2}|ParserError: Expected string but found \"1\"",
      "[truthy]|ParserError: Unexpected truthy",
      "[truex]|ParserError: Unexpected truex",
      "nul|ParserError: Unexpected nul",
      "[1n]|SyntaxError: Syntax Error",
      "[123abc]|SyntaxError: Syntax Error",
      "[@]|SyntaxError: Unsupported syntax: Decorators are not allowed in JSON",
      "[;]|SyntaxError: Unsupported syntax: Semicolons are not allowed in JSON",
      "[1 + 2]|SyntaxError: Unsupported syntax: Operators are not allowed in JSON",
      "[\"a\\q\"]|SyntaxError: Syntax Error",
      "[\"\\u12\"]|SyntaxError: Syntax Error",
      "{\"a\": }|ParserError: Unexpected }",
      "{\"a\": 1/2}|SyntaxError: Unsupported syntax: Operators are not allowed in JSON",
      "[1] /* unterminated|SyntaxError: Expected \"*/\" to terminate multi-line comment",
      "[@] /* unterminated|SyntaxError: Unsupported syntax: Decorators are not allowed in JSON",
      "[1 /* never|SyntaxError: Expected \"*/\" to terminate multi-line comment",
      "[@] 1/2|SyntaxError: Unsupported syntax: Decorators are not allowed in JSON",
      "/ [1]|SyntaxError: Unsupported syntax: Operators are not allowed in JSON",
      "{\"v\": 1.0.0}|ParserError: Unexpected 1.0.0",
      "[a+1]|ParserError: Unexpected a+1",
      "{\"a\":1 \"b\":2}|ParserError: Expected \",\" but found \"\"b\"\"",
      "{\"a\":1,|ParserError: Expected string but found end of file",
      "{\\n<<<<<<< HEAD\\n|ParserError: Expected string but found \"<<<<<<<\"",
      "[\\x]|SyntaxError: Syntax Error",
      "{\"a\": [\"x\" \"y\"]}|ParserError: Expected \",\" but found \"\"y\"\"",
    })
    void readsJsonOverAStructuralIndexFrom140(String json, String failure) {
        assertThat(jsonFailure("1.4.0", unescape(json))).isEqualTo(failure);
    }

    /**
     * json.zig over js_lexer.zig's JSON mode (1.3.10): a token's text is the lexer's, a missing colon
     * {@code Expected " ="}, and the lexer reads one token past the root.
     */
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', quoteCharacter = '`', value = {
      "{|ParserError: Expected string but found end of file",
      "[|ParserError: Unexpected end of file",
      "[1|ParserError: Expected \",\" but found end of file",
      "{\"a\":1|ParserError: Expected \",\" but found end of file",
      "{\"a\"|ParserError: Expected \" =\" but found end of file",
      "{\"a\" 1}|ParserError: Expected \" =\" but found \"1\"",
      "{1:2}|reads past Expected string but found \"1\"",
      "{\"v\": 1.0.0}|reads past Expected \",\" but found \".0\"",
      "{\"a\": [\"x\" \"y\"]}|reads past Expected \",\" but found \"\"y\"\"",
      "[a+1]|ParserError: Unexpected a",
      "[1n]|ParserError: Unexpected 1n",
      "[\\x]|SyntaxError: Unexpected escape sequence",
      "[truthy]|ParserError: Unexpected truthy",
      "\"abc|SyntaxError: Unterminated string literal",
      "\"ab\\tcd\"|SyntaxError: Syntax Error",
      "{\\n<<<<<<< HEAD\\n|SyntaxError: Unsupported syntax: Operators are not allowed in JSON",
      "[1] /* unterminated|SyntaxError: Expected \"*/\" to terminate multi-line comment",
      "{} ;|SyntaxError: Unsupported syntax: Semicolons are not allowed in JSON",
      "{} 1x|SyntaxError: Syntax Error",
      "{} \"abc|SyntaxError: Unterminated string literal",
      "{} /|parses",
      "{} x y z ;|parses",
    })
    void readsJsonThroughTheJavaScriptLexerBefore140(String json, String failure) {
        assertThat(jsonFailure("1.3.14", unescape(json))).isEqualTo(failure);
    }

    @Test
    void looksPastTheRootOnlyForASlashFrom140() {
        assertThat(jsonFailure("1.4.0", "{} ;")).isEqualTo("parses");
        assertThat(jsonFailure("1.4.0", "{} 1x \"abc")).isEqualTo("parses");
        assertThat(jsonFailure("1.4.0", "{} /")).isEqualTo("SyntaxError: Unsupported syntax: Operators are not allowed in JSON");
        // the indexer reads on in 8 KiB chunks: a slash past the chunk the root ends in is never seen
        String padded = "{\"a\": \"" + repeat('x', 9000) + "\"}";
        assertThat(jsonFailure("1.4.0", padded + " /")).isEqualTo("SyntaxError: Unsupported syntax: Operators are not allowed in JSON");
        String far = "{}" + repeat(' ', 9000) + "/";
        assertThat(jsonFailure("1.4.0", far)).isEqualTo("parses");
    }

    @Test
    void readsPastAnErrorItOnlyLogsBefore140() {
        // the array is ["x"] and the install goes on with it, which the engine does not reproduce
        assertThatThrownBy(() -> Json.parse("{\"a\": [\"x\" \"y\"]}", BunRelease.of("1.3.14")))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("Expected \",\" but found \"\"y\"\"");
    }

    @Test
    void readsTheSameValuesEitherWay() {
        String json = "\uFEFF{\"a\": [1, -2.5e3, 0x10, 017, .5, true, null, \"\\u00e9\\n\\uD83D\\uDE00\", 'q'], // c\n" +
                      "  /* d */ \"b\": {\"c\": \"\u00a0x\",},}";
        for (String release : new String[]{"1.3.14", "1.4.0"}) {
            Json.Expr root = Json.parse(json, BunRelease.of(release));
            List<Json.Expr> a = requireNonNull(root.get("a")).items();
            assertThat(a).hasSize(9);
            assertThat(a.get(0).asNumber()).isEqualTo(1.0);
            assertThat(a.get(1).asNumber()).isEqualTo(-2500.0);
            assertThat(a.get(2).asNumber()).isEqualTo(16.0);
            assertThat(a.get(3).asNumber()).isEqualTo(15.0);
            assertThat(a.get(4).asNumber()).isEqualTo(0.5);
            assertThat(a.get(5).asBool()).isTrue();
            assertThat(a.get(6).kind).isEqualTo(Json.Kind.NULL);
            assertThat(a.get(7).asString()).isEqualTo("\u00e9\n\uD83D\uDE00");
            assertThat(a.get(8).asString()).isEqualTo("q");
            assertThat(requireNonNull(requireNonNull(root.get("b")).get("c")).asString()).isEqualTo("\u00a0x");
        }
    }

    private static String json(String release, String text, boolean quote) {
        StringBuilder out = new StringBuilder();
        Strings.appendJsonString(out, text, quote, BunRelease.of(release));
        return out.toString();
    }

    /**
     * fmt.rs (1.4.0, bun#30412): a quoted string is escaped by {@code encode_json_string_chars}, lowercase and nothing
     * above the control characters; until 1.4.1 (bun#40718) quotes it by the printer again. The printer quoting for
     * JSON writes BEL and VT as <code>&#92;u0007</code> and <code>&#92;u000B</code> from 1.4.0 (bun#36746).
     */
    @Test
    void escapesQuotedStringsMinimallyIn140AndBelAndVtAsUnicodeFrom140() {
        String text = "a\u0007b\u000Bc\u001F ﻿\"\\";
        assertThat(json("1.3.14", text, true)).isEqualTo("\"a\\x07b\\vc\\u001F\\u2028\\uFEFF\\\"\\\\\"");
        assertThat(json("1.4.0", text, true)).isEqualTo("\"a\\u0007b\\u000bc\\u001f ﻿\\\"\\\\\"");
        assertThat(json("1.4.0", text, false)).isEqualTo("a\\u0007b\\u000Bc\\u001F\\u2028\\uFEFF\\\"\\\\");
        assertThat(json("1.4.1", text, true)).isEqualTo("\"a\\u0007b\\u000Bc\\u001F\\u2028\\uFEFF\\\"\\\\\"");
    }

    /**
     * bun.lock.rs (1.4.0, bun#31417): an npm package's registry url and a git package's resolution are JSON-escaped;
     * before, written as they are.
     */
    @Test
    void escapesTheRegistryAndTheGitResolutionFrom140() {
        String npm = "\"ms\": [\"ms@2.1.3\", \"https://r.example/a\\u2028b/ms/-/ms-2.1.3.tgz\", {}, \"" + INTEGRITY +
                     "\"],";
        String later = writtenBack("1.4.0", lock("", npm));
        assertThat(later).contains("\"https://r.example/a\\u2028b/ms/-/ms-2.1.3.tgz\"");
        String earlier = writtenBack("1.3.14", lock("", npm));
        assertThat(earlier).contains("\"https://r.example/a b/ms/-/ms-2.1.3.tgz\"");
    }

    private static java.util.Map<String, String> dotEnv(String release, String env) {
        java.util.Map<String, String> process = new java.util.HashMap<>();
        process.put("B", "x");
        return DotEnv.load(BunRelease.of(release), HostEnvironment.of(process), Collections.singletonMap(".env", env))
          .known();
    }

    /**
     * env_loader.rs parse_quoted (1.4.0, bun#34003): the first closing quote ends a value, the rest of its line
     * dropped; before, a closing quote followed by more of its line is passed over.
     */
    @Test
    void endsAQuotedEnvValueAtItsFirstClosingQuoteFrom140() {
        String env = "X='it's'\nY=\"x\" junk\nZ=z\n";
        assertThat(dotEnv("1.4.0", env)).containsEntry("X", "it").containsEntry("Y", "x").containsEntry("Z", "z");
        assertThat(dotEnv("1.3.14", env)).containsEntry("X", "it's").containsEntry("Y", "\"x\" junk")
          .containsEntry("Z", "z");
    }

    /**
     * env_loader.rs WHITESPACE_CHARS (1.4.0, bun#34001): 0xA0, the last byte of a UTF-8 no-break space, is not
     * trimmed.
     */
    @Test
    void keepsANoBreakSpaceAtTheEndOfAnEnvValueFrom140() {
        String env = "A= x\nK=x \n C=x\n";
        assertThat(dotEnv("1.4.0", env)).containsEntry("A", " x").containsEntry("K", "x ")
          .doesNotContainKey("C");
        assertThat(dotEnv("1.3.14", env)).containsEntry("K", "x�");
    }

    /**
     * env_loader.rs parse_bytes (1.4.0, bun#34002): a leading byte order mark is dropped; before, the first line is.
     */
    @Test
    void stripsAnEnvFilesByteOrderMarkFrom140() {
        String env = "﻿A=1\nZ=2\n";
        assertThat(dotEnv("1.4.0", env)).containsEntry("A", "1").containsEntry("Z", "2");
        assertThat(dotEnv("1.3.14", env)).doesNotContainKey("A").containsEntry("Z", "2");
    }

    /**
     * env_loader.rs expand_into (1.4.0, bun#36199): left to right, a lone {@code $} itself and {@code \$} a
     * {@code $}; before, right to left (B is set to x).
     */
    @Test
    void expandsEnvValuesLeftToRightFrom140() {
        String env = "P=pa$$w0rd\nQ=a$-b\nR=${H\nS=a\\$\nT=${A:-$B}\nU=$B-${B}\n";
        assertThat(dotEnv("1.4.0", env)).containsEntry("P", "pa$").containsEntry("Q", "a$-b")
          .containsEntry("R", "${H").containsEntry("S", "a$").containsEntry("T", "x").containsEntry("U", "x-x");
        assertThat(dotEnv("1.3.14", env.replace("T=${A:-$B}\n", ""))).containsEntry("P", "pa")
          .containsEntry("Q", "a-b").containsEntry("R", "").containsEntry("S", "a\\$").containsEntry("U", "x-x");
    }

    /**
     * ini/lib.rs prepare_str over json.rs (1.4.0, bun#33032): a quoted value is strict JSON, and one whose parse
     * logged an error, a trailing comma or a comment, stays its text; 1.3.x took what the parse returned.
     */
    @Test
    void keepsAQuotedNpmrcValueWithAJsonErrorAsTextFrom140() {
        HostEnvironment env = HostEnvironment.of(Collections.<String, String>emptyMap());
        String npmrc = "omit='[\"dev\",]'\nc='[\"a\"] // c'\nn='[1, 0x10]'\n";
        assertThat(Ini.parse(npmrc, env, BunRelease.of("1.4.0")))
          .containsEntry("omit", "[\"dev\",]").containsEntry("c", "[\"a\"] // c")
          .containsEntry("n", java.util.Arrays.asList(1L, 16L));
        assertThat(Ini.parse(npmrc, env, BunRelease.of("1.3.14")))
          .containsEntry("omit", Collections.singletonList("dev")).containsEntry("c", Collections.singletonList("a"));
        assertThat(BunOptions.load(BunRelease.of("1.4.0"), null, null, npmrc, env).isLocalDevDependencies()).isTrue();
        assertThat(BunOptions.load(BunRelease.of("1.3.14"), null, null, npmrc, env).isLocalDevDependencies()).isFalse();
    }

    /**
     * ini/lib.rs prepare_str (1.4.0, bun#32712): {@code ''} is the empty string; before, JSON's empty document, an
     * empty object, so that it leaves the user's registry in place.
     */
    @Test
    void readsAnEmptyQuotedNpmrcValueAsAStringFrom140() {
        HostEnvironment env = HostEnvironment.of(Collections.<String, String>emptyMap());
        assertThat(Ini.parse("registry=''\n", env, BunRelease.of("1.4.0"))).containsEntry("registry", "");
        assertThat(Ini.parse("registry=''\n", env, BunRelease.of("1.3.14")).get("registry"))
          .isEqualTo(Collections.emptyMap());
        String user = "registry=https://u.example/\n";
        assertThat(BunOptions.load(BunRelease.of("1.3.14"), null, user, "registry=''\n", env).getRegistry())
          .isEqualTo("https://u.example/");
    }

    // what the release's TOML parser fails a bunfig.toml with, "reads past" what 1.3.x only logs, else the value at the
    // dotted path
    private static String toml(String release, String bunfig, String path) {
        Json.Expr expr;
        try {
            expr = Toml.parse(bunfig, BunRelease.of(release));
        } catch (Toml.SyntaxError e) {
            return e.error + ": " + e.getMessage();
        } catch (CannotKnow e) {
            return "reads past " + e.getMessage().replaceFirst(".*reads past its own error \\((.*)\\)$", "$1");
        }
        for (String key : path.split("\\.")) {
            expr = expr == null ? null : expr.get(key);
        }
        if (expr == null) {
            return "absent";
        }
        return expr.isString() ? "\"" + expr.asString() + "\"" : expr.kind == Json.Kind.NUMBER ?
          String.valueOf(expr.asNumber()) : String.valueOf(expr.asBool());
    }

    /**
     * toml.rs (1.4.0, bun#32953): strict TOML 1.1, every error fatal and redacted, beside 1.3.10's lexer.
     */
    @Test
    void parsesBunfigAsStrictTomlFrom140() {
        assertThat(toml("1.4.0", "[install]\nlinker = isolated\n", "install.linker"))
          .isEqualTo("SyntaxError: Strings must be quoted");
        assertThat(toml("1.3.14", "[install]\nlinker = isolated\n", "install.linker")).isEqualTo("\"isolated\"");

        assertThat(toml("1.4.0", "[install]\nregistry = \"a\"\nregistry = \"b\"\n", "install.registry"))
          .isEqualTo("SyntaxError: Cannot redefine key");
        assertThat(toml("1.3.14", "[install]\nregistry = \"a\"\nregistry = \"b\"\n", "install.registry"))
          .isEqualTo("SyntaxError: Cannot redefine key 'registry'");

        String reopened = "[install]\nfrozenLockfile = true\n[install]\nregistry = \"r\"\n";
        assertThat(toml("1.4.0", reopened, "install.registry")).isEqualTo("SyntaxError: Cannot redefine table");
        assertThat(toml("1.3.14", reopened, "install.registry")).isEqualTo("\"r\"");

        String dotted = "install.registry = \"r\"\n[x]\ny = 1\n[install]\ndev = true\n";
        assertThat(toml("1.4.0", dotted, "install.dev")).isEqualTo("SyntaxError: Cannot redefine table");

        assertThat(toml("1.4.0", "[install.scopes]\n@myorg = \"x\"\n", "install.scopes.@myorg"))
          .isEqualTo("SyntaxError: Expected a key but found (redacted)");
        assertThat(toml("1.3.14", "[install.scopes]\n@myorg = \"x\"\n", "install.scopes.@myorg")).isEqualTo("\"x\"");

        assertThat(toml("1.4.0", "[install]\nregistry : \"x\"\n", "install.registry"))
          .isEqualTo("SyntaxError: Expected '=' after a key but found (redacted)");
        assertThat(toml("1.3.14", "[install]\nregistry : \"x\"\n", "install.registry")).isEqualTo("\"x\"");

        assertThat(toml("1.4.0", "a = 1 b = 2\n", "a"))
          .isEqualTo("SyntaxError: Expected a newline or end of file after a key/value pair");
        assertThat(toml("1.4.0", "[install]\nregistry =\n\"x\"\n", "install.registry"))
          .isEqualTo("SyntaxError: Missing value after '='; values must be on the same line");
    }

    @Test
    void readsBunfigCommentsEscapesNumbersAndDatesAsTomlFrom140() {
        // a # that ends its line swallowed the next in 1.3.x
        assertThat(toml("1.4.0", "[install] #\nx = 1\n", "install.x")).isEqualTo("1.0");
        assertThat(toml("1.3.14", "[install] #\nx = 1\n", "install.x")).isEqualTo("absent");
        assertThat(toml("1.4.0", "#\nx = 1\n", "x")).isEqualTo("1.0");
        assertThat(toml("1.3.14", "#\nx = 1\n", "x")).isEqualTo("absent");

        assertThat(toml("1.4.0", "x = \"a\\tb\"\n", "x")).isEqualTo("\"a\tb\"");
        assertThat(toml("1.3.14", "x = \"a\\tb\"\n", "x")).isEqualTo("\"a\fb\"");
        assertThat(toml("1.4.0", "x = \"a\\qb\"\n", "x")).isEqualTo("SyntaxError: Invalid escape sequence: (redacted)");
        assertThat(toml("1.3.14", "x = \"a\\qb\"\n", "x")).isEqualTo("\"aqb\"");

        assertThat(toml("1.4.0", "x = 017\n", "x")).isEqualTo("SyntaxError: Leading zeros are not allowed in numbers");
        assertThat(toml("1.3.14", "x = 017\n", "x")).isEqualTo("15.0");
        assertThat(toml("1.4.0", "x = 9007199254740992\n", "x")).isEqualTo("SyntaxError: Integer cannot be losslessly " +
          "represented as a JavaScript number; it must be within +/-(2^53 - 1)");
        assertThat(toml("1.4.0", "x = 0x1F\ny = -0\n", "x")).isEqualTo("31.0");
        assertThat(toml("1.4.0", "x = 1979-05-27T07:32:00Z\n", "x")).isEqualTo("\"1979-05-27T07:32:00Z\"");

        assertThat(toml("1.4.0", "x = '''\nabc'''\n", "x")).isEqualTo("\"abc\"");
        assertThat(toml("1.3.14", "x = '''\nabc'''\n", "x")).isEqualTo("\"\nabc\"");

        // an unknown character is the end of the file to 1.3.x, which logs "Unexpected " and goes on
        assertThat(toml("1.3.14", "x = 1\n%\ny = 2\n", "x")).isEqualTo("reads past Unexpected ");
        assertThat(toml("1.4.0", "x = 1\n%\ny = 2\n", "x"))
          .isEqualTo("SyntaxError: Expected a key but found (redacted)");
    }

    /**
     * Arguments.zig loadConfig and bunfig's arguments.rs: a bunfig.toml that does not parse is the parser's error,
     * then {@code failed to load bunfig}.
     */
    @Test
    void reportsABunfigThatDoesNotParseAsSuch() {
        org.openrewrite.javascript.internal.LockFileRegeneration.Result result = new BunLockRegenerator(
          BunLockRegenerator.Inputs.builder()
            .lock(EMPTY_ROOT)
            .manifest("", "{\"name\":\"root\"}")
            .bunfig("[install]\nlinker = isolated\n")
            .now(java.time.Instant.parse("2026-10-11T00:00:00Z"))
            .release(BunRelease.of("1.4.0"))
            .build(), (url, name, extended) -> null).regenerate();
        assertThat(requireNonNull(result.getFailure()).getDetail())
          .contains("SyntaxError: failed to load bunfig: Strings must be quoted");
    }

    private static String repeat(char c, int n) {
        char[] chars = new char[n];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }

    // the table's \n, \t and \\ escapes
    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\t", "\t");
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
