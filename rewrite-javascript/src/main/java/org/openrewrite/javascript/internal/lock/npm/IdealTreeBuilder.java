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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Spec;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.CONFLICT;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.OK;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.REPLACE;
import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/build-ideal-tree.js (npm 11.17.0) for a lock-only install over an existing
 * lock.
 */
public final class IdealTreeBuilder {

    /**
     * The npm this is a port of: what {@code engines.npm} is checked against.
     */
    public static final String NPM_VERSION = "11.17.0";

    private static final class DepsQueue {
        private final List<Node> deps = new ArrayList<>();
        private boolean sorted = true;

        int length() {
            return deps.size();
        }

        void push(Node item) {
            if (!deps.contains(item)) {
                sorted = false;
                deps.add(item);
            }
        }

        Node pop() {
            if (!sorted) {
                deps.sort((a, b) -> {
                    int byDepth = a.getDepth() - b.getDepth();
                    return byDepth != 0 ? byDepth : LocaleCompare.en(a.getPath(), b.getPath());
                });
                sorted = true;
            }
            return deps.remove(0);
        }
    }

    @Value
    private static class Task {
        Edge edge;
        Node dep;
    }

    private final String path;
    private final VirtualTreeLoader.ProjectFiles files;
    private final NpmOptions options;
    private final PackumentSource registry;
    private final Pacote pacote;
    private final Host host;
    private final Instant now;

    private final boolean preferDedupe;
    private final boolean strictPeerDeps;

    private @Nullable Node currentDep;
    private final DepsQueue depsQueue = new DepsQueue();
    private final Set<Node> depsSeen = new LinkedHashSet<>();
    private final Set<Node> linkNodes = new LinkedHashSet<>();
    private final Set<Node> loadFailures = new LinkedHashSet<>();
    private final Map<String, Map<String, Object>> manifests = new LinkedHashMap<>();
    private boolean mutateTree;
    private final Map<Node, Node> peerSetSource = new IdentityHashMap<>();
    private final Map<Node, Node> virtualRoots = new IdentityHashMap<>();
    private boolean flagsSuspect;

    @Getter
    private @Nullable Node idealTree;

    /**
     * The peer conflicts placement warned about ("ERESOLVE overriding peer dependency") and went past, in order.
     */
    @Getter
    private final List<PlaceDep.PeerConflict> warnings = new ArrayList<>();

    /**
     * The {@code EBADENGINE} warnings an install without {@code engine-strict} logs.
     */
    @Getter
    private final List<NpmInstallChecks.UnsupportedEngine> engineWarnings = new ArrayList<>();

    /**
     * The root package.json as normalized, with the formatting the lock is written in; null when the project
     * has none.
     */
    @Getter
    private @Nullable PackageJson rootManifest;

    /**
     * {@code path}: the project directory the lock's locations resolve against; {@code host}: what npm reads from the
     * machine ({@link Host#UNKNOWN} satisfies every check); {@code now}: the clock {@code min-release-age} counts from.
     */
    public IdealTreeBuilder(String path, VirtualTreeLoader.ProjectFiles files, NpmOptions options,
                            PackumentSource registry, Host host, Instant now) {
        this.path = PosixPath.resolve(path);
        this.files = files;
        this.options = options;
        this.registry = registry;
        this.pacote = new Pacote(registry, options, files, this.path);
        this.host = host;
        this.now = now;
        this.strictPeerDeps = options.isStrictPeerDeps();
        this.preferDedupe = options.isPreferDedupe();
    }

    /**
     * Throws MalformedLock for no lock or one npm discards, CannotKnow where arborist reads what the engine lacks, and
     * npm's rejections: ERESOLVE, E404, EALLOW*, ETARGET, EOVERRIDE, EUNSUPPORTEDPROTOCOL, EBADPLATFORM, EBADENGINE.
     */
    public Node buildIdealTree() {
        if (idealTree != null) {
            return idealTree;
        }
        if (options.getInstallStrategy() == NpmOptions.InstallStrategy.LINKED) {
            throw new CannotKnow("the filesystem (install-strategy=linked places packages outside node_modules)");
        }
        try {
            initTree();
            inflateAncientLockfile();
            applyUserRequests();
            buildDeps();
            fixDepFlags();
            pruneFailedOptional();
            checkEngineAndPlatform();
        } catch (StackOverflowError e) {
            // arborist recurses the same way (some peer cycles nest placements without end): npm's RangeError
            throw new NpmRejection(null, "Maximum call stack size exceeded", null);
        }
        return Objects.requireNonNull(idealTree);
    }

    private void checkEngineAndPlatform() {
        Node tree = Objects.requireNonNull(idealTree);
        Set<String> omitSet = options.omitSet();
        Host environment = Host.builder().os(options.getOs()).cpu(options.getCpu()).libc(options.getLibc()).build();

        for (Node node : tree.getInventory().values()) {
            if (!node.isOptional() && !node.shouldOmit(omitSet)) {
                try {
                    if (!(node.isRoot() && truthy(node.getPackage().get("devEngines")))) {
                        NpmInstallChecks.checkEngine(node.getPackage(), NPM_VERSION, host.getNodeVersion(),
                                options.isForce());
                    }
                } catch (NpmInstallChecks.UnsupportedEngine err) {
                    if (options.isEngineStrict()) {
                        throw err;
                    }
                    engineWarnings.add(err);
                }
                NpmInstallChecks.checkPlatform(node.getPackage(), options.isForce(), Host.UNKNOWN, host);
            }
            if (node.isOptional() && !node.isInert()) {
                try {
                    NpmInstallChecks.checkEngine(node.getPackage(), NPM_VERSION, host.getNodeVersion(), false);
                    NpmInstallChecks.checkPlatform(node.getPackage(), false, environment, host);
                } catch (RuntimeException error) {
                    for (Node n : OptionalSet.optionalSet(node)) {
                        n.setInert(true);
                    }
                }
            }
        }
    }

    private void initTree() {
        Node root;
        try {
            rootManifest = files.manifest("");
            root = rootNodeFromPackage(rootManifest != null ? rootManifest.getContent() : new LinkedHashMap<>());
        } catch (JsonParseEvenBetterErrors.JsonParseException | PackageJson.NeedsPackageFiles err) {
            throw err;
        } catch (RuntimeException err) {
            // arborist starts over from an empty manifest here and writes an empty lock
            throw new MalformedManifest(err instanceof PackageJson.FieldError ?
                    ((PackageJson.FieldError) err).getField() + ": " + err.getMessage() : err.getMessage(), err);
        }
        NpmLockRegenerator.setWorkspaces(root, files);

        VirtualTreeLoader loader = new VirtualTreeLoader(path, options);
        Node tree = loader.loadVirtual(files, root);
        Shrinkwrap meta = Objects.requireNonNull(tree.getMeta());
        if (meta.getLoadingError() != null) {
            throw new MalformedLock("the lock does not parse: " + meta.getLoadingError().getMessage());
        }
        if (!meta.isLoadedFromDisk()) {
            throw new MalformedLock("no lock");
        }
        if (!meta.isLoadedFromObject()) {
            throw new MalformedLock("the lock's JSON root is not an object");
        }
        flagsSuspect = loader.isFlagsSuspect();
        if (rootManifest != null) {
            meta.inferFormattingOptions(rootManifest);
        }

        queueProblemNodes(tree, new LinkedHashSet<>());
        idealTree = tree;
    }

    // treeverse's depth: visit a node, then each edge's target in order, each once
    private void queueProblemNodes(Node node, Set<Node> seen) {
        if (!seen.add(node)) {
            return;
        }
        for (Edge edge : node.getEdgesOut().values()) {
            boolean skipPeerOptional = edge.getType() == Edge.Type.PEER_OPTIONAL && !options.isSave();
            if (!skipPeerOptional && (edge.getTo() == null || !edge.isValid())) {
                depsQueue.push(node);
                break;
            }
        }
        for (Edge edge : node.getEdgesOut().values()) {
            if (edge.getTo() != null) {
                queueProblemNodes(edge.getTo(), seen);
            }
        }
    }

    private Node rootNodeFromPackage(Map<String, Object> pkg) {
        return new Node(Node.options()
                .path(path)
                .realpath(path)
                .pkg(pkg)
                .extraneous(false)
                .dev(false)
                .devOptional(false)
                .peer(false)
                .optional(false)
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .loadOverrides(true)
                .build());
    }

    private void applyUserRequests() {
        depsQueue.push(Objects.requireNonNull(Objects.requireNonNull(idealTree).getTarget()));
    }

    private void inflateAncientLockfile() {
        Node tree = Objects.requireNonNull(idealTree);
        Shrinkwrap meta = Objects.requireNonNull(tree.getMeta());
        Inventory inventory = tree.getInventory();
        boolean ancient = meta.isAncientLockfile();
        boolean old = meta.isLoadedFromDisk() && !(Js.toNumber(meta.getOriginalLockfileVersion()) >= 2);

        if (inventory.size() == 0 || !ancient && !old) {
            return;
        }

        for (Node node : inventory.values()) {
            if (node.isProjectRoot()) {
                continue;
            }
            if (!Objects.requireNonNull(node.getLocation()).startsWith("node_modules")) {
                continue;
            }
            Object resolved = node.getResolved();
            Object version = node.getVersion();
            boolean useResolved = truthy(resolved) && (!truthy(version) || Js.string(resolved).startsWith("file:"));
            Object id = useResolved ? resolved : version;
            Spec spec = Spec.resolve(node.getName(), Js.string(id), PosixPath.dirname(node.getPath()));
            try {
                Pacote.Options pacoteOptions = Pacote.Options.builder()
                        .fullMetadata(false)
                        .before(releaseAgeBefore(spec))
                        .defaultTag(options.getTag())
                        .nodeVersion(host.getNodeVersion())
                        .npmVersion(NPM_VERSION)
                        .integrity(node.getIntegrity())
                        .build();
                Map<String, Object> mani = pacote.manifest(spec, pacoteOptions);
                Map<String, Object> pkg = new LinkedHashMap<>(mani);
                pkg.put("_id", Js.string(Js.get(mani, "name")) + "@" + Js.string(Js.get(mani, "version")));
                node.setPackage(pkg);
            } catch (CannotKnow | PackageJson.NeedsPackageFiles er) {
                throw er;
            } catch (RuntimeException er) {
                // npm warns "Could not fetch metadata" and keeps the lock's entry
            }
        }

        DepFlags.resetDepFlags(tree);
        DepFlags.calcDepFlags(tree);

        if (options.getLockfileVersion() == null) {
            meta.setOriginalLockfileVersion(Shrinkwrap.DEFAULT_LOCKFILE_VERSION);
        }
    }

    private void buildDeps() {
        Node tree = Objects.requireNonNull(Objects.requireNonNull(idealTree).getTarget());
        tree.assertRootOverrides();
        depsQueue.push(tree);
        buildDepStep();
    }

    private void buildDepStep() {
        Node tree = Objects.requireNonNull(idealTree);
        while (true) {
            currentDep = null;

            if (depsQueue.length() == 0) {
                resolveLinks();
                if (depsQueue.length() == 0) {
                    return;
                }
            }

            Node node = depsQueue.pop();
            Object bd = node.getPackage().get("bundleDependencies");
            boolean hasBundle = bd instanceof List && !((List<?>) bd).isEmpty();
            Object hasShrinkwrap = node.getHasShrinkwrap();

            if (depsSeen.contains(node) || node.getRoot() != tree) {
                continue;
            }

            depsSeen.add(node);
            currentDep = node;

            boolean crackOpen = node != tree &&
                                truthy(node.getResolved()) &&
                                (hasBundle || truthy(hasShrinkwrap)) &&
                                !node.isInert();
            if (crackOpen) {
                throw new CannotKnow((truthy(hasShrinkwrap) ? "npm-shrinkwrap.json of " : "bundled dependencies of ") +
                                     node.getPkgid());
            }

            List<Task> tasks = new ArrayList<>();
            Node peerSource = peerSetSource.containsKey(node) ? peerSetSource.get(node) : node;
            for (Edge edge : problemEdges(node)) {
                if (edge.isPeerConflicted()) {
                    continue;
                }

                Node source = edge.isPeer() ? peerSource : node;

                Node virtualRoot = virtualRoot(source, true);
                Edge vrEdge = virtualRoot.getEdgesOut().get(edge.getName());
                Node vrDep = vrEdge != null && vrEdge.isValid() ? vrEdge.getTo() : null;
                Set<Node> required = new LinkedHashSet<>();
                required.add(edge.getFrom());
                Node parent = edge.isPeer() ? virtualRoot : null;
                Node dep = vrDep != null && vrDep.satisfies(edge) ? vrDep : null;

                if (dep == null && edge.getType() == Edge.Type.PEER_OPTIONAL) {
                    Node from = Objects.requireNonNull(edge.getFrom());
                    dep = findHoistableNode(from.getResolveParent() != null ? from.getResolveParent() : from, edge);
                }
                if (dep == null) {
                    dep = nodeFromEdge(edge, parent, null, required);
                }

                tasks.add(new Task(edge, dep));
            }

            tasks.sort((a, b) -> LocaleCompare.en(a.getEdge().getName(), b.getEdge().getName()));

            List<Runnable> promises = new ArrayList<>();
            for (Task task : tasks) {
                PlaceDep pd = new PlaceDep(task.getEdge(), task.getDep(), options);
                warnings.addAll(pd.getWarnings());
                visitPlacements(pd, promises);
            }

            for (Edge edge : node.getEdgesOut().values()) {
                Node to = edge.getTo();
                if (to != null && to.isLink() && to.getTarget() != null) {
                    linkNodes.add(to);
                }
            }

            for (Runnable prefetch : promises) {
                prefetch.run();
            }
        }
    }

    // treeverse's depth with a visit only: a stack, so the children of a placement are visited last first
    private void visitPlacements(PlaceDep top, List<Runnable> promises) {
        Deque<PlaceDep> stack = new ArrayDeque<>();
        stack.push(top);
        while (!stack.isEmpty()) {
            PlaceDep pd = stack.pop();
            visitPlacement(pd, promises);
            for (PlaceDep child : pd.getChildren()) {
                stack.push(child);
            }
        }
    }

    private void visitPlacement(PlaceDep pd, List<Runnable> promises) {
        Node placed = pd.getPlaced();
        Edge edge = pd.getEdge();
        CanPlaceDep cpd = pd.getCanPlace();
        if (placed != null) {
            if (!placed.getErrors().isEmpty()) {
                loadFailures.add(placed);
            }
            mutateTree = true;
            if (Objects.requireNonNull(cpd).getCanPlaceSelf() == OK) {
                for (Edge edgeIn : placed.getEdgesIn()) {
                    if (edgeIn == edge) {
                        continue;
                    }
                    Node from = edgeIn.getFrom();
                    if (!edgeIn.isPeerConflicted() && !edgeIn.isValid()) {
                        if (depsSeen.contains(from) && options.isSave()) {
                            depsSeen.remove(from);
                            depsQueue.push(from);
                        } else if (!depsSeen.contains(from)) {
                            depsQueue.push(from);
                        }
                    }
                }
            } else if (cpd.getCanPlaceSelf() == REPLACE) {
                for (Edge edgeIn : placed.getEdgesIn()) {
                    if (edgeIn == edge) {
                        continue;
                    }
                    if (!edgeIn.isValid() && !edgeIn.isPeerConflicted()) {
                        depsSeen.remove(edgeIn.getFrom());
                        depsQueue.push(edgeIn.getFrom());
                    }
                }
            }

            if (cpd.getCanPlaceSelf() != CONFLICT) {
                depsQueue.push(placed);
                for (Node dep : pd.getNeedEvaluation()) {
                    depsSeen.remove(dep);
                    depsQueue.push(dep);
                }

                for (Edge e : problemEdges(placed)) {
                    promises.add(() -> {
                        try {
                            fetchManifest(Spec.resolve(e.getName(), e.getSpec(), FromPath.fromPath(placed, e)), e);
                        } catch (RuntimeException ignored) {
                        }
                    });
                }
            }
        }
    }

    private @Nullable Node findHoistableNode(Node root, Edge edge) {
        Deque<Node> queue = new ArrayDeque<>(root.getChildren().values());
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (Objects.equals(node.getName(), edge.getName()) && !node.isInDepBundle() && node.satisfies(edge)) {
                return node;
            }
            queue.addAll(node.getChildren().values());
        }
        return null;
    }

    private Node nodeFromEdge(Edge edge, @Nullable Node parentOption, @Nullable Edge secondEdge, Set<Node> required) {
        Node from = Objects.requireNonNull(edge.getFrom());
        Node parent = parentOption != null ? parentOption : virtualRoot(from, false);

        Spec spec = Spec.resolve(edge.getName(), edge.getSpec(), from.getPath());
        Node first = nodeFromSpec(edge.getName(), spec, parent, edge);

        Spec spec2 = secondEdge != null ? Spec.resolve(edge.getName(), secondEdge.getSpec(),
                Objects.requireNonNull(secondEdge.getFrom()).getPath()) : null;
        Node second = secondEdge != null && !secondEdge.isValid() ?
                nodeFromSpec(edge.getName(), spec2, parent, secondEdge) : null;

        Node node = second != null && edge.isValid() ? second : first;
        node.setParent(parent);

        if (required.contains(from) && edge.getType() != Edge.Type.PEER_OPTIONAL ||
            secondEdge != null && required.contains(secondEdge.getFrom()) &&
            secondEdge.getType() != Edge.Type.PEER_OPTIONAL) {
            required.add(node);
        }

        Node src = parent.getSourceReference();
        peerSetSource.put(node, src);

        return loadPeerSet(node, required);
    }

    private Node virtualRoot(Node node, boolean reuse) {
        if (reuse && virtualRoots.containsKey(node)) {
            return virtualRoots.get(node);
        }

        Node vr = new Node(Node.options()
                .path(node.getRealpath())
                .sourceReference(node)
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .overrides(node.getOverrides())
                .build());

        for (Node child : node.getChildren().values()) {
            if (child.isLink()) {
                new Node(Node.options()
                        .path(child.getRealpath())
                        .sourceReference(child.getTarget())
                        .root(vr)
                        .build());
            }
        }

        virtualRoots.put(node, vr);
        return vr;
    }

    private List<Edge> problemEdges(Node node) {
        Object bd = node.isProjectRoot() || node.isWorkspace() ? null : node.getPackage().get("bundleDependencies");
        Set<Object> bundled = new LinkedHashSet<>();
        if (bd instanceof List) {
            bundled.addAll((List<?>) bd);
        } else if (bd instanceof String) {
            for (char c : ((String) bd).toCharArray()) {
                bundled.add(String.valueOf(c));
            }
        }

        List<Edge> problems = new ArrayList<>();
        for (Edge edge : node.getEdgesOut().values()) {
            if (bundled.contains(edge.getName())) {
                continue;
            }

            Node to = edge.getTo();
            if (to != null && loadFailures.contains(to)) {
                continue;
            }

            if (to != null && to.isInShrinkwrap()) {
                continue;
            }

            if (to == null) {
                if (edge.getType() != Edge.Type.PEER_OPTIONAL) {
                    problems.add(edge);
                }
                continue;
            }

            if (!edge.isValid() && (edge.getType() != Edge.Type.PEER_OPTIONAL || options.isSave())) {
                problems.add(edge);
            }
        }
        return problems;
    }

    private @Nullable Double releaseAgeBefore(Spec spec) {
        Object before = options.before(now);
        if (before == null) {
            return null;
        }
        if (ReleaseAgeExclude.isReleaseAgeExcluded(ReleaseAgeExclude.trustedSpecName(spec),
                options.getMinReleaseAgeExclude())) {
            return null;
        }
        if (before instanceof String) {
            throw new CannotKnow("the date before=" + before + " names (only V8's legacy date parser reads it)");
        }
        return (Double) before;
    }

    private Map<String, Object> fetchManifest(Spec spec, @Nullable Edge edge) {
        checkAllow(spec, edge);
        Map<String, Object> fetchOptions = new LinkedHashMap<>();
        Pacote.Options.OptionsBuilder pacoteOptions = Pacote.Options.builder()
                .fullMetadata(true)
                .before(releaseAgeBefore(spec))
                .defaultTag(options.getTag())
                .nodeVersion(host.getNodeVersion())
                .npmVersion(NPM_VERSION);
        spec = Objects.requireNonNull(Objects.requireNonNull(idealTree).getMeta()).checkYarnLock(spec, fetchOptions);

        Map<String, Object> cached = manifests.get(spec.getRaw());
        if (cached != null) {
            return cached;
        }
        Map<String, Object> mani = pacote.manifest(spec,
                pacoteOptions.integrity(fetchOptions.get("integrity")).build());
        manifests.put(spec.getRaw(), mani);
        return mani;
    }

    private void checkAllow(Spec spec, @Nullable Edge edge) {
        String allow;
        switch (Objects.requireNonNull(spec.getType())) {
            case GIT:
                allow = options.getAllowGit();
                break;
            case REMOTE:
                allow = options.getAllowRemote();
                break;
            case FILE:
                allow = options.getAllowFile();
                break;
            case DIRECTORY:
                allow = options.getAllowDirectory();
                break;
            default:
                return;
        }
        if ("all".equals(allow)) {
            return;
        }
        Node from = edge == null ? null : edge.getFrom();
        boolean isRoot = from != null && (from.isProjectRoot() || from.isWorkspace());
        if (!"none".equals(allow) && isRoot) {
            return;
        }
        String type = spec.getType().toString();
        throw new NpmRejection("EALLOW" + type.toUpperCase(),
                "Fetching" + ("root".equals(allow) ? " non-root" : "") + " packages of type \"" + type +
                "\" have been disabled", spec.toString());
    }

    private Node failureNode(String name, @Nullable Node parent, RuntimeException error, @Nullable Edge edge) {
        if (error instanceof NpmRejection) {
            Node from = edge == null ? null : edge.getFrom();
            ((NpmRejection) error).setRequiredBy(from != null && truthy(from.getLocation()) ? from.getLocation() : ".");
        }
        Node n = new Node(Node.options()
                .name(name)
                .parent(parent)
                .error(error)
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .build());
        loadFailures.add(n);
        return n;
    }

    private Node nodeFromSpec(String name, Spec spec, Node parent, @Nullable Edge edge) {
        try {
            checkAllow(spec, edge);
        } catch (NpmRejection error) {
            return failureNode(name, parent, error, edge);
        }

        Node tree = Objects.requireNonNull(idealTree);
        boolean installLinks = options.isInstallLinks();
        boolean isWorkspace = tree.getWorkspaces() != null && tree.getWorkspaces().containsKey(spec.getName());

        boolean isProjectInternalFileSpec = false;
        if (edge != null && (edge.getRawSpec().startsWith("file:../") || edge.getRawSpec().startsWith("file:./"))) {
            String targetPath = PosixPath.resolve(parent.getRealpath(), edge.getRawSpec().substring(5));
            String resolvedProjectRoot = PosixPath.resolve(tree.getRealpath());
            isProjectInternalFileSpec = targetPath.startsWith(resolvedProjectRoot + "/") ||
                                        targetPath.equals(resolvedProjectRoot);
        }

        boolean parentWasInstalled = !parent.isLink() && truthy(parent.getResolved()) &&
                                     Js.string(parent.getResolved()).startsWith("file:");
        boolean isTransitiveFileDep = spec.getType() == Spec.Type.DIRECTORY && parentWasInstalled && installLinks;

        boolean shouldLink = (isWorkspace || isProjectInternalFileSpec || !installLinks) && !isTransitiveFileDep;
        if (spec.getType() == Spec.Type.DIRECTORY && shouldLink) {
            String realpath = Objects.requireNonNull(spec.getFetchSpec());
            Link link = new Link(Node.options()
                    .name(name)
                    .parent(parent)
                    .realpath(realpath)
                    .pkg(directoryManifest(realpath, spec))
                    .installLinks(installLinks)
                    .legacyPeerDeps(options.isLegacyPeerDeps())
                    .build());
            linkNodes.add(link);
            return link;
        }

        if (isWorkspace) {
            Edge rootEdge = tree.getEdgesOut().get(spec.getName());
            Node existingNode = rootEdge == null ? null : rootEdge.getTo();
            if (existingNode != null && existingNode.isWorkspace() && edge != null && existingNode.satisfies(edge)) {
                return existingNode;
            }
        }

        if (isTransitiveFileDep && edge != null) {
            String parentOriginalPath = Js.string(parent.getResolved()).substring(5);
            String relativePath = edge.getRawSpec().substring(5);
            String absolutePath = PosixPath.resolve(parentOriginalPath, relativePath);
            spec = Spec.resolve(name, "file:" + absolutePath, null);
        }

        Map<String, Object> pkg;
        try {
            pkg = fetchManifest(spec, edge);
        } catch (CannotKnow | PackageJson.NeedsPackageFiles error) {
            throw error;
        } catch (RuntimeException error) {
            return failureNode(name, parent, error, edge);
        }
        if (!truthy(pkg.get("version")) && spec.isRegistry()) {
            return failureNode(name, parent, new NpmRejection("EINCOMPLETEMANIFEST",
                    "incomplete manifest for " + name + ", missing version", null), edge);
        }
        return new Node(Node.options()
                .name(name)
                .pkg(pkg)
                .parent(parent)
                .installLinks(installLinks)
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .build());
    }

    // PackageJson.normalize(realpath).catch(() => ({ content: {} })) over the project's files
    private Map<String, Object> directoryManifest(String realpath, Spec spec) {
        String location = Relpath.relpath(Objects.requireNonNull(idealTree).getRealpath(), realpath);
        PackageJson manifest;
        try {
            manifest = files.manifest(location);
        } catch (PackageJson.NeedsPackageFiles e) {
            throw e;
        } catch (RuntimeException e) {
            return new LinkedHashMap<>();
        }
        if (manifest == null) {
            throw new CannotKnow("directory manifest " + spec.getRawSpec() + " (" + location + ")");
        }
        return manifest.getContent();
    }

    private Node loadPeerSet(Node node, Set<Node> required) {
        List<Edge> peerEdges = new ArrayList<>();
        for (Edge e : node.getEdgesOut().values()) {
            if (e.isPeer() && !(e.isValid() && e.getTo() != null)) {
                peerEdges.add(e);
            }
        }
        peerEdges.sort((a, b) -> LocaleCompare.en(a.getName(), b.getName()));

        for (Edge edge : peerEdges) {
            Node nodeParent = node.getParent();
            if (nodeParent == null) {
                break;
            }
            if (edge.isValid() && edge.getTo() != null) {
                continue;
            }

            Edge parentEdge = nodeParent.getEdgesOut().get(edge.getName());
            Node sourceReference = Objects.requireNonNull(nodeParent.getSourceReference());
            boolean isMine = sourceReference.isProjectRoot() || sourceReference.isWorkspace();
            boolean conflictOK = options.isForce() || !isMine && !strictPeerDeps;

            if (edge.getTo() == null) {
                if (parentEdge == null) {
                    nodeFromEdge(edge, nodeParent, null, required);
                    continue;
                } else {
                    Node dep = nodeFromEdge(parentEdge, nodeParent, edge, required);

                    if (edge.isValid()) {
                        continue;
                    }

                    if (conflictOK || !required.contains(dep)) {
                        edge.setPeerConflicted(true);
                        continue;
                    }

                    throw failPeerConflict(edge, parentEdge);
                }
            }

            Node current = edge.getTo();
            Node dep = nodeFromEdge(edge, null, null, required);
            if (dep.canReplace(current)) {
                nodeFromEdge(edge, nodeParent, null, required);
                continue;
            }

            if (conflictOK || !required.contains(edge.getFrom())) {
                continue;
            }

            throw failPeerConflict(edge, parentEdge);
        }
        return node;
    }

    private PlaceDep.CouldNotResolve failPeerConflict(Edge edge, @Nullable Edge currentEdge) {
        Node node = Objects.requireNonNull(edge.getFrom());
        Node curNode = node.resolve(edge.getName());
        if (curNode == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'explain')");
        }
        Map<String, Object> expl = new LinkedHashMap<>();
        expl.put("code", "ERESOLVE");
        expl.put("current", curNode.explain());
        expl.put("currentEdge", currentEdge != null ? currentEdge.explain() : null);
        expl.put("edge", edge.explain());
        expl.put("strictPeerDeps", strictPeerDeps);
        expl.put("force", options.isForce());
        return new PlaceDep.CouldNotResolve("unable to resolve dependency tree",
                new PlaceDep.PeerConflict(edge, null, curNode, null, null, expl), warnings);
    }

    private void resolveLinks() {
        Node tree = Objects.requireNonNull(idealTree);
        for (Node link : new ArrayList<>(linkNodes)) {
            linkNodes.remove(link);

            if (link.getRoot() != tree) {
                continue;
            }

            Node target = Objects.requireNonNull(link.getTarget());
            boolean external = !target.isDescendantOf(Objects.requireNonNull(tree.getTarget()));

            if (external) {
                continue;
            }

            boolean unseenLink = (target.getParent() != null || target.getFsParent() != null) &&
                                 !depsSeen.contains(target);

            if (unseenLink) {
                depsQueue.push(target);
            }
        }
    }

    private void fixDepFlags() {
        Node tree = Objects.requireNonNull(idealTree);
        boolean metaFromDisk = Objects.requireNonNull(tree.getMeta()).isLoadedFromDisk();
        if (metaFromDisk && mutateTree) {
            DepFlags.resetDepFlags(tree);
        }

        if (!metaFromDisk || mutateTree) {
            DepFlags.calcDepFlags(tree);
        } else {
            tree.unsetDepFlags();
        }

        boolean needPrune = metaFromDisk && (mutateTree || flagsSuspect);
        if (needPrune) {
            idealTreePrune();
        }
    }

    private void idealTreePrune() {
        for (Node node : Objects.requireNonNull(idealTree).getInventory().values()) {
            if (node.isExtraneous()) {
                node.setParent(null);
            }
        }
    }

    private void pruneFailedOptional() {
        for (Node node : loadFailures) {
            if (!node.isOptional()) {
                Throwable error = node.getErrors().get(0);
                throw error instanceof RuntimeException ? (RuntimeException) error : new IllegalStateException(error);
            }

            for (Node n : OptionalSet.optionalSet(node)) {
                n.setInert(true);
            }
        }
    }
}
