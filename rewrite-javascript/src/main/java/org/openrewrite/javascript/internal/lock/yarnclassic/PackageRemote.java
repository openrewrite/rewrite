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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Ssri;

/**
 * Port of yarn src/types.js {@code PackageRemote} (yarn 1.22.22): where a package comes from. Patterns whose
 * remotes share a {@link Lockfile#keyForRemote key} and a name share one lock entry.
 */
final class PackageRemote {

    /**
     * {@code tarball}, {@code git}, {@code copy}, {@code link} or {@code workspace}: the fetcher yarn picks.
     */
    @Nullable
    String type;

    @Nullable
    Object registry;

    @Nullable
    String reference;

    @Nullable
    String resolved;

    @Nullable
    String hash;

    /**
     * The integrity as yarn holds it: the lock's string for a reused entry, an {@link Ssri.Integrity} for a fresh
     * one.
     */
    @Nullable
    Object integrity;

    @Nullable
    Object packageName;

    /**
     * A workspace's: the registry's tarball of the same version, which a {@code --focus} install fetches.
     */
    @Nullable
    PackageRemote registryRemote;

    PackageRemote copy() {
        PackageRemote copy = new PackageRemote();
        copy.type = type;
        copy.registry = registry;
        copy.reference = reference;
        copy.resolved = resolved;
        copy.hash = hash;
        copy.integrity = integrity;
        copy.packageName = packageName;
        copy.registryRemote = registryRemote;
        return copy;
    }
}
