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

    private final Lockfile lockfile;
    private final BunOptions options;

    /**
     * Before 1.2.4, the package keys whose own entry is {@code "bundled": true}
     * ({@link BunRelease#bundlesADependencyByItsOwnKey()}).
     */
    private final Set<String> bundledEntries = new HashSet<>();

    private TextLockfileReader(BunOptions options) {
        this.options = options;
        this.lockfile = new Lockfile(options.getRelease());
    }

    // appendPackageDedupe; in 1.2.3 appendPackageNoDedupe, every entry a package of its own
    private int appendPackage(Package pkg) {
        if (options.getRelease().readsEveryPackageEntryAsItsOwnPackage()) {
            return lockfile.appendPackage(pkg).meta.id;
        }
        return lockfile.appendPackageDedupe(pkg);
    }

    /**
     * {@code loadFromDir} for a bun.lock: a text that does not parse is a {@code parse_file} error like any other, the
     * parser's own.
     */
    static Lockfile load(String text, BunOptions options) {
        Json.Expr root;
        try {
            root = Json.parse(text, options.getRelease());
        } catch (Json.SyntaxError e) {
            throw new ParseError(e.error, e.getMessage());
        }
        return new TextLockfileReader(options).parseIntoBinaryLockfile(root);
    }

    private ParseError error(String error, String message) {
        return new ParseError(options.getRelease().reportsEveryLockfileErrorAsInvalidLockfile() ? "InvalidLockfile" :
                error, message);
    }

    /**
     * A row of the overrides from 1.4.0, at every lockfile version: a string is the plain rule of its key, an object
     * the rules of its key's group, {@code "."} the key's own and every other key a child scoped to it.
     */
    private void readOverrideRow(String nameStr, Json.Expr value) {
        BunRelease release = options.getRelease();
        String versionStr = value.asString();
        if (versionStr != null) {
            if (!lockfile.overrides.putLockfileRule(null, new OverrideSelector.PackageSelector(nameStr, ""),
                    versionStr, release)) {
                throw error("InvalidOverridesObject", "Invalid override version");
            }
            return;
        }
        if (!value.isObject()) {
            throw error("InvalidOverridesObject", "Expected a string or an object");
        }
        OverrideSelector.PackageSelector parent;
        try {
            parent = OverrideSelector.parsePackageSegment(nameStr);
        } catch (OverrideSelector.Invalid e) {
            throw error("InvalidOverridesObject", "Invalid override key");
        }
        for (Json.Property child : value.properties()) {
            String childKey = child.key.asString();
            String childValue = child.value.asString();
            if (childValue == null) {
                throw error("InvalidOverridesObject", "Expected a string");
            }
            OverrideSelector.PackageSelector ruleParent = null;
            OverrideSelector.PackageSelector ruleTarget = parent;
            if (!".".equals(childKey)) {
                try {
                    ruleTarget = OverrideSelector.parsePackageSegment(childKey);
                } catch (OverrideSelector.Invalid e) {
                    throw error("InvalidOverridesObject", "Invalid override key");
                }
                ruleParent = parent;
            }
            if (!lockfile.overrides.putLockfileRule(ruleParent, ruleTarget, childValue, release)) {
                throw error("InvalidOverridesObject", "Invalid override version");
            }
        }
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
        BunRelease release = options.getRelease();
        if (num > (release.keepsTheLockfileVersionItRead() ? Lockfile.TEXT_LOCKFILE_VERSION_SCOPED_OVERRIDES :
                Lockfile.TEXT_LOCKFILE_VERSION_CURRENT)) {
            throw error("UnknownLockfileVersion", release.explainsAnUnsupportedLockfileVersion() ?
                    "Unsupported lockfile version " + (long) num + ". This lockfile was likely created by a newer " +
                    "version of Bun. (This is Bun v" + release + ", which supports lockfile versions up to " +
                    Lockfile.TEXT_LOCKFILE_VERSION_SCOPED_OVERRIDES + ".)" : "Unknown lockfile version");
        }
        lockfile.textLockfileVersion = lockfileVersion;

        Json.Expr configVersionExpr = options.getRelease().writesConfigVersion() ? root.get("configVersion") : null;
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
                if (release.readsScopedOverrides()) {
                    readOverrideRow(nameStr, prop.value);
                    continue;
                }
                if (!prop.value.isString()) {
                    throw error("InvalidOverridesObject", "Expected a string");
                }
                long nameHash = Wyhash11.stringHash(nameStr);
                DependencyVersion version = DependencyVersion.parse(nameStr, nameHash, prop.value.asString(),
                        options.getRelease());
                if (version == null) {
                    throw error("InvalidOverridesObject", "Invalid override version");
                }
                lockfile.overrides.map.put(nameHash, new Dependency(nameHash, nameStr, version, Behavior.NONE));
            }
        }

        // before 1.2.14 bun.lock holds no catalogs
        boolean catalogs = options.getRelease().parsesCatalogSpecs();
        Json.Expr catalogExpr = catalogs ? root.get("catalog") : null;
        if (catalogExpr != null) {
            if (!catalogExpr.isObject()) {
                throw error("InvalidCatalogObject", "Expected an object");
            }
            readCatalog(catalogExpr, lockfile.catalogs.defaultMap, "InvalidCatalogObject");
        }

        Json.Expr catalogsExpr = catalogs ? root.get("catalogs") : null;
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
                // before 1.2.19 an entry of a named catalog is an InvalidCatalogObject
                readCatalog(catalogProp.value, lockfile.catalogs.getOrPutGroup(catalogName),
                        options.getRelease().tellsDependencyGroupsApart() ? "InvalidCatalogsObject" :
                                "InvalidCatalogObject");
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
                SemverVersion.ParseResult parsed = SemverVersion.parse(versionExpr.asString(), options.getRelease());
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
                    workspacesObj, true);
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
                    int[] offLen = parseAppendDependencies(value, false, null, Collections.<String>emptySet(), null,
                            true);
                    pkg.dependenciesOff = offLen[0];
                    pkg.dependenciesLen = offLen[1];
                    Json.Expr binExpr = value.get("bin");
                    Json.Expr binDirExpr = value.get("binDir");
                    if (binExpr != null) {
                        pkg.bin = Bin.parseAppend(binExpr);
                    } else if (binDirExpr != null) {
                        pkg.bin = Bin.parseAppendFromDirectories(binDirExpr);
                    }
                    int pkgId = appendPackage(pkg);
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
                return new Lockfile(release);
            }
            // from 1.4.0 the workspaces' and the root's edges are resolved as against an empty object
            if (!release.resolvesALockWithoutPackages()) {
                return lockfile;
            }
        } else if (!pkgsExpr.isObject()) {
            throw error("InvalidPackagesObject", "Expected an object");
        }
        List<Json.Property> pkgRows = pkgsExpr == null ? Collections.<Json.Property>emptyList() :
                pkgsExpr.properties();

        // from 1.2.4 the keys marked bundled, whose dependency of that name is; before, the keys whose own entry is,
        // which every dependency bound to them is
        boolean bundlesByKey = release.bundlesADependencyByItsOwnKey();
        Set<String> bundledPkgs = new HashSet<>();
        for (Json.Property prop : bundlesByKey ? pkgRows : Collections.<Json.Property>emptyList()) {
            if (!prop.value.isArray()) {
                throw error("InvalidPackageInfo", "Expected an array");
            }
            List<Json.Expr> pkgInfo = prop.value.items();
            Json.Expr infoObj = null;
            if (release.findsTheBundledMarkInAnyEntry()) {
                // index 2 for an npm resolution (after the registry string), index 1 otherwise
                for (Json.Expr item : pkgInfo) {
                    if (item.isObject()) {
                        infoObj = item;
                        break;
                    }
                }
            } else if (pkgInfo.size() >= 3) {
                infoObj = pkgInfo.get(2);
            }
            if (infoObj == null) {
                continue;
            }
            Json.Expr bundledExpr = infoObj.get("bundled");
            if (bundledExpr == null) {
                continue;
            }
            Boolean bundled = bundledExpr.asBool();
            if (bundled != null && bundled) {
                bundledPkgs.add(prop.key.asString());
            }
        }

        nextPkgKey:
        for (Json.Property prop : pkgRows) {
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
            if (resInfoStr.startsWith("@root:") && options.getRelease().readsAnUnnamedRootEntry()) {
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
            if (!nameStr.isEmpty() && options.getRelease().rejectsAnUnsafePackageName() &&
                !Dependency.isSafeInstallFolderName(nameStr)) {
                throw error("InvalidPackageResolution", "Invalid package name");
            }
            long nameHash = Wyhash11.stringHash(nameStr);

            Resolution res;
            try {
                res = Resolution.fromTextLockfile(resStr, options.getRelease());
            } catch (Resolution.UnexpectedResolution e) {
                throw error("UnexpectedResolution", e.getMessage());
            }

            boolean npmUrlNeedsIntegrity = false;
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
                    res = res.withUrl(buildURL(options.getRelease().expandsEmptyRegistryToScope() ?
                            options.scopeForPackageName(nameStr) : BunOptions.DEFAULT_REGISTRY, nameStr, res.version));
                } else {
                    npmUrlNeedsIntegrity = !urlIsUnderRegistry(registryStr, options.scopeForPackageName(nameStr)) &&
                                           !urlIsUnderRegistry(registryStr, BunOptions.DEFAULT_REGISTRY);
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
                        // before 1.2.5 the match falls through to the error
                        if (release.readsAWorkspaceEntryUnderAnotherKey()) {
                            continue nextPkgKey;
                        }
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
                    Json.Expr bundledExpr = bundlesByKey ? null : infoObj.get("bundled");
                    if (bundledExpr != null) {
                        Boolean bundled = bundledExpr.asBool();
                        if (bundled == null) {
                            throw error("InvalidPackageInfo", "Expected a boolean");
                        }
                        if (bundled) {
                            bundledEntries.add(pkgPath);
                        }
                    }
                    int[] offLen = parseAppendDependencies(infoObj, true, pkgPath, bundledPkgs, null,
                            res.tag == Resolution.Tag.WORKSPACE);
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
                    pkg.meta.integrity = Integrity.parse(integrityStr, options.getRelease());
                    if (lockfileVersion >= Lockfile.TEXT_LOCKFILE_VERSION_STRICT && npmUrlNeedsIntegrity &&
                        !pkg.meta.integrity.tag.isSupported()) {
                        throw error("InvalidPackageInfo", "Missing integrity hash for npm package resolved to a " +
                                                          "tarball URL outside the configured registry");
                    }
                    break;
                }
                case LOCAL_TARBALL:
                case REMOTE_TARBALL:
                    if (options.getRelease().carriesTarballIntegrity() && i < pkgInfo.size()) {
                        String integrityStr = pkgInfo.get(i).asString();
                        if (integrityStr != null) {
                            pkg.meta.integrity = Integrity.parse(integrityStr, options.getRelease());
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
                    boolean enforceSafeTag = res.tag == Resolution.Tag.GITHUB ?
                            options.getRelease().rejectsAnUnsafeGitHubTag() :
                            lockfileVersion >= Lockfile.TEXT_LOCKFILE_VERSION_STRICT;
                    if (enforceSafeTag && !Repository.isSafeResolvedTag(bunTagStr)) {
                        throw error("InvalidPackageInfo", "Invalid git dependency tag");
                    }
                    res = res.withRepository(res.repository.withResolved(bunTagStr));
                    if (options.getRelease().carriesTarballIntegrity() && i < pkgInfo.size()) {
                        String integrityStr = pkgInfo.get(i).asString();
                        if (integrityStr != null) {
                            pkg.meta.integrity = Integrity.parse(integrityStr, options.getRelease());
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
            int pkgId = appendPackage(pkg);
            if (pkgMap.containsKey(pkgPath)) {
                throw error("InvalidPackageKey", "Duplicate package path");
            }
            pkgMap.put(pkgPath, pkgId);
        }

        for (int k = 0; k < lockfile.dependencies.size(); k++) {
            lockfile.resolutions.add(INVALID_PACKAGE_ID);
        }

        // from 1.2.19 a package's same-named dependencies take the first group's resolution
        boolean groupsApart = release.tellsDependencyGroupsApart();
        // from 1.4.1 an edge is bound as it is, and tagged a workspace link afterwards as a reparse would
        boolean tagsLinks = release.tagsWorkspaceLinksAsAReparseDoes();
        Set<String> seenDeps = new HashSet<>();
        Package rootPkg = lockfile.packages.get(0);
        rootPkg.resolution = Resolution.ROOT;
        rootPkg.meta.origin = Package.Origin.LOCAL;
        for (int depId = rootPkg.dependenciesOff; depId < rootPkg.dependenciesEnd(); depId++) {
            Dependency dep = lockfile.dependencies.get(depId);
            Integer resId = resolvePeerDepVersionBased(dep);
            String key = null;
            if (resId == null) {
                key = dep.name;
                resId = pkgMap.get(key);
            }
            if (resId == null) {
                if (mayStayUnresolved(dep)) {
                    continue;
                }
                throw dependencyResolutionFailure(dep, null);
            }
            if (tagsLinks || groupsApart && !dep.behavior.isWorkspace() && !seenDeps.add(dep.name)) {
                lockfile.resolutions.set(depId, resId);
                continue;
            }
            mapDepToPkg(dep, depId, resId, key);
        }

        if (lockfileVersion != 0) {
            for (int pkgId = workspacePkgsOff; pkgId < workspacePkgsOff + workspacePkgsLen; pkgId++) {
                Package workspace = lockfile.packages.get(pkgId);
                seenDeps.clear();
                for (int depId = workspace.dependenciesOff; depId < workspace.dependenciesEnd(); depId++) {
                    Dependency dep = lockfile.dependencies.get(depId);
                    Integer resId = resolvePeerDepVersionBased(dep);
                    String key = null;
                    if (resId == null) {
                        key = workspace.name + "/" + dep.name;
                        resId = pkgMap.get(key);
                    }
                    if (resId == null) {
                        key = dep.name;
                        resId = pkgMap.get(key);
                    }
                    if (resId == null) {
                        if (mayStayUnresolved(dep)) {
                            continue;
                        }
                        throw dependencyResolutionFailure(dep, workspace.name);
                    }
                    if (tagsLinks || groupsApart && !seenDeps.add(dep.name)) {
                        lockfile.resolutions.set(depId, resId);
                        continue;
                    }
                    mapDepToPkg(dep, depId, resId, key);
                }
            }
        }

        for (Json.Property prop : pkgRows) {
            String pkgPath = prop.key.asString();
            Integer pkgId = pkgMap.get(pkgPath);
            if (pkgId == null) {
                throw error("InvalidPackagesObject", "Invalid packages object");
            }
            Package pkg = lockfile.packages.get(pkgId);
            if (groupsApart && pkg.resolution.tag == Resolution.Tag.WORKSPACE) {
                continue;
            }
            for (int depId = pkg.dependenciesOff; depId < pkg.dependenciesEnd(); depId++) {
                Dependency dep = lockfile.dependencies.get(depId);
                // a stripped catalog: edge stays unresolved, as in a fresh install
                if (dep.version.tag == DependencyVersion.Tag.UNINITIALIZED &&
                    release.stripsCatalogReferencesOutsideTheWorkspaces()) {
                    continue;
                }
                Integer resId = resolvePeerDepVersionBased(dep);
                String key = null;
                if (resId == null) {
                    try {
                        // from 1.4.0 an optional peer is looked up no higher than its bundle's, as the hoist does
                        key = findResolution(pkgMap, pkgPath, dep.name,
                                dep.behavior.isOptionalPeer() && release.boundsTheOptionalPeersOfABundle() ?
                                        bundledPkgs : null);
                    } catch (InvalidPackageKey e) {
                        throw error("InvalidPackageKey", "Invalid package path");
                    }
                    resId = key == null ? null : pkgMap.get(key);
                }
                if (resId == null) {
                    if (mayStayUnresolved(dep)) {
                        continue;
                    }
                    throw dependencyResolutionFailure(dep, pkgPath);
                }
                if (tagsLinks) {
                    lockfile.resolutions.set(depId, resId);
                    continue;
                }
                mapDepToPkg(dep, depId, resId, key);
            }
        }

        if (tagsLinks) {
            lockfile.tagWorkspaceLinks(options.isLinkWorkspacePackages());
        }

        try {
            lockfile.resolve();
        } catch (Tree.DependencyLoop e) {
            // from 1.4.0 the hoist has no dependency loop to fail on
            throw error("InvalidPackagesObject", e.getMessage());
        }
        return lockfile;
    }

    /**
     * {@code may_stay_unresolved}: an edge a fresh install may itself leave unresolved, which bun.lock lists without a
     * package; from 1.4.0 a peer too, before an optional dependency alone.
     */
    private boolean mayStayUnresolved(Dependency dep) {
        return dep.behavior.optional() ||
               dep.behavior.isPeer() && options.getRelease().readsAPeerTheLockLeavesUnresolved();
    }

    /**
     * {@code resolve_peer_dep_version_based} (from 1.4.0): a required peer whose range, its catalog entry's for a
     * {@code catalog:} peer, is not {@code *} and that no override names binds as the resolver's peer pass binds it,
     * to the first package of the range's name, highest first, that satisfies the range, else to the highest when it
     * is of the range's kind; null leaves it to the path walk. A peer cannot be bound from the printed tree as another
     * edge is: it never gets its own path where the version hoisted above satisfies it.
     */
    private @Nullable Integer resolvePeerDepVersionBased(Dependency dep) {
        if (!options.getRelease().bindsALoadedPeerByVersion() || !dep.behavior.isPeer() ||
            dep.behavior.isOptionalPeer()) {
            return null;
        }
        // deferred_peer_range: optional and * peers keep the path walk, which reproduces what the resolver did
        DependencyVersion range = lockfile.catalogs.resolveRange(dep);
        if (range.tag == DependencyVersion.Tag.NPM && range.npm().version.isStar()) {
            return null;
        }
        // the package index is keyed by real names, which the range, not the dependency, carries for an alias
        long nameHash;
        switch (range.tag) {
            case NPM:
                nameHash = Wyhash11.stringHash(range.npm().name);
                break;
            case DIST_TAG:
                nameHash = Wyhash11.stringHash(range.distTag().name);
                break;
            case GIT:
            case GITHUB:
                nameHash = Wyhash11.stringHash(range.repository().packageName);
                break;
            case TARBALL:
                nameHash = Wyhash11.stringHash(range.tarball().packageName);
                break;
            default:
                nameHash = dep.nameHash;
                break;
        }
        // the resolver applies an override before its scan; the tree reflects it, so an overridden peer keeps the
        // walk. npm: aliases and workspace edges are never overridden, and a catalog peer is by its own name.
        boolean overridable = !dep.behavior.isWorkspace() &&
                              (dep.version.tag != DependencyVersion.Tag.NPM || !dep.version.npm().isAlias);
        long overrideNameHash = dep.version.tag == DependencyVersion.Tag.CATALOG ? dep.nameHash : nameHash;
        // OverrideMap::has_rule_for_name
        if (overridable && lockfile.overrides.map.containsKey(overrideNameHash)) {
            return null;
        }
        List<Integer> candidates = lockfile.packageIndex.get(nameHash);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (int id : candidates) {
            if (id < lockfile.packages.size() &&
                satisfiesDependencyVersion(lockfile.packages.get(id).resolution, range)) {
                return id;
            }
        }
        int first = candidates.get(0);
        if (first < lockfile.packages.size()) {
            Resolution.Tag resTag = lockfile.packages.get(first).resolution.tag;
            if (resTag == Resolution.Tag.NPM && range.tag == DependencyVersion.Tag.NPM ||
                resTag == Resolution.Tag.GIT && range.tag == DependencyVersion.Tag.GIT ||
                resTag == Resolution.Tag.GITHUB && range.tag == DependencyVersion.Tag.GITHUB) {
                return first;
            }
        }
        return null;
    }

    /**
     * {@code Resolution::satisfies_dependency_version}: an npm version the range accepts, or the same git or GitHub
     * repository.
     */
    private static boolean satisfiesDependencyVersion(Resolution resolution, DependencyVersion version) {
        if (resolution.tag == Resolution.Tag.NPM && version.tag == DependencyVersion.Tag.NPM) {
            return version.npm().version.satisfies(resolution.version);
        }
        if (resolution.tag == Resolution.Tag.GIT && version.tag == DependencyVersion.Tag.GIT ||
            resolution.tag == Resolution.Tag.GITHUB && version.tag == DependencyVersion.Tag.GITHUB) {
            return resolution.repository.eql(version.repository());
        }
        return false;
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
            DependencyVersion version = DependencyVersion.parse(depNameStr, depNameHash, prop.value.asString(),
                    options.getRelease());
            if (version == null) {
                throw error(errorName, "Invalid catalog version");
            }
            if (group.containsKey(depNameStr)) {
                throw error(errorName, "Duplicate catalog entry");
            }
            group.put(depNameStr, new Dependency(depNameHash, depNameStr, version, Behavior.NONE));
        }
    }

    /**
     * @param key the package key the dependency was found under; before 1.2.4 the dependency is bundled as that
     *            entry is ({@link BunRelease#bundlesADependencyByItsOwnKey()})
     */
    private void mapDepToPkg(Dependency dep, int depId, int pkgId, @Nullable String key) {
        lockfile.resolutions.set(depId, pkgId);
        if (!options.getRelease().bundlesADependencyByItsOwnKey()) {
            dep.behavior = dep.behavior.set(Behavior.BUNDLED, key != null && bundledEntries.contains(key));
        }
        if (lockfile.textLockfileVersion != 0) {
            Resolution res = lockfile.packages.get(pkgId).resolution;
            if (res.tag == Resolution.Tag.WORKSPACE) {
                dep.version = dep.version.asWorkspace(res.value);
                // before 1.2.19 a dependency on a member carries the workspace bit too
                if (!options.getRelease().tellsDependencyGroupsApart()) {
                    dep.behavior = dep.behavior.add(Behavior.WORKSPACE);
                }
            }
        }
    }

    private ParseError dependencyResolutionFailure(Dependency dep, @Nullable String pkgPath) {
        String behavior = dep.behavior.isDev() ? "dev" :
                dep.behavior.optional() ? "optional" :
                        dep.behavior.isPeer() ? "peer" :
                                dep.behavior.isWorkspace(options.getRelease()) ? "workspace" : "prod";
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
     * name counting as one folder; the key the entry stands at, whose own bundled mark counts before 1.2.4.
     *
     * @param bundledPkgs the keys marked {@code "bundled": true}, above whose parent the walk stops
     *                    ({@code find_resolution_bounded_at_bundle}, from 1.4.0); null for no bound
     */
    static @Nullable String findResolution(Map<String, Integer> map, String pkgPath, String depName,
                                           @Nullable Set<String> bundledPkgs) {
        StringBuilder pathBuf = new StringBuilder(pkgPath).append('/');
        int offset = pkgPath.length() + 1;
        boolean atBundleRoot = false;
        boolean valid = true;
        while (valid) {
            pathBuf.setLength(offset);
            pathBuf.append(depName);
            String candidate = pathBuf.toString();
            if (map.containsKey(candidate)) {
                return candidate;
            }
            if (offset == 0 || atBundleRoot) {
                return null;
            }
            String before = pathBuf.substring(0, offset - 1);
            if (bundledPkgs != null) {
                atBundleRoot = bundledPkgs.contains(before);
            }
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
     *
     * @param catalogsApply whether the package is the root or a workspace, whose {@code catalog:} edges from 1.4.0
     *                      alone reference the catalogs ({@link CatalogMap#stripReference})
     */
    private int[] parseAppendDependencies(Json.Expr obj, boolean checkForBundled, @Nullable String pkgPath,
                                          Set<String> bundledPkgs, Json.@Nullable Expr workspacesObj,
                                          boolean catalogsApply) {
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
                DependencyVersion version = DependencyVersion.parse(nameStr, nameHash, versionStr,
                        options.getRelease());
                if (version == null) {
                    throw error("InvalidDependencyVersion", "Invalid dependency version");
                }
                Dependency dep = new Dependency(nameHash, nameStr, version, behavior);
                if (!catalogsApply && options.getRelease().stripsCatalogReferencesOutsideTheWorkspaces()) {
                    CatalogMap.stripReference(dep);
                }
                if (checkForBundled && bundledPkgs.contains(pkgPath + "/" + nameStr)) {
                    dep.behavior = dep.behavior.add(Behavior.BUNDLED);
                }
                lockfile.dependencies.add(dep);
            }
        }

        List<Dependency> members = new ArrayList<>();
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
                    members.add(new Dependency(Wyhash11.stringHash(name), name, version,
                            Behavior.of(Behavior.WORKSPACE)));
                    continue workspaces;
                }
            }
        }
        // before 1.2.19 the root's members follow its other dependencies, unsorted
        boolean membersSorted = options.getRelease().tellsDependencyGroupsApart();
        if (membersSorted) {
            lockfile.dependencies.addAll(members);
        }

        int end = lockfile.dependencies.size();
        List<Dependency> slice = new ArrayList<>(lockfile.dependencies.subList(off, end));
        slice.sort((l, r) -> Dependency.compare(l, r, options.getRelease()));
        for (int k = 0; k < slice.size(); k++) {
            lockfile.dependencies.set(off + k, slice.get(k));
        }
        if (!membersSorted) {
            lockfile.dependencies.addAll(members);
            end = lockfile.dependencies.size();
        }
        return new int[]{off, end - off};
    }

    /**
     * {@code url_is_under_registry} (from 1.4.0): the registry, without its trailing slash, is a prefix of the url
     * that ends it or is followed by a {@code /}.
     */
    static boolean urlIsUnderRegistry(String url, String registry) {
        String prefix = Strings.withoutTrailingSlash(registry);
        return url.startsWith(prefix) && (url.length() == prefix.length() || url.charAt(prefix.length()) == '/');
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
