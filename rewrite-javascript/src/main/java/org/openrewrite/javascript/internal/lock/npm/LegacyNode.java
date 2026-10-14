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
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Port of the tree setters of @npmcli/arborist lib/node.js (arborist 1.0.14, npm 7.1.0): the {@code parent},
 * {@code fsParent} and {@code root} setters, {@code replaceWith}, and the edge and metadata refreshes they call
 * ({@code [_reloadEdges]}, {@code [_refreshPath]}, {@code [_refreshTopMeta]}, {@code [_delistFromMeta]},
 * {@code [_reloadNamedEdges]}), which arborist 2.0.0 (npm 7.1.1) replaced with a root setter that places a node by its
 * path ({@link Node}, {@link NpmRelease#placesNodesFromRoot()}). A node is placed by its parent or fsParent setter,
 * which reload its edges and the edges of its name below its new parent; the root setter only roots a node and its
 * descendants, and returns before it lists a node that is its own root, so the root is never in its own inventory.
 * {@code target} is read from the field: arborist 1's plain nodes have none.
 */
final class LegacyNode {

    private LegacyNode() {
    }

    /**
     * {@code set root (root)}.
     */
    static void setRoot(Node node, @Nullable Node root) {
        boolean nullRoot = root == null;
        if (root == null) {
            root = node;
        } else {
            // should only ever be 1 step
            while (root.getRoot() != root) {
                root = root.getRoot();
            }
        }

        if (root == node.getRoot()) {
            return;
        }

        delistFromMeta(node);
        node.rootReference(root);
        node.refreshLocation();

        if (node.getTop().getMeta() != null) {
            refreshTopMeta(node);
        }

        if (node.target != null && !nullRoot) {
            node.target.setRoot(root);
        }

        for (Node c : new ArrayList<>(node.getFsChildren())) {
            c.setRoot(root);
        }
        for (Node c : node.getChildren().values()) {
            c.setRoot(root);
        }
    }

    /**
     * {@code set fsParent (fsParent)}.
     */
    static void setFsParent(Node node, @Nullable Node fsParent) {
        if (node.getFsParent() == fsParent) {
            return;
        }

        Node current = node.getFsParent();
        if (current != null) {
            current.getFsChildren().remove(node);
        }

        if (fsParent == null) {
            node.fsParentReference(null);
            // reload ALL edges, since they're now all suspect and likely invalid
            reloadEdges(node, e -> true);
            return;
        }

        if (node.getRelease().ignoresNodeAsOwnParent() &&
            (node == fsParent || Objects.equals(fsParent.getRealpath(), node.getRealpath()))) {
            return;
        }

        // prune off the original location, so we don't leave edges lying around
        if (current != null) {
            node.setFsParent(null);
        }

        String fspp = fsParent.getRealpath();
        String nmPath = PosixPath.resolve(Js.string(fspp), "node_modules", Js.string(node.getName()));
        // actually in the node_modules folder!  this can happen when a link points deep within a node_modules
        // folder, so that the target node is loaded before its parent.
        if (nmPath.equals(node.getPath())) {
            node.fsParentReference(null);
            node.setParent(fsParent);
            return;
        }

        // a pseudo-parent: contained in the parent node's fs tree, but NOT in its node_modules folder
        node.fsParentReference(fsParent);
        fsParent.getFsChildren().add(node);
        // refresh the path BEFORE setting root, so meta gets updated properly
        refreshPath(node, fsParent, current != null ? current.getPath() : null);
        node.setRoot(fsParent.getRoot());
        reloadEdges(node, e -> e.getTo() == null);
    }

    /**
     * {@code [_reloadEdges] (filter)}: called when an fsParent may account for edges that seemed missing.
     */
    static void reloadEdges(Node node, Predicate<Edge> filter) {
        node.clearExplanation();
        for (Edge edge : node.getEdgesOut().values()) {
            if (filter.test(edge)) {
                edge.reload();
            }
        }
        for (Node c : new ArrayList<>(node.getFsChildren())) {
            reloadEdges(c, filter);
        }
        for (Node c : node.getChildren().values()) {
            reloadEdges(c, filter);
        }
    }

    /**
     * {@code replaceWith (node)}: {@code replacement} takes {@code node}'s place, its children and fsChildren with it.
     */
    static void replaceWith(Node node, Node replacement) {
        replacement.setPath(node.getPath());
        replacement.setName(node.getName());
        if (!replacement.isLink()) {
            replacement.setRealpath(node.getPath());
        }
        replacement.setRoot(node.isRoot() ? replacement : node.getRoot());
        // pretend to be in the tree, so top/etc refs are not changing for kids.
        replacement.setParent(null);
        replacement.parentReference(node.getParent());

        // from npm 7.0.6, the children of a node a link replaces stay where they are, since links have none
        if (!replacement.isLink() || !node.getRelease().keepsChildrenOfNodeLinkReplaces()) {
            for (Node c : new ArrayList<>(node.getFsChildren())) {
                c.setFsParent(replacement);
            }
            for (Node c : node.getChildren().values()) {
                c.setParent(replacement);
            }
        }

        // now remove the hidden reference, and call parent setter to finalize.
        replacement.parentReference(null);
        replacement.setParent(node.getParent());
    }

    /**
     * {@code set parent (parent)}: moves a node within a tree or between trees, reloading the edges in and out, the
     * edges of its name under the new parent, and every edge below it.
     */
    static void setParent(Node node, @Nullable Node parent) {
        Node oldParent = node.getParent();

        if (node == parent && node.getRelease().ignoresNodeAsOwnParent()) {
            return;
        }

        // link nodes can't contain children directly; children go under the link target.
        if (parent != null) {
            if (parent.isLink()) {
                parent = parent.target;
            }
            if (oldParent == parent) {
                return;
            }
        }

        // ok now we know something is actually changing, and parent is not a link
        String newPath = parent != null ?
                PosixPath.resolve(Js.string(parent.getPath()), "node_modules", Js.string(node.getName())) :
                node.getPath();
        boolean pathChange = !Objects.equals(newPath, node.getPath());
        Node newTop = parent != null ? parent.getTop() : node;
        boolean topChange = newTop != node.getTop();
        Node newRoot = parent != null ? parent.getRoot() : null;
        boolean rootChange = newRoot != node.getRoot();

        // delist from where this node (and its children) are tracked before the path and root change
        if (node.getPath() != null && (pathChange || topChange || rootChange)) {
            delistFromMeta(node);
            for (Node c : new ArrayList<>(node.getFsChildren())) {
                delistFromMeta(c);
            }
            for (Node c : node.getChildren().values()) {
                delistFromMeta(c);
            }
        }

        // remove from former parent.
        if (oldParent != null) {
            oldParent.getChildren().delete(node.getName());
        }

        // update internal link.  at this point, the node is actually in the new location in the tree, but the paths
        // are not updated yet.
        node.parentReference(parent);

        // remove former child.  calls back into this setter to unlist
        if (parent != null) {
            Node oldChild = parent.getChildren().get(node.getName());
            if (oldChild != null) {
                oldChild.setParent(null);
            }
            parent.getChildren().set(node.getName(), node);
        }

        // the point of no return: this.location is no longer valid
        if (parent != null) {
            refreshPath(node, parent, oldParent != null ? oldParent.getPath() : null);
        }

        // the root setter updates this.location and the root of every child, and of this.target for a link; it is a
        // no-op when the root does not change, so the location and metadata are refreshed by hand
        if (!rootChange) {
            node.refreshLocation();
        } else {
            node.setRoot(newRoot);
        }

        // if the new top is not the root, and it has meta, then we're updating nodes within a link target's folder.
        if (newTop != newRoot && newTop.getMeta() != null) {
            refreshTopMeta(node);
        }

        // refresh dep links; also done when a node leaves the tree, so deduplication is covered.
        for (Edge edge : new ArrayList<>(node.getEdgesIn())) {
            edge.reload();
        }
        for (Edge edge : node.getEdgesOut().values()) {
            edge.reload();
        }

        // in case any of the parent's other descendants were resolving to a different instance of this package
        if (parent != null) {
            reloadNamedEdges(parent, Js.string(node.getName()), true);
        }

        // since loading a parent can add *or change* resolutions, we also walk the tree from this point reloading all
        // edges.
        reloadEdges(node, e -> true);

        // the paths of children and fsChildren have likely changed, and the root may have been set.
        if (!rootChange) {
            for (Node c : node.getChildren().values()) {
                c.refreshLocation();
            }
            for (Node c : new ArrayList<>(node.getFsChildren())) {
                c.refreshLocation();
            }
        }
    }

    /**
     * {@code [_refreshTopMeta]}: lists a node and its children in the metadata of its top, a link target's folder.
     */
    static void refreshTopMeta(Node node) {
        Objects.requireNonNull(node.getTop().getMeta()).add(node);
        for (Node c : node.getChildren().values()) {
            refreshTopMeta(c);
        }
    }

    /**
     * {@code [_delistFromMeta]}: removes a node from the inventory and metadata of its root, and from the metadata of
     * its top where that is another node of the same tree.
     */
    static void delistFromMeta(Node node) {
        Node top = node.getTop();
        Node root = node.getRoot();

        root.getInventory().delete(node);
        if (root.getMeta() != null) {
            root.getMeta().delete(node.getPath());
        }

        // a top and a node of different roots means the parent is changing and the root is not yet set: keep the
        // metadata to apply
        if (top.getMeta() != null && top != root && top.getRoot() == node.getRoot()) {
            top.getMeta().delete(node.getPath());
        }
    }

    /**
     * {@code [_refreshPath] (parent, fromPath)}: the path of a node, and of everything below it, under a new parent.
     */
    static void refreshPath(Node node, Node parent, @Nullable String fromPath) {
        String ppath = Js.string(parent.getPath());
        String relPath = fromPath != null ? PosixPath.relative(fromPath, Js.string(node.getPath())) : null;
        String oldPath = node.getPath();
        String newPath = relPath != null ? PosixPath.resolve(ppath, relPath) :
                parent == node.getParent() ? PosixPath.resolve(ppath, "node_modules", Js.string(node.getName())) :
                        // fsParent initial assignment, nothing to update here
                        oldPath;

        // if no change, nothing to do!
        if (Objects.equals(newPath, oldPath)) {
            return;
        }

        delistFromMeta(node);
        node.setPath(newPath);
        if (!node.isLink()) {
            node.setRealpath(node.getPath());
            for (Node link : node.getLinksIn()) {
                link.setRealpath(newPath);
            }
        }

        node.refreshLocation();
        for (Node c : new ArrayList<>(node.getFsChildren())) {
            refreshPath(c, node, oldPath);
        }
        for (Node c : node.getChildren().values()) {
            refreshPath(c, node, oldPath);
        }
    }

    /**
     * {@code [_reloadNamedEdges] (name, root)}: reloads the edges named {@code name} from this node down, stopping
     * where a reload changes nothing, except below the first node.
     */
    static void reloadNamedEdges(Node node, String name, boolean root) {
        // either it's the node in question, or it's going to block it anyway
        if (name.equals(node.getName()) && !node.isTop()) {
            // reload the edges in so that anything that SHOULD be blocked by this node actually will be.
            for (Edge e : new ArrayList<>(node.getEdgesIn())) {
                e.reload();
            }
            return;
        }

        Edge edge = node.getEdgesOut().get(name);
        // if we don't have an edge, do nothing, but keep descending
        if (edge != null) {
            Node toBefore = edge.getTo();
            edge.reload();
            Node toAfter = edge.getTo();
            if (toBefore == toAfter && !root) {
                // nothing changed: either it already referred to this node, or it is blocked by another node.  but
                // always descend past the first node, since its new child is likely why we are here.
                return;
            }
        }
        for (Node c : node.getChildren().values()) {
            reloadNamedEdges(c, name, false);
        }
        for (Node c : new ArrayList<>(node.getFsChildren())) {
            reloadNamedEdges(c, name, false);
        }
    }
}
