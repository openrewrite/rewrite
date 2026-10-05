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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Port of @npmcli/arborist lib/inventory.js (npm 11.17.0).
 */
public final class Inventory {

    static final List<String> KEYS = Collections.unmodifiableList(
            Arrays.asList("name", "license", "funding", "realpath", "packageName"));

    private final Map<String, Node> byLocation = new LinkedHashMap<>();
    private final Map<String, Map<@Nullable Object, Set<Node>>> index = new LinkedHashMap<>();

    public Inventory() {
        for (String key : KEYS) {
            index.put(key, new LinkedHashMap<>());
        }
    }

    public String getPrimaryKey() {
        return "location";
    }

    public List<String> getIndexes() {
        return new ArrayList<>(KEYS);
    }

    public List<Node> filter(Predicate<Node> fn) {
        List<Node> matches = new ArrayList<>();
        for (Node node : values()) {
            if (fn.test(node)) {
                matches.add(node);
            }
        }
        return matches;
    }

    public void add(Node node) {
        Node root = byLocation.get("");
        if (root != null && node.getRoot() != root && node.getRoot() != root.getRoot()) {
            return;
        }

        Node current = byLocation.get(node.getLocation());
        if (current != null) {
            if (current == node) {
                return;
            }
            delete(current);
        }
        byLocation.put(node.getLocation(), node);
        for (Map.Entry<String, Map<@Nullable Object, Set<Node>>> entry : index.entrySet()) {
            String key = entry.getKey();
            Object val;
            if ("name".equals(key) || "realpath".equals(key)) {
                val = property(node, key);
            } else if ("license".equals(key)) {
                Map<String, Object> pkg = node.getPackage();
                val = null;
                if (truthy(pkg.get("license"))) {
                    val = pkg.get("license");
                } else if (truthy(pkg.get("licence"))) {
                    val = pkg.get("licence");
                } else if (pkg.get("licenses") instanceof List) {
                    val = first((List<?>) pkg.get("licenses"));
                } else if (pkg.get("licences") instanceof List) {
                    val = first((List<?>) pkg.get("licences"));
                }
            } else if (truthy(property(node, key))) {
                val = property(node, key);
            } else {
                val = node.getPackage().get(key);
            }
            if (val instanceof Map) {
                if ("license".equals(key)) {
                    val = ((Map<?, ?>) val).get("type");
                } else if ("funding".equals(key)) {
                    val = ((Map<?, ?>) val).get("url");
                }
            }
            entry.getValue().computeIfAbsent(val, k -> new LinkedHashSet<>()).add(node);
        }
    }

    public void delete(Node node) {
        if (!has(node)) {
            return;
        }
        byLocation.remove(node.getLocation());
        for (Map.Entry<String, Map<@Nullable Object, Set<Node>>> entry : index.entrySet()) {
            String key = entry.getKey();
            Map<@Nullable Object, Set<Node>> map = entry.getValue();
            boolean onNode = !"license".equals(key) && !"funding".equals(key);
            Object nodeVal = onNode ? property(node, key) : null;
            Object val = onNode ? nodeVal : node.getPackage().get(key);
            Set<Node> set = map.get(val);
            if (set != null) {
                set.remove(node);
                if (set.isEmpty()) {
                    map.remove(nodeVal);
                }
            }
        }
    }

    public Set<Node> query(String key, @Nullable Object val) {
        Map<@Nullable Object, Set<Node>> map = index.get(key);
        Set<Node> found = map.get(val);
        return found != null ? found : new LinkedHashSet<>();
    }

    public Set<@Nullable Object> query(String key) {
        return index.get(key).keySet();
    }

    public boolean has(Node node) {
        return byLocation.get(node.getLocation()) == node;
    }

    public @Nullable Node get(String location) {
        return byLocation.get(location);
    }

    public int size() {
        return byLocation.size();
    }

    public List<Node> values() {
        return new ArrayList<>(byLocation.values());
    }

    public List<String> keys() {
        return new ArrayList<>(byLocation.keySet());
    }

    public List<Map.Entry<String, Node>> entries() {
        return new ArrayList<>(byLocation.entrySet());
    }

    // name and realpath are a node's own fields, packageName a getter; the others come from its package
    private static @Nullable Object property(Node node, String key) {
        switch (key) {
            case "name":
                return node.getName();
            case "realpath":
                return node.getRealpath();
            case "packageName":
                return node.getPackageName();
            default:
                return null;
        }
    }

    private static @Nullable Object first(List<?> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private static boolean truthy(@Nullable Object val) {
        if (val == null || Boolean.FALSE.equals(val)) {
            return false;
        }
        if (val instanceof String) {
            return !((String) val).isEmpty();
        }
        if (val instanceof Number) {
            double d = ((Number) val).doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return true;
    }
}
