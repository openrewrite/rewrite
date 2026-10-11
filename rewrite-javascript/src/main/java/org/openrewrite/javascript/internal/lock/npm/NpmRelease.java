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
     * {@code #applyRootOverridesToWorkspaces}: a workspace's edge to a package the root overrides is detached when the
     * root's copy does not satisfy it, from npm 11.3.0 until 11.14.0.
     */
    boolean appliesRootOverridesToWorkspaces() {
        return atLeast(11, 3, 0) && !atLeast(11, 14, 0);
    }

    /**
     * {@code #checkAllow}: {@code allow-git}, {@code allow-file}, {@code allow-directory} and {@code allow-remote} are
     * enforced before a spec is linked or fetched, from npm 11.15.0; before, pacote alone enforces them, on what it
     * fetches.
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
     * {@code #nodeFromSpec}: a registry manifest without a version is a load failure ({@code EINCOMPLETEMANIFEST}),
     * from npm 11.15.0.
     */
    boolean failsIncompleteManifests() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code #fetchManifest}: pacote's {@code _isRoot} is whether the node the manifest is fetched under is the project
     * root or a workspace, from npm 11.13.0 until 11.15.0 asks the edge's dependent instead; before, pacote is told
     * nothing.
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

    /**
     * {@code #nodeFromSpec}: under {@code install-links}, a directory dependency of an installed {@code file:} package
     * is installed from that package's source instead of linked, from npm 11.6.0.
     */
    boolean installsTransitiveFileDependencies() {
        return atLeast(11, 6, 0);
    }

    /**
     * {@code #pruneFailedOptional}: an optional dependency that failed to load stays in the tree, inert, from npm
     * 11.3.0; before, it and its optional set are removed.
     */
    boolean keepsFailedOptionalInert() {
        return atLeast(11, 3, 0);
    }

    /**
     * {@code #initTree}: a missing optional peer does not queue its dependent, from npm 11.6.3.
     */
    boolean leavesMissingPeerOptionalUnqueued() {
        return atLeast(11, 6, 3);
    }

    /**
     * {@code #nodeFromSpec}: a {@code file:./} or {@code file:../} dependency inside the project is linked whatever
     * {@code install-links} says, from npm 11.5.0.
     */
    boolean linksProjectInternalFileSpecs() {
        return atLeast(11, 5, 0);
    }

    /**
     * {@code #checkEngineAndPlatform}: an optional package the engine or platform excludes is marked inert with its
     * optional set, from npm 11.6.2.
     */
    boolean marksUninstallableOptionalInert() {
        return atLeast(11, 6, 2);
    }

    /**
     * {@code #idealTreePrune}: a node flagged both peer and optional is pruned like an extraneous one, from npm 11.5.0
     * until 11.6.3.
     */
    boolean prunesOptionalPeers() {
        return atLeast(11, 5, 0) && !atLeast(11, 6, 3);
    }

    /**
     * {@code #initTree}: a node with an edge to nothing is queued like one with an invalid edge, from npm 11.6.2.
     */
    boolean queuesNodesMissingDependencies() {
        return atLeast(11, 6, 2);
    }

    /**
     * {@code #inflateAncientLockfile}: the dependency flags are reset before they are calculated again, from npm
     * 11.6.3.
     */
    boolean resetsDepFlagsOfInflatedLock() {
        return atLeast(11, 6, 3);
    }

    /**
     * An invalid optional peer edge is a problem only when the install saves, and a placement that invalidates an edge
     * of a node already visited queues that node again when it does, from npm 11.10.1 (npm/cli#8726).
     */
    boolean resolvesInvalidPeerOptionalBySave() {
        return atLeast(11, 10, 1);
    }

    /**
     * {@code #checkEngineAndPlatform}: a node {@code omit} leaves out is not checked, from npm 11.5.0.
     */
    boolean skipsEngineCheckOfOmitted() {
        return atLeast(11, 5, 0);
    }

    /**
     * {@code #loadPeerSet}: the peers of a node that lost its parent while they loaded are left alone, from npm 11.6.0;
     * before, arborist reads the missing parent.
     */
    boolean stopsPeerSetOfDetachedNode() {
        return atLeast(11, 6, 0);
    }

    // @npmcli/arborist calc-dep-flags.js

    /**
     * The flags are unset along the edges from a queue of changed nodes, from npm 11.6.3; before, by a depth walk that
     * unsets each flag down the dependencies.
     */
    boolean calculatesDepFlagsByQueue() {
        return atLeast(11, 6, 3);
    }

    /**
     * The depth walk flags the target of a peer edge peer and unsets the flag only where no peer edge comes in, from
     * npm 11.6.1.
     */
    boolean marksPeerEdgeTargetsPeer() {
        return atLeast(11, 6, 1);
    }

    /**
     * {@code calcDepFlagsStep} and {@code unsetFlag} pass over a link whose target is null, from npm 10.8.2; before,
     * they set the flags of the null target.
     */
    boolean passesOverLinkWithoutTarget() {
        return atLeast(10, 8, 2);
    }

    // @npmcli/arborist dep-valid.js

    /**
     * {@code lockedGitCommittish}: a git edge naming a ref is invalid when the lock records another committish for it,
     * from npm 11.17.0.
     */
    boolean comparesLockedGitCommittish() {
        return atLeast(11, 17, 0);
    }

    /**
     * A git committish of up to 64 hex digits is a commit, from npm 11.10.0; before, 40 exactly.
     */
    boolean readsSha256Commits() {
        return atLeast(11, 10, 0);
    }

    // @npmcli/arborist edge.js

    /**
     * {@code error}: the override sets of an edge and its target are compared only when the target has one, from npm
     * 11.6.4.
     */
    boolean comparesOverridesOfOverriddenTargets() {
        return atLeast(11, 6, 4);
    }

    /**
     * {@code spec}: a {@code $name} override the root does not declare is read from the edge's own package, from npm
     * 11.3.0.
     */
    boolean resolvesOverrideReferenceInOwnPackage() {
        return atLeast(11, 3, 0);
    }

    // @npmcli/arborist link.js

    /**
     * {@code recalculateOutEdgesOverrides}: a link forwards the overrides it receives to its target, from npm 11.14.0.
     */
    boolean forwardsLinkOverridesToTarget() {
        return atLeast(11, 14, 0);
    }

    /**
     * {@code recalculateOutEdgesOverrides}: a link forwards only an override set with a rule naming a dependency of its
     * target, from npm 11.15.0 (npm/cli#9357).
     */
    boolean forwardsLinkOverridesWithMatchingRule() {
        return atLeast(11, 15, 0);
    }

    // @npmcli/arborist load-virtual.js

    /**
     * {@code #resolveLinks}: a failure to assign a link target's package.json is swallowed like a failure to read it,
     * from npm 11.6.1, where @npmcli/package-json replaces read-package-json-fast.
     */
    boolean guardsLinkTargetPackage() {
        return atLeast(11, 6, 1);
    }

    /**
     * {@code #resolveLinks}: a link whose target the lock lacks is {@code EMISSINGTARGET}, from npm 11.4.0; before,
     * arborist reads the missing target.
     */
    boolean rejectsLinkWithoutTarget() {
        return atLeast(11, 4, 0);
    }

    // @npmcli/arborist node.js

    /**
     * {@code shouldOmit}: only a node under the project root or a workspace is omitted, from npm 11.6.1.
     */
    boolean omitsUnderProjectTopsOnly() {
        return atLeast(11, 6, 1);
    }

    /**
     * An edge passes its override set on to its target, which keeps the most specific of the sets coming in, and an
     * edge is invalid when its set conflicts with its target's, from npm 11.2.0 (also edge.js); before, a node takes
     * its parent's rule for it and the set of the last edge in.
     */
    boolean propagatesOverrideSets() {
        return atLeast(11, 2, 0);
    }

    // @npmcli/arborist optional-set.js

    /**
     * The set gathers the dependencies no node outside it depends on, from npm 11.6.2; before, those no node outside it
     * requires through a non-optional edge.
     */
    boolean boundsOptionalSetByMembership() {
        return atLeast(11, 6, 2);
    }

    /**
     * A node not flagged optional has an optional set too, from npm 11.6.3.
     */
    boolean gathersOptionalSetOfAnyNode() {
        return atLeast(11, 6, 3);
    }

    /**
     * The set leaves out what an inert node outside it depends on, from npm 11.13.0.
     */
    boolean leavesInertDependentsOutOfOptionalSet() {
        return atLeast(11, 13, 0);
    }

    // @npmcli/arborist override-set.js

    /**
     * {@code doOverrideSetsConflict}: two sets neither of which contains the other conflict only when their rules for a
     * package do not intersect, from npm 11.6.3.
     */
    boolean comparesOverrideRulesForConflict() {
        return atLeast(11, 6, 3);
    }

    /**
     * {@code findSpecificOverrideSet}: two sets neither of which contains the other, with compatible rules, resolve to
     * their nearest common ancestor, from npm 11.12.0.
     */
    boolean fallsBackToCommonOverrideAncestor() {
        return atLeast(11, 12, 0);
    }

    /**
     * {@code getEdgeRule}: a rule's key is matched against the spec the edge declares, from npm 11.2.0; before, against
     * the spec with the edge's override applied.
     */
    boolean matchesEdgeRuleByRawSpec() {
        return atLeast(11, 2, 0);
    }

    // @npmcli/arborist shrinkwrap.js

    /**
     * {@code commit}: an extraneous node outside node_modules, a workspace or {@code file:} dependency the manifest no
     * longer names, gets no entry, from npm 11.15.0.
     */
    boolean dropsExtraneousOutsideNodeModules() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code commit}: an extraneous node outside node_modules keeps its entry under another package's directory, from
     * npm 11.17.0.
     */
    boolean keepsNestedExtraneousFsChildren() {
        return atLeast(11, 17, 0);
    }

    /**
     * {@code libc} is one of the package fields an entry keeps, from npm 11.11.0.
     */
    boolean locksLibc() {
        return atLeast(11, 11, 0);
    }

    /**
     * {@code commit}: an inert node without a version, an optional dependency that failed to load, gets no entry, from
     * npm 11.15.0.
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
     * {@code min-release-age}'s flatten: {@code before} wins over it within one source and a falsy age filters nothing,
     * from npm 11.15.0; before, the two exclude each other and any age sets {@code before}.
     */
    boolean yieldsMinReleaseAgeToBefore() {
        return atLeast(11, 15, 0);
    }

    // @npmcli/package-json normalize.js

    /**
     * {@code binDir} is a normalize step that reads the package's directory even for a manifest without a path, as the
     * registry's are, from npm 11.6.1 until 11.6.4 (@npmcli/package-json 7.0.1 and 7.0.2).
     */
    boolean expandsBinDirOfRegistryManifests() {
        return atLeast(11, 6, 1) && !atLeast(11, 6, 4);
    }

    // pacote fetcher.js

    /**
     * {@code canUse}: the {@code EALLOWGIT} message names the spec type as every {@code EALLOW*} does, from npm 11.10.0
     * (pacote 21.3.1).
     */
    boolean namesTypeInAllowRejection() {
        return atLeast(11, 10, 0);
    }

    // semver

    /**
     * The node-semver npm bundles: 7.8.4 from npm 11.17.0 (7.8.2 takes an x-range with a number after a wildcard for no
     * range); 7.8.1 from npm 11.16.0, which strips build metadata from a whole range; 7.7.3 from npm 11.6.2 and 7.7.4
     * from npm 10.9.5, which strip it from a comparator as 7.8.0 does; before, 7.7.2, as every release from 7.5.4 (npm
     * 10.0.0) answers.
     */
    NodeSemver.Release semver() {
        return atLeast(11, 17, 0) ? NodeSemver.Release.V7_8_4 :
                atLeast(11, 16, 0) ? NodeSemver.Release.V7_8_1 :
                        atLeast(11, 6, 2) || atLeast(10, 9, 5) && !atLeast(11, 0, 0) ? NodeSemver.Release.V7_8_0 :
                                NodeSemver.Release.V7_7_2;
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
