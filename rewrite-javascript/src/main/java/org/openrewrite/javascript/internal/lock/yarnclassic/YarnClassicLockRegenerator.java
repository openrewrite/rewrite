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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A lock-only {@code yarn install} (yarn 1.22.22), run as cli/index.js runs it: {@link Cli}, {@link Config},
 * {@link Install}.
 */
public final class YarnClassicLockRegenerator {

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.YARN;
    private static final Pattern CONFIG_FILE = Pattern.compile("(\\.\\./)*\\.(yarnrc|yarnrc\\.yml|npmrc)");

    /**
     * What the engine is given for a Yarn classic project.
     */
    @Value
    @Builder(toBuilder = true)
    public static class Inputs {
        /**
         * The yarn.lock as the project holds it; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The project's manifests by location: {@code ""} the root, {@code packages/a} a workspace member.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The .yarnrc, .yarnrc.yml and .npmrc files of the project's directory, the directories above it and those from
         * {@link #cwd} up, and the package.json of the directories above, by path from the root.
         */
        @Singular
        Map<String, String> configFiles;

        /**
         * The files of the user's home directory yarn reads, by name ({@code .npmrc}, {@code .yarnrc}): the host's
         * registry configuration is its .npmrc.
         */
        @Singular
        Map<String, String> homeFiles;

        /**
         * The paths the project holds relative to its root, and the files of the directories above it
         * ({@code ../.npmrc}), when the host says; a config file among them the host did not read is unknown.
         */
        @Nullable
        Set<String> paths;

        /**
         * The project's directory from the repository's root ({@code mid/app}, {@code ""} at the root), when the
         * host says; yarn looks above it for a workspace root that lists the project.
         */
        @Nullable
        String directory;

        /**
         * The directory yarn runs in relative to the root ({@code packages/a}), {@code ""} the root's: a workspace
         * member whose own lock yarn passes over for the root's.
         */
        @Builder.Default
        String cwd = "";

        /**
         * The environment yarn runs in; null when the host does not know it.
         */
        @Nullable
        Map<String, String> env;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The config files by their path from the directory yarn runs in.
         */
        Map<String, String> configFilesFromCwd() {
            if (cwd.isEmpty()) {
                return configFiles;
            }
            Map<String, String> files = new LinkedHashMap<>();
            for (Map.Entry<String, String> file : configFiles.entrySet()) {
                String path = fromCwd(file.getKey());
                if (path != null) {
                    files.put(path, file.getValue());
                }
            }
            return files;
        }

        /**
         * A path from the root as a path from {@link #cwd}, when it sits in cwd or a directory above it.
         */
        @Nullable
        String fromCwd(String path) {
            int slash = path.lastIndexOf('/');
            String dir = slash < 0 || path.startsWith("../") ? "" : path.substring(0, slash);
            if (!dir.isEmpty() && !cwd.equals(dir) && !cwd.startsWith(dir + "/")) {
                return null;
            }
            return Config.up(segments(cwd) - segments(dir)) + (dir.isEmpty() ? path :
                    path.substring(slash + 1));
        }

        private static int segments(String location) {
            return location.isEmpty() ? 0 : location.split("/").length;
        }

        /**
         * How many directories above the project's the inputs reach: the repository's root when the host says
         * where the project sits in it, else the deepest file given.
         */
        int reach() {
            if (directory != null) {
                return directory.isEmpty() ? 0 : directory.split("/").length;
            }
            Set<String> files = new LinkedHashSet<>(configFiles.keySet());
            if (paths != null) {
                files.addAll(paths);
            }
            return Config.depth(files);
        }

        /**
         * path.relative from the directory {@code depth} levels above the project's to the project's.
         */
        String relativePath(int depth) {
            if (directory == null) {
                throw new CannotKnow("whether the workspaces of " + Config.up(depth) + "package.json " +
                                     "list this project, whose directory in the repository the host did not give");
            }
            String[] segments = directory.split("/");
            return String.join("/", Arrays.asList(segments).subList(segments.length - depth, segments.length));
        }

        // declared so that javadoc, which reads the source before Lombok, finds it
        public static class InputsBuilder {
        }
    }

    private final Inputs inputs;
    private final RegistrySource registry;

    public YarnClassicLockRegenerator(Inputs inputs, RegistrySource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    /**
     * The lock yarn writes for the edited manifests over the project's lock, or the failure, after a second install
     * over the written lock changed nothing.
     */
    public Result regenerate() {
        String lock = inputs.getLock();
        if (lock == null) {
            return Result.failure(INSTALL.failure(Rejected.discardsLock("the project has no yarn.lock")));
        }
        String written;
        try {
            written = install(lock);
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, this::install, this::failure);
    }

    /**
     * One install over a lock: the lock the project holds afterwards.
     */
    String install(String lock) {
        Inputs these = lock.equals(inputs.getLock()) ? inputs : inputs.toBuilder().lock(lock).build();
        requireConfigFiles(these);
        Map<String, @Nullable Object> flags = start(these);
        Config config = new Config(these, flags);
        String saved = new Install(these, config, flags, registry).run();
        return saved == null ? lock : saved;
    }

    /**
     * cli/index.js {@code start} and {@code main}: the flags the .yarnrc chain gives the install.
     */
    static Map<String, @Nullable Object> start(Inputs inputs) {
        Map<String, @Nullable Object> rc = Rc.getRcConfigForCwd(inputs.configFilesFromCwd(), inputs.getHomeFiles());
        Cli cli = new Cli(inputs);
        cli.start(rc);
        cli.main();
        return Rc.getRcArgs(rc);
    }

    // every install reads the config files of the directory it runs in and of each directory above it
    private static void requireConfigFiles(Inputs inputs) {
        Set<String> paths = inputs.getPaths();
        if (paths == null) {
            return;
        }
        Map<String, String> configFiles = inputs.configFilesFromCwd();
        for (String path : paths) {
            String fromCwd = inputs.fromCwd(path);
            if (fromCwd != null && CONFIG_FILE.matcher(fromCwd).matches() && !configFiles.containsKey(fromCwd)) {
                throw CannotKnow.unread(path);
            }
        }
    }

    /**
     * Where yarn run in the project's directory installs and writes its lock, a {@code --focus} install's too; a walk
     * that fails, which {@link #regenerate()} reports, stops here.
     */
    public static InstallRoot installRoot(Inputs inputs) {
        try {
            Map<String, @Nullable Object> flags = start(inputs);
            int workspaceRoot = Config.findWorkspaceRoot(inputs, !Js.truthy(flags.get("strictSemver")));
            return InstallRoot.at(Math.max(workspaceRoot, 0));
        } catch (RuntimeException e) {
            return InstallRoot.STOPS_HERE;
        }
    }

    private Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    private static RuntimeException outcome(RuntimeException e) {
        if (e instanceof LocaleCompare.UnknownOrder) {
            return new CannotKnow("the order yarn finds the workspaces in: " + e.getMessage());
        }
        if (e instanceof MessageError) {
            String message = String.valueOf(e.getMessage());
            return Rejected.rejects(reasonFor(message), message);
        }
        // the registry's 404 is its verdict, as in every port; yarn's own plain Errors (a TTY question) have no code
        if (e instanceof NpmRegistry.NotFound) {
            return Rejected.rejects(SharedError.PACKAGE_NOT_FOUND, ((NpmRegistry.NotFound) e).getPackageName(),
                    e.toString());
        }
        if (e instanceof JsError) {
            JsError error = (JsError) e;
            Reason reason = error.isSyntaxError() ? Reason.MALFORMED_MANIFEST : Reason.RESOLUTION_REQUIRED;
            return Rejected.crashes(reason, null, error.toString());
        }
        return e;
    }

    private static Reason reasonFor(String message) {
        if (message.startsWith("Couldn't find any versions for") || message.startsWith("No valid versions found")) {
            return SharedError.NO_MATCHING_VERSION.getReason();
        }
        if (message.startsWith("Couldn't find package")) {
            return SharedError.PACKAGE_NOT_FOUND.getReason();
        }
        if (message.startsWith("Can't install from a lockfile of version")) {
            return Reason.UNSUPPORTED_LOCKFILE_VERSION;
        }
        if (message.startsWith("Can't add ")) {
            return Reason.MALFORMED_LOCK;
        }
        if (message.startsWith("package.json: ") || message.contains("workspaces") ||
            message.contains("Workspaces") || message.contains("nohoist") ||
            message.startsWith("There are more than one workspace with name")) {
            return Reason.MALFORMED_MANIFEST;
        }
        return Reason.RESOLUTION_REQUIRED;
    }
}
