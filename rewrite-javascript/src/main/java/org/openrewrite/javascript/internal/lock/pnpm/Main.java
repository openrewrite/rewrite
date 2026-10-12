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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.SpecUtils;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Port of pnpm/src main.ts and getConfig.ts (pnpm 11.2.2) up to the command: the workspace pnpm finds, its config,
 * the package manager check, which names the release that installs, the config dependencies, the pnpmfile hooks and
 * the workspace's projects.
 */
public final class Main {

    /**
     * @pnpm/cli.meta's packageManager.version: the release ported.
     */
    public static final String PNPM_VERSION = "11.2.2";

    /**
     * Whether {@code version}, a release or a range, names only releases the engine reproduces
     * ({@link PnpmReleases#PROVEN}).
     */
    public static boolean runsAsPorted(String version) {
        return PnpmReleases.isProven(version);
    }

    /**
     * Whether the release that installs is one of {@code releases}, the range of a behavior a ported module took on
     * after {@link #PNPM_VERSION}: {@code pnpmVersion} is that release, or a range where the inputs do not say which
     * release of it installs; null when some of those are of {@code releases} and some are not.
     */
    static @Nullable Boolean releaseIn(String pnpmVersion, String releases) {
        String exact = NodeSemver.valid(pnpmVersion, NodeSemver.Options.DEFAULT);
        if (exact != null) {
            return NodeSemver.satisfies(exact, releases, NodeSemver.Options.DEFAULT);
        }
        if (NodeSemver.subset(pnpmVersion, releases, NodeSemver.Options.DEFAULT)) {
            return true;
        }
        return NodeSemver.intersects(pnpmVersion, releases, NodeSemver.Options.DEFAULT) ? null : false;
    }

    // the inputs' root: workspaceDir, which is also rootProjectManifestDir, or the project's directory without one
    static final String WORKSPACE_DIR = "";

    private final PnpmLockRegenerator.Inputs inputs;
    private final EnvInstaller envInstaller;
    private final CreateNewStoreController stores;
    private final EnvLockfile.OnDisk lockFile;
    private final Map<String, EnvLockfile.OnDisk> otherLockFiles = new HashMap<>();

    /**
     * The release that runs, the one that built the LST; null where the host recorded none, when 11.2.2 runs and the
     * engine follows it to the release it hands the install to.
     */
    private final @Nullable PnpmRelease runs;

    // the package manager check's store controller failure, which pnpm ends with unless a lock it reads locks the
    // release; the inputs do not say whether it does
    private @Nullable PnpmError unlessLocked;

    /**
     * filterProjectsFromDir's result for a workspace install: every project, and the graph of their dependencies.
     */
    @Value
    static class FilterResults {
        List<Verification.Project> allProjects;
        ProjectsGraph.Result allProjectsGraph;
    }

    Main(PnpmLockRegenerator.Inputs inputs, EnvInstaller envInstaller, CreateNewStoreController stores,
         EnvLockfile.OnDisk lockFile) {
        this.inputs = inputs;
        this.envInstaller = envInstaller;
        this.stores = stores;
        this.lockFile = lockFile;
        this.runs = inputs.getRelease();
    }

    /**
     * The release whose main.ts runs: the one that built the LST, else 11.2.2.
     */
    PnpmRelease running() {
        return runs == null ? PnpmRelease.PORTED : runs;
    }

    /**
     * The release that installs after the package manager check returned {@code installs}: the one that runs, which
     * hands the install to no other where the engine follows it ({@link SwitchCliVersion}); without the release that
     * built the LST, the one 11.2.2 hands the install to, or the range of them the inputs leave it at.
     *
     * @throws CannotKnow where that is a release the engine does not reproduce
     */
    PnpmRelease installs(String installs) {
        return runs != null ? runs : PnpmReleases.installs(installs);
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
     * project into. The user's .npmrc gives registries, credentials and network settings only, none of which it reads.
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
     * main.ts' findWorkspaceDir from where pnpm runs, then getConfig of the release that runs, whose install options
     * the port reads.
     */
    PnpmOptions config() {
        return config(running());
    }

    /**
     * main.ts' findWorkspaceDir and getConfig as {@code release} runs them: the release that runs, or the one it hands
     * the install to, which reads the same files into its own config.
     */
    PnpmOptions config(PnpmRelease release) {
        String workspaceDir = WorkspaceRootFinder.findWorkspaceDir(inputs::holds, dir(), inputs.getEnv());
        if (workspaceDir != null && !workspaceDir.equals(WORKSPACE_DIR)) {
            // the inputs are rooted at the workspace found from the lock up; the environment names another directory
            throw new CannotKnow("the config of the workspace in " + workspaceDir + ", which " +
                                 "NPM_CONFIG_WORKSPACE_DIR names (the inputs are those of the workspace found from " +
                                 "the lock up)");
        }
        PnpmOptions options = getConfig(inputs, workspaceDir, release);
        requireSettings(options);
        return options;
    }

    // getConfig copies every camelCase key of pnpm-workspace.yaml into the config the install reads as its options;
    // these change the lock or the install's outcome and are not settings the port reads
    private static void requireSettings(PnpmOptions options) {
        Map<String, Boolean> asIfUnset = new LinkedHashMap<>(AS_IF_UNSET);
        if (options.getWorkspacePackagePatterns() != null) {
            // recursive.ts: true when every project of the workspace is installed
            asIfUnset.put("pruneLockfileImporters", true);
        }
        for (String option : UNPORTED_OPTIONS) {
            if (options.isExplicitlySet(option) && !(asIfUnset.containsKey(option) &&
                                                     asIfUnset.get(option).equals(options.get(option)))) {
                throw new CannotKnow("what pnpm does with " + option + " (" +
                                     Js.jsonStringify(options.get(option)) + "), an option of the install");
            }
        }
        Object only = options.get("only");
        boolean production = "prod".equals(only) || "production".equals(only) ||
                             !Js.truthy(only) && options.isTrue("production");
        boolean dev = !production && ("dev".equals(only) || "development".equals(only) || options.isTrue("dev"));
        if (production || dev || Boolean.FALSE.equals(options.get("optional"))) {
            throw new CannotKnow("the lock pnpm writes without its " + (production ? "devDependencies" :
                    dev ? "dependencies" : "optionalDependencies") + " (only, production, dev or optional)");
        }
        for (String networkOnly : new String[]{"offline", "preferOffline"}) {
            if (options.isTrue(networkOnly)) {
                throw new CannotKnow("the metadata pnpm's cache holds (" + networkOnly + ")");
            }
        }
    }

    private static final List<String> UNPORTED_OPTIONS = Arrays.asList("dedupe", "dir", "explicitlySetKeys",
            "finders", "fixLockfile", "forceFullResolution", "frozenLockfile", "frozenLockfileIfExists", "hooks",
            "ignoredPackages", "ignorePackageManifest", "latest", "lockfileCheck", "packageManager",
            "packageVulnerabilityAudit", "pruneDirectDependencies", "pruneLockfileImporters", "resolutionOnly",
            "rootProjectManifest", "rootProjectManifestDir", "saveLockfile", "update", "updateMatching",
            "updatePackageManifest", "updateToLatest", "wantedPackageManager", "workspace", "workspaceDir",
            "workspacePackagePatterns");

    // the value of an option above that a lock-only install treats as the option unset (extendInstallOptions'
    // defaults, install.ts' frozenLockfileIfExists under lockfileOnly, the commands' truthiness checks)
    private static final Map<String, Boolean> AS_IF_UNSET = new LinkedHashMap<>();

    static {
        for (String option : Arrays.asList("dedupe", "fixLockfile", "forceFullResolution", "frozenLockfile",
                "frozenLockfileIfExists", "ignorePackageManifest", "latest", "pruneDirectDependencies",
                "resolutionOnly", "update", "updatePackageManifest", "updateToLatest", "workspace")) {
            AS_IF_UNSET.put(option, false);
        }
        AS_IF_UNSET.put("saveLockfile", true);
    }

    // config.dir: where pnpm runs, the edited manifest's directory, else the lock's
    private String dir() {
        return inputs.getDir() != null ? inputs.getDir() : inputs.getLockDir();
    }

    private static PnpmOptions getConfig(PnpmLockRegenerator.Inputs inputs, @Nullable String workspaceDir) {
        return getConfig(inputs, workspaceDir, inputs.getRelease() == null ? PnpmRelease.PORTED : inputs.getRelease());
    }

    private static PnpmOptions getConfig(PnpmLockRegenerator.Inputs inputs, @Nullable String workspaceDir,
                                         PnpmRelease release) {
        String rootManifestText = inputs.getManifests().get("");
        Map<String, @Nullable Object> rootManifest = rootManifestText == null ? null : rootManifest(rootManifestText);
        requireConfigFiles(inputs);
        String workspaceManifest = inputs.getFiles().get(WorkspaceRootFinder.WORKSPACE_MANIFEST_FILENAME);
        if (workspaceManifest == null && WORKSPACE_DIR.equals(workspaceDir)) {
            // workspace-manifest-reader reads a missing pnpm-workspace.yaml as an empty one
            workspaceManifest = "";
        }
        return PnpmOptions.load(workspaceManifest, inputs.getFiles().get(".npmrc"), inputs.getUserNpmrc(),
                rootManifest, inputs.getEnv(), release);
    }

    // Node reads the package.json pnpm runs beside before pnpm starts, and stops at one that is not JSON; config
    // converts it after the .npmrc's credentials, as every project manifest is converted
    private static Map<String, @Nullable Object> rootManifest(String text) {
        try {
            return Manifests.parse(text, "package.json");
        } catch (PnpmError e) {
            if ("ERR_PNPM_JSON_PARSE".equals(e.getCode())) {
                throw new JsError(JsError.ERROR, "Invalid package config package.json.", "ERR_INVALID_PACKAGE_CONFIG");
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
            CannotKnow.requireRead(inputs.getFiles().get(file), inputs.getPaths(), file);
        }
    }

    /**
     * main.ts' recursive branch: filterProjectsFromDir's projects of the workspace, each with its manifest as read and
     * installable under engineStrict and nodeVersion, and their graph; filters that select some are cannot-know.
     */
    FilterResults filterProjectsFromDir(PnpmOptions options) {
        if (options.isExplicitlySet("recursiveInstall") && !options.isTrue("recursiveInstall") ||
            options.isTrue("workspaceRoot") || options.isExplicitlySet("filter") ||
            options.isExplicitlySet("filterProd")) {
            throw new CannotKnow("which projects pnpm installs (recursiveInstall, workspaceRoot, filter or " +
                                 "filterProd select them from the directory it runs in)");
        }
        List<Verification.Project> projects = new ArrayList<>();
        for (String dir : FindPackages.findWorkspaceProjects(manifestDirs(inputs),
                options.getWorkspacePackagePatterns())) {
            projects.add(new Verification.Project(dir, FindPackages.getLockfileImporterId(dir), readManifest(dir,
                    options.getRelease())));
        }
        for (Verification.Project project : projects) {
            PackageIsInstallable.projectIsInstallable(project.getId(), project.getManifest(),
                    options.isTrue("engineStrict"), options.getNodeVersion(), null, inputs.getHost(),
                    options.getRelease());
        }
        return new FilterResults(projects, ProjectsGraph.createProjectsGraph(projects, false,
                Js.truthy(options.get("linkWorkspacePackages")), options.getRelease()));
    }

    // project-manifest-reader over the one manifest findPackages lists in dir, which release, the one that installs,
    // converts
    private Map<String, @Nullable Object> readManifest(String dir, PnpmRelease release) {
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
            return Manifests.normalizedForTheWriter(Manifests.readProjectManifest(text,
                    RootRelativePath.join(dir, "package.json"), release), running());
        }
        if (others.isEmpty()) {
            throw CannotKnow.unread(RootRelativePath.join(dir, "package.json"));
        }
        String path = others.get(0);
        String other = inputs.getFiles().get(path);
        if (other == null) {
            throw CannotKnow.unread(path);
        }
        if (path.endsWith("/package.yaml") || "package.yaml".equals(path)) {
            return Manifests.normalizedForTheWriter(Manifests.readPackageYaml(other, path, release), running());
        }
        return Manifests.normalizedForTheWriter(Manifests.readJson5(other, path, release), running());
    }

    // the directories holding a manifest findPackages' patterns can match: those the host read, and those it holds
    // a package.json or another manifest in without having read it
    static Set<String> manifestDirs(PnpmLockRegenerator.Inputs inputs) {
        Set<String> dirs = new LinkedHashSet<>(inputs.getManifests().keySet());
        if (inputs.getPaths() != null) {
            for (String path : inputs.getPaths()) {
                if ("package.json".equals(path) || path.endsWith("/package.json")) {
                    dirs.add(RootRelativePath.dirname(path));
                }
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
     * release into the env document of the lock in rootProjectManifestDir. Returns the release that installs, the
     * {@code packageManager.version} of the config its install reads ({@link PnpmOptions#getPackageManagerVersion()}):
     * the one pnpm hands the install to, else this one. Where the inputs do not say which release that is (the one
     * the root's lock pins of a range, the one corepack starts when the host does not say whether it runs pnpm), a
     * range of them.
     */
    String checkWantedPackageManager(PnpmOptions options) {
        PnpmOptions.WantedPackageManager pm = options.getWantedPackageManager();
        // the release that built the LST answered --version as corepack started it, where corepack did
        String corepackStarts = inputs.getEnv() == null && runs == null ?
                startedByCorepack(pm, this::corepackSpec) : null;
        String installs = installsAfterCheck(pm, options);
        return corepackStarts == null || corepackStarts.equals(installs) ? installs :
                installs + " || " + corepackStarts;
    }

    // main.ts' package manager block where pnpm is run directly, or under corepack where the environment says so
    private String installsAfterCheck(PnpmOptions.@Nullable WantedPackageManager pm, PnpmOptions options) {
        String current = running().version();
        if (pm == null || "ignore".equals(pm.getOnFail())) {
            return current;
        }
        boolean underCorepack = inputs.getEnv() != null && inputs.getEnv().containsKey("COREPACK_ROOT");
        EnvLockfile.OnDisk rootLockFile = lockFileOf(WORKSPACE_DIR);
        if ("pnpm".equals(pm.getName()) && "download".equals(pm.getOnFail()) && !underCorepack) {
            SwitchCliVersion.Switched switched = new SwitchCliVersion(envInstaller, stores, runs)
                    .switchCliVersion(pm, options, rootLockFile);
            unlessLocked = switched.getUnlessLocked();
            return switched.getRelease();
        }
        checkPackageManager(pm, underCorepack, current, running());
        if (rootLockFile == null) {
            if (SyncEnvLockfile.syncEnvLockfile(pm, () -> null, running()) != null) {
                unlessLocked = stores.storeControllerFailure(options.packageManagerBootstrap(running()));
            }
            return current;
        }
        String version = SyncEnvLockfile.syncEnvLockfile(pm, rootLockFile::readEnvLockfile, running());
        if (version != null) {
            PnpmOptions bootstrap = options.packageManagerBootstrap(running());
            envInstaller.resolvePackageManagerIntegrities(version, rootLockFile.readEnvLockfile(), true, rootLockFile,
                    stores.createStoreController(bootstrap), bootstrap);
        }
        return current;
    }

    /**
     * What pnpm ends with when {@code e} comes before installDeps' store controller: e, or the failure of a store
     * controller the package manager check created unless a lock the inputs do not hold records the release.
     */
    RuntimeException beforeNextStoreController(RuntimeException e) {
        PnpmError failed = unlessLocked;
        if (failed == null || e instanceof PnpmError && failed.getCode().equals(((PnpmError) e).getCode()) &&
                              String.valueOf(failed.getMessage()).equals(e.getMessage())) {
            return e;
        }
        String other = e instanceof CannotKnow ? ((CannotKnow) e).getSubject() : "with " +
                (e instanceof PnpmError ? "[" + ((PnpmError) e).getCode() + "] " : "") + e.getMessage();
        return new CannotKnow("whether pnpm fails with [" + failed.getCode() + "] " + failed.getMessage() +
                              " (unless the env document of the workspace's " + LockfileReader.WANTED_LOCKFILE +
                              " records the release) or else " + other);
    }

    // the lock file in dir as the run finds it: this lock, another the inputs hold, or none where the host lists the
    // project's files; null when they do not say what it holds
    private EnvLockfile.@Nullable OnDisk lockFileOf(String dir) {
        if (dir.equals(inputs.getLockDir())) {
            return lockFile;
        }
        EnvLockfile.OnDisk other = otherLockFiles.get(dir);
        if (other != null) {
            return other;
        }
        String path = RootRelativePath.join(dir, LockfileReader.WANTED_LOCKFILE);
        String text = inputs.getFiles().get(path);
        if (text == null && (inputs.getPaths() == null || inputs.getPaths().contains(path))) {
            return null;
        }
        other = new EnvLockfile.OnDisk(text, running());
        otherLockFiles.put(dir, other);
        return other;
    }

    /**
     * The release pnpm installs with: the one it switches to or fails without, else this one; CannotKnow where corepack
     * would start one not {@link #runsAsPorted} or refuse the spec, as the host does not say whether it starts pnpm.
     */
    static String release(PnpmOptions.@Nullable WantedPackageManager wanted,
                                 Supplier<SpecUtils.@Nullable Descriptor> corepack) {
        startedByCorepack(wanted, corepack);
        return runDirectly(wanted);
    }

    private static boolean checks(PnpmOptions.@Nullable WantedPackageManager wanted) {
        return wanted != null && ("download".equals(wanted.getOnFail()) || "error".equals(wanted.getOnFail()));
    }

    // the release pnpm run directly installs with: the one the project names where pnpm switches to it or fails
    // without it, else this one
    private static String runDirectly(PnpmOptions.@Nullable WantedPackageManager wanted) {
        return checks(wanted) && "pnpm".equals(wanted.getName()) && wanted.getVersion() instanceof String ?
                (String) wanted.getVersion() : PNPM_VERSION;
    }

    /**
     * The release corepack starts, a version or a range, where it is another than the one pnpm run directly installs
     * with and both {@link #runsAsPorted}; null where it is the same one, where corepack finds no spec, and where
     * pnpm fails for another package manager either way. CannotKnow where corepack would start a release that does
     * not run as ported, or refuse the spec.
     */
    private static @Nullable String startedByCorepack(PnpmOptions.@Nullable WantedPackageManager wanted,
                                                      Supplier<SpecUtils.@Nullable Descriptor> corepack) {
        if (checks(wanted) && !"pnpm".equals(wanted.getName())) {
            return null;
        }
        String direct = runDirectly(wanted);
        String starts;
        try {
            SpecUtils.Descriptor spec = corepack.get();
            if (spec == null) {
                return null;
            }
            String version = SpecUtils.installVersion(spec);
            if (version != null && version.equals(NodeSemver.valid(direct, NodeSemver.Options.DEFAULT))) {
                return null;
            }
            String started = version == null ? spec.getRange() : version;
            if (runsAsPorted(started) && runsAsPorted(direct)) {
                return started;
            }
            starts = version == null ? "starts the release at " + spec.getRange() : "starts pnpm " + version;
        } catch (Rejected e) {
            starts = "refuses to start pnpm (" + e.getMessage() + ")";
        }
        throw new CannotKnow("the pnpm release that installs the project: corepack " + starts + " and pnpm run " +
                             "directly installs with " + direct + "; the host does not say which runs");
    }

    // corepack's findProjectSpec from where pnpm runs, up through the directories above the root the host holds
    private SpecUtils.@Nullable Descriptor corepackSpec() {
        int levels = 0;
        Set<String> above = new LinkedHashSet<>(inputs.getFilesAbove().keySet());
        if (inputs.getPathsAbove() != null) {
            above.addAll(inputs.getPathsAbove());
        }
        for (String path : above) {
            levels = Math.max(levels, path.split("/").length - 1);
        }
        return SpecUtils.findProjectSpec(dir(), levels, dir -> {
            if (!dir.startsWith("..")) {
                return inputs.getManifests().get(dir);
            }
            String file = dir + "/package.json";
            return CannotKnow.requireRead(inputs.getFilesAbove().get(file), inputs.getPathsAbove(), file);
        }, path -> path.startsWith("..") ? inputs.getFilesAbove().containsKey(path) ||
                                           inputs.getPathsAbove() != null && inputs.getPathsAbove().contains(path) :
                inputs.holds(path), "pnpm", HostEnvironment.UNKNOWN);
    }

    private static void checkPackageManager(PnpmOptions.WantedPackageManager pm, boolean underCorepack,
                                            String current, PnpmRelease running) {
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
            !running.underSemver(semver -> NodeSemver.satisfies(current, Js.string(pm.getVersion()),
                    NodeSemver.Options.INCLUDE_PRERELEASE.release(semver)))) {
            String msg = "This project is configured to use " + Js.string(pm.getVersion()) +
                         " of pnpm. Your current pnpm is v" + current;
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

    private static final Map<String, String> RUNTIME_DISPLAY_NAMES = new LinkedHashMap<>();

    static {
        RUNTIME_DISPLAY_NAMES.put("node", "Node.js");
        RUNTIME_DISPLAY_NAMES.put("deno", "Deno");
        RUNTIME_DISPLAY_NAMES.put("bun", "Bun");
    }

    private static final String RUNTIME_ON_FAIL_HINT = "If you want to bypass this version check, set " +
                                                       "\"runtimeOnFail\" to \"warn\" or \"ignore\" (e.g. via " +
                                                       "--runtime-on-fail=ignore), or set \"devEngines.runtime.onFail\"/" +
                                                       "\"engines.runtime.onFail\" to \"warn\" or \"ignore\"";

    /**
     * main.ts' runtime check after the package manager check, which {@code options}' release makes from pnpm 11.4.0
     * on ({@link PnpmRelease#checksTheRuntimeVersion}): getWantedRuntimes of the root manifest, each runtime under the
     * runtimeOnFail setting getConfig applies, and checkRuntime of each. Only a runtime whose onFail is "error" ends
     * the install; any other warns. The host's Node.js is the one the host reports, satisfied where it reports none,
     * as every engine check takes it; whether the host runs Deno or Bun, and which, the inputs do not say.
     */
    @SuppressWarnings("unchecked")
    void checkRuntimes(PnpmOptions options) {
        String text = inputs.getManifests().get(WORKSPACE_DIR);
        if (text == null) {
            return;
        }
        Map<String, @Nullable Object> manifest = rootManifest(text);
        Object runtimeOnFail = options.get("runtimeOnFail");
        List<Map<String, @Nullable Object>> wanted = new ArrayList<>();
        Set<Object> seen = new LinkedHashSet<>();
        for (String enginesFieldName : new String[]{"devEngines", "engines"}) {
            Object engines = manifest.get(enginesFieldName);
            Object enginesRuntime = engines instanceof Map ? ((Map<String, @Nullable Object>) engines).get("runtime") :
                    null;
            if (enginesRuntime == null || enginesRuntime == Js.UNDEFINED) {
                continue;
            }
            List<@Nullable Object> runtimes = enginesRuntime instanceof List ? (List<@Nullable Object>) enginesRuntime :
                    Collections.singletonList(enginesRuntime);
            for (Object runtime : runtimes) {
                if (runtime == null || runtime == Js.UNDEFINED) {
                    if (!options.getRelease().checksTheRuntimeVersion()) {
                        return;
                    }
                    throw new JsError(JsError.ERROR,
                            "Cannot read properties of " + Js.string(runtime) + " (reading 'name')");
                }
                Object name = runtime instanceof Map ? ((Map<String, @Nullable Object>) runtime).get("name") : null;
                if (!RUNTIME_DISPLAY_NAMES.containsKey(name) || !seen.add(name)) {
                    continue;
                }
                wanted.add((Map<String, @Nullable Object>) runtime);
            }
        }
        for (Map<String, @Nullable Object> runtime : wanted) {
            Object onFail = Js.truthy(runtimeOnFail) ? runtimeOnFail : runtime.get("onFail");
            if ("error".equals(onFail)) {
                checkRuntime(runtime, options.getRelease());
            }
        }
    }

    // checkRuntime of a runtime whose onFail is "error", where failRuntimeCheck throws
    private void checkRuntime(Map<String, @Nullable Object> runtime, PnpmRelease release) {
        String runtimeName = (String) runtime.get("name");
        String displayName = RUNTIME_DISPLAY_NAMES.get(runtimeName);
        Object wantedRange = runtime.get("version");
        String failure;
        if (!Js.truthy(wantedRange) || !(wantedRange instanceof String) ||
            release.underSemver(semver -> NodeSemver.validRange((String) wantedRange,
                    NodeSemver.Options.DEFAULT.release(semver))) == null) {
            failure = Js.truthy(wantedRange) ?
                    "This project requires an invalid " + displayName + " version range: " + Js.string(wantedRange) :
                    "This project requires a " + displayName + " runtime but does not specify a version range";
        } else if (!"node".equals(runtimeName)) {
            if (!release.checksTheRuntimeVersion()) {
                return;
            }
            throw new CannotKnow("whether the host runs " + displayName + " " + wantedRange + ", which the root " +
                                 "project requires with onFail \"error\"");
        } else {
            String currentVersion = inputs.getHost().getNodeVersion();
            if (currentVersion == null || release.underSemver(semver -> NodeSemver.satisfies(currentVersion,
                    (String) wantedRange, NodeSemver.Options.INCLUDE_PRERELEASE.release(semver)))) {
                return;
            }
            failure = "This project requires " + displayName + " " + wantedRange + ". Your current " + displayName +
                      " is " + currentVersion;
        }
        if (release.checksTheRuntimeVersion()) {
            throw new PnpmError("BAD_RUNTIME_VERSION", failure, RUNTIME_ON_FAIL_HINT);
        }
    }

    /**
     * getConfig.ts' installConfigDepsAndLoadHooks: the config dependencies into the env document of the lock in
     * lockfileDir, else rootProjectManifestDir; the pnpmfiles, its plugins' among them, from lockfileDir or config.dir.
     */
    void installConfigDepsAndLoadHooks(PnpmOptions options, @Nullable String lockfileDir) {
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
            EnvLockfile.OnDisk rootLockFile = lockFileOf(rootDir);
            if (rootLockFile != null) {
                envInstaller.resolveAndInstallConfigDeps(configDeps, rootLockFile, options);
            } else {
                // into the env document of rootDir's lock, which the inputs do not hold: what it does not lock resolves
                try {
                    envInstaller.resolveAndInstallConfigDeps(configDeps, new EnvLockfile.OnDisk(null), options);
                } catch (PnpmError | JsError e) {
                    throw new CannotKnow("whether pnpm resolves the config dependencies, which fail here (" +
                                         Js.message(e) + ") unless the env document of " +
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
                options.get("globalPnpmfile"), pnpmfile == null || pnpmfile == Js.UNDEFINED ?
                        Collections.emptyList() : pnpmfile instanceof List ? (List<?>) pnpmfile :
                        Collections.singletonList(pnpmfile), tryLoadDefaultPnpmfile(options));
    }

    /**
     * getConfig.ts: config.tryLoadDefaultPnpmfile, which recursive.ts' requireHooks reads for each project too.
     */
    static boolean tryLoadDefaultPnpmfile(PnpmOptions options) {
        Object pnpmfile = options.get("pnpmfile");
        return pnpmfile == null || pnpmfile == Js.UNDEFINED;
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
