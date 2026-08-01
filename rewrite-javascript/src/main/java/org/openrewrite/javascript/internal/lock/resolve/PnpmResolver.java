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
 * The pnpm {@link LockResolver}: resolves the whole closure of an edited manifest set from scratch and serializes
 * {@code pnpm-lock.yaml} byte-exact (lockfileVersion 9), deferring anything not yet proven reproducible. It parses
 * the importer manifests, adapts the live registry, runs the shared {@link NpmGraphBuilder} (pnpm and npm share the
 * clean-closure node-semver resolution), and writes with {@link PnpmLockWriter}. A clean prod-only closure — flat
 * or a directly-declared fork (pnpm keeps both content-addressed versions with no nesting) — is reproduced exactly;
 * a workspace, a dev/optional/peer surface, or an existing lock below version 9 fails loud, leaving the old lock
 * untouched.
 */
public final class PnpmResolver implements LockResolver {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> DEFERRED_SCOPES =
            Arrays.asList("devDependencies", "optionalDependencies", "peerDependencies", "bundleDependencies");

    @Override
    public PackageManager packageManager() {
        return PackageManager.Pnpm;
    }

    @Override
    public String resolve(ResolveRequest request) {
        requireProdOnly(request.getImporterManifests());
        requireVersion9(request.getExistingLock());
        Registry registry = new NpmRegistryAdapter(request.getRegistries(), request.getClient());
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(request.getImporterManifests());
        return new PnpmLockWriter().write(graph);
    }

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

    /** Only lockfileVersion 9 is reproduced; an existing v6 (or older) lock defers rather than upgrade its shape. */
    private static void requireVersion9(@Nullable String existingLock) {
        if (existingLock == null) {
            return;
        }
        int nl = existingLock.indexOf('\n');
        String firstLine = (nl < 0 ? existingLock : existingLock.substring(0, nl)).trim();
        if (!firstLine.startsWith("lockfileVersion:")) {
            return;
        }
        String raw = firstLine.substring("lockfileVersion:".length()).trim().replace("'", "").replace("\"", "");
        int dot = raw.indexOf('.');
        String major = dot >= 0 ? raw.substring(0, dot) : raw;
        try {
            if (Integer.parseInt(major.trim()) != 9) {
                throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                        "existing pnpm lockfileVersion " + raw + " is not supported (need 9)");
            }
        } catch (NumberFormatException ignored) {
            // Unparseable version: fall through and let the writer produce a fresh v9 lock.
        }
    }
}
