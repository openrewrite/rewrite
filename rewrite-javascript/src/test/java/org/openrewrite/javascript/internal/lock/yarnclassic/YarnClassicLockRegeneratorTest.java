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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every install yarn ran, recorded or probed, up to its first step: the error yarn stopped with, or the request.
 */
class YarnClassicLockRegeneratorTest {

    private static final Pattern PROJECT_DIR = Pattern.compile("\\S*/project/");
    private static final Pattern QUOTED_PROJECT_DIR = Pattern.compile("'[^\\s']*/project/");
    private static final Pattern LAYOUT_DIR = Pattern.compile("\\S*/(?:repo|again)/(mid/(app/)?)?");
    private static final Pattern OVER = Pattern.compile("(?m)^over=(\\S+)$");
    private static final Pattern STEP = Pattern.compile("^\\[\\d+/\\d+] ");
    private static final Pattern MANIFEST_SYNTAX_ERROR = Pattern.compile("^error " + JsError.SYNTAX_ERROR +
                                                                         ": (\\.\\./)*package\\.json: ");
    private static final String CANNOT_KNOW = "cannot-know ";

    private static final String ROOT = "{\"name\":\"r\",\"version\":\"1.0.0\"}";
    private static final String WORKSPACE_ROOT = "{\"name\":\"r\",\"version\":\"1.0.0\",\"private\":true," +
                                                 "\"workspaces\":[\"packages/*\"]}";
    private static final String MERGED = "<<<<<<< HEAD\na@1:\n  version \"1.0.0\"\n=======\nb@1:\n" +
                                         "  version \"1.0.0\"\n>>>>>>> x\n";
    private static final String VERSION_2 = "# yarn lockfile v2\n\n";
    private static final String WORKSPACES_DISABLED = "error Your project root defines workspaces but the feature " +
                                                      "is disabled in your Yarn config. Please check " +
                                                      "\"workspaces-experimental\" in your .yarnrc file.";

    static Stream<Arguments> installs() {
        List<Arguments> out = new ArrayList<>();
        for (YarnClassicScenarios.Scenario scenario : YarnClassicScenarios.discover()) {
            String before = scenario.file("yarn-before.log");
            if (before != null) {
                out.add(Arguments.of(scenario.getLabel() + "/before", scenario, true, null, before));
            }
            String after = scenario.file("yarn-after.log");
            if (after != null) {
                out.add(Arguments.of(scenario.getLabel() + "/after", scenario, false, scenario.file("before"),
                        after));
            }
            String again = scenario.file("yarnclassic-again.log");
            Matcher over = again == null ? null : OVER.matcher(again);
            if (over != null && over.find()) {
                out.add(Arguments.of(scenario.getLabel() + "/again", scenario, false, scenario.file(over.group(1)),
                        again));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("installs")
    void stopsWhereYarnStops(String name, YarnClassicScenarios.Scenario scenario, boolean before,
                             @Nullable String lock, String log) {
        String outcome = untilTheFirstStep(new YarnClassicLockRegenerator(scenario.inputs(before, lock)));
        String expected = errorBeforeTheSteps(log);
        Matcher syntaxError = expected == null ? null : MANIFEST_SYNTAX_ERROR.matcher(expected);
        if (outcome.startsWith(CANNOT_KNOW)) {
            // yarn handed the install to another release or ran it at a workspace root above the project
            assertThat(scenario.expect()).startsWith("cannot-know");
        } else if (syntaxError != null && syntaxError.find()) {
            // V8's JSON.parse wording is not ported, only the error and the file it names
            assertThat(outcome).startsWith(syntaxError.group());
        } else if (expected != null) {
            assertThat(outcome).isEqualTo(expected);
        } else {
            assertThat(outcome).startsWith("requests");
        }
    }

    /**
     * Installs yarn 1.22.22 ran offline in a hermetic directory, and the error it printed before its first step or,
     * where it reached the step, the patterns the port requests.
     */
    static Stream<Arguments> probes() {
        return Stream.of(
                probe("illegal name, then the .npmrc's variable", "{\"name\":\"a b\",\"version\":\"1.0.0\"}",
                        null, null, "registry=${MISSING_VAR}", env(),
                        "error package.json: Name contains illegal characters"),
                probe("the .npmrc's variable", ROOT, null, null, "registry=${MISSING_VAR}", env(),
                        "error Error: Failed to replace env in config: ${MISSING_VAR}"),
                probe("dot name", "{\"name\":\".a\",\"version\":\"1.0.0\"}", null, null, null, env(),
                        "error package.json: Name can't start with a dot"),
                probe("number version, then the lock's version", "{\"name\":\"r\",\"version\":1}", VERSION_2, null,
                        null, env(), "error package.json: \"version\" is not a string"),
                probe("illegal name, then the lock's version", "{\"name\":\"a b\",\"version\":\"1.0.0\"}", VERSION_2,
                        null, null, env(), "error package.json: Name contains illegal characters"),
                probe("disabled workspaces, then the lock's version", WORKSPACE_ROOT, VERSION_2,
                        "workspaces-experimental false", null, env(), WORKSPACES_DISABLED),
                probe("--no-lockfile over a version 2 lock", ROOT, VERSION_2, "--no-lockfile true", null, env(),
                        "requests"),
                probe("--install.no-lockfile over a version 2 lock", ROOT, VERSION_2, "--install.no-lockfile true",
                        null, env(), "requests"),
                probe("number manifest", "5", null, null, null, env(),
                        "error TypeError: Cannot create property '_registry' on number '5'"),
                probe("string manifest", "\"abc\"", null, null, null, env(),
                        "error TypeError: Cannot create property '_registry' on string 'abc'"),
                probe("null manifest", "null", null, null, null, env(),
                        "error TypeError: Cannot set properties of null (setting '_registry')"),
                probe("boolean manifest", "true", null, null, null, env(),
                        "error TypeError: Cannot create property '_registry' on boolean 'true'"),
                probe("array manifest", "[]", null, null, null, env(), "requests"),
                probe("YARN_WORKSPACES_EXPERIMENTAL", WORKSPACE_ROOT, null, null, null,
                        env("YARN_WORKSPACES_EXPERIMENTAL", "false"), WORKSPACES_DISABLED),
                probe("npm_config_workspaces_experimental", WORKSPACE_ROOT, null, null, null,
                        env("npm_config_workspaces_experimental", "false"), WORKSPACES_DISABLED),
                probe("the environment over the .yarnrc", WORKSPACE_ROOT, null, "workspaces-experimental true", null,
                        env("YARN_WORKSPACES_EXPERIMENTAL", "false"), WORKSPACES_DISABLED),
                probe("npm_config_undefined stands for keys npm has no name for", WORKSPACE_ROOT, null, null, null,
                        env("npm_config_undefined", "false"), WORKSPACES_DISABLED),
                probe("environment key below a boolean one", ROOT, null, null, null,
                        env("YARN_FOO", "true", "YARN_FOO__BAR", "x"),
                        "error TypeError: Cannot create property 'bar' on boolean 'true'"),
                probe("constructor without a lock", dependency("constructor"), null, null, null, env(),
                        "requests constructor@^1.0.0"),
                probe("constructor in a merged lock", dependency("constructor"), MERGED, null, null, env(),
                        "error TypeError: Cannot assign to read only property 'name' of function " +
                        "'function Object() { [native code] }'"),
                probe("constructor in a Berry lock", dependency("constructor"), "__metadata:\n  version: 6\n", null,
                        null, env(), "error TypeError: Cannot assign to read only property 'name' of function " +
                                     "'function Object() { [native code] }'"),
                probe("toString in a merged lock", dependency("toString"), MERGED, null, null, env(),
                        "error TypeError: Cannot assign to read only property 'name' of function " +
                        "'function toString() { [native code] }'"),
                probe("valueOf in a lock js-yaml read", dependency("valueOf"), "x: [a, b]\n", null, null, env(),
                        "error TypeError: Cannot assign to read only property 'name' of function " +
                        "'function valueOf() { [native code] }'"),
                probe("hasOwnProperty in a lock yarn's parser read", dependency("hasOwnProperty"),
                        "# yarn lockfile v1\n\n", null, null, env(), "requests hasOwnProperty@^1.0.0"),
                probe("__proto__ in a merged lock", dependency("__proto__"), MERGED, null, null, env(),
                        "requests __proto__"),
                probe("map in an array lock", dependency("map"), "[a, b]\n", null, null, env(),
                        "error TypeError: Cannot assign to read only property 'name' of function " +
                        "'function map() { [native code] }'"),
                probe("length in an array lock", dependency("length"), "[a, b]\n", null, null, env(),
                        "error TypeError: Cannot create property 'optionalDependencies' on number '2'"),
                probe("__proto__ in an array lock", dependency("__proto__"), "[a, b]\n", null, null, env(),
                        "requests __proto__"),
                probe("map in a lock of dashes", dependency("map"), "- a\n- b\n", null, null, env(),
                        "requests map@^1.0.0"),
                probe("array entry under the name", "{\"name\":\"r\",\"version\":\"1.0.0\",\"dependencies\":" +
                                                    "{\"ms\":\"^2.0.0\"}}", "ms: [a, b]\n", null, null, env(),
                        "requests ms"),
                probe("array entry under the pattern", "{\"name\":\"r\",\"version\":\"1.0.0\",\"dependencies\":" +
                                                       "{\"ms\":\"^2.0.0\"}}", "ms@^2.0.0: [a, b]\n", null, null,
                        env(), "requests ms@^2.0.0"),
                probe("alias cycle", "{\"name\":\"r\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1\"}}",
                        "# yarn lockfile v1\n\na \"b\"\nb \"a\"\n", null, null, env(),
                        "error RangeError: Maximum call stack size exceeded"),
                probe("yarn-path", ROOT, null, YARN_PATH, null, env(), HANDED_OFF),
                probe("yarn-path, YARN_IGNORE_PATH=1", ROOT, null, YARN_PATH, null, env("YARN_IGNORE_PATH", "1"),
                        "requests"),
                probe("yarn-path, YARN_IGNORE_PATH=true", ROOT, null, YARN_PATH, null,
                        env("YARN_IGNORE_PATH", "true"), "requests"),
                probe("yarn-path, YARN_IGNORE_PATH=false", ROOT, null, YARN_PATH, null,
                        env("YARN_IGNORE_PATH", "false"), HANDED_OFF),
                probe("yarn-path, YARN_IGNORE_PATH=0", ROOT, null, YARN_PATH, null, env("YARN_IGNORE_PATH", "0"),
                        HANDED_OFF),
                probe("yarn-path, YARN_IGNORE_PATH empty", ROOT, null, YARN_PATH, null, env("YARN_IGNORE_PATH", ""),
                        HANDED_OFF),
                probe("packageManager", BERRY_MANAGED, null, null, null, env(), BERRY_REJECTED),
                probe("packageManager, COREPACK_ROOT", BERRY_MANAGED, null, null, null, env("COREPACK_ROOT", "/x"),
                        "requests"),
                probe("packageManager, COREPACK_ROOT empty", BERRY_MANAGED, null, null, null,
                        env("COREPACK_ROOT", ""), BERRY_REJECTED),
                probe("packageManager, SKIP_YARN_COREPACK_CHECK=1", BERRY_MANAGED, null, null, null,
                        env("SKIP_YARN_COREPACK_CHECK", "1"), "requests"),
                probe("packageManager, SKIP_YARN_COREPACK_CHECK=0", BERRY_MANAGED, null, null, null,
                        env("SKIP_YARN_COREPACK_CHECK", "0"), "requests")
        );
    }

    private static final String YARN_PATH = "yarn-path \"./r.js\"";
    private static final String HANDED_OFF = CANNOT_KNOW + "what the release yarn-path names (r.js) writes: yarn " +
                                             "1.22.22 hands the install to it";
    private static final String BERRY_MANAGED = "{\"name\":\"r\",\"version\":\"1.0.0\"," +
                                                "\"packageManager\":\"yarn@4.10.3\"}";
    private static final String BERRY_REJECTED = "error This project's package.json defines \"packageManager\": " +
                                                 "\"yarn@4.10.3\". However the current global version of Yarn is " +
                                                 "1.22.22.";

    /**
     * A range that is not semver and names a directory holding a package.json is a {@code file:} dependency
     * (package-request.js {@code normalizeRange}; {@code open-implicit-file-range}), whether or not the host read it.
     */
    @Test
    void aRangeNamingADirectoryTheProjectHoldsIsAFileDependency() {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"x\":\"next\"}}")
                .lock("# yarn lockfile v1\n\n\n")
                .now(Instant.EPOCH);
        Result unread = new YarnClassicLockRegenerator(inputs.paths(new LinkedHashSet<>(Arrays.asList(
                "package.json", "yarn.lock", "next/package.json"))).build()).regenerate();
        assertThat(unread.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the package.json " +
                                                       "of file:next, which the project's manifests do not include");

        Result absent = new YarnClassicLockRegenerator(inputs.paths(new LinkedHashSet<>(Arrays.asList(
                "package.json", "yarn.lock"))).build()).regenerate();
        assertThat(requireNonNull(absent.getFailure()).getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND);

        Result outside = new YarnClassicLockRegenerator(inputs.manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\"," +
                                                                            "\"dependencies\":{\"x\":\"a/../../b\"}}")
                .build()).regenerate();
        assertThat(outside.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know whether " +
                                                        "a/../../b/package.json exists outside the project, which " +
                                                        "makes the range a/../../b a file: dependency");
    }

    /**
     * file-resolver.js rejects a target that does not exist, naming its absolute path (here relative to the root;
     * {@code open-member-cwd-implicit-file-member-dir}); a directory without package.json is a manifest named by that
     * absolute path, and a host that gives no paths does not say.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "nothing there|package.json,yarn.lock|RESOLUTION_REQUIRED: yarn rejects this edit: Package \"x\" refers " +
            "to a non-existing file '\"missing\"'.",
            "a directory without package.json|package.json,yarn.lock,missing/README.md|RESOLUTION_REQUIRED: the " +
            "engine cannot know the package.json of file:./missing, which the project's manifests do not include",
            "no paths|-|RESOLUTION_REQUIRED: the engine cannot know the package.json of file:./missing, which the " +
            "project's manifests do not include",
    })
    void rejectsAFileTargetThatDoesNotExist(String label, @Nullable String paths, String outcome) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"x\":\"file:./missing\"}}")
                .lock("# yarn lockfile v1\n\n\n")
                .now(Instant.EPOCH);
        if (paths != null) {
            inputs.paths(new LinkedHashSet<>(Arrays.asList(paths.split(","))));
        }
        assertThat(new YarnClassicLockRegenerator(inputs.build()).regenerate().getErrorMessage()).isEqualTo(outcome);
    }

    /**
     * A yarn-path release directly in a directory above the project is missing when the host's paths reach that far
     * and do not hold it; further up it may sit above the repository.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "../yarn-1.22.22.js|-|error Error: Cannot find module '../yarn-1.22.22.js'",
            "../../yarn-1.22.22.js|-|cannot-know what the release yarn-path names (../../yarn-1.22.22.js) writes",
            "../../yarn-1.22.22.js|mid/app|error Error: Cannot find module '../../yarn-1.22.22.js'",
            "../../../yarn-1.22.22.js|mid/app|cannot-know what the release yarn-path names (../../../yarn-1.22.22.js)",
    })
    void findsAReleaseMissingOnlyAsFarUpAsThePathsReach(String release, @Nullable String directory, String outcome) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", ROOT)
                .lock("# yarn lockfile v1\n\n\n")
                .configFile(".yarnrc", "yarn-path \"" + release + "\"\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", ".yarnrc", "../README.md")))
                .directory(directory)
                .now(Instant.EPOCH)
                .build();
        assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs))).startsWith(outcome);
    }

    /**
     * Whether an ancestor's workspaces list the project depends on the project's path from it
     * ({@code open-parent-workspaces-*}), which a host that does not say where the project sits leaves unknown.
     */
    @Test
    void matchesAnAncestorsWorkspacesAgainstTheProjectsPath() {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", ROOT)
                .lock("# yarn lockfile v1\n\n\n")
                .configFile("../package.json", "{\"private\":true,\"workspaces\":[\"a*\"]}")
                .now(Instant.EPOCH);
        assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs.build()))).isEqualTo(CANNOT_KNOW +
                "whether the workspaces of ../package.json list this project, whose directory in the repository the " +
                "host did not give");
        assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs.directory("app").build()))).isEqualTo(
                CANNOT_KNOW + "the lock yarn writes for this edit: yarn installs at the workspace root ../, whose " +
                "workspaces list app, and writes ../yarn.lock, not this project's");
        assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs.directory("b").build())))
                .isEqualTo("requests");
    }

    /**
     * Run in packages/a, yarn writes the lock of the root whose workspaces it matches, whatever config files sit on
     * the way: the root's regeneration reads them from packages/a as its cwd
     * ({@code open-parent-workspaces-lists-project-own-yarnrc}, {@code open-member-cwd-own-yarnrc-registry}).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "listed|packages/*||2",
            "dot slash|./packages/*||0",
            "not listed|libs/*||0",
            "own .yarnrc|packages/*|.yarnrc|2",
            "own .npmrc|packages/*|.npmrc|2",
            "between .yarnrc.yml|packages/*|../.yarnrc.yml|2",
            "root .yarnrc|packages/*|../../.yarnrc|2",
            "above the root .npmrc|packages/*|../../../.npmrc|2"
    })
    void installsAtTheRootWhoseLockAnInstallThereWrites(String label, String pattern, @Nullable String configFile,
                                                        int installRoot) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"a\",\"version\":\"1.0.0\"}")
                .lock("# yarn lockfile v1\n\n\n")
                .configFile("../../package.json", "{\"private\":true,\"workspaces\":[\"" + pattern + "\"]}")
                .directory("x/packages/a")
                .now(Instant.EPOCH);
        if (configFile != null) {
            inputs.configFile(configFile, "registry \"https://registry.npmjs.org\"\n");
        }
        assertThat(YarnClassicLockRegenerator.installRoot(inputs.build())).isEqualTo(installRoot);
    }

    /**
     * A packageManager yarn 1 accepts leaves the install at the root, which walks from the project's directory as
     * {@code cwd}; {@code --focus} makes it a focused install there instead.
     */
    @Test
    void installsAtTheRootWithAPackageManagerBelowItAndNotUnderFocus() {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .configFile("../package.json", "{\"private\":true,\"workspaces\":[\"a\"]}")
                .directory("a")
                .now(Instant.EPOCH);
        assertThat(YarnClassicLockRegenerator.installRoot(inputs.manifest("", "{\"name\":\"a\"}").build()))
                .isEqualTo(1);
        assertThat(YarnClassicLockRegenerator.installRoot(inputs.manifest("",
                "{\"name\":\"a\",\"packageManager\":\"yarn@1.22.22\"}").build())).isEqualTo(1);
        assertThat(YarnClassicLockRegenerator.installRoot(inputs.configFile("../.yarnrc", "--focus true\n").build()))
                .isEqualTo(0);
    }

    /**
     * Run in packages/a, yarn walks to the packageManager from there and tests a range's directory there
     * (package-request.js {@code normalizeRange} reads {@code config.cwd}), while it writes the root's lock
     * ({@code open-member-cwd-package-manager-root-other}, {@code open-member-cwd-implicit-file-root-dir}).
     */
    @ParameterizedTest(name = "{0} in {1}")
    @CsvSource(delimiter = '|', value = {
            "packageManager||error This project's package.json defines \"packageManager\": \"yarn@pnpm@9.15.0\". " +
            "However the current global version of Yarn is 1.22.22.",
            "packageManager|packages/a|requests workspace-aggregator-",
            "implicit file range||file:lib",
            "implicit file range|packages/a|lib",
    })
    void readsWhereYarnRuns(String label, @Nullable String cwd, String outcome) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "packages/a/package.json",
                        "lib/package.json")))
                .cwd(cwd == null ? "" : cwd)
                .now(Instant.EPOCH);
        if ("packageManager".equals(label)) {
            inputs.manifest("", "{\"name\":\"root\",\"private\":true,\"workspaces\":[\"packages/*\"]," +
                                "\"packageManager\":\"pnpm@9.15.0\"}")
                    .manifest("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"," +
                                            "\"packageManager\":\"yarn@1.22.22\"}");
            assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs.build())))
                    .startsWith(outcome);
            return;
        }
        YarnClassicLockRegenerator.Inputs built = inputs.manifest("", WORKSPACE_ROOT)
                .manifest("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"}").build();
        YarnClassicOptions options = YarnClassicOptions.defaults();
        PackageResolver resolver = new PackageResolver(options, new Lockfile(), new ResolutionMap(options),
                (url, name) -> null, new ProjectFiles(built.getManifests(), built.getPaths(), built.getCwd()),
                Host.UNKNOWN, new ArrayList<>());
        PackageRequest request = new PackageRequest(DependencyRequestPattern.builder().pattern("x@lib")
                .registry("npm").optional(false).parentNames(new ArrayList<>()).build(), resolver);
        EventLoop.Promise<String> range = request.normalizeRange("lib");
        resolver.loop.run();
        assertThat(range.getValue()).isEqualTo(outcome);
    }

    /**
     * At a workspace root above the project yarn reads that root's lock, then rejects the workspaces getWorkspaces
     * rejects before it globs them; a lock there that the inputs lack may stop it first.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "not private|{\"name\":\"top\",\"workspaces\":[\"mid/*\"]}|false|error Workspaces can only be enabled " +
            "in private projects.",
            "packages a string|{\"private\":true,\"workspaces\":{\"packages\":\"mid/*\",\"nohoist\":[]}}|false|" +
            "error The workspaces field in package.json must be an array.",
            "not private, its lock unread|{\"name\":\"top\",\"workspaces\":[\"mid/*\"]}|true|cannot-know the lock " +
            "yarn writes for this edit: yarn installs at the workspace root ../../",
            "eligible|{\"private\":true,\"workspaces\":[\"mid/*\"]}|false|cannot-know the lock yarn writes for this " +
            "edit: yarn installs at the workspace root ../../, whose workspaces list mid/app",
    })
    void rejectsAtAWorkspaceRootAboveWhatGetWorkspacesRejects(String label, String root, boolean rootLock,
                                                               String outcome) {
        Set<String> paths = new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "../../package.json"));
        if (rootLock) {
            paths.add("../../yarn.lock");
        }
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"app\",\"version\":\"1.0.0\"}")
                .lock("# yarn lockfile v1\n\n\n")
                .configFile("../../package.json", root)
                .paths(paths)
                .directory("mid/app")
                .now(Instant.EPOCH)
                .build();
        assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs))).startsWith(outcome);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("probes")
    void stopsWhereYarnStopsWhenProbed(String label, YarnClassicLockRegenerator.Inputs inputs, String outcome) {
        assertThat(untilTheFirstStep(new YarnClassicLockRegenerator(inputs))).isEqualTo(outcome);
    }

    /**
     * An empty workspaces list makes the project a workspace root all the same, private or not
     * ({@code open-ws-empty-list-resolution}, {@code open-ws-empty-list-not-private}).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "private|{\"name\": \"root\", \"version\": \"1.0.0\", \"private\": true, \"workspaces\": [], " +
            "\"dependencies\": {\"left-pad\": \"1.3.0\"}, \"resolutions\": {\"left-pad\": \"1.2.0\"}}|" +
            "left-pad@1.2.0 left-pad@1.3.0 workspace-aggregator-",
            "not private|{\"name\": \"root\", \"version\": \"1.0.0\", \"workspaces\": [], " +
            "\"dependencies\": {\"ms\": \"2.1.3\"}}|ms@2.1.3 workspace-aggregator-",
            "no workspaces|{\"name\": \"root\", \"version\": \"1.0.0\", \"private\": true, " +
            "\"dependencies\": {\"left-pad\": \"1.3.0\"}, \"resolutions\": {\"left-pad\": \"1.2.0\"}}|" +
            "left-pad@1.2.0 left-pad@1.3.0",
            "nohoist only|{\"name\": \"root\", \"version\": \"1.0.0\", \"private\": true, \"workspaces\": " +
            "{\"nohoist\": [\"**/ms\"]}, \"dependencies\": {\"ms\": \"2.1.3\"}}|ms@2.1.3",
            "workspaces a string|{\"name\": \"root\", \"version\": \"1.0.0\", \"private\": true, \"workspaces\": " +
            "\"packages/*\", \"dependencies\": {\"ms\": \"2.1.3\"}}|ms@2.1.3",
    })
    void requestsTheAggregatorOfAnyWorkspaceRoot(String label, String manifest, String patterns) {
        YarnClassicLockRegenerator regenerator = new YarnClassicLockRegenerator(YarnClassicLockRegenerator.Inputs
                .builder().manifest("", manifest).now(Instant.EPOCH).build());
        List<String> warnings = new ArrayList<>();
        YarnClassicOptions options = regenerator.init(warnings);
        YarnClassicLockRegenerator.InstallCwdRequest request = regenerator.fetchRequestFromCwd(
                regenerator.lockfile(options), options, new ResolutionMap(options), warnings);
        List<String> requested = new ArrayList<>();
        for (DependencyRequestPattern dep : request.getRequests()) {
            String pattern = dep.getPattern();
            requested.add(pattern.startsWith("workspace-aggregator-") ? "workspace-aggregator-" : pattern);
        }
        assertThat(String.join(" ", requested)).isEqualTo(patterns);
        assertThat(request.getWorkspaceLayout() != null).isEqualTo(patterns.contains("workspace-aggregator-"));
    }

    /**
     * What config.js {@code getWorkspaces} rejects: members in a root that is not private
     * ({@code open-ws-not-private}), and a {@code packages} that is not a list beside a {@code nohoist} list.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "members not private|{\"name\": \"root\", \"version\": \"1.0.0\", \"workspaces\": [\"packages/*\"]}|" +
            "Workspaces can only be enabled in private projects.",
            "packages a string|{\"name\": \"r\", \"version\": \"1.0.0\", \"private\": true, \"workspaces\": " +
            "{\"packages\": \"packages/*\", \"nohoist\": [\"**/x\"]}}|The workspaces field in package.json must be " +
            "an array.",
    })
    void rejectsWhatGetWorkspacesRejects(String label, String manifest, String message) {
        assertThat(untilTheFirstStep(withMember(manifest, null))).isEqualTo("error " + message);
    }

    @Test
    void warnsOfANohoistListWhileNohoistIsOff() {
        YarnClassicLockRegenerator regenerator = withMember("{\"name\": \"r\", \"version\": \"1.0.0\", " +
                                                            "\"private\": true, \"workspaces\": {\"packages\": [], " +
                                                            "\"nohoist\": [\"**/x\"]}}",
                "workspaces-nohoist-experimental false\n");
        List<String> warnings = new ArrayList<>();
        YarnClassicOptions options = regenerator.init(warnings);
        regenerator.fetchRequestFromCwd(regenerator.lockfile(options), options, new ResolutionMap(options), warnings);
        assertThat(warnings).contains("\"r\" defines nohoist but the feature is disabled in your Yarn config " +
                                      "(\"workspaces-nohoist-experimental\" in .yarnrc file)");
    }

    /**
     * root-dep-in-two-scopes: yarn warns of the collision from both reads of the root manifest that pass isRoot.
     */
    @Test
    void warnsOfACollisionUnderEachRootRead() {
        YarnClassicLockRegenerator regenerator = new YarnClassicLockRegenerator(YarnClassicLockRegenerator.Inputs
                .builder().manifest("", "{\"name\": \"p\", \"version\": \"1.0.0\", \"dependencies\": " +
                                        "{\"ms\": \"^2.1.0\"}, \"devDependencies\": {\"ms\": \"2.1.2\"}}")
                .now(Instant.EPOCH).build());
        List<String> warnings = new ArrayList<>();
        YarnClassicOptions options = regenerator.init(warnings);
        regenerator.fetchRequestFromCwd(regenerator.lockfile(options), options, new ResolutionMap(options), warnings);
        String collision = ": \"dependencies\" has dependency \"ms\" with range \"^2.1.0\" that collides with a " +
                           "dependency in \"devDependencies\" of the same name with version \"2.1.2\"";
        assertThat(warnings).containsExactly("package.json" + collision, "p@1.0.0" + collision);
    }

    /**
     * A registry the host cannot reach or that refuses its credentials is the host's failure at any depth; a
     * package the registry does not have is yarn's: the registry's 404 for a root dependency (open-root-package-404),
     * {@code requiredPackageNotFoundRegistry} below one (small-workspace-protocol-unpublished-name).
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "root-dep|UNREACHABLE|",
            "child-dep|UNREACHABLE|",
            "root-dep|AUTH_FAILED|",
            "child-dep|AUTH_FAILED|",
            "root-dep|absent|PACKAGE_NOT_FOUND root-dep: yarn rejects this edit: Error: " +
            "https://registry.yarnpkg.com/root-dep: Not found",
            "child-dep|absent|PACKAGE_NOT_FOUND null: yarn rejects this edit: Couldn't find package " +
            "\"child-dep@^1.0.0\" required by \"root-dep@^1.0.0\" on the \"npm\" registry.",
    })
    void typesARegistryFailureByWhatFailed(String unfetched, String answer, @Nullable String failure) {
        PackumentSource registry = (url, name) -> {
            if (name.equals(unfetched)) {
                if ("absent".equals(answer)) {
                    return null;
                }
                throw new NodeRegistryException(NodeRegistryException.Reason.valueOf(answer), url, name, null,
                        "the host could not fetch " + name, null);
            }
            return "{\"name\":\"" + name + "\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":" +
                   "{\"name\":\"" + name + "\",\"version\":\"1.0.0\"," +
                   ("root-dep".equals(name) ? "\"dependencies\":{\"child-dep\":\"^1.0.0\"}," : "") +
                   "\"dist\":{\"tarball\":\"https://registry.npmjs.org/" + name + "/-/" + name + "-1.0.0.tgz\"," +
                   "\"shasum\":\"abc\"}}}}";
        };
        YarnClassicLockRegenerator regenerator = new YarnClassicLockRegenerator(YarnClassicLockRegenerator.Inputs
                .builder()
                .lock("# yarn lockfile v1\n\n\n")
                .manifest("", "{\"name\":\"x\",\"version\":\"1.0.0\",\"dependencies\":{\"root-dep\":\"^1.0.0\"}}")
                .now(Instant.EPOCH)
                .build(), registry);
        if (failure == null) {
            assertThatThrownBy(regenerator::regenerate)
                    .isInstanceOfSatisfying(NodeRegistryException.class, e ->
                            assertThat(e.getReason()).isEqualTo(NodeRegistryException.Reason.valueOf(answer)));
        } else {
            Failure actual = requireNonNull(regenerator.regenerate().getFailure());
            assertThat(actual.getReason() + " " + actual.getPackageName() + ": " + actual.getDetail())
                    .isEqualTo(failure);
        }
    }

    /**
     * link-resolver.js writes a bare entry for a target without package.json; only the host's paths say so.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "paths without the target's package.json|package.json|success",
            "paths with the target's package.json|package.json,local/package.json|cannot-know",
            "no paths||cannot-know",
    })
    void linksATargetThePathsSayHasNoPackageJson(String label, @Nullable String paths, String outcome) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .manifest("", "{\"name\":\"x\",\"version\":\"1.0.0\",\"dependencies\":{\"local\":\"link:./local\"}}")
                .host(Host.builder().os("darwin").build())
                .now(Instant.EPOCH);
        if (paths != null) {
            inputs.paths(new LinkedHashSet<>(Arrays.asList(paths.split(","))));
        }
        Result result = new YarnClassicLockRegenerator(inputs.build()).regenerate();
        if ("success".equals(outcome)) {
            assertThat(result.getLockFileContent()).isEqualTo("# THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS " +
                                                              "FILE DIRECTLY.\n# yarn lockfile v1\n\n\n" +
                                                              "\"local@link:./local\":\n  version \"0.0.0\"\n" +
                                                              "  uid \"\"\n");
        } else {
            assertThat(result.getErrorMessage()).contains(YarnClassicLockRegenerator.CANNOT_KNOW +
                                                          " the package.json of link:./local");
        }
    }

    /**
     * Workspace members are what yarn's glob finds among the project's files (yarn 1.22.22's bundled glob over the
     * same tree found {@code packages/node_modules/package.json} and {@code yarn.json} too), not the manifests the
     * host chose to give, and config files above the project are read as the project's own are.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "a member the host did not give|packages/b/package.json|the workspace member packages/b/package.json",
            "a directory the pattern names node_modules|packages/node_modules/package.json|" +
            "the workspace member packages/node_modules/package.json",
            "a member's yarn.json|packages/a/yarn.json|the workspace member packages/a/yarn.json",
            "below a member's node_modules|packages/a/node_modules/x/package.json|",
            "a hidden directory|packages/.b/package.json|",
            "a .npmrc above the project|../.npmrc|../.npmrc (its contents were not read)",
            "a .yarnrc.yml two directories up|../../.yarnrc.yml|../../.yarnrc.yml (its contents were not read)",
            "a .yarnrc below the project|packages/a/.yarnrc|",
    })
    void readsWhatYarnFindsAmongTheProjectsPaths(String label, String path, @Nullable String cannotKnow) {
        Result result = new YarnClassicLockRegenerator(YarnClassicLockRegenerator.Inputs.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .manifest("", WORKSPACE_ROOT)
                .manifest("packages/a", "{\"name\": \"a\", \"version\": \"1.0.0\"}")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "packages/a/package.json", path)))
                .host(Host.builder().os("darwin").build())
                .now(Instant.EPOCH)
                .build()).regenerate();
        if (cannotKnow == null) {
            assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        } else {
            assertThat(result.getErrorMessage()).contains(YarnClassicLockRegenerator.CANNOT_KNOW + " " + cannotKnow);
        }
    }

    private static YarnClassicLockRegenerator withMember(String root, @Nullable String yarnrc) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", root)
                .manifest("packages/a", "{\"name\": \"a\", \"version\": \"1.0.0\"}")
                .now(Instant.EPOCH);
        if (yarnrc != null) {
            inputs.configFile(".yarnrc", yarnrc);
        }
        return new YarnClassicLockRegenerator(inputs.build());
    }

    private static Arguments probe(String label, String manifest, @Nullable String lock, @Nullable String yarnrc,
                                   @Nullable String npmrc, Map<String, String> env, String outcome) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", manifest)
                .lock(lock)
                .env(env)
                .now(Instant.EPOCH);
        if (yarnrc != null) {
            inputs.configFile(".yarnrc", yarnrc + "\n");
        }
        if (npmrc != null) {
            inputs.configFile(".npmrc", npmrc + "\n");
        }
        return Arguments.of(label, inputs.build(), outcome);
    }

    private static Map<String, String> env(String... keysAndValues) {
        Map<String, String> env = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            env.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return Collections.unmodifiableMap(env);
    }

    private static String dependency(String name) {
        return "{\"name\":\"r\",\"version\":\"1.0.0\",\"dependencies\":{\"" + name + "\":\"^1.0.0\"}}";
    }

    /**
     * config.js {@code init}, install.js {@code run} and {@code fetchRequestFromCwd}: {@code error <what yarn
     * prints>}, or {@code requests} and the requested patterns.
     */
    private static String untilTheFirstStep(YarnClassicLockRegenerator regenerator) {
        try {
            List<String> warnings = new ArrayList<>();
            YarnClassicOptions options = regenerator.init(warnings);
            Lockfile lockfile = regenerator.lockfile(options);
            StringBuilder out = new StringBuilder("requests");
            for (DependencyRequestPattern request : regenerator.fetchRequestFromCwd(lockfile, options,
                    new ResolutionMap(options), warnings).getRequests()) {
                out.append(' ').append(request.getPattern());
            }
            return out.toString();
        } catch (MessageError e) {
            return "error " + e.getMessage();
        } catch (YarnClassicLockRegenerator.Rejected e) {
            return "error " + e.getMessage().substring(YarnClassicLockRegenerator.YARN_REJECTS.length() + 2);
        } catch (JsError e) {
            return "error " + e.getName() + ": " + e.getMessage();
        } catch (CannotKnow e) {
            return CANNOT_KNOW + e.getSubject();
        }
    }

    /**
     * The error line yarn printed before its first step, or node's when the release yarn-path names did not load,
     * a path it names made relative to the recorder's project directory; {@code null} when yarn reached a step.
     */
    private static @Nullable String errorBeforeTheSteps(String log) {
        for (String line : log.split("\r?\n")) {
            if (STEP.matcher(line).find()) {
                return null;
            }
            if (line.startsWith("error ")) {
                String message = PROJECT_DIR.matcher(line.substring("error ".length())).replaceFirst("");
                Matcher layout = LAYOUT_DIR.matcher(message);
                if (layout.find()) {
                    String up = layout.group(2) != null ? "" : layout.group(1) != null ? "../" : "../../";
                    message = message.substring(0, layout.start()) + up + message.substring(layout.end());
                }
                return "error " + message;
            }
            if (line.startsWith("Error: ")) {
                return "error " + QUOTED_PROJECT_DIR.matcher(line).replaceFirst("'");
            }
        }
        return null;
    }
}
