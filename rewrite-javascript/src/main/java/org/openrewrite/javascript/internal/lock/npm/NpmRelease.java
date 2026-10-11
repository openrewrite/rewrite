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
import org.openrewrite.javascript.internal.lock.BraceExpansion;
import org.openrewrite.javascript.semver.NodeSemver;

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
     * {@code #resolveLinks} gives a link's overrides to its target before the target resolves, from npm 11.18.0
     * (npm/cli#9659).
     */
    boolean forwardsLinkOverridesToSubtree() {
        return atLeast(11, 18, 0);
    }

    /**
     * {@code #loadPeerSet} resolves an optional peer only against a version in the tree that does not satisfy it, from
     * npm 11.20.0 and, of npm 12, from 12.1.0; before, every optional peer the virtual root lacks.
     */
    boolean peerSetHoldsOptionalPeerOnlyOnConflict() {
        return atLeast(12, 1, 0) || atLeast(11, 20, 0) && !atLeast(12, 0, 0);
    }

    /**
     * {@code #loadPeerSet} stops at a node its own peer set detached, where it failed reading the conflict, from npm
     * 11.19.0 and, of npm 12, from 12.0.2.
     */
    boolean peerSetStopsAtDetachedNode() {
        return atLeast(12, 0, 2) || atLeast(11, 19, 0) && !atLeast(12, 0, 0);
    }

    // @npmcli/arborist calc-dep-flags.js

    /**
     * A link only unsets its target's flags, as an edge does, from npm 11.18.0; before, it assigns its own.
     */
    boolean linkOnlyUnsetsTargetFlags() {
        return atLeast(11, 18, 0);
    }

    // @npmcli/arborist link.js

    /**
     * A link forwards its overrides when a rule applies anywhere under its target ({@code overrideMatchesSubtree}),
     * from npm 11.18.0 (npm/cli#9619); before, when a rule names a dependency the target declares.
     */
    boolean linkOverridesMatchSubtree() {
        return atLeast(11, 18, 0);
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

    // brace-expansion

    /**
     * The brace-expansion minimatch calls: 5.0.9 from npm 11.19.1 and, of npm 12, from 12.1.0, which combines the
     * sets of a pattern left to right (index.js expand_); before, 5.0.6 and 5.0.7, which expand alike.
     */
    BraceExpansion.Release braceExpansion() {
        return atLeast(12, 1, 0) || atLeast(11, 19, 1) && !atLeast(12, 0, 0) ? BraceExpansion.Release.V5_0_9 :
                BraceExpansion.Release.V5_0_6;
    }

    // semver

    /**
     * The node-semver npm bundles: 7.8.5 from npm 11.18.0, whose {@code ~1.2} reaches down to {@code 1.2.0-0} under
     * {@code includePrerelease} (classes/range.js).
     */
    NodeSemver.Release semver() {
        return atLeast(11, 18, 0) ? NodeSemver.Release.V7_8_5 : NodeSemver.Release.V7_8_4;
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
