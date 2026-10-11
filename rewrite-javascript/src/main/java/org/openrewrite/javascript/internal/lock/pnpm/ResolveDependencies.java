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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EventLoop;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Port of @pnpm/installing.deps-resolver src/resolveDependencies.ts (pnpm 11.2.2) with the small modules it calls:
 * getNonDevWantedDependencies, hoistPeers, replaceVersionInBareSpecifier, unwrapPackageName and their siblings.
 */
final class ResolveDependencies {

    private static final Set<String> NON_EXOTIC_RESOLVED_VIA = new LinkedHashSet<>(Arrays.asList(
            "custom-resolver", "github.com/denoland/deno", "github.com/oven-sh/bun", "jsr-registry",
            "local-filesystem", "named-registry", "nodejs.org", "npm-registry", "workspace"));

    private ResolveDependencies() {
    }

    /**
     * A dependency a manifest wants, by alias.
     */
    @Getter
    @Setter
    static final class WantedDependency {
        private String alias;
        private String bareSpecifier;
        private boolean dev;
        private boolean optional;
        private @Nullable Boolean injected;
        private @Nullable String saveCatalogName;
        private @Nullable String prevSpecifier;
        private boolean updateSpec;
        private boolean isNew;
        private @Nullable Integer updateDepth;

        WantedDependency(String alias, String bareSpecifier, boolean dev, boolean optional) {
            this.alias = alias;
            this.bareSpecifier = bareSpecifier;
            this.dev = dev;
            this.optional = optional;
        }

        WantedDependency copy() {
            WantedDependency copy = new WantedDependency(alias, bareSpecifier, dev, optional);
            copy.injected = injected;
            copy.saveCatalogName = saveCatalogName;
            copy.prevSpecifier = prevSpecifier;
            copy.updateSpec = updateSpec;
            copy.isNew = isNew;
            copy.updateDepth = updateDepth;
            return copy;
        }
    }

    /**
     * A peer a package declares: its range, and whether {@code peerDependenciesMeta} makes it optional.
     */
    @Getter
    @Setter
    static final class PeerDependency {
        private final String version;
        private @Nullable Boolean optional;

        PeerDependency(String version) {
            this.version = version;
        }
    }

    @Value
    static class MissingPeerInfo {
        String range;
        boolean optional;
    }

    /**
     * {@code NodeId}: a leaf's is its package id, a link's its {@code link:} path, every other node's a number.
     */
    @Value
    static class NodeId {
        Object id;

        static NodeId of(String pkgId) {
            return new NodeId(pkgId);
        }

        static NodeId of(long counter) {
            return new NodeId(counter);
        }

        boolean isLink() {
            return id instanceof String && ((String) id).startsWith("link:");
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    /**
     * One {@code name@version} as resolved: its resolution, its normalized peers and the manifest fields the lock
     * records.
     */
    @Getter
    @Setter
    static final class ResolvedPackage {
        private final String id;
        private boolean isLeaf;
        private Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
        private boolean prod;
        private boolean dev;
        private boolean optional;
        private final String name;
        private final String version;
        private Map<String, PeerDependency> peerDependencies = new LinkedHashMap<>();
        private Set<String> optionalDependencies = new LinkedHashSet<>();
        private boolean hasBin;
        private boolean hasBundledDependencies;
        private boolean prepare;
        private String pkgIdWithPatchHash;
        private PatchingConfig.@Nullable PatchInfo patch;

        /**
         * {@code additionalInfo}: deprecated, bundleDependencies, bundledDependencies, engines, cpu, os, libc.
         */
        private Map<String, @Nullable Object> additionalInfo = new LinkedHashMap<>();

        ResolvedPackage(String id, String name, String version) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.pkgIdWithPatchHash = id;
        }
    }

    @Value
    static class CatalogLookup {
        String catalogName;
        String specifier;
        String userSpecifiedBareSpecifier;
    }

    /**
     * {@code PkgAddress} or, with {@link #isLinkedDependency}, a {@code LinkedDependency}.
     */
    @Getter
    @Setter
    static final class PkgAddress {
        private final String alias;
        private final String pkgId;
        private boolean optional;
        private Map<String, @Nullable Object> pkg = new LinkedHashMap<>();
        private @Nullable String normalizedBareSpecifier;
        private @Nullable CatalogLookup catalogLookup;
        private @Nullable NodeId nodeId;
        private boolean depIsLinked;
        private boolean isNew;
        private boolean isLinkedDependency;
        private @Nullable String resolvedVia;
        private boolean installable = true;
        private @Nullable String version;
        private boolean updated;
        private @Nullable String rootDir;
        private Map<String, MissingPeerInfo> missingPeers = new LinkedHashMap<>();
        private @Nullable MissingPeersOfChildren missingPeersOfChildren;
        private @Nullable String publishedAt;
        private @Nullable String saveCatalogName;
        private boolean dev;
        private @Nullable Map<String, @Nullable Object> resolution;
        private @Nullable String name;

        PkgAddress(String alias, String pkgId) {
            this.alias = alias;
            this.pkgId = pkgId;
        }
    }

    /**
     * The missing peers of a package's children, known once its children are resolved (a deferred promise).
     */
    static final class MissingPeersOfChildren {
        private final Promise<Map<String, MissingPeerInfo>> promise;
        private boolean resolved;

        MissingPeersOfChildren(EventLoop loop) {
            this.promise = new Promise<>(loop);
        }

        void resolve(Map<String, MissingPeerInfo> missingPeers) {
            resolved = true;
            promise.resolve(missingPeers);
        }

        boolean isResolved() {
            return resolved;
        }

        Promise<Map<String, MissingPeerInfo>> get() {
            return promise;
        }
    }

    /**
     * {@code missingPeersOfChildrenByPkgId}'s entry: the depth a package was first reached at, and its children's
     * missing peers.
     */
    @Value
    static class MissingPeersOfChildrenAtDepth {
        int depth;
        MissingPeersOfChildren missingPeersOfChildren;
    }

    /**
     * {@code ChildrenByParentId}'s entry: a child's alias and package id.
     */
    @Value
    static class ChildId {
        String alias;
        String id;
    }

    /**
     * What resolveDependenciesOfImporters returns.
     */
    @Value
    static class ImportersResolution {
        List<PkgAddressesByImporter> pkgAddressesByImportersWithoutPeers;

        @Nullable
        Instant publishedBy;

        @Nullable
        Map<String, String> time;
    }

    @Value
    static class PendingNode {
        String alias;
        NodeId nodeId;
        ResolvedPackage resolvedPackage;
        int depth;
        boolean installable;
        List<String> parentIds;
    }

    /**
     * {@code DependenciesTreeNode}: the children by alias (computed when first asked for), installability, the package
     * and the depth it was first reached at ({@code -1} for a link).
     */
    @Getter
    @Setter
    static final class DependenciesTreeNode {
        private @Nullable Supplier<Map<String, NodeId>> lazyChildren;
        private @Nullable Map<String, NodeId> children;
        private boolean installable;
        private final ResolvedPackage resolvedPackage;
        private int depth;

        DependenciesTreeNode(ResolvedPackage resolvedPackage, int depth) {
            this.resolvedPackage = resolvedPackage;
            this.depth = depth;
        }

        Map<String, NodeId> children() {
            if (children == null) {
                children = lazyChildren == null ? new LinkedHashMap<>() : lazyChildren.get();
                lazyChildren = null;
            }
            return children;
        }
    }

    /**
     * {@code ParentPkgAliases}: the aliases above a node, each the edge that resolved it or {@code true}.
     */
    static final class ParentPkgAliases extends LinkedHashMap<String, Object> {
        ParentPkgAliases() {
        }

        ParentPkgAliases(Map<String, Object> from) {
            super(from);
        }
    }

    @Value
    static class ParentPkg {
        @Nullable
        NodeId nodeId;

        boolean installable;

        String rootDir;

        boolean optional;

        String pkgId;

        @Nullable
        String resolvedVia;
    }

    @Value
    static class PeersResolutionResult {
        Map<String, MissingPeerInfo> missingPeers;
        Map<String, PkgAddress> resolvedPeers;
    }

    @Value
    static class PostponedResolutionOpts {
        PreferredVersions.ByName preferredVersions;
        ParentPkgAliases parentPkgAliases;

        @Nullable
        Instant publishedBy;
    }

    /**
     * {@code ResolvedDependenciesOptions}, also the importer's own options (without its parent aliases).
     */
    @Getter
    @Setter
    static final class ResolvedDependenciesOptions {
        private int currentDepth;
        private List<String> parentIds = new ArrayList<>();
        private ParentPkg parentPkg;
        private ParentPkgAliases parentPkgAliases = new ParentPkgAliases();
        private @Nullable Map<String, @Nullable String> preferredDependencies;
        private boolean proceed;
        private @Nullable Instant publishedBy;
        private boolean pickLowestVersion;
        private @Nullable Map<String, @Nullable String> resolvedDependencies;
        private int updateDepth;
        private String prefix = "";
        private boolean updateToLatest;
        private @Nullable String pinnedVersion;

        ResolvedDependenciesOptions(ParentPkg parentPkg) {
            this.parentPkg = parentPkg;
        }

        ResolvedDependenciesOptions copy() {
            ResolvedDependenciesOptions copy = new ResolvedDependenciesOptions(parentPkg);
            copy.currentDepth = currentDepth;
            copy.parentIds = parentIds;
            copy.parentPkgAliases = parentPkgAliases;
            copy.preferredDependencies = preferredDependencies;
            copy.proceed = proceed;
            copy.publishedBy = publishedBy;
            copy.pickLowestVersion = pickLowestVersion;
            copy.resolvedDependencies = resolvedDependencies;
            copy.updateDepth = updateDepth;
            copy.prefix = prefix;
            copy.updateToLatest = updateToLatest;
            copy.pinnedVersion = pinnedVersion;
            return copy;
        }
    }

    @Getter
    @Setter
    static final class ImporterToResolve {
        private boolean updatePackageManifest;
        private PreferredVersions.ByName preferredVersions;
        private ParentPkgAliases parentPkgAliases;
        private List<WantedDependency> wantedDependencies;
        private ResolvedDependenciesOptions options;
        private @Nullable String pinnedVersion;

        ImporterToResolve(PreferredVersions.ByName preferredVersions, ParentPkgAliases parentPkgAliases,
                          List<WantedDependency> wantedDependencies, ResolvedDependenciesOptions options) {
            this.preferredVersions = preferredVersions;
            this.parentPkgAliases = parentPkgAliases;
            this.wantedDependencies = wantedDependencies;
            this.options = options;
        }
    }

    @Getter
    @Setter
    static final class InfoFromLockfile {
        private final String pkgId;
        private @Nullable PackageSnapshot dependencyLockfile;
        private @Nullable String name;
        private @Nullable String version;
        private @Nullable Map<String, @Nullable Object> resolution;

        InfoFromLockfile(String pkgId) {
            this.pkgId = pkgId;
        }
    }

    @Getter
    @Setter
    static final class ExtendedWantedDependency {
        private @Nullable InfoFromLockfile infoFromLockfile;
        private @Nullable String preferredVersion;
        private boolean proceed;
        private final WantedDependency wantedDependency;

        ExtendedWantedDependency(@Nullable InfoFromLockfile infoFromLockfile, boolean proceed,
                                 WantedDependency wantedDependency) {
            this.infoFromLockfile = infoFromLockfile;
            this.proceed = proceed;
            this.wantedDependency = wantedDependency;
        }
    }

    /**
     * {@code ResolveDependenciesOfDependency}: the edge, and what resolves its children and the peers they miss.
     */
    @Value
    static class DependencyResolution {
        @Nullable
        PkgAddress resolveDependencyResult;

        @Nullable
        Function<PostponedResolutionOpts, Promise<PeersResolutionResult>> postponedResolution;

        @Nullable
        Function<ParentPkgAliases, Promise<PeersResolutionResult>> postponedPeersResolution;
    }

    @Value
    static class ResolvedDependenciesResult {
        List<PkgAddress> pkgAddresses;
        Promise<PeersResolutionResult> resolvingPeers;
    }

    /**
     * What resolving one importer's direct dependencies leaves for resolveDependenciesOfImporters' second step.
     */
    @Value
    static class ImporterDependencies {
        List<PkgAddress> pkgAddresses;
        List<Function<PostponedResolutionOpts, Promise<PeersResolutionResult>>> postponedResolutionsQueue;
        List<Function<ParentPkgAliases, Promise<PeersResolutionResult>>> postponedPeersResolutionQueue;
    }

    @Getter
    static final class PkgAddressesByImporter {
        private final List<PkgAddress> pkgAddresses;
        @Setter
        private Map<String, MissingPeerInfo> missingPeers;
        @Setter
        private Map<String, PkgAddress> resolvedPeers;

        PkgAddressesByImporter(List<PkgAddress> pkgAddresses, Map<String, MissingPeerInfo> missingPeers,
                               Map<String, PkgAddress> resolvedPeers) {
            this.pkgAddresses = pkgAddresses;
            this.missingPeers = missingPeers;
            this.resolvedPeers = resolvedPeers;
        }
    }

    @Value
    static class RootResult {
        List<List<PkgAddress>> pkgAddressesByImporters;

        @Nullable
        Map<String, String> time;
    }

    @Value
    static class PolicyViolation {
        String name;
        String version;
        String code;
        String reason;
    }

    /**
     * {@code ResolutionContext}: what one resolution of every importer shares.
     */
    @Getter
    @Setter
    static final class ResolutionContext {
        private boolean autoInstallPeers;
        private boolean autoInstallPeersFromHighestMatch;
        private @Nullable Map<String, Map<String, PreferredVersions.Selector>> allPreferredVersions;
        private final Set<String> updatedSet = new LinkedHashSet<>();
        private Map<String, @Nullable Map<String, @Nullable Object>> catalogs = new LinkedHashMap<>();
        private String defaultTag = "latest";
        private boolean forceFullResolution;
        private final Map<String, ResolvedPackage> resolvedPkgsById = new LinkedHashMap<>();
        private boolean resolvePeersFromWorkspaceRoot;
        private final Map<String, List<ChildId>> childrenByParentId = new LinkedHashMap<>();
        private final List<PendingNode> pendingNodes = new ArrayList<>();
        private LockfileObject wantedLockfile;
        private boolean injectWorkspacePackages;
        private double linkWorkspacePackagesDepth = -1;
        private String lockfileDir;
        private PackageRequester storeController;
        private final Set<String> skipped = new LinkedHashSet<>();
        private final Map<NodeId, DependenciesTreeNode> dependenciesTree = new LinkedHashMap<>();
        private boolean preferWorkspacePackages;
        private @Nullable ReadPackageHook readPackageHook;
        private Map<String, String> registries = new LinkedHashMap<>();
        private List<String> namedRegistryPrefixes = new ArrayList<>();
        private @Nullable String resolutionMode;
        private @Nullable Map<String, Map<String, Verification.Project>> workspacePackages;
        private final Map<String, MissingPeersOfChildrenAtDepth> missingPeersOfChildrenByPkgId =
                new LinkedHashMap<>();
        private boolean hoistPeers;
        private @Nullable Instant maximumPublishedBy;
        private @Nullable Function<String, @Nullable Object> publishedByExclude;
        private @Nullable String trustPolicy;
        private @Nullable Function<String, Object> trustPolicyExclude;
        private @Nullable Double trustPolicyIgnoreAfter;
        private final List<PolicyViolation> resolutionPolicyViolations = new ArrayList<>();
        private boolean blockExoticSubdeps;
        private final Set<String> allPeerDepNames = new LinkedHashSet<>();
        private @Nullable Map<String, PatchingConfig.PatchGroup> patchedDependencies;
        private final Set<String> appliedPatches = new LinkedHashSet<>();
        private final EventLoop loop = new EventLoop();
        private long nodeIdCounter;

        ResolutionContext(LockfileObject wantedLockfile, String lockfileDir, PackageRequester storeController) {
            this.wantedLockfile = wantedLockfile;
            this.lockfileDir = lockfileDir;
            this.storeController = storeController;
        }

        NodeId nextNodeId() {
            return NodeId.of(++nodeIdCounter);
        }
    }

    static Promise<RootResult> resolveRootDependencies(ResolutionContext ctx, List<ImporterToResolve> importers) {
        if (ctx.isAutoInstallPeers()) {
            ctx.setAllPreferredVersions(PreferredVersions.getPreferredVersionsFromLockfileAndManifests(
                    ctx.getWantedLockfile().getPackages(), Collections.emptyList()));
        } else if (ctx.isHoistPeers()) {
            ctx.setAllPreferredVersions(new LinkedHashMap<>());
        }
        return resolveDependenciesOfImporters(ctx, importers).then(ofImporters -> {
            List<PkgAddressesByImporter> byImporters = ofImporters.getPkgAddressesByImportersWithoutPeers();
            Map<String, String> time = ofImporters.getTime();
            if (!ctx.isHoistPeers()) {
                return Promise.resolved(ctx.getLoop(), new RootResult(addressesOf(byImporters), time));
            }
            List<PkgAddress> workspaceRootDeps = new ArrayList<>();
            if (ctx.isResolvePeersFromWorkspaceRoot()) {
                for (int i = 0; i < importers.size(); i++) {
                    if (".".equals(importers.get(i).getOptions().getParentIds().get(0))) {
                        workspaceRootDeps = byImporters.get(i).getPkgAddresses();
                        break;
                    }
                }
            }
            return hoistMissingPeers(ctx, importers, byImporters, workspaceRootDeps, ofImporters.getPublishedBy())
                    .then(done -> Promise.resolved(ctx.getLoop(), new RootResult(addressesOf(byImporters), time)));
        });
    }

    // resolveRootDependencies' while (true): every importer's required peers, then the optional peers it can hoist
    private static Promise<Void> hoistMissingPeers(ResolutionContext ctx, List<ImporterToResolve> importers,
                                                   List<PkgAddressesByImporter> byImporters,
                                                   List<PkgAddress> workspaceRootDeps,
                                                   @Nullable Instant publishedBy) {
        List<Promise<Map<String, List<String>>>> byImporter = new ArrayList<>();
        for (int index = 0; index < byImporters.size(); index++) {
            byImporter.add(hoistRequiredPeers(ctx, importers.get(index), byImporters.get(index), workspaceRootDeps,
                    publishedBy, new LinkedHashMap<>()));
        }
        return Promise.all(ctx.getLoop(), byImporter).then(allMissingOptionalPeersByImporters -> {
            boolean[] hasNewMissingPeers = {false};
            List<Promise<Void>> hoisting = new ArrayList<>();
            for (int index = 0; index < allMissingOptionalPeersByImporters.size(); index++) {
                Map<String, List<String>> allMissingOptionalPeers = allMissingOptionalPeersByImporters.get(index);
                ImporterToResolve importer = importers.get(index);
                PkgAddressesByImporter result = byImporters.get(index);
                hoisting.add(Promise.async(ctx.getLoop(), () -> {
                    if (allMissingOptionalPeers.isEmpty() || ctx.getAllPreferredVersions() == null) {
                        return Promise.resolved(ctx.getLoop(), null);
                    }
                    Map<String, String> optionalDependencies = getHoistableOptionalPeers(allMissingOptionalPeers,
                            ctx.getAllPreferredVersions());
                    if (optionalDependencies.isEmpty()) {
                        return Promise.resolved(ctx.getLoop(), null);
                    }
                    hasNewMissingPeers[0] = true;
                    List<WantedDependency> wantedDependencies = getNonDevWantedDependencies(
                            Collections.<String, @Nullable Object>singletonMap("optionalDependencies",
                                    optionalDependencies));
                    ResolvedDependenciesOptions options = importer.getOptions().copy();
                    options.setParentPkgAliases(importer.getParentPkgAliases());
                    options.setPublishedBy(publishedBy);
                    options.setUpdateToLatest(false);
                    return resolveDependencies(ctx, importer.getPreferredVersions(), wantedDependencies, options)
                            .then(resolved -> {
                                result.getPkgAddresses().addAll(resolved.getPkgAddresses());
                                return resolved.getResolvingPeers().then(peers -> {
                                    PeersResolutionResult filtered = filterMissingPeers(peers,
                                            importer.getParentPkgAliases());
                                    result.setMissingPeers(filtered.getMissingPeers());
                                    result.setResolvedPeers(filtered.getResolvedPeers());
                                    return Promise.resolved(ctx.getLoop(), null);
                                });
                            });
                }));
            }
            return Promise.all(ctx.getLoop(), hoisting).then(done -> hasNewMissingPeers[0] ?
                    hoistMissingPeers(ctx, importers, byImporters, workspaceRootDeps, publishedBy) :
                    Promise.resolved(ctx.getLoop(), null));
        });
    }

    // the inner while (true) of one importer: hoist its missing required peers until none is left to hoist
    private static Promise<Map<String, List<String>>> hoistRequiredPeers(
            ResolutionContext ctx, ImporterToResolve importer, PkgAddressesByImporter result,
            List<PkgAddress> workspaceRootDeps, @Nullable Instant publishedBy,
            Map<String, List<String>> allMissingOptionalPeers) {
        return Promise.async(ctx.getLoop(), () -> {
            ParentPkgAliases parentPkgAliases = importer.getParentPkgAliases();
            for (PkgAddress pkgAddress : result.getPkgAddresses()) {
                parentPkgAliases.put(pkgAddress.getAlias(), Boolean.TRUE);
            }
            List<Map.Entry<String, MissingPeerInfo>> missingOptionalPeers = new ArrayList<>();
            List<Map.Entry<String, MissingPeerInfo>> missingRequiredPeers = new ArrayList<>();
            for (Map.Entry<String, MissingPeerInfo> peer : result.getMissingPeers().entrySet()) {
                if (peer.getValue().isOptional()) {
                    missingOptionalPeers.add(peer);
                } else {
                    missingRequiredPeers.add(peer);
                    parentPkgAliases.put(peer.getKey(), Boolean.TRUE);
                }
            }
            if (ctx.isAutoInstallPeers()) {
                for (Map.Entry<String, PkgAddress> resolved : result.getResolvedPeers().entrySet()) {
                    if (!Js.truthy(parentPkgAliases.get(resolved.getKey()))) {
                        result.getPkgAddresses().add(resolved.getValue());
                    }
                }
            }
            for (Map.Entry<String, MissingPeerInfo> optional : missingOptionalPeers) {
                List<String> ranges = allMissingOptionalPeers.get(optional.getKey());
                if (ranges == null) {
                    allMissingOptionalPeers.put(optional.getKey(),
                            new ArrayList<>(Collections.singletonList(optional.getValue().getRange())));
                } else if (!ranges.contains(optional.getValue().getRange())) {
                    ranges.add(optional.getValue().getRange());
                }
            }
            if (missingRequiredPeers.isEmpty()) {
                return Promise.resolved(ctx.getLoop(), allMissingOptionalPeers);
            }
            Map<String, String> dependencies = hoistPeers(ctx.isAutoInstallPeers(), ctx.getAllPreferredVersions(),
                    workspaceRootDeps, missingRequiredPeers);
            if (dependencies.isEmpty()) {
                return Promise.resolved(ctx.getLoop(), allMissingOptionalPeers);
            }
            List<WantedDependency> wantedDependencies = getNonDevWantedDependencies(
                    Collections.<String, @Nullable Object>singletonMap("dependencies", dependencies));
            ResolvedDependenciesOptions options = importer.getOptions().copy();
            options.setParentPkgAliases(parentPkgAliases);
            options.setPublishedBy(publishedBy);
            options.setUpdateToLatest(false);
            return resolveDependencies(ctx, importer.getPreferredVersions(), wantedDependencies, options)
                    .then(resolved -> {
                        result.getPkgAddresses().addAll(resolved.getPkgAddresses());
                        return resolved.getResolvingPeers().then(peers -> {
                            PeersResolutionResult filtered = filterMissingPeers(peers, parentPkgAliases);
                            result.setMissingPeers(filtered.getMissingPeers());
                            result.setResolvedPeers(filtered.getResolvedPeers());
                            return hoistRequiredPeers(ctx, importer, result, workspaceRootDeps, publishedBy,
                                    allMissingOptionalPeers);
                        });
                    });
        });
    }

    private static List<List<PkgAddress>> addressesOf(List<PkgAddressesByImporter> byImporters) {
        List<List<PkgAddress>> out = new ArrayList<>();
        for (PkgAddressesByImporter importer : byImporters) {
            out.add(importer.getPkgAddresses());
        }
        return out;
    }

    private static Promise<ImportersResolution> resolveDependenciesOfImporters(ResolutionContext ctx,
                                                                               List<ImporterToResolve> importers) {
        boolean pickLowestVersion = "time-based".equals(ctx.getResolutionMode()) ||
                                    "lowest-direct".equals(ctx.getResolutionMode());
        List<Promise<ImporterDependencies>> resolving = new ArrayList<>();
        for (ImporterToResolve importer : importers) {
            resolving.add(Promise.async(ctx.getLoop(), () -> {
                List<ExtendedWantedDependency> extendedWantedDeps = getDepsToResolve(
                        importer.getWantedDependencies(), ctx.getWantedLockfile(), ctx.getRegistries(),
                        importer.getOptions().getPreferredDependencies(),
                        importer.getOptions().isProceed() || ctx.isForceFullResolution(),
                        importer.getOptions().getResolvedDependencies());
                List<Promise<DependencyResolution>> dependencies = new ArrayList<>();
                for (ExtendedWantedDependency extendedWantedDep : extendedWantedDeps) {
                    dependencies.add(Promise.async(ctx.getLoop(), () -> resolveDependenciesOfImporterDependency(ctx,
                            importer, pickLowestVersion, extendedWantedDep)));
                }
                return Promise.all(ctx.getLoop(), dependencies).then(resolved -> {
                    List<PkgAddress> pkgAddresses = new ArrayList<>();
                    List<Function<PostponedResolutionOpts, Promise<PeersResolutionResult>>> postponed =
                            new ArrayList<>();
                    List<Function<ParentPkgAliases, Promise<PeersResolutionResult>>> postponedPeers =
                            new ArrayList<>();
                    for (DependencyResolution result : resolved) {
                        if (result.getResolveDependencyResult() != null) {
                            pkgAddresses.add(result.getResolveDependencyResult());
                        }
                        if (result.getPostponedResolution() != null) {
                            postponed.add(result.getPostponedResolution());
                        }
                        if (result.getPostponedPeersResolution() != null) {
                            postponedPeers.add(result.getPostponedPeersResolution());
                        }
                    }
                    return Promise.resolved(ctx.getLoop(),
                            new ImporterDependencies(pkgAddresses, postponed, postponedPeers));
                });
            }));
        }
        return Promise.all(ctx.getLoop(), resolving).then(resolveResults ->
                resolvePostponedOfImporters(ctx, importers, resolveResults));
    }

    private static Promise<ImportersResolution> resolvePostponedOfImporters(
            ResolutionContext ctx, List<ImporterToResolve> importers, List<ImporterDependencies> resolveResults) {
        List<List<PkgAddress>> addressesByImporter = new ArrayList<>();
        for (ImporterDependencies result : resolveResults) {
            addressesByImporter.add(result.getPkgAddresses());
        }
        Instant publishedBy = null;
        Map<String, String> time = null;
        if ("time-based".equals(ctx.getResolutionMode())) {
            List<PkgAddress> all = new ArrayList<>();
            for (List<PkgAddress> addresses : addressesByImporter) {
                all.addAll(addresses);
            }
            Object time0 = ctx.getWantedLockfile().getTime();
            @SuppressWarnings("unchecked")
            Map<String, @Nullable Object> timeFromLockfile = time0 instanceof Map ?
                    (Map<String, @Nullable Object>) time0 : Collections.<String, @Nullable Object>emptyMap();
            Map<String, String> newTime = new LinkedHashMap<>();
            for (PkgAddress pkgAddress : all) {
                if (pkgAddress.getPublishedAt() != null && !pkgAddress.getPublishedAt().isEmpty()) {
                    newTime.put(pkgAddress.getPkgId(), pkgAddress.getPublishedAt());
                } else if (Js.truthy(timeFromLockfile.get(pkgAddress.getPkgId()))) {
                    newTime.put(pkgAddress.getPkgId(), Js.string(timeFromLockfile.get(pkgAddress.getPkgId())));
                }
            }
            double latest = Double.NaN;
            for (String publishedAt : newTime.values()) {
                double t = V8Date.dateParse(publishedAt);
                if (Double.isNaN(latest) || t > latest) {
                    latest = t;
                }
            }
            if (!newTime.isEmpty()) {
                if (Double.isNaN(latest)) {
                    throw new CannotKnow("the time-based cutoff (a publish time in the lock does not parse)");
                }
                publishedBy = Instant.ofEpochMilli((long) latest + 60 * 60 * 1000);
                time = newTime;
            }
        }
        if (ctx.getMaximumPublishedBy() != null &&
            (publishedBy == null || publishedBy.isAfter(ctx.getMaximumPublishedBy()))) {
            publishedBy = ctx.getMaximumPublishedBy();
        }
        Instant cutoff = publishedBy;
        Map<String, String> newTime = time;
        List<Promise<PkgAddressesByImporter>> byImporters = new ArrayList<>();
        for (int i = 0; i < importers.size(); i++) {
            ImporterToResolve importer = importers.get(i);
            ImporterDependencies resolveResult = resolveResults.get(i);
            byImporters.add(Promise.async(ctx.getLoop(), () ->
                    resolvePostponedOfImporter(ctx, importer, resolveResult, cutoff)));
        }
        return Promise.all(ctx.getLoop(), byImporters).then(out ->
                Promise.resolved(ctx.getLoop(), new ImportersResolution(out, cutoff, newTime)));
    }

    private static Promise<PkgAddressesByImporter> resolvePostponedOfImporter(
            ResolutionContext ctx, ImporterToResolve importer, ImporterDependencies resolveResult,
            @Nullable Instant publishedBy) {
        List<PkgAddress> pkgAddresses = resolveResult.getPkgAddresses();
        PreferredVersions.ByName newPreferredVersions = new PreferredVersions.ByName(new LinkedHashMap<>(),
                importer.getPreferredVersions());
        ParentPkgAliases currentParentPkgAliases = new ParentPkgAliases();
        for (PkgAddress pkgAddress : pkgAddresses) {
            if (currentParentPkgAliases.get(pkgAddress.getAlias()) != Boolean.TRUE) {
                currentParentPkgAliases.put(pkgAddress.getAlias(), pkgAddress);
            }
            if (pkgAddress.isUpdated()) {
                ctx.getUpdatedSet().add(pkgAddress.getAlias());
            }
            ResolvedPackage resolvedPackage = ctx.getResolvedPkgsById().get(pkgAddress.getPkgId());
            if (resolvedPackage == null) {
                continue;
            }
            if (!newPreferredVersions.hasOwn(resolvedPackage.getName())) {
                Map<String, PreferredVersions.Selector> inherited =
                        importer.getPreferredVersions().get(resolvedPackage.getName());
                newPreferredVersions.put(resolvedPackage.getName(),
                        inherited == null ? new LinkedHashMap<>() : new LinkedHashMap<>(inherited));
            }
            Map<String, PreferredVersions.Selector> selectors = newPreferredVersions.get(resolvedPackage.getName());
            if (selectors.get(resolvedPackage.getVersion()) == null) {
                selectors.put(resolvedPackage.getVersion(), PreferredVersions.Selector.of("version",
                        PreferredVersions.DIRECT_DEP_SELECTOR_WEIGHT));
            }
        }
        ParentPkgAliases newParentPkgAliases = new ParentPkgAliases(importer.getParentPkgAliases());
        newParentPkgAliases.putAll(currentParentPkgAliases);
        PostponedResolutionOpts postponedResolutionOpts = new PostponedResolutionOpts(newPreferredVersions,
                newParentPkgAliases, publishedBy);
        List<Promise<PeersResolutionResult>> children = new ArrayList<>();
        for (Function<PostponedResolutionOpts, Promise<PeersResolutionResult>> postponed :
                resolveResult.getPostponedResolutionsQueue()) {
            children.add(postponed.apply(postponedResolutionOpts));
        }
        return Promise.all(ctx.getLoop(), children).then(childrenResults -> {
            if (!ctx.isHoistPeers()) {
                return Promise.resolved(ctx.getLoop(),
                        new PkgAddressesByImporter(pkgAddresses, new LinkedHashMap<>(), new LinkedHashMap<>()));
            }
            List<Promise<PeersResolutionResult>> peers = new ArrayList<>();
            for (Function<ParentPkgAliases, Promise<PeersResolutionResult>> postponed :
                    resolveResult.getPostponedPeersResolutionQueue()) {
                peers.add(postponed.apply(postponedResolutionOpts.getParentPkgAliases()));
            }
            return Promise.all(ctx.getLoop(), peers).then(postponedPeersResolution -> {
                Map<String, PkgAddress> resolvedPeers = new LinkedHashMap<>();
                for (PeersResolutionResult r : childrenResults) {
                    resolvedPeers.putAll(r.getResolvedPeers());
                }
                for (PeersResolutionResult r : postponedPeersResolution) {
                    resolvedPeers.putAll(r.getResolvedPeers());
                }
                List<Map<String, MissingPeerInfo>> missing = new ArrayList<>(
                        filterMissingPeersFromPkgAddresses(pkgAddresses, currentParentPkgAliases, resolvedPeers));
                for (PeersResolutionResult r : childrenResults) {
                    missing.add(r.getMissingPeers());
                }
                for (PeersResolutionResult r : postponedPeersResolution) {
                    missing.add(r.getMissingPeers());
                }
                return Promise.resolved(ctx.getLoop(), new PkgAddressesByImporter(pkgAddresses,
                        mergePkgsDeps(missing, ctx.isAutoInstallPeersFromHighestMatch()), resolvedPeers));
            });
        });
    }

    private static Promise<DependencyResolution> resolveDependenciesOfImporterDependency(
            ResolutionContext ctx, ImporterToResolve importer, boolean pickLowestVersion,
            ExtendedWantedDependency extendedWantedDep) {
        WantedDependency wanted = extendedWantedDep.getWantedDependency();
        Catalogs.Resolution catalogResult = Catalogs.resolveFromCatalog(ctx.getCatalogs(), wanted.getAlias(),
                wanted.getBareSpecifier());
        if ("misconfiguration".equals(catalogResult.getType())) {
            throw catalogResult.getError();
        }
        Catalogs.Resolution catalogLookup = "found".equals(catalogResult.getType()) ? catalogResult : null;
        String originalBareSpecifier = wanted.getBareSpecifier();
        if (catalogLookup != null) {
            wanted.setBareSpecifier(catalogLookup.getSpecifier());
            extendedWantedDep.setPreferredVersion(getCatalogExistingVersionFromSnapshot(catalogLookup,
                    ctx.getWantedLockfile(), wanted));
        }
        ResolvedDependenciesOptions options = importer.getOptions().copy();
        options.setParentPkgAliases(importer.getParentPkgAliases());
        options.setPickLowestVersion(pickLowestVersion && !importer.isUpdatePackageManifest());
        options.setPinnedVersion(importer.getPinnedVersion());
        options.setPublishedBy(ctx.getMaximumPublishedBy());
        return resolveDependenciesOfDependency(ctx, importer.getPreferredVersions(), options, extendedWantedDep)
                .then(result -> {
                    if (result.getResolveDependencyResult() != null && catalogLookup != null) {
                        result.getResolveDependencyResult().setCatalogLookup(new CatalogLookup(
                                catalogLookup.getCatalogName(), catalogLookup.getSpecifier(), originalBareSpecifier));
                    }
                    return Promise.resolved(ctx.getLoop(), result);
                });
    }

    private static List<Map<String, MissingPeerInfo>> filterMissingPeersFromPkgAddresses(
            List<PkgAddress> pkgAddresses, ParentPkgAliases currentParentPkgAliases,
            Map<String, PkgAddress> resolvedPeers) {
        List<Map<String, MissingPeerInfo>> out = new ArrayList<>();
        for (PkgAddress pkgAddress : pkgAddresses) {
            Map<String, MissingPeerInfo> missing = new LinkedHashMap<>();
            for (Map.Entry<String, MissingPeerInfo> peer : pkgAddress.getMissingPeers().entrySet()) {
                Object parent = currentParentPkgAliases.get(peer.getKey());
                if (!Js.truthy(parent)) {
                    missing.put(peer.getKey(), peer.getValue());
                    continue;
                }
                if (parent != Boolean.TRUE) {
                    resolvedPeers.put(peer.getKey(), (PkgAddress) parent);
                }
            }
            out.add(missing);
        }
        return out;
    }

    static Promise<ResolvedDependenciesResult> resolveDependencies(ResolutionContext ctx,
                                                                   PreferredVersions.ByName preferredVersions,
                                                                   List<WantedDependency> wantedDependencies,
                                                                   ResolvedDependenciesOptions options) {
        return Promise.async(ctx.getLoop(), () -> {
            List<ExtendedWantedDependency> extendedWantedDeps = getDepsToResolve(wantedDependencies,
                    ctx.getWantedLockfile(), ctx.getRegistries(), options.getPreferredDependencies(),
                    options.isProceed() || ctx.isForceFullResolution(), options.getResolvedDependencies());
            List<Function<PostponedResolutionOpts, Promise<PeersResolutionResult>>> postponedResolutionsQueue =
                    new ArrayList<>();
            List<Function<ParentPkgAliases, Promise<PeersResolutionResult>>> postponedPeersResolutionQueue =
                    new ArrayList<>();
            List<PkgAddress> pkgAddresses = new ArrayList<>();
            List<Promise<Void>> resolving = new ArrayList<>();
            for (ExtendedWantedDependency extendedWantedDep : extendedWantedDeps) {
                resolving.add(Promise.async(ctx.getLoop(), () -> resolveDependenciesOfDependency(ctx,
                        preferredVersions, options, extendedWantedDep).then(result -> {
                    if (result.getResolveDependencyResult() != null) {
                        pkgAddresses.add(result.getResolveDependencyResult());
                    }
                    if (result.getPostponedResolution() != null) {
                        postponedResolutionsQueue.add(result.getPostponedResolution());
                    }
                    if (result.getPostponedPeersResolution() != null) {
                        postponedPeersResolutionQueue.add(result.getPostponedPeersResolution());
                    }
                    return Promise.resolved(ctx.getLoop(), null);
                })));
            }
            return Promise.all(ctx.getLoop(), resolving).then(done -> resolvePostponed(ctx, preferredVersions,
                    options, pkgAddresses, postponedResolutionsQueue, postponedPeersResolutionQueue));
        });
    }

    // the rest of resolveDependencies, once every dependency of this level is resolved
    private static Promise<ResolvedDependenciesResult> resolvePostponed(
            ResolutionContext ctx, PreferredVersions.ByName preferredVersions, ResolvedDependenciesOptions options,
            List<PkgAddress> pkgAddresses,
            List<Function<PostponedResolutionOpts, Promise<PeersResolutionResult>>> postponedResolutionsQueue,
            List<Function<ParentPkgAliases, Promise<PeersResolutionResult>>> postponedPeersResolutionQueue) {
        PreferredVersions.ByName newPreferredVersions = new PreferredVersions.ByName(new LinkedHashMap<>(),
                preferredVersions);
        ParentPkgAliases currentParentPkgAliases = new ParentPkgAliases();
        for (PkgAddress pkgAddress : pkgAddresses) {
            if (currentParentPkgAliases.get(pkgAddress.getAlias()) != Boolean.TRUE) {
                currentParentPkgAliases.put(pkgAddress.getAlias(), pkgAddress);
            }
            if (pkgAddress.isUpdated()) {
                ctx.getUpdatedSet().add(pkgAddress.getAlias());
            }
            ResolvedPackage resolvedPackage = ctx.getResolvedPkgsById().get(pkgAddress.getPkgId());
            if (resolvedPackage == null) {
                continue;
            }
            if (!newPreferredVersions.hasOwn(resolvedPackage.getName())) {
                Map<String, PreferredVersions.Selector> inherited = preferredVersions.get(resolvedPackage.getName());
                newPreferredVersions.put(resolvedPackage.getName(),
                        inherited == null ? new LinkedHashMap<>() : new LinkedHashMap<>(inherited));
            }
            Map<String, PreferredVersions.Selector> selectors = newPreferredVersions.get(resolvedPackage.getName());
            if (selectors.get(resolvedPackage.getVersion()) == null) {
                selectors.put(resolvedPackage.getVersion(), PreferredVersions.Selector.of("version"));
            }
        }
        ParentPkgAliases newParentPkgAliases = new ParentPkgAliases(options.getParentPkgAliases());
        newParentPkgAliases.putAll(currentParentPkgAliases);
        PostponedResolutionOpts postponedResolutionOpts = new PostponedResolutionOpts(newPreferredVersions,
                newParentPkgAliases, options.getPublishedBy());
        List<Promise<PeersResolutionResult>> children = new ArrayList<>();
        for (Function<PostponedResolutionOpts, Promise<PeersResolutionResult>> postponed : postponedResolutionsQueue) {
            children.add(postponed.apply(postponedResolutionOpts));
        }
        return Promise.all(ctx.getLoop(), children).then(childrenResults -> {
            if (!ctx.isHoistPeers()) {
                return Promise.resolved(ctx.getLoop(), new ResolvedDependenciesResult(pkgAddresses,
                        Promise.resolved(ctx.getLoop(),
                                new PeersResolutionResult(new LinkedHashMap<>(), new LinkedHashMap<>()))));
            }
            return Promise.resolved(ctx.getLoop(), new ResolvedDependenciesResult(pkgAddresses,
                    startResolvingPeers(ctx, childrenResults, pkgAddresses, options.getParentPkgAliases(),
                            currentParentPkgAliases, postponedPeersResolutionQueue)));
        });
    }

    private static Promise<PeersResolutionResult> startResolvingPeers(
            ResolutionContext ctx, List<PeersResolutionResult> childrenResults, List<PkgAddress> pkgAddresses,
            ParentPkgAliases parentPkgAliases, ParentPkgAliases currentParentPkgAliases,
            List<Function<ParentPkgAliases, Promise<PeersResolutionResult>>> postponedPeersResolutionQueue) {
        return Promise.async(ctx.getLoop(), () -> {
            List<Promise<PeersResolutionResult>> resolving = new ArrayList<>();
            for (Function<ParentPkgAliases, Promise<PeersResolutionResult>> postponed :
                    postponedPeersResolutionQueue) {
                resolving.add(postponed.apply(parentPkgAliases));
            }
            return Promise.all(ctx.getLoop(), resolving).then(results -> {
                Map<String, PkgAddress> resolvedPeers = new LinkedHashMap<>();
                for (PeersResolutionResult r : childrenResults) {
                    resolvedPeers.putAll(r.getResolvedPeers());
                }
                for (PeersResolutionResult r : results) {
                    resolvedPeers.putAll(r.getResolvedPeers());
                }
                List<Map<String, MissingPeerInfo>> missing = new ArrayList<>(
                        filterMissingPeersFromPkgAddresses(pkgAddresses, currentParentPkgAliases, resolvedPeers));
                for (PeersResolutionResult r : childrenResults) {
                    missing.add(r.getMissingPeers());
                }
                for (PeersResolutionResult r : results) {
                    missing.add(r.getMissingPeers());
                }
                return Promise.resolved(ctx.getLoop(), new PeersResolutionResult(
                        mergePkgsDeps(missing, ctx.isAutoInstallPeersFromHighestMatch()), resolvedPeers));
            });
        });
    }

    private static Map<String, MissingPeerInfo> mergePkgsDeps(List<Map<String, MissingPeerInfo>> pkgsDeps,
                                                             boolean autoInstallPeersFromHighestMatch) {
        Map<String, List<String>> ranges = new LinkedHashMap<>();
        Map<String, Boolean> optionals = new LinkedHashMap<>();
        for (Map<String, MissingPeerInfo> deps : pkgsDeps) {
            for (Map.Entry<String, MissingPeerInfo> dep : deps.entrySet()) {
                if (!ranges.containsKey(dep.getKey())) {
                    ranges.put(dep.getKey(), new ArrayList<>());
                    optionals.put(dep.getKey(), dep.getValue().isOptional());
                } else {
                    optionals.put(dep.getKey(), optionals.get(dep.getKey()) && dep.getValue().isOptional());
                }
                ranges.get(dep.getKey()).add(dep.getValue().getRange());
            }
        }
        Map<String, MissingPeerInfo> merged = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ranges.entrySet()) {
            String intersection = safeIntersect(entry.getValue());
            if (intersection != null) {
                merged.put(entry.getKey(), new MissingPeerInfo(intersection, optionals.get(entry.getKey())));
            } else if (autoInstallPeersFromHighestMatch) {
                merged.put(entry.getKey(), new MissingPeerInfo(String.join(" || ", entry.getValue()),
                        optionals.get(entry.getKey())));
            }
        }
        return merged;
    }

    // mergePeers.ts safeIntersect
    static @Nullable String safeIntersect(List<String> ranges) {
        try {
            return SemverRangeIntersect.intersect(ranges);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Promise<DependencyResolution> resolveDependenciesOfDependency(
            ResolutionContext ctx, PreferredVersions.ByName preferredVersions, ResolvedDependenciesOptions options,
            ExtendedWantedDependency extendedWantedDep) {
        WantedDependency wanted = extendedWantedDep.getWantedDependency();
        int updateDepth = wanted.getUpdateDepth() != null ? wanted.getUpdateDepth() : options.getUpdateDepth();
        boolean updateShouldContinue = options.getCurrentDepth() <= updateDepth;
        boolean updateRequested = updateShouldContinue;
        InfoFromLockfile info = extendedWantedDep.getInfoFromLockfile();
        boolean update = updateRequested ||
                         info == null || info.getDependencyLockfile() == null ||
                         ctx.getWorkspacePackages() != null && ctx.getLinkWorkspacePackagesDepth() != -1 &&
                         wantedDepIsLocallyAvailable(ctx.getWorkspacePackages(), wanted, ctx.getDefaultTag(),
                                 ctx.getRegistries().get("default")) ||
                         ctx.getUpdatedSet().contains(info.getName());

        ResolveDependencyOptions resolveDependencyOpts = new ResolveDependencyOptions();
        resolveDependencyOpts.currentDepth = options.getCurrentDepth();
        resolveDependencyOpts.parentPkg = options.getParentPkg();
        resolveDependencyOpts.parentPkgAliases = options.getParentPkgAliases();
        resolveDependencyOpts.preferredVersions = preferredVersions;
        resolveDependencyOpts.currentPkg = info;
        resolveDependencyOpts.preferredVersion = extendedWantedDep.getPreferredVersion();
        resolveDependencyOpts.pickLowestVersion = options.isPickLowestVersion();
        resolveDependencyOpts.prefix = options.getPrefix();
        resolveDependencyOpts.proceed = extendedWantedDep.isProceed() || updateShouldContinue ||
                                        !ctx.getUpdatedSet().isEmpty();
        resolveDependencyOpts.publishedBy = options.getPublishedBy();
        resolveDependencyOpts.update = update ? options.isUpdateToLatest() ? "latest" : "compatible" : null;
        resolveDependencyOpts.updateDepth = updateDepth;
        resolveDependencyOpts.updateRequested = updateRequested;
        resolveDependencyOpts.parentIds = options.getParentIds();
        resolveDependencyOpts.pinnedVersion = options.getPinnedVersion();

        ParentPkg parentPkg = options.getParentPkg();
        if ("workspace".equals(parentPkg.getResolvedVia()) && parentPkg.getPkgId().startsWith("file:")) {
            Catalogs.Resolution catalogResult = Catalogs.resolveFromCatalog(ctx.getCatalogs(), wanted.getAlias(),
                    wanted.getBareSpecifier());
            if ("misconfiguration".equals(catalogResult.getType())) {
                throw catalogResult.getError();
            }
            if ("found".equals(catalogResult.getType())) {
                wanted.setBareSpecifier(catalogResult.getSpecifier());
                extendedWantedDep.setPreferredVersion(getCatalogExistingVersionFromSnapshot(catalogResult,
                        ctx.getWantedLockfile(), wanted));
                resolveDependencyOpts.preferredVersion = extendedWantedDep.getPreferredVersion();
            }
        }

        return resolveDependency(wanted, ctx, resolveDependencyOpts).then(resolveDependencyResult -> {
            if (resolveDependencyResult == null) {
                return Promise.resolved(ctx.getLoop(), new DependencyResolution(null, null, null));
            }
            if (resolveDependencyResult.isLinkedDependency()) {
                @SuppressWarnings("ConstantConditions")
                String directory = Js.string(resolveDependencyResult.getResolution().get("directory"));
                ResolvedPackage linked = new ResolvedPackage("", Js.string(resolveDependencyResult.getName()),
                        Js.string(resolveDependencyResult.getVersion()));
                DependenciesTreeNode node = new DependenciesTreeNode(linked, -1);
                node.setChildren(new LinkedHashMap<>());
                node.setInstallable(true);
                ctx.getDependenciesTree().put(createNodeIdForLinkedLocalPkg(ctx.getLockfileDir(), directory), node);
                return Promise.resolved(ctx.getLoop(), new DependencyResolution(resolveDependencyResult, null, null));
            }
            if (!resolveDependencyResult.isNew()) {
                MissingPeersOfChildren missingPeersOfChildren = resolveDependencyResult.getMissingPeersOfChildren();
                return Promise.resolved(ctx.getLoop(), new DependencyResolution(resolveDependencyResult, null,
                        missingPeersOfChildren == null ? null : parentPkgAliases ->
                                missingPeersOfChildren.get().then(missingPeers -> Promise.resolved(ctx.getLoop(),
                                        filterMissingPeers(new PeersResolutionResult(missingPeers,
                                                new LinkedHashMap<>()), parentPkgAliases)))));
            }
            List<String> parentIds = new ArrayList<>(options.getParentIds());
            parentIds.add(resolveDependencyResult.getPkgId());
            PackageSnapshot dependencyLockfile = info == null ? null : info.getDependencyLockfile();
            int parentDepth = options.getCurrentDepth();
            return Promise.resolved(ctx.getLoop(), new DependencyResolution(resolveDependencyResult,
                    postponedResolutionOpts -> resolveChildren(ctx, resolveDependencyResult, parentIds,
                            dependencyLockfile, parentDepth, updateDepth, options.getPrefix(),
                            postponedResolutionOpts).then(result -> {
                        if (resolveDependencyResult.getMissingPeersOfChildren() != null) {
                            resolveDependencyResult.getMissingPeersOfChildren().resolve(result.getMissingPeers());
                        }
                        return Promise.resolved(ctx.getLoop(),
                                filterMissingPeers(result, postponedResolutionOpts.getParentPkgAliases()));
                    }), null));
        });
    }

    static NodeId createNodeIdForLinkedLocalPkg(String lockfileDir, String pkgDir) {
        return NodeId.of("link:" + RootRelativePath.relative(lockfileDir, pkgDir));
    }

    private static PeersResolutionResult filterMissingPeers(PeersResolutionResult result,
                                                            ParentPkgAliases parentPkgAliases) {
        Map<String, MissingPeerInfo> newMissing = new LinkedHashMap<>();
        Map<String, PkgAddress> resolvedPeers = result.getResolvedPeers();
        for (Map.Entry<String, MissingPeerInfo> peer : result.getMissingPeers().entrySet()) {
            Object parent = parentPkgAliases.get(peer.getKey());
            if (Js.truthy(parent)) {
                if (parent != Boolean.TRUE) {
                    resolvedPeers.put(peer.getKey(), (PkgAddress) parent);
                }
            } else {
                newMissing.put(peer.getKey(), peer.getValue());
            }
        }
        return new PeersResolutionResult(newMissing, resolvedPeers);
    }

    private static Promise<PeersResolutionResult> resolveChildren(ResolutionContext ctx, PkgAddress parentPkg,
                                                                  List<String> parentIds,
                                                                  @Nullable PackageSnapshot dependencyLockfile,
                                                                  int parentDepth, int updateDepth, String prefix,
                                                                  PostponedResolutionOpts opts) {
        Map<String, @Nullable String> currentResolvedDependencies = null;
        if (dependencyLockfile != null) {
            currentResolvedDependencies = new LinkedHashMap<>();
            if (dependencyLockfile.getDependencies() != null) {
                currentResolvedDependencies.putAll(dependencyLockfile.getDependencies());
            }
            if (dependencyLockfile.getOptionalDependencies() != null) {
                currentResolvedDependencies.putAll(dependencyLockfile.getOptionalDependencies());
            }
        }
        Map<String, @Nullable String> resolvedDependencies = parentPkg.isUpdated() ? null :
                currentResolvedDependencies;
        Object peers = dependencyLockfile != null && dependencyLockfile.getPeerDependencies() != null ?
                dependencyLockfile.getPeerDependencies() : parentPkg.getPkg().get("peerDependencies");
        boolean parentDependsOnPeer = peers instanceof Map && !((Map<?, ?>) peers).isEmpty();
        List<WantedDependency> wantedDependencies = getNonDevWantedDependencies(parentPkg.getPkg());
        ResolvedDependenciesOptions options = new ResolvedDependenciesOptions(new ParentPkg(parentPkg.getNodeId(),
                parentPkg.isInstallable(), Js.string(parentPkg.getRootDir()), parentPkg.isOptional(),
                parentPkg.getPkgId(), parentPkg.getResolvedVia()));
        options.setCurrentDepth(parentDepth + 1);
        options.setParentPkgAliases(opts.getParentPkgAliases());
        options.setPreferredDependencies(currentResolvedDependencies);
        options.setPrefix(prefix);
        options.setProceed(!parentPkg.isDepIsLinked() || parentDependsOnPeer);
        options.setPublishedBy(opts.getPublishedBy());
        options.setResolvedDependencies(resolvedDependencies);
        options.setUpdateDepth(updateDepth);
        options.setParentIds(parentIds);
        return resolveDependencies(ctx, opts.getPreferredVersions(), wantedDependencies, options).then(result -> {
            List<ChildId> children = new ArrayList<>();
            Map<String, NodeId> childNodeIds = new LinkedHashMap<>();
            for (PkgAddress child : result.getPkgAddresses()) {
                children.add(new ChildId(child.getAlias(), child.getPkgId()));
                childNodeIds.put(child.getAlias(), child.getNodeId() != null ? child.getNodeId() :
                        NodeId.of(child.getPkgId()));
            }
            ctx.getChildrenByParentId().put(parentPkg.getPkgId(), children);
            DependenciesTreeNode node = new DependenciesTreeNode(
                    ctx.getResolvedPkgsById().get(parentPkg.getPkgId()), parentDepth);
            node.setChildren(childNodeIds);
            node.setInstallable(parentPkg.isInstallable());
            ctx.getDependenciesTree().put(parentPkg.getNodeId(), node);
            return result.getResolvingPeers();
        });
    }

    static List<ExtendedWantedDependency> getDepsToResolve(List<WantedDependency> wantedDependencies,
                                                           LockfileObject wantedLockfile,
                                                           Map<String, String> registries,
                                                           @Nullable Map<String, @Nullable String> preferredDeps,
                                                           boolean proceed,
                                                           @Nullable Map<String, @Nullable String> resolvedDeps) {
        Map<String, @Nullable String> resolvedDependencies = resolvedDeps == null ? Collections.emptyMap() :
                resolvedDeps;
        Map<String, @Nullable String> preferredDependencies = preferredDeps == null ? Collections.emptyMap() :
                preferredDeps;
        List<ExtendedWantedDependency> extendedWantedDeps = new ArrayList<>();
        boolean proceedAll = proceed;
        for (WantedDependency wantedDependency : wantedDependencies) {
            String reference = null;
            boolean proceedThis = proceedAll;
            String alias = wantedDependency.getAlias();
            if (alias != null && !alias.isEmpty()) {
                String resolved = resolvedDependencies.get(alias);
                String preferred = preferredDependencies.get(alias);
                if (Js.truthy(resolved) && (referenceSatisfiesWantedSpec(wantedLockfile, wantedDependency, resolved) ||
                                            resolved.startsWith("file:"))) {
                    reference = resolved;
                } else if (validRange(wantedDependency.getBareSpecifier()) != null && Js.truthy(preferred) &&
                           referenceSatisfiesWantedSpec(wantedLockfile, wantedDependency, preferred)) {
                    proceedThis = true;
                    reference = preferred;
                }
            }
            InfoFromLockfile infoFromLockfile = getInfoFromLockfile(wantedLockfile, registries, reference, alias);
            if (!proceedAll && (infoFromLockfile == null ||
                                infoFromLockfile.getDependencyLockfile() != null &&
                                (infoFromLockfile.getDependencyLockfile().getPeerDependencies() != null ||
                                 hasTransitivePeers(infoFromLockfile.getDependencyLockfile())))) {
                proceedThis = true;
                proceedAll = true;
                for (ExtendedWantedDependency extended : extendedWantedDeps) {
                    extended.setProceed(true);
                }
            }
            extendedWantedDeps.add(new ExtendedWantedDependency(infoFromLockfile, proceedThis, wantedDependency));
        }
        return extendedWantedDeps;
    }

    private static boolean hasTransitivePeers(PackageSnapshot snapshot) {
        return snapshot.getTransitivePeerDependencies() != null && !snapshot.getTransitivePeerDependencies().isEmpty();
    }

    private static boolean referenceSatisfiesWantedSpec(LockfileObject lockfile, WantedDependency wantedDep,
                                                        String preferredRef) {
        String depPath = DepPath.refToRelative(preferredRef, wantedDep.getAlias());
        if (depPath == null) {
            return false;
        }
        PackageSnapshot pkgSnapshot = lockfile.getPackages() == null ? null : lockfile.getPackages().get(depPath);
        if (pkgSnapshot == null) {
            return false;
        }
        String version = nameVerFromPkgSnapshot(depPath, pkgSnapshot)[1];
        if (validRange(wantedDep.getBareSpecifier()) == null) {
            for (ProjectSnapshot importer : lockfile.getImporters().values()) {
                if (wantedDep.getBareSpecifier().equals(importer.getSpecifiers().get(wantedDep.getAlias()))) {
                    return true;
                }
            }
        }
        return satisfiesLoose(version, wantedDep.getBareSpecifier());
    }

    /**
     * {@code nameVerFromPkgSnapshot}: the name, and the version the snapshot records else the dependency path's.
     */
    static @Nullable String[] nameVerFromPkgSnapshot(String depPath, PackageSnapshot pkgSnapshot) {
        DepPath.DependencyPath pkgInfo = DepPath.parse(depPath);
        String version = pkgSnapshot.getVersion() != null ? pkgSnapshot.getVersion() : pkgInfo.getVersion();
        return new String[]{pkgInfo.getName(), version, pkgInfo.getNonSemverVersion()};
    }

    private static @Nullable InfoFromLockfile getInfoFromLockfile(LockfileObject lockfile,
                                                                 Map<String, String> registries,
                                                                 @Nullable String reference,
                                                                 @Nullable String alias) {
        if (!Js.truthy(reference) || !Js.truthy(alias)) {
            return null;
        }
        String depPath = DepPath.refToRelative(reference, alias);
        if (depPath == null || depPath.isEmpty()) {
            return null;
        }
        PackageSnapshot dependencyLockfile = lockfile.getPackages() == null ? null :
                lockfile.getPackages().get(depPath);
        if (dependencyLockfile != null) {
            if (dependencyLockfile.getPeerDependencies() != null && dependencyLockfile.getDependencies() != null) {
                @SuppressWarnings("unchecked")
                Map<String, @Nullable Object> peerDependencies =
                        (Map<String, @Nullable Object>) dependencyLockfile.getPeerDependencies();
                Map<String, @Nullable String> dependencies = new LinkedHashMap<>();
                for (Map.Entry<String, @Nullable String> dep : dependencyLockfile.getDependencies().entrySet()) {
                    if (Js.truthy(peerDependencies.get(dep.getKey()))) {
                        continue;
                    }
                    dependencies.put(dep.getKey(), dep.getValue());
                }
                dependencyLockfile = dependencyLockfile.copy();
                dependencyLockfile.setDependencies(dependencies);
            }
            String[] nameVer = nameVerFromPkgSnapshot(depPath, dependencyLockfile);
            InfoFromLockfile info = new InfoFromLockfile(nameVer[2] != null ? nameVer[2] :
                    nameVer[0] + "@" + nameVer[1]);
            info.setName(nameVer[0]);
            info.setVersion(nameVer[1]);
            info.setDependencyLockfile(dependencyLockfile);
            info.setResolution(dependencyLockfile.getResolution() == null ? null :
                    LockfileUtils.pkgSnapshotToResolution(depPath, dependencyLockfile, registries));
            return info;
        }
        DepPath.DependencyPath parsed = DepPath.parse(depPath);
        String pkgId = parsed.getNonSemverVersion() != null ? parsed.getNonSemverVersion() :
                parsed.getName() != null && parsed.getVersion() != null ? parsed.getName() + "@" + parsed.getVersion() :
                        depPath;
        return new InfoFromLockfile(pkgId);
    }

    /**
     * {@code ResolveDependencyOptions}.
     */
    static final class ResolveDependencyOptions {
        int currentDepth;
        @Nullable InfoFromLockfile currentPkg;
        @Nullable String preferredVersion;
        ParentPkg parentPkg;
        List<String> parentIds = new ArrayList<>();
        ParentPkgAliases parentPkgAliases = new ParentPkgAliases();
        PreferredVersions.ByName preferredVersions;
        String prefix = "";
        boolean proceed;
        @Nullable Instant publishedBy;
        boolean pickLowestVersion;
        @Nullable String update;
        int updateDepth;
        boolean updateRequested;
        @Nullable String pinnedVersion;
    }

    private static Promise<@Nullable PkgAddress> resolveDependency(WantedDependency wantedDependency,
                                                                   ResolutionContext ctx,
                                                                   ResolveDependencyOptions options) {
        InfoFromLockfile currentPkg = options.currentPkg;
        WantedDependency wanted;
        if (options.parentPkg.isInstallable()) {
            wanted = wantedDependency;
        } else {
            wanted = wantedDependency.copy();
            wanted.setOptional(true);
        }
        PreferredVersions.ByName preferredVersions = !options.updateRequested && options.preferredVersion != null ?
                getExactSinglePreferredVersions(wanted, options.preferredVersion) :
                options.preferredVersions;
        return Promise.async(ctx.getLoop(), () -> requestPackage(wanted, ctx, options, currentPkg, preferredVersions))
                .recover(e -> {
                    boolean trustDowngrade = e instanceof PnpmError &&
                                             "ERR_PNPM_TRUST_DOWNGRADE".equals(((PnpmError) e).getCode());
                    if ((e instanceof PnpmError || e instanceof JsError) && wanted.isOptional() && !trustDowngrade) {
                        return Promise.resolved(ctx.getLoop(), null);
                    }
                    throw e;
                })
                .then(pkgResponse -> pkgResponse == null ? Promise.resolved(ctx.getLoop(), null) :
                        resolvedDependency(wanted, ctx, options, currentPkg, pkgResponse));
    }

    private static Promise<PackageRequester.PackageResponse> requestPackage(
            WantedDependency wantedDependency, ResolutionContext ctx, ResolveDependencyOptions options,
            @Nullable InfoFromLockfile currentPkg, PreferredVersions.ByName preferredVersions) {
        boolean calcSpecifier = options.currentDepth == 0;
        if (options.update == null && currentPkg != null && currentPkg.getVersion() != null &&
            currentPkg.getPkgId().endsWith("@" + currentPkg.getVersion()) && !calcSpecifier) {
            wantedDependency.setBareSpecifier(replaceVersionInBareSpecifier(wantedDependency.getBareSpecifier(),
                    currentPkg.getVersion(), ctx.getNamedRegistryPrefixes()));
        }
        PackageRequester.RequestPackageOptions request = new PackageRequester.RequestPackageOptions();
        request.alwaysTryWorkspacePackages = ctx.getLinkWorkspacePackagesDepth() >= options.currentDepth;
        request.currentPkg = currentPkg;
        request.currentPkgPublishedAt = currentPkg == null ? null : timeOf(ctx, currentPkg.getPkgId());
        request.defaultTag = ctx.getDefaultTag();
        request.publishedBy = options.publishedBy;
        request.publishedByExclude = ctx.getPublishedByExclude();
        request.trustPolicy = ctx.getTrustPolicy();
        request.trustPolicyExclude = ctx.getTrustPolicyExclude();
        request.trustPolicyIgnoreAfter = ctx.getTrustPolicyIgnoreAfter();
        request.pickLowestVersion = options.pickLowestVersion;
        request.lockfileDir = ctx.getLockfileDir();
        request.preferredVersions = preferredVersions;
        request.preferWorkspacePackages = ctx.isPreferWorkspacePackages();
        request.projectDir = options.currentDepth > 0 && !wantedDependency.getBareSpecifier().startsWith("file:") ?
                ctx.getLockfileDir() : options.parentPkg.getRootDir();
        request.update = options.update;
        request.workspacePackages = ctx.getWorkspacePackages();
        request.injectWorkspacePackages = ctx.isInjectWorkspacePackages();
        request.calcSpecifier = calcSpecifier;
        request.pinnedVersion = options.pinnedVersion;
        return ctx.getLoop().request(() -> ctx.getStoreController().requestPackage(wantedDependency, request));
    }

    // resolveDependency after the package is requested
    private static Promise<@Nullable PkgAddress> resolvedDependency(WantedDependency wantedDependency,
                                                                    ResolutionContext ctx,
                                                                    ResolveDependencyOptions options,
                                                                    @Nullable InfoFromLockfile currentPkg,
                                                                    PackageRequester.PackageResponse pkgResponse) {
        if (pkgResponse.getPolicyViolation() != null) {
            ctx.getResolutionPolicyViolations().add(pkgResponse.getPolicyViolation());
        }
        if (ctx.isBlockExoticSubdeps() && options.currentDepth > 0 && pkgResponse.getResolvedVia() != null &&
            !NON_EXOTIC_RESOLVED_VIA.contains(pkgResponse.getResolvedVia())) {
            throw new PnpmError("EXOTIC_SUBDEP", "Exotic dependency \"" + wantedDependency.getAlias() +
                                                 "\" (resolved via " + pkgResponse.getResolvedVia() + ") is not " +
                                                 "allowed in subdependencies when blockExoticSubdeps is enabled");
        }
        if (pkgResponse.getUnknownManifest() != null) {
            throw pkgResponse.getUnknownManifest();
        }
        Map<String, @Nullable Object> responseManifest = pkgResponse.getManifest();
        if (ctx.getAllPreferredVersions() != null && responseManifest != null &&
            Js.truthy(responseManifest.get("version"))) {
            String name = Js.string(responseManifest.get("name"));
            ctx.getAllPreferredVersions().computeIfAbsent(name, n -> new LinkedHashMap<>())
                    .put(Js.string(responseManifest.get("version")), PreferredVersions.Selector.of("version"));
        }

        if (pkgResponse.isLocal()) {
            if (responseManifest == null) {
                throw new PnpmError("MISSING_PACKAGE_JSON", "Can't install " + wantedDependency.getBareSpecifier() +
                                                            ": Missing package.json file");
            }
            String alias = wantedDependency.getAlias() != null ? wantedDependency.getAlias() :
                    pkgResponse.getAlias() != null ? pkgResponse.getAlias() : Js.string(responseManifest.get("name"));
            PkgAddress linked = new PkgAddress(alias, pkgResponse.getId());
            linked.setDev(wantedDependency.isDev());
            linked.setLinkedDependency(true);
            linked.setName(responseManifest.get("name") == null ? null : Js.string(responseManifest.get("name")));
            linked.setOptional(wantedDependency.isOptional());
            linked.setResolution(pkgResponse.getResolution());
            linked.setVersion(responseManifest.get("version") == null ? null :
                    Js.string(responseManifest.get("version")));
            linked.setNormalizedBareSpecifier(pkgResponse.getNormalizedBareSpecifier());
            linked.setPkg(responseManifest);
            return Promise.resolved(ctx.getLoop(), linked);
        }

        Map<String, @Nullable Object> manifest = getManifestFromResponse(pkgResponse, wantedDependency, currentPkg);
        if (manifest.get("dependencies") == null) {
            manifest.put("dependencies", new LinkedHashMap<String, @Nullable Object>());
        }
        ReadPackageHook readPackageHook = ctx.getReadPackageHook();
        if (readPackageHook == null) {
            return Promise.resolved(ctx.getLoop(),
                    pkgAddress(wantedDependency, ctx, options, currentPkg, pkgResponse, manifest));
        }
        return Promise.resolved(ctx.getLoop(), readPackageHook.apply(manifest, null)).then(hooked ->
                Promise.resolved(ctx.getLoop(), pkgAddress(wantedDependency, ctx, options, currentPkg, pkgResponse,
                        hooked)));
    }

    // resolveDependency after the readPackage hook
    private static @Nullable PkgAddress pkgAddress(WantedDependency wantedDependency, ResolutionContext ctx,
                                                   ResolveDependencyOptions options,
                                                   @Nullable InfoFromLockfile currentPkg,
                                                   PackageRequester.PackageResponse pkgResponse,
                                                   Map<String, @Nullable Object> manifest) {
        boolean prepare;
        boolean hasBin;
        Map<String, @Nullable Object> pkg = manifest;
        Object peerDependencies = pkg.get("peerDependencies");
        if (Js.truthy(peerDependencies) && Js.truthy(pkg.get("dependencies"))) {
            Map<String, @Nullable Object> copy = new LinkedHashMap<>(pkg);
            Map<String, @Nullable Object> dependencies = new LinkedHashMap<>(asMap(pkg.get("dependencies")));
            for (String peer : Js.ownKeys(peerDependencies)) {
                if (ctx.isAutoInstallPeers() || Js.truthy(options.parentPkgAliases.get(peer))) {
                    dependencies.remove(peer);
                }
            }
            copy.put("dependencies", dependencies);
            pkg = copy;
        }
        Object engines = pkg.get("engines");
        if (engines instanceof Map && ((Map<?, ?>) engines).get("runtime") != null) {
            PkgManifestUtils.convertEnginesRuntimeToDependencies(pkg, "engines", "dependencies");
        }
        if (!Js.truthy(pkg.get("name"))) {
            throw new PnpmError("MISSING_PACKAGE_NAME", "Can't install " + wantedDependency.getBareSpecifier() +
                                                        ": Missing package name");
        }
        String pkgName = Js.string(pkg.get("name"));
        String pkgIdWithPatchHash = pkgResponse.getId().startsWith(pkgName + "@") ? pkgResponse.getId() :
                pkgName + "@" + pkgResponse.getId();
        PatchingConfig.PatchInfo patch = PatchingConfig.getPatchInfo(ctx.getPatchedDependencies(), pkgName,
                Js.string(pkg.get("version")));
        if (patch != null) {
            ctx.getAppliedPatches().add(patch.getKey());
            pkgIdWithPatchHash = pkgIdWithPatchHash + "(patch_hash=" + patch.getHash() + ")";
        }

        if (parentIdsContainSequence(options.parentIds, options.parentPkg.getPkgId(), pkgResponse.getId()) ||
            pkgResponse.getId().equals(options.parentPkg.getPkgId())) {
            return null;
        }

        PackageSnapshot dependencyLockfile = currentPkg == null ? null : currentPkg.getDependencyLockfile();
        prepare = "git-repository".equals(pkgResponse.getResolvedVia()) &&
                  asMap(pkg.get("scripts")).get("prepare") instanceof String;
        if (dependencyLockfile != null && Js.truthy(dependencyLockfile.getDeprecated()) &&
            !pkgResponse.isUpdated() && !Js.truthy(pkg.get("deprecated"))) {
            pkg.put("deprecated", dependencyLockfile.getDeprecated());
        }
        Object bin = pkg.get("bin");
        if (dependencyLockfile != null && dependencyLockfile.getHasBin() != null && !Js.truthy(bin)) {
            hasBin = Js.truthy(dependencyLockfile.getHasBin());
        } else {
            boolean binPresent = Js.truthy(bin) &&
                                 !("".equals(bin) || bin instanceof Map && ((Map<?, ?>) bin).isEmpty());
            hasBin = binPresent || Js.truthy(asMap(pkg.get("directories")).get("bin"));
        }

        for (String name : Js.ownKeys(pkg.get("peerDependencies"))) {
            ctx.getAllPeerDepNames().add(name);
        }
        for (String name : Js.ownKeys(pkg.get("peerDependenciesMeta"))) {
            ctx.getAllPeerDepNames().add(name);
        }
        NodeId nodeId = pkgIsLeaf(pkg) ? NodeId.of(pkgResponse.getId()) : ctx.nextNodeId();

        boolean parentIsInstallable = options.parentPkg.isInstallable();
        boolean installable = parentIsInstallable && !Boolean.FALSE.equals(pkgResponse.getIsInstallable());
        boolean isNew = !ctx.getResolvedPkgsById().containsKey(pkgResponse.getId());
        String parentImporterId = options.parentIds.get(0);
        boolean currentIsOptional = wantedDependency.isOptional() || options.parentPkg.isOptional();

        if (isNew) {
            if (Boolean.FALSE.equals(pkgResponse.getIsInstallable()) || !parentIsInstallable) {
                ctx.getSkipped().add(pkgResponse.getId());
            }
            ctx.getResolvedPkgsById().put(pkgResponse.getId(), getResolvedPackage(pkgIdWithPatchHash, hasBin, patch,
                    pkg, pkgResponse, prepare, wantedDependency, parentImporterId, currentIsOptional));
        } else {
            ResolvedPackage existing = ctx.getResolvedPkgsById().get(pkgResponse.getId());
            existing.setProd(existing.isProd() || !wantedDependency.isDev() && !wantedDependency.isOptional());
            existing.setDev(existing.isDev() || wantedDependency.isDev());
            existing.setOptional(existing.isOptional() && currentIsOptional);
            DependenciesTreeNode node = ctx.getDependenciesTree().get(nodeId);
            if (node != null) {
                node.setDepth(Math.min(node.getDepth(), options.currentDepth));
            } else {
                String alias = wantedDependency.getAlias() != null ? wantedDependency.getAlias() :
                        pkgResponse.getAlias() != null ? pkgResponse.getAlias() : pkgName;
                ctx.getPendingNodes().add(new PendingNode(alias, nodeId, existing, options.currentDepth, installable,
                        options.parentIds));
            }
        }

        String rootDir = "directory".equals(pkgResponse.getResolution().get("type")) ?
                RootRelativePath.join(ctx.getLockfileDir(), Js.string(pkgResponse.getResolution().get("directory"))) :
                options.prefix;
        MissingPeersOfChildren missingPeersOfChildren = null;
        if (ctx.isHoistPeers() && !options.parentIds.contains(pkgResponse.getId())) {
            MissingPeersOfChildrenAtDepth existing = ctx.getMissingPeersOfChildrenByPkgId().get(pkgResponse.getId());
            if (existing != null) {
                if (existing.getDepth() >= options.currentDepth ||
                    existing.getMissingPeersOfChildren().isResolved()) {
                    missingPeersOfChildren = existing.getMissingPeersOfChildren();
                }
            } else {
                missingPeersOfChildren = new MissingPeersOfChildren(ctx.getLoop());
                ctx.getMissingPeersOfChildrenByPkgId().put(pkgResponse.getId(),
                        new MissingPeersOfChildrenAtDepth(options.currentDepth, missingPeersOfChildren));
            }
        }

        ResolvedPackage resolvedPkg = ctx.getResolvedPkgsById().get(pkgResponse.getId());
        String alias = wantedDependency.getAlias() != null ? wantedDependency.getAlias() :
                pkgResponse.getAlias() != null ? pkgResponse.getAlias() : pkgName;
        PkgAddress address = new PkgAddress(alias, pkgResponse.getId());
        address.setDepIsLinked(false);
        address.setResolvedVia(pkgResponse.getResolvedVia());
        address.setNew(isNew);
        address.setNodeId(nodeId);
        address.setNormalizedBareSpecifier(pkgResponse.getNormalizedBareSpecifier());
        address.setMissingPeersOfChildren(missingPeersOfChildren);
        address.setRootDir(rootDir);
        address.setMissingPeers(getMissingPeers(pkg));
        address.setOptional(resolvedPkg.isOptional());
        address.setVersion(resolvedPkg.getVersion());
        address.setSaveCatalogName(wantedDependency.getSaveCatalogName());
        address.setInstallable(installable);
        address.setPkg(pkg);
        address.setUpdated(pkgResponse.isUpdated());
        address.setPublishedAt(pkgResponse.getPublishedAt());
        return address;
    }

    private static @Nullable String timeOf(ResolutionContext ctx, String pkgId) {
        Object time = ctx.getWantedLockfile().getTime();
        if (!(time instanceof Map)) {
            return null;
        }
        Object at = ((Map<?, ?>) time).get(pkgId);
        return at == null ? null : Js.string(at);
    }

    static Map<String, @Nullable Object> getManifestFromResponse(PackageRequester.PackageResponse pkgResponse,
                                                                 WantedDependency wantedDependency,
                                                                 @Nullable InfoFromLockfile currentPkg) {
        if (pkgResponse.getManifest() != null) {
            return pkgResponse.getManifest();
        }
        Map<String, @Nullable Object> pkg = new LinkedHashMap<>();
        if (currentPkg != null && Js.truthy(currentPkg.getName()) && Js.truthy(currentPkg.getVersion())) {
            pkg.put("name", currentPkg.getName());
            pkg.put("version", currentPkg.getVersion());
            return pkg;
        }
        String alias = wantedDependency.getAlias();
        if (Js.truthy(alias)) {
            pkg.put("name", alias);
        } else {
            String[] parts = wantedDependency.getBareSpecifier().split("/", -1);
            pkg.put("name", parts[parts.length - 1]);
        }
        pkg.put("version", "0.0.0");
        return pkg;
    }

    private static Map<String, MissingPeerInfo> getMissingPeers(Map<String, @Nullable Object> pkg) {
        Map<String, MissingPeerInfo> missingPeers = new LinkedHashMap<>();
        Map<String, @Nullable Object> peerDependencies = asMap(pkg.get("peerDependencies"));
        Map<String, @Nullable Object> meta = asMap(pkg.get("peerDependenciesMeta"));
        for (String peerName : Js.keys(peerDependencies)) {
            Object peerMeta = meta.get(peerName);
            boolean optional = peerMeta instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) peerMeta).get("optional"));
            missingPeers.put(peerName, new MissingPeerInfo(Js.string(peerDependencies.get(peerName)), optional));
        }
        return missingPeers;
    }

    static boolean pkgIsLeaf(Map<String, @Nullable Object> pkg) {
        return Js.ownKeys(pkg.get("dependencies")).isEmpty() &&
               Js.ownKeys(pkg.get("optionalDependencies")).isEmpty() &&
               Js.ownKeys(pkg.get("peerDependencies")).isEmpty() &&
               Js.ownKeys(pkg.get("peerDependenciesMeta")).isEmpty();
    }

    private static ResolvedPackage getResolvedPackage(String pkgIdWithPatchHash, boolean hasBin,
                                                      PatchingConfig.@Nullable PatchInfo patch,
                                                      Map<String, @Nullable Object> pkg,
                                                      PackageRequester.PackageResponse pkgResponse, boolean prepare,
                                                      WantedDependency wantedDependency, String parentImporterId,
                                                      boolean optional) {
        ResolvedPackage resolved = new ResolvedPackage(pkgResponse.getId(), Js.string(pkg.get("name")),
                Js.string(pkg.get("version")));
        Map<String, @Nullable Object> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("bundledDependencies", pkg.get("bundledDependencies"));
        additionalInfo.put("bundleDependencies", pkg.get("bundleDependencies"));
        additionalInfo.put("cpu", pkg.get("cpu"));
        additionalInfo.put("deprecated", pkg.get("deprecated"));
        additionalInfo.put("engines", pkg.get("engines"));
        additionalInfo.put("os", pkg.get("os"));
        additionalInfo.put("libc", pkg.get("libc"));
        resolved.setAdditionalInfo(additionalInfo);
        resolved.setLeaf(pkgIsLeaf(pkg));
        resolved.setPkgIdWithPatchHash(pkgIdWithPatchHash);
        resolved.setDev(wantedDependency.isDev());
        resolved.setHasBin(hasBin);
        resolved.setHasBundledDependencies(pkg.get("bundledDependencies") != null ||
                                           pkg.get("bundleDependencies") != null);
        resolved.setOptional(optional);
        resolved.setOptionalDependencies(new LinkedHashSet<>(Js.ownKeys(pkg.get("optionalDependencies"))));
        resolved.setPatch(patch);
        resolved.setPeerDependencies(peerDependenciesWithoutOwn(pkg));
        resolved.setPrepare(prepare);
        resolved.setProd(!wantedDependency.isDev() && !wantedDependency.isOptional());
        resolved.setResolution(pkgResponse.getResolution());
        return resolved;
    }

    private static Map<String, PeerDependency> peerDependenciesWithoutOwn(Map<String, @Nullable Object> pkg) {
        Object peers = pkg.get("peerDependencies");
        Object peersMeta = pkg.get("peerDependenciesMeta");
        if (peers == null && peersMeta == null) {
            return new LinkedHashMap<>();
        }
        Set<String> ownDeps = new LinkedHashSet<>();
        ownDeps.add(Js.string(pkg.get("name")));
        ownDeps.addAll(Js.ownKeys(pkg.get("dependencies")));
        ownDeps.addAll(Js.ownKeys(pkg.get("optionalDependencies")));
        Map<String, PeerDependency> result = new LinkedHashMap<>();
        Map<String, @Nullable Object> peerDependencies = asMap(peers);
        for (String peerName : Js.keys(peerDependencies)) {
            if (ownDeps.contains(peerName)) {
                continue;
            }
            result.put(peerName, new PeerDependency(Js.string(peerDependencies.get(peerName))));
        }
        Map<String, @Nullable Object> meta = asMap(peersMeta);
        for (String peerName : Js.keys(meta)) {
            Object peerMeta = meta.get(peerName);
            if (ownDeps.contains(peerName) || !(peerMeta instanceof Map) ||
                !Boolean.TRUE.equals(((Map<?, ?>) peerMeta).get("optional"))) {
                continue;
            }
            result.computeIfAbsent(peerName, n -> new PeerDependency("*")).setOptional(true);
        }
        return result;
    }

    private static @Nullable String getCatalogExistingVersionFromSnapshot(Catalogs.Resolution catalogLookup,
                                                                         LockfileObject wantedLockfile,
                                                                         WantedDependency wantedDependency) {
        Object catalogs = wantedLockfile.getCatalogs();
        Object catalog = catalogs instanceof Map ? ((Map<?, ?>) catalogs).get(catalogLookup.getCatalogName()) : null;
        Object existing = catalog instanceof Map ? ((Map<?, ?>) catalog).get(wantedDependency.getAlias()) : null;
        if (!(existing instanceof Map)) {
            return null;
        }
        return Js.strictEquals(((Map<?, ?>) existing).get("specifier"), catalogLookup.getSpecifier()) ?
                (((Map<?, ?>) existing).get("version") == null ? null :
                        Js.string(((Map<?, ?>) existing).get("version"))) : null;
    }

    /**
     * getNonDevWantedDependencies.ts: a package's {@code optionalDependencies} then {@code dependencies}, bundled ones
     * left out.
     */
    static List<WantedDependency> getNonDevWantedDependencies(Map<String, @Nullable Object> pkg) {
        Object bd = pkg.get("bundledDependencies") != null ? pkg.get("bundledDependencies") :
                pkg.get("bundleDependencies");
        if (Boolean.TRUE.equals(bd)) {
            bd = pkg.get("dependencies") != null ? new ArrayList<>(Js.ownKeys(pkg.get("dependencies"))) :
                    new ArrayList<>();
        }
        Set<String> bundledDeps = new LinkedHashSet<>();
        if (bd instanceof List) {
            for (Object name : (List<?>) bd) {
                bundledDeps.add(Js.string(name));
            }
        }
        Map<String, @Nullable Object> optionalDependencies = asMap(pkg.get("optionalDependencies"));
        Map<String, @Nullable Object> deps = new LinkedHashMap<>(optionalDependencies);
        deps.putAll(asMap(pkg.get("dependencies")));
        deps = Js.ordered(deps);
        Map<String, @Nullable Object> dependenciesMeta = asMap(pkg.get("dependenciesMeta"));
        List<WantedDependency> wanted = new ArrayList<>();
        for (String alias : Js.keys(deps)) {
            if (bundledDeps.contains(alias)) {
                continue;
            }
            WantedDependency dependency = new WantedDependency(alias, Js.string(deps.get(alias)), false,
                    Js.truthy(optionalDependencies.get(alias)));
            Object meta = dependenciesMeta.get(alias);
            if (meta instanceof Map && ((Map<?, ?>) meta).get("injected") instanceof Boolean) {
                dependency.setInjected((Boolean) ((Map<?, ?>) meta).get("injected"));
            }
            wanted.add(dependency);
        }
        return wanted;
    }

    /**
     * hoistPeers.ts {@code hoistPeers}: the version each missing required peer is installed at.
     */
    static Map<String, String> hoistPeers(boolean autoInstallPeers,
                                          @Nullable Map<String, Map<String, PreferredVersions.Selector>> allPreferred,
                                          List<PkgAddress> workspaceRootDeps,
                                          List<Map.Entry<String, MissingPeerInfo>> missingRequiredPeers) {
        Map<String, String> dependencies = new LinkedHashMap<>();
        for (Map.Entry<String, MissingPeerInfo> peer : missingRequiredPeers) {
            String peerName = peer.getKey();
            String range = peer.getValue().getRange();
            PkgAddress rootDepByAlias = null;
            for (PkgAddress rootDep : workspaceRootDeps) {
                if (rootDep.getAlias().equals(peerName)) {
                    rootDepByAlias = rootDep;
                    break;
                }
            }
            if (rootDepByAlias != null && Js.truthy(rootDepByAlias.getNormalizedBareSpecifier())) {
                dependencies.put(peerName, rootDepByAlias.getNormalizedBareSpecifier());
                continue;
            }
            PkgAddress rootDep = null;
            for (PkgAddress candidate : workspaceRootDeps) {
                if (peerName.equals(candidate.getPkg().get("name")) &&
                    (rootDep == null || KeySorting.lexCompare(candidate.getAlias(), rootDep.getAlias()) < 0)) {
                    rootDep = candidate;
                }
            }
            if (rootDep != null && Js.truthy(rootDep.getNormalizedBareSpecifier())) {
                dependencies.put(peerName, rootDep.getNormalizedBareSpecifier());
                continue;
            }
            Map<String, PreferredVersions.Selector> preferred = allPreferred == null ? null :
                    allPreferred.get(peerName);
            if (preferred != null) {
                List<String> versions = new ArrayList<>();
                List<String> nonVersions = new ArrayList<>();
                for (Map.Entry<String, PreferredVersions.Selector> selector : preferred.entrySet()) {
                    if ("version".equals(selector.getValue().getSelectorType())) {
                        versions.add(selector.getKey());
                    } else {
                        nonVersions.add(selector.getKey());
                    }
                }
                boolean isExactVersion = NodeSemver.valid(range, NodeSemver.Options.DEFAULT) != null;
                String satisfyingVersion = isExactVersion ?
                        NodeSemver.maxSatisfying(versions, range, NodeSemver.Options.INCLUDE_PRERELEASE) : null;
                List<String> specs = new ArrayList<>();
                if (satisfyingVersion != null) {
                    specs.add(satisfyingVersion);
                    specs.addAll(nonVersions);
                    dependencies.put(peerName, String.join(" || ", specs));
                } else if (isExactVersion && autoInstallPeers) {
                    dependencies.put(peerName, range);
                } else {
                    String highest = NodeSemver.maxSatisfying(versions, "*", NodeSemver.Options.INCLUDE_PRERELEASE);
                    if (highest != null) {
                        specs.add(highest);
                    }
                    specs.addAll(nonVersions);
                    dependencies.put(peerName, String.join(" || ", specs));
                }
            } else if (autoInstallPeers) {
                dependencies.put(peerName, range);
            }
        }
        return dependencies;
    }

    /**
     * hoistPeers.ts {@code getHoistableOptionalPeers}: a missing optional peer at the highest version known that every
     * range admits.
     */
    static Map<String, String> getHoistableOptionalPeers(Map<String, List<String>> allMissingOptionalPeers,
                                                         Map<String, Map<String, PreferredVersions.Selector>> allPref) {
        Map<String, String> optionalDependencies = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> peer : allMissingOptionalPeers.entrySet()) {
            Map<String, PreferredVersions.Selector> preferred = allPref.get(peer.getKey());
            if (preferred == null) {
                continue;
            }
            String maxSatisfyingVersion = null;
            for (Map.Entry<String, PreferredVersions.Selector> entry : preferred.entrySet()) {
                // a weighted selector is an object, never the string 'version'
                if (entry.getValue().getWeight() != null || !"version".equals(entry.getValue().getSelectorType())) {
                    continue;
                }
                String version = entry.getKey();
                boolean all = true;
                for (String range : peer.getValue()) {
                    all &= satisfies(version, range);
                }
                if (all && (maxSatisfyingVersion == null ||
                            NodeSemver.compare(version, maxSatisfyingVersion, NodeSemver.Options.DEFAULT) > 0)) {
                    maxSatisfyingVersion = version;
                }
            }
            if (maxSatisfyingVersion != null) {
                optionalDependencies.put(peer.getKey(), maxSatisfyingVersion);
            }
        }
        return optionalDependencies;
    }

    private static PreferredVersions.ByName getExactSinglePreferredVersions(WantedDependency wantedDependency,
                                                                           String version) {
        String pkgName = unwrapPackageName(wantedDependency.getAlias(), wantedDependency.getBareSpecifier())[0];
        Map<String, PreferredVersions.Selector> selectors = new LinkedHashMap<>();
        selectors.put(version, PreferredVersions.Selector.of("version"));
        Map<String, Map<String, PreferredVersions.Selector>> own = new LinkedHashMap<>();
        own.put(pkgName, selectors);
        return PreferredVersions.ByName.of(own);
    }

    /**
     * unwrapPackageName.ts: the real package name and specifier of an {@code npm:} alias.
     */
    static String[] unwrapPackageName(String alias, String originalBareSpecifier) {
        if (!originalBareSpecifier.startsWith("npm:")) {
            return new String[]{alias, originalBareSpecifier};
        }
        String value = originalBareSpecifier.substring(4);
        int index = value.lastIndexOf('@');
        if (index == -1 || index == 0) {
            return new String[]{value, "*"};
        }
        return new String[]{value.substring(0, index), value.substring(index + 1)};
    }

    static String replaceVersionInBareSpecifier(String bareSpecifier, String version,
                                                List<String> namedRegistryPrefixes) {
        if (validRange(bareSpecifier) != null) {
            return version;
        }
        String prefix = null;
        if (bareSpecifier.startsWith("npm:")) {
            prefix = "npm:";
        } else {
            for (String candidate : namedRegistryPrefixes) {
                if (bareSpecifier.startsWith(candidate)) {
                    prefix = candidate;
                    break;
                }
            }
        }
        if (prefix == null) {
            return bareSpecifier;
        }
        if (validRange(bareSpecifier.substring(prefix.length())) != null) {
            return prefix + version;
        }
        int versionDelimiter = bareSpecifier.lastIndexOf('@');
        if (versionDelimiter == -1 || bareSpecifier.indexOf('/') > versionDelimiter) {
            return bareSpecifier + "@" + version;
        }
        return bareSpecifier.substring(0, versionDelimiter + 1) + version;
    }

    static boolean parentIdsContainSequence(List<String> pkgIds, String pkgId1, String pkgId2) {
        int pkg1Index = pkgIds.indexOf(pkgId1);
        if (pkg1Index == -1 || pkg1Index == pkgIds.size() - 1) {
            return false;
        }
        int pkg2Index = pkgIds.lastIndexOf(pkgId2);
        return pkg1Index < pkg2Index && pkg2Index != pkgIds.size() - 1;
    }

    private static boolean wantedDepIsLocallyAvailable(Map<String, Map<String, Verification.Project>> workspacePackages,
                                                       WantedDependency wantedDependency, String defaultTag,
                                                       String registry) {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier(
                wantedDependency.getBareSpecifier(), wantedDependency.getAlias(),
                defaultTag.isEmpty() ? "latest" : defaultTag, registry);
        if (spec == null || !workspacePackages.containsKey(spec.getName())) {
            return false;
        }
        List<String> localVersions = new ArrayList<>(workspacePackages.get(spec.getName()).keySet());
        switch (spec.getType()) {
            case "tag":
                return NodeSemver.maxSatisfying(localVersions, "*", NodeSemver.Options.DEFAULT) != null;
            case "version":
                return localVersions.contains(spec.getFetchSpec());
            case "range":
                return NodeSemver.maxSatisfying(localVersions, spec.getFetchSpec(), NodeSemver.Options.LOOSE) != null;
            default:
                return false;
        }
    }

    static @Nullable String validRange(String range) {
        try {
            return NodeSemver.validRange(range, NodeSemver.Options.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean satisfiesLoose(@Nullable String version, String range) {
        if (version == null) {
            return false;
        }
        try {
            return NodeSemver.satisfies(version, range, NodeSemver.Options.LOOSE);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean satisfies(String version, String range) {
        try {
            return NodeSemver.satisfies(version, range, NodeSemver.Options.DEFAULT);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> asMap(@Nullable Object value) {
        return value instanceof Map ? (Map<String, @Nullable Object>) value : Collections.emptyMap();
    }
}
