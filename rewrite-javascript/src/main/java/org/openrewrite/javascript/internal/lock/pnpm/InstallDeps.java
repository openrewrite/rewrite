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
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.pnpm.ExtendInstallOptions.InstallOptions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Port of @pnpm/installing.commands src/installDeps.ts (pnpm 11.2.2) for {@code pnpm install --lockfile-only}: the
 * store controller, then recursive.ts over the workspace's projects, or install over the project's manifest.
 */
final class InstallDeps {

    private final PnpmLockRegenerator.Inputs inputs;
    private final RegistryMetadata registry;
    private final CreateNewStoreController stores;
    private final Instant now;
    private final Install install;

    InstallDeps(PnpmLockRegenerator.Inputs inputs, RegistryMetadata registry, CreateNewStoreController stores,
                Instant now, Install install) {
        this.inputs = inputs;
        this.registry = registry;
        this.stores = stores;
        this.now = now;
        this.install = install;
    }

    /**
     * {@code installDeps(opts, [])} up to the resolution of the lock: {@code allProjects} and {@code allProjectsGraph}
     * are main.ts' filterProjectsFromDir's, null outside a workspace.
     */
    Install.Result installDeps(PnpmOptions opts, @Nullable List<Verification.Project> allProjects,
                               ProjectsGraph.@Nullable Result allProjectsGraph) {
        if (Install.dryRun(opts) && Js.truthy(opts.get("pnprServer"))) {
            throw new PnpmError("CONFIG_CONFLICT_DRY_RUN_WITH_PNPR_SERVER", "Cannot use --dry-run with a configured " +
                    "pnpr server because the pnpr install path resolves and links through the server");
        }
        NpmResolver store = stores.createInstallStoreController(opts);
        verifyPacquetIdentity(opts, install::readEnvLockfile, inputs.getHost());
        Object runtimeOnFail = opts.get("runtimeOnFail");
        if (allProjects != null && Js.truthy(runtimeOnFail)) {
            for (Verification.Project project : allProjects) {
                PkgManifestUtils.applyRuntimeOnFailOverride(project.getManifest(), runtimeOnFail, opts.getRelease());
            }
        }
        if (allProjects != null && allProjectsGraph != null) {
            checkWorkspaceCycles(opts, allProjectsGraph);
            return new Recursive(inputs, registry, stores, now, install).recursive(allProjects, opts, store);
        }
        Verification.Project project = tryReadProjectManifest(opts);
        if (project != null && Js.truthy(runtimeOnFail)) {
            PkgManifestUtils.applyRuntimeOnFailOverride(project.getManifest(), runtimeOnFail, opts.getRelease());
        }
        return install.install(project, new InstallOptions(opts, opts.getOverrides(), null, false, store,
                setupPolicyHandlers(opts)));
    }

    /**
     * installDeps.ts' pacquet check of 11.5.3 (verifyPacquetIdentity.ts), which a lock-only install makes too
     * ({@link PnpmRelease#verifiesPacquetBeforeInstalling}): where the configDependencies name pacquet and the env
     * document locks it and its binary for the host's platform by integrity, pnpm checks the registry's signatures of
     * both against npm's signing keys, which no input holds; otherwise it warns and uses its own engine. The env
     * document is the lock's directory's.
     */
    @SuppressWarnings("unchecked")
    static void verifyPacquetIdentity(PnpmOptions opts, Supplier<@Nullable EnvLockfile> envLockfiles, Host host) {
        Object configDependencies = opts.get("configDependencies");
        if (!(configDependencies instanceof Map)) {
            return;
        }
        Map<String, @Nullable Object> declared = (Map<String, @Nullable Object>) configDependencies;
        String packageName = declared.get("@pnpm/pacquet") != null ? "@pnpm/pacquet" :
                declared.get("pacquet") != null ? "pacquet" : null;
        if (packageName == null || !opts.getRelease().verifiesPacquetBeforeInstalling()) {
            return;
        }
        EnvLockfile envLockfile = envLockfiles.get();
        if (envLockfile == null) {
            return;
        }
        Object shim = envLockfile.configDependencies().get(packageName);
        if (!(shim instanceof Map)) {
            return;
        }
        String shimKey = packageName + "@" + Js.string(((Map<String, @Nullable Object>) shim).get("version"));
        if (registryIntegrity(envLockfile.packages().get(shimKey)) == null) {
            return;
        }
        if (host.getOs() == null || host.getCpu() == null) {
            throw new CannotKnow("the platform and architecture of the host, whose pacquet binary pnpm checks");
        }
        String platformPkgName = "@pacquet/" + host.getOs() + "-" + host.getCpu();
        Object snapshot = envLockfile.snapshots().get(shimKey);
        Object optionalDependencies = snapshot instanceof Map ?
                ((Map<String, @Nullable Object>) snapshot).get("optionalDependencies") : null;
        Object platformVersion = optionalDependencies instanceof Map ?
                ((Map<String, @Nullable Object>) optionalDependencies).get(platformPkgName) : null;
        if (platformVersion == null) {
            return;
        }
        String platformKey = platformPkgName + "@" + Js.string(platformVersion);
        if (registryIntegrity(envLockfile.packages().get(platformKey)) == null) {
            return;
        }
        throw new CannotKnow("whether the registry's signatures of " + shimKey + " and " + platformKey + " match " +
                             "the integrities the env document locks (pnpm checks them before the install)");
    }

    @SuppressWarnings("unchecked")
    private static @Nullable String registryIntegrity(@Nullable Object packageInfo) {
        Object resolution = packageInfo instanceof Map ?
                ((Map<String, @Nullable Object>) packageInfo).get("resolution") : null;
        Object integrity = resolution instanceof Map ? ((Map<String, @Nullable Object>) resolution).get("integrity") :
                null;
        return integrity instanceof String && !((String) integrity).isEmpty() ? (String) integrity : null;
    }

    // cli.utils' tryReadProjectManifest, which checks packageIsInstallable; null without a package.json
    private Verification.@Nullable Project tryReadProjectManifest(PnpmOptions opts) {
        String dir = install.getLockDir();
        String text = inputs.getManifests().get(dir);
        if (text == null) {
            return null;
        }
        Map<String, @Nullable Object> manifest = Manifests.normalizedForTheWriter(Manifests.readProjectManifest(text,
                RootRelativePath.join(dir, "package.json"), opts.getRelease()), opts.getRelease());
        PackageIsInstallable.projectIsInstallable(".", manifest, opts.isTrue("engineStrict"),
                opts.getNodeVersion(), opts.get("supportedArchitectures"), inputs.getHost(), opts.getRelease());
        return new Verification.Project(dir, ".", manifest);
    }

    // projects that depend on each other in a cycle, an error under disallowWorkspaceCycles
    private static void checkWorkspaceCycles(PnpmOptions opts, ProjectsGraph.Result projectsGraph) {
        if (opts.isTrue("ignoreWorkspaceCycles") || !opts.isTrue("disallowWorkspaceCycles")) {
            return;
        }
        GraphSequencer.Result<String> sequencedGraph = GraphSequencer.sequenceGraph(projectsGraph.getGraph());
        if (!sequencedGraph.isSafe()) {
            List<String> cycles = new ArrayList<>();
            for (List<String> cycle : sequencedGraph.getCycles()) {
                List<String> ids = new ArrayList<>();
                for (String dir : cycle) {
                    ids.add(FindPackages.getLockfileImporterId(dir));
                }
                cycles.add(String.join(", ", ids));
            }
            String cyclicDependenciesInfo = cycles.isEmpty() ? "" : ": " + String.join("; ", cycles);
            throw new PnpmError("DISALLOW_WORKSPACE_CYCLES", "There are cyclic workspace dependencies" +
                                                             cyclicDependenciesInfo);
        }
        if (!projectsGraph.getUnknown().isEmpty()) {
            throw new CannotKnow("whether " + projectsGraph.getUnknown().get(0) + " names a project of the " +
                                 "workspace, which disallowWorkspaceCycles checks for cycles");
        }
    }

    /**
     * policyHandlers.ts' setupPolicyHandlers outside a terminal: the minimumReleaseAge handler, which fails on an
     * immature pick under minimumReleaseAgeStrict; null when it is not active.
     */
    static @Nullable Consumer<List<ResolveDependencies.PolicyViolation>> setupPolicyHandlers(PnpmOptions opts) {
        Double minimumReleaseAge = opts.getNumber("minimumReleaseAge");
        if (minimumReleaseAge == null || minimumReleaseAge == 0) {
            return null;
        }
        return violations -> handleResolutionPolicyViolations(opts, violations);
    }

    private static void handleResolutionPolicyViolations(PnpmOptions opts,
                                                         List<ResolveDependencies.PolicyViolation> violations) {
        if (violations.isEmpty()) {
            return;
        }
        List<ResolveDependencies.PolicyViolation> immature = new ArrayList<>();
        for (ResolveDependencies.PolicyViolation violation : violations) {
            if (NpmResolver.MINIMUM_RELEASE_AGE_VIOLATION_CODE.equals(violation.getCode())) {
                immature.add(violation);
            }
        }
        if (immature.isEmpty()) {
            return;
        }
        immature.sort((a, b) -> (a.getName() + "@" + a.getVersion()).compareTo(b.getName() + "@" + b.getVersion()));
        if (!opts.isTrue("minimumReleaseAgeStrict")) {
            // loose mode: from 11.1.3 policyHandlers' pickManifestUpdates adds each immature pick to
            // pnpm-workspace.yaml's minimumReleaseAgeExclude beside the lock (PnpmRelease#checksTheReleaseAgeAsAPolicy),
            // a file the regeneration does not write
            if (!Boolean.FALSE.equals(opts.get("save")) && opts.getRelease().checksTheReleaseAgeAsAPolicy()) {
                List<String> picks = new ArrayList<>();
                for (ResolveDependencies.PolicyViolation v : immature) {
                    picks.add(v.getName() + "@" + v.getVersion());
                }
                throw new CannotKnow("the pnpm-workspace.yaml pnpm writes beside the lock, whose " +
                                     "minimumReleaseAgeExclude takes " + String.join(", ", picks));
            }
            return;
        }
        StringBuilder list = new StringBuilder();
        for (ResolveDependencies.PolicyViolation v : immature) {
            if (list.length() > 0) {
                list.append('\n');
            }
            list.append("  ").append(v.getName()).append('@').append(v.getVersion()).append(' ').append(v.getReason());
        }
        throw new PnpmError("NO_MATURE_MATCHING_VERSION", immature.size() + " " + (immature.size() == 1 ?
                "version does" : "versions do") + " not meet the minimumReleaseAge constraint:\n" + list);
    }
}
