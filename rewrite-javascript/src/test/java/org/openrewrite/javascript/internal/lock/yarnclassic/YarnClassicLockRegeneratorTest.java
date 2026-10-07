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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every install yarn ran in a recorded scenario (the from-scratch phase, the edit, and the second install over what
 * the edit left), up to its first step ({@code [1/4] Resolving packages...}, or {@code [1/5] Validating
 * package.json...} for a root with engines): the config, the lock and the manifests read, and install.js's
 * {@code fetchRequestFromCwd}. Where yarn stopped before its steps, the port stops with yarn's message; where yarn
 * went on, the port gathers the request without an error.
 */
class YarnClassicLockRegeneratorTest {

    private static final Pattern PROJECT_DIR = Pattern.compile("\\S*/project/");
    private static final Pattern OVER = Pattern.compile("(?m)^over=(\\S+)$");
    private static final Pattern STEP = Pattern.compile("^\\[\\d+/\\d+] ");
    private static final String MANIFEST_SYNTAX_ERROR = JsError.SYNTAX_ERROR + ": package.json: ";

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
        String error = null;
        try {
            YarnClassicLockRegenerator regenerator = new YarnClassicLockRegenerator(scenario.inputs(before, lock));
            YarnClassicOptions options = regenerator.options();
            Lockfile lockfile = regenerator.lockfile();
            regenerator.fetchRequestFromCwd(lockfile, options, new ResolutionMap(options), new ArrayList<>());
        } catch (MessageError e) {
            error = e.getMessage();
        } catch (JsError e) {
            error = e.getName() + ": " + e.getMessage();
        }
        String expected = errorBeforeTheSteps(log);
        if (expected != null && error != null && expected.startsWith(MANIFEST_SYNTAX_ERROR)) {
            // V8's JSON.parse wording is not ported, only the error and the file it names
            assertThat(error).startsWith(MANIFEST_SYNTAX_ERROR);
        } else {
            assertThat(error).isEqualTo(expected);
        }
    }

    /**
     * An empty workspaces list makes the project a workspace root all the same, private or not
     * ({@code open-ws-empty-list-resolution}, {@code open-ws-empty-list-not-private}): the aggregator is requested
     * after the root's own dependencies, and a root that is not private is no error while it lists no members.
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
        YarnClassicOptions options = regenerator.options();
        YarnClassicLockRegenerator.InstallCwdRequest request = regenerator.fetchRequestFromCwd(
                regenerator.lockfile(), options, new ResolutionMap(options), new ArrayList<>());
        List<String> requested = new ArrayList<>();
        for (DependencyRequestPattern dep : request.getRequests()) {
            String pattern = dep.getPattern();
            requested.add(pattern.startsWith("workspace-aggregator-") ? "workspace-aggregator-" : pattern);
        }
        assertThat(String.join(" ", requested)).isEqualTo(patterns);
        assertThat(request.getWorkspaceLayout() != null).isEqualTo(patterns.contains("workspace-aggregator-"));
    }

    /**
     * What config.js {@code getWorkspaces} rejects, as yarn 1.22.22 printed it: members in a root that is not
     * private ({@code open-ws-not-private}), and a {@code packages} field that is not a list once a {@code nohoist}
     * list makes the object a workspaces config.
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
        YarnClassicLockRegenerator regenerator = withMember(manifest, null);
        YarnClassicOptions options = regenerator.options();
        Lockfile lockfile = regenerator.lockfile();
        assertThatThrownBy(() -> regenerator.fetchRequestFromCwd(lockfile, options, new ResolutionMap(options),
                new ArrayList<>()))
                .isInstanceOf(MessageError.class)
                .hasMessage(message);
    }

    @Test
    void warnsOfANohoistListWhileNohoistIsOff() {
        YarnClassicLockRegenerator regenerator = withMember("{\"name\": \"r\", \"version\": \"1.0.0\", " +
                                                            "\"private\": true, \"workspaces\": {\"packages\": [], " +
                                                            "\"nohoist\": [\"**/x\"]}}",
                "workspaces-nohoist-experimental false\n");
        YarnClassicOptions options = regenerator.options();
        List<String> warnings = new ArrayList<>();
        regenerator.fetchRequestFromCwd(regenerator.lockfile(), options, new ResolutionMap(options), warnings);
        assertThat(warnings).contains("\"r\" defines nohoist but the feature is disabled in your Yarn config " +
                                      "(\"workspaces-nohoist-experimental\" in .yarnrc file)");
    }

    private static YarnClassicLockRegenerator withMember(String root, @Nullable String yarnrc) {
        return new YarnClassicLockRegenerator(YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", root)
                .manifest("packages/a", "{\"name\": \"a\", \"version\": \"1.0.0\"}")
                .yarnrc(yarnrc)
                .now(Instant.EPOCH)
                .build());
    }

    /**
     * The error yarn printed before its first step, with the recorder's project directory taken out of a path it
     * names; {@code null} when yarn reached a step.
     */
    private static @Nullable String errorBeforeTheSteps(String log) {
        for (String line : log.split("\r?\n")) {
            if (STEP.matcher(line).find()) {
                return null;
            }
            if (line.startsWith("error ")) {
                return PROJECT_DIR.matcher(line.substring("error ".length())).replaceFirst("");
            }
        }
        return null;
    }
}
