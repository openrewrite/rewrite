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
     * {@code #applyRootOverridesToWorkspaces}: a workspace's edge to a package the root overrides is detached when the
     * root's copy does not satisfy it, from npm 11.3.0 until 11.14.0.
     */
    boolean appliesRootOverridesToWorkspaces() {
        return atLeast(11, 3, 0) && !atLeast(11, 14, 0);
    }

    /**
     * {@code #checkAllow}: {@code allow-git}, {@code allow-file}, {@code allow-directory} and {@code allow-remote}
     * are enforced before a spec is linked or fetched, from npm 11.15.0; before, pacote alone enforces them, on what
     * it fetches.
     */
    boolean checksAllowedSpecTypes() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code #releaseAgeBefore}: a package {@code min-release-age-exclude} names is exempt from {@code before}, from
     * npm 11.17.0.
     */
    boolean exemptsFromReleaseAge() {
        return atLeast(11, 17, 0);
    }

    /**
     * {@code #nodeFromSpec}: a registry manifest without a version is a load failure
     * ({@code EINCOMPLETEMANIFEST}), from npm 11.15.0.
     */
    boolean failsIncompleteManifests() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code #fetchManifest}: pacote's {@code _isRoot} is whether the node the manifest is fetched under is the
     * project root or a workspace, from npm 11.13.0 until 11.15.0 asks the edge's dependent instead; before, pacote
     * is told nothing.
     */
    boolean fetchesAsRootUnderRootParent() {
        return atLeast(11, 13, 0);
    }

    /**
     * {@code #findHoistableNode}: an invalid optional peer edge takes a node already in the tree that satisfies it
     * before the registry is asked, from npm 11.14.0 (npm/cli#9249).
     */
    boolean findsHoistablePeerOptional() {
        return atLeast(11, 14, 0);
    }

    // @npmcli/arborist dep-valid.js

    /**
     * {@code lockedGitCommittish}: a git edge naming a ref is invalid when the lock records another committish for
     * it, from npm 11.17.0.
     */
    boolean comparesLockedGitCommittish() {
        return atLeast(11, 17, 0);
    }

    // @npmcli/arborist link.js

    /**
     * {@code recalculateOutEdgesOverrides}: a link forwards the overrides it receives to its target, from npm 11.14.0.
     */
    boolean forwardsLinkOverridesToTarget() {
        return atLeast(11, 14, 0);
    }

    /**
     * {@code recalculateOutEdgesOverrides}: a link forwards only an override set with a rule naming a dependency of
     * its target, from npm 11.15.0 (npm/cli#9357).
     */
    boolean forwardsLinkOverridesWithMatchingRule() {
        return atLeast(11, 15, 0);
    }

    // @npmcli/arborist optional-set.js

    /**
     * The set leaves out what an inert node outside it depends on, from npm 11.13.0.
     */
    boolean leavesInertDependentsOutOfOptionalSet() {
        return atLeast(11, 13, 0);
    }

    // @npmcli/arborist shrinkwrap.js

    /**
     * {@code commit}: an extraneous node outside node_modules, a workspace or {@code file:} dependency the manifest
     * no longer names, gets no entry, from npm 11.15.0.
     */
    boolean dropsExtraneousOutsideNodeModules() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code commit}: an extraneous node outside node_modules keeps its entry under another package's directory,
     * from npm 11.17.0.
     */
    boolean keepsNestedExtraneousFsChildren() {
        return atLeast(11, 17, 0);
    }

    /**
     * {@code commit}: an inert node without a version, an optional dependency that failed to load, gets no entry,
     * from npm 11.15.0.
     */
    boolean skipsInertNodesWithoutVersion() {
        return atLeast(11, 15, 0);
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

    /**
     * {@code allow-directory} is a config from npm 11.14.0.
     */
    boolean definesAllowDirectory() {
        return atLeast(11, 14, 0);
    }

    /**
     * {@code allow-file} is a config from npm 11.14.0.
     */
    boolean definesAllowFile() {
        return atLeast(11, 14, 0);
    }

    /**
     * {@code allow-git} is a config from npm 11.9.0.
     */
    boolean definesAllowGit() {
        return atLeast(11, 9, 0);
    }

    /**
     * {@code allow-remote} is a config from npm 11.14.0.
     */
    boolean definesAllowRemote() {
        return atLeast(11, 14, 0);
    }

    /**
     * {@code min-release-age} is a config from npm 11.10.0.
     */
    boolean definesMinReleaseAge() {
        return atLeast(11, 10, 0);
    }

    /**
     * {@code min-release-age-exclude} is a config from npm 11.17.0.
     */
    boolean definesMinReleaseAgeExclude() {
        return atLeast(11, 17, 0);
    }

    /**
     * {@code min-release-age}'s flatten: {@code before} wins over it within one source and a falsy age filters
     * nothing, from npm 11.15.0; before, the two exclude each other and any age sets {@code before}.
     */
    boolean yieldsMinReleaseAgeToBefore() {
        return atLeast(11, 15, 0);
    }

    // pacote fetcher.js

    /**
     * {@code canUse}: the {@code EALLOWGIT} message names the spec type as every {@code EALLOW*} does, from npm
     * 11.10.0 (pacote 21.3.1).
     */
    boolean namesTypeInAllowRejection() {
        return atLeast(11, 10, 0);
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
