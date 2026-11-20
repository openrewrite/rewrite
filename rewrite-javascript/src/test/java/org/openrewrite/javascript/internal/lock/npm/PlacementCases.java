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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * The cases of arborist's test/can-place-dep.js or test/place-dep.js as generate/placement-cases.js records them.
 */
final class PlacementCases {

    /**
     * One case's tree, dependency and edge after its setup.
     */
    static final class Placement {
        final JsonNode declared;
        final JsonNode answer;
        final Node tree;
        final Node dep;
        final Edge edge;
        final @Nullable Node target;

        Placement(JsonNode declared, JsonNode answer, Node tree, Node dep, Edge edge, @Nullable Node target) {
            this.declared = declared;
            this.answer = answer;
            this.tree = tree;
            this.dep = dep;
            this.edge = edge;
            this.target = target;
        }

        String name() {
            return declared.get("name").asText();
        }

        boolean flag(String key) {
            return declared.path(key).asBoolean(false);
        }
    }

    private final JsonNode fixture;
    private final Map<Integer, Node> nodes = new HashMap<>();
    private final Map<Integer, OverrideSet> overrides = new HashMap<>();
    private final Map<Integer, Placement> placements = new HashMap<>();

    PlacementCases(String name) {
        this.fixture = fixture(name);
    }

    /**
     * One test per case run, each first performing the setups that precede it, as tap interleaves them.
     */
    List<DynamicTest> tests(Consumer<Placement> run) {
        List<DynamicTest> tests = new ArrayList<>();
        List<Integer> setups = new ArrayList<>();
        for (JsonNode event : fixture.get("events")) {
            if (event.has("setup")) {
                setups.add(event.get("setup").asInt());
                continue;
            }
            int index = event.get("run").asInt();
            List<Integer> before = new ArrayList<>(setups);
            setups.clear();
            tests.add(dynamicTest(fixture.get("cases").get(index).get("name").asText(), () -> {
                for (int s : before) {
                    setup(s);
                }
                run.accept(placements.get(index));
            }));
        }
        return tests;
    }

    int caseCount() {
        return fixture.get("cases").size();
    }

    /**
     * The names of the cases that declare the flag, in the order they are declared.
     */
    List<String> names(String flag) {
        List<String> names = new ArrayList<>();
        for (JsonNode c : fixture.get("cases")) {
            if (c.path(flag).asBoolean(false)) {
                names.add(c.get("name").asText());
            }
        }
        return names;
    }

    private void setup(int index) {
        JsonNode c = fixture.get("cases").get(index);
        Node tree = node(c.get("tree").asInt());
        Node dep = node(c.get("dep").asInt());
        Node target = c.has("targetLoc") ? tree.getInventory().get(c.get("targetLoc").asText()) : null;
        Node node = tree.getInventory().get(c.get("nodeLoc").asText());
        Edge edge = node.getEdgesOut().get(dep.getName());
        if (!dep.satisfies(edge)) {
            edge.setPeerConflicted(true);
        }
        Node.Options.OptionsBuilder vr = opts()
                .sourceReference(node)
                .path(node.getPath())
                .pkg(new LinkedHashMap<>(node.getPackage()));
        for (JsonNode peer : c.path("peerSet")) {
            vr.child(options(peer).build());
        }
        dep.setParent(new Node(vr.build()));

        for (Node child : dep.getParent().getChildren().values()) {
            for (Edge edgeIn : child.getEdgesIn()) {
                if (edgeIn.isInvalid()) {
                    edgeIn.setPeerConflicted(true);
                }
            }
        }
        placements.put(index, new Placement(c, fixture.get("answers").get(index), tree, dep, edge, target));
    }

    private Node node(int id) {
        Node node = nodes.get(id);
        if (node == null) {
            JsonNode declared = fixture.get("declarations").get("nodes").get(id);
            Node.Options options = options(declared.get("options")).build();
            node = "Link".equals(declared.get("class").asText()) ? new Link(options) : new Node(options);
            nodes.put(id, node);
        }
        return node;
    }

    private Node.Options.OptionsBuilder options(JsonNode declared) {
        Node.Options.OptionsBuilder options = opts();
        for (Iterator<Map.Entry<String, JsonNode>> it = declared.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            JsonNode value = field.getValue();
            switch (field.getKey()) {
                case "path":
                    options.path(value.asText());
                    break;
                case "realpath":
                    options.realpath(value.asText());
                    break;
                case "name":
                    options.name(value.asText());
                    break;
                case "pkg":
                    options.pkg(json(value));
                    break;
                case "resolved":
                    options.resolved(value.asText());
                    break;
                case "integrity":
                    options.integrity(value.asText());
                    break;
                case "error":
                    options.error(new IllegalStateException(value.get("$error").asText()));
                    break;
                case "overrides":
                    options.overrides(overrideSet(value.get("$overrides").asInt()));
                    break;
                case "children":
                    for (JsonNode child : value) {
                        options.child(options(child).build());
                    }
                    break;
                case "fsChildren":
                    for (JsonNode child : value) {
                        options.fsChild(options(child).build());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("no builder for the declared " + field.getKey());
            }
        }
        return options;
    }

    private OverrideSet overrideSet(int id) {
        return overrides.computeIfAbsent(id, i -> {
            JsonNode declared = fixture.get("declarations").get("overrides").get(i);
            return new OverrideSet(json(declared.get("overrides")));
        });
    }

    /**
     * The generator's {@code plain}: enums by name, errors by message, Maps as entry lists, null members left out.
     */
    static @Nullable Object plain(@Nullable Object v) {
        if (v == null || v == Js.UNDEFINED) {
            return null;
        }
        if (v instanceof Enum) {
            return ((Enum<?>) v).name();
        }
        if (v instanceof Throwable) {
            Map<String, Object> error = new TreeMap<>();
            error.put("message", ((Throwable) v).getMessage());
            return error;
        }
        if (v instanceof Map) {
            Map<String, Object> o = new TreeMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                Object value = plain(e.getValue());
                if (value != null) {
                    o.put((String) e.getKey(), value);
                }
            }
            return o;
        }
        if (v instanceof Collection) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object x : (Collection<?>) v) {
                list.add(plain(x));
            }
            return list;
        }
        return v;
    }

    /**
     * {@link NpmTreeSupport#describe} and then every edge marked peerConflicted, as the generator prints a tree.
     */
    static String describe(Node root) {
        StringBuilder out = new StringBuilder(NpmTreeSupport.describe(root));
        for (Map.Entry<String, Node> entry : root.getInventory().entries()) {
            for (Map.Entry<String, Edge> e : entry.getValue().getEdgesOut().entries()) {
                if (e.getValue().isPeerConflicted()) {
                    out.append("\npeerConflicted \"").append(entry.getKey()).append("\" ").append(e.getKey());
                }
            }
        }
        return out.toString();
    }
}
