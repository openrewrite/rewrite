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

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The bun {@link LockResolver}: resolves the whole closure of an edited manifest set from scratch and serializes
 * {@code bun.lock} byte-exact, deferring anything not yet proven reproducible. It parses the importer manifests,
 * adapts the live registry ({@link NpmRegistryAdapter}), runs {@link NpmGraphBuilder} (bun hoists and forks like
 * npm), and writes with {@link BunLockWriter}. A clean, hoisted, prod-only closure — flat or a single
 * directly-declared fork — is reproduced exactly; a workspace, a dev/optional/peer surface, or a
 * closure-reshaping the builder/writer cannot yet match fails loud, leaving the old lock untouched.
 */
public final class BunResolver implements LockResolver {

    /** bun.lock is JSONC (trailing commas); tolerate them when reading the existing lock's versions. */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();
    private static final List<String> DEFERRED_SCOPES =
            Arrays.asList("devDependencies", "optionalDependencies", "peerDependencies", "bundleDependencies");

    @Override
    public PackageManager packageManager() {
        return PackageManager.Bun;
    }

    @Override
    public String resolve(ResolveRequest request) {
        requireProdOnly(request.getImporterManifests());
        requireBun1(request.getExistingLock());
        Registry registry = new NpmRegistryAdapter(request.getRegistries(), request.getClient());
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(request.getImporterManifests());
        return new BunLockWriter().write(graph, 1, 1);
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

    /** The writer targets bun 1.3.x output (lockfileVersion/configVersion 1); a different version defers. */
    private static void requireBun1(@Nullable String existingLock) {
        if (existingLock == null) {
            return;
        }
        try {
            JsonNode root = JSON.readTree(existingLock);
            JsonNode lockfileVersion = root.get("lockfileVersion");
            if (lockfileVersion != null && lockfileVersion.isInt() && lockfileVersion.asInt() != 1) {
                throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                        "bun.lock lockfileVersion " + lockfileVersion.asInt() + " is not supported (need 1)");
            }
            JsonNode configVersion = root.get("configVersion");
            if (configVersion != null && configVersion.isInt() && configVersion.asInt() != 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "bun.lock configVersion " + configVersion.asInt() + " is not supported (need 1)");
            }
        } catch (EngineFailure e) {
            throw e;
        } catch (Exception ignored) {
            // Unparseable existing lock: fall through to the bun 1 default.
        }
    }
}
