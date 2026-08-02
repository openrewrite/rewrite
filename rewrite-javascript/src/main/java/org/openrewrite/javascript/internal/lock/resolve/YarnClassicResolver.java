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
import org.openrewrite.javascript.internal.lock.YarnLock;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The classic yarn (v1) {@link LockResolver}: resolves the whole closure of an edited manifest set from scratch and
 * serializes the {@code yarn.lock} byte-exact, deferring anything not yet proven reproducible. It parses the importer
 * manifests, adapts the live registry (reusing {@link NpmRegistryAdapter}), runs {@link NpmGraphBuilder} (node-semver
 * dedup is package-manager-neutral), and writes with {@link YarnClassicLockWriter}. A clean closure — flat, with
 * merged selectors and directly-declared forks — including its {@code devDependencies} and {@code optionalDependencies}
 * (yarn v1 records neither scope specially — every entry is an unmarked flat block) is reproduced exactly; a root
 * {@code peerDependencies}/{@code bundleDependencies} surface or a closure-reshaping the builder/writer cannot yet
 * match fails loud, leaving the old lock untouched.
 */
public final class YarnClassicResolver implements LockResolver {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> DEFERRED_SCOPES =
            Arrays.asList("peerDependencies", "bundleDependencies");

    @Override
    public PackageManager packageManager() {
        return PackageManager.YarnClassic;
    }

    @Override
    public String resolve(ResolveRequest request) {
        requireResolvableScopes(request.getImporterManifests());
        Registry registry = new NpmRegistryAdapter(request.getRegistries(), request.getClient());
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(request.getImporterManifests());
        return new YarnClassicLockWriter(mirrorToYarnpkg(request.getExistingLock())).write(graph);
    }

    /**
     * A {@code dependencies}/{@code devDependencies}/{@code optionalDependencies} closure is reproduced; a root
     * {@code peerDependencies} or {@code bundleDependencies} declaration reshapes resolution in ways not yet modeled
     * and defers.
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
                            "importer declares " + scope + " (not yet modeled)");
                }
            }
        }
    }

    /** Mirror the host the existing lock uses; a fresh resolve (no lock) defaults to yarn's yarnpkg mirror. */
    private static boolean mirrorToYarnpkg(@Nullable String existingLock) {
        return existingLock == null || !existingLock.contains(YarnLock.NPM_REGISTRY);
    }
}
