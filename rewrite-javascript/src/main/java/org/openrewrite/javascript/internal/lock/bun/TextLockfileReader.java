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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/lockfile/bun.lock.zig {@code parseIntoBinaryLockfile}, {@code PkgMap.findResolution} and
 * {@code parseAppendDependencies} (bun 1.3.10).
 */
final class TextLockfileReader {

    static final class ParseError extends IllegalArgumentException {
        final String error;

        ParseError(String error, String message) {
            super(message);
            this.error = error;
        }
    }

    private static final String[] GROUP_NAMES = {"dependencies", "devDependencies", "optionalDependencies",
            "peerDependencies"};
    private static final int[] GROUP_BEHAVIORS = {Behavior.PROD, Behavior.DEV, Behavior.OPTIONAL, Behavior.PEER};

    private final Lockfile lockfile = new Lockfile();
    private final BunOptions options;

    private TextLockfileReader(BunOptions options) {
        this.options = options;
    }

    /**
     * {@code loadFromDir} for a bun.lock: a text that does not parse is a {@code parse_file} error like any other.
     */
    static Lockfile load(String text, BunOptions options) {
        Json.Expr root;
        try {
            root = Json.parse(text);
        } catch (Json.SyntaxError e) {
            throw new ParseError("ParserError", e.getMessage());
        }
        return new TextLockfileReader(options).parseIntoBinaryLockfile(root);
    }

    private static ParseError error(String error, String message) {
        return new ParseError(error, message);
    }

    private Lockfile parseIntoBinaryLockfile(Json.Expr root) {
        Json.Expr lockfileVersionExpr = root.get("lockfileVersion");
        if (lockfileVersionExpr == null) {
            throw error("InvalidLockfileVersion", "Missing lockfile version");
        }
        double num = lockfileVersionExpr.asNumber();
        if (lockfileVersionExpr.kind != Json.Kind.NUMBER || num < 0 || num > 4294967295.0 || num != Math.floor(num)) {
            throw error("InvalidLockfileVersion", "Invalid lockfile version");
        }
        int lockfileVersion = (int) num;
        if (lockfileVersion != 0 && lockfileVersion != 1) {
            throw error("UnknownLockfileVersion", "Unknown lockfile version");
        }
        lockfile.textLockfileVersion = lockfileVersion;

        Json.Expr configVersionExpr = root.get("configVersion");
        if (configVersionExpr != null) {
            Integer configVersion = configVersionFromExpr(configVersionExpr);
            if (configVersion == null) {
                throw error("InvalidConfigVersion", "Invalid \"configVersion\". Expected a number");
            }
            lockfile.savedConfigVersion = configVersion;
        }

        Json.Expr trustedDependenciesExpr = root.get("trustedDependencies");
        if (trustedDependenciesExpr != null) {
            if (!trustedDependenciesExpr.isArray()) {
                throw error("InvalidTrustedDependenciesSet", "Expected an array");
            }
            LinkedHashSet<Integer> trusted = new LinkedHashSet<>();
            for (Json.Expr dep : trustedDependenciesExpr.items()) {
                if (!dep.isString()) {
                    throw error("InvalidTrustedDependenciesSet", "Expected a string");
                }
                trusted.add((int) Wyhash11.stringHash(dep.asString()));
            }
            lockfile.trustedDependencies = trusted;
        }

        Json.Expr patchedDependenciesExpr = root.get("patchedDependencies");
        if (patchedDependenciesExpr != null) {
            if (!patchedDependenciesExpr.isObject()) {
                throw error("InvalidPatchedDependencies", "Expected an object");
            }
            for (Json.Property prop : patchedDependenciesExpr.properties()) {
                if (!prop.value.isString()) {
                    throw error("InvalidPatchedDependencies", "Expected a string");
                }
                lockfile.patchedDependencies.put(Wyhash11.stringHash(prop.key.asString()), prop.value.asString());
            }
        }

        Json.Expr overridesExpr = root.get("overrides");
        if (overridesExpr != null) {
            if (!overridesExpr.isObject()) {
                throw error("InvalidOverridesObject", "Expected an object");
            }
            for (Json.Property prop : overridesExpr.properties()) {
                String nameStr = prop.key.asString();
                if (nameStr.isEmpty()) {
                    throw error("InvalidOverridesObject", "Expected a non-empty string");
                }
                if (!prop.value.isString()) {
                    throw error("InvalidOverridesObject", "Expected a string");
                }
                long nameHash = Wyhash11.stringHash(nameStr);
                DependencyVersion version = DependencyVersion.parse(nameStr, nameHash, prop.value.asString());
                if (version == null) {
                    throw error("InvalidOverridesObject", "Invalid override version");
                }
                lockfile.overrides.map.put(nameHash, new Dependency(nameHash, nameStr, version, Behavior.NONE));
            }
        }

        Json.Expr catalogExpr = root.get("catalog");
        if (catalogExpr != null) {
            if (!catalogExpr.isObject()) {
                throw error("InvalidCatalogObject", "Expected an object");
            }
            readCatalog(catalogExpr, lockfile.catalogs.defaultMap, "InvalidCatalogObject");
        }

        Json.Expr catalogsExpr = root.get("catalogs");
        if (catalogsExpr != null) {
            if (!catalogsExpr.isObject()) {
                throw error("InvalidCatalogsObject", "Expected an object");
            }
            for (Json.Property catalogProp : catalogsExpr.properties()) {
                String catalogName = catalogProp.key.asString();
                if (catalogName.isEmpty()) {
                    throw error("InvalidCatalogsObject", "Expected a non-empty string");
                }
                if (!catalogProp.value.isObject()) {
                    throw error("InvalidCatalogsObject", "Expected an object");
                }
                readCatalog(catalogProp.value, lockfile.catalogs.getOrPutGroup(catalogName), "InvalidCatalogsObject");
            }
        }

        Json.Expr workspacesObj = root.getObject("workspaces");
        if (workspacesObj == null) {
            throw error("InvalidWorkspaceObject", "Missing a workspaces object property");
        }

        Json.Expr maybeRootPkg = null;
        for (Json.Property prop : workspacesObj.properties()) {
            if (!prop.value.isObject()) {
                throw error("InvalidWorkspaceObject", "Expected an object");
            }
            String path = prop.key.asString();
            if (path.isEmpty()) {
                if (maybeRootPkg != null) {
                    throw error("InvalidWorkspaceObject", "Duplicate root package");
                }
                maybeRootPkg = prop.value;
                continue;
            }
            Json.Expr nameExpr = prop.value.get("name");
            if (nameExpr == null || !nameExpr.isString()) {
                throw error("InvalidWorkspaceObject", "Expected a string name property");
            }
            long nameHash = Wyhash11.stringHash(nameExpr.asString());
            lockfile.workspacePaths.put(nameHash, path);
            Json.Expr versionExpr = prop.value.get("version");
            if (versionExpr != null) {
                if (!versionExpr.isString()) {
                    throw error("InvalidWorkspaceObject", "Expected a string version property");
                }
                SemverVersion.ParseResult parsed = SemverVersion.parse(versionExpr.asString());
                if (!parsed.valid) {
                    throw error("InvalidSemver", "Invalid semver version");
                }
                lockfile.workspaceVersions.put(nameHash, parsed.version.min());
            }
        }

        if (maybeRootPkg == null) {
            throw error("InvalidWorkspaceObject", "Expected root package");
        }
        Json.Expr rootPkgExpr = maybeRootPkg;

        {
            Json.Expr nameExpr = rootPkgExpr.get("name");
            String maybeName = null;
            if (nameExpr != null) {
                maybeName = nameExpr.asString();
                if (maybeName == null) {
                    throw error("InvalidWorkspaceObject", "Expected a string");
                }
            }
            int[] offLen = parseAppendDependencies(rootPkgExpr, false, null, Collections.<String>emptySet(),
                    workspacesObj);
            Package rootPkg = new Package();
            if (maybeName != null) {
                rootPkg.name = maybeName;
                rootPkg.nameHash = Wyhash11.stringHash(maybeName);
            }
            rootPkg.dependenciesOff = offLen[0];
            rootPkg.dependenciesLen = offLen[1];
            rootPkg.meta.id = 0;
            lockfile.packages.add(rootPkg);
            lockfile.getOrPutID(0, rootPkg.nameHash);
        }

        Map<String, Integer> pkgMap = new HashMap<>();
        int workspacePkgsOff = 1;
        int workspacePkgsLen = 0;

        if (lockfileVersion != 0) {
            workspaces:
            for (String workspacePath : lockfile.workspacePaths.values()) {
                for (Json.Property prop : workspacesObj.properties()) {
                    String path = prop.key.asString();
                    if (!path.equals(workspacePath)) {
                        continue;
                    }
                    Json.Expr value = prop.value;
                    Package pkg = new Package();
                    pkg.resolution = Resolution.of(Resolution.Tag.WORKSPACE, path);
                    String name = value.get("name").asString();
                    pkg.name = name;
                    pkg.nameHash = Wyhash11.stringHash(name);
                    int[] offLen = parseAppendDependencies(value, false, null, Collections.<String>emptySet(), null);
                    pkg.dependenciesOff = offLen[0];
                    pkg.dependenciesLen = offLen[1];
                    Json.Expr binExpr = value.get("bin");
                    Json.Expr binDirExpr = value.get("binDir");
                    if (binExpr != null) {
                        pkg.bin = Bin.parseAppend(binExpr);
                    } else if (binDirExpr != null) {
                        pkg.bin = Bin.parseAppendFromDirectories(binDirExpr);
                    }
                    int pkgId = lockfile.appendPackageDedupe(pkg);
                    if (pkgMap.containsKey(name)) {
                        throw error("InvalidWorkspaceObject", "Duplicate workspace name: '" + name + "'");
                    }
                    pkgMap.put(name, pkgId);
                    workspacePkgsLen++;
                    continue workspaces;
                }
            }
        }

        Json.Expr pkgsExpr = root.get("packages");
        if (pkgsExpr == null) {
            if (workspacePkgsLen == 0) {
                return new Lockfile();
            }
            return lockfile;
        }
        if (!pkgsExpr.isObject()) {
            throw error("InvalidPackagesObject", "Expected an object");
        }

        Set<String> bundledPkgs = new HashSet<>();
        for (Json.Property prop : pkgsExpr.properties()) {
            if (!prop.value.isArray()) {
                throw error("InvalidPackageInfo", "Expected an array");
            }
            List<Json.Expr> pkgInfo = prop.value.items();
            if (pkgInfo.size() < 3) {
                continue;
            }
            Json.Expr bundledExpr = pkgInfo.get(2).get("bundled");
            if (bundledExpr == null) {
                continue;
            }
            Boolean bundled = bundledExpr.asBool();
            if (bundled != null && bundled) {
                bundledPkgs.add(prop.key.asString());
            }
        }

        nextPkgKey:
        for (Json.Property prop : pkgsExpr.properties()) {
            String pkgPath = prop.key.asString();
            if (!prop.value.isArray()) {
                throw error("InvalidPackageInfo", "Expected an array");
            }
            int i = 0;
            List<Json.Expr> pkgInfo = prop.value.items();
            if (pkgInfo.isEmpty()) {
                throw error("InvalidPackageInfo", "Missing package info");
            }
            Json.Expr resInfo = pkgInfo.get(i++);
            String resInfoStr = resInfo.asString();
            if (resInfoStr == null) {
                throw error("InvalidPackageResolution", "Expected a string");
            }
            String nameStr;
            String resStr;
            if (resInfoStr.startsWith("@root:")) {
                nameStr = "";
                resStr = resInfoStr.substring(1);
            } else {
                String[] split = Dependency.splitNameAndVersion(resInfoStr);
                if (split == null) {
                    throw error("InvalidPackageResolution", "Invalid package resolution");
                }
                nameStr = split[0];
                resStr = split[1];
            }
            long nameHash = Wyhash11.stringHash(nameStr);

            Resolution res;
            try {
                res = Resolution.fromTextLockfile(resStr);
            } catch (Resolution.UnexpectedResolution e) {
                throw error("UnexpectedResolution", e.getMessage());
            }

            if (res.tag == Resolution.Tag.NPM) {
                if (i >= pkgInfo.size()) {
                    throw error("InvalidPackageInfo", "Missing npm registry");
                }
                Json.Expr registryExpr = pkgInfo.get(i++);
                String registryStr = registryExpr.asString();
                if (registryStr == null) {
                    throw error("InvalidPackageInfo", "Expected a string");
                }
                if (registryStr.isEmpty()) {
                    res = res.withUrl(buildURL(options.scopeForPackageName(nameStr), nameStr, res.version));
                } else {
                    res = res.withUrl(registryStr);
                }
            }

            if (lockfileVersion != 0 && res.tag == Resolution.Tag.WORKSPACE) {
                if (pkgMap.containsKey(pkgPath)) {
                    continue;
                }
                int workspacePkgsEnd = workspacePkgsOff + workspacePkgsLen;
                for (int workspacePkgId = workspacePkgsOff; workspacePkgId < workspacePkgsEnd; workspacePkgId++) {
                    if (res.eql(lockfile.packages.get(workspacePkgId).resolution)) {
                        pkgMap.put(pkgPath, workspacePkgId);
                        continue nextPkgKey;
                    }
                }
                throw error("InvalidPackageInfo", "Unknown workspace: '" + res.value + "'");
            }

            Package pkg = new Package();
            switch (res.tag) {
                case NPM:
                case FOLDER:
                case GIT:
                case GITHUB:
                case LOCAL_TARBALL:
                case REMOTE_TARBALL:
                case SYMLINK:
                case WORKSPACE: {
                    if (res.tag == Resolution.Tag.WORKSPACE && lockfileVersion != 0) {
                        break;
                    }
                    if (i >= pkgInfo.size()) {
                        throw error("InvalidPackageInfo", "Missing dependencies object");
                    }
                    Json.Expr infoObj = pkgInfo.get(i++);
                    if (!infoObj.isObject()) {
                        throw error("InvalidPackageInfo", "Expected an object");
                    }
                    int[] offLen = parseAppendDependencies(infoObj, true, pkgPath, bundledPkgs, null);
                    pkg.dependenciesOff = offLen[0];
                    pkg.dependenciesLen = offLen[1];
                    Json.Expr binExpr = infoObj.get("bin");
                    Json.Expr binDirExpr = infoObj.get("binDir");
                    if (binExpr != null) {
                        pkg.bin = Bin.parseAppend(binExpr);
                    } else if (binDirExpr != null) {
                        pkg.bin = Bin.parseAppendFromDirectories(binDirExpr);
                    }
                    if (res.tag != Resolution.Tag.WORKSPACE) {
                        Json.Expr os = infoObj.get("os");
                        if (os != null) {
                            pkg.meta.os = Negatable.fromJson(Negatable.OPERATING_SYSTEM, os);
                        }
                        Json.Expr arch = infoObj.get("cpu");
                        if (arch != null) {
                            pkg.meta.arch = Negatable.fromJson(Negatable.ARCHITECTURE, arch);
                        }
                    }
                    break;
                }
                case ROOT: {
                    if (i >= pkgInfo.size()) {
                        throw error("InvalidPackageInfo", "Missing package binaries object");
                    }
                    Json.Expr binObj = pkgInfo.get(i++);
                    if (!binObj.isObject()) {
                        throw error("InvalidPackageInfo", "Expected an object");
                    }
                    Json.Expr binExpr = binObj.get("bin");
                    Json.Expr binDirExpr = binObj.get("binDir");
                    if (binExpr != null) {
                        pkg.bin = Bin.parseAppend(binExpr);
                    } else if (binDirExpr != null) {
                        pkg.bin = Bin.parseAppendFromDirectories(binDirExpr);
                    }
                    break;
                }
                default:
                    break;
            }

            switch (res.tag) {
                case NPM: {
                    if (i >= pkgInfo.size()) {
                        throw error("InvalidPackageInfo", "Missing integrity");
                    }
                    String integrityStr = pkgInfo.get(i++).asString();
                    if (integrityStr == null) {
                        throw error("InvalidPackageInfo", "Expected a string");
                    }
                    pkg.meta.integrity = Integrity.parse(integrityStr);
                    break;
                }
                case LOCAL_TARBALL:
                case REMOTE_TARBALL:
                    if (i < pkgInfo.size()) {
                        String integrityStr = pkgInfo.get(i).asString();
                        if (integrityStr != null) {
                            pkg.meta.integrity = Integrity.parse(integrityStr);
                            i++;
                        }
                    }
                    break;
                case GIT:
                case GITHUB: {
                    if (i >= pkgInfo.size()) {
                        throw error("InvalidPackageInfo", "Missing git dependency tag");
                    }
                    String bunTagStr = pkgInfo.get(i++).asString();
                    if (bunTagStr == null) {
                        throw error("InvalidPackageInfo", "Expected a string");
                    }
                    res = res.withRepository(res.repository.withResolved(bunTagStr));
                    if (i < pkgInfo.size()) {
                        String integrityStr = pkgInfo.get(i).asString();
                        if (integrityStr != null) {
                            pkg.meta.integrity = Integrity.parse(integrityStr);
                            i++;
                        }
                    }
                    break;
                }
                default:
                    break;
            }

            pkg.name = nameStr;
            pkg.nameHash = nameHash;
            pkg.resolution = res;
            int pkgId = lockfile.appendPackageDedupe(pkg);
            if (pkgMap.containsKey(pkgPath)) {
                throw error("InvalidPackageKey", "Duplicate package path");
            }
            pkgMap.put(pkgPath, pkgId);
        }

        for (int k = 0; k < lockfile.dependencies.size(); k++) {
            lockfile.resolutions.add(INVALID_PACKAGE_ID);
        }

        Set<String> seenDeps = new HashSet<>();
        Package rootPkg = lockfile.packages.get(0);
        rootPkg.resolution = Resolution.ROOT;
        rootPkg.meta.origin = Package.Origin.LOCAL;
        for (int depId = rootPkg.dependenciesOff; depId < rootPkg.dependenciesEnd(); depId++) {
            Dependency dep = lockfile.dependencies.get(depId);
            Integer resId = pkgMap.get(dep.name);
            if (resId == null) {
                if (dep.behavior.optional()) {
                    continue;
                }
                throw dependencyResolutionFailure(dep, null);
            }
            if (!dep.behavior.isWorkspace() && !seenDeps.add(dep.name)) {
                lockfile.resolutions.set(depId, resId);
                continue;
            }
            mapDepToPkg(dep, depId, resId);
        }

        if (lockfileVersion != 0) {
            for (int pkgId = workspacePkgsOff; pkgId < workspacePkgsOff + workspacePkgsLen; pkgId++) {
                Package workspace = lockfile.packages.get(pkgId);
                seenDeps.clear();
                for (int depId = workspace.dependenciesOff; depId < workspace.dependenciesEnd(); depId++) {
                    Dependency dep = lockfile.dependencies.get(depId);
                    Integer resId = pkgMap.get(workspace.name + "/" + dep.name);
                    if (resId == null) {
                        resId = pkgMap.get(dep.name);
                    }
                    if (resId == null) {
                        if (dep.behavior.optional()) {
                            continue;
                        }
                        throw dependencyResolutionFailure(dep, workspace.name);
                    }
                    if (!seenDeps.add(dep.name)) {
                        lockfile.resolutions.set(depId, resId);
                        continue;
                    }
                    mapDepToPkg(dep, depId, resId);
                }
            }
        }

        for (Json.Property prop : pkgsExpr.properties()) {
            String pkgPath = prop.key.asString();
            Integer pkgId = pkgMap.get(pkgPath);
            if (pkgId == null) {
                throw error("InvalidPackagesObject", "Invalid packages object");
            }
            Package pkg = lockfile.packages.get(pkgId);
            if (pkg.resolution.tag == Resolution.Tag.WORKSPACE) {
                continue;
            }
            for (int depId = pkg.dependenciesOff; depId < pkg.dependenciesEnd(); depId++) {
                Dependency dep = lockfile.dependencies.get(depId);
                Integer resId;
                try {
                    resId = findResolution(pkgMap, pkgPath, dep.name);
                } catch (InvalidPackageKey e) {
                    throw error("InvalidPackageKey", "Invalid package path");
                }
                if (resId == null) {
                    if (dep.behavior.optional()) {
                        continue;
                    }
                    throw dependencyResolutionFailure(dep, pkgPath);
                }
                mapDepToPkg(dep, depId, resId);
            }
        }

        try {
            lockfile.resolve();
        } catch (Tree.DependencyLoop e) {
            throw error("InvalidPackagesObject", e.getMessage());
        }
        return lockfile;
    }

    private static @Nullable Integer configVersionFromExpr(Json.Expr expr) {
        if (expr.kind != Json.Kind.NUMBER) {
            return null;
        }
        double n = expr.asNumber();
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        if (Math.floor(n) != n) {
            return null;
        }
        return n > 1 ? 1 : null;
    }

    private void readCatalog(Json.Expr obj, LinkedHashMap<String, Dependency> group, String errorName) {
        for (Json.Property prop : obj.properties()) {
            String depNameStr = prop.key.asString();
            if (depNameStr.isEmpty()) {
                throw error(errorName, "Expected a non-empty string");
            }
            if (!prop.value.isString()) {
                throw error(errorName, "Expected a string");
            }
            long depNameHash = Wyhash11.stringHash(depNameStr);
            DependencyVersion version = DependencyVersion.parse(depNameStr, depNameHash, prop.value.asString());
            if (version == null) {
                throw error(errorName, "Invalid catalog version");
            }
            if (group.containsKey(depNameStr)) {
                throw error(errorName, "Duplicate catalog entry");
            }
            group.put(depNameStr, new Dependency(depNameHash, depNameStr, version, Behavior.NONE));
        }
    }

    private void mapDepToPkg(Dependency dep, int depId, int pkgId) {
        lockfile.resolutions.set(depId, pkgId);
        if (lockfile.textLockfileVersion != 0) {
            Resolution res = lockfile.packages.get(pkgId).resolution;
            if (res.tag == Resolution.Tag.WORKSPACE) {
                dep.version = dep.version.asWorkspace(res.value);
            }
        }
    }

    private static ParseError dependencyResolutionFailure(Dependency dep, @Nullable String pkgPath) {
        String behavior = dep.behavior.isDev() ? "dev" :
                dep.behavior.optional() ? "optional" :
                        dep.behavior.isPeer() ? "peer" :
                                dep.behavior.isWorkspace() ? "workspace" : "prod";
        if (pkgPath != null) {
            return error("InvalidPackageInfo", "Failed to resolve " + behavior + " dependency '" + dep.name +
                                               "' for package '" + pkgPath + "'");
        }
        return error("InvalidPackageInfo", "Failed to resolve root " + behavior + " dependency '" + dep.name + "'");
    }

    private static final class InvalidPackageKey extends RuntimeException {
    }

    /**
     * {@code PkgMap.findResolution}: the dependency's name under the key, then under each ancestor key, a scoped
     * name counting as one folder.
     */
    static @Nullable Integer findResolution(Map<String, Integer> map, String pkgPath, String depName) {
        StringBuilder pathBuf = new StringBuilder(pkgPath).append('/');
        int offset = pkgPath.length() + 1;
        boolean valid = true;
        while (valid) {
            pathBuf.setLength(offset);
            pathBuf.append(depName);
            Integer entry = map.get(pathBuf.toString());
            if (entry != null) {
                return entry;
            }
            if (offset == 0) {
                return null;
            }
            String before = pathBuf.substring(0, offset - 1);
            int slash = before.lastIndexOf('/');
            if (slash < 0) {
                offset = 0;
                continue;
            }
            int at = before.lastIndexOf('@');
            if (at < 0) {
                offset = slash + 1;
                continue;
            }
            if (at > slash) {
                valid = false;
                continue;
            }
            int nextSlash = before.substring(0, slash).lastIndexOf('/');
            if (nextSlash < 0) {
                if (at != 0) {
                    throw new InvalidPackageKey();
                }
                offset = 0;
                continue;
            }
            if (nextSlash > at) {
                offset = slash + 1;
                continue;
            }
            if (nextSlash + 1 != at) {
                valid = false;
                continue;
            }
            offset = at;
        }
        throw new InvalidPackageKey();
    }

    /**
     * {@code parseAppendDependencies}: the four groups in order, an optional peer by {@code optionalPeers}, a bundled
     * edge by its bundled key; the root also gets one workspace edge per member. Sorted by behavior, then name.
     */
    private int[] parseAppendDependencies(Json.Expr obj, boolean checkForBundled, @Nullable String pkgPath,
                                          Set<String> bundledPkgs, Json.@Nullable Expr workspacesObj) {
        Set<Long> optionalPeers = new HashSet<>();
        Json.Expr optionalPeersExpr = obj.get("optionalPeers");
        if (optionalPeersExpr != null) {
            if (!optionalPeersExpr.isArray()) {
                throw error("InvalidPackageInfo", "Expected an array");
            }
            for (Json.Expr item : optionalPeersExpr.items()) {
                if (!item.isString()) {
                    throw error("InvalidPackageInfo", "Expected a string");
                }
                optionalPeers.add(Wyhash11.stringHash(item.asString()));
            }
        }

        int off = lockfile.dependencies.size();
        for (int g = 0; g < GROUP_NAMES.length; g++) {
            Json.Expr deps = obj.get(GROUP_NAMES[g]);
            if (deps == null) {
                continue;
            }
            if (!deps.isObject()) {
                throw error("InvalidPackagesTree", "Expected an object");
            }
            Behavior groupBehavior = Behavior.of(GROUP_BEHAVIORS[g]);
            for (Json.Property prop : deps.properties()) {
                String nameStr = prop.key.asString();
                long nameHash = Wyhash11.stringHash(nameStr);
                String versionStr = prop.value.asString();
                if (versionStr == null) {
                    throw error("InvalidDependencyVersion", "Expected a string");
                }
                Behavior behavior = groupBehavior.isPeer() && optionalPeers.contains(nameHash) ?
                        groupBehavior.add(Behavior.OPTIONAL) : groupBehavior;
                DependencyVersion version = DependencyVersion.parse(nameStr, nameHash, versionStr);
                if (version == null) {
                    throw error("InvalidDependencyVersion", "Invalid dependency version");
                }
                Dependency dep = new Dependency(nameHash, nameStr, version, behavior);
                if (checkForBundled && bundledPkgs.contains(pkgPath + "/" + nameStr)) {
                    dep.behavior = dep.behavior.add(Behavior.BUNDLED);
                }
                lockfile.dependencies.add(dep);
            }
        }

        if (workspacesObj != null) {
            workspaces:
            for (String workspacePath : lockfile.workspacePaths.values()) {
                for (Json.Property prop : workspacesObj.properties()) {
                    String path = prop.key.asString();
                    if (!path.equals(workspacePath)) {
                        continue;
                    }
                    String name = prop.value.get("name").asString();
                    DependencyVersion version = new DependencyVersion(DependencyVersion.Tag.WORKSPACE, "", path);
                    lockfile.dependencies.add(new Dependency(Wyhash11.stringHash(name), name, version,
                            Behavior.of(Behavior.WORKSPACE)));
                    continue workspaces;
                }
            }
        }

        int end = lockfile.dependencies.size();
        List<Dependency> slice = new ArrayList<>(lockfile.dependencies.subList(off, end));
        slice.sort(Dependency::compare);
        for (int k = 0; k < slice.size(); k++) {
            lockfile.dependencies.set(off + k, slice.get(k));
        }
        return new int[]{off, end - off};
    }

    /**
     * {@code ExtractTarball.buildURL}: the registry's tarball url of a version.
     */
    static String buildURL(String registryUrl, String fullName, SemverVersion version) {
        String registry = Strings.trimRight(registryUrl, "/");
        String name = fullName;
        if (name.charAt(0) == '@') {
            int i = name.indexOf('/');
            if (i >= 0) {
                name = name.substring(i + 1);
            }
        }
        StringBuilder out = new StringBuilder(registry).append('/').append(fullName).append("/-/").append(name)
                .append('-').append(Long.toUnsignedString(version.major))
                .append('.').append(Long.toUnsignedString(version.minor))
                .append('.').append(Long.toUnsignedString(version.patch));
        if (version.hasPre()) {
            out.append('-').append(version.pre);
        }
        if (version.hasBuild()) {
            out.append('+').append(version.build);
        }
        return out.append(".tgz").toString();
    }
}
