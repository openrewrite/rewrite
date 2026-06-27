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
package org.openrewrite.golang.internal.modgraph;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Supplies module metadata to the resolver, abstracting WHERE the bytes come
 * from (the local {@code $GOMODCACHE}, a GOPROXY, or a tiered combination) so
 * the resolution algorithm is identical regardless. This is the Java home of
 * what used to be the Go {@code modgraph.ModSource}; the proxy implementation
 * performs HTTP directly through the CLI's configured {@link
 * org.openrewrite.ipc.http.HttpSender}, so no peer-initiated fetch crosses the
 * RPC boundary.
 */
public interface ModSource {

    /** The go.mod bytes for {@code path@version}, or {@code null} if not found. */
    byte @Nullable [] goMod(String path, String version);

    /**
     * The {@code .go} source files of the package at {@code importPath} within
     * {@code modPath@version}, keyed by base filename (tests included), or
     * {@code null} if the package could not be located. This gives the
     * package-import walk the dependency sources it needs without any Go tooling
     * — the proxy implementation downloads and extracts the module zip on demand.
     */
    @Nullable Map<String, byte[]> packageGoFiles(String modPath, String version, String importPath);
}
