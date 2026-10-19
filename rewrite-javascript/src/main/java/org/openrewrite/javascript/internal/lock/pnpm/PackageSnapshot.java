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

import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.types {@code PackageSnapshot} (pnpm 11.2.2): a packages entry and its
 * snapshots entry merged.
 */
@Getter
@Setter
final class PackageSnapshot {

    private @Nullable String id;

    private @Nullable String name;

    private @Nullable String version;

    private @Nullable Map<String, @Nullable Object> resolution;

    private @Nullable Object engines;

    private @Nullable Object cpu;

    private @Nullable Object os;

    private @Nullable Object libc;

    private @Nullable Object deprecated;

    private @Nullable Object hasBin;

    private @Nullable Object bundledDependencies;

    private @Nullable Object peerDependencies;

    private @Nullable Object peerDependenciesMeta;

    private @Nullable Map<String, @Nullable String> dependencies;

    private @Nullable Map<String, @Nullable String> optionalDependencies;

    private @Nullable List<String> transitivePeerDependencies;

    private @Nullable Object optional;

    private @Nullable Boolean patched;

    /**
     * {@code {...snapshot}}: a shallow copy.
     */
    PackageSnapshot copy() {
        PackageSnapshot copy = new PackageSnapshot();
        copy.id = id;
        copy.name = name;
        copy.version = version;
        copy.resolution = resolution;
        copy.engines = engines;
        copy.cpu = cpu;
        copy.os = os;
        copy.libc = libc;
        copy.deprecated = deprecated;
        copy.hasBin = hasBin;
        copy.bundledDependencies = bundledDependencies;
        copy.peerDependencies = peerDependencies;
        copy.peerDependenciesMeta = peerDependenciesMeta;
        copy.dependencies = dependencies;
        copy.optionalDependencies = optionalDependencies;
        copy.transitivePeerDependencies = transitivePeerDependencies;
        copy.optional = optional;
        copy.patched = patched;
        return copy;
    }
}
