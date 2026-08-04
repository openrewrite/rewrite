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
package org.openrewrite.javascript.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an npm {@code package-lock.json} v3-format string into a fully linked
 * graph of {@link ResolvedDependency} instances: each nested {@link Dependency}
 * request carries a {@code resolved} pointer, so the graph is navigable to
 * arbitrary depth via {@link Dependency#getResolved()}. The Bun, yarn, and pnpm
 * lock formats are reduced to the same npm v3 shape upstream by their
 * respective adapters, so this parser handles all PMs.
 * <p>
 * Linking mirrors the TypeScript-side {@code parseResolutions} two-pass
 * approach: pass one creates one {@link ResolvedDependency} per distinct
 * {@code name@version} and indexes it by lock-file path; pass two populates the
 * dependency lists, resolving each request Node-style (nearest
 * {@code node_modules} walking up from the dependent's path) with a semver
 * fallback for paths the walk cannot reach. The lists are filled in place after
 * construction, per the linkage contract on {@link ResolvedDependency};
 * instances never escape this class until fully linked.
 */
public final class LockFileParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final LatestRelease VERSION_PRECEDENCE = new LatestRelease(null);

    private LockFileParser() {}

    @Value
    public static class ParseResult {
        List<ResolvedDependency> all;
        Map<String, ResolvedDependency> topLevel;

        @Getter(AccessLevel.NONE)
        Map<String, ResolvedDependency> byPath;

        @Getter(AccessLevel.NONE)
        Map<String, List<ResolvedDependency>> byName;

        /**
         * Resolve a dependency request made from the project root (a declared
         * dependency in package.json), using the same semantics as the
         * transitive links inside {@link #getAll()}.
         */
        public @Nullable ResolvedDependency resolve(String name, String versionConstraint) {
            return resolve(name, versionConstraint, "");
        }

        /**
         * Resolves a dependency name from a given path context using Node.js-style
         * resolution: nearest {@code node_modules} first, then walking up to parent
         * directories. Falls back to semver matching among all versions of the name
         * for paths the walk cannot reach (the yarn/pnpm adapters park duplicate
         * versions under synthetic paths that are not on any walk-up chain).
         */
        @Nullable ResolvedDependency resolve(String name, String versionConstraint, String contextPath) {
            String currentPath = contextPath;
            while (true) {
                String candidatePath = currentPath.isEmpty()
                        ? "node_modules/" + name
                        : currentPath + "/node_modules/" + name;
                ResolvedDependency resolved = byPath.get(candidatePath);
                if (resolved != null) {
                    return resolved;
                }
                if (currentPath.isEmpty()) {
                    break;
                }
                int lastNodeModules = currentPath.lastIndexOf("/node_modules/");
                currentPath = lastNodeModules < 0 ? "" : currentPath.substring(0, lastNodeModules);
            }

            List<ResolvedDependency> candidates = byName.get(name);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            Validated<VersionComparator> constraint =
                    Semver.validate(versionConstraint, null, Semver.Ecosystem.NODE);
            if (constraint.isValid()) {
                for (ResolvedDependency candidate : candidates) {
                    if (candidate.getVersion() != null &&
                            constraint.getValue().isValid(null, candidate.getVersion())) {
                        return candidate;
                    }
                }
            }
            // No version satisfies the constraint: fall back to the highest version.
            ResolvedDependency max = candidates.get(0);
            for (int i = 1; i < candidates.size(); i++) {
                ResolvedDependency candidate = candidates.get(i);
                if (candidate.getVersion() != null && (max.getVersion() == null ||
                        VERSION_PRECEDENCE.compare(null, candidate.getVersion(), max.getVersion()) > 0)) {
                    max = candidate;
                }
            }
            return max;
        }
    }

    public static ParseResult parse(String npmV3Json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(npmV3Json);
        } catch (IOException e) {
            throw new RuntimeException("malformed lock JSON: " + e.getMessage(), e);
        }
        JsonNode packages = root.get("packages");
        if (packages == null || !packages.isObject()) {
            throw new RuntimeException("lock file is missing the `packages` map");
        }

        List<ResolvedDependency> all = new ArrayList<>();
        Map<String, ResolvedDependency> byNameAndVersion = new LinkedHashMap<>();
        Map<String, ResolvedDependency> byPath = new LinkedHashMap<>();
        Map<String, List<ResolvedDependency>> byName = new LinkedHashMap<>();
        Map<String, ResolvedDependency> topLevel = new LinkedHashMap<>();
        // Only the entry that created a ResolvedDependency populates its lists,
        // so the same name@version appearing at several paths isn't filled twice.
        List<Map.Entry<String, JsonNode>> creatorEntries = new ArrayList<>();

        // Pass 1: create all ResolvedDependency instances and index them by path.
        packages.fields().forEachRemaining(entry -> {
            String pathKey = entry.getKey();
            if (pathKey.isEmpty()) {
                return; // root entry
            }
            String name = nameFromPathKey(pathKey);
            if (name == null) {
                return;
            }
            JsonNode body = entry.getValue();
            String version = body.path("version").asText(null);

            String nameAndVersion = name + "@" + version;
            ResolvedDependency dep = byNameAndVersion.get(nameAndVersion);
            if (dep == null) {
                dep = new ResolvedDependency(
                        name, version,
                        emptyListIfPresent(body.get("dependencies")),
                        emptyListIfPresent(body.get("devDependencies")),
                        emptyListIfPresent(body.get("peerDependencies")),
                        emptyListIfPresent(body.get("optionalDependencies")),
                        readStringMap(body.get("engines")),
                        body.path("license").asText(null));
                byNameAndVersion.put(nameAndVersion, dep);
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(dep);
                all.add(dep);
                creatorEntries.add(entry);
            }
            byPath.put(pathKey, dep);
            if (isTopLevel(pathKey)) {
                topLevel.put(name, dep);
            }
        });

        ParseResult result = new ParseResult(all, topLevel, byPath, byName);

        // Pass 2: populate the dependency lists, linking each request to its
        // resolution from the dependent's own path context.
        Map<String, Dependency> dependencyCache = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : creatorEntries) {
            String pathKey = entry.getKey();
            JsonNode body = entry.getValue();
            ResolvedDependency dep = byPath.get(pathKey);
            fillDependencies(dep.getDependencies(), body.get("dependencies"), pathKey, result, dependencyCache);
            fillDependencies(dep.getDevDependencies(), body.get("devDependencies"), pathKey, result, dependencyCache);
            fillDependencies(dep.getPeerDependencies(), body.get("peerDependencies"), pathKey, result, dependencyCache);
            fillDependencies(dep.getOptionalDependencies(), body.get("optionalDependencies"), pathKey, result, dependencyCache);
        }
        return result;
    }

    /**
     * Extracts the package name from a lock-file path key.
     * <p>
     * The name is whatever comes after the LAST {@code node_modules/} segment,
     * which handles top-level deps, nested deps, and scoped packages uniformly.
     */
    private static @Nullable String nameFromPathKey(String pathKey) {
        int marker = pathKey.lastIndexOf("node_modules/");
        if (marker < 0) {
            return null;
        }
        String tail = pathKey.substring(marker + "node_modules/".length());
        return tail.isEmpty() ? null : tail;
    }

    private static boolean isTopLevel(String pathKey) {
        // Top-level entries have exactly one "node_modules/" segment.
        return pathKey.indexOf("node_modules/") == 0
                && pathKey.indexOf("/node_modules/", "node_modules/".length()) < 0;
    }

    /**
     * A mutable list to be populated in pass 2, or null when the entry does not
     * declare this scope (the model uses null, not an empty list, for absent scopes).
     */
    private static @Nullable List<Dependency> emptyListIfPresent(@Nullable JsonNode node) {
        if (node == null || !node.isObject() || node.isEmpty()) {
            return null;
        }
        return new ArrayList<>();
    }

    private static void fillDependencies(@Nullable List<Dependency> target,
                                         @Nullable JsonNode node,
                                         String contextPath,
                                         ParseResult result,
                                         Map<String, Dependency> dependencyCache) {
        if (target == null || node == null || !node.isObject()) {
            return;
        }
        node.fields().forEachRemaining(e -> {
            String name = e.getKey();
            String constraint = e.getValue().asText("");
            ResolvedDependency resolved = result.resolve(name, constraint, contextPath);
            // Reuse Dependency instances (see the @JsonIdentityInfo contract on
            // Dependency): identical requests resolving identically share one object.
            String cacheKey = name + '@' + constraint + '@' +
                    (resolved == null ? "unresolved" : resolved.getName() + '@' + resolved.getVersion());
            target.add(dependencyCache.computeIfAbsent(cacheKey,
                    k -> new Dependency(name, constraint, resolved)));
        });
    }

    private static @Nullable Map<String, String> readStringMap(@Nullable JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Map<String, String> out = new LinkedHashMap<>();
        node.fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue().asText("")));
        return out.isEmpty() ? null : out;
    }
}
