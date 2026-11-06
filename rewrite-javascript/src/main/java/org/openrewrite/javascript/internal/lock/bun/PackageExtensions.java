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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/** Port of src/install/lockfile/package_extensions.rs (Bun 1.4.4), for text lockfile installs. */
final class PackageExtensions {
    private static final String[] KINDS = {"dependencies", "peerDependencies", "optionalPeers"};
    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Map<String, Set<String>>> applied = new LinkedHashMap<>();

    private static final class Rule {
        final OverrideSelector.PackageSelector selector;
        final List<Dependency> edges;

        Rule(OverrideSelector.PackageSelector selector, List<Dependency> edges) {
            this.selector = selector;
            this.edges = edges;
        }
    }

    void readApplied(Json.Expr section) {
        if (!section.isObject()) {
            return;
        }
        for (Json.Property row : section.properties()) {
            for (String kind : KINDS) {
                Json.Expr names = row.value.get(kind);
                if (names == null || !names.isArray()) {
                    continue;
                }
                for (Json.Expr name : names.items()) {
                    if (name.isString()) {
                        record(row.key.asString(), kind, name.asString());
                    }
                }
            }
        }
    }

    void readRules(Json.Expr manifest, BunRelease release) {
        rules.clear();
        Json.Expr extensions = manifest.get("packageExtensions");
        if (extensions == null || !extensions.isObject()) {
            return;
        }
        for (Json.Property row : extensions.properties()) {
            try {
                OverrideSelector.PackageSelector selector = OverrideSelector.parsePackageSegment(row.key.asString());
                DependencyVersion range = selector.range.isEmpty() ? null :
                        DependencyVersion.parse(selector.name, null, selector.range, release);
                if (!selector.range.isEmpty() && (range == null || range.tag != DependencyVersion.Tag.NPM)) {
                    continue;
                }
                if (!row.value.isObject()) {
                    continue;
                }
                List<Dependency> edges = new ArrayList<>();
                boolean valid = true;
                String[] groups = {"dependencies", "optionalDependencies", "peerDependencies"};
                int[] bits = {Behavior.PROD, Behavior.OPTIONAL, Behavior.PEER};
                for (int g = 0; g < groups.length; g++) {
                    Json.Expr group = row.value.get(groups[g]);
                    if (group == null) {
                        continue;
                    }
                    if (!group.isObject()) {
                        valid = false;
                        break;
                    }
                    for (Json.Property edge : group.properties()) {
                        String name = edge.key.asString();
                        String literal = edge.value.asString();
                        DependencyVersion version = literal == null ? null : DependencyVersion.parse(name, null, literal, release);
                        if (name.isEmpty() || version == null || version.tag == DependencyVersion.Tag.FOLDER ||
                                version.tag == DependencyVersion.Tag.SYMLINK || version.tag == DependencyVersion.Tag.WORKSPACE ||
                                version.tag == DependencyVersion.Tag.CATALOG || version.tag == DependencyVersion.Tag.TARBALL &&
                                !version.tarball().remote) {
                            valid = false;
                            break;
                        }
                        Dependency dep = new Dependency(name, version, Behavior.of(bits[g]));
                        edges.removeIf(old -> old.nameHash == dep.nameHash && old.behavior.isPeer() == dep.behavior.isPeer());
                        edges.add(dep);
                    }
                }
                Json.Expr meta = row.value.get("peerDependenciesMeta");
                if (meta != null && meta.isObject()) {
                    for (Json.Property entry : meta.properties()) {
                        Json.Expr optional = entry.value.get("optional");
                        if (optional == null || !Boolean.TRUE.equals(optional.asBool())) {
                            continue;
                        }
                        String name = entry.key.asString();
                        Dependency peer = null;
                        for (Dependency edge : edges) {
                            if (edge.name.equals(name) && edge.behavior.isPeer()) {
                                peer = edge;
                                break;
                            }
                        }
                        if (peer == null) {
                            peer = new Dependency(name, DependencyVersion.parse(name, null, "*", release),
                                    Behavior.of(Behavior.PEER));
                            edges.add(peer);
                        }
                        peer.behavior = Behavior.of(peer.behavior.bits | Behavior.OPTIONAL);
                    }
                }
                if (valid) {
                    rules.add(new Rule(selector, edges));
                }
            } catch (OverrideSelector.Invalid ignored) {
                // Bun warns and ignores an invalid rule, without changing the resolution.
            }
        }
    }

    private void record(String label, String kind, String name) {
        applied.computeIfAbsent(label, key -> new LinkedHashMap<>())
                .computeIfAbsent(kind, key -> new LinkedHashSet<>()).add(name);
    }

    private boolean was(String label, String kind, String name) {
        Map<String, Set<String>> record = applied.get(label);
        return record != null && record.containsKey(kind) && record.get(kind).contains(name);
    }

    boolean extend(Lockfile lockfile, Package pkg) {
        if (pkg.resolution.tag != Resolution.Tag.NPM) {
            return false;
        }
        String label = pkg.name + "@" + pkg.resolution.version;
        List<Dependency> wanted = new ArrayList<>();
        for (Rule rule : rules) {
            if (!rule.selector.name.equals(pkg.name) || !rule.selector.range.isEmpty() &&
                    !SemverQuery.parse(rule.selector.range, lockfile.release).head.satisfies(pkg.resolution.version, lockfile.release)) {
                continue;
            }
            for (Dependency edge : rule.edges) {
                Dependency first = null;
                for (Dependency old : wanted) {
                    if (old.nameHash == edge.nameHash && old.behavior.isPeer() == edge.behavior.isPeer()) {
                        first = old;
                        break;
                    }
                }
                if (first == null) {
                    wanted.add(edge.copy());
                } else if (edge.behavior.isOptionalPeer()) {
                    first.behavior = Behavior.of(first.behavior.bits | Behavior.OPTIONAL);
                }
            }
        }
        if (wanted.isEmpty() && !applied.containsKey(label)) {
            return false;
        }
        List<Dependency> rows = new ArrayList<>();
        List<Integer> targets = new ArrayList<>();
        Map<String, Set<String>> now = new LinkedHashMap<>();
        boolean changed = false;
        for (int row = pkg.dependenciesOff; row < pkg.dependenciesEnd(); row++) {
            Dependency dep = lockfile.dependencies.get(row).copy();
            int target = lockfile.resolutions.get(row);
            Dependency rule = null;
            for (Dependency edge : wanted) {
                if (edge.nameHash == dep.nameHash && edge.behavior.isPeer() == dep.behavior.isPeer()) {
                    rule = edge;
                    break;
                }
            }
            String kind = dep.behavior.isPeer() ? "peerDependencies" : "dependencies";
            if (was(label, kind, dep.name)) {
                changed |= rule == null || !dep.version.literal.equals(rule.version.literal) ||
                        dep.behavior.optional() != rule.behavior.optional();
                if (rule == null) {
                    continue;
                }
                if (!dep.version.literal.equals(rule.version.literal) || dep.behavior.optional() != rule.behavior.optional()) {
                    dep = rule.copy();
                    target = INVALID_PACKAGE_ID;
                }
                now.computeIfAbsent(kind, key -> new LinkedHashSet<>()).add(dep.name);
            } else if (dep.behavior.isPeer()) {
                boolean madeOptional = was(label, "optionalPeers", dep.name);
                if (rule != null && rule.behavior.optional()) {
                    if (madeOptional || !dep.behavior.optional()) {
                        changed |= !dep.behavior.optional();
                        dep.behavior = Behavior.of(dep.behavior.bits | Behavior.OPTIONAL);
                        now.computeIfAbsent("optionalPeers", key -> new LinkedHashSet<>()).add(dep.name);
                    }
                } else if (madeOptional) {
                    dep.behavior = Behavior.of(dep.behavior.bits & ~Behavior.OPTIONAL);
                    changed = true;
                }
            }
            rows.add(dep);
            targets.add(target);
        }
        for (Dependency edge : wanted) {
            boolean found = false;
            for (Dependency dep : rows) {
                found |= dep.nameHash == edge.nameHash;
            }
            if (!found) {
                rows.add(edge.copy());
                targets.add(INVALID_PACKAGE_ID);
                now.computeIfAbsent(edge.behavior.isPeer() ? "peerDependencies" : "dependencies", key -> new LinkedHashSet<>()).add(edge.name);
                changed = true;
            }
        }
        if (now.isEmpty()) {
            applied.remove(label);
        } else {
            applied.put(label, now);
        }
        if (changed) {
            pkg.dependenciesOff = lockfile.dependencies.size();
            pkg.dependenciesLen = rows.size();
            lockfile.dependencies.addAll(rows);
            lockfile.resolutions.addAll(targets);
        }
        return changed;
    }

    List<String> lines(Lockfile lockfile) {
        Map<String, String> lines = new TreeMap<>(Strings::order);
        for (int row : lockfile.hoistedDependencies) {
            int id = lockfile.resolutions.get(row);
            if (id == INVALID_PACKAGE_ID) {
                continue;
            }
            Package pkg = lockfile.packages.get(id);
            String label = pkg.name + "@" + pkg.resolution;
            if (!applied.containsKey(label)) {
                continue;
            }
            StringBuilder line = new StringBuilder();
            Strings.appendJsonString(line, label, true, lockfile.release);
            line.append(": {");
            boolean any = false;
            for (String kind : KINDS) {
                List<String> names = new ArrayList<>();
                for (Dependency dep : lockfile.dependenciesOf(pkg)) {
                    if (dep.behavior.isPeer() == !kind.equals("dependencies") && was(label, kind, dep.name)) {
                        names.add(dep.name);
                    }
                }
                if (names.isEmpty()) {
                    continue;
                }
                names.sort(Strings::order);
                if (any) {
                    line.append(',');
                }
                any = true;
                line.append(" \"").append(kind).append("\": [");
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0) {
                        line.append(", ");
                    }
                    Strings.appendJsonString(line, names.get(i), true, lockfile.release);
                }
                line.append(']');
            }
            if (any) {
                lines.put(label, line.append(" },\n").toString());
            }
        }
        return new ArrayList<>(lines.values());
    }
}
