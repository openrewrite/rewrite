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
package org.openrewrite.javascript.internal.lock;

import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * What a regeneration asks one package manager's port: its lock, the project files it reads, where it installs, its
 * install over the lock, and what it reads of the files the recipes edit.
 */
public interface Port {

    String lockFile();

    /**
     * Whether the package manager reads the contents of a project file of this name.
     */
    boolean reads(String fileName);

    /**
     * Whether it reads a file of this name only from its lock's directory.
     */
    default boolean readsOnlyBesideLock(String fileName) {
        return false;
    }

    /**
     * Whether it reads a project's manifest from a file of this name, as it reads a package.json.
     */
    default boolean readsAsManifest(String fileName) {
        return false;
    }

    /**
     * Whether its install also reads the locks of the workspace's other projects.
     */
    default boolean readsOtherLocks() {
        return false;
    }

    /**
     * Whether its install reads and rewrites a file of this name beside its lock, which the regeneration writes too.
     */
    default boolean writesBesideLock(String fileName) {
        return false;
    }

    /**
     * Where the package manager installs when run in the edited manifest's directory, by its own walk.
     */
    InstallRoot installRoot(LockDirectory dir);

    /**
     * What the package manager reads of the files the dependency recipes edit.
     */
    ManifestRules manifestRules();

    Result regenerate(LockDirectory dir, HostInputs host, NpmRegistryClient client);
}
