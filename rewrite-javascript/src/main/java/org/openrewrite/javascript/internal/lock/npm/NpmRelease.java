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

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The npm release an install is reproduced as. The port follows npm 11.17.0; what another release does differently is
 * asked here, one question per upstream change, named for what npm does and answered by the release that changed it.
 * The ported modules read as 11.17.0's source with each other release's branch beside it.
 */
@Getter
@EqualsAndHashCode
public final class NpmRelease implements Comparable<NpmRelease> {

    /**
     * The release the port follows, and the one an install runs as when nothing names another.
     */
    public static final NpmRelease PORTED = new NpmRelease(11, 17, 0);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    private NpmRelease(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * The release of a version such as {@code 10.9.2}; a prerelease or a range names none.
     */
    public static NpmRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not an npm release: " + version);
        }
        return new NpmRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    public boolean atLeast(int major, int minor, int patch) {
        return compareTo(new NpmRelease(major, minor, patch)) >= 0;
    }

    // @npmcli/arborist build-ideal-tree.js

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

    // npm-pick-manifest

    /**
     * The npm-pick-manifest npm bundles: 9.1.0 from npm 10.8.2, which picks as 11.0.3 does; 9.0.1 and older before.
     */
    PickManifest.Release pickManifest() {
        return atLeast(10, 8, 2) ? PickManifest.Release.V11_0_3 : PickManifest.Release.V9_0_1;
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
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
