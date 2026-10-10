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
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.NodeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Yarn classic regenerator's inputs: the config files of the lock's directory, of each directory above it and of
 * those from where yarn runs up to it, and the package.json of those directories.
 */
final class YarnClassicInputs {

    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");
    private static final String NPM_DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    private YarnClassicInputs() {
    }

    static boolean reads(String fileName) {
        return CONFIG_FILES.contains(fileName);
    }

    static InstallRoot installRoot(LockDirectory dir) {
        return YarnClassicLockRegenerator.installRoot(project(dir).build());
    }

    static Inputs from(LockDirectory dir, Host host) {
        return from(dir, host, null);
    }

    /**
     * With the host's registry configuration as the home directory's .npmrc, which yarn reads under the project's.
     */
    static Inputs from(LockDirectory dir, Host host, @Nullable NodeRegistries registries) {
        Inputs.InputsBuilder inputs = project(dir).host(host);
        if (registries != null) {
            inputs.homeFile(".npmrc", userNpmrc(registries));
        }
        return inputs.build();
    }

    /**
     * The registries and their credentials as .npmrc lines; npm's own default, which the host also reports when
     * nothing names a registry, is left unnamed.
     */
    static String userNpmrc(NodeRegistries registries) {
        StringBuilder npmrc = new StringBuilder();
        NodeRegistry registry = registries.getDefaultRegistry();
        if (!NPM_DEFAULT_REGISTRY.equals(registry.getUrl()) || hasCredentials(registry)) {
            npmrc.append("registry=").append(registry.getUrl()).append('\n');
        }
        credentials(npmrc, registry);
        for (Map.Entry<String, NodeRegistry> scoped : registries.getByScope().entrySet()) {
            npmrc.append(scoped.getKey()).append(":registry=").append(scoped.getValue().getUrl()).append('\n');
            credentials(npmrc, scoped.getValue());
        }
        return npmrc.toString();
    }

    // keyed by the registry's url without its protocol, as yarn's getRegistryOption looks them up
    private static void credentials(StringBuilder npmrc, NodeRegistry registry) {
        String key = registry.getUrl().replaceFirst("^[A-Za-z][A-Za-z0-9+.-]*:", "");
        key = key.endsWith("/") ? key : key + "/";
        if (registry.getAuthToken() != null) {
            npmrc.append(key).append(":_authToken=").append(EcmaScript.quote(registry.getAuthToken())).append('\n');
        } else if (registry.getAuthBase64() != null) {
            npmrc.append(key).append(":_auth=").append(EcmaScript.quote(registry.getAuthBase64())).append('\n');
        } else if (registry.getUsername() != null && registry.getPassword() != null) {
            npmrc.append(key).append(":username=").append(EcmaScript.quote(registry.getUsername())).append('\n')
                    .append(key).append(":_password=").append(EcmaScript.quote(Base64.getEncoder().encodeToString(
                            registry.getPassword().getBytes(StandardCharsets.UTF_8)))).append('\n');
        }
        if (registry.isAlwaysAuth()) {
            npmrc.append(key).append(":always-auth=true\n");
        }
    }

    private static boolean hasCredentials(NodeRegistry registry) {
        return registry.getAuthToken() != null || registry.getAuthBase64() != null || registry.getUsername() != null;
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
        if (dir.getPaths() == null) {
            return null;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String path : dir.getPaths()) {
            int slash = path.lastIndexOf('/');
            if (!path.startsWith("../") || path.substring(0, slash).matches("\\.\\.(/\\.\\.)*")) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static boolean isOnTheWayTo(String dir, String runsIn) {
        return !dir.startsWith("..") && (runsIn.equals(dir) || runsIn.startsWith(dir + "/"));
    }
}
