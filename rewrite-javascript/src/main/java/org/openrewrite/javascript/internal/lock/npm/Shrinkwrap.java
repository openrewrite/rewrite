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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/shrinkwrap.js (npm 11.17.0): the lock as data, rebuilt from the tree by
 * {@link #commit()} and written by {@link #toString()}.
 */
public final class Shrinkwrap {

    /**
     * load's filename: an npm-shrinkwrap.json the project holds is read and written in place of package-lock.json,
     * by an npm {@code release} before 12.0.0.
     */
    public static boolean readsShrinkwrap(NpmRelease release, Predicate<String> holds) {
        // before npm 7.4.2 a package-lock.json beside it is read and written instead
        return holds.test("npm-shrinkwrap.json") && release.readsNpmShrinkwrap() &&
               (release.prefersShrinkwrapToLock() || !holds.test("package-lock.json"));
    }

    /**
     * {@code ELOCKFILEVERSION}: a lock newer than npm 12 reads.
     */
    public static final class UnsupportedLockfileVersion extends IllegalStateException implements NpmError {
        UnsupportedLockfileVersion(@Nullable Object lockfileVersion) {
            super("Unsupported lockfileVersion " + Js.string(lockfileVersion) + ". This npm only supports up to " +
                  MAX_LOCKFILE_VERSION + ". Please upgrade npm.");
        }

        @Override
        public String getCode() {
            return "ELOCKFILEVERSION";
        }
    }

    /**
     * What save writes beside the lock it reads: the yarn.lock, which it rewrites from the tree.
     */
    public static boolean writesBesideLock(Predicate<String> holds) {
        return holds.test("yarn.lock");
    }

    // npm 12.0.0: a node's patch record and a manifest extension's state each make the lock version 4, the newest
    // version it reads
    private static final int PATCHED_LOCKFILE_VERSION = 4;
    private static final int PACKAGE_EXTENSIONS_LOCKFILE_VERSION = 4;
    private static final int MAX_LOCKFILE_VERSION = 4;

    public static final List<String> KEY_ORDER = Collections.unmodifiableList(Arrays.asList(
            "name", "version", "lockfileVersion", "resolved", "integrity", "requires", "packages", "dependencies"));

    private static final Pattern NODE_MODULES = Pattern.compile("(?:^|/)node_modules/");
    private static final Pattern YARN_REGISTRY = Pattern.compile("^https?://registry\\.yarnpkg\\.com/");
    private static final Pattern NPM_REGISTRY = Pattern.compile("^https?://registry\\.npmjs\\.org/");
    // before npm 8.5.2 a dot of the host matches any character
    private static final Pattern YARN_REGISTRY_UNESCAPED = Pattern.compile("^https?://registry.yarnpkg.com/");
    private static final Pattern NPM_REGISTRY_UNESCAPED = Pattern.compile("^https?://registry.npmjs.org/");
    private static final Pattern LAST_SEGMENT = Pattern.compile("(^|/)[^/]*$");

    private static final List<String> PKG_META_KEYS = Collections.unmodifiableList(Arrays.asList(
            "version", "dependencies", "peerDependencies", "peerDependenciesMeta", "optionalDependencies",
            "bundleDependencies", "acceptDependencies", "funding", "engines", "os", "cpu", "libc", "_integrity",
            "license", "_hasShrinkwrap", "hasInstallScript", "bin", "deprecated", "workspaces"));

    private Map<String, Node> awaitingUpdate = new LinkedHashMap<>();

    @Getter
    private final String path;

    @Getter
    private final NpmOptions resolveOptions;

    @Getter
    private @Nullable Node tree;

    /**
     * The lock as JSON; {@code packages} lags the tree until an entry is read through {@link #get}.
     */
    @Getter
    private @Nullable Map<String, Object> data;

    /**
     * A {@code packages} array as loaded: indexed like an object in {@link #data}, written back as the
     * array until a tree replaces it.
     */
    private @Nullable List<Object> packagesArray;

    @Getter
    @Setter
    private @Nullable YarnLock yarnLock;

    /**
     * The version written: the option, else the lock's own unless that is 1, else 3; null until loaded.
     */
    @Getter
    @Setter
    private @Nullable Object lockfileVersion;

    /**
     * The lock's {@code lockfileVersion} as loaded, whatever JSON type it had; {@link Js#UNDEFINED} when
     * absent, null before loading.
     */
    @Getter
    @Setter
    private @Nullable Object originalLockfileVersion;

    @Getter
    @Setter
    private boolean loadedFromDisk;

    @Getter
    private boolean ancientLockfile;

    /**
     * Whether the lock text parsed to a JSON object; arborist spreads any other value into its data.
     */
    @Getter
    private boolean loadedFromObject;

    @Getter
    private @Nullable Throwable loadingError;

    /**
     * {@code JSON.stringify}'s space: a number of spaces or the indent string; 2 until inferred.
     */
    @Getter
    @Setter
    private Object indent = 2;

    @Getter
    @Setter
    private String newline = "\n";

    /**
     * The digest of the root's {@code packageExtensions} the lock's root entry records, from npm 12.0.0, whatever
     * JSON type it has there; null without one.
     */
    @Getter
    @Setter
    private @Nullable Object packageExtensionsHash;

    /**
     * The digest of the root's .npm-extension file the lock's root entry records, from npm 12.0.0; null without one.
     */
    @Getter
    @Setter
    private @Nullable Object npmExtensionHash;

    public Shrinkwrap(String path, NpmOptions options) {
        this.path = PosixPath.resolve(path);
        this.resolveOptions = options;
        this.lockfileVersion = options.getLockfileVersion();
    }

    public static Map<String, Object> metaFromNode(Node node, String path, NpmOptions options) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (node.isLink()) {
            meta.put("resolved", Relpath.relpath(path, node.getRealpath()));
            meta.put("link", true);
            return meta;
        }

        for (String key : PKG_META_KEYS) {
            if ("libc".equals(key) && !options.getRelease().locksLibc()) {
                continue;
            }
            Object val = metaFieldFromPkg(node.getPackage(), key);
            if (truthy(val) && isPkgMetaKey(key, options.getRelease())) {
                meta.put(key.startsWith("_") ? key.substring(1) : key, val);
            }
        }
        Object pname = node.getPackageName();
        if (truthy(pname) && (node.isRoot() && options.getRelease().writesRootPackageName() ||
                              !Js.strictEquals(pname, node.getName()) ||
                              options.getRelease().namesEntryByFolder() &&
                              !Js.strictEquals(Node.nameFromFolder(node.getRealpath()), pname))) {
            meta.put("name", pname);
        }

        Object devDependencies = node.getPackage().get("devDependencies");
        if (node.isTop() && truthy(devDependencies)) {
            meta.put("devDependencies", devDependencies);
        }

        if (truthy(node.getIntegrity())) {
            meta.put("integrity", node.getIntegrity());
        }
        if (node.isInBundle()) {
            meta.put("inBundle", true);
        }
        if (truthy(node.getHasShrinkwrap())) {
            meta.put("hasShrinkwrap", node.getHasShrinkwrap());
        }
        if (node.hasInstallScript()) {
            meta.put("hasInstallScript", true);
        }
        // npm 12.0.0's node keys, which only it sets
        if (truthy(node.getPatched())) {
            meta.put("patched", node.getPatched());
        }
        if (truthy(node.getPackageExtensionsApplied())) {
            meta.put("packageExtensionsApplied", node.getPackageExtensionsApplied());
        }
        if (truthy(node.getNpmExtensionApplied())) {
            meta.put("npmExtensionApplied", node.getNpmExtensionApplied());
        }

        Object resolved = ConsistentResolve.consistentResolve(node.getResolved(), node.getPath(), path, true,
                options.getRelease());
        if (truthy(resolved)) {
            Object written = options.getRelease().definesOmitLockfileRegistryResolved() &&
                             node.isRegistryDependency() ? OverrideResolves.overrideResolves(resolved, options) :
                    resolved;
            if (written != null) {
                meta.put("resolved", written);
            }
        }

        if (node.isExtraneous()) {
            meta.put("extraneous", true);
        } else {
            if (node.isPeer()) {
                meta.put("peer", true);
            }
            if (node.isDev()) {
                meta.put("dev", true);
            }
            if (node.isOptional()) {
                meta.put("optional", true);
            }
            if (node.isDevOptional() && !node.isDev() && !node.isOptional()) {
                meta.put("devOptional", true);
            }
        }
        return meta;
    }

    // pkgMetaKeys of the release: _hasShrinkwrap until npm 12.0.0
    private static boolean isPkgMetaKey(String key, NpmRelease release) {
        return !"_hasShrinkwrap".equals(key) || release.readsNpmShrinkwrap();
    }

    private static @Nullable Object metaFieldFromPkg(Map<String, Object> pkg, String key) {
        Object val = pkg.get(key);
        if (truthy(val)) {
            if ("license".equals(key) && val instanceof Map && truthy(((Map<?, ?>) val).get("type"))) {
                return ((Map<?, ?>) val).get("type");
            }
            boolean emptyObject = val instanceof Map && ((Map<?, ?>) val).isEmpty() ||
                                  val instanceof List && ((List<?>) val).isEmpty();
            if (!emptyObject) {
                return val;
            }
        }
        return null;
    }

    /**
     * Arborist's {@code checkYarnLock}: the spec a yarn.lock resolves {@code spec} to when no lock was
     * loaded, with {@code resolved} and {@code integrity} added to {@code options} for the fetch.
     */
    public Spec checkYarnLock(String spec, Map<String, Object> options) {
        return checkYarnLock(Spec.npa(spec, null, resolveOptions.getRelease().npmPackageArg()), options);
    }

    public Spec checkYarnLock(Spec parsed, Map<String, Object> options) {
        YarnLock yarnLock = this.yarnLock;
        boolean useYarnLock = yarnLock != null && !loadedFromDisk;
        YarnLock.Entry fromYarn = useYarnLock ? yarnLock.getEntries().get(parsed.getRaw()) : null;
        if (fromYarn != null && truthy(fromYarn.get("version"))) {
            Object resolved = fromYarn.get("resolved");
            Object version = fromYarn.get("version");
            Object integrity = fromYarn.get("integrity");
            String resolvedString = resolved instanceof String ? (String) resolved : null;
            boolean isYarnReg = parsed.isRegistry() && resolvedString != null &&
                                yarnRegRe().matcher(resolvedString).find();
            boolean isnpmReg = parsed.isRegistry() && !isYarnReg && resolvedString != null &&
                               npmRegRe().matcher(resolvedString).find();
            boolean isReg = isnpmReg || isYarnReg;
            VersionFromTgz.NameVersion tgz =
                    isReg ? VersionFromTgz.versionFromTgz(parsed.getName(), resolvedString,
                            resolveOptions.getRelease()) : null;
            Object yspec = resolved;
            if (tgz != null && Objects.equals(tgz.getName(), parsed.getName()) &&
                Js.strictEquals(tgz.getVersion(), version)) {
                yspec = version;
            } else if (isReg && tgz != null && truthy(tgz.getName()) && truthy(tgz.getVersion())) {
                yspec = "npm:" + tgz.getName() + "@" + tgz.getVersion();
            }
            if (truthy(yspec)) {
                options.put("resolved", resolvedString == null ? resolved :
                        yarnRegRe().matcher(resolvedString).replaceFirst("https://registry.npmjs.org/"));
                options.put("integrity", integrity);
                return Spec.npa(parsed.getName() + "@" + yspec, null, resolveOptions.getRelease().npmPackageArg());
            }
        }
        return parsed;
    }

    private Pattern yarnRegRe() {
        return resolveOptions.getRelease().escapesRegistryHostDots() ? YARN_REGISTRY : YARN_REGISTRY_UNESCAPED;
    }

    private Pattern npmRegRe() {
        return resolveOptions.getRelease().escapesRegistryHostDots() ? NPM_REGISTRY : NPM_REGISTRY_UNESCAPED;
    }

    public void setData(@Nullable Map<String, Object> data) {
        this.data = data;
        packagesArray = null;
    }

    public void reset() {
        tree = null;
        awaitingUpdate = new LinkedHashMap<>();
        packagesArray = null;
        packageExtensionsHash = null;
        npmExtensionHash = null;
        Object lockfileVersion = truthy(this.lockfileVersion) ? this.lockfileVersion :
                resolveOptions.getRelease().defaultLockfileVersion();
        originalLockfileVersion = lockfileVersion;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lockfileVersion", lockfileVersion);
        data.put("requires", true);
        data.put("packages", new LinkedHashMap<>());
        data.put("dependencies", new LinkedHashMap<>());
        this.data = data;
    }

    public void inferFormattingOptions(PackageJson packageJson) {
        if (packageJson.getIndent() != null) {
            indent = packageJson.getIndent();
        }
        if (packageJson.getNewline() != null) {
            newline = packageJson.getNewline();
        }
    }

    /**
     * Arborist's {@code load()} over the lock (null when none), yarn.lock and package.json, read only to type the
     * dependencies of a lockfileVersion 1 lock.
     */
    public Shrinkwrap load(VirtualTreeLoader.ProjectFiles files) {
        LockFile lock = files.getLock();
        String yarn = files.getYarnLock();
        Object data;
        try {
            loadedFromDisk = lock != null && truthy(lock.getText());

            if (truthy(yarn)) {
                yarnLock = new YarnLock(resolveOptions.getRelease());
                try {
                    yarnLock.parse(yarn);
                } catch (RuntimeException ignored) {
                }
            }

            JsonParseEvenBetterErrors.Parsed parsed = loadedFromDisk ? Objects.requireNonNull(lock).parsed() :
                    JsonParseEvenBetterErrors.parse("{}");
            data = parsed.getValue();
            loadedFromObject = data instanceof Map;
            // inferFormattingOptions destructures the parsed value, which throws for null
            Js.get(data, "lockfileVersion");
            if (parsed.getIndent() != null) {
                indent = parsed.getIndent();
            }
            if (parsed.getNewline() != null) {
                newline = parsed.getNewline();
            }
        } catch (RuntimeException er) {
            loadingError = er;
            loadedFromDisk = false;
            ancientLockfile = false;
            data = new LinkedHashMap<>();
        }
        NpmRelease release = resolveOptions.getRelease();
        Object lockfileVersion = release.defaultLockfileVersion();
        Object fileVersion = Js.get(data, "lockfileVersion");
        if (Js.toNumber(fileVersion) > MAX_LOCKFILE_VERSION && release.refusesNewerLockfile()) {
            throw new UnsupportedLockfileVersion(fileVersion);
        }
        if (!release.keepsNewerLockfileVersion()) {
            // before npm 8.1.0, `lockfileVersion` is the constant 2 for every lock read
            lockfileVersion = release.defaultLockfileVersion();
        } else if (truthy(this.lockfileVersion)) {
            lockfileVersion = this.lockfileVersion;
        } else if (!release.keepsLoadedLockfileVersion()) {
            // Math.max(lock.lockfileVersion || 0, defaultLockfileVersion)
            double version = Js.toNumber(truthy(fileVersion) ? fileVersion : 0);
            lockfileVersion = Double.isNaN(version) ? Double.NaN :
                    (Object) Math.max(version, release.defaultLockfileVersion());
        } else if (truthy(fileVersion) && !Js.strictEquals(fileVersion, 1)) {
            lockfileVersion = fileVersion;
        }

        Object packages = Js.get(data, "packages");
        Object dependencies = Js.get(data, "dependencies");
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : Js.entries(data)) {
            merged.put(entry.getKey(), entry.getValue());
        }
        merged.put("lockfileVersion", lockfileVersion);
        merged.put("requires", true);
        merged.put("packages", truthy(packages) ? indexed(packages) : new LinkedHashMap<>());
        merged.put("dependencies", truthy(dependencies) ? dependencies : new LinkedHashMap<>());
        this.data = merged;

        originalLockfileVersion = fileVersion;

        // data.packages?.['']?.packageExtensionsHash || null, and the .npm-extension file's beside it
        Object rootEntry = packages == null || packages == Js.UNDEFINED ? null : Js.get(packages, "");
        if (rootEntry != null && rootEntry != Js.UNDEFINED) {
            Object extensionsHash = Js.get(rootEntry, "packageExtensionsHash");
            Object extensionHash = Js.get(rootEntry, "npmExtensionHash");
            if ((truthy(extensionsHash) || truthy(extensionHash)) &&
                resolveOptions.getRelease().recordsPatchesAndExtensions()) {
                packageExtensionsHash = truthy(extensionsHash) ? extensionsHash : null;
                npmExtensionHash = truthy(extensionHash) ? extensionHash : null;
            }
        }

        if (!truthy(this.lockfileVersion)) {
            this.lockfileVersion = lockfileVersion;
        }
        ancientLockfile = loadedFromDisk && !(Js.toNumber(fileVersion) >= 2) && !truthy(Js.get(data, "requires"));

        if (truthy(dependencies) && !truthy(packages)) {
            Map<String, Object> pkg = new LinkedHashMap<>();
            try {
                PackageJson manifest = files.manifest("");
                if (manifest != null) {
                    pkg = manifest.getContent();
                }
            } catch (PackageJson.NeedsPackageFiles e) {
                throw e;
            } catch (RuntimeException ignored) {
            }
            loadAll("", null, this.data);
            fixDependencies(pkg);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private Object indexed(Object packages) {
        if (!(packages instanceof List)) {
            return packages;
        }
        packagesArray = (List<Object>) packages;
        Map<String, Object> indexed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : Js.entries(packages)) {
            indexed.put(entry.getKey(), entry.getValue());
        }
        return indexed;
    }

    private void loadAll(String location, @Nullable String name, @Nullable Object lock) {
        Map<String, Object> meta = metaFromLock(location, name, lock);
        if (truthy(meta.get("link"))) {
            location = String.valueOf(meta.get("resolved"));
        }
        Object dependencies = Js.get(lock, "dependencies");
        if (truthy(dependencies)) {
            for (Map.Entry<String, Object> dep : Js.entries(dependencies)) {
                String loc = location + (location.isEmpty() ? "" : "/") + "node_modules/" + dep.getKey();
                loadAll(loc, dep.getKey(), dep.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fixDependencies(Map<String, Object> pkg) {
        Map<String, Object> root = asObject(packages().get(""));
        for (String key : PKG_META_KEYS) {
            if ("libc".equals(key) && !resolveOptions.getRelease().locksLibc()) {
                continue;
            }
            Object val = metaFieldFromPkg(pkg, key);
            if (truthy(val) && isPkgMetaKey(key, resolveOptions.getRelease())) {
                root.put(key.startsWith("_") ? key.substring(1) : key, val);
            }
        }

        for (Map.Entry<String, Object> entry : Js.entries(packages())) {
            String loc = entry.getKey();
            Map<String, Object> meta = asObject(entry.getValue());
            if (!truthy(meta.get("requires")) || loc.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Object> require : Js.entries(meta.get("requires"))) {
                String name = require.getKey();
                Map<String, Object> dep = resolveMetaNode(loc, name);
                String depType = "dependencies";
                if (dep != null && truthy(dep.get("optional")) && !truthy(meta.get("optional"))) {
                    depType = "optionalDependencies";
                } else if (dep != null && truthy(dep.get("dev")) && !truthy(meta.get("dev"))) {
                    depType = "devDependencies";
                }
                if (!truthy(meta.get(depType))) {
                    meta.put(depType, new LinkedHashMap<>());
                }
                ((Map<String, Object>) meta.get(depType)).put(name, require.getValue());
            }
            meta.remove("requires");
        }
    }

    private @Nullable Map<String, Object> resolveMetaNode(String loc, String name) {
        for (String path = loc; ; path = LAST_SEGMENT.matcher(path).replaceFirst("")) {
            String check = path + (path.isEmpty() ? "" : "/") + "node_modules/" + name;
            Object found = Js.get(packages(), check);
            if (truthy(found)) {
                return asObject(found);
            }
            if (path.isEmpty()) {
                break;
            }
        }
        return null;
    }

    private Map<String, Object> loadedData() {
        if (data == null) {
            throw new JsError(JsError.ERROR, "run load() before getting or setting data");
        }
        return data;
    }

    // the first use of a packages primitive is the root entry's write, where arborist fails
    @SuppressWarnings("unchecked")
    private Map<String, Object> packages() {
        Object packages = Objects.requireNonNull(loadedData().get("packages"));
        if (!(packages instanceof Map)) {
            throw new IllegalArgumentException("Cannot create property '' on " + Js.typeOf(packages) + " '" +
                                               Js.string(packages) + "'");
        }
        return (Map<String, Object>) packages;
    }

    private @Nullable Map<String, Object> lockFromLoc(@Nullable Object lock, List<String> path, int i) {
        if (!truthy(lock)) {
            return null;
        }
        Map<String, Object> object = asObject(lock);

        if (i < path.size() && path.get(i).isEmpty()) {
            i++;
        }

        if (i >= path.size()) {
            return object;
        }

        if (!truthy(object.get("dependencies"))) {
            return null;
        }

        return lockFromLoc(Js.get(object.get("dependencies"), path.get(i)), path, i + 1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(@Nullable Object val) {
        return val instanceof Map ? (Map<String, Object>) val : Collections.emptyMap();
    }

    private String pathToLoc(String path) {
        return Relpath.relpath(this.path, PosixPath.resolve(this.path, path));
    }

    public void delete(String nodePath) {
        Map<String, Object> data = loadedData();
        String location = pathToLoc(nodePath);
        awaitingUpdate.remove(location);

        packages().remove(location);
        List<String> path = Arrays.asList(NODE_MODULES.split(location, -1));
        String name = path.get(path.size() - 1);
        Map<String, Object> pLock = lockFromLoc(data, path.subList(0, path.size() - 1), 0);
        if (pLock != null && truthy(pLock.get("dependencies"))) {
            deleteProperty(pLock.get("dependencies"), name);
        }
    }

    // delete value[name] in strict mode: an own property a string or array cannot lose throws
    private static void deleteProperty(Object value, String name) {
        if (value instanceof Map) {
            ((Map<?, ?>) value).remove(name);
            return;
        }
        boolean ownIndex = value instanceof String && Js.arrayIndex(name) >= 0 &&
                           Js.arrayIndex(name) < ((String) value).length();
        if ((value instanceof String || value instanceof List) && ("length".equals(name) || ownIndex)) {
            throw new IllegalArgumentException("Cannot delete property '" + name + "' of [object " +
                                               (value instanceof List ? "Array" : "String") + "]");
        }
    }

    public Map<String, Object> get(String nodePath) {
        Map<String, Object> data = loadedData();

        String location = pathToLoc(nodePath);
        if (awaitingUpdate.containsKey(location)) {
            updateWaitingNode(location);
        }

        Object meta = Js.get(packages(), location);
        if (truthy(meta)) {
            return asObject(meta);
        }

        List<String> path = Arrays.asList(NODE_MODULES.split(location, -1));
        String name = path.get(path.size() - 1);
        Map<String, Object> lock = lockFromLoc(data, path, 0);

        return metaFromLock(location, name, lock);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> metaFromLock(String location, @Nullable String name, @Nullable Object entry) {
        if (!truthy(entry)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> lock = asObject(entry);

        Spec spec = SpecFromLock.specFromLock(name, lock, path, resolveOptions.getRelease().npmPackageArg());
        Spec.Type type = spec == null ? null : spec.getType();

        if (spec != null && type == Spec.Type.DIRECTORY) {
            String target = Relpath.relpath(path, spec.getFetchSpec());
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("link", true);
            link.put("resolved", target);
            packages().put(location, link);
            if (!truthy(Js.get(packages(), target))) {
                Map<String, Object> targetLock = new LinkedHashMap<>(lock);
                targetLock.put("version", null);
                metaFromLock(target, name, targetLock);
            }
            return (Map<String, Object>) packages().get(location);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        Object requires = lock.get("requires");
        if (requires instanceof Map || requires instanceof List) {
            meta.put("requires", requires);
        }

        if (truthy(lock.get("optional"))) {
            meta.put("optional", true);
        }
        if (truthy(lock.get("dev"))) {
            meta.put("dev", true);
        }

        if (location.isEmpty() && lock.containsKey("name")) {
            meta.put("name", lock.get("name"));
        }

        if (truthy(lock.get("integrity"))) {
            meta.put("integrity", lock.get("integrity"));
        }

        if (truthy(lock.get("version")) && !truthy(lock.get("integrity"))) {
            if (spec != null && type == Spec.Type.GIT) {
                meta.put("resolved", ConsistentResolve.consistentResolve(spec, path, path, false,
                        resolveOptions.getRelease()));
                packages().put(location, meta);
                return meta;
            } else if (spec != null && spec.isRegistry()) {
                meta.put("version", lock.get("version"));
            }
        }

        if (truthy(lock.get("resolved")) || (spec != null && type != null && !spec.isRegistry())) {
            if (spec != null && spec.isRegistry()) {
                meta.put("resolved", lock.get("resolved"));
            } else if (spec != null && type == Spec.Type.FILE) {
                meta.put("resolved", ConsistentResolve.consistentResolve(spec, path, path, true,
                        resolveOptions.getRelease()));
            } else if (spec != null && truthy(spec.getFetchSpec())) {
                meta.put("resolved", spec.getFetchSpec());
            }
        }

        if (!truthy(meta.get("version")) && spec != null) {
            if (type == Spec.Type.FILE || type == Spec.Type.REMOTE) {
                VersionFromTgz.NameVersion fromTgz = VersionFromTgz.versionFromTgz(spec.getName(), spec.getFetchSpec(),
                        resolveOptions.getRelease());
                if (fromTgz == null) {
                    fromTgz = VersionFromTgz.versionFromTgz(spec.getName(), (String) meta.get("resolved"),
                            resolveOptions.getRelease());
                }
                if (fromTgz != null) {
                    meta.put("version", fromTgz.getVersion());
                    if (!fromTgz.getName().equals(name)) {
                        meta.put("name", fromTgz.getName());
                    }
                }
            } else if (type == Spec.Type.ALIAS) {
                meta.put("name", spec.getSubSpec().getName());
                meta.put("version", spec.getSubSpec().getFetchSpec());
            } else if (type == Spec.Type.VERSION) {
                meta.put("version", spec.getFetchSpec());
            }
        }

        if (truthy(lock.get("bundled"))) {
            meta.put("inBundle", true);
        }

        packages().put(location, meta);
        return meta;
    }

    public void add(Node node) {
        loadedData();

        String loc = Relpath.relpath(path, node.getPath());
        if (path.equals(node.getPath())) {
            tree = node;
        }

        if (node.getResolved() == null || node.getIntegrity() == null) {
            Map<String, Object> locked = get(node.getPath());
            Object resolved = locked.get("resolved");
            Object integrity = locked.get("integrity");
            Object hasShrinkwrap = locked.get("hasShrinkwrap");
            Object version = locked.get("version");

            Object pathFixed = null;
            if (truthy(resolved)) {
                if (!Js.string(resolved).startsWith("file:")) {
                    pathFixed = resolved;
                } else if (resolved instanceof String) {
                    pathFixed = "file:" + ConsistentResolve.escapeHash(
                            PosixPath.resolve(path, ((String) resolved).substring(5)), resolveOptions.getRelease());
                } else {
                    // only an array reads as a file: string, and Array.prototype.slice hands path.resolve an array
                    throw new IllegalArgumentException(
                            "The \"paths[1]\" argument must be of type string. Received an instance of Array");
                }
            }

            boolean resolvedOk = !truthy(resolved) || !truthy(node.getResolved()) ||
                                 Js.strictEquals(node.getResolved(), pathFixed);
            boolean integrityOk = !truthy(integrity) || !truthy(node.getIntegrity()) ||
                                  Js.strictEquals(node.getIntegrity(), integrity);
            // before npm 7.15.1 the version the lock records at the path is not compared
            boolean checksVersion = resolveOptions.getRelease().checksVersionOfLockedEntry();
            boolean versionOk = !checksVersion || !truthy(version) || !truthy(node.getVersion()) ||
                                Js.strictEquals(version, node.getVersion());

            boolean allOk = (truthy(resolved) || truthy(integrity) || checksVersion && truthy(version)) &&
                            resolvedOk && integrityOk && versionOk;

            if (!allOk) {
                Map<String, Object> fromNode = metaFromNode(node, path, resolveOptions);
                pathFixed = fromNode.get("resolved");
                integrity = fromNode.get("integrity");
                hasShrinkwrap = fromNode.get("hasShrinkwrap");
            }
            node.setResolved(truthy(node.getResolved()) ? node.getResolved() : truthy(pathFixed) ? pathFixed : null);
            node.setIntegrity(truthy(node.getIntegrity()) ? node.getIntegrity() : truthy(integrity) ? integrity : null);
            // from npm 12.0.0 the lock's hasShrinkwrap is not read
            node.setHasShrinkwrap(truthy(node.getHasShrinkwrap()) ? node.getHasShrinkwrap() :
                    truthy(hasShrinkwrap) && resolveOptions.getRelease().readsNpmShrinkwrap() ? hasShrinkwrap : false);
        }
        awaitingUpdate.put(loc, node);
    }

    public void addEdge(Edge edge) {
        YarnLock yarnLock = this.yarnLock;
        if (yarnLock == null || !edge.isValid()) {
            return;
        }

        Node node = Objects.requireNonNull(edge.getTo());

        if (node.getResolved() != null && node.getIntegrity() != null) {
            return;
        }

        Map<String, YarnLock.Entry> entries = yarnLock.getEntries();
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Object pathFixed = null;
        if (truthy(node.getResolved())) {
            if (!Js.string(node.getResolved()).contains("file:")) {
                pathFixed = node.getResolved();
            } else {
                pathFixed = ConsistentResolve.consistentResolve(node.getResolved(), node.getPath(), path, true,
                        resolveOptions.getRelease());
            }
        }

        Spec spec = Spec.npa(node.getName() + "@" + edge.getSpec(), null,
                resolveOptions.getRelease().npmPackageArg());
        YarnLock.Entry entry = entries.get(node.getName() + "@" + edge.getSpec());

        if (entry == null ||
            mismatch(node.getVersion(), entry.get("version")) ||
            mismatch(node.getIntegrity(), entry.get("integrity")) ||
            mismatch(pathFixed, entry.get("resolved"))) {
            return;
        }

        Object entryResolved = entry.get("resolved");
        // before npm 7.5.6 the yarn registry's URL is taken as it is
        if (resolveOptions.getRelease().rewritesYarnRegistryOfEdgeEntries() && entryResolved instanceof String &&
            yarnRegRe().matcher((String) entryResolved).find() && spec.isRegistry()) {
            entryResolved = yarnRegRe().matcher((String) entryResolved).replaceFirst("https://registry.npmjs.org/");
            entry.set("resolved", entryResolved);
        }

        Object entryIntegrity = entry.get("integrity");
        node.setIntegrity(truthy(node.getIntegrity()) ? node.getIntegrity() :
                truthy(entryIntegrity) ? entryIntegrity : null);
        Object resolved = truthy(node.getResolved()) ? node.getResolved() :
                ConsistentResolve.consistentResolve(entryResolved, path, node.getPath(), false,
                        resolveOptions.getRelease());
        node.setResolved(truthy(resolved) ? resolved : null);

        awaitingUpdate.put(Relpath.relpath(path, node.getPath()), node);
    }

    private static boolean mismatch(@Nullable Object a, @Nullable Object b) {
        return truthy(a) && truthy(b) && !Js.strictEquals(a, b);
    }

    /**
     * What {@link #get} would give a node placed at each location, as values: the entries of {@code packages}, with
     * those of the nodes awaiting an update as they would be written, and the legacy {@code dependencies} it falls back
     * on.
     */
    List<Object> placementState() {
        Map<String, Object> awaiting = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : awaitingUpdate.entrySet()) {
            Object meta;
            try {
                meta = metaFromNode(entry.getValue(), path, resolveOptions);
            } catch (RuntimeException e) {
                // what writing it would throw, which this state leaves to the write
                meta = e.toString();
            }
            awaiting.put(entry.getKey(), meta);
        }
        Map<String, Object> data = loadedData();
        return Arrays.asList(copy(data.get("packages")), awaiting, copy(data.get("dependencies")));
    }

    // a copy of a JSON value that later changes to the value leave alone
    private static @Nullable Object copy(@Nullable Object value) {
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                copy.put(entry.getKey(), copy(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object element : (List<?>) value) {
                copy.add(copy(element));
            }
            return copy;
        }
        return value;
    }

    private void updateWaitingNode(String loc) {
        Node node = awaitingUpdate.remove(loc);
        packages().put(loc, metaFromNode(node, path, resolveOptions));
    }

    /**
     * An extraneous node outside node_modules that is a direct fsChild of the root, or a detached link
     * target: a workspace or {@code file:} dependency the manifest no longer names gets no entry.
     */
    static boolean isStaleTopLevelEntry(Node node, String loc) {
        if (!node.getRelease().dropsExtraneousOutsideNodeModules()) {
            return false;
        }
        if (!node.getRelease().keepsNestedExtraneousFsChildren()) {
            return node.isExtraneous() && !NODE_MODULES.matcher(loc).find() && !"node_modules".equals(loc);
        }
        return node.isExtraneous() && !NODE_MODULES.matcher(loc).find() && !"node_modules".equals(loc) &&
               (node.getFsParent() == null || node.getFsParent().isRoot());
    }

    /**
     * Rebuilds the lock data from the tree and returns what is written for the lockfile version.
     */
    public Map<String, Object> commit() {
        Map<String, Object> data = loadedData();
        Node tree = this.tree;
        if (tree != null) {
            if (yarnLock != null) {
                yarnLock.fromTree(tree);
            }
            Map<String, Object> root = metaFromNode(Objects.requireNonNull(tree.getTarget()), path, resolveOptions);
            // npm 12.0.0 records the state of the root's manifest extensions on the root entry
            if (truthy(packageExtensionsHash)) {
                root.put("packageExtensionsHash", packageExtensionsHash);
            }
            if (truthy(npmExtensionHash)) {
                root.put("npmExtensionHash", npmExtensionHash);
            }
            Map<String, Object> packages = new LinkedHashMap<>();
            data.put("packages", packages);
            packagesArray = null;
            if (!root.isEmpty()) {
                packages.put("", root);
            }
            // before npm 7.1.1 the tree's own inventory, which arborist 1 never lists the root in
            boolean ofTreeRoot = resolveOptions.getRelease().commitsInventoryOfTreeRoot();
            for (Node node : (ofTreeRoot ? tree.getRoot() : tree).getInventory().values()) {
                if (ofTreeRoot && (node == tree || node.isRoot() || "".equals(node.getLocation()))) {
                    continue;
                }
                String loc = Relpath.relpath(path, node.getPath());
                if (isStaleTopLevelEntry(node, loc)) {
                    continue;
                }
                Map<String, Object> meta = metaFromNode(node, path, resolveOptions);
                if (resolveOptions.getRelease().skipsInertNodesWithoutVersion() && node.isInert() &&
                    !truthy(node.getPackage().get("version"))) {
                    continue;
                }
                packages.put(loc, meta);
            }
        } else if (!awaitingUpdate.isEmpty()) {
            for (String loc : new ArrayList<>(awaitingUpdate.keySet())) {
                updateWaitingNode(loc);
            }
        }

        if (!truthy(lockfileVersion)) {
            lockfileVersion = resolveOptions.getRelease().defaultLockfileVersion();
        }
        // only an object's values hold an entry to read
        Map<String, Object> entries = asObject(data.get("packages"));
        if (holdsPatchOrExtensionState(entries) && resolveOptions.getRelease().recordsPatchesAndExtensions()) {
            // a patched node, and the state of a manifest extension, make the lock version 4, which an older npm
            // refuses
            if (some(entries, "patched") && Js.toNumber(lockfileVersion) < PATCHED_LOCKFILE_VERSION) {
                lockfileVersion = PATCHED_LOCKFILE_VERSION;
            }
            boolean hasExtensionState = truthy(packageExtensionsHash) || truthy(npmExtensionHash) ||
                                        some(entries, "packageExtensionsApplied", "npmExtensionApplied");
            if (hasExtensionState && Js.toNumber(lockfileVersion) < PACKAGE_EXTENSIONS_LOCKFILE_VERSION) {
                lockfileVersion = PACKAGE_EXTENSIONS_LOCKFILE_VERSION;
            }
        }
        data.put("lockfileVersion", lockfileVersion);

        double version = Js.toNumber(lockfileVersion);
        if (tree != null && version <= 3) {
            buildLegacyLockfile(tree, data, Collections.emptyList());
        }

        Map<String, Object> out = new LinkedHashMap<>(data);
        if (packagesArray != null) {
            out.put("packages", packagesArray);
        }
        if (version >= 3) {
            out.remove("dependencies");
        } else if (version < 2) {
            out.remove("packages");
        }
        return out;
    }

    // whether commit has anything of npm 12.0.0's to read: a hash of the root's, or an entry that holds a patch or an
    // extension record or that is no object to read one from
    private boolean holdsPatchOrExtensionState(Map<String, Object> packages) {
        if (truthy(packageExtensionsHash) || truthy(npmExtensionHash)) {
            return true;
        }
        for (Object p : packages.values()) {
            if (!(p instanceof Map) || truthy(((Map<?, ?>) p).get("patched")) ||
                truthy(((Map<?, ?>) p).get("packageExtensionsApplied")) ||
                truthy(((Map<?, ?>) p).get("npmExtensionApplied"))) {
                return true;
            }
        }
        return false;
    }

    // Object.values(this.data.packages).some(p => p[key] || ...)
    private static boolean some(Map<String, Object> packages, String... keys) {
        for (String location : Js.keys(packages)) {
            Object p = packages.get(location);
            for (String key : keys) {
                if (truthy(Js.get(p, key))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Object> buildLegacyLockfile(Node node, Map<String, Object> lock, List<String> path) {
        if (node == tree) {
            lock.put("name", truthy(node.getPackageName()) ? node.getPackageName() : node.getName());
            if (truthy(node.getVersion())) {
                lock.put("version", node.getVersion());
            }
        }

        List<Edge> valid = new ArrayList<>();
        for (Edge e : node.getEdgesIn()) {
            if (e.isValid()) {
                valid.add(e);
            }
        }
        valid.sort((a, b) -> {
            String[] aloc = a.getFrom().getLocation().split("node_modules", -1);
            String[] bloc = b.getFrom().getLocation().split("node_modules", -1);
            if (aloc.length > bloc.length) {
                return 1;
            }
            if (bloc.length > aloc.length) {
                return -1;
            }
            return LocaleCompare.en(aloc[aloc.length - 1], bloc[bloc.length - 1]);
        });
        Edge edge = valid.isEmpty() ? null : valid.get(0);

        Object res = ConsistentResolve.consistentResolve(node.getResolved(), this.path, this.path, true,
                resolveOptions.getRelease());
        Spec rSpec = specFromResolved(res, resolveOptions.getRelease());

        Spec spec = rSpec;
        if (edge != null) {
            spec = Spec.resolve(node.getName(), edge.getSpec(), edge.getFrom().getRealpath(),
                    resolveOptions.getRelease().npmPackageArg());
        }

        Spec.Type specType = spec == null ? null : spec.getType();
        Spec.Type rType = rSpec == null ? null : rSpec.getType();
        if (node.isLink()) {
            lock.put("version", "file:" + ConsistentResolve.escapeHash(
                    Relpath.relpath(this.path, node.getRealpath()), resolveOptions.getRelease()));
        } else if (specType == Spec.Type.FILE || specType == Spec.Type.REMOTE) {
            lock.put("version", spec.getSaveSpec());
        } else if (specType == Spec.Type.GIT || rType == Spec.Type.GIT) {
            lock.put("version", node.getResolved());
            if (spec != null && truthy(spec.getRaw())) {
                lock.put("from", spec.getRaw());
            }
        } else if (!node.isRoot() && truthy(node.getPackageName()) &&
                   !Js.strictEquals(node.getPackageName(), node.getName())) {
            lock.put("version", "npm:" + Js.string(node.getPackageName()) + "@" + Js.string(node.getVersion()));
        } else if (truthy(node.getVersion())) {
            lock.put("version", node.getVersion());
        }

        if (node.isInDepBundle()) {
            lock.put("bundled", true);
        }

        if (truthy(node.getResolved()) && !node.isLink() &&
            rType != Spec.Type.GIT && rType != Spec.Type.FILE && rType != Spec.Type.DIRECTORY &&
            specType != Spec.Type.DIRECTORY && specType != Spec.Type.GIT && specType != Spec.Type.FILE &&
            specType != Spec.Type.REMOTE) {
            Object resolved = OverrideResolves.overrideResolves(node.getResolved(), resolveOptions);
            if (resolved != null) {
                lock.put("resolved", resolved);
            }
        }

        if (truthy(node.getIntegrity())) {
            lock.put("integrity", node.getIntegrity());
        }

        if (node.isExtraneous()) {
            lock.put("extraneous", true);
        } else if (!node.isLink()) {
            if (node.isPeer()) {
                lock.put("peer", true);
            }
            if (node.isDevOptional() && !node.isDev() && !node.isOptional()) {
                lock.put("devOptional", true);
            }
            if (node.isDev()) {
                lock.put("dev", true);
            }
            if (node.isOptional()) {
                lock.put("optional", true);
            }
        }

        Node depender = Objects.requireNonNull(node.getTarget());
        if (depender.getEdgesOut().size() > 0) {
            if (node != tree) {
                Map<String, Object> requires = new LinkedHashMap<>();
                for (Map.Entry<String, Edge> entry : depender.getEdgesOut().entries()) {
                    Edge v = entry.getValue();
                    if (v.isPeer()) {
                        continue;
                    }
                    String edgeSpec = v.getSpec();
                    // before npm 7.1.1 a file: spec is written as the manifest has it
                    if (edgeSpec.startsWith("file:") && resolveOptions.getRelease().relativizesFileRequires()) {
                        String p = PosixPath.resolve(node.getRealpath(), edgeSpec.substring("file:".length()));
                        requires.put(entry.getKey(), "file:" + ConsistentResolve.escapeHash(
                                Relpath.relpath(node.getRealpath(), p), resolveOptions.getRelease()));
                    } else {
                        requires.put(entry.getKey(), edgeSpec);
                    }
                }
                lock.put("requires", requires);
            } else {
                lock.put("requires", true);
            }
        }

        CaseInsensitiveMap<Node> children = depender.getChildren();
        if (children.isEmpty()) {
            lock.remove("dependencies");
        } else {
            List<String> kidPath = new ArrayList<>(path);
            kidPath.add(node.getRealpath());
            Map<String, Object> dependencies = new LinkedHashMap<>();
            boolean found = false;
            for (Map.Entry<String, Node> entry : children.entries()) {
                Node kid = entry.getValue();
                if (path.contains(kid.getRealpath())) {
                    continue;
                }
                dependencies.put(entry.getKey(), buildLegacyLockfile(kid, new LinkedHashMap<>(), kidPath));
                found = true;
            }
            if (found) {
                lock.put("dependencies", dependencies);
            }
        }
        return lock;
    }

    private static @Nullable Spec specFromResolved(@Nullable Object resolved, NpmRelease release) {
        try {
            return Spec.npa(resolved, null, release.npmPackageArg());
        } catch (RuntimeException er) {
            return null;
        }
    }

    /**
     * The yarn.lock save writes beside the lock, rebuilt from the tree by {@link #commit()}; null when it writes none.
     */
    public @Nullable String yarnLockText() {
        return yarnLock == null || yarnLock.getEntries().isEmpty() ? null : yarnLock.toString();
    }

    public Map<String, Object> toJSON() {
        loadedData();
        return commit();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * @param format {@code true} for the lock's own indentation, {@code false} for none, or an indent
     *               string or width of its own
     */
    public String toString(Object format) {
        Map<String, Object> data = toJSON();
        Object defaultIndent = truthy(indent) ? indent : 2;
        Object indent = Boolean.TRUE.equals(format) ? defaultIndent : truthy(format) ? format : 0;
        String eol = truthy(format) ? (truthy(newline) ? newline : "\n") : "";
        return JsonStringifyNice.stringify(data, KEY_ORDER, indent, resolveOptions.getRelease().jsonStringifyNice())
                .replace("\n", eol);
    }
}
