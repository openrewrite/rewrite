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

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/load-virtual.js (npm 11.17.0): the tree a lock describes, with
 * the project's own manifests read from the files it was given rather than the disk.
 */
public final class VirtualTreeLoader {

    /**
     * What a lock-only install reads from the project: the lock, a yarn.lock beside it, and the package.json of the
     * root ({@code ""}) and of every link target, by location, as text or parsed; each is read once.
     */
    public static final class ProjectFiles {
        private static final Object NONE = new Object();

        @Getter
        private final @Nullable LockFile lock;

        @Getter
        private final @Nullable String yarnLock;

        /**
         * The manifests the host supplied, by location.
         */
        @Getter
        private final Map<String, String> manifests;

        /**
         * Manifests held parsed already: the lock's own entries, standing in where the host holds no package.json.
         */
        private final Map<String, Map<String, Object>> parsedManifests;

        /**
         * Locations whose package.json the host holds without its contents.
         */
        private final Set<String> unread;

        /**
         * Locations whose package npm reads with something the engine is not given, each with what that is.
         */
        private final Map<String, CannotKnow> unknown;

        /**
         * How many directories the project's is below the root of what the host holds; null when it has not said.
         */
        private final @Nullable Integer depth;

        /**
         * The npm release that reads the files: a manifest is normalized by the @npmcli/package-json it bundles.
         */
        private final NpmRelease release;

        /**
         * The files the project holds, relative to its directory, when the host says; npm 12.0.0 reads the patch
         * files among them.
         */
        @Getter
        private final @Nullable Set<String> paths;

        private final Map<String, Object> read = new HashMap<>();

        @Builder
        ProjectFiles(@Nullable LockFile lock, @Nullable String yarnLock, @Singular Map<String, String> manifests,
                     @Singular Map<String, Map<String, Object>> parsedManifests,
                     @Singular("unread") Set<String> unread, @Singular("unknown") Map<String, CannotKnow> unknown,
                     @Nullable Integer depth, @Nullable NpmRelease release, @Nullable Set<String> paths) {
            this.lock = lock;
            this.yarnLock = yarnLock;
            this.manifests = manifests;
            this.parsedManifests = parsedManifests;
            this.unread = unread;
            this.unknown = unknown;
            this.depth = depth;
            this.release = release != null ? release : NpmRelease.PORTED;
            this.paths = paths;
        }

        /**
         * These files as the npm {@code release} reads them, the project holding {@code paths} (null when the host
         * does not say).
         */
        ProjectFiles readBy(NpmRelease release, @Nullable Set<String> paths) {
            return new ProjectFiles(lock, yarnLock, manifests, parsedManifests, unread, unknown, depth, release,
                    paths);
        }

        /**
         * The manifest at a location, normalized as arborist reads one from disk, or as it was given parsed;
         * null where there is none. A text that does not normalize throws the same error on every read, and
         * {@link CannotKnow} is thrown for a package.json the host holds unread or one above the root of what it holds.
         */
        public @Nullable PackageJson manifest(String location) {
            Object value = read.get(location);
            if (value == null) {
                String text = manifests.get(location);
                Map<String, Object> parsed = parsedManifests.get(location);
                try {
                    if (unknown.containsKey(location)) {
                        throw unknown.get(location);
                    }
                    if (depth != null && climbs(location) > depth) {
                        throw new CannotKnow("the package.json of " + location + ", which is outside the repository");
                    }
                    if (text == null && parsed == null && unread.contains(location)) {
                        throw CannotKnow.unread((location.isEmpty() ? "" : location + "/") + "package.json");
                    }
                    value = text != null ? PackageJson.normalize(text, release) :
                        parsed != null ? PackageJson.parsed(parsed) : NONE;
                } catch (RuntimeException e) {
                    value = e;
                }
                read.put(location, value);
            }
            if (value instanceof RuntimeException) {
                throw (RuntimeException) value;
            }
            return value == NONE ? null : (PackageJson) value;
        }

        /**
         * Every location that holds a package.json.
         */
        public Set<String> manifestLocations() {
            Set<String> locations = new LinkedHashSet<>(manifests.keySet());
            locations.addAll(parsedManifests.keySet());
            locations.addAll(unread);
            return locations;
        }

        // the directories a location from the project's climbs above it
        static int climbs(String location) {
            int up = 0;
            for (String segment : location.split("/")) {
                if (!"..".equals(segment)) {
                    break;
                }
                up++;
            }
            return up;
        }
    }

    /**
     * {@code ENOLOCK}: no lock to load a tree from.
     */
    public static final class NoLockException extends IllegalStateException implements NpmError {
        NoLockException() {
            super("loadVirtual requires existing shrinkwrap file");
        }

        @Override
        public String getCode() {
            return "ENOLOCK";
        }
    }

    /**
     * {@code EMISSINGTARGET}: a link whose target the lock does not hold.
     */
    public static final class MissingTargetException extends IllegalStateException implements NpmError {
        MissingTargetException(String targetLoc, String location) {
            super("Missing target in lock file: \"" + targetLoc + "\" is referenced by \"" + location +
                  "\" but does not exist.\nTo fix:\n1. rm package-lock.json\n2. npm install");
        }

        @Override
        public String getCode() {
            return "EMISSINGTARGET";
        }
    }

    private final String path;
    private final NpmOptions options;

    @Getter
    private @Nullable Node virtualTree;

    /**
     * Whether the root manifest and the lock disagreed, so that the flags were recalculated.
     */
    @Getter
    private boolean flagsSuspect;

    private @Nullable Node rootOptionProvided;
    private ProjectFiles files = ProjectFiles.builder().build();

    public VirtualTreeLoader(String path, NpmOptions options) {
        this.path = PosixPath.resolve(path);
        this.options = options;
    }

    public Node loadVirtual(ProjectFiles files) {
        return loadVirtual(files, null);
    }

    /**
     * @param root the root node to load the lock under, as the ideal tree builder supplies it; one that
     *             already carries a {@link Shrinkwrap} is loaded from that without reading the files
     */
    public Node loadVirtual(ProjectFiles files, @Nullable Node root) {
        if (virtualTree != null) {
            return virtualTree;
        }
        this.files = files;

        if (root != null && root.getMeta() != null) {
            loadFromShrinkwrap(root.getMeta(), root);
            return Objects.requireNonNull(virtualTree);
        }

        Shrinkwrap s = new Shrinkwrap(path, options).load(files);
        if (!s.isLoadedFromDisk() && root == null) {
            throw new NoLockException();
        }

        Object pkg = rootPackage(s);
        rootOptionProvided = root;
        if (root == null) {
            root = NpmLockRegenerator.setWorkspaces(loadNode("", pkg, true), files);
        }

        loadFromShrinkwrap(s, root);
        root.assertRootOverrides();
        return Objects.requireNonNull(virtualTree);
    }

    // PackageJson.normalize(this.path).then(p => p.content).catch(() => s.data.packages[''] || {})
    private Object rootPackage(Shrinkwrap s) {
        try {
            PackageJson manifest = files.manifest("");
            if (manifest != null) {
                return manifest.getContent();
            }
        } catch (PackageJson.NeedsPackageFiles | CannotKnow e) {
            throw e;
        } catch (RuntimeException ignored) {
        }
        Object fromLock = Js.get(Objects.requireNonNull(s.getData()).get("packages"), "");
        return truthy(fromLock) ? fromLock : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private void loadFromShrinkwrap(Shrinkwrap s, Node root) {
        // before npm 7.5.4 a root the caller provides is treated as any other
        boolean suspectsProvidedRoot = options.getRelease().suspectsFlagsUnderProvidedRoot();
        if (rootOptionProvided == null || !suspectsProvidedRoot) {
            root.unsetDepFlags();
        } else {
            flagsSuspect = true;
        }

        checkRootEdges(s, root);
        root.setMeta(s);
        virtualTree = root;
        Map<String, Object> links = new LinkedHashMap<>();
        Map<String, Node> nodes = new LinkedHashMap<>();
        nodes.put("", root);
        for (Map.Entry<String, Object> entry : Js.entries(Objects.requireNonNull(s.getData()).get("packages"))) {
            String location = entry.getKey();
            if (location.isEmpty()) {
                continue;
            }
            Object meta = entry.getValue();
            if (truthy(Js.get(meta, "link"))) {
                links.put(location, meta);
            } else {
                nodes.put(location, loadNode(location, meta, false));
            }
        }
        resolveLinks(links, nodes);
        if (!options.getRelease().placesLockNodesByRoot()) {
            assignParentage(nodes);
        }
        if (!options.getRelease().assignsBundlesOfOldLocksOnly()) {
            assignEveryBundle(nodes);
        } else if (!(Js.toNumber(s.getOriginalLockfileVersion()) >= 2)) {
            assignBundles(nodes);
        }
        if (flagsSuspect && !suspectsProvidedRoot) {
            // [reCalcDepFlags] before npm 7.5.4: every node of the tree, the root's flags calculated again too
            for (Node node : Objects.requireNonNull(virtualTree).getInventory().values()) {
                node.resetDepFlags();
            }
            DepFlags.calcDepFlags(virtualTree, true);
        } else if (flagsSuspect) {
            for (Node node : nodes.values()) {
                if (node.isRoot() || node == rootOptionProvided) {
                    continue;
                }
                node.resetDepFlags();
            }
            DepFlags.calcDepFlags(virtualTree, rootOptionProvided == null);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkRootEdges(Shrinkwrap s, Node root) {
        if (!s.isLoadedFromDisk() || s.isAncientLockfile()) {
            return;
        }

        Map<String, Object> lock = s.get("");
        Object prod = or(lock.get("dependencies"));
        Object dev = or(lock.get("devDependencies"));
        Object optional = or(lock.get("optionalDependencies"));
        Object peer = or(lock.get("peerDependencies"));
        Map<String, Object> peerOptional = new LinkedHashMap<>();

        if (truthy(lock.get("peerDependenciesMeta"))) {
            for (Map.Entry<String, Object> entry : Js.entries(lock.get("peerDependenciesMeta"))) {
                String name = entry.getKey();
                if (truthy(Js.get(entry.getValue(), "optional")) && Js.get(peer, name) != Js.UNDEFINED) {
                    peerOptional.put(name, Js.get(peer, name));
                    Js.delete(peer, name);
                }
            }
        }

        for (String name : Js.keys(optional)) {
            Js.delete(prod, name);
        }

        Map<String, Object> lockWS = new LinkedHashMap<>();
        Map<String, String> workspaces = MapWorkspaces.virtual(path, Objects.requireNonNull(s.getData()),
                options.getRelease());
        for (Map.Entry<String, String> ws : workspaces.entrySet()) {
            lockWS.put(ws.getKey(), "file:" + ConsistentResolve.escapeHash(ws.getValue(), options.getRelease()));
        }

        Set<String> rootNames = new LinkedHashSet<>(root.getEdgesOut().keys());

        Map<Edge.Type, Object> lockByType = new LinkedHashMap<>();
        lockByType.put(Edge.Type.DEV, dev);
        lockByType.put(Edge.Type.OPTIONAL, optional);
        lockByType.put(Edge.Type.PEER, peer);
        lockByType.put(Edge.Type.PEER_OPTIONAL, peerOptional);
        lockByType.put(Edge.Type.PROD, prod);
        lockByType.put(Edge.Type.WORKSPACE, lockWS);

        for (Map.Entry<Edge.Type, Object> byType : lockByType.entrySet()) {
            for (Map.Entry<String, Object> dep : Js.entries(byType.getValue())) {
                String name = dep.getKey();
                Edge edge = root.getEdgesOut().get(name);
                if (edge == null || edge.getType() != byType.getKey() ||
                    !Js.strictEquals(edge.getSpec(), dep.getValue())) {
                    flagsSuspect = true;
                    return;
                }
                rootNames.remove(name);
            }
        }
        if (!rootNames.isEmpty()) {
            flagsSuspect = true;
        }
    }

    // deps || {}
    private static Object or(@Nullable Object deps) {
        return truthy(deps) ? deps : new LinkedHashMap<>();
    }

    private void resolveLinks(Map<String, Object> links, Map<String, Node> nodes) {
        for (Map.Entry<String, Object> entry : links.entrySet()) {
            String location = entry.getKey();
            Object meta = entry.getValue();
            Object resolved = Js.get(meta, "resolved");
            if (!(resolved instanceof String)) {
                throw new IllegalArgumentException(
                        "The \"paths[1]\" argument must be of type string. Received " + Js.received(resolved));
            }
            String targetPath = PosixPath.resolve(path, (String) resolved);
            String targetLoc = Relpath.relpath(path, targetPath);
            Node target = nodes.get(targetLoc);

            if (target == null) {
                if (!options.getRelease().rejectsLinkWithoutTarget()) {
                    // #loadLink reads the flags of the target it was not given
                    throw new IllegalArgumentException("Cannot read properties of undefined (reading 'extraneous')");
                }
                throw new MissingTargetException(targetLoc, location);
            }

            Link link = loadLink(location, targetLoc, target);
            nodes.put(location, link);
            nodes.put(targetLoc, Objects.requireNonNull(link.getTarget()));

            // before npm 7.0.9 only a lock older than version 2 has its link targets' package.json read; before 7.1.1
            // a target in node_modules has it read too
            Shrinkwrap lock = Objects.requireNonNull(Objects.requireNonNull(virtualTree).getMeta());
            boolean readsTarget = options.getRelease().readsLinkTargetPackages() ||
                                  lock.isLoadedFromDisk() && !(Js.toNumber(lock.getOriginalLockfileVersion()) >= 2);
            boolean outsideNodeModules = link.getTarget().getParent() == null ||
                                         !options.getRelease().readsPackagesOfTopLinkTargetsOnly();
            if (outsideNodeModules && readsTarget) {
                if (!options.getRelease().guardsLinkTargetPackage()) {
                    // rpj(pj).catch(() => null): only the read is guarded, and assigning the package may throw
                    PackageJson manifest = null;
                    try {
                        manifest = files.manifest(targetLoc);
                    } catch (PackageJson.NeedsPackageFiles e) {
                        throw e;
                    } catch (RuntimeException ignored) {
                    }
                    if (manifest != null) {
                        link.getTarget().setPackage(manifest.getContent());
                    }
                    continue;
                }
                try {
                    PackageJson manifest = files.manifest(targetLoc);
                    if (manifest != null) {
                        link.getTarget().setPackage(manifest.getContent());
                    }
                } catch (PackageJson.NeedsPackageFiles | CannotKnow e) {
                    throw e;
                } catch (RuntimeException ignored) {
                    // arborist's .catch(() => null): the package stays assigned, its edges loaded up to the failure
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void assignBundles(Map<String, Node> nodes) {
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String location = entry.getKey();
            Node node = entry.getValue();
            if (location.isEmpty() ||
                node.isLink() && !truthy(Objects.requireNonNull(node.getTarget()).getLocation())) {
                continue;
            }
            Node parent = node.getParent();
            if (parent == null) {
                continue;
            }
            Map<String, Object> ppkg = parent.getPackage();
            if (truthy(node.getPackage().get("inBundle")) && !truthy(ppkg.get("inBundle")) &&
                parent.getEdgesOut().has(node.getName())) {
                Object bundleDependencies = ppkg.get("bundleDependencies");
                if (!truthy(bundleDependencies)) {
                    List<Object> list = new ArrayList<>();
                    list.add(node.getName());
                    ppkg.put("bundleDependencies", list);
                } else {
                    ((List<Object>) bundleDependencies).add(node.getName());
                }
            }
        }
    }

    // [assignParentage] of arborist 1 (npm 7.1.0), whose nodes and links are made with no parent: each takes as
    // parent, or else fsParent, the first node the lock holds walking up its path. The bundles it also assigns, for a
    // child its parent depends on, are assignEveryBundle's, which reads each node's parent once all are placed.
    private void assignParentage(Map<String, Node> nodes) {
        boolean skipsRoot = options.getRelease().skipsRootParentage();
        for (Map.Entry<String, Node> entry : new ArrayList<>(nodes.entrySet())) {
            String location = entry.getKey();
            Node node = entry.getValue();
            if (location.isEmpty() && skipsRoot) {
                continue;
            }
            String name = Js.string(node.getName());
            for (String p : PosixPath.walkUp(PosixPath.dirname(Js.string(node.getPath())))) {
                String ploc = Relpath.relpath(path, p);
                Node parent = nodes.get(ploc);
                if (parent == null || parent == node && skipsRoot) {
                    continue;
                }
                String locTest = (ploc + "/node_modules/" + name).replaceFirst("^/", "");
                if (location.equals(locTest)) {
                    node.setParent(parent);
                } else {
                    node.setFsParent(parent);
                }
                break;
            }
        }
    }

    // [assignBundles] before npm 7.11.0: for a lock of any version, and every flagged child, once; before 7.1.1
    // ([assignParentage] of arborist 1), only a child its parent depends on
    @SuppressWarnings("unchecked")
    private void assignEveryBundle(Map<String, Node> nodes) {
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String location = entry.getKey();
            Node node = entry.getValue();
            if (location.isEmpty()) {
                continue;
            }
            Node parent = node.getParent();
            if (parent == null) {
                continue;
            }
            Map<String, Object> ppkg = parent.getPackage();
            if (truthy(node.getPackage().get("inBundle")) && !truthy(ppkg.get("inBundle")) &&
                (options.getRelease().bundlesChildrenWithoutEdges() || parent.getEdgesOut().has(node.getName()))) {
                Object bundleDependencies = ppkg.get("bundleDependencies");
                if (!truthy(bundleDependencies)) {
                    List<Object> list = new ArrayList<>();
                    list.add(node.getName());
                    ppkg.put("bundleDependencies", list);
                } else if (!((List<Object>) bundleDependencies).contains(node.getName())) {
                    ((List<Object>) bundleDependencies).add(node.getName());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Node loadNode(String location, Object meta, boolean loadOverrides) {
        String p = virtualTree != null ? virtualTree.getRealpath() : path;
        String nodePath = PosixPath.resolve(p, location);
        if (!(meta instanceof Map) && !(meta instanceof List)) {
            String type = meta instanceof Number ? "number" : meta instanceof Boolean ? "boolean" : "string";
            Object value = meta instanceof Number ? Js.numberToString((Number) meta) : meta;
            throw new IllegalArgumentException("Cannot create property 'name' on " + type + " '" + value + "'");
        }
        // an array has none of the properties read below, so it loads as an empty package, as in arborist
        Map<String, Object> sw = meta instanceof Map ? (Map<String, Object>) meta : new LinkedHashMap<>();
        if (!truthy(sw.get("name"))) {
            sw.put("name", Node.nameFromFolder(nodePath));
        }
        NpmRelease release = options.getRelease();
        // npm 12.0.0 reads an entry's patch and manifest-extension records, and no hasShrinkwrap
        boolean records = (truthy(sw.get("patched")) || truthy(sw.get("packageExtensionsApplied")) ||
                           truthy(sw.get("npmExtensionApplied"))) && release.recordsPatchesAndExtensions();
        boolean hasShrinkwrap = truthy(sw.get("hasShrinkwrap")) && release.readsNpmShrinkwrap();
        return new Node(Node.options()
                .release(options.getRelease())
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .root(virtualTree)
                .path(nodePath)
                .realpath(nodePath)
                .integrity(sw.get("integrity"))
                .patched(records ? sw.get("patched") : null)
                .packageExtensionsApplied(records ? sw.get("packageExtensionsApplied") : null)
                .npmExtensionApplied(records ? sw.get("npmExtensionApplied") : null)
                .resolved(ConsistentResolve.consistentResolve(sw.get("resolved"), path, nodePath, false, release))
                .pkg(sw)
                .hasShrinkwrap(hasShrinkwrap ? sw.get("hasShrinkwrap") : null)
                .loadOverrides(loadOverrides)
                .extraneous(truthy(sw.get("extraneous")))
                .devOptional(truthy(sw.get("devOptional")) || truthy(sw.get("dev")) || truthy(sw.get("optional")))
                .peer(truthy(sw.get("peer")))
                .optional(truthy(sw.get("optional")))
                .dev(truthy(sw.get("dev")))
                .build());
    }

    private Link loadLink(String location, String targetLoc, Node target) {
        String linkPath = PosixPath.resolve(path, location);
        Link link = new Link(Node.options()
                .release(options.getRelease())
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .path(linkPath)
                .realpath(PosixPath.resolve(path, targetLoc))
                .target(target)
                .pkg(target.getPackage())
                .build());
        link.setExtraneous(target.isExtraneous());
        link.setDevOptional(target.isDevOptional());
        link.setPeer(target.isPeer());
        link.setOptional(target.isOptional());
        link.setDev(target.isDev());
        return link;
    }
}
