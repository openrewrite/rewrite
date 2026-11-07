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
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep508.Pep508Requirement;

import java.util.*;
import java.util.function.Function;

/**
 * A manifest's direct declarations, each by the group Poetry or PDM records it under, and what each declaration says
 * besides its version (extras, marker, URL, or Poetry's other table keys). Poetry and PDM record every locked package's
 * groups, and the markers it carries per group, from the groups whose declarations reach it, so an edit that changes
 * a declaration's shape or which groups reach a package changes entries the version-only engines do not compute.
 */
public final class GroupedDeclarations {

    /**
     * Canonical name to the scopes declaring it, each with that declaration's shape. A scope is the group, followed by
     * {@code [extra]} for an optional dependency, so a name declared both as a dependency and in an extra is two scopes.
     */
    private final Map<String, Map<String, String>> shapes = new TreeMap<>();

    private GroupedDeclarations() {
    }

    /**
     * Poetry's groups: {@code [project]} dependencies, optional dependencies and {@code [tool.poetry.dependencies]}
     * are {@code main}, each {@code [tool.poetry.group.<name>]} and PEP 735 group its own.
     */
    public static GroupedDeclarations poetry(Map<String, Object> pyproject) {
        GroupedDeclarations d = new GroupedDeclarations();
        Map<String, Object> project = asMap(pyproject.get("project"));
        d.addPep508("main", project.get("dependencies"));
        for (Map.Entry<String, Object> extra : asMap(project.get("optional-dependencies")).entrySet()) {
            d.addPep508("main[" + extra.getKey() + "]", extra.getValue());
        }
        for (Map.Entry<String, Object> group : asMap(pyproject.get("dependency-groups")).entrySet()) {
            d.addPep508(group.getKey(), group.getValue());
        }
        Map<String, Object> poetry = asMap(asMap(pyproject.get("tool")).get("poetry"));
        d.addPoetry("main", asMap(poetry.get("dependencies")));
        for (Map.Entry<String, Object> group : asMap(poetry.get("group")).entrySet()) {
            d.addPoetry(group.getKey(), asMap(asMap(group.getValue()).get("dependencies")));
        }
        return d;
    }

    /**
     * PDM's groups: {@code [project]} dependencies are {@code default}, each optional dependency, PEP 735 group and
     * {@code [tool.pdm.dev-dependencies]} group its own.
     */
    public static GroupedDeclarations pdm(Map<String, Object> pyproject) {
        GroupedDeclarations d = new GroupedDeclarations();
        Map<String, Object> project = asMap(pyproject.get("project"));
        d.addPep508("default", project.get("dependencies"));
        for (Map.Entry<String, Object> extra : asMap(project.get("optional-dependencies")).entrySet()) {
            d.addPep508(Pep508Requirement.canonicalize(extra.getKey()), extra.getValue());
        }
        for (Map.Entry<String, Object> group : asMap(pyproject.get("dependency-groups")).entrySet()) {
            d.addPep508(Pep508Requirement.canonicalize(group.getKey()), group.getValue());
        }
        Map<String, Object> pdm = asMap(asMap(pyproject.get("tool")).get("pdm"));
        for (Map.Entry<String, Object> group : asMap(pdm.get("dev-dependencies")).entrySet()) {
            d.addPep508(Pep508Requirement.canonicalize(group.getKey()), group.getValue());
        }
        return d;
    }

    /**
     * The first name declared in a scope both manifests share whose declaration there changed more than its version,
     * or null when every such change is to a version alone.
     */
    public @Nullable String firstReshaped(GroupedDeclarations edited) {
        for (Map.Entry<String, Map<String, String>> before : shapes.entrySet()) {
            Map<String, String> after = edited.shapes.get(before.getKey());
            if (after == null) {
                continue;
            }
            for (Map.Entry<String, String> scope : before.getValue().entrySet()) {
                String shape = after.get(scope.getKey());
                if (shape != null && !shape.equals(scope.getValue())) {
                    return before.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Each package's groups: the groups of the declarations from which it is reachable, following {@code edges} from a
     * package's canonical name to the canonical names it depends on.
     */
    public Map<String, Set<String>> reachableGroups(Function<String, Collection<String>> edges) {
        Map<String, Set<String>> groups = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> declared : shapes.entrySet()) {
            for (String scope : declared.getValue().keySet()) {
                String group = scope.contains("[") ? scope.substring(0, scope.indexOf('[')) : scope;
                Deque<String> queue = new ArrayDeque<>();
                queue.add(declared.getKey());
                while (!queue.isEmpty()) {
                    String name = queue.poll();
                    if (groups.computeIfAbsent(name, n -> new TreeSet<>()).add(group)) {
                        queue.addAll(edges.apply(name));
                    }
                }
            }
        }
        return groups;
    }

    private void addPep508(String scope, @Nullable Object list) {
        if (!(list instanceof List)) {
            return;
        }
        for (Object item : (List<?>) list) {
            if (item instanceof String) {
                Pep508Requirement req = Pep508Requirement.parse((String) item);
                if (req != null) {
                    String shape = new TreeSet<>(req.getExtras()) + "|" + (req.getUrl() == null ? "" : req.getUrl()) +
                                   "|" + (req.getMarker() == null ? "" : req.getMarker().toString());
                    put(req.getCanonicalName(), scope, shape);
                }
            }
        }
    }

    private void addPoetry(String scope, Map<String, Object> table) {
        for (Map.Entry<String, Object> e : table.entrySet()) {
            if ("python".equals(e.getKey())) {
                continue;
            }
            Object value = e.getValue();
            String shape;
            if (value instanceof String) {
                shape = "";
            } else if (value instanceof Map) {
                Map<String, Object> rest = new TreeMap<>(asMap(value));
                rest.remove("version");
                shape = rest.toString();
            } else {
                // a multiple-constraints list: any change to it is more than a version
                shape = String.valueOf(value);
            }
            put(Pep508Requirement.canonicalize(e.getKey()), scope, shape);
        }
    }

    private void put(String name, String scope, String shape) {
        // a name declared twice in one scope (under different markers) is one shape of both
        shapes.computeIfAbsent(name, n -> new TreeMap<>()).merge(scope, shape, (a, b) -> a + "\n" + b);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(@Nullable Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }
}
