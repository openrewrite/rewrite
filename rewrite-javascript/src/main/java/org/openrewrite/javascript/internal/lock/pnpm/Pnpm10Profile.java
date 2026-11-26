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
import org.openrewrite.javascript.semver.NodeSemver;

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
            if (entry.getKey().isEmpty()) {
                String switching = packageManagerSwitch(manifest.get("packageManager"), release);
                if (switching != null) {
                    return switching;
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
                    if (unsupported == null) {
                        unsupported = policy(((Map<?, ?>) workspace).get("minimumReleaseAge"),
                                ((Map<?, ?>) workspace).get("trustPolicy"), release);
                    }
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
                    String[] pair = line.trim().split("=", 2);
                    String key = pair[0].trim();
                    String unsupported = pair.length < 2 ? null : policy(
                            "minimum-release-age".equals(key) ? pair[1].trim() : null,
                            "trust-policy".equals(key) ? pair[1].trim() : null, release);
                    if (unsupported != null) {
                        return unsupported;
                    }
                    if (Arrays.asList("shared-workspace-lockfile", "global", "pnpmfile", "pnpmfiles",
                            "use-node-version", "node-version", "resolution-mode", "inject-workspace-packages",
                            "dedupe-peers").contains(key)) {
                        return reason("advanced .npmrc setting " + key);
                    }
                    // the file npm-conf then loads as the user's or global config is no input
                    // (PnpmRelease#loadsTheUserAndGlobalConfigFromTrustedSettings)
                    if (Arrays.asList("userconfig", "globalconfig", "prefix").contains(key) &&
                        !release.loadsTheUserAndGlobalConfigFromTrustedSettings()) {
                        return reason("a project .npmrc naming " + key + ", which names the file loaded as the " +
                                      ("userconfig".equals(key) ? "user's" : "global") + " config");
                    }
                }
            }
        }
        String pnpmfile = pnpmfile(inputs, release);
        if (pnpmfile != null) {
            return pnpmfile;
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

    // the release age and trust rules pnpm 10.16.0 to 10.32.x change (PnpmRelease#readsAReleaseAgeThePortDoesNot,
    // PnpmRelease#readsATrustPolicyThePortDoesNot)
    private static @Nullable String policy(@Nullable Object minimumReleaseAge, @Nullable Object trustPolicy,
                                           PnpmRelease release) {
        if (minimumReleaseAge != null && Js.toNumber(minimumReleaseAge) != 0 &&
            release.readsAReleaseAgeThePortDoesNot()) {
            return reason("minimumReleaseAge, whose rules changed six times from 10.16.0 to 10.32.x");
        }
        if ("no-downgrade".equals(trustPolicy) && release.readsATrustPolicyThePortDoesNot()) {
            return reason("trustPolicy no-downgrade, whose rules changed five times from 10.21.0 to 10.32.x");
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

    // the default pnpmfile, whose hooks pnpm runs but where ignorePnpmfile is set where the release reads it:
    // .npmrc, or pnpm-workspace.yaml from 10.6.0 (PnpmRelease#readsEverySettingFromTheWorkspaceManifest)
    private static @Nullable String pnpmfile(PnpmLockRegenerator.Inputs inputs, PnpmRelease release) {
        String found = null;
        for (String name : new String[]{".pnpmfile.cjs", ".pnpmfile.mjs"}) {
            if (inputs.getFiles().containsKey(name)) {
                found = name;
                break;
            }
        }
        if (found == null) {
            return null;
        }
        String npmrc = inputs.getFiles().get(".npmrc");
        if (npmrc != null) {
            for (String line : npmrc.split("\n")) {
                String[] pair = line.trim().split("=", 2);
                if (pair.length == 2 && "ignore-pnpmfile".equals(pair[0].trim()) && "true".equals(pair[1].trim())) {
                    return null;
                }
            }
        }
        String workspaceManifest = inputs.getFiles().get("pnpm-workspace.yaml");
        if (workspaceManifest != null && release.readsEverySettingFromTheWorkspaceManifest()) {
            Object workspace = JsYaml.load(workspaceManifest);
            if (workspace instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) workspace).get("ignorePnpmfile"))) {
                return null;
            }
        }
        return reason("the hooks of " + found);
    }

    // switchCliVersion switches to a version that is not clean, as semver.valid cleans it from 10.2.1
    // (pnpm/pnpm#9049), which the engine does not follow; from 10.4.0 the running release installs
    private static @Nullable String packageManagerSwitch(@Nullable Object packageManager, PnpmRelease release) {
        if (!(packageManager instanceof String) || !((String) packageManager).startsWith("pnpm@") ||
            release.keepsTheRunningReleaseForAPackageManagerVersionThatIsNotClean()) {
            return null;
        }
        String version = ((String) packageManager).substring("pnpm@".length()).split("\\+", 2)[0];
        String clean = release.underSemver(semver -> NodeSemver.valid(version,
                NodeSemver.Options.DEFAULT.release(semver)));
        if (clean == null || clean.equals(version) ||
            release.atLeast(10, 2, 1) && clean.equals(release.toString())) {
            return null;
        }
        return reason("packageManager " + packageManager + ", which pnpm " + release + " switches to");
    }

    static String reason(String feature) {
        return "the pnpm 10 native profile for " + feature + " (only registry dependencies, linked workspaces " +
               "and catalogs have been reproduced)";
    }
}
