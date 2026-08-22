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
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
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

    String displayName = "Use BuildKit cache mounts";

    String description = "Adds a BuildKit cache mount to each `RUN` instruction that invokes a package manager, so " +
            "that downloaded dependencies survive across builds instead of being fetched again for every layer. " +
            "Cache mounts require BuildKit, so a Dockerfile that pins a `# syntax=` frontend older than " +
            "`docker/dockerfile:1.2` is left alone. A mount is only added where the cache directory is a pure " +
            "download cache, so a command that installs into the image from that directory, such as " +
            "`pip install --target`, keeps fetching what it needs. It is also left off where it would cache " +
            "nothing: a command that turns its cache off, such as `pip install --no-cache-dir`, or empties it " +
            "again, such as `npm cache clean`; a stage that puts the cache somewhere else by setting `HOME`, " +
            "`GOPATH`, `CARGO_HOME` or the like, or that builds on the official `gradle` image, which keeps its " +
            "Gradle home outside `/root`; and a target the stage has already put something at, such as a " +
            "`settings.xml` copied into `/root/.m2`, which a mount over it would hide. " +
            "`RUN` instructions in shell form and in exec " +
            "form are both handled; a heredoc body is left alone, because the commands it holds are not modelled " +
            "as commands. A `RUN` that already mounts the same target, and a `RUN` that a preceding `USER` has " +
            "left running as somebody other than root, whose home directory this recipe cannot know, are both " +
            "left alone. Maven, Gradle, `apt` and `apk` mount with `sharing=locked`, because a build sharing " +
            "one of those caches with another build running at the same time, as the platforms of a " +
            "multi-platform build do, can corrupt it. " +
            "`apt` and `apk` are not in the default set. An `apt-get` cache mount is only added to a " +
            "`RUN` that runs `apt-get update` itself, since an empty cache hides the package lists an earlier " +
            "layer wrote; it only caches once the `docker-clean` configuration that discards the cache is " +
            "removed, which this recipe adds along with the mount; and it is incompatible with the " +
            "`rm -rf /var/lib/apt/lists/*` cleanup that `org.openrewrite.docker.AddAptGetCleanup` adds, which " +
            "this recipe leaves in place. " +
            "What a base image holds is beyond what a Dockerfile says, so review what this recipe changes " +
            "rather than applying it unattended: a base image that ships a populated cache directory, or that " +
            "sets `HOME`, `GRADLE_USER_HOME` or `GOPATH` to somewhere this recipe does not expect, is invisible " +
            "here, and a mount over such a directory hides what is in it. Nor does a Dockerfile say which " +
            "builder reads it, and a builder that is not BuildKit does not accept `RUN --mount` at all.";

    private static final Set<String> SHARING_MODES = new HashSet<>(Arrays.asList("shared", "private", "locked"));

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
        if (sharing != null) {
            validated = validated.and(Validated.test("sharing", "must be one of shared, private, locked",
                    sharing, mode -> SHARING_MODES.contains(mode)));
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
                Stage context = new Stage(baseImage(stage.getFrom()));
                return stage.withInstructions(ListUtils.map(stage.getInstructions(), instruction -> {
                    if (instruction instanceof Docker.Run) {
                        return context.root ?
                                addCacheMounts((Docker.Run) instruction, context.applicable(enabled), context, sharing) :
                                instruction;
                    }
                    context.read(instruction);
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

    private static final Pattern SYNTAX_DIRECTIVE = Pattern.compile("^#\\s*syntax\\s*=\\s*[^\\s@]+:([^\\s@]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FRONTEND_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)");

    /// A `# syntax=` directive names the frontend that builds the file, and `RUN --mount=type=cache` is only
    /// stable from `docker/dockerfile:1.2` onwards. A file that pins an older one is left alone; a file that
    /// pins nothing is built by the daemon's own frontend, which on any Docker that still receives updates is
    /// BuildKit.
    private static boolean supportsCacheMounts(Docker.File file) {
        String frontendTag = frontendTag(file);
        if (frontendTag == null) {
            return true;
        }
        Matcher version = FRONTEND_VERSION.matcher(frontendTag);
        if (!version.find()) {
            return true;
        }
        int major = Integer.parseInt(version.group(1));
        int minor = Integer.parseInt(version.group(2));
        return major > 1 || (major == 1 && minor >= 2);
    }

    /// A directive is only read at the head of the file, where the leading comments of the file sit in the
    /// prefix of whichever element the parser reached first. Every one of them is read, because a `syntax`
    /// directive may be preceded by another directive such as `escape`.
    private static @Nullable String frontendTag(Docker.File file) {
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
            for (Comment comment : space.getComments()) {
                Matcher directive = SYNTAX_DIRECTIVE.matcher(comment.getText().trim());
                if (directive.find()) {
                    return directive.group(1);
                }
            }
        }
        return null;
    }

    /// What the instructions of a stage say, up to the `RUN` being looked at, about where a package manager's
    /// cache lives and what is already there: a cache mount starts out empty and hides whatever the image has
    /// at its target, and the target this recipe knows is the one a root user with an untouched environment
    /// would use.
    private static class Stage {
        private final String baseImage;
        private final Map<String, String> environment = new HashMap<>();
        private final List<String> writtenPaths = new ArrayList<>();
        private boolean root = true;

        private Stage(String baseImage) {
            this.baseImage = baseImage;
        }

        private void read(Docker.Instruction instruction) {
            if (instruction instanceof Docker.User) {
                String user = ArgumentContents.textWithVariables(((Docker.User) instruction).getUser());
                root = "root".equals(user) || "0".equals(user);
            } else if (instruction instanceof Docker.Env) {
                for (Docker.Env.EnvPair pair : ((Docker.Env) instruction).getPairs()) {
                    environment.put(pair.getKey().getText(), ArgumentContents.textWithVariables(pair.getValue()));
                }
            } else if (instruction instanceof Docker.Arg) {
                Docker.Arg arg = (Docker.Arg) instruction;
                if (arg.getValue() != null) {
                    environment.put(arg.getName().getText(), ArgumentContents.textWithVariables(arg.getValue()));
                }
            } else if (instruction instanceof Docker.Copy) {
                destination(((Docker.Copy) instruction).getForm());
            } else if (instruction instanceof Docker.Add) {
                destination(((Docker.Add) instruction).getForm());
            }
        }

        private void destination(Docker.CopyAddForm form) {
            if (form instanceof Docker.CopyShellForm) {
                writtenPaths.add(ArgumentContents.textWithVariables(((Docker.CopyShellForm) form).getDestination()));
            } else if (form instanceof Docker.HeredocForm && ((Docker.HeredocForm) form).getDestination() != null) {
                writtenPaths.add(ArgumentContents.textWithVariables(((Docker.HeredocForm) form).getDestination()));
            }
        }

        private Set<PackageManager> applicable(Set<PackageManager> enabled) {
            Set<PackageManager> applicable = EnumSet.noneOf(PackageManager.class);
            for (PackageManager packageManager : enabled) {
                if (!packageManager.cacheMovedByBaseImage(baseImage)) {
                    applicable.add(packageManager);
                }
            }
            return applicable;
        }

        /// Whether the stage has already put something at `target`, which a mount over it would hide, as a
        /// `COPY` of a `settings.xml` into `/root/.m2` does.
        private boolean holds(String target) {
            for (String written : writtenPaths) {
                String path = written.endsWith("/") ? written.substring(0, written.length() - 1) : written;
                if (path.equals(target) || path.startsWith(target + "/") || target.startsWith(path + "/")) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String baseImage(Docker.From from) {
        String image = ArgumentContents.textWithVariables(from.getImageName());
        int slash = image.lastIndexOf('/');
        return slash < 0 ? image : image.substring(slash + 1);
    }

    private static final String DOCKER_CLEAN = "/etc/apt/apt.conf.d/docker-clean";
    private static final String REMOVE_DOCKER_CLEAN = "rm -f " + DOCKER_CLEAN + " && ";

    private static Docker.Run addCacheMounts(Docker.Run run, Set<PackageManager> enabled, Stage stage, @Nullable String sharing) {
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
            for (Target target : packageManager.targets) {
                if (!target.movedBy(stage.environment) && !alreadyMounted(run, target.path) &&
                        !stage.holds(target.path) && !removes(commandText, target.path)) {
                    mounts.add(new Docker.Flag(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, "mount",
                            flagValue(packageManager.mount(target.path, sharing))));
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
                if (packageManager.matches(executable, arguments) && packageManager.caches(arguments, commandText)) {
                    detected.add(packageManager);
                }
            }
        }
        return detected;
    }

    private static final Pattern ENVIRONMENT_ASSIGNMENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*=.*");
    private static final Pattern CONTINUATION = Pattern.compile("^[\\\\`]$");

    /// The words of each command a shell would run in turn, so that a package manager is recognized by the
    /// command it heads rather than by appearing anywhere in the text, and each command of a chain is read.
    /// A separator inside a quoted string separates nothing, so the words of `echo "build && test"` are one
    /// command headed by `echo` rather than two.
    private static List<List<String>> shellCommands(String text) {
        List<List<String>> commands = new ArrayList<>();
        for (String command : splitOnSeparators(text)) {
            List<String> words = Arrays.stream(command.trim().split("\\s+"))
                    .filter(word -> !word.isEmpty() && !CONTINUATION.matcher(word).matches())
                    .collect(toList());
            if (!words.isEmpty()) {
                commands.add(words);
            }
        }
        return commands;
    }

    private static List<String> splitOnSeparators(String text) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                current.append(c);
                if (c == quote) {
                    quote = 0;
                } else if (c == '\\' && quote == '"' && i + 1 < text.length()) {
                    current.append(text.charAt(++i));
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
                current.append(c);
            } else if (c == ';' || c == '|' || c == '&') {
                commands.add(current.toString());
                current.setLength(0);
                if (i + 1 < text.length() && text.charAt(i + 1) == c) {
                    i++;
                }
            } else {
                current.append(c);
            }
        }
        commands.add(current.toString());
        return commands;
    }

    private static String basename(String executable) {
        int slash = executable.lastIndexOf('/');
        return slash < 0 ? executable : executable.substring(slash + 1);
    }

    private static final Pattern MOUNT_TARGET = Pattern.compile("(?:^|,)target=([^,]*)");

    private static boolean alreadyMounted(Docker.Run run, String target) {
        if (run.getFlags() == null) {
            return false;
        }
        for (Docker.Flag flag : run.getFlags()) {
            if ("mount".equals(flag.getName()) && flag.getValue() != null) {
                Matcher existing = MOUNT_TARGET.matcher(ArgumentContents.textWithVariables(flag.getValue()));
                if (existing.find() && existing.group(1).equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Whether the command deletes the directory the mount would keep, which would leave the mount holding
    /// nothing the next build could use.
    private static boolean removes(String commandText, String target) {
        return Pattern.compile("rm\\s+(-\\S+\\s+)*\\S*" + Pattern.quote(target)).matcher(commandText).find();
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

    /// A directory a package manager downloads into, and the environment variables that move it, each paired
    /// with the value this recipe's target assumes. A variable paired with `null` moves the directory whatever
    /// it is set to.
    static class Target {
        final String path;
        final String[][] variables;

        Target(String path, String[][] variables) {
            this.path = path;
            this.variables = variables;
        }

        boolean movedBy(Map<String, String> environment) {
            for (String[] variable : variables) {
                String value = environment.get(variable[0]);
                if (value != null && !value.equals(variable[1])) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final String[][] HOME = {{"HOME", "/root"}};

    enum PackageManager {
        MAVEN(
                "maven",
                new String[]{"mvn", "mvnw"},
                new String[][]{},
                new Target[]{
                        new Target("/root/.m2", HOME)},
                new String[]{},
                null,
                null,
                true,
                false),
        GRADLE(
                "gradle",
                new String[]{"gradle", "gradlew"},
                new String[][]{},
                new Target[]{
                        new Target("/root/.gradle", new String[][]{{"HOME", "/root"}, {"GRADLE_USER_HOME", "/root/.gradle"}})},
                new String[]{},
                null,
                null,
                true,
                false),
        NPM(
                "npm",
                new String[]{"npm"},
                new String[][]{{"ci"}, {"install"}},
                new Target[]{
                        new Target("/root/.npm", new String[][]{{"HOME", "/root"}, {"npm_config_cache", null}, {"NPM_CONFIG_CACHE", null}})},
                new String[]{},
                Pattern.compile("\\bnpm\\s+cache\\s+(clean|clear)\\b"),
                null,
                false,
                false),
        YARN(
                "yarn",
                new String[]{"yarn"},
                new String[][]{{"install"}},
                new Target[]{
                        new Target("/usr/local/share/.cache/yarn", new String[][]{{"YARN_CACHE_FOLDER", null}})},
                new String[]{},
                Pattern.compile("\\byarn\\s+cache\\s+clean\\b|\\bYARN_CACHE_FOLDER="),
                null,
                false,
                false),
        PIP(
                "pip",
                new String[]{"pip", "pip3", "python", "python3"},
                new String[][]{{"install"}, {"pip", "install"}},
                new Target[]{
                        new Target("/root/.cache/pip", new String[][]{{"HOME", "/root"}, {"PIP_CACHE_DIR", null}, {"PIP_NO_CACHE_DIR", null}, {"XDG_CACHE_HOME", null}})},
                new String[]{"--target", "-t", "--no-cache-dir", "--no-cache"},
                Pattern.compile("\\bpip3?\\s+cache\\s+purge\\b"),
                null,
                false,
                false),
        GO(
                "go",
                new String[]{"go"},
                new String[][]{{"build"}, {"install"}, {"mod", "download"}},
                new Target[]{
                        new Target("/root/.cache/go-build", new String[][]{{"HOME", "/root"}, {"GOCACHE", null}}),
                        new Target("/go/pkg/mod", new String[][]{{"GOPATH", "/go"}, {"GOMODCACHE", null}})},
                new String[]{},
                Pattern.compile("\\bgo\\s+clean\\b[^&|;]*-(mod)?cache\\b"),
                null,
                false,
                false),
        CARGO(
                "cargo",
                new String[]{"cargo"},
                new String[][]{{"build"}, {"install"}, {"fetch"}},
                new Target[]{
                        new Target("/usr/local/cargo/registry", new String[][]{{"CARGO_HOME", "/usr/local/cargo"}})},
                new String[]{},
                null,
                null,
                false,
                false),
        APT(
                "apt",
                new String[]{"apt-get"},
                new String[][]{{"install"}},
                new Target[]{
                        new Target("/var/cache/apt", new String[][]{}),
                        new Target("/var/lib/apt/lists", new String[][]{})},
                new String[]{},
                Pattern.compile("rm\\s+(-[a-zA-Z]+\\s+)*/var/lib/apt/lists"),
                Pattern.compile("\\bapt-get\\s+(-\\S+\\s+)*update\\b"),
                true,
                true),
        APK(
                "apk",
                new String[]{"apk"},
                new String[][]{{"add"}},
                new Target[]{
                        new Target("/var/cache/apk", new String[][]{})},
                new String[]{"--no-cache"},
                null,
                null,
                true,
                true);

        final String id;
        final Target[] targets;
        final boolean explicitOnly;
        private final String[] executables;
        private final String[][] subcommands;

        /// Arguments that tell this package manager not to cache at all, so that a mount over its cache
        /// directory would keep nothing.
        private final String[] cacheDisablingArguments;

        /// What a command does to empty the cache it has just filled, such as `npm cache clean`, or to fill
        /// somewhere else, such as assigning `YARN_CACHE_FOLDER`.
        private final @Nullable Pattern cacheDefeatedBy;

        /// What a command must also do for the mount to be filled rather than merely hiding what the image
        /// already has at the target: an `apt-get install` only reads package lists another command wrote.
        private final @Nullable Pattern cacheFilledBy;

        private final boolean locked;

        PackageManager(
                String id,
                String[] executables,
                String[][] subcommands,
                Target[] targets,
                String[] cacheDisablingArguments,
                @Nullable Pattern cacheDefeatedBy,
                @Nullable Pattern cacheFilledBy,
                boolean locked,
                boolean explicitOnly) {
            this.id = id;
            this.executables = executables;
            this.subcommands = subcommands;
            this.targets = targets;
            this.cacheDisablingArguments = cacheDisablingArguments;
            this.cacheDefeatedBy = cacheDefeatedBy;
            this.cacheFilledBy = cacheFilledBy;
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

        /// Whether a mount over this package manager's cache would in fact be filled by the command: not so
        /// for a command told to install into the image from what it downloads, one that turns caching off,
        /// one that empties the cache it just filled, or one that reads a directory the empty cache would hide.
        boolean caches(List<String> arguments, String commandText) {
            for (String argument : arguments) {
                for (String disabling : cacheDisablingArguments) {
                    if (argument.equals(disabling) || argument.startsWith(disabling + "=")) {
                        return false;
                    }
                }
            }
            return (cacheDefeatedBy == null || !cacheDefeatedBy.matcher(commandText).find()) &&
                    (cacheFilledBy == null || cacheFilledBy.matcher(commandText).find());
        }

        /// The official `gradle` image ships a `GRADLE_USER_HOME` that is not under `/root`, so the directory
        /// this recipe would mount is not the one Gradle reads.
        boolean cacheMovedByBaseImage(String baseImage) {
            return this == GRADLE && "gradle".equals(baseImage);
        }

        String mount(String target, @Nullable String sharing) {
            String mode = sharing != null ? sharing : (locked ? "locked" : null);
            return "type=cache,target=" + target + (mode == null ? "" : ",sharing=" + mode);
        }
    }
}
