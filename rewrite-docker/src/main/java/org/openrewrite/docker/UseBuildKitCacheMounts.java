/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * Adds BuildKit cache mounts to RUN instructions that invoke a package manager, so that the
 * downloads a build makes are kept outside the image layer and reused by the next build.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UseBuildKitCacheMounts extends Recipe {

    @Option(displayName = "Package managers",
            description = "The package managers to add cache mounts for, from `maven`, `gradle`, `npm`, `yarn`, " +
                    "`pip`, `go`, `cargo`, `apt` and `apk`. Defaults to every package manager except `apt` and " +
                    "`apk`, which change what a build installs and so are only added when asked for by name.",
            example = "maven",
            required = false)
    @Nullable
    List<String> packageManagers;

    @Option(displayName = "Sharing",
            description = "The `sharing` mode to give every mount this recipe adds, overriding the default of " +
                    "`locked` for the package managers that cannot share a cache between concurrent builds and " +
                    "BuildKit's own default of `shared` for the rest.",
            valid = {"shared", "private", "locked"},
            example = "locked",
            required = false)
    @Nullable
    String sharing;

    @Override
    public String getDisplayName() {
        return "Use BuildKit cache mounts";
    }

    @Override
    public String getDescription() {
        return "Adds a BuildKit cache mount to each `RUN` instruction that invokes a package manager, so that " +
                "downloaded dependencies survive across builds instead of being fetched again for every layer. " +
                "Cache mounts require BuildKit, so a Dockerfile that pins a `# syntax=` frontend older than " +
                "`docker/dockerfile:1.2` is left alone. A mount is only added where the cache directory is a pure " +
                "download cache, so a command that installs into the image from that directory, such as " +
                "`pip install --target`, keeps fetching what it needs. `RUN` instructions in shell form and in " +
                "exec form are both handled; a heredoc body is left alone, because the commands it holds are not " +
                "modelled as commands. A `RUN` that already mounts the same target, and a `RUN` that a preceding " +
                "`USER` has left running as somebody other than root, whose home directory this recipe cannot " +
                "know, are both left alone. `apt` and `apk` are not in the default set: an `apt-get` cache mount " +
                "only caches once the `docker-clean` configuration that discards the cache is removed, which this " +
                "recipe adds along with the mount, and it is incompatible with the `rm -rf /var/lib/apt/lists/*` " +
                "cleanup that `org.openrewrite.docker.AddAptGetCleanup` adds, which this recipe leaves in place.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (packageManagers != null) {
            for (String packageManager : packageManagers) {
                validated = validated.and(Validated.test("packageManagers",
                        "must be one of " + Arrays.stream(PackageManager.values()).map(pm -> pm.id).collect(joining(", ")),
                        packageManager, pm -> PackageManager.of(pm) != null));
            }
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Set<PackageManager> enabled = enabled();
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                if (enabled.isEmpty() || !supportsCacheMounts(file)) {
                    return file;
                }
                return super.visitFile(file, ctx);
            }

            @Override
            public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
                boolean[] root = {true};
                return stage.withInstructions(ListUtils.map(stage.getInstructions(), instruction -> {
                    if (instruction instanceof Docker.User) {
                        root[0] = isRoot((Docker.User) instruction);
                    } else if (root[0] && instruction instanceof Docker.Run) {
                        return addCacheMounts((Docker.Run) instruction, enabled, sharing);
                    }
                    return instruction;
                }));
            }
        };
    }

    private Set<PackageManager> enabled() {
        if (packageManagers == null) {
            Set<PackageManager> defaults = EnumSet.noneOf(PackageManager.class);
            for (PackageManager packageManager : PackageManager.values()) {
                if (!packageManager.explicitOnly) {
                    defaults.add(packageManager);
                }
            }
            return defaults;
        }
        Set<PackageManager> enabled = EnumSet.noneOf(PackageManager.class);
        for (String packageManager : packageManagers) {
            PackageManager pm = PackageManager.of(packageManager);
            if (pm != null) {
                enabled.add(pm);
            }
        }
        return enabled;
    }

    private static final Pattern SYNTAX_DIRECTIVE = Pattern.compile("^#\\s*syntax\\s*=\\s*\\S+:([^\\s@]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FRONTEND_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)");

    /// A `# syntax=` directive names the frontend that builds the file, and `RUN --mount=type=cache` is only
    /// stable from `docker/dockerfile:1.2` onwards. A file that pins an older one is left alone; a file that
    /// pins nothing is built by the daemon's own frontend, which on any Docker that still receives updates is
    /// BuildKit.
    private static boolean supportsCacheMounts(Docker.File file) {
        Comment comment = firstComment(file);
        if (comment == null) {
            return true;
        }
        Matcher directive = SYNTAX_DIRECTIVE.matcher(comment.getText().trim());
        if (!directive.find()) {
            return true;
        }
        Matcher version = FRONTEND_VERSION.matcher(directive.group(1));
        if (!version.find()) {
            return true;
        }
        int major = Integer.parseInt(version.group(1));
        int minor = Integer.parseInt(version.group(2));
        return major > 1 || (major == 1 && minor >= 2);
    }

    /// A directive is only read at the head of the file, where the first comment of the file sits in the
    /// prefix of whichever element the parser reached first.
    private static @Nullable Comment firstComment(Docker.File file) {
        List<Space> spaces = new ArrayList<>();
        spaces.add(file.getPrefix());
        if (!file.getGlobalArgs().isEmpty()) {
            spaces.add(file.getGlobalArgs().get(0).getPrefix());
        }
        if (!file.getStages().isEmpty()) {
            spaces.add(file.getStages().get(0).getPrefix());
            spaces.add(file.getStages().get(0).getFrom().getPrefix());
        }
        for (Space space : spaces) {
            if (!space.getComments().isEmpty()) {
                return space.getComments().get(0);
            }
        }
        return null;
    }

    private static boolean isRoot(Docker.User user) {
        String name = ArgumentContents.textWithVariables(user.getUser());
        return "root".equals(name) || "0".equals(name);
    }

    private static final Pattern APT_LISTS_CLEANUP = Pattern.compile("rm\\s+(-[a-zA-Z]+\\s+)*/var/lib/apt/lists");
    private static final String DOCKER_CLEAN = "/etc/apt/apt.conf.d/docker-clean";
    private static final String REMOVE_DOCKER_CLEAN = "rm -f " + DOCKER_CLEAN + " && ";

    private static Docker.Run addCacheMounts(Docker.Run run, Set<PackageManager> enabled, @Nullable String sharing) {
        List<List<String>> commands;
        String commandText;
        boolean shellForm = run.getCommand() instanceof Docker.ShellForm;
        if (shellForm) {
            commandText = ((Docker.ShellForm) run.getCommand()).getArgument().getText();
            commands = shellCommands(commandText);
        } else if (run.getCommand() instanceof Docker.ExecForm) {
            List<String> words = ((Docker.ExecForm) run.getCommand()).getArguments().stream()
                    .map(Docker.Literal::getText).collect(toList());
            commandText = String.join(" ", words);
            commands = singletonList(words);
        } else {
            return run;
        }

        Set<PackageManager> detected = detect(commands, enabled, commandText);
        if (!shellForm) {
            detected.remove(PackageManager.APT);
        }
        if (detected.isEmpty()) {
            return run;
        }

        List<Docker.Flag> mounts = new ArrayList<>();
        for (PackageManager packageManager : detected) {
            for (String mount : packageManager.mounts(sharing)) {
                if (!alreadyMounted(run, mount)) {
                    mounts.add(new Docker.Flag(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, "mount", flagValue(mount)));
                }
            }
        }
        boolean removeDockerClean = detected.contains(PackageManager.APT) && !commandText.contains(DOCKER_CLEAN);
        if (mounts.isEmpty() && !removeDockerClean) {
            return run;
        }

        Docker.Run withMounts = mounts.isEmpty() ? run : run.withFlags(ListUtils.concatAll(
                run.getFlags() == null ? emptyList() : run.getFlags(), mounts));
        if (removeDockerClean) {
            Docker.ShellForm command = (Docker.ShellForm) withMounts.getCommand();
            withMounts = withMounts.withCommand(command.withArgument(
                    command.getArgument().withText(REMOVE_DOCKER_CLEAN + commandText)));
        }
        return withMounts;
    }

    private static Set<PackageManager> detect(List<List<String>> commands, Set<PackageManager> enabled, String commandText) {
        Set<PackageManager> detected = EnumSet.noneOf(PackageManager.class);
        for (List<String> command : commands) {
            List<String> words = command;
            while (!words.isEmpty() && (ENVIRONMENT_ASSIGNMENT.matcher(words.get(0)).matches() || "sudo".equals(words.get(0)))) {
                words = words.subList(1, words.size());
            }
            if (words.isEmpty()) {
                continue;
            }
            String executable = basename(words.get(0));
            List<String> arguments = words.subList(1, words.size());
            for (PackageManager packageManager : enabled) {
                if (packageManager.matches(executable, arguments) && packageManager.cacheIsPure(arguments, commandText)) {
                    detected.add(packageManager);
                }
            }
        }
        return detected;
    }

    private static final Pattern ENVIRONMENT_ASSIGNMENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*=.*");
    private static final Pattern COMMAND_SEPARATOR = Pattern.compile("&&|\\|\\||[;|]");
    private static final Pattern CONTINUATION = Pattern.compile("^[\\\\`]$");

    /// The words of each command a shell would run in turn, so that a package manager is recognized by the
    /// command it heads rather than by appearing anywhere in the text, and each command of a chain is read.
    private static List<List<String>> shellCommands(String text) {
        List<List<String>> commands = new ArrayList<>();
        for (String command : COMMAND_SEPARATOR.split(text)) {
            List<String> words = Arrays.stream(command.trim().split("\\s+"))
                    .filter(word -> !word.isEmpty() && !CONTINUATION.matcher(word).matches())
                    .collect(toList());
            if (!words.isEmpty()) {
                commands.add(words);
            }
        }
        return commands;
    }

    private static String basename(String executable) {
        int slash = executable.lastIndexOf('/');
        return slash < 0 ? executable : executable.substring(slash + 1);
    }

    private static final Pattern MOUNT_TARGET = Pattern.compile("(?:^|,)target=([^,]*)");

    private static boolean alreadyMounted(Docker.Run run, String mount) {
        if (run.getFlags() == null) {
            return false;
        }
        Matcher added = MOUNT_TARGET.matcher(mount);
        if (!added.find()) {
            return false;
        }
        for (Docker.Flag flag : run.getFlags()) {
            if ("mount".equals(flag.getName()) && flag.getValue() != null) {
                Matcher existing = MOUNT_TARGET.matcher(ArgumentContents.textWithVariables(flag.getValue()));
                if (existing.find() && existing.group(1).equals(added.group(1))) {
                    return true;
                }
            }
        }
        return false;
    }

    /// A flag's value is split on the `=` that separates each key from its value, so that the flag this recipe
    /// builds holds what the same flag would hold had it been written in the file to begin with.
    private static Docker.Argument flagValue(String value) {
        List<Docker.ArgumentContent> contents = new ArrayList<>();
        int start = 0;
        for (int equals = value.indexOf('='); equals >= 0; equals = value.indexOf('=', start)) {
            contents.addAll(ArgumentContents.of(value.substring(start, equals), null));
            contents.addAll(ArgumentContents.of("=", null));
            start = equals + 1;
        }
        contents.addAll(ArgumentContents.of(value.substring(start), null));
        return new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, contents);
    }

    enum PackageManager {
        MAVEN("maven", new String[]{"mvn", "mvnw"}, new String[][]{}, new String[]{"/root/.m2"}, false, false),
        GRADLE("gradle", new String[]{"gradle", "gradlew"}, new String[][]{}, new String[]{"/root/.gradle"}, false, false),
        NPM("npm", new String[]{"npm"}, new String[][]{{"ci"}, {"install"}}, new String[]{"/root/.npm"}, false, false),
        YARN("yarn", new String[]{"yarn"}, new String[][]{{"install"}}, new String[]{"/usr/local/share/.cache/yarn"}, false, false),
        PIP("pip", new String[]{"pip", "pip3"}, new String[][]{{"install"}}, new String[]{"/root/.cache/pip"}, false, false),
        GO("go", new String[]{"go"}, new String[][]{{"build"}, {"mod", "download"}}, new String[]{"/root/.cache/go-build", "/go/pkg/mod"}, false, false),
        CARGO("cargo", new String[]{"cargo"}, new String[][]{{"build"}}, new String[]{"/usr/local/cargo/registry"}, false, false),
        APT("apt", new String[]{"apt-get"}, new String[][]{{"install"}}, new String[]{"/var/cache/apt", "/var/lib/apt/lists"}, true, true),
        APK("apk", new String[]{"apk"}, new String[][]{{"add"}}, new String[]{"/var/cache/apk"}, true, true);

        final String id;
        private final String[] executables;
        private final String[][] subcommands;
        private final String[] targets;
        private final boolean locked;
        final boolean explicitOnly;

        PackageManager(String id, String[] executables, String[][] subcommands, String[] targets, boolean locked, boolean explicitOnly) {
            this.id = id;
            this.executables = executables;
            this.subcommands = subcommands;
            this.targets = targets;
            this.locked = locked;
            this.explicitOnly = explicitOnly;
        }

        static @Nullable PackageManager of(String id) {
            for (PackageManager packageManager : values()) {
                if (packageManager.id.equalsIgnoreCase(id)) {
                    return packageManager;
                }
            }
            return null;
        }

        boolean matches(String executable, List<String> arguments) {
            if (Arrays.stream(executables).noneMatch(executable::equals)) {
                return false;
            }
            if (subcommands.length == 0) {
                return true;
            }
            List<String> words = arguments.stream().filter(argument -> !argument.startsWith("-")).collect(toList());
            return Arrays.stream(subcommands).anyMatch(subcommand ->
                    words.size() >= subcommand.length &&
                            words.subList(0, subcommand.length).equals(Arrays.asList(subcommand)));
        }

        /// Whether the cache directory holds nothing the built image goes on to need, which is not so for a
        /// command told to install into the image from what it downloads, or one that already turns caching off.
        boolean cacheIsPure(List<String> arguments, String commandText) {
            switch (this) {
                case PIP:
                    return arguments.stream().noneMatch(argument ->
                            "--target".equals(argument) || argument.startsWith("--target=") || "-t".equals(argument));
                case APK:
                    return !arguments.contains("--no-cache");
                case APT:
                    return !APT_LISTS_CLEANUP.matcher(commandText).find();
                default:
                    return true;
            }
        }

        List<String> mounts(@Nullable String sharing) {
            String mode = sharing != null ? sharing : (locked ? "locked" : null);
            return Arrays.stream(targets)
                    .map(target -> "type=cache,target=" + target + (mode == null ? "" : ",sharing=" + mode))
                    .collect(toList());
        }
    }
}
