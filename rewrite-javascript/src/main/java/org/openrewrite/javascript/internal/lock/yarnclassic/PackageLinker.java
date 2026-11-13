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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Port of yarn src/package-linker.js (yarn 1.22.22): peer selection and the installed locations read by bin linking
 * for a node_modules install without workspaces. Files are not copied and links are not written.
 */
final class PackageLinker {
    private final PackageResolver resolver;

    PackageLinker(PackageResolver resolver) {
        this.resolver = resolver;
    }

    void resolvePeerModules() {
        for (Manifest pkg : resolver.getManifests()) {
            Object peers = pkg.get("peerDependencies");
            if (!Js.truthy(peers)) {
                continue;
            }
            PackageReference ref = requireNonNull(pkg.reference);
            List<String> refTree = Collections.emptyList();
            int shortest = Integer.MAX_VALUE;
            for (PackageRequest request : ref.requests) {
                if (request.parentNames.size() < shortest) {
                    shortest = request.parentNames.size();
                    refTree = request.parentNames;
                }
            }
            for (String name : Js.keys(peers)) {
                Object range = Js.get(peers, name);
                int resolvedDistance = Integer.MAX_VALUE;
                PackageReference resolved = null;
                for (Manifest peerPkg : resolver.getAllInfoForPackageName(name)) {
                    PackageReference peerRef = peerPkg.reference;
                    if (peerRef == null) {
                        continue;
                    }
                    int distance = getLevelDistance(peerRef, refTree);
                    if (distance < resolvedDistance && satisfiesPeerDependency(range, peerRef.version)) {
                        resolvedDistance = distance;
                        resolved = peerRef;
                    }
                }
                if (resolved != null) {
                    ref.addDependencies(resolved.patterns);
                }
            }
        }
    }

    private static int getLevelDistance(PackageReference ref, List<String> refTree) {
        int min = Integer.MAX_VALUE;
        for (PackageRequest request : ref.requests) {
            int distance = refTree.size() - request.parentNames.size();
            if (distance >= 0 && distance < min &&
                refTree.subList(0, request.parentNames.size()).equals(request.parentNames)) {
                min = distance;
            }
        }
        return min;
    }

    private boolean satisfiesPeerDependency(Object range, Object version) {
        return "*".equals(range) || range instanceof String && version instanceof String &&
                Semver.satisfiesWithPrereleases((String) version, (String) range, resolver.config.isLooseSemver());
    }

    void copyModules(List<String> patterns, boolean ignoreOptional) {
        resolvePeerModules();
        PackageHoister hoister = new PackageHoister(resolver, ignoreOptional);
        hoister.seed(patterns);
        Map<String, Manifest> tree = hoister.init();
        Map<PackageReference, List<String>> locations = new IdentityHashMap<>();
        for (Map.Entry<String, Manifest> entry : tree.entrySet()) {
            locations.computeIfAbsent(requireNonNull(entry.getValue().reference), k -> new ArrayList<>())
                    .add(entry.getKey());
        }
        // copyModules links each installed package's dependency bins after recording all installed locations.
        for (Map.Entry<String, Manifest> entry : tree.entrySet()) {
            linkBinDependencies(entry.getValue(), entry.getKey() + "/node_modules", locations);
        }
    }

    private void linkBinDependencies(Manifest pkg, String dir, Map<PackageReference, List<String>> locations) {
        for (String pattern : requireNonNull(pkg.reference).dependencies) {
            Manifest dep = resolver.getStrictResolvedPattern(pattern);
            List<String> installed = locations.get(dep.reference);
            if (installed != null && !installed.isEmpty() && Js.truthy(dep.get("bin")) &&
                !Js.keys(dep.get("bin")).isEmpty()) {
                findNearestInstalledVersionOfPackage(dep, dir, installed);
            }
        }
    }

    private void findNearestInstalledVersionOfPackage(Manifest pkg, String binLoc, List<String> locations) {
        String name = Js.string(pkg.getName());
        for (String loc : locations) {
            String current = binLoc;
            while (true) {
                if ((current + "/" + name).equals(loc) ||
                    (current + "/node_modules/" + name).equals(loc)) {
                    return;
                }
                int slash = current.lastIndexOf('/');
                if (slash < 0) {
                    // The lock directory is the common ancestor of the relative installed paths.
                    if (loc.equals(name) || loc.equals("node_modules/" + name)) {
                        return;
                    }
                    break;
                }
                current = current.substring(0, slash);
            }
        }
        if (!resolver.config.getRelease().checksForAnAccessibleBinLocation()) {
            throw new JsError(JsError.TYPE_ERROR, "Reduce of empty array with no initial value");
        }
        throw new JsError(Js.INVARIANT_VIOLATION,
                "could not find a copy of " + name + " to link in " + binLoc);
    }
}
