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
import org.openrewrite.javascript.internal.lock.berry.Configuration;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.marker.NodeResolutionResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.berry.Configuration.DEFAULT_RC_FILENAME;
import static org.openrewrite.javascript.internal.registry.NodeRegistries.hasCredentials;

/**
 * The Yarn Berry regenerator's inputs: the manifests, paths and patch files under the lock's directory, the
 * {@code .yarnrc.yml} of each directory from where Yarn runs up, and the package.json and paths of those above it.
 */
final class YarnBerryInputs {

    private static final String NPM_DEFAULT_REGISTRY = "https://registry.npmjs.org/";
    private static final Pattern MANIFEST_ABOVE = Pattern.compile("(\\.\\./)+package\\.json");

    private YarnBerryInputs() {
    }

    static boolean reads(String fileName) {
        return DEFAULT_RC_FILENAME.equals(fileName) || fileName.endsWith(".patch");
    }

    static InstallRoot installRoot(LockDirectory dir) {
        return YarnBerryLockRegenerator.installRoot(holds(dir), dir.depth());
    }

    /**
     * The host's registries as the home .yarnrc.yml stands for them: the view's, else the marker's global and user
     * npmrc; not its project npmrc (the repository root's), as Yarn reads no .npmrc.
     */
    static NodeRegistries userConfig(ExecutionContext ctx, @Nullable NodeResolutionResult marker, Environment env) {
        return YarnClassicInputs.userConfig(ctx, marker, env);
    }

    /**
     * The host's credentials for every registry the marker's npmrc names, its project npmrc's too, without that
     * file's own credentials, which Yarn never reads.
     */
    static NodeRegistries hostCredentials(ExecutionContext ctx, @Nullable NodeResolutionResult marker,
                                          Environment env) {
        return YarnClassicInputs.hostCredentials(ctx, marker, env);
    }

    /**
     * With the home directory's .yarnrc.yml, which Yarn reads under the project's rc files: {@code user}, the host's
     * registries as the user's own config (null for none), with the host's {@code credentials} for others too.
     */
    static Inputs from(LockDirectory dir, Host host, Instant now, @Nullable NodeRegistries user,
                       @Nullable NodeRegistries credentials) {
        Inputs inputs = project(dir, host, now);
        return user == null ? inputs : inputs.toBuilder()
                .homeFile(DEFAULT_RC_FILENAME, userYarnrc(user, credentials == null ? user : credentials)).build();
    }

    /**
     * {@code user}'s registries as .yarnrc.yml settings, each registry's credentials, {@code credentials}' others too,
     * as its {@code npmRegistries} entry; npm's own default, the host's answer when nothing names one, stays unnamed.
     */
    static String userYarnrc(NodeRegistries user, NodeRegistries credentials) {
        StringBuilder yarnrc = new StringBuilder();
        NodeRegistry registry = user.getDefaultRegistry();
        if (!NPM_DEFAULT_REGISTRY.equals(registry.getUrl()) || hasCredentials(registry)) {
            yarnrc.append("npmRegistryServer: ").append(EcmaScript.quote(registry.getUrl())).append('\n');
        }
        StringBuilder entries = new StringBuilder();
        Set<String> keys = new LinkedHashSet<>();
        if (hasCredentials(registry)) {
            entry(entries, registry, keys);
        }
        List<NodeRegistry> others = new ArrayList<>(user.getByScope().values());
        others.add(credentials.getDefaultRegistry());
        others.addAll(credentials.getByScope().values());
        for (NodeRegistry other : others) {
            if (hasCredentials(other) && !keys.contains(key(other))) {
                entry(entries, other, keys);
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
                credentials(yarnrc, scoped.getValue());
                alwaysAuth(yarnrc, scoped.getValue());
            }
        }
        return yarnrc.toString();
    }

    // npmAlwaysAuth: the entry stands whole for its registry, and the host's credentials go with every request to it
    private static void entry(StringBuilder entries, NodeRegistry registry, Set<String> keys) {
        keys.add(key(registry));
        entries.append("  ").append(EcmaScript.quote(key(registry))).append(":\n");
        credentials(entries, registry);
        entries.append("    npmAlwaysAuth: true\n");
    }

    // npmRegistries' key, as getRegistryConfiguration's lookup without the protocol finds it
    private static String key(NodeRegistry registry) {
        return registry.getUrl().replaceFirst("/\\z", "").replaceFirst("^[a-z]+:", "");
    }

    private static void credentials(StringBuilder yarnrc, NodeRegistry registry) {
        if (registry.getAuthToken() != null) {
            yarnrc.append("    npmAuthToken: ").append(EcmaScript.quote(registry.getAuthToken())).append('\n');
        } else if (registry.getAuthBase64() != null) {
            yarnrc.append("    npmAuthIdent: ").append(EcmaScript.quote(registry.getAuthBase64())).append('\n');
        } else if (registry.getUsername() != null && registry.getPassword() != null) {
            yarnrc.append("    npmAuthIdent: ")
                    .append(EcmaScript.quote(registry.getUsername() + ":" + registry.getPassword())).append('\n');
        }
    }

    // an npmrc's always-auth without credentials is no setting Yarn reads, and npmAlwaysAuth there would be YN0033
    private static void alwaysAuth(StringBuilder yarnrc, NodeRegistry registry) {
        if (registry.isAlwaysAuth() && hasCredentials(registry)) {
            yarnrc.append("    npmAlwaysAuth: true\n");
        }
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
