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

/**
 * Port of @yarnpkg/plugin-essentials sources/commands/install.ts with {@code Project.install} for
 * {@code yarn install --mode=update-lockfile} (Yarn 4.10.3), as far as the resolution: the configuration and the
 * project's release, the project found (lockfile and workspaces), and the lockfile migration rules.
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

    public BerryLockRegenerator(Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * The Yarn release the project runs, named by the root manifest's {@code packageManager}, else by the release
     * file {@code yarnPath} names.
     *
     * @throws CannotKnow when neither names one, or the data has none of the one named
     */
    YarnRelease release() {
        Manifest rootManifest = manifest("");
        BerryOptions options = BerryOptions.load(inputs.getConfigFiles(), inputs.getEnv());
        Object yarnPath = options.get("yarnPath");
        String version = BerryOptions.yarnVersion(rootManifest == null ? null : rootManifest.packageManager,
                yarnPath instanceof String ? (String) yarnPath : null);
        if (version == null) {
            throw new CannotKnow("the Yarn release that runs (no packageManager in package.json and no yarnPath)");
        }
        YarnRelease release = YarnRelease.of(version);
        if (release == null) {
            throw new CannotKnow("the data of Yarn " + version);
        }
        return release;
    }

    /**
     * Under the release, the configuration, the project found, and the migration rules applied.
     *
     * @throws BerryOptions.UsageError where the configuration rejects a setting
     */
    Loaded load(YarnRelease release) {
        BerryOptions options = BerryOptions.load(inputs.getConfigFiles(), inputs.getEnv());
        options.restrictTo(release.getSettings());
        options.withYarnVersion(release.getVersion());

        Project project = new Project(new Configuration(options, release));
        project.setupResolutions(inputs.getLock());
        Map<String, Manifest> manifests = new LinkedHashMap<>();
        for (String location : inputs.getManifests().keySet()) {
            manifests.put(location.isEmpty() ? "." : location, manifest(location));
        }
        project.setupWorkspaces(manifests);

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

    private @Nullable Manifest manifest(String location) {
        String text = inputs.getManifests().get(location);
        return text == null ? null : Manifest.fromText(text);
    }
}
