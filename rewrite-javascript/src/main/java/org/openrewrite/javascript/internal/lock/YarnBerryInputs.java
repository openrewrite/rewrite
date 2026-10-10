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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.lock.berry.Configuration;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.NodeRegistries;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.openrewrite.javascript.internal.lock.berry.Configuration.DEFAULT_RC_FILENAME;

/**
 * The Yarn Berry regenerator's inputs: the manifests, paths and patch files under the lock's directory, and the
 * {@code .yarnrc.yml} of each directory from where Yarn runs up.
 */
final class YarnBerryInputs {

    private static final String NPM_DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    private YarnBerryInputs() {
    }

    static boolean reads(String fileName) {
        return DEFAULT_RC_FILENAME.equals(fileName) || fileName.endsWith(".patch");
    }

    static InstallRoot installRoot(LockDirectory dir) {
        return YarnBerryLockRegenerator.installRoot(holds(dir), dir.depth());
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        return from(dir, host, now, null);
    }

    /**
     * With the host's registry configuration as the home directory's .yarnrc.yml, which Yarn reads under the
     * project's rc files.
     */
    static Inputs from(LockDirectory dir, Host host, Instant now, @Nullable NodeRegistries registries) {
        Inputs inputs = project(dir, host, now);
        return registries == null ? inputs :
                inputs.toBuilder().homeFile(DEFAULT_RC_FILENAME, userYarnrc(registries)).build();
    }

    /**
     * The registries as .yarnrc.yml settings ({@code npmRegistryServer}, {@code npmScopes}); npm's own default,
     * which the host also reports when nothing names a registry, is left unnamed.
     */
    static String userYarnrc(NodeRegistries registries) {
        StringBuilder yarnrc = new StringBuilder();
        NodeRegistry registry = registries.getDefaultRegistry();
        if (!NPM_DEFAULT_REGISTRY.equals(registry.getUrl()) || registry.getAuthToken() != null ||
            registry.getAuthBase64() != null || registry.getUsername() != null) {
            yarnrc.append("npmRegistryServer: ").append(EcmaScript.quote(registry.getUrl())).append('\n');
        }
        if (!registries.getByScope().isEmpty()) {
            yarnrc.append("npmScopes:\n");
            for (Map.Entry<String, NodeRegistry> scoped : registries.getByScope().entrySet()) {
                yarnrc.append("  ").append(EcmaScript.quote(scoped.getKey().substring(1))).append(":\n")
                        .append("    npmRegistryServer: ").append(EcmaScript.quote(scoped.getValue().getUrl()))
                        .append('\n');
            }
        }
        return yarnrc.toString();
    }

    private static Inputs project(LockDirectory dir, Host host, Instant now) {
        Map<String, String> rcFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            if (file.getKey().equals(DEFAULT_RC_FILENAME) || file.getKey().endsWith("/" + DEFAULT_RC_FILENAME)) {
                rcFiles.put(file.getKey(), file.getValue());
            }
        }
        Map<String, String> patches = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.filesUnder("").entrySet()) {
            if (file.getKey().endsWith(".patch")) {
                patches.put(file.getKey(), file.getValue());
            }
        }
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(dir.manifestsUnder(""))
                .configFiles(Configuration.findRcFiles(dir.getRunsIn(), rcFiles))
                .runsIn(dir.getRunsIn())
                .files(patches)
                .paths(dir.pathsUnder(""))
                .host(host)
                .now(now)
                .build();
    }

    /**
     * Whether a file is there, by its path from the lock's directory ({@code ../yarn.lock}): read, held, the lock or a
     * manifest.
     */
    static Predicate<String> holds(LockDirectory dir) {
        return path -> dir.holds(path) || "yarn.lock".equals(path) && dir.getLock() != null ||
                       "package.json".equals(path) && dir.getManifests().containsKey("");
    }
}
