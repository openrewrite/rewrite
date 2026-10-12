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
import org.openrewrite.javascript.internal.lock.BraceExpansion;
import org.openrewrite.javascript.internal.lock.GitHost;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.lock.Ssri;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The npm release an install is reproduced as. The port follows npm 11.17.0; what another release does differently is
 * asked here, one question per upstream change, named for what npm does and answered by the release that changed it.
 * The ported modules read as 11.17.0's source with each other release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version but npm's own version
 * ({@link NpmOptions#getNpmVersion()}), which {@code engines.npm} and {@code devEngines.packageManager} hold npm to.
 */
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

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        return compareTo(new NpmRelease(major, minor, patch)) >= 0;
    }

    // @npmcli/arborist build-ideal-tree.js

    /**
     * The root's {@code packageExtensions} extend the manifests they select ({@code #loadPackageExtensions}, {@code
     * #applyPackageExtension}, package-extensions.js), from npm 12.0.0.
     */
    boolean appliesPackageExtensions() {
        return atLeast(12, 0, 0);
    }

    /**
     * {@code #applyRootOverridesToWorkspaces}: a workspace's edge to a package the root overrides is detached when the
     * root's copy does not satisfy it, from npm 11.3.0 until 11.14.0.
     */
    boolean appliesRootOverridesToWorkspaces() {
        return atLeast(11, 3, 0) && !atLeast(11, 14, 0);
    }

    /**
     * {@code [_problemEdges]} builds what a workspace bundles as any of its dependencies, as it does the project
     * root's, from npm 7.14.0; before, it leaves them to the bundle, as a dependency's.
     */
    boolean buildsWhatWorkspacesBundle() {
        return atLeast(7, 14, 0);
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
     * {@code #fetchManifest} asks pacote for the full packument ({@code fullMetadata: true}): from npm 10.4.0.
     */
    boolean fetchesFullMetadata() {
        return atLeast(10, 4, 0);
    }

    /**
     * {@code #findHoistableNode}: an invalid optional peer edge takes a node already in the tree that satisfies it
     * before the registry is asked, from npm 11.14.0 (npm/cli#9249).
     */
    boolean findsHoistablePeerOptional() {
        return atLeast(11, 14, 0);
    }

    /**
     * {@code #resolveLinks} gives a link's overrides to its target before the target resolves, from npm 11.18.0
     * (npm/cli#9659).
     */
    boolean forwardsLinkOverridesToSubtree() {
        return atLeast(11, 18, 0);
    }

    /**
     * {@code #initTree}: the lock takes the newline and default indent of the root package.json ({@code
     * inferFormattingOptions}), from npm 8.3.2; before, those of the lock it read.
     */
    boolean infersLockFormatFromManifest() {
        return atLeast(8, 3, 2);
    }

    /**
     * {@code [_inflateAncientLockfile]} fetches a node by its version, by {@code file:} of its path where it has
     * neither version nor resolved, and reads a spec from the node's parent directory, from npm 7.18.0; before, by its
     * resolved where it has no version (the latest where it has neither), read from the node's own path.
     */
    boolean inflatesFromParentDirectory() {
        return atLeast(7, 18, 0);
    }

    /**
     * {@code [_inflateAncientLockfile]} fetches a node without a version, or with a {@code file:} resolved, by its
     * resolved, from npm 7.1.1; before, every node by {@code name@version}, read from the working directory.
     */
    boolean inflatesLocalNodesByResolved() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code #nodeFromSpec}: under {@code install-links}, a directory dependency of an installed {@code file:} package
     * is installed from that package's source instead of linked, from npm 11.6.0.
     */
    boolean installsTransitiveFileDependencies() {
        return atLeast(11, 6, 0);
    }

    /**
     * {@code [_placeDep]}: where no target will take the dependency and the install is forced, the node in the way is
     * kept and the conflict warned about, from npm 7.5.4; before, it is npm's {@code ERESOLVE} even forced.
     */
    boolean keepsConflictWhenForced() {
        return atLeast(7, 5, 4);
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
     * {@code [_loadPeerSet]} lets a peer whose current node does not satisfy it conflict where its dependent is not
     * required by the project (it came in only through an optional peer), from npm 7.5.5; before, only where forced
     * or not the project's own.
     */
    boolean letsUnrequiredDependentsConflict() {
        return atLeast(7, 5, 5);
    }

    /**
     * {@code [_loadPeerSet]} lets a missing peer whose parent's edge loads a node that does not satisfy it conflict
     * where that node is not required by the project, from npm 7.5.4; before, only where forced or not the project's
     * own.
     */
    boolean letsUnrequiredPeersConflict() {
        return atLeast(7, 5, 4);
    }

    /**
     * {@code [_nodeFromEdge]}: the link a nesting loop (a1 -> b1 -> a2 -> b2 -> a1) gets to the earlier instance is
     * made under the dependent, or its resolve parent for a peer, from npm 7.1.1; before, under the virtual root the
     * dependency was loaded into.
     */
    boolean linksNestingLoopsUnderDependent() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code #nodeFromSpec}: a {@code file:./} or {@code file:../} dependency inside the project is linked whatever
     * {@code install-links} says, from npm 11.5.0.
     */
    boolean linksProjectInternalFileSpecs() {
        return atLeast(11, 5, 0);
    }

    /**
     * {@code [_loadPeerSet]} loads optional peers into the set too, so that a conflict among them shows, and a peer
     * edge that is valid only because nothing is there, from npm 7.5.0; before, only an invalid edge to a required
     * peer.
     */
    boolean loadsOptionalPeersIntoPeerSets() {
        return atLeast(7, 5, 0);
    }

    /**
     * {@code #loadPeerSet} marks the peer edge whose conflict it lets stand ({@code edge.overridden}, later
     * {@code peerConflicted}), from npm 7.20.3; before, only a placement that warns about one marks it.
     */
    boolean marksPeerConflictsOfLoadedPeerSet() {
        return atLeast(7, 20, 3);
    }

    /**
     * {@code #checkEngineAndPlatform}: an optional package the engine or platform excludes is marked inert with its
     * optional set, from npm 11.6.2.
     */
    boolean marksUninstallableOptionalInert() {
        return atLeast(11, 6, 2);
    }

    /**
     * {@code [_canPlaceDep]}, where the peer set came from: the set in the way is replaced once the target's own edges
     * accept the new one, and its members the new set would leave invalid are taken out of the tree to be built deeper
     * ({@code needNesting}), from npm 7.11.2; before, it is replaced only where no valid edge from the target to one of
     * its members objects.
     */
    boolean nestsReplacedPeerSets() {
        return atLeast(7, 11, 2);
    }

    /**
     * {@code [_nodeFromEdge]}: a nesting loop is not linked to a top, a workspace or link target, from npm 7.5.0;
     * before, only the root is passed over.
     */
    boolean passesOverTopsInNestingLoops() {
        return atLeast(7, 5, 0);
    }

    /**
     * {@code #loadPeerSet} resolves an optional peer only against a version in the tree that does not satisfy it, from
     * npm 11.20.0 and, of npm 12, from 12.1.0; before, every optional peer the virtual root lacks.
     */
    boolean peerSetHoldsOptionalPeerOnlyOnConflict() {
        return atLeast(12, 1, 0) || atLeast(11, 20, 0) && !atLeast(12, 0, 0);
    }

    /**
     * {@code [_virtualRoot]} stands a virtual root at its source's realpath, with a virtual copy of each link target
     * among the source's children, from npm 7.6.3; before, at {@code /virtual-root}, with none.
     */
    boolean placesVirtualRootAtSourcePath() {
        return atLeast(7, 6, 3);
    }

    /**
     * {@code #buildDepStep} places every dependency of a step before it visits any placement, until npm 9.6.6; from
     * then, each placement is visited as soon as it is made.
     */
    boolean placesWholeStepBeforeVisiting() {
        return !atLeast(9, 6, 6);
    }

    /**
     * {@code #buildDepStep} places each dependency with place-dep.js and can-place-dep.js, from npm 7.20.3 (arborist
     * 2.8.0); before, with build-ideal-tree.js's own {@code [_placeDep]}, {@code [_canPlaceDep]} and
     * {@code [_canPlacePeers]} ({@link LegacyPlacement}), whose explanation of a peer conflict also stands in the last
     * one a check found for the node an edge does not resolve to.
     */
    boolean placesWithPlaceDep() {
        return atLeast(7, 20, 3);
    }

    /**
     * {@code [_canPlaceDep]} tries an older node over the one in place for a peer edge, as prefer-dedupe does for any
     * edge, since peers try harder to be singletons, from npm 7.0.8; before, only under prefer-dedupe.
     */
    boolean prefersDedupeForPeers() {
        return atLeast(7, 0, 8);
    }

    /**
     * {@code #nodeFromSpec}: a spec naming a workspace that satisfies the edge takes the workspace instead of a
     * registry manifest, from npm 8.5.0.
     */
    boolean prefersSatisfyingWorkspace() {
        return atLeast(8, 5, 0);
    }

    /**
     * A placement dedupes with {@code [_pruneDedupable]} (node.js {@code canDedupe}): the target's subtree on a KEEP,
     * the node the edge leaves, and each deeper node of the name, from npm 7.11.2; before, nothing on a KEEP, and a
     * node the placed one can replace, unless a nearer node of the name masks it.
     */
    boolean prunesDedupable() {
        return atLeast(7, 11, 2);
    }

    /**
     * {@code [_pruneDedupable]} descends into a node's children in location order, then into those of each node its
     * folder holds outside node_modules, from npm 7.14.0; before, into its children alone, in the order they were
     * added.
     */
    boolean prunesDedupableByLocation() {
        return atLeast(7, 14, 0);
    }

    /**
     * {@code #idealTreePrune}: a node flagged both peer and optional is pruned like an extraneous one, from npm 11.5.0
     * until 11.6.3.
     */
    boolean prunesOptionalPeers() {
        return atLeast(11, 5, 0) && !atLeast(11, 6, 3);
    }

    /**
     * {@code [_placeDep]}: a deeper node of the placed name that it can replace is pruned where the node's parent or
     * grandparent is missing, which masks nothing, from npm 7.1.1; before, reading the missing one fails.
     */
    boolean prunesReplaceableWithoutGrandparent() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code [_pruneForReplacement]}: what the replaced node alone held is pruned, with what only it reaches, unless a
     * valid edge from outside holds it, from npm 7.0.11; before, its direct dependencies alone, any edge from outside,
     * valid or not, holding one.
     */
    boolean prunesReplacedDepsByValidEdges() {
        return atLeast(7, 0, 11);
    }

    /**
     * {@code DepsQueue} keeps one list per depth, each sorted by path alone, in place of one list sorted when it is
     * popped: npm 10.4.0 until 10.8.0.
     */
    boolean queuesDepsByDepth() {
        return atLeast(10, 4, 0) && !atLeast(10, 8, 0);
    }

    /**
     * {@code #initTree}: a node with an edge to nothing is queued like one with an invalid edge, from npm 11.6.2.
     */
    boolean queuesNodesMissingDependencies() {
        return atLeast(11, 6, 2);
    }

    /**
     * {@code #initTree} queues each node of the loaded tree that has an invalid edge, from npm 8.6.0; before, only the
     * root is queued.
     */
    boolean queuesProblemNodesOfLock() {
        return atLeast(8, 6, 0);
    }

    /**
     * {@code [_resolveLinks]} takes a link to point outside the project when its target does not descend from the root,
     * and {@code [_buildDepStep]} hands it only links that have a target, from npm 7.20.0; before, when the link's
     * realpath is outside the project directory, any link.
     */
    boolean readsExternalLinksByTarget() {
        return atLeast(7, 20, 0);
    }

    /**
     * {@code [_inflateAncientLockfile]} calculates the dependency flags again once the old lock's manifests are
     * fetched, from npm 7.5.4; before, it leaves the flags as loaded.
     */
    boolean recalculatesFlagsOfInflatedLock() {
        return atLeast(7, 5, 4);
    }

    /**
     * {@code [_applyUserRequests]} assigns the root its own package again, which detaches and remakes its edges, only
     * for packages added or removed, from npm 7.1.1; before, for every install.
     */
    boolean refreshesRootPackageForAddOrRemoveOnly() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code [_canPlaceDep]}: a node that matches the one in place, as a link to a like target elsewhere does, replaces
     * it where only it satisfies the edge, from npm 7.8.0; before, a match is always kept.
     */
    boolean replacesMatchThatFailsEdge() {
        return atLeast(7, 8, 0);
    }

    /**
     * {@code [_loadPeerSet]}: a peer whose current node does not satisfy it is loaded into the peer set where what the
     * edge would load can replace that node, from npm 7.11.0; before, the current node stands.
     */
    boolean replacesPeerWithReplacement() {
        return atLeast(7, 11, 0);
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
     * {@code [_buildDepStep]} hands {@code [_resolveLinks]} the links among each built node's dependencies, which
     * queues a target not yet built, from npm 7.0.9; before, only the links the build makes.
     */
    boolean resolvesLinksOfDependencies() {
        return atLeast(7, 0, 9);
    }

    /**
     * The root's {@code patchedDependencies} are resolved against the tree once it is built
     * (patched-dependencies.js), from npm 12.0.0.
     */
    boolean resolvesPatchedDependencies() {
        return atLeast(12, 0, 0);
    }

    /**
     * {@code #nodeFromSpec}: a workspace that satisfies the edge is returned for it, from npm 9.6.0; before, the edge's
     * current target.
     */
    boolean returnsSatisfyingWorkspace() {
        return atLeast(9, 6, 0);
    }

    /**
     * {@code #checkEngineAndPlatform}: a node {@code omit} leaves out is not checked, from npm 11.5.0.
     */
    boolean skipsEngineCheckOfOmitted() {
        return atLeast(11, 5, 0);
    }

    /**
     * {@code #inflateAncientLockfile} leaves out a node outside node_modules, a link target the link brings in, from
     * npm 8.6.0; before, it fetches its manifest too, as {@code file:} of its path when it has no version.
     */
    boolean skipsLinkTargetsWhenInflating() {
        return atLeast(8, 6, 0);
    }

    /**
     * {@code #loadPeerSet}: a node that fetching the peer its parent declares replaced is not failed for that peer's
     * conflict, from npm 11.19.0 and, of npm 12, from 12.0.2; before, arborist reads the conflict off the detached
     * node. The guard of npm 11.6.0 ({@link #stopsPeerSetOfDetachedNode()}) stands at the head of the loop, before the
     * next peer is read.
     */
    boolean skipsPeerConflictOfDetachedNode() {
        return atLeast(12, 0, 2) || atLeast(11, 19, 0) && !atLeast(12, 0, 0);
    }

    /**
     * {@code [_placeDep]} passes over an unmet peer of the placed node that its virtual root does not hold, an optional
     * peer {@code [_loadPeerSet]} never loads, from npm 7.0.8, as 7.0.0 does; npm 7.0.1 to 7.0.7, whose one peer loop
     * dropped 7.0.0's check, place the missing peer, a TypeError.
     */
    boolean skipsPeersVirtualRootLacks() {
        return atLeast(7, 0, 8) || !atLeast(7, 0, 1);
    }

    /**
     * {@code [_placeDep]} passes over a place, other than a top, that has a peer edge of the name it places, from npm
     * 7.0.8; before, every place from the start up is checked.
     */
    boolean skipsPlacesWithPeerEdgeOfName() {
        return atLeast(7, 0, 8);
    }

    /**
     * A placement leaves tops out: it prunes none of the name as deduplicable, and a top's edge to what the name
     * resolves to under the target does not hold the dependency back, from npm 7.20.0.
     */
    boolean skipsTopsWhenPlacing() {
        return atLeast(7, 20, 0);
    }

    /**
     * {@code [_resolveLinks]} passes over a link that was never placed in the ideal tree, from npm 7.1.1 (arborist
     * 2.0.0, whose root setter gives a link target its fsParent); before, it takes the node the inventory holds at the
     * link's realpath as the link's target, and gives a target with neither parent nor fsParent the first node of the
     * tree walking up from it as fsParent, and queues it.
     */
    boolean skipsUnplacedLinks() {
        return atLeast(7, 1, 1);
    }

    /**
     * The queue of nodes to build is a plain array, duplicates and all, sorted by depth and path at every step, until
     * npm 9.6.6; from then, {@code DepsQueue} keeps each node once and sorts only after a push.
     */
    boolean sortsDepsQueueAtEveryStep() {
        return !atLeast(9, 6, 6);
    }

    /**
     * {@code #loadPeerSet}: the peers of a node that lost its parent while they loaded are left alone, from npm 11.6.0;
     * before, arborist reads the missing parent.
     */
    boolean stopsPeerSetOfDetachedNode() {
        return atLeast(11, 6, 0);
    }

    /**
     * {@code [_placeDep]}: past a source that peer-depends on the name, the next place up is checked as the peer set's
     * source, its peers too, and whether a conflict there is the project's is read from that place, from npm 7.0.11;
     * before, only the source itself, the conflict the project's where the source is the root or a workspace.
     */
    boolean treatsFirstPlaceAsPeerSetSource() {
        return atLeast(7, 0, 11);
    }

    /**
     * {@code [_loadPeerSet]}: where a peer the set lacks has an edge of its name from the virtual root, a second node
     * by the peer's own spec is loaded and kept where it satisfies both, from npm 7.5.0; before, the root's edge alone
     * decides.
     */
    boolean triesPeerSpecForParentEdge() {
        return atLeast(7, 5, 0);
    }

    /**
     * {@code [_resolveLinks]} finds a link target's fsParent walking up the directories of its realpath, outside the
     * project too, and only then passes over an external link, from npm 7.0.10; before, walking up the segments of its
     * location, the project root last, and an external link is passed over first.
     */
    boolean walksUpRealpathOfLinkTargets() {
        return atLeast(7, 0, 10);
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
     * A link only unsets its target's flags, as an edge does, from npm 11.18.0; before, it assigns its own.
     */
    boolean linkOnlyUnsetsTargetFlags() {
        return atLeast(11, 18, 0);
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

    /**
     * {@code calcDepFlagsStep} unsets each flag a node lacks up its resolve parents that still carry it, and a link's
     * target takes a step of its own, from npm 7.18.0; before, the link's step walks the target's edges.
     */
    boolean resetsFlagsOfParents() {
        return atLeast(7, 18, 0);
    }

    /**
     * The depth walk descends from what a node's step returns, a link's target, from npm 7.5.4; before, from the node
     * itself, so not past a link.
     */
    boolean walksDepsOfLinkTargets() {
        return atLeast(7, 5, 4);
    }

    // @npmcli/arborist can-place-dep.js

    /**
     * {@code checkCanPlace}: a dependency the target's own edge of its name does not accept conflicts only where the
     * target has no node of that name to dedupe against, from npm 8.1.1; before, there too.
     */
    boolean skipsTargetEdgeCheckWithCurrent() {
        return atLeast(8, 1, 1);
    }

    // @npmcli/arborist consistent-resolve.js

    /**
     * A {@code #} in a path that becomes a {@code file:} spec is written {@code %23} (also link.js, load-virtual.js,
     * node.js and shrinkwrap.js), from npm 8.15.1 until 10.9.3 and 11.2.0, whose npm-package-arg escapes a path
     * before the URL parser reads it.
     */
    boolean escapesHashOfFilePaths() {
        return atLeast(8, 15, 1) && !atLeast(10, 9, 3) || atLeast(11, 0, 0) && !atLeast(11, 2, 0);
    }

    /**
     * A hosted git URL that carries credentials resolves to https, from npm 7.4.3; before, every one to ssh.
     */
    boolean keepsHttpsOfHostedGitWithAuth() {
        return atLeast(7, 4, 3);
    }

    /**
     * A hosted git URL written as https resolves to https ({@code hosted.default}), from npm 12.0.0; before, only one
     * that carries credentials, and every other to ssh.
     */
    boolean keepsHostedGitHttps() {
        return atLeast(12, 0, 0);
    }

    /**
     * A resolved whose spec is {@code *}, as npm-package-arg 10 reads a bare name, is returned as written, from npm
     * 9.0.0; before, a bare name, whose spec npm-package-arg 9 reads as empty, unless it ends in {@code @}.
     */
    boolean returnsRawOfStarSpec() {
        return atLeast(9, 0, 0);
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
     * {@code tarballValid} compares a {@code file:} tarball's resolved and spec with their backslashes read as
     * slashes, from npm 7.0.13; before, as written, so that a resolved that is no string matches nothing.
     */
    boolean comparesTarballPathsWithSlashes() {
        return atLeast(7, 0, 13);
    }

    /**
     * {@code linkValid}: a workspace's link stays valid under {@code install-links}, from npm 9.0.0; before, every link
     * is invalid there.
     */
    boolean keepsWorkspaceLinksValid() {
        return atLeast(9, 0, 0);
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
     * {@code error} is computed when it is first read after the edge is made or reloaded, from npm 9.6.6; before, when
     * the edge is made and whenever its target changes or it is reloaded hard ({@code #loadError}).
     */
    boolean computesEdgeErrorWhenRead() {
        return atLeast(9, 6, 6);
    }

    /**
     * {@code spec}: an override whose value is {@code *} leaves the edge's spec as declared, from npm 9.0.0; before,
     * the edge reads {@code *}.
     */
    boolean ignoresStarOverride() {
        return atLeast(9, 0, 0);
    }

    /**
     * {@code satisfiedBy} reads a dependency the root project bundles by the overridden spec, as any other ({@code
     * inDepBundle} in place of {@code inBundle}), from npm 12.0.0.
     */
    boolean overridesWhatTheRootBundles() {
        return atLeast(12, 0, 0);
    }

    /**
     * {@code satisfiedBy} reads a node that is bundled or shrinkwrapped by the spec as declared, from npm 8.12.0;
     * before, through the override as any other.
     */
    boolean readsRawSpecOfBundled() {
        return atLeast(8, 12, 0);
    }

    /**
     * {@code spec}: a {@code $name} override read by an edge of a virtual root resolves against the real root's
     * package, from npm 8.16.0; before, against the virtual root's.
     */
    boolean readsReferenceFromRealRoot() {
        return atLeast(8, 16, 0);
    }

    /**
     * {@code reload} takes the edge's rule from its dependent's overrides again, from npm 8.7.0; before, the edge keeps
     * the overrides it was made with.
     */
    boolean recomputesOverridesOnReload() {
        return atLeast(8, 7, 0);
    }

    /**
     * {@code spec}: a {@code $name} override the root does not declare is read from the edge's own package, from npm
     * 11.3.0.
     */
    boolean resolvesOverrideReferenceInOwnPackage() {
        return atLeast(11, 3, 0);
    }

    // @npmcli/arborist from-path.js

    /**
     * A node's {@code resolved} is read with npm-package-arg only once no override answers, from npm 9.6.6; before,
     * first, so that one npa refuses fails the dependent's edge even where an override answers.
     */
    boolean parsesResolvedAfterOverride() {
        return atLeast(9, 6, 6);
    }

    /**
     * An edge an override replaces resolves its spec from the project root's directory, from npm 8.16.0; before, from
     * the node's own, as any other.
     */
    boolean resolvesOverriddenSpecFromRoot() {
        return atLeast(8, 16, 0);
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

    /**
     * A link forwards its overrides when a rule applies anywhere under its target ({@code overrideMatchesSubtree}),
     * from npm 11.18.0 (npm/cli#9619); before, when a rule names a dependency the target declares.
     */
    boolean linkOverridesMatchSubtree() {
        return atLeast(11, 18, 0);
    }

    /**
     * A link's {@code version} is its target's, which dep-valid.js reads in place of the package's, from npm 7.1.1
     * (arborist 2.0.0); before, the version of the package it took from its target when the target was set, which a
     * later read of the target's package.json leaves as it was.
     */
    boolean readsLinkVersionFromTarget() {
        return atLeast(7, 1, 1);
    }

    /**
     * A link takes the root of its parent, fsParent or target when made, and roots its target with it whenever the
     * target is set, from npm 7.1.1 (arborist 2.0.0); before, a link made without a parent is its own root until one
     * is set, and a target is rooted with the link only while it is its own root.
     */
    boolean rootsTargetWithLink() {
        return atLeast(7, 1, 1);
    }

    /**
     * A link made without a target whose realpath is the root's path takes the root as its target, from npm 7.8.0;
     * before, a new node at that path.
     */
    boolean targetsRootItLinksTo() {
        return atLeast(7, 8, 0);
    }

    // @npmcli/arborist load-virtual.js

    /**
     * {@code [assignBundles]} reads which children a node bundles from the entries' {@code inBundle} only in a lock
     * older than version 2, and only children the node depends on, from npm 7.11.0; before, in a lock of any version,
     * every flagged child.
     */
    boolean assignsBundlesOfOldLocksOnly() {
        return atLeast(7, 11, 0);
    }

    /**
     * {@code [assignBundles]} takes a child flagged {@code inBundle} to be bundled whether or not its parent depends on
     * it, from npm 7.1.1 (arborist 2.0.0) until 7.11.0; before, arborist 1's {@code [assignParentage]} only one its
     * parent depends on.
     */
    boolean bundlesChildrenWithoutEdges() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code #resolveLinks}: a failure to assign a link target's package.json is swallowed like a failure to read it,
     * from npm 11.6.1, where @npmcli/package-json replaces read-package-json-fast.
     */
    boolean guardsLinkTargetPackage() {
        return atLeast(11, 6, 1);
    }

    /**
     * The nodes and links of the lock are placed by the root they are made with, from npm 7.1.1 (arborist 2.0.0);
     * before, arborist 1's {@code [assignParentage]} gives each its parent or fsParent, the first node the lock holds
     * walking up its path, once the links are made and their targets read.
     */
    boolean placesLockNodesByRoot() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code [resolveLinks]} reads each link target's package.json, which its user may have changed, from npm 7.0.9;
     * before, only for a lock older than version 2.
     */
    boolean readsLinkTargetPackages() {
        return atLeast(7, 0, 9);
    }

    /**
     * {@code [resolveLinks]} reads the package.json of a link target that has no parent, outside node_modules, from
     * npm 7.1.1; before, of a target in node_modules too.
     */
    boolean readsPackagesOfTopLinkTargetsOnly() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code #resolveLinks}: a link whose target the lock lacks is {@code EMISSINGTARGET}, from npm 11.4.0; before,
     * arborist reads the missing target.
     */
    boolean rejectsLinkWithoutTarget() {
        return atLeast(11, 4, 0);
    }

    /**
     * {@code [assignParentage]} passes over the root, and a node it finds as its own parent, from npm 7.0.10.
     */
    boolean skipsRootParentage() {
        return atLeast(7, 0, 10);
    }

    /**
     * {@code [loadFromShrinkwrap]} holds the flags of a lock loaded under a root the caller provides, as the ideal
     * tree builder does, suspect: it calculates the flags of the lock's nodes again, that root's left alone, from npm
     * 7.5.4; before, it resets the root's flags and calculates every node's again only where the root's edges and the
     * lock disagree.
     */
    boolean suspectsFlagsUnderProvidedRoot() {
        return atLeast(7, 5, 4);
    }

    // @npmcli/arborist node.js

    /**
     * A node at the filesystem root may go without a name, from npm 7.1.0; before, a node with neither a name nor a
     * folder or package to name it after is a TypeError wherever it is.
     */
    boolean acceptsUnnamedFilesystemRoot() {
        return atLeast(7, 1, 0);
    }

    /**
     * {@code canReplaceWith}: a node of another package name, such as an alias, cannot replace this one, from npm
     * 7.20.3; before, the name the node is installed under is all that is compared.
     */
    boolean comparesPackageNamesToReplace() {
        return atLeast(7, 20, 3);
    }

    /**
     * {@code canDedupe} looks for what can stand in for the node from its resolve parent's resolve parent, from npm
     * 7.14.0; before, from its parent's parent, so a node under a top that lives in another's folder never dedupes.
     */
    boolean dedupesFromResolveParents() {
        return atLeast(7, 14, 0);
    }

    /**
     * {@code isDescendantOf} walks resolve parents, so a node in a top's folder outside node_modules descends from that
     * top, from npm 7.20.0; before, parents only.
     */
    boolean descendsThroughResolveParents() {
        return atLeast(7, 20, 0);
    }

    /**
     * {@code replace}: a link the node replaces takes its target out of the tree, from npm 8.8.0; before, the target
     * stays.
     */
    boolean dropsTargetOfReplacedLink() {
        return atLeast(8, 8, 0);
    }

    /**
     * The {@code parent} setter ignores a node as its own parent, and the {@code fsParent} setter a node as its own
     * fsParent or one at its own realpath, from npm 7.0.10 ({@link LegacyNode}).
     */
    boolean ignoresNodeAsOwnParent() {
        return atLeast(7, 0, 10);
    }

    /**
     * {@code canReplaceWith} leaves out the edges into the node from its own dependencies, which are replaced with it,
     * from npm 7.0.11; before, every edge in must accept the replacement, so a dependency cycle holds a node in place.
     */
    boolean ignoresOwnDepSetToReplace() {
        return atLeast(7, 0, 11);
    }

    /**
     * The root is in its own inventory, so that what walks the inventory, as {@code [_checkEngineAndPlatform]} does,
     * reaches it, from npm 7.1.1 (arborist 2.0.0); before, the root setter returns before it lists the root.
     */
    boolean inventoriesRoot() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code replaceWith} leaves the children and fsChildren of a node a link replaces where they are, from npm 7.0.6
     * ({@link LegacyNode}); before, they move under the link, which puts them under its target.
     */
    boolean keepsChildrenOfNodeLinkReplaces() {
        return atLeast(7, 0, 6);
    }

    /**
     * The {@code root} setter brings along the target a link lost only where the link joins another tree, from npm
     * 7.8.0; before, also where the link is taken out of its tree, which takes the target out with it.
     */
    boolean keepsTargetOfDetachedLink() {
        return atLeast(7, 8, 0);
    }

    /**
     * {@code [_loadDeps]} loads devDependencies before peerDependencies, so that a dev edge is kept over a peer edge
     * of its name, from npm 7.0.4; before, after them.
     */
    boolean loadsDevDepsBeforePeerDeps() {
        return atLeast(7, 0, 4);
    }

    /**
     * {@code [_loadDeps]}: a virtual root loads its source's devDependencies where that source is a top with a path,
     * from npm 7.5.1; between 7.1.1 and then, never ({@link #skipsDevDepsOfVirtualRoots()}).
     */
    boolean loadsDevDepsOfTopSources() {
        return atLeast(7, 5, 1);
    }

    /**
     * {@code [_loadDeps]} loads peer, prod, optional and then dev dependencies, an edge replacing an earlier one of its
     * name that is no workspace's, from npm 7.12.0; before, optional dependencies first, an earlier edge kept.
     */
    boolean loadsPeerDepsFirst() {
        return atLeast(7, 12, 0);
    }

    /**
     * Before npm 7.12.0, {@code [_loadDeps]} loads dependencies last, a prod edge replacing a dev one of its name,
     * from npm 7.5.5; before, right after optionalDependencies.
     */
    boolean loadsProdDepsLast() {
        return atLeast(7, 5, 5);
    }

    /**
     * A node's children and edges are case-insensitive-map.js's, one entry for names alike in NFKD lower case, and an
     * edge is satisfied by a node of its own name only (edge.js), from npm 7.21.0; before, plain {@code Map}s.
     */
    boolean matchesNamesCaseInsensitively() {
        return atLeast(7, 21, 0);
    }

    /**
     * {@code shouldOmit}: only a node under the project root or a workspace is omitted, from npm 11.6.1.
     */
    boolean omitsUnderProjectTopsOnly() {
        return atLeast(11, 6, 1);
    }

    /**
     * The {@code root} setter places a node, taking the parent or fsParent it finds walking up the node's path and the
     * tops below it as children, and the {@code parent} and {@code fsParent} setters change the path and set the root,
     * from npm 7.1.1 (arborist 2.0.0); before, arborist 1's setters ({@link LegacyNode}): the parent and fsParent
     * setters place a node and reload its edges, and the root setter only roots it, never listing the root itself.
     */
    boolean placesNodesFromRoot() {
        return atLeast(7, 1, 1);
    }

    /**
     * An edge passes its override set on to its target, which keeps the most specific of the sets coming in, and an
     * edge is invalid when its set conflicts with its target's, from npm 11.2.0 (also edge.js); before, a node takes
     * its parent's rule for it and the set of the last edge in.
     */
    boolean propagatesOverrideSets() {
        return atLeast(11, 2, 0);
    }

    /**
     * {@code loadOverrides}: the root reads the {@code overrides} of its package.json, from npm 8.3.0; before, npm has
     * no overrides.
     */
    boolean readsOverrides() {
        return atLeast(8, 3, 0);
    }

    /**
     * {@code [_loadDeps]}: a virtual root, a node standing in for another, loads no devDependencies, from npm 7.1.1;
     * before, any top with a path loads them.
     */
    boolean skipsDevDepsOfVirtualRoots() {
        return atLeast(7, 1, 1);
    }

    /**
     * The {@code root} setter gives a node without overrides its parent's rule for it, from npm 8.7.0.
     */
    boolean takesParentOverrideRuleWhenRooted() {
        return atLeast(8, 7, 0);
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
     * A rule whose key names no spec ({@code npa(key).rawSpec === ''}, as npm-package-arg before 10.0.0 reads a bare
     * name) has the key spec {@code ''}, which every edge and node of its name matches, until npm 9.0.0.
     */
    boolean keepsEmptyOverrideKeySpec() {
        return !atLeast(9, 0, 0);
    }

    /**
     * {@code getEdgeRule}: a rule's key is matched against the spec the edge declares, from npm 11.2.0; before, against
     * the spec with the edge's override applied.
     */
    boolean matchesEdgeRuleByRawSpec() {
        return atLeast(11, 2, 0);
    }

    /**
     * {@code getEdgeRule} reads the edge's spec with npm-package-arg: a range or version (a git spec's range) is
     * compared with the rule's key, any other kind of spec takes the rule, from npm 9.3.0; before,
     * {@code semver.intersects(edge.spec, rule.keySpec)}, which throws on a spec that is no range.
     */
    boolean matchesEdgeRuleBySpecType() {
        return atLeast(9, 3, 0);
    }

    // @npmcli/arborist place-dep.js

    /**
     * {@code replaceOldDep}: the peer set a workspace's edge brings in is left in place, with a warning, as the project
     * root's is, from npm 8.1.3; before, only the tree root's.
     */
    boolean leavesWorkspacePeerEntriesAlone() {
        return atLeast(8, 1, 3);
    }

    /**
     * {@code placeInTree}: a dependency that its target or a resolve parent of it already is (a nesting loop) is placed
     * as a link to that one, from npm 7.20.6; before, build-ideal-tree.js's {@code #nodeFromEdge} links it under the
     * dependent (the dependent's resolve parent for a peer) when one above the dependent is the node it loaded.
     */
    boolean linksNestingLoopsWherePlaced() {
        return atLeast(7, 20, 6);
    }

    /**
     * {@code replaceOldDep}: a peer set at the target that the replacing node no longer satisfies is pruned, its
     * dependents evaluated again ({@code needEvaluation}), or its edge warned about when it cannot go deeper, from npm
     * 7.24.2; before, it stays in place with its peer edge invalid.
     */
    boolean prunesConflictedPeerSets() {
        return atLeast(7, 24, 2);
    }

    // @npmcli/arborist reify.js

    /**
     * {@code [_saveIdealTree]} writes the root package.json back on every install, until npm 7.14.0; from then, only
     * where the install adds or removes a dependency, which a lock-only install of the edited manifest never does.
     */
    boolean rewritesRootPackageJson() {
        return !atLeast(7, 14, 0);
    }

    /**
     * {@code [_saveIdealTree]} writes the root package.json through update-root-package-json.js, the file's own
     * fields kept and its dependency fields taken from the package arborist read, from npm 7.0.15; before, it writes
     * that package whole.
     */
    boolean updatesRootPackageJsonDependencies() {
        return atLeast(7, 0, 15);
    }

    // @npmcli/arborist shrinkwrap.js

    /**
     * {@code add}: a node takes the resolved and integrity the lock records at its path only where the lock's version
     * there is its own, from npm 7.15.1; before, whatever version the lock records.
     */
    boolean checksVersionOfLockedEntry() {
        return atLeast(7, 15, 1);
    }

    /**
     * {@code commit} writes an entry for each node of the inventory of the tree's root but the root, from npm 7.1.1
     * (arborist 2.0.0); before, for each node of the tree's own inventory, which never lists the root.
     */
    boolean commitsInventoryOfTreeRoot() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code defaultLockfileVersion}, what a lock is written as when neither the config nor the lock says: 3 from npm
     * 9.0.0; 2 before.
     */
    int defaultLockfileVersion() {
        return atLeast(9, 0, 0) ? 3 : 2;
    }

    /**
     * {@code commit}: an extraneous node outside node_modules, a workspace or {@code file:} dependency the manifest no
     * longer names, gets no entry, from npm 11.15.0.
     */
    boolean dropsExtraneousOutsideNodeModules() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code yarnRegRe} and {@code npmRegRe}, which pick a yarn.lock's registry URLs, read the dots of the registry
     * hosts as dots, from npm 8.5.2; before, as any character.
     */
    boolean escapesRegistryHostDots() {
        return atLeast(8, 5, 2);
    }

    /**
     * {@code load} keeps the version of the lock it reads unless it is 1, which it upgrades to the default, from npm
     * 9.0.0; before, it takes the greater of that version and the default ({@code Math.max}).
     */
    boolean keepsLoadedLockfileVersion() {
        return atLeast(9, 0, 0);
    }

    /**
     * {@code commit}: an extraneous node outside node_modules keeps its entry under another package's directory, from
     * npm 11.17.0.
     */
    boolean keepsNestedExtraneousFsChildren() {
        return atLeast(11, 17, 0);
    }

    /**
     * {@code load} writes the lock as the {@code lockfile-version} option, else as the greater of the version it reads
     * and the default, from npm 8.1.0; before, every lock is written as version 2, with {@code packages} and {@code
     * dependencies}.
     */
    boolean keepsNewerLockfileVersion() {
        return atLeast(8, 1, 0);
    }

    /**
     * {@code libc} is one of the package fields an entry keeps, from npm 11.11.0.
     */
    boolean locksLibc() {
        return atLeast(11, 11, 0);
    }

    /**
     * {@code metaFromNode} writes the package name of a node whose folder is named otherwise even when the node
     * itself goes by that name, as a link target just placed does: from npm 10.8.2.
     */
    boolean namesEntryByFolder() {
        return atLeast(10, 8, 2);
    }

    /**
     * {@code load} reads and writes a project's npm-shrinkwrap.json in place of a package-lock.json beside it, from
     * npm 7.4.2; before, it reads the npm-shrinkwrap.json only where there is no package-lock.json.
     */
    boolean prefersShrinkwrapToLock() {
        return atLeast(7, 4, 2);
    }

    /**
     * An npm-shrinkwrap.json is read, the project's in place of its package-lock.json and a dependency's for the tree
     * below it ({@code hasShrinkwrap}), until npm 12.0.0.
     */
    boolean readsNpmShrinkwrap() {
        return !atLeast(12, 0, 0);
    }

    /**
     * A node's {@code patched}, {@code packageExtensionsApplied} and {@code npmExtensionApplied} and the root's
     * extension hashes are read from the lock (load-virtual.js) and written to it, from npm 12.0.0.
     */
    boolean recordsPatchesAndExtensions() {
        return atLeast(12, 0, 0);
    }

    /**
     * {@code load} refuses a lock whose {@code lockfileVersion} is above 4 ({@code ELOCKFILEVERSION}), from npm
     * 12.0.0.
     */
    boolean refusesNewerLockfile() {
        return atLeast(12, 0, 0);
    }

    /**
     * {@code [_buildLegacyLockfile]}: a {@code file:} spec in an entry's {@code requires} is written relative to the
     * node, from npm 7.1.1; before, as the manifest has it.
     */
    boolean relativizesFileRequires() {
        return atLeast(7, 1, 1);
    }

    /**
     * {@code addEdge}: a registry dependency a yarn.lock entry resolves at the yarn registry is resolved at the npm
     * registry instead, from npm 7.5.6; before, at the yarn registry's URL as the entry writes it.
     */
    boolean rewritesYarnRegistryOfEdgeEntries() {
        return atLeast(7, 5, 6);
    }

    /**
     * {@code commit}: an inert node without a version, an optional dependency that failed to load, gets no entry, from
     * npm 11.15.0.
     */
    boolean skipsInertNodesWithoutVersion() {
        return atLeast(11, 15, 0);
    }

    /**
     * {@code metaFromNode} writes the root's package name in its entry whatever the name of the directory npm runs in,
     * from npm 7.20.6; before, only where the two differ.
     */
    boolean writesRootPackageName() {
        return atLeast(7, 20, 6);
    }

    // @npmcli/arborist update-root-package-json.js

    /**
     * A dependency the root package.json names in both {@code dependencies} and {@code peerDependencies} is written
     * back into {@code dependencies} with the peer's spec, from npm 7.5.5.
     */
    boolean copiesPeerSpecsOverDependencies() {
        return atLeast(7, 5, 5);
    }

    /**
     * {@code dependencies} is written back without what {@code optionalDependencies} names, and left out when that
     * empties it, from npm 7.5.3.
     */
    boolean prunesRepeatedOptionalsOfRootPackageJson() {
        return atLeast(7, 5, 3);
    }

    // @npmcli/arborist version-from-tgz.js

    /**
     * A tarball URL is read with Node's legacy {@code url.parse}, whose protocol may be of either case and whose path
     * keeps its query, escapes a quote and keeps a {@code .} segment, until npm 10.5.0; from then, with WHATWG URL
     * after {@code http:/} or {@code https:/}.
     */
    boolean readsTarballUrlWithUrlParse() {
        return !atLeast(10, 5, 0);
    }

    // @npmcli/arborist yarn-lock.js

    /**
     * A yarn.lock is written with yarn's quoting ({@code quoteIfNeeded}), an entry's fields in yarn's order and its
     * integrity unquoted, from npm 9.0.1; before, every key and value is JSON-quoted and an entry's scalar fields come
     * before its objects, each in {@code localeCompare} order.
     */
    boolean quotesYarnLockAsYarnDoes() {
        return atLeast(9, 0, 1);
    }

    /**
     * {@code parse} reads a line ending in {@code \r\n} without its {@code \r}, from npm 7.0.13; before, the {@code \r}
     * stays on the line, so that a yarn.lock written with CRLF is invalid from its first blank line, and only the
     * entries before it are read.
     */
    boolean readsCrlfYarnLock() {
        return atLeast(7, 0, 13);
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
     * {@code install-links} is a config from npm 8.8.0 (lib/utils/config/definitions.js until npm 9.7.2).
     */
    boolean definesInstallLinks() {
        return atLeast(8, 8, 0);
    }

    /**
     * {@code install-strategy} is a config from npm 9.0.0; before, {@code global-style} and {@code legacy-bundling}
     * are arborist's own options, {@code legacyBundling} read first.
     */
    boolean definesInstallStrategy() {
        return atLeast(9, 0, 0);
    }

    /**
     * {@code install-strategy} takes {@code linked} from npm 9.4.0; before, arborist hoists it.
     */
    boolean definesLinkedInstallStrategy() {
        return atLeast(9, 4, 0);
    }

    /**
     * {@code lockfile-version} is a config from npm 8.1.0 (lib/utils/config/definitions.js).
     */
    boolean definesLockfileVersion() {
        return atLeast(8, 1, 0);
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
     * {@code omit-lockfile-registry-resolved} is a config from npm 8.10.0, when shrinkwrap.js's {@code metaFromNode}
     * begins to ask whether a node is a registry dependency.
     */
    boolean definesOmitLockfileRegistryResolved() {
        return atLeast(8, 10, 0);
    }

    /**
     * {@code prefer-dedupe} is a config from npm 9.7.0.
     */
    boolean definesPreferDedupe() {
        return atLeast(9, 7, 0);
    }

    /**
     * The default of {@code install-links}: {@code true} from npm 9.0.0 until 9.4.2.
     */
    boolean installLinksDefault() {
        return atLeast(9, 0, 0) && !atLeast(9, 4, 2);
    }

    /**
     * {@code location}'s flatten turns {@code global} on when it is {@code global}, from npm 8.11.0; before, it only
     * follows {@code global}.
     */
    boolean setsGlobalFromLocation() {
        return atLeast(8, 11, 0);
    }

    /**
     * {@code lockfile-version}'s type takes the strings {@code 1}, {@code 2} and {@code 3} an npmrc file holds, from
     * npm 8.1.2; before, only the numbers.
     */
    boolean typesLockfileVersionAsStrings() {
        return atLeast(8, 1, 2);
    }

    /**
     * {@code min-release-age}'s flatten: {@code before} wins over it within one source and a falsy age filters nothing,
     * from npm 11.15.0; before, the two exclude each other and any age sets {@code before}.
     */
    boolean yieldsMinReleaseAgeToBefore() {
        return atLeast(11, 15, 0);
    }

    // @npmcli/config index.js

    /**
     * The options npm installs with are flattened from the config as {@code validate} leaves it (nopt's clean drops a
     * value of none of its definition's types and makes a date a Date): before npm 9.0.0, where {@code load}
     * validates, and in 9.1.0 and 9.2.0, whose npm.js sets {@code location} between loading and the command's
     * validation; otherwise npm.js flattens them while it loads, before the command validates.
     */
    boolean flattensValidatedConfig() {
        return !atLeast(9, 0, 0) || atLeast(9, 1, 0) && !atLeast(9, 3, 0);
    }

    /**
     * {@code validate} refuses an auth key outside a registry's scope in an npmrc file ({@code ERR_INVALID_AUTH}), and
     * a {@code registry} that is no URL where it names that scope ({@code ERR_INVALID_URL}), from npm 9.0.0.
     */
    boolean refusesUnscopedAuth() {
        return atLeast(9, 0, 0);
    }

    /**
     * {@code validate} holds {@code email}, {@code certfile} and {@code keyfile} to a registry's scope as it does the
     * other auth keys, from npm 12.0.0.
     */
    boolean refusesUnscopedEmailAndCertificate() {
        return atLeast(12, 0, 0);
    }

    // @npmcli/config parse-field.js

    /**
     * A {@code before} an npmrc file sets reaches pacote as the string it is written as, whose fetchers fail on it
     * ({@code this.before.toISOString is not a function}): npm 9.0.0 and 9.0.1, and 9.3.0 until 9.6.2, whose
     * parseField reads a date into a Date; the other releases flatten the validated config
     * ({@link #flattensValidatedConfig()}).
     */
    boolean keepsNpmrcBeforeAString() {
        return !flattensValidatedConfig() && !atLeast(9, 6, 2);
    }

    // @npmcli/map-workspaces

    /**
     * The @npmcli/map-workspaces npm bundles: 3.0.6 from npm 10.5.2, which finds the members 5.0.3 finds; 3.0.4 and
     * 3.0.3 from npm 9.6.3, which run glob 10 and 9; 3.0.2 and the releases back to 2.0.2 (npm 8.5.4), which find the
     * members alike under glob 7 and 8; 2.0.1 in npm 8.5.3, which refuses a duplicate name as soon as it is found;
     * 2.0.0 from npm 8.1.0 and 1.0.4 from 7.20.6, which also read a backslash as glob's escape; before, 1.0.3, whose
     * duplicate name's message names no paths.
     */
    MapWorkspaces.Release mapWorkspaces() {
        return atLeast(10, 5, 2) ? MapWorkspaces.Release.V5_0_3 :
                atLeast(9, 6, 3) ? MapWorkspaces.Release.V3_0_4 :
                        atLeast(8, 5, 4) ? MapWorkspaces.Release.V2_0_2 :
                                atLeast(8, 5, 3) ? MapWorkspaces.Release.V2_0_1 :
                                        atLeast(7, 20, 6) ? MapWorkspaces.Release.V2_0_0 :
                                                MapWorkspaces.Release.V1_0_3;
    }

    /**
     * A POSIX character class ({@code [[:alpha:]]}) in a workspaces pattern is read as one by glob 9 and its minimatch
     * 7 (map-workspaces 3.0.3), from npm 9.6.3; before, glob 7 and 8's minimatch 3 and 5 read {@code [[:alpha:]} as a
     * bracket expression of those characters followed by {@code ]}.
     */
    boolean readsPosixClassesOfWorkspacePatterns() {
        return atLeast(9, 6, 3);
    }

    // @npmcli/package-json normalize.js

    /**
     * {@code binDir} is a normalize step that reads the package's directory even for a manifest without a path, as the
     * registry's are, from npm 11.6.1 until 11.6.4 (@npmcli/package-json 7.0.1 and 7.0.2).
     */
    boolean expandsBinDirOfRegistryManifests() {
        return atLeast(11, 6, 1) && !atLeast(11, 6, 4);
    }

    /**
     * Normalizing removes an {@code overrides} that is not an object or is empty (the {@code overrides} step of
     * @npmcli/package-json 8.0.0), from npm 12.0.0.
     */
    boolean normalizesOverrides() {
        return atLeast(12, 0, 0);
    }

    /**
     * The @npmcli/package-json whose {@code bin} step pacote runs over a fetched manifest: 6.0.1 from npm 10.9.0,
     * which reads as 7.0.5 there; 5.1.1 and 5.2.0 from npm 10.8.1; 5.1.0 and, before npm 10.6.0,
     * read-package-json-fast before that.
     */
    PackageJson.Release packageJson() {
        return atLeast(10, 9, 0) ? PackageJson.Release.V7_0_5 :
                atLeast(10, 8, 1) ? PackageJson.Release.V5_2_0 : PackageJson.Release.V5_1_0;
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

    // hosted-git-info

    /**
     * The hosted-git-info npm bundles: 9.0.3 from npm 11.15.0, whose https URL of a sourcehut repository ends in no
     * {@code .git}; 9.0.2 and the releases back to 5.2.1 (npm 8.19.3) that read a URL as it does (parse-url.js); 4.1.0
     * from npm 8.3.1, and 5.0.0 and 5.1.0 that answer as it does; 4.0.2 from npm 7.7.0 (4.0.1 at 7.7.0 and 7.7.1
     * answers alike), which knows no sourcehut; 3.0.8 from npm 7.5.0, which reads a URL with Node's {@code url.parse}
     * and an scp-style one with a pattern of its own, so that a GitLab project in a subgroup is no hosted git there,
     * and an scp-style URL whose committish holds a colon is; before, 3.0.7, whose shortcut pattern needs a user and a
     * project ({@code github:foo} is no host there).
     */
    GitHost.Release hostedGitInfo() {
        return atLeast(11, 15, 0) ? GitHost.Release.V9_0_3 :
                atLeast(8, 19, 3) ? GitHost.Release.V9_0_2 :
                        atLeast(8, 3, 1) ? GitHost.Release.V4_1_0 :
                                atLeast(7, 7, 0) ? GitHost.Release.V4_0_2 :
                                        atLeast(7, 5, 0) ? GitHost.Release.V3_0_8 : GitHost.Release.V3_0_7;
    }

    // json-stringify-nice

    /**
     * The json-stringify-nice that writes the lock: 1.1.3 from npm 7.8.0, which sorts as 1.1.4 (npm 7.12.1) does;
     * before, 1.1.1, which takes an empty string or another falsy value for neither a scalar nor an object.
     */
    JsonStringifyNice.Release jsonStringifyNice() {
        return atLeast(7, 8, 0) ? JsonStringifyNice.Release.V1_1_4 : JsonStringifyNice.Release.V1_1_1;
    }

    // npm base-cmd.js

    /**
     * Whether a command checks the root's {@code devEngines} before it runs ({@code checkDevEngines}): from npm 10.9.0.
     */
    boolean checksDevEngines() {
        return atLeast(10, 9, 0);
    }

    /**
     * {@code validateCli} fails the command when {@code strict-npmrc} is set and an npmrc file holds a key the config
     * does not define ({@code EUNKNOWNCONFIG}), from npm 12.0.0; before, {@code strict-npmrc} is such a key itself.
     */
    boolean strictNpmrcRefusesUnknownKeys() {
        return atLeast(12, 0, 0);
    }

    // npm explain-dep.js

    /**
     * {@code explainDependents} names a dependent without a name, the root project, {@code the root project} among
     * those past the first it explains, from npm 7.0.11 (npm/cli#2134); before, it reads that name's length, a
     * TypeError that fails an install as npm logs a peer conflict it warns about.
     */
    boolean namesRootProjectAmongMoreDependents() {
        return atLeast(7, 0, 11);
    }

    // npm explain-eresolve.js

    /**
     * {@code explain} reads the explanation's {@code peerConflict} as the node in the way and the conflicting peer, and
     * finds what is being installed on the dependency or the current node too, from npm 7.20.3; before, as
     * build-ideal-tree.js explains a conflict, {@code peerConflict} is the conflicting peer alone.
     */
    boolean explainsPeerConflictAsCurrentAndPeer() {
        return atLeast(7, 20, 3);
    }

    // npm flat-options.js

    /**
     * lib/utils/flat-options.js hands arborist the config's {@code prefer-dedupe}, which no definition declares and
     * the config keeps as the npmrc file writes it, until npm 7.7.0; from then until 9.7.0 no flatten reads it.
     */
    boolean flattensUndeclaredPreferDedupe() {
        return !atLeast(7, 7, 0);
    }

    // npm-install-checks

    /**
     * {@code checkPlatform} holds a package's {@code libc} to the machine's, which a machine other than linux has none
     * of, from npm 9.6.3 (npm-install-checks 6.1.0); before, only {@code os} and {@code cpu}.
     */
    boolean checksLibc() {
        return atLeast(9, 6, 3);
    }

    /**
     * {@code checkPlatform} finds glibc by the process report's {@code glibcVersionRuntime}, from npm 9.6.5
     * (npm-install-checks 6.1.1); in 9.6.3 and 9.6.4 it reads {@code glibcRuntimeVersion}, which no report has, so a
     * glibc machine has no libc.
     */
    boolean detectsGlibc() {
        return atLeast(9, 6, 5);
    }

    /**
     * Whether a {@code devEngines} failure names the property ({@code Invalid devEngines.runtime}) where earlier
     * releases name the engine alone ({@code Invalid engine "runtime"}): npm-install-checks 7.1.2 (dev-engines.js),
     * which npm 10.9.5 bundles and, of npm 11, the releases from 11.6.1.
     */
    boolean namesDevEnginesProperty() {
        return atLeast(11, 6, 1) || atLeast(10, 9, 5) && !atLeast(11, 0, 0);
    }

    // npm-package-arg

    /**
     * The npm-package-arg npm bundles, with the libraries it calls: 13.0.1 from npm 11.6.2, which takes {@code .tar.gz}
     * and no other character between the two for a tarball's ending; 12.0.2 from npm 11.2.0 and, of npm 10, from
     * 10.9.3, which escapes a file path before the URL parser reads it; 12.0.1 and the 11.x and 12.0.0 that answer
     * nearly as it does from npm 10.0.0; 10.0.0 and 10.1.0 from npm 9.0.0, which refuse a file: URL with a host and
     * need no name of an alias; 9.1.0 from npm 8.14.0, which reads a bare name as the default tag; 9.0.0 to 9.0.2
     * from npm 8.5.2, which read a git committish as a ref or a semver: range alone; 8.1.5 from npm 7.18.0, whose
     * messages name no package; 8.1.4 from npm 7.16.0, which reads a file: spec as a path, its #, ? and % as they
     * are; before, 8.1.0 to 8.1.2, which also trim no registry spec (8.1.0, before npm 7.5.5, also reads a whole
     * scp-style git string as a name and a spec, which no lock asks of it).
     */
    Spec.Options npmPackageArg() {
        return Spec.Options.of(
                atLeast(11, 6, 2) ? Spec.Release.V13_0_2 :
                        atLeast(11, 2, 0) || atLeast(10, 9, 3) && !atLeast(11, 0, 0) ? Spec.Release.V13_0_0 :
                                atLeast(10, 0, 0) ? Spec.Release.V12_0_1 :
                                        atLeast(9, 0, 0) ? Spec.Release.V10_1_0 :
                                                atLeast(8, 14, 0) ? Spec.Release.V9_1_0 :
                                                        atLeast(8, 5, 2) ? Spec.Release.V9_0_0 :
                                                                atLeast(7, 18, 0) ? Spec.Release.V8_1_5 :
                                                                        atLeast(7, 16, 0) ? Spec.Release.V8_1_4 :
                                                                                Spec.Release.V8_1_1,
                validateNpmPackageName(), hostedGitInfo(), semver());
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

    // pacote fetcher.js

    /**
     * {@code canUse}: the {@code EALLOWGIT} message names the spec type as every {@code EALLOW*} does, from npm 11.10.0
     * (pacote 21.3.1).
     */
    boolean namesTypeInAllowRejection() {
        return atLeast(11, 10, 0);
    }

    // pacote registry.js

    /**
     * A registry spec npm-package-arg reads as a tag with no spec (a bare name) is fetched as the configured {@code tag}
     * when that is not {@code latest}, until npm 9.0.0 (pacote 12 and 13, beside npm-package-arg 8 and 9).
     */
    boolean takesDefaultTagForEmptySpec() {
        return !atLeast(9, 0, 0);
    }

    // read-package-json-fast

    /**
     * {@code normalize} lists a manifest's optional dependencies among its dependencies too (1.2.1), until npm 7.5.3;
     * from then (2.0.1) it drops them from {@code dependencies}.
     */
    boolean foldsOptionalDependencies() {
        return !atLeast(7, 5, 3);
    }

    /**
     * A package.json read from disk fills {@code bin} from the files of {@code directories.bin}, from npm 7.20.1
     * (2.0.3); before, it is read without them.
     */
    boolean readsBinDirectoryOfFiles() {
        return atLeast(7, 20, 1);
    }

    /**
     * A package.json read from disk loses its fields whose names start with an underscore, from npm 7.5.6 (2.0.2).
     */
    boolean stripsUnderscoreFieldsOfFiles() {
        return atLeast(7, 5, 6);
    }

    // semver

    /**
     * The node-semver npm bundles: 7.8.5 from npm 11.18.0, whose {@code ~1.2} reaches down to {@code 1.2.0-0} under
     * {@code includePrerelease} (classes/range.js); 7.8.4 from npm 11.17.0 (7.8.2 takes an x-range with a number after
     * a wildcard for no range); 7.8.1 from npm 11.16.0, which strips build metadata from a whole range; 7.7.3 from npm
     * 11.6.2 and 7.7.4 from npm 10.9.5, which strip it from a comparator as 7.8.0 does; 7.7.2, as every release from
     * 7.5.4 (npm 9.8.1) answers; 7.5.2 in npm 9.7.2 to 9.8.0, which reads a loose range's {@code ||} groups untrimmed;
     * before, 7.7.2 again, as 7.3.6 to 7.5.1 (npm 8.7.0 to 9.7.1) answer, and 7.3.5 (npm 7.7.0 to 8.6.0) and 7.3.4
     * (npm 7.1.0 to 7.6.3) but for {@code >=} and a 0, any character, 0, any character, 0, which their {@code GTE0}
     * (dots unescaped) reads as any; 7.3.2 before npm 7.1.0, which also keeps every set and comparator as parsed, so
     * that a prerelease another group admits satisfies a range with a group of any version.
     */
    NodeSemver.Release semver() {
        return atLeast(11, 18, 0) ? NodeSemver.Release.V7_8_5 :
                atLeast(11, 17, 0) ? NodeSemver.Release.V7_8_4 :
                        atLeast(11, 16, 0) ? NodeSemver.Release.V7_8_1 :
                                atLeast(11, 6, 2) || atLeast(10, 9, 5) && !atLeast(11, 0, 0) ?
                                        NodeSemver.Release.V7_8_0 :
                                        atLeast(9, 7, 2) && !atLeast(9, 8, 1) ? NodeSemver.Release.V7_5_2 :
                                                atLeast(7, 1, 0) ? NodeSemver.Release.V7_7_2 :
                                                        NodeSemver.Release.V7_3_2;
    }

    // ssri

    /**
     * The ssri npm bundles: 13.0.1 from npm 11.10.0, which drops a hash whose algorithm Node cannot hash with; before,
     * 13.0.0, which keeps it, as every release from 10.0.5 (npm 10.0.0) answers.
     */
    Ssri.Release ssri() {
        return atLeast(11, 10, 0) ? Ssri.Release.V13_0_1 : Ssri.Release.V13_0_0;
    }

    // validate-npm-package-name

    /**
     * The validate-npm-package-name npm bundles: 7.0.2 from npm 11.8.0, which refuses a name that starts with a hyphen;
     * 6.0.2 from npm 11.5.0 and 10.9.5, which rewords the excluded names, as 7.0.0 does; 6.0.1 from npm 11.4.2 and
     * 10.9.3, which refuses a scoped name whose own part starts with a period; before, 6.0.0.
     */
    ValidateNpmPackageName.Release validateNpmPackageName() {
        return atLeast(11, 8, 0) ? ValidateNpmPackageName.Release.V7_0_2 :
                atLeast(11, 5, 0) || atLeast(10, 9, 5) && !atLeast(11, 0, 0) ?
                        ValidateNpmPackageName.Release.V6_0_2 :
                        atLeast(11, 4, 2) || atLeast(10, 9, 3) && !atLeast(11, 0, 0) ?
                                ValidateNpmPackageName.Release.V6_0_1 : ValidateNpmPackageName.Release.V6_0_0;
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
