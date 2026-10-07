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
package org.openrewrite.javascript.internal.lock.bun;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/PackageManager/install_with_manager.zig (bun 1.3.10) for {@code bun install
 * --lockfile-only}, up to the resolver: the lock loaded ({@code loadFromCwd}), the config version chosen, the root
 * package.json diffed against the lock's root and swapped in, and the edges the install enqueues collected in
 * the order bun enqueues them. When nothing is enqueued, {@link PendingInstall#save()} cleans the lockfile and
 * writes it, as bun does after its resolver has nothing to do.
 */
public final class BunLockRegenerator {

    /**
     * What the engine is given for a Bun project.
     */
    @Value
    @Builder
    public static class Inputs {
        /**
         * The bun.lock; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The project's manifests as edited, by location: {@code ""} the root, {@code packages/a} a member.
         */
        @Singular
        Map<String, String> manifests;

        @Nullable
        String npmrc;

        @Nullable
        String bunfig;

        @Singular("env")
        Map<String, String> env;
    }

    /**
     * {@code Lockfile.LoadResult}: a lock that was not found, one that failed to load (bun prints the error and
     * ignores it), or one that loaded.
     */
    @Getter
    public static final class LoadResult {
        private final @Nullable Lockfile lockfile;
        private final TextLockfileReader.@Nullable ParseError error;

        private LoadResult(@Nullable Lockfile lockfile, TextLockfileReader.@Nullable ParseError error) {
            this.lockfile = lockfile;
            this.error = error;
        }

        boolean isOk() {
            return lockfile != null;
        }

        /**
         * {@code chooseConfigVersion}: the lock's, 0 for a lock without one, the current version for a new lock.
         */
        int chooseConfigVersion() {
            if (lockfile == null) {
                return 1;
            }
            return lockfile.savedConfigVersion != null ? lockfile.savedConfigVersion : 0;
        }
    }

    /**
     * The install after the root's diff: the lockfile, and the dependency ids bun enqueues, in order.
     */
    @Getter
    public static final class PendingInstall {
        private final LoadResult loadResult;
        private final Lockfile lockfile;
        private final int configVersion;
        private final boolean needsNewLockfile;
        private final PackageDiff.@Nullable Summary summary;
        private final List<Integer> enqueued;
        private final Log log;

        PendingInstall(LoadResult loadResult, Lockfile lockfile, int configVersion, boolean needsNewLockfile,
                       PackageDiff.@Nullable Summary summary, List<Integer> enqueued, Log log) {
            this.loadResult = loadResult;
            this.lockfile = lockfile;
            this.configVersion = configVersion;
            this.needsNewLockfile = needsNewLockfile;
            this.summary = summary;
            this.enqueued = enqueued;
            this.log = log;
        }

        /**
         * {@code cleanWithLogger} then {@code saveLockfile}: the bun.lock text, or null when bun deletes the empty
         * lockfile. Only meaningful once nothing is left to resolve.
         */
        @Nullable
        String save() {
            if (!enqueued.isEmpty()) {
                throw new IllegalStateException("the install enqueued " + enqueued.size() + " dependencies to resolve");
            }
            Lockfile cleaned = Cleaner.clean(lockfile);
            if (cleaned.isEmpty()) {
                return null;
            }
            return TextLockfileWriter.saveFromBinary(cleaned, configVersion);
        }
    }

    private BunLockRegenerator() {
    }

    static LoadResult loadFromCwd(@Nullable String lock, BunOptions options) {
        if (lock == null) {
            return new LoadResult(null, null);
        }
        try {
            return new LoadResult(TextLockfileReader.load(lock, options), null);
        } catch (TextLockfileReader.ParseError e) {
            return new LoadResult(null, e);
        }
    }

    static PendingInstall installWithManager(Inputs inputs) {
        BunOptions options = BunOptions.load(inputs.getBunfig(), inputs.getNpmrc(), inputs.getEnv());
        Log log = new Log();
        LoadResult loadResult = loadFromCwd(inputs.getLock(), options);
        int configVersion = loadResult.chooseConfigVersion();
        Map<String, String> manifests = inputs.getManifests();
        String rootManifest = manifests.get("");
        if (rootManifest == null) {
            throw new IllegalArgumentException("the root package.json is required");
        }
        Json.Expr rootJson = Json.parse(rootManifest);

        Lockfile managerLockfile = loadResult.isOk() ? loadResult.getLockfile() : new Lockfile();
        boolean needsNewLockfile = !loadResult.isOk();
        PackageDiff.Summary summary = null;
        List<Integer> enqueued = new ArrayList<>();

        if (loadResult.isOk()) {
            Package root = managerLockfile.rootPackage();
            if (root == null || root.dependenciesLen == 0) {
                needsNewLockfile = true;
            } else {
                Lockfile lockfile = new Lockfile();
                Package maybeRoot = new Package();
                maybeRoot.parseWithJSON(lockfile, options, log, "", rootJson, Features.MAIN, manifests);
                int[] mapping = new int[maybeRoot.dependenciesLen];
                Arrays.fill(mapping, INVALID_PACKAGE_ID);
                summary = PackageDiff.generate(options, log, managerLockfile, lockfile, root, maybeRoot, mapping,
                        manifests);
                if (summary.hasDiffs()) {
                    replaceRoot(managerLockfile, lockfile, root, maybeRoot, mapping, summary, enqueued);
                }
            }
        }

        if (needsNewLockfile) {
            managerLockfile = new Lockfile();
            Package root = new Package();
            root.parseWithJSON(managerLockfile, options, log, "", rootJson, Features.MAIN, manifests);
            managerLockfile.appendPackage(root);
            for (int depId = root.dependenciesOff; depId < root.dependenciesEnd(); depId++) {
                enqueued.add(depId);
            }
        }
        return new PendingInstall(loadResult, managerLockfile, configVersion, needsNewLockfile, summary, enqueued, log);
    }

    private static void replaceRoot(Lockfile manager, Lockfile lockfile, Package root, Package maybeRoot, int[] mapping,
                                    PackageDiff.Summary summary, List<Integer> enqueued) {
        List<Dependency> newDependencies = new ArrayList<>(lockfile.dependenciesOf(maybeRoot));
        int off = manager.dependencies.size();
        int len = newDependencies.size();
        Package managerRoot = manager.packages.get(0);
        int oldOff = managerRoot.dependenciesOff;
        int oldLen = managerRoot.dependenciesLen;
        managerRoot.dependenciesOff = off;
        managerRoot.dependenciesLen = len;

        Set<Long> allNameHashes = new LinkedHashSet<>();
        if (summary.overridesChanged) {
            allNameHashes.addAll(manager.overrides.map.keySet());
            allNameHashes.addAll(lockfile.overrides.map.keySet());
        }

        manager.overrides = lockfile.overrides.cloneMap();
        manager.catalogs = lockfile.catalogs.cloneMap();
        manager.trustedDependencies = lockfile.trustedDependencies == null ? null :
                new LinkedHashSet<>(lockfile.trustedDependencies);

        List<Integer> oldResolutions = new ArrayList<>(manager.resolutions.subList(oldOff, oldOff + oldLen));
        for (int i = 0; i < len; i++) {
            manager.dependencies.add(newDependencies.get(i).cloneDependency());
            manager.resolutions.add(mapping[i] != INVALID_PACKAGE_ID ? oldResolutions.get(mapping[i]) :
                    INVALID_PACKAGE_ID);
        }

        manager.workspacePaths = new LinkedHashMap<>(lockfile.workspacePaths);
        manager.workspaceVersions = new LinkedHashMap<>(lockfile.workspaceVersions);

        for (Map.Entry<Long, String> entry : lockfile.patchedDependencies.entrySet()) {
            String existing = manager.patchedDependencies.get(entry.getKey());
            if (existing == null || !existing.equals(entry.getValue())) {
                manager.patchedDependencies.put(entry.getKey(), entry.getValue());
            }
        }
        manager.patchedDependencies.keySet().retainAll(lockfile.patchedDependencies.keySet());

        if (summary.overridesChanged && !allNameHashes.isEmpty()) {
            for (int i = 0; i < manager.dependencies.size(); i++) {
                if (allNameHashes.contains(manager.dependencies.get(i).nameHash)) {
                    manager.resolutions.set(i, INVALID_PACKAGE_ID);
                    enqueued.add(i);
                }
            }
        }
        if (summary.catalogsChanged) {
            for (int i = 0; i < manager.dependencies.size(); i++) {
                if (manager.dependencies.get(i).version.tag == DependencyVersion.Tag.CATALOG) {
                    manager.resolutions.set(i, INVALID_PACKAGE_ID);
                    enqueued.add(i);
                }
            }
        }
        if (summary.add > 0 || summary.update > 0) {
            for (int i = 0; i < mapping.length; i++) {
                if (mapping[i] == INVALID_PACKAGE_ID) {
                    enqueued.add(off + i);
                }
            }
        }
    }
}
