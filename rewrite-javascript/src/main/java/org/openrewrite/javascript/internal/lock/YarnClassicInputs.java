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
        return from(dir, host, null, null);
    }

    /**
     * With the home directory's .npmrc, which yarn reads under the project's: {@code user}, the host's registries as
     * the user's own config (null for none), holding the credentials {@code credentials} has as well.
     */
    static Inputs from(LockDirectory dir, Host host, @Nullable NodeRegistries user,
                       @Nullable NodeRegistries credentials) {
        Inputs.InputsBuilder inputs = project(dir).host(host);
        if (user != null) {
            inputs.homeFile(".npmrc", userNpmrc(user, credentials == null ? user : credentials));
        }
        return inputs.build();
    }

    /**
     * {@code user}'s registries as .npmrc lines, with their credentials and {@code credentials}' others; npm's own
     * default, the host's answer when nothing names one, stays unnamed.
     */
    static String userNpmrc(NodeRegistries user, NodeRegistries credentials) {
        StringBuilder npmrc = new StringBuilder();
        Set<String> keys = new LinkedHashSet<>();
        NodeRegistry registry = user.getDefaultRegistry();
        if (!NPM_DEFAULT_REGISTRY.equals(registry.getUrl()) || hasCredentials(registry)) {
            npmrc.append("registry=").append(registry.getUrl()).append('\n');
        }
        credentials(npmrc, registry, keys);
        alwaysAuth(npmrc, registry);
        for (Map.Entry<String, NodeRegistry> scoped : user.getByScope().entrySet()) {
            npmrc.append(scoped.getKey()).append(":registry=").append(scoped.getValue().getUrl()).append('\n');
            credentials(npmrc, scoped.getValue(), keys);
            alwaysAuth(npmrc, scoped.getValue());
        }
        credentials(npmrc, credentials.getDefaultRegistry(), keys);
        for (NodeRegistry scoped : credentials.getByScope().values()) {
            credentials(npmrc, scoped, keys);
        }
        return npmrc.toString();
    }

    // keyed by the registry's url without its protocol, as yarn's getRegistryOption looks them up, once per key
    private static void credentials(StringBuilder npmrc, NodeRegistry registry, Set<String> keys) {
        String key = key(registry);
        if (!hasCredentials(registry) || !keys.add(key)) {
            return;
        }
        if (registry.getAuthToken() != null) {
            npmrc.append(key).append(":_authToken=").append(EcmaScript.quote(registry.getAuthToken())).append('\n');
        } else if (registry.getAuthBase64() != null) {
            npmrc.append(key).append(":_auth=").append(EcmaScript.quote(registry.getAuthBase64())).append('\n');
        } else if (registry.getUsername() != null && registry.getPassword() != null) {
            npmrc.append(key).append(":username=").append(EcmaScript.quote(registry.getUsername())).append('\n')
                    .append(key).append(":_password=").append(EcmaScript.quote(Base64.getEncoder().encodeToString(
                            registry.getPassword().getBytes(StandardCharsets.UTF_8)))).append('\n');
        }
    }

    private static void alwaysAuth(StringBuilder npmrc, NodeRegistry registry) {
        if (registry.isAlwaysAuth()) {
            npmrc.append(key(registry)).append(":always-auth=true\n");
        }
    }

    private static String key(NodeRegistry registry) {
        String key = registry.getUrl().replaceFirst("^[A-Za-z][A-Za-z0-9+.-]*:", "");
        return key.endsWith("/") ? key : key + "/";
    }

    private static boolean hasCredentials(NodeRegistry registry) {
        return registry.getAuthToken() != null || registry.getAuthBase64() != null ||
               registry.getUsername() != null && registry.getPassword() != null;
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
