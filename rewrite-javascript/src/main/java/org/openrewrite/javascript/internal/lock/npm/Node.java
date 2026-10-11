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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;
import static org.openrewrite.javascript.semver.NodeSemver.Options.DEFAULT;

/**
 * Port of @npmcli/arborist lib/node.js (npm 11.17.0): a package at a location in the tree, with
 * its edges out to dependencies and in from dependents.
 */
public class Node {

    private static final Pattern NAME_FROM_NODE_MODULES =
            Pattern.compile("(?:^|/|\\\\)node_modules[\\\\/](@[^/\\\\]+[\\\\/][^\\\\/]+|[^\\\\/]+)$");

    /**
     * Arborist's constructor options; {@code children} and {@code fsChildren} declare a tree.
     */
    @Value
    @Builder(toBuilder = true)
    public static class Options {
        @Singular
        List<Options> children;

        @Builder.Default
        boolean dev = true;

        @Builder.Default
        boolean devOptional = true;

        boolean dummy;

        @Nullable
        Throwable error;

        @Builder.Default
        boolean extraneous = true;

        @Singular
        List<Options> fsChildren;

        @Nullable
        Node fsParent;

        @Nullable
        Object hasShrinkwrap;

        boolean inert;

        boolean installLinks;

        @Nullable
        Object integrity;

        boolean legacyPeerDeps;

        @Singular("linkIn")
        List<Node> linksIn;

        boolean loadOverrides;

        @Nullable
        Shrinkwrap meta;

        @Nullable
        String name;

        @Builder.Default
        boolean optional = true;

        @Nullable
        OverrideSet overrides;

        @Nullable
        Node parent;

        @Nullable
        String path;

        @Builder.Default
        boolean peer = true;

        /**
         * The package.json content, a mutable map the node normalizes in place and keeps as its package;
         * anything other than a map is an empty package.
         */
        @Nullable
        Object pkg;

        @Nullable
        String realpath;

        @Nullable
        Object resolved;

        @Nullable
        Node root;

        @Nullable
        Node sourceReference;

        /**
         * A link's target; ignored by a plain node.
         */
        @Nullable
        Node target;

        // declared so that javadoc, which reads the source before Lombok, finds it
        public static class OptionsBuilder {
        }
    }

    public static Options.OptionsBuilder options() {
        return Options.builder();
    }

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private @Nullable String name;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private @Nullable String path;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private @Nullable String realpath;

    // resolved, integrity and hasShrinkwrap hold the lock's values as read, whatever JSON type they have
    private @Nullable Object resolved;

    @Getter
    @Setter
    private @Nullable Object integrity;

    @Getter
    @Setter
    private @Nullable Object hasShrinkwrap;

    @Getter
    @Setter
    private boolean installLinks;

    @Getter
    @Setter
    private boolean legacyPeerDeps;

    private final CaseInsensitiveMap<Node> children = new CaseInsensitiveMap<>();

    @Getter
    private final Set<Node> fsChildren = new LinkedHashSet<>();

    @Getter
    private final Inventory inventory = new Inventory();

    @Getter
    private final Set<Node> tops = new LinkedHashSet<>();

    @Getter
    private final Set<Node> linksIn = new LinkedHashSet<>();

    @Getter
    @Setter
    private boolean dev;

    @Getter
    @Setter
    private boolean optional;

    @Getter
    @Setter
    private boolean devOptional;

    @Getter
    @Setter
    private boolean peer;

    @Getter
    @Setter
    private boolean extraneous;

    @Getter
    private final boolean dummy;

    @Getter
    @Setter
    private boolean inert;

    @Getter
    private final Set<Edge> edgesIn = new LinkedHashSet<>();

    @Getter
    private final CaseInsensitiveMap<Edge> edgesOut = new CaseInsensitiveMap<>();

    @Getter
    @Setter
    private @Nullable OverrideSet overrides;

    @Getter
    private @Nullable String location;

    @Getter
    @Setter
    private List<Throwable> errors;

    @Getter
    private final @Nullable Node sourceReference;

    Map<String, Object> pkg;
    private @Nullable Node parent;
    private @Nullable Node fsParent;
    private @Nullable Node root;
    private @Nullable Map<String, String> workspaces;
    private @Nullable Map<String, Object> explanation;
    private @Nullable Shrinkwrap meta;

    // a link's target, read through getTarget(); the root setter rewires other links' targets directly
    @Nullable
    Node target;

    @SuppressWarnings("unchecked")
    public Node(Options options) {
        this.errors = new ArrayList<>();
        if (options.getError() != null) {
            this.errors.add(options.getError());
        }

        this.sourceReference = options.getSourceReference();
        if (sourceReference != null) {
            this.pkg = sourceReference.getPackage();
        } else {
            Map<String, Object> content = options.getPkg() instanceof Map ?
                    (Map<String, Object>) options.getPkg() : new LinkedHashMap<>();
            this.pkg = PackageJson.syncNormalize(content);
        }

        String path = options.getPath();
        String realpath = options.getRealpath();
        Object nameSource = truthy(path) ? path : truthy(pkg.get("name")) ? pkg.get("name") : realpath;
        String fromFolder = truthy(options.getName()) ? null : nameFromFolder(nameSource);
        this.name = truthy(options.getName()) ? options.getName() :
                truthy(fromFolder) ? fromFolder :
                        truthy(pkg.get("name")) ? String.valueOf(pkg.get("name")) : null;

        this.path = truthy(path) ? PosixPath.resolve(path) : null;

        if (!truthy(this.name) && (this.path == null || !this.path.equals(PosixPath.dirname(this.path)))) {
            throw new IllegalArgumentException("could not detect node name from path or package");
        }

        this.realpath = !isLink() ? this.path : PosixPath.resolve(realpath);

        setResolved(truthy(options.getResolved()) ? options.getResolved() : null);
        if (!truthy(getResolved())) {
            Object resolved = ConsistentResolve.consistentResolve(pkg.get("_resolved"));
            if (truthy(resolved) && !(Js.string(resolved).startsWith("file:") && truthy(pkg.get("_where")))) {
                setResolved(resolved);
            }
        }
        Object legacyIntegrity = pkg.get("_integrity");
        this.integrity = truthy(options.getIntegrity()) ? options.getIntegrity() :
                truthy(legacyIntegrity) ? legacyIntegrity : null;
        Object legacyHasShrinkwrap = pkg.get("_hasShrinkwrap");
        this.hasShrinkwrap = truthy(options.getHasShrinkwrap()) ? options.getHasShrinkwrap() :
                truthy(legacyHasShrinkwrap) ? legacyHasShrinkwrap : false;
        this.installLinks = options.isInstallLinks();
        this.legacyPeerDeps = options.isLegacyPeerDeps();

        this.linksIn.addAll(options.getLinksIn());

        if (!options.isDummy()) {
            this.dev = options.isDev();
            this.optional = options.isOptional();
            this.devOptional = options.isDevOptional();
            this.peer = options.isPeer();
            this.extraneous = options.isExtraneous();
            this.dummy = false;
        } else {
            this.dummy = true;
        }

        this.inert = options.isInert();

        if (options.getOverrides() != null) {
            this.overrides = options.getOverrides();
        } else if (options.isLoadOverrides()) {
            Object overrides = pkg.get("overrides");
            if (truthy(overrides) && !Js.keys(overrides).isEmpty()) {
                this.overrides = new OverrideSet(overrides);
            }
        }

        setMeta(options.getMeta());

        setParent(options.getParent());

        this.fsParent = null;
        setFsParent(options.getFsParent());

        if (options.getParent() == null && options.getFsParent() == null) {
            setRoot(options.getRoot());
        }

        for (Options c : options.getChildren()) {
            new Node(c.toBuilder().parent(this).build());
        }
        for (Options c : options.getFsChildren()) {
            new Node(c.toBuilder().fsParent(this).build());
        }

        loadDeps();
    }

    static @Nullable String nameFromFolder(@Nullable Object dir) {
        if (!truthy(dir)) {
            return null;
        }
        if (!(dir instanceof String)) {
            throw new IllegalArgumentException(
                    "The \"path\" argument must be of type string. Received " + Js.received(dir));
        }
        String parent = PosixPath.basename(PosixPath.dirname((String) dir));
        String base = PosixPath.basename((String) dir);
        return parent.startsWith("@") ? parent + "/" + base : base;
    }

    public @Nullable Shrinkwrap getMeta() {
        return meta;
    }

    public void setMeta(@Nullable Shrinkwrap meta) {
        this.meta = meta;
        if (meta != null) {
            meta.add(this);
        }
    }

    public @Nullable Map<String, String> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(Map<String, String> workspaces) {
        if (this.workspaces != null) {
            for (String name : this.workspaces.keySet()) {
                if (!workspaces.containsKey(name)) {
                    edgesOut.get(name).detach();
                }
            }
        }
        this.workspaces = workspaces;
        loadWorkspaces();
        loadDeps();
    }

    public boolean hasInstallScript() {
        Object scripts = pkg.get("scripts");
        Map<?, ?> s = scripts instanceof Map ? (Map<?, ?>) scripts : Collections.emptyMap();
        return truthy(pkg.get("hasInstallScript")) || truthy(s.get("install")) ||
               truthy(s.get("preinstall")) || truthy(s.get("postinstall"));
    }

    /**
     * @return the package's {@code version} as the data holds it, a string unless the manifest is wrong.
     */
    public Object getVersion() {
        Object version = pkg.get("version");
        return truthy(version) ? version : "";
    }

    /**
     * @return the package's {@code name} as the data holds it, a string unless the manifest is wrong.
     */
    public @Nullable Object getPackageName() {
        Object name = pkg.get("name");
        return truthy(name) ? name : null;
    }

    public String getPkgid() {
        Object name = Js.get(pkg, "name");
        Object version = Js.get(pkg, "version");
        Object pkgName = name == Js.UNDEFINED ? "" : name;
        Object pkgVersion = version == Js.UNDEFINED ? "" : version;
        boolean isProjectRoot = isProjectRoot();
        Object myname = isProjectRoot ? (truthy(pkgName) ? pkgName : this.name) : this.name;
        String alias = !isProjectRoot && truthy(pkgName) && !Js.strictEquals(myname, pkgName) ?
                "npm:" + Js.string(pkgName) + "@" : "";
        return Js.string(myname) + "@" + alias + Js.string(pkgVersion);
    }

    public boolean isOverridden() {
        if (overrides == null) {
            return false;
        }
        if (!truthy(overrides.getValue())) {
            return false;
        }
        if (!Objects.equals(overrides.getName(), name)) {
            return false;
        }
        for (Edge edge : edgesIn) {
            if (edge.getOverrides() != null && Objects.equals(edge.getOverrides().getName(), name) &&
                Js.strictEquals(edge.getOverrides().getValue(), getVersion())) {
                if (!edge.getOverrides().isEqual(edge.getFrom().getOverrides())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, Object> getPackage() {
        return pkg;
    }

    public void setPackage(@Nullable Object pkg) {
        for (Edge edge : edgesOut.values()) {
            edge.detach();
        }
        explanation = null;
        this.pkg = pkg instanceof Map ? (Map<String, Object>) pkg : new LinkedHashMap<>();
        loadWorkspaces();
        loadDeps();
        for (Edge edge : new ArrayList<>(edgesIn)) {
            edge.reload(true);
        }
    }

    public Map<String, Object> explain() {
        return explain(null, Collections.emptyList());
    }

    public Map<String, Object> explain(@Nullable Edge edge) {
        return explain(edge, Collections.emptyList());
    }

    public Map<String, Object> explain(@Nullable Edge edge, List<Node> seen) {
        if (explanation != null) {
            return explanation;
        }
        return explanation = doExplain(edge, seen);
    }

    Map<String, Object> doExplain(@Nullable Edge edge, List<Node> seen) {
        Map<String, Object> why = new LinkedHashMap<>();
        if (isProjectRoot() && sourceReference == null) {
            why.put("location", path);
            return why;
        }

        why.put("name", isProjectRoot() || isTop() ? getPackageName() : name);
        why.put("version", pkg.get("version"));
        if (!errors.isEmpty() || !truthy(getPackageName()) || !truthy(pkg.get("version"))) {
            why.put("errors", !errors.isEmpty() ? errors :
                    Collections.singletonList(new IllegalStateException("invalid package: lacks name and/or version")));
            why.put("package", pkg);
        }

        if (getRoot().sourceReference != null) {
            Map<String, Object> whileInstalling = new LinkedHashMap<>();
            whileInstalling.put("name", getRoot().getPackage().get("name"));
            whileInstalling.put("version", getRoot().getPackage().get("version"));
            whileInstalling.put("path", getRoot().sourceReference.getPath());
            why.put("whileInstalling", whileInstalling);
        }

        if (sourceReference != null) {
            return sourceReference.explain(edge, seen);
        }

        if (seen.contains(this)) {
            return why;
        }

        why.put("location", location);
        why.put("isWorkspace", isWorkspace());

        List<Node> seenWithThis = new ArrayList<>(seen);
        seenWithThis.add(this);

        List<Map<String, Object>> dependents = new ArrayList<>();
        why.put("dependents", dependents);
        if (edge != null) {
            dependents.add(edge.explain(seenWithThis));
        } else {
            List<Edge> edges = new ArrayList<>();
            for (Edge e : edgesIn) {
                if (!e.isValid() && !e.getFrom().isProjectRoot()) {
                    continue;
                }
                edges.add(e);
            }
            for (Edge e : edges) {
                dependents.add(e.explain(seenWithThis));
            }
        }

        if (!linksIn.isEmpty()) {
            List<Map<String, Object>> links = new ArrayList<>();
            for (Node link : linksIn) {
                links.add(link.doExplain(edge, seenWithThis));
            }
            why.put("linksIn", links);
        }
        return why;
    }

    public boolean isDescendantOf(Node node) {
        for (Node p = this; p != null; p = p.getResolveParent()) {
            if (p == node) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldOmit(Set<String> omitSet) {
        if (omitSet.isEmpty()) {
            return false;
        }
        Node top = getTop();
        if (!top.isProjectRoot() && !top.isWorkspace()) {
            return false;
        }
        return (peer && omitSet.contains("peer")) ||
               (dev && omitSet.contains("dev")) ||
               (optional && omitSet.contains("optional")) ||
               (devOptional && omitSet.contains("optional") && omitSet.contains("dev"));
    }

    public @Nullable Node getBundler() {
        return getBundler(new ArrayList<>());
    }

    @Nullable
    Node getBundler(List<Node> path) {
        if (path.contains(this)) {
            return null;
        }
        path.add(this);

        Node parent = this.parent;
        if (parent == null) {
            return null;
        }

        Node pBundler = parent.getBundler(path);
        if (pBundler != null) {
            return pBundler;
        }

        Object bd = parent.getPackage().get("bundleDependencies");
        if (bd instanceof List && ((List<?>) bd).contains(name)) {
            return parent;
        }

        for (Edge edge : edgesIn) {
            Node eBundler = edge.getFrom().getBundler(path);
            if (eBundler == null) {
                continue;
            }
            if (eBundler == parent) {
                return eBundler;
            }
        }
        return null;
    }

    public boolean isInBundle() {
        return getBundler() != null;
    }

    public boolean isInDepBundle() {
        Node bundler = getBundler();
        return bundler != null && bundler != getRoot();
    }

    public boolean isWorkspace() {
        if (isProjectRoot()) {
            return false;
        }
        Edge edge = getRoot().getEdgesOut().get(getPackageName());
        if (edge == null) {
            return false;
        }
        Node to = edge.getTo();
        return edge.getType() == Edge.Type.WORKSPACE && to != null && (to.getTarget() == this || to == this);
    }

    public boolean isRoot() {
        return this == getRoot();
    }

    public boolean isProjectRoot() {
        return this == getRoot() || this == getRoot().getTarget();
    }

    public boolean isRegistryDependency() {
        if (edgesIn.isEmpty()) {
            return false;
        }
        for (Edge edge : edgesIn) {
            if (!Spec.npa(edge.getSpec()).isRegistry()) {
                return false;
            }
        }
        return true;
    }

    /**
     * This node and its resolve parents, walked as the iteration proceeds.
     */
    public Iterable<Node> ancestry() {
        return () -> new Iterator<Node>() {
            private @Nullable Node next = Node.this;

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Node next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                Node current = next;
                next = current.getResolveParent();
                return current;
            }
        };
    }

    public void setRoot(@Nullable Node root) {
        while (root != null && root.getRoot() != root) {
            root = root.getRoot();
        }
        if (root == null) {
            root = this;
        }

        delistFromMeta();

        if (path == null || root.realpath == null || root.path == null) {
            this.root = root;
            return;
        }

        this.root = this;

        for (Node link : new ArrayList<>(linksIn)) {
            link.target = null;
            linksIn.remove(link);
        }

        Node target = getTarget();
        if (isLink()) {
            if (target != null) {
                target.linksIn.remove(this);
                if (target.getRoot() == this) {
                    target.delistFromMeta();
                }
            }
            this.target = null;
        }

        if (parent != null && parent.getRoot() != root) {
            parent.getChildren().delete(name);
            this.parent = null;
        }
        if (fsParent != null && fsParent.getRoot() != root) {
            fsParent.fsChildren.remove(this);
            this.fsParent = null;
        }

        if (root == this) {
            refreshLocation();
        } else {
            String loc = Relpath.relpath(root.realpath, path);
            Node current = root.inventory.get(loc);
            if (current != null) {
                current.setRoot(null);
            }

            this.root = root;
            refreshLocation();

            for (String p : PosixPath.walkUp(PosixPath.dirname(path))) {
                if (p.equals(path)) {
                    continue;
                }
                String ploc = Relpath.relpath(root.realpath, p);
                Node parent = root.inventory.get(ploc);
                if (parent != null) {
                    if (parent.isLink()) {
                        continue;
                    }
                    String childLoc = ploc + (ploc.isEmpty() ? "" : "/") + "node_modules/" + name;
                    boolean isParent = childLoc.equals(location);
                    if (isParent) {
                        Node oldChild = parent.getChildren().get(name);
                        if (oldChild != null && oldChild != this) {
                            oldChild.setRoot(null);
                        }
                        if (this.parent != null) {
                            this.parent.getChildren().delete(name);
                            this.parent.reloadNamedEdges(name);
                        }
                        parent.getChildren().set(name, this);
                        this.parent = parent;
                        if (!isLink()) {
                            parent.reloadNamedEdges(name);
                        }
                    } else {
                        if (fsParent != null) {
                            fsParent.fsChildren.remove(this);
                        }
                        parent.fsChildren.add(this);
                        this.fsParent = parent;
                    }
                    break;
                }
            }

            if (parent == null) {
                root.tops.add(this);
            } else {
                root.tops.remove(this);
            }

            String nmloc = location + (truthy(location) ? "/" : "") + "node_modules/";
            for (Node child : new ArrayList<>(root.tops)) {
                if (!root.tops.contains(child)) {
                    continue;
                }
                boolean isChild = (nmloc + child.name).equals(child.location);
                boolean isFsChild =
                        PosixPath.dirname(child.path).startsWith(path) &&
                        child != this &&
                        child.parent == null &&
                        (child.fsParent == null ||
                         child.fsParent == this ||
                         PosixPath.dirname(path).startsWith(child.fsParent.path));

                if (!isChild && !isFsChild) {
                    continue;
                }

                if (isLink()) {
                    child.setRoot(null);
                } else {
                    if (child.fsParent != null) {
                        child.fsParent.fsChildren.remove(child);
                    }
                    child.fsParent = null;
                    if (isChild) {
                        getChildren().set(child.name, child);
                        child.parent = this;
                        root.tops.remove(child);
                    } else {
                        fsChildren.add(child);
                        child.fsParent = this;
                    }
                }
            }

            for (Node node : root.inventory.query("realpath", realpath)) {
                if (node == this) {
                    continue;
                }
                if (isLink()) {
                    Node nodeTarget = node.getTarget();
                    this.target = nodeTarget;
                    this.pkg = nodeTarget.pkg;
                    nodeTarget.linksIn.add(this);
                    if (parent != null) {
                        parent.reloadNamedEdges(name);
                    }
                    break;
                } else {
                    if (node.isLink()) {
                        node.target = this;
                        node.pkg = this.pkg;
                        linksIn.add(node);
                        if (node.parent != null) {
                            node.parent.reloadNamedEdges(node.name);
                        }
                    }
                }
            }
        }

        for (Edge edge : new ArrayList<>(edgesIn)) {
            if (edge.getFrom().getRoot() != root) {
                edge.reload();
            }
        }
        for (Edge edge : edgesOut.values()) {
            if (edge.getTo() == null || edge.getTo().getRoot() != root) {
                edge.reload();
            }
        }

        Set<Node> family = new LinkedHashSet<>();
        family.addAll(fsChildren);
        family.addAll(getChildren().values());
        family.addAll(inventory.values());
        family.remove(this);

        for (Node child : family) {
            if (child.getRoot() != root) {
                child.delistFromMeta();
                child.parent = null;
                getChildren().delete(child.name);
                child.fsParent = null;
                fsChildren.remove(child);
                for (Node l : new ArrayList<>(child.linksIn)) {
                    l.target = null;
                    child.linksIn.remove(l);
                }
            }
        }
        for (Node child : family) {
            if (child.getRoot() != root) {
                child.setRoot(root);
            }
        }

        if (isLink() && target != null && getTarget() == null && root != this) {
            target.setRoot(root);
        }
    }

    public Node getRoot() {
        return root != null ? root : this;
    }

    private void loadWorkspaces() {
        if (workspaces == null) {
            return;
        }
        for (Map.Entry<String, String> ws : workspaces.entrySet()) {
            new Edge(this, Edge.Type.WORKSPACE, ws.getKey(), "file:" + ws.getValue());
        }
    }

    void loadDeps() {
        Object pd = pkg.get("peerDependencies");
        Object ad = truthy(pkg.get("acceptDependencies")) ? pkg.get("acceptDependencies") : Collections.emptyMap();
        if ((pd instanceof Map || pd instanceof List) && !legacyPeerDeps) {
            Object pm = pkg.get("peerDependenciesMeta");
            if (!truthy(pm)) {
                pm = Collections.emptyMap();
            }
            Map<String, Object> peerDependencies = new LinkedHashMap<>();
            Map<String, Object> peerOptional = new LinkedHashMap<>();
            for (Map.Entry<String, Object> dep : Js.entries(pd)) {
                // assigning __proto__ on a plain object adds no property
                if ("__proto__".equals(dep.getKey())) {
                    continue;
                }
                Object meta = Js.get(pm, dep.getKey());
                if (meta != null && meta != Js.UNDEFINED && truthy(Js.get(meta, "optional"))) {
                    peerOptional.put(dep.getKey(), dep.getValue());
                } else {
                    peerDependencies.put(dep.getKey(), dep.getValue());
                }
            }
            loadDepType(peerDependencies, Edge.Type.PEER, ad);
            loadDepType(peerOptional, Edge.Type.PEER_OPTIONAL, ad);
        }

        loadDepType(pkg.get("dependencies"), Edge.Type.PROD, ad);
        loadDepType(pkg.get("optionalDependencies"), Edge.Type.OPTIONAL, ad);

        boolean thisDev = isTop() && path != null;
        boolean srcDev = sourceReference == null || (sourceReference.isTop() && sourceReference.getPath() != null);
        if (thisDev && srcDev) {
            loadDepType(pkg.get("devDependencies"), Edge.Type.DEV, ad);
        }
    }

    private void loadDepType(@Nullable Object deps, Edge.Type type, Object ad) {
        for (Map.Entry<String, Object> dep : Js.entries(truthy(deps) ? deps : Collections.emptyMap())) {
            Edge current = edgesOut.get(dep.getKey());
            if (current == null || current.getType() != Edge.Type.WORKSPACE) {
                if (!(dep.getValue() instanceof String)) {
                    throw new IllegalArgumentException("must provide string spec");
                }
                Object accept = Js.get(ad, dep.getKey());
                if (accept != Js.UNDEFINED && !(accept instanceof String)) {
                    throw new IllegalArgumentException("accept field must be a string if provided");
                }
                new Edge(this, type, dep.getKey(), (String) dep.getValue(),
                        accept == Js.UNDEFINED ? null : (String) accept, null);
            }
        }
    }

    public @Nullable Node getFsParent() {
        return fsParent;
    }

    public void setFsParent(@Nullable Node fsParent) {
        if (fsParent == null) {
            if (this.fsParent != null) {
                setRoot(null);
            }
            return;
        }

        if (fsParent.isLink()) {
            fsParent = fsParent.getTarget();
        }

        if (this == fsParent || Objects.equals(fsParent.realpath, realpath)) {
            return;
        }

        if (this.fsParent == fsParent) {
            return;
        }

        Node oldFsParent = this.fsParent;
        String newPath = oldFsParent == null ? path :
                PosixPath.resolve(fsParent.path, PosixPath.relative(oldFsParent.path, path));
        String nmPath = PosixPath.resolve(fsParent.path, "node_modules", name);

        if (Objects.equals(newPath, nmPath)) {
            setParent(fsParent);
            return;
        }

        boolean pathChange = !Objects.equals(newPath, path);

        Node oldParent = this.parent;
        String oldName = name;
        if (this.parent != null) {
            this.parent.getChildren().delete(name);
            this.parent = null;
        }
        if (this.fsParent != null) {
            this.fsParent.fsChildren.remove(this);
            this.fsParent = null;
        }

        if (pathChange) {
            changePath(newPath);
        }

        if (oldParent != null) {
            oldParent.reloadNamedEdges(oldName);
        }

        setRoot(fsParent.getRoot());
    }

    public boolean canReplaceWith(Node node) {
        return canReplaceWith(node, Collections.emptySet());
    }

    public boolean canReplaceWith(Node node, Collection<String> ignorePeers) {
        if (!Objects.equals(node.name, name)) {
            return false;
        }
        if (!Js.strictEquals(node.getPackageName(), getPackageName())) {
            return false;
        }

        if (edgesOut.size() > 0) {
            if (node.overrides != null) {
                if (!node.overrides.isEqual(overrides)) {
                    return false;
                }
            } else {
                if (overrides != null) {
                    return false;
                }
            }
        }

        Set<String> ignore = new LinkedHashSet<>(ignorePeers);
        Set<Node> depSet = GatherDepSet.gatherDepSet(Collections.singletonList(this),
                e -> e.getTo() != this && e.isValid());

        for (Edge edge : edgesIn) {
            if (!isTop() && edge.getFrom().parent == parent && edge.isPeer() && ignore.contains(edge.getFrom().name)) {
                continue;
            }
            if (!depSet.contains(edge.getFrom()) && !edge.satisfiedBy(node)) {
                return false;
            }
        }
        return true;
    }

    public boolean canReplace(Node node) {
        return node.canReplaceWith(this);
    }

    public boolean canReplace(Node node, Collection<String> ignorePeers) {
        return node.canReplaceWith(this, ignorePeers);
    }

    public boolean canDedupe() {
        return canDedupe(false, false);
    }

    public boolean canDedupe(boolean preferDedupe) {
        return canDedupe(preferDedupe, false);
    }

    public boolean canDedupe(boolean preferDedupe, boolean explicitRequest) {
        if (isInDepBundle() || isInShrinkwrap()) {
            return false;
        }

        if (getResolveParent() == null || getResolveParent().getResolveParent() == null) {
            return false;
        }

        if (edgesIn.isEmpty()) {
            return true;
        }

        Node other = getResolveParent().getResolveParent().resolve(name);

        if (other == null) {
            return false;
        }

        if (other.matches(this)) {
            return true;
        }

        if (!other.canReplace(this)) {
            return false;
        }

        if (preferDedupe || NodeSemver.compare(other.getVersion(), getVersion(), DEFAULT) == 0) {
            return true;
        }

        if (!isOverridden() && NodeSemver.compare(other.getVersion(), getVersion(), DEFAULT) > 0) {
            return true;
        }

        return explicitRequest;
    }

    public boolean satisfies(Edge requested) {
        return Objects.equals(name, requested.getName()) && requested.satisfiedBy(this);
    }

    public boolean satisfies(String requested) {
        Spec parsed = Spec.npa(requested);
        String name = parsed.getName() != null ? parsed.getName() : this.name;
        return Objects.equals(this.name, name) && satisfies(new Edge(
                new Node(options().path(getRoot().realpath).build()),
                Edge.Type.PROD, name, parsed.getRawSpec()));
    }

    public boolean matches(Node node) {
        if (node == this) {
            return true;
        }
        if (!Objects.equals(node.name, name)) {
            return false;
        }
        if (isLink()) {
            return node.isLink() && getTarget().matches(node.getTarget());
        }
        if (isProjectRoot() && node.isProjectRoot()) {
            return Objects.equals(path, node.path);
        }
        if (truthy(integrity) && truthy(node.integrity)) {
            return Js.strictEquals(integrity, node.integrity);
        }
        if (truthy(getResolved()) && truthy(node.getResolved())) {
            return Js.strictEquals(getResolved(), node.getResolved());
        }
        return truthy(getPackageName()) && truthy(node.getPackageName()) &&
               Js.strictEquals(getPackageName(), node.getPackageName()) &&
               truthy(getVersion()) && truthy(node.getVersion()) &&
               Js.strictEquals(getVersion(), node.getVersion());
    }

    public void replaceWith(Node node) {
        node.replace(this);
    }

    public void replace(Node node) {
        delistFromMeta();

        if (node.parent != null && node.parent.getChildren().get(name) == node) {
            path = PosixPath.resolve(node.parent.path, "node_modules", name);
        } else {
            path = node.path;
            name = node.name;
        }

        if (!isLink()) {
            realpath = path;
        }
        refreshLocation();

        if (!isLink()) {
            for (Node kid : node.getChildren().values()) {
                kid.setParent(this);
            }
            if (node.isLink() && node.getTarget() != null) {
                node.getTarget().setRoot(null);
            }
        }

        if (!node.isRoot()) {
            setRoot(node.getRoot());
        }
    }

    public boolean isInShrinkwrap() {
        return parent != null && (truthy(parent.hasShrinkwrap) || parent.isInShrinkwrap());
    }

    public @Nullable Node getParent() {
        return parent;
    }

    public void setParent(@Nullable Node parent) {
        if (parent == null) {
            if (this.parent != null) {
                setRoot(null);
            }
            return;
        }

        if (parent.isLink()) {
            parent = parent.getTarget();
        }

        if (this == parent) {
            return;
        }

        Node oldParent = this.parent;
        if (oldParent == parent) {
            return;
        }

        String newPath = PosixPath.resolve(parent.path, "node_modules", name);
        boolean pathChange = !newPath.equals(path);

        if (oldParent != null) {
            oldParent.getChildren().delete(name);
            this.parent = null;
        }
        if (fsParent != null) {
            fsParent.fsChildren.remove(this);
            this.fsParent = null;
        }

        if (pathChange) {
            changePath(newPath);
        }

        setRoot(parent.getRoot());
    }

    void delistFromMeta() {
        Node root = getRoot();
        if (root.realpath == null || path == null) {
            return;
        }
        root.inventory.delete(this);
        root.tops.remove(this);
        if (root.meta != null) {
            root.meta.delete(path);
        }
    }

    void changePath(String newPath) {
        delistFromMeta();
        String oldPath = path;
        path = newPath;
        Matcher nameChange = NAME_FROM_NODE_MODULES.matcher(newPath);
        if (nameChange.find() && !nameChange.group(1).equals(name)) {
            name = nameChange.group(1).replace('\\', '/');
        }

        if (!isLink()) {
            realpath = newPath;
            for (Node link : linksIn) {
                link.delistFromMeta();
                link.realpath = newPath;
                link.refreshLocation();
            }
        }
        for (Node child : new ArrayList<>(fsChildren)) {
            child.changePath(PosixPath.resolve(newPath, PosixPath.relative(oldPath, child.path)));
        }
        for (Map.Entry<String, Node> child : getChildren().entries()) {
            child.getValue().changePath(PosixPath.resolve(newPath, "node_modules", child.getKey()));
        }

        refreshLocation();
    }

    void refreshLocation() {
        Node root = getRoot();
        location = Relpath.relpath(root.realpath, path);
        root.inventory.add(this);
        if (root.meta != null) {
            root.meta.add(this);
        }
    }

    /**
     * @throws OverrideConflict ({@code EOVERRIDE}) when an override changes a direct dependency's spec.
     */
    public void assertRootOverrides() {
        if (!isProjectRoot() || overrides == null) {
            return;
        }
        for (Edge edge : edgesOut.values()) {
            if (!edge.getSpec().equals(edge.getRawSpec()) && !edge.getSpec().startsWith("$")) {
                throw new OverrideConflict("Override for " + edge.getName() + "@" + edge.getRawSpec() +
                                           " conflicts with direct dependency");
            }
        }
    }

    /**
     * npm's {@code EOVERRIDE}.
     */
    public static final class OverrideConflict extends IllegalStateException implements NpmError {
        OverrideConflict(String message) {
            super(message);
        }

        @Override
        public String getCode() {
            return "EOVERRIDE";
        }
    }

    void addEdgeOut(Edge edge) {
        if (overrides != null) {
            edge.setOverrides(overrides.getEdgeRule(edge));
        }
        edgesOut.set(edge.getName(), edge);
    }

    void recalculateOutEdgesOverrides() {
        for (Edge edge : edgesOut.values()) {
            edge.reload(true);
            if (edge.getTo() != null) {
                edge.getTo().updateOverridesEdgeInAdded(edge.getOverrides());
            }
        }
    }

    boolean updateOverridesEdgeInRemoved(@Nullable OverrideSet otherOverrideSet) {
        if (overrides == null || !overrides.isEqual(otherOverrideSet)) {
            return false;
        }
        OverrideSet newOverrideSet = null;
        for (Edge edge : edgesIn) {
            if (newOverrideSet != null && edge.getOverrides() != null) {
                newOverrideSet = OverrideSet.findSpecificOverrideSet(edge.getOverrides(), newOverrideSet);
            } else {
                newOverrideSet = edge.getOverrides();
            }
        }
        if (overrides.isEqual(newOverrideSet)) {
            return false;
        }
        overrides = newOverrideSet;
        if (overrides != null) {
            recalculateOutEdgesOverrides();
        }
        return true;
    }

    /**
     * @return {@code null} where arborist returns undefined, for override sets that conflict.
     */
    @Nullable
    Boolean updateOverridesEdgeInAdded(@Nullable OverrideSet otherOverrideSet) {
        if (otherOverrideSet == null) {
            return false;
        }
        if (overrides == null) {
            overrides = otherOverrideSet;
            recalculateOutEdgesOverrides();
            return true;
        }
        if (overrides.isEqual(otherOverrideSet)) {
            return false;
        }
        OverrideSet newOverrideSet = OverrideSet.findSpecificOverrideSet(overrides, otherOverrideSet);
        if (newOverrideSet != null) {
            if (!overrides.isEqual(newOverrideSet)) {
                overrides = newOverrideSet;
                recalculateOutEdgesOverrides();
                return true;
            }
            return false;
        }
        return null;
    }

    void deleteEdgeIn(Edge edge) {
        edgesIn.remove(edge);
        if (edge.getOverrides() != null) {
            updateOverridesEdgeInRemoved(edge.getOverrides());
        }
    }

    void addEdgeIn(Edge edge) {
        if (overrides == null || !overrides.isEqual(edge.getOverrides())) {
            updateOverridesEdgeInAdded(edge.getOverrides());
        }
        edgesIn.add(edge);

        Shrinkwrap meta = getRoot().meta;
        if (meta != null) {
            meta.addEdge(edge);
        }
    }

    void reloadNamedEdges(String name) {
        reloadNamedEdges(name, location);
    }

    private void reloadNamedEdges(String name, @Nullable String rootLoc) {
        Edge edge = edgesOut.get(name);
        boolean rootLocResolved = edge != null && edge.getTo() != null &&
                                  Objects.equals(edge.getTo().location, rootLoc + "/node_modules/" + edge.getName());
        boolean sameResolved = edge != null && resolve(name) == edge.getTo();
        boolean recheck = rootLocResolved || !sameResolved;
        if (edge != null && recheck) {
            edge.reload(true);
        }
        for (Node c : getChildren().values()) {
            c.reloadNamedEdges(name, rootLoc);
        }
        for (Node c : new ArrayList<>(fsChildren)) {
            c.reloadNamedEdges(name, rootLoc);
        }
    }

    public boolean isLink() {
        return false;
    }

    public @Nullable Node getTarget() {
        return this;
    }

    public CaseInsensitiveMap<Node> getChildren() {
        return children;
    }

    public @Nullable Object getResolved() {
        return resolved;
    }

    public void setResolved(@Nullable Object resolved) {
        this.resolved = resolved;
    }

    public int getDepth() {
        if (isTop()) {
            return 0;
        }
        return parent.getDepth() + 1;
    }

    public boolean isTop() {
        return parent == null;
    }

    public Node getTop() {
        if (isTop()) {
            return this;
        }
        return parent.getTop();
    }

    public boolean isFsTop() {
        return fsParent == null;
    }

    public Node getFsTop() {
        if (isFsTop()) {
            return this;
        }
        return fsParent.getFsTop();
    }

    public @Nullable Node getResolveParent() {
        return parent != null ? parent : fsParent;
    }

    public @Nullable Node resolve(String name) {
        Node mine = getChildren().get(name);
        if (mine != null) {
            return mine;
        }
        Node resolveParent = getResolveParent();
        if (resolveParent != null) {
            return resolveParent.resolve(name);
        }
        return null;
    }

    /**
     * @return the directory whose {@code node_modules} holds this node, or {@code null} when it is not
     * directly inside a {@code node_modules} folder.
     */
    public @Nullable String inNodeModules() {
        String rp = realpath;
        if (rp == null || name == null) {
            return null;
        }
        boolean scoped = name.charAt(0) == '@';
        String d = PosixPath.dirname(rp);
        String nm = scoped ? PosixPath.dirname(d) : d;
        String dir = PosixPath.dirname(nm);
        String base = scoped ? PosixPath.basename(d) + "/" + PosixPath.basename(rp) : PosixPath.basename(rp);
        return base.equals(name) && "node_modules".equals(PosixPath.basename(nm)) ? dir : null;
    }

    public void resetDepFlags() {
        extraneous = true;
        dev = true;
        optional = true;
        devOptional = true;
        peer = true;
    }

    public void unsetDepFlags() {
        extraneous = false;
        dev = false;
        optional = false;
        devOptional = false;
        peer = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + (location != null ? location : path) + " " + getPkgid() + "}";
    }
}
