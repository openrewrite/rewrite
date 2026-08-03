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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The resolved dependency closure, a plain value model each {@link LockResolver} produces and each package
 * manager's serializer consumes. It captures <em>what</em> was resolved — the workspace importers with their
 * declared and resolved edges, and every distinct {@code name@version} node with its own resolved edges — but
 * not <em>where</em> anything is placed. Layout (npm/bun hoisting, pnpm content-addressing, yarn descriptors) is
 * computed from this graph by each serializer.
 */
@Value
public class ResolutionGraph {

    List<Importer> importers;

    /** Every resolved package instance, keyed by {@link #key(String, String)} ({@code name@version}). */
    Map<String, ResolvedNode> nodes;

    public static String key(String name, String version) {
        return name + "@" + version;
    }

    public @Nullable ResolvedNode node(String name, String version) {
        return nodes.get(key(name, version));
    }

    /**
     * A workspace importer: its directory ({@code ""} for the root), its own name/version (which the lock's
     * importer entry mirrors), the ranges it declares per scope, and the version each declared dependency
     * resolved to.
     */
    @Value
    public static class Importer {
        String dir;

        /** The importer's own {@code name}, if its manifest declares one. */
        @Nullable String name;

        /** The importer's own {@code version}, if its manifest declares one. */
        @Nullable String version;

        /** Declared ranges by scope ({@code dependencies}/{@code devDependencies}/…) then dependency name. */
        Map<String, Map<String, String>> declared;

        /** The version each directly-declared dependency of this importer resolved to. */
        Map<String, String> resolved;
    }
}
