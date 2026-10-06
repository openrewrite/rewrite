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
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
     * What a lock-only install reads from the project: the lock, a yarn.lock beside it, and the
     * package.json of the root ({@code ""}) and of every directory a link resolves to, by location.
     */
    @Value
    @Builder
    public static class ProjectFiles {
        @Nullable
        String lock;

        @Nullable
        String yarnLock;

        @Singular
        Map<String, String> manifests;
    }

    /**
     * {@code ENOLOCK}: no lock to load a tree from.
     */
    public static final class NoLockException extends IllegalStateException {
        NoLockException() {
            super("loadVirtual requires existing shrinkwrap file");
        }
    }

    /**
     * {@code EMISSINGTARGET}: a link whose target the lock does not hold.
     */
    public static final class MissingTargetException extends IllegalStateException {
        MissingTargetException(String targetLoc, String location) {
            super("Missing target in lock file: \"" + targetLoc + "\" is referenced by \"" + location +
                  "\" but does not exist.\nTo fix:\n1. rm package-lock.json\n2. npm install");
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
    private Map<String, String> manifests = new LinkedHashMap<>();

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
        manifests = files.getManifests();

        if (root != null && root.getMeta() != null) {
            loadFromShrinkwrap(root.getMeta(), root);
            return Objects.requireNonNull(virtualTree);
        }

        Shrinkwrap s = new Shrinkwrap(path, options).load(files.getLock(), files.getYarnLock(), manifests.get(""));
        if (!s.isLoadedFromDisk() && root == null) {
            throw new NoLockException();
        }

        Object pkg = rootPackage(s);
        rootOptionProvided = root;
        if (root == null) {
            root = setWorkspaces(loadNode("", pkg, true), manifests);
        }

        loadFromShrinkwrap(s, root);
        root.assertRootOverrides();
        return Objects.requireNonNull(virtualTree);
    }

    // PackageJson.normalize(this.path).then(p => p.content).catch(() => s.data.packages[''] || {})
    private Object rootPackage(Shrinkwrap s) {
        String text = manifests.get("");
        if (text != null) {
            try {
                return PackageJson.normalize(text).getContent();
            } catch (PackageJson.NeedsPackageFiles e) {
                throw e;
            } catch (RuntimeException ignored) {
            }
        }
        Object fromLock = Js.get(Objects.requireNonNull(s.getData()).get("packages"), "");
        return truthy(fromLock) ? fromLock : new LinkedHashMap<>();
    }

    /**
     * Arborist's {@code setWorkspaces}: the members the node's {@code workspaces} patterns name among the
     * project's manifests become its workspace edges.
     */
    static Node setWorkspaces(Node node, Map<String, String> manifests) {
        Map<String, String> workspaces = MapWorkspaces.mapWorkspaces(node.getPath(), node.getPackage(), manifests);
        if (!workspaces.isEmpty()) {
            node.setWorkspaces(workspaces);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private void loadFromShrinkwrap(Shrinkwrap s, Node root) {
        if (rootOptionProvided == null) {
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
        if (!(Js.toNumber(s.getOriginalLockfileVersion()) >= 2)) {
            assignBundles(nodes);
        }
        if (flagsSuspect) {
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
        Map<String, Object> prod = depMap(lock.get("dependencies"));
        Map<String, Object> dev = depMap(lock.get("devDependencies"));
        Map<String, Object> optional = depMap(lock.get("optionalDependencies"));
        Map<String, Object> peer = depMap(lock.get("peerDependencies"));
        Map<String, Object> peerOptional = new LinkedHashMap<>();

        if (truthy(lock.get("peerDependenciesMeta"))) {
            for (Map.Entry<String, Object> entry : Js.entries(lock.get("peerDependenciesMeta"))) {
                String name = entry.getKey();
                if (truthy(Js.get(entry.getValue(), "optional")) && Js.get(peer, name) != Js.UNDEFINED) {
                    peerOptional.put(name, peer.get(name));
                    peer.remove(name);
                }
            }
        }

        for (String name : Js.keys(optional)) {
            prod.remove(name);
        }

        Map<String, Object> lockWS = new LinkedHashMap<>();
        Map<String, String> workspaces = MapWorkspaces.virtual(path, Objects.requireNonNull(s.getData()));
        for (Map.Entry<String, String> ws : workspaces.entrySet()) {
            lockWS.put(ws.getKey(), "file:" + ws.getValue());
        }

        Set<String> rootNames = new LinkedHashSet<>(root.getEdgesOut().keys());

        Map<Edge.Type, Map<String, Object>> lockByType = new LinkedHashMap<>();
        lockByType.put(Edge.Type.DEV, dev);
        lockByType.put(Edge.Type.OPTIONAL, optional);
        lockByType.put(Edge.Type.PEER, peer);
        lockByType.put(Edge.Type.PEER_OPTIONAL, peerOptional);
        lockByType.put(Edge.Type.PROD, prod);
        lockByType.put(Edge.Type.WORKSPACE, lockWS);

        for (Map.Entry<Edge.Type, Map<String, Object>> byType : lockByType.entrySet()) {
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> depMap(@Nullable Object deps) {
        return truthy(deps) && deps instanceof Map ? (Map<String, Object>) deps : new LinkedHashMap<>();
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
                throw new MissingTargetException(targetLoc, location);
            }

            Link link = loadLink(location, targetLoc, target);
            nodes.put(location, link);
            nodes.put(targetLoc, Objects.requireNonNull(link.getTarget()));

            if (link.getTarget().getParent() == null) {
                String text = manifests.get(targetLoc);
                if (text != null) {
                    try {
                        link.getTarget().setPackage(PackageJson.normalize(text).getContent());
                    } catch (PackageJson.NeedsPackageFiles e) {
                        throw e;
                    } catch (RuntimeException ignored) {
                        // arborist's .catch(() => null): the package stays assigned, its edges loaded up to the failure
                    }
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
        return new Node(Node.options()
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .root(virtualTree)
                .path(nodePath)
                .realpath(nodePath)
                .integrity(sw.get("integrity"))
                .resolved(ConsistentResolve.consistentResolve(sw.get("resolved"), path, nodePath, false))
                .pkg(sw)
                .hasShrinkwrap(sw.get("hasShrinkwrap"))
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
