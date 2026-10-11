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

import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.berry.BerryManifestRules;
import org.openrewrite.javascript.internal.lock.berry.Configuration;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.berry.Configuration.DEFAULT_RC_FILENAME;

/**
 * Yarn Berry's port: the rc files Yarn reads from where it runs up, the home .yarnrc.yml the host's registries stand
 * for, the manifests above, the patch files, and the paths under the lock's directory and directly above it.
 */
final class YarnBerryInputs implements Port {

    static final YarnBerryInputs PORT = new YarnBerryInputs();

    private static final Pattern MANIFEST_ABOVE = Pattern.compile("(\\.\\./)+package\\.json");

    private YarnBerryInputs() {
    }

    @Override
    public String lockFile() {
        return "yarn.lock";
    }

    @Override
    public boolean reads(String fileName) {
        return DEFAULT_RC_FILENAME.equals(fileName) || fileName.endsWith(".patch");
    }

    @Override
    public ManifestRules manifestRules() {
        return BerryManifestRules.RULES;
    }

    @Override
    public InstallRoot installRoot(LockDirectory dir) {
        return YarnBerryLockRegenerator.installRoot(holds(dir), dir.depth());
    }

    @Override
    public Result regenerate(LockDirectory dir, HostInputs host, NpmRegistryClient client) {
        return new YarnBerryLockRegenerator(from(dir, host), new HostRegistrySource(host.transport(), client))
                .regenerate();
    }

    /**
     * With the home .yarnrc.yml Yarn reads under every project rc file: the host's registries as the user's global
     * and user npmrc, Yarn reading no .npmrc, with the host's credentials for the registries the project names too.
     */
    static Inputs from(LockDirectory dir, HostInputs host) {
        return project(dir, host).toBuilder()
                .env(host.getEnvironment().process())
                .homeFile(DEFAULT_RC_FILENAME, host.userYarnrc())
                .build();
    }

    private static Inputs project(LockDirectory dir, HostInputs host) {
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
        Map<String, String> configFiles = new LinkedHashMap<>(Configuration.findRcFiles(dir.getRunsIn(), rcFiles));
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            if (MANIFEST_ABOVE.matcher(file.getKey()).matches()) {
                configFiles.put(file.getKey(), file.getValue());
            }
        }
        Set<String> paths = dir.pathsUnder("");
        Set<String> above = dir.pathsAbove("");
        if (paths != null && above != null) {
            paths.addAll(above);
        }
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(dir.manifestsUnder(""))
                .configFiles(configFiles)
                .runsIn(dir.getRunsIn())
                .files(patches)
                .paths(paths)
                .host(host.getHost())
                .now(host.getNow())
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
