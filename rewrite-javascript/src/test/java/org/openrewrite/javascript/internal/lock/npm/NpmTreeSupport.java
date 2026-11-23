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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builders for package data as arborist's tests write it inline, and a port of
 * {@code lib/tree-check.js}, the invariant check arborist runs after every root change in debug mode.
 */
final class NpmTreeSupport {

    private NpmTreeSupport() {
    }

    /**
     * A JSON object from alternating keys and values, in order.
     */
    static Map<String, Object> obj(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    static List<Object> arr(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    static Node.Options.OptionsBuilder opts() {
        return Node.options();
    }

    static Node node(Node.Options.OptionsBuilder options) {
        return new Node(options.build());
    }

    static Link link(Node.Options.OptionsBuilder options) {
        return new Link(options.build());
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A fixture under npm-arborist/, written from arborist itself by the script of the same name in its
     * generate/ directory.
     */
    static JsonNode fixture(String name) {
        try (InputStream is = NpmTreeSupport.class.getResourceAsStream("/npm-arborist/" + name)) {
            return MAPPER.readTree(name.endsWith(".gz") ? new GZIPInputStream(is) : is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * JSON as the maps and lists package data is made of.
     */
    @SuppressWarnings("unchecked")
    static <T> T json(String json) {
        try {
            return (T) MAPPER.readValue(json, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T json(JsonNode json) {
        return (T) MAPPER.convertValue(json, Object.class);
    }

    /**
     * Node options declared as JSON: path, name, pkg, resolved, integrity and children.
     */
    static Node.Options.OptionsBuilder opts(JsonNode declared) {
        Node.Options.OptionsBuilder options = opts();
        if (declared.has("path")) {
            options.path(declared.get("path").asText());
        }
        if (declared.has("name")) {
            options.name(declared.get("name").asText());
        }
        if (declared.has("pkg")) {
            options.pkg(json(declared.get("pkg")));
        }
        if (declared.has("resolved")) {
            options.resolved(declared.get("resolved").asText());
        }
        if (declared.has("integrity")) {
            options.integrity(declared.get("integrity").asText());
        }
        if (declared.has("children")) {
            for (JsonNode child : declared.get("children")) {
                options.child(opts(child).build());
            }
        }
        return options;
    }

    static Shrinkwrap emptyLock(String path) {
        Shrinkwrap meta = new Shrinkwrap(path, NpmOptions.DEFAULT);
        meta.setData(obj("lockfileVersion", 2, "packages", obj(), "dependencies", obj()));
        return meta;
    }

    private static final JsonNode TREE_DUMPS = fixture("tree-dumps");

    /**
     * What arborist printed at the named point of an upstream test sequence.
     */
    static String treeDump(String snapshot) {
        JsonNode dump = TREE_DUMPS.get(snapshot);
        if (dump == null) {
            throw new IllegalArgumentException("no arborist dump for " + snapshot);
        }
        return dump.asText();
    }

    /**
     * Asserts the tree, and the raw lock data of its metadata when it has any, against arborist's.
     */
    static void assertTree(String snapshot, Node root) {
        assertThat(describe(root)).as(snapshot).isEqualTo(treeDump(snapshot));
        Shrinkwrap meta = root.getMeta();
        if (meta != null) {
            assertThat(meta.getData()).as(snapshot + " / data").isEqualTo(json(treeDump(snapshot + " / data")));
        }
    }

    static void assertGet(String snapshot, Shrinkwrap meta, String location) {
        assertThat(meta.get(location)).as(snapshot + " / get").isEqualTo(json(treeDump(snapshot + " / get")));
    }

    private static String loc(@Nullable Node n) {
        return n == null ? "-" : n.getLocation() == null ? "?" : "\"" + n.getLocation() + "\"";
    }

    private static String flag(boolean b) {
        return b ? "1" : "0";
    }

    /**
     * {@code JSON.stringify} with sorted keys and no indentation, for comparing data of any shape.
     */
    static String canonical(@Nullable Object data) {
        return JsonStringifyNice.stringify(data, Collections.emptyList(), 0);
    }

    static @Nullable Object jsValue(@Nullable Object value) {
        return value == Js.UNDEFINED ? null : value;
    }

    /**
     * Every inventoried node with its relations and edges, in inventory order, as the dump script prints them.
     */
    static String describe(Node root) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Node> entry : root.getInventory().entries()) {
            Node n = entry.getValue();
            out.add("\"" + entry.getKey() + "\" " + n.getClass().getSimpleName() + " name=" + n.getName() +
                    " pkg=" + Js.string(n.getPackageName()) + "@" + Js.string(n.getVersion()) +
                    " path=" + n.getPath() + " realpath=" + n.getRealpath() +
                    " parent=" + loc(n.getParent()) + " fsParent=" + loc(n.getFsParent()) +
                    " target=" + loc(n.getTarget()) + " root=" + loc(n.getRoot()) +
                    " flags=" + flag(n.isDev()) + flag(n.isOptional()) + flag(n.isDevOptional()) + flag(n.isPeer()) +
                      flag(n.isExtraneous()) +
                    " resolved=" + Js.string(n.getResolved()) + " integrity=" + Js.string(n.getIntegrity()));
            for (Map.Entry<String, Node> c : n.getChildren().entries()) {
                out.add("  child " + c.getKey() + " " + loc(c.getValue()));
            }
            for (Node c : n.getFsChildren()) {
                out.add("  fsChild " + loc(c));
            }
            for (Node c : n.getLinksIn()) {
                out.add("  linkIn " + loc(c));
            }
            for (Map.Entry<String, Edge> e : n.getEdgesOut().entries()) {
                out.add("  out " + e.getKey() + " " + e.getValue().getType() + " " + e.getValue().getSpec() +
                        " to=" + loc(e.getValue().getTo()) + " error=" + e.getValue().getError());
            }
            for (Edge e : n.getEdgesIn()) {
                out.add("  in " + loc(e.getFrom()) + " " + e.getType() + " " + e.getName());
            }
        }
        for (Node t : root.getTops()) {
            out.add("top " + loc(t));
        }
        return String.join("\n", out);
    }

    static Node treeCheck(Node tree) {
        Node root = tree.getRoot();
        Inventory inventory = root.getInventory();
        Set<Node> seen = new LinkedHashSet<>();
        check(tree, root, inventory, seen, tree, "self");
        for (Node node : inventory.values()) {
            if (!seen.contains(node) && node != root) {
                throw new AssertionError("unreachable in inventory: " + node.getLocation());
            }
        }
        return tree;
    }

    private static void check(@Nullable Node node, Node root, Inventory inventory, Set<Node> seen, Node via,
                              String viaType) {
        if (node == null || !seen.add(node)) {
            return;
        }
        String at = node.getPath() + " via " + viaType + " of " + via.getPath();
        if (node.isRoot() && node != root) {
            throw new AssertionError("double root: " + at);
        }
        if (node.getRoot() != root) {
            throw new AssertionError("node from other root in tree: " + at);
        }
        if (!node.isRoot() && node.getInventory().size() != 0) {
            throw new AssertionError("non-root has non-zero inventory: " + at);
        }
        if (!node.isRoot() && !inventory.has(node) && !node.isDummy()) {
            throw new AssertionError("not in inventory: " + at);
        }
        if (!node.isTop()) {
            for (Edge edge : node.getEdgesOut().values()) {
                if (edge.isDev()) {
                    throw new AssertionError("dev edges on non-top node: " + at);
                }
            }
        }
        if (Objects.equals(node.getPath(), root.getPath()) && node != root && !root.isLink()) {
            throw new AssertionError("node with same path as root: " + at);
        }
        if (!node.isLink() && !Objects.equals(node.getPath(), node.getRealpath())) {
            throw new AssertionError("non-link with mismatched path/realpath: " + at);
        }
        check(node.getParent(), root, inventory, seen, node, "parent");
        check(node.getFsParent(), root, inventory, seen, node, "fsParent");
        check(node.getTarget(), root, inventory, seen, node, "target");
        for (Node kid : node.getChildren().values()) {
            check(kid, root, inventory, seen, node, "children");
        }
        for (Node kid : new ArrayList<>(node.getFsChildren())) {
            check(kid, root, inventory, seen, node, "fsChildren");
        }
        for (Node link : new ArrayList<>(node.getLinksIn())) {
            check(link, root, inventory, seen, node, "linksIn");
        }
        for (Node top : new ArrayList<>(node.getTops())) {
            check(top, root, inventory, seen, node, "tops");
        }
    }
}
