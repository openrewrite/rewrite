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
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * The Yarn classic regenerator's inputs: the config files of the lock's directory, of each directory above it and of
 * those from where yarn runs up to it, and the package.json of those directories.
 */
final class YarnClassicInputs {

    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");
    private static final String NPM_DEFAULT_REGISTRY = "https://registry.npmjs.org/";
    private static final Pattern USERINFO = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*://)[^/?#@]*@");

    private YarnClassicInputs() {
    }

    static boolean reads(String fileName) {
        return CONFIG_FILES.contains(fileName);
    }

    static InstallRoot installRoot(LockDirectory dir) {
        return YarnClassicLockRegenerator.installRoot(project(dir).build());
    }

    /**
     * The host's registries as yarn's user-level config: the view's, else the marker's global and user npmrc; its
     * project npmrc (the repository root's) yarn reads as a project file, under the home one from a member.
     */
    static NodeRegistries userConfig(ExecutionContext ctx, @Nullable NodeResolutionResult marker, Environment env) {
        return RegistryDiscovery.discover(ctx, marker == null || marker.getNpmrcConfigs() == null ? marker :
                marker.withNpmrcConfigs(marker.getNpmrcConfigs().stream()
                        .filter(npmrc -> npmrc.getScope() != NpmrcScope.Project).collect(toList())), env);
    }

    /**
     * The host's credentials for every registry the marker's npmrc names, its project npmrc's too; that file's own
     * credentials and always-auth stay the project's, read where yarn reads them.
     */
    static NodeRegistries hostCredentials(ExecutionContext ctx, @Nullable NodeResolutionResult marker,
                                          Environment env) {
        return RegistryDiscovery.discover(ctx, marker == null || marker.getNpmrcConfigs() == null ? marker :
                marker.withNpmrcConfigs(marker.getNpmrcConfigs().stream()
                        .map(npmrc -> npmrc.getScope() == NpmrcScope.Project ? registriesOnly(npmrc) : npmrc)
                        .collect(toList())), env);
    }

    private static Npmrc registriesOnly(Npmrc npmrc) {
        Map<String, String> registries = new LinkedHashMap<>();
        for (Map.Entry<String, String> property : npmrc.getProperties().entrySet()) {
            if ("registry".equals(property.getKey()) || property.getKey().endsWith(":registry")) {
                registries.put(property.getKey(), USERINFO.matcher(property.getValue()).replaceFirst("$1"));
            }
        }
        return npmrc.withProperties(registries);
    }

    static Inputs from(LockDirectory dir, Host host) {
        return from(dir, host, null, null);
    }

    /**
     * With the home directory's .npmrc, which yarn reads under the project's: {@code user}, the host's registries as
     * the user's own config (null for none), with the host's {@code credentials} for those the project names too.
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
        for (Map.Entry<String, NodeRegistry> scoped : user.getByScope().entrySet()) {
            npmrc.append(scoped.getKey()).append(":registry=").append(scoped.getValue().getUrl()).append('\n');
            credentials(npmrc, scoped.getValue(), keys);
        }
        credentials(npmrc, credentials.getDefaultRegistry(), keys);
        for (NodeRegistry scoped : credentials.getByScope().values()) {
            credentials(npmrc, scoped, keys);
        }
        return npmrc.toString();
    }

    /**
     * Keyed by the registry's url without its protocol, as getRegistryOption reads them, once per key; always-auth
     * with them, since the host's credentials for a registry go with every request to it, as npm sends them.
     */
    private static void credentials(StringBuilder npmrc, NodeRegistry registry, Set<String> keys) {
        String key = key(registry);
        boolean sent = registry.isAlwaysAuth();
        if (hasCredentials(registry) && keys.add(key)) {
            sent = true;
            if (registry.getAuthToken() != null) {
                npmrc.append(key).append(":_authToken=").append(EcmaScript.quote(registry.getAuthToken()))
                        .append('\n');
            } else if (registry.getAuthBase64() != null) {
                npmrc.append(key).append(":_auth=").append(EcmaScript.quote(registry.getAuthBase64())).append('\n');
            } else if (registry.getUsername() != null && registry.getPassword() != null) {
                npmrc.append(key).append(":username=").append(EcmaScript.quote(registry.getUsername())).append('\n')
                        .append(key).append(":_password=").append(EcmaScript.quote(Base64.getEncoder()
                                .encodeToString(registry.getPassword().getBytes(StandardCharsets.UTF_8))))
                        .append('\n');
            }
        }
        if (sent && keys.add(key + ":always-auth")) {
            npmrc.append(key).append(":always-auth=true\n");
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
