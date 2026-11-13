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

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @pnpm/installing.env-installer src/pruneEnvLockfile.ts (pnpm 11.2.2).
 */
final class PruneEnvLockfile {

    private PruneEnvLockfile() {
    }

    static LockfileObject convertToLockfileEnvObject(EnvLockfile envLockfile) {
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>(envLockfile.configDependencies());
        if (envLockfile.packageManagerDependencies() != null) {
            dependencies.putAll(envLockfile.packageManagerDependencies());
        }
        Map<String, @Nullable Object> importer = new LinkedHashMap<>();
        importer.put("dependencies", dependencies);
        Map<String, @Nullable Object> importers = new LinkedHashMap<>();
        importers.put(".", importer);
        Map<String, @Nullable Object> file = new LinkedHashMap<>();
        file.put("lockfileVersion", envLockfile.getDocument().get("lockfileVersion"));
        file.put("importers", importers);
        file.put("packages", envLockfile.packages());
        file.put("snapshots", envLockfile.snapshots());
        return LockfileFormatConverters.convertToLockfileObject(file);
    }

    static void pruneEnvLockfile(EnvLockfile envLockfile) {
        replacePrunedEntries(envLockfile, convertToLockfileEnvObject(envLockfile));
    }

    // what both pruneEnvLockfile and resolvePackageManagerIntegrities do with the lock they prune
    static void replacePrunedEntries(EnvLockfile envLockfile, LockfileObject lockfileObject) {
        LockfileObject pruned = UpdateLockfile.pruneSharedLockfile(lockfileObject, null);
        Map<String, @Nullable Object> prunedFile = LockfileFormatConverters.convertToLockfileFile(pruned);
        Object packages = prunedFile.get("packages");
        Object snapshots = prunedFile.get("snapshots");
        envLockfile.getDocument().put("packages", packages == null ? new LinkedHashMap<>() : packages);
        envLockfile.getDocument().put("snapshots", snapshots == null ? new LinkedHashMap<>() : snapshots);
    }
}
