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

import static org.openrewrite.javascript.internal.lock.npm.Js.UNDEFINED;
import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/inventory.js (npm 11.17.0). Every view it returns is a snapshot: unlike
 * a JavaScript Map or Set, a loop over one sees neither the nodes added nor the nodes removed while it runs.
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
            Map<String, Object> pkg = node.getPackage();
            Object val;
            if ("name".equals(key)) {
                val = node.getName();
            } else if ("realpath".equals(key)) {
                val = node.getRealpath();
            } else if ("license".equals(key)) {
                val = UNDEFINED;
                if (truthy(pkg.get("license"))) {
                    val = pkg.get("license");
                } else if (truthy(pkg.get("licence"))) {
                    val = pkg.get("licence");
                } else if (pkg.get("licenses") instanceof List) {
                    val = first((List<?>) pkg.get("licenses"));
                } else if (pkg.get("licences") instanceof List) {
                    val = first((List<?>) pkg.get("licences"));
                }
            } else if ("packageName".equals(key) && truthy(node.getPackageName())) {
                val = node.getPackageName();
            } else {
                val = Js.get(pkg, key);
            }
            if (val instanceof Map || val instanceof List) {
                if ("license".equals(key)) {
                    val = Js.get(val, "type");
                } else if ("funding".equals(key)) {
                    val = Js.get(val, "url");
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
            Object nodeVal = "name".equals(key) ? node.getName() :
                    "realpath".equals(key) ? node.getRealpath() :
                            "packageName".equals(key) ? node.getPackageName() : UNDEFINED;
            Object val = nodeVal != UNDEFINED ? nodeVal : Js.get(node.getPackage(), key);
            Set<Node> set = map.get(val);
            if (set != null) {
                set.remove(node);
                if (set.isEmpty()) {
                    map.remove(nodeVal);
                }
            }
        }
    }

    /**
     * @param val {@code null} asks for the nodes with no value for the key.
     */
    public Set<Node> query(String key, @Nullable Object val) {
        Map<@Nullable Object, Set<Node>> map = index.get(key);
        Set<Node> found = map.get(val == null && map.containsKey(UNDEFINED) ? UNDEFINED : val);
        return found != null ? new LinkedHashSet<>(found) : new LinkedHashSet<>();
    }

    public Set<@Nullable Object> query(String key) {
        Set<@Nullable Object> keys = new LinkedHashSet<>();
        for (Object val : index.get(key).keySet()) {
            keys.add(val == UNDEFINED ? null : val);
        }
        return keys;
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

    private static @Nullable Object first(List<?> list) {
        return list.isEmpty() ? UNDEFINED : list.get(0);
    }
}
