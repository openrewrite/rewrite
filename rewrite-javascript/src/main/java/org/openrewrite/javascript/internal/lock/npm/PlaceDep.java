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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.LocaleCompare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.CONFLICT;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.KEEP;
import static org.openrewrite.javascript.internal.lock.npm.DeepestNestingTarget.deepestNestingTarget;
import static org.openrewrite.javascript.internal.lock.npm.GatherDepSet.gatherDepSet;
import static org.openrewrite.javascript.internal.lock.npm.NpmOptions.InstallStrategy.NESTED;
import static org.openrewrite.javascript.internal.lock.npm.NpmOptions.InstallStrategy.SHALLOW;

/**
 * Port of @npmcli/arborist lib/place-dep.js (npm 11.17.0): places a dependency for an edge somewhere up its
 * dependent's ancestry, with the peers it brings, pruning what that replaces or makes redundant.
 */
public final class PlaceDep {

    /**
     * What arborist's {@code explainPeerConflict} builds for an {@code ERESOLVE}: the explanation it logs or
     * throws, and the nodes in it.
     */
    @Value
    public static class PeerConflict {
        Edge edge;

        /**
         * The node being placed; null for the conflict the ideal tree builder raises while loading a peer set.
         */
        @Nullable
        Node dep;

        /**
         * What the edge's name resolves to from its dependent.
         */
        @Nullable
        Node current;

        /**
         * The node in the way of the conflicting peer: {@code peerConflict.current} of the explanation.
         */
        @Nullable
        Node peerCurrent;

        /**
         * The conflicting peer: {@code peerConflict.peer} of the explanation.
         */
        @Nullable
        Node peer;

        Map<String, Object> explanation;
    }

    /**
     * npm's {@code ERESOLVE}: the placement conflicts and is neither forced nor someone else's.
     */
    @Getter
    public static final class CouldNotResolve extends IllegalStateException implements NpmError {
        private final PeerConflict conflict;

        /**
         * The peer conflicts warned about before this one, in this placement or its peers'.
         */
        private final List<PeerConflict> warnings;

        CouldNotResolve(PeerConflict conflict, List<PeerConflict> warnings) {
            this("could not resolve", conflict, warnings);
        }

        CouldNotResolve(String message, PeerConflict conflict, List<PeerConflict> warnings) {
            super(message);
            this.conflict = conflict;
            this.warnings = warnings;
        }

        @Override
        public String getCode() {
            return "ERESOLVE";
        }
    }

    @Getter
    private final Node dep;

    @Getter
    private final Edge edge;

    private final boolean explicitRequest;
    private final NpmOptions options;

    @Getter
    private final @Nullable PlaceDep parent;

    private final @Nullable List<String> updateNames;

    @Getter
    private @Nullable CanPlaceDep canPlace;

    @Getter
    private @Nullable CanPlaceDep canPlaceSelf;

    @Getter
    private final Map<Node, CanPlaceDep> checks = new LinkedHashMap<>();

    @Getter
    private final List<PlaceDep> children = new ArrayList<>();

    @Getter
    private final Set<Node> needEvaluation = new LinkedHashSet<>();

    @Getter
    private @Nullable Node placed;

    @Getter
    private final @Nullable Node current;

    @Getter
    private final String name;

    @Getter
    private final PlaceDep top;

    /**
     * The peer conflicts arborist warns about ("ERESOLVE overriding peer dependency") and continues past, in
     * this placement's top and all of its peer placements, in the order it logs them.
     */
    @Getter
    private final List<PeerConflict> warnings;

    /**
     * A placement of a lock-only install, {@code dep} in a virtual tree whose root holds its peer set as children.
     * @throws CouldNotResolve for npm's {@code ERESOLVE}.
     */
    public PlaceDep(Edge edge, Node dep, NpmOptions options) {
        this(edge, dep, options, false, Collections.emptyList(), null);
    }

    /**
     * {@code dep} in a virtual tree whose root holds its peer set as children; {@code updateNames} what {@code npm
     * update} was given; throws CouldNotResolve for npm's {@code ERESOLVE}.
     */
    public PlaceDep(Edge edge, Node dep, NpmOptions options, boolean explicitRequest, List<String> updateNames) {
        this(edge, dep, options, explicitRequest, updateNames, null);
    }

    private PlaceDep(Edge edge, Node dep, NpmOptions options, boolean explicitRequest,
                     @Nullable List<String> updateNames, @Nullable PlaceDep parent) {
        this.dep = dep;
        this.edge = edge;
        this.explicitRequest = explicitRequest;
        this.options = options;
        this.parent = parent;
        this.updateNames = updateNames;

        this.current = edge.getTo();
        this.name = edge.getName();
        this.top = parent != null ? parent.top : this;
        this.warnings = parent != null ? parent.warnings : new ArrayList<>();

        if (edge.getTo() != null &&
            edge.getError() == null &&
            !explicitRequest &&
            !updateNames.contains(edge.getName())) {
            return;
        }

        Node start = getStartNode();

        for (Node target : start.ancestry()) {
            Edge targetEdge = target.getEdgesOut().get(edge.getName());
            if (!target.isTop() && targetEdge != null && targetEdge.isPeer()) {
                continue;
            }

            CanPlaceDep cpd = new CanPlaceDep(dep, target, edge, options.isPreferDedupe(),
                    parent != null ? parent.canPlace : null, Collections.emptyList(), explicitRequest);
            checks.put(target, cpd);

            if (cpd.getCanPlaceSelf() != CONFLICT) {
                canPlaceSelf = cpd;
            }

            if (cpd.getCanPlace() != CONFLICT) {
                canPlace = cpd;
            } else {
                break;
            }

            if (!dep.getErrors().isEmpty()) {
                break;
            }

            if (options.getInstallStrategy() == NESTED) {
                break;
            }

            if (options.getInstallStrategy() == SHALLOW) {
                Node rp = target.getResolveParent();
                if (rp != null && rp.isProjectRoot()) {
                    break;
                }
            }
        }

        if (canPlace == null) {
            if (!options.isForce() && (isMine() || options.isStrictPeerDeps())) {
                throw failPeerConflict();
            }

            if (canPlaceSelf == null) {
                warnPeerConflict();
                return;
            }

            canPlace = canPlaceSelf;
        }

        Node target = canPlace.getTarget();

        CanPlaceDep.Result placementType = canPlace.getCanPlace() == CONFLICT ?
                canPlace.getCanPlaceSelf() : canPlace.getCanPlace();

        if (placementType == KEEP) {
            if (edge.isPeer() && !edge.isValid()) {
                warnPeerConflict();
            }

            pruneDedupable(target, true);
            return;
        }

        // before npm 7.20.6 the ideal tree builder links a nesting loop when it loads the node
        for (Node p = options.getRelease().linksNestingLoopsWherePlaced() ? target : null; p != null;
             p = p.getResolveParent()) {
            if (p.matches(dep) && !p.isTop()) {
                placed = new Link(Node.options().release(options.getRelease()).parent(target).target(p).build());
                return;
            }
        }

        Node virtualRoot = dep.getParent();
        Node.Options.OptionsBuilder placedOptions = Node.options()
                .release(options.getRelease())
                .name(dep.getName())
                .pkg(dep.getPackage())
                .resolved(dep.getResolved())
                .integrity(dep.getIntegrity())
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .error(dep.getErrors().isEmpty() ? null : dep.getErrors().get(0));
        // npm 12.0.0 carries what a root manifest extension did to the dep, which only it records
        if (Js.truthy(dep.getPackageExtensionsApplied())) {
            placedOptions.packageExtensionsApplied(dep.getPackageExtensionsApplied());
        }
        if (Js.truthy(dep.getNpmExtensionApplied())) {
            placedOptions.npmExtensionApplied(dep.getNpmExtensionApplied());
        }
        if (dep.getOverrides() != null) {
            placedOptions.overrides(dep.getOverrides());
        }
        if (dep.isLink()) {
            placedOptions.target(dep.getTarget()).realpath(dep.getRealpath());
        }
        Node placed = dep instanceof Link ? new Link(placedOptions.build()) : new Node(placedOptions.build());
        this.placed = placed;

        Node oldDep = target.getChildren().get(name);
        if (oldDep != null) {
            replaceOldDep(oldDep, placed);
        } else {
            placed.setParent(target);
        }

        if (edge.isPeer() && !placed.satisfies(edge)) {
            warnPeerConflict();
        }

        if (edge.isValid() && edge.getTo() != null && edge.getTo() != placed) {
            pruneDedupable(edge.getTo(), false);
        }

        // a JavaScript Set or Map iterator skips the entries deleted before it reaches them, and pruning only deletes
        Inventory inventory = target.getRoot().getInventory();
        for (Node node : inventory.query("name", name)) {
            if (!inventory.query("name", name).contains(node)) {
                continue;
            }
            if (node.isDescendantOf(target) && !node.isTop()) {
                pruneDedupable(node, false);
                if (node.getRoot() == target.getRoot()) {
                    for (Node kid : node.getChildren().values()) {
                        if (node.getChildren().get(kid.getName()) != kid) {
                            continue;
                        }
                        pruneDedupable(kid, false);
                    }
                }
            }
        }

        for (Edge peerEdge : placed.getEdgesOut().values()) {
            if (peerEdge.isValid() || !peerEdge.isPeer() || peerEdge.isPeerConflicted()) {
                continue;
            }

            Node peer = virtualRoot.getChildren().get(peerEdge.getName());

            if (peer == null) {
                continue;
            }

            if (!peer.satisfies(peerEdge)) {
                continue;
            }

            // arborist passes this.updateName, which is undefined
            children.add(new PlaceDep(peerEdge, peer, options, explicitRequest, null, this));
        }
    }

    private void replaceOldDep(Node oldDep, Node placed) {
        Node target = oldDep.getParent();

        List<Node> oldDeps = new ArrayList<>();
        for (Map.Entry<String, Edge> entry : oldDep.getEdgesOut().entries()) {
            Edge edge = entry.getValue();
            if (!placed.getEdgesOut().has(entry.getKey()) && edge.getTo() != null) {
                oldDeps.addAll(gatherDepSet(singletonList(edge.getTo()), e -> e.getTo() != edge.getTo()));
            }
        }

        List<Node> prunePeerSets = new ArrayList<>();
        // before npm 7.24.2 the peer sets the replacement no longer satisfies are left as they are
        for (Edge edge : options.getRelease().prunesConflictedPeerSets() ? oldDep.getEdgesIn() :
                Collections.<Edge>emptySet()) {
            if (placed.satisfies(edge) ||
                !edge.isPeer() ||
                edge.getFrom().getParent() != target ||
                edge.isPeerConflicted()) {
                continue;
            }
            for (Edge entryEdge : PeerEntrySets.peerEntrySets(edge.getFrom()).keySet()) {
                Node entryNode = entryEdge.getTo();
                Node deepestTarget = deepestNestingTarget(entryNode, null);
                Node entryFrom = entryEdge.getFrom();
                if (deepestTarget != target &&
                    !(options.getRelease().leavesWorkspacePeerEntriesAlone() ?
                            entryFrom.isProjectRoot() || entryFrom.isWorkspace() : entryFrom.isRoot())) {
                    prunePeerSets.addAll(gatherDepSet(singletonList(entryNode),
                            e -> e.getTo() != entryNode && !e.isPeerConflicted()));
                } else {
                    warnPeerConflict(edge, this.dep);
                }
            }
        }

        placed.replace(oldDep);
        pruneForReplacement(placed, oldDeps);
        for (Node dep : prunePeerSets) {
            for (Edge edge : dep.getEdgesIn()) {
                needEvaluation.add(edge.getFrom());
            }
            dep.setRoot(null);
        }
    }

    private void pruneForReplacement(Node node, List<Node> oldDeps) {
        Set<Node> invalidDeps = new LinkedHashSet<>();
        for (Edge e : node.getEdgesOut().values()) {
            if (e.getTo() != null && !e.isValid()) {
                invalidDeps.add(e.getTo());
            }
        }
        for (Node dep : oldDeps) {
            Set<Node> set = gatherDepSet(singletonList(dep), e -> e.getTo() != dep && e.isValid());
            invalidDeps.addAll(set);
        }

        Set<Node> deps = gatherDepSet(invalidDeps, edge ->
                edge.getFrom() != node && edge.getTo() != node && edge.isValid());

        for (Node dep : deps) {
            dep.setRoot(null);
        }
    }

    private void pruneDedupable(Node node, boolean descend) {
        if (node.canDedupe(options.isPreferDedupe(), explicitRequest)) {
            Set<Node> deps = gatherDepSet(singletonList(node), e -> e.getTo() != node && e.isValid());
            for (Node n : deps) {
                n.setRoot(null);
            }
            return;
        }
        if (descend) {
            Comparator<Node> nodeSort = (a, b) -> LocaleCompare.en(a.getLocation(), b.getLocation());

            List<Node> children = node.getChildren().values();
            children.sort(nodeSort);
            for (Node child : children) {
                pruneDedupable(child, true);
            }
            List<Node> fsChildren = new ArrayList<>(node.getFsChildren());
            fsChildren.sort(nodeSort);
            for (Node topNode : fsChildren) {
                List<Node> kids = topNode.getChildren().values();
                kids.sort(nodeSort);
                for (Node child : kids) {
                    pruneDedupable(child, true);
                }
            }
        }
    }

    public boolean isMine() {
        Edge edge = top.edge;
        Node node = edge.getFrom();

        if (node.isWorkspace() || node.isProjectRoot()) {
            return true;
        }

        if (!edge.isPeer()) {
            return false;
        }

        boolean hasPeerEdges = false;
        for (Edge e : node.getEdgesIn()) {
            if (e.isPeer()) {
                hasPeerEdges = true;
                continue;
            }
            if (e.getFrom().isWorkspace() || e.getFrom().isProjectRoot()) {
                return true;
            }
        }
        if (hasPeerEdges) {
            for (Edge e : PeerEntrySets.peerEntrySets(node).keySet()) {
                if (e.getFrom().isWorkspace() || e.getFrom().isProjectRoot()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void warnPeerConflict() {
        warnPeerConflict(edge, dep);
    }

    private void warnPeerConflict(Edge edge, Node dep) {
        edge.setPeerConflicted(true);
        warnings.add(explainPeerConflict(edge, dep));
    }

    private CouldNotResolve failPeerConflict() {
        return new CouldNotResolve(explainPeerConflict(top.edge, top.dep), warnings);
    }

    private PeerConflict explainPeerConflict(Edge edge, Node dep) {
        Node node = edge.getFrom();
        Node curNode = node.resolve(edge.getName());

        Map<String, Object> expl = new LinkedHashMap<>();
        expl.put("code", "ERESOLVE");
        expl.put("edge", edge.explain());
        expl.put("dep", dep.explain(edge));
        expl.put("force", options.isForce());
        expl.put("isMine", isMine());
        expl.put("strictPeerDeps", options.isStrictPeerDeps());

        Node peerCurrent = null;
        Node peer = null;
        if (parent != null) {
            expl.put("current", curNode != null ? curNode.explain(edge) : null);
            peerCurrent = current;
            expl.put("peerConflict", current != null ? current.explain(this.edge) : null);
        } else {
            expl.put("current", curNode != null ? curNode.explain() : null);
            if (canPlaceSelf != null && canPlaceSelf.getCanPlaceSelf() != CONFLICT) {
                for (CanPlaceDep conflicted : canPlaceSelf.getConflictChildren()) {
                    if (conflicted.getCurrent() != null) {
                        peerCurrent = conflicted.getCurrent();
                        peer = conflicted.getDep();
                        Map<String, Object> peerConflict = new LinkedHashMap<>();
                        peerConflict.put("current", peerCurrent.explain());
                        peerConflict.put("peer", peer.explain(conflicted.getEdge()));
                        expl.put("peerConflict", peerConflict);
                        break;
                    }
                }
            } else {
                peerCurrent = current;
                peer = this.dep;
                Map<String, Object> peerConflict = new LinkedHashMap<>();
                peerConflict.put("current", current != null ? current.explain() : null);
                peerConflict.put("peer", this.dep.explain(this.edge));
                expl.put("peerConflict", peerConflict);
            }
        }

        return new PeerConflict(edge, dep, curNode, peerCurrent, peer, expl);
    }

    private Node getStartNode() {
        Node from = parent != null ? parent.getStartNode() : edge.getFrom();
        return deepestNestingTarget(from, name);
    }

    public List<PlaceDep> getAllChildren() {
        Set<PlaceDep> set = new LinkedHashSet<>(children);
        List<PlaceDep> all = new ArrayList<>(set);
        for (int i = 0; i < all.size(); i++) {
            for (PlaceDep grandchild : all.get(i).children) {
                if (set.add(grandchild)) {
                    all.add(grandchild);
                }
            }
        }
        return all;
    }
}
