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
package org.openrewrite.javascript.internal.lock.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The npm {@link LockResolver}: resolves the whole closure of an edited manifest set from scratch and serializes
 * the {@code package-lock.json} byte-exact, deferring anything not yet proven reproducible. It parses the importer
 * manifests, adapts the live registry, runs {@link NpmGraphBuilder}, and writes with {@link NpmLockWriter}. The
 * lockfileVersion is taken from the existing lock (defaulting to 3). A clean, hoisted closure — flat, a single
 * directly-declared fork, or one carrying already-satisfied {@code peerDependencies} — including its
 * {@code devDependencies} and {@code optionalDependencies} (marked per npm's dev/optional reachability), is
 * reproduced exactly; a workspace, a root {@code peerDependencies}/{@code bundleDependencies} surface, an
 * unsatisfied peer, or a closure-reshaping the builder/writer cannot yet match fails loud, leaving the old lock
 * untouched.
 */
public final class NpmResolver implements LockResolver {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> DEFERRED_SCOPES =
            Arrays.asList("peerDependencies", "bundleDependencies");

    @Override
    public PackageManager packageManager() {
        return PackageManager.Npm;
    }

    @Override
    public String resolve(ResolveRequest request) {
        requireResolvableScopes(request.getImporterManifests());
        int lockfileVersion = lockfileVersionOf(request.getExistingLock());
        Registry registry = new NpmRegistryAdapter(request.getRegistries(), request.getClient());
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(request.getImporterManifests());
        return new NpmLockWriter().write(graph, lockfileVersion);
    }

    /**
     * A {@code dependencies}/{@code devDependencies}/{@code optionalDependencies} closure is reproduced; a root
     * {@code peerDependencies} or {@code bundleDependencies} declaration reshapes resolution in ways not yet
     * modeled and defers.
     */
    private static void requireResolvableScopes(Map<String, String> importerManifests) {
        for (String manifestJson : importerManifests.values()) {
            JsonNode manifest;
            try {
                manifest = JSON.readTree(manifestJson);
            } catch (Exception e) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null, "could not parse importer manifest");
            }
            for (String scope : DEFERRED_SCOPES) {
                JsonNode node = manifest.get(scope);
                if (node != null && node.isObject() && node.size() > 0) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "importer declares " + scope + " (only prod dependencies are resolved today)");
                }
            }
        }
    }

    private static int lockfileVersionOf(@Nullable String existingLock) {
        if (existingLock == null) {
            return 3;
        }
        try {
            JsonNode root = JSON.readTree(existingLock);
            JsonNode version = root.get("lockfileVersion");
            if (version != null && version.isInt()) {
                int v = version.asInt();
                if (v == 2 || v == 3) {
                    return v;
                }
                throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                        "existing lockfileVersion " + v + " is not supported (need 2 or 3)");
            }
        } catch (EngineFailure e) {
            throw e;
        } catch (Exception ignored) {
            // Unparseable existing lock: fall through to the default.
        }
        return 3;
    }
}
