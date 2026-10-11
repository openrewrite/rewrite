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
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicManifestRules;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Yarn classic's port: the config files yarn reads from where it runs up, the home .npmrc the host's registries stand
 * for, the manifests above, and the paths under the lock's directory and directly above it.
 */
final class YarnClassicInputs implements Port {

    static final YarnClassicInputs PORT = new YarnClassicInputs();

    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");

    private YarnClassicInputs() {
    }

    @Override
    public String lockFile() {
        return "yarn.lock";
    }

    @Override
    public boolean reads(String fileName) {
        return CONFIG_FILES.contains(fileName);
    }

    @Override
    public ManifestRules manifestRules() {
        return YarnClassicManifestRules.RULES;
    }

    @Override
    public InstallRoot installRoot(LockDirectory dir) {
        return YarnClassicLockRegenerator.installRoot(project(dir).build());
    }

    @Override
    public Result regenerate(LockDirectory dir, HostInputs host, NpmRegistryClient client) {
        return new YarnClassicLockRegenerator(from(dir, host), new HostRegistrySource(host.transport(), client))
                .regenerate();
    }

    /**
     * With the home .npmrc yarn reads under the project's: the host's registries as the user's global and user
     * npmrc, with the host's credentials for the registries the project names too.
     */
    static Inputs from(LockDirectory dir, HostInputs host) {
        return project(dir)
                .host(host.getHost())
                .env(host.getEnvironment().process())
                .homeFile(".npmrc", host.userNpmrc(HostInputs.GLOBAL_AND_USER))
                .build();
    }

    // what yarn reads of the project, which is all its walk reads
    private static Inputs.InputsBuilder project(LockDirectory dir) {
        Map<String, String> configFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            String path = file.getKey();
            int slash = path.lastIndexOf('/');
            String name = path.substring(slash + 1);
            String parent = slash < 0 ? "" : path.substring(0, slash);
            boolean above = parent.matches("\\.\\.(/\\.\\.)*");
            if (CONFIG_FILES.contains(name) && (parent.isEmpty() || above || isOnTheWayTo(parent, dir.getRunsIn())) ||
                "package.json".equals(name) && (above || isOnTheWayTo(parent, dir.getRunsIn()))) {
                configFiles.put(path, file.getValue());
            }
        }
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(dir.getManifests())
                .configFiles(configFiles)
                .paths(paths(dir))
                .directory(dir.getPath())
                .cwd(dir.getRunsIn());
    }

    // the paths under the lock's directory and those directly in a directory above it
    private static @Nullable Set<String> paths(LockDirectory dir) {
        Set<String> paths = dir.pathsUnder("");
        Set<String> above = dir.pathsAbove("");
        if (paths != null && above != null) {
            paths.addAll(above);
        }
        return paths;
    }

    private static boolean isOnTheWayTo(String dir, String runsIn) {
        return !dir.startsWith("..") && (runsIn.equals(dir) || runsIn.startsWith(dir + "/"));
    }
}
