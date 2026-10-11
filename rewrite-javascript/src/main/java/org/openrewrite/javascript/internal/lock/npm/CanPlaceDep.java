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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.CONFLICT;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.KEEP;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.OK;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.REPLACE;
import static org.openrewrite.javascript.internal.lock.npm.DeepestNestingTarget.deepestNestingTarget;
import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/can-place-dep.js (npm 11.17.0): whether a dependency, with its peer set, can
 * be placed in a target's {@code node_modules}. It only reads the tree.
 */
public final class CanPlaceDep {

    public enum Result {
        CONFLICT, OK, REPLACE, KEEP
    }

    @Getter
    private final Result canPlace;

    @Getter
    private @Nullable Result canPlaceSelf;

    @Getter
    private final Node dep;

    @Getter
    private final Node target;

    @Getter
    private final Edge edge;

    private final boolean explicitRequest;
    private final List<Node> peerPath;

    @Getter
    private final boolean preferDedupe;

    @Getter
    private final @Nullable CanPlaceDep parent;

    @Getter
    private final List<CanPlaceDep> children = new ArrayList<>();

    @Getter
    private final String name;

    @Getter
    private final @Nullable Node current;

    private final @Nullable Edge targetEdge;
    private final boolean edgeOverride;
    private @Nullable Result canPlacePeers;

    /**
     * @param dep a node in a virtual tree whose root holds its peer set as children.
     */
    CanPlaceDep(Node dep, Node target, Edge edge, boolean preferDedupe, boolean explicitRequest) {
        this(dep, target, edge, preferDedupe, null, Collections.emptyList(), explicitRequest);
    }

    CanPlaceDep(Node dep, Node target, Edge edge, boolean preferDedupe, @Nullable CanPlaceDep parent,
                List<Node> peerPath, boolean explicitRequest) {
        this.dep = dep;
        this.target = target;
        this.edge = edge;
        this.explicitRequest = explicitRequest;

        this.peerPath = peerPath;
        this.preferDedupe = preferDedupe || edge.isPeer();
        this.parent = parent;

        this.name = edge.getName();
        this.current = target.getChildren().get(name);
        this.targetEdge = target.getEdgesOut().get(name);

        this.edgeOverride = !dep.satisfies(edge);

        this.canPlace = checkCanPlace();
        if (canPlaceSelf == null) {
            canPlaceSelf = canPlace;
        }
    }

    private Result checkCanPlace() {
        if (!dep.getErrors().isEmpty()) {
            return current != null ? REPLACE : OK;
        }

        if (targetEdge != null && targetEdge.isPeer() && !target.isTop()) {
            return CONFLICT;
        }

        if (current == null && targetEdge != null && !dep.satisfies(targetEdge) && targetEdge != edge) {
            return CONFLICT;
        }

        return current != null ? checkCanPlaceCurrent(current) : checkCanPlaceNoCurrent();
    }

    private Result checkCanPlaceCurrent(Node current) {
        if (dep.matches(current)) {
            if (current.satisfies(edge) || edgeOverride) {
                return explicitRequest ? REPLACE : KEEP;
            }
        }

        Object curVer = current.getVersion();
        Object newVer = dep.getVersion();
        boolean tryReplace = truthy(curVer) && truthy(newVer) &&
                             NodeSemver.compare(newVer, curVer, NodeSemver.Options.DEFAULT) >= 0;
        if (tryReplace && dep.canReplace(current)) {
            Result cpp = canPlacePeers(REPLACE);
            if (cpp != CONFLICT) {
                return cpp;
            }
        }

        if (current.satisfies(edge) && (!explicitRequest || preferDedupe)) {
            return KEEP;
        }

        if (preferDedupe && !tryReplace && dep.canReplace(current)) {
            Result cpp = canPlacePeers(REPLACE);
            if (cpp != CONFLICT) {
                return cpp;
            }
        }

        Node myDeepest = getDeepestNestingTarget();

        if (target != myDeepest) {
            return CONFLICT;
        }

        if (!edge.isPeer() && target == edge.getFrom()) {
            return canPlacePeers(REPLACE);
        }

        if (parent == null && !edge.isPeer()) {
            return CONFLICT;
        }

        boolean canReplace = true;
        for (Map.Entry<Edge, Set<Node>> entry : PeerEntrySets.peerEntrySets(current).entrySet()) {
            Edge entryEdge = entry.getKey();
            if (entryEdge == edge || entryEdge == getPeerEntryEdge()) {
                continue;
            }

            Node entryNode = entryEdge.getTo();
            Node entryRep = dep.getParent().getChildren().get(entryNode.getName());
            if (entryRep != null) {
                if (entryRep.canReplace(entryNode, dep.getParent().getChildren().keys())) {
                    continue;
                }
            }

            boolean canClobber = entryRep == null;
            if (entryRep == null) {
                // iterated as a JavaScript Set, which visits the members added while it runs
                Set<Node> peerReplacementWalk = new LinkedHashSet<>(Collections.singletonList(entryNode));
                List<Node> walk = new ArrayList<>(peerReplacementWalk);
                OUTER:
                for (int i = 0; i < walk.size(); i++) {
                    for (Edge e : walk.get(i).getEdgesOut().values()) {
                        if (!e.isPeer() || !e.isValid()) {
                            continue;
                        }
                        Node rep = dep.getParent().getChildren().get(e.getName());
                        if (rep == null) {
                            if (e.getTo() != null && peerReplacementWalk.add(e.getTo())) {
                                walk.add(e.getTo());
                            }
                            continue;
                        }
                        if (!rep.satisfies(e)) {
                            canClobber = false;
                            break OUTER;
                        }
                    }
                }
            }
            if (canClobber) {
                continue;
            }

            boolean canNestCurrent = true;
            for (Node currentPeer : entry.getValue()) {
                if (!canNestCurrent) {
                    break;
                }

                Node curDeep = deepestNestingTarget(entryEdge.getFrom(), currentPeer.getName());
                if (curDeep == target || target.isDescendantOf(curDeep)) {
                    canNestCurrent = false;
                    canReplace = false;
                }
            }
        }

        if (canReplace) {
            return canPlacePeers(REPLACE);
        }

        return CONFLICT;
    }

    private Result checkCanPlaceNoCurrent() {
        Node current = target != getPeerEntryEdge().getFrom() ? target.resolve(name) : null;
        if (current != null) {
            for (Edge edge : current.getEdgesIn()) {
                if (edge.getFrom().isDescendantOf(target) && edge.isValid()) {
                    if (!dep.satisfies(edge)) {
                        return CONFLICT;
                    }
                }
            }
        }

        return canPlacePeers(OK);
    }

    public Node getDeepestNestingTarget() {
        Node start = parent != null ? parent.getDeepestNestingTarget() : edge.getFrom();
        return deepestNestingTarget(start, name);
    }

    public List<CanPlaceDep> getConflictChildren() {
        List<CanPlaceDep> conflictChildren = new ArrayList<>();
        for (CanPlaceDep c : getAllChildren()) {
            if (c.canPlace == CONFLICT) {
                conflictChildren.add(c);
            }
        }
        return conflictChildren;
    }

    public List<CanPlaceDep> getAllChildren() {
        Set<CanPlaceDep> set = new LinkedHashSet<>(children);
        List<CanPlaceDep> all = new ArrayList<>(set);
        for (int i = 0; i < all.size(); i++) {
            for (CanPlaceDep grandchild : all.get(i).children) {
                if (set.add(grandchild)) {
                    all.add(grandchild);
                }
            }
        }
        return all;
    }

    public CanPlaceDep getTop() {
        return parent != null ? parent.getTop() : this;
    }

    private Result canPlacePeers(Result state) {
        canPlaceSelf = state;
        if (canPlacePeers != null) {
            return canPlacePeers;
        }

        List<Node> peerPath = new ArrayList<>(this.peerPath);
        peerPath.add(dep);
        boolean sawConflict = false;
        for (Edge peerEdge : dep.getEdgesOut().values()) {
            Node peer = peerEdge.getTo();
            if (!peerEdge.isPeer() || peer == null || peerPath.contains(peer)) {
                continue;
            }
            Node target = deepestNestingTarget(this.target, peer.getName());
            CanPlaceDep cpp = new CanPlaceDep(peer, target, peerEdge, true, this, peerPath, false);
            children.add(cpp);

            if (cpp.canPlace == CONFLICT) {
                sawConflict = true;
            }
        }

        canPlacePeers = sawConflict ? CONFLICT : state;
        return canPlacePeers;
    }

    public Edge getPeerEntryEdge() {
        return getTop().edge;
    }

    public String getDescription() {
        return canPlace.name();
    }
}
