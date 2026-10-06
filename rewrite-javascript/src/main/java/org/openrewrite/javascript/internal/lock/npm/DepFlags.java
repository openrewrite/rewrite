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

    public static void resetDepFlags(Node tree) {
        for (Node node : tree.getInventory().values()) {
            node.resetDepFlags();
        }
    }
}
