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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/lockfile.preferred-versions src/index.ts and resolver-base's version selector
 * types (pnpm 11.2.2).
 */
final class PreferredVersions {

    static final long DIRECT_DEP_SELECTOR_WEIGHT = 1000;
    static final long EXISTING_VERSION_SELECTOR_WEIGHT = 1_000_000;

    /**
     * A selector's type, {@code version}, {@code range} or {@code tag}, with its weight; a selector given as a bare
     * type (no weight) counts 1 where weights are summed.
     */
    @Value
    static class Selector {
        String selectorType;

        @Nullable
        Long weight;

        static Selector of(String selectorType) {
            return new Selector(selectorType, null);
        }

        static Selector of(String selectorType, long weight) {
            return new Selector(selectorType, weight);
        }
    }

    /**
     * A {@code PreferredVersions} object: the selectors by package name it holds itself, then those of the object it
     * was created from ({@code Object.create(parent)}).
     */
    static final class ByName {
        private final Map<String, Map<String, Selector>> own;
        private final @Nullable ByName parent;

        ByName(Map<String, Map<String, Selector>> own, @Nullable ByName parent) {
            this.own = own;
            this.parent = parent;
        }

        static ByName of(Map<String, Map<String, Selector>> own) {
            return new ByName(own, null);
        }

        @Nullable
        Map<String, Selector> get(String name) {
            Map<String, Selector> selectors = own.get(name);
            return selectors != null || parent == null ? selectors : parent.get(name);
        }

        boolean hasOwn(String name) {
            return own.containsKey(name);
        }

        void put(String name, Map<String, Selector> selectors) {
            own.put(name, selectors);
        }

        /**
         * {@code {...object}}: its own selectors alone.
         */
        Map<String, Map<String, Selector>> spreadOwn() {
            return new LinkedHashMap<>(own);
        }
    }

    private PreferredVersions() {
    }

    /**
     * {@code getPreferredVersionsFromLockfileAndManifests}.
     */
    static Map<String, Map<String, Selector>> getPreferredVersionsFromLockfileAndManifests(
            @Nullable Map<String, PackageSnapshot> snapshots, Collection<Map<String, @Nullable Object>> manifests,
            PnpmRelease release) {
        Map<String, Map<String, Selector>> preferredVersions = new LinkedHashMap<>();
        for (Map<String, @Nullable Object> manifest : manifests) {
            Map<String, @Nullable Object> specs = getAllDependenciesFromManifest(manifest);
            for (String name : Js.keys(specs)) {
                String spec = Js.string(specs.get(name));
                String selector = Verification.versionSelectorType(spec, release);
                if (selector == null) {
                    continue;
                }
                preferredVersions.computeIfAbsent(name, n -> new LinkedHashMap<>())
                        .put(spec, Selector.of(selector, DIRECT_DEP_SELECTOR_WEIGHT));
            }
        }
        if (snapshots == null) {
            return preferredVersions;
        }
        addPreferredVersionsFromLockfile(snapshots, preferredVersions, release);
        return preferredVersions;
    }

    // Object.prototype's own properties, which a plain object keyed by package name answers with before 11.10.0
    private static final Set<String> OBJECT_PROTOTYPE = new LinkedHashSet<>(Arrays.asList("constructor",
            "__defineGetter__", "__defineSetter__", "hasOwnProperty", "__lookupGetter__", "__lookupSetter__",
            "isPrototypeOf", "propertyIsEnumerable", "toString", "valueOf", "__proto__", "toLocaleString"));

    private static void addPreferredVersionsFromLockfile(Map<String, PackageSnapshot> snapshots,
                                                         Map<String, Map<String, Selector>> preferredVersions,
                                                         PnpmRelease release) {
        Map<String, Set<String>> uniqueNameVersions = new LinkedHashMap<>();
        for (Map.Entry<String, PackageSnapshot> entry : snapshots.entrySet()) {
            DepPath.DependencyPath pkgInfo = DepPath.parse(entry.getKey());
            String name = Js.string(pkgInfo.getName() == null ? Js.UNDEFINED : pkgInfo.getName());
            // a lock package named after a member of Object.prototype ({@code constructor}) finds that member in
            // uniqueNameVersions before 11.10.0, which seeds it without a prototype
            // (PnpmRelease#seedsPreferredVersionsWithoutAPrototype)
            if (OBJECT_PROTOTYPE.contains(name) && !release.seedsPreferredVersionsWithoutAPrototype()) {
                throw new JsError(JsError.TYPE_ERROR, "uniqueNameVersions[name].add is not a function");
            }
            String version = entry.getValue().getVersion() != null ? entry.getValue().getVersion() :
                    pkgInfo.getVersion() != null ? pkgInfo.getVersion() : "undefined";
            uniqueNameVersions.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(version);
        }
        for (String name : Js.keys(uniqueNameVersions)) {
            for (String version : uniqueNameVersions.get(name)) {
                Map<String, Selector> selectors = preferredVersions.computeIfAbsent(name, n -> new LinkedHashMap<>());
                Selector existingSelector = selectors.get(version);
                if (existingSelector == null) {
                    selectors.put(version, Selector.of("version", EXISTING_VERSION_SELECTOR_WEIGHT));
                    continue;
                }
                if (!"version".equals(existingSelector.getSelectorType())) {
                    throw new JsError(JsError.ERROR,
                            "Encountered unexpected version selector '" + existingSelector.getSelectorType() +
                                      "' for dependency '" + name + "@" + version + "'");
                }
                selectors.put(version, addWeightToVersionSelector(existingSelector, EXISTING_VERSION_SELECTOR_WEIGHT));
            }
        }
    }

    private static Selector addWeightToVersionSelector(Selector selector, long weight) {
        return selector.getWeight() == null ? Selector.of(selector.getSelectorType(), weight + 1) :
                Selector.of(selector.getSelectorType(), selector.getWeight() + weight);
    }

    /**
     * @pnpm/pkg-manifest.utils {@code getAllDependenciesFromManifest} without peers.
     */
    static Map<String, @Nullable Object> getAllDependenciesFromManifest(Map<String, @Nullable Object> manifest) {
        Map<String, @Nullable Object> all = new LinkedHashMap<>();
        all.putAll(Verification.field(manifest, "devDependencies"));
        all.putAll(Verification.field(manifest, "dependencies"));
        all.putAll(Verification.field(manifest, "optionalDependencies"));
        return Js.ordered(all);
    }
}
