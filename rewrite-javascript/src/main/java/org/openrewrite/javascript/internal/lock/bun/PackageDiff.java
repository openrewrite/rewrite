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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of bun's src/install/lockfile/Package.zig {@code Diff.generate} (bun 1.3.10), for an install without update
 * requests.
 */
final class PackageDiff {

    static final class Summary {
        int add;
        int remove;
        int update;
        boolean overridesChanged;
        boolean catalogsChanged;
        final LinkedHashMap<Integer, Boolean> addedTrustedDependencies = new LinkedHashMap<>();
        final Set<Integer> removedTrustedDependencies = new LinkedHashSet<>();
        boolean patchedDependenciesChanged;

        boolean hasDiffs() {
            return add > 0 || remove > 0 || update > 0 || overridesChanged || catalogsChanged ||
                   !addedTrustedDependencies.isEmpty() || !removedTrustedDependencies.isEmpty() ||
                   patchedDependenciesChanged;
        }
    }

    private PackageDiff() {
    }

    /**
     * {@code idMapping}: for the root, per dependency of {@code to}, the index of the unchanged dependency of {@code
     * from} it keeps the resolution of, else {@link Package#INVALID_PACKAGE_ID}; {@code manifests} by location.
     */
    static Summary generate(BunOptions options, Log log, Lockfile fromLockfile, Lockfile toLockfile, Package from,
                            Package to, int @Nullable [] idMapping, Map<String, String> manifests) {
        Summary summary = new Summary();
        boolean isRoot = idMapping != null;
        List<Dependency> toDeps = toLockfile.dependenciesOf(to);
        List<Dependency> fromDeps = fromLockfile.dependenciesOf(from);
        List<Integer> fromResolutions = fromLockfile.resolutionsOf(from);
        int toI = 0;

        if (fromLockfile.overrides.map.size() != toLockfile.overrides.map.size()) {
            summary.overridesChanged = true;
        } else {
            fromLockfile.overrides.sort();
            toLockfile.overrides.sort();
            Iterator<Map.Entry<Long, Dependency>> fromIt = fromLockfile.overrides.map.entrySet().iterator();
            Iterator<Map.Entry<Long, Dependency>> toIt = toLockfile.overrides.map.entrySet().iterator();
            while (fromIt.hasNext()) {
                Map.Entry<Long, Dependency> f = fromIt.next();
                Map.Entry<Long, Dependency> t = toIt.next();
                if (!f.getKey().equals(t.getKey()) || !f.getValue().eql(t.getValue())) {
                    summary.overridesChanged = true;
                    break;
                }
            }
        }

        if (isRoot) {
            summary.catalogsChanged = catalogsChanged(fromLockfile.catalogs, toLockfile.catalogs);
        }

        trustedDependencies(fromLockfile, toLockfile, summary);

        summary.patchedDependenciesChanged = patchedDependenciesChanged(fromLockfile, toLockfile);

        for (int i = 0; i < fromDeps.size(); i++) {
            Dependency fromDep = fromDeps.get(i);
            int prevI = toI;
            boolean found = false;
            for (; toI < toDeps.size(); toI++) {
                if (fromDep.nameHash == toDeps.get(toI).nameHash && fromDep.behavior.eq(toDeps.get(toI).behavior)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (toI = 0; toI < prevI; toI++) {
                    if (fromDep.nameHash == toDeps.get(toI).nameHash && fromDep.behavior.eq(toDeps.get(toI).behavior)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                summary.remove++;
                continue;
            }
            int matched = toI;
            toI++;
            if (toDeps.get(matched).eql(fromDep)) {
                if (idMapping != null) {
                    boolean updateMapping = true;
                    if (isRoot && fromDep.behavior.isWorkspace()) {
                        updateMapping = workspaceUnchanged(options, log, fromLockfile, toLockfile,
                                fromResolutions.get(i), fromDep, manifests);
                        toDeps = toLockfile.dependenciesOf(to);
                    }
                    if (updateMapping) {
                        idMapping[matched] = i;
                        continue;
                    }
                } else {
                    continue;
                }
            }
            summary.update++;
        }

        summary.add = Math.max(0, toDeps.size() - Math.max(0, fromDeps.size() - summary.remove));

        if (from.resolution.tag != Resolution.Tag.ROOT && !to.scripts.eql(from.scripts)) {
            for (int h = 0; h < Package.Scripts.NAMES.length; h++) {
                if (!to.scripts.hooks[h].equals(from.scripts.hooks[h])) {
                    summary.update++;
                }
            }
        }
        return summary;
    }

    private static boolean workspaceUnchanged(BunOptions options, Log log, Lockfile fromLockfile, Lockfile toLockfile,
                                              int fromResolution, Dependency fromDep, Map<String, String> manifests) {
        String workspacePath = toLockfile.workspacePaths.get(fromDep.nameHash);
        if (workspacePath == null) {
            return false;
        }
        String source = manifests.get(RootRelativePath.normalize(workspacePath.replace('\\', '/')));
        if (source == null) {
            return false;
        }
        Json.Expr json;
        try {
            json = Json.parse(source);
        } catch (Json.SyntaxError e) {
            return false;
        }
        Package workspacePkg = new Package();
        workspacePkg.parseWithJSON(toLockfile, options, log,
                RootRelativePath.normalize(workspacePath.replace('\\', '/')), json,
                Features.WORKSPACE, manifests);
        Package fromPkg = fromLockfile.packages.get(fromResolution);
        Summary diff = generate(options, log, fromLockfile, toLockfile, fromPkg, workspacePkg, null, manifests);
        return !diff.hasDiffs();
    }

    private static boolean catalogsChanged(CatalogMap from, CatalogMap to) {
        if (from.defaultMap.size() != to.defaultMap.size()) {
            return true;
        }
        if (from.groups.size() != to.groups.size()) {
            return true;
        }
        from.sort();
        to.sort();
        if (!sameDeps(from.defaultMap, to.defaultMap)) {
            return true;
        }
        Iterator<Map.Entry<String, LinkedHashMap<String, Dependency>>> fromIt = from.groups.entrySet().iterator();
        Iterator<Map.Entry<String, LinkedHashMap<String, Dependency>>> toIt = to.groups.entrySet().iterator();
        while (fromIt.hasNext()) {
            Map.Entry<String, LinkedHashMap<String, Dependency>> f = fromIt.next();
            Map.Entry<String, LinkedHashMap<String, Dependency>> t = toIt.next();
            if (!f.getKey().equals(t.getKey())) {
                return true;
            }
            if (f.getValue().size() != t.getValue().size()) {
                return true;
            }
            if (!sameDeps(f.getValue(), t.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameDeps(Map<String, Dependency> from, Map<String, Dependency> to) {
        Iterator<Map.Entry<String, Dependency>> fromIt = from.entrySet().iterator();
        Iterator<Map.Entry<String, Dependency>> toIt = to.entrySet().iterator();
        while (fromIt.hasNext() && toIt.hasNext()) {
            Map.Entry<String, Dependency> f = fromIt.next();
            Map.Entry<String, Dependency> t = toIt.next();
            if (!f.getKey().equals(t.getKey()) || !f.getValue().eql(t.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static void trustedDependencies(Lockfile fromLockfile, Lockfile toLockfile, Summary summary) {
        Set<Integer> from = fromLockfile.trustedDependencies;
        Set<Integer> to = toLockfile.trustedDependencies;
        if (from == null && to == null) {
            return;
        }
        if (from != null && to != null) {
            for (int t : to) {
                if (!from.contains(t)) {
                    summary.addedTrustedDependencies.put(t, true);
                }
            }
            for (int f : from) {
                if (!to.contains(f)) {
                    summary.removedTrustedDependencies.add(f);
                }
            }
            return;
        }
        if (from != null) {
            for (int hash : DefaultTrustedDependencies.HASHES) {
                if (!from.contains(hash)) {
                    summary.addedTrustedDependencies.put(hash, false);
                }
            }
            for (int f : from) {
                if (!DefaultTrustedDependencies.HASHES.contains(f)) {
                    summary.removedTrustedDependencies.add(f);
                }
            }
            return;
        }
        for (int t : to) {
            summary.addedTrustedDependencies.put(t, true);
        }
    }

    private static boolean patchedDependenciesChanged(Lockfile fromLockfile, Lockfile toLockfile) {
        if (fromLockfile.patchedDependencies.size() != toLockfile.patchedDependencies.size()) {
            return true;
        }
        for (Map.Entry<Long, String> entry : toLockfile.patchedDependencies.entrySet()) {
            String val = fromLockfile.patchedDependencies.get(entry.getKey());
            if (val == null || !val.equals(entry.getValue())) {
                return true;
            }
        }
        for (Long key : fromLockfile.patchedDependencies.keySet()) {
            if (!toLockfile.patchedDependencies.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
