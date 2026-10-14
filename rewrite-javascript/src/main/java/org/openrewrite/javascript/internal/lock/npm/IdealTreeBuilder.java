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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.CONFLICT;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.OK;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.REPLACE;
import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/build-ideal-tree.js (npm 11.17.0) for a lock-only install over an existing
 * lock.
 */
public final class IdealTreeBuilder {

    private static final class DepsQueue {
        private final List<Node> deps = new ArrayList<>();
        private boolean sorted = true;

        // npm 10.4.0 until 10.8.0: [{ sorted, items }] indexed by depth
        private final boolean byDepth;
        private final List<@Nullable Depth> depths = new ArrayList<>();
        private int minDepth;
        private int length;

        // before npm 9.6.6: this[_depsQueue], an array pushed to as it is and sorted at every #buildDepStep
        private final boolean everyStep;

        private static final class Depth {
            boolean sorted = true;
            final List<Node> items = new ArrayList<>();
        }

        DepsQueue(NpmRelease release) {
            byDepth = release.queuesDepsByDepth();
            everyStep = release.sortsDepsQueueAtEveryStep();
        }

        int length() {
            return byDepth ? length : deps.size();
        }

        void push(Node item) {
            if (byDepth) {
                int itemDepth = item.getDepth();
                Depth depth = itemDepth < depths.size() ? depths.get(itemDepth) : null;
                if (depth == null) {
                    length++;
                    depth = new Depth();
                    depth.items.add(item);
                    while (depths.size() <= itemDepth) {
                        depths.add(null);
                    }
                    depths.set(itemDepth, depth);
                    // upstream: "no minDepth check needed, this branch is only reached when we are in the middle of a
                    // shallower depth and creating a new one"
                    return;
                }
                if (!depth.items.contains(item)) {
                    length++;
                    depth.sorted = false;
                    depth.items.add(item);
                    if (itemDepth < minDepth) {
                        minDepth = itemDepth;
                    }
                }
                return;
            }
            if (everyStep) {
                deps.add(item);
                return;
            }
            if (!deps.contains(item)) {
                sorted = false;
                deps.add(item);
            }
        }

        Node pop() {
            if (byDepth) {
                Depth depth = null;
                while (depth == null || depth.items.isEmpty()) {
                    if (minDepth >= depths.size()) {
                        // a depth first queued while a deeper one was being worked on is never looked at again: the
                        // loop counts minDepth up without end, and npm hangs
                        throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "it never finishes: its queue of " +
                                "dependencies to build no longer reaches " + stranded());
                    }
                    depth = depths.get(minDepth);
                    if (depth == null || depth.items.isEmpty()) {
                        minDepth++;
                    }
                }
                if (!depth.sorted) {
                    depth.items.sort((a, b) -> LocaleCompare.en(a.getPath(), b.getPath()));
                    depth.sorted = true;
                }
                length--;
                return depth.items.remove(0);
            }
            if (!sorted || everyStep) {
                deps.sort((a, b) -> {
                    int byDepth = a.getDepth() - b.getDepth();
                    return byDepth != 0 ? byDepth : LocaleCompare.en(a.getPath(), b.getPath());
                });
                sorted = true;
            }
            return deps.remove(0);
        }

        private String stranded() {
            for (Depth depth : depths) {
                if (depth != null && !depth.items.isEmpty()) {
                    Node node = depth.items.get(0);
                    return truthy(node.getLocation()) ? node.getLocation() : node.getName();
                }
            }
            return "anything";
        }

        /**
         * Every node waiting, in no particular order.
         */
        List<Node> items() {
            List<Node> items = new ArrayList<>(deps);
            for (Depth depth : depths) {
                if (depth != null) {
                    items.addAll(depth.items);
                }
            }
            return items;
        }
    }

    @Value
    private static class Task {
        Edge edge;
        Node dep;
    }

    // where every virtual root stands before npm 7.6.3
    private static final String VIRTUAL_ROOT = "/virtual-root";

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
    private final DepsQueue depsQueue;
    private final Set<Node> depsSeen = new LinkedHashSet<>();
    private final Set<Node> linkNodes = new LinkedHashSet<>();
    private final Set<Node> loadFailures = new LinkedHashSet<>();
    private final Map<String, Map<String, Object>> manifests = new LinkedHashMap<>();
    private boolean mutateTree;
    private @Nullable PackageExtensions packageExtensions;
    private final Map<Node, Node> peerSetSource = new IdentityHashMap<>();
    private final Map<Node, Node> virtualRoots = new IdentityHashMap<>();
    private boolean flagsSuspect;

    // before npm 7.24.2: what the last step replaced, and the state the loop was in after each step that replaced
    // something, with that replacement
    private @Nullable String replaced;
    private final Map<List<Object>, String> statesAfterReplace = new HashMap<>();

    // before npm 7.20.3: build-ideal-tree.js's own placement, made when it is first asked
    private @Nullable LegacyPlacement legacyPlacement;

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
        this.depsQueue = new DepsQueue(options.getRelease());
        this.host = host;
        this.now = now;
        this.strictPeerDeps = options.isStrictPeerDeps();
        this.preferDedupe = options.isPreferDedupe();
    }

    /**
     * Throws MalformedLock for no lock or one npm discards, CannotKnow where arborist reads what the engine lacks, and
     * npm's rejections: ERESOLVE, E404, EALLOW*, ETARGET, EOVERRIDE, EUNSUPPORTEDPROTOCOL, EBADPLATFORM, EBADENGINE,
     * and from npm 12.0.0 ELOCKFILEVERSION, EEXTENSION* and EPATCH*.
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
            loadPackageExtensions();
            inflateAncientLockfile();
            applyUserRequests();
            buildDeps();
            fixDepFlags();
            pruneFailedOptional();
            checkEngineAndPlatform();
            resolvePatchedDependencies();
        } catch (StackOverflowError e) {
            // arborist recurses the same way (some peer cycles nest placements without end): npm's RangeError
            throw new NpmRejection(null, "Maximum call stack size exceeded", null);
        }
        return Objects.requireNonNull(idealTree);
    }

    private void checkEngineAndPlatform() {
        Node tree = Objects.requireNonNull(idealTree);
        Set<String> omitSet = options.omitSet();
        String npmVersion = options.getNpmVersion();
        Host environment = Host.builder().os(options.getOs()).cpu(options.getCpu()).libc(options.getLibc()).build();

        NpmRelease release = options.getRelease();
        for (Node node : tree.getInventory().values()) {
            // before npm 7.1.1 (arborist 2.0.0) the root is not in its own inventory
            if (node.isRoot() && !release.inventoriesRoot()) {
                continue;
            }
            if (!node.isOptional() && !(release.skipsEngineCheckOfOmitted() && node.shouldOmit(omitSet))) {
                try {
                    if (!(options.getRelease().devEnginesReplaceRootEngines() && node.isRoot() &&
                          truthy(node.getPackage().get("devEngines")))) {
                        NpmInstallChecks.checkEngine(node.getPackage(), npmVersion, host.getNodeVersion(),
                                options.isForce(), options.getRelease());
                    }
                } catch (NpmInstallChecks.UnsupportedEngine err) {
                    if (options.isEngineStrict()) {
                        throw err;
                    }
                    engineWarnings.add(err);
                }
                NpmInstallChecks.checkPlatform(node.getPackage(), options.isForce(), Host.UNKNOWN, host, release);
            }
            if (release.marksUninstallableOptionalInert() && node.isOptional() && !node.isInert()) {
                try {
                    NpmInstallChecks.checkEngine(node.getPackage(), npmVersion, host.getNodeVersion(), false,
                            options.getRelease());
                    NpmInstallChecks.checkPlatform(node.getPackage(), false, environment, host, release);
                } catch (RuntimeException error) {
                    for (Node n : OptionalSet.optionalSet(node)) {
                        n.setInert(true);
                    }
                }
            }
        }
    }

    // npm 12.0.0's #loadPackageExtensions. It follows #loadNpmExtension, which --ignore-scripts turns off
    // (ignoreExtension): the lock's .npm-extension state is then left as it is
    private void loadPackageExtensions() {
        Node tree = Objects.requireNonNull(idealTree);
        Shrinkwrap meta = Objects.requireNonNull(tree.getMeta());
        Object raw = Js.get(Objects.requireNonNull(tree.getTarget()).getPackage(), "packageExtensions");
        Object lockedHash = meta.getPackageExtensionsHash();
        if (raw == Js.UNDEFINED && lockedHash == null || !options.getRelease().appliesPackageExtensions()) {
            return;
        }
        packageExtensions = new PackageExtensions(raw, options.getRelease().semver());
        meta.setPackageExtensionsHash(packageExtensions.getHash());

        // a rule set that changed since the lock was written leaves the locked manifests of the packages it
        // extends, or extended, stale: they are rebuilt from fresh manifests under the current rules
        if (meta.isLoadedFromDisk() && !Js.strictEquals(lockedHash, packageExtensions.getHash())) {
            for (Node node : new ArrayList<>(tree.getInventory().values())) {
                if (node.isProjectRoot() || node.isWorkspace() || node.isTop()) {
                    continue;
                }
                boolean affected = truthy(node.getPackageExtensionsApplied()) ||
                                   packageExtensions.wouldMatch(node.getPackageName(), node.getVersion());
                if (affected) {
                    for (Edge edge : node.getEdgesIn()) {
                        depsQueue.push(edge.getFrom());
                    }
                    node.setParent(null);
                }
            }
        }
    }

    // npm 12.0.0's resolvePatchedDependencies, which does nothing a lock shows without a patch declared or recorded
    private void resolvePatchedDependencies() {
        Node tree = Objects.requireNonNull(idealTree);
        if (PatchedDependencies.concerns(tree) && options.getRelease().resolvesPatchedDependencies()) {
            PatchedDependencies.resolvePatchedDependencies(tree, path, files.getPaths());
        }
    }

    private void initTree() {
        Node root;
        try {
            rootManifest = files.manifest("");
            root = rootNodeFromPackage(rootManifest != null ? rootManifest.getContent() : new LinkedHashMap<>());
        } catch (JsonParseEvenBetterErrors.JsonParseException | PackageJson.NeedsPackageFiles | CannotKnow err) {
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
        if (rootManifest != null && options.getRelease().infersLockFormatFromManifest()) {
            meta.inferFormattingOptions(rootManifest);
        }
        if (options.getRelease().appliesRootOverridesToWorkspaces()) {
            applyRootOverridesToWorkspaces(tree);
        }

        if (options.getRelease().queuesProblemNodesOfLock()) {
            queueProblemNodes(tree, new LinkedHashSet<>());
        }
        idealTree = tree;
    }

    // a workspace's edge to a package the root overrides is detached, with the workspace's own copy of it, when the
    // root's copy does not satisfy the edge
    private void applyRootOverridesToWorkspaces(Node tree) {
        Object overrides = tree.getRoot().getPackage().get("overrides");
        List<String> rootOverrides = truthy(overrides) ? Js.keys(overrides) : new ArrayList<>();

        for (Node node : tree.getRoot().getInventory().values()) {
            if (!node.isWorkspace()) {
                continue;
            }

            for (String depName : rootOverrides) {
                Edge edge = node.getEdgesOut().get(depName);
                Node rootNode = tree.getRoot().getChildren().get(depName);

                if (edge == null || rootNode == null) {
                    continue;
                }

                Object resolvedRootVersion = Js.get(rootNode.getPackage(), "version");
                if (!NodeSemver.satisfies(resolvedRootVersion == Js.UNDEFINED ? null : resolvedRootVersion,
                        edge.getSpec(), NodeSemver.Options.DEFAULT.release(options.getRelease().semver()))) {
                    edge.detach();
                    node.getChildren().delete(depName);
                }
            }
        }
    }

    // treeverse's depth: visit a node, then each edge's target in order, each once
    private void queueProblemNodes(Node node, Set<Node> seen) {
        if (!seen.add(node)) {
            return;
        }
        NpmRelease release = options.getRelease();
        for (Edge edge : node.getEdgesOut().values()) {
            boolean problem;
            if (release.resolvesInvalidPeerOptionalBySave()) {
                boolean skipPeerOptional = edge.getType() == Edge.Type.PEER_OPTIONAL && !options.isSave();
                problem = !skipPeerOptional && (edge.getTo() == null || !edge.isValid());
            } else if (release.queuesNodesMissingDependencies()) {
                problem = edge.getTo() == null && (!release.leavesMissingPeerOptionalUnqueued() ||
                                                   edge.getType() != Edge.Type.PEER_OPTIONAL) || !edge.isValid();
            } else {
                problem = !edge.isValid();
            }
            if (problem) {
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
                .release(options.getRelease())
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
        Node tree = Objects.requireNonNull(idealTree);
        // before npm 7.1.1 every install triggers a refresh of all the root's edges out
        if (!options.getRelease().refreshesRootPackageForAddOrRemoveOnly()) {
            tree.setPackage(tree.getPackage());
        }
        depsQueue.push(Objects.requireNonNull(tree.getTarget()));
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
            boolean skipsLinkTargets = options.getRelease().skipsLinkTargetsWhenInflating();
            if (skipsLinkTargets && !Objects.requireNonNull(node.getLocation()).startsWith("node_modules")) {
                continue;
            }
            Object resolved = node.getResolved();
            Object version = node.getVersion();
            Object id;
            String where;
            if (options.getRelease().inflatesFromParentDirectory()) {
                boolean useResolved = truthy(resolved) && (!truthy(version) || Js.string(resolved).startsWith("file:"));
                // before npm 8.6.0, version || `file:${node.path}`
                id = useResolved ? resolved : skipsLinkTargets || truthy(version) ? version : "file:" + node.getPath();
                where = PosixPath.dirname(node.getPath());
            } else {
                // before npm 7.18.0 the resolved of a node without a version, which may be none (the latest), read
                // from the node's own path
                boolean useResolved = !truthy(version) || truthy(resolved) && Js.string(resolved).startsWith("file:");
                id = useResolved ? resolved : version;
                where = node.getPath();
            }
            Spec spec = !options.getRelease().inflatesLocalNodesByResolved() ?
                    // before npm 7.1.1 every node by `${node.name}@${node.version}`, read from the working directory
                    Spec.npa(node.getName() + "@" + Js.string(version), path, options.getRelease().npmPackageArg()) :
                    Spec.resolve(node.getName(), id == null ? "" : Js.string(id), where,
                            options.getRelease().npmPackageArg());
            try {
                Pacote.Options pacoteOptions = Pacote.Options.builder()
                        .fullMetadata(false)
                        .before(releaseAgeBefore(spec))
                        .defaultTag(options.getTag())
                        .nodeVersion(host.getNodeVersion())
                        .npmVersion(options.getNpmVersion())
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

        if (options.getRelease().resetsDepFlagsOfInflatedLock()) {
            DepFlags.resetDepFlags(tree);
        }
        if (options.getRelease().recalculatesFlagsOfInflatedLock()) {
            DepFlags.calcDepFlags(tree);
        }

        if (options.getLockfileVersion() == null) {
            meta.setOriginalLockfileVersion(options.getRelease().defaultLockfileVersion());
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

            if (replaced != null) {
                // before npm 7.24.2 the peer set a replacement no longer satisfies stays where it is, and building it
                // again can replace the replacement in turn: a loop that comes back to where it was never ends
                String earlier = statesAfterReplace.putIfAbsent(loopState(), replaced);
                if (earlier != null) {
                    throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "it never finishes: its tree and its " +
                            "queue of dependencies to build come back to where they were when " + earlier);
                }
                replaced = null;
            }

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
                // pacote.extract(node.resolved, ...) reads the resolved value as a spec of its own, with the options as
                // they are: no dependent makes it a root's
                checkAllow(Spec.npa(node.getResolved(), null, options.getRelease().npmPackageArg()), null);
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

                if (dep == null && edge.getType() == Edge.Type.PEER_OPTIONAL &&
                    options.getRelease().findsHoistablePeerOptional()) {
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
            if (!options.getRelease().placesWithPlaceDep()) {
                // tasks.sort(...).map(({ edge, dep }) => this[_placeDep](dep, node, edge)), each placing its peers as
                // it goes; then every node placed is queued and its problem edges' manifests fetched
                List<List<Node>> placed = new ArrayList<>();
                for (Task task : tasks) {
                    placed.add(legacyPlacement().placeDep(task.getDep(), node, task.getEdge(), null,
                            Collections.emptyList()));
                }
                for (List<Node> set : placed) {
                    for (Node p : set) {
                        mutateTree = true;
                        depsQueue.push(p);
                        String from = FromPath.fromPath(p, null);
                        for (Edge e : problemEdges(p)) {
                            Spec spec = Spec.resolve(e.getName(), e.getSpec(), from,
                                    options.getRelease().npmPackageArg());
                            promises.add(() -> {
                                try {
                                    fetchManifest(spec, null, null);
                                } catch (RuntimeException ignored) {
                                }
                            });
                        }
                    }
                }
            } else if (options.getRelease().placesWholeStepBeforeVisiting()) {
                // tasks.sort(...).map(({ edge, dep }) => new PlaceDep(...)), then the visits
                List<PlaceDep> placeDeps = new ArrayList<>();
                for (Task task : tasks) {
                    PlaceDep pd = new PlaceDep(task.getEdge(), task.getDep(), options);
                    warnings.addAll(pd.getWarnings());
                    placeDeps.add(pd);
                }
                for (PlaceDep pd : placeDeps) {
                    visitPlacements(pd, promises);
                }
            } else {
                for (Task task : tasks) {
                    PlaceDep pd = new PlaceDep(task.getEdge(), task.getDep(), options);
                    warnings.addAll(pd.getWarnings());
                    visitPlacements(pd, promises);
                }
            }

            // before npm 7.0.9 only the links the build makes are resolved
            for (Edge edge : options.getRelease().resolvesLinksOfDependencies() ? node.getEdgesOut().values() :
                    Collections.<Edge>emptyList()) {
                Node to = edge.getTo();
                // before npm 7.20.0 a link that lost its target is queued too
                if (to != null && to.isLink() &&
                    (to.getTarget() != null || !options.getRelease().readsExternalLinksByTarget())) {
                    linkNodes.add(to);
                }
            }

            for (Runnable prefetch : promises) {
                prefetch.run();
            }
        }
    }

    private LegacyPlacement legacyPlacement() {
        if (legacyPlacement == null) {
            legacyPlacement = new LegacyPlacement(Objects.requireNonNull(idealTree), options, depsQueue::push,
                    depsSeen, loadFailures, peerSetSource, virtualRoots, warnings);
        }
        return legacyPlacement;
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
                        if (depsSeen.contains(from) && options.isSave() &&
                            options.getRelease().resolvesInvalidPeerOptionalBySave()) {
                            depsSeen.remove(from);
                            depsQueue.push(from);
                        } else if (!depsSeen.contains(from)) {
                            depsQueue.push(from);
                        }
                    }
                }
            } else if (cpd.getCanPlaceSelf() == REPLACE) {
                if (!options.getRelease().prunesConflictedPeerSets()) {
                    replaced = placed.getName() + "@" + Js.string(placed.getVersion()) + " replaced the one at " +
                               placed.getLocation();
                }
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
                            // arborist passes the parent placement here, which is no node
                            fetchManifest(Spec.resolve(e.getName(), e.getSpec(), FromPath.fromPath(placed, e),
                                    options.getRelease().npmPackageArg()), null, e);
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

        Spec.Options npa = options.getRelease().npmPackageArg();
        Spec spec = Spec.resolve(edge.getName(), edge.getSpec(), from.getPath(), npa);
        Node first = nodeFromSpec(edge.getName(), spec, parent, edge);

        Spec spec2 = secondEdge != null ? Spec.resolve(edge.getName(), secondEdge.getSpec(),
                Objects.requireNonNull(secondEdge.getFrom()).getPath(), npa) : null;
        Node second = secondEdge != null && !secondEdge.isValid() ?
                nodeFromSpec(edge.getName(), spec2, parent, secondEdge) : null;

        Node node = second != null && edge.isValid() ? second : first;
        if (node == null) {
            // a satisfying workspace was taken for an edge to nothing, before npm 9.6.0
            throw new JsError(JsError.TYPE_ERROR, "Cannot set properties of null (setting 'parent')");
        }
        node.setParent(parent);

        if (required.contains(from) && edge.getType() != Edge.Type.PEER_OPTIONAL ||
            secondEdge != null && required.contains(secondEdge.getFrom()) &&
            secondEdge.getType() != Edge.Type.PEER_OPTIONAL) {
            required.add(node);
        }

        if (!options.getRelease().linksNestingLoopsWherePlaced()) {
            // before npm 7.20.6 a nesting loop (a1 -> b1 -> a2 -> b2 -> a1 -> ...) is linked to the earlier instance
            // here, under the dependent, or under its resolve parent for a peer; before 7.1.1 under the virtual root
            Node realParent = !options.getRelease().linksNestingLoopsUnderDependent() ? parent :
                    edge.isPeer() ? from.getResolveParent() : from;
            for (Node p = from.getResolveParent(); p != null; p = p.getResolveParent()) {
                // before npm 7.5.0 only the root is passed over, not every top
                boolean passedOver = options.getRelease().passesOverTopsInNestingLoops() ? p.isTop() : p.isRoot();
                if (p.matches(node) && !passedOver) {
                    return new Link(Node.options().release(options.getRelease()).parent(realParent).target(p).build());
                }
            }
        }

        Node src = parent.getSourceReference();
        peerSetSource.put(node, src);

        return loadPeerSet(node, required);
    }

    private Node virtualRoot(Node node, boolean reuse) {
        if (reuse && virtualRoots.containsKey(node)) {
            return virtualRoots.get(node);
        }

        // before npm 7.6.3 every virtual root stands at /virtual-root, with no copies of link targets
        boolean atSourcePath = options.getRelease().placesVirtualRootAtSourcePath();
        Node vr = new Node(Node.options()
                .release(options.getRelease())
                .path(atSourcePath ? node.getRealpath() : VIRTUAL_ROOT)
                .sourceReference(node)
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .overrides(node.getOverrides())
                .build());

        for (Node child : atSourcePath ? node.getChildren().values() : Collections.<Node>emptyList()) {
            if (child.isLink()) {
                new Node(Node.options()
                        .release(options.getRelease())
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
        // before npm 7.14.0 a workspace's bundle is left to it
        Object bd = node.isProjectRoot() || node.isWorkspace() && options.getRelease().buildsWhatWorkspacesBundle() ?
                null : node.getPackage().get("bundleDependencies");
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

            if (!edge.isValid() && (edge.getType() != Edge.Type.PEER_OPTIONAL || options.isSave() ||
                                    !options.getRelease().resolvesInvalidPeerOptionalBySave())) {
                problems.add(edge);
            }
        }
        return problems;
    }

    private @Nullable Double releaseAgeBefore(Spec spec) {
        Object before = options.before(now);
        if (before == null || options.getRelease().keepsNpmrcBeforeAString()) {
            // the string such a release keeps fails the fetcher before any date is read (Pacote#manifest)
            return null;
        }
        if (options.getRelease().exemptsFromReleaseAge() &&
            ReleaseAgeExclude.isReleaseAgeExcluded(ReleaseAgeExclude.trustedSpecName(spec),
                    options.getMinReleaseAgeExclude(), options.getRelease())) {
            return null;
        }
        if (before instanceof String) {
            throw new CannotKnow("the date before=" + before + " names (only V8's legacy date parser reads it)");
        }
        return (Double) before;
    }

    // parent: the node the manifest is fetched under, which is none for the pre-fetch of a placed node's problem edges
    private Map<String, Object> fetchManifest(Spec spec, @Nullable Node parent, @Nullable Edge edge) {
        NpmRelease release = options.getRelease();
        boolean isRoot;
        if (release.checksAllowedSpecTypes()) {
            checkAllow(spec, edge);
            Node from = edge == null ? null : edge.getFrom();
            isRoot = from != null && (from.isProjectRoot() || from.isWorkspace());
        } else {
            isRoot = release.fetchesAsRootUnderRootParent() && parent != null &&
                     (parent.isProjectRoot() || parent.isWorkspace());
        }
        Map<String, Object> fetchOptions = new LinkedHashMap<>();
        Pacote.Options.OptionsBuilder pacoteOptions = Pacote.Options.builder()
                .fullMetadata(options.getRelease().fetchesFullMetadata())
                .before(releaseAgeBefore(spec))
                .defaultTag(options.getTag())
                .nodeVersion(host.getNodeVersion())
                .npmVersion(options.getNpmVersion())
                .root(isRoot);
        spec = Objects.requireNonNull(Objects.requireNonNull(idealTree).getMeta()).checkYarnLock(spec, fetchOptions);

        Map<String, Object> cached = manifests.get(spec.getRaw());
        if (cached != null) {
            return cached;
        }
        Map<String, Object> mani = pacote.manifest(spec,
                pacoteOptions.integrity(fetchOptions.get("integrity")).build());
        if (options.getRelease().dropsLicenseOfFetchedManifest()) {
            // .then(({ license, ...mani }) => ...)
            mani = new LinkedHashMap<>(mani);
            mani.remove("license");
        }
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
                .release(options.getRelease())
                .name(name)
                .parent(parent)
                .error(error)
                .installLinks(options.isInstallLinks())
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .build());
        loadFailures.add(n);
        return n;
    }

    private @Nullable Node nodeFromSpec(String name, Spec spec, Node parent, @Nullable Edge edge) {
        if (options.getRelease().checksAllowedSpecTypes()) {
            try {
                checkAllow(spec, edge);
            } catch (NpmRejection error) {
                return failureNode(name, parent, error, edge);
            }
        }

        Node tree = Objects.requireNonNull(idealTree);
        boolean installLinks = options.isInstallLinks();
        boolean isWorkspace = tree.getWorkspaces() != null && tree.getWorkspaces().containsKey(spec.getName());

        NpmRelease release = options.getRelease();
        boolean isProjectInternalFileSpec = false;
        if (release.linksProjectInternalFileSpecs() && edge != null &&
            (edge.getRawSpec().startsWith("file:../") || edge.getRawSpec().startsWith("file:./"))) {
            String targetPath = PosixPath.resolve(parent.getRealpath(), edge.getRawSpec().substring(5));
            String resolvedProjectRoot = PosixPath.resolve(tree.getRealpath());
            isProjectInternalFileSpec = targetPath.startsWith(resolvedProjectRoot + "/") ||
                                        targetPath.equals(resolvedProjectRoot);
        }

        boolean parentWasInstalled = !parent.isLink() && truthy(parent.getResolved()) &&
                                     Js.string(parent.getResolved()).startsWith("file:");
        boolean isTransitiveFileDep = release.installsTransitiveFileDependencies() &&
                                      spec.getType() == Spec.Type.DIRECTORY && parentWasInstalled && installLinks;

        boolean shouldLink = (isWorkspace || isProjectInternalFileSpec || !installLinks) && !isTransitiveFileDep;
        if (spec.getType() == Spec.Type.DIRECTORY && shouldLink) {
            String realpath = Objects.requireNonNull(spec.getFetchSpec());
            Link link = new Link(Node.options()
                    .release(options.getRelease())
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

        if (isWorkspace && release.prefersSatisfyingWorkspace()) {
            Edge rootEdge = tree.getEdgesOut().get(spec.getName());
            Node existingNode = rootEdge == null ? null : rootEdge.getTo();
            if (existingNode != null && existingNode.isWorkspace() && edge != null && existingNode.satisfies(edge)) {
                // before npm 9.6.0, edge.to: what the edge resolves to now, which may be nothing
                return release.returnsSatisfyingWorkspace() ? existingNode : edge.getTo();
            }
        }

        if (isTransitiveFileDep && edge != null) {
            String parentOriginalPath = Js.string(parent.getResolved()).substring(5);
            String relativePath = edge.getRawSpec().substring(5);
            String absolutePath = PosixPath.resolve(parentOriginalPath, relativePath);
            spec = Spec.resolve(name, "file:" + absolutePath, null, options.getRelease().npmPackageArg());
        }

        Map<String, Object> pkg;
        try {
            pkg = fetchManifest(spec, parent, edge);
        } catch (CannotKnow | PackageJson.NeedsPackageFiles error) {
            throw error;
        } catch (RuntimeException error) {
            return failureNode(name, parent, error, edge);
        }
        if (options.getRelease().failsIncompleteManifests() && !truthy(pkg.get("version")) && spec.isRegistry()) {
            return failureNode(name, parent, new NpmRejection("EINCOMPLETEMANIFEST",
                    "incomplete manifest for " + name + ", missing version", null), edge);
        }
        // npm 12.0.0 extends a copy of the manifest before the node reads its edges; a rule that does not apply
        // fails the install, not the node
        PackageExtensions.Extended extended = packageExtensions == null ? null : packageExtensions.apply(pkg);
        Node node = new Node(Node.options()
                .name(name)
                .pkg(extended != null ? extended.getPkg() : pkg)
                .parent(parent)
                .installLinks(installLinks)
                .legacyPeerDeps(options.isLegacyPeerDeps())
                .release(options.getRelease())
                .build());
        if (extended != null) {
            node.setPackageExtensionsApplied(extended.getApplied());
        }
        return node;
    }

    // PackageJson.normalize(realpath).catch(() => ({ content: {} })) over the project's files
    private Map<String, Object> directoryManifest(String realpath, Spec spec) {
        if (!options.getRelease().placesVirtualRootAtSourcePath() &&
            (VIRTUAL_ROOT.equals(realpath) || realpath.startsWith(VIRTUAL_ROOT + "/"))) {
            // a directory spec of a node in a virtual root, which stands at /virtual-root before npm 7.6.3: npm
            // reads a package.json under that made-up path, which fails, and the link takes an empty package
            return new LinkedHashMap<>();
        }
        String location = Relpath.relpath(Objects.requireNonNull(idealTree).getRealpath(), realpath);
        PackageJson manifest;
        try {
            manifest = files.manifest(location);
        } catch (PackageJson.NeedsPackageFiles | CannotKnow e) {
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
        // before npm 7.5.0 only a non-optional peer edge that is invalid
        boolean loadsOptionalPeers = options.getRelease().loadsOptionalPeersIntoPeerSets();
        List<Edge> peerEdges = new ArrayList<>();
        for (Edge e : node.getEdgesOut().values()) {
            if (loadsOptionalPeers ? e.isPeer() && !(e.isValid() && e.getTo() != null) :
                    e.isPeer() && !e.isValid() && !e.isOptional()) {
                peerEdges.add(e);
            }
        }
        peerEdges.sort((a, b) -> LocaleCompare.en(a.getName(), b.getName()));

        for (Edge edge : peerEdges) {
            Node nodeParent = node.getParent();
            if (nodeParent == null && options.getRelease().stopsPeerSetOfDetachedNode()) {
                break;
            }
            if (edge.isValid() && (edge.getTo() != null || !loadsOptionalPeers)) {
                continue;
            }
            if (nodeParent == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'edgesOut')");
            }

            Edge parentEdge = nodeParent.getEdgesOut().get(edge.getName());
            Node sourceReference = Objects.requireNonNull(nodeParent.getSourceReference());
            boolean isMine = sourceReference.isProjectRoot() || sourceReference.isWorkspace();
            boolean conflictOK = options.isForce() || !isMine && !strictPeerDeps;

            if (edge.getTo() == null) {
                if (parentEdge == null) {
                    if (edge.getType() == Edge.Type.PEER_OPTIONAL &&
                        options.getRelease().peerSetHoldsOptionalPeerOnlyOnConflict()) {
                        Node current = sourceReference.resolve(edge.getName());
                        if (current == null || current.satisfies(edge)) {
                            continue;
                        }
                    }
                    nodeFromEdge(edge, nodeParent, null, required);
                    continue;
                } else {
                    // before npm 7.5.0 the parent's edge alone, with no second node by the peer's own spec
                    Node dep = nodeFromEdge(parentEdge, nodeParent,
                            options.getRelease().triesPeerSpecForParentEdge() ? edge : null, required);

                    if (edge.isValid()) {
                        continue;
                    }

                    // before npm 7.5.4 a conflict stands only where it is forced or not the project's
                    if (conflictOK || !required.contains(dep) && options.getRelease().letsUnrequiredPeersConflict()) {
                        if (options.getRelease().marksPeerConflictsOfLoadedPeerSet()) {
                            edge.setPeerConflicted(true);
                        }
                        continue;
                    }

                    if (node.getParent() == null && options.getRelease().skipsPeerConflictOfDetachedNode()) {
                        break;
                    }

                    throw failPeerConflict(edge, parentEdge);
                }
            }

            Node current = edge.getTo();
            if (options.getRelease().replacesPeerWithReplacement()) {
                Node dep = nodeFromEdge(edge, null, null, required);
                if (dep.canReplace(current)) {
                    nodeFromEdge(edge, nodeParent, null, required);
                    continue;
                }
            }

            // before npm 7.5.5 a conflict stands only where it is forced or not the project's
            if (conflictOK || !required.contains(edge.getFrom()) &&
                              options.getRelease().letsUnrequiredDependentsConflict()) {
                continue;
            }

            throw failPeerConflict(edge, parentEdge);
        }
        return node;
    }

    private PlaceDep.CouldNotResolve failPeerConflict(Edge edge, @Nullable Edge currentEdge) {
        if (!options.getRelease().placesWithPlaceDep()) {
            // explained with the last peer conflict the placement found where the edge resolves to nothing
            return legacyPlacement().failPeerConflict(edge, currentEdge);
        }
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
        if (!options.getRelease().skipsUnplacedLinks()) {
            resolveLinksOfArborist1(tree);
            return;
        }
        for (Node link : new ArrayList<>(linkNodes)) {
            linkNodes.remove(link);

            if (link.getRoot() != tree) {
                continue;
            }

            Node target = link.getTarget();
            boolean external;
            if (options.getRelease().readsExternalLinksByTarget()) {
                if (target == null) {
                    throw new IllegalArgumentException("Cannot read properties of null (reading 'isDescendantOf')");
                }
                external = !target.isDescendantOf(Objects.requireNonNull(tree.getTarget()));
            } else {
                // /^\.\.(\/|$)/.test(relpath(this.path, link.realpath))
                String loc = Relpath.relpath(path, link.getRealpath());
                external = loc.equals("..") || loc.startsWith("../");
            }

            if (external) {
                continue;
            }
            if (target == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'parent')");
            }

            boolean unseenLink = (target.getParent() != null || target.getFsParent() != null) &&
                                 !depsSeen.contains(target);

            if (unseenLink) {
                if (link.getOverrides() != null && options.getRelease().forwardsLinkOverridesToSubtree()) {
                    target.updateOverridesEdgeInAdded(link.getOverrides());
                }
                depsQueue.push(target);
            }
        }
    }

    // [_resolveLinks] of arborist 1 (npm 7.1.0), whose root setter gives a link target no fsParent: the node the
    // inventory holds at a link's realpath becomes its target, and a target with neither parent nor fsParent takes the
    // first node of the tree walking up from it as fsParent and is queued
    private void resolveLinksOfArborist1(Node tree) {
        NpmRelease release = options.getRelease();
        boolean walksUpRealpath = release.walksUpRealpathOfLinkTargets();
        for (Node link : new ArrayList<>(linkNodes)) {
            linkNodes.remove(link);
            String realpath = Js.string(link.getRealpath());
            String loc = Relpath.relpath(path, realpath);
            Node fromInv = tree.getInventory().get(loc);
            if (fromInv != null && fromInv != link.getTarget()) {
                ((Link) link).setTarget(fromInv);
            }

            boolean external = loc.equals("..") || loc.startsWith("../");

            // outside the root, somebody else's problem, ignore it: before npm 7.0.10, before the fsParent is found
            if (external && !walksUpRealpath) {
                continue;
            }

            if (targetOf(link).getParent() == null && targetOf(link).getFsParent() == null) {
                // the fsParent is likely some node in the tree, possibly the root: where its deps may be installed
                if (walksUpRealpath) {
                    for (String p : PosixPath.walkUp(PosixPath.dirname(realpath))) {
                        String ploc = Relpath.relpath(path, p);
                        Node node = ploc.isEmpty() ? tree : tree.getInventory().get(ploc);
                        if (node != null) {
                            targetOf(link).setFsParent(node);
                            depsQueue.push(targetOf(link));
                            break;
                        }
                    }
                } else {
                    List<String> parts = Arrays.asList(loc.split("/", -1));
                    for (int p = parts.size() - 1; p > -1; p--) {
                        String ploc = String.join("/", parts.subList(0, p));
                        if (ploc.isEmpty() && external) {
                            break;
                        }
                        Node node = ploc.isEmpty() ? tree : tree.getInventory().get(ploc);
                        if (node != null) {
                            targetOf(link).setFsParent(node);
                            depsQueue.push(targetOf(link));
                            p = -1;
                        }
                    }
                }
            }

            if (external) {
                continue;
            }

            // before npm 7.0.9 a target is queued only where the walk up gives it an fsParent
            Node target = targetOf(link);
            boolean unseenLink = (target.getParent() != null || target.getFsParent() != null) &&
                                 !depsSeen.contains(target);
            if (unseenLink && release.resolvesLinksOfDependencies()) {
                depsQueue.push(target);
            }
        }
    }

    // link.target, read where a missing one is a TypeError
    private static Node targetOf(Node link) {
        Node target = link.getTarget();
        if (target == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'parent')");
        }
        return target;
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
        boolean prunesOptionalPeers = options.getRelease().prunesOptionalPeers();
        for (Node node : Objects.requireNonNull(idealTree).getInventory().values()) {
            if (node.isExtraneous() || prunesOptionalPeers && node.isPeer() && node.isOptional()) {
                node.setParent(null);
            }
        }
    }

    /**
     * Everything the rest of the loop reads, as values: the tree with its edges, which of its nodes were built and
     * which wait to be, the virtual roots kept to place peers from, the links and load failures, and what the lock
     * gives a node placed at a location. A node no longer in the tree is skipped when its turn comes, and a node
     * already built is built again only when it is queued again, so the queue counts by the nodes of the tree it
     * holds that are not built yet.
     */
    private List<Object> loopState() {
        Node tree = Objects.requireNonNull(idealTree);
        List<Object> state = new ArrayList<>();
        state.add(treeState(tree));
        state.add(locations(depsSeen, tree));
        List<Node> waiting = new ArrayList<>();
        for (Node node : depsQueue.items()) {
            if (!depsSeen.contains(node)) {
                waiting.add(node);
            }
        }
        state.add(locations(waiting, tree));
        Map<String, Object> roots = new LinkedHashMap<>();
        for (Map.Entry<Node, Node> vr : virtualRoots.entrySet()) {
            if (vr.getKey().getRoot() == tree) {
                roots.put(vr.getKey().getLocation(), treeState(vr.getValue()));
            }
        }
        state.add(roots);
        state.add(locations(linkNodes, tree));
        state.add(locations(loadFailures, tree));
        state.add(Objects.requireNonNull(tree.getMeta()).placementState());
        return state;
    }

    private static Set<String> locations(Iterable<Node> nodes, Node tree) {
        Set<String> locations = new TreeSet<>();
        for (Node node : nodes) {
            if (node.getRoot() == tree) {
                locations.add(node.getLocation());
            }
        }
        return locations;
    }

    // each node of a tree by location, with what placement reads of it
    private static Map<String, List<Object>> treeState(Node root) {
        Map<String, List<Object>> nodes = new TreeMap<>();
        for (Node node : root.getInventory().values()) {
            List<Object> edges = new ArrayList<>();
            for (Edge edge : node.getEdgesOut().values()) {
                Node to = edge.getTo();
                edges.add(Arrays.asList(edge.getName(), edge.getType(), edge.getRawSpec(), edge.getSpec(),
                        to == null ? null : to.getLocation(), edge.getError(), edge.isPeerConflicted()));
            }
            List<String> errors = new ArrayList<>();
            for (Throwable error : node.getErrors()) {
                errors.add(String.valueOf(error.getMessage()));
            }
            Node target = node.isLink() ? node.getTarget() : null;
            nodes.put(node.getLocation(), Arrays.asList(node.getName(), node.isLink(),
                    target == null ? null : target.getLocation(), node.getPackage(), node.getResolved(),
                    node.getIntegrity(), errors, edges));
        }
        return nodes;
    }

    private void pruneFailedOptional() {
        boolean keepsInert = options.getRelease().keepsFailedOptionalInert();
        for (Node node : loadFailures) {
            if (!node.isOptional()) {
                Throwable error = node.getErrors().get(0);
                throw error instanceof RuntimeException ? (RuntimeException) error : new IllegalStateException(error);
            }

            for (Node n : OptionalSet.optionalSet(node)) {
                if (keepsInert) {
                    n.setInert(true);
                } else {
                    n.setParent(null);
                }
            }
        }
    }
}
