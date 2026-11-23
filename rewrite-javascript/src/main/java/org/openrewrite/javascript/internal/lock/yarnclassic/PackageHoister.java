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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Port of yarn src/package-hoister.js (yarn 1.22.22): the node_modules tree of a project without workspaces.
 * Cache locations identify packages; keys are the ancestry parts separated by '#', as in Yarn.
 */
final class PackageHoister {
    static final class HoistManifest {
        String key;
        List<String> parts;
        final Manifest pkg;
        final String loc;
        boolean isRequired;
        final boolean isIncompatible;

        HoistManifest(String key, List<String> parts, Manifest pkg, String loc,
                      boolean isRequired, boolean isIncompatible) {
            this.key = key;
            this.parts = parts;
            this.pkg = pkg;
            this.loc = loc;
            this.isRequired = isRequired;
            this.isIncompatible = isIncompatible;
        }
    }

    private static final class QueueItem {
        final String pattern;
        final HoistManifest parent;
        QueueItem(String pattern, HoistManifest parent) {
            this.pattern = pattern;
            this.parent = parent;
        }
    }

    private static final class Visit {
        final Manifest pkg;
        final List<Manifest> ancestry;
        final String pattern;
        Visit(Manifest pkg, List<Manifest> ancestry, String pattern) {
            this.pkg = pkg;
            this.ancestry = ancestry;
            this.pattern = pattern;
        }
    }

    private static final class Occurrence {
        final String pattern;
        final Set<Manifest> occurrences = new LinkedHashSet<>();
        Occurrence(String pattern) {
            this.pattern = pattern;
        }
    }

    private final PackageResolver resolver;
    private final boolean ignoreOptional;
    final Map<String, HoistManifest> tree = new LinkedHashMap<>();
    private final Map<String, HoistManifest> taintedKeys = new LinkedHashMap<>();
    private List<QueueItem> levelQueue = new ArrayList<>();

    PackageHoister(PackageResolver resolver, boolean ignoreOptional) {
        this.resolver = resolver;
        this.ignoreOptional = ignoreOptional;
    }

    private boolean taintKey(String key, HoistManifest info) {
        HoistManifest existing = taintedKeys.get(key);
        if (existing != null && !existing.loc.equals(info.loc)) {
            return false;
        }
        taintedKeys.put(key, info);
        return true;
    }

    void seed(List<String> patterns) {
        prepass(patterns);
        for (String pattern : resolver.dedupePatterns(patterns)) {
            seed(pattern, true, null);
        }
        while (true) {
            List<QueueItem> queue = levelQueue;
            if (queue.isEmpty()) {
                propagateRequired();
                return;
            }
            levelQueue = new ArrayList<>();
            queue.sort((a, b) -> Misc.sortAlpha(a.pattern, b.pattern));
            List<QueueItem> sortedQueue = new ArrayList<>();
            Set<String> availableSet = new HashSet<>();
            boolean hasChanged = true;
            while (!queue.isEmpty() && hasChanged) {
                hasChanged = false;
                List<QueueItem> copy = queue;
                queue = new ArrayList<>();
                for (QueueItem item : copy) {
                    Manifest pkg = resolver.getStrictResolvedPattern(item.pattern);
                    if (availableSet.containsAll(Js.keys(pkg.get("peerDependencies")))) {
                        sortedQueue.add(item);
                        // Upstream adds the pattern, not the package name.
                        availableSet.add(item.pattern);
                        hasChanged = true;
                    } else {
                        queue.add(item);
                    }
                }
            }
            sortedQueue.addAll(queue);
            for (QueueItem item : sortedQueue) {
                HoistManifest info = seed(item.pattern, false, item.parent);
                if (info != null) {
                    hoist(info);
                }
            }
        }
    }

    private @Nullable HoistManifest seed(String pattern, boolean direct, @Nullable HoistManifest parent) {
        Manifest pkg = resolver.getStrictResolvedPattern(pattern);
        PackageReference ref = requireNonNull(pkg.reference);
        List<String> parentParts = Collections.emptyList();
        boolean incompatible = ref.ignore;
        boolean markedOptional = Boolean.TRUE.equals(ref.optional) && ignoreOptional;
        boolean required = direct && !ref.ignore && !incompatible && !markedOptional;
        if (parent != null) {
            if (!tree.containsKey(parent.key)) {
                return null;
            }
            if (!direct && !incompatible && parent.isRequired && !markedOptional) {
                required = true;
            }
            parentParts = parent.parts;
        }
        String loc = PackageFetcher.generateModuleCachePath(ref, resolver.config.getRelease());
        List<String> parts = concat(parentParts, Js.string(pkg.getName()));
        String key = String.join("#", parts);
        HoistManifest info = new HoistManifest(key, parts, pkg, loc, required, incompatible);
        tree.put(key, info);
        taintKey(key, info);
        Set<String> pushed = new HashSet<>();
        for (String dep : ref.dependencies) {
            if (pushed.add(dep)) {
                levelQueue.add(new QueueItem(dep, info));
            }
        }
        return info;
    }

    private void propagateRequired() {
        List<HoistManifest> toVisit = new ArrayList<>();
        for (HoistManifest info : tree.values()) {
            if (info.isRequired) {
                toVisit.add(info);
            }
        }
        for (int i = 0; i < toVisit.size(); i++) {
            HoistManifest info = toVisit.get(i);
            for (String dep : requireNonNull(info.pkg.reference).dependencies) {
                HoistManifest depInfo = lookupDependency(info, dep);
                if (depInfo == null) {
                    continue;
                }
                PackageReference depRef = depInfo.pkg.reference;
                boolean optional = depRef != null && Boolean.TRUE.equals(depRef.optional) && ignoreOptional &&
                        !(info.isRequired && !"optional".equals(depRef.hint));
                if (!depInfo.isRequired && !depInfo.isIncompatible && !optional) {
                    depInfo.isRequired = true;
                    toVisit.add(depInfo);
                }
            }
        }
    }

    private @Nullable HoistManifest lookupDependency(HoistManifest info, String pattern) {
        Manifest pkg = resolver.getStrictResolvedPattern(pattern);
        for (int i = info.parts.size(); i >= 0; i--) {
            String key = String.join("#", concat(info.parts.subList(0, i), Js.string(pkg.getName())));
            HoistManifest existing = tree.get(key);
            if (existing != null) {
                return existing;
            }
        }
        return null;
    }

    private static final class NewParts {
        final List<String> parts;
        final boolean duplicate;
        NewParts(List<String> parts, boolean duplicate) {
            this.parts = parts;
            this.duplicate = duplicate;
        }
    }

    private NewParts getNewParts(String key, HoistManifest info, List<String> parts) {
        boolean stepUp = false;
        List<String> stack = new ArrayList<>();
        String name = parts.remove(parts.size() - 1);
        for (int i = parts.size() - 1; i >= 0; i--) {
            List<String> checkParts = concat(parts.subList(0, i), name);
            String checkKey = String.join("#", checkParts);
            HoistManifest existing = tree.get(checkKey);
            if (existing != null) {
                if (existing.loc.equals(info.loc)) {
                    if (!existing.isRequired && info.isRequired) {
                        existing.isRequired = true;
                    }
                    return new NewParts(checkParts, true);
                }
                break;
            }
            HoistManifest taint = taintedKeys.get(checkKey);
            if (taint != null && !taint.loc.equals(info.loc)) {
                break;
            }
        }
        List<String> peers = Js.keys(info.pkg.get("peerDependencies"));
        hoistLoop: while (!parts.isEmpty()) {
            for (String peer : peers) {
                if (tree.containsKey(String.join("#", concat(parts, peer)))) {
                    break hoistLoop;
                }
            }
            String checkKey = String.join("#", concat(parts, name));
            if (tree.containsKey(checkKey) || !key.equals(checkKey) && taintedKeys.containsKey(checkKey)) {
                stepUp = true;
                break;
            }
            stack.add(parts.remove(parts.size() - 1));
        }
        parts.add(name);
        if (!isValidPosition(parts, info)) {
            stepUp = true;
        }
        while (stepUp && !stack.isEmpty()) {
            parts.remove(parts.size() - 1);
            parts.add(stack.remove(stack.size() - 1));
            parts.add(name);
            if (isValidPosition(parts, info)) {
                stepUp = false;
            }
        }
        return new NewParts(parts, false);
    }

    private boolean isValidPosition(List<String> parts, HoistManifest info) {
        if (parts.isEmpty()) {
            return false;
        }
        String key = String.join("#", parts);
        HoistManifest existing = tree.get(key);
        if (existing != null && existing.loc.equals(info.loc)) {
            return true;
        }
        HoistManifest taint = taintedKeys.get(key);
        return taint == null || taint.loc.equals(info.loc);
    }

    private void hoist(HoistManifest info) {
        String oldKey = info.key;
        List<String> oldParts = info.parts;
        tree.remove(oldKey);
        NewParts result = getNewParts(oldKey, info, new ArrayList<>(oldParts));
        String newKey = String.join("#", result.parts);
        if (result.duplicate) {
            declareRename(info, oldParts, result.parts);
            return;
        }
        if (!oldKey.equals(newKey)) {
            declareRename(info, oldParts, result.parts);
        }
        info.key = newKey;
        info.parts = result.parts;
        tree.put(newKey, info);
    }

    private void declareRename(HoistManifest info, List<String> oldParts, List<String> newParts) {
        List<String> process = oldParts.subList(0, oldParts.size() - 1);
        for (int i = newParts.size() - 1; i < process.size(); i++) {
            taintKey(String.join("#", concat(process.subList(0, i), Js.string(info.pkg.getName()))), info);
        }
    }

    private void prepass(List<String> initialPatterns) {
        List<String> patterns = resolver.dedupePatterns(initialPatterns);
        Collections.sort(patterns);
        Map<String, List<Visit>> visited = new LinkedHashMap<>();
        Map<String, Map<String, Occurrence>> occurrences = new LinkedHashMap<>();
        Set<String> roots = new HashSet<>();
        for (String pattern : patterns) {
            roots.add(Js.string(resolver.getStrictResolvedPattern(pattern).getName()));
            add(pattern, Collections.emptyList(), Collections.emptyList(), visited, occurrences);
        }
        List<String> names = new ArrayList<>(Js.keys(occurrences));
        Collections.sort(names);
        for (String name : names) {
            Map<String, Occurrence> versions = occurrences.get(name);
            if (versions.size() == 1 || tree.containsKey(name) || roots.contains(name)) {
                continue;
            }
            List<String> keys = new ArrayList<>(Js.keys(versions));
            Collections.sort(keys);
            int count = 0;
            String pattern = null;
            for (String version : keys) {
                Occurrence occurrence = versions.get(version);
                if (count == 0 || occurrence.occurrences.size() > count) {
                    count = occurrence.occurrences.size();
                    pattern = occurrence.pattern;
                }
            }
            if (count > 1) {
                seed(requireNonNull(pattern), false, null);
            }
        }
    }

    private void add(String pattern, List<Manifest> ancestry, List<String> ancestryPatterns,
                     Map<String, List<Visit>> visited, Map<String, Map<String, Occurrence>> occurrences) {
        Manifest pkg = resolver.getStrictResolvedPattern(pattern);
        if (ancestry.contains(pkg)) {
            return;
        }
        List<Visit> previous = visited.get(pattern);
        if (previous != null) {
            for (Visit visit : previous) {
                visitAdd(visit.pkg, visit.ancestry, visit.pattern, occurrences);
            }
            visitAdd(pkg, ancestry, pattern, occurrences);
            return;
        }
        visitAdd(pkg, ancestry, pattern, occurrences);
        for (String dep : requireNonNull(pkg.reference).dependencies) {
            List<Manifest> depAncestry = new ArrayList<>(ancestry);
            depAncestry.add(pkg);
            add(dep, depAncestry, concat(ancestryPatterns, dep), visited, occurrences);
        }
        Visit visit = new Visit(pkg, ancestry, pattern);
        visited.computeIfAbsent(pattern, k -> new ArrayList<>()).add(visit);
        for (String ancestor : ancestryPatterns) {
            List<Visit> ancestorVisits = visited.get(ancestor);
            if (ancestorVisits != null) {
                ancestorVisits.add(visit);
            }
        }
    }

    private static void visitAdd(Manifest pkg, List<Manifest> ancestry, String pattern,
                                 Map<String, Map<String, Occurrence>> occurrences) {
        Occurrence occurrence = occurrences.computeIfAbsent(Js.string(pkg.getName()), k -> new LinkedHashMap<>())
                .computeIfAbsent(Js.string(pkg.getVersion()), k -> new Occurrence(pattern));
        if (!ancestry.isEmpty()) {
            occurrence.occurrences.add(ancestry.get(ancestry.size() - 1));
        }
    }

    private static List<String> concat(List<String> parts, String name) {
        List<String> out = new ArrayList<>(parts);
        out.add(name);
        return out;
    }

    Map<String, Manifest> init() {
        Map<String, Manifest> flatTree = new LinkedHashMap<>();
        for (HoistManifest info : tree.values()) {
            if (info.isRequired) {
                flatTree.put("node_modules/" + String.join("/node_modules/", info.parts), info.pkg);
            }
        }
        return flatTree;
    }
}
