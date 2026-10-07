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
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @yarnpkg/plugin-essentials sources/commands/install.ts, {@code --mode=update-lockfile} (Yarn 4.10.3).
 */
public final class BerryLockRegenerator {

    /**
     * What the engine is given for a Yarn Berry project.
     */
    @Value
    @Builder
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
         * The rc files by path from the root: {@code .yarnrc.yml}, and {@code ../.yarnrc.yml} for a directory above.
         */
        @Singular
        Map<String, String> configFiles;

        /**
         * The environment Yarn runs under; the {@code YARN_*} keys are settings.
         */
        @Singular("env")
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

    private @Nullable BerryOptions options;

    // each manifest parsed once, with the same throw on every read
    private final Map<String, Object> manifests = new LinkedHashMap<>();

    public BerryLockRegenerator(Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * The Yarn release the project runs: the release file {@code yarnPath} names unless {@code ignorePath} is set
     * (the release corepack starts hands the install to it, yarnpkg-cli lib.ts {@code run}), else the root
     * manifest's {@code packageManager}.
     *
     * @throws CannotKnow when neither names a version, the data has none of the one named, or it is a release
     *                    after the one this package ports
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

    /**
     * Under the release, the configuration, the project found, and the migration rules applied.
     *
     * @throws BerryOptions.UsageError where the configuration rejects a setting
     */
    Loaded load(YarnRelease release) {
        BerryOptions options = options().copy();
        options.restrictTo(release.getSettings());

        Project project = new Project(new Configuration(options, release));
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
        LegacyMigrationResolver legacyMigrationResolver = new LegacyMigrationResolver(
                new MultiResolver(new WorkspaceResolver(), new NpmRemapResolver(), new NpmSemverResolver(),
                        new NpmTagResolver()));
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

    private BerryOptions options() {
        if (options == null) {
            options = BerryOptions.load(inputs.getConfigFiles(), inputs.getEnv());
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
