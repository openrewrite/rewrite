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
import org.openrewrite.javascript.internal.lock.bun.OverrideSelector.PackageSelector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/lockfile/OverrideMap.zig (bun 1.3.10): one flat map from a name hash to the dependency
 * every edge of that name resolves with, from {@code overrides}, else {@code resolutions}. From 1.4.0 (OverrideMap.rs,
 * {@link BunRelease#readsScopedOverrides}) beside it the rules scoped to the parent that declares the edge, to the range
 * the edge declares, or both, which win over the flat map where they match.
 */
final class OverrideMap {

    /**
     * The field of the root manifest parseAppend reads overrides from: {@code overrides} when it has one, else
     * {@code resolutions} when it has that, else null.
     */
    static @Nullable String readsFrom(String rootManifest) {
        Json.Expr expr;
        try {
            expr = Json.parse(rootManifest);
        } catch (RuntimeException e) {
            return null;
        }
        return expr.get("overrides") != null ? "overrides" : expr.get("resolutions") != null ? "resolutions" : null;
    }

    /**
     * The first warning {@code release} gives reading the overrides of a root manifest that declares nothing else, as
     * {@link #parseAppend} reads them; null where it reads every rule.
     */
    static @Nullable String warning(BunRelease release, String rootManifest) {
        List<String> warnings = new ArrayList<>();
        new OverrideMap().parseAppend(new Lockfile(release), new Package(), Json.parse(rootManifest),
                new Workspaces(release), Collections.<String, String>emptyMap(), warnings);
        return warnings.isEmpty() ? null : warnings.get(0);
    }

    /**
     * {@code ScopedOverride} (from 1.4.0): a rule that is not a plain name, scoped to a parent, to the range the
     * dependent declares, or both. The parent's version and the target range are {@link DependencyVersion#ZEROED}
     * (no range) or an npm range.
     */
    static final class ScopedOverride {
        final @Nullable Dependency parent;
        final DependencyVersion targetRange;
        final Dependency dep;

        ScopedOverride(@Nullable Dependency parent, DependencyVersion targetRange, Dependency dep) {
            this.parent = parent;
            this.targetRange = targetRange;
            this.dep = dep;
        }

        boolean parentHasRange() {
            return parent != null && parent.version.tag == DependencyVersion.Tag.NPM;
        }

        boolean hasTargetRange() {
            return targetRange.tag == DependencyVersion.Tag.NPM;
        }
    }

    final LinkedHashMap<Long, Dependency> map = new LinkedHashMap<>();

    /**
     * From 1.4.0, the scoped rules, and the names they target.
     */
    final List<ScopedOverride> scoped = new ArrayList<>();
    private final Set<Long> scopedNames = new LinkedHashSet<>();

    // OwnerIndex: dependency id -> owning package id, filled lazily as packages are only ever appended while it lives
    private final List<Integer> ownerByDep = new ArrayList<>();
    private int packagesIndexed;

    /**
     * {@code get}: the version an edge of {@code nameHash}, the dependency {@code dependencyId} of {@code lockfile},
     * resolves with, null for none. The flat map's, unless (from 1.4.0) a scoped rule of the name matches: a ranged
     * parent over a parent over none, within a tier a matching target range winning, ties going to the parent range
     * text, then the target range text, that sorts first.
     */
    @Nullable
    DependencyVersion get(Lockfile lockfile, int dependencyId, long nameHash) {
        if (scoped.isEmpty()) {
            return getFlat(nameHash);
        }
        if (scopedNames.contains(nameHash)) {
            ScopedOverride rule = scopedRuleFor(lockfile, dependencyId, nameHash);
            if (rule != null) {
                return rule.dep.version;
            }
        }
        return getFlat(nameHash);
    }

    private @Nullable DependencyVersion getFlat(long nameHash) {
        if (map.isEmpty()) {
            return null;
        }
        Dependency dep = map.get(nameHash);
        return dep == null ? null : dep.version;
    }

    private @Nullable ScopedOverride scopedRuleFor(Lockfile lockfile, int dependencyId, long nameHash) {
        DependencyVersion declared = lockfile.dependencies.get(dependencyId).version;
        Owner owner = null;
        boolean ownerKnown = false;
        ScopedOverride best = null;
        int bestTier = 0;
        for (ScopedOverride rule : scoped) {
            if (rule.dep.nameHash != nameHash) {
                continue;
            }
            int tier = 0;
            if (rule.parent != null) {
                if (!ownerKnown) {
                    owner = ownerOf(lockfile, dependencyId);
                    ownerKnown = true;
                }
                if (owner == null || owner.nameHash != rule.parent.nameHash) {
                    continue;
                }
                if (rule.parentHasRange()) {
                    if (owner.version == null || !rule.parent.version.npm().version.satisfies(owner.version)) {
                        continue;
                    }
                    tier = 4;
                } else {
                    tier = 2;
                }
            }
            if (rule.hasTargetRange()) {
                if (best != null && bestTier > tier + 1) {
                    continue;
                }
                if (declared.tag != DependencyVersion.Tag.NPM ||
                    !SemverIntersects.intersects(rule.targetRange.npm().version, declared.npm().version)) {
                    continue;
                }
                tier++;
            }
            if (best == null || tier > bestTier || tier == bestTier && compareRangeText(rule, best) < 0) {
                best = rule;
                bestTier = tier;
            }
        }
        return best;
    }

    private static int compareRangeText(ScopedOverride l, ScopedOverride r) {
        int byParent = l.parentHasRange() && r.parentHasRange() ?
                Strings.order(l.parent.version.literal, r.parent.version.literal) : 0;
        return byParent != 0 ? byParent : Strings.order(l.targetRange.literal, r.targetRange.literal);
    }

    // the name and the version of the package whose dependencies hold the edge: an npm resolution's version, a
    // workspace's own; null for an edge no package holds
    private static final class Owner {
        final long nameHash;
        final @Nullable SemverVersion version;

        Owner(long nameHash, @Nullable SemverVersion version) {
            this.nameHash = nameHash;
            this.version = version;
        }
    }

    private @Nullable Owner ownerOf(Lockfile lockfile, int dependencyId) {
        int ownerId = ownerPackageId(lockfile, dependencyId);
        if (ownerId == INVALID_PACKAGE_ID) {
            return null;
        }
        Package owner = lockfile.packages.get(ownerId);
        SemverVersion version;
        switch (owner.resolution.tag) {
            case NPM:
                version = owner.resolution.version;
                break;
            case WORKSPACE:
                version = lockfile.workspaceVersions.get(owner.nameHash);
                break;
            default:
                version = null;
                break;
        }
        return new Owner(owner.nameHash, version);
    }

    private int ownerPackageId(Lockfile lockfile, int dependencyId) {
        int dependenciesLen = lockfile.dependencies.size();
        for (int pkgId = packagesIndexed; pkgId < lockfile.packages.size(); pkgId++) {
            Package pkg = lockfile.packages.get(pkgId);
            int end = Math.min(pkg.dependenciesEnd(), dependenciesLen);
            int begin = Math.min(pkg.dependenciesOff, end);
            while (ownerByDep.size() < end) {
                ownerByDep.add(INVALID_PACKAGE_ID);
            }
            for (int i = begin; i < end; i++) {
                ownerByDep.set(i, pkgId);
            }
        }
        packagesIndexed = lockfile.packages.size();
        return dependencyId < ownerByDep.size() ? ownerByDep.get(dependencyId) : INVALID_PACKAGE_ID;
    }

    /**
     * {@code contains_name} (from 1.4.0): whether a plain rule names the package; a scoped rule does not make every
     * edge of its name the root's own.
     */
    boolean containsName(long nameHash, String name) {
        Dependency dep = map.get(nameHash);
        return dep != null && dep.name.equals(name);
    }

    boolean isEmpty() {
        return map.isEmpty() && scoped.isEmpty();
    }

    /**
     * Whether a rule is scoped to a parent or a version range ({@code has_scoped}, from 1.4.0).
     */
    boolean hasScoped() {
        return !scoped.isEmpty();
    }

    /**
     * {@code has_rule_for_name} (from 1.4.0): whether a plain or a scoped rule targets the name.
     */
    boolean hasRuleForName(long nameHash) {
        return map.containsKey(nameHash) || scopedNames.contains(nameHash);
    }

    /**
     * The names an edge is resolved again for when the overrides change: the plain rules' and (from 1.4.0) the scoped
     * rules' ({@code append_overridden_name_hashes}).
     */
    void appendOverriddenNameHashes(Collection<Long> out) {
        out.addAll(map.keySet());
        out.addAll(scopedNames);
    }

    /**
     * {@code append_catalog_valued_name_hashes}: the names of the rules, plain or scoped, whose value is a
     * {@code catalog:} spec.
     */
    void appendCatalogValuedNameHashes(Collection<Long> out) {
        for (Map.Entry<Long, Dependency> e : map.entrySet()) {
            if (e.getValue().version.tag == DependencyVersion.Tag.CATALOG) {
                out.add(e.getKey());
            }
        }
        for (ScopedOverride rule : scoped) {
            if (rule.dep.version.tag == DependencyVersion.Tag.CATALOG) {
                out.add(rule.dep.nameHash);
            }
        }
    }

    /**
     * Whether the rules differ ({@code OverrideMap::changed}): as {@code Diff.generate} compares them, by count, then
     * sorted in place (from 1.2.8, {@link BunRelease#sortsOverridesBeforeComparingThem()}), pairwise; from 1.4.0 the
     * scoped rules too, by parent, target range and rule.
     */
    static boolean changed(OverrideMap from, OverrideMap to, BunRelease release) {
        if (from.map.size() != to.map.size() || from.scoped.size() != to.scoped.size()) {
            return true;
        }
        if (from.isEmpty()) {
            return false;
        }
        if (release.sortsOverridesBeforeComparingThem()) {
            from.sort();
            to.sort();
        }
        Iterator<Map.Entry<Long, Dependency>> fromIt = from.map.entrySet().iterator();
        Iterator<Map.Entry<Long, Dependency>> toIt = to.map.entrySet().iterator();
        while (fromIt.hasNext()) {
            Map.Entry<Long, Dependency> f = fromIt.next();
            Map.Entry<Long, Dependency> t = toIt.next();
            if (!f.getKey().equals(t.getKey()) || !f.getValue().eql(t.getValue(), release)) {
                return true;
            }
        }
        for (int i = 0; i < from.scoped.size(); i++) {
            ScopedOverride f = from.scoped.get(i);
            ScopedOverride t = to.scoped.get(i);
            boolean parentChanged = f.parent == null ? t.parent != null : t.parent == null || !f.parent.eql(t.parent, release);
            if (parentChanged || !f.targetRange.eql(t.targetRange, release) || !f.dep.eql(t.dep, release)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The plain rules by name; from 1.4.0 the scoped rules too, those without a parent first, then by parent name,
     * parent range, target name and target range.
     */
    void sort() {
        List<Map.Entry<Long, Dependency>> entries = new ArrayList<>(map.entrySet());
        entries.sort((l, r) -> Strings.order(l.getValue().name, r.getValue().name));
        map.clear();
        for (Map.Entry<Long, Dependency> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        scoped.sort((l, r) -> {
            int c = Boolean.compare(l.parent != null, r.parent != null);
            if (c == 0 && l.parent != null && r.parent != null) {
                c = Strings.order(l.parent.name, r.parent.name);
                if (c == 0) {
                    c = Strings.order(l.parent.version.literal, r.parent.version.literal);
                }
            }
            if (c == 0) {
                c = Strings.order(l.dep.name, r.dep.name);
            }
            return c != 0 ? c : Strings.order(l.targetRange.literal, r.targetRange.literal);
        });
    }

    OverrideMap cloneMap(BunRelease release) {
        OverrideMap out = new OverrideMap();
        for (Map.Entry<Long, Dependency> e : map.entrySet()) {
            out.map.put(e.getKey(), e.getValue().cloneDependency(release));
        }
        for (ScopedOverride rule : scoped) {
            Dependency dep = rule.dep.cloneDependency(release);
            out.pushScoped(new ScopedOverride(rule.parent == null ? null : rule.parent.cloneDependency(release),
                    rule.hasTargetRange() ? cloneRange(dep, rule.targetRange, release) : DependencyVersion.ZEROED,
                    dep));
        }
        return out;
    }

    // clone_range: parsed again as an npm range, as the dependency's clone parses its spec
    private static DependencyVersion cloneRange(Dependency dep, DependencyVersion range, BunRelease release) {
        DependencyVersion parsed = DependencyVersion.parseWithTag(dep.name, dep.nameHash, range.literal,
                DependencyVersion.Tag.NPM, range.literal, release);
        return parsed == null ? DependencyVersion.ZEROED : parsed;
    }

    /**
     * {@code push_scoped}: replaces the rule of the same parent, parent range, target and target range.
     */
    void pushScoped(ScopedOverride rule) {
        if (scopedNames.contains(rule.dep.nameHash)) {
            for (int i = 0; i < scoped.size(); i++) {
                ScopedOverride existing = scoped.get(i);
                if (existing.dep.nameHash == rule.dep.nameHash && sameParent(existing.parent, rule.parent) &&
                    existing.targetRange.literal.equals(rule.targetRange.literal)) {
                    scoped.set(i, rule);
                    return;
                }
            }
        } else {
            scopedNames.add(rule.dep.nameHash);
        }
        scoped.add(rule);
    }

    private static boolean sameParent(@Nullable Dependency a, @Nullable Dependency b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.nameHash == b.nameHash && a.version.literal.equals(b.version.literal);
    }

    /**
     * {@code put_lockfile_rule} (from 1.4.0), a row of bun.lock's overrides: false where the value, the parent's range
     * or the target's does not parse, a range as no npm range.
     */
    boolean putLockfileRule(@Nullable PackageSelector parent, PackageSelector target, String value,
                            BunRelease release) {
        long nameHash = Wyhash11.stringHash(target.name);
        DependencyVersion targetRange = lockfileRange(target.name, nameHash, target.range, release);
        if (targetRange == null) {
            return false;
        }
        Dependency parentDep = null;
        if (parent != null) {
            long parentNameHash = Wyhash11.stringHash(parent.name);
            DependencyVersion version = lockfileRange(parent.name, parentNameHash, parent.range, release);
            if (version == null) {
                return false;
            }
            parentDep = new Dependency(parentNameHash, parent.name, version, Behavior.NONE);
        }
        DependencyVersion version = DependencyVersion.parse(target.name, nameHash, value, release);
        if (version == null) {
            return false;
        }
        Dependency dep = new Dependency(nameHash, target.name, version, Behavior.NONE);
        if (parentDep == null && targetRange.tag != DependencyVersion.Tag.NPM) {
            map.put(nameHash, dep);
        } else {
            pushScoped(new ScopedOverride(parentDep, targetRange, dep));
        }
        return true;
    }

    private static @Nullable DependencyVersion lockfileRange(String name, long nameHash, String range,
                                                             BunRelease release) {
        if (range.isEmpty()) {
            return DependencyVersion.ZEROED;
        }
        DependencyVersion version = DependencyVersion.parse(name, nameHash, range, release);
        return version != null && version.tag == DependencyVersion.Tag.NPM ? version : null;
    }

    /**
     * {@code parseAppend} for the root {@code rootPackage}, whose dependencies are parsed, and the members its
     * workspaces name, read from {@code manifests} by location.
     */
    void parseAppend(Lockfile lockfile, Package rootPackage, Json.Expr expr, Workspaces workspaceNames,
                     Map<String, String> manifests) {
        parseAppend(lockfile, rootPackage, expr, workspaceNames, manifests, new ArrayList<>());
    }

    /**
     * @param warnings what bun warns of the rules it skips, which changes nothing it installs (from 1.4.0).
     */
    void parseAppend(Lockfile lockfile, Package rootPackage, Json.Expr expr, Workspaces workspaceNames,
                     Map<String, String> manifests, List<String> warnings) {
        if (lockfile.release.readsScopedOverrides()) {
            Json.Expr overrides = expr.get("overrides");
            Json.Expr resolutions = overrides == null ? expr.get("resolutions") : null;
            if (overrides == null && resolutions == null) {
                return;
            }
            ParseContext ctx = new ParseContext(resolutions != null, lockfile, rootPackage, workspaceNames, manifests,
                    warnings);
            if (overrides != null) {
                parseScopedFromOverrides(ctx, overrides);
            } else {
                parseScopedFromResolutions(ctx, resolutions);
            }
            return;
        }
        Json.Expr overrides = expr.get("overrides");
        if (overrides != null) {
            parseFromOverrides(lockfile, rootPackage, overrides);
            return;
        }
        Json.Expr resolutions = expr.get("resolutions");
        if (resolutions != null) {
            parseFromResolutions(lockfile, rootPackage, resolutions);
        }
    }

    void parseFromOverrides(Lockfile lockfile, Package rootPackage, Json.Expr expr) {
        if (!expr.isObject()) {
            throw new IllegalArgumentException("Invalid");
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k == null || k.isEmpty()) {
                continue;
            }
            long nameHash = Wyhash11.stringHash(k);
            Json.Expr valueExpr = prop.value;
            Json.Expr value;
            if (valueExpr.isString()) {
                value = valueExpr;
            } else if (valueExpr.isObject()) {
                Json.Expr dot = valueExpr.get(".");
                if (dot == null) {
                    continue;
                }
                if (!dot.isString()) {
                    continue;
                }
                value = dot;
            } else {
                continue;
            }
            String versionStr = value.asString();
            if (versionStr.startsWith("patch:")) {
                continue;
            }
            Dependency version = parseOverrideValue(lockfile, rootPackage, k, versionStr);
            if (version != null) {
                map.put(nameHash, version);
            }
        }
    }

    void parseFromResolutions(Lockfile lockfile, Package rootPackage, Json.Expr expr) {
        if (!expr.isObject()) {
            return;
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k.startsWith("**/")) {
                k = k.substring(3);
            }
            if (k.isEmpty()) {
                continue;
            }
            Json.Expr value = prop.value;
            if (!value.isString()) {
                continue;
            }
            if (k.charAt(0) == '@') {
                int firstSlash = k.indexOf('/');
                if (firstSlash < 0) {
                    continue;
                }
                if (k.indexOf('/', firstSlash + 1) >= 0) {
                    continue;
                }
            } else if (k.indexOf('/') >= 0) {
                continue;
            }
            String versionStr = value.asString();
            if (versionStr.startsWith("patch:")) {
                continue;
            }
            Dependency version = parseOverrideValue(lockfile, rootPackage, k, versionStr);
            if (version != null) {
                map.put(Wyhash11.stringHash(k), version);
            }
        }
    }

    /**
     * A {@code $name} value is the root's own dependency of that name, the whole dependency: the lock writes that
     * dependency's name and literal for the key.
     */
    static @Nullable Dependency parseOverrideValue(Lockfile lockfile, Package rootPackage, String key, String value) {
        if (value.isEmpty()) {
            return null;
        }
        if (value.charAt(0) == '$') {
            String refName = value.substring(1);
            for (Dependency dep : lockfile.dependenciesOf(rootPackage)) {
                if (dep.name.equals(refName)) {
                    return dep;
                }
            }
            return null;
        }
        long nameHash = Wyhash11.stringHash(key);
        DependencyVersion version = DependencyVersion.parse(key, nameHash, value, lockfile.release);
        if (version == null) {
            return null;
        }
        return new Dependency(nameHash, key, version, Behavior.NONE);
    }

    // from 1.4.0: OverrideMap.rs's parse_from_overrides and parse_from_resolutions

    private static final class ParseContext {
        final boolean resolutions;
        final Lockfile lockfile;
        final Package rootPackage;
        final Workspaces workspaceNames;
        final Map<String, String> manifests;
        final List<String> warnings;

        ParseContext(boolean resolutions, Lockfile lockfile, Package rootPackage, Workspaces workspaceNames,
                     Map<String, String> manifests, List<String> warnings) {
            this.resolutions = resolutions;
            this.lockfile = lockfile;
            this.rootPackage = rootPackage;
            this.workspaceNames = workspaceNames;
            this.manifests = manifests;
            this.warnings = warnings;
        }

        String label() {
            return resolutions ? "resolution" : "override";
        }

        String jsonName() {
            return resolutions ? "resolutions" : "overrides";
        }

        String missingNameMessage() {
            return resolutions ? "Missing resolution package name" : "Missing overridden package name";
        }

        void warn(String message) {
            warnings.add(message);
        }
    }

    private static boolean isCommentKey(String key) {
        return key.startsWith("//");
    }

    // https://docs.npmjs.com/cli/v9/configuring-npm/package-json#overrides
    private void parseScopedFromOverrides(ParseContext ctx, Json.Expr expr) {
        if (!expr.isObject()) {
            ctx.warn("\"overrides\" must be an object");
            return;
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k == null || k.isEmpty()) {
                ctx.warn(ctx.missingNameMessage());
                continue;
            }
            if (isCommentKey(k)) {
                continue;
            }
            Json.Expr valueExpr = prop.value;
            String value = valueExpr.asString();
            if (value != null) {
                parseStringRule(ctx, k, value);
                continue;
            }
            if (!valueExpr.isObject()) {
                ctx.warn("Invalid override value for \"" + k + "\"");
                continue;
            }
            PackageSelector parentSelector;
            try {
                parentSelector = OverrideSelector.parsePackageSegment(k);
            } catch (OverrideSelector.Invalid e) {
                warnSelectorError(ctx, k, e.kind);
                continue;
            }
            Dependency parent = parseParent(ctx, parentSelector);
            if (parent == null) {
                continue;
            }
            for (Json.Property child : valueExpr.properties()) {
                String childKey = child.key.asString();
                if (childKey == null || isCommentKey(childKey)) {
                    continue;
                }
                if (child.value.isObject()) {
                    warnSelectorError(ctx, childKey, OverrideSelector.Kind.TOO_DEEP);
                    continue;
                }
                String childValue = child.value.asString();
                if (childValue == null) {
                    ctx.warn("Invalid override value for \"" + childKey + "\"");
                    continue;
                }
                if (".".equals(childKey)) {
                    putRule(ctx, null, parentSelector, childValue);
                    continue;
                }
                OverrideSelector selector;
                try {
                    selector = OverrideSelector.parse(childKey);
                } catch (OverrideSelector.Invalid e) {
                    warnSelectorError(ctx, childKey, e.kind);
                    continue;
                }
                if (selector.parent != null) {
                    warnSelectorError(ctx, childKey, OverrideSelector.Kind.TOO_DEEP);
                    continue;
                }
                putRule(ctx, parent, selector.target, childValue);
            }
        }
    }

    // yarn classic: https://classic.yarnpkg.com/lang/en/docs/selective-version-resolutions/
    // yarn berry: https://yarnpkg.com/configuration/manifest#resolutions
    private void parseScopedFromResolutions(ParseContext ctx, Json.Expr expr) {
        if (!expr.isObject()) {
            ctx.warn("\"resolutions\" must be an object with string values");
            return;
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k == null || k.isEmpty()) {
                ctx.warn(ctx.missingNameMessage());
                continue;
            }
            if (isCommentKey(k)) {
                continue;
            }
            String value = prop.value.asString();
            if (value == null) {
                ctx.warn("Invalid resolution value for \"" + k + "\"");
                continue;
            }
            parseStringRule(ctx, k, value);
        }
    }

    private void parseStringRule(ParseContext ctx, String key, String value) {
        OverrideSelector selector;
        try {
            selector = OverrideSelector.parse(key);
        } catch (OverrideSelector.Invalid e) {
            warnSelectorError(ctx, key, e.kind);
            return;
        }
        Dependency parent = null;
        if (selector.parent != null) {
            parent = parseParent(ctx, selector.parent);
            if (parent == null) {
                return;
            }
        }
        putRule(ctx, parent, selector.target, value);
    }

    private void putRule(ParseContext ctx, @Nullable Dependency parent, PackageSelector target, String value) {
        if (value.startsWith("patch:")) {
            ctx.warn("Bun currently does not support patched package \"" + ctx.jsonName() + "\"");
            return;
        }
        Dependency dep = parseScopedOverrideValue(ctx, target.name, value);
        if (dep == null) {
            return;
        }
        DependencyVersion targetRange = parseRange(ctx, dep.name, dep.nameHash, target.range);
        if (targetRange == null) {
            return;
        }
        if (parent == null && targetRange.tag != DependencyVersion.Tag.NPM) {
            map.put(dep.nameHash, dep);
        } else {
            pushScoped(new ScopedOverride(parent, targetRange, dep));
        }
    }

    private static void warnSelectorError(ParseContext ctx, String key, OverrideSelector.Kind kind) {
        switch (kind) {
            case EMPTY_NAME:
                ctx.warn(ctx.missingNameMessage());
                break;
            case INVALID_NAME:
                ctx.warn("Invalid package name \"" + key + "\"");
                break;
            case TOO_DEEP:
                ctx.warn("Bun currently only supports one level of nested \"" + ctx.jsonName() + "\"");
                break;
            default:
                ctx.warn("Missing version range after \"" + key + "\"");
                break;
        }
    }

    private static @Nullable Dependency parseParent(ParseContext ctx, PackageSelector selector) {
        long nameHash = Wyhash11.stringHash(selector.name);
        DependencyVersion version = parseRange(ctx, selector.name, nameHash, selector.range);
        return version == null ? null : new Dependency(nameHash, selector.name, version, Behavior.NONE);
    }

    private static @Nullable DependencyVersion parseRange(ParseContext ctx, String name, long nameHash,
                                                          String range) {
        if (range.isEmpty()) {
            return DependencyVersion.ZEROED;
        }
        DependencyVersion version = DependencyVersion.parse(name, nameHash, range, ctx.lockfile.release);
        if (version != null && version.tag == DependencyVersion.Tag.NPM) {
            return version;
        }
        ctx.warn("Invalid version range \"" + range + "\" for \"" + name + "\"");
        return null;
    }

    /**
     * {@code parse_override_value}: the rule is always stored under {@code key}; a {@code $name} value only supplies
     * the spec, the root's own declaration of the name, else the one every workspace declares.
     */
    private static @Nullable Dependency parseScopedOverrideValue(ParseContext ctx, String key, String value) {
        BunRelease release = ctx.lockfile.release;
        if (value.isEmpty()) {
            ctx.warn("Missing " + ctx.label() + " value");
            return null;
        }
        if ("-".equals(value)) {
            ctx.warn("Removing \"" + key + "\" with \"-\" is not supported");
            return null;
        }
        long nameHash = Wyhash11.stringHash(key);
        String literal = value;
        DependencyVersion.Tag tag = null;
        if (value.charAt(0) == '$') {
            String refName = value.substring(1);
            Dependency rootRef = null;
            for (Dependency dep : ctx.lockfile.dependenciesOf(ctx.rootPackage)) {
                // from 1.4.1 the root's workspaces entries are no declarations
                if ((!release.readsAReferencedSpecAsWritten() || !dep.behavior.isWorkspace()) &&
                    dep.name.equals(refName)) {
                    rootRef = dep;
                    break;
                }
            }
            if (rootRef != null) {
                literal = rootRef.version.literal;
                // 1.4.0 reads it as the kind the root's dependency is
                tag = release.readsAReferencedSpecAsWritten() ? null : rootRef.version.tag;
            } else {
                String declared;
                try {
                    declared = workspaceRefLiteral(ctx, refName);
                } catch (Ambiguous e) {
                    ctx.warn("Could not resolve \"" + value + "\": workspaces declare different versions of \"" +
                             refName + "\"");
                    return null;
                }
                if (declared == null) {
                    ctx.warn("Could not resolve \"" + value + "\": \"" + refName + "\" is not in dependencies");
                    return null;
                }
                literal = declared;
            }
        }
        DependencyVersion version = DependencyVersion.parseWithOptionalTag(key, nameHash, literal, tag, release);
        if (version == null) {
            ctx.warn("Invalid " + ctx.label() + " value \"" + value + "\"");
            return null;
        }
        return new Dependency(nameHash, key, version, Behavior.NONE);
    }

    private static final class Ambiguous extends Exception {
        Ambiguous() {
            super(null, null, false, false);
        }
    }

    // the one literal the root's workspaces declare for the name, null where none does
    private static @Nullable String workspaceRefLiteral(ParseContext ctx, String refName) throws Ambiguous {
        String found = null;
        for (String relativeDir : ctx.workspaceNames.map.keySet()) {
            String manifest = ctx.manifests.get(relativeDir);
            if (manifest == null) {
                continue;
            }
            Json.Expr root;
            try {
                root = Json.parse(manifest);
            } catch (Json.SyntaxError e) {
                continue;
            }
            String literal = declaredDependency(root, refName);
            if (literal == null) {
                continue;
            }
            if (found == null) {
                found = literal;
            } else if (!found.equals(literal)) {
                throw new Ambiguous();
            }
        }
        return found;
    }

    private static final List<String> DEPENDENCY_GROUPS = Collections.unmodifiableList(java.util.Arrays.asList(
            "dependencies", "devDependencies", "optionalDependencies", "peerDependencies"));

    // the first string-valued entry for the name across the four dependency sections of a package.json object
    private static @Nullable String declaredDependency(Json.Expr root, String name) {
        for (String group : DEPENDENCY_GROUPS) {
            Json.Expr deps = root.get(group);
            Json.Expr value = deps == null ? null : deps.get(name);
            if (value != null && value.isString()) {
                return value.asString();
            }
        }
        return null;
    }
}
