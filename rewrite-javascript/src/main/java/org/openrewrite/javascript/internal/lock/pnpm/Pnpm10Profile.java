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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.Arrays;
import java.util.Map;
import java.util.Locale;

/**
 * The bounded pnpm 10 profile: registry dependencies, linked workspaces and catalogs. The 10.x modules predate
 * the port's patch format, runtime and config dependency handling, and parts of workspace resolution. Those inputs
 * remain cannot-know until their own upstream paths have been ported and recorded.
 */
final class Pnpm10Profile {
    private Pnpm10Profile() {
    }

    static void requireSupported(PnpmLockRegenerator.Inputs inputs, PnpmRelease release) {
        String unsupported = unsupported(inputs, release);
        if (unsupported != null) {
            throw new CannotKnow(unsupported);
        }
    }

    static @Nullable String unsupported(PnpmLockRegenerator.Inputs inputs, PnpmRelease release) {
        try {
            return classify(inputs, release);
        } catch (PnpmError | JsYaml.YamlException invalid) {
            // Invalid input is left to the upstream reader's own failure, not classified as a supported feature.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable String classify(PnpmLockRegenerator.Inputs inputs, PnpmRelease release) {
        if (release.atLeast(11, 0, 0)) {
            return null;
        }
        if (inputs.getEnv() != null) {
            for (Map.Entry<String, String> env : inputs.getEnv().entrySet()) {
                String key = env.getKey().toLowerCase(Locale.ROOT);
                if ((key.startsWith("npm_config_") || key.startsWith("pnpm_config_")) &&
                    !key.endsWith("userconfig") && !key.endsWith("npmrc_auth_file") &&
                    !(key.equals("pnpm_config_minimum_release_age") && "0".equals(env.getValue()))) {
                    return reason("environment configuration " + env.getKey());
                }
            }
        }
        for (Map.Entry<String, String> entry : inputs.getManifests().entrySet()) {
            Map<String, @Nullable Object> manifest = Manifests.parse(entry.getValue(), entry.getKey());
            for (String field : Arrays.asList("engines", "devEngines")) {
                Object engines = manifest.get(field);
                if (engines instanceof Map && (((Map<?, ?>) engines).containsKey("runtime") ||
                    "devEngines".equals(field) && ((Map<?, ?>) engines).containsKey("packageManager"))) {
                    return reason(field + " runtime or package manager fields");
                }
            }
            Object pnpm = manifest.get("pnpm");
            if (pnpm instanceof Map) {
                if (Js.jsonStringify(pnpm).contains("${")) {
                    return reason("environment placeholders in manifest settings");
                }
                String unsupported = settings((Map<String, @Nullable Object>) pnpm);
                if (unsupported != null) {
                    return unsupported;
                }
            }
            for (String field : Arrays.asList("dependencies", "devDependencies", "optionalDependencies",
                    "peerDependencies")) {
                Object deps = manifest.get(field);
                if (deps instanceof Map) {
                    for (Object spec : ((Map<?, ?>) deps).values()) {
                        if (spec instanceof String && unsupportedSpecifier((String) spec)) {
                            return reason("dependency protocol " + spec);
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, String> file : inputs.getFiles().entrySet()) {
            if (file.getKey().endsWith("pnpm-workspace.yaml")) {
                if (file.getValue().contains("${")) {
                    return reason("environment placeholders in workspace settings");
                }
                Object workspace = JsYaml.load(file.getValue());
                if (workspace instanceof Map) {
                    String unsupported = settings((Map<String, @Nullable Object>) workspace);
                    if (unsupported != null) {
                        return unsupported;
                    }
                    Object packages = ((Map<?, ?>) workspace).get("packages");
                    if (packages instanceof Iterable) {
                        for (Object pattern : (Iterable<?>) packages) {
                            if (pattern instanceof String) {
                                String glob = (String) pattern;
                                for (char c : new char[]{'{', '}', '(', ')', '[', ']'}) {
                                    if (glob.indexOf(c) >= 0) {
                                        return reason("workspace patterns with braces, brackets or extglobs");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (file.getKey().endsWith(".npmrc")) {
                if (file.getValue().contains("${")) {
                    return reason("environment placeholders in .npmrc");
                }
                // Advanced .npmrc settings use npm-conf/nopt in pnpm 10, rather than pnpm 11's schema parser.
                for (String line : file.getValue().split("\n")) {
                    String key = line.trim().split("=", 2)[0].trim();
                    if (Arrays.asList("shared-workspace-lockfile", "global", "pnpmfile", "pnpmfiles",
                            "use-node-version", "node-version", "resolution-mode", "inject-workspace-packages",
                            "dedupe-peers").contains(key)) {
                        return reason("advanced .npmrc setting " + key);
                    }
                }
            }
        }
        String lock = inputs.getLock();
        if (lock != null) {
            if (lock.contains("\n---\n") || lock.startsWith("---\n")) {
                return reason("environment lock documents");
            }
            Object raw = JsYaml.load(lock);
            if (raw instanceof Map) {
                Map<?, ?> object = (Map<?, ?>) raw;
                if (object.containsKey("patchedDependencies") || object.containsKey("configDependencies")) {
                    return reason("patches or config dependencies in the lock");
                }
                Object packages = object.get("packages");
                if (packages instanceof Map) {
                    for (Object pkg : ((Map<?, ?>) packages).values()) {
                        if (pkg instanceof Map) {
                            Object resolution = ((Map<?, ?>) pkg).get("resolution");
                            if (resolution instanceof Map && (((Map<?, ?>) resolution).containsKey("type") ||
                                ((Map<?, ?>) resolution).containsKey("repo") ||
                                ((Map<?, ?>) resolution).containsKey("commit"))) {
                                return reason("non-registry locked packages");
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable String settings(Map<String, @Nullable Object> settings) {
        for (String field : Arrays.asList("patchedDependencies", "configDependencies", "packageConfigs",
                "useNodeVersion", "nodeVersion", "runtimeOnFail", "pnpmfile", "pnpmfiles", "globalPnpmfile",
                "resolutionMode", "virtualStoreOnly")) {
            if (settings.containsKey(field)) {
                return reason("setting " + field);
            }
        }
        for (String field : Arrays.asList("sharedWorkspaceLockfile", "enableModulesDir")) {
            if (Boolean.FALSE.equals(settings.get(field))) {
                return reason("setting " + field + "=false");
            }
        }
        for (String field : Arrays.asList("injectWorkspacePackages", "dedupePeers", "global")) {
            if (Js.truthy(settings.get(field))) {
                return reason("setting " + field);
            }
        }
        return null;
    }

    static boolean unsupportedSpecifier(String specifier) {
        if (specifier.startsWith("workspace:") || specifier.startsWith("catalog:") || specifier.startsWith("link:")) {
            return false;
        }
        try {
            // npm-package-arg also recognizes protocol-less hosted git and filesystem specs. Registry aliases
            // must be parsed rather than rejected by their slash (npm:@scope/package@range).
            return !Spec.resolve("dependency", specifier, "/", Spec.Release.PNPM_2_0_0).isRegistry();
        } catch (IllegalArgumentException invalid) {
            return true;
        }
    }

    static String reason(String feature) {
        return "the pnpm 10 native profile for " + feature + " (only registry dependencies, linked workspaces " +
               "and catalogs have been reproduced)";
    }
}
