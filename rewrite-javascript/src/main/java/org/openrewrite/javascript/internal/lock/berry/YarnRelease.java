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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.V8RegExp;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * The Yarn 4.x release an install is reproduced as: the data it ships that reaches a lock, from
 * {@code yarn-releases.json.gz}, and what it does otherwise than 4.10.3, whose code this package ports. Each difference
 * is a question named for what Yarn does, answered by the release that changed it; the ported modules read as 4.10.3's
 * source with each other release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version.
 */
@Getter
final class YarnRelease {

    private static final String RESOURCE = "yarn-releases.json.gz";

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private static volatile @Nullable Map<String, @Nullable Object> data;

    private final String version;

    @Getter(AccessLevel.NONE)
    private final int major;

    @Getter(AccessLevel.NONE)
    private final int minor;

    @Getter(AccessLevel.NONE)
    private final int patch;

    /**
     * {@code [descriptor, extension data]} in the table's order.
     */
    private final List<Map.Entry<String, Map<String, @Nullable Object>>> packageExtensions;

    private final List<String> patches;
    private final Pattern nodeGypMatch;
    private final String nodeGypRange;

    /**
     * The plugins the CLI bundles, in the order it activates them.
     */
    private final List<String> plugins;

    private final Set<String> settings;

    /**
     * Per builtin compat patch, the hash= its effects give a version: {@code ranges} the semver exclusivity of its
     * effects with the hash those and the unranged ones give, {@code none} the hash of the unranged ones
     * ({@code generate/compat-patch-hashes.js}). Its text changed at several releases.
     */
    @Getter(AccessLevel.NONE)
    private final Map<String, @Nullable Object> compatPatchHashes;

    private YarnRelease(String version, int major, int minor, int patch,
                        List<Map.Entry<String, Map<String, @Nullable Object>>> packageExtensions,
                        List<String> patches, Pattern nodeGypMatch, String nodeGypRange, List<String> plugins,
                        Set<String> settings, Map<String, @Nullable Object> compatPatchHashes) {
        this.version = version;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.packageExtensions = packageExtensions;
        this.patches = patches;
        this.nodeGypMatch = nodeGypMatch;
        this.nodeGypRange = nodeGypRange;
        this.plugins = plugins;
        this.settings = settings;
        this.compatPatchHashes = compatPatchHashes;
    }

    /**
     * The hash= the release's builtin compat patch gives {@code version} ({@code makePatchHash}); null for a patch the
     * release does not ship, or a version two of its ranges hold.
     */
    @Nullable
    String compatPatchHash(String name, String version) {
        Object patchTable = compatPatchHashes.get(name);
        if (!(patchTable instanceof Map)) {
            return null;
        }
        String hash = (String) Js.get(patchTable, "none");
        boolean matched = false;
        for (Object entry : (List<?>) Js.get(patchTable, "ranges")) {
            List<?> range = (List<?>) entry;
            if (YarnpkgSemverUtils.satisfiesWithPrereleases(version, (String) range.get(0), false)) {
                if (matched) {
                    return null;
                }
                hash = (String) range.get(1);
                matched = true;
            }
        }
        return hash;
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        int c = Integer.compare(this.major, major);
        if (c == 0) {
            c = Integer.compare(this.minor, minor);
        }
        return (c != 0 ? c : Integer.compare(this.patch, patch)) >= 0;
    }

    boolean hasPlugin(String name) {
        return plugins.contains(name);
    }

    // @yarnpkg/core Project.ts

    /**
     * {@code LOCKFILE_VERSION}: 8, 9 from 4.14.0, 10 from 4.15.0.
     */
    int getLockfileVersion() {
        return atLeast(4, 15, 0) ? 10 : atLeast(4, 14, 0) ? 9 : 8;
    }

    /**
     * {@code generateLockfile} writes a checksum without its cache key when the key is the lockfile's: before 4.1.0.
     */
    boolean writesChecksumWithoutCacheKey() {
        return !atLeast(4, 1, 0);
    }

    // @yarnpkg/core Configuration.ts

    /**
     * The command the usage errors for an unknown setting or shape key name: {@code yarn config -v} before 4.9.3.
     */
    String unknownSettingHint() {
        return atLeast(4, 9, 3) ? "yarn config" : "yarn config -v";
    }

    /**
     * {@code IGNORED_ENV_VARIABLES} holds {@code zipDataEpilogue}, from 4.1.0.
     */
    boolean ignoresZipDataEpilogue() {
        return atLeast(4, 1, 0);
    }

    /**
     * {@code IGNORED_ENV_VARIABLES} holds {@code osOverride}, {@code cpuOverride} and {@code libcOverride}, from
     * 4.10.0.
     */
    boolean ignoresArchitectureOverrides() {
        return atLeast(4, 10, 0);
    }

    /**
     * {@code IGNORED_ENV_VARIABLES} holds {@code ignoreScripts}, so that Yarn 1's {@code YARN_IGNORE_SCRIPTS} is no
     * setting: from 4.15.0.
     */
    boolean ignoresYarnClassicIgnoreScripts() {
        return atLeast(4, 15, 0);
    }

    /**
     * {@code normalizePackage} adds no optional {@code @types} peer for a peer whose types the package depends on,
     * from 4.9.2.
     */
    boolean skipsTypesPeerOfATypesDependency() {
        return atLeast(4, 9, 2);
    }

    // @yarnpkg/core miscUtils.ts

    /**
     * {@code replaceEnvVariables} keeps a {@code ${...}} a backslash precedes, from 4.11.0.
     */
    boolean escapesEnvironmentSubstitutions() {
        return atLeast(4, 11, 0);
    }

    /**
     * {@code replaceEnvVariables} scans the value once, with nested defaults, {@code ${VAR-default}} beside
     * {@code ${VAR:-default}}, escapes of {@code \}, {@code $} and {@code }}, and usage errors for a substitution it
     * does not read: from 4.13.0.
     */
    boolean scansEnvironmentSubstitutions() {
        return atLeast(4, 13, 0);
    }

    // @yarnpkg/plugin-npm index.ts and npmConfigUtils.ts

    /**
     * {@code isPackageApproved}: the resolvers leave out a version {@code npmMinimalAgeGate} quarantines and
     * {@code npmPreapprovedPackages} does not exempt, and the {@code latest} tag falls to the newest approved version
     * below it, from 4.10.0.
     */
    boolean approvesPackagesByAge() {
        return atLeast(4, 10, 0);
    }

    /**
     * {@code shouldBeQuarantined} quarantines a version the packument gives no publish time, from 4.10.2; before, the
     * time {@code new Date(undefined)} makes is no age, and a packument without times throws.
     */
    boolean quarantinesVersionsWithoutPublishTime() {
        return atLeast(4, 10, 2);
    }

    /**
     * {@code npmMinimalAgeGate} is a duration ({@code 7d}) read in minutes ({@code SettingsType.DURATION},
     * {@code parseDuration}), from 4.11.0; before, a number of minutes ({@code parseInt}).
     */
    boolean readsAgeGateAsDuration() {
        return atLeast(4, 11, 0);
    }

    /**
     * {@code npmMinimalAgeGate} defaults to {@code 1d}, from 4.15.0; before, to none.
     */
    boolean gatesAgeByDefault() {
        return atLeast(4, 15, 0);
    }

    /**
     * An {@code npmScopes} entry takes {@code npmMinimalAgeGate}, which {@code getMinimalAgeGate} reads for a package
     * of that scope before the global one: from 4.17.0.
     */
    boolean gatesAgePerScope() {
        return atLeast(4, 17, 0);
    }

    /**
     * {@code getScopeRegistry} sends the {@code jsr} scope to npm.jsr.io when {@code npmScopes} does not name it, from
     * 4.9.0.
     */
    boolean sendsJsrScopeToJsr() {
        return atLeast(4, 9, 0);
    }

    // @yarnpkg/plugin-npm npmHttpUtils.ts and NpmSemverFetcher.ts

    /**
     * {@code getIdentUrl} and {@code getLocatorUrl} encode the scope, name and version
     * ({@code encodeURIComponent}), from 4.15.0.
     */
    boolean encodesRegistryPaths() {
        return atLeast(4, 15, 0);
    }

    // @yarnpkg/plugin-npm NpmSemverResolver.ts

    /**
     * {@code getCandidates} tests {@code isPackageApproved} after the range, outside its try, so that a test that
     * throws fails the install, and fails with {@code All versions satisfying "<range>" are quarantined} where the
     * range matches only quarantined versions: from 4.13.0.
     */
    boolean approvesCandidatesAfterTheRange() {
        return atLeast(4, 13, 0);
    }

    /**
     * {@code selectMatchingVersions}: a {@code *} range that matches no version matches the prereleases, from 4.18.0.
     */
    boolean resolvesStarToPrereleases() {
        return atLeast(4, 18, 0);
    }

    // @yarnpkg/plugin-npm NpmTagResolver.ts

    /**
     * {@code getCandidates}: below a quarantined {@code latest} that is no prerelease, the newest approved version is
     * no prerelease either, from 4.10.3.
     */
    boolean skipsPrereleasesBelowLatest() {
        return atLeast(4, 10, 3);
    }

    // @yarnpkg/plugin-http TarballHttpResolver.ts, TarballHttpFetcher.ts and urlUtils.ts

    /**
     * {@code isTgzUrl}: a url is a tarball's by its path, which may end in a segment without a dot, from 4.0.2; before,
     * an http(s) url ending in {@code .tgz} or {@code .tar.gz}.
     */
    boolean readsTarballUrlsByPathname() {
        return atLeast(4, 0, 2);
    }

    // @yarnpkg/plugin-essentials commands/install.ts

    /**
     * {@code LOCKFILE_MIGRATION_RULES} over a lock older than version 9 set {@code approvedGitRepositories} to
     * {@code **} and {@code enableScripts} to true, from 4.14.0.
     */
    boolean migratesGitApprovalAndScripts() {
        return atLeast(4, 14, 0);
    }

    /**
     * {@code LOCKFILE_MIGRATION_RULES} over a lock older than version 10 set {@code npmMinimalAgeGate} to {@code 0},
     * from 4.15.0.
     */
    boolean migratesAgeGate() {
        return atLeast(4, 15, 0);
    }

    // @yarnpkg/plugin-git gitUtils.ts and index.ts

    /**
     * {@code validateRepoUrl} blocks a repository {@code approvedGitRepositories} does not match (YN0080), which
     * defaults to none: from 4.14.0.
     */
    boolean approvesGitRepositories() {
        return atLeast(4, 14, 0);
    }

    // @yarnpkg/plugin-exec ExecResolver.ts

    /**
     * {@code bindDescriptor}: only a workspace may depend on an {@code exec:} package (YN0057), from 4.14.0.
     */
    boolean limitsExecToWorkspaces() {
        return atLeast(4, 14, 0);
    }

    // @yarnpkg/plugin-catalog utils.ts

    /**
     * {@code resolveDescriptorFromCatalog} returns a catalog's range no resolver supports as it is, unbound, from
     * 4.11.0.
     */
    boolean keepsCatalogRangesNoResolverSupports() {
        return atLeast(4, 11, 0);
    }

    // @yarnpkg/parsers syml.ts

    /**
     * {@code parseViaJsYaml} parses with js-yaml 4's {@code load}, from 4.18.0; before, js-yaml 3.14.1's
     * {@code safeLoad}.
     */
    boolean parsesWithJsYaml4() {
        return atLeast(4, 18, 0);
    }

    /**
     * The release, or null when the data has none of that version.
     */
    @SuppressWarnings("unchecked")
    static @Nullable YarnRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            return null;
        }
        Map<String, @Nullable Object> all = data();
        Object release = Js.get(all.get("releases"), version);
        if (!(release instanceof Map)) {
            return null;
        }
        Map<String, Object> tables = (Map<String, Object>) all.get("tables");
        List<Map.Entry<String, Map<String, @Nullable Object>>> extensions = new ArrayList<>();
        for (Object entry : (List<Object>) tables.get((String) Js.get(release, "packageExtensions"))) {
            List<Object> pair = (List<Object>) entry;
            extensions.add(new AbstractMap.SimpleImmutableEntry<>((String) pair.get(0),
                    (Map<String, @Nullable Object>) pair.get(1)));
        }
        Map<String, Object> nodeGyp = (Map<String, Object>) Js.get(release, "nodeGyp");
        Set<String> settings = new LinkedHashSet<>((List<String>) tables.get((String) Js.get(release, "settings")));
        return new YarnRelease(version, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), Collections.unmodifiableList(extensions),
                Collections.unmodifiableList((List<String>) Js.get(release, "patches")),
                V8RegExp.of((String) nodeGyp.get("pattern")), (String) nodeGyp.get("range"),
                Collections.unmodifiableList((List<String>) tables.get((String) Js.get(release, "plugins"))),
                Collections.unmodifiableSet(settings),
                (Map<String, @Nullable Object>) tables.get((String) Js.get(release, "compatPatchHashes")));
    }

    /**
     * Every release the data holds, in ascending order.
     */
    static List<String> all() {
        List<String> versions = new ArrayList<>(Js.keys(data().get("releases")));
        versions.sort((a, b) -> {
            Matcher x = VERSION.matcher(a);
            Matcher y = VERSION.matcher(b);
            if (!x.matches() || !y.matches()) {
                return a.compareTo(b);
            }
            for (int i = 1; i <= 3; i++) {
                int c = Integer.compare(Integer.parseInt(x.group(i)), Integer.parseInt(y.group(i)));
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        });
        return versions;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> data() {
        Map<String, @Nullable Object> loaded = data;
        if (loaded == null) {
            InputStream resource = YarnRelease.class.getResourceAsStream(RESOURCE);
            if (resource == null) {
                throw new IllegalStateException(RESOURCE + " is missing");
            }
            try (InputStream in = new GZIPInputStream(resource)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
                loaded = (Map<String, @Nullable Object>) Js.jsonParse(new String(out.toByteArray(),
                        StandardCharsets.UTF_8));
                data = loaded;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return loaded;
    }

    @Override
    public String toString() {
        return version;
    }
}
