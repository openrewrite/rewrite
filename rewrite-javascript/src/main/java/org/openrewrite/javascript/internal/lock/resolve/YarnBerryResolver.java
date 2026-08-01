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
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.BerryZipChecksum;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The Yarn Berry {@link LockResolver}: resolves the whole closure of an edited manifest set from scratch and
 * serializes the {@code yarn.lock} byte-exact, deferring anything not yet proven reproducible. It parses the
 * importer manifests, adapts the live registry (shared {@link NpmRegistryAdapter}), builds the graph with the
 * shared node-semver {@link NpmGraphBuilder}, reproduces each node's checksum from its tarball
 * ({@code BerryZipChecksum}), and writes with {@link YarnBerryLockWriter}. The {@code __metadata} version and
 * {@code cacheKey} are taken from the existing lock; only the validated {@code 10c0} checksum format resolves —
 * any other cacheKey, a dev/optional/peer surface, a workspace, or a fork fails loud, leaving the old lock alone.
 */
public final class YarnBerryResolver implements LockResolver {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> DEFERRED_SCOPES =
            Arrays.asList("devDependencies", "optionalDependencies", "peerDependencies", "bundleDependencies");

    @Override
    public PackageManager packageManager() {
        return PackageManager.YarnBerry;
    }

    @Override
    public String resolve(ResolveRequest request) {
        requireProdOnly(request.getImporterManifests());
        Metadata meta = metadataOf(request.getExistingLock());
        Registry registry = new NpmRegistryAdapter(request.getRegistries(), request.getClient());
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(request.getImporterManifests());

        YarnBerryLockWriter.Checksums checksums = (name, version, tarballUrl) -> {
            NodeRegistry reg = request.getRegistries().registryFor(name);
            byte[] tarball = request.getClient().getTarball(reg, name, version, tarballUrl);
            return BerryZipChecksum.checksum(tarball, name, meta.cacheKey);
        };
        return new YarnBerryLockWriter().write(graph, meta.cacheKey, meta.version, checksums);
    }

    /** Only a pure {@code dependencies} closure is reproduced today; a dev/optional/peer scope defers. */
    private static void requireProdOnly(Map<String, String> importerManifests) {
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

    /** The existing lock's {@code __metadata} version and cacheKey; only the {@code 10c0} format is reproducible. */
    private static Metadata metadataOf(@Nullable String existingLock) {
        if (existingLock == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null,
                    "yarn berry resolution needs the existing lock's __metadata (version + cacheKey)");
        }
        String cacheKey = null;
        int version = -1;
        Object loaded = new Yaml().load(existingLock);
        if (loaded instanceof Map) {
            Object meta = ((Map<?, ?>) loaded).get("__metadata");
            if (meta instanceof Map) {
                Object c = ((Map<?, ?>) meta).get("cacheKey");
                cacheKey = c == null ? null : String.valueOf(c);
                Object v = ((Map<?, ?>) meta).get("version");
                if (v instanceof Number) {
                    version = ((Number) v).intValue();
                }
            }
        }
        if (!"10c0".equals(cacheKey)) {
            throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, null,
                    "yarn berry cacheKey " + cacheKey + " is not a validated checksum format (only 10c0 so far)");
        }
        if (version < 0) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "yarn berry lock has no __metadata.version");
        }
        return new Metadata(cacheKey, version);
    }

    private static final class Metadata {
        final String cacheKey;
        final int version;

        Metadata(String cacheKey, int version) {
            this.cacheKey = cacheKey;
            this.version = version;
        }
    }
}
