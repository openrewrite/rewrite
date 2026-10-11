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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Port of bun's src/install/lockfile/Package.zig with Package/Meta.zig and Package/Scripts.zig (bun 1.3.10). */
final class Package {

    static final int INVALID_PACKAGE_ID = Integer.MAX_VALUE;

    enum Origin {
        LOCAL, NPM, TARBALL
    }

    static final class Meta {
        Origin origin = Origin.NPM;
        int arch = Negatable.ARCHITECTURE.allValue;
        int os = Negatable.OPERATING_SYSTEM.allValue;
        int id = INVALID_PACKAGE_ID;
        Integrity integrity = Integrity.UNKNOWN;
        boolean hasInstallScript;

        Meta copy(int id) {
            Meta m = new Meta();
            m.origin = origin;
            m.arch = arch;
            m.os = os;
            m.id = id;
            m.integrity = integrity;
            m.hasInstallScript = hasInstallScript;
            return m;
        }
    }

    static final class Scripts {
        static final String[] NAMES = {"preinstall", "install", "postinstall", "preprepare", "prepare", "postprepare"};

        final String[] hooks = {"", "", "", "", "", ""};
        boolean filled;

        boolean eql(Scripts r) {
            return Arrays.equals(hooks, r.hooks);
        }

        boolean hasAny() {
            for (String hook : hooks) {
                if (!hook.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        Scripts copy() {
            Scripts s = new Scripts();
            System.arraycopy(hooks, 0, s.hooks, 0, hooks.length);
            s.filled = filled;
            return s;
        }

        void parseAlloc(Json.Expr json) {
            Json.Expr scripts = json.get("scripts");
            if (scripts != null && scripts.isObject()) {
                for (int i = 0; i < NAMES.length; i++) {
                    Json.Expr script = scripts.get(NAMES[i]);
                    String input = script == null ? null : script.asString();
                    if (input != null) {
                        hooks[i] = input;
                    }
                }
            }
        }
    }

    String name = "";
    long nameHash;
    Resolution resolution = Resolution.UNINITIALIZED;
    int dependenciesOff;
    int dependenciesLen;
    Meta meta = new Meta();
    Bin bin = Bin.NONE;
    Scripts scripts = new Scripts();

    int dependenciesEnd() {
        return dependenciesOff + dependenciesLen;
    }

    private static final String[] GROUP_PROPS = {"workspaces", "dependencies", "devDependencies",
            "optionalDependencies", "peerDependencies"};
    private static final int[] GROUP_BEHAVIORS = {Behavior.WORKSPACE, Behavior.PROD, Behavior.DEV, Behavior.OPTIONAL,
            Behavior.PEER};

    private static boolean groupEnabled(Features features, int g) {
        switch (g) {
            case 0:
                return features.workspaces;
            case 1:
                return features.dependencies;
            case 2:
                return features.devDependencies;
            case 3:
                return features.optionalDependencies;
            default:
                return features.peerDependencies;
        }
    }

    /**
     * {@code parseWithJSON} for the root ({@link Features#MAIN}) or a workspace member ({@link Features#WORKSPACE}) at
     * {@code sourceDir}; the members the root names are read from {@code manifests}, by location.
     */
    void parseWithJSON(Lockfile lockfile, BunOptions options, Log log, String sourceDir, Json.Expr json,
                       Features features, Map<String, String> manifests) {
        parseWithJSON(lockfile, options, log, sourceDir, json, features, manifests, null);
    }

    /**
     * @param resolved what a folder or workspace resolver resolves a package that is not the root to.
     */
    void parseWithJSON(Lockfile lockfile, BunOptions options, Log log, String sourceDir, Json.Expr json,
                       Features features, Map<String, String> manifests, @Nullable Resolution resolved) {
        meta.origin = features.isMain ? Origin.LOCAL : Origin.NPM;
        name = "";
        nameHash = 0;

        Workspaces workspaceNames = new Workspaces();
        Set<Long> optionalPeerDependencies = new HashSet<>();
        Json.Expr peerDependenciesMeta = json.get("peerDependenciesMeta");
        if (peerDependenciesMeta != null && peerDependenciesMeta.isObject()) {
            for (Json.Property prop : peerDependenciesMeta.properties()) {
                Json.Expr optional = prop.value.get("optional");
                if (optional == null || !Boolean.TRUE.equals(optional.asBool())) {
                    continue;
                }
                optionalPeerDependencies.add(Wyhash11.stringHash(prop.key.asString()));
            }
        }

        for (int g = 0; g < GROUP_PROPS.length; g++) {
            if (!groupEnabled(features, g)) {
                continue;
            }
            boolean isWorkspace = g == 0;
            Json.Expr dependencies = json.get(GROUP_PROPS[g]);
            if (dependencies == null) {
                continue;
            }
            if (dependencies.isArray()) {
                if (!isWorkspace) {
                    log.addError(GROUP_PROPS[g] + " expects a map of specifiers");
                    throw new IllegalArgumentException("InvalidPackageJSON");
                }
                workspaceNames.processNamesArray(log, dependencies.items(), relativeTo(sourceDir, manifests));
            } else if (dependencies.isObject()) {
                if (isWorkspace) {
                    Json.Expr packages = dependencies.get("packages");
                    if (packages != null) {
                        if (!packages.isArray()) {
                            log.addError("\"workspaces.packages\" expects an array of strings");
                            throw new IllegalArgumentException("InvalidPackageJSON");
                        }
                        workspaceNames.processNamesArray(log, packages.items(), relativeTo(sourceDir, manifests));
                    }
                    continue;
                }
                for (Json.Property item : dependencies.properties()) {
                    if (item.value.asString() == null) {
                        log.addError(GROUP_PROPS[g] + " expects a map of specifiers");
                        throw new IllegalArgumentException("InvalidPackageJSON");
                    }
                }
            } else {
                log.addError(isWorkspace ? "\"workspaces\" expects an array of strings" :
                        GROUP_PROPS[g] + " expects a map of specifiers");
                throw new IllegalArgumentException("InvalidPackageJSON");
            }
        }

        if (features.trustedDependencies) {
            Json.Expr trusted = json.get("trustedDependencies");
            if (trusted != null) {
                if (!trusted.isArray()) {
                    log.addError("trustedDependencies expects an array of strings");
                    throw new IllegalArgumentException("InvalidPackageJSON");
                }
                if (lockfile.trustedDependencies == null) {
                    lockfile.trustedDependencies = new LinkedHashSet<>();
                }
                for (Json.Expr item : trusted.items()) {
                    String trustedName = item.asString();
                    if (trustedName == null) {
                        log.addError("trustedDependencies expects an array of strings");
                        throw new IllegalArgumentException("InvalidPackageJSON");
                    }
                    lockfile.trustedDependencies.add((int) Wyhash11.stringHash(trustedName));
                }
            }
        }

        Json.Expr nameExpr = json.get("name");
        String nameStr = nameExpr == null ? null : nameExpr.asString();
        if (nameStr != null && !nameStr.isEmpty()) {
            name = nameStr;
            nameHash = Wyhash11.stringHash(nameStr);
        }
        if (features.isMain) {
            resolution = Resolution.ROOT;
        } else if (resolved != null) {
            resolution = resolved;
        }

        Json.Expr patchedDeps = json.get("patchedDependencies");
        if (patchedDeps != null) {
            for (Json.Property prop : patchedDeps.properties()) {
                if (prop.value.isString()) {
                    lockfile.patchedDependencies.put(Wyhash11.stringHash(prop.key.asString()), prop.value.asString());
                }
            }
        }

        bin = parseBin(json);
        scripts.parseAlloc(json);
        scripts.filled = true;

        Map<Long, Boolean> duplicateCheckerMap = new HashMap<>();
        List<Dependency> packageDependencies = new ArrayList<>();
        for (int g = 0; g < GROUP_PROPS.length; g++) {
            if (!groupEnabled(features, g)) {
                continue;
            }
            Behavior groupBehavior = Behavior.of(GROUP_BEHAVIORS[g]);
            if (g == 0) {
                Set<Integer> seenWorkspaceNames = new HashSet<>();
                for (Map.Entry<String, Workspaces.Entry> e : workspaceNames.map.entrySet()) {
                    Workspaces.Entry entry = e.getValue();
                    String path = e.getKey();
                    if (!seenWorkspaceNames.add((int) Wyhash11.stringHash(entry.name))) {
                        log.addError("Workspace name \"" + entry.name + "\" already exists");
                        throw new IllegalArgumentException("InstallFailed");
                    }
                    SemverVersion workspaceVersion = null;
                    if (entry.version != null) {
                        SemverVersion.ParseResult result = SemverVersion.parse(entry.version);
                        if (result.valid && result.wildcard == SemverQuery.Wildcard.NONE) {
                            workspaceVersion = result.version.min();
                        }
                    }
                    Dependency dep = parseDependency(lockfile, options, log, sourceDir, groupBehavior, features,
                            packageDependencies, duplicateCheckerMap, DependencyVersion.Tag.WORKSPACE, workspaceVersion,
                            entry.name, path);
                    if (dep != null) {
                        if (groupBehavior.isPeer() && optionalPeerDependencies.contains(dep.nameHash)) {
                            dep.behavior = dep.behavior.add(Behavior.OPTIONAL);
                        }
                        packageDependencies.add(dep);
                        lockfile.workspacePaths.put(dep.nameHash, dep.version.string());
                        if (workspaceVersion != null) {
                            lockfile.workspaceVersions.put(dep.nameHash, workspaceVersion);
                        }
                    }
                }
            } else {
                Json.Expr dependencies = json.get(GROUP_PROPS[g]);
                if (dependencies == null) {
                    continue;
                }
                for (Json.Property item : dependencies.properties()) {
                    String externalName = item.key.asString();
                    String version = item.value.asString() == null ? "" : item.value.asString();
                    Dependency dep = parseDependency(lockfile, options, log, sourceDir, groupBehavior, features,
                            packageDependencies, duplicateCheckerMap, null, null, externalName, version);
                    if (dep != null) {
                        if (groupBehavior.isPeer() && optionalPeerDependencies.contains(dep.nameHash)) {
                            dep.behavior = dep.behavior.add(Behavior.OPTIONAL);
                        }
                        packageDependencies.add(dep);
                    }
                }
            }
        }

        packageDependencies.sort(Dependency::compare);
        dependenciesOff = lockfile.dependencies.size();
        dependenciesLen = packageDependencies.size();
        for (Dependency dep : packageDependencies) {
            lockfile.dependencies.add(dep);
            lockfile.resolutions.add(INVALID_PACKAGE_ID);
        }

        if (features.isMain) {
            lockfile.overrides.parseAppend(lockfile, this, json);
            boolean foundAnyCatalogOrCatalogObject = false;
            boolean hasWorkspaces = false;
            Json.Expr workspacesExpr = json.get("workspaces");
            if (workspacesExpr != null) {
                foundAnyCatalogOrCatalogObject = lockfile.catalogs.parseAppend(log, workspacesExpr);
                hasWorkspaces = true;
            }
            if (!foundAnyCatalogOrCatalogObject && hasWorkspaces) {
                lockfile.catalogs.parseAppend(log, json);
            }
        }
    }

    /**
     * {@code fromNPM}: the package a registry version resolves to, its dependencies appended unresolved in bun's group
     * order; a repeated peer or optional name is skipped, except that an optional one replaces the plain dependency.
     */
    static Package fromNPM(Lockfile lockfile, PackageManifest manifest, SemverVersion version,
                           PackageManifest.PackageVersion packageVersion, Features features) {
        Package pkg = new Package();
        List<Dependency> dependencies = new ArrayList<>();
        Object[][] groups = {
                {features.dependencies, packageVersion.dependencies, Behavior.PROD},
                {features.devDependencies, Collections.<String>emptyList(), Behavior.DEV},
                {features.optionalDependencies, packageVersion.optionalDependencies, Behavior.OPTIONAL},
                {features.peerDependencies, packageVersion.peerDependencies, Behavior.PEER},
        };
        for (Object[] group : groups) {
            if (!(Boolean) group[0]) {
                continue;
            }
            @SuppressWarnings("unchecked") List<String> map = (List<String>) group[1];
            int groupBit = (Integer) group[2];
            boolean isPeer = groupBit == Behavior.PEER;
            boolean isOptional = groupBit == Behavior.OPTIONAL;
            list:
            for (int i = 0; i < map.size() / 2; i++) {
                String key = map.get(2 * i);
                long keyHash = Wyhash11.stringHash(key);
                Integer duplicateAt = null;
                if (isPeer || groupBit == Behavior.DEV || isOptional) {
                    for (int j = 0; j < dependencies.size(); j++) {
                        if (dependencies.get(j).nameHash == keyHash) {
                            if (isOptional) {
                                duplicateAt = j;
                                break;
                            }
                            continue list;
                        }
                    }
                }
                Behavior behavior = Behavior.of(groupBit);
                if (isPeer) {
                    behavior = behavior.set(Behavior.OPTIONAL, i < packageVersion.nonOptionalPeerDependenciesStart);
                }
                List<Long> bundled = packageVersion.bundledDependencies;
                if (packageVersion.allDependenciesBundled() || bundled != null && bundled.contains(keyHash)) {
                    behavior = behavior.add(Behavior.BUNDLED);
                }
                DependencyVersion parsed = DependencyVersion.parse(key, keyHash, map.get(2 * i + 1));
                Dependency dependency = new Dependency(keyHash, key, parsed == null ? DependencyVersion.ZEROED : parsed,
                        behavior);
                if (duplicateAt != null) {
                    dependencies.remove((int) duplicateAt);
                    dependencies.add(dependency);
                    continue;
                }
                dependencies.add(dependency);
            }
        }
        pkg.name = manifest.name;
        pkg.nameHash = Wyhash11.stringHash(manifest.name);
        pkg.resolution = Resolution.npm(version, packageVersion.tarballUrl);
        pkg.bin = packageVersion.bin;
        pkg.meta.arch = packageVersion.cpu;
        pkg.meta.os = packageVersion.os;
        pkg.meta.integrity = packageVersion.integrity;
        pkg.meta.hasInstallScript = packageVersion.hasInstallScript;
        pkg.dependenciesOff = lockfile.dependencies.size();
        pkg.dependenciesLen = dependencies.size();
        for (Dependency dep : dependencies) {
            lockfile.dependencies.add(dep);
            lockfile.resolutions.add(INVALID_PACKAGE_ID);
        }
        return pkg;
    }

    // the manifests of the project by location relative to the package.json being read
    private static Map<String, String> relativeTo(String sourceDir, Map<String, String> manifests) {
        if (sourceDir.isEmpty()) {
            return manifests;
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : manifests.entrySet()) {
            String relative = RootRelativePath.relative(sourceDir.replace('\\', '/'),
                    e.getKey().replace('\\', '/'));
            if (!relative.startsWith("..")) {
                out.put(relative, e.getValue());
            }
        }
        return out;
    }

    private static Bin parseBin(Json.Expr json) {
        Json.Expr bin = json.get("bin");
        if (bin != null) {
            if (bin.isObject()) {
                List<Json.Property> props = bin.properties();
                if (props.isEmpty()) {
                    return Bin.NONE;
                }
                List<String> values = new ArrayList<>();
                for (Json.Property prop : props) {
                    String key = prop.key.asString();
                    String value = prop.value.asString();
                    if (key == null || value == null) {
                        return Bin.NONE;
                    }
                    values.add(key);
                    values.add(value);
                }
                return new Bin(props.size() == 1 ? Bin.Tag.NAMED_FILE : Bin.Tag.MAP, values);
            }
            String str = bin.asString();
            if (str != null && !str.isEmpty()) {
                return new Bin(Bin.Tag.FILE, Collections.singletonList(str));
            }
        }
        Json.Expr dirs = json.get("directories");
        if (dirs != null) {
            Json.Expr binProp = dirs.get("bin");
            String str = binProp == null ? null : binProp.asString();
            if (str != null && !str.isEmpty()) {
                return new Bin(Bin.Tag.DIR, Collections.singletonList(str));
            }
        }
        return Bin.NONE;
    }

    private static @Nullable Dependency parseDependency(Lockfile lockfile, BunOptions options, Log log,
                                                        String sourceDir, Behavior groupBehavior, Features features,
                                                        List<Dependency> packageDependencies,
                                                        Map<Long, Boolean> duplicateCheckerMap,
                                                        DependencyVersion.@Nullable Tag tag,
                                                        @Nullable SemverVersion workspaceVer, String externalAlias,
                                                        String version) {
        long aliasHash = Wyhash11.stringHash(externalAlias);
        DependencyVersion dependencyVersion = DependencyVersion.parseWithOptionalTag(externalAlias, aliasHash, version,
                tag);
        if (dependencyVersion == null) {
            dependencyVersion = DependencyVersion.ZEROED;
        }
        SemverQuery.Group workspaceRange = null;
        long nameHash;
        switch (dependencyVersion.tag) {
            case NPM:
                nameHash = Wyhash11.stringHash(dependencyVersion.npm().name);
                break;
            case WORKSPACE:
                nameHash = aliasHash;
                if (version.startsWith("workspace:")) {
                    String input = version.substring("workspace:".length());
                    String trimmed = Strings.trim(input, Strings.WHITESPACE_CHARS);
                    if (trimmed.length() != 1 ||
                        trimmed.charAt(0) != '*' && trimmed.charAt(0) != '^' && trimmed.charAt(0) != '~') {
                        int at = Math.max(input.lastIndexOf('@'), 0);
                        if (at > 0) {
                            workspaceRange = SemverQuery.parse(input.substring(at + 1));
                            nameHash = Wyhash11.stringHash(input.substring(0, at));
                        } else {
                            workspaceRange = SemverQuery.parse(input);
                        }
                    }
                }
                break;
            default:
                nameHash = aliasHash;
                break;
        }

        String workspacePath = null;
        SemverVersion workspaceVersion = workspaceVer;
        if (tag == null) {
            workspacePath = lockfile.workspacePaths.get(nameHash);
            workspaceVersion = lockfile.workspaceVersions.get(nameHash);
        }

        switch (dependencyVersion.tag) {
            case FOLDER: {
                String relative = RootRelativePath.join(sourceDir.replace('\\', '/'),
                        dependencyVersion.string().replace('\\', '/'));
                dependencyVersion = new DependencyVersion(DependencyVersion.Tag.FOLDER, dependencyVersion.literal,
                        relative.isEmpty() ? "." : relative);
                break;
            }
            case NPM: {
                DependencyVersion.NpmInfo npm = dependencyVersion.npm();
                if (workspaceVersion != null) {
                    if (options.isLinkWorkspacePackages() && npm.version.satisfies(workspaceVersion)) {
                        String path = workspacePath;
                        DependencyVersion dep = DependencyVersion.parseWithTag(externalAlias, aliasHash, path,
                                DependencyVersion.Tag.WORKSPACE, path);
                        if (dep != null) {
                            dependencyVersion = new DependencyVersion(dep.tag, dependencyVersion.literal, dep.value);
                        }
                    } else {
                        for (int i = 0; i < packageDependencies.size(); i++) {
                            Dependency dep = packageDependencies.get(i);
                            if (dep.nameHash == nameHash && dep.behavior.isWorkspace()) {
                                packageDependencies.set(i,
                                        new Dependency(aliasHash, externalAlias, dependencyVersion, groupBehavior));
                                return null;
                            }
                        }
                    }
                }
                break;
            }
            case WORKSPACE: {
                if (workspacePath != null) {
                    if (workspaceRange != null) {
                        if (workspaceVersion != null && workspaceRange.satisfies(workspaceVersion)) {
                            dependencyVersion = dependencyVersion.asWorkspace(workspacePath);
                            break;
                        }
                        String versionLiteral = Strings.trim(workspaceRange.input, Strings.WHITESPACE_CHARS);
                        if (versionLiteral.isEmpty() || workspaceRange.isStar() ||
                            SemverVersion.isTaggedVersionOnly(versionLiteral)) {
                            dependencyVersion = dependencyVersion.asWorkspace(workspacePath);
                            break;
                        }
                        log.addError("No matching version for workspace dependency \"" + externalAlias +
                                     "\". Version: \"" + dependencyVersion.literal + "\"");
                        throw new IllegalArgumentException("InstallFailed");
                    }
                    dependencyVersion = dependencyVersion.asWorkspace(workspacePath);
                } else {
                    String workspace = dependencyVersion.string();
                    String path = "*".equals(workspace) ? "*" :
                            RootRelativePath.join(sourceDir.replace('\\', '/'), workspace.replace('\\', '/'));
                    dependencyVersion = dependencyVersion.asWorkspace(path);
                    boolean foundMatchingWorkspace = lockfile.workspacePaths.containsKey(nameHash);
                    if (!foundMatchingWorkspace) {
                        lockfile.workspacePaths.put(nameHash, "");
                    }
                    if (workspaceVersion != null) {
                        lockfile.workspaceVersions.put(nameHash, workspaceVersion);
                        for (Dependency packageDep : packageDependencies) {
                            boolean matches;
                            if (packageDep.version.tag == DependencyVersion.Tag.NPM) {
                                matches = Wyhash11.stringHash(packageDep.realname()) == nameHash &&
                                          packageDep.version.npm().version.satisfies(workspaceVersion);
                            } else if (packageDep.version.tag == DependencyVersion.Tag.WORKSPACE) {
                                matches = foundMatchingWorkspace &&
                                          Wyhash11.stringHash(packageDep.realname()) == nameHash;
                            } else {
                                matches = false;
                            }
                            if (matches) {
                                packageDep.version = dependencyVersion;
                                lockfile.workspacePaths.put(nameHash, path);
                                return null;
                            }
                        }
                    } else if (foundMatchingWorkspace) {
                        for (Dependency packageDep : packageDependencies) {
                            if (packageDep.version.tag == DependencyVersion.Tag.WORKSPACE &&
                                Wyhash11.stringHash(packageDep.realname()) == nameHash) {
                                packageDep.version = dependencyVersion;
                                return null;
                            }
                        }
                        throw new IllegalArgumentException("InstallFailed");
                    }
                    lockfile.workspacePaths.put(nameHash, path);
                }
                break;
            }
            default:
                break;
        }

        Dependency thisDep = new Dependency(aliasHash, externalAlias, dependencyVersion, groupBehavior);
        if (features.checkForDuplicateDependencies && !groupBehavior.isPeer() && !groupBehavior.isWorkspace()) {
            if (duplicateCheckerMap.containsKey(aliasHash)) {
                if (groupBehavior.isOptional()) {
                    for (int i = 0; i < packageDependencies.size(); i++) {
                        if (packageDependencies.get(i).nameHash == thisDep.nameHash) {
                            packageDependencies.set(i, thisDep);
                            break;
                        }
                    }
                    return null;
                }
            }
            duplicateCheckerMap.put(aliasHash, true);
        }
        return thisDep;
    }

    boolean containsDependency(int id) {
        return id >= dependenciesOff && id < dependenciesOff + dependenciesLen;
    }

    @Override
    public String toString() {
        return name + "@" + resolution;
    }
}
