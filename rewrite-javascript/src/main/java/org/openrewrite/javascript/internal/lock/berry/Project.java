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

import java.util.ArrayDeque;
import java.util.ArrayList;
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

/**
 * Port of @yarnpkg/core sources/Project.ts (Yarn 4.10.3).
 */
final class Project {

    static final int LOCKFILE_VERSION = 8;
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
    final Map<String, Package> originalPackages = new LinkedHashMap<>();
    final Set<String> optionalBuilds = new LinkedHashSet<>();

    /**
     * {@code __metadata.version} as the lockfile spells it ({@code "8"}), {@code -1} for a Yarn 1 lockfile, null
     * for none.
     */
    @Nullable
    Object lockfileLastVersion;

    boolean lockfileNeedsRefresh;

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

    Project(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * {@code setupResolutions}: every entry of a lockfile with {@code __metadata} becomes an original package,
     * its checksum and the descriptors of its key; a Yarn 1 lockfile only sets {@link #lockfileLastVersion}.
     */
    void setupResolutions(@Nullable String content) {
        storedResolutions = new LinkedHashMap<>();
        storedDescriptors = new LinkedHashMap<>();
        storedPackages = new LinkedHashMap<>();
        lockfileContent = content;
        parsedLockfile = null;
        String defaultLanguageName = configuration.getOptions().getString("defaultLanguageName");
        if (content == null) {
            return;
        }
        Map<String, @Nullable Object> parsed = parsedLockfile = Syml.parseSyml(content);
        Object metadata = parsed.get("__metadata");
        if (Js.truthy(metadata)) {
            Object lockfileVersion = Js.get(metadata, "version");
            Object cacheKey = Js.get(metadata, "cacheKey");
            lockfileLastVersion = lockfileVersion;
            lockfileNeedsRefresh = toNumber(lockfileVersion) < LOCKFILE_VERSION;

            for (String key : Js.keys(parsed)) {
                if ("__metadata".equals(key)) {
                    continue;
                }
                Object data = parsed.get(key);
                if (data == null) {
                    throw new IllegalStateException("TypeError: Cannot read properties of null (reading 'resolution')");
                }
                Object resolution = Js.get(data, "resolution");
                if (resolution == Syml.UNDEFINED) {
                    throw new IllegalStateException("Assertion failed: Expected the lockfile entry to have a " +
                                                    "resolution field (" + key + ")");
                }
                if (!(resolution instanceof String)) {
                    throw new IllegalStateException("TypeError: string.match is not a function");
                }
                Locator locator = StructUtils.parseLocator((String) resolution, true);

                Manifest manifest = new Manifest();
                manifest.load(data, true);

                String version = manifest.version;
                String languageName = manifest.languageName != null && !manifest.languageName.isEmpty() ?
                        manifest.languageName : defaultLanguageName;
                Object rawLinkType = Js.get(data, "linkType");
                if (!(rawLinkType instanceof String)) {
                    throw new IllegalStateException(rawLinkType == Syml.UNDEFINED || rawLinkType == null ?
                            "TypeError: Cannot read properties of " + (rawLinkType == null ? "null" : "undefined") +
                            " (reading 'toUpperCase')" : "TypeError: data.linkType.toUpperCase is not a function");
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
                    if (toNumber(lockfileVersion) <= 6) {
                        descriptor = configuration.normalizeDependency(descriptor);
                        // `$1` names a group inside the lookahead, which never captures: it is replaced by nothing
                        Matcher legacyPatch = LEGACY_PATCH.matcher(descriptor.range);
                        descriptor = StructUtils.makeDescriptor(descriptor,
                                legacyPatch.find() ? "npm%3A" + descriptor.range.substring(legacyPatch.end()) :
                                        descriptor.range);
                    }
                    storedDescriptors.put(descriptor.descriptorHash, descriptor);
                    storedResolutions.put(descriptor.descriptorHash, locator.locatorHash);
                }
            }
        } else if (content.contains("yarn lockfile v1")) {
            lockfileLastVersion = -1;
        }
    }

    /**
     * {@code setupWorkspaces}: when a workspace's setup ends it claims each member it lists that no workspace has
     * claimed and starts that member's setup; the setups end in the order they start, and the workspaces are added
     * as the reducer's promise chain adds them, each followed by the members it claimed.
     *
     * @param manifests a directory's package.json by its path relative to the root ({@code .} the root), null
     *                  without one
     * @throws CannotKnow when two workspaces whose setups may end in either order list one member
     */
    void setupWorkspaces(FastGlob fastGlob, Function<String, @Nullable Manifest> manifests) {
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
            throw new IllegalStateException("Duplicate workspace name " +
                                            StructUtils.stringifyIdent(workspace.getAnchoredLocator()) + ": " +
                                            workspace.getRelativeCwd() + " conflicts with " + dup.getRelativeCwd());
        }
        workspaces.add(workspace);
        workspacesByCwd.put(workspace.getRelativeCwd(), workspace);
        workspacesByIdent.put(workspace.getAnchoredLocator().identHash, workspace);
    }

    Workspace getTopLevelWorkspace() {
        return getWorkspaceByCwd(".");
    }

    @Nullable
    Workspace tryWorkspaceByCwd(String workspaceCwd) {
        String normalized = PortablePath.normalize(workspaceCwd).replaceAll("/+$", "");
        return workspacesByCwd.get(normalized.isEmpty() ? "." : normalized);
    }

    Workspace getWorkspaceByCwd(String workspaceCwd) {
        Workspace workspace = tryWorkspaceByCwd(workspaceCwd);
        if (workspace == null) {
            throw new IllegalStateException("Workspace not found (" + workspaceCwd + ")");
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
                SemverUtils.validRange(specifier) == null) {
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
            throw new IllegalStateException("Workspace not found (" + StructUtils.stringifyDescriptor(descriptor) +
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
     * {@code getDependencyMeta}: the top-level workspace's {@code dependenciesMeta} for an ident, its exact-version
     * entry over its plain one.
     */
    Map<String, Object> getDependencyMeta(Ident ident, @Nullable String version) {
        Map<String, Object> dependencyMeta = new LinkedHashMap<>();
        Map<@Nullable String, Map<String, Object>> dependencyMetaSet =
                getTopLevelWorkspace().getManifest().dependenciesMeta.get(StructUtils.stringifyIdent(ident));
        if (dependencyMetaSet == null) {
            return dependencyMeta;
        }
        Map<String, Object> defaultMeta = dependencyMetaSet.get(null);
        if (defaultMeta != null) {
            dependencyMeta.putAll(defaultMeta);
        }
        if (version == null || !SemverUtils.valid(version)) {
            return dependencyMeta;
        }
        for (Map.Entry<@Nullable String, Map<String, Object>> meta : dependencyMetaSet.entrySet()) {
            if (meta.getKey() != null && meta.getKey().equals(version)) {
                dependencyMeta.putAll(meta.getValue());
            }
        }
        return dependencyMeta;
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
                throw new IllegalStateException("Assertion failed: The descriptor ident cannot be changed through " +
                                                "aliases");
            }
            entry.setValue(resolver.bindDescriptor(dependency, pkg, opts));
        }
        return pkg;
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
        metadata.put("version", LOCKFILE_VERSION);
        metadata.put("cacheKey", Cache.getCacheKey(configuration.getOptions()));
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
                    throw new IllegalStateException("Assertion failed: The descriptor should have been registered");
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
     * {@code persistLockfile}: the generated lockfile with the line endings of the current one; null when that
     * leaves the file as it is.
     *
     * @param hostEol the host's {@code os.EOL}, used when the current file has no line break
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

    // JavaScript's relational comparison of a lockfile scalar with a number
    static double toNumber(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        if (value == null) {
            return 0;
        }
        return value instanceof String ? Js.stringToNumber((String) value) : Double.NaN;
    }
}
