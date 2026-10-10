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
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of pnpm/src main.ts and getConfig.ts (pnpm 11.2.2) up to the command: the workspace pnpm finds, its config,
 * the package manager check, the config dependencies and the pnpmfile hooks.
 */
final class Main {

    /**
     * @pnpm/cli.meta's packageManager.version: the release ported.
     */
    static final String PNPM_VERSION = "11.2.2";

    // the inputs' root: workspaceDir, which is also rootProjectManifestDir, or the project's directory without one
    static final String WORKSPACE_DIR = "";

    private final PnpmLockRegenerator.Inputs inputs;
    private final EnvInstaller envInstaller;
    private final EnvLockfile.OnDisk lockFile;

    Main(PnpmLockRegenerator.Inputs inputs, EnvInstaller envInstaller, EnvLockfile.OnDisk lockFile) {
        this.inputs = inputs;
        this.envInstaller = envInstaller;
        this.lockFile = lockFile;
    }

    /**
     * Run in the lock's directory, the workspace root whose lock pnpm installs the lock's project into; when pnpm stops
     * before it picks a lock, the workspace root, whose lock's regeneration reports why.
     */
    static InstallRoot installRoot(PnpmLockRegenerator.Inputs inputs) {
        String lockDir = inputs.getLockDir();
        if (lockDir.isEmpty()) {
            return InstallRoot.HERE;
        }
        int workspaceRoot = lockDir.split("/").length;
        try {
            String into = installsInto(inputs, lockDir);
            if (into.isEmpty()) {
                return new InstallRoot(workspaceRoot, false);
            }
            if (!into.equals(lockDir) || inputs.getLock() != null) {
                return InstallRoot.HERE;
            }
            // under lockfileDir pnpm run here would start a lock of every project; run at the root, it writes that one
            PnpmOptions options = getConfig(inputs);
            return Js.truthy(options.get("lockfileDir")) && FindPackages.findWorkspaceProjects(manifestDirs(inputs),
                    options.getWorkspacePackagePatterns()).contains(lockDir) ? new InstallRoot(workspaceRoot, true) :
                    InstallRoot.HERE;
        } catch (RuntimeException e) {
            return new InstallRoot(workspaceRoot, true);
        }
    }

    /**
     * findWorkspaceDir and getConfig's lockfileDir, run in {@code dir}: the directory of the lock pnpm installs dir's
     * project into.
     */
    static String installsInto(PnpmLockRegenerator.Inputs inputs, String dir) {
        String workspaceDir = WorkspaceRootFinder.findWorkspaceDir(inputs::holds, dir, inputs.getEnv());
        if (workspaceDir == null || dir.isEmpty()) {
            return dir;
        }
        if (!workspaceDir.isEmpty()) {
            return workspaceDir;
        }
        PnpmOptions options = getConfig(inputs);
        String lockfileDir = options.getLockfileDir(dir);
        boolean listed = FindPackages.findWorkspaceProjects(manifestDirs(inputs),
                options.getWorkspacePackagePatterns()).contains(dir);
        return lockfileDir == null || lockfileDir.equals(dir) || !listed ? dir : WORKSPACE_DIR;
    }

    /**
     * main.ts' findWorkspaceDir from the lock's directory, then getConfig.
     */
    PnpmOptions config() {
        String workspaceDir = WorkspaceRootFinder.findWorkspaceDir(inputs::holds, inputs.getLockDir(),
                inputs.getEnv());
        if (workspaceDir != null && !workspaceDir.equals(WORKSPACE_DIR)) {
            // the inputs are the workspace's found from the lock up; the environment names another
            throw new CannotKnow("the workspace in " + workspaceDir + " (NPM_CONFIG_WORKSPACE_DIR), whose files " +
                                 "the inputs do not hold");
        }
        return getConfig(inputs);
    }

    private static PnpmOptions getConfig(PnpmLockRegenerator.Inputs inputs) {
        String rootManifestText = inputs.getManifests().get("");
        Map<String, @Nullable Object> rootManifest = rootManifestText == null ? null : rootManifest(rootManifestText);
        requireConfigFiles(inputs);
        return PnpmOptions.load(inputs.getFiles().get(WorkspaceRootFinder.WORKSPACE_MANIFEST_FILENAME),
                inputs.getFiles().get(".npmrc"), rootManifest, inputs.getEnv());
    }

    // Node reads the package.json pnpm runs beside before pnpm starts, and stops at one that is not JSON; config
    // converts it after the .npmrc's credentials, as every project manifest is converted
    private static Map<String, @Nullable Object> rootManifest(String text) {
        try {
            return Manifests.parse(text, "package.json");
        } catch (PnpmError e) {
            if ("ERR_PNPM_JSON_PARSE".equals(e.getCode())) {
                throw new JsError("ERR_INVALID_PACKAGE_CONFIG", "Invalid package config package.json.");
            }
            throw e;
        }
    }

    // a pnpm-workspace.yaml or .npmrc the project holds that the host did not read, and no stand-in for it
    private static void requireConfigFiles(PnpmLockRegenerator.Inputs inputs) {
        if (inputs.getPaths() == null) {
            return;
        }
        for (String file : new String[]{"pnpm-workspace.yaml", ".npmrc"}) {
            if (inputs.getPaths().contains(file) && !inputs.getFiles().containsKey(file)) {
                throw new CannotKnow(file + " (its contents were not read)");
            }
        }
    }

    // the directories holding a manifest findPackages' patterns can match
    static Set<String> manifestDirs(PnpmLockRegenerator.Inputs inputs) {
        Set<String> dirs = new LinkedHashSet<>(inputs.getManifests().keySet());
        if (inputs.getPaths() != null) {
            for (String path : inputs.getPaths()) {
                for (String other : PnpmLockRegenerator.OTHER_MANIFEST_NAMES) {
                    if (path.equals(other) || path.endsWith("/" + other)) {
                        dirs.add(RootRelativePath.dirname(path));
                    }
                }
            }
        }
        return dirs;
    }

    /**
     * The package manager check: switchCliVersion or checkPackageManager and syncEnvLockfile, which write pnpm's
     * release into the env document of the lock in rootProjectManifestDir.
     */
    void checkWantedPackageManager(PnpmOptions options) {
        PnpmOptions.WantedPackageManager pm = options.getWantedPackageManager();
        if (pm == null || "ignore".equals(pm.getOnFail())) {
            return;
        }
        boolean underCorepack = inputs.getEnv() != null && inputs.getEnv().containsKey("COREPACK_ROOT");
        if ("pnpm".equals(pm.getName()) && "download".equals(pm.getOnFail()) && !underCorepack) {
            new SwitchCliVersion(envInstaller, lockFile, inputs.getLockDir()).switchCliVersion(pm, options,
                    WORKSPACE_DIR);
            return;
        }
        checkPackageManager(pm, underCorepack);
        if (!inputs.getLockDir().equals(WORKSPACE_DIR)) {
            return;
        }
        String version = SyncEnvLockfile.syncEnvLockfile(pm, lockFile::readEnvLockfile);
        if (version != null) {
            envInstaller.resolvePackageManagerIntegrities(version, lockFile.readEnvLockfile(), true, lockFile, options);
        }
    }

    private static void checkPackageManager(PnpmOptions.WantedPackageManager pm, boolean underCorepack) {
        if (!Js.truthy(pm.getName())) {
            return;
        }
        boolean shouldError = "error".equals(pm.getOnFail()) || "download".equals(pm.getOnFail());
        if (!"pnpm".equals(pm.getName())) {
            if (shouldError) {
                throw new PnpmError("OTHER_PM_EXPECTED", "This project is configured to use " +
                                                         Js.string(pm.getName()));
            }
            return;
        }
        if (Js.truthy(pm.getVersion()) &&
            !NodeSemver.satisfies(PNPM_VERSION, Js.string(pm.getVersion()), NodeSemver.Options.INCLUDE_PRERELEASE)) {
            String msg = "This project is configured to use " + Js.string(pm.getVersion()) +
                         " of pnpm. Your current pnpm is v" + PNPM_VERSION;
            if (underCorepack) {
                msg += "\nCorepack invoked pnpm with this version, and pnpm does not switch versions when running " +
                       "under corepack.";
            }
            if (shouldError) {
                String baseHint = "If you want to bypass this version check, you can set the \"pmOnFail\" " +
                                  "configuration to \"warn\" or \"ignore\" (e.g. via --pm-on-fail=ignore). If using " +
                                  "\"devEngines.packageManager\", you can set its \"onFail\" to \"warn\" or \"ignore\"";
                throw new PnpmError("BAD_PM_VERSION", msg, underCorepack ?
                        "Align the \"packageManager\" field in package.json with \"devEngines.packageManager\", " +
                        "or invoke pnpm directly (without corepack) so it can switch versions automatically.\n" +
                        baseHint : baseHint);
            }
        }
    }

    /**
     * getConfig.ts' installConfigDepsAndLoadHooks: the config dependencies into the env document of the lock in
     * lockfileDir, else rootProjectManifestDir; the pnpmfiles, its plugins' among them, from lockfileDir or here.
     */
    void installConfigDepsAndLoadHooks(PnpmOptions options, @Nullable String lockfileDir) {
        String lockDir = inputs.getLockDir();
        Object configDependencies = options.get("configDependencies");
        if (Js.truthy(configDependencies)) {
            if (!(configDependencies instanceof Map)) {
                throw new CannotKnow("what pnpm makes of configDependencies (" + Js.typeOf(configDependencies) +
                                     ", where pnpm reads an object)");
            }
            if (lockDir.equals(lockfileDir == null ? WORKSPACE_DIR : lockfileDir)) {
                @SuppressWarnings("unchecked")
                Map<String, @Nullable Object> configDeps = (Map<String, @Nullable Object>) configDependencies;
                envInstaller.resolveAndInstallConfigDeps(configDeps, lockFile, options);
            }
        }
        if (options.isTrue("ignorePnpmfile")) {
            return;
        }
        if (configDependencies instanceof Map) {
            @SuppressWarnings("unchecked")
            List<String> names = new ArrayList<>(Js.keys((Map<String, @Nullable Object>) configDependencies));
            Collections.sort(names);
            for (String name : names) {
                if (isPluginName(name)) {
                    throw new CannotKnow("what the pnpmfile of the config dependency " + name + " does (pnpm runs " +
                                         "its hooks)");
                }
            }
        }
        Object pnpmfile = options.get("pnpmfile");
        new RequireHooks(inputs).requireHooks(lockfileDir == null ? lockDir : lockfileDir,
                options.get("globalPnpmfile"), pnpmfile == null || pnpmfile == JsYaml.UNDEFINED ?
                        Collections.emptyList() : pnpmfile instanceof List ? (List<?>) pnpmfile :
                        Collections.singletonList(pnpmfile), tryLoadDefaultPnpmfile(options));
    }

    /**
     * getConfig.ts: config.tryLoadDefaultPnpmfile, which recursive.ts' requireHooks reads for each project too.
     */
    static boolean tryLoadDefaultPnpmfile(PnpmOptions options) {
        Object pnpmfile = options.get("pnpmfile");
        return pnpmfile == null || pnpmfile == JsYaml.UNDEFINED;
    }

    private static boolean isPluginName(String configDepName) {
        if (configDepName.startsWith("pnpm-plugin-")) {
            return true;
        }
        if (configDepName.isEmpty() || configDepName.charAt(0) != '@') {
            return false;
        }
        return configDepName.startsWith("@pnpm/plugin-") || configDepName.contains("/pnpm-plugin-");
    }
}
