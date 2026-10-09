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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
         * The rc files by path from the root: {@code .yarnrc.yml}, and {@code ../.yarnrc.yml} for a directory above.
         */
        @Singular
        Map<String, String> configFiles;

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

        private final List<String> warnings;

        Loaded(Project project, LegacyMigrationResolver legacyMigrationResolver, Map<String, Object> migratedSettings,
               List<String> warnings) {
            this.project = project;
            this.legacyMigrationResolver = legacyMigrationResolver;
            this.migratedSettings = migratedSettings;
            this.warnings = warnings;
        }
    }

    private final Inputs inputs;
    private final RegistrySource registry;

    private @Nullable BerryOptions options;

    // each manifest parsed once, with the same throw on every read
    private final Map<String, Object> manifests = new LinkedHashMap<>();

    public YarnBerryLockRegenerator(Inputs inputs, RegistrySource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    YarnBerryLockRegenerator(Inputs inputs) {
        this(inputs, new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name) {
                return null;
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version) {
                return null;
            }
        });
    }

    /**
     * Configuration.findProjectCwd: Yarn's project is the nearest directory holding a yarn.lock, so a project with a
     * lock of its own, the only one the scan asks about, installs here: 0.
     */
    public static int installRoot() {
        return 0;
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
        if (lock.contains(MERGE_CONFLICT_START)) {
            throw new CannotKnow("how Yarn repairs the lock's merge conflict (it asks git for each side)");
        }
        Manifest rootManifest;
        try {
            rootManifest = manifest("");
        } catch (Js.SyntaxError e) {
            throw Rejected.rejects(Reason.MALFORMED_MANIFEST, "Invalid package.json in package.json (corepack)");
        }
        if (rootManifest != null && rootManifest.packageManager != null) {
            parseSpec(rootManifest.packageManager);
        }
        YarnRelease release = release();
        if (options().getThirdPartyPlugin() != null) {
            throw new CannotKnow("what the plugin " + options().getThirdPartyPlugin() + " does to the lock (Yarn " +
                                 "runs its code)");
        }
        Loaded loaded = load(release, settings);
        Project project = loaded.getProject();
        if (project.lockfileLastVersion == null) {
            throw Rejected.discardsLock(lock.trim().isEmpty() ? "the lock is empty" : "the lock has no __metadata");
        }
        settings.putAll(loaded.getMigratedSettings());
        boolean hardened = project.configuration.getOptions().getBoolean("enableHardenedMode");
        if (hardened) {
            project.lockfileNeedsRefresh = true;
        }
        project.resolveEverything(loaded.getLegacyMigrationResolver(), registry, hardened);
        project.fetchEverything(registry);
        String written = write(project);
        return written == null ? lock : written;
    }

    /**
     * corepack's {@code parseSpec} of the root manifest's {@code packageManager}, which corepack reads before it
     * starts the release it names (corepack 0.35.0).
     */
    private static void parseSpec(String raw) {
        int atIndex = raw.indexOf('@');
        if (atIndex == -1 || atIndex == raw.length() - 1) {
            throw corepackRejects("No version specified for " + raw + " in \"packageManager\" of package.json");
        }
        String name = raw.substring(0, atIndex);
        String range = raw.substring(atIndex + 1);
        boolean supported = "npm".equals(name) || "pnpm".equals(name) || "yarn".equals(name);
        if (WhatwgUrl.tryParse(range) == null) {
            if (!SemverUtils.valid(range)) {
                throw corepackRejects("Invalid package manager specification in package.json (" + raw + "); " +
                                      "expected a semver version");
            }
            if (!supported) {
                throw corepackRejects("Unsupported package manager specification (" + raw + ")");
            }
        } else if (supported) {
            throw corepackRejects("Illegal use of URL for known package manager. Instead, select a specific " +
                                  "version, or set COREPACK_ENABLE_UNSAFE_CUSTOM_URLS=1 in your environment (" + raw +
                                  ")");
        }
    }

    private static Rejected corepackRejects(String message) {
        return Rejected.rejects(Reason.MALFORMED_MANIFEST, message + " (corepack)");
    }

    /**
     * The release the project runs: {@code yarnPath}'s file unless {@code ignorePath}, else {@code packageManager};
     * CannotKnow when neither names a version, the data lacks it, or it is later than the release ported.
     */
    YarnRelease release() {
        BerryOptions options = options();
        Object yarnPath = options.get("yarnPath");
        String version;
        if (yarnPath != null && !options.getBoolean("ignorePath")) {
            version = BerryOptions.yarnPathVersion(Js.string(yarnPath));
            if (version == null) {
                throw new CannotKnow("the Yarn release " + Js.string(yarnPath) + " is (yarnPath names no version)");
            }
        } else {
            Manifest rootManifest = manifest("");
            version = BerryOptions.packageManagerVersion(rootManifest == null ? null : rootManifest.packageManager);
            if (version == null) {
                throw new CannotKnow("the Yarn release that runs (no packageManager in package.json and no " +
                                     "yarnPath)");
            }
        }
        YarnRelease release = YarnRelease.of(version);
        if (release == null) {
            throw new CannotKnow("the data of Yarn " + version);
        }
        if (YarnRelease.compareVersions(version, YarnRelease.PORTED) > 0) {
            throw new CannotKnow("the lock Yarn " + version + " writes (this engine ports Yarn " +
                                 YarnRelease.PORTED + ")");
        }
        return release;
    }

    Loaded load(YarnRelease release) {
        return load(release, Collections.emptyMap());
    }

    /**
     * Under the release, the configuration, the project found, and the migration rules applied ({@code settings} from
     * an earlier install's migration); throws BerryOptions.UsageError where the configuration rejects a setting.
     */
    Loaded load(YarnRelease release, Map<String, Object> settings) {
        BerryOptions options = options().copy();
        options.restrictTo(release.getSettings());
        for (Map.Entry<String, Object> setting : settings.entrySet()) {
            options.useCompat(setting.getKey(), setting.getValue());
        }

        Configuration configuration = new Configuration(options, release);
        Project project = new Project(configuration, inputs.getNow(), inputs.getPaths(), inputs.getFiles());
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
            boolean old = !legacy && Project.toNumber(lockfileLastVersion) < Project.LOCKFILE_VERSION;
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

        List<String> warnings = new ArrayList<>();
        LegacyMigrationResolver legacyMigrationResolver = new LegacyMigrationResolver(configuration.makeResolver());
        legacyMigrationResolver.setup(project, warnings);
        return new Loaded(project, legacyMigrationResolver, migratedSettings, warnings);
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
        if (e instanceof Project.HeapExhausted) {
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
        if (e instanceof BerryOptions.UsageError) {
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
    private BerryOptions options() {
        if (options == null) {
            try {
                options = BerryOptions.load(inputs.getConfigFiles(), HostEnvironment.of(inputs.getEnv()));
            } catch (JsError e) {
                throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "Internal Error: " + e.getMessage());
            }
        }
        return options;
    }

    private @Nullable Manifest manifest(String location) {
        String text = inputs.getManifests().get(location);
        if (text == null) {
            String file = location.isEmpty() ? "package.json" : location + "/package.json";
            if (inputs.getPaths() != null && inputs.getPaths().contains(file)) {
                throw new CannotKnow(file + " (its contents were not read)");
            }
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
