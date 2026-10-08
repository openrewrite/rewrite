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
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.types {@code LockfileObject} (pnpm 11.2.2): the lock as pnpm holds it in memory, the
 * file's {@code packages} and {@code snapshots} merged per dependency path and each importer's specifiers split
 * out. Every other top-level key ({@code lockfileVersion}, {@code settings}, {@code catalogs}, {@code overrides},
 * the checksums, {@code patchedDependencies}, {@code ignoredOptionalDependencies}, {@code time}, and any key pnpm
 * does not know) is held as the JavaScript value read, as the spread in {@code convertToLockfileObject} holds it.
 */
@Getter
@Setter
final class LockfileObject {

    static final String LOCKFILE_VERSION = "9.0";
    static final String LOCKFILE_MAJOR_VERSION = "9";

    private Map<String, ProjectSnapshot> importers = new LinkedHashMap<>();

    private @Nullable Map<String, PackageSnapshot> packages;

    private Map<String, @Nullable Object> rest = new LinkedHashMap<>();

    @Nullable
    Object get(String key) {
        return rest.get(key);
    }

    void set(String key, @Nullable Object value) {
        rest.put(key, value);
    }

    @Nullable
    Object getLockfileVersion() {
        return rest.get("lockfileVersion");
    }

    @Nullable
    Object getSettings() {
        return rest.get("settings");
    }

    @Nullable
    Object getCatalogs() {
        return rest.get("catalogs");
    }

    @Nullable
    Object getOverrides() {
        return rest.get("overrides");
    }

    @Nullable
    Object getPackageExtensionsChecksum() {
        return rest.get("packageExtensionsChecksum");
    }

    @Nullable
    Object getPnpmfileChecksum() {
        return rest.get("pnpmfileChecksum");
    }

    @Nullable
    Object getPatchedDependencies() {
        return rest.get("patchedDependencies");
    }

    @Nullable
    Object getIgnoredOptionalDependencies() {
        return rest.get("ignoredOptionalDependencies");
    }

    @Nullable
    Object getTime() {
        return rest.get("time");
    }
}
