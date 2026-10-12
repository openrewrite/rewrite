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

    // yarn src/rc.js

    /**
     * {@code loadRcFile} parses an rc file as its path names it, a .yarnrc.yml as YAML, from 1.22.1; before, every rc
     * file as a .yarnrc.
     */
    boolean parsesRcFilesByTheirPath() {
        return atLeast(1, 22, 1);
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

    // yarn src/lockfile/parse.js

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

    // yarn src/fetchers/tarball-fetcher.js

    /**
     * {@code createExtractor}'s map keeps every link an archive holds inside the package's directory, from 1.22.0.
     */
    boolean jailsTarballLinks() {
        return atLeast(1, 22, 0);
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
