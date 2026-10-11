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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.lock.SpecUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Port of @yarnpkg/plugin-essentials sources/commands/install.ts, {@code --mode=update-lockfile} (Yarn 4.10.3).
 */
public final class YarnBerryLockRegenerator {

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.YARN;

    private static final String MERGE_CONFLICT_START = "<<<<<<<";

    /**
     * What the engine is given for a Yarn Berry project.
     */
    @Value
    @Builder(toBuilder = true)
    public static class Inputs {
        /**
         * The yarn.lock; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The project's manifests by directory relative to the root: {@code ""} the root, {@code packages/a} a
         * workspace member.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The files the project holds relative to its root, when the host says; a package.json among them the host
         * did not read is unknown.
         */
        @Nullable
        Set<String> paths;

        /**
         * The text of the project's patch files by path from the root.
         */
        @Singular
        Map<String, String> files;

        /**
         * The rc files by path from the root, from where Yarn runs up: {@code packages/a/.yarnrc.yml},
         * {@code .yarnrc.yml}, {@code ../.yarnrc.yml} for a directory above, and its {@code ../package.json}.
         */
        @Singular
        Map<String, String> configFiles;

        /**
         * The files of the user's home directory Yarn reads, by name ({@code .yarnrc.yml}): the host's registry
         * configuration is its .yarnrc.yml.
         */
        @Singular
        Map<String, String> homeFiles;

        /**
         * The directory Yarn runs in, the edited manifest's, relative to the root.
         */
        @Builder.Default
        String runsIn = "";

        /**
         * The environment Yarn runs under; the {@code YARN_*} keys are settings.
         */
        @Nullable
        Map<String, String> env;

        @Builder.Default
        Host host = Host.UNKNOWN;

        @NonNull
        Instant now;
    }

    /**
     * The project as {@code yarn install} has it when it starts resolving.
     */
    @Getter
    static final class Loaded {
        private final Project project;
        private final LegacyMigrationResolver legacyMigrationResolver;

        /**
         * The settings the migration rules wrote to {@code .yarnrc.yml}.
         */
        private final Map<String, Object> migratedSettings;

        Loaded(Project project, LegacyMigrationResolver legacyMigrationResolver, Map<String, Object> migratedSettings) {
            this.project = project;
            this.legacyMigrationResolver = legacyMigrationResolver;
            this.migratedSettings = migratedSettings;
        }
    }

    private final Inputs inputs;
    private final RegistrySource registry;

    private @Nullable Configuration options;

    // each manifest parsed once, with the same throw on every read
    private final Map<String, Object> manifests = new LinkedHashMap<>();

    public YarnBerryLockRegenerator(Inputs inputs, RegistrySource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    /**
     * Configuration.findProjectCwd from where Yarn runs, whose lock it writes; whether it runs in one of the project's
     * workspaces is Project.find's, which the install checks. {@code holds} answers by path from where Yarn runs.
     */
    public static InstallRoot installRoot(Predicate<String> holds, int levels) {
        int projectCwd = Configuration.findProjectCwd(holds, levels);
        return projectCwd < 0 ? InstallRoot.STOPS_HERE : InstallRoot.at(projectCwd);
    }

    /**
     * The lock Yarn writes for the edited manifests over the project's lock, or the failure, after a second install
     * over the written lock, with the settings the first wrote, changed nothing.
     */
    public Result regenerate() {
        String lock = inputs.getLock();
        if (lock == null) {
            return Result.failure(INSTALL.failure(Rejected.discardsLock("the project has no yarn.lock")));
        }
        Map<String, Object> settings = new LinkedHashMap<>();
        String written;
        try {
            written = install(lock, settings);
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, again -> new YarnBerryLockRegenerator(inputs.toBuilder().lock(again).build(),
                registry).install(again, settings), YarnBerryLockRegenerator::failure);
    }

    /**
     * One install over a lock: the lock the project holds afterwards; {@code settings} is what the migration rules
     * wrote to {@code .yarnrc.yml}, added to when this install migrates the lock.
     */
    String install(String lock, Map<String, Object> settings) {
        YarnRelease release = release();
        options().checkShapeKeys(release);
        if (options().getThirdPartyPlugin() != null) {
            throw new CannotKnow("what the plugin " + options().getThirdPartyPlugin() + " does to the lock (Yarn " +
                                 "runs its code)");
        }
        // Configuration.find's plugin pass rejects a setting the release does not know before install reads the lock
        options().copy().restrictTo(release);
        if (lock.contains(MERGE_CONFLICT_START)) {
            throw new CannotKnow("how Yarn repairs the lock's merge conflict (it asks git for each side)");
        }
        Loaded loaded = load(release, settings);
        Project project = loaded.getProject();
        find(project);
        if (project.lockfileLastVersion == null) {
            throw Rejected.discardsLock(lock.trim().isEmpty() ? "the lock is empty" : "the lock has no __metadata");
        }
        settings.putAll(loaded.getMigratedSettings());
        boolean hardened = project.configuration.getBoolean("enableHardenedMode");
        if (hardened) {
            project.lockfileNeedsRefresh = true;
        }
        project.resolveEverything(loaded.getLegacyMigrationResolver(), registry, hardened);
        project.fetchEverything(registry);
        String written = write(project);
        return written == null ? lock : written;
    }

    /**
     * Project.find from where Yarn runs: the project it finds must be this lock's, and the nearest package directory
     * one of its workspaces.
     */
    private void find(Project project) {
        String runsIn = inputs.getRunsIn();
        if (runsIn.isEmpty()) {
            return;
        }
        int levels = runsIn.split("/").length;
        int projectCwd = Configuration.findProjectCwd(path -> {
            String file = PosixPath.normalize(runsIn + "/" + path);
            return "yarn.lock".equals(file) || inputs.getPaths() != null && inputs.getPaths().contains(file) ||
                   file.endsWith("package.json") && inputs.getManifests().containsKey(file.contains("/") ?
                           PosixPath.dirname(file) : "");
        }, levels);
        if (projectCwd != levels) {
            throw new CannotKnow("the lock Yarn writes: run in " + runsIn + " its project is " +
                                 (levels - projectCwd) + " directories below this lock's");
        }
        if (project.tryWorkspaceByCwd(runsIn) == null) {
            throw new Configuration.UsageError("The nearest package directory (" + runsIn + ") doesn't seem to be " +
                                              "part of the project declared in .");
        }
    }

    /**
     * {@code yarnPath}'s release unless {@code ignorePath}, else the packageManager corepack finds where Yarn runs;
     * CannotKnow when neither names a version or the data lacks it (Yarn 2 and 3, a release after the data's).
     */
    YarnRelease release() {
        SpecUtils.Descriptor spec = SpecUtils.findProjectSpec(inputs.getRunsIn(), levelsAbove(), this::manifestText,
                path -> inputs.getPaths() != null && inputs.getPaths().contains(path), "yarn",
                HostEnvironment.of(inputs.getEnv()));
        Configuration options = options();
        Object yarnPath = options.get("yarnPath");
        String version;
        if (yarnPath != null && !options.getBoolean("ignorePath")) {
            version = Configuration.yarnPathVersion(Js.string(yarnPath));
            if (version == null) {
                throw new CannotKnow("the Yarn release " + Js.string(yarnPath) + " is (yarnPath names no version)");
            }
        } else {
            if (spec == null) {
                throw new CannotKnow("the Yarn release that runs (corepack falls back to its own default and no " +
                                     "yarnPath names one)");
            }
            version = SpecUtils.installVersion(spec);
            if (version == null) {
                throw new CannotKnow("the Yarn release at " + spec.getRange() + " (corepack downloads it)");
            }
        }
        YarnRelease release = YarnRelease.of(version);
        if (release == null) {
            throw new CannotKnow("the data of Yarn " + version);
        }
        return release;
    }

    /**
     * Under the release, the configuration, the project found, and the migration rules applied ({@code settings} from
     * an earlier install's migration); throws Configuration.UsageError where the configuration rejects a setting.
     */
    Loaded load(YarnRelease release, Map<String, Object> settings) {
        Configuration options = options().copy();
        options.restrictTo(release);
        for (Map.Entry<String, Object> setting : settings.entrySet()) {
            options.useCompat(setting.getKey(), setting.getValue());
        }

        Configuration configuration = options;
        Project project = new Project(configuration, inputs.getNow(), inputs.getPaths(), inputs.getFiles(),
                inputs.getHost());
        project.setupResolutions(inputs.getLock());
        List<String> directories = new ArrayList<>();
        for (String location : inputs.getManifests().keySet()) {
            directories.add(location.isEmpty() ? "." : location);
        }
        project.setupWorkspaces(new FastGlob(directories, inputs.getPaths(), inputs.getHost()),
                cwd -> manifest(".".equals(cwd) ? "" : cwd));

        Map<String, Object> migratedSettings = new LinkedHashMap<>();
        Object lockfileLastVersion = project.lockfileLastVersion;
        if (lockfileLastVersion != null) {
            boolean legacy = Integer.valueOf(-1).equals(lockfileLastVersion);
            boolean old = !legacy && Js.toNumber(lockfileLastVersion) < release.getLockfileVersion();
            if (legacy && options.useCompat("nodeLinker", "node-modules")) {
                migratedSettings.put("nodeLinker", "node-modules");
            }
            if (old && options.useCompat("enableGlobalCache", false)) {
                migratedSettings.put("enableGlobalCache", false);
            }
            if (old && options.useCompat("compressionLevel", "mixed")) {
                migratedSettings.put("compressionLevel", "mixed");
            }
        }

        LegacyMigrationResolver legacyMigrationResolver = new LegacyMigrationResolver(configuration.makeResolver());
        legacyMigrationResolver.setup(project);
        return new Loaded(project, legacyMigrationResolver, migratedSettings);
    }

    /**
     * {@code persistLockfile} of the project as it stands: the new lockfile, or null when it leaves the file as it
     * is.
     */
    @Nullable
    String write(Project project) {
        String os = inputs.getHost().getOs();
        return project.persistLockfile("win32".equals(os) ? "\r\n" : "\n");
    }

    private static Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    // a plain Error reaches Yarn's report as YN0001, or uncaught as Internal Error or its stack: none is Yarn's own
    private static RuntimeException outcome(RuntimeException e) {
        if (e instanceof Project.UnknownChecksums) {
            return new CannotKnow(String.valueOf(e.getMessage()), Reason.CHECKSUM_UNAVAILABLE);
        }
        if (e instanceof ResolutionChain.Endless) {
            return Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, String.valueOf(e.getMessage()));
        }
        if (e instanceof ReportError) {
            ReportError error = (ReportError) e;
            String message = error.getName() + " " + error.getMessage();
            boolean crash = error.getCode() == ReportError.EXCEPTION ||
                            error.getCode() == ReportError.STACK_OVERFLOW_RESOLUTION;
            return crash ? Rejected.crashes(reasonFor(error), null, message) :
                    Rejected.rejects(reasonFor(error), message);
        }
        if (e instanceof JsYaml.YamlException) {
            return Rejected.crashes(Reason.MALFORMED_LOCK, null, e.toString());
        }
        if (e instanceof Configuration.UsageError) {
            return Rejected.rejects(Reason.RESOLUTION_REQUIRED, "Usage Error: " + e.getMessage());
        }
        if (e instanceof Js.SyntaxError) {
            return Rejected.crashes(Reason.MALFORMED_MANIFEST, null, "YN0001 SyntaxError: " + e.getMessage());
        }
        if (e instanceof JsError) {
            return Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "YN0001 " + e);
        }
        return e;
    }

    private static Reason reasonFor(ReportError error) {
        String message = String.valueOf(error.getMessage());
        switch (error.getCode()) {
            case ReportError.RESOLUTION_FAILED:
                return message.endsWith(": No candidates found") ? SharedError.NO_MATCHING_VERSION.getReason() :
                        Reason.RESOLUTION_REQUIRED;
            case ReportError.REMOTE_NOT_FOUND:
                return SharedError.NO_MATCHING_VERSION.getReason();
            case ReportError.NETWORK_ERROR:
                return message.endsWith("Package not found") ? SharedError.PACKAGE_NOT_FOUND.getReason() :
                        Reason.REGISTRY_UNREACHABLE;
            case ReportError.RESOLUTION_MISMATCH:
                return Reason.MALFORMED_LOCK;
            default:
                return Reason.RESOLUTION_REQUIRED;
        }
    }

    // an Error the configuration throws reaches the CLI outside any report, which prints it as an Internal Error
    private Configuration options() {
        if (options == null) {
            Map<String, String> rcFiles = new LinkedHashMap<>(inputs.getConfigFiles());
            for (Map.Entry<String, String> file : inputs.getHomeFiles().entrySet()) {
                rcFiles.put(Configuration.HOME + file.getKey(), file.getValue());
            }
            try {
                options = Configuration.load(rcFiles, HostEnvironment.of(inputs.getEnv()));
            } catch (JsError e) {
                throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "Internal Error: " + e.getMessage());
            }
        }
        return options;
    }

    // how far above the project's directory the host's files reach, as far as corepack walks here
    private int levelsAbove() {
        int levels = 0;
        List<String> held = new ArrayList<>(inputs.getConfigFiles().keySet());
        if (inputs.getPaths() != null) {
            held.addAll(inputs.getPaths());
        }
        for (String path : held) {
            int up = 0;
            while (path.startsWith("../", 3 * up)) {
                up++;
            }
            levels = Math.max(levels, up);
        }
        return levels;
    }

    private @Nullable String manifestText(String location) {
        String file = location.isEmpty() ? "package.json" : location + "/package.json";
        String text = location.startsWith("..") ? inputs.getConfigFiles().get(file) :
                inputs.getManifests().get(location);
        return CannotKnow.requireRead(text, inputs.getPaths(), file);
    }

    private @Nullable Manifest manifest(String location) {
        String text = manifestText(location);
        if (text == null) {
            return null;
        }
        Object parsed = manifests.computeIfAbsent(location, l -> {
            try {
                return Manifest.fromText(text);
            } catch (RuntimeException e) {
                return e;
            }
        });
        if (parsed instanceof RuntimeException) {
            throw (RuntimeException) parsed;
        }
        return (Manifest) parsed;
    }
}
