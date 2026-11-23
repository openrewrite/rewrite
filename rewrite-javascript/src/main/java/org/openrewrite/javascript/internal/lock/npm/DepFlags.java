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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Port of @npmcli/arborist lib/calc-dep-flags.js and lib/reset-dep-flags.js (npm 11.17.0).
 */
public final class DepFlags {

    private DepFlags() {
    }

    public static void calcDepFlags(Node tree) {
        calcDepFlags(tree, true);
    }

    public static void calcDepFlags(Node tree, boolean resetRoot) {
        if (!tree.getRelease().calculatesDepFlagsByQueue()) {
            calcDepFlagsByWalk(tree, resetRoot);
            return;
        }
        if (resetRoot) {
            tree.unsetDepFlags();
        }

        Set<Node> seen = new LinkedHashSet<>();
        Deque<Node> queue = new ArrayDeque<>();
        queue.push(tree);

        Node node;
        while ((node = queue.poll()) != null) {
            seen.add(node);

            if (!node.isExtraneous()) {
                for (Node n = node.getResolveParent(); n != null && n.isExtraneous(); n = n.getResolveParent()) {
                    n.setExtraneous(false);
                }
            }

            if (node.isLink()) {
                Node target = node.getTarget();
                if (target == null) {
                    continue;
                }
                if (tree.getRelease().linkOnlyUnsetsTargetFlags()) {
                    boolean changed = false;
                    if (target.isDev() && !node.isDev()) {
                        target.setDev(false);
                        changed = true;
                    }
                    if (target.isOptional() && !node.isOptional()) {
                        target.setOptional(false);
                        changed = true;
                    }
                    if (target.isDevOptional() && !node.isDevOptional()) {
                        target.setDevOptional(false);
                        changed = true;
                    }
                    if (target.isPeer() && !node.isPeer()) {
                        target.setPeer(false);
                        changed = true;
                    }
                    if (target.isExtraneous() && !node.isExtraneous()) {
                        target.setExtraneous(false);
                        changed = true;
                    }
                    if (changed || !seen.contains(target)) {
                        queue.push(target);
                    }
                    continue;
                }
                target.setDev(node.isDev());
                target.setOptional(node.isOptional());
                target.setDevOptional(node.isDevOptional());
                target.setPeer(node.isPeer());
                target.setExtraneous(node.isExtraneous());
                queue.push(target);
                continue;
            }

            for (Edge edge : node.getEdgesOut().values()) {
                Node to = edge.getTo();
                if (to == null) {
                    continue;
                }
                boolean peer = edge.isPeer();
                boolean optional = edge.isOptional();
                boolean dev = edge.isDev();

                boolean changed = false;

                if (to.isExtraneous() && !node.isExtraneous() && !(peer && optional)) {
                    to.setExtraneous(false);
                    changed = true;
                }

                if (to.isDev() && !node.isDev() && !dev) {
                    to.setDev(false);
                    changed = true;
                }

                if (to.isOptional() && !node.isOptional() && !optional) {
                    to.setOptional(false);
                    changed = true;
                }

                if (to.isDevOptional() && !node.isDevOptional() && !node.isDev() && !node.isOptional() && !dev &&
                    !optional) {
                    to.setDevOptional(false);
                    changed = true;
                }

                if (to.isPeer() && !node.isPeer() && !peer) {
                    to.setPeer(false);
                    changed = true;
                }

                if (changed) {
                    queue.push(to);
                }
            }
        }

        seen.remove(tree);
        for (Node n : seen) {
            if (n.isDevOptional() && (n.isDev() || n.isOptional())) {
                n.setDevOptional(false);
            }
        }
    }

    private enum Flag {
        EXTRANEOUS, DEV, PEER, DEV_OPTIONAL, OPTIONAL;

        boolean of(Node node) {
            switch (this) {
                case EXTRANEOUS:
                    return node.isExtraneous();
                case DEV:
                    return node.isDev();
                case PEER:
                    return node.isPeer();
                case DEV_OPTIONAL:
                    return node.isDevOptional();
                default:
                    return node.isOptional();
            }
        }

        void unset(Node node) {
            switch (this) {
                case EXTRANEOUS:
                    node.setExtraneous(false);
                    break;
                case DEV:
                    node.setDev(false);
                    break;
                case PEER:
                    node.setPeer(false);
                    break;
                case DEV_OPTIONAL:
                    node.setDevOptional(false);
                    break;
                default:
                    node.setOptional(false);
                    break;
            }
        }
    }

    // calc-dep-flags.js before npm 11.6.3: treeverse's depth walk over the edges out, each node's step unsetting a
    // flag down the dependencies that still carry it
    private static void calcDepFlagsByWalk(Node tree, boolean resetRoot) {
        if (resetRoot) {
            tree.setDev(false);
            tree.setOptional(false);
            tree.setDevOptional(false);
            tree.setPeer(false);
        }
        // depth-descent.js: a stack, so the last of a node's children is visited first; the children are those of
        // what the visit returned, a link's target
        Set<Node> seen = new LinkedHashSet<>();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(tree);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (!seen.add(node)) {
                continue;
            }
            Node visited = calcDepFlagsStep(node);
            // before npm 7.5.4 the children are those of the node visited: a link has none
            Node parent = node.getRelease().walksDepsOfLinkTargets() ? visited : node;
            for (Edge edge : parent.getEdgesOut().values()) {
                if (edge.getTo() != null) {
                    stack.push(edge.getTo());
                }
            }
        }
    }

    private static Node calcDepFlagsStep(Node node) {
        node.setExtraneous(false);
        boolean resetsParents = node.getRelease().resetsFlagsOfParents();
        if (resetsParents) {
            resetParents(node, Flag.EXTRANEOUS);
            resetParents(node, Flag.DEV);
            resetParents(node, Flag.PEER);
            resetParents(node, Flag.DEV_OPTIONAL);
            resetParents(node, Flag.OPTIONAL);
        }

        if (node.isLink()) {
            Node target = node.getTarget();
            if (target == null) {
                if (!node.getRelease().passesOverLinkWithoutTarget()) {
                    throw new IllegalArgumentException("Cannot set properties of null (setting 'dev')");
                }
                return node;
            }
            target.setDev(node.isDev());
            target.setOptional(node.isOptional());
            target.setDevOptional(node.isDevOptional());
            target.setPeer(node.isPeer());
            if (resetsParents) {
                return calcDepFlagsStep(target);
            }
            // before npm 7.18.0 the target's edges are walked in the link's own step
            target.setExtraneous(false);
            node = target;
        }

        boolean marksPeers = node.getRelease().marksPeerEdgeTargetsPeer();
        for (Edge edge : node.getEdgesOut().values()) {
            Node to = edge.getTo();
            if (to == null) {
                continue;
            }
            boolean peer = edge.isPeer();
            boolean optional = edge.isOptional();
            boolean dev = edge.isDev();
            to.setExtraneous(false);

            if (marksPeers) {
                if (peer) {
                    to.setPeer(true);
                } else if (to.isPeer() && !hasIncomingPeerEdge(to)) {
                    unsetFlag(to, Flag.PEER, true);
                }
            }

            boolean unsetDevOpt = !node.isDevOptional() && !node.isDev() && !node.isOptional() && !dev && !optional;
            boolean unsetDev = unsetDevOpt || !node.isDev() && !dev;
            boolean unsetOpt = unsetDevOpt || !node.isOptional() && !optional;

            if (!marksPeers && !node.isPeer() && !peer) {
                unsetFlag(to, Flag.PEER, false);
            }
            if (unsetDevOpt) {
                unsetFlag(to, Flag.DEV_OPTIONAL, marksPeers);
            }
            if (unsetDev) {
                unsetFlag(to, Flag.DEV, marksPeers);
            }
            if (unsetOpt) {
                unsetFlag(to, Flag.OPTIONAL, marksPeers);
            }
        }
        return node;
    }

    private static boolean hasIncomingPeerEdge(Node node) {
        Node target = node.isLink() && node.getTarget() != null ? node.getTarget() : node;
        for (Edge edge : target.getEdgesIn()) {
            if (edge.getType() == Edge.Type.PEER) {
                return true;
            }
        }
        return false;
    }

    private static void resetParents(Node node, Flag flag) {
        if (flag.of(node)) {
            return;
        }
        for (Node p = node; p != null && (p == node || flag.of(p)); p = p.getResolveParent()) {
            flag.unset(p);
        }
    }

    // peerByPeerEdges: from npm 11.6.1 the peer flag is unset down peer edges only; before, down prod edges only
    private static void unsetFlag(Node start, Flag flag, boolean peerByPeerEdges) {
        if (!flag.of(start)) {
            return;
        }
        flag.unset(start);
        Set<Node> seen = new LinkedHashSet<>();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (!seen.add(node)) {
                continue;
            }
            node.setExtraneous(false);
            flag.unset(node);
            Node target = node.isLink() ? node.getTarget() : null;
            if (node.isLink() && target == null && !node.getRelease().passesOverLinkWithoutTarget()) {
                throw new IllegalArgumentException("Cannot set properties of null (setting '" +
                                                   (flag == Flag.DEV_OPTIONAL ? "devOptional" :
                                                           flag.name().toLowerCase(Locale.ROOT)) + "')");
            }
            if (target != null) {
                target.setExtraneous(false);
                flag.unset(target);
            }
            Node targetNode = target != null ? target : node;
            for (Edge edge : targetNode.getEdgesOut().values()) {
                Node to = edge.getTo();
                if (to == null || !flag.of(to)) {
                    continue;
                }
                boolean follow;
                if (flag == Flag.PEER) {
                    follow = peerByPeerEdges ? edge.getType() == Edge.Type.PEER : edge.getType() == Edge.Type.PROD;
                } else {
                    follow = edge.getType() == Edge.Type.PROD || edge.getType() == Edge.Type.PEER;
                }
                if (follow) {
                    stack.push(to);
                }
            }
        }
    }

    public static void resetDepFlags(Node tree) {
        for (Node node : tree.getInventory().values()) {
            node.resetDepFlags();
        }
    }
}
