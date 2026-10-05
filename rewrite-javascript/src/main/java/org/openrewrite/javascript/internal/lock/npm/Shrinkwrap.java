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
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/shrinkwrap.js (npm 11.17.0): the lock metadata a tree's root carries,
 * kept current as nodes enter, move and leave.
 */
public final class Shrinkwrap {

    private static final Pattern NODE_MODULES = Pattern.compile("(?:^|/)node_modules/");
    private static final Pattern YARN_REGISTRY = Pattern.compile("^https?://registry\\.yarnpkg\\.com/");

    private static final List<String> PKG_META_KEYS = Collections.unmodifiableList(Arrays.asList(
            "version", "dependencies", "peerDependencies", "peerDependenciesMeta", "optionalDependencies",
            "bundleDependencies", "acceptDependencies", "funding", "engines", "os", "cpu", "libc", "_integrity",
            "license", "_hasShrinkwrap", "hasInstallScript", "bin", "deprecated", "workspaces"));

    /**
     * Arborist's {@code resolveOptions}.
     */
    @Value
    public static class ResolveOptions {
        public static final ResolveOptions DEFAULT = new ResolveOptions(false);

        boolean omitLockfileRegistryResolved;
    }

    private final Map<String, Node> awaitingUpdate = new LinkedHashMap<>();

    @Getter
    private final String path;

    @Getter
    private final ResolveOptions resolveOptions;

    @Getter
    private @Nullable Node tree;

    /**
     * The lock as JSON; {@code packages} lags the tree until an entry is read through {@link #get}.
     */
    @Getter
    @Setter
    private @Nullable Map<String, Object> data;

    @Getter
    @Setter
    private @Nullable YarnLock yarnLock;

    public Shrinkwrap(String path) {
        this(path, ResolveOptions.DEFAULT);
    }

    public Shrinkwrap(String path, ResolveOptions resolveOptions) {
        this.path = PosixPath.resolve(path);
        this.resolveOptions = resolveOptions;
    }

    public static Map<String, Object> metaFromNode(Node node, String path) {
        return metaFromNode(node, path, ResolveOptions.DEFAULT);
    }

    public static Map<String, Object> metaFromNode(Node node, String path, ResolveOptions options) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (node.isLink()) {
            meta.put("resolved", Relpath.relpath(path, node.getRealpath()));
            meta.put("link", true);
            return meta;
        }

        for (String key : PKG_META_KEYS) {
            Object val = metaFieldFromPkg(node.getPackage(), key);
            if (truthy(val)) {
                meta.put(key.startsWith("_") ? key.substring(1) : key, val);
            }
        }
        Object pname = node.getPackageName();
        if (truthy(pname) && (node.isRoot() || !Js.strictEquals(pname, node.getName()) ||
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
        if (node.isHasShrinkwrap()) {
            meta.put("hasShrinkwrap", true);
        }
        if (node.hasInstallScript()) {
            meta.put("hasInstallScript", true);
        }

        String resolved = ConsistentResolve.consistentResolve(node.getResolved(), node.getPath(), path, true);
        if (truthy(resolved)) {
            String written = node.isRegistryDependency() ?
                    OverrideResolves.overrideResolves(resolved, options) : resolved;
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

    private Map<String, Object> loadedData() {
        if (data == null) {
            throw new IllegalStateException("run load() before getting or setting data");
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> packages() {
        return (Map<String, Object>) Objects.requireNonNull(loadedData().get("packages"));
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
        if (pLock != null && pLock.get("dependencies") instanceof Map) {
            ((Map<?, ?>) pLock.get("dependencies")).remove(name);
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
    private Map<String, Object> metaFromLock(String location, String name, @Nullable Map<String, Object> lock) {
        if (lock == null) {
            return new LinkedHashMap<>();
        }

        Spec spec = SpecFromLock.specFromLock(name, lock, path);
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
                meta.put("resolved", ConsistentResolve.consistentResolve(spec, path, path, false));
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
                meta.put("resolved", ConsistentResolve.consistentResolve(spec, path, path, true));
            } else if (spec != null && truthy(spec.getFetchSpec())) {
                meta.put("resolved", spec.getFetchSpec());
            }
        }

        if (!truthy(meta.get("version")) && spec != null) {
            if (type == Spec.Type.FILE || type == Spec.Type.REMOTE) {
                VersionFromTgz.NameVersion fromTgz = VersionFromTgz.versionFromTgz(spec.getName(), spec.getFetchSpec());
                if (fromTgz == null) {
                    fromTgz = VersionFromTgz.versionFromTgz(spec.getName(), (String) meta.get("resolved"));
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
                if (!(resolved instanceof String) || !((String) resolved).startsWith("file:")) {
                    pathFixed = resolved;
                } else {
                    pathFixed = "file:" + PosixPath.resolve(path, ((String) resolved).substring(5));
                }
            }

            boolean resolvedOk = !truthy(resolved) || !truthy(node.getResolved()) ||
                                 node.getResolved().equals(pathFixed);
            boolean integrityOk = !truthy(integrity) || !truthy(node.getIntegrity()) ||
                                  node.getIntegrity().equals(integrity);
            boolean versionOk = !truthy(version) || !truthy(node.getVersion()) ||
                                Js.strictEquals(version, node.getVersion());

            boolean allOk = (truthy(resolved) || truthy(integrity) || truthy(version)) &&
                            resolvedOk && integrityOk && versionOk;

            if (!allOk) {
                Map<String, Object> fromNode = metaFromNode(node, path, resolveOptions);
                pathFixed = fromNode.get("resolved");
                integrity = fromNode.get("integrity");
                hasShrinkwrap = fromNode.get("hasShrinkwrap");
            }
            node.setResolved(truthy(node.getResolved()) ? node.getResolved() :
                    truthy(pathFixed) ? String.valueOf(pathFixed) : null);
            node.setIntegrity(truthy(node.getIntegrity()) ? node.getIntegrity() :
                    truthy(integrity) ? String.valueOf(integrity) : null);
            node.setHasShrinkwrap(node.isHasShrinkwrap() || truthy(hasShrinkwrap));
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

        String pathFixed = null;
        if (truthy(node.getResolved())) {
            if (!node.getResolved().contains("file:")) {
                pathFixed = node.getResolved();
            } else {
                pathFixed = ConsistentResolve.consistentResolve(node.getResolved(), node.getPath(), path, true);
            }
        }

        Spec spec = Spec.npa(node.getName() + "@" + edge.getSpec());
        YarnLock.Entry entry = entries.get(node.getName() + "@" + edge.getSpec());

        if (entry == null ||
            mismatch(node.getVersion(), entry.get("version")) ||
            mismatch(node.getIntegrity(), entry.get("integrity")) ||
            mismatch(pathFixed, entry.get("resolved"))) {
            return;
        }

        Object entryResolved = entry.get("resolved");
        if (entryResolved instanceof String && YARN_REGISTRY.matcher((String) entryResolved).find() &&
            spec.isRegistry()) {
            entryResolved = YARN_REGISTRY.matcher((String) entryResolved).replaceFirst("https://registry.npmjs.org/");
            entry.set("resolved", entryResolved);
        }

        Object entryIntegrity = entry.get("integrity");
        node.setIntegrity(truthy(node.getIntegrity()) ? node.getIntegrity() :
                entryIntegrity instanceof String && truthy(entryIntegrity) ? (String) entryIntegrity : null);
        String resolved = truthy(node.getResolved()) ? node.getResolved() :
                ConsistentResolve.consistentResolve(entryResolved instanceof String ? (String) entryResolved : null,
                        path, node.getPath(), false);
        node.setResolved(truthy(resolved) ? resolved : null);

        awaitingUpdate.put(Relpath.relpath(path, node.getPath()), node);
    }

    private static boolean mismatch(@Nullable Object a, @Nullable Object b) {
        return truthy(a) && truthy(b) && !Js.strictEquals(a, b);
    }

    private void updateWaitingNode(String loc) {
        Node node = awaitingUpdate.remove(loc);
        packages().put(loc, metaFromNode(node, path, resolveOptions));
    }
}
