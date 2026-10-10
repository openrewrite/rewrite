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
 * the package manager check, the config dependencies, the pnpmfile hooks and the workspace's projects.
 */
public final class Main {

    /**
     * @pnpm/cli.meta's packageManager.version: the release ported.
     */
    public static final String PNPM_VERSION = "11.2.2";

    // the inputs' root: workspaceDir, which is also rootProjectManifestDir, or the project's directory without one
    static final String WORKSPACE_DIR = "";

    private final PnpmLockRegenerator.Inputs inputs;
    private final EnvInstaller envInstaller;
    private final CreateNewStoreController stores;
    private final EnvLockfile.OnDisk lockFile;

    Main(PnpmLockRegenerator.Inputs inputs, EnvInstaller envInstaller, CreateNewStoreController stores,
         EnvLockfile.OnDisk lockFile) {
        this.inputs = inputs;
        this.envInstaller = envInstaller;
        this.stores = stores;
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
                return InstallRoot.at(workspaceRoot);
            }
            if (!into.equals(lockDir) || inputs.getLock() != null) {
                return InstallRoot.HERE;
            }
            // under lockfileDir pnpm run here would start a lock of every project; run at the root, it writes that one
            PnpmOptions options = getConfig(inputs, WorkspaceRootFinder.findWorkspaceDir(inputs::holds, lockDir,
                    inputs.getEnv()));
            return Js.truthy(options.get("lockfileDir")) && FindPackages.findWorkspaceProjects(manifestDirs(inputs),
                    options.getWorkspacePackagePatterns()).contains(lockDir) ?
                    InstallRoot.stops(workspaceRoot, InstallRoot.Stop.WALK_FAILS) : InstallRoot.HERE;
        } catch (RuntimeException e) {
            return InstallRoot.stops(workspaceRoot, InstallRoot.Stop.WALK_FAILS);
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
        PnpmOptions options = getConfig(inputs, workspaceDir);
        String lockfileDir = options.getLockfileDir(dir);
        boolean listed = FindPackages.findWorkspaceProjects(manifestDirs(inputs),
                options.getWorkspacePackagePatterns()).contains(dir);
        return lockfileDir == null || lockfileDir.equals(dir) || !listed ? dir : WORKSPACE_DIR;
    }

    /**
     * main.ts' findWorkspaceDir from where pnpm runs, then getConfig.
     */
    PnpmOptions config() {
        String workspaceDir = WorkspaceRootFinder.findWorkspaceDir(inputs::holds, dir(), inputs.getEnv());
        if (workspaceDir != null && !workspaceDir.equals(WORKSPACE_DIR)) {
            // the inputs are rooted at the workspace found from the lock up; the environment names another directory
            throw new CannotKnow("the config of the workspace in " + workspaceDir + ", which " +
                                 "NPM_CONFIG_WORKSPACE_DIR names (the inputs are those of the workspace found from " +
                                 "the lock up)");
        }
        return getConfig(inputs, workspaceDir);
    }

    // config.dir: where pnpm runs, the edited manifest's directory, else the lock's
    private String dir() {
        return inputs.getDir() != null ? inputs.getDir() : inputs.getLockDir();
    }

    private static PnpmOptions getConfig(PnpmLockRegenerator.Inputs inputs, @Nullable String workspaceDir) {
        String rootManifestText = inputs.getManifests().get("");
        Map<String, @Nullable Object> rootManifest = rootManifestText == null ? null : rootManifest(rootManifestText);
        requireConfigFiles(inputs);
        String workspaceManifest = inputs.getFiles().get(WorkspaceRootFinder.WORKSPACE_MANIFEST_FILENAME);
        if (workspaceManifest == null && WORKSPACE_DIR.equals(workspaceDir)) {
            // workspace-manifest-reader reads a missing pnpm-workspace.yaml as an empty one
            workspaceManifest = "";
        }
        return PnpmOptions.load(workspaceManifest, inputs.getFiles().get(".npmrc"), rootManifest, inputs.getEnv());
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

    /**
     * main.ts' recursive branch: filterProjectsFromDir's projects of the workspace, each with its manifest as read and
     * installable under engineStrict and nodeVersion; filters that select some of them are cannot-know.
     */
    List<Verification.Project> filterProjectsFromDir(PnpmOptions options) {
        if (options.isExplicitlySet("recursiveInstall") && !options.isTrue("recursiveInstall") ||
            options.isTrue("workspaceRoot") || options.isExplicitlySet("filter") ||
            options.isExplicitlySet("filterProd")) {
            throw new CannotKnow("which projects pnpm installs (recursiveInstall, workspaceRoot, filter or " +
                                 "filterProd select them from the directory it runs in)");
        }
        List<Verification.Project> projects = new ArrayList<>();
        for (String dir : FindPackages.findWorkspaceProjects(manifestDirs(inputs),
                options.getWorkspacePackagePatterns())) {
            projects.add(new Verification.Project(dir, FindPackages.getLockfileImporterId(dir), readManifest(dir)));
        }
        for (Verification.Project project : projects) {
            PackageIsInstallable.projectIsInstallable(project.getId(), project.getManifest(),
                    options.isTrue("engineStrict"), options.getNodeVersion(), null, inputs.getHost());
        }
        return projects;
    }

    // project-manifest-reader over the one manifest findPackages lists in dir
    private Map<String, @Nullable Object> readManifest(String dir) {
        String text = inputs.getManifests().get(dir);
        List<String> others = new ArrayList<>();
        for (String other : PnpmLockRegenerator.OTHER_MANIFEST_NAMES) {
            String path = RootRelativePath.join(dir, other);
            if (inputs.holds(path)) {
                others.add(path);
            }
        }
        if (!others.isEmpty() && (text != null || others.size() > 1)) {
            // findPackages lists every manifest file of a directory, in the order the filesystem lists them
            throw new CannotKnow("the manifest pnpm reads from " + others.get(others.size() - 1) +
                                 " (findPackages lists each manifest of " + (dir.isEmpty() ? "the root" : dir) +
                                 ", in the order the filesystem gives)");
        }
        if (text != null) {
            return Manifests.readProjectManifest(text, RootRelativePath.join(dir, "package.json"));
        }
        String path = others.get(0);
        String other = inputs.getFiles().get(path);
        if (other == null) {
            throw new CannotKnow(path + " (its contents were not read)");
        }
        if (!path.endsWith("/package.yaml") && !"package.yaml".equals(path)) {
            throw new CannotKnow("what pnpm reads from " + path + " (json5's parse is not ported)");
        }
        return Manifests.readPackageYaml(other, path);
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
            new SwitchCliVersion(envInstaller, stores, lockFile, inputs.getLockDir()).switchCliVersion(pm, options,
                    WORKSPACE_DIR);
            return;
        }
        checkPackageManager(pm, underCorepack);
        if (!inputs.getLockDir().equals(WORKSPACE_DIR)) {
            if (SyncEnvLockfile.syncEnvLockfile(pm, () -> null) != null) {
                stores.createStoreControllerUnlessLocked(options);
            }
            return;
        }
        String version = SyncEnvLockfile.syncEnvLockfile(pm, lockFile::readEnvLockfile);
        if (version != null) {
            envInstaller.resolvePackageManagerIntegrities(version, lockFile.readEnvLockfile(), true, lockFile,
                    stores.createStoreController(options), options);
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
     * lockfileDir, else rootProjectManifestDir; the pnpmfiles, its plugins' among them, from lockfileDir or config.dir.
     */
    void installConfigDepsAndLoadHooks(PnpmOptions options, @Nullable String lockfileDir) {
        String lockDir = inputs.getLockDir();
        Object configDependencies = options.get("configDependencies");
        if (Js.truthy(configDependencies)) {
            stores.createStoreController(options);
            if (!(configDependencies instanceof Map)) {
                throw new CannotKnow("what pnpm makes of configDependencies (" + Js.typeOf(configDependencies) +
                                     ", where pnpm reads an object)");
            }
            @SuppressWarnings("unchecked")
            Map<String, @Nullable Object> configDeps = (Map<String, @Nullable Object>) configDependencies;
            String rootDir = lockfileDir == null ? WORKSPACE_DIR : lockfileDir;
            if (lockDir.equals(rootDir)) {
                envInstaller.resolveAndInstallConfigDeps(configDeps, lockFile, options);
            } else {
                // into the env document of rootDir's lock, which the inputs do not hold: what it does not lock resolves
                try {
                    envInstaller.resolveAndInstallConfigDeps(configDeps, new EnvLockfile.OnDisk(null), options);
                } catch (PnpmError | JsError e) {
                    throw new CannotKnow("whether pnpm resolves the config dependencies, which fail here (" +
                                         e.getMessage() + ") unless the env document of " +
                                         (rootDir.isEmpty() ? "the workspace's " : rootDir + "/") +
                                         LockfileReader.WANTED_LOCKFILE + " locks them");
                }
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
        new RequireHooks(inputs).requireHooks(lockfileDir == null ? dir() : lockfileDir,
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
