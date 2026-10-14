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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

/**
 * Port of @yarnpkg/core sources/Project.ts (Yarn 4.10.3).
 */
final class Project {

    static final String LOCKFILE = "yarn.lock";

    private static final Pattern MULTIPLE_KEYS_REGEXP = Pattern.compile(" *, *");
    private static final Pattern LEGACY_PATCH = Pattern.compile("^patch:[^@]+@(?!npm(:|%3A))");
    private static final Pattern EOL = Pattern.compile("\\r?\\n");

    final Configuration configuration;

    final List<Workspace> workspaces = new ArrayList<>();
    final Map<String, Workspace> workspacesByCwd = new LinkedHashMap<>();
    final Map<String, Workspace> workspacesByIdent = new LinkedHashMap<>();

    Map<String, String> storedResolutions = new LinkedHashMap<>();
    Map<String, Descriptor> storedDescriptors = new LinkedHashMap<>();
    Map<String, Package> storedPackages = new LinkedHashMap<>();
    final Map<String, String> storedChecksums = new LinkedHashMap<>();

    final Set<String> accessibleLocators = new LinkedHashSet<>();
    final Set<String> conditionalLocators = new LinkedHashSet<>();
    final Set<String> disabledLocators = new LinkedHashSet<>();

    /**
     * The conditional packages that are disabled on some of the machines the host could be and not on others.
     */
    final Set<String> hostDependentLocators = new LinkedHashSet<>();

    final Map<String, Package> originalPackages = new LinkedHashMap<>();
    final Set<String> optionalBuilds = new LinkedHashSet<>();

    /**
     * Each descriptor of the lockfile as read, with the locator it resolved to: what an unchanged builtin patch is
     * carried from.
     */
    final Map<String, Locator> lockfileLocators = new LinkedHashMap<>();

    /**
     * {@code getPackageMetadata}'s answers by registry and package.
     */
    final Map<String, Map<String, @Nullable Object>> packageMetadata = new HashMap<>();

    /**
     * The package.json of a directory relative to the root ({@code .} the root), null without one.
     */
    Function<String, @Nullable Manifest> manifests = cwd -> null;

    /**
     * The files the project holds relative to its root, when the host says.
     */
    final @Nullable Set<String> paths;

    /**
     * The text of the files the host read, by path from the root: the patch files.
     */
    final Map<String, String> files;

    /**
     * {@code __metadata.version} as the lockfile spells it ({@code "8"}), {@code -1} for a Yarn 1 lockfile, null
     * for none.
     */
    @Nullable
    Object lockfileLastVersion;

    boolean lockfileNeedsRefresh;

    /**
     * {@code new Date()} while Yarn runs.
     */
    final Instant now;

    /**
     * The yarn.lock Yarn read, null when the project has none.
     */
    @Nullable
    String lockfileContent;

    /**
     * {@link #lockfileContent} parsed, which the legacy migration reads too.
     */
    @Nullable
    Map<String, @Nullable Object> parsedLockfile;

    /**
     * The machine Yarn runs on, whose architecture {@code current} names.
     */
    final Host host;

    Project(Configuration configuration, Instant now, @Nullable Set<String> paths, Map<String, String> files,
            Host host) {
        this.configuration = configuration;
        this.now = now;
        this.paths = paths;
        this.files = files;
        this.host = host;
    }

    /**
     * Checksums of fetched packages the engine cannot compute, named.
     */
    static final class UnknownChecksums extends RuntimeException {
        UnknownChecksums(String subject) {
            super(subject);
        }
    }

    /**
     * {@code setupResolutions}: every entry of a lockfile with {@code __metadata} becomes an original package,
     * its checksum and the descriptors of its key; a Yarn 1 lockfile only sets {@link #lockfileLastVersion}.
     */
    void setupResolutions(@Nullable String content) {
        storedResolutions = new LinkedHashMap<>();
        storedDescriptors = new LinkedHashMap<>();
        storedPackages = new LinkedHashMap<>();
        lockfileLocators.clear();
        lockfileContent = content;
        parsedLockfile = null;
        String defaultLanguageName = configuration.getString("defaultLanguageName");
        if (content == null) {
            return;
        }
        Map<String, @Nullable Object> parsed = parsedLockfile = Syml.parseSyml(content,
                configuration.getRelease());
        Object metadata = parsed.get("__metadata");
        if (Js.truthy(metadata)) {
            Object lockfileVersion = Js.get(metadata, "version");
            Object cacheKey = Js.get(metadata, "cacheKey");
            lockfileLastVersion = lockfileVersion;
            lockfileNeedsRefresh = Js.toNumber(lockfileVersion) < configuration.getRelease().getLockfileVersion();

            for (String key : Js.keys(parsed)) {
                if ("__metadata".equals(key)) {
                    continue;
                }
                Object data = parsed.get(key);
                if (data == null) {
                    throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'resolution')");
                }
                Object resolution = Js.get(data, "resolution");
                if (resolution == Syml.UNDEFINED) {
                    throw new JsError(JsError.ERROR, "Assertion failed: Expected the lockfile entry to have a " +
                                                     "resolution field (" + key + ")");
                }
                if (!(resolution instanceof String)) {
                    throw new JsError(JsError.TYPE_ERROR, "string.match is not a function");
                }
                Locator locator = StructUtils.parseLocator((String) resolution, true);

                Manifest manifest = new Manifest();
                manifest.load(data, true);

                String version = manifest.version;
                String languageName = manifest.languageName != null && !manifest.languageName.isEmpty() ?
                        manifest.languageName : defaultLanguageName;
                Object rawLinkType = Js.get(data, "linkType");
                if (!(rawLinkType instanceof String)) {
                    throw new JsError(JsError.TYPE_ERROR, rawLinkType == Syml.UNDEFINED || rawLinkType == null ?
                            "Cannot read properties of " + (rawLinkType == null ? "null" : "undefined") +
                            " (reading 'toUpperCase')" : "data.linkType.toUpperCase is not a function");
                }
                String linkType = ((String) rawLinkType).toUpperCase(Locale.ROOT);
                Object conditions = Js.get(data, "conditions");
                if (conditions == Syml.UNDEFINED) {
                    conditions = null;
                }

                Object checksum = Js.get(data, "checksum");
                if (checksum != null && checksum != Syml.UNDEFINED) {
                    String stored = Js.string(checksum);
                    storedChecksums.put(locator.locatorHash, cacheKey != Syml.UNDEFINED && !stored.contains("/") ?
                            Js.string(cacheKey) + "/" + stored : stored);
                }

                Package pkg = new Package(locator, version, languageName, linkType, conditions,
                        manifest.dependencies, manifest.peerDependencies, manifest.dependenciesMeta,
                        manifest.peerDependenciesMeta, manifest.bin);
                originalPackages.put(pkg.locatorHash, pkg);

                for (String entry : MULTIPLE_KEYS_REGEXP.split(key, -1)) {
                    Descriptor descriptor = StructUtils.parseDescriptor(entry);
                    if (Js.toNumber(lockfileVersion) <= 6) {
                        descriptor = configuration.normalizeDependency(descriptor);
                        // `$1` names a group inside the lookahead, which never captures: it is replaced by nothing
                        Matcher legacyPatch = LEGACY_PATCH.matcher(descriptor.range);
                        descriptor = StructUtils.makeDescriptor(descriptor,
                                legacyPatch.find() ? "npm%3A" + descriptor.range.substring(legacyPatch.end()) :
                                        descriptor.range);
                    }
                    storedDescriptors.put(descriptor.descriptorHash, descriptor);
                    storedResolutions.put(descriptor.descriptorHash, locator.locatorHash);
                    lockfileLocators.put(descriptor.descriptorHash, locator);
                }
            }
        } else if (content.contains("yarn lockfile v1")) {
            lockfileLastVersion = -1;
        }
    }

    /**
     * {@code setupWorkspaces}: each finished setup claims the unclaimed members it lists and starts theirs; CannotKnow
     * when two setups that may end in either order list one member.
     */
    void setupWorkspaces(FastGlob fastGlob, Function<String, @Nullable Manifest> manifests) {
        this.manifests = manifests;
        workspaces.clear();
        workspacesByCwd.clear();
        workspacesByIdent.clear();

        Set<String> pathsChecked = new HashSet<>();
        Map<String, String> claimedBy = new HashMap<>();
        Map<String, List<Workspace>> claimed = new HashMap<>();
        Deque<Workspace> setups = new ArrayDeque<>();
        Workspace root = new Workspace(this, ".");
        pathsChecked.add(".");
        setups.add(root);
        while (!setups.isEmpty()) {
            Workspace workspace = setups.poll();
            workspace.setup(manifests.apply(workspace.getRelativeCwd()), fastGlob);
            List<Workspace> members = new ArrayList<>();
            for (String memberCwd : workspace.getWorkspacesCwds()) {
                if (pathsChecked.add(memberCwd)) {
                    Workspace member = new Workspace(this, memberCwd);
                    claimedBy.put(memberCwd, workspace.getRelativeCwd());
                    members.add(member);
                    setups.add(member);
                } else if (claimedBy.containsKey(memberCwd) &&
                           !claimedAbove(workspace.getRelativeCwd(), claimedBy.get(memberCwd), claimedBy)) {
                    throw new CannotKnow("the order of the workspaces: " + claimedBy.get(memberCwd) + " and " +
                                         workspace.getRelativeCwd() + " both list " + memberCwd + ", which " +
                                         "follows whichever of them Yarn finishes setting up first");
                }
            }
            claimed.put(workspace.getRelativeCwd(), members);
        }
        addWorkspaces(root, claimed);
    }

    // the claimer's setup ended before this workspace's started: it claimed this workspace, or one that did
    private static boolean claimedAbove(String workspaceCwd, String claimer, Map<String, String> claimedBy) {
        for (String cwd = workspaceCwd; cwd != null; cwd = claimedBy.get(cwd)) {
            if (cwd.equals(claimer)) {
                return true;
            }
        }
        return false;
    }

    private void addWorkspaces(Workspace workspace, Map<String, List<Workspace>> claimed) {
        addWorkspace(workspace);
        for (Workspace member : claimed.get(workspace.getRelativeCwd())) {
            addWorkspaces(member, claimed);
        }
    }

    private void addWorkspace(Workspace workspace) {
        Workspace dup = workspacesByIdent.get(workspace.getAnchoredLocator().identHash);
        if (dup != null) {
            throw new JsError(JsError.ERROR, "Duplicate workspace name " +
                                             StructUtils.stringifyIdent(workspace.getAnchoredLocator()) + ": " +
                                             workspace.getRelativeCwd() + " conflicts with " + dup.getRelativeCwd());
        }
        workspaces.add(workspace);
        workspacesByCwd.put(workspace.getRelativeCwd(), workspace);
        workspacesByIdent.put(workspace.getAnchoredLocator().identHash, workspace);
    }

    boolean lockfileHeld(Locator locator) {
        for (Locator locked : lockfileLocators.values()) {
            if (locked.locatorHash.equals(locator.locatorHash)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code isPackageCompatible} with the supported architectures of every machine the host could be; null when
     * the machines disagree.
     */
    private @Nullable Boolean isPackageCompatible(Package pkg) {
        Boolean compatible = null;
        for (Host machine : machines(pkg)) {
            boolean answer = StructUtils.isPackageCompatible(pkg, configuration.getSupportedArchitectures(machine));
            if (compatible == null) {
                compatible = answer;
            } else if (compatible != answer) {
                return null;
            }
        }
        return compatible;
    }

    // the host with each member it does not say taken as every value the conditions name, and as one they do not;
    // getLibc detects no libc off linux
    private List<Host> machines(Package pkg) {
        Map<String, Set<String>> named = new HashMap<>();
        Matcher m = StructUtils.CONDITION_REGEX.matcher(Js.string(pkg.conditions));
        while (m.find()) {
            named.computeIfAbsent(m.group(1), k -> new LinkedHashSet<>()).add(m.group(2));
        }
        Set<String> oses = named.getOrDefault("os", Collections.emptySet());
        List<Host> machines = new ArrayList<>();
        for (String os : candidates(host.getOs(), oses)) {
            for (String cpu : candidates(host.getCpu(), named.get("cpu"))) {
                boolean linux = "linux".equals(os) || os.isEmpty() && !oses.contains("linux");
                for (String libc : linux ? candidates(host.getLibc(), named.get("libc")) :
                        Collections.<String>singletonList(null)) {
                    machines.add(host.toBuilder().os(os).cpu(cpu).libc(libc).build());
                }
            }
        }
        return machines;
    }

    // "" matches no condition
    private static List<String> candidates(@Nullable String known, @Nullable Set<String> named) {
        if (known != null) {
            return Collections.singletonList(known);
        }
        List<String> out = named == null ? new ArrayList<>() : new ArrayList<>(named);
        out.add("");
        return out;
    }

    Workspace getTopLevelWorkspace() {
        return getWorkspaceByCwd(".");
    }

    @Nullable
    Workspace tryWorkspaceByCwd(String workspaceCwd) {
        String normalized = PosixPath.normalize(workspaceCwd).replaceAll("/+$", "");
        return workspacesByCwd.get(normalized.isEmpty() ? "." : normalized);
    }

    Workspace getWorkspaceByCwd(String workspaceCwd) {
        Workspace workspace = tryWorkspaceByCwd(workspaceCwd);
        if (workspace == null) {
            throw new JsError(JsError.ERROR, "Workspace not found (" + workspaceCwd + ")");
        }
        return workspace;
    }

    @Nullable
    Workspace tryWorkspaceByIdent(Ident ident) {
        return workspacesByIdent.get(ident.identHash);
    }

    @Nullable
    Workspace tryWorkspaceByDescriptor(Descriptor descriptor) {
        if (descriptor.range.startsWith(WorkspaceResolver.PROTOCOL)) {
            String specifier = descriptor.range.substring(WorkspaceResolver.PROTOCOL.length());
            if (!"^".equals(specifier) && !"~".equals(specifier) && !"*".equals(specifier) &&
                YarnpkgSemverUtils.validRange(specifier) == null) {
                return tryWorkspaceByCwd(specifier);
            }
        }
        Workspace workspace = tryWorkspaceByIdent(descriptor);
        if (workspace == null) {
            return null;
        }
        if (StructUtils.isVirtualDescriptor(descriptor)) {
            descriptor = StructUtils.devirtualizeDescriptor(descriptor);
        }
        return workspace.accepts(descriptor.range) ? workspace : null;
    }

    Workspace getWorkspaceByDescriptor(Descriptor descriptor) {
        Workspace workspace = tryWorkspaceByDescriptor(descriptor);
        if (workspace == null) {
            throw new JsError(JsError.ERROR, "Workspace not found (" + StructUtils.stringifyDescriptor(descriptor) +
                                             ")");
        }
        return workspace;
    }

    @Nullable
    Workspace tryWorkspaceByLocator(Locator locator) {
        Workspace workspace = tryWorkspaceByIdent(locator);
        if (workspace == null) {
            return null;
        }
        if (StructUtils.isVirtualLocator(locator)) {
            locator = StructUtils.devirtualizeLocator(locator);
        }
        return workspace.getAnchoredLocator().locatorHash.equals(locator.locatorHash) ? workspace : null;
    }

    private void deleteDescriptor(String descriptorHash) {
        storedResolutions.remove(descriptorHash);
        storedDescriptors.remove(descriptorHash);
    }

    private void deleteLocator(String locatorHash) {
        originalPackages.remove(locatorHash);
        storedPackages.remove(locatorHash);
        accessibleLocators.remove(locatorHash);
    }

    /**
     * {@code forgetTransientResolutions}: every package whose resolver does not persist its resolution (or that no
     * resolver supports) leaves the stored maps with the descriptors that resolved to it.
     */
    void forgetTransientResolutions(Resolver resolver, Resolver.ResolveOptions opts) {
        Map<String, Set<String>> reverseLookup = new LinkedHashMap<>();
        for (Map.Entry<String, String> resolution : storedResolutions.entrySet()) {
            reverseLookup.computeIfAbsent(resolution.getValue(), k -> new LinkedHashSet<>()).add(resolution.getKey());
        }
        for (Package pkg : new ArrayList<>(originalPackages.values())) {
            boolean shouldPersistResolution;
            try {
                shouldPersistResolution = resolver.shouldPersistResolution(pkg, opts);
            } catch (RuntimeException e) {
                shouldPersistResolution = false;
            }
            if (!shouldPersistResolution) {
                deleteLocator(pkg.locatorHash);
                Set<String> descriptors = reverseLookup.remove(pkg.locatorHash);
                if (descriptors != null) {
                    for (String descriptorHash : descriptors) {
                        deleteDescriptor(descriptorHash);
                    }
                }
            }
        }
    }

    void forgetVirtualResolutions() {
        for (Package pkg : storedPackages.values()) {
            for (Map.Entry<String, Descriptor> dependency : pkg.dependencies.entrySet()) {
                if (StructUtils.isVirtualDescriptor(dependency.getValue())) {
                    dependency.setValue(StructUtils.devirtualizeDescriptor(dependency.getValue()));
                }
            }
        }
    }

    /**
     * {@code preparePackage}: the package normalized, then each dependency through the {@code reduceDependency}
     * hooks and bound by the resolver.
     */
    Package preparePackage(Package originalPkg, Resolver resolver, Resolver.ResolveOptions opts) {
        Package pkg = configuration.normalizePackage(originalPkg);
        for (Map.Entry<String, Descriptor> entry : pkg.dependencies.entrySet()) {
            Descriptor descriptor = entry.getValue();
            Descriptor dependency = configuration.reduceDependency(descriptor, this, pkg, resolver, opts);
            if (!StructUtils.areIdentsEqual(descriptor, dependency)) {
                throw new JsError(JsError.ERROR, "Assertion failed: The descriptor ident cannot be changed through " +
                                                 "aliases");
            }
            entry.setValue(resolver.bindDescriptor(dependency, pkg, opts));
        }
        return pkg;
    }

    /**
     * The package.json of a directory relative to the root, null when the project holds none there.
     * @throws CannotKnow when the host says nothing of the project's files
     */
    @Nullable
    Manifest readManifest(String cwd) {
        Manifest manifest = manifests.apply(cwd);
        if (manifest == null && paths == null) {
            throw new CannotKnow(cwd + "/package.json (the host gave no view of the project's files)");
        }
        return manifest;
    }

    /**
     * {@code xfs.readFilePromise(path, 'utf8')} of a file relative to the root: CannotKnow for a file held unread (or
     * any, when the host says nothing of the files), Node's ENOENT JsError for a file the project does not hold.
     */
    String readFile(String path) {
        String text = files.get(path);
        if (text != null) {
            return text;
        }
        if (paths == null) {
            throw new CannotKnow(path + " (the host gave no view of the project's files)");
        }
        if (paths.contains(path)) {
            throw CannotKnow.unread(path);
        }
        throw new JsError(JsError.ERROR, "ENOENT: no such file or directory, open '" + path + "'");
    }

    /**
     * {@code resolveEverything} of an install that is not lockfile-only, {@code checkResolutions} in hardened mode;
     * throws the first failing wave's failure once it settles, else a CannotKnow, unless a chain exhausts the heap.
     */
    void resolveEverything(LegacyMigrationResolver legacyMigrationResolver, RegistrySource registry,
                           boolean checkResolutions) {
        forgetVirtualResolutions();
        Resolver realResolver = configuration.makeResolver();
        forgetTransientResolutions(realResolver, new Resolver.ResolveOptions(this, realResolver, registry));

        Resolver resolver = new MultiResolver(new LockfileResolver(realResolver), legacyMigrationResolver,
                realResolver);
        Resolver noLockfileResolver = new MultiResolver(legacyMigrationResolver, realResolver);
        ResolutionPass pass = new ResolutionPass(resolver, registry,
                checkResolutions ? new Resolver.ResolveOptions(this, noLockfileResolver, registry) : null);
        for (Workspace workspace : workspaces) {
            pass.resolutionQueue.add(Collections.singletonList(workspace.getAnchoredDescriptor()));
        }
        CannotKnow unknown = null;
        RuntimeException failure = null;
        ResolutionChain.Endless exhausted = null;
        // past the wave Yarn throws from, what it scheduled resolves on in the background
        while (!pass.resolutionQueue.isEmpty()) {
            List<List<Descriptor>> copy = new ArrayList<>(pass.resolutionQueue);
            pass.resolutionQueue.clear();
            RuntimeException rejected = null;
            ResolutionChain.Endless pending = null;
            for (List<Descriptor> dependencies : copy) {
                for (Descriptor descriptor : dependencies) {
                    try {
                        pass.scheduleDescriptorResolution(descriptor);
                    } catch (CannotKnow e) {
                        if (unknown == null) {
                            unknown = e;
                        }
                    } catch (ResolutionChain.Endless e) {
                        if (e.getEnding() == ResolutionChain.Ending.HEAP_EXHAUSTED) {
                            exhausted = exhausted == null ? e : exhausted;
                        } else if (e.getEnding() == ResolutionChain.Ending.EMPTY_EVENT_LOOP) {
                            pending = pending == null ? e : pending;
                        } else if (rejected == null) {
                            rejected = e;
                        }
                    } catch (RuntimeException e) {
                        if (rejected == null) {
                            rejected = e;
                        }
                    }
                }
            }
            if (failure == null) {
                failure = pending != null ? pending : rejected;
            }
        }
        if (exhausted != null) {
            throw exhausted;
        }
        if (failure != null) {
            throw failure;
        }
        if (unknown != null) {
            throw unknown;
        }

        Set<String> volatileDescriptors = new LinkedHashSet<>();
        Set<String> newOptionalBuilds = new LinkedHashSet<>(pass.allPackages.keySet());
        Set<String> newAccessibleLocators = new LinkedHashSet<>();
        new VirtualPass(pass.allDescriptors, pass.allResolutions, pass.allPackages, newAccessibleLocators,
                newOptionalBuilds, volatileDescriptors).apply();
        newOptionalBuilds.removeAll(pass.resolutionDependencies);
        for (String descriptorHash : volatileDescriptors) {
            pass.allDescriptors.remove(descriptorHash);
            pass.allResolutions.remove(descriptorHash);
        }

        Set<String> newConditionalLocators = new LinkedHashSet<>();
        Set<String> newDisabledLocators = new LinkedHashSet<>();
        Set<String> newHostDependentLocators = new LinkedHashSet<>();
        for (Package pkg : pass.allPackages.values()) {
            if (pkg.conditions == null || !newOptionalBuilds.contains(pkg.locatorHash)) {
                continue;
            }
            Boolean compatible = isPackageCompatible(pkg);
            if (compatible == null) {
                newHostDependentLocators.add(pkg.locatorHash);
            } else if (!compatible) {
                newDisabledLocators.add(pkg.locatorHash);
            }
            newConditionalLocators.add(pkg.locatorHash);
        }

        storedResolutions = pass.allResolutions;
        storedDescriptors = pass.allDescriptors;
        storedPackages = pass.allPackages;
        accessibleLocators.clear();
        accessibleLocators.addAll(newAccessibleLocators);
        conditionalLocators.clear();
        conditionalLocators.addAll(newConditionalLocators);
        disabledLocators.clear();
        disabledLocators.addAll(newDisabledLocators);
        hostDependentLocators.clear();
        hostDependentLocators.addAll(newHostDependentLocators);
        originalPackages.clear();
        originalPackages.putAll(pass.originalPackages);
        optionalBuilds.clear();
        optionalBuilds.addAll(newOptionalBuilds);
    }

    /**
     * {@code resolveEverything}'s scheduling, a wave of dependencies at a time.
     */
    private final class ResolutionPass {
        final ResolutionChain chain = new ResolutionChain();
        final Resolver.ResolveOptions opts;
        final Resolver.@Nullable ResolveOptions checkOpts;

        final Map<String, Descriptor> allDescriptors = new LinkedHashMap<>();
        final Map<String, Package> allPackages = new LinkedHashMap<>();
        final Map<String, String> allResolutions = new LinkedHashMap<>();
        final Map<String, Package> originalPackages = new LinkedHashMap<>();
        final Set<String> resolutionDependencies = new LinkedHashSet<>();
        final List<List<Descriptor>> resolutionQueue = new ArrayList<>();

        // each hash's outcome: its package, or what it threw
        final Map<String, Object> packageResolutions = new HashMap<>();
        final Map<String, Object> descriptorResolutions = new HashMap<>();

        ResolutionPass(Resolver resolver, RegistrySource registry, Resolver.@Nullable ResolveOptions checkOpts) {
            this.opts = new Resolver.ResolveOptions(Project.this, resolver, registry, chain);
            this.checkOpts = checkOpts;
        }

        Package scheduleDescriptorResolution(Descriptor descriptor) {
            Object outcome = descriptorResolutions.get(descriptor.descriptorHash);
            if (outcome == null) {
                allDescriptors.put(descriptor.descriptorHash, descriptor);
                try {
                    outcome = startDescriptorResolution(descriptor);
                } catch (RuntimeException e) {
                    outcome = e;
                }
                descriptorResolutions.put(descriptor.descriptorHash, outcome);
            }
            if (outcome instanceof RuntimeException) {
                throw (RuntimeException) outcome;
            }
            return (Package) outcome;
        }

        private Package startDescriptorResolution(Descriptor descriptor) {
            Resolver resolver = opts.getResolver();
            Map<String, Package> resolvedDependencies = new LinkedHashMap<>();
            int outer = chain.startStep();
            try {
                for (Map.Entry<String, Descriptor> dependency :
                        resolver.getResolutionDependencies(descriptor, opts).entrySet()) {
                    Descriptor bound = resolver.bindDescriptor(dependency.getValue(),
                            getTopLevelWorkspace().getAnchoredLocator(), opts);
                    Package resolvedPackage = scheduleDescriptorResolution(bound);
                    resolutionDependencies.add(resolvedPackage.locatorHash);
                    resolvedDependencies.put(dependency.getKey(), resolvedPackage);
                }
            } finally {
                chain.endStep(outer);
            }

            List<Locator> candidateResolutions;
            try {
                candidateResolutions = resolver.getCandidates(descriptor, resolvedDependencies, opts);
            } catch (RuntimeException e) {
                throw prettify(e, StructUtils.stringifyDescriptor(descriptor));
            }
            if (candidateResolutions.isEmpty()) {
                throw new ReportError(ReportError.RESOLUTION_FAILED, StructUtils.stringifyDescriptor(descriptor) +
                                                                     ": No candidates found");
            }
            Locator finalResolution = candidateResolutions.get(0);

            if (checkOpts != null) {
                List<Locator> locators = checkOpts.getResolver().getSatisfying(descriptor, resolvedDependencies,
                        Collections.singletonList(finalResolution), checkOpts).getLocators();
                boolean found = false;
                for (Locator locator : locators) {
                    found |= locator.locatorHash.equals(finalResolution.locatorHash);
                }
                if (!found) {
                    throw new ReportError(ReportError.RESOLUTION_MISMATCH,
                            "Invalid resolution " + prettyResolution(descriptor, finalResolution));
                }
            }

            allDescriptors.put(descriptor.descriptorHash, descriptor);
            allResolutions.put(descriptor.descriptorHash, finalResolution.locatorHash);
            return schedulePackageResolution(finalResolution);
        }

        private Package schedulePackageResolution(Locator locator) {
            Object outcome = packageResolutions.get(locator.locatorHash);
            if (outcome == null) {
                try {
                    outcome = startPackageResolution(locator);
                } catch (RuntimeException e) {
                    outcome = e;
                }
                packageResolutions.put(locator.locatorHash, outcome);
            }
            if (outcome instanceof RuntimeException) {
                throw (RuntimeException) outcome;
            }
            return (Package) outcome;
        }

        private Package startPackageResolution(Locator locator) {
            Package originalPkg;
            try {
                originalPkg = opts.getResolver().resolve(locator, opts);
            } catch (RuntimeException e) {
                throw prettify(e, StructUtils.stringifyLocator(locator));
            }
            if (!StructUtils.areLocatorsEqual(locator, originalPkg)) {
                throw new JsError(JsError.ERROR, "Assertion failed: The locator cannot be changed by the resolver " +
                                                 "(went from " + StructUtils.stringifyLocator(locator) + " to " +
                                                 StructUtils.stringifyLocator(originalPkg) + ")");
            }
            originalPackages.put(originalPkg.locatorHash, originalPkg);
            Package pkg = preparePackage(originalPkg, opts.getResolver(), opts);
            resolutionQueue.add(new ArrayList<>(pkg.dependencies.values()));
            allPackages.put(pkg.locatorHash, pkg);
            return pkg;
        }
    }

    /**
     * {@code miscUtils.prettifyAsyncErrors}: Yarn's error with what failed in front of its message.
     */
    private static RuntimeException prettify(RuntimeException e, String what) {
        if (e instanceof ReportError) {
            return new ReportError(((ReportError) e).getCode(), what + ": " + e.getMessage());
        }
        if (e instanceof JsError) {
            return new JsError(((JsError) e).getName(), what + ": " + e.getMessage());
        }
        return e;
    }

    private static String prettyResolution(Descriptor descriptor, Locator resolution) {
        Descriptor devirtualized = StructUtils.isVirtualDescriptor(descriptor) ?
                StructUtils.devirtualizeDescriptor(descriptor) : descriptor;
        return StructUtils.stringifyDescriptor(devirtualized) + " \u2192 " +
               (devirtualized.identHash.equals(resolution.identHash) ? resolution.reference :
                       StructUtils.stringifyLocator(resolution));
    }

    /**
     * {@code applyVirtualResolutionMutations} without the peer requirements and warnings, which the lockfile omits.
     */
    private final class VirtualPass {
        final Map<String, Descriptor> allDescriptors;
        final Map<String, String> allResolutions;
        final Map<String, Package> allPackages;
        final Set<String> accessibleLocators;
        final Set<String> optionalBuilds;
        final Set<String> volatileDescriptors;

        final Map<String, Integer> virtualStack = new HashMap<>();
        final List<Locator> resolutionStack = new ArrayList<>();
        final Map<String, Locator> allVirtualInstances = new HashMap<>();
        final Map<String, Set<String>> allVirtualDependents = new HashMap<>();
        final Map<String, Set<String>> allVirtualResolutions = new HashMap<>();
        final Map<String, Package> originalWorkspaceDefinitions = new HashMap<>();

        VirtualPass(Map<String, Descriptor> allDescriptors, Map<String, String> allResolutions,
                    Map<String, Package> allPackages, Set<String> accessibleLocators, Set<String> optionalBuilds,
                    Set<String> volatileDescriptors) {
            this.allDescriptors = allDescriptors;
            this.allResolutions = allResolutions;
            this.allPackages = allPackages;
            this.accessibleLocators = accessibleLocators;
            this.optionalBuilds = optionalBuilds;
            this.volatileDescriptors = volatileDescriptors;
        }

        void apply() {
            for (Workspace workspace : workspaces) {
                String locatorHash = workspace.getAnchoredLocator().locatorHash;
                Package pkg = allPackages.get(locatorHash);
                if (pkg == null) {
                    throw new JsError(JsError.ERROR, "Assertion failed: The workspace should have an associated " +
                                                     "package");
                }
                originalWorkspaceDefinitions.put(locatorHash, StructUtils.copyPackage(pkg));
            }
            for (Workspace workspace : workspaces) {
                Locator locator = workspace.getAnchoredLocator();
                volatileDescriptors.remove(workspace.getAnchoredDescriptor().descriptorHash);
                resolvePeerDependencies(workspace.getAnchoredDescriptor(), locator, locator.locatorHash, false);
            }
        }

        private void reportStackOverflow() {
            throw new ReportError(ReportError.STACK_OVERFLOW_RESOLUTION, "Encountered a stack overflow when " +
                                                                         "resolving peer dependencies");
        }

        private void resolvePeerDependencies(Descriptor parentDescriptor, Locator parentLocator, String top,
                                             boolean optional) {
            if (resolutionStack.size() > 1000) {
                reportStackOverflow();
            }
            resolutionStack.add(parentLocator);
            resolvePeerDependenciesImpl(parentDescriptor, parentLocator, top, optional);
            resolutionStack.remove(resolutionStack.size() - 1);
        }

        private void resolvePeerDependenciesImpl(Descriptor parentDescriptor, Locator parentLocator, String top,
                                                 boolean optional) {
            if (!optional) {
                optionalBuilds.remove(parentLocator.locatorHash);
            }
            if (!accessibleLocators.add(parentLocator.locatorHash)) {
                return;
            }
            Package parentPackage = allPackages.get(parentLocator.locatorHash);
            if (parentPackage == null) {
                throw new JsError(JsError.ERROR, "Assertion failed: The package (" +
                                                 StructUtils.stringifyLocator(parentLocator) + ") should have been " +
                                                 "registered");
            }

            Set<String> dedupeCandidates = new LinkedHashSet<>();
            Map<String, Descriptor> parentPeerProvisions = new HashMap<>();
            List<Runnable> firstPass = new ArrayList<>();
            List<Runnable> secondPass = new ArrayList<>();
            List<Runnable> thirdPass = new ArrayList<>();
            List<Runnable> fourthPass = new ArrayList<>();

            for (Descriptor descriptor : new ArrayList<>(parentPackage.dependencies.values())) {
                if (parentPackage.peerDependencies.containsKey(descriptor.identHash) &&
                    !parentPackage.locatorHash.equals(top)) {
                    continue;
                }
                if (StructUtils.isVirtualDescriptor(descriptor)) {
                    throw new JsError(JsError.ERROR, "Assertion failed: Virtual packages shouldn't be encountered " +
                                                     "when virtualizing a branch");
                }
                volatileDescriptors.remove(descriptor.descriptorHash);

                boolean isOptional = optional;
                if (!isOptional) {
                    Map<@Nullable String, Map<String, Object>> dependencyMetaSet =
                            parentPackage.dependenciesMeta.get(StructUtils.stringifyIdent(descriptor));
                    Map<String, Object> dependencyMeta = dependencyMetaSet == null ? null : dependencyMetaSet.get(null);
                    if (dependencyMeta != null && Js.truthy(dependencyMeta.get("optional"))) {
                        isOptional = true;
                    }
                }
                boolean dependencyOptional = isOptional;

                String resolution = allResolutions.get(descriptor.descriptorHash);
                if (resolution == null) {
                    throw new JsError(JsError.ERROR, "Assertion failed: The resolution (" +
                                                     StructUtils.stringifyDescriptor(descriptor) + ") should have " +
                                                     "been registered");
                }
                Package original = originalWorkspaceDefinitions.get(resolution);
                Package pkg = original != null ? original : allPackages.get(resolution);
                if (pkg == null) {
                    throw new JsError(JsError.ERROR, "Assertion failed: The package (" + resolution + ", resolved " +
                                                     "from " + StructUtils.stringifyDescriptor(descriptor) + ") " +
                                                     "should have been registered");
                }
                if (pkg.peerDependencies.isEmpty()) {
                    resolvePeerDependencies(descriptor, pkg, top, dependencyOptional);
                    continue;
                }

                Descriptor[] virtualizedDescriptor = new Descriptor[1];
                Package[] virtualizedPackage = new Package[1];
                Set<String> missingPeerDependencies = new LinkedHashSet<>();

                firstPass.add(() -> {
                    virtualizedDescriptor[0] = StructUtils.virtualizeDescriptor(descriptor,
                            parentLocator.locatorHash);
                    virtualizedPackage[0] = StructUtils.virtualizePackage(pkg, parentLocator.locatorHash);
                    parentPackage.dependencies.put(descriptor.identHash, virtualizedDescriptor[0]);
                    allResolutions.put(virtualizedDescriptor[0].descriptorHash, virtualizedPackage[0].locatorHash);
                    allDescriptors.put(virtualizedDescriptor[0].descriptorHash, virtualizedDescriptor[0]);
                    allPackages.put(virtualizedPackage[0].locatorHash, virtualizedPackage[0]);
                    allVirtualResolutions.computeIfAbsent(virtualizedPackage[0].locatorHash,
                            k -> new LinkedHashSet<>()).add(virtualizedDescriptor[0].descriptorHash);
                    dedupeCandidates.add(virtualizedPackage[0].locatorHash);
                });

                secondPass.add(() -> {
                    Package virtual = virtualizedPackage[0];
                    for (Descriptor peerDescriptor : new ArrayList<>(virtual.peerDependencies.values())) {
                        Descriptor peerProvision = parentPeerProvisions.computeIfAbsent(peerDescriptor.identHash,
                                k -> provide(parentPackage, parentDescriptor, parentLocator, peerDescriptor));
                        if ("missing:".equals(peerProvision.range) &&
                            virtual.dependencies.containsKey(peerDescriptor.identHash)) {
                            virtual.peerDependencies.remove(peerDescriptor.identHash);
                            continue;
                        }
                        virtual.dependencies.put(peerDescriptor.identHash, peerProvision);
                        if (StructUtils.isVirtualDescriptor(peerProvision)) {
                            String dependentLocatorHash = allResolutions.get(peerProvision.descriptorHash);
                            allVirtualDependents.computeIfAbsent(String.valueOf(dependentLocatorHash),
                                    k -> new LinkedHashSet<>()).add(virtual.locatorHash);
                        }
                        if ("missing:".equals(peerProvision.range)) {
                            missingPeerDependencies.add(peerProvision.identHash);
                        }
                    }
                    Map<String, Descriptor> sorted = new LinkedHashMap<>();
                    for (Map.Entry<String, Descriptor> entry : MiscUtils.sortMap(virtual.dependencies.entrySet(),
                            e -> StructUtils.stringifyIdent(e.getValue()))) {
                        sorted.put(entry.getKey(), entry.getValue());
                    }
                    virtual.dependencies = sorted;
                });

                thirdPass.add(() -> {
                    if (!allPackages.containsKey(virtualizedPackage[0].locatorHash)) {
                        return;
                    }
                    Integer stackDepth = virtualStack.get(pkg.locatorHash);
                    if (stackDepth != null && stackDepth >= 2) {
                        reportStackOverflow();
                    }
                    int next = stackDepth != null ? stackDepth + 1 : 1;
                    virtualStack.put(pkg.locatorHash, next);
                    resolvePeerDependencies(virtualizedDescriptor[0], virtualizedPackage[0], top,
                            dependencyOptional);
                    virtualStack.put(pkg.locatorHash, next - 1);
                });

                fourthPass.add(() -> {
                    if (!allPackages.containsKey(virtualizedPackage[0].locatorHash)) {
                        return;
                    }
                    for (String missingPeerDependency : missingPeerDependencies) {
                        virtualizedPackage[0].dependencies.remove(missingPeerDependency);
                    }
                });
            }

            firstPass.forEach(Runnable::run);
            secondPass.forEach(Runnable::run);

            while (!dedupeCandidates.isEmpty()) {
                String locatorHash = dedupeCandidates.iterator().next();
                dedupeCandidates.remove(locatorHash);
                dedupe(allPackages.get(locatorHash), top, dedupeCandidates);
            }

            thirdPass.forEach(Runnable::run);
            fourthPass.forEach(Runnable::run);
        }

        // the peer provision a parent gives its dependencies: a sibling, the parent itself, or nothing
        private Descriptor provide(Package parentPackage, Descriptor parentDescriptor, Locator parentLocator,
                                   Descriptor peerDescriptor) {
            Descriptor peerProvision = parentPackage.dependencies.get(peerDescriptor.identHash);
            if (peerProvision == null && StructUtils.areIdentsEqual(parentLocator, peerDescriptor)) {
                if (parentDescriptor.identHash.equals(parentLocator.identHash)) {
                    peerProvision = parentDescriptor;
                } else {
                    peerProvision = StructUtils.makeDescriptor(parentLocator, parentDescriptor.range);
                    allDescriptors.put(peerProvision.descriptorHash, peerProvision);
                    allResolutions.put(peerProvision.descriptorHash, parentLocator.locatorHash);
                    volatileDescriptors.remove(peerProvision.descriptorHash);
                }
            }
            return peerProvision != null ? peerProvision : StructUtils.makeDescriptor(peerDescriptor, "missing:");
        }

        // a virtual package with the same dependencies as one met before resolves to that one instead
        private void dedupe(Package virtualPackage, String top, Set<String> dedupeCandidates) {
            List<String> parts = new ArrayList<>();
            parts.add(StructUtils.devirtualizeLocator(virtualPackage).locatorHash);
            for (Descriptor descriptor : virtualPackage.dependencies.values()) {
                String resolution = !"missing:".equals(descriptor.range) ?
                        allResolutions.get(descriptor.descriptorHash) : "missing:";
                if (resolution == null) {
                    throw new JsError(JsError.ERROR, "Assertion failed: Expected the resolution for " +
                                                     StructUtils.stringifyDescriptor(descriptor) + " to have been " +
                                                     "registered");
                }
                parts.add(resolution.equals(top) ? resolution + " (top)" : resolution);
            }
            String dependencyHash = HashUtils.makeHash(parts.toArray(new String[0]));

            Locator masterLocator = allVirtualInstances.get(dependencyHash);
            if (masterLocator == null) {
                allVirtualInstances.put(dependencyHash, virtualPackage);
                return;
            }
            Set<String> masterResolutions = allVirtualResolutions.computeIfAbsent(masterLocator.locatorHash,
                    k -> new LinkedHashSet<>());
            Set<String> resolutions = allVirtualResolutions.get(virtualPackage.locatorHash);
            if (resolutions != null) {
                for (String descriptorHash : new ArrayList<>(resolutions)) {
                    allResolutions.put(descriptorHash, masterLocator.locatorHash);
                    masterResolutions.add(descriptorHash);
                }
            }
            allPackages.remove(virtualPackage.locatorHash);
            accessibleLocators.remove(virtualPackage.locatorHash);
            dedupeCandidates.remove(virtualPackage.locatorHash);

            Set<String> dependents = allVirtualDependents.get(virtualPackage.locatorHash);
            if (dependents != null) {
                Set<String> masterDependents = allVirtualDependents.computeIfAbsent(masterLocator.locatorHash,
                        k -> new LinkedHashSet<>());
                for (String dependent : new ArrayList<>(dependents)) {
                    masterDependents.add(dependent);
                    dedupeCandidates.add(dependent);
                }
            }
        }
    }

    /**
     * {@code fetchEverything} of an install that only updates the lockfile.
     * @throws CannotKnow naming every checksum the engine cannot compute, when Yarn's own fetches all succeed
     */
    void fetchEverything(RegistrySource registry) {
        Fetcher fetcher = configuration.makeFetcher();
        Fetcher.FetchOptions opts = new Fetcher.FetchOptions(this, fetcher, registry);
        Set<String> locatorHashes = new LinkedHashSet<>(MiscUtils.sortMap(storedResolutions.values(), locatorHash -> {
            Package pkg = storedPackages.get(locatorHash);
            if (pkg == null) {
                throw new JsError(JsError.ERROR, "Assertion failed: The locator should have been registered");
            }
            return StructUtils.stringifyLocator(pkg);
        }));
        locatorHashes.removeIf(storedChecksums::containsKey);

        RuntimeException firstError = null;
        List<String> unknown = new ArrayList<>();
        for (String locatorHash : locatorHashes) {
            Package pkg = storedPackages.get(locatorHash);
            if (StructUtils.isVirtualLocator(pkg)) {
                continue;
            }
            String checksum;
            try {
                checksum = fetcher.fetch(pkg, opts);
            } catch (CannotKnow e) {
                unknown.add(e.getSubject());
                continue;
            } catch (ReportError | JsError e) {
                if (firstError == null) {
                    firstError = prettify(e, StructUtils.stringifyLocator(pkg));
                }
                continue;
            }
            if (checksum != null) {
                storedChecksums.put(pkg.locatorHash, checksum);
            } else {
                storedChecksums.remove(pkg.locatorHash);
            }
        }
        if (firstError != null) {
            throw firstError;
        }
        if (!unknown.isEmpty()) {
            throw new UnknownChecksums(String.join("; ", unknown));
        }
    }

    /**
     * {@code generateLockfile}: each original package under its descriptors, sorted and joined, with the header.
     */
    String generateLockfile() {
        Map<String, Set<String>> reverseLookup = new LinkedHashMap<>();
        for (Map.Entry<String, String> resolution : storedResolutions.entrySet()) {
            reverseLookup.computeIfAbsent(resolution.getValue(), k -> new LinkedHashSet<>()).add(resolution.getKey());
        }
        Map<String, Object> optimizedLockfile = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        String cacheKey = Cache.getCacheKey(configuration);
        metadata.put("version", configuration.getRelease().getLockfileVersion());
        metadata.put("cacheKey", cacheKey);
        optimizedLockfile.put("__metadata", metadata);

        for (Map.Entry<String, Set<String>> entry : reverseLookup.entrySet()) {
            Package pkg = originalPackages.get(entry.getKey());
            if (pkg == null) {
                continue;
            }
            List<String> descriptors = new ArrayList<>();
            for (String descriptorHash : entry.getValue()) {
                Descriptor descriptor = storedDescriptors.get(descriptorHash);
                if (descriptor == null) {
                    throw new JsError(JsError.ERROR, "Assertion failed: The descriptor should have been registered");
                }
                descriptors.add(StructUtils.stringifyDescriptor(descriptor));
            }
            descriptors.sort(String::compareTo);
            String key = String.join(", ", descriptors);

            Manifest manifest = new Manifest();
            manifest.version = Package.HARD.equals(pkg.linkType) ? pkg.version : "0.0.0-use.local";
            manifest.languageName = pkg.languageName;
            manifest.dependencies = new LinkedHashMap<>(pkg.dependencies);
            manifest.peerDependencies = new LinkedHashMap<>(pkg.peerDependencies);
            manifest.dependenciesMeta = new LinkedHashMap<>(pkg.dependenciesMeta);
            manifest.peerDependenciesMeta = new LinkedHashMap<>(pkg.peerDependenciesMeta);
            manifest.bin = new LinkedHashMap<>(pkg.bin);

            Map<String, Object> lockEntry = manifest.exportTo(false);
            lockEntry.put("linkType", pkg.linkType.toLowerCase(Locale.ROOT));
            lockEntry.put("resolution", StructUtils.stringifyLocator(pkg));
            String checksum = storedChecksums.get(pkg.locatorHash);
            if (checksum != null && configuration.getRelease().writesChecksumWithoutCacheKey()) {
                int cacheKeyIndex = checksum.indexOf('/');
                if (cacheKeyIndex == -1) {
                    throw new JsError(JsError.ERROR, "Assertion failed: Expected the checksum to reference its " +
                                                     "cache key");
                }
                if (checksum.substring(0, cacheKeyIndex).equals(cacheKey)) {
                    checksum = checksum.substring(cacheKeyIndex + 1);
                }
            }
            lockEntry.put("checksum", checksum != null ? checksum : Syml.UNDEFINED);
            lockEntry.put("conditions", Js.truthy(pkg.conditions) ? pkg.conditions : Syml.UNDEFINED);
            optimizedLockfile.put(key, lockEntry);
        }

        String header = "# This file is generated by running \"yarn install\" inside your project.\n" +
                        "# Manual changes might be lost - proceed with caution!\n" +
                        "\n";
        return header + Syml.stringifySyml(optimizedLockfile);
    }

    /**
     * {@code persistLockfile}: the generated lockfile with the current one's line endings ({@code hostEol} when it has
     * none); null when that leaves the file as it is.
     */
    @Nullable
    String persistLockfile(String hostEol) {
        String currentContent = lockfileContent != null ? lockfileContent : "";
        String normalizedContent = normalizeLineEndings(currentContent, generateLockfile(), hostEol);
        return normalizedContent.equals(currentContent) ? null : normalizedContent;
    }

    static String normalizeLineEndings(String originalContent, String newContent, String hostEol) {
        Matcher m = EOL.matcher(originalContent);
        int crlf = 0;
        int all = 0;
        while (m.find()) {
            all++;
            if (m.group().length() == 2) {
                crlf++;
            }
        }
        String eol = all == 0 ? hostEol : crlf > all - crlf ? "\r\n" : "\n";
        return EOL.matcher(newContent).replaceAll(Matcher.quoteReplacement(eol));
    }

}
