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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Yarn 1 release an install is reproduced as. The port follows yarn 1.22.22; what another release does
 * differently is asked here, one question per upstream change, named for what yarn does and answered by the release
 * that changed it (its tag {@code v1.x.y} of yarnpkg/yarn). The ported modules read as 1.22.22's source with each other
 * release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version but yarn's own version
 * ({@link #toString()}), which the lock's header, {@code engines.yarn} and yarn's messages name.
 */
public final class YarnClassicRelease implements Comparable<YarnClassicRelease> {

    /**
     * The release the port follows, and the one an install runs as when nothing names another.
     */
    public static final YarnClassicRelease PORTED = new YarnClassicRelease(1, 22, 22);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    private YarnClassicRelease(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * The release of a version such as {@code 1.22.19}; a prerelease or a range names none.
     */
    public static YarnClassicRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not a Yarn release: " + version);
        }
        return new YarnClassicRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)));
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        return compareTo(new YarnClassicRelease(major, minor, patch)) >= 0;
    }

    // yarn src/cli/index.js

    /**
     * {@code main} stops a project whose package.json names another {@code packageManager} than yarn 0 or 1, unless
     * {@code COREPACK_ROOT} is set: from 1.22.20.
     */
    boolean checksPackageManager() {
        return atLeast(1, 22, 20);
    }

    /**
     * {@code findPackageManager} finds that field in the nearest package.json from where yarn runs up, past one
     * JSON.parse rejects, and {@code SKIP_YARN_COREPACK_CHECK} skips the check, from 1.22.21; 1.22.20 reads the
     * manifest of the directory yarn runs in ({@code readRootManifest}), and its errors end yarn before the install
     * runs.
     */
    boolean findsPackageManagerAbove() {
        return atLeast(1, 22, 21);
    }

    /**
     * {@code onUnexpectedError} reports an error that is no MessageError as {@code An unexpected error occurred:
     * "<message>".}, before 1.22.20. 1.22.21 reports it by its stack, whose first line names the error; 1.22.20 reports
     * none, and the engine names the error as 1.22.21 does.
     */
    boolean reportsUnexpectedErrors() {
        return !atLeast(1, 22, 20);
    }

    // yarn src/cli/commands/install.js

    /**
     * {@code fetchRequestFromCwd} makes the resolution of a package the root's optionalDependencies name optional
     * under {@code --ignore-optional}, from 1.17.0; before, every resolution is required.
     */
    boolean resolvesOptionalDependenciesOptionally() {
        return atLeast(1, 17, 0);
    }

    // yarn src/config.js

    /**
     * {@code init} holds a focused install's workspace by its package name ({@code focusedWorkspaceName}) and stops a
     * focus at the workspace root before it loads the registries, from 1.8.0; 1.7.0 takes the workspace whose package
     * the directory yarn runs in names, and stops a focus at the root in install's {@code init}.
     */
    boolean focusesByPackageName() {
        return atLeast(1, 8, 0);
    }

    /**
     * {@code unsafe-disable-integrity-migration} is a boolean that defaults to true, so that a lock entry without
     * integrity stays without it, from 1.12.0; before, the migration adds the integrity unless the option holds a
     * truthy value.
     */
    boolean keepsEntriesWithoutIntegrity() {
        return atLeast(1, 12, 0);
    }

    // yarn src/rc.js

    /**
     * {@code loadRcFile} parses an rc file as its path names it, a .yarnrc.yml as YAML, from 1.22.1; before, every rc
     * file as a .yarnrc.
     */
    boolean parsesRcFilesByTheirPath() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code loadRcFile} keeps a .yarnrc.yml's {@code yarnPath} as {@code yarn-path}, resolved against the file's
     * directory, from 1.17.2; 1.17.1 keeps it as {@code yarnPath}, unresolved, which {@code start} runs from where
     * yarn runs.
     */
    boolean resolvesYarnrcYmlYarnPath() {
        return atLeast(1, 17, 2);
    }

    /**
     * {@code loadRcFile} keeps a .yarnrc.yml's settings unless its {@code yarnPath} is a string, from 1.22.1; before,
     * it keeps its {@code yarnPath} alone, as {@code yarn-path}.
     */
    boolean keepsYarnrcYmlSettings() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code loadRcFile} reads the {@code yarnPath} of a .yarnrc.yml only where the file holds a value, from 1.22.20;
     * before, a file of no value throws.
     */
    boolean readsYarnPathOfAValue() {
        return atLeast(1, 22, 20);
    }

    // yarn src/util/rc.js

    /**
     * {@code parseRcPaths} throws what reading an rc file throws but a missing file, from 1.22.1; before, a file that
     * throws counts as empty.
     */
    boolean throwsRcFileErrors() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code getRcPaths} reads a .yarnrc.yml beside every .yarnrc, from 1.17.1.
     */
    boolean readsYarnrcYml() {
        return atLeast(1, 17, 1);
    }

    /**
     * {@code getRcPaths} reads each directory's .yarnrc.yml after its .yarnrc, from 1.17.2; 1.17.1 reads it before.
     */
    boolean readsYarnrcYmlAfterYarnrc() {
        return atLeast(1, 17, 2);
    }

    // yarn src/lockfile/parse.js

    /**
     * {@code parse} reads with js-yaml's failsafe schema what the lockfile parser cannot read, from 1.17.0.
     */
    boolean readsWithYamlWhatTheLockfileParserCannot() {
        return atLeast(1, 17, 0);
    }

    /**
     * {@code parse} reads a {@code .yml} file with js-yaml alone, from 1.22.1; before, as a lock first.
     */
    boolean parsesYmlAsYaml() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code parse} reads a {@code .yml} file whose document is no object as an empty object, from 1.22.2.
     */
    boolean readsYmlOfNoObjectAsEmpty() {
        return atLeast(1, 22, 2);
    }

    // yarn src/lockfile/index.js

    /**
     * {@code getLockfile} shares an entry among the patterns of one name and remote only, and no longer gives a shared
     * entry the {@code name} of the package where a pattern's name is another, from 1.22.22.
     */
    boolean sharesEntriesByName() {
        return atLeast(1, 22, 22);
    }

    /**
     * {@code fromDirectory} sets a Yarn Berry lock ({@code __metadata}) aside and resolves from scratch, from 1.19.0;
     * before, it reads the lock's entries as its own.
     */
    boolean setsBerryLocksAside() {
        return atLeast(1, 19, 0);
    }

    // yarn src/fetchers/tarball-fetcher.js

    /**
     * {@code createExtractor}'s map keeps every link an archive holds inside the package's directory, from 1.22.0.
     */
    boolean jailsTarballLinks() {
        return atLeast(1, 22, 0);
    }

    /**
     * {@code createExtractor} reports an archive tar cannot extract as corrupt ({@code errorExtractingTarball}), a
     * MessageError, from 1.8.0; before, by the extraction's own error.
     */
    boolean reportsCorruptArchives() {
        return atLeast(1, 8, 0);
    }

    /**
     * {@code createExtractor} checks a tarball against the remote's sha1 hash and its integrity both, from 1.19.0;
     * before, against its integrity, or its hash where it has none.
     */
    boolean checksTheHashBesideTheIntegrity() {
        return atLeast(1, 19, 0);
    }

    // yarn src/registries/npm-registry.js

    /**
     * {@code getRequestUrl} asks registry.yarnpkg.com and registry.npmjs.org over https whatever the url says, from
     * 1.17.3.
     */
    boolean requestsPublicRegistriesOverHttps() {
        return atLeast(1, 17, 3);
    }

    // yarn src/lockfile/index.js, src/package-request.js, src/resolvers/registries/npm-resolver.js (the integrity)

    /**
     * Yarn records each package's integrity, checks a tarball and a lock's entries against it, adds it to an entry
     * that has none and writes it to the lock, from 1.10.0; before, a lock holds no integrity and yarn reads none.
     */
    boolean recordsIntegrity() {
        return atLeast(1, 10, 0);
    }

    // yarn src/package-request.js

    /**
     * {@code findVersionInfo}'s {@code flatGlobalError} names the package that asks for a flat graph, from 1.12.0;
     * before, it passes none, and the message's {@code $0@$1} read {@code undefined@undefined}.
     */
    boolean namesTheFlatPackage() {
        return atLeast(1, 12, 0);
    }

    // yarn src/resolvers/registries/npm-resolver.js

    /**
     * {@code findVersionInRegistryResponse}'s {@code malformedRegistryResponse} names the package asked for, escaped,
     * from 1.12.0; before, the name the response gives.
     */
    boolean namesTheRequestedPackageOfAMalformedResponse() {
        return atLeast(1, 12, 0);
    }

    /**
     * {@code cleanRegistry} rewrites an http registry.npmjs.org url to registry.yarnpkg.com as an https one, from
     * 1.10.0 ({@code NPM_REGISTRY_RE}); before, an https one only.
     */
    boolean cleansHttpNpmRegistryUrls() {
        return atLeast(1, 10, 0);
    }

    // yarn src/resolvers/exotics/link-resolver.js

    /**
     * {@code resolve} makes a link to a directory without a package.json a package of the directory's name at 0.0.0,
     * from 1.17.0; before, only a link to a path that does not exist, and a directory without one fails.
     */
    boolean linksADirectoryWithoutManifest() {
        return atLeast(1, 17, 0);
    }

    /**
     * {@code resolve} makes a link to the lockfile's directory such a package too, from 1.18.0; before, it is the
     * root's own package.
     */
    boolean linksTheLockfileFolderAsADirectory() {
        return atLeast(1, 18, 0);
    }

    // yarn src/util/normalize-manifest/resolve-relative.js

    /**
     * A file: or link: dependency on the lockfile's directory is written {@code .}, from 1.18.0; before, one written
     * with a {@code ./} prefix becomes {@code ./.}.
     */
    boolean writesTheLockfileFolderAsDot() {
        return atLeast(1, 18, 0);
    }

    @Override
    public int compareTo(YarnClassicRelease o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) {
            c = Integer.compare(minor, o.minor);
        }
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof YarnClassicRelease && compareTo((YarnClassicRelease) o) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    /**
     * The version, as {@code yarn --version} prints it.
     */
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
