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
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicRelease;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicReleases;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

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
    public boolean writes(String lock) {
        return !YarnBerryInputs.PORT.writes(lock);
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
        // the release from(...) installs as; the regeneration reports one the engine does not reproduce
        YarnClassicRelease release;
        try {
            release = release(dir.packageManagerVersion(PackageManager.YarnClassic));
        } catch (CannotKnow e) {
            return InstallRoot.STOPS_HERE;
        }
        return YarnClassicLockRegenerator.installRoot(project(dir).release(release).build());
    }

    // the release the NodeRuntime marker records, none without one
    private static @Nullable YarnClassicRelease release(@Nullable String version) {
        return version == null ? null : YarnClassicReleases.installedAs(version);
    }

    @Override
    public Result regenerate(LockDirectory dir, HostInputs host, NpmRegistryClient client) {
        Inputs inputs;
        try {
            inputs = from(dir, host);
        } catch (CannotKnow e) {
            return Result.failure(LockOnlyInstall.cannotKnow(e));
        }
        return new YarnClassicLockRegenerator(inputs, new HostRegistrySource(host.transport(), client)).regenerate();
    }

    /**
     * As the Yarn release that built the LST, 1.22.22 where the host recorded none. With the home .npmrc yarn reads
     * under the project's: the host's registries as the user's global and user npmrc, with the host's credentials for
     * the registries the project names too.
     *
     * @throws CannotKnow where the yarn that built the LST is a release the engine does not reproduce
     */
    static Inputs from(LockDirectory dir, HostInputs host) {
        return project(dir)
                .release(release(host.packageManagerVersion(PackageManager.YarnClassic)))
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
        // every package.json the host read, by location from the lock's directory: a file: or link: dependency leads
        // to one outside it too
        Map<String, String> manifests = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            String path = file.getKey();
            if ("package.json".equals(path)) {
                manifests.put("", file.getValue());
            } else if (path.endsWith("/package.json")) {
                manifests.put(path.substring(0, path.length() - "/package.json".length()), file.getValue());
            }
        }
        manifests.putAll(dir.getManifests());
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(manifests)
                .configFiles(configFiles)
                .paths(paths(dir))
                .directory(dir.getPath())
                .cwd(dir.getRunsIn());
    }

    // the paths under the lock's directory, those directly in a directory above it, and each package.json elsewhere
    private static @Nullable Set<String> paths(LockDirectory dir) {
        Set<String> paths = dir.pathsUnder("");
        Set<String> above = dir.pathsAbove("");
        if (paths != null && above != null && dir.getPaths() != null) {
            paths.addAll(above);
            for (String held : dir.getPaths()) {
                if (held.endsWith("/package.json")) {
                    paths.add(held);
                }
            }
        }
        return paths;
    }

    private static boolean isOnTheWayTo(String dir, String runsIn) {
        return !dir.startsWith("..") && (runsIn.equals(dir) || runsIn.startsWith(dir + "/"));
    }
}
