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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeRuntime;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.javascript.internal.registry.NodeRegistries.hasCredentials;

/**
 * The host's side of a regeneration, the same for every port: its machine, the time, the environment, its registry
 * configuration, which each port reads as its package manager's user-level config, and the package manager release
 * that built the LST.
 */
@Value
@Builder(toBuilder = true)
public class HostInputs {

    /**
     * The npmrc scopes npm and Yarn classic read as the user's own config; pnpm and Bun read the user's file only.
     */
    public static final Set<NpmrcScope> GLOBAL_AND_USER = EnumSet.of(NpmrcScope.Global, NpmrcScope.User);

    public static final Set<NpmrcScope> USER = EnumSet.of(NpmrcScope.User);

    private static final String ALWAYS_AUTH = "always-auth";

    private static final Pattern USERINFO = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*://)[^/?#@]*@");

    /**
     * The machine the package manager runs on: the execution context's where the host set it, else the one the
     * parser ran on ({@link NodeRuntime}), else unknown.
     */
    @Builder.Default
    Host host = Host.UNKNOWN;

    /**
     * The package manager the parser found ({@link NodeRuntime#getPackageManager()}); null without a marker or one.
     */
    @Getter(AccessLevel.NONE)
    @Nullable
    PackageManager packageManager;

    /**
     * What its {@code --version} answered in the project's root ({@link NodeRuntime#getPackageManagerVersion()}).
     */
    @Getter(AccessLevel.NONE)
    @Nullable
    String packageManagerVersion;

    Instant now;

    @Builder.Default
    HostEnvironment environment = HostEnvironment.UNKNOWN;

    /**
     * The view's registries as given; with none, the npmrc files stand for the host's registries.
     */
    @Singular
    List<NodeRegistry> registries;

    @Singular
    List<NodeRegistryCredentials> credentials;

    /**
     * The marker's npmrc files: the global and user ones, which the package managers read as such, and the project
     * one (the repository root's), whose registries get the host's credentials.
     */
    @Singular("npmrc")
    List<Npmrc> npmrc;

    /**
     * Where the host expands its npmrc files' variables and reads its .netrc.
     */
    @Builder.Default
    Environment system = Environment.SYSTEM;

    /**
     * @param runtime the marker of the machine and package manager release that built the LST, if the parser
     *                recorded one: its Node version stands where the view sets none, and its os, cpu and libc
     *                together where the view sets no platform, so that no platform is one field from each machine
     */
    public static HostInputs of(ExecutionContext ctx, @Nullable NodeResolutionResult marker,
                                @Nullable NodeRuntime runtime, Instant now) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        Host.HostBuilder machine = Host.builder()
                .nodeVersion(view.getNodeVersion())
                .os(view.getOs())
                .cpu(view.getCpu())
                .libc(view.getLibc());
        if (runtime != null) {
            if (view.getNodeVersion() == null) {
                machine.nodeVersion(runtime.getNodeVersion());
            }
            if (view.getOs() == null) {
                machine.os(runtime.getOs()).cpu(runtime.getCpu()).libc(runtime.getLibc());
            }
        }
        return HostInputs.builder()
                .host(machine.build())
                .packageManager(runtime == null ? null : runtime.getPackageManager())
                .packageManagerVersion(runtime == null ? null : runtime.getPackageManagerVersion())
                .now(now)
                .registries(view.getRegistries())
                .credentials(view.getRegistryCredentials())
                .npmrc(marker == null || marker.getNpmrcConfigs() == null ? new ArrayList<>() :
                        marker.getNpmrcConfigs())
                .build();
    }

    /**
     * The release of {@code pm} that built the LST, which the port of {@code pm} installs the project as: what the
     * marker's package manager answered to {@code --version}, when that package manager is {@code pm}, the one the
     * request is for; null otherwise, as without a marker or an answer.
     */
    public @Nullable String packageManagerVersion(PackageManager pm) {
        return pm == packageManager ? packageManagerVersion : null;
    }

    /**
     * Every registry the host knows, with its credentials and transport settings, for the requests a port makes.
     */
    public NodeRegistries transport() {
        return RegistryDiscovery.discover(registries, credentials, npmrc, system);
    }

    /**
     * The host's registries as the user .npmrc of a package manager reading the npmrc {@code scopes}, with the
     * credentials of every registry the npmrc files name under always-auth, as the host sends them with each request.
     */
    public String userNpmrc(Set<NpmrcScope> scopes) {
        NodeRegistries user = discover(scopes, true);
        StringBuilder npmrc = new StringBuilder();
        String alwaysAuth = globalAlwaysAuth(scopes);
        if (alwaysAuth != null) {
            npmrc.append(ALWAYS_AUTH).append('=').append(alwaysAuth).append('\n');
        }
        Set<String> keys = new LinkedHashSet<>();
        NodeRegistry registry = user.getDefaultRegistry();
        if (namesDefault(scopes)) {
            npmrc.append("registry=").append(registry.getUrl()).append('\n');
        }
        npmrcCredentials(npmrc, registry, keys);
        for (Map.Entry<String, NodeRegistry> scoped : user.getByScope().entrySet()) {
            npmrc.append(scoped.getKey()).append(":registry=").append(scoped.getValue().getUrl()).append('\n');
            npmrcCredentials(npmrc, scoped.getValue(), keys);
        }
        NodeRegistries others = projectCredentials();
        for (NodeRegistry other : others.all()) {
            npmrcCredentials(npmrc, other, keys);
        }
        return npmrc.toString();
    }

    /**
     * The host's registries as Yarn Berry's home .yarnrc.yml, as it reads no .npmrc: npmRegistryServer, npmScopes,
     * and each registry's credentials as its npmRegistries entry.
     */
    public String userYarnrc() {
        NodeRegistries user = discover(GLOBAL_AND_USER, false);
        StringBuilder yarnrc = new StringBuilder();
        NodeRegistry registry = user.getDefaultRegistry();
        if (namesDefault(GLOBAL_AND_USER)) {
            yarnrc.append("npmRegistryServer: ").append(EcmaScript.quote(registry.getUrl())).append('\n');
        }
        StringBuilder entries = new StringBuilder();
        Set<String> keys = new LinkedHashSet<>();
        if (hasCredentials(registry)) {
            yarnrcEntry(entries, registry, keys);
        }
        List<NodeRegistry> others = new ArrayList<>(user.getByScope().values());
        others.addAll(projectCredentials().all());
        for (NodeRegistry other : others) {
            if (hasCredentials(other) && !keys.contains(yarnrcKey(other))) {
                yarnrcEntry(entries, other, keys);
            }
        }
        if (entries.length() > 0) {
            yarnrc.append("npmRegistries:\n").append(entries);
        }
        if (!user.getByScope().isEmpty()) {
            yarnrc.append("npmScopes:\n");
            for (Map.Entry<String, NodeRegistry> scoped : user.getByScope().entrySet()) {
                yarnrc.append("  ").append(EcmaScript.quote(scoped.getKey().substring(1))).append(":\n")
                        .append("    npmRegistryServer: ").append(EcmaScript.quote(scoped.getValue().getUrl()))
                        .append('\n');
                yarnrcCredentials(yarnrc, scoped.getValue());
                if (scoped.getValue().isAlwaysAuth() && hasCredentials(scoped.getValue())) {
                    yarnrc.append("    npmAlwaysAuth: true\n");
                }
            }
        }
        return yarnrc.toString();
    }

    // a global always-auth left out, to stand in the rendered file as itself, not as each registry's
    private NodeRegistries discover(Set<NpmrcScope> scopes, boolean globalAlwaysAuthAside) {
        return RegistryDiscovery.discover(registries, credentials, npmrc.stream()
                .filter(file -> scopes.contains(file.getScope()))
                .map(file -> globalAlwaysAuthAside ? withoutGlobalAlwaysAuth(file) : file)
                .collect(toList()), system);
    }

    // the user's file over the global one, as npm layers them
    private @Nullable String globalAlwaysAuth(Set<NpmrcScope> scopes) {
        String value = null;
        for (NpmrcScope scope : Arrays.asList(NpmrcScope.Global, NpmrcScope.User)) {
            for (Npmrc file : npmrc) {
                if (scopes.contains(scope) && file.getScope() == scope &&
                    file.getProperties().containsKey(ALWAYS_AUTH)) {
                    value = file.getProperties().get(ALWAYS_AUTH);
                }
            }
        }
        return value;
    }

    private static Npmrc withoutGlobalAlwaysAuth(Npmrc file) {
        if (!file.getProperties().containsKey(ALWAYS_AUTH)) {
            return file;
        }
        Map<String, String> properties = new LinkedHashMap<>(file.getProperties());
        properties.remove(ALWAYS_AUTH);
        return file.withProperties(properties);
    }

    // the view's list as given, else the npmrc files of those scopes, says whether the host names a default
    private boolean namesDefault(Set<NpmrcScope> scopes) {
        if (!registries.isEmpty()) {
            return registries.stream().anyMatch(registry -> registry.getScope() == null);
        }
        return npmrc.stream().anyMatch(file -> scopes.contains(file.getScope()) &&
                                               file.getProperties().containsKey("registry"));
    }

    // the host's credentials for every registry the npmrc files name, the project's registry lines only, since the
    // package manager reads that file's own credentials and always-auth where it reads it
    private NodeRegistries projectCredentials() {
        List<Npmrc> files = new ArrayList<>();
        for (Npmrc file : npmrc) {
            files.add(file.getScope() == NpmrcScope.Project ? registriesOnly(file) : withoutGlobalAlwaysAuth(file));
        }
        return RegistryDiscovery.discover(registries, credentials, files, system);
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

    // keyed by the registry's url without its protocol, once per key
    private static void npmrcCredentials(StringBuilder npmrc, NodeRegistry registry, Set<String> keys) {
        String key = npmrcKey(registry);
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

    private static String npmrcKey(NodeRegistry registry) {
        String key = registry.getUrl().replaceFirst("^[A-Za-z][A-Za-z0-9+.-]*:", "");
        return key.endsWith("/") ? key : key + "/";
    }

    // npmAlwaysAuth: the entry stands whole for its registry, and the host's credentials go with every request to it
    private static void yarnrcEntry(StringBuilder entries, NodeRegistry registry, Set<String> keys) {
        keys.add(yarnrcKey(registry));
        entries.append("  ").append(EcmaScript.quote(yarnrcKey(registry))).append(":\n");
        yarnrcCredentials(entries, registry);
        entries.append("    npmAlwaysAuth: true\n");
    }

    // npmRegistries' key, as getRegistryConfiguration's lookup without the protocol finds it
    private static String yarnrcKey(NodeRegistry registry) {
        return registry.getUrl().replaceFirst("/\\z", "").replaceFirst("^[a-z]+:", "");
    }

    private static void yarnrcCredentials(StringBuilder yarnrc, NodeRegistry registry) {
        if (registry.getAuthToken() != null) {
            yarnrc.append("    npmAuthToken: ").append(EcmaScript.quote(registry.getAuthToken())).append('\n');
        } else if (registry.getAuthBase64() != null) {
            yarnrc.append("    npmAuthIdent: ").append(EcmaScript.quote(registry.getAuthBase64())).append('\n');
        } else if (registry.getUsername() != null && registry.getPassword() != null) {
            yarnrc.append("    npmAuthIdent: ")
                    .append(EcmaScript.quote(registry.getUsername() + ":" + registry.getPassword())).append('\n');
        }
    }
}
