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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Parity with arborist over package.json values of unexpected JSON types and keys naming integers or
 * {@code Object.prototype} members ({@code npm-arborist/generate/package-shapes.js}).
 */
class PackageShapesTest {

    static Stream<Arguments> cases() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : fixture("package-shapes")) {
            cases.add(Arguments.of(c.get("json").asText(), c.get("legacyPeerDeps").asBoolean(), c.get("expected"),
                    c.has("throws") ? c.get("throws").asText() : null));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void packageShape(String packageJson, boolean legacyPeerDeps, @Nullable JsonNode expected,
                      @Nullable String message) {
        if (message != null) {
            assertThatThrownBy(() -> describe(packageJson, legacyPeerDeps)).hasMessage(message);
            return;
        }
        assertThat(describe(packageJson, legacyPeerDeps)).isEqualTo(NpmTreeSupport.<Map<String, Object>>json(expected));
    }

    private static Map<String, Object> describe(String packageJson, boolean legacyPeerDeps) {
        Node root = node(opts().path("/r").pkg(json(packageJson)).loadOverrides(true).legacyPeerDeps(legacyPeerDeps));
        Node d = node(opts().parent(root).name("d")
                .pkg(obj("name", "d", "version", "1.0.0", "dependencies", obj("a", "1.x", "b", "^1"))));
        Node a = node(opts().parent(root).name("a")
                .pkg(obj("name", "a", "version", "1.0.0", "dependencies", obj("b", "1"))));

        List<Object> edges = new ArrayList<>();
        List<Object> nodes = new ArrayList<>();
        for (Node node : new Node[]{root, d, a}) {
            nodes.add(rule(node.getOverrides()));
            for (Edge edge : node.getEdgesOut().values()) {
                edges.add(Arrays.asList(node.getName(), edge.getName(), edge.getType().toString(),
                        attempt(edge::getSpec), edge.getRawSpec(), edge.getAccept(), rule(edge.getOverrides()),
                        attempt(edge::getError), edge.getTo() == null ? null : edge.getTo().getLocation()));
            }
        }
        Map<String, Object> described = new LinkedHashMap<>();
        described.put("package", sortKeys(root.getPackage()));
        described.put("rules", rules(root.getOverrides()));
        described.put("nested", rules(a.getOverrides()));
        described.put("nodes", nodes);
        described.put("edges", edges);
        described.put("rootOverrides", attempt(() -> {
            root.assertRootOverrides();
            return "ok";
        }));
        return described;
    }

    private static @Nullable Object attempt(Callable<@Nullable Object> read) {
        try {
            return read.call();
        } catch (Exception e) {
            return "THROW";
        }
    }

    private static @Nullable List<@Nullable Object> rule(@Nullable OverrideSet rule) {
        return rule == null ? null : Arrays.asList(rule.getKey(), rule.getName(), rule.getKeySpec(), rule.getValue());
    }

    private static @Nullable List<Object> rules(@Nullable OverrideSet overrides) {
        if (overrides == null) {
            return null;
        }
        List<Object> rules = new ArrayList<>();
        for (OverrideSet rule : overrides.getRuleset().values()) {
            rules.add(rule(rule));
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object sortKeys(@Nullable Object value) {
        if (value instanceof List) {
            List<Object> sorted = new ArrayList<>();
            for (Object element : (List<Object>) value) {
                sorted.add(sortKeys(element));
            }
            return sorted;
        }
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                sorted.put(entry.getKey(), sortKeys(entry.getValue()));
            }
            return sorted;
        }
        return value;
    }
}
