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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.CONFLICT;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.KEEP;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.OK;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.REPLACE;
import static org.openrewrite.javascript.internal.lock.npm.GatherDepSet.gatherDepSet;
import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;
import static org.openrewrite.javascript.internal.lock.npm.NpmOptions.InstallStrategy.NESTED;
import static org.openrewrite.javascript.internal.lock.npm.NpmOptions.InstallStrategy.SHALLOW;
import static org.openrewrite.javascript.internal.lock.npm.PeerSet.peerSet;

/**
 * Port of the placement in @npmcli/arborist lib/arborist/build-ideal-tree.js (npm 7.20.2): {@code [_placeDep]},
 * {@code [_canPlaceDep]}, {@code [_canPlacePeers]}, {@code [_pruneDedupable]}, {@code [_pruneForReplacement]} and the
 * peer conflicts they explain, which npm 7.20.3 replaced with place-dep.js and can-place-dep.js ({@link PlaceDep},
 * {@link CanPlaceDep}). A placement places the peers it brings as soon as it is placed and returns every node it
 * placed; a check answers once for a dependency with its peers, lets a conflicting peer set stand where the set
 * came from, and may take nodes out of the tree, and queue their dependents again, to make room.
 */
final class LegacyPlacement {

    /**
     * {@code this[_peerConflict]}: the last peer a check could not place, and what was in its way, as explained.
     */
    @Value
    private static class LastPeerConflict {
        Map<String, Object> peer;

        @Nullable
        Map<String, Object> current;
    }

    private final Node idealTree;
    private final NpmOptions options;
    private final Consumer<Node> depsQueue;
    private final Set<Node> depsSeen;
    private final Set<Node> loadFailures;
    private final Map<Node, Node> peerSetSource;
    private final Map<Node, Node> virtualRoots;
    private final List<PlaceDep.PeerConflict> warnings;

    private @Nullable LastPeerConflict peerConflict;

    /**
     * The ideal tree builder's state the placement reads and changes: its queue of nodes to build (a push), the nodes
     * it built, the nodes that failed to load, the node that brought each peer set in, the virtual roots it keeps by
     * the node they stand in for, and the peer conflicts it warned about.
     */
    LegacyPlacement(Node idealTree, NpmOptions options, Consumer<Node> depsQueue, Set<Node> depsSeen,
                    Set<Node> loadFailures, Map<Node, Node> peerSetSource, Map<Node, Node> virtualRoots,
                    List<PlaceDep.PeerConflict> warnings) {
        this.idealTree = idealTree;
        this.options = options;
        this.depsQueue = depsQueue;
        this.depsSeen = depsSeen;
        this.loadFailures = loadFailures;
        this.peerSetSource = peerSetSource;
        this.virtualRoots = virtualRoots;
        this.warnings = warnings;
    }

    /**
     * {@code [_placeDep]}: places {@code dep}, a node of a virtual tree whose root holds its peer set, for {@code edge}
     * of {@code node}, as high up the dependent's ancestry as it can go, then the peers it brings.
     *
     * @param peerEntryEdge the edge that brought the peer set in, for a peer of it; null for the dependency itself.
     * @return every node placed, the dependency's first and then its peers', or none where what is there stays.
     * @throws PlaceDep.CouldNotResolve for npm's {@code ERESOLVE}.
     */
    List<Node> placeDep(Node dep, Node node, Edge edge, @Nullable Edge peerEntryEdge, List<Node> peerPath) {
        // a lock-only install requests nothing explicitly, names nothing to update and fixes no vulnerability
        if (edge.getTo() != null && edge.getError() == null) {
            return new ArrayList<>();
        }

        Node start = edge.isPeer() && !node.isProjectRoot() ?
                (node.getResolveParent() != null ? node.getResolveParent() : node) : node;

        Node target = null;
        CanPlaceDep.Result canPlace = null;
        boolean isSource = false;
        Node source = peerSetSource.get(dep);
        for (Node check = start; check != null; check = check.getResolveParent()) {
            if (check == source) {
                isSource = true;
            }

            Edge checkEdge = check.getEdgesOut().get(edge.getName());
            if (options.getRelease().skipsPlacesWithPeerEdgeOfName() && !check.isTop() && checkEdge != null &&
                checkEdge.isPeer()) {
                continue;
            }

            // before npm 7.0.11 only the source itself is checked as the source
            CanPlaceDep.Result cp = canPlaceDep(dep, check, edge, peerEntryEdge, peerPath,
                    options.getRelease().treatsFirstPlaceAsPeerSetSource() && isSource);
            isSource = false;

            if (cp != CONFLICT) {
                canPlace = cp;
                target = check;
            } else {
                break;
            }

            // legacyBundling
            if (options.getInstallStrategy() == NESTED) {
                break;
            }

            // globalStyle
            if (options.getInstallStrategy() == SHALLOW && check.getResolveParent() == idealTree.getTarget()) {
                break;
            }
        }

        if (target == null) {
            if (options.isForce() && options.getRelease().keepsConflictWhenForced()) {
                Node to = edge.getTo();
                if (to == null) {
                    throw new IllegalArgumentException("Cannot read properties of null (reading 'resolveParent')");
                }
                target = to.getResolveParent();
                canPlace = KEEP;
            } else {
                throw failPeerConflict(edge, null);
            }
        } else {
            peerConflict = null;
        }

        // this.log.silly('placeDep', target.location || 'ROOT', ...) reads the target's location
        if (target == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'location')");
        }

        if (canPlace == KEEP) {
            if (edge.isPeer()) {
                Node kept = target.getChildren().get(edge.getName());
                if (kept == null) {
                    throw new IllegalArgumentException("Cannot read properties of undefined (reading 'satisfies')");
                }
                if (!kept.satisfies(edge)) {
                    warnPeerConflict(edge);
                }
            }

            if (options.getRelease().prunesDedupable()) {
                pruneDedupable(target, true);
            }
            return new ArrayList<>();
        }

        Node virtualRoot = dep.getParent();

        Node.Options.OptionsBuilder newDepOptions = Node.options()
                .release(options.getRelease())
                .name(dep.getName())
                .pkg(dep.getPackage())
                .resolved(dep.getResolved())
                .integrity(dep.getIntegrity())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .error(dep.getErrors().isEmpty() ? null : dep.getErrors().get(0));
        if (dep.isLink()) {
            Node depTarget = dep.getTarget();
            if (depTarget == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'path')");
            }
            newDepOptions.target(depTarget).realpath(depTarget.getPath());
        }
        Node newDep = dep instanceof Link ? new Link(newDepOptions.build()) : new Node(newDepOptions.build());
        if (loadFailures.contains(dep)) {
            loadFailures.add(newDep);
        }

        List<Node> placed = new ArrayList<>();
        placed.add(newDep);
        Node oldChild = target.getChildren().get(edge.getName());
        if (oldChild != null) {
            List<Node> oldDeps = new ArrayList<>();
            for (Map.Entry<String, Edge> entry : oldChild.getEdgesOut().entries()) {
                Edge e = entry.getValue();
                if (!newDep.getEdgesOut().has(entry.getKey()) && e.getTo() != null) {
                    Node oldDep = e.getTo();
                    if (options.getRelease().prunesReplacedDepsByValidEdges()) {
                        oldDeps.addAll(gatherDepSet(singletonList(oldDep), x -> x.getTo() != oldDep));
                    } else {
                        // before npm 7.0.11 the dropped dependency alone
                        oldDeps.add(oldDep);
                    }
                }
            }
            newDep.replace(oldChild);
            pruneForReplacement(newDep, oldDeps);
            for (Edge edgeIn : new ArrayList<>(newDep.getEdgesIn())) {
                if (edgeIn.isInvalid() && edgeIn != edge) {
                    depsQueue.accept(edgeIn.getFrom());
                    depsSeen.remove(edgeIn.getFrom());
                }
            }
        } else {
            newDep.setParent(target);
        }

        if (edge.isPeer() && !newDep.satisfies(edge)) {
            warnPeerConflict(edge);
        }

        boolean prunesDedupable = options.getRelease().prunesDedupable();
        if (prunesDedupable && edge.isValid() && edge.getTo() != null && edge.getTo() != newDep) {
            pruneDedupable(edge.getTo(), false);
        } else if (!prunesDedupable && edge.isValid()) {
            // before npm 7.11.2 the node the edge leaves goes where the placed one can replace it
            Node to = edge.getTo();
            if (to == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'parent')");
            }
            if (to.getParent() != target && newDep.canReplace(to)) {
                to.setParent(null);
            }
        }

        for (Edge edgeIn : new ArrayList<>(newDep.getEdgesIn())) {
            // arborist asks whether it built the dependent of the edge it placed for, not of this one
            if (edgeIn != edge && !edgeIn.isValid() && !depsSeen.contains(edge.getFrom())) {
                depsQueue.accept(edgeIn.getFrom());
            }
        }

        // a JavaScript Set iterator skips the members deleted before it reaches them, and pruning only deletes
        Inventory inventory = idealTree.getInventory();
        for (Node n : inventory.query("name", newDep.getName())) {
            if (!inventory.query("name", newDep.getName()).contains(n)) {
                continue;
            }
            if (!prunesDedupable) {
                pruneReplaceable(n, newDep, target);
            } else if ((!n.isTop() || !options.getRelease().skipsTopsWhenPlacing()) && n.isDescendantOf(target)) {
                pruneDedupable(n, false);
            }
        }

        for (Edge peerEdge : newDep.getEdgesOut().values()) {
            if (virtualRoot == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'children')");
            }
            Node peer = virtualRoot.getChildren().get(peerEdge.getName());

            if (!peerEdge.isPeer() || peerEdge.isValid()) {
                continue;
            }
            if (peer == null) {
                if (!options.getRelease().skipsPeersVirtualRootLacks()) {
                    // before npm 7.0.8 the missing peer is placed: [_canPlaceDep] reads it at the first place up
                    Node peerStart = !newDep.isRoot() && newDep.getResolveParent() != null ?
                            newDep.getResolveParent() : newDep;
                    Edge entryEdge = peerEntryEdge != null ? peerEntryEdge : edge;
                    throw new IllegalArgumentException("Cannot read properties of undefined (reading '" +
                                                       readOfMissingDep(peerStart, peerEdge.getName(),
                                                               entryEdge.getFrom()) + "')");
                }
                continue;
            }

            placed.addAll(placeDep(peer, newDep, peerEdge, peerEntryEdge != null ? peerEntryEdge : edge, peerPath));
        }

        if (virtualRoot == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'sourceReference')");
        }
        virtualRoots.remove(virtualRoot.getSourceReference());

        return placed;
    }

    // before npm 7.11.2: a node of the placed name deeper in the target that the placed one can replace goes, unless
    // a nearer node of the name masks it that it cannot be replaced with
    private static void pruneReplaceable(Node node, Node newDep, Node target) {
        if (node != newDep &&
            node.isDescendantOf(target) &&
            !node.isInShrinkwrap() &&
            !node.isInBundle() &&
            node.canReplaceWith(newDep)) {
            Node parent = node.getParent();
            Node mask;
            if (!node.getRelease().prunesReplaceableWithoutGrandparent() && parent != target) {
                // before npm 7.1.1 a missing parent or grandparent is read
                if (parent == null) {
                    throw new IllegalArgumentException("Cannot read properties of null (reading 'parent')");
                }
                if (parent.getParent() == null) {
                    throw new IllegalArgumentException("Cannot read properties of undefined (reading 'resolve')");
                }
            }
            mask = parent != target && parent != null && parent.getParent() != null &&
                   parent.getParent() != target ? parent.getParent().resolve(newDep.getName()) : null;

            if (mask == null || mask == newDep || node.canReplaceWith(mask)) {
                node.setParent(null);
            }
        }
    }

    // what [_canPlaceDep] (npm 7.0.7) reads first of a dep that is undefined, checking target: dep.matches for the node in
    // place, dep.name where the target is not the entry edge's dependent, else [_canPlacePeers]'s dep.parent
    private static String readOfMissingDep(Node target, String name, @Nullable Node entryFrom) {
        return target.getChildren().has(name) ? "matches" : target != entryFrom ? "name" : "parent";
    }

    private void pruneDedupable(Node node, boolean descend) {
        if (node.canDedupe(options.isPreferDedupe())) {
            node.setRoot(null);
            return;
        }
        if (descend && !options.getRelease().prunesDedupableByLocation()) {
            // for (const child of node.children.values()): a JavaScript Map iterator skips the children deleted
            // before it reaches them
            for (Node child : node.getChildren().values()) {
                if (node.getChildren().get(child.getName()) == child) {
                    pruneDedupable(child, true);
                }
            }
        } else if (descend) {
            Comparator<Node> nodeSort = (a, b) -> LocaleCompare.en(a.getLocation(), b.getLocation());

            List<Node> children = node.getChildren().values();
            children.sort(nodeSort);
            List<Node> fsChildren = new ArrayList<>(node.getFsChildren());
            fsChildren.sort(nodeSort);
            for (Node child : children) {
                pruneDedupable(child, true);
            }
            for (Node topNode : fsChildren) {
                List<Node> kids = topNode.getChildren().values();
                kids.sort(nodeSort);
                for (Node child : kids) {
                    pruneDedupable(child, true);
                }
            }
        }
    }

    private void pruneForReplacement(Node node, List<Node> oldDeps) {
        Set<Node> invalidDeps = new LinkedHashSet<>();
        for (Edge e : node.getEdgesOut().values()) {
            if (e.getTo() != null && !e.isValid()) {
                invalidDeps.add(e.getTo());
            }
        }
        // before npm 7.0.11 each old dependency alone, and any edge from outside, valid or not, holds a node
        boolean byValidEdges = options.getRelease().prunesReplacedDepsByValidEdges();
        for (Node dep : oldDeps) {
            if (byValidEdges) {
                invalidDeps.addAll(gatherDepSet(singletonList(dep), e -> e.getTo() != dep && e.isValid()));
            } else {
                invalidDeps.add(dep);
            }
        }

        Set<Node> deps = gatherDepSet(invalidDeps, edge ->
                edge.getFrom() != node && edge.getTo() != node && (!byValidEdges || edge.isValid()));

        for (Node dep : deps) {
            dep.setParent(null);
        }
    }

    /**
     * {@code [_canPlaceDep]}: whether {@code dep}, with its peers, can go in {@code target}'s node_modules for
     * {@code edge}: OK where nothing is there, REPLACE to clobber what is, KEEP where what is there will do, CONFLICT
     * where it may not go.
     */
    private CanPlaceDep.Result canPlaceDep(Node dep, Node target, Edge edge, @Nullable Edge peerEntryEdge,
                                           List<Node> peerPath, boolean isSource) {
        Edge entryEdge = peerEntryEdge != null ? peerEntryEdge : edge;
        Node source = peerSetSource.get(dep);

        isSource = isSource || target == source;
        // const { isProjectRoot, isWorkspace } = isSource ? target : source || {}
        Node effectiveSource = isSource ? target : source;
        boolean isMine = effectiveSource != null && (effectiveSource.isProjectRoot() || effectiveSource.isWorkspace());

        if (target.getChildren().has(edge.getName())) {
            Node current = target.getChildren().get(edge.getName());

            if (dep.matches(current)) {
                // before npm 7.8.0 a match is kept whether or not it satisfies the edge
                if (!options.getRelease().replacesMatchThatFailsEdge() || current.satisfies(edge) ||
                    !dep.satisfies(edge)) {
                    return KEEP;
                }
            }

            Object curVer = current.getVersion();
            Object newVer = dep.getVersion();
            boolean tryReplace = truthy(curVer) && truthy(newVer) &&
                                 NodeSemver.compare(newVer, curVer, NodeSemver.Options.DEFAULT) >= 0;
            if (tryReplace && dep.canReplace(current)) {
                CanPlaceDep.Result res = canPlacePeers(dep, target, edge, REPLACE, peerEntryEdge, peerPath, isSource);
                if (res != CONFLICT) {
                    return res;
                }
            }

            if (edge.satisfiedBy(current)) {
                return KEEP;
            }

            // before npm 7.0.8 only under prefer-dedupe
            boolean preferDedupe = options.isPreferDedupe() ||
                                   edge.isPeer() && options.getRelease().prefersDedupeForPeers();
            if (preferDedupe && !tryReplace && dep.canReplace(current)) {
                CanPlaceDep.Result res = canPlacePeers(dep, target, edge, REPLACE, peerEntryEdge, peerPath, isSource);
                if (res != CONFLICT) {
                    return res;
                }
            }

            if (isSource) {
                boolean canReplace = true;
                if (peerEntryEdge != null && !options.getRelease().nestsReplacedPeerSets()) {
                    // before npm 7.11.2: no valid edge from the target to the set in the way may object
                    Set<Node> peerSet = peerSet(current);
                    OUTER:
                    for (Node p : peerSet) {
                        for (Edge e : p.getEdgesIn()) {
                            if (peerSet.contains(e.getFrom())) {
                                continue;
                            }
                            if (e.getFrom() == target && e.isValid()) {
                                Node rep = virtualChildren(dep).get(e.getName());
                                boolean override = false;
                                for (Edge in : rep == null ? new LinkedHashSet<Edge>() : rep.getEdgesIn()) {
                                    if (!in.isValid()) {
                                        override = true;
                                        break;
                                    }
                                }
                                if (rep == null || !rep.satisfies(e) || override) {
                                    canReplace = false;
                                    break OUTER;
                                }
                            }
                        }
                    }
                } else if (peerEntryEdge != null) {
                    Set<Node> currentPeerSet = peerSet(current);

                    Set<Edge> targetEdges = new LinkedHashSet<>();
                    for (Node p : currentPeerSet) {
                        for (Edge e : p.getEdgesIn()) {
                            if (currentPeerSet.contains(e.getFrom())) {
                                continue;
                            }
                            if (e.getFrom() != target || !e.isValid()) {
                                continue;
                            }
                            targetEdges.add(e);
                        }
                    }

                    for (Edge e : targetEdges) {
                        Node rep = virtualChildren(dep).get(e.getName());
                        Node targetCurrent = e.getTo();
                        if (rep == null) {
                            for (Edge curEdge : targetCurrent.getEdgesOut().values()) {
                                Node newRepDep = virtualChildren(dep).get(curEdge.getName());
                                if (curEdge.isValid() && newRepDep != null && !newRepDep.satisfies(curEdge)) {
                                    canReplace = false;
                                    break;
                                }
                            }
                            continue;
                        }

                        boolean override = false;
                        for (Edge in : rep.getEdgesIn()) {
                            if (!in.isValid()) {
                                override = true;
                                break;
                            }
                        }
                        if (rep.satisfies(e) && !override) {
                            continue;
                        }
                        canReplace = false;
                        break;
                    }

                    if (canReplace) {
                        boolean needNesting = false;
                        OUTER:
                        for (Node n : currentPeerSet) {
                            Node rep = virtualChildren(dep).get(n.getName());
                            if (rep != null) {
                                continue;
                            }

                            for (Edge e : n.getEdgesOut().values()) {
                                Node repDep = virtualChildren(dep).get(e.getName());
                                if (repDep == null) {
                                    continue;
                                }

                                if (repDep.satisfies(e)) {
                                    continue;
                                }

                                needNesting = true;
                                break OUTER;
                            }
                        }

                        if (needNesting) {
                            Set<Node> dependants = new LinkedHashSet<>();
                            Set<Node> reresolve = new LinkedHashSet<>();
                            NEXT:
                            for (Node n : currentPeerSet) {
                                Node rep = virtualChildren(dep).get(n.getName());
                                if (rep != null) {
                                    continue;
                                }
                                Set<Node> deps = new LinkedHashSet<>();
                                for (Edge e : n.getEdgesIn()) {
                                    if (e.getFrom() == target) {
                                        continue NEXT;
                                    }
                                    if (currentPeerSet.contains(e.getFrom())) {
                                        continue;
                                    }
                                    deps.add(e.getFrom());
                                }
                                reresolve.add(n);
                                dependants.addAll(deps);
                            }
                            for (Node dependant : dependants) {
                                depsQueue.accept(dependant);
                                depsSeen.remove(dependant);
                            }
                            for (Node n : reresolve) {
                                n.setRoot(null);
                            }
                        }
                    }
                }

                if (canReplace) {
                    CanPlaceDep.Result ret = canPlacePeers(dep, target, edge, REPLACE, peerEntryEdge, peerPath,
                            isSource);
                    if (ret != CONFLICT) {
                        return ret;
                    }
                }

                if (!options.isStrictPeerDeps() && !isMine || options.isForce()) {
                    warnPeerConflict(edge);
                    return KEEP;
                }
            }

            return CONFLICT;
        }

        if (target != entryEdge.getFrom() && target.getEdgesOut().has(dep.getName())) {
            Edge targetEdge = target.getEdgesOut().get(dep.getName());
            if (!targetEdge.satisfiedBy(dep)) {
                return CONFLICT;
            }
        }

        Node current = target != entryEdge.getFrom() ? target.resolve(dep.getName()) : null;
        if (current != null) {
            // before npm 7.20.0 a top's edges hold the dependency too
            boolean skipsTops = options.getRelease().skipsTopsWhenPlacing();
            for (Edge e : current.getEdgesIn()) {
                if ((!skipsTops || !e.getFrom().isTop()) && e.getFrom().isDescendantOf(target) && e.isValid()) {
                    if (!e.satisfiedBy(dep)) {
                        return CONFLICT;
                    }
                }
            }
        }

        return canPlacePeers(dep, target, edge, OK, peerEntryEdge, peerPath, isSource);
    }

    // dep.parent.children: the peer set the dependency was loaded with
    private static CaseInsensitiveMap<Node> virtualChildren(Node dep) {
        Node parent = dep.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'children')");
        }
        return parent.getChildren();
    }

    /**
     * {@code [_canPlacePeers]}: {@code ret} where every peer of {@code dep} can go in the target too, else CONFLICT.
     */
    private CanPlaceDep.Result canPlacePeers(Node dep, Node target, Edge edge, CanPlaceDep.Result ret,
                                             @Nullable Edge peerEntryEdge, List<Node> peerPath, boolean isSource) {
        if (dep.getParent() == null || peerEntryEdge != null && peerPath.contains(dep)) {
            return ret;
        }

        Edge entryEdge = peerEntryEdge != null ? peerEntryEdge : edge;
        List<Node> path = new ArrayList<>(peerPath);
        path.add(dep);

        for (Edge peerEdge : dep.getEdgesOut().values()) {
            if (!peerEdge.isPeer() || peerEdge.getTo() == null) {
                continue;
            }
            Node peer = peerEdge.getTo();
            CanPlaceDep.Result canPlacePeer = canPlaceDep(peer, target, peerEdge, entryEdge, path,
                    options.getRelease().treatsFirstPlaceAsPeerSetSource() && isSource);
            if (canPlacePeer != CONFLICT) {
                continue;
            }

            Node current = target.resolve(peer.getName());
            peerConflict = new LastPeerConflict(peer.explain(peerEdge), current != null ? current.explain() : null);
            return CONFLICT;
        }
        return ret;
    }

    /**
     * {@code [_failPeerConflict]}: npm's {@code ERESOLVE}, for a placement or for a peer set the ideal tree builder
     * loads.
     */
    PlaceDep.CouldNotResolve failPeerConflict(Edge edge, @Nullable Edge currentEdge) {
        return new PlaceDep.CouldNotResolve("unable to resolve dependency tree", explainPeerConflict(edge, currentEdge),
                warnings);
    }

    private PlaceDep.PeerConflict explainPeerConflict(Edge edge, @Nullable Edge currentEdge) {
        Node node = edge.getFrom();
        if (node == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'resolve')");
        }
        Node curNode = node.resolve(edge.getName());
        LastPeerConflict pc = peerConflict;
        Map<String, Object> current = curNode != null ? curNode.explain() : pc != null ? pc.getCurrent() : null;
        Map<String, Object> expl = new LinkedHashMap<>();
        expl.put("code", "ERESOLVE");
        expl.put("current", current);
        expl.put("currentEdge", currentEdge != null ? currentEdge.explain() : null);
        expl.put("edge", edge.explain());
        expl.put("peerConflict", pc != null ? pc.getPeer() : null);
        expl.put("strictPeerDeps", options.isStrictPeerDeps());
        expl.put("force", options.isForce());
        return new PlaceDep.PeerConflict(edge, null, curNode, null, null, expl);
    }

    // [_warnPeerConflict]: marks the edge, which is then never built again (this[_edgesOverridden]), and warns
    private void warnPeerConflict(Edge edge) {
        edge.setPeerConflicted(true);
        PlaceDep.PeerConflict conflict = explainPeerConflict(edge, null);
        if (!options.getRelease().namesRootProjectAmongMoreDependents()) {
            // npm's log.warn renders an ERESOLVE warning's explanation at depth 2 (setup-log.js), which before npm
            // 7.0.11 fails on the root project among the dependents past the first
            ExplainEresolve.explain(conflict.getExplanation(), 2, options.getRelease());
        }
        warnings.add(conflict);
    }
}
