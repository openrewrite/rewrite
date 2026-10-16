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
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Port of @pnpm/installing.deps-resolver src/resolveDependencies.ts (pnpm 11.2.2) with the small modules it calls:
 * getNonDevWantedDependencies, hoistPeers, replaceVersionInBareSpecifier, unwrapPackageName and their siblings.
 * hoistPeers.ts' getHoistableOptionalPeers follows the release that installs
 * ({@link ResolutionContext#getPnpmVersion()}): 11.2.2's up to 11.5.0, 11.5.1's from 11.5.1
 * ({@link #LOCKED_OPTIONAL_PEER_CANDIDATES}). From 11.7.0 a package's children are resolved by the occurrence that
 * claims them and no request starts while a shallower one is active, which moved buildTree here from
 * resolveDependencyTree.ts ({@link PnpmRelease#resolvesSharedChildrenInADeterministicContext}).
 */
final class ResolveDependencies {

    private static final Set<String> NON_EXOTIC_RESOLVED_VIA = new LinkedHashSet<>(Arrays.asList(
            "custom-resolver", "github.com/denoland/deno", "github.com/oven-sh/bun", "jsr-registry",
            "local-filesystem", "named-registry", "nodejs.org", "npm-registry", "workspace"));

    // Number.MAX_SAFE_INTEGER, the order of an importer the install does not name
    private static final long MAX_SAFE_INTEGER = (1L << 53) - 1;

    /**
     * The releases whose getHoistableOptionalPeers reads a selector with a weight by its type, as hoistPeers does
     * (pnpm/pnpm#12075, released in 11.5.1): a version only the lock holds is then a candidate for a missing
     * optional peer. An earlier release compares the selector itself with {@code 'version'}, which one with a weight
     * never is. Recorded with 11.2.2, 11.5.0, 11.5.1, 11.15.1 and 11.28.5.
     */
    static final String LOCKED_OPTIONAL_PEER_CANDIDATES = ">=11.5.1";

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

        /**
         * From 11.7.0: the claim on the package's children the occurrence found ({@link ChildrenResolution#getId()}).
         */
        private long childrenResolutionId;

        private @Nullable String publishedAt;
        private @Nullable String saveCatalogName;
        private boolean dev;
        private @Nullable Map<String, @Nullable Object> resolution;
        private @Nullable String name;

        /**
         * From 11.5.2: the dep path the lock records for a package the install does not update, and the peers it
         * records it with ({@link PnpmRelease#prefersThePeerContextsTheLockRecords}).
         */
        private @Nullable String previousDepPath;
        private @Nullable Map<String, String> lockedPeerContext;

        /**
         * From 11.11.0: a resolved peer attached to its importer for reuse under autoInstallPeers, which resolvePeers
         * resolves after the importer's own dependencies
         * ({@link PnpmRelease#resolvesHoistedPeerProvidersAfterOwnDependencies}).
         */
        private boolean hoistedPeerProvider;

        PkgAddress(String alias, String pkgId) {
            this.alias = alias;
            this.pkgId = pkgId;
        }

        // {...address, hoistedPeerProvider: true}
        PkgAddress asHoistedPeerProvider() {
            PkgAddress copy = new PkgAddress(alias, pkgId);
            copy.optional = optional;
            copy.pkg = pkg;
            copy.normalizedBareSpecifier = normalizedBareSpecifier;
            copy.catalogLookup = catalogLookup;
            copy.nodeId = nodeId;
            copy.depIsLinked = depIsLinked;
            copy.isNew = isNew;
            copy.isLinkedDependency = isLinkedDependency;
            copy.resolvedVia = resolvedVia;
            copy.installable = installable;
            copy.version = version;
            copy.updated = updated;
            copy.rootDir = rootDir;
            copy.missingPeers = missingPeers;
            copy.missingPeersOfChildren = missingPeersOfChildren;
            copy.childrenResolutionId = childrenResolutionId;
            copy.publishedAt = publishedAt;
            copy.saveCatalogName = saveCatalogName;
            copy.dev = dev;
            copy.resolution = resolution;
            copy.name = name;
            copy.previousDepPath = previousDepPath;
            copy.lockedPeerContext = lockedPeerContext;
            copy.hoistedPeerProvider = true;
            return copy;
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

        void reject(RuntimeException error) {
            promise.reject(error);
        }

        boolean isResolved() {
            return resolved;
        }

        Promise<Map<String, MissingPeerInfo>> get() {
            return promise;
        }
    }

    /**
     * {@code missingPeersOfChildrenByPkgId}'s entry: the depth of the occurrence that holds the claim on a package's
     * children, and their missing peers. 11.2.2 shares the missing peers from here, 11.7.0 from the claim, which
     * gives the same where no claim is taken over; the port reads the claim's for every release.
     */
    @Value
    static class MissingPeersOfChildrenAtDepth {
        int depth;
        MissingPeersOfChildren missingPeersOfChildren;
    }

    /**
     * An occurrence of a package as 11.7.0 orders those that claim its children: by depth, by its importer's place in
     * the install, then by the ids of its parents.
     */
    @Value
    static class ChildrenResolutionOwner {
        int depth;
        long importerOrder;
        List<String> parentPath;
    }

    /**
     * {@code childrenResolutionByPkgId}'s entry (11.7.0): the claim in force on a package's children, the occurrence
     * that holds it, and the missing peers of the children it resolves.
     */
    @Value
    static class ChildrenResolution {
        long id;
        ChildrenResolutionOwner owner;

        @Nullable
        MissingPeersOfChildren missingPeersOfChildren;
    }

    /**
     * What claimChildrenResolution gives an occurrence (11.7.0): the claim in force, whether the occurrence holds it,
     * and the missing peers of children it shares.
     */
    @Value
    static class ChildrenResolutionClaim {
        long id;
        boolean owner;

        @Nullable
        MissingPeersOfChildren missingPeersOfChildren;
    }

    /**
     * {@code nodeResolutionContextByNodeId}'s entry (11.7.0): where a node stands that builds its children from those
     * its package's current owner resolves.
     */
    @Value
    static class NodeResolutionContext {
        int depth;
        boolean installable;
        List<String> parentIds;
        String pkgId;
    }

    /**
     * {@code packageResolutionBarrier} (11.7.0): the package resolutions active at each depth, and the requests that
     * wait for a shallower one to end.
     */
    @Getter
    static final class PackageResolutionBarrier {
        private final Map<Integer, Integer> activeByDepth = new LinkedHashMap<>();
        private final List<Promise<Void>> waiters = new ArrayList<>();
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

        @Nullable
        Map<String, String> lockedPeerContext;

        @Nullable
        String previousDepPath;
    }

    /**
     * {@code DependenciesTreeNode}: the children by alias (computed when first asked for), installability, the package
     * and the depth it was first reached at ({@code -1} for a link). From 11.5.2 it carries the dep path the lock
     * records for it, the peers the lock records it with, and the children whose provider the manifests changed.
     */
    @Getter
    @Setter
    static final class DependenciesTreeNode {
        private @Nullable Supplier<Map<String, NodeId>> lazyChildren;
        private @Nullable Map<String, NodeId> children;
        private boolean installable;
        private final ResolvedPackage resolvedPackage;
        private int depth;
        private @Nullable Map<String, String> lockedPeerContext;
        private @Nullable String previousDepPath;
        private @Nullable Set<String> dependencyNamesWhoseCurrentProviderMustWin;

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

        /**
         * From 11.5.2 (pnpm 11.2.2's getInfoFromLockfile names none): the dep path, and the peers the lock records
         * the package with.
         */
        private @Nullable String depPath;
        private @Nullable Map<String, String> lockedPeerContext;

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

        /**
         * {@code pnpmVersion}, the install's {@code packageManager.version}: the release that installs, a range
         * where the inputs do not say which release of it does.
         */
        private String pnpmVersion = Main.PNPM_VERSION;

        /**
         * The release that installs, which the resolver's modules ask what it does.
         */
        private PnpmRelease release = PnpmRelease.PORTED;

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

        /**
         * From 11.7.0 (pnpm/pnpm#12358, {@link PnpmRelease#resolvesSharedChildrenInADeterministicContext}): the package
         * resolutions active at each depth, the claims on each package's children, each importer's place in the
         * install, and the nodes whose children are built from those of their package's current owner. Before 11.7.0
         * they are kept all the same, where only a release from 11.7.0 waits on them or takes a claim over.
         */
        private final PackageResolutionBarrier packageResolutionBarrier = new PackageResolutionBarrier();
        private final Map<String, ChildrenResolution> childrenResolutionByPkgId = new LinkedHashMap<>();
        private long childrenResolutionId;
        private final Map<String, Integer> importerResolutionOrder = new LinkedHashMap<>();
        private final Map<NodeId, NodeResolutionContext> nodeResolutionContextByNodeId = new LinkedHashMap<>();

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

        long nextChildrenResolutionId() {
            return ++childrenResolutionId;
        }
    }

    static Promise<RootResult> resolveRootDependencies(ResolutionContext ctx, List<ImporterToResolve> importers) {
        if (ctx.isAutoInstallPeers()) {
            ctx.setAllPreferredVersions(PreferredVersions.getPreferredVersionsFromLockfileAndManifests(
                    ctx.getWantedLockfile().getPackages(), Collections.emptyList(), ctx.getRelease()));
        } else if (ctx.isHoistPeers()) {
            ctx.setAllPreferredVersions(new LinkedHashMap<>());
        }
        return resolveDependenciesOfImporters(ctx, importers).then(ofImporters -> {
            List<PkgAddressesByImporter> byImporters = ofImporters.getPkgAddressesByImportersWithoutPeers();
            Map<String, String> time = ofImporters.getTime();
            if (!ctx.isHoistPeers()) {
                return Promise.resolved(ctx.getLoop(), new RootResult(addressesOf(byImporters), time));
            }
            WorkspaceRootDeps workspaceRootDeps = new WorkspaceRootDeps(new ArrayList<>(), new ArrayList<>());
            if (ctx.isResolvePeersFromWorkspaceRoot()) {
                for (int i = 0; i < importers.size(); i++) {
                    if (".".equals(importers.get(i).getOptions().getParentIds().get(0))) {
                        // 11.2.2 reads the root's own list, which the loop below grows; 11.12.0 a copy of it
                        // (PnpmRelease#copiesTheRootDependenciesPeersAreHoistedAs)
                        List<PkgAddress> live = byImporters.get(i).getPkgAddresses();
                        workspaceRootDeps = new WorkspaceRootDeps(live, hoistableRootDeps(importers.get(i), live));
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
                                                   WorkspaceRootDeps workspaceRootDeps,
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
                            ctx.getAllPreferredVersions(), ctx.getPnpmVersion());
                    if (optionalDependencies.isEmpty()) {
                        return Promise.resolved(ctx.getLoop(), null);
                    }
                    hasNewMissingPeers[0] = true;
                    List<WantedDependency> wantedDependencies = getNonDevWantedDependencies(
                            Collections.<String, @Nullable Object>singletonMap("optionalDependencies",
                                    optionalDependencies), ctx.getRelease());
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
            WorkspaceRootDeps workspaceRootDeps, @Nullable Instant publishedBy,
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
                        // 11.11.0 marks a copy, which no release before reads apart from the address itself
                        result.getPkgAddresses().add(resolved.getValue().asHoistedPeerProvider());
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
            Map<String, String> dependencies = workspaceRootDeps.hoistPeers(ctx.isAutoInstallPeers(),
                    ctx.getAllPreferredVersions(), missingRequiredPeers, ctx.getRelease());
            if (dependencies.isEmpty()) {
                return Promise.resolved(ctx.getLoop(), allMissingOptionalPeers);
            }
            List<WantedDependency> wantedDependencies = getNonDevWantedDependencies(
                    Collections.<String, @Nullable Object>singletonMap("dependencies", dependencies),
                    ctx.getRelease());
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
                        importer.getOptions().getResolvedDependencies(), ctx.getRelease());
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
                    options.isProceed() || ctx.isForceFullResolution(), options.getResolvedDependencies(),
                    preferredVersions, ctx.getRelease());
            List<Function<PostponedResolutionOpts, Promise<PeersResolutionResult>>> postponedResolutionsQueue =
                    new ArrayList<>();
            List<Function<ParentPkgAliases, Promise<PeersResolutionResult>>> postponedPeersResolutionQueue =
                    new ArrayList<>();
            List<PkgAddress> pkgAddresses = new ArrayList<>();
            if (extendedWantedDeps.isEmpty() || ctx.getRelease().takesChildrenInTheOrderWanted()) {
                // 11.3.0 (pnpm/pnpm#8155): Promise.all's results, then the queues in the order wanted
                List<Promise<DependencyResolution>> resolving = new ArrayList<>();
                for (ExtendedWantedDependency extendedWantedDep : extendedWantedDeps) {
                    resolving.add(Promise.async(ctx.getLoop(), () -> resolveDependenciesOfDependency(ctx,
                            preferredVersions, options, extendedWantedDep)));
                }
                return Promise.all(ctx.getLoop(), resolving).then(resolved -> {
                    for (DependencyResolution result : resolved) {
                        if (result.getResolveDependencyResult() != null) {
                            pkgAddresses.add(result.getResolveDependencyResult());
                        }
                        if (result.getPostponedResolution() != null) {
                            postponedResolutionsQueue.add(result.getPostponedResolution());
                        }
                        if (result.getPostponedPeersResolution() != null) {
                            postponedPeersResolutionQueue.add(result.getPostponedPeersResolution());
                        }
                    }
                    return resolvePostponed(ctx, preferredVersions, options, pkgAddresses, postponedResolutionsQueue,
                            postponedPeersResolutionQueue);
                });
            }
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
                                 ctx.getRegistries().get("default"), ctx.getRelease()) ||
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
            // 11.5.2: what the lock records of a package the install does not update
            if (!update && info != null) {
                resolveDependencyResult.setPreviousDepPath(info.getDepPath());
                resolveDependencyResult.setLockedPeerContext(info.getLockedPeerContext());
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
            String pkgId = resolveDependencyResult.getPkgId();
            long childrenResolutionId = resolveDependencyResult.getChildrenResolutionId();
            return Promise.resolved(ctx.getLoop(), new DependencyResolution(resolveDependencyResult,
                    postponedResolutionOpts -> {
                        // 11.7.0: a claim a lower occurrence took over leaves the children to that occurrence
                        if (!isCurrentChildrenResolution(ctx, pkgId, childrenResolutionId)) {
                            setDependencyTreeNodeWithCurrentChildren(ctx, parentDepth, parentIds,
                                    resolveDependencyResult);
                            return resolveMissingPeersFromCurrentChildrenResolution(ctx, pkgId,
                                    postponedResolutionOpts.getParentPkgAliases());
                        }
                        return resolveChildren(ctx, resolveDependencyResult, parentIds, dependencyLockfile,
                                parentDepth, updateDepth, options.getPrefix(), postponedResolutionOpts).then(result -> {
                            if (!isCurrentChildrenResolution(ctx, pkgId, childrenResolutionId)) {
                                return resolveMissingPeersFromCurrentChildrenResolution(ctx, pkgId,
                                        postponedResolutionOpts.getParentPkgAliases());
                            }
                            if (resolveDependencyResult.getMissingPeersOfChildren() != null) {
                                resolveDependencyResult.getMissingPeersOfChildren().resolve(result.getMissingPeers());
                            }
                            return Promise.resolved(ctx.getLoop(),
                                    filterMissingPeers(result, postponedResolutionOpts.getParentPkgAliases()));
                        });
                    }, null));
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
        // 11.7.0: a claim a lower occurrence took over builds the node from the children that occurrence resolves
        if (!isCurrentChildrenResolution(ctx, parentPkg.getPkgId(), parentPkg.getChildrenResolutionId())) {
            setDependencyTreeNodeWithCurrentChildren(ctx, parentDepth, parentIds, parentPkg);
            return Promise.resolved(ctx.getLoop(), new PeersResolutionResult(new LinkedHashMap<>(),
                    new LinkedHashMap<>()));
        }
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
        List<WantedDependency> wantedDependencies = getNonDevWantedDependencies(parentPkg.getPkg(), ctx.getRelease());
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
            if (!isCurrentChildrenResolution(ctx, parentPkg.getPkgId(), parentPkg.getChildrenResolutionId())) {
                setDependencyTreeNodeWithCurrentChildren(ctx, parentDepth, parentIds, parentPkg);
                return result.getResolvingPeers();
            }
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
            node.setDependencyNamesWhoseCurrentProviderMustWin(
                    dependencyNamesWhoseCurrentProviderMustWin(result.getPkgAddresses(), dependencyLockfile));
            node.setLockedPeerContext(parentPkg.getLockedPeerContext());
            node.setPreviousDepPath(parentPkg.getPreviousDepPath());
            ctx.getDependenciesTree().put(parentPkg.getNodeId(), node);
            ctx.getNodeResolutionContextByNodeId().put(parentPkg.getNodeId(), new NodeResolutionContext(parentDepth,
                    parentPkg.isInstallable(), parentIds, parentPkg.getPkgId()));
            updateChildrenResolutionNodes(ctx, parentPkg.getPkgId(), parentPkg.getNodeId());
            return result.getResolvingPeers();
        });
    }

    // startPackageResolution (11.7.0): the package's resolution is active at its depth until the function it gives is
    // run, once, which wakes every request waiting for its turn
    private static Runnable startPackageResolution(ResolutionContext ctx, int depth) {
        PackageResolutionBarrier barrier = ctx.getPackageResolutionBarrier();
        Integer activeCount = barrier.getActiveByDepth().get(depth);
        barrier.getActiveByDepth().put(depth, (activeCount == null ? 0 : activeCount) + 1);
        boolean[] finished = {false};
        return () -> {
            if (finished[0]) {
                return;
            }
            finished[0] = true;
            Integer count = barrier.getActiveByDepth().get(depth);
            int nextActiveCount = (count == null ? 1 : count) - 1;
            if (nextActiveCount > 0) {
                barrier.getActiveByDepth().put(depth, nextActiveCount);
            } else {
                barrier.getActiveByDepth().remove(depth);
            }
            List<Promise<Void>> waiters = new ArrayList<>(barrier.getWaiters());
            barrier.getWaiters().clear();
            for (Promise<Void> waiter : waiters) {
                waiter.resolve(null);
            }
        };
    }

    /**
     * waitForPackageResolutionTurn (11.7.0): what a request at {@code depth} waits for while a shallower package's
     * resolution is active, checked again each time one ends; null where it waits for nothing and is made at once.
     * Before 11.7.0 no request waits, which only a shallower active resolution tells apart. 11.7.0 makes a request
     * that waits for nothing an await later, as it makes every such request, which leaves their order as it is.
     */
    private static @Nullable Promise<Void> waitForPackageResolutionTurn(ResolutionContext ctx, int depth) {
        if (!hasActivePackageResolutionBeforeDepth(ctx, depth) ||
            !ctx.getRelease().resolvesSharedChildrenInADeterministicContext()) {
            return null;
        }
        Promise<Void> turn = new Promise<>(ctx.getLoop());
        ctx.getPackageResolutionBarrier().getWaiters().add(turn);
        return turn.then(v -> {
            Promise<Void> next = waitForPackageResolutionTurn(ctx, depth);
            return next != null ? next : Promise.resolved(ctx.getLoop(), null);
        });
    }

    private static boolean hasActivePackageResolutionBeforeDepth(ResolutionContext ctx, int depth) {
        for (Map.Entry<Integer, Integer> active : ctx.getPackageResolutionBarrier().getActiveByDepth().entrySet()) {
            if (active.getKey() < depth && active.getValue() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * claimChildrenResolution (11.7.0): the claim on the package's children of the occurrence at
     * {@code currentDepth} under {@code parentIds}. The first occurrence to resolve claims them, and from 11.7.0 a
     * lower one ({@link #compareChildrenResolutionOwners}) takes them over, its missing peers of children settling
     * the previous owner's; a release before 11.7.0, whose first occurrence keeps them, is told apart there alone. An
     * occurrence that holds no claim shares the owner's missing peers of children, as 11.2.2 shares the first's.
     */
    private static ChildrenResolutionClaim claimChildrenResolution(ResolutionContext ctx, int currentDepth,
                                                                   List<String> parentIds, String pkgId) {
        Integer importerOrder = ctx.getImporterResolutionOrder().get(parentIds.get(0));
        ChildrenResolutionOwner owner = new ChildrenResolutionOwner(currentDepth,
                importerOrder == null ? MAX_SAFE_INTEGER : importerOrder, parentIds);
        ChildrenResolution existing = ctx.getChildrenResolutionByPkgId().get(pkgId);
        if (existing == null || compareChildrenResolutionOwners(owner, existing.getOwner()) < 0 &&
                                ctx.getRelease().resolvesSharedChildrenInADeterministicContext()) {
            MissingPeersOfChildren previousMissingPeersOfChildren = existing == null ? null :
                    existing.getMissingPeersOfChildren();
            MissingPeersOfChildren missingPeersOfChildren = ctx.isHoistPeers() && !parentIds.contains(pkgId) ?
                    new MissingPeersOfChildren(ctx.getLoop()) : null;
            ChildrenResolution resolution = new ChildrenResolution(ctx.nextChildrenResolutionId(), owner,
                    missingPeersOfChildren);
            ctx.getChildrenResolutionByPkgId().put(pkgId, resolution);
            if (missingPeersOfChildren != null) {
                ctx.getMissingPeersOfChildrenByPkgId().put(pkgId, new MissingPeersOfChildrenAtDepth(owner.getDepth(),
                        missingPeersOfChildren));
            }
            if (previousMissingPeersOfChildren != null) {
                if (missingPeersOfChildren != null) {
                    missingPeersOfChildren.get().then(missingPeers -> {
                        previousMissingPeersOfChildren.resolve(missingPeers);
                        return Promise.<Void>resolved(ctx.getLoop(), null);
                    }).recover(e -> {
                        previousMissingPeersOfChildren.reject(e);
                        return Promise.resolved(ctx.getLoop(), null);
                    });
                } else {
                    previousMissingPeersOfChildren.resolve(new LinkedHashMap<>());
                }
            }
            return new ChildrenResolutionClaim(resolution.getId(), true, missingPeersOfChildren);
        }
        MissingPeersOfChildren missingPeersOfChildren = null;
        // from 11.9.0 an occurrence deeper than the owner no longer shares the missing peers of children the owner
        // resolved already (PnpmRelease#sharesMissingPeersOfChildrenOnlyWithAnOwnerAsDeep)
        MissingPeersOfChildren owners = existing.getMissingPeersOfChildren();
        if (ctx.isHoistPeers() && !parentIds.contains(pkgId) && owners != null &&
            (existing.getOwner().getDepth() >= currentDepth ||
             owners.isResolved() && !ctx.getRelease().sharesMissingPeersOfChildrenOnlyWithAnOwnerAsDeep())) {
            missingPeersOfChildren = owners;
        }
        return new ChildrenResolutionClaim(existing.getId(), false, missingPeersOfChildren);
    }

    // compareChildrenResolutionOwners (11.7.0): by depth, by importer, by each parent's id in turn, then the shorter
    // path first
    private static int compareChildrenResolutionOwners(ChildrenResolutionOwner owner1, ChildrenResolutionOwner owner2) {
        if (owner1.getDepth() != owner2.getDepth()) {
            return Integer.compare(owner1.getDepth(), owner2.getDepth());
        }
        if (owner1.getImporterOrder() != owner2.getImporterOrder()) {
            return Long.compare(owner1.getImporterOrder(), owner2.getImporterOrder());
        }
        int pathLength = Math.min(owner1.getParentPath().size(), owner2.getParentPath().size());
        for (int i = 0; i < pathLength; i++) {
            int result = KeySorting.lexCompare(owner1.getParentPath().get(i), owner2.getParentPath().get(i));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(owner1.getParentPath().size(), owner2.getParentPath().size());
    }

    // isCurrentChildrenResolution (11.7.0): whether the claim is still in force, which only a release from 11.7.0
    // takes over
    private static boolean isCurrentChildrenResolution(ResolutionContext ctx, String pkgId, long childrenResolutionId) {
        ChildrenResolution current = ctx.getChildrenResolutionByPkgId().get(pkgId);
        return current != null && current.getId() == childrenResolutionId;
    }

    // resolveMissingPeersFromCurrentChildrenResolution (11.7.0): the missing peers of the children the package's
    // current owner resolves, as a claim it took over reports them
    private static Promise<PeersResolutionResult> resolveMissingPeersFromCurrentChildrenResolution(
            ResolutionContext ctx, String pkgId, ParentPkgAliases parentPkgAliases) {
        ChildrenResolution current = ctx.getChildrenResolutionByPkgId().get(pkgId);
        MissingPeersOfChildren missingPeersOfChildren = current == null ? null : current.getMissingPeersOfChildren();
        if (missingPeersOfChildren == null) {
            return Promise.resolved(ctx.getLoop(), new PeersResolutionResult(new LinkedHashMap<>(),
                    new LinkedHashMap<>()));
        }
        return missingPeersOfChildren.get().then(missingPeers -> Promise.resolved(ctx.getLoop(),
                filterMissingPeers(new PeersResolutionResult(missingPeers, new LinkedHashMap<>()), parentPkgAliases)));
    }

    // setDependencyTreeNodeWithCurrentChildren (11.7.0): the node of an occurrence whose claim a lower one took over,
    // its children built from those the package's current owner resolves
    private static void setDependencyTreeNodeWithCurrentChildren(ResolutionContext ctx, int parentDepth,
                                                                 List<String> parentIds, PkgAddress parentPkg) {
        String pkgId = parentPkg.getPkgId();
        boolean installable = parentPkg.isInstallable();
        DependenciesTreeNode node = new DependenciesTreeNode(ctx.getResolvedPkgsById().get(pkgId), parentDepth);
        node.setLazyChildren(() -> buildTree(ctx, pkgId, parentIds, currentChildren(ctx, pkgId), parentDepth + 1,
                installable));
        node.setInstallable(installable);
        node.setLockedPeerContext(parentPkg.getLockedPeerContext());
        node.setPreviousDepPath(parentPkg.getPreviousDepPath());
        ctx.getDependenciesTree().put(parentPkg.getNodeId(), node);
        ctx.getNodeResolutionContextByNodeId().put(parentPkg.getNodeId(), new NodeResolutionContext(parentDepth,
                installable, parentIds, pkgId));
    }

    // updateChildrenResolutionNodes (11.7.0): every other node of the package builds its children from those the
    // occurrence at currentNodeId resolved
    private static void updateChildrenResolutionNodes(ResolutionContext ctx, String pkgId,
                                                      @Nullable NodeId currentNodeId) {
        for (Map.Entry<NodeId, NodeResolutionContext> resolution :
                ctx.getNodeResolutionContextByNodeId().entrySet()) {
            NodeResolutionContext nodeContext = resolution.getValue();
            if (resolution.getKey().equals(currentNodeId) || !nodeContext.getPkgId().equals(pkgId)) {
                continue;
            }
            DependenciesTreeNode node = ctx.getDependenciesTree().get(resolution.getKey());
            if (node == null || node.getDepth() == -1) {
                continue;
            }
            node.setChildren(null);
            node.setLazyChildren(() -> buildTree(ctx, pkgId, nodeContext.getParentIds(), currentChildren(ctx, pkgId),
                    nodeContext.getDepth() + 1, nodeContext.isInstallable()));
        }
    }

    // ctx.childrenByParentId[pkgId] ?? []
    private static List<ChildId> currentChildren(ResolutionContext ctx, String pkgId) {
        List<ChildId> children = ctx.getChildrenByParentId().get(pkgId);
        return children == null ? Collections.emptyList() : children;
    }

    /**
     * buildTree, which 11.7.0 moved here from resolveDependencyTree.ts: the nodes of a package's children, each of
     * which builds its own children when first asked for.
     */
    static Map<String, NodeId> buildTree(ResolutionContext ctx, String parentId, List<String> parentIds,
                                         @Nullable List<ChildId> children, int depth, boolean installable) {
        Map<String, NodeId> childrenNodeIds = new LinkedHashMap<>();
        if (children == null) {
            return childrenNodeIds;
        }
        boolean installableSoFar = installable;
        for (ChildId child : children) {
            String alias = child.getAlias();
            String id = child.getId();
            if (id.startsWith("link:")) {
                childrenNodeIds.put(alias, NodeId.of(id));
                continue;
            }
            if (parentIdsContainSequence(parentIds, parentId, id) || parentId.equals(id)) {
                continue;
            }
            ResolvedPackage resolved = ctx.getResolvedPkgsById().get(id);
            if (resolved.isLeaf()) {
                childrenNodeIds.put(alias, NodeId.of(id));
                continue;
            }
            NodeId childNodeId = ctx.nextNodeId();
            childrenNodeIds.put(alias, childNodeId);
            installableSoFar = installableSoFar || !ctx.getSkipped().contains(id);
            boolean childInstallable = installableSoFar;
            List<String> childParentIds = new ArrayList<>(parentIds);
            childParentIds.add(id);
            DependenciesTreeNode node = new DependenciesTreeNode(resolved, depth);
            node.setInstallable(childInstallable);
            node.setLazyChildren(() -> buildTree(ctx, id, childParentIds, ctx.getChildrenByParentId().get(id),
                    depth + 1, childInstallable));
            ctx.getDependenciesTree().put(childNodeId, node);
        }
        return childrenNodeIds;
    }

    // the children whose provider is not the one the lock records under the parent (11.5.2): a new child, a linked
    // one, or one resolved to another dep path
    private static Set<String> dependencyNamesWhoseCurrentProviderMustWin(List<PkgAddress> pkgAddresses,
                                                                          @Nullable PackageSnapshot dependencyLockfile) {
        Set<String> names = new LinkedHashSet<>();
        for (PkgAddress child : pkgAddresses) {
            String previousRef = null;
            if (dependencyLockfile != null && dependencyLockfile.getDependencies() != null) {
                previousRef = dependencyLockfile.getDependencies().get(child.getAlias());
            }
            if (previousRef == null && dependencyLockfile != null &&
                dependencyLockfile.getOptionalDependencies() != null) {
                previousRef = dependencyLockfile.getOptionalDependencies().get(child.getAlias());
            }
            if (previousRef == null || child.isLinkedDependency() ||
                !Objects.equals(child.getPreviousDepPath(), DepPath.refToRelative(previousRef, child.getAlias()))) {
                names.add(child.getAlias());
            }
        }
        return names;
    }

    static List<ExtendedWantedDependency> getDepsToResolve(List<WantedDependency> wantedDependencies,
                                                           LockfileObject wantedLockfile,
                                                           Map<String, String> registries,
                                                           @Nullable Map<String, @Nullable String> preferredDeps,
                                                           boolean proceed,
                                                           @Nullable Map<String, @Nullable String> resolvedDeps,
                                                           PnpmRelease release) {
        return getDepsToResolve(wantedDependencies, wantedLockfile, registries, preferredDeps, proceed, resolvedDeps,
                null, release);
    }

    /**
     * {@code getDepsToResolve} of a package's children, whose {@code preferredVersions} 11.8.0 reads: a locked pin a
     * direct dependency's higher version in range supersedes is resolved again to that version
     * ({@link PnpmRelease#movesAStalePinUpToADirectVersion}).
     */
    static List<ExtendedWantedDependency> getDepsToResolve(List<WantedDependency> wantedDependencies,
                                                           LockfileObject wantedLockfile,
                                                           Map<String, String> registries,
                                                           @Nullable Map<String, @Nullable String> preferredDeps,
                                                           boolean proceed,
                                                           @Nullable Map<String, @Nullable String> resolvedDeps,
                                                           PreferredVersions.@Nullable ByName preferredVersions,
                                                           PnpmRelease release) {
        Map<String, @Nullable String> resolvedDependencies = resolvedDeps == null ? Collections.emptyMap() :
                resolvedDeps;
        Map<String, @Nullable String> preferredDependencies = preferredDeps == null ? Collections.emptyMap() :
                preferredDeps;
        List<ExtendedWantedDependency> extendedWantedDeps = new ArrayList<>();
        boolean proceedAll = proceed;
        for (WantedDependency wantedDependency : wantedDependencies) {
            String reference = null;
            String preferredVersion = null;
            boolean proceedThis = proceedAll;
            String alias = wantedDependency.getAlias();
            if (alias != null && !alias.isEmpty()) {
                String resolved = resolvedDependencies.get(alias);
                String preferred = preferredDependencies.get(alias);
                if (Js.truthy(resolved) && (referenceSatisfiesWantedSpec(wantedLockfile, wantedDependency, resolved,
                        release) ||
                                            resolved.startsWith("file:"))) {
                    String[] pinned = resolved.startsWith("file:") ? null :
                            getPinnedNameVer(wantedLockfile, resolved, alias);
                    String higherDirectVersion = pinned == null ? null : findHigherDirectDepVersion(preferredVersions,
                            pinned[0], pinned[1], wantedDependency.getBareSpecifier(), release);
                    if (higherDirectVersion != null && release.movesAStalePinUpToADirectVersion()) {
                        proceedThis = true;
                        preferredVersion = higherDirectVersion;
                    } else {
                        reference = resolved;
                    }
                } else if (validRange(wantedDependency.getBareSpecifier(), release) != null && Js.truthy(preferred) &&
                           referenceSatisfiesWantedSpec(wantedLockfile, wantedDependency, preferred, release)) {
                    proceedThis = true;
                    reference = preferred;
                }
            }
            InfoFromLockfile infoFromLockfile = getInfoFromLockfile(wantedLockfile, registries, reference, alias,
                    release);
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
            ExtendedWantedDependency extended = new ExtendedWantedDependency(infoFromLockfile, proceedThis,
                    wantedDependency);
            extended.setPreferredVersion(preferredVersion);
            extendedWantedDeps.add(extended);
        }
        return extendedWantedDeps;
    }

    // getPinnedNameVer (11.8.0): the name and version of the package a locked reference pins, null for none
    private static String @Nullable [] getPinnedNameVer(LockfileObject lockfile, String reference, String alias) {
        String depPath = DepPath.refToRelative(reference, alias);
        if (depPath == null || lockfile.getPackages() == null) {
            return null;
        }
        PackageSnapshot pkgSnapshot = lockfile.getPackages().get(depPath);
        return pkgSnapshot == null ? null : nameVerFromPkgSnapshot(depPath, pkgSnapshot);
    }

    // findHigherDirectDepVersion (11.8.0): the highest version a direct dependency names that is above the pinned one
    // and in the wanted range
    private static @Nullable String findHigherDirectDepVersion(PreferredVersions.@Nullable ByName preferredVersions,
                                                               @Nullable String pinnedName,
                                                               @Nullable String pinnedVersion, String bareSpecifier,
                                                               PnpmRelease release) {
        if (preferredVersions == null || pinnedName == null || pinnedVersion == null ||
            NodeSemver.valid(pinnedVersion, NodeSemver.Options.DEFAULT) == null ||
            validRange(bareSpecifier, release) == null) {
            return null;
        }
        Map<String, PreferredVersions.Selector> selectors = preferredVersions.get(pinnedName);
        if (selectors == null) {
            return null;
        }
        String best = null;
        for (Map.Entry<String, PreferredVersions.Selector> selector : selectors.entrySet()) {
            String candidate = selector.getKey();
            Long weight = selector.getValue().getWeight();
            if (weight != null && "version".equals(selector.getValue().getSelectorType()) &&
                weight == PreferredVersions.DIRECT_DEP_SELECTOR_WEIGHT &&
                NodeSemver.valid(candidate, NodeSemver.Options.DEFAULT) != null &&
                NodeSemver.compare(candidate, pinnedVersion, NodeSemver.Options.DEFAULT) > 0 &&
                release.underSemver(semver -> NodeSemver.satisfies(candidate, bareSpecifier,
                        NodeSemver.Options.LOOSE.release(semver))) &&
                (best == null || NodeSemver.compare(candidate, best, NodeSemver.Options.DEFAULT) > 0)) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean hasTransitivePeers(PackageSnapshot snapshot) {
        return snapshot.getTransitivePeerDependencies() != null && !snapshot.getTransitivePeerDependencies().isEmpty();
    }

    private static boolean referenceSatisfiesWantedSpec(LockfileObject lockfile, WantedDependency wantedDep,
                                                        String preferredRef, PnpmRelease release) {
        String depPath = DepPath.refToRelative(preferredRef, wantedDep.getAlias());
        if (depPath == null) {
            return false;
        }
        PackageSnapshot pkgSnapshot = lockfile.getPackages() == null ? null : lockfile.getPackages().get(depPath);
        if (pkgSnapshot == null) {
            return false;
        }
        String version = nameVerFromPkgSnapshot(depPath, pkgSnapshot)[1];
        if (validRange(wantedDep.getBareSpecifier(), release) == null) {
            for (ProjectSnapshot importer : lockfile.getImporters().values()) {
                if (wantedDep.getBareSpecifier().equals(importer.getSpecifiers().get(wantedDep.getAlias()))) {
                    return true;
                }
            }
        }
        return satisfiesLoose(version, wantedDep.getBareSpecifier(), release);
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
                                                                 @Nullable String alias, PnpmRelease release) {
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
            Map<String, String> lockedPeerContext = getLockedPeerContext(dependencyLockfile);
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
            info.setDepPath(depPath);
            info.setName(nameVer[0]);
            info.setVersion(nameVer[1]);
            info.setDependencyLockfile(dependencyLockfile);
            info.setLockedPeerContext(lockedPeerContext);
            info.setResolution(dependencyLockfile.getResolution() == null ? null :
                    LockfileUtils.pkgSnapshotToResolution(depPath, dependencyLockfile, registries, release));
            return info;
        }
        DepPath.DependencyPath parsed = DepPath.parse(depPath);
        String pkgId = parsed.getNonSemverVersion() != null ? parsed.getNonSemverVersion() :
                parsed.getName() != null && parsed.getVersion() != null ? parsed.getName() + "@" + parsed.getVersion() :
                        depPath;
        InfoFromLockfile info = new InfoFromLockfile(pkgId);
        info.setDepPath(depPath);
        return info;
    }

    // {...nameVerFromPkgSnapshot(depPath, snapshot), ...omitDepsFields(snapshot), ...pkg} (11.5.2): the manifest over
    // what the lock records of the package, but its dependency fields
    private static Map<String, @Nullable Object> underSnapshot(String depPath, PackageSnapshot snapshot,
                                                               Map<String, @Nullable Object> pkg) {
        Map<String, @Nullable Object> merged = new LinkedHashMap<>();
        String[] nameVer = nameVerFromPkgSnapshot(depPath, snapshot);
        merged.put("name", nameVer[0] == null ? Js.UNDEFINED : nameVer[0]);
        merged.put("version", nameVer[1] == null ? Js.UNDEFINED : nameVer[1]);
        merged.put("nonSemverVersion", nameVer[2] == null ? Js.UNDEFINED : nameVer[2]);
        putDefined(merged, "id", snapshot.getId());
        putDefined(merged, "name", snapshot.getName());
        putDefined(merged, "version", snapshot.getVersion());
        putDefined(merged, "resolution", snapshot.getResolution());
        putDefined(merged, "engines", snapshot.getEngines());
        putDefined(merged, "cpu", snapshot.getCpu());
        putDefined(merged, "os", snapshot.getOs());
        putDefined(merged, "libc", snapshot.getLibc());
        putDefined(merged, "deprecated", snapshot.getDeprecated());
        putDefined(merged, "hasBin", snapshot.getHasBin());
        putDefined(merged, "bundledDependencies", snapshot.getBundledDependencies());
        putDefined(merged, "transitivePeerDependencies", snapshot.getTransitivePeerDependencies());
        putDefined(merged, "optional", snapshot.getOptional());
        putDefined(merged, "patched", snapshot.getPatched());
        merged.putAll(pkg);
        return merged;
    }

    private static void putDefined(Map<String, @Nullable Object> map, String key, @Nullable Object value) {
        if (value != null && value != Js.UNDEFINED) {
            map.put(key, value);
        }
    }

    // getLockedPeerContext (11.5.2): each peer the lock records the package with, by the dep path it resolved to
    private static @Nullable Map<String, String> getLockedPeerContext(PackageSnapshot dependencyLockfile) {
        if (dependencyLockfile.getPeerDependencies() == null) {
            return null;
        }
        Map<String, String> lockedPeerContext = new LinkedHashMap<>();
        for (String peerName : Js.ownKeys(dependencyLockfile.getPeerDependencies())) {
            String ref = dependencyLockfile.getDependencies() == null ? null :
                    dependencyLockfile.getDependencies().get(peerName);
            if (ref == null && dependencyLockfile.getOptionalDependencies() != null) {
                ref = dependencyLockfile.getOptionalDependencies().get(peerName);
            }
            String depPath = ref == null ? null : DepPath.refToRelative(ref, peerName);
            if (depPath != null) {
                lockedPeerContext.put(peerName, depPath);
            }
        }
        return lockedPeerContext.isEmpty() ? null : lockedPeerContext;
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
        // 11.7.0: the package's resolution is active from here to its end, and its request waits while a shallower
        // one is active (startPackageResolution, waitForPackageResolutionTurn)
        Runnable finishPackageResolution = startPackageResolution(ctx, options.currentDepth);
        Promise<Void> turn = waitForPackageResolutionTurn(ctx, options.currentDepth);
        return turn == null ? requestDependency(wanted, ctx, options, currentPkg, finishPackageResolution) :
                turn.then(v -> requestDependency(wanted, ctx, options, currentPkg, finishPackageResolution));
    }

    // resolveDependency from the package's request on, which ends its resolution however it ends
    private static Promise<@Nullable PkgAddress> requestDependency(WantedDependency wanted, ResolutionContext ctx,
                                                                   ResolveDependencyOptions options,
                                                                   @Nullable InfoFromLockfile currentPkg,
                                                                   Runnable finishPackageResolution) {
        PreferredVersions.ByName preferredVersions = !options.updateRequested && options.preferredVersion != null ?
                getExactSinglePreferredVersions(wanted, options.preferredVersion) :
                options.preferredVersions;
        return Promise.async(ctx.getLoop(), () -> requestPackage(wanted, ctx, options, currentPkg, preferredVersions))
                .recover(e -> {
                    finishPackageResolution.run();
                    String code = e instanceof PnpmError ? ((PnpmError) e).getCode() : null;
                    // before 11.1.3 an immature pick also fails an optional dependency
                    boolean kept = "ERR_PNPM_TRUST_DOWNGRADE".equals(code) ||
                                   "ERR_PNPM_NO_MATURE_MATCHING_VERSION".equals(code) &&
                                   !ctx.getRelease().checksTheReleaseAgeAsAPolicy();
                    if ((e instanceof PnpmError || e instanceof JsError) && wanted.isOptional() && !kept) {
                        // from 11.11.0 one the lock holds a satisfying entry of fails all the same, with a hint
                        // where it has none (PnpmRelease#keepsAFailingOptionalDependencyTheLockResolves)
                        if (!wantedLockfileContainsSatisfyingEntry(ctx, wanted) ||
                            !ctx.getRelease().keepsAFailingOptionalDependencyTheLockResolves()) {
                            return Promise.resolved(ctx.getLoop(), null);
                        }
                        if (e instanceof PnpmError) {
                            ((PnpmError) e).hintIfNone(OPTIONAL_DEPENDENCY_NOT_SKIPPED);
                        }
                    }
                    throw e;
                })
                .then(pkgResponse -> {
                    if (pkgResponse == null) {
                        return Promise.resolved(ctx.getLoop(), null);
                    }
                    try {
                        return resolvedDependency(wanted, ctx, options, currentPkg, pkgResponse,
                                finishPackageResolution);
                    } catch (RuntimeException e) {
                        finishPackageResolution.run();
                        throw e;
                    }
                });
    }

    private static final String OPTIONAL_DEPENDENCY_NOT_SKIPPED = "This optional dependency is not skipped, because " +
            "the lockfile contains a resolution for it. Skipping it would remove the locked entries, making the " +
            "lockfile differ depending on which machine ran the install. If the version was intentionally removed " +
            "from the registry, update the dependent package or remove the entries from the lockfile.";

    // wantedLockfileContainsSatisfyingEntry (11.11.0): whether the lock holds a package of the dependency's name, an
    // npm: alias's real one, in its range
    private static boolean wantedLockfileContainsSatisfyingEntry(ResolutionContext ctx,
                                                                 WantedDependency wantedDependency) {
        String alias = wantedDependency.getAlias();
        if (alias == null || alias.isEmpty()) {
            return false;
        }
        String[] unwrapped = unwrapPackageName(alias, wantedDependency.getBareSpecifier());
        String pkgName = unwrapped[0];
        String bareSpecifier = unwrapped[1];
        PnpmRelease release = ctx.getRelease();
        if (validRange(bareSpecifier, release) == null) {
            return false;
        }
        Map<String, PackageSnapshot> packages = ctx.getWantedLockfile().getPackages();
        if (packages == null) {
            return false;
        }
        for (String depPath : packages.keySet()) {
            DepPath.DependencyPath parsed = DepPath.parse(depPath);
            String version = parsed.getVersion();
            if (pkgName.equals(parsed.getName()) && version != null && release.underSemver(semver -> {
                try {
                    return NodeSemver.satisfies(version, bareSpecifier, NodeSemver.Options.DEFAULT.release(semver));
                } catch (IllegalArgumentException e) {
                    return false;
                }
            })) {
                return true;
            }
        }
        return false;
    }

    private static Promise<PackageRequester.PackageResponse> requestPackage(
            WantedDependency wantedDependency, ResolutionContext ctx, ResolveDependencyOptions options,
            @Nullable InfoFromLockfile currentPkg, PreferredVersions.ByName preferredVersions) {
        boolean calcSpecifier = options.currentDepth == 0;
        if (options.update == null && currentPkg != null && currentPkg.getVersion() != null &&
            currentPkg.getPkgId().endsWith("@" + currentPkg.getVersion()) && !calcSpecifier) {
            wantedDependency.setBareSpecifier(replaceVersionInBareSpecifier(wantedDependency.getBareSpecifier(),
                    currentPkg.getVersion(), namedRegistryPrefixes(ctx, wantedDependency.getBareSpecifier()),
                    ctx.getRelease()));
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

    // resolveDependency after the package is requested, which ends the package's resolution unless it throws, when
    // its caller does
    private static Promise<@Nullable PkgAddress> resolvedDependency(WantedDependency wantedDependency,
                                                                    ResolutionContext ctx,
                                                                    ResolveDependencyOptions options,
                                                                    @Nullable InfoFromLockfile currentPkg,
                                                                    PackageRequester.PackageResponse pkgResponse,
                                                                    Runnable finishPackageResolution) {
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
            finishPackageResolution.run();
            return Promise.resolved(ctx.getLoop(), linked);
        }

        Map<String, @Nullable Object> manifest = getManifestFromResponse(pkgResponse, wantedDependency, currentPkg);
        if (manifest.get("dependencies") == null) {
            manifest.put("dependencies", new LinkedHashMap<String, @Nullable Object>());
        }
        ReadPackageHook readPackageHook = ctx.getReadPackageHook();
        if (readPackageHook == null) {
            return Promise.resolved(ctx.getLoop(), finishing(finishPackageResolution, () ->
                    pkgAddress(wantedDependency, ctx, options, currentPkg, pkgResponse, manifest)));
        }
        return Promise.resolved(ctx.getLoop(), readPackageHook.apply(manifest, null)).then(hooked ->
                Promise.resolved(ctx.getLoop(), finishing(finishPackageResolution, () ->
                        pkgAddress(wantedDependency, ctx, options, currentPkg, pkgResponse, hooked))));
    }

    // what pkgAddress gives, the package's resolution ended however it ends (resolveDependency's finally)
    private static @Nullable PkgAddress finishing(Runnable finishPackageResolution,
                                                  Supplier<@Nullable PkgAddress> pkgAddress) {
        try {
            return pkgAddress.get();
        } finally {
            finishPackageResolution.run();
        }
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
            PkgManifestUtils.convertEnginesRuntimeToDependencies(pkg, "engines", "dependencies", ctx.getRelease());
        }
        if (!Js.truthy(pkg.get("name"))) {
            throw new PnpmError("MISSING_PACKAGE_NAME", "Can't install " + wantedDependency.getBareSpecifier() +
                                                        ": Missing package name");
        }
        String pkgName = Js.string(pkg.get("name"));
        String pkgIdWithPatchHash = pkgResponse.getId().startsWith(pkgName + "@") ? pkgResponse.getId() :
                pkgName + "@" + pkgResponse.getId();
        PatchingConfig.PatchInfo patch = PatchingConfig.getPatchInfo(ctx.getPatchedDependencies(), pkgName,
                Js.string(pkg.get("version")), ctx.getRelease());
        if (patch != null) {
            ctx.getAppliedPatches().add(patch.getKey());
            pkgIdWithPatchHash = pkgIdWithPatchHash + "(patch_hash=" + patch.getHash() + ")";
        }

        if (parentIdsContainSequence(options.parentIds, options.parentPkg.getPkgId(), pkgResponse.getId()) ||
            pkgResponse.getId().equals(options.parentPkg.getPkgId())) {
            return null;
        }

        PackageSnapshot dependencyLockfile = currentPkg == null ? null : currentPkg.getDependencyLockfile();
        if (options.update == null && dependencyLockfile != null && currentPkg.getDepPath() != null &&
            !pkgResponse.isUpdated() && dependencyLockfile.getPeerDependencies() == null &&
            ctx.getRelease().readsALockedPackageFromItsSnapshot()) {
            // 11.5.2 names the dep path, which 11.2.2's getInfoFromLockfile left out, and so takes this branch
            prepare = false;
            hasBin = Boolean.TRUE.equals(dependencyLockfile.getHasBin());
            pkg = underSnapshot(currentPkg.getDepPath(), dependencyLockfile, pkg);
        } else {
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
        boolean packageIsNew = !ctx.getResolvedPkgsById().containsKey(pkgResponse.getId());
        String parentImporterId = options.parentIds.get(0);
        boolean currentIsOptional = wantedDependency.isOptional() || options.parentPkg.isOptional();
        // 11.7.0: the occurrence that holds the claim on the package's children resolves them. Before 11.7.0 no claim
        // is taken over from the first occurrence, so that isNew is 11.2.2's !resolvedPkgsById[id], and the missing
        // peers of children the claim gives are those of 11.2.2's missingPeersOfChildrenByPkgId
        ChildrenResolutionClaim childrenResolution = claimChildrenResolution(ctx, options.currentDepth,
                options.parentIds, pkgResponse.getId());
        boolean isNew = childrenResolution.isOwner();

        if (packageIsNew) {
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
            if (!isNew) {
                DependenciesTreeNode node = ctx.getDependenciesTree().get(nodeId);
                if (node != null) {
                    node.setDepth(Math.min(node.getDepth(), options.currentDepth));
                } else {
                    String alias = wantedDependency.getAlias() != null ? wantedDependency.getAlias() :
                            pkgResponse.getAlias() != null ? pkgResponse.getAlias() : pkgName;
                    ctx.getPendingNodes().add(new PendingNode(alias, nodeId, existing, options.currentDepth,
                            installable, options.parentIds,
                            currentPkg == null ? null : currentPkg.getLockedPeerContext(),
                            currentPkg == null ? null : currentPkg.getDepPath()));
                }
            }
        }

        String rootDir = "directory".equals(pkgResponse.getResolution().get("type")) ?
                RootRelativePath.join(ctx.getLockfileDir(), Js.string(pkgResponse.getResolution().get("directory"))) :
                options.prefix;
        MissingPeersOfChildren missingPeersOfChildren = childrenResolution.getMissingPeersOfChildren();

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
        address.setChildrenResolutionId(childrenResolution.getId());
        address.setRootDir(rootDir);
        // 11.2.2's are the peers of this occurrence's manifest; 11.15.0's those of the package as its first occurrence
        // resolved it, less its own dependencies and name, with its optional peers named only in peerDependenciesMeta
        // (PnpmRelease#takesMissingPeersFromTheResolvedPackage)
        Map<String, MissingPeerInfo> missingPeers = getMissingPeers(pkg);
        Map<String, MissingPeerInfo> resolvedPeers = missingPeersOf(resolvedPkg.getPeerDependencies());
        boolean same = new ArrayList<>(missingPeers.entrySet()).equals(new ArrayList<>(resolvedPeers.entrySet()));
        address.setMissingPeers(same || !ctx.getRelease().takesMissingPeersFromTheResolvedPackage() ? missingPeers :
                resolvedPeers);
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

    // getMissingPeers of 11.15.0, over a resolved package's peers
    private static Map<String, MissingPeerInfo> missingPeersOf(Map<String, PeerDependency> peerDependencies) {
        Map<String, MissingPeerInfo> missingPeers = new LinkedHashMap<>();
        for (Map.Entry<String, PeerDependency> peer : peerDependencies.entrySet()) {
            missingPeers.put(peer.getKey(), new MissingPeerInfo(peer.getValue().getVersion(),
                    Boolean.TRUE.equals(peer.getValue().getOptional())));
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
        return getNonDevWantedDependencies(pkg, PnpmRelease.PORTED);
    }

    /**
     * {@link #getNonDevWantedDependencies(Map)} as {@code release} reads a package: from 11.4.0 a dependency name no
     * npm package can have is an error ({@link PnpmRelease#rejectsAnInvalidDependencyName}).
     */
    static List<WantedDependency> getNonDevWantedDependencies(Map<String, @Nullable Object> pkg,
                                                              PnpmRelease release) {
        String pkgDescription = pkg.get("name") != null && pkg.get("name") != Js.UNDEFINED ?
                "Package \"" + Js.string(pkg.get("name")) + (pkg.get("version") != null &&
                                                             pkg.get("version") != Js.UNDEFINED ?
                        "@" + Js.string(pkg.get("version")) : "") + "\"" : "Package";
        assertValidDependencyAliases(pkg.get("dependencies"), pkgDescription, release);
        assertValidDependencyAliases(pkg.get("optionalDependencies"), pkgDescription, release);
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
     * validateDependencyAlias.ts' {@code assertValidDependencyAliases}, with validate-npm-package-name 7.0.2, where
     * {@code release} makes it ({@link PnpmRelease#rejectsAnInvalidDependencyName}).
     */
    static void assertValidDependencyAliases(@Nullable Object deps, String parentPkgDescription, PnpmRelease release) {
        if (deps == null || deps == Js.UNDEFINED) {
            return;
        }
        for (String alias : Js.ownKeys(deps)) {
            if (!ValidateNpmPackageName.validForOldPackages(alias, ValidateNpmPackageName.Release.V7_0_2) &&
                release.rejectsAnInvalidDependencyName()) {
                throw invalidDependencyName(alias, parentPkgDescription);
            }
        }
    }

    /**
     * {@code assertValidDependencyAliases} where a release asks it unconditionally (the env document's, from 11.8.0).
     */
    static void assertValidDependencyAliases(@Nullable Object deps, String parentPkgDescription) {
        if (deps == null || deps == Js.UNDEFINED) {
            return;
        }
        for (String alias : Js.ownKeys(deps)) {
            if (!ValidateNpmPackageName.validForOldPackages(alias, ValidateNpmPackageName.Release.V7_0_2)) {
                throw invalidDependencyName(alias, parentPkgDescription);
            }
        }
    }

    private static PnpmError invalidDependencyName(String alias, String parentPkgDescription) {
        return new PnpmError("INVALID_DEPENDENCY_NAME", parentPkgDescription + " contains a dependency with " +
                "an invalid name: " + Js.jsonStringify(alias), "A dependency name must be a valid npm " +
                "package name — a single `name` or `@scope/name` consisting of URL-friendly " +
                "characters, with no leading `.` or `_`, and not equal to reserved names such as " +
                "`node_modules`.");
    }

    /**
     * A dependency of the workspace root a missing peer may be hoisted as: its alias, the name of its package, and
     * the specifier it is saved with.
     */
    @Value
    static class RootDep {
        String alias;

        @Nullable
        Object pkgName;

        @Nullable
        String normalizedBareSpecifier;
    }

    // getHoistableRootDeps (11.12.0): a copy of the root's resolved dependencies, each by the specifier it saves, else
    // its wanted one, and the wanted ones that resolved to nothing
    private static List<RootDep> hoistableRootDeps(ImporterToResolve rootImporter, List<PkgAddress> rootPkgAddresses) {
        List<RootDep> rootDeps = new ArrayList<>();
        Map<String, String> wantedSpecifierByAlias = new LinkedHashMap<>();
        for (WantedDependency wanted : rootImporter.getWantedDependencies()) {
            if (Js.truthy(wanted.getAlias()) && Js.truthy(wanted.getBareSpecifier())) {
                wantedSpecifierByAlias.put(wanted.getAlias(), wanted.getBareSpecifier());
            }
        }
        Set<String> coveredAliases = new LinkedHashSet<>();
        for (PkgAddress pkgAddress : rootPkgAddresses) {
            String specifier = pkgAddress.getNormalizedBareSpecifier();
            if (specifier == null) {
                specifier = wantedSpecifierByAlias.get(pkgAddress.getAlias());
            }
            rootDeps.add(new RootDep(pkgAddress.getAlias(), pkgAddress.getPkg().get("name"), specifier));
            coveredAliases.add(pkgAddress.getAlias());
        }
        for (Map.Entry<String, String> wanted : wantedSpecifierByAlias.entrySet()) {
            if (!coveredAliases.contains(wanted.getKey())) {
                rootDeps.add(new RootDep(wanted.getKey(), unwrapPackageName(wanted.getKey(), wanted.getValue())[0],
                        wanted.getValue()));
            }
        }
        return rootDeps;
    }

    // 11.2.2's workspaceRootDeps, the root's resolved dependencies as they are read: each by the specifier it saves
    private static List<RootDep> rootDepsOf(List<PkgAddress> rootPkgAddresses) {
        List<RootDep> rootDeps = new ArrayList<>();
        for (PkgAddress pkgAddress : rootPkgAddresses) {
            rootDeps.add(new RootDep(pkgAddress.getAlias(), pkgAddress.getPkg().get("name"),
                    pkgAddress.getNormalizedBareSpecifier()));
        }
        return rootDeps;
    }

    /**
     * The workspace root's dependencies missing peers are hoisted as: 11.2.2's root's own list of resolved
     * dependencies, which grows with the peers the loop hoists into it and the providers it attaches to it, and
     * 11.12.0's copy, taken before the loop with the dependencies the root wants ({@link #hoistableRootDeps}).
     */
    @Value
    static class WorkspaceRootDeps {
        List<PkgAddress> live;
        List<RootDep> copy;

        /**
         * {@code hoistPeers} over the root's list as it is now, and from 11.12.0 over the copy
         * ({@link PnpmRelease#copiesTheRootDependenciesPeersAreHoistedAs}), asked where the two hoist otherwise.
         */
        Map<String, String> hoistPeers(boolean autoInstallPeers,
                                       @Nullable Map<String, Map<String, PreferredVersions.Selector>> allPreferred,
                                       List<Map.Entry<String, MissingPeerInfo>> missingRequiredPeers,
                                       PnpmRelease release) {
            Map<String, String> overCopy = ResolveDependencies.hoistPeers(autoInstallPeers, allPreferred, copy,
                    missingRequiredPeers, release);
            Map<String, String> overLive = ResolveDependencies.hoistPeers(autoInstallPeers, allPreferred,
                    rootDepsOf(live), missingRequiredPeers, release);
            return overCopy.equals(overLive) || release.copiesTheRootDependenciesPeersAreHoistedAs() ? overCopy :
                    overLive;
        }
    }

    /**
     * hoistPeers.ts {@code hoistPeers} as 11.2.2 runs it over the root's resolved dependencies.
     */
    static Map<String, String> hoistPeers(boolean autoInstallPeers,
                                          @Nullable Map<String, Map<String, PreferredVersions.Selector>> allPreferred,
                                          List<PkgAddress> workspaceRootDeps,
                                          List<Map.Entry<String, MissingPeerInfo>> missingRequiredPeers) {
        return hoistPeers(autoInstallPeers, allPreferred, rootDepsOf(workspaceRootDeps), missingRequiredPeers,
                PnpmRelease.PORTED);
    }

    /**
     * hoistPeers.ts {@code hoistPeers}: the version each missing required peer is installed at; from 11.12.0 a peer
     * range, not only an exact version, takes the highest preferred version it satisfies
     * ({@link PnpmRelease#hoistsAPeerInRange}).
     */
    static Map<String, String> hoistPeers(boolean autoInstallPeers,
                                          @Nullable Map<String, Map<String, PreferredVersions.Selector>> allPreferred,
                                          List<RootDep> workspaceRootDeps,
                                          List<Map.Entry<String, MissingPeerInfo>> missingRequiredPeers,
                                          PnpmRelease release) {
        Map<String, String> dependencies = new LinkedHashMap<>();
        for (Map.Entry<String, MissingPeerInfo> peer : missingRequiredPeers) {
            String peerName = peer.getKey();
            String range = peer.getValue().getRange();
            RootDep rootDepByAlias = null;
            for (RootDep rootDep : workspaceRootDeps) {
                if (rootDep.getAlias().equals(peerName)) {
                    rootDepByAlias = rootDep;
                    break;
                }
            }
            if (rootDepByAlias != null && Js.truthy(rootDepByAlias.getNormalizedBareSpecifier())) {
                dependencies.put(peerName, rootDepByAlias.getNormalizedBareSpecifier());
                continue;
            }
            RootDep rootDep = null;
            for (RootDep candidate : workspaceRootDeps) {
                if (peerName.equals(candidate.getPkgName()) &&
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
                // 11.2.2 takes the preferred versions an exact peer version names; 11.12.0 those any peer range
                // satisfies, and where a range none, the range or nothing (PnpmRelease#hoistsAPeerInRange)
                boolean isExactVersion = NodeSemver.valid(range, NodeSemver.Options.DEFAULT) != null;
                String exactSatisfying = isExactVersion ? release.underSemver(semver -> NodeSemver.maxSatisfying(
                        versions, range, NodeSemver.Options.INCLUDE_PRERELEASE.release(semver))) : null;
                String hoisted = exactSatisfying != null ? join(exactSatisfying, nonVersions) :
                        isExactVersion && autoInstallPeers ? range : highest(versions, nonVersions);
                String inRange = inRange(range, range, versions, nonVersions, autoInstallPeers, release);
                // 11.14.0 matches the range getPeerVersionRange reads out of the spec, writing the spec itself where
                // it installs a range (PnpmRelease#readsAPeerRangeOutOfItsSpecifier)
                String rangeForMatch = ReadPackageHook.getPeerVersionRange(range, release);
                String inRangeForMatch = rangeForMatch.equals(range) ? inRange :
                        inRange(range, rangeForMatch, versions, nonVersions, autoInstallPeers, release);
                String chosen = !Objects.equals(inRange, inRangeForMatch) &&
                                release.readsAPeerRangeOutOfItsSpecifier() ? inRangeForMatch :
                        Objects.equals(hoisted, inRange) || release.hoistsAPeerInRange() ? inRange : hoisted;
                if (chosen != null) {
                    dependencies.put(peerName, chosen);
                }
            } else if (autoInstallPeers) {
                dependencies.put(peerName, range);
            }
        }
        return dependencies;
    }

    // hoistPeers' pick from 11.12.0 for a peer of the spec range: where rangeForMatch is a range, the highest preferred
    // version it satisfies, else (versions there being) the spec itself under autoInstallPeers or nothing; where it
    // is none, the highest of all; a version beside the preferred specs that are no versions
    private static @Nullable String inRange(String range, String rangeForMatch, List<String> versions,
                                            List<String> nonVersions, boolean autoInstallPeers, PnpmRelease release) {
        boolean isSemverRange = release.underSemver(semver -> NodeSemver.validRange(rangeForMatch,
                NodeSemver.Options.INCLUDE_PRERELEASE.release(semver))) != null;
        String rangeSatisfying = isSemverRange ? release.underSemver(semver -> NodeSemver.maxSatisfying(
                versions, rangeForMatch, NodeSemver.Options.INCLUDE_PRERELEASE.release(semver))) : null;
        return rangeSatisfying != null ? join(rangeSatisfying, nonVersions) :
                isSemverRange && !versions.isEmpty() ? autoInstallPeers ? range : null :
                        highest(versions, nonVersions);
    }

    private static String join(String version, List<String> nonVersions) {
        List<String> specs = new ArrayList<>();
        specs.add(version);
        specs.addAll(nonVersions);
        return String.join(" || ", specs);
    }

    // [semver.maxSatisfying(versions, '*'), ...nonVersions].filter(spec => spec != null).join(' || ')
    private static String highest(List<String> versions, List<String> nonVersions) {
        List<String> specs = new ArrayList<>();
        String highest = NodeSemver.maxSatisfying(versions, "*", NodeSemver.Options.INCLUDE_PRERELEASE);
        if (highest != null) {
            specs.add(highest);
        }
        specs.addAll(nonVersions);
        return String.join(" || ", specs);
    }

    /**
     * hoistPeers.ts {@code getHoistableOptionalPeers} as the release that installs runs it. Where {@code pnpmVersion}
     * is a range with releases on both sides of {@link #LOCKED_OPTIONAL_PEER_CANDIDATES}, the inputs do not say which
     * of the two runs: they must hoist the same.
     */
    static Map<String, String> getHoistableOptionalPeers(Map<String, List<String>> allMissingOptionalPeers,
                                                         Map<String, Map<String, PreferredVersions.Selector>> allPref,
                                                         String pnpmVersion) {
        Boolean lockedCandidates = Main.releaseIn(pnpmVersion, LOCKED_OPTIONAL_PEER_CANDIDATES);
        Map<String, String> optionalDependencies = getHoistableOptionalPeers(allMissingOptionalPeers, allPref,
                !Boolean.FALSE.equals(lockedCandidates));
        if (lockedCandidates == null) {
            Map<String, String> withoutThem = getHoistableOptionalPeers(allMissingOptionalPeers, allPref, false);
            if (!optionalDependencies.equals(withoutThem)) {
                throw new CannotKnow("the optional peers pnpm hoists: " + specs(optionalDependencies) + " from " +
                                     "11.5.1, which counts the versions the lock holds, and " + specs(withoutThem) +
                                     " before; the release that installs is one of " + pnpmVersion);
            }
        }
        return optionalDependencies;
    }

    private static String specs(Map<String, String> dependencies) {
        List<String> specs = new ArrayList<>();
        for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
            specs.add(dependency.getKey() + "@" + dependency.getValue());
        }
        return specs.isEmpty() ? "none" : String.join(", ", specs);
    }

    /**
     * hoistPeers.ts {@code getHoistableOptionalPeers}: a missing optional peer at the highest version known that every
     * range admits. With {@code lockedCandidates} a selector with a weight is read by its type, as 11.5.1 reads it
     * ({@link #LOCKED_OPTIONAL_PEER_CANDIDATES}); without, the selector itself is compared with {@code 'version'}.
     */
    static Map<String, String> getHoistableOptionalPeers(Map<String, List<String>> allMissingOptionalPeers,
                                                         Map<String, Map<String, PreferredVersions.Selector>> allPref,
                                                         boolean lockedCandidates) {
        Map<String, String> optionalDependencies = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> peer : allMissingOptionalPeers.entrySet()) {
            Map<String, PreferredVersions.Selector> preferred = allPref.get(peer.getKey());
            if (preferred == null) {
                continue;
            }
            String maxSatisfyingVersion = null;
            for (Map.Entry<String, PreferredVersions.Selector> entry : preferred.entrySet()) {
                // compared itself, a weighted selector is an object, never the string 'version'
                if (!lockedCandidates && entry.getValue().getWeight() != null ||
                    !"version".equals(entry.getValue().getSelectorType())) {
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

    // the named-registry prefixes the release reads (PnpmRelease#readsNamedRegistries): none before 11.1.0, which
    // matters only to a spec that starts with one
    private static List<String> namedRegistryPrefixes(ResolutionContext ctx, @Nullable String bareSpecifier) {
        for (String prefix : ctx.getNamedRegistryPrefixes()) {
            if (bareSpecifier != null && bareSpecifier.startsWith(prefix)) {
                return ctx.getRelease().readsNamedRegistries() ? ctx.getNamedRegistryPrefixes() :
                        Collections.emptyList();
            }
        }
        return ctx.getNamedRegistryPrefixes();
    }

    static String replaceVersionInBareSpecifier(String bareSpecifier, String version,
                                                List<String> namedRegistryPrefixes) {
        return replaceVersionInBareSpecifier(bareSpecifier, version, namedRegistryPrefixes, PnpmRelease.PORTED);
    }

    static String replaceVersionInBareSpecifier(String bareSpecifier, String version,
                                                List<String> namedRegistryPrefixes, PnpmRelease release) {
        if (validRange(bareSpecifier, release) != null) {
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
        // an npm: range is the alias's from 11.1.0 (PnpmRelease#takesTheAliasAsTheNameOfAnNpmRange); before, it
        // takes the version after it, as a name would
        if (validRange(bareSpecifier.substring(prefix.length()), release) != null &&
            (!"npm:".equals(prefix) || release.takesTheAliasAsTheNameOfAnNpmRange())) {
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
                                                       String registry, PnpmRelease release) {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier(
                wantedDependency.getBareSpecifier(), wantedDependency.getAlias(),
                defaultTag.isEmpty() ? "latest" : defaultTag, registry, release);
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
                return release.underSemver(semver -> NodeSemver.maxSatisfying(localVersions, spec.getFetchSpec(),
                        NodeSemver.Options.LOOSE.release(semver))) != null;
            default:
                return false;
        }
    }

    // semver.validRange under the node-semver the release bundles
    static @Nullable String validRange(String range, PnpmRelease release) {
        try {
            return release.underSemver(semver -> NodeSemver.validRange(range,
                    NodeSemver.Options.DEFAULT.release(semver)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean satisfiesLoose(@Nullable String version, String range, PnpmRelease release) {
        if (version == null) {
            return false;
        }
        try {
            return release.underSemver(semver -> NodeSemver.satisfies(version, range,
                    NodeSemver.Options.LOOSE.release(semver)));
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
