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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.Js.string;
import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of npm lib/utils/explain-eresolve.js and explain-dep.js (npm 11.17.0), without colors: the text npm
 * prints for an {@code ERESOLVE}.
 */
final class ExplainEresolve {

    private ExplainEresolve() {
    }

    static String explain(Map<String, Object> expl, int depth) {
        Map<String, Object> edge = map(expl.get("edge"));
        Map<String, Object> dep = map(expl.get("dep"));
        Map<String, Object> current = map(expl.get("current"));
        Map<String, Object> peerConflict = map(expl.get("peerConflict"));
        Map<String, Object> currentEdge = map(expl.get("currentEdge"));

        List<String> out = new ArrayList<>();
        Map<String, Object> whileInstalling = dep != null && map(dep.get("whileInstalling")) != null ?
                map(dep.get("whileInstalling")) :
                current != null && map(current.get("whileInstalling")) != null ? map(current.get("whileInstalling")) :
                edge != null && map(edge.get("from")) != null ? map(map(edge.get("from")).get("whileInstalling")) :
                        null;
        if (whileInstalling != null) {
            out.add("While resolving: " + printNode(whileInstalling));
        }

        if (current != null) {
            out.add("Found: " + explainNode(current, depth, new LinkedHashSet<>()));
        } else if (peerConflict != null && map(peerConflict.get("current")) != null) {
            out.add("Found: " + explainNode(map(peerConflict.get("current")), depth, new LinkedHashSet<>()));
        } else if (currentEdge != null) {
            out.add("Found: " + explainEdge(currentEdge, depth, new LinkedHashSet<>()));
        } else if (edge != null) {
            out.add("Found: " + explainEdge(edge, depth, new LinkedHashSet<>()));
        }

        out.add("\nCould not resolve dependency:\n" + explainEdge(edge, depth, new LinkedHashSet<>()));

        if (peerConflict != null) {
            out.add("\nConflicting peer dependency: " +
                    explainNode(map(peerConflict.get("peer")), depth, new LinkedHashSet<>()));
        }
        return String.join("\n", out);
    }

    private static String explainNode(Map<String, Object> node, int depth, Set<String> seen) {
        return printNode(node) + explainDependents(node, depth, seen) + explainLinksIn(node, depth, seen);
    }

    private static String printNode(Map<String, Object> node) {
        String pkgid = string(node.get("name")) + "@" + string(node.get("version"));
        return pkgid + (truthy(node.get("location")) ? "\n" + string(node.get("location")) : "");
    }

    private static String explainLinksIn(Map<String, Object> node, int depth, Set<String> seen) {
        List<Object> linksIn = list(node.get("linksIn"));
        if (linksIn == null || linksIn.isEmpty() || depth <= 0) {
            return "";
        }
        List<String> messages = new ArrayList<>();
        for (Object link : linksIn) {
            messages.add(explainNode(map(link), depth - 1, seen));
        }
        return ("\n" + String.join("\n", messages)).replace("\n", "\n  ");
    }

    private static String explainDependents(Map<String, Object> node, int depth, Set<String> seen) {
        List<Object> dependents = list(node.get("dependents"));
        if (dependents == null || dependents.isEmpty() || depth <= 0) {
            return "";
        }
        int max = (int) Math.ceil(depth / 2.0);
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < Math.min(max, dependents.size()); i++) {
            messages.add(explainEdge(map(dependents.get(i)), depth, seen));
        }
        if (dependents.size() > max) {
            int len = 0;
            int maxLen = 50;
            List<String> showNames = new ArrayList<>();
            for (int i = max; i < dependents.size(); i++) {
                Map<String, Object> from = map(map(dependents.get(i)).get("from"));
                String depName = from != null && truthy(from.get("name")) ? string(from.get("name")) :
                        "the root project";
                len += depName.length();
                if (len >= maxLen && i < dependents.size() - 1) {
                    showNames.add("...");
                    break;
                }
                showNames.add(depName);
            }
            messages.add((dependents.size() - max) + " more (" + String.join(", ", showNames) + ")");
        }
        return ("\n" + String.join("\n", messages)).replace("\n", "\n  ");
    }

    private static String explainEdge(Map<String, Object> edge, int depth, Set<String> seen) {
        String name = string(edge.get("name"));
        String type = string(edge.get("type"));
        String spec = string(edge.get("spec"));
        Map<String, Object> from = map(edge.get("from"));
        String dep = "workspace".equals(type) ?
                Relpath.relpath(string(from == null ? null : from.get("location")), spec.substring("file:".length())) :
                name + "@\"" + spec + "\"";
        if (truthy(edge.get("overridden"))) {
            dep = "overridden " + dep + " (was \"" + string(edge.get("rawSpec")) + "\")";
        }
        String fromMsg = " from " + explainFrom(from, depth, seen);
        return ("prod".equals(type) ? "" : type + " ") + (truthy(edge.get("bundled")) ? "bundled " : "") + dep +
                fromMsg;
    }

    private static String explainFrom(@Nullable Map<String, Object> from, int depth, Set<String> seen) {
        if (from == null || !truthy(from.get("name")) && !truthy(from.get("version"))) {
            return "the root project";
        }
        String nodeId = string(from.get("name")) + "@" + string(from.get("version")) + ":" +
                        string(from.get("location"));
        if (seen.contains(nodeId)) {
            return printNode(from);
        }
        seen.add(nodeId);
        String result = printNode(from) + explainDependents(from, depth - 1, seen) +
                explainLinksIn(from, depth - 1, seen);
        seen.remove(nodeId);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> map(@Nullable Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable List<Object> list(@Nullable Object value) {
        return value instanceof List ? (List<Object>) value : null;
    }
}
