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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core Configuration.ts {@code getPackageExtensions} and {@code normalizePackage} (Yarn 4.10.3).
 */
final class PackageExtensions {

    enum Type {
        DEPENDENCY, PEER_DEPENDENCY, PEER_DEPENDENCY_META
    }

    @Value
    static class Extension {
        Type type;

        @Nullable
        Descriptor descriptor;

        @Nullable
        String selector;

        @Nullable
        String key;

        @Nullable
        Object value;
    }

    /**
     * {@code [range, extensionsPerRange]}.
     */
    @Value
    static class RangeExtensions {
        String range;
        List<Extension> extensions;
    }

    private PackageExtensions() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, List<RangeExtensions>> load(YarnRelease release, Configuration options) {
        Map<String, List<RangeExtensions>> packageExtensions = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, @Nullable Object>> builtin : release.getPackageExtensions()) {
            register(packageExtensions, StructUtils.parseDescriptor(builtin.getKey(), true), builtin.getValue());
        }
        for (Map.Entry<String, @Nullable Object> user : options.getMap("packageExtensions").entrySet()) {
            register(packageExtensions, StructUtils.parseDescriptor(user.getKey(), true),
                    (Map<String, @Nullable Object>) user.getValue());
        }
        return packageExtensions;
    }

    private static void register(Map<String, List<RangeExtensions>> packageExtensions, Descriptor descriptor,
                                 Map<String, @Nullable Object> extensionData) {
        if (YarnpkgSemverUtils.validRange(descriptor.range) == null) {
            throw new JsError(JsError.ERROR, "Only semver ranges are allowed as keys for the packageExtensions " +
                                             "setting");
        }
        Manifest extension = new Manifest();
        extension.load(extensionData, true);
        List<Extension> extensionsPerRange = new ArrayList<>();
        packageExtensions.computeIfAbsent(descriptor.identHash, k -> new ArrayList<>())
                .add(new RangeExtensions(descriptor.range, extensionsPerRange));
        for (Descriptor dependency : extension.dependencies.values()) {
            extensionsPerRange.add(new Extension(Type.DEPENDENCY, dependency, null, null, null));
        }
        for (Descriptor peerDependency : extension.peerDependencies.values()) {
            extensionsPerRange.add(new Extension(Type.PEER_DEPENDENCY, peerDependency, null, null, null));
        }
        for (Map.Entry<String, Map<String, Object>> meta : extension.peerDependenciesMeta.entrySet()) {
            for (Map.Entry<String, Object> field : meta.getValue().entrySet()) {
                extensionsPerRange.add(new Extension(Type.PEER_DEPENDENCY_META, null, meta.getKey(), field.getKey(),
                        field.getValue()));
            }
        }
    }

    static Package normalizePackage(Configuration configuration, Package original,
                                    Map<String, List<RangeExtensions>> packageExtensions) {
        Package pkg = StructUtils.copyPackage(original);
        List<RangeExtensions> extensionsPerIdent = packageExtensions.get(original.identHash);
        if (extensionsPerIdent != null && original.version != null) {
            for (RangeExtensions rangeExtensions : extensionsPerIdent) {
                if (!YarnpkgSemverUtils.satisfiesWithPrereleases(original.version, rangeExtensions.range, false)) {
                    continue;
                }
                for (Extension extension : rangeExtensions.extensions) {
                    switch (extension.type) {
                        case DEPENDENCY: {
                            Descriptor descriptor = extension.descriptor;
                            if (!pkg.dependencies.containsKey(descriptor.identHash)) {
                                pkg.dependencies.put(descriptor.identHash,
                                        configuration.normalizeDependency(descriptor));
                            }
                            break;
                        }
                        case PEER_DEPENDENCY: {
                            Descriptor descriptor = extension.descriptor;
                            if (!pkg.peerDependencies.containsKey(descriptor.identHash)) {
                                pkg.peerDependencies.put(descriptor.identHash, descriptor);
                            }
                            break;
                        }
                        case PEER_DEPENDENCY_META: {
                            Map<String, Object> current = pkg.peerDependenciesMeta.get(extension.selector);
                            if (current == null || !current.containsKey(extension.key) ||
                                current.get(extension.key) != extension.value &&
                                !extensionValueEquals(current.get(extension.key), extension.value)) {
                                // the copy shares its meta objects with the original package, as Yarn's does
                                pkg.peerDependenciesMeta.computeIfAbsent(extension.selector,
                                        k -> new LinkedHashMap<>()).put(extension.key, extension.value);
                            }
                            break;
                        }
                        default:
                            throw new JsError(JsError.ERROR, "Assertion failed: " + extension.type);
                    }
                }
            }
        }

        for (String identString : new ArrayList<>(pkg.peerDependenciesMeta.keySet())) {
            Ident ident = StructUtils.parseIdent(identString);
            if (!pkg.peerDependencies.containsKey(ident.identHash)) {
                pkg.peerDependencies.put(ident.identHash, StructUtils.makeDescriptor(ident, "*"));
            }
        }

        for (Descriptor descriptor : new ArrayList<>(pkg.peerDependencies.values())) {
            if ("types".equals(descriptor.scope)) {
                continue;
            }
            String typesName = descriptor.scope != null && !descriptor.scope.isEmpty() ?
                    descriptor.scope + "__" + descriptor.name : descriptor.name;
            Ident typesIdent = StructUtils.makeIdent("types", typesName);
            String stringifiedTypesIdent = StructUtils.stringifyIdent(typesIdent);
            if (pkg.peerDependencies.containsKey(typesIdent.identHash) ||
                pkg.peerDependenciesMeta.containsKey(stringifiedTypesIdent) ||
                configuration.getRelease().skipsTypesPeerOfATypesDependency() &&
                pkg.dependencies.containsKey(typesIdent.identHash)) {
                continue;
            }
            pkg.peerDependencies.put(typesIdent.identHash, StructUtils.makeDescriptor(typesIdent, "*"));
            Map<String, Object> optional = new LinkedHashMap<>();
            optional.put("optional", true);
            pkg.peerDependenciesMeta.put(stringifiedTypesIdent, optional);
        }

        pkg.dependencies = sortByDescriptor(pkg.dependencies);
        pkg.peerDependencies = sortByDescriptor(pkg.peerDependencies);
        return pkg;
    }

    // strict equality of the booleans and undefined a meta field holds
    private static boolean extensionValueEquals(@Nullable Object a, @Nullable Object b) {
        return a instanceof Boolean && a.equals(b);
    }

    private static Map<String, Descriptor> sortByDescriptor(Map<String, Descriptor> dependencies) {
        Map<String, Descriptor> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Descriptor> entry : MiscUtils.sortMap(dependencies.entrySet(),
                e -> StructUtils.stringifyDescriptor(e.getValue()))) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
}
