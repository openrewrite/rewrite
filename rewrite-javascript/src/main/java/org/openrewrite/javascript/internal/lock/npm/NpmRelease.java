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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The npm release an install is reproduced as. The port follows npm 11.17.0; what another release does differently is
 * asked here, one question per upstream change, named for what npm does and answered by the release that changed it.
 * The ported modules read as 11.17.0's source with each other release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version: a release that is
 * {@linkplain #followed() followed} notes each release it was compared with, so that another release giving the same
 * answers ({@link #answersAs}) is known to install the project the same way without installing it again.
 */
public final class NpmRelease implements Comparable<NpmRelease> {

    /**
     * The release the port follows, and the one an install runs as when nothing names another.
     */
    public static final NpmRelease PORTED = new NpmRelease(11, 17, 0, null);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    // the releases an install compared this one with; null when its answers are not being noted
    private final @Nullable Set<NpmRelease> asked;

    private NpmRelease(int major, int minor, int patch, @Nullable Set<NpmRelease> asked) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.asked = asked;
    }

    /**
     * The release of a version such as {@code 10.9.2}; a prerelease or a range names none.
     */
    public static NpmRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not an npm release: " + version);
        }
        return new NpmRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                null);
    }

    /**
     * This release, noting from here on every release an install compares it with.
     */
    public NpmRelease followed() {
        return new NpmRelease(major, minor, patch, new LinkedHashSet<>());
    }

    /**
     * Whether this release answers every question {@code followed} was asked as it did: an install that asked
     * nothing else of its release runs the same under both.
     */
    public boolean answersAs(NpmRelease followed) {
        if (followed.asked == null) {
            return equals(followed);
        }
        for (NpmRelease threshold : followed.asked) {
            if (compareTo(threshold) >= 0 != followed.compareTo(threshold) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        NpmRelease threshold = new NpmRelease(major, minor, patch, null);
        if (asked != null) {
            asked.add(threshold);
        }
        return compareTo(threshold) >= 0;
    }

    // @npmcli/arborist build-ideal-tree.js

    /**
     * {@code #checkEngineAndPlatform} leaves the root's {@code engines} unchecked when it has {@code devEngines}:
     * from npm 10.9.0.
     */
    boolean devEnginesReplaceRootEngines() {
        return atLeast(10, 9, 0);
    }

    /**
     * {@code #fetchManifest} drops {@code license} from the manifest pacote returns: npm 10.4.0 until 10.8.0.
     */
    boolean dropsLicenseOfFetchedManifest() {
        return atLeast(10, 4, 0) && !atLeast(10, 8, 0);
    }

    /**
     * {@code #fetchManifest} asks pacote for the full packument ({@code fullMetadata: true}): from npm 10.4.0.
     */
    boolean fetchesFullMetadata() {
        return atLeast(10, 4, 0);
    }

    /**
     * {@code DepsQueue} keeps one list per depth, each sorted by path alone, in place of one list sorted when it is
     * popped: npm 10.4.0 until 10.8.0.
     */
    boolean queuesDepsByDepth() {
        return atLeast(10, 4, 0) && !atLeast(10, 8, 0);
    }

    // @npmcli/arborist shrinkwrap.js

    /**
     * {@code metaFromNode} writes the package name of a node whose folder is named otherwise even when the node
     * itself goes by that name, as a link target just placed does: from npm 10.8.2.
     */
    boolean namesEntryByFolder() {
        return atLeast(10, 8, 2);
    }

    // @npmcli/config definitions.js

    /**
     * The default of {@code allow-git}: {@code none} from npm 12.0.0.
     */
    String allowGitDefault() {
        return atLeast(12, 0, 0) ? "none" : "all";
    }

    /**
     * The default of {@code allow-remote}: {@code none} from npm 12.0.0.
     */
    String allowRemoteDefault() {
        return atLeast(12, 0, 0) ? "none" : "all";
    }

    // @npmcli/map-workspaces

    /**
     * The @npmcli/map-workspaces npm bundles: 3.0.6 from npm 10.5.2, which finds the members 5.0.3 finds; 3.0.4
     * before.
     */
    MapWorkspaces.Release mapWorkspaces() {
        return atLeast(10, 5, 2) ? MapWorkspaces.Release.V5_0_3 : MapWorkspaces.Release.V3_0_4;
    }

    // @npmcli/package-json normalize.js

    /**
     * The @npmcli/package-json whose {@code bin} step pacote runs over a fetched manifest: 6.0.1 from npm 10.9.0,
     * which reads as 7.0.5 there; 5.1.1 and 5.2.0 from npm 10.8.1; 5.1.0 and, before npm 10.6.0,
     * read-package-json-fast before that.
     */
    PackageJson.Release packageJson() {
        return atLeast(10, 9, 0) ? PackageJson.Release.V7_0_5 :
                atLeast(10, 8, 1) ? PackageJson.Release.V5_2_0 : PackageJson.Release.V5_1_0;
    }

    // npm-pick-manifest

    /**
     * The npm-pick-manifest npm bundles: 9.1.0 from npm 10.8.2, which picks as 11.0.3 does; 9.0.1 and older before.
     */
    PickManifest.Release pickManifest() {
        return atLeast(10, 8, 2) ? PickManifest.Release.V11_0_3 : PickManifest.Release.V9_0_1;
    }

    // pacote dir.js

    /**
     * The manifest of a directory gets its {@code bin} from the files of {@code directories.bin}:
     * read-package-json-fast until npm 10.6.0 and @npmcli/package-json 7 from npm 11.6.1 fill it,
     * @npmcli/package-json 5 and 6 between them do not.
     */
    boolean dirFetcherReadsBinDirectory() {
        return !atLeast(10, 6, 0) || atLeast(11, 6, 1);
    }

    @Override
    public int compareTo(NpmRelease o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) {
            c = Integer.compare(minor, o.minor);
        }
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof NpmRelease && compareTo((NpmRelease) o) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
